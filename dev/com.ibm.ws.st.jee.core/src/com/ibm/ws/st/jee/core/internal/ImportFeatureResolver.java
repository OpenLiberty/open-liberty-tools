/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

import com.ibm.ws.st.core.internal.FeatureResolver;
import com.ibm.ws.st.core.internal.FeatureResolverFeature;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.RequiredFeatureMap;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;

@SuppressWarnings("restriction")
public class ImportFeatureResolver extends FeatureResolver {
    private static final int SEARCH_LIMIT = 7500;

    // map holds runtime id to API info mapping
    private static Map<String, APIInfo> runtimeAPIMap = new HashMap<String, APIInfo>();

    // Support both the old com.ibm.ws.* names and the new com.ibm.websphere.* names for dev/api/spec libraries.
    // The com.ibm.ws.javaee.dd.ejb library is in lib and its name has not changed.
    private static final String[][] PREFERRED_JAR_FEATURE_MAP = { { "com.ibm.ws.javaee.servlet", "servlet" },
                                                                  { "com.ibm.ws.javaee.dd.ejb", "ejbLite" },
                                                                  { "com.ibm.ws.javaee.ejb", "ejbLite" },
                                                                  { "com.ibm.ws.javaee.jsp", "jsp" },
                                                                  { "com.ibm.ws.javaee.jaxws", "jaxws" },
                                                                  { "com.ibm.ws.javaee.jaxrs", "jaxrs" },
                                                                  { "com.ibm.ws.javaee.persistence", "jpa" },
                                                                  { "com.ibm.websphere.javaee.servlet", "servlet" },
                                                                  { "com.ibm.websphere.javaee.ejb", "ejbLite" },
                                                                  { "com.ibm.websphere.javaee.jsp", "jsp" },
                                                                  { "com.ibm.websphere.javaee.jaxws", "jaxws" },
                                                                  { "com.ibm.websphere.javaee.jaxrs", "jaxrs" },
                                                                  { "com.ibm.websphere.javaee.persistence", "jpa" },
                                                                  { "com.ibm.websphere.javaee.jsonb", "jsonb" },
                                                                  { "com.ibm.websphere.javaee.jsonp", "jsonp" },
                                                                  { "com.ibm.websphere.javaee.cdi", "cdi" }
    };

    private static final Set<String> IGNORE_PKG_SET = new HashSet<String>();

    static {
        IGNORE_PKG_SET.add("javax.annotation");
        IGNORE_PKG_SET.add("javax.annotation.security");
        IGNORE_PKG_SET.add("javax.annotation.sql");
    }

    // class holds a list of all packages and feature mapping for a single runtime
    static class APIInfo {
        String[] packages;
        String[][] features;
        Map<String, String> preferredFeatureMap;
        Map<String, List<String>> apiPackageMap;
    }

    private static APIInfo getDevAnnotations(WebSphereRuntime wsRuntime, IJavaProject javaProject) throws CoreException {
        if (wsRuntime == null)
            return null;

        String key = wsRuntime.getRuntime().getId();
        APIInfo info = runtimeAPIMap.get(key);
        if (info != null)
            return info;

        long time = System.currentTimeMillis();

        // using a list since this maps jars to features and they have a many-to-many relationship
        List<String[]> jarMap = new ArrayList<String[]>();
        Map<String, String[]> packageMap = new HashMap<String, String[]>(125);
        Map<String, String> preferredFeatures = new HashMap<String, String>();

        List<String> features = FeatureList.getFeatures(false, wsRuntime);
        for (String feature : features) {
            Set<String> jars = FeatureList.getFeatureAPIJars(feature, wsRuntime);
            for (String jar : jars) {
                jarMap.add(new String[] { jar, feature });
            }
        }

        IVMInstall vmInstall = wsRuntime.getVMInstall();
        String jreLocation = vmInstall != null ? vmInstall.getInstallLocation().toString().replace('\\', '/') : null;
        IPackageFragment[] pfs = javaProject.getPackageFragments();
        for (IPackageFragment pf : pfs) {
            IPath path = pf.getPath();
            if (pf.getKind() == IPackageFragmentRoot.K_BINARY) {
                String curElementName = pf.getElementName();
                if (path.toString().contains("/dev/")) {
                    List<String> matchedFeatures = new ArrayList<String>(3);
                    for (String[] s : jarMap) {
                        if (path.toPortableString().contains(s[0])) {
                            IClassFile[] cfs = pf.getClassFiles();
                            if (cfs.length > 0) {
                                String feature = s[1];
                                if (feature.indexOf("-") > 0) {
                                    feature = feature.substring(0, feature.indexOf("-"));
                                }
                                matchedFeatures.add(feature);

                                // jpa-2.1 uses jpaContainer-2.1 at its core, so imports of jpa interfaces will
                                // resolve only to jpaContainer unless jpa is piggybacked on here. The end result
                                // is to always automatically add the jpa-2.1 feature, never jpaContainer-2.1.
                                if (curElementName.startsWith("javax.persistence")) {
                                    matchedFeatures.add("jpa");
                                }
                            }
                        }
                    }

                    if (!matchedFeatures.isEmpty())
                        packageMap.put(curElementName, matchedFeatures.toArray(new String[matchedFeatures.size()]));

                    // populate the preferred features
                    String preferredFeature = getFeatureFromPreferredList(pf.getPath());
                    if (preferredFeature != null && curElementName != null && curElementName.length() > 0) {
                        preferredFeatures.put(curElementName, preferredFeature);
                    }
                } else if ((curElementName.startsWith("javax.jws") || curElementName.startsWith("javax.xml.ws"))
                           && jreLocation != null && path.toString().startsWith(jreLocation)) {
                    // Special case the javax.jws package that is coming from the JDK.
                    packageMap.put(curElementName, new String[] { "jaxws" });
                }
            }
        }

        // build API info object, sorted to help with binary searching later
        info = new APIInfo();
        Set<String> packageSet = packageMap.keySet();
        int size = packageSet.size();
        info.packages = packageSet.toArray(new String[size]);
        Arrays.sort(info.packages);

        info.features = new String[size][];
        for (int i = 0; i < size; i++)
            info.features[i] = packageMap.get(info.packages[i]);

        info.preferredFeatureMap = preferredFeatures;

        info.apiPackageMap = getAPIPackageMap(wsRuntime);

        runtimeAPIMap.put(key, info);

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "# API Packages: " + packageMap.keySet().size() + " - " + key);
            Trace.tracePerf("Package & annotation metadata", time);
        }

        return info;
    }

    private static Map<String, List<String>> getAPIPackageMap(WebSphereRuntime wsRuntime) {
        Map<String, List<String>> apiPackageMap = new HashMap<String, List<String>>();
        List<String> featureList = FeatureList.getFeatures(false, wsRuntime);
        for (String feature : featureList) {
            Set<String> packages = FeatureList.getFeatureAPIPackages(feature, wsRuntime);
            for (String pkg : packages) {
                List<String> features = apiPackageMap.get(pkg);
                if (features == null) {
                    features = new ArrayList<String>();
                    apiPackageMap.put(pkg, features);
                }
                if (feature.indexOf("-") > 0) {
                    feature = feature.substring(0, feature.indexOf("-"));
                }
                if (!features.contains(feature)) {
                    features.add(feature);
                }
            }
        }
        return apiPackageMap;
    }

    /**
     * @param path The path to an API Jar.
     * @return The feature name associated with the jar.
     */
    private static String getFeatureFromPreferredList(IPath path) {
        for (String[] curMap : PREFERRED_JAR_FEATURE_MAP) {
            int index = path.toString().indexOf(curMap[0]);
            if (index > 0 && path.toString().matches(".*(" + curMap[0] + "(\\.\\d+)*_(\\d+\\.)*jar)"))
                return curMap[1];
        }
        return null;
    }

    /**
     * Performs a binary search to determine whether the array contains the given string.
     *
     * @param array a string array
     * @param s a string to find
     * @return <code>true</code> if the array contains the string, and <code>false</code> otherwise
     */
    private static int contains(String[] array, String s) {
        int low = 0;
        int mid = 0;
        int high = array.length - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            int result = array[mid].compareTo(s);
            if (result < 0)
                low = mid + 1;
            else if (result == 0)
                return mid;
            else
                high = mid - 1;
        }
        return -1;
    }

    /**
     * Check if the package (pkg) is an API package. If it is and hasn't been found before,
     * add it to the feature list.
     *
     * @param pkg a package name
     * @param info API info
     * @param features the list of features to add to
     * @param existingFeatures the set of features already contained in the server config
     * @param featurePackages contains the packages that are included by the existing features, used to
     *            decide if a package is already covered by an existing feature (for example, the jmsMdb
     *            feature includes the ejb jars)
     * @param isAcceptMultipleFeatures When <code>true</code> the complete list of mapped features is returned.
     *            When <code>false</code> only a single feature will be added to <code>features</code> if and only if
     *            <code>pkg</code> maps to only one feature. <code> false </code> is typically used in cases when
     *            there is not enough information to choose between multiple features.
     */
    private static void checkAndAddFeatures(String pkg, APIInfo info, List<String> features, FeatureSet existingFeatures, FeaturePackages featurePackages,
                                            boolean isAcceptMultipleFeatures) {

        if (ignorePackage(pkg)) {
            return;
        }

        // Try the apiPackage map first as it is more precise
        List<String> apiFeatures = info.apiPackageMap.get(pkg);
        if (apiFeatures != null && apiFeatures.size() == 1) {
            if (isFeatureNeeded(apiFeatures.get(0), pkg, features, existingFeatures, featurePackages)) {
                features.add(apiFeatures.get(0));
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Found feature: " + apiFeatures.get(0) + " (" + pkg + ")");
                }
            }
        }

        int index = contains(info.packages, pkg);
        if (index > 0) {
            String[] featureList = info.features[index];

            // Gather the features related to this package
            // and perform analysis to find any required features.
            String preferredFeature = info.preferredFeatureMap.get(pkg);
            boolean containsPreferredFeature = false;

            for (String feature : featureList) {
                // We don't need to add any features if they are already added to the
                // server config (existingFeatures) or have already been detected (requiredFeatures)
                if (!isFeatureNeeded(feature, pkg, features, existingFeatures, featurePackages)) {
                    return;
                }
                if (preferredFeature != null && preferredFeature.equals(feature)) {
                    containsPreferredFeature = true;
                }
            }

            if (featureList.length == 1) {
                features.add(featureList[0]);
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Found feature: " + featureList[0] + " (" + pkg + ")");
            } else if (isAcceptMultipleFeatures) {
                // If we found multiple features and the preferred feature is one of them
                // then use that feature.
                // containsPreferredFeature can only be true iff featureList.length > 0
                if (containsPreferredFeature) {
                    features.add(preferredFeature);
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Found feature: " + preferredFeature + " (" + pkg + ")");
                }
            }
        }
    }

    private static boolean isFeatureNeeded(String feature, String pkg, List<String> features, FeatureSet existingFeatures, FeaturePackages featurePackages) {
        if (!features.isEmpty() && features.contains(feature)) {
            return false;
        }
        if (existingFeatures != null && existingFeatures.resolve(feature) != null) {
            return false;
        }
        if (featurePackages != null && featurePackages.containsPackage(pkg)) {
            return false;
        }
        return true;
    }

    /**
     * Process all annotations on an element to look for possible required features.
     *
     * @param type a parent type
     * @param annotatable an object with annotations
     * @param info API info
     * @param requiredFeatures a list of currently required features
     * @param featurePackages contains the packages that are included by the existing features, used to
     *            decide if a package is already covered by an existing feature (for example, the jmsMdb
     *            feature includes the ejb jars)
     * @throws JavaModelException
     */
    private static void processAnnotations(IType type, IAnnotatable annotatable, APIInfo info, List<String> requiredFeatures, FeatureSet existingFeatures,
                                           FeaturePackages featurePackages) throws JavaModelException {
        IAnnotation[] ann = annotatable.getAnnotations();
        for (IAnnotation a : ann) {
            String pkg = a.getElementName();
            int ind = pkg.lastIndexOf(".");
            if (ind > 0) { // we only need to look at fully qualified annotations, other packages were caught via imports
                pkg = pkg.substring(0, ind);
                checkAndAddFeatures(pkg, info, requiredFeatures, existingFeatures, featurePackages, true);
            }
        }
    }

    private static boolean deltaIncludesJavaChange(IModuleResourceDelta mrd) {
        IModuleResource mr = mrd.getModuleResource();
        if (mr instanceof IModuleFolder) {
            IModuleResourceDelta[] members = mrd.getAffectedChildren();
            if (members != null) {
                for (IModuleResourceDelta mrdChild : members)
                    if (deltaIncludesJavaChange(mrdChild))
                        return true;
            }
        } else if (mr instanceof IModuleFile) {
            if (mrd.getKind() != IModuleResourceDelta.REMOVED && mr.getName().endsWith(".class"))
                return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the given delta contains Java class changes, and <code>false</code> otherwise.
     *
     * @param delta
     * @return <code>true</code> if the delta contains Java class changes, and <code>false</code> otherwise
     */
    private static boolean deltaIncludesJavaChange(IModuleResourceDelta[] delta) {
        if (delta == null)
            return false;

        for (IModuleResourceDelta mrd : delta)
            if (deltaIncludesJavaChange(mrd))
                return true;

        return false;
    }

    private FeatureResolverFeature[] getFeaturesForModule(WebSphereRuntime wr, IModule[] module, IModuleResourceDelta[] delta, FeatureSet existingFeatures,
                                                          FeaturePackages featurePackages, IProgressMonitor monitor) {
        if (module == null || module.length == 0)
            return null;

        try {
            IProject project = module[module.length - 1].getProject();
            if (project == null || !project.hasNature(JavaCore.NATURE_ID))
                return null;

            // no need to run when there is a delta and it doesn't include .class files
            if (delta != null && !deltaIncludesJavaChange(delta))
                return null;

            IJavaProject javaProject = JavaCore.create(project);
            APIInfo info = getDevAnnotations(wr, javaProject);
            if (info == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "No WAS classpath, skipping import scanning");
                return null;
            }
            long time = System.currentTimeMillis();

            // search through all compilation units for element and method annotations
            int count = 0;
            List<String> requiredFeatures = new ArrayList<String>();
            IPackageFragment[] pfs = javaProject.getPackageFragments();
            packageSearch: for (IPackageFragment pf : pfs) {
                if (pf.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    ICompilationUnit[] cus = pf.getCompilationUnits();
                    for (ICompilationUnit cu : cus) {
                        IImportDeclaration[] imps = cu.getImports();
                        for (IImportDeclaration im : imps) {
                            String pkg = im.getElementName();
                            if (im.isOnDemand())
                                pkg = pkg.substring(0, pkg.length() - 2);
                            else {
                                int ind = pkg.lastIndexOf(".");
                                if (ind > 0)
                                    pkg = pkg.substring(0, ind);
                            }
                            checkAndAddFeatures(pkg, info, requiredFeatures, existingFeatures, featurePackages, true);
                        }

                        IType[] allTypes = cu.getAllTypes();
                        for (IType type : allTypes) {
                            count++;
                            try {
                                processAnnotations(type, type, info, requiredFeatures, existingFeatures, featurePackages);

                                IMethod[] methods = type.getMethods();
                                for (IMethod m : methods) {
                                    count++;
                                    processAnnotations(type, m, info, requiredFeatures, existingFeatures, featurePackages);
                                }
                            } catch (Exception e) {
                                if (Trace.ENABLED)
                                    Trace.logError("Error processing type for annotations: " + type.getFullyQualifiedName(), e);
                            }
                        }
                        if (count > SEARCH_LIMIT)
                            break packageSearch;
                    }
                }
            }

            if (Trace.ENABLED)
                Trace.tracePerf("Import scanning", time);

            // return the required features
            if (requiredFeatures.isEmpty()) {
                return null;
            }

            // Convert the list of strings to an array of FeatureResolverFeature

            FeatureResolverFeature[] result = new FeatureResolverFeature[requiredFeatures.size()];

            for (int x = 0; x < result.length; x++) {
                result[x] = new FeatureResolverFeature(requiredFeatures.get(x));
            }
            return result;

        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error scanning imports", e);
        }
        return null;
    }

    @Override
    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                    RequiredFeatureMap requiredFeatureMap, boolean includeAll, IProgressMonitor monitor) {
        if (moduleList == null || moduleList.isEmpty())
            return;

        if (deltaList != null && moduleList.size() != deltaList.size()) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "The delta list should be either null or have the same size as the module list", null);
            return;
        }

        // Ensure that no features are added for packages already covered by existing features
        // or features that have already been found by other resolvers such as the facet
        // feature resolver (the import feature resolver should have lowest priority).
        Set<String> featureSet = new HashSet<String>();
        if (existingFeatures != null) {
            Iterator<String> featureIterator = existingFeatures.iterator();
            while (featureIterator.hasNext()) {
                featureSet.add(featureIterator.next());
            }
        }
        for (FeatureResolverFeature feature : requiredFeatureMap.getFeatures()) {
            featureSet.add(feature.getName());
        }
        FeaturePackages featurePackages = new FeaturePackages(wr, featureSet);

        for (int i = 0; i < moduleList.size(); i++) {
            IModule[] module = moduleList.get(i);
            IModuleResourceDelta[] delta = deltaList == null ? null : deltaList.get(i);
            FeatureResolverFeature[] requiredFeatures = getFeaturesForModule(wr, module, delta, existingFeatures, featurePackages, monitor);
            if (requiredFeatures != null) {
                for (FeatureResolverFeature feature : requiredFeatures) {
                    FeatureResolver.checkAndAddFeature(requiredFeatureMap, existingFeatures, wr, feature, Collections.singletonList(module), includeAll);
                }
            }
        }
    }

    @Override
    public FeatureResolverFeature[] getContainedFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, IProgressMonitor monitor) {
        if (moduleList == null || moduleList.isEmpty() || wr == null)
            return null;

        List<String> containedFeatures = new ArrayList<String>();

        for (IModule[] module : moduleList) {
            if (module == null || module.length == 0)
                continue;

            try {
                IProject project = module[module.length - 1].getProject();
                if (project == null || !project.hasNature(JavaCore.NATURE_ID))
                    continue;

                IJavaProject javaProject = JavaCore.create(project);
                APIInfo info = getDevAnnotations(wr, javaProject);
                if (info == null) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "No WAS classpath, skipping scanning");
                    continue;
                }
                long time = System.currentTimeMillis();

                IVMInstall vmInstall = wr.getVMInstall();
                String jreLocation = vmInstall != null ? vmInstall.getInstallLocation().toString().replace('\\', '/') : null;
                IPackageFragment[] pfs = javaProject.getPackageFragments();
                for (IPackageFragment pf : pfs) {
                    if (!pf.isDefaultPackage() && pf.getKind() == IPackageFragmentRoot.K_BINARY) {
                        IPath path = pf.getPath();
                        if (!path.toString().contains("/dev/") && (jreLocation == null || !path.toString().startsWith(jreLocation))) {
                            if (pf.containsJavaResources())
                                checkAndAddFeatures(pf.getElementName(), info, containedFeatures, null, null, false);
                        }
                    }
                }
                if (Trace.ENABLED)
                    Trace.tracePerf("Package scanning", time);

                // return the contained features
                if (containedFeatures.isEmpty()) {
                    return null;
                }

            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error scanning packages", e);
            }
        }

        if (containedFeatures.isEmpty())
            return null;

        // Convert the list of strings to an array of FeatureResolverFeature
        FeatureResolverFeature[] result = new FeatureResolverFeature[containedFeatures.size()];
        for (int x = 0; x < result.length; x++) {
            result[x] = new FeatureResolverFeature(containedFeatures.get(x));
        }
        return result;
    }

    private static class FeaturePackages {
        private final WebSphereRuntime wsRuntime;
        private final Set<String> features;
        private Set<String> packages;

        public FeaturePackages(WebSphereRuntime wsRuntime, Set<String> features) {
            this.wsRuntime = wsRuntime;
            this.features = features;
        }

        public boolean containsPackage(String pkg) {
            Set<String> packages = getPackages();
            return packages.contains(pkg);
        }

        private Set<String> getPackages() {
            if (packages == null) {
                packages = new HashSet<String>();
                if (features == null || features.isEmpty()) {
                    // Only have existing features if deploying the app to a specific server.  If
                    // just getting the required features for an app then existing features will
                    // be null.
                    return packages;
                }

                long timestamp = System.currentTimeMillis();

                // Get all of the enabled features including those enabled implicitly by
                // existing features.
                Set<String> allEnabled = new HashSet<String>();
                Iterator<String> iterator = features.iterator();
                while (iterator.hasNext()) {
                    String feature = iterator.next();
                    allEnabled.add(feature);
                    allEnabled.addAll(FeatureList.getFeatureChildren(feature, wsRuntime));
                }

                // Get all of the jars provided by the enabled features
                Set<String> jars = new HashSet<String>();
                for (String feature : allEnabled) {
                    Set<String> featureJars = FeatureList.getFeatureAPIJars(feature, wsRuntime);
                    jars.addAll(featureJars);
                }

                // Get all of the packages in the jars
                for (String jar : jars) {
                    IPath path = wsRuntime.getRuntimeLocation().append(jar);
                    File file = path.toFile();
                    if (!file.exists()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Jar file from feature list does not exist: " + jar);
                        continue;
                    }
                    ZipInputStream inputStream = null;
                    try {
                        inputStream = new ZipInputStream(new FileInputStream(file));
                        for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream.getNextEntry()) {
                            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                String name = entry.getName();
                                int index = name.lastIndexOf('/');
                                String pkg = name.substring(0, index);
                                pkg = pkg.replace('/', '.');
                                packages.add(pkg);
                            }
                        }
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Exception trying to read jar file: " + path.toOSString(), e);
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }
                if (Trace.ENABLED) {
                    Trace.tracePerf("Existing feature package collection", timestamp);
                }
            }

            return packages;
        }
    }

    private static boolean ignorePackage(String pkg) {
        return IGNORE_PKG_SET.contains(pkg);
    }

}

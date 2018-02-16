/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

/**
 * Classpath provider for the runtime.
 */
public class WebSphereRuntimeClasspathProvider extends RuntimeClasspathProviderDelegate {
    // Top level folder
    public static final String FOLDER_DEV = "dev";
    // Sub folders under dev
    public static final String FOLDER_API = "api";
    public static final String FOLDER_SPI = "spi";
    public static final String FOLDER_TOOLS = "tools"; // Also as top level folder on Liberty 8.5.
    // Sub folders under api and spi.
    public static final String FOLDER_SPEC = "spec"; // Also as top level folder on Liberty 8.5.
    public static final String FOLDER_IBM = "ibm";
    public static final String FOLDER_THIRD_PARTY = "third-party"; // Also as top level folder on Liberty 8.5.
    public static final String FOLDER_STABLE = "stable";
    // Sub folders under spec, ibm or third-party
    public static final String FOLDER_JAVADOC = "javadoc";
    // Top level folder on Liberty 8.5 only.
    public static final String FOLDER_IBM_API = "ibm-api";

    private static final String PATTERN_JAR_FULL_VERSION = ".*\\_\\d+(\\.\\d+(\\.\\d+))\\.jar";
    private static final Pattern patternJarFullVersion = Pattern.compile(PATTERN_JAR_FULL_VERSION);
    private static final String JAVADOC_ZIP_EXT = "-javadoc.zip";
    private static final String JAVADOC_URL_EXT = "-javadoc.url";
    private static Map<String, RuntimeClasspathEntry> parsedEntriesCache = new HashMap<String, RuntimeClasspathEntry>();

    /**
     * Finds jars at the specified <code>path</code> and adds them to the provided <code>list</code>.
     *
     * @param path the file path to look for jars
     * @param pc
     * @param list
     * @param searchAllSubdirectories when <code>true</code> searches all subdirectories, when <code>false</code>
     *            checks directory names against predefined inclusion list.
     * @param jarsToSkip a list of jars that we do not want to take into account when finding jars. Can be <code>null</code>
     */
    protected static void findJars(IPath path, ProjectPrefs prefs, List<IClasspathEntry> list, boolean searchAllSubdirectories, Set<String> jarsToSkip) {
        File dir = path.toFile();
        if (!dir.exists() || !dir.isDirectory())
            return;

        File[] files = dir.listFiles();
        IPath javadocPath = path.append(FOLDER_JAVADOC);
        File javadocDir = javadocPath.toFile();
        // Use the javadoc folder if the folder exist.
        if (!javadocDir.exists() || !javadocDir.isDirectory()) {
            // No javadoc is available.
            javadocDir = null;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (searchAllSubdirectories || isIncludedFolder(f.getName(), prefs)) {
                    findJars(path.append(f.getName()), prefs, list, searchAllSubdirectories, jarsToSkip);
                }
            } else {
                String name = f.getName();
                boolean skip = false;
                if (jarsToSkip != null && !jarsToSkip.isEmpty()) {
                    for (String jarToSkip : jarsToSkip) {
                        if (jarToSkip.endsWith(name)) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (skip) {
                    continue;
                }
                if (name != null && name.endsWith(".jar")) {
                    File matchedJavaDoc = null;
                    String javaDocUrlStr = null;
                    // Skip the javadoc check if no javadoc folder exists.
                    if (javadocDir != null) {
                        // The javadoc jar naming convention for v8.5.5 has the version number of the jar.
                        matchedJavaDoc = getJavadocFile(javadocDir, name, JAVADOC_ZIP_EXT);
                        if (matchedJavaDoc == null) {
                            // Check the url file for v8.5.5 or later, mainly for spec jar and third party jars.
                            // The url method was short lived and stopped being used after 8.5.5.3 because it did
                            // not work well.
                            javaDocUrlStr = getJavadocUrl(javadocDir, name);
                            if (javaDocUrlStr == null) {
                                // Default to v8.5 support. The javadoc jar naming convention for v8.5 skips the version number of the jar.
                                int ind = name.lastIndexOf("_");
                                if (ind >= 0) {
                                    String javaDocZipName = name.substring(0, ind) + JAVADOC_ZIP_EXT;
                                    File javaDoc = new File(javadocDir, javaDocZipName);
                                    if (javaDoc.exists() && javaDoc.isFile()) {
                                        matchedJavaDoc = javaDoc;
                                    }
                                }
                            }
                        }
                    }
                    IClasspathEntry entry = null;
                    if (matchedJavaDoc != null) {
                        URI uri = matchedJavaDoc.toURI();
                        IClasspathAttribute attr = JavaCore.newClasspathAttribute("javadoc_location", "jar:" + uri.toASCIIString() + "!/");
                        entry = JavaCore.newLibraryEntry(path.append(name), null, null, null, new IClasspathAttribute[] { attr }, false);
                    } else if (javaDocUrlStr != null) {
                        try {
                            URL javaDocUrl = new URL(javaDocUrlStr);
                            URI uri = javaDocUrl.toURI();
                            IClasspathAttribute attr = JavaCore.newClasspathAttribute("javadoc_location", uri.toASCIIString());
                            entry = JavaCore.newLibraryEntry(path.append(name), null, null, null, new IClasspathAttribute[] { attr }, false);
                        } catch (Exception e) {
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.INFO, "Invalid javadoc URL " + javaDocUrlStr + " for " + javadocDir + "/" + name);
                            }
                        }
                    } else
                        entry = JavaCore.newLibraryEntry(path.append(name), null, null);

                    list.add(entry);
                }
            }
        }
    }

    private static String getJavadocUrl(File javadocDir, String jarName) {
        File javaDocUrlFile = getJavadocFile(javadocDir, jarName, JAVADOC_URL_EXT);
        if (javaDocUrlFile != null) {
            Properties urlProps = new Properties();
            FileUtil.loadProperties(urlProps, new Path(javaDocUrlFile.getAbsolutePath()));
            return urlProps.getProperty("URL");
        }
        return null;
    }

    private static File getJavadocFile(File javadocDir, String jarName, String extension) {
        if (jarName == null || !jarName.endsWith(".jar"))
            return null;
        String rootName = jarName.substring(0, jarName.length() - 4);
        File javadocFile = new File(javadocDir, rootName + extension);
        if (javadocFile.exists() && javadocFile.isFile()) {
            return javadocFile;
        }
        // Check for the javadoc file using the jar name minus the micro version.
        // See RTC work item 162563
        Matcher matcher = patternJarFullVersion.matcher(jarName);
        if (matcher.matches()) {
            int index = rootName.lastIndexOf('.');
            if (index > 0) {
                rootName = rootName.substring(0, index);
                javadocFile = new File(javadocDir, rootName + extension);
                if (javadocFile.exists() && javadocFile.isFile()) {
                    return javadocFile;
                }
            }
        }
        return null;
    }

    private static boolean isIncludedFolder(String s, ProjectPrefs prefs) {
        if (FOLDER_API.equals(s) || FOLDER_SPEC.equals(s))
            return true;
        if (FOLDER_TOOLS.equals(s) || FOLDER_SPI.equals(s))
            return false;

        if (FOLDER_THIRD_PARTY.equals(s))
            return !prefs.isExcludeThirdPartyAPI();
        else if (FOLDER_STABLE.equals(s))
            return !prefs.isExcludeStableAPI();
        else if (FOLDER_IBM.equals(s) || FOLDER_IBM_API.equals(s))
            return !prefs.isExcludeIBMAPI();
        return !prefs.isExcludeUnrecognized();
    }

    @Override
    public IClasspathEntry[] resolveClasspathContainer(final IProject project, IRuntime runtime) {

        WebSphereRuntimeTargetedProject wrtp = null;

        if (Activator.getInstance().getPreferenceUseDynamicClasspathContainer()) {
            wrtp = WebSphereRuntimeTargetedProjectRegistry.INSTANCE.getProject(project);

            if (wrtp == null) {
                wrtp = WebSphereRuntimeTargetedProjectRegistry.INSTANCE.addProject(project);
                // Run on the same thread, because the WebSphereRuntimeTargetedProject looks only for features based on facets, which
                // does not call any dangerous JDT methods, which can cause deadlocks and stack overflow errors.
                // If for some reason the following method calls JDT code that causes the classpath of this project to be resolved again,
                // then might need to set the parameter to true to run a separate job.
                wrtp.refresh(false);
            }
        }

        IPath runtimeLocation = runtime.getLocation();

        if (runtimeLocation == null)
            return new IClasspathEntry[0];

        // Check for empty classpath extension.
        for (ClasspathExtension ce : Activator.getInstance().getEmptyContainerExtensions()) {
            if (ce.supportsNature(project) || ce.containsSupportedFacet(project)) {
                return new IClasspathEntry[0];
            }
        }

        List<IClasspathEntry> list = new ArrayList<IClasspathEntry>(30);

        ProjectPrefs prefs = new ProjectPrefs(project);
        Set<String> jarsToSkip = null;

        if (wrtp != null) {
            // Protect the following code against any exception, because if an unchecked exception is thrown, the classpath container
            // will be empty. If we catch an exception, log it, and move on. This will cause that all the jars in FOLDER_DEV will be included.
            try {
                IClasspathEntry[] tmp = wrtp.getClasspathEntriesCache().getEntries();
                if (tmp.length > 0) {
                    return tmp;
                }
                jarsToSkip = wrtp.getConflictedJars();
            } catch (Exception e) {
                Trace.logError("Could not get list of jars to ignore for project " + project.getName(), e);
            }
        }

        findJars(runtimeLocation.append(FOLDER_DEV), prefs, list, false, jarsToSkip);

        // find jars from user-defined liberty features to be added to classpath
        // Do not pass a list of jars to ignore, because at this moment, user features do not provide
        // this information of conflicting features. At the moment, that information is hardcoded.
        findJars(runtimeLocation.append(Constants.USR_EXTENSION_FOLDER), prefs, list, true, null);

        // sort classpath entries
        IClasspathEntry[] entries = list.toArray(new IClasspathEntry[list.size()]);
        Arrays.sort(entries, new RuntimeClasspathEntriesComparator());
        //If duplicate feature entries exist (same feature but different version) - always select only latest version
        entries = filterDuplicateFeatures(entries);
        if (wrtp != null) {
            wrtp.getClasspathEntriesCache().setEntries(entries);
        }
        return entries;
    }

    /**
     * This will remove entries for features that have multiple versions, and only retain the latest version.
     * Assumptions are the list is already sorted
     *
     *
     * This method compares the last segment of the path of two <code>IClasspathEntry</code>s according to the following rules:
     * <ol>
     * <li>If both entries follow the pattern qualified.name.1.2.3_4.5.6.jar (where 1.2.3 is the spec version and 4.5.6 is the bundle version)
     * and qualified.name is equals, the spec version will be compared and the higher version will come first. If both version are equals, the bundle
     * version will be compared and the higher version will come first.</li>
     * <li>If both entries follow the pattern qualified.name_4.5.6.jar (where there is no spec version and 4.5.6 is the bundle version)
     * and qualified.name is equals, the bundle version will be compared and the higher version will come first.</li>
     * <li>If both entries follow the pattern qualified.name.1.2.3_4.5.6.jar or qualified.name_4.5.6.jar and qualified.name is NOT equals,
     * the qualified.name will be compared using <code>String.compareTo(String)</code> (alphabetical order).</li>
     * <li>If any of the entries follow a pattern qualified.name.1.2.3_4.5.6.jar or qualified.name_4.5.6.jar, and either the bundle version of
     * spec version does not follow an OSGi version pattern, both entries will be compared using the full path, using
     * <code>String.compareTo(String)</code> (alphabetical order).</li>
     * <li>If none of the entries follow a pattern qualified.name.1.2.3_4.5.6.jar or qualified.name_4.5.6.jar, both entries will be compared using the
     * full path, using <code>String.compareTo(String)</code> (alphabetical order).</li>
     * </ol>
     *
     * @param entries
     */
    public static IClasspathEntry[] filterDuplicateFeatures(IClasspathEntry[] entries) {

        ArrayList<IClasspathEntry> filteredEntries = new ArrayList<IClasspathEntry>();
        RuntimeClasspathEntry cpe, nextCpe;
        for (int j = 0; j < entries.length;) {
            boolean foundLatest = false;
            IClasspathEntry cp = entries[j];
            String cpePath = cp.getPath().lastSegment();
            synchronized (parsedEntriesCache) {
                if (!parsedEntriesCache.containsKey(cpePath)) {
                    cpe = new RuntimeClasspathEntry(cpePath);
                    parsedEntriesCache.put(cpePath, cpe);
                } else {
                    cpe = parsedEntriesCache.get(cpePath);
                }
            }
            int k = 1;
            IClasspathEntry latestVersion = cp;
            while (!foundLatest) {
                if (j + k == entries.length) {
                    latestVersion = cp;
                    foundLatest = true;
                    filteredEntries.add(latestVersion);
                } else {
                    IClasspathEntry nextCp = entries[j + k];
                    String nextCpePath = nextCp.getPath().lastSegment();
                    synchronized (parsedEntriesCache) {
                        if (!parsedEntriesCache.containsKey(nextCpePath)) {
                            nextCpe = new RuntimeClasspathEntry(nextCpePath);
                            parsedEntriesCache.put(nextCpePath, nextCpe);
                        } else {
                            nextCpe = parsedEntriesCache.get(nextCpePath);
                        }
                    }
                    if (cpe.getJarName().equals(nextCpe.getJarName())) {
                        if (cpe.isVersionGreater(nextCpe)) {
                            latestVersion = cp;
                        } else {
                            latestVersion = nextCp;
                        }
                    } else {
                        foundLatest = true;
                        filteredEntries.add(latestVersion);
                    }
                }
                k++; //Increment to next entry
            }
            j = (j + k - 1);
        }

        return filteredEntries.toArray(new IClasspathEntry[filteredEntries.size()]);

    }

    /**
     * This class is used to sort entries in the Liberty classpath container in alphabetical order
     */
    public static class RuntimeClasspathEntriesComparator implements Comparator<IClasspathEntry> {

        /** {@inheritDoc} */
        @Override
        public int compare(IClasspathEntry cp1, IClasspathEntry cp2) {
            String cp1Path = cp1.getPath().lastSegment();
            String cp2Path = cp2.getPath().lastSegment();

            return cp1Path.compareTo(cp2Path);

        }
    }

    private static class RuntimeClasspathEntry {
        private String jarName;
        private Version specVersion;
        private Version jarVersion;

        private static final String PATTERN_LIBERTY_JARS = "((\\.\\d+(\\.\\d+(\\.\\d+)?)?)?\\_\\d+(\\.\\d+(\\.\\d+)?)?\\.jar)$";
        //private static final String PATTERN_LIBERTY_JARS = "(([^\\w]\\d+(\\.\\d+(\\.\\d+)?)?)?\\_\\d+(\\.\\d+(\\.\\d+)?)?\\.jar)$";

        private static final Pattern patternLibertyJars = Pattern.compile(PATTERN_LIBERTY_JARS);

        public RuntimeClasspathEntry(String classpathEntryName) {
            if (classpathEntryName == null)
                throw new IllegalArgumentException();

            Matcher matcher = patternLibertyJars.matcher(classpathEntryName);
            if (matcher.find()) {
                jarName = classpathEntryName.substring(0, matcher.start());
                String versionStr = classpathEntryName.substring(matcher.start() + 1, classpathEntryName.length() - 4);
                int underscoreLocation = versionStr.indexOf('_');
                if (underscoreLocation != -1) {
                    specVersion = Version.create(versionStr.substring(0, underscoreLocation));
                    jarVersion = Version.create(versionStr.substring(underscoreLocation + 1));
                } else {
                    jarVersion = Version.create(versionStr);
                }
            } else {
                jarName = classpathEntryName;
            }
        }

        public String getJarName() {
            return jarName;
        }

        public Version getSpecVersion() {
            return specVersion;
        }

        public Version getJarVersion() {
            return jarVersion;
        }

        public boolean isVersionGreater(RuntimeClasspathEntry other) {

            int compareResult;
            if (getSpecVersion() != null && other.getSpecVersion() != null) {
                compareResult = (getSpecVersion().compareTo(other.getSpecVersion()));
                if (compareResult == 0) {
                    if (getJarVersion() != null && other.getJarVersion() != null)
                        return (getJarVersion().compareTo(other.getJarVersion()) > 0) ? true : false;

                    return false;
                }
                return (compareResult > 0) ? true : false;

            }
            if (getJarVersion() != null && other.getJarVersion() != null)
                return (getJarVersion().compareTo(other.getJarVersion()) > 0) ? true : false;

            return false;
        }

    }

}
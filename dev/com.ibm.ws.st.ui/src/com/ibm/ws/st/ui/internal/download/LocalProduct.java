/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.ManifestElement;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.ISource;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * An archive (file system) product
 */
public class LocalProduct extends AbstractProduct {

    private static final String CORE_RUNTIME_MARKER = "wlp/lib/versions/WebSphereApplicationServer.properties";
    private static final String ASSET_MANAGER = "wlp/bin/installUtility.bat";
    private static final String APPLIES_TO = "Applies-To";
    private static final String PACKAGE_TYPE = "Archive-Content-Type";
    private static final String PROVIDE_FEATURE = "Provide-Feature";
    private static final String SUBSYTEM_APPLIES_TO = "IBM-AppliesTo";
    private static final String SUBSYTEM_TYPE = "Subsystem-Type";
    private static final String SUBSYTEM_SYMBOLIC_NAME = "Subsystem-SymbolicName";
    private static final String SUBSYTEM_CONTENT = "Subsystem-Content";
    private static final String DEFAULT_PRODUCT_ID = "com.ibm.websphere.appserver";

    private final LocalSource source;
    private License license;

    public static LocalProduct create(String path) {
        final File archiveFile = new File(path);
        if (!archiveFile.exists()) {
            return null;
        }
        if (isCoreLibertyArchive(archiveFile)) {
            return new LocalProduct(archiveFile, getCoreProperties(archiveFile));
        }

        return new LocalProduct(archiveFile, getAddOnProperties(archiveFile));
    }

    private static boolean isCoreLibertyArchive(File archiveFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(archiveFile);
            return zipFile.getEntry(CORE_RUNTIME_MARKER) != null;
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem reading archive: " + archiveFile, e);
            return false;
        } finally {
            try {
                if (zipFile != null)
                    zipFile.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static Map<String, String> getCoreProperties(File archiveFile) {
        ZipFile zipFile = null;
        InputStream is = null;
        try {
            zipFile = new ZipFile(archiveFile);
            ZipEntry entry = zipFile.getEntry(ASSET_MANAGER);
            String onPremise = entry == null ? "false" : "true";

            entry = zipFile.getEntry(CORE_RUNTIME_MARKER);
            if (entry != null) {
                is = zipFile.getInputStream(entry);
                Properties prop = new Properties();
                prop.load(is);

                Map<String, String> coreProp = new HashMap<String, String>();
                String provideFeature = getCoreFeatures(archiveFile);
                coreProp.put(IProduct.PROP_NAME, archiveFile.getName());
                coreProp.put(IProduct.PROP_DESCRIPTION, archiveFile.getAbsolutePath());
                coreProp.put(IProduct.PROP_TYPE, "install");
                coreProp.put(IProduct.PROP_PRODUCT_ID, prop.getProperty("com.ibm.websphere.productId"));
                coreProp.put(IProduct.PROP_PRODUCT_VERSION, prop.getProperty("com.ibm.websphere.productVersion"));
                coreProp.put(IProduct.PROP_PRODUCT_INSTALL_TYPE, prop.getProperty("com.ibm.websphere.productInstallType"));
                coreProp.put(IProduct.PROP_PRODUCT_EDITION, prop.getProperty("com.ibm.websphere.productEdition"));
                coreProp.put(IProduct.PROP_PRODUCT_LICENSE_TYPE, prop.getProperty("com.ibm.websphere.productLicenseType", "ILAN"));
                coreProp.put(IProduct.PROP_ON_PREMISE, onPremise);
                if (provideFeature != null)
                    coreProp.put("provideFeature", provideFeature);
                return coreProp;
            }
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem reading archive: " + archiveFile, e);
        } finally {
            try {
                if (is != null)
                    is.close();

                if (zipFile != null)
                    zipFile.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private static String getCoreFeatures(File archiveFile) {
        String archivePath = archiveFile.getAbsolutePath();
        String provideFeature = null;
        Attributes manAttrs;
        try {
            manAttrs = FileUtil.getJarManifestAttributes(archivePath);
            if (manAttrs != null)
                provideFeature = manAttrs.getValue(PROVIDE_FEATURE);
        } catch (IOException e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to get provide features properties in " + archivePath, e);
            }
        }

        // Manifest has no provide feature property, so we have to extract the
        // information from the various feature.mf files
        try {
            if (provideFeature == null)
                provideFeature = getPublicFeatures(archiveFile);
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to get installed features in " + archivePath, e);
            }
        }

        return provideFeature;
    }

    private static String getPublicFeatures(File archiveFile) throws ZipException, IOException {
        StringBuilder sb = new StringBuilder();
        ZipFile jar = null;
        try {
            boolean isMultiple = false;
            jar = new ZipFile(archiveFile);
            Enumeration<? extends ZipEntry> enu = jar.entries();
            while (enu.hasMoreElements()) {
                ZipEntry entry = enu.nextElement();
                String name = entry.getName().toLowerCase();
                if (entry.isDirectory() || !name.startsWith("wlp/lib/features") || !name.endsWith(".mf"))
                    continue;

                InputStream in = null;
                try {
                    in = jar.getInputStream(entry);
                    Map<String, String> props = new HashMap<String, String>();
                    FileUtil.readSubsystem(in, props);

                    // Parse the symbolic name value
                    String content = props.get("Subsystem-SymbolicName");

                    // Per the java docs, the header name we are passing is
                    // only specified to provide error messages when the
                    // header value is invalid
                    ManifestElement[] elements = ManifestElement.parseHeader("Subsystem-SymbolicName", content);
                    if (elements != null) {
                        String visibility = elements[0].getAttribute("visibility");
                        if (visibility == null) {
                            // try directive map
                            visibility = elements[0].getDirective("visibility");
                        }
                        if ("public".equals(visibility)) {
                            if (isMultiple)
                                sb.append(',');
                            else
                                isMultiple = true;

                            sb.append(elements[0].getValue());
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not process manifest file" + entry.getName(), e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Trouble closing zip file", e);
                }
            }
        }

        return (sb.length() == 0) ? null : sb.toString();
    }

    private static Map<String, String> getAddOnProperties(File archiveFile) {
        String archivePath = archiveFile.getAbsolutePath();
        String appliesTo = null;
        String type = null;
        String provideFeature = null;
        String requireFeature = null;
        try {
            Attributes manAttrs = FileUtil.getJarManifestAttributes(archivePath);
            if (manAttrs != null) {
                appliesTo = manAttrs.getValue(APPLIES_TO);
                type = manAttrs.getValue(PACKAGE_TYPE);
                provideFeature = manAttrs.getValue(PROVIDE_FEATURE);
            } else {
                // Check to see if we are dealing with a feature archive
                Map<String, String> props = FileUtil.getJarSubSystemProperties(archivePath);
                if (props != null) {
                    appliesTo = props.get(SUBSYTEM_APPLIES_TO);
                    if (appliesTo == null) {
                        // There was no header, so use default product id
                        appliesTo = DEFAULT_PRODUCT_ID;
                    }
                    if ("osgi.subsystem.feature".equals(props.get(SUBSYTEM_TYPE))) {
                        type = "feature";
                    }

                    // Parse the symbolic name value
                    String content = props.get(SUBSYTEM_SYMBOLIC_NAME);
                    // Per the java docs, the header name we are passing is
                    // only specified to provide error messages when the
                    // header value is invalid
                    ManifestElement[] elements = ManifestElement.parseHeader(SUBSYTEM_SYMBOLIC_NAME, content);
                    if (elements != null) {
                        provideFeature = elements[0].getValue();
                    }

                    content = props.get(SUBSYTEM_CONTENT);
                    elements = ManifestElement.parseHeader(SUBSYTEM_CONTENT, content);
                    if (elements != null) {
                        StringBuilder sb = new StringBuilder();
                        boolean isFirstTime = true;
                        for (ManifestElement e : elements) {
                            if ("osgi.subsystem.feature".equals(e.getAttribute("type"))) {
                                if (!isFirstTime)
                                    sb.append(',');

                                sb.append(e.getValue());
                                isFirstTime = false;
                            }
                        }

                        if (sb.length() > 0)
                            requireFeature = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to get header properties in " + archivePath, e);
            }
        }

        Map<String, String> addOnProp = new HashMap<String, String>();
        addOnProp.put(IProduct.PROP_NAME, archiveFile.getName());
        addOnProp.put(IProduct.PROP_DESCRIPTION, archivePath);
        if (appliesTo != null) {
            addOnProp.put(IProduct.PROP_APPLIES_TO, appliesTo);
        }
        if (type != null) {
            addOnProp.put(IProduct.PROP_TYPE, type);
        }
        if (provideFeature != null) {
            addOnProp.put("provideFeature", provideFeature);
        }
        if (requireFeature != null) {
            addOnProp.put("requireFeature", requireFeature);
        }

        return addOnProp;
    }

    private LocalProduct(File file, Map<String, String> properties) {
        super(properties);
        source = new LocalSource(file);
    }

    @Override
    public ProductType getProductType() {
        return ProductType.LOCAL_TYPE;
    }

    @Override
    public long getSize() {
        return source.getSize();
    }

    @Override
    public License getLicense(IProgressMonitor monitor) throws IOException {
        if (license == null) {
            license = DownloadHelper.getLicense(source.getFile());
        }

        return license;
    }

    @Override
    public String getSiteName() {
        return "Local";
    }

    @Override
    public ISource getSource() {
        return source;
    }

    protected static class LocalSource implements ISource {
        private final File file;

        protected LocalSource(File file) {
            this.file = file;
        }

        @Override
        public String getLocation() {
            return file.getAbsolutePath();
        }

        @Override
        public long getSize() {
            if (file.exists())
                return file.length();
            return -1;
        }

        protected File getFile() {
            return file;
        }
    }
}

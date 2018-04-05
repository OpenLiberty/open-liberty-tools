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
package com.ibm.ws.st.ui.internal.download;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.repository.AbstractInstaller;
import com.ibm.ws.st.core.internal.repository.FeatureInstaller;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRemoteSource;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class DownloadHelper {
    private static final int BUFFER_SIZE = 8192;

    private static ExtractorInstaller defaultInstaller = new ExtractorInstaller();
    private static FeatureInstaller featureInstaller = new FeatureInstaller();
    private static ConfigSnippetInstaller configSnippetInstaller = new ConfigSnippetInstaller();

    private static final String[] SHARED_FOLDERS = new String[] {
                                                                  "usr/shared/apps", "usr/shared/config", "usr/shared/resources"
    };

    private static final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
    static {
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(1);
    }

    protected static final Pattern RUNTIME_FILE_NAME_PATTERN = Pattern.compile("wlp-developers-\\d+\\.\\d\\.\\w+\\.\\w+\\.jar");

    static AbstractInstaller getInstaller(IProduct product) {
        if (product.getType() == IProduct.Type.FEATURE)
            return featureInstaller;

        if (product.getType() == IProduct.Type.CONFIG_SNIPPET)
            return configSnippetInstaller;

        return defaultInstaller;
    }

    /**
     * Returns the given size in a formatted string.
     *
     * @param size the size in bytes
     */
    public static String getSize(long size) {
        return getSize(size, numberFormat);
    }

    /**
     * Returns the given size in a formatted string.
     *
     * @param size the size in bytes
     */
    public static String getSize(long size, NumberFormat f) {
        float s = size / 1024f;
        if (s < 1024)
            return f.format(s) + " KB";
        return f.format(s / 1024f) + " MB";
    }

    public static Map<IProduct, IStatus> install(List<IProduct> installList, List<PasswordAuthentication> authList, Map<String, Object> settings, IProgressMonitor monitor2) {
        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        // calculate for progress monitor
        Map<IProduct, IStatus> result = new LinkedHashMap<IProduct, IStatus>();

        try {
            monitor.beginTask(Messages.jobInstallingRuntime, 50 * installList.size());

            for (int i = 0; i < installList.size(); ++i) {
                IProduct p = installList.get(i);
                AbstractInstaller installer = getInstaller(p);
                IStatus status = installer.install(p, (authList == null) ? null : authList.get(i), settings, new SubProgressMonitor(monitor, 50));
                result.put(p, status);
            }
        } finally {
            monitor.done();
        }

        return result;
    }

    protected static void unzip(List<File> files, IPath path, IProgressMonitor monitor2) throws CoreException {
        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        long totalSize = 0L;
        for (File file : files) {
            totalSize += (int) (file.length() / 10240); // 10 KB for a monitor unit
        }
        monitor.beginTask(Messages.jobInstallingRuntime, (int) totalSize);
        try {
            for (File file : files) {
                try {
                    long length = file.length();
                    unzip(file, path, length, new SubProgressMonitor(monitor, (int) (length / 10240)), null);
                } catch (Exception e) {
                    if (monitor.isCanceled())
                        return;
                    Trace.logError("Error unzipping file: " + file.getName(), e);
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorInstallingRuntimeEnvironment, path.toOSString() + "\n\n")
                                                                                              + e.getLocalizedMessage(), e));
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static void download(IProduct product, File temp, IProgressMonitor monitor) throws IOException {
        byte[] BUFFER = new byte[BUFFER_SIZE];
        FileOutputStream out = null;
        int currentWorked = 0;
        int processedSize = 0;

        try {
            InputStream in = ((IRemoteSource) product.getSource()).getInputStream();
            // Make sure the input stream object is not null
            if (in == null)
                throw new IOException(NLS.bind(Messages.errorInvalidInputStream, product.getName()));

            long archiveSize = product.getSize();
            String msg = NLS.bind(Messages.taskDownloading, new Object[] { "{0}", getSize(archiveSize) });
            monitor.beginTask(NLS.bind(msg, "0"), 1000);

            out = new FileOutputStream(temp);
            int r = in.read(BUFFER);
            while (r >= 0) {
                out.write(BUFFER, 0, r);
                processedSize += r;

                int newWorked = (int) (1000 * (float) processedSize / archiveSize);
                if (newWorked - currentWorked > 3) {
                    String displayName = product.getName() == null ? "" : product.getName() + " ";
                    monitor.subTask(displayName + NLS.bind(msg, getSize(processedSize)));
                    int deltaWork = newWorked - currentWorked;
                    currentWorked += deltaWork;
                    monitor.worked(deltaWork);
                }

                if (monitor.isCanceled())
                    break;
                r = in.read(BUFFER);
            }
        } catch (IOException e) {
            Trace.logError("Failed to download runtime installation files", e);
            throw new IOException(Messages.errorDownloadingInstallFiles + "\n\n" + e.getLocalizedMessage(), e);
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                // ignore
            }
            monitor.done();
        }
    }

    @SuppressWarnings("serial")
    protected static void unzip(File srcFile, IPath path, long totalSize, final IProgressMonitor monitor, String fileDisplayName) throws IOException {
        try {
            String fileNameToDisplay = fileDisplayName == null ? srcFile.getName() : fileDisplayName;
            String extractorClass = getExtractorClassName(srcFile);
            if (extractorClass == null) {
                // We have an 8.5.0 stream binary so we need to use the legacy uninstaller
                legacyUnzip(srcFile, path, totalSize, monitor);
            } else {
                // We found the header so we can try using the new version to install.
                Map<String, Object> extractor = createExtractor(srcFile, extractorClass);
                extractor.put("license.accept", Boolean.TRUE);
                extractor.put("install.dir", path.toFile());
                @SuppressWarnings("boxing")
                int size = (Integer) extractor.get("install.monitor.size");
                String msg = NLS.bind(Messages.taskUncompressing, new Object[] { "{0}", getSize(totalSize) });
                monitor.beginTask(NLS.bind(msg, "0"), size);
                monitor.subTask(fileNameToDisplay);
                extractor.put("install.monitor", new ArrayList<Object>() {
                    @Override
                    public boolean add(Object e) {
                        monitor.worked(1);
                        return !!!monitor.isCanceled();
                    }
                });
                @SuppressWarnings("boxing")
                int rc2 = (Integer) extractor.get("install.code");
                if (rc2 != 0) {
                    msg = (String) extractor.get("install.error.message");
                    throw new IOException(msg);
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * @param srcFile
     * @return
     * @throws IOException
     */
    static String getExtractorClassName(File srcFile) throws IOException {
        JarFile jarFile = new JarFile(srcFile);
        Manifest man = jarFile.getManifest();
        String extractorClass = null;
        if (man != null) {
            extractorClass = man.getMainAttributes().getValue("Map-Based-Self-Extractor");
        }
        jarFile.close();
        return extractorClass;
    }

    /**
     * @param srcFile
     * @param extractorClass
     * @return
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static Map<String, Object> createExtractor(File srcFile, String extractorClass) throws IOException {
        try {
            ClassLoader loader = new URLClassLoader(new URL[] { srcFile.toURI().toURL() }, null);
            @SuppressWarnings("unchecked")
            Class<Map<String, Object>> clazz = (Class<Map<String, Object>>) loader.loadClass(extractorClass);
            Map<String, Object> extractor = clazz.newInstance();
            Integer result = (Integer) extractor.put("install.version", Integer.valueOf(1));
            if (result == null) {
                throw new IOException(Messages.unsupportedInstaller);
            }
            @SuppressWarnings("boxing")
            int rc = (Integer) extractor.get("installer.init.code");
            if (rc == 0) {
                return extractor;
            }
            throw new IOException((String) extractor.get("installer.init.error.message"));
        } catch (Exception e) {
            throw new IOException(Messages.unexpectedInstallerError);
        }
    }

    /**
     * Unzip the input stream into the given path.
     *
     * @param srcFile
     * @param path
     * @param totalSize
     * @param monitor
     * @throws IOException
     */
    private static void legacyUnzip(File srcFile, IPath path, long totalSize, IProgressMonitor monitor) throws IOException {
        byte[] BUFFER = new byte[BUFFER_SIZE];
        FileInputStream in = null;
        ZipInputStream zin = null;
        int currentWorked = 0;
        int processedSize = 0;

        try {
            String msg = NLS.bind(Messages.taskUncompressing, new Object[] { "{0}", getSize(totalSize) });
            monitor.beginTask(NLS.bind(msg, "0"), 1000);

            if (!path.toFile().exists() && !path.toFile().mkdirs())
                throw new IOException("Could not create folder: " + path);

            in = new FileInputStream(srcFile);
            BufferedInputStream bin = new BufferedInputStream(in);
            zin = new ZipInputStream(bin);
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                int index = name.indexOf("/");
                if (index >= 0)
                    name = name.substring(index + 1);

                if (entry.isDirectory()) {
                    if (!path.append(name).toFile().exists() && !path.append(name).toFile().mkdirs())
                        throw new IOException("Could not create folder: " + path.append(name));
                } else {
                    FileOutputStream fout = null;
                    try {
                        // check if parent exists (some zips don't include directory entries)
                        File f = path.append(name).toFile();
                        File parent = f.getParentFile();
                        if (!parent.exists() && !parent.mkdirs())
                            throw new IOException("Could not create folder: " + path.append(name));

                        fout = new FileOutputStream(f);
                        int r = zin.read(BUFFER);
                        while (r >= 0) {
                            fout.write(BUFFER, 0, r);
                            r = zin.read(BUFFER);
                        }
                    } finally {
                        try {
                            if (fout != null)
                                fout.close();
                        } catch (IOException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Trouble closing output stream", e);
                        }
                    }
                }

                processedSize += entry.getCompressedSize();

                int newWorked = (int) (1000 * (float) processedSize / totalSize);
                if (newWorked - currentWorked > 3) {
                    monitor.subTask(srcFile.getName() + " " + NLS.bind(msg, getSize(processedSize)));
                    int deltaWork = newWorked - currentWorked;
                    currentWorked += deltaWork;
                    monitor.worked(deltaWork);
                }

                zin.closeEntry();
                entry = zin.getNextEntry();

                if (monitor.isCanceled())
                    return;
            }

            // create shared folders that aren't in the jar
            if (srcFile.getName().endsWith(".jar")) {
                File f = path.toFile();
                for (int i = 0; i < SHARED_FOLDERS.length; i++) {
                    File sharedFile = new File(f, SHARED_FOLDERS[i]);
                    if (!sharedFile.exists() && !sharedFile.mkdirs())
                        throw new IOException("Could not create folder: " + sharedFile.getAbsolutePath());
                }
            }

            // set executable bit in /bin folder
            File binFolder = path.append("bin").toFile();
            File[] scripts = binFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.contains(".");
                }
            });
            if (scripts != null && scripts.length > 0) {
                for (File s : scripts)
                    s.setExecutable(true);
            }
        } finally {
            try {
                if (zin != null)
                    zin.close();
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Trouble closing zip input stream", e);
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Trouble closing input stream", e);
            }
            monitor.done();
        }
    }

    public static List<File> getArchives() {
        List<File> folders = getFolders();
        List<File> buildsFound = new ArrayList<File>();

        for (File folderToSearch : folders) {
            File[] files = folderToSearch.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return RUNTIME_FILE_NAME_PATTERN.matcher(name).find();
                }
            });

            if (files != null)
                buildsFound.addAll(Arrays.asList(files));
        }

        if (buildsFound.size() > 1) {
            Collections.sort(buildsFound, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o2.getName().compareTo(o1.getName());
                }
            });
        }

        return buildsFound;
    }

    private static List<File> getFolders() {
        List<File> folders = new ArrayList<File>();

        String osName = System.getProperty("os.name");
        String userHome = System.getProperty("user.home");

        folders.add(new File(userHome));

        if (osName != null) {
            osName = osName.trim().toLowerCase();
            if (osName.contains("win")) {
                folders.add(new File(userHome, "Desktop"));
                folders.add(new File(userHome, "My Documents/Downloads"));
            } else if (osName.contains("mac")) {
                folders.add(new File(userHome, "Desktop"));
                folders.add(new File(userHome, "Downloads"));
            } else {
                folders.add(new File(userHome, "tmp"));
                folders.add(new File(System.getProperty("java.io.tmpdir")));
            }
        }

        return folders;
    }

    /**
     * Returns the license, or <code>null</code> if the archive does not have a license.
     */
    public static License getLicense(File archiveFile) throws IOException {
        String extractorClass = getExtractorClassName(archiveFile);
        if (extractorClass == null) {
            String licenseText = getLegacyLicense(archiveFile);
            if (licenseText == null) {
                return null;
            }

            return new License(null, licenseText);
        }

        // We found the header so we can try using the new version to install.
        Map<String, Object> extractor = createExtractor(archiveFile, extractorClass);
        Reader r = (Reader) extractor.get("license.agreement");

        // We do not have a license, so we just return null
        if (r == null) {
            return null;
        }

        String licenseName = (String) extractor.get("license.name");
        StringBuilder license = new StringBuilder();
        license.append("<HTML>\n<HEAD>\n<META HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset=UTF-8\"/>\n"
                       + "<TITLE>Software License</TITLE>\n</HEAD>\n<BODY>\n");

        BufferedReader in = null;
        try {
            in = new BufferedReader(r);
            String s = in.readLine();
            while (s != null) {
                license.append(s + "<br>\n");
                s = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
            r.close();
        }

        license.append("<br>\n");

        r = (Reader) extractor.get("license.info");
        // We have a license info, so add its contents
        if (r != null) {
            in = null;
            try {
                in = new BufferedReader(r);
                String s = in.readLine();
                while (s != null) {
                    license.append(s + "<br>\n");
                    s = in.readLine();
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                r.close();
            }
        }

        license.append("</BODY>\n</HTML>");

        return new License(licenseName, license.toString());
    }

    private static String getLegacyLicense(File archiveFile) throws IOException {
        BufferedReader in = null;
        ZipFile zipFile = null;
        try {
            // To provide language neutral licenses we need to load the license for the locale e.g. zh_TW
            // and the language e.g. zh and English in case the language doesn't have a translation e.g. tlh
            zipFile = new ZipFile(archiveFile);
            Locale locale = Locale.getDefault();
            String[] filesToFind = new String[] { "LA_" + locale, "LA_" + locale.getLanguage(), "LA_" + Locale.ENGLISH.getLanguage() };
            for (int i = 0; i < filesToFind.length; i++) {
                ZipEntry entry = zipFile.getEntry("wlp/lafiles/" + filesToFind[i]);
                if (entry != null) {
                    in = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), "UTF-16"));

                    String s = in.readLine();
                    StringBuilder license = new StringBuilder();
                    while (s != null) {
                        license.append(s + "\r\n");
                        s = in.readLine();
                    }

                    return license.toString();
                }
            }
            return null;
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not get license", e);
            throw e;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                // ignore
            }
            try {
                if (zipFile != null)
                    zipFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Check is SSL is working by trying to create a simple socket.
     *
     * @return <code>true</code> if SSL is working, and <code>false</code>
     *         if it fails due to missing classes.
     */
    public static boolean isSSLWorking() {
        try {
            SSLSocketFactory.getDefault().createSocket();
        } catch (SocketException e) {
            if (e.getCause() instanceof ClassNotFoundException)
                return false;
        } catch (IOException e) {
            // ignore
        }
        return true;
    }

    /**
     * Validates that the given folder exists as a folder on disk and is empty. Returns null if the folder is valid, and
     * returns an error message or "" (folder in empty) if the folder is invalid.
     *
     * @param folder
     * @return <code>null</code> if the folder is valid, and a non-null error otherwise
     */
    public static String validateTargetRuntimeLocation(String folder) {
        if (folder == null || folder.trim().isEmpty())
            return "";

        IPath path = new Path(folder);
        if (!path.toFile().isAbsolute()) {
            return Messages.errorInvalidFolder;
        } else if (path.toFile().exists()) {
            if (path.toFile().isFile())
                return Messages.errorInvalidFolder;

            String[] s = path.toFile().list();
            if (s != null && s.length > 0)
                return Messages.errorFolderNotEmpty;
        }

        if (path.hasTrailingSeparator())
            return com.ibm.ws.st.core.internal.Messages.errorInstallDirTrailingSlash;

        return null;
    }

    static String readFully(InputStream in, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding));
        StringBuilder builder = new StringBuilder();

        for (String line; (line = reader.readLine()) != null;) {
            builder.append(line);
            builder.append('\r');
            builder.append('\n');
        }

        reader.close();
        in.close();

        return builder.toString();
    }

    public static IRuntimeInfo getRuntimeCore(final IRuntime runtime) {
        final WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        final List<IRuntimeInfo.IProduct> productList = new ArrayList<IRuntimeInfo.IProduct>();
        if (wsRuntime != null) {
            List<IPath> paths = wsRuntime.getRuntimePropertiesPaths();
            for (IPath path : paths) {
                Properties prop = new Properties();
                FileUtil.loadProperties(prop, path);
                productList.add(createProduct(prop));
            }
        }

        if (productList.isEmpty()) {
            Properties prop = new Properties();
            productList.add(createProduct(prop));
        }

        return new IRuntimeInfo() {
            @Override
            public List<com.ibm.ws.st.core.internal.repository.IRuntimeInfo.IProduct> getProducts() {
                return productList;
            }

            @Override
            public String getVersion() {
                return productList.get(0).getProductVersion();
            }

            @Override
            public String getPrimaryProductId() {
                return productList.get(0).getProductId();
            }

            @Override
            public List<String> getInstalledFeatures() {
                if (wsRuntime == null) {
                    return null;
                }
                return FeatureList.getSymbolicNameFeatures(false, wsRuntime);
            }

            @Override
            public IPath getLocation() {
                return runtime.getLocation();
            }

            @Override
            public boolean isOnPremiseSupported() {
                if (wsRuntime == null) {
                    return false;
                }
                return wsRuntime.isOnPremiseSupported();
            }
        };
    }

    private static IRuntimeInfo.IProduct createProduct(final Properties prop) {
        return new IRuntimeInfo.IProduct() {
            @Override
            public String getProductId() {
                return prop.getProperty(Constants.RUNTIME_PROP_PRODUCT_ID);
            }

            @Override
            public String getProductVersion() {
                return prop.getProperty(Constants.RUNTIME_PROP_PRODUCT_VERSION);
            }

            @Override
            public String getProductEdition() {
                return prop.getProperty(Constants.RUNTIME_PROP_PRODUCT_EDITION);
            }

            @Override
            public String getProductInstallType() {
                return prop.getProperty(Constants.RUNTIME_PROP_PRODUCT_INSTALL_TYPE);
            }

            @Override
            public String getProductLicenseType() {
                return prop.getProperty(Constants.RUNTIME_PROP_PRODUCT_LICENSE_TYPE);
            }
        };
    }
}
/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.common.core.ext.internal.Activator;
import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;

/**
 * Utility class for file operations.
 */
public class FileUtil {
    private static final int BUFFER = 65536;
    private static byte[] buf = null; // TODO - not thread-safe

    public static IStatus move(IPath from, IPath to) throws IOException {
        buf = new byte[BUFFER];
        InputStream in = null;
        OutputStream out = null;

        try {
            File fromFile = from.toFile();
            in = new FileInputStream(fromFile);

            File toFile = to.toFile();
            out = new FileOutputStream(toFile);

            int avail = in.read(buf);
            while (avail > 0) {
                out.write(buf, 0, avail);
                avail = in.read(buf);
            }

            in.close();
            in = null;
            out.close();
            out = null;

            long ts = fromFile.lastModified();
            if (ts != IResource.NULL_STAMP && ts != 0) {
                if (!toFile.setLastModified(ts)) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not set last modified on " + toFile.getAbsolutePath());
                }
            }

            if (!fromFile.delete())
                return new Status(IStatus.WARNING, Activator.PLUGIN_ID, NLS.bind(Messages.warningFileDelete, from.toOSString()));
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            buf = null;
        }
        return Status.OK_STATUS;
    }

    public static IStatus copy(URL from, IPath to) throws IOException {
        buf = new byte[BUFFER];
        InputStream in = null;
        OutputStream out = null;

        try {
            URLConnection urlConn = from.openConnection();
            in = urlConn.getInputStream();

            File toFile = to.toFile();
            out = new FileOutputStream(toFile);

            int avail = in.read(buf);
            while (avail > 0) {
                out.write(buf, 0, avail);
                avail = in.read(buf);
            }

            in.close();
            in = null;
            out.close();
            out = null;

            long ts = urlConn.getLastModified();
            if (ts != IResource.NULL_STAMP && ts != 0) {
                if (!toFile.setLastModified(ts)) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not set last modified on " + toFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            buf = null;
        }
        return Status.OK_STATUS;
    }

    public static void loadProperties(Properties properties, IPath filePath) {
        if (properties == null || filePath == null)
            return;
        InputStream in = null;
        try {
            in = new FileInputStream(filePath.toFile());
            properties.load(in);
        } catch (FileNotFoundException e) {
            // this is ok, the file has not been created yet
        } catch (IOException e) {
            Trace.logError("Could not read the properties file: " + filePath, e);
        } catch (Exception e) {
            Trace.logError("Error during processing the properties file: " + filePath, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not close the properties file: " + filePath, e);
                }
            }
        }
    }

    public static void saveCachedProperties(Properties properties, IPath filePath) {
        if (properties == null || filePath == null)
            return;
        if (!makeDir(filePath.removeLastSegments(1)) && Trace.ENABLED) {
            Trace.trace(Trace.WARNING, "Failed to create directory for properties file: " + filePath.toOSString(), null);
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(filePath.toString()));
            properties.store(out, null);
        } catch (Exception e) {
            Trace.logError("Can't write to the properties file:" + filePath, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Trace.logError("Can't close the properties file:" + filePath, e);
                }
            }
        }
    }

    public static String genModuleURICacheKey(IModule[] module) {
        StringBuilder key = new StringBuilder();
        if (module != null) {
            for (IModule m : module) {
                key.append(m.getName()).append('/');
            }
        }
        return key.toString();
    }

    /**
     * Removes the given directory.
     * If recursive is true, any files and subdirectories within
     * the directory will also be deleted; otherwise the
     * operation will fail if the directory is not empty.
     * IMPORTANT: use this method very carefully since
     * it will remove all the subdirectories of the given
     * directory. The first directory in the path cannot
     * be removed to prevent the whole driver to be removed.
     *
     * @param dir       java.lang.String
     * @param recursive boolean
     * @exception java.io.IOException
     */
    public static void deleteDirectory(String dir, boolean recursive) throws IOException {
        if (dir == null || dir.length() <= 0) {
            return;
        }

        // Safety feature. Prevent to remove directory from the root
        // of the drive, i.e. directory with less than 2 file separator.
        if ((new StringTokenizer(dir.replace(File.separatorChar, '/'), "/")).countTokens() < 2) {
            return;
        }

        File fp = new File(dir);
        if (!fp.exists() || !fp.isDirectory())
            throw new IOException("Directory does not exist: " + fp.toString());

        if (recursive) {
            // Remove the contents of the given directory before delete.
            String[] fileList = fp.list();
            if (fileList != null) {
                String curBasePath = dir + File.separator;
                for (int i = 0; i < fileList.length; i++) {
                    // Remove each file one at a time.
                    File curFp = new File(curBasePath + fileList[i]);
                    if (curFp.exists()) {
                        if (curFp.isDirectory()) {
                            // Remove the directory and sub directories;
                            deleteDirectory(dir + File.separator + fileList[i], recursive);
                        } else {
                            if (!curFp.delete() && Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Could not delete " + curFp.getName());
                        }
                    }
                }
            }
        }
        boolean isSuccess = fp.delete();

        if (!isSuccess) {
            throw new IOException("Directory cannot be removed.");
        }
    }

    public static boolean makeDir(IPath path) {
        boolean result = true;

        if (path != null) {
            try {
                File fp = path.toFile();
                if (!fp.exists() || !fp.isDirectory()) {
                    // Create the directory.
                    result = fp.mkdirs();
                }
            } catch (Exception e) {
                Trace.logError("Failed to create directory: " + path.toOSString(), e);
                result = false;
            }
        }
        return result;
    }

    public static String getJarManifestAttribute(String jarPath, String attribute) throws ZipException, IOException {
        if (jarPath != null && attribute != null) {
            Attributes mainAttr = getJarManifestAttributes(jarPath);
            if (mainAttr != null) {
                return mainAttr.getValue(attribute);
            }
        }
        return null;
    }

    public static Attributes getJarManifestAttributes(String jarPath) throws ZipException, IOException {
        if (jarPath != null) {
            File file = new File(jarPath);
            InputStream in = null;
            ZipFile jar = null;
            try {
                jar = new ZipFile(file);
                ZipEntry mf = jar.getEntry("META-INF/MANIFEST.MF");
                if (mf != null) {
                    in = jar.getInputStream(mf);
                    Manifest manifest = new Manifest(in);
                    return manifest.getMainAttributes();
                }
            } finally {
                closeQuietly(in);
                closeQuietly(jar);
            }
        }
        return null;
    }

    public static Map<String, String> getJarSubSystemProperties(String jarPath) throws ZipException, IOException {
        if (jarPath != null) {
            File file = new File(jarPath);
            InputStream in = null;
            ZipFile jar = null;
            try {
                jar = new ZipFile(file);
                ZipEntry mf = jar.getEntry("OSGI-INF/SUBSYSTEM.MF");
                if (mf != null) {
                    Map<String, String> props = new HashMap<String, String>();
                    in = jar.getInputStream(mf);
                    readSubsystem(in, props);
                    return props;
                }
            } finally {
                closeQuietly(in);
                closeQuietly(jar);
            }
        }
        return null;
    }

    public static void readSubsystem(InputStream is, Map<String, String> props) throws IOException {
        if (is == null) {
            return;
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$

            String nextLine = br.readLine();
            // null is end of stream
            while (nextLine != null) {
                // header name is everything up to the first ':'
                int colonI = nextLine.indexOf(':');
                if (colonI == -1 || colonI == 0) {
                    // missing header name, file is invalid, skip to next line
                    nextLine = br.readLine();
                    continue;
                }

                String hName = nextLine.substring(0, colonI);

                // The rest of the line plus any continuation lines are the value
                StringBuilder hValue = new StringBuilder();
                if (nextLine.length() > colonI + 1) {
                    String hvPart = nextLine.substring(colonI + 1);
                    hValue.append(hvPart.trim());
                }

                // read continuation lines
                nextLine = br.readLine();
                while (nextLine != null && nextLine.length() > 0 && nextLine.charAt(0) == ' ') {
                    hValue.append(nextLine.trim());
                    nextLine = br.readLine(); // the last read which causes this loop to exit populates nextLine with the next header
                }

                // Now we have the header name and value, so add it to the map
                props.put(hName, hValue.toString());
            }
        } finally {
            closeQuietly(br);
        }
    }

    public static boolean zipDirectory(File folderToCompress, String destinationZipFile) throws IOException {
        if (folderToCompress == null || destinationZipFile == null)
            return false;

        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            File zipFile = new File(destinationZipFile);
            if (folderToCompress.exists() && folderToCompress.isDirectory()) {
                fos = new FileOutputStream(zipFile);
                zos = new ZipOutputStream(fos);

                addToZipStream(zos, folderToCompress);

                zos.close();
                return true;
            }

        } finally {
            closeQuietly(zos);
            closeQuietly(fos);
        }

        return false;
    }

    /**
     * Equivalent to Closeable.close(), except any exceptions will be ignored. This is typically used in finally blocks.
     */
    private static void closeQuietly(Closeable stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException ex) {
            //ignore

        }
    }

    /**
     * Recursively add files to zip output stream
     */
    private static void addToZipStream(ZipOutputStream zos, File file) throws IOException {
        addToZipStream(zos, file, file.getAbsolutePath());
    }

    /**
     * Recursively adds files to a provided ZipOutputStream.
     *
     * @param zos  The ZipOutputStream object.
     * @param file A file or folder to be zipped.
     * @param root The root directory for the zip structure. Must be a parent of the provided {@code file}.
     * @throws IOException
     */
    private static void addToZipStream(ZipOutputStream zos, File file, String root) throws IOException {
        byte[] buf = new byte[8192];
        if (file.isFile()) {
            String absPath = file.getAbsolutePath();
            String entry = absPath.substring(root.length() + 1, absPath.length());
            entry = entry.replace("\\", "/");
            ZipEntry zipEntry = new ZipEntry(entry);
            zos.putNextEntry(zipEntry);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);

                int length;
                while ((length = fis.read(buf)) > 0) {
                    zos.write(buf, 0, length);
                }
                fis.close();
                zos.closeEntry();
            } finally {
                closeQuietly(fis);
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                // TODO investigate using java NIO APIs, in particular DirectoryStream.class which
                // may give better performance for iterating over files within a large directory
                // Note: it is only available since Java 1.7
                addToZipStream(zos, f, root);
            }
        }
    }

    /**
     * Determines if the path is an absolute path Windows or Unix system.
     * Windows: path starting with drive letter followed by ":\" or ":/" is an absolute path e.g. C:\temp or C:/temp
     * Unix: any path starting with "/" is an absolute path e.g. "/home/xx"
     *
     * @param path
     * @return true if path is absolute, false otherwise
     */
    public static boolean isAbsolutePath(String path) {
        String pattern = "^(?i)[a-z]:[\\\\/].*";
        return path.matches(pattern) || path.startsWith("/");
    }

    public static void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            if (Trace.ENABLED) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                Trace.trace(Trace.WARNING, "Unable to delete " + file.toString(), t);
            }
        }
    }

    /**
     * Get a sorted list of files in the given directory.
     *
     * @param dir        The directory containing the files.
     * @param ignoreCase Whether to ignore case when sorting.
     * @return The sorted list of files.
     */
    public static File[] getSortedFiles(File dir, final boolean ignoreCase) {
        try {
            if (dir == null || !dir.isDirectory()) {
                return new File[0];
            }

            File[] files = dir.listFiles(new FileFilter() {
                /** {@inheritDoc} */
                @Override
                public boolean accept(File file) {
                    return file.isFile();
                }
            });

            Arrays.sort(files, new Comparator<File>() {
                /** {@inheritDoc} */
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            return files;
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Could not get file list for directory: " + dir, e);
            }
            return new File[0];
        }
    }

    /**
     * This method returns a string without square brackets.
     *
     * @param host: String starting with [ or ending with ] or eclosed in [ ]
     * @return hostName without the square brackets [ ]
     */
    public static String getStringWithoutBrackets(String host) {

        String hostName = host;

        if (hostName.startsWith("["))
            hostName = hostName.substring(1, hostName.length());
        if (hostName.endsWith("]"))
            hostName = hostName.substring(0, hostName.length() - 1);

        return hostName;
    }

    /**
     * Get the canonical path for the given path
     *
     * @param path The path as an IPath
     * @return
     */
    public static IPath getCanonicalPath(IPath path) {
        return new Path(getCanonicalPath(path.toOSString()));
    }

    /**
     * Get the canonical path for the given path
     *
     * @param path The path as a String
     * @return
     */
    public static String getCanonicalPath(String path) {
        String canonicalPath = path;
        try {
            canonicalPath = (new File(path)).getCanonicalPath();
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to get the canonical path for: " + path, e);
            }
        }
        return canonicalPath;
    }
}
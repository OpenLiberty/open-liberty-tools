/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.ibm.ws.st.core.internal.Trace;

public class FileUtil {
    public static void copyFiles(String sourcePath, String targetPath) throws IOException {
        File source = new File(sourcePath);
        File dest = new File(targetPath);
        dest.mkdirs();
        File[] files = source.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                copyFiles(sourcePath + "/" + fileName, targetPath + "/" + fileName);
            } else {
                copyFile(sourcePath + "/" + fileName, targetPath + "/" + fileName);
            }
        }
    }

    public static void copyFile(String sourceFilename, String targetFilename) throws IOException {
        FileInputStream fin = null;
        FileOutputStream fout = null;
        BufferedInputStream bin = null;
        BufferedOutputStream bout = null;
        File fpSource = null;

        try {
            try {
                fin = new FileInputStream(sourceFilename);
            } catch (FileNotFoundException e1) {
                throw new IOException(sourceFilename + " does not exist!");
            }
            try {
                fout = new FileOutputStream(targetFilename);
            } catch (FileNotFoundException e1) {
                throw new IOException(targetFilename + " does not exist!");
            }
            bin = new BufferedInputStream(fin);
            bout = new BufferedOutputStream(fout);

            fpSource = new File(sourceFilename);

            int fileLength = (int) fpSource.length();

            byte byteBuff[] = new byte[fileLength];

            while (fileLength > 0 && bin.read(byteBuff, 0, fileLength) != -1)
                bout.write(byteBuff, 0, fileLength);
        } catch (IOException e2) {
            throw e2;
//            throw new IOException(
//                            "Error reading/writing files in test FileUtil!");
        } finally {
            try {
                if (bin != null)
                    bin.close();

                if (bout != null)
                    bout.close();
            } catch (IOException e3) {
                throw new IOException("Cannot close the files in test FileUtil!");
            }
        }
    }

    public static Document getDOM(String filePath) throws Exception {
        File file = new File(filePath);
        InputSource input = new InputSource(new FileInputStream(file));
        input.setSystemId(filePath);
        return getDOM(input);
    }

    public static Document getDOM(URL url) throws Exception {
        InputStream inputStream = url.openConnection().getInputStream();
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setSystemId(url.toString());
        return getDOM(inputSource);
    }

    public static Document getDOM(InputSource source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(source);
        return document;
    }

    public static void saveDOM(Document doc, String filePath) throws Exception {
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(filePath);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
    }

    public static void unzipWLPDriver(File srcFile, IPath path) throws IOException {
        final int BUFFER_SIZE = 8192;
        final String[] SHARED_FOLDERS = new String[] { "usr/shared/apps", "usr/shared/config", "usr/shared/resources" };

        byte[] BUFFER = new byte[BUFFER_SIZE];
        FileInputStream in = null;
        ZipInputStream zin = null;

        try {
            if (!path.toFile().exists() && !path.toFile().mkdirs())
                throw new IOException("Could not create folder: " + path);

            in = new FileInputStream(srcFile);
            BufferedInputStream bin = new BufferedInputStream(in);
            zin = new ZipInputStream(bin);
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (!name.equals("META-INF/MANIFEST.MF") && !name.startsWith("wlp/lib/extract")) {
                    int index = name.indexOf("/");
                    if (index >= 0)
                        name = name.substring(index + 1);

                    if (entry.isDirectory()) {
                        if (!path.append(name).toFile().exists() && !path.append(name).toFile().mkdirs())
                            throw new IOException("Could not create folder: " + path.append(name));
                    } else {
                        FileOutputStream fout = null;
                        try {
                            fout = new FileOutputStream(path.append(name).toFile());
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
                                e.printStackTrace();
                            }
                        }
                    }
                }

                zin.closeEntry();
                entry = zin.getNextEntry();

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
                e.printStackTrace();
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes the given directory.
     * If force is true, any files and subdirectories within
     * the directory will also be deleted; otherwise the
     * operation will fail if the directory is not empty.
     * IMPORTANT: use this method very carefully since
     * it will remove all the subdirectories of the given
     * directory. The first directory in the path cannot
     * be removed to prevent the whole driver to be removed.
     *
     * @param dir java.lang.String
     * @param force boolean
     * @exception java.io.IOException
     */
    public static void deleteDirectory(String dir, boolean force) throws IOException {
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

        if (force) {
            // Remove the contents of the given directory before delete.
            deleteDirectoryContents(fp);
        }
        boolean isSuccess = fp.delete();

        if (!isSuccess) {
            throw new IOException("Directory cannot be removed.");
        }
    }

    /**
     * Delete the contents of the given directory.
     *
     * @param dir
     * @throws IOException
     */
    public static void deleteDirectoryContents(String dir) throws IOException {
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

        deleteDirectoryContents(fp);
    }

    private static void deleteDirectoryContents(File dir) throws IOException {
        String[] fileList = dir.list();
        if (fileList != null) {
            String curBasePath = dir.getAbsolutePath() + File.separator;
            for (int i = 0; i < fileList.length; i++) {
                // Remove each file one at a time.
                File curFp = new File(curBasePath + fileList[i]);
                if (curFp.exists()) {
                    if (curFp.isDirectory()) {
                        // Remove the directory and sub directories;
                        deleteDirectory(dir.getAbsolutePath() + File.separator + fileList[i], true);
                    } else {
                        curFp.delete();
                    }
                }
            }
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

    public static boolean safeDelete(File f) {
        for (int i = 0; i < 25; i++) {
            if (!f.exists())
                return true;
            if (f.delete())
                return true;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return false;
    }

    public static void unZip(String zipFile, String outputFolder) throws IOException {

        File f = new File(outputFolder);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                String path = outputFolder + File.separator + ze.getName();

                if (ze.isDirectory()) {
                    File unzipFile = new File(path);
                    if (!unzipFile.isDirectory()) {
                        unzipFile.mkdirs();
                    }
                } else {
                    FileOutputStream fout = new FileOutputStream(path, false);
                    try {
                        for (int c = zin.read(); c != -1; c = zin.read()) {
                            fout.write(c);
                        }
                        zin.closeEntry();
                    } finally {
                        fout.close();
                    }
                }
            }
        } finally {
            zin.close();
        }

    }

}
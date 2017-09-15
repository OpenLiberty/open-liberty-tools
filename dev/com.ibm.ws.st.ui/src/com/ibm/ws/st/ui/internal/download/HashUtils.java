/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static final MessageDigest messagedigestMD5;
    static final MessageDigest messagedigestSHA256;

    static {
        try {
            messagedigestMD5 = MessageDigest.getInstance("MD5");
            messagedigestSHA256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            //should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate MD5 hash of a String
     *
     * @param str - the String to hash
     * @return the MD5 hash value
     */
    public static String getMD5String(String str) {
        return getHashString(str, messagedigestMD5);
    }

    /**
     * Calculate MD5 hash of a File
     *
     * @param file - the File to hash
     * @return the MD5 hash value
     * @throws IOException
     */
    public static String getFileMD5String(File file) throws IOException {
        return getFileHashString(file, messagedigestMD5);
    }

    /**
     * Calculate SHA-256 hash of a String
     *
     * @param str - the String to hash
     * @return the SHA-256 hash value
     */
    public static String getSHA256String(String str) {
        return getHashString(str, messagedigestSHA256);
    }

    /**
     * Calculate SHA-256 hash of a File
     *
     * @param file - the File to hash
     * @return the SHA-256 hash value
     * @throws IOException
     */
    public static String getFileSHA256String(File file) throws IOException {
        return getFileHashString(file, messagedigestSHA256);
    }

    private static String getHashString(String str, MessageDigest messagedigest) {
        messagedigest.update(str.getBytes());
        return byteArrayToHexString(messagedigest.digest());
    }

    private static String getFileHashString(File file, MessageDigest messagedigest) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fis.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, numRead);
            }
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (Exception e) {
                    // intentionally empty
                }
        }

        return byteArrayToHexString(messagedigest.digest());
    }

    private static String byteArrayToHexString(byte[] byteArray) {

        StringBuffer stringbuffer = new StringBuffer(2 * byteArray.length);
        for (int i = 0; i < byteArray.length; i++) {
            char upper = hexDigits[(byteArray[i] & 0xf0) >> 4];
            char lower = hexDigits[byteArray[i] & 0xf];
            stringbuffer.append(upper);
            stringbuffer.append(lower);
        }
        return stringbuffer.toString();

    }

}

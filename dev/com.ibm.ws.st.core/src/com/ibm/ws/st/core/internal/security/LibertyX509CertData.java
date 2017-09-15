/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal.security;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;

/**
 * Objects of this class contain detailed information about X.509 certificates.
 * The main purpose of an LibertyX509CertData object is to return the (mostly) human
 * readable attributes of a certificate.
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertData implements Iterable<LibertyX509CertData.Entry> {

    private final List<Entry> entries_ = new LinkedList<Entry>();

    //
    // Standard key usage assertions defined by RFC 3280
    // [http://www.ietf.org/rfc/rfc3280.txt] section 4.2.1.3.
    //
    private static final String[] KEY_USAGE = new String[] { "digitalSignature", //$NON-NLS-1$
                                                            "nonRepudiation", //$NON-NLS-1$
                                                            "keyEncipherment", //$NON-NLS-1$
                                                            "dataEncipherment", //$NON-NLS-1$
                                                            "keyAgreement", //$NON-NLS-1$
                                                            "keyCertSign", //$NON-NLS-1$
                                                            "cRLSign", //$NON-NLS-1$
                                                            "encipherOnly", //$NON-NLS-1$
                                                            "decipherOnly" //$NON-NLS-1$
    };

    //
    // Standard subject alternative names defined by RFC 3280
    // [http://www.ietf.org/rfc/rfc3280.txt] section 4.2.1.7.
    //
    private static final String[] ALTERNATIVE_NAMES = new String[] { "otherName", //$NON-NLS-1$
                                                                    "rfc822Name", //$NON-NLS-1$
                                                                    "dNSName", //$NON-NLS-1$
                                                                    "x400Address", //$NON-NLS-1$
                                                                    "directoryName", //$NON-NLS-1$
                                                                    "ediPartyName", //$NON-NLS-1$
                                                                    "uniformResourceIdentifier", //$NON-NLS-1$
                                                                    "iPAddress", //$NON-NLS-1$
                                                                    "registeredID" //$NON-NLS-1$
    };

    /**
     * Constructs a new certificate data object for the given certificate.
     * 
     * @param certificate The certificate to describe. This should be an
     *            X.509 certificate, however, this class will minimally handle other
     *            types of certificates. Passing in a null certificate is not recommended,
     *            but is tolerated. If the certificate is null, the certificate data object
     *            will still be constructed, but will return zero attributes.
     */
    public LibertyX509CertData(Certificate certificate) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificate=[" + certificate + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (certificate != null) {
            //
            // Public key information is available for any kind of certificate.
            // Retrieval of the encoded public key is commented out because it
            // is not currently rendered to the user in a useful format.
            //
            PublicKey publicKey = certificate.getPublicKey();
//			add(Messages.X509_CERT_LABEL_PUBLIC_KEY_ENCODED,getPublicKeyEncoded(publicKey));
            add(Messages.X509_CERT_LABEL_PUBLIC_KEY_FORMAT, getPublicKeyFormat(publicKey));
            add(Messages.X509_CERT_LABEL_PUBLIC_KEY_ALGORITHM, getPublicKeyAlgorithm(publicKey));
            //
            // Everything else of interest is unique to X.509 certificates.
            // Retrieval of the signature is commented out because it
            // is not currently rendered to the user in a useful format.
            // Retrieval of the basic constraints is commented out because
            // a private "getBasicConstraints()" method is not implemented.
            //
            if (certificate instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certificate;
                add(Messages.X509_CERT_LABEL_ISSUER_DN, cert.getIssuerX500Principal().getName());
                add(Messages.X509_CERT_LABEL_ISSUER_ID, binify(cert.getIssuerUniqueID()));
                add(Messages.X509_CERT_LABEL_ISSUER_ALTERNATE_NAMES, getIssuerAlternativeNames(cert));
                add(Messages.X509_CERT_LABEL_SUBJECT_DN, cert.getSubjectDN().toString());
                add(Messages.X509_CERT_LABEL_SUBJECT_ID, binify(cert.getIssuerUniqueID()));
                add(Messages.X509_CERT_LABEL_SUBJECT_ALTERNATE_NAMES, getSubjectAlternativeNames(cert));
                add(Messages.X509_CERT_LABEL_VERSION, Integer.toString(cert.getVersion()));
                add(Messages.X509_CERT_LABEL_SERIAL, cert.getSerialNumber().toString());
                add(Messages.X509_CERT_LABEL_NOT_BEFORE, DateFormat.getDateTimeInstance().format(cert.getNotBefore()));
                add(Messages.X509_CERT_LABEL_NOT_AFTER, DateFormat.getDateTimeInstance().format(cert.getNotAfter()));
                add(Messages.X509_CERT_LABEL_SIG_ALG_NAME, cert.getSigAlgName());
                add(Messages.X509_CERT_LABEL_SIG_ALG_OID, cert.getSigAlgOID());
//				add(Messages.X509_CERT_LABEL_SIGNATURE,hexify(cert.getSignature()));
                add(Messages.X509_CERT_LABEL_KEY_USAGE, getKeyUsage(cert));
                add(Messages.X509_CERT_LABEL_EXT_KEY_USAGE, getExtKeyUsage(cert));
//				add(Messages.X509_CERT_LABEL_BASIC_CONSTRAINTS,getBasicConstraints(cert));
            }
        }
    }

    /**
     * Returns an iterator of certificate attributes.
     * 
     * @return An iterator of certificate attributes.
     *         This method never returns null; however, the iterator
     *         may have zero elements.
     */
    @Override
    public Iterator<Entry> iterator() {
        return entries_.iterator();
    }

    /**
     * Returns an array of certificate attributes.
     * 
     * @return An array of certificate attributes, possibly
     *         empty, but never null.
     */
    public Entry[] entries() {
        return entries_.toArray(new Entry[0]);
    }

    /**
     * Returns a string representation of the object as a series
     * of attribute key / value string pairs.
     * 
     * @return A string representation of the object, never null.
     */
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("{"); //$NON-NLS-1$
        boolean comma = false;
        for (Entry entry : entries_) {
            if (comma) {
                b.append(',');
            } else {
                comma = true;
            }
            b.append(entry.toString());
        }
        b.append('}');
        return b.toString();
    }

    /**
     * Objects of this class represent attributes of an LibertyX509CertData object.
     * Each entry is defined by a single attribute name and its value.
     * 
     * @author cbrealey@ca.ibm.com
     */
    public static class Entry {

        private final String key_;
        private final String val_;

        //
        // Constructs a new entry representing a certificate attribute.
        // Marked private to restrict its construction to within this unit.
        //
        Entry(String key, String val) {
            key_ = key;
            val_ = val;
        }

        /**
         * Returns the name of the attribute.
         * 
         * @return The name of the attribute, never null.
         */
        public String getKey() {
            return key_;
        }

        /**
         * Returns the value of the attribute.
         * 
         * @return The value of the attribute, possibly null.
         */
        public String getVal() {
            return val_;
        }

        /**
         * Returns a string representation of the object.
         * 
         * @return A string representation of the object, never null.
         */
        @Override
        public String toString() {
            return "{" + key_ + "," + val_ + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    //
    // Adds a certificate attribute key/value pair to the internal list.
    //
    private void add(String key, String val) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "key=[" + key + //$NON-NLS-1$
                                        "] val=[" + val + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        entries_.add(new Entry(key, val));
    }

    //
    // Returns the issuer alternative names, or null if none
    // or if a parsing exception is thrown.
    //
    private static String getIssuerAlternativeNames(X509Certificate cert) {
        try {
            return getAlternativeNames(cert.getIssuerAlternativeNames());
        } catch (CertificateParsingException e) {
            Trace.logError(e.getMessage(), e);
            return null;
        }
    }

    //
    // Returns the subject alternative names, or null if none
    // or if a parsing exception is thrown.
    //
    private static String getSubjectAlternativeNames(X509Certificate cert) {
        try {
            return getAlternativeNames(cert.getSubjectAlternativeNames());
        } catch (CertificateParsingException e) {
            Trace.logError(e.getMessage(), e);
            return null;
        }
    }

    //
    // Does most of the work of the two get...AlternativeNames() methods above.
    // Read about X509Certificate.getSubjectAlternativeNames() to understand
    // the somewhat cryptic algorithm inside this method.
    //
    private static String getAlternativeNames(Collection<List<?>> alternativeNames) {
        if (alternativeNames != null) {
            StringBuffer buffer = new StringBuffer();
            int i = 0;
            for (List<?> alternativeName : alternativeNames) {
                Object indexObj = alternativeName.get(0);
                if (indexObj != null && indexObj instanceof Integer) {
                    int index = ((Integer) indexObj).intValue();
                    if (index >= 0 && index < ALTERNATIVE_NAMES.length) {
                        String valueAsString = null;
                        Object value = alternativeName.get(1);
                        if (value != null) {
                            if (value instanceof byte[]) {
                                valueAsString = hexify((byte[]) value);
                            } else {
                                valueAsString = value.toString();
                            }
                            if (i++ > 0) {
                                buffer.append(' ');
                            }
                            buffer.append(NLS.bind(Messages.X509_ALTERNATIVE_NAME, new String[] { ALTERNATIVE_NAMES[index], valueAsString }));
                        }
                    }
                }
            }
            return buffer.length() == 0 ? null : buffer.toString();
        }
        return null;
    }

//	//
//	// Returns the encoded public key as a string of hex digits.
//	//
//	private static String getPublicKeyEncoded(PublicKey publicKey) {
//		return publicKey != null ? hexify(publicKey.getEncoded()) : null;
//	}

    //
    // Returns the format of the public key.
    //
    private static String getPublicKeyFormat(PublicKey publicKey) {
        return publicKey != null ? publicKey.getFormat() : null;
    }

    //
    // Returns the algorithm of the public key.
    //
    private static String getPublicKeyAlgorithm(PublicKey publicKey) {
        return publicKey != null ? publicKey.getAlgorithm() : null;
    }

    //
    // Returns the standard key usage assertions per RFC 3280.
    //
    private static String getKeyUsage(X509Certificate cert) {
        boolean[] bits = cert.getKeyUsage();
        if (bits != null) {
            String separator = null;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < bits.length && i < KEY_USAGE.length; i++) {
                if (bits[i]) {
                    if (separator == null) {
                        separator = Messages.X509_KEY_USAGE_SEPARATOR;
                    } else {
                        buffer.append(separator);
                    }
                    buffer.append(KEY_USAGE[i]);
                }
            }
            return buffer.length() == 0 ? null : buffer.toString();
        }
        return null;
    }

    //
    // Returns the extended key usage assertions, if any.
    //
    private static String getExtKeyUsage(X509Certificate cert) {
        try {
            List<String> extendedKeyUsages = cert.getExtendedKeyUsage();
            if (extendedKeyUsages != null) {
                String separator = null;
                StringBuffer buffer = new StringBuffer();
                for (String extendedKeyUsage : extendedKeyUsages) {
                    if (separator == null) {
                        separator = Messages.X509_KEY_USAGE_SEPARATOR;
                    } else {
                        buffer.append(separator);
                    }
                    buffer.append(extendedKeyUsage);
                }
                return buffer.length() == 0 ? null : buffer.toString();
            }
            return null;
        } catch (CertificateParsingException e) {
            Trace.logError(e.getMessage(), e);
            return null;
        }
    }

    //
    // Returns a string binary representation of a bunch of bits.
    //
    private static String binify(boolean[] bits) {
        if (bits == null) {
            return ""; //$NON-NLS-1$
        }
        StringBuffer buffer = new StringBuffer();
        for (boolean b : bits) {
            buffer.append(b ? '1' : '0');
        }
        return NLS.bind(Messages.X509_BINARY, buffer);
    }

    //
    // Returns a string hex representation of a bunch of bytes.
    //
    private static String hexify(byte[] bytes) {
        if (bytes == null) {
            return ""; //$NON-NLS-1$
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
//			if (i > 0) {
//				buffer.append(' ');
//			}
            String hex = Integer.toHexString(bytes[i] & 0xff);
            if (hex.length() == 1) {
                buffer.append('0');
            }
            buffer.append(hex);
            if (i % 4 == 3) {
                buffer.append(' ');
            }
        }
        return NLS.bind(Messages.X509_HEX, buffer);
    }
}

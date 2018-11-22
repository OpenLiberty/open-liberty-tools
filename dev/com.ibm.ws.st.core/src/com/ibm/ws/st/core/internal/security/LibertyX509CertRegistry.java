/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;

/**
 * LibertyX509CertRegistry provides transient and persistent storage of
 * certificates considered trusted by the client. Using methods on
 * the singleton instance of this class, clients can register
 * certificates in memory or on disk, remove certificates from the
 * registry, and ask if a certificate or certificate path is trusted
 * according to the certificates in the registry.
 *
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertRegistry {

    private static LibertyX509CertRegistry instance_;
    private static final String USER_KEYSTORE = "libertycertsKeystore"; //$NON-NLS-1$
    private static final String SECURE_STORAGE_NODE = "com.ibm.ws.st.core.internal.security.LibertyX509CertRegistry";
    private static final String SECURE_STORAGE_PASSWORD_KEY = "password";

    private File keyStoreFile_ = null;
    private KeyStore transientKeyStore_ = null;
    private KeyStore persistentKeyStore_ = null;

    private LibertyX509CertRegistry() {
        // Declaring the CTOR private.
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return The singleton instance of this class.
     *         This method never returns null.
     */
    public static LibertyX509CertRegistry instance() {
        if (instance_ == null) {
            instance_ = new LibertyX509CertRegistry();
        }
        return instance_;
    }

    /**
     * Adds the given certificate to the registry if it is not already present.
     * The certificate is kept in transient or "in memory" storage; therefore,
     * it will be discarded when the host process or session terminates, or in
     * response to a {@link #purge()}. The certificate can also be removed by
     * calling {@link #removeCertificate(Certificate)}.
     * <p/>
     * If the certificate is already present in the registry's on disk storage
     * area, it is effectively moved to transient storage.
     *
     * @param certificate The certificate to store in memory.
     *                        The method does nothing if the certificate is null.
     * @throws KeyStoreException If the underlying key stores used to manage
     *                               the certificates cannot be constructed, loaded, or otherwise manipulated.
     *                               If an exception is thrown, the certificate may or may not have been
     *                               registered successfully.
     */
    public void trustCertificateTransiently(Certificate certificate) throws KeyStoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificate=[" + certificate + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (certificate != null) {
            //
            // Add the certificate to the key store if it isn't already there.
            // If the certificate exists in the other key store, remove it.
            // If the persistent key store is updated, we write it immediately.
            //
            String alias = transientKeyStore().getCertificateAlias(certificate);
            if (Trace.ENABLED) {
                Trace.trace(Trace.SECURITY, "prior transient alias=[" + alias + //$NON-NLS-1$
                                            "]"); //$NON-NLS-1$
            }
            if (alias == null) {
                transientKeyStore_.setCertificateEntry(newAlias(), certificate);
            }
            alias = persistentKeyStore().getCertificateAlias(certificate);
            if (Trace.ENABLED) {
                Trace.trace(Trace.SECURITY, "prior persistent alias=[" + alias + //$NON-NLS-1$
                                            "]"); //$NON-NLS-1$
            }
            if (alias != null) {
                persistentKeyStore_.deleteEntry(alias);
                store();
            }
        }
    }

    /**
     * Adds the given certificate to the registry if it is not already present.
     * The certificate is kept in persistent or "on disk" storage immediately;
     * therefore, it will survive from one host process or session to the next,
     * and can only be removed from the registry by calling {@link #removeCertificate(Certificate)}.
     * <p/>
     * If the certificate is already present in the registry's in memory storage
     * area, it is effectively moved to on disk storage.
     *
     * @param certificate The certificate to store on disk.
     *                        The method does nothing if the certificate is null.
     * @throws KeyStoreException If the underlying key stores used to manage
     *                               the certificates cannot be constructed, loaded, or otherwise manipulated.
     *                               If an exception is thrown, the certificate may or may not have been
     *                               registered successfully.
     */
    public void trustCertificatePersistently(Certificate certificate) throws KeyStoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificate=[" + certificate + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (certificate != null) {
            //
            // Add the certificate to the key store if it isn't already there.
            // If the certificate exists in the other key store, remove it.
            // If the persistent key store is updated, we write it immediately.
            //
            String alias = persistentKeyStore().getCertificateAlias(certificate);
            if (Trace.ENABLED) {
                Trace.trace(Trace.SECURITY, "prior persistent alias=[" + alias + //$NON-NLS-1$
                                            "]"); //$NON-NLS-1$
            }
            if (alias == null) {
                persistentKeyStore_.setCertificateEntry(newAlias(), certificate);
                store();
            }
            if (transientKeyStore_ != null) {
                alias = transientKeyStore_.getCertificateAlias(certificate);
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "prior transient alias=[" + alias + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
                if (alias != null) {
                    transientKeyStore_.deleteEntry(alias);
                }
            }
        }
    }

    /**
     * Removes a certificate from the registry.
     * The method has no effect if the certificate is not registered.
     *
     * @param certificate The certificate to remove.
     *                        The method does nothing if the certificate is null.
     * @throws KeyStoreException If the underlying key stores used to manage
     *                               the certificates cannot be manipulated.
     *                               If an exception is thrown, the certificate may or may not have been
     *                               removed successfully.
     */
    public void removeCertificate(Certificate certificate) throws KeyStoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificate=[" + certificate + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (certificate != null) {
            if (transientKeyStore_ != null) {
                String alias = transientKeyStore_.getCertificateAlias(certificate);
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "prior transient alias=[" + alias + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
                if (alias != null) {
                    transientKeyStore_.deleteEntry(alias);
                }
            }
            String alias = persistentKeyStore().getCertificateAlias(certificate);
            if (Trace.ENABLED) {
                Trace.trace(Trace.SECURITY, "prior persistent alias=[" + alias + //$NON-NLS-1$
                                            "]"); //$NON-NLS-1$
            }
            if (alias != null) {
                persistentKeyStore_.deleteEntry(alias);
                store();
            }
        }
    }

    /**
     * Returns all certificates registered in transient storage.
     *
     * @return A possibly empty, but never null, array of
     *         all certificates registered in transient storage.
     * @throws KeyStoreException If the underlying key store used to manage
     *                               the certificates cannot be manipulated.
     */
    public Certificate[] getCertificatesTrustedTransiently() throws KeyStoreException {
        return transientKeyStore_ == null ? new Certificate[0] : certificates(transientKeyStore_);
    }

    /**
     * Returns all certificates registered in persistent storage.
     *
     * @return A possibly empty, but never null, array of
     *         all certificates registered in persistent storage.
     * @throws KeyStoreException If the underlying key store used to manage
     *                               the certificates cannot be manipulated.
     */
    public Certificate[] getCertificatesTrustedPersistently() throws KeyStoreException {
        return certificates(persistentKeyStore());
    }

    /**
     * Determines if the given certificate is registered.
     *
     * @param certificate The certificate to check.
     * @return True if and only if the certificate exists in the registry.
     *         Returns false if the certificate does not exist, or is null.
     * @throws KeyStoreException If the underlying key store used to manage
     *                               the certificates cannot be manipulated.
     */
    public boolean isTrusted(Certificate certificate) throws KeyStoreException {
        boolean trusted = certificate != null
                          && ((transientKeyStore_ != null && (transientKeyStore_.getCertificateAlias(certificate) != null))
                              || (persistentKeyStore().getCertificateAlias(certificate) != null));
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificate=[" + certificate + //$NON-NLS-1$
                                        "] trusted=[" + trusted + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return trusted;
    }

    /**
     * Determines if any certificate in the given certificate path is registered.
     *
     * @param certPath The certificate path to check.
     * @return True if and only if at least one certificate in the path
     *         exists in the registry. Returns false if none of the certificates
     *         in the path exist, or if the certificate path is null.
     * @throws KeyStoreException If the underlying key stores used to manage
     *                               the certificates cannot be manipulated.
     */
    public boolean isTrusted(CertPath certPath) throws KeyStoreException {
        boolean trusted = false;
        if (certPath != null) {
            for (Certificate certificate : certPath.getCertificates()) {
                if (isTrusted(certificate)) {
                    trusted = true;
                }
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certPath=[" + certPath + //$NON-NLS-1$
                                        "] trusted=[" + trusted + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return trusted;
    }

    /**
     * Releases memory being used by the registry.
     * This should not be misconstrued as a way to remove certificates
     * from the registry, though it may have that effect.
     * This method is designed to be called when memory consumption
     * thresholds are exceeded and memory needs to be reclaimed.
     *
     * @param removeTransientCertificates Controls whether or not
     *                                        transient certificates are discarded.
     *                                        <p/>If false, the in memory cache of persistent certificates is
     *                                        discarded, however, this has no observable effect since the
     *                                        persistent key store is written to disk after every update.
     *                                        The in memory key store of transient certificates is not touched.
     *                                        purge(false) should be called when memory reclamation is desired,
     *                                        but not critical.
     *                                        <p/>If true, the in memory cache of persistent certificates is
     *                                        discarded as explained above, <b>and</b> the in memory key store
     *                                        of transient certificates is discarded. purge(true) should be
     *                                        called only when the need to reclaim memory is critical since the
     *                                        effect of the call will be to discard all certificates the user
     *                                        has accepted as trusted for the current session.
     */
    public void purge(boolean removeTransientCertificates) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "removeTransientCertificates=[" + removeTransientCertificates + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        persistentKeyStore_ = null;
        if (removeTransientCertificates) {
            transientKeyStore_ = null;
        }
    }

    //
    // Computes a new, unique alias string. This is used by the two
    // trustCertificate...() methods when they need to add a certificate.
    //
    private String newAlias() {
        String alias = UUID.randomUUID().toString();
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "alias=[" + alias + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return alias;
    }

    //
    // Creates the transient key store if it doesn't already exist.
    //
    private KeyStore transientKeyStore() throws KeyStoreException {
        if (transientKeyStore_ == null) {
            transientKeyStore_ = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                transientKeyStore_.load(null, getPassword().toCharArray());
            } catch (Exception e) {
                Trace.logError(e.getMessage(), e);
                throw new KeyStoreException(Messages.X509_CANNOT_LOAD_TRANSIENT_KEYSTORE, e);
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "transientKeyStore_=[" + transientKeyStore_ + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return transientKeyStore_;
    }

    //
    // Creates the persistent key store if it doesn't already exist,
    // and loads the key store from the file if it exists.
    //
    private KeyStore persistentKeyStore() throws KeyStoreException {
        if (persistentKeyStore_ == null) {
            File keyStoreFile = keyStoreFile();
            persistentKeyStore_ = KeyStore.getInstance(KeyStore.getDefaultType());
            if (keyStoreFile.exists()) {
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(keyStoreFile);
                    persistentKeyStore_.load(inputStream, getPassword().toCharArray());
                    inputStream.close();
                } catch (Exception e) {
                    Trace.logError(e.getMessage(), e);
                    throw new KeyStoreException(NLS.bind(Messages.X509_CANNOT_READ_PERSISTENT_KEYSTORE, keyStoreFile), e);
                } finally {
                    try {
                        if (inputStream != null)
                            inputStream.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            } else {
                try {
                    persistentKeyStore_.load(null, getPassword().toCharArray());
                } catch (Exception e) {
                    Trace.logError(e.getMessage(), e);
                    throw new KeyStoreException(Messages.X509_CANNOT_LOAD_PERSISTENT_KEYSTORE, e);
                }
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "persistentKeyStore_=[" + persistentKeyStore_ + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return persistentKeyStore_;
    }

    //
    // Writes the persistent key store to disk.
    //
    private void store() throws KeyStoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "persistentKeyStore_=[" + persistentKeyStore_ + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (persistentKeyStore_ != null) {
            File keyStoreFile = keyStoreFile();
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(keyStoreFile);
                persistentKeyStore_.store(outputStream, getPassword().toCharArray());
                outputStream.close();
            } catch (Exception e) {
                Trace.logError(e.getMessage(), e);
                throw new KeyStoreException(NLS.bind(Messages.X509_CANNOT_WRITE_PERSISTENT_KEYSTORE, keyStoreFile));
            } finally {
                try {
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    //
    // Returns a handle to the key store file.
    // The file is kept in the plug-in state location.
    //
    private File keyStoreFile() throws KeyStoreException {
        if (keyStoreFile_ == null) {
            IPath stateLocation = Activator.getInstance().getStateLocation();
            if (stateLocation != null) {
                keyStoreFile_ = stateLocation.append(USER_KEYSTORE).toFile();
            } else {
                throw new KeyStoreException(Messages.X509_CANNOT_GET_PLUGIN_STATE_LOCATION);
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "keyStoreFile_=[" + keyStoreFile_ + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return keyStoreFile_;
    }

    //
    // Returns all certificates in the key store.
    //
    private Certificate[] certificates(KeyStore keyStore) throws KeyStoreException {
        List<Certificate> certificates = new LinkedList<Certificate>();
        Enumeration<String> e = keyStore.aliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate != null) {
                certificates.add(certificate);
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "keystore=[" + keyStore + //$NON-NLS-1$
                                        "] certificates=[" + certificates + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return certificates.toArray(new Certificate[0]);
    }

    private static void savePassword(String password) {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = preferences.node(SECURE_STORAGE_NODE);
        try {
            node.put(SECURE_STORAGE_PASSWORD_KEY, password, true);
            preferences.flush();
        } catch (Exception e) {
            Trace.logError("Failed to store server password", e);
        }
    }

    static String getPassword() {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = preferences.node(SECURE_STORAGE_NODE);
        String password = null;
        try {
            password = node.get(SECURE_STORAGE_PASSWORD_KEY, "");
        } catch (StorageException e) {
            Trace.logError("Failed to retrieve server password", e);
        }
        if (password == null || password.isEmpty()) {
            password = LibertySecurityHelper.generatePassword();
            savePassword(password);
        }
        return password;
    }
}

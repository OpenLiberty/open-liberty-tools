/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;

/**
 * This X509TrustManager performs up to three stages of validation
 * of the X.509 certificate path returned from a server claiming to
 * be a Liberty server. The stages are as follows:
 * <ol>
 * <li>The trust manager tries to validate the certificate path
 * against a set of trusted certificates in a key store that is
 * located within the plug-in containing this class. The standard
 * PKIX validation algorithm is used, with CRL (Certificate
 * Revocation List) processing disabled.</li>
 * <li>If stage 1 validation fails, the trust manager tries to
 * validate the specific certificate that failed in the first stage
 * against a registry of certificates previously accepted by the user
 * as trusted. If first stage validation failed but did not identify
 * a specific certificate, the trust manager tries to validate the
 * root certificate of the path.</li>
 * <li>If stage 2 validation fails, the trust manager begins asking
 * extenders of the "com.ibm.ws.st.core.libertyX509CertPathValidator"
 * to attempt to validate the certificate path and the offending or
 * root certificate from stage 1. If none of the extenders validate
 * the certificate path, the trust manager will throw a
 * CertificateException resulting in the connection to the server
 * being rejected. If an extender validates the certificate path, it
 * will instruct this trust manager to accept the identity of the
 * server as genuined either one time, for the remainder of the work
 * workbench session, or for the lifetime of the workspace.</li>
 * </ol>
 *
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509TrustManager implements X509TrustManager {

    private static final String CERTIFICATE_TYPE = "X.509"; //$NON-NLS-1$
    private static final String ALGORITHM_TYPE = "PKIX"; //$NON-NLS-1$
    private static final String TRUSTED_KEYSTORE = "libertycerts"; //$NON-NLS-1$

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        //
        // We have no trusted CA certificates to return in this manner.
        // All certificate validation is performed by checkServerTrusted().
        //
        return null;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        //
        // We are performing server SSL authentication, not client SSL authentication.
        //
        return;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certs=[" + Arrays.toString(certs) + //$NON-NLS-1$
                                        "] authType=[" + authType + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        //
        // Part 1 of 5: Generate a CertPath from the incoming list of certs.
        // This should never fail on any self-respecting JRE, but if it does,
        // the only response is to let the CertificateException thrown by
        // either of the CertificateFactory methods below to percolate up the
        // stack. Without a CertPath, we cannot hope to proceed to any of other
        // parts of the algorithm.
        //
        CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certificateFactory=[" + certificateFactory + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        CertPath certPath = certificateFactory.generateCertPath(Arrays.asList(certs));
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certPath=[" + certPath + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        //
        // Some running variables:
        // valid - Remains false until we have successfully validated the cert path.
        // cause - Used to capture the exception, if any, indicating a validation failure.
        // nastyCert - Used to capture the certificate that caused validation to fail.
        //
        boolean valid = false;
        Exception cause = null;
        CertPath nastyCertPath = null;
        Certificate nastyCert = null;
        int nastyIndex = -1;

        // check system variable to see if we should ignore the pre-defined libertycerts file.
        String Trusted_KeyStore = TRUSTED_KEYSTORE;
        if (Boolean.getBoolean("Liberty.Security.IgnorePredefined.KeyStore"))
            Trusted_KeyStore = Trusted_KeyStore + "_ignore"; //$NON-NLS-1$

        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        URL keyStoreURL = bundle == null ? null : FileLocator.find(bundle, new Path(Trusted_KeyStore), null);

        if (keyStoreURL != null) {
            try {
                //
                // Part 2 of 5: Try to validate the cert path against our own
                // trusted keystore if it exists. It would be kept at the root of this plug-in
                // and contains all known self-signed root certificates from we know about.
                // If Liberty ever introduces a new self-signed root certificate,
                // it should be added to the keystore in the next release of the tools.
                //
                // First, we need a CertPathValidator and our trusted KeyStore.
                //
                CertPathValidator certPathValidator = CertPathValidator.getInstance(ALGORITHM_TYPE);
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "certPathValidator=[" + certPathValidator + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "keystore=[" + keyStore + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }

                //
                // Load the trusted KeyStore.
                //
                InputStream inputStream = keyStoreURL.openStream();
                char[] password = LibertyX509CertRegistry.getPassword().toCharArray();
                keyStore.load(inputStream, password);

                //
                // Validate. If it fails, an exception is thrown and caught below.
                // We call setRevocationEnabled(false) to disable CRL (Certificate
                // Revocation List) checking. We don't support revocation of Liberty
                // certificates, therefore, we don't have a CRL to provide,
                // and by default, PKIX validation will fail without a CRL.
                //
                PKIXParameters parameters = new PKIXParameters(keyStore);
                parameters.setRevocationEnabled(false);
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "parameters=[" + parameters + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
                certPathValidator.validate(certPath, parameters);
                //
                // If we made it this far without a CertificateException or
                // some other exception being tossed, then we're valid and done.
                //
                valid = true;
            } catch (NoSuchAlgorithmException e) {
                //
                // CertPathValidator.getInstance() or KeyStore.load(...) failed.
                //
                Trace.logError("CertPathValidator.getInstance() or KeyStore.load(...) failed.", e);
                cause = e;
            } catch (KeyStoreException e) {
                //
                // KeyStore.getInstance() or new PKIXParameters(...) failed.
                //
                Trace.logError("KeyStore.getInstance() or new PKIXParameters(...) failed.", e);
                cause = e;
            } catch (IOException e) {
                //
                // Failed to open keystore input stream, or load the keystore from it.
                //
                Trace.logError("Failed to open keystore input stream, or load the keystore from it.", e);
                cause = e;
            } catch (InvalidAlgorithmParameterException e) {
                //
                // Failed to construct or use PKIXParameters.
                //
                Trace.logError("Failed to construct or use PKIXParameters.", e);
                cause = e;
            } catch (CertificateException e) {
                //
                // A general certificate validation exception was thrown. We don't have a set
                // of trusted certificates in a predefined KeyStore at the time of implementation
                // so the exception will only show on INFO level traces. If in the future we have a
                // predefined KeyStore then we should change this to an ERROR level log message.
                //
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "No predefined KeyStore was found.", e);
                }
                cause = e;
            } catch (CertPathValidatorException e) {
                //
                // The previous exceptions are all genuinely unexpected.
                // This one isn't. It indicates that PKIX validation of the
                // server certificate path failed, usually because it could
                // not match any of the certificates in the path with any of
                // the trusted certificates in our predefined KeyStore.
                // As a "normal" exception, we trace it, but we don't log it
                // to avoid cluttering the log and needlessly scaring the user.
                //
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "Server certificate validation failed.", e);
                }
                cause = e;
                nastyCertPath = e.getCertPath();
                if (nastyCertPath != null) {
                    List<? extends Certificate> nastyCerts = nastyCertPath.getCertificates();
                    int size = nastyCerts.size();
                    if (size > 0) {
                        nastyIndex = e.getIndex();
                        nastyCert = nastyIndex >= 0 ? nastyCerts.get(nastyIndex) : nastyCerts.get(size - 1);
                    }
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.SECURITY, "nastyCertPath=[" + nastyCertPath + //$NON-NLS-1$
                                                    "] size=[" + size + //$NON-NLS-1$
                                                    "] nastyIndex=[" + nastyIndex + //$NON-NLS-1$
                                                    "] nastyCert=[" + nastyCert + //$NON-NLS-1$
                                                    "]"); //$NON-NLS-1$
                    }
                }
            }
        }

        if (!valid) {
            //
            // Part 3 of 5: PKIX validation against our trusted KeyStore failed.
            // If failure was due to a CertPathValidatorException, we should already
            // have the certificate path, invalid certificate, and the index of it
            // in the path. If not, we'll grab the root certificate and its index in
            // the original path. Then we can determine if the certificate matches
            // one that has been previously stored in memory or on disk by the user
            // (or some other agent).
            //
            if (nastyCertPath == null) {
                nastyCertPath = certPath;
                List<? extends Certificate> nastyCerts = certPath.getCertificates();
                nastyIndex = nastyCerts.size() - 1;
                if (nastyIndex >= 0) {
                    nastyCert = nastyCerts.get(nastyIndex);
                }
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "nastyCertPath=[" + nastyCertPath + //$NON-NLS-1$
                                                "] nastyIndex=[" + nastyIndex + //$NON-NLS-1$
                                                "] nastyCert=[" + nastyCert + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
            }
            try {
                //
                // If we've come this far, nastyCert cannot be null (unless the original
                // incoming certificate path is empty, which should never be the case).
                // If for some reason it is, validation will quietly fail.
                //
                valid = LibertyX509CertRegistry.instance().isTrusted(nastyCert);
            } catch (KeyStoreException e) {
                //
                // Something unexpected happened while checking the registry.
                // Since a more "normal" validation failure already occurred
                // back in part 2, and since we aren't designed to pass more
                // than one cause (exception) onto the extenders in part 4,
                // we log the exception and quietly ask the extenders to help.
                //
                Trace.logError("An unexpected error occurred while checking the registry.", e);
            }
        }

        if (!valid) {
            //
            // Part 4 of 5: Not only has PKIX validation against our trusted
            // KeyStore failed, but so has our attempt to find the certificate
            // in the in memory or on disk collection of user-trusted certificates.
            // Ask the extenders, if any, of the libertyX509CertPathValidator extension
            // point if they can validate the cert path. Depending upon the answer,
            // we may save the certificate for subsequent inquiries in part 3 above.
            //
            if (nastyCertPath != null) {
                LibertyX509CertPathValidatorRegistry registry = LibertyX509CertPathValidatorRegistry.instance();
                LibertyX509CertPathValidatorResult[] results = registry.validate(nastyCertPath, nastyIndex, null, cause);
                if (results.length > 0) {
                    //
                    // It's the result of the last validator that ran which matters
                    // since any and all previous validators must have ABSTAINED.
                    //
                    LibertyX509CertPathValidatorResult result = results[results.length - 1];
                    LibertyX509CertPathValidatorResult.Status status = result.getStatus();
                    //
                    // If the status is ABSTAINED or REJECTED, the certificate path is invalid.
                    // If the status is VALID_ONCE, VALID_FOR_SESSION or VALID_FOR_WORKSPACE,
                    // the path is trusted.
                    //
                    if (status != LibertyX509CertPathValidatorResult.Status.ABSTAINED && status != LibertyX509CertPathValidatorResult.Status.REJECTED) {
                        valid = true;
                        try {
                            if (status == LibertyX509CertPathValidatorResult.Status.VALID_FOR_SESSION) {
                                //
                                // VALID_FOR_SESSION: Store the certificate transiently.
                                //
                                LibertyX509CertRegistry.instance().trustCertificateTransiently(result.getCertificate());
                            } else if (status == LibertyX509CertPathValidatorResult.Status.VALID_FOR_WORKSPACE) {
                                //
                                // VALID_FOR_WORKSPACE: Store the certificate persistently.
                                //
                                LibertyX509CertRegistry.instance().trustCertificatePersistently(result.getCertificate());
                            }
                        } catch (KeyStoreException e) {
                            //
                            // Couldn't save the fact we trust the certificate.
                            // This doesn't change the fact that it's trusted,
                            // so we log and intentionally suppress the exception.
                            // TODO: Need API to notify the extension that approved it.
                            //
                            Trace.logError("Saving the trusted certificate to the KeyStore failed", e);
                        }
                    }
                }
            }
        }
        //
        // Part 5 of 5: Validation is done, for good or ill.
        // If the certificate path is not valid, throw an exception
        // with all cause information we've collected so far.
        // If the certificate path is valid, return normally.
        //
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "valid=[" + valid + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        if (!valid) {
            throw cause == null ? new CertificateException() : new CertificateException(cause);
        }
    }
}

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

import java.security.cert.Certificate;

/**
 * Objects of this class contain information about the results of
 * certificate path validators ({@link LibertyX509CertPathValidator}).
 * A result object is characterized by the status of the validation
 * attempt, one of REJECTED, ABSTAINED, VALID_ONCE, VALID_FOR_SESSION
 * or VALID_FOR_WORKSPACE, and by the certificate, if any, that caused
 * validation to fail.
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorResult {

    /**
     * @author cbrealey@ca.ibm.com
     *         This enum is used by implementations of LibertyX509CertPathValidator
     *         to indicate the validity of a certificate path.
     */
    public enum Status {
        /**
         * The certificate is judged to not be valid.
         * Remaining validators, if any, will not be invoked.
         */
        REJECTED,
        /**
         * The certificate is judged to not be valid.
         * The next validator, if any, will be invoked.
         * If there is no next validator, the framework
         * will reject the certificate.
         */
        ABSTAINED,
        /**
         * The certificate is judged to be valid this one time.
         * The next time the framework encounters the certificate,
         * it will be revalidated.
         */
        VALID_ONCE,
        /**
         * The certificate is judged to be valid for the remainder
         * of the Eclipse session. The next time the framework
         * encounters the certificate within the same running session,
         * it will be automatically accepted as valid and no validators
         * will be invoked. The validity of the certificate will be
         * discarded when the running session terminates.
         */
        VALID_FOR_SESSION,
        /**
         * The certificate is judged to be valid for the lifetime
         * of the workspace. The next time the framework encounters
         * the certificate from any running Eclipse session on the
         * workspace, the certificate will be automatically accepted
         * as valid.
         */
        VALID_FOR_WORKSPACE
    }

    private final Certificate certificate_;
    private final Status status_;

    /**
     * Constructs a new result object.
     * 
     * @param certificate In general, the specific certificate that caused
     *            validation to fail (with a status of REJECTED or ABSTAINED), or null
     *            if validation failed but a specific causal certificate cannot be
     *            determined, or if validation passed (with a status of VALID_ONCE,
     *            VALID_FOR_SESSION or VALID_FOR_WORKSPACE).
     * @param status The status of the result. This must not be null.
     */
    public LibertyX509CertPathValidatorResult(Certificate certificate, Status status) {
        if (status == null) {
            throw new IllegalArgumentException();
        }
        certificate_ = certificate;
        status_ = status;
    }

    /**
     * Returns the certificate that caused validation to fail.
     * 
     * @return The certificate that caused validation to fail,
     *         or null if either validation failed for some reason other than
     *         a specific certificate, or validation passed.
     */
    public Certificate getCertificate() {
        return certificate_;
    }

    /**
     * Returns the status of the validation attempt.
     * 
     * @return The status of the validation attempt.
     */
    public Status getStatus() {
        return status_;
    }

    /**
     * Returns a non translatable string representation.
     * 
     * @return A string representation of the object, never null.
     */
    @Override
    public String toString() {
        return "{" + status_ + "," + certificate_ + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}

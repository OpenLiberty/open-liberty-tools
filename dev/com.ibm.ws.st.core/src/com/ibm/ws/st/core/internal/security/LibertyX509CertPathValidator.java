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

import java.security.cert.CertPath;

/**
 * LibertyX509CertPathValidators are plugged into the Liberty Tools framework
 * via the libertyX509CertPathValidator extension point. When the Liberty
 * tooling receives an X.509 certificate path from an endpoint
 * claiming to be a Liberty server, it will perform standard PKIX
 * validation in an attempt to identify a trusted Certificate Authority.
 * If this fails, the tooling will invoke each LibertyX509CertPathValidator
 * plugged into the extension point until one of them accepts the
 * certificate path, or until there are no more LibertyX590CertPathValidator
 * objects left.
 * 
 * @author cbrealey@ca.ibm.com
 */
public abstract class LibertyX509CertPathValidator {
    /**
     * @param certPath The X.509 certificate path to validate.
     * @param index The zero-based index of the certificate in the certPath
     *            that failed standard PKIX certificate path validation, or -1 if none
     *            of the certificates in the path have been flagged as invalid.
     * @param message A message accompanying the request to
     *            validate the certificate path, or null for no message.
     * @param cause A causal exception, or null if there is none.
     * @return The result of validating the certificate path.
     */
    public abstract LibertyX509CertPathValidatorResult validate(CertPath certPath, int index, String message, Throwable cause);
}

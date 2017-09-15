/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.security;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.Random;

import javax.net.ssl.SSLContext;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;

public class LibertySecurityHelper {

    public static boolean validateSocketProtocol(final String protocol) {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            // see if this provider supports the provided security protocol
            Service service = provider.getService(SSLContext.class.getSimpleName(), protocol);
            if (service != null)
                return true;
        }

        // trace
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "The protocol: '" + protocol + "' is not supported."); //$NON-NLS-1$ //$NON-NLS-2$)
        }

        return false;
    }

    public static SSLContext getSSLContext() throws NoSuchAlgorithmException {
        /*
         * The SSL_TLSv2 protocol label is being used here only for IBM JVMs because TLS would
         * only enable TLS v1.0 and not newer versions.
         *
         * SSL_TLSv2 will not enable SSL by default due to the POODLE vulnerability.
         *
         * The TLS protocol label should be used by default for non-IBM JVMs.
         */
        boolean isIBMJRE = System.getProperty("java.vendor").indexOf("IBM") > -1;
        String defaultProtocol = isIBMJRE ? "SSL_TLSv2" : "TLS";

        String protocol = Activator.getPreference("socket.protocol", defaultProtocol);

        String userProvidedProtocol = System.getProperty("SECURITY_SOCKET_PROTOCOL"); //$NON-NLS-1$
// comment it out, if user specified an unsupported protocol, we should make it aware
//		if(userProvidedProtocol != null && validateSocketProtocol(userProvidedProtocol))
        if (userProvidedProtocol != null) {
            // trace
            if (Trace.ENABLED) {
                validateSocketProtocol(userProvidedProtocol);
            }
            protocol = userProvidedProtocol;
        } else if (isIBMJRE && !validateSocketProtocol(defaultProtocol)) {
            if (Trace.ENABLED)
                Trace.trace(Trace.SECURITY, "IBM JRE detected but protocol " + defaultProtocol + " is invalid.");
            // if we're using an IBM JRE and SSL_TLSv2 isn't supported then default back to TLS
            protocol = "TLS";
        }

        return SSLContext.getInstance(protocol);
    }

    public static String generatePassword() {
        String charStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        char[] chars = charStr.toCharArray();
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            int index = random.nextInt(charStr.length());
            builder.append(chars[index]);
        }
        return builder.toString();
    }

}
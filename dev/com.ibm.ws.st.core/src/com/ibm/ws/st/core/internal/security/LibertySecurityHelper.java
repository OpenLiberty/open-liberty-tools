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
        
        // we will not support TLS 1.0, TLS 1.1 etc
        String[] invalidProtocols = new String[] {"SSL","SSLv2","SSLv3","TLS","TLSv1","TLSv1.1","SSL_TLS","SSL_TLSv2","SSLv2Hello"};
        if (Arrays.asList(invalidProtocols).contains(protocol)) {
            // trace
            if (Trace.ENABLED) {
                Trace.trace(Trace.SECURITY, "The protocol: '" + protocol + "' is not supported."); //$NON-NLS-1$ //$NON-NLS-2$)
            }
            return false;
        }
        
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
         * This is the order of preference we will use to determine the SSL protocol 
         * - User provided from system property "SECURITY_SOCKET_PROTOCOL"
         * - Activator preference "socket.protocol"
         * - default TLSv1.2
         */
        String defaultProtocol = "TLSv1.2";
        String protocol = null;

        String activatorProtocol = Activator.getPreference("socket.protocol", null);

        String userProvidedProtocol = System.getProperty("SECURITY_SOCKET_PROTOCOL"); //$NON-NLS-1$
        // comment it out, if user specified an unsupported protocol, we should make it aware 
        // if(userProvidedProtocol != null && validateSocketProtocol(userProvidedProtocol))
        if ((userProvidedProtocol != null) && (validateSocketProtocol(userProvidedProtocol))) {
            protocol = userProvidedProtocol;
        } 
        
        if ((protocol == null) && (activatorProtocol != null) && (validateSocketProtocol(activatorProtocol))) {
            protocol = activatorProtocol;
        }
        
        if (protocol == null) {
            if (Trace.ENABLED) {
                // we don't actually use the rc from validate as we are just setting 
                // defaultProtocol, this is just for trace purposes
                validateSocketProtocol(defaultProtocol);
            }
            protocol = defaultProtocol;
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
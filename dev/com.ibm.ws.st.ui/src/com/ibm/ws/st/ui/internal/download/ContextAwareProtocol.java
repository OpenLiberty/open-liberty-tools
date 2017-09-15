/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 *
 */
public class ContextAwareProtocol extends Protocol {

    private static final ThreadLocal<Boolean> trustAll = new ThreadLocal<Boolean>() {
        /** {@inheritDoc} */
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private final ProtocolSocketFactory trustedSockets;

    public static void setTrusted(boolean trusted) {
        trustAll.set(trusted);
    }

    /**
     * @param scheme
     * @param factory
     * @param defaultPort
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public ContextAwareProtocol(String scheme, ProtocolSocketFactory factory, int defaultPort) throws KeyManagementException, NoSuchAlgorithmException {
        super(scheme, factory, defaultPort);
        trustedSockets = new TrustAllSSLSocketFactory();
    }

    /** {@inheritDoc} */
    @Override
    public ProtocolSocketFactory getSocketFactory() {
        if (trustAll.get()) {
            return trustedSockets;
        }
        return super.getSocketFactory();
    }

}

/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.ibm.ws.st.core.internal.security.LibertySecurityHelper;

/**
 *
 */
public class TrustAllSSLSocketFactory implements ProtocolSocketFactory, X509TrustManager {

    private final SSLSocketFactory socFactory;

    public TrustAllSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context;
        context = LibertySecurityHelper.getSSLContext();
        context.init(null, new TrustManager[] { this }, null);
        socFactory = context.getSocketFactory();
    }

    /** {@inheritDoc} */
    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
        // TODO Auto-generated method stub
        return socFactory.createSocket(arg0, arg1);
    }

    /** {@inheritDoc} */
    @Override
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException {
        return socFactory.createSocket(arg0, arg1, arg2, arg3);
    }

    /** {@inheritDoc} */
    @Override
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3, HttpConnectionParams arg4) throws IOException, UnknownHostException, ConnectTimeoutException {
        return socFactory.createSocket(arg0, arg1, arg2, arg3);
    }

    /** {@inheritDoc} */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // By not doing anything all clients are trusted
    }

    /** {@inheritDoc} */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // By not doing anything all servers are trusted
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

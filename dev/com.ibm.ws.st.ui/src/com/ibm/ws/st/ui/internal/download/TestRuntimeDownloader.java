/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.ISite;
import com.ibm.ws.st.core.internal.repository.ISource;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Trace;

public class TestRuntimeDownloader implements ISite {
    private boolean authenticated = false;
    private static final int DELAY = 2000;

    public TestRuntimeDownloader() {
        super();
    }

    static void delay(IProgressMonitor monitor) {
        monitor.beginTask("Connecting...", 100);
        monitor.worked(20);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            // ignore
        }
        monitor.worked(50);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            // ignore
        }
        monitor.done();
    }

    @Override
    public boolean authenticate(PasswordAuthentication pa, IProgressMonitor monitor) {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "authenticate");
        delay(monitor);
        authenticated = (pa != null && "tim".equals(pa.getUserName()));
        return authenticated;
    }

    @Override
    public List<IProduct> getCoreProducts(IProgressMonitor monitor) {
        final IProduct p = new IProduct() {
            @Override
            public String getName() {
                return "wlp-test.zip";
            }

            @Override
            public String getDescription() {
                return "Test download description";
            }

            @Override
            public Type getType() {
                return IProduct.Type.INSTALL;
            }

            @Override
            public ProductType getProductType() {
                return ProductType.LOCAL_TYPE;
            }

            @Override
            public long getSize() {
                return 25000;
            }

            @Override
            public License getLicense(IProgressMonitor monitor) throws IOException {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "getLicense");
                delay(monitor);
                if (!isAuthenticated())
                    throw new IOException("not authenticated");
                return new License("Test license", "Test License");
            }

            @Override
            public String getSiteName() {
                return "Test Download";
            }

            @Override
            public List<String> getProvideFeature() {
                return null;
            }

            @Override
            public List<String> getRequireFeature() {
                return null;
            }

            @Override
            public IRuntimeInfo getRuntimeInfo() {
                return null;
            }

            @Override
            public String getAttribute(String name) {
                return null;
            }

            @Override
            public ISource getSource() {
                return null;
            }

            @Override
            public String getHashSHA256() {
                return null;
            }

            @Override
            public boolean isInstallOnlyFeature() {
                return false;
            }
        };

        return Arrays.asList(p);
    }

    @Override
    public String getName() {
        return "Test Download";
    }

    @Override
    public boolean isAuthenticationRequired() {
        return true;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void reset() {
        // ignore
    }

    @Override
    public List<IProduct> getApplicableProducts(IRuntimeInfo runtimeInfo, IProgressMonitor monitor) {
        return Collections.emptyList();
    }

    @Override
    public List<IProduct> getConfigSnippetProducts(IProgressMonitor monitor) {
        return Collections.emptyList();
    }
}

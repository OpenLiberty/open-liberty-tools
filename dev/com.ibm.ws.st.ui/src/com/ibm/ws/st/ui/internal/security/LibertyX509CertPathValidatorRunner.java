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

package com.ibm.ws.st.ui.internal.security;

import java.security.cert.CertPath;

import org.eclipse.swt.widgets.Display;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.security.LibertyX509CertPathValidator;
import com.ibm.ws.st.core.internal.security.LibertyX509CertPathValidatorResult;

/**
 * This class is plugged into the com.ibm.ws.st.core.libertyX509CertPathValidator
 * extension point. If invoked by the X.509 certificate path validation framework,
 * it raises a modal dialog that gives the user the opportunity to review the
 * certificate path presented by the supposed Liberty server and
 * accept it or reject it.
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorRunner extends LibertyX509CertPathValidator {

    @Override
    public LibertyX509CertPathValidatorResult validate(CertPath certPath, int index, String causeMessage, Throwable causeThrowable) {
        try {
            //
            // Because validate() isn't called on the UI thread, we must
            // post a Runnable for synchronous execution on the UI thread
            // that launches the dialog.
            //
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "certPath=[" + certPath + //$NON-NLS-1$
                                        "] index=[" + index + //$NON-NLS-1$
                                        "] causeMessage=[" + causeMessage + //$NON-NLS-1$
                                        "] causeThrowable=[" + causeThrowable + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            Display display = Display.getDefault();
            LibertyX509CertPathValidatorRunnable runnable = new LibertyX509CertPathValidatorRunnable(display, certPath, index, causeMessage, causeThrowable);
            display.syncExec(runnable);
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "runnable().result=[" + runnable.result() + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            return runnable.result();
        } catch (Throwable t) {
            //
            // If anything goes wrong with the dialog, catch the exception
            // and return null, equivalent to rejecting the certificate.
            //
            Trace.logError(t.getMessage(), t);
        }
        return null;
    }
}

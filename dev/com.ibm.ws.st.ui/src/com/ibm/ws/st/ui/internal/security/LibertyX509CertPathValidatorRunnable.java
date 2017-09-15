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
import java.security.cert.Certificate;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.security.LibertyX509CertPathValidatorResult;

/**
 * When instantiated and run, this Runnable launches a modal dialog
 * that gives the user the opportunity to review the certificate path
 * presented by the supposed Liberty server and accept it or
 * reject it. The run() method must only be called from the UI thread.
 *
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorRunnable implements Runnable {

    private final Display display_;
    private final CertPath certPath_;
    private final int index_;
    private final String causeMessage_;
    private final Throwable causeThrowable_;
    private LibertyX509CertPathValidatorResult result_;

    private static final int TRANSIENT = 0;
    private static final int PERSISTENT = 1;
    private static final String HEADLESS_MODE_AUTO_ACCEPT_CERT = "headless.auto.accept.cert";

    /**
     * Constructs a new runnable.
     *
     * @param display The display under which the dialog will be opened.
     * @param certPath The certificate path to show to the user.
     * @param index The index of the untrusted certificate in the path,
     *            or -1 if there is no specific untrusted certificate.
     * @param causeMessage A message providing additional information about
     *            the nature or cause of the untrusted certificate path,
     *            or null if there is no message.
     * @param causeThrowable An exception providing additional information about
     *            the nature or cause of the untrusted certificate path,
     *            or null if there is no cause.
     */
    public LibertyX509CertPathValidatorRunnable(Display display, CertPath certPath, int index, String causeMessage, Throwable causeThrowable) {
        display_ = display;
        certPath_ = certPath;
        index_ = index;
        causeMessage_ = causeMessage;
        causeThrowable_ = causeThrowable;
    }

    /**
     * Creates and opens a certificate path validation warning dialog
     * (of type LibertyX509CertPathValidatorDialog) and waits for the dialog
     * to be dismissed by the user with a course of action, namely to either
     * <ol>
     * <li>accept the server's claimed identity for the remainder of the session,</li>
     * <li>accept the server's claimed identify for the life of the workspace, or</li>
     * <li>reject the server's claimed identity.</li>
     * </ol>
     */
    @Override
    public void run() {
        try {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "display_=[" + display_ + //$NON-NLS-1$
                                        "] certPath_=[" + certPath_ + //$NON-NLS-1$
                                        "] index_=[" + index_ + //$NON-NLS-1$
                                        "] causeMessage_=[" + causeMessage_ + //$NON-NLS-1$
                                        "] causeThrowable_=[" + causeThrowable_ + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }

            //
            // Create and open the certificate path dialog, then set
            // resulting status according to how the dialog was dismissed.
            //
            Shell shell = getShell();

            LibertyX509CertPathValidatorDialog dialog = new LibertyX509CertPathValidatorDialog(shell, certPath_, index_, causeMessage_, causeThrowable_);
            int index = -1;
            if (PromptUtil.isRunningGUIMode() && !LibertyX509CertPathValidatorDialog.isOpen) {
                // open dialog to allow user to select whether to trust the certificate or not
                index = dialog.open();
            } else if (Boolean.parseBoolean(Activator.getPreference(HEADLESS_MODE_AUTO_ACCEPT_CERT, "false"))) {
                // in headless mode only trust the certificate if the user has explicitly set the preference
                index = TRANSIENT;
            }
            LibertyX509CertPathValidatorResult.Status status = LibertyX509CertPathValidatorResult.Status.REJECTED;
            switch (index) {
                case TRANSIENT:
                    status = LibertyX509CertPathValidatorResult.Status.VALID_FOR_SESSION;
                    break;
                case PERSISTENT:
                    status = LibertyX509CertPathValidatorResult.Status.VALID_FOR_WORKSPACE;
                    break;
            }
            //
            // Include the untrusted or root certificate in the status.
            //
            List<? extends Certificate> certificates = certPath_.getCertificates();
            int size = certificates.size();
            Certificate certificate = certPath_.getCertificates().get(index_ < 0 || index_ >= size ? size - 1 : index_);
            result_ = new LibertyX509CertPathValidatorResult(certificate, status);
        } catch (Throwable t) {
            //
            // If anything goes wrong, log the exception and return.
            // result_ will remain null, and the certificate path
            // will be rejected by the framework.
            //
            Trace.logError(t.getMessage(), t);
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "result_=[" + result_ + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }
    }

    /**
     * Returns the result of the user's choice.
     *
     * @return The result of the user's choice, possibly null.
     */
    public LibertyX509CertPathValidatorResult result() {
        return result_;
    }

    private Shell getShell() {
        Shell shell = display_.getActiveShell();
        if (shell == null) {
            Shell[] shells = display_.getShells();
            for (Shell sh : shells) {
                if (sh != null)
                    return sh;
            }
        } else
            return shell;
        return new Shell(display_);
    }

}

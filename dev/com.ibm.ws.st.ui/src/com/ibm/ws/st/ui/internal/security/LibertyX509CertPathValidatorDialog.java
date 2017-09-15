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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * This dialog presents basic and detailed information about
 * an X.509 certificate path along with buttons for the user
 * to reject, accept temporarily or accept permanently
 *
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorDialog extends MessageDialog {

    private final Certificate[] certificates_;
    private final int index_;
    private final String causeMessage_;
    private final Throwable causeThrowable_;
    public static boolean isOpen = false;

    private static final String[] BUTTONS = {
                                              Messages.X509_DIALOG_BUTTON_VALID_FOR_SESSION,
                                              Messages.X509_DIALOG_BUTTON_VALID_FOR_WORKSPACE,
                                              Messages.X509_DIALOG_BUTTON_REJECTED
    };

    /**
     * Constructs a new certificate path warning dialog.
     *
     * @param shell The parent shell of the dialog. This must not be null.
     * @param certPath The untrusted certificate path that is the subject
     *            of this warning dialog. Ordinarily this should never be passed in
     *            as null; however, the dialog will gracefully handle a null (or empty)
     *            certificate path by presenting minimal information to the user.
     * @param index The index of the untrusted certificate in the path,
     *            or -1 if there is no specifically untrusted certificate. The index
     *            may be used to draw the user's attention to a particular certificate.
     * @param causeMessage A message with information about the certificate path
     *            being shown to the user, intended but not required to explain why
     *            the certificate path is not trusted. This parameter can be null.
     * @param causeThrowable A Throwable with information about the certificate path
     *            being shown to the user, intended but not required to explain why
     *            the certificate path is not trusted. Messages will be collected from
     *            this Throwable and, up to a fixed limit, any nested Throwables.
     */
    public LibertyX509CertPathValidatorDialog(Shell shell, CertPath certPath, int index, String causeMessage, Throwable causeThrowable) {
        super(shell, Messages.X509_DIALOG_TITLE, null, Messages.X509_DIALOG_MESSAGE, MessageDialog.WARNING, BUTTONS, 2);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        certificates_ = certPath != null ? certPath.getCertificates().toArray(new Certificate[0]) : null;
        index_ = index;
        causeMessage_ = causeMessage;
        causeThrowable_ = causeThrowable;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "parent=[" + parent + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }
        return new LibertyX509CertPathValidatorControl(parent, SWT.NONE, certificates_, index_, causes());
    }

    //
    // Organizes the "causeMsg" string (if any) and "cause" Throwable (if any)
    // into an array of message strings. Just in case somebody introduces a
    // cycle into a chain of Throwables, this method places a limit on how
    // deeply it will traverse the "cause" Throwable.
    //
    private String[] causes() {
        List<String> causes = new LinkedList<String>();
        if (causeMessage_ != null) {
            causes.add(causeMessage_);
        }
        Throwable causeExc = causeThrowable_;
        int limit = 20;
        while (causeExc != null && limit > 0) {
            causes.add(causeExc.getLocalizedMessage());
            causeExc = causeExc.getCause();
            limit--;
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "causes=[" + causes + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }
        return causes.toArray(new String[0]);
    }

    /** {@inheritDoc} */
    @Override
    public int open() {
        isOpen = true;
        return super.open();
    }

    /** {@inheritDoc} */
    @Override
    public boolean close() {
        isOpen = false;
        return super.close();
    }
}

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

import java.security.cert.Certificate;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * This kind of Composite displays information about a certificate
 * path along with messages, if any, about the status of the path.
 * It contains the following controls arranged in a single columns:
 * <ol>
 * <li>An LibertyX509CertPathValidatorDetailsControl with detailed
 * information about the certificates in the path and their attributes.</li>
 * <li>A series of messages pertinent to the certificate path, intended
 * but not required to explain the reasons why the certificate path is
 * not trusted.</li>
 * </ol>
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorControl extends Composite {

    public LibertyX509CertPathValidatorControl(Composite parent, int style, Certificate[] certificates, int index, String[] causes) {
        super(parent, style);

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "parent=[" + parent + //$NON-NLS-1$
                                    "] style=[" + style + //$NON-NLS-1$
                                    "] certificates=[" + Arrays.toString(certificates) + //$NON-NLS-1$
                                    "] index=" + index + //$NON-NLS-1$
                                    "] causes=" + Arrays.toString(causes) + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }

        //
        // This control has a single column consisting of a
        // certificate path details control (omitted if
        // certificates is null) and a list of cause messages,
        // if any. Notice we provide a width hint to keep the
        // initial size of the dialog in check.
        //
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        setLayout(gridLayout);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = 512;
        setLayoutData(gridData);
        //
        // The certificate path details control.
        //
        if (certificates != null) {
            new LibertyX509CertPathValidatorDetailsControl(this, SWT.NONE, certificates, index);
        }
        //
        // The list of detailed cause messages, if any.
        //
        if (causes.length > 0) {
            Label causeLabel = new Label(this, SWT.NONE);
            causeLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            causeLabel.setText(Messages.X509_CONTROL_CERTIFICATE_CAUSE_LABEL);
            List causeList = new List(this, SWT.MULTI);
            causeList.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            for (String cause : causes) {
                causeList.add(cause);
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "return"); //$NON-NLS-1$
        }
    }
}

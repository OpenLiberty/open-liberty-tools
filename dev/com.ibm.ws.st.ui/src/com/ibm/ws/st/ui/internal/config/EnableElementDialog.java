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
package com.ibm.ws.st.ui.internal.config;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Dialog that allows the user to enable an element by adding the appropriate feature.
 */
public class EnableElementDialog extends FeatureSelectionDialog {
    public EnableElementDialog(Shell parent, WebSphereRuntime wsRuntime, String elemName, List<String> possibleFeatures) {
        super(parent, wsRuntime, elemName, possibleFeatures, Messages.enableElementTitle, Messages.enableElementMessage, Messages.enableElementLabel);
    }
}

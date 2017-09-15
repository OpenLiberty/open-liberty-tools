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
package com.ibm.ws.st.ui.internal.marker;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.ui.internal.Messages;

/**
 * Quick fix for out of sync published applications
 * 
 * Add missing <application> element to root configuration file
 */
public class QuickFixUpdateSharedLibRef extends QuickFixAddApplicationElement {

    final String label;

    public QuickFixUpdateSharedLibRef(String appName) {
        super(appName);
        label = NLS.bind(Messages.updateSharedLibRefLabel, appName);
    }

    @Override
    public String getLabel() {
        return label;
    }
}

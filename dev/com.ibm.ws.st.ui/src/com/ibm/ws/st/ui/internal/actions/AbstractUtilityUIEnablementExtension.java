/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 *
 */
public class AbstractUtilityUIEnablementExtension {
    public boolean notifyUtilityDisabled(String utilityId, WebSphereServer wsServer, WebSphereUtilityAction action, Shell shell, String serverType, String utilityType) {
        return false;
    }
}

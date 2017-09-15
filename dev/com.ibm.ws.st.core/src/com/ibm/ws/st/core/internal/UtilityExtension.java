/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.Map;

import com.ibm.ws.st.core.internal.launch.IUtilityExecutionDelegate;
import com.ibm.ws.st.core.internal.launch.RemoteUtility;

/**
 * Base class for utility extensions. Currently only provides a method to get
 * the remote utility since these are referenced directly from the
 * UtilityLaunchConfigurationDelegate.
 */
public abstract class UtilityExtension {

    public abstract RemoteUtility getRemoteUtility(Map<String, String> commandVariables, int timeout, IUtilityExecutionDelegate delegate);

}

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
package com.ibm.ws.st.ui.internal.download;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.WebSphereRuntime;

/**
 *
 */
public interface IRuntimeHandler {

    public void setLocation(IPath location);

    public void setRuntimeName(String name);

    public ValidationResult validateRuntime(boolean isEmptyFolderCheck);

    public String getRuntimeName();

    public String getRuntimeTypeId();

    public IPath getLocation();

    public WebSphereRuntime getRuntime();
}

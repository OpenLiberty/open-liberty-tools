/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.wst.server.core.IRuntime;

/**
 * This extension point provides a way for extenders to override the debug target
 * used when when a Liberty server is started in debug mode.
 */
public abstract class AbstractLibertyDebugFactory {
    /**
     * Create a debug target
     * 
     * @param launch the launch that the debug target is created on
     * @param hostName the host name of the debug process
     * @param debugPortStr the debug port number of the debug process
     * @param debugTargetLabel the label of the debug target
     * @param runtime the runtime of the server that the debug target is created on.
     * @return the debug target
     * @throws CoreException when there are error during WSA debug target exception.
     */
    public abstract IDebugTarget createDebugTarget(ILaunch launch, String hostName, String debugPortStr, String debugTargetLabel, IRuntime runtime) throws CoreException;

}

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
package com.ibm.ws.st.common.core.ext.internal.servertype;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

/**
 * Allows a server type to specify its own server launch configuration
 */
public abstract class AbstractLaunchConfigurationExtension extends AbstractJavaLaunchConfigurationDelegate {
    @Override
    public abstract void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException;

    /**
     * Sets up a server that is already started prior to calling this method.
     *
     * @param launchMode the launch mode
     * @param serverBehaviour the server behaviour
     * @throws CoreException
     */
    public abstract void launchStartedServer(String launchMode, ServerBehaviourDelegate serverBehaviour) throws CoreException;
}

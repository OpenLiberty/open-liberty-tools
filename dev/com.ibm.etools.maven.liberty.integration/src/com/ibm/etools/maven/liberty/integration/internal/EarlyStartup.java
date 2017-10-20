/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.etools.maven.liberty.integration.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

import com.ibm.etools.maven.liberty.integration.manager.internal.LibertyManager;

public class EarlyStartup implements IStartup {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    @Override
    public void earlyStartup() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(LibertyManager.getInstance(), IResourceChangeEvent.POST_CHANGE);
    }

}

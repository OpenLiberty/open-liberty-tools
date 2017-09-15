/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import org.eclipse.core.runtime.IPath;

/**
 * Marker element for configuration files and folders.
 */
public interface IConfigurationElement {
    /**
     * Return the name of the configuration element.
     */
    public String getName();

    /**
     * Returns the path of the configuration element.
     */
    public IPath getPath();
}

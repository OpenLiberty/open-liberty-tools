/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.net.URI;
import java.util.HashSet;

/**
 * A filter for configuration files to prevent recursive includes
 */
public class ConfigurationIncludeFilter implements IncludeFilter {

    private final HashSet<URI> uriSet = new HashSet<URI>();

    @Override
    public boolean accept(URI includeURI) {
        return uriSet.add(includeURI);
    }
}

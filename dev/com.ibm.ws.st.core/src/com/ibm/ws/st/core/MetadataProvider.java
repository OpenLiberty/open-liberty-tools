/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core;

import java.net.URL;

/**
 * Base class for metadata provider extensions
 */
public abstract class MetadataProvider {

    public URL getDefaultFeatureList() {
        return null;
    }

    public URL getDefaultSchema() {
        return null;
    }

}

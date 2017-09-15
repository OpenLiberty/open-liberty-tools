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

package com.ibm.ws.st.docker.core.internal;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;

/**
 * Abstract handler class for flattening an image if it is getting large.
 */
public abstract class AbstractFlattenImageHandler {

    public abstract boolean handleFlattenImage(BaseDockerContainer container, String newImageName);

}

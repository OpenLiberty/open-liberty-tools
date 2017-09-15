/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.repository;

import java.io.IOException;
import java.io.InputStream;

public interface IRemoteSource extends ISource {

    /**
     * 
     * @return the input stream of the remote archive
     * 
     * @throws Exception
     */
    public InputStream getInputStream() throws IOException;
}

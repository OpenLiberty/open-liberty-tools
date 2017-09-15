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

package com.ibm.ws.st.common.core.ext.internal.producer;

import java.util.Map;

import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

public abstract class AbstractServerProducer {

    public abstract IServer createServer(IRuntime rt, Map<String, String> serverInfo) throws ServerCreationException;

}

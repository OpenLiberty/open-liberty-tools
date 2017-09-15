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
package com.ibm.ws.st.core.internal;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

public interface IPromptActionHandler {

    public void prePromptAction(List<IModule[]> publishedModules, PublishHelper helper, IProgressMonitor monitor);

    public void postPromptAction(IPromptResponse response, PublishHelper helper);

}

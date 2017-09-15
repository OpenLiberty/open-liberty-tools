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
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.server.ui.ServerUICore;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;

public class ResourceAdapterFactory implements IAdapterFactory {
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IResource.class) {
            ConfigurationFile file = (ConfigurationFile) adaptableObject;
            return file.getIFile();
        }

        if (adapterType == IWorkbenchAdapter.class)
            return ServerUICore.getLabelProvider();

        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] { IResource.class, IWorkbenchAdapter.class };
    }
}

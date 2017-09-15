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

package com.ibm.ws.st.common.core.ext.internal;

import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;

public class ServerSetupFactory {

    public static AbstractServerSetup getServerSetup(String serviceType, Map<String, String> serviceInfo, IRuntime runtime) throws UnsupportedServiceException, Exception {
        AbstractServerSetup setup = Activator.getServerSetup(serviceType);
        if (setup == null)
            throw new UnsupportedServiceException(NLS.bind(Messages.errorNoServerSetupFound, serviceType));
        setup.initialize(serviceType, serviceInfo, runtime);
        return setup;
    }
}

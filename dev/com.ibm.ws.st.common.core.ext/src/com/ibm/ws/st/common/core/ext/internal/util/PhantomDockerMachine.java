/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal.util;

import java.util.Map;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;

/**
 * A phantom docker machine for platforms that don't require a docker
 * machine (such as Linux).
 */
public class PhantomDockerMachine extends AbstractDockerMachine {

    public PhantomDockerMachine(IPlatformHandler platformHandler) {
        super(platformHandler);
    }

    /** {@inheritDoc} */
    @Override
    public String getMachineName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MachineType getMachineType() {
        return MachineType.PHANTOM;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRealMachine() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getHost() throws Exception {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getDockerEnv() throws Exception {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Phantom docker machine";
    }

}

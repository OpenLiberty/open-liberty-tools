/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.profiling;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 *
 */
public class ProfileExtension extends org.eclipse.jst.server.core.ServerProfilerDelegate {

    public static final String ARG = "-DtestProfilingArg=test";

    /** {@inheritDoc} */
    @Override
    public void process(ILaunch launch, IVMInstall vmInstall, VMRunnerConfiguration vmConfig, IProgressMonitor monitor) throws CoreException {

        ArrayList<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(vmConfig.getVMArguments()));
        args.add(ARG);
        vmConfig.setVMArguments(args.toArray(new String[args.size()]));
    }

}

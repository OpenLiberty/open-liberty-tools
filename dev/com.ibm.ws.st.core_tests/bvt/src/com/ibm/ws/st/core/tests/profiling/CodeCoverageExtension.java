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

import java.util.List;

import com.ibm.ws.st.core.internal.launch.AbstractServerStartupExtension;
import com.ibm.ws.st.core.internal.launch.ServerStartInfo;

/**
 *
 */
public class CodeCoverageExtension extends AbstractServerStartupExtension {

    public static final String ARG = "-DtestCodeCoverageArg=test";
    public static Boolean isProfiling = null;

    /** {@inheritDoc} */
    @Override
    public void setJVMOptions(ServerStartInfo serverStartInfo, List<String> additionalJVMOptions) {
        additionalJVMOptions.add(ARG);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean isProfiling(ServerStartInfo serverStartInfo, List<String> startupJVMOptions) {
        return isProfiling;
    }

}

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
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.TaskModel;

/**
 * Interface for server type extensions to the new server wizard. Specifically
 * for the initial composite.
 */
public interface IServerWizardComposite {

    public void setup(TaskModel taskModel);

    public void validate();

    public boolean isComplete();

    public void performFinish(IProgressMonitor monitor) throws CoreException;

    public Composite getComposite();

    // To reinitialize the composite when configurations such as runtime has changed.
    public void reInitialize();

}

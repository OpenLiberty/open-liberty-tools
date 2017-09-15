/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * This is for importing exiting projects into workspace
 * The ImportOperation references to some classes that is not exported, so we will
 * have compile errors when we call the run() method. However, if we use required
 * bundle, then there is no problems.
 * 
 * So, I extended the class and have my own myRun() method to avoid the problem.
 */
public class MyImportOperation extends ImportOperation {
    public MyImportOperation(IPath containerPath, Object source,
                             IImportStructureProvider provider,
                             IOverwriteQuery overwriteImplementor, List<?> filesToImport) {
        super(containerPath, source, provider, overwriteImplementor,
                filesToImport);
    }

    public synchronized final void myRun(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
        final InvocationTargetException[] iteHolder = new InvocationTargetException[1];
        try {
            execute(monitor);
        } catch (OperationCanceledException e) {
            throw new InterruptedException(e.getMessage());
        }
        // Re-throw the InvocationTargetException, if any occurred
        if (iteHolder[0] != null) {
            throw iteHolder[0];
        }
    }
}
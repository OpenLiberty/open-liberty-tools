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

package com.ibm.ws.st.docker.ui.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.docker.core.internal.AbstractFlattenImageHandler;

/**
 *
 */
public class FlattenImageHandler extends AbstractFlattenImageHandler {

    /** {@inheritDoc} */
    @Override
    public boolean handleFlattenImage(final BaseDockerContainer container, final String newImageName) {
        final Boolean[] isFlattened = new Boolean[1];
        isFlattened[0] = Boolean.FALSE;
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    final String currentImage = container.getImageName();
                    final Shell shell = Display.getDefault().getActiveShell();
                    boolean flatten;
                    if (!PromptUtil.isSuppressDialog()) {
                        flatten = MessageDialog.openQuestion(shell, Messages.dockerFlattenImageTitle,
                                                             NLS.bind(Messages.dockerFlattenImageMessage, currentImage));
                    } else {
                        // Running headlessly
                        System.out.println("Suppressing flattening dialog; image " + currentImage + " will be flattened.");
                        flatten = true;
                    }

                    if (flatten) {
                        new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                            @Override
                            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                try {
                                    isFlattened[0] = Boolean.valueOf(container.flatten(newImageName, monitor));
                                } catch (final Throwable t) {
                                    Display.getDefault().asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            MessageDialog.openError(shell, NLS.bind(Messages.dockerFlattenImageError, currentImage), t.getLocalizedMessage());
                                        }
                                    });
                                    throw new InvocationTargetException(t);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Trace.logError("Failed to flatten the image for the " + container.getContainerName() + " container.", e);
                }
            }
        });

        return isFlattened[0].booleanValue();
    }

}

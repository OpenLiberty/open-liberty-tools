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
package com.ibm.ws.st.common.ui.ext.internal.servertype;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * Abstract class for a wizard fragment provider associated with the
 * server type UI extension.
 */
public abstract class WizardFragmentProvider {

    /**
     * Return whether this provider is active based on the
     * information collected so far in taskModel.
     *
     * @param taskModel The wizard TaskModel.
     * @return True if fragments are available, false otherwise.
     */
    public abstract boolean isActive(TaskModel taskModel);

    /**
     * Get the initial composite.
     *
     * @param parent The parent composite
     * @param handle The wizard handle
     * @param taskModel The task model
     * @return
     */
    public abstract Composite getInitialComposite(Composite parent, IWizardHandle handle, TaskModel taskModel);

    /**
     * Get the fragments for this provider not including the initial composite.
     *
     * @return The fragments following the initial composite.
     */
    public abstract List<WizardFragment> getFollowingFragments();

}

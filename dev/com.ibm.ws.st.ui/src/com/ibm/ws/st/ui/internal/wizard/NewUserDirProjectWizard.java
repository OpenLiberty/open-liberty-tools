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
package com.ibm.ws.st.ui.internal.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class NewUserDirProjectWizard extends Wizard {
    protected NewUserDirProjectWizardPage page;
    protected IProject createdProject;

    public NewUserDirProjectWizard() {
        super();
        setWindowTitle(Messages.wizNewUserProjectTitle);
        setDefaultPageImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_RUNTIME));
        setNeedsProgressMonitor(true);
    }

    private String getProjectName() {
        return page.getProjectName();
    }

    private IPath getProjectLocation() {
        IPath location = page.getLocationPath();

        if (location != null && location.equals(ResourcesPlugin.getWorkspace().getRoot().getLocation()))
            location = null;

        return location;
    }

    protected IProject getProject() {
        return createdProject;
    }

    @Override
    public void addPages() {
        page = new NewUserDirProjectWizardPage();
        addPage(page);
        super.addPages();
    }

    @Override
    public boolean performFinish() {
        try {
            final String projectName = getProjectName();
            final IPath location = getProjectLocation();
            final IWorkingSet[] workingSets = page.getSelectedWorkingSets();
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        createdProject = WebSphereUtil.createUserProject(projectName, location, monitor);
                        PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(createdProject, workingSets);
                    } catch (CoreException ce) {
                        Trace.logError("Error creating user project: " + projectName, ce);
                        throw new InvocationTargetException(ce);
                    }
                }
            });
        } catch (Exception e) {
            Trace.logError("Error creating project: " + getProjectName(), e);
            return false;
        }

        return true;
    }
}

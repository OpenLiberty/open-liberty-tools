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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 * Wizard to create a new liberty profile configuration file resource.
 */
public class NewConfigFileWizard extends Wizard implements INewWizard {

    private IStructuredSelection selection;
    private NewConfigFilePage mainPage;

    /** {@inheritDoc} */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
        setWindowTitle(Messages.wizNewConfigFileTitle);
    }

    /** {@inheritDoc} */
    @Override
    public void addPages() {
        mainPage = new NewConfigFilePage(selection);
        addPage(mainPage);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performFinish() {
        return mainPage.finish();
    }

    private static class NewConfigFilePage extends WizardNewFileCreationPage {
        public NewConfigFilePage(IStructuredSelection selection) {
            super("NewConfigFilePage", selection);
            setTitle(Messages.wizNewConfigFilePageTitle);
            setDescription(Messages.wizNewConfigFilePageDesc);
            setFileExtension("xml");
            setAppropriatePath(selection);
        }

        /** {@inheritDoc} */
        @Override
        protected InputStream getInitialContents() {
            return new ByteArrayInputStream(Constants.INITIAL_CONFIG_CONTENT.getBytes());
        }

        public boolean finish() {
            // create the new config file
            IFile newFile = createNewFile();
            if (newFile == null)
                return false;

            Activator.openConfigurationEditor(newFile, newFile.getLocation().toFile().toURI());
            return true;
        }

        private void setAppropriatePath(IStructuredSelection selection) {
            // If the selection is a project, try to find a server
            // folder in the project.  If no server folders try the shared
            // config folder.
            Object obj = selection.getFirstElement();
            if (obj instanceof IProject) {
                IProject project = (IProject) obj;
                WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                for (WebSphereServerInfo server : servers) {
                    IFolder folder = server.getServerFolder();
                    if (folder != null && folder.exists() && folder.getProject().equals(project)) {
                        setContainerFullPath(folder.getFullPath());
                        return;
                    }
                    folder = server.getUserDirectory().getSharedConfigFolder();
                    if (folder != null && folder.exists() && folder.getProject().equals(project)) {
                        setContainerFullPath(folder.getFullPath());
                        return;
                    }
                }
            }
        }
    }
}

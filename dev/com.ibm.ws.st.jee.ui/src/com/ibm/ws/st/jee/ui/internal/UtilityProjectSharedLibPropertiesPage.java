/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.ui.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.jee.core.internal.JEEServerExtConstants;
import com.ibm.ws.st.jee.core.internal.SharedLibRefInfo;
import com.ibm.ws.st.jee.core.internal.SharedLibertyUtils;

@SuppressWarnings("restriction")
public class UtilityProjectSharedLibPropertiesPage extends PropertyPage {
    protected Text libIdText;
    protected Text libDirectoryText;
    protected IProject project;
    protected Properties settings = new Properties();
    protected String libId = "";
    protected String libDirectory = "";
    protected Button apiVisibilityCheckboxAPI;
    protected Button apiVisibilityCheckboxIBMAPI;
    protected Button apiVisibilityCheckboxSpec;
    protected Button apiVisibilityCheckboxStable;
    protected Button apiVisibilityCheckboxThirdParty;
    protected EnumSet<APIVisibility> apiVisibility;

    @Override
    protected Control createContents(Composite parent) {
        loadSettings();

        Composite composite = new Composite(parent, SWT.NULL);

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
        composite.setLayoutData(data);

        Label label = new Label(composite, SWT.WRAP);
        label.setText(Messages.sharedLibDescription);
        data = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
        data.horizontalSpan = 3;
        label.setLayoutData(data);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.sharedLibId);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.verticalIndent = 8;
        label.setLayoutData(data);
        libIdText = new Text(composite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        data.verticalIndent = 8;
        libIdText.setLayoutData(data);
        libIdText.setText(libId);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.sharedLibDirectory);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        label.setLayoutData(data);
        libDirectoryText = new Text(composite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        libDirectoryText.setLayoutData(data);
        libDirectoryText.setText(libDirectory);

        Button browse = SWTUtil.createButton(composite, Messages.browse);
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setMessage(Messages.sharedLibBrowseMessage);
                dialog.setFilterPath(libDirectoryText.getText());
                String selectedDirectory = dialog.open();
                if (selectedDirectory != null)
                    libDirectoryText.setText(selectedDirectory);
            }
        });

        Group group = new Group(composite, SWT.SHADOW_ETCHED_IN);
        group.setText(Messages.sharedLibAPIVisibilityLabel);
        data = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        data.horizontalSpan = 3;
        data.minimumWidth = 130;
        group.setLayoutData(data);
        group.setLayout(new GridLayout());

        SelectionListener selectionListener = new SelectionAdapter() {

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                setValid(isValid());
            }

        };

        apiVisibilityCheckboxAPI = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxAPI.setText(Messages.sharedLibAPIVisibilityAPI);
        apiVisibilityCheckboxAPI.addSelectionListener(selectionListener);

        apiVisibilityCheckboxIBMAPI = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxIBMAPI.setText(Messages.sharedLibAPIVisibilityIBMAPI);
        apiVisibilityCheckboxIBMAPI.addSelectionListener(selectionListener);

        apiVisibilityCheckboxSpec = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxSpec.setText(Messages.sharedLibAPIVisibilitySpec);
        apiVisibilityCheckboxSpec.addSelectionListener(selectionListener);

        apiVisibilityCheckboxStable = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxStable.setText(Messages.sharedLibAPIVisibilityStable);
        apiVisibilityCheckboxStable.addSelectionListener(selectionListener);

        apiVisibilityCheckboxThirdParty = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxThirdParty.setText(Messages.sharedLibAPIVisibilityThirdParty);
        apiVisibilityCheckboxThirdParty.addSelectionListener(selectionListener);

        updateAPIVisibilityCheckboxValues(apiVisibility);

        return composite;
    }

    protected void updateAPIVisibilityCheckboxValues(EnumSet<APIVisibility> apiVisibility) {
        apiVisibilityCheckboxAPI.setSelection(apiVisibility.contains(APIVisibility.API));
        apiVisibilityCheckboxIBMAPI.setSelection(apiVisibility.contains(APIVisibility.IBM_API));
        apiVisibilityCheckboxSpec.setSelection(apiVisibility.contains(APIVisibility.SPEC));
        apiVisibilityCheckboxStable.setSelection(apiVisibility.contains(APIVisibility.STABLE));
        apiVisibilityCheckboxThirdParty.setSelection(apiVisibility.contains(APIVisibility.THIRD_PARTY));
    }

    protected void loadSettings() {
        IAdaptable element = getElement();
        project = element.getAdapter(IProject.class);

        if (project == null)
            return;

        settings = SharedLibertyUtils.getUtilPrjSharedLibInfo(project);

        libId = settings.getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, "");
        libDirectory = settings.getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, "");

        apiVisibility = APIVisibility.getAPIVisibilityFromProperties(settings);

    }

    protected void saveSettings() {
        if (project == null)
            return;

        IPath path = project.getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH);
        SharedLibertyUtils.saveSettings(settings, path);
    }

    @Override
    public boolean isValid() {
        return apiVisibilityCheckboxAPI.getSelection() || apiVisibilityCheckboxIBMAPI.getSelection() || apiVisibilityCheckboxSpec.getSelection()
               || apiVisibilityCheckboxStable.getSelection() || apiVisibilityCheckboxThirdParty.getSelection();
    }

    @Override
    public boolean performOk() {
        String id = libIdText.getText().trim();
        String dir = libDirectoryText.getText().trim();

        boolean apiVisibilityChanged = apiVisibilityChanged();

        boolean isIdDifferent = !libId.equals(id);
        if (apiVisibilityChanged || isIdDifferent || !libDirectory.equals(dir)) {
            boolean isNewIdEmpty = id.isEmpty();
            boolean isPublished = SharedLibertyUtils.isPublished(project);
            if (isNewIdEmpty && isPublished) {
                if (!MessageDialog.openConfirm(getShell(), Messages.sharedLibConfirmRemoveTitle, NLS.bind(Messages.sharedLibConfirmRemoveMessage, project.getName())))
                    return false;
            }
            MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, null, null);
            List<IServer> publishServers = new ArrayList<IServer>();

            // remove the shared library from server
            if (isPublished)
                remove(project, publishServers, status);

            // save the new changes
            settings.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, id);
            settings.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, dir);

            if (apiVisibilityChanged) {
                settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_API_KEY, Boolean.toString(apiVisibilityCheckboxAPI.getSelection()));
                settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_IBM_API_KEY, Boolean.toString(apiVisibilityCheckboxIBMAPI.getSelection()));
                settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_SPEC_KEY, Boolean.toString(apiVisibilityCheckboxSpec.getSelection()));
                settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_STABLE_KEY, Boolean.toString(apiVisibilityCheckboxStable.getSelection()));
                settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_THIRD_PARTY_KEY, Boolean.toString(apiVisibilityCheckboxThirdParty.getSelection()));
            }

            saveSettings();

            // find any other modules that use the shared library so we can
            // update their information in the server configuration
            IProject[] refProjects = (isIdDifferent) ? updateSharedLibRef(libId, id) : new IProject[0];
            List<IServer> allServers = new ArrayList<IServer>(publishServers);

            // update server configuration
            IProject sharedLibProject = (isPublished && !isNewIdEmpty) ? project : null;
            if (refProjects.length > 0 || sharedLibProject != null)
                update(sharedLibProject, refProjects, publishServers, allServers, status);

            // request a publish if the servers allow it
            if (!allServers.isEmpty())
                publish(allServers);

            if (!status.isOK()) {
                MessageDialog.openError(getShell(), Messages.sharedLibServerUpdateFailedTitle, Messages.sharedLibServerUpdateFailedMessage);
                Trace.logError(status.toString(), null);
            }
        }
        return true;
    }

    protected boolean apiVisibilityChanged() {
        if (apiVisibilityCheckboxAPI.getSelection() != apiVisibility.contains(APIVisibility.API)
            || apiVisibilityCheckboxIBMAPI.getSelection() != apiVisibility.contains(APIVisibility.IBM_API)
            || apiVisibilityCheckboxSpec.getSelection() != apiVisibility.contains(APIVisibility.SPEC)
            || apiVisibilityCheckboxStable.getSelection() != apiVisibility.contains(APIVisibility.STABLE)
            || apiVisibilityCheckboxThirdParty.getSelection() != apiVisibility.contains(APIVisibility.THIRD_PARTY)) {
            return true;
        }
        return false;
    }

    @Override
    public void performDefaults() {
        libIdText.setText(libId);
        libDirectoryText.setText(libDirectory);
        updateAPIVisibilityCheckboxValues(APIVisibility.getDefaults());
    }

    private static void remove(IProject project, List<IServer> publishServers, MultiStatus status) {
        IServer[] servers = ServerCore.getServers();
        for (IServer server : servers) {
            IModule[] modules = server.getModules();
            IModule publishedModule = null;
            for (IModule module : modules) {
                if (module.getProject() != null && project.equals(module.getProject())) {
                    publishedModule = module;
                    break;
                }
            }

            if (publishedModule != null) {
                IServerWorkingCopy wc = server.createWorkingCopy();
                try {
                    wc.modifyModules(null, new IModule[] { publishedModule }, null);
                    wc.save(true, null);
                    publishServers.add(server);
                } catch (CoreException ce) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to modify modules for server " + server.getName(), ce));
                }
            }
        }
    }

    private static void update(IProject project, IProject[] refProjects, List<IServer> publishServers, List<IServer> allServers, MultiStatus status) {
        IModule sharedLibModule = (project == null) ? null : ServerUtil.getModule(project);
        IServer[] servers = ServerCore.getServers();
        for (IServer server : servers) {
            List<IModule> addList = new ArrayList<IModule>(refProjects.length + 1);
            // If the shared library id is not empty, we need to re-add it
            // to the server configuration, since it was removed in an
            // earlier operation.
            if (sharedLibModule != null && publishServers != null && publishServers.contains(server)) {
                addList.add(sharedLibModule);
            }
            IModule[] modules = server.getModules();
            for (IModule module : modules) {
                // build the list of projects that have a reference to the
                // updated shared library, so we can modify the server
                // configuration.
                for (IProject refProject : refProjects) {
                    if (module.getProject() != null && refProject.equals(module.getProject())) {
                        addList.add(module);
                        break;
                    }
                }
            }

            if (!addList.isEmpty()) {
                IModule[] add = addList.toArray(new IModule[addList.size()]);
                IServerWorkingCopy wc = server.createWorkingCopy();
                try {
                    wc.modifyModules(add, null, null);
                    wc.save(true, null);
                    if (!allServers.contains(server))
                        allServers.add(server);
                } catch (CoreException ce) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to modify modules on server " + server.getName(), ce));
                }
            }
        }
    }

    private static IProject[] updateSharedLibRef(String oldId, String newId) {
        if (oldId == null || oldId.isEmpty())
            return new IProject[0];

        List<IProject> projectList = new ArrayList<IProject>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (IProject project : projects) {
            if (SharedLibertyUtils.hasSharedLibSettingsFile(project, JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH)) {
                SharedLibRefInfo refInfo = SharedLibertyUtils.getSharedLibRefInfo(project);
                List<String> sharedLibRefIds = refInfo.getLibRefIds();
                int index = sharedLibRefIds.indexOf(oldId);
                if (index >= 0) {
                    if (newId == null || newId.isEmpty())
                        sharedLibRefIds.remove(oldId);
                    else
                        sharedLibRefIds.set(index, newId);
                    projectList.add(project);
                    refInfo.setLibRefIds(sharedLibRefIds);
                    SharedLibertyUtils.saveSettings(refInfo, project.getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH));
                }
            }
        }

        projects = new IProject[projectList.size()];
        return projectList.toArray(projects);
    }

    static void publish(List<IServer> servers) {
        for (IServer server : servers) {
            // request a publish if the server allows auto publish
            // we are casting to the internal server to eliminate using
            // hard coded values
            if (!(server instanceof Server)) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Expecting server " + server.getName() + " to be an instance of org.eclipse.wst.server.core.internal.Server");
                continue;
            }
            Server s = (Server) server;
            s.setServerPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
        }
    }
}

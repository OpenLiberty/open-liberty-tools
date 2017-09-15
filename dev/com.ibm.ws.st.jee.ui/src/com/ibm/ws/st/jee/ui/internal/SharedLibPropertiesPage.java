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
package com.ibm.ws.st.jee.ui.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.jee.core.internal.JEEServerExtConstants;
import com.ibm.ws.st.jee.core.internal.SharedLibRefInfo;
import com.ibm.ws.st.jee.core.internal.SharedLibertyUtils;

@SuppressWarnings("restriction")
public class SharedLibPropertiesPage extends PropertyPage {
    protected IProject project;
    protected SharedLibRefInfo settings = new SharedLibRefInfo();
    protected List<String> libRefIds, defaultLibRefIds;
    protected Table libTable;
    protected Button apiVisibilityCheckboxAPI;
    protected Button apiVisibilityCheckboxIBMAPI;
    protected Button apiVisibilityCheckboxSpec;
    protected Button apiVisibilityCheckboxThirdParty;
    EnumSet<APIVisibility> apiVisibility;

    @Override
    protected Control createContents(Composite parent) {
        loadSettings();

        Composite composite = new Composite(parent, SWT.NULL);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
        composite.setLayoutData(data);

        Label label = new Label(composite, SWT.WRAP);
        label.setText(Messages.sharedLibReferences);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.widthHint = 200;
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        libTable = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 100;
        data.verticalSpan = 2;
        libTable.setLayoutData(data);

        resetTable();

        final Button add = SWTUtil.createButton(composite, Messages.add);
        add.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                SharedLibSelectionDialog dialog = new SharedLibSelectionDialog(getShell(), libRefIds);
                if (dialog.open() == Window.OK) {
                    String id = dialog.getId();
                    libRefIds.add(id);
                    TableItem item = new TableItem(libTable, SWT.NONE);
                    item.setText(id);
                    item.setData(id);
                    item.setImage(Activator.getImage(Activator.IMG_LIBRARY));
                }
            }
        });

        final Button remove = SWTUtil.createButton(composite, Messages.remove);
        remove.setEnabled(false);
        remove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int[] indices = libTable.getSelectionIndices();
                if (indices == null || indices.length == 0) {
                    return;
                }

                for (int ind : indices) {
                    final TableItem item = libTable.getItem(ind);
                    libRefIds.remove(item.getData());
                }
                libTable.remove(indices);
                remove.setEnabled(false);
            }
        });

        libTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int ind = libTable.getSelectionIndex();
                remove.setEnabled(ind >= 0);
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

        apiVisibilityCheckboxThirdParty = new Button(group, SWT.CHECK);
        apiVisibilityCheckboxThirdParty.setText(Messages.sharedLibAPIVisibilityThirdParty);
        apiVisibilityCheckboxThirdParty.addSelectionListener(selectionListener);

        updateAPIVisibilityCheckboxeValues(apiVisibility);

        return composite;
    }

    protected void loadSettings() {
        IAdaptable element = getElement();
        project = element.getAdapter(IProject.class);

        if (project == null)
            return;

        settings = SharedLibertyUtils.getSharedLibRefInfo(project);

        libRefIds = settings.getLibRefIds();
        defaultLibRefIds = new ArrayList<String>(libRefIds);

        apiVisibility = APIVisibility.getAPIVisibilityFromProperties(settings);

    }

    protected void saveSettings() {
        if (project == null)
            return;

        IPath path = project.getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH);
        SharedLibertyUtils.saveSettings(settings, path);
        final IModule module = ServerUtil.getModule(project);
        if (module != null) {
            IServer[] servers = ServerUtil.getServersByModule(module, null);
            for (IServer server : servers) {
                try {
                    WebSphereServerBehaviour serverB = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
                    if (serverB != null)
                        serverB.setModulePublishState(IServer.PUBLISH_STATE_INCREMENTAL, new IModule[] { module });
                    // We are casting to the internal server to eliminate using
                    // hard coded values
                    if (!(server instanceof Server)) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Expecting server " + server.getName() + " to be an instance of org.eclipse.wst.server.core.internal.Server");
                        continue;
                    }
                    Server s = (Server) server;
                    if (s.getAutoPublishSetting() != Server.AUTO_PUBLISH_DISABLE && server.getServerState() == IServer.STATE_STARTED)
                        server.publish(IServer.PUBLISH_AUTO, null, null, null);
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to update publish state for " + module.getName(), e);
                }
            }
        }
    }

    protected void resetTable() {
        libTable.select(-1);
        libTable.removeAll();
        for (String s : libRefIds) {
            TableItem item = new TableItem(libTable, SWT.NONE);
            item.setText(s);
            item.setData(s);
            item.setImage(Activator.getImage(Activator.IMG_LIBRARY));
        }
    }

    @Override
    public boolean isValid() {
        return apiVisibilityCheckboxAPI.getSelection() || apiVisibilityCheckboxIBMAPI.getSelection() || apiVisibilityCheckboxSpec.getSelection()
               || apiVisibilityCheckboxThirdParty.getSelection();
    }

    @Override
    public boolean performOk() {

        boolean apiVisibilityChanged = apiVisibilityChanged();
        boolean sharedLibraryChanged = !libRefIds.equals(defaultLibRefIds);

        if (apiVisibilityChanged) {
            settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_API_KEY, Boolean.toString(apiVisibilityCheckboxAPI.getSelection()));
            settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_IBM_API_KEY, Boolean.toString(apiVisibilityCheckboxIBMAPI.getSelection()));
            settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_SPEC_KEY, Boolean.toString(apiVisibilityCheckboxSpec.getSelection()));
            settings.setProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_THIRD_PARTY_KEY, Boolean.toString(apiVisibilityCheckboxThirdParty.getSelection()));
        }

        if (sharedLibraryChanged) {
            settings.setLibRefIds(libRefIds);
        }

        if (sharedLibraryChanged || apiVisibilityChanged) {
            saveSettings();
        }
        return true;
    }

    protected boolean apiVisibilityChanged() {
        if (apiVisibilityCheckboxAPI.getSelection() != apiVisibility.contains(APIVisibility.API)
            || apiVisibilityCheckboxIBMAPI.getSelection() != apiVisibility.contains(APIVisibility.IBM_API)
            || apiVisibilityCheckboxSpec.getSelection() != apiVisibility.contains(APIVisibility.SPEC)
            || apiVisibilityCheckboxThirdParty.getSelection() != apiVisibility.contains(APIVisibility.THIRD_PARTY)) {
            return true;
        }
        return false;
    }

    protected void updateAPIVisibilityCheckboxeValues(EnumSet<APIVisibility> apiVisibility) {
        apiVisibilityCheckboxAPI.setSelection(apiVisibility.contains(APIVisibility.API));
        apiVisibilityCheckboxIBMAPI.setSelection(apiVisibility.contains(APIVisibility.IBM_API));
        apiVisibilityCheckboxSpec.setSelection(apiVisibility.contains(APIVisibility.SPEC));
        apiVisibilityCheckboxThirdParty.setSelection(apiVisibility.contains(APIVisibility.THIRD_PARTY));
    }

    @Override
    public void performDefaults() {
        libRefIds = defaultLibRefIds;
        defaultLibRefIds = new ArrayList<String>(libRefIds);
        resetTable();
        updateAPIVisibilityCheckboxeValues(APIVisibility.getDefaults());
    }
}

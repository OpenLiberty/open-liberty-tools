/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.common.ui.ext.internal.servertype.ServerTypeUIExtension;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContextIds;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

/**
 * Wizard page to set the server name.
 */
public class WebSphereServerParentComposite extends AbstractWebSphereServerComposite {
    protected Label description;
    protected boolean updating;
    protected IWizardHandle wHandle;
    protected Composite parent;
    protected List<ServerTypeUIExtension> activeServerTypeExtensions = new ArrayList<ServerTypeUIExtension>();
    protected TaskModel taskModel;
    protected IServerWizardComposite comp = null;
    protected static final String STAND_ALONE_LIBERTY = com.ibm.ws.st.ui.internal.Messages.wizServerTypeLibertyServer;
    private final HashMap<String, IServerWizardComposite> compCache = new HashMap<String, IServerWizardComposite>();
    protected Map<String, WebSphereServerInfo> serverInfoMap;
    protected Point initialSize = null;

    /**
     * @param parent2
     * @param wizard
     * @param fragmentMap2
     */
    public WebSphereServerParentComposite(Composite parent, IWizardHandle wizard, List<ServerTypeUIExtension> activeServerTypeExtensions, TaskModel taskModel) {
        super(parent, wizard);
        wHandle = wizard;
        this.activeServerTypeExtensions = activeServerTypeExtensions;
        this.taskModel = taskModel;
        if (taskModel != null) {
            // Initialize the server type to Liberty
            taskModel.putObject(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA, WebSphereServer.LIBERTY_SERVER_TYPE);
        }
        wizard.setTitle(Messages.wizServerTitle);
        wizard.setDescription(Messages.wizServerDescription);
        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));
        createControl();
    }

    @Override
    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginWidth = 10;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 0;
        setLayout(layout);
        setLayoutData(new GridData(GridData.FILL_BOTH));
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ContextIds.RUNTIME_COMPOSITE);
        //add ServerType widget only if docker is running
        if (activeServerTypeExtensions != null && !activeServerTypeExtensions.isEmpty()) {
            addServerTypeWidget();
        } else {
            comp = (IServerWizardComposite) getLibertyServerComposite();
            setupComposite(comp, false);
        }

    }

    @Override
    protected void init() {
        if (server == null)
            return;

        updating = true;
        server.setDefaults(new NullProgressMonitor());

        updating = false;

    }

    /**
     * delegate validation to serverTypeComposite
     */
    @Override
    public void validate() {
        if (comp != null)
            comp.validate();
    }

    /**
     * delegate isComplete to serverTypeComposite
     */
    @Override
    public boolean isComplete() {
        if (comp != null)
            return comp.isComplete();
        return false;
    }

    /**
     * delegate finish to serverTypeComposite
     */
    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        if (comp != null) {
            comp.performFinish(monitor);
        }

    }

    protected boolean runtimeHasServers() {
        // Default to true so that the user has the New button if anything goes wrong
        boolean hasServers = true;
        IRuntime rt = (IRuntime) this.taskModel.getObject(TaskModel.TASK_RUNTIME);
        if (rt != null) {
            WebSphereRuntime wrt = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);
            if (wrt != null) {
                wrt.updateServerCache(true);
                hasServers = wrt.hasServers();
            }
        }
        return hasServers;
    }

    private void addServerTypeWidget() {

        //server type Label
        Label label = new Label(WebSphereServerParentComposite.this, SWT.NONE);
        label.setText(Messages.serverType);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        // Add buttons for the active extensions
        Button libertyButton = new Button(WebSphereServerParentComposite.this, SWT.RADIO);
        libertyButton.setText(Messages.wizServerTypeLibertyServer);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalSpan = 2;
        libertyButton.setLayoutData(data);
        libertyButton.setData(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA, WebSphereServer.LIBERTY_SERVER_TYPE);

        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Button button = (Button) e.getSource();
                if (button.getSelection()) {
                    setServerType((String) button.getData(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA));
                    if (comp != null) {
                        GridData data = (GridData) comp.getComposite().getLayoutData();
                        if (!data.exclude) {
                            data.exclude = true;
                            comp.getComposite().setVisible(!data.exclude);
                            comp.getComposite().setLayoutData(data);
                        }
                    }
                    String buttonText = ((Button) e.getSource()).getText();
                    comp = getComposite(buttonText);
                    taskModel.putObject("compositeType", buttonText);

                    updateCompLayout();
                }
                WebSphereServerParentComposite.this.setFocus();
            }

        };
        libertyButton.addSelectionListener(listener);

        for (ServerTypeUIExtension ext : activeServerTypeExtensions) {
            final Button button = new Button(WebSphereServerParentComposite.this, SWT.RADIO);
            button.setText(ext.getLabel());
            data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
            data.horizontalIndent = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 7;
            data.horizontalSpan = 3;
            button.setLayoutData(data);
            button.addSelectionListener(listener);
            button.setData(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA, ext.getTypeId());
        }

        libertyButton.setSelection(true);
        libertyButton.notifyListeners(SWT.Selection, new Event());

    }

    //DefaultServerComposite is provided if runtime has no Servers
    protected Composite getLibertyServerComposite() {
        if (isLocalhost()) {
            if (runtimeHasServers())
                return new WebSphereServerComposite(WebSphereServerParentComposite.this, wHandle);
            return new WebSphereDefaultServerComposite(WebSphereServerParentComposite.this, wHandle, taskModel);
        }
        return new WebSphereRemoteServerComposite(WebSphereServerParentComposite.this, wHandle);
    }

    protected void setupComposite(IServerWizardComposite comp, boolean isIndentationRequired) {
        if (comp != null) {
            comp.setup(taskModel);
            GridData data = new GridData();
            data.horizontalAlignment = SWT.FILL;
            data.grabExcessHorizontalSpace = true;
            data.verticalAlignment = SWT.FILL;
            data.widthHint = 400;
            data.grabExcessVerticalSpace = true;
            data.horizontalSpan = 3;
            data.verticalIndent = 7;
            if (isIndentationRequired)
                data.horizontalIndent = SWTUtil.convertWidthInCharsToPixels(WebSphereServerParentComposite.this, 4);
            comp.getComposite().setLayoutData(data);

        }
    }

    protected IServerWizardComposite getComposite(String serverType) {
        IServerWizardComposite comp = compCache.get(serverType);
        if (comp == null) {
            comp = createComposite(serverType);
            compCache.put(serverType, comp);
        }
        return comp;
    }

    protected IServerWizardComposite createComposite(String serverType) {
        if (!serverType.equals(STAND_ALONE_LIBERTY) && activeServerTypeExtensions != null) {
            for (ServerTypeUIExtension ext : activeServerTypeExtensions) {
                if (serverType.equals(ext.getLabel()))
                    return (IServerWizardComposite) ext.getComposite(WebSphereServerParentComposite.this, wHandle, taskModel);
            }

        } else {
            return (IServerWizardComposite) getLibertyServerComposite();
        }
        return null;
    }

    protected boolean isLocalhost() {
        IServerWorkingCopy swc = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
        String host = swc.getHost();

        if (host != null)
            return SocketUtil.isLocalhost(host);

        Trace.logError("The value for host in the server task model is null", new Exception("Host value is null"));
        return true;
    }

    protected void setServerType(String serverType) {
        taskModel.putObject(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA, serverType);
        IServerWorkingCopy swc = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
        if (swc != null) {
            WebSphereServer wsServer = swc.getAdapter(WebSphereServer.class);
            if (wsServer != null) {
                wsServer.setServerType(serverType);
            }
        }
    }

    /**
     * Disposes and recreates the children depending on hasServers.
     */
    protected void updateChildren(boolean hasServers) {
        if (comp != null) {
            GridData data = (GridData) comp.getComposite().getLayoutData();
            if (!data.exclude) {
                data.exclude = true;
                comp.getComposite().setVisible(!data.exclude);
                comp.getComposite().setLayoutData(data);
            }
        }

        // since the runtime has changed, the cached children have to be disposed and cleared
        for (IServerWizardComposite child : compCache.values()) {
            ((Widget) child).dispose();
        }
        compCache.clear();

        setServerType((String) taskModel.getObject(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA));
        String compositeType = (String) taskModel.getObject("compositeType");

        if (compositeType != null) {
            comp = getComposite(compositeType);
        } else {
            // if compositeType hasn't been set, get the liberty server by default.
            comp = getComposite(STAND_ALONE_LIBERTY);
        }

        updateCompLayout();
        WebSphereServerParentComposite.this.setFocus();
    }

    protected void updateCompLayout() {
        // This must come after the 'comp' field is set since it ends up
        // invoking the isComplete method that use the 'comp' field.
        setupComposite(comp, true);
        GridData data = (GridData) comp.getComposite().getLayoutData();
        data.exclude = false;
        comp.getComposite().setVisible(!data.exclude);
        validate();
        WebSphereServerParentComposite.this.redraw();
        WebSphereServerParentComposite.this.layout(true, true);

        //resize wizard vertically.
        Shell shell = WebSphereServerParentComposite.this.getShell();
        shell.layout(true, true);

        if (initialSize == null) {
            // Do not resize if wizard was launched from the Runtime Explorer.
            Boolean preventResize = (Boolean) taskModel.getObject("preventResize");
            if (preventResize != null && preventResize.booleanValue()) {
                taskModel.putObject("preventResize", Boolean.valueOf(false));
                return;
            }
            initialSize = shell.getSize();
        }

        final Point newSize = shell.computeSize(initialSize.x, SWT.DEFAULT, true);
        shell.setSize(newSize);
        // Update the wizard so the children get updated based on the current server type
        wHandle.update();
    }
}

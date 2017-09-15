/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.util.HashSet;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.generation.MetaDataRemover;
import com.ibm.ws.st.ui.internal.DDETreeContentProvider;
import com.ibm.ws.st.ui.internal.DDETreeLabelProvider;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public abstract class AbstractWebSphereServerComposite extends Composite implements IServerWizardComposite {
    protected IServerWorkingCopy serverWC;
    protected WebSphereServer server;
    protected IWizardHandle wizard;
    protected TreeViewer treeViewer;
    protected DDETreeContentProvider contentProvider;
    protected final HashSet<String> runtimeIds = new HashSet<String>();
    protected UserDirectory userDirectory;
    private Label label;
    private Tree tree;

    protected AbstractWebSphereServerComposite(Composite parent, IWizardHandle wizard) {
        super(parent, SWT.NONE);
        this.wizard = wizard;
    }

    abstract protected void createControl();

    abstract protected void init();

    @Override
    // To reinitialize the composite when configurations such as runtime has changed.
    public void reInitialize() {
        init();
    }

    /**
     *
     */
    protected void createConfigControl(Composite comp) {
        GridData data;
        label = new Label(comp, SWT.NONE);
        label.setText(Messages.wizServerConfiguration);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
        data.horizontalSpan = 3;
        data.verticalIndent = 3;
        label.setLayoutData(data);

        tree = new Tree(comp, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.horizontalSpan = 3;
        data.minimumHeight = 80;
        tree.setLayoutData(data);
        tree.setFont(this.getFont());

        treeViewer = new TreeViewer(tree);
        treeViewer.setAutoExpandLevel(3);
        contentProvider = new DDETreeContentProvider();
        treeViewer.setContentProvider(contentProvider);
        treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new DDETreeLabelProvider()));
    }

    protected void setConfigControlVisible(boolean b) {
        label.setVisible(b);
        tree.setVisible(b);
    }

    protected String getEmptyConfigMsg() {
        return Messages.configNone;
    }

    @Override
    public void setup(TaskModel taskModel) {
        IServerWorkingCopy wc = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
        UserDirectory userDir = (UserDirectory) taskModel.getObject(WebSphereRuntime.PROP_USER_DIRECTORY);
        setServer(wc, userDir);
    }

    protected void setServer(IServerWorkingCopy newServer, UserDirectory userDir) {
        if (newServer == null) {
            serverWC = null;
            server = null;
            userDirectory = null;
        } else {
            serverWC = newServer;
            server = (WebSphereServer) newServer.loadAdapter(WebSphereServer.class, null);
            userDirectory = userDir;
        }

        init();
        validate();
    }

    protected void performCancel() {
        removeGeneratedMetaData(null);
    }

    protected void addMetaDataRuntimeId(IRuntime runtime) {
        // this was a creation scenario for the runtime so we keep
        // track of it in case the user cancels the operation and
        // we needed to remove any meta data that was generated
        if (runtime != null && runtime.isWorkingCopy() && ((IRuntimeWorkingCopy) runtime).getOriginal() == null) {
            runtimeIds.add(runtime.getId());
        }
    }

    // Remove metadata that were generated during the process of creating
    // a server and is no longer needed. This could have been caused by
    // creating a runtime then canceling the process, or creating a runtime
    // then renaming it and finishing the process
    protected void removeGeneratedMetaData(IRuntime runtimeToExclude) {
        if (runtimeToExclude != null) {
            runtimeIds.remove(runtimeToExclude.getId());
        }

        if (!runtimeIds.isEmpty()) {
            MetaDataRemover.removeCancelledMetaData(runtimeIds);
        }
    }

    @Override
    public Composite getComposite() {
        return this;
    }

}

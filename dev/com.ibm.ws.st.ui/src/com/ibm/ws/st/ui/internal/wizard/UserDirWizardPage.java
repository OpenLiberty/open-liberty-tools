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

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

public class UserDirWizardPage extends WizardPage {
    protected boolean projectOptionSelected = true;
    protected Table projectTable;
    protected IProject selectedProject;
    protected IPath selectedPath;

    public UserDirWizardPage() {
        super("userDir", Messages.wizUserDirTitle, null);
        setDescription(Messages.wizUserDirDescription);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        comp.setLayout(layout);

        final Label message = new Label(comp, SWT.WRAP);
        message.setText(Messages.wizUserDirMessage);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        message.setLayoutData(data);
        Point p = message.computeSize(200, SWT.DEFAULT);
        message.setSize(p);

        final Button projectOption = new Button(comp, SWT.RADIO);
        projectOption.setText(Messages.wizUserDirProject);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        projectOption.setLayoutData(data);

        projectTable = new Table(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.horizontalIndent = 15;
        projectTable.setLayoutData(data);

        projectTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] ti = projectTable.getSelection();
                if (ti == null || ti.length != 1)
                    return;
                selectedProject = (IProject) ti[0].getData();
                setPageComplete(validate());
            }
        });

        updateUserDirProjects();

        final Button newProject = SWTUtil.createButton(comp, Messages.create);
        newProject.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NewUserDirProjectWizard wizard = new NewUserDirProjectWizard();
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                //dialog.setPageSize(425, 275);
                if (dialog.open() == Window.CANCEL)
                    return;
                updateUserDirProjects();
                TableItem[] items = projectTable.getItems();
                for (TableItem ti : items) {
                    IProject p = (IProject) ti.getData();
                    if (wizard.getProject().equals(p)) {
                        projectTable.setSelection(ti);
                        selectedProject = (IProject) ti.getData();
                        setPageComplete(validate());
                        return;
                    }
                }
            }
        });

        Button externalOption = new Button(comp, SWT.RADIO);
        externalOption.setText(Messages.wizUserDirExternal);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        externalOption.setLayoutData(data);

        final Text folder = new Text(comp, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalIndent = 15;
        folder.setLayoutData(data);
        folder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                selectedPath = new Path(folder.getText());
                setPageComplete(validate());
            }
        });

        final Button browse = SWTUtil.createButton(comp, Messages.browse);
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setMessage(Messages.wizUserDirExternalBrowse);
                dialog.setFilterPath(folder.getText());
                String selectedDirectory = dialog.open();
                if (selectedDirectory != null)
                    folder.setText(selectedDirectory);
            }
        });

        final Label externalWarning = new Label(comp, SWT.WRAP);
        externalWarning.setText(Messages.wizUserDirExternalMessage);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        data.horizontalIndent = 15;
        externalWarning.setLayoutData(data);
        p = externalWarning.computeSize(200, SWT.DEFAULT);
        externalWarning.setSize(p);

        projectOption.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                projectOptionSelected = projectOption.getSelection();
                projectTable.setEnabled(projectOptionSelected);
                newProject.setEnabled(projectOptionSelected);
                folder.setEnabled(!projectOptionSelected);
                browse.setEnabled(!projectOptionSelected);
                externalWarning.setEnabled(!projectOptionSelected);
                setPageComplete(validate());
            }
        });

        Dialog.applyDialogFont(comp);
        projectOption.setSelection(true);
        projectTable.setEnabled(true);
        newProject.setEnabled(true);
        folder.setEnabled(false);
        browse.setEnabled(false);
        externalWarning.setEnabled(false);
        setControl(comp);
        setPageComplete(validate());
    }

    protected void updateUserDirProjects() {
        projectTable.removeAll();

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject p : projects) {
            if (p.getFolder(Constants.SERVERS_FOLDER).exists() &&
                    p.getFolder(Constants.SHARED_FOLDER).exists()) {
                TableItem item = new TableItem(projectTable, SWT.NONE);
                item.setData(p);
                item.setImage(Activator.getImage(Activator.IMG_USER_PROJECT));
                item.setText(p.getName());
            }
        }
    }

    protected boolean validate() {
        if (projectOptionSelected) {
            if (selectedProject == null) {
                setMessage(null, IMessageProvider.NONE);
                return false;
            }
        } else {
            if (selectedPath == null || selectedPath.toFile() == null) {
                setMessage(null, IMessageProvider.NONE);
                return false;
            }
            File f = selectedPath.toFile();
            if (!f.exists()) {
                setMessage(Messages.wizUserDirExternalInvalid, IMessageProvider.ERROR);
                return false;
            }
        }

        setMessage(null, IMessageProvider.NONE);
        return true;
    }

    public IPath getPath() {
        return selectedPath;
    }

    public IProject getProject() {
        return selectedProject;
    }
}

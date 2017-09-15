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
package com.ibm.ws.st.ui.internal.config;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

import com.ibm.ws.st.core.internal.config.validation.FilterItem;
import com.ibm.ws.st.core.internal.config.validation.ValidationFilterSettings;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

/**
 *
 */
public class IgnoreFilterPropertyPage extends PropertyPage {

    protected TreeViewer treeViewer;
    protected IProject project;
    protected ValidationFilterSettings ignoreSettings;

    @Override
    protected void performDefaults() {
        // do nothing
    }

    @Override
    protected void performApply() {
        // do nothing
    }

    @Override
    protected Control createContents(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());

        // Selected path
        Label label = new Label(composite, SWT.WRAP);
        label.setText(Messages.validationFilterLabel);
        GridData data = new GridData(GridData.FILL, GridData.FILL, false, false);
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Tree tree = new Tree(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, true);
        tree.setLayoutData(data);
        tree.setFont(this.getFont());

        treeViewer = new TreeViewer(tree);
        treeViewer.setAutoExpandLevel(3);
        treeViewer.setContentProvider(new IgnoreFilterTreeContentProvider());
        treeViewer.setLabelProvider(new IgnoreFilterLabelProvider());

        final Button filterRemove = SWTUtil.createButton(composite, Messages.remove);
        filterRemove.setEnabled(false);

        // Listeners
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                final ISelection selection = event.getSelection();
                if (selection.isEmpty() || !(selection instanceof StructuredSelection)) {
                    filterRemove.setEnabled(false);
                } else {
                    filterRemove.setEnabled(true);
                }
            }
        });

        filterRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                boolean success = true;

                /* Wait until we finish removing all items before redrawing */
                treeViewer.getTree().setRedraw(false);
                for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
                    Object element = iterator.next();
                    if (element instanceof FilterItem) {
                        if (!ignoreSettings.removeFilter((FilterItem) element)) {
                            success = false;
                            break;
                        }
                    }
                }
                if (!success) {
                    showErrorMessage();
                    refreshTree();
                }
                treeViewer.getTree().setRedraw(true);
            }
        });

        loadSettings();

        return composite;
    }

    private void loadSettings() {
        final IAdaptable element = getElement();
        project = (IProject) element.getAdapter(IProject.class);

        if (project == null) {
            return;
        }

        ignoreSettings = new ValidationFilterSettings(project);
        treeViewer.setInput(ignoreSettings);
    }

    protected void refreshTree() {
        ignoreSettings.refresh();
        treeViewer.setInput(ignoreSettings);
    }

    protected void showErrorMessage() {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MessageDialog.openError(shell, Messages.title, Messages.removeFilterFailedMessage);
    }
}

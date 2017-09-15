/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal.wizard;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.ibm.ws.st.docker.ui.internal.Activator;
import com.ibm.ws.st.docker.ui.internal.Messages;
import com.ibm.ws.st.docker.ui.internal.Trace;

/**
 * This dialog displays a table of containers in use by existing servers, and the names of the corresponding servers.
 * Intended to be used only when containerAndServerNames (passed as constructor parameter) is NOT empty.
 */
public class ContainersInUseDialog extends TitleAreaDialog {

    private final Map<String, String> containerAndServerNames;

    /**
     * Create this dialog and set the data to be displayed.
     *
     * @param parentShell
     * @param containerNames - Must not be empty.
     */
    public ContainersInUseDialog(Shell parentShell, Map<String, String> containerAndServerNames) {
        super(parentShell);
        this.containerAndServerNames = containerAndServerNames;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite dialogArea = (Composite) super.createDialogArea(parent);

        setTitle(Messages.containersInUseDialogBoldMessage);
        setMessage(Messages.wizDockerInUseContainersCannotCreateNewServer);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));

        // Dialogs always use GridLayout - set the margins
        GridLayout dialogLayout = (GridLayout) dialogArea.getLayout();
        dialogLayout.marginWidth = 10;
        dialogLayout.marginHeight = 10;

        // Table to display container and server names
        Table containersTable = new Table(dialogArea, SWT.BORDER | SWT.NO_FOCUS);
        containersTable.setHeaderVisible(true);
        // This Table fills the dialog
        containersTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TableColumn containerNamesCol = new TableColumn(containersTable, SWT.NONE);
        containerNamesCol.setText(Messages.containersInUseDialogTableColumnContainersHeader);

        TableColumn serverNamesCol = new TableColumn(containersTable, SWT.NONE);
        serverNamesCol.setText(Messages.containersInUseDialogTableColumnServersHeader);

        // Put the contents of the name map into the table - key is container name, value is server name
        for (Map.Entry<String, String> entry : containerAndServerNames.entrySet()) {
            TableItem item = new TableItem(containersTable, SWT.NONE);
            item.setText(new String[] { entry.getKey(), entry.getValue() });

            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Container " + entry.getKey() + //$NON-NLS-1$
                                        " in use by server " + entry.getValue()); //$NON-NLS-1$
            }
        }

        // Pack all columns, making them autofit their contents (table will allow scrolling if necessary)
        for (TableColumn column : containersTable.getColumns()) {
            column.pack();
        }

        return dialogArea;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.containersInUseDialogTitle);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // OK only, no Cancel
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }
}

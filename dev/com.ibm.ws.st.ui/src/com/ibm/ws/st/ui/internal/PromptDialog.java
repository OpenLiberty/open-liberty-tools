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
package com.ibm.ws.st.ui.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.ibm.ws.st.core.internal.IPromptIssue;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.PromptAction;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;

public class PromptDialog extends TitleAreaDialog {
    protected String message;
    protected AbstractPrompt[] prompts;
    protected PromptResponse response = new PromptResponse();
    protected boolean[] alwaysApply;
    protected int style;
    protected Table issueTable;
    protected Button alwaysButton;
    protected IPromptIssue currentIssue = null;
    private boolean isCancelPressed = false;
    protected boolean isUpdating = false;
    protected final List<TableEditor> editors = new ArrayList<TableEditor>(4);

    public PromptDialog(Shell parentShell, String message, AbstractPrompt[] prompts, int style) {
        super(parentShell);
        this.message = message;
        this.prompts = prompts;
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setHelpAvailable(((style & PromptHandler.STYLE_HELP) != 0));
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.title);
        setShellStyle(SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        setTitle(Messages.wizPromptMessage);
        setMessage(message);
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 7;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label label = new Label(composite, SWT.WRAP);
        label.setText(Messages.wizPromptLabel);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        issueTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.heightHint = 200;
        data.verticalSpan = 3;
        data.horizontalSpan = 2;
        issueTable.setLayoutData(data);

        issueTable.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                releaseEditors();
            }
        });

        final TableColumn issueColumn = new TableColumn(issueTable, SWT.NONE);
        issueColumn.setText(Messages.wizPromptIssueColumn);
        issueColumn.setResizable(true);
        issueColumn.setWidth(200);
        issueColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                sortTable(issueTable, issueColumn);
            }
        });

        final TableColumn descColumn = new TableColumn(issueTable, SWT.WRAP);
        descColumn.setText(Messages.wizPromptDescriptionColumn);
        descColumn.setResizable(true);
        descColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                sortTable(issueTable, descColumn);
            }
        });

        final TableColumn actionColumn = new TableColumn(issueTable, SWT.NONE);
        actionColumn.setText(Messages.wizPromptActionColumn);
        actionColumn.setResizable(true);
        actionColumn.setWidth(180);

        issueTable.setHeaderVisible(true);
        issueTable.setLinesVisible(false);
        issueTable.setSortDirection(SWT.UP);
        issueTable.setSortColumn(issueColumn);

        alwaysButton = new Button(composite, SWT.CHECK);
        alwaysButton.setText(Messages.wizPromptAlways);
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
        alwaysButton.setLayoutData(data);
        alwaysButton.setEnabled(false);

        alwaysButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (!isUpdating && currentIssue != null)
                    response.putApplyAlways(currentIssue, alwaysButton.getSelection());
            }
        });

        init();

        issueTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] ti = issueTable.getSelection();
                if (ti == null || ti.length != 1)
                    return;
                currentIssue = (IPromptIssue) ti[0].getData();
                isUpdating = true;
                if (response.isEnableApplyAlways(currentIssue)) {
                    alwaysButton.setEnabled(true);
                    alwaysButton.setSelection(response.getApplyAlways(currentIssue));
                } else {
                    alwaysButton.setEnabled(false);
                }
                isUpdating = false;
            }
        });

        final Composite comp = composite;
        comp.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle area = comp.getClientArea();
                Point preferredSize = issueTable.computeSize(SWT.DEFAULT, 100);
                int width = area.width - 2 * issueTable.getBorderWidth() -15;
                if (preferredSize.y > area.height + issueTable.getHeaderHeight()) {
                    Point vBarSize = issueTable.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                Point actualSize = issueTable.getSize();
                if (actualSize.x > area.width) {
                    // table is getting smaller so make the columns smaller first and then resize the table to
                    // match the client area width
                    // set the max width as 220 for first and 180 for the last column (issue and action)
                    if (width / 4 < 220) {
                        issueTable.getColumn(0).setWidth(width / 4);
                        issueTable.getColumn(2).setWidth(width / 4);
                    } else {
                        issueTable.getColumn(0).setWidth(220);
                        issueTable.getColumn(2).setWidth(180);
                    }
                    issueTable.getColumn(1).setWidth(width - issueTable.getColumn(0).getWidth() - issueTable.getColumn(2).getWidth());
                    issueTable.setSize(area.width, area.height);
                } else {
                    // table is getting bigger so make the table 
                    // bigger first and then make the columns wider
                    // to match the client area width
                    issueTable.setSize(area.width, area.height);
                    if (width / 4 < 220) {
                        issueTable.getColumn(0).setWidth(width / 4);
                        issueTable.getColumn(2).setWidth(width / 4);
                    } else {
                        issueTable.getColumn(0).setWidth(220);
                        issueTable.getColumn(2).setWidth(180);
                    }
                    issueTable.getColumn(1).setWidth(width - issueTable.getColumn(0).getWidth() - issueTable.getColumn(2).getWidth());
                }
            }
        });

        Dialog.applyDialogFont(composite);
        return composite;
    }

    public IPromptResponse getResponses() {
        return isCancelPressed ? null : response;
    }

    void init() {
        if (prompts == null || prompts.length == 0) {
            return;
        }

        List<IPromptIssue> issueList = new ArrayList<IPromptIssue>();
        for (AbstractPrompt prompt : prompts) {
            IPromptIssue[] issues = prompt.getIssues();
            if (issues == null || issues.length == 0)
                continue;

            for (IPromptIssue issue : issues) {
                issueList.add(issue);
                response.putSelectionAction(issue, issue.getDefaultAction());
                if (prompt.getApplyAlways())
                    response.putApplyAlways(issue, false);
            }
        }

        sortIssueList(issueList, 0, -1);
        initTable(issueList);
    }

    private void initTable(List<IPromptIssue> issues) {
        Control[] children = issueTable.getChildren();
        for (Control child : children) {
            child.dispose();
        }
        releaseEditors();
        issueTable.removeAll();

        for (IPromptIssue issue : issues) {
            TableItem item = new TableItem(issueTable, SWT.NONE);
            item.setData(issue);
            item.setText(0, issue.getType());
        }

        int descColWidth = 620;
        final TableItem[] items = issueTable.getItems();
        for (TableItem item : items) {
            final IPromptIssue issue = (IPromptIssue) item.getData();
            TableEditor editor = new TableEditor(issueTable);
            Composite composite = new Composite(issueTable, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 2;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setBackground(issueTable.getBackground());

            StyledText label = new StyledText(composite, SWT.READ_ONLY);
            label.setText(issue.getSummary());
            label.setBackground(issueTable.getBackground());
            label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
            label.pack();

            final Link link = new Link(composite, SWT.NONE);
            link.setText("(<a>" + Messages.wizPromptDetailsLabel + "</a>)");
            link.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
            link.setBackground(issueTable.getBackground());
            link.pack();

            final DefaultToolTip toolTipDelayed = new DefaultToolTip(link, ToolTip.RECREATE, true);
            toolTipDelayed.setText(issue.getDetails());
            toolTipDelayed.setHideDelay(5000);

            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Point size = link.getSize();
                    toolTipDelayed.show(new Point(30, size.y));
                }
            });

            editor.grabHorizontal = true;
            editor.setEditor(composite, item, 1);
            // set descColWidth to a max of 620, if the server name is too long, the description 
            // field will have a large width that can take up entire screen size
            descColWidth = Math.min(descColWidth, label.getSize().x + link.getSize().x + 5);

            editors.add(editor);

            editor = new TableEditor(issueTable);
            final CCombo combo = new CCombo(issueTable, SWT.READ_ONLY | SWT.FLAT);
            combo.setBackground(issueTable.getBackground());

            final PromptAction[] actions = issue.getPossibleActions();
            final PromptAction defaultAction = response.getSelectedAction(issue);
            int i = 0;
            int selectedActionIndex = 0;
            for (PromptAction action : actions) {
                combo.add(action.toString());
                if (action == defaultAction)
                    selectedActionIndex = i;
                else
                    ++i;
            }

            combo.select(selectedActionIndex);

            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    int c = combo.getSelectionIndex();
                    response.putSelectionAction(issue, actions[c]);
                    currentIssue = issue;
                    isUpdating = true;
                    if (response.isEnableApplyAlways(issue)) {
                        alwaysButton.setEnabled(true);
                        alwaysButton.setSelection(response.getApplyAlways(issue));
                    } else {
                        alwaysButton.setEnabled(false);
                    }
                    isUpdating = false;
                }
            });
            editor.grabHorizontal = true;
            editor.setEditor(combo, item, 2);
            editors.add(editor);
        }

        issueTable.getColumn(1).setWidth(descColWidth);
    }

    void sortTable(Table table, TableColumn column) {
        TableItem[] items = table.getItems();
        List<IPromptIssue> issueList = new ArrayList<IPromptIssue>(items.length);
        int dir = table.getSortDirection() == SWT.DOWN ? 1 : -1;
        TableColumn currentColumn = table.getSortColumn();
        int columnNum = 0;
        for (int j = 0; j < table.getColumnCount(); j++) {
            if (table.getColumn(j).equals(column)) {
                columnNum = j;
                break;
            }
        }
        if (column.equals(currentColumn))
            dir = -dir;
        else
            dir = 1;

        for (TableItem item : items) {
            issueList.add((IPromptIssue) item.getData());
        }
        sortIssueList(issueList, columnNum, dir);
        table.setSortDirection(dir == 1 ? SWT.DOWN : SWT.UP);
        table.setSortColumn(column);
        initTable(issueList);
    }

    protected void sortIssueList(List<IPromptIssue> issueList, final int columnNum, int dir) {
        if (dir == 1) {
            Collections.sort(issueList, new Comparator<IPromptIssue>() {
                @Override
                public int compare(IPromptIssue p1, IPromptIssue p2) {
                    if (columnNum == 0)
                        return p2.getType().compareTo(p1.getType());
                    else if (columnNum == 1)
                        return p2.getSummary().compareTo(p1.getSummary());
                    return 0;
                }
            });
        } else {
            Collections.sort(issueList, new Comparator<IPromptIssue>() {
                @Override
                public int compare(IPromptIssue p1, IPromptIssue p2) {
                    if (columnNum == 0)
                        return p1.getType().compareTo(p2.getType());
                    else if (columnNum == 1)
                        return p1.getSummary().compareTo(p2.getSummary());
                    return 0;
                }
            });
        }
    }

    protected void releaseEditors() {
        for (TableEditor editor : editors) {
            editor.dispose();
        }
        editors.clear();
    }

    @Override
    protected void cancelPressed() {
        isCancelPressed = true;
        super.cancelPressed();
    }
}

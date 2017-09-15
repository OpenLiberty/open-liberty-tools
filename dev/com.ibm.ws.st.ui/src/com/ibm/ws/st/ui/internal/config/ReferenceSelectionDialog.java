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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.SearchPattern;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Dialog for selecting references (which may be ids or variables).
 */
public class ReferenceSelectionDialog extends TitleAreaDialog {

    protected final Set<String> ids;
    protected final Set<String> vars;
    protected final ConfigVars configVars;
    protected final boolean multi;
    protected Set<String> newRefs;
    protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
    protected Tree refTree;
    protected TreeItem idTreeRoot;
    protected TreeItem varTreeRoot;

    public ReferenceSelectionDialog(Shell parent, Set<String> ids, Set<String> vars, ConfigVars configVars, boolean multi) {
        super(parent);
        this.ids = ids;
        this.vars = vars;
        this.configVars = configVars;
        this.multi = multi;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.referenceSelectionTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(multi ? Messages.referenceSelectionMultiLabel : Messages.referenceSelectionSingleLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(multi ? Messages.referenceSelectionMultiMessage : Messages.referenceSelectionSingleMessage);

        // Main composite
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Filter text
        final Text filterText = new Text(composite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        filterText.setLayoutData(data);
        filterText.setMessage(Messages.filterMessage);

        // Reference selection tree
        int selection = multi ? SWT.MULTI : SWT.SINGLE;
        refTree = new Tree(composite, SWT.BORDER | selection | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 15 * 16;
        refTree.setLayoutData(data);

        Color gray = composite.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

        // Id root item - parent label item for ids
        idTreeRoot = new TreeItem(refTree, SWT.NONE);
        idTreeRoot.setText(Messages.referenceSelectionAvailableIds);
        idTreeRoot.setForeground(gray);
        idTreeRoot.setImage(Activator.getImage(Activator.IMG_FACTORY_REF));

        // Var root item - parent label item for vars
        varTreeRoot = new TreeItem(refTree, SWT.NONE);
        varTreeRoot.setText(Messages.referenceSelectionAvailableVars);
        varTreeRoot.setForeground(gray);
        varTreeRoot.setImage(Activator.getImage(Activator.IMG_VARIABLE_REF));

        createItems(ids, vars, configVars, "");

        HoverHelper.addHoverHelp(refTree);

        // Listeners
        filterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = filterText.getText();
                if (text == null) {
                    text = "";
                }
                createItems(ids, vars, configVars, text);
                enableOKButton();
            }
        });

        refTree.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                enableOKButton();
            }
        });

        refTree.addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                okPressed();
                close();
            }
        });

        return composite;
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton();
        return control;
    }

    /** {@inheritDoc} */
    @Override
    public void create() {
        super.create();
        refTree.setFocus();
    }

    @Override
    protected void okPressed() {
        // Collect up the selected items
        TreeItem[] items = refTree.getSelection();
        newRefs = new HashSet<String>(items.length);
        for (TreeItem item : items) {
            TreeItem parent = item.getParentItem();
            if (parent == idTreeRoot) {
                newRefs.add(item.getText());
            } else if (parent == varTreeRoot) {
                newRefs.add(ConfigVarsUtils.getVarRef(item.getText()));
            }
        }
        super.okPressed();
    }

    protected void enableOKButton() {
        // Enable the OK button only if the user has not selected
        // either of the root items since these are just labels.
        boolean enable = false;
        if (refTree != null && refTree.getSelectionCount() > 0) {
            enable = true;
            TreeItem[] items = refTree.getSelection();
            for (TreeItem item : items) {
                TreeItem parent = item.getParentItem();
                if (parent == null) {
                    enable = false;
                    break;
                }
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(enable);
    }

    protected void createItems(Set<String> ids, Set<String> vars, ConfigVars configVars, String filter) {
        pattern.setPattern(filter);

        // Create the ids for the table.
        ArrayList<String> idList = new ArrayList<String>(ids.size());
        idList.addAll(ids);
        Collections.sort(idList);
        idTreeRoot.removeAll();
        for (String id : idList) {
            if (pattern.matches(id)) {
                TreeItem item = new TreeItem(idTreeRoot, SWT.NONE);
                item.setText(id);
                item.setImage(Activator.getImage(Activator.IMG_FACTORY_REF));
            }
        }
        idTreeRoot.setExpanded(true);

        // Create vars for the table.
        ArrayList<String> varList = new ArrayList<String>(vars.size());
        varList.addAll(vars);
        Collections.sort(varList);
        varTreeRoot.removeAll();
        for (String var : varList) {
            if (pattern.matches(var)) {
                TreeItem item = new TreeItem(varTreeRoot, SWT.NONE);
                item.setText(var);
                item.setImage(Activator.getImage(Activator.IMG_VARIABLE_REF));
                String value = configVars.getValue(var);
                if (value != null && !value.isEmpty()) {
                    item.setData(HoverHelper.HOVER_DATA, NLS.bind(Messages.variableValue, new String[] { var, "\"" + value + "\"" }));
                }
            }
        }
        varTreeRoot.setExpanded(true);
    }

    // Valid when multi-selection enabled
    public Set<String> getRefs() {
        return newRefs;
    }

    // Valid when single-selection enabled
    public String getRef() {
        if (!newRefs.isEmpty()) {
            Iterator<String> it = newRefs.iterator();
            return it.next();
        }
        return null;
    }

}

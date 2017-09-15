/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Dialog that allows the user to select feature from the list.
 */
public class FeatureSelectionDialog extends TitleAreaDialog {
    protected WebSphereRuntime wsRuntime;
    protected String elemName;
    protected List<String> possibleFeatures;
    protected Table featureTable;
    protected Text descriptionLabel;
    protected Text enablesLabel;
    protected Text enabledByLabel;
    protected List<String> selectedFeatures;
    protected String title;  // dialog title
    protected String message;  // dialog message
    protected String explanation;   // explanation of the list box.

    public FeatureSelectionDialog(Shell parent, WebSphereRuntime wsRuntime, String elemName, List<String> possibleFeatures, String title, String message, String explanation) {
        super(parent);
        this.elemName = elemName;
        this.possibleFeatures = possibleFeatures;
        this.wsRuntime = wsRuntime;
        this.title = title;
        this.message = message;
        this.explanation = explanation;
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(title);
        setMessage(message);

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.widthHint = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        Label label = new Label(composite, SWT.WRAP);
        label.setText(NLS.bind(explanation, elemName));
        data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        label.setLayoutData(data);

        featureTable = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 100;
        featureTable.setLayoutData(data);

        FeatureUI.createColumns(featureTable);

        for (String s : possibleFeatures) {
            TableItem item = new TableItem(featureTable, SWT.NONE);
            item.setText(0, s);
            String name = FeatureList.getFeatureDisplayName(s, wsRuntime);
            if (name != null)
                item.setText(1, name);
            item.setImage(Activator.getImage(Activator.IMG_FEATURE_ELEMENT));
        }

        // If only one choice check it, this will cause ok button to be enabled
        // as well so user can just click enter
        if (possibleFeatures.size() == 1) {
            featureTable.getItem(0).setChecked(true);
        }

        FeatureUI.resizeColumns(featureTable);

        // info labels
        ScrolledComposite descriptionScroll = new ScrolledComposite(composite, SWT.V_SCROLL);
        descriptionLabel = new Text(descriptionScroll, SWT.WRAP | SWT.READ_ONLY);
        descriptionLabel.setText(NLS.bind(Messages.featureDescription, ""));
        descriptionLabel.setBackground(parent.getBackground());
        descriptionScroll.setContent(descriptionLabel);

        enablesLabel = new Text(composite, SWT.READ_ONLY);
        enablesLabel.setText(NLS.bind(Messages.featureEnables, ""));
        enablesLabel.setBackground(parent.getBackground());
        enablesLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        int lineHeight = enablesLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        data.heightHint = lineHeight * 4;
        descriptionLabel.setSize(300, lineHeight);
        descriptionLabel.setEnabled(false);
        descriptionScroll.setLayoutData(data);
        descriptionScroll.getVerticalBar().setPageIncrement(lineHeight);
        descriptionScroll.getVerticalBar().setIncrement(lineHeight);

        enabledByLabel = new Text(composite, SWT.READ_ONLY);
        enabledByLabel.setText(NLS.bind(Messages.featureEnabledBy, ""));
        enabledByLabel.setBackground(parent.getBackground());
        enabledByLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        featureTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                enableOKButton();
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
            }
        });

        return composite;
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        featureTable.setFocus();
        enableOKButton();
        return control;
    }

    /** {@inheritDoc} */
    @Override
    public void create() {
        super.create();
        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
        // resize description since the UI isn't visible yet 
        descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    protected void enableOKButton() {
        boolean enable = false;
        for (TableItem item : featureTable.getItems()) {
            if (item.getChecked()) {
                enable = true;
                break;
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(enable);
    }

    @Override
    protected void okPressed() {
        selectedFeatures = new ArrayList<String>();
        for (TableItem item : featureTable.getItems()) {
            if (item.getChecked()) {
                selectedFeatures.add(item.getText());
            }
        }
        super.okPressed();
    }

    public List<String> getSelectedFeatures() {
        return selectedFeatures;
    }
}

/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.net.URL;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.w3c.dom.Element;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.util.DetailsViewerAdapter;

/**
 *
 */
@SuppressWarnings("restriction")
public class SchemaPropertiesEntryPage extends WizardPage {

    protected SchemaPropertiesCustomObject.SchemaPropertiesData propertiesData;

    public SchemaPropertiesEntryPage(SchemaPropertiesCustomObject.SchemaPropertiesData propertiesData) {
        super(Messages.schemaPropsEntryPageName, Messages.schemaPropsEntryPageTitle, null);
        this.propertiesData = propertiesData;
        setDescription(Messages.schemaPropsEntryPageDescription);
        setPageComplete(false);
    }

    /** {@inheritDoc} */
    @Override
    public void createControl(Composite parent) {
        ScrolledComposite composite = new ScrolledComposite(parent, SWT.V_SCROLL);
        composite.setExpandHorizontal(true);
        composite.setExpandVertical(true);
        composite.setBackground(parent.getBackground());

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        composite.setLayoutData(data);

        Customization customization = ConfigUIUtils.getCustomization();
        DetailsViewerAdapter dvAdapter = propertiesData.getDVAdapter();
        dvAdapter.init(composite, customization);
        composite.setContent(dvAdapter.getControl());
        composite.setMinHeight(dvAdapter.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y);
        setControl(composite);
        setPageComplete(true);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        URL url = propertiesData.getSchemaURL();
        DetailsViewerAdapter dvAdapter = propertiesData.getDVAdapter();
        dvAdapter.createElement(url.toString(), propertiesData.getChosenElem(), propertiesData.getProperties());
        Element tmpElem = dvAdapter.getElement();
        tmpElem.setUserData(SchemaPropertiesCustomObject.PARENT_ELEM_DATA, propertiesData.getParentElem(), null);
        tmpElem.setUserData(SchemaPropertiesCustomObject.EDITOR_PART_DATA, propertiesData.getEditorPart(), null);
        dvAdapter.loadContents();
        super.setVisible(visible);
    }
}

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

import java.util.EventListener;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.ConfigUtils;

/**
 * Custom control with variable support.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class VariableTextCustomObject extends BaseCustomObject {

    private static final int MAX_CCOMBO_SIZE = 5;

    @Override
    public void createCustomControl(final Element input, final String itemName, Composite composite, IEditorPart editorPart, EventListener listener) {

        ConfigVarComputer configVarComputer = getConfigVarComputer(input, itemName, editorPart);
        boolean isSchemaProperties = input.getUserData(SchemaPropertiesCustomObject.EDITOR_PART_DATA) != null;

        String defaultValue = null;
        XSDSimpleTypeDefinition baseType = null;
        String baseTypeName = null;
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        if (attrDecl != null) {
            CMDataType attrType = attrDecl.getAttrType();
            defaultValue = attrDecl.getDefaultValue();
            baseType = ConfigUtils.getBaseType(attrDecl);
            baseTypeName = ConfigUtils.getBaseTypeName(baseType);
            if (baseTypeName == null && attrType != null)
                baseTypeName = attrType.getDataTypeName();
        }

        Map<String, XSDEnumerationFacet> enumMap = baseType != null ? ConfigUIUtils.getEnumerationMap(baseType) : null;

        Attr attr = input.getAttributeNode(itemName);
        String value = defaultValue;
        if (attr != null) {
            value = attr.getNodeValue();
        }

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to center
        setLabelVerticalAlign(composite, GridData.CENTER);

        if (enumMap != null && enumMap.size() > 0) {
            // If there is an enumeration, use a combo box
            final CCombo comboControl = widgetFactory.createCCombo(composite, SWT.FLAT);
            String[] enumValues = enumMap.keySet().toArray(new String[enumMap.size()]);
            comboControl.setItems(enumValues);
            comboControl.setVisibleItemCount(Math.min(enumValues.length, MAX_CCOMBO_SIZE));
            if (value != null && !value.isEmpty()) {
                comboControl.setText(value);
                comboControl.setToolTipText(value);
            }
            CComboModifiers.addContentProposalProvider(comboControl, attrDecl, enumMap, configVarComputer, baseTypeName);
            if (!isSchemaProperties)
                CComboModifiers.addVariableHyperlink(comboControl, configVarComputer);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = true;
            data.horizontalIndent = LEFT_INDENT;
            data.horizontalSpan = 2;
            comboControl.setLayoutData(data);

            comboControl.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    String text = comboControl.getText();
                    Attr attr = input.getAttributeNode(itemName);
                    if (text == null || text.isEmpty()) {
                        if (attr != null) {
                            input.removeAttributeNode(attr);
                        }
                    } else {
                        input.setAttribute(itemName, text);
                    }
                    comboControl.setToolTipText(text);
                }
            });

            comboControl.setEnabled(!getReadOnly());
        } else {
            // If no enumeration then use a text box
            final Text textControl = widgetFactory.createText(composite, "");
            if (value != null && !value.isEmpty()) {
                textControl.setText(value);
                textControl.setToolTipText(value);
            }
            TextModifiers.addVariableContentProposalProvider(textControl, configVarComputer, baseTypeName);
            if (!isSchemaProperties)
                TextModifiers.addVariableHyperlink(textControl, configVarComputer);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = true;
            data.horizontalIndent = LEFT_INDENT;
            data.horizontalSpan = 2;
            textControl.setLayoutData(data);

            textControl.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    String text = textControl.getText();
                    Attr attr = input.getAttributeNode(itemName);
                    if (text == null || text.isEmpty()) {
                        if (attr != null) {
                            input.removeAttributeNode(attr);
                        }
                    } else {
                        input.setAttribute(itemName, text);
                    }
                    textControl.setToolTipText(text);
                }
            });

            textControl.setEnabled(!getReadOnly());
        }
    }
}

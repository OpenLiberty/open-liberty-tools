/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.provisional.dialogs.PasswordDialog;

/**
 * Custom control for passwords.
 */
@SuppressWarnings("restriction")
public class PasswordTextCustomObject extends BaseCustomObject {

    @Override
    public void createCustomControl(final Element input, final String itemName, final Composite parent, final IEditorPart editorPart, final EventListener listener) {

        final Shell shell = parent.getShell();

        // Get the current password from the attribute value.
        String password = DOMUtils.getAttributeValue(input, itemName);

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Text box for the password
        final Text passwordText = widgetFactory.createText(parent, password == null ? "" : password, SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalIndent = LEFT_INDENT - 1;
        data.verticalIndent = 1;
        passwordText.setLayoutData(data);

        // Set button
        final Button setButton = widgetFactory.createButton(parent, Messages.setButton, SWT.PUSH);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = false;
        setButton.setLayoutData(data);

        // Listeners
        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = passwordText.getText();
                updateAttr(input, itemName, text);
            }
        });

        setButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                WebSphereRuntime wsRuntime = ConfigUIUtils.getRuntime(editorPart);
                if (wsRuntime == null) {
                    MessageDialog.openError(shell, Messages.title, Messages.passwordDialogNoRuntime);
                    return;
                }
                boolean isSupportHash = false;
                CMElementDeclaration elementDeclaration = SchemaUtil.getElement(input);
                if (elementDeclaration != null) {
                    CMAttributeDeclaration attribDeclaration = SchemaUtil.getAttr(elementDeclaration, itemName);
                    if (attribDeclaration != null) {
                        String type = ConfigUtils.getTypeName(attribDeclaration);
                        if (Constants.PASSWORD_HASH_TYPE.equals(type)) {
                            isSupportHash = true;
                        }
                    }
                }
                Map<String, CustomPasswordEncryptionInfo> customEncryptionMap = wsRuntime.listCustomEncryption();
                PasswordDialog dialog = new PasswordDialog(shell, isSupportHash, wsRuntime, customEncryptionMap);
                if (dialog.open() == IStatus.OK) {
                    String password = dialog.getEncodedPassword();
                    if (password != null) {
                        passwordText.setText(password);
                        String encodingMethod = dialog.getPasswordEncoding();
                        if (customEncryptionMap.containsKey(encodingMethod)) {
                            // do check whether the feature is available.
                            checkFeatureEnabled(shell, wsRuntime, itemName, input, editorPart, customEncryptionMap.get(encodingMethod).getFeatureName());
                        }
                    }
                }
            }
        });

        passwordText.setEnabled(!getReadOnly());
        setButton.setEnabled(!getReadOnly());
    }

    protected boolean checkFeatureEnabled(final Shell shell, final WebSphereRuntime wsRuntime, final String itemName, final Element element, final IEditorPart editorPart,
                                          final String featureName) {
        boolean result = false;
        Document doc = element.getOwnerDocument();
        List<String> currentFeatures = ConfigUIUtils.getFeatures(editorPart.getEditorInput(), doc);
        if (currentFeatures != null) {
            if (isListed(currentFeatures, featureName)) {
                // already exists, do nothing.
                result = true;
            } else {
                // pop up
                URI uri = ConfigUIUtils.getURI(editorPart.getEditorInput());
                List<String> featureList = new ArrayList<String>();
                featureList.add(featureName);

                EnableCustomEncryptionDialog enableDialog = new EnableCustomEncryptionDialog(shell, wsRuntime, itemName, featureList);
                if (enableDialog.open() == IStatus.OK) {
                    List<String> features = enableDialog.getSelectedFeatures();
                    for (String feature : features) {
                        Element featureElem = null;
                        Element featureManager = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.FEATURE_MANAGER);
                        if (featureManager == null) {
                            CMElementDeclaration elemDecl = SchemaUtil.getElement(doc, new String[] { Constants.SERVER_ELEMENT, Constants.FEATURE_MANAGER }, uri);
                            if (elemDecl != null) {
                                featureManager = ConfigUtils.addElement(doc.getDocumentElement(), elemDecl);
                            }
                        }
                        if (featureManager != null) {
                            CMElementDeclaration elemDecl = SchemaUtil.getElement(doc, new String[] { Constants.SERVER_ELEMENT, Constants.FEATURE_MANAGER,
                                                                                                     Constants.FEATURE }, uri);
                            if (elemDecl != null) {
                                featureElem = ConfigUtils.addElement(featureManager, elemDecl, feature);
                            }
                        }

                        if (featureElem != null) {
                            result = true;
                        } else {
                            Trace.logError("Failed to add feature: " + feature, null);
                        }
                    }
                }
            }
        } else {
            // error condition
            Trace.logError("Failed to acquire the current features.", null);
        }
        return result;
    }

    protected void updateAttr(Element elem, String attrName, String password) {
        // Update the attribute value.
        if (password == null || password.isEmpty()) {
            elem.removeAttribute(attrName);
            return;
        }
        elem.setAttribute(attrName, password);
    }

    private boolean isListed(List<String> currentFeatures, String featureName) {
        boolean result = false;
        if (currentFeatures != null && featureName != null) {
            for (String currentFeature : currentFeatures) {
                if (currentFeature.equalsIgnoreCase(featureName)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
}

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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.customization.ICustomElementListSelectionDialog;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;

/**
 * Custom object for the Add dialog customization (allows overriding the
 * editor dialog for adding child elements).
 */
@SuppressWarnings("restriction")
public class AddConfigElemCustomObject implements ICustomElementListSelectionDialog {

    /** {@inheritDoc} */
    @Override
    public void invoke(Element element) {
        Shell shell = Display.getCurrent().getActiveShell();
        AddConfigElemDialog dialog = new AddConfigElemDialog(shell, element);
        if (dialog.open() == IStatus.OK) {
            IEditorPart part = null;
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null)
                    part = page.getActiveEditor();
            }

            CMElementDeclaration newItem = dialog.getNewItem();

            // Check if the element is enabled and if not, ask the user if they
            // want to enable it (only for top level elements)
            if (Constants.SERVER_ELEMENT.equals(element.getNodeName()) && part != null) {
                URI uri = ConfigUIUtils.getURI(part.getEditorInput());
                WebSphereRuntime wsRuntime = ConfigUIUtils.getRuntime(part);
                if (uri != null && wsRuntime != null) {
                    Document doc = element.getOwnerDocument();
                    List<String> currentFeatures = ConfigUIUtils.getFeatures(part.getEditorInput(), doc);
                    List<String> possibleFeatures = ConfigUtils.getFeaturesToEnable(newItem.getElementName(), currentFeatures, wsRuntime);
                    if (possibleFeatures != null && !possibleFeatures.isEmpty()) {
                        Collections.sort(possibleFeatures);
                        EnableElementDialog enableDialog = new EnableElementDialog(shell, wsRuntime, newItem.getElementName(), possibleFeatures);
                        if (enableDialog.open() == IStatus.OK) {
                            List<String> features = enableDialog.getSelectedFeatures();
                            for (String feature : features) {
                                Element featureElem = null;
                                Element featureManager = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.FEATURE_MANAGER);
                                if (featureManager == null) {
                                    CMElementDeclaration elemDecl = SchemaUtil.getElement(doc, new String[] { Constants.SERVER_ELEMENT, Constants.FEATURE_MANAGER }, uri);
                                    if (elemDecl != null)
                                        featureManager = ConfigUtils.addElement(doc.getDocumentElement(), elemDecl);
                                }
                                if (featureManager != null) {
                                    CMElementDeclaration elemDecl = SchemaUtil.getElement(doc, new String[] { Constants.SERVER_ELEMENT, Constants.FEATURE_MANAGER,
                                                                                                             Constants.FEATURE }, uri);
                                    if (elemDecl != null) {
                                        featureElem = ConfigUtils.addElement(featureManager, elemDecl, feature);
                                    }
                                }
                                if (featureElem == null)
                                    Trace.logError("Failed to add feature: " + feature, null);
                            }
                        }
                    }
                }
            }

            // Create the new element
            Element child = ConfigUtils.addElement(element, newItem);
            if (child == null) {
                Trace.logError("Failed to add child element: " + newItem.getNodeName(), null);
                return;
            }

            // Select the new element in the editor
            if (part != null && part instanceof DDEMultiPageEditorPart) {
                ((DDEMultiPageEditorPart) part).refresh();
                ((DDEMultiPageEditorPart) part).setSelection(child);
            }
        }
    }

}

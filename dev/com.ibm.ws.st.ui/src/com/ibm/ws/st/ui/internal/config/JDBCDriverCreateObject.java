/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

/**
 * Customization object for creating a JDBC driver.
 */
public class JDBCDriverCreateObject implements IAdvancedCustomizationObject {

    /** {@inheritDoc} */
    @Override
    public String invoke(String value, Node itemNode, Element closestAncestor, IEditorPart editorPart) {
        Shell shell = editorPart.getSite().getShell();
        Document doc = closestAncestor.getOwnerDocument();
        URI docURI = ConfigUIUtils.getURI(editorPart.getEditorInput(), doc);
        UserDirectory userDir = ConfigUtils.getUserDirectory(docURI);

        String[] tags = SchemaUtil.getTags(closestAncestor);
        String label1 = AbstractChainableDialog.getLabel(doc, tags, docURI, closestAncestor.getNodeName());
        tags = AbstractChainableDialog.arrayAppend(tags, Constants.JDBC_DRIVER);
        String label2 = AbstractChainableDialog.getLabel(doc, tags, docURI, Constants.JDBC_DRIVER);
        JDBCDriverCreateDialog dialog = new JDBCDriverCreateDialog(shell, doc, docURI, userDir, tags, new String[] { label1, label2 });
        dialog.open();
        if (dialog.isOK()) {
            List<Element> elements = dialog.getElements();
            Element rootElement = doc.getDocumentElement();
            for (Element elem : elements) {
                rootElement.appendChild(doc.createTextNode("\n    "));
                rootElement.appendChild(elem);
            }
            rootElement.appendChild(doc.createTextNode("\n\n"));
            return dialog.getRefString(dialog.getIds());
        }
        return value;
    }
}

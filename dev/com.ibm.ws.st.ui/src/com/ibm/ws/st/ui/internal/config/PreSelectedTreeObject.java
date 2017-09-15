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

import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.xwt.dde.customization.ICustomPreSelectedTreeObject;

/**
 * Implements the editor customization allowing navigation to a particular
 * element in the document.
 */
public class PreSelectedTreeObject implements ICustomPreSelectedTreeObject {

    /** {@inheritDoc} */
    @Override
    public Element getPreSelectedTreeElement(Document document, IEditorInput editorInput) {
        if (editorInput instanceof IConfigEditorInput) {
            String xpath = ((IConfigEditorInput) editorInput).getXPath();
            return DOMUtils.getElement(document, xpath);
        }
        return document.getDocumentElement();
    }
}

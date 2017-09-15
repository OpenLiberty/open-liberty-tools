/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.customization;

import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Customization objects that which to open the editor with a pre-selected 
 * element should implement this interface.
 *
 */

public interface ICustomPreSelectedTreeObject {

	/**
	 * This method is invoked when the DDE is about to open. It allows an
	 * element to be pre-selected when opening the customized editor.
	 * The element to be returned must come from the document passed as
	 * argument.
	 * 
	 * @param document - The document to be displayed in the DDE
	 * 
	 * @return Element - The element to be pre-selected when opening the DDE
	 */
	public Element getPreSelectedTreeElement(Document document, IEditorInput editorInput);
}

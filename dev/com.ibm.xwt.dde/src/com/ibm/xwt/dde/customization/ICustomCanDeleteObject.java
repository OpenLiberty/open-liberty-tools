/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.customization;

import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;

/*
 *  Customization objects associated with the creation of DOM
 *  elements should implement this intercace.
 *  
 */

public interface ICustomCanDeleteObject {
	
	/**
	 * This method is invoked when the option to delete an element is displayed to the user
	 * Parameter values are set as follows:
	 * 
	 *  Element parentElement:    DOM element corresponding to the parent of the element to delete.
	 *                         
	 *  IEditorPart editorPart:   Current editor.
	 *  
	 * The boolean value returned by this method must be true if the element can be deleted and false otherwise.
	 */
	
	public boolean canDelete(Element parentElement, IEditorPart editorPart);

}

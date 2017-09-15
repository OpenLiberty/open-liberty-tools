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
 *  elements should implement this interface.
 *  
 */

public interface ICustomCreationObject {
	
	/**
	 * This method is invoked when the user attempts to create an element with a custom creator.
	 * Parameter values are set as follows:
	 * 
	 *  Element parentElement:    DOM element corresponding to the parent of the element to be created.
	 *                            or empty string if the item does not exist.
	 *                         
	 *  IEditorPart editorPart:   Current editor.
	 *  
	 * The Element value returned by this method must be the new created element, or null of no
	 * element was created. The purpose of the returned value is to set the selection to it
     * once the creation operation is finished.
	 */
	
	public Element create(Element parentElement, IEditorPart editorPart);

}

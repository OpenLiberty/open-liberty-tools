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
 *  Customization objects associated with the deletion of DOM
 *  elements should implement this intercace.
 *  
 */

public interface ICustomDeletionObject {
	
	/**
	 * This method is invoked when the user attempts to delete an element with a custom deletion class.
	 * Parameter values are set as follows:
	 * 
	 *  Element element:        DOM element user has selected for deletion
	 *                         
	 *  IEditorPart editorPart: Current editor.
	 *  
	 * The boolean value returned by this method must be true if the element was deleted and false
	 * otherwise (i.e. the user canceled its deletion). The main purpose of the return value is
	 * to know when the editor should refresh and update its contents.
	 * 
	 */
	
	public boolean delete(Element element, IEditorPart editorPart);

}

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

import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;

/*
 *  Customization objects associated with the deletion multiple of DOM
 *  elements should implement this interface. This is particularly
 *  useful when refactoring logic needs to be implemented. The custom code
 *  can, but its not required to, perform the actual removal of the elements
 *  from the document. The confirmation dialog box is not shown when custom
 *  code to handle multiple deletions is used. However, from the custom code
 *  it is possible to invoke the confirmation dialog box through its return
 *  value.   
 *  
 */

public interface ICustomMultipleDeletionObject {

	/**
	 * This method is invoked when the user attempts to delete two or more elements.
	 * Parameter values are set as follows:
	 * 
	 *  Element[] elements:        DOM elements selected for deletion
	 *                         
	 *  IEditorPart editorPart:	   Current editor.
	 *  
	 * The integer value returned by this method must be:
	 * 
	 *  -1  if no deletion process has to be performed (i.e. user cancelled)
	 *   0  if the deletion process has to be performed with no confirmation dialog
	 *   1  if the deletion process has to be performed with confirmation dialog
	 *  
	 * Examples:
	 *  If the custom code presents a dialog box for multiple deletion and the
	 *  user cancels, the method must return -1.
	 *  If the custom code presents a dialog box for multiple deletion and the
	 *  operation is sucessfull, the method must return 0.
	 *  If the custom code detects that the selection does not require any special
	 *  logic, in which case the default behaviour should be implemented, the
	 *  method must return 1.
	 * 
	 */
	
	public int multipleDelete(Element[] elements, IEditorPart editorPart);
	
}

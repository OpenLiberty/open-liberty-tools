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
import org.w3c.dom.Node;

/*
 *  Customization objects associated with button and link controls
 *  that need to know about the editing context should implement
 *  this interface
 */

public interface IAdvancedCustomizationObject {
	
	/**
	 * This method is invoked when the user clicks a button or link control.
	 * Parameter values are set as follows:
	 * 
	 *  String value:             Current value of the associated XML element/attribute
	 *                            or empty string if the item does not exist.
	 * 
	 *  Node itemNode:            DOM node corresponding to the associated XML element/attribute
	 *                            or null if the item does not exist.
	 * 
	 *  Element closestAncestor:  DOM element corresponding to the closest ancestor of the
	 *                            associated XML element/attribute.
	 *                         
	 *  IEditorPart editorPart:   Current editor.
	 *  
	 * The String value returned by this method is used to update the associated XML
	 * element/attribute, triggering its creation when required. If the return value
	 * of this method is null, no update operation is performed
	 */
	
	public String invoke(String value, Node itemNode, Element closestAncestor, IEditorPart editorPart);

}

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

import java.util.Map;

import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*

 *  
 */

public interface ICustomListValidationObject {
	/**
	 * This method is invoked when the item needs to be validated.
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
	 *  IEditorInput editorInput:      The resource being validated.
	 *  
	 * If the item is valid, the method must return null. If the item is invalid, the method must
	 * return a map of Node, validation messages (warning or error) with a brief description of the validation problem.
	 * The message must be an object of the class com.ibm.xwt.dde.customization.ValidationMessage
	 * 
	 */
	public Map<Node, ValidationMessage[]> validate(String value, Node itemNode, Element closestAncestor, IEditorInput editorInput);
}

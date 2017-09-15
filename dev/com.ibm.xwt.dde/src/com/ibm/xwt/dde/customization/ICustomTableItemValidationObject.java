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

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*
 *  Customization objects that perform the validation
 *  of a table item should implement this interface.
 *  
 */

public interface ICustomTableItemValidationObject {

	/**
	 * This method is invoked when the item needs to be validated.
	 * Parameter values are set as follows:
	 * 
	 *  String[] values:          Array of string values of the table items in the order in which
	 *  						  they appear, or an empty string array if there are no items in
	 *  						  the table.
	 * 
	 *  Node[] itemNodes:         Array of DOM nodes corresponding to the associated XML elements
	 *                            of the table items in the order in which they appear, or an empty Node
	 *                            array if there are no items in the table.
	 * 
	 *  Element closestAncestor:  DOM element corresponding to the closest ancestor of the
	 *                            associated XML elements.
	 *                         
	 *  IResournce resource:      The resource being validated.
	 *  
	 * If the item is valid, the method must return null. If the item is invalid, the method must
	 * return a message (warning or error) with a brief description of the validation problem.
	 * The message must be an object of the class com.ibm.xwt.dde.customization.ValidationMessage
	 */
	public ValidationMessage validate(String[] values, Node[] itemNodes, Element closestAncestor, IResource resource);

}

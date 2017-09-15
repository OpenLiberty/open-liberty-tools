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

/*
 *  Customization objects that perform the validation
 *  of a node (item that appears on the tree) should
 *  implement this interface.
 *  
 */

public interface ICustomNodeValidationObject {

	/**
	 * This method is invoked when the item needs to be validated.
	 * Parameter values are set as follows:
	 * 
	 *  Element treeNodeElement:  DOM Element associated with the tree node.
	 * 
	 *  IResournce resource:      The resource being validated.
	 *  
	 * If the node is valid, the method must return null. If the node contains one or more
	 * problems (errors or warnings) these should be returned in the form of an array of
	 * com.ibm.xwt.dde.customization.ValidationMessage objects, each one with a brief
	 * description of the validation problem.
	 */
	public ValidationMessage[] validate(Element treeNodeElement, IResource resource);

}

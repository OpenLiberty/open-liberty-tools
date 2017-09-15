/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others.
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

/**
 * Customization objects that which to decide whether this item
 * should be disabled or not based on another item's value should 
 * implement this interface.
 * 
 * It's recommended to use {@link ICustomDisableObject}  to trigger
 * an action when the item you would like to depend on gets changed.
 *
 */

public interface ICustomShouldItemDisableObject {

	/**
	 * This method is invoked when the items of an element might be disabled
	 * based on the value of another element.
	 * 
	 * Parameter values are set as follows:
	 * 
	 *  @param String value:             Current value of the associated XML element/attribute
	 *                            or empty string if the item does not exist.
	 * 
	 *  @param Node itemNode:            DOM node corresponding to the associated XML element/attribute
	 *                            or null if the item does not exist.
	 * 
	 *  @param Element closestAncestor:  DOM element corresponding to the closest ancestor of the
	 *                            associated XML element/attribute.
	 *                         
	 *  @param Resource resource:		  The resource being edited
	 *  
	 *  @return boolean
	 *  
	 */
	
	public boolean getDisabled(String value, Node itemNode, Element closestAncestor, IResource resource);

}

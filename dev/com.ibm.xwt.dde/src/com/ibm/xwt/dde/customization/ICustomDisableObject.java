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
 * Customization objects that which to disable other elements
 * or attributes based on this item's value should implement this
 * interface.
 *
 */

public interface ICustomDisableObject {
	

	/**
	 * This method is invoked when the DDE item has changed, letting
	 * the customization object take decisions on disabling other items
	 * based on this change.
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
	 * @return boolean - A boolean that will let the DDE know if the new value will affect
	 *  the enablement of items that depend on this.
	 * 
	 */
	public boolean setDisableTrigger(String value, Node itemNode, Element closestAncestor, IResource resource);
	
}

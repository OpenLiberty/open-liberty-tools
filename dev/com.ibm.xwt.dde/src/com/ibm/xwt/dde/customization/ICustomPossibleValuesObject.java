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

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*
 *  Objects used to populate the contents of combos should implement
 *  this interface
 */

public interface ICustomPossibleValuesObject {

	/**
	 * This method is invoked when the items of a comboBox need to be populated
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
	 *  Resource resource:		  The resource being edited
	 *  
	 * The Map value returned by this method must be not null and contain in its keys
	 * the set of labels to populate the comboBox, and in its values the set of actual
	 * values that should be written in the XML document.
	 */
	
	Map getPosibleValues(String value, Node itemNode, Element closestAncestor, IResource resource);
}

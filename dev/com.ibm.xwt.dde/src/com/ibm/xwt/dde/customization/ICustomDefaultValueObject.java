/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
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
 *  Customization objects associated with the default value
 *  calculation of document items should implement this interface.
 *  
 */

public interface ICustomDefaultValueObject {

	/**
	 * This method is invoked when the user creates a new element and the default value for its associated
	 * items need to be populated. Parameter values are set as follows:
	 * 
	 *  Element ancestor:  DOM element corresponding to the node the user selected to create, which is an
	 *  				   ancestor of the detail for which the default value is calculated.
	 *                         
	 *  IEditorPart editorPart:   Current editor.
	 *  
	 * The Element value returned by this method must be the default value to be assigned to the newly created
	 * item or null if no default value is to be assigned. If the associated item is a table, multiple entries
	 * can be specified using comma separated values. Note that if the returned value is null and the item is
	 * optional, the corresponding element tag / attribute it will not be created in the document.
	 */
	public String getDefaultValue(Element ancestor, IEditorPart editorPart);
}

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

/*
 *  Customization objects associated with button and link controls
 *  should implement this interface
 */

public interface ICustomizationObject {

	/**
	 * This method is invoked when the user clicks a button or link control.
	 * The String parameter will contain the current value of the associated
	 * XML element/attribute (or empty string if the item does not exist).
	 * The String value returned by this method is then used to update the
	 * associated XML element/attribute triggering their creation when
	 * required. If the return value of this method is null, no update
	 * operation is performed
	 */
	public String invoke(String value);

}

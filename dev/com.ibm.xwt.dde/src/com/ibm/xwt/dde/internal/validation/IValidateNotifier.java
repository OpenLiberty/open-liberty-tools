/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.validation;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.Node;

public interface IValidateNotifier extends ModifyListener, SelectionListener {

	//Call this method to notify DDE
	//about removed Elements or Attributes
	public void removalNotify(Node node);	
	
	//Validates a node associated with specific control
	//Before node is deleted from the DOM
	//call removalNotify
	
	public void validateControl(Control control);	
	
	//Call this to validate current tree node
	public void validateTreeNode();
	
	//Validates the whole document
	public void validateDocument();
}

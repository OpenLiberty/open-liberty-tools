/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.customization;

import java.util.EventListener;

import org.eclipse.ui.IEditorPart;
import org.eclipse.swt.widgets.Composite;
import org.w3c.dom.Element;

/*
 *  Customization objects associated with Support for custom item controls.
 *  
 */

public interface ICustomControlObject2 extends ICustomControlObject{
	
	/**
	 * This method is invoked when the user attempts to create customized Content in a viewer.
	 * Parameter values are set as follows:
	 * 
	 *  Element input:		      DOM element corresponding to the parent of the element to be created.
	 *                            or empty string if the item does not exist.
	 *                            
	 *  String itemName:		  The name of the item
	 *                            
	 *  Composite composite:	  The Composite to add the customized content to                          
	 *                         
	 *  IEditorPart editorPart:   Current editor
	 *  
	 *  EventListener listener:   The Listener to be notified of any change
	 *  
	 *  boolean readOnlyMode:	  Is editor in read only mode
	 *  
	 */
	
	public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, EventListener listener, boolean readOnlyMode);
	
	
	/**
	 * Post processing of custom controls.  Called after custom controls have been created and layout applied.  
	 * This can be used to set the value of any Text fields.
	 */
    public void postLayoutProcessing();
}

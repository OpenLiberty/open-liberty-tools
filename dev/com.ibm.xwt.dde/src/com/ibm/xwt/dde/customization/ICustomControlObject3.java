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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import com.ibm.xwt.dde.internal.validation.IValidateNotifier;
import java.util.List;

public interface ICustomControlObject3 extends ICustomControlObject2 {
	public final static String CUSTOM_CONTROL_DATA_DETAIL_ITEM = "CUSTOM_CONTROL_DATA_DETAIL_ITEM"; //$NON-NLS-1$
	public final static String CUSTOM_CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM = "CUSTOM_CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM"; //$NON-NLS-1$
	public final static String CUSTOM_CONTROL_TABLE_ITEM_ELEMENT = "CUSTOM_CONTROL_TABLE_ITEM_ELEMENT"; //$NON-NLS-1$
	
	public List<Control> getControls();
	
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
	 *  IValidateNotifier listener:   The Listener to be notified of any change and trigger validation
	 *  
	 *  boolean readOnlyMode:	  Is editor in read only mode
	 *  
	 */
	public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, IValidateNotifier listener, boolean readOnlyMode);
}

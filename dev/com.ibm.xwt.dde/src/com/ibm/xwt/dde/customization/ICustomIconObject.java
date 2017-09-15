/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Element;

/*
 *  Customization objects associated with treeLabels and
 *  detailSectionTitles should implement this intercace.
 *  
 */

public interface ICustomIconObject {
	
	/**
	 * This method is invoked when a icon needs to be updated.
	 * Parameter values are set as follows:
	 * 
	 *  Element element:        DOM element for which the label is needed
	 *                         
	 *  IResource resource:     Current resource being edited.
	 *
	 *  The String value returned by this method should be the label
	 *  associated with the given DOM element.
	 */
	
	public Image getIcon(CMElementDeclaration elementDecl, IResource resource);
	
	/**
	 * This method is invoked when a icon needs to be updated.
	 * Parameter values are set as follows:
	 * 
	 *  CMElementDeclaration elementDecl:    Element declaration for which the label is needed
	 *                         
	 *  IResource resource:     Current resource being edited.
	 *
	 *  The String value returned by this method should be the label
	 *  associated with the given DOM element.
	 */
	
	public Image getIcon(Element element, IResource resource);

}

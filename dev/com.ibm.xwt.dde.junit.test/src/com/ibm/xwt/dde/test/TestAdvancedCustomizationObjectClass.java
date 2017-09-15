/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.test;

import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

public class TestAdvancedCustomizationObjectClass implements
		IAdvancedCustomizationObject {

	public String invoke(String value, Node itemNode, Element closestAncestor,
			IEditorPart editorPart) {
		System.out.println("Hyperlink clicked");
		return null;
	}

}

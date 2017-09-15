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

import java.util.EventListener;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.customization.ICustomControlObject;

public class TestCustomControlClass implements ICustomControlObject {

	public void createCustomControl(Element input, String itemName,
			Composite composite, IEditorPart editorPart, EventListener listener) {
        System.out.println("Create Custom Control");
	}

    public void postLayoutProcessing() {
		// TODO Auto-generated method stub
		
	}

}

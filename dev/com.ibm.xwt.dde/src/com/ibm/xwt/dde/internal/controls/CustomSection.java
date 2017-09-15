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

package com.ibm.xwt.dde.internal.controls;

public class CustomSection extends AbstractControl {

	private String label;
	private String headerText;
	private int ending;
	
	public CustomSection(int order, String label, String headerText) {
		super(order);
		this.label = label;
		this.headerText = headerText;
	}
	
	public String getLabel() {
		return label;
	}

	public String getHeaderText() {
		return headerText;
	}

	public int getEnding() {
		return ending;
	}

	public void setEnding(int ending) {
		this.ending = ending;
	}

}
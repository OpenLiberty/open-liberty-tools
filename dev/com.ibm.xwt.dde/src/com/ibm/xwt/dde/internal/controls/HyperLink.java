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

import org.eclipse.swt.graphics.Image;

public class HyperLink extends AbstractControl {
	
	private String label;
	private Image icon;
	private String tooltip;
	private Class customCode;
	boolean leftIndentation;

	public HyperLink(int order, String label, Image icon, String tooltip, Class customCode, boolean leftIndentation) {
		super(order);
		this.label = label;
		this.icon = icon;
		this.tooltip = tooltip;
		this.customCode = customCode;
		this.leftIndentation = leftIndentation;
	}

	public String getLabel() {
		return label;
	}

	public Image getIcon() {
		return icon;
	}

	public String getTooltip() {
		return tooltip;
	}

	public Class getCustomCode() {
		return customCode;
	}

	public boolean isLeftIndentation() {
		return leftIndentation;
	}
}

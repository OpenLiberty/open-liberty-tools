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

package com.ibm.xwt.dde.internal.validation;

import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;

public class DetailItemValidation {
	
	private DetailItem detailItem;
	private SimpleDetailItem containingSimpleDetailItem;
	private ValidationMessage message;
	
	public DetailItemValidation(DetailItem detailItem, SimpleDetailItem containingSimpleDetailItem, ValidationMessage message) {
		this.detailItem = detailItem;
		this.containingSimpleDetailItem = containingSimpleDetailItem;
		this.message = message;
	}
	
	public DetailItem getDetailItem() {
		return detailItem;
	}
	
	
	public SimpleDetailItem getContainingSimpleDetailItem() {
		return containingSimpleDetailItem;
	}


	public ValidationMessage getMessage() {
		return message;
	}

}

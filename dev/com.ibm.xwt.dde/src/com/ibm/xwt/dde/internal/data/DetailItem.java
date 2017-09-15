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
package com.ibm.xwt.dde.internal.data;

import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;

public abstract class DetailItem {
	
	DetailItemCustomization detailItemCustomization;
	
	abstract public String getName();
	
	abstract public boolean isRequired();
	
	public DetailItemCustomization getDetailItemCustomization() {
		return detailItemCustomization;
	}
	
	public void setDetailItemCustomization(DetailItemCustomization detailItemCustomization) {
		this.detailItemCustomization = detailItemCustomization;
	}

}

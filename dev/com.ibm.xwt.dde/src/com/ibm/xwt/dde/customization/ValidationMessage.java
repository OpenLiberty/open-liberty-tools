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

package com.ibm.xwt.dde.customization;

public class ValidationMessage {
	
	public final static int MESSAGE_TYPE_WARNING = 0;
	public final static int MESSAGE_TYPE_ERROR = 1;
	
	private String Message;
	private int type;
	
	public ValidationMessage(String Message, int type) {
		this.Message = Message;
		this.type = type;
	}
	
	public String getMessage() {
		return Message;
	}

	public int getMessageType() {
		return type;
	}

}

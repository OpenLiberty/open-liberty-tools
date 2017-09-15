/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.data;

import java.util.Map;

import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class AtomicDetailItem extends DetailItem {
	
	abstract public boolean hasEditableValue();
	
	abstract public Map getPossibleValues();
	
	abstract public String getValue();
	
	abstract public void setValue(String newValue);
	
	abstract public boolean exists();
	
	abstract public void delete();
	
	abstract public Node getNode();
	
	abstract public String getDocumentation();
	
	abstract public Element getClosestAncestor(); 
	
	abstract public CMNode getCMNode();
	
	abstract public boolean isOptionalWithinContext();
	
}
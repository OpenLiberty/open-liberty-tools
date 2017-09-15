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

import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*
 *  Customization objects associated with the execution of
 *  hyperlinks should implement this interface.
 *  
 */

public interface ICustomHyperlinkObject {

	
	/**
	 * This method is invoked when the user clicks an hyperlink control.
	 * Parameter values are set as follows:
	 * 
	 *  Element treeNode:         DOM element corresponding to the currently
	 *                            selected tree node.
	 * 
	 *  IEditorPart editorPart:   Current editor.
	 *  
	 * The Node value returned by this method can be used to navigate to a different
	 * section of the document within the editor. If the execution of the hyperlink
	 * code deals with external resources i.e. opening another editor, then the return
	 * value should be null. 
	 */
	
	public Node hyperlink(Element treeNode, IEditorPart editorPart);
}

/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.ui.IURIEditorInput;

/**
 * Interface for configuration editor input to support navigation to a
 * particular element in the document.
 */
public interface IConfigEditorInput extends IURIEditorInput {
    public String getXPath();
}

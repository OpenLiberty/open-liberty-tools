/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

/**
 * A prompt issue that will be displayed in a prompt dialog
 */
public interface IPromptIssue {

    public String getType();

    public String getSummary();

    public String getDetails();

    public PromptAction[] getPossibleActions();

    public PromptAction getDefaultAction();
}

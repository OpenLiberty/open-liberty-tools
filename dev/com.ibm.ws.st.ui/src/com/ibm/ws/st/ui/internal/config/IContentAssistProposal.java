/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.ui.internal.config;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

/**
 * Proposal interface for ContentAssistTextModifier
 */
public interface IContentAssistProposal {

    public String getLabel();

    public Image getImage();

    public boolean isEnabled();

    public boolean hasDetails();

    public void createDetails(Composite parent);

    public String getText();

    public int getCursorPosition();

}

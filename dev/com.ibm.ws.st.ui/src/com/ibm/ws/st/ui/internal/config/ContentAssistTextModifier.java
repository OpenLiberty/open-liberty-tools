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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

/**
 * Add content assist support to a text control.
 */
public class ContentAssistTextModifier extends ContentAssistBaseModifier {

    protected final Text textBox;

    /**
     * Add content assist support to a text control. Content assist will
     * display when the user types the autoChar or Ctrl + Space.
     * 
     * @param textBox The text control
     * @param proposalProvider The proposal provider
     * @param autoChar Character that when typed will bring up the content assist
     */
    public static void addContentAssistModifier(Text textBox, IContentAssistProposalProvider proposalProvider, char autoChar) {
        ContentAssistTextModifier modifier = new ContentAssistTextModifier(textBox, proposalProvider, autoChar);
        modifier.createControls();
    }

    private ContentAssistTextModifier(Text textBox, IContentAssistProposalProvider proposalProvider, char autoChar) {
        super(textBox, proposalProvider, autoChar);
        this.textBox = textBox;
    }

    /** {@inheritDoc} */
    @Override
    protected String getText() {
        return textBox.getText();
    }

    /** {@inheritDoc} */
    @Override
    protected void setText(String text) {
        textBox.setText(text);
    }

    /** {@inheritDoc} */
    @Override
    protected Point getSelection() {
        return textBox.getSelection();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSelection(int start) {
        textBox.setSelection(start);
    }

    /** {@inheritDoc} */
    @Override
    protected void addControlListeners() {
        textBox.addModifyListener(new TextModifyListener());
        textBox.addListener(SWT.KeyDown, new KeyListener());
        textBox.addListener(SWT.MouseDown, new MouseDownListener());
        textBox.addListener(SWT.FocusOut, new FocusOutListener());
        textBox.getShell().addListener(SWT.Move, new MoveListener());
        textBox.addDisposeListener(new ControlDisposeListener());
    }

}

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

package com.ibm.ws.st.ui.internal.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;

/**
 * Add content assist support to a text control.
 */
public class ContentAssistCComboModifier extends ContentAssistBaseModifier {

    protected final CCombo comboBox;

    /**
     * Add content assist support to a combo control. Content assist will
     * display when the user types the autoChar or Ctrl + Space.
     * 
     * @param textBox The text control
     * @param proposalProvider The proposal provider
     * @param autoChar Character that when typed will bring up the content assist
     */
    public static void addContentAssistModifier(CCombo comboBox, IContentAssistProposalProvider proposalProvider, char autoChar) {
        ContentAssistCComboModifier modifier = new ContentAssistCComboModifier(comboBox, proposalProvider, autoChar);
        modifier.createControls();
    }

    private ContentAssistCComboModifier(CCombo comboBox, IContentAssistProposalProvider proposalProvider, char autoChar) {
        super(comboBox, proposalProvider, autoChar);
        this.comboBox = comboBox;
    }

    /** {@inheritDoc} */
    @Override
    protected String getText() {
        return comboBox.getText();
    }

    /** {@inheritDoc} */
    @Override
    protected void setText(String text) {
        comboBox.setText(text);
    }

    /** {@inheritDoc} */
    @Override
    protected Point getSelection() {
        return comboBox.getSelection();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSelection(int start) {
        comboBox.setSelection(new Point(start, start));
    }

    /** {@inheritDoc} */
    @Override
    protected void addControlListeners() {
        comboBox.addModifyListener(new TextModifyListener());
        comboBox.addListener(SWT.KeyDown, new KeyListener());
        comboBox.addListener(SWT.MouseDown, new MouseDownListener());
        comboBox.addListener(SWT.FocusOut, new FocusOutListener());
        comboBox.getShell().addListener(SWT.Move, new MoveListener());
        comboBox.addDisposeListener(new ControlDisposeListener());
    }

}

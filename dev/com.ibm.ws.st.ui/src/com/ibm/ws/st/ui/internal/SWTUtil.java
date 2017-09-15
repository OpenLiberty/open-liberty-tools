/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * SWT utility class
 */
public class SWTUtil {
    private static FontMetrics fontMetrics;

    protected static void initializeDialogUnits(Control testControl) {
        // Compute and store a font metric
        GC gc = new GC(testControl);
        gc.setFont(JFaceResources.getDialogFont());
        fontMetrics = gc.getFontMetrics();
        gc.dispose();
    }

    /**
     * Returns a width hint for a button control.
     */
    protected static int getButtonWidthHint(Button button) {
        int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
        return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
    }

    /**
     * Create a new button with the standard size.
     * 
     * @param comp the component to add the button to
     * @param label the button label
     * @return a button
     */
    public static Button createButton(Composite comp, String label) {
        Button b = new Button(comp, SWT.PUSH);
        b.setText(label);
        if (fontMetrics == null)
            initializeDialogUnits(comp);
        GridData data = new GridData(SWT.CENTER, SWT.BEGINNING, false, false);
        data.widthHint = getButtonWidthHint(b);
        b.setLayoutData(data);
        return b;
    }

    /**
     * Convert DLUs to pixels.
     * 
     * @param comp a component
     * @param x pixels
     * @return dlus
     */
    public static int convertHorizontalDLUsToPixels(Composite comp, int x) {
        if (fontMetrics == null)
            initializeDialogUnits(comp);
        return Dialog.convertHorizontalDLUsToPixels(fontMetrics, x);
    }

    /**
     * Convert DLUs to pixels.
     * 
     * @param comp a component
     * @param y pixels
     * @return dlus
     */
    public static int convertVerticalDLUsToPixels(Composite comp, int y) {
        if (fontMetrics == null)
            initializeDialogUnits(comp);
        return Dialog.convertVerticalDLUsToPixels(fontMetrics, y);
    }

    /**
     * Convert DLUs to pixels.
     *
     * @param comp a component
     * @param y pixels
     * @return dlus
     */
    public static int convertWidthInCharsToPixels(Composite comp, int y) {
        if (fontMetrics == null)
            initializeDialogUnits(comp);
        return Dialog.convertWidthInCharsToPixels(fontMetrics, y);
    }

    /**
     * Shorten the text to fit the size of the control.
     * Should be called after layout is done (otherwise
     * the available space will be 0).
     *
     * @param control The control to fit the text to
     * @param text The text to be shortened (if necessary)
     * @param min The minimum length of the shortened text
     * @return The shortened text
     */
    public static String shortenText(Control control, String text, int min) {
        // Shorten text to fit the given control.
        GC gc = new GC(control);
        String shortenedText = text;
        try {
            Point size = (new GC(control)).textExtent(text);
            int availSpace = control.getSize().x - control.getBorderWidth();
            if (size.x > availSpace) {
                float ratio = (float) availSpace / (float) size.x;
                int len = (int) Math.floor(text.length() * ratio);
                // Don't want to show ... by itself or with just a few characters
                // better to just cut it off in this case.  The tooltip will still
                // be there.
                if (len >= (min + 3)) {
                    len = len - 3;
                    shortenedText = text.substring(0, len) + "...";
                }
            }
        } finally {
            gc.dispose();
        }
        return shortenedText;
    }

    /**
     * Format tooltip by adding newlines in long text.
     * 
     * @param text The text to format
     * @param maxLineLen The maximum line length
     * @return The formatted text
     */
    public static String formatTooltip(String text, int maxLineLen) {
        StringBuilder s = new StringBuilder(text);
        StringBuilder sb = new StringBuilder();
        while (s.length() > maxLineLen) {
            // find a good place (a space) to split
            int ind = maxLineLen;
            while (ind > 0 && s.charAt(ind) != ' ')
                ind--;

            sb.append(s.substring(0, ind == 0 ? maxLineLen : ind));
            sb.append("\n");
            s.delete(0, ind == 0 ? maxLineLen : ind + 1);
        }
        sb.append(s);
        return sb.toString();
    }
}

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.util.EventListener;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.w3c.dom.Element;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.customization.ICustomControlObject3;
import com.ibm.xwt.dde.internal.validation.IValidateNotifier;

/**
 * Class for handling elements that are empty. Just show a label with
 * a message indicating that the element has no attributes.
 */
@SuppressWarnings("restriction")
public class EmptyElementCustomControl implements ICustomControlObject3 {

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, EventListener listener, boolean readOnlyMode) {
        this.createCustomControl(input, itemName, composite, editorPart, listener);
    }

    /** {@inheritDoc} */
    @Override
    public void postLayoutProcessing() {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, EventListener listener) {
        String elemLabel = ConfigUIUtils.getTreeLabel(input, false);
        String text = NLS.bind(Messages.emptyElementText, elemLabel);
        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Make sure no border is drawn.
        // Should be handled by setting FormToolkit.KEY_DRAW_BORDER to false
        // on the widget, but this doesn't work on some machines so need to set
        // the border style.  Just setting the border style also doesn't work
        // on some machines so both the border style and KEY_DRAW_BORDER need to be
        // set.
        // Eclipse bug 410763 is open for this problem.
        int borderStyle = widgetFactory.getBorderStyle();
        widgetFactory.setBorderStyle(SWT.NONE);

        Text textWidget = widgetFactory.createText(composite, "", SWT.FLAT | SWT.WRAP | SWT.READ_ONLY);
        textWidget.setBackground(composite.getBackground());
        GridData data = new GridData(SWT.HORIZONTAL, SWT.TOP, true, false, 1, 1);
        textWidget.setLayoutData(data);
        textWidget.setText(text);

        // Restore the border style.
        widgetFactory.setBorderStyle(borderStyle);
    }

    /** {@inheritDoc} */
    @Override
    public List<Control> getControls() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, IValidateNotifier listener, boolean readOnlyMode) {
        this.createCustomControl(input, itemName, composite, editorPart, listener);
    }

}

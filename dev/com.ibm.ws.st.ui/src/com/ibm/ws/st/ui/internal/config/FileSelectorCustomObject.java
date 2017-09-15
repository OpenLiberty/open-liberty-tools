/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.io.File;
import java.util.EventListener;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Custom control for selecting a set of files using the FileSelectorDialog.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public abstract class FileSelectorCustomObject extends BaseCustomObject {

    protected static final String JAR_FILTER = "*.jar";

    @Override
    public void createCustomControl(final Element input, final String itemName, final Composite composite, final IEditorPart editorPart, final EventListener listener) {
        final Shell shell = composite.getShell();

        final ConfigVarComputer configVarComputer = getConfigVarComputer(input, itemName, editorPart);

        String attrType = null;
        String defaultValue = null;
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        if (attrDecl != null) {
            attrType = ConfigUtils.getTypeName(attrDecl);
            defaultValue = attrDecl.getDefaultValue();
        }

        Attr attr = input.getAttributeNode(itemName);
        String value = defaultValue;
        if (attr != null) {
            value = attr.getNodeValue();
        }

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to center
        setLabelVerticalAlign(composite, GridData.CENTER);

        final Text textControl = widgetFactory.createText(composite, "");
        if (value != null && !value.isEmpty()) {
            textControl.setText(value);
            textControl.setToolTipText(value);
        }
        TextModifiers.addVariableContentProposalProvider(textControl, configVarComputer, attrType);
        TextModifiers.addVariableHyperlink(textControl, configVarComputer);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        data.horizontalIndent = LEFT_INDENT;
        textControl.setLayoutData(data);

        textControl.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = textControl.getText();
                Attr attr = input.getAttributeNode(itemName);
                if (text == null || text.isEmpty()) {
                    if (attr != null) {
                        input.removeAttributeNode(attr);
                    }
                } else {
                    input.setAttribute(itemName, text);
                }
                textControl.setToolTipText(text);
            }
        });

        final Button browseButton = widgetFactory.createButton(composite, Messages.editorBrowse, SWT.PUSH);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = false;
        browseButton.setLayoutData(data);

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String path = getPath(input, configVarComputer.getConfigVars());
                if (path == null || path.isEmpty()) {
                    MessageDialog.openError(shell, Messages.title, Messages.filesetNoDirSpecified);
                    return;
                }

                File file = new File(path);
                if (!file.exists()) {
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.filesetDirNotFound, path));
                    return;
                }

                FileSelectorDialog dialog = new FileSelectorDialog(shell, path, getTitle(), getLabel(), getMessage());
                dialog.setInitialFilter(getDefaultFilter());
                if (dialog.open() == IStatus.OK) {
                    textControl.setText(dialog.getFileList());
                }
            }
        });

        textControl.setEnabled(!getReadOnly());
        browseButton.setEnabled(!getReadOnly());
    }

    protected String resolvePath(String path, String base) {
        String result = path;
        if (path != null) {
            File file = new File(path);
            if (!file.isAbsolute() && base != null) {
                IPath ipath = new Path(base);
                ipath.append(path);
                result = ipath.toOSString();
            }
        }
        return result;
    }

    protected abstract String getPath(Element elem, ConfigVars vars);

    protected abstract String getTitle();

    protected abstract String getLabel();

    protected abstract String getMessage();

    protected abstract String getDefaultFilter();
}

/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Parent class for chainable dialogs.
 *
 * TODO: One fine day have this read from the schema and generate the
 * contents for any element.
 */
public abstract class AbstractChainableDialog extends TitleAreaDialog {

    protected Document doc;
    protected URI docURI;
    protected UserDirectory userDir;
    protected Rectangle parentLocation = null;
    protected String[] tags;
    protected String[] labels;

    public AbstractChainableDialog(Shell parent, Document doc, URI docURI, UserDirectory userDir,
                                   String[] tags, String[] labels) {
        super(parent);
        this.doc = doc;
        this.docURI = docURI;
        this.userDir = userDir;
        this.tags = tags.clone();
        this.labels = labels.clone();
    }

    public void setParentLocation(Rectangle location) {
        parentLocation = location;
    }

    /** {@inheritDoc} */
    @Override
    protected Point getInitialLocation(Point initialSize) {
        if (parentLocation != null) {
            return new Point(parentLocation.x + 10, parentLocation.y + 10);
        }
        return super.getInitialLocation(initialSize);
    }

    public abstract String[] getIds();

    public abstract List<Element> getElements();

    protected String[] getIds(String elemName) {
        if (doc == null || docURI == null || userDir == null) {
            return new String[0];
        }

        WebSphereServerInfo serverInfo = ConfigUtils.getServer(docURI);
        return DOMUtils.getIds(doc, docURI, serverInfo, userDir, elemName);
    }

    protected String getRefString(String[] ids) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String id : ids) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public static String[] arrayAppend(String[] array, String newItem) {
        String[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = newItem;
        return newArray;
    }

    protected Composite createTopLevelComposite(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 3;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.minimumWidth = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());
        return composite;
    }

    protected void createBreadcrumb(Composite composite) {
        if (labels.length < 3) {
            // Don't show on first dialog.
            return;
        }
        String breadcrumb = getBreadcrumb();
        String msg = NLS.bind(Messages.chainableConfigContext, breadcrumb);
        StyledText text = new StyledText(composite, SWT.NONE);
        text.setBackground(composite.getBackground());
        text.setText(msg);
        text.setToolTipText(breadcrumb);
        StyleRange style = new StyleRange();
        style.start = 0;
        style.length = msg.length();
        style.fontStyle = SWT.ITALIC;
        text.setStyleRange(style);
        GridData data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        data.horizontalSpan = 3;
        text.setLayoutData(data);
    }

    protected String getBreadcrumb() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String label : labels) {
            if (first) {
                first = false;
            } else {
                builder.append(" -> ");
            }
            builder.append(label);
        }
        return builder.toString();
    }

    protected static String getLabel(Document doc, String[] tags, URI docURI, String defaultLabel) {
        String label = SchemaUtil.getLabel(doc, tags, docURI);
        if (label == null) {
            label = defaultLabel;
        }
        return label;
    }

}

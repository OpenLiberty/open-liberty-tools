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
package com.ibm.ws.st.ui.internal;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;

import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.URILocation;

public class ConfigHyperlink implements IHyperlink {
    private final IRegion region;
    private final URILocation location;
    private final String name;

    /**
     * Create a new ${x} hyperlink.
     */
    public ConfigHyperlink(IRegion region, URILocation location, String name) {
        this.region = region;
        this.location = location;
        this.name = name;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        String msgKey = Messages.open;
        if (location instanceof DocumentLocation) {
            switch (((DocumentLocation) location).getType()) {
                case SERVER_XML:
                    msgKey = Messages.openConfigFileEditor;
                    break;
                case BOOTSTRAP:
                    msgKey = Messages.openBootstrapPropertiesEditor;
                    break;
                case SERVER_ENV:
                    msgKey = Messages.openServerEnvEditor;
                    break;
                default:
                    break;
            }
        }
        return NLS.bind(msgKey, name);
    }

    @Override
    public void open() {
        if (location instanceof DocumentLocation) {
            DocumentLocation docLocation = (DocumentLocation) location;
            IEditorPart editorPart = Activator.openEditor(docLocation);
            if (editorPart != null)
                Activator.goToLocation(editorPart, docLocation);
        } else {
            Activator.open(location);
        }
    }

    @Override
    public String toString() {
        return "Link " + name + " resolved to  " + location.getLocationString();
    }
}

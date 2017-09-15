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

import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class FilesetIncludesCustomObject extends FileSelectorCustomObject {

    /** {@inheritDoc} */
    @Override
    protected String getPath(Element elem, ConfigVars vars) {
        String path = elem.getAttribute(Constants.FILESET_DIR);
        if (path != null) {
            path = resolvePath(vars.resolve(path), vars.getValue(ConfigVars.SERVER_OUTPUT_DIR));
        }
        return path;
    }

    /** {@inheritDoc} */
    @Override
    protected String getTitle() {
        return Messages.filesetIncludesTitle;
    }

    /** {@inheritDoc} */
    @Override
    protected String getLabel() {
        return Messages.filesetIncludesLabel;
    }

    /** {@inheritDoc} */
    @Override
    protected String getMessage() {
        return Messages.filesetIncludesMessage;
    }

    /** {@inheritDoc} */
    @Override
    protected String getDefaultFilter() {
        return JAR_FILTER;
    }

}

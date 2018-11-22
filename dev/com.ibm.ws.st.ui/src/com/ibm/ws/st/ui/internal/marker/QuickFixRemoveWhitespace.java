/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for removing whitespace from server environment files.
 */
public class QuickFixRemoveWhitespace extends AbstractMarkerResolution {
    @Override
    public String getLabel() {
        return Messages.whitespaceQuickFix;
    }

    @Override
    public void run(IMarker marker) {
        int start = marker.getAttribute(IMarker.CHAR_START, 0);
        int end = marker.getAttribute(IMarker.CHAR_END, 0);

        IFile file = getResource(marker);
        if (file == null)
            return;
        InputStreamReader in = null;
        try {
            // read file
            in = new InputStreamReader(file.getContents(), file.getCharset());
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[256];
            int n = in.read(buf);
            while (n >= 0) {
                sb.append(buf, 0, n);
                n = in.read(buf);
            }
            in.close();

            // remove whitespace and save
            sb.delete(start, end);
            file.setContents(new ByteArrayInputStream(sb.toString().getBytes(file.getCharset())), true, true, null);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix for removing whitespace failed", e);
            showErrorMessage();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                //ignore

            }
        }
    }

    @Override
    protected String getErrorMessage() {
        return Messages.whitespaceQuickFixFailed;
    }
}

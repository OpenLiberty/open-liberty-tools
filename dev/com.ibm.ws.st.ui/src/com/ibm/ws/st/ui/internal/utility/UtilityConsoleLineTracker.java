/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.utility;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;

import com.ibm.ws.st.ui.internal.Trace;

public class UtilityConsoleLineTracker implements IConsoleLineTracker {
    class AbstractHyperlink implements IHyperlink {
        @Override
        public void linkEntered() {
            // do nothing
        }

        @Override
        public void linkExited() {
            // do nothing
        }

        @Override
        public void linkActivated() {
            // do nothing
        }
    }

    private IConsole console;

    @Override
    public void init(IConsole console) {
        this.console = console;
    }

    @Override
    public void lineAppended(IRegion line) {
        try {
            int offset = line.getOffset();
            int length = line.getLength();
            String text = console.getDocument().get(offset, length);

            if (text != null) {
                int[] p = UtilityMessageHelper.getPath(text);
                if (p != null) {
                    final String linkText = text.substring(p[0], p[1]);
                    final File file = new File(linkText);
                    if (file.isAbsolute()) {
                        IHyperlink link = new AbstractHyperlink() {
                            @Override
                            public void linkActivated() {
                                try {
                                    File file2 = file;
                                    if (!file.getName().endsWith(".log"))
                                        file2 = file.getParentFile();
                                    Desktop.getDesktop().open(file2);
                                } catch (IOException e) {
                                    Trace.logError("Error opening folder " + linkText, e);
                                }
                            }
                        };
                        console.addLink(link, offset + p[0], p[1] - p[0]);
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem trying to add links to console", e);
        }
    }

    @Override
    public void dispose() {
        // ignore
    }
}

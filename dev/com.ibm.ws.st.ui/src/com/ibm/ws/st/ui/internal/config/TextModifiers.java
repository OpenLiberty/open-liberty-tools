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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.ui.internal.Activator;

/**
 * Modifiers for configuration editor text controls.
 */
public class TextModifiers extends ControlModifiersBase {

    public static void addVariableContentProposalProvider(Text textControl, ConfigVarComputer configVarComputer, String type) {
        IContentAssistProposalProvider proposalProvider = new MultiProposalProvider(new VariableProposalProvider(configVarComputer, type, false));
        ContentAssistTextModifier.addContentAssistModifier(textControl, proposalProvider, '$');
    }

    public static void addVariableHyperlink(final Text textControl, final ConfigVarComputer configVarComputer) {
        textControl.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.F3) {
                    ConfigVars configVars = configVarComputer.getConfigVars();
                    Point point = textControl.getSelection();
                    int startOffset = point.x;
                    int endOffset = point.y;
                    if (point.x != point.y) {
                        endOffset = endOffset - 1;
                    }
                    String fullText = textControl.getText();
                    String varName = ConfigVarsUtils.getVariableName(fullText, startOffset, endOffset);
                    if (varName != null && !varName.isEmpty()) {
                        DocumentLocation location = configVars.getDocumentLocation(varName);
                        if (location != null && location.getURI() != null) {
                            IEditorPart editorPart = Activator.openEditor(location);
                            if (editorPart != null && !location.isConfigFile())
                                Activator.goToLocation(editorPart, location);
                        }
                    }
                }
            }
        });
    }
}

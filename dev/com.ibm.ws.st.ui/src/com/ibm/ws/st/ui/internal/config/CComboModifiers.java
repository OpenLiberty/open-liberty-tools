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

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.xsd.XSDEnumerationFacet;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.ui.internal.Activator;

/**
 * Modifiers for configuration editor text controls.
 */
@SuppressWarnings("restriction")
public class CComboModifiers extends ControlModifiersBase {

    public static void addContentProposalProvider(CCombo comboControl, CMAttributeDeclaration attrDecl, Map<String, XSDEnumerationFacet> enumMap,
                                                  ConfigVarComputer configVarComputer, String type) {
        IContentAssistProposalProvider proposalProvider = new MultiProposalProvider(new EnumProposalProvider(attrDecl, enumMap), new VariableProposalProvider(configVarComputer, type, true));
        ContentAssistCComboModifier.addContentAssistModifier(comboControl, proposalProvider, '$');
    }

    public static void addVariableHyperlink(final CCombo comboControl, final ConfigVarComputer configVarComputer) {
        comboControl.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.F3) {
                    ConfigVars configVars = configVarComputer.getConfigVars();
                    Point point = comboControl.getSelection();
                    int startOffset = point.x;
                    int endOffset = point.y;
                    if (point.x != point.y) {
                        endOffset = endOffset - 1;
                    }
                    String fullText = comboControl.getText();
                    String varName = ConfigVarsUtils.getVariableName(fullText, startOffset, endOffset);
                    if (varName != null && !varName.isEmpty()) {
                        DocumentLocation location = configVars.getDocumentLocation(varName);
                        if (location != null && location.getURI() != null) {
                            Activator.openEditor(location);
                        }
                    }
                }
            }
        });
    }
}

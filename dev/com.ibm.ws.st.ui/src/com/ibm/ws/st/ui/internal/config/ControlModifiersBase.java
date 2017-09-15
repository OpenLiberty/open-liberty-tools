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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.xsd.XSDEnumerationFacet;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Modifiers for configuration editor text controls.
 */
public abstract class ControlModifiersBase {

    protected static final IContentAssistProposal[] NO_PROPOSALS = new IContentAssistProposal[0];

    public static class VariableProposalProvider implements IContentAssistProposalProvider {

        private final ConfigVarComputer configVarComputer;
        private final String type;
        private final boolean singleEntry;

        public VariableProposalProvider(ConfigVarComputer configVarComputer, String type, boolean singleEntry) {
            this.configVarComputer = configVarComputer;
            this.type = type;
            this.singleEntry = singleEntry;
        }

        @Override
        public IContentAssistProposal[] getProposals(String text, int startOffset, int endOffset) {
            // Determine if there are any proposals given the current position in
            // the document and the variables that are available.

            ConfigVars configVars = configVarComputer.getConfigVars();

            String startStr = text.substring(0, startOffset);
            String endStr = "";
            String match = "";
            int start = startStr.lastIndexOf("${");
            int end = startStr.lastIndexOf("}");
            if (start >= 0 && end < start) {
                // In a variable which may be terminated (with '}') or not.
                // If terminated then replace the whole thing.  If not
                // terminated just replace from current position back to start.
                // Match is from the first character after '${' to the current
                // position.
                start += 2;
                match = startStr.substring(start);
                startStr = startStr.substring(0, start);
                end = text.indexOf("}", startOffset);
                if (end >= 0) {
                    endStr = text.substring(end);
                } else {
                    endStr = "}" + text.substring(startOffset);
                }
            } else if (startOffset > 0 && text.charAt(startOffset - 1) == '$') {
                // If the position is just to the right of a '$', replace
                // the selection, nothing to match.
                match = "";
                startStr = startStr + "{";
                endStr = "}" + text.substring(endOffset);
            } else {
                // No signs of a variable.
                if (singleEntry && (!startStr.isEmpty() || !text.substring(endOffset).isEmpty())) {
                    // For single entry if there is text that is not part of the selection,
                    // return empty set.
                    return NO_PROPOSALS;
                }
                // Replace the current selection.
                match = "";
                startStr = startStr + "${";
                endStr = "}" + text.substring(endOffset);
            }

            // Get the matching global variables
            List<String> names = configVars.getGlobalVars(type, true, false);
            List<String> varNames = ConfigUIUtils.getSortedMatches(match, names);
            ArrayList<IContentAssistProposal> proposals = new ArrayList<IContentAssistProposal>(varNames.size());
            String typeName = configVars.getTypeName(type);
            for (String varName : varNames) {
                String str = startStr + varName + endStr;
                IContentAssistProposal proposal = new VarContentProposal(varName, typeName, str, startStr.length() + varName.length() + 1, configVars);
                proposals.add(proposal);
            }

            // Get the matching implicit local variables
            if (!configVars.isGlobalScope()) {
                names = configVars.getScopedVars(type, false, false);
                varNames = ConfigUIUtils.getSortedMatches(match, names);
                if (!varNames.isEmpty()) {
                    proposals.add(new LabelProposal(Messages.contentAssistImplicitLocalLabel));
                    for (String varName : varNames) {
                        String str = startStr + varName + endStr;
                        IContentAssistProposal proposal = new VarContentProposal(varName, typeName, str, startStr.length() + varName.length() + 1, configVars);
                        proposals.add(proposal);
                    }
                }
            }

            return proposals.toArray(new IContentAssistProposal[proposals.size()]);
        }
    }

    public static class LabelProposal implements IContentAssistProposal {

        private final String label;

        public LabelProposal(String label) {
            this.label = label;
        }

        /** {@inheritDoc} */
        @Override
        public String getLabel() {
            return "-- " + label + " --";
        }

        /** {@inheritDoc} */
        @Override
        public Image getImage() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isEnabled() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasDetails() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void createDetails(Composite parent) {
            // Do nothing
        }

        /** {@inheritDoc} */
        @Override
        public String getText() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int getCursorPosition() {
            return 0;
        }

    }

    public static class VarContentProposal implements IContentAssistProposal {

        String varName;
        String typeName;
        String fullText;
        int cursorPos;
        ConfigVars vars;

        public VarContentProposal(String varName, String typeName, String fullText, int cursorPos, ConfigVars vars) {
            this.varName = varName;
            this.typeName = typeName;
            this.fullText = fullText;
            this.cursorPos = cursorPos;
            this.vars = vars;
        }

        /** {@inheritDoc} */
        @Override
        public String getLabel() {
            return varName;
        }

        /** {@inheritDoc} */
        @Override
        public Image getImage() {
            return Activator.getImage(Activator.IMG_VARIABLE_REF);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasDetails() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void createDetails(Composite parent) {
            StyledText text = new StyledText(parent, SWT.WRAP);
            text.setLayoutData(new GridData(GridData.FILL_BOTH));
            text.setBackground(parent.getBackground());
            text.setForeground(parent.getForeground());
            List<StyleRange> styles = new ArrayList<StyleRange>(3);
            StringBuilder builder = new StringBuilder();
            addItem(builder, styles, Messages.contentAssistVariableLabel, varName, true);
            addItem(builder, styles, Messages.contentAssistTypeLabel, typeName, true);
            addItem(builder, styles, Messages.contentAssistValueLabel, vars.getValue(varName), false);
            text.setText(builder.toString());
            for (StyleRange style : styles)
                text.setStyleRange(style);
        }

        /** {@inheritDoc} */
        @Override
        public String getText() {
            return fullText;
        }

        /** {@inheritDoc} */
        @Override
        public int getCursorPosition() {
            return cursorPos;
        }

    }

    @SuppressWarnings("restriction")
    public static class EnumProposalProvider implements IContentAssistProposalProvider {

        private final CMAttributeDeclaration attrDecl;
        private final Map<String, XSDEnumerationFacet> enumMap;
        private final List<String> values;

        public EnumProposalProvider(CMAttributeDeclaration attrDecl, Map<String, XSDEnumerationFacet> enumMap) {
            this.attrDecl = attrDecl;
            this.enumMap = enumMap;
            values = new ArrayList<String>(enumMap.keySet());
        }

        @Override
        @SuppressWarnings("deprecation")
        public IContentAssistProposal[] getProposals(String text, int startOffset, int endOffset) {
            // Determine if there are any proposals given the current position in
            // the document and the enumerators that are available.
            String match = text.substring(0, startOffset);

            // Get the matching values
            List<String> valueMatches = ConfigUIUtils.getSortedMatches(match, values);
            String defaultValue = attrDecl.getDefaultValue();
            ArrayList<IContentAssistProposal> proposals = new ArrayList<IContentAssistProposal>(valueMatches.size());
            for (String value : valueMatches) {
                IContentAssistProposal proposal = new EnumProposal(enumMap.get(value), value.equals(defaultValue), value.length());
                proposals.add(proposal);
            }

            return proposals.toArray(new IContentAssistProposal[proposals.size()]);
        }
    }

    public static class EnumProposal implements IContentAssistProposal {

        XSDEnumerationFacet enumFacet;
        boolean isDefault;
        int cursorPos;

        public EnumProposal(XSDEnumerationFacet enumFacet, boolean isDefault, int cursorPos) {
            this.enumFacet = enumFacet;
            this.isDefault = isDefault;
            this.cursorPos = cursorPos;
        }

        /** {@inheritDoc} */
        @Override
        public String getLabel() {
            return enumFacet.getLexicalValue();
        }

        /** {@inheritDoc} */
        @Override
        public Image getImage() {
            if (isDefault)
                return Activator.getImage(Activator.IMG_ENUMERATOR_DEFAULT);
            return Activator.getImage(Activator.IMG_ENUMERATOR);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasDetails() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void createDetails(Composite parent) {
            StyledText text = new StyledText(parent, SWT.WRAP | SWT.V_SCROLL);
            text.setLayoutData(new GridData(GridData.FILL_BOTH));
            text.setBackground(parent.getBackground());
            text.setForeground(parent.getForeground());
            List<StyleRange> styles = new ArrayList<StyleRange>(2);
            StringBuilder builder = new StringBuilder();
            addItem(builder, styles, Messages.contentAssistValueLabel, enumFacet.getLexicalValue(), true);
            String description = SchemaUtil.getDocumentation(enumFacet.getAnnotation());
            if (description != null)
                addItem(builder, styles, Messages.contentAssistDescriptionLabel, description, true);
            text.setText(builder.toString());
            for (StyleRange style : styles)
                text.setStyleRange(style);
            Point parentSize = parent.getSize();
            Point textSize = text.computeSize(parentSize.x, SWT.DEFAULT, true);
            text.getVerticalBar().setVisible(parentSize.y <= textSize.y);
        }

        /** {@inheritDoc} */
        @Override
        public String getText() {
            return enumFacet.getLexicalValue();
        }

        /** {@inheritDoc} */
        @Override
        public int getCursorPosition() {
            return cursorPos;
        }
    }

    protected static void addItem(StringBuilder builder, List<StyleRange> styles, String label, String value, boolean addNewline) {
        int offset = builder.length();
        builder.append(label);
        builder.append("  ");
        styles.add(new StyleRange(offset, label.length(), null, null, SWT.BOLD));
        builder.append(value);
        if (addNewline) {
            builder.append("\n");
        }
    }

    public static class MultiProposalProvider implements IContentAssistProposalProvider {

        private final IContentAssistProposalProvider[] providers;

        public MultiProposalProvider(IContentAssistProposalProvider... providers) {
            this.providers = providers;
        }

        @Override
        public IContentAssistProposal[] getProposals(String text, int startOffset, int endOffset) {
            List<IContentAssistProposal> allProposals = new ArrayList<IContentAssistProposal>();
            for (IContentAssistProposalProvider provider : providers) {
                IContentAssistProposal[] proposals = provider.getProposals(text, startOffset, endOffset);
                allProposals.addAll(Arrays.asList(proposals));
            }
            if (allProposals.isEmpty())
                allProposals.add(new LabelProposal(Messages.contentAssistEmpty));

            return allProposals.toArray(new IContentAssistProposal[allProposals.size()]);
        }
    }
}

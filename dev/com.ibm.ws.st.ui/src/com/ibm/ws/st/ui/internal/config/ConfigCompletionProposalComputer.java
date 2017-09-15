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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.config.URILocation;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Compute completion proposals for the source view of the config editor.
 */
@SuppressWarnings("restriction")
public class ConfigCompletionProposalComputer extends AbstractXMLCompletionProposalComputer {

    /** {@inheritDoc} */
    @Override
    protected void addAttributeNameProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    /** {@inheritDoc} */
    @Override
    protected void addAttributeValueProposals(ContentAssistRequest request, CompletionProposalInvocationContext context) {
        Node node = request.getNode();
        if (node == null) {
            return;
        }

        Attr attr = getAttributeNode(request);
        Document document = node.getOwnerDocument();
        if (document == null || attr == null) {
            return;
        }

        CMAttributeDeclaration attrDecl = null;
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(document);
        if (modelQuery != null) {
            attrDecl = modelQuery.getCMAttributeDeclaration(attr);
        }

        List<ICompletionProposal> proposals = computeProposals(request, document, attr, attrDecl);
        for (ICompletionProposal proposal : proposals) {
            request.addProposal(proposal);
        }

        return;
    }

    @Override
    protected void addCommentProposal(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addEmptyDocumentProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addEndTagNameProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addEndTagProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addEntityProposals(ContentAssistRequest arg0, ITextRegion arg1, IDOMNode arg2, CompletionProposalInvocationContext arg3) {
        return;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addEntityProposals(Vector arg0, Properties arg1, String arg2, int arg3, IStructuredDocumentRegion arg4, ITextRegion arg5,
                                      CompletionProposalInvocationContext arg6) {
        return;
    }

    @Override
    protected void addPCDATAProposal(String arg0, ContentAssistRequest arg1, CompletionProposalInvocationContext arg2) {
        return;
    }

    @Override
    protected void addStartDocumentProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addTagCloseProposals(ContentAssistRequest arg0, CompletionProposalInvocationContext arg1) {
        return;
    }

    @Override
    protected void addTagInsertionProposals(ContentAssistRequest arg0, int arg1, CompletionProposalInvocationContext arg2) {
        return;
    }

    @Override
    protected void addTagNameProposals(ContentAssistRequest arg0, int arg1, CompletionProposalInvocationContext arg2) {
        return;
    }

    @Override
    public void sessionEnded() {
        return;
    }

    @Override
    public void sessionStarted() {
        return;
    }

    private List<ICompletionProposal> computeProposals(ContentAssistRequest request, Document document, Attr attr, CMAttributeDeclaration attrDecl) {
        IEditorInput editorInput = ConfigUIUtils.getActiveEditorInput();

        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

        String type = null;
        boolean isRefType = false;
        if (attrDecl != null) {
            type = ConfigUtils.getTypeName(attrDecl);
            isRefType = ConfigVars.getTypeSet(type) == ConfigVars.REFERENCE_TYPES;
            if (isRefType) {
                // If reference type then compute reference proposals.
                URI docURI = null;
                WebSphereServerInfo serverInfo = null;
                UserDirectory userDir = null;
                if (editorInput != null) {
                    docURI = ConfigUIUtils.getURI(editorInput, document);
                    if (docURI != null) {
                        serverInfo = ConfigUtils.getServerInfo(docURI);
                        userDir = serverInfo != null ? serverInfo.getUserDirectory() : ConfigUtils.getUserDirectory(docURI);
                    }
                }
                String[] references = SchemaUtil.getReferences(attrDecl);
                Map<String, String> idMap = new HashMap<String, String>();
                for (String reference : references) {
                    Map<String, URILocation> map = ConfigUIUtils.getIdMap(document, docURI, serverInfo, userDir, reference);
                    for (String id : map.keySet())
                        idMap.put(id, reference);
                }
                proposals.addAll(computeReferenceProposals(request, idMap));
            }
        }

        // Compute variable proposals (but not for variable names themselves)
        if (!isVariableName(attr)) {
            // Don't include variables defined in the configuration files if this is
            // an include location attribute
            Element elem = attr.getOwnerElement();
            boolean isIncludeLocation = elem != null && Constants.INCLUDE_ELEMENT.equals(elem.getNodeName()) && Constants.LOCATION_ATTRIBUTE.equals(attr.getNodeName());
            ConfigVars configVars;
            if (isRefType) {
                configVars = ConfigUIUtils.getConfigVars(editorInput, isIncludeLocation ? null : document);
            } else {
                configVars = ConfigUIUtils.getConfigVars(editorInput, isIncludeLocation ? null : elem, isIncludeLocation ? null : attr.getName());
            }
            proposals.addAll(computeVariableProposals(request, configVars, type));
        }

        return proposals;
    }

    private List<ICompletionProposal> computeVariableProposals(ContentAssistRequest request, ConfigVars vars, String type) {
        // Determine if there are any proposals given the current position in
        // the document and the variables that are available.
        ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();

        int offset = request.getReplacementBeginPosition();
        String text = request.getText();
        String match = request.getMatchString();
        if (text == null || match == null || offset < 0) {
            return list;
        }

        int matchLen = match.length();
        String prefix = "";
        String suffix = "";
        String endQuote = "";
        int replaceStart = offset;
        int replaceEnd = replaceStart;
        if ("=".equals(text)) {
            // No attribute value at all, not even the quotation marks
            prefix = "\"${";
            suffix = "}";
            endQuote = "\"";
        } else {
            prefix = "${";
            suffix = "}";
            boolean inVariable = false;
            boolean start = true;
            int matchOffset = 0;
            for (int index = 0; index < matchLen; index++) {
                char c = match.charAt(index);
                switch (c) {
                    case '"':
                        if (start) {
                            // Skip past the opening quote if there is one
                            matchOffset = 1;
                        }
                        break;
                    case '$':
                        if (index + 1 < matchLen && match.charAt(index + 1) == '{') {
                            inVariable = true;
                            matchOffset = index + 2;
                            index++;
                            prefix = "";
                        } else if (index + 1 == matchLen) {
                            // If $ and at end of match
                            inVariable = true;
                            matchOffset = index + 1;
                            prefix = "{";
                        }
                        break;
                    case '}':
                        if (inVariable) {
                            inVariable = false;
                            matchOffset = index + 1;
                            prefix = "${";
                        }
                        break;
                    case ',':
                    case ' ':
                        if (!inVariable) {
                            matchOffset = index + 1;
                            prefix = "${";
                        }
                        break;
                    default:
                        break;
                }
                if (start) {
                    start = false;
                }
            }
            if (inVariable) {
                // If variable is terminated, replace the whole thing, if not
                // just replace the match part.
                match = matchOffset >= matchLen ? "" : match.substring(matchOffset);
                replaceStart = offset + matchOffset;
                int varEnd = text.indexOf("}", matchOffset);
                if (varEnd >= 0) {
                    suffix = "}";
                    replaceEnd = offset + varEnd + 1;
                } else {
                    suffix = "}";
                    replaceEnd = offset + matchLen;
                }
            } else if (matchOffset > 0 && matchOffset < matchLen) {
                match = match.substring(matchOffset);
                replaceStart = offset + matchOffset;
                replaceEnd = replaceStart + match.length();
            } else {
                // Not in a variable so just append at end of match
                match = "";
                replaceStart = offset + matchLen;
                replaceEnd = replaceStart;
            }
        }

        List<String> names = vars.getVars(type, true);
        List<String> varNames = ConfigUIUtils.getSortedMatches(match, names);
        String typeName = vars.getTypeName(type);
        for (String varName : varNames) {
            String str = prefix + varName + suffix;
            int strlen = str.length();
            String fullStr = str + endQuote;
            String value = vars.getValue(varName);
            if (value == null)
                value = Messages.contentAssistUnresolved;
            ICompletionProposal proposal = ConfigCompletionProposal.createVariableProposal(fullStr, replaceStart, replaceEnd - replaceStart, strlen, varName, typeName, value);
            list.add(proposal);
        }

        return list;
    }

    private List<ICompletionProposal> computeReferenceProposals(ContentAssistRequest request, Map<String, String> idMap) {
        // Determine if there are any proposals given the current position in
        // the document and the ids that are available.
        ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();

        int offset = request.getReplacementBeginPosition();
        String text = request.getText();
        String match = request.getMatchString();
        if (text == null || match == null || offset < 0) {
            return list;
        }

        int matchLen = match.length();
        String prefix = "";
        String endQuote = "";
        int replaceStart = offset;
        int replaceEnd = replaceStart;
        if ("=".equals(text)) {
            // No attribute value at all, not even the quotation marks
            prefix = "\"";
            endQuote = "\"";
        } else {
            boolean inVariable = false;
            boolean start = true;
            int matchOffset = 0;
            for (int index = 0; index < matchLen; index++) {
                char c = match.charAt(index);
                switch (c) {
                    case '"':
                        if (start) {
                            // Skip past the opening quote if there is one
                            matchOffset = 1;
                        }
                        break;
                    case '$':
                        if (index + 1 < matchLen && match.charAt(index + 1) == '{') {
                            inVariable = true;
                            index++;
                        }
                        break;
                    case '}':
                        if (inVariable) {
                            inVariable = false;
                            matchOffset = -1;
                        }
                        break;
                    case ',':
                    case ' ':
                        if (!inVariable) {
                            matchOffset = index + 1;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (inVariable || matchOffset == -1) {
                // Not a valid location for a reference so return empty list
                return list;
            } else if (matchOffset > 0 && matchOffset < matchLen) {
                match = match.substring(matchOffset);
                replaceStart = offset + matchOffset;
                replaceEnd = replaceStart + match.length();
            } else if (matchOffset >= matchLen) {
                match = "";
                replaceStart = offset + matchLen;
                replaceEnd = replaceStart;
            }
        }

        List<String> ids = new ArrayList<String>(idMap.keySet());
        List<String> idMatches = ConfigUIUtils.getSortedMatches(match, ids);
        for (String idMatch : idMatches) {
            String str = prefix + idMatch;
            int strlen = str.length();
            String fullStr = str + endQuote;
            ICompletionProposal proposal = ConfigCompletionProposal.createReferenceProposal(fullStr, replaceStart, replaceEnd - replaceStart, strlen, idMatch, idMap.get(idMatch));
            list.add(proposal);
        }

        return list;
    }

    private Attr getAttributeNode(ContentAssistRequest request) {
        // Determine the attribute for which the user is currently
        // trying to specify the value.  Needed in order to get the type.
        Node node = request.getNode();
        if (node == null) {
            return null;
        }

        Attr attr = null;
        switch (node.getNodeType()) {
            case Node.ATTRIBUTE_NODE:
                attr = (Attr) node;
                break;
            case Node.ELEMENT_NODE:
                Element elem = (Element) node;
                ITextRegion valueRegion = request.getRegion();
                IStructuredDocumentRegion docRegion = request.getDocumentRegion();
                if (valueRegion != null && docRegion != null) {
                    ITextRegionList textRegions = docRegion.getRegions();
                    int index = textRegions.indexOf(valueRegion) - 1;
                    while (index >= 0) {
                        ITextRegion textRegion = textRegions.get(index--);
                        if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(textRegion.getType())) {
                            String name = docRegion.getFullText(textRegion);
                            attr = elem.getAttributeNode(name);
                            break;
                        }
                    }
                }
                break;
            default:
                break;
        }
        return attr;
    }

    protected boolean isVariableName(Attr attr) {
        // Check if this attribute is a variable name attribute
        if (attr != null && Constants.VARIABLE_NAME.equals(attr.getNodeName())) {
            Element elem = attr.getOwnerElement();
            if (elem != null && Constants.VARIABLE_ELEMENT.equals(elem.getNodeName())) {
                return true;
            }
        }
        return false;
    }

    protected static class ConfigCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3 {
        // Provide styled text for the additional information popup.

        protected final ICompletionProposal proposal;
        protected final List<StyleRange> styles;

        public static ConfigCompletionProposal createVariableProposal(String replaceStr, int replaceStart, int replaceLen, int cursorPos, String varName, String varType,
                                                                      String varValue) {
            List<StyleRange> styles = new ArrayList<StyleRange>(3);
            StringBuilder builder = new StringBuilder();
            addItem(builder, styles, Messages.contentAssistVariableLabel, varName, true);
            addItem(builder, styles, Messages.contentAssistTypeLabel, varType, true);
            addItem(builder, styles, Messages.contentAssistValueLabel, varValue, false);
            ICompletionProposal proposal = new CompletionProposal(replaceStr, replaceStart, replaceLen, cursorPos, Activator.getImage(Activator.IMG_VARIABLE_REF), varName, null, builder.toString());
            return new ConfigCompletionProposal(proposal, styles);
        }

        public static ConfigCompletionProposal createReferenceProposal(String replaceStr, int replaceStart, int replaceLen, int cursorPos, String idName, String reference) {
            List<StyleRange> styles = new ArrayList<StyleRange>(3);
            StringBuilder builder = new StringBuilder();
            addItem(builder, styles, Messages.contentAssistIdLabel, idName, true);
            addItem(builder, styles, Messages.contentAssistTypeLabel, reference, true);
            ICompletionProposal proposal = new CompletionProposal(replaceStr, replaceStart, replaceLen, cursorPos, Activator.getImage(Activator.IMG_FACTORY_REF), idName, null, builder.toString());
            return new ConfigCompletionProposal(proposal, styles);
        }

        private ConfigCompletionProposal(ICompletionProposal proposal, List<StyleRange> styles) {
            this.proposal = proposal;
            this.styles = styles;
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

        @Override
        public IInformationControlCreator getInformationControlCreator() {
            return new IInformationControlCreator() {
                @Override
                public IInformationControl createInformationControl(Shell parent) {
                    return new DefaultInformationControl(parent, new ConfigProposalInformationPresenter());
                }
            };
        }

        @Override
        public int getPrefixCompletionStart(IDocument arg0, int arg1) {
            return 0;
        }

        @Override
        public CharSequence getPrefixCompletionText(IDocument arg0, int arg1) {
            return null;
        }

        @Override
        public void apply(IDocument doc) {
            proposal.apply(doc);
        }

        @Override
        public String getAdditionalProposalInfo() {
            return proposal.getAdditionalProposalInfo();
        }

        @Override
        public IContextInformation getContextInformation() {
            return proposal.getContextInformation();
        }

        @Override
        public String getDisplayString() {
            return proposal.getDisplayString();
        }

        @Override
        public Image getImage() {
            return proposal.getImage();
        }

        @Override
        public Point getSelection(IDocument doc) {
            return proposal.getSelection(doc);
        }

        protected class ConfigProposalInformationPresenter implements IInformationPresenter {

            /** {@inheritDoc} */
            @Override
            public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
                for (StyleRange style : styles) {
                    presentation.addStyleRange(style);
                }
                return hoverInfo;
            }
        }
    }
}

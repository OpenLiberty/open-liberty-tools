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
package com.ibm.ws.st.core.internal.config.validation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidatorMessage;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;

/**
 * Supports resource validation for server configuration files.
 */
public class ResourceConfigurationValidator extends AbstractConfigurationValidator {
    private final ValidationResult result = new ValidationResult();

    public ResourceConfigurationValidator() {
        super();
    }

    @Override
    protected void createMessage(String text, IResource resource, Level level, Node node, QuickFixData fixData) {
        ValidatorMessage message = ValidatorMessage.create(text, resource);
        message.setType(ConfigurationValidator.MARKER_TYPE);
        message.setAttribute(IMarker.SEVERITY, getSeverity(level));

        if (node != null) {
            DocumentLocation docLocation = DocumentLocation.createDocumentLocation(node);
            if (docLocation.getLine() != -1) {
                message.setAttribute(IMarker.LINE_NUMBER, docLocation.getLine());
                if (docLocation.getStartOffset() != -1 && docLocation.getEndOffset() != -1) {
                    message.setAttribute(IMarker.CHAR_START, docLocation.getStartOffset());
                    message.setAttribute(IMarker.CHAR_END, docLocation.getEndOffset());
                }
            }

            final QuickFixType fixType = fixData.getFixType();
            if (fixType != QuickFixType.NONE) {
                final String xpath = DOMUtils.createXPath(node);
                message.setAttribute(QUICK_FIX_TYPE_ATTR, fixType.ordinal());
                message.setAttribute(XPATH_ATTR, xpath);
                if (fixData.getBestMatch() != null) {
                    message.setAttribute(BEST_MATCH, fixData.getBestMatch());
                }
                if (fixData.getUndefinedReferenceName() != null) {
                    message.setAttribute(REFERENCE_NAME, fixData.getUndefinedReferenceName());
                }
                if (fixData.getUndefinedReferenceOffset() >= 0) {
                    message.setAttribute(REFERENCE_OFFSET, fixData.getUndefinedReferenceOffset());
                }
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    message.setAttribute(ELEMENT_NODE_NAME, node.getNodeName());
                } else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                    final Node elemNode = ((Attr) node).getOwnerElement();
                    if (elemNode != null) {
                        message.setAttribute(ELEMENT_NODE_NAME, elemNode.getNodeName());
                    }
                    message.setAttribute(ATTRIBUTE_NODE_NAME, node.getNodeName());
                }
                HashMap<String, Object> attributes = fixData.getAttributes();
                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    message.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }

        result.add(message);
    }

    public ValidationResult getValidationResult() {
        return result;
    }

    private int getSeverity(Level level) {
        switch (level) {
            case INFO:
                return IMarker.SEVERITY_INFO;
            case WARNING:
                return IMarker.SEVERITY_WARNING;
            default:
                return IMarker.SEVERITY_ERROR;
        }
    }
}

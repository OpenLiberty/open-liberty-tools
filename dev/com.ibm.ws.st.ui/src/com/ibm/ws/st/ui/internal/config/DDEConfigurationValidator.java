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
package com.ibm.ws.st.ui.internal.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.validation.AbstractConfigurationValidator;
import com.ibm.xwt.dde.customization.ICustomItemValidationObject;
import com.ibm.xwt.dde.customization.ICustomNodeValidationObject;
import com.ibm.xwt.dde.customization.ValidationMessage;

/**
 * Supports editor validation for server configuration files.
 */
public class DDEConfigurationValidator extends AbstractConfigurationValidator implements ICustomNodeValidationObject, ICustomItemValidationObject {

    private final List<ValidationMessage> messages = new ArrayList<ValidationMessage>();

    private Node validateNode = null;

    public DDEConfigurationValidator() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public ValidationMessage[] validate(Element treeNodeElement, IResource resource) {
        if (!DOMUtils.isServerElement(treeNodeElement)) {
            validateNode = treeNodeElement;
        }
        validate(treeNodeElement.getOwnerDocument(), resource);
        return messages.toArray(new ValidationMessage[messages.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationMessage validate(String value, Node itemNode, Element closestAncestor, IResource resource) {
        validateNode = itemNode;
        validate(closestAncestor.getOwnerDocument(), resource);
        if (messages.size() > 0) {
            return messages.get(0);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected void createMessage(String text, IResource resource, Level level, Node node, QuickFixData fixData) {
        if (validateNode != null) {
            if (!node.isSameNode(validateNode)) {
                if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                    if (!(((Attr) node).getOwnerElement()).isSameNode(validateNode)) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        StringBuilder builder = new StringBuilder(text);
        String location = getTopLevelLocation();
        if (location != null) {
            builder.append("; URI:");
            builder.append(location);
        }

        if (node != null) {
            DocumentLocation docLocation = DocumentLocation.createDocumentLocation(node);
            if (docLocation.getLine() != -1) {
                builder.append(", Line: ");
                builder.append(docLocation.getLine());
                if (docLocation.getColumn() != -1) {
                    builder.append(", Column: ");
                    builder.append(docLocation.getColumn());
                }
            }
        }

        ValidationMessage message = new ValidationMessage(builder.toString(), getType(level));
        messages.add(message);
    }

    private int getType(Level level) {
        switch (level) {
            case INFO:
            case WARNING:
                return ValidationMessage.MESSAGE_TYPE_WARNING;
            default:
                return ValidationMessage.MESSAGE_TYPE_ERROR;
        }
    }

}

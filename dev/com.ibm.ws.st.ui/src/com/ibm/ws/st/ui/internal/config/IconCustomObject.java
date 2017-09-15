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

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.xsd.XSDTypeDefinition;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.xwt.dde.customization.ICustomIconObject;

/**
 * Provides the icons for the tree view.
 * 
 * All hard coded for now until some categories can get added to the schema.
 */
@SuppressWarnings("restriction")
public class IconCustomObject implements ICustomIconObject {

    /** {@inheritDoc} */
    @Override
    public Image getIcon(CMElementDeclaration elementDecl, IResource resource) {
        if (elementDecl == null)
            return Activator.getImage(Activator.IMG_CONFIG_ELEM);

        XSDTypeDefinition typeDef = SchemaUtil.getTypeDefinitionFromSchema(elementDecl);
        if (typeDef == null)
            return Activator.getImage(Activator.IMG_CONFIG_ELEM);

        String typeName = typeDef.getName();
        if (typeName == null || typeName.isEmpty()) {
            return Activator.getImage(Activator.IMG_CONFIG_ELEM);
        } else if (typeName.contains("security")) {
            return Activator.getImage(Activator.IMG_SECURITY_ELEM);
        } else if (typeName.startsWith("includeType")) {
            return Activator.getImage(Activator.IMG_INCLUDE_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.kernel.feature")) {
            return Activator.getImage(Activator.IMG_FEATURES_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.app.manager.monitor")) {
            return Activator.getImage(Activator.IMG_APP_MONITOR_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.app.manager")) {
            return Activator.getImage(Activator.IMG_APP_MANAGER_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.jdbc") || typeName.startsWith("com.ibm.ws.jndi")) {
            return Activator.getImage(Activator.IMG_DATASOURCE_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.logging")) {
            return Activator.getImage(Activator.IMG_LOGGING_ELEM);
        } else if (typeName.startsWith("variableDefinitionType")) {
            return Activator.getImage(Activator.IMG_VARIABLE_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.ssl")) {
            return Activator.getImage(Activator.IMG_SSL_ELEM);
        } else if (typeName.startsWith("com.ibm.ws.http") || typeName.startsWith("com.ibm.ws.session")) {
            return Activator.getImage(Activator.IMG_HTTP_ELEM);
        } else {
            return Activator.getImage(Activator.IMG_CONFIG_ELEM);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Image getIcon(Element element, IResource resource) {
        CMElementDeclaration elemDecl = ConfigUIUtils.getElementDecl(element);
        return getIcon(elemDecl, resource);
    }

}

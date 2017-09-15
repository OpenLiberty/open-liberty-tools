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
package com.ibm.ws.st.jee.core.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.config.DOMUtils;

/**
 *
 */
@SuppressWarnings("restriction")
public class DeploymentDescriptorHelper {
    private static final String XML_FILENAME = "/WEB-INF/ibm-web-ext.xml";
    private static final String XMI_FILENAME = "/WEB-INF/ibm-web-ext.xmi";
    private static final String APP_XML_NAME = "/META-INF/application.xml";
    private static final String MODULE = "module";
    private static final String WEB = "web";
    private static final String WEB_URI = "web-uri";
    private static final String CONTEXT_ROOT = "context-root";

    /**
     * Returns the context root from ibm-web-ext.xmi file
     * 
     * @param root the root of the web project
     * @return
     */
    public static String getContextRootFromExtXmi(IVirtualFolder root) {
        if (root == null)
            return null;
        Element top = getDocumentRoot(root, XMI_FILENAME);
        if (top == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unable to read context root from xmi of " + root.getName() + " The document element was null.");
            return null;
        }

        String context_root = top.getAttribute("contextRoot");
        context_root = context_root.trim();
        if (!context_root.isEmpty())
            return context_root;
        return null;
    }

    /**
     * Returns the context root from ibm-web-ext.xml file
     * 
     * @param root the root of the web project
     * @return
     */
    public static String getContextRootFromExtXml(IVirtualFolder root) {
        if (root == null)
            return null;
        Node node = getDocumentRoot(root, XML_FILENAME);
        if (node == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unable to read context root from xml of " + root.getName() + " The document element was null.");
            return null;
        }

        Element element = DOMUtils.getFirstChildElement(node, CONTEXT_ROOT);
        if (element != null) {
            String context_root = element.getAttribute("uri");
            context_root = context_root.trim();
            if (!context_root.isEmpty())
                return context_root;
        }
        return null;
    }

    /**
     * Returns the component root of the project
     * 
     * @param project
     * @return
     */
    public static IVirtualFolder getComponentRoot(IProject project) {
        if (project == null)
            return null;
        IVirtualComponent vc = ComponentCore.createComponent(project);
        return vc.getRootFolder();
    }

    private static Element getDocumentRoot(IVirtualFolder root, String filePath) {
        if (root == null || filePath == null)
            return null;
        IVirtualFile vFile = root.getFile(filePath);
        IFile iFile = vFile.getUnderlyingFile();
        if (!iFile.exists())
            return null;
        InputStream fileContent = null;
        try {
            fileContent = iFile.getContents();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(fileContent);
            return doc.getDocumentElement();
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unable to get the document element of " + root.getName() + " / " + filePath, e);
        } finally {
            if (fileContent != null)
                try {
                    fileContent.close();
                } catch (IOException e) {
                    //Nothing to do.
                }
        }
        return null;
    }

    /**
     * Returns the context root of the web module from the application.xml
     * 
     * @param appRoot
     * @param deploymentName the uri of the web module
     * @return
     */
    public static String getWebContextRootFromEAR(IVirtualFolder appRoot, String deploymentName) {
        if (appRoot == null || deploymentName == null)
            return null;
        Node node = getDocumentRoot(appRoot, APP_XML_NAME);
        if (node == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unable to read application.xml for " + appRoot.getName() + " The document element was null.");
            return null;
        }

        for (Element moduleElem = DOMUtils.getFirstChildElement(node, MODULE); moduleElem != null; moduleElem = DOMUtils.getNextElement(moduleElem, MODULE)) {
            Element web = DOMUtils.getFirstChildElement(moduleElem, WEB);
            if (web != null) {
                Element web_uri = DOMUtils.getFirstChildElement(web, WEB_URI);
                if (deploymentName.equals(DOMUtils.getTextContent(web_uri))) {
                    Element context_root = DOMUtils.getFirstChildElement(web, CONTEXT_ROOT);
                    return DOMUtils.getTextContent(context_root);
                }
            }
        }
        return null;
    }

}

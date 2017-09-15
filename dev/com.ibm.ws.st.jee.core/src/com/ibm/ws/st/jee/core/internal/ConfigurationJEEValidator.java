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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.ValidatorMessage;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;

/**
 *
 */
@SuppressWarnings("restriction")
public class ConfigurationJEEValidator extends AbstractValidator {
    private static final String MARKER_TYPE = Activator.PLUGIN_ID + ".jeeConfigmarker";
    private static final ValidationResult EMPTY_RESULT = new ValidationResult();
    private static final String DOT_SETTINGS_COMPONENT_FILE_NAME = ".settings/org.eclipse.wst.common.component";
    private static final String WAR_TYPE = "war";
    private static final String JST_WEB = "jst.web";
    private static final String XML_FILENAME = "/WEB-INF/ibm-web-ext.xml";
    private static final String XMI_FILENAME = "/WEB-INF/ibm-web-ext.xmi";

    @Override
    public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor) {
        if (!(resource instanceof IFile)) {
            return EMPTY_RESULT;
        }
        ValidationResult result = new ValidationResult();
        ArrayList<IResource> dependsOn = new ArrayList<IResource>();
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        final WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        final URI uri = resource.getLocationURI();
        for (WebSphereServerInfo server : servers) {
            final ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null && !monitor.isCanceled()) {
                List<Element> appElems = DOMUtils.getApplicationElements(configFile.getDomDocument());
                try {
                    resource.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
                } catch (CoreException e) {
                    if (Trace.ENABLED)
                        Trace.logError("Error removing markers: " + MARKER_TYPE, e);
                }
                for (Element appElem : appElems) {
                    if (monitor.isCanceled())
                        return result;
                    if (WAR_TYPE.equals(appElem.getAttribute(Constants.APP_TYPE))) {
                        String appContextRoot = appElem.getAttribute(Constants.APP_CONTEXT_ROOT);
                        String appName = configFile.getAppName(appElem);
                        IProject project = wsRoot.getProject(appName);
                        if (project != null) {
                            IModule[] modules = ServerUtil.getModules(project);
                            for (IModule module : modules) {
                                if (JST_WEB.equals(module.getModuleType().getId())) {
                                    IWebModule webModule = (IWebModule) module.loadAdapter(IWebModule.class, null);
                                    IVirtualFolder root = null;
                                    if (webModule != null) {
                                        String webContextRoot = webModule.getContextRoot();
                                        final Attr contextRootNode = appElem.getAttributeNode(Constants.APP_CONTEXT_ROOT);
                                        // Either the attribute is missing or its value is empty
                                        if (appContextRoot.isEmpty()) {
                                            root = DeploymentDescriptorHelper.getComponentRoot(project);
                                            String contextRootInExt; //  = getContextRootFromExt(root);
                                            if ((contextRootInExt = DeploymentDescriptorHelper.getContextRootFromExtXml(root)) != null) {
                                                if (!isContextRootsEqual(contextRootInExt, webContextRoot))
                                                    addMarker(result, resource, getXMIMismatchMsg(root, appName, true), appElem);
                                            } else if ((contextRootInExt = DeploymentDescriptorHelper.getContextRootFromExtXmi(root)) != null) {
                                                if (!isContextRootsEqual(contextRootInExt, webContextRoot))
                                                    addMarker(result, resource, getXMIMismatchMsg(root, appName, false), appElem);
                                            } else if (!isContextRootsEqual(appName, webContextRoot)) {
                                                if (contextRootNode == null) {
                                                    addMarker(result, resource, webContextRoot, appName, appElem);
                                                } else {
                                                    addMarker(result, resource, NLS.bind(Messages.warningWebContextRootNotMatch, appName), contextRootNode);
                                                }
                                            }
                                        } else {
                                            if (!isContextRootsEqual(appContextRoot, webContextRoot)) {
                                                addMarker(result, resource, NLS.bind(Messages.warningWebContextRootNotMatch, appName), contextRootNode);
                                            }
                                        }
                                    }
                                    IResource dot_Component = project.findMember(DOT_SETTINGS_COMPONENT_FILE_NAME);
                                    if (dot_Component != null)
                                        dependsOn.add(dot_Component);
                                    if (root != null) {
                                        dependsOn.add(root.getFile(XML_FILENAME).getUnderlyingResource());
                                        dependsOn.add(root.getFile(XMI_FILENAME).getUnderlyingResource());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!dependsOn.isEmpty()) {
            result.setDependsOn(dependsOn.toArray(new IResource[dependsOn.size()]));
        }
        return result;
    }

    private String getXMIMismatchMsg(IVirtualFolder root, String appName, boolean isXML) {
        String fileName;
        if (isXML)
            fileName = XML_FILENAME;
        else
            fileName = XMI_FILENAME;

        IVirtualFile vFile = root.getFile(fileName);
        IFile iFile = vFile.getUnderlyingFile();
        return NLS.bind(Messages.warningWebContextRootNotMatchXMI, appName, iFile.getFullPath().toPortableString());

    }

    private boolean isContextRootsEqual(final String contextRoot1, final String contextRoot2) {
        if (contextRoot1 == null || contextRoot2 == null)
            return false;
        String a = contextRoot1.trim();
        String b = contextRoot2.trim();
        if (a.equals(b))
            return true;

        a = constructFullContextRoot(a);
        b = constructFullContextRoot(b);

        return a.equals(b);
    }

    private String constructFullContextRoot(final String s) {
        if (s.isEmpty())
            return s;
        String r;
        if (s.charAt(0) != '/')
            r = "/" + s;
        else
            r = s;

        if (!r.endsWith("/"))
            r = r + "/";
        return r;
    }

    private void addMarker(ValidationResult result, IResource resource, String contextRoot, String appName, Node node) {
        result.add(createMessage(NLS.bind(Messages.warningWebContextRootNotMatchProjectName, new String[] { contextRoot, appName }), resource, IMarker.SEVERITY_WARNING, node));
    }

    private void addMarker(ValidationResult result, IResource resource, String msg, Node node) {
        result.add(createMessage(msg, resource, IMarker.SEVERITY_WARNING, node));
    }

    protected ValidatorMessage createMessage(String text, IResource resource, int level, Node node) {
        ValidatorMessage message = ValidatorMessage.create(text, resource);
        message.setType(MARKER_TYPE);
        message.setAttribute(IMarker.SEVERITY, level);

        if (node != null) {
            DocumentLocation docLocation = DocumentLocation.createDocumentLocation(node);
            if (docLocation.getLine() != -1) {
                message.setAttribute(IMarker.LINE_NUMBER, docLocation.getLine());
                if (docLocation.getStartOffset() != -1 && docLocation.getEndOffset() != -1) {
                    message.setAttribute(IMarker.CHAR_START, docLocation.getStartOffset());
                    message.setAttribute(IMarker.CHAR_END, docLocation.getEndOffset());
                }
            }

        }
        return message;
    }

}

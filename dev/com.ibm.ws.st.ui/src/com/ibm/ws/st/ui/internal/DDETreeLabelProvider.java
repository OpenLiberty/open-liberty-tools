/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.ui.ServerUICore;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.IConfigurationElement;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.ui.internal.RuntimeExplorer.NodeType;
import com.ibm.xwt.dde.internal.validation.ValidationManager;
import com.ibm.xwt.dde.internal.viewers.DDEViewer;
import com.ibm.xwt.dde.internal.viewers.DDEViewer.TreeFilterProcessor;
import com.ibm.xwt.dde.internal.viewers.TreeLabelProvider;

@SuppressWarnings("restriction")
public class DDETreeLabelProvider extends TreeLabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider, IDescriptionProvider {

    public DDETreeLabelProvider() {
        super(DDETreeContentProvider.getCustomization(), getTreeFilterProcessor(), getValidationManager(), null);
    }

    protected static ValidationManager getValidationManager() {
        return new ValidationManager(DDETreeContentProvider.getCustomization(), null, ValidationManager.WORKBENCH_VALIDATION);
    }

    protected static TreeFilterProcessor getTreeFilterProcessor() {
        DDEViewer viewer = new DDEViewer(null, DDETreeContentProvider.getCustomization(), null);
        return viewer.getTreeFilterProcessor();
    }

    @Override
    public String getText(Object element) {
        if (element instanceof IRuntime)
            return ((IRuntime) element).getName() + " (" + ((IRuntime) element).getLocation() + ")";
        if (element instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) element;
            return node.getName();
        }
        if (element instanceof WebSphereServerInfo) {
            WebSphereServerInfo server = (WebSphereServerInfo) element;
            return server.getServerName();
        }
        if (element instanceof UserDirectory) {
            UserDirectory userDir = (UserDirectory) element;
            if (userDir.getProject() != null)
                return userDir.getProject().getName();
            return userDir.getPath().toOSString();
        }
        if (element instanceof IConfigurationElement) {
            IConfigurationElement configElement = (IConfigurationElement) element;
            return configElement.getName();
        }
        if (element instanceof ExtendedConfigFile) {
            ExtendedConfigFile configFile = (ExtendedConfigFile) element;
            return configFile.getName();
        }
        if (element instanceof CustomServerConfigTreeNode) {
            return ((CustomServerConfigTreeNode) element).getLabel();
        }
        if (element instanceof String)
            return (String) element;
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IRuntime)
            return ServerUICore.getLabelProvider().getImage(element);
        if (element instanceof UserDirectory) {
            UserDirectory userDir = (UserDirectory) element;
            if (userDir.getProject() != null)
                return Activator.getImage(Activator.IMG_USER_PROJECT);
            return Activator.getImage(Activator.IMG_USER_FOLDER);
        }
        if (element instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) element;
            if (node.getType() == NodeType.SERVERS)
                return Activator.getImage(Activator.IMG_SERVER_FOLDER);
            if (node.getType() == NodeType.SHARED_CONFIGURATIONS)
                return Activator.getImage(Activator.IMG_CONFIG_FOLDER);
            return Activator.getImage(Activator.IMG_APP_FOLDER);
        }
        if (element instanceof WebSphereServerInfo)
            return Activator.getImage(Activator.IMG_SERVER);
        if (element instanceof ConfigurationFolder)
            return Activator.getImage(Activator.IMG_CONFIG_FOLDER);
        if (element instanceof ConfigurationFile)
            return Activator.getImage(Activator.IMG_CONFIG_FILE);
        if (element instanceof ExtendedConfigFile) {
            ExtendedConfigFile configFile = (ExtendedConfigFile) element;
            Image image = Activator.getImage(configFile.getName());
            if (image == null) {
                if (element instanceof Bootstrap) {
                    image = Activator.getImage(Activator.IMG_BOOTSTRAP_PROPS);
                } else if (element instanceof ServerEnv) {
                    image = Activator.getImage(Activator.IMG_SERVER_ENV);
                }
            }
            return image;
        }
        if (element instanceof CustomServerConfigTreeNode) {
            return ((CustomServerConfigTreeNode) element).getIcon();
        }

        return super.getImage(element);
    }

    @Override
    public StyledString getStyledText(Object element) {
        StyledString.Styler styler = StyledString.QUALIFIER_STYLER;
        if (element instanceof Element) {
            try {
                Element element2 = (Element) element;
                if (element2.getParentNode() != null && element2.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
                    StyledString ss = new StyledString(super.getText(element));
                    URI uri = DDETreeContentProvider.getURI(element2);
                    IPath path = new Path(uri.getPath());
                    ss.append(" [" + path.lastSegment() + "]", StyledString.DECORATIONS_STYLER);

                    String s = element2.getAttribute("description");
                    if (s != null && !s.trim().isEmpty())
                        ss.append(" " + getFirstSegment(s), styler);

                    return ss;
                }
                if (Constants.INCLUDE_ELEMENT.equals(element2.getNodeName())) {
                    return new StyledString(super.getText(element));
                }
                StyledString ss = new StyledString(super.getText(element));
                String s = getFeatures(element2, null);
                if (s != null)
                    ss.append(s, styler);
                return ss;
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error getting node text", e);
            }
        }

        if (element instanceof IRuntime) {
            IRuntime runtime = (IRuntime) element;
            StyledString ss = new StyledString(runtime.getName());
            ss.append(" [" + runtime.getLocation().toPortableString() + "]", styler);
            return ss;
        }

        if (element instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) element;
            return new StyledString(node.getName());
        }
        if (element instanceof WebSphereServerInfo) {
            WebSphereServerInfo serverInfo = (WebSphereServerInfo) element;

            WebSphereServer server = WebSphereUtil.getWebSphereServer(serverInfo);
            if (server != null) {
                StyledString ss = new StyledString(serverInfo.getServerName());
                ss.append(" [" + server.getServer().getName() + "]", styler);
                return ss;
            }

            return new StyledString(serverInfo.getServerName());
        }

        if (element instanceof ConfigurationFile) {
            ConfigurationFile configFile = (ConfigurationFile) element;
            StyledString ss = new StyledString(configFile.getName());

            String s = configFile.getServerDescription();
            if (s != null && !s.trim().isEmpty())
                ss.append(" " + getFirstSegment(s), styler);

            return ss;
        }

        if (element instanceof ExtendedConfigFile) {
            ExtendedConfigFile extendedConfigFile = (ExtendedConfigFile) element;
            String name = extendedConfigFile.getName();
            StyledString ss = new StyledString(name);

            if (element instanceof Bootstrap && !ExtendedConfigFile.BOOTSTRAP_PROPS_FILE.equals(name)) {
                ss.append(" (" + ExtendedConfigFile.BOOTSTRAP_PROPS_FILE + ')', styler);
            } else if (element instanceof ServerEnv && !ExtendedConfigFile.SERVER_ENV_FILE.equals(name)) {
                ss.append(" (" + ExtendedConfigFile.SERVER_ENV_FILE + ')', styler);
            }
            return ss;
        }

        if (element instanceof String)
            return new StyledString((String) element, styler);
        return new StyledString(getText(element));
    }

    private String getFeatures(Element element, String s) {
        StringBuilder sb = new StringBuilder();
        String t = getNodeDetails(element);
        if (t != null && t.length() > 0)
            sb.append(t);

        if (Constants.FEATURE_MANAGER.equals(element.getNodeName())) {
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (sb.length() > 40)
                    return sb.toString() + "...";
                t = getNodeDetails(n);
                if (t != null)
                    sb.append(t);
            }
        }

        return sb.toString();
    }

    private String getNodeDetails(Node element) {
        if (element == null)
            return null;

        StringBuilder sb = new StringBuilder();

        Text text = getTextNode(element);
        if (text != null) {
            String c = text.getData();
            if (c != null && c.trim().length() > 0) {
                sb.append(" ");
                sb.append(c.trim());
            }
        }

        List<String> names = getNames(element);
        for (String n : names) {
            if (sb.length() > 40) {
                sb.append("...");
                return sb.toString();
            }
            if (element instanceof Element) {
                sb.append(" ");
                sb.append(n);
                sb.append("=");
                sb.append(getString((Element) element, n));
            }
        }
        return sb.toString();
    }

    private Text getTextNode(Node element) {
        if (element == null)
            return null;

        NodeList nodes = element.getChildNodes();
        int size = nodes.getLength();
        if (size == 0)
            return null;

        for (int nX = 0; nX < size; nX++) {
            Node node = nodes.item(nX);
            if (node instanceof Text)
                return (Text) node;
        }
        // couldn't find a Text node
        return null;
    }

    private List<String> getNames(Node element) {
        if (element == null)
            return new ArrayList<String>(0);
        NamedNodeMap map = element.getAttributes();
        List<String> list = new ArrayList<String>();
        if (map == null)
            return list;
        int size = map.getLength();
        for (int i = 0; i < size; i++) {
            Node node = map.item(i);
            String name = node.getNodeName();
            if (!Constants.INSTANCE_ID.equals(name))
                list.add(name);
        }
        return list;
    }

    private String getString(Element element, String key) {
        if (element == null)
            return null;
        Attr attr = element.getAttributeNode(key);
        if (attr == null)
            return null;
        return attr.getValue();
    }

    private String getDescription(Element element) {
        if (element == null)
            return "null";
        StringBuilder sb = new StringBuilder("<");
        sb.append(element.getNodeName());

        List<String> names = getNames(element);
        for (String n : names) {
            sb.append(" ");
            sb.append(n);
            sb.append("=");
            sb.append(getString(element, n));
        }

        Text text = getTextNode(element);
        if (text != null) {
            String c = text.getData();
            if (c != null && c.trim().length() > 0) {
                sb.append(">");
                sb.append(c.trim());
                sb.append("</");
                sb.append(element.getNodeName());
                sb.append(">");
            } else
                sb.append("/>");
        } else
            sb.append("/>");

        return sb.toString();
    }

    @Override
    public String getDescription(Object anElement) {
        if (anElement instanceof Element) {
            Element element = (Element) anElement;
            if (element.getParentNode() != null && element.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
                URI uri = DDETreeContentProvider.getURI(element);
                if ("file".equals(uri.getScheme()))
                    return new File(uri).getAbsolutePath();

                return uri.toString().replace("%20", " ");
            }

            return getDescription(element);
        } else if (anElement instanceof ConfigurationFile) {
            ConfigurationFile file = (ConfigurationFile) anElement;
            URI uri = file.getURI();
            if ("file".equals(uri.getScheme()))
                return new File(uri).getAbsolutePath();

            return uri.toString().replace("%20", " ");
        }
        return null;
    }

    private static String getFirstSegment(String s) {
        if (s == null)
            return s;

        boolean cut = false;
        String ss = s.trim();
        ss = ss.replace("\r\n", " ");
        ss = ss.replace("\n", " ");
        ss = ss.replace("\r", " ");

        if (ss.length() > 40) {
            int ind = ss.lastIndexOf(" ", 40);
            if (ind > 30) {
                ss = ss.substring(0, ind);
                cut = true;
            }

            if (ss.length() > 37) {
                ss = ss.substring(0, Math.min(ss.length(), 37));
                cut = true;
            }
        }

        if (cut)
            ss += "...";

        return ss;
    }
}

/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilderImpl;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;

import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.LibertyRuntimeProvider;
import com.ibm.ws.st.core.internal.LibertyRuntimeProviderExtension;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 * Configuration file utilities.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class ConfigUtils {
    private static final String CONTENT_TYPE_ID = "com.ibm.ws.st.configuration";

    public static final int PASSWORD_OK = 0;
    public static final int PASSWORD_PLAIN_TEXT = 1;
    public static final int PASSWORD_NOT_SUPPORT_AES = 2;
    public static final int PASSWORD_NOT_SUPPORT_HASH = 3;
    public static final int PASSWORD_NOT_SUPPORT_CUSTOM = 4;

    /**
     * Get the encoded password.
     */
    public static String encodePassword(String password, WebSphereRuntime wsRuntime) {
        if (password == null || password.length() == 0 || validatePassword(password, null, wsRuntime) != PASSWORD_PLAIN_TEXT)
            return password;

        try {
            return PasswordUtil.encode(password);
        } catch (InvalidPasswordEncodingException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not encode password", e);
        } catch (UnsupportedCryptoAlgorithmException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not encode password", e);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Caught unexpected exception while encoding password", e);
        }
        return null;
    }

    /**
     * Validates the password against the runtime
     *
     * @param password     a password
     * @param passwordType the type of password. Either a <code>Constants.PASSWORD_TYPE</code> or a <code>Constants.PASSWORD_HASH_TYPE</code>
     * @param wsRuntime    a WebSphere runtime
     *
     * @return <code>PASSWORD_OK</code> if the password is encoded or encrypted;
     *         <code>PASSWORD_PLAIN_TEXT</code> if the password is not encoded or encrypted;
     *         <code>PASSWORD_NOT_SUPPORT_AES</code> if the password is AES encrypted and the runtime doesn't support the encryption;
     *         <code>PASSWORD_NOT_SUPPORT_HASH</code> if the password is hash encoded and the runtime doesn't support the encoding.
     *
     */
    public static int validatePassword(String password, String passwordType, WebSphereRuntime wsRuntime) {
        if (password == null)
            return PASSWORD_PLAIN_TEXT;

        String data = password;
        if (data.length() < 2 || data.charAt(0) != '{')
            return PASSWORD_PLAIN_TEXT;

        int end = data.indexOf('}', 1);
        if (end < 1)
            return PASSWORD_PLAIN_TEXT;

        String algorithm = password.substring(1, end);
        if ("xor".equals(algorithm))
            return PASSWORD_OK;

        boolean isVersion850 = wsRuntime != null && wsRuntime.getRuntimeVersion().startsWith("8.5.0");

        if ("aes".equals(algorithm)) {
            if (isVersion850)
                return PASSWORD_NOT_SUPPORT_AES;
            return PASSWORD_OK;
        }

        if ("hash".equals(algorithm)) {
            if (passwordType != null && !Constants.PASSWORD_HASH_TYPE.equals(passwordType))
                return PASSWORD_NOT_SUPPORT_HASH;
            return PASSWORD_OK;
        }
        if (wsRuntime != null && !algorithm.isEmpty()) {
            List<String> supportedCustomEncryption = wsRuntime.getSupportedCustomEncryption();
            if (supportedCustomEncryption != null && supportedCustomEncryption.contains(algorithm))
                return PASSWORD_OK;
            //if algorithm contains custom do not show any warning message. According to 1Q16 design command-line utilities are not supported for custom encryption
            if (algorithm.contains("custom"))
                return PASSWORD_OK;
            return PASSWORD_NOT_SUPPORT_CUSTOM;
        }

        return PASSWORD_PLAIN_TEXT; //It has {xxxx} but it is unknown. Return as plain password.
    }

    /**
     * returns encryption algorithm used from the encrypted password string
     *
     * @param value
     * @return
     */
    public static String getEncryptionAlgorithm(String value) {
        if (value == null)
            return null;
        if (value.length() < 2 || value.charAt(0) != '{')
            return null;
        int end = value.indexOf('}', 1);
        if (end < 1)
            return null;
        String algorithm = value.substring(1, end);
        return algorithm;
    }

    /**
     * Check if a file can be found for the given URI that has
     * server configuration content type.
     */
    public static boolean isServerConfigFile(URI uri) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocationURI(uri);
        if (files.length > 0) {
            return isServerConfigFile(files[0]);
        }
        File file = new File(uri);
        if (file.exists()) {
            return isServerConfigFile(file);
        }
        return false;
    }

    /**
     * Check if the file has server configuration content type.
     */
    public static boolean isServerConfigFile(IFile file) {
        InputStream in = null;
        try {
            in = file.getContents();
            IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
            IContentType contentType = contentTypeManager.findContentTypeFor(in, file.getName());
            if (contentType == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "The content type is null for the following file: "
                                               + (file.getLocation() == null ? file.getFullPath().toOSString() : file.getLocation().toOSString()));
            } else if (CONTENT_TYPE_ID.equals(contentType.getId())) {
                return true;
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine content type from file: "
                                           + (file.getLocation() == null ? file.getFullPath().toOSString() : file.getLocation().toOSString()),
                            e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Check if the file has server configuration content type.
     */
    public static boolean isServerConfigFile(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
            IContentType contentType = contentTypeManager.findContentTypeFor(in, file.getName());
            if (contentType == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "The content type is null for the following file: " + file.getAbsolutePath());
            } else if (CONTENT_TYPE_ID.equals(contentType.getId())) {
                return true;
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine content type from file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Look up a server given a configuration resource.
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static WebSphereServerInfo getServer(IResource resource) {
        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            if (isContainedIn(resource, server.getServerFolder()))
                return server;

            if (isContainedIn(resource, server.getUserDirectory().getSharedConfigFolder()))
                return server;
        }
        return null;
    }

    /**
     * Returns true if the resource is contained in (a child or grand-child of) the given container (folder or project).
     */
    private static boolean isContainedIn(IResource resource, IContainer container) {
        if (container == null)
            return false;

        IContainer parent = resource.getParent();
        while (parent != null && parent instanceof IFolder) {
            if (container.equals(parent))
                return true;
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Look up a server given a configuration URI string.
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static WebSphereServerInfo getServer(String uriString) {
        try {
            URI uri = new URI(uriString);
            return getServer(uri);
        } catch (URISyntaxException e) {
            Trace.logError("Invalid URI: " + uriString, e);
        }
        return null;
    }

    /**
     * Look up a server given a configuration URI.
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static WebSphereServerInfo getServer(URI uri) {
        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null)
                return server;
        }

        WebSphereServerInfo wsInfo = getServerFromRuntimeProviders(uri, servers);

        if (wsInfo != null) {
            return wsInfo;
        }

        return null;
    }

    private static ConfigurationFile getConfigFromRuntimeProviders(URI uri, WebSphereServerInfo server) {
        final List<LibertyRuntimeProvider> list = LibertyRuntimeProviderExtension.getLibertyRuntimeProviders();
        for (LibertyRuntimeProvider provider : list) {
            URI targetConfigURI = provider.getTargetConfigFileLocation(uri, server);
            if (targetConfigURI != null) { // Get the first one from the first extension
                ConfigurationFile configFile = server.getConfigurationFileFromURI(targetConfigURI);
                if (configFile != null)
                    return configFile;
            }
        }
        return null;
    }

    /*
     * For any server.xml, return the WSInfo for the containing project
     */
    private static WebSphereServerInfo getServerFromRuntimeProviders(URI uri, WebSphereServerInfo[] servers) {
        final List<LibertyRuntimeProvider> list = LibertyRuntimeProviderExtension.getLibertyRuntimeProviders();
        for (LibertyRuntimeProvider provider : list) {
            for (WebSphereServerInfo server : servers) {
                URI alternateUri = provider.getTargetConfigFileLocation(uri, server);
                // We found the right server so return the first one
                if (alternateUri != null) {
                    return server;
                }
            }
        }
        // Allow liberty runtime providers to provide their WSInfo for this particular uri, when the server has NOT been created in the Servers view.
        for (LibertyRuntimeProvider provider : list) {
            WebSphereServerInfo temporaryWSInfo = provider.getWebSphereServerInfo(uri);
            if (temporaryWSInfo != null) {
                return temporaryWSInfo;
            }
        }

        return null;
    }

    /**
     * Get a ConfigurationFile given a resource
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static ConfigurationFile getConfigFile(IResource resource) {
        URI uri = resource.getLocation().toFile().toURI();
        return getConfigFile(uri);
    }

    /**
     * Get a ConfigurationFile given a uri
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static ConfigurationFile getConfigFile(String uriString) {
        try {
            URI uri = new URI(uriString);
            return getConfigFile(uri);
        } catch (URISyntaxException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Failed to locate configuration file because of invalid uri: " + uriString, e);
        }
        return null;
    }

    /**
     * Get a ConfigurationFile from the URI
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static ConfigurationFile getConfigFile(URI uri) {
        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null)
                return configFile;
        }
        return null;
    }

    /**
     * For any server.xml designated by uri, return the ConfigurationFile instance provided by
     * the Custom Liberty Runtime Provider extension
     *
     * @param uri
     * @return
     */
    public static ConfigurationFile getMappedConfigFile(URI uri) {
        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = getConfigFromRuntimeProviders(uri, server);
            if (configFile != null) {
                // Then we have a mapped config file
                return configFile;
            }
        }
        return null;
    }

    /**
     * For any server.xml designated by uri, return the ConfigurationFile instance provided by
     * the Custom Liberty Runtime Provider extension
     *
     * @param uri
     * @return
     */
    public static IFile getMappedConfigIFile(URI uri) {
        IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(uri);
        if (containers != null && containers.length == 1) { // Should only be the one
            IContainer container = containers[0];
            IProject project = container.getProject();
            IResource file = project.findMember(container.getProjectRelativePath());
            if (file instanceof IFile && file.exists()) {
                return (IFile) file;
            }
        }
        return null;
    }

    /**
     * Lookup the server for a configuration URI.
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static WebSphereServerInfo getServerInfo(URI docURI) {
        if (docURI == null)
            return null;

        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(docURI);
            if (configFile != null) {
                switch (configFile.getLocationType()) {
                    case SERVER:
                        return server;
                    default:
                        return null;
                }
            }
        }

        // Allow ghost runtime providers to provide the server (could be a ghost runtime)
        WebSphereServerInfo wsInfo = getServerFromRuntimeProviders(docURI, servers);
        if (wsInfo != null) {
            return wsInfo;
        }

        // The document may be within a server directory but not included
        // anywhere yet.  Check for this as well.
        for (WebSphereServerInfo server : servers) {
            URI serverURI = server.getServerURI();
            if (!URIUtil.canonicalRelativize(serverURI, docURI).isAbsolute()) {
                return server;
            }
        }

        return null;
    }

    public static IFolder getMappedConfigFolder(IResource resource) {
        List<LibertyRuntimeProvider> list = LibertyRuntimeProviderExtension.getLibertyRuntimeProviders();
        for (LibertyRuntimeProvider provider : list) {
            // This will allow any configuration file to be validated against the ghost runtime.
            IFolder configFolder = provider.getConfigFolder(resource);
            if (configFolder != null) { // Get the first one from the first extension
                return configFolder;
            }
        }
        return null;
    }

    /**
     * Get the UserDirectory for the given configuration file URI.
     *
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     *
     * @param docURI The configuration file URI.
     * @return The UserDirectory or null if not found.
     */
    public static UserDirectory getUserDirectory(URI docURI) {
        if (docURI == null)
            return null;

        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(docURI);
            if (configFile != null) {
                switch (configFile.getLocationType()) {
                    case SERVER:
                        return server.getUserDirectory();
                    case SHARED:
                        return server.getUserDirectory();
                    default:
                        // May still be in the user directory, just not in the shared
                        // sub-directory
                        break;
                }
            }
        }

        // The document may be within a user directory but not included
        // anywhere yet.  Check for this as well.
        WebSphereRuntime[] runtimes = WebSphereUtil.getWebSphereRuntimes();
        for (WebSphereRuntime runtime : runtimes) {
            for (UserDirectory userDir : runtime.getUserDirectories()) {
                URI userDirURI = userDir.getPath().toFile().toURI();
                if (!URIUtil.canonicalRelativize(userDirURI, docURI).isAbsolute()) {
                    return userDir;
                }
            }
        }

        // At this point, it is likely that there is no runtime and no server created for this particular
        // configuration file. (Likely a stand-alone file, or a file in a specialized build project)
        // Allow custom liberty runtime providers to contribute their own user directory
        WebSphereServerInfo wsInfo = getServerFromRuntimeProviders(docURI, servers);
        if (wsInfo != null) {
            return wsInfo.getUserDirectory();
        }

        return null;
    }

    public static void getVariables(ConfigurationFile cf, ConfigVars vars) {
        getVariables(cf, cf.getDomDocument(), cf.getURI(), cf.getWebSphereServer(), cf.getUserDirectory(), vars);
    }

    public static void getVariables(ConfigurationFile configFile, Document document, URI uri, WebSphereServerInfo serverInfo, UserDirectory userDir, ConfigVars vars) {
        if (document == null) {
            return;
        }

        final Stack<URI> includeFilter = new Stack<URI>();
        final VarsContext varsContext = new VarsContext();

        try {
            includeFilter.push(uri);
            vars.startContext();
            getVariables(configFile, document, uri, serverInfo, userDir, vars, includeFilter, varsContext);
            vars.endContext();
        } finally {
            includeFilter.clear();
            varsContext.clear();
        }
    }

    /**
     * Get local variables for the given configuration element (all of its attributes
     * can be used as variables including attributes with default values).
     *
     * @param elem        The element for which to get the local variables
     * @param attrExclude The attribute to exclude. If null, all attributes are included.
     * @param uri         The document URI.
     * @param vars        The ConfigVars object to add the variables to.
     */
    public static void getLocalVariables(Element elem, String attrExclude, URI uri, ConfigVars vars) {
        CMElementDeclaration elemDecl = SchemaUtil.getElement(elem.getOwnerDocument(), getElementTags(elem), uri);
        if (elemDecl == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not find declaration for element: " + elem.getLocalName() + ", from URI: " + uri);
            return;
        }
        CMNamedNodeMap attrDecls = elemDecl.getAttributes();
        NamedNodeMap attrs = elem.getAttributes();

        vars.startContext();
        // Get all the existing attributes on the element
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            String name = attr.getName();
            if (!name.equals(attrExclude)) {
                String value = attr.getValue();
                ConfigVars.Type type = null;
                CMAttributeDeclaration attrDecl = (CMAttributeDeclaration) attrDecls.getNamedItem(name);
                if (attrDecl != null) {
                    type = vars.getType(getTypeName(attrDecl));
                }
                DocumentLocation location = DocumentLocation.createDocumentLocation(uri, attr);
                vars.add(name, value, type, location);
            }
        }

        // Look for any attributes that are not on the element but have a default value
        for (int i = 0; i < attrDecls.getLength(); i++) {
            CMAttributeDeclaration attrDecl = (CMAttributeDeclaration) attrDecls.item(i);
            String name = attrDecl.getAttrName();
            if (attrs.getNamedItem(name) == null && !name.equals(attrExclude)) {
                String value = attrDecl.getDefaultValue();
                if (value != null && !value.isEmpty()) {
                    String typeName = vars.getTypeName(getTypeName(attrDecl));
                    vars.add(name, value, vars.getType(typeName), null);
                }
            }
        }
        vars.endContext();
    }

    private static String[] getElementTags(Element elem) {
        ArrayList<String> tags = new ArrayList<String>();
        for (Node current = elem; current != null && current.getNodeType() == Node.ELEMENT_NODE; current = current.getParentNode()) {
            tags.add(0, current.getNodeName());
        }
        return tags.toArray(new String[tags.size()]);
    }

    private static void getVariables(ConfigurationFile configFile, Document document, URI uri, WebSphereServerInfo serverInfo, UserDirectory userDir, ConfigVars vars,
                                     Stack<URI> includeFilter, VarsContext varsContext) {
        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getDefaultDropins()) {
                processDropinForVars(dropin, serverInfo, userDir, vars, includeFilter, varsContext);
            }
        }

        Element elem = document.getDocumentElement();
        if (elem != null) {
            for (Element child = DOMUtils.getFirstChildElement(elem); child != null; child = DOMUtils.getNextElement(child)) {
                final String name = child.getNodeName();
                if (name.equals(Constants.INCLUDE_ELEMENT)) {
                    processIncludeForVars(child, uri, serverInfo, userDir, vars, includeFilter, varsContext);
                } else if (name.equals(Constants.VARIABLE_ELEMENT)) {
                    processVariableElement(child, uri, vars, varsContext);
                }
            }
        }

        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getOverrideDropins()) {
                processDropinForVars(dropin, serverInfo, userDir, vars, includeFilter, varsContext);
            }
        }
    }

    private static void processIncludeForVars(Element elem,
                                              URI uri,
                                              WebSphereServerInfo serverInfo,
                                              UserDirectory userDir,
                                              ConfigVars vars,
                                              Stack<URI> includeFilter,
                                              VarsContext varsContext) {
        final String location = elem.getAttribute(Constants.LOCATION_ATTRIBUTE);
        if (location == null) {
            return;
        }

        // We must allow Custom Runtime Providers to override this location first
        final IResource project = userDir.getProject();
        String mappedLocation = null;
        if (project != null) {
            final IFolder mappedConfigFolder = ConfigUtils.getMappedConfigFolder(project);
            if (mappedConfigFolder != null) {
                // we will simply append the path value (the value of the include element) to this config folder path
                final IResource includeFile = mappedConfigFolder.findMember(location);
                if (includeFile != null && includeFile.exists()) {
                    mappedLocation = includeFile.getLocation().toString();
                }
            }
        }

        final URI includeURI = resolve(uri, mappedLocation != null ? mappedLocation : location, serverInfo, userDir);
        // We process the include configuration, if the URI exists and we have not seen it before
        if (includeURI != null && !includeFilter.contains(includeURI) && new File(includeURI).exists()) {
            final Document document = getDOM(includeURI, userDir.getWebSphereRuntime());
            if (document != null) {
                // Get the onConflict setting for the include and save it in the varsContext
                String attrValue = DOMUtils.getAttributeValue(elem, Constants.ONCONFLICT_ATTRIBUTE);
                if (attrValue != null) {
                    IncludeConflictResolution conflictResolution = IncludeConflictResolution.getConflictResolution(attrValue);
                    varsContext.pushIgnore(conflictResolution == IncludeConflictResolution.IGNORE);
                } else {
                    // Use the parent setting if the attribute is not specified
                    varsContext.pushIgnore();
                }
                includeFilter.push(includeURI);
                ConfigurationFile configFile = null;
                if (serverInfo != null) {
                    configFile = serverInfo.getConfigurationFileFromURI(includeURI);
                }
                getVariables(configFile, document, includeURI, serverInfo, userDir, vars, includeFilter, varsContext);
                includeFilter.pop();
                varsContext.popIgnore();
            }
        }
    }

    private static void processDropinForVars(ConfigurationFile dropin, WebSphereServerInfo serverInfo, UserDirectory userDir, ConfigVars vars, Stack<URI> includeFilter,
                                             VarsContext varsContext) {
        includeFilter.push(dropin.getURI());
        getVariables(dropin, dropin.getDomDocument(), dropin.getURI(), serverInfo, userDir, vars, includeFilter, varsContext);
        includeFilter.pop();
    }

    private static class VarsContext {

        // Keep track of the stack of on conflict settings for each include - only ignore counts
        // for variables since replace and merge will behave the same way
        private final List<Boolean> ignoreList = new ArrayList<Boolean>();

        // Keep track of what variables have already been declared
        private final Map<String, Boolean> declaredVars = new HashMap<String, Boolean>();

        protected VarsContext() {
            // empty constructor
        }

        // Push the ignore value for the current include
        protected void pushIgnore(boolean value) {
            ignoreList.add(value ? Boolean.TRUE : Boolean.FALSE);
        }

        // If the current include does not specify onConflict then it inherits the parent setting.
        // If there is no parent setting then the default is false.
        protected void pushIgnore() {
            if (!ignoreList.isEmpty()) {
                Boolean value = ignoreList.get(ignoreList.size() - 1);
                ignoreList.add(value);
            } else {
                ignoreList.add(Boolean.FALSE);
            }
        }

        // Remove the last setting when finished processing the current include
        protected void popIgnore() {
            if (!ignoreList.isEmpty()) {
                ignoreList.remove(ignoreList.size() - 1);
            }
        }

        protected boolean isIgnore() {
            if (!ignoreList.isEmpty() && ignoreList.get(ignoreList.size() - 1).booleanValue()) {
                return true;
            }
            return false;
        }

        protected void addDeclared(String name, boolean isDefault) {
            declaredVars.put(name, isDefault ? Boolean.TRUE : Boolean.FALSE);
        }

        protected boolean isDeclared(String name) {
            return declaredVars.containsKey(name);
        }

        // Always check if the variable is declared first before calling this.
        // If it is not declared then this is meaningless.
        protected boolean isDefault(String name) {
            Boolean isDefault = declaredVars.get(name);
            if (isDefault != null) {
                return isDefault.booleanValue();
            }
            return false;
        }

        protected void clear() {
            ignoreList.clear();
            declaredVars.clear();
        }
    }

    private static void processVariableElement(Element elem, URI uri, ConfigVars vars, VarsContext varsContext) {
        final String varName = elem.getAttribute("name");
        String varValue = null;
        String defaultValue = null;
        if (elem.hasAttribute("value")) {
            varValue = elem.getAttribute("value");
        } else if (elem.hasAttribute("defaultValue")) {
            defaultValue = elem.getAttribute("defaultValue");
        }
        if (varName != null) {
            // Handle the value and defaultValue cases separately
            if (varValue != null) {
                // If the variable has not been declared yet then add it, or if the current include is not
                // ignored then replace the previous variable declaration with this one
                if (!varsContext.isDeclared(varName) || !varsContext.isIgnore()) {
                    varsContext.addDeclared(varName, false);
                    vars.add(varName, varValue, DocumentLocation.createDocumentLocation(uri, elem));
                }
            } else if (defaultValue != null) {
                // If the variable has not been declared yet then add it, or if only a default value has been
                // declared and the current include is not ignored then replace the previous default
                // with this one.  For default values, check if the variable is declared in ConfigVars
                // instead of the VarsContext since a value could have been specified in bootstrap.properties
                // and this overrides a default value.
                if (!vars.isDefined(varName) || (varsContext.isDefault(varName) && !varsContext.isIgnore())) {
                    varsContext.addDeclared(varName, true);
                    vars.add(varName, defaultValue, DocumentLocation.createDocumentLocation(uri, elem));
                }
            }
        }
    }

    public static Document getDOM(URI uri) {
        return getDOM(uri, null);
    }

    public static Document getDOM(URI uri, WebSphereRuntime runtime) {
        WebSphereServerInfo[] servers = null;
        if (runtime != null) {
            List<WebSphereServerInfo> serverList = runtime.getWebSphereServerInfos();
            servers = serverList.toArray(new WebSphereServerInfo[serverList.size()]);
        } else {
            servers = WebSphereUtil.getWebSphereServerInfos();
        }

        for (WebSphereServerInfo server : servers) {
            final ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null)
                return configFile.getDomDocument();
        }

        return getDOMFromModel(uri);

    }

    protected static Document getDOMFromModel(URI uri) {
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        final IFile file = workspaceRoot.getFileForLocation(new Path(uri.getPath()));

        IDOMModel dom = null;
        InputStream in = null;
        final long time = System.currentTimeMillis();
        try {
            final IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model = null;

            try {
                if (file != null && file.exists()) {
                    model = manager.getExistingModelForRead(file);
                    if (model == null)
                        model = manager.getModelForRead(file);
                }

                if (model == null) {
                    in = new BufferedInputStream(new FileInputStream(new File(uri)));
                    model = manager.getModelForRead(new File(uri).getAbsolutePath(), in, null);
                }

                if ((model == null || !(model instanceof IDOMModel))) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Unable to create DOM Model for uri: " + uri);
                    return null;
                }
            } catch (FileNotFoundException e) {
                // caused when the file does not exist
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Invalid file: " + uri, e);
                return null;
            } catch (IllegalArgumentException e) {
                // caused when the URI is not valid
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Invalid uri: " + uri, e);
                return null;
            } catch (Exception e) {
                Trace.logError("Failed to load IDOMModel from uri: " + uri, e);
                return null;
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore
                    }
            }

            dom = (IDOMModel) model;
            return dom.getDocument();
        } catch (Exception e) {
            Trace.logError("Failed to get document from IDOMModel for uri: " + uri, e);
            return null;
        } finally {
            if (dom != null)
                dom.releaseFromRead();

            if (Trace.ENABLED)
                Trace.tracePerf("Configuration file IDOM load", time);
        }
    }

    public static Document getDOMFromFile(File file) {

        IDOMModel dom = null;
        InputStream in = null;
        final long time = System.currentTimeMillis();
        try {
            final IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model = null;

            try {
                if (file != null && file.exists()) {
                    in = new FileInputStream(file);
                    model = manager.getModelForRead(file.getAbsolutePath(), in, null);
                }

                if ((model == null || !(model instanceof IDOMModel))) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Unable to create DOM Model for uri: " + file);
                    return null;
                }
            } catch (FileNotFoundException e) {
                // caused when the file does not exist
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Invalid file: " + file, e);
                return null;
            } catch (IllegalArgumentException e) {
                // caused when the URI is not valid
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Invalid uri: " + file, e);
                return null;
            } catch (Exception e) {
                Trace.logError("Failed to load IDOMModel from uri: " + file, e);
                return null;
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore
                    }
            }

            dom = (IDOMModel) model;
            return dom.getDocument();
        } catch (Exception e) {
            Trace.logError("Failed to get document from IDOMModel for uri: " + file, e);
            return null;
        } finally {
            if (dom != null)
                dom.releaseFromRead();

            if (Trace.ENABLED)
                Trace.tracePerf("Configuration file IDOM load", time);
        }
    }

    /**
     * Get the associated file in the workspace given a URI.
     */
    public static IFile getWorkspaceFile(UserDirectory userDir, URI uri) {
        IFile file = getWorkspaceFile(userDir.getSharedConfigFolder(), uri);
        return file;
    }

    private static IFile getWorkspaceFile(IFolder folder, URI uri) {
        if (folder == null)
            return null;
        URI workspaceURI = folder.getLocation().toFile().toURI();
        URI relativeURI = URIUtil.canonicalRelativize(workspaceURI, uri);
        if (relativeURI.isAbsolute()) {
            return null;
        }
        IPath relativePath = new Path(relativeURI.getPath());
        IFile file = folder.getFile(relativePath);
        return file;
    }

    public static URI resolve(URI baseUri, String include, WebSphereServerInfo serverInfo, UserDirectory userDir) {
        if (include == null) {
            return null;
        }

        if (ConfigVarsUtils.containsReference(include)) {
            if (serverInfo != null) {
                return serverInfo.resolve(baseUri, include);
            }
        }

        return (userDir == null) ? null : userDir.resolve(baseUri, include);
    }

    /**
     * Do not call this method from a thread that has a lock on a runtime as the call
     * to getServer will try to get locks on the other runtimes and this can lead
     * to deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static URI resolve(URI baseUri, String include, UserDirectory userDir) {
        if (include == null) {
            return null;
        }

        return resolve(baseUri, include, getServer(baseUri), userDir);
    }

    /**
     * Get all the shared library ids for the given configuration file, including
     * the drop-in shared library ids.
     */
    public static String[] getSharedLibraryIds(ConfigurationFile configFile) {
        List<String> ids = new ArrayList<String>();
        String[] configIds = configFile.getSharedLibraryIds();
        for (String id : configIds) {
            ids.add(id);
        }
        String[] dropinIds = getDropInLibIds(configFile.getWebSphereServer(), configFile.getUserDirectory());
        for (String id : dropinIds) {
            ids.add(id);
        }
        return ids.toArray(new String[ids.size()]);
    }

    /**
     * Get the drop-in shared library ids.
     *
     * @param serverInfo The <code>WebSphereServerInfo</code> object or null if there isn't one
     * @parem userDir The <code>UserDirectory</code> object or null if there isn't one
     */
    public static String[] getDropInLibIds(WebSphereServerInfo serverInfo, UserDirectory userDir) {
        Map<String, URILocation> ids = new HashMap<String, URILocation>();
        addDropInLibIds(ids, serverInfo, userDir);
        return ids.keySet().toArray(new String[ids.size()]);
    }

    /**
     * Add the drop-in lib ids and locations to the map.
     *
     * @param ids        <code>Map</code> of ids to location
     * @param serverInfo The <code>WebSphereServerInfo</code> object or null if there isn't one
     * @param userDir    The <code>UserDirectory</code> object or null if there isn't one
     */
    public static void addDropInLibIds(Map<String, URILocation> ids, WebSphereServerInfo serverInfo, UserDirectory userDir) {
        List<IPath> paths = new ArrayList<IPath>();
        if (serverInfo != null) {
            serverInfo.addDropInLibPaths(paths);
        } else if (userDir != null) {
            userDir.addDropInLibPaths(paths);
        }
        for (IPath path : paths) {
            addDropInLibIds(ids, path);
        }
    }

    private static void addDropInLibIds(Map<String, URILocation> ids, IPath path) {
        File file = path.toFile();
        if (file.exists()) {
            File[] dirs = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    // Added "global" check for 108883 (disable automatic shared library support).
                    // Remove "global" check when runtime re-enables support for automatic shared libraries (108982).
                    return file.isDirectory() && "global".equals(file.getName());
                }
            });
            for (File dir : dirs) {
                ids.put(dir.getName(), new URILocation(dir.toURI()));
            }
        }
    }

    /**
     * Add the element described by the schema element declaration to the parent.
     *
     * This only works for model elements.
     */
    public static Element addElement(Element parentElement, CMElementDeclaration cmElementDeclaration) {
        return addElement(parentElement, cmElementDeclaration, null);
    }

    /**
     * Add the element described by the schema element declaration to the parent.
     * If the content is not null, creates a text child for the new element with the
     * content.
     *
     * This only works for model elements.
     */
    @SuppressWarnings("rawtypes")
    public static Element addElement(Element parentElement, CMElementDeclaration cmElementDeclaration, String content) {
        if (parentElement == null || cmElementDeclaration == null) {
            if (Trace.ENABLED)
                Trace.logError("The addElement method was called with null parent or null element declaration.", null);
            return null;
        }
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
        boolean append = false;
        Element element = null;

        Node refChild = parentElement.getLastChild();
        if (modelQuery.canInsert(parentElement, cmElementDeclaration, parentElement.getChildNodes().getLength(), ModelQuery.VALIDITY_STRICT)) {
            append = true;
        } else {
            for (int i = parentElement.getChildNodes().getLength(); !modelQuery.canInsert(parentElement, cmElementDeclaration, i, ModelQuery.VALIDITY_STRICT) && i > 0; i--) {
                refChild = refChild.getPreviousSibling();
            }
            if (refChild == null) {
                refChild = parentElement.getFirstChild();
            }
        }
        Document document = parentElement.getNodeType() == Node.DOCUMENT_NODE ? (Document) parentElement : parentElement.getOwnerDocument();
        DOMContentBuilder domContentBuilder = new DOMContentBuilderImpl(document);

        boolean parentNodeFormatRequired = false;
        if (document.getDocumentElement() != parentElement) {
            NodeList childNodes = parentElement.getChildNodes();
            parentNodeFormatRequired = childNodes == null
                                       || childNodes.getLength() == 0
                                       || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE
                                           && ((childNodes.item(0)).getNodeValue().trim().length() == 0));
        }

        IDOMModel model = null;
        if (parentElement instanceof IDOMNode) {
            model = ((IDOMNode) parentElement).getModel();
            model.aboutToChangeModel();
        }

        try {
            domContentBuilder.setBuildPolicy(0);
            domContentBuilder.build(parentElement, cmElementDeclaration);
            List list = domContentBuilder.getResult();
            boolean newLineRequired = false;
            for (Iterator i = list.iterator(); i.hasNext();) {
                Node newNode = (Node) i.next();
                if (newNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                    parentElement.setAttributeNode((Attr) newNode);
                } else {
                    if (append) {
                        parentElement.appendChild(newNode);
                        newLineRequired = true;
                    } else {
                        parentElement.insertBefore(newNode, refChild);
                    }
                }
            }
            if (list.size() > 0) {
                Object object = list.get(0);
                if (object instanceof Element) {
                    element = (Element) object;
                }
            }

            if (element != null) {
                if (newLineRequired) {
                    Text textNode = parentElement.getOwnerDocument().createTextNode(System.getProperty("line.separator"));
                    parentElement.appendChild(textNode);
                }
                if (content != null) {
                    Text textNode = element.getOwnerDocument().createTextNode(content);
                    element.appendChild(textNode);
                }
                if (parentNodeFormatRequired) {
                    formatXMLNode(parentElement);
                } else {
                    formatXMLNode(element);
                }
            }
        } finally {
            if (model != null)
                model.changedModel();
        }

        return element;
    }

    /**
     * Adds appropriate spacing for a node.
     */
    public static void formatXMLNode(Node node) {
        DocumentRewriteSession rewriteSession = null;
        if (node instanceof IDOMNode) {
            rewriteSession = ((IDocumentExtension4) ((IDOMNode) node).getModel().getStructuredDocument()).startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
        }
        IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
        try {
            formatProcessor.formatNode(node);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Format failed for node: " + node.getNodeName(), e);
        } finally {
            if (node instanceof IDOMNode && rewriteSession != null) {
                ((IDocumentExtension4) ((IDOMNode) node).getModel().getStructuredDocument()).stopRewriteSession(rewriteSession);
            }
        }
    }

    /**
     * Checks if the given element is already enabled and if not returns
     * a list of features that can be used to enable it.
     *
     * @param elemName        The element name
     * @param enabledFeatures The currently enabled features
     * @param wsRuntime       The runtime
     * @return The features that can be used to enable the element or null
     *         if already enabled
     */
    public static List<String> getFeaturesToEnable(String elemName, List<String> enabledFeatures, WebSphereRuntime wsRuntime) {
        if (wsRuntime == null)
            return null;

        // Before 8.5.5 the feature list does not specify the configuration
        // elements so return null
        String runtimeVersion = wsRuntime.getRuntimeVersion();
        if (runtimeVersion == null || runtimeVersion.startsWith("8.5.0"))
            return null;

        // go through all features to find features that support this config element
        List<String> allFeatures = FeatureList.getFeatures(false, wsRuntime);
        List<String> containingFeatures = new ArrayList<String>();
        for (String feature : allFeatures) {
            Set<String> configElements = FeatureList.getFeatureConfigElements(feature, wsRuntime);
            if (configElements.contains(elemName)) {
                for (String f : enabledFeatures) {
                    if (f.equalsIgnoreCase(feature)) //found in existing feature, return now
                        return null;
                }

                containingFeatures.add(feature);
            }
        }

        // TODO: temporary workaround. If the config element isn't in any features, it is most likely because
        // it is from the kernel or an auto-feature we don't know about. Best to exclude these for now until
        // we get this data in the featureList.xml
        if (containingFeatures.isEmpty())
            return null;

        // check if the enabled features recursively enable the element
        for (String feature : containingFeatures) {
            Set<String> parents = FeatureList.getFeatureParents(feature, wsRuntime);
            for (String feature2 : enabledFeatures) {
                for (String f : parents) {
                    if (f.equalsIgnoreCase(feature2)) {
                        return null;
                    }
                }
            }
        }

        return containingFeatures;
    }

    private static final String VARIABLE_TYPE = "variableType";

    // If this is a union of variable type and another type then return
    // the other type as the base type (e.g. union of variable type and
    // int type).  Otherwise, return null.
    public static XSDSimpleTypeDefinition getBaseType(CMNode node) {
        if (node == null)
            return null;
        List<XSDSimpleTypeDefinition> memberTypes = SchemaUtil.getMemberTypesFromUnion(node);
        if (memberTypes == null || memberTypes.isEmpty())
            return null;
        boolean hasVariableType = false;
        XSDSimpleTypeDefinition type = null;
        for (XSDSimpleTypeDefinition memberType : memberTypes) {
            if (VARIABLE_TYPE.equals(memberType.getName()))
                hasVariableType = true;
            else
                type = memberType;
        }

        if (hasVariableType && memberTypes.size() == 2) {
            return type;
        }
        return null;
    }

    // Get the type name for the given type.
    public static String getBaseTypeName(XSDSimpleTypeDefinition baseType) {
        if (baseType != null) {
            XSDSimpleTypeDefinition std = baseType;
            String baseTypeName = std.getName();
            while ((baseTypeName == null || baseTypeName.length() == 0) && std.getBaseTypeDefinition() != null) {
                std = std.getBaseTypeDefinition();
                baseTypeName = std.getName();
            }
            return baseTypeName;
        }
        return null;
    }

    // Given a schema declaration, get the type name accounting for
    // anonymous unions
    public static String getTypeName(CMNode nodeDecl) {
        CMDataType dataType = null;
        if (nodeDecl instanceof CMAttributeDeclaration) {
            dataType = ((CMAttributeDeclaration) nodeDecl).getAttrType();
        } else if (nodeDecl instanceof CMElementDeclaration) {
            dataType = ((CMElementDeclaration) nodeDecl).getDataType();
        }

        String typeName = null;
        if (dataType != null) {
            XSDSimpleTypeDefinition baseType = ConfigUtils.getBaseType(nodeDecl);
            typeName = ConfigUtils.getBaseTypeName(baseType);
            if (typeName == null)
                typeName = dataType.getDataTypeName();
        }
        return typeName;
    }

    public static final String MERGED_FOLDER = "mergedFolder";

    public static IPath getMergedConfigLocation(IResource resource) {
        IPath location = Activator.getInstance().getStateLocation().append(MERGED_FOLDER);
        IPath resourcePath = resource.getFullPath();
        location = location.append(resourcePath);
        return location;
    }

    /**
     * Merge the mergeElem into the mainElem. If there is a conflict then
     * mergeElem overrides mainElem.
     *
     * NOTE: Elements are likely not from the same document so children
     * must be imported.
     */
    public static void mergeElement(Element mainElem, Element mergeElem, URI uri, boolean saveLocations) {
        NamedNodeMap attrs = mergeElem.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            mainElem.setAttribute(attr.getNodeName(), attr.getNodeValue());
        }

        for (Element child = DOMUtils.getFirstChildElement(mergeElem); child != null; child = DOMUtils.getNextElement(child)) {
            // Append child nodes at the end so they will override any earlier nodes
            Node node = mainElem.getOwnerDocument().importNode(child, true);
            mainElem.appendChild(node);
            if (saveLocations) {
                setDocumentLocation(node, child, uri);
            }
        }
    }

    /**
     * Get a resolved list of elements for the given element name. The idAttr is used
     * to decide how elements get merged. If idAttr is null it is assumed that the
     * element is a singleton (like featureManager).
     * Resolves using the config root, includes and config dropins.
     */
    public static List<Element> getResolvedElements(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory userDir, String elementName, String idAttr) {
        return getResolvedElements(doc, uri, serverInfo, userDir, elementName, idAttr, false);
    }

    /**
     * Get a resolved list of elements for the given element name. The idAttr is used
     * to decide how elements get merged. If idAttr is null it is assumed that the
     * element is a singleton (like featureManager). If saveLocations is true, the
     * locations will be saved in the user data of the elements.
     * Resolves using the config root, includes and config dropins.
     */
    public static List<Element> getResolvedElements(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory userDir, String elementName, String idAttr,
                                                    boolean saveLocations) {
        ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
        Document resultDoc = getResolvedElements(doc, uri, serverInfo, userDir, elementName, idAttr, includeFilter, saveLocations);
        List<Element> elems = new ArrayList<Element>();
        if (resultDoc != null) {
            Element resultRoot = resultDoc.getDocumentElement();
            for (Element elem = DOMUtils.getFirstChildElement(resultRoot, elementName); elem != null; elem = DOMUtils.getNextElement(elem, elementName)) {
                elems.add(elem);
            }
        }
        return elems;
    }

    public static final String DOCUMENT_LOCATION_KEY = "documentLocationKey";
    private static final UserDataHandler docLocationDataHandler = new DocLocationDataHandler();

    private static Document getResolvedElements(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory userDir, String elementName, String idAttr,
                                                IncludeFilter includeFilter,
                                                boolean saveLocations) {
        // If we have seen the configuration file before, simply return
        if (!includeFilter.accept(uri)) {
            return null;
        }

        Document mainDoc = DOMUtils.getTmpDoc();
        if (mainDoc == null)
            return null;
        Element mainRoot = mainDoc.createElement(Constants.SERVER_ELEMENT);
        mainDoc.appendChild(mainRoot);

        ConfigurationFile configFile = null;
        if (serverInfo != null && uri != null) {
            configFile = serverInfo.getConfigurationFileFromURI(uri);
        }

        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getDefaultDropins()) {
                Document dropinsDoc = getResolvedElements(dropin.getDomDocument(), dropin.getURI(), serverInfo, userDir, elementName, idAttr, includeFilter, saveLocations);
                // Always merge dropins
                if (dropinsDoc != null)
                    mergeDocs(mainDoc, dropinsDoc, elementName, idAttr, IncludeConflictResolution.MERGE, dropin.getURI(), saveLocations);
            }
        }

        for (Element elem = DOMUtils.getFirstChildElement(doc.getDocumentElement()); elem != null; elem = DOMUtils.getNextElement(elem)) {
            if (DOMUtils.isInclude(elem)) {
                String location = elem.getAttribute(Constants.LOCATION_ATTRIBUTE);
                if (location == null)
                    continue;
                URI includeURI = resolve(uri, location, serverInfo, userDir);
                if (includeURI == null || !(new File(includeURI)).exists())
                    continue;
                final Document includeDoc = getDOM(includeURI, userDir != null ? userDir.getWebSphereRuntime() : null);
                if (includeDoc == null)
                    continue;
                Document mergeDoc = getResolvedElements(includeDoc, includeURI, serverInfo, userDir, elementName, idAttr, includeFilter, saveLocations);
                if (mergeDoc != null) {
                    String onConflict = elem.getAttribute(Constants.ONCONFLICT_ATTRIBUTE);
                    mergeDocs(mainDoc, mergeDoc, elementName, idAttr, IncludeConflictResolution.getConflictResolution(onConflict), includeURI, saveLocations);
                }
            } else if (elem.getNodeName().equals(elementName)) {
                Element mainElem = findElement(mainRoot, elementName, idAttr, idAttr != null ? elem.getAttribute(idAttr) : null);
                if (mainElem == null) {
                    Node node = mainDoc.importNode(elem, true);
                    mainRoot.appendChild(node);
                    if (saveLocations) {
                        node.setUserData(DOCUMENT_LOCATION_KEY, DocumentLocation.createDocumentLocation(uri, elem), docLocationDataHandler);
                    }
                } else {
                    mergeElement(mainElem, elem, uri, saveLocations);
                }
            }
        }

        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getOverrideDropins()) {
                Document dropinsDoc = getResolvedElements(dropin.getDomDocument(), dropin.getURI(), serverInfo, userDir, elementName, idAttr, includeFilter, saveLocations);
                // Always merge dropins
                if (dropinsDoc != null)
                    mergeDocs(mainDoc, dropinsDoc, elementName, idAttr, IncludeConflictResolution.MERGE, dropin.getURI(), saveLocations);
            }
        }

        return mainDoc;
    }

    private static class DocLocationDataHandler implements UserDataHandler {

        public DocLocationDataHandler() {
            // Empty
        }

        /** {@inheritDoc} */
        @Override
        public void handle(short operation, String key, Object data, Node src, Node dst) {
            if (dst != null) {
                dst.setUserData(key, data, this);
            }
        }

    }

    private static void mergeDocs(Document mainDoc, Document mergeDoc, String elementName, String idAttr, IncludeConflictResolution conflictRes, URI uri, boolean saveLocations) {
        Element mainServer = mainDoc.getDocumentElement();
        Element mergeServer = mergeDoc.getDocumentElement();
        for (Element mergeElem = DOMUtils.getFirstChildElement(mergeServer, elementName); mergeElem != null; mergeElem = DOMUtils.getNextElement(mergeElem, elementName)) {
            Element mainElem = findElement(mainServer, elementName, idAttr, idAttr != null ? mergeElem.getAttribute(idAttr) : null);
            if (mainElem != null) {
                switch (conflictRes) {
                    case MERGE:
                        mergeElement(mainElem, mergeElem, uri, saveLocations);
                        break;
                    case REPLACE:
                        Node node = mainDoc.importNode(mergeElem, true);
                        mainServer.replaceChild(node, mainElem);
                        if (saveLocations) {
                            setDocumentLocation(node, mergeElem, uri);
                        }
                        break;
                    case IGNORE:
                        // Do nothing
                        break;
                }
            } else {
                Node node = mainDoc.importNode(mergeElem, true);
                mainServer.appendChild(node);

                if (saveLocations) {
                    setDocumentLocation(node, mergeElem, uri);
                }
            }
        }
    }

    // Copies location over from mergeElem if it exists,
    // otherwise, creates a new one using uri and mergeElem.
    public static void setDocumentLocation(Node node, Element mergeElem, URI uri) {
        DocumentLocation mergeLocation = (DocumentLocation) mergeElem.getUserData(DOCUMENT_LOCATION_KEY);
        if (mergeLocation == null)
            mergeLocation = DocumentLocation.createDocumentLocation(uri, mergeElem);

        node.setUserData(DOCUMENT_LOCATION_KEY, mergeLocation, docLocationDataHandler);
    }

    public static Element findElement(Element parent, String elementName, String idAttr, String id) {
        if (idAttr != null && (id == null || id.isEmpty())) {
            // Elements that have an id attribute defined in the schema but it is not set are
            // all treated as distinct so return null;
            return null;
        }
        for (Element elem = DOMUtils.getFirstChildElement(parent, elementName); elem != null; elem = DOMUtils.getNextElement(elem, elementName)) {
            if (idAttr == null) {
                // If there is no id attribute then this is a singleton so there should only
                // be one.
                return elem;
            }
            String idValue = elem.getAttribute(idAttr);
            if (id.equals(idValue))
                return elem;
        }
        return null;
    }

    public static Document documentLoad(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(file);
    }

    // Referenced from: com.ibm.ws.st.core.internal.config.ConfigurationFile.getHTTPPort(String, int)
    public static int getHTTPPort(Document doc, String portAttr) {
        List<Element> httpEndpoints = new ArrayList<Element>();
        if (doc != null) {
            Element root = doc.getDocumentElement();
            for (Element elem = getFirstChildElement(root, Constants.HTTP_ENDPOINT); elem != null; elem = getNextElement(elem, Constants.HTTP_ENDPOINT)) {
                httpEndpoints.add(elem);
            }
        }

        for (Element elem : httpEndpoints) {
            if (Constants.DEFAULT_HTTP_ENDPOINT.equals(elem.getAttribute(Constants.FACTORY_ID))) {
                String port = elem.getAttribute(portAttr);
                if (port != null && !port.isEmpty()) {
                    try {
                        return Integer.parseInt(port);
                    } catch (NumberFormatException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "HttpEndpoint had invalid HttpPort", e);
                    }
                }
            }
        }
        return -1;
    }

    public static File createBasicRegConfig(String user, String pw) {
        File tempFile = null;
        String encryptedPW = pw;
        try {
            encryptedPW = PasswordUtil.encode(pw);
        } catch (Exception e) {
            Trace.logError("Error encoding password", e);
        }
        try {
            tempFile = File.createTempFile("defaultBasicReg", "xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element serverElem = doc.createElement(Constants.SERVER_ELEMENT);
            doc.appendChild(serverElem);

            addPreElementText(doc, serverElem, true, true);
            Element basicReg = doc.createElement(Constants.BASIC_USER_REGISTY);
            basicReg.setAttribute(Constants.INSTANCE_ID, "basic");
            basicReg.setAttribute("realm", "BasicRealm");
            serverElem.appendChild(basicReg);
            addPostElementText(doc, serverElem);

            addPreElementText(doc, basicReg, false, false);
            Element usr = doc.createElement(Constants.VARIABLE_USER);
            usr.setAttribute(Constants.VARIABLE_NAME, user);
            usr.setAttribute(Constants.PASSWORD_ATTRIBUTE, encryptedPW);
            basicReg.appendChild(usr);
            addPostElementText(doc, basicReg);

            addPreElementText(doc, serverElem, true, true);
            Element admin = doc.createElement(Constants.ADMIN_ROLE);
            serverElem.appendChild(admin);
            addPostElementText(doc, serverElem);

            addPreElementText(doc, admin, false, false);
            Element adminUser = doc.createElement(Constants.VARIABLE_USER);
            adminUser.setTextContent(user);
            admin.appendChild(adminUser);
            addPostElementText(doc, admin);

            save(doc, tempFile);
        } catch (Exception e) {
            Trace.logError("Error creating the basicRegistry config file", e);
        }
        return tempFile;
    }

    public static int validateRemoteSecurity(Document doc, String user, String pw) {
        int result = 1;
        Element root = doc.getDocumentElement();
        Element basicReg = getFirstChildElement(root, Constants.BASIC_USER_REGISTY);
        for (Element usr = getFirstChildElement(basicReg); usr != null; usr = getNextElement(usr)) {

            String unFRXML = usr.getAttribute(Constants.VARIABLE_NAME);
            String pwFRXML = usr.getAttribute(Constants.PASSWORD_ATTRIBUTE);
            if (PasswordUtil.isEncrypted(pwFRXML)) {
                try {
                    pwFRXML = PasswordUtil.decode(pwFRXML);
                } catch (Exception e) {
                    Trace.logError("Error decoding password", e);
                }
            }

            if (unFRXML != null && unFRXML.equals(user)) {// found user with name
                if (pwFRXML != null && pwFRXML.equals(pw)) {// pw match
                    // Find out if user has admin role
                    if (isAdmin(doc, user)) {
                        result = 0; // Has user, pw matches, user has adminRole
                        break;
                    }
                    result = 8; // Has user, pw matches, user does not have adminRole
                    break;
                }
                result = 4; // Has user, pw mismatch/nopw
                break;
            }
            result = 2; // Does not have user
            // DO NOT BREAK, there might be more
        }
        return result;
    }

    public static void updateRemoteFileAccess(Document doc, File xml, String logdir, String restConnectorFeature) {
        if (logdir == null)
            return;
        Element root = doc.getDocumentElement();

        if (restConnectorFeature != null) {
            // change restConnector feature to the specified one (usually a newer version)
            Element featureManager = getFirstChildElement(root, Constants.FEATURE_MANAGER);
            Element feature = getFirstChildElement(featureManager);

            boolean found = false;
            while (feature != null && !found) {
                if (feature.getTextContent().toLowerCase().startsWith("restconnector-")) {
                    found = true;
                } else {
                    feature = getNextElement(feature);
                }
            }

            if (feature != null) {
                feature.setTextContent(restConnectorFeature);
            }
        }

        // add remoteFileAccess element
        Element remoteFileAccess = getFirstChildElement(root, Constants.REMOTE_FILE_ACCESS);
        if (remoteFileAccess == null) {
            addPreElementText(doc, root, true, true);
            Element elem = doc.createElement(Constants.REMOTE_FILE_ACCESS);
            root.appendChild(elem);
            addPostElementText(doc, root);
            remoteFileAccess = elem;
        }

        // check for existing entry
        for (Element fElem = getFirstChildElement(remoteFileAccess); fElem != null; fElem = getNextElement(fElem)) {
            String dir = fElem.getTextContent();
            if (dir != null && !dir.isEmpty() && dir.equalsIgnoreCase(logdir))
                return;
        }

        // if no entry is present add writeDir element
        Element elem = doc.createElement(Constants.WRITE_DIR);
        elem.setTextContent(logdir);
        remoteFileAccess.appendChild(elem);
        addPostElementText(doc, remoteFileAccess);
        save(doc, xml);
    }

    public static void updateRemoteSecurity(Document doc, File xml, String user, String pw, int code) {

        Element root = doc.getDocumentElement();
        Element basicReg = getFirstChildElement(root, Constants.BASIC_USER_REGISTY);
        if (basicReg == null) {
            addPreElementText(doc, root, true, true);
            Element elem = doc.createElement(Constants.BASIC_USER_REGISTY);
            root.appendChild(elem);
            addPostElementText(doc, root);
            basicReg = elem;
        }
        // code 1
        // no user entry
        // Add one with the user/pw pair
        if (code == 1) {
            String encryptedPW = pw;
            try {
                encryptedPW = PasswordUtil.encode(pw);
            } catch (Exception e) {
                Trace.logError("Error encoding password", e);
            }

            addPreElementText(doc, basicReg, false, false);
            Element elem = doc.createElement(Constants.VARIABLE_USER);
            elem.setAttribute(Constants.VARIABLE_NAME, user);
            elem.setAttribute(Constants.PASSWORD_ATTRIBUTE, encryptedPW);
            basicReg.appendChild(elem);
            addPostElementText(doc, basicReg);
            addAdminRole(doc, user);
        }
        // code 8
        // user match and pw match, but no administrator-role for
        if (code == 8) {
            addAdminRole(doc, user);
        }

        save(doc, xml);
    }

    private static void addAdminRole(Document doc, String user) {
        Element root = doc.getDocumentElement();
        Element admin = getFirstChildElement(root, Constants.ADMIN_ROLE);

        if (admin == null) {
            addPreElementText(doc, root, true, true);
            admin = doc.createElement(Constants.ADMIN_ROLE);
            root.appendChild(admin);
            addPostElementText(doc, root);
        }

        addPreElementText(doc, admin, false, false);
        Element usr = doc.createElement(Constants.VARIABLE_USER);
        usr.setTextContent(user);
        admin.appendChild(usr);
        addPostElementText(doc, admin);

    }

    public static boolean isAdmin(Document doc, String user) {
        Element root = doc.getDocumentElement();
        Element admin = getFirstChildElement(root, Constants.ADMIN_ROLE);
        for (Element usr = getFirstChildElement(admin, Constants.VARIABLE_USER); usr != null; usr = getNextElement(usr)) {
            if (usr.getTextContent().equals(user)) {
                return true;
            }
        }
        return false;
    }

    public static Element getFirstChildElement(Node start, String name) {
        if (start == null)
            return null;

        Node node = start.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start, String name) {
        Node node = start.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private static Element getFirstChildElement(Element element) {
        if (element == null)
            return null;
        Node node = element.getFirstChild();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private static Element getNextElement(Element element) {
        if (element == null)
            return null;
        Node node = element.getNextSibling();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static void save(Document doc, File xml) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(xml);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, streamResult);
        } catch (Exception e) {
            Trace.logError("Error saving the document", e);
        }
    }

    public static ArrayList<String> getIncludes(Document doc) {
        ArrayList<String> includeFiles = new ArrayList<String>();
        Element root = doc.getDocumentElement();
        for (Element e = getFirstChildElement(root, Constants.INCLUDE_ELEMENT); e != null; e = getNextElement(e, Constants.INCLUDE_ELEMENT)) {
            String fileLoc = e.getAttribute(Constants.LOCATION_ATTRIBUTE);
            if (fileLoc != null && fileLoc != "") {
                includeFiles.add(fileLoc);
            }
        }
        return includeFiles;
    }

    public static void addPreElementText(Document doc, Element parent, boolean isTopLevel, boolean hasChildren) {
        if (parent == null)
            return;

        StringBuilder builder = new StringBuilder();
        if (isTopLevel || !hasChildren) {
            builder.append("\n    ");
        }
        Node node = parent.getParentNode();
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            builder.append("    ");
            node = node.getParentNode();
        }
        final Text text = doc.createTextNode(builder.toString());
        parent.appendChild(text);
    }

    public static void addPostElementText(Document doc, Element parent) {
        if (parent == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        Node node = parent.getParentNode();
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            builder.append("    ");
            node = node.getParentNode();
        }
        Node text = doc.createTextNode(builder.toString());
        parent.appendChild(text);
    }

}

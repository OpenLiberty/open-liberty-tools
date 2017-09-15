/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.ServerExtensionWrapper;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.validation.CustomServerVariablesManager;

@SuppressWarnings("restriction")
public class ConfigurationFile implements IAdaptable, IConfigurationElement {
    public static final String USER_DATA_URI = "uri";
    public static final String USER_DATA_USER_DIRECTORY = "userDirectory";
    private static final String XML_EXTENSION = ".xml";

    public enum LOCATION_TYPE {
        SERVER, SHARED, URL, FILE_SYSTEM
    }

    private static final String COMMENT_TEXT1 = "Enablefeatures";
    private static final String COMMENT_TEXT2 = "<featureManager><feature>servlet-3.0</feature></featureManager>";

    public static class Application {
        private final String name;
        private final String type;
        private final String location;
        private final String autoStart;
        private final String[] sharedLibRefs;
        EnumSet<APIVisibility> apiVisibility;

        public Application(String name, String type, String location, String autoStart, String[] sharedLibRefs, EnumSet<APIVisibility> apiVisibility) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.autoStart = autoStart;
            this.sharedLibRefs = sharedLibRefs;
            this.apiVisibility = apiVisibility;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getLocation() {
            return location;
        }

        public String getAutoStart() {
            return autoStart;
        }

        public String[] getSharedLibRefs() {
            return sharedLibRefs;
        }

        public EnumSet<APIVisibility> getAPIVisibility() {
            return apiVisibility;
        }

        @Override
        public String toString() {
            return "Application[" + getName() + "]";
        }
    }

    protected URI uri;
    protected long lastModified = -1;
    protected Document document;
    protected Element serverElement;
    protected IDOMDocument domDocument;
    protected UserDirectory userDir;
    protected final WebSphereServerInfo server; // only set if the file is associated with a server
    protected IdentityHashMap<Element, ConfigurationFile> includes;
    protected List<String> unresolvedIncludes;
    protected List<ConfigurationFile> defaultDropins;
    protected List<ConfigurationFile> overrideDropins;
    protected ConfigVars configVars;
    protected final Object configLock;

    public ConfigurationFile(URI uri, UserDirectory userDir) throws IOException {
        this(uri, userDir, null);
    }

    public ConfigurationFile(URI uri, UserDirectory userDir, WebSphereServerInfo server) throws IOException {
        this.uri = uri;
        this.userDir = userDir;
        this.server = server;
        // Have to lock on the runtime if there is one since deadlocks
        // can occur otherwise.
        this.configLock = server == null ? this : server.getWebSphereRuntime();
        load();
    }

    public Document getDomDocument() {
        if (domDocument != null)
            return domDocument;

        domDocument = (IDOMDocument) ConfigUtils.getDOMFromModel(getURI());
        if (domDocument != null) {
            domDocument.setUserData(USER_DATA_URI, getURI().toString(), null);
            domDocument.setUserData(USER_DATA_USER_DIRECTORY, getUserDirectory(), null);
        }

        return domDocument;
    }

    private Map<String, Boolean> getAppLabelMap() {
        // RTC 154074
        //
        // Need to be called before synchronizing on confligLock, to
        // eliminate any potential deadlock. The potential deadlock is
        // caused by making a call to WebsphereRuntime which is synchronized
        // and the runtime call itself tries to access the WebsphereServerInfo.
        // We could end up with one thread holding a lock on the runtime object
        // and another thread holding a lock on WebsphereServerInfo and each
        // thread is waiting for the other.
        Map<String, Boolean> appLabelMap = new HashMap<String, Boolean>();
        URL schemaURL = server == null ? SchemaUtil.getSchemaURL(uri) : server.getConfigurationSchemaURL();
        Document doc = getDomDocument();
        for (String appLabel : ServerExtensionWrapper.getAllApplicationElements()) {
            boolean isAppLabelHasNameAttr = SchemaUtil.getAttribute(doc, new String[] { Constants.SERVER_ELEMENT, appLabel }, Constants.APP_NAME, schemaURL) != null;
            appLabelMap.put(appLabel, isAppLabelHasNameAttr ? Boolean.TRUE : Boolean.FALSE);
        }
        return appLabelMap;
    }

    private void load() throws IOException {
        InputStream in = null;
        long time = System.currentTimeMillis();
        try {
            File file = new File(uri);
            lastModified = file.lastModified();
            in = new BufferedInputStream(new FileInputStream(file));
            document = documentLoad(in);
            serverElement = document.getDocumentElement();
            if (serverElement == null)
                throw new IOException("Could not read config file");
        } catch (FileNotFoundException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Invalid file: " + uri, e);
            throw e;
        } catch (IllegalArgumentException e) {
            // caused when includeURI is not a valid file
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Invalid path: " + uri, e);
            throw new IOException("Could not read config file", e);
        } catch (IOException e) {
            Trace.logError("Could not load configuration file: " + uri, e);
            throw e;
        } catch (Exception e) {
            Trace.logError("Could not load configuration file: " + uri, e);
            throw new IOException(e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                // ignore
            }
            if (Trace.ENABLED)
                Trace.tracePerf("Configuration file load", time);
        }
    }

    public static Document documentLoad(InputStream in) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                // The source view will flag this as a warning
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Warning while reading configuration file.\nReason: " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                // The source view will flag this error, so we can ignore it.
                // Adding it as a warning to tracing
                Trace.logError("Error while reading configuration file.", e);
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                // The source view will flag this error, so we can ignore it.
                // Adding it as a warning to tracing
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error while reading configuration file.\nReason: " + e.getMessage());
            }
        });
        return parser.parse(new InputSource(in));
    }

    public LOCATION_TYPE getLocationType() {
        if (server != null && server.getServerURI() != null && !URIUtil.canonicalRelativize(server.getServerURI(), uri).isAbsolute())
            return LOCATION_TYPE.SERVER;
        if (userDir.getSharedConfigURI() != null && !URIUtil.canonicalRelativize(userDir.getSharedConfigURI(), uri).isAbsolute())
            return LOCATION_TYPE.SHARED;

        return LOCATION_TYPE.FILE_SYSTEM;
    }

    /**
     * Get the set of direct includes.
     */
    public ConfigurationFile[] getLocalIncludedFiles() {
        Collection<ConfigurationFile> includes = getIncludes().values();
        ConfigurationFile[] configFiles = new ConfigurationFile[includes.size()];
        int i = 0;
        for (ConfigurationFile include : includes) {
            configFiles[i++] = include;
        }
        return configFiles;
    }

    public boolean hasIncludes() {
        return !getIncludes().isEmpty();
    }

    /**
     * Get the full set of config files that this config file includes
     * (directly or through dropins), not including this config file itself.
     */
    public ConfigurationFile[] getAllIncludedFiles() {
        final ArrayList<ConfigurationFile> includedList = new ArrayList<ConfigurationFile>(5);

        // prevent the returned list from including the current configuration
        // file, e.g. A -> B -> A, we should just return { B }
        final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
        includeFilter.accept(getURI());

        // Add any default dropins
        for (ConfigurationFile dropin : getDefaultDropins()) {
            dropin.getAllConfigFiles(includedList, includeFilter);
        }

        // Add explicit includes
        final Collection<ConfigurationFile> includes = getIncludes().values();
        for (ConfigurationFile include : includes)
            include.getAllConfigFiles(includedList, includeFilter);

        // Add any override dropins
        for (ConfigurationFile dropin : getOverrideDropins()) {
            dropin.getAllConfigFiles(includedList, includeFilter);
        }

        return includedList.toArray(new ConfigurationFile[includedList.size()]);
    }

    /**
     * Get all of the configuration files.
     */
    public void getAllConfigFiles(List<ConfigurationFile> found,
                                  IncludeFilter includeFilter) {
        // if we have seen the configuration file before, simply return
        if (!includeFilter.accept(getURI())) {
            return;
        }

        found.add(this);

        // Add any default dropins
        for (ConfigurationFile dropin : getDefaultDropins()) {
            dropin.getAllConfigFiles(found, includeFilter);
        }

        // Add explicit includes
        Collection<ConfigurationFile> includes = getIncludes().values();
        for (ConfigurationFile include : includes)
            include.getAllConfigFiles(found, includeFilter);

        // Add any override dropins
        for (ConfigurationFile dropin : getOverrideDropins()) {
            dropin.getAllConfigFiles(found, includeFilter);
        }
    }

    public String[] getAllUnresolvedIncludes() {
        final ArrayList<String> includeList = new ArrayList<String>(5);
        final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();

        getAllUnresolvedIncludes(includeList, includeFilter);
        return includeList.toArray(new String[includeList.size()]);
    }

    public void getAllUnresolvedIncludes(List<String> list, IncludeFilter includeFilter) {
        synchronized (configLock) {
            // if we have seen the configuration file before, simply return
            if (!includeFilter.accept(getURI())) {
                return;
            }
            Collection<ConfigurationFile> includes = getIncludes().values();
            for (String include : unresolvedIncludes) {
                list.add(include);
            }

            for (ConfigurationFile config : includes) {
                config.getAllUnresolvedIncludes(list, includeFilter);
            }
        }
    }

    /**
     * Check for out of sync include files. Only checks this files includes.
     * Callers should loop through all of the files associated with a configuration
     * and call this method to find out if there are any changes in the full
     * configuration.
     */
    public boolean hasOutOfSyncLocalIncludes() {
        synchronized (configLock) {
            Collection<ConfigurationFile> includes = getIncludes().values();
            for (ConfigurationFile config : includes) {
                final File file = new File(config.getURI());
                // We have an included server configuration file that was
                // deleted, or was not in the workspace and it was modified
                if (!file.exists() || (config.getIFile() == null && file.lastModified() != config.lastModified)) {
                    return true;
                }
            }

            // Check for unresolved includes to see if any of them now exist
            for (String location : unresolvedIncludes) {
                URI includeURI = (server != null) ? server.resolve(getURI(), location) : getUserDirectory().resolve(getURI(), location);
                if (includeURI != null && new File(includeURI).exists()) {
                    return true;
                }
            }

            // Check if any config dropins have been added or removed
            if (hasOutOfSyncDropins())
                return true;

            return false;
        }
    }

    /**
     * Returns the user directory this config file was loaded from.
     *
     * @return
     */
    public UserDirectory getUserDirectory() {
        return userDir;
    }

    /**
     * Returns the server this config file is associated with. May be null.
     *
     * @return
     */
    public WebSphereServerInfo getWebSphereServer() {
        return server;
    }

    public Document getDocument() {
        return document;
    }

    public URI getURI() {
        return uri;
    }

    public IFile getIFile() {
        return getIFile(userDir.getProject());
    }

    private IFile getIFile(IContainer folder) {
        if (folder == null || !folder.exists())
            return null;
        URI workspaceURI = folder.getLocation().toFile().toURI();
        URI relativeURI = URIUtil.canonicalRelativize(workspaceURI, uri);
        if (relativeURI.isAbsolute())
            return null;
        IPath relativePath = new Path(relativeURI.getPath());
        return folder.getFile(relativePath);
    }

    @Override
    public IPath getPath() {
        return new Path(new File(uri).getAbsolutePath());
    }

    @Override
    public String getName() {
        IPath path = new Path(uri.getPath());
        return path.lastSegment();
    }

    public String getServerDescription() {
        Element elem = getServerElement();
        if (elem != null) {
            return elem.getAttribute("description");
        }
        return "";
    }

    public Element addElement(String name) {
        return addElement(getServerElement(), name);
    }

    public Element addElement(Element parent, String name) {
        if (parent == null)
            return null;

        synchronized (configLock) {
            final Document doc = getDocument();
            boolean isTopLevel = DOMUtils.isServerElement(parent);
            NodeList childNodes = parent.getChildNodes();
            boolean hasChildren = !(childNodes == null || childNodes.getLength() == 0
                                    || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE
                                        && ((childNodes.item(0)).getNodeValue().trim().length() == 0)));

            if (isTopLevel && name.equals(Constants.FEATURE_MANAGER)) {
                removeInitialComments();
            }

            addPreElementText(parent, isTopLevel, hasChildren);
            Element elem = doc.createElement(name);
            parent.appendChild(elem);
            addPostElementText(parent);

            if (isTopLevel && name.equals(Constants.INCLUDE_ELEMENT)) {
                resetIncludes();
            }

            return elem;
        }
    }

    private boolean hasSignificantChildren(Element elem) {
        NodeList childNodes = elem.getChildNodes();
        if (childNodes == null || childNodes.getLength() == 0)
            return false;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            int type = node.getNodeType();
            switch (type) {
                case Node.TEXT_NODE:
                    if (!(node.getNodeValue().trim().length() == 0))
                        return true;
                    break;
                case Node.ELEMENT_NODE:
                case Node.CDATA_SECTION_NODE:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    private void addPreElementText(Element parent, boolean isTopLevel, boolean hasChildren) {
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
        final Text text = getDocument().createTextNode(builder.toString());
        parent.appendChild(text);
    }

    private void addPostElementText(Element parent) {
        if (parent == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        Node node = parent.getParentNode();
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            builder.append("    ");
            node = node.getParentNode();
        }
        Node text = getDocument().createTextNode(builder.toString());
        parent.appendChild(text);
    }

    public boolean hasElement(String name) {
        if (name == null)
            return false;
        return (getFirstChildElement(getServerElement(), name) != null);
    }

    public void setAttribute(String elementName, String name, String value) {
        if (elementName == null || name == null)
            return;

        synchronized (configLock) {
            Element element = getFirstChildElement(getServerElement(), elementName);
            if (element == null)
                return;

            element.setAttribute(name, value);
        }
    }

    public boolean hasFeature(String feature) {
        if (feature == null)
            return false;
        for (Element fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER); fManager != null; fManager = getNextElement(fManager,
                                                                                                                                                 Constants.FEATURE_MANAGER)) {
            for (Element fElem = getFirstChildElement(fManager); fElem != null; fElem = getNextElement(fElem)) {
                if (feature.equalsIgnoreCase(fElem.getTextContent()))
                    return true;
            }
        }
        return false;
    }

    public List<String> getFeatures() {
        List<String> features = new ArrayList<String>();
        for (Element fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER); fManager != null; fManager = getNextElement(fManager,
                                                                                                                                                 Constants.FEATURE_MANAGER)) {
            for (Element fElem = getFirstChildElement(fManager); fElem != null; fElem = getNextElement(fElem)) {
                String feature = fElem.getTextContent();
                if (feature != null && !feature.isEmpty() && !features.contains(feature))
                    features.add(feature);
            }
        }
        return features;
    }

    /**
     * Get all configured features.
     * Features are resolved using the config root, includes and config dropins.
     *
     * @return list of resolved features
     */
    public List<String> getAllFeatures() {
        final List<String> features = new ArrayList<String>();

        List<Element> elements = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), Constants.FEATURE_MANAGER, null);
        for (Element elem : elements) {
            for (Element fElem = getFirstChildElement(elem, Constants.FEATURE); fElem != null; fElem = getNextElement(fElem, Constants.FEATURE)) {
                String feature = fElem.getTextContent();
                if (feature != null && !feature.isEmpty() && !features.contains(feature))
                    features.add(feature);
            }
        }
        return features;
    }

    public void addFeatures(Collection<String> features) {
        for (String feature : features)
            addFeature(feature);
    }

    public void addFeature(String feature) {
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile adding feature: " + feature);
            Element fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER);
            if (fManager == null) {
                addElement(Constants.FEATURE_MANAGER);
                fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER);
                if (fManager == null) {
                    // something went wrong
                    if (Trace.ENABLED) {
                        Trace.logError("Failed to add feature manager to configuration file: " + uri, null);
                    }
                    return;
                }
            }

            // Add the feature to the feature manager
            Element child = addElement(fManager, Constants.FEATURE);
            Document doc = fManager.getOwnerDocument();
            Text text = doc.createTextNode(feature);
            child.appendChild(text);
        }
    }

    /**
     * Replace feature1 with feature 2.
     *
     * @param feature1 a feature to remove
     * @param feature2 a feature to add
     */
    public void replaceFeature(String feature1, String feature2) {
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile replacing feature: " + feature1 + " -> " + feature2);

            // check for existing node first
            for (Element fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER); fManager != null; fManager = getNextElement(fManager,
                                                                                                                                                     Constants.FEATURE_MANAGER)) {
                for (Element fElem = getFirstChildElement(fManager); fElem != null; fElem = getNextElement(fElem)) {
                    if (feature1.equals(fElem.getTextContent())) {
                        fElem.setTextContent(feature2);
                        return;
                    }
                }
            }
        }
    }

    public void addInclude(boolean optional, String include) {
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile adding include: " + include + " " + optional);

            Element includeElement = addElement(Constants.INCLUDE_ELEMENT);
            if (optional)
                includeElement.setAttribute(Constants.OPTIONAL_ATTRIBUTE, "true");
            includeElement.setAttribute(Constants.LOCATION_ATTRIBUTE, include);

            resetIncludes();
        }
    }

    public Application[] getApplications() {
        // RTC 154074 - Make sure that we wait if we are in the process of
        // adding an application to the configuration file. The configLock
        // would be owned by the thread that is updating the configuration
        // file.
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "getApplications() - before synchronized block");

        List<Application> list = new ArrayList<Application>();
        Map<String, Boolean> appLabelMap = getAppLabelMap();
        synchronized (configLock) {
            for (String appLabel : ServerExtensionWrapper.getAllApplicationElements()) {
                List<Element> elements = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), appLabel, Constants.FACTORY_ID);
                for (Element appElem : elements) {
                    Application app = createApplication(appElem, appLabel, appLabelMap);
                    list.add(app);
                }
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "getApplications() - after synchronized block");

        return list.toArray(new Application[list.size()]);
    }

    private Application createApplication(Element appElem, String appLabel, Map<String, Boolean> appLabelMap) {
        String appId = getAppId(appElem, appLabelMap);

        EnumSet<APIVisibility> apiVisibility = APIVisibility.getDefaults();

        Element classLoaderElement = getFirstChildElement(appElem, Constants.LIB_CLASSLOADER);
        if (classLoaderElement != null) {
            String apiVisibilityAttributeValue = classLoaderElement.getAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME);
            if (apiVisibilityAttributeValue != null && apiVisibilityAttributeValue.trim().length() > 0) {
                apiVisibility = APIVisibility.parseFromAttribute(apiVisibilityAttributeValue);
            }
        }

        String appName = getAppName(appElem, appId);
        String appLocation = resolveValue(appElem.getAttribute(Constants.APP_LOCATION));
        String appAutostart = resolveValue(appElem.getAttribute(Constants.APP_AUTOSTART));

        Application app = new Application(appName, ServerExtensionWrapper.getAppTypeFromAppElement(appLabel), appLocation, appAutostart, getSharedLibRefs(appElem), apiVisibility);
        return app;
    }

    private String[] getSharedLibRefs(Element appElem) {
        List<String> libRefIds = new ArrayList<String>(2);

        for (Element child = getFirstChildElement(appElem, Constants.LIB_CLASSLOADER); child != null; child = getNextElement(child, Constants.LIB_CLASSLOADER)) {
            String ids = child.getAttribute(Constants.LIB_COMMON_LIBREF);
            if (ids != null && !ids.isEmpty()) {
                ids = resolveValue(ids);
                StringTokenizer st = new StringTokenizer(ids, ",");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken().trim();
                    if (!s.isEmpty() && !libRefIds.contains(s))
                        libRefIds.add(s);
                }
            }
        }

        if (libRefIds.isEmpty())
            return null;

        String[] result = new String[libRefIds.size()];
        return libRefIds.toArray(result);
    }

    /**
     * Add the given application to the configuration, or update an existing application if the
     * name already exists.
     *
     * @param name
     * @param applicationElement
     * @param location
     * @param apiVisibility
     */
    public void addApplication(String name, String applicationElement, String location, Map<String, String> attributes, List<String> sharedLibRefs,
                               EnumSet<APIVisibility> apiVisibility) {
        Map<String, Boolean> appLabelMap = getAppLabelMap();
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile adding application: " + name);
            if (name == null)
                return;

            boolean isSupportAppElement = server.getSchemaHelper().isSupportedApplicationElement(applicationElement);
            // check for existing node first
            if (isSupportAppElement) {
                for (Element appElem = getFirstChildElement(getServerElement(), applicationElement); appElem != null; appElem = getNextElement(appElem, applicationElement)) {
                    String appId = getAppId(appElem, appLabelMap);
                    if (name.equals(getAppName(appElem, appId))) {
                        String appLocation = resolveValue(appElem.getAttribute(Constants.APP_LOCATION));
                        if (location != null && location.equals(appLocation)) {
                            addSharedLibRefs(appElem, sharedLibRefs, apiVisibility);
                            return;
                        }
                        // update the other attributes
                        appElem.setAttribute(Constants.APP_LOCATION, location);
                        addAppAttributes(appElem, attributes);
                        addSharedLibRefs(appElem, sharedLibRefs, apiVisibility);
                        return;
                    }
                }
            }

            // check for non domain specific config support
            String type = ServerExtensionWrapper.getAppTypeFromAppElement(applicationElement);
            if (type != null) {
                for (Element appElem = getFirstChildElement(getServerElement(), Constants.APPLICATION); appElem != null; appElem = getNextElement(appElem, Constants.APPLICATION)) {
                    String appId = getAppId(appElem, appLabelMap);
                    if (name.equals(getAppName(appElem, appId))) {
                        String appLocation = resolveValue(appElem.getAttribute(Constants.APP_LOCATION));
                        if (location != null && location.equals(appLocation)) {
                            addSharedLibRefs(appElem, sharedLibRefs, apiVisibility);
                            return;
                        }
                        appElem.setAttribute(Constants.APP_TYPE, type);
                        appElem.setAttribute(Constants.APP_LOCATION, location);
                        addAppAttributes(appElem, attributes);
                        addSharedLibRefs(appElem, sharedLibRefs, apiVisibility);
                        return;
                    }
                }
            }

            Element appElement;
            if (!isSupportAppElement) {
                appElement = addElement(Constants.APPLICATION);
                appElement.setAttribute(Constants.APP_TYPE, type);
            } else {
                appElement = addElement(applicationElement);
            }
            appElement.setAttribute(Constants.INSTANCE_ID, name);
            if (appHasNameAttr(appElement, appLabelMap))
                appElement.setAttribute(Constants.APP_NAME, name);
            appElement.setAttribute(Constants.APP_LOCATION, location);
            addAppAttributes(appElement, attributes);

            if (sharedLibRefs != null)
                addSharedLibRefs(appElement, sharedLibRefs, apiVisibility);
        }
    }

    private boolean appHasNameAttr(Element appElem, Map<String, Boolean> appLabelMap) {
        return Boolean.TRUE == appLabelMap.get(appElem.getNodeName());
    }

    public String getAppName(Element appElem) {
        // Originally applications had both an id and a name but it was
        // decided in a design issue discussion that new application
        // types would just have the id.  This code will handle both.
        Map<String, Boolean> appLabelMap = getAppLabelMap();
        String appId = getAppId(appElem, appLabelMap);
        return getAppName(appElem, appId);
    }

    private String getAppId(Element appElem, Map<String, Boolean> appLabelMap) {
        if (appHasNameAttr(appElem, appLabelMap))
            return Constants.APP_NAME;

        return Constants.INSTANCE_ID;
    }

    private String getAppName(Element appElem, String appId) {
        String name = appElem.getAttribute(appId);
        // If the id (or name) was not set, use the location to
        // get the application name.
        if (name == null || name.isEmpty()) {
            String location = appElem.getAttribute(Constants.APP_LOCATION);
            location = resolveValue(location);
            if (location != null && !location.isEmpty()) {
                IPath path = new Path(location);
                path = path.removeFileExtension();
                name = path.lastSegment();
            }
        } else {
            name = resolveValue(name);
        }
        return name;
    }

    private void addAppAttributes(Element appElem, Map<String, String> attributes) {
        synchronized (configLock) {
            if (attributes != null && !attributes.isEmpty()) {
                Set<Map.Entry<String, String>> set = attributes.entrySet();
                Iterator<Map.Entry<String, String>> itr = set.iterator();
                while (itr.hasNext()) {
                    Map.Entry<String, String> entry = itr.next();
                    appElem.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void addSharedLibRefs(Element parent, List<String> refIds, EnumSet<APIVisibility> apiVisibility) {
        if (parent == null)
            return;

        String[] currentIds = getSharedLibRefs(parent);
        List<String> newIds = new ArrayList<String>();
        List<String> oldIds = new ArrayList<String>();

        if (refIds == null || refIds.isEmpty()) {
            // nothing to do
            if (currentIds == null || currentIds.length == 0)
                return;

            for (String id : currentIds)
                oldIds.add(id);
        } else {
            newIds.addAll(refIds);
            if (currentIds != null) {
                for (String id : currentIds) {
                    if (newIds.contains(id)) {
                        newIds.remove(id);
                    } else {
                        oldIds.add(id);
                    }
                }
            }
        }

        EnumSet<APIVisibility> currentAPIVisibility = APIVisibility.getDefaults();

        Element classLoaderElement = getFirstChildElement(parent, Constants.LIB_CLASSLOADER);
        if (classLoaderElement != null) {
            String apiVisibilityAttributeValue = classLoaderElement.getAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME);
            if (apiVisibilityAttributeValue != null && apiVisibilityAttributeValue.trim().length() > 0) {
                currentAPIVisibility = APIVisibility.parseFromAttribute(classLoaderElement.getAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME));
            }
        }

        boolean apiVisibilityChanged = !currentAPIVisibility.equals(apiVisibility);

        // No ids to add or remove
        if (newIds.isEmpty() && oldIds.isEmpty() && !apiVisibilityChanged)
            return;

        synchronized (configLock) {
            if (!newIds.isEmpty()) {
                Element child = getFirstChildElement(parent, Constants.LIB_CLASSLOADER);
                if (child == null) {
                    child = addElement(parent, Constants.LIB_CLASSLOADER);
                    child.setAttribute(Constants.LIB_COMMON_LIBREF, listToString(newIds));
                } else {
                    String ids = child.getAttribute(Constants.LIB_COMMON_LIBREF);
                    StringBuilder sb = new StringBuilder();
                    if (ids != null && ids.length() > 0) {
                        sb.append(ids);
                        sb.append(',');
                    }
                    sb.append(listToString(newIds));
                    child.setAttribute(Constants.LIB_COMMON_LIBREF, sb.toString());
                }
            }

            if (!oldIds.isEmpty()) {
                List<Element> removeList = new ArrayList<Element>();
                for (Element child = getFirstChildElement(parent, Constants.LIB_CLASSLOADER); child != null; child = getNextElement(child, Constants.LIB_CLASSLOADER)) {
                    String ids = child.getAttribute(Constants.LIB_COMMON_LIBREF);
                    if (ids != null && !ids.isEmpty()) {
                        List<String> libRefIds = new ArrayList<String>();
                        StringTokenizer st = new StringTokenizer(ids, ",");
                        while (st.hasMoreTokens()) {
                            String s = st.nextToken().trim();
                            if (!s.isEmpty() && !oldIds.contains(s))
                                libRefIds.add(s);
                        }
                        if (!libRefIds.isEmpty())
                            child.setAttribute(Constants.LIB_COMMON_LIBREF, listToString(libRefIds));
                        else {
                            // Remove child if it only has LIB_COMMON_LIBREF
                            if (child.getAttributes().getLength() > 1)
                                child.removeAttribute(Constants.LIB_COMMON_LIBREF);
                            else
                                removeList.add(child);
                        }
                    }
                }

                for (Element child : removeList)
                    removeElement(child);
            }

            if (apiVisibilityChanged) {
                Element child = getFirstChildElement(parent, Constants.LIB_CLASSLOADER);
                if (child != null) {
                    child.setAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME, APIVisibility.generateAttributeValue(apiVisibility));
                }
            }
        }
    }

    private String listToString(List<String> items) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : items) {
            if (item.trim().isEmpty())
                continue;
            if (first) {
                sb.append(item);
                first = false;
            } else {
                sb.append(',').append(item);
            }
        }

        return sb.toString();
    }

    public void addSharedLibrary(String libraryId, String directory, String jarName, EnumSet<APIVisibility> apiVisibility) {
        if (libraryId == null || directory == null || jarName == null) {
            Trace.logError("Null parameters are passed in addSharedLibrary() " + libraryId + " " + directory + " " + jarName, null);
            return;
        }

        synchronized (configLock) {
            // check for existing node first
            Element serverElem = getServerElement();
            for (Element libElem = getFirstChildElement(serverElem, Constants.LIBRARY); libElem != null; libElem = getNextElement(libElem, Constants.LIBRARY)) {
                if (libraryId.equals(libElem.getAttribute(Constants.INSTANCE_ID))) {

                    String sharedLibAttribute = libElem.getAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME);

                    if (!APIVisibility.getDefaults().equals(apiVisibility) && (sharedLibAttribute == null || sharedLibAttribute.trim().length() == 0)) {
                        libElem.setAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME, APIVisibility.generateAttributeValue(apiVisibility));
                    }

                    for (Element filesetElem = getFirstChildElement(libElem, Constants.LIB_FILESET); filesetElem != null; filesetElem = getNextElement(filesetElem,
                                                                                                                                                       Constants.LIB_FILESET)) {
                        String includeName = filesetElem.getAttribute(Constants.LIB_INCLUDES);
                        if (includeName != null && directory.equals(filesetElem.getAttribute(Constants.LIB_DIR)) &&
                            (includeName.equals("*.jar") || includeName.equals("*") ||
                             jarName.equals(includeName)))
                            return; // there is an existing one
                    }
                    // add new fileset
                    addFileset(libElem, directory, jarName);
                    return;
                }
            }

            // no matched library add a new one.
            Element e = addElement(Constants.LIBRARY);
            e.setAttribute(Constants.INSTANCE_ID, libraryId);
            if (!APIVisibility.getDefaults().equals(apiVisibility)) {
                e.setAttribute(Constants.API_VISIBILITY_ATTRIBUTE_NAME, APIVisibility.generateAttributeValue(apiVisibility));
            }
            addFileset(e, directory, jarName);
        }
    }

    public String[] getSharedLibraryIds() {
        final List<String> idList = new ArrayList<String>();

        List<Element> elements = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), Constants.LIBRARY, Constants.INSTANCE_ID);
        for (Element elem : elements) {
            String value = elem.getAttribute(Constants.INSTANCE_ID);
            if (value != null && !value.isEmpty()) {
                value = resolveValue(value);
                if (!idList.contains(value))
                    idList.add(value);
            }
        }

        return idList.toArray(new String[idList.size()]);
    }

    public String getLogDirectoryAttribute() {
        List<Element> elements = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), Constants.LOGGING, null);
        for (Element elem : elements) {
            if (elem != null) {
                String logDir = elem.getAttribute(Constants.LOG_DIR);
                if (logDir != null && !logDir.isEmpty())
                    return logDir;
            }
        }
        return null;
    }

    public void removeSharedLibrary(String libraryId, String directory, String jarName) {
        if (libraryId == null || directory == null || jarName == null) {
            Trace.logError("Null parameters are passed in removeSharedLibrary() " + libraryId + " " + directory + " " + jarName, null);
            return;
        }

        synchronized (configLock) {
            // check for existing node
            Element serverElem = getServerElement();
            for (Element libElem = getFirstChildElement(serverElem, Constants.LIBRARY); libElem != null; libElem = getNextElement(libElem, Constants.LIBRARY)) {
                if (libraryId.equals(libElem.getAttribute(Constants.INSTANCE_ID))) {
                    for (Element filesetElem = getFirstChildElement(libElem, Constants.LIB_FILESET); filesetElem != null; filesetElem = getNextElement(filesetElem,
                                                                                                                                                       Constants.LIB_FILESET)) {
                        if (directory.equals(filesetElem.getAttribute(Constants.LIB_DIR)) &&
                            jarName.equals(filesetElem.getAttribute(Constants.LIB_INCLUDES))) {
                            removeElement(filesetElem);
                        }
                    }

                    // after remove the fileset, check if the lib is empty. if so, remove it
                    if (!hasSignificantChildren(libElem)) {
                        removeElement(libElem);
                    }
                }
            }
        }
    }

    /**
     * Remove an element. If the element is a child of the server element
     * then we remove the extra spacing.
     *
     * @param element
     */
    private void removeElement(Element element) {
        synchronized (configLock) {
            Node parentNode = element.getParentNode();

            // remove prior text
            Node previous = element.getPreviousSibling();
            while (previous != null && previous.getNodeType() == Node.TEXT_NODE && previous.getNodeValue().trim().length() == 0) {
                // Save off the node to remove
                Node nodeToRemove = previous;
                // Get the previous sibling before removing the node, once it is
                // removed and is no longer part of the DOM then getPreviousSibling
                // will not work.
                previous = previous.getPreviousSibling();
                parentNode.removeChild(nodeToRemove);
            }

            // remove element
            parentNode.removeChild(element);

            // clean up single child text node
            NodeList childNodes = parentNode.getChildNodes();
            if (childNodes != null && childNodes.getLength() == 1 && childNodes.item(0).getNodeType() == Node.TEXT_NODE &&
                childNodes.item(0).getNodeValue().trim().length() == 0)
                parentNode.removeChild(childNodes.item(0));
        }

    }

    /**
     * @param libElem
     * @param directory
     * @param jarName
     */
    private void addFileset(Element libElem, String directory, String jarName) {
        synchronized (configLock) {
            Element e = addElement(libElem, Constants.LIB_FILESET);
            e.setAttribute(Constants.LIB_DIR, directory);
            e.setAttribute(Constants.LIB_INCLUDES, jarName);
        }
    }

    public boolean removeApplication(String name) {
        if (name == null)
            return false;

        for (String appLabel : ServerExtensionWrapper.getAllApplicationElements()) {
            for (Element appElem = getFirstChildElement(getServerElement(), appLabel); appElem != null; appElem = getNextElement(appElem, appLabel)) {
                if (name.equals(getAppName(appElem))) {
                    removeElement(appElem);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean removeElement(String name) {
        if (name == null)
            return false;

        synchronized (configLock) {
            Element appElem = getFirstChildElement(getServerElement(), name);
            if (appElem != null) {
                removeElement(appElem);
                return true;
            }

            return false;
        }
    }

    private void touch() {
        final IFile ifile = getIFile();
        if (ifile != null) {
            try {
                ifile.touch(new NullProgressMonitor());
            } catch (CoreException ce) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not refresh local file", ce);
            }
        }
    }

    public void save(IProgressMonitor progressMonitor) throws IOException {
        // If document is null, it might be related to xml parsing errors
        // and we do not want to store an empty file.
        // So, just touch the file to indicate that it was changed
        if (document == null) {
            touch();
            return;
        }

        BufferedOutputStream w = null;
        try {
            w = new BufferedOutputStream(new FileOutputStream(new File(uri)));
            save(w);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            try {
                if (w != null)
                    w.close();
            } catch (Exception e) {
                // ignore
            }
            if (getIFile() != null)
                try {
                    IProgressMonitor monitor = progressMonitor == null ? new NullProgressMonitor() : progressMonitor;
                    getIFile().refreshLocal(2, monitor);
                } catch (CoreException ce) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not refresh local file", ce);
                }
        }
    }

    private void save(OutputStream os) throws IOException {
        Result result = new StreamResult(os);
        Source source = new DOMSource(document);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
        } catch (Exception e) {
            throw (IOException) (new IOException().initCause(e));
        }
    }

    public List<ServerPort> getPorts(List<ServerPort> portList) {
        int httpPort = getHTTPPort();
        if (httpPort != -1) {
            ServerPort sp = new ServerPort(Constants.HTTP_ENDPOINT + '/' + Constants.HTTP_PORT, Constants.HTTP_PORT, httpPort, "http");
            portList.add(sp);
        }

        int httpsPort = getHTTPSPort();
        if (httpsPort != -1) {
            ServerPort sp = new ServerPort(Constants.HTTP_ENDPOINT + '/' + Constants.HTTPS_PORT, Constants.HTTPS_PORT, httpsPort, "https");
            portList.add(sp);
        }

        return portList;
    }

    public List<Integer> getAllPorts(boolean secure) {
        List<Integer> allPorts = new ArrayList<Integer>();
        List<Element> httpEndpoints = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), Constants.HTTP_ENDPOINT, Constants.FACTORY_ID);
        for (Element elem : httpEndpoints) {
            String port = elem.getAttribute(secure ? Constants.HTTPS_PORT : Constants.HTTP_PORT);
            if (port != null && !port.isEmpty()) {
                port = resolveValue(port);
                try {
                    allPorts.add(new Integer(port));
                } catch (NumberFormatException numberFormatException) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "HTTP endpoint had invalid port", numberFormatException);
                }
            }
        }
        return allPorts;
    }

    public Document flatten(File file) {
        try {
            Document newDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Node node = getServerElement();
            Node cloneNode = newDocument.importNode(node, false);
            final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
            CMElementDeclaration includeDecl = null;
            Document domDocument = getDomDocument();
            if (domDocument != null) {
                includeDecl = SchemaUtil.getElement(getDomDocument(), new String[] { Constants.SERVER_ELEMENT, Constants.INCLUDE_ELEMENT }, getURI());
            }

            newDocument.appendChild(cloneNode);
            flattenImpl(newDocument, cloneNode, includeFilter, includeDecl);

            BufferedOutputStream w = null;
            try {
                w = new BufferedOutputStream(new FileOutputStream(file));
                Result result = new StreamResult(w);
                Source source = new DOMSource(newDocument);
                try {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
                    transformer.transform(source, result);
                } catch (Exception e) {
                    throw (IOException) (new IOException().initCause(e));
                }
            } catch (IOException e) {
                Trace.logError("Error flattening config", e);
            } catch (Exception e) {
                Trace.logError("Error flattening config", e);
            } finally {
                try {
                    if (w != null)
                        w.close();
                } catch (Exception e) {
                    // ignore
                }
            }

            return document;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private void flattenImpl(Document newDocument, Node newNode, IncludeFilter includeFilter, CMElementDeclaration includeDecl) {
        // if we have seen the configuration file before, simply return
        if (!includeFilter.accept(getURI())) {
            return;
        }

        Element root = getServerElement();
        if (root == null) {
            return;
        }

        for (ConfigurationFile dropin : getDefaultDropins()) {
            IFile ifile = dropin.getIFile();
            String location = ifile != null ? ifile.getFullPath().toString() : dropin.getPath().toString();
            Node comment = newDocument.createComment(NLS.bind(Messages.mergedConfigBeginDropin, location));
            newNode.appendChild(comment);
            dropin.flattenImpl(newDocument, newNode, includeFilter, includeDecl);
            comment = newDocument.createComment(NLS.bind(Messages.mergedConfigEndDropin, location));
            newNode.appendChild(comment);
        }

        for (Element elem = getFirstChildElement(root); elem != null; elem = getNextElement(elem)) {
            if (isInclude(elem)) {
                ConfigurationFile configFile = getIncludes().get(elem);
                if (configFile != null) {
                    IFile ifile = configFile.getIFile();
                    String location = ifile != null ? ifile.getFullPath().toString() : configFile.getPath().toString();
                    Node comment = newDocument.createComment(NLS.bind(Messages.mergedConfigBeginInclude, new String[] { location, formatAttributes(elem, includeDecl) }));
                    newNode.appendChild(comment);
                    configFile.flattenImpl(newDocument, newNode, includeFilter, includeDecl);
                    comment = newDocument.createComment(NLS.bind(Messages.mergedConfigEndInclude, location));
                    newNode.appendChild(comment);
                }
            } else {
                Node cloneNode = newDocument.importNode(elem, true);
                newNode.appendChild(cloneNode);
            }
        }

        for (ConfigurationFile dropin : getOverrideDropins()) {
            IFile ifile = dropin.getIFile();
            String location = ifile != null ? ifile.getFullPath().toString() : dropin.getPath().toString();
            Node comment = newDocument.createComment(NLS.bind(Messages.mergedConfigBeginDropin, location));
            newNode.appendChild(comment);
            dropin.flattenImpl(newDocument, newNode, includeFilter, includeDecl);
            comment = newDocument.createComment(NLS.bind(Messages.mergedConfigEndDropin, location));
            newNode.appendChild(comment);
        }
    }

    @SuppressWarnings("deprecation")
    private String formatAttributes(Element elem, CMElementDeclaration elemDecl) {
        StringBuilder builder = new StringBuilder();
        if (elemDecl == null) {
            NamedNodeMap attrs = elem.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attr = (Attr) attrs.item(i);
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(attr.getLocalName() + "='" + attr.getValue() + "'");
            }
            return builder.toString();
        }

        CMNamedNodeMap attrs = elemDecl.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            CMAttributeDeclaration attr = (CMAttributeDeclaration) attrs.item(i);
            String name = attr.getAttrName();
            String value = elem.hasAttribute(name) ? elem.getAttribute(name) : attr.getDefaultValue();
            if (value == null) {
                // The "optional" attribute currently does not have a default.
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name + "='" + value + "'");
        }
        return builder.toString();
    }

    @Override
    @SuppressWarnings({ "rawtypes" })
    public Object getAdapter(Class adapter) {
        if (IResource.class.equals(adapter))
            return getIFile();
        //if (IContributorResource)
        //ResourceMapping
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConfigurationFile))
            return false;

        ConfigurationFile cf = (ConfigurationFile) obj;

        if ((uri == null && cf.uri != null) || (uri != null && !uri.equals(cf.uri)))
            return false;

        if ((userDir == null && cf.userDir != null) || (userDir != null && !userDir.equals(cf.userDir)))
            return false;

        if (lastModified != cf.lastModified)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (uri != null)
            hash += uri.hashCode();
        if (userDir != null)
            hash += userDir.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        long lastModified = new File(uri).lastModified();
        return "WAS Configuration File [" + uri.toASCIIString() + ":" + lastModified + "]";
    }

    public int getHTTPPort() {
        return getHTTPPort(Constants.HTTP_PORT, 9080);
    }

    public int getHTTPSPort() {
        return getHTTPPort(Constants.HTTPS_PORT, 9443);
    }

    private int getHTTPPort(String portAttr, int defaultPortNum) {
        List<Element> httpEndpoints = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), Constants.HTTP_ENDPOINT, Constants.FACTORY_ID);
        for (Element elem : httpEndpoints) {
            if (Constants.DEFAULT_HTTP_ENDPOINT.equals(elem.getAttribute(Constants.FACTORY_ID))) {
                String port = elem.getAttribute(portAttr);
                if (port != null && !port.isEmpty()) {
                    port = resolveValue(port);
                    try {
                        return Integer.parseInt(port);
                    } catch (NumberFormatException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "HttpEndpoint had invalid HttpPort", e);
                    }
                }
            }
        }

        String[] tags = new String[] { Constants.SERVER_ELEMENT, Constants.HTTP_ENDPOINT };
        String defaultPort = SchemaUtil.getAttributeDefault(getDomDocument(), tags, portAttr, uri);
        if (defaultPort != null) {
            try {
                return Integer.parseInt(defaultPort);
            } catch (NumberFormatException e) {
                Trace.logError("Could not parse the schema default port number: " + defaultPort, e);
            }
        }

        return defaultPortNum;
    }

    public String getResolvedAttributeValue(String element, String attribute) {
        List<Element> elements = ConfigUtils.getResolvedElements(getDocument(), getURI(), server, getUserDirectory(), element, null);
        if (elements.size() > 0) {
            // Since elements are merged, there should just be 1
            String value = elements.get(0).getAttribute(attribute);
            if (value != null && !value.isEmpty())
                return resolveValue(value);
        }
        return null;
    }

    private String resolveValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        ConfigVars vars = getConfigVars();
        return vars.resolve(value.trim());
    }

    private ConfigVars getConfigVars() {
        synchronized (configLock) {
            if (configVars == null) {
                configVars = new ConfigVars();
                if (server != null) {
                    server.getVariables(configVars);
                } else if (userDir != null) {
                    userDir.getVariables(configVars);
                }
                if (server != null) {
                    CustomServerVariablesManager customServerVariablesManager = CustomServerVariablesManager.getInstance();
                    customServerVariablesManager.addCustomServerVariables(configVars, server);
                }
                ConfigUtils.getVariables(this, configVars);
            }
            return configVars;
        }
    }

    public boolean hasChanged() {
        File file = new File(uri);
        long timestamp = file.lastModified();
        return timestamp != lastModified;
    }

    public boolean isNewerThan(long timestamp, boolean recurse) {
        if (recurse) {
            // The list includes this config file.
            final List<ConfigurationFile> configFiles = new ArrayList<ConfigurationFile>();
            final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
            getAllConfigFiles(configFiles, includeFilter);
            for (ConfigurationFile configFile : configFiles) {
                if (configFile.isNewerThan(timestamp, false)) {
                    return true;
                }
            }
            return false;
        }
        File file = new File(uri);
        long lastModified = file.lastModified();
        return lastModified > timestamp;
    }

    private Map<Element, ConfigurationFile> getIncludes() {
        synchronized (configLock) {
            if (includes == null) {
                includes = new IdentityHashMap<Element, ConfigurationFile>();
                unresolvedIncludes = new ArrayList<String>();
                for (Element includeElem = getFirstChildElement(getServerElement(), Constants.INCLUDE_ELEMENT); includeElem != null; includeElem = getNextElement(
                                                                                                                                                                  includeElem,
                                                                                                                                                                  Constants.INCLUDE_ELEMENT)) {
                    String location = getAttributeValue(includeElem, Constants.LOCATION_ATTRIBUTE);
                    if (location != null) {
                        final boolean referenceInInclude = ConfigVarsUtils.containsReference(location);
                        URI includeURI = (referenceInInclude && server != null) ? server.resolve(getURI(), location) : getUserDirectory().resolve(getURI(), location);
                        try {
                            if (includeURI != null && new File(includeURI).exists()) {
                                // if include URI is same as current configuration
                                // URI, do not include it
                                if (!includeURI.equals(getURI())) {
                                    ConfigurationFile config = new ConfigurationFile(includeURI, getUserDirectory(), server);
                                    includes.put(includeElem, config);
                                }
                            } else {
                                unresolvedIncludes.add(location);
                            }
                        } catch (FileNotFoundException e) {
                            // caused when file does not exist
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Invalid file: " + includeURI);
                        } catch (IllegalArgumentException e) {
                            // caused when the URI is not valid
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Invalid uri: " + includeURI);
                        } catch (Exception e) {
                            Trace.logError("Error loading config file " + includeURI, e);
                        }
                    }
                }
            }
            return includes;
        }
    }

    private boolean hasOutOfSyncDropins() {
        if (server == null || !isConfigRoot())
            return false;
        List<File> dropins = getDropinFiles(server.getConfigDefaultDropinsPath());
        if (checkDropins(defaultDropins, dropins))
            return true;
        dropins = getDropinFiles(server.getConfigOverrideDropinsPath());
        if (checkDropins(overrideDropins, dropins))
            return true;
        return false;
    }

    private boolean checkDropins(List<ConfigurationFile> oldDropins, List<File> newDropins) {
        if (oldDropins == null)
            return (newDropins != null && newDropins.size() > 0);
        if (newDropins == null)
            return oldDropins.size() > 0;
        if (oldDropins.size() != newDropins.size())
            return true;
        // If the sizes are the same, check if any dropins have been removed in
        // which case there was an equal number of adds and removes
        for (ConfigurationFile dropin : oldDropins) {
            File file = new File(dropin.getURI());
            if (!file.exists())
                return true;
        }
        return false;
    }

    public List<ConfigurationFile> getDefaultDropins() {
        synchronized (configLock) {
            if (defaultDropins == null) {
                if (server != null && isConfigRoot()) {
                    defaultDropins = getDropins(server.getConfigDefaultDropinsPath());
                } else {
                    defaultDropins = Collections.emptyList();
                }
            }
            return defaultDropins;
        }
    }

    public List<ConfigurationFile> getOverrideDropins() {
        synchronized (configLock) {
            if (overrideDropins == null) {
                if (server != null && isConfigRoot()) {
                    overrideDropins = getDropins(server.getConfigOverrideDropinsPath());
                } else {
                    overrideDropins = Collections.emptyList();
                }
            }
            return overrideDropins;
        }
    }

    private List<File> getDropinFiles(IPath path) {
        List<File> dropins = new ArrayList<File>();
        if (path != null && path.toFile().isDirectory()) {
            File[] files = path.toFile().listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(XML_EXTENSION)) {
                    dropins.add(file);
                }
            }
        }
        return dropins;
    }

    private List<ConfigurationFile> getDropins(IPath path) {
        List<ConfigurationFile> dropins = new ArrayList<ConfigurationFile>();
        File[] files = FileUtil.getSortedFiles(path.toFile(), true);
        for (File file : files) {
            try {
                if (file.getName().endsWith(XML_EXTENSION)) {
                    ConfigurationFile config = new ConfigurationFile(file.toURI(), getUserDirectory(), server);
                    dropins.add(config);
                }
            } catch (Exception e) {
                Trace.logError("Error loading config file " + file.toURI(), e);
            }
        }
        return dropins;
    }

    private void resetIncludes() {
        synchronized (configLock) {
            includes = null;
            unresolvedIncludes = null;
            defaultDropins = null;
            overrideDropins = null;
        }
    }

    public boolean encodePassword(String xpath) {
        Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            Node node = DOMUtils.getNode(doc, xpath);
            if (node != null && node.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) node;
                String value = attr.getValue();
                WebSphereRuntime wsRuntime = null;
                if (server != null)
                    wsRuntime = server.getWebSphereRuntime();
                value = ConfigUtils.encodePassword(value, wsRuntime);
                if (value != null) {
                    attr.setValue(value);
                    return true;
                }
            }
            return false;
        }
    }

    public boolean addVariable(String name, String value) {
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile adding variable name: " + name + " value: " + value);

            try {
                final Element variableElement = addElement(Constants.VARIABLE_ELEMENT);
                variableElement.setAttribute(Constants.VARIABLE_NAME, name);
                variableElement.setAttribute(Constants.VALUE_ATTRIBUTE, value);
                return true;
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.logError("Failed to add a variable element", e);
                }
            }

            return false;
        }
    }

    public boolean replaceVariableReference(String xpath, String reference,
                                            int referenceOffset, String replacement) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            if (node != null && node.getNodeType() == Node.ATTRIBUTE_NODE) {
                final Attr attr = (Attr) node;
                final String value = attr.getValue();
                final StringBuilder sb = new StringBuilder();

                sb.append(value.substring(0, referenceOffset));
                sb.append(replacement);
                sb.append(value.substring(referenceOffset + reference.length(), value.length()));
                attr.setValue(sb.toString());
                return true;
            }
            return false;
        }
    }

    public boolean changePropertyName(String xpath, String name) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            if (node != null && node.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) node;
                String value = attr.getValue();
                Element elem = attr.getOwnerElement();
                elem.removeAttribute(attr.getNodeName());
                elem.setAttribute(name, value);
                return true;
            }

            return false;
        }
    }

    public boolean changeElementName(String xpath, String name) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                final Element elem = doc.createElement(name);
                final NamedNodeMap attrs = node.getAttributes();
                if (attrs != null) {
                    for (int i = 0; i < attrs.getLength(); ++i) {
                        final Node attr = attrs.item(i);
                        elem.setAttribute(attr.getNodeName(), attr.getNodeValue());
                    }
                }

                Node child = node.getFirstChild();
                while (child != null) {
                    final Node next = child.getNextSibling();
                    elem.appendChild(child);
                    child = next;
                }
                final Node parent = node.getParentNode();
                final Node sibling = node.getNextSibling();
                parent.removeChild(node);
                if (sibling == null) {
                    parent.appendChild(elem);
                } else {
                    parent.insertBefore(elem, sibling);
                }
                return true;
            }

            return false;
        }
    }

    public boolean changeFeature(String xpath, String feature) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            if (node != null && Constants.FEATURE.equals(node.getNodeName())) {
                Node oldTextNode = DOMUtils.getTextNode(node);
                if (oldTextNode != null) {
                    Node newTextNode = doc.createTextNode(feature);
                    node.replaceChild(newTextNode, oldTextNode);
                    return true;
                }
            }

            return false;
        }
    }

    public boolean changeFactoryRef(String xpath, String newRef, String oldRef, int index) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            boolean found = false;
            if (node != null && node.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) node;
                String oldValue = attr.getValue();
                String[] items = oldValue.split("[, ]+");
                StringBuilder builder = new StringBuilder();
                boolean first = true;
                for (int i = 0; i < items.length; i++) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    if (i == index && oldRef.equals(items[i])) {
                        builder.append(newRef);
                        found = true;
                    } else {
                        builder.append(items[i]);
                    }
                }
                attr.setValue(builder.toString());
            }

            return found;
        }
    }

    public boolean removeDupFactoryRef(String xpath, String ref, int index) {
        final Document doc = getDocument();
        if (doc == null) {
            return false;
        }

        synchronized (configLock) {
            final Node node = DOMUtils.getNode(doc, xpath);
            boolean found = false;
            if (node != null && node.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) node;
                String oldValue = attr.getValue();
                String[] items = oldValue.split("[, ]+");
                StringBuilder builder = new StringBuilder();
                boolean first = true;
                for (int i = 0; i < items.length; i++) {
                    if (i == index && ref.equals(items[i])) {
                        found = true;
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(items[i]);
                }
                attr.setValue(builder.toString());
            }

            return found;
        }
    }

    private Element getServerElement() {
        return serverElement;
    }

    private Element getFirstChildElement(Element element) {
        if (element == null)
            return null;
        Node node = element.getFirstChild();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private Element getNextElement(Element element) {
        if (element == null)
            return null;
        Node node = element.getNextSibling();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private Element getFirstChildElement(Element element, String name) {
        if (element == null)
            return null;
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private Element getNextElement(Element element, String name) {
        if (element == null) {
            return null;
        }
        Node node = element.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private String getAttributeValue(Element element, String name) {
        if (element == null)
            return null;
        Attr attr = element.getAttributeNode(name);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    private boolean isInclude(Element element) {
        return element.getNodeName().equals(Constants.INCLUDE_ELEMENT);
    }

    private void removeInitialComments() {
        // remove all comments and any new lines in between comments
        Node node = serverElement.getFirstChild();
        while (node != null) {
            final Node current = node;

            node = node.getNextSibling();
            if (current.getNodeType() == Node.COMMENT_NODE) {
                final String value = current.getNodeValue().replaceAll("\\s", "");
                if (isInitialComment(value)) {
                    // if previous and following nodes are text nodes, remove the
                    // previous text node
                    final Node previous = current.getPreviousSibling();
                    if (previous != null && previous.getNodeType() == Node.TEXT_NODE
                        && node != null && node.getNodeType() == Node.TEXT_NODE) {
                        serverElement.removeChild(previous);
                    }
                    serverElement.removeChild(current);
                }
            }
        }
    }

    private boolean isInitialComment(final String value) {
        // we expect 2 comments, so check for either one
        // 1st comment:
        // <!-- Enable features -->
        // 2nd comment:
        // <!--
        // <featureManager>
        //     <feature>servlet-3.0</feature>
        // </featureManager>
        // -->
        return COMMENT_TEXT1.equals(value) || COMMENT_TEXT2.equals(value);
    }

    public Element getApplicationElement(String appElementLabel, String appName) {
        if (appElementLabel == null || appName == null)
            return null;
        boolean isSupportAppElement = server.getSchemaHelper().isSupportedApplicationElement(appElementLabel);
        if (isSupportAppElement) {
            for (Element appElem = getFirstChildElement(getServerElement(), appElementLabel); appElem != null; appElem = getNextElement(appElem, appElementLabel)) {
                if (appName.equals(getAppName(appElem))) {
                    return appElem;
                }
            }
        }

        // check for non domain specific config support
        String type = ServerExtensionWrapper.getAppTypeFromAppElement(appElementLabel);
        if (type != null) {
            for (Element appElem = getFirstChildElement(getServerElement(), Constants.APPLICATION); appElem != null; appElem = getNextElement(appElem, Constants.APPLICATION)) {
                if (appName.equals(getAppName(appElem))) {
                    return appElem;
                }
            }
        }
        return null;
    }

    /**
     * Gets the updateTrigger value of applicationMonitor.
     *
     * @return the value. Null if it doesn't exist
     */
    public String getAppMonitorUpdateTrigger() {
        Element monitor = getFirstChildElement(getServerElement(), "applicationMonitor");
        if (monitor != null) {
            return monitor.getAttribute("updateTrigger");
        }
        return null;
    }

    public void removeFeatures(Collection<String> features) {
        for (String feature : features)
            removeFeature(feature);
    }

    /**
     * Remove feature.
     * This method is currently limited to being used by test case code only. It is not
     * called when a feature is removed in the server config.
     *
     * @param feature a feature to remove
     */
    public void removeFeature(String feature) {
        synchronized (configLock) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ConfigurationFile removing feature: " + feature);

            // check for existing node first
            for (Element fManager = getFirstChildElement(getServerElement(), Constants.FEATURE_MANAGER); fManager != null; fManager = getNextElement(fManager,
                                                                                                                                                     Constants.FEATURE_MANAGER)) {
                for (Element fElem = getFirstChildElement(fManager); fElem != null; fElem = getNextElement(fElem)) {
                    if (feature.equals(fElem.getTextContent())) {
                        removeElement(fElem);
                        return;
                    }
                }
            }
        }
    }

    public boolean isConfigRoot() {
        // Check if the uri is a config root file
        if (uri == null || userDir == null)
            return false;
        URI relative = URIUtil.canonicalRelativize(userDir.getServersPath().toFile().toURI(), uri);
        if (relative.isAbsolute()) {
            return false;
        }
        String path = relative.getPath();
        String[] splitPath = path.split("/");
        if (splitPath.length == 2 && Constants.SERVER_XML.equals(splitPath[1]))
            return true;
        return false;
    }
}
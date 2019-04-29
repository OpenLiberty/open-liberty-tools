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
package com.ibm.ws.st.core.internal.config.validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.ResolverResult;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVars.ExpressionOperandError;
import com.ibm.ws.st.core.internal.config.ConfigVars.ResolvedValueInfo;
import com.ibm.ws.st.core.internal.config.ConfigVars.UndefinedReference;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.config.IncludeConflictResolution;
import com.ibm.ws.st.core.internal.config.LocalConfigVars;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.expression.Expression;

@SuppressWarnings("restriction")
public abstract class AbstractConfigurationValidator {
    public static final String QUICK_FIX_TYPE_ATTR = "quickFixTypeAttr";
    public static final String XPATH_ATTR = "xpathAttr";
    public static final String BEST_MATCH = "bestMatch";
    public static final String ELEMENT_NODE_NAME = "elementNodeName";
    public static final String ATTRIBUTE_NODE_NAME = "attributeNodeName";
    public static final String REFERENCE_NAME = "referenceName";
    public static final String REFERENCE_OFFSET = "referenceOffset";
    public static final String APPLICATION_NAME = "applicationName";
    public static final String APP_SECURITY_ENABLED = "appSecurityEnabled";

    private static final String ORIG_NODE = "origNode";
    private static final String NODE_CONTEXT = "nodeContext";

    private WebSphereRuntime wsRuntime = null;
    private final HashMap<URI, ValidationContext> includes = new HashMap<URI, ValidationContext>();
    private ValidationContext topLevelContext;
    private ValidationContext currentContext;
    private DocumentBuilder documentBuilder = null;
    private ConfigVars globalVars;
    private Map<String, Set<String>> ids;
    private final ResolvedValueInfo resolvedInfo = new ResolvedValueInfo();
    private ValidationFilterMatcher ignoreMatcher;
    private final QuickFixData quickFixData = new QuickFixData();
    private final Matcher matcher = new Matcher();
    private final List<String> attributes = new ArrayList<String>(4);
    private Map<String, Element> featureMap;
    private List<String> features;
    private ResolverResult resolverResult;

    protected enum Level {
        INFO,
        WARNING,
        ERROR
    }

    public enum QuickFixType {
        NONE,
        PLAIN_TEXT_PASSWORD,
        UNRECOGNIZED_ELEMENT,
        UNAVAILABLE_ELEMENT,
        UNRECOGNIZED_PROPERTY,
        UNDEFINED_VARIABLE,
        UNRECOGNIZED_FEATURE,
        OUT_OF_SYNC_APP,
        OUT_OF_SYNC_SHARED_LIB_REF_MISMATCH,
        FACTORY_ID_NOT_FOUND,
        DUPLICATE_FACTORY_ID,
        INVALID_WHITESPACE,
        SUPERSEDED_FEATURE,
        FEATURE_CONFLICT,
        SSL_NO_KEYSTORE,
        REMOTE_SERVER_SECURE_PORT_MISMATCH
    }

    public AbstractConfigurationValidator() {
        super();
    }

    /**
     * Validate the given <code>ConfigurationFile</code>.
     *
     * @param configFile The <code>ConfigurationFile</code> to validate.
     */
    public void validate(ConfigurationFile configFile) {
        validate(ValidationContext.createValidationContext(configFile, null, null));
    }

    /**
     * Validate the given resource.
     *
     * @param resource The resource to validate.
     */
    public void validate(IResource resource) {
        validate(ValidationContext.createValidationContext(resource, null, null));
    }

    /**
     * Validate the given DOM document. This method can be used when a DOM
     * already exists for a resource.
     *
     * @param document The DOM to validate.
     * @param resource The resource from which the DOM was created.
     */
    public void validate(Document document, IResource resource) {
        validate(ValidationContext.createValidationContext(document, resource, null, null));
    }

    private void validate(ValidationContext context) {
        try {
            topLevelContext = context;
            URI uri = context.getURI();
            WebSphereServerInfo serverInfo = null;
            UserDirectory userDir = null;
            if (uri != null) {
                serverInfo = ConfigUtils.getServerInfo(uri);
                if (serverInfo != null) {
                    userDir = serverInfo.getUserDirectory();
                    wsRuntime = serverInfo.getWebSphereRuntime();
                } else {
                    userDir = ConfigUtils.getUserDirectory(uri);
                    if (userDir != null) {
                        wsRuntime = userDir.getWebSphereRuntime();
                    } else {
                        wsRuntime = WebSphereUtil.getTargetedRuntime(uri);
                    }
                }
            }

            processVariables(topLevelContext, serverInfo, userDir);
            processIds(topLevelContext);
            initIgnoreMatcher(topLevelContext);
            checkRemoteServerSecurePort(topLevelContext, serverInfo);
            featureMap = DOMUtils.getFeatureElementMap(context.getDocument(), context.getURI(), context.getServer(), context.getUserDirectory());
            features = new ArrayList<String>(featureMap.keySet());
            resolverResult = RuntimeFeatureResolver.resolve(wsRuntime, features);
            // Add main document to the include list to prevent it from
            // being recursively included
            includes.put(topLevelContext.getURI(), topLevelContext);
            // Create the temporary document to use for merged elements
            Document mergeDoc = createTmpDoc();
            processDocument(topLevelContext, mergeDoc);

            // Specific check for ssl, appSecurity and ejbRemote features but no keystore
            // element (see 176956).  Use the merged document which contains all elements.
            Element root = mergeDoc.getDocumentElement();
            if (root != null) {
                NodeList nodes = root.getElementsByTagName(Constants.FEATURE_MANAGER);
                HashSet<String> features = new HashSet<String>(3);
                Element featureManager = null;
                if (nodes.getLength() > 0) {
                    featureManager = (Element) nodes.item(0);
                    for (Element elem = DOMUtils.getFirstChildElement(featureManager, Constants.FEATURE); elem != null; elem = DOMUtils.getNextElement(elem, Constants.FEATURE)) {
                        String feature = DOMUtils.getTextContent(elem);
                        if (FeatureList.isEnabledBy("ssl-1.0", feature, wsRuntime)) {
                            features.add("ssl");
                        }
                        if (FeatureList.isEnabledBy("appSecurity-1.0", feature, wsRuntime)) {
                            features.add("appSecurity");
                        }
                        if (FeatureList.isEnabledBy("ejbRemote-3.2", feature, wsRuntime)) {
                            features.add("ejbRemote");
                        }
                        if (features.size() == 3) {
                            // If all three features have already been detected, break out of the loop.
                            break;
                        }
                    }
                }

                if (features.size() == 3 || features.contains("ssl")) {
                    // First check what keystore (if any) we need to look for.

                    // If the default ssl is overridden then it will reference an
                    // ssl element that has a keystore ref.  If the keystore is not
                    // defined then the reference checking will have picked it up.
                    //    <ssl id="sslConfig" keyStoreRef="myKeyStore"/>
                    //    <sslDefault sslRef="sslConfig"/>
                    boolean hasNonDefaultSSL = false;
                    nodes = root.getElementsByTagName(Constants.SSL_DEFAULT_ELEMENT);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element defaultSSLElement = (Element) nodes.item(i);
                        String sslRef = defaultSSLElement.getAttribute(Constants.SSL_REF_ATTR);
                        if (sslRef != null && !sslRef.isEmpty() && !sslRef.equals(Constants.DEFAULT_SSL_CONFIG_ID)) {
                            hasNonDefaultSSL = true;
                            break;
                        }
                    }
                    if (!hasNonDefaultSSL) {
                        // Check all ssl elements with the default id to see if the keystore ref
                        // is set to something other than the default.  If it is and the keystore
                        // is not defined then the reference checking will have picked it
                        // up already.
                        //    <ssl id="defaultSSLConfig" keyStoreRef="myKeyStore" />
                        boolean hasNonDefaultKeystore = false;
                        nodes = root.getElementsByTagName(Constants.SSL_ELEMENT);
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Element sslElement = (Element) nodes.item(i);
                            String id = sslElement.getAttribute(Constants.FACTORY_ID);
                            if (Constants.DEFAULT_SSL_CONFIG_ID.equals(id)) {
                                String keystoreRef = sslElement.getAttribute(Constants.KEYSTORE_REF_ATTR);
                                if (keystoreRef != null && !keystoreRef.isEmpty() && !keystoreRef.equals(Constants.DEFAULT_KEY_STORE)) {
                                    hasNonDefaultKeystore = true;
                                    break;
                                }
                            }
                        }
                        if (!hasNonDefaultKeystore) {
                            // Make sure the default keystore is defined and give a warning
                            // if not.
                            boolean foundValidKeystore = false;
                            nodes = root.getElementsByTagName(Constants.KEY_STORE);
                            for (int i = 0; i < nodes.getLength(); i++) {
                                Element keystoreElem = (Element) nodes.item(i);
                                String id = keystoreElem.getAttribute(Constants.INSTANCE_ID);
                                String password = keystoreElem.getAttribute(Constants.PASSWORD_ATTRIBUTE);
                                // The id can be unset or set to the default keystore id, the password must be set
                                if ((id == null || id.isEmpty() || id.equals(Constants.DEFAULT_KEY_STORE)) && password != null && !password.isEmpty()) {
                                    foundValidKeystore = true;
                                    break;
                                }
                            }
                            if (!foundValidKeystore) {
                                // If only ssl enabled then just give a warning.  If all three features
                                // are enabled then the server won't work so give an error.
                                Level level = Level.WARNING;
                                if (features.size() == 3) {
                                    level = Level.ERROR;
                                }
                                Node errorNode = featureManager != null ? (Node) featureManager.getUserData(ORIG_NODE) : null;
                                if (errorNode == null)
                                    errorNode = topLevelContext.getDocument().getDocumentElement();
                                String message = Messages.missingKeystore;
                                quickFixData.setValues(QuickFixType.SSL_NO_KEYSTORE, null, null, -1);
                                if (features.contains("appSecurity")) {
                                    message = Messages.missingKeystoreAndUR;
                                    quickFixData.setAttribute(APP_SECURITY_ENABLED, Boolean.TRUE);
                                }
                                createMessage(message, getTopLevelResource(), level, errorNode, quickFixData);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Trace.logError("Error during validation for: " + context.getURI(), e);
        } finally {
            if (topLevelContext != null) {
                topLevelContext.dispose();
            }
        }
    }

    /**
     * Process the configuration variables
     *
     * @param context The validation context.
     */
    private void processVariables(ValidationContext context, WebSphereServerInfo server, UserDirectory userDir) {
        globalVars = new ConfigVars();

        if (server != null) {
            server.getVariables(globalVars);
        } else if (userDir != null) {
            userDir.getVariables(globalVars);
        }

        processCustomVars(globalVars, context.getResource().getProject());

        final Document doc = context.getDocument();
        if (doc != null) {
            ConfigUtils.getVariables(context.getConfigFile(), doc, context.getURI(), server, context.getUserDirectory(), globalVars);
        }
    }

    private void processCustomVars(ConfigVars globalVars, IProject project) {
        if (project != null) {
            CustomServerVariablesManager customServerVariablesManager = CustomServerVariablesManager.getInstance();
            customServerVariablesManager.addCustomServerVariables(globalVars, project);
        }
    }

    /**
     * Get all of the ids in the configuration. Needed for validating
     * references.
     */
    private void processIds(ValidationContext context) {
        ids = new HashMap<String, Set<String>>();
        if (context.getDocument() != null) {
            ids.putAll(DOMUtils.getAllIds(context.getDocument(), context.getURI(), context.getUserDirectory()));
        }

        String[] dropInLibIds = ConfigUtils.getDropInLibIds(context.getServer(), context.getUserDirectory());
        if (dropInLibIds.length > 0) {
            Set<String> libSet = ids.get(Constants.LIBRARY);
            if (libSet == null) {
                libSet = new HashSet<String>();
                ids.put(Constants.LIBRARY, libSet);
            }
            for (String id : dropInLibIds) {
                libSet.add(id);
            }
        }
    }

    private void initIgnoreMatcher(ValidationContext context) {
        // Validation will run on workspace resources
        final IResource resource = context.getResource();
        if (resource != null) {
            ignoreMatcher = new ValidationFilterMatcher(resource.getProject());
        } else {
            // Assume the user directory is within the workspace
            final UserDirectory ud = context.getUserDirectory();
            if (ud != null) {
                ignoreMatcher = new ValidationFilterMatcher(ud.getProject());
            }
        }
    }

    /**
     * Process the document.
     *
     * @param context The validation context.
     */
    private void processDocument(ValidationContext context, Document mergeDoc) {
        currentContext = context;

        Document doc = currentContext.getDocument();
        if (doc == null) {
            // This should not happen.
            Trace.logError("Could not load DOM for: " + context.getURI(), null);
            return;
        }
        Element serverElem = doc.getDocumentElement();
        if (serverElem == null || !DOMUtils.isServerElement(serverElem)) {
            return;
        }
        processServer(serverElem, mergeDoc);

        currentContext = currentContext.getParent();
    }

    /**
     * Process the server element.
     *
     * @param serverElem The server element.
     */
    protected void processServer(Element serverElem, Document mergeDoc) {
        CMElementDeclaration serverDecl = SchemaUtil.getElement(topLevelContext.getDocument(), new String[] { "server" }, currentContext.getURI());

        if (serverDecl == null) {
            Trace.logError("Validation: could not get server element declaration for " + currentContext.getURI(), null);
            return;
        }

        Element mergeElem = mergeDoc.getDocumentElement();
        if (mergeElem == null) {
            Node clone = mergeDoc.importNode(serverElem, false);
            mergeDoc.appendChild(clone);
            initData(clone, serverElem);
            mergeElem = (Element) clone;
        }

        ConfigurationFile configFile = currentContext.getConfigFile();
        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getDefaultDropins()) {
                ValidationContext dropinContext = ValidationContext.createValidationContext(dropin, currentContext, null);
                dropinContext.setDropin(true);
                processDocument(dropinContext, mergeDoc);
            }
        }

        for (Element element = DOMUtils.getFirstChildElement(serverElem); element != null; element = DOMUtils.getNextElement(element)) {
            processElement(element, mergeElem, serverDecl);
        }

        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getOverrideDropins()) {
                ValidationContext dropinContext = ValidationContext.createValidationContext(dropin, currentContext, null);
                dropinContext.setDropin(true);
                processDocument(dropinContext, mergeDoc);
            }
        }

    }

    /**
     * Process each element.
     *
     * @param element
     */
    protected void processElement(Element element, Element mergeParent, CMElementDeclaration parentDecl) {
        String name = element.getNodeName();
        if (name.equals(Constants.INCLUDE_ELEMENT)) {
            processInclude(element, mergeParent.getOwnerDocument());
        } else if (name.equals(Constants.VARIABLE_ELEMENT)) {
            processVariable(element, mergeParent, parentDecl);
        } else {
            processGeneralElement(element, mergeParent, parentDecl);
        }
    }

    /**
     * Temporary documents are used for creating a merged view of an
     * element when necessary.
     */
    private Document createTmpDoc() {
        Document doc = getDocumentBuilder().newDocument();
        return doc;
    }

    private DocumentBuilder getDocumentBuilder() {
        if (documentBuilder == null) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                documentBuilder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return documentBuilder;
    }

    /**
     * Process an include element. Look for errors with the include itself
     * and then process the included document.
     *
     * @param element The include element.
     */
    private void processInclude(Element element, Document mergeDoc) {
        String path = DOMUtils.getAttributeValue(element, Constants.LOCATION_ATTRIBUTE);
        if ((path != null && !path.isEmpty())) {
            String optional = DOMUtils.getAttributeValue(element, Constants.OPTIONAL_ATTRIBUTE);
            IncludeConflictResolution onConflict = IncludeConflictResolution.getConflictResolution(DOMUtils.getAttributeValue(element, Constants.ONCONFLICT_ATTRIBUTE));
            boolean isOptional = optional == null ? false : Boolean.parseBoolean(optional);
            currentContext.setCurrentInclude(element);
            ValidationContext newContext = null;
            ValidationContext mergedContext = null;
            try {
                // We have to check the Custom Runtime Provider extensions first, otherwise, we will flag
                // include errors incorrectly.
                IResource currentContextResource = currentContext.getResource();
                IFolder mappedConfigFolder = ConfigUtils.getMappedConfigFolder(currentContextResource);
                if (mappedConfigFolder != null) {
                    // we will simply append the path value (the value of the include element) to this config folder path
                    IResource includeFile = mappedConfigFolder.findMember(path);
                    if (includeFile != null && includeFile.exists()) {
                        newContext = ValidationContext.createValidationContext(includeFile, currentContext, IncludeConflictResolution.MERGE);
                    }
                } else {
                    newContext = ValidationContext.createValidationContext(path, currentContext.getURI(), currentContext.getUserDirectory(), currentContext,
                                                                           IncludeConflictResolution.MERGE);
                }
                if (newContext == null) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Failed to create validation context for include file " + path + ".");
                    }
                    if (isTopLevel()) {
                        createMessage(NLS.bind(Messages.errorLoadingInclude, path), currentContext.getResource(), isOptional ? Level.INFO : Level.ERROR, element);
                    }
                } else {
                    ValidationContext previousContext = includes.get(newContext.getURI());
                    // If we have already seen the include URI, we skip it,
                    // and emit a message if necessary
                    if (previousContext != null) {
                        if (emitMessage(previousContext, currentContext)) {
                            String includePath = getFilePathString(newContext);
                            createMessage(NLS.bind(Messages.infoMultipleInclude, includePath), getTopLevelResource(), Level.INFO, getErrorNode(element));
                        }
                        return;
                    }
                    includes.put(newContext.getURI(), currentContext);

                    // Process include on its own to create merged document for include
                    Document includeMergeDoc = createTmpDoc();
                    processDocument(newContext, includeMergeDoc);

                    // Process merged document with parent
                    mergedContext = new DOMValidationContext(includeMergeDoc, newContext.getResource(), newContext.getServer(), newContext.getUserDirectory(), newContext.getParent(), onConflict);
                    processDocument(mergedContext, mergeDoc);
                }
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to validate included file " + path + ".", e);
                }
                if (isTopLevel()) {
                    createMessage(NLS.bind(Messages.errorLoadingInclude, path), currentContext.getResource(), isOptional ? Level.INFO : Level.ERROR, element);
                }
            } finally {
                if (newContext != null) {
                    newContext.dispose();
                }
                if (mergedContext != null) {
                    mergedContext.dispose();
                }
                currentContext.setCurrentInclude(null);
            }
        }
    }

    /**
     * Process a variable element. Make sure it has not been previously declared.
     */
    private void processVariable(Element elem, Element mergeParent, CMElementDeclaration parentDecl) {
        Attr attr = elem.getAttributeNode(Constants.VARIABLE_NAME);
        if (attr == null) {
            // The schema validator will complain about this since it is
            // a required attribute so just return
            return;
        }
        String name = attr.getValue();
        if (ConfigVarsUtils.containsReference(name)) {
            if (isTopLevel()) {
                createMessage(NLS.bind(Messages.variableNameContainsRefs, name), currentContext.getResource(), Level.ERROR, attr);
            }
            name = globalVars.resolve(name);
        }

        // Check for either a value or a defaultValue attribute but not both
        Attr valueAttr = elem.getAttributeNode(Constants.VARIABLE_VALUE);
        Attr defaultValueAttr = elem.getAttributeNode(Constants.VARIABLE_DEFAULT_VALUE);
        if (valueAttr != null && defaultValueAttr != null) {
            createMessage(NLS.bind(Messages.invalidVariableDecl, name), getTopLevelResource(), Level.ERROR, elem);
            // The element is in error so skip further checking
            return;
        } else if (valueAttr == null && defaultValueAttr == null) {
            createMessage(NLS.bind(Messages.variableDeclNoValue, name), getTopLevelResource(), Level.ERROR, elem);
            // The element is in error so skip further checking
            return;
        }

        Element mergeElem = lookupElement(Constants.VARIABLE_ELEMENT, Constants.VARIABLE_NAME, name, mergeParent);
        if (mergeElem == null) {
            mergeElem = mergeParent.getOwnerDocument().createElement(elem.getNodeName());
            initData(mergeElem, elem);
            mergeParent.appendChild(mergeElem);
        } else {
            ValidationContext mergeContext = (ValidationContext) mergeElem.getUserData(NODE_CONTEXT);
            IncludeConflictResolution resolution = currentContext.getConflictResolution();
            if (resolution == IncludeConflictResolution.IGNORE) {
                return;
            }

            if (resolution == IncludeConflictResolution.REPLACE) {
                Element oldElem = mergeElem;
                String oldElemName = oldElem.getNodeName() + "[" + name + "]";
                String replaceElemName = elem.getNodeName() + "[" + name + "]";
                DocumentLocation location = DocumentLocation.createDocumentLocation(currentContext.getURI(), elem);
                DocumentLocation mergeLocation = DocumentLocation.createDocumentLocation(mergeContext.getURI(), mergeElem);

                createMessage(NLS.bind(Messages.infoReplaceItem,
                                       new String[] { replaceElemName, location.getLocationString(), oldElemName, mergeLocation.getLocationString() }),
                              getTopLevelResource(),
                              Level.INFO, getErrorNode(elem));
                mergeElem = mergeParent.getOwnerDocument().createElement(elem.getNodeName());
                initData(mergeElem, elem);
                mergeParent.replaceChild(mergeElem, oldElem);
            }
        }

        CMElementDeclaration elemDecl = SchemaUtil.getElement(parentDecl, elem.getNodeName());
        if (elemDecl == null) {
            // This should not happen
            if (Trace.ENABLED) {
                Trace.logError("Validator: could not get schema declaration for variable element", null);
            }
            return;
        }
        processAttributes(elem, mergeElem, elemDecl);
    }

    private void checkRemoteServerSecurePort(ValidationContext validationContext, WebSphereServerInfo webSphereServerInfo) {
        ConfigurationFile configFile = null;
        if (webSphereServerInfo != null && (configFile = webSphereServerInfo.getConfigRoot()) != null && configFile.getIFile().equals(getTopLevelResource())) {
            WebSphereServer webSphereServer = WebSphereUtil.getWebSphereServer(webSphereServerInfo);
            if (webSphereServer != null && !webSphereServer.isLocalHost()) {
                String serverSecurePort = webSphereServer.getServerSecurePort();
                if (serverSecurePort != null && !serverSecurePort.isEmpty()) {
                    try {
                        List<Integer> allSecurePorts = validationContext.getConfigFile().getAllPorts(true);
                        if (!allSecurePorts.contains(new Integer(serverSecurePort))) {
                            String defaultSecurePort = Integer.toString(validationContext.getConfigFile().getHTTPSPort());
                            quickFixData.setValues(QuickFixType.REMOTE_SERVER_SECURE_PORT_MISMATCH, null, null, -1);
                            createMessage(NLS.bind(Messages.securePortMismatch, new String[] { defaultSecurePort, serverSecurePort }), getTopLevelResource(), Level.ERROR,
                                          validationContext.getConfigFile().getDocument().getDocumentElement(),
                                          quickFixData);
                        }
                    } catch (NumberFormatException numberFormatException) {
                        // do nothing
                    }
                }
            }
        }
    }

    /**
     * Process an element. Elements can override each other (the
     * last value seen for an attribute is used). Only informational messages
     * are given to indicate that something has been overridden. To facilitate
     * this a merged view of the element is created as the configuration file is
     * processed.
     */
    private void processGeneralElement(Element elem, Element mergeParent, CMElementDeclaration parentDecl) {
        CMElementDeclaration elemDecl = SchemaUtil.getElement(parentDecl, elem.getNodeName());
        checkElement(elem, elemDecl, parentDecl);

        // Don't process further if the element is not defined in the schema.
        if (elemDecl == null) {
            return;
        }

        CMDataType elemType = elemDecl.getDataType();
        if (elemType != null && ConfigVars.isAtomic(elemType)) {
            processAtomicElement(elem, mergeParent);
            // Atomics don't have attributes or children so just return
            return;
        }

        boolean hasId = elem.hasAttribute(Constants.FACTORY_ID);
        boolean singletonNested = !isTopLevelElem(elem) && isSingleton(elemDecl, parentDecl);
        Element mergeElem = null;
        if (singletonNested) {
            // The schema validator will raise an error as there should only be one child
            // so don't complain about any overriding (leave mergeElem set to null).
        } else if (hasId) {
            mergeElem = lookupElement(elem.getNodeName(), Constants.FACTORY_ID, elem.getAttribute(Constants.FACTORY_ID), mergeParent);
        } else {
            // If an element has an id attribute of type factoryTypeId, and the
            // attribute is missing, we treat the element as unique
            final CMAttributeDeclaration attrDecl = SchemaUtil.getAttr(elemDecl, Constants.FACTORY_ID);
            if (attrDecl != null) {
                final String attrType = ConfigUtils.getTypeName(attrDecl);
                if (attrType != null) {
                    hasId = Constants.FACTORY_TYPE_ID.equals(attrType);
                }
            }
            if (!hasId && Constants.SERVER_ELEMENT.equals(parentDecl.getElementName())) {
                mergeElem = lookupElement(elem.getNodeName(), mergeParent);
            }
        }

        if (mergeElem == null) {
            mergeElem = mergeParent.getOwnerDocument().createElement(elem.getNodeName());
            initData(mergeElem, elem);
            mergeParent.appendChild(mergeElem);
        } else {
            ValidationContext mergeContext = (ValidationContext) mergeElem.getUserData(NODE_CONTEXT);
            IncludeConflictResolution resolution = currentContext.getConflictResolution();
            if (resolution == IncludeConflictResolution.IGNORE) {
                return;
            }

            if (resolution == IncludeConflictResolution.REPLACE) {
                Element oldElem = mergeElem;
                String oldElemName = oldElem.getNodeName();
                String replaceElemName = elem.getNodeName();
                if (hasId) {
                    oldElemName = oldElemName + "[" + oldElem.getAttribute(Constants.FACTORY_ID) + "]";
                    replaceElemName = replaceElemName + "[" + elem.getAttribute(Constants.FACTORY_ID) + "]";
                }
                DocumentLocation location = DocumentLocation.createDocumentLocation(currentContext.getURI(), elem);
                DocumentLocation mergeLocation = DocumentLocation.createDocumentLocation(mergeContext.getURI(), mergeElem);

                createMessage(NLS.bind(Messages.infoReplaceItem,
                                       new String[] { replaceElemName, location.getLocationString(), oldElemName, mergeLocation.getLocationString() }),
                              getTopLevelResource(),
                              Level.INFO, getErrorNode(elem));
                mergeElem = mergeParent.getOwnerDocument().createElement(elem.getNodeName());
                initData(mergeElem, elem);
                mergeParent.replaceChild(mergeElem, oldElem);
            }
        }

        processAttributes(elem, mergeElem, elemDecl);
        for (Element child = DOMUtils.getFirstChildElement(elem); child != null; child = DOMUtils.getNextElement(child)) {
            processElement(child, mergeElem, elemDecl);
        }
    }

    private boolean isSingleton(CMElementDeclaration elemDecl, CMElementDeclaration parentDecl) {
        // Neither declaration should be null since the children are not processed
        // without a parent declaration.
        if (parentDecl.getContent() instanceof CMGroup) {
            int maxOccur = parentDecl.getContent().getMaxOccur();
            if (maxOccur == -1)
                return false;
        }
        int maxOccur = elemDecl.getMaxOccur();
        if (maxOccur == 1)
            return true;
        return false;
    }

    private void processAttributes(Element elem, Element mergeElem, CMElementDeclaration elemDecl) {
        NamedNodeMap attrs = elem.getAttributes();
        fillDeclaredAttributes(attrs);
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            processAttribute((Attr) attr, mergeElem, elemDecl, attributes);
        }
    }

    private void fillDeclaredAttributes(NamedNodeMap attrs) {
        attributes.clear();
        for (int i = 0; i < attrs.getLength(); i++) {
            attributes.add(attrs.item(i).getNodeName());
        }
    }

    private void processAtomicElement(Element elem, Element mergeParent) {
        if (Constants.FEATURE_MANAGER.equals(mergeParent.getNodeName()) && Constants.FEATURE.equals(elem.getNodeName())) {
            // Special handling for features
            processFeature(elem, mergeParent);
            return;
        }
        String content = DOMUtils.getTextContent(elem);
        NodeList children = mergeParent.getElementsByTagName(elem.getNodeName());
        Element mergeNode = null;
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            String text = DOMUtils.getTextContent(child);
            if (text != null && text.equals(content)) {
                ValidationContext mergeContext = (ValidationContext) child.getUserData(NODE_CONTEXT);
                mergeNode = child;
                if (emitMessage(mergeContext, currentContext)) {
                    if (currentContext.isDropin()) {
                        DocumentLocation location = DocumentLocation.createDocumentLocation(currentContext.getURI(), elem);
                        String line = location.getLine() != -1 ? String.valueOf(location.getLine()) : Messages.unknownMsg;
                        Node errorNode = (Node) mergeNode.getUserData(ORIG_NODE);
                        if (errorNode == null)
                            errorNode = topLevelContext.getDocument().getDocumentElement();
                        createMessage(NLS.bind(Messages.infoDuplicateItemDropin, new String[] { elem.getNodeName(), text, location.getLocationString(), line }),
                                      getTopLevelResource(), Level.INFO, errorNode);
                    } else {
                        createMessage(NLS.bind(Messages.infoDuplicateItem, new String[] { elem.getNodeName(), text }), getTopLevelResource(), Level.INFO, getErrorNode(elem));
                    }
                }
                setNodeData(mergeNode, elem);
            }
        }
        if (mergeNode == null) {
            Node clone = mergeParent.getOwnerDocument().importNode(elem, true);
            mergeParent.appendChild(clone);
            initData(clone, elem);
        }
    }

    private void processAttribute(Attr attr, Element mergeParent, CMElementDeclaration parentDecl, List<String> declaredAttributes) {
        String name = attr.getNodeName();
        String value = checkAttribute(attr, mergeParent, parentDecl, declaredAttributes);
        Node mergeAttr = mergeParent.getAttributeNode(name);
        if (mergeAttr == null) {
            mergeParent.setAttribute(name, value);
            mergeAttr = mergeParent.getAttributeNode(name);
        } else if (!value.equals(mergeAttr.getNodeValue())) {
            ValidationContext previousContext = (ValidationContext) mergeAttr.getUserData(NODE_CONTEXT);
            if (emitMessage(previousContext, currentContext)) {
                if (currentContext.isDropin()) {
                    DocumentLocation location = DocumentLocation.createDocumentLocation(currentContext.getURI(), attr);
                    String line = location.getLine() != -1 ? String.valueOf(location.getLine()) : Messages.unknownMsg;
                    Node errorNode = (Node) mergeAttr.getUserData(ORIG_NODE);
                    if (errorNode == null)
                        errorNode = topLevelContext.getDocument().getDocumentElement();
                    createMessage(NLS.bind(Messages.infoOverrideItemDropin,
                                           new String[] { name, mergeParent.getNodeName(), mergeAttr.getNodeValue(), value, location.getLocationString(), line }),
                                  getTopLevelResource(), Level.INFO, errorNode);
                } else {
                    createMessage(NLS.bind(Messages.infoOverrideItem, new String[] { name, mergeParent.getNodeName(), mergeAttr.getNodeValue(), value }), getTopLevelResource(),
                                  Level.INFO, getErrorNode(attr));
                }
            }
            mergeAttr.setNodeValue(value);
        }
        setNodeData(mergeAttr, attr);
    }

    private Element lookupElement(String name, Element parent) {
        return DOMUtils.getFirstChildElement(parent, name);
    }

    /**
     * Returns the first element child of the parent for the given name
     * where the ids match.
     */
    private Element lookupElement(String name, String idAttr, String id, Element parent) {
        for (Element elem = DOMUtils.getFirstChildElement(parent, name); elem != null; elem = DOMUtils.getNextElement(elem, name)) {
            if (id.equals(elem.getAttribute(idAttr))) {
                return elem;
            }
        }
        return null;
    }

    /**
     * Special handling for features.
     */
    private void processFeature(Element elem, Element mergeParent) {
        String content = DOMUtils.getTextContent(elem);
        String canonicalName = FeatureList.getCanonicalFeatureName(content, wsRuntime);
        if (isTopLevel()) {
            if (canonicalName != null) {
                if (FeatureList.isFeatureSuperseded(canonicalName, wsRuntime)) {
                    quickFixData.setValues(QuickFixType.SUPERSEDED_FEATURE, content, null, -1);
                    createMessage(NLS.bind(Messages.supersededFeature, content),
                                  getTopLevelResource(), Level.WARNING, elem,
                                  quickFixData);
                }
            } else {
                String bestMatch = matcher.getBestMatch(FeatureList.getFeatures(false, wsRuntime), content, null);
                if (bestMatch != null) {
                    quickFixData.setValues(QuickFixType.UNRECOGNIZED_FEATURE, bestMatch, null, -1);
                    createMessage(NLS.bind(Messages.unrecognizedFeature, content),
                                  getTopLevelResource(), Level.WARNING, elem,
                                  quickFixData);
                } else {
                    createMessage(NLS.bind(Messages.unrecognizedFeature, content),
                                  getTopLevelResource(), Level.WARNING, elem);
                }
            }
        }

        // Continue processing only if feature is valid
        if (canonicalName != null) {
            NodeList children = mergeParent.getElementsByTagName(elem.getNodeName());
            Element mergeNode = null;
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                String text = DOMUtils.getTextContent(child);
                if (text != null && text.equalsIgnoreCase(content)) {
                    mergeNode = child;
                    ValidationContext mergeContext = (ValidationContext) child.getUserData(NODE_CONTEXT);
                    if (mergeContext != null && isTopLevel(mergeContext) && isTopLevel(currentContext)) {
                        createMessage(NLS.bind(Messages.infoDuplicateItem, new String[] { elem.getNodeName(), content }), getTopLevelResource(), Level.INFO, getErrorNode(elem));
                    }
                    setNodeData(child, elem);
                    mergeNode = child;
                }
            }
            if (mergeNode == null) {
                Node clone = mergeParent.getOwnerDocument().importNode(elem, true);
                mergeParent.appendChild(clone);
                initData(clone, elem);
            }

            checkFeatureConflicts(canonicalName, elem);

        }
    }

    private void checkFeatureConflicts(String featureName, Element elem) {
        Element element = null;
        Set<FeatureConflict> featureConflicts = resolverResult.getFeatureConflicts();
        String featureSymbolicName = FeatureList.getFeatureSymbolicName(featureName, wsRuntime);
        if (featureSymbolicName != null) {
            // Create a temporary set so not looping through and removing at the same time
            Set<FeatureConflict> tmpConflictSet = new HashSet<FeatureConflict>(featureConflicts);
            for (FeatureConflict featureConflict : tmpConflictSet) {
                if (featureSymbolicName.equals(featureConflict.getDependencyChainA().get(0))
                    || featureSymbolicName.equals(featureConflict.getDependencyChainB().get(0))) {
                    //create marker without conflict location if both conflicts appear within same validation context.
                    String featureName1 = FeatureList.getPublicFeatureName(featureConflict.getConfiguredFeatureA(), wsRuntime);
                    String featureName2 = FeatureList.getPublicFeatureName(featureConflict.getConfiguredFeatureB(), wsRuntime);
                    //feature conflicts may not be in same order as expected. Check featureNames before location is determined
                    List<String> dependancyChainA = featureConflict.getDependencyChainA();
                    List<String> dependancyChainB = featureConflict.getDependencyChainB();

                    if (!featureName1.equalsIgnoreCase(featureName)) {
                        featureName2 = featureName1;
                        dependancyChainA = featureConflict.getDependencyChainB();
                        dependancyChainB = featureConflict.getDependencyChainA();

                    }
                    String conflictALoc = currentContext.getURI().getPath();
                    URI conflictBURI = getFeatureElementLoc(featureName2);
                    String conflictBLoc = null;
                    if (conflictBURI != null) {
                        conflictBLoc = conflictBURI.getPath();
                    }
                    //feature may not be a recognized feature
                    if (conflictALoc == null || conflictBLoc == null)
                        return;
                    quickFixData.setValues(QuickFixType.FEATURE_CONFLICT, featureSymbolicName, null, -1);
                    if (currentContext.getParent() == null && conflictALoc.equals(conflictBLoc)) {
                        createMessage(NLS.bind(Messages.featureConflict,
                                               RuntimeFeatureResolver.getDependencyChainString(dependancyChainA, wsRuntime),
                                               RuntimeFeatureResolver.getDependencyChainString(dependancyChainB, wsRuntime)),
                                      getTopLevelResource(), Level.WARNING, elem, quickFixData);
                    } else {

                        //Do not create messages for conflicts within same include file , if context is not top level. it will be handled in different validation call
                        if (currentContext.getParent() != null && conflictALoc.equals(conflictBLoc))
                            return;
                        //get common parent
                        ValidationContext commonParent = getCommonParent(currentContext, featureName2);
                        //if common parent is not the toplevel context , skip the validation. It will be handled in different call.
                        if (commonParent != topLevelContext)
                            return;
                        element = currentContext.getParent() != null ? commonParent.getCurrentInclude() : elem;

                        // Using DocumentLocation to get the workspace path
                        DocumentLocation location = DocumentLocation.createDocumentLocation(currentContext.getURI(), elem);
                        DocumentLocation configLocation = DocumentLocation.createDocumentLocation(conflictBURI, elem);

                        // for conflicts between Dropins marker should appear on the server element
                        if (currentContext.isDropin() && element == null) {
                            Element ele = commonParent.getDocument().getDocumentElement();
                            createMessage(NLS.bind(Messages.featureConflictWithLoc,
                                                   new String[] { RuntimeFeatureResolver.getDependencyChainString(dependancyChainA, wsRuntime),
                                                                  RuntimeFeatureResolver.getDependencyChainString(dependancyChainB, wsRuntime),
                                                                  location.getLocationString(), configLocation.getLocationString() }),
                                          commonParent.getResource(), Level.WARNING, ele, quickFixData);
                        } else {
                            createMessage(NLS.bind(Messages.featureConflictWithLoc,
                                                   new String[] { RuntimeFeatureResolver.getDependencyChainString(dependancyChainA, wsRuntime),
                                                                  RuntimeFeatureResolver.getDependencyChainString(dependancyChainB, wsRuntime),
                                                                  location.getLocationString(), configLocation.getLocationString() }),
                                          commonParent.getResource(), Level.WARNING, element, quickFixData);
                        }

                    }
                    featureConflicts.remove(featureConflict);
                    break;
                }
            }
        }
    }

    /**
     * Given a featureName, returns the config file location, that has the given feature.
     * It searches in all Included files from current top level context.
     *
     * @param featureName
     * @return
     */
    private URI getFeatureElementLoc(String featureName) {
        Element elem = featureMap.get(featureName);
        if (elem != null) {
            DocumentLocation location = (DocumentLocation) elem.getUserData(ConfigUtils.DOCUMENT_LOCATION_KEY);
            if (location != null) {
                return location.getURI();
            }
        }
        ConfigurationFile server = topLevelContext.getConfigFile();
        if (server == null)
            return null;
        if (server.hasFeature(featureName))
            return server.getURI();
        ConfigurationFile[] includes = server.getAllIncludedFiles();
        for (ConfigurationFile file : includes) {
            if (file.hasFeature(featureName)) {
                return file.getURI();
            }
        }
        return null;
    }

    /**
     * checks whether a Config file has a given feature. Search is performed in all included files.
     *
     * @param file
     * @param featureName
     * @return
     */
    private boolean hasFeature(ConfigurationFile file, String featureName) {
        if (file.hasFeature(featureName))
            return true;
        ConfigurationFile[] includes = file.getAllIncludedFiles();
        for (ConfigurationFile fyl : includes) {
            if (fyl.hasFeature(featureName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns common parent for current context and a given feature Name.
     *
     * @param context
     * @param featureName
     * @return
     */
    private ValidationContext getCommonParent(ValidationContext context, String featureName) {
        ValidationContext toplevl = context;
        ConfigurationFile configFile = toplevl.getConfigFile();
        while (configFile != null && !hasFeature(configFile, featureName)) {
            toplevl = toplevl.getParent();
            configFile = toplevl.getConfigFile();
        }
        return toplevl;
    }

    private void checkElement(Element elem, CMElementDeclaration elemDecl, CMElementDeclaration parentDecl) {
        if (!isTopLevel()) {
            return;
        }
        if (elemDecl == null) {
            String bestMatch = getBestElementNameMatch(elem, parentDecl);
            // Since the schema validation will flag this error, we only create
            // a marker if we find a best match.
            if (bestMatch != null) {
                quickFixData.setValues(QuickFixType.UNRECOGNIZED_ELEMENT, bestMatch, null, -1);
                createMessage(NLS.bind(Messages.unrecognizedElement, new String[] { elem.getNodeName() }),
                              getTopLevelResource(), Level.WARNING, elem, quickFixData);
            }
        } else {
            // Check if element is enabled by the features but only for top level nodes
            Node parent = elem.getParentNode();
            if (parent != null && Constants.SERVER_ELEMENT.equals(parent.getNodeName())) {
                List<String> containingFeatures = ConfigUtils.getFeaturesToEnable(elem.getNodeName(), features, wsRuntime);
                if (containingFeatures != null && !containingFeatures.isEmpty()) {
                    // Add a validation warning with a quick fix to add one of the possible enabling features
                    StringBuilder possibleFeatures = new StringBuilder();
                    for (String feature : containingFeatures) {
                        if (possibleFeatures.length() > 0)
                            possibleFeatures.append(",");
                        possibleFeatures.append(feature);
                    }

                    quickFixData.setValues(QuickFixType.UNAVAILABLE_ELEMENT, possibleFeatures.toString(), null, -1);
                    createMessage(NLS.bind(Messages.unavailableElement, new String[] { elem.getNodeName() }),
                                  getTopLevelResource(), Level.WARNING, elem, quickFixData);
                }
            }
        }
    }

    private String getBestElementNameMatch(Element elem, CMElementDeclaration parentDecl) {
        if (parentDecl == null) {
            return null;
        }
        return matcher.getBestMatch(parentDecl.getLocalElements(), elem.getNodeName(), null);
    }

    private String checkAttribute(Attr attr, Element mergeElem, CMElementDeclaration elemDecl, List<String> declaredAttributes) {
        String value = attr.getNodeValue();
        CMAttributeDeclaration attrDecl = SchemaUtil.getAttr(elemDecl, attr.getNodeName());
        String type = attrDecl != null ? ConfigUtils.getTypeName(attrDecl) : null;

        boolean hasVarRef = ConfigVarsUtils.containsReference(value);
        ConfigVars vars = globalVars;
        // Only gather up the local variables if the value needs resolution
        if (hasVarRef) {
            vars = new LocalConfigVars(globalVars);
            ConfigUtils.getLocalVariables(attr.getOwnerElement(), attr.getName(), currentContext.getURI(), vars);
        }
        vars.resolve(value, resolvedInfo, type);
        String resolvedValue = resolvedInfo.getResolvedValue();

        if (isTopLevel()) {
            if (attrDecl == null) {
                final String elemName = mergeElem.getNodeName();
                final String attrName = attr.getNodeName();
                if (ignoreMatcher == null || !ignoreMatcher.isIgnoreAttribute(getTopLevelResource(), elemName, attrName)) {
                    // Check if the element has extra properties.  If it does, only give an informational if
                    // there is an attribute defined that has a close name match.  Otherwise give a warning.
                    boolean hasExtraProperties = SchemaUtil.hasExtraProperties(elemDecl);
                    String bestMatch = getBestPropertyNameMatch(attr, elemDecl, declaredAttributes);
                    if (hasExtraProperties) {
                        if (bestMatch != null) {
                            quickFixData.setValues(QuickFixType.UNRECOGNIZED_PROPERTY, bestMatch, null, -1);
                            createMessage(NLS.bind(Messages.unrecognizedProperty,
                                                   new String[] { attrName, elemName }),
                                          getTopLevelResource(), Level.INFO, attr, quickFixData);
                        }
                    } else {
                        quickFixData.setValues(QuickFixType.UNRECOGNIZED_PROPERTY, bestMatch, null, -1);
                        createMessage(NLS.bind(Messages.unrecognizedProperty,
                                               new String[] { attrName, elemName }),
                                      getTopLevelResource(), Level.WARNING, attr, quickFixData);
                    }
                }
                return resolvedValue;
            }

            if (resolvedValue.isEmpty() && attrDecl.getUsage() == CMAttributeDeclaration.REQUIRED) {
                createMessage(NLS.bind(Messages.emptyRequiredAttribute, new String[] { attr.getNodeName(), mergeElem.getNodeName() }), getTopLevelResource(), Level.WARNING, attr);
            }

            if (Constants.PASSWORD_TYPE.equals(type) || Constants.PASSWORD_HASH_TYPE.equals(type)) {
                if (!value.isEmpty()) {
                    int code = ConfigUtils.validatePassword(value, type, wsRuntime);
                    if (code == ConfigUtils.PASSWORD_PLAIN_TEXT) {
                        quickFixData.setValues(QuickFixType.PLAIN_TEXT_PASSWORD, null, null, -1);
                        createMessage(Messages.warningPlainTextPassword, getTopLevelResource(), Level.WARNING, attr, quickFixData);
                    } else if (code == ConfigUtils.PASSWORD_NOT_SUPPORT_AES) {
                        createMessage(Messages.warningAESEncryptedPasswordNotSupported, getTopLevelResource(), Level.WARNING, attr);
                    } else if (code == ConfigUtils.PASSWORD_NOT_SUPPORT_HASH) {
                        createMessage(Messages.warningHashEncodedPasswordNotSupported, getTopLevelResource(), Level.WARNING, attr);
                    } else if (code == ConfigUtils.PASSWORD_NOT_SUPPORT_CUSTOM && ConfigUtils.getEncryptionAlgorithm(value) != null) {
                        createMessage(NLS.bind(Messages.warningCustomEncryptedPasswordNotSupported, ConfigUtils.getEncryptionAlgorithm(value)), getTopLevelResource(),
                                      Level.WARNING, attr);
                    }
                }
            }

            if (ConfigVars.getTypeSet(type) == ConfigVars.REFERENCE_TYPES) {
                // Look for id/ref errors (id not defined, duplicate id in a reference list).
                // Don't try to quick fix in the case of a variable because this could
                // get complicated (the variable might be used elsewhere).
                String[] references = SchemaUtil.getReferences(attrDecl);
                if (references.length > 0) {
                    Set<String> idSet = new HashSet<String>();
                    for (String reference : references) {
                        Set<String> set = ids.get(reference);
                        if (set != null)
                            idSet.addAll(set);
                    }
                    String[] items = value.split("[, ]+");
                    Set<String> itemSet = new HashSet<String>();
                    for (int i = 0; i < items.length; i++) {
                        String item = items[i];
                        String varName = ConfigVarsUtils.getVariableName(item);
                        if (varName != null) {
                            String varValue = vars.getValue(varName);
                            if (varValue != null) {
                                String[] varItems = varValue.split("[, ]+");
                                for (String varItem : varItems) {
                                    // Don't try to do quick fixes for items in a variable.
                                    checkFactoryRef(varItem, idSet, references, itemSet, false, attr, i);
                                    itemSet.add(varItem);
                                }
                            }
                        } else {
                            checkFactoryRef(item, idSet, references, itemSet, true, attr, i);
                            itemSet.add(item);
                        }
                    }

                    // Also check that for a singleton ref (pidType) that there is no nested
                    // element as well since can't have both.
                    if (Constants.PID_TYPE.equals(type) && !value.isEmpty()) {
                        for (String reference : references) {
                            NodeList nested = attr.getOwnerElement().getElementsByTagName(reference);
                            if (nested != null && nested.getLength() > 0) {
                                createMessage(NLS.bind(Messages.singlePidRefAndNested, new String[] { mergeElem.getNodeName(), attr.getNodeName(), reference }),
                                              getTopLevelResource(), Level.WARNING, attr);
                                break;
                            }
                        }
                    }
                }
            }

            if (!resolvedInfo.isFullyResolved()) {
                UndefinedReference[] refs = resolvedInfo.getUndefinedReferences();
                for (UndefinedReference ref : refs) {
                    // Don't complain about unresolved predefined variables
                    if (!vars.isPredefinedVar(ref.getReferenceName())) {
                        final String bestMatch = matcher.getBestMatch(vars, ref.getReferenceName(), type);
                        quickFixData.setValues(QuickFixType.UNDEFINED_VARIABLE, bestMatch, ref.getReferenceName(), ref.getReferenceOffset());
                        createMessage(NLS.bind(Messages.unresolvedPropertyValue, new String[] { attr.getNodeName(), mergeElem.getNodeName(), ref.getReferenceName() }),
                                      getTopLevelResource(), Level.WARNING, attr, quickFixData);
                    }
                }
            }

            // Obtain expression operand errors (if any)
            ExpressionOperandError expressionLeftOperandError = resolvedInfo.getExpressionLeftOperandError();
            ExpressionOperandError expressionRightOperandError = resolvedInfo.getExpressionRightOperandError();

            if (resolvedInfo.isInvalidExpression()) {
                createMessage(NLS.bind(Messages.invalidVariableExpression, new String[] { value, attr.getNodeName(), mergeElem.getNodeName(), vars.getTypeName(type) }),
                              getTopLevelResource(), Level.WARNING, attr);
            } else if (expressionLeftOperandError != null || expressionRightOperandError != null) {

                // Left expression operand
                if (expressionLeftOperandError != null) {
                    switch (resolvedInfo.getExpressionLeftOperandError()) {
                        case MISSING:
                            createMessage(NLS.bind(Messages.expressionMissingLeftOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                          getTopLevelResource(), Level.WARNING, attr);
                            break;
                        case UNDEFINED:
                            Expression expression = new Expression(value);
                            String leftOperand = expression.getLeftOperand();
                            int index = 0;
                            if (leftOperand.startsWith("${")) {
                                index = 2;
                                leftOperand = leftOperand.substring(2);
                            }
                            if (!vars.isPredefinedVar(leftOperand)) {
                                final String bestMatch = matcher.getBestMatch(vars, leftOperand, type);
                                quickFixData.setValues(QuickFixType.UNDEFINED_VARIABLE, bestMatch, leftOperand, index);
                                createMessage(NLS.bind(Messages.expressionUndefinedLeftOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                              getTopLevelResource(), Level.WARNING, attr, quickFixData);
                            }
                            break;
                        case INVALID_VALUE:
                            createMessage(NLS.bind(Messages.expressionInvalidLeftOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                          getTopLevelResource(), Level.WARNING, attr);
                            break;
                    }
                }

                // Right expression operand
                if (expressionRightOperandError != null) {
                    switch (resolvedInfo.getExpressionRightOperandError()) {
                        case MISSING:
                            createMessage(NLS.bind(Messages.expressionMissingRightOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                          getTopLevelResource(), Level.WARNING, attr);
                            break;
                        case UNDEFINED:
                            Expression expression = new Expression(value);
                            String rightOperand = expression.getRightOperand();
                            if (rightOperand.endsWith("}")) {
                                rightOperand = rightOperand.substring(0, rightOperand.length() - 1);
                            }
                            if (!vars.isPredefinedVar(rightOperand)) {
                                final String bestMatch = matcher.getBestMatch(vars, rightOperand, type);
                                int index = value.indexOf(expression.getOperator().getSymbol()) + 1;
                                quickFixData.setValues(QuickFixType.UNDEFINED_VARIABLE, bestMatch, rightOperand, index);

                                createMessage(NLS.bind(Messages.expressionUndefinedRightOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                              getTopLevelResource(), Level.WARNING, attr, quickFixData);
                            }
                            break;
                        case INVALID_VALUE:
                            createMessage(NLS.bind(Messages.expressionInvalidRightOperand, new String[] { value, attr.getNodeName(), mergeElem.getNodeName() }),
                                          getTopLevelResource(), Level.WARNING, attr);
                            break;
                    }
                }
            } else if (resolvedInfo.isTypeMismatch()) {
                createMessage(NLS.bind(Messages.incorrectVariableReferenceType, new String[] { value, attr.getNodeName(), mergeElem.getNodeName(), vars.getTypeName(type) }),
                              getTopLevelResource(), Level.WARNING, attr);
            } else if (resolvedInfo.isFullyResolved() && (ConfigVars.getTypeSet(type) == ConfigVars.DURATION_TYPES)) {
                // Special check for duration types since they are just specified
                // as 'string' type in the schema.  Must either be a number or match
                // the duration type specification.
                try {
                    Long.valueOf(resolvedValue);
                } catch (NumberFormatException e) {
                    if (!ConfigVars.isDurationType(resolvedValue)) {
                        createMessage(NLS.bind(Messages.invalidValue, new String[] { resolvedValue, attr.getNodeName(), mergeElem.getNodeName(), vars.getTypeName(type) }),
                                      getTopLevelResource(), Level.WARNING, attr);
                    }
                }
            } else if (resolvedInfo.isFullyResolved() && hasVarRef) {
                // Check that the value is valid for the type.  Only do this if there are variable
                // references since the schema validator should catch this otherwise.
                XSDTypeDefinition typeDef = SchemaUtil.getTypeDefinitionFromSchema(attrDecl);
                if (typeDef instanceof XSDSimpleTypeDefinition) {
                    XSDSimpleTypeDefinition simpleTypeDef = (XSDSimpleTypeDefinition) typeDef;
                    if (!simpleTypeDef.isValidLiteral(resolvedValue)) {
                        createMessage(NLS.bind(Messages.invalidValueNoType, new String[] { resolvedValue, attr.getNodeName(), mergeElem.getNodeName() }),
                                      getTopLevelResource(), Level.WARNING, attr);
                    }
                }
            }

        }

        return resolvedValue;

    }

    private void checkFactoryRef(String id, Set<String> idSet, String[] references, Set<String> itemSet, boolean doQuickFix, Node node, int index) {
        if (idSet == null || !idSet.contains(id)) {
            String bestMatch = null;
            if (doQuickFix && idSet != null) {
                bestMatch = matcher.getBestMatch(idSet, id, null);
            }
            String messageKey = references.length > 1 ? Messages.factoryIdNotFoundMulti : Messages.factoryIdNotFound;
            String message = NLS.bind(messageKey, new String[] { formatList(references), id });
            if (bestMatch != null) {
                quickFixData.setValues(QuickFixType.FACTORY_ID_NOT_FOUND, bestMatch, id, index);
                createMessage(message, getTopLevelResource(), Level.WARNING, node, quickFixData);
            } else {
                createMessage(message, getTopLevelResource(), Level.WARNING, node);
            }
        }
        if (itemSet.contains(id)) {
            if (doQuickFix) {
                quickFixData.setValues(QuickFixType.DUPLICATE_FACTORY_ID, null, id, index);
                createMessage(NLS.bind(Messages.duplicateFactoryId, id), getTopLevelResource(),
                              Level.WARNING, node, quickFixData);
            } else {
                createMessage(NLS.bind(Messages.duplicateFactoryId, id),
                              getTopLevelResource(), Level.WARNING, node);
            }
        }
    }

    private String formatList(String[] list) {
        StringBuilder builder = new StringBuilder();
        for (String str : list) {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append(str);
        }
        return builder.toString();
    }

    private String getBestPropertyNameMatch(Node attr, CMElementDeclaration parentDecl, List<String> declaredAttributes) {
        if (parentDecl == null) {
            return null;
        }
        return matcher.getBestMatch(parentDecl.getAttributes(), attr.getNodeName(), declaredAttributes);
    }

    private boolean isTopLevel() {
        return currentContext.getParent() == null;
    }

    private boolean isTopLevel(ValidationContext context) {
        return context.getParent() == null;
    }

    private boolean isTopLevelElem(Element elem) {
        Node node = elem.getParentNode();
        return (node == null || Constants.SERVER_ELEMENT.equals(node.getNodeName()));
    }

    private void initData(Node mergeNode, Node node) {
        // Save original node for the top level only since these
        // are the only nodes against which errors should be reported.
        // Also, saving other nodes might cause problems since DOM
        // is not thread safe.
        if (isTopLevel()) {
            mergeNode.setUserData(ORIG_NODE, node, null);
        }
        mergeNode.setUserData(NODE_CONTEXT, currentContext, null);
    }

    private void setNodeData(Node mergeNode, Node node) {
        // Only save top level nodes as those are the only nodes
        // for which errors should be reported.  Also saving other
        // nodes might cause problems since DOM is not thread safe.
        if (isTopLevel()) {
            mergeNode.setUserData(ORIG_NODE, node, null);
        } else {
            mergeNode.setUserData(ORIG_NODE, null, null);
        }
        mergeNode.setUserData(NODE_CONTEXT, currentContext, null);
    }

    private boolean emitMessage(ValidationContext previous, ValidationContext current) {
        if (previous == null) {
            // Something went wrong, return true to be on the safe side.
            Trace.logError("Previous context is null.", null);
            return true;
        }
        // If one of the two is at the top level then the message should
        // be emitted.
        if (isTopLevel(previous) || isTopLevel(current)) {
            return true;
        }
        // Check if these are the same context (same include file) in which
        // case it is an error in the include and the message should not
        // be emitted.
        if (previous == current) {
            return false;
        }
        // Check if the two were included by the same include file in
        // which case it is an error on the include, not the main document.
        ValidationContext firstInclude = getTopLevelInclude(previous);
        ValidationContext secondInclude = getTopLevelInclude(current);
        if (firstInclude != null && secondInclude != null && firstInclude == secondInclude) {
            return false;
        }
        return true;
    }

    private ValidationContext getTopLevelInclude(ValidationContext context) {
        // Determine the top level include that caused this one to be
        // included.  So if A->B->C->D, then B is the top level include for
        // B, C, and D.  Used to determine if two include files have a
        // common include parent.
        ValidationContext firstInclude = null;
        for (ValidationContext parent = context; parent != null; parent = parent.getParent()) {
            if (parent.getParent() != null) {
                firstInclude = parent;
            }
        }
        return firstInclude;
    }

    private Node getErrorNode(Node node) {
        // If at the top level then report the error on the node itself.
        if (isTopLevel()) {
            return node;
        }
        // If not at the top level then report the error against the
        // current include element of the main document.
        return topLevelContext.getCurrentInclude();
    }

    private IResource getTopLevelResource() {
        return topLevelContext.getResource();
    }

    protected String getTopLevelLocation() {
        URI uri = topLevelContext.getURI();
        if (uri != null) {
            return uri.toString();
        }
        return null;
    }

    private String getFilePathString(ValidationContext context) {
        String path;
        IResource resource = context.getResource();
        if (resource != null) {
            path = resource.getFullPath().toString();
        } else {
            URI uri = context.getURI();
            if (uri.getScheme().equals("file")) {
                path = (new Path(uri.getPath())).toOSString();
            } else {
                path = uri.toString();
            }
        }
        return path;
    }

    protected void createMessage(String text, IResource resource, Level level, Node node) {
        quickFixData.setValues(QuickFixType.NONE, null, null, -1);
        createMessage(text, resource, level, node, quickFixData);
    }

    protected abstract void createMessage(String text, IResource resource, Level level, Node node, QuickFixData fixData);

    public static class QuickFixData {
        private QuickFixType fixType;
        private String bestMatch;
        private String undefinedReferenceName;
        private int undefinedReferenceOffset;
        private final HashMap<String, Object> attributes = new HashMap<String, Object>();

        protected void setValues(QuickFixType type, String match, String refName, int refOffset) {
            fixType = type;
            bestMatch = match;
            undefinedReferenceName = refName;
            undefinedReferenceOffset = refOffset;
            attributes.clear();
        }

        public QuickFixType getFixType() {
            return fixType;
        }

        public String getBestMatch() {
            return bestMatch;
        }

        public String getUndefinedReferenceName() {
            return undefinedReferenceName;
        }

        public int getUndefinedReferenceOffset() {
            return undefinedReferenceOffset;
        }

        public void setAttribute(String attr, Object value) {
            attributes.put(attr, value);
        }

        public HashMap<String, Object> getAttributes() {
            return attributes;
        }
    }
}

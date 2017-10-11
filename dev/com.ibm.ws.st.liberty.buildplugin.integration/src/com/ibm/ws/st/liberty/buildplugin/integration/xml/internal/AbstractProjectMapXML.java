/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.ws.st.liberty.buildplugin.integration.xml.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping.ProjectMapping;

public abstract class AbstractProjectMapXML extends XMLDocumentBuilder {

    public static final String ROOT_ELEMENT = "mappings";
    private static final String PROJECT_ELEMENT = "project";
    private static final String SERVER_ID_ELEMENT = "serverId";
    private static final String RUNTIME_ID_ELEMENT = "runtimeId";
    private static final String NAME_ATTR = "name";
    private static final String IGNORED_ATTR = "ignored";
    public static final String PROJECT_MAP_FILE_NAME = "mavenProjectMapping.xml";

    private final File xmlFile;

    protected AbstractProjectMapXML(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public synchronized void unmarshall(ConcurrentHashMap<String, ProjectMapping> trackedLibertyMavenProjects,
                                        Set<String> ignoredProjects) throws ParserConfigurationException, SAXException, IOException {
        readDocument(xmlFile);
        fillMap(trackedLibertyMavenProjects, ignoredProjects);
    }

    private void fillMap(ConcurrentHashMap<String, ProjectMapping> trackedLibertyMavenProjects, Set<String> ignoredProjects) {
        Element root = doc.getDocumentElement();
        NodeList mappings = root.getChildNodes();

        // iterate through project mappings
        for (int i = 0; i < mappings.getLength(); i++) {
            String projectName = null;
            String serverID = null;
            String runtimeID = null;
            boolean isIgnored = false;

            Node projNode = mappings.item(i);
            if (projNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            // project node
            Element project = (Element) projNode;
            projectName = project.getAttribute(NAME_ATTR).trim();

            if (invalidValue(projectName))
                continue;

            String isIgnoredValue = project.getAttribute(IGNORED_ATTR).trim();
            if (isIgnoredValue != null) {
                isIgnored = Boolean.parseBoolean(isIgnoredValue);
            }

            if (isIgnored) {
                ignoredProjects.add(projectName);
                return;
            } else if (ignoredProjects.contains(projectName))
                ignoredProjects.remove(projectName);

            // get child nodes: server and runtime
            NodeList projectChildren = project.getChildNodes();
            for (int j = 0; j < projectChildren.getLength(); j++) {
                Node childNode = projectChildren.item(j);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element c = (Element) childNode;
                    if (SERVER_ID_ELEMENT.equals(c.getNodeName())) {
                        serverID = c.getTextContent().trim();
                        if (invalidValue(serverID)) {
                            Trace.logError("Invalid server ID found in project mapping metadata file: " + serverID, null);
                            serverID = null;
                        }
                    } else if (RUNTIME_ID_ELEMENT.equals(c.getNodeName())) {
                        runtimeID = c.getTextContent().trim();
                        if (invalidValue(runtimeID)) {
                            Trace.logError("Invalid runtime ID found in project mapping metadata file: " + runtimeID, null);
                            runtimeID = null;
                        }
                    }
                }
            } // project children loop

            trackedLibertyMavenProjects.put(projectName, new ProjectMapping(runtimeID, serverID));

        } // root children loop
    }

    private boolean invalidValue(String value) {
        return value == null || value.isEmpty();
    }

    public synchronized void marshall(ConcurrentHashMap<String, ProjectMapping> trackedLibertyMavenProjects,
                                      Set<String> ignoredProjects) throws ParserConfigurationException, IOException, TransformerException {
        createNewDocument(ROOT_ELEMENT);
        marshallMap(trackedLibertyMavenProjects, ignoredProjects);
        writeXMLDocument(xmlFile);
    }

    private synchronized void marshallMap(ConcurrentHashMap<String, ProjectMapping> values, Set<String> ignoredProjects) {
        if (values != null) {
            for (Entry<String, ProjectMapping> entry : values.entrySet()) {
                Element child = doc.createElement(PROJECT_ELEMENT);
                child.setAttribute(NAME_ATTR, entry.getKey());
                createElement(child, entry.getValue());
                doc.getDocumentElement().appendChild(child);
            }
        }

        if (ignoredProjects != null) {
            for (String projectName : ignoredProjects) {
                Element child = doc.createElement(PROJECT_ELEMENT);
                child.setAttribute(NAME_ATTR, projectName);
                child.setAttribute(IGNORED_ATTR, Boolean.TRUE.toString());
                doc.getDocumentElement().appendChild(child);
            }
        }
    }

    private void createElement(Element elem, ProjectMapping value) {
        if (value == null) {
            return;
        }

        // create server element
        String serverID = value.getServerID();
        if (serverID != null) {
            createElement(elem, SERVER_ID_ELEMENT, serverID);
        }

        // create runtime element
        String runtimeID = value.getRuntimeID();
        if (runtimeID != null) {
            createElement(elem, RUNTIME_ID_ELEMENT, runtimeID);
        }
    }

    private void createElement(Element elem, String key, String value) {
        if (value == null) {
            return;
        }
        Element child = doc.createElement(key);
        child.appendChild(doc.createTextNode(value));
        elem.appendChild(child);
    }

}

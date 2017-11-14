/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.buildplugin.integration.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Constants;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;

/**
 * A single instance of this class can be used to build the LibertyBuildPluginConfiguration data model based on
 * DOM data and retrieve the model object.
 *
 * This Builder works for v1 config files and v2 config files with only one server element and one application element
 *
 */
public class DOMBasedLibertyBuildPluginConfigurationBuilder {

    private final LibertyBuildPluginConfiguration config;

    DOMBasedLibertyBuildPluginConfigurationBuilder(long lastModified) {
        config = new LibertyBuildPluginConfiguration(lastModified);
    }

    public void buildModel(Element rootElement) {
        checkChildElements(rootElement);
    }

    public LibertyBuildPluginConfiguration getModel() {
        return config;
    }

    /*
     * Check immediate element children of element and add them to the model
     */
    private void checkChildElements(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                addToModel((Element) child);
            }
        }
    }

    private void addToModel(Element elem) {
        if (elem == null)
            return;

        ConfigurationType type = null;
        try {
            type = ConfigurationType.valueOf(elem.getNodeName());
        } catch (Exception e) {
            Trace.logError(elem.getNodeName() + " is not a supported liberty configuration element and will be ignored.", null);
        }
        if (type == null)
            return;

        try {
            switch (type) {
                case activeBuildProfiles:
                    handleActiveBuildProfiles(elem);
                    break;
                case applications:
                    handleApplications(elem);
                    break;
                case bootstrapProperties:
                    handleBootstrapProperties(elem);
                    break;
                case jvmOptions:
                    handleJVMOptions(elem);
                    break;
                case projectCompileDependency:
                    handleProjectCompileDependencies(elem);
                    break;
                case servers:
                    handleServers(elem);
                    break;
                default: {
                    setValue(type, elem);
                }
            }
        } catch (Exception e) {
            Trace.logError("Failed to parse data for liberty configuration element: " + elem.getNodeName(), e);
        }
    }

    private void handleApplications(Element elem) {
        // TODO For multiple Applications support, simply call checkChildElements(elem);
        //       <applications>
        //         <application>
        NodeList children = elem.getChildNodes();
        int numOfChildren = children.getLength();
        for (int i = 0; i < numOfChildren; i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(Constants.APPLICATION_ELEMENT)) {
                checkChildElements((Element) child);
                break; // TODO for multiple application support.  Support first app for now.
            }
        }
    }

    private void handleServers(Element elem) {
        // TODO for multiple servers support should simply call this instead: checkChildElements(elem);
        NodeList children = elem.getChildNodes();
        int numOfChildren = children.getLength();
        for (int i = 0; i < numOfChildren; i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(Constants.SERVER_ELEMENT)) {
                checkChildElements((Element) child);
                break; // TODO only support the first server element
            }
        }
    }

    private void handleActiveBuildProfiles(Element elem) {
        NodeList children = elem.getChildNodes();
        ArrayList<String> profiles = new ArrayList<String>(5);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(Constants.PROFILE_ID)) {
                String profile = child.getTextContent().trim();
                if (profile != null && !profile.isEmpty())
                    profiles.add(profile);
            }
        }
        config.setActiveBuildProfiles(profiles);
    }

    private void handleBootstrapProperties(Element elem) {
        // TODO For multiple servers support, the bootstrap properties must be associated with each server.
        NodeList children = elem.getChildNodes();
        HashMap<String, String> props = new HashMap<String, String>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                String key = child.getNodeName();
                String prop = child.getTextContent().trim();
                if (key != null && !key.isEmpty() && prop != null && !prop.isEmpty())
                    props.put(key, prop);
            }
        }
        config.setBootstrapProperties(props);
    }

    private void handleJVMOptions(Element elem) {
        NodeList children = elem.getChildNodes();
        ArrayList<String> params = new ArrayList<String>(5);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(Constants.JVM_PARAM)) {
                String param = child.getTextContent().trim();
                if (param != null && !param.isEmpty())
                    params.add(param);
            }
        }
        config.setJvmOptions(params);
    }

    private void handleProjectCompileDependencies(Element elem) {
        String param = elem.getTextContent().trim();
        if (param != null && !param.isEmpty())
            config.addProjectCompileDependency(param);
    }

    private void setValue(ConfigurationType type, Element elem) {
        config.setConfigValue(type, elem.getTextContent().trim());
    }
}
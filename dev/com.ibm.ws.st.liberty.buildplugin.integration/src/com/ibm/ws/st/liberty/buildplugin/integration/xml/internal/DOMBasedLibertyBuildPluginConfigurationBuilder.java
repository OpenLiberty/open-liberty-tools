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
 *
 * A single instance of this class can be used to build the LibertyBuildPluginConfiguration data model based on
 * DOM data and retrieve the model object.
 *
 */
public class DOMBasedLibertyBuildPluginConfigurationBuilder {

    private final LibertyBuildPluginConfiguration config;

    DOMBasedLibertyBuildPluginConfigurationBuilder(long lastModified) {
        config = new LibertyBuildPluginConfiguration(lastModified);
    }

    public void addToModel(Element elem) {
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
                case bootstrapProperties:
                    handleBootstrapProperties(elem);
                    break;
                case jvmOptions:
                    handleJVMOptions(elem);
                    break;
                case projectCompileDependency:
                    handleProjectCompileDependencies(elem);
                    break;
                default: {
                    setValue(type, elem);
                }
            }
        } catch (Exception e) {
            Trace.logError("Failed to parse data for liberty configuration element: " + elem.getNodeName(), e);
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

    public LibertyBuildPluginConfiguration getModel() {
        return config;
    }

}
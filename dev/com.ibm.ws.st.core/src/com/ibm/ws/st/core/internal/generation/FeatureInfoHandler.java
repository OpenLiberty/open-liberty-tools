/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.st.core.internal.config.FeatureList.FeatureMapType;
import com.ibm.ws.st.core.internal.generation.Feature.FeatureType;

public class FeatureInfoHandler extends DefaultHandler {
    private static final String FEATURE = "feature";
    private static final String FEATURE_INFO = "featureInfo";
    private static final String FEATURE_INFO_CORE = "core";
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "displayName";
    private static final String DESCRIPTION = "description";
    private static final String CATEGORY = "category";
    private static final String API_JAR = "apiJar";
    private static final String SPI_JAR = "spiJar";
    private static final String API_PACKAGE = "apiPackage";
    private static final String SPI_PACKAGE = "spiPackage";
    private static final String ENABLES = "enables";
    private static final String SUPERSEDED = "superseded";
    private static final String SUPERSEDED_BY = "supersededBy";
    private static final String CONFIG_ELEMENT = "configElement";
    private static final String SYMBOLIC_NAME = "symbolicName";
    private static final String SINGLETON = "singleton";
    private static final String INCLUDE = "include";
    private static final String TOLERATES = "tolerates";
    private static final String PROTECTED_FEATURE = "protectedFeature";
    private static final String PRIVATE_FEATURE = "privateFeature";
    private static final String AUTO_FEATURE = "autoFeature";
    private static final String KERNEL_FEATURE = "kernelFeature";
    private static final String AUTO_PROVISION = "autoProvision";
    private static final String PROCESS_TYPE = "processType";

    private final HashMap<FeatureMapType, HashMap<String, Feature>> featureMaps;
    private Feature current = null;
    private StringBuilder chars = null;
    private String featureInfoName = null;

    public FeatureInfoHandler(HashMap<FeatureMapType, HashMap<String, Feature>> featureMaps) {
        this.featureMaps = featureMaps;
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes attributes) {
        // We only need to use the featureInfo name if it is not "core".
        if (qname.equals(FEATURE_INFO)) {
            String nameAttr = attributes.getValue("", NAME);
            featureInfoName = nameAttr == null || nameAttr.equals(FEATURE_INFO_CORE) ? null : nameAttr;
        } else if (qname.equals(FEATURE)) {
            createSet(attributes, FeatureType.PUBLIC);
        } else if (qname.equals(PROTECTED_FEATURE)) {
            createSet(attributes, FeatureType.PROTECTED);
        } else if (qname.equals(PRIVATE_FEATURE)) {
            createSet(attributes, FeatureType.PRIVATE);
        } else if (qname.equals(AUTO_FEATURE)) {
            createSet(attributes, FeatureType.AUTO);
        } else if (qname.equals(KERNEL_FEATURE)) {
            createSet(attributes, FeatureType.KERNEL);
        } else if (qname.equals(INCLUDE)) {
            processInclude(attributes);
        } else if (qname.equals(SYMBOLIC_NAME) || qname.equals(PROCESS_TYPE) || qname.equals(DISPLAY_NAME) || qname.equals(DESCRIPTION)
                   || qname.equals(CATEGORY) || qname.equals(ENABLES) || qname.equals(API_JAR) || qname.equals(API_PACKAGE) || qname.equals(SPI_JAR)
                   || qname.equals(SPI_PACKAGE) || qname.equals(SUPERSEDED) || qname.equals(SUPERSEDED_BY) || qname.equals(CONFIG_ELEMENT)
                   || qname.equals(SINGLETON) || qname.equals(AUTO_PROVISION)) {
            chars = new StringBuilder();
        }
    }

    private void createSet(Attributes attributes, FeatureType featureType) {
        String name = attributes.getValue("", NAME);
        if (name != null && !name.equals("")) {
            // If the featureInfo name is set and it's not the "core" set of features then
            // we should prefix the feature name with the featureInfo name.
            if (featureInfoName != null && !featureInfoName.equals("")) {
                name = featureInfoName + ":" + name;
            }
        }
        current = new Feature(name, featureInfoName, featureType);
    }

    private void processInclude(Attributes attributes) {
        if (current != null) {
            String symbolicName = attributes.getValue("", SYMBOLIC_NAME);
            if (symbolicName != null && !symbolicName.isEmpty()) {
                List<String> tolerates = new ArrayList<String>();
                String toleratesValue = attributes.getValue("", TOLERATES);
                if (toleratesValue != null && !toleratesValue.isEmpty()) {
                    for (String version : toleratesValue.split(",")) {
                        tolerates.add(version.trim());
                    }
                }
                Map<String, List<String>> includes = current.getIncludes();
                includes.put(symbolicName, tolerates);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qname) {
        if (qname.equals(SYMBOLIC_NAME)) {
            if (current != null) {
                current.setSymbolicName(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(PROCESS_TYPE)) {
            if (current != null) {
                current.addProcessType(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(DISPLAY_NAME)) {
            if (current != null) {
                current.setDisplayName(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(DESCRIPTION)) {
            if (current != null) {
                current.setDescription(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(CATEGORY)) {
            if (current != null) {
                current.addCategoryElements(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(ENABLES)) {
            if (current != null) {
                current.addEnables(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(API_JAR)) {
            if (current != null) {
                current.addAPIJar(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(API_PACKAGE)) {
            if (current != null) {
                current.addAPIPackage(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(SPI_JAR)) {
            if (current != null) {
                current.addSPIJar(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(SPI_PACKAGE)) {
            if (current != null) {
                current.addSPIPackage(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(SUPERSEDED)) {
            if (current != null) {
                current.setSuperseded("true".equals(chars.toString().trim()));
            }
            chars = null;
        } else if (qname.equals(SINGLETON)) {
            if (current != null) {
                current.setSingleton("true".equals(chars.toString().trim()));
            }
            chars = null;
        } else if (qname.equals(SUPERSEDED_BY)) {
            if (current != null) {
                current.addSupersededBy(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(CONFIG_ELEMENT)) {
            if (current != null) {
                current.addConfigElement(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(AUTO_PROVISION)) {
            if (current != null) {
                current.addAutoProvision(chars.toString().trim());
            }
            chars = null;
        } else if (qname.equals(FEATURE)) {
            // Don't add the client only features and Jakarta EE9 features to the map. For Client only features, processType is set to CLIENT
            if (!current.getProcessType().equals(ProcessType.CLIENT) && !current.isJakartaEE9Feature()) {
                featureMaps.get(FeatureMapType.PUBLIC_FEATURES_KEYED_BY_NAME).put(current.getName(), current);
                // Older feature lists may not have a symbolic name
                if (current.getSymbolicName() != null) {
                    featureMaps.get(FeatureMapType.ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME).put(current.getSymbolicName(), current);
                }
            }
            current = null;
        } else if (qname.equals(PROTECTED_FEATURE) || qname.equals(PRIVATE_FEATURE) || qname.equals(AUTO_FEATURE) || qname.equals(KERNEL_FEATURE)) {
            // Older feature lists may not have symbolic name
            if (current.getSymbolicName() != null) {
                featureMaps.get(FeatureMapType.ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME).put(current.getSymbolicName(), current);
            }
            current = null;
        } else if (qname.equals(FEATURE_INFO)) {
            featureInfoName = null;
        }
    }

    @Override
    public void characters(char[] c, int start, int length) {
        if (chars != null) {
            chars.append(c, start, length);
        }
    }

    public static HashMap<FeatureMapType, HashMap<String, Feature>> parseFeatureListXML(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        HashMap<FeatureMapType, HashMap<String, Feature>> map = new HashMap<FeatureMapType, HashMap<String, Feature>>();
        map.put(FeatureMapType.PUBLIC_FEATURES_KEYED_BY_NAME, new HashMap<String, Feature>());
        map.put(FeatureMapType.ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME, new HashMap<String, Feature>());
        FeatureInfoHandler handler = new FeatureInfoHandler(map);
        parser.parse(is, handler);
        return map;
    }
}

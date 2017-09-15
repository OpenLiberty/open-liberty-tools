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
package com.ibm.ws.st.core.internal.config.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.config.DOMUtils;

/**
 * A representation of the validation ignore filter settings
 */
public class ValidationFilterSettings implements FilterItem {

    private final HashMap<String, ConfigFileFilterItem> configFileMap = new HashMap<String, ConfigFileFilterItem>();
    private final IProject project;
    private ValidationFilterListener listener;
    private Document document;

    // Public constructors
    public ValidationFilterSettings(IProject project) {
        this.project = project;
        load();
    }

    public void refresh() {
        if (configFileMap.size() > 0) {
            configFileMap.clear();
        }

        load();
    }

    private void load() {
        document = ValidationFilterUtil.loadSettings(project);
        if (document != null) {
            final Element docElement = document.getDocumentElement();
            for (Element child = DOMUtils.getFirstChildElement(docElement, Constants.ELEMENT_MATCH); child != null; child = DOMUtils.getNextElement(child,
                                                                                                                                                                    Constants.ELEMENT_MATCH)) {
                addConfigItem(child);
            }
        }
    }

    private void addConfigItem(Element matchElem) {
        final String pathString = getConfigPath(matchElem);
        ConfigFileFilterItem cfi = configFileMap.get(pathString);
        if (cfi == null) {
            cfi = new ConfigFileFilterItem(this, pathString);
            configFileMap.put(pathString, cfi);
        }
        addMatchItem(cfi, matchElem);
    }

    private void addMatchItem(ConfigFileFilterItem configItem, Element matchElem) {
        final String elemName = getMatchElementName(matchElem);
        final String attrName = getMatchAttributeName(matchElem);
        final String pattern = getIgnorePattern(matchElem);
        final MatchFilterItem matchItem = new MatchFilterItem(configItem, pattern, elemName, attrName, matchElem);
        configItem.addMatchItem(matchItem);
    }

    private String getConfigPath(Element matchElem) {
        final Element childElem = DOMUtils.getFirstChildElement(matchElem, Constants.ELEMENT_FILE);
        if (childElem != null) {
            final Attr attr = childElem.getAttributeNode(Constants.ATTRIBUTE_PATH);
            if (attr != null) {
                return attr.getValue();
            }
        }
        return Constants.MATCH_ALL;
    }

    private String getMatchAttributeName(Element matchElem) {
        final Element childElem = DOMUtils.getFirstChildElement(matchElem, Constants.ELEMENT_ATTRIBUTE);
        if (childElem != null) {
            final Attr attr = childElem.getAttributeNode(Constants.ATTRIBUTE_NAME);
            if (attr != null) {
                return attr.getValue();
            }
        }
        return Constants.MATCH_ALL;
    }

    private String getMatchElementName(Element matchElem) {
        final Element childElem = DOMUtils.getFirstChildElement(matchElem, Constants.ELEMENT_ELEMENT);
        if (childElem != null) {
            final Attr attr = childElem.getAttributeNode(Constants.ATTRIBUTE_NAME);
            if (attr != null) {
                return attr.getValue();
            }
        }
        return Constants.MATCH_ALL;
    }

    private String getIgnorePattern(Element matchElem) {
        final Element childElem = DOMUtils.getFirstChildElement(matchElem, Constants.ELEMENT_IGNORE);
        if (childElem != null) {
            final Attr attr = childElem.getAttributeNode(Constants.ATTRIBUTE_PATTERN);
            if (attr != null) {
                return attr.getValue();
            }
        }
        return "";
    }

    public void addFilterListener(ValidationFilterListener listener) {
        this.listener = listener;
    }

    public void removeListener(ValidationFilterListener listener) {
        this.listener = null;
    }

    public boolean removeFilter(FilterItem item) {
        // remove the filter according to its type
        if (item instanceof MatchFilterItem) {
            return removeMatchItem((MatchFilterItem) item);
        }

        if (item instanceof ConfigFileFilterItem) {
            return removeConfigFileItem((ConfigFileFilterItem) item);
        }

        return false;
    }

    private boolean removeMatchItem(MatchFilterItem matchItem) {
        // We might have removed the item as part of removing
        // the parent
        if (!matchItem.parent.containsMatchItem(matchItem)) {
            return true;
        }

        // Remove the match element from the DOM tree
        if (removeElement(matchItem.node)) {
            if (ValidationFilterUtil.saveSettings(project, document)) {
                // notify the listener of the change
                if (listener != null) {
                    listener.filterRemoved(matchItem);
                }

                // remove the item from the parent's list
                matchItem.parent.removeMatchItem(matchItem);

                // if the parent becomes empty, then we remove it from the
                // map, and we notify the listener
                if (!matchItem.parent.hasChildren()) {
                    configFileMap.remove(matchItem.parent.getPath());
                    if (listener != null) {
                        listener.filterRemoved(matchItem.parent);
                    }
                }
                refreshResource(matchItem.parent.getPath());
                return true;
            }
        }

        return false;
    }

    private boolean removeConfigFileItem(ConfigFileFilterItem configItem) {
        if (configFileMap.containsKey(configItem.getPath())) {
            MatchFilterItem[] matchItems = configItem.getMatchItems();
            for (MatchFilterItem item : matchItems) {
                if (!removeElement(item.node)) {
                    return false;
                }
            }

            configFileMap.remove(configItem.getPath());
            if (ValidationFilterUtil.saveSettings(project, document)) {
                // notify the listener of the change
                if (listener != null) {
                    listener.filterRemoved(configItem);
                }
                configItem.removeAllChildren();
                refreshResource(configItem.getPath());
                return true;
            }
        }
        return true;
    }

    private void refreshResource(String path) {
        if (path != null) {
            IFile file = project.getFile(path);
            try {
                file.touch(new NullProgressMonitor());
            } catch (CoreException e1) {
                // ignore for now
            }
        }
    }

    private boolean removeElement(Element elem) {
        if (elem.getParentNode() == null) {
            return true;
        }

        if (elem.getOwnerDocument() != document) {
            return false;
        }

        final Node parent = elem.getParentNode();
        Node sibling = elem.getNextSibling();
        try {
            if (sibling != null && sibling.getNodeType() == Node.TEXT_NODE) {
                parent.removeChild(sibling);
            }
            sibling = elem.getPreviousSibling();
            if (sibling != null && sibling.getNodeType() == Node.TEXT_NODE) {
                parent.removeChild(sibling);
            }
            parent.removeChild(elem);
        } catch (DOMException e) {
            Trace.logError("Failed to remove '" + elem.getNodeName() + "'", e);
            return false;
        }

        return true;
    }

    protected Set<ConfigFileFilterItem> getConfigFileItems() {
        return new HashSet<ConfigFileFilterItem>(configFileMap.values());
    }

    // FilterItem methods

    @Override
    public FilterItem getParent() {
        return null;
    }

    @Override
    public boolean hasChildren() {
        return configFileMap.size() > 0;
    }

    @Override
    public Object[] getChildren() {
        return configFileMap.values().toArray();
    }
}

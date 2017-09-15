/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.looseconfig;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.st.core.internal.Trace;

/**
 * Read the information from the .project file of an eclipse project.
 * Currently only reads in the linked resources.
 */
public class ProjectInfoHandler extends DefaultHandler {
    private static final String LINKED_RESOURCES = "linkedResources";
    private static final String LINK = "link";
    private static final String NAME = "name";

    private final IProject project;
    private final List<IResource> linkedResources;
    private State currentState = State.DESCRIPTION;
    private StringBuilder chars = null;

    private enum State {
        DESCRIPTION,
        LINKED_RESOURCES,
        LINK
    }

    public ProjectInfoHandler(IProject project, List<IResource> linkedResources) {
        this.project = project;
        this.linkedResources = linkedResources;
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes attributes) {
        if (qname.equals(LINKED_RESOURCES)) {
            currentState = State.LINKED_RESOURCES;
        } else if (currentState == State.LINKED_RESOURCES && qname.equals(LINK)) {
            currentState = State.LINK;
        } else if (currentState == State.LINK && qname.equals(NAME)) {
            chars = new StringBuilder();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qname) {
        if (currentState == State.LINK) {
            if (qname.equals(NAME) && chars != null) {
                String name = chars.toString().trim();
                IResource resource = project.findMember(new Path(name));
                if (resource != null)
                    linkedResources.add(resource);
            } else if (qname.equals(LINK)) {
                currentState = State.LINKED_RESOURCES;
            }
        } else if (qname.equals(LINKED_RESOURCES)) {
            currentState = State.DESCRIPTION;
        }
        chars = null;
    }

    @Override
    public void characters(char[] c, int start, int length) {
        if (chars != null) {
            chars.append(c, start, length);
        }
    }

    /**
     * Parse the .project file and read the information about linked resources.
     * 
     * @param project The eclipse project.
     * @return The list of linked resources.
     */
    static List<IResource> parseProject(IProject project) {
        List<IResource> list = new ArrayList<IResource>();
        IFile file = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ProjectInfoHandler handler = new ProjectInfoHandler(project, list);
            parser.parse(file.getContents(), handler);
        } catch (Exception e) {
            Trace.logError("Failed to read the project info: " + file.getLocation(), e);
        }
        return list;
    }
}

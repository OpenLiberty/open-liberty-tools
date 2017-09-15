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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.XMLWriter;

/**
 * A validation ignore filter utility class
 */
public class ValidationFilterUtil {
    private static final String VALIDATION_SETTINGS_FOLDER = ".settings";
    private static final String VALIDATION_SETTINGS_FILE = "com.ibm.ws.st.validation.xml";
    private static final String ELEMENT_VALIDATION_FILTER = "validationFilter";

    public static Document loadSettings(IProject project) {
        return loadSettings(project, false);
    }

    public static Document loadSettings(IProject project, boolean toCreateInitialDoc) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "ValidationFilterUtil.loadSettings");
        }

        if (project == null || !project.isAccessible()) {
            if (Trace.ENABLED && project != null) {
                Trace.trace(Trace.RESOURCE, "Project is not accessible: " + project);
            }
            return null;
        }

        final IFile file = project.getFolder(VALIDATION_SETTINGS_FOLDER).getFile(VALIDATION_SETTINGS_FILE);
        if (!file.exists()) {
            if (toCreateInitialDoc) {
                return createInitialDocument();
            }
            return null;
        }

        if (file.isAccessible()) {
            return loadSettings(file);
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.RESOURCE, "File is not accessible: " + file);
        }

        return null;
    }

    private static Document loadSettings(IFile file) {
        InputStream in = null;
        Document doc = null;
        try {
            in = file.getContents();

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder parser = factory.newDocumentBuilder();

            parser.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Warning while reading validation ignore settings - " + e.getMessage());
                    }
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Error while reading validation ignore settings - " + e.getMessage());
                    }
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Error while reading validation ignore settings - " + e.getMessage());
                    }
                }
            });

            doc = parser.parse(in);
        } catch (SAXException e) {
            // ignore the exception - it's taken care of by the error handler
        } catch (Exception e) {
            Trace.logError("Could not load validation ingore settings: " + file.getLocationURI(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (doc != null && doc.getDocumentElement() != null) {
            return doc;
        }
        return null;
    }

    private static Document createInitialDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Trace.logError("Failed to create a document builder", e);
            return null;
        }

        final Document doc = builder.newDocument();
        try {
            Element docElement = doc.createElement(ELEMENT_VALIDATION_FILTER);
            doc.appendChild(docElement);
        } catch (DOMException e) {
            Trace.logError("Failed to create intial document", e);
            return null;
        }
        return doc;
    }

    public static boolean saveSettings(IProject project, Document doc) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "ValidationFilterUtil.saveSettings");
        }

        if (project == null || !project.isAccessible()) {
            if (Trace.ENABLED && project != null) {
                Trace.trace(Trace.RESOURCE, "Project is not accessible: " + project);
            }
            return false;
        }

        final IFolder folder = project.getFolder(VALIDATION_SETTINGS_FOLDER);
        if (!folder.exists())
            try {
                folder.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                Trace.logError("Failed to create folder: " + folder, e);
                return false;
            }

        final IFile file = folder.getFile(VALIDATION_SETTINGS_FILE);
        if (file.exists() && file.isReadOnly()) {
            IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { file }, null);
            if (status.getSeverity() == IStatus.ERROR) {
                // didn't work or not under source control
                Trace.logError("validateEdit failed for: " + file, new CoreException(status));
                return false;
            }
        }

        return saveSettings(file, doc);
    }

    private static boolean saveSettings(IFile file, Document doc) {
        InputStream in = null;
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLWriter p = new XMLWriter(out, file.getProject());
            p.print(doc);
            p.flush();

            in = new ByteArrayInputStream(out.toByteArray());

            if (file.exists())
                file.setContents(in, true, true, new NullProgressMonitor());
            else
                file.create(in, true, new NullProgressMonitor());

            return true;
        } catch (Exception e) {
            Trace.logError("Failed to save file: " + file.getLocationURI(), e);
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            	Trace.logError("Failed to save file: " + file.getLocationURI(), e);
            }
        }
    }

    public static boolean ignoreAllAttributes(IResource resource) {
        if (resource == null) {
            return false;
        }

        final IProject project = resource.getProject();
        final Document doc = loadSettings(project, true);

        if (doc == null) {
            return false;
        }

        final Element docElem = doc.getDocumentElement();
        final String pathString = getRelativePathString(project, resource);
        final Element matchElem = addElement(doc, docElem, Constants.ELEMENT_MATCH);

        addElement(doc, matchElem, Constants.ELEMENT_FILE, Constants.ATTRIBUTE_PATH, pathString);
        addElement(doc, matchElem, Constants.ELEMENT_IGNORE, Constants.ATTRIBUTE_PATTERN, Constants.PATTERN_UNREC_ATTR);

        return saveSettings(project, doc);
    }

    public static boolean ignoreAllAttributes(IResource resource, String elemName) {
        if (resource == null) {
            return false;
        }

        final IProject project = resource.getProject();
        final Document doc = loadSettings(project, true);

        if (doc == null) {
            return false;
        }

        final Element docElem = doc.getDocumentElement();
        final String pathString = getRelativePathString(project, resource);
        final Element matchElem = addElement(doc, docElem, Constants.ELEMENT_MATCH);
        addElement(doc, matchElem, Constants.ELEMENT_FILE, Constants.ATTRIBUTE_PATH, pathString);
        addElement(doc, matchElem, Constants.ELEMENT_IGNORE, Constants.ATTRIBUTE_PATTERN, Constants.PATTERN_UNREC_ATTR);
        addElement(doc, matchElem, Constants.ELEMENT_ELEMENT, Constants.ATTRIBUTE_NAME, elemName);

        return saveSettings(project, doc);
    }

    public static boolean ignoreAttribute(IResource resource, String elemName, String attrName) {
        if (resource == null) {
            return false;
        }

        final IProject project = resource.getProject();
        final Document doc = loadSettings(project, true);

        if (doc == null) {
            return false;
        }

        final Element docElem = doc.getDocumentElement();
        final String pathString = getRelativePathString(project, resource);
        final Element matchElem = addElement(doc, docElem, Constants.ELEMENT_MATCH);
        addElement(doc, matchElem, Constants.ELEMENT_FILE, Constants.ATTRIBUTE_PATH, pathString);
        addElement(doc, matchElem, Constants.ELEMENT_IGNORE, Constants.ATTRIBUTE_PATTERN, Constants.PATTERN_UNREC_ATTR);
        addElement(doc, matchElem, Constants.ELEMENT_ELEMENT, Constants.ATTRIBUTE_NAME, elemName);
        addElement(doc, matchElem, Constants.ELEMENT_ATTRIBUTE, Constants.ATTRIBUTE_NAME, attrName);

        return saveSettings(project, doc);
    }

    public static String getRelativePathString(IProject project, IResource resource) {
        if (resource == null) {
            return "";
        }

        final IPath resPath = resource.getLocation();
        if (project != null) {
            final IPath projPath = project.getLocation();
            if (projPath.isPrefixOf(resPath)) {
                final IPath relPath = resPath.makeRelativeTo(projPath);
                return relPath.toOSString();
            }
        }
        return resPath.toOSString();
    }

    private static Element addElement(Document doc,
                                      Element parent,
                                      String childName,
                                      String attrName,
                                      String attrValue) {
        final Element child = addElement(doc, parent, childName);
        child.setAttribute(attrName, attrValue);
        return child;
    }

    private static Element addElement(Document doc,
                                      Element parent,
                                      String childName) {
        final Element child = doc.createElement(childName);
        parent.appendChild(child);
        return child;
    }
}

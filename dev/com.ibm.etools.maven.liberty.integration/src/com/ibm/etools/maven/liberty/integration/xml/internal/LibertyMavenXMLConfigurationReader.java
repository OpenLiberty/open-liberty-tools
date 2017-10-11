/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.xml.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConfiguration;
import com.ibm.etools.maven.liberty.integration.internal.Trace;

public class LibertyMavenXMLConfigurationReader {

    private Document document;
    private Element rootElement;
    private long lastModified;

    public LibertyMavenConfiguration load(URI uri) throws IOException {
        InputStream in = null;
        long time = System.currentTimeMillis();
        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Loading Liberty plugin configuration file : " + uri.toString());

            File file = new File(uri);
            lastModified = file.lastModified();
            in = new BufferedInputStream(new FileInputStream(file));
            document = documentLoad(in);
            rootElement = document.getDocumentElement();
            if (rootElement == null)
                throw new IOException("Could not read config file");
            return loadModel();
        } catch (FileNotFoundException e) {
            Trace.logError("Invalid file: " + uri, e);
            throw e;
        } catch (IllegalArgumentException e) {
            // caused when URI is not a valid file
            Trace.logError("Invalid path: " + uri, e);
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
            if (Trace.ENABLED) {
                long t = System.currentTimeMillis() - time;
                Trace.trace(Trace.INFO, "Liberty plugin configuration file load time " + t + "ms");
            }
        }
    }

    private LibertyMavenConfiguration loadModel() throws IOException {
        // Get the child nodes
        NodeList children = rootElement.getChildNodes();
        if (children == null || children.getLength() == 0) {
            throw new IOException("Liberty-Maven configuration file is invalid.");
        }

        DOMBasedLibertyMavenConfigurationBuilder configBuilder = new DOMBasedLibertyMavenConfigurationBuilder(lastModified);
        // Iterate through each of the nodes and add them to the model
        Element elem = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                elem = (Element) child;
                configBuilder.addToModel(elem);
            }
        }
        return configBuilder.getModel();
    }

    public Document documentLoad(InputStream in) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                Trace.trace(Trace.WARNING, "Warning while reading configuration file.\nReason: " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                Trace.logError("Error while reading configuration file.", e);
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                Trace.trace(Trace.WARNING, "Error while reading configuration file.\nReason: " + e.getMessage());
            }
        });
        return parser.parse(new InputSource(in));
    }

}

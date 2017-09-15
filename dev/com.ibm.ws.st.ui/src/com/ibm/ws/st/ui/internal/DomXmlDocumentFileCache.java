/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/** Added as part of WASRTC 118980 to prevent memory leak. */
@SuppressWarnings("restriction")
public class DomXmlDocumentFileCache {

    private static final DomXmlDocumentFileCache instance = new DomXmlDocumentFileCache();

    private DomXmlDocumentFileCache() {}

    public static DomXmlDocumentFileCache getInstance() {
        return instance;
    }

    /** Synchronize on MapLock when accessing map */
    private final Object mapLock = new Object();
    Map<File /* XML File */, DomXmlCacheEntry /* Cached XML entry */> map = new HashMap<File, DomXmlCacheEntry>();

    /** Read the contents of a file into a String */
    private static String readFileIntoString(File f) throws IOException {

        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(f));

            String str;
            while (null != (str = br.readLine())) {
                sb.append(str + "\n");
            }

            return sb.toString();

        } finally {
            if (br != null) {
                br.close();
            }
        }

    }

    /**
     * Parse the document XML file from the configuration file; but only if the file contents has changed
     * from when we last parsed it.
     */
    public Document getDocument(ConfigurationFile cf) throws IOException {

        File f = new File(cf.getURI());
        if (!f.exists() || !f.canRead()) {
            return parseDomDocument(cf);
        }

        String fileContents = readFileIntoString(f);

        DomXmlCacheEntry e;
        synchronized (mapLock) {
            e = map.get(f);

            boolean createNew = false;

            if (e != null) {
                // We have seen this file before
                if (e.document != null && e.document.get() != null && e.fileContents != null && e.fileContents.get() != null
                    && e.fileContents.get().equalsIgnoreCase(fileContents)) {
                    // We've seen this exact file contents before, so just return
                    // the document object for it

                    createNew = false;
                } else {
                    // The file has changed; we need to reparse it.
                    map.remove(f);
                    createNew = true;
                }

            } else {
                // We have not seen this file before
                createNew = true;
            }

            if (createNew) {
                e = new DomXmlCacheEntry();
                e.fileContents = new SoftReference<String>(fileContents);
                e.document = new SoftReference<Document>(parseDomDocument(cf));
                map.put(f, e);
            }
        }

        if (e != null) {
            return e.document.get();
        }
        return null;

    }

    // Load our own IDOMModel
    //
    // We load our own IDOMModel, instead on relying on the one provided by
    // the configuration file, because we could end up with orphaned nodes
    // in the server's view (if it was expanded and the configuration file
    // was open, and we add/remove application to/from the server). The DDE
    // editor registers a model state listener, so when an application is
    // added to the server, the model is changed and the editor will re-load
    // the model which will update the document tree (replacing child nodes
    // with new ones). As a result, the nodes in the server view are orphaned
    // and we end up with null pointer excpetion when selecting any config
    // item in the server's view
    @SuppressWarnings({ "restriction", "deprecation" })
    private static Document parseDomDocument(ConfigurationFile configFile) {

        // The contents of this method are the same as the original getDomDocument method of DDETreeContentProvider.

        InputStream in = null;
        IDOMModel dom = null;

        try {
            final URI uri = configFile.getURI();
            IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model = null;
            try {
                in = new BufferedInputStream(new FileInputStream(new File(uri)));
                model = manager.getModelForRead(
                                                new File(uri).getAbsolutePath(), in, null);

                if (model == null || !(model instanceof IDOMModel)) {
                    Trace.logError(
                                   "Unable to load IDOM Model from uri: " + uri, null);
                    return null;
                }
            } catch (Exception e) {
                Trace.logError("Error loading IDOM Model from uri: " + uri, e);
                return null;
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore
                    }
            }

            dom = (IDOMModel) model;

            // put the document uri and user directory into user data to help
            // downstream viewers
            IDOMDocument domDocument = dom.getDocument();
            domDocument.setUserData(ConfigurationFile.USER_DATA_URI,
                                    uri.toString(), null);
            domDocument.setUserData(ConfigurationFile.USER_DATA_USER_DIRECTORY,
                                    configFile.getUserDirectory(), null);

            return domDocument;
        } finally {
            if (dom != null)
                dom.releaseFromRead();
        }
    }

    class DomXmlCacheEntry {
        SoftReference<String> fileContents;
        SoftReference<Document> document;

    }
}
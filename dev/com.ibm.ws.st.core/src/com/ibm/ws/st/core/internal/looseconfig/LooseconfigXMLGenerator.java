/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.looseconfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.PublishUnit;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.XMLWriter;

public class LooseconfigXMLGenerator {
    public static final String ELE_ARCHIVE = "archive";
    public static final String ELE_DIR = "dir";
    public static final String ELE_FILE = "file";

    public static final String ATT_TARGET_IN_ARCHIVE = "targetInArchive";
    public static final String ATT_SOURCE_ON_DISK = "sourceOnDisk";
    public static final String ATT_EXCLUDE = "exclude";
    private WebSphereServerBehaviour serverBehaviour = null;

    public LooseconfigXMLGenerator(WebSphereServerBehaviour serverBehaviour) {
        this.serverBehaviour = serverBehaviour;
    }

    public void generateRepository(IPath path, PublishUnit app) throws ParserConfigurationException, IOException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        fillLooseContent(doc, null, app);

        saveDocument(path, doc);
    }

    protected void fillLooseContent(Document doc, Element parent, PublishUnit pUnit) {
        IModule[] module = pUnit.getModule();
        LooseArchive arch = new LooseArchive(module[module.length - 1], serverBehaviour);
        Element archive = doc.createElement(ELE_ARCHIVE);

        // Handles archives in archives such as a war in an ear.  Will generate
        // <archive targetInArchive="archive_name"> followed by content for
        // the nested archive itself.
        if (pUnit.getParent() != null) { //Root module doesn't have a targetInArchive
            IModule[] parentModule = pUnit.getParent().getModule();
            IProject parentProject = parentModule[parentModule.length - 1].getProject();
            String targetInArchive = DeploymentAssemblyUtil.getDeployPath(parentProject, module[module.length - 1].getName());
            if (targetInArchive == null) // binary module.  It is added as jar in the parent.
                return;
            archive.setAttribute(ATT_TARGET_IN_ARCHIVE, targetInArchive);
        }

        if (parent == null)
            doc.appendChild(archive);
        else
            parent.appendChild(archive);

        List<PublishUnit> children = pUnit.getChildren();
        if (children != null) {
            for (PublishUnit pu : children) {
                if (pu.getDeltaKind() != ServerBehaviourDelegate.REMOVED)
                    fillLooseContent(doc, archive, pu);
            }
        }

        List<DeploymentEntry> entries = arch.getFilteredDeploymentEntries();
        for (DeploymentEntry entry : entries) {
            Element el;
            if (DeploymentEntry.TYPE_DIR == entry.getType()) {
                el = doc.createElement(ELE_DIR);
                el.setAttribute(ATT_TARGET_IN_ARCHIVE, entry.getTargetInArchive());
                el.setAttribute(ATT_SOURCE_ON_DISK, entry.getSourceOnDisk());
                if (entry.getExclude() != null) {
                    el.setAttribute(ATT_EXCLUDE, entry.getExclude());
                }
            } else {
                el = doc.createElement(ELE_FILE);
                el.setAttribute(ATT_TARGET_IN_ARCHIVE, entry.getTargetInArchive());
                el.setAttribute(ATT_SOURCE_ON_DISK, entry.getSourceOnDisk());
            }

            archive.appendChild(el);
        }

    }

    private static void saveDocument(IPath path, Document d) throws IOException {
        XMLWriter w = null;
        try {
            IPath appFolder = path.removeLastSegments(1);
            File appDir = appFolder.toFile();
            if (!appDir.exists()) {
                //in this case, we will need to check if the apps directory is there in the case of first time publish.
                IPath appsFolder = appFolder.removeLastSegments(1);
                File appsDir = appsFolder.toFile();
                if (!appsDir.exists()) {
                    if (!appsDir.mkdir()) {
                        Trace.logError("saveDocument(..) can't make directory " + appsDir.toString(), null);
                    }
                }

                if (!appDir.mkdir()) {
                    Trace.logError("saveDocument(..) can't make directory " + appDir.toString(), null);
                }
            }
            w = new XMLWriter(new BufferedOutputStream(new FileOutputStream(path.toFile())), null);
            w.print(d);
        } catch (IOException e) {
            throw e;
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Exception e) {
                    Trace.logError("Failed to save loose config xml file: " + path, e);
                }
            }
        }
    }
}

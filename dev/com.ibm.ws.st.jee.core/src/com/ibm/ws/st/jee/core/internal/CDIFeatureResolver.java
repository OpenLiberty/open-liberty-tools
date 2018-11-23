/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ModuleDelegate;

import com.ibm.ws.st.core.internal.FeatureResolver;
import com.ibm.ws.st.core.internal.FeatureResolverFeature;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.RequiredFeatureMap;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

@SuppressWarnings("restriction")
public class CDIFeatureResolver extends FeatureResolver {

    private static final String FILE_NAME = "beans.xml";
    private static final QName SCHEMA_QNAME = new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
    private static final String ROOT_ELEM = "beans";
    private static final String VERSION_ATTR = "version";
    private static final String BEANS1_1 = "beans_1_1.xsd";
    private static final String BEANS2_0 = "beans_2_0.xsd";

    private static final Set<String> folderSet = new HashSet<String>();
    private static final Map<String, FeatureResolverFeature> cdiMap = new HashMap<String, FeatureResolverFeature>();

    static {
        folderSet.add("WEB-INF");
        folderSet.add("META-INF");

        cdiMap.put("1.0", JEEConstants.FEATURE_CDI10);
        cdiMap.put("1.1", JEEConstants.FEATURE_CDI12);
        cdiMap.put("2.0", JEEConstants.FEATURE_CDI20);
    }

    @Override
    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList,
                                    FeatureSet existingFeatures, RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
        if (existingFeatures != null && existingFeatures.supports(JEEConstants.FEATURE_CDI.getName()))
            return;

        for (int i = 0; i < moduleList.size(); i++) {
            IModule[] module = moduleList.get(i);
            IModule m = module[module.length - 1];
            IModuleResourceDelta[] delta = deltaList == null ? null : deltaList.get(i);
            IModuleResource resource = null;
            if (delta != null) {
                resource = getBeansFile(m, delta, monitor);
            } else {
                resource = getBeansFile(m, monitor);
            }

            if (resource != null) {
                File file = getFile(resource);
                FeatureResolverFeature feature = null;
                if (file != null) {
                    feature = getFeature(file);
                } else {
                    feature = JEEConstants.FEATURE_CDI;
                }
                FeatureResolver.checkAndAddFeature(requiredFeatures, existingFeatures, wr, feature, moduleList, includeAll);
            }
        }

    }

    /*
     * Look for beans.xml file in standard folders.
     */
    private static IModuleResource getBeansFile(IModule module, IProgressMonitor monitor) {
        ModuleDelegate moduleDelegate = (ModuleDelegate) module.loadAdapter(ModuleDelegate.class, monitor);
        if (moduleDelegate == null)
            return null;

        try {
            IModuleResource[] members = moduleDelegate.members();
            for (IModuleResource member : members) {
                if (member instanceof IModuleFolder && folderSet.contains(member.getName())) {
                    IModuleResource[] folderMembers = ((IModuleFolder) member).members();
                    for (IModuleResource folderMember : folderMembers) {
                        if (folderMember instanceof IModuleFile && FILE_NAME.equals(folderMember.getName())) {
                            return folderMember;
                        }
                    }
                }
            }
        } catch (CoreException ce) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Problem scanning module members for beans.xml file", ce);
            return null;
        }
        return null;
    }

    /*
     * Look for beans.xml file in standard folders.
     */
    private static IModuleResource getBeansFile(IModule module, IModuleResourceDelta[] delta, IProgressMonitor monitor) {
        if (delta == null)
            return null;

        for (IModuleResourceDelta mrd : delta) {
            IModuleResource mr = mrd.getModuleResource();
            if (mr instanceof IModuleFolder && folderSet.contains(mr.getName())) {
                IModuleResourceDelta[] childDeltas = mrd.getAffectedChildren();
                for (IModuleResourceDelta childDelta : childDeltas) {
                    IModuleResource child = childDelta.getModuleResource();
                    if (child instanceof IModuleFile && FILE_NAME.equals(child.getName())) {
                        if (childDelta.getKind() != IModuleResourceDelta.REMOVED) {
                            return child;
                        }
                    }
                }
            }
        }
        return null;
    }

    /*
     * Use StAX to parse the file and look for the version of the schema that is
     * being used.
     */
    private static FeatureResolverFeature getFeature(File file) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        InputStream fileStream = null;
        String version = "1.0";
        try {
            fileStream = new FileInputStream(file);
            reader = factory.createXMLStreamReader(fileStream);
            if (reader.hasNext()) {
                reader.nextTag();
                if (reader.isStartElement() && ROOT_ELEM.equals(reader.getLocalName())) {
                    // If the version is explicitly specified, use that, otherwise rely
                    // on the schema version
                    int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        if (VERSION_ATTR.equals(reader.getAttributeLocalName(i))) {
                            version = reader.getAttributeValue(i);
                            break;
                        } else if (SCHEMA_QNAME.equals(reader.getAttributeName(i))) {
                            String value = reader.getAttributeValue(i);
                            if (value != null) {
                                if (value.endsWith(BEANS1_1)) {
                                    version = "1.1";
                                    // Don't break as should keep looking in case there is a version attribute
                                } else if (value.endsWith(BEANS2_0)) {
                                    version = "2.0";
                                    // Don't break as should keep looking in case there is a version attribute
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Problem parsing beans.xml file: " + file.getAbsolutePath(), e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    // Ignore
                }
            }
        }
        if (cdiMap.containsKey(version)) {
            return cdiMap.get(version);
        }
        return JEEConstants.FEATURE_CDI;
    }

    /*
     * Get the File object for the given module resource.
     */
    private static File getFile(IModuleResource moduleResource) {
        File file = (File) moduleResource.getAdapter(File.class);
        if (file == null) {
            IFile iFile = (IFile) moduleResource.getAdapter(IFile.class);
            if (iFile != null) {
                IPath location = iFile.getLocation();
                if (location != null) {
                    return new File(location.toString());
                }
            }
        }
        return file;
    }
}

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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

/**
 *
 */
@SuppressWarnings("restriction")
public class LooseArchive {

    //We don't need targetInArchive in this class. The parent knows it, not this class.
    //certain can have it here, but there is not use yet.
    //  protected String targetInArchive;

    //
    //  We don't need to keep track of the non-binary child modules here.  They are
    //  treated as modules in the gen and will gen the entries themselves.  We handle
    //  the binary modules here.
    //  protected List<LooseArchive> childArchives;

    protected List<DeploymentEntry> deploymentEntries = new ArrayList<DeploymentEntry>();

    // Used for mapping paths when necessary
    private final WebSphereServerBehaviour serverBehaviour;
    private final AbstractServerBehaviourExtension serverBehaviourExt;

    /**
     * @return the deploymentEntries
     */
    public List<DeploymentEntry> getDeploymentEntries() {
        return deploymentEntries;
    }

    protected IPath defaultOutputPath = null;

    public LooseArchive(IModule module) {
        this(module, null);
    }

    public LooseArchive(IModule module, WebSphereServerBehaviour serverBehaviour) {
        super();
        this.serverBehaviour = serverBehaviour;
        this.serverBehaviourExt = (AbstractServerBehaviourExtension) serverBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        process(module);
    }

    protected void process(IModule module) {
        // Get the build path stuff
        StructureEdit sEdit = null;
        Hashtable<IPath, IClasspathEntry> javaSourcePathEntries = new Hashtable<IPath, IClasspathEntry>();
        IProject project = module.getProject();

        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                defaultOutputPath = javaProject.getOutputLocation();

                //Get the build path stuff
                IClasspathEntry[] javaClasspathEntries = javaProject.getRawClasspath();
                for (IClasspathEntry entry : javaClasspathEntries) {
                    if (IClasspathEntry.CPE_SOURCE == entry.getEntryKind()) {
                        IPath path = entry.getPath().removeFirstSegments(1).makeAbsolute(); //The first segment is always the project name
                        javaSourcePathEntries.put(path, entry);
                    }
                }
            }
        } catch (CoreException e) {
            Trace.logError("Problem to calculate the build path entires of " + project.getName(), e);
        }

        // Get linked resources
        List<IResource> linkedResources = ProjectInfoHandler.parseProject(project);

        try {
            // get the deployment assembly stuff.
            sEdit = StructureEdit.getStructureEditForRead(project);
            WorkbenchComponent wbc = sEdit.getComponent();
            Object[] mappings = wbc.getResources().toArray();
            for (Object o : mappings) {
                if (o instanceof ComponentResource) {
                    ComponentResource res = (ComponentResource) o;
                    IPath runtimePath = res.getRuntimePath();
                    IPath source = res.getSourcePath().makeAbsolute();
                    IPath outputPath = mappingDeploymentPathToOutputPath(project, source, javaSourcePathEntries);
                    IPath os;
                    if (outputPath.segmentCount() == 1) {
                        IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(outputPath.lastSegment());
                        os = p.getLocation();
                    } else {
                        IFile out = ResourcesPlugin.getWorkspace().getRoot().getFile(outputPath);
                        os = out.getLocation();
                    }
                    //TODO exclude is not there yet.
                    if (os != null) {
                        // Do any path mapping
                        IPath mappedPath = null;
                        if (serverBehaviourExt != null) {
                            mappedPath = serverBehaviourExt.getMappedPath(os, serverBehaviour);
                            if (mappedPath != null) {
                                os = mappedPath;
                            }
                        }
                        String pathEntryUpdate = (mappedPath != null) ? os.toString() : os.toOSString();
                        DeploymentEntry de = new DeploymentEntry(runtimePath.toString(), pathEntryUpdate, DeploymentEntry.TYPE_DIR, null);
                        if (!deploymentEntries.contains(de)) {
                            deploymentEntries.add(de);
                        }
                    } else if (Trace.ENABLED) {
                        Trace.logError("Failed to get source location for: " + outputPath, null);
                    }

                    // Add any linked resources that are within the source location
                    for (IResource resource : linkedResources) {
                        IPath resourcePath = resource.getFullPath();
                        if (outputPath.isPrefixOf(resourcePath) && !outputPath.equals(resourcePath)) {
                            IPath resourceLocation = resource.getLocation();
                            if (resourceLocation != null) {
                                IPath relPath = resourcePath.makeRelativeTo(outputPath);
                                IPath targetPath = runtimePath.append(relPath);
                                int type = resource.getType() == IResource.FOLDER ? DeploymentEntry.TYPE_DIR : DeploymentEntry.TYPE_FILE;
                                DeploymentEntry de = new DeploymentEntry(targetPath.toString(), resourceLocation.toOSString(), type, null);
                                if (!deploymentEntries.contains(de)) {
                                    deploymentEntries.add(de);
                                }
                            } else if (Trace.ENABLED) {
                                Trace.logError("Failed to get source location for linked resource: " + resourcePath, null);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Trace.logError("Exception in LooseArchive.", e);
        } finally {
            if (sEdit != null) {
                sEdit.dispose();
            }
        }

        // Any referenced jars in a utility project, won't be of much
        // value unless the utility project itself is referenced by
        // another module such a web module. The parent web module
        // will add the referenced jars to its deployment entry list.
        if ("jst.utility".equals(module.getModuleType().getId()))
            return;

        // Get the referenced jars in deployment assembly
        IVirtualComponent vc = ComponentCore.createComponent(project);
        IVirtualReference[] refs = vc.getReferences();
        for (IVirtualReference ref : refs) {
            IVirtualComponent component = ref.getReferencedComponent();
            if (component.isBinary()) {
                IPath runtimePath = ref.getRuntimePath();
                String targetInArchive = runtimePath.append(ref.getArchiveName()).makeAbsolute().toString();

                if (component instanceof VirtualArchiveComponent) {
                    File file = (File) ((VirtualArchiveComponent) component).getAdapter(File.class);
                    if (file == null) {
                        Trace.logError("LooseConfig - the virtual component does not adapt to a File: " + ((VirtualArchiveComponent) component).getArchivePath(), null);
                    } else {
                        DeploymentEntry de = new DeploymentEntry(targetInArchive, file.getAbsolutePath(), DeploymentEntry.TYPE_FILE, null);
                        if (!deploymentEntries.contains(de)) {
                            deploymentEntries.add(de);
                        }
                    }
                }
            } else if (canAddReferencedComponentClasspathDependencies(module, component.getProject())) {
                addReferencedComponentClasspathDependencies(ref);
            }
        }
    }

    private IPath mappingDeploymentPathToOutputPath(IProject project, IPath dplymntPath, Hashtable<IPath, IClasspathEntry> javaSourcePathEntries) {
        String dPathS = dplymntPath.toPortableString();
        IPath tempPath = null;
        int tempLength = 0;
        Enumeration<IPath> enu = javaSourcePathEntries.keys();
        while (enu.hasMoreElements()) {
            IPath path = enu.nextElement();
            String s = path.toPortableString();
            if (dPathS.startsWith(s)) {
                // We need to check all source folders in the build path to cover the below case.
                //      delymnt path points to /src/abc/ef
                //      The build path has folder:  /src and /src/abc.  We need to use the /src/abc
                if (s.length() > tempLength) {
                    tempPath = path;
                    tempLength = s.length();
                }
            }
        }

        IPath resultPath = null;
        if (tempPath == null) { //This is the case like WebContent, we will map it to the folder in the project.
            resultPath = project.getFullPath().append(dplymntPath);
        } else { // We need to return the output path of the source folder.
            IClasspathEntry entry = javaSourcePathEntries.get(tempPath);
            IPath output = entry.getOutputLocation();
            if (output == null) { // default classpath is used.
                output = defaultOutputPath;
            }
            if (!dplymntPath.equals(tempPath)) { //e.g. deployment path = /src/abc  and the build path is /src
                int d = dplymntPath.segmentCount();
                int b = tempPath.segmentCount();
                int c = d - b;
                IPath suffixPath = dplymntPath.removeFirstSegments(c);
                resultPath = output.append(suffixPath);
            } else {
                resultPath = output;
            }
        }

        if (resultPath == null)
            Trace.logError("LooseConfig - Can't find the output of " + dplymntPath.toString() + " in " + project.getName(), null);
        return resultPath;
    }

    public List<DeploymentEntry> getFilteredDeploymentEntries() {
        List<String> rootEntries = new ArrayList<String>();
        List<DeploymentEntry> nonRootEntries = new ArrayList<DeploymentEntry>();
        List<DeploymentEntry> filteredEntries = new ArrayList<DeploymentEntry>();
        for (DeploymentEntry de : deploymentEntries) {
            if ("/".equals(de.targetInArchive)) {
                rootEntries.add(de.sourceOnDisk);
                filteredEntries.add(de);
            } else {
                nonRootEntries.add(de);
            }
        }
        for (DeploymentEntry de : nonRootEntries) {
            if (!isEntryImplicit(de, rootEntries)) {
                filteredEntries.add(de);
            }
        }
        return filteredEntries;
    }

    private boolean isEntryImplicit(DeploymentEntry de, List<String> rootEntries) {
        for (String root : rootEntries) {
            if (de.sourceOnDisk != null && de.sourceOnDisk.startsWith(root)) {
                IPath sourcePath = new Path(de.sourceOnDisk.substring(root.length()));
                IPath targetInArchivePath = new Path(de.targetInArchive);
                if (sourcePath.equals(targetInArchivePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addReferencedComponentClasspathDependencies(IVirtualReference reference) {
        final IVirtualComponent referencedComponent = reference.getReferencedComponent();
        IProject project = referencedComponent.getProject();
        IClasspathEntry[] javaClasspathEntries = null;

        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                javaClasspathEntries = javaProject.getRawClasspath();
            }
        } catch (CoreException e) {
            Trace.logError("Problem to calculate the build path entires of " + project.getName(), e);
        }

        if (javaClasspathEntries == null || javaClasspathEntries.length == 0)
            return;

        final IPath runtimePath = reference.getRuntimePath();
        IVirtualReference[] refs = referencedComponent.getReferences();
        for (IVirtualReference ref : refs) {
            IVirtualComponent component = ref.getReferencedComponent();
            if (component.isBinary()) {
                if (component instanceof VirtualArchiveComponent) {
                    File file = (File) ((VirtualArchiveComponent) component).getAdapter(File.class);
                    if (file == null) {
                        Trace.logError("LooseConfig - the virtual component does not adapt to a File: " + ((VirtualArchiveComponent) component).getArchivePath(), null);
                    }
                    // Revisit: Do we really need the extra match? The check on the
                    // refRuntimePath might be enough.
                    else if (isMatchedComponentDependency(file, project.getLocation(), javaClasspathEntries)) {
                        IPath refRuntimePath = ref.getRuntimePath();
                        //if path isn't ../, it shouldn't be added here
                        if (!refRuntimePath.toString().startsWith("../"))
                            continue;

                        refRuntimePath = runtimePath.append(refRuntimePath.removeFirstSegments(1)).append(ref.getArchiveName());
                        DeploymentEntry de = new DeploymentEntry(refRuntimePath.toString(), file.getAbsolutePath(), DeploymentEntry.TYPE_FILE, null);
                        if (!deploymentEntries.contains(de)) {
                            deploymentEntries.add(de);
                        }
                    }
                }
            }
        }

    }

    private boolean isMatchedComponentDependency(File file, IPath projPath, IClasspathEntry[] javaClasspathEntries) {
        for (IClasspathEntry entry : javaClasspathEntries) {
            final IClasspathAttribute[] attributes = entry.getExtraAttributes();
            for (int i = 0; i < attributes.length; i++) {
                final IClasspathAttribute attribute = attributes[i];
                final String name = attribute.getName();
                if (name.equals("org.eclipse.jst.component.dependency")) {
                    IPath location = getEntryLocation(entry, projPath);
                    if (location != null) {
                        if (location.toFile().getAbsolutePath().equals(file.getAbsolutePath()))
                            return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean canAddReferencedComponentClasspathDependencies(IModule module, IProject refProject) {
        if ("jst.web".equals(module.getModuleType().getId())) {
            try {
                IFacetedProject facetedProject = ProjectFacetsManager.create(refProject);
                Iterator<IProjectFacetVersion> iterator = facetedProject.getProjectFacets().iterator();
                while (iterator.hasNext()) {
                    IProjectFacetVersion facet = iterator.next();
                    if ("jst.utility".equals(facet.getProjectFacet().getId()))
                        return true;
                }
            } catch (CoreException ce) {
                // ignore
            }
        }

        return false;
    }

    private static IPath getEntryLocation(final IClasspathEntry entry, IPath projPath) {
        if (entry == null) {
            return null;
        }

        final IPath entryPath = entry.getPath();
        if (entryPath == null)
            return null;

        final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(entryPath);
        if (resource != null) {
            return resource.getLocation();
        }

        if (entryPath.isAbsolute() || projPath == null)
            return entryPath;

        return projPath.append(entryPath);
    }
}

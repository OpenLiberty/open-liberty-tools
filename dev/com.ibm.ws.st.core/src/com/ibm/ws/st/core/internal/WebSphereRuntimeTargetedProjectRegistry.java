/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Singleton class that acts as a registry for WebSphereRuntimeTargetedProject objects.
 * 
 * This class implements IResourceChangeListener to be able to remove projects from the
 * registry when a project is closed or removed. It is registered as resource change lister of
 * the workspace during creation of the singleton instance.
 */
public class WebSphereRuntimeTargetedProjectRegistry implements IResourceChangeListener {

    public static final WebSphereRuntimeTargetedProjectRegistry INSTANCE = new WebSphereRuntimeTargetedProjectRegistry();

    private final Map<IProject, WebSphereRuntimeTargetedProject> registry = Collections.synchronizedMap(new HashMap<IProject, WebSphereRuntimeTargetedProject>());

    private WebSphereRuntimeTargetedProjectRegistry() {
        //Private constructor, do not want clients to create instances
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
    }

    /**
     * Returns a WebSphereRuntimeTargetedProject corresponding to the projects passed in as parameter.
     * 
     * @param project the project for which we want to get an WebSphereRuntimeTargetedProject.
     * @return the WebSphereRuntimeTargetedProject. If the project is not in the registry, null will be
     *         returned. See {@link WebSphereRuntimeTargetedProjectRegistry#addProject(IProject)}
     */
    public synchronized WebSphereRuntimeTargetedProject getProject(IProject project) {
        return registry.get(project);
    }

    /**
     * Adds a new WebSphereRuntimeTargetedProject for the IProject passed in as parameter
     * 
     * @param project the project we want to wrap in a WebSphereRuntimeTargetedProject
     * @return the WebSphereRuntimeTargetedProject for the IProject.
     */
    public synchronized WebSphereRuntimeTargetedProject addProject(final IProject project) {
        final WebSphereRuntimeTargetedProject wrtp = new WebSphereRuntimeTargetedProject(project);
        registry.put(project, wrtp);
        //System.out.println("+++++++++++++++++++++ Added project " + project.getName());
        return wrtp;
    }

    /**
     * Removes the IProject from the registry
     * 
     * @param project the IProject to remove.
     */
    public synchronized void removeProject(final IProject project) {
        registry.remove(project);
    }

    /** {@inheritDoc} */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IProject closing = null;
        switch (event.getType()) {
            case IResourceChangeEvent.PRE_DELETE:
            case IResourceChangeEvent.PRE_CLOSE:
                try {
                    closing = (IProject) event.getResource();
                    removeProject(closing);
                } catch (ClassCastException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "The resource is not a project " + e.getMessage());
                    }
                }
        }

    }
}

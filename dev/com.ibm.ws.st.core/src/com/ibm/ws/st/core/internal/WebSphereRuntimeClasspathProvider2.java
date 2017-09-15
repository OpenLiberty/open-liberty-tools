/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.IClasspathProvider;
import org.eclipse.jst.server.core.internal.JavaServerPlugin;
import org.eclipse.jst.server.core.internal.RuntimeClasspathContainer;
import org.eclipse.jst.server.core.internal.RuntimeClasspathProviderWrapper;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;

@SuppressWarnings("restriction")
public class WebSphereRuntimeClasspathProvider2 implements IClasspathProvider {
    private final IRuntimeComponent rc;

    public WebSphereRuntimeClasspathProvider2(final IRuntimeComponent rc) {
        this.rc = rc;
    }

    @Override
    public List<IClasspathEntry> getClasspathEntries(IProjectFacetVersion fv) {
        IProjectFacet pf = fv.getProjectFacet();
        if (pf == null)
            return null;

        boolean found = false;
        for (ClasspathExtension ce : Activator.getInstance().getClasspathExtensions()) {
            if (ce.supportsFacet(pf)) {
                found = true;
                break;
            }
        }

        if (found) {
            String runtimeTypeId = rc.getProperty("type-id");
            String runtimeId = rc.getProperty("id");
            if (runtimeTypeId == null || runtimeId == null)
                return null;
            RuntimeClasspathProviderWrapper rcpw = JavaServerPlugin.findRuntimeClasspathProviderBySupport(runtimeTypeId);
            if (rcpw != null) {
                IPath path = new Path(RuntimeClasspathContainer.SERVER_CONTAINER);
                path = path.append(rcpw.getId()).append(runtimeId);
                IClasspathEntry cpentry = JavaCore.newContainerEntry(path);
                return Collections.singletonList(cpentry);
            }
        }
        return null;
    }

    public static final class Factory implements IAdapterFactory {
        private static final Class<?>[] ADAPTER_TYPES = { IClasspathProvider.class };

        @SuppressWarnings("rawtypes")
        @Override
        public Object getAdapter(final Object adaptable, final Class adapterType) {
            IRuntimeComponent rc = (IRuntimeComponent) adaptable;
            return new WebSphereRuntimeClasspathProvider2(rc);
        }

        @Override
        public Class<?>[] getAdapterList() {
            return ADAPTER_TYPES;
        }
    }
}
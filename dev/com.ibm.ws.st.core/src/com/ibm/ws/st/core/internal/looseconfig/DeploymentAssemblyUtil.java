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
package com.ibm.ws.st.core.internal.looseconfig;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

import com.ibm.ws.st.core.internal.Trace;

/**
 *
 */
@SuppressWarnings("restriction")
public class DeploymentAssemblyUtil {
    public static String getDeployPath(IProject project, String source) {
        if (source == null || source.isEmpty() || project == null)
            return null;

        String result = null;
        StructureEdit sEdit = null;
        try {
            sEdit = StructureEdit.getStructureEditForRead(project);
            WorkbenchComponent wbc = sEdit.getComponent();

            //folders
            Object[] mappings = wbc.getResources().toArray();
            for (Object o : mappings) {
                ComponentResource res = (ComponentResource) o;
                IPath sourcePath = res.getSourcePath();
                if (source.equals(sourcePath.toString())) {
                    result = res.getRuntimePath().toString();
                    break;
                }
            }

            //projects/modules
            IVirtualComponent vc = ComponentCore.createComponent(project);
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(IVirtualComponent.REQUESTED_REFERENCE_TYPE, IVirtualComponent.DISPLAYABLE_REFERENCES);
            IVirtualReference[] refs = vc.getReferences(options);
            for (IVirtualReference ref : refs) {
                String name = ref.getReferencedComponent().getName();
                if (source.equals(name)) {
                    IPath runtimePath = ref.getRuntimePath();
                    result = runtimePath.append(ref.getArchiveName()).makeAbsolute().toString();
                    break;
                }
            }
        } catch (Exception e) {
            Trace.logError("Exception in DeploymentAssemblyUtil.", e);
        } finally {
            if (sEdit != null) {
                sEdit.dispose();
            }
        }
        return result;
    }
}

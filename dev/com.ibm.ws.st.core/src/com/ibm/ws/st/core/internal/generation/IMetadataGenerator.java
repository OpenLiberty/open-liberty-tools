/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

import com.ibm.ws.st.core.internal.WebSphereRuntime;

/**
 * Interface for metadata generator.
 */
public interface IMetadataGenerator {

    /**
     * Returns the id for this generator. It will not be null or empty string.
     */
    public String getGeneratorId();

    /**
     * Returns the base path for saving generated files given the root.
     */
    public IPath getBasePath(IPath root);

    /**
     * Get the WebSphereRuntime associated with this generator.
     */
    public WebSphereRuntime getWebSphereRuntime();

    /**
     * Return if this generator supports feature list generation.
     */
    public boolean supportsFeatureListGeneration();

    /**
     * Generate the actual metadata.
     */
    public void generateMetadata(IJobChangeListener listener, boolean isRegenInfoCache);

    /**
     * Generate the schema.
     */
    public void generateSchema(String file, IProgressMonitor monitor, int timeout) throws CoreException;

    /**
     * Generate the feature list. Does nothing if feature list generation is not supported.
     */
    public void generateFeatureList(String file, IProgressMonitor monitor, int timeout, String... options) throws CoreException;

    /**
     * Remove the metadata.
     */
    public void removeMetadata(IPath dir, boolean deleteDirectory, boolean destroy);

    /**
     * Returns whether the generator is ready to generate metadata.
     *
     * @return true if the generator is ready or false otherwise.
     */
    public boolean isReadyToGenerateMetadata();

    /**
     * Whether or not the metadata for this generator exists (eg true if it has already been generated, or false if it
     * has yet to be generated). Calling this method will not cause the metadata to be generated.
     */
    public boolean metadataExists();
}

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

public class DeploymentEntry {
    public final static int TYPE_DIR = 0;
    public final static int TYPE_FILE = 1;

    protected String targetInArchive;
    protected String sourceOnDisk;
    protected int type; // dir or file
    protected String exclude;

    public DeploymentEntry(String targetInArchive, String sourceOnDisk, int type, String exclude) {
        this.targetInArchive = targetInArchive;
        this.sourceOnDisk = sourceOnDisk;
        this.type = type;
        this.exclude = exclude;
    }

    /**
     * @return the targetInArchive
     */
    public String getTargetInArchive() {
        return targetInArchive;
    }

    /**
     * @return the sourceOnDisk
     */
    public String getSourceOnDisk() {
        return sourceOnDisk;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return the exclude
     */
    public String getExclude() {
        return exclude;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DeploymentEntry)) {
            return false;
        }

        DeploymentEntry o = (DeploymentEntry) other;
        return type == o.type && isEqual(sourceOnDisk, o.sourceOnDisk)
               && isEqual(targetInArchive, o.targetInArchive) && isEqual(exclude, o.exclude);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private boolean isEqual(String one, String two) {
        if (one == two)
            return true;

        if (one == null || two == null)
            return false;

        return one.equals(two);
    }
}

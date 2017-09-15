/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.crypto;

/**
 * An Object which stores the information regarding to the custom password encryption
 */
public class CustomPasswordEncryptionInfo {
    private final String name;
    private final String featureName;
    private final String description;

    /**
     * ctor (isEnabled is set as false)
     *
     * @param name encoding name
     * @param featureName feature name i.e., usr:customEncryption-1.0
     * @param description the description of the custom password encryption
     */
    public CustomPasswordEncryptionInfo(String name, String featureName, String description) {
        this.name = name;
        this.featureName = featureName;
        this.description = description;
    }

    /**
     * ctor
     *
     * @param name encoding name
     * @param featureName feature name i.e., usr:customEncryption-1.0
     * @param description the description of the custom password encryption
     * @param isEnabled whether the feature is enabled.
     */
    public CustomPasswordEncryptionInfo(String name, String featureName, String description, boolean isEnabled) {
        this(name, featureName, description);
    }

    /**
     * returns custom password encryption encoding name.
     */
    public String getName() {
        return name;
    }

    /**
     * returns custom password encryption feature name.
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * returns custom password encryption descrption.
     */
    public String getDescription() {
        return description;
    }

}

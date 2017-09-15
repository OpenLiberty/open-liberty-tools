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
package com.ibm.ws.st.core.internal.config;

import com.ibm.ws.st.core.internal.Constants;

/**
 * A configuration include conflict resolution
 */
public enum IncludeConflictResolution {

    MERGE(Constants.CONFLICT_MERGE_LABEL),
    REPLACE(Constants.CONFLICT_REPLACE_LABEL),
    IGNORE(Constants.CONFLICT_IGNORE_LABEL);

    private final String label;

    private IncludeConflictResolution(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static IncludeConflictResolution getConflictResolution(String label) {
        if (label != null && !label.isEmpty()) {
            for (IncludeConflictResolution type : IncludeConflictResolution.values()) {
                if (type.toString().equalsIgnoreCase(label))
                    return type;
            }
        }

        return IncludeConflictResolution.MERGE;
    }
}

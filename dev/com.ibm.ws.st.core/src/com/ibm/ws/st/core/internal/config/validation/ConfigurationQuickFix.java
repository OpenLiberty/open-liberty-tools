/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

public interface ConfigurationQuickFix {

    public enum ResolutionType {
        BEST_MATCH_ATTR,
        IGNORE_ALL_ATTR_ALL_ELEM,
        IGNORE_ALL_ATTR_ELEM,
        IGNORE_ATTR_ELEM,
        BEST_MATCH_VARIABLE
    }

    public ResolutionType getResolutionType();
}

/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 * Interface used to request handling of feature conflicts.
 */
public abstract class FeatureConflictHandler {

    public abstract boolean handleFeatureConflicts(WebSphereServerInfo wsServerInfo, Map<String, List<String>> requiredFeatures, Set<FeatureConflict> conflicts,
                                                   boolean quickFixMode);

    public abstract boolean handleFeatureConflicts(WebSphereRuntime wsRuntime, ConfigurationFile file, Map<String, List<String>> requiredFeatures,
                                                   Set<FeatureConflict> conflicts,
                                                   boolean quickFixMode);

}
/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import com.ibm.ws.st.core.internal.FeatureResolverFeature;

@SuppressWarnings("restriction")
public interface JEEConstants {
    public FeatureResolverFeature FEATURE_JSP = new FeatureResolverFeature("jsp");
    public FeatureResolverFeature FEATURE_JDBC = new FeatureResolverFeature("jdbc");

    /** The JMS feature will not be added to a feature list if wmqJmsClient is already present. */
    public FeatureResolverFeature FEATURE_JMS = new FeatureResolverFeature("wasJmsClient", new String[] { "wmqJmsClient" });

    public FeatureResolverFeature FEATURE_CDI10 = new FeatureResolverFeature("cdi-1.0");
    public FeatureResolverFeature FEATURE_CDI12 = new FeatureResolverFeature("cdi-1.2");
    public FeatureResolverFeature FEATURE_CDI20 = new FeatureResolverFeature("cdi-2.0");
    public FeatureResolverFeature FEATURE_CDI = new FeatureResolverFeature("cdi");
}

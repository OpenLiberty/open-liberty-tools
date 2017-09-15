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
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.wizard.Wizard;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class SchemaPropertiesWizard extends Wizard {

    public SchemaPropertiesWizard(SchemaPropertiesCustomObject.SchemaPropertiesData propertiesData) {
        setWindowTitle(Messages.schemaPropsWizardTitle);
        setDefaultPageImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));
        if (propertiesData.getChosenElem() == null)
            addPage(new SchemaPropertiesSelectPage(propertiesData));
        addPage(new SchemaPropertiesEntryPage(propertiesData));
        setNeedsProgressMonitor(false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performFinish() {
        return true;
    }
}

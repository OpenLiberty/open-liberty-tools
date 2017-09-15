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
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.ISite;
import com.ibm.ws.st.ui.internal.Messages;

public class ConfigSnippetComposite extends AddonsComposite {
    public ConfigSnippetComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler) {
        super(parent, map, container, handler);
        container.setDescription(Messages.wizInstallConfigSnippetDescription);
    }

    @Override
    protected void createControl() {
        super.createControl();
        addArchive.setEnabled(false);
    }

    @Override
    protected void initializeTypeFilterList() {
        typeFilterList.add(IProduct.Type.CONFIG_SNIPPET);
        typeFilterMenuList.add(IProduct.Type.CONFIG_SNIPPET);
    }

    @Override
    protected void handleDrop(String[] files, Text filterText) {
        // we do not allow drag and drop, so no-op
    }

    @Override
    public void exit() {
        @SuppressWarnings("unchecked")
        List<IProduct> selectedAddOn = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (selectedAddOn != null && !selectedAddOn.isEmpty()) {
            List<IProduct> configList = new ArrayList<IProduct>();
            for (IProduct p : selectedAddOn) {
                if (p.getType() == IProduct.Type.CONFIG_SNIPPET)
                    configList.add(p);
            }

            List<IProduct> requireList = getRequiredFeatures(configList);
            if (!requireList.isEmpty()) {
                List<IProduct> addList = new ArrayList<IProduct>(requireList.size());
                for (IProduct p : requireList) {
                    if (!selectedAddOn.contains(p)) {
                        addList.add(p);
                    }
                }

                if (!addList.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (IProduct p : addList) {
                        sb.append("    - ").append(p.getName()).append("\n");
                    }

                    if (MessageDialog.openQuestion(getShell(), Messages.wizInstallAddonTitle, NLS.bind(Messages.wizInstallMissingFeatures, sb.toString()))) {
                        selectedAddOn.addAll(addList);
                    }
                }
            }
        }

        super.exit();
    }

    @Override
    protected boolean isCoreRuntimeExpected() {
        return false;
    }

    private List<IProduct> getRequiredFeatures(List<IProduct> productList) {
        if (productList == null || productList.isEmpty())
            return Collections.emptyList();

        List<IProduct> requireList = new ArrayList<IProduct>();
        for (IProduct product : productList) {
            List<String> featureList = product.getRequireFeature();
            if (featureList == null || featureList.isEmpty())
                continue;

            for (String f : featureList) {
                for (IProduct p : allApplicableAddOnList) {
                    if (p.getType() == IProduct.Type.FEATURE) {
                        if (f.equals(p.getProvideFeature().get(0)) && !isInstalled(p)) {
                            requireList.add(p);
                        }
                    }
                }
            }
        }
        return requireList;
    }

    @Override
    protected List<IProduct> getApplicableAddOns(ISite site) {
        return site.getConfigSnippetProducts(new NullProgressMonitor());
    }
}

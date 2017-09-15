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
package com.ibm.ws.st.ui.internal.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.config.EnableCustomEncryptionDialog;

public abstract class UtilityWizardPage extends WizardPage {
    protected WebSphereRuntime wsRuntime;
    protected WebSphereServerInfo server;
    protected WebSphereServer wsServer;
    // Check for whether to add the generated config file as an include to the server.xml
    protected boolean doInclude = false;

    // server.xml
    protected final ConfigurationFile serverConfig;

    // directory of server.xml
    protected final IPath configDir;

    public UtilityWizardPage(WebSphereServerInfo server) {
        super("utility");
        this.wsRuntime = server.getWebSphereRuntime();
        this.server = server;
        wsServer = WebSphereUtil.getWebSphereServer(server);
        serverConfig = server.getConfigRoot();
        configDir = serverConfig.getPath().removeLastSegments(1);
        // Include file option only available as of Liberty 8.5.5.2 and up
        doInclude = WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", wsRuntime.getRuntimeVersion());
    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 3);
        comp.setLayout(layout);

        Label label = new Label(comp, SWT.WRAP);
        label.setText(getUserMessage());
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalSpan = 3;
        label.setLayoutData(data);
        Point p = label.computeSize(200, SWT.DEFAULT);
        label.setSize(p);

        // add an invisible separator to add a few pixels of empty space under the message
        label = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
        label.setVisible(false);

        createUtilityControl(comp);

        Dialog.applyDialogFont(comp);

        setPageComplete(validate());
        setControl(comp);
    }

    protected abstract String getUserMessage();

    protected abstract void createUtilityControl(Composite comp);

    protected boolean validate() {
        setMessage(null, IMessageProvider.NONE);
        return true;
    }

    public boolean preFinish() {
        return true;
    }

    public abstract void finish(IProgressMonitor monitor) throws Exception;

    protected boolean notifyRemoteConfigOverwrite(String utilityTitle) {
        if (!wsServer.isLocalSetup() && wsServer.getServer().getServerState() == IServer.STATE_STOPPED) {
            return MessageDialog.openQuestion(getShell(), utilityTitle, Messages.notifyOverwriteRemoteConfigFile);
        }
        return true;
    }

    /**
     * Returns whether the algorithm is the custom encryption.
     */
    protected boolean isCustomPasswordEncryption(String encodingMethod) {
        if (!"xor".equals(encodingMethod) && !"aes".equals(encodingMethod)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the custom feature name from encoding method.
     */
    protected String getUserFeatureName(String encodingMethod) {
        String featureName = null;
        if (wsRuntime != null) {
            Map<String, CustomPasswordEncryptionInfo> customEncryptionMap = wsRuntime.listCustomEncryption();
            CustomPasswordEncryptionInfo cpei = customEncryptionMap.get(encodingMethod);
            if (cpei != null) {
                featureName = cpei.getFeatureName();
            }
        }
        return featureName;
    }

    /**
     * Returns whether the feature is enabled.
     */
    protected boolean isFeatureEnabled(String feature) {
        return serverConfig.hasFeature(feature);
    }

    protected void enableFeature(final String itemName, final String featureName, final IProgressMonitor monitor) {
        List<String> featureList = new ArrayList<String>();
        featureList.add(featureName);
        final List<String> featureListFinal = featureList;

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                EnableCustomEncryptionDialog enableDialog = new EnableCustomEncryptionDialog(getShell(), wsRuntime, itemName, featureListFinal);
                if (enableDialog.open() == IStatus.OK) {
                    List<String> features = enableDialog.getSelectedFeatures();
                    if (!features.isEmpty()) {
                        serverConfig.addFeatures(features);
                    }
                }
            }
        });

    }

}

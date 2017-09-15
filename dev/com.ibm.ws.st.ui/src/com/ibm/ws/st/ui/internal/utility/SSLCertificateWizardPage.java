/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.utility;

import java.io.File;
import java.net.InetAddress;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.config.PasswordComponent;

/**
 * Future TODO: the script output recommends adding the following lines to the server.xml,
 * and we could do this automatically:
 * <featureManager>
 * <feature>ssl-1.0</feature>
 * </featureManager>
 * <keyStore id="defaultKeyStore" password="<encoded password>" />
 */
public class SSLCertificateWizardPage extends UtilityWizardPage {

    public static final String SSL_INCLUDE_FILE = Constants.GENERATED_SSL_INCLUDE;

    protected PasswordComponent passwordComp;
    protected String validity;
    protected String subject;

    public SSLCertificateWizardPage(WebSphereServerInfo server) {
        super(server);
        setTitle(Messages.wizSSLCertificateTitle);
        setDescription(Messages.wizSSLCertificateDescription);
    }

    @Override
    public void createUtilityControl(Composite comp) {
        Map<String, CustomPasswordEncryptionInfo> customEncryptionMap = (wsRuntime != null) ? wsRuntime.listCustomEncryption() : null;
        passwordComp = new PasswordComponent(comp, Messages.keystorePassword, 3, server.getWebSphereRuntime(), false, customEncryptionMap);
        passwordComp.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                setPageComplete(validate());
            }
        });

        final Button validityCheck = new Button(comp, SWT.CHECK);
        validityCheck.setText(Messages.wizSSLCertificateValidity);
        GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.verticalIndent = 8;
        data.horizontalSpan = 2;
        validityCheck.setLayoutData(data);

        final Text validityText = new Text(comp, SWT.BORDER);
        data = new GridData(GridData.FILL, SWT.CENTER, true, false);
        data.verticalIndent = 8;
        validityText.setLayoutData(data);
        validityText.setText("365");
        validityText.setEnabled(false);
        validityText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                validity = validityText.getText();
                setPageComplete(validate());
            }
        });

        validityCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                validityText.setEnabled(validityCheck.getSelection());
                if (validityCheck.getSelection())
                    validity = validityText.getText();
                else
                    validity = null;
                setPageComplete(validate());
            }
        });

        final Button subjectCheck = new Button(comp, SWT.CHECK);
        subjectCheck.setText(Messages.wizSSLCertificateSubject);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.verticalIndent = 8;
        data.horizontalSpan = 2;
        subjectCheck.setLayoutData(data);

        final Text subjectText = new Text(comp, SWT.BORDER);
        data = new GridData(GridData.FILL, SWT.CENTER, true, false);
        data.verticalIndent = 8;
        subjectText.setLayoutData(data);
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String s = "CN=" + addr.getHostName() + ",OU=" + server.getServerName() + ",O=ibm,C=us";
            subjectText.setText(s);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine host name", e);
        }
        subjectText.setEnabled(false);
        subjectText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                subject = subjectText.getText();
                setPageComplete(validate());
            }
        });

        subjectCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                subjectText.setEnabled(subjectCheck.getSelection());
                if (subjectCheck.getSelection())
                    subject = subjectText.getText();
                else
                    subject = null;
                setPageComplete(validate());
            }
        });

        passwordComp.setFocus();
    }

    @Override
    protected String getUserMessage() {
        return Messages.wizSSLCertificateMessage;
    }

    @Override
    protected boolean validate() {

        String password = passwordComp.getPassword();
        if (password != null && !password.isEmpty() && password.length() < 6) {
            setMessage(Messages.errorPassword, IMessageProvider.ERROR);
            return false;
        }
        if (validity != null && !validity.isEmpty()) {
            try {
                if (Integer.parseInt(validity) < 365) {
                    setMessage(Messages.errorSSLValidity, IMessageProvider.ERROR);
                    return false;
                }
            } catch (NumberFormatException e) {
                setMessage(Messages.errorSSLValidity, IMessageProvider.ERROR);
                return false;
            }
        }
        if (subject != null && !subject.isEmpty() && subject.length() < 4) {
            setMessage(Messages.errorSSLSubject, IMessageProvider.ERROR);
            return false;
        }
        if (password == null || password.isEmpty()) {
            setMessage(null, IMessageProvider.ERROR);
            return false;
        }
        if (validity != null && validity.isEmpty()) {
            setMessage(null, IMessageProvider.ERROR);
            return false;
        }
        if (subject != null && subject.isEmpty()) {
            setMessage(null, IMessageProvider.ERROR);
            return false;
        }
        return super.validate();
    }

    @Override
    public boolean preFinish() {
        // If the user deselects the include checkbox or include is not supported by the runtime then
        // no need to do pre-finish checks
        if (!doInclude)
            return true;

        if (!notifyRemoteConfigOverwrite(Messages.wizSSLCertificateTitle))
            return false;

        boolean overwrite = true;

        File keyFile = configDir.append("resources/security/key.jks").toFile();
        File configFile = configDir.append(SSL_INCLUDE_FILE).toFile();

        StringBuilder sb = new StringBuilder();
        // KeyStore
        if (keyFile.exists())
            sb.append(keyFile.getAbsolutePath() + "\n");
        // Include file
        if (configFile.exists())
            sb.append(configFile.getAbsolutePath() + "\n");
        // If one or more of the above files exists prompt the user
        if (sb.length() > 0) {
            final String existingFiles = sb.toString();
            overwrite = MessageDialog.openQuestion(getShell(), Messages.wizSSLCertificateTitle, NLS.bind(Messages.overwriteExistingFiles, existingFiles));

            if (wsServer != null && wsServer.isLocalSetup()) {
                // If the user doesn't want to overwrite the files then do not proceed with the generation
                if (!overwrite)
                    return false;

                if (keyFile.exists()) {
                    if (!keyFile.renameTo(new File(keyFile.getAbsolutePath() + ".tmp"))) {
                        MessageDialog.openError(getShell(), Messages.wizSSLCertificateTitle, NLS.bind(Messages.errorBackupFile, keyFile.getAbsolutePath()));
                        return false;
                    }
                }
                if (configFile.exists()) {
                    if (!configFile.renameTo(new File(configFile.getAbsolutePath() + ".tmp"))) {
                        MessageDialog.openError(getShell(), Messages.wizSSLCertificateTitle, NLS.bind(Messages.errorBackupFile, configFile.getAbsolutePath()));
                        return false;
                    }
                }
            } else { //remote case we dont want to generate temp files here, that is done in the RemoteCreateSSLCertificate class
                // If the user doesn't want to overwrite the files then do not proceed with the generation
                if (!overwrite)
                    return false;
            }
        }
        return true;
    } // end preFinish

    @Override
    public void finish(IProgressMonitor monitor) throws Exception {
        int validityInt = -1;
        File configFile = configDir.append(SSL_INCLUDE_FILE).toFile();

        try {
            validityInt = Integer.parseInt(validity);
        } catch (NumberFormatException e) {
            // ignore
        }
        String password = passwordComp.getPassword();
        String passwordEncoding = passwordComp.getPasswordEncoding();
        String passwordKey = passwordComp.getPasswordKey();

        // Only generate the include file if the user selects the include checkbox
        String includeOption = doInclude ? SSL_INCLUDE_FILE : null;

        // Run the utility to create the SSL certificate
        ILaunch launch = wsRuntime.createSSLCertificate(server, password, passwordEncoding, passwordKey, validityInt, subject, includeOption, monitor);

        if (launch == null || monitor.isCanceled())
            return;

        if (wsServer != null && wsServer.isLocalSetup()) {
            // Wait for process to complete
            while (!launch.isTerminated() && !monitor.isCanceled()) {
                Thread.sleep(500);
            }

            if (monitor.isCanceled())
                return;

            // Check if the generated include file exists
            if (doInclude) {
                File keyFile = configDir.append("resources/security/key.jks").toFile();
                File tempConfig = new File(configFile.getAbsolutePath() + ".tmp");
                File tempKey = new File(keyFile.getAbsolutePath() + ".tmp");
                if (configFile.exists()) {
                    boolean includeExists = false;

                    // Include the generated config file in the server config if it isn't already there
                    for (ConfigurationFile config : serverConfig.getAllIncludedFiles()) {
                        if (config.getPath().toOSString().equals(configDir.append(SSL_INCLUDE_FILE).toOSString())) {
                            includeExists = true;
                            break;
                        }
                    }
                    // Add the include to the server config if it isn't already added
                    if (!includeExists) {
                        serverConfig.addInclude(false, SSL_INCLUDE_FILE);
                        serverConfig.save(monitor);
                    }
                    //If the custom password encryption is selected, check whether the feature is enabled.
                    if (isCustomPasswordEncryption(passwordEncoding)) {
                        String featureName = getUserFeatureName(passwordEncoding);
                        if (!isFeatureEnabled(featureName)) {
                            enableFeature(Messages.keystorePassword, featureName, monitor);
                            if (isFeatureEnabled(featureName)) {
                                // if the serverConfig object is updated, save it.
                                serverConfig.save(monitor);
                            }
                        }
                    }
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openInformation(getShell(), Messages.wizSSLCertificateTitle, Messages.serverXMLUpdated);
                        }
                    });

                    // Clean up any temp files
                    if (tempKey.exists()) {
                        tempKey.delete();
                    }
                    if (tempConfig.exists()) {
                        tempConfig.delete();
                    }
                } else {
                    // Expected the config file to exist so something must have gone wrong during generation,
                    // therefore, restore the backup file.
                    if (tempConfig.exists()) {
                        tempConfig.renameTo(configFile);
                    }
                    if (tempKey.exists()) {
                        tempKey.renameTo(keyFile);
                    }
                }
            }
        } // end finish
    }
}

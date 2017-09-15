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

package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;

/**
 * Create SSL Certificate Utility for the base Liberty case
 */
public class LibertySSLCreateCertificationUtility {

    public ILaunch createSSLCertificate(WebSphereRuntime websphereRuntime, WebSphereServerInfo serverInfo, String password, String passwordEncoding, String passwordKey,
                                        int validity, String subject,
                                        String includeFileName, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        websphereRuntime.verifyServerExists(serverInfo);

        String serverName = serverInfo.getServerName();
        monitor2.beginTask(NLS.bind(Messages.taskCreateSSLCertificate, serverName), 200);

        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        try {
            ILaunchConfiguration lc = null;
            if (wsServer.isLocalSetup()) {
                List<String> command = new ArrayList<String>();
                command.add(CommandConstants.CREATE_SSL_CERTIFICATE_SECURITY_UTIL);
                command.add(CommandConstants.CREATE_SSL_CERTIFICATE);
                command.add(CommandConstants.CREATE_SSL_CERTIFICATE_SERVER + serverInfo.getServerName());
                command.add(CommandConstants.PASSWORD + password);
                if (!websphereRuntime.getRuntimeVersion().startsWith("8.5.0")) { // not supported until 8.5.5
                    if (passwordEncoding != null && !passwordEncoding.isEmpty())
                        command.add(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_ENCODING + passwordEncoding);
                    if (passwordKey != null && !passwordKey.isEmpty())
                        command.add(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_KEY + passwordKey);
                }
                if (WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", websphereRuntime.getRuntimeVersion())) { // not supported until 8.5.5.2
                    if (includeFileName != null)
                        command.add(CommandConstants.CREATE_CONFIG_FILE + includeFileName);
                }
                if (validity >= 0)
                    command.add(CommandConstants.CREATE_SSL_CERTIFICATE_VALIDITY + validity);
                if (subject != null)
                    command.add(CommandConstants.CREATE_SSL_CERTIFICATE_SUBJECT + subject);
                lc = websphereRuntime.createUtilityLaunchConfig(UtilityLaunchFactory.getLaunchConfigurationType(wsServer.getServerType()), serverInfo, null,
                                                                command.toArray(new String[command.size()]));
            } else { //remote case
                Map<String, String> commandVariables = new HashMap<String, String>();
                commandVariables.put(CommandConstants.UTILITY_TYPE, CommandConstants.CREATE_SSL_CERTIFICATE);
                commandVariables.put(CommandConstants.PASSWORD, password);
                if (!websphereRuntime.getRuntimeVersion().startsWith("8.5.0")) { // not supported until 8.5.5
                    if (passwordEncoding != null && !passwordEncoding.isEmpty())
                        commandVariables.put(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_ENCODING, passwordEncoding);
                    if (passwordKey != null && !passwordKey.isEmpty())
                        commandVariables.put(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_KEY, passwordKey);
                }
                if (WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", websphereRuntime.getRuntimeVersion())) { // not supported until 8.5.5.2
                    if (includeFileName != null)
                        commandVariables.put(CommandConstants.CREATE_CONFIG_FILE, includeFileName);
                }
                if (validity >= 0)
                    commandVariables.put(CommandConstants.CREATE_SSL_CERTIFICATE_VALIDITY, Integer.toString(validity));
                if (subject != null)
                    commandVariables.put(CommandConstants.CREATE_SSL_CERTIFICATE_SUBJECT, subject);
                lc = websphereRuntime.createRemoteUtilityLaunchConfig(UtilityLaunchFactory.getLaunchConfigurationType(wsServer.getServerType()), serverInfo, commandVariables);
            }
            return lc.launch(ILaunchManager.RUN_MODE, monitor2);
        } catch (Throwable t) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorCreateSSLCertificate, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
        }
    }

}

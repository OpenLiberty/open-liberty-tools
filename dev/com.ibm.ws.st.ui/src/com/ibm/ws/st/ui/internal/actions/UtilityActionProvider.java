/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.download.SiteHelper;

public class UtilityActionProvider extends CommonActionProvider {
    public static final String UTILITY_MENU_PATH = "utility";
    protected NewExtendedConfigAction[] createConfigActions = new NewExtendedConfigAction[1];
    protected NewExtendedConfigAction[] newServerEnvActions = new NewExtendedConfigAction[2];
    protected NewExtendedConfigAction[] newJVMOptionsActions = new NewExtendedConfigAction[4];
    protected PackageAction packageAction;
    protected PluginConfigAction pluginConfigAction;
    protected DumpAction dumpAction;
    protected SSLCertificateAction generateSSLAction;
    protected ConfigSnippetAction configSnippetAction;
    protected NewConfigDropinAction[] newConfigDropinActions = new NewConfigDropinAction[2];

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        Shell shell = aSite.getViewSite().getShell();
        StructuredViewer viewer = aSite.getStructuredViewer();
        ISelectionProvider selectionProvider = aSite.getStructuredViewer();

        createConfigActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE, selectionProvider, viewer);

        newServerEnvActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.SERVER_ENV_FILE, selectionProvider, viewer, Constants.SERVER_CONFIG_VAR);
        newServerEnvActions[1] = new NewExtendedConfigAction(ExtendedConfigFile.SERVER_ENV_FILE, selectionProvider, viewer, Constants.WLP_USER_DIR_VAR + "/"
                                                                                                                            + Constants.SHARED_FOLDER);

        newJVMOptionsActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selectionProvider, viewer, Constants.SERVER_CONFIG_VAR);
        newJVMOptionsActions[1] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selectionProvider, viewer, Constants.WLP_USER_DIR_VAR + "/"
                                                                                                                              + Constants.SHARED_FOLDER);
        newJVMOptionsActions[2] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selectionProvider, viewer, Constants.SERVER_CONFIG_VAR + "/"
                                                                                                                              + Constants.CONFIG_DROPINS_FOLDER + "/"
                                                                                                                              + Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
        newJVMOptionsActions[3] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selectionProvider, viewer, Constants.SERVER_CONFIG_VAR + "/"
                                                                                                                              + Constants.CONFIG_DROPINS_FOLDER + "/"
                                                                                                                              + Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);

        newConfigDropinActions[0] = new NewConfigDropinAction(NewConfigDropinAction.DropinType.DEFAULTS, selectionProvider, viewer);
        newConfigDropinActions[1] = new NewConfigDropinAction(NewConfigDropinAction.DropinType.OVERRIDES, selectionProvider, viewer);

        packageAction = new PackageAction(shell, selectionProvider);
        pluginConfigAction = new PluginConfigAction(shell, selectionProvider);
        dumpAction = new DumpAction(shell, selectionProvider);
        generateSSLAction = new SSLCertificateAction(shell, selectionProvider);
        configSnippetAction = new ConfigSnippetAction(shell, selectionProvider);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        IMenuManager newMenu = menu.findMenuUsingPath("org.eclipse.wst.server.ui.internal.cnf.newMenuId");
        if (newMenu != null) {
            MenuManager configMenu = new MenuManager(Messages.menuNewExtendedConfig, Activator.getImageDescriptor(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE), "extendedConfig");
            for (int i = 0; i < createConfigActions.length; i++)
                configMenu.add(createConfigActions[i]);
            // Add the submenu for server.env
            MenuManager subConfigMenu = new MenuManager(ExtendedConfigFile.SERVER_ENV_FILE, Activator.getImageDescriptor(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE), ExtendedConfigFile.SERVER_ENV_FILE);
            for (int i = 0; i < newServerEnvActions.length; i++)
                subConfigMenu.add(newServerEnvActions[i]);
            configMenu.add(subConfigMenu);
            // Add the submenu for jvm.options
            subConfigMenu = new MenuManager(ExtendedConfigFile.JVM_OPTIONS_FILE, Activator.getImageDescriptor(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE), ExtendedConfigFile.JVM_OPTIONS_FILE);
            for (int i = 0; i < newJVMOptionsActions.length; i++)
                subConfigMenu.add(newJVMOptionsActions[i]);
            configMenu.add(subConfigMenu);

            newMenu.add(configMenu);

            MenuManager configDropinsMenu = new MenuManager(Messages.menuNewConfigDropin, Activator.getImageDescriptor(Activator.IMG_CONFIG_FILE), "configDropins");
            for (int i = 0; i < newConfigDropinActions.length; i++) {
                if (newConfigDropinActions[i].isApplicable())
                    configDropinsMenu.add(newConfigDropinActions[i]);
            }
            newMenu.add(configDropinsMenu);
        }

        // if the utility menu already exists then reuse it
        IMenuManager utilityMenu = menu.findMenuUsingPath(UTILITY_MENU_PATH);
        if (utilityMenu == null)
            utilityMenu = new MenuManager(Messages.menuUtilities, UTILITY_MENU_PATH);

        addAction(utilityMenu, generateSSLAction);

        addAction(utilityMenu, pluginConfigAction);

        utilityMenu.add(new GroupMarker("packageserver"));

        addAction(utilityMenu, packageAction);

        utilityMenu.add(new Separator());
        addAction(utilityMenu, dumpAction);
        if (SiteHelper.downloadAndInstallSupported()) {
            utilityMenu.add(new Separator());
            addAction(utilityMenu, configSnippetAction);
        }

        menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, utilityMenu);
        //menu.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, utilityMenu);
        //menu.insertBefore("org.eclipse.wst.server.ui.internal.cnf.serverEtcSectionEnd", utilityMenu);
    }

    /**
     * @param utilityMenu
     * @param generateSSLAction2
     */
    private void addAction(IMenuManager utilityMenu, WebSphereUtilityAction action) {
        WebSphereServer wsServer = action.getServer();
        // remote utilities should only be added if they're supported
        if (wsServer == null || (!wsServer.isLocalSetup() && !action.isUtilityRemoteSupported()))
            return;
        utilityMenu.add(action);
    }
}

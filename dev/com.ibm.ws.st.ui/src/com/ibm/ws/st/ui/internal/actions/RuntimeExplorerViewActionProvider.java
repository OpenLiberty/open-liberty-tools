/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.download.SiteHelper;

public class RuntimeExplorerViewActionProvider extends CommonActionProvider {
    protected OpenConfigFileAction openAction;
    protected ShowInExplorerAction showInExplorerAction;
    protected ShowInFilesystemAction showInFilesystemAction;
    protected RefreshConfigFileAction refreshConfigAction;
    protected RefreshAction refreshAction;
    protected DeleteAction deleteAction;
    protected NewRuntimeAction newRuntimeAction;
    protected EditRuntimeAction editRuntimeAction;
    protected NewServerAction newServerAction;
    protected NewWebSphereServerAction newWebSphereServerAction;
    protected NewQuickServerAction newQuickServerAction;
    protected ShowInServersAction showInServersAction;
    protected PropertiesAction propertiesAction;
    protected NewExtendedConfigAction[] createConfigActions = new NewExtendedConfigAction[1];
    protected NewExtendedConfigAction[] newServerEnvActions = new NewExtendedConfigAction[2];
    protected NewExtendedConfigAction[] newJVMOptionsActions = new NewExtendedConfigAction[4];
    protected NewConfigDropinAction[] newConfigDropinActions = new NewConfigDropinAction[2];
    protected AddOnRuntimeAction addOnRuntimeAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);

        ISelectionProvider selProvider = aSite.getStructuredViewer();
        Shell shell = aSite.getViewSite().getShell();
        StructuredViewer viewer = aSite.getStructuredViewer();
        openAction = new OpenConfigFileAction(selProvider);
        showInExplorerAction = new ShowInExplorerAction(selProvider);
        showInFilesystemAction = new ShowInFilesystemAction(selProvider);
        refreshConfigAction = new RefreshConfigFileAction(selProvider);
        refreshAction = new RefreshAction(selProvider, viewer);
        deleteAction = new DeleteAction(shell, selProvider);

        newRuntimeAction = new NewRuntimeAction(shell);
        editRuntimeAction = new EditRuntimeAction(shell, selProvider, viewer);
        newServerAction = new NewServerAction(selProvider, shell);
        newWebSphereServerAction = new NewWebSphereServerAction(selProvider, shell);
        newQuickServerAction = new NewQuickServerAction(selProvider);

        showInServersAction = new ShowInServersAction(selProvider);
        propertiesAction = new PropertiesAction(selProvider, shell);

        createConfigActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE, selProvider, viewer);

        newServerEnvActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.SERVER_ENV_FILE, selProvider, viewer, Constants.SERVER_CONFIG_VAR);
        newServerEnvActions[1] = new NewExtendedConfigAction(ExtendedConfigFile.SERVER_ENV_FILE, selProvider, viewer, Constants.WLP_USER_DIR_VAR + "/"
                                                                                                                      + Constants.SHARED_FOLDER);

        newJVMOptionsActions[0] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selProvider, viewer, Constants.SERVER_CONFIG_VAR);
        newJVMOptionsActions[1] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selProvider, viewer, Constants.WLP_USER_DIR_VAR + "/"
                                                                                                                        + Constants.SHARED_FOLDER);
        newJVMOptionsActions[2] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selProvider, viewer, Constants.SERVER_CONFIG_VAR + "/"
                                                                                                                        + Constants.CONFIG_DROPINS_FOLDER + "/"
                                                                                                                        + Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
        newJVMOptionsActions[3] = new NewExtendedConfigAction(ExtendedConfigFile.JVM_OPTIONS_FILE, selProvider, viewer, Constants.SERVER_CONFIG_VAR + "/"
                                                                                                                        + Constants.CONFIG_DROPINS_FOLDER + "/"
                                                                                                                        + Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);

        newConfigDropinActions[0] = new NewConfigDropinAction(NewConfigDropinAction.DropinType.DEFAULTS, selProvider, viewer);
        newConfigDropinActions[1] = new NewConfigDropinAction(NewConfigDropinAction.DropinType.OVERRIDES, selProvider, viewer);

        addOnRuntimeAction = new AddOnRuntimeAction(shell, selProvider);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        MenuManager newMenu = new MenuManager(Messages.actionNew, "new");
        newMenu.add(newRuntimeAction);
        if (newServerAction.isEnabled())
            newMenu.add(newServerAction);
        if (newWebSphereServerAction.isEnabled())
            newMenu.add(newWebSphereServerAction);
        if (newQuickServerAction.isEnabled())
            newMenu.add(newQuickServerAction);

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

        menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, newMenu);

        if (openAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);

        if (editRuntimeAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, editRuntimeAction);

        if (showInServersAction.isEnabled() || showInFilesystemAction.isEnabled()) {
            String text = Messages.actionShowIn;
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final IBindingService bindingService = workbench.getAdapter(IBindingService.class);
            final TriggerSequence[] activeBindings = bindingService.getActiveBindingsFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
            if (activeBindings.length > 0)
                text += "\t" + activeBindings[0].format();

            MenuManager showInMenu = new MenuManager(text, IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
            if (showInServersAction.isEnabled())
                showInMenu.add(showInServersAction);
            showInMenu.add(showInExplorerAction);
            showInMenu.add(showInFilesystemAction);
            menu.appendToGroup(ICommonMenuConstants.GROUP_SHOW, showInMenu);
        }

        if (deleteAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, deleteAction);

        if (refreshConfigAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, refreshConfigAction);

        if (refreshAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, refreshAction);

        if (propertiesAction.isEnabled())
            menu.appendToGroup(ICommonMenuConstants.GROUP_PROPERTIES, propertiesAction);

        if (SiteHelper.downloadAndInstallSupported() && addOnRuntimeAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, new Separator());
            menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, addOnRuntimeAction);
        }

    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        actionBars.setGlobalActionHandler("org.eclipse.ui.navigator.Open", openAction);
        if (refreshConfigAction.isEnabled())
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshConfigAction);
        if (refreshAction.isEnabled())
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
        actionBars.updateActionBars();
    }
}

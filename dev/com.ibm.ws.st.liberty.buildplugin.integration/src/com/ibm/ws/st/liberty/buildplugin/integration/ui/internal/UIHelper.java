/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.buildplugin.integration.ui.internal;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.Activator;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.ui.internal.RuntimeExplorerView;

@SuppressWarnings("restriction")
public class UIHelper {

    public static final String RUNTIME_EXPLORER_VIEW_ID = "com.ibm.ws.st.ui.runtime.view";

    public static void openRuntimeExplorerView() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(RUNTIME_EXPLORER_VIEW_ID);
                if (part == null) {
                    try {
                        part = page.showView(RUNTIME_EXPLORER_VIEW_ID);
                    } catch (PartInitException e) {
                        Trace.logError("Could not open runtime explorer view", e);
                    }
                }
            }
        }
    }

    public static void refreshRuntimeExplorerView() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(RUNTIME_EXPLORER_VIEW_ID);
                if (part == null) {
                    try {
                        part = page.showView(RUNTIME_EXPLORER_VIEW_ID);
                    } catch (PartInitException e) {
                        Trace.logError("Could not open runtime explorer view", e);
                    }
                }
                if (part != null) {
                    if (part instanceof RuntimeExplorerView) {
                        RuntimeExplorerView v = (RuntimeExplorerView) part;
                        v.getCommonViewer().refresh();
                    }
                }
            }
        }
    }

    public static boolean handleGenerationPrompt(String promptPreferenceKey, String generationPromptMsg) {
        IPreferenceStore store = Activator.getInstance().getPreferenceStore();
        String val = store.getString(promptPreferenceKey);
        if (!val.isEmpty()) { // empty is the default and we prompt in that case
            // preference is set, reuse the last decision instead of prompting
            if (Messages.yesLabel.equals(val))
                return true;
            if (Messages.noLabel.equals(val))
                return false;
        }
        MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(Display.getDefault().getActiveShell(),
                                                                                   Messages.libertyPromptTitle,
                                                                                   generationPromptMsg,
                                                                                   Messages.generationPromptToggle,
                                                                                   false,
                                                                                   null,
                                                                                   promptPreferenceKey);
        int returnCode = dialog.getReturnCode();
        boolean result = returnCode == 2;
        boolean rememberDecision = dialog.getToggleState();
        if (rememberDecision) {
            String value = result ? Messages.yesLabel : Messages.noLabel;
            store.setValue(promptPreferenceKey, value);
        }
        return result;
    }

}

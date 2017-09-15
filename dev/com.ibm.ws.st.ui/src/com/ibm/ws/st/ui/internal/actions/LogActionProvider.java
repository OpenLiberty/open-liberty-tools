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
package com.ibm.ws.st.ui.internal.actions;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class LogActionProvider extends CommonActionProvider {

    protected OpenMessagesAction openMessagesAction;
    protected OpenTraceLogAction openTraceLogAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        Shell shell = aSite.getViewSite().getShell();
        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        openMessagesAction = new OpenMessagesAction(selectionProvider, shell);
        openTraceLogAction = new OpenTraceLogAction(selectionProvider, shell);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {

        MenuManager openLogs = new MenuManager(Messages.menuOpenLogFiles, "OpenLogFiles");

        openLogs.add(openMessagesAction);

        File[] logFileList = openMessagesAction.getFileList();
        if (logFileList != null && logFileList.length > 0) {
            MenuManager subMenu = new MenuManager(Messages.menuPreviousMessageLogs);
            for (File currentFile : logFileList) {
                LocalFileAction logFileAction = new LocalFileAction(currentFile);
                subMenu.add(logFileAction);
            }
            openLogs.add(subMenu);
        }

        openLogs.add(openTraceLogAction);

        File[] traceFileList = openTraceLogAction.getFileList();
        if (traceFileList != null && traceFileList.length > 0) {
            MenuManager subMenu = new MenuManager(Messages.menuPreviousTraceLogs);
            for (File currentFile : traceFileList) {
                LocalFileAction logFileAction = new LocalFileAction(currentFile);
                subMenu.add(logFileAction);
            }
            openLogs.add(subMenu);
        }

        menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, openLogs);

    }

    private class LocalFileAction extends Action {

        private final File file;

        /** {@inheritDoc} */
        @Override
        public String getText() {
            Path path = new Path(file.getPath());
            return path.lastSegment();
        }

        public LocalFileAction(File file) {
            this.file = file;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            final IPath path = new Path(file.getAbsolutePath());
            final IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchPage page = null;
                    if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                        IWorkbenchWindow window;
                        window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        page = window.getActivePage();
                    }
                    try {
                        if (page != null) {
                            IDE.openEditorOnFileStore(page, fileStore);
                        }

                    } catch (PartInitException e) {
                        Trace.logError("Error Opening " + path.toOSString(), e);
                    }
                }
            });
        }
    }
}
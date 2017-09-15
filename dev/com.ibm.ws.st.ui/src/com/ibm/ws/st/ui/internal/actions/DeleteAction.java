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
package com.ibm.ws.st.ui.internal.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.internal.DeleteRuntimeDialog;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

@SuppressWarnings("restriction")
public class DeleteAction extends SelectionProviderAction {
    private final Shell shell;
    private IRuntime runtime;
    private WebSphereServerInfo server;

    public DeleteAction(Shell shell, ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionDelete);
        this.shell = shell;
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        runtime = null;
        server = null;

        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IRuntime) {
                runtime = (IRuntime) obj;
            } else if (obj instanceof WebSphereServerInfo) {
                server = (WebSphereServerInfo) obj;
                runtime = server.getWebSphereRuntime().getRuntime();
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    @Override
    public void run() {
        if (server != null)
            deleteWebSphereServer(runtime, server);
        else if (runtime != null)
            deleteRuntime(runtime);
    }

    private void deleteRuntime(IRuntime runtime) {
        if (runtime == null)
            return;

        List<IServer> serverList = getServerList();
        // check for use
        boolean inUse = false;
        try {
            inUse = FacetUtil.isRuntimeTargeted(runtime);
        } catch (Throwable t) {
            // ignore - facet framework not found
        }

        if (!serverList.isEmpty() || inUse) {
            DeleteRuntimeDialog dialog = new DeleteRuntimeDialog(shell, !serverList.isEmpty(), inUse);
            if (dialog.open() != 0)
                return;

            if (dialog.isDeleteServers()) {
                List<IServer> runningList = getRunningServerList(serverList);
                try {
                    if (!runningList.isEmpty()) {
                        stopServers(runningList);
                    }
                    deleteServers(serverList);
                } catch (CoreException ce) {
                    MessageDialog.openError(shell, Messages.title, ce.getLocalizedMessage());
                    return;
                }
            }

            if (dialog.isRemoveTargets()) {
                try {
                    FacetUtil.removeTargets(runtime, new NullProgressMonitor());
                } catch (Throwable t) {
                    // facet framework failure, or may be missing entirely
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error deleting facet targets", t);
                }
            }
        } else {
            if (!MessageDialog.openConfirm(shell, Messages.title, NLS.bind(Messages.confirmDelete, runtime.getName())))
                return;
        }

        try {
            runtime.delete();
        } catch (CoreException ce) {
            MessageDialog.openError(shell, Messages.title, ce.getLocalizedMessage());
        } catch (Exception e) {
            Trace.logError("Error deleting runtime: " + runtime.getId(), e);
        }
    }

    private void deleteWebSphereServer(IRuntime runtime, WebSphereServerInfo serverInfo) {
        if (runtime == null)
            return;

        String serverName = serverInfo.getServerName();
        List<IServer> serverList = getServerList(serverInfo);

        if (!serverList.isEmpty()) {
            if (!MessageDialog.openConfirm(shell, Messages.title, NLS.bind(Messages.confirmDeleteServerInUse, serverName)))
                return;

            List<IServer> runningList = getRunningServerList(serverList);
            try {
                if (!runningList.isEmpty()) {
                    stopServers(runningList);
                }
                deleteServers(serverList);
            } catch (CoreException ce) {
                MessageDialog.openError(shell, Messages.title, ce.getLocalizedMessage());
                return;
            }
        } else {
            if (!MessageDialog.openConfirm(shell, Messages.title, NLS.bind(Messages.confirmDelete, serverName)))
                return;
        }

        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (wr == null) {
            MessageDialog.openError(shell, Messages.title, Messages.errorWebsphereRuntimeAdapter);
            return;
        }

        // The server files might be locked, so we keep trying for 3 seconds
        for (int i = 0; i < 11; i++) {
            try {
                wr.deleteServer(serverInfo, null);
                return;
            } catch (Exception e) {
                // We failed to delete the server
                if (i == 11) {
                    MessageDialog.openError(shell, Messages.title, e.getLocalizedMessage());
                } else {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
            }
        }
    }

    private void deleteServers(final List<IServer> serverList) throws CoreException {
        final DeleteActivity activity = new DeleteActivity(Messages.taskDeletingServers);
        Job job = new Job(Messages.taskDeletingServers) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    for (IServer server : serverList)
                        server.delete();

                    activity.setIsDone(true);
                } catch (Exception e) {
                    Trace.logError("Error while deleting servers", e);
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e);
                }

                return Status.OK_STATUS;
            }
        };

        // set rule for workspace and servers
        int size = serverList.size();
        ISchedulingRule[] rules = new ISchedulingRule[size + 1];
        for (int i = 0; i < size; ++i) {
            rules[i] = serverList.get(i);
        }
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        rules[size] = ruleFactory.createRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setRule(MultiRule.combine(rules));
        job.setPriority(Job.BUILD);

        job.schedule();

        try {
            waitForActivity(activity, 500, 10f);
        } catch (TimeoutException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
        }
    }

    private void stopServers(List<IServer> serverList) throws CoreException {
        final DeleteActivity activity = new DeleteActivity("");
        for (IServer server : serverList) {
            activity.setName(NLS.bind(Messages.taskStoppingServer, server.getName()));
            activity.setIsDone(false);
            server.stop(false, new IOperationListener() {
                @Override
                public void done(IStatus status) {
                    if (status.getCode() == (IStatus.ERROR)) {
                        Trace.logError("Error encountered while stopping the server..", status.getException());
                    }
                    activity.setIsDone(true);
                }
            });
            try {
                waitForActivity(activity, 500, 10f);
            } catch (TimeoutException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
            }
        }
    }

    private List<IServer> getServerList() {
        IServer[] servers = ServerCore.getServers();
        if (servers == null) {
            return Collections.emptyList();
        }

        List<IServer> list = new ArrayList<IServer>();
        for (IServer server : servers) {
            if (runtime.equals(server.getRuntime()))
                list.add(server);
        }

        return list;
    }

    private List<IServer> getServerList(WebSphereServerInfo serverInfo) {
        String serverName = serverInfo.getServerName();
        WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
        List<IServer> list = new ArrayList<IServer>(1); // 1-1 mapping
        for (WebSphereServer ws : servers) {
            if (runtime.equals(ws.getServer().getRuntime()) && serverName.equals(ws.getServerName())) {
                IServer server = ws.getServer();
                if (server.isWorkingCopy())
                    server = ((IServerWorkingCopy) server).getOriginal();
                list.add(server);
                break;
            }
        }

        return list;
    }

    private List<IServer> getRunningServerList(List<IServer> serverList) {
        if (serverList.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<IServer> runningList = new ArrayList<IServer>(serverList.size());
        for (IServer server : serverList) {
            if ((server.getServerState() == IServer.STATE_STARTING || server.getServerState() == IServer.STATE_STARTED) && SocketUtil.isLocalhost(server.getHost()))
                runningList.add(server);
        }

        return runningList;
    }

    private void waitForActivity(DeleteActivity activity, int pollingDelay, float timeout) throws TimeoutException {
        int iter = (int) (timeout * 1000f / pollingDelay);
        for (int i = 0; i < iter; i++) {
            try {
                Thread.sleep(pollingDelay);
            } catch (InterruptedException e) {
                // ignore
            }

            if (activity.isDone()) {
                return;
            }
        }
        throw new TimeoutException(NLS.bind(Messages.taskTimeoutError, activity.getName()));
    }

    private static class DeleteActivity {
        private String name;
        private boolean isDone = false;

        protected DeleteActivity(String name) {
            this.name = name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        protected void setIsDone(boolean isDone) {
            this.isDone = isDone;
        }

        protected String getName() {
            return name;
        }

        protected boolean isDone() {
            return isDone;
        }
    }
}

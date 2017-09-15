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
package com.ibm.ws.st.ui.internal.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Generic server utilities.
 */
public class ServerUtil {
    public static IServer[] getServers() {
        IServer[] servers = ServerCore.getServers();
        List<IServer> list = new ArrayList<IServer>(3);
        for (IServer server : servers) {
            IServerType st = server.getServerType();
            if (st != null) {
                if (st.getId().startsWith(Constants.SERVER_ID_PREFIX))
                    list.add(server);
            }
        }
        return list.toArray(new IServer[list.size()]);
    }

    public static IRuntime[] getRuntimes() {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        List<IRuntime> list = new ArrayList<IRuntime>(3);
        for (IRuntime runtime : runtimes) {
            if (WebSphereUtil.isWebSphereRuntime(runtime)) {
                list.add(runtime);
            }
        }
        return list.toArray(new IRuntime[list.size()]);
    }

    public static IServer[] getServers(IRuntime runtime) {
        IServer[] servers = ServerCore.getServers();
        if (runtime == null || servers == null)
            return null;

        List<IServer> list = new ArrayList<IServer>();
        for (IServer server : servers) {
            if (runtime.equals(server.getRuntime()))
                list.add(server);
        }

        return list.toArray(new IServer[list.size()]);
    }

    public static void deleteServers(final IServer[] servers, boolean isWaitRequired) throws CoreException {
        final ServerActivity activity = new ServerActivity(Messages.taskDeletingServers);
        Job job = new Job(Messages.taskDeletingServers) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IStatus status = Status.OK_STATUS;
                try {
                    for (IServer server : servers) {
                        server.delete();
                        if (monitor.isCanceled()) {
                            status = Status.CANCEL_STATUS;
                            break;
                        }
                    }
                } catch (Exception e) {
                    Trace.logError("Error while deleting servers", e);
                    status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e);
                }

                activity.setIsDone(true);
                return status;
            }
        };

        // set rule for workspace and servers
        int size = servers.length;
        ISchedulingRule[] rules = new ISchedulingRule[size + 1];
        for (int i = 0; i < size; ++i) {
            rules[i] = servers[i];
        }
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        rules[size] = ruleFactory.createRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setRule(MultiRule.combine(rules));
        job.setPriority(Job.BUILD);

        job.schedule();

        if (isWaitRequired) {
            try {
                waitForActivity(activity, 500, 10f);
            } catch (TimeoutException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
            }
        }
    }

    public static void stopServers(IServer[] servers) throws CoreException {
        final ServerActivity activity = new ServerActivity("");
        for (IServer server : servers) {
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
                waitForActivity(activity, 500, server.getStopTimeout());
            } catch (TimeoutException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
            }
        }
    }

    private static void waitForActivity(ServerActivity activity, int pollingDelay, float timeout) throws TimeoutException {
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

    private static class ServerActivity {
        private String name;
        private boolean isDone = false;

        protected ServerActivity(String name) {
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

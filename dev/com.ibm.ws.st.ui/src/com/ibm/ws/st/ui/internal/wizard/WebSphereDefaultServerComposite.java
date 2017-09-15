/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.AbstractWizardFragment;
import com.ibm.ws.st.ui.internal.download.AddonUtil;
import com.ibm.ws.st.ui.internal.download.LocalProduct;

/**
 *
 */
public class WebSphereDefaultServerComposite extends AbstractWebSphereServerComposite {
    NewServerNameComposite nameComp;
    IRuntime runtime;
    private TaskModel taskModel = null;

    private static class XMLDocUtil extends ConfigUtils {
        static Document getXMLDoc(URI uri) {
            return getDOMFromModel(uri);
        }
    }

    protected WebSphereDefaultServerComposite(Composite parent, IWizardHandle wizard, TaskModel taskModel) {
        super(parent, wizard);
        this.taskModel = taskModel;
        wizard.setDescription(Messages.wizServerDescription);
        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));

        createControl();
    }

    @Override
    protected void createControl() {

        GridLayout gridLayout = new GridLayout();

        setLayout(gridLayout);

        nameComp = new NewServerNameComposite(this, wizard, true);
        // When we are creating a default server, the user did not yet
        // choose a user directory (only the runtime).
        nameComp.setRuntime(runtime, null, null);

        // We need to create a sub composite for the correct indentation.
        Composite subComp = new Composite(this, SWT.NONE);
        subComp.setLayout(new GridLayout());
        subComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        createConfigControl(subComp);
    }

    protected void setRuntime(IRuntime runtime, String archive) {
        this.runtime = runtime;
        // When we are creating a default server, the user did not yet
        // choose a user directory (only the runtime).
        nameComp.setRuntime(runtime, null, archive);
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {

        IRuntime runtime = (IRuntime) taskModel.getObject(TaskModel.TASK_RUNTIME);
        IRuntimeWorkingCopy runtimeWC = runtime.isWorkingCopy() ? (IRuntimeWorkingCopy) runtime : null;
        try {
            // There is a bug in WTP. The same progress monitor is being
            // passed to the various wizard fragments. If we are also
            // creating a new runtime, the runtime create fragment has
            // already used the monitor, so we pass a sub progress monitor
            // instead. Otherwise, we use the monitor that was passed in.
            nameComp.createServer(monitor);
            server.setServerName(nameComp.getServerName());
            server.setUserDir(nameComp.getUserDir());
            removeGeneratedMetaData(runtime);
            // Show success dialog if we are also creating a new runtime
            if (runtimeWC != null && runtimeWC.getOriginal() == null) {
                @SuppressWarnings("unchecked")
                final List<IProduct> selectedList = (List<IProduct>) taskModel.getObject(AbstractDownloadComposite.SELECTED_DOWNLOADERS);
                @SuppressWarnings("unchecked")
                final List<IStatus> result = (List<IStatus>) taskModel.getObject(AbstractDownloadComposite.INSTALL_RESULT);
                // sanity check (should not be null)

                Map<IProduct, IStatus> installResult = new LinkedHashMap<IProduct, IStatus>(selectedList.size());

                //Put the Products and their install status into a map
                Iterator<IProduct> productItr = selectedList.iterator();
                Iterator<IStatus> statusItr = result.iterator();
                while (productItr.hasNext() && statusItr.hasNext())
                    installResult.put(productItr.next(), statusItr.next());
                AddonUtil.showResult(null, installResult);
            }
        } finally {
            // Finish the monitor's task that was started by the runtime
            // create operation
            if (runtimeWC != null && runtimeWC.getOriginal() == null)
                monitor.done();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        IServerWorkingCopy wc = (IServerWorkingCopy) this.taskModel.getObject(TaskModel.TASK_SERVER);
        runtime = (IRuntime) taskModel.getObject(TaskModel.TASK_RUNTIME);

        // If we are using an archive for the runtime, pass the archive location for template
        // extraction
        String archiveSource = (String) this.taskModel.getObject(AbstractWizardFragment.ARCHIVE_SOURCE);
        if (archiveSource == null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) this.taskModel.getObject(AbstractDownloadComposite.ADDON_MAP);
            if (map != null) {
                IProduct core = (IProduct) map.get(AbstractDownloadComposite.SELECTED_CORE_MANAGER);
                if (core instanceof LocalProduct)
                    archiveSource = core.getSource().getLocation();
            }
        }
        setRuntime(runtime, archiveSource);
        // When we are creating a default server, the user did not yet
        // choose a user directory (only the runtime).
        serverWC = wc;
        server = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);

        if (runtime == null)
            return;
        IPath templatePath = runtime.getLocation();
        if (templatePath == null)
            return;

        templatePath = templatePath.append("templates/servers/defaultServer/server.xml");
        if (!templatePath.toFile().exists()) {
            setConfigControlVisible(false);
            return;
        }

        setConfigControlVisible(true);
        final WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        final boolean metadataDirExistsBefore = (wsRuntime == null) ? false : wsRuntime.metadataDirectoryExists();

        Document document = XMLDocUtil.getXMLDoc(templatePath.toFile().toURI());
        if (document != null) {
            treeViewer.setInput(document.getDocumentElement()); // will cause metadata to be generated for new runtime
            // meta data did not exist before and was generated, so keep track
            // of the runtime id
            if (!metadataDirExistsBefore && (wsRuntime != null && wsRuntime.metadataDirectoryExists())) {
                addMetaDataRuntimeId(runtime);
            }
        } else {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Default server template document is null. The default features will not be shown in the Server Creation wizard.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void validate() {
        if (nameComp != null)
            nameComp.validate();
    }

    @Override
    public boolean isComplete() {
        return (nameComp == null || nameComp.isValid());
    }

}

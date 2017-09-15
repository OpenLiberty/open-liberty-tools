/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.core.internal.SetLooseConfigCommand;
import com.ibm.ws.st.core.internal.SetPublishWithErrorCommand;
import com.ibm.ws.st.core.internal.SetStopServerOnShutdownCommand;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 * General server editor section.
 */
public class ServerGeneralEditorSection extends ServerEditorSection {
    protected WebSphereServer wsServer;

    protected Button looseConfigButton;
    protected Button stopOnShutdown;

    protected Label serverName;
    protected boolean updating;
    protected PropertyChangeListener listener;
    protected Button publishWithError;
    public final String PUBLISH_WITH_ERROR = "publishWithError";

    // Keep track of the initial state of the loose config button.   Each time the button is checked/unchecked,
    // the PROP_LOOSE_CONFIG property is changed on the server.  So we can't reliably use that.
    private boolean initialLooseConfigButtonState;

    public ServerGeneralEditorSection() {
        // do nothing
    }

    protected void addChangeListener() {
        listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (updating)
                    return;
                updating = true;
                if (WebSphereServer.PROP_LOOSE_CONFIG.equals(event.getPropertyName())) {
                    Boolean b = (Boolean) event.getNewValue();
                    if (b != null) {
                        looseConfigButton.setSelection(b.booleanValue());
                    }
                }
                updating = false;
            }
        };
        server.addPropertyChangeListener(listener);
    }

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
        section.setText(Messages.editorGeneralTitle);
        section.setDescription(Messages.editorGeneralDescription);
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
        whs.setHelp(composite, ContextIds.EDITOR_GENERAL);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);

        // server name
        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.server);

        serverName = new Label(composite, SWT.NONE);
        serverName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        label = new Label(composite, SWT.NONE);
        label.setText("");

        // link to edit configuration
        Hyperlink openConfigLink = toolkit.createHyperlink(composite, Messages.editorGeneralOpenConfiguration, SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        openConfigLink.setLayoutData(data);

        openConfigLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent arg0) {
                ConfigurationFile configFile = wsServer.getConfiguration();
                if (configFile != null)
                    Activator.openConfigurationEditor(configFile.getIFile(), configFile.getURI());
            }
        });

        // loose config
        looseConfigButton = toolkit.createButton(composite, Messages.editorGeneralLooseConfig, SWT.CHECK | SWT.WRAP);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalSpan = 2;
        data.verticalIndent = 5;
        looseConfigButton.setLayoutData(data);
        looseConfigButton.setToolTipText(Messages.editorGeneralLooseConfigMessage);
        looseConfigButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                boolean sel = looseConfigButton.getSelection();
                execute(new SetLooseConfigCommand(wsServer, sel));
                updating = false;
            }
        });

        // stop server when workbench shuts down
        stopOnShutdown = toolkit.createButton(composite, Messages.editorGeneralStopServerOnShutdown, SWT.CHECK | SWT.WRAP);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalSpan = 2;
        data.verticalIndent = 5;
        stopOnShutdown.setLayoutData(data);
        stopOnShutdown.setToolTipText(Messages.editorGeneralStopServerOnShutdownMessage);
        stopOnShutdown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                boolean sel = stopOnShutdown.getSelection();
                execute(new SetStopServerOnShutdownCommand(wsServer, sel));
                updating = false;
            }
        });

        //publish project with Error
        publishWithError = toolkit.createButton(composite, Messages.publishWithErrors, SWT.CHECK | SWT.WRAP);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalSpan = 2;
        data.verticalIndent = 5;
        publishWithError.setLayoutData(data);
        publishWithError.setToolTipText(Messages.publishWithErrors);
        //initialize value from the server object
        publishWithError.setSelection(wsServer.isPublishWithError());
        publishWithError.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                boolean sel = publishWithError.getSelection();
                execute(new SetPublishWithErrorCommand(wsServer, sel));
                updating = false;
            }
        });
        initialize();
    }

    @Override
    public void dispose() {
        if (server != null)
            server.removePropertyChangeListener(listener);
        super.dispose();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        if (server != null) {
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            addChangeListener();
            if (input instanceof IServerEditorPartInput) {
                readOnly = ((IServerEditorPartInput) input).isServerReadOnly();
            }
        }
        initialize();
    }

    protected void initialize() {
        if (serverName == null || wsServer == null)
            return;
        serverName.setText(wsServer.getServerName() + "");
        updating = true;
        looseConfigButton.setSelection(wsServer.isLooseConfigEnabled());
        boolean isEnabled = !readOnly && wsServer.isLocalSetup();

        // Give extension ability to override the state of the loose config button
        WebSphereServerBehaviour webSphereServerBehaviour = wsServer.getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            Boolean state = ext.isLooseConfigEnabled(webSphereServerBehaviour);
            if (state != null) {
                isEnabled = state.booleanValue();
            }
        }

        looseConfigButton.setEnabled(isEnabled);
        if (isEnabled) {
            updateRunWithWorkspaceResourcesState(true);
        }

        initialLooseConfigButtonState = looseConfigButton.getSelection();

        stopOnShutdown.setSelection(wsServer.isStopOnShutdown());
        stopOnShutdown.setEnabled(!readOnly);
        updating = false;
    }

    /**
     * During initialize state, only the field enable is checked. The actual value will preserve.
     *
     * @param isInitialize
     */
    protected void updateRunWithWorkspaceResourcesState(boolean isInitialize) {
        if (looseConfigButton == null)
            return;

        looseConfigButton.setEnabled(true);
        if (!isInitialize && wsServer != null) {
            wsServer.setLooseConfigEnabled(true);
            looseConfigButton.setSelection(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSave(IProgressMonitor monitor) {
        // Allow extensions to perform additional changes prior to doSave is called.
        IStatus status = null;
        WebSphereServerBehaviour webSphereServerBehaviour = wsServer.getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            boolean selection = looseConfigButton.getSelection();
            // Only call extensions when the loose config state actually changed
            if (initialLooseConfigButtonState != selection) {
                status = ext.preSaveLooseConfigChange(webSphereServerBehaviour, selection);
                // If the status from extensions is not ok, then don't change the state yet
                if (status != Status.OK_STATUS) {
                    // If cancelled, then reset the loose config mode
                    looseConfigButton.setSelection(!selection);
                    execute(new SetLooseConfigCommand(wsServer, !selection));
                    return;
                }
            }
        }
        // If the editor is saved, then update the last loose config state
        initialLooseConfigButtonState = looseConfigButton.getSelection();
        super.doSave(monitor);
    }
}

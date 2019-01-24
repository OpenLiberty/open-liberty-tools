/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.ui.internal;

import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.jee.core.internal.JEEServerExtConstants;
import com.ibm.ws.st.jee.core.internal.SharedLibertyUtils;

/**
 * Dialog to type in or select a shared library id.
 */
@SuppressWarnings("restriction")
public class SharedLibSelectionDialog extends Dialog {
    protected String id = null;
    protected List<String> currentRefIds;

    Label messageLabel = null;
    Label errorLabel = null;
    TreeItem currentlySelectedItem = null;

    public SharedLibSelectionDialog(Shell parent, List<String> currentRefIds) {
        super(parent);
        this.currentRefIds = currentRefIds;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.sharedLibTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.minimumWidth = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.sharedLibId);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text idText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        idText.setLayoutData(data);

        idText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                id = idText.getText();
                validate();
            }
        });

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.sharedLibExisting);
        data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        label.setLayoutData(data);

        final Tree libTree = new Tree(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 300;
        data.heightHint = 150;
        libTree.setLayoutData(data);

        boolean found = false;
        Color bg = libTree.getBackground();
        Color fg = libTree.getForeground();
        final Color gray = new Color(bg.getDevice(), (bg.getRed() + fg.getRed()) / 2, (bg.getGreen() + fg.getGreen()) / 2, (bg.getBlue() + fg.getBlue()) / 2);
        libTree.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                gray.dispose();
            }
        });

        TreeItem workspaceItem = new TreeItem(libTree, SWT.NONE);
        workspaceItem.setText(Messages.sharedLibWorkspace);
        Image img = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
        workspaceItem.setImage(img);
        workspaceItem.setForeground(gray);

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            Properties settings = SharedLibertyUtils.getUtilPrjSharedLibInfo(project);
            String libId = settings.getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, null);
            if (libId != null) {
                TreeItem item = new TreeItem(workspaceItem, SWT.NONE);
                item.setText(NLS.bind(Messages.sharedLibProject, libId, project.getName()));
                item.setImage(Activator.getImage(Activator.IMG_LIBRARY));
                item.setData(libId);
                if (currentRefIds.contains(libId))
                    item.setForeground(gray);
                found = true;
            }
        }

        if (!found) {
            TreeItem item = new TreeItem(workspaceItem, SWT.NONE);
            item.setText(Messages.sharedLibNone);
            item.setForeground(gray);
        }
        workspaceItem.setExpanded(true);

        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo ws : servers) {
            TreeItem serverItem = new TreeItem(libTree, SWT.NONE);
            serverItem.setText(NLS.bind(Messages.sharedLibServer, ws.getServerName()));
            serverItem.setImage(Activator.getImage(Activator.IMG_SERVER));
            serverItem.setForeground(gray);
            serverItem.setData(ws);

            ConfigurationFile configFile = ws.getConfigRoot();
            String[] ids = ConfigUtils.getSharedLibraryIds(configFile);
            if (ids != null && ids.length > 0) {
                for (String s : ids) {
                    TreeItem item = new TreeItem(serverItem, SWT.NONE);
                    item.setText(s);
                    item.setImage(Activator.getImage(Activator.IMG_LIBRARY));
                    item.setData(s);
                    if (currentRefIds.contains(s))
                        item.setForeground(gray);
                    found = true;
                }
            } else {
                TreeItem item = new TreeItem(serverItem, SWT.NONE);
                item.setText(Messages.sharedLibNone);
                item.setForeground(gray);
            }
            serverItem.setExpanded(true);
        }

        libTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                TreeItem[] items = libTree.getSelection();
                if (items == null || items.length != 1)
                    return;

                currentlySelectedItem = items[0];

                Object obj = items[0].getData();
                if (obj == null || !(obj instanceof String)) {
                    idText.setText("");
                    return;
                }

                idText.setText((String) obj);
            }
        });

        libTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent event) {
                if (isOKEnabled()) {
                    okPressed();
                    close();
                }
            }
        });

        // Validation message composite
        Composite messageComposite = new Composite(parent, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        messageComposite.setLayout(layout);

        data = new GridData(GridData.FILL_BOTH);
        messageComposite.setLayoutData(data);

        Image errorIcon = Activator.getImage(Activator.IMG_ERROR);
        errorLabel = new Label(messageComposite, SWT.NONE);
        errorLabel.setImage(errorIcon);
        errorLabel.setVisible(false);
        data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        errorLabel.setLayoutData(data);

        messageLabel = new Label(messageComposite, SWT.WRAP);
        messageLabel.setText("");
        messageLabel.setVisible(false);
        data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.widthHint = 500;
        data.grabExcessVerticalSpace = true;
        data.horizontalIndent = PopupDialog.POPUP_HORIZONTALSPACING;
        data.verticalIndent = PopupDialog.POPUP_VERTICALSPACING;
        messageLabel.setLayoutData(data);

        return composite;
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        validate();
        return control;
    }

    protected void validate() {
        boolean ok = false;
        if (id != null && !id.trim().isEmpty())
            ok = !currentRefIds.contains(id);

        String validationMsg = "";
        if (ok) {
            validationMsg = validateServer();
            ok = validationMsg.isEmpty();
        }
        handleValidationMessages(validationMsg);
        getButton(IDialogConstants.OK_ID).setEnabled(ok);
    }

    private String validateServer() {
        if (currentlySelectedItem != null) {
            TreeItem parent = currentlySelectedItem.getParentItem();
            if (parent != null) {
                WebSphereServerInfo wsInfo = (WebSphereServerInfo) parent.getData();
                if (wsInfo != null) {
                    WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(wsInfo);
                    /*
                     * Maven type servers are currently created in target folders by default so we will block them
                     * and ask the user to manually configure their server using the config files in the src folders
                     */
                    if (wsServer != null && Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(wsServer.getServerType()))
                        return Messages.sharedLibMavenServer;

                }
            }
        }
        return "";
    }

    private void handleValidationMessages(final String msg) {

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                if (messageLabel != null) {
                    errorLabel.setVisible(!msg.isEmpty());
                    messageLabel.setText(msg);
                    messageLabel.setVisible(!msg.isEmpty());
                    Point location = messageLabel.getShell().getLocation(); // preserve location
                    messageLabel.getShell().pack();
                    messageLabel.getShell().setLocation(location);
                }
            }
        });
    }

    public String getId() {
        return id;
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }

    protected boolean isOKEnabled() {
        return getButton(IDialogConstants.OK_ID).isEnabled();
    }
}

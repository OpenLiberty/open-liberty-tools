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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.generation.Metadata;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;

public class RuntimeAdvancedDialog extends TitleAreaDialog {
    protected WebSphereRuntime runtime;

    protected Table userDirTable;
    protected Button clearCacheButton;

    protected Image projectErrorImage;
    protected Image folderErrorImage;

    public RuntimeAdvancedDialog(Shell parentShell, WebSphereRuntime runtime) {
        super(parentShell);
        this.runtime = runtime;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_RUNTIME));
        setTitle(Messages.runtimeAdvancedTitle);
        setMessage(Messages.runtimeAdvancedDescription);

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        Group userDirGroup = new Group(composite, SWT.NONE);
        userDirGroup.setText(Messages.runtimeUserDirGroup);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        userDirGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        userDirGroup.setLayoutData(data);

        userDirTable = new Table(userDirGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 60;
        data.verticalSpan = 2;
        userDirTable.setLayoutData(data);

        final Button userDirNew = SWTUtil.createButton(userDirGroup, Messages.create);
        userDirNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UserDirWizard wizard = new UserDirWizard();
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                dialog.setPageSize(425, 275);
                if (dialog.open() == Window.CANCEL)
                    return;

                IProject project = wizard.getProject();
                if (project != null)
                    runtime.addUserDirectory(project);
                else
                    runtime.addUserDirectory(wizard.getPath());
                updateUserDirs();
            }
        });

        final Button userDirRemove = SWTUtil.createButton(userDirGroup, Messages.remove);
        userDirRemove.setEnabled(false);
        userDirRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] ti = userDirTable.getSelection();
                if (ti == null || ti.length != 1)
                    return;
                UserDirectory ud = (UserDirectory) ti[0].getData();
                runtime.removeUserDirectory(ud);
                updateUserDirs();
                userDirRemove.setEnabled(false);
            }
        });

        userDirTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] ti = userDirTable.getSelection();
                UserDirectory ud = null;
                boolean enabled = false;
                if (ti != null && ti.length == 1) {
                    ud = (UserDirectory) ti[0].getData();
                    IPath runtimeLocation = runtime.getRuntime().getLocation();
                    if (runtimeLocation == null || !ud.getPath().equals(runtimeLocation.append(Constants.USER_FOLDER)))
                        enabled = true;
                }

                setMessage(Messages.runtimeAdvancedDescription, IMessageProvider.NONE);
                if (ud != null) {
                    if (ud.getProject() != null) {
                        if (!ud.getProject().exists())
                            setMessage(Messages.runtimeUserDirNotFound, IMessageProvider.ERROR);
                    } else {
                        if (!ud.getPath().toFile().exists())
                            setMessage(Messages.runtimeUserDirNotFound, IMessageProvider.ERROR);
                    }
                }
                userDirRemove.setEnabled(enabled);
            }
        });

        // create error images
        Image img = Activator.getImage(Activator.IMG_USER_PROJECT);
        projectErrorImage = new Image(img.getDevice(), img.getBounds());
        GC gc = new GC(projectErrorImage);
        gc.drawImage(img, 0, 0);
        gc.drawImage(Activator.getImage(Activator.IMG_ERROR_OVERLAY), 0, 0);
        gc.dispose();

        img = Activator.getImage(Activator.IMG_USER_FOLDER);
        folderErrorImage = new Image(img.getDevice(), img.getBounds());
        gc = new GC(folderErrorImage);
        gc.drawImage(img, 0, 0);
        gc.drawImage(Activator.getImage(Activator.IMG_ERROR_OVERLAY), 0, 0);
        gc.dispose();

        userDirTable.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                projectErrorImage.dispose();
                folderErrorImage.dispose();
            }
        });

        final Label label = new Label(composite, SWT.WRAP);

        IRuntime rt = runtime.getRuntime();
        // do not want to enable button in creation scenario
        boolean enable = !rt.isWorkingCopy() || ((IRuntimeWorkingCopy) rt).getOriginal() != null;

        label.setText(timestampString(enable));
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.widthHint = 250;
        label.setLayoutData(data);

        clearCacheButton = SWTUtil.createButton(composite, Messages.runtimeCacheRefresh);
        ((GridData) clearCacheButton.getLayoutData()).horizontalAlignment = SWT.BEGINNING;
        clearCacheButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshRuntime(label);
            }
        });

        clearCacheButton.setEnabled(enable);

        init();

        return composite;
    }

    void refreshRuntime(final Label label) {
        runtime.generateMetadata(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                try {
                    if (event.getResult().isOK()) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String s = timestampString(true);
                                    if (Trace.ENABLED) {
                                        Trace.trace(Trace.INFO, "Metadata regeneration complete at " + s);
                                    }
                                    label.setText(s);
                                    runtime.fireMetadataRefreshEvent();
                                }
                                catch (SWTException e) {
                                    // nothing
                                }
                            }
                        };
                        Display.getDefault().asyncExec(r);
                    }
                } finally {
                    event.getJob().removeJobChangeListener(this);
                }
            }
        }, true, Metadata.ALL_METADATA);
    }

    @SuppressWarnings("boxing")
    String timestampString(boolean enabled) {
        String s = "";
        if (enabled) {
            URL url = runtime.getConfigurationSchemaURL();
            URL fallbackSchemaURL = WebSphereRuntime.getFallbackSchema();
            if (url != null && !url.sameFile(fallbackSchemaURL)) {
                // schema exists and is not fallback so get the timestamp
                File file = null;
                try {
                    file = new File(url.toURI());
                    if (file.exists()) {
                        long timestamp = file.lastModified();
                        DateFormat df = DateFormat.getDateTimeInstance();
                        s = df.format(timestamp);
                    } else {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Timestamp of last runtime update unavailable because schema does not exist");
                        }
                    }
                } catch (URISyntaxException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Timestamp of last runtime update unavailable because of bad URL for schema", e);
                    }
                }
            } else {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Timestamp of last runtime update unavailable because configuration schema URL is null");
                }
            }
        }
        return NLS.bind(Messages.runtimeCacheDescription, s);
    }

    protected void updateUserDirs() {
        userDirTable.removeAll();

        List<UserDirectory> userDirs = null;
        if (runtime != null)
            userDirs = runtime.getUserDirectories();
        if (userDirs != null) {
            for (UserDirectory ud : userDirs) {
                TableItem item = new TableItem(userDirTable, SWT.NONE);
                item.setData(ud);
                if (ud.getProject() != null) {
                    item.setText(ud.getProject().getName());
                    if (ud.getProject().exists())
                        item.setImage(Activator.getImage(Activator.IMG_USER_PROJECT));
                    else
                        item.setImage(projectErrorImage);
                } else {
                    item.setText(ud.getPath().toOSString());
                    if (ud.getPath().toFile().exists())
                        item.setImage(Activator.getImage(Activator.IMG_USER_FOLDER));
                    else
                        item.setImage(folderErrorImage);
                }
            }
        }
    }

    protected void init() {
        if (userDirTable == null || runtime == null)
            return;

        updateUserDirs();
        userDirTable.select(0);
    }
}

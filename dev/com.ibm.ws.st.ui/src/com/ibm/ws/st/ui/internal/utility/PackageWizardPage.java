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
package com.ibm.ws.st.ui.internal.utility;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContentAssistCombo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

public class PackageWizardPage extends UtilityWizardPage {
    public static final String PREF_PATHS = "package.path";
    private static final String PREF_OVERWRITE = "package.overwrite";
    private static final String PREF_INCLUDE = "package.include";

    private static final String ALL = "all";
    private static final String MINIFY = "minify";
    private static final String USR = "usr";

    protected ContentAssistCombo folder;
    protected String exportPath;
    protected boolean overwrite;
    protected String include;
    protected boolean isServerStarted;
    protected boolean publishServer;

    public PackageWizardPage(WebSphereServerInfo server) {
        super(server);
        setTitle(Messages.wizPackageTitle);
        setDescription(Messages.wizPackageDescription);
    }

    @Override
    public void createUtilityControl(Composite comp) {
        Group archiveGroup = new Group(comp, SWT.NONE);
        archiveGroup.setText(Messages.wizPackageExport);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        archiveGroup.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalSpan = 3;
        archiveGroup.setLayoutData(data);

        List<String> previousLocations = Activator.getPreferenceList(PREF_PATHS);
        folder = new ContentAssistCombo(archiveGroup, previousLocations);
        folder.setLayoutData(new GridData(GridData.FILL, SWT.CENTER, true, false));

        if (previousLocations != null && !previousLocations.isEmpty()) {
            String s = previousLocations.get(0);
            folder.setText(s);
            exportPath = s;
        }

        folder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                exportPath = folder.getText();
                setPageComplete(validate());
            }
        });

        final Button browse = SWTUtil.createButton(archiveGroup, Messages.browse);
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SAVE | SWT.SINGLE);
                dialog.setFilterExtensions(new String[] { "*.zip", "*.jar" });
                String pathString = folder.getText();
                if (pathString != null && !pathString.isEmpty()) {
                    IPath path = new Path(pathString);
                    if (!path.toFile().isDirectory()) {
                        String fileName = path.lastSegment();
                        if (fileName != null && !fileName.isEmpty())
                            dialog.setFileName(fileName);
                        path = path.removeLastSegments(1);
                    }
                    if (!path.isEmpty() && path.toFile().isDirectory())
                        dialog.setFilterPath(path.toOSString());
                }
                String file = dialog.open();
                if (file != null)
                    folder.setText(file);
            }
        });

        final Button overwriteButton = new Button(archiveGroup, SWT.CHECK);
        overwriteButton.setText(Messages.wizPackageOverwrite);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        overwriteButton.setLayoutData(data);

        String overwritePref = Activator.getPreference(PREF_OVERWRITE, null);
        if ("true".equals(overwritePref)) {
            overwriteButton.setSelection(true);
            overwrite = true;
        }

        overwriteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                overwrite = overwriteButton.getSelection();
            }
        });

        Label label = new Label(comp, SWT.NONE);
        label.setText(Messages.wizPackageInclude);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.verticalIndent = 8;
        label.setLayoutData(data);

        final Combo includeCombo = new Combo(comp, SWT.READ_ONLY);
        String version = wsRuntime.getRuntimeVersion();
        boolean includeMinify = !(version != null && version.startsWith("8.5.0"));
        final String[] includeOptions = includeMinify ? new String[] { ALL, MINIFY, USR } : new String[] { ALL, USR };
        includeCombo.add(Messages.wizPackageIncludeAll);
        if (includeMinify)
            includeCombo.add(Messages.wizPackageIncludeMinify);

        includeCombo.add(Messages.wizPackageIncludeUsr);

        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        data.verticalIndent = 8;
        includeCombo.setLayoutData(data);

        includeCombo.select(0);
        String includePref = Activator.getPreference(PREF_INCLUDE, includeOptions[0]);

        for (int i = 0; i < includeOptions.length && include == null; i++) {
            if (includeOptions[i].equals(includePref)) {
                includeCombo.select(i);
                include = includeOptions[i];
            }
        }

        includeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                include = includeOptions[includeCombo.getSelectionIndex()];
            }
        });

        //label is used for layout purposes to maintain consistent spacing within the wizard dialog
        new Label(comp, SWT.NONE);

        label = new Label(comp, SWT.NONE);
        label.setText(Messages.wizPackageIncludeCommonMessage);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        try {
            if (wsServer != null) {
                if (wsServer.isLocalSetup())
                    isServerStarted = wsRuntime.isServerStarted(server, null);
                else
                    isServerStarted = wsServer.getServer().getServerState() == IServer.STATE_STARTED;
            }
        } catch (CoreException ce) {
            // ignore, we have to assume the server is stopped
        }
    }

    @Override
    protected String getUserMessage() {
        return Messages.wizPackageMessage;
    }

    @Override
    protected boolean validate() {
        if (isServerStarted) {
            setMessage(Messages.errorPackageServerRunning, IMessageProvider.ERROR);
            return false;
        }

        if (exportPath == null || exportPath.isEmpty()) {
            setMessage(null, IMessageProvider.ERROR);
            return false;
        }
        // The server package utility will fail if by chance the "File" is a directory with the same name
        // (eg. Directory named "something.zip" in the server output directory and user entered "something" in the textbox).
        // So we will append ".zip" if the path doesn't include it already despite this being an unlikely scenario.
        if (!exportPath.endsWith(Constants.ZIP_EXT) && !exportPath.endsWith(Constants.JAR_EXT)) {
            exportPath = exportPath + Constants.ZIP_EXT;
        }
        File f = new File(exportPath);
        // When the entered path is not an absolute path the command line utility will use the
        // server output path as the working directory
        if (!f.isAbsolute()) {
            f = server.getServerOutputPath().append(exportPath).toFile();
        }
        if (f.exists() && f.isDirectory()) {
            setMessage(Messages.errorPackageInvalidFile, IMessageProvider.ERROR);
            return false;
        }

        return super.validate();
    }

    @Override
    public boolean preFinish() {
        // package utility appends .zip if no archive extension is mentioned
        if (!exportPath.endsWith(Constants.ZIP_EXT) && !exportPath.endsWith(Constants.JAR_EXT)) {
            exportPath = exportPath + Constants.ZIP_EXT;
        }
        File f = new File(exportPath);
        // When the entered path is not an absolute path the command line utility will use the
        // server output path as the working directory
        if (!f.isAbsolute()) {
            f = server.getServerOutputPath().append(exportPath).toFile();
        }

        if (f.exists() && !overwrite) {
            if (!MessageDialog.openConfirm(getShell(), Messages.wizPackageTitle, NLS.bind(Messages.wizPackageFileExists, exportPath)))
                return false;
        }

        String version = wsServer.getWebSphereRuntime().getRuntimeVersion();
        if (version != null && version != "8.5.0.0" && wsServer.getServer().getServerPublishState() != IServer.PUBLISH_STATE_NONE)
            publishServer = MessageDialog.openQuestion(getShell(), Messages.wizPackageTitle, Messages.notifyPublishRequired);

        return true;
    }

    @Override
    public void finish(IProgressMonitor monitor) throws Exception {
        final File archiveFile = new File(exportPath);

        // remember preferences
        Activator.addToPreferenceList(PREF_PATHS, exportPath);
        Activator.setPreference(PREF_OVERWRITE, overwrite ? "true" : "false");
        Activator.setPreference(PREF_INCLUDE, include);

        wsRuntime.packageServer(server, archiveFile, include, publishServer, monitor);
    }
}

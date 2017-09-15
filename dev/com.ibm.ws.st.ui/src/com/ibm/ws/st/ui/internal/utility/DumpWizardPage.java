/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
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

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

/**
 * Wizard page to generate server or Java (JVM) dump.
 */
public class DumpWizardPage extends UtilityWizardPage {
    private static final String PREF_DUMP_TYPE = "dump.type";
    public static final String PREF_SERVER_PATH = "dump.server.path";
    private static final String PREF_SERVER_OVERWRITE = "dump.server.overwrite";
    private static final String PREF_SERVER_INCLUDE = "dump.server.include";
    private static final String PREF_JVM_INCLUDE = "dump.jvm.include";
    private static final String[] INCLUDE_OPTIONS = new String[] { "heap", "system", "thread" };
    private static final int SERVER_NUM_INCLUDE_OPTIONS = 3;
    private static final int JVM_NUM_INCLUDE_OPTIONS = 2;

    protected boolean isServerDump = true;
    protected String serverExportPath;
    protected boolean serverOverwrite;
    protected boolean[] serverInclude;
    protected boolean[] jvmInclude;

    public DumpWizardPage(WebSphereServerInfo server) {
        super(server);
        setTitle(Messages.wizDumpTitle);
        setDescription(Messages.wizDumpDescription);
    }

    @Override
    public void createUtilityControl(Composite comp) {
        // no extra controls for 8.5.0.0
        if ("8.5.0.0".equals(wsRuntime.getRuntimeVersion()))
            return;

        final Button serverDumpButton = new Button(comp, SWT.RADIO);
        serverDumpButton.setText(Messages.wizDumpServer);
        GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalSpan = 3;
        data.verticalIndent = 5;
        serverDumpButton.setLayoutData(data);

        int INDENT = 18;

        final Label serverIncludeLabel = new Label(comp, SWT.NONE);
        serverIncludeLabel.setText(Messages.wizDumpInclude);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalIndent = INDENT;
        serverIncludeLabel.setLayoutData(data);

        final Composite serverIncludeComp = new Composite(comp, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 0;
        layout.marginWidth = 15;
        layout.horizontalSpacing = 8;
        serverIncludeComp.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        serverIncludeComp.setLayoutData(data);

        serverInclude = new boolean[SERVER_NUM_INCLUDE_OPTIONS];
        String[] serverIncludeLabels = new String[] { Messages.wizDumpServerIncludeHeap, Messages.wizDumpServerIncludeSystem, Messages.wizDumpServerIncludeThread };
        String serverIncludePref = Activator.getPreference(PREF_SERVER_INCLUDE, null);
        final Button[] serverIncludeButtons = new Button[SERVER_NUM_INCLUDE_OPTIONS];
        for (int i = 0; i < SERVER_NUM_INCLUDE_OPTIONS; i++) {
            serverIncludeButtons[i] = new Button(serverIncludeComp, SWT.CHECK);
            serverIncludeButtons[i].setText(serverIncludeLabels[i]);
            serverIncludeButtons[i].setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
            if (serverIncludePref != null && serverIncludePref.contains(INCLUDE_OPTIONS[i])) {
                serverInclude[i] = true;
                serverIncludeButtons[i].setSelection(true);
            }

            final int ii = i;
            serverIncludeButtons[i].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent se) {
                    serverInclude[ii] = serverIncludeButtons[ii].getSelection();
                }
            });
        }

        final Group serverArchiveGroup = new Group(comp, SWT.NONE);
        serverArchiveGroup.setText(Messages.wizPackageExport);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        serverArchiveGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalSpan = 3;
        data.horizontalIndent = INDENT;
        serverArchiveGroup.setLayoutData(data);

        final Combo serverFolder = new Combo(serverArchiveGroup, SWT.NONE);
        data = new GridData(GridData.FILL, SWT.CENTER, true, false);
        serverFolder.setLayoutData(data);

        List<String> previousLocations = Activator.getPreferenceList(PREF_SERVER_PATH);
        if (previousLocations != null && previousLocations.size() > 0) {
            serverFolder.setItems(previousLocations.toArray(new String[previousLocations.size()]));
            serverFolder.select(0);
            serverExportPath = previousLocations.get(0);
        }

        serverFolder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                serverExportPath = serverFolder.getText();
                setPageComplete(validate());
            }
        });

        final Button serverBrowse = SWTUtil.createButton(serverArchiveGroup, Messages.browse);
        serverBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SAVE | SWT.SINGLE);
                dialog.setFilterExtensions(new String[] { "*.zip" });
                dialog.setFileName(serverFolder.getText());
                String file = dialog.open();
                if (file != null)
                    serverFolder.setText(file);
            }
        });

        final Button serverOverwriteButton = new Button(serverArchiveGroup, SWT.CHECK);
        serverOverwriteButton.setText(Messages.wizPackageOverwrite);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        serverOverwriteButton.setLayoutData(data);

        String serverOverwritePref = Activator.getPreference(PREF_SERVER_OVERWRITE, null);
        if ("true".equals(serverOverwritePref)) {
            serverOverwriteButton.setSelection(true);
            serverOverwrite = true;
        }

        serverOverwriteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                serverOverwrite = serverOverwriteButton.getSelection();
            }
        });

        Button jvmDumpButton = new Button(comp, SWT.RADIO);
        jvmDumpButton.setText(Messages.wizDumpJVM);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalSpan = 3;
        data.verticalIndent = 5;
        jvmDumpButton.setLayoutData(data);

        final Label jvmIncludeLabel = new Label(comp, SWT.NONE);
        jvmIncludeLabel.setText(Messages.wizDumpInclude);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        data.horizontalIndent = INDENT;
        jvmIncludeLabel.setLayoutData(data);

        final Composite jvmIncludeComp = new Composite(comp, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 0;
        layout.marginWidth = 15;
        layout.horizontalSpacing = 8;
        jvmIncludeComp.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        jvmIncludeComp.setLayoutData(data);

        String jvmIncludePref = Activator.getPreference(PREF_JVM_INCLUDE, null);
        jvmInclude = new boolean[JVM_NUM_INCLUDE_OPTIONS];
        String[] jvmIncludeOptionsText = new String[] { Messages.wizDumpJVMIncludeHeap, Messages.wizDumpJVMIncludeSystem };
        final Button[] jvmIncludeButtons = new Button[JVM_NUM_INCLUDE_OPTIONS];
        for (int i = 0; i < JVM_NUM_INCLUDE_OPTIONS; i++) {
            jvmIncludeButtons[i] = new Button(jvmIncludeComp, SWT.CHECK);
            jvmIncludeButtons[i].setText(jvmIncludeOptionsText[i]);
            jvmIncludeButtons[i].setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
            if (jvmIncludePref != null && jvmIncludePref.contains(INCLUDE_OPTIONS[i])) {
                jvmInclude[i] = true;
                jvmIncludeButtons[i].setSelection(true);
            }

            final int ii = i;
            jvmIncludeButtons[i].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent se) {
                    jvmInclude[ii] = jvmIncludeButtons[ii].getSelection();
                }
            });
        }

        // initial dump setting and enablement
        String prefDumpType = Activator.getPreference(PREF_DUMP_TYPE, null);
        if (prefDumpType != null && "false".equals(prefDumpType)) {
            serverDumpButton.setSelection(false);
            jvmDumpButton.setSelection(true);
        }

        isServerDump = serverDumpButton.getSelection();
        serverIncludeLabel.setEnabled(isServerDump);
        serverIncludeButtons[0].setEnabled(isServerDump);
        serverIncludeButtons[1].setEnabled(isServerDump);
        serverIncludeButtons[2].setEnabled(isServerDump);
        serverArchiveGroup.setEnabled(isServerDump);
        serverFolder.setEnabled(isServerDump);
        serverBrowse.setEnabled(isServerDump);
        serverOverwriteButton.setEnabled(isServerDump);

        jvmIncludeLabel.setEnabled(!isServerDump);
        jvmIncludeButtons[0].setEnabled(!isServerDump);
        jvmIncludeButtons[1].setEnabled(!isServerDump);

        serverDumpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                isServerDump = serverDumpButton.getSelection();
                serverIncludeLabel.setEnabled(isServerDump);
                serverIncludeButtons[0].setEnabled(isServerDump);
                serverIncludeButtons[1].setEnabled(isServerDump);
                serverIncludeButtons[2].setEnabled(isServerDump);
                serverArchiveGroup.setEnabled(isServerDump);
                serverFolder.setEnabled(isServerDump);
                serverBrowse.setEnabled(isServerDump);
                serverOverwriteButton.setEnabled(isServerDump);

                jvmIncludeLabel.setEnabled(!isServerDump);
                jvmIncludeButtons[0].setEnabled(!isServerDump);
                jvmIncludeButtons[1].setEnabled(!isServerDump);
            }
        });
    }

    @Override
    protected String getUserMessage() {
        return Messages.wizDumpMessage;
    }

    @Override
    public boolean preFinish() {
        if (!isServerDump)
            return true;

        // 8.5.0.0
        if (serverExportPath == null || serverExportPath.trim().isEmpty())
            return true;

        if (!serverExportPath.endsWith(Constants.ZIP_EXT) && !serverExportPath.endsWith(Constants.JAR_EXT)) {
            serverExportPath = serverExportPath + Constants.ZIP_EXT;
        }

        final File f = new File(serverExportPath);
        if (f.exists() && !serverOverwrite) {
            if (!MessageDialog.openConfirm(getShell(), Messages.wizPackageTitle, NLS.bind(Messages.wizPackageFileExists, serverExportPath)))
                return false;
        }

        return true;
    }

    @Override
    public void finish(IProgressMonitor monitor) throws Exception {
        // save preferences
        String serverIncludeStr = null;
        String jvmIncludeStr = null;
        if (serverInclude != null) {
            Activator.setPreference(PREF_DUMP_TYPE, String.valueOf(isServerDump));
            Activator.addToPreferenceList(PREF_SERVER_PATH, serverExportPath);
            Activator.setPreference(PREF_SERVER_OVERWRITE, serverOverwrite ? "true" : "false");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < SERVER_NUM_INCLUDE_OPTIONS; i++) {
                if (serverInclude[i]) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(INCLUDE_OPTIONS[i]);
                }
            }
            if (sb.length() > 0)
                serverIncludeStr = sb.toString();

            Activator.setPreference(PREF_SERVER_INCLUDE, serverIncludeStr);

            sb = new StringBuilder();
            for (int i = 0; i < JVM_NUM_INCLUDE_OPTIONS; i++) {
                if (jvmInclude[i]) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(INCLUDE_OPTIONS[i]);
                }
            }
            if (sb.length() > 0)
                jvmIncludeStr = sb.toString();
            Activator.setPreference(PREF_JVM_INCLUDE, jvmIncludeStr);
        }

        if (isServerDump) {
            File archiveFile = null;
            if (serverExportPath != null && !serverExportPath.trim().isEmpty())
                archiveFile = new File(serverExportPath);

            wsRuntime.dumpServer(server, archiveFile, serverIncludeStr, monitor);
        } else
            wsRuntime.javadumpServer(server, jvmIncludeStr, monitor);
    }
}

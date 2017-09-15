/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContentAssistCombo;
import com.ibm.ws.st.ui.internal.ContextIds;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.download.IRuntimeHandler;
import com.ibm.ws.st.ui.internal.download.SiteHelper;
import com.ibm.ws.st.ui.internal.download.ValidationResult;

/**
 * Composite to set a runtime's name, install directory, and JRE.
 */
public class WebSphereRuntimeComposite extends Composite {

    private static final String BLUEMIX_BUNDLE_ID = "com.ibm.cftools.branding";

    // the composite supports three modes:
    //   NEW_FOLDER      - create a completely new runtime (only option with download link)
    //   EXISTING_FOLDER - create a runtime from an existing folder on disk (disable folder browsing)
    //   ARCHIVE         - create a runtime that will be populated from an archive (folder is a target, don't validate it)
    //   EDIT            - edit an installed runtime
    public enum Mode {
        NEW_FOLDER, EXISTING_FOLDER, ARCHIVE, EDIT
    }

    public static interface IMessageHandler {
        public void setMessage(String message, int severity);
    }

    public static interface IDownloadRequestHandler {
        public void downloadRequested(boolean b);
    }

    protected IMessageHandler messageHandler;
    protected Mode mode;

    protected ContentAssistCombo installDir;
    protected Text name;
    protected Combo combo;
    protected Button jreUseSpecific;
    protected Button jreUseDefault;
    protected List<IVMInstall> installedJREs;
    protected String[] jreNames;
    protected boolean updating;
    protected String runtimeTypeId;
    protected Link advancedLink;
    protected Button installUseNew;
    protected Button installUseExisting;
    protected IRuntimeHandler runtimeHandler;
    protected IDownloadRequestHandler requestHandler;
    protected boolean isDownloading;
    protected Link bluemixLink;

    /**
     * Create a new WebSphereRuntimeComposite.
     *
     * @param parent the parent composite
     * @param handler a message handler
     * @param mode the mode to run in
     */
    public WebSphereRuntimeComposite(Composite parent, IMessageHandler handler, Mode mode, IDownloadRequestHandler reqHandler) {
        super(parent, SWT.NONE);
        this.messageHandler = handler;
        this.requestHandler = reqHandler;

        this.mode = mode;

        createControl();
    }

    public void setRuntimeHandler(IRuntimeHandler runtimeHandler) {
        this.runtimeHandler = runtimeHandler;

        init();
        validate();
    }

    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        setLayout(layout);
        setLayoutData(new GridData(GridData.FILL_BOTH));
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ContextIds.RUNTIME_COMPOSITE);

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.name);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        name = new Text(this, SWT.BORDER);
        name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        name.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                runtimeHandler.setRuntimeName(name.getText());
                validate();
            }
        });

        label = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        label.setVisible(false);

        label = new Label(this, SWT.NONE);
        label.setText(Messages.runtimeCreateOptionLabel);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        Composite installGroup = new Composite(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        installGroup.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        installGroup.setLayoutData(data);

        installUseExisting = new Button(installGroup, SWT.RADIO);
        installUseExisting.setText(Messages.runtimeExistingDirLabel);
        installUseExisting.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, true, 3, 1));
        installUseExisting.setSelection(true);

        Label pathLabel = new Label(installGroup, SWT.NONE);
        pathLabel.setText(Messages.runtimeInstallPath);
        data = new GridData(SWT.FILL, SWT.CENTER, false, false);
        data.horizontalIndent = 20;
        pathLabel.setLayoutData(data);

        installDir = new ContentAssistCombo(installGroup, new RuntimeLocationContentProvider());
        installDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        installDir.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                runtimeHandler.setLocation(new Path(installDir.getText()));
                validate();
            }
        });

        final Button browse = SWTUtil.createButton(installGroup, Messages.browse);
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                DirectoryDialog dialog = new DirectoryDialog(WebSphereRuntimeComposite.this.getShell());
                dialog.setMessage(Messages.runtimeInstallMessage);
                dialog.setFilterPath(installDir.getText());
                String selectedDirectory = dialog.open();
                if (selectedDirectory != null)
                    installDir.setTextAndSuggest(selectedDirectory);
            }
        });

        installUseNew = new Button(installGroup, SWT.RADIO);
        if (SiteHelper.downloadAndInstallSupported()) {
            installUseNew.setText(Messages.runtimeNewDirLabel);
        } else {
            installUseNew.setText(Messages.runtimeArchiveInstallLabel);
        }
        installUseNew.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, true, 3, 1));

        if (mode == Mode.ARCHIVE) {
            installUseExisting.setSelection(false);
            installUseNew.setSelection(true);
            installUseExisting.setEnabled(false);
        } else {
            installUseExisting.setSelection(true);
            installUseNew.setSelection(false);
            if (mode == Mode.EXISTING_FOLDER)
                installUseNew.setEnabled(false);
        }

        installUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (installUseExisting.getSelection()) {
                    isDownloading = false;
                    requestHandler.downloadRequested(false);
                } else {
                    isDownloading = true;
                    requestHandler.downloadRequested(true);
                }
                installDir.setEnabled(!isDownloading);
                browse.setEnabled(!isDownloading);
                validate();
            }
        });

        if (mode == Mode.EXISTING_FOLDER || mode == Mode.ARCHIVE) {
            installDir.setEnabled(false);
            browse.setEnabled(false);
        }

        label = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
        label.setVisible(false);

        updateJREs();

        Group jreGroup = new Group(this, SWT.NONE);
        jreGroup.setText(Messages.runtimeJREGroup);
        layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        jreGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        jreGroup.setLayoutData(data);

        // JRE location
        jreUseSpecific = new Button(jreGroup, SWT.RADIO);
        jreUseSpecific.setText(Messages.runtimeJRESpecific);
        jreUseSpecific.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

        combo = new Combo(jreGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(jreNames);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        data.horizontalIndent = 50;
        combo.setLayoutData(data);

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                int sel = combo.getSelectionIndex();
                IVMInstall vmInstall = null;
                if (sel >= 0)
                    vmInstall = installedJREs.get(sel);

                runtimeHandler.getRuntime().setVMInstall(vmInstall);
                validate();
            }
        });

        jreUseDefault = new Button(jreGroup, SWT.RADIO);
        IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
        jreUseDefault.setText(NLS.bind(Messages.runtimeJREDefault, vmInstall.getName()));
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        jreUseDefault.setLayoutData(data);
        jreUseDefault.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (jreUseDefault.getSelection()) {
                    combo.setEnabled(false);
                    runtimeHandler.getRuntime().setVMInstall(null);
                } else {
                    combo.setEnabled(true);
                    int sel = combo.getSelectionIndex();
                    IVMInstall vmInstall = null;
                    if (sel >= 0)
                        vmInstall = installedJREs.get(sel);

                    runtimeHandler.getRuntime().setVMInstall(vmInstall);
                }
                validate();
            }
        });

        Link link = new Link(jreGroup, SWT.NONE);
        link.setText("<a>" + Messages.runtimeJREConfigure + "</a>");
        link.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String currentVM = combo.getText();
                if (showPreferencePage()) {
                    updateJREs();
                    combo.setItems(jreNames);
                    combo.setText(currentVM);
                    if (combo.getSelectionIndex() == -1)
                        combo.select(0);

                    IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
                    jreUseDefault.setText(NLS.bind(Messages.runtimeJREDefault, vmInstall.getName()));
                    validate();
                }
            }
        });

        advancedLink = new Link(this, SWT.NONE);
        advancedLink.setText("<a>" + Messages.runtimeAdvancedLink + "</a>");
        data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        advancedLink.setLayoutData(data);
        advancedLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RuntimeAdvancedDialog dialog = new RuntimeAdvancedDialog(getShell(), runtimeHandler.getRuntime());
                dialog.open();
            }
        });

        if (Platform.getBundle(BLUEMIX_BUNDLE_ID) == null) {
            bluemixLink = new Link(this, SWT.NONE);
            bluemixLink.setText(Messages.runtimeBluemixLink);
            data = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
            data.horizontalSpan = 2;
            data.verticalSpan = 8;
            bluemixLink.setLayoutData(data);
            bluemixLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                        URL url = new URL(Constants.BLUEMIX_URL);
                        browser.openURL(url);
                    } catch (Exception e1) {
                        Trace.logError("Failed to open browser for: " + Constants.BLUEMIX_URL, e1);
                    }
                }
            });
        }

        init();
        validate();

        Dialog.applyDialogFont(this);

        name.forceFocus();
    }

    protected void updateJREs() {
        // get all installed JVMs
        installedJREs = new ArrayList<IVMInstall>();
        IVMInstallType[] vmInstallTypes = JavaRuntime.getVMInstallTypes();
        int size = vmInstallTypes.length;
        for (int i = 0; i < size; i++) {
            IVMInstall[] vmInstalls = vmInstallTypes[i].getVMInstalls();
            int size2 = vmInstalls.length;
            for (int j = 0; j < size2; j++) {
                installedJREs.add(vmInstalls[j]);
            }
        }

        // get names
        size = installedJREs.size();
        jreNames = new String[size];
        for (int i = 0; i < size; i++) {
            IVMInstall vmInstall = installedJREs.get(i);
            jreNames[i] = vmInstall.getName();
        }
    }

    protected boolean showPreferencePage() {
        String id = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage";

        // should be using the following API, but it only allows a single preference page instance.
        // see bug 168211 for details
        //PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null);
        //return (dialog.open() == Window.OK);

        PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
        IPreferenceNode node = manager.find("org.eclipse.jdt.ui.preferences.JavaBasePreferencePage").findSubNode(id);
        PreferenceManager manager2 = new PreferenceManager();
        manager2.addToRoot(node);
        PreferenceDialog dialog = new PreferenceDialog(getShell(), manager2);
        dialog.create();
        return (dialog.open() == Window.OK);
    }

    private void init() {
        if (name == null || runtimeHandler == null || runtimeHandler.getRuntime() == null)
            return;

        updating = true;
        if (runtimeHandler.getRuntimeName() != null)
            name.setText(runtimeHandler.getRuntimeName());
        else
            name.setText("");

        runtimeTypeId = runtimeHandler.getRuntimeTypeId();

        if (runtimeHandler.getLocation() != null && !runtimeHandler.getLocation().isEmpty()) {
            installDir.setText(runtimeHandler.getLocation().toOSString());
        } else {
            installDir.setText("");
        }

        // JRE
        if (runtimeHandler.getRuntime().isUsingDefaultJRE()) {
            combo.setEnabled(false);
            jreUseDefault.setSelection(true);
            combo.select(0);
        } else {
            combo.setEnabled(true);
            jreUseSpecific.setSelection(true);
            boolean found = false;
            int size = installedJREs.size();
            for (int i = 0; i < size; i++) {
                IVMInstall vmInstall = installedJREs.get(i);
                if (vmInstall.equals(runtimeHandler.getRuntime().getVMInstall())) {
                    combo.select(i);
                    found = true;
                }
            }
            if (!found)
                combo.select(0);
        }

        updating = false;
    }

    protected void validate() {
        if (runtimeHandler != null) {
            ValidationResult result = runtimeHandler.validateRuntime(mode == Mode.ARCHIVE || isDownloading);
            processValidationResult(result.getMessage(), result.getLevel());
        } else {
            processValidationResult(null, IMessageProvider.ERROR);
        }
    }

    private void processValidationResult(String message, int level) {
        messageHandler.setMessage(message, level);
        advancedLink.setEnabled(level == IMessageProvider.NONE);
    }

    protected class RuntimeLocationContentProvider extends ContentAssistCombo.ContentProvider {
        @Override
        public String[] getSuggestions(String hint, boolean showAll) {
            List<String> previousLocations = Activator.getPreferenceList(runtimeTypeId + ".folder");
            if (hint == null || hint.isEmpty())
                return previousLocations.toArray(new String[previousLocations.size()]);

            List<String> suggestions = new ArrayList<String>();

            // add the suggested path if there is one
            List<IPath> suggestedPaths = WebSphereRuntime.findValidLocations(new Path(hint));
            if (suggestedPaths != null) {
                for (IPath path : suggestedPaths) {
                    try {
                        suggestions.add(path.toFile().getCanonicalPath());
                    } catch (IOException e) {
                        suggestions.add(path.toOSString());
                    }
                }
            }

            for (String previousLocation : previousLocations) {
                Path previousPath = new Path(previousLocation);
                if (suggestedPaths != null && matchPathsByFile(previousPath, suggestedPaths)) {
                    // don't add the suggested path again
                    continue;
                }
                if (showAll || installDir.matches(previousPath.toPortableString())) {
                    suggestions.add(previousPath.toOSString());
                }
            }

            return suggestions.toArray(new String[suggestions.size()]);
        }

        /**
         * Checks if a given path matches any of the paths in the list based on whether
         * they point to the same file on the system. This is helpful for Windows since
         * two paths can be different due to case sensitivity but point to the same file.
         *
         * @param thePath
         * @param pathList
         * @return
         */
        private boolean matchPathsByFile(IPath thePath, List<IPath> pathList) {
            for (IPath item : pathList) {
                if (item.toFile().equals(thePath.toFile())) {
                    return true;
                }
            }
            return false;
        }
    }
}

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
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.ISite;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContentAssistCombo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.utility.PackageWizardPage;

public class DownloaderComposite extends AbstractDownloadComposite {
    public static final String PREF_ARCHIVES = "archives";

    protected boolean downloadSupported = SiteHelper.downloadAndInstallSupported();

    protected IProduct coreProduct;

    protected boolean noAccess = false;

    protected boolean isLocalCoreArchive = true;
    protected ContentAssistCombo archive;

    protected ContentAssistCombo destinationDir;
    protected Combo combo;
    protected Table table;
    protected Label description;
    protected Group authenticationComp;
    protected Label userLabel;
    protected Text user;
    protected boolean userTouched;
    protected Label passwordLabel;
    protected Text password;
    protected Button unzipButton;
    protected Button browse;
    protected Button connect;
    protected Label downloadLicenseLabel;

    protected LocalProduct localCoreArchive = null;
    protected String localArchiveName = null;
    protected ISite currentSite = null;
    protected ISite[] runtimeSites;
    protected Map<String, ISite> runtimeSiteMap = new HashMap<String, ISite>();
    protected Map<ISite, PasswordAuthentication> siteAuthentication = new HashMap<ISite, PasswordAuthentication>();
    protected boolean isLicenseDownloadFailed = false;
    protected boolean isLicenseDownloadRequired = false;
    protected String archiveSource = null;
    protected IRuntimeHandler runtimeHandler;
    protected boolean updating;

    public DownloaderComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler, IRuntimeHandler runtimeHandler, String archiveSource) {
        super(parent, map, container, handler);
        container.setTitle(Messages.wizInstallTitle);
        container.setDescription(Messages.wizInstallDescription);
        runtimeSites = SiteHelper.getPredefinedRuntimeSites();
        this.runtimeHandler = runtimeHandler;
        this.archiveSource = archiveSource;
        for (ISite site : runtimeSites) {
            runtimeSiteMap.put(site.getName(), site);
        }
        map.put(SITE_AUTHENTICATION, siteAuthentication);
        createControl();
    }

    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        layout.numColumns = 2;
        setLayout(layout);

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.wizInstallDestinationLabel);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Composite destinationGroup = createLocationBrowseComposite(this);
        destinationDir = new ContentAssistCombo(destinationGroup, new RuntimeLocationContentProvider());
        destinationDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        destinationDir.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                runtimeHandler.setLocation(new Path(destinationDir.getText()));
                validate();
            }
        });

        final Button destinationBrowse = SWTUtil.createButton(destinationGroup, Messages.browse);
        destinationBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                DirectoryDialog dialog = new DirectoryDialog(DownloaderComposite.this.getShell());
                dialog.setMessage(Messages.runtimeInstallMessage);
                dialog.setFilterPath(destinationDir.getText());
                String selectedDirectory = dialog.open();
                if (selectedDirectory != null)
                    destinationDir.setTextAndSuggest(selectedDirectory);
            }
        });

        if (downloadSupported) {
            createArchiveAndDownload();
        } else {
            createArchive();
        }
    }

    protected void createArchive() {
        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.wizInstallArchiveLabel);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Composite archiveGroup = createLocationBrowseComposite(this);
        createArchiveControl(archiveGroup, 0);

        isLicenseDownloadRequired = false;

        Dialog.applyDialogFont(this);
        destinationDir.setFocus();
    }

    protected void createArchiveAndDownload() {

        Label lineLabel = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        lineLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        lineLabel.setVisible(false);

        unzipButton = new Button(this, SWT.RADIO);
        unzipButton.setText(Messages.wizInstallArchive);
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        unzipButton.setLayoutData(data);
        unzipButton.setSelection(isLocalCoreArchive);

        createArchiveControl(this, 20);

        Button downloadButton = new Button(this, SWT.RADIO);
        downloadButton.setText(Messages.wizDownloadMessage);
        data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        downloadButton.setLayoutData(data);

        // Selects default site as a start
        currentSite = SiteHelper.getDefaultAddOnSite();

        if (runtimeSites.length > 1) {
            combo = new Combo(this, SWT.READ_ONLY | SWT.FLAT | SWT.BORDER);
            data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
            data.horizontalIndent = 20;
            data.horizontalSpan = 2;
            combo.setLayoutData(data);

            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    ISite newSite = runtimeSiteMap.get(combo.getText());
                    if (newSite == currentSite)
                        return;

                    currentSite = newSite;
                    coreProduct = null;
                    description.setText("");
                    if (currentSite.isAuthenticationRequired() && !currentSite.isAuthenticated()) {
                        table.removeAll();
                        description.setText("");
                        authenticationComp.setVisible(true);
                        authenticationComp.setText(currentSite.getName());
                        if (!userTouched) {
                            String lastUser = Activator.getPreference("download.user." + currentSite.getName(), null);
                            if (lastUser != null)
                                user.setText(lastUser);
                            else
                                user.setText("");
                            password.setText("");
                            userTouched = false;
                        }
                    } else {
                        authenticationComp.setVisible(false);
                        updateTable();
                    }
                    validate();
                }
            });

            for (ISite site : runtimeSites) {
                combo.add(site.getName());
            }

            combo.setText(currentSite.getName());
        }

        table = new Table(this, SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
        table.setHeaderVisible(false);
        table.setLinesVisible(false);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.horizontalIndent = 20;
        data.horizontalSpan = 2;
        data.minimumHeight = 100;
        table.setLayoutData(data);
        table.setFont(getFont());

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                TableItem[] sel = table.getSelection();
                Object selection = (sel != null && sel.length == 1) ? sel[0].getData() : null;
                if (selection != null) {
                    coreProduct = (IProduct) selection;
                    description.setText(coreProduct.getDescription());
                } else {
                    coreProduct = null;
                    description.setText("");
                }
                isLicenseDownloadRequired = true;
                validate();
            }
        });

        description = new Label(this, SWT.WRAP);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.heightHint = 60;
        data.horizontalSpan = 2;
        data.horizontalIndent = 20;
        description.setLayoutData(data);

        authenticationComp = new Group(this, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        authenticationComp.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalSpan = 2;
        data.horizontalIndent = 20;
        authenticationComp.setLayoutData(data);

        userLabel = new Label(authenticationComp, SWT.NONE);
        userLabel.setText(Messages.user);
        userLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        user = new Text(authenticationComp, SWT.BORDER);
        user.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        user.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                userTouched = true;
                enableConnectIfValid();
            }
        });

        passwordLabel = new Label(authenticationComp, SWT.NONE);
        passwordLabel.setText(Messages.password);
        passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        password = new Text(authenticationComp, SWT.BORDER);
        password.setEchoChar('*');
        password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        password.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                enableConnectIfValid();
            }
        });

        connect = new Button(authenticationComp, SWT.PUSH);
        connect.setText(Messages.connect);
        connect.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));

        connect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (performAuthentication(user.getText(), password.getText())) {
                    updateTable();
                    authenticationComp.setVisible(false);
                    validate();
                }
            }
        });

        unzipButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                unzipButtonSelected();
                validate();
            }
        });

        downloadLicenseLabel = new Label(this, SWT.NONE);
        downloadLicenseLabel.setText(Messages.taskDownloadLicense);
        downloadLicenseLabel.setVisible(false);

        // Defect 93503, button is initialized to unzip so set appropriate fields
        unzipButtonSelected();

        if (archiveSource != null)
            downloadButton.setEnabled(false);

        isLicenseDownloadRequired = !archive.getText().isEmpty();

        Dialog.applyDialogFont(this);
        table.setFocus();

        if (currentSite.isAuthenticationRequired()) {
            authenticationComp.setVisible(true);
            authenticationComp.setText(currentSite.getName());
        } else {
            authenticationComp.setVisible(false);
            updateTable();
        }
    }

    protected Composite createLocationBrowseComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.horizontalSpan = 2;
        composite.setLayoutData(data);
        return composite;
    }

    protected void createArchiveControl(Composite parent, int indent) {
        ArchiveContentProvider contentProvider = new ArchiveContentProvider();
        archive = new ContentAssistCombo(parent, contentProvider);
        if (archiveSource != null) {
            archive.setText(archiveSource);
            addToMapList(ARCHIVES, archiveSource);
        } else {
            String[] items = contentProvider.getSuggestions("", true);
            if (items != null) {
                for (String s : items) {
                    if (new File(s).exists()) {
                        archive.setText(s);
                        addToMapList(ARCHIVES, s);
                        break;
                    }
                }
            }
        }

        GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        data.horizontalIndent = indent;
        archive.setLayoutData(data);
        archive.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                addToMapList(ARCHIVES, archive.getText());
                if (downloadSupported) {
                    isLicenseDownloadRequired = true;
                }
                validate();
            }
        });

        browse = SWTUtil.createButton(parent, Messages.browseButtonAcc);
        ((GridData) browse.getLayoutData()).verticalAlignment = GridData.CENTER;
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                FileDialog dialog = new FileDialog(getShell());
                dialog.setFilterExtensions(new String[] { "*.jar;*.zip" });
                dialog.setFileName(archive.getText());
                String file = dialog.open();
                if (file != null)
                    archive.setText(file);
            }
        });
    }

    void unzipButtonSelected() {
        isLocalCoreArchive = unzipButton.getSelection();
        archive.setEnabled(isLocalCoreArchive);
        browse.setEnabled(isLocalCoreArchive);
        if (combo != null) {
            combo.setEnabled(!isLocalCoreArchive);
        }
        table.setEnabled(!isLocalCoreArchive);
        description.setEnabled(!isLocalCoreArchive);
        authenticationComp.setEnabled(!isLocalCoreArchive);
        userLabel.setEnabled(!isLocalCoreArchive);
        user.setEnabled(!isLocalCoreArchive);
        passwordLabel.setEnabled(!isLocalCoreArchive);
        password.setEnabled(!isLocalCoreArchive);
        enableConnectIfValid();
        if (isLocalCoreArchive) {
            if (table.getSelectionCount() > 0) {
                table.deselectAll();
            }
            description.setText("");
        } else {
            coreProduct = null;
        }
    }

    void enableConnectIfValid() {
        connect.setEnabled(!isLocalCoreArchive && !user.getText().isEmpty() && !password.getText().isEmpty());
    }

    protected void updateTable() {
        table.removeAll();
        noAccess = false;

        TableItem updateItem = new TableItem(table, SWT.NONE);
        updateItem.setText(Messages.wizDownloadConnecting);

        final Thread thread = new Thread("Checking available repositories") {
            @Override
            public void run() {
                List<IProduct> productList = currentSite.getCoreProducts(new NullProgressMonitor());
                for (final IProduct p : productList) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (table.isDisposed())
                                return;

                            int count = table.getItemCount();
                            TableItem item = new TableItem(table, SWT.NONE, count - 1);
                            item.setImage(Activator.getImage(Activator.IMG_RUNTIME));
                            item.setText(p.getName());
                            item.setData(p);
                        }
                    });
                }

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (table.isDisposed())
                            return;
                        // remove waiting item
                        int count = table.getItemCount();
                        table.remove(count - 1);

                        if (count == 1) {
                            noAccess = true;
                            validate();
                        }
                    }
                });
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    protected void validate() {
        ValidationResult result = runtimeHandler.validateRuntime(true);
        if (result.getLevel() != IMessageProvider.NONE)
            setMessage(result.getMessage(), result.getLevel());

        if (isLocalCoreArchive) {
            String archiveName = archive.getText();

            coreProduct = null;
            if (archiveName != null && !archiveName.trim().isEmpty()) {

                File f = new File(archiveName);
                if (!f.exists() || !(f.getName().endsWith(".jar") || f.getName().endsWith(".zip"))) {
                    setMessage(Messages.errorInvalidArchive, IMessageProvider.ERROR);
                    return;
                }

                if (!archiveName.equals(localArchiveName)) {
                    localArchiveName = archiveName;
                    localCoreArchive = LocalProduct.create(archiveName);
                }

                if (localCoreArchive == null || localCoreArchive.getType() != IProduct.Type.INSTALL) {
                    setMessage(Messages.errorInvalidCoreArchive, IMessageProvider.ERROR);
                    return;
                }
                coreProduct = localCoreArchive;
            }
        } else if (noAccess) {
            setMessage(Messages.errorCouldNotConnect, IMessageProvider.ERROR);
            return;
        }

        // if we selected a runtime archive that is different
        // from what we currently store, we need to update the
        // map
        if (coreProduct != null && coreProduct != map.get(SELECTED_CORE_MANAGER)) {
            map.put(SELECTED_CORE_MANAGER, coreProduct);
            map.put(RUNTIME_CORE, (coreProduct.getRuntimeInfo()));
            if (isLocalCoreArchive) {
                map.remove(RUNTIME_SITE);
            } else {
                map.put(RUNTIME_SITE, currentSite);
            }
            map.remove(LICENSE_ACCEPT);
            isLicenseDownloadFailed = false;
        }

        if (destinationDir.getText().isEmpty()) {
            setMessage(Messages.wizInstallDestinationNotSet, IMessageProvider.ERROR);
            return;
        }

        String error = DownloadHelper.validateTargetRuntimeLocation(destinationDir.getText());
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
            return;
        }

        if (coreProduct == null) {
            setMessage(Messages.wizInstallArchiveNotSet, IMessageProvider.ERROR);
            return;
        }

        if (isLicenseDownloadRequired) {
            downloadLicense();
            setMessage(null, IMessageProvider.ERROR);
            return;
        }

        if (isLicenseDownloadFailed) {
            setMessage(NLS.bind(Messages.wizLicenseError, coreProduct.getName()), IMessageProvider.ERROR);
            return;
        }

        setMessage(null, IMessageProvider.NONE);
    }

    @Override
    public void exit() {
        // Add selected downloaders
        @SuppressWarnings("unchecked")
        List<IProduct> selectedList = (List<IProduct>) map.get(SELECTED_DOWNLOADERS);
        if (selectedList == null) {
            selectedList = new ArrayList<IProduct>();
            map.put(SELECTED_DOWNLOADERS, selectedList);
        } else {
            selectedList.clear();
        }

        IProduct coreManager = (IProduct) map.get(SELECTED_CORE_MANAGER);
        // it should not be null, but just in case something went wrong
        if (coreManager != null) {
            selectedList.add(coreManager);
        }
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        // no op
    }

    protected boolean performAuthentication(String user, String password) {
        final PasswordAuthentication authentication = getAuthentication(currentSite, user, password);
        if (authentication == null) {
            return false;
        }

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {

                        monitor.beginTask(Messages.taskAuthenticating, 200);
                        try {
                            if (!currentSite.authenticate(authentication, new SubProgressMonitor(monitor, 100)))
                                throw new InvocationTargetException(new Exception(Messages.errorAuthenticationFailed));
                            Activator.setPreference("download.user." + currentSite.getName(), authentication.getUserName());
                        } catch (SocketException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Error authenticating", e);
                            if (!DownloadHelper.isSSLWorking())
                                throw new InvocationTargetException(new Exception(Messages.errorSSLSocketFailed));
                            throw new InvocationTargetException(e);
                        } catch (IOException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Error authenticating", e);
                            throw new InvocationTargetException(e);
                        }
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (Exception e) {
            setMessage(e.getCause().getLocalizedMessage(), IMessageProvider.ERROR);
            return false;
        }
        return true;
    }

    private PasswordAuthentication getAuthentication(ISite site, String user, String password) {
        PasswordAuthentication authentication = siteAuthentication.get(site);
        if (authentication != null && user.equals(authentication.getUserName()) && password.equals(new String(authentication.getPassword()))) {
            return authentication;
        }

        authentication = new PasswordAuthentication(user, password.toCharArray());
        siteAuthentication.put(site, authentication);

        return authentication;
    }

    protected void downloadLicense() {
        if (coreProduct == null)
            return;

        isLicenseDownloadFailed = true;
        isLicenseDownloadRequired = false;
        downloadLicenseLabel.setVisible(true);

        Thread downloadThread = new Thread(Messages.taskDownloadLicense) {
            @Override
            public void run() {
                @SuppressWarnings("unchecked")
                Map<IProduct, License> licenseMap = (Map<IProduct, License>) map.get(LICENSE);
                if (licenseMap != null) {
                    licenseMap.clear();
                }
                try {
                    License license = coreProduct.getLicense(new NullProgressMonitor());
                    if (license != null) {
                        if (licenseMap == null) {
                            licenseMap = AddonUtil.createLicenseMap();
                            map.put(LICENSE, licenseMap);
                        }
                        licenseMap.put(coreProduct, license);
                    }
                    isLicenseDownloadFailed = false;
                } catch (IOException e) {
                    Trace.logError("Error getting license for " + coreProduct.getName(), e);
                } finally {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (downloadLicenseLabel.isDisposed())
                                return;
                            downloadLicenseLabel.setVisible(false);
                            validate();
                        }
                    });
                }
            }
        };
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    protected class ArchiveContentProvider extends ContentAssistCombo.ContentProvider {
        @Override
        public String[] getSuggestions(String hint, boolean showAll) {
            // get locations from three places:
            //  - WLP archives found in obvious places like desktop
            //  - archives recently created by the packaging tool
            //  - recently used locations
            List<String> items = new ArrayList<String>();
            items.addAll(Activator.getPreferenceList(PREF_ARCHIVES));

            List<File> files = DownloadHelper.getArchives();
            if (files != null && !files.isEmpty()) {
                int size = files.size();
                for (int i = 0; i < size; i++)
                    items.add(files.get(i).getAbsolutePath());
            }
            items.addAll(Activator.getPreferenceList(PackageWizardPage.PREF_PATHS));

            List<String> suggestions = new ArrayList<String>();

            for (String s : items) {
                boolean found = false;
                for (String t : suggestions) {
                    if (t.equalsIgnoreCase(s))
                        found = true;
                }
                if (found) // don't add the suggested path again
                    continue;

                if (showAll || archive.matches(s)) {
                    suggestions.add(s);
                }
            }

            return suggestions.toArray(new String[suggestions.size()]);
        }
    }

    @SuppressWarnings("unchecked")
    void addToMapList(String mapKey, String value) {
        List<String> list = (List<String>) map.get(mapKey);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(mapKey, list);
        } else {
            list.clear();
        }
        list.add(value);
    }

    @Override
    public void enter() {
        destinationDir.setFocus();
    }

    protected class RuntimeLocationContentProvider extends ContentAssistCombo.ContentProvider {
        @Override
        public String[] getSuggestions(String hint, boolean showAll) {
            List<String> previousLocations = Activator.getPreferenceList(runtimeHandler.getRuntimeTypeId() + ".folder");
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
                if (showAll || destinationDir.matches(previousPath.toPortableString())) {
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

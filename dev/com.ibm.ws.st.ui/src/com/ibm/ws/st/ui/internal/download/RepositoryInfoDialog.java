/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
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
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.download.SiteHelper.SiteDelegate;

public class RepositoryInfoDialog extends TitleAreaDialog {

    /** {@inheritDoc} */
    @Override
    protected Point getInitialSize() {
        return new Point(800, 500);
    }

    private Text nameText;
    private Label urlLabel;
    private Text urlText;
    private Label userLabel;
    private Text userText;
    private Label passwordLabel;
    private Text passwordText;
    private Label locationLabel;
    Text locationText;
    private Button remoteButton;
    private Button localButton;
    Button browseButton;
    protected final SiteDelegate site;
    private final ModifyListener listener;
    private final SelectionAdapter enablementListener;
    private String siteName;
    private String user;
    private String password;
    URL url;
    private final List<SiteDelegate> allSites;
    private final Map<String, Object> map;

    Control[] remoteControls;
    Control[] localControls;

    public RepositoryInfoDialog(Shell parentShell, SiteDelegate site, List<SiteDelegate> allSites, Map<String, Object> map) {
        super(parentShell);
        this.site = site;
        setHelpAvailable(false);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        listener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                enableOKButton(validate());
            }
        };
        enablementListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                handleEnablement();
                enableOKButton(validate());
            }
        };
        this.allSites = allSites;
        this.map = map;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText((site == null) ? Messages.repositoryInfoDialogNewTitle : Messages.repositoryInfoDialogEditTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_RUNTIME));
        setTitle((site == null) ? Messages.repositoryInfoDialogNewTitle : Messages.repositoryInfoDialogEditTitle);
        setMessage((site == null) ? Messages.repositoryInfoDialogNewDescription : Messages.repositoryInfoDialogEditDescription);

        FieldDecoration reqFieldIndicator = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_REQUIRED);

        Composite comp = new Composite(parent, SWT.BORDER);
        comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        GridLayout layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 4);
        comp.setLayout(layout);

        Composite nameGroup = new Composite(comp, SWT.NONE);
        nameGroup.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 4);
        layout.numColumns = 3;
        nameGroup.setLayout(layout);

        Label label = new Label(nameGroup, SWT.NONE);
        label.setText(Messages.name);
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

        nameText = new Text(nameGroup, SWT.BORDER);
        nameText.setLayoutData(getFieldGridData(2));
        ControlDecoration dec = new ControlDecoration(nameText, SWT.TOP | SWT.LEFT);
        dec.setImage(reqFieldIndicator.getImage());

        Composite radioGroup = new Composite(comp, SWT.NONE);
        radioGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 4);
        layout.numColumns = 1;
        radioGroup.setLayout(layout);

        remoteButton = new Button(radioGroup, SWT.RADIO);
        remoteButton.setText(Messages.repositoryInfoDialogRemoteGroupLabel);

        final Composite remoteGroup = new Composite(radioGroup, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 4);
        layout.numColumns = 3;
        remoteGroup.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalIndent = 15;
        remoteGroup.setLayoutData(data);

        label = new Label(remoteGroup, SWT.NONE);
        label.setText(Messages.url);
        GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        gd.widthHint = 70;
        label.setLayoutData(gd);
        urlLabel = label;

        urlText = new Text(remoteGroup, SWT.BORDER);
        urlText.setLayoutData(getFieldGridData(2));
        dec = new ControlDecoration(urlText, SWT.TOP | SWT.LEFT);
        dec.setImage(reqFieldIndicator.getImage());

        label = new Label(remoteGroup, SWT.NONE);
        label.setText(Messages.user);
        gd = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        gd.widthHint = 70;
        label.setLayoutData(gd);
        userLabel = label;

        userText = new Text(remoteGroup, SWT.BORDER);
        userText.setLayoutData(getFieldGridData(2));
        dec = new ControlDecoration(userText, SWT.TOP | SWT.LEFT);
        dec.setImage(reqFieldIndicator.getImage());
        dec.setShowOnlyOnFocus(true);

        label = new Label(remoteGroup, SWT.NONE);
        label.setText(Messages.password);
        gd = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        gd.widthHint = 70;
        label.setLayoutData(gd);
        passwordLabel = label;

        passwordText = new Text(remoteGroup, SWT.BORDER);
        passwordText.setLayoutData(getFieldGridData(2));
        passwordText.setEchoChar('*');
        dec = new ControlDecoration(passwordText, SWT.TOP | SWT.LEFT);
        dec.setImage(reqFieldIndicator.getImage());
        dec.setShowOnlyOnFocus(true);

        localButton = new Button(radioGroup, SWT.RADIO);
        localButton.setText(Messages.repositoryInfoDialogLocalGroupLabel);

        final Composite localGroup = new Composite(radioGroup, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(comp, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(comp, 4);
        layout.numColumns = 3;
        localGroup.setLayout(layout);
        data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        data.horizontalIndent = 15;
        localGroup.setLayoutData(data);

        label = new Label(localGroup, SWT.NONE);
        label.setText(Messages.repositoryInfoDialogLocationLabel);
        gd = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        gd.widthHint = 70;
        label.setLayoutData(gd);
        locationLabel = label;

        locationText = new Text(localGroup, SWT.BORDER);
        locationText.setLayoutData(getFieldGridData(1));
        dec = new ControlDecoration(urlText, SWT.TOP | SWT.LEFT);
        dec.setImage(reqFieldIndicator.getImage());

        // zip=based repository install was added to runtime installUtility in 8.5.5.8
        IRuntimeInfo runtimeInfo = (IRuntimeInfo) map.get(AbstractDownloadComposite.RUNTIME_CORE);
        if (SiteHelper.isZipRepoSupported(runtimeInfo)) {
            browseButton = new Button(localGroup, SWT.PUSH | SWT.RIGHT_TO_LEFT);
            browseButton.setText(Messages.browseButtonAcc4);
            browseButton.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));
            data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
            browseButton.setLayoutData(data);
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final Menu menu = new Menu(getShell(), SWT.CASCADE);
                    MenuItem directory = new MenuItem(menu, SWT.NONE);
                    directory.setText(Messages.repositoryInfoDialogDirectoryRepoButton);
                    directory.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent arg0) {
                            DirectoryDialog dialog = new DirectoryDialog(getShell());
                            String file = dialog.open();
                            if (file != null)
                                locationText.setText(file);
                        }
                    });
                    MenuItem zip = new MenuItem(menu, SWT.NONE);
                    zip.setText(Messages.repositoryInfoDialogZipRepoButton);
                    zip.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent arg0) {
                            FileDialog dialog = new FileDialog(getShell());
                            dialog.setFilterExtensions(new String[] { "*.zip" });
                            String file = dialog.open();
                            if (file != null)
                                locationText.setText(file);
                        }
                    });
                    displayDropdownMenu(browseButton, menu, true);
                    menu.dispose();
                }
            });
        } else {
            browseButton = new Button(localGroup, SWT.NONE);
            browseButton.setText(Messages.browseButtonAcc);
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    DirectoryDialog dialog = new DirectoryDialog(getShell());
                    String file = dialog.open();
                    if (file != null)
                        locationText.setText(file);
                }
            });
        }

        remoteControls = new Control[] { urlLabel, urlText, userLabel, userText, passwordLabel, passwordText };
        localControls = new Control[] { locationLabel, locationText, browseButton };

        init();

        return comp;
    }

    private void init() {
        URL url;
        if (site != null && (url = site.getURL()) != null) {
            nameText.setText(site.getName());

            if (url.toString().startsWith(FileUtil.FILE_URI)) { // local site
                locationText.setText(site.getURL().toString().replace(FileUtil.FILE_URI, ""));
                localButton.setSelection(true);
            } else { // remote site
                urlText.setText(site.getURL().toString());
                if (site.getUser() != null)
                    userText.setText(site.getUser());
                if (site.getPassword() != null)
                    passwordText.setText(site.getPassword());
                remoteButton.setSelection(true);
            }
        } else {
            urlText.setText("http://");
            remoteButton.setSelection(true);
            locationText.setText("");
        }

        nameText.setFocus();
        nameText.addModifyListener(listener);
        urlText.addModifyListener(listener);
        userText.addModifyListener(listener);
        passwordText.addModifyListener(listener);
        locationText.addModifyListener(listener);
        localButton.addSelectionListener(enablementListener);
        remoteButton.addSelectionListener(enablementListener);

        handleEnablement();

        setErrorMessage(null);
    }

    protected GridData getFieldGridData(int horizontalSpan) {
        int margin = FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
        GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH + margin;
        data.horizontalIndent = margin;
        data.horizontalSpan = horizontalSpan;
        return data;
    }

    protected boolean validate() {

        siteName = nameText.getText().trim();
        if (siteName.isEmpty()) {
            setErrorMessage(Messages.errorRepositoryEmptyName);
            return false;
        }

        // we changed the name of the site, so check for existing
        // sites
        if (site == null || !siteName.equalsIgnoreCase(site.getName())) {
            for (SiteDelegate s : allSites) {
                if (siteName.equalsIgnoreCase(s.getName())) {
                    setErrorMessage(Messages.errorRepositoryAlreadyExists);
                    return false;
                }
            }
        }

        // local repository
        if (localButton.getSelection()) {
            String location = locationText.getText();
            if (location.isEmpty()) {
                setErrorMessage(null);
                return false;
            }

            try {
                url = new URL(FileUtil.FILE_URI + location);
            } catch (MalformedURLException e) {
                setErrorMessage(Messages.errorRepositoryInvalidLocation);
                return false;
            }

            if (url == null) {
                setErrorMessage(Messages.errorRepositoryInvalidLocation);
                return false;
            }

            try {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.equalsIgnoreCase("repository.config");
                    }
                };
                File file = new File(url.toURI());
                IRuntimeInfo runtimeInfo = (IRuntimeInfo) map.get(AbstractDownloadComposite.RUNTIME_CORE);

                if (!file.exists()) {
                    setErrorMessage(Messages.errorRepositoryInvalidLocation);
                    return false;
                } else if (file.isDirectory()) { // directory repo validation
                    if (file.listFiles(filter).length != 1) {
                        setErrorMessage(Messages.errorRepositoryInvalidLocation);
                        return false;
                    }
                } else if (!SiteHelper.isZipRepoSupported(runtimeInfo) || !SiteHelper.isValidOnPremZipRepository(file)) { // file repo validation
                    setErrorMessage(Messages.errorRepositoryInvalidArchive);
                    return false;
                }
            } catch (Throwable t) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Failed to verify local repository directory", t);
                setErrorMessage(Messages.errorRepositoryInvalidLocation);
                return false;
            }
        } else { //remote repository
            if ("http://".equalsIgnoreCase(urlText.getText())) {
                setErrorMessage(Messages.errorRepositoryInvalidURL);
                return false;
            }

            try {
                url = new URL(urlText.getText());
            } catch (MalformedURLException e) {
                setErrorMessage(Messages.errorRepositoryInvalidURL);
                return false;
            }

            if (userText.getText().isEmpty()) {
                if (!passwordText.getText().isEmpty()) {
                    setErrorMessage(Messages.errorRepositoryUserNotSet);
                    return false;
                }
            } else if (passwordText.getText().isEmpty()) {
                setErrorMessage(Messages.errorRepositoryPasswordNotSet);
                return false;
            }
        }

        setErrorMessage(null);
        setMessage((site == null) ? Messages.repositoryInfoDialogNewDescription : Messages.repositoryInfoDialogEditDescription);
        return true;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton(validate());
        setErrorMessage(null);
        return control;
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    @Override
    protected void okPressed() {
        if (remoteButton.getSelection()) {
            user = userText.getText().trim();
            password = passwordText.getText().trim();
        } else {
            user = null;
            password = null;
        }
        super.okPressed();
    }

    protected String getSiteName() {
        return siteName;
    }

    protected URL getSiteURL() {
        return url;
    }

    protected String getSiteUser() {
        return user;
    }

    protected String getSitePassword() {
        return password;
    }

    void handleEnablement() {

        for (Control c : remoteControls) {
            c.setEnabled(remoteButton.getSelection());
        }

        for (Control c : localControls) {
            c.setEnabled(!remoteButton.getSelection());
        }
    }

    protected void displayDropdownMenu(Control anchor, Menu menu, boolean subtractWidth) {
        Point size = anchor.getSize();
        Point point = anchor.toDisplay(0, size.y);
        menu.setLocation(point.x - (subtractWidth ? size.x : 0), point.y);
        menu.setVisible(true);

        while (!menu.isDisposed() && menu.isVisible()) {
            Display display = menu.getShell().getDisplay();
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

}

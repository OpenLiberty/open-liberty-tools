/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContentAssistCombo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

public class NewAddonDialog extends TitleAreaDialog {
    protected static final String PREF_ADDONS = "addons";

    protected Map<String, Object> map;
    protected ContentAssistCombo archive;
    protected String archiveName;
    protected final FeatureHandler featureHandler;

    public NewAddonDialog(Shell parentShell, FeatureHandler featureHandler, Map<String, Object> map) {
        super(parentShell);
        this.map = map;
        this.featureHandler = featureHandler;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.wizInstallAddonTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_ADD_ON));
        setTitle(Messages.wizInstallAddonTitle);
        setMessage(Messages.wizInstallAddonDescription);

        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        GridLayout layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        layout.numColumns = 3;
        comp.setLayout(layout);

        Label label = new Label(comp, SWT.NONE);
        label.setText(Messages.wizInstallAddonArchiveMessage);
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 3, 1));

        label = new Label(comp, SWT.NONE);
        label.setText(Messages.wizInstallAddonArchive);
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

        ExtendsContentProvider extContentProvider = new ExtendsContentProvider();
        archive = new ContentAssistCombo(comp, extContentProvider);
        String[] exts = extContentProvider.getSuggestions("", true);
        if (exts != null) {
            for (String s : exts) {
                if (new File(s).exists()) {
                    archiveName = s;
                    archive.setText(s);
                    break;
                }
            }
        }

        archive.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        archive.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                archiveName = archive.getText().trim();
                enableOKButton(validate());
            }
        });

        Button browse = SWTUtil.createButton(comp, Messages.browse);
        ((GridData) browse.getLayoutData()).verticalAlignment = GridData.CENTER;
        browse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                FileDialog dialog = new FileDialog(getShell());
                dialog.setFilterExtensions(new String[] { "*.jar;*.zip;*.esa" });
                dialog.setFileName(archive.getText());
                String file = dialog.open();
                if (file != null)
                    archive.setText(file);
            }
        });

        label = new Label(comp, SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
        data.verticalSpan = 5;
        label.setLayoutData(data);

        return comp;
    }

    protected boolean validate() {
        if (archiveName == null || archiveName.isEmpty()) {
            setMessage(Messages.wizInstallAddonDescription, IMessageProvider.NONE);
            return false;
        }

        File file = new File(archiveName);
        if (!file.exists()) {
            setMessage(Messages.errorInvalidArchive, IMessageProvider.ERROR);
            return false;
        }

        String error = validateArchive(archiveName, featureHandler, map);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
            return false;
        }
        setMessage(Messages.wizInstallAddonDescription, IMessageProvider.NONE);
        return true;
    }

    @SuppressWarnings("unchecked")
    protected static String validateArchive(String archiveName, FeatureHandler featureHandler, Map<String, Object> map) {
        LocalProduct product = LocalProduct.create(archiveName);
        if (product == null || product.getType() == IProduct.Type.INSTALL) {
            return Messages.errorWizNotAddonJar;
        }

        IRuntimeInfo newCore = (IRuntimeInfo) map.get(AbstractDownloadComposite.RUNTIME_CORE);
        IStatus status = ProductHelper.isApplicableTo(product, newCore/* , featureHandler */);
        if (status != Status.OK_STATUS) {
            // check if the user tried to add a repository zip here and if so give them a more helpful message
            // than just "it's not a valid addon", can point them to use the configure repositories dialog
            if (SiteHelper.isZipRepoSupported(newCore) && SiteHelper.isValidOnPremZipRepository(new File(archiveName)))
                return Messages.errorAddonIsRepo;
            return status.getMessage();
        }

        if (featureHandler != null && featureHandler.containsFeatures(product.getProvideFeature())) {
            return Messages.errorAddonAlreadyInstalled;
        }

        List<String> localArchives = (List<String>) map.get(AbstractDownloadComposite.ARCHIVES);
        if (localArchives != null && !localArchives.isEmpty()) {
            int size = localArchives.size();
            for (int i = 0; i < size; i++) {
                File file = new File(localArchives.get(i).trim());
                if (product.getName().equals(file.getName())) {
                    return NLS.bind(Messages.errorWizAddonJarDup, archiveName);
                }
            }
        }
        return null;
    }

    public String getArchive() {
        return archiveName;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton(validate());
        return control;
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    protected class ExtendsContentProvider extends ContentAssistCombo.ContentProvider {
        @Override
        public String[] getSuggestions(String hint, boolean showAll) {
            // get locations from three places:
            //  - WLP archives found in obvious places like desktop
            //  - recently used locations
            List<String> items = new ArrayList<String>();
            items.addAll(Activator.getPreferenceList(PREF_ADDONS));

            // TODO: search for local add-ons
            /*
             * List<File> files = DownloadHelper.getArchives();
             * if (files != null && !files.isEmpty()) {
             * int size = files.size();
             * for (int i = 0; i < size; i++)
             * items.add(files.get(i).getAbsolutePath());
             * }
             */

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
}

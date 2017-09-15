/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.download.SiteHelper.SiteDelegate;

/**
 * A Liberty Profile repositories configuration dialog
 */
public class RepositoriesConfigurationDialog extends TitleAreaDialog {

    protected Table table;
    protected Button newButton;
    protected Button editButton;
    protected Button removeButton;
    protected Button upButton;
    protected Button downButton;
    protected CheckboxTableViewer tableViewer;
    protected SiteDelegate currentSite;
    protected List<SiteDelegate> activeSiteList = new ArrayList<SiteDelegate>();
    private boolean isRefreshRequired = false;
    protected final Map<String, Object> map;

    class RepoContentProvider implements IStructuredContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            SiteDelegate[] sites = new SiteDelegate[activeSiteList.size()];
            return activeSiteList.toArray(sites);
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // do nothing
        }

        @Override
        public void dispose() {
            // do nothing
        }
    }

    class RepoTableLabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            SiteDelegate site = (SiteDelegate) element;
            return site.getName();
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
            // do nothing
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
            // do nothing
        }

        @Override
        public void dispose() {
            // do nothing
        }
    }

    public RepositoriesConfigurationDialog(Shell parentShell, Map<String, Object> map) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        SiteDelegate[] sites = SiteHelper.getConfigurableSites();
        for (SiteDelegate site : sites) {
            activeSiteList.add(site);
        }
        this.map = map;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.configureRepoDialogTitle);
    }

    @SuppressWarnings("unused")
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_RUNTIME));
        setTitle(Messages.configureRepoDialogTitle);
        setMessage(Messages.repoPrefencePageDescriptionLabel1);

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(composite, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(composite, 4);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        composite.setLayoutData(data);

        Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        label.setVisible(false);

        label = new Label(composite, SWT.WRAP);
        label.setText(Messages.repoPrefencePageDescriptionLabel2);
        data = new GridData(SWT.FILL, SWT.CENTER, false, false);
        data.horizontalSpan = 2;
        data.widthHint = 275;
        label.setLayoutData(data);

        label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        label.setVisible(false);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.repoPrefencePageDescriptionLabel3);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL
                                     | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TableLayout tableLayout = new TableLayout();
        new TableColumn(table, SWT.NONE);
        tableLayout.addColumnData(new ColumnWeightData(100));
        table.setLayout(tableLayout);

        tableViewer = new CheckboxTableViewer(table);
        tableViewer.setContentProvider(new RepoContentProvider());
        tableViewer.setLabelProvider(new RepoTableLabelProvider());
        tableViewer.setInput("init"); // initialize the table

        tableViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent e) {
                // if no other sites are checked, don't allow the single one
                // currently checked to become unchecked.
                Object[] obj = tableViewer.getCheckedElements();
                if (obj.length == 0) {
                    tableViewer.setChecked(e.getElement(), true);
                }
            }
        });

        // Select default site
        SiteDelegate[] selectedSites = SiteHelper.getSelectedSites();
        if (selectedSites.length > 0) {
            tableViewer.setCheckedElements(selectedSites);
        }
        else {
            Object obj = tableViewer.getElementAt(0);
            if (obj != null)
                tableViewer.setChecked(obj, true);
        }

        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = ((IStructuredSelection) tableViewer.getSelection());
                if (sel.size() == 0 || sel.size() > 1)
                    return;

                Object firstElem = sel.getFirstElement();
                if (firstElem != null) {
                    currentSite = (SiteDelegate) firstElem;
                    editButton.setEnabled(currentSite != SiteHelper.getDefaultSiteDelegate());
                    removeButton.setEnabled(currentSite != SiteHelper.getDefaultSiteDelegate());
                    upButton.setEnabled(activeSiteList.size() > 1 && currentSite != SiteHelper.getDefaultSiteDelegate() && activeSiteList.indexOf(currentSite) > 0);
                    downButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) < activeSiteList.size() - 2);
                }
            }
        });

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection sel = ((IStructuredSelection) tableViewer.getSelection());
                if (sel.size() == 0 || sel.size() > 1)
                    return;

                Object firstElem = sel.getFirstElement();
                if (firstElem != null) {
                    currentSite = (SiteDelegate) firstElem;
                    if (currentSite == SiteHelper.getDefaultSiteDelegate())
                        return;

                    RepositoryInfoDialog d = new RepositoryInfoDialog(getShell(), currentSite, activeSiteList, map);
                    if (d.open() == Window.OK && isSiteInfoChanged(currentSite, d)) {
                        if (currentSite.getState() != SiteDelegate.State.COPY) {
                            int index1 = activeSiteList.indexOf(currentSite);
                            currentSite = new SiteDelegate(currentSite);
                            activeSiteList.set(index1, currentSite);
                        }

                        currentSite.setName(d.getSiteName());
                        currentSite.setURL(d.getSiteURL());
                        currentSite.setUser(d.getSiteUser());
                        currentSite.setPassword(d.getSitePassword());
                        tableViewer.refresh();
                    }
                }
            }
        });

        Composite buttonComp = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = convertVerticalDLUsToPixels(3);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 1;
        buttonComp.setLayout(layout);
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
        buttonComp.setLayoutData(data);

        newButton = SWTUtil.createButton(buttonComp, Messages.addButtonAcc);
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                RepositoryInfoDialog d = new RepositoryInfoDialog(getShell(), null, activeSiteList, map);
                if (d.open() == Window.OK) {
                    SiteDelegate site = new SiteDelegate(d.getSiteName());
                    site.setURL(d.getSiteURL());
                    site.setUser(d.getSiteUser());
                    site.setPassword(d.getSitePassword());
                    site.setState(SiteDelegate.State.NEW);
                    activeSiteList.add(site);
                    moveSite(site, -1);
                    tableViewer.refresh();
                    tableViewer.setChecked(site, true);
                    IStructuredSelection sel = ((IStructuredSelection) tableViewer.getSelection());
                    if (sel.size() == 1 && currentSite != null) {
                        upButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) > 0);
                        downButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) < activeSiteList.size() - 2);
                    }
                }
            }
        });

        editButton = SWTUtil.createButton(buttonComp, Messages.editButtonAcc);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                RepositoryInfoDialog d = new RepositoryInfoDialog(getShell(), currentSite, activeSiteList, map);
                if (d.open() == Window.OK && isSiteInfoChanged(currentSite, d)) {
                    boolean isChecked = tableViewer.getChecked(currentSite);
                    if (currentSite.getState() != SiteDelegate.State.COPY) {
                        int index1 = activeSiteList.indexOf(currentSite);
                        currentSite = new SiteDelegate(currentSite);
                        activeSiteList.set(index1, currentSite);
                    }

                    currentSite.setName(d.getSiteName());
                    currentSite.setURL(d.getSiteURL());
                    currentSite.setUser(d.getSiteUser());
                    currentSite.setPassword(d.getSitePassword());
                    tableViewer.refresh();
                    tableViewer.setChecked(currentSite, isChecked);

                    editButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            }
        });

        removeButton = SWTUtil.createButton(buttonComp, Messages.removeButton);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                activeSiteList.remove(currentSite);
                currentSite = null;
                tableViewer.refresh();
                editButton.setEnabled(false);
                removeButton.setEnabled(false);
                upButton.setEnabled(false);
                downButton.setEnabled(false);
                Object[] checkedElements = tableViewer.getCheckedElements();
                if (checkedElements == null || checkedElements.length == 0) {
                    Object obj = tableViewer.getElementAt(0);
                    if (obj != null)
                        tableViewer.setChecked(obj, true);
                }
            }
        });

        upButton = SWTUtil.createButton(buttonComp, Messages.upButton);
        upButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                moveSite(currentSite, -1);
                tableViewer.refresh();
                upButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) > 0);
                downButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) < activeSiteList.size() - 2);
            }
        });

        downButton = SWTUtil.createButton(buttonComp, Messages.downButton);
        downButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                moveSite(currentSite, 1);
                tableViewer.refresh();
                upButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) > 0);
                downButton.setEnabled(activeSiteList.size() > 1 && activeSiteList.indexOf(currentSite) < activeSiteList.size() - 2);
            }
        });

        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        upButton.setEnabled(false);
        downButton.setEnabled(false);

        return composite;
    }

    @Override
    protected void okPressed() {
        List<SiteDelegate> selectedSiteList = new ArrayList<SiteDelegate>(activeSiteList.size());
        for (SiteDelegate site : activeSiteList) {
            if (tableViewer.getChecked(site))
                selectedSiteList.add(site);
        }
        isRefreshRequired = SiteHelper.applyConfigSiteChanges(activeSiteList, selectedSiteList);
        super.okPressed();
    }

    protected void moveSite(SiteDelegate site, int direction) {
        int index = activeSiteList.indexOf(site);
        if (index == -1)
            return;

        int newIndex = index + direction;
        SiteDelegate moveSite = activeSiteList.get(newIndex);
        activeSiteList.set(newIndex, site);
        activeSiteList.set(index, moveSite);
    }

    protected void replaceSite(SiteDelegate oldSite, SiteDelegate newSite, List<SiteDelegate> sites) {
        int index = sites.indexOf(oldSite);
        if (index == -1)
            return;

        sites.set(index, newSite);
    }

    protected boolean isSiteInfoChanged(SiteDelegate site, RepositoryInfoDialog d) {
        if (!d.getSiteName().equals(nullToEmptyString(site.getName())))
            return true;

        if (!d.getSiteURL().equals(site.getURL())) {
            return true;
        }

        if (!nullToEmptyString(d.getSiteUser()).equals(nullToEmptyString(site.getUser())))
            return true;

        return !nullToEmptyString(d.getSitePassword()).equals(nullToEmptyString(site.getPassword()));
    }

    protected boolean isRefreshRequired() {
        return isRefreshRequired;
    }

    private String nullToEmptyString(String value) {
        return (value == null) ? "" : value;
    }
}

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
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * A composite used to accept a license.
 */
public class LicenseComposite extends AbstractDownloadComposite {
    static class LicenseLayout extends Layout {
        private static final int INDENT = 15;
        private static final int SPACING = 5;
        private static final int MARGIN = 5;

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
            int wHint2 = wHint;
            int hHint2 = hHint;
            if (wHint2 < 200)
                wHint2 = 200;
            if (hHint2 < 300)
                hHint2 = 300;
            return new Point(wHint2, hHint2);
        }

        @Override
        protected void layout(Composite composite, boolean flushCache) {
            Control[] children = composite.getChildren();
            Point p2 = children[2].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
            Point p3 = children[3].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);

            Rectangle r = composite.getClientArea();
            r.x += MARGIN;
            r.y += MARGIN;
            r.width -= MARGIN * 2;
            r.height -= MARGIN * 2;
            children[3].setBounds(r.x + INDENT, r.y + r.height - p3.y, r.width - INDENT, p3.y);
            children[2].setBounds(r.x + INDENT, r.y + r.height - p3.y - p2.y - SPACING, r.width - INDENT, p2.y);
            if (children[0].isEnabled()) {
                Table t = (Table) children[0];
                TableColumn col = t.getColumn(0);
                col.setWidth(r.width - t.getBorderWidth() * 2);
                int ty = Math.min(t.getItemCount(), 4) * t.getItemHeight() + t.getBorderWidth() * 2;
                children[1].setBounds(r.x, r.y + ty + SPACING, r.width, r.height - ty - p2.y - p3.y - SPACING * 4);
                children[0].setBounds(r.x, r.y, r.width, ty);
            } else {
                children[1].setBounds(r.x, r.y, r.width, r.height - p2.y - p3.y - SPACING * 3);
            }
        }
    }

    protected static final String PAGE_NAME = "license";
    protected Table licenseTable;
    protected Browser licenseBrowser;
    protected Button accept;
    protected Button decline;
    protected boolean browserAvailable;
    protected Text licenseText;

    /**
     * Create a new LicenseWizardPage.
     * 
     * @param parent a parent composite
     * @param taskModel a task model
     * @param wizard the wizard this composite is contained in
     */
    public LicenseComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler) {
        super(parent, map, container, handler);
        container.setTitle(Messages.wizLicenseTitle);
        container.setDescription(Messages.wizLicenseDescription);
        createControl();
    }

    protected void createControl() {
        setLayout(new LicenseLayout());

        licenseTable = new Table(this, SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
        licenseTable.setHeaderVisible(false);
        licenseTable.setLinesVisible(false);
        new TableColumn(licenseTable, SWT.NONE);

        licenseTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                TableItem[] selections = licenseTable.getSelection();
                if (selections.length == 0) {
                    if (browserAvailable) {
                        licenseBrowser.setText("");
                    } else {
                        licenseText.setText("");
                    }
                    return;
                }
                TableItem item = selections[0];
                if (browserAvailable) {
                    licenseBrowser.setText((String) item.getData());
                } else {
                    licenseText.setText((String) item.getData());
                }
            }
        });

        try {
            licenseBrowser = new Browser(this, SWT.BORDER);
            licenseBrowser.setFont(getFont());
            licenseBrowser.setJavascriptEnabled(false);
            licenseBrowser.addLocationListener(new LocationAdapter() {
                @Override
                public void changing(LocationEvent event) {
                    // block hyperlinks
                    if (event.location.startsWith("http://www.ibm.com"))
                        event.doit = false;
                }
            });
            browserAvailable = true;
        } catch (SWTError swtError) {
            licenseText = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
            licenseText.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            browserAvailable = false;
        }

        accept = new Button(this, SWT.RADIO);
        accept.setText(Messages.wizLicenseAccept);
        accept.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                map.put(LICENSE_ACCEPT, Boolean.valueOf(accept.getSelection()));
                setMessage(null, accept.getSelection() ? IMessageProvider.NONE : IMessageProvider.ERROR);
            }
        });

        decline = new Button(this, SWT.RADIO);
        decline.setText(Messages.wizLicenseDecline);

        Dialog.applyDialogFont(this);
    }

    private void updateLicense() {
        @SuppressWarnings("unchecked")
        Map<IProduct, License> licenseMap = (Map<IProduct, License>) map.get(LICENSE);
        if (licenseMap == null || licenseMap.isEmpty()) {
            licenseBrowser.setText(Messages.wizLicenseMissing);
            accept.setEnabled(false);
            decline.setEnabled(false);
            setMessage(null, IMessageProvider.ERROR);
            return;
        }

        licenseTable.removeAll();

        Collection<License> licenses = licenseMap.values();
        List<String> finalLicenseList = new ArrayList<String>(licenses.size());
        int count = 0;
        for (License license : licenses) {
            String licenseTextValue = browserAvailable ? convertToHTML(license.getText()) : convertToText(license.getText());
            if (!finalLicenseList.contains(licenseTextValue)) {
                TableItem item = new TableItem(licenseTable, SWT.NONE);
                item.setImage(Activator.getImage(Activator.IMG_LICENSE));
                String licenseName = license.getName();
                if (licenseName == null) {
                    item.setText(NLS.bind(Messages.licenseAgreementLabel, String.valueOf(++count)));
                } else {
                    item.setText(licenseName);
                }
                item.setData(licenseTextValue);
                finalLicenseList.add(licenseTextValue);
            }
        }
        accept.setEnabled(true);
        decline.setEnabled(true);
        licenseTable.setSelection(0);
        if (browserAvailable) {
            licenseBrowser.setText((String) licenseTable.getItem(0).getData());
        } else {
            licenseText.setText((String) licenseTable.getItem(0).getData());
        }

        if (finalLicenseList.size() == 1) {
            licenseTable.setEnabled(false);
            licenseTable.setVisible(false);
        } else {
            licenseTable.setEnabled(true);
            licenseTable.setVisible(true);
        }
        // update the layout
        licenseTable.getParent().layout(true);

        // mark complete if accept button was selected
        boolean accepted = Boolean.TRUE.equals(map.get(LICENSE_ACCEPT));
        accept.setSelection(accepted);
        decline.setSelection(!accepted);
        setMessage(null, accepted ? IMessageProvider.NONE : IMessageProvider.ERROR);
    }

    private static String convertToHTML(String s) {
        if (s.startsWith("<") || s.contains("<br") || s.contains("<p") || s.contains("<div"))
            return s;
        return s.replace("\r\n", "<br>\n");
    }

    private static String convertToText(String value) {
        String valueLowerCase = value.toLowerCase();
        int bodyStart = valueLowerCase.indexOf("<body>");
        if (bodyStart != -1) {
            int bodyEnd = valueLowerCase.lastIndexOf("</body>");
            if (bodyEnd > bodyStart) {
                return removeTags(value.substring(bodyStart + 6, bodyEnd));
            }
        }
        return removeTags(value);
    }

    private static String removeTags(String value) {
        return value.replaceAll("\\<.*?>", "").trim();
    }

    @Override
    public void enter() {
        updateLicense();
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        // no op
    }
}

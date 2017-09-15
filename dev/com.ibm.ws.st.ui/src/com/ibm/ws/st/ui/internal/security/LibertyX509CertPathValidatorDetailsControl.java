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

package com.ibm.ws.st.ui.internal.security;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.security.LibertyX509CertData;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * This kind of Composite displays information about an X.509 certificate
 * or, to a limited extent, another type of certificate.
 * It contains three controls arranged in a single columns:
 * <ol>
 * <li>For X.509 certificates only, a message (label) identifying the
 * domain name of the issuer of the untrusted certificate.</li>
 * <li>A single column table listing all the certificates in the
 * untrusted certificate path, root certificate last.</li>
 * <li>A double column table listing the attribute names and values of the
 * currently selected certificate in the single column table above it.</li>
 * </ol>
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorDetailsControl extends Composite {

    private Cert[] certs_;

    /**
     * Constructs a new control for the given certificate path and
     * the specific untrusted certificate at the given index in the path.
     * 
     * @param parent The parent composite for this control.
     * @param style The style for this control.
     * @param certificates The certificate path to display.
     * @param index The index of a certificate in the path that is not
     *            trusted, or -1 if there is no specific untrusted certificate.
     */
    public LibertyX509CertPathValidatorDetailsControl(Composite parent, int style, Certificate[] certificates, int index) {
        super(parent, style);
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "parent=[" + parent + //$NON-NLS-1$
                                    "] style=[" + style + //$NON-NLS-1$
                                    "] certificates=[" + Arrays.toString(certificates) + //$NON-NLS-1$
                                    "] index=" + index + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }
        //
        // Initialize this composite.
        //
        loadCerts(certificates);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        //
        // If there is at least one certificate in the path and the
        // root certificate is an X.509 certificate, display a message
        // telling the user that the identity of the root certificate
        // could not be verified. Notice we use the root certificate,
        // not the certificate identified by the "index" parameter,
        // though in practice "index" will point to the root anyways.
        //
        if (certificates.length > 0 && certificates[certificates.length - 1] instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) certificates[certificates.length - 1];
            X500Principal issuerPrincipal = x509Certificate.getIssuerX500Principal();
            String issuerCN = issuerPrincipal.getName();
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "issuerCN=[" + issuerCN + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            Label messageLabel = new Label(parent, SWT.WRAP);
            messageLabel.setText(NLS.bind(Messages.X509_CONTROL_ISSUER, issuerCN));
            messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        }
        //
        // The following table lists the domain names of all the certificates
        // in the certificate path, from target to root (aka. trust anchor).
        // We initially select the certificate identified by the "index"
        // parameter to be untrusted or, if "index" is -1 or out of range, the root.
        //
        Composite certListComposite = new Composite(parent, SWT.NONE);
        certListComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TableColumnLayout certListTableColumnLayout = new TableColumnLayout();
        certListComposite.setLayout(certListTableColumnLayout);
        //
        final TableViewer certListTableViewer = new TableViewer(certListComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table certListTable = certListTableViewer.getTable();
        certListTable.setHeaderVisible(true);
        certListTable.setLinesVisible(true);
        //
        TableViewerColumn certListTableViewerColumn = new TableViewerColumn(certListTableViewer, SWT.NONE);
        TableColumn certListTableColumn = certListTableViewerColumn.getColumn();
        certListTableColumnLayout.setColumnData(certListTableColumn, new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));
        certListTableColumn.setResizable(true);
        certListTableColumn.setText(Messages.X509_CONTROL_COLUMN_TITLE_CERTIFICATE);
        //
        certListTableViewer.setLabelProvider(new CertListLabelProvider());
        certListTableViewer.setContentProvider(new CertListContentProvider());
        certListTableViewer.setInput(certs_);
        certListTable.select(index >= 0 && index < certs_.length ? index : certs_.length - 1);
        //
        // The following table lists the attribute names and values of
        // whatever certificate is currently selected in the above table.
        // We initially display the details for the selected certificate
        // in the table above.
        //
        Label certDetailsLabel = new Label(parent, SWT.WRAP);
        certDetailsLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        certDetailsLabel.setText(Messages.X509_CONTROL_CERTIFICATE_DETAILS_LABEL);
        //
        Composite certDetailsComposite = new Composite(parent, SWT.NONE);
        certDetailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TableColumnLayout certDetailsTableColumnLayout = new TableColumnLayout();
        certDetailsComposite.setLayout(certDetailsTableColumnLayout);
        //
        final TableViewer certDetailsTableViewer = new TableViewer(certDetailsComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table certDetailsTable = certDetailsTableViewer.getTable();
        certDetailsTable.setHeaderVisible(true);
        certDetailsTable.setLinesVisible(true);
        //
        TableViewerColumn certDetailsTableViewerColumn1 = new TableViewerColumn(certDetailsTableViewer, SWT.NONE);
        TableColumn certDetailsTableColumn1 = certDetailsTableViewerColumn1.getColumn();
        certDetailsTableColumnLayout.setColumnData(certDetailsTableColumn1, new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));
        certDetailsTableColumn1.setResizable(true);
        certDetailsTableColumn1.setText(Messages.X509_CONTROL_COLUMN_TITLE_ATTRIBUTE);
        //
        TableViewerColumn certDetailsTableViewerColumn2 = new TableViewerColumn(certDetailsTableViewer, SWT.NONE);
        TableColumn certDetailsTableColumn2 = certDetailsTableViewerColumn2.getColumn();
        certDetailsTableColumnLayout.setColumnData(certDetailsTableColumn2, new ColumnWeightData(2, ColumnWeightData.MINIMUM_WIDTH, true));
        certDetailsTableColumn2.setResizable(true);
        certDetailsTableColumn2.setText(Messages.X509_CONTROL_COLUMN_TITLE_VALUE);
        //
        certDetailsTableViewer.setLabelProvider(new CertLabelProvider());
        certDetailsTableViewer.setContentProvider(new CertContentProvider());
        int selectionIndex = certListTable.getSelectionIndex();
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "selectionIndex=[" + selectionIndex + //$NON-NLS-1$
                                    "]"); //$NON-NLS-1$
        }
        if (selectionIndex > -1 && selectionIndex < certs_.length) {
            certDetailsTableViewer.setInput(certs_[selectionIndex]);
        }
        //
        // Tie selection events in the table of certificates to
        // the table of certificate details. Only update the latter
        // if the something different is selected in the former.
        //
        certListTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) certListTableViewer.getSelection();
                Object object = selection.getFirstElement();
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "selected object=[" + object + //$NON-NLS-1$
                                            "]"); //$NON-NLS-1$
                }
                if (object != null && !object.equals(certDetailsTableViewer.getInput())) {
                    certDetailsTableViewer.setInput(object);
                }
            }
        });
        //
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "return"); //$NON-NLS-1$
        }
    }

    //
    // This is the content provider for the rows of the table of certificates.
    // The getElements() method returns the array of certificate
    // descriptors corresponding to the certificates in the path.
    //
    private static class CertListContentProvider implements IStructuredContentProvider {

        /**
         * 
         */
        public CertListContentProvider() {} // empty constructor to get rid of access warning

        @Override
        public void dispose() {
            // Do nothing.
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing.
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "inputElement=[" + inputElement + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            if (inputElement instanceof Cert[]) {
                return (Cert[]) inputElement;
            }
            return null;
        }
    }

    //
    // This is the label provider for a row in the table of certificates.
    // The getColumnText() method returns the name of the certificate
    // for the one and only column of the row of the table.
    //
    private static class CertListLabelProvider extends LabelProvider implements ITableLabelProvider {

        /**
         * 
         */
        public CertListLabelProvider() {} // empty constructor to get rid of access warning

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "element=[" + element + //$NON-NLS-1$
                                        "] columnIndex=[" + columnIndex + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            if (element instanceof Cert) {
                return ((Cert) element).getName();
            }
            return null;
        }

        public void dispose() {
            super.dispose();
        }
    }

    //
    // This is the content provider for the rows of the table of certificate attributes.
    // The getElements() method returns the array of entries describing
    // the attributes of the certificate.
    //
    private static class CertContentProvider implements IStructuredContentProvider {

        /**
         * 
         */
        public CertContentProvider() {} // empty constructor to get rid of access warning

        @Override
        public void dispose() {
            // Do nothing.
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing.
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "inputElement=[" + inputElement + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            if (inputElement instanceof Cert) {
                Cert cert = (Cert) inputElement;
                return cert.getCertData().entries();
            }
            return null;
        }
    }

    //
    // This is the label provider for a row in the table of certificate attributes.
    // The getColumnText() method returns the attribute name or value of an entry
    // for the first or second column, respectively, of the row of the table.
    //
    private static class CertLabelProvider extends LabelProvider implements ITableLabelProvider {

        /**
         * 
         */
        public CertLabelProvider() {} // empty constructor to get rid of access warning

        @Override
        public void dispose() {
            super.dispose();
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "element=[" + element + //$NON-NLS-1$
                                        "] columnIndex=[" + columnIndex + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
            }
            if (element instanceof LibertyX509CertData.Entry) {
                LibertyX509CertData.Entry entry = (LibertyX509CertData.Entry) element;
                return (columnIndex == 0 ? entry.getKey() : entry.getVal());
            }
            return null;
        }

    }

    //
    // This method builds an array of Cert objects
    // corresponding to the array of Certificates.
    //
    private void loadCerts(Certificate[] certificates) {
        if (certificates != null) {
            certs_ = new Cert[certificates.length];
            for (int i = 0; i < certificates.length; i++) {
                certs_[i] = new Cert(i, null, certificates[i]);
            }
        } else {
            certs_ = new Cert[0];
        }
    }

    //
    // A Cert object captures a Certificate, its position
    // in a certificate path, its name for display to the
    // user, and its attribute names and values in the form
    // of an LibertyX509CertData object.
    //
    private static class Cert {

        private final int index_;
        private String name_;
        private final Certificate certificate_;
        private LibertyX509CertData certData_;

        //
        // Constructs a new Cert object.
        //
        public Cert(int index, String name, Certificate certificate) {
            index_ = index;
            name_ = name;
            certificate_ = certificate;
        }

        //
        // Returns the subject principal domain name of the X.509
        // certificate within this Cert object. If the certificate
        // is not X.509, this method returns the index as the name.
        //
        public String getName() {
            if (name_ == null) {
                if (certificate_ instanceof X509Certificate) {
                    name_ = ((X509Certificate) certificate_).getSubjectX500Principal().getName();
                }
            }
            return name_ == null ? Integer.toString(index_ + 1) : name_;
        }

        //
        // Returns the LibertyX509CertData object containing the
        // certificate attribute names and values of interest.
        //
        public LibertyX509CertData getCertData() {
            if (certData_ == null && certificate_ != null) {
                certData_ = new LibertyX509CertData(certificate_);
            }
            return certData_;
        }

        //
        // Returns a non translatable string representation.
        // @return A string representation of the object, never null.
        //
        @Override
        public String toString() {
            return "{" + index_ + "," + name_ + "," + certificate_ + "," + certData_ + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
    }
}

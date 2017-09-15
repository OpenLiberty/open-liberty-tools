/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereRuntime;

public class ProductInfoPropertyPage extends PropertyPage {
    public ProductInfoPropertyPage() {
        super();
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent) {
        try {
            IAdaptable element = getElement();
            IRuntime runtime = null;
            IServer server = (IServer) element.getAdapter(IServer.class);
            if (server != null)
                runtime = server.getRuntime();
            else
                runtime = (IRuntime) element.getAdapter(IRuntime.class);

            WebSphereRuntime wsRuntime = null;
            if (runtime != null)
                wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

            final Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 1;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            // progress info
            final Label progressLabel = new Label(composite, SWT.NONE);
            progressLabel.setText(com.ibm.ws.st.core.internal.Messages.taskProductInfo);
            progressLabel.setLayoutData(new GridData(GridData.FILL, GridData.END, true, true));

            final ProgressIndicator progress = new ProgressIndicator(composite);
            progress.setLayoutData(new GridData(GridData.FILL, GridData.END, true, false));

            Dialog.applyDialogFont(composite);

            if (wsRuntime == null) {
                Label label = new Label(composite, SWT.READ_ONLY | SWT.NO_TRIM);
                label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
                label.setText(Messages.productInfoUnavailable);

                progressLabel.dispose();
                progress.dispose();
            } else {
                final Display display = getShell().getDisplay();
                final WebSphereRuntime wsRuntime2 = wsRuntime;
                Thread t = new Thread("Liberty product info") {
                    @Override
                    public void run() {
                        IProgressMonitor monitor = new NullProgressMonitor() {
                            @Override
                            public void beginTask(final String name, final int max) {
                                display.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.beginTask(max);
                                    }
                                });
                            }

                            @Override
                            public void internalWorked(final double work) {
                                display.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.worked(work);
                                    }
                                });
                            }
                        };

                        monitor.beginTask(com.ibm.ws.st.core.internal.Messages.taskProductInfo, 200);

                        final int[] indent = new int[1]; // hint to get text area and feature info label aligned 
                        try {
                            final String version = wsRuntime2.getProductInfo("version", new SubProgressMonitor(monitor, 100));
                            display.syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    Text versionText = new Text(composite, SWT.READ_ONLY | SWT.NO_TRIM | SWT.WRAP);
                                    versionText.setForeground(composite.getForeground());
                                    versionText.setBackground(composite.getBackground());
                                    versionText.setText(version);

                                    GridData data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
                                    versionText.setLayoutData(data);
                                    versionText.moveAbove(progressLabel);
                                    composite.layout(true);
                                    indent[0] = Math.abs(versionText.computeTrim(0, 0, 100, 100).x);
                                }
                            });
                        } catch (Exception e) {
                            display.syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    Label label = new Label(composite, SWT.READ_ONLY | SWT.NO_TRIM);
                                    label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
                                    label.setText(Messages.productInfoUnavailable);
                                }
                            });
                        }

                        String featureInfo = null;
                        try {
                            featureInfo = wsRuntime2.getProductInfo("featureInfo", new SubProgressMonitor(monitor, 100));
                        } catch (Exception e) {
                            // ignore, logged elsewhere
                        }
                        monitor.done();

                        final String featureInfo2 = featureInfo;
                        display.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                progressLabel.dispose();
                                progress.dispose();

                                Label featureInfoLabel = new Label(composite, SWT.NONE);
                                featureInfoLabel.setText(Messages.productInfoFeatures);
                                GridData data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
                                data.verticalIndent = 10;
                                data.horizontalIndent = indent[0];
                                featureInfoLabel.setLayoutData(data);

                                FontData[] fontData = featureInfoLabel.getFont().getFontData();
                                for (int i = 0; i < fontData.length; i++)
                                    fontData[i].setStyle(SWT.BOLD);

                                final Font font = new Font(featureInfoLabel.getDisplay(), fontData);
                                featureInfoLabel.setFont(font);
                                featureInfoLabel.addDisposeListener(new DisposeListener() {
                                    @Override
                                    public void widgetDisposed(DisposeEvent e) {
                                        font.dispose();
                                    }
                                });

                                if (featureInfo2 == null) {
                                    Label label = new Label(composite, SWT.READ_ONLY | SWT.NO_TRIM);
                                    label.setText(Messages.productInfoUnavailable);
                                    data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
                                    data.horizontalIndent = 15;
                                    label.setLayoutData(data);
                                } else {
                                    final Text featureInfoText = new Text(composite, SWT.READ_ONLY | SWT.NO_TRIM | SWT.WRAP | SWT.V_SCROLL);
                                    featureInfoText.setForeground(composite.getForeground());
                                    featureInfoText.setBackground(composite.getBackground());
                                    featureInfoText.setText(featureInfo2);
                                    featureInfoText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
                                }
                                composite.layout(true);
                            }
                        });
                    }
                };
                t.setDaemon(true);
                t.start();
            }

            return composite;
        } catch (Exception e) {
            Trace.logError("Error creating product info property page", e);
            return null;
        }
    }
}

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
package com.ibm.ws.st.ui.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.LibertyConfigSyncConflictHandler;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class LibertyUIConfigConflictHandler extends LibertyConfigSyncConflictHandler {

    /** {@inheritDoc} */
    @Override
    public Resolution handleConflict(final List<Pair> conflictFiles, IPath tempDirectory, JMXConnection jmxConnection, String remoteConfigRoot) throws Exception {
        Resolution res = null;
        boolean isConfigRootFile = false;
        StringBuffer modifiedFiles = new StringBuffer(); // for label to list conflicting files
        for (Pair p : conflictFiles) {
            String file = p.getRight().replace('\\', '/');
            if (remoteConfigRoot != null && remoteConfigRoot.equals(file))
                isConfigRootFile = true;
            modifiedFiles.append(file + "\n");
        }

        // For the comparison dialog we're only supporting the root configuration file (ie. server.xml located in ${server.config.dir})
        // TODO add the ability to compare multiple files in the compare dialog

        // Compare dialog is for server.xml file only. There is a possibility that the modifiedFiles list might also have a non root file(e.g. server.env). 
        // In that case, compare dialog is shown, if user selects "compare" comparison is only done on server.xml file and all the files in the list are synced
        // if user selects "Overwrite" no comparison dialog is shown.
        if (isConfigRootFile)
            res = showConflictDialogWithCompareOption(modifiedFiles.toString());
        else
            res = showConflictDialog(modifiedFiles.toString()); // nothing to compare so just show the basic dialog without compare option

        if (res != null && res == Resolution.MERGE) {
            final ArrayList<Pair> comparableFiles = new ArrayList<Pair>();
            // download all remote files
            for (Pair p : conflictFiles) {
                String remoteFilePath = p.getRight(); // right is remote
                IPath remotePath = new Path(remoteFilePath);
                String name = remotePath.lastSegment();
                File tmpFile = tempDirectory.append("tmpRemote" + name).toFile();
                if (tmpFile.exists())
                    tmpFile.delete();
                String downloadCopy = tmpFile.getAbsolutePath();
                jmxConnection.downloadFile(remoteFilePath, downloadCopy);
                comparableFiles.add(new Pair(p.getLeft(), downloadCopy));
            }

            final String hostname = jmxConnection.getHost();
            final int[] mergeResult = { 0 };
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    final Shell shell = Display.getDefault().getActiveShell();
                    ConfigCompareDialog compareDialog = new ConfigCompareDialog(shell, Messages.configSyncCompareMessage, SWT.NONE, comparableFiles, hostname);
                    mergeResult[0] = compareDialog.open();
                }
            });
            switch (mergeResult[0]) {
                case 0:
                    res = Resolution.MERGE;
                    break;
                case 1:
                    res = Resolution.CANCEL;
                    break;
            }
        }

        return res;
    }

    private Resolution showConflictDialogWithCompareOption(final String files) {
        Resolution res = null;
        final int[] result = { 1 };
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {

                final Shell shell = Display.getDefault().getActiveShell();

                MessageDialog dlg = new MessageDialog(shell,
                                Messages.title,
                                null,
                                NLS.bind(Messages.configSyncConflicts, files.replace("\\", "/")),
                                MessageDialog.QUESTION,
                                new String[] {
                                              Messages.configSyncCompare,
                                              Messages.configSyncOverwriteRemote,
                                              Messages.configSyncCancelButton },
                                0);

                result[0] = dlg.open();
            }
        });

        switch (result[0]) {
            case 0:
                res = Resolution.MERGE;
                break;
            case 1:
                res = Resolution.OVERWRITE_REMOTE;
                break;
            case 2:
                res = Resolution.CANCEL;
                break;
        }
        return res;
    }

    private Resolution showConflictDialog(final String files) {
        Resolution res = null;
        final int[] result = { 0 };
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {

                final Shell shell = Display.getDefault().getActiveShell();

                MessageDialog dlg = new MessageDialog(shell,
                                Messages.title,
                                null,
                                NLS.bind(Messages.configSyncConflicts, files.replace("\\", "/")),
                                MessageDialog.QUESTION,
                                new String[] {
                                              Messages.configSyncOverwriteRemote,
                                              Messages.configSyncCancelButton },
                                0);

                result[0] = dlg.open();
            }
        });

        switch (result[0]) {
            case 0:
                res = Resolution.OVERWRITE_REMOTE;
                break;
            case 1:
                res = Resolution.CANCEL;
                break;
        }
        return res;
    }

    class ConfigCompareDialog extends TitleAreaDialog {
        protected String message;
        protected int style;
        protected List<Pair> comparableFiles;
        protected String hostname;
        protected TextMergeViewer textMerge = null;
        protected File localFile;
        protected File remoteFileCopy;

        public ConfigCompareDialog(Shell parentShell, String message, int style, List<Pair> comparableFiles, String hostname) {
            super(parentShell);
            this.message = message;
            this.style = style;
            this.comparableFiles = comparableFiles;
            this.hostname = hostname;
            setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
            setHelpAvailable(((style & PromptHandler.STYLE_HELP) != 0));
            setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
            Pair initialComparison = comparableFiles.get(0);
            this.localFile = new File(initialComparison.getLeft());
            this.remoteFileCopy = new File(initialComparison.getRight());
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Messages.title);
            setShellStyle(SWT.RESIZE);
        }

        /** {@inheritDoc} */
        @Override
        protected Control createDialogArea(Composite parent) {
            GridLayout layout = new GridLayout();
            layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
            layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
            layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
            layout.numColumns = 1;
            GridData data = new GridData(GridData.FILL_BOTH);
            data.grabExcessHorizontalSpace = true;
            data.grabExcessVerticalSpace = true;

            setTitle(Messages.configSyncCompare);
            setMessage(message);

            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(layout);
            comp.setLayoutData(data);

            data = new GridData(GridData.FILL_BOTH);
            data.grabExcessHorizontalSpace = true;
            data.grabExcessVerticalSpace = true;
            CompareConfiguration cc = new CompareConfiguration();
            cc.setLeftLabel(NLS.bind(Messages.configSyncCompareLeftLabel, localFile.getName()));
            cc.setRightLabel(NLS.bind(Messages.configSyncCompareLeftLabel, remoteFileCopy.getName()));
            CompareViewerPane pane = new CompareViewerPane(comp, SWT.NONE);
            pane.setLayoutData(data);

            data = new GridData(GridData.FILL_BOTH);
            data.grabExcessHorizontalSpace = true;
            data.grabExcessVerticalSpace = true;
            textMerge = new TextMergeViewer(pane, cc);
            pane.setContent(textMerge.getControl());

            textMerge.getControl().setLayoutData(data);
            textMerge.setContentProvider(new IMergeViewerContentProvider() {

                @Override
                public void dispose() {
                    // do nothing
                }

                @Override
                public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
                    // do nothing
                }

                @Override
                public Object getAncestorContent(Object arg0) {
                    return null;
                }

                @Override
                public Image getAncestorImage(Object arg0) {
                    return null;
                }

                @Override
                public String getAncestorLabel(Object arg0) {
                    return null;
                }

                @Override
                public Object getLeftContent(Object arg0) {
                    String content = readFileContents(localFile);
                    return new Document(content);
                }

                @Override
                public Image getLeftImage(Object arg0) {
                    return null;
                }

                @Override
                public String getLeftLabel(Object arg0) {
                    return NLS.bind(Messages.configSyncCompareLeftLabel, localFile.getName());
                }

                @Override
                public Object getRightContent(Object arg0) {
                    String content = readFileContents(remoteFileCopy);
                    return new Document(content);
                }

                @Override
                public Image getRightImage(Object arg0) {
                    return null;
                }

                @Override
                public String getRightLabel(Object arg0) {
                    return NLS.bind(Messages.configSyncCompareRightLabel, localFile.getName(), hostname);
                }

                @Override
                public boolean isLeftEditable(Object arg0) {
                    return true;
                }

                @Override
                public boolean isRightEditable(Object arg0) {
                    return false;
                }

                @Override
                public void saveLeftContent(Object input, byte[] bytes) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(localFile, false); // false for overwriting contents
                        fos.write(bytes);
                    } catch (IOException e) {
                        Trace.logError("Couldn't save merges to local file: " + localFile.getAbsolutePath(), e);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }

                @Override
                public void saveRightContent(Object arg0, byte[] arg1) {
                    // right content is remote file which is always read only
                }

                @Override
                public boolean showAncestor(Object arg0) {
                    return false;
                }

                private String readFileContents(File file) {
                    String content = null;
                    Scanner scanner = null;
                    try {
                        scanner = new Scanner(file);
                        content = scanner.useDelimiter("\\Z").next();
                    } catch (Exception e) {
                        Trace.logError("Could not read local file contents: " + localFile.getAbsolutePath(), e);
                    } finally {
                        if (scanner != null) {
                            try {
                                scanner.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                    return content;
                }

            });
            return comp;
        }

        /** {@inheritDoc} */
        @Override
        protected Control createButtonBar(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            // create a layout with spacing and margins appropriate for the font
            // size.
            GridLayout layout = new GridLayout();
            layout.numColumns = 0; // this is incremented by createButton
            layout.makeColumnsEqualWidth = true;
            layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
            layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
            layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
            composite.setLayout(layout);
            GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END
                                         | GridData.VERTICAL_ALIGN_CENTER);
            data.grabExcessHorizontalSpace = true;
            composite.setLayoutData(data);
            composite.setFont(parent.getFont());

            // create help control if needed
            if (isHelpAvailable()) {
                Control helpControl = createHelpControl(composite);
                ((GridData) helpControl.getLayoutData()).horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            }

            // create buttons
            // "Publish" is used instead of "OK" label
            createButton(composite, IDialogConstants.OK_ID, Messages.configSyncPublishButton,
                         true);
            createButton(composite, IDialogConstants.CANCEL_ID,
                         Messages.configSyncCancelButton, false);

            return composite;
        }

        /** {@inheritDoc} */
        @Override
        protected void okPressed() {
            if (textMerge != null && textMerge.internalIsLeftDirty()) {
                textMerge.flushLeft(new NullProgressMonitor());
            }
            super.okPressed();
        }

        /** {@inheritDoc} */
        @Override
        protected Point getInitialSize() {
            return new Point(800, 600);
        }
    }

}

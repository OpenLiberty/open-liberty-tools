/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;

public class InstallContentComposite extends AbstractDownloadComposite {
    protected Text installDir;
    protected Label contentLabel;
    protected Label sizeLabel;

    public InstallContentComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler) {
        super(parent, map, container, handler);
        container.setTitle(Messages.wizInstallContentTitle);
        container.setDescription(Messages.wizInstallContentDescription);
        createControl();
    }

    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        layout.numColumns = 2;
        setLayout(layout);

        final Group installGroup = new Group(this, SWT.NONE);
        installGroup.setText(Messages.wizInstallFolder);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        installGroup.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        installGroup.setLayoutData(data);

        installDir = new Text(installGroup, SWT.NONE);
        installDir.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        installDir.setText((String) map.get(FOLDER));
        installDir.setEnabled(false);

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.wizInstallArchiveSize);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        sizeLabel = new Label(this, SWT.NONE);
        sizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        label = new Label(this, SWT.NONE);
        label.setText(Messages.wizInstallFileName);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        contentLabel = new Label(this, SWT.WRAP);
        contentLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

        Dialog.applyDialogFont(this);
    }

    @SuppressWarnings("unchecked")
    private void updateInfo() {
        long size = 0;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        List<IProduct> productList = (List<IProduct>) map.get(SELECTED_DOWNLOADERS);
        for (IProduct p : productList) {
            size += p.getSize();
            if (!first)
                sb.append(",\n");
            else
                first = false;
            sb.append(p.getName());
        }
        sizeLabel.setText(DownloadHelper.getSize(size));
        contentLabel.setText(sb.toString());
        layout();
    }

    @Override
    public void enter() {
        updateInfo();
        installDir.setFocus();
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        // no-op
    }
}

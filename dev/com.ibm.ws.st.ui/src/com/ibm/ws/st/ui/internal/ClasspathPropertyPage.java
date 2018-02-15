/*******************************************************************************
 * Copyright (c) 2012, 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.ibm.ws.st.core.internal.ProjectPrefs;
import com.ibm.ws.st.core.internal.Trace;

public class ClasspathPropertyPage extends PropertyPage {
    protected ProjectPrefs prefs;

    protected boolean updating;
    protected boolean changed;

    protected Button excludeThirdParty;
    protected Button excludeStable;
    protected Button excludeIBMAPI;
    protected Button excludeUnknown;

    protected IProject project;

    public ClasspathPropertyPage() {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        try {
            IAdaptable element = getElement();
            project = element.getAdapter(IProject.class);
            if (project != null)
                prefs = new ProjectPrefs(project);

            Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 1;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            //IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
            //whs.setHelp(composite, ContextIds.CLASSPATH_PROPERTY_PAGE);

            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.runtimeClasspathMessage);

            excludeThirdParty = new Button(composite, SWT.CHECK);
            excludeThirdParty.setText(Messages.runtimeClasspathExcludeThirdParty);
            GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            data.horizontalIndent = 15;
            excludeThirdParty.setLayoutData(data);
            excludeThirdParty.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    if (updating)
                        return;
                    changed = true;
                    prefs.setExcludeThirdPartyAPI(excludeThirdParty.getSelection());
                }
            });

            excludeStable = new Button(composite, SWT.CHECK);
            excludeStable.setText(Messages.runtimeClasspathExcludeStable);
            data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            data.horizontalIndent = 15;
            excludeStable.setLayoutData(data);
            excludeStable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    if (updating)
                        return;
                    changed = true;
                    prefs.setExcludeStableAPI(excludeStable.getSelection());
                }
            });

            excludeIBMAPI = new Button(composite, SWT.CHECK);
            excludeIBMAPI.setText(Messages.runtimeClasspathExcludeIBM);
            data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            data.horizontalIndent = 15;
            excludeIBMAPI.setLayoutData(data);
            excludeIBMAPI.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    if (updating)
                        return;
                    changed = true;
                    prefs.setExcludeIBMAPI(excludeIBMAPI.getSelection());
                }
            });

            excludeUnknown = new Button(composite, SWT.CHECK);
            excludeUnknown.setText(Messages.runtimeClasspathExcludeUnknown);
            data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            data.horizontalIndent = 15;
            excludeUnknown.setLayoutData(data);
            excludeUnknown.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    if (updating)
                        return;
                    changed = true;
                    prefs.setExcludeUnrecognized(excludeUnknown.getSelection());
                }
            });

            init();

            Dialog.applyDialogFont(composite);

            return composite;
        } catch (Exception e) {
            Trace.logError("Error creating property page", e);
            return null;
        }
    }

    protected void init() {
        updating = true;
        excludeThirdParty.setSelection(prefs.isExcludeThirdPartyAPI());
        excludeStable.setSelection(prefs.isExcludeStableAPI());
        excludeIBMAPI.setSelection(prefs.isExcludeIBMAPI());
        excludeUnknown.setSelection(prefs.isExcludeUnrecognized());
        updating = false;
    }

    @Override
    protected void performDefaults() {
        prefs.reset();
        changed = true;
        init();
    }

    protected boolean save() {
        if (prefs == null)
            return true;

        boolean b = prefs.save();

        if (changed) {
            try {
                // force reset of project classpath
                IPath path = new Path("org.eclipse.jst.server.core.container/com.ibm.ws.st.core.runtimeClasspathProvider");
                IJavaProject javaProject = JavaCore.create(project);
                IClasspathEntry[] entries = javaProject.getRawClasspath();
                for (IClasspathEntry entry : entries) {
                    IPath entryPath = entry.getPath();
                    if (entryPath.segmentCount() > 1 && path.segment(0).equals(entryPath.segment(0)) && path.segment(1).equals(entryPath.segment(1))) {
                        JavaCore.setClasspathContainer(entryPath, new IJavaProject[] { javaProject }, new IClasspathContainer[] { null }, null);
                    }
                }
            } catch (Exception e) {
                Trace.logError("Could not reset the classpath container: " + project, e);
            }
            changed = false;
        }
        return b;
    }

    @Override
    protected void performApply() {
        save();
    }

    @Override
    public boolean performOk() {
        return save();
    }
}

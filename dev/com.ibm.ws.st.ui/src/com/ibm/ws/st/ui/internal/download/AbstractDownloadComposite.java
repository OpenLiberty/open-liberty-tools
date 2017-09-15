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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.ibm.ws.st.ui.internal.Activator;

public abstract class AbstractDownloadComposite extends Composite {
    protected static final String ADDON_DOWNLOADERS = "addonDownloaders";
    public static final String SELECTED_DOWNLOADERS = "selectedDownloaders";
    public static final String LICENSE = "license";
    protected static final String LICENSE_ACCEPT = "accept";
    public static final String FOLDER = "folder";
    public static final String RUNTIME_TYPE_ID = "runtimeTypeId";
    protected static final String UNZIP = "unzip";
    public static final String ARCHIVES = "archives";
    public static final String SELECTED_ADDONS = "selectedAddOns";
    public static final String SELECTED_CORE_MANAGER = "coreManager";
    public static final String RUNTIME_CORE = "core";
    public static final String RUNTIME_EXTEND = "extend";
    protected static final String RUNTIME_SITE = "runtimeSite";
    public static final String PRODUCT_AUTHENTICATION = "productAuthentication";
    protected static final String SITE_AUTHENTICATION = "siteAuthentication";
    public static final String ADDON_MAP = "addonMap";
    public static final String INSTALL_RESULT = "installResult";
    public static final String RUNTIME_HANDLER = "runtimeHandler";
    public static final String TYPE_FILTER_PRESET = "typeFilterPreset";

    protected Map<String, Object> map;
    private final IMessageHandler handler;
    private final IContainer container;

    public AbstractDownloadComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler) {
        super(parent, SWT.NONE);
        this.handler = handler;
        this.container = container;
        this.map = map;
        container.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_ADD_ON));
    }

    public IContainer getContainer() {
        return container;
    }

    /**
     * Called to tell the composite to finish any extra
     */
    public void exit() {
        // empty
    }

    public void enter() {
        // empty
    }

    abstract public void performCancel(IProgressMonitor monitor) throws CoreException;

    protected void setMessage(String newMessage, int newType) {
        handler.setMessage(newMessage, newType);
    }

    public static interface IMessageHandler {
        public void setMessage(String message, int severity);
    }

    public static interface IContainer {
        public void setTitle(String title);

        public void setDescription(String desc);

        public void setImageDescriptor(ImageDescriptor image);

        public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InterruptedException, InvocationTargetException;
    }
}

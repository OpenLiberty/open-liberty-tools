/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import com.ibm.ws.st.core.internal.config.validation.FilterItem;
import com.ibm.ws.st.core.internal.config.validation.ValidationFilterListener;
import com.ibm.ws.st.core.internal.config.validation.ValidationFilterSettings;

public class IgnoreFilterTreeContentProvider implements ITreeContentProvider {

    protected TreeViewer treeViewer;
    protected ValidationFilterListener listener;

    @Override
    public void dispose() {
        Object input = treeViewer.getInput();
        if (input != null && input instanceof ValidationFilterSettings) {
            ((ValidationFilterSettings) input).removeListener(listener);
        }
        treeViewer = null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        treeViewer = (TreeViewer) viewer;

        if (listener == null) {
            listener = new ValidationFilterListener() {
                @Override
                public void filterRemoved(FilterItem item) {
                    removeItem(item);
                }
            };
        }

        if (oldInput != null && oldInput instanceof ValidationFilterSettings) {
            ((ValidationFilterSettings) oldInput).removeListener(listener);
        }

        if (newInput != null && newInput instanceof ValidationFilterSettings) {
            ((ValidationFilterSettings) newInput).addFilterListener(listener);
        }
    }

    protected void removeItem(final FilterItem item) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                treeViewer.remove(item);
            }
        });
    }

    @Override
    public Object[] getChildren(Object element) {
        if (element instanceof FilterItem) {
            return ((FilterItem) element).getChildren();
        }
        return null;
    }

    @Override
    public Object[] getElements(Object element) {
        return getChildren(element);
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof FilterItem) {
            return ((FilterItem) element).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof FilterItem) {
            return ((FilterItem) element).hasChildren();
        }
        return false;
    }
}

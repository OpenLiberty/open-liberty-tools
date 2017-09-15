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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Helper class that provides hover help on tables and trees.
 */
public class HoverHelper {

    public static final String HOVER_DATA = "hoverData";
    public static final String DATA_ITEM = "dataItem";

    /**
     * Add hover help support to a table.
     * Table items that need hover help should use setData with HOVER_DATA
     * as the key and the tooltip string as the value.
     */
    public static void addHoverHelp(final Table table) {
        // Disable native tooltip
        table.setToolTipText("");

        final Shell shell = table.getShell();
        final Display display = shell.getDisplay();

        // Implement a "fake" tooltip
        final Listener labelListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                Label label = (Label) event.widget;
                Shell shell = label.getShell();
                switch (event.type) {
                    case SWT.MouseDown:
                        Event e = new Event();
                        e.item = (TableItem) label.getData(DATA_ITEM);
                        // Assuming table is single select, set the selection as if
                        // the mouse down event went through to the table
                        table.setSelection(new TableItem[] { (TableItem) e.item });
                        table.notifyListeners(SWT.Selection, e);
                        shell.dispose();
                        table.setFocus();
                        break;
                    case SWT.MouseExit:
                        shell.dispose();
                        break;
                    default:
                        break;
                }
            }
        };

        Listener tableListener = new Listener() {
            Shell tip = null;
            Label label = null;

            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                    case SWT.Dispose:
                        case SWT.KeyDown:
                        case SWT.MouseMove: {
                        if (tip == null)
                            break;
                        tip.dispose();
                        tip = null;
                        label = null;
                        break;
                    }
                    case SWT.MouseHover: {
                        TableItem item = table.getItem(new Point(event.x, event.y));
                        if (item != null) {
                            if (tip != null && !tip.isDisposed())
                                tip.dispose();
                            tip = new Shell(shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                            tip.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            FillLayout layout = new FillLayout();
                            layout.marginWidth = 2;
                            tip.setLayout(layout);
                            label = new Label(tip, SWT.NONE);
                            label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                            label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            label.setData(DATA_ITEM, item);
                            String tooltipStr = (String) item.getData(HOVER_DATA);
                            if (tooltipStr == null || tooltipStr.isEmpty()) {
                                tooltipStr = item.getText();
                            }
                            label.setText(tooltipStr);
                            label.addListener(SWT.MouseExit, labelListener);
                            label.addListener(SWT.MouseDown, labelListener);
                            Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                            Rectangle rect = item.getBounds(0);
                            Point pt = table.toDisplay(rect.x, rect.y);
                            tip.setBounds(pt.x, pt.y, size.x, size.y);
                            tip.setVisible(true);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };
        table.addListener(SWT.Dispose, tableListener);
        table.addListener(SWT.KeyDown, tableListener);
        table.addListener(SWT.MouseMove, tableListener);
        table.addListener(SWT.MouseHover, tableListener);
    }

    /**
     * Add hover help support to a tree.
     * Tree items that need hover help should use setData with HOVER_DATA
     * as the key and the tooltip string as the value.
     */
    public static void addHoverHelp(final Tree tree) {
        // Disable native tooltip
        tree.setToolTipText("");

        final Shell shell = tree.getShell();
        final Display display = shell.getDisplay();

        // Implement a "fake" tooltip
        final Listener labelListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                Label label = (Label) event.widget;
                Shell shell = label.getShell();
                switch (event.type) {
                    case SWT.MouseDown:
                        Event e = new Event();
                        e.item = (TreeItem) label.getData(DATA_ITEM);
                        // Assuming tree is single select, set the selection as if
                        // the mouse down event went through to the tree
                        tree.setSelection(new TreeItem[] { (TreeItem) e.item });
                        tree.notifyListeners(SWT.Selection, e);
                        shell.dispose();
                        tree.setFocus();
                        break;
                    case SWT.MouseExit:
                        shell.dispose();
                        break;
                    default:
                        break;
                }
            }
        };

        Listener treeListener = new Listener() {
            Shell tip = null;
            Label label = null;

            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                    case SWT.Dispose:
                        case SWT.KeyDown:
                        case SWT.MouseMove: {
                        if (tip == null)
                            break;
                        tip.dispose();
                        tip = null;
                        label = null;
                        break;
                    }
                    case SWT.MouseHover: {
                        TreeItem item = tree.getItem(new Point(event.x, event.y));
                        if (item != null) {
                            if (tip != null && !tip.isDisposed())
                                tip.dispose();
                            tip = new Shell(shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                            tip.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            FillLayout layout = new FillLayout();
                            layout.marginWidth = 2;
                            tip.setLayout(layout);
                            label = new Label(tip, SWT.NONE);
                            label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                            label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                            label.setData(DATA_ITEM, item);
                            String tooltipStr = (String) item.getData(HOVER_DATA);
                            if (tooltipStr == null || tooltipStr.isEmpty()) {
                                tooltipStr = item.getText();
                            }
                            label.setText(tooltipStr);
                            label.addListener(SWT.MouseExit, labelListener);
                            label.addListener(SWT.MouseDown, labelListener);
                            Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                            Rectangle rect = item.getBounds(0);
                            Point pt = tree.toDisplay(rect.x, rect.y);
                            tip.setBounds(pt.x, pt.y, size.x, size.y);
                            tip.setVisible(true);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };
        tree.addListener(SWT.Dispose, treeListener);
        tree.addListener(SWT.KeyDown, treeListener);
        tree.addListener(SWT.MouseMove, treeListener);
        tree.addListener(SWT.MouseHover, treeListener);
    }

}

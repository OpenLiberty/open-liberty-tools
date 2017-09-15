/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IProduct.Type;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.ISite;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;

public class AddonsComposite extends AbstractDownloadComposite {
    protected static final String PAGE_NAME = "add-ons";
    protected Link configRepoLink;
    protected ScrolledComposite scrollComp;
    protected Button addArchive;
    protected Label summaryLabel;
    protected Text summaryValueLabel;
    protected Label downloadsTotalLabel;
    protected Text downloadsValueLabel;
    protected Text filterText;
    protected String filter;
    protected Font boldFont;
    protected Color selEdgeColor;
    protected Color selFillColor;
    protected List<IProduct> allApplicableAddOnList = new ArrayList<IProduct>();
    protected IRuntimeInfo core = null;
    protected FeatureHandler featureHandler = new FeatureHandler();
    protected List<IProduct.Type> typeFilterList = new ArrayList<IProduct.Type>();
    protected List<IProduct.Type> typeFilterMenuList = new ArrayList<IProduct.Type>();
    protected boolean isShowSelectedOnly = false;
    protected boolean isHideConflictingProducts = false;
    protected boolean isHideInstalledProducts = true;
    protected boolean updating = false;
    protected ISite selectedSite = null;
    protected HashMap<ISite, PasswordAuthentication> siteAuthentication;
    protected Map<IProduct, PasswordAuthentication> productAuthentication = new HashMap<IProduct, PasswordAuthentication>();
    protected Label barLabel;
    protected ProgressBar bar;
    protected final AtomicInteger licenseJobCounter = new AtomicInteger();
    protected final Map<IProduct, Boolean> licenseStatusMap = new ConcurrentHashMap<IProduct, Boolean>();
    protected final List<IProduct> licenseRemoveList = new ArrayList<IProduct>();
    private LicenseThread licenseThread;
    protected Listener scrollCompositeListener;
    Set<IProduct> implicitProducts = new HashSet<IProduct>();

    private Thread timerThread = null;

    static class AddOnLayout extends Layout {
        private static final int BORDER = 5;
        private static final int IMG = 48;
        private static final int GAP = 3;
        private static final int V_SPACE = 3;

        protected int colWidth = -1;

        protected Map<Control, Rectangle> map = new HashMap<Control, Rectangle>();

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
            return new Point(composite.getClientArea().width, layoutImpl(composite, wHint, flushCache, false));
        }

        @Override
        protected void layout(Composite composite, boolean flushCache) {
            layoutImpl(composite, 0, flushCache, true);
        }

        protected Rectangle getSelection(Control c) {
            return map.get(c);
        }

        protected int layoutImpl(Composite composite, int wHint, boolean flushCache, boolean apply) {
            Rectangle r = composite.getClientArea();
            int areaW = r.width > 0 ? r.width : wHint;
            int i = 0;
            int x = r.x + BORDER;
            int y = r.y + BORDER;
            int w = 0;
            if (areaW == 0)
                w = composite.getParent().getBounds().width - composite.getParent().getBorderWidth() * 2;
            else
                w = areaW - BORDER * 2;
            w = Math.max(100, w);

            if (apply)
                map.clear();

            // children are: image, name, description, size, button
            Control[] children = composite.getChildren();
            if (children.length > 4 && colWidth < 0) {
                Button b = (Button) children[4];
                String s = b.getText();
                b.setText(Messages.wizInstallAddonRemove);
                int ww = b.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache).x;
                b.setText(Messages.wizInstallAddonInstall);
                ww = Math.max(ww, b.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache).x);
                b.setText(s);

                colWidth = ww + 8;
            }
            int colWid = Math.max(50, colWidth);

            while (i < children.length) {
                if (i > 0)
                    y += V_SPACE;

                Rectangle br = new Rectangle(x, y, w, 5);

                // image
                if (apply)
                    children[i].setBounds(x, y, IMG, IMG);

                // label
                Point p = children[i + 1].computeSize(w - IMG - GAP, SWT.DEFAULT, flushCache);
                if (apply)
                    children[i + 1].setBounds(x + IMG + GAP, y, w - IMG - GAP, p.y);

                y += p.y + GAP;

                // Size for individual items has been removed as part of defect 213298: Misleading feature sizes
                // May add it back if individual selections have more accurate sizes in the future.
//                p = children[i + 3].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
//                if (apply)
//                    children[i + 3].setBounds(x + w - p.x - 2, y, p.x, p.y);

                int yy = y;

                p = children[i + 4].computeSize(colWid, SWT.DEFAULT, flushCache);
                if (apply)
                    children[i + 4].setBounds(x + w - colWid, y, colWid, p.y);
                yy += p.y + GAP;

                // description
                p = children[i + 2].computeSize(w - IMG - GAP * 2 - colWid, SWT.DEFAULT, flushCache);
                if (apply)
                    children[i + 2].setBounds(x + IMG + GAP, y, w - IMG - GAP * 2 - colWid, p.y);

                y = Math.max(y + p.y, yy);

                if (apply) {
                    br.x -= 3;
                    br.y -= 3;
                    br.width += 6;
                    br.height = y - br.y + 4;
                    map.put(children[i], br);
                }
                y += BORDER;
                i += 5;
            }
            return y - r.y;
        }
    }

    public AddonsComposite(Composite parent, Map<String, Object> map, IContainer container, IMessageHandler handler) {
        super(parent, map, container, handler);
        container.setTitle(Messages.wizInstallAddonTitle);
        container.setDescription(Messages.wizInstallAddonDescription);
        init();
        createControl();
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        if (licenseThread != null) {
            licenseThread.done();
            licenseThread = null;
        }
    }

    @Override
    public void enter() {
        updateAddons();
        updateSummary();
        licenseThread = new LicenseThread();
        licenseThread.setDaemon(true);
        licenseThread.start();
        validate();
    }

    @Override
    public void exit() {
        @SuppressWarnings("unchecked")
        List<IProduct> selectedList = (List<IProduct>) map.get(SELECTED_DOWNLOADERS);
        @SuppressWarnings("unchecked")
        List<PasswordAuthentication> authList = (List<PasswordAuthentication>) map.get(PRODUCT_AUTHENTICATION);

        if (selectedList == null) {
            selectedList = new ArrayList<IProduct>();
            map.put(SELECTED_DOWNLOADERS, selectedList);
        } else {
            selectedList.clear();
        }

        if (authList == null) {
            authList = new ArrayList<PasswordAuthentication>();
            map.put(PRODUCT_AUTHENTICATION, authList);
        } else {
            authList.clear();
        }

        IProduct coreManager = (IProduct) map.get(SELECTED_CORE_MANAGER);
        if (coreManager != null) {
            selectedList.add(coreManager);
            authList.add(productAuthentication.get(coreManager));
        }

        @SuppressWarnings("unchecked")
        List<IProduct> selectedAddOn = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (!selectedAddOn.isEmpty()) {
            sortAddonList(selectedAddOn);
            selectedList.addAll(selectedAddOn);
            for (IProduct p : selectedAddOn) {
                authList.add(productAuthentication.get(p));
            }
        }

        if (!licenseRemoveList.isEmpty()) {
            Map<IProduct, License> licenseMap = getLicenseMap(false);
            for (IProduct p : licenseRemoveList) {
                if (licenseMap != null && licenseMap.containsKey(p))
                    licenseMap.remove(p);

                if (licenseStatusMap.containsKey(p))
                    licenseStatusMap.remove(p);
            }
            licenseRemoveList.clear();
        }

        if (licenseThread != null) {
            licenseThread.done();
            licenseThread = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void init() {
        siteAuthentication = (HashMap<ISite, PasswordAuthentication>) map.get(SITE_AUTHENTICATION);
        if (siteAuthentication == null) {
            siteAuthentication = new HashMap<ISite, PasswordAuthentication>();
            map.put(SITE_AUTHENTICATION, siteAuthentication);
        }
        initializeTypeFilterList();
        scrollCompositeListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (scrollComp == null || scrollComp.isDisposed())
                    return;

                if (event.type == SWT.MouseDown) {
                    Button b = getInstallButton(event);
                    if (b != null)
                        b.setFocus();
                    else
                        scrollComp.setFocus();
                } else if (event.type == SWT.KeyDown) {
                    if (event.keyCode == SWT.PAGE_DOWN || event.keyCode == SWT.PAGE_UP) {
                        ScrollBar vBar = scrollComp.getVerticalBar();
                        Rectangle r = scrollComp.getClientArea();
                        int selection = (event.keyCode == SWT.PAGE_DOWN) ? vBar.getSelection() + r.height : vBar.getSelection() - r.height;
                        if (selection < vBar.getMaximum() && selection >= 0) {
                            vBar.setSelection(selection);
                            Composite comp = (Composite) scrollComp.getContent();
                            Point location = comp.getLocation();
                            comp.setLocation(location.x, -vBar.getSelection());
                        }
                    }
                }
            }
        };
    }

    protected void initializeTypeFilterList() {
        Object presetTypes = map.get(TYPE_FILTER_PRESET);
        // If there are no filter presets then just exclude the unsupported types
        if (presetTypes == null) {
            for (IProduct.Type type : IProduct.Type.values()) {
                if (type != IProduct.Type.INSTALL && type != IProduct.Type.IFIX
                    && type != IProduct.Type.CONFIG_SNIPPET && type != IProduct.Type.UNKNOWN) {
                    typeFilterList.add(type);
                    typeFilterMenuList.add(type);
                }
            }
        } else {
            // When type filter presets are set only include those types
            @SuppressWarnings("unchecked")
            List<IProduct.Type> types = (List<Type>) presetTypes;
            for (IProduct.Type type : IProduct.Type.values()) {
                if (types.contains(type)) {
                    typeFilterList.add(type);
                    typeFilterMenuList.add(type);
                }
            }
        }
    }

    protected void createControl() {
        final Shell shell = getShell();

        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        layout.numColumns = 3;
        setLayout(layout);

        Composite filterComp = new Composite(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        filterComp.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.horizontalSpan = 3;
        data.horizontalIndent = 0;
        data.verticalIndent = 0;
        filterComp.setLayoutData(data);

        final Button filterButton = new Button(filterComp, SWT.PUSH | SWT.RIGHT_TO_LEFT);
        filterButton.setText(Messages.wizSearchFilterLabel);
        filterButton.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));
        data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        filterButton.setLayoutData(data);

        filterText = new Text(filterComp, SWT.SEARCH);
        filterText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        filterText.setText(Messages.wizDefaultFilterText);

        filterButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final Menu menu = new Menu(shell, SWT.POP_UP);
                fillFilterMenu(menu);
                displayDropdownMenu(filterButton, menu, true);
                menu.dispose();
            }
        });

        configRepoLink = new Link(this, SWT.NONE);
        configRepoLink.setText("<a>" + Messages.wizConfigRepoLabel + "</a>");
        configRepoLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false, 3, 1));

        configRepoLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RepositoriesConfigurationDialog d = new RepositoriesConfigurationDialog(getShell(), map);
                if (d.open() == Window.OK && d.isRefreshRequired()) {
                    refreshAddonTable();
                    updateSummary();
                    validate();
                }
            }
        });

        scrollComp = new ScrolledComposite(this, SWT.BORDER | SWT.V_SCROLL);
        data = new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1);
        data.heightHint = 350;
        scrollComp.setLayoutData(data);
        scrollComp.setAlwaysShowScrollBars(true);
        scrollComp.setBackground(scrollComp.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        int lineHeight = filterText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        scrollComp.getVerticalBar().setPageIncrement(lineHeight * 3);
        scrollComp.getVerticalBar().setIncrement(lineHeight);

        DropTarget target = new DropTarget(scrollComp, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        target.setTransfer(new Transfer[] { fileTransfer });

        target.addDropListener(new DropTargetAdapter() {
            @Override
            public void drop(DropTargetEvent event) {
                if (!fileTransfer.isSupportedType(event.currentDataType))
                    return;

                handleDrop((String[]) event.data, filterText);
            }
        });

        scrollComp.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent event) {
                Control c = scrollComp.getContent();
                if (c == null)
                    return;
                Rectangle r = scrollComp.getClientArea();
                r.height = c.computeSize(r.width, SWT.DEFAULT).y;
                c.setBounds(r);
            }
        });

        Font font = getFont();
        if (boldFont == null) {
            Display display = getShell().getDisplay();
            FontData[] fontData = font.getFontData();
            fontData[0].setStyle(SWT.BOLD);
            fontData[0].setHeight(fontData[0].getHeight());
            boldFont = new Font(display, fontData);

            Color c1 = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
            Color c2 = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
            selEdgeColor = new Color(display, (c1.getRed() + c2.getRed() * 3) / 4, (c1.getGreen() + c2.getGreen() * 3) / 4, (c1.getBlue() + c2.getBlue() * 3) / 4);
            selFillColor = new Color(display, (c1.getRed() + c2.getRed() * 8) / 9, (c1.getGreen() + c2.getGreen() * 8) / 9, (c1.getBlue() + c2.getBlue() * 8) / 9);
        }

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                if (boldFont != null)
                    boldFont.dispose();
                if (selEdgeColor != null)
                    selEdgeColor.dispose();
                if (selFillColor != null)
                    selFillColor.dispose();
            }
        });

        filterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (updating)
                    return;

                String text = filterText.getText();
                if (Messages.wizDefaultFilterText.equals(text))
                    filter = null;
                else
                    filter = filterText.getText();
                filterUpdate();
            }
        });

        filterText.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                if (filterText.getText().isEmpty()) {
                    updating = true;
                    filterText.setText(Messages.wizDefaultFilterText);
                    updating = false;
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (Messages.wizDefaultFilterText.equals(filterText.getText())) {
                    updating = true;
                    filterText.setText("");
                    updating = false;
                }
            }
        });

        addArchive = new Button(this, SWT.NONE);
        addArchive.setText(Messages.wizInstallAddonAddArchive);
        addArchive.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));
        addArchive.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                NewAddonDialog dialog = new NewAddonDialog(getShell(), featureHandler, map);
                if (dialog.open() == IStatus.OK) {
                    String archiveName = dialog.getArchive();
                    if (archiveName != null) {
                        addArchiveAddOn(archiveName, filterText);
                    }
                }
            }
        });

        Composite summaryComp = new Composite(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        summaryComp.setLayout(layout);
        data = new GridData(GridData.END, GridData.FILL, true, true, 1, 1);
        summaryComp.setLayoutData(data);

        summaryLabel = new Label(summaryComp, SWT.NONE);
        summaryLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false, 1, 1));
        summaryLabel.setText(Messages.wizInstallAddonSummary);

        summaryValueLabel = new Text(summaryComp, SWT.READ_ONLY);
        summaryValueLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 1, 1));
        summaryValueLabel.setForeground(summaryComp.getForeground());
        summaryValueLabel.setBackground(summaryComp.getBackground());

        downloadsTotalLabel = new Label(summaryComp, SWT.NONE);
        downloadsTotalLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false, 1, 1));
        downloadsTotalLabel.setText(Messages.wizInstallDownloadSummary);

        downloadsValueLabel = new Text(summaryComp, SWT.READ_ONLY);
        downloadsValueLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 1, 1));
        downloadsValueLabel.setForeground(summaryComp.getForeground());
        downloadsValueLabel.setBackground(summaryComp.getBackground());

        barLabel = new Label(this, SWT.NONE);
        barLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
        barLabel.setText(Messages.taskDownloadLicense);

        bar = new ProgressBar(this, SWT.SMOOTH);
        bar.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        bar.setMaximum(200);

        Dialog.applyDialogFont(this);
        filterText.setFocus();
        updateSummary();
    }

    protected void filterUpdate() {
        boolean updateRunning = timerThread != null && timerThread.isAlive();

        if (!updateRunning) {
            // sleep for 500ms after each key press to allow user to type without updating the
            // entire dialog before they're done typing
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Starting timer thread");
            startTimerThread();
        } else {
            timerThread.interrupt();
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Interrupted and restarted timer thread");
            startTimerThread();
        }

    }

    private void startTimerThread() {
        timerThread = new Thread() {
            @Override
            public void run() {
                try {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Addon timer started for 500ms");
                    sleep(500);
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Updating addon table");
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            updateAddonTable();
                        }
                    });
                } catch (InterruptedException e) {
                    // thread was interrupted
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Addon timer thread was interrupted");
                }
            }
        };
        timerThread.start();
    }

    protected void handleDrop(String[] files, Text filterText) {
        if (files == null)
            return;

        for (String archiveName : files) {
            String error = NewAddonDialog.validateArchive(archiveName, featureHandler, map);
            if (error != null)
                MessageDialog.openError(getShell(), Messages.title, error);
            else
                addArchiveAddOn(archiveName, filterText);
        }
    }

    protected void displayDropdownMenu(Control anchor, Menu menu, boolean subtractWidth) {
        Point size = anchor.getSize();
        Point point = anchor.toDisplay(0, size.y);
        menu.setLocation(point.x - (subtractWidth ? size.x : 0), point.y);
        menu.setVisible(true);

        while (!menu.isDisposed() && menu.isVisible()) {
            Display display = menu.getShell().getDisplay();
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    protected void fillFilterMenu(final Menu menu) {
        final SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                MenuItem item = (MenuItem) event.getSource();
                if (item.getSelection())
                    typeFilterList.add((IProduct.Type) item.getData());
                else {
                    typeFilterList.remove(item.getData());
                }
                updateAddonTable();
            }
        };

        MenuItem item;
        for (IProduct.Type type : typeFilterMenuList) {
            item = new MenuItem(menu, SWT.CHECK);
            item.setText(type.toString());
            item.setData(type);
            item.setSelection(typeFilterList.contains(type));
            item.addSelectionListener(listener);

        }

        item = new MenuItem(menu, SWT.SEPARATOR);
        final MenuItem selectedItem = new MenuItem(menu, SWT.CHECK);
        selectedItem.setText(Messages.wizSelectedFilterLabel);
        selectedItem.setSelection(isShowSelectedOnly);
        selectedItem.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                isShowSelectedOnly = selectedItem.getSelection();
                updateAddonTable();
            }
        });

        final MenuItem hideDuplicatesItem = new MenuItem(menu, SWT.CHECK);
        hideDuplicatesItem.setText(Messages.wizConflictFilterLabel);
        hideDuplicatesItem.setSelection(isHideConflictingProducts);
        hideDuplicatesItem.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                isHideConflictingProducts = hideDuplicatesItem.getSelection();
                updateAddonTable();
            }
        });

        final MenuItem hideInstalledItem = new MenuItem(menu, SWT.CHECK);
        hideInstalledItem.setText(Messages.wizInstalledFilterLabel);
        hideInstalledItem.setSelection(isHideInstalledProducts);
        hideInstalledItem.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                isHideInstalledProducts = hideInstalledItem.getSelection();
                updateAddonTable();
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void addArchiveAddOn(String archiveName, Text filterText) {
        List<String> archives = (List<String>) map.get(ARCHIVES);
        if (archives == null) {
            archives = new ArrayList<String>();
            map.put(ARCHIVES, archives);
        }
        archives.add(archiveName);
        List<IProduct> addOnList = (List<IProduct>) map.get(SELECTED_ADDONS);
        LocalProduct product = LocalProduct.create(archiveName);
        addOnList.add(product);
        allApplicableAddOnList.add(product);
        downloadLicense(product);

        // add provide feature list, if any
        featureHandler.addFeatures(product.getProvideFeature());

        if (product.getType() == IProduct.Type.FEATURE)
            featureHandler.addFeatures(FeatureResolver.getAllRequireFeature(product, allApplicableAddOnList));

        filterText.setText("");
        updateAddonTable();
        updateSummary();
        validate();

        // if necessary, scroll to the bottom of the list where the new item is
        Rectangle r = scrollComp.getClientArea();
        int h = scrollComp.getContent().computeSize(scrollComp.getClientArea().width, SWT.DEFAULT).y;
        if (h > r.height)
            scrollComp.setOrigin(0, h - r.height);
    }

    protected boolean isCoreRuntimeExpected() {
        return true;
    }

    protected void sortAddonList(List<IProduct> addonList) {
        // Revisit: For now we will sort the add-on list based on the type
        //          dependency, i.e. feature before sample (we won't allow
        //          installation of ifixes). Once we get pre-req information
        //          for features, we should use that information to sort
        //          within features - RTC task 119470.
        Collections.sort(addonList, new Comparator<IProduct>() {
            @Override
            public int compare(IProduct p1, IProduct p2) {
                int t1 = getDisplayType(p1).ordinal();
                int t2 = getDisplayType(p2).ordinal();

                if (t1 == t2)
                    return 0;

                return (t1 < t2) ? -1 : 1;
            }
        });
    }

    protected void validate() {
        @SuppressWarnings("unchecked")
        List<IProduct> addOnList = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (addOnList == null || addOnList.isEmpty()) {
            setMessage(null, (isCoreRuntimeExpected() && map.get(SELECTED_CORE_MANAGER) != null) ? IMessageProvider.NONE : IMessageProvider.ERROR);
            return;
        }

        // We have some license download jobs running, so we cannot complete
        // the page
        if (licenseJobCounter.get() > 0) {
            setMessage(null, IMessageProvider.ERROR);
            return;
        }

        // check all licenses were downloaded OK
        StringBuilder sb = new StringBuilder();
        for (IProduct p : addOnList) {
            if (!Boolean.TRUE.equals(this.licenseStatusMap.get(p))) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(p.getName());
            }
        }

        if (sb.length() > 0) {
            setMessage(NLS.bind(Messages.wizLicenseError, sb.toString()), IMessageProvider.ERROR);
            return;
        }

        setMessage(null, IMessageProvider.NONE);
    }

    protected boolean isAuthenticationRequired(IProduct product) {
        // TODO: use license type when support is added by runtime
        // Massive now accepts unauthenticated download
        //if (product instanceof MassiveProduct)
        //    return true;

        return false;
    }

    protected PasswordAuthentication getAuthentication(IProduct product) {
        PasswordAuthentication authentication = productAuthentication.get(product);
        if (authentication == null) {
            authentication = getAuthentication(getSite(product));
            productAuthentication.put(product, authentication);
        }
        return authentication;
    }

    protected PasswordAuthentication getAuthentication(ISite site) {
        PasswordAuthentication authentication = siteAuthentication.get(site);

        if (authentication == null || authentication.getUserName().isEmpty() || authentication.getPassword().length == 0) {
            PasswordDialog dialog = new PasswordDialog(getShell(), NLS.bind(Messages.loginPasswordDialogMessage, site.getName()), authentication);
            if (dialog.open() == Window.CANCEL) {
                return null;
            }

            authentication = new PasswordAuthentication(dialog.getUser(), dialog.getPassword().toCharArray());
            siteAuthentication.put(site, authentication);
        }

        return authentication;
    }

    private ISite getSite(IProduct p) {
        // Revisit: multi-site support
        return selectedSite;
    }

    protected void updateAddons() {
        IRuntimeInfo newCore = (IRuntimeInfo) map.get(RUNTIME_CORE);
        @SuppressWarnings("unchecked")
        List<IProduct> selectedAddOns = (List<IProduct>) map.get(SELECTED_ADDONS);

        if (selectedAddOns == null) {
            selectedAddOns = new ArrayList<IProduct>(4);
            map.put(SELECTED_ADDONS, selectedAddOns);
        }

        selectedSite = SiteHelper.getDefaultAddOnSite();

        // same runtime core
        if (core == newCore) {
            updateAddonTable();
            return;
        }

        // remove previously selected add-ons
        if (!selectedAddOns.isEmpty()) {
            removeSelectedAddOns(selectedAddOns);
        }

        core = newCore;
        licenseStatusMap.clear();
        productAuthentication.clear();

        // reset feature handler
        featureHandler.reset();

        // add features to be installed from a new runtime
        IProduct coreManager = (IProduct) map.get(SELECTED_CORE_MANAGER);
        if (coreManager != null)
            featureHandler.addFeatures(coreManager.getProvideFeature());

        // get applicable add-ons
        refreshAddonTable();
    }

    protected void refreshAddonTable() {
        configRepoLink.setEnabled(false);
        configRepoLink.setVisible(core.isOnPremiseSupported());
        filterText.setEnabled(false);

        Control content = scrollComp.getContent();
        if (content != null)
            content.dispose();

        Composite labelComp = new Composite(scrollComp, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.verticalSpacing = 12;
        labelComp.setLayout(layout);
        labelComp.setBackground(scrollComp.getBackground());

        Label descriptionLabel = new Label(labelComp, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
        descriptionLabel.setBackground(scrollComp.getBackground());
        descriptionLabel.setText(Messages.wizDownloadConnecting);
        scrollComp.setContent(labelComp);
        labelComp.setSize(labelComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        final ISite[] availableSites = (core.isOnPremiseSupported()) ? SiteHelper.getAvailableSites() : new ISite[] { SiteHelper.getDefaultAddOnSite() };

        allApplicableAddOnList.clear();

        final ArrayList<SiteThread> siteThreads = new ArrayList<SiteThread>(availableSites.length);
        final Map<ISite, List<IProduct>> siteProductMap = new ConcurrentHashMap<ISite, List<IProduct>>();

        for (int i = 0; i < availableSites.length; ++i) {
            if (isSiteSupported(availableSites[i], core)) {
                SiteThread siteThread = new SiteThread(availableSites[i], siteProductMap);
                siteThread.setDaemon(true);
                siteThread.start();
                siteThreads.add(siteThread);
            }
        }

        final Thread finalizeThread = new Thread("Checking available repositories") {
            @Override
            public void run() {
                try {
                    for (SiteThread thread : siteThreads) {
                        thread.join();
                    }
                } catch (InterruptedException e) {
                    // ignore
                }

                if (availableSites.length > 0) {
                    List<IProduct> productList = siteProductMap.get(availableSites[0]);
                    if (productList != null)
                        allApplicableAddOnList.addAll(productList);
                    for (int i = 1; i < availableSites.length; ++i) {
                        productList = siteProductMap.get(availableSites[i]);
                        if (productList != null) {
                            for (IProduct p : productList) {
                                if (!allApplicableAddOnList.contains(p))
                                    allApplicableAddOnList.add(p);
                            }
                        }
                    }
                }

                // There is no guarantee that products will be sorted in the list
                // by type.
                Collections.sort(allApplicableAddOnList, new Comparator<IProduct>() {
                    @Override
                    public int compare(IProduct p1, IProduct p2) {
                        int t1 = getDisplayType(p1).ordinal();
                        int t2 = getDisplayType(p2).ordinal();

                        if (t1 == t2)
                            return Collator.getInstance().compare(p1.getName(), p2.getName());

                        return (t1 < t2) ? -1 : 1;
                    }
                });

                @SuppressWarnings("unchecked")
                // Update selected list with matched components from the full
                // list of components.
                final List<IProduct> selectedList = (List<IProduct>) map.get(SELECTED_ADDONS);
                if (selectedList != null) {
                    List<IProduct> removeList = new ArrayList<IProduct>(selectedList.size());
                    for (int i = 0; i < selectedList.size(); ++i) {
                        IProduct p = selectedList.get(i);
                        if (p instanceof LocalProduct) {
                            allApplicableAddOnList.add(p);
                        } else {
                            int index = allApplicableAddOnList.indexOf(p);
                            if (index == -1)
                                removeList.add(p);
                            else {
                                // REVISIT: should we only maintain selection
                                //          if same site for both products?
                                selectedList.set(i, allApplicableAddOnList.get(index));
                                Map<IProduct, License> licenseMap = getLicenseMap(false);
                                if (licenseMap != null)
                                    licenseMap.remove(p);
                                licenseStatusMap.put(p, Boolean.FALSE);
                                downloadLicense(selectedList.get(i));
                            }
                        }
                    }

                    for (IProduct p : removeList) {
                        if (!licenseRemoveList.contains(p))
                            licenseRemoveList.add(p);
                        selectedList.remove(p);
                        featureHandler.removeFeatures(p.getProvideFeature());
                        if (p.getType() == IProduct.Type.FEATURE)
                            featureHandler.removeFeatures(FeatureResolver.getAllRequireFeature(p, allApplicableAddOnList));
                    }
                }

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (scrollComp.isDisposed())
                            return;

                        updateAddonTable();
                        updateSummary();
                        if (!configRepoLink.isDisposed()) {
                            configRepoLink.setVisible(core.isOnPremiseSupported());
                            configRepoLink.setEnabled(true);
                        }
                        filterText.setEnabled(true);
                    }
                });
            }
        };

        finalizeThread.setDaemon(true);
        finalizeThread.start();
    }

    /**
     * @param site
     * @param core
     * @return true if the site is supported, false otherwise
     */
    private boolean isSiteSupported(ISite site, IRuntimeInfo core) {
        return SiteHelper.isRepoSupported(site, core);
    }

    protected void updateAddonTable() {
        Control content = scrollComp.getContent();
        if (content != null)
            content.dispose();

        implicitProducts.clear();

        if (allApplicableAddOnList.isEmpty()) {
            Composite labelComp = new Composite(scrollComp, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.horizontalSpacing = 8;
            layout.verticalSpacing = 12;
            labelComp.setLayout(layout);
            labelComp.setBackground(scrollComp.getBackground());

            Label descriptionLabel = new Label(labelComp, SWT.NONE);
            descriptionLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
            descriptionLabel.setBackground(scrollComp.getBackground());
            descriptionLabel.setText(Messages.wizNoApplicableAddonMessage);
            scrollComp.setContent(labelComp);
            labelComp.setSize(labelComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            return;
        }

        // disable vertical and horizontal scroll bars
        // until we have updated the composite
        ScrollBar vBar = scrollComp.getVerticalBar();
        ScrollBar hBar = scrollComp.getHorizontalBar();

        if (vBar != null)
            vBar.setEnabled(false);

        if (hBar != null)
            hBar.setEnabled(false);

        final Composite comp = new Composite(scrollComp, SWT.NONE);
        comp.setBackground(scrollComp.getBackground());
        comp.setLayout(new AddOnLayout());

        comp.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event) {
                Control[] c = comp.getChildren();
                int i = 0;
                @SuppressWarnings("unchecked")
                List<IProduct> selected = (List<IProduct>) map.get(SELECTED_ADDONS);
                while (i < c.length) {
                    Object data = c[i].getData();
                    if (selected.contains(data)) {
                        Rectangle r = ((AddOnLayout) comp.getLayout()).getSelection(c[i]);
                        if (r == null)
                            return;

                        event.gc.setBackground(selFillColor);
                        event.gc.fillRoundRectangle(r.x, r.y, r.width, r.height, 7, 7);

                        event.gc.setForeground(selEdgeColor);
                        event.gc.drawRoundRectangle(r.x, r.y, r.width, r.height, 7, 7);
                    }
                    i += 5;
                }
            }
        });

        comp.addListener(SWT.MouseDown, scrollCompositeListener);

        @SuppressWarnings("unchecked")
        final List<IProduct> selected = (List<IProduct>) map.get(SELECTED_ADDONS);
        final Color gray = comp.getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
        final Color darkGray = comp.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Applicable Addons length: " + allApplicableAddOnList.size());
        for (IProduct product : allApplicableAddOnList) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Addon: " + product.getName());
            Color c = null;
            boolean isConflicting = false;
            boolean isInstalled = false;

            // filter out items by type
            if (isFilteredOut(product))
                continue;

            if (selected.contains(product))
                c = selFillColor;
            else {
                if (isShowSelectedOnly) {
                    continue;
                }

                // filter already installed archives
                if (isInstalled(product)) {
                    if (isHideInstalledProducts)
                        continue;
                    isInstalled = true;
                } else if (isConflicting(product)) {
                    if (isHideConflictingProducts)
                        continue;
                    isConflicting = true;
                }
                c = comp.getBackground();
            }

            if (filter == null || filter.isEmpty() || matches(product, filter)) {
                final Label imgLabel = new Label(comp, SWT.NONE);
                imgLabel.setBackground(c);
                imgLabel.setImage(getImageFor(product));
                imgLabel.setData(product);
                imgLabel.addListener(SWT.MouseDown, scrollCompositeListener);

                final Label nameLabel = new Label(comp, SWT.WRAP);
                nameLabel.setFont(boldFont);
                nameLabel.setBackground(c);
                nameLabel.setText(product.getName());
                nameLabel.addListener(SWT.MouseDown, scrollCompositeListener);

                final Label descLabel = new Label(comp, SWT.WRAP);
                descLabel.setBackground(c);
                String desc = product.getDescription();
                if (desc == null)
                    desc = "";
                descLabel.setText(desc);
                descLabel.addListener(SWT.MouseDown, scrollCompositeListener);

                /*
                 * Individual sizes have been removed, but may be added back in the future if
                 * the sizing includes dependencies
                 */
                final Label sizeLabel = new Label(comp, SWT.NONE);
                sizeLabel.setBackground(c);
//                long size = product.getSize();
//                if (size >= 0)
//                    sizeLabel.setText(DownloadHelper.getSize(size));
//                else
                sizeLabel.setText("");
                //sizeLabel.addListener(SWT.MouseDown, scrollCompositeListener);

                final Button install = new Button(comp, SWT.PUSH | SWT.FLAT);
                install.setBackground(c);
                if (isInstalled)
                    install.setText(Messages.wizInstallAddonInstalled);
                else if (isSelected(product))
                    install.setText(Messages.wizInstallAddonRemove);
                else
                    install.setText(Messages.wizInstallAddonInstall);
                install.addListener(SWT.KeyDown, scrollCompositeListener);
                install.setData(product);

                if (isConflicting) {
                    implicitProducts.add(product);
                    nameLabel.setForeground(gray);
                    descLabel.setForeground(gray);
                    sizeLabel.setForeground(gray);
                    install.setEnabled(false);
                    nameLabel.setToolTipText(Messages.wizConflictToolTipMessage);
                    descLabel.setToolTipText(Messages.wizConflictToolTipMessage);
                } else if (isInstalled) {
                    nameLabel.setForeground(darkGray);
                    descLabel.setForeground(darkGray);
                    sizeLabel.setForeground(darkGray);
                    install.setEnabled(false);
                    nameLabel.setToolTipText(Messages.wizInstalledToolTipMessage);
                    descLabel.setToolTipText(Messages.wizInstalledToolTipMessage);
                }

                final IProduct item2 = product;
                install.addFocusListener(new FocusListener() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        // no op
                    }

                    @Override
                    public void focusGained(FocusEvent e) {
                        // Scroll up/down if necessary
                        Rectangle r = scrollComp.getClientArea();
                        Point p = scrollComp.getOrigin();
                        Rectangle rc1 = getSelectionRectangle(item2);
                        Rectangle rc2 = install.getBounds();
                        if (rc1 != null && rc2 != null) {
                            if (rc2.y < p.y || rc2.y > p.y + r.height) {
                                scrollComp.setOrigin(0, rc1.y);
                            }
                        }
                    }
                });

                install.getAccessible().addAccessibleListener(new AccessibleAdapter() {
                    @Override
                    public void getName(AccessibleEvent e) {
                        e.result = nameLabel.getText() + " " + descLabel.getText() + " " + install.getText();
                    }
                });

                install.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        if (selected.contains(item2)) {
                            if (!licenseRemoveList.contains(item2))
                                licenseRemoveList.add(item2);
                            selected.remove(item2);
                            featureHandler.removeFeatures(item2.getProvideFeature());
                            if (item2.getType() == IProduct.Type.FEATURE)
                                featureHandler.removeFeatures(FeatureResolver.getAllRequireFeature(item2, allApplicableAddOnList));
                        } else {
                            // if a product requires authentication, make sure we ask for
                            // authentication if it's missing
                            if (!isAuthenticationRequired(item2) || getAuthentication(item2) != null) {
                                selected.add(item2);
                                downloadLicense(item2);
                                featureHandler.addFeatures(item2.getProvideFeature());
                                if (item2.getType() == IProduct.Type.FEATURE)
                                    featureHandler.addFeatures(FeatureResolver.getAllRequireFeature(item2, allApplicableAddOnList));

                                // remove previously selected feature assets if they
                                // are part of the extended archive or conflict with
                                // another feature
                                if ((item2.getType() == IProduct.Type.EXTENDED || item2.getType() == IProduct.Type.FEATURE) && selected.size() > 1) {
                                    List<IProduct> dupList = new ArrayList<IProduct>(selected.size() - 1);
                                    for (IProduct p : selected) {
                                        if (p.getType() == IProduct.Type.FEATURE) {
                                            List<String> featureList = p.getProvideFeature();
                                            if (featureHandler.isMultipleSelection(featureList.get(0))) {
                                                featureHandler.removeFeatures(featureList);
                                                featureHandler.removeFeatures(FeatureResolver.getAllRequireFeature(p, allApplicableAddOnList));
                                                dupList.add(p);
                                            }
                                        }
                                    }
                                    for (IProduct p : dupList) {
                                        @SuppressWarnings("unchecked")
                                        Map<IProduct, License> licenseMap = (Map<IProduct, License>) map.get(LICENSE);
                                        if (licenseMap != null) {
                                            licenseMap.remove(p);
                                        }
                                        selected.remove(p);
                                        if (!licenseRemoveList.contains(p))
                                            licenseRemoveList.add(p);
                                    }
                                }
                            }
                        }

                        // Since we filter the list of add-on to eliminate
                        // conflicts, we will loose the current scroll
                        // location. So, keep track of the selection so we can
                        // scroll back to the item after we update the list.
                        Point p = scrollComp.getOrigin();
                        Rectangle rc1 = getSelectionRectangle(item2);

                        updateAddonTable();
                        updateSummary();
                        validate();
                        map.remove(LICENSE_ACCEPT);

                        // Get the current selection position and adjust the
                        // scroll location accordingly.
                        Rectangle rc2 = getSelectionRectangle(item2);
                        if (rc2 != null && rc1 != null) {
                            // same horizontal position, so use previous origin
                            if (rc1.y == rc2.y) {
                                scrollComp.setOrigin(p);
                            } else {
                                int h = rc2.y + p.y - rc1.y;
                                if (h > 0)
                                    scrollComp.setOrigin(0, h);
                            }
                        }
                        Button currentInstallButton = getInstallButton(item2);
                        if (currentInstallButton != null)
                            currentInstallButton.setFocus();
                    }
                });
            }
        }

        Rectangle r = scrollComp.getClientArea();
        r.height = comp.computeSize(r.width, SWT.DEFAULT).y;
        comp.setBounds(r);

        scrollComp.setContent(comp);

        if (vBar != null)
            vBar.setEnabled(true);

        if (hBar != null)
            hBar.setEnabled(true);
    }

    private boolean isFilteredOut(IProduct product) {
        // if the filter list is empty then don't need to filter out item
        if (typeFilterList.isEmpty())
            return false;

        IProduct.Type type = getDisplayType(product);

        return !typeFilterList.contains(type);
    }

    private Image getImageFor(IProduct product) {
        try {
            IProduct.Type type = getDisplayType(product);
            if (type == IProduct.Type.SAMPLE)
                return Activator.getImage(Activator.IMG_ADD_ON_SAMPLE);
            else if (type == IProduct.Type.FEATURE)
                return Activator.getImage(Activator.IMG_ADD_ON_FEATURE);
            else if (type == IProduct.Type.CONFIG_SNIPPET)
                return Activator.getImage(Activator.IMG_ADD_ON_CONFIG_SNIPPET);
            else if (type == IProduct.Type.OPEN_SOURCE)
                return Activator.getImage(Activator.IMG_ADD_ON_OPEN_SOURCE);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Failed to get image for product " + product.getName());
        }

        return Activator.getImage(Activator.IMG_ADD_ON);
    }

    protected boolean matches(IProduct item, String filter) {
        List<String> features = item.getProvideFeature();
        boolean featureIDMatch = features != null && !features.isEmpty() && features.get(0).toLowerCase().contains(filter.toLowerCase());
        return featureIDMatch || item.getName().toLowerCase().contains(filter.toLowerCase()) ||
               item.getDescription().toLowerCase().contains(filter.toLowerCase());
    }

    protected Rectangle getSelectionRectangle(IProduct item) {
        Composite comp = (Composite) scrollComp.getContent();
        Control[] cos = comp.getChildren();
        for (Control co : cos) {
            if (co.getData() == item) {
                return ((AddOnLayout) comp.getLayout()).getSelection(co);
            }
        }

        return null;
    }

    protected Button getInstallButton(IProduct item) {
        Composite comp = (Composite) scrollComp.getContent();
        Control[] cos = comp.getChildren();
        for (Control co : cos) {
            if (co.getData() == item && co instanceof Button) {
                return (Button) co;
            }
        }

        return null;
    }

    protected Button getInstallButton(Event event) {
        Widget w = event.widget;
        if (w == null)
            return null;

        if (w instanceof Composite) {
            return getInstallButton(event.y);
        }

        Composite comp = (Composite) scrollComp.getContent();
        Control[] cos = comp.getChildren();
        for (int i = 0; i < cos.length; ++i) {
            if (cos[i] == w) {
                for (int j = i + 1; j < cos.length; ++j)
                    if (cos[j] instanceof Button)
                        return (Button) cos[j];
            }
        }

        return null;
    }

    private Button getInstallButton(int y) {
        Composite comp = (Composite) scrollComp.getContent();
        Control[] cos = comp.getChildren();
        for (Control co : cos) {
            if (co instanceof Button) {
                Rectangle c = getSelectionRectangle((IProduct) co.getData());
                if (y >= c.y && y <= c.y + c.height)
                    return (Button) co;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected void updateSummary() {
        int size = 0;
        List<IProduct> addOnList = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (addOnList != null) {
            size = addOnList.size();
        }

        String oldText = summaryValueLabel.getText();
        String newText = size + "";
        summaryValueLabel.setText(newText);

        // Update the layout if the length of the lablel's text has changed
        // (e.g. double digits instead of single digits or vice versa)
        if (!oldText.isEmpty() && newText.length() != oldText.length()) {
            summaryValueLabel.getParent().getParent().layout(true);
        }

        long downloadSize = 0;
        String oldDownloadTotal = downloadsValueLabel.getText();
        String newDownloadTotal = "0 KB";
        if (size == 0) {
            downloadsValueLabel.setText(newDownloadTotal);
        } else {
            downloadSize = getTotalDownloadSize();
            newDownloadTotal = DownloadHelper.getSize(downloadSize, NumberFormat.getIntegerInstance());
            downloadsValueLabel.setText(newDownloadTotal);
        }
        // Update the layout if the label's text has changed
        if (!oldDownloadTotal.isEmpty() && !newDownloadTotal.equals(oldDownloadTotal)) {
            downloadsValueLabel.getParent().getParent().layout(true);
        }
    }

    private long getTotalDownloadSize() {
        @SuppressWarnings("unchecked")
        List<IProduct> selected = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (selected == null)
            return 0;
        long downloadTotal = 0;
        for (IProduct p : selected) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Selected product: " + p.getName() + " Size: " + p.getSize());
            downloadTotal += p.getSize();
        }
        for (IProduct p : implicitProducts) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Implicitly selected product: " + p.getName() + " Size: " + p.getSize());
            downloadTotal += p.getSize();
        }
        return downloadTotal;
    }

    @SuppressWarnings("unchecked")
    protected boolean isSelected(IProduct manager) {
        if (manager == null)
            return false;

        List<IProduct> selected = (List<IProduct>) map.get(SELECTED_ADDONS);
        if (selected == null)
            return false;
        return selected.contains(manager);
    }

    private void removeSelectedAddOns(List<IProduct> addOnList) {
        @SuppressWarnings("unchecked")
        List<String> archives = (List<String>) map.get(ARCHIVES);
        for (IProduct addOn : addOnList) {
            // We have a mixture of local and remote archives, and we only
            // want to remove the full path of local archives.
            if (addOn instanceof LocalProduct) {
                archives.remove(addOn.getSource().getLocation());
            }
        }

        addOnList.clear();
        featureHandler.reset();
    }

    protected List<IProduct> getApplicableAddOns(ISite site) {
        return site.getApplicableProducts(core, new NullProgressMonitor());
    }

    private boolean isConflicting(IProduct p) {
        return featureHandler.containsFeatures(p.getProvideFeature());
    }

    protected boolean isInstalled(IProduct p) {
        if (p.isInstallOnlyFeature()) // install-only features are installed if their required features are installed
            return featureHandler.isInstalled(core, p.getRequireFeature());
        return featureHandler.isInstalled(core, p.getProvideFeature());
    }

    protected void downloadLicense(final IProduct p) {
        if (licenseRemoveList.contains(p))
            licenseRemoveList.remove(p);

        if (isLicenseExist(p)) {
            licenseStatusMap.put(p, Boolean.TRUE);
            return;
        }

        JobChangeAdapter listener = new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                try {
                    licenseJobCounter.decrementAndGet();
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            validate();
                        }
                    });
                } finally {
                    event.getJob().removeJobChangeListener(this);
                }
            }
        };
        Job job = new Job(Messages.taskDownloadLicense) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    License license = p.getLicense(monitor);
                    if (license != null) {
                        Map<IProduct, License> licenseMap = getLicenseMap(true);
                        licenseMap.put(p, license);
                    }
                    licenseStatusMap.put(p, Boolean.TRUE);
                } catch (IOException e) {
                    Trace.logError("Error getting license for " + p.getName(), e);
                }
                return Status.OK_STATUS;
            }

        };

        licenseStatusMap.put(p, Boolean.FALSE);
        licenseJobCounter.incrementAndGet();
        job.addJobChangeListener(listener);
        job.setPriority(Job.SHORT);
        job.schedule();
    }

    private boolean isLicenseExist(IProduct p) {
        Map<IProduct, License> licenseMap = getLicenseMap(false);
        return licenseMap == null ? false : licenseMap.containsKey(p);
    }

    protected Map<IProduct, License> getLicenseMap(boolean toCreate) {
        @SuppressWarnings("unchecked")
        Map<IProduct, License> licenseMap = (Map<IProduct, License>) map.get(LICENSE);
        if (licenseMap == null && toCreate) {
            licenseMap = AddonUtil.createLicenseMap();
            map.put(LICENSE, licenseMap);
        }

        return licenseMap;
    }

    private class LicenseThread extends Thread {
        private volatile boolean done = false;

        public LicenseThread() {
            // empty
        }

        @Override
        public void run() {
            boolean isResetRequired = false;
            final int[] i = new int[1];
            while (!done) {
                if (!isResetRequired) {
                    isResetRequired = true;
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!bar.isDisposed()) {
                                barLabel.setVisible(false);
                                bar.setVisible(false);
                            }
                        }
                    });
                }

                while (licenseJobCounter.get() > 0) {
                    if (isResetRequired) {
                        isResetRequired = false;
                        i[0] = 0;
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (!bar.isDisposed()) {
                                    barLabel.setVisible(true);
                                    bar.setVisible(true);
                                }
                            }
                        });
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Throwable th) {
                        // ignore
                    }
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (bar.isDisposed())
                                return;
                            if (++i[0] < bar.getMaximum())
                                bar.setSelection(i[0]);
                        }
                    });
                }
            }
        }

        public void done() {
            done = true;
        }
    }

    private class SiteThread extends Thread {

        private final ISite site;
        private final Map<ISite, List<IProduct>> siteProductMap;

        SiteThread(ISite site, Map<ISite, List<IProduct>> siteProductMap) {
            this.site = site;
            this.siteProductMap = siteProductMap;
        }

        @Override
        public void run() {
            siteProductMap.put(site, getApplicableAddOns(site));
        }
    }

    IProduct.Type getDisplayType(IProduct product) {
        // check if the product has a display type, if it does then use it instead of the actual type for filtering
        // display types allow products of one type to be displayed like another (eg. a feature that should display as an addon)
        IProduct.Type type = product.getType();
        try {
            if (product.getAttribute(IProduct.PROP_DISPLAY_TYPE) != null)
                type = IProduct.Type.valueOf(product.getAttribute(IProduct.PROP_DISPLAY_TYPE));
        } catch (Exception e) {
            Trace.logError("Error getting product type", e);
        }
        return type;
    }
}

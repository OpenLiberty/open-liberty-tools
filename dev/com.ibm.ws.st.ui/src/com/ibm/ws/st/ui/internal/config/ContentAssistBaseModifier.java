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

package com.ibm.ws.st.ui.internal.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Base class for adding content assist support to a control.
 */
public abstract class ContentAssistBaseModifier {

    private static final int MIN_ITEMS = 5;
    private static final int MAX_ITEMS = 8;
    private static final String PROPOSAL_KEY = "proposalKey";
    private static final int MIN_CONTENT_ASSIST_WIDTH = 200;

    protected final Control control;
    protected final IContentAssistProposalProvider proposalProvider;
    protected final char autoChar;
    protected Shell popupShell;
    protected Shell popupDetails;
    protected Table suggestionTable;
    protected boolean updating = false;
    protected Color gray;
    protected TableColumn tableColumn;

    protected ContentAssistBaseModifier(Control control, IContentAssistProposalProvider proposalProvider, char autoChar) {
        this.control = control;
        this.proposalProvider = proposalProvider;
        this.autoChar = autoChar;
    }

    protected void createControls() {
        // Shell for table of suggestions
        popupShell = new Shell(control.getShell(), SWT.ON_TOP);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        popupShell.setLayout(layout);

        // Shell for suggestion details
        popupDetails = new Shell(control.getShell(), SWT.ON_TOP);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        popupDetails.setLayout(layout);
        popupDetails.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        popupDetails.setForeground(control.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        // Suggestion table
        suggestionTable = new Table(popupShell, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL);
        suggestionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        suggestionTable.setHeaderVisible(false);

        Color bg = suggestionTable.getBackground();
        Color fg = suggestionTable.getForeground();
        gray = new Color(bg.getDevice(), (bg.getRed() + fg.getRed()) / 2, (bg.getGreen() + fg.getGreen()) / 2, (bg.getBlue() + fg.getBlue()) / 2);
        tableColumn = new TableColumn(suggestionTable, SWT.NONE);

        // Add listeners to the control
        addControlListeners();

        // Handle ESC and CR in the shell, otherwise it will
        // get disposed
        popupShell.addListener(SWT.Traverse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.detail) {
                    case SWT.TRAVERSE_ESCAPE:
                        handleESC(event);
                        event.detail = SWT.TRAVERSE_NONE;
                        break;
                    case SWT.TRAVERSE_RETURN:
                        handleCR(event);
                        event.detail = SWT.TRAVERSE_NONE;
                        break;
                    default:
                        break;
                }
            }
        });

        // Listen for selections in the suggestion table
        suggestionTable.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                updating = true;
                if (suggestionTable.getSelectionIndex() != -1) {
                    TableItem item = suggestionTable.getItem(suggestionTable.getSelectionIndex());
                    IContentAssistProposal proposal = (IContentAssistProposal) item.getData(PROPOSAL_KEY);
                    if (proposal.isEnabled()) {
                        setText(proposal.getText());
                        setSelection(proposal.getCursorPosition());
                        popupDetails.setVisible(false);
                        popupShell.setVisible(false);
                    }
                }
                updating = false;
            }

            @Override
            public void widgetSelected(SelectionEvent event) {
                showDetails();
            }
        });

        // Once the user has clicked in the table, it becomes the focus
        // so need to listen for keys
        suggestionTable.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.keyCode) {
                    case SWT.ARROW_DOWN:
                        handleArrowDown(event);
                        break;
                    case SWT.ARROW_UP:
                        handleArrowUp(event);
                        break;
                    default:
                        break;
                }
            }
        });

        suggestionTable.addListener(SWT.FocusOut, new FocusOutListener());
    }

    protected void fillSuggestionTable(String filter) {
        suggestionTable.removeAll();

        Point selection = getSelection();
        IContentAssistProposal[] proposals = proposalProvider.getProposals(filter, selection.x, selection.y);
        if (proposals == null)
            return;

        for (IContentAssistProposal proposal : proposals) {
            TableItem item = new TableItem(suggestionTable, SWT.NONE);
            item.setText(proposal.getLabel());
            item.setImage(proposal.getImage());
            item.setData(PROPOSAL_KEY, proposal);
            if (!proposal.isEnabled())
                item.setForeground(gray);
        }
    }

    protected void showSuggestions() {
        fillSuggestionTable(getText());
        if (suggestionTable.getItemCount() > 0) {
            // Position the popup under the text box
            Point size = control.getSize();
            Point location = control.toDisplay(0, size.y);
            popupShell.setLocation(location);
            int numItems = Math.max(MIN_ITEMS, Math.min(MAX_ITEMS, suggestionTable.getItemCount()));
            int height = suggestionTable.getItemHeight() * numItems + popupShell.getBorderWidth() * 2;
            int width = size.x > MIN_CONTENT_ASSIST_WIDTH ? size.x : MIN_CONTENT_ASSIST_WIDTH;
            popupShell.setSize(width, height);
            tableColumn.setWidth(suggestionTable.getClientArea().width);
            popupShell.setVisible(true);

            // Make the details popup the same size as the main popup
            size = popupShell.getSize();
            Display display = popupDetails.getDisplay();
            if ((popupShell.getStyle() & SWT.MIRRORED) != 0) {
                // If mirrored, put details on the left
                int x = location.x - size.x;
                if (x < 0) {
                    // No space so put it on the right
                    x = location.x + size.x;
                }
                location.x = x;
            } else {
                // If not mirrored, put details on the right
                int x = location.x + size.x;
                Rectangle bounds = display.getBounds();
                if ((x + size.x) > bounds.width) {
                    // See if it can go to the left of the suggestions
                    if (location.x > size.x) {
                        x = location.x - size.x;
                    }
                }
                location.x = x;
            }
            popupDetails.setLocation(location);
            popupDetails.setSize(size.x, size.y);
            suggestionTable.setSelection(0);
            showDetails();
        } else {
            popupDetails.setVisible(false);
            popupShell.setVisible(false);
        }
    }

    protected void showDetails() {
        if (suggestionTable.getSelectionIndex() != -1) {
            TableItem item = suggestionTable.getItem(suggestionTable.getSelectionIndex());
            IContentAssistProposal proposal = (IContentAssistProposal) item.getData(PROPOSAL_KEY);
            if (proposal.hasDetails()) {
                // Dispose any existing children first
                Control[] children = popupDetails.getChildren();
                for (Control control : children) {
                    control.dispose();
                }
                // Ask the proposal to fill in the details
                proposal.createDetails(popupDetails);
                popupDetails.layout(true);
                popupDetails.setVisible(true);
                return;
            }
        }
        popupDetails.setVisible(false);
    }

    protected void handleArrowDown(Event event) {
        int startIndex = suggestionTable.getSelectionIndex();
        int index = (startIndex + 1) % suggestionTable.getItemCount();
        if (startIndex == -1)
            startIndex = suggestionTable.getItemCount() - 1;
        IContentAssistProposal proposal = (IContentAssistProposal) suggestionTable.getItem(index).getData(PROPOSAL_KEY);
        while (!proposal.isEnabled() && index != startIndex) {
            index = (index + 1) % suggestionTable.getItemCount();
            proposal = (IContentAssistProposal) suggestionTable.getItem(index).getData(PROPOSAL_KEY);
        }
        if (proposal.isEnabled()) {
            suggestionTable.setSelection(index);
            suggestionTable.setFocus();
            showDetails();
        }
        event.doit = false;
    }

    protected void handleArrowUp(Event event) {
        int startIndex = suggestionTable.getSelectionIndex();
        int index = startIndex;
        index = --index < 0 ? suggestionTable.getItemCount() - 1 : index;
        if (startIndex == -1)
            startIndex = 0;
        IContentAssistProposal proposal = (IContentAssistProposal) suggestionTable.getItem(index).getData(PROPOSAL_KEY);
        while (!proposal.isEnabled() && index != startIndex) {
            index = --index < 0 ? suggestionTable.getItemCount() - 1 : index;
            proposal = (IContentAssistProposal) suggestionTable.getItem(index).getData(PROPOSAL_KEY);
        }
        if (proposal.isEnabled()) {
            suggestionTable.setSelection(index);
            suggestionTable.setFocus();
            showDetails();
        }
        event.doit = false;
    }

    protected void handleCR(Event event) {
        updating = true;
        if (suggestionTable.getSelectionIndex() != -1) {
            TableItem item = suggestionTable.getItem(suggestionTable.getSelectionIndex());
            IContentAssistProposal proposal = (IContentAssistProposal) item.getData(PROPOSAL_KEY);
            if (proposal.isEnabled()) {
                setText(proposal.getText());
                setSelection(proposal.getCursorPosition());
                popupDetails.setVisible(false);
                popupShell.setVisible(false);
            }
        }
        updating = false;
        event.doit = false;
    }

    protected void handleESC(Event event) {
        popupDetails.setVisible(false);
        popupShell.setVisible(false);
        event.doit = false;
    }

    protected abstract String getText();

    protected abstract void setText(String text);

    protected abstract Point getSelection();

    protected abstract void setSelection(int start);

    protected abstract void addControlListeners();

    // Update the suggestions in the table as the user types
    protected class TextModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            if (updating)
                return;
            if (getText().isEmpty()) {
                popupDetails.setVisible(false);
                popupShell.setVisible(false);
            } else if (popupShell.isVisible()) {
                showSuggestions();
            }
        }
    }

    // The control is the focus until user clicks in the table
    // so listen for keys.
    protected class KeyListener implements Listener {
        @Override
        public void handleEvent(Event event) {
            if (!popupShell.isVisible()) {
                if ((event.stateMask & SWT.CTRL) != 0 && event.keyCode == SWT.SPACE) {
                    showSuggestions();
                    event.doit = false;
                } else if (event.character == autoChar) {
                    showSuggestions();
                }
                return;
            }

            switch (event.keyCode) {
                case SWT.SPACE:
                    if ((event.stateMask & SWT.CTRL) != 0)
                        event.doit = false;
                    break;
                case SWT.ARROW_DOWN:
                    handleArrowDown(event);
                    break;
                case SWT.ARROW_UP:
                    handleArrowUp(event);
                    break;
                case SWT.CR:
                    handleCR(event);
                    break;
                case SWT.ESC:
                    handleESC(event);
                    break;
                default:
                    break;
            }
        }
    }

    protected class MouseDownListener implements Listener {
        @Override
        public void handleEvent(Event event) {
            popupDetails.setVisible(false);
            popupShell.setVisible(false);
        }
    }

    protected class FocusOutListener implements Listener {
        @Override
        public void handleEvent(Event event) {
            if (control.isDisposed())
                return;

            control.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (control.isDisposed() || control.getDisplay().isDisposed())
                        return;
                    Control focusControl = control.getDisplay().getFocusControl();
                    if (focusControl == null || (focusControl != suggestionTable && focusControl != popupShell && focusControl != popupDetails)) {
                        popupDetails.setVisible(false);
                        popupShell.setVisible(false);
                    }
                }
            });
        }
    }

    protected class MoveListener implements Listener {
        @Override
        public void handleEvent(Event event) {
            if (!popupDetails.isDisposed())
                popupDetails.setVisible(false);
            if (!popupShell.isDisposed())
                popupShell.setVisible(false);
        }
    }

    protected class ControlDisposeListener implements DisposeListener {
        @Override
        public void widgetDisposed(DisposeEvent notUsed) {
            popupDetails.dispose();
            popupShell.dispose();
            gray.dispose();
        }
    }
}

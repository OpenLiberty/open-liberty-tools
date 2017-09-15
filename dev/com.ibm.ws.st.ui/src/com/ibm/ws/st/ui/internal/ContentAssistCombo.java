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
package com.ibm.ws.st.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * A text box with a drop-down arrow as well as drop-down triggered by typing.
 * If the drop-down arrow is used, all suggestions are shown. For the drop-down
 * that shows as the user types, only suggestions that match what the user
 * has typed so far are shown.
 * 
 * Content must be provided by the consumer by extending the ContentProvider class.
 * This includes the pattern matching for what the user has typed so far. There is
 * a BasicContentProvider class that can be used which takes the full list of suggestions and
 * does the filtering. However, if normalization or other operations on the text
 * are required before matching, then the consumer should provide its own ContentProvider.
 * For the pattern matching, the ContentProvider should append a wild card before and
 * after the filter.
 */
public class ContentAssistCombo extends Composite {
    private static final int MAX_ITEMS = 6;
    protected final Object NO_SUGGESTIONS = new Object();

    protected ContentProvider contentProvider;
    protected boolean updating = false;
    protected Text textBox;
    protected Label label;

    /**
     * Create a new content assist combo.
     * 
     * @param parent
     * @param contentProvider
     */
    public ContentAssistCombo(Composite parent, ContentProvider contentProvider) {
        super(parent, SWT.NONE);
        this.contentProvider = contentProvider;
        createControl();
    }

    /**
     * Create a new content assist combo with a simple list.
     * 
     * @param parent
     * @param list
     */
    public ContentAssistCombo(Composite parent, List<String> list) {
        super(parent, SWT.NONE);
        contentProvider = new SimpleContentProvider(list);
        createControl();
    }

    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        setLayout(layout);

        final Composite textComposite = new Composite(this, SWT.BORDER);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        textComposite.setLayout(layout);
        textComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        textComposite.setFont(this.getFont());

        textBox = new Text(textComposite, SWT.NONE);
        textBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(textComposite, SWT.NONE);
        label.setBackground(textBox.getBackground());
        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        label.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));

        final Shell popupShell = new Shell(getShell(), SWT.ON_TOP);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        popupShell.setLayout(layout);

        final Table suggestionTable = new Table(popupShell, SWT.SINGLE | SWT.FULL_SELECTION);
        suggestionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        suggestionTable.setHeaderVisible(false);

        new TableColumn(suggestionTable, SWT.NONE);

        textBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                if (textBox.getText().isEmpty()) {
                    popupShell.setVisible(false);
                } else {
                    fillSuggestionTable(suggestionTable, false);
                    if (suggestionTable.getItemCount() > 0) {
                        showSuggestions(popupShell, suggestionTable, textComposite);
                    } else {
                        popupShell.setVisible(false);
                    }
                }
            }
        });

        textBox.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (!popupShell.isVisible())
                    return;

                switch (event.keyCode) {
                    case SWT.ARROW_DOWN:
                        int index = (suggestionTable.getSelectionIndex() + 1) % suggestionTable.getItemCount();
                        suggestionTable.setSelection(index);
                        event.doit = false;
                        break;
                    case SWT.ARROW_UP:
                        index = suggestionTable.getSelectionIndex() - 1;
                        index = index < 0 ? suggestionTable.getItemCount() - 1 : index;
                        suggestionTable.setSelection(index);
                        event.doit = false;
                        break;
                    case SWT.ESC:
                        popupShell.setVisible(false);
                        break;
                    default:
                        break;
                }
            }
        });

        textBox.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (popupShell.isVisible()) {
                    popupShell.setVisible(false);
                }
            }
        });

        // Detect enter key pressed
        textBox.addListener(SWT.Traverse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.detail == SWT.TRAVERSE_RETURN) {
                    updating = true;
                    if (popupShell.isVisible() && suggestionTable.getSelectionIndex() != -1) {
                        TableItem item = suggestionTable.getItem(suggestionTable.getSelectionIndex());
                        if (!NO_SUGGESTIONS.equals(item.getData())) {
                            String text = suggestionTable.getItem(suggestionTable.getSelectionIndex()).getText();
                            textBox.setText(text);
                            textBox.setSelection(text.length());
                        }
                        popupShell.setVisible(false);
                        event.doit = false;
                    }
                    updating = false;
                }
            }
        });

        label.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (!label.isEnabled())
                    return;
                if (popupShell.isVisible()) {
                    popupShell.setVisible(false);
                } else {
                    fillSuggestionTable(suggestionTable, true);
                    if (suggestionTable.getItemCount() == 0) {
                        TableItem item = new TableItem(suggestionTable, SWT.NONE);
                        item.setText(Messages.contentAssistEmpty);
                        item.setData(NO_SUGGESTIONS);
                    }

                    showSuggestions(popupShell, suggestionTable, textComposite);
                    popupShell.setFocus();
                }
            }
        });

        suggestionTable.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                updating = true;
                if (suggestionTable.getSelectionIndex() != -1) {
                    TableItem item = suggestionTable.getItem(suggestionTable.getSelectionIndex());
                    if (!NO_SUGGESTIONS.equals(item.getData())) {
                        String text = item.getText();
                        textBox.setText(text);
                        textBox.setSelection(text.length());
                    }
                    popupShell.setVisible(false);
                }
                updating = false;
            }
        });

        suggestionTable.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.ESC) {
                    popupShell.setVisible(false);
                }
            }
        });

        suggestionTable.addListener(SWT.MeasureItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TableItem item = (TableItem) event.item;
                Point p = event.gc.stringExtent(item.getText());
                event.width = p.x;
                event.height = p.y;
            }
        });

        suggestionTable.addListener(SWT.EraseItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.detail &= ~SWT.FOREGROUND;
            }
        });

        suggestionTable.addListener(SWT.PaintItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TableItem item = (TableItem) event.item;
                String s = item.getText();

                GC gc = event.gc;

                int x = event.x;
                int y = event.y;
                Point match = find(s, 0);
                Color bg = null;
                if (match != null) {
                    bg = gc.getBackground();
                    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
                }
                while (match != null) {
                    Point p1 = gc.stringExtent(s.substring(0, match.x));
                    Point p2 = gc.stringExtent(s.substring(0, match.y));

                    gc.setAlpha(16);
                    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
                    gc.fillRectangle(x + p1.x + 2, y + 2, p2.x - p1.x - 2, p2.y - 1);
                    gc.setAlpha(64);

                    gc.drawRoundRectangle(x + p1.x + 1, y + 1, p2.x - p1.x - 1, p2.y, 2, 2);
                    gc.setAlpha(255);

                    match = find(s, match.y);
                }
                if (bg != null)
                    gc.setBackground(bg);

                gc.drawString(s, x + 1, y + 1, true);
            }
        });

        Listener focusOutListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (getDisplay().isDisposed())
                            return;
                        Control control = getDisplay().getFocusControl();
                        if (control == null || (control != suggestionTable && control != popupShell))
                            popupShell.setVisible(false);
                    }
                });
            }
        };

        textBox.addListener(SWT.FocusOut, focusOutListener);
        label.addListener(SWT.FocusOut, focusOutListener);
        suggestionTable.addListener(SWT.FocusOut, focusOutListener);

        getShell().addListener(SWT.Move, new Listener() {
            @Override
            public void handleEvent(Event event) {
                popupShell.setVisible(false);
            }
        });
    }

    protected void fillSuggestionTable(Table table, boolean includeHistory) {
        table.removeAll();

        String[] entries = contentProvider.getSuggestions(textBox.getText(), includeHistory);
        if (entries == null)
            return;

        for (String entry : entries) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(entry);
        }

        if (table.getItemCount() > 0)
            table.select(0);
    }

    protected void showSuggestions(Shell popupShell, Table suggestionTable, Composite textComposite) {
        Point location = ContentAssistCombo.this.toDisplay(textComposite.getLocation());
        Point size = textComposite.getSize();
        // If mirrored (right to left) then need to adjust x since SWT is
        // giving the upper right corner instead of the upper left corner of
        // the composite.  If the text box is used it gives the upper left but
        // because of the label for the drop-down the composite must be used as
        // the popup should span the text box and the label.
        if ((popupShell.getStyle() & SWT.MIRRORED) != 0)
            popupShell.setLocation(location.x - size.x, location.y + size.y);
        else
            popupShell.setLocation(location.x, location.y + size.y);
        int height = suggestionTable.getItemHeight() * Math.min(MAX_ITEMS, suggestionTable.getItemCount()) + popupShell.getBorderWidth() * 2;
        popupShell.setSize(size.x, height);
        suggestionTable.getColumn(0).setWidth(suggestionTable.getClientArea().width);
        popupShell.setVisible(true);
    }

    /**
     * Add a ModifyListener to the text box
     */
    public void addModifyListener(ModifyListener listener) {
        textBox.addModifyListener(listener);
    }

    /**
     * Set the current string in the text box.
     */
    public void setText(String text) {
        updating = true;
        textBox.setText(text);
        textBox.setSelection(text.length());
        updating = false;
    }

    /**
     * Set the current string in the text box and open the assist if there are suggestions.
     */
    public void setTextAndSuggest(String text) {
        textBox.setText(text);
        textBox.setFocus();
        textBox.setSelection(textBox.getText().length());
    }

    /**
     * Get the current string in the text box.
     */
    public String getText() {
        return textBox.getText();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
        label.setEnabled(enabled);
    }

    /**
     * Returns <code>true</code> if the given text matches the current filter, and <code>false</code>
     * otherwise.
     * 
     * @param text a String to search
     * @return <code>true</code> if the given text matches the current filter, and <code>false</code>
     *         otherwise
     */
    public boolean matches(String text) {
        return find(text, 0) != null;
    }

    /**
     * Find the first occurrence of the filter between <code>start</code>(inclusive) and
     * <code>end</code>(exclusive), or <code>null</code> if the pattern isn't found. The filter
     * accepts a simple expression pattern using '?' (any character) and '*' (any number of
     * characters). Case differences and slashes in opposite directions are ignored.
     * 
     * @param text the String to search in
     * @param start the starting index of the search range, inclusive
     * @return an <code>Point</code> object that keeps the starting
     *         (x, inclusive) and ending (y, exclusive) position of the first occurrence of the
     *         pattern in the text, or <code>null</code> if not found
     */
    protected Point find(String text, int start) {
        if (text == null)
            throw new IllegalArgumentException();

        int end = text.length();
        if (start < 0 || start >= end)
            return null;

        int matchStart = -1;

        String filter = textBox.getText();
        int ind = filter.indexOf("*");
        int posText = start; // current position in text
        int posFilter = 0; // current position in filter
        while (posFilter < filter.length()) {
            String segment = null;
            ind = filter.indexOf("*", posFilter);
            if (ind >= 0)
                segment = filter.substring(posFilter, ind);
            else
                segment = filter.substring(posFilter);

            if (segment.length() > 0) {
                int nextMatch = findSegment(text, posText, segment);
                if (nextMatch < 0)
                    return null;

                if (matchStart == -1)
                    matchStart = nextMatch;

                posText = nextMatch + segment.length();
            }
            posFilter += segment.length() + 1;
        }

        if (matchStart < 0)
            return null;

        return new Point(matchStart, posText);
    }

    /**
     * Returns the index of the given segment in the text, starting at the given position.
     * 
     * @param text a string
     * @param start the starting index
     * @param segment a string
     * @return the starting index in the text of the segment, or -1 if not found
     */
    private static int findSegment(String text, int start, String segment) {
        int max = text.length() - segment.length();
        for (int i = start; i <= max; i++) {
            if (regionMatches(text, i, segment, 0))
                return i;
        }
        return -1;
    }

    /**
     * Returns <code>true</code> if string a contains b at the exact starting positions in each string.
     * Ignores case differences, differences in slash direction, and '?' wildcards.
     * 
     * @return <code>true</code> if string a contains b at the exact starting positions in each string,
     *         or <code>false</code> otherwise
     */
    private static boolean regionMatches(String a, int aStart, String b, int bStart) {
        int i = 0;
        int len = b.length();
        while (i < len) {
            char aChar = a.charAt(aStart + i);
            char bChar = b.charAt(bStart + i);
            i++;

            if (aChar == bChar)
                continue;

            // ignore case
            if (Character.toLowerCase(aChar) == Character.toLowerCase(bChar))
                continue;

            // skip single wild cards
            if (bChar == '?')
                continue;

            // ignore slash differences
            if ((aChar == '/' && bChar == '\\') || (aChar == '\\' && bChar == '/'))
                continue;

            return false;
        }
        return true;
    }

    /**
     * Interface for the content provider for the drop-down suggestions
     */
    public static abstract class ContentProvider {
        /**
         * Get suggestions based on the given hint and/or or all of the suggestions.
         * 
         * @param hint The hint, or <code>null</code> if there is no hint
         * @param showAll <code>true</code> if all suggestions (e.g. historical entries) should be included,
         *            and <code>false</code> otherwise
         * @return The appropriate set of suggestions for the given filter.
         *         Empty array or <code>null</code> if there are no valid suggestions.
         */
        public String[] getSuggestions(String hint, boolean showAll) {
            return null;
        }
    }

    private class SimpleContentProvider extends ContentAssistCombo.ContentProvider {
        private final List<String> list;

        public SimpleContentProvider(List<String> list) {
            this.list = list;
        }

        @Override
        public String[] getSuggestions(String hint, boolean showAll) {
            List<String> suggestions = new ArrayList<String>();

            for (String s : list) {
                if (showAll || matches(s)) {
                    suggestions.add(s);
                }
            }

            return suggestions.toArray(new String[suggestions.size()]);
        }
    }
}

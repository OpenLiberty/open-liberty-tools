/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.viewers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomElementListSelectionDialog;
import com.ibm.xwt.dde.customization.ICustomLabelObject;
import com.ibm.xwt.dde.customization.ICustomMultipleDeletionObject;
import com.ibm.xwt.dde.customization.ICustomPreSelectedTreeObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.internal.actions.AddElementAction;
import com.ibm.xwt.dde.internal.actions.MoveAction;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.ValidationManager;

public class DDEViewer extends Viewer implements IPostSelectionProvider {
	
	private static final String sseErrorAnnotation1 = "org.eclipse.wst.sse.ui.temp.error";
	private static final String sseErrorAnnotation2 = "org.eclipse.ui.workbench.texteditor.error";

	private static final int IDLE_DELAY = 1500;
	private Timer timer = new Timer();
	private TimerTask timerTask;
	
	private Object input;
	private TreeViewer treeViewer;    
	private DetailsViewer detailsViewer;
	private Customization customization;
	private ScrolledComposite mainComposite;
	private Composite detailsComposite;
	private Composite missingGrammarMessageComposite;
	private ScrolledComposite rightContentOuterScrolledComposite;
	private ToolBar overviewToolBar;
	private ToolBar detailSectionToolBar;
	private Composite treeButtonsComposite;
	private Composite treeComposite;
	private IEditorPart editorPart;
	private Section detailsSection;
	private SashForm sashForm;
	private Button moveUpButton;
	private Button moveDownButton;
	private Button removeButton;
	private Button addButton;
	private boolean missingGrammarMessageVisible;
	private Form form;
	private TabbedPropertySheetWidgetFactory widgetFactory;
	private FormToolkit formToolkit;
	private Text filterText;
	private ToolBar clearTreeFilterToolBar;
	private TreeFilter treeFilter;
	private TreeFilterProcessor treeFilterProcessor;
	private Section overviewSection;
	private Text overviewSectionDescriptionText;
	private Text detailSectionDescriptionText;
	private Composite overviewSectionContentComposite;
	private Composite rightContentInnerComposite;
	private Composite detailsViewerContainer;
	private ToolItem expandSectionsToolItem;
	private ValidationManager validationManager;
	private boolean validationDirtyFlag = true;
	private boolean designViewDirtyFlag;
	private TreeLabelProvider treeLabelProvider;
	private IStructuredTextUndoManager undoManager;
	private boolean designViewActive;
	private IHyperlinkListener formMessageHyperlinkListener;
	private boolean sourceViewContainsErrors;
	private Node firstSourceViewErrorNode;
	private IResource resource;
	private boolean fireSelections;
	private FilterHandlerThread filterHandlerThread;
	private boolean readOnlyMode;
	private boolean designViewDirtySelectionFlag;
	private CommandStackListener commandStackListener;

	
	public DDEViewer(Composite parent, Customization customization, IEditorPart editorPart) {
		this.customization = customization;
		this.editorPart = editorPart;
		treeFilterProcessor = new TreeFilterProcessor();
		IEditorInput editorInput = null;
		if (editorPart != null)
		  editorInput = editorPart.getEditorInput();
		if(editorInput != null) {
			resource = (IResource) editorInput.getAdapter(IResource.class);
		}
		validationManager = new ValidationManager(customization, resource, ValidationManager.EDITOR_VALIDATION);
		validationManager.setEditorInput(editorInput);
		treeLabelProvider = new TreeLabelProvider(customization, treeFilterProcessor, validationManager, resource);
		fireSelections = true;
		filterHandlerThread = new FilterHandlerThread();
	}
	
	public Control getControl() {
		return mainComposite;
	}
	
  /**
   * This is not API.  Do not use.
   * 
   * This is a hook to support automated testing only.
   * 
   * @return
   */
	public Composite getFormHead() {
		return form.getHead();
	}
	
	private class FilterHandlerThread extends Thread {

		public void run() {
		
			// Update the filter text
			String filterValue = filterText.getText();
			treeFilterProcessor.setFilterText(filterValue);
			
			// Update the matchings
			treeFilterProcessor.updateMatchingElements();
			
			// Add the filter to the tree, making sure there is always only one
			// REF 91190 - this is run after a 250 ms delay after multiple key strokes have occurred 
			// in the filter text. In this bug, the user cleared out the entire string. So in this case, it 
			// does not make sense that the filter should be added, and certainly, the clearTreeFilterToolBar button
			// should not be shown/visible when the text is already cleared.
			if(treeViewer.getFilters().length == 0 && filterValue.length() > 0) {
				treeViewer.addFilter(treeFilter);
				clearTreeFilterToolBar.setVisible(true);
			}
			
			// Refresh the treeViewer
			treeViewer.refresh();
			
			// Expand all nodes in the treeViewer
			treeViewer.expandAll();
			
			// Refresh the detailsViewer
			detailsViewer.refresh();
			
			
		}
		
	}

	public void createContents(Composite parent) {
		
		widgetFactory = new TabbedPropertySheetWidgetFactory();
		// Create main composite
		mainComposite = widgetFactory.createScrolledComposite(parent, SWT.FLAT| SWT.V_SCROLL | SWT.H_SCROLL );
		mainComposite.setExpandHorizontal(true);
		mainComposite.setExpandVertical(true);
		mainComposite.setMinWidth(500);
		mainComposite.setMinHeight(250);
		if(customization != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, customization.getHelpContextId());
		}
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		mainComposite.setLayout(gridLayout);
		mainComposite.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				widgetFactory.dispose();
				formToolkit.dispose();
				((TreeLabelProvider)treeViewer.getLabelProvider()).dispose();
			}
		});

		formToolkit = new FormToolkit(mainComposite.getDisplay());
		form = formToolkit.createForm(mainComposite);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);
		formToolkit.decorateFormHeading(form);
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginBottom = 0;
		form.getBody().setLayout(gridLayout);
		
		boolean hideOverviewSection = false;
		if(customization != null) {
			hideOverviewSection = customization.isHideOverviewSection();
		}
		
		formMessageHyperlinkListener = new IHyperlinkListener(){

			public void linkActivated(HyperlinkEvent e) {
				ErrorMessageSummaryPopupDialog popupDialog = new ErrorMessageSummaryPopupDialog(mainComposite.getShell(), DDEViewer.this);
				popupDialog.create();
				Hyperlink hyperlink = (Hyperlink)e.widget;
				Point point = form.toDisplay(hyperlink.getLocation());
				Rectangle clientArea = popupDialog.getShell().getClientArea();
				Point ShellSize = popupDialog.getShell().getSize();
				Point actualClientArea = popupDialog.getClientAreaActualSize();
				int horizontalBorder = ShellSize.x - clientArea.width;
				int verticalBorder = ShellSize.y - clientArea.height;
				popupDialog.getShell().setSize(actualClientArea.x + horizontalBorder, actualClientArea.y + verticalBorder);
				if(popupDialog.getShell().getBounds().height > 350) {
					popupDialog.getShell().setSize(ShellSize.x, 350);
					
				}
				popupDialog.getShell().setLocation(point);
				popupDialog.open();
			}

			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}
		};
		form.addMessageHyperlinkListener(formMessageHyperlinkListener);

		
		// Update form header
		updateHeader();
		
		// Add a sashForm to the form
		sashForm = new SashForm(form.getBody(), SWT.HORIZONTAL);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalIndent = 12;
		
		sashForm.setLayoutData(gridData);
		sashForm.setForeground(widgetFactory.getColors().getForeground());
		sashForm.setBackground(widgetFactory.getColors().getBackground());
		if(hideOverviewSection) {
			sashForm.SASH_WIDTH = 0;
		}
		
		// Add left hand side composite (overview - tree) to sash
		Composite leftContent = widgetFactory.createComposite(sashForm, SWT.FLAT);
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 9;
		gridLayout.marginLeft = 8;
		gridLayout.marginTop = 0;
		gridLayout.marginBottom = 0;
		leftContent.setLayout(gridLayout);
		
		overviewSection = widgetFactory.createSection(leftContent, ExpandableComposite.TITLE_BAR);
		overviewSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		overviewSection.clientVerticalSpacing = 0;
		
		// Add toolBar to overview section
		overviewToolBar = new ToolBar(overviewSection, SWT.FLAT);
		overviewSection.setTextClient(overviewToolBar);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		overviewToolBar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		overviewToolBar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		
		// Add "Sort Alphabetically" toolItem to overview Section
		ToolItem toolItem = new ToolItem(overviewToolBar, SWT.CHECK);
		Image image = DDEPlugin.getDefault().getImage("icons/alphab_sort.gif");
		toolItem.setImage(image);
		toolItem.setToolTipText(Messages.LABEL_SORT_ALPHABETICALLY);
		
		// Obtain and initialize (when applicable) the tree sorting preference
		int treeSortingPreference = Customization.TREE_SORTING_PREFERENCE_DEFAULT;
		if(customization != null) {
			treeSortingPreference = customization.getTreeSortingPreference();
		}
		String preferenceKeyForEditor = ModelUtil.getPreferenceKeyForEditor(editorPart.getSite().getId(), DDEPlugin.PREFERENCES_SORT_TREE_ALPHABETICALLY);
		if(treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_SORTED) {
			DDEPlugin.getDefault().getPreferenceStore().setValue(preferenceKeyForEditor, true);
		} else if(treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED) {
			DDEPlugin.getDefault().getPreferenceStore().setValue(preferenceKeyForEditor, false);
		} else {
			boolean valueAlreadyInitialized = DDEPlugin.getDefault().getPreferenceStore().contains(preferenceKeyForEditor);
			if(!valueAlreadyInitialized) {
				switch(treeSortingPreference) {
					case Customization.TREE_SORTING_PREFERENCE_DEFAULT:
						boolean defaultValue = DDEPlugin.getDefault().getPreferenceStore().getBoolean(DDEPlugin.PREFERENCES_SORT_TREE_ALPHABETICALLY);
						DDEPlugin.getDefault().getPreferenceStore().setDefault(preferenceKeyForEditor, defaultValue);
						break;
					case Customization.TREE_SORTING_PREFERENCE_SORTED:
						DDEPlugin.getDefault().getPreferenceStore().setDefault(preferenceKeyForEditor, true);
						break;
					case Customization.TREE_SORTING_PREFERENCE_UNSORTED:
						DDEPlugin.getDefault().getPreferenceStore().setDefault(preferenceKeyForEditor, false);
						break;
				}
			}
		}


		// Set the initial state of the sort button
		if(treeSortingPreference != Customization.TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED) {
			if(treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_SORTED || DDEPlugin.getDefault().getPreferenceStore().getBoolean(preferenceKeyForEditor)) {		
				toolItem.setSelection(true);
			}
		}
		
		final boolean keepPreferenceStorageUpdated = !(treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_SORTED || treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED);
		toolItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				IPreferenceStore preferenceStore = DDEPlugin.getDefault().getPreferenceStore();
				String preferenceKey = DDEPlugin.PREFERENCES_SORT_TREE_ALPHABETICALLY;
				preferenceKey = ModelUtil.getPreferenceKeyForEditor(editorPart.getSite().getId() , preferenceKey);
				boolean sort = treeViewer.getSorter() == null;
				String confirmationMessage = null;
				if(customization != null) {
					if(sort) {
						confirmationMessage = customization.getTreeSortConfirmationMessage();
					} else {
						confirmationMessage = customization.getTreeUnsortConfirmationMessage();
					}
				}
				if(confirmationMessage != null) {
					if(!MessageDialog.openQuestion(DDEViewer.this.getControl().getShell(), Messages.LABEL_SORT_ALPHABETICALLY, confirmationMessage)) {
						((ToolItem)e.widget).setSelection(!((ToolItem)e.widget).getSelection());
						return;
					}
				}
				
				if(sort) {
					treeViewer.setSorter(new ViewerSorter());
					if(keepPreferenceStorageUpdated) {
						preferenceStore.setValue(preferenceKey, true);
					}
				} else {
					treeViewer.setSorter(null);
					if(keepPreferenceStorageUpdated) {
						preferenceStore.setValue(preferenceKey, false);
					}
				}
				treeViewer.refresh(true);
				refreshTreeButtons();
			}
		});	

		// Add "Expand All" toolItem to overview Section
		toolItem = new ToolItem(overviewToolBar, SWT.PUSH);
		image = DDEPlugin.getDefault().getImage("icons/expand_all.gif");
		toolItem.setImage(image);
		toolItem.setToolTipText(Messages.LABEL_EXPAND_ALL);
		toolItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				ModelUtil.getInstances((Document)input, "");
				treeViewer.expandAll();
			}
		});
		
		// Add "Collapse All" toolItem to overview Section
		toolItem = new ToolItem(overviewToolBar, SWT.PUSH);
		image = DDEPlugin.getDefault().getImage("icons/collapse_all.gif");
		toolItem.setImage(image);
		toolItem.setToolTipText(Messages.LABEL_COLLAPSE_ALL);
		toolItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				treeViewer.collapseAll();
			}
		});
		
		//Since, by default, the accessible interface of the ToolBar class does not
		//provide the name of ToolBar items to accessibility clients, we must add an
		//accessible listener to the ToolBar which sets ToolBar item names by
		//accessing its children
		overviewToolBar.getAccessible().addAccessibleListener(new AccessibleAdapter(){
			public void getName(AccessibleEvent e) {
				int childId = e.childID; //get the child ID for this accessible event
				if (childId!= ACC.CHILDID_SELF){ //ensure this is not for the toolbar, but for a child item
					ToolItem toolbarItem = overviewToolBar.getItem(childId);
        			if (toolbarItem != null) {
						String toolTipText = toolbarItem.getToolTipText();
						if(toolTipText!=null){ //set the toolbar accessible name to the tool tip text if not null
							e.result = toolTipText;
						}
					}      	
				}			
			}
		});
		
		overviewSectionContentComposite = new Composite(overviewSection, SWT.NONE);
		overviewSectionContentComposite.setBackground(widgetFactory.getColors().getBackground());
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		overviewSectionContentComposite.setLayout(gridLayout);
		
		overviewSectionDescriptionText = new Text(overviewSectionContentComposite, SWT.WRAP | SWT.READ_ONLY);
		overviewSectionDescriptionText.setBackground(widgetFactory.getColors().getBackground());
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		gridData.exclude = true;
		overviewSectionDescriptionText.setLayoutData(gridData);
		overviewSection.clientVerticalSpacing = 0;
		
		// A tree composite (holds tree viewer and tree buttons) to the left hand side composite
		treeComposite = widgetFactory.createComposite(overviewSectionContentComposite, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 1;
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 1;
		gridLayout.marginLeft = 1;
		gridLayout.marginRight = 3;
		gridLayout.marginBottom = 16;
		treeComposite.setLayout(gridLayout);    
		treeComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Add filter text field
		filterText = widgetFactory.createText(treeComposite, Messages.TYPE_FILTER_TEXT);

		// Add mouseListener to the filter text field
		filterText.addMouseListener(new MouseListener(){
			
			public void mouseDoubleClick(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
				if(filterText.getData("FOCUS") == null) {
					filterText.setData("FOCUS", "focus");
					filterText.selectAll();
				}
			}

			public void mouseUp(MouseEvent e) {
			}
		});
		
		// Add focusListener to the filter text field
		filterText.addFocusListener(new FocusListener(){

			public void focusGained(FocusEvent e) {
				filterText.selectAll();
			}

			public void focusLost(FocusEvent e) {
				filterText.setData("FOCUS", null);
			}
		});
		
		// Add keyListener to the filter text field
		filterText.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent arg0) {
				String text = filterText.getText();

				// Check if the filter text is not the empty string or the initial filter text
				if(Messages.TYPE_FILTER_TEXT.equals(text) || "".equals(text)) {
					
					// Check if the clear filter button is visible
					if(clearTreeFilterToolBar.isVisible()) {
						// Clear the filter text
						treeFilterProcessor.setFilterText(null);
						
						// Clear the filter matches
						treeFilterProcessor.clearMatches();
						
						// Remove the filter from the treeViewer
						treeViewer.removeFilter(treeFilter);
						
						// Refersh the treeViewer
						treeViewer.refresh();
						
						// Set the expandLevel of the treeViewer to 2
						treeViewer.expandToLevel(2);
						
						// Hide the clear filter button
						clearTreeFilterToolBar.setVisible(false);
						
						// Refresh the detailsViewer
						detailsViewer.refresh();
					}
				} else {
					
					DDEViewer.this.getControl().getDisplay().timerExec(250, filterHandlerThread);
				}
			}
		});
		
		
		
		filterText.addKeyListener(new KeyListener(){

			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.ARROW_DOWN) {
					treeViewer.getTree().setFocus();
				} 
			}

			public void keyReleased(KeyEvent e) {
			}
		});
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.verticalIndent = 1;
		gridData.verticalAlignment = SWT.BEGINNING;
		filterText.setLayoutData(gridData);
		
		// Create toolBar for the clear filter button
		clearTreeFilterToolBar = new ToolBar(treeComposite, SWT.FLAT);
		clearTreeFilterToolBar.setBackground(widgetFactory.getColors().getBackground());
		
		// Add clear filter text toolItem to the toolBar
		toolItem = new ToolItem(clearTreeFilterToolBar, SWT.PUSH);
		image = DDEPlugin.getDefault().getImage("icons/clear.gif");
		toolItem.setImage(image);
		toolItem.setToolTipText(Messages.LABEL_CLEAR);
		toolItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				// Clear the text of the filter text field
				filterText.setText("");
				
				// Clear the filter text
				treeFilterProcessor.setFilterText(null);
				
				// Clear the filterMatches
				treeFilterProcessor.clearMatches();
				
				// Hide the clear filter toolbar/button
				clearTreeFilterToolBar.setVisible(false);
				
				// Remove the filter from the treeViewer
				treeViewer.removeFilter(treeFilter);
				
				// Refresh the treeViewer
				treeViewer.refresh(false);
				
				// Set the expandLevel of the treeViewer to 2
				treeViewer.expandToLevel(2);
				
				// Refresh the detailsViewer
				detailsViewer.refresh();
			}
		});
		
		gridData = new GridData();
		gridData.verticalIndent = 1;
		gridData.horizontalIndent = 4;
		clearTreeFilterToolBar.setLayoutData(gridData);
		clearTreeFilterToolBar.setVisible(false);
		
		new Label(treeComposite, SWT.NONE); // filler
		
		// Add tree viewer to tree composite
		treeViewer = new TreeViewer(widgetFactory.createTree(treeComposite,  SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL));
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalIndent = 7;
		treeViewer.getTree().setLayoutData(gridData);
		
		
		treeViewer.addHelpListener(new HelpListener(){

			public void helpRequested(HelpEvent e) {
				TreeItem[] selection = ((Tree)e.widget).getSelection();
				if(selection.length == 1) {
					TreeItem treeItem = selection[0];
					Object object = treeItem.getData();
					if(object instanceof Element) {
						Element element = (Element)object;
						String elementFullPath = ModelUtil.getElementFullPath(element);
						if(customization != null) {
							DetailItemCustomization itemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), elementFullPath);
							if(itemCustomization != null) {
								String helpContextId = itemCustomization.getHelpContextId();
								if(helpContextId != null) {
									PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContextId);
									return;
								}
							}
						}
					}
				}
				PlatformUI.getWorkbench().getHelpSystem().displayDynamicHelp();
			}
		});
		
		
		// Add tree buttons composite to tree composite
		treeButtonsComposite = new Composite(treeComposite, SWT.NONE);
		treeButtonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		treeButtonsComposite.setBackground(widgetFactory.getColors().getBackground());
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.marginTop = 2;
		rowLayout.marginBottom = 0;
		rowLayout.marginLeft = 7;
		rowLayout.marginRight = 0;
		rowLayout.fill = true;
		rowLayout.spacing = 4;
		treeButtonsComposite.setLayout(rowLayout);

		// Add button
		addButton = new Button(treeButtonsComposite, SWT.FLAT);
		addButton.setText(Messages.LABEL_ADD_DOTS);
		addButton.addSelectionListener(new SelectionListener(){
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				triggerAddButton();
			}
		});
		

		// Remove button
		removeButton = new Button(treeButtonsComposite, SWT.FLAT);
		removeButton.setText(Messages.LABEL_REMOVE);
		removeButton.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				performDelete();
				((Button)e.widget).setFocus();
			}
		});

		
		// Add separator before the up/down buttons
		Composite composite = new Composite(treeButtonsComposite, SWT.None);
		composite.setBackground(widgetFactory.getColors().getBackground());
		RowData rowData = new RowData();
		rowData.height = 0;
		composite.setLayoutData(rowData);
		
		// Move up button
		moveUpButton = new Button(treeButtonsComposite, SWT.FLAT);
		moveUpButton.setText(Messages.LABEL_UP);
		moveUpButton.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				MoveAction moveAction = new MoveAction(DDEViewer.this, true);
				moveAction.run();
				((Button)e.widget).setFocus();
			}
		});
	
		// Move down button
		moveDownButton = new Button(treeButtonsComposite, SWT.FLAT);
		moveDownButton.setText(Messages.LABEL_DOWN);
		moveDownButton.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				MoveAction moveAction = new MoveAction(DDEViewer.this, false);
				moveAction.run();
				((Button)e.widget).setFocus();
			}
		});
		
		// Set up the tree context menu
		MenuManager menuManager = new MenuManager();  
		treeViewer.getTree().setMenu(menuManager.createContextMenu(treeViewer.getTree()));
		menuManager.addMenuListener(new TreeItemMenuListener(this, customization, editorPart));
		menuManager.setRemoveAllWhenShown(true);
		
		// Set the tree sorter (when applicable)
		if(treeSortingPreference != Customization.TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED) {
			if(treeSortingPreference == Customization.TREE_SORTING_PREFERENCE_ALWAYS_SORTED || DDEPlugin.getDefault().getPreferenceStore().getBoolean(preferenceKeyForEditor)) {		
				treeViewer.setSorter(new ViewerSorter());
			}
		}

		// Set the tree content provider
		treeViewer.setContentProvider(new TreeContentProvider(customization));
		
		// Set the tree label provider
		treeViewer.setLabelProvider(treeLabelProvider);
		
		// Add the element selection change listener to the tree viewer
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener(){

			public void selectionChanged(SelectionChangedEvent event) {   
				if(!missingGrammarMessageVisible) {
					ISelection selection = event.getSelection();
					if (selection instanceof StructuredSelection) {
						StructuredSelection structuredSelection = (StructuredSelection)selection;
						if (structuredSelection.size() == 1) {  
							Object object = structuredSelection.getFirstElement();
							if (object instanceof Element) {
								Element element = (Element)object;
								updateDetailSectionHeaderText(element);
								detailsViewer.setInput(element);
								
								Control currentControl = detailsViewer.getCurrentControl();
								if(currentControl == null)
									rightContentOuterScrolledComposite.setOrigin(0,0);
								updateDetailsViewScrolling();
								updateDetailSectionTitle(element);
								if(fireSelections) {
									fireSelectionChanged(event);
								}
							}
						} else if(structuredSelection.size() > 1) {
							updateDetailSectionTitle(null);
							detailsViewer.setInput(Messages.MULTIPLE_ITEMS_ARE_CURRENTLY_SELECTED);
							updateDetailsViewScrolling();
						}
						else {
							//if nothing is selected then don't show any details
							detailsViewer.setInput(null);
							updateDetailsViewScrolling();
						}
					}
					if(ModelUtil.isDesignViewPageActiveAndInFocus()) {
						refreshTreeButtons();
					} else {
						designViewDirtySelectionFlag = true;
					}
				}
			}
		});		
		
		// Add keyListener to the tree to handle the delete key
		treeViewer.getTree().addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent event) {
				if(event.keyCode == SWT.DEL) {
					if(removeButton.isEnabled()) {
						performDelete();
					}
				}
			}

			public void keyReleased(KeyEvent event) {
			}
		});
		
		// Set the overview section client
		overviewSection.setClient(overviewSectionContentComposite);
		
		// Create the right content composite
		Composite rightContentOutter = widgetFactory.createComposite(sashForm);
		gridLayout = new GridLayout();
		gridLayout.marginRight = 7;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		rightContentOutter.setLayout(gridLayout);
		rightContentOutter.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Create the right content outer scrolled composite
		rightContentOuterScrolledComposite = widgetFactory.createScrolledComposite(rightContentOutter, SWT.FLAT | SWT.V_SCROLL | SWT.H_SCROLL);
		rightContentOuterScrolledComposite.setExpandHorizontal(true);
		rightContentOuterScrolledComposite.setExpandVertical(true);   
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		rightContentOuterScrolledComposite.setLayoutData(gridData);
		
		//add resize listener to force update on the scrollbar minimum height
		rightContentOuterScrolledComposite.addControlListener(new ControlAdapter() {
			
			 public void controlResized(ControlEvent e) {
				 	updateDetailsViewScrolling();
		        }
		});
		
		// Create the right content inner composite
		rightContentInnerComposite = widgetFactory.createComposite(rightContentOuterScrolledComposite, SWT.FLAT);
		gridLayout = new GridLayout();
		gridLayout.marginTop = 0;
		gridLayout.marginBottom = 4;
		gridLayout.marginLeft = 9;
		gridLayout.marginRight = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		rightContentInnerComposite.setLayout(gridLayout);
		
		// Set the scrolled composite content
		rightContentOuterScrolledComposite.setContent(rightContentInnerComposite);

		// Create the detail section
		detailsSection = widgetFactory.createSection(rightContentInnerComposite, ExpandableComposite.TITLE_BAR);
		detailsSection.setText(Messages.LABEL_DETAILS);
		detailsSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		detailsSection.clientVerticalSpacing = 0;
		
		// Add toolBar to details section
		detailSectionToolBar = new ToolBar(detailsSection, SWT.FLAT);
		detailsSection.setTextClient(detailSectionToolBar);
		detailSectionToolBar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		detailSectionToolBar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		
		expandSectionsToolItem = new ToolItem(detailSectionToolBar, SWT.CHECK);
		image = DDEPlugin.getDefault().getImage("icons/expand_sections.gif");
		expandSectionsToolItem.setImage(image);
		expandSectionsToolItem.setDisabledImage(DDEPlugin.getDefault().getImage("icons/expand_sections_disabled.gif"));
		expandSectionsToolItem.setToolTipText(Messages.LABEL_EXPAND_SECTIONS);
		if(DDEPlugin.getDefault().getPreferenceStore().getBoolean(DDEPlugin.PREFERENCES_EXPAND_SECTIONS)) {		
			expandSectionsToolItem.setSelection(true);
		}
		expandSectionsToolItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				IPreferenceStore preferenceStore = DDEPlugin.getDefault().getPreferenceStore();
				boolean value = !detailsViewer.getExpandSections();
				detailsViewer.setExpandSections(value);
				preferenceStore.setValue(DDEPlugin.PREFERENCES_EXPAND_SECTIONS, value);
				updateDetailsViewScrolling();
			}
		});
		
		//Since, by default, the accessible interface of the ToolBar class does not
		//provide the name of ToolBar items to accessibility clients, we must add an
		//accessible listener to the ToolBar which sets ToolBar item names by
		//accessing its children
		detailSectionToolBar.getAccessible().addAccessibleListener(new AccessibleAdapter(){
			public void getName(AccessibleEvent e) {
				int childId = e.childID; //get the childID for this accessible event
				if (childId!= ACC.CHILDID_SELF){ //ensure this is not for the toolbar, but for a child item
					ToolItem toolbarItem = detailSectionToolBar.getItem(childId);
        			if (toolbarItem != null) {
						String toolTipText = toolbarItem.getToolTipText();
						if(toolTipText!=null){ //set the toolbar accessible name to the tool tip text if not null
							e.result = toolTipText;
						}
					}      	
				}			
			}
		});
		
		// Add details composite to scrolled composite
		detailsComposite = new Composite(detailsSection, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.marginTop = 0;
		gridLayout.marginLeft = 0;
		gridLayout.marginRight = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		detailsComposite.setLayout(gridLayout);

		// Add the detail section header text
		detailSectionDescriptionText = new Text(detailsComposite, SWT.WRAP | SWT.READ_ONLY);
		detailSectionDescriptionText.setBackground(widgetFactory.getColors().getBackground());
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.exclude = true;
		detailSectionDescriptionText.setLayoutData(gridData);

		detailsViewerContainer = widgetFactory.createComposite(detailsComposite);
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		detailsViewerContainer.setLayout(gridLayout);
		detailsViewerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		
		// Add the details viewer to the details composite
		detailsViewer = new DetailsViewer(detailsViewerContainer, widgetFactory, editorPart, customization, treeFilterProcessor, this);
		detailsViewer.setContentProvider(new DetailsContentProvider(customization));    
		detailsViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
	
		detailsViewer.addSelectionChangedListener(new ISelectionChangedListener(){

			public void selectionChanged(SelectionChangedEvent event) {
				if(fireSelections) {
					fireSelectionChanged(event);
				}
			}
		});

		// Set the sash distribution of the left and right hand side
		if(hideOverviewSection) {
			sashForm.setWeights(new int[] {0, 100});
			sashForm.pack();
		} else {
			sashForm.setWeights(new int[] {45, 55});	
			// Make the sash change color when the mouse is over it
			sashForm.pack();
			final Control[] controls = sashForm.getChildren();
      if(controls != null && controls.length > 2)
			{
				final Sash sash = (Sash) sashForm.getChildren()[2];
				sash.addMouseTrackListener(new MouseTrackListener(){

					public void mouseEnter(MouseEvent e) {
						sash.setBackground(widgetFactory.getColors().getColor(IFormColors.H_GRADIENT_START));
					}
	
					public void mouseExit(MouseEvent e) {
						sash.setBackground(widgetFactory.getColors().getBackground());
					}
	
					public void mouseHover(MouseEvent e) {
					}
				});
			}
		}
		
		

		detailsSection.setClient(detailsComposite);
		mainComposite.setContent(form);
		treeFilter = new TreeFilter();

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=487160
		// Mars.1 - not reproducible
		// Mars.2 RC2 - reproducible
		// Neon - reproducible
		// Workaround, do it only for nonWindows platforms:
		String osName =  System.getProperty("os.name").toLowerCase();
		boolean isWindows = osName != null ? osName.indexOf("win") >= 0 : true; // Don't do workaround otherwise, if osName is null
		if (!isWindows) {
			Composite tabFolder = mainComposite;
			// Find the tab folder that has already been sized
			do {
				tabFolder = tabFolder.getParent();
				if (tabFolder == null) {
					break;				
				}
				if (tabFolder instanceof CTabFolder && tabFolder.getSize().x != 0 && tabFolder.getSize().y != 0) {
					break;
				}
			} while (tabFolder.getParent() != null);
			mainComposite.setSize(tabFolder.getSize());
		}
	}


	public void updateDetailsViewScrolling() {
		if (rightContentOuterScrolledComposite != null && rightContentInnerComposite != null) {
			rightContentOuterScrolledComposite.layout(true,true);	
			/* REF: 107439
			 * Use inner composite of a main section to calculate
			 * height based on known width. Since toolbar height is not included calculate it too 
			 */
			int innerHeight = detailsViewerContainer.computeSize(detailsViewerContainer.getSize().x, SWT.DEFAULT).y;
			int toolbarHeight = detailSectionToolBar.computeSize(detailsViewerContainer.getSize().x, SWT.DEFAULT).y; 
			int newHeight = innerHeight+toolbarHeight;
			int vsWidth = rightContentOuterScrolledComposite.getVerticalBar().getSize().x+8;
			if (rightContentOuterScrolledComposite.getMinHeight()!=newHeight)
			{
				rightContentOuterScrolledComposite.setMinHeight(newHeight);
				if (detailsViewer != null && detailsViewer.getScrolledComposite()!=null)
					rightContentOuterScrolledComposite.setMinWidth(detailsViewer.getScrolledComposite().getMinWidth()+vsWidth);
				/*calculate again
				switching between views with scroll bar and without 
				causes width to be incorrect when calculating first time (width of sctollbar is not included)*/
				updateDetailsViewScrolling();
			}
			
			//increment fix for scrollbar REF: 107446
			ScrollBar vs = rightContentOuterScrolledComposite.getVerticalBar();
			if (vs!=null)
			{
				vs.setPageIncrement(vs.getMaximum()/3);
				vs.setIncrement(vs.getMaximum()/10);
			}			
		}		
	}

	public void setInput(Object input) {
		if(this.input != input) {
			this.input = input;
			Object object = editorPart.getAdapter(ITextEditor.class);
			if(object instanceof StructuredTextEditor) {
				StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
				this.readOnlyMode = !structuredTextEditor.isEditable();
			}
			if(input instanceof Node) {
				Node node = (Node)input;
				treeViewer.setInput(node);
				treeViewer.expandToLevel(2);
				if(node.getNodeType() == Node.DOCUMENT_NODE) {
					Document document = (Document)node;
					ICustomPreSelectedTreeObject preSelectedObject= null;
					if(customization != null)
						preSelectedObject = customization.getCustomPreSelectedTreeObject();
					
					if (preSelectedObject == null) {
						for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
							if(childNode.getNodeType() == Node.ELEMENT_NODE) {
								
								Element rootElement = (Element)childNode;
								treeViewer.setSelection(new StructuredSelection(rootElement));
								validationManager.setDocument(document);

								updateValidationInformation();
								break;
							}
						}
					} else {
						Element preSelectedElement = preSelectedObject.getPreSelectedTreeElement(document, editorPart.getEditorInput());
						// If the element returned by the customized object is null,
						// then we select the root element.
						if (preSelectedElement == null) {
							for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
								if(childNode.getNodeType() == Node.ELEMENT_NODE) {
									
									preSelectedElement = (Element)childNode;
								}
							}
						}
						if (preSelectedElement != null) 
						{
						  // Walk up the tree structure and expand it, and then select it
              Node parent = preSelectedElement;
              // Counter to keep track of which level to expand to
              int i = 0;
              // In DOM trees, there can be endless nodes that reference each other.  We could potentially be in 
              // an endless loop.  Let's cap it arbitrarily at 100 levels.
              while (parent != document.getDocumentElement() && parent != null && i < 100)
              {
                 parent = parent.getParentNode();
                 i++;
                 treeViewer.expandToLevel(parent, i);
              }
              treeViewer.setSelection(new StructuredSelection(preSelectedElement));
						}
						validationManager.setDocument(document);

						updateValidationInformation();
					}
				}
				refreshHeaderLabel();
				updateOverviewSectionTitle();
				updateOverviewSectionHeaderText();
				refreshTreeButtons();
				updateDetailsViewScrolling();
			}
		}
	}


	public TreeViewer getTreeViewer() {
		return treeViewer;
	}

	
	public Object getInput() {
		return input;
	}

	public ISelection getSelection() {
		ISelection selection = null;
		selection = detailsViewer.getSelection();
		if(selection == null) {
			selection = treeViewer.getSelection();
		}
		return selection;
	}

	public void refresh() {
		refresh(false);
	}
	
	public void refresh(boolean forceRefresh) {

		if(ModelUtil.isSourceViewPageActiveAndInFocus()) {
			designViewDirtyFlag = true;
		} else {
			if(!missingGrammarMessageVisible && !DDEViewer.this.getControl().isDisposed()) {
				// Refresh the tree viewer
				treeViewer.refresh(true);
		
				// Refresh (when applicable) the detail section title
				ISelection selection = treeViewer.getSelection();
				if(selection instanceof IStructuredSelection) {
					IStructuredSelection structuredSelection = (IStructuredSelection)selection;
					Object object = structuredSelection.getFirstElement();
					if(object instanceof Element) {
						Element element = (Element)object;
						updateDetailSectionTitle(element);
						updateDetailSectionHeaderText(element);
					}
				}
				
				// Update the header only when forceRefresh is true
				if(forceRefresh) {
					updateHeader();
				}
				
				// Refresh the header label
				refreshHeaderLabel();
				
				// Update the overview section title
				updateOverviewSectionTitle();
				
				// Update the overview section description
				updateOverviewSectionHeaderText();
		
				// Check if the design view is active and has focus
//				boolean refershDetailsViewer = !ModelUtil.isDesignViewPageActiveAndInFocus();
				
				// Refresh the detailsViewer when applicable (either it is not active and with focus, or forceRefresh is true)
//				if(forceRefresh || refershDetailsViewer) {
				if(forceRefresh || !designViewActive) {
					
					detailsViewer.refresh();
					updateDetailsViewScrolling();
				}
				if(!validationDirtyFlag) {
					//updateValidation();
					updateValidationInformation();
				}
			}
		}
	}

	
	public void setSelection(ISelection selection, boolean reveal) {
		if (selection instanceof IStructuredSelection) {
			List selections = ((IStructuredSelection) selection).toList();
			if(selections.size() >= 1) {
				Object object = selections.get(0);
				Element selectedElement = null;
				if(object instanceof Element) {
					selectedElement = (Element)object;
				} else if(object instanceof Attr) {
					Attr attr = (Attr)object;
					selectedElement = attr.getOwnerElement();
				}
				if(selectedElement != null) {
					// Update treeViewer selection
					ModelQuery modelQuery = ModelQueryUtil.getModelQuery(selectedElement.getOwnerDocument());
					if(ModelUtil.isRootElement(selectedElement)) {
						StructuredSelection structuredSelection = new StructuredSelection(selectedElement);
						treeViewer.setSelection(structuredSelection, reveal);
					} else {
						Element element = selectedElement;
						Node selectedElementParentNode = element.getParentNode();
						if(selectedElementParentNode instanceof Element) {
							Element parentElement = (Element)selectedElementParentNode;
							CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
							CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
							// Reduce the scope of the selection to elements that appear in the tree
							if(cmElementDeclaration != null && parentCMElementDeclaration != null) {
								while(!ModelUtil.elementMustAppearInTree(customization, parentElement, parentCMElementDeclaration, cmElementDeclaration)) {
									element = parentElement;
									if(ModelUtil.isRootElement(element)) {
										break;
									}
									Node currentParentNode = element.getParentNode();
									if(currentParentNode instanceof Element) {
										parentElement = (Element) currentParentNode;
										cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
										parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
									} else {
										break;
									}
								}
								
								// Ensure required tree nodes are expanded before selection
								Stack nodes = new Stack();
								Node parentNode = element.getParentNode();
								while(parentNode.getNodeType() == Node.ELEMENT_NODE) {
									nodes.push(parentNode);
									parentNode = parentNode.getParentNode();
								}
								while(!nodes.empty()) {
									//StructuredSelection structuredSelection = new StructuredSelection(element);
									treeViewer.setExpandedState(nodes.pop(), true);
								}
								
								// Set the treeViewer selection
								StructuredSelection structuredSelection = new StructuredSelection(element);
								treeViewer.setSelection(structuredSelection, reveal);
							}
						}
					}
					
					// Update detailsViewer selection
					detailsViewer.setScrolledComposite(rightContentOuterScrolledComposite);
					detailsViewer.setSelection(selection, reveal);
				}
			}
		}
	}
	
	public void activate() {
		designViewActive = true;
		if(designViewDirtyFlag) {
			refresh(true);
			designViewDirtyFlag = false;
		}
		updateValidation();
		updateValidationInformation();
		detailsViewer.activate();
		if(designViewDirtySelectionFlag) {
			designViewDirtySelectionFlag = false;
			refreshTreeButtons();
			updateDetailsViewScrolling();
		}
	}
	
	public void deActivate() {
		designViewActive = false;
	}

	private void updateDetailSectionTitle(Element element) {
		String title = null;
		if(element != null) {
			String path = ModelUtil.getElementFullPath(element);
			if(customization != null) {
				DetailItemCustomization detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), path);
				if(detailItemCustomization != null) {
					
					// Check if there is a detail section title class
					Class detailSectionTitleClass = detailItemCustomization.getDetailSectionTitleClass();
					if(detailSectionTitleClass != null) {
						try {
							Object object = detailSectionTitleClass.newInstance();
							if(object instanceof ICustomLabelObject) {
								ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
								title = customLabelObject.getLabel(element, resource);
							}
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						}
					} else {
						String detailSectionTitle = detailItemCustomization.getDetailSectionTitle();
						if(detailSectionTitle != null) {	
							if(detailSectionTitle.indexOf('$') != -1) {
								detailSectionTitle = ModelUtil.processLabelDistinguishers(element, detailSectionTitle);
							}
							title = detailSectionTitle;
						}
					}
				}
				else {
					Class detailSectionTitleClass = customization.getDetailSectionTitleClass();
					if(detailSectionTitleClass != null) {
						try {
							Object object = detailSectionTitleClass.newInstance();
							if(object instanceof ICustomLabelObject) {
								ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
								title = customLabelObject.getLabel(element, resource);
							}
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(title == null) {
			title = Messages.LABEL_DETAILS;
		}
		if(!detailsSection.getText().equals(title)) {
			detailsSection.setText(title);
			detailsSection.layout();
		}
	}

	private void updateDetailSectionHeaderText(Element element) {
		String path = ModelUtil.getElementFullPath(element);
		String sectionHeaderText = null;
		if(customization != null) {
			DetailItemCustomization detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), path);
			if(detailItemCustomization != null) {
				sectionHeaderText = detailItemCustomization.getSectionHeaderText();
				if(sectionHeaderText != null) {
					sectionHeaderText = ModelUtil.formatHeaderOrFooterText(sectionHeaderText);
				}
			}
		}
	}

	
	public void hideMissingGrammarMessage() {
		if(missingGrammarMessageVisible) {
			missingGrammarMessageComposite.dispose();
			GridData gridData = (GridData)form.getLayoutData();
			gridData.exclude = false;
			mainComposite.setContent(form);
			mainComposite.setMinWidth(500);
			mainComposite.setMinHeight(250);
			form.setVisible(true);
			
			mainComposite.layout();
			missingGrammarMessageVisible = false;
			refresh();
		}
		
	}
	
	
	public void showMissingGrammarMessage() {
		if(!missingGrammarMessageVisible) {
			GridData gridData = (GridData)form.getLayoutData();
			gridData.exclude = true;
			form.setVisible(false);

			// Missing grammar message composite
			missingGrammarMessageComposite = new Composite(mainComposite, SWT.NONE);
			GridLayout gridLayout = new GridLayout();
			missingGrammarMessageComposite.setLayout(gridLayout);
			missingGrammarMessageComposite.setBackground(widgetFactory.getColors().getBackground());


			Composite innerComposite = new Composite(missingGrammarMessageComposite, SWT.NONE);
			gridLayout = new GridLayout();
			gridLayout.numColumns = 2;
			innerComposite.setLayout(gridLayout);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.verticalAlignment = SWT.CENTER;
			gridData.horizontalAlignment = SWT.CENTER;
			innerComposite.setLayoutData(gridData);
			innerComposite.setBackground(widgetFactory.getColors().getBackground());
			
			
			// Missing grammar message icon
			Label warningIcon = new Label(innerComposite, SWT.NONE);
			warningIcon.setBackground(widgetFactory.getColors().getBackground());
			warningIcon.setImage(DDEPlugin.getDefault().getImageDescriptor("icons/warning.gif").createImage());

			// Missing grammar message text
			Label label = new Label(innerComposite, SWT.WRAP);
			label.setBackground(widgetFactory.getColors().getBackground());
			label.setText(Messages.MISSING_GRAMMAR_MESSAGE);
			
			missingGrammarMessageComposite.pack();
			mainComposite.setMinSize(missingGrammarMessageComposite.getSize());
			mainComposite.setContent(missingGrammarMessageComposite);
			mainComposite.layout();
			missingGrammarMessageVisible = true;
		}
	}
	
	
	private void refreshTreeButtons() {
		if(!readOnlyMode) {
			boolean enableAdd = false;
			boolean enableRemove = true;
			boolean enableMoveUp = false;
			boolean enableMoveDown = false;
	
			TreeItem[] treeItems = treeViewer.getTree().getSelection();
			if(treeItems.length == 1) {
			
				TreeItem treeItem = treeItems[0];
				TreeItem parentTreeItem = treeItem.getParentItem();
				Element element = (Element)treeItem.getData();
				Node node = element.getParentNode();
				
				if(!(node.getNodeType() == Node.DOCUMENT_NODE)) {
					enableRemove = ModelUtil.canDeleteElement(element, customization, editorPart);
				} else {
					enableRemove = false;
				}
				
				ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
				if(modelQuery != null) {
					CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
					if(cmElementDeclaration != null) {
						if(ModelUtil.insertActionsAvailable(element, cmElementDeclaration, modelQuery, customization, editorPart)) {
							enableAdd = true;
						}
					}
				}
				
				if(treeViewer.getSorter() == null) {
					if(parentTreeItem != null) {
						TreeItem[] childItems = parentTreeItem.getItems();
						if(!childItems[0].equals(treeItem)) {
							enableMoveUp = true;
						}
						if(!childItems[childItems.length - 1].equals(treeItem)) {
							enableMoveDown = true;
						}
					}
				}
			} else if(treeItems.length > 1) {
				for (int i = 0; i < treeItems.length && enableRemove; i++) {
					Object object = treeItems[i].getData();
					if(object instanceof Element) {
						Element currentElement = (Element)object;
						enableRemove = ModelUtil.canDeleteElement(currentElement, customization, editorPart);
					}
				}
			} else if(treeItems.length == 0) {
				enableRemove = false;
			}
			
			addButton.setEnabled(enableAdd);
			removeButton.setEnabled(enableRemove);
			moveUpButton.setEnabled(enableMoveUp);
			moveDownButton.setEnabled(enableMoveDown);
		} else {
			addButton.setEnabled(false);
			removeButton.setEnabled(false);
			moveUpButton.setEnabled(false);
			moveDownButton.setEnabled(false);
		}
	}
	
	
	private void updateHeader() {
		if(customization != null) {
			// Update header icon
			Image headerIcon = customization.getHeaderIcon();
			if(headerIcon != null) {
				form.setImage(headerIcon);
			}
			
			// Update header actions
			form.getToolBarManager().removeAll();
			form.updateToolBar();
			Action[] actions = customization.getHeaderActions();
			if(actions != null) {
				for (int i = 0; i < actions.length; i++) {
					form.getToolBarManager().add(actions[i]);
				}
			}
			form.updateToolBar();
		}
	}

	
	private void refreshHeaderLabel() {
		String headerLabel = null;
		if(customization != null && input != null) {
			ICustomLabelObject customLabelObject = customization.getHeaderLabelObject();
			if(customLabelObject != null) {
				Document document = (Document)input;
				Element element = document.getDocumentElement();
				if(element != null) {
					headerLabel = customLabelObject.getLabel(element, resource);
				}
			} else {
				headerLabel = customization.getHeaderLabel();
				if(headerLabel != null && headerLabel.indexOf('$') != -1) {
					Document document = (Document)input;
					Element element = document.getDocumentElement();
					if(element != null) {
						headerLabel = ModelUtil.processLabelDistinguishers(element, headerLabel);
					}
				}
			}

		}
		if(headerLabel == null) {
			headerLabel = editorPart.getEditorSite().getRegisteredName();
		}
		if(!headerLabel.equals(form.getText())) {
			form.setText(headerLabel);
		}
	}
	
	
	public TreeFilterProcessor getTreeFilterProcessor() {
		return treeFilterProcessor;
	}
	
	
	private class TreeFilter extends ViewerFilter {

		public boolean select(Viewer viewer, Object parentElement, Object element) {

			if(element instanceof Element) {
				Element domElement = (Element)element;

				if(treeFilterProcessor.isMatchedElement(domElement)) {
					return true;
				}
				
				// Check the children
				TreeContentProvider treeContentProvider = (TreeContentProvider)treeViewer.getContentProvider();
				Stack stack = new Stack();
				stack.addAll(Arrays.asList(treeContentProvider.getChildren(element)));
				while(!stack.isEmpty()) {
					Object object = stack.pop();
					if(object instanceof Element) {
						Element childElement = (Element)object;
						if(treeFilterProcessor.isMatchedElement(childElement)) {
							return true;
						}
						stack.addAll(Arrays.asList(treeContentProvider.getChildren(childElement)));
					}
				}
			}
			return false;
		}
	}
	
	public class TreeFilterProcessor {
	
		private HashMap matchedElements;
		private String filterText;
		
		public TreeFilterProcessor() {
			matchedElements = new HashMap();
		}
		
		public boolean isMatchedElement(Element element) {
			return "true".equals(matchedElements.get(element));
		}
		
		public void clearMatches() {
			matchedElements.clear();
		}
		
		public void setFilterText(String filterText) {
			this.filterText = filterText;
		}
		
		public String getFilterText() {
			return filterText;
		}
		
		public void updateMatchingElements() {
			matchedElements.clear();
			if(filterText != null && !"".equals(filterText)) {
				Stack elementStack = new Stack();
				NodeList nodeList = ((Document)input).getChildNodes();
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					if(node.getNodeType() == Node.ELEMENT_NODE) {
						elementStack.add(node);
					}
				}
				
				while(!elementStack.isEmpty()) {
					Element element = (Element)elementStack.pop();
					elementStack.addAll(Arrays.asList(((TreeContentProvider)treeViewer.getContentProvider()).getChildren(element)));
					matchedElements.put(element, elementMatches(element)? "true": "false");
				}
			}
		}
		
		private boolean elementMatches(Element domElement) {
			String filterTextValue = filterText.toLowerCase();
	
			// Check if the element node contains the filter text value
			if(domElement.getNodeName().toLowerCase().indexOf(filterTextValue) != -1) {
				return true;
			}
			
			
			// Check if the element tree label matches
			if(ModelUtil.getTreeNodeLabel(domElement, customization, resource).toLowerCase().indexOf(filterTextValue) != -1) {
				return true;
			}
	
			// Check for element customization match
			if(customization != null) {
				DetailItemCustomization detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(domElement), ModelUtil.getElementFullPath(domElement));
				if(detailItemCustomization != null) {
					if(customizationItemMatches(filterTextValue, detailItemCustomization)) {
						return true;
					}
				}
			}
	
			// Check the detail items associated with the element
			DetailItem[] detailItems = detailsViewer.getContentProvider().getItems(domElement);
			for (int i = 0; i < detailItems.length; i++) {
				DetailItem detailItem = detailItems[i];
				
				// Check if the detail item name matches
				if(detailItem.getName().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check the atomicDetailItems within simpleDetailItems
				if(detailItem instanceof SimpleDetailItem) {
					SimpleDetailItem simpleDetailItem = (SimpleDetailItem)detailItem;
					DetailItem[] childDetailItems = simpleDetailItem.getAtomicDetailItems();
					for (int j = 0; j < childDetailItems.length; j++) {
						DetailItem childDetailItem = childDetailItems[j];
						
						// Check if the child detail item name matches
						if(childDetailItem.getName().indexOf(filterTextValue) != -1) {
							return true;
						}
						
						// Check for child detail item customization match
						DetailItemCustomization childDetailItemCustomization = childDetailItem.getDetailItemCustomization();
						if(childDetailItemCustomization != null) {
							if(customizationItemMatches(filterTextValue, childDetailItemCustomization)) {
								return true;
							}
						}
					}
				}
				
				// check for detail item customization match when applicable
				DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
				if(detailItemCustomization != null) {
					if(customizationItemMatches(filterTextValue, detailItemCustomization)) {
						return true;
					}
				}
	
			}
			return false;
		}
		
		
		private boolean customizationItemMatches(String filterTextValue, DetailItemCustomization detailItemCustomization) {
	
			String value = null;
			// Check if the label contains the filter text value
			if(detailItemCustomization !=null){
				value = detailItemCustomization.getLabel();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the tree label contains the filter text value
				value = detailItemCustomization.getTreeLabel();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the checkBoxText contains the filter text value
				value = detailItemCustomization.getCheckBoxText();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the detailSectionTitle contains the filter text value
				value = detailItemCustomization.getDetailSectionTitle();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the sectionHeaderText contains the filter text value
				value = detailItemCustomization.getSectionHeaderText();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the headerText contains the filter text value
				value = detailItemCustomization.getHeaderText();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
				
				// Check if the footerText contains the filter text value
				value = detailItemCustomization.getFooterText();
				if(value != null && value.toLowerCase().indexOf(filterTextValue) != -1) {
					return true;
				}
			}
			return false;
		}
	}

	private void updateOverviewSectionTitle() {
		String overviewSectionTitle= null;
		if(customization != null) {
			Document document = (Document)input;
			Element element = document.getDocumentElement();
			Object object = customization.getOverviewSectionTitleObject();
			if(object instanceof ICustomLabelObject) {
				ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
				if(element != null) {
					overviewSectionTitle = customLabelObject.getLabel(element, resource);
				}
			} else {
				overviewSectionTitle = customization.getOverviewSectionTitle();
				// Apply distinguishers
				if(overviewSectionTitle != null && overviewSectionTitle.indexOf('$') != -1) {
					if(element != null) {
						overviewSectionTitle = ModelUtil.processLabelDistinguishers(element, overviewSectionTitle);
					}
				}
			}
		}
		if(overviewSectionTitle == null) {
			overviewSectionTitle = Messages.LABEL_OVERVIEW;
		}
		if(!overviewSectionTitle.equals(overviewSection.getText())) {
			overviewSection.setText(overviewSectionTitle);
			overviewSection.layout();
		}
	}
	
	private void updateOverviewSectionHeaderText() {
		if(customization != null) {
			String overviewSectionDescription= null;
			Document document = (Document)input;
			Element element = document.getDocumentElement();
			Object object = customization.getOverviewSectionDescriptionObject();
			if(object instanceof ICustomLabelObject) {
				ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
				if(element != null) {
					overviewSectionDescription = customLabelObject.getLabel(element, resource);
				}
			} else {
				overviewSectionDescription = customization.getOverviewSectionDescription();
				// Apply distinguishers
				if(overviewSectionDescription != null && overviewSectionDescription.indexOf('$') != -1) {
					if(element != null) {
						overviewSectionDescription = ModelUtil.processLabelDistinguishers(element, overviewSectionDescription);
					}
				}
			}
			GridData gridData = (GridData)overviewSectionDescriptionText.getLayoutData();
			if(overviewSectionDescription != null) {
				if(gridData.exclude) {
					overviewSectionDescriptionText.setVisible(true);
					gridData.exclude = false;
					((GridLayout)treeComposite.getLayout()).marginTop = 9;
					overviewSection.layout();
				}
				if(!overviewSectionDescription.equals(overviewSectionDescriptionText.getText())) {
					overviewSectionDescriptionText.setText(overviewSectionDescription);
					overviewSection.layout();
				}
			} else {
				if(!gridData.exclude) {
					((GridData)overviewSectionDescriptionText.getLayoutData()).exclude = true;
					overviewSectionDescriptionText.setVisible(false);
					((GridLayout)treeComposite.getLayout()).marginTop = 3;
					overviewSection.layout();
				}
			}
		}
	}

	public void setExpandSectionsToolItemEnabled(boolean enabled) {
		expandSectionsToolItem.setEnabled(enabled);
	}
	
	public ValidationManager getValidationManager() {
		return validationManager;
	}
	
	public void updateValidationInformation() {
		// Run validation
		
		int messageType = 0;
		String newMessage = null;
		
		// Check for errors	
		int messageCount = validationManager.getMessageManager().getDocumentMessageCount(ValidationMessage.MESSAGE_TYPE_ERROR);
		if(messageCount > 0) {
			messageType = IMessageProvider.ERROR;
			if(messageCount == 1) {
				newMessage = Messages.ERROR_DETECTED;
			} else {
				newMessage = MessageFormat.format(Messages.ERRORS_DETECTED, new Object[] {new Integer(messageCount)});
			}
		} else if (messageCount == 0 && sourceViewContainsErrors) {
			// Check for Source errors
			messageType = IMessageProvider.ERROR;
			newMessage = Messages.SOURCE_ERROR_DETECTED;
			
		} else {
			// Check for warnings
			messageCount = validationManager.getMessageManager().getDocumentMessageCount(ValidationMessage.MESSAGE_TYPE_WARNING);
						
			if(messageCount > 0) {
				messageType = IMessageProvider.WARNING;
				if(messageCount == 1) {
					newMessage = Messages.WARNING_DETECTED;
				} else {
					newMessage = MessageFormat.format(Messages.WARNINGS_DETECTED, new String[] {Integer.toString(messageCount)});
				}
			}
		}
		
		// Update form header
		if(newMessage == null) {
			if(form.getMessage() != null) {
				form.setMessage(null);
			}
		} else {
			if(!newMessage.equals(form.getMessage())) {
				form.setMessage(newMessage, messageType);				
			}
		}

		if (treeViewer != null)
		{
			Tree tree = treeViewer.getTree();
			if (tree != null && !tree.isDisposed())
			{
				treeViewer.refresh(true);
				
				// Update decorators on detailsViewer
				detailsViewer.upadteDecorators();
			}
		}

	}
	
	public void setUndoManager(IStructuredTextUndoManager undoManager) {
		this.undoManager = undoManager;
		final CommandStack commandStack = undoManager.getCommandStack();
		commandStackListener = new CommandStackListener(){

			private Command lastRedoCommand;
			
			public void commandStackChanged(EventObject event) {
				Command executedCommand = commandStack.getMostRecentCommand();
				Command undoCommand = commandStack.getUndoCommand();
				Command RedoCommand = commandStack.getRedoCommand();
				if(!executedCommand.equals(undoCommand) || executedCommand.equals(lastRedoCommand)) {
					validationManager.validateDocument();
					refresh(true);
				}
				lastRedoCommand = RedoCommand;
			}
			
		};
		commandStack.addCommandStackListener(commandStackListener);
	}
	
	public void setValidationDirty() {
		if(!validationDirtyFlag) {
			validationManager.getMessageManager().clearMessages();
			updateValidationInformation();
			form.removeMessageHyperlinkListener(formMessageHyperlinkListener);
			form.setMessage(Messages.THE_DOCUMENT_HAS_BEEN_MODIFIED_OUTSIDE_THE_EDITOR, IMessageProvider.INFORMATION);
			validationDirtyFlag = true;
		}
	}
	
	public void updateValidation() {
			validationManager.validateDocument();
			form.addMessageHyperlinkListener(formMessageHyperlinkListener);
			updateValidationInformation();
			validationDirtyFlag = false;
	}

	public Customization getCustomization() {
		return customization;
	}
	
	public IEditorPart getEditorPart() {
		return editorPart;
	}
	
	public TabbedPropertySheetWidgetFactory getWidgetFactory() {
		return widgetFactory;
	}
	
	public DetailsViewer getDetailsViewer() {
		return detailsViewer;
	}

	public TreeLabelProvider getTreeLabelProvider() {
		return treeLabelProvider;
	}
	
	public IStructuredTextUndoManager getUndoManager() {
		return undoManager;
	}
	
	
	public void updateErrorsFromAnnotationModel(final IAnnotationModel model) {
		if(timerTask != null) {
			timerTask.cancel();
		}
		timerTask = new TimerTask() {
			public void run() {
				Node node = getSourceViewFirstNodeWithErrors(model);
				if(node != null) {
					firstSourceViewErrorNode = node;
					setSourceViewContainsErrors(true);
				} else {
					setSourceViewContainsErrors(false);
				}
			}
		};
		timer.schedule(timerTask, IDLE_DELAY);
	}
	
	private Node getSourceViewFirstNodeWithErrors(IAnnotationModel model) {
		Iterator annotationIterator = model.getAnnotationIterator();
		while (annotationIterator.hasNext()) {
			Annotation annotation = (Annotation)annotationIterator.next();
			String annotationType = annotation.getType();
			if(sseErrorAnnotation1.equals(annotationType) || sseErrorAnnotation2.equals(annotationType)) {
				if(annotation instanceof TemporaryAnnotation) {
					TemporaryAnnotation temporaryAnnotation = (TemporaryAnnotation)annotation;
					Position position = temporaryAnnotation.getPosition();
					IDOMModel domModel = ((IDOMNode)input).getModel();
					IndexedRegion indexedregion = domModel.getIndexedRegion(position.offset);
					if (indexedregion != null) {
						return (Node)indexedregion;
					}
				}
			}
		}
		return null;
	}

	public void setSourceViewContainsErrors(boolean sourceViewContainsErrors) {
		if(this.sourceViewContainsErrors != sourceViewContainsErrors) {
			if(!this.detailsViewer.getIsEditingTableContent()) {
				this.sourceViewContainsErrors = sourceViewContainsErrors;
				if(designViewActive) {
					getControl().getDisplay().syncExec(new Runnable(){
						public void run() {
							updateValidation();
							updateValidationInformation();
						}
					});
				}
			}
		}
	}
	
	public boolean getSourceViewContainsErrors() {
		return sourceViewContainsErrors;
	}

	public Node getFirstSourceViewErrorNode() {
		return firstSourceViewErrorNode;
	}
	
	public boolean isValidationDirty() {
		return validationDirtyFlag;
	}
	
	public void performDelete() {
		TreeItem[] treeItems = treeViewer.getTree().getSelection();
		int selectedItemCount = treeItems.length;
		if(selectedItemCount == 1) {
			Object object = treeItems[0].getData();
			if(object instanceof Element) {
				Element element = (Element)object;
				Action deleteAction = ModelUtil.getDeletionAction(element, DDEViewer.this, customization, editorPart);
				deleteAction.run();
			}
		} else if(selectedItemCount > 1) {
			fireSelections = false;
			undoManager.beginRecording(this, Messages.REMOVE_ALL_SELECTED_ITEMS);
			List elementList = new ArrayList();
			for (int i = 0; i < treeItems.length; i++) {
				Object object = treeItems[i].getData();
				if(object instanceof Element) {
					elementList.add(object);
				}
			}
			ICustomMultipleDeletionObject customMultipleDeletionObject = customization != null ? customization.getCustomMultipleDeletionObject() : null;
			int response = 1;
			if(customMultipleDeletionObject != null) {
				Element[] elements = (Element[]) elementList.toArray(new Element[elementList.size()]);
				response = customMultipleDeletionObject.multipleDelete(elements, editorPart);
			}
			if(response == 1) {
				boolean confirmation = MessageDialog.openQuestion(DDEViewer.this.getControl().getShell(), Messages.LABEL_REMOVE, Messages.REMOVE_ALL_SELECTED_ITEMS_QUESTION);
				if(!confirmation) {
					response = -1;
				}
			}

			if(response > -1) {
				Iterator iterator = elementList.iterator();
				while(iterator.hasNext()) {
					Object object = iterator.next();
					if(object instanceof Element) {
						Element currentElement = (Element)object;
						if(ModelUtil.isNodeInDocument((Document)input, currentElement)) {
							Action deleteAction = ModelUtil.getDeletionAction(currentElement, DDEViewer.this, customization, editorPart);
							deleteAction.run();	
						}
					}
				}
			}
			undoManager.endRecording(this);
			fireSelections = true;
			
		}
	}
	
	
	public boolean isReadOnlyMode() {
		return readOnlyMode;
	}

	public void dispose() {
		if (undoManager != null && commandStackListener != null) {
			CommandStack commandStack = undoManager.getCommandStack();
			commandStack.removeCommandStackListener(commandStackListener);
		}
		detailsViewer.dispose();
		input = null;
	}
	
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		treeViewer.addPostSelectionChangedListener(listener);
		detailsViewer.addPostSelectionChangedListener(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
	    if (treeViewer != null)
		  treeViewer.removePostSelectionChangedListener(listener);
	    if (detailsViewer != null)
		  detailsViewer.removePostSelectionChangedListener(listener);
	}
	
	public void triggerAddButton() {
		TreeItem treeItem = treeViewer.getTree().getSelection()[0];
		Element element = (Element)treeItem.getData();
		List insertActions = ModelUtil.getInsertActions(element, customization, DDEViewer.this, editorPart);
		//Get the customization Class
		Class addButtonClass = customization.getAddButtonClass();
		
		//If the cutomization class is specified and of the right type, instantiate it and give it control to create the custom Add dialog
		if(addButtonClass != null) {
			Object object = null;
			try {
				object = addButtonClass.newInstance();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			}
			ICustomElementListSelectionDialog customElementListSelectionDialog = (ICustomElementListSelectionDialog)object;
			customElementListSelectionDialog.invoke(element);
		} else {
			
			ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(getControl().getShell(), new ILabelProvider(){

				public Image getImage(Object element) {
					AddElementAction action = (AddElementAction)element;
					return action.getImage();
				}

				public String getText(Object element) {
					Action action = (Action)element;
					return action.getText();
				}

				public void addListener(ILabelProviderListener listener) {
				}

				public void dispose() {
				}

				public boolean isLabelProperty(Object element, String property) {
					return false;
				}

				public void removeListener(ILabelProviderListener listener) {
				}

			});
			elementListSelectionDialog.setTitle(Messages.ADD_ITEM);
			elementListSelectionDialog.setIgnoreCase(true);
			elementListSelectionDialog.setElements(insertActions.toArray());
			elementListSelectionDialog.setHelpAvailable(false);
			String message = MessageFormat.format(Messages.ADD_ITEM_MESSAGE, new Object[] {treeItem.getText()});
			elementListSelectionDialog.setMessage(message);
			if (elementListSelectionDialog.open() == ElementListSelectionDialog.OK) {
				Object result = elementListSelectionDialog.getFirstResult();
				if(result instanceof Action) {
					Action action = (Action)result;
					action.run();
				}
			}
		}
	}
}
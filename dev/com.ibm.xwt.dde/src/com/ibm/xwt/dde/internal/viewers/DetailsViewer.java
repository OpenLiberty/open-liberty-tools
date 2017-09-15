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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.derived.HTMLTextPresenter;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;
import com.ibm.xwt.dde.customization.ICustomControlObject;
import com.ibm.xwt.dde.customization.ICustomControlObject2;
import com.ibm.xwt.dde.customization.ICustomControlObject3;
import com.ibm.xwt.dde.customization.ICustomDisableObject;
import com.ibm.xwt.dde.customization.ICustomHyperlinkObject;
import com.ibm.xwt.dde.customization.ICustomPossibleValuesObject;
import com.ibm.xwt.dde.customization.ICustomShouldItemDisableObject;
import com.ibm.xwt.dde.customization.ICustomSuggestedValuesObject;
import com.ibm.xwt.dde.customization.ICustomizationObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.controls.AbstractControl;
import com.ibm.xwt.dde.internal.controls.CustomSection;
import com.ibm.xwt.dde.internal.controls.HyperLink;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.data.AtomicAttributeDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicElementDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.RepeatableAtomicDetailItemSet;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.IValidateNotifier;
import com.ibm.xwt.dde.internal.validation.ValidationManager;
import com.ibm.xwt.dde.internal.viewers.DDEViewer.TreeFilterProcessor;

public class DetailsViewer extends Viewer implements IPostSelectionProvider {
	
	private final static String CONTROL_DATA_DETAIL_ITEM = "CONTROL_DATA_DETAIL_ITEM"; //$NON-NLS-1$
	private final static String CONTROL_DATA_CUSTOM_CODE = "CONTROL_DATA_CUSTOM_CODE"; //$NON-NLS-1$
	private final static String CONTROL_DATA_ASSOCIATED_CONTROL = "CONTROL_DATA_ASSOCIATED_CONTROL"; //$NON-NLS-1$
	private final static String CONTROL_DATA_POSSIBLE_VALUES = "CONTROL_DATA_POSSIBLE_VALUES"; //$NON-NLS-1$
	private final static String CONTROL_DATA_COMBO_POSSIBLE_VALUES = "CONTROL_DATA_COMBO_POSSIBLE_VALUES"; //$NON-NLS-1$
	private final static String CONTROL_DATA_COMBO_SUGGESTED_VALUES = "CONTROL_DATA_COMBO_SUGGESTED_VALUES"; //$NON-NLS-1$
	private final static String CONTROL_DATA_ASSOCIATED_LABEL = "CONTROL_DATA_ASSOCIATED_LABEL"; //$NON-NLS-1$
	private final static String CONTROL_DATA_DECORATOR = "CONTROL_DATA_DECORATOR"; //$NON-NLS-1$
	private final static String CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION = "CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION"; //$NON-NLS-1$
	private final static String CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM = "CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM"; //$NON-NLS-1$
	
	private final static String TABLE_BUTTON_ADD = "TABLE_BUTTON_ADD"; //$NON-NLS-1$
	private final static String TABLE_BUTTON_REMOVE = "TABLE_BUTTON_REMOVE"; //$NON-NLS-1$
	private final static String TABLE_BUTTON_MOVEUP = "TABLE_BUTTON_MOVEUP"; //$NON-NLS-1$
	private final static String TABLE_BUTTON_MOVEDOWN = "TABLE_BUTTON_MOVEDOWN"; //$NON-NLS-1$

	private Object input;
	private Composite detailsViewerComposite;
	private ScrolledComposite detailsComposite;
	private ScrolledComposite scrolledComposite;
	private DetailsContentProvider contentProvider;
	private TabbedPropertySheetWidgetFactory widgetFactory;  
	private InternalControlListener internalControlListener;
	private CustomControlListener customControlListener;
	private Composite controlsComposite;
	private boolean expandSections;
	private ISelection hiddenSelection;
	private Customization customization;
	private TreeFilterProcessor treeFilterProcessor;
	private IEditorPart editorPart;
	private DDEViewer ddeViewer;
	private List<Control> controlList;
	private Control currentControl;
	private DefaultInformationControl defaultInformationControl;
	private boolean isEditingTableContent;
	boolean globalDetectSchemaLabel;
	private ListenerList postSelectionChangedListeners = new ListenerList();
	private boolean readOnlyMode = false;
	//Used for indicating if to enable sections expand/collapse toolbar button
	private boolean sectionsPresent = false;
	//Used to store custom control class instances
	private List<ICustomControlObject> customControlInstances = new ArrayList<ICustomControlObject>();

	
	public DetailsViewer(Composite parent, TabbedPropertySheetWidgetFactory widgetFactory, IEditorPart editorPart, Customization customization, TreeFilterProcessor treeFilterProcessor, DDEViewer ddeViewer) {
		this.widgetFactory = widgetFactory;    
		this.customization = customization;
		this.treeFilterProcessor = treeFilterProcessor;
		this.editorPart = editorPart;
		this.ddeViewer = ddeViewer;
		controlList = new ArrayList<Control>();
		internalControlListener = new InternalControlListener(this);
		customControlListener = new CustomControlListener(this);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 0;
		gridLayout.marginLeft = 1;
		gridLayout.marginRight = 1;
		detailsViewerComposite =  widgetFactory.createComposite(parent);
		if (ddeViewer!=null)
			readOnlyMode = ddeViewer.isReadOnlyMode();
		if (ddeViewer==null)
			detailsViewerComposite.setBackground(parent.getBackground());
		detailsViewerComposite.setLayout(gridLayout);
		defaultInformationControl = new DefaultInformationControl(DetailsViewer.this.getControl().getShell(), new HTMLTextPresenter(false));
		if(customization!= null)
			globalDetectSchemaLabel = customization.getGlobalDetectSchemaLabel();
		if(DDEPlugin.getDefault().getPreferenceStore().getBoolean(DDEPlugin.PREFERENCES_EXPAND_SECTIONS)) {
			expandSections = true;
		} else {
			expandSections = false;
		}
		detailsViewerComposite.addDisposeListener(new DisposeListener(){

			public void widgetDisposed(DisposeEvent e) {
				defaultInformationControl.dispose();
			}
		});
	}
	
	
	public Control getControl()	{
		return detailsViewerComposite;
	}

	
	public Object getInput() {
		return input;
	}

	
	public ISelection getSelection() {
		if(detailsComposite != null) {
			Stack controls = new Stack();
			controls.addAll(Arrays.asList(detailsComposite.getChildren()));
			while(!controls.empty()) {
				Control control = (Control)controls.pop();
				if(control instanceof Composite && !(control instanceof Table)) {
					Composite composite = (Composite)control;
					controls.addAll(Arrays.asList(composite.getChildren()));
				} else if(control.isFocusControl()) {
					Object object = control.getData(CONTROL_DATA_DETAIL_ITEM);
					if(object instanceof AtomicDetailItem) {
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
						Node node = atomicDetailItem.getNode();
						if(node != null) {
							ISelection selection = new StructuredSelection(node);
							return selection;
						}
					}
				}
			}
		}
		return hiddenSelection;
	}

	
	public void refresh() {
		// Refresh the viewer by reseting its input
		Object input = this.input;
		this.input = null;
		setInput(input);
	}


	public boolean getExpandSections() {
		return expandSections;
	}
	
	public void setExpandSections(boolean expandSections) {
		this.expandSections = expandSections;
		Stack controls = new Stack();
		controls.addAll(Arrays.asList(detailsComposite.getChildren()));
		while(!controls.empty()) {
			Control control = (Control)controls.pop();
			if(control instanceof Composite) {
				Composite composite = (Composite)control;
				controls.addAll(Arrays.asList(composite.getChildren()));
			}
			if(control instanceof Section) {
				Section section = (Section)control;
				section.setExpanded(expandSections);
			}
		}
	}

	
	public void setContentProvider(DetailsContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	
	public DetailsContentProvider getContentProvider() {
		return contentProvider;
	}
	
	
	public void setInput(Object input) { 
		if(this.input != input) {
			if (contentProvider == null) {
				return;
			}
			this.input = input;
			if (detailsComposite != null) {
				detailsComposite.dispose();
			}
			if (ddeViewer!=null)
				readOnlyMode = ddeViewer.isReadOnlyMode();
			// Each time there is a new input to the details viewer (ie. by selecting a different node in the tree view
			// the hiddenSelection should be nulled.
			hiddenSelection = null;
			detailsComposite = new ScrolledComposite(detailsViewerComposite, SWT.FLAT );
			detailsComposite.setExpandHorizontal(true);
			detailsComposite.setExpandVertical(true);
			if (ddeViewer != null)
				detailsComposite.setBackground(widgetFactory.getColors().getBackground());
			else
				detailsComposite.setBackground(detailsViewerComposite.getBackground());
			GridLayout gridLayout = new GridLayout();
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 0;
			detailsComposite.setLayout(gridLayout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			gridData.grabExcessHorizontalSpace = true;
			detailsComposite.setLayoutData(gridData);
			
	        Composite content = new Composite(detailsComposite, SWT.FLAT);
	        if (ddeViewer != null)
	        	content.setBackground(widgetFactory.getColors().getBackground());
	        else
	        	content.setBackground(detailsViewerComposite.getBackground());
            
	        GridLayout gridLayout2 = new GridLayout();
	        gridLayout2.marginWidth = 0;
	        gridLayout2.marginHeight = 0;
	        content.setLayout(gridLayout2);
	        GridData gridData2 = new GridData(GridData.FILL_BOTH);
	        gridData2.grabExcessHorizontalSpace = true;
	        content.setLayoutData(gridData2);
	        detailsComposite.setContent(content);
	        
	        createContent(content, input);
			detailsViewerComposite.layout(true);
			upadteDecorators();
		}
	}

	
	private void createCustomControl(DetailItemCustomization itemNodeCustomization, Class customCtrlClass, DetailItem item, Composite currentComposite)
	{
		try {
			Object object = customCtrlClass.newInstance();
			String itemName = item.getName();
			if(object instanceof ICustomControlObject) {
				ICustomControlObject customControlObject = (ICustomControlObject)object;
				// Should be a DetailItem, and not Control.  If there are exceptions,
				// then this is an issue with the Editor customization.
			    CreateLinkOrLabel(item,itemNodeCustomization,currentComposite);
				
				//Give control 
				if(input instanceof Element) {
					if (object instanceof ICustomControlObject3)
					((ICustomControlObject3)object).createCustomControl((Element)input, itemName, 
							currentComposite, editorPart, customControlListener, readOnlyMode);
					
					else if (object instanceof ICustomControlObject2)
						((ICustomControlObject2)object).createCustomControl((Element)input, itemName, 
								currentComposite, editorPart, internalControlListener, readOnlyMode);
					
					else if (object instanceof ICustomControlObject)
						customControlObject.createCustomControl((Element)input, itemName, currentComposite, editorPart, internalControlListener);
					
				    customControlInstances.add(customControlObject);
				    
					if (object instanceof ICustomControlObject3)
					{
						List<Control> controls = ((ICustomControlObject3)object).getControls();
						if (controls!=null)
						{
							for (Control control: controls)
							{
								control.setData(ICustomControlObject3.CUSTOM_CONTROL_DATA_DETAIL_ITEM, item);
								controlList.add(control);
							}
						}
					}
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void createControls(Object item, Composite currentComposite, Composite viewerComposite, int detailsSortingPreference)
	{
		// Add custom controls to composite
		if(item instanceof AbstractControl) {
			AbstractControl abstractControl = (AbstractControl)item;
			addCustomControlToComposite(currentComposite, abstractControl);
		} else if(item instanceof AtomicDetailItem) { // Add atomic detail items to the controls composite
			AtomicDetailItem atomicDetailItem = (AtomicDetailItem)item;
			AddAtomicDetailItemToComposite(atomicDetailItem, currentComposite, null);
		} else if (item instanceof SimpleDetailItem) { // Add simple items (at the end)
			SimpleDetailItem simpleDetailItem = (SimpleDetailItem)item;
			DetailItemCustomization detailItemCustomization = simpleDetailItem.getDetailItemCustomization();
			if(detailItemCustomization != null) {
				sectionsPresent = !detailItemCustomization.isHideSectionTitle();
			} else {
				sectionsPresent = true;
			}
			Composite simpleDetailItemSectionComposite = createSimpleDetailItemSection(simpleDetailItem, viewerComposite, currentComposite);				
			DetailItem[] simpleDetailItemAtomicDetailItems = simpleDetailItem.getAtomicDetailItems();
			
			int localSchemaSortOption = -1;
			if (detailItemCustomization != null)
			{
			  localSchemaSortOption = detailItemCustomization.getDetailsSortingOption();
			}
			// Sort by schema if global setting is set or if local one is set
			boolean doSortSimpleDetailItems = ((detailsSortingPreference == Customization.DETAILS_SORTING_PREFERENCE_SCHEMA && localSchemaSortOption != Customization.DETAILS_SORTING_PREFERENCE_DEFAULT) ||
					localSchemaSortOption == Customization.DETAILS_SORTING_PREFERENCE_SCHEMA);
			
			AbstractControl[] simpleDetailItemCustomControls = getCustomControls(detailItemCustomization);
			Object[] simpleDetailItemItems = obtainSortedMergedItems(simpleDetailItemAtomicDetailItems, simpleDetailItemCustomControls, detailItemCustomization, doSortSimpleDetailItems);
			
			// Bug.  The input selection element and this child element can have different customizations.  If the input has the sort
			// by schema turned off, and this child element has it turned on, then at this point, the simpleDetailItemItems array 
			// is sorted by 'default'.  We now need to sort by schema, and only in this specific locally defined case:
			if (detailsSortingPreference <= Customization.DETAILS_SORTING_PREFERENCE_DEFAULT && localSchemaSortOption == Customization.DETAILS_SORTING_PREFERENCE_SCHEMA)
			{
				List simpleList = new ArrayList();
				simpleList.addAll(Arrays.asList(simpleDetailItemItems));
				simpleDetailItemItems = contentProvider.sortItemsBySchema(simpleDetailItem.getElement(), simpleList);
			}
			
			for(int j = 0; j < simpleDetailItemItems.length; j++) {
				// Add atomic and repeatable items to simple item section
				if(simpleDetailItemItems[j] instanceof RepeatableAtomicDetailItemSet) {
					RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)simpleDetailItemItems[j];
					createAtomicDetailItemTable(repeatableAtomicItem, simpleDetailItemSectionComposite, simpleDetailItem); 
				} else if(simpleDetailItemItems[j] instanceof AtomicDetailItem){
					AddAtomicDetailItemToComposite((AtomicDetailItem)simpleDetailItemItems[j], simpleDetailItemSectionComposite, simpleDetailItem);
				} else if(simpleDetailItemItems[j] instanceof AbstractControl) {
					AbstractControl abstractControl = (AbstractControl)simpleDetailItemItems[j];
					addCustomControlToComposite(simpleDetailItemSectionComposite, abstractControl);
				}
			}
		} else if (item instanceof RepeatableAtomicDetailItemSet) { // Add repeatable items to controls composite
			RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)item;
			createAtomicDetailItemTable(repeatableAtomicItem, currentComposite, null);
		}
	}
	
	private void createContent(Composite composite, Object input) {
		
		// If the input is a message, display the message
		if(input instanceof String) {
			String text = (String)input;
			Composite messageComposite = widgetFactory.createComposite(composite);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 2;
			messageComposite.setLayout(gridLayout);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalAlignment = SWT.CENTER;
			gridData.verticalIndent = 25;
			messageComposite.setLayoutData(gridData);
			Label imageLabel = widgetFactory.createLabel(messageComposite, null);
			imageLabel.setImage(DDEPlugin.getDefault().getImage("icons/information.gif"));
			widgetFactory.createLabel(messageComposite, text);
			return;
		}

		// Obtain input customization
		DetailItemCustomization inputNodeCustomization = getInputNodeCustomization(input);

		// Create (when applicable) the section header title
		boolean headerTextPresent = createSectionHeaderText(inputNodeCustomization, composite);

		// Create (when applicable) available children section
		if(input != null && customization != null && customization.isAddChildHelperEnabled()) {
			createChildElementHelper(composite, customization.getAddChildHelperLimit());
		}

		int detailsSortingPreference = -1;
        if(customization != null) {
            detailsSortingPreference = customization.getDetailsSortingPreference();
        }
        int detailsSortingOption = -1;
        if (inputNodeCustomization != null)
        {
        	detailsSortingOption = inputNodeCustomization.getDetailsSortingOption();
        }

        boolean doSchemaSort = ((detailsSortingPreference == Customization.DETAILS_SORTING_PREFERENCE_SCHEMA && detailsSortingOption != Customization.DETAILS_SORTING_PREFERENCE_DEFAULT) ||
        	detailsSortingOption == Customization.DETAILS_SORTING_PREFERENCE_SCHEMA);
        contentProvider.setSchemaSort(doSchemaSort);

		// Obtain detail items
		DetailItem[] detailItems = contentProvider.getItems(input);  
		
		// Obtain customControls
		AbstractControl[] customControls = getCustomControls(inputNodeCustomization);
		
		// Obtain sorted merged items
		Object[] items = obtainSortedMergedItems(detailItems, customControls, inputNodeCustomization, doSchemaSort);
		
		// Obtain custom sections
		Stack customSections = getCustomSectionsStack(inputNodeCustomization);

		// Clear control list
		controlList.clear();
		
		// Create controls composite
		controlsComposite = widgetFactory.createComposite(composite, SWT.FLAT);
		if (ddeViewer != null)
			controlsComposite.setBackground(widgetFactory.getColors().getBackground());
		else
			controlsComposite.setBackground(detailsViewerComposite.getBackground());
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginRight = 1;
		gridLayout.marginLeft = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = headerTextPresent? 6 : 2;
		gridLayout.marginBottom = 11;
		controlsComposite.setLayout(gridLayout);
		controlsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite currentComposite = controlsComposite;
		int currentItemOrderInCustomization = -1;
		int currentCustomSectionEnd = -1;
		
		boolean isSchemaGroupsEnabled = false;
		if (customization!=null)
			isSchemaGroupsEnabled = customization.getEnableSchemaGroups();
		if (items.length == 0 && input instanceof Element && customization != null &&
				customization.getEmptyElementCustomControlObject() != null) {
			// If this is an empty element and there is a class for dealing with empty elements then use that
			// Create a new composite with just 1 column
			try {
				Composite emptyElementComposite = widgetFactory.createComposite(controlsComposite, SWT.FLAT);
				emptyElementComposite.setBackground(composite.getBackground());
				gridLayout = new GridLayout();
				gridLayout.marginRight = 0;
				gridLayout.marginLeft = 0;
				gridLayout.marginWidth = 0;
				gridLayout.marginHeight = 0;
				gridLayout.marginTop = 0;
				gridLayout.marginBottom = 0;
				emptyElementComposite.setLayout(gridLayout);
				emptyElementComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				ICustomControlObject3 customControlObj = customization.getEmptyElementCustomControlObject();
				customControlObj.createCustomControl((Element)input, null, 
						emptyElementComposite, editorPart, customControlListener, readOnlyMode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (!isSchemaGroupsEnabled)
		{
			// Create controls
			for (int i = 0; i < items.length; i++) {
	
				// Obtain the customized order of the current item
				currentItemOrderInCustomization = getCurrentItemOrderInCustomization(items[i]);
				
				// Check if a custom section is starting / ending
				if(currentCustomSectionEnd != -1 && (currentItemOrderInCustomization >= currentCustomSectionEnd || currentItemOrderInCustomization == -1)) {
					currentComposite = controlsComposite;
					currentCustomSectionEnd = -1;
				}
				if(currentCustomSectionEnd == -1) {
					if(!customSections.isEmpty()) {
						CustomSection customSection = (CustomSection)customSections.peek();
						if(currentItemOrderInCustomization >= customSection.getOrder()) {
							currentCustomSectionEnd = customSection.getEnding();
							currentComposite = createCustomSection(composite, customSection);
							customSections.pop();
							sectionsPresent = true;
						}
					}
				}
				//If there is a customization defined for custom controls on this item, use it, if not use the default way for create contents.		
				//Class customControlClass = (inputNodeCustomization != null?inputNodeCustomization.getCustomControlClass():null);
				DetailItemCustomization itemNodeCustomization = null;
				String itemName = null;
				if(items[i] instanceof DetailItem)
				{	
					itemNodeCustomization =((DetailItem)items[i]).getDetailItemCustomization();
					itemName = ((DetailItem)items[i]).getName();
				}
				Class itemCustomControlClass = (itemNodeCustomization != null?itemNodeCustomization.getCustomControlClass():null);
				if(itemCustomControlClass!= null)
				{
					createCustomControl(itemNodeCustomization, itemCustomControlClass, (DetailItem)items[i], currentComposite);
				}
				else{
					createControls(items[i], currentComposite, composite, detailsSortingPreference);
				}
			}
		}
		
		else
		{
			//Set up grouping information
			HashMap<String, Element> groupsMap = new HashMap<String, Element>();
			Element einput = (Element)input;
			if (einput==null)
				return;
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(einput.getOwnerDocument());
			if (modelQuery != null) {
				CMElementDeclaration elementDeclaration = modelQuery.getCMElementDeclaration(einput);
				groupsMap = ModelUtil.getGroupDeclFromType(ModelUtil.getTypeDefinitionFromSchema(elementDeclaration));				
			}
			
			LinkedHashMap<String, ArrayList<DetailItem>> groupItemsMap = new LinkedHashMap<String, ArrayList<DetailItem>>();
			for (int i=0; i<detailItems.length; i++)
			{
				DetailItem item = detailItems[i];
				if (item instanceof AtomicDetailItem)
				{
					AtomicDetailItem aitem = ((AtomicDetailItem)item);
					String groupID = ModelUtil.getGroupIDFromSchema(aitem.getCMNode());
					if (groupID!=null && groupsMap.containsKey(groupID))
					{
						ArrayList<DetailItem> detailItemsGroup = groupItemsMap.get(groupID);
						if (detailItemsGroup==null)
						{
							detailItemsGroup = new ArrayList<DetailItem>();
							groupItemsMap.put(groupID, detailItemsGroup);
						}					
						detailItemsGroup.add(item);
					}
				}
			}
			
			// Create non grouped controls
			for (int i = 0; i < items.length; i++) {
	
				// Obtain the customized order of the current item
				currentItemOrderInCustomization = getCurrentItemOrderInCustomization(items[i]);
		
				
				// Check if a custom section is starting / ending
				if(currentCustomSectionEnd != -1 && (currentItemOrderInCustomization >= currentCustomSectionEnd || currentItemOrderInCustomization == -1)) {
					currentComposite = controlsComposite;
					currentCustomSectionEnd = -1;
				}
				if(currentCustomSectionEnd == -1) {
					if(!customSections.isEmpty()) {
						CustomSection customSection = (CustomSection)customSections.peek();
						if(currentItemOrderInCustomization >= customSection.getOrder()) {
							currentCustomSectionEnd = customSection.getEnding();
							currentComposite = createCustomSection(composite, customSection);
							customSections.pop();
							sectionsPresent = true;
						}
					}
				}
				
				//Skip control when it is in a group
				//and when it is not in custom section
				boolean skipControl = false;
				for (String key: groupItemsMap.keySet())
				{
					ArrayList<DetailItem> groupItems = groupItemsMap.get(key);
					if (groupItems!=null && groupItems.contains(items[i]))
					{
						skipControl = true;
						if(currentCustomSectionEnd!=-1)
						{
							skipControl = false;
							groupItems.remove(items[i]);
						}
						break;
					}
				}
				
				if (skipControl)
					continue;
				//If there is a customization defined for custom controls on this item, use it, if not use the default way for create contents.		
				//Class customControlClass = (inputNodeCustomization != null?inputNodeCustomization.getCustomControlClass():null);
				DetailItemCustomization itemNodeCustomization = null;
				String itemName = null;
				if(items[i] instanceof DetailItem)
				{	
					itemNodeCustomization =((DetailItem)items[i]).getDetailItemCustomization();
					itemName = ((DetailItem)items[i]).getName();
				}
				Class itemCustomControlClass = (itemNodeCustomization != null?itemNodeCustomization.getCustomControlClass():null);
				if(itemCustomControlClass!= null)
				{
					createCustomControl(itemNodeCustomization, itemCustomControlClass, (DetailItem) items[i], currentComposite);
				}
				else{
					createControls(items[i], currentComposite, composite, detailsSortingPreference);
				}
			}
			
			
			
			//Create Groups 
			HashMap<String, Composite> sectionsMap = new HashMap<String, Composite>(groupsMap.size());

			for (String key: groupItemsMap.keySet())
			{
				ArrayList<DetailItem> groupItems = groupItemsMap.get(key);
				Composite secInnerComp = null;
				for(DetailItem item: groupItems)
				{
					if (secInnerComp == null && !sectionsMap.containsKey(key))
					{	
						Element gDecl = groupsMap.get(key);
						Composite sComposite = createGroupSection(composite, gDecl.getAttribute("label"), gDecl.getTextContent());
						sectionsMap.put(key, sComposite);	
						secInnerComp = sComposite;

					}				
					
					DetailItemCustomization itemNodeCustomization = null;
					String itemName = null;
					if(item instanceof DetailItem)
					{	
						itemNodeCustomization =((DetailItem)item).getDetailItemCustomization();
						itemName = ((DetailItem)item).getName();
					}
					Class itemCustomControlClass = (itemNodeCustomization != null?itemNodeCustomization.getCustomControlClass():null);
					
					if(itemCustomControlClass!= null)
					{
						createCustomControl(itemNodeCustomization, itemCustomControlClass, item, secInnerComp);
					}
					else
					{
						createControls(item, secInnerComp, composite, detailsSortingPreference);
					}
				}
				secInnerComp = null;
			}
		}
		
		// If there are no controls in the controls composite, hide it
		if(controlsComposite.getChildren().length == 0) {
			GridData gridData = (GridData)controlsComposite.getLayoutData();
			gridData.exclude = true;
			controlsComposite.setSize(0, 0);
			composite.layout();
		}
		
		if (customControlInstances.size()>0) {
			detailsComposite.setMinWidth(currentComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
			// setMinWidth above calls layout
			for (ICustomControlObject customControl : customControlInstances)
			{
				if (customControl instanceof ICustomControlObject2)
				((ICustomControlObject2) customControl).postLayoutProcessing();
			}
			customControlInstances.clear();
		}
		// Enable/disable the section expansion button according to the presence of sections
		if (ddeViewer != null)
			ddeViewer.setExpandSectionsToolItemEnabled(sectionsPresent);
	}
	
	
	private int getCurrentItemOrderInCustomization(Object object) {
		if(object instanceof DetailItem) {
			DetailItem detailItem = (DetailItem)object;
			DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
			if(detailItemCustomization != null) {
				return detailItemCustomization.getOrder();
			}
		} else if(object instanceof AbstractControl) {
			AbstractControl abstractControl = (AbstractControl)object;
			return abstractControl.getOrder();
		}
		return -1;
	}
	
	private Object[] obtainSortedMergedItems(DetailItem[] detailItems, AbstractControl[] abstractControls, DetailItemCustomization inputNodeCustomization, boolean doSchemaSort) {
		Object[] items = new Object[detailItems.length + abstractControls.length];
		
		// Merge items and customControls
		for (int i = 0; i < detailItems.length; i++) {
			items[i] = detailItems[i];
		}
		for (int i = 0; i < abstractControls.length; i++) {
			items[i + detailItems.length] = abstractControls[i];
		}
		
		if (!doSchemaSort) {
	      // Sort detail items
		  sortDetailItems(items);
		}
		return items;
	}

	
	private AbstractControl[] getCustomControls(DetailItemCustomization detailItemCustomization) {
		AbstractControl[] customControls = null;
		if(detailItemCustomization != null) {
			customControls = detailItemCustomization.getCustomControls();
		}
		if(customControls == null) {
			customControls = new AbstractControl[0];
		}
		return customControls;
	}
	

	private void addCustomControlToComposite(Composite composite, AbstractControl customControl) {
		if(customControl instanceof HyperLink) {
			HyperLink hyperLink = (HyperLink)customControl;
			
			// Set the composite where the hyperlink will be located
			Composite hyperlinkComposite = new Composite(composite, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			hyperlinkComposite.setLayoutData(gridData);
			
			hyperlinkComposite.setBackground(widgetFactory.getColors().getBackground());
			
			GridLayout gridLayout = new GridLayout();
			gridLayout.marginHeight = 0;
			gridLayout.marginWidth = 0;
			gridLayout.marginLeft = hyperLink.isLeftIndentation()? 5 : 0;
			gridLayout.numColumns = 2;
			hyperlinkComposite.setLayout(gridLayout);
			
			boolean iconPresent = false;
			
			// Add the icon when applicable
			Image icon = hyperLink.getIcon();
			if(icon != null) {
				Label label = new Label(hyperlinkComposite, SWT.NONE);
				label.setImage(icon);
				label.setBackground(widgetFactory.getColors().getBackground());
				iconPresent = true;
			}
			
			// Add the actual link
			Link link = new Link(hyperlinkComposite, SWT.NONE);
			if(!iconPresent) {
				gridData = new GridData();
				gridData.horizontalSpan = 2;
				link.setLayoutData(gridData);
			}
			
			// Set the link label
			String label = hyperLink.getLabel();
			if(label == null) {
				label = "";
			}
			link.setText(label);
			link.setBackground(widgetFactory.getColors().getBackground());
			
			link.setEnabled(!readOnlyMode);
			
			// Set the link tooltip
			String toolTip = hyperLink.getTooltip();
			if(toolTip != null) {
				link.setToolTipText(toolTip);
			}
			
			final Class hyperlinkClass = hyperLink.getCustomCode();

			if(hyperlinkClass != null) {
				link.addSelectionListener(new SelectionListener(){

					public void widgetDefaultSelected(SelectionEvent e) {
					}

					public void widgetSelected(SelectionEvent e) {
						try {
							Object object = hyperlinkClass.newInstance();
							if(object instanceof ICustomHyperlinkObject) {
								ICustomHyperlinkObject customHyperlinkObject = (ICustomHyperlinkObject)object;
								
								Element nodeElement = null;
								if(input instanceof Element) {
									nodeElement = (Element)input;
								}
								Node result = customHyperlinkObject.hyperlink(nodeElement, editorPart);
								ddeViewer.setValidationDirty();
								ddeViewer.updateValidation();
								if(result != null) {
									ISelection selection = new StructuredSelection(result);
									ddeViewer.setSelection(selection, true);
								}
							}
						} catch (Exception exception) {exception.printStackTrace();}
					}
				});
			}
			
			
		}

	}

	
	private boolean createSectionHeaderText(DetailItemCustomization detailItemCustomization, Composite composite) {
		if(detailItemCustomization != null) {
			String sectionHeaderText = detailItemCustomization.getSectionHeaderText();
			if(sectionHeaderText != null) {
				sectionHeaderText = ModelUtil.formatHeaderOrFooterText(sectionHeaderText);
				Text sectionHeaderLabel = new Text(composite, SWT.MULTI | SWT.WRAP);
				sectionHeaderLabel.setText(sectionHeaderText);
				Color bg = sectionHeaderLabel.getBackground();
				sectionHeaderLabel.setBackground(bg);
				sectionHeaderLabel.setEditable(false);
				GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
				gridData.horizontalSpan = 3;
				sectionHeaderLabel.setLayoutData(gridData);
				return true;
			}
		}
		return false;
	}
	
	
	private void createChildElementHelper(Composite parentComposite, int limit) {

		// Obtain list of children
		List<?> insertActions = ModelUtil.getInsertActions((Element) input, customization, ddeViewer, editorPart);
		int childCount = insertActions.size();
		
		// Only create UI if there is at least one child element
		if(childCount > 0) {
			
			Iterator<?> iterator = insertActions.iterator();
			StringBuilder stringBuilder = new StringBuilder();
			int counter = 0;
			while(iterator.hasNext()) {
				Action action = (Action) iterator.next();
				stringBuilder.append(action.getText());
				if(counter++ == limit) {
					stringBuilder.append(MessageFormat.format(Messages.AVAILABLE_CHILD_ELEMENTS_MORE, childCount - limit));
					break;
				}
				if(iterator.hasNext()) {
					stringBuilder.append(", ");
				}
			}
			if(childCount <= limit) {
				stringBuilder.append('.');
			}
			
			// Containing composite
			final Composite composite = new Composite(parentComposite, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.verticalIndent = 4;
			composite.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			composite.setLayoutData(gridData);
			GridLayout gridLayout = new GridLayout(2, false);
			gridLayout.marginWidth = 12;
			gridLayout.marginTop = 4;
			gridLayout.marginBottom = 12;
			composite.setLayout(gridLayout);

			// Border
			composite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent paintEvent) {
		            Rectangle rectangle = composite.getBounds();
		            Rectangle outerBorder = new Rectangle(rectangle.x, rectangle.y - 4, rectangle.width - 1, rectangle.height - 1);
		            paintEvent.gc.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		            paintEvent.gc.drawRectangle(outerBorder);
		            Rectangle innerBorder = new Rectangle(rectangle.x + 1, rectangle.y - 3, rectangle.width - 3, rectangle.height - 3);
		            paintEvent.gc.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
					paintEvent.gc.drawRectangle(innerBorder);
				}
			});
			
			// Title
			Text titleText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
			FontDescriptor fontDescriptor = FontDescriptor.createFrom(titleText.getFont()).setStyle(SWT.BOLD);
			Font boldFont = fontDescriptor.createFont(titleText.getDisplay());
			titleText.setFont(boldFont);
			titleText.setText(MessageFormat.format(Messages.AVAILABLE_CHILD_ELEMENTS, insertActions.size()));
			titleText.setEditable(false);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 2;
			titleText.setLayoutData(gridData);

			// Content (list of children)
			Text contentText = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			contentText.setText(stringBuilder.toString());
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			contentText.setLayoutData(gridData);
			
			// Add button
			Button addButton = widgetFactory.createButton(composite, Messages.LABEL_ADD_DOTS, SWT.PUSH);
			addButton.addSelectionListener(new SelectionAdapter(){
				
				@Override
				public void widgetSelected(SelectionEvent selectionEvent) {
					ddeViewer.triggerAddButton();
				}
				
			});
			
			// Separator
			Composite separator = new Composite(parentComposite, SWT.NONE);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.heightHint = 4;
			separator.setLayoutData(gridData);
		}
	}
	
	
	
	private Stack getCustomSectionsStack(DetailItemCustomization detailItemCustomization) {
		Stack stack = new Stack();
		if(detailItemCustomization != null) {
			if(detailItemCustomization != null) {
				CustomSection[] customSections = detailItemCustomization.getCustomSections();
				if(customSections != null) {
					int i = customSections.length - 1;
					while(i >= 0) {
						stack.push(customSections[i--]);						
					}
				}
			}
		}
		return stack;
	}
	
	
	private DetailItemCustomization getInputNodeCustomization(Object input) {
		DetailItemCustomization detailItemCustomization = null;
		if(input instanceof Element) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(((Element)input).getOwnerDocument());
			CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(((Element)input));
			Element element = (Element)input;
			String path = ModelUtil.getElementFullPath(element);
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), path);
				//If no itemCustomization is specified, try the typeCustomization
				if(detailItemCustomization == null)
					detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration,path);
			}
		}
		return detailItemCustomization;
	}
	

	public void activate() {
		if(hiddenSelection != null) {
			setSelection(hiddenSelection, true);
		}
	}
	
	public void setSelection(String detailItemLocalPath) {
		if(detailItemLocalPath != null) {
			Iterator iterator = controlList.iterator();
			while(iterator.hasNext()) {
				Control control = (Control)iterator.next();
				DetailItem detailItem = (DetailItem)control.getData(CONTROL_DATA_DETAIL_ITEM);
				if(detailItem != null) {
					SimpleDetailItem containingSimpleDetailItem = (SimpleDetailItem)control.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
					String path = "";
					if(containingSimpleDetailItem != null) {
						path += containingSimpleDetailItem.getName() + '/';
					}
					path += detailItem.getName();
					if(detailItemLocalPath.equals(path)) {
						control.setFocus();
						if(control instanceof Table) {
							Table table = (Table)control;
							if(table.getItemCount() == 0) {
								Button addButton = (Button)table.getData(TABLE_BUTTON_ADD);
								addButton.setFocus();
							}
						}
						return;
					}
				}
			}
		}
	}
	
	public void setSelection(ISelection selection, boolean reveal)	{
		if(detailsComposite != null) {
			// Check if the selection is an attribute, reset it accordinly.
			if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				if (selection instanceof ITextSelection) {
					ITextSelection textSelection = (ITextSelection) selection;
					if (structuredSelection.size() == 1) {
						if (structuredSelection.getFirstElement() instanceof IDOMNode) {
							IDOMNode domNode = (IDOMNode) structuredSelection.getFirstElement();
							IStructuredDocumentRegion startStructuredDocumentRegion = domNode.getStartStructuredDocumentRegion();
							if (startStructuredDocumentRegion != null) {
								ITextRegion matchingRegion = startStructuredDocumentRegion.getRegionAtCharacterOffset(textSelection.getOffset());
								while (matchingRegion != null && !matchingRegion.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
									matchingRegion = startStructuredDocumentRegion.getRegionAtCharacterOffset(startStructuredDocumentRegion.getStartOffset(matchingRegion)-1);
								}
								if (matchingRegion != null && matchingRegion.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
									String attrName = startStructuredDocumentRegion.getText(matchingRegion);
									Node attr = domNode.getAttributes().getNamedItem(attrName);
									if (attr != null) {
										selection = new StructuredSelection(attr);
									}
								}
							}
						}
					}
				}
			}
			List selections = ((IStructuredSelection) selection).toList();
			if(selections.size() > 0) {
				Object object = selections.get(0);
				if(object instanceof Node) {
					Node selectedNode = (Node)object;
					hiddenSelection = new StructuredSelection(selectedNode);
					Stack controls = new Stack();
					controls.addAll(Arrays.asList(detailsComposite.getChildren()));
					while(!controls.empty()) {
						Control control = (Control)controls.pop();
						if(control instanceof Composite && !(control instanceof Table) && !(control instanceof CCombo)) {
							Composite composite = (Composite)control;
							controls.addAll(Arrays.asList(composite.getChildren()));
						} else {
							object = control.getData(CONTROL_DATA_DETAIL_ITEM);
							if(object instanceof AtomicDetailItem) {
								AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
								Node node = atomicDetailItem.getNode();
								if(selectedNode.equals(node)) {
									if(reveal) {
										control.setFocus();
										control.setBackground(widgetFactory.getColors().getBackground());
									} else {
										control.setBackground(widgetFactory.getColors().getColor(IFormColors.H_GRADIENT_START));
										//control.setBackground(widgetFactory.getColors().getBackground());
										currentControl = control;
										scrolledComposite.showControl(control);

									}
								} else {
//									control.setBackground(widgetFactory.getColors().getBackground());
									control.setBackground(controlsComposite.getBackground());
								}
							} else if(object instanceof RepeatableAtomicDetailItemSet) {
								if(control instanceof Table)
								{
									Table table = (Table)control;
									TableItem tableItems[] = table.getItems();
									for (int i = 0; i < tableItems.length; i++) {
										if(((AtomicDetailItem)tableItems[i].getData()).getNode() == selectedNode) {
											if(reveal) {
												table.forceFocus();
											}
											table.setSelection(i);
											int itemCount = table.getItemCount();
											if(itemCount == 1) {
												((Button)table.getData(TABLE_BUTTON_MOVEUP)).setEnabled(false);
												((Button)table.getData(TABLE_BUTTON_MOVEDOWN)).setEnabled(false);
											} else if (i == 0) {
												((Button)table.getData(TABLE_BUTTON_MOVEUP)).setEnabled(false);
												((Button)table.getData(TABLE_BUTTON_MOVEDOWN)).setEnabled(true);
											} else if(i == itemCount -1) {
												((Button)table.getData(TABLE_BUTTON_MOVEUP)).setEnabled(true);
												((Button)table.getData(TABLE_BUTTON_MOVEDOWN)).setEnabled(false);
											} else {
												((Button)table.getData(TABLE_BUTTON_MOVEUP)).setEnabled(true);
												((Button)table.getData(TABLE_BUTTON_MOVEDOWN)).setEnabled(true);
											}
											((Button)table.getData(TABLE_BUTTON_REMOVE)).setEnabled(true);
											break;
										}
									}
								}
							}
						}
					}
				} else if(object instanceof String) {
					setSelection((String)object);
				}
			} 
		}
	}


	private void AddAtomicDetailItemToComposite(AtomicDetailItem atomicDetailItem, Composite composite, SimpleDetailItem containingSimpleDetailItem) {

		String labelText = null;
		Class buttonClass = null;
		String buttonToolTip = null;
		String buttonLabel = null;
		Image buttonIcon = null;
		Class linkClass = null;
		String linkToolTip = null;
		Map possibleValues = null;
		List suggestedValues = null;
		boolean detectSchemaLabel = false;
		boolean required = false;
		int numberOfTextLines = 1;
		int style = DetailItemCustomization.STYLE_DEFAULT;
		boolean readOnly = false;
		boolean hideLabel = false;
		boolean disabled = false;
		String headerText = null;
		String footerText = null;
		String checkBoxText = null;
		boolean horizontalScrolling = false;
		String helpContextId = null;
		boolean showItemAsOptional = false;
		String buttonAccessibilityName = null;
		char echoChar = 0;
		boolean wrapText = false;
		Class shouldItemDisableClass = null;
		
		// If the atomic detail item is customized, set its properties to those in the customization
		DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
		if(detailItemCustomization == null && customization!=null)
		{
			detailItemCustomization = customization.getTypeCustomizationConsideringUnions(atomicDetailItem.getCMNode(), ModelUtil.getElementFullPath(atomicDetailItem.getClosestAncestor()));
		}
		if(detailItemCustomization != null) {
			labelText = detailItemCustomization.getLabel();
			buttonClass = detailItemCustomization.getButtonClass();
			buttonLabel = detailItemCustomization.getButtonLabel();
			buttonIcon = detailItemCustomization.getButtonIcon();
			buttonToolTip = detailItemCustomization.getButtonToolTip();
			linkClass = detailItemCustomization.getLinkClass();
			linkToolTip = detailItemCustomization.getLabelLinkToolTip();
			possibleValues = detailItemCustomization.getPossibleValues();
			suggestedValues = detailItemCustomization.getSuggestedValues();
			required = detailItemCustomization.isRequired();
			numberOfTextLines = detailItemCustomization.getLines();
			readOnly = detailItemCustomization.isReadOnly();
			style = detailItemCustomization.getStyle();
			hideLabel = detailItemCustomization.isHideLabel();
			disabled = detailItemCustomization.isDisabled();
			headerText = detailItemCustomization.getHeaderText();
			footerText = detailItemCustomization.getFooterText();
			checkBoxText = detailItemCustomization.getCheckBoxText();
			horizontalScrolling = detailItemCustomization.isHorizontalScrolling();
			helpContextId = detailItemCustomization.getHelpContextId();
			showItemAsOptional = detailItemCustomization.isShowItemAsOptional();
			buttonAccessibilityName = detailItemCustomization.getButtonAccessibilityName();
			echoChar = detailItemCustomization.getEchoChar();
			wrapText = detailItemCustomization.isWrapText();
			shouldItemDisableClass = detailItemCustomization.getShouldItemDisableClass();
			detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();
			if(detailItemCustomization.getPossibleValuesClass() != null) {
				Object object = null;
				try {
					object = detailItemCustomization.getPossibleValuesClass().newInstance();
				} catch (Exception e) {e.printStackTrace();}
				if(object instanceof ICustomPossibleValuesObject) {
					ICustomPossibleValuesObject customPossibleValuesObject = (ICustomPossibleValuesObject)object;
					IEditorInput editorInput = editorPart.getEditorInput();
					IResource resource = null;
					if(editorInput != null) {
						resource = (IResource) editorInput.getAdapter(IResource.class);
					}
					possibleValues = customPossibleValuesObject.getPosibleValues(atomicDetailItem.getValue(), atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
				}
			}
            if(detailItemCustomization.getSuggestedValuesClass() != null) {
                Object object = null;
                try {
                        object = detailItemCustomization.getSuggestedValuesClass().newInstance();
                } catch (Exception e) {e.printStackTrace();}
                if(object instanceof ICustomSuggestedValuesObject) {
                        ICustomSuggestedValuesObject customSuggestedValuesObject = (ICustomSuggestedValuesObject)object;
                        IEditorInput editorInput = editorPart.getEditorInput();
                        IResource resource = null;
                        if(editorInput != null) {
                                resource = (IResource) editorInput.getAdapter(IResource.class);
                        }
                        suggestedValues = customSuggestedValuesObject.getSuggestedValues(atomicDetailItem.getValue(), atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
                }
            }
		}
		
		// Create (when applicable) the header text
		if(headerText != null) {
			headerText = ModelUtil.formatHeaderOrFooterText(headerText);
			addSeparator(composite);
			Composite headerTextComposite = new Composite(composite, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			headerTextComposite.setLayoutData(gridData);
			FillLayout fillLayout = new FillLayout();
			headerTextComposite.setLayout(fillLayout);
			Text headerTextControl = new Text(headerTextComposite, SWT.WRAP | SWT.READ_ONLY);
			
			headerTextControl.setBackground(widgetFactory.getColors().getBackground());
			headerTextControl.setText(headerText);
		}
		
		// Properties not customized are assigned their default values
		//Case where Label is not defined in the customization, we would look at the schema
		if(labelText == null && (detectSchemaLabel || globalDetectSchemaLabel))
		{
			labelText = ModelUtil.getLabel(atomicDetailItem.getCMNode());
		}
		//The case where the label is neither defined in the customization file nor in the schema file.
		if(labelText == null)
		{
			labelText = atomicDetailItem.getName();
		}
		if(!required) {
			if(!atomicDetailItem.isOptionalWithinContext()) {
				required = atomicDetailItem.isRequired();
			}
		}
		if(possibleValues == null) {
			possibleValues = atomicDetailItem.getPossibleValues();
		}
		
		if(style == DetailItemCustomization.STYLE_DEFAULT || style == DetailItemCustomization.STYLE_TREE_NODE) {
			if(possibleValues != null || suggestedValues != null) {
				style = DetailItemCustomization.STYLE_COMBO;
			} else if(atomicDetailItem.hasEditableValue()){
				style = DetailItemCustomization.STYLE_TEXT;
			} else {
				style = DetailItemCustomization.STYLE_CHECKBOX;
			}
		}
		
		Control control = null;
		GridData controlGridData = new GridData(GridData.FILL_HORIZONTAL);

		// Create a label (or link) for the item
		Control labelOrLinkControl = null;
		if(!hideLabel) {
			// Add ':' after the label preceded by '*' if the item is required
			if(!"".equals(labelText)) {
				if(required && style != DetailItemCustomization.STYLE_CHECKBOX && !showItemAsOptional) {
					labelText = MessageFormat.format(Messages.LABEL_ASTERISK, new String[] {labelText});
				}
				labelText = MessageFormat.format(Messages.LABEL_COLON, new String[] {labelText});
			}
			if(linkClass != null) {
				Hyperlink labelLink = widgetFactory.createHyperlink(composite, labelText, SWT.NONE);
				labelLink.setData(CONTROL_DATA_CUSTOM_CODE, linkClass);
				labelLink.addHyperlinkListener(internalControlListener);
				if(linkToolTip != null) {
					labelLink.setToolTipText(ModelUtil.formatToolTip(linkToolTip));
				}
				if(readOnlyMode) {
					labelLink.setEnabled(false);
				}
				labelOrLinkControl = labelLink;
			} else {
				Label label = widgetFactory.createLabel(composite, labelText);
				label.setForeground(widgetFactory.getColors().getColor(IFormColors.TITLE));
				labelOrLinkControl = label;
			}
			String filterText = null;
			if (treeFilterProcessor != null)
				filterText = treeFilterProcessor.getFilterText();
			if(filterText != null && !"".equals(filterText)) {
				if(labelText.toLowerCase().indexOf(filterText.toLowerCase()) != -1 || atomicDetailItem.getName().toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
					labelOrLinkControl.setFont(DDEPlugin.getDefault().FONT_DEFAULT_BOLD);
				}
			}
			labelOrLinkControl.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
			labelOrLinkControl.setData(CONTROL_DATA_DETAIL_ITEM, atomicDetailItem);
			labelOrLinkControl.addMouseTrackListener(internalControlListener);

		}
		// Create the control according to the style
		switch(style) {
		
		case DetailItemCustomization.STYLE_TEXT:
			Text text = null;
			// If the number of textlines is greater than 1, create a scrolling text area
			if(numberOfTextLines > 1) {
				// Set the label or link alignment
				if(labelOrLinkControl != null) {
					((GridData)labelOrLinkControl.getLayoutData()).verticalAlignment = GridData.BEGINNING;
				}
				String value = ModelUtil.formatMultiLineTextForEditing(atomicDetailItem.getValue());
				text = widgetFactory.createText(composite, value, SWT.MULTI | SWT.V_SCROLL | ((wrapText)? SWT.WRAP : SWT.NONE) | (horizontalScrolling? SWT.H_SCROLL : SWT.NONE));
				controlGridData.heightHint = 16 * numberOfTextLines;
				if(labelText != null && !"".equals(labelText)) {
					final String accessibleLabelText = labelText;
					text.getAccessible().addAccessibleListener(new AccessibleAdapter(){
						
						public void getName(AccessibleEvent accessibleEvent) {
							accessibleEvent.result = accessibleLabelText;
						}
						
					});
				}
			} else {
				String value = atomicDetailItem.getValue().trim();
				if(possibleValues != null) {
					String keys[] = (String[])possibleValues.keySet().toArray(new String[possibleValues.keySet().size()]);
					String values[] = (String[])possibleValues.values().toArray(new String[possibleValues.values().size()]);
					for (int i = 0; i < values.length; i++) {
						if(value.equals(values[i])) {
							value = keys[i];
							break;
						}
					}
				}
				text = widgetFactory.createText(composite, value);
			}
			if(readOnly) {
				text.setEditable(false);
			}
			text.addModifyListener(internalControlListener);
			if(echoChar != 0) {
				text.setEchoChar(echoChar);
			}
			control = text; 
			break;
			
		case DetailItemCustomization.STYLE_COMBO:
			CCombo combo = widgetFactory.createCCombo(composite);

			if (possibleValues != null) {
				Map comboPossibleValues = null;
				if(!required) {
					comboPossibleValues = new LinkedHashMap();
					comboPossibleValues.put("", "");
					comboPossibleValues.putAll(possibleValues);
				} else {
					comboPossibleValues = possibleValues;
				}
				combo.setData(CONTROL_DATA_COMBO_POSSIBLE_VALUES, comboPossibleValues);
				// Add the set of labels to the combo
				Set labels = comboPossibleValues.keySet();
				for (Iterator iterator = labels.iterator(); iterator.hasNext();) {
					String currentLabel = (String) iterator.next();
					combo.add(currentLabel);
				}
				// Set the combo text using the label that corresponds to the current value
				String value = atomicDetailItem.getValue();
				String keys[] = (String[])possibleValues.keySet().toArray(new String[possibleValues.keySet().size()]);
				String values[] = (String[])possibleValues.values().toArray(new String[possibleValues.values().size()]);
				for (int i = 0; i < values.length; i++) {
					if(value.equals(values[i])) {
						value = keys[i];
						break;
					}
				}
				combo.setText(value);
				
				combo.addSelectionListener(internalControlListener);
				control = combo;
				
			} else if (suggestedValues != null){
				List comboSuggestedValues = new LinkedList();
				comboSuggestedValues.add("");
				comboSuggestedValues.addAll(suggestedValues);
				combo.setData(CONTROL_DATA_COMBO_SUGGESTED_VALUES, comboSuggestedValues);
				for (Iterator iterator = comboSuggestedValues.iterator(); iterator.hasNext();) {
					String currentLabel = (String) iterator.next();
					combo.add(currentLabel);
				}
				String value = atomicDetailItem.getValue();
				combo.setText(value);
				
				combo.addSelectionListener(internalControlListener);
				combo.setEditable(true);
				combo.addModifyListener(internalControlListener);
				
				control = combo;
			}
			
			break;

		case DetailItemCustomization.STYLE_CHECKBOX:
			if((checkBoxText == null || checkBoxText.equals("")) && hideLabel && (detectSchemaLabel || globalDetectSchemaLabel))
				checkBoxText= labelText;
			Button button = widgetFactory.createButton(composite, checkBoxText, SWT.CHECK);
			
			button.addMouseTrackListener(internalControlListener);
			
			button.addSelectionListener(internalControlListener);

			if(possibleValues != null && possibleValues.size() > 1) {
				Object[] possibleValueKeys = possibleValues.keySet().toArray();
				String checkedValue = (String)possibleValues.get(possibleValueKeys[0]);
				if(atomicDetailItem.getValue().equals(checkedValue)) {
					button.setSelection(true);
				}
			} else if(atomicDetailItem.exists()) {
				button.setSelection(true);
			}
			button.setForeground(widgetFactory.getColors().getColor(IFormColors.TITLE));
			control = button;

			if(("".equals(checkBoxText) || checkBoxText == null) && (labelText != null) && !"".equals(labelText)) {
				final String checkboxAccessibilityText = labelText;
				button.getAccessible().addAccessibleListener(new AccessibleAdapter() {
					public void getName(AccessibleEvent accessibleEvent) {
						accessibleEvent.result = checkboxAccessibilityText;
					}
				});
			}
		}
		
		// If the atomic detail item contains a button class, create a button control for it
		if(buttonClass != null) {
//			// If no label has been specified for the button, set it to "..."
//			if(buttonLabel == null) {
//				buttonLabel = "..."; //$NON-NLS-1$
//			}
			Button button = widgetFactory.createButton(composite, buttonLabel, SWT.PUSH);
			if(buttonToolTip != null) {
				button.setToolTipText(buttonToolTip);
			}

			if(buttonAccessibilityName != null) {
				final String accessibiltyName = buttonAccessibilityName;
				button.getAccessible().addAccessibleListener(new AccessibleAdapter(){
					public void getName(AccessibleEvent e) {
						e.result = accessibiltyName;
					}
				});
			}
			
			if(buttonLabel == null) {
				button.setImage(DDEPlugin.getDefault().getImage("icons/dots.gif"));
				if(buttonAccessibilityName == null) {
					button.getAccessible().addAccessibleListener(new AccessibleAdapter(){
						public void getName(AccessibleEvent e) {
							e.result = Messages.CLICK_TO_SET;
						}
					});
				}
			}
			
			if(buttonIcon!=null)
			{
				button.setImage(buttonIcon);
			}
			
			button.setData(CONTROL_DATA_CUSTOM_CODE, buttonClass);
			button.setData(CONTROL_DATA_ASSOCIATED_CONTROL, control);
			button.setData(CONTROL_DATA_DETAIL_ITEM, atomicDetailItem);
			button.addSelectionListener(internalControlListener);
			GridData gridData = new GridData();
			gridData.horizontalAlignment = SWT.FILL;
			button.setLayoutData(gridData);
			if(readOnlyMode) {
				button.setEnabled(false);
			}
			
		} else {
			// If no button is specified, the control should span two columns
				controlGridData.horizontalSpan = 2;
		}
		
		// Create (when applicable) the footerText
		if(footerText != null) {
			footerText = ModelUtil.formatHeaderOrFooterText(footerText);
			Composite headerTextComposite = new Composite(composite, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			headerTextComposite.setLayoutData(gridData);
			FillLayout fillLayout = new FillLayout();
			headerTextComposite.setLayout(fillLayout);
			Text headerTextControl = new Text(headerTextComposite, SWT.WRAP | SWT.READ_ONLY);
			headerTextControl.setBackground(widgetFactory.getColors().getBackground());
			headerTextControl.setText(footerText);
		}
		
		if(disabled) {
			control.setEnabled(false);
		}

		if(hideLabel) {
			controlGridData.horizontalSpan++;
		}
		
		// Add focus listener
			control.addFocusListener(internalControlListener);
		
		// Associate the link with the control
		if(linkClass != null && labelOrLinkControl != null) {
			labelOrLinkControl.setData(CONTROL_DATA_ASSOCIATED_CONTROL, control);
		}
		
		// Associate the control with the atomic item
			control.setData(CONTROL_DATA_DETAIL_ITEM, atomicDetailItem);
			control.setData(CONTROL_DATA_ASSOCIATED_LABEL, labelOrLinkControl);
			control.setData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM, containingSimpleDetailItem);
			control.setData(CONTROL_DATA_POSSIBLE_VALUES, possibleValues);
			control.setLayoutData(controlGridData);
		if(readOnlyMode) {
			control.setEnabled(false);
		}
		
		// Set the help context id
		if(helpContextId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(control, helpContextId);
		}
		
		// Check if the item should be disabled
		if(shouldItemDisableClass != null) {
			Object object = null;
			try {
				object = shouldItemDisableClass.newInstance();
			} catch (Exception e) {e.printStackTrace();}
			if(object instanceof ICustomShouldItemDisableObject) {
				ICustomShouldItemDisableObject shouldItemDisableObject = (ICustomShouldItemDisableObject)object;
				IEditorInput editorInput = editorPart.getEditorInput();
				IResource resource = null;
				if(editorInput != null) {
					resource = (IResource) editorInput.getAdapter(IResource.class);
				}
				boolean disableItem = shouldItemDisableObject.getDisabled(atomicDetailItem.getValue(), atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
				if (disableItem) {
					control.setEnabled(false);
				}
			}
		}
		// Add the control to the control list
		controlList.add(control);
		
		// Indentation to ensure field decorations are not overlaped.
		controlGridData.horizontalIndent = 2;
	}
	
	private Composite createCustomSection(Composite parentComposite, CustomSection customSection) {
		String label = customSection.getLabel();
		String sectionHeaderText = customSection.getHeaderText();
		if(label == null) {
			label = "";
		}
		Section section = null;
		if(sectionHeaderText != null) {
			section = widgetFactory.createSection(parentComposite,  ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | Section.DESCRIPTION);
			sectionHeaderText = ModelUtil.formatHeaderOrFooterText(sectionHeaderText);
			section.setDescription(sectionHeaderText);
		} else {
			section = widgetFactory.createSection(parentComposite, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		}

		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(gridData);
		section.setText(label);
		final Composite composite = widgetFactory.createComposite(section, SWT.NONE);
		composite.setBackground(widgetFactory.getColors().getBackground());
		GridLayout gridLayout = new GridLayout(3, false);


		int marginBottom = 9;
		section.clientVerticalSpacing = 9;
		if(sectionHeaderText != null) {
			section.descriptionVerticalSpacing = 9;
			// Add spacing for the special case of no icon and description
			Composite newComposite = widgetFactory.createComposite(parentComposite);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.heightHint = 4;
			newComposite.setLayoutData(gridData);
			marginBottom -= 7;
		}
		
		
		
		gridLayout.marginHeight = 2;			
		gridLayout.marginLeft = 2;
		gridLayout.marginRight = 2;
		gridLayout.marginWidth = 0;
		gridLayout.marginBottom = marginBottom;

		composite.setLayout(gridLayout);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		composite.setLayoutData(gridData);
		section.setClient(composite);
		
		/*
		 *  add a listener to custom secitons to update scrolling height
		 */
		section.addExpansionListener(new IExpansionListener(){

			public void expansionStateChanged(ExpansionEvent e) {
				ddeViewer.updateDetailsViewScrolling();
			}

			public void expansionStateChanging(ExpansionEvent e) {
			}
		});
		
		// If the setting specifies sections should be collapsed, collapse the section
		if(!expandSections) {
			section.setExpanded(false);
		}
		
		return composite;
	}

	private Composite createGroupSection(Composite parentComposite, String label, String desc)
	{
		if (desc==null)
			desc="";
		//create section
		Section sec = widgetFactory.createSection(parentComposite, ExpandableComposite.TITLE_BAR |Section.DESCRIPTION | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		sec.setLayoutData(gridData);
		sec.clientVerticalSpacing = 9;
		sec.setText(label);
		sec.setDescription(desc);
		// Create composite inside the section
		final Composite sectionClientComposite = widgetFactory.createComposite(sec, SWT.FLAT);
		GridLayout gridSectionChildLayout = new GridLayout(3,false);
		gridSectionChildLayout.marginBottom=20;
		gridSectionChildLayout.marginTop=5;
		gridSectionChildLayout.marginLeft=0;
		gridSectionChildLayout.marginRight=0;
		gridSectionChildLayout.marginHeight=0;
		gridSectionChildLayout.marginWidth=0;
		sectionClientComposite.setLayout(gridSectionChildLayout);
		
		sectionClientComposite.setLayoutData(gridData);
		sec.setClient(sectionClientComposite);
		
		sec.addExpansionListener(new IExpansionListener(){

			public void expansionStateChanged(ExpansionEvent e) {
				ddeViewer.updateDetailsViewScrolling();
			}

			public void expansionStateChanging(ExpansionEvent e) {
			}
		});
		sectionsPresent = true;
		return sectionClientComposite;
	}
	
	private Composite createSimpleDetailItemSection(final SimpleDetailItem detailItem, Composite parentComposite, Composite currentControlsComposite) {
		boolean hideSectionTitle = false;
		String label = null;
		String sectionHeaderText = null;
		boolean sectionIsRequired = detailItem.isRequired();
		
		// Obtain customization information (if available)
		DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
		if(detailItemCustomization != null) {
			label = detailItemCustomization.getLabel();
			hideSectionTitle = detailItemCustomization.isHideSectionTitle();
			sectionHeaderText = detailItemCustomization.getSectionHeaderText();
			if(!sectionIsRequired) {
				sectionIsRequired = detailItemCustomization.isRequired();
			}
		}
		
		// If there is no custom label, assign the default (detail item name)
		if(label == null) {
			label = detailItem.getName();
		}
		
		final String itemLabel = label;

		// Check if the section should be created
		if(hideSectionTitle) {
			// If no section is to be created, return the controls composite
			return currentControlsComposite;
		} else {
			//addSeparator(parentComposite);
			Section section = null;
			
			// Create section with/without description
			if(sectionHeaderText != null) {
				section = widgetFactory.createSection(parentComposite,  ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | Section.DESCRIPTION);
				sectionHeaderText = ModelUtil.formatHeaderOrFooterText(sectionHeaderText);
				section.setDescription(sectionHeaderText);
			} else {
				section = widgetFactory.createSection(parentComposite,  ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
			}
			
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			section.setLayoutData(gridData);
			section.setData(CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION, detailItem);

			// Set the section text
			if(!sectionIsRequired) {
				section.setText(MessageFormat.format(Messages.OPTIONAL, new String[] {itemLabel}));
			} else {
				section.setText(itemLabel);	
			}
			
			
			/* footer truncation fix
			 * adds extra composite and a control resized listener
			 * to update the height hint of a composite forcing the
			 * section to recalculate height and layout
			 */
			
			// Create composite inside the section
			final Composite sectionClientComposite = widgetFactory.createComposite(section, SWT.FLAT);
			GridLayout gridSectionChildLayout = new GridLayout();
			gridSectionChildLayout.marginBottom=0;
			gridSectionChildLayout.marginTop=0;
			gridSectionChildLayout.marginLeft=0;
			gridSectionChildLayout.marginRight=0;
			gridSectionChildLayout.marginHeight=0;
			gridSectionChildLayout.marginWidth=0;
			sectionClientComposite.setLayout(gridSectionChildLayout);
			sectionClientComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			final Composite composite = widgetFactory.createComposite(sectionClientComposite, SWT.FLAT);
			composite.setBackground(widgetFactory.getColors().getBackground());
			GridLayout gridLayout = new GridLayout(3, false);

			// Adjust spacing depending on whether the section contains an icon and a description
			int marginBottom = 9;
			section.clientVerticalSpacing = 9;
			if(sectionHeaderText != null) {
				if(sectionIsRequired) {
					section.descriptionVerticalSpacing = 9;
					// Add spacing for the special case of no icon and description
					Composite newComposite = widgetFactory.createComposite(parentComposite);
					gridData = new GridData(GridData.FILL_HORIZONTAL);
					gridData.heightHint = 4;
					newComposite.setLayoutData(gridData);
					marginBottom -= 7;
				}
			} else {
				if(!sectionIsRequired) {
					section.clientVerticalSpacing = 0;
				}
			}
			
			
			gridLayout.marginHeight = 2;			
			gridLayout.marginLeft = 2;
			gridLayout.marginRight = 2;
			gridLayout.marginWidth = 0;
			gridLayout.marginBottom = marginBottom;

			composite.setLayout(gridLayout);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.grabExcessHorizontalSpace = true;
			composite.setLayoutData(gridData);
			section.setClient(sectionClientComposite);
			
			//add listener to update height hint to fix truncation bug (WS WI:107439)
			sectionClientComposite.addControlListener(new ControlAdapter() {
				 public void controlResized(ControlEvent e) {
			          Rectangle r = sectionClientComposite.getClientArea();
			          Composite child = composite;
			          GridData gd = (GridData) child.getLayoutData();
			          gd.heightHint = child.computeSize(r.width, SWT.DEFAULT).y+15;
			          child.setLayoutData(gd);
			          sectionClientComposite.getParent().layout(true);
			        }
			});
		
			// If the section is optional, add the toolitem to its title to clear it
			if(!sectionIsRequired) {
				ToolBar toolBar = new ToolBar(section, SWT.FLAT);
				final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
				toolBar.setCursor(handCursor);
				// Cursor needs to be explicitly disposed
				toolBar.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						if ((handCursor != null) &&
								(handCursor.isDisposed() == false)) {
							handCursor.dispose();
						}
					}
				});
				
				final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
				toolItem.setImage(DDEPlugin.getDefault().getImage("icons/erase.gif"));
				toolItem.setDisabledImage(DDEPlugin.getDefault().getImage("icons/erase_disabled.gif"));
				toolItem.setToolTipText(Messages.CLEAR_OPTIONAL_ITEM);
				if(detailItem.getElement() == null) {
					toolItem.setEnabled(false);
				}
				
				if(readOnlyMode) {
					toolItem.setEnabled(false);
				}
				// Set up the confirmation message
				MessageFormat messageFormat = new MessageFormat(Messages.CLEAR_OPTIONAL_ITEM_CONFIRMATION);			
				final String confirmationMessage = messageFormat.format(new Object[] {label});
				
				toolItem.addSelectionListener(new SelectionListener(){
					public void widgetDefaultSelected(SelectionEvent e) {
					}

					public void widgetSelected(SelectionEvent e) {
						Element element = detailItem.getElement();
						if(element != null) {
							boolean confirmation = MessageDialog.openQuestion(detailsViewerComposite.getShell(), Messages.CLEAR_OPTIONAL_ITEM, confirmationMessage);
							if(confirmation) {
								if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
									MessageFormat messageFormat = new MessageFormat(Messages.CLEAR);
									ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{itemLabel}));
									ModelUtil.removePrecedingText(element);
									element.getParentNode().removeChild(element);
									TreeItem[] treeItems = ddeViewer.getTreeViewer().getTree().getSelection();
									if(treeItems.length == 1) {
										Object object = treeItems[0].getData();
										if(object instanceof Element) {
											Element containingTreeNodeElement = (Element)object;
											ddeViewer.getValidationManager().validateDetailItem(containingTreeNodeElement, detailItem, false);
											ddeViewer.updateValidationInformation();
										}
									}
									ddeViewer.getUndoManager().endRecording(this);
									refresh();
								}
							}
						}
					}
				});
				section.setTextClient(toolBar);
			}
			
			section.addExpansionListener(new IExpansionListener(){

				public void expansionStateChanged(ExpansionEvent e) {
					ddeViewer.updateDetailsViewScrolling();
				}

				public void expansionStateChanging(ExpansionEvent e) {
				}
			});
			
			// If the setting specifies sections should be collapsed, collapse the section
			if(!expandSections) {
				section.setExpanded(false);
			}
			
			return composite;
		} 
	}


	private void createAtomicDetailItemTable(final RepeatableAtomicDetailItemSet repeatableAtomicItem, Composite composite, SimpleDetailItem containingSimpleDetailItem) {
		
		String labelText = null;
		boolean detectSchemaLabel = false;
		boolean required = false;
		boolean hideLabel = false;
		Image tableIcon = null;
		int textLines = 1;
		String headerText = null;
		String footerText = null;
		String helpContextId = null;
		Map possibleValues = null;

		// Obtain the customization properties for the item
		DetailItemCustomization detailItemCustomization = repeatableAtomicItem.getDetailItemCustomization();
		if(detailItemCustomization != null) {
			labelText = detailItemCustomization.getLabel();
			required = detailItemCustomization.isRequired();
			hideLabel = detailItemCustomization.isHideLabel();
			tableIcon = detailItemCustomization.getTableIcon();
			textLines = detailItemCustomization.getLines();
			headerText = detailItemCustomization.getHeaderText();
			footerText = detailItemCustomization.getFooterText();
			helpContextId = detailItemCustomization.getHelpContextId();
			possibleValues = detailItemCustomization.getPossibleValues();
			detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();

			if(detailItemCustomization.getPossibleValuesClass() != null) {
				Object object = null;
				try {
					object = detailItemCustomization.getPossibleValuesClass().newInstance();
				} catch (Exception e) {e.printStackTrace();}
				if(object instanceof ICustomPossibleValuesObject) {
					ICustomPossibleValuesObject customPossibleValuesObject = (ICustomPossibleValuesObject)object;
					IEditorInput editorInput = editorPart.getEditorInput();
					IResource resource = null;
					if(editorInput != null) {
						resource = (IResource) editorInput.getAdapter(IResource.class);
					}
					possibleValues = customPossibleValuesObject.getPosibleValues(null, null, repeatableAtomicItem.getClosestAncestor(), resource);
				}
			}
		}
		
		// Create (when applicable) the header text
		if(headerText != null) {
			headerText = ModelUtil.formatHeaderOrFooterText(headerText);
			addSeparator(composite);
			Label headerLabel = widgetFactory.createLabel(composite, headerText, SWT.WRAP);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			headerLabel.setLayoutData(gridData);
		}
		//Case where Label is not defined in the customization, we would look at the schema
		if(labelText == null && (detectSchemaLabel || globalDetectSchemaLabel))
		{
			CMElementDeclaration availableCMElementDeclaration = repeatableAtomicItem.getCMElementDeclaration();
			if(availableCMElementDeclaration != null) 
			{
				labelText = ModelUtil.getLabel((CMNode)availableCMElementDeclaration);
			}
		}
		
		if(labelText == null) {
			labelText = repeatableAtomicItem.getName();
		}
		
		if(possibleValues == null) {
			possibleValues = repeatableAtomicItem.getPossibleValues();
		}
		
		final String[] labels = possibleValues != null? (String[])possibleValues.keySet().toArray(new String[possibleValues.size()]) : null;
		final List valueList = possibleValues != null? new ArrayList(possibleValues.values()) : null;
		
		final String atomicDetailItemLabel = labelText;
		
		if(!hideLabel) {
			// For those properties not customized, set their default values

			if(!required) {
				required = repeatableAtomicItem.isRequired();
			}
			
			// Add ':' to the label preceded by '*' if the item is required
			if(required) {
				labelText = MessageFormat.format(Messages.LABEL_ASTERISK, new String[] {labelText});
			}
			labelText = MessageFormat.format(Messages.LABEL_COLON, new String[] {labelText});
		
			// Create the label
			Label label = widgetFactory.createLabel(composite, labelText);
			GridData labelGridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(labelGridData);
			label.setForeground(widgetFactory.getColors().getColor(IFormColors.TITLE));
			
			label.setData(CONTROL_DATA_DETAIL_ITEM, repeatableAtomicItem);
			label.addMouseTrackListener(internalControlListener);
			
			String filterText = treeFilterProcessor.getFilterText();
			if(filterText != null && !"".equals(filterText)) {
				if(labelText.toLowerCase().indexOf(filterText.toLowerCase()) != -1 || repeatableAtomicItem.getName().toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
					label.setFont(DDEPlugin.getDefault().FONT_DEFAULT_BOLD);
				}
			}
		}

		// Create the table
		final Table table = widgetFactory.createTable(composite, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		if(hideLabel) {
			gridData.horizontalSpan = 2;
		}
		gridData.heightHint= textLines * 16;
		gridData.horizontalIndent = 2;
		table.setLayoutData(gridData);
		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(100, false));
		table.setLayout(tableLayout);
		new TableColumn(table, SWT.NONE);
		
		table.setData(CONTROL_DATA_DETAIL_ITEM, repeatableAtomicItem);
		table.setData("TABLE_ICON", tableIcon);
		table.setData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM, containingSimpleDetailItem);
		controlList.add(table);
		table.addMouseTrackListener(internalControlListener);
		
		if(helpContextId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(table, helpContextId);
		}

		// Create tableViewer
		final TableViewer tableViewer = new TableViewer(table);
		tableViewer.setColumnProperties(new String[]{"ELEMENT"}); //$NON-NLS-1$
		tableViewer.setContentProvider(new IStructuredContentProvider(){

			public Object[] getElements(Object inputElement) {
				return ((RepeatableAtomicDetailItemSet)inputElement).getItems();
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
		});


		tableViewer.setLabelProvider(new ITableLabelProvider(){

			public Image getColumnImage(Object element, int columnIndex) {
				Object object = table.getData("TABLE_ICON");
				if(object instanceof Image) {
					Image image = (Image)object;
					return image;
				}
				return null;
			}

			public String getColumnText(Object element, int columnIndex) {
				if(valueList != null) {
					String value = ((AtomicDetailItem)element).getValue();
					int index = valueList.indexOf(value);
					if(index == -1) {
						return value;
					}
					return labels[index];
					
				}
				return ((AtomicDetailItem)element).getValue();
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

		if(!readOnlyMode) {
			if(possibleValues != null) {
				
				tableViewer.setCellEditors(new CellEditor[] {new ComboBoxCellEditor(tableViewer.getTable(), labels, SWT.READ_ONLY)});
				
				tableViewer.setCellModifier(new ICellModifier(){
		
					public boolean canModify(Object element, String property) {
						return true;
					}
		
					public Object getValue(Object element, String property) {
						String value = ((AtomicDetailItem)element).getValue();
						return new Integer(valueList.indexOf(value));
					}
					
					public void modify(Object element, String property, Object value) {
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)((TableItem)element).getData();
						int selectedValue = ((Integer)value).intValue();
						if(selectedValue != -1) {
							atomicDetailItem.setValue((String)valueList.get(selectedValue));
						}
						int index = table.getSelectionIndex();
						tableViewer.refresh(true, true);
						table.setSelection(index);
						if(DetailsViewer.this.isEditingTableContent) {
							DetailsViewer.this.isEditingTableContent = false;
						}
						invokeValidationForControl(table);
						updateOptionalSectionToolBar(table);
					}
					
				});
				
			} else {
			
			    tableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(tableViewer.getTable())});
			
				tableViewer.setCellModifier(new ICellModifier(){
		
					public boolean canModify(Object element, String property) {
						if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
							return true;
						}
						return false;
					}
		
					public Object getValue(Object element, String property) {
						return ((AtomicDetailItem)element).getValue();
					}
					
					public void modify(Object element, String property, Object value) {
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)((TableItem)element).getData();
						atomicDetailItem.setValue((String)value);
						int index = table.getSelectionIndex();
						tableViewer.refresh(true, true);
						table.setSelection(index);
						if(DetailsViewer.this.isEditingTableContent) {
							DetailsViewer.this.isEditingTableContent = false;
						}
						invokeValidationForControl(table);
						updateOptionalSectionToolBar(table);
					}
				});
			}
		} else {
			table.setEnabled(false);
		}
		
		
		
		tableViewer.getTable().addControlListener(new ControlAdapter(){

			public void controlResized(ControlEvent e) {
				((Table)e.widget).getColumn(0).setWidth(((Table)e.widget).getSize().x);
			}
			
		});

		// Create composite for table buttons
		Composite buttonComposite = widgetFactory.createComposite(composite, SWT.FLAT);
		buttonComposite.setBackground(widgetFactory.getColors().getBackground());
		GridLayout buttonGridLayout = new GridLayout();
		buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		buttonGridLayout.numColumns = 1;
		buttonGridLayout.marginWidth = 0;
		buttonGridLayout.marginLeft = 2;
		buttonGridLayout.marginRight = 0;
		buttonGridLayout.marginHeight = 2;
		buttonComposite.setLayout(buttonGridLayout);

		// Create "Add" button
		final Button addButton = new Button(buttonComposite, SWT.FLAT);
		addButton.setText(Messages.LABEL_ADD);
		addButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		addButton.setEnabled(!readOnlyMode);
		table.setData(TABLE_BUTTON_ADD, addButton);
		
		// Create "Remove" button
		final Button removeButton = new Button(buttonComposite, SWT.FLAT);
		removeButton.setText(Messages.LABEL_REMOVE);
		removeButton.setEnabled(false);
		removeButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setData(TABLE_BUTTON_REMOVE, removeButton);
		
		// Create "Move Up" button
		final Button moveUpButton = new Button(buttonComposite, SWT.FLAT);
		moveUpButton.setText(Messages.LABEL_UP);
		moveUpButton.setEnabled(false);
		moveUpButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setData(TABLE_BUTTON_MOVEUP, moveUpButton);
		
		// Create "Move Down" button
		final Button moveDownButton = new Button(buttonComposite, SWT.FLAT);
		moveDownButton.setText(Messages.LABEL_DOWN);
		moveDownButton.setEnabled(false);
		moveDownButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setData(TABLE_BUTTON_MOVEDOWN, moveDownButton);
		
		// Set the "Add" button listener
		addButton.addSelectionListener(new SelectionAdapter(){
			
			public void widgetSelected(SelectionEvent event) {
				if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
					DetailsViewer.this.isEditingTableContent = true;
					RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)tableViewer.getInput();
					MessageFormat messageFormat = new MessageFormat(Messages.ADD);
					ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
					String value = ModelUtil.getDefaultValueForDetailItem(repeatableAtomicItem, customization, editorPart);
					
					boolean containsDefaultValueClass = false;
					DetailItemCustomization detailItemCustomization = repeatableAtomicItem.getDetailItemCustomization();
					if(detailItemCustomization != null && detailItemCustomization.getDefaultValueClass() != null) {
						containsDefaultValueClass = true;
					}
					
					if(!(containsDefaultValueClass && value == null)) {
						Map possibleValues = repeatableAtomicItem.getPossibleValues();
						if(value == null && possibleValues != null) {
							value = possibleValues.values().iterator().next().toString();
						}else if(value == null) {
							value = Messages.LABEL_NEW_ITEM;
						}
						AtomicDetailItem atomicDetailItem = repeatableAtomicItem.addItem(value);
						tableViewer.insert(atomicDetailItem, -1);
		//				tableViewer.setData("FORCE_EDIT", "FORCE_EDIT"); //$NON-NLS-1$ //$NON-NLS-2$
						tableViewer.editElement(atomicDetailItem, 0);
						
						table.setSelection(table.getItemCount() - 1);
						if(table.getItemCount() > 1) {
							moveUpButton.setEnabled(true);
						} else {
							moveUpButton.setEnabled(false);
						}
						moveDownButton.setEnabled(false);
						removeButton.setEnabled(true);
						ddeViewer.getUndoManager().endRecording(this);
					}
				}
			}
		});

		// Set the "Remove" button listener
		removeButton.addSelectionListener(new SelectionAdapter(){
			
			public void widgetSelected(SelectionEvent event) {
				if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
					IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
					Object object = selection.getFirstElement();
					if(object instanceof AtomicDetailItem) {
						RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)tableViewer.getInput();
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
						MessageFormat messageFormat = new MessageFormat(Messages.REMOVE);
						ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
						repeatableAtomicItem.removeItem(atomicDetailItem);
						ddeViewer.getUndoManager().endRecording(this);
						tableViewer.remove(atomicDetailItem);
						removeButton.setEnabled(false);
						moveUpButton.setEnabled(false);
						moveDownButton.setEnabled(false);
						if(input instanceof Element) {
							Element containingTreeNodeElement = (Element)input;
							SimpleDetailItem containingSimpleDetailItem = null;
							object = table.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
							if(object instanceof SimpleDetailItem) {
								containingSimpleDetailItem = (SimpleDetailItem)object;
							}
							ddeViewer.getValidationManager().getMessageManager().removeDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem);
						}
						invokeValidationForControl(table);
						updateOptionalSectionToolBar(table);
					}
				}
			}
		});

		// Set the "Move Up" button selection listener
		moveUpButton.addSelectionListener(new SelectionAdapter(){
			
			public void widgetSelected(SelectionEvent event) {
				if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
					IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
					Object object = selection.getFirstElement();
					if(object instanceof AtomicDetailItem) {
						RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)tableViewer.getInput();
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
						MessageFormat messageFormat = new MessageFormat(Messages.MOVE_UP);
						ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
						repeatableAtomicItem.moveDetailItem(atomicDetailItem, true);
						ddeViewer.getUndoManager().endRecording(this);
						int index = table.getSelectionIndex();
						tableViewer.refresh();
						table.setSelection(index - 1);
						if(index == 1) {
							moveUpButton.setEnabled(false);
						}
						moveDownButton.setEnabled(true);
						invokeValidationForControl(table);
					}
				}
			}});


		// Set the "Move Down" button selection listener
		moveDownButton.addSelectionListener(new SelectionAdapter(){
			
			public void widgetSelected(SelectionEvent e) {
				if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
					IStructuredSelection selection = (IStructuredSelection)tableViewer.getSelection();
					Object object = selection.getFirstElement();
					if(object instanceof AtomicDetailItem) {
						RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)tableViewer.getInput();
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
						MessageFormat messageFormat = new MessageFormat(Messages.MOVE_DOWN);
						ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
						repeatableAtomicItem.moveDetailItem(atomicDetailItem, false);
						ddeViewer.getUndoManager().endRecording(this);
						int index = table.getSelectionIndex();
						tableViewer.refresh();
						table.setSelection(index + 1);
						if(index + 2 == table.getItemCount()) {
							moveDownButton.setEnabled(false);
						}
						moveUpButton.setEnabled(true);
						invokeValidationForControl(table);
					}
				}
			}
		});

		// Add a selectionListener to the table to enable/disable add/remove/up/down buttons
		if(!readOnlyMode) {
			table.addSelectionListener(new SelectionListener(){
	
				public void widgetSelected(SelectionEvent event) {
	
					int index = table.getSelectionIndex();
					int count = table.getItemCount();
					if(index == -1 || count == 1) {
						moveUpButton.setEnabled(false);
						moveDownButton.setEnabled(false);
					} else if(index == 0) {
						moveUpButton.setEnabled(false);
						moveDownButton.setEnabled(true);
					} else if(index == count -1) {
						moveUpButton.setEnabled(true);
						moveDownButton.setEnabled(false);
					} else {
						moveUpButton.setEnabled(true);
						moveDownButton.setEnabled(true);
					}
					if(!removeButton.getEnabled()) {
						removeButton.setEnabled(true);
					}
				}
	
				public void widgetDefaultSelected(SelectionEvent event) {
				}
				
			});
		}
		
		// Add a SelectionChangedListener to the tableViewer to propagate its selection
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener(){

			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				Object object = selection.getFirstElement();
				if(object instanceof AtomicDetailItem) {
					AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
					Node node = atomicDetailItem.getNode();
					selection = new StructuredSelection(node);
					hiddenSelection = selection;
					SelectionChangedEvent selectionChangedEvent = new SelectionChangedEvent(tableViewer, selection);
					fireSelectionChanged(selectionChangedEvent);
					firePostSelectionChanged(selectionChangedEvent);
				}
			}
		});
		
		// Add keyListener to the table for keyboard triggered cell editing
		if(!readOnlyMode) {
			table.addKeyListener(new KeyListener() {
	
				public void keyPressed(KeyEvent event) {
					if(event.character == 13 || event.character == 32 || event.keyCode == SWT.F2) {
						Object object = tableViewer.getElementAt(table.getSelectionIndex());
						if(object instanceof AtomicDetailItem) {
							AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
	//						tableViewer.setData("FORCE_EDIT", "FORCE_EDIT");
							tableViewer.editElement(atomicDetailItem, 0);
						}
					} else if(event.character == SWT.DEL) {
						Object object = tableViewer.getElementAt(table.getSelectionIndex());
						if(object instanceof AtomicDetailItem) {
							RepeatableAtomicDetailItemSet repeatableAtomicItem = (RepeatableAtomicDetailItemSet)tableViewer.getInput();
							AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
							MessageFormat messageFormat = new MessageFormat(Messages.REMOVE);
							ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
							repeatableAtomicItem.removeItem(atomicDetailItem);
							ddeViewer.getUndoManager().endRecording(this);
							tableViewer.remove(atomicDetailItem);
							removeButton.setEnabled(false);
							moveUpButton.setEnabled(false);
							moveDownButton.setEnabled(false);
							if(input instanceof Element) {
								Element containingTreeNodeElement = (Element)input;
								SimpleDetailItem containingSimpleDetailItem = null;
								object = table.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
								if(object instanceof SimpleDetailItem) {
									containingSimpleDetailItem = (SimpleDetailItem)object;
								}
								ddeViewer.getValidationManager().getMessageManager().removeDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem);
							}
							invokeValidationForControl(table);
							updateOptionalSectionToolBar(table);
						}
					}
					
				}
	
				public void keyReleased(KeyEvent e) {
				}
				
			});
		}
		
		// Set the tableViewer input
		tableViewer.setInput(repeatableAtomicItem);
		
		// Create (when applicable) the footerText
		if(footerText != null) {
			footerText = ModelUtil.formatHeaderOrFooterText(footerText);
			Label footerLabel = widgetFactory.createLabel(composite, footerText, SWT.WRAP);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			footerLabel.setLayoutData(gridData);
			addSeparator(composite);
		}
	}
	
	
	
	private Object[] sortDetailItems(Object detailItemArray[]) {
		if (detailItemArray.length > 0) {
			Comparator comparator = new Comparator() {
				
				private int getOrder(Object object) {
					if(object instanceof AbstractControl) {
						AbstractControl abstractControl = (AbstractControl)object;
						return abstractControl.getOrder();
					} else if(object instanceof DetailItem) {
						DetailItem detailItem = (DetailItem)object;
						DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
						if(detailItemCustomization != null) {
							return detailItemCustomization.getOrder();
						}
					}
					return -1;
				}
				
				private String getName(Object object) {
					String returnValue = null;
					if(object instanceof AbstractControl) {
						AbstractControl abstractControl = (AbstractControl)object;
						return Integer.toString(abstractControl.getOrder());
					} else if(object instanceof DetailItem) {
						DetailItem detailItem = (DetailItem)object;
						DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
						if(detailItemCustomization != null) {
							returnValue = detailItemCustomization.getLabel();
						}
						if(returnValue == null) {
							returnValue = detailItem.getName();
						}
					}
					return returnValue;
				}
				
				public int compare(Object arg0, Object arg1) {
					int order_1 = getOrder(arg0);
					int order_2 = getOrder(arg1);

					// If both items have a customized order, use it
					if(order_1 != -1 && order_2 != -1) {
						return order_1 - order_2;
					}
					
					// If one of the items doesnt have a customized order, use order and labels.
					String name_1 = null;
					String name_2 = null;
					if(order_1 == -1) {
						name_1 = getName(arg0);
					} else {
						name_1 = Integer.toString(order_1);
					}
					if(order_2 == -1) {
						name_2 = getName(arg1);
					} else {
						name_2 = Integer.toString(order_2);
					}
					
					return Collator.getInstance().compare(name_1, name_2);
				}
			};
			Arrays.sort(detailItemArray, comparator);
		}
		return detailItemArray;
	}

	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
	    postSelectionChangedListeners.add(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
	    postSelectionChangedListeners.remove(listener);
	}
	
    protected void firePostSelectionChanged(final SelectionChangedEvent event)
    {
        Object[] listeners = postSelectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i)
        {
            final ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
            SafeRunnable.run(new SafeRunnable()
            {
                public void run()
                {
                    listener.selectionChanged(event);
                }
            });
        }
    }
	
    
    private class CustomControlListener extends InternalControlListener implements IValidateNotifier {

		public CustomControlListener(ISelectionProvider selectionProvider) {
			super(selectionProvider);
		}

		public void validateTreeNode() {
			ddeViewer.getValidationManager().getMessageManager().removeTreeNodeDetailItemMessages((Element)input);
			ddeViewer.getValidationManager().validateTreeNode((Element)input, true, false, false);	
		}
		
		public void removalNotify(Node node)
		{
			if (node instanceof Element)
				ddeViewer.getValidationManager().getMessageManager().removeDetailItemMessage((Element)input, null, new AtomicElementDetailItem((Element)node));
			else if (node instanceof Attr)
			{
				CMAttributeDeclaration cmAttrDecl = ModelQueryUtil.getModelQuery(node.getOwnerDocument()).getCMAttributeDeclaration((Attr)node);
				ddeViewer.getValidationManager().getMessageManager().removeDetailItemMessage((Element)input, null, new AtomicAttributeDetailItem((Element)((Attr)node).getOwnerElement(), cmAttrDecl));
			}
		}
		
		public void validateControl(Control control) {			
			Object item = control.getData(ICustomControlObject3.CUSTOM_CONTROL_DATA_DETAIL_ITEM);
			// Validate only if custom control sets the data.
			if (item instanceof DetailItem) {
				invokeValidationForControl(control);
			}	
		}
		
		public void validateDocument()
		{
			ddeViewer.updateValidation();
		}
    	
    }

    
	class InternalControlListener implements FocusListener, SelectionListener, IHyperlinkListener, ModifyListener, MouseTrackListener, Runnable {

		private ISelectionProvider selectionProvider;
		private boolean listen = true;

		
		public InternalControlListener(ISelectionProvider selectionProvider) {
			this.selectionProvider = selectionProvider;
		}
		
		public void widgetSelected(SelectionEvent e)  {
			Object atomicDetailItemObject = e.widget.getData(CONTROL_DATA_DETAIL_ITEM);
			// Check if the control has an associated atomicDetailItem
			if(atomicDetailItemObject instanceof AtomicDetailItem) {
				AtomicDetailItem atomicDetailItem = (AtomicDetailItem)atomicDetailItemObject;
				String atomicDetailItemLabel = null;
				boolean deleteIfEmpty = true;
				boolean isRequired = false;
				boolean clearOptionalSectionIfEmpty = false;
				DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
				if(detailItemCustomization != null) {
					atomicDetailItemLabel = detailItemCustomization.getLabel();
					deleteIfEmpty = detailItemCustomization.isDeleteIfEmpty();
					isRequired = detailItemCustomization.isRequired();
					clearOptionalSectionIfEmpty = detailItemCustomization.isClearOptionalSectionIfEmpty();
				}
				if(atomicDetailItemLabel == null) {
					atomicDetailItemLabel = atomicDetailItem.getName();
				}
				if(atomicDetailItem.isRequired()) {
					isRequired = true;
				}
				// If the control is a combo, update the value of the node to the selection
				if(e.widget instanceof CCombo) {
					CCombo combo = (CCombo)e.widget;
					boolean validateInput = true;
					if(editorPart != null)
					{
						validateInput = ((DDEMultiPageEditorPart)editorPart).validateEditorInput();
					}
					if(validateInput) {
						MessageFormat messageFormat = new MessageFormat(Messages.SET);
						if (ddeViewer != null)
							ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
						int index = combo.getSelectionIndex();
						if (index != -1) {  
							// Either Possible or Suggested
							Map map = (Map) combo.getData(CONTROL_DATA_COMBO_POSSIBLE_VALUES);
						    List list = (List) combo.getData(CONTROL_DATA_COMBO_SUGGESTED_VALUES);
							String value = null;
							boolean allows_empty = false;
							if (map != null) {
							  String key = combo.getItem(index);
							  value = (String)map.get(key);
							  if (atomicDetailItem.getPossibleValues() != null) {
							    allows_empty = atomicDetailItem.getPossibleValues().containsValue("");
		  					  }
							}
							else if (list != null) {
							   Object key = list.get(index);
							   // There is no map.  String is taken from customization as is.
							   if (key instanceof String) {
								  value = (String)key;
							   }
							   if (detailItemCustomization != null) {
								   if (detailItemCustomization.getSuggestedValues() != null)
								     allows_empty = detailItemCustomization.getSuggestedValues().contains("");
							   }
						    }					
							String atomicDetailItemValue = (atomicDetailItem.exists()) ? atomicDetailItem.getValue() : "";
							
							if (!atomicDetailItemValue.equals(value) || allows_empty) {
								if("".equals(value) && atomicDetailItem.exists() && clearOptionalSectionIfEmpty) {
									Object object = e.widget.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
									if(object instanceof SimpleDetailItem) {
										SimpleDetailItem simpleDetailItem = (SimpleDetailItem)object;
										if(!simpleDetailItem.isRequired()) {
											clearAtomicDetailItemOptionalSection(simpleDetailItem, atomicDetailItem);
											return;
										}
									}
								}
								
								if("".equals(value) && deleteIfEmpty && !isRequired && atomicDetailItem.exists() && !allows_empty) {
									clearDetailItemValidationForControl(combo);
									atomicDetailItem.delete();
									hiddenSelection = null;
								} else {
									atomicDetailItem.setValue(value);
								}
								// Fire a selection change event
								Object object = atomicDetailItem.getNode();
								if(object instanceof Node) {
									Node node = (Node)object;
									ISelection selection = new StructuredSelection(node);
									SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, selection);
									fireSelectionChanged(selectionChangedEvent);
									firePostSelectionChanged(selectionChangedEvent);
								}
							}
						}  
						if (ddeViewer != null)
							ddeViewer.getUndoManager().endRecording(this);
						invokeValidationForControl(combo);
						updateOptionalSectionToolBar(combo);
					} else {
						combo.setText(atomicDetailItem.getValue());
					}
				} else if (e.widget instanceof Button) { // If the control is a Button
					Object buttonClassObject = e.widget.getData(CONTROL_DATA_CUSTOM_CODE);
					Object controlObject = e.widget.getData(CONTROL_DATA_ASSOCIATED_CONTROL);
					Button button = (Button)e.widget;
					
					// Check if the control contains custom code associated with it
					if(buttonClassObject instanceof Class && controlObject instanceof Control) {
						
						if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
							// Execute custom code
							MessageFormat messageFormat = new MessageFormat(Messages.SET);
							ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
							Control control = (Control)controlObject;
							invokeExternalObject((Class)buttonClassObject, (AtomicDetailItem)atomicDetailItemObject, (Control)controlObject);
							invokeValidationForControl(control);
							updateOptionalSectionToolBar(control);
							ddeViewer.getUndoManager().endRecording(this);
						}
					} else {
						// If there is no custom code associated with the control, it must be a checkbox button
						// Create or delete the associated node according to the value of the checkbox
						boolean validateInput = true;
						if (editorPart!=null)
						{
							validateInput = ((DDEMultiPageEditorPart)editorPart).validateEditorInput();
						}
							
						if(validateInput) {
							MessageFormat messageFormat = new MessageFormat(Messages.TOGGLE);
							if (ddeViewer!=null)
								ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{atomicDetailItemLabel}));
							
							boolean isButtonChecked = button.getSelection();
							
							Map map = (Map) button.getData(CONTROL_DATA_POSSIBLE_VALUES);
							String newValue = null;
							String checkedValue = null;
							String uncheckedValue = null;
							if(map != null && map.size() > 1) {
								Object[] keys = map.keySet().toArray();
								checkedValue = (String)map.get(keys[0]);
								uncheckedValue = (String)map.get(keys[1]);
							}
							if(isButtonChecked) {
								if(checkedValue != null) {
									newValue = checkedValue;
								} else if(!atomicDetailItem.exists()) {
									atomicDetailItem.setValue(""); //$NON-NLS-1$
								}
							} else {
								if(uncheckedValue != null) {
									newValue = uncheckedValue;
								} else if(atomicDetailItem.exists()) {
									atomicDetailItem.delete();
									hiddenSelection = null;
								}
							}
							if(newValue != null) {
								if("".equals(newValue) && deleteIfEmpty && !isRequired && atomicDetailItem.exists()) {
									clearDetailItemValidationForControl(button);
									atomicDetailItem.delete();
									hiddenSelection = null;
								} else {
									atomicDetailItem.setValue(newValue);
								}
							}
							if (ddeViewer!=null)
								ddeViewer.getUndoManager().endRecording(this);
							invokeValidationForControl(button);
						} else {
							button.setSelection(!button.getSelection());
						}
					}
					updateOptionalSectionToolBar(button);
					//Fix for checkboxes creating new items that are not selected
					//in outline view (REF 108373)
					Object object = atomicDetailItem.getNode();
					if(object instanceof Node) {
						Node node = (Node)object;
						ISelection selection = new StructuredSelection(node);
						hiddenSelection = selection;
						if(ddeViewer!=null)
						{
							//force update selection on source editor, prevents selection of wrong element in outline 
							DDEMultiPageEditorPart ddeMPP =(DDEMultiPageEditorPart) ddeViewer.getEditorPart();
							Object editor = ddeMPP.getAdapter(ITextEditor.class);
							if(editor instanceof StructuredTextEditor) {
								StructuredTextEditor structuredTextEditor = (StructuredTextEditor)editor;
								ISelectionProvider selectionProvider = structuredTextEditor.getSelectionProvider();
								if(selectionProvider != null) {
									selectionProvider.setSelection(selection);
								}
							}
							
							//This timer is used to make sure proper element is still selected in case
							//where the selection changed before element was created and selected
							DetailsViewer.this.getControl().getDisplay().timerExec(750, new Runnable() {
								
								public void run() {
									if(getSelection()!=null)
									{
										SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, getSelection());
										fireSelectionChanged(selectionChangedEvent);
										firePostSelectionChanged(selectionChangedEvent);
									}
								}
							});
						}
					}
				}
				// In this method we will decide whether there is a need to
				// disable other items based on this item's value
				disableItems(atomicDetailItem);
			}             
			
		}

		private void clearAtomicDetailItemOptionalSection(SimpleDetailItem containingSimpleDetailItem, AtomicDetailItem atomicDetailItem) {
			if(!containingSimpleDetailItem.isRequired()) {
				String path = containingSimpleDetailItem.getName() + '/' + atomicDetailItem.getName();
				Element element = containingSimpleDetailItem.getElement();
				element.getParentNode().removeChild(element);
				refresh();
				setSelection(path);
	
				TreeItem[] treeItems = ddeViewer.getTreeViewer().getTree().getSelection();
				if(treeItems.length == 1) {
					Object object = treeItems[0].getData();
					if(object instanceof Element) {
						Element containingTreeNodeElement = (Element)object;
						ddeViewer.getValidationManager().validateDetailItem(containingTreeNodeElement, containingSimpleDetailItem, false);
						ddeViewer.updateValidationInformation();
					}
				}
			}
		}
		
		
		
		public void widgetDefaultSelected(SelectionEvent e) {
		} 

		public void focusGained(FocusEvent event) {
			// If the control that gained the focus has an associated atomic detail item, obtain
			// its node and fire the change selection even with it
			Object object = event.widget.getData(CONTROL_DATA_DETAIL_ITEM);
			if (object instanceof AtomicDetailItem) {
				AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
				object = atomicDetailItem.getNode();
				if(object instanceof Node) {
					Node node = (Node)object;
					ISelection selection = new StructuredSelection(node);
					hiddenSelection = selection;
					SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, selection);
					fireSelectionChanged(selectionChangedEvent);
					firePostSelectionChanged(selectionChangedEvent);
				} else {
					ISelection selection = new StructuredSelection();
					SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, selection);
					fireSelectionChanged(selectionChangedEvent);
					firePostSelectionChanged(selectionChangedEvent);
				}
			}
		}    

		public void focusLost(FocusEvent event)  {
			if(this.modifyEvent != null) {
				DetailsViewer.this.getControl().getDisplay().timerExec(0, this);
			}
		}

		public void linkActivated(HyperlinkEvent e) {
			Object labelLinkClassObject = e.widget.getData(CONTROL_DATA_CUSTOM_CODE);
			Object atomicDetailItemObject = e.widget.getData(CONTROL_DATA_DETAIL_ITEM);
			Object controlObject = e.widget.getData(CONTROL_DATA_ASSOCIATED_CONTROL);
			// If the control has an atomic detail item, custom code and an associated control, execute the custom code

			if (atomicDetailItemObject instanceof AtomicDetailItem) {
				AtomicDetailItem atomicDetailItem = (AtomicDetailItem) atomicDetailItemObject;
				if (labelLinkClassObject instanceof Class) {
					boolean hasCustomControl = hasCustomControlCustomization(atomicDetailItem);
					//There is no custom control customization and the Object is a Control
					//This is the support for the existing non-custom control case.
					if(!hasCustomControl && controlObject instanceof Control)
						invokeExternalObject((Class)labelLinkClassObject, atomicDetailItem, (Control)controlObject);
					
					//There is a custom control customization and the Object is not a Control
					if(hasCustomControl && !(controlObject instanceof Control))
						invokeExternalObject((Class)labelLinkClassObject, atomicDetailItem, null);
				}
			}
			updateOptionalSectionToolBar((Control)e.widget);
		}
		
		public void linkEntered(HyperlinkEvent e) {
		}

		
		public void linkExited(HyperlinkEvent e) {
		}

		private void invokeExternalObject(Class externalClass, AtomicDetailItem atomicDetailItem, Control control) {
			try {
				String newValue = null;
				Object object = externalClass.newInstance();
				String currentValue = atomicDetailItem.getValue(); 
				boolean hasCustomControl = hasCustomControlCustomization(atomicDetailItem);
				
				if (object instanceof ICustomizationObject) {
					ICustomizationObject externalObject = (ICustomizationObject)object;
					newValue = externalObject.invoke(currentValue);   
				} else if(object instanceof IAdvancedCustomizationObject) {
					Node itemNode = atomicDetailItem.getNode();
					Element closestAncestor = atomicDetailItem.getClosestAncestor();
					IAdvancedCustomizationObject advancedCustomizationObject = (IAdvancedCustomizationObject)object;
					newValue = advancedCustomizationObject.invoke(currentValue, itemNode, closestAncestor, editorPart);
				}
				// We support updating of controls only for non-custom control scenarios
				if(control != null && newValue != null && !hasCustomControl) {
					boolean deleteIfEmpty = true;
					boolean isRequired = false;
					boolean clearOptionalSectionIfEmpty = false;
					DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
					if(detailItemCustomization != null) {
						deleteIfEmpty = detailItemCustomization.isDeleteIfEmpty();
						isRequired = detailItemCustomization.isRequired();
						clearOptionalSectionIfEmpty = detailItemCustomization.isClearOptionalSectionIfEmpty();
					}
					if(atomicDetailItem.isRequired()) {
						isRequired = true;
					}
					if(!newValue.equals(currentValue)) {
						if("".equals(newValue) && atomicDetailItem.exists() && clearOptionalSectionIfEmpty) {
							control.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
							if(object instanceof SimpleDetailItem) {
								SimpleDetailItem simpleDetailItem = (SimpleDetailItem)object;
								if(!simpleDetailItem.isRequired()) {
									clearAtomicDetailItemOptionalSection(simpleDetailItem, atomicDetailItem);
									return;
								}
							}
						}
						
						if("".equals(newValue) && deleteIfEmpty && !isRequired && atomicDetailItem.exists()) {
							clearDetailItemValidationForControl(control);
							atomicDetailItem.delete();
							hiddenSelection = null;
						} else {
							atomicDetailItem.setValue(newValue);
							invokeValidationForControl(control);
						}
						refresh();
					}
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		private ModifyEvent modifyEvent;
		
		public void modifyText(ModifyEvent e) {
			if(listen) {
				this.modifyEvent = e;
				DetailsViewer.this.getControl().getDisplay().timerExec(260, this);
			}
		}
		
		Thread delayManager;

		public void performModifyText(ModifyEvent e) {
			if(listen) {
				listen = false;
				Object item = e.widget.getData(CONTROL_DATA_DETAIL_ITEM);
				// Check if the control has an associated AtomicDetailItem
				if (item instanceof AtomicDetailItem) {
					AtomicDetailItem atomicDetailItem = (AtomicDetailItem)item;
					// If the control is a Text, update the corresponding node with its value
					if (e.widget instanceof Text) {
					Text text = (Text)e.widget;
					
					boolean validateInput=true;
					if (((DDEMultiPageEditorPart)editorPart)!=null)
					{
						validateInput = ((DDEMultiPageEditorPart) editorPart).validateEditorInput();
					}
					if(validateInput) {
						String label = null;
						String value = text.getText();  
						boolean deleteIfEmpty = true;
						boolean isRequired = true;
						boolean clearOptionalSectionIfEmpty = false;
						DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
							if(detailItemCustomization != null) {
								deleteIfEmpty = detailItemCustomization.isDeleteIfEmpty();
								label = detailItemCustomization.getLabel();
								isRequired = detailItemCustomization.isRequired();
								clearOptionalSectionIfEmpty = detailItemCustomization.isClearOptionalSectionIfEmpty();
							}
							if(label == null) {
								label = atomicDetailItem.getName();
							}
							if(atomicDetailItem.isRequired()) {
								isRequired = true;
							}
							if(!atomicDetailItem.getValue().equals(value)) {
								MessageFormat messageFormat = new MessageFormat(Messages.EDIT);
								if(ddeViewer!=null)
									ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{label}));
								if("".equals(value) && atomicDetailItem.exists() && clearOptionalSectionIfEmpty) {
									Object object = e.widget.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
									if(object instanceof SimpleDetailItem) {
										SimpleDetailItem simpleDetailItem = (SimpleDetailItem)object;
										if(!simpleDetailItem.isRequired()) {
											clearAtomicDetailItemOptionalSection(simpleDetailItem, atomicDetailItem);
											listen = true;
											return;
										}
									}
								}
								
								if("".equals(value) && deleteIfEmpty && ( !isRequired || atomicDetailItem.isOptionalWithinContext() ) && atomicDetailItem.exists()) {
									clearDetailItemValidationForControl(text);
									atomicDetailItem.delete();
									hiddenSelection = null;
								} else {
									atomicDetailItem.setValue(value);
								}
								if(ddeViewer!=null)
									ddeViewer.getUndoManager().endRecording(this);
								invokeValidationForControl(text);
								updateOptionalSectionToolBar(text);
							}
							// Fire a selection change event
							// Fix for text fields creating new items that are not selected
							// in outline view (WAS WI:108373)
							Object object = atomicDetailItem.getNode();
							if(object instanceof Node) {
								Node node = (Node)object;
								ISelection selection = new StructuredSelection(node);
								hiddenSelection = selection;
								if(ddeViewer!=null)
								{
									// force update selection on source editor, prevents selection of wrong element in outline 
									DDEMultiPageEditorPart ddeMPP =(DDEMultiPageEditorPart) ddeViewer.getEditorPart();
									Object editor = ddeMPP.getAdapter(ITextEditor.class);
									if(editor instanceof StructuredTextEditor) {
										StructuredTextEditor structuredTextEditor = (StructuredTextEditor)editor;
										ISelectionProvider selectionProvider = structuredTextEditor.getSelectionProvider();
										if(selectionProvider != null) {
											selectionProvider.setSelection(selection);
										}
									}
									
									// This timer is used to make sure proper element is still selected in case
									// where the selection changed before element was created and selected
									DetailsViewer.this.getControl().getDisplay().timerExec(750, new Runnable() {
										
										public void run() {
											if(getSelection()!=null)
											{
												SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, getSelection());
												fireSelectionChanged(selectionChangedEvent);
												firePostSelectionChanged(selectionChangedEvent);
											}
										}
									});
								}
							}
						} else {
							text.setText(atomicDetailItem.getValue());
						}
					} else if (e.widget instanceof CCombo) {
						CCombo combo = (CCombo)e.widget;
						if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
							String label = null;
							String value = combo.getText();  
							boolean deleteIfEmpty = true;
							boolean isRequired = true;
							boolean clearOptionalSectionIfEmpty = false;
								DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
								if(detailItemCustomization != null) {
									deleteIfEmpty = detailItemCustomization.isDeleteIfEmpty();
									label = detailItemCustomization.getLabel();
									isRequired = detailItemCustomization.isRequired();
									clearOptionalSectionIfEmpty = detailItemCustomization.isClearOptionalSectionIfEmpty();
								}
								if(label == null) {
									label = atomicDetailItem.getName();
								}
								if(atomicDetailItem.isRequired()) {
									isRequired = true;
								}
								if(!atomicDetailItem.getValue().equals(value)) {
									MessageFormat messageFormat = new MessageFormat(Messages.EDIT);
									ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[]{label}));
									if("".equals(value) && atomicDetailItem.exists() && clearOptionalSectionIfEmpty) {
										Object object = e.widget.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
										if(object instanceof SimpleDetailItem) {
											SimpleDetailItem simpleDetailItem = (SimpleDetailItem)object;
											if(!simpleDetailItem.isRequired()) {
												clearAtomicDetailItemOptionalSection(simpleDetailItem, atomicDetailItem);
												listen = true;
												return;
											}
										}
									}
									
									if("".equals(value) && deleteIfEmpty && !isRequired && atomicDetailItem.exists()) {
										clearDetailItemValidationForControl(combo);
										atomicDetailItem.delete();
										hiddenSelection = null;
									} else {
										atomicDetailItem.setValue(value);
									}
									ddeViewer.getUndoManager().endRecording(this);
									invokeValidationForControl(combo);
									updateOptionalSectionToolBar(combo);
								}
								// Fire a selection change event
								Object object = atomicDetailItem.getNode();
								if(object instanceof Node) {
									Node node = (Node)object;
									ISelection selection = new StructuredSelection(node);
									SelectionChangedEvent selectionChangedEvent= new SelectionChangedEvent(selectionProvider, selection);
									fireSelectionChanged(selectionChangedEvent);
									firePostSelectionChanged(selectionChangedEvent);
								}
							} else {
								combo.setText(atomicDetailItem.getValue());
							}
					}
					// In this method we will decide whether there is a need to
					// disable other items based on this item's value
					disableItems(atomicDetailItem);
				}
				listen = true;
			}
		}

		public void mouseEnter(MouseEvent e) {
		}

		public void mouseExit(MouseEvent e) {
			defaultInformationControl.setVisible(false);
		}

		public void mouseHover(MouseEvent e) {
			if(customization != null) {
				Control control = (Control)e.widget;
				Object object = control.getData(CONTROL_DATA_DETAIL_ITEM);
				String hoverHelpText = null;
				if(object instanceof DetailItem) {
					String tagName = "<b>"+((DetailItem)object).getName()+"</b> ";
					if (object instanceof AtomicDetailItem)
					{
						AtomicDetailItem atomicDetailItem = (AtomicDetailItem)object;
						
						if(atomicDetailItem.getDetailItemCustomization() != null) {
							hoverHelpText = atomicDetailItem.getDetailItemCustomization().getHoverHelp();
						}
						if(hoverHelpText == null && customization.isDisplayDocumentationAsHoverText()) {
							hoverHelpText = atomicDetailItem.getDocumentation();
						}
					} // Bug 113676 - ClassCastException when hovering over "Feature" label (RepeatableAtomicDetailItemSet)
					else if (object instanceof RepeatableAtomicDetailItemSet)
					{
						RepeatableAtomicDetailItemSet repeatableItem = (RepeatableAtomicDetailItemSet) object;
						DetailItemCustomization detailItemCustomization = repeatableItem.getDetailItemCustomization();
						if (detailItemCustomization != null) {
						   hoverHelpText = detailItemCustomization.getHoverHelp();
						}
						if(hoverHelpText == null && customization.isDisplayDocumentationAsHoverText()) {
							// Get the first item's documentation as the hover help text
							if(repeatableItem.getItems().length > 0)
								hoverHelpText = repeatableItem.getItems()[0].getDocumentation();
							//else show the documentation from the element's declaration
							else 
								hoverHelpText = repeatableItem.getDocumentation();
							
							//also show the default value under the documentation if it is not null
							String defaultAttribute = repeatableItem.getElementDefaultAttribute();
							
							if(defaultAttribute != null)
								hoverHelpText += "<br><br>" + defaultAttribute;							//$NON-NLS-1$
						}						
					}
					if(hoverHelpText == null || "".equals(hoverHelpText)) {
						hoverHelpText = tagName;
					}
					if(hoverHelpText != null && !"".equals(hoverHelpText)) {
						defaultInformationControl.setSizeConstraints(400, 400);
						defaultInformationControl.setInformation(hoverHelpText);
						//determine the size of the hover help to be displayed
						Point computeSizeHint = defaultInformationControl.computeSizeHint();
						defaultInformationControl.setSize(computeSizeHint.x, computeSizeHint.y);
						Point controlLocation = control.getLocation();

						//draw the hover help relative to the cursor location in a table (so as to not block values)
						if(control instanceof Table)
							defaultInformationControl.setLocation(control.getParent().toDisplay(e.x + controlLocation.x, e.y + controlLocation.y + 25));
						//otherwise draw it just under the control
						else
							defaultInformationControl.setLocation(control.getParent().toDisplay(controlLocation.x, controlLocation.y + 25));
						
						defaultInformationControl.setVisible(true);
					}
				}
			}
		}

		public void run() {
			performModifyText(this.modifyEvent);
			this.modifyEvent = null;
		}
	}
	
	private void updateOptionalSectionToolBar(Control control) {
		if(!control.isDisposed()) {
			Object object = control.getParent().getParent();
			if(object instanceof Section) {
				Section section = (Section)object;
				object = section.getTextClient();
				if(object instanceof ToolBar) {
					ToolBar toolBar = (ToolBar)object;
					ToolItem toolItem = toolBar.getItem(0);
					if(toolItem != null) {
						if(!toolItem.isDisposed()) {
							if(!toolItem.getEnabled()) {
								object = section.getData(CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION);
								if(object instanceof SimpleDetailItem) {
									SimpleDetailItem simpleDetailItem = (SimpleDetailItem)object;
									Element element = simpleDetailItem.getElement();
									if(element != null) {
										toolItem.setEnabled(true);
										if(input instanceof Element) {
											Element containingTreeNodeElement = (Element)input;
											ddeViewer.getValidationManager().validateDetailItem(containingTreeNodeElement, simpleDetailItem, false);
											ddeViewer.updateValidationInformation();
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	

	private void addSeparator(Composite composite) {
		Composite newComposite = widgetFactory.createComposite(composite);
		GridData gridData = new GridData();
		gridData.heightHint = 1;
		gridData.horizontalSpan = 3;
		newComposite.setLayoutData(gridData);
	}
	
	private void invokeValidationForControl(Control control) {
		if(!control.isDisposed()) {
			Object object = control.getData(CONTROL_DATA_DETAIL_ITEM);
			if (object == null) {
				object = control.getData(ICustomControlObject3.CUSTOM_CONTROL_DATA_DETAIL_ITEM);
			}
			if(object instanceof DetailItem) {
				DetailItem detailItem = (DetailItem)object;
				SimpleDetailItem simpleDetailItem = null;
				object = control.getParent().getParent();
				if(object instanceof Section) {
					Section section = (Section)object;
					object = section.getData(CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION);
					if(object instanceof SimpleDetailItem) {
						 simpleDetailItem = (SimpleDetailItem)object;
					}
				}
				if(input instanceof Element) {
					Element element = (Element)input;
					if(ddeViewer!=null)
					{
						ddeViewer.getValidationManager().validateDetailItem(element, simpleDetailItem, detailItem, false);
						if(ddeViewer.getValidationManager().getMessageManager().getDocumentMessageCount(ValidationMessage.MESSAGE_TYPE_ERROR) == 0)
							ddeViewer.setSourceViewContainsErrors(false);
						else
							ddeViewer.updateValidationInformation();
					}
				}
			}
		}
	}
	
	private void clearDetailItemValidationForControl(Control control) {
		if(!control.isDisposed()) {
			Object object = control.getData(CONTROL_DATA_DETAIL_ITEM);
			if(object instanceof DetailItem) {
				DetailItem detailItem = (DetailItem)object;
				SimpleDetailItem simpleDetailItem = null;
				object = control.getParent().getParent();
				if(object instanceof Section) {
					Section section = (Section)object;
					object = section.getData(CONTROL_DATA_OPTIONAL_DETAIL_ITEM_SECTION);
					if(object instanceof SimpleDetailItem) {
						 simpleDetailItem = (SimpleDetailItem)object;
					}
				}
				if(input instanceof Element) {
					Element element = (Element)input;
					if(ddeViewer!=null)
					{
						ddeViewer.getValidationManager().getMessageManager().removeDetailItemMessage(element, simpleDetailItem, detailItem);
						ddeViewer.updateValidationInformation();
					}
				}
			}
		}
	}
	
	private void disableItems(AtomicDetailItem atomicDetailItem) {
		DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
		if(detailItemCustomization != null && atomicDetailItem.exists()) {
			Class disableClass = detailItemCustomization.getDisableClass();
			if(disableClass != null) {
				Object object = null;
				try {
					object = disableClass.newInstance();
				} catch (Exception ex) {ex.printStackTrace();}
				if(object instanceof ICustomDisableObject) {
					ICustomDisableObject disableObject = (ICustomDisableObject)object;
					IEditorInput editorInput = editorPart.getEditorInput();
					IResource resource = null;
					if(editorInput != null) {
						resource = (IResource) editorInput.getAdapter(IResource.class);
					}

					boolean disableAll = disableObject.setDisableTrigger(atomicDetailItem.getValue(), atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
					if (disableAll) {
						refresh();
					}
				}
			}
		}
	}
	
	public void upadteDecorators() {
		if(input instanceof Element) {
			if (ddeViewer==null)
				return;
			Element containingTreeNodeElement = (Element)input;
			ValidationManager validationManager = ddeViewer.getValidationManager();
			Iterator iterator = controlList.iterator();
			while (iterator.hasNext()) {
				SimpleDetailItem containingSimpleDetailItem = null;
				DetailItem detailItem = null;
	
				Control control = (Control) iterator.next();
				
				Table table = null;
				if (control instanceof Table) {
					table = (Table)control;
				}
				
				Object object = control.getData(CONTROL_DATA_DETAIL_ITEM);
				
	            boolean isCustomControl = false;
	            if (object == null) {
	            	object = control.getData(ICustomControlObject3.CUSTOM_CONTROL_DATA_DETAIL_ITEM);
		            isCustomControl = true;
	            }
	
				if(object instanceof DetailItem) {
					detailItem = (DetailItem)object;
					object = control.getData(CONTROL_DATA_CONTAINING_SIMPLE_DETAIL_ITEM);
					if(object instanceof SimpleDetailItem) {
						containingSimpleDetailItem = (SimpleDetailItem)object;
					}
					ValidationMessage message = validationManager.getMessageManager().getDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, detailItem);
					if(message != null) {
						ControlDecoration controlDecoration = (ControlDecoration)control.getData(CONTROL_DATA_DECORATOR);
						if(controlDecoration == null) {
							controlDecoration = new ControlDecoration(control, SWT.LEFT | SWT.BOTTOM);
							control.setData(CONTROL_DATA_DECORATOR, controlDecoration);
						}
						switch(message.getMessageType()) {
						case ValidationMessage.MESSAGE_TYPE_WARNING:
							controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
							break;
						case ValidationMessage.MESSAGE_TYPE_ERROR:
							controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
							break;
						}
						controlDecoration.setDescriptionText(message.getMessage());
					} else {
						ControlDecoration controlDecoration = (ControlDecoration)control.getData(CONTROL_DATA_DECORATOR);
						if(controlDecoration != null) {
							controlDecoration.hide();
							controlDecoration.dispose();
							control.setData(CONTROL_DATA_DECORATOR, null);
						}
					}
				}
			}
		}
	}
	
	public boolean getIsEditingTableContent() {
		return isEditingTableContent;
	}
	
	public Control CreateLinkOrLabel(DetailItem detailItem,DetailItemCustomization detailItemCustomization, Composite composite)
	{
		Control labelOrLinkControl = null;
		String labelText = null;

		Class linkClass = null;
		String linkToolTip = null;
		Map possibleValues = null;
		List suggestedValues = null;
		boolean required = false;
		int style = DetailItemCustomization.STYLE_DEFAULT;
		boolean hideLabel = false;
		boolean showItemAsOptional = false;
		boolean detectSchemaLabel = false;
		
		// If the atomic detail item is customized, set its properties to those in the customization
		if(detailItemCustomization!= null)
		{
			hideLabel = detailItemCustomization.isHideLabel();
			labelText = detailItemCustomization.getLabel();
			required = detailItemCustomization.isRequired();
			style = detailItemCustomization.getStyle();
			showItemAsOptional = detailItemCustomization.isShowItemAsOptional();
			linkClass = detailItemCustomization.getLinkClass();
			linkToolTip = detailItemCustomization.getLabelLinkToolTip();
			possibleValues = detailItemCustomization.getPossibleValues();
			suggestedValues = detailItemCustomization.getSuggestedValues();
			detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();
			
		}
		// Properties not customized are assigned their default values
		if(detailItem instanceof AtomicDetailItem)
		{
			possibleValues = getPossibleValuesFromCustomization(detailItemCustomization,((AtomicDetailItem)detailItem));
			suggestedValues = getSuggestedValuesFromCustomization(detailItemCustomization,((AtomicDetailItem)detailItem));
			
			//Case where Label is not defined in the customization, we would look at the schema
			if(labelText == null && (detectSchemaLabel || globalDetectSchemaLabel))
			{
				labelText = ModelUtil.getLabel(((AtomicDetailItem) detailItem).getCMNode());
			}
			//The case where the label is neither defined in the customization file nor in the schema file.
			if(labelText == null)
			{
				labelText = ((AtomicDetailItem)detailItem).getName();
			}
			if(!required) {
				if(!((AtomicDetailItem)detailItem).isOptionalWithinContext()) {
					required = ((AtomicDetailItem)detailItem).isRequired();
				}
			}
			if(possibleValues == null) {
				possibleValues = ((AtomicDetailItem)detailItem).getPossibleValues();
			}
			if(style == DetailItemCustomization.STYLE_DEFAULT || style == DetailItemCustomization.STYLE_TREE_NODE) {
				if(possibleValues != null || suggestedValues != null) {
					style = DetailItemCustomization.STYLE_COMBO;
				} else if(((AtomicDetailItem)detailItem).hasEditableValue()){
					style = DetailItemCustomization.STYLE_TEXT;
				} else {
					style = DetailItemCustomization.STYLE_CHECKBOX;
				}
			}
		}
		else if (detailItem instanceof RepeatableAtomicDetailItemSet)
		{
			// We get into here only for custom control cases
			
			//Case where Label is not defined in the customization, we would look at the schema
			if(labelText == null && detectSchemaLabel || globalDetectSchemaLabel)
			{
				AtomicDetailItem[] items = ((RepeatableAtomicDetailItemSet) detailItem).getItems();
				if (items.length > 0)
				{
				  labelText = ModelUtil.getLabel(items[0].getCMNode());
				}
			}
			//The case where the label is neither defined in the customization file nor in the schema file.
			if(labelText == null)
			{
				labelText = ((RepeatableAtomicDetailItemSet)detailItem).getName();
			}
		}

		if(!hideLabel) {
			// Add ':' after the label preceded by '*' if the item is required
			if(!"".equals(labelText)) {
				if(required && style != DetailItemCustomization.STYLE_CHECKBOX && !showItemAsOptional) {
					labelText = MessageFormat.format(Messages.LABEL_ASTERISK, new String[] {labelText});
				}
				labelText = MessageFormat.format(Messages.LABEL_COLON, new String[] {labelText});
			}
			if(linkClass != null) {
				Hyperlink labelLink = widgetFactory.createHyperlink(composite, labelText, SWT.NONE);
				labelLink.setData(CONTROL_DATA_CUSTOM_CODE, linkClass);
				labelLink.addHyperlinkListener(internalControlListener);
				if(linkToolTip != null) {
					labelLink.setToolTipText(ModelUtil.formatToolTip(linkToolTip));
				}
				if(readOnlyMode) {
					labelLink.setEnabled(false);
				}
				labelOrLinkControl = labelLink;
				// Associate the link, For custom controls the control will be null for now until we have support to ask the adopter what control we should set it on
				// In Liberty's use case, it is simply an "Include" link to open the included file into another editor
                if(labelOrLinkControl != null) {
                    labelOrLinkControl.setData(CONTROL_DATA_ASSOCIATED_CONTROL, null);
                }
			} else {
				Label label = widgetFactory.createLabel(composite, labelText);
				label.setForeground(widgetFactory.getColors().getColor(IFormColors.TITLE));
				labelOrLinkControl = label;
			}
			String filterText = null;
			if(treeFilterProcessor!=null)
				 filterText = treeFilterProcessor.getFilterText();
			if(filterText != null && !"".equals(filterText)) {
				if(labelText.toLowerCase().indexOf(filterText.toLowerCase()) != -1 || detailItem.getName().toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
					labelOrLinkControl.setFont(DDEPlugin.getDefault().FONT_DEFAULT_BOLD);
				}
			}
			labelOrLinkControl.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
			labelOrLinkControl.setData(CONTROL_DATA_DETAIL_ITEM, detailItem);
			labelOrLinkControl.addMouseTrackListener(internalControlListener);

		}
		return labelOrLinkControl;
	}
	
	//Helper Class to return the possible Values for an Item Customization
	public Map getPossibleValuesFromCustomization(DetailItemCustomization detailItemCustomization, DetailItem detailItem)
	{ 
		Map possibleValues= null;
		if(detailItemCustomization != null)
		{
			possibleValues = detailItemCustomization.getPossibleValues();
			if(detailItemCustomization.getPossibleValuesClass() != null) {
				Object object = null;
				try {
					object = detailItemCustomization.getPossibleValuesClass().newInstance();
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				if(object instanceof ICustomPossibleValuesObject) {
					ICustomPossibleValuesObject customPossibleValuesObject = (ICustomPossibleValuesObject)object;
					IEditorInput editorInput = editorPart.getEditorInput();
					IResource resource = null;
					if(editorInput != null) {
						resource = (IResource) editorInput.getAdapter(IResource.class);
					}
					if(detailItem instanceof AtomicDetailItem)
						possibleValues = customPossibleValuesObject.getPosibleValues(((AtomicDetailItem)detailItem).getValue(), ((AtomicDetailItem)detailItem).getNode(), ((AtomicDetailItem)detailItem).getClosestAncestor(), resource);
					else if (detailItem instanceof RepeatableAtomicDetailItemSet)
						possibleValues = customPossibleValuesObject.getPosibleValues(null, null, ((RepeatableAtomicDetailItemSet)detailItem).getClosestAncestor(), resource);
					}
			}
		}
		return possibleValues;
	}
	
	//Helper Class to return the suggested Values for an Item Customization
	public List getSuggestedValuesFromCustomization(DetailItemCustomization detailItemCustomization, DetailItem detailItem)
	{
		List suggestedValues = null;
		if(detailItemCustomization != null)
		{
			suggestedValues = detailItemCustomization.getSuggestedValues();
			if(detailItemCustomization.getSuggestedValuesClass() != null) {
	            Object object = null;
	            try {
	                    object = detailItemCustomization.getSuggestedValuesClass().newInstance();
	            } catch (Exception e) {e.printStackTrace();}
	            if(object instanceof ICustomSuggestedValuesObject) {
	                    ICustomSuggestedValuesObject customSuggestedValuesObject = (ICustomSuggestedValuesObject)object;
	                    IEditorInput editorInput = editorPart.getEditorInput();
	                    IResource resource = null;
	                    if(editorInput != null) {
	                            resource = (IResource) editorInput.getAdapter(IResource.class);
	                    }
	                    if(detailItem instanceof AtomicDetailItem)
	                    	suggestedValues = customSuggestedValuesObject.getSuggestedValues(((AtomicDetailItem)detailItem).getValue(), ((AtomicDetailItem)detailItem).getNode(), ((AtomicDetailItem)detailItem).getClosestAncestor(), resource);
	    				else if (detailItem instanceof RepeatableAtomicDetailItemSet)
	    					suggestedValues = customSuggestedValuesObject.getSuggestedValues(null, null, ((RepeatableAtomicDetailItemSet)detailItem).getClosestAncestor(), resource);
	            }
	        }
		}
		return suggestedValues;
	}
	
	// Helper method to check if there is any customControlClass defined for this item
	private boolean hasCustomControlCustomization(AtomicDetailItem atomicDetailItem)
	{
		boolean hasCustomization = false;
		DetailItemCustomization custom = atomicDetailItem.getDetailItemCustomization();
		if(custom !=null)
		{
			hasCustomization = custom.getCustomControlClass() != null;
		}
		return hasCustomization;
	}
	
	public void setScrolledComposite(ScrolledComposite composite)
	{
		this.scrolledComposite = composite;
	}
	
	public Control getCurrentControl()
	{
		return currentControl;
	}
	
	public ScrolledComposite getScrolledComposite() {
		return detailsComposite;
	}

	public void dispose()
	{
	    postSelectionChangedListeners.clear();
	}
}

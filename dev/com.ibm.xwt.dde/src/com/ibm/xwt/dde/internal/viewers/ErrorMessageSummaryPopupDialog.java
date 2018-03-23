/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomIconObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.actions.AddElementAction;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicElementDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.DetailItemValidation;

public class ErrorMessageSummaryPopupDialog extends PopupDialog {

	private ScrolledComposite dialogScrolledComposite;
	private Composite dialogComposite;
	private DDEViewer ddeViewer;
	
	public ErrorMessageSummaryPopupDialog(Shell parent, DDEViewer ddeViewer) {
		super(parent, SWT.BORDER | SWT.RESIZE, true, false, false, false, false, null, null);
		this.ddeViewer = ddeViewer;
	}

	public Point getClientAreaActualSize() {
		return dialogComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	protected Control createDialogArea(Composite parent) {

		dialogScrolledComposite = new ScrolledComposite(parent, SWT.FLAT | SWT.V_SCROLL | SWT.H_SCROLL);
		dialogScrolledComposite.setExpandHorizontal(true);
		dialogScrolledComposite.setExpandVertical(true);   
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		dialogScrolledComposite.setLayoutData(gridData);
	
		dialogComposite = new Composite(dialogScrolledComposite, SWT.FLAT);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginTop = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginLeft = 0;
		gridLayout.marginRight = 0;
		gridLayout.marginWidth = 3;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 3;
		gridLayout.verticalSpacing = 0;
		gridLayout.numColumns = 2;
		dialogComposite.setLayout(gridLayout);
		gridData = new GridData();
		dialogComposite.setLayoutData(gridData);

		int errorCount = ddeViewer.getValidationManager().getMessageManager().getDocumentMessageCount(ValidationMessage.MESSAGE_TYPE_ERROR);
		boolean sourceViewContainsErrors = ddeViewer.getSourceViewContainsErrors();
		TreeNodeElement[] treeNodeElements = obtainSortedTreeNodeElements(errorCount > 0 || sourceViewContainsErrors, errorCount == 0 && !sourceViewContainsErrors);
		
		for (int i = 0; i < treeNodeElements.length; i++) {
			final TreeNodeElement currentTreeNodeElement = treeNodeElements[i];
			
			Label treeNodeImageLabel = new Label(dialogComposite, SWT.NONE);
			treeNodeImageLabel.setImage(currentTreeNodeElement.treeNodeImage);
			
			Hyperlink hyperlink = ddeViewer.getWidgetFactory().createHyperlink(dialogComposite, currentTreeNodeElement.treeNodeLabel, SWT.NONE);
			hyperlink.setFont(DDEPlugin.getDefault().FONT_DEFAULT_BOLD);
			hyperlink.setUnderlined(false);
			hyperlink.addHyperlinkListener(new IHyperlinkListener(){

				public void linkActivated(HyperlinkEvent e) {
					ErrorMessageSummaryPopupDialog.this.close();
					Stack nodes = new Stack();
					Node parentNode = currentTreeNodeElement.element.getParentNode();
					while(parentNode.getNodeType() == Node.ELEMENT_NODE) {
						nodes.push(parentNode);
						parentNode = parentNode.getParentNode();
					}
					while(!nodes.empty()) {
						ddeViewer.getTreeViewer().setExpandedState(nodes.pop(), true);
					}
					IStructuredSelection selection = new StructuredSelection(currentTreeNodeElement.element);
					ddeViewer.getTreeViewer().setSelection(selection);
				}

				public void linkEntered(HyperlinkEvent e) {
				}

				public void linkExited(HyperlinkEvent e) {
				}
			});
			
			new Label(dialogComposite, SWT.NONE); // filler
			
			Composite treeNodeContentComposite = new Composite(dialogComposite, SWT.NONE);
			gridLayout = new GridLayout();
			gridLayout.marginHeight = 0;
			gridLayout.marginWidth = 0;
			gridLayout.marginBottom = 5;
			treeNodeContentComposite.setLayout(gridLayout);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			treeNodeContentComposite.setLayoutData(gridData);
			// If there are no design errors but there are source errors, then don't show anything
			if (errorCount > 0 || !sourceViewContainsErrors)
			createTreeNodeMessageContent(currentTreeNodeElement, treeNodeContentComposite, errorCount > 0, errorCount == 0);
		}
		
		if(ddeViewer.getSourceViewContainsErrors() && errorCount == 0) {
			// The source view error message will get created here
			
			Label treeNodeImageLabel = new Label(dialogComposite, SWT.NONE);
			treeNodeImageLabel.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			
			Text text = new Text(dialogComposite, SWT.READ_ONLY);
			text.setText(Messages.ERRORS_IN_SOURCE_VIEW);
			
			new Label(dialogComposite, SWT.NONE); //filler

			ImageHyperlink imageHyperlink = new ImageHyperlink(dialogComposite, SWT.NONE);
			imageHyperlink.setText(Messages.SWITCH_TO_SOURCE_VIEW);
			imageHyperlink.setImage(DDEPlugin.getDefault().getImage("icons/goto.gif"));
			
			imageHyperlink.addHyperlinkListener(new IHyperlinkListener(){

				public void linkActivated(HyperlinkEvent e) {
					ErrorMessageSummaryPopupDialog.this.close();
					Node firstSourceViewErrorNode = ddeViewer.getFirstSourceViewErrorNode();
					((DDEMultiPageEditorPart)ddeViewer.getEditorPart()).setEditorActivePage(DDEMultiPageEditorPart.SOURCE_VIEW_PAGE);
					((DDEMultiPageEditorPart)ddeViewer.getEditorPart()).setSelection(firstSourceViewErrorNode);
				}

				public void linkEntered(HyperlinkEvent e) {
				}

				public void linkExited(HyperlinkEvent e) {
				}
			});

		}
		
		dialogScrolledComposite.setContent(dialogComposite);
		dialogScrolledComposite.setMinSize(dialogComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return dialogScrolledComposite;
	}

	
	private void createTreeNodeMessageContent(final TreeNodeElement treeNodeElement, Composite composite, boolean includeErrors, boolean includeWarnings) {
		Composite contentComposite = new Composite(composite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.numColumns = 4;
		gridLayout.verticalSpacing = 2;
		//gridLayout.horizontalSpacing = 4;
		gridLayout.horizontalSpacing = 0;
		contentComposite.setLayout(gridLayout);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		contentComposite.setLayoutData(gridData);

		// Missing required items
		CMElementDeclaration[] treeNodeMissingRequiredChildren = ddeViewer.getValidationManager().getMessageManager().getTreeNodeMissingRequiredChildren(treeNodeElement.element);

		// If there is one or more missing required items, show them in a list with quickfixes
		if(treeNodeMissingRequiredChildren.length > 0) {
			
			Label label = new Label(contentComposite, SWT.NONE);
			label.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			
			label = new Label(contentComposite, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalIndent = 1;
			label.setLayoutData(gridData);
			
			Text text = new Text(contentComposite, SWT.READ_ONLY);
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			text.setLayoutData(gridData);
			
			if(treeNodeMissingRequiredChildren.length == 1) {
				text.setText(Messages.SINGLE_REQUIRED_ITEM_MISSING);	
			} else {
				text.setText(Messages.MULTIPLE_REQUIRED_ITEMS_MISSING);
			}
			
			
			for (int i = 0; i < treeNodeMissingRequiredChildren.length; i++) {
				final CMElementDeclaration cmElementDeclaration = treeNodeMissingRequiredChildren[i];
				
				String missingTreeNodeLabel = null;
				Image missingTreeNodeIcon = null;
				
				String namespace = ModelUtil.getNamespaceURI(cmElementDeclaration);
				String path = ModelUtil.getNodeFullPath(treeNodeElement.element, cmElementDeclaration);
				Customization customization = ddeViewer.getCustomization();
				if (customization!=null)
				{
					DetailItemCustomization detailItemCustomization = ddeViewer.getCustomization().getItemCustomization(namespace, path);
					if(detailItemCustomization == null)
						detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration,path);
					
					if(detailItemCustomization != null) {
						missingTreeNodeLabel = detailItemCustomization.getLabel();
						missingTreeNodeIcon = detailItemCustomization.getIcon();
						//if there is no icon attribute try iconClass
						if(missingTreeNodeIcon == null) {
							Object iconClass;
							if(detailItemCustomization.getIconClass()!=null) {
								try {
									iconClass = detailItemCustomization.getIconClass().newInstance();
									if(iconClass instanceof ICustomIconObject)
									{
    										ICustomIconObject iconClassObj = (ICustomIconObject)iconClass;
    										iconClassObj.getClass().newInstance();
    										missingTreeNodeIcon = iconClassObj.getIcon(cmElementDeclaration, null);
									}
								} catch (IllegalAccessException e1) {
									e1.printStackTrace();
								} catch (InstantiationException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
					else
					{
					    Object iconClass;
						if(customization.getIconClass()!=null) {
						        try {
						                iconClass = detailItemCustomization.getIconClass().newInstance();
						                if(iconClass instanceof ICustomIconObject)
						                {
						                	ICustomIconObject iconClassObj = (ICustomIconObject)iconClass;
						                	iconClassObj.getClass().newInstance();
						                	missingTreeNodeIcon = iconClassObj.getIcon(cmElementDeclaration, null);
						                }
						        } catch (IllegalAccessException e1) {
						                e1.printStackTrace();
						        } catch (InstantiationException e1) {
						                e1.printStackTrace();
						        }
						}
					}
				}
				if(missingTreeNodeLabel == null) {
					missingTreeNodeLabel = cmElementDeclaration.getElementName();
				}
				if(missingTreeNodeIcon == null) {
					missingTreeNodeIcon = DDEPlugin.getDefault().getImage("icons/parameters.gif");
				}
				
				label = new Label(contentComposite, SWT.NONE); // Filler
				
				Composite missingItemComposite = new Composite(contentComposite, SWT.NONE);
				gridLayout = new GridLayout();
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				gridLayout.numColumns = 3;
				gridLayout.horizontalSpacing = 1;
				missingItemComposite.setLayout(gridLayout);
				gridData = new GridData();
				gridData.horizontalSpan = 2;
				missingItemComposite.setLayoutData(gridData);
				
				label = new Label(missingItemComposite, SWT.NONE);
				label.setImage(missingTreeNodeIcon);
				gridData = new GridData();
				gridData.horizontalIndent = 5;
				label.setLayoutData(gridData);
				
				label = new Label(missingItemComposite, SWT.NONE);
				gridData = new GridData();
				label.setLayoutData(gridData);
				
				Text missingItemText = new Text(missingItemComposite, SWT.READ_ONLY);
				missingItemText.setText(missingTreeNodeLabel);
				gridData = new GridData();
				missingItemText.setLayoutData(gridData);

				ImageHyperlink imageHyperlink = ddeViewer.getWidgetFactory().createImageHyperlink(contentComposite, SWT.NONE);
				imageHyperlink.setText(Messages.LABEL_ADD);
				imageHyperlink.setImage(DDEPlugin.getDefault().getImage("icons/quickfix_error.gif"));
				gridData = new GridData();
				gridData.horizontalIndent = 50;
				
				imageHyperlink.setLayoutData(gridData);
				imageHyperlink.addHyperlinkListener(new IHyperlinkListener(){

					public void linkActivated(HyperlinkEvent e) {
						
						AddElementAction addElementAction = new AddElementAction(treeNodeElement.element, cmElementDeclaration, ddeViewer, ddeViewer.getEditorPart(), ddeViewer.getCustomization());
						addElementAction.run();
						ErrorMessageSummaryPopupDialog.this.close();
					}

					public void linkEntered(HyperlinkEvent e) {
					}

					public void linkExited(HyperlinkEvent e) {
					}
				});
			}
		}

		// Detail Item error/warnings
		DetailItemValidation[] detailItemValidations = ddeViewer.getValidationManager().getMessageManager().getTreeNodeDetailItemMessages(treeNodeElement.element, includeErrors, includeWarnings);
		
		// Sort the validation items by their associated control labels
		Comparator comparator = new Comparator() {
			public int compare(Object arg0, Object arg1) {
				DetailItem detailItem_1 = ((DetailItemValidation)arg0).getDetailItem();
				DetailItem detailItem_2 = ((DetailItemValidation)arg1).getDetailItem();
				String name_1 = getControlLabel(detailItem_1);
				String name_2 = getControlLabel(detailItem_2);
				return Collator.getInstance().compare(name_1, name_2);
			}
		};
		Arrays.sort(detailItemValidations, comparator);
		
		
		for (int j = 0; j < detailItemValidations.length; j++) {
			DetailItemValidation detailItemValidation = detailItemValidations[j];
			
			//Check to see if there is any custom messages
			ValidationMessage[] customizedMessages = ddeViewer.getValidationManager().getMessageManager().getMessages(treeNodeElement.element, includeErrors, includeWarnings);
			
			final DetailItem detailItem = detailItemValidation.getDetailItem();
			final SimpleDetailItem containingSimpleDetailItem = detailItemValidation.getContainingSimpleDetailItem();
			ValidationMessage message= detailItemValidation.getMessage();
			if(message!=null && message.getMessage().equals("") && customizedMessages != null)
				createCustomMessages(message, customizedMessages,detailItem, contentComposite);
			else{
				String controlLabel = getControlLabel(detailItem);
				
				Label iconLabel = new Label(contentComposite, SWT.NONE);
				switch(message.getMessageType()) {
				case ValidationMessage.MESSAGE_TYPE_ERROR:
					iconLabel.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
					break;
				case ValidationMessage.MESSAGE_TYPE_WARNING:
					iconLabel.setImage(DDEPlugin.getDefault().getImage("icons/warning_small.gif"));
					break;
				}
				gridData = new GridData();
				gridData.verticalAlignment = SWT.TOP;
				iconLabel.setLayoutData(gridData);
		
	
				Link link = new Link(contentComposite, SWT.NONE);
				
				link.setText(MessageFormat.format(Messages.LABEL_COLON, "<a>"+controlLabel+"</a>"));
				
				gridData = new GridData();
				gridData.horizontalIndent = 4;
				gridData.verticalAlignment = SWT.TOP;
				link.setLayoutData(gridData);
				
				link.addSelectionListener(new SelectionListener(){
	
					public void widgetDefaultSelected(SelectionEvent e) {
					}
	
					public void widgetSelected(SelectionEvent e) {
						ErrorMessageSummaryPopupDialog.this.close();
						Stack nodes = new Stack();
						Node parentNode = treeNodeElement.element.getParentNode();
						while(parentNode.getNodeType() == Node.ELEMENT_NODE) {
							nodes.push(parentNode);
							parentNode = parentNode.getParentNode();
						}
						while(!nodes.empty()) {
							ddeViewer.getTreeViewer().setExpandedState(nodes.pop(), true);
						}
						IStructuredSelection selection = new StructuredSelection(treeNodeElement.element);
						ddeViewer.getTreeViewer().setSelection(selection);
						Node node = null;
						if(detailItem instanceof AtomicDetailItem) {
							AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
							node = atomicDetailItem.getNode();
						}
						if(node != null) {
							selection = new StructuredSelection(node);
						} else {
							selection = new StructuredSelection(ModelUtil.getDetailItemLocalPath(containingSimpleDetailItem, detailItem));
						}
						ddeViewer.getDetailsViewer().setSelection(selection, true);
						
					}});

					Text text = new Text(contentComposite, SWT.READ_ONLY | SWT.WRAP);
					text.setText(message.getMessage());
					gridData = new GridData();
					gridData.horizontalSpan = 2;
					text.setLayoutData(gridData);
			}
		}
		
		// TreeNode errors
		ValidationMessage[] treeNodeMessages = ddeViewer.getValidationManager().getMessageManager().getTreeNodeMessages(treeNodeElement.element, includeErrors, includeWarnings);
		for (int i = 0; i < treeNodeMessages.length; i++) {
			ValidationMessage currentValidationMessage = treeNodeMessages[i];
			
			Label label = new Label(contentComposite, SWT.NONE);
			if(currentValidationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_ERROR) {		
				label.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			} else if(currentValidationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_WARNING) {		
				label.setImage(DDEPlugin.getDefault().getImage("icons/warning_small.gif"));
			}
			
			label = new Label(contentComposite, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalIndent = 1;
			label.setLayoutData(gridData);

			Text text = new Text(contentComposite, SWT.READ_ONLY);
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			text.setLayoutData(gridData);
			text.setText(currentValidationMessage.getMessage());	
		}
		
	}
	//Helper Method to create any custom Messages for Liberty
	private void createCustomMessages(ValidationMessage message, ValidationMessage[] customizedMessages, DetailItem detailItem , Composite contentComposite)
	{
		String controlLabel = getControlLabel(detailItem);
		
		Label iconLabel = new Label(contentComposite, SWT.NONE);
		switch(message.getMessageType()) {
		case ValidationMessage.MESSAGE_TYPE_ERROR:
			iconLabel.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			break;
		case ValidationMessage.MESSAGE_TYPE_WARNING:
			iconLabel.setImage(DDEPlugin.getDefault().getImage("icons/warning_small.gif"));
			break;
		}
		Link link = new Link(contentComposite, SWT.NONE);
		link.setText("<a>" + controlLabel + "</a>:");
		 GridData gridData = new GridData();
			gridData.horizontalIndent = 1;
			//label.setLayoutData(gridData);
		
		if(message.getMessage().equals("") && customizedMessages != null)
		{
			for(int i = 0; i<customizedMessages.length; i++)
			{
				Label label = new Label(contentComposite, SWT.NONE);
				gridData = new GridData();
				gridData.horizontalIndent = 1;
				label.setLayoutData(gridData);

				Text text = new Text(contentComposite, SWT.READ_ONLY);
				gridData = new GridData();
				gridData.horizontalSpan = 2;
				text.setLayoutData(gridData);
				text.setText(customizedMessages[i].getMessage());	
			}
		}
	}
	
	private TreeNodeElement[] obtainSortedTreeNodeElements(boolean withErrors, boolean withWarnings) {
		Element[] elements = ddeViewer.getValidationManager().getMessageManager().getTreeNodeElements(true, withErrors, withWarnings);
		TreeNodeElement[] treeNodeElements = new TreeNodeElement[elements.length];
		for (int i = 0; i < elements.length; i++) {
			treeNodeElements[i] = new TreeNodeElement(elements[i]);
		}
		Arrays.sort(treeNodeElements, new Comparator(){
			public int compare(Object arg0, Object arg1) {
				TreeNodeElement treeNodeElement0 = (TreeNodeElement)arg0;
				TreeNodeElement treeNodeElement1 = (TreeNodeElement)arg1;
				return treeNodeElement0.treeNodeLabel.compareTo(treeNodeElement1.treeNodeLabel);
			}
		});
		return treeNodeElements;
	}
	
	
	private class TreeNodeElement {
		String treeNodeLabel;
		Image treeNodeImage;
		Element element;
		public TreeNodeElement(Element element) {
			this.element = element;
			treeNodeLabel = ddeViewer.getTreeLabelProvider().getText(element);
			treeNodeImage = ddeViewer.getTreeLabelProvider().getImage(element);
		}
	}
	
	

	private String getControlLabel(DetailItem detailItem) {
		String controlLabel = null;
		DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
		if(detailItemCustomization != null) {
			controlLabel = detailItemCustomization.getLabel();
		}
		if(controlLabel == null) {
			controlLabel = detailItem.getName();
		}
		if(detailItem instanceof AtomicElementDetailItem) {
			AtomicElementDetailItem atomicElementDetailItem = (AtomicElementDetailItem)detailItem;
			if(ModelUtil.isCMNodeRepeatable(atomicElementDetailItem.getCMNode())) {
				if(detailItemCustomization == null || (detailItemCustomization!= null && !detailItemCustomization.isSingleOccurrence() && detailItemCustomization.getStyle() == DetailItemCustomization.STYLE_DEFAULT)) {
					if (!ddeViewer.getCustomization().getHideRepeatableItemNumbers())
					{
						MessageFormat messageFormat = new MessageFormat(Messages.ITEM);
						String item = messageFormat.format(new Integer[]{new Integer(ModelUtil.getRepeatableElementIndex((Element)atomicElementDetailItem.getNode()) + 1)});
						controlLabel += " (" + item + ")";
					}
				}
			}
		}
		
		return controlLabel;
	}

}

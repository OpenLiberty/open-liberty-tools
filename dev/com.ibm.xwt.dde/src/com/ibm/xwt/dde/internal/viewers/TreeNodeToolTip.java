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
import java.util.Stack;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomIconObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.internal.actions.AddElementAction;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.DetailItemValidation;
import com.ibm.xwt.dde.internal.validation.ValidationManager;

public class TreeNodeToolTip extends ToolTip {

	private Element treeNodeElement;
	private ValidationManager validationManager;
	private Customization customization;
	private DDEViewer ddeViewer;
	private IEditorPart editorPart;
	private TabbedPropertySheetWidgetFactory widgetFactory;
	
	
	public TreeNodeToolTip(Control control, Element treeNodeElement, DDEViewer ddeViewer) {
		super(control, ToolTip.NO_RECREATE, true);
		this.treeNodeElement = treeNodeElement;
		this.validationManager = ddeViewer.getValidationManager();
		this.customization = ddeViewer.getCustomization();
		this.editorPart = ddeViewer.getEditorPart();
		this.widgetFactory = ddeViewer.getWidgetFactory();
		this.ddeViewer = ddeViewer;
	}

	protected Composite createToolTipContentArea(Event event, final Composite parent) {
		Composite toolTipMainComposite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 1;
		toolTipMainComposite.setLayout(gridLayout);
		
		// ToolTip headerComposite (title)
		Composite headerComposite = new Composite(toolTipMainComposite, SWT.NONE);
		headerComposite.setBackground(widgetFactory.getColors().getBackground());
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		headerComposite.setLayoutData(gridData);
		gridLayout = new GridLayout();
		gridLayout.marginRight = 2;
		gridLayout.marginLeft = 2;
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 1;
		gridLayout.horizontalSpacing = 2;
		headerComposite.setLayout(gridLayout);

		// ToolTip headerIcon
		Label headerIconLabel = new Label(headerComposite, SWT.NONE);
		headerIconLabel.setBackground(widgetFactory.getColors().getBackground());
		headerIconLabel.setImage(ddeViewer.getTreeLabelProvider().getImage(treeNodeElement, true));
		
		// ToolTip headerLabel
		Label headerLabel = new Label(headerComposite, SWT.NONE);
		headerLabel.setBackground(widgetFactory.getColors().getBackground());
		headerLabel.setText(ddeViewer.getTreeLabelProvider().getText(treeNodeElement));
		headerLabel.setFont(DDEPlugin.getDefault().FONT_DEFAULT_BOLD);

		// ToolTip content composite		
		final Composite toolTipContentComposite = new Composite(toolTipMainComposite, SWT.NONE);
		toolTipContentComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.marginTop = 0;
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 3;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 1;
		toolTipContentComposite.setLayout(gridLayout);

		int treeNodeErrorCount = validationManager.getMessageManager().getTreeNodeMessageCount(treeNodeElement, ValidationMessage.MESSAGE_TYPE_ERROR, false);
		
		// Missing required items
		CMElementDeclaration[] treeNodeMissingRequiredChildren = validationManager.getMessageManager().getTreeNodeMissingRequiredChildren(treeNodeElement);

		// If there is one or more missing required items, show them in a list with quickfixes
		if(treeNodeMissingRequiredChildren.length > 0) {
			
			Label label = new Label(toolTipContentComposite, SWT.NONE);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			label.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			
			label = new Label(toolTipContentComposite, SWT.NONE);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.horizontalIndent = 3;
			label.setLayoutData(gridData);
			
			
			if(treeNodeMissingRequiredChildren.length == 1) {
				label.setText(Messages.SINGLE_REQUIRED_ITEM_MISSING);	
			} else {
				label.setText(Messages.MULTIPLE_REQUIRED_ITEMS_MISSING);
			}
			
			for (int i = 0; i < treeNodeMissingRequiredChildren.length; i++) {
				final CMElementDeclaration cmElementDeclaration = treeNodeMissingRequiredChildren[i];
				
				String missingTreeNodeLabel = null;
				Image missingTreeNodeIcon = null;
				
				String namespace = ModelUtil.getNamespaceURI(cmElementDeclaration);
				String path = ModelUtil.getNodeFullPath(treeNodeElement, cmElementDeclaration);
				DetailItemCustomization detailItemCustomization = customization != null ? customization.getItemCustomization(namespace, path) : null;
				if(detailItemCustomization == null && customization!=null)
					detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration,path);
				if(detailItemCustomization != null) {
					missingTreeNodeLabel = detailItemCustomization.getLabel();
					missingTreeNodeIcon = detailItemCustomization.getIcon();
					//if there is no icon attribute try iconClass
					if(missingTreeNodeIcon==null)
					{
						Object iconClass;
						if(detailItemCustomization.getIconClass()!=null)
						{
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
                       if(customization.getIconClass()!=null)
                       {
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
				if(missingTreeNodeLabel == null) {
					missingTreeNodeLabel = cmElementDeclaration.getElementName();
				}
				if(missingTreeNodeIcon == null) {
					missingTreeNodeIcon = DDEPlugin.getDefault().getImage("icons/parameters.gif");
				}
				
				label = new Label(toolTipContentComposite, SWT.NONE); // Filler
				
				label = new Label(toolTipContentComposite, SWT.NONE);
				label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				label.setImage(missingTreeNodeIcon);
				gridData = new GridData();
				gridData.horizontalIndent = 5;
				label.setLayoutData(gridData);
				
				label = new Label(toolTipContentComposite, SWT.NONE);
				label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				label.setText(missingTreeNodeLabel);
				gridData = new GridData();
				gridData.horizontalIndent = 6;
				label.setLayoutData(gridData);

				ImageHyperlink imageHyperlink = widgetFactory.createImageHyperlink(toolTipContentComposite, SWT.NONE);
				imageHyperlink.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				imageHyperlink.setText("Add");
				imageHyperlink.setImage(DDEPlugin.getDefault().getImage("icons/quickfix_error.gif"));
				gridData = new GridData();
				gridData.horizontalIndent = 40;
				gridData.horizontalAlignment = SWT.RIGHT;
				imageHyperlink.setLayoutData(gridData);
				
				imageHyperlink.addHyperlinkListener(new IHyperlinkListener(){

					public void linkActivated(HyperlinkEvent e) {
						
						AddElementAction addElementAction = new AddElementAction(treeNodeElement, cmElementDeclaration, ddeViewer, editorPart, customization);
						addElementAction.run();
						TreeNodeToolTip.this.hide();
					}

					public void linkEntered(HyperlinkEvent e) {
					}

					public void linkExited(HyperlinkEvent e) {
					}
				});
			}
		}
		
		
		
		
		
		// If there are errors in the detail items associated, show them with links to the controls
		DetailItemValidation[] detailItemValidations = new DetailItemValidation[0];
		if(treeNodeErrorCount > 0) {
			detailItemValidations = validationManager.getMessageManager().getTreeNodeDetailItemMessages(treeNodeElement, true, false);
		} else {
			detailItemValidations = validationManager.getMessageManager().getTreeNodeDetailItemMessages(treeNodeElement, false, true);
		}
	
		for (int j = 0; j < detailItemValidations.length; j++) {
			DetailItemValidation detailItemValidation = detailItemValidations[j];
			final DetailItem detailItem = detailItemValidation.getDetailItem();
			final SimpleDetailItem containingSimpleDetailItem = detailItemValidation.getContainingSimpleDetailItem();
			ValidationMessage message= detailItemValidation.getMessage();
			String controlLabel = null;
			DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
			if(detailItemCustomization != null) {
				controlLabel = detailItemCustomization.getLabel();
			}
			if(controlLabel == null) {
				controlLabel = detailItem.getName();
			}

			Label label = new Label(toolTipContentComposite, SWT.NONE);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			switch(message.getMessageType()) {
			case ValidationMessage.MESSAGE_TYPE_WARNING:
				label.setImage(DDEPlugin.getDefault().getImage("icons/warning_small.gif"));
				break;
			case ValidationMessage.MESSAGE_TYPE_ERROR:
				label.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
				break;
			}
			
			String hyperLinkLabel = MessageFormat.format(Messages.LABEL_COLON_SPACE, new Object[]{controlLabel, message.getMessage()});
			
			Hyperlink hyperlink = widgetFactory.createHyperlink(toolTipContentComposite, hyperLinkLabel, SWT.NONE);
			
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.horizontalIndent = 2;

			hyperlink.setLayoutData(gridData);
			hyperlink.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			hyperlink.addHyperlinkListener(new IHyperlinkListener(){

				public void linkActivated(HyperlinkEvent e) {
					TreeNodeToolTip.this.hide();
					
					Stack nodes = new Stack();
					Node parentNode = treeNodeElement.getParentNode();
					while(parentNode.getNodeType() == Node.ELEMENT_NODE) {
						nodes.push(parentNode);
						parentNode = parentNode.getParentNode();
					}
					while(!nodes.empty()) {
						ddeViewer.getTreeViewer().setExpandedState(nodes.pop(), true);
					}
					
					IStructuredSelection selection = new StructuredSelection(treeNodeElement);
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
				}

				public void linkEntered(HyperlinkEvent e) {
				}

				public void linkExited(HyperlinkEvent e) {
				}
			});
		}
		
		
		
		
		
		// Obtain child elements with errors/warnings
		//Element[] childTreeNodeElements = validationManager.getMessageManager().getChildTreeNodeElements(treeNodeElement, true, true, true, false);
		Element[] childTreeNodeElements = new Element[0];
		if(treeNodeErrorCount > 0) {
			childTreeNodeElements = validationManager.getMessageManager().getChildTreeNodeElements(treeNodeElement, false, false, true, false);
		} else {
			childTreeNodeElements = validationManager.getMessageManager().getChildTreeNodeElements(treeNodeElement, false, false, false, true);
		}
		
		
		if(childTreeNodeElements.length > 0) {
			
			Label label = new Label(toolTipContentComposite, SWT.NONE);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			if(treeNodeErrorCount > 0) {
				label.setImage(DDEPlugin.getDefault().getImage("icons/error.gif"));
			} else {
				label.setImage(DDEPlugin.getDefault().getImage("icons/warning_small.gif"));
			}
			
			label = new Label(toolTipContentComposite, SWT.NONE);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.horizontalIndent = 3;
			label.setLayoutData(gridData);
			
			
			if(childTreeNodeElements.length == 1) {
				label.setText(treeNodeErrorCount > 0? Messages.THE_FOLLOWING_ITEM_CONTAIN_ERRORS : Messages.THE_FOLLOWING_ITEM_CONTAIN_WARNINGS);
			} else {
				label.setText(treeNodeErrorCount > 0? Messages.THE_FOLLOWING_ITEMS_CONTAIN_ERRORS : Messages.THE_FOLLOWING_ITEMS_CONTAIN_WARNINGS);
			}
			
			for (int i = 0; i < childTreeNodeElements.length; i++) {
				final Element childTreeNodeElement = childTreeNodeElements[i];

				Image elementImage = ddeViewer.getTreeLabelProvider().getImage(childTreeNodeElement);
				String elementLabel = ddeViewer.getTreeLabelProvider().getText(childTreeNodeElement);
				
				label = new Label(toolTipContentComposite, SWT.NONE); // Filler
			
				ImageHyperlink imageHyperlink = widgetFactory.createImageHyperlink(toolTipContentComposite, SWT.NONE);
				imageHyperlink.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				imageHyperlink.setImage(elementImage);
				imageHyperlink.setText(elementLabel);
				
				gridData = new GridData();
				gridData.horizontalSpan = 3;
				gridData.horizontalIndent = 5;
				imageHyperlink.setLayoutData(gridData);
				imageHyperlink.addHyperlinkListener(new IHyperlinkListener(){

					public void linkActivated(HyperlinkEvent e) {
						IStructuredSelection selection = new StructuredSelection(childTreeNodeElement);
						Stack nodes = new Stack();
						Node parentNode = childTreeNodeElement.getParentNode();
						while(parentNode.getNodeType() == Node.ELEMENT_NODE) {
							nodes.push(parentNode);
							parentNode = parentNode.getParentNode();
						}
						while(!nodes.empty()) {
							ddeViewer.getTreeViewer().setExpandedState(nodes.pop(), true);
						}
						ddeViewer.getTreeViewer().setSelection(selection);
						ddeViewer.getTreeViewer().getTree().setFocus();

						TreeNodeToolTip.this.hide();
					}

					public void linkEntered(HyperlinkEvent e) {
					}

					public void linkExited(HyperlinkEvent e) {
					}
				});
				
				
			}
		}
		
		
		return toolTipContentComposite;
	}

	protected void afterHideToolTip(Event event) {
	}
	

}

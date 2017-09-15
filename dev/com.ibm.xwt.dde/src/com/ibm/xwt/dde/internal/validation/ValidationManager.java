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

package com.ibm.xwt.dde.internal.validation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.validation.internal.operations.LocalizedMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl.XSDAttributeUseAdapter;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl.XSDElementDeclarationAdapter;
import org.eclipse.xsd.XSDAttributeDeclaration;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.ICustomItemValidationObject;
import com.ibm.xwt.dde.customization.ICustomListValidationObject;
import com.ibm.xwt.dde.customization.ICustomNodeValidationObject;
import com.ibm.xwt.dde.customization.ICustomPossibleValuesObject;
import com.ibm.xwt.dde.customization.ICustomTableItemValidationObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.RepeatableAtomicDetailItemSet;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.viewers.DetailsContentProvider;

public class ValidationManager {

	public static final int EDITOR_VALIDATION = 0;
	public static final int WORKBENCH_VALIDATION = 1;
	
	private Customization customization;
	private IEditorInput editorInput;
	private DetailsContentProvider detailsContentProvider;
	private MessageManager messageManager;
	private Document document;
	private IResource resource;
	private DataTypeValidator dataTypeValidator;
	private int validationEnviroment;
	boolean isCustomListValidation = false;
	
	
	public ValidationManager(Customization customization, IResource resource, int validationEnviroment) {
		this.customization = customization;
		this.resource = resource;
		detailsContentProvider = new DetailsContentProvider(customization);
		messageManager = new MessageManager();
		dataTypeValidator = new DataTypeValidator();
		this.validationEnviroment = validationEnviroment;
	}
	
	
	public void setDocument(Document document) {
		this.document = document;
	}
	
	public void setEditorInput(IEditorInput editor) {
		this.editorInput = editor;
	}
	
	public IEditorInput getEditorInput() {
		return editorInput;
	}

	
	public void validateDocument() {
		if(document != null) {
			messageManager.clearMessages();
			Element documentElement = document.getDocumentElement();
			if(documentElement != null) {
				validateTreeNode(documentElement, true, false, true);
			}
		}		
	}
	
	
	public void validateDetailItem(Element containingTreeNodeElement, DetailItem detailItem, boolean skipTriggeredValidations) {
		validateDetailItem(containingTreeNodeElement, null, detailItem, skipTriggeredValidations);
	}
	
	
	public void validateDetailItem(Element containingTreeNodeElement, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem, boolean skipTriggeredValidations) {
		if(detailItem instanceof AtomicDetailItem) {
			AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
			validateAtomicDetailItem(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem);
		} else if(detailItem instanceof RepeatableAtomicDetailItemSet) {
			RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItem;
			validateRepeatableAtomicDetailItemSet(containingTreeNodeElement, containingSimpleDetailItem, repeatableAtomicDetailItemSet);
		} else if(detailItem instanceof SimpleDetailItem) {
			SimpleDetailItem simpleDetailItem = (SimpleDetailItem)detailItem;
			validateSimpleDetailItem(containingTreeNodeElement, simpleDetailItem, skipTriggeredValidations);
		}
		
		// Triggered validations
		if(!skipTriggeredValidations) {
			DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
			if(detailItemCustomization != null) {
				String[] triggerValidationPath = detailItemCustomization.getTriggerValidationPath();
				boolean[] triggerValidationRecurse = detailItemCustomization.isTrigerNodeValidationRecurse();
				for (int i = 0; i < triggerValidationPath.length; i++) {
					Node[] instances = ModelUtil.getInstances(document, triggerValidationPath[i]);
					for (int j = 0; j < instances.length; j++) {
						if(instances[j].getNodeType() == Node.ELEMENT_NODE) {
							Element treeNodeElement = (Element)instances[j];
							boolean recurse = true;
							if(triggerValidationRecurse.length == 1) {
								recurse = triggerValidationRecurse[0];
							} else if(triggerValidationRecurse.length > i) {
								recurse = triggerValidationRecurse[i];
							}
							validateTreeNode(treeNodeElement, recurse, false, true);
						}
					}
				}
			}
		}
	}
	
	
	public void validateTreeNode(Element treeNodeElement, boolean recurse, boolean skipDetailItems, boolean skipTriggeredValidations) {
		Stack elementStack = new Stack();
		elementStack.push(treeNodeElement);
		while(!elementStack.isEmpty()) {
			Element currentElement = (Element)elementStack.pop();
			
			// Validate the tree node
			validateTreeNodeElementContents(currentElement, skipTriggeredValidations);
			
			// Validate the associated tree node detail items according to the skipDetailItems flag
			if(!skipDetailItems) {
				validateTreeNodeElementDetailItems(currentElement, skipTriggeredValidations);
			}
			
			// Recurse the tree nodes according to the recurse flag
			if(recurse) {
				List elementList = ModelUtil.getTreeChildElements(currentElement, customization);
				Iterator iterator = elementList.iterator();
				while (iterator.hasNext()) {
					Element childElement = (Element)iterator.next();
					elementStack.push(childElement);
				}
			}
		}
	}
	
	
	private void validateAtomicDetailItem(Element containingTreeNodeElement, SimpleDetailItem containingSimpleDetailItem, AtomicDetailItem atomicDetailItem) {
		messageManager.removeDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem);
		
		// If the atomic detail item is in an optional simple detail item that does not exist, do not validate.
		if(containingSimpleDetailItem != null) {
			boolean simpleDetailItemIsRequierd = containingSimpleDetailItem.isRequired();
			boolean simpleDetailItemExists = containingSimpleDetailItem.getElement() != null;
			if(!simpleDetailItemIsRequierd && !simpleDetailItemExists) {
				return;	
			}
		}
		
		// Obtain information on the atomic detail item to validate
		String itemName = null;
		Map possibleValues = null;
		boolean isRequired = atomicDetailItem.isRequired();
		Class validationClass = null;
		String value = atomicDetailItem.getValue();
		DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
		boolean skipSyntaxValidation = false;
		if(detailItemCustomization != null) {
			skipSyntaxValidation = detailItemCustomization.isSkipSyntaxValidation();
			itemName = detailItemCustomization.getLabel();
			possibleValues = detailItemCustomization.getPossibleValues();
			if(!isRequired) {
				isRequired = detailItemCustomization.isRequired();
			}
			validationClass = detailItemCustomization.getValidationClass();
			possibleValues = detailItemCustomization.getPossibleValues();
			 
			Class possibleValuesClass = detailItemCustomization.getPossibleValuesClass();
			if(possibleValuesClass != null) {
				try {
					Object object = possibleValuesClass.newInstance();
					if(object instanceof ICustomPossibleValuesObject) {
						ICustomPossibleValuesObject customPossibleValuesObject = (ICustomPossibleValuesObject)object;
						possibleValues = customPossibleValuesObject.getPosibleValues(value, atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
		if(itemName == null) {
			itemName = atomicDetailItem.getName();
		}
		if(possibleValues == null) {
			possibleValues = atomicDetailItem.getPossibleValues();
		}
		
		// Check if the item is required and it doesn't exist
		boolean optionalWithinContext = atomicDetailItem.isOptionalWithinContext();
		if(!optionalWithinContext && isRequired && atomicDetailItem.hasEditableValue() && !atomicDetailItem.exists()) {
			ValidationMessage message = new ValidationMessage(Messages.REQUIRED_ITEM_MISSING, ValidationMessage.MESSAGE_TYPE_ERROR);
			messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem, message);
			return;
		}
		
		// If the item exists, check its value
		if(atomicDetailItem.exists() && atomicDetailItem.hasEditableValue()) {
			
			if(!skipSyntaxValidation) {
				// If the item has possible values, check if the current value is among them
				ValidationMessage possibleValuesErrorMsg = null;
				if(possibleValues != null) {
					if(!possibleValues.containsValue(value)) {
						possibleValuesErrorMsg = new ValidationMessage(Messages.THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_SELECTIONS, ValidationMessage.MESSAGE_TYPE_ERROR);
						
						// REF 119662: Error shows for union of variableType and enumeration if value is a variable
						// At this point, if the value is not among the list of possible selections, then it is too quick/early to
						// flag it as an error because the value could be valid against a union of types.
					}
				}
				
				// Check the data type of the value
				CMNode cmNode = atomicDetailItem.getCMNode();
				XSDTypeDefinition xsdTypeDefinition = null;
				if(cmNode instanceof CMElementDeclaration) {
					CMElementDeclaration cmElementDeclaration = (CMElementDeclaration)cmNode;
					if(cmElementDeclaration instanceof XSDElementDeclarationAdapter) {
						XSDElementDeclarationAdapter xsdElementDeclarationAdapter = (XSDElementDeclarationAdapter)cmElementDeclaration;
						Notifier target = xsdElementDeclarationAdapter.getTarget();
						if(target instanceof XSDElementDeclaration) {
							XSDElementDeclaration xsdElementDeclaration = (XSDElementDeclaration)target;
							xsdTypeDefinition = xsdElementDeclaration.getResolvedElementDeclaration().getTypeDefinition();
						}
					}
				} else if(cmNode instanceof CMAttributeDeclaration) {
					CMAttributeDeclaration cmAttributeDeclaration = (CMAttributeDeclaration)cmNode;
					if(cmAttributeDeclaration instanceof XSDAttributeUseAdapter) {
						XSDAttributeUseAdapter xsdAttributeUseAdapter = (XSDAttributeUseAdapter)cmAttributeDeclaration;
						Notifier target = xsdAttributeUseAdapter.getTarget();
						if(target instanceof XSDAttributeUse) {
							XSDAttributeUse xsdAttributeUse = (XSDAttributeUse)target;
							XSDAttributeDeclaration xsdAttributeDeclaration = xsdAttributeUse.getAttributeDeclaration();
							if(xsdAttributeDeclaration != null) {
								xsdTypeDefinition = xsdAttributeDeclaration.getResolvedAttributeDeclaration().getTypeDefinition();
							}
						}
					}
				}
				if(xsdTypeDefinition != null) {
					isCustomListValidation = false;
					String errorDescription = dataTypeValidator.validateXSDTypeDefinition(xsdTypeDefinition, value);
					if(errorDescription != null) {
						// REF: 119662: Error shows for union of variableType and enumeration if value is a variable
						// In this case, two error conditions are reported:
						// #1. Error message returned by the data type validator, meaning, the value is not ok according to the grammar/XSD
						// #2. The value isn't part of the list of possible values
						// Which error message should we display? To not regress current behaviour, we should return message #2
						// because that was returned originally.
						
						// Otherwise, just return the message from the data type validator
						ValidationMessage message = new ValidationMessage(errorDescription, ValidationMessage.MESSAGE_TYPE_ERROR);
						messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem, message);
						return;
					}
				}
				else {
					// To cover the case if the value isn't part of the list of possible values, since we don't return right away.
					if (possibleValuesErrorMsg != null)
						messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem, possibleValuesErrorMsg);
				}
			}
			// If the item has a custom validation, perform it
			if(validationClass != null) {
				try {
					Object object = validationClass.newInstance();
					if(object instanceof ICustomItemValidationObject) {
						ICustomItemValidationObject customItemValidationObject = (ICustomItemValidationObject)object;
						ValidationMessage message = customItemValidationObject.validate(value, atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), resource);
						if(message != null) {
							messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem, message);
						}
					}
					//The custom Validation used when more than one messages are returned for each Node
					else if(object instanceof ICustomListValidationObject) {
						isCustomListValidation = true;
						ICustomListValidationObject customListValidationObject = (ICustomListValidationObject)object;
						messageManager.clearMessages();
						Map messageMap = customListValidationObject.validate(value, atomicDetailItem.getNode(), atomicDetailItem.getClosestAncestor(), editorInput);
						ValidationMessage[] messages = new ValidationMessage[20];
						if(messageMap != null && !messageMap.isEmpty()) {
							Iterator nodes = messageMap.keySet().iterator();
							do
							{
								//If the messageMap contains messages, For all messages in the message map, add the message to the messageManager
								Node item = (Node)nodes.next();
								messages = (ValidationMessage[])messageMap.get(item);
								if(item instanceof Element){
									for(int k = 0; k <messages.length; k++){
										messageManager.addCustomDetailItemMessage((Element)item, containingSimpleDetailItem, atomicDetailItem,(ValidationMessage)messages[k]);
									}
								}
								else if(item instanceof Attr)
								{
									Element owner= ((Attr) item).getOwnerElement();
									DetailItem[] detailItems = detailsContentProvider.getItems(owner);
									int count = detailItems.length;
									for(int j = 0;j<count;j++)
									{
										if(detailItems[j] instanceof AtomicDetailItem)
										{
											if(detailItems[j].getName().equals(((Attr)item).getName()))
											{	atomicDetailItem = (AtomicDetailItem)detailItems[j];}
													
										}
									}
									for(int k = 0; k <messages.length; k++){
										messageManager.addCustomDetailItemMessage(owner, containingSimpleDetailItem, atomicDetailItem,(ValidationMessage)messages[k]);
									}
								}
							}while(nodes.hasNext());
						}//If the validation didn't return any Validation messages, that means no error or warning, just remove the existing messages for this node from the map
						else{
							messageManager.clearMessages();
							messageManager.removeCustomDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, atomicDetailItem);
						}
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void validateSimpleDetailItem(Element containingTreeNodeElement, SimpleDetailItem simpleDetailItem, boolean skipTriggeredValidations) {
		DetailItem[] atomicDetailItems = simpleDetailItem.getAtomicDetailItems();
		for (int j = 0; j < atomicDetailItems.length; j++) {
			validateDetailItem(containingTreeNodeElement, simpleDetailItem, atomicDetailItems[j], skipTriggeredValidations);
		}
	}
	
	
	private void validateRepeatableAtomicDetailItemSet(Element containingTreeNodeElement, SimpleDetailItem containingSimpleDetailItem, RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet) {
		messageManager.removeDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, repeatableAtomicDetailItemSet);
		
		// If the atomic detail item is in an optional simple detail item that does not exist, do not validate.
		if(containingSimpleDetailItem != null) {
			boolean simpleDetailItemIsRequierd = containingSimpleDetailItem.isRequired();
			boolean simpleDetailItemExists = containingSimpleDetailItem.getElement() != null;
			if(!simpleDetailItemIsRequierd && !simpleDetailItemExists) {
				return;	
			}
		}
		
		String itemName = null;
		boolean isRequired = repeatableAtomicDetailItemSet.isRequired(); 
		Class validationClass = null;
		DetailItemCustomization detailItemCustomization = repeatableAtomicDetailItemSet.getDetailItemCustomization();
		if(detailItemCustomization != null) {
			itemName = detailItemCustomization.getLabel();
			if(!isRequired) {
				isRequired = detailItemCustomization.isRequired();
			}
			validationClass = detailItemCustomization.getValidationClass();
		}
		if(itemName == null) {
			itemName = repeatableAtomicDetailItemSet.getName();
		}
		
		// If the item is required but there are no entries, generate an error
		if(repeatableAtomicDetailItemSet.getItemCount() == 0 && isRequired) {
			ValidationMessage message = new ValidationMessage(Messages.THE_LIST_MUST_CONTAIN_AT_LEAST_ONE_ENTRY, ValidationMessage.MESSAGE_TYPE_ERROR);
			messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, repeatableAtomicDetailItemSet, message);
		} 
		AtomicDetailItem[] items = repeatableAtomicDetailItemSet.getItems();
		for (int i = 0; i < items.length; i++) {
			validateAtomicDetailItem(containingTreeNodeElement, containingSimpleDetailItem, items[i]);	
		}
		// If the item has a custom validation, perform it
		if(validationClass != null) {
			try {
				Object object = validationClass.newInstance();
				if(object instanceof ICustomTableItemValidationObject) { // If the custom validation object is for tables, invoke it.
					ICustomTableItemValidationObject customTableItemValidationObject = (ICustomTableItemValidationObject)object;
					items = repeatableAtomicDetailItemSet.getItems();
					String [] values = new String[items.length];
					Node[] itemNodes = new Node[items.length];
					for (int i = 0; i < items.length; i++) {
						AtomicDetailItem currentAtomicDetailItem = items[i];
						values[i] = currentAtomicDetailItem.getValue();
						itemNodes[i] = currentAtomicDetailItem.getNode();
					}
					Element closestAncestor = null;
					if(containingSimpleDetailItem != null) {
						closestAncestor = containingSimpleDetailItem.getElement();
					}
					if(closestAncestor == null) {
						closestAncestor = containingTreeNodeElement;
					}
					ValidationMessage validationMessage = customTableItemValidationObject.validate(values, itemNodes, closestAncestor, resource);
					if(validationMessage != null) {
						messageManager.addDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, repeatableAtomicDetailItemSet, validationMessage);
					}
				}
				//The custom Validation used when more than one messages are returned for each Node
				else if(object instanceof ICustomListValidationObject) {
					isCustomListValidation = true;
					messageManager.clearMessages();
					ICustomListValidationObject customListValidationObject = (ICustomListValidationObject)object;
					items = repeatableAtomicDetailItemSet.getItems();
					Element closestAncestor = null;
					if(containingSimpleDetailItem != null) {
						closestAncestor = containingSimpleDetailItem.getElement();
					}
					if(closestAncestor == null) {
						closestAncestor = containingTreeNodeElement;
					}
					int length = items.length;
					String [] values = new String[length];
					Node[] itemNodes = new Node[length];
					Map messageMap = new HashMap();
					for (int i = 0; i < length; i++) {
						AtomicDetailItem currentAtomicDetailItem = items[i];
						values[i] = currentAtomicDetailItem.getValue();
						itemNodes[i] = currentAtomicDetailItem.getNode();
						messageMap = customListValidationObject.validate(values[i], itemNodes[i], closestAncestor, editorInput);
						ValidationMessage[] messages = new ValidationMessage[20];
						if(messageMap != null && !messageMap.isEmpty()) {
							Iterator nodes = messageMap.keySet().iterator();
							do
							{
								//If the messageMap contains messages, For all messages in the message map, add the message to the messageManager
								Node item = (Node)nodes.next();
								messages = (ValidationMessage[])messageMap.get(item);
								if(item instanceof Element){
									for(int k = 0; k <messages.length; k++){
										messageManager.addCustomDetailItemMessage((Element)item, containingSimpleDetailItem, repeatableAtomicDetailItemSet,(ValidationMessage)messages[k]);
									}
								}
								else if(item instanceof Attr)
								{
									Element owner= ((Attr) item).getOwnerElement();
									DetailItem[] detailItems = detailsContentProvider.getItems(owner);
									int count = detailItems.length;
									for(int j = 0;j<count;j++)
									{
										if(detailItems[j] instanceof AtomicDetailItem)
										{
											if(detailItems[j].getName().equals(((Attr)item).getName()))
											{	repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItems[j];}
													
										}
									}
									for(int k = 0; k <messages.length; k++){
										messageManager.addCustomDetailItemMessage(owner, containingSimpleDetailItem, repeatableAtomicDetailItemSet,(ValidationMessage)messages[k]);
									}
								}
							}while(nodes.hasNext());
						}//If the validation didn't return any Validation messages, that means no error or warning, just remove the existing messages for this node from the map
						else{
							messageManager.clearMessages();
							messageManager.removeCustomDetailItemMessage(containingTreeNodeElement, containingSimpleDetailItem, repeatableAtomicDetailItemSet);
						}
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}
	}

	
	private void validateTreeNodeElementContents(Element treeNodeElement, boolean skipTriggeredValidations) {
		// Clear all the messages associated with the tree node
		messageManager.removeTreeNodeMessages(treeNodeElement);
		messageManager.removeTreeNodeMissingRequiredChildren(treeNodeElement);

		// Verify that all the required child elements are created
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(treeNodeElement.getOwnerDocument());
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(treeNodeElement);
		if(cmElementDeclaration != null) {
			List modelQueryInsertActions = new ArrayList();
			List availableContent = new ArrayList();
			modelQuery.getInsertActions(treeNodeElement, cmElementDeclaration, -1, ModelQuery.INCLUDE_CHILD_NODES, ModelQuery.VALIDITY_STRICT, modelQueryInsertActions);
			Iterator iterator = modelQueryInsertActions.iterator();
			while(iterator.hasNext()) {
				ModelQueryAction modelQueryAction = (ModelQueryAction)iterator.next();
				CMNode cmNode = modelQueryAction.getCMNode();
				availableContent.add(cmNode);
			}
			iterator = availableContent.iterator();
			while (iterator.hasNext()) {
				Object object = iterator.next();
				if(object instanceof CMElementDeclaration) {
					CMElementDeclaration childCMElementDeclaration = (CMElementDeclaration)object;
					if(ModelUtil.elementMustAppearInTree(customization, treeNodeElement, cmElementDeclaration, childCMElementDeclaration)) {
						if(ModelUtil.getInstancesOfElement(treeNodeElement, childCMElementDeclaration).length == 0) {
							boolean isRequired = false;
							int groupTypesInBetween = ModelUtil.getGroupTypesInBetween(cmElementDeclaration, childCMElementDeclaration);	
							if(childCMElementDeclaration.getMinOccur() > 0 && ((groupTypesInBetween & (ModelUtil.OPTIONAL)) == 0 && groupTypesInBetween != 0)) {
								isRequired = true;
							} else if(customization != null) {
								String namespace = ModelUtil.getNamespaceURI(childCMElementDeclaration);
								String path = ModelUtil.getNodeFullPath(treeNodeElement, childCMElementDeclaration);
								DetailItemCustomization detailItemCustomization = customization.getItemCustomization(namespace, path);
								if(detailItemCustomization == null)
									detailItemCustomization = customization.getTypeCustomizationConsideringUnions(childCMElementDeclaration,path);
								if(detailItemCustomization != null) {
									isRequired = detailItemCustomization.isRequired();
								}
							}
							if(isRequired && !isPresentBySubstitutionGroup(treeNodeElement, cmElementDeclaration, childCMElementDeclaration) && ! isPresentAndRequiredByGroupChoice(treeNodeElement, cmElementDeclaration, childCMElementDeclaration)) {
								messageManager.addMissingRequiredTreeNode(treeNodeElement, childCMElementDeclaration);
							}
						}
					}
				}
			}
			// Perform custom validation when applicable
			String namespace = ModelUtil.getNodeNamespace(treeNodeElement);
			String path = ModelUtil.getElementFullPath(treeNodeElement);
			DetailItemCustomization itemCustomization = null;
			if(customization != null) {
				itemCustomization = customization.getItemCustomization(namespace, path);
			}
			if(itemCustomization != null) {
				Class validationClass = itemCustomization.getValidationClass();
				if(validationClass != null) {
					try {
						Object object = validationClass.newInstance();
						if(object instanceof ICustomNodeValidationObject) {
							ICustomNodeValidationObject customNodeValidationObject = (ICustomNodeValidationObject)object;
							ValidationMessage[] message = customNodeValidationObject.validate(treeNodeElement, resource);
							if(message != null) {
								for (int i = 0; i < message.length; i++) {
									messageManager.addTreeNodeMessage(treeNodeElement, message[i]);
								}
							}
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Triggered validations
		if(!skipTriggeredValidations) {
			String namespace = ModelUtil.getNodeNamespace(treeNodeElement);
			String path = ModelUtil.getElementFullPath(treeNodeElement);
			DetailItemCustomization itemCustomization = null;
			if(customization != null) {
				itemCustomization = customization.getItemCustomization(namespace, path);
			}	
			if(itemCustomization != null) {
				String[] triggerValidationPath = itemCustomization.getTriggerValidationPath();
				boolean[] triggerValidationRecurse = itemCustomization.isTrigerNodeValidationRecurse();
				for (int i = 0; i < triggerValidationPath.length; i++) {
					Node[] instances = ModelUtil.getInstances(document, triggerValidationPath[i]);
					for (int j = 0; j < instances.length; j++) {
						if(instances[j].getNodeType() == Node.ELEMENT_NODE) {
							Element currrentTreeNodeElement = (Element)instances[j];
							boolean recurse = true;
							if(triggerValidationRecurse.length == 1) {
								recurse = triggerValidationRecurse[0];
							} else if(triggerValidationRecurse.length > i) {
								recurse = triggerValidationRecurse[i];
							}
							validateTreeNode(currrentTreeNodeElement, recurse, false, true);
						}
					}
				}
			}
		}
	}
	
	
	private boolean isPresentBySubstitutionGroup(Element parentElement, CMElementDeclaration cmElementDeclaration, CMElementDeclaration childCMElementDeclaration) {
		Object substitutionValueObject = childCMElementDeclaration.getProperty("SubstitutionGroupValue");
		if(substitutionValueObject instanceof String) {
			String substitutionValue = (String)substitutionValueObject;
			if(!"".equals(substitutionValue)) {
				CMNamedNodeMap localElements = cmElementDeclaration.getLocalElements();
				Iterator localElementsIterator = localElements.iterator();
				while(localElementsIterator.hasNext()) {
					Object localElementObject = localElementsIterator.next();
					if(localElementObject instanceof CMElementDeclaration) {
						CMElementDeclaration localElementCMElementDeclaration = (CMElementDeclaration)localElementObject;
						if(substitutionValue.equals(localElementCMElementDeclaration.getNodeName())) {
							Object substitutionGroupObject = localElementCMElementDeclaration.getProperty("SubstitutionGroup");
							if(substitutionGroupObject instanceof CMNodeList) {
								CMNodeList cmNodeList = (CMNodeList)substitutionGroupObject;
								int length = cmNodeList.getLength();
								if(length > 1) {
									for(int i = 0; i < length; i++) {
										CMNode cmNode = cmNodeList.item(i);
										if(cmNode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
											CMElementDeclaration substitutionCMElementDeclaration = (CMElementDeclaration)cmNode;
											if(ModelUtil.getInstancesOfElement(parentElement, substitutionCMElementDeclaration).length > 0) {
												return true;
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
		return false;
	}
	
	private boolean isPresentAndRequiredByGroupChoice(Element parentElement, CMElementDeclaration parentCMElementDeclaration, CMElementDeclaration childCMElementDeclaration) {
		CMGroup containingGroup = ModelUtil.getContainingGroup(parentCMElementDeclaration, childCMElementDeclaration);
		if(containingGroup != null) {
			if(containingGroup.getOperator() == CMGroup.CHOICE) {
				CMNodeList groupChildNodes = containingGroup.getChildNodes();
				for (int i = 0; i < groupChildNodes.getLength(); i++) {
					CMNode cmNode = groupChildNodes.item(i);
					if(cmNode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
						CMElementDeclaration cmElementDeclaration = (CMElementDeclaration)cmNode;
						if(ModelUtil.getInstancesOfElement(parentElement, cmElementDeclaration).length > 0 || cmElementDeclaration.getMinOccur() == 0) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private void validateTreeNodeElementDetailItems(Element treeNodeElement, boolean skipTriggeredValidations) {
		DetailItem[] detailItems = detailsContentProvider.getItems(treeNodeElement);
		for (int i = 0; i < detailItems.length; i++) {
			DetailItem detailItem = detailItems[i];
			validateDetailItem(treeNodeElement, detailItem, skipTriggeredValidations);
		}
	}
	
	public MessageManager getMessageManager() {
		return messageManager;
	}
	
	/* * * * * * * * * * * * * * * * * * * * Message Manager * * * * * * * * * * * * * * * * * * * */
	
	public class MessageManager {

		private HashMap messages;
		
		public MessageManager() {
			messages = new HashMap();
		}
		
		
		public void addTreeNodeMessage(Element treeNodeElement, ValidationMessage message) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode == null) {
				validationNode = new TreeNodeValidationNode(treeNodeElement);
				messages.put(treeNodeElement, validationNode);				
			}
			validationNode.treeNodeMessages.add(message);
			updateCountAndCascade(validationNode, message.getMessageType(), 1);
		}
		
		
		public void addMissingRequiredTreeNode(Element treeNodeParentElement, CMElementDeclaration cmElementDeclaration) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeParentElement);
			if(validationNode == null) {
				validationNode = new TreeNodeValidationNode(treeNodeParentElement);
				messages.put(treeNodeParentElement, validationNode);
			}
			validationNode.missingRequiredChildren.add(cmElementDeclaration);
			if(validationNode.missingRequiredChildren.size() == 1) {
				updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_ERROR, 1);
			}
		}
		
		
		public void addDetailItemMessage(Element treeNodeElement, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem, ValidationMessage message) {
			Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode == null) {
				validationNode = new TreeNodeValidationNode(treeNodeElement);
				messages.put(treeNodeElement, validationNode);				
			}
			DetailItemValidationNode detailItemValidationNode = new DetailItemValidationNode(detailItemKey, containingSimpleDetailItem, detailItem, message);
			validationNode.detailItemMessages.put(detailItemKey, detailItemValidationNode);
			updateCountAndCascade(validationNode, message.getMessageType(), 1);
		}
		
		public void addCustomDetailItemMessage(Element treeNodeElement, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem, ValidationMessage message) {
			Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode == null) {
				validationNode = new TreeNodeValidationNode(treeNodeElement);
				messages.put(treeNodeElement, validationNode);				
			}
			DetailItemValidationNode detailItemValidationNode = new DetailItemValidationNode(detailItemKey, containingSimpleDetailItem, detailItem, message);
			if(isCustomListValidation){
				validationNode.messageList.add(message);
				detailItemValidationNode.message = new ValidationMessage("", message.getMessageType());
				validationNode.detailItemMessages.put(detailItemKey, detailItemValidationNode);
			}
			else
			{
				validationNode.detailItemMessages.put(detailItemKey, detailItemValidationNode);
			}
			updateCountAndCascade(validationNode, message.getMessageType(), 1);
		}
		
		
		public void removeDetailItemMessage(Element treeNodeElement, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem) {
			Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey);
				if(detailItemValidationNode != null) {
					validationNode.detailItemMessages.remove(detailItemKey);
					updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -1);
				}
				Object detailItemLocalPath = getDetailItemKey(containingSimpleDetailItem, detailItem, true); 
				if(!detailItemKey.equals(detailItemLocalPath)) {
					detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemLocalPath);
					if(detailItemValidationNode != null) {
						validationNode.detailItemMessages.remove(detailItemLocalPath);
						updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -1);
					}
				}
			}
		}
		
		public void removeCustomDetailItemMessage(Element treeNodeElement, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem) {
			Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
			TreeNodeValidationNode validationNode = null;
			if(isCustomListValidation)
			{
				Iterator iterator = messages.keySet().iterator();
				while(iterator.hasNext()){
					Object obj = iterator.next();
					if(obj instanceof Node){
						validationNode = (TreeNodeValidationNode) messages.get(obj);
						int count = -1;
						if (validationNode !=null)
							count =  validationNode.messageList.size();
						if(count>0){
							DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey);
							if(detailItemValidationNode !=null){
								validationNode.messageList.clear();
								validationNode.detailItemMessages.remove(detailItemKey);
								if(detailItemValidationNode != null)
								{
									if(detailItemValidationNode.message != null)
										updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -count);
								}
							}
						}
					}
				}
			}else{
				validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			}
			if(validationNode != null) {
				int count =  validationNode.messageList.size();
				DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey);
				if(detailItemValidationNode != null) {
					int counter = -1;
					if(!isCustomListValidation)
					{
						if(((DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey)).message.getMessage().equals(""))
						{
							counter = -count;
							validationNode.messageList.clear();
						}
						validationNode.detailItemMessages.remove(detailItemKey);
						updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(),counter);
					}
					else{
						validationNode.messageList.clear();
						validationNode.detailItemMessages.remove(detailItemKey);
						updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -count);
					}
				}
				Object detailItemLocalPath = getDetailItemKey(containingSimpleDetailItem, detailItem, true); 
				if(!detailItemKey.equals(detailItemLocalPath)) {
					detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemLocalPath);
					if(detailItemValidationNode != null) {
						if(!isCustomListValidation)
						{
							validationNode.detailItemMessages.remove(detailItemKey);
							//validationNode.messageList.clear();
							updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -1);
						}
						else{
							validationNode.messageList.clear();
							updateCountAndCascade(validationNode, detailItemValidationNode.message.getMessageType(), -count);
						}
					}
				}
			}
		}
		
		
		public void removeTreeNodeDetailItemMessages(Element treeNodeElement) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				int detailItemErrorCount = 0;
				int detailItemWarningCount = 0;
				Collection values = validationNode.detailItemMessages.values();
				Iterator iterator = values.iterator();
				while(iterator.hasNext()) {
					ValidationMessage message = ((DetailItemValidationNode)iterator.next()).message;
					switch(message.getMessageType()) {
					case ValidationMessage.MESSAGE_TYPE_ERROR:
						detailItemErrorCount++;
						break;
					case ValidationMessage.MESSAGE_TYPE_WARNING:
						detailItemWarningCount++;
						break;
					}
				}
				validationNode.detailItemMessages.clear();
				updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_ERROR, -detailItemErrorCount);
				updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_WARNING, -detailItemWarningCount);
			}
		}
		
		
		public void removeTreeNodeMissingRequiredChildren(Element treeNodeElement) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				if(validationNode.missingRequiredChildren.size() > 0) {
					updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_ERROR, -1);
					validationNode.missingRequiredChildren.clear();	
				}
			}
		}
		
		
		public void removeTreeNodeMessages(Element treeNodeElement) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				int treeNodeValidationErrorCount = 0;
				int treeNodeValidationWarningCount = 0;
				List values = validationNode.treeNodeMessages;
				Iterator iterator = values.iterator();
				while(iterator.hasNext()) {
					ValidationMessage message = (ValidationMessage)iterator.next();
					switch(message.getMessageType()) {
					case ValidationMessage.MESSAGE_TYPE_ERROR:
						treeNodeValidationErrorCount++;
						break;
					case ValidationMessage.MESSAGE_TYPE_WARNING:
						treeNodeValidationWarningCount++;
						break;
					}
				}
				validationNode.treeNodeMessages.clear();
				updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_ERROR, -treeNodeValidationErrorCount);
				updateCountAndCascade(validationNode, ValidationMessage.MESSAGE_TYPE_WARNING, -treeNodeValidationWarningCount);
			}
		}
		
		
		public Element[] getChildTreeNodeElements(Element treeNodeElement, boolean recurse, boolean excludeCascading, boolean withErrors, boolean withWarnings) {
			List elements = new ArrayList();
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				Stack childrenStack = new Stack();
				childrenStack.addAll(validationNode.children);
				while(!childrenStack.isEmpty()) {
					TreeNodeValidationNode childValidationNode = (TreeNodeValidationNode)childrenStack.pop();
					if(recurse) {
						childrenStack.addAll(childValidationNode.children);
					}
					if((withErrors && childValidationNode.errors > 0) || (withWarnings && childValidationNode.warnings > 0) ||
							(!excludeCascading && ((withErrors && childValidationNode.cascadingErrors > 0)
									|| (withWarnings && childValidationNode.cascadingWarnings > 0)))) {
						elements.add(childValidationNode.element);
					}
				}
			}
			return (Element[])elements.toArray(new Element[elements.size()]);
		}
		
		
		public void removeTreeNode(Element parentTreeNodeElement, Element treeNodeElement) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				Stack childrenStack = new Stack();
				childrenStack.addAll(validationNode.children);
				while(!childrenStack.isEmpty()) {
					TreeNodeValidationNode childValidationNode = (TreeNodeValidationNode)childrenStack.pop();
					childrenStack.addAll(childValidationNode.children);
					messages.remove(childValidationNode.element);
				}
				TreeNodeValidationNode parentValidationNode = (TreeNodeValidationNode)messages.get(parentTreeNodeElement);
				if(parentValidationNode != null) {
					parentValidationNode.children.remove(validationNode);
					messages.remove(validationNode.element);
					parentValidationNode.cascadingErrors -= validationNode.errors + validationNode.cascadingErrors;
					parentValidationNode.cascadingWarnings -= validationNode.warnings + validationNode.cascadingWarnings;
					cascade(parentValidationNode, ValidationMessage.MESSAGE_TYPE_ERROR, - validationNode.errors - validationNode.cascadingErrors);
					cascade(parentValidationNode, ValidationMessage.MESSAGE_TYPE_WARNING, - validationNode.warnings - validationNode.cascadingWarnings);
				}
			}
		}

		
		private void updateCountAndCascade(TreeNodeValidationNode validationNode, int messageType, int increment) {
			if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
				validationNode.errors += increment;
			} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
				validationNode.warnings += increment;
			}
			cascade(validationNode, messageType, increment);
		}
		
		
		private void cascade(TreeNodeValidationNode validationNode, int messageType, int increment) {
			TreeNodeValidationNode currentValidationNode = validationNode;
			 
			while(parentNodeIsElement(currentValidationNode.element)) {
				Node parentNode = currentValidationNode.element.getParentNode();

				Element parentElement = (Element)parentNode;
				TreeNodeValidationNode parentValidationNode = (TreeNodeValidationNode)messages.get(parentElement);
				if(parentValidationNode == null) {
					parentValidationNode = new TreeNodeValidationNode(parentElement);
					messages.put(parentElement, parentValidationNode);
				}
				validationNode.parent = parentValidationNode;

				if(currentValidationNode.missingRequiredChildren.size() == 0 && currentValidationNode.errors == 0 && currentValidationNode.warnings == 0 &&
						currentValidationNode.cascadingErrors == 0 && currentValidationNode.cascadingWarnings == 0) {
					parentValidationNode.children.remove(currentValidationNode);
					messages.remove(currentValidationNode.element);
				} else {
					if(parentValidationNode.children.indexOf(currentValidationNode) == -1) {
						parentValidationNode.children.add(currentValidationNode);
					}
				}
				
				currentValidationNode = parentValidationNode;

				// Update the parent validation node cascading error/warning count
				if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
					currentValidationNode.cascadingErrors += increment;
				} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
					currentValidationNode.cascadingWarnings += increment;
				}
			}
		}
		
		
		private boolean parentNodeIsElement(Element element) {
			Node parentNode = element.getParentNode();
			if(parentNode != null) {
				return parentNode.getNodeType() == Node.ELEMENT_NODE;
			}
			return false;
		}

		
		public ValidationMessage getDetailItemMessage(Node node, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(node);
			if(validationNode != null) {
				if(detailItem instanceof AtomicDetailItem) {
					Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
					DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey);
					if(detailItemValidationNode != null) {
						return detailItemValidationNode.message;
					}

				} else if(detailItem instanceof RepeatableAtomicDetailItemSet) {
					String messageText = null;
					int messageType = 0;
					Object repeatableDetailItemKey = getDetailItemKey(containingSimpleDetailItem, detailItem, false);
					DetailItemValidationNode repeatableDetailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(repeatableDetailItemKey);
					if(repeatableDetailItemValidationNode != null) {
						messageText = repeatableDetailItemValidationNode.message.getMessage();
						messageType = repeatableDetailItemValidationNode.message.getMessageType();
					}
					RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItem;
					AtomicDetailItem[] items = repeatableAtomicDetailItemSet.getItems();
					for (int i = 0; i < items.length; i++) {
						Object detailItemKey = getDetailItemKey(containingSimpleDetailItem, items[i], false);
						DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)validationNode.detailItemMessages.get(detailItemKey);
						if(detailItemValidationNode != null) {
							MessageFormat messageFormat = new MessageFormat(Messages.ITEM);
							String item = messageFormat.format(new Integer[]{new Integer(i + 1)});
							if(messageText == null) {
								if(detailItem.getDetailItemCustomization().getCustomization().getHideRepeatableItemNumbers())
									messageText = detailItemValidationNode.message.getMessage();
								else
									messageText = MessageFormat.format(Messages.VALIDATION_MESSAGE , new Object[]{item, detailItemValidationNode.message.getMessage()});
							} else {
								String message = null;
								if(detailItem.getDetailItemCustomization().getCustomization().getHideRepeatableItemNumbers())
									message = detailItemValidationNode.message.getMessage();
								else
									message = MessageFormat.format(Messages.VALIDATION_MESSAGE , new Object[]{item, detailItemValidationNode.message.getMessage()});
								messageText = messageText.concat(System.getProperty("line.separator")).concat(message);
							}
							if(messageType != ValidationMessage.MESSAGE_TYPE_ERROR) {
								messageType = detailItemValidationNode.message.getMessageType();
							}
						}
					}
					if(messageText != null) {
						ValidationMessage validationMessage = new ValidationMessage(messageText, messageType);
						return validationMessage;
					}
				}
			}
			return null;
		}

		
		public DetailItemValidation[] getTreeNodeDetailItemMessages(Element treeNodeElement, boolean includeErrors, boolean includeWarnings) {
			List detailItemValidations = new ArrayList();
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				Collection values = validationNode.detailItemMessages.values();
				Iterator iterator = values.iterator();
				while (iterator.hasNext()) {
					DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode)iterator.next();
					int messageType = detailItemValidationNode.message.getMessageType();
					DetailItem detailItem = detailItemValidationNode.detailItem;
					SimpleDetailItem containingSimpleDetailItem = detailItemValidationNode.containingSimpleDetailItem;
					ValidationMessage message = detailItemValidationNode.message;
					DetailItemValidation detailItemValidation = new DetailItemValidation(detailItem, containingSimpleDetailItem, message);
					if(includeErrors && messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
						detailItemValidations.add(detailItemValidation);
					}
					if(includeWarnings && messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
						detailItemValidations.add(detailItemValidation);
					}
				}
			}
			return (DetailItemValidation[])detailItemValidations.toArray(new DetailItemValidation[detailItemValidations.size()]);
		}
	
		public ValidationMessage[] getTreeNodeMessages(Element treeNodeElement, boolean includeErrors, boolean includeWarnings) {
			List result = new ArrayList();
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				List treeNodeMessages = validationNode.treeNodeMessages;
				Iterator iterator = treeNodeMessages.iterator();
				while(iterator.hasNext()) {
					ValidationMessage validationMessage = (ValidationMessage)iterator.next();
					if((includeErrors && validationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_ERROR) ||
							(includeWarnings && validationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_WARNING)) {
						result.add(validationMessage);
					}
				}
			}
			return (ValidationMessage[])result.toArray(new ValidationMessage[result.size()]);
		}
		
		public ValidationMessage[] getMessages(Element treeNodeElement, boolean includeErrors, boolean includeWarnings) {
			List result = new ArrayList();
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode) messages.get(treeNodeElement);
			if(validationNode != null) {
				List myMessages = validationNode.messageList;
				Iterator iterator = myMessages.iterator();
				while(iterator.hasNext()) {
					ValidationMessage validationMessage = (ValidationMessage)iterator.next();
					if((includeErrors && validationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_ERROR) ||
							(includeWarnings && validationMessage.getMessageType() == ValidationMessage.MESSAGE_TYPE_WARNING)) {
						result.add(validationMessage);
					}
				}
			}
			return (ValidationMessage[])result.toArray(new ValidationMessage[result.size()]);
		}

		
		
		/****************/
		
		public int getDocumentMessageCount(int messageType) {
			if(document != null) {
				return getTreeNodeMessageCount(document.getDocumentElement(), messageType, false);
			}
			return 0;
		}
		
		
		public int getTreeNodeMessageCount(Element treeNodeElement, int messageType, boolean excludeCascading) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode != null) {
				if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
					return validationNode.errors + (excludeCascading? 0 : validationNode.cascadingErrors);
				} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
					return validationNode.warnings + (excludeCascading? 0 : validationNode.cascadingWarnings);
				}
			}
			return 0;
		}
		
		public CMElementDeclaration[] getTreeNodeMissingRequiredChildren(Element treeNodeElement) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode != null) {
				return (CMElementDeclaration[])validationNode.missingRequiredChildren.toArray(new CMElementDeclaration[validationNode.missingRequiredChildren.size()]);
			}
			return new CMElementDeclaration[0];
		}
		

		public void clearMessages() {
			messages.clear();
		}
		

		
		public boolean treeNodehasErrors(Element treeNodeElement, boolean excludeCascading) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode != null) {
				return validationNode.errors > 0 || (!excludeCascading && validationNode.cascadingErrors > 0);
			}
			return false;
		}
		
		
		public boolean treeNodehasWarnings(Element treeNodeElement, boolean excludeCascading) {
			TreeNodeValidationNode validationNode = (TreeNodeValidationNode)messages.get(treeNodeElement);
			if(validationNode != null) {
				return validationNode.warnings > 0 || (!excludeCascading && validationNode.cascadingWarnings > 0);
			}
			return false;
		}

		public Element[] getTreeNodeElements(boolean excludeCascading, boolean withErrors, boolean withWarnings) {
			List elements = new ArrayList();
			Collection values = messages.values();
			Iterator iterator = values.iterator();
			while(iterator.hasNext()) {
				TreeNodeValidationNode validationNode = (TreeNodeValidationNode)iterator.next();
				if((withErrors && validationNode.errors > 0) || (withWarnings && validationNode.warnings > 0) ||
						(!excludeCascading && ((withErrors && validationNode.cascadingErrors > 0)
								|| (withWarnings && validationNode.cascadingWarnings > 0)))) {
					elements.add(validationNode.element);
				}
			}
			return (Element[])elements.toArray(new Element[elements.size()]);
		}
		
		private Object getDetailItemKey(SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem, boolean obtainPathOnly) {
			if(!obtainPathOnly) {
				if(detailItem instanceof AtomicDetailItem) {
					AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
					Node node = atomicDetailItem.getNode();
					if(node != null) {
						return node;
					}
				}
			}
			return ModelUtil.getDetailItemLocalPath(containingSimpleDetailItem, detailItem);
		}

		private class DetailItemValidationNode {
			
			Object detailItemElementOrLocalPath;
			SimpleDetailItem containingSimpleDetailItem;
			DetailItem detailItem;
			ValidationMessage message;
			
			public DetailItemValidationNode(Object detailItemElementOrLocalPath, SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem, ValidationMessage message) {
				this.detailItemElementOrLocalPath = detailItemElementOrLocalPath;
				this.containingSimpleDetailItem = containingSimpleDetailItem;
				this.detailItem = detailItem;
				this.message = message;
			}
			
		}
		
		private class TreeNodeValidationNode {

			int errors = 0;
			int warnings = 0;
			int cascadingErrors = 0;
			int cascadingWarnings = 0;
			
			Element element;
			TreeNodeValidationNode parent;
			List children;
			List treeNodeMessages;
			List missingRequiredChildren;
			HashMap detailItemMessages;
			List messageList;
			
			public TreeNodeValidationNode(Element element) {
				this.element = element;
				children = new ArrayList();
				treeNodeMessages = new ArrayList();
				missingRequiredChildren = new ArrayList();
				detailItemMessages = new HashMap();
			    messageList = new ArrayList();
			}
		}
		
		public void printMessages() {
			Collection keySet = messages.values();
			Iterator iterator = keySet.iterator();
			while (iterator.hasNext()) {
				TreeNodeValidationNode validationNode = (TreeNodeValidationNode)iterator.next();
				
				Element element = validationNode.element;
				System.out.println("");
				System.out.println(element.getNodeName() + " (errors=" + validationNode.errors + " cascading=" + validationNode.cascadingErrors + ")");
				
				List list = validationNode.treeNodeMessages;
				if(list != null) {
					Iterator iterator2 = list.iterator();
					while (iterator2.hasNext()) {
						ValidationMessage message = (ValidationMessage) iterator2.next();
						System.out.print("   " + message.getMessage() + " ");
						int messageType = message.getMessageType();
						if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
							System.out.println("(Error)");
						} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
							System.out.println("(Warning)");
						}
					}
				}
			
				
				HashMap hashMap = (HashMap) validationNode.detailItemMessages;
				if(hashMap != null) {
					Set keySet2 = hashMap.keySet();
					Iterator iterator2 = keySet2.iterator();
					while (iterator2.hasNext()) {
						Object itemPath = iterator2.next();
						DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode) hashMap.get(itemPath);
						
						ValidationMessage message = detailItemValidationNode.message;
						System.out.print("   " + itemPath + ": " + message.getMessage() + " ");
						int messageType = message.getMessageType();
						if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
							System.out.println("(Error)");
						} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
							System.out.println("(Warning)");
						}
					}
				}
			}
		}
		
		
		private class MessageExporter {
			
			private IFile file;
			private IValidator validator;
			private IReporter reporter;
			boolean exportDetailedErrorLocations;
			
			public MessageExporter(IFile file, IValidator validator, IReporter reporter, boolean exportDetailedErrorLocations) {
				this.file = file;
				this.validator = validator;
				this.reporter = reporter;
				this.exportDetailedErrorLocations = exportDetailedErrorLocations;
			}
			
			public void exportMessagesToReporter() {
				Collection keySet = messages.values();
				Iterator iterator = keySet.iterator();
				while (iterator.hasNext()) {
					TreeNodeValidationNode validationNode = (TreeNodeValidationNode)iterator.next();
					exportTreeNodeMessages(validationNode);
					exportTreeNodeMissingRequiredChildrenMessages(validationNode);
					exportTreeNodeDetailItemMessages(validationNode);
				}
			} 
			
			private void exportTreeNodeMessages(TreeNodeValidationNode treeNodeValidationNode) {
				Element element = treeNodeValidationNode.element;
				String treeNodeLabel = ModelUtil.getTreeNodeLabel(element, customization, resource);
				List treeNodeMessages = treeNodeValidationNode.treeNodeMessages;
				if(treeNodeMessages != null) {
					Iterator treeNodeMessagesIterator = treeNodeMessages.iterator();
					while (treeNodeMessagesIterator.hasNext()) {
						ValidationMessage message = (ValidationMessage) treeNodeMessagesIterator.next();
						int localizedMessageSeverity = LocalizedMessage.LOW_SEVERITY;
						int messageType = message.getMessageType();
						if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
							localizedMessageSeverity = LocalizedMessage.HIGH_SEVERITY;
						} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
							localizedMessageSeverity = LocalizedMessage.NORMAL_SEVERITY;
						}
						String messageText = MessageFormat.format(Messages.VALIDATION_MESSAGE, new Object[]{treeNodeLabel, message.getMessage()});
						LocalizedMessage localizedMessage = new LocalizedMessage(localizedMessageSeverity, messageText);
						localizedMessage.setTargetObject(file);
						setLocalizedMessageCoordinates(element, localizedMessage);
						reporter.addMessage(validator, localizedMessage);
					}
				}
			}
			
			private void exportTreeNodeMissingRequiredChildrenMessages(TreeNodeValidationNode treeNodeValidationNode) {
				Element element = treeNodeValidationNode.element;
				String treeNodeLabel = ModelUtil.getTreeNodeLabel(element, customization, resource);
				List missingRequiredChildren = treeNodeValidationNode.missingRequiredChildren;
				if(missingRequiredChildren != null) {
					if(missingRequiredChildren.size() > 0) {
						String message = null;
						Iterator missingRequiredChildrenIterator = missingRequiredChildren.iterator();
						while(missingRequiredChildrenIterator.hasNext()) {
							CMElementDeclaration cmElementDeclaration = (CMElementDeclaration)missingRequiredChildrenIterator.next();
							String label = null;
							if(customization != null) {
								DetailItemCustomization itemCustomization = customization.getItemCustomization(ModelUtil.getNamespaceURI(cmElementDeclaration), ModelUtil.getNodeFullPath(element, cmElementDeclaration));
								if(itemCustomization == null)
									itemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration,ModelUtil.getNodeFullPath(element, cmElementDeclaration));
								if(itemCustomization != null) {
									label = itemCustomization.getLabel();
								}
							}
							if(label == null) {
								label = cmElementDeclaration.getElementName();
							}
							if(message == null) {
								message = label;
							} else {
								message = MessageFormat.format(Messages.ITEM_LIST, new Object[]{message, label});
							}
						}
						if(missingRequiredChildren.size() == 1) {
							message = MessageFormat.format(Messages.DETAILED_VALIDATION_MESSAGE, new Object[]{treeNodeLabel, Messages.SINGLE_REQUIRED_ITEM_MISSING, message});
							
						} else {
							message = MessageFormat.format(Messages.DETAILED_VALIDATION_MESSAGE, new Object[]{treeNodeLabel, Messages.MULTIPLE_REQUIRED_ITEMS_MISSING, message});
						}
						LocalizedMessage localizedMessage = new LocalizedMessage(LocalizedMessage.HIGH_SEVERITY, message);
						localizedMessage.setTargetObject(file);
						setLocalizedMessageCoordinates(element, localizedMessage);
						reporter.addMessage(validator, localizedMessage);
					}
				}
			}
			
			private void exportTreeNodeDetailItemMessages(TreeNodeValidationNode treeNodeValidationNode) {
				HashMap treeNodeDetailItemMessages = (HashMap) treeNodeValidationNode.detailItemMessages;
				if(treeNodeDetailItemMessages != null) {
					Iterator treeNodeDetailItemMessagesIterator = treeNodeDetailItemMessages.keySet().iterator();
					while (treeNodeDetailItemMessagesIterator.hasNext()) {
						Object detailItemKey = treeNodeDetailItemMessagesIterator.next();
						DetailItemValidationNode detailItemValidationNode = (DetailItemValidationNode) treeNodeDetailItemMessages.get(detailItemKey);
						ValidationMessage message = detailItemValidationNode.message;
						
						DetailItem detailItem = detailItemValidationNode.detailItem;
						String label = null;
						DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
						if(detailItemCustomization != null) {
							label = detailItemCustomization.getLabel();
							// Customized detailed item labels may have mnemonics
							label = stripMnemonicFromLabel(label);
						}
						if(label == null) {
							label = detailItem.getName();
						}
						int localizedMessageSeverity = LocalizedMessage.LOW_SEVERITY;
						int messageType = message.getMessageType();
						if(messageType == ValidationMessage.MESSAGE_TYPE_ERROR) {
							localizedMessageSeverity = LocalizedMessage.HIGH_SEVERITY;
						} else if(messageType == ValidationMessage.MESSAGE_TYPE_WARNING) {
							localizedMessageSeverity = LocalizedMessage.NORMAL_SEVERITY;
						}
						String messageText = MessageFormat.format(Messages.VALIDATION_MESSAGE, new Object[]{label, message.getMessage()});
						LocalizedMessage localizedMessage = new LocalizedMessage(localizedMessageSeverity, messageText);
						localizedMessage.setTargetObject(file);
						Node localizationNode = ModelUtil.getDetailItemValueNode(detailItem);
						if(localizationNode == null) {
							localizationNode = treeNodeValidationNode.element;
						}
						setLocalizedMessageCoordinates(localizationNode, localizedMessage);
						reporter.addMessage(validator, localizedMessage);
					}
				}
			}

      private String stripMnemonicFromLabel(String label)
      {
        String labelBackup = label;
        try
        {
          int index = label.indexOf("&"); //$NON-NLS-1$
          if (index == -1) // if there is no '&' just return the original label
          {
            return label;
          }
          int length = label.length(); // We need this later to determine if the '&' is at the end of the label
          // Now check if there is another '&' in the label, starting from after the first '&'
          int indexOfSecondAmp = label.indexOf("&", index + 1); //$NON-NLS-1$
          if (indexOfSecondAmp == -1 && // This means there is only one '&'.  There should be one and only one '&' that represents the mnemonic
              length != index + 1) // If the '&' is at the end, then it must not be a mnemonic
          {
            char c = label.charAt(index + 1); 
            // Do additional checking to see if the character after the '&' is not a space
            if (c != ' ')
            {
              StringBuffer sb = new StringBuffer(label);
              // Now, remove the '&'
              sb.delete(index, index + 1);
              label = sb.toString();
            }
          }
        }
        catch (Exception e)
        {
          // Return the original label if we encounter problems
          return labelBackup;
        }
        return label;
      }
			
			private void setLocalizedMessageCoordinates(Node node, LocalizedMessage localizedMessage) {
				int offset = ((IDOMNode)node).getStartOffset();
				int line = ((IDOMDocument)document).getStructuredDocument().getLineOfOffset(offset);
				localizedMessage.setLineNo(line + 1);
				if(exportDetailedErrorLocations) {
					if(node.getNodeType() == Node.TEXT_NODE) {
						localizedMessage.setOffset(offset);
						localizedMessage.setLength(node.getNodeValue().length());
					} else if(node.getNodeType() == Node.ELEMENT_NODE) {
						localizedMessage.setOffset(offset + 1);
						localizedMessage.setLength(node.getNodeName().length());
					} else if(node.getNodeType() == Node.ATTRIBUTE_NODE) {
						Attr attr = (Attr)node;
						Element ownerElement = attr.getOwnerElement();
						int attributeValueOffset = ((IDOMNode)ownerElement).getStartOffset() + ((IDOMNode)node).getValueRegion().getStart();
						localizedMessage.setOffset(attributeValueOffset);
						localizedMessage.setLength(attr.getValue().length() + 2);
					}
				}
			}
			
		}
		
		
		public void exportMessagesToReporter(IFile file, IValidator validator, IReporter reporter, boolean exportDetailedErrorLocations) {
			MessageExporter messageExporter = new MessageExporter(file, validator, reporter, exportDetailedErrorLocations);
			messageExporter.exportMessagesToReporter();
		}
	}

}

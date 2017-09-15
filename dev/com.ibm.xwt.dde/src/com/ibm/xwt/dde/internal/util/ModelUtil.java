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
package com.ibm.xwt.dde.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMContent;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMVisitor;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMNamespaceHelper;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceTable;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl;
import org.eclipse.xsd.XSDAnnotation;
import org.eclipse.xsd.XSDAttributeDeclaration;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.XSDVariety;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomCanCreateObject;
import com.ibm.xwt.dde.customization.ICustomCanDeleteObject;
import com.ibm.xwt.dde.customization.ICustomDefaultValueObject;
import com.ibm.xwt.dde.customization.ICustomIconObject;
import com.ibm.xwt.dde.customization.ICustomLabelObject;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.actions.AddElementAction;
import com.ibm.xwt.dde.internal.actions.DeleteElementAction;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.data.AtomicAttributeDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.RepeatableAtomicDetailItemSet;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.viewers.DDEViewer;
import com.ibm.xwt.dde.internal.viewers.DetailsContentProvider;


public class ModelUtil {

	public static final int SEQUENCE = 1;
	public static final int ALL = 2;
	public static final int CHOICE = 4;
	public static final int OPTIONAL = 8;
	public static final int REPEATABLE = 16;

	/**
	 * An element is said to be atomic if it can be represented using a single control.
	 * Atomic elements are those that may contain only text value and no attributes
	 *  
	 * @param CMElementDeclaration (Content Model)
	 * @return true if the element is atomic, false otherwise
	 */

	// This implementation of isAtomic takes into consideration filtering
	public static boolean isAtomicCMElementDeclaration(Customization customization, Element parentElement, CMElementDeclaration cmElementDeclaration) {
		CMNamedNodeMap cmNamedNodeMap = cmElementDeclaration.getAttributes();
		for(int i = 0; i < cmNamedNodeMap.getLength(); i++) {
			CMAttributeDeclaration cmAttributeDelcaration = (CMAttributeDeclaration) cmNamedNodeMap.item(i);
			String attributeNamespace = getNamespaceURI(cmAttributeDelcaration);
			String path = getNodeFullPath(parentElement, cmElementDeclaration, cmAttributeDelcaration);
			DetailItemCustomization detailItemCustomization = null;
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(attributeNamespace, path);
				if(detailItemCustomization == null)
					detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
			}
			if(detailItemCustomization == null || (detailItemCustomization != null && !detailItemCustomization.isHidden())) {
				return false;
			}
		}
		CMContent cmContent = cmElementDeclaration.getContent();
		if(cmContent == null) {
			return true;
		} else if(cmContent.getNodeType() == CMContent.GROUP) {
			CMGroup cmGroup = (CMGroup)cmContent;
			if(cmGroup.getChildNodes().getLength() == 0)
				return true;
		}
		return false;
	}
	
	public static boolean isAtomicCMElementDeclaration(Customization customization, Element grandParentElement, CMElementDeclaration parentCMElementDeclaration, CMElementDeclaration cmElementDeclaration) {
		CMNamedNodeMap cmNamedNodeMap = cmElementDeclaration.getAttributes();
		for(int i = 0; i < cmNamedNodeMap.getLength(); i++) {
			CMAttributeDeclaration cmAttributeDelcaration = (CMAttributeDeclaration) cmNamedNodeMap.item(i);
			String attributeNamespace = getNamespaceURI(cmAttributeDelcaration);
			String path = getNodeFullPath(grandParentElement, parentCMElementDeclaration, cmAttributeDelcaration);
			DetailItemCustomization detailItemCustomization = null;
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(attributeNamespace, path);
				if(detailItemCustomization == null)
					detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
			}
			if(detailItemCustomization == null || (detailItemCustomization != null && !detailItemCustomization.isHidden())) {
				return false;
			}
		}
		CMContent cmContent = cmElementDeclaration.getContent();
		if(cmContent == null) {
			return true;
		} else if(cmContent.getNodeType() == CMContent.GROUP) {
			CMGroup cmGroup = (CMGroup)cmContent;
			if(cmGroup.getChildNodes().getLength() == 0)
				return true;
		}
		return false;
	}


	/**
	 * An element is said to be simple if its children are all atomic
	 * 
	 * The CMElementDeclaration of a simple element is either a single atomic
	 * element or one or more nested non-repeatable groups ('sequence' or 'all')
	 * each containing zero or more atomic elements.
	 *  
	 * @param CMElementDeclaration (Content Model)
	 * @return true if the element is simple, false otherwise
	 */
	public static boolean isSimpleCMElementDeclaration(Customization customization, Element parentElement, CMElementDeclaration cmElementDeclaration) {
		IsSimpleCMElementDeclarationVisitor visitor = new IsSimpleCMElementDeclarationVisitor(customization, parentElement, cmElementDeclaration);
		visitor.visitCMNode(cmElementDeclaration.getContent());
		return visitor.getResult();
	}

	// Inner class used for determining if a given element declaration is simple
	private static class IsSimpleCMElementDeclarationVisitor extends CMVisitor {                                             
		public boolean result = true;
		private List visitedGroups;
		private Customization customization;
		private Element grandParentElement;
		private CMElementDeclaration parentCMElementDeclaration;

		public IsSimpleCMElementDeclarationVisitor(Customization customization, Element parentElement, CMElementDeclaration cmElementDeclaration) {
			this.customization = customization;
			this.grandParentElement = parentElement;
			this.parentCMElementDeclaration = cmElementDeclaration;
			visitedGroups = new ArrayList();
		}

		public void visitCMGroup(CMGroup group)	{         
			if(result) {
				if(group.getOperator() == CMGroup.CHOICE || group.getMaxOccur() == -1 || group.getMaxOccur() > 1) {
					// If the group is a choice or is repeatable, then the element is not simple 
					result = false;
				} else if(visitedGroups.indexOf(group) == -1) {
					super.visitCMGroup(group);
					visitedGroups.add(group);
				}
			}
		}

		public void visitCMElementDeclaration(CMElementDeclaration cmElementDeclaration) {
			if (result) {
				result = isAtomicCMElementDeclaration(customization, grandParentElement, parentCMElementDeclaration, cmElementDeclaration);
				visitCMNode(cmElementDeclaration.getContent());
			}
		}

		public boolean getResult() {
			return result;
		}  
	}

	/**
	 * Determines if a given element is repeatable based on its declaration.
	 * 
	 * @param CMElementDeclaration (Content Model)
	 * @return true if the element can be repeated
	 */
	public static boolean isCMNodeRepeatable(CMNode node) {                                
		if (node instanceof CMContent)	{
			CMContent content = (CMContent)node;
			return content.getMaxOccur() > 1 || content.getMaxOccur() == -1;
		} 
		return false;
	}

	/**
	 * This method returns the types of groups between two given CMElementDeclarations (one being an ancestor of the other)
	 * An integer value is returned using the following bit-wise semantics:
	 * 
	 * 0   None			No groups were found between the two given CMElementDeclarations
	 * 1   Sequence		One or more "Sequence" groups were found between the two given CMElementDeclarations
	 * 2   All			One or more "All" groups were found between the two given CMElementDeclarations
	 * 4   Choice		One or more "Choice" groups were found between the two given CMElementDeclarations
	 *
	 * 8   Optional		At least one of the groups found between the two given CMElementDeclarations is optional
	 * 16  Repeatable	At least one of the groups found between the two given CMElementDeclarations is repeatable
	 * 
	 * @param ancestorCMElementDeclaration
	 * @param childCMElementDeclaration
	 * @return
	 */

	public static int getGroupTypesInBetween(CMElementDeclaration ancestorCMElementDeclaration, CMElementDeclaration childCMElementDeclaration) {
		CMContent cmContent = ancestorCMElementDeclaration.getContent();
		if(cmContent != null && cmContent.getNodeType() == CMContent.GROUP) {
			CMGroup cmGroup = (CMGroup)cmContent;
			return recursiveGetGroupTypesInBetween(cmGroup,childCMElementDeclaration);
		}
		return 0;
	}

	// Recursive method used to determine the groups in between two elements
	private static int recursiveGetGroupTypesInBetween(CMGroup cmGroup, CMNode cmNode) {
		CMNodeList cmGroupChildren = cmGroup.getChildNodes();
		if(cmGroupChildren != null) {
			for (int i = 0; i < cmGroupChildren.getLength(); i++) {
				CMNode cmGroupChild = cmGroupChildren.item(i);
				if(cmGroupChild.equals(cmNode)) {
					return getGroupDescription(cmGroup);
				} else if(cmGroupChild.getNodeType() == CMNode.GROUP) {
					CMGroup childCMGroup = (CMGroup)cmGroupChild;
					int result = recursiveGetGroupTypesInBetween(childCMGroup, cmNode);
					if(result > 0) {
						return result | getGroupDescription(cmGroup);
					}
				}
			}
		}
		return 0;
	}

	private static int getGroupDescription(CMGroup cmGroup) {
		int result = 0;
		switch(cmGroup.getOperator()) {
		case CMGroup.SEQUENCE:	result |= SEQUENCE; break;
		case CMGroup.ALL:		result |= ALL; break;
		case CMGroup.CHOICE:	result |= CHOICE; break;
		}
		if(cmGroup.getMinOccur() == 0) {
			result |= OPTIONAL;
		}
		if(cmGroup.getMaxOccur() == -1 || cmGroup.getMaxOccur() > 1) {
			result |= REPEATABLE;
		}
		return result;
	}

	/**
	 * This method returns the DOM instances of an element given its CMElementDeclaration
	 * and its DOM parent element
	 * 
	 * @param parentElement
	 * @param cmElementDeclaration
	 * @return All instances of the element that correspond to the CMElementDeclaration that are
	 * children of the given parent element.
	 */
	public static Element[] getInstancesOfElement(Element parentElement, CMElementDeclaration cmElementDeclaration){
		List result = new ArrayList();
		String nodeName = DOMNamespaceHelper.computeName(cmElementDeclaration, parentElement, null);
		String namespaceURI = null;
		CMDocument cmDocument = (CMDocument)cmElementDeclaration.getProperty("CMDocument"); //$NON-NLS-1$
		if (cmDocument != null) {     
			namespaceURI = (String)cmDocument.getProperty("http://org.eclipse.wst/cm/properties/targetNamespaceURI");    //$NON-NLS-1$
		}
		for(Node node = parentElement.getFirstChild(); node != null; node = node.getNextSibling()) {
			if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(nodeName)) {
				if(!(namespaceURI != null && ModelUtil.getNodeNamespace(node) != null && !namespaceURI.equals(ModelUtil.getNodeNamespace(node)))) {
					result.add((Element)node);
				}
			}
		}
		return (Element[]) result.toArray(new Element[result.size()]);
	}


	/**
	 * Given the CMElementDeclaration of an element and its parent element DOM Node, this method returns
	 * a reference to the element, creating 
	 * @param parentElement
	 * @param cmElementDeclaration
	 * @return
	 */
	public static Element obtainOrCreateElement(Element parentElement, CMElementDeclaration cmElementDeclaration, Customization customization) {
		Element[] instance = getInstancesOfElement(parentElement, cmElementDeclaration);
		if(instance.length > 0) {
			return instance[0];
		}
		
		NodeList childNodes = parentElement.getChildNodes();
		boolean parentNodeFormatRequired = childNodes == null || childNodes.getLength() == 0 || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE && ((childNodes.item(0)).getNodeValue().trim().length() == 0));
		
		Element element = null;
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
		boolean append = false;
		Node refChild = parentElement.getLastChild();
		
		if(modelQuery.canInsert(parentElement, cmElementDeclaration, parentElement.getChildNodes().getLength(), ModelQuery.VALIDITY_STRICT)) {
			append = true;
		} else {
			for(int i = parentElement.getChildNodes().getLength(); !modelQuery.canInsert(parentElement, cmElementDeclaration, i - 1, ModelQuery.VALIDITY_STRICT) && i > 0; i--) {
				refChild = refChild.getPreviousSibling();
			}
			if(refChild == null) {
				refChild = parentElement.getFirstChild();
			}
		}

		IDOMModel model = ((IDOMNode)parentElement).getModel();
		model.aboutToChangeModel();
		
		String namespaceURI = null;
		//Element element = null;
		CMDocument cmDocument = (CMDocument)cmElementDeclaration.getProperty("CMDocument"); //$NON-NLS-1$
		if (cmDocument != null) {     
			namespaceURI = (String)cmDocument.getProperty("http://org.eclipse.wst/cm/properties/targetNamespaceURI");    //$NON-NLS-1$
		}
		if(namespaceURI != null) {
			element = parentElement.getOwnerDocument().createElementNS(namespaceURI, DOMNamespaceHelper.computeName(cmElementDeclaration, parentElement, null));
		} else {
			element = parentElement.getOwnerDocument().createElement(cmElementDeclaration.getElementName());
		}
		Text textNode = parentElement.getOwnerDocument().createTextNode(System.getProperty("line.separator"));
		if(append) {
			parentElement.appendChild(element);
			parentElement.appendChild(textNode);
		} else {
			parentElement.insertBefore(element, refChild);
			parentElement.insertBefore(textNode, refChild);
		}
		if(parentNodeFormatRequired) {
			formatXMLNode(parentElement);
		} else {
			formatXMLNode(element);
		}
		model.changedModel();
		return element;
		
	}

	public static void formatXMLNode(Node node) {
		DocumentRewriteSession rewriteSession = null;
		if(node instanceof IDOMNode) {
			rewriteSession=((IDocumentExtension4)((IDOMNode)node).getModel().getStructuredDocument()).startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
		}
		IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
		try {
			formatProcessor.formatNode(node);
		} catch (Exception e) {}
		finally {
			if(node instanceof IDOMNode&&rewriteSession !=null) {
				((IDocumentExtension4)((IDOMNode)node).getModel().getStructuredDocument()).stopRewriteSession(rewriteSession);
			}
		}
	}
	
	public static Element recursivelyCompressEmptyElementTags(Element element) {
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element)node;
				node = recursivelyCompressEmptyElementTags(currentElement);
			}
		}
		return compressEmptyElementTag(element);
	}
	
	public static Element compressEmptyElementTag(Element element) {
		IDOMElement newElement = (IDOMElement)element;
		IStructuredDocumentRegion startTagStructuredDocumentRegion = newElement.getFirstStructuredDocumentRegion();
		IStructuredDocumentRegion endTagStructuredDocumentRegion = newElement.getLastStructuredDocumentRegion();
		if (startTagStructuredDocumentRegion != endTagStructuredDocumentRegion && startTagStructuredDocumentRegion != null) {
			ITextRegionList regions = startTagStructuredDocumentRegion.getRegions();
			ITextRegion lastRegion = regions.get(regions.size() - 1);
			if (lastRegion.getType() != DOMRegionContext.XML_EMPTY_TAG_CLOSE) {
				NodeList childNodes = newElement.getChildNodes();
				if (childNodes == null || childNodes.getLength() == 0 || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE && ((childNodes.item(0)).getNodeValue().trim().length() == 0))) {
					IDOMModel structuredModel = newElement.getModel();
					IStructuredDocument structuredDocument = structuredModel.getStructuredDocument();
					int startTagStartOffset = newElement.getStartOffset();
					int offset = endTagStructuredDocumentRegion.getStart();
					int length = endTagStructuredDocumentRegion.getLength();
					structuredDocument.replaceText(structuredDocument, offset, length, ""); //$NON-NLS-1$
					newElement = (IDOMElement) structuredModel.getIndexedRegion(startTagStartOffset); // save
					offset = startTagStructuredDocumentRegion.getStart() + lastRegion.getStart();
					structuredDocument.replaceText(structuredDocument, offset, 0, "/"); //$NON-NLS-1$
					newElement = (IDOMElement) structuredModel.getIndexedRegion(startTagStartOffset); // save
				}
			}
		}
		return newElement;
	}

	/**
	 * Given the CMElementDeclaration of an element and its parent, this method determines if instances of such element should
	 * appear in the tree or not depending on its granularity and context:
	 *   
	 *   Granularity:
	 *   All atomic items and non-repeatable simple items can be flatened in the details view, so they should never appear
	 *   in the tree. This is, all complex and repeatable simple items should appear in the tree.
	 *   
	 *   Context:
	 *   Independently of its granularity, if a given item is contained in one or more (potentially nested) choice or
	 *   repeatable groups, then it must appear in the tree.
	 *   
	 * @param parentCMElementDeclaration
	 * @param childCMElementDeclaration
	 * @return true if 
	 */

	public static boolean elementMustAppearInTree(Customization customization, Element parentElement, CMElementDeclaration parentCMElementDeclaration, CMElementDeclaration childCMElementDeclaration) {
		boolean singleOccurrence = false;
		
		// Check if the customization of the item determines weather it must appear in the tree or not
		if(customization != null) {
			String path = getNodeFullPath(parentElement, childCMElementDeclaration);
			String elementNamespace = getNamespaceURI(childCMElementDeclaration);
			DetailItemCustomization detailItemCustomization = customization.getItemCustomization(elementNamespace, path);
			if(detailItemCustomization == null)
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(childCMElementDeclaration, path);

			if(detailItemCustomization != null) {
				if(detailItemCustomization.isHidden()) {
					return false; // Hidden items should not be shown in the tree
				}
				int style = detailItemCustomization.getStyle();
				if(style == DetailItemCustomization.STYLE_TREE_NODE) {
					return true;
				} else if(style == DetailItemCustomization.STYLE_CHECKBOX || style == DetailItemCustomization.STYLE_COMBO || style == DetailItemCustomization.STYLE_TEXT) {
					return false;
				}
				singleOccurrence = detailItemCustomization.isSingleOccurrence();
			}
		}
		
		// If the element is part of a substitution group, then it must appear in the tree
		Object object = null;
		object = childCMElementDeclaration.getProperty("SubstitutionGroupValue");
		if(object != null && !"".equals(object)) {
			return true;
		}
		// If the element is in a (potentially nested) choice or a repeatable group, then it must appear in the tree;		
		if(((getGroupTypesInBetween(parentCMElementDeclaration, childCMElementDeclaration) & (CHOICE | REPEATABLE)) != 0) && !singleOccurrence) {
			return true;
		}
		if(!isAtomicCMElementDeclaration(customization, parentElement, childCMElementDeclaration)) {
			boolean isSimple = isSimpleCMElementDeclaration(customization, parentElement, childCMElementDeclaration);
			if(!isSimple || (isSimple && isCMNodeRepeatable(childCMElementDeclaration) && !singleOccurrence)) {
				// Complex and repeatable simple items must appear in the tree
				return true;
			}
		}
		return false;
	}


	public static String getNamespaceURI(CMNode cmNode) {
		CMDocument cmDocument = (CMDocument)cmNode.getProperty("CMDocument"); //$NON-NLS-1$
		if (cmDocument != null) {     
			return (String)cmDocument.getProperty("http://org.eclipse.wst/cm/properties/targetNamespaceURI");    //$NON-NLS-1$
		}
		return null;
	}


	public static String getElementFullPath(Element element) {
		String path = element.getLocalName();
		for(Node parentNode = element.getParentNode(); parentNode != null; parentNode = parentNode.getParentNode()) {
			if(parentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element parentElement = (Element)parentNode;
				path = parentElement.getLocalName() + '/' + path;
			}
		}
		return '/' + path;
	}

	public static String getNodeFullPath(Element parentElement, CMNode cmNode) {
		String name = "";
		if(cmNode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
			name = '@' + ((CMAttributeDeclaration)cmNode).getAttrName();
		} else if(cmNode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
			name = '/' + ((CMElementDeclaration)cmNode).getElementName();
		}
		return getElementFullPath(parentElement) + name;
	}

	public static String getNodeFullPath(Element grandParentElement, CMElementDeclaration parentCMElementDeclaration, CMNode cmNode) {
		String name = "";
		if(cmNode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
			name = '@' + ((CMAttributeDeclaration)cmNode).getAttrName();
		} else if(cmNode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
			name = '/' + ((CMElementDeclaration)cmNode).getElementName();
		}
		return getElementFullPath(grandParentElement) + '/' + parentCMElementDeclaration.getElementName() + name;
	}


	/**
	 * This method helps format the contents of a DOM element in order to
	 * fit it in a multi-line text field control
	 * 
	 * @param text
	 * @return
	 */
	public static String formatMultiLineTextForEditing(String text) {
		StringTokenizer stringTokenizer = new StringTokenizer(text, "\n", true);
		String result = "";
		while(stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if(token != null && token.length() > 0) {
				if(token.charAt(0) == '\n' && !"".equals(result)) {
					result += "\r\n";
				} else {
					result += token.trim();
				}
			}
		}
		return result.trim();
	}
	
	
	public static String formatToolTip(String toolTip) {
		StringTokenizer stringTokenizer = new StringTokenizer(toolTip, "\n", false);
		String result = "";
		while(stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken().trim();
			result += token + ' ';
		}
		return result;
	}
	
	/**
	 * This method obtains the namespace URI of the content model document
	 * associated with the given DOM document.
	 * 
	 * @param document
	 * @return the namespace URI of the content model document, "" if the
	 * content model document does not have a namespace, or null if the
	 * model is being infered.
	 */
	public static String getModelNamespaceURI(Document document) {
		String modelNamespaceURI = null;
		Element documentElement = document.getDocumentElement();
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(document);
		CMNode cmNode = modelQuery.getCMNode(documentElement);
		if(cmNode != null) {
			CMDocument cmDocument = (CMDocument)cmNode.getProperty("CMDocument"); //$NON-NLS-1$
			if (cmDocument != null) {     
				Object object = cmDocument.getProperty("http://org.eclipse.wst/cm/properties/namespaceInfo"); //$NON-NLS-1$
				if(object instanceof Vector) {
					Vector vector = (Vector)object;
					Object firstElement = vector.firstElement();
					if(firstElement instanceof NamespaceInfo) {
						NamespaceInfo namespaceInfo = (NamespaceInfo)firstElement;
						if(namespaceInfo.uri != null) {
							modelNamespaceURI = namespaceInfo.uri;
						} else {
							modelNamespaceURI = ""; //$NON-NLS-1$
						}
					}
				}
			}
		}
		return modelNamespaceURI;
	}

	
	public static boolean isModelPresent(Document document) {
		Element documentElement = document.getDocumentElement();
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(document);
		try {
			CMNode cmNode = modelQuery.getCMNode(documentElement);
			if(cmNode != null) {
				return true;
				}
		} catch (Exception e) {}
		return false;
	}
	
	
	/**
	 * Verifies if the given element is the root element of its containing document
	 * @param element
	 * @return
	 */
	public static boolean isRootElement(Element element) {
		return element == element.getOwnerDocument().getDocumentElement();
	}
	
	
	/**
	 * Processes a string label, popullating the distinguisher values
	 * @param element
	 * @param label
	 * @return
	 */
	public static String processLabelDistinguishers(Element element, String label) {
		String result = "";
		boolean processToken = false;
		StringTokenizer stringTokenizer = new StringTokenizer(label,"$", true);
		while(stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if("$".equals(token)) {
				processToken = !processToken;
			} else if(processToken) {
				Element currentElement = null;
				String attributeName = null;
				if(".".equals(token)) {
					currentElement = element;
				} else {
					if(token.charAt(0) == '/') {
						currentElement = element.getOwnerDocument().getDocumentElement();	
						token = token.substring(currentElement.getNodeName().length()+1);
					} else {
						currentElement = element;
					}
					StringTokenizer segmentStringTokenizer = new StringTokenizer(token,"/");
					while(segmentStringTokenizer.hasMoreTokens()) {
						String segmentToken = segmentStringTokenizer.nextToken();
						int index = segmentToken.indexOf('@');
						if(index != -1) {
							attributeName = segmentToken.substring(index + 1);
							segmentToken = segmentToken.substring(0, index);
						}
							
						NodeList nodeList = currentElement.getElementsByTagNameNS(ModelUtil.getNodeNamespace(currentElement), segmentToken);
						if(nodeList.getLength() == 0) {
							nodeList = currentElement.getElementsByTagName(segmentToken);							
						}
						if(nodeList.getLength() > 0) {
							currentElement = (Element)nodeList.item(0);
						}
					}
				}
				if(attributeName != null) {
					String attributeValue = currentElement.getAttribute(attributeName);
					if(attributeValue != null) {
						result += attributeValue.trim();
					}
				} else {
					for (Node node = currentElement.getFirstChild(); node != null; node = node.getNextSibling()) {
						if (node.getNodeType() == Node.TEXT_NODE) {
							String text = node.getNodeValue();
							if(!(text == null)) {
								result += text.trim();
							}
						}
					}
				}
			} else {
				result += token;
			}
		}
	return result;
	}
	
	public static CMGroup getContainingGroup(CMElementDeclaration ancestorCMElementDeclaration, CMElementDeclaration cmElementDeclaration) {
		CMContent cmContent = ancestorCMElementDeclaration.getContent();
		if(cmContent.getNodeType() == CMContent.GROUP) {
			Stack cmGroupStack = new Stack();
			cmGroupStack.push((CMGroup)cmContent);
			while(!cmGroupStack.isEmpty()) {
				CMGroup currentGroup = (CMGroup) cmGroupStack.pop();
				CMNodeList cmNodeList = currentGroup.getChildNodes();
				for (int i = 0; i < cmNodeList.getLength(); i++) {
					CMNode cmNode = cmNodeList.item(i);
					if(cmNode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
						CMElementDeclaration potentialCMElementDeclaration = (CMElementDeclaration)cmNode;
						if(potentialCMElementDeclaration.equals(cmElementDeclaration)) {
							return currentGroup;
						}
					} else if(cmNode.getNodeType() == CMNode.GROUP) {
						cmGroupStack.push(cmNode);
					}
				}
			}
		}
		return null;
	}
	
	public static boolean canInsertConsideringDuplication(ModelQuery modelQuery, Element element, CMElementDeclaration cmElementDeclaration, Customization customization, IEditorPart editorPart) {
		if(customization != null) {
			DetailItemCustomization detailItemCustomization = customization.getItemCustomization(getNamespaceURI(cmElementDeclaration), ModelUtil.getNodeFullPath(element, cmElementDeclaration));
			if(detailItemCustomization == null)
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, ModelUtil.getElementFullPath(element));
			if(detailItemCustomization != null) {
				boolean canCreate = detailItemCustomization.isCanCreate();
				if(!canCreate) {
					return false;
				}
				Class canCreateClass = detailItemCustomization.getCanCreateClass();
				if(canCreateClass != null) {
					try {
						Object object = canCreateClass.newInstance();
						if(object instanceof ICustomCanCreateObject) {
							ICustomCanCreateObject customCanCreateObject = (ICustomCanCreateObject)object;
							return customCanCreateObject.canCreate(element, editorPart);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return true;
		
	}
	
	public static boolean insertActionsAvailable(Element element, CMElementDeclaration cmElementDeclaration, ModelQuery modelQuery, Customization customization, IEditorPart editorPart) {
		List modelQueryInsertActions = new ArrayList();
		modelQuery.getInsertActions(element, cmElementDeclaration, -1, ModelQuery.INCLUDE_CHILD_NODES, ModelQuery.VALIDITY_STRICT, modelQueryInsertActions);
		Iterator iterator = modelQueryInsertActions.iterator();
		while(iterator.hasNext()) {
			ModelQueryAction modelQueryAction = (ModelQueryAction)iterator.next();
			CMNode cmNode = modelQueryAction.getCMNode();
			if(cmNode instanceof CMElementDeclaration) {
				CMElementDeclaration availableCMElementDeclaration = (CMElementDeclaration)cmNode;
				if(canInsertConsideringDuplication(modelQuery, element, availableCMElementDeclaration, customization, editorPart)) {
					if(elementMustAppearInTree(customization, element, cmElementDeclaration, availableCMElementDeclaration)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	
	public static List getInsertActions(Element element, Customization customization, DDEViewer ddeViewer, IEditorPart editorPart) {
		boolean detectSchemaLabel = false;
		boolean globalDetectSchemaLabel = false;
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
		String path = getElementFullPath(element);
		List actions = new ArrayList();

		List modelQueryInsertActions = new ArrayList();
		List availableContent = new ArrayList();
		modelQuery.getInsertActions(element, cmElementDeclaration, -1, ModelQuery.INCLUDE_CHILD_NODES, ModelQuery.VALIDITY_STRICT, modelQueryInsertActions);
		Iterator iterator = modelQueryInsertActions.iterator();
		while(iterator.hasNext()) {
			ModelQueryAction modelQueryAction = (ModelQueryAction)iterator.next();
			CMNode cmNode = modelQueryAction.getCMNode();
			availableContent.add(cmNode);
		}

		//List availableContent = modelQuery.getAvailableContent(element, cmElementDeclaration, ModelQuery.VALIDITY_STRICT);
		for (iterator = availableContent.iterator(); iterator.hasNext();) {
			Object object = iterator.next();
			if(object instanceof CMElementDeclaration) {
				CMElementDeclaration availableCMElementDeclaration = (CMElementDeclaration)object;
				if(canInsertConsideringDuplication(modelQuery, element, availableCMElementDeclaration, customization, editorPart)) {
					// Include only items that appear in the tree
					if(elementMustAppearInTree(customization, element, cmElementDeclaration, availableCMElementDeclaration)) {
						AddElementAction action = new AddElementAction(element, availableCMElementDeclaration, ddeViewer, editorPart, customization);
						DetailItemCustomization detailItemCustomization = null;
						
						if(customization != null) {
							globalDetectSchemaLabel = customization.getGlobalDetectSchemaLabel();
							detailItemCustomization = customization.getItemCustomization(getNamespaceURI(availableCMElementDeclaration), path + '/' + availableCMElementDeclaration.getElementName());
							if(detailItemCustomization == null)
								detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, ModelUtil.getElementFullPath(element));
							String label = null;
							if(detailItemCustomization != null) {
								label = detailItemCustomization.getCreationLabel();
								//Check to see if detectSchemaLabel is set for this item
								detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();
								if(label == null) {
									label = detailItemCustomization.getLabel();
								}
								if(label != null) {
									action.setText(label);
								}
								Image image = detailItemCustomization.getIcon();
								if(image != null) {
									ImageDescriptor imageDescriptor = ImageDescriptor.createFromImage(image);
									action.setImageDescriptor(imageDescriptor);
									action.setImage(image);
								}
								//if there is no icon attribute try iconClass
								else
								{
									if (detailItemCustomization.getIconClass()!=null)
									{
										Object imgClass;
										try {
											imgClass = detailItemCustomization.getIconClass().newInstance();
											if(imgClass instanceof ICustomIconObject) {
												ICustomIconObject customImgObject = (ICustomIconObject)imgClass;
												image = customImgObject.getIcon(availableCMElementDeclaration, null);
												if(image!=null)
												{
													ImageDescriptor imageDescriptor = ImageDescriptor.createFromImage(image);
													action.setImageDescriptor(imageDescriptor);
													action.setImage(image);
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
							else
							{
								//no detailitem customization
								//try global iconClass
								if (action.getImage() == null && customization.getIconClass()!=null)
								{
									Object imgClass;
									try {
										imgClass = customization.getIconClass().newInstance();
										if(imgClass instanceof ICustomIconObject) {
											ICustomIconObject customImgObject = (ICustomIconObject)imgClass;
											Image image = customImgObject.getIcon(availableCMElementDeclaration, null);
											if(image!=null)
											{
												ImageDescriptor imageDescriptor = ImageDescriptor.createFromImage(image);
												action.setImageDescriptor(imageDescriptor);
												action.setImage(image);
											}
											
										}
									} catch (IllegalAccessException e) {
										e.printStackTrace();
									} catch (InstantiationException e) {
										e.printStackTrace();
									}
								}
								
							}
							//The case where there is no label defined for the item in the customization file or there is no 
							//customization for the item & the detectSchemaLabel flag to set the label from the schema is set to true
							if (label == null && (detectSchemaLabel|| globalDetectSchemaLabel))
							{
								if(cmElementDeclaration != null) 
								{
									label = getLabel((CMNode)availableCMElementDeclaration);
									if(label != null)
										action.setText(label);
							   	}
							}
						}
						actions.add(action);
					}
				}
			}
		}
		return actions;
	}

	
	public static Action getDeletionAction(Element element, DDEViewer ddeViewer, Customization customization, IEditorPart editorPart) {
		DeleteElementAction deleteElementAction = new DeleteElementAction(element, ddeViewer, editorPart);
		DetailItemCustomization detailItemCustomization = null;
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
		if(customization != null) {
			detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), getElementFullPath(element));
			if(detailItemCustomization == null)
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, ModelUtil.getElementFullPath(element));
			if(detailItemCustomization != null) {
				Class deletionClass = detailItemCustomization.getDeletionClass();
				if(deletionClass != null) {
					deleteElementAction.setDeletionClass(deletionClass);
				}
				boolean canDelete = detailItemCustomization.isCanDelete();
				if(!canDelete) {
					deleteElementAction.setEnabled(false);
				}
				Class canDeleteClass = detailItemCustomization.getCanDeleteClass();
				if(canDeleteClass != null) {
					try {
						Object object = canDeleteClass.newInstance();
						if(object instanceof ICustomCanDeleteObject) {
							ICustomCanDeleteObject customCanDeleteObject = (ICustomCanDeleteObject)object;
							if(!customCanDeleteObject.canDelete(element, editorPart)) {
								deleteElementAction.setEnabled(false);
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
		Node parentNode = element.getParentNode();
		if(parentNode.getNodeType() == Node.DOCUMENT_NODE) {
			deleteElementAction.setEnabled(false);
		}
//		if(!modelQuery.canRemove(element, ModelQuery.VALIDITY_STRICT) || parentNode.getNodeType() == Node.DOCUMENT_NODE){
//			deleteElementAction.setEnabled(false);
//		}
		deleteElementAction.setImageDescriptor(DDEPlugin.getDefault().getImageDescriptor("icons/remove.gif"));
		return deleteElementAction;
	}
	
	public static String formatHeaderOrFooterText(String text) {
		return text.replace("\\n", "\n").replace("\\t", "\t"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	public static boolean canDeleteElement(Element element, Customization customization, IEditorPart editorPart) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
		if(customization != null) {
			DetailItemCustomization detailItemCustomization = customization.getItemCustomization(ModelUtil.getNodeNamespace(element), ModelUtil.getElementFullPath(element));
			if(detailItemCustomization == null)
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, ModelUtil.getElementFullPath(element));
			if(detailItemCustomization != null) {
				if(!detailItemCustomization.isCanDelete()) {
					return false;
				}
				Class canDeleteClass = detailItemCustomization.getCanDeleteClass();
				if(canDeleteClass != null) {
					try {
						Object object = canDeleteClass.newInstance();
						if(object instanceof ICustomCanDeleteObject) {
							ICustomCanDeleteObject customCanDeleteObject = (ICustomCanDeleteObject)object;
							if(!customCanDeleteObject.canDelete(element, editorPart)) {
								return false;
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
		return !isRootElement(element);
	}
	
	
	
	
	
	
	public static NodeList runXPathAgainstDocument(Document document, String path, String namespaceData) {
		if(document != null) {
			try {
				XPath xpath = XPathFactory.newInstance().newXPath();
				if(namespaceData != null && !"".equals(namespaceData)) {
					final HashMap namespaces = new HashMap();
					StringTokenizer stringTokenizer = new StringTokenizer(namespaceData,",");
					while(stringTokenizer.hasMoreTokens()) {
						String token = stringTokenizer.nextToken();
						int index = token.indexOf('=');
						if(index != -1) {
							String key = token.substring(0, index).trim();
							String value = token.substring(index+1).trim();
							namespaces.put(key, value);
						}
					}
					NamespaceContext namespaceContext = new NamespaceContext(){
	
						public String getNamespaceURI(String prefix) {
							return (String)namespaces.get(prefix);
						}
	
						public String getPrefix(String namespace) {
							return null;
						}
	
						public Iterator getPrefixes(String namespace) {
							return null;
						}
					};
					xpath.setNamespaceContext(namespaceContext);					
				}
				XPathExpression expr = xpath.compile(path);
				return (NodeList)expr.evaluate(document, XPathConstants.NODESET);
			} catch (Exception e) {/*e.printStackTrace();*/}
		}
		return null;
	}
	
	public static void assignDefaultValues(Element element, Customization customization, IEditorPart editorPart) {
		if(customization != null) {
			Stack elements = new Stack();
			DetailsContentProvider detailsContentProvider = new DetailsContentProvider(customization);
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			elements.push(element);
			while(!elements.isEmpty()) {
				Element currentElement = (Element)elements.pop();
				CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(currentElement);
				NodeList childNodes = currentElement.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node item = childNodes.item(i);
					if(item.getNodeType() == Node.ELEMENT_NODE) {
						Element childElement = (Element)item;
						CMElementDeclaration childCMElementDeclaration = modelQuery.getCMElementDeclaration(childElement);
						if(elementMustAppearInTree(customization, currentElement, cmElementDeclaration, childCMElementDeclaration)) {
							elements.push(childElement);
						}
					}
				}				
				Element parentElement = (Element)currentElement.getParentNode();
				CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
				if(elementMustAppearInTree(customization, parentElement, parentCMElementDeclaration, cmElementDeclaration)) {
					Stack detailItems = new Stack();
					DetailItem[] items = detailsContentProvider.getItems(currentElement);
					detailItems.addAll(Arrays.asList(items));
					while(!detailItems.isEmpty()) {
						DetailItem detailItem = (DetailItem) detailItems.pop();
						if(detailItem instanceof SimpleDetailItem) {
							SimpleDetailItem simpleDetailItem = (SimpleDetailItem)detailItem;
							DetailItem[] atomicDetailItems = simpleDetailItem.getAtomicDetailItems();
							detailItems.addAll(Arrays.asList(atomicDetailItems));
						} else {
							DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
							if(detailItemCustomization != null) {
								String defaultValue = detailItemCustomization.getDefaultValue();
								Class defaultValueClass = detailItemCustomization.getDefaultValueClass();
								boolean isRequired = detailItemCustomization.isRequired();
								if(defaultValueClass != null) {
									try {
										Object object = defaultValueClass.newInstance();
										if(object instanceof ICustomDefaultValueObject) {
											ICustomDefaultValueObject customDefaultValueObject = (ICustomDefaultValueObject)object;
											Element ancestor = element;
											defaultValue = customDefaultValueObject.getDefaultValue(ancestor, editorPart);
										}
									} catch (Exception exception) {
										exception.printStackTrace();
									} 
								}
								if(defaultValue != null) {
									if(detailItem instanceof AtomicDetailItem) {
										AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
										atomicDetailItem.setValue(defaultValue);
									} else if(detailItem instanceof RepeatableAtomicDetailItemSet) {
										RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItem;
//										if(repeatableAtomicDetailItemSet.getItemCount() == 1) {
//											repeatableAtomicDetailItemSet.removeItem(repeatableAtomicDetailItemSet.getItems()[0]);
//										}
										StringTokenizer stringTokenizer = new StringTokenizer(defaultValue, ",");
										while(stringTokenizer.hasMoreTokens()) {
											String token = stringTokenizer.nextToken();
											repeatableAtomicDetailItemSet.addItem(token);
										}
									}
								} else if(isRequired && !detailItem.isRequired()){
									if(detailItem instanceof AtomicDetailItem) {
										AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
										atomicDetailItem.setValue("");
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static String getDefaultValueForDetailItem(DetailItem detailItem, Customization customization, IEditorPart editorPart) {
		String defaultValue = null;
		DetailItemCustomization detailItemCustomization = detailItem.getDetailItemCustomization();
		if(detailItemCustomization != null) {
			Class defaultValueClass = detailItemCustomization.getDefaultValueClass();
			if(defaultValueClass != null) {
				try {
					Object object = defaultValueClass.newInstance();
					if(object instanceof ICustomDefaultValueObject) {
						ICustomDefaultValueObject customDefaultValueObject = (ICustomDefaultValueObject)object;
						Element ancestor = null;
						if(detailItem instanceof AtomicDetailItem) {
							AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
							ancestor = atomicDetailItem.getClosestAncestor();
						} else if(detailItem instanceof RepeatableAtomicDetailItemSet) {
							RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItem;
							ancestor = repeatableAtomicDetailItemSet.getClosestAncestor();
						}
						defaultValue = customDefaultValueObject.getDefaultValue(ancestor, editorPart);
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				} 
			} else {
				defaultValue = detailItemCustomization.getDefaultValue();
			}
			
		}
		
		return defaultValue;
	}

	public static List getTreeChildElements(Element element, Customization customization) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		List elements = new ArrayList();
		CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(element);
		if(parentCMElementDeclaration != null) {
			for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element)node;
					CMElementDeclaration childCMElementDeclaration = modelQuery.getCMElementDeclaration(childElement);
					if(childCMElementDeclaration != null) {
						if(ModelUtil.elementMustAppearInTree(customization, element, parentCMElementDeclaration, childCMElementDeclaration)) {
							elements.add(childElement);
						}
					}
				}
			}
		}
		return elements;
	}
	
	public static boolean isDesignViewPageActiveAndInFocus() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench != null) {
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if(activeWorkbenchWindow != null) {
				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
				if(activePage != null) {
					IWorkbenchPart activePart = activePage.getActivePart();
					if(activePart != null) {
						if(activePart instanceof DDEMultiPageEditorPart) {
							DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)activePart;
							if(ddeMultiPageEditorPart.getEditorActivePage() == DDEMultiPageEditorPart.DESIGN_VIEW_PAGE){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	

	public static boolean isSourceViewPageActiveAndInFocus() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench != null) {
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if(activeWorkbenchWindow != null) {
				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
				if(activePage != null) {
					IWorkbenchPart activePart = activePage.getActivePart();
					if(activePart instanceof DDEMultiPageEditorPart) {
						DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)activePart;
						if(ddeMultiPageEditorPart.getEditorActivePage() == DDEMultiPageEditorPart.SOURCE_VIEW_PAGE){
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	
	public static String getDetailItemLocalPath(SimpleDetailItem containingSimpleDetailItem, DetailItem detailItem) {
		String detailItemLocalPath = "";
		if(containingSimpleDetailItem != null) {
			detailItemLocalPath += containingSimpleDetailItem.getName() + '/';
		}
		if(detailItem instanceof AtomicAttributeDetailItem) {
			detailItemLocalPath += '@';
		}
		detailItemLocalPath += detailItem.getName();
		return detailItemLocalPath;
	}

	
	public static Node[] getInstances(Document document, String path) {
		List nodeList = new ArrayList();
		nodeList.add(document);
		StringTokenizer stringTokenizer = new StringTokenizer(path, "/");
		while(stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			List newNodeList = new ArrayList();
			Iterator iterator = nodeList.iterator();
			String targetElementName = null;
			String targetAttributeName = null;
			int attributeSeparator = token.indexOf('@');
			if(attributeSeparator != -1) {
				targetElementName = token.substring(0, attributeSeparator);
				targetAttributeName = token.substring(attributeSeparator + 1);
			} else {
				targetElementName = token;
			}
			while(iterator.hasNext()) {
				Node currentNode = (Node)iterator.next();
				for(Node currentNodeChild = currentNode.getFirstChild(); currentNodeChild != null; currentNodeChild = currentNodeChild.getNextSibling()) {
					if(currentNodeChild.getNodeType() == Node.ELEMENT_NODE) {
						Element currentNodeChildElement = (Element)currentNodeChild;
						if(targetElementName.equals(currentNodeChildElement.getLocalName())) {
							if(targetAttributeName != null) {
								NamedNodeMap attributes = currentNodeChildElement.getAttributes();
								Node attributeNode = attributes.getNamedItem(targetAttributeName);
								if(attributeNode != null) {
									newNodeList.add(attributeNode);
								}
							} else {
								newNodeList.add(currentNodeChildElement);
							}
						}
					}
				} 
			}
			nodeList = newNodeList;
		}
		return (Node[])nodeList.toArray(new Node[nodeList.size()]);
	}
	
	public static Node getDetailItemValueNode(DetailItem detailItem) {
		if(detailItem instanceof AtomicDetailItem) {
			AtomicDetailItem atomicDetailItem = (AtomicDetailItem)detailItem;
			Node node = atomicDetailItem.getNode();
			if(node != null) {
				if(node.getNodeType() == Node.ELEMENT_NODE && atomicDetailItem.hasEditableValue()) {
					boolean isCDATAStorage = false;
					DetailItemCustomization detailItemCustomization = atomicDetailItem.getDetailItemCustomization();
					if(detailItemCustomization != null) {
						isCDATAStorage = detailItemCustomization.isCDATASectionStorage();
					}
					for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
						if (!isCDATAStorage && childNode.getNodeType() == Node.TEXT_NODE) {
							String text = childNode.getNodeValue();
							if(!((text == null) || (text.trim().length() == 0))) {
								return childNode;
							}
						} else if (isCDATAStorage && childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
							String text = childNode.getNodeValue();
							if(!((text == null) || (text.trim().length() == 0))) {
								return childNode;
							}
						}
					}
				}
				return node;
			}
		} else if(detailItem instanceof RepeatableAtomicDetailItemSet) {
			RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)detailItem;
			AtomicDetailItem[] items = repeatableAtomicDetailItemSet.getItems();
			if(items.length > 0) {
				return items[0].getNode().getParentNode();
			}
		} else if(detailItem instanceof SimpleDetailItem) {
			
		}
		return null;
	}
	
	
	public static void removeBlankCreatedRepeatableItems(Element element, Customization customization) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		Stack elementStack = new Stack();
		elementStack.push(element);
		while(!elementStack.isEmpty()) {
			Element currentElement = (Element)elementStack.pop();
			NodeList childNodes = currentElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node currentChildNode = childNodes.item(i);
				if(currentChildNode.getNodeType() == Node.ELEMENT_NODE) {
					Element currentChildElement = (Element)currentChildNode;
					elementStack.push(currentChildElement);
				}
			}
			Element currentParentElement = null;
			CMElementDeclaration currentParentCMElementDeclaration = null;
			CMElementDeclaration currentElementCMElementDeclaration = modelQuery.getCMElementDeclaration(currentElement);
			Node currentParentNode = currentElement.getParentNode();
			if(currentParentNode.getNodeType() == Node.ELEMENT_NODE) {
				currentParentElement = (Element)currentParentNode;
				currentParentCMElementDeclaration = modelQuery.getCMElementDeclaration(currentParentElement);
			}
			if(currentParentElement != null && currentParentCMElementDeclaration != null && currentElementCMElementDeclaration != null) {
				if(!ModelUtil.elementMustAppearInTree(customization, currentParentElement, currentParentCMElementDeclaration, currentElementCMElementDeclaration)) {
					if(ModelUtil.isAtomicCMElementDeclaration(customization, currentParentElement, currentElementCMElementDeclaration)) {
						boolean singleOccurrence = false;
						int style = DetailItemCustomization.STYLE_DEFAULT;
						String currentElementNamespace = ModelUtil.getNodeNamespace(currentElement);
						String currentElementPath = ModelUtil.getElementFullPath(currentElement);
						DetailItemCustomization itemCustomization = customization != null ? customization.getItemCustomization(currentElementNamespace, currentElementPath) : null;
						
						if(itemCustomization == null && customization!=null)
							itemCustomization = customization.getTypeCustomizationConsideringUnions(currentElementCMElementDeclaration, currentElementPath);

						if(itemCustomization != null) {
							singleOccurrence = itemCustomization.isSingleOccurrence();
							style = itemCustomization.getStyle();
						}
						if(ModelUtil.isCMNodeRepeatable(currentElementCMElementDeclaration) && !singleOccurrence && (style == DetailItemCustomization.STYLE_DEFAULT)) {
							currentParentElement.removeChild(currentElement);
						}
					}
				}
			}
		}
	}
	
	public static String getTreeNodeLabel(Element domElement, Customization customization, IResource resource) {

		boolean detectSchemaLabel = false;
		boolean globalDetectSchemaLabel = false;
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(domElement.getOwnerDocument());
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(domElement);

		
		// Verify if the label is customized
		if(customization != null) {
			globalDetectSchemaLabel = customization.getGlobalDetectSchemaLabel();
			String label = null;
			String path = ModelUtil.getElementFullPath(domElement);
			String elementNamespace = ModelUtil.getNodeNamespace(domElement);
			DetailItemCustomization detailItemCustomization = customization.getItemCustomization(elementNamespace, path);

			if(detailItemCustomization == null)
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);

			if(detailItemCustomization != null) {

				// Check if there is a custom label class
				Class treeLabelClass = detailItemCustomization.getTreeLabelClass();
				if(treeLabelClass != null) {
					try {
						Object object = treeLabelClass.newInstance();
						if(object instanceof ICustomLabelObject) {
							ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
							return customLabelObject.getLabel(domElement, resource);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					}
				}

				// Check if there is a tree label for the item
				label = detailItemCustomization.getTreeLabel();
				if(label != null) {
					// Apply distinguishers
					if(label.indexOf('$') != -1) {
						label = ModelUtil.processLabelDistinguishers(domElement, label);
					}
					return label;
				} 
				//Check to see if detectSchemaLabel is set for this item
				detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();
				// Check if there is a label for the item
				label = detailItemCustomization.getLabel();
				if(label != null) {
					return label;
				}
			}
			else {
				// no detailitem customization 
				// Try global customization
				// Check if there is a custom label class
				if (customization!=null) {
					Class treeLabelClass = customization.getTreeLabelClass();
					if(treeLabelClass != null) {
						try {
							Object object = treeLabelClass.newInstance();
							if(object instanceof ICustomLabelObject) {
								ICustomLabelObject customLabelObject = (ICustomLabelObject)object;
								return customLabelObject.getLabel(domElement, resource);
							}
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InstantiationException e) {
							e.printStackTrace();
						}
					}
				}			
			}
			//The case where there is no label defined for the item in the customization file
			//and the detectSchemaLabel flag to set the label from the schema is set to true
			if (label == null && (detectSchemaLabel|| globalDetectSchemaLabel))
			{
				if(cmElementDeclaration != null) 
				{
					String schemaLabel = getLabel((CMNode)cmElementDeclaration);
					if(schemaLabel != null)
						return schemaLabel;
		   		}
				
			}
		}
		return domElement.getLocalName();
	}
	
	public static int getRepeatableElementIndex(Element element) {
		int index = 0;
		String elementName = element.getNodeName();
		for(Node node = element.getParentNode().getFirstChild(); node != null; node = node.getNextSibling()) {
			if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(elementName)) {
				if(node.equals(element)) {
					return index;
				}
				index++;
			}
		}
		return 0;
	}

	public static String getPreferenceKeyForEditor(String siteId, String preferenceKey) {
		return siteId + '.' + preferenceKey;
	}
	
	public static String getNodeNamespace(Node node) {
		while(node != null) {
			String namespaceURI = node.getNamespaceURI();
			if(namespaceURI != null) {
				return namespaceURI;
			} else if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				NamedNodeMap attributes = element.getAttributes();
				if(attributes != null) {
					Node attrNode = attributes.getNamedItemNS("http://www.w3.org/2001/XMLSchema-instance", "type");
					if(attrNode != null && attrNode.getNodeType() == Node.ATTRIBUTE_NODE) {
						Attr attr = (Attr)attrNode;
						String value = attr.getValue();
						if(value != null) {
							int index = value.indexOf(':');
							if(index != -1) {
								String prefix = value.substring(0, index);
								NamespaceTable namespaceTable = new NamespaceTable(element.getOwnerDocument());
								if(namespaceTable != null) {
									namespaceTable.addElementLineage(element);
									String uriForPrefix = namespaceTable.getURIForPrefix(prefix);
									if(uriForPrefix != null) {
										return uriForPrefix;
									}
								}
							}
						}
					}
				}
			}
			node = node.getParentNode();
		}
		return null;
	}
	
	public static Element[] getElementTreeChildren(Customization customization, Element element) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		List elements = new ArrayList();
		CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
		if(cmElementDeclaration != null) {
			for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element)node;
					CMElementDeclaration childCMElementDeclaration = modelQuery.getCMElementDeclaration(childElement);
					if(elementMustAppearInTree(customization, childElement, cmElementDeclaration, childCMElementDeclaration)) {
						elements.add(childElement);
					}
				}
			}
		}
		return (Element[]) elements.toArray(new Element[elements.size()]);
	}
	
	public static boolean removeCDATASection(Element element) {
		boolean cdataRemoved = false;
		for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if(node.getNodeType() == Node.CDATA_SECTION_NODE) {
				element.removeChild(node);
				cdataRemoved = true;
			}
		}
		return cdataRemoved;
	}
	
	public static boolean isNodeInDocument(Document document, Node node) {
		if(document != null && node != null && node.getOwnerDocument() == document) {
			Node parentNode = node.getParentNode();
			while(parentNode != null) {
				if(parentNode == document) {
					return true;
				} else {
					parentNode = parentNode.getParentNode();
				}
			}
		}
		return false;
	}
	
	public static void removePrecedingText(Element element) {
		if(element != null) {
			Node parentNode = element.getParentNode();
			if(parentNode != null) {
				Node previousSibling = element.getPreviousSibling();
				if(previousSibling != null && previousSibling.getNodeType() == Node.TEXT_NODE) {
					Text text = (Text)previousSibling;
					String nodeValue = text.getNodeValue();
					if(nodeValue != null && nodeValue.trim().length() == 0) {
						int index = nodeValue.lastIndexOf("\r\n");
						if(index == -1) {
							index = nodeValue.lastIndexOf("\n");
						}
						if(index != -1) {
							nodeValue = nodeValue.substring(0, index);
							if("".equals(nodeValue)) {
								parentNode.removeChild(text);
							} else {
								text.setNodeValue(nodeValue);
							}
						}
					}
				}
			}
		}
	}
	
	
	public static void removeNextText(Element element) {
		if(element != null) {
			Node parentNode = element.getParentNode();
			if(parentNode != null) {
				Node nextSibling = element.getNextSibling();
				if(nextSibling != null && nextSibling.getNodeType() == Node.TEXT_NODE) {
					Text text = (Text)nextSibling;
					String nodeValue = text.getNodeValue();
					if(nodeValue != null && nodeValue.trim().length() == 0) {
						int index = nodeValue.lastIndexOf("\r\n");
						if(index == -1) {
							index = nodeValue.lastIndexOf("\n");
						}
						if(index != -1) {
							nodeValue = nodeValue.substring(0, index);
							if("".equals(nodeValue)) {
								parentNode.removeChild(text);
							} else {
								text.setNodeValue(nodeValue);
							}
						}
					}
				}
			}
		}
	}
	
	public static void removeText(Node node) {
		Node nextSib = node.getNextSibling();
		Node prevSib = node.getPreviousSibling();
		while(prevSib != null && !(prevSib instanceof Element))
		{
			prevSib = prevSib.getPreviousSibling();
		}
		if(nextSib instanceof Element){
			if(node != null && node.getNodeType() == Node.TEXT_NODE) {
				Text text = (Text)node;
				String nodeValue = text.getNodeValue();
				
				if(nodeValue != null && nodeValue.trim().length() == 0) {
					int index = nodeValue.lastIndexOf("\r\n");
					if(index == -1) {
						index = nodeValue.lastIndexOf("\n");
					}
					if(index != -1) {
						nodeValue = nodeValue.substring(0, index);
						if("".equals(nodeValue)) {
							node.getParentNode().removeChild(text);
						} else {
							text.setNodeValue(nodeValue);
						}
					}
					
				}
			}
		}
		//The case there is no Sibling after the text Node, i.e, the item removed was the last Item on the list
		else{
			removeNextText((Element)prevSib);
		}
	}
	
    public static Element getFirstChildElement(Node start) {
        Node node = start.getFirstChild();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start) {
        Node node = start.getNextSibling();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getFirstChildElement(Node start, String name) {
        Node node = start.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start, String name) {
        Node node = start.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getFirstChildElement(Node start, String ns, String name) {
        if (ns == null) {
            return getFirstChildElement(start, name);
        }
        Node node = start.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(name) && ns.equals(node.getNamespaceURI())) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start, String ns, String name) {
        if (ns == null) {
            return getNextElement(start, name);
        }
        Node node = start.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(name) && ns.equals(node.getNamespaceURI())) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static String getAttributeValue(Element element, String name) {
        Attr attr = element.getAttributeNode(name);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    public static String getTextContent(Node parent) {
        // Some DOMs do not support Node.getTextContent().
        Node textNode = getTextNode(parent);
        if (textNode != null) {
            return textNode.getNodeValue();
        }
        return null;
    }

    public static Node getTextNode(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null && node.getNodeType() != Node.TEXT_NODE) {
            node = node.getNextSibling();
        }
        return node;
    }
    
    static final String EXT_XSD = "http://www.ibm.com/xmlns/dde/schema/annotation/ext";
	static final String SCHEMA_XSD = "http://www.w3.org/2001/XMLSchema";
	static public String getLabel(CMNode node) {
		return getLabelFromSchema(node, EXT_XSD, "label");
		} 
	
	//If the Item does not have a label defined, look for the label in the schema
	static String getLabelFromSchema(CMNode node, String ns, String tag) {
		final String APPINFO = "appinfo";
		String label = null;
		XSDAnnotation annotation = null;
		if (node instanceof XSDImpl.XSDElementDeclarationAdapter) {
			XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDElementDeclaration) {
				XSDElementDeclaration edecl = (XSDElementDeclaration) target;
				//Try Element Annotation first
				annotation = edecl.getAnnotation();
			   if(annotation == null)
			   {
				   //If the element didn't have any annotation, try the type annotation
					XSDTypeDefinition type = edecl.getTypeDefinition();
					annotation = type.getAnnotation();
					while (annotation == null && type.getBaseType() != null && !type.isCircular()) {
						type = type.getBaseType();
						annotation = type.getAnnotation();
					}
			   }
			}
		} else if (node instanceof XSDImpl.XSDAttributeUseAdapter) {
			XSDImpl.XSDAttributeUseAdapter adapter = (XSDImpl.XSDAttributeUseAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDAttributeUse) {
				XSDAttributeUse use = (XSDAttributeUse) target;
				XSDAttributeDeclaration attr = use.getAttributeDeclaration();
				annotation = attr.getAnnotation();
				  
				if(annotation == null)
				{
				   //If the attribute didn't have any annotation, try the type annotation
					XSDTypeDefinition type = attr.getTypeDefinition();
					annotation = type.getAnnotation();
					while (annotation == null && type.getBaseType() != null && !type.isCircular()) {
						type = type.getBaseType();
						annotation = type.getAnnotation();
					}
				}
			}
		}
		if (annotation != null) {
			Element e = annotation.getElement();
			for (Element e2 = ModelUtil.getFirstChildElement(e, SCHEMA_XSD, APPINFO); e2 != null; e2 = ModelUtil.getNextElement(e2, SCHEMA_XSD, APPINFO)) {
				for (Element e3 = ModelUtil.getFirstChildElement(e2, ns, tag); e3 != null; e3 = ModelUtil.getNextElement(e3, ns, tag)) {
					String s = ModelUtil.getTextContent(e3);
					if (s != null) {
						return s.trim();
					}
				}
			}
		}
		return label;
	}

	/**
	 * Get the XSDSchema XSDTypeDefinition from a CMNode
	 * @param node
	 * @return
	 */
	public static XSDTypeDefinition getTypeDefinitionFromSchema(CMNode node) {
		XSDTypeDefinition type = null;
		if (node instanceof XSDImpl.XSDElementDeclarationAdapter) {
			XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDElementDeclaration) {
				XSDElementDeclaration edecl = (XSDElementDeclaration) target;
				type = edecl.getTypeDefinition();
			}
		}else if (node instanceof XSDImpl.XSDAttributeUseAdapter) {
			XSDImpl.XSDAttributeUseAdapter adapter = (XSDImpl.XSDAttributeUseAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDAttributeUse) {
				XSDAttributeUse use = (XSDAttributeUse) target;
				XSDAttributeDeclaration attr = use.getAttributeDeclaration();
				type = attr.getTypeDefinition();
			}
			if (target instanceof XSDAttributeDeclaration) {
				XSDAttributeDeclaration attr = (XSDAttributeDeclaration) target;
				type = attr.getTypeDefinition();
			}
		}
		return type;
	}

	
	//Helper Method to return the Schema type of an element or attribute Node
	public static String getTypeFromSchema(CMNode node) {
		String typeName = null;
		XSDTypeDefinition type = getTypeDefinitionFromSchema(node);
		if (type != null)
		{
			typeName = getTypeName(type.getTargetNamespace(), type.getName());
		}
		return typeName;
	}

	public static List<XSDSimpleTypeDefinition> getMemberTypesFromUnion(CMNode node) {
		List<XSDSimpleTypeDefinition> types = new ArrayList<XSDSimpleTypeDefinition>();
		XSDTypeDefinition type = getTypeDefinitionFromSchema(node);
		// Only look at anonymous simple types
		if(type instanceof XSDSimpleTypeDefinition && type.getName() == null) {
		    XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) type;
		    if (st.getVariety().equals(XSDVariety.UNION_LITERAL)) {
		       List<XSDSimpleTypeDefinition> memberTypeDefinitions = st.getMemberTypeDefinitions();
		       StringBuffer s = new StringBuffer();
		       for (XSDSimpleTypeDefinition std : memberTypeDefinitions) {
		    	   String name = std.getName();
		    	   String ns = std.getTargetNamespace();
		    	   if (name != null && name.length() > 0)
		    	   {
		    		  types.add(std);
		    	   }
		       }
		    }
	    }
		return types;
		
	}
		
	//Recursively finds all group declarations in type hierarchy
	public static HashMap<String, Element>  getGroupDeclFromType(XSDTypeDefinition typeDef)
	{
		HashMap<String, Element> map = new HashMap<String, Element>();
		getGroupDeclFromType(typeDef,map);
		return map;
	}
	
	//helper method to find group declaration in type hierarchy
	private static void getGroupDeclFromType(XSDTypeDefinition typeDef, HashMap<String, Element> map)
	{
		XSDTypeDefinition baseType = typeDef.getBaseType();
		XSDTypeDefinition rootType = typeDef.getRootType();
		if (rootType!=typeDef)
		{
			getGroupDeclFromType(baseType, map);
		}
		XSDAnnotation ann = typeDef.getAnnotation();
		if(ann != null)
		{
			EList<Element> elements = ann.getApplicationInformation();
			if (elements.size()>0)
			{
				Element element = elements.get(0);
				NodeList childNodes = element.getChildNodes();
				if (childNodes!=null && childNodes.getLength()>0)
				{
					for (int i=0; i< childNodes.getLength(); i++)
					{
						Node node = childNodes.item(i);
						if (node.getLocalName() !=null && node.getLocalName().equals("groupDecl"))
						{
							String groupID = ((Element)node).getAttribute("id");
							map.put(groupID, (Element)node);
						}
					}
				}
			}
		}
	}
	
	public static String getGroupIDFromSchema(CMNode node) {
		XSDAnnotation annotation = null;
		if (node instanceof XSDImpl.XSDElementDeclarationAdapter) {
			XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDElementDeclaration) {
				XSDElementDeclaration edecl = (XSDElementDeclaration) target;
				annotation = edecl.getAnnotation();
			}
		} else if (node instanceof XSDImpl.XSDAttributeUseAdapter) {
			XSDImpl.XSDAttributeUseAdapter adapter = (XSDImpl.XSDAttributeUseAdapter) node;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDAttributeUse) {
				XSDAttributeUse use = (XSDAttributeUse) target;
				XSDAttributeDeclaration attr = use.getAttributeDeclaration();
				annotation = attr.getAnnotation();
			}
		}
		if (annotation != null) {
			EList<Element> elements = annotation.getApplicationInformation();
			if (elements.size()>0)
			{
				Element e = elements.get(0);
				NodeList childNodes = e.getChildNodes();
				if (childNodes!=null)
				{
					for (int i=0; i< childNodes.getLength(); i++)
					{
						Node n = childNodes.item(i);
						if (n.getLocalName() !=null && n.getLocalName().equals("group"))
						{
							return ((Element)n).getAttribute("id");
						}
					}
				}
			}
		}
		return null;
	}

	private static String getTypeName(String namespace, String name) {
		if (namespace == null || namespace.isEmpty())
			return name;
		return namespace + ":" + name;
	}
}
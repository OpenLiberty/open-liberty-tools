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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMVisitor;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl.XSDElementDeclarationAdapter;
import org.eclipse.xsd.XSDAttributeGroupContent;
import org.eclipse.xsd.XSDAttributeGroupDefinition;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDComplexTypeContent;
import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDModelGroup;
import org.eclipse.xsd.XSDModelGroupDefinition;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDParticleContent;
import org.eclipse.xsd.XSDTypeDefinition;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.data.AtomicAttributeDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicDetailItem;
import com.ibm.xwt.dde.internal.data.AtomicElementDetailItem;
import com.ibm.xwt.dde.internal.data.DetailItem;
import com.ibm.xwt.dde.internal.data.RepeatableAtomicDetailItemSet;
import com.ibm.xwt.dde.internal.data.SimpleDetailItem;
import com.ibm.xwt.dde.internal.util.ModelUtil;

public class DetailsContentProvider {

	private Customization customization;
	private boolean doSchemaSort = false;
	
	public DetailsContentProvider(Customization customization) {
		this.customization = customization;
	}
	
	public DetailItem[] getItems(Object input) {
		if(input instanceof Element) {
			Element element = (Element)input;
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			if (modelQuery != null) {
				CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
				if (cmElementDeclaration != null) {
					// Obtain the detail items for the element declaration attributes
					return getDetailItemsForElement(element, cmElementDeclaration, true);
				}
			}
		}
		return new DetailItem[]{};
	}
	
	public void setSchemaSort(boolean doSchemaSort)
	{
		this.doSchemaSort = doSchemaSort;
	}

	private DetailItem[] getDetailItemsForElement(Element elementOrParentElement, CMElementDeclaration cmElementDeclaration, boolean elementExists) {
		List detailItems = new ArrayList();
		CMNamedNodeMap attributes = cmElementDeclaration.getAttributes();
		for(int i = 0; i < attributes.getLength(); i++) {
			CMAttributeDeclaration cmAttributeDelcaration = (CMAttributeDeclaration) attributes.item(i);
			AtomicAttributeDetailItem atomicAttributeDetailItem = null;
			String attributeNamespace = ModelUtil.getNamespaceURI(cmAttributeDelcaration);
			String attributePath = null;
			if(elementExists) {
				atomicAttributeDetailItem = new AtomicAttributeDetailItem(elementOrParentElement, cmAttributeDelcaration);
				attributePath = ModelUtil.getNodeFullPath(elementOrParentElement, cmAttributeDelcaration);
			} else {
				atomicAttributeDetailItem = new AtomicAttributeDetailItem(elementOrParentElement, cmElementDeclaration, cmAttributeDelcaration);
				attributePath = ModelUtil.getNodeFullPath(elementOrParentElement, cmElementDeclaration, cmAttributeDelcaration);
			}
			DetailItemCustomization detailItemCustomization = null;
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(attributeNamespace, attributePath);
			}
			if(detailItemCustomization == null && customization!=null)
			{
				detailItemCustomization = customization.getTypeCustomizationConsideringUnions(cmAttributeDelcaration, attributePath);
			}
			if(!(detailItemCustomization != null && detailItemCustomization.isHidden())) {
				atomicAttributeDetailItem.setDetailItemCustomization(detailItemCustomization);
				detailItems.add(atomicAttributeDetailItem);
			}
		}
		// If the element contains text, create a detail item for it
		if (cmElementDeclaration.getContentType() == CMElementDeclaration.PCDATA || cmElementDeclaration.getContentType() == CMElementDeclaration.MIXED) {
			AtomicElementDetailItem atomicElementDetailItem = null;
			String elementNamespace = ModelUtil.getNamespaceURI(cmElementDeclaration);
			String elementPath = null;
			if(elementExists) {
				atomicElementDetailItem = new AtomicElementDetailItem(elementOrParentElement);
				elementPath = ModelUtil.getElementFullPath(elementOrParentElement) + "/.";
			} else {
				atomicElementDetailItem = new AtomicElementDetailItem(elementOrParentElement, cmElementDeclaration);
				elementPath = ModelUtil.getNodeFullPath(elementOrParentElement, cmElementDeclaration) + "/.";
			}
			DetailItemCustomization detailItemCustomization = null;
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(elementNamespace, elementPath);
			}
			if(!(detailItemCustomization != null && detailItemCustomization.isHidden())) {
				atomicElementDetailItem.setDetailItemCustomization(detailItemCustomization);
				detailItems.add(atomicElementDetailItem);
			}
		}
		// Obtain the detail items from the content of the element declaration
		ElementContentVisitor visitor = null;
		if(elementExists) {
			visitor = new ElementContentVisitor(elementOrParentElement, customization);
		} else {
			visitor = new ElementContentVisitor(elementOrParentElement, cmElementDeclaration, customization);
		}
		visitor.visitCMNode(cmElementDeclaration.getContent());
		detailItems.addAll(visitor.getResult());

	    if (doSchemaSort)
	    {
            List schemaOrderedDetailItems = new ArrayList();

            if (cmElementDeclaration instanceof XSDElementDeclarationAdapter)
            {
                XSDElementDeclarationAdapter adapter = (XSDElementDeclarationAdapter) cmElementDeclaration;
                Object target = adapter.getTarget();
                if (target instanceof XSDElementDeclaration)
                {
                    XSDElementDeclaration elemDecl = (XSDElementDeclaration) target;
                    XSDTypeDefinition typeDefinition = elemDecl.getTypeDefinition();
                    if (typeDefinition instanceof XSDComplexTypeDefinition)
                    {
                        return analyzeComplexType(detailItems,	schemaOrderedDetailItems, typeDefinition);
                    }
                }
            }
	    }
		
		return (DetailItem[]) detailItems.toArray(new DetailItem[detailItems.size()]);
	}

	private DetailItem[] analyzeComplexType(List detailItems, List schemaOrderedDetailItems, XSDTypeDefinition typeDefinition) {
		XSDTypeDefinition ct = (XSDComplexTypeDefinition) typeDefinition;
		while (ct instanceof XSDComplexTypeDefinition) {
		    traverseHierarchy((XSDComplexTypeDefinition) ct, schemaOrderedDetailItems, detailItems);
		    ct = ct.getBaseType();
		    if ("anyType".equals(ct.getName())) {
		        break;
		    }
		}
		// Add whatever detail items remain
		schemaOrderedDetailItems.addAll(detailItems);
		return (DetailItem[]) schemaOrderedDetailItems.toArray(new DetailItem[schemaOrderedDetailItems.size()]);
	}
	
	/**
	 * Internal package.  Not API.  Do not use.
	 * @param input
	 * @param detailItems
	 * @return
	 */
	public DetailItem[] sortItemsBySchema(Object input, List detailItems)
	{
        List schemaOrderedDetailItems = new ArrayList();
		if(input instanceof Element) {
			Element element = (Element)input;
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			if (modelQuery != null) {
				CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
				if (cmElementDeclaration != null) {
		            XSDElementDeclarationAdapter adapter = (XSDElementDeclarationAdapter) cmElementDeclaration;
		            Object target = adapter.getTarget();
		            if (target instanceof XSDElementDeclaration)
		            {
		                XSDElementDeclaration elemDecl = (XSDElementDeclaration) target;
		                XSDTypeDefinition typeDefinition = elemDecl.getTypeDefinition();
		                if (typeDefinition instanceof XSDComplexTypeDefinition)
		                {
		                    return analyzeComplexType(detailItems, schemaOrderedDetailItems, typeDefinition);
		                }
		            }
				}
			}
        }
        return (DetailItem[]) detailItems.toArray(new DetailItem[detailItems.size()]);

	}
	
	// Inner class used for obtaining the detail items from the CMContent of a CMElementDeclaration
	private class ElementContentVisitor extends CMVisitor {                                             
		private ModelQuery modelQuery;
		private Element parentElement;
		private Element element;
		private CMElementDeclaration cmElementDeclaration;
		private List detailItems;
		private Customization customization;


		public ElementContentVisitor(Element element, Customization customization) {
			this.element = element;
			this.modelQuery =  ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			this.cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
			detailItems = new ArrayList();
			this.customization = customization;
		}

		public ElementContentVisitor(Element parentElement, CMElementDeclaration cmElementDeclaration, Customization customization) {
			this.parentElement = parentElement;
			this.modelQuery =  ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			this.cmElementDeclaration = cmElementDeclaration;
			detailItems = new ArrayList();
			this.customization = customization;
		}

		public void visitCMElementDeclaration(CMElementDeclaration currentCMElementDeclaration) {
			String elementNamespace = ModelUtil.getNamespaceURI(currentCMElementDeclaration);
			String elementPath = null;
			if(element != null) {
				elementPath = ModelUtil.getNodeFullPath(element, currentCMElementDeclaration);
			} else {
				elementPath = ModelUtil.getNodeFullPath(parentElement, cmElementDeclaration, currentCMElementDeclaration);
			}
			DetailItemCustomization detailItemCustomization = null;
			if(customization != null) {
				detailItemCustomization = customization.getItemCustomization(elementNamespace, elementPath);
			}
			// if the element is abstract, don't process it
			boolean elementIsAbstract = false;
			Object object = currentCMElementDeclaration.getProperty("Abstract");
			if(object instanceof Boolean && ((Boolean)object).booleanValue()) {
				elementIsAbstract = true;
			}
			if(!elementIsAbstract){
				if(!(detailItemCustomization != null && (detailItemCustomization.isHidden() || detailItemCustomization.getStyle() == DetailItemCustomization.STYLE_TREE_NODE))) {
					if(detailItemCustomization != null && detailItemCustomization.getStyle() != DetailItemCustomization.STYLE_DEFAULT) {
						AtomicDetailItem atomicDetailItem = null;
						if(element != null) {
							Element[] instances = ModelUtil.getInstancesOfElement(element, currentCMElementDeclaration);
							// REF 80656 - Can potentially find more than one instance of this element
							// Change from == 1 to >= 1
							// Otherwise, we are making the wrong assumption that there is no element in the DOM
							if(instances.length >= 1) {
								atomicDetailItem = new AtomicElementDetailItem(instances[0]);
							} else {
								atomicDetailItem = new AtomicElementDetailItem(element, currentCMElementDeclaration);
							}
						} else {
							atomicDetailItem = new AtomicElementDetailItem(parentElement, cmElementDeclaration, currentCMElementDeclaration);
						}
						atomicDetailItem.setDetailItemCustomization(detailItemCustomization);
						detailItems.add(atomicDetailItem);
					} else {
						processElementDeclarationByDefault(currentCMElementDeclaration, detailItemCustomization);
					}
				}
			}
		}
		
		
		List getResult() {
			return detailItems;
		} 
		
		
		private void processElementDeclarationByDefault(CMElementDeclaration currentCMElementDeclaration, DetailItemCustomization detailItemCustomization) {
			// Verify the context of the element declaration to ensure it is not in a choice or repeatable group
			boolean singleOccurrence = false;
			if(detailItemCustomization != null) {
				singleOccurrence = detailItemCustomization.isSingleOccurrence();
			}
			if(((ModelUtil.getGroupTypesInBetween(cmElementDeclaration, currentCMElementDeclaration) & (ModelUtil.CHOICE | ModelUtil.REPEATABLE)) == 0) || singleOccurrence) {
				// Verify if the element declaration is atomic
				boolean isAtomic = false;
				if(element != null) {
					isAtomic = ModelUtil.isAtomicCMElementDeclaration(customization, element, currentCMElementDeclaration);					
				} else {
					isAtomic = ModelUtil.isAtomicCMElementDeclaration(customization, parentElement, cmElementDeclaration, currentCMElementDeclaration);					
				}
				if(isAtomic) {
					// Verify if the atomic element declaration is repeatable
					if(ModelUtil.isCMNodeRepeatable(currentCMElementDeclaration) && !singleOccurrence) {
						// create a detail item for the repeatable atomic element declaration
						RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = null;
						if(element != null) {
							repeatableAtomicDetailItemSet = new RepeatableAtomicDetailItemSet(element, currentCMElementDeclaration);
						} else {
							repeatableAtomicDetailItemSet = new RepeatableAtomicDetailItemSet(parentElement, cmElementDeclaration, currentCMElementDeclaration);
						}
						repeatableAtomicDetailItemSet.setDetailItemCustomization(detailItemCustomization);
						detailItems.add(repeatableAtomicDetailItemSet);
					} else {
						// Current atomic element declaration is non-repeatable, look in the DOM to see if there are instances of it
						Element instances[] = null;
						if(element != null) {
							instances = ModelUtil.getInstancesOfElement(element, currentCMElementDeclaration);
						}
						if(instances != null && instances.length > 0) {
							for (int i = 0; i < instances.length; i++) {
								AtomicElementDetailItem atomicElementDetailItem = new AtomicElementDetailItem(instances[i]);
								atomicElementDetailItem.setDetailItemCustomization(detailItemCustomization);
								detailItems.add(atomicElementDetailItem);
							}
						} else {
							// Create a detail item for it from the content model element declaration
							AtomicElementDetailItem atomicElementDetailItem = null;
							if(element != null) {
								atomicElementDetailItem = new AtomicElementDetailItem(element, currentCMElementDeclaration);
							} else {
								atomicElementDetailItem = new AtomicElementDetailItem(parentElement, cmElementDeclaration, currentCMElementDeclaration);
							}
							atomicElementDetailItem.setDetailItemCustomization(detailItemCustomization);
							detailItems.add(atomicElementDetailItem);
						}
					}
				} else if(element != null && ModelUtil.isSimpleCMElementDeclaration(customization, element, currentCMElementDeclaration)) {
					if(ModelUtil.isCMNodeRepeatable(currentCMElementDeclaration) && !singleOccurrence) {
						/* for now repeatable simple items wont be shown on the details section */
					} else {
						// Current simple element is non-repeatable, look in the DOM to see if there are instances of it
						Element[] instances = ModelUtil.getInstancesOfElement(element, currentCMElementDeclaration);
						if(instances.length > 0) {
							for (int i = 0; i < instances.length; i++) {
								// Create a simple item for each instance found
								DetailItem[] atomicDetailItems = getDetailItemsForElement(instances[i], currentCMElementDeclaration, true);
								SimpleDetailItem simpleDetailItem = new SimpleDetailItem(currentCMElementDeclaration, instances[i], atomicDetailItems);
								simpleDetailItem.setDetailItemCustomization(detailItemCustomization);
								detailItems.add(simpleDetailItem);
							}

						} else {
							// If no instances of the simple item are found in the DOM, create an empty one from the content model
							DetailItem[] atomicDetailItems = getDetailItemsForElement(element, currentCMElementDeclaration, false);
							SimpleDetailItem simpleDetailItem = new SimpleDetailItem(currentCMElementDeclaration, atomicDetailItems);
							simpleDetailItem.setDetailItemCustomization(detailItemCustomization);
							detailItems.add(simpleDetailItem);
						}
					}
				}
			}
		}
		
	}

	private void traverseHierarchy(XSDComplexTypeDefinition ct, List schemaOrderedDetailItems, List detailItems)
	{
        for (XSDAttributeGroupContent gc : ct.getAttributeContents())
        {
            if (gc instanceof XSDAttributeUse)
            {
                XSDAttributeUse au = (XSDAttributeUse) gc;
                String targetName = au.getAttributeDeclaration().getName();
                for (Object di : detailItems)
                {
                    if (di instanceof AtomicAttributeDetailItem)
                    {
                      AtomicAttributeDetailItem aadi = (AtomicAttributeDetailItem) di;
                      String s = aadi.getName();
                      if (s!= null && s.equals(targetName))
                      {
                          schemaOrderedDetailItems.add(di);
                          detailItems.remove(di);
                          break;
                      }
                    }
                }
            }
            else if (gc instanceof XSDAttributeGroupDefinition)
            {
                XSDAttributeGroupDefinition agd = ((XSDAttributeGroupDefinition)gc).getResolvedAttributeGroupDefinition();
                for (XSDAttributeUse au : agd.getAttributeUses())
                {
                    String targetName = au.getAttributeDeclaration().getName();
                    for (Object di : detailItems)
                    {
                        if (di instanceof AtomicAttributeDetailItem)
                        {
                          AtomicAttributeDetailItem aadi = (AtomicAttributeDetailItem) di;
                          String s = aadi.getName();
                          if (s!= null && s.equals(targetName))
                          {
                              schemaOrderedDetailItems.add(di);
                              detailItems.remove(di);
                              break;
                          }
                        }
                    }

                }
            }
        }
        XSDComplexTypeContent content = ct.getContent();
        if (content instanceof XSDParticle)
        {
           XSDParticleContent xsdParticleContent = ((XSDParticle)content).getContent();
           if (xsdParticleContent instanceof XSDModelGroup)
           {
              XSDModelGroup mg = (XSDModelGroup) xsdParticleContent;
              checkModelGroup(mg, schemaOrderedDetailItems, detailItems);  
           }
           else if (xsdParticleContent instanceof XSDModelGroupDefinition)
           {
              XSDModelGroupDefinition groupDef = (XSDModelGroupDefinition) xsdParticleContent;
              groupDef = groupDef.getResolvedModelGroupDefinition();
              XSDModelGroup mg = groupDef.getModelGroup();
              List<XSDParticle> contents = mg.getContents();
              for (XSDParticle p : contents)
              {
                  XSDParticleContent content2 = p.getContent();
                  if (content2 instanceof XSDModelGroup)
                  {
                      checkModelGroup((XSDModelGroup)content2, schemaOrderedDetailItems, detailItems);
                  }
              }

              
           }
           
        }
        
        
	}

    private void checkModelGroup(XSDModelGroup mg, List schemaOrderedDetailItems, List detailItems)
    {
        List<XSDParticle> particles = mg.getParticles();
        for (XSDParticle part : particles)
        {
           XSDParticleContent content2 = part.getContent();
           if (content2 instanceof XSDElementDeclaration)
           {
               XSDElementDeclaration xsdElem = (XSDElementDeclaration) content2;
               String targetName = xsdElem.getName();
               for (Object di : detailItems)
               {
                   if (di instanceof AtomicElementDetailItem)
                   {
                     AtomicElementDetailItem aedi = (AtomicElementDetailItem) di;
                     String s = aedi.getName();
                     if (s!= null && s.equals(targetName))
                     {
                         schemaOrderedDetailItems.add(di);
                         detailItems.remove(di);
                         break;
                     }
                   }
                   else if (di instanceof SimpleDetailItem)
                   {
                       SimpleDetailItem adi = (SimpleDetailItem) di;
                       String s = adi.getName();
                       if (s!=null && s.equals(targetName))
                       {
                           schemaOrderedDetailItems.add(di);
                           detailItems.remove(di);
                           break;
                       }
                   }
               }
           }
           else if (content2 instanceof XSDModelGroupDefinition)
           {
              XSDModelGroupDefinition groupDef = (XSDModelGroupDefinition) content2;
              groupDef = groupDef.getResolvedModelGroupDefinition();
              XSDModelGroup mg2 = groupDef.getModelGroup();
              checkModelGroup(mg2, schemaOrderedDetailItems, detailItems);
           }

        }
    }

}
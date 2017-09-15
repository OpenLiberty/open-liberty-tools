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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomIconObject;
import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.ValidationManager;
import com.ibm.xwt.dde.internal.viewers.DDEViewer.TreeFilterProcessor;

public class TreeLabelProvider extends LabelProvider implements IFontProvider {

	private static final Image DEFAULT_ELEMENT_ICON = DDEPlugin.getDefault().getImageDescriptor("icons/parameters.gif").createImage(); //$NON-NLS-1$

	private Customization customization;
	private IResource resource;
	private TreeFilterProcessor treeFilterProcessor;
	private ValidationManager validationManager;
	
	public TreeLabelProvider(Customization customization, TreeFilterProcessor treeFilterProcessor, ValidationManager validationManager, IResource resource) {
		this.customization = customization;
		this.resource = resource;
		this.treeFilterProcessor = treeFilterProcessor;
		this.validationManager = validationManager;
	}

	public Image getImage(Object element) {
		return getImage(element, false);
	}
	
	public Image getImage(Object element, boolean ignoreDecoration)  {
		Image result = null;
		if (element instanceof Element) {
			Element domElement = (Element) element;

			// Verify if there is a customized icon for the element
			DetailItemCustomization detailItemCustomization = getDetailItemCustomization(domElement);
			if(detailItemCustomization != null && detailItemCustomization.getIcon() != null) {
				result = detailItemCustomization.getIcon();
			} 
			//if there is no icon attribute try iconClass
			else if(detailItemCustomization != null && detailItemCustomization.getIconClass() != null) {
				Object object;
				try {
					object = detailItemCustomization.getIconClass().newInstance();
					if(object instanceof ICustomIconObject) {
						ICustomIconObject customLabelObject = (ICustomIconObject)object;
						result = customLabelObject.getIcon((Element)element, resource);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
			else if(customization!=null && customization.getIconClass()!=null) {
				Object object;
				try {
					object = customization.getIconClass().newInstance();
					if(object instanceof ICustomIconObject) {
						ICustomIconObject customLabelObject = (ICustomIconObject)object;
						result = customLabelObject.getIcon((Element)element, resource);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
			else {			
				result = DEFAULT_ELEMENT_ICON;
			}
			if(!ignoreDecoration) {
				result = decorateImage(result, domElement);
			}
		}
		return result;
	}

	public String getText(Object input)	{
		if (input instanceof Element) {
			Element treeNodeElement = (Element)input;
			return ModelUtil.getTreeNodeLabel(treeNodeElement, customization, resource);
		}
		return "";
	}
	
	
	
	private DetailItemCustomization getDetailItemCustomization(Element element) {
		DetailItemCustomization itemCustomization = null;
		if(customization != null) {
			String path = ModelUtil.getElementFullPath(element);
			String elementNamespace = ModelUtil.getNodeNamespace(element);
			itemCustomization = customization.getItemCustomization(elementNamespace, path);
			if( itemCustomization == null)
			{
				ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
				if (modelQuery != null) {
					CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
					itemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
				}				
			}
		}
		return itemCustomization;
	}



	public Font getFont(Object element) {
		if(element instanceof Element) {
			Element domElement = (Element)element;
			if(treeFilterProcessor.isMatchedElement(domElement)) {
				return DDEPlugin.getDefault().FONT_DEFAULT_BOLD;
			}
		}
		return null;
	}

	private Image decorateImage(Image image, Element element) {
		ImageDescriptor decoratorImageDescriptor = null;
		
		int decoration = 0; /* 0: no decoration, 1: information, 2: warning, 3:error, 4:quickFix */
		
		if(validationManager.getMessageManager().getTreeNodeMessageCount(element, ValidationMessage.MESSAGE_TYPE_ERROR, false) > 0) {
			decoration = 3;
//			/***/
//			if(validationManager.getMessageManager().getTreeNodeMissingRequiredChildren(element).length > 0) {
//				decoration = 4;
//			}
//			/***/
		} else if(validationManager.getMessageManager().getTreeNodeMessageCount(element, ValidationMessage.MESSAGE_TYPE_WARNING, false) > 0) {
			decoration = 2;
		}

		switch (decoration) {
		case 0:
			return image;
		case 2:
			decoratorImageDescriptor = DDEPlugin.getDefault().getImageDescriptor("icons/warning_overlay.gif");
			break;
		case 3:
			decoratorImageDescriptor = DDEPlugin.getDefault().getImageDescriptor("icons/error_overlay.gif");
			break;
		case 4:
			decoratorImageDescriptor = DDEPlugin.getDefault().getImageDescriptor("icons/error_fix_overlay.gif");
			break;
		}
		String imageCode = image.hashCode() + "-" + decoration;
		Image decoratedImage = DDEPlugin.getDefault().getImageFromRegistry(imageCode);
		if (decoratedImage == null) {
			DecorationOverlayIcon decorationOverlayIcon = new DecorationOverlayIcon(image, decoratorImageDescriptor, IDecoration.BOTTOM_LEFT);
			decoratedImage = decorationOverlayIcon.createImage();
			DDEPlugin.getDefault().getImageRegistry().put(imageCode, decoratedImage);
		}
		return decoratedImage;
	}

}

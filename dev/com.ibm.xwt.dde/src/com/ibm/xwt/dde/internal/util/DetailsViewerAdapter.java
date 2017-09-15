/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.xsd.util.XSDConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.viewers.DetailsContentProvider;
import com.ibm.xwt.dde.internal.viewers.DetailsViewer;

public class DetailsViewerAdapter {
	
    protected DetailsViewer detailsViewer = null;
    protected Element element = null;  
    private final String NO_NAMESPACE_LOCATION = "xsi:noNamespaceSchemaLocation"; //$NON-NLS-1$
    private final String SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

    /**
     * Creates details viewer adapter
     */
    public DetailsViewerAdapter() {
    }
    
    /**
     * Creates details viewer inside given composite 
     * @param parent composite
     * @param customization
     */
    public DetailsViewerAdapter(Composite parent, Customization customization) {
    	init(parent,customization);
    }
    
    /**
     * Initializes details viewer inside given composite 
     * @param parent composite
     * @param customization
     */
    public void init(Composite parent, Customization customization)
    {
    	TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();
    	widgetFactory.setBackground(null);
    	detailsViewer = new DetailsViewer(parent, widgetFactory, null, customization, null, null);
    	detailsViewer.setContentProvider(new DetailsContentProvider(customization));
    }
   
    /**
     * Creates an element with given parameters and sets input to DetailsViewer
     * @param absoluteSchemaPath
     * @param elementName (can use prefix)
     */
    public void createElement(String schemaPath, String elementName) {
    	createElement(schemaPath, null, elementName, null);
    }
    
    /**
     * Creates an element with given parameters and sets input to DetailsViewer
     * @param absoluteSchemaPath
     * @param nameSpace
     * @param elementName (can use prefix)
     */
    public void createElement(String schemaPath, String schemaNameSpace, String elementName) {
    	createElement(schemaPath, schemaNameSpace, elementName, null);
    }
    
    /**
     * Creates an element with given parameters and sets input to DetailsViewer
     * @param absoluteSchemaPath
     * @param elementName (can use prefix)
     * @param properties (used to specify attribute values)
     */
    public void createElement(String schemaPath, String elementName, Map<String, String> properties) {
    	createElement(schemaPath, null, elementName, properties);
    }
    
    /**
     * Creates an element with given parameters and sets input to DetailsViewer
     * @param absoluteSchemaPath
     * @param nameSpace
     * @param elementName (can use prefix)
     * @param properties (used to specify attribute values)
     */
    public void createElement(String schemaPath, String schemaNameSpace , String elementName, Map<String, String> properties) {
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	IStructuredModel sm = null;
    	try {
    		dbf.setNamespaceAware(true);
    		DocumentBuilder db = dbf.newDocumentBuilder(); 
    	    Document doc = db.newDocument();
    	    Element new_el = null;
    	    
    	    if (schemaNameSpace!=null) {
	    		elementName = "tns:"+elementName;
	    		new_el = doc.createElement(elementName); //$NON-NLS-1$
	    		new_el.setAttribute("xmlns:xsi", XSDConstants.SCHEMA_INSTANCE_URI_2001);  //$NON-NLS-1$
		    	new_el.setAttribute("xmlns:tns", schemaNameSpace);  //$NON-NLS-1$
		    	new_el.setAttribute(SCHEMA_LOCATION, schemaNameSpace+" "+schemaPath); //$NON-NLS-1$
	    	}
    	    else {
    	    	new_el = doc.createElement(elementName);
    	    	new_el.setAttribute("xmlns:xsi", XSDConstants.SCHEMA_INSTANCE_URI_2001);  //$NON-NLS-1$
    	    	new_el.setAttribute(NO_NAMESPACE_LOCATION, schemaPath);
    	    }

    	    if (properties!=null && properties.size()>0) {
    	    	Set<String> keys = properties.keySet();
    	    	for (String key : keys) {
	    			new_el.setAttribute(key, properties.get(key));
				}
    	    }
    		doc.appendChild(new_el);
    		
    	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    		DOMSource source = new DOMSource(doc);
    		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    		StreamResult result = new StreamResult(outputStream);
    		transformer.transform(source, result);
    		
    		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

    		sm = StructuredModelManager.getModelManager().getModelForRead(ContentTypeIdForXML.ContentTypeID_XML, inputStream, null);    	    
    	    doc = ((IDOMModel) sm).getDocument();    	  
    	    sm.reload(inputStream);
        	
    	    Element el = null;
    	    NodeList nodeList = doc.getElementsByTagName(elementName);
            if (nodeList != null && nodeList.getLength() > 0) {
                el = (Element) nodeList.item(0);
                element = el;               
            }   	
	    }
	    catch (Exception e) {
			e.printStackTrace();
		}
	    finally {
	    	if (sm != null)
	    		sm.releaseFromRead();
	    }
    }
    
    /**
     * Creates all widgets inside details viewer
     */
    public void loadContents() {
    	if(detailsViewer!=null)
    		detailsViewer.setInput(element);
    }
    
    /**
     * Sets the element to be used by details viewer
     * @param element
     */
    public void setElement(Element el) {
    	element = el;
    }
    
    /**
     * returns details viewer composite
     * @return 
     */
    public Control getControl() {
    	if(detailsViewer != null) {
    		return detailsViewer.getControl();
    	}
    	else {
    		return null;
    	}
	}
    
    /**
     * returns element that is used by details viewer
     * @return
     */
    public Element getElement() {
		return element;
	}
    
    private boolean isSchemaAttr(Attr attr) {
    	String attrKey = attr.getName();
    	if(attrKey.equals(NO_NAMESPACE_LOCATION) || attrKey.equals(SCHEMA_LOCATION)) { 
    		return true;
    	}
    	if(attrKey.equals("xmlns:xsi") || attrKey.equals("xmlns:tns")) { //$NON-NLS-1$ //$NON-NLS-2$ 
    		return true;
    	}
    	return false;
    }
    
    /**
     * returns element attributes
     * @return
     */
    public Map<String, String> getElementProperties() {
    	NamedNodeMap nodeMap = element.getAttributes();
    	Map<String,String> prop = new LinkedHashMap<String, String>();
    	for (int i = 0; i < nodeMap.getLength(); i++) {
    		Attr attr = (Attr) nodeMap.item(i);
    		if (!isSchemaAttr(attr))
    			prop.put(attr.getLocalName(), attr.getValue());
    	}
		return prop;
	}

}

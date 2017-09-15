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
package com.ibm.xwt.dde.internal.customization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.customization.ICustomControlObject3;
import com.ibm.xwt.dde.customization.ICustomLabelObject;
import com.ibm.xwt.dde.customization.ICustomMultipleDeletionObject;
import com.ibm.xwt.dde.customization.ICustomPreSelectedTreeObject;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.controls.AbstractControl;
import com.ibm.xwt.dde.internal.controls.CustomSection;
import com.ibm.xwt.dde.internal.controls.HyperLink;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;


public class CustomizationManager {

	private static final CustomizationManager customizationManager = new CustomizationManager();
	private static final String EXTENSION_ID = "com.ibm.xwt.dde.customization"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String CUSTOMIZATION_ELEMENT = "customization"; //$NON-NLS-1$ 
	private static final String CUSTOMIZATION_ADD_ON_ELEMENT = "customization-add-on"; //$NON-NLS-1$
	private static final String CUSTOMIZATION_FILE = "customizationFile";  //$NON-NLS-1$   
	private static final String CUSTOMIZATION_PROPERTIES_FILE = "translations";  //$NON-NLS-1$
	private static final String OVERRIDE_CUSTOMIZATION_NAMESPACE = "overrideCustomizationNamespace";//$NON-NLS-1$
	private static final String OVERRIDE_CUSTOMIZATION_SCHEMA_LOCATION = "overrideCustomizationSchemaLocation";//$NON-NLS-1$
	private static final String DEBUG_MODE = "debugMode";  //$NON-NLS-1$
	private static final String POLLING_LOOP_MODE = "com.ibm.xwt.dde.fileMonitor";  //$NON-NLS-1$
	private static final String TRUE = "true";  //$NON-NLS-1$
	private static final String PRIORITY = "priority";  //$NON-NLS-1$
	private HashMap customizationMap;
	private List customizationAddOns;
	private FileMonitor fileMonitor;

	
	public static CustomizationManager getInstance() {
		return customizationManager;
	}

	
	private CustomizationManager() {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		customizationMap = new HashMap();
		customizationAddOns = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement configurationElement = extensions[i];
			if(CUSTOMIZATION_ELEMENT.equals(configurationElement.getName())) {
				String customizationID = configurationElement.getAttribute(ID);
				String customizationFile = configurationElement.getAttribute(CUSTOMIZATION_FILE);
				String customizationPropertiesFile = configurationElement.getAttribute(CUSTOMIZATION_PROPERTIES_FILE);
				String overrideCustomizationNamespace = configurationElement.getAttribute(OVERRIDE_CUSTOMIZATION_NAMESPACE);
				String overrideCustomizationSchemaLocation = configurationElement.getAttribute(OVERRIDE_CUSTOMIZATION_SCHEMA_LOCATION);
				boolean debugMode = false;
				if(TRUE.equals(configurationElement.getAttribute(DEBUG_MODE))) {
					debugMode = true;
				}
				Customization customization = new Customization(customizationFile, customizationPropertiesFile, configurationElement, customizationID, debugMode);
				if (overrideCustomizationNamespace != null && overrideCustomizationSchemaLocation != null){
					customization.setOverrides(overrideCustomizationNamespace, overrideCustomizationSchemaLocation);
				}
				customizationMap.put(customizationID, customization);
			} else if(CUSTOMIZATION_ADD_ON_ELEMENT.equals(configurationElement.getName())) {
				String customizationID = configurationElement.getAttribute(ID);
				String customizationFile = configurationElement.getAttribute(CUSTOMIZATION_FILE);
				String customizationPropertiesFile = configurationElement.getAttribute(CUSTOMIZATION_PROPERTIES_FILE);
				String customizationFilePriority = configurationElement.getAttribute(PRIORITY);
				int priority = Integer.parseInt(customizationFilePriority);
				boolean debugMode = false;
				if(TRUE.equals(configurationElement.getAttribute(DEBUG_MODE))) {
					debugMode = true;
				}
				CustomizationAddOn customizationAddOn = new CustomizationAddOn(customizationID, customizationFile, customizationPropertiesFile, debugMode, configurationElement, priority);
				customizationAddOns.add(customizationAddOn);
			}
		}

		Collections.sort(customizationAddOns, new Comparator(){
			public int compare(Object a, Object b) {
				CustomizationAddOn c1 = (CustomizationAddOn)a;
				CustomizationAddOn c2 = (CustomizationAddOn)b;
				return c1.getPriority() - c2.priority;
			}
		});
		if (System.getProperty(POLLING_LOOP_MODE) != null) {
			fileMonitor = new FileMonitor();
			fileMonitor.start();
		}
	}
	
	private class CustomizationAddOn {
		
		private String customizationID;
		private String customizationFile;
		private String customizationFilePath;
		private String customizationPropertiesFile;
		private String customizationPropertiesFilePath;
		private boolean debugMode;
		private IConfigurationElement configurationElement;
		private int priority;
		
		public CustomizationAddOn(String customizationID, String customizationFile, String customizationPropertiesFile, boolean debugMode, IConfigurationElement configurationElement, int priority) {
			this.customizationID = customizationID;
			this.customizationFile = customizationFile;
			this.customizationPropertiesFile = customizationPropertiesFile;
			this.debugMode = debugMode;
			this.configurationElement = configurationElement;
			this.priority = priority;
			Bundle bundle = Platform.getBundle(configurationElement.getContributor().getName());
			try {
				customizationFilePath = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationFile), null)).toExternalForm();
				customizationPropertiesFilePath = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationPropertiesFile), null)).toExternalForm();
			} catch (IOException e) {}
		}
		
		public String getCustomizationID() {
			return customizationID;
		}
		
		public String getCustomizationFile() {
			return customizationFile;
		}
		
		public String getCustomizationFilePath() {
			return customizationFilePath;
		}
		
		public String getCustomizationPropertiesFile() {
			return customizationPropertiesFile;
		}
		
		public String getCustomizationPropertiesFilePath() {
			return customizationPropertiesFilePath;
		}
		
		public boolean isDebugMode() {
			return debugMode;
		}
		
		public IConfigurationElement getConfigurationElement() {
			return configurationElement;
		}
		
		public int getPriority() {
			return priority;
		}
		
	}
	
	public synchronized void resetCustomizations() {
		Collection entries = customizationMap.values();
		for(Iterator iterator = entries.iterator(); iterator.hasNext();) {
			Customization customization = (Customization) iterator.next();
			if(customization!= null && customization.isParsed()) {
				customization.parse();
			}
		}
	}

	
	public synchronized Customization getCustomization(String customizationID) {
		Customization customization = (Customization) customizationMap.get(customizationID);
		if(customization != null) {
			if(!customization.isParsed()) {
				customization.parse();
			}
			return customization;
		}
		return null;
	}
	

	public class Customization {
		
		public static final int TREE_SORTING_PREFERENCE_DEFAULT = 0;
		public static final int TREE_SORTING_PREFERENCE_SORTED = 1;
		public static final int TREE_SORTING_PREFERENCE_UNSORTED = 2;
		public static final int TREE_SORTING_PREFERENCE_ALWAYS_SORTED = 3;
		public static final int TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED = 4;
		public static final int DETAILS_SORTING_PREFERENCE_DEFAULT = 0;
		public static final int DETAILS_SORTING_PREFERENCE_SCHEMA = 1;

		private HashMap itemCustomizationMap;
		private HashMap typeCustomizationMap;
		private HashMap itemCustomizationMapForRegularExpresions;
		private String customizationFile;
		private String customizationPropertiesFile;
		private String customizationNamespace;
		private String customizationSchemaLocation;
		private String overrideCustomizationNamespace;
		private String overrideCustomizationSchemaLocation;
		private IConfigurationElement configurationElement;
		private String customizationFilePath;
		private String customizationPropertiesFilePath;
		private String headerLabel;
		private List typeNames = new ArrayList<String>();;
		private String overviewSectionTitle;
		private boolean globalDetectSchemaLabel;
		private Class addButtonClass;
		private String globalValidationClass;
		private Class globalIconClass;
		private Class globalDetailSectionTitleClass;
		private Class globalTreeLabelClass;
		private String overviewSectionDescription;
		private String editorTitle;
		private ICustomLabelObject overviewSectionDescriptionObject;
		private Image headerIcon;
		private Action[] headerActions;
		private ICustomLabelObject headerLabelObject;
		private ICustomLabelObject overviewSectionTitleObject;
		private String customizationID;
		private boolean debugMode;
		private boolean isParsed;
		private boolean displayDocumentationAsHoverHelp;
		private String helpContextId;
		private int treeSortingPreference;
		private String treeSortConfirmationMessage;
		private String treeUnsortConfirmationMessage;
		private boolean hideOverviewSection;
		private ICustomMultipleDeletionObject multipleDeletionObject;
		private ICustomPreSelectedTreeObject preSelectedTreeElementObject;
		private int detailsSortingPreference;
		private boolean hideRepeatableItemNumbers;
		private boolean enableSchemaGroups;
		private boolean addChildHelperEnabled;
		private int addChildHelperLimit = 10;
		private Class emptyElementCustomControlClass;
		private ICustomControlObject3 emptyElementCustomControlObject;

		
		public Customization(String customizationFile, String customizationPropertiesFile, IConfigurationElement configurationElement, String customizationID, boolean debugMode) {
			this.customizationID = customizationID;
			this.customizationFile = customizationFile;
			this.customizationPropertiesFile = customizationPropertiesFile;
			this.configurationElement = configurationElement;
			this.debugMode = debugMode;
			Bundle bundle = Platform.getBundle(configurationElement.getContributor().getName());
			try {
				customizationFilePath = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationFile), null)).toExternalForm();
			} catch (Exception e) {
				ILog log = DDEPlugin.getDefault().getLog();
				MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_CUSTOMIZATION_FILE_NOT_FOUND);
				String errorMessage = messageFormat.format(new String[]{customizationFile, customizationID, bundle.getSymbolicName()});
				IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage);
				log.log(status);
			}
			if(customizationPropertiesFile != null) {
				try {
					customizationPropertiesFilePath = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationPropertiesFile), null)).toExternalForm();					
				} catch(Exception e) {
					//e.printStackTrace();
					ILog log = DDEPlugin.getDefault().getLog();
					MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_CUSTOMIZATION_PROPERTIES_FILE_NOT_FOUND);
					String errorMessage = messageFormat.format(new String[]{customizationPropertiesFile, customizationID, bundle.getSymbolicName()});
					IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage);
					log.log(status);
				}
			}
		}
		
		public List getTypeName()
		{
			return typeNames;
		}
		
		public String getOverviewSectionDescription() {
			return overviewSectionDescription;
		}
		
		public ICustomLabelObject getOverviewSectionDescriptionObject() {
			return overviewSectionDescriptionObject;
		}
		
		public String getOverviewSectionTitle() {
			return overviewSectionTitle;
		}
		
		public boolean getGlobalDetectSchemaLabel()
		{
			return globalDetectSchemaLabel;
		}
		
		public Class getAddButtonClass()
		{
			return addButtonClass;
		}
		
		public Class getIconClass()
		{
			return globalIconClass;
		}
		
		public Class getTreeLabelClass()
		{
			return globalTreeLabelClass;
		}
		
		public Class getDetailSectionTitleClass()
		{
			return globalDetailSectionTitleClass;
		}
		
		public String getGlobalValidationClass()
		{
			return globalValidationClass;
		}
		
		public boolean getHideRepeatableItemNumbers()
		{
			return hideRepeatableItemNumbers;
		}
		
		public boolean getEnableSchemaGroups()
		{
			return enableSchemaGroups;
		}
		
		public ICustomMultipleDeletionObject getCustomMultipleDeletionObject() {
			return multipleDeletionObject;
		}
		
		public ICustomLabelObject getOverviewSectionTitleObject() {
			return overviewSectionTitleObject;
		}

		public String getHeaderLabel() {
			return headerLabel;
		}
		
		public Image getHeaderIcon() {
			return headerIcon;
		}
		
		public Action[] getHeaderActions() {
			return headerActions;
		}
		
		public String getCustomizationFilePath() {
			return customizationFilePath;
		}
		
		public String getCustomizationPropertiesFilePath() {
			return customizationPropertiesFilePath;
		}
		
		public ICustomLabelObject getHeaderLabelObject() {
			return headerLabelObject;
		}
		
		public boolean isDisplayDocumentationAsHoverText() {
			return displayDocumentationAsHoverHelp;
		}
		
		public String getHelpContextId() {
			return helpContextId;
		}
		
		public String getCustomizationSchemaLocation() {
			return customizationSchemaLocation;
		}
		
		public ICustomPreSelectedTreeObject getCustomPreSelectedTreeObject() {
			return preSelectedTreeElementObject;
		}
		
		public boolean isAddChildHelperEnabled() {
			return addChildHelperEnabled;
		}
		
		public int getAddChildHelperLimit() {
			return addChildHelperLimit;
		}
		
		public ICustomControlObject3 getEmptyElementCustomControlObject() {
			return emptyElementCustomControlObject;
		}
	
		public DetailItemCustomization getItemCustomization(String namespace, String path) {
			if(namespace == null) {
				namespace = "";
			}
			String key = getKey(namespace, path);
			DetailItemCustomization itemCustomization = (DetailItemCustomization)itemCustomizationMap.get(key);
			if(itemCustomization != null) {
				return itemCustomization;
			} else {
				Set keys = itemCustomizationMapForRegularExpresions.keySet();
				for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
					String currentKey = (String)iterator.next();
					if(currentKey.length() >= namespace.length() && namespace.equals(currentKey.substring(0, namespace.length()))) {
						String pathSegment = currentKey.substring(currentKey.indexOf('*') + 1);
						// The namespace segment appears before the path (see getKey).   For example, http://www.w3.org/2001/XMLSchema-instance*@type
						// or, for a null namespace, *@type
						String namespaceSegment = currentKey.substring(0, currentKey.indexOf('*')); 
						if(pathSegment.length() <= path.length() && pathSegment.equals(path.substring(path.length() - pathSegment.length()))
								&& namespace.equals(namespaceSegment)) {  // The namespace should also be compared, otherwise a detail item from another namespace with the same path could be returned.
							return (DetailItemCustomization) itemCustomizationMapForRegularExpresions.get(currentKey);
						}
					}
				}
			}
			return null;
		}
		
		private DetailItemCustomization getTypeCustomizationInUnion(String typeNameStr)
		{
			TypeCustomization typeCustomization = null;
			if(typeNames != null){
				Iterator i = typeNames.iterator();
				while (i.hasNext() && typeCustomization == null)
				{
					String typeName = (String)i.next();
					if(typeName.equals(typeNameStr))
					{
						typeCustomization = (TypeCustomization)typeCustomizationMap.get(typeName);
						if (!typeCustomization.isUnion())
						{
							typeCustomization = null;
						}
					}
				}
			} 
		    return typeCustomization;
		}
		
		public DetailItemCustomization getTypeCustomization(String typeNameStr, String path)
		{
			DetailItemCustomization typeCustomization = null;
			String currentNodePath = null;
			String parentNodePath = null;

			if(typeNames != null){
				Iterator i = typeNames.iterator();
				while (i.hasNext() && typeCustomization == null)
				{
					String typeName = (String)i.next();
					if(typeName.equals(typeNameStr) /*|| typeNameStr.endsWith(typeName)*/)
					{
						typeCustomization = (TypeCustomization)typeCustomizationMap.get(typeName);
						if (((TypeCustomization)typeCustomization).isUnion())
						{
							return null;
						}
					}
				}
			} 
			if(typeCustomization!= null)
				return typeCustomization;
//   Need to properly check if the parent's type is customized and if it has item/node customizations within it.
//   REF 113611, this section of code returns the parent's item customization as the type customization.			
//			else
//			{
//				String namespace = null;
//				int index  = -1;
//				if(typeNameStr!= null)
//					index = typeNameStr.lastIndexOf(":");
//				if(index != -1)
//					namespace= typeNameStr.substring(0,index);
//				if(namespace!=null)
//					typeCustomization = (DetailItemCustomization)itemCustomizationMap.get(getKey(namespace,path));
//				else
//					typeCustomization = (DetailItemCustomization)itemCustomizationMap.get(getKey("",path));
//			}
			return typeCustomization;
		}
		
		public DetailItemCustomization getTypeCustomizationConsideringUnions(CMNode cmNode, String path)
		{
			DetailItemCustomization detailItemCustomization = getTypeCustomization(ModelUtil.getTypeFromSchema(cmNode), path);
			if (detailItemCustomization == null) {
				// REF: 113981 - If it is still null, then look at Unions.  The union attribute must be set to true
				List<XSDSimpleTypeDefinition> list = ModelUtil.getMemberTypesFromUnion(cmNode);
				for (XSDSimpleTypeDefinition st : list) {
					String name = st.getName();
					String ns = st.getTargetNamespace();
					// Construct keys to look up in type customization map
					if (name != null && ns != null) {
						detailItemCustomization = getTypeCustomizationInUnion(ns + ":" + name);
					} else if (name != null && ns == null) {
						detailItemCustomization = getTypeCustomizationInUnion(name);
					}
					// Assumption: Use the first type in the memberType list that has a customization
					if (detailItemCustomization != null) {
						return detailItemCustomization;
					}
				}
			}
            return detailItemCustomization;
		}
		
		
		private String getKey(String namespace, String path) {
			return namespace + path;
		}
		
		private String getTypeName(String namespace, String name, String path) {
			//Case where there is no namespace
			if (namespace == null || namespace.isEmpty())
			{
				if(path==null || path.isEmpty())
					return name;
				else
					return name + "_" + path;
			}
			//Case where the namespace is specified
			else
				if (path ==null || path.isEmpty())
					return namespace + ":" + name;
				else
					return namespace + ":" + name + "_" + path;
		}
		
		
		boolean isParsed() {
			return isParsed;
		}
		

		void parse() {
			clearCustomizationData();
			itemCustomizationMap = new HashMap();
			typeCustomizationMap = new HashMap();
			itemCustomizationMapForRegularExpresions = new HashMap();
			Bundle bundle = Platform.getBundle(configurationElement.getContributor().getName());
			URL customizationFileURL = null;
			try {
				customizationFileURL = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationFile), null));
				parseFile(customizationFile, customizationPropertiesFile, customizationFileURL.toExternalForm(), getResourceBundle(bundle, customizationPropertiesFile), bundle, 0);
				isParsed = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			Iterator iterator = customizationAddOns.iterator();
			int i = 1;
			while(iterator.hasNext()) {
				CustomizationAddOn customizationAddOn = (CustomizationAddOn)iterator.next();
				if(customizationID.equals(customizationAddOn.getCustomizationID())) {
					bundle = Platform.getBundle(customizationAddOn.getConfigurationElement().getContributor().getName());
					try {
						customizationFileURL = FileLocator.toFileURL(FileLocator.find(bundle, new Path(customizationAddOn.getCustomizationFile()), null));
						parseFile(customizationAddOn.getCustomizationFile(), customizationAddOn.getCustomizationPropertiesFile(), customizationFileURL.toExternalForm(), getResourceBundle(bundle, customizationAddOn.getCustomizationPropertiesFile()), bundle, i++ * 10000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		
		private ResourceBundle getResourceBundle(Bundle bundle, String customizationPropertiesFile) {
			if (customizationPropertiesFile == null) {
				return null;
			}
			ResourceBundle resourceBundle = null;
			List propertiesFilePaths = new ArrayList();
			IPath path = new Path(customizationPropertiesFile);
			String propertiesFilePath = path.removeFileExtension().toString();
			String language = Locale.getDefault().getLanguage();
			String country = Locale.getDefault().getCountry();
			String variant = Locale.getDefault().getVariant();
			if(!"".equals(language)) {
				if(!"".equals(country)) {
					if(!"".equals(variant)) {
						propertiesFilePaths.add(propertiesFilePath + '_' + language + '_' + country + '_' + variant);
					}
					propertiesFilePaths.add(propertiesFilePath + '_' + language + '_' + country);
				}
				propertiesFilePaths.add(propertiesFilePath + '_' + language);
			}
			propertiesFilePaths.add(propertiesFilePath);
			Iterator iterator = propertiesFilePaths.iterator();
			URL customizationFilePropertiesURL = null;
			while(iterator.hasNext() && customizationFilePropertiesURL == null) {
				String currentPathString = (String)iterator.next();
				IPath currentPath = new Path(currentPathString).addFileExtension(path.getFileExtension()); 
				try {
					customizationFilePropertiesURL =  FileLocator.toFileURL(FileLocator.find(bundle, currentPath, null));
				} catch(Exception e) {}
			}
			if(customizationFilePropertiesURL != null) {
				try {
					InputStream bundleStream= customizationFilePropertiesURL.openStream();
					resourceBundle = new PropertyResourceBundle(bundleStream);
					bundleStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return resourceBundle;
		}

		private void clearCustomizationData() {
			headerLabel = null;
			overviewSectionTitle = null;
			globalDetectSchemaLabel = false;
			hideRepeatableItemNumbers = false;
			enableSchemaGroups = false;
			addButtonClass= null;
			globalValidationClass = null;
			globalIconClass = null;
			globalDetailSectionTitleClass = null;
			globalTreeLabelClass = null;
			overviewSectionDescription = null;
			overviewSectionDescriptionObject = null;
			headerIcon = null;
			headerActions = null;
			headerLabelObject = null;
			overviewSectionTitleObject = null;
			helpContextId = null;
			treeSortConfirmationMessage = null;
			treeUnsortConfirmationMessage = null;
			editorTitle = null;
			addChildHelperEnabled = false;
			addChildHelperLimit = 10;
		}
		
		/**
		 * Set user specified overrides from extension point.  These overrides will replace the attributes 'customizationNamespace' and 'customizationSchemaLocation' set in the 
		 * customization.xml file
		 * 
		 * @param overrideCustomizationNamespace
		 * @param overrideCustomizationSchemaLocation
		 */
		
		public void setOverrides(String overrideCustomizationNamespace, String overrideCustomizationSchemaLocation){
			this.overrideCustomizationNamespace = overrideCustomizationNamespace;
			this.overrideCustomizationSchemaLocation = overrideCustomizationSchemaLocation;
		}
		
		
		private void parseFile(String customizationFile, String customizationPropertiesFile, String fileName, ResourceBundle resourceBundle, Bundle bundle, int counterInitialization) {
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			saxParserFactory.setValidating(false);
			saxParserFactory.setNamespaceAware(true);
			SAXParser saxParser;
			XMLReader xmlReader;
			try {
				saxParser = saxParserFactory.newSAXParser();
				xmlReader = saxParser.getXMLReader();
				XMLEventHandler xmlEventHandler = new XMLEventHandler(bundle, resourceBundle, customizationFile, customizationPropertiesFile, counterInitialization);
				xmlReader.setContentHandler(xmlEventHandler);
				xmlReader.parse(fileName);
			} catch (Exception e) {
				e.printStackTrace();
				ILog log = DDEPlugin.getDefault().getLog();
				MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_CUSTOMIZATION_FILE_COULD_NOT_BE_SUCCESSFULLY_PARSED);
				String errorMessage = messageFormat.format(new String[]{customizationFile, fileName, customizationID, bundle.getSymbolicName()});
				IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage, e);
				log.log(status);
			}
		}

		private class XMLEventHandler extends DefaultHandler {
			
			private final static String NODE = "node";
			private final static String ITEM = "item";
			private final static String nodeType = "nodeType";
			private final static String itemType = "itemType";
			private final static String PATH = "path";
			private final static String LABEL = "label";
			private final static String HIDDEN = "hide";
			private final static String TRUE = "true";
			private final static String FALSE = "false";
			private final static String LINK = "labelLinkClass";
			private final static String BUTTON = "buttonClass";
			private final static String LINES = "textLines";
			private final static String NAME = "name";
			private final static String NAMESPACE = "namespace";
			private final static String ICON = "icon";
			private final static String STYLE = "style";
			private final static String STYLE_DEFAULT = "default";
			private final static String STYLE_TEXT = "text";
			private final static String STYLE_COMBO = "combo";
			private final static String STYLE_LIST = "list";
			private final static String STYLE_CHECKBOX = "checkBox";
			private final static String REQUIRED = "required";
			private final static String POSSIBLE_VALUES = "possibleValues";
			private final static String SUGGESTED_VALUES = "suggestedValues";
			private final static String VALUE = "value";
			private final static String SUGGEST = "suggest";
			private final static String TREE_NODE = "treeNode";
			private final static String BUTTON_LABEL = "buttonLabel";
			private final static String BUTTON_ICON = "buttonIcon";
			private final static String BUTTON_TOOLTIP = "buttonToolTip";
			private final static String LABEL_TOOLTIP = "labelLinkToolTip";
			private final static String TOOLTIP = "toolTip";
			private final static String DELETE_IF_EMPTY = "deleteIfEmpty";
			private final static String CUSTOMIZATION = "customization";
			private final static String CUSTOMIZATION_NAMESPACE = "customizationNamespace";
			private final static String CUSTOMIZATION_SCHEMA_LOCATION = "customizationSchemaLocation";
			private final static String READ_ONLY = "readOnly";
			private final static String TREE_LABEL = "treeLabel";
			private final static String HIDE_LABEL = "hideLabel";
			private final static String DISABLED = "disabled";
			private final static String CREATION_CLASS = "creationClass";
			private final static String SINGLE_OCCURRENCE = "singleOccurrence";
			private final static String TABLE_ICON = "tableIcon";
			private final static String CREATION_LABEL = "creationLabel";
			private final static String HIDE_SECTION_TITLE = "hideSectionTitle";
			private final static String INHERIT_CUSTOMIZATION = "inheritCustomization";
			private final static String TREE_LABEL_DATA = "treeLabelData";
			private final static String HEADER_TEXT = "headerText";
			private final static String FOOTER_TEXT = "footerText";
			private final static String SECTION_HEADER_TEXT = "sectionHeaderText";
			private final static String SECTION_HEADER_TEXT_DATA = "sectionHeaderTextData";
			private final static String SECTION_HEADER_TEXT_CLASS = "sectionHeaderTextClass";
			private final static String DETAIL_SECTION_TITLE = "detailSectionTitle";
			private final static String DETAIL_SECTION_TITLE_DATA = "detailSectionTitleData";
			private final static String CHECK_BOX_TEXT = "checkBoxText";
			private final static String TREE_LABEL_CLASS = "treeLabelClass";
			private final static String DETAIL_SECTION_TITLE_CLASS = "detailSectionTitleClass";
			private final static String DELETION_CLASS = "deletionClass";
			private final static String HEADER_LABEL = "headerLabel";
			private final static String HEADER_ICON = "headerIcon";
			private final static String HEADER_ACTIONS = "headerActions";
			private final static String HEADER_LABEL_DATA = "headerLabelData";
			private final static String HEADER_LABEL_CLASS = "headerLabelClass";
			private final static String OVERVIEW_SECTION_TITLE = "overviewSectionTitle";
			private final static String OVERVIEW_SECTION_TITLE_DATA = "overviewSectionTitleData";
			private final static String OVERVIEW_SECTION_TITLE_CLASS = "overviewSectionTitleClass";
			private final static String OVERVIEW_SECTION_HEADER_TEXT = "overviewSectionHeaderText";
			private final static String OVERVIEW_SECTION_HEADER_TEXT_DATA = "overviewSectionHeaderTextData";
			private final static String OVERVIEW_SECTION_HEADER_TEXT_CLASS = "overviewSectionHeaderTextClass";
			private final static String PRE_SELECTED_TREE_ELEMENT_CLASS = "preSelectedTreeElementClass";
			private final static String CAN_CREATE = "canCreate";
			private final static String CAN_DELETE = "canDelete";
			private final static String CAN_CREATE_CLASS = "canCreateClass";
			private final static String CAN_DELETE_CLASS = "canDeleteClass";
			private final static String POSSIBLE_VALUES_CLASS = "possibleValuesClass";
			private final static String SUGGESTED_VALUES_CLASS = "suggestedValuesClass";
			private final static String DEFAULT_VALUE = "defaultValue";
			private final static String DEFAULT_VALUE_CLASS = "defaultValueClass";
			private final static String VALIDATION_CLASS = "validationClass";
			private final static String CUSTOMCONTROL_CLASS = "customControlClass";
			private final static String DETECTSCHEMALABEL = "detectSchemaLabel";
			private final static String GLOBALDETECTSCHEMALABEL = "globalDetectSchemaLabel";
			private final static String ADDBUTTONCLASS= "addButtonClass";
			private final static String GLOBALVALIDATIONCLASS = "globalValidationClass";
			private final static String HIDE_REPEATABLE_ITEM_NUMBERS = "hideRepeatableItemNumbers";
			private final static String ENABLE_SCHEMA_GROUPS = "enableSchemaGroups";
			private final static String TRIGGER_NODE_VALIDATION_PATH = "triggerNodeValidationPath";
			private final static String TRIGGER_NODE_VALIDATION_RECURSE = "triggerNodeValidationRecurse";
			private final static String CDATA_SECTION_STORAGE = "CDATASectionStorage";
			private final static String HORIZONTAL_SCROLLING = "horizontalScrolling";
			private final static String HELP_CONTEXT_ID = "helpContextId";
			private final static String HOVER_HELP = "hoverHelp";
			private final static String DISPLAY_DOCUMENTATION_AS_HOVER_HELP = "displayDocumentationAsHoverHelp";
			private final static String SKIP_SYNTAX_VALIDATION = "skipSyntaxValidation";
			private final static String SECTION = "section";
			private final static String HYPERLINK = "hyperlink";
			private final static String HYPERLINK_CLASS = "hyperlinkClass";
			private final static String LEFT_INDENTATION = "leftIndentation";
			private final static String TREE_SORTING_PREFERENCE = "treeSortingPreference";
			private final static String DETAILS_SORTING_PREFERENCE = "detailsSortingPreference";
			private final static String SORTED = "sorted";
			private final static String UNSORTED = "unsorted";
			private final static String ALWAYS_SORTED = "alwaysSorted";
			private final static String ALWAYS_UNSORTED = "alwaysUnsorted";
			private final static String DEFAULT = "default";
			private final static String SORT_BY_SCHEMA = "sortBySchemaDefinition";
			private final static String TREE_SORT_CONFIRMATION_MESSAGE = "treeSortConfirmationMessage";
			private final static String TREE_UNSORT_CONFIRMATION_MESSAGE = "treeUnsortConfirmationMessage";
			private final static String CLEAR_OPTIONAL_SECTION_IF_EMPTY = "clearOptionalSectionIfEmpty";
			private final static String SHOW_ITEM_AS_OPTIONAL = "showItemAsOptional";
			private final static String EDITOR_TITLE = "editorTitle";
			private final static String BUTTON_ACCESSIBILITY_NAME = "buttonAccessibilityName";
			private final static String HIDE_OVERVIEW_SECTION = "hideOverviewSection";
			private final static String MULTIPLE_DELETION_CLASS = "multipleDeletionClass";
			private final static String ECHO_CHAR = "echoChar";
			private final static String WRAP_TEXT = "wrapText";
			private final static String DISABLE_CLASS = "disableClass";
			private final static String SHOULD_ITEM_DISABLE_CLASS = "shouldItemDisableClass";
			private final static String IN_UNION = "inUnion";
			private final static String ICON_CLASS = "iconClass";
			private final static String ADD_CHILD_ELEMENT_HELPER = "addChildElementHelper";
			private final static String ADD_CHILD_ELEMENT_HELPER_LIMIT = "addChildElementHelperLimit";
			private final static String EMPTY_ELEMENT_CUSTOM_CONTROL_CLASS = "emptyElementCustomControlClass";
			
			Bundle bundle;
			ResourceBundle resourceBundle;
			
			private String customizationFile;
			private String customizationPropertiesFile;
			
			private Stack currentNodeCustomization;
			private Stack customControlsListStack;
			private Stack customSectionListStack;
			private Stack pathStack;
			private Stack typeStack;
			private Stack counterStack;
			private int counter;
			private int counterInitialization;
			private List missingClassList;
			private List missingTranslationList;
			private List missingImageList;
			private DetailItemCustomization currentDetailItemCustomization;
			
			
			public XMLEventHandler(Bundle bundle, ResourceBundle resourceBundle, String customizationFile, String customizationPropertiesFile, int counterInitialization) {
				pathStack = new Stack();
				typeStack = new Stack();
				counterStack = new Stack();
				currentNodeCustomization = new Stack();
				customControlsListStack = new Stack();
				customSectionListStack = new Stack();
				this.counterInitialization = counterInitialization;
				counter = counterInitialization;
				missingClassList = new ArrayList();
				missingTranslationList = new ArrayList();
				missingImageList = new ArrayList();
				this.bundle = bundle;
				this.resourceBundle = resourceBundle;
				this.customizationFile = customizationFile;
				this.customizationPropertiesFile = customizationPropertiesFile;
			}
			


			private String getNodeFullPath() {
				String fullPath = "";
				Iterator iterator = pathStack.iterator();
				while(iterator.hasNext()) {
					String pathSegment = (String)iterator.next();
					if(pathSegment != null && pathSegment.length() > 0) {
						if(!(pathSegment.charAt(0) == '@') && fullPath.length() > 0) {
							fullPath += '/';
						}
						fullPath += pathSegment;							
					}
				}
				return fullPath;
			}
			
			public void endElement(String uri, String localName, String qName) throws SAXException {
				super.endElement(uri, localName, qName);
				if(( pathStack.size() > 0 && (NODE.equals(localName) || ITEM.equals(localName) || nodeType.equals(localName) || itemType.equals(localName)))) {
					pathStack.pop();
					if(NODE.equals(localName)) {
						counter = ((Integer)counterStack.pop()).intValue();
						Object object = currentNodeCustomization.pop();
						List customControlsList = (List)customControlsListStack.pop();
						List customSectionsList = (List)customSectionListStack.pop();
						if(object instanceof DetailItemCustomization) {
							DetailItemCustomization detailItemCustomization = (DetailItemCustomization)object;
							AbstractControl[] customControls = detailItemCustomization.getCustomControls();
							if(customControls != null && customControls.length > 0) {
								List list = Arrays.asList(customControls);
								customControlsList.addAll(0, list);
							}
							CustomSection[] customSections = detailItemCustomization.getCustomSections();
							if(customSections != null && customSections.length > 0) {
								List list = Arrays.asList(customSections);
								customSectionsList.addAll(0, list);
							}
							detailItemCustomization.setControls((AbstractControl[])customControlsList.toArray(new AbstractControl[customControlsList.size()]));
							detailItemCustomization.setCustomSections((CustomSection[])customSectionsList.toArray(new CustomSection[customSectionsList.size()]));
						}
					}
				} else if(SECTION.equals(localName)) {
					// Close last section
					List customSectionList = (List)customSectionListStack.peek();
					if(customSectionList.size() > 0) {
						CustomSection customSection = (CustomSection)customSectionList.get(customSectionList.size() - 1);
						customSection.setEnding(counter);
					}
				}
			}
			
			private String getTranslation(String value) {
				if(resourceBundle != null) {
					if(value != null && value.length() > 0 && value.charAt(0) == '%') {
						try {
							value = resourceBundle.getString(value.substring(1));
						} catch (Exception e) {
							if(missingTranslationList.indexOf(value) == -1) {
								e.printStackTrace();
								ILog log = DDEPlugin.getDefault().getLog();
								MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_MISSING_TRANSLATION);
								String errorMessage = messageFormat.format(new String[]{value.substring(1), customizationPropertiesFile, customizationFilePath, customizationID, bundle.getSymbolicName()});
								IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage);
								log.log(status);
								missingTranslationList.add(value);
							}
						}
					}
				}
				return value;
			}
			
			private Class getClass(String className) {
				Class clazz = null;
				if(className != null) {
					try {
						clazz = bundle.loadClass(className);
					} catch (ClassNotFoundException e) {
						if(missingClassList.indexOf(className) == -1) {
							e.printStackTrace();
							ILog log = DDEPlugin.getDefault().getLog();
							MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_MISSING_CUSTOMIZATION_CLASS);
							String errorMessage = messageFormat.format(new String[]{className, customizationFile, customizationFilePath, customizationID, bundle.getSymbolicName()});
							IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage, e);
							log.log(status);
							missingClassList.add(className);
						}
					}
				}
				return clazz;
			}
			
			private Image getImage(String imagePath) {
				Image image = null;
				String imageKey = customizationID + '/' + imagePath;
				URL fileURL = FileLocator.find(bundle, new Path(imagePath), null);
				if (fileURL != null) {
					try {
						image = DDEPlugin.getDefault().getImageFromRegistry(imageKey);
						if (image == null) {
							InputStream inputStream = fileURL.openStream();
							image = new Image(Display.getDefault(), inputStream);
							DDEPlugin.getDefault().getImageRegistry().put(imageKey, image);
						}
					}
					catch (IOException e) {e.printStackTrace();
					}
				} else {
					if(missingImageList.indexOf(imageKey) == -1) {
						ILog log = DDEPlugin.getDefault().getLog();
						MessageFormat messageFormat = new MessageFormat(Messages.ERROR_LOG_MESSAGE_MISSING_IMAGE);
						String errorMessage = messageFormat.format(new String[]{imagePath, customizationFile, customizationFilePath, customizationID, bundle.getSymbolicName()});
						IStatus status = new Status(Status.ERROR, DDEPlugin.getDefault().getBundle().getSymbolicName(), errorMessage);
						log.log(status);
						missingImageList.add(imagePath);
					}
				}
				return image;
			}


			public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
				if(NODE.equals(localName) || ITEM.equals(localName) || nodeType.equals(localName) || itemType.equals(localName)) {
					
					// Customization data
					String name = null;
					String namespace = null;
					String path = null;
					boolean isHidden = false;
					String label = null;
					String toolTip = null;
					String labelToolTip = null;
					int style = DetailItemCustomization.STYLE_DEFAULT;
					String buttonLabel = null;
					Image buttonIcon = null;
					String buttonToolTip = null;
					Class linkClass = null;
					Class buttonClass = null;
					Class creationClass = null;
					Class treeLabelClass = null;
					Class detailSectionTitleClass = null;
					Class deletionClass = null;
					Class sectionHeaderTextClass = null;
					Class iconClass = null;
					Image iconImage = null;
					int textLines = 1;
					boolean required = false;
					boolean readOnly = false;
					boolean deleteIfEmpty = true;
					boolean hideLabel = false;
					boolean disabled = false;
					boolean singleOccurrence = false;
					boolean hideSectionTitle = false;
					String creationLabel = null;
					Image tableIcon = null;
					String treeLabel = null;
					String headerText = null;
					String footerText = null;
					String sectionHeaderText = null;
					String detailSectionTitle = null;
					String checkBoxText = null;
					boolean canCreate = true;
					boolean canDelete = true;
					Class canCreateClass = null;
					Class canDeleteClass = null;
					Class possibleValuesClass = null;
					Class suggestedValuesClass = null;
					String defaultValue = null;
					Class defaultValueClass = null;
					Class validationClass = null;
					Class customControlClass = null;
					boolean detectSchemaLabel = false;
					String[] triggerValidationPath = new String[0];
					boolean[] triggerNodeValidationRecurse = new boolean[0];
					boolean cDataSectionStorage = false;
					boolean horizontalScrolling = false;
					String helpContextID = null;
					String hoverHelp = null;
					boolean skipSyntaxValidation = false;
					AbstractControl[] customControls = null;
					CustomSection[] customSections = null;
					boolean clearOptionalSectionIfEmpty = false;
					boolean showItemAsOptional = false;
					String buttonAccessibilityName = null;
					char echoChar = 0;
					boolean wrapText = false;
					Class disableClass = null;
					Class shouldItemDisableClass = null;
					int detailsSortingOption = -1; // -1 there is no attribute in the customization
					boolean isUnion = false;
					
					String temp = null; // Temporary String for retreiving data
					
					// Obtain name attribute
					name = attrs.getValue(NAME);
					
					// Obtain namespace
					namespace = attrs.getValue(NAMESPACE);
					if(namespace == null) {
						if (overrideCustomizationNamespace == null)
							namespace = customizationNamespace;
						else
							namespace = overrideCustomizationNamespace;
					}
					//If both name and namespace are defined then we have a type name
					if (name != null && namespace != null){
						String typeName = getTypeName(namespace, name, path);
						typeStack.add(typeName);
					}
					
					// Obtain path
					temp = attrs.getValue(PATH);
					if(temp != null && temp.length() > 0) {
						pathStack.push(temp);
					}
					path = getNodeFullPath();

					// Obtain inheritCustomization
					if(TRUE.equals(attrs.getValue(INHERIT_CUSTOMIZATION))) {
						DetailItemCustomization detailItemCustomization = getItemCustomization(namespace, path);
//						DetailItemCustomization detailItemCustomization = (DetailItemCustomization)itemCustomizationMap.get(getKey(namespace, path));
//						if(detailItemCustomization == null) {
//							String keyForRegularExpressions = "";
//							if(path.indexOf('@') != -1) {
//								keyForRegularExpressions = getKey(namespace, "*" + path.substring(path.lastIndexOf('@')));
//							} else {
//								keyForRegularExpressions = getKey(namespace, "*" + path.substring(path.lastIndexOf('/')));
//							}
//							detailItemCustomization = (DetailItemCustomization)itemCustomizationMapForRegularExpresions.get(keyForRegularExpressions);
//						}					
						// If there is inheritance, popullate values
						if(detailItemCustomization != null) {
							isHidden = detailItemCustomization.isHidden();
							label = detailItemCustomization.getLabel();
							toolTip = detailItemCustomization.getToolTip();
							labelToolTip = detailItemCustomization.getLabelLinkToolTip();
							style = detailItemCustomization.getStyle();
							buttonLabel = detailItemCustomization.getButtonLabel();
							buttonIcon = detailItemCustomization.getButtonIcon();
							buttonToolTip = detailItemCustomization.getButtonToolTip();
							linkClass = detailItemCustomization.getLinkClass();
							buttonClass = detailItemCustomization.getButtonClass();
							creationClass = detailItemCustomization.getCreationClass();
							iconImage = detailItemCustomization.getIcon();
							textLines = detailItemCustomization.getLines();
							required = detailItemCustomization.isRequired();
							readOnly = detailItemCustomization.isReadOnly();
							deleteIfEmpty = detailItemCustomization.isDeleteIfEmpty();
							hideLabel = detailItemCustomization.isHideLabel();
							disabled = detailItemCustomization.isDisabled();
							singleOccurrence = detailItemCustomization.isSingleOccurrence();
							hideSectionTitle = detailItemCustomization.isHideSectionTitle();
							creationLabel = detailItemCustomization.getCreationLabel();
							tableIcon = detailItemCustomization.getTableIcon();
							treeLabel = detailItemCustomization.getTreeLabel();
							headerText = detailItemCustomization.getHeaderText();
							footerText = detailItemCustomization.getFooterText();
							sectionHeaderText = detailItemCustomization.getSectionHeaderText();
							sectionHeaderTextClass = detailItemCustomization.getSectionHeaderTextClass();
							detailSectionTitle = detailItemCustomization.getDetailSectionTitle();
							checkBoxText = detailItemCustomization.getCheckBoxText();
							treeLabelClass = detailItemCustomization.getTreeLabelClass();
							detailSectionTitleClass = detailItemCustomization.getDetailSectionTitleClass();
							iconClass = detailItemCustomization.getIconClass();
							deletionClass = detailItemCustomization.getDeletionClass();
							canCreate = detailItemCustomization.isCanCreate();
							canDelete = detailItemCustomization.isCanDelete();
							canCreateClass = detailItemCustomization.getCanCreateClass();
							canDeleteClass = detailItemCustomization.getCanDeleteClass();
							possibleValuesClass = detailItemCustomization.getPossibleValuesClass();
							suggestedValuesClass = detailItemCustomization.getSuggestedValuesClass();
							validationClass = detailItemCustomization.getValidationClass();
							customControlClass = detailItemCustomization.getCustomControlClass();
							detectSchemaLabel = detailItemCustomization.getDetectSchemaLabel();
							triggerValidationPath = detailItemCustomization.getTriggerValidationPath();
							triggerNodeValidationRecurse = detailItemCustomization.isTrigerNodeValidationRecurse();
							cDataSectionStorage = detailItemCustomization.isCDATASectionStorage();
							horizontalScrolling = detailItemCustomization.isHorizontalScrolling();
							helpContextID = detailItemCustomization.getHelpContextId();
							hoverHelp = detailItemCustomization.getHoverHelp();
							skipSyntaxValidation = detailItemCustomization.isSkipSyntaxValidation();
							customControls = detailItemCustomization.getCustomControls();
							customSections = detailItemCustomization.getCustomSections();
							clearOptionalSectionIfEmpty = detailItemCustomization.isClearOptionalSectionIfEmpty();
							showItemAsOptional = detailItemCustomization.isShowItemAsOptional();
							buttonAccessibilityName = detailItemCustomization.getButtonAccessibilityName();
							echoChar = detailItemCustomization.getEchoChar();
							wrapText = detailItemCustomization.isWrapText();
							disableClass = detailItemCustomization.getDisableClass();
							shouldItemDisableClass = detailItemCustomization.getShouldItemDisableClass();
							detailsSortingOption = detailItemCustomization.getDetailsSortingOption();
						}
					}
					
					// Obtain isHidden
					temp = attrs.getValue(HIDDEN);
					if(TRUE.equals(temp)) {
						isHidden = true;
					} else if(FALSE.equals(temp)) {
						isHidden = false;
					}

					// Obtain label
					temp = attrs.getValue(LABEL);
					if(temp != null) {
						label = getTranslation(temp);
					}
					
					// Obtain style
					if(attrs.getValue(STYLE) != null) {
						temp = attrs.getValue(STYLE);
						if(STYLE_DEFAULT.equals(temp)) {
							style = DetailItemCustomization.STYLE_DEFAULT;
						} else if(STYLE_TEXT.equals(temp)) {
							style = DetailItemCustomization.STYLE_TEXT;
						} else if(STYLE_COMBO.equals(temp)) {
							style = DetailItemCustomization.STYLE_COMBO;
						} else if(STYLE_LIST.equals(temp)) {
							style = DetailItemCustomization.STYLE_LIST;
						} else if(STYLE_CHECKBOX.equals(temp)) {
							style = DetailItemCustomization.STYLE_CHECKBOX;
						} else if(TREE_NODE.equals(temp)) {
							style = DetailItemCustomization.STYLE_TREE_NODE;
						}
					}
					
					// Obtain singleOccurrence
					temp = attrs.getValue(SINGLE_OCCURRENCE);
					if(TRUE.equals(temp)) {
						singleOccurrence = true;
					} else if(FALSE.equals(temp)) {
						singleOccurrence = false;
					}
					// Obtain customControlClass
					temp = attrs.getValue(CUSTOMCONTROL_CLASS);
					if(temp != null) {
						customControlClass = getClass(temp);
					}
					
					// Obtain detectSchemaLabel
					temp = attrs.getValue(DETECTSCHEMALABEL);
					if(TRUE.equals(temp)) {
						detectSchemaLabel = true;
					} else{
						detectSchemaLabel = false;
					}
					

					// Obtain validationClass if it is defined on the item
					temp = attrs.getValue(VALIDATION_CLASS);
					if(temp != null) {
						validationClass = getClass(temp);
					}
					//if no validationClass is defined for the item, check to see if the class is defined globally to be applied on all items
					if(validationClass == null && globalValidationClass !=null)
					{
						validationClass = getClass(globalValidationClass);
					}
					
					// Obtain TriggerValidationClass
					temp = attrs.getValue(TRIGGER_NODE_VALIDATION_PATH);
					if(temp != null) {
						StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
						triggerValidationPath = new String[stringTokenizer.countTokens()];
						int i = 0;
						while(stringTokenizer.hasMoreTokens()) {
							triggerValidationPath[i++] = stringTokenizer.nextToken().trim();
						}
					}
					
					// Obtain TriggerValidationRecurse
					temp = attrs.getValue(TRIGGER_NODE_VALIDATION_RECURSE);
					if(temp != null) {
						StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
						triggerNodeValidationRecurse = new boolean[stringTokenizer.countTokens()];
						int i = 0;
						while(stringTokenizer.hasMoreTokens()) {
							triggerNodeValidationRecurse[i++] = TRUE.equals(stringTokenizer.nextToken().trim())? true : false;
						}
					}
					
					// Obtain helpContextId
					temp = attrs.getValue(HELP_CONTEXT_ID);
					if(temp != null) {
						helpContextID = temp;
					}
					
					// If the element is an "item" or the type is a simple type, obtain the item specific information
					if(ITEM.equals(localName) || itemType.equals(localName)) {
					
						// Obtain toolTip
						temp = attrs.getValue(TOOLTIP);
						if(temp != null) {
							toolTip = getTranslation(temp);
						}
						
						// Obtain headerText
						temp = attrs.getValue(HEADER_TEXT);
						if(temp != null) {
							headerText = getTranslation(temp);
						}
						
						// Obtain footerText
						temp = attrs.getValue(FOOTER_TEXT);
						if(temp != null) {
							footerText = getTranslation(temp);
						}
						
						// Obtain checkBoxText
						temp = attrs.getValue(CHECK_BOX_TEXT);
						if(temp != null) {
							checkBoxText = getTranslation(temp);
						}

						// Obtain label toolTip
						temp = attrs.getValue(LABEL_TOOLTIP);
						if(temp != null) {
							labelToolTip = getTranslation(temp);
						}
						
						// Obtain button label
						temp = attrs.getValue(BUTTON_LABEL); 
						if(temp != null) {
							buttonLabel = getTranslation(temp);
						}
						
						// Obtain button image
						temp = attrs.getValue(BUTTON_ICON); 
						if(temp != null) {
							buttonIcon = getImage(temp);
						}
						
						// Obtain button accessibility name
						temp = attrs.getValue(BUTTON_ACCESSIBILITY_NAME); 
						if(temp != null) {
							buttonAccessibilityName = getTranslation(temp);
						}
						
						// Obtain button toolTip
						temp = attrs.getValue(BUTTON_TOOLTIP);
						if(temp != null) {
							buttonToolTip = getTranslation(temp);
						}
						
						// Obtain defaultValue
						temp = attrs.getValue(DEFAULT_VALUE);
						if(temp != null) {
							defaultValue = getTranslation(temp);
						}
						
						// Obtain CDATASectionStorage
						temp = attrs.getValue(CDATA_SECTION_STORAGE);
						if(TRUE.equals(temp)) {
							cDataSectionStorage = true;
						}
						
						// Obtain horizontalScrolling
						temp = attrs.getValue(HORIZONTAL_SCROLLING);
						if(TRUE.equals(temp)) {
							horizontalScrolling = true;
						}
						
						// Obtain wrapText
						temp = attrs.getValue(WRAP_TEXT);
						if(TRUE.equals(temp)) {
							wrapText = true;
						}
						
						// Obtain possibleValuesClass
						temp = attrs.getValue(POSSIBLE_VALUES_CLASS);
						if(temp != null) {
							possibleValuesClass = getClass(temp);
						}
						
						// Obtain suggestedValuesClass
						temp = attrs.getValue(SUGGESTED_VALUES_CLASS);
						if(temp != null) {
							suggestedValuesClass = getClass(temp);
						}
						
						// Obtain defaultValueClass
						temp = attrs.getValue(DEFAULT_VALUE_CLASS);
						if(temp != null) {
							defaultValueClass = getClass(temp);
						}
						
						// Obtain link class
						temp = attrs.getValue(LINK);
						if(temp != null) {
							linkClass = getClass(temp);
						}
						
						// Obtain button class
						temp = attrs.getValue(BUTTON);
						if(temp != null) {
							buttonClass = getClass(temp);
						}
						
						// Obtain textLines
						temp = attrs.getValue(LINES);
						if(temp != null) {
							textLines = Integer.valueOf(temp).intValue();
						}
						
						// Obtain tableIcon
						temp = attrs.getValue(TABLE_ICON);
						if(temp != null) {
							tableIcon = getImage(temp);
						}
						
						// Obtain required
						temp = attrs.getValue(REQUIRED);
						if(TRUE.equals(temp)) {
							required = true;
						} else if(FALSE.equals(temp)) {
							required = false;
						}
						
						// Obtain readOnly
						temp = attrs.getValue(READ_ONLY);
						if(TRUE.equals(temp)) {
							readOnly = true;
						} else if(FALSE.equals(temp)) {
							readOnly = false;
						}
						
						// Obtain deleteIfEmpty
						temp = attrs.getValue(DELETE_IF_EMPTY);
						if(TRUE.equals(temp)) {
							deleteIfEmpty = true;
						} else if(FALSE.equals(temp)) {
							deleteIfEmpty = false;
						}
						
						// Obtain hideLabel
						temp = attrs.getValue(HIDE_LABEL);
						if(TRUE.equals(temp)) {
							hideLabel = true;
						} else if(FALSE.equals(temp)) {
							hideLabel = false;
						}
						
						// Obtain disabled
						temp = attrs.getValue(DISABLED);
						if(TRUE.equals(temp)) {
							disabled = true;
						} else if(FALSE.equals(temp)) {
							disabled = false;
						}
						
						// Obtain hoverHelp
						temp = attrs.getValue(HOVER_HELP);
						if(temp != null) {
							hoverHelp = getTranslation(temp);
						}
						
						// Obtain skipSyntaxValidation
						temp = attrs.getValue(SKIP_SYNTAX_VALIDATION);
						if(temp != null) {
							if(TRUE.equals(temp)) {
								skipSyntaxValidation = true;
							}
						}
						
						// Obtain clear optional section if empty
						temp = attrs.getValue(CLEAR_OPTIONAL_SECTION_IF_EMPTY);
						if(temp != null) {
							if(TRUE.equals(temp)) {
								clearOptionalSectionIfEmpty = true;
							}
						}
						
						// Obtain show item as optional
						temp = attrs.getValue(SHOW_ITEM_AS_OPTIONAL);
						if(temp != null) {
							if(TRUE.equals(temp)) {
								showItemAsOptional = true;
							}
						}
						
						// Obtain echoChar
						temp = attrs.getValue(ECHO_CHAR);
						if(temp != null) {
							String echoCharString = getTranslation(temp);
							if(echoCharString != null && echoCharString.length() == 1) {
								echoChar = echoCharString.charAt(0);
							}
						}
						
						// Obtain disableClass
						temp = attrs.getValue(DISABLE_CLASS);
						if(temp != null) {
							disableClass = getClass(temp);
						}
						
						// Obtain shouldItemDisableClass
						temp = attrs.getValue(SHOULD_ITEM_DISABLE_CLASS);
						if(temp != null) {
							shouldItemDisableClass = getClass(temp);
						}
						
						
						// Obtain union
						temp = attrs.getValue(IN_UNION);
						if (temp != null) {
							if(TRUE.equals(temp)) {
								isUnion = true;
							}
						}
						
					} else { 
					/* If the element is not an "item" then it must be a node or nodeType, obtain the
					   node specific information */
						
						// Obtain canCreate
						temp = attrs.getValue(CAN_CREATE);
						if(FALSE.equals(temp)) {
							canCreate = false;
						}

						// Obtain canDelete
						temp = attrs.getValue(CAN_DELETE);
						if(FALSE.equals(temp)) {
							canDelete = false;
						}
						
						// Obtain canCreateClass
						temp = attrs.getValue(CAN_CREATE_CLASS);
						if(temp != null) {
							canCreateClass = getClass(temp);
						}
						
						// Obtain canDeleteClass
						temp = attrs.getValue(CAN_DELETE_CLASS);
						if(temp != null) {
							canDeleteClass = getClass(temp);
						}
						
						// Obtain tree label
						temp = attrs.getValue(TREE_LABEL);
						if(temp != null) {
							treeLabel = getTranslation(temp);
							
							// Check if there are paths in the treeLabelData
							temp = attrs.getValue(TREE_LABEL_DATA);
							if(temp != null) {
								StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
								String paths[] = new String[stringTokenizer.countTokens()];
								int i = 0;
								while(stringTokenizer.hasMoreTokens()) {
									paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
								}
								MessageFormat messageFormat = new MessageFormat(treeLabel);
								treeLabel = messageFormat.format(paths);
							}
						}
						
						// Obtain detailSectionTitle
						temp = attrs.getValue(DETAIL_SECTION_TITLE);
						if(temp != null) {
							detailSectionTitle = getTranslation(temp);
							
							// Check if there are paths in the detailSectionTitleData
							temp = attrs.getValue(DETAIL_SECTION_TITLE_DATA);
							if(temp != null) {
								StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
								String paths[] = new String[stringTokenizer.countTokens()];
								int i = 0;
								while(stringTokenizer.hasMoreTokens()) {
									paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
								}
								MessageFormat messageFormat = new MessageFormat(detailSectionTitle);
								detailSectionTitle = messageFormat.format(paths);
							}
						}
						
						// Obtain creation label
						temp = attrs.getValue(CREATION_LABEL);
						if(temp != null) {
							creationLabel = getTranslation(temp);
						}
						
						// Obtain hideSectionTitle
						temp = attrs.getValue(HIDE_SECTION_TITLE);
						if(TRUE.equals(temp)) {
							hideSectionTitle = true;
						} else if(FALSE.equals(temp)) {
							hideSectionTitle = false;
						}
						
						// Obtain sectionHeaderText class
						temp = attrs.getValue(SECTION_HEADER_TEXT_CLASS);
						if(temp != null) {
							sectionHeaderTextClass = getClass(temp);
						}
						
						// Obtain sectionHeaderText
						temp = attrs.getValue(SECTION_HEADER_TEXT);
						if(temp != null) {
							sectionHeaderText = getTranslation(temp);

							// Check if there are paths in the sectionHeaderTextData
							temp = attrs.getValue(SECTION_HEADER_TEXT_DATA);
							if(temp != null) {
								StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
								String paths[] = new String[stringTokenizer.countTokens()];
								int i = 0;
								while(stringTokenizer.hasMoreTokens()) {
									paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
								}
								MessageFormat messageFormat = new MessageFormat(treeLabel);
								sectionHeaderText = messageFormat.format(paths);
							}
						}
						
						// Obtain creation class
						temp = attrs.getValue(CREATION_CLASS);
						if(temp != null) {
							creationClass = getClass(temp);
						}
						
						// Obtain deletion class
						temp = attrs.getValue(DELETION_CLASS);
						if(temp != null) {
							deletionClass = getClass(temp);
						}
						
						// Obtain treeLabel class
						temp = attrs.getValue(TREE_LABEL_CLASS);
						if(temp != null) {
							treeLabelClass = getClass(temp);
						}
						
						if (treeLabelClass==null && globalTreeLabelClass!=null) {
							treeLabelClass = globalTreeLabelClass;
						}

						// Obtain detailSectionTitle class
						temp = attrs.getValue(DETAIL_SECTION_TITLE_CLASS);
						if(temp != null) {
							detailSectionTitleClass = getClass(temp);
						}					
						if (detailSectionTitleClass==null && globalDetailSectionTitleClass!=null) {
							detailSectionTitleClass = globalDetailSectionTitleClass;
						}
						
						// Obtain icon class
						temp = attrs.getValue(ICON_CLASS);
						if(temp != null) {
							iconClass = getClass(temp);							
						}
						
						if (iconClass==null && globalIconClass!=null)
						{
							iconClass = globalIconClass;
						}

						// Obtain icon
						temp = attrs.getValue(ICON);
						if(temp != null) {
							iconImage = getImage(temp);
						}
						
						// detailsSortingOption
						temp = attrs.getValue(DETAILS_SORTING_PREFERENCE);
						if (temp != null) {
							if (temp.equals(SORT_BY_SCHEMA))
							   detailsSortingOption = DETAILS_SORTING_PREFERENCE_SCHEMA;
							else
							   detailsSortingOption = DETAILS_SORTING_PREFERENCE_DEFAULT;
						}
					}

					
					DetailItemCustomization itemCustomization = null;
					TypeCustomization typeCustomization = null;
					String typeName = null;
					if( name !=null && namespace != null)
					{
						typeName = getTypeName(namespace, name, path);
					}
					
					if(!"*".equals(path) && typeName== null) {
						itemCustomization = new DetailItemCustomization(label, isHidden, buttonLabel, toolTip, labelToolTip, buttonToolTip, buttonClass, linkClass, textLines, iconImage, style, required, readOnly, deleteIfEmpty, treeLabel, hideLabel, disabled, singleOccurrence, creationClass, null, creationLabel, tableIcon, hideSectionTitle, headerText, footerText, sectionHeaderText, sectionHeaderTextClass, detailSectionTitle, checkBoxText, treeLabelClass, detailSectionTitleClass, deletionClass, canCreate, canDelete, canCreateClass, canDeleteClass, possibleValuesClass, suggestedValuesClass, defaultValue, defaultValueClass, validationClass, customControlClass, detectSchemaLabel, triggerValidationPath, triggerNodeValidationRecurse, cDataSectionStorage, horizontalScrolling, helpContextID, hoverHelp, Customization.this, skipSyntaxValidation, customControls, customSections, clearOptionalSectionIfEmpty, showItemAsOptional, buttonAccessibilityName, echoChar, wrapText, disableClass, shouldItemDisableClass, detailsSortingOption, iconClass, buttonIcon);
						if(path.indexOf('*') != -1) {
							// For the items that are globally defined, store their order only
							// if they elements/attributes children of globally defined elements
							boolean storeItemOrder = false;
							int index = path.indexOf('/');
							if(index != -1) {
								storeItemOrder = path.substring(index + 1).indexOf('/') != -1 || path.substring(index + 1).indexOf('@') != -1;
							}
							if(storeItemOrder) {
								itemCustomization.setOrder(counter++);
							} else {
								itemCustomization.setOrder(-1);
							}
							if(itemCustomization != null)
								itemCustomizationMapForRegularExpresions.put(getKey(namespace, path), itemCustomization);
						} else {
							itemCustomization.setOrder(counter++);
							itemCustomizationMap.put(getKey(namespace, path), itemCustomization);
						}
					}
					//If the type is defined then we add it to the map
					if(nodeType.equals(localName) || itemType.equals(localName))
					{ 
						if( typeName != null)
						{
							int i = typeName.indexOf("_");
							String tempName = typeName;
							if(i != -1)
								tempName = typeName.substring(0, typeName.indexOf("_"));
							typeCustomization = new TypeCustomization(tempName, label, isHidden, buttonLabel, toolTip, labelToolTip, buttonToolTip, buttonClass, linkClass, textLines, iconImage, style, required, readOnly, deleteIfEmpty, treeLabel, hideLabel, disabled, singleOccurrence, creationClass, null, creationLabel, tableIcon, hideSectionTitle, headerText, footerText, sectionHeaderText, sectionHeaderTextClass, detailSectionTitle, checkBoxText, treeLabelClass, detailSectionTitleClass, deletionClass, canCreate, canDelete, canCreateClass, canDeleteClass, possibleValuesClass, suggestedValuesClass, defaultValue, defaultValueClass, validationClass, customControlClass, detectSchemaLabel, triggerValidationPath, triggerNodeValidationRecurse, cDataSectionStorage, horizontalScrolling, helpContextID, hoverHelp, Customization.this, skipSyntaxValidation, customControls, customSections, clearOptionalSectionIfEmpty, showItemAsOptional, buttonAccessibilityName, echoChar, wrapText, disableClass, shouldItemDisableClass, detailsSortingOption, isUnion, iconClass, buttonIcon);
							//String typePathPair = typeName+"_"+ getNodeFullPath();
							typeNames.add(typeName);
							typeCustomizationMap.put(typeName, typeCustomization);
						}
					}
										
					if(NODE.equals(localName)) {
						customControlsListStack.push(new ArrayList());
						customSectionListStack.push(new ArrayList());
						currentNodeCustomization.push(itemCustomization);
						counterStack.push(new Integer(counter));
						counter = counterInitialization;
					}
					if(itemCustomization != null)
						currentDetailItemCustomization = itemCustomization;
					else
						currentDetailItemCustomization = typeCustomization;
					
				} else if(HYPERLINK.equals(localName)) {
					String temp = null;
					String label = null;
					String tooltip = null;
					Image icon = null;
					Class hyperlinkClass = null;
					boolean leftIndentation = false;
					
					// Obtain label
					temp = attrs.getValue(LABEL);
					if(temp != null) {
						label = getTranslation(temp);
					}
					
					// Obtain tooltip
					temp = attrs.getValue(TOOLTIP);
					if(temp != null) {
						tooltip = getTranslation(temp);
					}
					
					// Obtain hyperlink class
					temp = attrs.getValue(HYPERLINK_CLASS);
					if(temp != null) {
						hyperlinkClass = getClass(temp);
					}
					
					// Obtain icon
					temp = attrs.getValue(ICON);
					if(temp != null) {
						icon = getImage(temp);
					}
					
					if(TRUE.equals(attrs.getValue(LEFT_INDENTATION))) {
						leftIndentation = true;
					}

					HyperLink hyperlink = new HyperLink(counter++, label, icon, tooltip, hyperlinkClass, leftIndentation);
					List customControlsList = (List)customControlsListStack.peek();
					customControlsList.add(hyperlink);

				} else if(SECTION.equals(localName)) {
					String label = getTranslation(attrs.getValue(LABEL));
					String headerText = getTranslation(attrs.getValue(HEADER_TEXT));
					CustomSection customSection = new CustomSection(counter, label, headerText);
					List customSectionList = (List)customSectionListStack.peek();
					customSectionList.add(customSection);

				} else if(POSSIBLE_VALUES.equals(localName)) {
					if(currentDetailItemCustomization != null) {
						currentDetailItemCustomization.setPossibleValues(new LinkedHashMap());
					}
				} else if(SUGGESTED_VALUES.equals(localName)) {
					if(currentDetailItemCustomization != null) {
						currentDetailItemCustomization.setSuggestedValues(new LinkedList());
					}
				} else if(VALUE.equals(localName)) {
					if(currentDetailItemCustomization != null) {
						Map possibleValues = currentDetailItemCustomization.getPossibleValues();
						String value = getTranslation(attrs.getValue(VALUE));
						String label = getTranslation(attrs.getValue(LABEL));
						if(value != null && label != null) {
							possibleValues.put(label, value);
						}
					}
				} else if(SUGGEST.equals(localName)) {
					if(currentDetailItemCustomization != null) {
						List suggestedValues = currentDetailItemCustomization.getSuggestedValues();
						String suggest = getTranslation(attrs.getValue(VALUE));
						if(suggest != null) {
							suggestedValues.add(suggest);
						}
					}
				} else if(CUSTOMIZATION.equals(localName)) {
					if (overrideCustomizationNamespace != null && overrideCustomizationSchemaLocation != null){
						customizationNamespace = overrideCustomizationNamespace;
						customizationSchemaLocation = overrideCustomizationSchemaLocation;
					}
					else{
						customizationNamespace = attrs.getValue(CUSTOMIZATION_NAMESPACE);
						customizationSchemaLocation = attrs.getValue(CUSTOMIZATION_SCHEMA_LOCATION);
					}
					
					// Obtain helpContextId
					helpContextId = attrs.getValue(HELP_CONTEXT_ID);

					// Obtain overviewSectionTitle
					overviewSectionTitle = getTranslation(attrs.getValue(OVERVIEW_SECTION_TITLE));
					
					// Obtain globalDetectSchemaLabel
					String flag = attrs.getValue(GLOBALDETECTSCHEMALABEL);
					if(TRUE.equals(flag)) {
						globalDetectSchemaLabel = true;
					} else{
						globalDetectSchemaLabel = false;
					}
					
					//Obtain globalValidationClass
					globalValidationClass = attrs.getValue(GLOBALVALIDATIONCLASS);				
					
					//obtain hide repeatable item numbers
					if (TRUE.equals(attrs.getValue(HIDE_REPEATABLE_ITEM_NUMBERS)))
						hideRepeatableItemNumbers = true;
					else
						hideRepeatableItemNumbers = false;
									
					//obtain if schema groups parsing enabled
					if (TRUE.equals(attrs.getValue(ENABLE_SCHEMA_GROUPS)))
						enableSchemaGroups = true;
					else
						enableSchemaGroups = false;
					
					//Obtain detail section title class globally defined
					String glDetailSecionTitleClassName = attrs.getValue(DETAIL_SECTION_TITLE_CLASS);
					if(glDetailSecionTitleClassName != null)
					{
						globalDetailSectionTitleClass = getClass(glDetailSecionTitleClassName);
					}
					
					//Obtain tree label class globally defined
					String globalTreeLabelClassName = attrs.getValue(TREE_LABEL_CLASS);
					if(globalTreeLabelClassName != null)
					{
						globalTreeLabelClass = getClass(globalTreeLabelClassName);
					}
					
					//obtain addButtonClass
					String addButtonClassName = null;
					addButtonClassName = attrs.getValue(ADDBUTTONCLASS);
					if(addButtonClassName != null)
					{
						addButtonClass = getClass(addButtonClassName);
					}
					
					//obtain global icon class
					String globalIconClassName = attrs.getValue(ICON_CLASS);
					if(globalIconClassName != null)
					{
						globalIconClass = getClass(globalIconClassName);
					}
										
					// Obtain hideOverviewSection
					hideOverviewSection = TRUE.equals(attrs.getValue(HIDE_OVERVIEW_SECTION));

					// Check if there are paths in the overviewSectionTitleData
					String temp = attrs.getValue(OVERVIEW_SECTION_TITLE_DATA);
					if(temp != null) {
						StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
						String paths[] = new String[stringTokenizer.countTokens()];
						int i = 0;
						while(stringTokenizer.hasMoreTokens()) {
							paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
						}
						MessageFormat messageFormat = new MessageFormat(overviewSectionTitle);
						overviewSectionTitle = messageFormat.format(paths);
					}
					
					//Obtain overviewSectionTitleClass
					overviewSectionTitleObject = null;
					temp = attrs.getValue(OVERVIEW_SECTION_TITLE_CLASS);
					if(temp != null) {
						Class headerLabelClass = getClass(temp);
						if(headerLabelClass != null) {
						try {
							Object object = headerLabelClass.newInstance();
							if(object instanceof ICustomLabelObject) {
								overviewSectionTitleObject = (ICustomLabelObject)object;
							}
						} catch (IllegalAccessException e) {e.printStackTrace();}
						catch (InstantiationException e) {e.printStackTrace();}
						}
					}
					
					// Obtain overviewSectionHeaderText
					overviewSectionDescription = getTranslation(attrs.getValue(OVERVIEW_SECTION_HEADER_TEXT));
					
					// Obtain editorTitle
					editorTitle = getTranslation(attrs.getValue(EDITOR_TITLE));
					
					if(TRUE.equals(attrs.getValue(ADD_CHILD_ELEMENT_HELPER))) {
						addChildHelperEnabled = true;
						String addChildElementHelperLimit = attrs.getValue(ADD_CHILD_ELEMENT_HELPER_LIMIT);
						if(addChildElementHelperLimit != null && addChildElementHelperLimit.length() > 0) {
							try {
								addChildHelperLimit = Integer.parseInt(addChildElementHelperLimit);
							} catch(NumberFormatException numberFormatException) {
								numberFormatException.printStackTrace();
							}
						}
					}
					
					// Check if there are paths in the overviewSectionHeaderTextData
					temp = attrs.getValue(OVERVIEW_SECTION_HEADER_TEXT_DATA);
					if(temp != null) {
						StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
						String paths[] = new String[stringTokenizer.countTokens()];
						int i = 0;
						while(stringTokenizer.hasMoreTokens()) {
							paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
						}
						MessageFormat messageFormat = new MessageFormat(overviewSectionDescription);
						overviewSectionDescription = messageFormat.format(paths);
					}
					
					// Obtain multipleDeletionObject
					multipleDeletionObject = null;
					temp = attrs.getValue(MULTIPLE_DELETION_CLASS);
					if(temp != null) {
						Class multipleDeletionClass = getClass(temp);
						if(multipleDeletionClass != null) {
							try {
								Object object = multipleDeletionClass.newInstance();
								if(object instanceof ICustomMultipleDeletionObject) {
									multipleDeletionObject = (ICustomMultipleDeletionObject)object;
								}
							} catch (IllegalAccessException e) {e.printStackTrace();}
							catch (InstantiationException e) {e.printStackTrace();}
						}
					}
					
					// Obtain overviewSectionHeaderTextClass
					overviewSectionDescriptionObject = null;
					temp = attrs.getValue(OVERVIEW_SECTION_HEADER_TEXT_CLASS);
					if(temp != null) {
						Class headerLabelClass = getClass(temp);
						if(headerLabelClass != null) {
							try {
								Object object = headerLabelClass.newInstance();
								if(object instanceof ICustomLabelObject) {
									overviewSectionDescriptionObject = (ICustomLabelObject)object;
								}
							} catch (IllegalAccessException e) {e.printStackTrace();}
							catch (InstantiationException e) {e.printStackTrace();}
						}
					}
					
					// Obtain preSelectedTreeElementClass
					//preSelectedTreeElementObject = null;
					temp = attrs.getValue(PRE_SELECTED_TREE_ELEMENT_CLASS);
					if(temp != null) {
						Class preSelectedTreeElementClass = getClass(temp);
						if(preSelectedTreeElementClass != null) {
							try {
								Object object = preSelectedTreeElementClass.newInstance();
								if(object instanceof ICustomPreSelectedTreeObject) {
									preSelectedTreeElementObject = (ICustomPreSelectedTreeObject)object;
								}
							} catch (IllegalAccessException e) {e.printStackTrace();}
							catch (InstantiationException e) {e.printStackTrace();}
						}
					}

					// Obtain header label
					headerLabel = getTranslation(attrs.getValue(HEADER_LABEL));
					
					// Check if there are paths in the headerLabelData
					temp = attrs.getValue(HEADER_LABEL_DATA);
					if(temp != null) {
						StringTokenizer stringTokenizer = new StringTokenizer(temp,",");
						String paths[] = new String[stringTokenizer.countTokens()];
						int i = 0;
						while(stringTokenizer.hasMoreTokens()) {
							paths[i++] = "$" + stringTokenizer.nextToken().trim() + "$";
						}
						MessageFormat messageFormat = new MessageFormat(headerLabel);
						headerLabel = messageFormat.format(paths);
					}
					
					//Obtain headerLabelClass
					headerLabelObject = null;
					temp = attrs.getValue(HEADER_LABEL_CLASS);
					if(temp != null) {
						Class headerLabelClass = getClass(temp);
						if(headerLabelClass != null) {
							try {
								Object object = headerLabelClass.newInstance();
								if(object instanceof ICustomLabelObject) {
									headerLabelObject = (ICustomLabelObject)object;
								}
							} catch (IllegalAccessException e) {e.printStackTrace();}
							catch (InstantiationException e) {e.printStackTrace();}
						}
					}

					// Obtain header icon
					temp = attrs.getValue(HEADER_ICON);
					if(temp != null) {
						String iconPath = temp;
						URL fileURL = FileLocator.find(bundle, new Path(iconPath), null);
						if (fileURL != null) {
							try {
								headerIcon = DDEPlugin.getDefault().getImageFromRegistry(iconPath);
								if (headerIcon == null) {
									InputStream inputStream = fileURL.openStream();
									headerIcon = new Image(Display.getDefault(), inputStream);
									DDEPlugin.getDefault().getImageRegistry().put(iconPath, headerIcon);
								}
							}
							catch (IOException e) {e.printStackTrace();}
						}
					}
					
					// Obtain header actions
					temp = attrs.getValue(HEADER_ACTIONS);
					if(temp != null) {
						List actions = new ArrayList();
						StringTokenizer stringTokenizer = new StringTokenizer(temp, ",");
						while(stringTokenizer.hasMoreTokens()) {
							String token = stringTokenizer.nextToken().trim();

							Class actionClass = getClass(token);
							if(actionClass != null) {
								try {
									Object object = actionClass.newInstance();
									if(object instanceof Action) {
										actions.add(object);
									}
								} catch (IllegalAccessException e) {e.printStackTrace();
								} catch (InstantiationException e) {e.printStackTrace();
								}
							}


						}
						headerActions = (Action[]) actions.toArray(new Action[actions.size()]);
					}
					
					// Obtain showDocumentationAsHoverHelp
					temp = attrs.getValue(DISPLAY_DOCUMENTATION_AS_HOVER_HELP);
					if(FALSE.equals(temp)) {
						displayDocumentationAsHoverHelp = false;
					} else {
						displayDocumentationAsHoverHelp = true;
					}
					
					// Obtain tree sorting preference
					temp = attrs.getValue(TREE_SORTING_PREFERENCE);
					if(temp != null) {
						if(SORTED.equals(temp)) {
							treeSortingPreference = TREE_SORTING_PREFERENCE_SORTED;
						} else if(UNSORTED.equals(temp)) {
							treeSortingPreference = TREE_SORTING_PREFERENCE_UNSORTED;
						} else if(ALWAYS_SORTED.equals(temp)) {
							treeSortingPreference = TREE_SORTING_PREFERENCE_ALWAYS_SORTED;
						} else if(ALWAYS_UNSORTED.equals(temp)) {
							treeSortingPreference = TREE_SORTING_PREFERENCE_ALWAYS_UNSORTED;
						} else if(DEFAULT.equals(temp)) {
							treeSortingPreference = TREE_SORTING_PREFERENCE_DEFAULT;
						}
					} else {
						treeSortingPreference = TREE_SORTING_PREFERENCE_DEFAULT;
					}

                    // Obtain details sorting preference
                    temp = attrs.getValue(DETAILS_SORTING_PREFERENCE);
                    if(temp != null) {
                        if(SORT_BY_SCHEMA.equals(temp)) {
                            detailsSortingPreference = DETAILS_SORTING_PREFERENCE_SCHEMA;
                        } else if(DEFAULT.equals(temp)) {
                            detailsSortingPreference = DETAILS_SORTING_PREFERENCE_DEFAULT;
                        } else {
                           detailsSortingPreference = DETAILS_SORTING_PREFERENCE_DEFAULT;
                        }
                    } else {
                        detailsSortingPreference = DETAILS_SORTING_PREFERENCE_DEFAULT;
                    }

					// Obtain tree sort confirmation message
					temp = attrs.getValue(TREE_SORT_CONFIRMATION_MESSAGE);
					if(temp != null) {
						treeSortConfirmationMessage = getTranslation(temp);
					}

					// Obtain tree unsort confirmation message
					temp = attrs.getValue(TREE_UNSORT_CONFIRMATION_MESSAGE);
					if(temp != null) {
						treeUnsortConfirmationMessage = getTranslation(temp);
					}
					
					// Obtain custom control class for empty elements
					temp = attrs.getValue(EMPTY_ELEMENT_CUSTOM_CONTROL_CLASS);
					if(temp != null) {
						Class emptyElementCustomControlClass = getClass(temp);
						if(emptyElementCustomControlClass != null) {
							try {
								Object object = emptyElementCustomControlClass.newInstance();
								if(object instanceof ICustomControlObject3) {
									emptyElementCustomControlObject = (ICustomControlObject3)object;
								}
							} catch (IllegalAccessException e) {e.printStackTrace();}
							catch (InstantiationException e) {e.printStackTrace();}
						}
					}
				}
			}
		}

		public String getCustomizationID() {
			return customizationID;
		}

		public int getTreeSortingPreference() {
			return treeSortingPreference;
		}

        public int getDetailsSortingPreference() {
            return this.detailsSortingPreference;
        }

		public String getTreeSortConfirmationMessage() {
			return treeSortConfirmationMessage;
		}

		public String getTreeUnsortConfirmationMessage() {
			return treeUnsortConfirmationMessage;
		}
		
		public String getEditorTitle() {
			return editorTitle;
		}
		
		public boolean isHideOverviewSection() {
			return hideOverviewSection;
		}
	}
	
	public class FileMonitor extends Thread {

		File files[];
		long lastModified[];
		Customization customizationReferences[];
		private final String FILE_SCHEME = "file:/"; 

		public FileMonitor() {
			Customization[] customizations = (Customization[])customizationMap.values().toArray(new Customization[customizationMap.size()]);
			List filesList = new ArrayList();
			List customizationReferencesList = new ArrayList();
			for (int i = 0; i < customizations.length; i++) {
				if(customizations[i].debugMode) {
					String path = customizations[i].getCustomizationFilePath();
					if(path != null) {
						if(FILE_SCHEME.equals(path.substring(0, FILE_SCHEME.length()))) {
							path = path.substring(FILE_SCHEME.length());
						}
						filesList.add(new File(path));
						customizationReferencesList.add(customizations[i]);
		
						path = customizations[i].getCustomizationPropertiesFilePath();
						if(path != null) {
							if(FILE_SCHEME.equals(path.substring(0, FILE_SCHEME.length()))) {
								path = path.substring(FILE_SCHEME.length());
							}
							filesList.add(new File(path));
							customizationReferencesList.add(customizations[i]);
						}
					}
				}
			}
			Iterator iterator = customizationAddOns.iterator();
			while(iterator.hasNext()) {
				CustomizationAddOn customizationAddOn = (CustomizationAddOn)iterator.next();
				if(customizationAddOn.isDebugMode()) {
					String path = customizationAddOn.getCustomizationFilePath();
					if(path != null) {
						if(FILE_SCHEME.equals(path.substring(0, FILE_SCHEME.length()))) {
							path = path.substring(FILE_SCHEME.length());
						}
						filesList.add(new File(path));
						customizationReferencesList.add(customizationMap.get(customizationAddOn.getCustomizationID()));
						path = customizationAddOn.getCustomizationPropertiesFilePath();
						if(path != null) {
							if(FILE_SCHEME.equals(path.substring(0, FILE_SCHEME.length()))) {
								path = path.substring(FILE_SCHEME.length());
							}
							filesList.add(new File(path));
							customizationReferencesList.add(customizationMap.get(customizationAddOn.getCustomizationID()));
						}
					}
				}
			}
			files = (File[])filesList.toArray(new File[filesList.size()]);
			customizationReferences = (Customization[]) customizationReferencesList.toArray(new Customization[customizationReferencesList.size()]);
			lastModified = new long[files.length];
			for (int i = 0; i < files.length; i++) {
				lastModified[i] = files[i].lastModified();
			}

		}

		public void run() {
			while(true) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				boolean modified = false;
				for (int i = 0; i < files.length; i++) {
					if(files[i].lastModified() != lastModified[i]) {
						customizationReferences[i].parse();
						lastModified[i] = files[i].lastModified();
						modified = true;
					}
				}

				if(modified) {
					Display.getDefault().asyncExec(new Runnable(){

						public void run() {
							IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
							for (int i = 0; i < workbenchWindows.length; i++) {
								IWorkbenchWindow workbenchWindow = workbenchWindows[i];
								IWorkbenchPage[] workbenchPages = workbenchWindow.getPages();
								for (int j = 0; j < workbenchPages.length; j++) {
									IWorkbenchPage workbenchPage = workbenchPages[j];
									IEditorReference[] editorReferences = workbenchPage.getEditorReferences();
									for (int k = 0; k < editorReferences.length; k++) {
										IEditorReference editorReference = editorReferences[k];
										IWorkbenchPart workbenchPart = editorReference.getPart(true);
										if(workbenchPart instanceof DDEMultiPageEditorPart) {
											DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)workbenchPart;
											ddeMultiPageEditorPart.getValidationManager().validateDocument();
											ddeMultiPageEditorPart.showCustomizedTitle();
											ddeMultiPageEditorPart.refresh();
										}
									}
								}
							}
						}
					});
				}
			}
		}
	}
	

}
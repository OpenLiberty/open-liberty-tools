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
package com.ibm.xwt.dde.editor;

import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.ui.internal.tabletree.IDesignViewer;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.xwt.dde.internal.customization.CustomizationManager;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.ValidationManager;
import com.ibm.xwt.dde.internal.viewers.DDEViewerDelegate;

public class DDEMultiPageEditorPart extends XMLMultiPageEditorPart {

	public static int DESIGN_VIEW_PAGE = 0;
	public static int SOURCE_VIEW_PAGE = 1;
	private DDEViewerDelegate ddeViewerDelegate;
	private IAnnotationModelListener annotationModelListener;
	private ActivationListener activationListener;
	private boolean pageCreationInProgress = false;
	
	// Constructor
	public DDEMultiPageEditorPart() {
		super();
		noToolbar();
	}

	
	/**
	 * This method sets the selection of the Editor using an x-path expression.
	 * @param path: 		 x-path expression used to set the editor selection.
	 * @param namespaceData: string containing the set of prefixes and namespaces used
	 * 						 in the x-path expression writen in the form:
	 * 						  			prefix = namespace
	 * 						 If there are many namespace, these are writen comma
	 * 						 separated.
	 * 
	 * Example
	 *
	 * x-path:  	  pre:web-app/pre:display-name
	 * namespaceData: pre=http://java.sun.com/xml/ns/javaee
	 *
	 * Note that if the document does not have a namespace, then no prefixes are
	 * needed in the x-path expression and the namespaceData parameter should be
	 * null.
	 */
	public void setSelection(String path, String namespaceData) {
		Document document = ddeViewerDelegate.getDocument();
		NodeList nodeList = ModelUtil.runXPathAgainstDocument(document, path, namespaceData);
		if(nodeList != null && nodeList.getLength() > 0) {
			Node node = nodeList.item(0);
			ISelection selection = new StructuredSelection(node);
			if(getActivePage() == DESIGN_VIEW_PAGE) {
				ddeViewerDelegate.setSelection(selection, true);
			} else {
				Object object = this.getAdapter(ITextEditor.class);
				if(object instanceof StructuredTextEditor) {
					StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
					ISelectionProvider selectionProvider = structuredTextEditor.getSelectionProvider();
					if(selectionProvider != null) {
						selectionProvider.setSelection(selection);
					}
				}
			}
		}
	}
	
	public Element getDocumentElement() {
		return ddeViewerDelegate.getDocument().getDocumentElement();
	}
	
	public void setSelection(Node node) {
		IStructuredSelection selection = new StructuredSelection(node);
		if(getActivePage() == DESIGN_VIEW_PAGE) {
			ddeViewerDelegate.setSelection(selection, true);
		} else {
			Object object = this.getAdapter(ITextEditor.class);
			if(object instanceof StructuredTextEditor) {
				StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
				ISelectionProvider selectionProvider = structuredTextEditor.getSelectionProvider();
				if(selectionProvider != null) {
					selectionProvider.setSelection(selection);
				}
			}
		}
	}


	/**
	 * This method returns the currently active page in the editor,
	 * using the constants DESIGN_VIEW_PAGE and SOURCE_VIEW_PAGE.
	 * @return
	 */
	public int getEditorActivePage() {
		return getActivePage();
	}
	
	public boolean isDesignPageActiveAndInFocus() {
		// Protect against a null delegate.  See RTC 126775.
		if (ddeViewerDelegate != null)
		  return ddeViewerDelegate.isDesignViewActive();
		return false;
	}
	
	
	/**
	 * This method sets the editor active page using the constants
	 * DESIGN_VIEW_PAGE and SOURCE_VIEW_PAGE.
	 * @return
	 */
	public void setEditorActivePage(int page) {
		if(page != getActivePage()) {
			if(page == DESIGN_VIEW_PAGE) {
				setActivePage(0);
			} else {
				this.setActivePage(1);
			}
		}
	}


	protected void createPages() {
		pageCreationInProgress = true;
		super.createPages();
		Object object = getAdapter(ITextEditor.class);
		if(object instanceof StructuredTextEditor) {
			StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
			IActionBars actionBars = ((IEditorSite)getSite()).getActionBars();
			actionBars.setGlobalActionHandler(ITextEditorActionConstants.UNDO, structuredTextEditor.getAction(ITextEditorActionConstants.UNDO));
			actionBars.setGlobalActionHandler(ITextEditorActionConstants.REDO, structuredTextEditor.getAction(ITextEditorActionConstants.REDO));
			actionBars.setGlobalActionHandler(ITextEditorActionConstants.REVERT_TO_SAVED, structuredTextEditor.getAction(ITextEditorActionConstants.REVERT_TO_SAVED));
			IAnnotationModel annotationModel = structuredTextEditor.getDocumentProvider().getAnnotationModel(getEditorInput());
			annotationModelListener = new IAnnotationModelListener(){

				public void modelChanged(IAnnotationModel model) {
					ddeViewerDelegate.updateErrorsFromAnnotationModel(model);
					
				}
			};
			annotationModel.addAnnotationModelListener(annotationModelListener);
		}
		pageCreationInProgress = false;
	}


	/**
	 * This method refreshes the contents of the editor. 
	 */
	public void refresh() {
		ddeViewerDelegate.refresh(true);
	}
	
	
	public void refreshDocumentValidation() {
		ddeViewerDelegate.getValidationManager().validateDocument();
	}
	
	
	/**
	 * @ Deprecated
	 * Instead of invoking this method to reset customizations add
	 * debugMode = "true" to the customization extension item. This
	 * will make the editor listen to changes in the customization
	 * file (and properties file when applicable) and react to any
	 * changes performed on it automatically.  
	 */
	public void resetCustomizations() {
		CustomizationManager.getInstance().resetCustomizations();
		ddeViewerDelegate.activateDesignView();
		ddeViewerDelegate.refresh(true);
	}
	
	public void showCustomizedTitle() {
		Customization customization = CustomizationManager.getInstance().getCustomization(getSite().getId());
		if(customization != null) {
			String editorTitle = customization.getEditorTitle();
			if(editorTitle != null) {
				this.setPartName(editorTitle);
			}
		}
	}
	
	
	protected IDesignViewer createDesignPage() {
		Customization customization = CustomizationManager.getInstance().getCustomization(getSite().getId());
		showCustomizedTitle();
		ddeViewerDelegate = new DDEViewerDelegate(getContainer(), customization, (IEditorPart)this);
		activationListener = new ActivationListener(getSite().getWorkbenchWindow().getPartService());
		return ddeViewerDelegate;
	}
	
	
	protected void pageChange(int newPageIndex) {
		if(!pageCreationInProgress) {
			super.pageChange(newPageIndex);
			if(getActivePage() == DESIGN_VIEW_PAGE) {
				ddeViewerDelegate.activateDesignView();
			} else {
				ddeViewerDelegate.deActivateDesignView();
			}
		}	else {
		  // Need to notify the Multi page editor service that the source editor within it exists and has become active
		  // This will ensure the key binding is activated.
		  activateSite();
		}
	}
	
	public ValidationManager getValidationManager() {
		return ddeViewerDelegate.getValidationManager();
	}
	
	
	class ActivationListener implements IPartListener, IWindowListener {

		public ActivationListener(IPartService partService) {
			partService.addPartListener(this);
			PlatformUI.getWorkbench().addWindowListener(this);
		}
		
		public void partActivated(IWorkbenchPart part) {
			if(DDEMultiPageEditorPart.this.equals(part)) {
				if(getActivePage() == DESIGN_VIEW_PAGE) {
					ddeViewerDelegate.activateDesignView();
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
			if(DDEMultiPageEditorPart.this.equals(part)) {
				if(getActivePage() == DESIGN_VIEW_PAGE) {
					ddeViewerDelegate.deActivateDesignView();
				}
			}
		}

		public void partOpened(IWorkbenchPart part) {
		}

		public void windowActivated(IWorkbenchWindow window) {
		}

		public void windowClosed(IWorkbenchWindow window) {
		}

		public void windowDeactivated(IWorkbenchWindow window) {
		}

		public void windowOpened(IWorkbenchWindow window) {
		}
	}
	
	
	public void dispose() {
		ddeViewerDelegate.dispose();
		Object object = this.getAdapter(ITextEditor.class);
		if(object instanceof StructuredTextEditor) {
			StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
			IAnnotationModel annotationModel = structuredTextEditor.getDocumentProvider().getAnnotationModel(getEditorInput());
			annotationModel.removeAnnotationModelListener(annotationModelListener);
		}
		if (activationListener != null) {
			getSite().getWorkbenchWindow().getPartService().removePartListener(activationListener);
			PlatformUI.getWorkbench().removeWindowListener(activationListener);
		}
		super.dispose();
		
		ddeViewerDelegate = null;
		
	}
	
	
	public boolean validateEditorInput() {
		Object object = getAdapter(ITextEditor.class);
		if(object instanceof StructuredTextEditor) {
			StructuredTextEditor structuredTextEditor = (StructuredTextEditor)object;
			structuredTextEditor.safelySanityCheckState(getEditorInput());
			return structuredTextEditor.validateEditorInputState();
		}
		return true;
	}

  /**
   * This is not API.  Do not use.
   * 
   * This is a hook to support automated testing only.
   * 
   * @return
   */
	public Composite getFormHeadComposite()
	{
		return ddeViewerDelegate.getFormHeadComposite();
	}
}
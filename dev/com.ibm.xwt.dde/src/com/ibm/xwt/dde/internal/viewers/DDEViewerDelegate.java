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

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.CMDocumentLoader;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.DOMObserverAdapter;
import org.eclipse.wst.xml.ui.internal.tabletree.IDesignViewer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.ValidationManager;


public class DDEViewerDelegate implements IDesignViewer {

	private final int grammarReloadAttempts = 25;
	private final int grammarReloadMillisecondsDelay = 250;
	private DDEViewer ddeViewer;
	private IDocument document;
	private Document domDocument;
	private boolean designViewActive;
	boolean listen;
	INodeNotifier cachedNotifier;
	private IModelStateListener modelStateListener;
	IDocumentListener documentListener;

	public DDEViewerDelegate(Composite parent, Customization customization, IEditorPart editorPart) {
		ddeViewer = new DDEViewer(parent, customization, editorPart);
		ddeViewer.createContents(parent);
		listen = true;
	}

	public Control getControl() {
		return ddeViewer.getControl();
	}

	public String getTitle() {
		return Messages.LABEL_DESIGN;
	}

	public void setSelection(ISelection selection, boolean reveal) {
		ddeViewer.setSelection(selection, reveal);
	}

	public Document getDocument() {
		return domDocument;
	}

	public void setDocument(IDocument document) {
		
		this.document = document;
		
		if(document != null) {
			
			documentListener = new IDocumentListener(){
	
				public void documentAboutToBeChanged(DocumentEvent arg0) {
				}
	
				public void documentChanged(DocumentEvent arg0) {
					if(listen) {
						updateFromListener();
					}
				}
			};
			
			document.addDocumentListener(documentListener);
			
			IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
			try {
				if ((model != null) && (model instanceof IDOMModel)) {
					this.domDocument = ((IDOMModel) model).getDocument();
					asyncFirstModelLoad(domDocument);
					if(!ModelUtil.isModelPresent(domDocument)) {
						ddeViewer.showMissingGrammarMessage();
					} else {
						ddeViewer.hideMissingGrammarMessage();
					}
					ddeViewer.setInput(domDocument);
					IStructuredTextUndoManager undoManager = model.getUndoManager();
					ddeViewer.setUndoManager(undoManager);
					
					modelStateListener = new IModelStateListener(){
	
						public void modelAboutToBeChanged(IStructuredModel model) {
							listen = false;
							cachedNotifier = null;
						}
						
						public void modelAboutToBeReinitialized(IStructuredModel structuredModel) {
						}
	
						public void modelChanged(IStructuredModel model) {
							listen = true;
							updateFromListener();
						}
	
						public void modelDirtyStateChanged(IStructuredModel model, boolean isDirty) {
						}
	
						public void modelReinitialized(IStructuredModel structuredModel) {
						}
	
						public void modelResourceDeleted(IStructuredModel model) {
						}
	
						public void modelResourceMoved(IStructuredModel oldModel, IStructuredModel newModel) {
						}
					};
					
					model.addModelStateListener(modelStateListener);
				}
				else {
				}
			}
			finally {
				if (model != null) {
					model.releaseFromRead();
				}
			}
		}
	}

	private void asyncFirstModelLoad(Document domDocument) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(domDocument);
		if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
			CMDocumentManager manager = modelQuery.getCMDocumentManager();
			manager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, false);
			
			// get domobserver and attempt to disable it
			DOMObserverAdapter domObserverAdapter = null;
			Object o = ((INodeNotifier)domDocument).getExistingAdapter(DOMObserverAdapter.class);
			boolean domObserverDisabled = true;
			if (o instanceof DOMObserverAdapter) {
				domObserverAdapter = (DOMObserverAdapter)o;
				domObserverDisabled = domObserverAdapter.disableObserver(true, true);
			}
			
			// if domobserver was disabled, load cmdocument ourselves
			// otherwise, let domobserver do it since it already started
			CMDocumentLoader loader = new CMDocumentLoader(domDocument, manager);;
			if (domObserverDisabled) {
				loader.loadCMDocuments();
			}

			if(!ModelUtil.isModelPresent(domDocument)) {
				Element documentElement = domDocument.getDocumentElement();
				if (documentElement != null) {
					String namespace = documentElement.getNamespaceURI();
					if (namespace == null) {
						namespace = ""; //$NON-NLS-1$
					}
					// let domobserver try to load
					int attempts = 0;
					while(!domObserverDisabled && domObserverAdapter.isObserverLoading() && attempts < grammarReloadAttempts) {
						try {
							Thread.sleep(grammarReloadMillisecondsDelay);
						} catch (InterruptedException e) {}
						++attempts;
					}
					
					// let cmdocument manager try to load
					int status = manager.getCMDocumentStatus(namespace);
					attempts = 0;
					while((status != CMDocumentCache.STATUS_LOADED) && (status != CMDocumentCache.STATUS_ERROR) && attempts < grammarReloadAttempts && !ModelUtil.isModelPresent(domDocument)) {
						try {
							Thread.sleep(grammarReloadMillisecondsDelay);
						} catch (InterruptedException e) {}
						status = manager.getCMDocumentStatus(namespace);
						if(status == CMDocumentCache.STATUS_NOT_LOADED) {
							loader.loadCMDocuments();
						}
						++attempts;
					}
				}
			}
		}
	}
	
	private void asyncModelLoad(Document domDocument) {
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(domDocument);
		if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
			CMDocumentManager manager = modelQuery.getCMDocumentManager();
			manager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, false);
			CMDocumentLoader loader = new CMDocumentLoader(domDocument, manager);
			loader.loadCMDocuments();
			if(!ModelUtil.isModelPresent(domDocument)) {
				Element documentElement = domDocument.getDocumentElement();
				if (documentElement != null) {
					String namespace = documentElement.getNamespaceURI();
					if (namespace == null) {
						namespace = ""; //$NON-NLS-1$
					}
					int status = manager.getCMDocumentStatus(namespace);
					int attempts = 0;
					while(status == CMDocumentCache.STATUS_LOADING && attempts < grammarReloadAttempts) {
						try {
							Thread.sleep(grammarReloadMillisecondsDelay);
						} catch (InterruptedException e) {}
						status = manager.getCMDocumentStatus(namespace);
						if(status == CMDocumentCache.STATUS_NOT_LOADED) {
							loader.loadCMDocuments();
						}
						++attempts;
					}
				}
			}
		}
	}

	public ISelectionProvider getSelectionProvider() {
		return ddeViewer;
	}

	public void activateDesignView() {
		designViewActive = true;
		ddeViewer.activate();
	}

	public void deActivateDesignView() {
		designViewActive = false;
		ddeViewer.deActivate();
	}

	public boolean isDesignViewActive() {
		return designViewActive;
	}


	public void refresh(boolean forceFrefresh) {
		ddeViewer.refresh(forceFrefresh);
	}

	
	private void updateFromListener() {
		if(!ddeViewer.getControl().isDisposed()) {
			ddeViewer.getControl().getDisplay().asyncExec(new Runnable(){
				public void run() {
					if(!ddeViewer.getControl().isDisposed()) {
						if(!designViewActive && !ddeViewer.isValidationDirty()) {
							ddeViewer.setValidationDirty();
						}
						asyncModelLoad(domDocument);
						if(!ModelUtil.isModelPresent(domDocument)) {
							ddeViewer.showMissingGrammarMessage();
						} else {
							ddeViewer.hideMissingGrammarMessage();
						}
						ddeViewer.refresh();
					}
				}
			});
		}
	}

	public void updateErrorsFromAnnotationModel(IAnnotationModel model) {
		if(!ddeViewer.getControl().isDisposed()) {
			ddeViewer.updateErrorsFromAnnotationModel(model);
		}
	}

	public ValidationManager getValidationManager() {
		return ddeViewer.getValidationManager();
	}

	
	public void dispose() {
		if(domDocument instanceof IDOMDocument) {
			IDOMModel model = ((IDOMDocument)domDocument).getModel();
			if(model != null) {
				model.removeModelStateListener(modelStateListener);
			}
		}
		if(document != null) {
			document.removeDocumentListener(documentListener);
		}
		ddeViewer.dispose();

		document = null;
		domDocument = null;
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
		return ddeViewer.getFormHead();
	}
}

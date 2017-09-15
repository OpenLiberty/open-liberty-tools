/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.validation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.operations.LocalizedMessage;
import org.eclipse.wst.validation.internal.operations.WorkbenchContext;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.customization.CustomizationManager;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.validation.ValidationManager;


public class DDEValidator extends AbstractValidator implements IValidator {

	public static final String GET_FILE = "getFile"; //$NON-NLS-1$
	public static final String COLUMN_NUMBER_ATTRIBUTE = "columnNumber"; //$NON-NLS-1$

	/** 
	 * @deprecated
	 */
	public void validate(IValidationContext context, IReporter reporter) throws ValidationException {

		// Check the validation context
		boolean workbenchValidation = context instanceof WorkbenchContext;
		
		// Iterate through the files to validate
		String[] fileURIs = context.getURIs();
		if (fileURIs != null && fileURIs.length > 0) {
			int filesCount = fileURIs.length;
			for (int i = 0; i < filesCount && !reporter.isCancelled(); i++) {
				String fileName = fileURIs[i];
				if (fileName != null) {
					IFile file = (IFile) context.loadModel(GET_FILE, new Object[]{fileName});
					if (file != null && shouldValidate(file)) {
						// Check if there is an active Design View Validation Manager already handling the current file 
						ValidationManager activeDesignViewValidationManager = getActiveDesignViewValidationManager(file);
						if(activeDesignViewValidationManager != null) {
							// if an active Design View Validation Manager has been found, export its already computed messages
							activeDesignViewValidationManager.getMessageManager().exportMessagesToReporter(file, this, reporter, !workbenchValidation);
						} else {
							// if no active Design View Validation Manager has been found, perform file validation
							performFileValidation(file, reporter, workbenchValidation);
						}
					}
				}
			}
		}
	}

	
	/**
	 * Determine if a given file should be validated.
	 * @deprecated
	 * 
	 * @param file
	 *            The file that may be validated.
	 * @return True if the file should be validated, false otherwise.
	 */
	private static boolean shouldValidate(IFile file) {
		IResource resource = file;
		do {
			if (resource.isDerived() || resource.isTeamPrivateMember()
					|| !resource.isAccessible()
					|| resource.getName().charAt(0) == '.') {
				return false;
			}
			resource = resource.getParent();
		} while ((resource.getType() & IResource.PROJECT) == 0);

		return true;
	}
	
	
	/**
	 * Obtains the customization for a given file through its content type - editor association
	 * @param file
	 * @return
	 */
	private Customization getCustomization(IFile file) {
		try {
			IContentDescription contentDescription = file.getContentDescription();
			IContentType contentType = contentDescription.getContentType();
			IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
			IEditorDescriptor[] editors = editorRegistry.getEditors(file.getName(), contentType);
			for (int i = 0; i < editors.length; i++) {
				Customization customization = CustomizationManager.getInstance().getCustomization(editors[i].getId());
				if(customization != null) {
					return customization;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	/**
	 * Performs the file validation
	 * @param file
	 * @param reporter
	 * @param workbenchValidation
	 */
	public void performFileValidation(IFile file, IReporter reporter, boolean workbenchValidation) {
		IStructuredModel modelForRead = null;
		boolean validationSuccessfull = false;
		
		// Clear all messages
		reporter.removeAllMessages(this, file);
		
		try {
			// Obtain the model
			modelForRead = StructuredModelManager.getModelManager().getModelForRead(file);
			if(modelForRead != null) {
				IDOMDocument document = ((IDOMModel)modelForRead).getDocument();
				Customization customization = getCustomization(file);	
				int validationEnviroment = workbenchValidation? ValidationManager.WORKBENCH_VALIDATION : ValidationManager.EDITOR_VALIDATION;
				ValidationManager validationManager = new ValidationManager(customization, file, validationEnviroment);
				validationManager.setEditorInput(getEditorInput(file));
				validationManager.setDocument(document);
				validationManager.validateDocument();
				validationManager.getMessageManager().exportMessagesToReporter(file, this, reporter, !workbenchValidation);
				validationSuccessfull = true;
			}
		} catch (Exception e) {e.printStackTrace();}
		finally {
			if(modelForRead != null) {
				modelForRead.releaseFromRead();
			}
			if(!validationSuccessfull) {
				LocalizedMessage localizedMessage = new LocalizedMessage(LocalizedMessage.HIGH_SEVERITY, Messages.ERROR_RESOURCE_CONTENTS_COULD_NOT_BE_VALIDATED);
				localizedMessage.setTargetObject(file);
				reporter.addMessage(this, localizedMessage);
			}
		}
	}
	
	
	/**
	 * Searches for an active Design View Validation Manager currently handling the given file
	 * @param file
	 * @return
	 */
	private ValidationManager getActiveDesignViewValidationManager(IFile file) {
		IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < workbenchWindows.length; i++) {
			IWorkbenchWindow workbenchWindow = workbenchWindows[i];
			IWorkbenchPage[] workbenchPages = workbenchWindow.getPages();
			for (int j = 0; j < workbenchPages.length; j++) {
				IWorkbenchPage workbenchPage = workbenchPages[j];
				IEditorReference[] editorReferences = workbenchPage.getEditorReferences();
				for (int k = 0; k < editorReferences.length; k++) {
					IEditorReference editorReference = editorReferences[k];
					IWorkbenchPart workbenchPart = editorReference.getPart(false);
					if(workbenchPart instanceof DDEMultiPageEditorPart) {
						DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)workbenchPart;
						if(ddeMultiPageEditorPart.isDesignPageActiveAndInFocus()) {
							IEditorInput editorInput = ddeMultiPageEditorPart.getEditorInput();
							Object adapter = editorInput.getAdapter(IFile.class);
							if(adapter instanceof IFile) {
								if(file.equals(adapter)) {
									return ddeMultiPageEditorPart.getValidationManager();
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Searches for an EditorInput currently handling the given file
	 * @param file
	 * @return
	 */
	private IEditorInput getEditorInput(IFile file) {
		IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < workbenchWindows.length; i++) {
			IWorkbenchWindow workbenchWindow = workbenchWindows[i];
			IWorkbenchPage[] workbenchPages = workbenchWindow.getPages();
			for (int j = 0; j < workbenchPages.length; j++) {
				IWorkbenchPage workbenchPage = workbenchPages[j];
				IEditorReference[] editorReferences = workbenchPage.getEditorReferences();
				for (int k = 0; k < editorReferences.length; k++) {
					IEditorReference editorReference = editorReferences[k];
					IWorkbenchPart workbenchPart = editorReference.getPart(false);
					if(workbenchPart instanceof DDEMultiPageEditorPart) {
						DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)workbenchPart;
						if(ddeMultiPageEditorPart != null) {
							IEditorInput editorInput = ddeMultiPageEditorPart.getEditorInput();
							if(editorInput != null){
								Object adapter = editorInput.getAdapter(IFile.class);
								if(adapter instanceof IFile) {
									if(file.equals(adapter)) {
										return editorInput;
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	
	public void cleanup(IReporter reporter) {

	}
	
	
  public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor)
  {
    ValidationResult result = new ValidationResult();
    try
    {
      validateFile(resource, state, result.getReporter(monitor));
    }
    catch (ValidationException e)
    {
      result.setValidationException(e);
    }
    return result;
  }
    
    
  protected IStatus validateFile(IResource resource, ValidationState state, IReporter reporter) throws ValidationException
  {
    // TODO: Once source validation is ported to use this method, we can then use the state to determine if 
    // it should be either a WORKBENCH_VALIDATION or an EDITOR_VALIDATION
    // ie. boolean workbenchValidation = state.get(CONTEXT) instanceof WorkbenchContext;
    
    if (resource instanceof IFile)
    {
      IFile file = (IFile) resource;
      ValidationManager activeDesignViewValidationManager = getActiveDesignViewValidationManager(file);
      if(activeDesignViewValidationManager != null) {
        // if an active Design View Validation Manager has been found, export its already computed messages
        activeDesignViewValidationManager.getMessageManager().exportMessagesToReporter(file, this, reporter, false); // !workbenchValidation
      } else {
        // if no active Design View Validation Manager has been found, perform file validation
        performFileValidation(file, reporter, true);   // workbenchValidation
      }
    }

    if (reporter.isCancelled())
    {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }
}

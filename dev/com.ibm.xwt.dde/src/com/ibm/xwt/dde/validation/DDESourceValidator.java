/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.validation;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.ui.internal.validation.DelegatingSourceValidator;

public abstract class DDESourceValidator extends DelegatingSourceValidator {

	private Validator _validator;
	  
	final protected void updateValidationMessages(List messages, IDOMDocument document, IReporter reporter) {
		for (int i = 0; i < messages.size(); i++) {
			IMessage message = (IMessage) messages.get(i);
			reporter.addMessage(this, message);
		}
	}

	  private Validator getValidator()
	  {
	    if (_validator == null)
	      _validator = ValidationFramework.getDefault().getValidator(getValidatorID());
	    return _validator;
	  }
	  
	  /*
	   * (non-Javadoc)
	   * 
	   * @see org.eclipse.wst.xml.ui.internal.validation.DelegatingSourceValidator#getDelegateValidator()
	   */
	  protected IValidator getDelegateValidator()
	  {
		  Validator v = getValidator();
		  if (v == null)
		    return null;
		  return v.asIValidator();
	  }
	
	public abstract String getValidatorID();
	
	protected boolean isDelegateValidatorEnabled(IFile file)
	{
		Validator v = getValidator();
		if (v == null)
			return false;
		if (!v.shouldValidate(file, false, false))
			return false;
		return v.isBuildValidation() || v.isManualValidation();
	}
}
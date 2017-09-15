/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.st.core.internal.IPromptIssue;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.PromptAction;

class PromptResponse implements IPromptResponse {

    final private Map<IPromptIssue, PromptAction> actionMap = new HashMap<IPromptIssue, PromptAction>();
    final private Map<IPromptIssue, Boolean> applyAlwaysMap = new HashMap<IPromptIssue, Boolean>();

    @Override
    public PromptAction getSelectedAction(IPromptIssue issue) {
        return actionMap.get(issue);
    }

    @Override
    public boolean getApplyAlways(IPromptIssue issue) {
        Boolean value = applyAlwaysMap.get(issue);
        return Boolean.TRUE == value ? true : false;
    }

    void putSelectionAction(IPromptIssue issue, PromptAction action) {
        actionMap.put(issue, action);
    }

    void putApplyAlways(IPromptIssue issue, boolean value) {
        applyAlwaysMap.put(issue, (value == true) ? Boolean.TRUE : Boolean.FALSE);
    }

    boolean isEnableApplyAlways(IPromptIssue issue) {
        return applyAlwaysMap.get(issue) != null;
    }
}

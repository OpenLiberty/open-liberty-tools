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
package com.ibm.ws.st.core.internal;

/**
 * The various prompt actions
 */
public enum PromptAction {
    IGNORE(Messages.promptActionIgnore),
    UPDATE_SERVER_CONFIG(Messages.promptUpdateServerConfiguration),
    REMOVE_FROM_SERVER(Messages.promotActionRemoveFromServer),
    RESTART_SERVERS(Messages.promptActionRestartServers),
    DELETE_SERVER_FILES(Messages.promptActionDeleteServers),
    RESTART_APPLICATIONS(Messages.promptActionRestartApplications),
    STOP_SERVER(Messages.promptActionStopServer),
    YES(Messages.prompt_yes),
    NO(Messages.prompt_no);

    private final String label;

    private PromptAction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

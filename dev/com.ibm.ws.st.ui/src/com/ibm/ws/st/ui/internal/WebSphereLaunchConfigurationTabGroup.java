/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

import com.ibm.ws.st.core.internal.Constants;

/**
 * A tab group for launching the server.
 */
public class WebSphereLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[3];

        IServerType[] servers = ServerCore.getServerTypes();
        List<String> list = new ArrayList<String>(5);
        for (IServerType st : servers) {
            if (st.getId().startsWith(Constants.SERVER_ID_PREFIX))
                list.add(st.getId());
        }

        tabs[0] = new ServerLaunchConfigurationTab(list.toArray(new String[list.size()]));
        tabs[0].setLaunchConfigurationDialog(dialog);
        tabs[1] = new SourceLookupTab();
        tabs[1].setLaunchConfigurationDialog(dialog);
        tabs[2] = new CommonTab();
        tabs[2].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}

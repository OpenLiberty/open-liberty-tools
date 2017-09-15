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

import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerCore;

public class RuntimeContentProvider extends DDETreeContentProvider {
    public Object[] getElements(Object inputElement) {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        List<IRuntime> list = new ArrayList<IRuntime>();
        for (IRuntime r : runtimes) {
            if (checkType(r))
                list.add(r);
        }
        return list.toArray();
    }
}

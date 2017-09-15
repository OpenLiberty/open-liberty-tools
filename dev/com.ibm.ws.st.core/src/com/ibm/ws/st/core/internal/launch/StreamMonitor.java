/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

public class StreamMonitor implements IStreamMonitor {
    private final List<IStreamListener> listeners = new ArrayList<IStreamListener>(2);
    private final StringBuffer sb = new StringBuffer();
    private static final String AUDIT = "[AUDIT   ]";
    private static final String ERROR = "[ERROR   ]";
    private static final String WARNING = "[WARNING ]";
    private static final String Err = "[err]";
    private boolean useConsoleLog = true;
    private boolean printMessage = false;

    public StreamMonitor(boolean useConsoleLog) {
        this.useConsoleLog = useConsoleLog;
    }

    @Override
    public void addListener(IStreamListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    protected void streamAppended(String s) {
        String r = null;
        if (useConsoleLog) {
            r = s;
        } else {
            // Note the date field length can be different
            //[5/1/12 11:23:18:363 EDT] 00000019 com.ibm.ws.kernel.feature.internal.FeatureManager            I CWWKF0007I: Feature update started.
            //[4/23/12 16:06:32:171 EDT] 00000011 com.ibm.ws.app.manager.internal.monitor.DropinMonitor        A CWWKZ0058I: Monitoring dropins for applications.

            int i = s.indexOf(']');
            if ((s.startsWith("[")) && i > 0) {
                printMessage = true;
                char c = s.charAt(i + 72);
                switch (c) {
                    case 'A':
                        r = AUDIT + s.substring(i + 73);
                        break;
                    case 'E':
                        r = ERROR + s.substring(i + 73);
                        break;
                    case 'W':
                        r = WARNING + s.substring(i + 73);
                        break;
                    case 'O': // system.out
                        r = s.substring(i + 73);
                        break;
                    case 'R': //system.err
                        r = Err + s.substring(i + 73);
                        break;
                    default:
                        printMessage = false; // if the message is not one of the types above, then  don't print it to the console
                        return;
                }
            } else if (printMessage)
                // when there is an exception, the message doesn't start with '[' bracket..print if the message is a part of the message starting with
                // one of the codes (A, E, O, W, R)
                r = s;
        }
        
        if (r != null) {
            sb.append(r);
            for (IStreamListener l : listeners)
                l.streamAppended(r, this);
        }
    }

    @Override
    public String getContents() {
        return sb.toString();
    }

    @Override
    public void removeListener(IStreamListener listener) {
        listeners.remove(listener);
    }
}

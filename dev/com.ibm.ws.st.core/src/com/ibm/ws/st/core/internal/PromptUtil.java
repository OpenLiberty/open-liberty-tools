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
package com.ibm.ws.st.core.internal;

import org.eclipse.wst.common.core.util.UIContextDetermination;

public class PromptUtil {
    private static final boolean WTP_AUTO_TEST = Boolean.getBoolean("wtp.autotest.noninteractive");
    private static final String SUPRESS_POP_UPS = System.getProperty("suppress_pop_ups", "");
    private static Boolean isRunningGUIMode = null;

    public static boolean isSuppressDialog() {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "WTP_AUTO_TEST=" + WTP_AUTO_TEST + " SUPRESS_POP_UPS=" + SUPRESS_POP_UPS);
        }

        if (WTP_AUTO_TEST || "1".equals(SUPRESS_POP_UPS)) {
            return true;
        }
        return false;
    }

    public static boolean isRunningGUIMode() {

        if (isRunningGUIMode == null) {
            boolean isGui = false;
            switch (UIContextDetermination.getCurrentContext()) {
                case UIContextDetermination.UI_CONTEXT:
                    if (!WTP_AUTO_TEST)
                        isGui = true;
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.DETAILS, "Running in GUI mode");
                    }
                    break;
                case UIContextDetermination.HEADLESS_CONTEXT:
                default:
                    isGui = false;
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.DETAILS, "Running in headless mode");
                    }
            }
            isRunningGUIMode = new Boolean(isGui);
        }
        return isRunningGUIMode.booleanValue();
    }
}

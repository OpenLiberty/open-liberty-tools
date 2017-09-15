/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.trace;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

/**
 *
 */
public class TraceSpecification implements DebugOptionsListener {

    private boolean isInfoEnabled = false;
    private boolean isWarningEnabled = false;
    private boolean isResourceEnabled = false;
    private boolean isExtensionEnabled = false;
    private boolean isPerfEnabled = false;
    private boolean isJMXEnabled = false;
    private boolean isErrorEnabled = false;
    private boolean isDetailsEnabled = false;
    private boolean isSSMEnabled = false;
    private boolean isSecurityEnabled = false;
    private final String debugOption;
    private final byte[] recognizedLevels;
    private final TraceDebugListener listener;

    public TraceSpecification(String pluginId, byte[] recognizedLevels, TraceDebugListener listener) {
        debugOption = pluginId + "/debug";
        this.recognizedLevels = new byte[recognizedLevels.length];
        System.arraycopy(recognizedLevels, 0, this.recognizedLevels, 0, recognizedLevels.length);
        this.listener = listener;
    }

    @Override
    public void optionsChanged(DebugOptions options) {
        final boolean debugOptionEnabled = options.getBooleanOption(debugOption, false);
        if (debugOptionEnabled && recognizedLevels != null) {
            for (byte level : recognizedLevels) {
                final String debugOptionName = TraceLevel.getDebugOptionName(level);
                if (debugOptionName != null) {
                    final boolean value = options.getBooleanOption(debugOption + "/" + debugOptionName, false);
                    setLevelEnabled(level, value);
                }
            }
        } else {
            resetOptions();
        }
        if (listener != null) {
            listener.debugChanged(debugOptionEnabled);
        }
    }

    private void resetOptions() {
        isInfoEnabled = false;
        isWarningEnabled = false;
        isResourceEnabled = false;
        isExtensionEnabled = false;
        isPerfEnabled = false;
        isJMXEnabled = false;
        isErrorEnabled = false;
        isDetailsEnabled = false;
        isSSMEnabled = false;
        isSecurityEnabled = false;
    }

    private final void setLevelEnabled(byte level, boolean value) {
        switch (level) {
            case TraceLevel.INFO:
                isInfoEnabled = value;
                break;
            case TraceLevel.WARNING:
                isWarningEnabled = value;
                break;
            case TraceLevel.PERFORMANCE:
                isPerfEnabled = value;
                break;
            case TraceLevel.RESOURCE:
                isResourceEnabled = value;
                break;
            case TraceLevel.EXTENSION_POINT:
                isExtensionEnabled = value;
                break;
            case TraceLevel.JMX:
                isJMXEnabled = value;
                break;
            case TraceLevel.ERROR:
                isErrorEnabled = value;
                break;
            case TraceLevel.DETAILS:
                isDetailsEnabled = value;
                break;
            case TraceLevel.SSM:
                isSSMEnabled = value;
            case TraceLevel.SECURITY:
                isSecurityEnabled = value;
            default:
                break;
        }
    }

    public final boolean isLevelEnabled(byte level) {
        switch (level) {
            case TraceLevel.INFO:
                return isInfoEnabled;
            case TraceLevel.WARNING:
                return isWarningEnabled;
            case TraceLevel.PERFORMANCE:
                return isPerfEnabled;
            case TraceLevel.RESOURCE:
                return isResourceEnabled;
            case TraceLevel.EXTENSION_POINT:
                return isExtensionEnabled;
            case TraceLevel.JMX:
                return isJMXEnabled;
            case TraceLevel.ERROR:
                return isErrorEnabled;
            case TraceLevel.DETAILS:
                return isDetailsEnabled;
            case TraceLevel.SSM:
                return isSSMEnabled;
            case TraceLevel.SECURITY:
                return isSecurityEnabled;
            default:
                break;
        }

        return false;
    }
}

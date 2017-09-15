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
package com.ibm.ws.st.core.internal.config;

import com.ibm.ws.st.core.internal.expression.Expression;
import com.ibm.ws.st.core.internal.expression.Expression.Operator;

/**
 * Configuration variables utilities
 */
public class ConfigVarsUtils {

    public static String getVarRef(String varName) {
        return "${" + varName + "}";
    }

    public static boolean containsReference(String value) {
        final int start = value.indexOf("${");
        final int end = value.indexOf("}");

        return start >= 0 && end > start;
    }

    /**
     * If the selection range is within a variable then return the name
     * of that variable, otherwise, return null.
     */
    public static String getVariableName(String text, int startOffset, int endOffset) {
        int varStart = -1;
        int varEnd = -1;
        int index = text.indexOf("${");
        while (index >= 0 && index <= startOffset) {
            varStart = index;
            index = text.indexOf("}", index);
            if (index != -1) {
                varEnd = index;
            } else {
                varEnd = text.length() - 1;
                index = varEnd;
            }
            if (startOffset >= varStart && endOffset <= varEnd) {
                String value = text.substring(varStart + 2, varEnd);

                // Check for variable expression
                for (Operator operator : Expression.Operator.values()) {
                    int operatorIndex = value.indexOf(operator.getSymbol());
                    if (operatorIndex != -1) {
                        if (startOffset - varStart - 2 <= operatorIndex && endOffset - varStart - 2 <= operatorIndex) {
                            value = value.substring(0, operatorIndex);
                        } else if (startOffset - varStart - 2 > operatorIndex && endOffset - varStart - 2 > operatorIndex) {
                            value = value.substring(operatorIndex + 1);
                        }

                        // If the value is a long literal, return null
                        try {
                            Long.parseLong(value);
                            return null;
                        } catch (NumberFormatException numberFormatException) {
                            // Do nothing
                        }
                    }
                }

                return value;
            }
            index = text.indexOf("${", index);
            if (startOffset == endOffset && startOffset == varEnd + 1 && (index == -1 || startOffset < index)) {
                // The offset is just after a closing '}' but not at the start of
                // another variable so go with the current variable.
                return text.substring(varStart + 2, varEnd);
            }
        }
        return null;
    }

    /**
     * If this is a variable reference, return the variable name. Otherwise
     * return null.
     */
    public static String getVariableName(String text) {
        if (text.startsWith("${") && text.endsWith("}")) {
            return text.substring(2, text.length() - 1);
        }
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.expression.Expression;

/**
 * Container for configuration variables.
 */
@SuppressWarnings("restriction")
public class ConfigVars {

    // Regular expressions for computing types
    private static final String DURATION_EXPR = "(\\d+)(\\D+)";
    private static final Pattern durationPattern = Pattern.compile(DURATION_EXPR);
    private static final Set<String> durationUnits;
    private static final Set<String> englishDurationUnits;

    static {
        Set<String> set = new HashSet<String>(5);
        set.add(Messages.durationDayAbbreviation);
        set.add(Messages.durationHourAbbreviation);
        set.add(Messages.durationMinuteAbbreviation);
        set.add(Messages.durationSecondAbbreviation);
        set.add(Messages.durationMillisecondAbbreviation);
        durationUnits = Collections.unmodifiableSet(set);

        set = new HashSet<String>(5);
        set.add("d");
        set.add("h");
        set.add("m");
        set.add("s");
        set.add("ms");
        englishDurationUnits = Collections.unmodifiableSet(set);
    }

    // Predefined variables
    public static final String WLP_INSTALL_DIR = "wlp.install.dir";
    public static final String WLP_USER_DIR = "wlp.user.dir";
    public static final String WLP_SERVER_NAME = "wlp.server.name";
    public static final String SHARED_APP_DIR = "shared.app.dir";
    public static final String SHARED_CONFIG_DIR = "shared.config.dir";
    public static final String SHARED_RESOURCE_DIR = "shared.resource.dir";
    public static final String SERVER_CONFIG_DIR = "server.config.dir";
    public static final String SERVER_OUTPUT_DIR = "server.output.dir";
    public static final String USR_EXTENSION_DIR = "usr.extension.dir";

    public enum Type {
        BOOLEAN("boolean"),
        SHORT("short"),
        INT("int"),
        LONG("long"),
        LOCATION("string"),
        DURATION("duration"),
        STRING("string"),
        TOKEN("token");

        private final String name;

        private Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Type STRING_TYPE = Type.STRING;
    public static final Type LOCATION_TYPE = Type.LOCATION;

    public static final EnumSet<Type> BOOLEAN_TYPES = EnumSet.of(Type.BOOLEAN);
    public static final EnumSet<Type> SHORT_TYPES = EnumSet.of(Type.SHORT);
    public static final EnumSet<Type> INT_TYPES = EnumSet.of(Type.INT, Type.SHORT);
    public static final EnumSet<Type> LONG_TYPES = EnumSet.of(Type.LONG, Type.INT, Type.SHORT);
    public static final EnumSet<Type> LOCATION_TYPES = EnumSet.allOf(Type.class);
    public static final EnumSet<Type> DURATION_TYPES = EnumSet.of(Type.DURATION, Type.LONG, Type.INT, Type.SHORT);
    public static final EnumSet<Type> REFERENCE_TYPES = EnumSet.allOf(Type.class);
    public static final EnumSet<Type> STRING_TYPES = EnumSet.allOf(Type.class);

    public static final HashMap<String, EnumSet<Type>> TYPE_MAP = new HashMap<String, EnumSet<Type>>();
    public static final HashMap<EnumSet<Type>, String> TYPE_NAME_MAP = new HashMap<EnumSet<Type>, String>();
    public static final HashSet<String> PREDEFINED_VARS = new HashSet<String>();

    public static final String LIST_START = "list(";
    public static final String LIST_END = ")";

    static {
        TYPE_MAP.put(Constants.XSD_BOOLEAN_TYPE, BOOLEAN_TYPES);
        TYPE_MAP.put(Constants.BOOLEAN_TYPE, BOOLEAN_TYPES);
        TYPE_MAP.put(Constants.XSD_SHORT_TYPE, SHORT_TYPES);
        TYPE_MAP.put(Constants.SHORT_TYPE, SHORT_TYPES);
        TYPE_MAP.put(Constants.XSD_INT_TYPE, INT_TYPES);
        TYPE_MAP.put(Constants.INT_TYPE, INT_TYPES);
        TYPE_MAP.put(Constants.XSD_LONG_TYPE, LONG_TYPES);
        TYPE_MAP.put(Constants.LONG_TYPE, LONG_TYPES);
        TYPE_MAP.put(Constants.DURATION_TYPE, DURATION_TYPES);
        TYPE_MAP.put(Constants.MINUTE_DURATION_TYPE, DURATION_TYPES);
        TYPE_MAP.put(Constants.SECOND_DURATION_TYPE, DURATION_TYPES);
        TYPE_MAP.put(Constants.LOCATION_TYPE, STRING_TYPES);
        TYPE_MAP.put(Constants.PID_TYPE, REFERENCE_TYPES);
        TYPE_MAP.put(Constants.PID_LIST_TYPE, REFERENCE_TYPES);
        TYPE_MAP.put(Constants.XSD_STRING_TYPE, STRING_TYPES);
        TYPE_MAP.put(Constants.XSD_TOKEN_TYPE, STRING_TYPES);

        TYPE_NAME_MAP.put(BOOLEAN_TYPES, "boolean");
        TYPE_NAME_MAP.put(SHORT_TYPES, "short");
        TYPE_NAME_MAP.put(INT_TYPES, "int");
        TYPE_NAME_MAP.put(LONG_TYPES, "long");
        TYPE_NAME_MAP.put(LOCATION_TYPES, "string");
        TYPE_NAME_MAP.put(DURATION_TYPES, "duration");
        TYPE_NAME_MAP.put(REFERENCE_TYPES, "string");
        TYPE_NAME_MAP.put(STRING_TYPES, "string");

        PREDEFINED_VARS.add(WLP_INSTALL_DIR);
        PREDEFINED_VARS.add(WLP_USER_DIR);
        PREDEFINED_VARS.add(WLP_SERVER_NAME);
        PREDEFINED_VARS.add(SHARED_APP_DIR);
        PREDEFINED_VARS.add(SHARED_CONFIG_DIR);
        PREDEFINED_VARS.add(SHARED_RESOURCE_DIR);
        PREDEFINED_VARS.add(SERVER_CONFIG_DIR);
        PREDEFINED_VARS.add(SERVER_OUTPUT_DIR);
        PREDEFINED_VARS.add(USR_EXTENSION_DIR);
    }

    // NOTE: use the get methods rather than accessing these maps directly
    // as get methods may be overridden in derived classes
    private final HashMap<String, String> varValues = new HashMap<String, String>();
    private final HashMap<String, Type> varTypes = new HashMap<String, Type>();
    private final HashMap<String, DocumentLocation> varLocations = new HashMap<String, DocumentLocation>();
    private final ConfigVarsContext context = new ConfigVarsContext();

    public boolean isGlobalScope() {
        return true;
    }

    public void startContext() {
        context.start();
    }

    public void endContext() {
        context.end();
    }

    public void add(String name, String value, DocumentLocation location) {
        add(name, value, null, location);
    }

    public void add(String name, String value, Type type) {
        add(name, value, type, null);
    }

    public void add(String name, String value, Type type, DocumentLocation location) {
        final boolean needToResolveValue = ConfigVarsUtils.containsReference(value);
        String resolvedValue;

        if (needToResolveValue) {
            if (context.isActive()) {
                context.addUnresolved(name, value, type);
                return;
            }
            resolvedValue = resolve(value);
        } else {
            resolvedValue = value;
        }

        addResolved(name, resolvedValue, type, location);
    }

    public void addResolved(String name, String value, Type type, DocumentLocation location) {
        if (location != null) {
            varLocations.put(name, location);
        }

        // Compute the type, if necessary
        final Type resolvedType = (type == null) ? computeType(value) : type;

        // Add resolved value and type to the map
        addResolved(name, value, resolvedType);
    }

    protected void addResolved(String name, String value, Type type) {
        varValues.put(name, value);
        varTypes.put(name, type);
    }

    public String getValue(String name) {
        return varValues.get(name);
    }

    public Type getType(String name) {
        return varTypes.get(name);
    }

    public boolean isDefined(String name) {
        return varValues.containsKey(name);
    }

    // Get the type name for the given type.  If type is null,
    // string type is assumed.
    public String getTypeName(CMDataType type) {
        EnumSet<Type> typeSet = getTypeSet(type);
        return TYPE_NAME_MAP.get(typeSet);
    }

    public String getTypeName(String type) {
        EnumSet<Type> typeSet = getTypeSet(type);
        return TYPE_NAME_MAP.get(typeSet);
    }

    public DocumentLocation getDocumentLocation(String name) {
        return varLocations.get(name);
    }

    // Get the variables that are valid for the given type set.
    public List<String> getVars(EnumSet<Type> types, boolean includeUnresolvedPredefined) {
        return getScopedVars(types, includeUnresolvedPredefined, false);
    }

    // Get sorted list of variables that are valid for the given type set.
    public List<String> getSortedVars(EnumSet<Type> types, boolean includeUnresolvedPredefined) {
        List<String> vars = getVars(types, includeUnresolvedPredefined);
        Collections.sort(vars);
        return vars;
    }

    // Get the variables that are valid for the given type.
    public List<String> getVars(CMDataType type, boolean includeUnresolvedPredefined) {
        return getVars(getTypeSet(type), includeUnresolvedPredefined);
    }

    // Get the variables that are valid for the given type name.
    public List<String> getVars(String type, boolean includeUnresolvedPredefined) {
        return getVars(getTypeSet(type), includeUnresolvedPredefined);
    }

    // Get sorted list of variables that are valid for the given type.
    public List<String> getSortedVars(String type, boolean includeUnresolvedPredefined) {
        return getSortedVars(getTypeSet(type), includeUnresolvedPredefined);
    }

    // Get the global variables (same as scoped variables for global scope)
    public List<String> getGlobalVars(EnumSet<Type> types, boolean includeUnresolvedPredefined, boolean sort) {
        return getScopedVars(types, includeUnresolvedPredefined, sort);
    }

    // Get the global variables - convenience method takes a CMDataType
    public List<String> getGlobalVars(CMDataType type, boolean includeUnresolvedPredefined, boolean sort) {
        return getGlobalVars(getTypeSet(type), includeUnresolvedPredefined, sort);
    }

    // Get the global variables - convenience method takes a type name
    public List<String> getGlobalVars(String type, boolean includeUnresolvedPredefined, boolean sort) {
        return getGlobalVars(getTypeSet(type), includeUnresolvedPredefined, sort);
    }

    // Get the scoped variables that are valid for the given type.
    public List<String> getScopedVars(EnumSet<Type> types, boolean includeUnresolvedPredefined, boolean sort) {
        ArrayList<String> vars = new ArrayList<String>();
        Iterator<Map.Entry<String, Type>> entries = varTypes.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Type> entry = entries.next();
            if ((types.contains(entry.getValue()))) {
                vars.add(entry.getKey());
            }
        }
        if (isGlobalScope() && includeUnresolvedPredefined && types.contains(Type.LOCATION)) {
            vars.addAll(getUnresolvedPredefinedVars());
        }
        if (sort)
            Collections.sort(vars);
        return vars;
    }

    // Get the scoped variables that are valid for the given type.
    public List<String> getScopedVars(CMDataType type, boolean includeUnresolvedPredefined, boolean sort) {
        return getScopedVars(getTypeSet(type), includeUnresolvedPredefined, sort);
    }

    public List<String> getScopedVars(String type, boolean includeUnresolvedPredefined, boolean sort) {
        return getScopedVars(getTypeSet(type), includeUnresolvedPredefined, sort);
    }

    // Get the type set for the given type.  If the type is null,
    // string type is assumed.
    public static EnumSet<Type> getTypeSet(CMDataType type) {
        EnumSet<Type> types = null;
        if (type != null) {
            String typeName = type.getDataTypeName();
            types = TYPE_MAP.get(typeName);
        }
        if (types == null) {
            types = STRING_TYPES;
        }
        return types;
    }

    // Get the type set for the given type name.  If the type is
    // null, string type is assumed.
    public static EnumSet<Type> getTypeSet(String typeName) {
        EnumSet<Type> types = TYPE_MAP.get(typeName);
        if (types == null) {
            types = STRING_TYPES;
        }
        return types;
    }

    // Check if the type is atomic
    public static boolean isAtomic(CMDataType type) {
        return TYPE_MAP.containsKey(type.getDataTypeName());
    }

    // Check if the variable is a predefined variable
    public boolean isPredefinedVar(String name) {
        return PREDEFINED_VARS.contains(name);
    }

    // Get the unresolved predefined variables
    private List<String> getUnresolvedPredefinedVars() {
        List<String> list = new ArrayList<String>();
        for (String name : PREDEFINED_VARS) {
            if (!isDefined(name)) {
                list.add(name);
            }
        }
        return list;
    }

    protected Type computeType(String value) {
        // Try to figure out the type
        try {
            long l = Long.parseLong(value);
            if (l <= Short.MAX_VALUE && l >= Short.MIN_VALUE) {
                return Type.SHORT;
            } else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
                return Type.INT;
            } else {
                return Type.LONG;
            }
        } catch (NumberFormatException e) {
            // Ignore
        }

        if (isBooleanType(value)) {
            return Type.BOOLEAN;
        }

        // Test for duration
        if (isDurationType(value)) {
            return Type.DURATION;
        }

        return Type.STRING;
    }

    private boolean isBooleanType(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    // Check the value to see if it is a duration type.  First try the
    // units for the current locale, if that fails try the English units.
    public static boolean isDurationType(String value) {
        if (isDurationType(value, durationUnits)) {
            return true;
        }
        return isDurationType(value, englishDurationUnits);
    }

    private static boolean isDurationType(String value, Set<String> durationUnits) {
        Matcher matcher = durationPattern.matcher(value);
        while (matcher.find()) {
            String unitStr = matcher.group(2);
            if (unitStr == null || !durationUnits.contains(unitStr)) {
                return false;
            }
            if (matcher.hitEnd()) {
                return true;
            }
        }
        return false;
    }

    // Resolve a value.  Currently assumes that variables
    // are defined before they are used.
    public String resolve(String value) {
        ResolvedValueInfo info = new ResolvedValueInfo();
        resolve(value, info);
        return info.getResolvedValue();
    }

    public void resolve(String value, ResolvedValueInfo info) {
        resolve(value, info, null);
    }

    public void resolve(String value, ResolvedValueInfo info, String expectedTypeName) {
        StringBuilder builder = new StringBuilder();
        int offset = 0;
        int start = value.indexOf("${");

        info.reset();
        while (start >= 0) {
            int end = value.indexOf("}", start);
            if (end >= 0) {
                builder.append(value, offset, start);
                String varName = value.substring(start + 2, end);
                int varOffset = start + 2;
                Expression expression = null;

                // Check for list
                if (varName.startsWith(LIST_START) && varName.endsWith(LIST_END)) {
                    varName = varName.substring(LIST_START.length(), varName.length() - 1);
                    varOffset = varOffset + LIST_START.length();
                } else {
                    expression = new Expression(varName);
                }

                // Check for expressions
                if (expression != null && expression.getOperator() != null) {

                    // Detect if there is a chained expression
                    Expression chainedExpression = new Expression(expression.getRightOperand());
                    if (chainedExpression.getOperator() != null) {
                        info.setInvalidExpression(true);
                    } else {
                        String expressionResult = calculateExpression(expression, info);
                        if (expressionResult != null) {
                            builder.append(expressionResult);
                        } else {
                            builder.append(varName);
                        }
                    }
                } else {
                    String varValue = getValue(varName);
                    if (varValue != null) {
                        builder.append(varValue);
                        if (expectedTypeName != null) {
                            if (!checkType(getType(varName), expectedTypeName)) {
                                info.setTypeMismatch(true);
                            }
                        }
                    } else {
                        info.addUndefinedReference(varName, varOffset);
                        builder.append(value, start, end + 1);
                    }
                }

            } else {
                break;
            }
            offset = end + 1;
            start = value.indexOf("${", offset);
        }
        if (offset < value.length()) {
            builder.append(value, offset, value.length());
        }
        info.setResolvedValue(builder.toString());
    }

    private String calculateExpression(Expression expression, ResolvedValueInfo info) {

        long leftOperandLongValue = 0;
        long rightOperandLongValue = 0;

        // Obtain left operand
        String leftOperand = expression.getLeftOperand();
        if (leftOperand.length() > 0) {
            if (!LONG_TYPES.contains(computeType(leftOperand))) {
                leftOperand = getValue(leftOperand);
            }
            if (leftOperand == null) {
                info.setExpressionLeftOperandError(ExpressionOperandError.UNDEFINED);
            } else {
                try {
                    leftOperandLongValue = Long.parseLong(leftOperand);
                } catch (NumberFormatException numberFormatException) {
                    info.setExpressionLeftOperandError(ExpressionOperandError.INVALID_VALUE);
                }
            }
        } else {
            info.setExpressionLeftOperandError(ExpressionOperandError.MISSING);
        }

        // Obtain right operand
        String rightOperand = expression.getRightOperand();
        if (rightOperand.length() > 0) {
            if (!LONG_TYPES.contains(computeType(rightOperand))) {
                rightOperand = getValue(rightOperand);
            }

            if (rightOperand == null) {
                info.setExpressionRightOperandError(ExpressionOperandError.UNDEFINED);
            } else {
                try {
                    rightOperandLongValue = Long.parseLong(rightOperand);
                } catch (NumberFormatException numberFormatException) {
                    info.setExpressionRightOperandError(ExpressionOperandError.INVALID_VALUE);
                }
            }
        } else {
            info.setExpressionRightOperandError(ExpressionOperandError.MISSING);
        }

        // If both operands were obtained, return calculated expression
        if (info.getExpressionLeftOperandError() == null && info.getExpressionRightOperandError() == null) {
            switch (expression.getOperator()) {
                case ADDITION:
                    return Long.toString(leftOperandLongValue + rightOperandLongValue);
                case SUBTRACTION:
                    return Long.toString(leftOperandLongValue - rightOperandLongValue);
                case MULTIPLICATION:
                    return Long.toString(leftOperandLongValue * rightOperandLongValue);
                case DIVISION:
                    return Long.toString(leftOperandLongValue / rightOperandLongValue);
            }
        }
        return null;
    }

    // Check if the value matches the expected type.
    public boolean checkType(String value, String expectedTypeName) {
        EnumSet<Type> types = getTypeSet(expectedTypeName);
        if (types.equals(STRING_TYPES)) {
            // String allows anything so don't bother computing the
            // type of the value.
            return true;
        }
        Type type = computeType(value);
        return checkType(type, expectedTypeName);
    }

    protected boolean checkType(Type actualType, String expectedTypeName) {
        EnumSet<Type> types = getTypeSet(expectedTypeName);
        return types.contains(actualType);
    }

    public void copyInto(ConfigVars vars) {
        vars.varValues.putAll(varValues);
        vars.varTypes.putAll(varTypes);
        vars.varLocations.putAll(varLocations);
    }

    @Override
    public String toString() {
        return "Values: " + varValues.toString() + "\n" + "Types: " + varTypes.toString();
    }

    protected class ConfigVarsContext {

        private final HashMap<String, String> unresolvedVarValues = new HashMap<String, String>();
        private final HashMap<String, Type> unresolvedVarTypes = new HashMap<String, Type>();
        private final HashMap<String, ArrayList<String>> unresolvedVarReferences = new HashMap<String, ArrayList<String>>();
        private final HashMap<String, Boolean> unresolvedVarFlags = new HashMap<String, Boolean>();
        private final Stack<String> varStack = new Stack<String>();
        private int activeCounter = 0;

        protected boolean isActive() {
            return activeCounter > 0;
        }

        protected void addUnresolved(String name, String value, Type type) {
            ArrayList<String> refs = getReferences(value);
            unresolvedVarValues.put(name, value);
            if (type != null) {
                unresolvedVarTypes.put(name, type);
            }
            unresolvedVarReferences.put(name, refs);
        }

        private ArrayList<String> getReferences(String value) {
            ArrayList<String> refs = new ArrayList<String>(2);
            int offset = 0;
            int start = value.indexOf("${");
            while (start >= 0) {
                int end = value.indexOf("}", start);
                if (end >= 0) {
                    refs.add(value.substring(start + 2, end));
                } else {
                    break;
                }
                offset = end + 1;
                start = value.indexOf("${", offset);
            }
            return refs;
        }

        protected void start() {
            // multiple activations
            if (++activeCounter > 1) {
                return;
            }

            // first activation
            if (unresolvedVarValues.size() > 0) {
                unresolvedVarValues.clear();
            }
            if (unresolvedVarTypes.size() > 0) {
                unresolvedVarTypes.clear();
            }
            if (unresolvedVarFlags.size() > 0) {
                unresolvedVarFlags.clear();
            }
            if (unresolvedVarReferences.size() > 0) {
                unresolvedVarReferences.clear();
            }
        }

        protected void end() {
            // If we are not active, there is nothing to do
            if (activeCounter == 0) {
                return;
            }

            // Resolve remaining references
            if (--activeCounter == 0) {
                Set<Map.Entry<String, String>> entries = unresolvedVarValues.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String name = entry.getKey();
                    if (!isResolved(name)) {
                        resolveAndAdd(name, entry.getValue(), unresolvedVarTypes.get(name));
                    }
                }
            }
        }

        private void resolveAndAdd(String name, String value, Type type) {
            varStack.push(name);
            final ArrayList<String> refs = unresolvedVarReferences.get(name);
            for (String varRef : refs) {
                final String varRefValue = unresolvedVarValues.get(varRef);
                if (varRefValue != null && !varStack.contains(varRef) && !isResolved(varRef)) {
                    resolveAndAdd(varRef, varRefValue, unresolvedVarTypes.get(varRef));
                }
            }

            final String resolvedValue = resolve(value);
            final Type resolvedType = (type == null) ? computeType(resolvedValue) : type;
            addResolved(name, resolvedValue, resolvedType);
            unresolvedVarFlags.put(name, Boolean.TRUE);
            varStack.pop();
        }

        private boolean isResolved(String name) {
            return Boolean.TRUE.equals(unresolvedVarFlags.get(name));
        }
    }

    public static enum ExpressionOperandError {

        MISSING,
        UNDEFINED,
        INVALID_VALUE;

    }

    public static class ResolvedValueInfo {

        private static int UNDEFINED_THRESHOLD = 4;

        private String resolvedValue;
        private boolean typeMismatch;
        private boolean invalidExpression;
        private ExpressionOperandError expressionLeftOperandError;
        private ExpressionOperandError expressionRightOperandError;
        private final ArrayList<UndefinedReference> undefinedList = new ArrayList<UndefinedReference>(2);

        protected void reset() {
            resolvedValue = null;
            typeMismatch = false;
            invalidExpression = false;
            expressionLeftOperandError = null;
            expressionRightOperandError = null;
            undefinedList.clear();
        }

        public String getResolvedValue() {
            return resolvedValue;
        }

        public boolean isFullyResolved() {
            return undefinedList.size() == 0;
        }

        public boolean isTypeMismatch() {
            return typeMismatch;
        }

        public boolean isInvalidExpression() {
            return invalidExpression;
        }

        public ExpressionOperandError getExpressionLeftOperandError() {
            return expressionLeftOperandError;
        }

        public ExpressionOperandError getExpressionRightOperandError() {
            return expressionRightOperandError;
        }

        public UndefinedReference[] getUndefinedReferences() {
            if (undefinedList.size() == 0) {
                return null;
            }

            final UndefinedReference[] refArray = new UndefinedReference[undefinedList.size()];
            undefinedList.toArray(refArray);
            return refArray;
        }

        protected void setResolvedValue(String value) {
            resolvedValue = value;
        }

        protected void setTypeMismatch(boolean value) {
            typeMismatch = value;
        }

        protected void setInvalidExpression(boolean invalidExpression) {
            this.invalidExpression = invalidExpression;
        }

        protected void setExpressionLeftOperandError(ExpressionOperandError expressionLeftOperandError) {
            this.expressionLeftOperandError = expressionLeftOperandError;
        }

        protected void setExpressionRightOperandError(ExpressionOperandError expressionRightOperandError) {
            this.expressionRightOperandError = expressionRightOperandError;
        }

        protected void addUndefinedReference(String name, int offset) {
            if (undefinedList.size() < UNDEFINED_THRESHOLD) {
                final UndefinedReference ref = new UndefinedReference(name, offset);
                undefinedList.add(ref);
            }
        }
    }

    public static class UndefinedReference {
        private final String name;
        private final int startOffset;

        protected UndefinedReference(String reference, int offset) {
            name = reference;
            startOffset = offset;
        }

        public String getReferenceName() {
            return name;
        }

        public int getReferenceOffset() {
            return startOffset;
        }
    }
}

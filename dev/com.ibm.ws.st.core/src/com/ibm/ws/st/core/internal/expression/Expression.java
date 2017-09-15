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
package com.ibm.ws.st.core.internal.expression;

public class Expression {

    public enum Operator {
        ADDITION('+'),
        SUBTRACTION('-'),
        MULTIPLICATION('*'),
        DIVISION('/');

        private final char symbol;

        private Operator(char symbol) {
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

    }

    private Operator operator;
    private String leftOperand;
    private String rightOperand;

    public Expression(String expression) {
        if (expression != null) {
            for (Operator operator : Operator.values()) {
                int index = expression.indexOf(operator.getSymbol());
                if (index != -1) {
                    this.operator = operator;
                    leftOperand = expression.substring(0, index);
                    rightOperand = expression.substring(index + 1);
                    break;
                }
            }
        }
    }

    public Operator getOperator() {
        return operator;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getRightOperand() {
        return rightOperand;
    }

}

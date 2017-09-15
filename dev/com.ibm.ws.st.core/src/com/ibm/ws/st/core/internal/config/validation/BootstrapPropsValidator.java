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
package com.ibm.ws.st.core.internal.config.validation;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.validation.ValidationResult;

import com.ibm.ws.st.core.internal.Messages;

/**
 * Validator for bootstrap.properties. The file is fully parsed, but currently only validates for
 * unescaped characters (unicode > 255).
 */
public class BootstrapPropsValidator extends AbstractTextFileParser {
    public BootstrapPropsValidator(IFile file) throws CoreException, UnsupportedEncodingException {
        super(file);
    }

    @Override
    protected void parseImpl(ValidationResult result) throws IOException {
        readChar();
        while (true) {
            if ('#' == nextChar) { // comment
                while (!isEOLChar(nextChar)) {
                    readChar();
                }
                readEOL();
            } else if ('!' == nextChar) { // comment
                while (!isEOLChar(nextChar)) {
                    readChar();
                }
                readEOL();
            } else if (isEOLChar(nextChar)) { // empty line
                readEOL();
            } else { // key=value, key:value, or key value
                StringBuilder sb = new StringBuilder();
                try {
                    while (!isEOLChar(nextChar)) {
                        sb.append(nextChar);
                        readChar();
                    }
                } catch (EOFException e) {
                    // catch EOF so we can check if last line contained whitespace
                }

                validateLine(sb, 0, result);
                readEOL();

                int len = sb.length();
                while (sb.charAt(len - 1) == '\\') {
                    sb.deleteCharAt(len - 1);
                    try {
                        while (!isEOLChar(nextChar)) {
                            sb.append(nextChar);
                            readChar();
                        }
                    } catch (EOFException e) {
                        // catch EOF so we can check if last line was valid
                    }

                    validateLine(sb, len - 1, result);
                    readEOL();
                }
            }
        }
    }

    protected void validateLine(StringBuilder sb, int st, ValidationResult result) {
        int len = sb.length();
        int start = -1;
        int bk = (len - st);
        for (int i = st; i < len; i++) {
            int ind = i - st;
            if (sb.codePointAt(i) > 255) {
                if (start == -1)
                    start = ind;
            } else if (start >= 0) {
                result.add(ConfigurationValidator.createMessage(file, Messages.invalidUnicode, line, charCount - bk + start, charCount - bk + ind));
                start = -1;
            }
        }
        if (start >= 0) {
            result.add(ConfigurationValidator.createMessage(file, Messages.invalidUnicode, line, charCount - bk + start, charCount - bk + (len - st)));
            start = -1;
        }
    }
}

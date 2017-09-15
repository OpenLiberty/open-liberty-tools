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

/**
 * Validator for server.env. Each line must be blank, a comment (#), or an entry. Entries must not start or end with whitespace.
 */
public class ServerEnvValidator extends AbstractTextFileParser {
    public ServerEnvValidator(IFile file) throws CoreException, UnsupportedEncodingException {
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
            } else if (isEOLChar(nextChar)) { // empty line
                readEOL();
            } else { // value
                StringBuilder sb = new StringBuilder();
                try {
                    while (!isEOLChar(nextChar)) {
                        sb.append(nextChar);
                        readChar();
                    }
                } catch (EOFException e) {
                    // catch EOF so we can check if last line contained whitespace
                }
                String s = sb.toString();
                String t = s.trim();
                if (t.length() == 0)
                    result.add(ConfigurationValidator.createInvalidWhitespaceMessage(file, line, charCount - s.length(), charCount));
                else {
                    int start = s.indexOf(t);
                    if (start > 0)
                        result.add(ConfigurationValidator.createInvalidWhitespaceMessage(file, line, charCount - s.length(), charCount - s.length() + start));

                    int end = start + t.length();
                    if (end < s.length())
                        result.add(ConfigurationValidator.createInvalidWhitespaceMessage(file, line, charCount - s.length() + end, charCount));
                }
                readEOL();
            }
        }
    }
}

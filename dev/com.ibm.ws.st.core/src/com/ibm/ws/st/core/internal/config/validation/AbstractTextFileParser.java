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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.ValidationResult;

import com.ibm.ws.st.core.internal.Trace;

/**
 * A simple abstract text parser.
 */
public abstract class AbstractTextFileParser {
    private static final char NL = '\n';
    private static final char CR = '\r';

    private final char[] buf = new char[256];
    private int num = -1;
    private int ind = 0;
    protected int line = 1;
    protected int charCount = -2;
    protected char nextChar;
    private final Reader r;
    protected final IFile file;

    public AbstractTextFileParser(IFile file) throws CoreException, UnsupportedEncodingException {
        this.file = file;
        this.r = new InputStreamReader(file.getContents(), file.getCharset());
    }

    protected static boolean isEOLChar(char c) {
        return NL == c || CR == c;
    }

    protected void readEOL() throws IOException {
        char c = nextChar;
        readChar();
        if (isEOLChar(nextChar) && c != nextChar)
            readChar();

        line++;
    }

    protected void readChar() throws IOException {
        charCount++;
        if (ind >= num) {
            num = r.read(buf);
            if (num < 0)
                throw new EOFException();
            ind = 0;
        }
        nextChar = buf[ind++];
    }

    /**
     * Subclasses must implement this method to parse the file's contents.
     * 
     * @param result a validation result to add messages (markers) to
     * @throws IOException if there is a problem parsing the file
     */
    protected abstract void parseImpl(ValidationResult result) throws IOException;

    public void parse(ValidationResult valResult, IProgressMonitor monitor) throws IOException {
        try {
            parseImpl(valResult);
        } catch (EOFException e) {
            // ignore
        } finally {
            close();
        }
    }

    private void close() {
        try {
            r.close();
        } catch (Exception e) {
            Trace.logError("Error validating file", e);
        }
    }
}

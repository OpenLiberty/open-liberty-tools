/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ReadOnlyFileStorage implements IStorage {
    private final File file;
    private InputStream stream = null;

    public ReadOnlyFileStorage(File input) {
        this.file = input;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stream = new ByteArrayInputStream(bos.toByteArray());
        }
    }

    @SuppressWarnings("rawtypes")
	public Object getAdapter(Class arg0) {
        return null;
    }
    
    public InputStream getContents() throws CoreException {
        return stream;
    }

    public IPath getFullPath() {
        return new Path(file.getAbsolutePath());
    }

    public String getName() {
        return file.getName();
    }

    public boolean isReadOnly() {
        return true;
    }
}

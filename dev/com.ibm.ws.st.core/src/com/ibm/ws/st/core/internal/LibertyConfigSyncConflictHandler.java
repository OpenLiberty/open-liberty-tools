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
package com.ibm.ws.st.core.internal;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.jmx.JMXConnection;

/**
 * This class is hooked into the libertyConfigConflictHandler extension point.
 * Upon detection of a conflict scenario the extensions will be used to find a
 * handler that will decide how to proceed. Possible ways to proceed include
 * overwriting local changes with the remote files or overwriting remote changes with
 * local files or merging the conflicts and updating both local and remote files.
 */
public abstract class LibertyConfigSyncConflictHandler {

    public enum Resolution {
        OVERWRITE_LOCAL, OVERWRITE_REMOTE, MERGE, CANCEL
    }

    public abstract Resolution handleConflict(List<Pair> comparableFiles, IPath tempDirectory, JMXConnection jmxConnection, String remoteConfigRoot) throws Exception;

    public static class Pair {

        private final String left;
        private final String right;

        public Pair(String left, String right) {
            this.left = left;
            this.right = right;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        @Override
        public int hashCode() {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof Pair))
                return false;
            Pair pairo = (Pair) o;
            return this.left.equals(pairo.getLeft()) &&
                   this.right.equals(pairo.getRight());
        }

    }

}
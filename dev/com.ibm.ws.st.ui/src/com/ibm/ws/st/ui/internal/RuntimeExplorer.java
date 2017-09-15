/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

public class RuntimeExplorer {
    public enum NodeType {
        SERVERS, SHARED_CONFIGURATIONS, SHARED_APPLICATIONS;
    }

    public static class Node {
        private final NodeType type;
        private final UserDirectory userDir;

        public Node(UserDirectory userDir, NodeType type) {
            this.userDir = userDir;
            this.type = type;
        }

        public NodeType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node))
                return false;

            Node n = (Node) o;
            return n.userDir.equals(userDir) && n.type.equals(type);
        }

        @Override
        public int hashCode() {
            int hash = type.hashCode();
            if (userDir != null)
                hash += userDir.hashCode();
            return hash;
        }

        public Object getParent() {
            return userDir;
        }

        public UserDirectory getUserDirectory() {
            return userDir;
        }

        public IRuntime getRuntime() {
            return userDir.getWebSphereRuntime().getRuntime();
        }

        public WebSphereRuntime getWebSphereRuntime() {
            return userDir.getWebSphereRuntime();
        }

        public String getName() {
            if (type == NodeType.SERVERS)
                return Messages.runtimeServers;
            if (type == NodeType.SHARED_CONFIGURATIONS)
                return Messages.runtimeSharedConfigurations;
            return Messages.runtimeSharedApplications;
        }

        public Object[] getChildren() {
            if (type == NodeType.SERVERS) {
                WebSphereRuntime wr = userDir.getWebSphereRuntime();
                return wr.getWebSphereServerInfos(userDir).toArray();
            } else if (type == NodeType.SHARED_CONFIGURATIONS)
                return userDir.getSharedConfiguration();

            return null;
        }
    }
}

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
package com.ibm.ws.st.core.internal.jmx;

import com.ibm.ws.st.core.internal.WebSphereServer;

public class JMXConnectionInfo {

    private final String host;
    private final String port;
    private final String user;
    private final String password;

    public JMXConnectionInfo(WebSphereServer wsServer) {
        host = wsServer.getServer().getHost();
        port = wsServer.getServerSecurePort();
        user = wsServer.getServerUserName();
        password = wsServer.getServerPassword();
    }

    public boolean equals(JMXConnectionInfo jmxInfo) {
        if (jmxInfo == null) {
            return false;
        }
        return (isEqualString(host, jmxInfo.host)
                && isEqualString(port, jmxInfo.port)
                && isEqualString(user, jmxInfo.user)
                && isEqualString(password, jmxInfo.password));
    }

    private boolean isEqualString(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }
        return (str1 != null && str1.equals(str2));
    }

}

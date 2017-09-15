/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ibm.ws.st.core.tests.jee.contextRoot.JEEContextRootests;
import com.ibm.ws.st.core.tests.profiling.AllProfilingTests;
import com.ibm.ws.st.core.tests.remote.AllRemoteTests;

// When running these tests, the following properties must be set:
//    -Dwas.runtime.liberty=<runtime location>
// And for the remote tests:
//    -Dliberty.remote.hostname=<hostname of the remote machine>
//    -Dliberty.remote.username=<configured user name from a user registry>
//    -Dliberty.remote.password=<configured password from a user registry>
//    -Dliberty.remote.https.port=<https port default is 9443>
//    -Dliberty.remote.remoteServerStartEnabled=true
//    -Dliberty.remote.installPath=<location of the runtime installation eg. C:/liberty/8559/wlp>
//    -Dliberty.remote.configPath=<location of the runtime server.xml eg. C:/liberty/usr/servers/defaultServerRemote>
//    -Dliberty.remote.oslogon.user=<OS login username eg. Administrator or root>
//    -Dliberty.remote.oslogon.pass=<OS login password>
//    -Dliberty.remote.sshlogon.user=user
//    -Dliberty.remote.sshlogon.password=password
//    -Dliberty.remote.sshlogon.keyfile=key
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      JEEContextRootests.class,
                      AllProfilingTests.class,
                      AllRemoteTests.class })
public class AllTests4 {
    // intentionally empty
}
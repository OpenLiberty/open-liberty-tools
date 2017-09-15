/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.docker;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ibm.ws.st.core.tests.DockerUtilitiesTest;
import com.ibm.ws.st.core.tests.jee.JEEPublishEarNoDD;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarDD;

// When running these tests, the following property must be set:
//    -Dwas.runtime.liberty=<runtime location>
//    -Dliberty.servertype=LibertyDocker
// If these tests are being run for remote docker then the following
// additional properties need to be set:
//    -Dliberty.remote.hostname=<remote host name>
//    -Dliberty.remote.osname=<os name: mac or linux>
//    -Dliberty.remote.oslogon.user=<os login id>
//    -Dliberty.remote.oslogon.pass=<os login password>
//    -Dliberty.remote.logonMethod=logonMethodOS (optional as this is the default)
// To use SSH logon for remote instead of OS logon use the:
//    -Dliberty.remote.hostname=<remote host name>
//    -Dliberty.remote.osname=<os name: mac or linux>
//    -Dliberty.remote.sshlogon.user=<ssh login id>
//    -Dliberty.remote.sshlogon.password=<ssh passphrase>
//    -Dliberty.remote.sshlogon.keyfile=<location of key file>
//    -Dliberty.remote.logonMethod=logonMethodSSH
@RunWith(Suite.class)
@Suite.SuiteClasses({
                      DockerServerTest.class,
                      JEEPublishWarDD.class,
                      JEEPublishEarNoDD.class,
                      DockerUtilitiesTest.class,
})
public class AllDockerSmokeTests {
// nothing to see here
}

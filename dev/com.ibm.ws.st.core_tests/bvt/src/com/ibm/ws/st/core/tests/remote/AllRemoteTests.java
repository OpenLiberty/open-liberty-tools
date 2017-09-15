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
package com.ibm.ws.st.core.tests.remote;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

// When running these tests, the following properties must be set:
//    -Dwas.runtime.liberty=<runtime location>
//    -Dliberty.remote.hostname=<remote host name>
//    -Dliberty.remote.osname=<windows, mac, linux, other>
//    -Dliberty.remote.username=<user>
//    -Dliberty.remote.password=<password>
//    -Dliberty.remote.https.port=<https port for remote server>
@RunWith(Suite.class)
@Suite.SuiteClasses({
                      RemoteServerTest.class,
                      RemoteServerStartDisabledTest.class,
                      RemoteServerFileUploadTest.class
})
public class AllRemoteTests {
// nothing to see here
}

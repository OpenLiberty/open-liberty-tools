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

import com.ibm.ws.st.core.tests.jee.JEEAddRemoveWebTest;
import com.ibm.ws.st.core.tests.jee.JEENoDD_DefaultTest;
import com.ibm.ws.st.core.tests.jee.JEEPublishEarDD;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarDD;
import com.ibm.ws.st.core.tests.jee.WebFragment_DDEARTest;
import com.ibm.ws.st.core.tests.jee.WebFragment_DDWEBTest;

// When running these tests, the following properties must be set:
//    -Dwas.runtime.liberty=<runtime location>
//    -Dliberty.servertype=LibertyDocker
//    -Dliberty.loosecfg=true
// Optional
//    -Dsafe.publish.timeout=180
//    -Dliberty.docker.container=<containerName>
//    -Dliberty.docker.image=<imageName> if it doesn't match websphere-liberty
//    -Dconsole.line.tracker.test - to turn on console line tracker to verify mapped links
@RunWith(Suite.class)
@Suite.SuiteClasses({
                      // EAR containing WAR with Web Fragment
                      WebFragment_DDEARTest.class,
                      // WAR with Web Fragment
                      WebFragment_DDWEBTest.class,

                      JEEAddRemoveWebTest.class,
                      JEENoDD_DefaultTest.class,

                      // WAR and EAR
                      JEEPublishWarDD.class,
                      JEEPublishEarDD.class
})
/**
 * A Test Suite created specifically for Liberty Docker with Loose Config enabled and the console tracker test option turned on.
 * This suite can be run per release FVT if desired, and the regular, console tracker-disabled, loose config tests can be run more regularly.
 */
public class AllDockerTests_LooseCfg_ConsoleLineTracker {
// nothing to see here
}

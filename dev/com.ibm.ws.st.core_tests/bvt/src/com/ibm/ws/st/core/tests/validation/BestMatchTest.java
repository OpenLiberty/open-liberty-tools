/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.ibm.ws.st.core.internal.config.validation.Matcher;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Best match tests", isStable = true)
public class BestMatchTest {

    private final ArrayList<String> choiceList = new ArrayList<String>();
    private final ArrayList<String> excludeList = new ArrayList<String>();
    private final Matcher matcher = new Matcher();

    @Test
    public void testBestMatch1() {
        fillChoiceList(choiceList, "cookieGroup", "cookieStore", "cookieChain");
        String bestMatch = matcher.getBestMatch(choiceList, "cookieMaze", null);
        assertNull("Best match for 'cookieMaze' - " + bestMatch, bestMatch);

        bestMatch = matcher.getBestMatch(choiceList, "cookieStar", null);
        assertNotNull("Best match for 'cookieStar' returned null", bestMatch);
        assertTrue("Best match for 'cookieStar' - " + bestMatch, "cookieStore".equals(bestMatch));
    }

    @Test
    public void testBestMatch2() {
        fillChoiceList(choiceList, "cookieDomain", "cookieSecure", "cookieFormat");
        String bestMatch = matcher.getBestMatch(choiceList, "cookieEntry", null);
        assertNull("Best match for 'cookieEntry' - " + bestMatch, bestMatch);
    }

    @Test
    public void testBestMatch3() {
        fillChoiceList(choiceList, "cookieCart", "cookieData", "cookieFat", "cookieFare");
        String bestMatch = matcher.getBestMatch(choiceList, "cookieHat", null);
        assertNotNull("Best match for 'cookieHat' returned null", bestMatch);
        assertTrue("Best match for 'cookieHat' - " + bestMatch, "cookieCart,cookieData,cookieFat".equals(bestMatch));
    }

    @Test
    public void testBestMatch4() {
        fillChoiceList(choiceList, "cookieCat", "cookieFat", "cookieMat");
        fillChoiceList(excludeList, "cookieFat");
        String bestMatch = matcher.getBestMatch(choiceList, "cookieBat", excludeList);
        assertNotNull("Best match for 'cookieBat' returned null", bestMatch);
        assertTrue("Best match for 'cookieMart' - " + bestMatch, "cookieCat,cookieMat".equals(bestMatch));
    }

    @Test
    public void testBestMatch5() {
        fillChoiceList(choiceList, "cookieDomain", "cookieSecure", "cookieName",
                                   "cookieData", "cookieFile", "cookieSize",
                                   "cookiePath", "cookieFormat", "cookieHistory",
                                   "cookieJar", "cookieBar", "cookiePortNumber",
                                   "cookieStar", "cookiePart", "cookieFrame",
                                   "cookieGroup");
        String bestMatch = matcher.getBestMatch(choiceList, "bookiePatch", null);
        assertNotNull("Best match for 'bookiePatch' returned null", bestMatch);
        assertTrue("Best match for 'bookiePatch' - " + bestMatch, "cookiePath".equals(bestMatch));
    }

    @Test
    public void testBestMatch6() {
        fillChoiceList(choiceList, "serverCookie", "serverCache", "serverFilter", "serverPath");
        String bestMatch = matcher.getBestMatch(choiceList, "driverCopier", null);
        assertNull("Best match for 'driverCopier' returned - " + bestMatch, bestMatch);

        // Use a lower matching percentage (default is 70%)
        // se[rverCo]ok[ie] <==> d[r]i[verCo]p[ie]r
        bestMatch = matcher.getBestMatch(choiceList, "driverCopier", null, 60);
        assertNotNull("Best match for 'driverCopier' returned null", bestMatch);
        assertTrue("Best match for 'driverCopier' - " + bestMatch, "serverCookie".equals(bestMatch));
    }

    @Test
    public void testBestMatch7() {
        fillChoiceList(choiceList, "groupFilter", "userFilter", "fileFilter",
                                   "secureFilter", "customFilter");
        String bestMatch = matcher.getBestMatch(choiceList, "activeFilter", null);
        assertNull("Best match for 'activeFilter' returned - " + bestMatch, bestMatch);

        bestMatch = matcher.getBestMatch(choiceList, "obscureFile", null);
        assertNotNull("Best match for 'obscureFile' returned null", bestMatch);
        assertTrue("Best match for 'obscureFile' - " + bestMatch, "secureFilter".equals(bestMatch));
    }

    @Test
    public void testBestMatch8() {
        fillChoiceList(choiceList, "packageCache", "packageError", "packageFlush",
                                   "dateFormat", "profilePattern", "packageLibrary",
                                   "serverName");
        String bestMatch = matcher.getBestMatch(choiceList, "pickingApache", null);
        assertNull("Best match for 'pickingApache' returned - " + bestMatch, bestMatch);

        bestMatch = matcher.getBestMatch(choiceList, "plateFrame", null);
        assertNull("Best match for 'plateFrame' returned - " + bestMatch, bestMatch);

        bestMatch = matcher.getBestMatch(choiceList, "packageCrush", null);
        assertNotNull("Best match for 'pacakgeCrush' returned null", bestMatch);
        assertTrue("Best match for 'packageCrush' - " + bestMatch, "packageFlush".equals(bestMatch));

        bestMatch = matcher.getBestMatch(choiceList, "driverFame", null);
        assertNotNull("Best match for 'driverFame' returned null", bestMatch);
        assertTrue("Best match for 'driverFame' - " + bestMatch, "serverName".equals(bestMatch));
    }

    private void fillChoiceList(ArrayList<String> list, String... strings) {
        list.clear();
        for (String str : strings) {
            list.add(str);
        }
    }
}
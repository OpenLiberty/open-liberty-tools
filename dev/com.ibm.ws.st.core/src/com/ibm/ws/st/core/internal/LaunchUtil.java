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
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.osgi.util.NLS;

public class LaunchUtil {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy K:mm:ss a");

    private static final String[] JAVA_EXECUTABLES = { "javaw", "javaw.exe", "java", "java.exe", "j9w", "j9w.exe", "j9", "j9.exe" };

    // launching/process constants
    public static final String VM_QUICKSTART = "-Xquickstart";

    private static String errorPage = null;

    private LaunchUtil() {
        // utility class, cannot instantiate
    }

    public static void setErrorPage(String newErrorPage) {
        errorPage = newErrorPage;
    }

    public static String getErrorPage() {
        return errorPage;
    }

    public static String getProcessLabel(String command) {
        return NLS.bind(Messages.processLabel, new Object[] { command, sdf.format(new Date()) });
    }

    public static String getProcessLabelAttr(String s1, String s2) {
        return NLS.bind(Messages.processLabelAttr, new Object[] { s1, s2, sdf.format(new Date()) });
    }

    /**
     * Returns the location that JAVA_HOME should be set to: the root vm install location
     * if it contains the java executable, or the /jre folder if it exists and contains the
     * executable. If JAVA_HOME can't be determined, default to vmInstallLocation.
     * 
     * @param vmInstallLocation the vm install location
     * @return the location of JAVA_HOME
     */
    public static File getJavaHome(File vmInstallLocation) {
        final String BIN = "bin" + File.separatorChar;
        for (int i = 0; i < JAVA_EXECUTABLES.length; i++) {
            File javaFile = new File(vmInstallLocation, BIN + JAVA_EXECUTABLES[i]);
            if (javaFile.isFile())
                return vmInstallLocation;
        }

        File jreLocation = new File(vmInstallLocation, "jre");
        if (!jreLocation.exists())
            return vmInstallLocation;
        for (int i = 0; i < JAVA_EXECUTABLES.length; i++) {
            File javaFile = new File(jreLocation, BIN + JAVA_EXECUTABLES[i]);
            if (javaFile.isFile())
                return jreLocation;
        }

        return vmInstallLocation;
    }

    public static boolean isIBMJRE(IVMInstall vm) {
        if (System.getProperty("os.name").toLowerCase().contains("mac"))
            return false;

        boolean isIBMJRE = false;
        if (vm instanceof AbstractVMInstall) {
            long time = 0;
            if (Trace.ENABLED_DETAILS)
                time = System.currentTimeMillis();
            LibraryLocation[] libs = ((AbstractVMInstall) vm).getLibraryLocations();

            if (libs == null)
                libs = vm.getVMInstallType().getDefaultLibraryLocations(vm.getInstallLocation());

            if (libs != null) {
                for (LibraryLocation lib : libs) {
                    String s = lib.getSystemLibraryPath().lastSegment();
                    if (s != null && s.startsWith("ibm")) {
                        isIBMJRE = true;
                        break;
                    }
                }
            }

            if (Trace.ENABLED_DETAILS)
                Trace.tracePerf("IBM JRE check", time);
        }
        return isIBMJRE;
    }
}
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

package com.ibm.ws.st.core.internal.security;

import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.security.LibertyX509CertPathValidatorResult.Status;

/**
 * The singleton instance of this class manages and drives extensions of
 * the "com.ibm.ws.st.core.libertyX509CertPathValidator" extension point.
 * 
 * @author cbrealey@ca.ibm.com
 */
public class LibertyX509CertPathValidatorRegistry {

    private static class Entry {
        public Entry() {}

        public IConfigurationElement element = null;
        public LibertyX509CertPathValidator validator = null;
    }

    private static LibertyX509CertPathValidatorRegistry instance_;
    private static final String XP_ID = "com.ibm.ws.st.core.libertyX509CertPathValidator"; //$NON-NLS-1$
    private static final String X_ATTR_CLASS = "class"; //$NON-NLS-1$
    private static final String X_ATTR_AUTONOMOUS = "autonomous"; //$NON-NLS-1$

    private List<Entry> autonomousValidators_ = null;
    private List<Entry> interactiveValidators_ = null;
    private int numberOfValidators = 0;

    private LibertyX509CertPathValidatorRegistry() {
        // Declaring the CTOR private.
    }

    /**
     * Returns the singleton instance of this class.
     * 
     * @return The singleton instance of this class.
     *         This method never returns null.
     */
    public static LibertyX509CertPathValidatorRegistry instance() {
        if (instance_ == null) {
            instance_ = new LibertyX509CertPathValidatorRegistry();
        }
        return instance_;
    }

    /**
     * Attempts to validate the given certificate path by calling the
     * validate method on each {@link LibertyX509CertPathValidator} plugged
     * into the libertyX509CertPathValidator extension point in turn until
     * one of the following conditions is met:
     * <ol>
     * <li>A validator accepts the certificate path as valid,
     * indicated by it returning a result with a status of
     * VALID_ONCE, VALID_FOR_SESSION or VALID_FOR_WORKSPACE.</li>
     * <li>A validator rejects the certificate path as invalid,
     * indicated by it returning a result with a status of
     * REJECTED.</li>
     * <li>There are no more validators, which occurs only when
     * all validators returns results with a status of
     * ABSTAINED.</li>
     * </ol>
     * Autonomous validators are called before interactive validators. Beyond
     * that, the order in which validators are called is not specified.
     * 
     * @param certPath The certificate path to validate.
     * @param index The index of the certificate in the path to validate,
     *            or -1 if there is no specific certificate in the path to validate.
     *            In this latter case, validators should check the root (last)
     *            certificate in the path; however, this is not a strict requirement.
     * @param shortMessage A short message summarizing the condition that
     *            caused this stage of certificate validation to be necessary, or
     *            null if there was no specific
     * @param longMessage
     * @param cause
     * @return An array of the result objects returned by the validators
     *         that were called, in the same number and order.
     * @throws CertificateException
     */
    public LibertyX509CertPathValidatorResult[] validate(CertPath certPath, int index, String message, Throwable cause) throws CertificateException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "certPath=[" + certPath + //$NON-NLS-1$
                                        "] index=[" + index + //$NON-NLS-1$
                                        "] message=[" + message + //$NON-NLS-1$
                                        "] cause=[" + cause + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        //
        // On first call, find all extensions and organize them
        // into two lists: autonomous and interactive.
        //
        if (autonomousValidators_ == null) {
            autonomousValidators_ = new LinkedList<Entry>();
            interactiveValidators_ = new LinkedList<Entry>();
            IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(XP_ID);
            for (IConfigurationElement configurationElement : configurationElements) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, "configurationElement class=[" + configurationElement.getAttribute(X_ATTR_CLASS) + //$NON-NLS-1$
                                                "] autonomous=[" + configurationElement.getAttribute(X_ATTR_AUTONOMOUS) + //$NON-NLS-1$
                                                "]"); //$NON-NLS-1$
                }
                Entry entry = new Entry();
                entry.element = configurationElement;
                String autonomous = configurationElement.getAttribute(X_ATTR_AUTONOMOUS);
                if (autonomous != null && "true".equals(autonomous)) { //$NON-NLS-1$
                    autonomousValidators_.add(entry);
                } else {
                    interactiveValidators_.add(entry);
                }
                numberOfValidators++;
            }
        }
        //
        // Run the autonomous validators first. If there are none,
        // or if they all abstained, move on to the interactive validators.
        //
        List<LibertyX509CertPathValidatorResult> results = new ArrayList<LibertyX509CertPathValidatorResult>(numberOfValidators);
        validate(autonomousValidators_, results, certPath, index, message, cause);
        if (results.size() == 0 || results.get(results.size() - 1).getStatus() == Status.ABSTAINED) {
            validate(interactiveValidators_, results, certPath, index, message, cause);
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.SECURITY, "results=[" + results + //$NON-NLS-1$
                                        "]"); //$NON-NLS-1$
        }
        return results.toArray(new LibertyX509CertPathValidatorResult[0]);
    }

    //
    // This private method runs the validators in the given list,
    // collecting their results, until one of them does not abstain.
    //
    private void validate(List<Entry> validators, List<LibertyX509CertPathValidatorResult> results, CertPath certPath, int index, String message, Throwable cause) {
        for (Entry entry : validators) {
            String className = entry.element.getAttribute(X_ATTR_CLASS);
            if (className != null) {
                try {
                    Object c = entry.element.createExecutableExtension(X_ATTR_CLASS);
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.SECURITY, "configurationElement object class=[" + c.getClass().getName() + //$NON-NLS-1$
                                                    "]"); //$NON-NLS-1$
                    }
                    if (c instanceof LibertyX509CertPathValidator) {
                        entry.validator = (LibertyX509CertPathValidator) c;
                        LibertyX509CertPathValidatorResult result = entry.validator.validate(certPath, index, message, cause);
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SECURITY, "result=[" + result + //$NON-NLS-1$
                                                        "]"); //$NON-NLS-1$
                        }
                        if (result != null) {
                            results.add(result);
                            if (result.getStatus() != Status.ABSTAINED) {
                                return;
                            }
                        }
                    } else {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SECURITY,
                                        (NLS.bind(Messages.X509_EXTENSION_HAS_WRONG_CLASS, new String[] { XP_ID, entry.element.getContributor().getName(),
                                                                                                         className, LibertyX509CertPathValidator.class.getName() })));
                        }
                    }
                } catch (CoreException e) {
                    //
                    // 	Failed to load the specified class.
                    // 	Log the cause, then move on to the next validator, if any.
                    //
                    Trace.logError(e.getMessage(), e);
                }
            } else {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SECURITY, NLS.bind(Messages.X509_EXTENSION_HAS_NO_CLASS, new String[] { XP_ID, entry.element.getContributor().getName(), X_ATTR_CLASS }));
                }
            }
        }
    }
}

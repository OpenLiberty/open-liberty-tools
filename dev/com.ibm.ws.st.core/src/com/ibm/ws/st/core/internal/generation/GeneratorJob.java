/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

public class GeneratorJob extends Job {
    // wait a maximum of 1 minute for the generation of either the schema or feature list
    static final int NUMBER_OF_ATTEMPTS = 120;

    static final GeneratorSchedulingRule GENERATOR_RULE = new GeneratorSchedulingRule();

    IMetadataGenerator metadataGen;
    final Helper[] helpers;
    final Map<Helper, IPath> helperPathMap = new HashMap<Helper, IPath>();

    static int numberOfAttempts = NUMBER_OF_ATTEMPTS;

    public interface Helper {
        public IPath getTarget(IMetadataGenerator metadataGen);
    }

    static public void generate(IMetadataGenerator metadataGen, Helper[] helpers, IJobChangeListener[] listeners) {
        GeneratorJob job = new GeneratorJob(metadataGen, helpers);
        if (listeners != null) {
            for (IJobChangeListener listener : listeners) {
                job.addJobChangeListener(listener);
            }
        }
        job.setPriority(Job.SHORT);
        job.setRule(GENERATOR_RULE);
        job.schedule();
    }

    // for testing purposes only
    static public int setNumberOfAttempts(int n) {
        int rc = numberOfAttempts;
        numberOfAttempts = n;
        return rc;
    }

    private GeneratorJob(IMetadataGenerator metadataGen, Helper[] helpers) {
        super(NLS.bind(Messages.jobRuntimeCache, metadataGen.getGeneratorId()));
        this.metadataGen = metadataGen;
        this.helpers = helpers;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        long time = System.currentTimeMillis();
        final int singleUnitTicks = 25;
        final IProgressMonitor monitor2 = monitor;
        int totalWork = helpers.length * singleUnitTicks;
        monitor.beginTask(NLS.bind(Messages.jobRuntimeCache, metadataGen.getGeneratorId()), totalWork);

        final IStatus[] finalStatus = { Status.OK_STATUS };
        List<Thread> threads = new ArrayList<Thread>();

        // Defect 183320: The temporary files should be associated and deleted within the scope of a single job
        // otherwise when multiple jobs are scheduled there can be timing issues that cause the job to fail.
        associateTempFiles();
        deleteTempFiles();

        // start the worker threads
        for (final Helper helper : helpers) {
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            Thread t = new Thread(helper.getClass().getName()) {
                @Override
                public void run() {
                    IStatus status = generate(helper, new SubProgressMonitor(monitor2, singleUnitTicks));
                    // Only update if the previous status was OK. For a cancel
                    // operation we already check the progress monitor.
                    if (finalStatus[0].isOK()) {
                        finalStatus[0] = status;
                    }
                }
            };
            threads.add(t);
            t.start();
        }
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Generator Job: All metadata generation threads started");

        // wait for all threads to complete
        for (Thread t : threads) {
            try {
                t.join();
                Trace.trace(Trace.INFO, "Thread " + t.getName() + " completed.");
            } catch (InterruptedException e) {
                // do nothing
            } finally {
                monitor.worked(100 / helpers.length);
            }
        }

        if (monitor.isCanceled()) {
            deleteTempFiles();
            return Status.CANCEL_STATUS;
        }

        if (!finalStatus[0].isOK()) {
            deleteTempFiles();
            Trace.logError(NLS.bind(Messages.metadataGenerationFailedDetails, metadataGen.getGeneratorId()), finalStatus[0].getException());
        } else {
            finalStatus[0] = renameTempFiles();

            // delete any remaining temporary files, if there was failure during
            // the renaming process
            if (!finalStatus[0].isOK())
                deleteTempFiles();
        }

        monitor.done();
        if (Trace.ENABLED)
            Trace.tracePerf("Generator job: All metadata generation threads finished", time);

        return finalStatus[0];
    }

    protected IStatus generate(Helper helper, IProgressMonitor monitor) {
        String generatorId = metadataGen.getGeneratorId();
        IPath tempPath = helperPathMap.get(helper);

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        try {
            if (helper instanceof SchemaMetadata)
                metadataGen.generateSchema(tempPath.toOSString(), monitor, NUMBER_OF_ATTEMPTS);
            else if (metadataGen.supportsFeatureListGeneration()) {
                metadataGen.generateFeatureList(tempPath.toOSString(), monitor, NUMBER_OF_ATTEMPTS, ((AbstractFeatureListMetadata) helper).getCommandOptions());
            }
        } catch (CoreException ce) {
            Trace.logError("Generation job failed", ce);
            return ce.getStatus();
        } catch (Exception ce) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, generatorId), ce);
        }

        return Status.OK_STATUS;
    }

    private void associateTempFiles() {
        for (Helper helper : helpers) {
            IPath path = helper.getTarget(metadataGen);
            helperPathMap.put(helper, path.removeLastSegments(1).append(path.lastSegment() + ".tmp"));
        }
    }

    private void deleteTempFiles() {
        for (Helper helper : helpers) {
            IPath tempPath = helperPathMap.get(helper);
            File tempFile = tempPath.toFile();
            FileUtil.deleteFile(tempFile);
        }
    }

    protected IStatus renameTempFiles() {
        String generatorId = metadataGen.getGeneratorId();
        for (Helper helper : helpers) {
            if (helper instanceof SchemaMetadata || metadataGen.supportsFeatureListGeneration()) {
                IPath path = helper.getTarget(metadataGen);
                IPath tempPath = helperPathMap.get(helper);
                File tempFile = tempPath.toFile();
                if (tempFile.exists()) {
                    File toFile = path.toFile();
                    FileUtil.deleteFile(toFile);
                    if (!WebSphereRuntime.rename(tempFile, toFile)) {
                        Trace.logError("Failed to generate " + helper.getTarget(metadataGen).toOSString() + " because the temporary file could not be renamed", null);
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, generatorId));
                    }
                }
                // It's OK for feature list extension metadata if no tmp file was found since it's expected that ext metadata would not have a tmp file by default
                else if (!(helper instanceof FeatureListExtMetadata)) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, generatorId));
                }
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object family) {
        return Constants.JOB_FAMILY.equals(family);
    }

    private static class GeneratorSchedulingRule implements ISchedulingRule {

        GeneratorSchedulingRule() {
            // empty
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule == GENERATOR_RULE;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return rule == GENERATOR_RULE;
        }
    }
}
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
package com.ibm.ws.st.core.tests.schema;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.SchemaLocationProvider;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.generation.GeneratorJob;
import com.ibm.ws.st.core.internal.generation.Metadata;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test schema utilities", isStable = true)
@RunWith(AllTests.class)
public class SchemaUtilTest extends ToolsTestBase {
    protected static final String RUNTIME_NAME = SchemaUtilTest.class.getCanonicalName() + "_runtime";

    final static String DIR = "/SchemaTesting/";
    static final String SERVER = "schema";
    static final String PROJECT = "SimpleSchemaTest";
    static final String FOLDER = "wlp";
    static final String FILE = "server.xml";

    protected static Document configDoc;
    protected static Document schemaDoc;
    protected static URI configURI;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(SchemaUtilTest.getOrderedTests());
        suite.setName(SchemaUtilTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testDefect158461"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetSchema"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeDefault_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttribute_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetElement1_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeType1_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeType2_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetLabel1_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetLabel2_1"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testRegenerateSchema"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetSchema2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeDefault_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttribute_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetElement1_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeType1_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetAttributeType2_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetLabel1_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testGetLabel2_2"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testTimeoutFailureSavesExisting"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testSchemaInNonTargetedProject"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "testSchemaInTargetedProject"));
        testSuite.addTest(TestSuite.createTest(SchemaUtilTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: SchemaUtilTest");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER, "resources/" + DIR + "/" + SERVER);
        setServerStartTimeout(120);
        setServerStopTimeout(60);
        createVM(JDK_NAME);
        importProjects(new Path(DIR), new String[] { "SimpleSchemaTest" });
    }

    private void testGetSchema(boolean usingFallBackSchema) throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        URL fallbackSchema = WebSphereRuntime.getFallbackSchema();
        assertNotNull(fallbackSchema);
        final URL schemaURL = runtime.getConfigurationSchemaURL();
        assertNotNull(schemaURL);
        assertFalse(schemaURL.sameFile(fallbackSchema));
        if (usingFallBackSchema) {
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT).getFolder(FOLDER).getFile(FILE);
            configURI = file.getLocationURI();
            schemaDoc = ConfigUtils.getDOM(configURI);
        } else {
            ConfigurationFile configFile = wsServer.getConfiguration();
            configURI = configFile.getURI();
            schemaDoc = configFile.getDomDocument();
        }
        assertNotNull(schemaDoc);
    }

    private void testGetAttributeDefault(String s) throws Exception {
        String[] tags = new String[] { "server", "executor" };
        String def = SchemaUtil.getAttributeDefault(schemaDoc, tags, "name", configURI);
        assertEquals(s, def);
    }

    private void testGetAttribute(String s) throws Exception {
        String[] tags = new String[] { "server", "executor" };
        CMAttributeDeclaration decl = SchemaUtil.getAttribute(schemaDoc, tags, "name", configURI);
        assertTrue(decl != null && decl.getAttrName().equals(s) || decl == null && s == null);
    }

    private void testGetElement1(String s) throws Exception {
        String[] tags = new String[] { "server", "basicRegistry" };
        CMElementDeclaration element = SchemaUtil.getElement(schemaDoc, tags, configURI);
        assertTrue(element != null && element.getElementName().equals(s) || element == null && s == null);
    }

    private void testGetAttributeType1(String s) throws Exception {
        String[] tags = { "server", "dataSource", "properties.datadirect.sqlserver" };
        CMDataType type = SchemaUtil.getAttributeType(schemaDoc, tags, "password", configURI);
        assertTrue(type != null && type.getDataTypeName().equals(s) || type == null && s == null);
    }

    private void testGetAttributeType2(String s) throws Exception {
        Element elem = DOMUtils.getFirstChildElement(schemaDoc);
        CMDataType type = SchemaUtil.getAttributeType(elem, "description", schemaDoc, configURI);
        assertTrue(type != null && type.getDataTypeName().equals(s) || type == null && s == null);
    }

    private void testFindAttribute(String s) throws Exception {
        Element elem = DOMUtils.getFirstChildElement(schemaDoc);
        Attr attr = elem.getAttributeNode("description");

        CMAttributeDeclaration adecl = SchemaUtil.findAttribute(attr, configURI);
        assertEquals(s, adecl.getAttrName());
    }

    private void testGetElement2(String s) throws Exception {
        ConfigurationFile configFile = wsServer.getConfiguration();
        Document dom = configFile.getDocument();
        Element elem = DOMUtils.getFirstChildElement(dom);
        CMElementDeclaration edecl = SchemaUtil.getElement(elem);
        assertEquals(s, edecl.getElementName());

    }

    private void testGetLabel1() throws Exception {
        String[] tags = { "server", "applicationMonitor" };
        CMAttributeDeclaration attrib = SchemaUtil.getAttribute(schemaDoc, tags, "pollingRate", configURI);
        String label = SchemaUtil.getLabel(attrib);
        assertTrue(label != null && label.isEmpty() == false);
    }

    private void testGetLabel2() throws Exception {
        String[] tags = { "server", "executor" };
        CMElementDeclaration element = SchemaUtil.getElement(schemaDoc, tags, configURI);
        String label = SchemaUtil.getLabel(element);
        assertTrue(label != null && label.isEmpty() == false);
    }

    @Test
    public void testGetSchema() throws Exception {
        testGetSchema(false);
    }

    @Test
    public void testGetAttributeDefault_1() throws Exception {
        // test with generated schema
        testGetAttributeDefault("Default Executor");
    }

    @Test
    public void testGetAttribute_1() throws Exception {
        // test with generated schema
        testGetAttribute("name");
    }

    @Test
    public void testGetElement1_1() throws Exception {
        // test with generated schema
        testGetElement1("basicRegistry");
    }

    @Test
    public void testGetAttributeType1_1() throws Exception {
        // test with generated schema
        testGetAttributeType1("password");
    }

    @Test
    public void testGetAttributeType2_1() throws Exception {
        // test with generated schema
        testGetAttributeType2("string");
    }

    //    @Test
    public void testFindAttribute_1() throws Exception {
        // test with generated schema
        testFindAttribute("description");
    }

//    @Test
    public void testGetElement2_1() throws Exception {
        // test with generated schema
        testGetElement2("server");
    }

    @Test
    public void testGetLabel1_1() throws Exception {
        // test with generated schema
        testGetLabel1();
    }

    @Test
    public void testGetLabel2_1() throws Exception {
        // test with generated schema
        testGetLabel2();
    }

    @Test
    public void testRegenerateSchema() throws Exception {
        final WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        IJobChangeListener listener = new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                notifyWaitingThread(runtime);
            }
        };
        doGeneration(runtime, listener);
    }

    @Test
    public void testGetSchema2() throws Exception {
        testGetSchema(true);
    }

    @Test
    public void testGetAttributeDefault_2() throws Exception {
        // test with fallback schema
        testGetAttributeDefault("Default Executor");
    }

    @Test
    public void testGetAttribute_2() throws Exception {
        // test with fallback schema
        testGetAttribute("name");
    }

    @Test
    public void testGetElement1_2() throws Exception {
        // test with fallback schema
        testGetElement1("basicRegistry");
    }

    @Test
    public void testGetAttributeType1_2() throws Exception {
        // test with fallback schema
        testGetAttributeType1("password");
    }

    @Test
    public void testGetAttributeType2_2() throws Exception {
        // test with fallback schema
        testGetAttributeType2("string");
    }

    //    @Test
    public void testFindAttribute_2() throws Exception {
        // test with fallback schema
        testFindAttribute("description");
    }

//    @Test
    public void testGetElement2_2() throws Exception {
        // test with fallback schema
        testGetElement2("server");
    }

    @Test
    public void testGetLabel1_2() throws Exception {
        // test with fallback schema
        testGetLabel1();
    }

    @Test
    public void testGetLabel2_2() throws Exception {
        // test with fallback schema
        testGetLabel2();
    }

    // test at end since it is destructive to setup
    @Test
    public void testTimeoutFailureSavesExisting() throws Exception {
        // we time out but the old schema is preserved
        // Note that metadata generation error(s) will be logged due to the artificial timeout
        final WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        final URL beforeSchema = runtime.getConfigurationSchemaURL();
        URL fallbackSchema = WebSphereRuntime.getFallbackSchema();
        assertNotNull(fallbackSchema);
        assertFalse(beforeSchema.sameFile(fallbackSchema));

        int save = GeneratorJob.setNumberOfAttempts(0);
        try {
            IJobChangeListener listener = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    notifyWaitingThread(runtime);
                }
            };
            doGeneration(runtime, listener);
            final URL afterSchema = runtime.getConfigurationSchemaURL();
            assertNotNull(afterSchema);
            assertTrue(afterSchema.sameFile(beforeSchema));
        } finally {
            GeneratorJob.setNumberOfAttempts(save);
        }
    }

    @Test
    public void testSchemaInNonTargetedProject() throws Exception {
        importProjects(new Path("SchemaTesting"), new String[] { "SimpleSchemaTest" });
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("SimpleSchemaTest");
        SchemaLocationProvider provider = new SchemaLocationProvider();
        IFile f = project.getFile("wlp/server.xml");
        assertTrue("Couldn't find server.xml file", f.exists());
        Map<?, ?> map = provider.getExternalSchemaLocation(f.getLocationURI());
        assertTrue("Expected only one schema but got " + map.values().size(), map.values().size() == 1);
        for (Object o : map.values()) {
            URL url = new URL((String) o);
            assertTrue(url.sameFile(WebSphereRuntime.getFallbackSchema()));
        }
    }

    @Test
    public void testSchemaInTargetedProject() throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("SimpleSchemaTest");
        IFacetedProject facetedProject = ProjectFacetsManager.create(project);
        Set<IRuntime> runtimes = new HashSet<IRuntime>();
        IRuntime runtimeA = FacetUtil.getRuntime(runtime);
        runtimes.add(runtimeA);
        facetedProject.setTargetedRuntimes(runtimes, null);
        SchemaLocationProvider provider = new SchemaLocationProvider();

        IFile f = project.getFile("wlp/server.xml");
        assertTrue("Couldn't find server.xml file", f.exists());
        Map<?, ?> map = provider.getExternalSchemaLocation(f.getLocationURI());
        assertTrue("Expected only one schema but got " + map.values().size(), map.values().size() == 1);
        for (Object o : map.values()) {
            URL url = new URL((String) o);
            assertFalse(url.sameFile(WebSphereRuntime.getFallbackSchema()));
        }
    }

    @Test
    public void testDefect158461() throws Exception {
        final WebSphereRuntime wr = wsServer.getWebSphereRuntime();
        IPath metaPath = wr.buildMetadataDirectoryPath();
        File file = metaPath.append("featureList.xml").toFile();

        assertTrue("featureList.xml does not exist", file.exists());
        long timestamp = file.lastModified();

        wr.generateMetadata(null, false, Metadata.ALL_METADATA);
        final IJobManager jobManager = Job.getJobManager();
        Job[] jobs = null;
        boolean found = false;
        while (!found) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            jobs = jobManager.find(Constants.JOB_FAMILY);
            if (jobs != null) {
                for (Job job : jobs) {
                    if (job instanceof GeneratorJob && job.getName().contains(RUNTIME_NAME) && job.getState() == Job.RUNNING) {
                        // let the job run a bit, before we cancel it
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        job.cancel();
                        found = true;
                        break;
                    }
                }
            }
        }

        file = metaPath.append("featureList.xml").toFile();
        assertTrue("Expecting the same timestamp: " + timestamp, timestamp == file.lastModified());

        IJobChangeListener listener = new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                notifyWaitingThread(wr);
            }
        };
        doGeneration(wr, listener);

        file = metaPath.append("featureList.xml").toFile();
        assertFalse("Expecting different timestamp", timestamp == file.lastModified());
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: SchemaUtilTest\n");
    }

    synchronized void doGeneration(WebSphereRuntime runtime, IJobChangeListener listener) {
        runtime.generateMetadata(listener, false, Metadata.ALL_METADATA);
        try {
            // wait for generation
            wait();
        } catch (InterruptedException e) {
            // nothing
        }
    }

    synchronized void notifyWaitingThread(WebSphereRuntime runtime) {
        notifyAll();
    }
}

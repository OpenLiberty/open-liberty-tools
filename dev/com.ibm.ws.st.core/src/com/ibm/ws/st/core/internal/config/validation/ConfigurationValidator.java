/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

import java.io.EOFException;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.ValidatorMessage;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.OutOfSyncModuleInfo;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;

public class ConfigurationValidator extends AbstractValidator {
    public static final String MARKER_TYPE = Activator.PLUGIN_ID + ".configmarker";
    private static final ValidationResult EMPTY_RESULT = new ValidationResult();
    private static final String[] CONTENT_TYPES = new String[] { "bootstrap.properties", "server.env", "jvm.options" };

    @Override
    public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor) {
        if (!(resource instanceof IFile))
            return EMPTY_RESULT;

        IFile file = (IFile) resource;
        try {
            file.deleteMarkers(ConfigurationValidator.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            Trace.logError("Error removing markers", e);
        }

        // For defined runtimes, do not validate any server config files in a templates folder inside the runtime folder
        IPath fileLocation = file.getLocation(); // Full system path of the file
        IPath fileFullPath = file.getFullPath(); // Used to ensure that templates is a subfolder of the runtime
        WebSphereRuntime[] webSphereRuntimes = WebSphereUtil.getWebSphereRuntimes();
        int length = webSphereRuntimes.length;
        for (int i = 0; i < length; i++) {
            WebSphereRuntime runtime = webSphereRuntimes[i];
            IPath runtimeLocation = runtime.getRuntimeLocation();
            if (runtimeLocation.append("templates").isPrefixOf(fileLocation)) { //$NON-NLS-1$
                return EMPTY_RESULT;
            }
        }

        // Workaround to not validate server.xml files in templates folder in build plugins
        // eg. target/liberty/wlp/templates
        // eg. build/wlp/templates/
        // There are two conditions:
        // 1) it must be in some templates folder
        // 2) the parent folder of the templates folder is a valid Liberty runtime.
        boolean hasTemplates = fileFullPath.toString().contains("templates"); //$NON-NLS-1$
        if (hasTemplates) {
            IContainer parent = file.getParent();
            // Get parent containers until we reach templates or null.
            while (parent != null) {
                if ("templates".equals(parent.getName())) { //$NON-NLS-1$
                    IContainer parentOfTemplates = parent.getParent();
                    if (parentOfTemplates == null) { // This could be the workspace root.  If so, then exit
                        break;
                    }
                    boolean validLocation = WebSphereRuntime.isValidLocation(parentOfTemplates.getLocation());
                    if (validLocation) {
                        return EMPTY_RESULT;
                    }
                    break; // Do the validation.
                }
                parent = parent.getParent();
            }
        }

        String contentType = null;
        try {
            IContentDescription contentDesc = file.getContentDescription();
            contentType = contentDesc.getContentType().getId();
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Could not determine content type", e);
            return EMPTY_RESULT;
        }

        try {
            AbstractTextFileParser parser = null;
            if (CONTENT_TYPES[0].equals(contentType))
                parser = new BootstrapPropsValidator(file);
            else if (CONTENT_TYPES[1].equals(contentType))
                parser = new ServerEnvValidator(file);
            else if (CONTENT_TYPES[2].equals(contentType))
                parser = new JVMOptionsValidator(file);

            if (parser != null) {
                ValidationResult result = new ValidationResult();
                parser.parse(result, monitor);
                result.setValidated(new IResource[] { file });
                return result;
            }
        } catch (EOFException e) {
            // ignore
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Error validating file", e);
        }

        ResourceConfigurationValidator validator = new ResourceConfigurationValidator();
        validator.validate(file);

        final ValidationResult valResult = validator.getValidationResult();
        final URI uri = file.getLocationURI();
        final WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            final ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null) {
                updateResultDependencies(configFile, valResult, server);
                if (server.getConfigRoot() == configFile) {
                    checkPublishedApps(file, configFile, server);
                }
                break;
            }
        }
        return valResult;
    }

    // Get list of dependencies and set them on the validation result
    private void updateResultDependencies(ConfigurationFile configFile,
                                          ValidationResult valResult,
                                          WebSphereServerInfo serverInfo) {
        final ConfigurationFile[] includes = configFile.getAllIncludedFiles();
        final String[] unresolvedIncludes = configFile.getAllUnresolvedIncludes();
        final ArrayList<IResource> resourceList = new ArrayList<IResource>(4);

        if (includes.length > 0) {
            for (ConfigurationFile cf : includes) {
                final IFile file = cf.getIFile();
                if (file != null) {
                    resourceList.add(file);
                }
            }
        }

        // Get the bootstrap.properties and server.env files if they exist
        if (serverInfo != null) {
            ExtendedConfigFile bootstrap = serverInfo.getBootstrap();
            if (bootstrap != null && bootstrap.getIFile() != null)
                resourceList.add(bootstrap.getIFile());
            ExtendedConfigFile serverEnv = serverInfo.getServerEnv();
            if (serverEnv != null && serverEnv.getIFile() != null)
                resourceList.add(serverEnv.getIFile());
            serverEnv = serverInfo.getSharedServerEnv();
            if (serverEnv != null && serverEnv.getIFile() != null)
                resourceList.add(serverEnv.getIFile());
            serverEnv = serverInfo.getEtcServerEnv();
            if (serverEnv != null && serverEnv.getIFile() != null)
                resourceList.add(serverEnv.getIFile());
        }

        // Add a dependency on unresolved includes
        // Once the file is created in the workspace, the validation
        // framework will kick in for the including file
        if (unresolvedIncludes.length > 0) {
            final URI baseURI = configFile.getURI();
            if (serverInfo != null) {
                final ConfigVars vars = new ConfigVars();
                serverInfo.getVariables(vars);
                for (String include : unresolvedIncludes) {
                    final URI resolvedURI = resolveURI(baseURI, include, vars);
                    if (resolvedURI != null) {
                        IFile file = serverInfo.getIFile(resolvedURI);
                        if (file != null) {
                            resourceList.add(file);
                        }
                    }
                }
            }
        }

        // Add a dependency on config dropins directories
        if (serverInfo != null) {
            IFolder dropinsFolder = serverInfo.getConfigDefaultDropinsFolder();
            if (dropinsFolder != null)
                resourceList.add(dropinsFolder);
            dropinsFolder = serverInfo.getConfigOverrideDropinsFolder();
            if (dropinsFolder != null)
                resourceList.add(dropinsFolder);
        }

        if (resourceList.size() > 0) {
            final IResource[] dependsOn = resourceList.toArray(new IResource[resourceList.size()]);
            valResult.setDependsOn(dependsOn);
        }
    }

    private URI resolveURI(URI baseURI, String location, ConfigVars vars) {
        final String resolvedLocation = vars.resolve(location);
        final File f = new File(resolvedLocation);
        if (!f.isAbsolute()) {
            try {
                // If include is already an absolute URI then resolve
                // will just return it.
                final URI result = baseURI.resolve(resolvedLocation);
                return result;
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        return f.toURI();
    }

    // Check that published applications on the server have a corresponding
    // <application> element in the configuration file
    private void checkPublishedApps(IFile file,
                                    ConfigurationFile configFile,
                                    WebSphereServerInfo serverInfo) {
        final WebSphereServer server = WebSphereUtil.getWebSphereServer(serverInfo);
        if (server != null) {
            final IModule[] modules = server.getServer().getModules();
            final WebSphereServerBehaviour wsBehaviour = server.getWebSphereServerBehaviour();
            if (wsBehaviour == null) {
                return;
            }

            // check for published modules that might be out of sync with
            // configuration file
            for (IModule m : modules) {
                if (!m.isExternal()) {
                    final OutOfSyncModuleInfo info = wsBehaviour.checkModuleConfigOutOfSync(m);
                    if (info != null) {
                        // Create a configuration marker to indicate
                        // the missing module
                        createOutOfSyncMarker(file, m, info.getType());
                    }
                }
            }
        }
    }

    protected static ValidatorMessage createMessage(IFile file, String message, int lineNum, int start, int end) {
        ValidatorMessage vm = ValidatorMessage.create(message, file);
        vm.setType(ConfigurationValidator.MARKER_TYPE);
        vm.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        vm.setAttribute(IMarker.LINE_NUMBER, lineNum);
        if (start >= 0) {
            vm.setAttribute(IMarker.CHAR_START, start + 1);
            vm.setAttribute(IMarker.CHAR_END, end + 1);
        }

        return vm;
    }

    protected static ValidatorMessage createInvalidWhitespaceMessage(IFile file, int lineNum, int start, int end) {
        ValidatorMessage vm = ValidatorMessage.create(Messages.invalidWhitespace, file);
        vm.setType(ConfigurationValidator.MARKER_TYPE);
        vm.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        vm.setAttribute(IMarker.LINE_NUMBER, lineNum);
        if (start >= 0) {
            vm.setAttribute(IMarker.CHAR_START, start + 1);
            vm.setAttribute(IMarker.CHAR_END, end + 1);
        }

        vm.setAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR, AbstractConfigurationValidator.QuickFixType.INVALID_WHITESPACE.ordinal());
        return vm;
    }

    private void createOutOfSyncMarker(IFile file, IModule module, OutOfSyncModuleInfo.Type type) {
        try {
            String label = (type == OutOfSyncModuleInfo.Type.SHARED_LIB_ENTRY_MISSING) ? Messages.sharedLibraryLabel : Messages.applicationLabel;
            final IMarker marker = file.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
            marker.setAttribute(IMarker.LINE_NUMBER, 1);
            if (type == OutOfSyncModuleInfo.Type.SHARED_LIB_REF_MISMATCH) {
                marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.publishedModuleSharedLibRefMismatchInConfig, new String[] { module.getName(), label }));
                marker.setAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR, AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_SHARED_LIB_REF_MISMATCH.ordinal());
            } else {
                marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.publishedModuleNotInConfig, new String[] { module.getName(), label }));
                marker.setAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR, AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_APP.ordinal());
            }
            marker.setAttribute(AbstractConfigurationValidator.APPLICATION_NAME, module.getName());
        } catch (CoreException ce) {
            if (Trace.ENABLED) {
                Trace.logError("Failed to create a configuration marker for application element: " + module.getName(), ce);
            }
        }
    }
}

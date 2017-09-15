/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.test;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DDJUNITTestPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.xwt.dde.junit.test";

	// The shared instance
	private static DDJUNITTestPlugin plugin;
	
	/**
	 * The constructor
	 */
	public DDJUNITTestPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DDJUNITTestPlugin getDefault() {
		return plugin;
	}
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.ibm.xwt.dde.junit.test", path); //$NON-NLS-1$
	}

	 /**
	   * Get the install URL of this plugin.
	   * 
	   * @return the install url of this plugin
	   */
	  public static String getInstallURL()
	  {
	    try
	    {
	      URL url = FileLocator.resolve(plugin.getBundle().getEntry("/")); //.getFile();
	      return url.getPath();
	    }
	    catch (IOException e)
	    {
	      return null;
	    }
	  }

		public static IPath getInstallLocation() {
		    return new Path(getBundleFullLocationPath(getDefault().getBundle()));
		}
		
		public static String getBundleFullLocationPath(Bundle curBundle)
		{
			if (curBundle == null) {
				return null;
			}
			URL installURL = curBundle.getEntry("/");
			String installLocation = null;
			try {
				URL realURL = FileLocator.resolve(installURL);
				installLocation = realURL.getFile();
				
				// Drop the beginning and end /
				if (installLocation != null && installLocation.startsWith("/") && installLocation.indexOf(":") > 0) {
					installLocation = installLocation.substring(1);
					// Make sure the path ends with a '/'		
					if (!installLocation.endsWith("/")) {
						installLocation = installLocation + "/";
					}
				}
			} catch (IOException e) {
				System.out.println("Could not get the Plugin Full location Path:" + " getPluginFullLocationPath()");
			}
			return installLocation;
		}
}

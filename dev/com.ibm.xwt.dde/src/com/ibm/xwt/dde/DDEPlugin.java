/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde;
import java.net.URL;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.xwt.dde.internal.customization.CustomizationManager;
/**
 * The activator class controls the plug-in life cycle
 */
public class DDEPlugin extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.xwt.dde.ui";

	// The shared instance
	private static DDEPlugin plugin;

	// Preference constants
	public static final String PREFERENCES_SORT_TREE_ALPHABETICALLY = "PreferenceSortTreeAlphabetically";
	public static final String PREFERENCES_EXPAND_SECTIONS = "PreferenceExpandSections";

	// Preference default values
	public static final boolean PREFERENCES_SORT_TREE_ALPHABETICALLY_DEFAULT_VALUE = true;
	public static final boolean PREFERENCES_EXPAND_SECTIONS_DEFAULT_VALUE = true;

	// Font constants
	public Font FONT_DEFAULT;
	public Font FONT_DEFAULT_BOLD;
	
	public DDEPlugin() {
		plugin = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		CustomizationManager.getInstance();
		initializeFonts();
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static DDEPlugin getDefault() {
		return plugin;
	}



	public Image getImage(String iconName) {
		ImageRegistry imageRegistry = getImageRegistry();
		if (imageRegistry.get(iconName) != null) {
			return imageRegistry.get(iconName);
		}
		else {
			//imageRegistry.put(iconName, ImageDescriptor.createFromFile(getClass(), iconName));
			imageRegistry.put(iconName, getImageDescriptor(iconName));
			return imageRegistry.get(iconName);
		}
	}


	public ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.ibm.xwt.dde", path); //$NON-NLS-1$
	}

	public Image getIcon(String name) {
		try {
			ImageRegistry imageRegistry = getImageRegistry();

			if (imageRegistry.get(name) != null) {
				return imageRegistry.get(name);
			}
			else {
				URL installURL = getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
				String imageString = "icons/" + name; //$NON-NLS-1$

				URL imageURL = new URL(installURL, imageString);
				imageRegistry.put(name, ImageDescriptor.createFromURL(imageURL));
				return imageRegistry.get(name);
			}

		} catch (Exception e) {
			return null;
		}
	}

	public Image getImageFromRegistry(String name) {
		ImageRegistry imageRegistry = getImageRegistry();
		return imageRegistry.get(name);
	}

	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(PREFERENCES_SORT_TREE_ALPHABETICALLY, PREFERENCES_SORT_TREE_ALPHABETICALLY_DEFAULT_VALUE);
		store.setDefault(PREFERENCES_EXPAND_SECTIONS, PREFERENCES_EXPAND_SECTIONS_DEFAULT_VALUE);
	}

	private void initializeFonts() {
		try {
			FONT_DEFAULT = JFaceResources.getDefaultFont();
			FontData[] fontData = FONT_DEFAULT.getFontData();
			for (int i = 0; i < fontData.length; i++) {
				fontData[i].setStyle(SWT.BOLD);
			}
			FONT_DEFAULT_BOLD = new Font(null, fontData);
		} catch (Exception exception) {}
	}

	
}

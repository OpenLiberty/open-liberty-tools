/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;

public abstract class AbstractLibertyBuildPluginRuntimeLabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider, org.eclipse.ui.navigator.ICommonLabelProvider, ILibertyBuildPluginImplProvider {

    /** {@inheritDoc} */
    @Override
    public String getDescription(Object obj) {
        if (obj instanceof LibertyBuildPluginProjectNode) {
            String description = ((LibertyBuildPluginProjectNode) obj).getDescription();
            if (description != null) {
                return description;
            }
        }
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public Image getImage(Object arg0) {
        Image runtimeImage = getRuntimeImage();
        Image disabledImage = new Image(Display.getDefault(), runtimeImage, SWT.IMAGE_GRAY);
        return disabledImage;
    }

    protected abstract Image getRuntimeImage();

    /** {@inheritDoc} */
    @Override
    public String getText(Object obj) {
        if (obj instanceof LibertyBuildPluginProjectNode) {
            LibertyBuildPluginProjectNode node = ((LibertyBuildPluginProjectNode) obj);
            StringBuilder mavenNodeText = new StringBuilder();
            mavenNodeText.append(node.getText());
            String installDir = node.getInstallDir();
            if (installDir != null)
                mavenNodeText.append(" [" + installDir + "]");
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(ILabelProviderListener arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLabelProperty(Object arg0, String arg1) {
        // intentionally empty
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(ILabelProviderListener arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public void restoreState(IMemento arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public void saveState(IMemento arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public void init(ICommonContentExtensionSite arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    public StyledString getStyledText(Object obj) {
        StyledString.Styler styler = StyledString.QUALIFIER_STYLER;
        if (obj instanceof LibertyBuildPluginProjectNode) {
            LibertyBuildPluginProjectNode node = ((LibertyBuildPluginProjectNode) obj);
            StyledString ss = new StyledString(node.getText());
            String installDir = node.getInstallDir();
            if (installDir != null)
                ss.append(" [" + installDir + "]", styler);
            return ss;
        }
        return null;
    }
}

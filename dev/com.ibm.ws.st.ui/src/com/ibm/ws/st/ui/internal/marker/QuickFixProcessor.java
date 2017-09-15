/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.config.validation.ConfigurationValidator;
import com.ibm.ws.st.ui.internal.Activator;

/**
 * Quick fix processor for the configuration editor source view.
 */
@SuppressWarnings("rawtypes")
public class QuickFixProcessor implements IQuickAssistProcessor {

    private static final MarkerResolutionGenerator resolutionGen = new MarkerResolutionGenerator();

    /** {@inheritDoc} */
    @Override
    public boolean canAssist(IQuickAssistInvocationContext quickAssistContext) {
        Iterator iter = getAnnotationIterator(quickAssistContext);
        if (iter != null) {
            while (iter.hasNext()) {
                Annotation anno = (Annotation) iter.next();
                if (anno instanceof SimpleMarkerAnnotation) {
                    IMarker marker = ((SimpleMarkerAnnotation) anno).getMarker();
                    try {
                        if (ConfigurationValidator.MARKER_TYPE.equals(marker.getType()) &&
                            resolutionGen.hasResolutions(marker))
                            return true;
                    } catch (CoreException e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Failed to get marker type for quick assist processing.", e);
                        }
                    }
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canFix(Annotation arg0) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext quickAssistContext) {
        Iterator iter = getAnnotationIterator(quickAssistContext);
        if (iter != null) {
            List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
            while (iter.hasNext()) {
                Annotation anno = (Annotation) iter.next();
                if (anno instanceof SimpleMarkerAnnotation) {
                    IMarker marker = ((SimpleMarkerAnnotation) anno).getMarker();
                    try {
                        if (ConfigurationValidator.MARKER_TYPE.equals(marker.getType())) {
                            IMarkerResolution[] resolutions = resolutionGen.getResolutions(marker);
                            if (resolutions != null)
                                for (IMarkerResolution resolution : resolutions)
                                    proposals.add(new QuickFixCompletionProposal(marker, resolution));
                        }
                    } catch (CoreException e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Failed to get marker type for quick assist processing.", e);
                        }
                    }
                }
            }
            return proposals.toArray(new ICompletionProposal[proposals.size()]);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getErrorMessage() {
        return null;
    }

    private Iterator getAnnotationIterator(IQuickAssistInvocationContext quickAssistContext) {
        ISourceViewer viewer = quickAssistContext.getSourceViewer();
        IAnnotationModel model = viewer != null ? viewer.getAnnotationModel() : null;
        if (model instanceof IAnnotationModelExtension2) {
            int documentOffset = quickAssistContext.getOffset();
            int length = viewer != null ? viewer.getSelectedRange().y : 0;
            return ((IAnnotationModelExtension2) model).getAnnotationIterator(documentOffset, length, true, true);
        }

        return null;
    }

    /*
     * Wrapper for the IMarkerResolution that implements ICompletionProposal
     */
    private static class QuickFixCompletionProposal implements ICompletionProposal {

        private final IMarker marker;
        private final IMarkerResolution resolution;

        public QuickFixCompletionProposal(IMarker marker, IMarkerResolution resolution) {
            this.marker = marker;
            this.resolution = resolution;
        }

        /** {@inheritDoc} */
        @Override
        public void apply(IDocument arg0) {
            resolution.run(marker);
        }

        /** {@inheritDoc} */
        @Override
        public String getAdditionalProposalInfo() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public IContextInformation getContextInformation() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getDisplayString() {
            return resolution.getLabel();
        }

        /** {@inheritDoc} */
        @Override
        public Image getImage() {
            return Activator.getImage(Activator.IMG_CONFIG_ELEMENT);
        }

        /** {@inheritDoc} */
        @Override
        public Point getSelection(IDocument arg0) {
            return null;
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.customization;

import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.ibm.xwt.dde.internal.controls.AbstractControl;
import com.ibm.xwt.dde.internal.controls.CustomSection;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;

public class TypeCustomization extends DetailItemCustomization{
	private String typeName;
	private boolean isUnion;

	public TypeCustomization(String typeName, String label, boolean hidden, String buttonLabel,
			String toolTip, String labelLinkToolTip, String buttonToolTip,
			Class buttonClass, Class linkClass, int lines, Image icon,
			int style, boolean required, boolean readOnly,
			boolean deleteIfEmpty, String treeLabel, boolean hideLabel,
			boolean disabled, boolean singleOccurrence, Class creationClass,
			Map possibleValues, String creationLabel, Image tableIcon,
			boolean hideSectionTitle, String headerText, String footerText,
			String sectionHeaderText, Class sectionHeaderTextClass,
			String detailSectionTitle, String checkBoxText,
			Class treeLabelClass, Class detailSectionTitleClass,
			Class deletionClass, boolean canCreate, boolean canDelete,
			Class canCreateClass, Class canDeleteClass,
			Class possibleValuesClass, Class suggestedValuesClass,
			String defaultValue, Class defaultValueClass,
			Class validationClass, Class customControlClass,
			boolean detectSchemaLabel, String[] triggerValidationPath,
			boolean[] trigerNodeValidationRecurse, boolean cDataSectionStorage,
			boolean horizontalScrolling, String helpContextID,
			String hoverHelp, Customization customization,
			boolean skipSyntaxValidation, AbstractControl[] customControls,
			CustomSection[] customSections,
			boolean clearOptionalSectionIfEmpty, boolean showItemAsOptional,
			String buttonAccessibilityName, char echoChar, boolean wrapText,
			Class disableClass, Class shouldItemDisableClass, int detailsSortingOption, boolean isUnion, Class iconClass, Image buttonIcon) {
		super(label, hidden, buttonLabel, toolTip, labelLinkToolTip, buttonToolTip,
				buttonClass, linkClass, lines, icon, style, required, readOnly,
				deleteIfEmpty, treeLabel, hideLabel, disabled, singleOccurrence,
				creationClass, possibleValues, creationLabel, tableIcon,
				hideSectionTitle, headerText, footerText, sectionHeaderText,
				sectionHeaderTextClass, detailSectionTitle, checkBoxText,
				treeLabelClass, detailSectionTitleClass, deletionClass, canCreate,
				canDelete, canCreateClass, canDeleteClass, possibleValuesClass,
				suggestedValuesClass, defaultValue, defaultValueClass, validationClass,
				customControlClass, detectSchemaLabel, triggerValidationPath,
				trigerNodeValidationRecurse, cDataSectionStorage, horizontalScrolling,
				helpContextID, hoverHelp, customization, skipSyntaxValidation,
				customControls, customSections, clearOptionalSectionIfEmpty,
				showItemAsOptional, buttonAccessibilityName, echoChar, wrapText,
				disableClass, shouldItemDisableClass, detailsSortingOption, iconClass, buttonIcon);
		this.typeName = typeName;
		this.isUnion = isUnion;
	}

	public boolean isUnion()
	{
		return isUnion;
	}
}

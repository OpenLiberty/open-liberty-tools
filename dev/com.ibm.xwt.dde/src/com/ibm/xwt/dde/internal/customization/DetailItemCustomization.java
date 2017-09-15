/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.customization;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.ibm.xwt.dde.internal.controls.AbstractControl;
import com.ibm.xwt.dde.internal.controls.CustomSection;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;

public class DetailItemCustomization {
	
	public static final int STYLE_DEFAULT = 0;
	public static final int STYLE_TEXT = 1;
	public static final int STYLE_COMBO = 2;
	public static final int STYLE_LIST = 3;
	public static final int STYLE_CHECKBOX = 4;
	public static final int STYLE_TREE_NODE = 5;
	
	private int style;
	private Image icon;
	private Map possibleValues;
	private List suggestedValues;
	private String label;
	boolean hidden;
	private String toolTip;
	private String buttonLabel;
	private Image buttonIcon;
	private String buttonToolTip;
	private String labelLinkToolTip;
	private Class buttonClass;
	private Class labelLinkClass;
	private Class creationClass;
	private Class treeLabelClass;
	private Class detailSectionTitleClass;
	private Class deletionClass;
	private Class sectionHeaderTextClass;
	private Class iconClass;
	private int lines;
	private boolean required;
	private boolean readOnly;
	private boolean disabled;
	private boolean deleteIfEmpty;
	private boolean hideLabel;
	private boolean singleOccurrence;
	private String treeLabel;
	private String creationLabel;
	private Image tableIcon;
	private boolean hideSectionTitle;
	private int order;
	private String headerText;
	private String footerText;
	private String sectionHeaderText;
	private String detailSectionTitle;
	private String checkBoxText;
	private boolean canCreate;
	private boolean canDelete;
	private Class canCreateClass;
	private Class canDeleteClass;
	private Class possibleValuesClass;
	private Class suggestedValuesClass;
	private String defaultValue;
	private Class defaultValueClass;
	private Class validationClass;
	private Class customControlClass;
	private String[] triggerNodeValidationPath;
	private boolean[] trigerNodeValidationRecurse;
	private boolean cDataSectionStorage;
	private boolean detectSchemaLabel;
	boolean horizontalScrolling;
	private String helpContextId;
	private Customization customization;
	private String hoverHelp;
	private boolean skipSyntaxValidation;
	private AbstractControl[] customControls;
	private CustomSection[] customSections;
	private boolean clearOptionalSectionIfEmpty;
	private boolean showItemAsOptional;
	private String buttonAccessibilityName;
	private char echoChar;
	private boolean wrapText;
	private Class disableClass;
	private Class shouldItemDisableClass;
	private int detailsSortingOption;

	public DetailItemCustomization(String label, boolean hidden, String buttonLabel, String toolTip, String labelLinkToolTip, String buttonToolTip, Class buttonClass, Class linkClass, int lines, Image icon, int style, boolean required, boolean readOnly, boolean deleteIfEmpty, String treeLabel, boolean hideLabel, boolean disabled, boolean singleOccurrence, Class creationClass, Map possibleValues, String creationLabel, Image tableIcon, boolean hideSectionTitle, String headerText, String footerText, String sectionHeaderText, Class sectionHeaderTextClass, String detailSectionTitle, String checkBoxText, Class treeLabelClass, Class detailSectionTitleClass, Class deletionClass, boolean canCreate, boolean canDelete, Class canCreateClass, Class canDeleteClass, Class possibleValuesClass, Class suggestedValuesClass, String defaultValue, Class defaultValueClass, Class validationClass, Class customControlClass, boolean detectSchemaLabel, String[] triggerValidationPath, boolean[] trigerNodeValidationRecurse, boolean cDataSectionStorage, boolean horizontalScrolling, String helpContextID, String hoverHelp, Customization customization, boolean skipSyntaxValidation, AbstractControl[] customControls, CustomSection[] customSections, boolean clearOptionalSectionIfEmpty, boolean showItemAsOptional, String buttonAccessibilityName, char echoChar, boolean wrapText, Class disableClass, Class shouldItemDisableClass, int detailsSortingOption, Class iconClass, Image buttonIcon) {
		this.label = label;
		this.hidden = hidden;
		this.toolTip = toolTip;
		this.labelLinkToolTip = labelLinkToolTip;
		this.buttonLabel = buttonLabel;
		this.buttonIcon = buttonIcon;
		this.buttonToolTip = buttonToolTip;
		this.buttonClass = buttonClass;
		this.labelLinkClass = linkClass;
		this.lines = lines;
		this.icon = icon;
		this.style = style;
		this.required = required;
		this.readOnly = readOnly;
		this.possibleValues = possibleValues;
		this.deleteIfEmpty = deleteIfEmpty;
		this.treeLabel = treeLabel;
		this.hideLabel = hideLabel;
		this.singleOccurrence = singleOccurrence;
		this.disabled = disabled;
		this.creationClass = creationClass;
		this.creationLabel = creationLabel;
		this.tableIcon = tableIcon;
		this.hideSectionTitle = hideSectionTitle;
		this.headerText = headerText;
		this.footerText = footerText;
		this.sectionHeaderText = sectionHeaderText;
		this.sectionHeaderTextClass = sectionHeaderTextClass;
		this.detailSectionTitle = detailSectionTitle;
		this.checkBoxText = checkBoxText;
		this.treeLabelClass = treeLabelClass;
		this.detailSectionTitleClass = detailSectionTitleClass;
		this.deletionClass = deletionClass;
		this.canCreate = canCreate;
		this.canDelete = canDelete;
		this.canCreateClass = canCreateClass;
		this.canDeleteClass = canDeleteClass;
		this.possibleValuesClass = possibleValuesClass;
		this.suggestedValuesClass = suggestedValuesClass;
		this.defaultValue = defaultValue;
		this.defaultValueClass = defaultValueClass;
		this.validationClass = validationClass;
		this.customControlClass = customControlClass;
		this.detectSchemaLabel = detectSchemaLabel;
		this.triggerNodeValidationPath = triggerValidationPath;
		this.trigerNodeValidationRecurse = trigerNodeValidationRecurse;
		this.cDataSectionStorage = cDataSectionStorage;
		this.horizontalScrolling = horizontalScrolling;
		this.helpContextId = helpContextID;
		this.customization = customization;
		this.hoverHelp = hoverHelp;
		this.skipSyntaxValidation = skipSyntaxValidation;
		this.customControls = customControls;
		this.customSections = customSections;
		this.clearOptionalSectionIfEmpty = clearOptionalSectionIfEmpty;
		this.showItemAsOptional = showItemAsOptional;
		this.buttonAccessibilityName = buttonAccessibilityName;
		this.echoChar = echoChar;
		this.wrapText = wrapText;
		this.disableClass = disableClass;
		this.shouldItemDisableClass = shouldItemDisableClass;
		this.detailsSortingOption = detailsSortingOption;
		this.iconClass = iconClass;
	}

	public String getLabel() {
		return label;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public String getButtonLabel() {
		return buttonLabel;
	}
	
	public Image getButtonIcon()
	{
		return buttonIcon;
	}
	
	public String getButtonToolTip() {
		return buttonToolTip;
	}
	
	public Class getButtonClass() {
		return buttonClass;
	}

	public Class getLinkClass() {
		return labelLinkClass;
	}

	public int getLines() {
		return lines;
	}

	public Image getIcon() {
		return icon;
	}

	public int getStyle() {
		return style;
	}
	
	public boolean isRequired() {
		return required;
	}

	public Map getPossibleValues() {
		return possibleValues;
	}

	public void setPossibleValues(Map possibleValues) {
		this.possibleValues = possibleValues;
	}
	
	public List getSuggestedValues() {
		return suggestedValues;
	}
	
	public void setSuggestedValues(List suggestedValues) {
		this.suggestedValues = suggestedValues;
	}

	public String getToolTip() {
		return toolTip;
	}

	public String getLabelLinkToolTip() {
		return labelLinkToolTip;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
	
	public boolean isDeleteIfEmpty() {
		return deleteIfEmpty;
	}
	
	public String getTreeLabel() {
		return treeLabel;
	}
	
	public boolean isHideLabel() {
		return hideLabel;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public Class getCreationClass() {
		return creationClass;
	}
	
	public boolean isSingleOccurrence() {
		return singleOccurrence;
	}

	public String getCreationLabel() {
		return creationLabel;
	}

	public Image getTableIcon() {
		return tableIcon;
	}

	public boolean isHideSectionTitle() {
		return hideSectionTitle;
	}
	
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public String getHeaderText() {
		return headerText;
	}
	
	public String getFooterText() {
		return footerText;
	}

	public String getSectionHeaderText() {
		return sectionHeaderText;
	}

	public String getDetailSectionTitle() {
		return detailSectionTitle;
	}
	
	public String getCheckBoxText() {
		return checkBoxText;
	}

	public Class getTreeLabelClass() {
		return treeLabelClass;
	}

	public Class getDetailSectionTitleClass() {
		return detailSectionTitleClass;
	}
	
	public Class getIconClass() {
		return iconClass;
	}
	
	public Class getDeletionClass() {
		return deletionClass;
	}

	public Class getSectionHeaderTextClass() {
		return sectionHeaderTextClass;
	}

	public boolean isCanCreate() {
		return canCreate;
	}

	public boolean isCanDelete() {
		return canDelete;
	}

	public Class getCanCreateClass() {
		return canCreateClass;
	}

	public Class getCanDeleteClass() {
		return canDeleteClass;
	}
	
	public Class getPossibleValuesClass() {
		return possibleValuesClass;
	}
	
	public Class getSuggestedValuesClass() {
		return suggestedValuesClass;
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}
	
	public Class getDefaultValueClass() {
		return defaultValueClass;
	}
	public Class getValidationClass() {
		return validationClass;
	}
	
	public Class getCustomControlClass()
	{
		return customControlClass;
	}
	
	public boolean getDetectSchemaLabel()
	{
		return detectSchemaLabel;
	}
	
	public String[] getTriggerValidationPath() {
		return triggerNodeValidationPath;
	}
	
	public boolean[] isTrigerNodeValidationRecurse(){
		return trigerNodeValidationRecurse;
	}
	
	public boolean isCDATASectionStorage() {
		return cDataSectionStorage;
	}
	
	public boolean isHorizontalScrolling() {
		return horizontalScrolling;
	}
	
	public String getHelpContextId() {
		return helpContextId;
	}
	
	public Customization getCustomization() {
		return customization;
	}
	
	public String getHoverHelp() {
		return hoverHelp;
	}
	
	public boolean isSkipSyntaxValidation() {
		return skipSyntaxValidation;
	}
	
	public void setControls(AbstractControl[] customControls) {
		this.customControls = customControls;
	}
	
	public AbstractControl[] getCustomControls() {
		return customControls;
	}

	public CustomSection[] getCustomSections() {
		return customSections;
	}

	public void setCustomSections(CustomSection[] customSections) {
		this.customSections = customSections;
	}

	public boolean isClearOptionalSectionIfEmpty() {
		return clearOptionalSectionIfEmpty;
	}

	public boolean isShowItemAsOptional() {
		return showItemAsOptional;
	}
	
	public String getButtonAccessibilityName() {
		return buttonAccessibilityName;
	}
	
	public char getEchoChar() {
		return echoChar;
	}
	
	public boolean isWrapText() {
		return wrapText;
	}
	
	public Class getDisableClass() {
		return disableClass;
	}
	
	public Class getShouldItemDisableClass() {
		return shouldItemDisableClass;
	}
	
	public int getDetailsSortingOption() {
		return detailsSortingOption;
	}
	
}

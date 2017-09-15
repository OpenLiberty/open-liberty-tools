/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.xwt.dde.internal.messages;


import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	
	static {
		NLS.initializeMessages("com.ibm.xwt.dde.internal.messages.Messages", Messages.class); //$NON-NLS-1$
	}

	public Messages() {
		super();
	}

	public static String LABEL_DESIGN;
	public static String LABEL_REMOVE;
	public static String LABEL_SOURCE;
	public static String LABEL_TREE;
	public static String LABEL_EXPAND;
	public static String LABEL_EXPAND_ALL; 
	public static String LABEL_COLLAPSE;
	public static String LABEL_COLLAPSE_ALL;
	public static String LABEL_OPEN;
	public static String LABEL_CLOSE;
	public static String LABEL_OVERVIEW;
	public static String LABEL_DETAILS;
	public static String LABEL_SORT;
	public static String LABEL_VIEW;
	public static String LABEL_ADD;
	public static String LABEL_DELETE;
	public static String LABEL_REPLACE;
	public static String LABEL_SORT_ALPHABETICALLY;
	public static String LABEL_EXPAND_SECTIONS;
	public static String LABEL_UP;
	public static String LABEL_DOWN;
	public static String LABEL_UP_WITH_MNEMONIC;
	public static String LABEL_DOWN_WITH_MNEMONIC;
	public static String LABEL_NEW_ITEM;
	public static String MISSING_GRAMMAR_MESSAGE;
	public static String LABEL_ADD_DOTS;
	public static String LABEL_REPLACE_DOTS;
	public static String ADD_ITEM;
	public static String ADD_ITEM_MESSAGE;
	public static String REPLACE_ITEM;
	public static String REPLACE_ITEM_MESSAGE;
	public static String REMOVE_WITH_MNEMONIC;
	public static String ADD_WITH_MNEMONIC;
	public static String TYPE_FILTER_TEXT;
	public static String LABEL_CLEAR;
	public static String DETAIL_SECTION_DESCRIPTION;
	public static String ERROR_DETECTED;
	public static String ERRORS_DETECTED;
	public static String WARNING_DETECTED;
	public static String WARNINGS_DETECTED;
	public static String SOURCE_ERROR_DETECTED;
	public static String OPTIONAL;
	public static String CLEAR_OPTIONAL_ITEM;
	public static String CLEAR_OPTIONAL_ITEM_CONFIRMATION;
	public static String REQUIRED_ITEM_MISSING;
	public static String MULTIPLE_REQUIRED_ITEMS_MISSING;
	public static String SINGLE_REQUIRED_ITEM_MISSING;
	public static String THE_FOLLOWING_ITEMS_CONTAIN_ERRORS;
	public static String THE_FOLLOWING_ITEM_CONTAIN_ERRORS;
	public static String THE_FOLLOWING_ITEMS_CONTAIN_WARNINGS;
	public static String THE_FOLLOWING_ITEM_CONTAIN_WARNINGS;
	public static String SINGLE_ERROR;
	public static String MULTIPLE_ERRORS;
	public static String SINGLE_WARNING;
	public static String MULTIPLE_WARNINGS;
	public static String THE_LIST_MUST_CONTAIN_AT_LEAST_ONE_ENTRY;
	public static String THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_SELECTIONS;
	
	
	
	public static String DATA_TYPE_VALIDATION_INVALID_decimal;
	public static String DATA_TYPE_VALIDATION_INVALID_date;
	public static String DATA_TYPE_VALIDATION_INVALID_dateTime;
	public static String DATA_TYPE_VALIDATION_INVALID_duration;
	public static String DATA_TYPE_VALIDATION_INVALID_gDay;
	public static String DATA_TYPE_VALIDATION_INVALID_gMonth;
	public static String DATA_TYPE_VALIDATION_INVALID_gMonthDay;
	public static String DATA_TYPE_VALIDATION_INVALID_gYear;
	public static String DATA_TYPE_VALIDATION_INVALID_gYearMonth;
	public static String DATA_TYPE_VALIDATION_INVALID_time;
	public static String DATA_TYPE_VALIDATION_INVALID_boolean;
	public static String DATA_TYPE_VALIDATION_INVALID_double;
	public static String DATA_TYPE_VALIDATION_INVALID_float;
	public static String DATA_TYPE_VALIDATION_INVALID_hexBinary;
	public static String ITEM;
	public static String ADD;
	public static String REMOVE;
	public static String MOVE_UP;
	public static String MOVE_DOWN;
	public static String EDIT;
	public static String CLEAR;
	public static String TOGGLE;
	public static String SET;
	public static String THE_DOCUMENT_HAS_BEEN_MODIFIED_OUTSIDE_THE_EDITOR;
	public static String ERRORS_IN_SOURCE_VIEW;
	public static String SWITCH_TO_SOURCE_VIEW;
	
	
	
	public static String THE_VALUE_MUST_BE_1_CHARACTER_LONG;
	public static String THE_VALUE_MUST_BE_X_CHARACTERS_LONG;
	public static String THE_VALUE_MUST_BE_AT_LEAST_1_CHARACTER_LONG;
	public static String THE_VALUE_MUST_BE_AT_LEAST_X_CHARACTERS_LONG;
	public static String THE_VALUE_CANNOT_BE_MORE_THAN_1_CHARACTER_LONG;
	public static String THE_VALUE_CANNOT_BE_MORE_THAN_X_CHARACTERS_LONG;
	public static String THE_VALUE_MUST_CONFORM_TO_THE_XML_SCHEMA_PATTERN_X;
	public static String THE_VALUE_MUST_BE_X;
	public static String THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_OPTIONS;
	public static String THE_VALUE_CANNOT_BE_GREATER_THAN_X;
	public static String THE_VALUE_CANNOT_BE_SMALLER_THAN_X;
	public static String THE_VALUE_MUST_BE_GREATER_THAN_X;
	public static String THE_VALUE_MUST_BE_SMALLER_THAN_X;
	public static String THE_VALUE_CANNOT_CONTAIN_FRACTIONAL_DIGITS;
	public static String THE_VALUE_CANNOT_CONTAIN_MORE_THAN_1_FRACTIONAL_DIGIT;
	public static String THE_VALUE_CANNOT_CONTAIN_MORE_THAN_X_FRACTIONAL_DIGIT;
	public static String LIST_ITEM_X;
	public static String THE_LIST_MUST_CONTAIN_1_ITEM;
	public static String THE_LIST_MUST_CONTAIN_X_ITEMS;
	public static String THE_LIST_MUST_CONTAIN_AT_LEAST_1_ITEM;
	public static String THE_LIST_MUST_CONTAIN_AT_LEAST_X_ITEMS;
	public static String THE_LIST_MUST_CONTAIN_AT_MOST_1_ITEM;
	public static String THE_LIST_MUST_CONTAIN_AT_MOST_X_ITEMS;
	public static String THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_LISTS;
	public static String THE_VALUE_IS_INVALID;
	public static String DATA_TYPE_VALIDATION_INVALID_UNION;
	
	public static String ERROR_LOG_MESSAGE_CUSTOMIZATION_FILE_NOT_FOUND;
	public static String ERROR_LOG_MESSAGE_CUSTOMIZATION_PROPERTIES_FILE_NOT_FOUND;
	public static String ERROR_LOG_MESSAGE_CUSTOMIZATION_FILE_COULD_NOT_BE_SUCCESSFULLY_PARSED;
	public static String ERROR_LOG_MESSAGE_MISSING_TRANSLATION;
	public static String ERROR_LOG_MESSAGE_MISSING_CUSTOMIZATION_CLASS;
	public static String ERROR_LOG_MESSAGE_MISSING_IMAGE;
	
	public static String ERROR_RESOURCE_CONTENTS_COULD_NOT_BE_VALIDATED;
	public static String MULTIPLE_ITEMS_ARE_CURRENTLY_SELECTED;
	
	public static String REMOVE_ALL_SELECTED_ITEMS_QUESTION;
	public static String REMOVE_ALL_SELECTED_ITEMS;
	
	public static String LABEL_MOVE_UP_WITH_MNEMONIC;
	public static String LABEL_MOVE_DOWN_WITH_MNEMONIC;
	
	public static String CLICK_TO_SET;
	
	public static String LABEL_COLON;
	public static String LABEL_ASTERISK;
	
	public static String VALIDATION_MESSAGE;
	public static String DETAILED_VALIDATION_MESSAGE;
	
	public static String LABEL_COLON_SPACE;
	
	public static String ITEM_LIST;
	public static String AVAILABLE_CHILD_ELEMENTS;
	public static String AVAILABLE_CHILD_ELEMENTS_MORE;
	
	public static String DEFAULT_VALUE;
}
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

package com.ibm.xwt.dde.internal.validation;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.xerces.impl.xpath.regex.RegularExpression;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDFacet;
import org.eclipse.xsd.XSDFractionDigitsFacet;
import org.eclipse.xsd.XSDLengthFacet;
import org.eclipse.xsd.XSDMaxExclusiveFacet;
import org.eclipse.xsd.XSDMaxInclusiveFacet;
import org.eclipse.xsd.XSDMaxLengthFacet;
import org.eclipse.xsd.XSDMinExclusiveFacet;
import org.eclipse.xsd.XSDMinInclusiveFacet;
import org.eclipse.xsd.XSDMinLengthFacet;
import org.eclipse.xsd.XSDPatternFacet;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.XSDVariety;

import com.ibm.xwt.dde.internal.messages.Messages;

public class DataTypeValidator {
	
	// Primitive types
	private final int QName 		= 0;
	private final int string 		= 1;
	private final int decimal 		= 2;
	private final int date			= 3;
	private final int dateTime		= 4;
	private final int duration		= 5;
	private final int gDay			= 6;
	private final int gMonth		= 7;
	private final int gMonthDay		= 8;
	private final int gYear			= 9;
	private final int gYearMonth	= 10;
	private final int time			= 11;
	private final int anyURI   		= 12;
	private final int base64Binary	= 13;
	private final int _boolean   	= 14;
	private final int _double   	= 15;
	private final int _float   		= 16;
	private final int hexBinary   	= 17;
	private final int NOTATION   	= 18;
	
	// Facets
	private final int length			= 0;
	private final int minLength			= 1;
	private final int maxLength			= 2;
	private final int pattern			= 3;
	private final int enumeration		= 4;
	private final int whiteSpace		= 5;
	private final int maxInclusive		= 6;
	private final int maxExclusive		= 7;
	private final int minInclusive		= 8;
	private final int minExclusive		= 9;
	private final int fractionDigits	= 10;

	private HashMap dataTypesMap;
	private HashMap facetsMap;
	
	static final DatatypeFactory datatypeFactory;
	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException exception) {
			throw new RuntimeException(exception);
		}
	}
	
	public DataTypeValidator() {
		dataTypesMap = new HashMap();
		dataTypesMap.put("QName", new Integer(QName));
		dataTypesMap.put("string", new Integer(string));
		dataTypesMap.put("decimal", new Integer(decimal));
		dataTypesMap.put("date", new Integer(date));
		dataTypesMap.put("dateTime", new Integer(dateTime));
		dataTypesMap.put("duration", new Integer(duration));
		dataTypesMap.put("gDay", new Integer(gDay));
		dataTypesMap.put("gMonth", new Integer(gMonth));
		dataTypesMap.put("gMonthDay", new Integer(gMonthDay));
		dataTypesMap.put("gYear", new Integer(gYear));
		dataTypesMap.put("gYearMonth", new Integer(gYearMonth));
		dataTypesMap.put("time", new Integer(time));
		dataTypesMap.put("anyURI", new Integer(anyURI));
		dataTypesMap.put("base64Binary", new Integer(base64Binary));
		dataTypesMap.put("boolean", new Integer(_boolean));
		dataTypesMap.put("double", new Integer(_double));
		dataTypesMap.put("float", new Integer(_float));
		dataTypesMap.put("hexBinary", new Integer(hexBinary));
		dataTypesMap.put("NOTATION", new Integer(NOTATION));
		
		facetsMap = new HashMap();
		facetsMap.put("length", new Integer(length));
		facetsMap.put("minLength", new Integer(minLength));
		facetsMap.put("maxLength", new Integer(maxLength));
		facetsMap.put("pattern", new Integer(pattern));
		facetsMap.put("enumeration", new Integer(enumeration));
		facetsMap.put("whiteSpace", new Integer(whiteSpace));
		facetsMap.put("maxInclusive", new Integer(maxInclusive));
		facetsMap.put("maxExclusive", new Integer(maxExclusive));
		facetsMap.put("minInclusive", new Integer(minInclusive));
		facetsMap.put("minExclusive", new Integer(minExclusive));
		facetsMap.put("fractionDigits", new Integer(fractionDigits));
	}
	

	public String validateXSDTypeDefinition(XSDTypeDefinition xsdTypeDefinition, String value) {
		XSDSimpleTypeDefinition xsdSimpleTypeDefinition = xsdTypeDefinition.getSimpleType();
		if(xsdSimpleTypeDefinition != null) {
			switch(xsdSimpleTypeDefinition.getVariety().getValue()) {
			case XSDVariety.ATOMIC:
				return validateAtomicSimpleTypeValue(xsdSimpleTypeDefinition, value);
			case XSDVariety.LIST:
				return validateListTypeValue(xsdSimpleTypeDefinition, value);
			case XSDVariety.UNION:
				return validateUnionTypeValue(xsdSimpleTypeDefinition, value);
			}
		}
		return null;
	}
	

	private String validateAtomicSimpleTypeValue(XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		String validationMessage = null;
		
		// If the base value is a primitive data type, validate it
		XSDSimpleTypeDefinition rootXSDSimpleTypeDefinition = xsdSimpleTypeDefinition.getRootType().getSimpleType();
		validationMessage = validatePrimitiveTypeValue(rootXSDSimpleTypeDefinition, value);
		if(validationMessage != null) {
			return validationMessage;
		}
		
		// Validate facets
		Iterator iterator = xsdSimpleTypeDefinition.getFacets().iterator();
		while(iterator.hasNext()) {
			XSDFacet xsdFacet = (XSDFacet)iterator.next();
			Integer mapValue = (Integer) facetsMap.get(xsdFacet.getFacetName());
			if(mapValue != null) {
				switch(mapValue.intValue()) {
				case length:
					XSDLengthFacet xsdLengthFacet = (XSDLengthFacet)xsdFacet;
					validationMessage = validateLengthFacet(xsdLengthFacet.getValue(), value);
					break;
				case minLength:
					XSDMinLengthFacet xsdMinLengthFacet = (XSDMinLengthFacet)xsdFacet;
					validationMessage = validateMinLengthFacet(xsdMinLengthFacet, value);
					break;		
				case maxLength:
					XSDMaxLengthFacet xsdMaxLengthFacet = (XSDMaxLengthFacet)xsdFacet;
					validationMessage = validateMaxLengthFacet(xsdMaxLengthFacet, value);
					break;	
				case pattern:
					XSDPatternFacet xsdPatternFacet = (XSDPatternFacet)xsdFacet;
					validationMessage = validatePatternFacet(xsdPatternFacet, value);
					break;
				case enumeration:
					XSDEnumerationFacet xsdEnumerationFacet = (XSDEnumerationFacet)xsdFacet;
					validationMessage = validateEnumerationFacet(xsdEnumerationFacet, xsdSimpleTypeDefinition, value);
					break;
				case whiteSpace: break;
				case maxInclusive:
					XSDMaxInclusiveFacet xsdMaxInclusiveFacet = (XSDMaxInclusiveFacet)xsdFacet;
					validationMessage = validateBoundFacet(true, true, xsdMaxInclusiveFacet.getValue(), xsdSimpleTypeDefinition, value);
					break;
				case maxExclusive:
					XSDMaxExclusiveFacet xsdMaxExclusiveFacet = (XSDMaxExclusiveFacet)xsdFacet;
					validationMessage = validateBoundFacet(true, false, xsdMaxExclusiveFacet.getValue(), xsdSimpleTypeDefinition, value);
					break;
				case minInclusive:
					XSDMinInclusiveFacet xsdMinInclusiveFacet = (XSDMinInclusiveFacet)xsdFacet;
					validationMessage = validateBoundFacet(false, true, xsdMinInclusiveFacet.getValue(), xsdSimpleTypeDefinition, value);
					break;
				case minExclusive:
					XSDMinExclusiveFacet xsdMinExclusiveFacet = (XSDMinExclusiveFacet)xsdFacet;
					validationMessage = validateBoundFacet(false, false, xsdMinExclusiveFacet.getValue(), xsdSimpleTypeDefinition, value);
					break;
				case fractionDigits:
					XSDFractionDigitsFacet xsdFractionDigitsFacet = (XSDFractionDigitsFacet)xsdFacet;
					validationMessage = validateFractionDigitsFacet(xsdFractionDigitsFacet, value);
					break;
				}
				if(validationMessage != null) {
					return validationMessage;
				}
			}
		}
		return null;
	}
		
	
	private String validateLengthFacet(int facetValue, String value) {
		if(value.length() != facetValue) {
			if(facetValue == 1) {
				return Messages.THE_VALUE_MUST_BE_1_CHARACTER_LONG;
			} else {
				MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_BE_X_CHARACTERS_LONG);
				return messageFormat.format(new String[] {"" + facetValue});
			}
		}
		return null;
	}
		
	
	private String validateMaxLengthFacet(XSDMaxLengthFacet xsdMaxLengthFacet, String value) {
		int facetValue = xsdMaxLengthFacet.getValue();
		if(value.length() > facetValue) {
			if(facetValue == 1) {
				return Messages.THE_VALUE_CANNOT_BE_MORE_THAN_1_CHARACTER_LONG;
			} else {
				MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_CANNOT_BE_MORE_THAN_X_CHARACTERS_LONG);
				return messageFormat.format(new String[] {"" + facetValue});
			}
		}
		return null;
	}
	
	
	private String validateMinLengthFacet(XSDMinLengthFacet xsdMinLengthFacet, String value) {
		int facetValue = xsdMinLengthFacet.getValue();
		if(value.length() < facetValue) {
			if(facetValue == 1) {
				return Messages.THE_VALUE_MUST_BE_AT_LEAST_1_CHARACTER_LONG;
			} else {
				MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_BE_AT_LEAST_X_CHARACTERS_LONG);
				return messageFormat.format(new String[] {"" + facetValue});
			}
		}
		return null;
	}
	
	
	private String validatePatternFacet(XSDPatternFacet xsdPatternFacet, String value) {
		Iterator patternIterator = xsdPatternFacet.getValue().iterator();
		while(patternIterator.hasNext()) {
			String pattern = (String)patternIterator.next();
			RegularExpression regularExpression = new RegularExpression(pattern, "X");
			if(!regularExpression.matches(value)) {
				MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_CONFORM_TO_THE_XML_SCHEMA_PATTERN_X);
				return messageFormat.format(new String[] {pattern});
			}
		}
		return null;
	}
	
	
	private String validateEnumerationFacet(XSDEnumerationFacet xsdEnumerationFacet, XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		Iterator iterator = xsdEnumerationFacet.getValue().iterator();
		while(iterator.hasNext()) {
			Object facetObject = iterator.next();
			if(valueMatches(xsdSimpleTypeDefinition, facetObject, value)) {
				return null;
			}
		}
		if(xsdEnumerationFacet.getValue().size() == 1) {
			MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_BE_X);
			return messageFormat.format(new String[] {xsdEnumerationFacet.getValue().get(0).toString()});
		} 
		return Messages.THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_OPTIONS;
	}
	
	private boolean valueMatches(XSDSimpleTypeDefinition xsdSimpleTypeDefinition, Object facetObject, String value) {
		Object valueObject = null;
		try {
			if(facetObject instanceof String) {
				valueObject = value;
			} else if(facetObject instanceof Boolean) {
				valueObject = new Boolean(value);
			} else if(facetObject instanceof Float) {
				valueObject = new Float(value);
			} else if(facetObject instanceof Double) {
				valueObject = new Double(value);
			} else if(facetObject instanceof BigDecimal) {
				valueObject = new BigDecimal(value);
			} else if(facetObject instanceof Duration) {
				try {
					valueObject = datatypeFactory.newDuration(value);
				} catch(Exception e){}
			} else if(facetObject instanceof XMLGregorianCalendar) {
				try {
					valueObject = datatypeFactory.newXMLGregorianCalendar(value);
				} catch(Exception e){}
			}
		} catch (Exception exception) {}
		if(facetObject.equals(valueObject)) {
			return true;
		}
		return false;
	}
		
	
	private String validateBoundFacet(boolean maxTrueMinFalse, boolean inclusiveTrueExclusiveFalse, Object facetValue, XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		String boundValue = null;
		int comparisonCoefficient = maxTrueMinFalse? 1 : -1;
		if(facetValue instanceof Float) {
			Float facetFloat = (Float)facetValue;
			int comparison = facetFloat.compareTo(new Float(value));
			if(comparison * comparisonCoefficient < 0 || (!inclusiveTrueExclusiveFalse && comparison == 0)) {
				boundValue = facetFloat.toString();
			}
		} else if(facetValue instanceof Double) {
			Double facetDouble = (Double)facetValue;
			int comparison = facetDouble.compareTo(new Double(value));
			if(comparison * comparisonCoefficient < 0 || (!inclusiveTrueExclusiveFalse && comparison == 0)) {
				boundValue = facetDouble.toString();
			}
		} else if(facetValue instanceof BigDecimal) {
			BigDecimal facetBigDecimal = (BigDecimal)facetValue;
			int comparison = facetBigDecimal.compareTo(new BigDecimal(value));
			if(comparison * comparisonCoefficient < 0 || (!inclusiveTrueExclusiveFalse && comparison == 0)) {
				boundValue = facetBigDecimal.toString();
			}
		} else if(facetValue instanceof Duration) {
			try {
				Duration facetXMLDuration = (Duration)facetValue;
				int comparison = facetXMLDuration.compare(datatypeFactory.newDuration(value));
				if(comparison * comparisonCoefficient < 0 || (!inclusiveTrueExclusiveFalse && comparison == 0)) {
					boundValue = facetXMLDuration.toString();
				}
			} catch(Exception e){}
		} 
		else if(facetValue instanceof XMLGregorianCalendar) {
			try {
				XMLGregorianCalendar facetXMLCalendar = (XMLGregorianCalendar)facetValue;
				int comparison = facetXMLCalendar.compare(datatypeFactory.newXMLGregorianCalendar(value));
				if(comparison * comparisonCoefficient < 0 || (!inclusiveTrueExclusiveFalse && comparison == 0)) {
					boundValue = facetXMLCalendar.toString();
				}
			} catch(Exception e){}
		}
		if(boundValue != null) {
			if(maxTrueMinFalse) {
				if(inclusiveTrueExclusiveFalse) {
					MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_CANNOT_BE_GREATER_THAN_X);
					return messageFormat.format(new String[] {boundValue});
				} else {
					MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_BE_SMALLER_THAN_X);
					return messageFormat.format(new String[] {boundValue});
				}
			} else {
				if(inclusiveTrueExclusiveFalse) {
					MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_CANNOT_BE_SMALLER_THAN_X);
					return messageFormat.format(new String[] {boundValue});
				} else {
					MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_MUST_BE_GREATER_THAN_X);
					return messageFormat.format(new String[] {boundValue});
				}
			}
		}
		return null;
	}
		
	
	private String validateFractionDigitsFacet(XSDFractionDigitsFacet xsdFractionDigitsFacet, String value) {
		int facetFractionDigits = xsdFractionDigitsFacet.getValue();
		int decimalSeparator = value.indexOf('.');
		if(decimalSeparator != -1) {
			int valueFractionDigits = value.substring(decimalSeparator +  1).length();
			if(valueFractionDigits > facetFractionDigits) {
				if(facetFractionDigits == 0) {
					return Messages.THE_VALUE_CANNOT_CONTAIN_FRACTIONAL_DIGITS;
				} else {
					if(facetFractionDigits == 1) {
						return Messages.THE_VALUE_CANNOT_CONTAIN_MORE_THAN_1_FRACTIONAL_DIGIT;
					} else {
						MessageFormat messageFormat = new MessageFormat(Messages.THE_VALUE_CANNOT_CONTAIN_MORE_THAN_X_FRACTIONAL_DIGIT);
						return messageFormat.format(new String[] {"" + facetFractionDigits});
					}
				}
			}
		}
		return null;
	}


	private String validatePrimitiveTypeValue(XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		Integer mapValue = (Integer) dataTypesMap.get(xsdSimpleTypeDefinition.getName());
		if(mapValue != null) {
			switch(mapValue.intValue()) {
			case QName:					return null;
			case string:				return null;
			case decimal:				return validateDecimal(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_decimal;
			case date:					return validateDate(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_date;
			case dateTime:				return validateDateTime(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_dateTime;
			case duration:				return validateDuration(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_duration;
			case gDay:					return validateGDay(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_gDay;
			case gMonth:				return validateGMonth(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_gMonth;
			case gMonthDay:				return validateGMonthDay(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_gMonthDay;
			case gYear:					return validateGYear(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_gYear;
			case gYearMonth:			return validateGYearMonth(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_gYearMonth;
			case time:					return validateTime(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_time;
			case anyURI:				return null;
			case base64Binary:			return null;
			case _boolean:				return validateBoolean(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_boolean;
			case _double:				return validateDouble(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_double;
			case _float:				return validateFloat(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_float;
			case hexBinary:				return validateHexBinary(value)? null : Messages.DATA_TYPE_VALIDATION_INVALID_hexBinary;
			case NOTATION:				return null;
			}
		}
		return null;
	}
	
	
	private String validateListTypeValue(XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		StringTokenizer stringTokenizer = new StringTokenizer(value, " ");
		String[] listValues = new String[stringTokenizer.countTokens()];
		int i = 0;
		while(stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			String validationMessage = validateAtomicSimpleTypeValue(xsdSimpleTypeDefinition.getItemTypeDefinition(), token);
			if(validationMessage != null) {
				MessageFormat messageFormat = new MessageFormat(Messages.LIST_ITEM_X);
				return messageFormat.format(new String[] {"" + (i + 1)}) + " " + validationMessage;
			}
			listValues[i++] = token;
		}
		Iterator iterator = xsdSimpleTypeDefinition.getFacets().iterator();
		while(iterator.hasNext()) {
			XSDFacet xsdFacet = (XSDFacet)iterator.next();
			Integer mapValue = (Integer)facetsMap.get(xsdFacet.getFacetName());
			if(mapValue != null) {
				switch(mapValue.intValue()) {
				case length:
					XSDLengthFacet xsdLengthFacet = (XSDLengthFacet)xsdFacet;
					int facetLengthValue = xsdLengthFacet.getValue();
					if(facetLengthValue != i) {
						if(facetLengthValue == 1) {
							return Messages.THE_LIST_MUST_CONTAIN_1_ITEM;
						} else {
							MessageFormat messageFormat = new MessageFormat(Messages.THE_LIST_MUST_CONTAIN_X_ITEMS);
							return messageFormat.format(new String[] {"" + facetLengthValue});
						}
					}
					break;
				case minLength:
					XSDMinLengthFacet xsdMinLengthFacet = (XSDMinLengthFacet)xsdFacet;
					int facetMinLengthValue = xsdMinLengthFacet.getValue();
					if(facetMinLengthValue > i) {
						if(facetMinLengthValue == 1) {
							return Messages.THE_LIST_MUST_CONTAIN_AT_LEAST_1_ITEM;
						} else {
							MessageFormat messageFormat = new MessageFormat(Messages.THE_LIST_MUST_CONTAIN_AT_LEAST_X_ITEMS);
							return messageFormat.format(new String[] {"" + facetMinLengthValue});
						}
					}
					break;
				case maxLength:
					XSDMaxLengthFacet xsdMaxLengthFacet = (XSDMaxLengthFacet)xsdFacet;
					int facetMaxLengthValue = xsdMaxLengthFacet.getValue();
					if(facetMaxLengthValue < i) {
						if(facetMaxLengthValue == 1) {
							return Messages.THE_LIST_MUST_CONTAIN_AT_MOST_1_ITEM;
						} else {
							MessageFormat messageFormat = new MessageFormat(Messages.THE_LIST_MUST_CONTAIN_AT_MOST_X_ITEMS);
							return messageFormat.format(new String[] {"" + facetMaxLengthValue});
						}
					}
					break;
				case enumeration:
					XSDEnumerationFacet xsdEnumerationFacet = (XSDEnumerationFacet)xsdFacet;
					String validationMessage = validateListEnumerationFacet(xsdEnumerationFacet, value);
					if(validationMessage != null) {
						return validationMessage;
					}
					break;
				case pattern:
					XSDPatternFacet xsdPatternFacet = (XSDPatternFacet)xsdFacet;
					validationMessage = validatePatternFacet(xsdPatternFacet, value);
					if(validationMessage != null) {
						return validationMessage;
					}
					break;
				}
			}
		}
		return null;
	}
	
	
	private String validateListEnumerationFacet(XSDEnumerationFacet xsdEnumerationFacet, String value) {
		StringTokenizer stringTokenizer = new StringTokenizer(value, " ");
		String[] listValues = new String[stringTokenizer.countTokens()];
		int i = 0;
		while(stringTokenizer.hasMoreTokens()) {
			listValues[i++] = stringTokenizer.nextToken();
		}
		Iterator facetEnumerationIterator = xsdEnumerationFacet.getValue().iterator();
		while(facetEnumerationIterator.hasNext()) {
			List facetEnumerationValueList = (List) facetEnumerationIterator.next();
			if(facetEnumerationValueList.size() == listValues.length) {
				Iterator facetEnumerationValueListIterator = facetEnumerationValueList.iterator();
				i = 0;
				while(facetEnumerationValueListIterator.hasNext()) {
					Object facetObject = facetEnumerationValueListIterator.next();
					if(!valueMatches(xsdEnumerationFacet.getSimpleTypeDefinition().getItemTypeDefinition(), facetObject, listValues[i])) {
						break;
					}
					i++;
				}
				if(facetEnumerationValueList.size() == i) {
					return null;
				}
			}
		}
		return Messages.THE_VALUE_IS_NOT_AMONG_THE_POSSIBLE_LISTS;
	}
	
	
	private String validateUnionTypeValue(XSDSimpleTypeDefinition xsdSimpleTypeDefinition, String value) {
		
		// Ensure the value is valid (one of the types in the union)
		EList memberTypeDefinitions = xsdSimpleTypeDefinition.getMemberTypeDefinitions();
		Iterator memberTypeDefinitionsIterator = memberTypeDefinitions.iterator();
		boolean validTypeFound = false;
		String msg = null;
		StringBuffer sb = new StringBuffer(Messages.DATA_TYPE_VALIDATION_INVALID_UNION);  //$NON-NLS-1$
		// We should gather up the messages to suggest to the user what they can do to fix the error
		int i = 0;
		while(!validTypeFound && memberTypeDefinitionsIterator.hasNext()) {
			XSDTypeDefinition memberTypeDefinition = (XSDTypeDefinition) memberTypeDefinitionsIterator.next();
			msg = validateXSDTypeDefinition(memberTypeDefinition, value);
			if (msg != null)
			{
				i++;
				sb.append("\n");
			    sb.append(Messages.bind(Messages.VALIDATION_MESSAGE, i, msg));
			}
			if(msg == null) {
				validTypeFound = true;
			}
		}
		if(!validTypeFound) {
			return sb.toString();
		}
		
		// Validate Facets
		Iterator iterator = xsdSimpleTypeDefinition.getFacets().iterator();
		while(iterator.hasNext()) {
			XSDFacet xsdFacet = (XSDFacet)iterator.next();
			Integer mapValue = (Integer) facetsMap.get(xsdFacet.getFacetName());
			if(mapValue != null) {
				switch(mapValue.intValue()) {
				case enumeration:
					XSDEnumerationFacet xsdEnumerationFacet = (XSDEnumerationFacet)xsdFacet;
					String validationMessage = validateEnumerationFacet(xsdEnumerationFacet, xsdSimpleTypeDefinition, value);
					if(validationMessage != null) {
						return validationMessage;
					}
					break;
				case pattern:
					XSDPatternFacet xsdPatternFacet = (XSDPatternFacet)xsdFacet;
					validationMessage = validatePatternFacet(xsdPatternFacet, value);
					if(validationMessage != null) {
						return validationMessage;
					}
					break;
				}
			}
		}
		return null;
	}
	
	
	private boolean validateDecimal(String value) {
		if(value.length() > 0) {
			if(value.charAt(0) == '+' || value.charAt(0) == '-') {
				if(value.length() == 1) {
					return false;
				}
				value = value.substring(1);
			}
			boolean dotPresent = false;
			while(value.length() > 0) {
				char currentChar = value.charAt(0);
				if(currentChar == '.') {
					if(dotPresent) {
						return false;
					} else {
						dotPresent = true;
					}
				} else if(!Character.isDigit(currentChar)) {
					return false;
				}
				value = value.substring(1);
			}
		} else {
			return false;
		}
		return true;
	}
	
		

	
	private boolean validateDate(String value) {
		if(value.length() > 0) {
			if(value.charAt(0) == '-') {
				value = value.substring(1);
			}
			if(value.length() < 10) {
				return false;
			}
			if(value.charAt(4) != '-' || value.charAt(7) != '-') {
				return false;
			}
			try {
				int year = Integer.parseInt(value.substring(0, 4));
				int month = Integer.parseInt(value.substring(5, 7));
				int day = Integer.parseInt(value.substring(8, 10));
				GregorianCalendar gregorianCalendar = new GregorianCalendar();
	            gregorianCalendar.setLenient(false); 
	            gregorianCalendar.set(GregorianCalendar.YEAR, year);
	            gregorianCalendar.set(GregorianCalendar.MONTH, month - 1);
	            gregorianCalendar.set(GregorianCalendar.DATE, day);
	            gregorianCalendar.getTime();
			} catch (Exception exception) {return false;}
			if(value.length() == 11) {
				if(value.charAt(10) != 'Z') {
					return false;
				}
			} else if(value.length() == 16) {
				if(!((value.charAt(10) == '+' || value.charAt(10) == '-') && value.charAt(13) == ':')) {
					return false;
				}
				try {
					int hours = Integer.parseInt(value.substring(11, 13));
					int minutes = Integer.parseInt(value.substring(14, 16));
					if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
						return false;
					}
				} catch (Exception exception) {return false;}
			} else if(value.length() != 10) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateDateTime(String value) {
		if(value.length() > 0) {
			if(value.charAt(0) == '-') {
				value = value.substring(1);
			}
			if(value.length() < 19) {
				return false;
			}
			if(value.charAt(4) != '-' || value.charAt(7) != '-' || value.charAt(10) != 'T' || value.charAt(13) != ':' || value.charAt(16) != ':') {
				return false;
			}
			try {
				int year = Integer.parseInt(value.substring(0, 4));
				int month = Integer.parseInt(value.substring(5, 7));
				int day = Integer.parseInt(value.substring(8, 10));
				int hours = Integer.parseInt(value.substring(11, 13));
				int minutes = Integer.parseInt(value.substring(14, 16));
				int seconds = Integer.parseInt(value.substring(17, 19));
				if(hours > 24 || minutes > 59 || seconds > 59 || hours < 0 || minutes < 0 || seconds < 0) {
					return false;
				}
				GregorianCalendar gregorianCalendar = new GregorianCalendar();
	            gregorianCalendar.setLenient(false); 
	            gregorianCalendar.set(GregorianCalendar.YEAR, year);
	            gregorianCalendar.set(GregorianCalendar.MONTH, month - 1);
	            gregorianCalendar.set(GregorianCalendar.DATE, day);
	            gregorianCalendar.getTime();
			} catch (Exception exception) {return false;}
			if(value.length() > 19) {
				value = value.substring(19);
				if(value.charAt(0) == '.') {
					value = value.substring(1);
					boolean secondFractionFound = false;
					while(value.length() > 0 && Character.isDigit(value.charAt(0))) {
						secondFractionFound = true;
						value = value.substring(1);
					}
					if(!secondFractionFound) {
						return false;
					} 
				}
				if(value.length() == 1) {
					if(value.charAt(0) != 'Z') {
						return false;
					}
				} else if(value.length() == 6) {
					if(!((value.charAt(0) == '+' || value.charAt(0) == '-') && value.charAt(3) == ':')) {
						return false;
					}
					try {
						int hours = Integer.parseInt(value.substring(1, 3));
						int minutes = Integer.parseInt(value.substring(4, 6));
						if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
							return false;
						}
					} catch (Exception exception) {return false;}
				} else if(value.length() != 0) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateDuration(String value) {
		if(value.length() > 1) {
			if(value.charAt(0) != 'P') {
				return false;
			}
			int timeToken = value.indexOf('T');
			String date = null;
			String time = null;
			if(timeToken != -1) {
				date = value.substring(1, timeToken);
				time = value.substring(timeToken + 1, value.length());
			} else {
				date = value.substring(1);
			}
			boolean yearFound = false;
			boolean monthFound = false;
			boolean dayFound = false;
			boolean numberJustParsed = false;
			while(date.length() > 0) {
				char currentChar = date.charAt(0);
				
				if(Character.isDigit(currentChar)) {
					numberJustParsed = true;
				} else {
					if(!numberJustParsed) {
						return false;
					}
					switch(currentChar) {
					case 'Y':
						if(yearFound || monthFound || dayFound) {
							return false;
						} else {
							yearFound = true;
						}
						break;
					case 'M':
						if(monthFound || dayFound) {
							return false;
						} else {
							monthFound = true;
						}
						break;
					case 'D':
						if(dayFound) {
							return false;
						} else {
							dayFound = true;
						}
						break;
					default:
						return false;
					}
					numberJustParsed = false;
				}
				date = date.substring(1);
			}
			if(numberJustParsed) {
				return false;
			}
			if(time != null) {
				boolean hoursFound = false;
				boolean minutesFound = false;
				boolean secondFractionFound = false;
				boolean secondsFound = false;
				while(time.length() > 0) {
					char currentChar = time.charAt(0);
					if(Character.isDigit(currentChar)) {
						numberJustParsed = true;
					} else {
						if(!numberJustParsed) {
							return false;
						}
						switch(currentChar) {
						case 'H':
							if(hoursFound || minutesFound || secondFractionFound || secondsFound) {
								return false;
							} else {
								hoursFound = true;
							}
						case 'M':
							if(minutesFound || secondFractionFound || secondsFound) {
								return false;
							} else {
								minutesFound = true;
							}
						case '.':
							if(secondFractionFound || secondsFound) {
								return false;
							} else {
								secondFractionFound = true;
							}
						case 'S':
							if(secondsFound) {
								return false;
							} else {
								secondsFound = true;
							}
						}
						numberJustParsed = false;
					}
					time = time.substring(1);
				}
			}
			if(numberJustParsed) {
				return false;
			}

		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateGDay(String value) {
		if(value.length() > 4) {
			if(!(value.charAt(0) == '-' && value.charAt(1) == '-' && value.charAt(2) == '-')) {
				return false;
			}
			try {
				int day = Integer.parseInt(value.substring(3, 5));
				if(day > 31 || day < 1) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else {
			return false;
		}
		if(value.length() == 6) {
			if(value.charAt(5) != 'Z') {
				return false;
			}
		} else if(value.length() == 11) {
			if(!((value.charAt(5) == '+' || value.charAt(5) == '-') && value.charAt(8) == ':')) {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(6, 8));
				int minutes = Integer.parseInt(value.substring(9, 11));
				if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else if(value.length() != 5) {
			return false;
		}
		return true;
	}
	
	
	private boolean validateGMonth(String value) {
		if(value.length() > 3) {
			if(!(value.charAt(0) == '-' && value.charAt(1) == '-')) {
				return false;
			}
			try {
				int month = Integer.parseInt(value.substring(2, 4));
				if(month > 12 || month < 1) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else {
			return false;
		}
		
		value = value.substring(4);
		
		if(value.length() > 1 && value.charAt(0) == '-') {
			if(value.charAt(1) == '-') {
				value = value.substring(2);
			} else {
				return false;
			}
		}

		
		if(value.length() == 1) {
			if(value.charAt(0) != 'Z') {
				return false;
			}
		} else if(value.length() == 6) {
			if(!((value.charAt(0) == '+' || value.charAt(0) == '-') && value.charAt(3) == ':')) {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(1, 3));
				int minutes = Integer.parseInt(value.substring(4, 6));
				if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else if(value.length() != 0) {
			return false;
		}
		return true;
	}
	
	
	private boolean validateGMonthDay(String value) {
		if(value.length() > 5) {
			if(!(value.charAt(0) == '-' && value.charAt(1) == '-' && value.charAt(4) == '-')) {
				return false;
			}
			try {
				int month = Integer.parseInt(value.substring(2, 4));
				int day = Integer.parseInt(value.substring(5, 7));
				if(month > 12 || month < 1 || day > 31 || day < 1) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else {
			return false;
		}
		if(value.length() == 8) {
			if(value.charAt(7) != 'Z') {
				return false;
			}
		} else if(value.length() == 13) {
			if(!((value.charAt(7) == '+' || value.charAt(7) == '-') && value.charAt(10) == ':')) {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(8, 10));
				int minutes = Integer.parseInt(value.substring(11, 13));
				if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else if(value.length() != 7) {
			return false;
		}
		return true;
	}
	
	
	private boolean validateGYear(String value) {
		if(value.length() > 3) {
			if(value.charAt(0) == '-') {
				value = value.substring(1);
			}
			try {
				int year = Integer.parseInt(value.substring(0, 4));
				if(year < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else {
			return false;
		}
		if(value.length() == 5) {
			if(value.charAt(4) != 'Z') {
				return false;
			}
		} else if(value.length() == 10) {
			if(!((value.charAt(4) == '+' || value.charAt(4) == '-') && value.charAt(7) == ':')) {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(5, 7));
				int minutes = Integer.parseInt(value.substring(8, 10));
				if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else if(value.length() != 4) {
			return false;
		}
		return true;
	}
	
	
	private boolean validateGYearMonth(String value) {
		if(value.length() > 6) {
			if(value.charAt(0) == '-') {
				value = value.substring(1);
			}
			if(value.charAt(4) != '-') {
				return false;
			}
			try {
				int year = Integer.parseInt(value.substring(0, 4));
				int month = Integer.parseInt(value.substring(5, 7));
				if(year < 0 || month > 12 || month < 1) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else {
			return false;
		}
		if(value.length() == 8) {
			if(value.charAt(7) != 'Z') {
				return false;
			}
		} else if(value.length() == 13) {
			if(!((value.charAt(7) == '+' || value.charAt(7) == '-') && value.charAt(10) == ':')) {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(8, 10));
				int minutes = Integer.parseInt(value.substring(11, 13));
				if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
		} else if(value.length() != 7) {
			return false;
		}
		return true;
	}
	
	
	private boolean validateTime(String value) {
		if(value.length() > 0) {
			if(value.length() < 8) {
				return false;
			}
			if(value.charAt(2) != ':' || value.charAt(5) != ':') {
				return false;
			}
			try {
				int hours = Integer.parseInt(value.substring(0, 2));
				int minutes = Integer.parseInt(value.substring(3, 5));
				int seconds = Integer.parseInt(value.substring(6, 8));
				if(hours > 24 || minutes > 59 || seconds > 59 || hours < 0 || minutes < 0 || seconds < 0) {
					return false;
				}
			} catch (Exception exception) {return false;}
			if(value.length() > 8) {
				value = value.substring(8);
				if(value.charAt(0) == '.') {
					value = value.substring(1);
					boolean secondFractionFound = false;
					while(value.length() > 0 && Character.isDigit(value.charAt(0))) {
						secondFractionFound = true;
						value = value.substring(1);
					}
					if(!secondFractionFound) {
						return false;
					} 
				}
				if(value.length() == 1) {
					if(value.charAt(0) != 'Z') {
						return false;
					}
				} else if(value.length() == 6) {
					if(!((value.charAt(0) == '+' || value.charAt(0) == '-') && value.charAt(3) == ':')) {
						return false;
					}
					try {
						int hours = Integer.parseInt(value.substring(1, 3));
						int minutes = Integer.parseInt(value.substring(4, 6));
						if(hours > 13 || minutes > 59 || hours < 0 || minutes < 0) {
							return false;
						}
					} catch (Exception exception) {return false;}
				} else if(value.length() != 0) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
		
	private boolean validateDouble(String value) {
		if(value.length() > 0) {
			if(!("INF".equals(value) || "-INF".equals(value) || "NaN".equals(value))) {
				try {
					Double.parseDouble(value);
				} catch (Exception exception) {return false;}
			}
		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateFloat(String value) {
		if(value.length() > 0) {
			if(!("INF".equals(value) || "-INF".equals(value) || "NaN".equals(value))) {
				try {
					Float.parseFloat(value);
				} catch (Exception exception) {return false;}
			}
		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateHexBinary(String value) {
		if(value.length() % 2 == 0) {
			for (int i = 0; i < value.length(); i++) {
				char currentChar = value.charAt(i);
				if(!(currentChar == 'a' || currentChar == 'b' || currentChar == 'c' || currentChar == 'd' || currentChar == 'e' || currentChar == 'f' || Character.isDigit(currentChar))) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	
	private boolean validateBoolean(String value) {
		if(!("true".equals(value) || "1".equals(value) || "false".equals(value) || "0".equals(value))) {
			return false;
		}
		return true;
	}

}
/*
   Licensed to Plutext Pty Ltd under one or more contributor license agreements.  
   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */
package org.docx4j.model.fields;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.RPr;

/**
 * Formats the string value of the field according to the three possible formatting switches:
 * 
 * + date-and-time-formatting-switch: \@ 
 * + numeric-formatting-switch: \#
 * + general-formatting-switch: \*
 * 
 * Note that the general-formatting-switch arguments CHARFORMAT and MERGEFORMAT are not handled here. 
 * It is the responsibility of the calling code to handle these.
 * 
 * TODO: use <w:decimalSymbol w:val="."/><w:listSeparator w:val=","/> from DocumentSettingsPart.
 * 
 * @author alberto, jharrop
 *
 */
public class FormattingSwitchHelper {
	
	private static Logger log = Logger.getLogger(FormattingSwitchHelper.class);		
	
	/* http://office.microsoft.com/en-us/word-help/insert-and-format-field-codes-in-word-2010-HA101830917.aspx
	 * Switches:
	 * 
	 * Format switches (\*):
	 * Capitalization formats
     * \* Caps        Capitalizes the first letter of each word.
     * \* FirstCap    Capitalizes the first letter of the first word.
     * \* Upper       Capitalizes all letters.
     * \* Lower       Capitalizes none of the result; all letters are lowercase.
	 *
	 * Number formats
     * \* alphabetic   Lowercase alphabetic characters (a, b, c).
     * \* ALPHABETIC   Uppercase alphabetic characters (A, B, C).
     * \* Arabic       Arabic cardinal numerals (1, 2, 3).
	 * \* ArabicDash   Only PAGE! Arabic cardinal numerals surrounded by hyphen characters (- 1 -, - 2 -, - 3 -). 	
     * \* CardText     Lowercase Cardinal text (can be combined with capitalization formats).  (one, two, three).
     * \* DollarText   ........ For example, { = 9.20 + 5.35 \* DollarText \* Upper } displays FOURTEEN AND 55/100.
     * \* Hex          Hexadecimal numbers. 
     * \* OrdText      Lowercase Ordinal text (can be combined with capitalization formats).  (first, second, third)
     * \* Ordinal      Ordinal Arabic numerals (1st, 2nd, 3rd).  
     * \* roman        Lowercase Roman numerals (i, ii, iii). 
     * \* ROMAN        Uppercase Roman numerals (I, II, III). 
	 * 
	 * Character formats and protecting previously applied formats
	 * \* Charformat   This switch applies the formatting of the first letter of the field name to the entire result.  
	 * \* MERGEFORMAT  This switch applies the formatting of the previous result to the new result. 
	 * 
	 * Numeric format switch (\#):
	 * 
	 * 0 (zero)    This format item specifies the requisite numeric places to display in the result. If the result does not include a digit in that place, Word displays a 0 (zero). 
	 * #    This format item specifies the requisite numeric places to display in the result. If the result does not include a digit in that place, Word displays a space. 
	 * x    This format item drops digits to the left of the "x" placeholder. If the placeholder is to the right of the decimal point, Word rounds the result to that place.
	 * . (decimal point) This format item determines the decimal point position. (Use the decimal symbol that is specified as part of the regional settings in Control Panel.)
	 * , (digit grouping symbol) This format item separates a series of three digits. (Use the digit grouping symbol that is specified as part of the regional settings in Control Panel.)
	 * - (minus sign) This format item adds a minus sign to a negative result or adds a space if the result is positive or 0 (zero).
	 * + (plus sign) This format item adds a plus sign to a positive result, a minus sign to a negative result, or a space if the result is 0 (zero).
	 * %, $, *, and so on This format item includes the specified character in the result. 
	 * positive; negative[; zero] This format item specifies different number formats for a positive result, a negative result, and a 0 (zero) result, separated by semicolons. 
	 * `numbereditem` This format item displays the number of the preceding item that you numbered by using the Caption command (References tab, Captions group) or by inserting a SEQ field. 
	 *
	 * 
	 * 
	 * Date Time format switch (\@):
	 * Quotation marks are not required around simple date-time formats that do not include spaces or text
	 * 
	 * Month
     * M         Number without a leading 0 (zero) for single-digit months.
     * MM        Number with a leading 0 (zero) for single-digit months.
     * MMM       Three-letter abbreviation.
     * MMMM      Full name.
	 *
	 * Day
	 * d         Day of the month as a number without a leading 0 (zero) for single-digit days.
     * dd    	 Day of the month as a number with a leading 0 (zero) for single-digit days.
     * ddd       Day of the week as three-letter abbreviation.
     * dddd      Day of the week as its full name.
     * 
 	 * Year
	 * yy    	 Year as two digits with a leading 0 (zero) for years 01 through 09.
     * yyyy    	 Year as four digits.
	 * 
	 * Hours
	 * h        (12h) Hour without a leading 0 (zero) for single-digit hours.
	 * H        (24h) Hour without a leading 0 (zero) for single-digit hours.
     * hh 		(12h) Hour with a leading 0 (zero) for single-digit hours.
     * HH 		(24h) Hour with a leading 0 (zero) for single-digit hours.
	 * 
	 * Minutes
	 * m    	Minutes without a leading 0 (zero) for single-digit minutes.
     * mm       Minutes with a leading 0 (zero) for single-digit minutes.
	 * 
	 * Seconds
	 * s    	Seconds without a leading 0 (zero) for single-digit seconds.
     * ss       Seconds with a leading 0 (zero) for single-digit seconds.
	 * 
	 * AM/PM
	 * am/pm or AM/PM    Displays AM or PM as uppercase. 
	 * 
	 * Other Date Time items
	 * 'text'    Display any specified text in a date or time. Enclose the text in single quotation marks.
	 *           (How do you escape a single quote? - The example of the page above causes an error in word...)  
	 * character This format item includes the specified character in a date or time, such as a : (colon), - (hyphen), * (asterisk), or space. 
	 * `numbereditem`  Number of the preceding item that you numbered. 
	 * 
	 */
	
	protected static final ThreadLocal<SimpleDateFormat> DATE_FORMATS = new ThreadLocal();
	
	/** Conversion of page number formats to fo as defined 
	 * <ul>
	 * <li>in w:fldSimple</li>
	 * <li>in w:pgNumType w:fmt</li>
	 * </ul>
	 */
	protected static final String DEFAULT_FORMAT_PAGE_TO_FO = "1";
	//A marker for the NumberFormat.NONE - value
	protected static final String NONE_STRING = new String();
	protected static final Map<String, String> FORMAT_PAGE_TO_FO = new HashMap<String, String>();
	
	public static final int DECORATION_NONE = 0;
	public static final int DECORATION_DASH = 1;
	
	
	protected static final Map<String, String> DATE_FORMAT_ITEMS_TO_JAVA = new HashMap<String, String>();
	
	static {
		FORMAT_PAGE_TO_FO.put("Arabic", "1");
		FORMAT_PAGE_TO_FO.put("ArabicDash", "1");
		FORMAT_PAGE_TO_FO.put("alphabetic", "a");
		FORMAT_PAGE_TO_FO.put("ALPHABETIC", "A");
		FORMAT_PAGE_TO_FO.put("roman", "i");
		FORMAT_PAGE_TO_FO.put("ROMAN", "I");

		FORMAT_PAGE_TO_FO.put(NumberFormat.NONE.value(), NONE_STRING); 		//"none"
		FORMAT_PAGE_TO_FO.put(NumberFormat.DECIMAL.value(), "1"); 			// "decimal"
		FORMAT_PAGE_TO_FO.put(NumberFormat.NUMBER_IN_DASH.value(), "1");	//"numberInDash"
		FORMAT_PAGE_TO_FO.put(NumberFormat.LOWER_LETTER.value(), "a");		//"lowerLetter"
		FORMAT_PAGE_TO_FO.put(NumberFormat.UPPER_LETTER.value(), "A");		//"upperLetter"
		FORMAT_PAGE_TO_FO.put(NumberFormat.LOWER_ROMAN.value(), "i");		//"lowerRoman"
		FORMAT_PAGE_TO_FO.put(NumberFormat.UPPER_ROMAN.value(), "I");		//"upperRoman"
		
		//Month
		DATE_FORMAT_ITEMS_TO_JAVA.put("M","M");			//Number without a leading 0 (zero) for single-digit months.
		DATE_FORMAT_ITEMS_TO_JAVA.put("MM","MM");		//Number with a leading 0 (zero) for single-digit months.
		DATE_FORMAT_ITEMS_TO_JAVA.put("MMM","MMM");		//Three-letter abbreviation.
		DATE_FORMAT_ITEMS_TO_JAVA.put("MMMM","MMMM");	//Full name.
		//Day
		DATE_FORMAT_ITEMS_TO_JAVA.put("d","d");			//Day of the month as a number without a leading 0 (zero) for single-digit days.
		DATE_FORMAT_ITEMS_TO_JAVA.put("dd","dd");		//Day of the month as a number with a leading 0 (zero) for single-digit days.
		DATE_FORMAT_ITEMS_TO_JAVA.put("ddd","EEE");		//Day of the week as three-letter abbreviation.
		DATE_FORMAT_ITEMS_TO_JAVA.put("dddd","EEEE");	//Day of the week as its full name.
		//Year
		DATE_FORMAT_ITEMS_TO_JAVA.put("yy","yy");		//Year as two digits with a leading 0 (zero) for years 01 through 09.
		DATE_FORMAT_ITEMS_TO_JAVA.put("yyyy","yyyy");	//Year as four digits.
		DATE_FORMAT_ITEMS_TO_JAVA.put("YYYY","yyyy");	//NOT IN SPEC Year as four digits.		
		//Hour
		DATE_FORMAT_ITEMS_TO_JAVA.put("h","K");			//(12h) Hour without a leading 0 (zero) for single-digit hours.
		DATE_FORMAT_ITEMS_TO_JAVA.put("H","H");			//(24h) Hour without a leading 0 (zero) for single-digit hours.
		DATE_FORMAT_ITEMS_TO_JAVA.put("hh","KK");		//(12h) Hour with a leading 0 (zero) for single-digit hours.
		DATE_FORMAT_ITEMS_TO_JAVA.put("HH","HH");		//(24h) Hour with a leading 0 (zero) for single-digit hours.
		//Minute
		DATE_FORMAT_ITEMS_TO_JAVA.put("m","m");			//Minutes without a leading 0 (zero) for single-digit minutes.
		DATE_FORMAT_ITEMS_TO_JAVA.put("mm","mm");		//Minutes with a leading 0 (zero) for single-digit minutes.
		//Second
		DATE_FORMAT_ITEMS_TO_JAVA.put("s","s");			//Seconds without a leading 0 (zero) for single-digit seconds.
		DATE_FORMAT_ITEMS_TO_JAVA.put("ss","ss");		//Seconds with a leading 0 (zero) for single-digit seconds.
		//AM/PM
		//DATE_FORMAT_ITEMS_TO_JAVA.put("am/pm","a");		//Displays AM or PM as uppercase.
		//DATE_FORMAT_ITEMS_TO_JAVA.put("AM/PM","a");		//Displays AM or PM as uppercase. 
		
	}
	
	public static String applyFormattingSwitch(FldSimpleModel model, String value) throws Docx4JException {

		// date-and-time-formatting-switch: \@ 
		Date date = null;
		try {
			date = getDate(model, value );
			String dtFormat = findFormat("\\@", model.getFldParameters());
			log.debug("Applying date format " + dtFormat + " to " + value);
			
			
			if (dtFormat==null) {
				// SPECIFICATION: If no date-and-time-formatting-switch is present, 
				// a date or time result is formatted in an implementation-defined manner.
				
				// TODO .. analyse what Word does
				// for now, just leave as is
			} else {
				value = formatDate(model, dtFormat, date);
			}
			
		} catch (FieldResultIsNotADateOrTimeException e) {
			// SPECIFICATION: If the result of a field is not a date or time,
			// the date-and-time-formatting-switch has no effect
		}
		
		// numeric-formatting-switch: \#
		double number;
		if (date==null) {
			// It is not a date, so see whether it is a number
			
			try {
				number = getNumber(model, value);
				String nFormat = findFormat("\\#", model.getFldParameters());
				log.debug("Applying number format " + nFormat + " to " + number);
				
				if (nFormat==null) {
					// SPECIFICATION: If no numeric-formatting-switch is present, 
					// a numeric result is formatted without leading spaces or
					// trailing fractional zeros.  
					// If the result is negative, a leading minus sign is present.  
					// If the result is a whole number, no radix point is present.
					
					// We'll only honour this if the number is really a number.
					// Not, for example, CL.87559-p
					try {
						number = Double.parseDouble(value);
						value = formatNumber(model, "#.##########", number );
					} catch (Exception e) {
						log.debug(value + " is not a number");				
					}
					
					
				} else {
					value = formatNumber(model, nFormat, number );
				}
				
			} catch (FieldResultIsNotANumberException e) {
				// SPECIFICATION: If the result of a field is not a number,
				// the numeric-formatting-switch has no effect
				
			}
			
		}
		
		// general-formatting-switch: \*
		// SPECIFICATION: A general-formatting-switch specifies a variety of
		// formats for a numeric or text result.  If the result type of a field
		// does not correspond to the format specified, this switch has no effect.
		
		//  Word 2010 can handle:
		//     \@ &quot;d 'de' MMMM 'de' yyyy  &quot; \* Upper"
		// (ie \@ and \* at same time)
		// but \@ must be before \*
		String gFormat = findFormat("\\*", model.getFldParameters());		
		value = gFormat==null ? value : formatGeneral(model, gFormat, value );
		
		
		log.debug("Result -> " + value + "\n");
		
		return value;
	}
	
	private static class FieldResultIsNotADateOrTimeException extends Exception {

		public FieldResultIsNotADateOrTimeException(){
			super();
		}
	}
	private static class FieldResultIsNotANumberException extends Exception {

		public FieldResultIsNotANumberException(){
			super();
		}
	}
	
	private static Date getDate(FldSimpleModel model, String dateStr) throws FieldResultIsNotADateOrTimeException {
		
		// eg 31/3/2013
		// Word's default format for date: 7/04/2013
		// and time: 11:05 AM		
		
		/* Word seems happy to parse dates in any of the following formats:
		 * 
		    <vt:lpwstr>13/4/2013</vt:lpwstr>
		    <vt:lpwstr>13/04/2013</vt:lpwstr>
		    <vt:lpwstr>13/04/13</vt:lpwstr>
		    <vt:lpwstr>13-04-2013</vt:lpwstr>
		    <vt:lpwstr>13/04/2013 11:05 AM</vt:lpwstr>
		    <vt:lpwstr>2013-08-19T00:00:00Z</vt:lpwstr>
		 * 
		 * Control Panel  > Region & Language > Formats determines whether Word 2010 interprets 11/12
		 * as 11 Dec, or 12 Nov.
		 * 
		 * But we'll use a docx4j property to control this. 
		 */
		
		// parse the string into a date.
		String inputFormat = DateFormatInferencer.determineDateFormat(dateStr);
		if (inputFormat==null) {
			log.debug("Unrecognised format; Can't parse " + dateStr);
			return null;
		} else {
			log.debug("Parsing with format: " + inputFormat);
			Date date = null;
			try {
				DateFormat dateTimeFormat = new SimpleDateFormat(inputFormat);
					// is this threadsafe in static method?
				
				return (Date)dateTimeFormat.parse(dateStr);
			} catch (ParseException e) {
				log.warn("Can't parse " + dateStr + " using format " + inputFormat);				
				throw new FieldResultIsNotADateOrTimeException();
			}
		}
	}

	private static double getNumber( FldSimpleModel model, String value) throws FieldResultIsNotANumberException {
		
		// Hmm, Word seems to be happy to extract a number from a DOCPROPERTY string,
		// but not from = ... so we should only use NumberExtractor for certain field types?

			// First, parse the value
			NumberExtractor nex = new NumberExtractor();
			try {
				value = nex.extractNumber(value);
			} catch (java.lang.IllegalStateException noMatch) {
				// There is no number in this string.
				// In this case Word just inserts the non-numeric text,
				// without attempting to format the number
				throw new FieldResultIsNotANumberException();
			}

			try {
				return Double.parseDouble(value);
			} catch (Exception e) {
				throw new FieldResultIsNotANumberException();				
			}
	}
	
	private static String formatNumber( FldSimpleModel model, String wordNumberPattern, double dub) 
		throws FieldFormattingException {


		// OK, now we have a number, let's apply the formatting string 
		
		/* Per the spec, if no numeric-formatting-switch is present, a numeric result is formatted 
		 * without leading spaces or trailing fractional zeros. 
		 * If the result is negative, a leading minus sign is present. 
		 * If the result is a whole number, no radix point is present. */
		
		
//		boolean encounteredPlus = false;
//		boolean encounteredMinus = false;
		boolean encounteredDecimalPoint = false;
		boolean encounteredX = false;
		
		// in Java's NumberFormatter, you can't mix # and 0,
		// but you can swap to the other after the decimal point
		char fillerBeforeDecimalPoint= '\0';
		char fillerAfterDecimalPoint= '\0';

		
		// in Java's NumberFormatter, you have # or 0, then a literal or %,$ etc then # or 0 again
		boolean encounteredNonFiller =false; 
		
		int round = 0;
		int fillerBeforeDecimalPointCount = 0; // to check for an anomolous result
		
		StringBuilder buffer = new StringBuilder(32);
		int valueStart = -1;
		int idx = 0;
		int idx2 = 0;
		char ch = '\0';
		char lastCh = '\0';
		boolean inLiteral = false;
		
		if ((wordNumberPattern != null) && (wordNumberPattern.length() > 0)) {
			
			while (idx < wordNumberPattern.length()) {
				ch = wordNumberPattern.charAt(idx);
				if (ch == '\'') {
					if (inLiteral) {
						// End literal
						buffer.append(wordNumberPattern.substring(valueStart, idx)); //ignore closing '
						idx2 = idx + 1; //skip '
						/*
						 * Word treats the whitespace outside the right single quote as significant, 
						 * and inserts zero, one or many spaces as if the whitespace was inside the 
						 * literal.
						 * 
						 * WARNING: downstream XML processing needs to treat
						 * this whitespace as significant. 
						 */
						while ((idx2 < wordNumberPattern.length()) && 
							   (wordNumberPattern.charAt(idx2) == ' ')) {
							buffer.append(' ');
							idx2++;
						}
						buffer.append('\'');
						idx = idx2 - 1;
						inLiteral = false;
						valueStart = -1;
					}
					else {
						// Start literal
						if (valueStart > -1) {
							appendNumberItem(buffer, wordNumberPattern.substring(valueStart, idx));
						}
						inLiteral = true;
						valueStart = idx;
						
						if (fillerBeforeDecimalPoint != '\0' || fillerAfterDecimalPoint != '\0') 
							encounteredNonFiller = true;
					}
				} else if (!inLiteral) {
					
//					if (lastCh != ch) {
//						if (valueStart > -1) {
//							appendNumberItem(buffer, wordNumberPattern.substring(valueStart, idx));
//						}
//						valueStart = idx;
//						lastCh = ch;
//					}
					
					if (ch=='x') {
						//wordNumberPattern.replaceFirst("x", "#");
						if (encounteredDecimalPoint) {
							encounteredX = true;
							round++; // we honour the last
							
							if (fillerAfterDecimalPoint == '\0') {
								buffer.append('#'); // replacing x
								fillerAfterDecimalPoint ='#'; // and now we're committed to that
							} else {
								buffer.append(fillerAfterDecimalPoint);
							}
							
						} else {
							throw new FieldFormattingException("TODO implement 'x' digit dropper before decimal point ");
						}
					} else {
						if ((ch=='0')||(ch=='#')) {
													
							if (encounteredNonFiller) {
								throw new FieldFormattingException("Can't format arbitrary character between [0|#]* ");
							}
							
							if (encounteredDecimalPoint) {
								round++;

								// Use uniform filler char
								if (fillerAfterDecimalPoint == '\0') {
									fillerAfterDecimalPoint=ch;
								}
								buffer.append(fillerAfterDecimalPoint);
							} else {
								
								if (fillerBeforeDecimalPoint == '\0') {
									fillerBeforeDecimalPoint=ch;
								}
								buffer.append(fillerBeforeDecimalPoint);
								fillerBeforeDecimalPointCount++;
							}
							
						} else if ((ch=='%') // Java special characters
									||(ch=='\u2030') //  ‰
									||(ch=='E')  
									||(ch=='\u00A4') // ¤
									) {
							// escape them by quoting
							buffer.append('\'');
							buffer.append(ch);
							buffer.append('\'');
							
							if (fillerBeforeDecimalPoint != '\0' || fillerAfterDecimalPoint != '\0') 
								encounteredNonFiller = true;
							
						} else  
							if (ch=='+') {
								// Drop it
//								encounteredPlus = true;
							}
							else if (ch=='-') {
								// Drop it
//								encounteredMinus = true;
						} else {
							buffer.append(ch); 
							if (ch=='.') {
								encounteredDecimalPoint = true;
							} else if (ch==',' || ch==' ') {
								// ok
							} else {
								if (fillerBeforeDecimalPoint != '\0' || fillerAfterDecimalPoint != '\0') 
									encounteredNonFiller = true;								
							}
						}
					}
				}
				idx++;
			}
		}
		if (valueStart > -1) {
			appendDateItem(buffer, wordNumberPattern.substring(valueStart));
		}
		
		if (fillerBeforeDecimalPointCount<1) {
			if ( (dub<0) // Word loses the negative sign!
					|| (dub>=1) ) // Word returns the fractional part only!
				throw new FieldFormattingException("Refusing to replicate Word anomolous result. ");		
			
		}
		
		String javaFormatter = buffer.toString();
		
		DecimalFormat formatter = null;
		try {
			formatter = new DecimalFormat(javaFormatter);
		} catch (java.lang.IllegalArgumentException iae) {
			// Malformed pattern
			throw new FieldFormattingException(iae.getMessage() + " from " + wordNumberPattern);
		}
		if (encounteredX) {
			formatter.setMaximumFractionDigits(round);
		}
		
		return formatter.format(dub);
		
	}
	
	private static void appendNumberItem(StringBuilder buffer, String dateItem) {
		// identity for now		
		buffer.append(dateItem);
	}
	

	private static String formatGeneral( FldSimpleModel model, String format, String value) {
		// Note that the general-formatting-switch arguments CHARFORMAT and MERGEFORMAT
		// are not handled here. It is the responsibility of the calling code
		// to handle these.
		
		// Drop surrounding quotes.  TODO improve this
		if (value.startsWith("\"")) value = value.substring(1);
		if (value.endsWith("\"")) value = value.substring(0, value.length()-1);
		
		// TODO: handle the SMALLCAPS exception!
		
		log.debug("Applying general format " + format + " to " + value);

		if (format.toUpperCase().contains("CAPS") ) {
			String[] bits = value.split(" ");
			StringBuffer sb = new StringBuffer();
			for (int i= 0; i<bits.length; i++) {
//				System.out.println("'" + bits[i] + "'");
				if (i>0) sb.append(" ");
				sb.append(firstCap(bits[i]));
				
				// TODO: test what Word does with whitespace
			}
			return sb.toString();
		}
		
		if (format.toUpperCase().contains("FIRSTCAP") ) {
			return firstCap(value);
		}
		
		if (format.toUpperCase().contains("UPPER") ) {
			return value.toUpperCase();
		}

		if (format.toUpperCase().contains("LOWER") ) {
			return value.toLowerCase();
		}
		
		log.debug("Ignoring format: " + format);
		// This method does not currently handle:
		// alphabetic, arabic, cardtext, dollartext, hex, ordtext, ordinal, or roman
		// so throw unimplemented, else 
		// mimic Word by including value "Error! Unknown switch argument" 
		
		return value;
	}
	
	private static String firstCap(String value) {
		
		if (value == null || value.length()==0) {
			return "";
		} else if (value.length()==1) {
			return value.substring(0, 1).toUpperCase();
		} else { // (value.length()>1) 
			return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
		}
		
	}
	

	public static String findFormat(String formatSwitch, List<String> fldParameters) {
		
		String ret = null;
		String param = null;
		if ((fldParameters != null) && (!fldParameters.isEmpty())) {
			for (int i=0; i<fldParameters.size(); i++) {
				if (fldParameters.get(i).startsWith(formatSwitch)) {
					param = fldParameters.get(i);
					break;
				}
			}
		}
		if ((param != null) &&  (param.length() > formatSwitch.length())) {
			ret = param.substring(formatSwitch.length());
			if ((ret.length() > 0) && 
				(ret.charAt(0) == '"') && (ret.charAt(ret.length() - 1) == '"')) {
				ret = ret.substring(1, ret.length() - 1);
			}
		}
		return ret;
	}
	
	
//	public static String getFldSimpleName(String instr) {
//		
//		String ret = null;
//		int startValue = 0;
//		int endValue = -1;	
//		if ((instr != null) && (instr.length() > 0)) {
//			while ((startValue < instr.length()) && 
//				   (instr.charAt(startValue) == ' ')) startValue++;
//			endValue = startValue;
//			while ((endValue < instr.length()) && 
//					   (instr.charAt(endValue) != ' ')) endValue++;
//			if (startValue < instr.length()) {
//				ret = instr.substring(startValue, endValue).toUpperCase();
//			}
//		}
//		return ret;
//	}
	
	public static String convertDatePattern(String wordDatePattern) {
		StringBuilder buffer = new StringBuilder(32);
		int valueStart = -1;
		int idx = 0;
		int idx2 = 0;
		char ch = '\0';
		char lastCh = '\0';
		boolean inLiteral = false;
		if ((wordDatePattern != null) && (wordDatePattern.length() > 0)) {
			while (idx < wordDatePattern.length()) {
				ch = wordDatePattern.charAt(idx);
				if (ch == '\'') {
					if (inLiteral) {
						buffer.append(wordDatePattern.substring(valueStart, idx)); //ignore closing '
						idx2 = idx + 1; //skip '
						/*
						 * Word treats the whitespace outside the right single quote as significant, 
						 * and inserts zero, one or many spaces as if the whitespace was inside the 
						 * literal.
						 * 
						 * WARNING: downstream XML processing needs to treat
						 * this whitespace as significant. 
						 */
						while ((idx2 < wordDatePattern.length()) && 
							   (wordDatePattern.charAt(idx2) == ' ')) {
							buffer.append(' ');
							idx2++;
						}
						buffer.append('\'');
						idx = idx2 - 1;
						inLiteral = false;
						valueStart = -1;
					}
					else {
						if (valueStart > -1) {
							appendDateItem(buffer, wordDatePattern.substring(valueStart, idx));
						}
						inLiteral = true;
						valueStart = idx;
					}
				}
				else if (!inLiteral) {
					if (lastCh != ch) {
						if (valueStart > -1) {
							appendDateItem(buffer, wordDatePattern.substring(valueStart, idx));
						}
						valueStart = idx;
						lastCh = ch;
					}
					if (((ch=='a') || (ch=='A')) && 
						 (isAMPM(wordDatePattern, idx))) {
						buffer.append('a');
						idx += 4;
						lastCh='\0';
						valueStart = -1;
					}
				}
				idx++;
			}
		}
		if (valueStart > -1) {
			appendDateItem(buffer, wordDatePattern.substring(valueStart));
		}
		return buffer.toString();
	}

	private static boolean isAMPM(String wordDatePattern, int idx) {
	int i=idx;
		//The a(A) of am/pm (AM/PM) doesn't need to be checked,
		//it was the reason why this got called.
		return (
				(idx + 4 < wordDatePattern.length()) &&
				((wordDatePattern.charAt(++i) == 'm') || (wordDatePattern.charAt(i) == 'M')) &&
				( wordDatePattern.charAt(++i) == '/') &&
				((wordDatePattern.charAt(++i) == 'p') || (wordDatePattern.charAt(i) == 'P')) &&
				((wordDatePattern.charAt(++i) == 'm') || (wordDatePattern.charAt(i) == 'M'))
				);
	}

	private static void appendDateItem(StringBuilder buffer, String dateItem) {
		String javaVal = DATE_FORMAT_ITEMS_TO_JAVA.get(dateItem);
		buffer.append(javaVal != null ? javaVal : dateItem);
	}
	
	public static String formatDate(FldSimpleModel model) {
		return formatDate(model, new Date()); 
	}

	
	public static String formatDate(FldSimpleModel model, Date date) {
		
		String format = findFormat("\\@", model.getFldParameters());
		return formatDate(model, format, date );
	}

	public static String formatDate(FldSimpleModel model, String format, Date date) {
		
		DateFormat dateFormat = null;
		if ((format != null) && (format.length() > 0)) {
			SimpleDateFormat simpleDateFormat = getSimpleDateFormat();
			simpleDateFormat.applyPattern(convertDatePattern(format));
			dateFormat = simpleDateFormat;
		}
		else {
			dateFormat = (model.getFldType().indexOf("DATE") > -1 ? 
						  SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT) :
					      SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)); 
		}
		return dateFormat.format(date);
	}
	
	
	private static SimpleDateFormat getSimpleDateFormat() {
		
		SimpleDateFormat ret = DATE_FORMATS.get();
		if (ret == null) {
			ret = (SimpleDateFormat)SimpleDateFormat.getDateTimeInstance();
			DATE_FORMATS.set(ret);
		}
		return ret;
	}

	
	
	// TODO: move the below methods to some other class
	
	/** Conversion of the word page number format to the fo page number format.
	 *  
	 * @param wordName word page number format
	 * @return null if the wordName is null, the correspondig fo value if present or a default.
	 */
	public static String getFoPageNumberFormat(String wordName) {
	String ret = null;
		if ((wordName != null) && (wordName.length() > 0)) {
			ret = FORMAT_PAGE_TO_FO.get(wordName);
			if (ret == null) {
				ret = DEFAULT_FORMAT_PAGE_TO_FO;
			}
			else if (ret == NONE_STRING) {
				ret = null;
			}
		}
		return ret;
	}
	
	/** Check if the page number format has a decoration (eg. dash).
	 *  
	 * @param wordName word page number format
	 * @return decoration type (one of the DECORATION_xxx constants).
	 */
	public static int getFoPageNumberDecoration(String wordName) {
	int ret = DECORATION_NONE;
		if ((wordName != null) && (wordName.length() > 0)) {
			if ("ArabicDash".equals(wordName) || 
			   NumberFormat.NUMBER_IN_DASH.value().equals(wordName)) {
				ret = DECORATION_DASH;
			}
		}
		return ret;
	}

	
}

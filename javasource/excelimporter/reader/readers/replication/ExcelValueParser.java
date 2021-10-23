package excelimporter.reader.readers.replication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;

import org.apache.poi.ss.usermodel.DateUtil;
import replication.ReplicationSettings;
import replication.ValueParser;
import replication.interfaces.IValueParser;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive.PrimitiveType;

import excelimporter.reader.readers.ExcelRowProcessor.ExcelCellData;
import excelimporter.proxies.constants.Constants;

public class ExcelValueParser extends ValueParser {

	private static HashMap<String, String> displayMaskMap = new HashMap<String, String>();
	static {
		displayMaskMap.put("m/d/yy", "MM/dd/yy");
		displayMaskMap.put("m/d/yy\\ h:mm;@", "MM/dd/yy HH:mm");
		displayMaskMap.put("m/d/yyyy", "MM/dd/yyyy");
		displayMaskMap.put("m/d/yyyy\\ h:mm;@", "MM/dd/yyyy HH:mm");

		displayMaskMap.put("dd\\-mmm\\-yy;@\\", "dd-MMMM-yy");
		displayMaskMap.put("[$-409]dd\\-mmm\\-yy;@\\", "dd-MMMM-yy");
		displayMaskMap.put("dd\\-mmm\\-yyyy;@\\", "dd-MMMM-yyyy");
		displayMaskMap.put("[$-409]dd\\-mmm\\-yyyy;@\\", "dd-MMMM-yyyy");
		displayMaskMap.put("h:mm:ss\\ AM/PM", "hh:mm:ss aa");
		displayMaskMap.put("[$-409]h:mm:ss\\ AM/PM", "hh:mm:ss aa");
		displayMaskMap.put("dddd\\,\\ mmmm\\ dd\\,\\ yyyy", "EEEE, MMMM dd, yyyy");
		displayMaskMap.put("[$-409]dddd\\,\\ mmmm\\ dd\\,\\ yyyy", "EEEE, MMMM dd, yyyy");

		displayMaskMap.put("\"$\"#,##0_);\\(\"$\"#,##0\\)", "#,##0");
		displayMaskMap.put("\"$\"#,##0_);[Red]\\(\"$\"#,##0\\)", "#,##0");

		displayMaskMap.put("\"$\"#,##0.00_);\\(\"$\"#,##0.00\\)", "#,##0.00");
		displayMaskMap.put("\"$\"#,##0.00_);[Red]\\(\"$\"#,##0.00\\)", "#,##0.00");

		displayMaskMap.put("0.0%", "#0.0%");
		displayMaskMap.put("0.00%", "#0.0%");
		displayMaskMap.put("0.000%", "#0.0%");
		displayMaskMap.put("0.0000%", "#0.0%");
	}

	public ExcelValueParser( Map<String, IValueParser> customValueParsers, ReplicationSettings settings ) {
		super(settings, customValueParsers);
	}

	@Override
	public Object getValueFromDataSet( String column, PrimitiveType type, Object dataSet ) throws ParseException {
		ExcelCellData[] objects = (ExcelCellData[]) dataSet;
		if ( objects.length > Integer.valueOf(column) )
			return getValue(type, column, objects[Integer.valueOf(column)]);
		else {
			Core.getLogger("ValueParser").warn("There is no column nr: " + column + " found on the current row");
			return null;
		}
	}

	@Override
	public String getKeyValueFromAlias(Object recordDataSet, String keyAlias) throws ParseException {
		return getKeyValueByPrimitiveType(this.settings.getMemberType(keyAlias), keyAlias,
			getValueFromDataSet(keyAlias, this.settings.getMemberType(keyAlias), recordDataSet));
	}

	@SuppressWarnings("static-access")
	private Object getValue(PrimitiveType type, String column, ExcelCellData cellData) throws ParseException {
		if (cellData == null) {
			if (Constants.getParseEmptyCells() && this.settings.hasValueParser(column))
				return getValue(type, column, (String) null);
			else
				return null;
		}
		else if (type == PrimitiveType.DateTime) {
			return parseToDateTime(column, cellData);
		}
		else if (type == PrimitiveType.Decimal || type == PrimitiveType.Integer || type == PrimitiveType.Long) {
			BigDecimal parsed = parseToNumber(cellData);
			if (parsed == null)
				throw new ParseException("Could not parse value '" + cellData.getFormattedData() + "' to " + type.name() + " in column #" + (cellData.getColNr() + 1));
			try {
				if (type == PrimitiveType.Long) {
					return parsed.setScale(0, RoundingMode.FLOOR).longValueExact();
				} else if (type == PrimitiveType.Integer) {
					return parsed.setScale(0, RoundingMode.FLOOR).intValueExact();
				} else
					return parsed;
			} catch (ArithmeticException ae) {
				throw new ValueParser.ParseException("Error casting " + parsed + " to " + type + ": " + ae.getMessage() , ae);
			}
		}
		else if (cellData.getFormattedData() != null) {
			try {
				return getValue(type, column, cellData.getFormattedData());
			} catch (Exception ignore) {
				return getValue(type, column, cellData.getRawData());
			}
		}
		else
			return getValue(type, column, cellData.getRawData());
	}

	private BigDecimal parseToNumber(ExcelCellData cellData) throws ParseException {
		if (cellData.getRawData() instanceof Number) {
			boolean isPercentage = cellData.getFormattedData() != null && cellData.getFormattedData().toString().endsWith("%");
			Number rawData = (Number) cellData.getRawData();
			if (isPercentage) {
				return BigDecimal.valueOf(rawData.doubleValue()).multiply(BigDecimal.valueOf(100));
			} else
				return BigDecimal.valueOf(rawData.doubleValue()).stripTrailingZeros();
		} else {
			String number = (cellData.getFormattedData() != null) ? cellData.getFormattedData().toString() : cellData.getRawData().toString();
			ParsePosition position = new ParsePosition(0);
			DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance(Locale.US);
			numberFormat.setParseBigDecimal(true);
			BigDecimal parsed = (BigDecimal) numberFormat.parse(number, position);
			String couldNoBeParsed = number.substring(position.getIndex());
			if (couldNoBeParsed.length() <= 1) // trailing $, €, % symbols are ignored
				return parsed;
			else
				throw new ValueParser.ParseException(number + " is not a valid number!");
		}
	}

	private Object parseToDateTime(String column, ExcelCellData cellData) throws ParseException {
		Object value = cellData.getFormattedData();
		if (value == null)
			value = cellData.getRawData();
		if (cellData.getRawData() instanceof Double)
			value = cellData.getRawData();
		else if (cellData.getRawData() instanceof String && this.nrPattern.matcher((String) cellData.getRawData()).matches())
			value = Double.valueOf((String) cellData.getRawData());

		else if (value instanceof String) {
			if (this.nrPattern.matcher((String) value).matches())
				value = Double.valueOf((String) value);
			else if (cellData.getDisplayMask() != null) {
				String displayMask = cellData.getDisplayMask();
				if (displayMaskMap.containsKey(displayMask))
					this.settings.addDisplayMask(column, displayMaskMap.get(displayMask));
			} else if (this.settings.hasDefaultInputMask(column) != null) {
				this.settings.addDisplayMask(column, this.settings.getDefaultInputMask(column));
			} else if (!this.settings.hasValueParser(column))
				logNode.warn("Unable to parse the Date(" + value + ") in field: " + cellData.getColNr());
		}

		if (value instanceof Double) {
			if (DateUtil.isValidExcelDate((Double) value)) {
				TimeZone tz = this.settings.getTimeZoneForMember(column);
				value = DateUtil.getJavaDate((Double) value, tz);
			} else
				throw new ParseException("The value was not stored in excel as a valid date.");
		}

		return getValue(PrimitiveType.DateTime, column, value);
	}

	private final Pattern nrPattern = Pattern.compile("^\\d{0,6}(\\.\\d{1,})$");

	/**
	 * Given an Excel date with either 1900 or 1904 date windowing, converts it to a java.util.Date.
	 *
	 * NOTE: If the default <code>TimeZone</code> in Java uses Daylight Saving Time then the conversion back to an Excel
	 * date may not give the same value, that is the comparison
	 * <CODE>excelDate == getExcelDate(getJavaDate(excelDate,false))</CODE> is not always true. For example if default
	 * timezone is <code>Europe/Copenhagen</code>, on 2004-03-28 the minute after 01:59 CET is 03:00 CEST, if the excel
	 * date represents a time between 02:00 and 03:00 then it is converted to past 03:00 summer time
	 *
	 * @param date The Excel date.
	 * @param use1904windowing true if date uses 1904 windowing, or false if using 1900 date windowing.
	 * @return Java representation of the date, or null if date is not a valid Excel date
	 * @see java.util.TimeZone
	 */
	@SuppressWarnings("static-access")
	public static Date getJavaDate( double date, boolean use1904windowing ) {
		if ( !HSSFDateUtil.isValidExcelDate(date) ) {
			return null;
		}
		int wholeDays = (int) Math.floor(date);
		int millisecondsInDay = (int) ((date - wholeDays) * ((24 * 60 * 60) * 1000L) + 0.5);
		Calendar calendar = new GregorianCalendar(); // using default time-zone
		setCalendar(calendar, wholeDays, millisecondsInDay, use1904windowing);
		return calendar.getTime();
	}

	public static void setCalendar( Calendar calendar, int wholeDays, int millisecondsInDay, boolean use1904windowing ) {
		int startYear = 1900;
		int dayAdjust = -1; // Excel thinks 2/29/1900 is a valid date, which it isn't
		if ( use1904windowing ) {
			startYear = 1904;
			dayAdjust = 1; // 1904 date windowing uses 1/2/1904 as the first day
		}
		else if ( wholeDays < 61 ) {
			// Date is prior to 3/1/1900, so adjust because Excel thinks 2/29/1900 exists
			// If Excel date == 2/29/1900, will become 3/1/1900 in Java representation
			dayAdjust = 0;
		}
		calendar.set(startYear, 0, wholeDays + dayAdjust, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, millisecondsInDay);
	}
}

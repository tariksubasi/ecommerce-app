package excelimporter.reader.readers;

public class ExcelRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 123456L;

	public ExcelRuntimeException() {
		super();
	}

	public ExcelRuntimeException(String message) {
		super(message);
	}

	public ExcelRuntimeException(Exception e) {
		super(e);
	}

	public ExcelRuntimeException(String message, Exception e) {
		super(message, e);
	}
}

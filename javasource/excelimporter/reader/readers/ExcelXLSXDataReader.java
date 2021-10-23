package excelimporter.reader.readers;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import replication.ReplicationSettings.MendixReplicationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.Predicate;

public class ExcelXLSXDataReader {

	public static long readData(String fullPathExcelFile, int sheetNr, int startRowNr, ExcelRowProcessor excelRowProcessor, Predicate<String> isColumnUsed)
			throws IOException, OpenXML4JException, SAXException, ExcelRuntimeException {
		OPCPackage opcPackage = null;
		InputStream sheet = null;
		try {
			opcPackage = OPCPackage.open(fullPathExcelFile, PackageAccess.READ);
			XSSFReader reader = new XSSFReader(opcPackage);
			ReadOnlySharedStringsTable stringsTable = new ReadOnlySharedStringsTable(opcPackage);
			StylesTable stylesTable = reader.getStylesTable();

			XMLReader parser = XMLReaderFactory.createXMLReader();
			ExcelXLSXReader.setXMLReaderProperties(parser);
			ExcelReader.logNode.trace("Loaded SAX Parser: " + parser);
			SheetHandler handler = new SheetHandler(isColumnUsed, stringsTable, stylesTable, startRowNr + 1, excelRowProcessor,
					sheetNr);
			parser.setContentHandler(handler);

            ArrayList<PackagePart> sheets = opcPackage.getPartsByContentType(XSSFRelation.WORKSHEET.getContentType());
            sheet = sheets.get(sheetNr).getInputStream();
			InputSource sheetSource = new InputSource(sheet);
			parser.parse(sheetSource);

			return excelRowProcessor.getRowCounter();
		} finally {
			try {
				if (excelRowProcessor != null) {
					excelRowProcessor.finish();
					if (excelRowProcessor.getRowCounter() == 0)
						ExcelReader.logNode
								.warn("Excel Importer could not import any rows. Please check if the template is configured correctly. If the file was not created with Microsoft Excel for desktop, try opening the file with Excel and saving it with the same name before importing.");
					else
						ExcelReader.logNode.info(
								"Excel Importer successfully imported " + excelRowProcessor.getRowCounter() + " rows");
				}
			} catch (MendixReplicationException e) {
				throw new ExcelRuntimeException(e);
			} finally {
				try {
					if (sheet != null)
						sheet.close();
				} catch (IOException ignore) {
				}
				if (opcPackage != null)
					opcPackage.revert();
			}
		}
	}

	private static class SheetHandler extends ExcelXLSXReader.ExcelSheetHandler {

		private boolean[] columnsUsed;
		private boolean handleCol = false;

		private ExcelRowProcessor excelRowProcessor;
		private Predicate<String> isColumnUsed;

		private SheetHandler(Predicate<String> isColumnUsed, ReadOnlySharedStringsTable stringsTable, StylesTable stylesTable,
                             int startRowNr, ExcelRowProcessor excelRowProcessor, int sheetNr) {
			super(stringsTable, stylesTable, sheetNr, startRowNr);
			this.excelRowProcessor = excelRowProcessor;

			this.isColumnUsed = isColumnUsed;
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (evaluateTextTag(localName, this.handleCol)) {
			} else if (evaluateColumn(localName, attributes)) {
				if (this.getCurrentColumnNr() < this.columnsUsed.length)
					this.handleCol = this.columnsUsed[this.getCurrentColumnNr()];
				else
					this.handleCol = false;

				if (this.handleCol) {
					evaluateCellStyle(attributes);
				}
			} else if (evaluateFormula(name)) {
			} else if (evaluateRow(localName, attributes)) {
			} else if (localName.equals("dimension")) { // only encountered once
				int colTo = evaluateDimension(attributes);

				this.columnsUsed = new boolean[colTo + 1];
				for (int i = 0; i < this.columnsUsed.length; i++) {
					this.columnsUsed[i] = this.isColumnUsed.test(String.valueOf(i));
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			// If there is something wrong with the Excel XML then we don't try to fix it
			// here.
			if (this.shouldHandleRow()) {
				closeTextProcessing(localName, this.handleCol);

				if (localName.equals("row")) {
					boolean processRow = false;
					// Check that at least one value is present, we want to skip blank lines
					for (Object value : this.getValues()) {
						if (null != value && value.toString().trim().length() != 0) {
							processRow = true;
							break;
						}
					}

					if (processRow) {
						try {
							this.excelRowProcessor.processValues(this.getValues(), this.getCurrentRow() - 1,
									this.getCurrentSheet());
						} catch (MendixReplicationException e) {
							throw new SAXException("Unable to store Excel row #" + this.getCurrentRow() + " @Sheet #" + this.getCurrentSheet(), e);
						}
					}
				}
			}
		}

	}
}

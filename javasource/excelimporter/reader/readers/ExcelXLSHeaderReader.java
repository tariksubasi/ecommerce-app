package excelimporter.reader.readers;

import com.mendix.core.CoreException;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class ExcelXLSHeaderReader implements ExcelHeadable {

    private final List<ExcelColumn> excelColumns;

    public ExcelXLSHeaderReader(ContentSupplier contentSupplier, int sheetNr, int rowNr) throws CoreException, IOException {
        // Headers - 1st pass
        HashMap<Integer, String> sstmap = headersFirstPass(contentSupplier, sheetNr, rowNr);

        // Headers - 2nd pass
        try (InputStream content = contentSupplier.get();
             POIFSFileSystem poifs = new POIFSFileSystem(content);
             InputStream workbook = poifs.createDocumentInputStream("Workbook")) {

            if (sstmap == null) {
                throw new CoreException("No headers could be found on sheet: " + sheetNr + " on row nr: " + rowNr);
            }
            // second pass
            ExcelHeadable header = new ExcelXLSReaderHeaderSecondPassListener(sheetNr, rowNr, sstmap);
            HSSFRequest req = new HSSFRequest();
            req.addListenerForAllRecords((HSSFListener) header);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.processEvents(req, workbook);
            this.excelColumns = header.getColumns();
        }
    }

    private HashMap<Integer, String> headersFirstPass(ContentSupplier fileStreamSupplier, int sheetNr, int rowNr) throws IOException, CoreException {
        try (InputStream content = fileStreamSupplier.get();
             POIFSFileSystem poifs = new POIFSFileSystem(content);
             InputStream workbook = poifs.createDocumentInputStream("Workbook")) {
            ExcelXLSReaderHeaderFirstPassListener firstPass = new ExcelXLSReaderHeaderFirstPassListener(sheetNr, rowNr);

            HSSFRequest req = new HSSFRequest();
            req.addListenerForAllRecords(firstPass);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.processEvents(req, workbook);

            return firstPass.getSSTMap();
        }
    }

    @Override
    public List<ExcelColumn> getColumns() {
        return this.excelColumns;
    }

}

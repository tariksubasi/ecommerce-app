package excelimporter.reader.readers;

import com.mendix.core.CoreException;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

public class ExcelXLSDataReader {

    public static long readData(ContentSupplier fileStreamSupplier, int sheetNr, int rowNr, ExcelRowProcessor excelRowProcessor, Predicate<String> isColumnUsed) throws CoreException, IOException {
        // Data - 1st pass ... make a sstmap describing which strings we want to load from the excel SST
        ExcelXLSReaderDataFirstPassListener firstPass = new ExcelXLSReaderDataFirstPassListener(sheetNr, rowNr, isColumnUsed);
        try (InputStream content = fileStreamSupplier.get();
             POIFSFileSystem poifs = new POIFSFileSystem(content);
             InputStream workbook = getInputStreamFromPOIFS(poifs)) {
            HSSFRequest req = new HSSFRequest();
            req.addListenerForAllRecords(firstPass);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.processEvents(req, workbook);
        }
        HashMap<Integer, String> sstmap = firstPass.getSSTMap();

        // Data - 2nd pass
        ExcelXLSReaderDataSecondPassListener secondPass = new ExcelXLSReaderDataSecondPassListener(sheetNr, rowNr, sstmap, excelRowProcessor, isColumnUsed, firstPass.getNrOfColumns());
        {
            try (InputStream content = fileStreamSupplier.get();
                 POIFSFileSystem poifs = new POIFSFileSystem(content);
                 InputStream workbook = getInputStreamFromPOIFS(poifs)) {
                HSSFRequest req = new HSSFRequest();
                req.addListenerForAllRecords(secondPass);
                HSSFEventFactory factory = new HSSFEventFactory();
                factory.processEvents(req, workbook);
            }
        }
        return secondPass.getRowCounter();
    }

    private static InputStream getInputStreamFromPOIFS(POIFSFileSystem poifs) throws CoreException, IOException {
        Set<String> entryNames = poifs.getRoot().getEntryNames();
        InputStream inStream = null;

        if (entryNames.contains("Workbook"))
            inStream = poifs.createDocumentInputStream("Workbook");
        else if (entryNames.contains("EncryptedPackage")) {
            try { // We try the default password as it might just be a write-protected file, as documented in https://poi.apache.org/encryption.html
                Biff8EncryptionKey.setCurrentUserPassword("VelvetSweatshop");
                inStream = poifs.createDocumentInputStream("EncryptedPackage");
                Biff8EncryptionKey.setCurrentUserPassword(null);
            } catch (IOException e) {
                throw new CoreException("Unable to open encrypted Excel files.", e);
            }
        }

        return inStream;
    }

}

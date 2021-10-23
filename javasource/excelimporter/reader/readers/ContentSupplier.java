package excelimporter.reader.readers;

import com.mendix.core.Core;
import com.mendix.core.CoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public interface ContentSupplier {
    InputStream get() throws CoreException;
    static ContentSupplier of(File f) {
        return () -> {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new CoreException("File not found: " + f.toString());
            }
        };
    }
    static ContentSupplier of(system.proxies.FileDocument f) {
        return () -> {
            InputStream content = Core.getFileDocumentContent(f.getContext(), f.getMendixObject());
            if (content == null)
                throw new CoreException("No content found in templatedocument");
            return content;
        };
    }
}

package excelimporter.reader.readers;

import replication.ReplicationSettings;

import java.util.Objects;

public interface ExcelRowProcessor {
    void processValues(ExcelRowProcessor.ExcelCellData[] values, int rowNow, int sheetNow ) throws ReplicationSettings.MendixReplicationException;
    void finish() throws ReplicationSettings.MendixReplicationException;
    long getRowCounter();

    class ExcelCellData {
        private final int colNr;
        private final Object rawData;
        private final String displayMask;
        private final Object formattedData;

        public ExcelCellData( int colNr, Object rawData, Object formattedData ) {
            this(colNr, rawData, formattedData, null);
        }

        public ExcelCellData( int colNr, Object rawData, Object formattedData, String displayMask ) {
            this.colNr = colNr;
            this.rawData = rawData;
            this.formattedData = formattedData;
            this.displayMask = displayMask;
        }

        public String getDisplayMask() {
            return this.displayMask;
        }

        public int getColNr() {
            return this.colNr;
        }

        public Object getFormattedData() {
            return this.formattedData;
        }

        public Object getRawData() {
            return this.rawData;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExcelCellData that = (ExcelCellData) o;
            return colNr == that.colNr &&
                    Objects.equals(rawData, that.rawData) &&
                    Objects.equals(displayMask, that.displayMask) &&
                    Objects.equals(formattedData, that.formattedData);
        }

        @Override
        public String toString() {
            return "ExcelCellData{" +
                    "colNr=" + colNr +
                    ", rawData=" + rawData +
                    ", displayMask='" + displayMask + '\'' +
                    ", formattedData=" + formattedData +
                    '}';
        }
    }
}

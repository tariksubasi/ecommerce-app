package excelimporter.reader.readers;

import java.util.Objects;

public class ExcelColumn {
	private String caption;
	private int colNr;
	
	public ExcelColumn( int colNr, String caption) {
		this.caption = caption;
		this.colNr = colNr;
	}
	
	public String getCaption() {
		return this.caption;
	}
	
	public int getColNr() {
		return this.colNr;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExcelColumn that = (ExcelColumn) o;
        return colNr == that.colNr &&
                Objects.equals(caption, that.caption);
    }

    @Override
    public String toString() {
        return "ExcelColumn{" +
                "caption='" + caption + '\'' +
                ", colNr=" + colNr +
                '}';
    }
}

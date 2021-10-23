package excelimporter.reader.readers;

import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive.PrimitiveType;
import excelimporter.reader.readers.replication.ExcelReplicationSettings;
import excelimporter.reader.readers.replication.ExcelValueParser;
import replication.MetaInfo;
import replication.MetaInfo.MetaInfoObject;
import replication.ReplicationSettings.MendixReplicationException;
import replication.ValueParser.ParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExcelRowProcessorImpl implements ExcelRowProcessor {

	private ExcelValueParser valueParser;
	private MetaInfo info;
	private ExcelReplicationSettings settings;
	private Map<String, Set<DocProperties>> docProps;

	private long rowCounter;
	private boolean hasDocProps;


	public ExcelRowProcessorImpl(ExcelReplicationSettings settings, Map<String, Set<DocProperties>> docProps) throws MendixReplicationException {
	    this.settings = settings;
		this.valueParser = new ExcelValueParser(settings.getValueParsers(), settings);
		this.info = new MetaInfo(settings, this.valueParser, "XLSReader");
		this.docProps = docProps;
		this.hasDocProps = this.docProps.size() > 0;

		this.rowCounter = 0;
	}

	public void processValues(ExcelRowProcessor.ExcelCellData[] values, int rowNow, int sheetNow ) throws MendixReplicationException {
		try {
			String objectKey = this.valueParser.buildObjectKey(values, settings.getMainObjectConfig());
			if ( ExcelReader.logNode.isTraceEnabled() )
				ExcelReader.logNode
						.trace("Start processing excel row: " + this.rowCounter + " found: " + values.length + " columns to process. Using ObjectKey: " + objectKey);


			Map<String, Long> prevObject = null;
			if ( this.hasDocProps )
				prevObject = new HashMap<String, Long>();
			rowNow++;
			sheetNow++;

			for( int i = 0; i < values.length; i++ ) {
				String alias = String.valueOf(i);
				if ( settings.aliasIsMapped(alias) ) {

					String id;
					PrimitiveType type = this.settings.getMemberType(alias);
					MetaInfoObject miObject;

					Object processedValue = this.valueParser.getValueFromDataSet(alias, type, values);

					if ( settings.treatFieldAsReference(alias) ) {
						miObject = this.info.setAssociationValue(objectKey, alias, processedValue);
						id = this.settings.getAssociationNameByAlias(alias);
					}
					else if ( settings.treatFieldAsReferenceSet(alias) ) {
						miObject = this.info.addAssociationValue(objectKey, alias, processedValue);
						id = this.settings.getAssociationNameByAlias(alias);
					}
					else {
						miObject = this.info.addValue(objectKey, alias, processedValue);
						id = this.settings.getMainObjectConfig().getObjectType();
					}
					Long columnObjectID = (miObject == null ? null : miObject.getId());

					if ( this.hasDocProps && this.docProps.containsKey(id) ) {
						if ( !prevObject.containsKey(id) || columnObjectID != prevObject.get(id) ) {
							prevObject.put(id, columnObjectID);

							for( DocProperties props : this.docProps.get(id) ) {
								Object value = null;
								switch (props.getDataSource()) {
								case DocumentPropertyRowNr:
									value = rowNow;
									break;
								case DocumentPropertySheetNr:
									value = sheetNow;
									break;
								case StaticValue:
									value = props.getStaticStringValue();
									break;
								case CellValue:
									break;
								}

								switch (props.getMappingType()) {
								case Attribute:
									this.info.addValue(objectKey, props.getColumnAlias(), value);
									break;
								case Reference:
									this.info.addAssociationValue(objectKey, props.getColumnAlias(), value);
									break;
								default:
									break;
								}
							}
						}
					}
				}
			}


		}
		catch( ParseException e ) {
			if ( !settings.getErrorHandler().valueException(e, e.getMessage()) )
				throw e;
		}

		this.rowCounter++;
		resetValuesArray(values);
	}

	private static void resetValuesArray( Object[] values ) {
        Arrays.fill(values, null);
	}

	public void finish() throws MendixReplicationException {
		this.info.finished();
	}
	
	public long getRowCounter() {
		return this.rowCounter;
	}
}

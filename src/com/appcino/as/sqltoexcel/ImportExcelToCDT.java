package com.appcino.as.sqltoexcel;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appcino.as.sqltoexcel.cdth.CDTHelper;
import com.appcino.as.sqltoexcel.cdth.CDTHelperUtils;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.MessageContainer;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.Datatype;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;

/**
 * This smart service plugin read excel file and returns a CDT array
 *
 * @author Anshuman Stutya
 *
 */
@PaletteInfo(paletteCategory = "Appian Smart Services", palette = "Document Generation")
@Order({ "ExcelDocument", "SheetNumber", "RowNumberToReadFrom", "NumberOfColumnsToReadInRow", "cdt" })
@Unattended
public class ImportExcelToCDT extends AppianSmartService {

	private final SmartServiceContext smartServiceCtx;
	private final ContentService contentService;
	private final TypeService typeService;
	
	private static final Logger LOG = LoggerFactory.getLogger(ImportExcelToCDT.class);

	private static final String ROW_NUMBER_TO_READ_FROM = "RowNumberToReadFrom";
	private static final String SHEET_NUMBER = "SheetNumber";
	private static final String NUMBER_OF_COLUMNS_TO_READ_IN_ROW = "NumberOfColumnsToReadInRow";
	private static final int MAX_ROWS = 100000;
	private static final int MAX_CONSECUTIVE_BLANK = 500;
	
	// inputs
	private Long excelDocument; // Input Excel Document to read.
	private Long sheetNumber; // Input Sheet Number in Excel file to Read. Default value is 0 meaning first sheet. Read single sheet at a time.
	private Long numberOfColumnsToReadInRow; // Input Column Number in a row to Read. Example : 4 means it will read only four columns of each row. Default value is 0 meaning read all columns of every row.
	private Long rowNumberToReadFrom; // Row Number from where data has to read. Value must be greater than 0. Default value is 1 meaning First Row.
	private TypedValue cdt; //Type of CDT to use for input
	
	// output
	private TypedValue returnCDT;
	private Boolean errorOccurred;
	private String errorTxt;



	public ImportExcelToCDT(SmartServiceContext ssc_, ContentService cs_, TypeService ts_) 
	{
		super();
		this.smartServiceCtx = ssc_;
		this.contentService = cs_;
		this.typeService = ts_;
	}
	
	@Override
	public void run() throws SmartServiceException 
	{
	
		String username = smartServiceCtx.getUsername();
		XSSFWorkbook wb = null;
		
		LOG.debug("Username :"+username);
		LOG.debug("Excel Document :" + excelDocument);
		LOG.debug("Sheet Number :" + sheetNumber);
		LOG.debug("Row no to read from :" + rowNumberToReadFrom);
		
		try {
			// get excel document to read
			Document xlsxCurrVersion = contentService.download(excelDocument,ContentConstants.VERSION_CURRENT, false)[0];

			String documentPath = xlsxCurrVersion.getInternalFilename();
			LOG.debug("Document Path :"+documentPath);
			FileInputStream fis = new FileInputStream(documentPath);
			wb = new XSSFWorkbook(fis);
			fis.close();

			XSSFSheet sheet = wb.getSheetAt(sheetNumber.intValue());
			
			// get field names from first row
			XSSFRow headerRow = sheet.getRow((this.rowNumberToReadFrom.intValue() - 1));
			
			//Determine the correct number of columns to read
			int numCols = headerRow.getLastCellNum();
			if (this.numberOfColumnsToReadInRow!=0 && this.numberOfColumnsToReadInRow < numCols) {
				numCols = this.numberOfColumnsToReadInRow.intValue();
			}
			
			
			LOG.debug("# columns = " + numCols);
			String[] fieldNames = new String[numCols];
			StringBuffer fieldList = new StringBuffer();
			for (int i = 0; i < numCols; i++) {
				fieldNames[i] = headerRow.getCell(i).getStringCellValue();
				if (LOG.isDebugEnabled()) {
					fieldList.append(fieldNames[i] + ", ");

				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Found Header Names: " + fieldList.toString());
			}

			// This is zero based, so should iterate all the way up
			int lastRowIndex = Math.min(sheet.getLastRowNum(), MAX_ROWS);
			
			// Use the member variable to determine where to start reading from
			// but validate its bounds
			int firstRowIndex = Math.min((int)Math.max(rowNumberToReadFrom, 1l), lastRowIndex);
			
			XSSFRow row;

			Datatype multiType = typeService.getTypeSafe(cdt.getInstanceType());
			Long singleTypeID = multiType.getTypeof();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Last row number :" + lastRowIndex);
				LOG.debug("CDT Instance Type MULTI= " + cdt.getInstanceType());
				LOG.debug("CDT Instance Type SINGLE= " + singleTypeID);
			}
			
			List<CDTHelper> helperList = new ArrayList<CDTHelper>();
			XSSFCell cell = null;
			int consecutiveBlankRows = 0;
			Datatype singleType = typeService.getType(singleTypeID);

			// Changed to start iterating from the rowNumberToReadFrom member variable
			// rather than '1'
			for (int r = firstRowIndex; r <= lastRowIndex; r++) {
				// create CDT Helper for each row
				CDTHelper helper = CDTHelperUtils.fromDatatype(singleType);
				row = sheet.getRow(r);

				int numBlank = 0;

				for (int c = 0; c < numCols; c++) {
					cell = row.getCell(c);
					CellType cellType = CellType.BLANK; 
					
					if (cell != null) {
						cellType = cell.getCellType();
						// If Formula, use the result of the formula to determine read type 
						if (cellType.equals(CellType.FORMULA)) {
							cellType = cell.getCachedFormulaResultType();
						}
					}
					
					String fieldName = fieldNames[c];
					Object o = null;
					switch (cellType) {
					case BLANK:
						// leave null cell rather than empty string
						++numBlank;
						break;
					case BOOLEAN:
						boolean b = cell.getBooleanCellValue();
						// TODO: test this or remove!
						o = new Boolean(b);
						break;
					case ERROR:
						o = new String("ERROR");
						break;
					// dates, numbers
					case NUMERIC:
						if (DateUtil.isCellDateFormatted(cell)) {
							Date d = cell.getDateCellValue();
							o = new Date(d.getTime());
						} else {
							// This will handle integers fine on the TypedValue
							// conversion
							double dec = cell.getNumericCellValue();
							o = new Double(dec);
						}
						break;
					case STRING:
						String s = cell.getStringCellValue();
						o = new String(s);
						break;
					default:
						o = new String("");
					}

					helper.setValue(fieldName, o);
				}

				if (numBlank >= numCols) {
					++consecutiveBlankRows;
				} else {
					consecutiveBlankRows = 0;
					helperList.add(helper);

					if (LOG.isDebugEnabled()) {
						LOG.debug("Row: " + (r - 1) + " = " + helper.toString());
					}
				}

				if (consecutiveBlankRows >= MAX_CONSECUTIVE_BLANK) {
					LOG.debug("Found too many consecutive blank rows, stopping file processing");
					break;
				}
			}

			LOG.debug("Creating CDT to return");

			// setup CDT to return
			TypedValue returnCDTTmp = new TypedValue();
			returnCDT = new TypedValue();
			returnCDTTmp.setInstanceType(cdt.getInstanceType());

			LOG.debug("Wrapping return CDT");

			Object[][] valueDoubleArray = CDTHelperUtils.getObjectMultiCDT(helperList);
			// wrap TypedValue in another TypedValue??
			returnCDTTmp.setValue(valueDoubleArray);
			returnCDT = new TypedValue(returnCDTTmp.getInstanceType(), returnCDTTmp);
			errorOccurred = false;
		} catch (Exception e) {
			e.printStackTrace();
			throw createException(e, e.getMessage());
		}
		finally
		{
				//wb.close();
				LOG.debug("Unable to close document.");
			
		}
	}

	private SmartServiceException createException(Throwable t, String key,
			Object... args) {
		return new SmartServiceException.Builder(getClass(), t).userMessage(
				key, args).build();
	}

	@Override
	public void validate(MessageContainer messages) {
		if (rowNumberToReadFrom <= 0) {
			messages.addError(ROW_NUMBER_TO_READ_FROM,"message.invalid_row_number");
		}
		if (sheetNumber < 0) {
			messages.addError(SHEET_NUMBER, "message.invalid_sheet_number");
		}
		if(numberOfColumnsToReadInRow < 0){
			messages.addError(NUMBER_OF_COLUMNS_TO_READ_IN_ROW, "message.invalid_column_number_in_row");
		}
	}

	@Input(required = Required.ALWAYS)
	@Name("ExcelDocument")
	@DocumentDataType
	public void setExcelDocument(Long val) {
		this.excelDocument = val;
	}
	
	@Input(required = Required.OPTIONAL, defaultValue = { "0" })
	@Name("SheetNumber")
	public void setSheetNumber(Long val) {
		this.sheetNumber = val;
	}

	@Input(required = Required.OPTIONAL, defaultValue = { "0" })
	@Name("NumberOfColumnsToReadInRow")
	public void setNumberOfColumnsToReadInRow(Long val) {
		this.numberOfColumnsToReadInRow = val;
	}

	@Input(required = Required.OPTIONAL, defaultValue = { "1" })
	@Name("RowNumberToReadFrom")
	public void setRowNumberToReadFrom(Long val) {
		this.rowNumberToReadFrom = val+1;
	}
	
	@Input(required = Required.ALWAYS)
	@Name("cdt")
	public void setCdt(TypedValue val) {
		this.cdt = val;
	}
	
	@Name("returnCDT")
	public TypedValue getReturnCDT() {
		return returnCDT;
	}

	@Name("errorOccurred")
	public Boolean getErrorOccurred() {
		return errorOccurred;
	}

	@Name("errorTxt")
	public String getErrorTxt() {
		return errorTxt;
	}


}
package com.appcino.as.sqltoexcel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.naming.Context;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.Approval;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.analytics2.ProcessAnalyticsService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.MessageContainer;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
/**
 * 
 * @author Anshuman Stutya
 * Smart service to export data from an SQL query to excel
 */
@PaletteInfo(paletteCategory = "Appian Smart Services", palette = "Document Generation")
public class ExportSQLToExcelNew extends AppianSmartService {
	
	private static final Logger LOG = Logger.getLogger(ExportSQLToExcelNew.class);
	private final SmartServiceContext smartServiceCtx;
	private String jndiName;
	private String sql;
	private Long excel_base_template;
	private String starting_cell;
	private Long sheet_number=0l;
	private String document_name_to_create;
	private Long document_save_directory;
	private Boolean include_header_row = false;
	private Long document_to_overwrite;
	private Long output_document;
	private String[] cell_keys;
	private String[] cell_values;
	private String sheetName;
	private Integer cloneSheet;
	private Boolean createNew = false;

	private DataSource ds;
	private Context ctx;

	private ContentService cs;

	public SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");
	public SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	public SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

	
	public ExportSQLToExcelNew(ContentService cs, ProcessAnalyticsService pas, SmartServiceContext smartServiceCtx, Context ctx) {
		super();
		this.smartServiceCtx = smartServiceCtx;
		this.cs = cs;
		this.ctx = ctx;
	}




	@Override
	public void run() throws SmartServiceException {
		Locale currentLocale = smartServiceCtx.getUserLocale() != null ? smartServiceCtx.getUserLocale() : smartServiceCtx.getPrimaryLocale();

		try {
			Workbook wb = new XSSFWorkbook();
			wb.createSheet("Sheet1");
			if (excel_base_template != null) {
				// Document xls = cs.download(excel_base_template, ContentConstants.VERSION_CURRENT, true)[0];
				Document xls = (Document) cs.getVersion(excel_base_template, ContentConstants.VERSION_CURRENT); 
				//String documentPath = xls.getInternalFilename();

				LOG.debug("Template filepath = " + cs.getInternalFilename(xls.getCurrentContentId()));

				String documentPath = cs.getInternalFilename(xls.getCurrentContentId());
				FileInputStream fis = new FileInputStream(documentPath);
				wb = WorkbookFactory.create(fis);
				fis.close();
			}
			
			if(this.createNew || this.cloneSheet !=null){
				if(this.cloneSheet != null) {
					wb.cloneSheet(this.cloneSheet);
				}
				else {
					wb.createSheet("BlankSheet");
				}
			}
			
			//Trying to set the Sheet name of current sheet
			if(this.sheetName != null)
			wb.setSheetName(this.sheet_number.intValue(), this.sheetName);
			
			Sheet s = wb.getSheetAt(this.sheet_number.intValue());


			//If any cell mapping is entered, put those values here

			if (cell_keys != null && cell_values != null
					&& cell_keys.length == cell_values.length) {
				for (int i = 0; i < cell_keys.length; i++) {
					Cell a = ExportHelper.getCell(s, cell_keys[i]);
					a.setCellValue(cell_values[i]);
				}
			}

			//Set the date format

			String dateformat = "d/m/yyyy";
			if (currentLocale.equals(Locale.US)) {
				dateformat = "m/d/yyyy";
			}
			DataFormat df = wb.createDataFormat();
			CellStyle cellStyleDateAndTime = wb.createCellStyle();
			cellStyleDateAndTime.setDataFormat(df.getFormat(dateformat + " h:mm"));
			CellStyle cellStyleDate = wb.createCellStyle();
			cellStyleDate.setDataFormat(df.getFormat(dateformat));



			//Query the SQL

			LOG.info("JNDI Connection String Name : " + this.jndiName);
			LOG.info("Context String Name : " + ctx);
			ds = (DataSource)ctx.lookup(this.jndiName);
			LOG.info (" Datasource = " + ds);


			Connection con = ds.getConnection();

			//con.setAutoCommit(false);
			LOG.info (" Connected to JNDI = " + con);

			PreparedStatement stmt = con.prepareStatement(this.sql);
			LOG.info (" Statement = " + stmt);
			stmt.setEscapeProcessing(true);
			stmt.setQueryTimeout(60);
			//--stmt.getResultSet();
			//ResultSet rs = stmt.executeQuery();
			
			boolean gotResult = stmt.execute();
			ResultSet rs = null;
			if(!gotResult){
				LOG.info("No results returned");
				} else {
				   rs = stmt.getResultSet(); 
			}
			LOG.info (" Resultset = " + rs);
			ResultSetMetaData rsmd = rs.getMetaData();
			LOG.info (" Resultset Meta data = " + rsmd);

			//If starting cell not specified, use A1
			starting_cell = StringUtils.isEmpty(starting_cell) ? "A1" : starting_cell;
			Cell reportStart = ExportHelper.getCell(s, starting_cell);
			Cell current = reportStart;


			LOG.info (" Written to Cell");
			LOG.info ("Column Count = " + rsmd.getColumnCount());
			int colCount = rsmd.getColumnCount();
			/*
			 * If header row is to be included, then run the following
			 */
			if (include_header_row !=null && include_header_row){
				for (int i=0; i< colCount; i++){
					current.setCellValue(rsmd.getColumnName(i+1));
					LOG.info ("Column Header " +i);
					Cell next = current.getRow().getCell(current.getColumnIndex() + 1);
					if (next ==null) {
						next = current.getRow().createCell(current.getColumnIndex() +1 );
					}
					current = next;
				}

				//Move to the next row to write detail values
				current = ExportHelper.getCell(s, reportStart.getRowIndex() +1, reportStart.getColumnIndex());
				LOG.info (" Header row written"); 
			}


			/* Now write the rows into DB */

			//Keep track of the row
			int rowIndex =reportStart.getRowIndex() +1;
			if (include_header_row !=null && include_header_row)
	        	  rowIndex++;

			while (rs.next()) {

				for (int i = 0; i < colCount; i++) {
					//Loop through each column and get value and write to sheet

					//Get the cell value
					//Check the value type
					//Set the correct value
					int column = i+1;

					populateCellValue(rs, current, column, cellStyleDate );

					Cell next = current.getRow().getCell(current.getColumnIndex() + 1);
					if (next ==null) {
						next = current.getRow().createCell(current.getColumnIndex() +1 );
					}
					current = next;                                  
				}

				//Go to next Row
				current = ExportHelper.getCell(s, rowIndex++, reportStart.getColumnIndex());


			}


			//clean up connection
			rs.close();
			LOG.info (" Result set closed ");
			stmt.close();
			LOG.info (" Statement closed ");
			con.close();
			// Complete work - package and return
			LOG.info (" Connection closed ");

			Long docId = registerDocument();

			File file = new File(cs.getInternalFilename(docId));

			//Create physical directories
			File directory = new File(file.getParentFile().getAbsolutePath()); 
			directory.mkdirs(); 

			LOG.debug(" Document Created, documentID = " + docId);
			LOG.debug (" File created, filepath from file = " + file.getAbsolutePath());
			LOG.debug (" Document Created, filepath = " + cs.getInternalFilename(docId));


			FileOutputStream out = new FileOutputStream(file);

			try{
				wb.write(out);

			} finally {
				out.close();
			}


			LOG.debug(" File stream closed");


			cs.setSizeOfDocumentVersion(docId);

			output_document = docId;

			LOG.debug(" Document size updated");

		} catch (Exception e) {
			LOG.error(e, e);
			throw createException(e, "error.export.general", e.getMessage());
		}

	}

	//Populate Cell Values
	private void populateCellValue(ResultSet rs, Cell current, int column, CellStyle cellStyleDate ) throws Exception {

		int coltype = rs.getMetaData().getColumnType(column);
		Date d = null;

		switch (coltype) {
		case Types.BIT:         
		case (Types.BOOLEAN):
			boolean b = rs.getBoolean(column);
		if (rs.wasNull()) {
			return;
		} else {
			current.setCellType(CellType.STRING);
			current.setCellValue(b ? "Yes" : "No");
			LOG.debug("Boolean Value written : " + b);
			return;
		}
		case Types.DATE:
			d = rs.getDate(column);
			if (d == null || rs.wasNull()) {
				return;
			} else {
				current.setCellStyle(cellStyleDate);
				current.setCellValue( DATE_FORMAT.format(d));
				LOG.debug("Date Value written : " + DATE_FORMAT.format(d));
				return;
			}
		case Types.TIMESTAMP:
			d = rs.getTimestamp(column);
			if (d == null || rs.wasNull()) {
				return;
			} else {
				current.setCellStyle(cellStyleDate);
				current.setCellValue( DATE_FORMAT.format(d));
				LOG.debug("Timestamp Value written : " + DATE_FORMAT.format(d));
				return;
			}
		case Types.TIME:
			d = rs.getTime(column);
			if (d == null || rs.wasNull()) {
				return;
			} else {
				current.setCellStyle(cellStyleDate);
				current.setCellValue( DATE_FORMAT.format(d));
				LOG.debug("Time Value written : " + DATE_FORMAT.format(d));
				return;
			}
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.DECIMAL:
		case Types.NUMERIC:
		case Types.REAL:
			double doub = rs.getDouble(column);
			if (rs.wasNull()) {
				return;
			} else {
				current.setCellType(CellType.NUMERIC);
//				current.setCellType(Cell.CELL_TYPE_NUMERIC);
				current.setCellValue(doub);
				LOG.debug("Float Value written : " + doub);
				return;
			}
		case Types.INTEGER:
		case Types.BIGINT:
		case Types.SMALLINT:
		case Types.TINYINT:
			long l = rs.getLong(column);
			if (rs.wasNull()) {
				return;
			} else {
				current.setCellType(CellType.NUMERIC);
//				current.setCellType(Cell.CELL_TYPE_NUMERIC);
				current.setCellValue(l);
				LOG.debug("Int Value written : " + l);
				return;
			}

		case Types.VARCHAR:
		case Types.CHAR:
		case Types.LONGVARCHAR:
			String s1 = rs.getString(column);
			if (rs.wasNull()) {
				return;
			} else if (StringUtils.isEmpty(s1)) {
				return;
			} else {
				
				if(s1.charAt(0) == '='){
					current.setCellType(CellType.FORMULA);
//					current.setCellType(Cell.CELL_TYPE_FORMULA);
					current.setCellFormula(s1.substring(1));
					LOG.debug("Formula written : " + s1 );
				} else {
					current.setCellType(CellType.STRING);
//					current.setCellType(Cell.CELL_TYPE_STRING);
					current.setCellValue(s1);
					LOG.debug("Char Value written : " + s1 );
				}
				return;
			}

		case Types.NVARCHAR:
		case Types.NCHAR:
		case Types.LONGNVARCHAR:
			String n1 = rs.getNString(column);
			if (rs.wasNull()) {
				return;
			} else if (StringUtils.isEmpty(n1)) {
				return;
			} else {
				if(n1.charAt(0) == '='){
					current.setCellType(CellType.FORMULA);
//					current.setCellType(Cell.CELL_TYPE_FORMULA);
					current.setCellFormula(n1.substring(1));
					LOG.debug("Formula written : " + n1 );
				} else {
					current.setCellType(CellType.STRING);
//					current.setCellType(Cell.CELL_TYPE_STRING);
					current.setCellValue(n1);
					LOG.debug("NChar Value written : " + n1 );
				}
				return;
			}
		default:
			LOG.warn("Error no handler for " + coltype);
			return ;
		}

	}

	private Long registerDocument() throws Exception {

		String name = document_name_to_create;
		String extension = "xlsx";

		Document d;

		ContentFilter cf = new ContentFilter(ContentConstants.TYPE_DOCUMENT);
		cf.setName(name);
		cf.setExtension(new String[] { extension });

		if (document_to_overwrite != null) {
			d = (Document) cs.getVersion(document_to_overwrite,
					ContentConstants.VERSION_CURRENT);
			d.setFileSystemId(ContentConstants.ALLOCATE_FSID);
			Approval a = cs.createVersion(d, ContentConstants.UNIQUE_FOR_PARENT);
			return a.getId()[0];
		} else {
			Content[] children = cs.getChildren(document_save_directory, cf, ContentConstants.GC_MOD_NORMAL);
			if (children == null || children.length == 0) {

				LOG.debug("Creating document from scratch");

				d = new Document(document_save_directory, name, extension);
				d.setState(ContentConstants.STATE_ACTIVE_PUBLISHED);
				d.setFileSystemId(ContentConstants.ALLOCATE_FSID);
				return cs.create(d, ContentConstants.UNIQUE_FOR_PARENT);
			} else {
				d = (Document) children[0];

				LOG.debug("Retrieved a previous document = " + d.getId());

				d.setFileSystemId(ContentConstants.ALLOCATE_FSID);
				Approval a = cs.createVersion(d,  ContentConstants.UNIQUE_FOR_PARENT);
				return a.getId()[0];
			}
		}
	}

	private SmartServiceException createException(Throwable t, String key, Object... args) {
		return new SmartServiceException.Builder(getClass(), t).userMessage(key,
				args).build();
	}





	public void onSave(MessageContainer messages) {
	}

	public void validate(MessageContainer messages) {
	}    



	@Input(required = Required.ALWAYS)
	@Name("jndiName")
	public void setJndiName(String val) {
		this.jndiName = val;
	}

	@Input(required = Required.ALWAYS)
	@Name("sheet_name")
	public void setSheetName(String val) {
		this.sheetName = val;
	}
	
	@Input(required = Required.OPTIONAL)
	@Name("sheet_number_to_clone")
	public void setCloneSheet(Integer val) {
		this.cloneSheet = val;
	}
	
	@Input(required = Required.ALWAYS)
	@Name("add_as_new_sheet")
	public void setCreateNew(Boolean val) {
		this.createNew = val;
	}
	
	@Input(required = Required.ALWAYS)
	@Name("sql")
	public void setSql(String val) {
		this.sql = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("excel_base_template")
	@DocumentDataType
	public void setExcel_base_template(Long val) {
		this.excel_base_template = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("starting_cell")
	public void setStarting_cell(String val) {
		this.starting_cell = val;
	}

	@Input(required = Required.ALWAYS)
	@Name("sheet_number")
	public void setSheet_number(Long val) {
		this.sheet_number = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("document_name_to_create")
	public void setDocument_name_to_create(String val) {
		this.document_name_to_create = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("document_save_directory")
	@FolderDataType
	public void setDocument_save_directory(Long val) {
		this.document_save_directory = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("include_header_row")
	public void setInclude_header_row(Boolean val) {
		this.include_header_row = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("document_to_overwrite")
	@DocumentDataType
	public void setDocument_to_overwrite(Long val) {
		this.document_to_overwrite = val;
	}

	@Name("output_document")
	@DocumentDataType
	public Long getOutput_document() {
		return output_document;
	}

	@Input(required = Required.OPTIONAL)
	@Name("Cell_Keys")
	public void setCell_keys(String[] val) {
		this.cell_keys = val;
	}

	@Input(required = Required.OPTIONAL)
	@Name("Cell_Values")
	public void setCell_values(String[] val) {
		this.cell_values = val;
	}

}
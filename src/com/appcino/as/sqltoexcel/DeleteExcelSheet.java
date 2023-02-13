package com.appcino.as.sqltoexcel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

import javax.naming.Context;

import org.apache.log4j.Logger;
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
public class DeleteExcelSheet extends AppianSmartService {
	
	private static final Logger LOG = Logger.getLogger(DeleteExcelSheet.class);

	private String document_name_to_create;
	private Long document_save_directory;
	private Long document_to_overwrite;
	private Long output_document;
	private Integer sheetNumber;
	private Long excel_base_template;
	

	private ContentService cs;

	public SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");
	public SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	public SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	
	

	public DeleteExcelSheet(ContentService cs, ProcessAnalyticsService pas, SmartServiceContext smartServiceCtx, Context ctx) {
		super();
		this.cs = cs;
	}




	@Override
	public void run() throws SmartServiceException {
		
		try {
			Workbook wb = new XSSFWorkbook();
			wb.createSheet("Sheet1");
			if (excel_base_template != null) {
				// Document xls = cs.download(excel_base_template, ContentConstants.VERSION_CURRENT, true)[0];
				Document xls = (Document) cs.getVersion(excel_base_template, ContentConstants.VERSION_CURRENT); 
				//String documentPath = xls.getInternalFilename();

				LOG.debug("Template filepath = " + cs.getInternalFilename(xls.getCurrentContentId()));

				String documentPath = cs.getInternalFilename(xls.getCurrentContentId());
				try(FileInputStream fis = new FileInputStream(documentPath)) {
					wb = WorkbookFactory.create(fis);
					fis.close();
				}
			}
			
			//Trying to set the Sheet name of current sheet
			if(this.sheetNumber != null)
			wb.removeSheetAt(this.sheetNumber);
			
			
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

	private SmartServiceException createException(Throwable t, String key,
			Object... args) {
		return new SmartServiceException.Builder(getClass(), t).userMessage(key,
				args).build();
	}





	public void onSave(MessageContainer messages) {
	}

	public void validate(MessageContainer messages) {
	}    



	@Input(required = Required.ALWAYS)
	@Name("sheetNumber")
	public void setJndiName(Integer val) {
		this.sheetNumber = val;
	}


	@Input(required = Required.OPTIONAL)
	@Name("excel_base_template")
	@DocumentDataType
	public void setExcel_base_template(Long val) {
		this.excel_base_template = val;
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



}
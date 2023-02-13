package com.appcino.as.sqltoexcel;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.Approval;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.ContentOutputStream;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;

public class DocUtils {

	private static final Logger LOG = Logger.getLogger(DocUtils.class);

	public static Long registerDocument(ContentService cs, Long docId, Long folder, String name, String extension, String contents) throws Exception {

		boolean isDebug = LOG.isDebugEnabled();

		ContentFilter cf = new ContentFilter(ContentConstants.TYPE_DOCUMENT);
		cf.setName(name);
		cf.setExtension(new String[] { extension });

		Long newDocID = null;
		Document d = new Document();

		try {
			if (docId == null) {

				/*
				if(isDebug){
					LOG.debug("New Document Path = " + d.getInternalFilename());
				}
				File file = new File(d.getInternalFilename());
				File directory = new File(file.getParentFile().getAbsolutePath()); 
				directory.mkdirs();*/
			}

			if (docId != null) {
				if(isDebug){
					LOG.debug("Overwriting document");
				}
				d = (Document)cs.getVersion(docId, ContentConstants.VERSION_CURRENT);

				Approval a = cs.createVersion(d, ContentConstants.UNIQUE_FOR_PARENT);
				newDocID = a.getId()[0];
				String nvFilePath = cs.getInternalFilename(newDocID);
				// write the content to filePath
				try(FileOutputStream fos = new FileOutputStream(nvFilePath)) {
					try(OutputStreamWriter writer = new OutputStreamWriter(fos)) {
						writer.write(contents);
						writer.flush();
						writer.close();
					}
					fos.close();
				}
			} else {
				if(isDebug){
					LOG.debug("Creating a new document");
				}
				Content[] children = cs.getChildren(folder, cf,ContentConstants.GC_MOD_NORMAL);
				if(children == null || children.length == 0){
					if(isDebug){
						LOG.debug("Creating a new document");
					}
					d.setParent(folder);
					d.setName(name);
					d.setExtension(extension);
					d.setFileSystemId(ContentConstants.ALLOCATE_FSID);
					try(ContentOutputStream cos = cs.upload(d,ContentConstants.UNIQUE_FOR_PARENT)){
						try(OutputStreamWriter writer = new OutputStreamWriter(cos)) {
							writer.write(contents);
							writer.flush();
							writer.close();
						}
						newDocID = cos.getContentId();
						d.setId(newDocID);
						cos.close();
					}					
				}
				else {
					if(isDebug){
						LOG.debug("Found existing document");
					}
					d = (Document)children[0];
					Approval a = cs.createVersion(d, ContentConstants.UNIQUE_FOR_PARENT);
					newDocID = a.getId()[0];
					String nvFilePath = cs.getInternalFilename(newDocID);
					// write the content to filePath
					try(FileOutputStream fos = new FileOutputStream(nvFilePath)) {
						try(OutputStreamWriter writer = new OutputStreamWriter(fos)) {
							writer.write(contents);
							writer.flush();
							writer.close();
						}
						fos.close();
					}
				}
			}
			cs.setSizeOfDocumentVersion(newDocID);
		} catch (Exception e) {
			LOG.error(e, e);
		}
		return newDocID;
	}

}

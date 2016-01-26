package edu.psu.ist.acs.micro.event.util;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StorageFileSystem;
import edu.cmu.ml.rtw.generic.data.store.StorageMongo;
import edu.cmu.ml.rtw.generic.util.Properties;
import edu.psu.ist.acs.micro.event.data.EventDataTools;

/**
 * EventProperties loads and represents a properties
 * configuration file (for specifying file paths and
 * other system dependent information)
 * 
 * @author Bill McDowell
 *
 */
public class EventProperties extends Properties {
	private String contextInputDirPath;
	private String experimentOutputDirPath;
	
	private String storageMongoMicroEventDatabaseName;
	private String storageFileSystemMicroEventDirPath;
	private boolean useMongoStorage;
	
	private String midNewsDocumentCollectionName;
	
	public EventProperties() {
		this(null);
	}
	
	public EventProperties(String path) {
		super( new String[] { (path == null) ? "event.properties" : path } );
		
		this.contextInputDirPath = loadProperty("contextInputDirPath");
		this.experimentOutputDirPath = loadProperty("experimentOutputDirPath");
		this.storageFileSystemMicroEventDirPath = loadProperty("storageFileSystemMicroEventDirPath");
		this.storageMongoMicroEventDatabaseName = loadProperty("storageMongoMicroEventDatabaseName");
		this.useMongoStorage = Boolean.valueOf(loadProperty("useMongoStorage"));
		this.midNewsDocumentCollectionName = loadProperty("midNewsDocumentCollectionName");
	}
	
	public String getContextInputDirPath() {
		return this.contextInputDirPath;
	}
	
	public String getExperimentOutputDirPath() {
		return this.experimentOutputDirPath;
	}
	
	public Storage<?,Document> getStorage(EventDataTools dataTools) {
		if (this.useMongoStorage) {
			return new StorageMongo("localhost", this.storageMongoMicroEventDatabaseName, dataTools.getDocumentSerializers(new DocumentNLPInMemory(dataTools), null));
		} else {
			return new StorageFileSystem<Document>(this.storageFileSystemMicroEventDirPath, dataTools.getDocumentSerializers(new DocumentNLPInMemory(dataTools), null));
		}
	}
	
	public String getMIDNewsDocumentCollectionName() {
		return this.midNewsDocumentCollectionName;
	}
}
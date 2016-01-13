package edu.psu.ist.acs.micro.event.util;

import edu.cmu.ml.rtw.generic.util.Properties;

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
	
	public EventProperties() {
		this(null);
	}
	
	public EventProperties(String path) {
		super( new String[] { (path == null) ? "event.properties" : path } );
		
		this.contextInputDirPath = loadProperty("contextInputDirPath");
		this.experimentOutputDirPath = loadProperty("experimentOutputDirPath");
	}
	
	public String getContextInputDirPath() {
		return this.contextInputDirPath;
	}
	
	public String getExperimentOutputDirPath() {
		return this.experimentOutputDirPath;
	}
}
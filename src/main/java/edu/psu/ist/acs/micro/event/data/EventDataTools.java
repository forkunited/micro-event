package edu.psu.ist.acs.micro.event.data;

import java.util.Map.Entry;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;
import edu.psu.ist.acs.micro.event.util.EventProperties;

/**
 * EventDataTools contains definitions of cleaning 
 * functions, gazetteers, and other tools used by
 * features in the event models.
 * 
 * @author Bill McDowell
 *
 */
public class EventDataTools extends DataTools {
	private EventProperties properties;
	
	public EventDataTools() {
		this(new EventProperties());
		
	}
	
	public EventDataTools(EventProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public EventDataTools(OutputWriter outputWriter, EventDataTools dataTools) {
		this(outputWriter, dataTools.properties);
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	public EventDataTools(OutputWriter outputWriter, EventProperties properties) {
		super(outputWriter);
		
		this.properties = properties;
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.ARTICLE_PUBLICATION_DATE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.ARTICLE_SOURCE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.ARTICLE_TITLE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.MID_SVM_RELEVANCE_SCORE);
	}
	
	public EventProperties getProperties() {
		return this.properties;
	}
}

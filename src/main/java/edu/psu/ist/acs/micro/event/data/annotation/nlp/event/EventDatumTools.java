package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;

public abstract class EventDatumTools<D extends Datum<L>, L> extends Datum.Tools<D, L> {		
	public static interface EventMentionExtractor<D extends Datum<L>, L> {
		String toString();
		EventMention[] extract(D datum);
	}
	
	private Map<String, EventMentionExtractor<D, L>> eventMentionExtractors;
		
	public EventDatumTools(DataTools dataTools) {
		super(dataTools);
		this.eventMentionExtractors = new HashMap<String, EventMentionExtractor<D, L>>();
	}
		
	public EventMentionExtractor<D, L> getEventMentionExtractor(String name) {
		return this.eventMentionExtractors.get(name);
	}
	
	public boolean addEventMentionExtractor(EventMentionExtractor<D, L> eventMentionExtractor) {
		this.eventMentionExtractors.put(eventMentionExtractor.toString(), eventMentionExtractor);
		return true;
	}
}

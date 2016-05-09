package edu.psu.ist.acs.micro.event.task.classify.tlink.det;

import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLAspect;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLClass;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLTense;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression.TimeMLDocumentFunction;

public class DetReportingDCT {
	public static TimeMLRelType determineRelation(EventMention event, TimeExpression time) {
		if (time.getTimeMLDocumentFunction() != TimeMLDocumentFunction.CREATION_TIME || 
				!event.getTokenSpan().getDocument().getName().equals(time.getTokenSpan().getDocument().getName()))
			return null;
		
		if (event.getTimeMLClass() == TimeMLClass.REPORTING 
				&& event.getTimeMLTense() == TimeMLTense.PAST
				&& event.getTimeMLAspect() == TimeMLAspect.PERFECTIVE) {
			return TimeMLRelType.IS_INCLUDED;
		} else {
			return null;
		}
	}
}

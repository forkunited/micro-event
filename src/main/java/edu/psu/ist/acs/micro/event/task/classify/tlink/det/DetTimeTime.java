package edu.psu.ist.acs.micro.event.task.classify.tlink.det;

import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;

public class DetTimeTime {
	public static TimeMLRelType determineRelation(TimeExpression t1, TimeExpression t2) {
		TimeMLRelType rel = t1.getRelationToTime(t2, false);
		if (rel != null) {
			return rel;
		} else {
			return null;
		}
	}
}

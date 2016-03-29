package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;

public class MethodClassificationTLinkTypeTimeTime extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	
	public MethodClassificationTLinkTypeTimeTime() {
		
	}
	
	public MethodClassificationTLinkTypeTimeTime(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return new String[0];
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return false;
	}

	@Override
	public boolean init(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> testData) {
		return true;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> clone() {
		return new MethodClassificationTLinkTypeTimeTime(this.context);
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeTimeTime(context);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "TimeTime";
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		
		for (TLinkDatum<TimeMLRelType> datum : data) {
			TLink tlink = datum.getTLink();
			if (tlink.getType() != TLink.Type.TIME_TIME)
				continue;
			
			TimeExpression t1 = (TimeExpression)tlink.getSource();
			TimeExpression t2 = (TimeExpression)tlink.getTarget();
			TimeMLRelType rel = t1.getRelationToTime(t2, false);
			if (rel != null) {
				map.put(datum, rel);
			}
		}
	
		return map;
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, Pair<TimeMLRelType, Double>> classifyWithScore(
			DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classifications = classify(data);
		Map<TLinkDatum<TimeMLRelType>, Pair<TimeMLRelType, Double>> scores = new HashMap<>();
		for (Entry<TLinkDatum<TimeMLRelType>, TimeMLRelType> entry : classifications.entrySet())
			scores.put(entry.getKey(), new Pair<TimeMLRelType, Double>(entry.getValue(), 1.0));
		return scores;
	}
}

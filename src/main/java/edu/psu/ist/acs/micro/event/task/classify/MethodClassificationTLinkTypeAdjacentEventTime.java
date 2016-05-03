package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.Trainable;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLAspect;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.NormalizedTimeValue;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.NormalizedTimeValue.Reference;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.Position;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkable.Type;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;

public class MethodClassificationTLinkTypeAdjacentEventTime extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	private static final int MAX_DISTANCE = 2;
	
	public MethodClassificationTLinkTypeAdjacentEventTime() {
		
	}
	
	public MethodClassificationTLinkTypeAdjacentEventTime(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
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
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> clone(String referenceName) {
		MethodClassificationTLinkTypeAdjacentEventTime clone = new MethodClassificationTLinkTypeAdjacentEventTime(this.context);
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeAdjacentEventTime(context);
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
		return "AdjacentEventTime";
	}
		
	private TimeMLRelType getTypeFromPreposition(String text) {
		text = text.toLowerCase();
		
		if (text.equals("in")) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (text.equals("on")) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (text.equals("for")) {
			return TimeMLRelType.SIMULTANEOUS;
		} else if (text.equals("at")) {
			return TimeMLRelType.SIMULTANEOUS;
		} else if (text.equals("by"))  {
			return TimeMLRelType.VAGUE;
		} else if (text.equals("over")) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (text.equals("during")) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (text.equals("throughout")) {
			return TimeMLRelType.SIMULTANEOUS;
		} else if (text.equals("within")) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (text.equals("until")) {
			return TimeMLRelType.BEFORE;
		} else if (text.equals("from")) {
			return TimeMLRelType.AFTER;
		} else if (text.equals("after")) {
			return TimeMLRelType.AFTER;
		} else {
			return TimeMLRelType.IS_INCLUDED;
		}
	}

	@Override
	public boolean hasTrainable() {
		return false;
	}

	@Override
	public Trainable<TLinkDatum<TimeMLRelType>, TimeMLRelType> getTrainable() {
		return null;
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
	
	@Override
	public Pair<TimeMLRelType, Double> classifyWithScore(
			TLinkDatum<TimeMLRelType> datum) {
		TimeMLRelType label = classify(datum);
		if (label != null)
			return new Pair<TimeMLRelType, Double>(label, 1.0);
		else
			return null;
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		
		for (TLinkDatum<TimeMLRelType> datum : data) {
			TimeMLRelType label = classify(datum);
			if (label != null)
				map.put(datum, label);
		}
	
		return map;
	}
	
	@Override
	public TimeMLRelType classify(TLinkDatum<TimeMLRelType> datum) {
		TLink tlink = datum.getTLink();
		if (tlink.getPosition() != Position.WITHIN_SENTENCE 
				|| tlink.getType() != TLink.Type.EVENT_TIME)
			return null;
		
		TimeExpression time = tlink.getFirstTime();
		EventMention event = tlink.getFirstEvent();
		
		if (time.getValue().getType() == NormalizedTimeValue.Type.PART_OF_YEAR)
			return null;
		
		TokenSpan eventSpan = event.getTokenSpan();
		if (eventSpan.getLength() > 1)
			return null;
		
		DocumentNLP document = eventSpan.getDocument();
		int sentenceIndex = event.getTokenSpan().getSentenceIndex();
		int eventIndex = eventSpan.getStartTokenIndex();
		int timeIndex = time.getTokenSpan().getStartTokenIndex();
		DependencyParse.DependencyPath path = document.getDependencyParse(sentenceIndex).getPath(eventIndex, timeIndex);

		PoSTag eventTag = document.getPoSTag(eventSpan.getSentenceIndex(), eventIndex);
		if (!PoSTagClass.classContains(PoSTagClass.VB, eventTag))
			return null;
		
		int eventToTimexDist = this.context.getDatumTools().getTokenSpanExtractor("BetweenSourceTarget").extract(datum)[0].getLength();
		boolean eventBeforeTime = timeIndex - eventIndex >= 0;
		boolean eventGovernsTime = path != null && path.getDependencyLength() == 1 && path.isAllGoverning();
		
		TimeMLRelType etRel = null;
		if (eventBeforeTime && eventToTimexDist <= MAX_DISTANCE) { // Timex after event adjacent
			if (eventToTimexDist == 0)
				etRel = TimeMLRelType.IS_INCLUDED;
			else if (timeIndex > 0 && document.getPoSTag(sentenceIndex, timeIndex - 1) == PoSTag.IN)
				etRel = getTypeFromPreposition(document.getTokenStr(sentenceIndex, timeIndex - 1));
			else 
				etRel = TimeMLRelType.IS_INCLUDED;
		/* NOTE: This bit was left out of CAEVO because it makes performance alot worse
		} else if (!eventBeforeTime && eventToTimexDist <= MAX_DISTANCE) { // Event after timex adjacent
			if (!eventGovernsTime)
				etRel = TimeMLRelType.VAGUE;
			else if (time.getValue().getReference() == Reference.PRESENT && event.getTimeMLAspect() == TimeMLAspect.PROGRESSIVE)
				etRel = TimeMLRelType.INCLUDES;
			else if (time.getValue().getReference() == Reference.PRESENT)
				etRel = TimeMLRelType.VAGUE;
			else if (eventToTimexDist == 0)
				etRel = TimeMLRelType.IS_INCLUDED;
			else if (timeIndex > 1 && document.getTokenStr(sentenceIndex, timeIndex - 1).toLowerCase().equals("said"))
				etRel = TimeMLRelType.VAGUE;
			else 
				etRel = TimeMLRelType.IS_INCLUDED;*/
		} else if (eventGovernsTime) { // Event governs time
			if (time.getValue().getReference() == Reference.PRESENT && event.getTimeMLAspect() == TimeMLAspect.PROGRESSIVE)
				etRel = TimeMLRelType.INCLUDES;
			else if (time.getValue().getReference() == Reference.PRESENT)
				etRel = TimeMLRelType.VAGUE;
			else 
				etRel = TimeMLRelType.IS_INCLUDED;
		} else if (path != null && path.getDependencyLength() == 1 && path.isAllGovernedBy()) { // Timex governs event
			etRel = TimeMLRelType.IS_INCLUDED;
		}
		
		if (etRel != null) {
			TimeMLRelType rel = etRel;
			if (tlink.getSource().getTLinkableType() != Type.EVENT)
				rel = TLink.getConverseTimeMLRelType(etRel);
			return rel;
		} else {
			return null;
		}
	}
}

package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
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
	public boolean init(DataFeatureMatrix<TLinkDatum<TimeMLRelType>, TimeMLRelType> testData) {
		return true;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> clone() {
		return new MethodClassificationTLinkTypeAdjacentEventTime(this.context);
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
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataFeatureMatrix<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		
		for (TLinkDatum<TimeMLRelType> datum : data.getData()) {
			TLink tlink = datum.getTLink();
			if (tlink.getPosition() != Position.WITHIN_SENTENCE 
					|| tlink.getType() != TLink.Type.EVENT_TIME)
				continue;
			
			TimeExpression time = tlink.getFirstTime();
			EventMention event = tlink.getFirstEvent();
			
			if (time.getValue().getType() == NormalizedTimeValue.Type.PART_OF_YEAR)
				continue;
			
			TokenSpan eventSpan = event.getTokenSpan();
			if (eventSpan.getLength() > 1)
				continue;
			
			DocumentNLP document = eventSpan.getDocument();
			int sentenceIndex = event.getTokenSpan().getSentenceIndex();
			int eventIndex = eventSpan.getStartTokenIndex();
			int timeIndex = time.getTokenSpan().getStartTokenIndex();
			System.out.println(document.getDependencyParse(sentenceIndex) + " " + eventIndex + " " + timeIndex);
			DependencyParse.DependencyPath path = document.getDependencyParse(sentenceIndex).getPath(eventIndex, timeIndex);

			PoSTag eventTag = document.getPoSTag(eventSpan.getSentenceIndex(), eventIndex);
			if (!PoSTagClass.classContains(PoSTagClass.VB, eventTag))
				continue;
			
			int eventToTimexDist = this.context.getDatumTools().getTokenSpanExtractor("BetweenSourceTarget").extract(datum)[0].getLength();
			boolean eventBeforeTime = timeIndex - eventIndex >= 0;
			boolean eventGovernsTime = path.getDependencyLength() == 1 && path.isAllGoverning();
			
			TimeMLRelType etRel = null;
			if (eventBeforeTime && eventToTimexDist <= MAX_DISTANCE) { // Timex after event adjacent
				if (eventToTimexDist == 0)
					etRel = TimeMLRelType.IS_INCLUDED;
				else if (timeIndex > 0 && document.getPoSTag(sentenceIndex, timeIndex - 1) == PoSTag.IN)
					etRel = getTypeFromPreposition(document.getTokenStr(sentenceIndex, timeIndex - 1));
				else 
					etRel = TimeMLRelType.IS_INCLUDED;
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
					etRel = TimeMLRelType.IS_INCLUDED;
			} else if (eventGovernsTime) { // Event governs time
				if (time.getValue().getReference() == Reference.PRESENT && event.getTimeMLAspect() == TimeMLAspect.PROGRESSIVE)
					etRel = TimeMLRelType.INCLUDES;
				else if (time.getValue().getReference() == Reference.PRESENT)
					etRel = TimeMLRelType.VAGUE;
				else 
					etRel = TimeMLRelType.IS_INCLUDED;
			} else if (path.getDependencyLength() == 1 && path.isAllGovernedBy()) { // Timex governs event
				etRel = TimeMLRelType.IS_INCLUDED;
			}
			
			if (etRel != null) {
				TimeMLRelType rel = etRel;
				if (tlink.getSource().getTLinkableType() != Type.EVENT)
					rel = TLink.getConverseTimeMLRelType(etRel);
				
				map.put(datum, rel);
			}
		}
	
		return map;
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
}

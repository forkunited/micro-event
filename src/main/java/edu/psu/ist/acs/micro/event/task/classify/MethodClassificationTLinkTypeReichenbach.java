package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLAspect;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLTense;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;

public class MethodClassificationTLinkTypeReichenbach extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	
	private static int SENTENCE_WINDOW = 1;
	private static boolean SAME_TENSE = false;
	private static boolean SAME_SENTENCE = true;
	private static boolean SIMPLIFY_PAST = false;
	private static boolean SIMPLIFY_PRESENT = false;
	private static boolean SIMPLIFY_ASPECT = true;
	private static boolean USE_EXTENDED_TENSE = true;
	//private static boolean USE_EXTENDED_TENSE_ACROSS_SENTENCE = true;
	
	
	public MethodClassificationTLinkTypeReichenbach() {
		
	}
	
	public MethodClassificationTLinkTypeReichenbach(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
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
		MethodClassificationTLinkTypeReichenbach clone = new MethodClassificationTLinkTypeReichenbach(this.context);
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeReichenbach(context);
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
		return "Reichenbach";
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		
		for (TLinkDatum<TimeMLRelType> datum : data) {
			TLink tlink = datum.getTLink();
			if (tlink.getType() != TLink.Type.EVENT_EVENT)
				continue;
			
			EventMention e1 = (EventMention)tlink.getSource();
			EventMention e2 = (EventMention)tlink.getTarget();
			
			TokenSpan e1Span = e1.getTokenSpan();
			TokenSpan e2Span = e2.getTokenSpan();
			if (!e1.getTokenSpan().getDocument().getName().equals(e2.getTokenSpan().getDocument().getName()))
				continue;
			
			if (e1Span.getSentenceIndex() == e2Span.getSentenceIndex() && !SAME_SENTENCE)
				continue;
			
			if (Math.abs(e1Span.getSentenceIndex() - e2Span.getSentenceIndex()) > SENTENCE_WINDOW)
				continue;
			
			DocumentNLP document = e1.getTokenSpan().getDocument();
			PoSTag pos1 = document.getPoSTag(e1.getTokenSpan().getSentenceIndex(), e1.getTokenSpan().getStartTokenIndex());
			PoSTag pos2 = document.getPoSTag(e2.getTokenSpan().getSentenceIndex(), e2.getTokenSpan().getStartTokenIndex());

			if (!PoSTagClass.classContains(PoSTagClass.VB, pos1) || !PoSTagClass.classContains(PoSTagClass.VB, pos2))
				continue;
			
			
			
			if (SAME_TENSE && e1.getTimeMLTense() != e2.getTimeMLTense())
				continue;
			
			TimeMLTense e1Tense = simplifyTense((USE_EXTENDED_TENSE) ? e1.getTimeMLExtendedTense() : e1.getTimeMLTense());
			TimeMLTense e2Tense = simplifyTense((USE_EXTENDED_TENSE) ? e2.getTimeMLExtendedTense() : e2.getTimeMLTense());
			TimeMLAspect e1Aspect = simplifyAspect(e1.getTimeMLAspect());
			TimeMLAspect e2Aspect = simplifyAspect(e2.getTimeMLAspect());
			
			boolean e1Past = (e1Tense == TimeMLTense.PAST);
			boolean e2Past = (e2Tense == TimeMLTense.PAST);
			boolean e1Pres = (e1Tense == TimeMLTense.PRESENT);
			boolean e2Pres = (e2Tense == TimeMLTense.PRESENT);		
			boolean e1Future = (e1Tense == TimeMLTense.FUTURE);
			boolean e2Future = (e2Tense == TimeMLTense.FUTURE);
			boolean e1Perf = (e1Aspect == TimeMLAspect.PERFECTIVE);
			boolean e1None = (e1Aspect == TimeMLAspect.NONE);
			boolean e2Perf = (e2Aspect == TimeMLAspect.PERFECTIVE);
			boolean e2None = (e2Aspect == TimeMLAspect.NONE);
			
			TimeMLRelType rel = null;
			if (e1Past && e1None && e2Past && e2Perf) 
				rel = TimeMLRelType.AFTER;    
			if (e1Past && e1None && e2Future && e2None) 
				rel = TimeMLRelType.BEFORE;
			if (e1Past && e1None && e2Future && e2Perf) 
				rel = TimeMLRelType.BEFORE; 
			if (e1Past && e1Perf && e2Past && e2None) 
				rel = TimeMLRelType.BEFORE ;
			if (e1Past && e1Perf && e2Pres && e2None) 
				rel = TimeMLRelType.BEFORE;
			if (e1Past && e1Perf && e2Pres && e2Perf) 
				rel = TimeMLRelType.BEFORE; 
			if (e1Past && e1Perf && e2Future && e2None) 
				rel = TimeMLRelType.BEFORE;
			if (e1Past && e1Perf && e2Future && e2Perf) 
				rel = TimeMLRelType.BEFORE;
			if (e1Pres && e1None && e2Past && e2Perf) 
				rel = TimeMLRelType.AFTER;
			if (e1Pres && e1None && e2Future && e2None) 
				rel = TimeMLRelType.BEFORE; 
			if (e1Pres && e1Perf && e2Past && e2Perf) 
				rel = TimeMLRelType.AFTER;    
			if (e1Pres && e1Perf && e2Future && e2None) 
				rel = TimeMLRelType.BEFORE;
			if (e1Pres && e1Perf && e2Future && e2Perf) 
				rel = TimeMLRelType.BEFORE;
			if (e1Future && e1None && e2Past && e2None) 
				rel = TimeMLRelType.AFTER;
			if (e1Future && e1None && e2Past && e2Perf) 
				rel = TimeMLRelType.AFTER; 
			if (e1Future && e1None && e2Pres && e2None) 
				rel = TimeMLRelType.AFTER; 
			if (e1Future && e1None && e2Pres && e2Perf) 
				rel = TimeMLRelType.AFTER;
			if (e1Future && e1Perf && e2Past && e2None) 
				rel = TimeMLRelType.AFTER;  
			if (e1Future && e1Perf && e2Past && e2Perf) 
				rel = TimeMLRelType.AFTER;
			if (e1Future && e1Perf && e2Pres && e2Perf) 
				rel = TimeMLRelType.AFTER;    

			if (rel != null) {
				map.put(datum, rel);
			}
		}
	
		return map;
	}
	
	private TimeMLTense simplifyTense(TimeMLTense tense){
		if (tense == TimeMLTense.PAST || (tense == TimeMLTense.PASTPART && SIMPLIFY_PAST)) 
			return TimeMLTense.PAST;
		else if (tense == TimeMLTense.PRESENT || (tense == TimeMLTense.PRESPART && SIMPLIFY_PRESENT)) 
			return TimeMLTense.PRESENT;
		else if (tense == TimeMLTense.FUTURE) 
			return tense;
		else 
			return null; 
	}
	
	private TimeMLAspect simplifyAspect(TimeMLAspect aspect){
		if ((SIMPLIFY_ASPECT && aspect == TimeMLAspect.PERFECTIVE_PROGRESSIVE) || aspect == TimeMLAspect.PERFECTIVE)
			return TimeMLAspect.PERFECTIVE;
		else if (aspect == TimeMLAspect.NONE) 
			return aspect;
		else 
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
}

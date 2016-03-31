package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLAspect;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLClass;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLTense;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;

public class MethodClassificationTLinkTypeReportingGovernor extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	
	public MethodClassificationTLinkTypeReportingGovernor() {
		
	}
	
	public MethodClassificationTLinkTypeReportingGovernor(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
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
		MethodClassificationTLinkTypeReportingGovernor clone = new MethodClassificationTLinkTypeReportingGovernor(this.context);
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeReportingGovernor(context);
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
		return "ReportingGovernor";
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		
		for (TLinkDatum<TimeMLRelType> datum : data) {
			TLink tlink = datum.getTLink();
			if (tlink.getType() != TLink.Type.EVENT_EVENT
					|| tlink.getPosition() != TLink.Position.WITHIN_SENTENCE)
				continue;
			
			EventMention e1 = (EventMention)tlink.getSource();
			EventMention e2 = (EventMention)tlink.getTarget();
			
			TokenSpan e1Span = e1.getTokenSpan();
			TokenSpan e2Span = e2.getTokenSpan();
			DocumentNLP document = e1.getTokenSpan().getDocument();
						
			DependencyParse deps = document.getDependencyParse(e1Span.getSentenceIndex());
			
			EventMention eGov = null;
			EventMention eDep = null;
			if (deps != null && deps.isDirectlyGoverning(e1Span.getStartTokenIndex(), e2Span.getStartTokenIndex())) {
				eGov = e1;
				eDep = e2;
			} else if (deps != null && deps.isDirectlyGoverning(e2Span.getStartTokenIndex(), e1Span.getStartTokenIndex())) {
				eGov = e2;
				eDep = e1;
			} else {
				ConstituencyParse parse = document.getConstituencyParse(e1Span.getSentenceIndex());
				if (parse == null)
					continue;
				
				if (parse.isDominating(e1Span.getStartTokenIndex(), e2Span.getStartTokenIndex())) {
					eGov = e1;
					eDep = e2;
				} else if (parse.isDominating(e2Span.getStartTokenIndex(), e1Span.getStartTokenIndex())) {
					eGov = e2;
					eDep = e1;
				} else {
					continue;
				}
			}
			
			if (eGov.getTimeMLClass() != TimeMLClass.REPORTING || eDep.getTimeMLClass() == TimeMLClass.REPORTING)
				continue;
			
			TimeMLRelType govDepRel = getRelation(eGov.getTimeMLTense(), eGov.getTimeMLAspect(), eDep.getTimeMLTense(), eDep.getTimeMLAspect());
			if (govDepRel != null) {
				TimeMLRelType rel = govDepRel;
				if (!e1.getId().equals(eGov.getId()))
					rel = TLink.getConverseTimeMLRelType(govDepRel);
				
				map.put(datum, rel);
			}
		}
	
		return map;
	}
	
	/**
	 * Determine the event-event relation from just the tenses and aspects.
	 * @param t1 Governing event's tense
	 * @param a1 Governing event's aspect
	 * @param t2 Dependent event's tense
	 * @param a2 Dependent event's aspect
	 * @return Relation from the rules, or null if no rule applied.
	 */
	private TimeMLRelType getRelation(TimeMLTense t1, TimeMLAspect a1, TimeMLTense t2, TimeMLAspect a2) {
		TimeMLRelType relation = null;

		switch (t1) {

			// gov=PAST
			case PAST:
				switch (t2) {
	
				// gov=PAST, dep=PAST
				case PAST:
					switch (a2) {
					case NONE: // p=0.95 20 of 21 (ignoring VAGUE)
					case PERFECTIVE: // p=1.00 1 of 1 (ignoring VAGUE)
						relation = TimeMLRelType.AFTER;
						break;
					case PROGRESSIVE: // p=1.00 1 of 1
						relation = TimeMLRelType.IS_INCLUDED;
						break;
						// never occurs in training data
					case PERFECTIVE_PROGRESSIVE:
					case IMPERFECTIVE:
					case IMPERFECTIVE_PROGRESSIVE:
						break;
					}
					break;
	
					// gov=PAST, dep=PRESENT
				case PRESENT:
					switch (a2) {
					case PERFECTIVE: // p=1.00 6 of 6 (ignoring VAGUE)
						relation = TimeMLRelType.AFTER;
						break;
					case PROGRESSIVE: // p=1.00 4 of 4 (ignoring VAGUE)
					case PERFECTIVE_PROGRESSIVE: // p=1.00 2 of 2 (ignoring VAGUE)
						relation = TimeMLRelType.IS_INCLUDED;
						break;
					case NONE: // p=0.71 22 of 31, but no linguistic reason why
						//relation = TimeMLRelType.AFTER; // FIXME Remove
					case IMPERFECTIVE: // p=0.50 5 of 10, but no linguistic reason
						//relation = TimeMLRelType.IS_INCLUDED; // FIXME Remove 
						break;
	
						// never occurs in training data
					case IMPERFECTIVE_PROGRESSIVE:
						break;
					}
					break;
	
					// gov=PAST, dep=FUTURE
				case FUTURE: // p=1.00 5 of 5 (ignoring VAGUE)
					relation = TimeMLRelType.BEFORE;
					break;
	
					// gov=PAST, dep=NONE
				case NONE: // p=0.70 14 of 20, but no linguistic reason why
					//relation = TimeMLRelType.BEFORE; // FIXME Remove 
					break;
	
					// never occurs in training data
				case INFINITIVE:
				case PASSIVE:
				case PASTPART:
				case PRESPART:
					break;
				}
				break;
				// gov=PRESENT
			case PRESENT:
				switch (t2) {
	
				// gov=PRESENT, dep=PAST
				case PAST: // not in data, but makes linguistic sense
					relation = TimeMLRelType.AFTER;
					break;
	
					// gov=PRESENT, dep=PRESENT
				case PRESENT:
					switch (a2) {
					case PERFECTIVE: // p=1.00 1 of 1 (ignoring VAGUE)
						relation = TimeMLRelType.AFTER;
						break;
	
					case NONE: // p=1.00 2 of 2, but no linguistic reason why
						//relation = TimeMLRelType.IS_INCLUDED; // FIXME Remove 
						break;
	
						// never occurs in training data
					case PROGRESSIVE:
					case PERFECTIVE_PROGRESSIVE:
					case IMPERFECTIVE:
					case IMPERFECTIVE_PROGRESSIVE:
						break;
					}
					break;
	
					// gov=PRESENT, dep=FUTURE
				case FUTURE: // p=1.00 2 of 2 (ignoring VAGUE)
					relation = TimeMLRelType.BEFORE;
					break;
	
					// gov=PRESENT, dep=... only 0-1 occurrences in training data
				case NONE:
				case INFINITIVE:
				case PASSIVE:
				case PASTPART:
				case PRESPART:
					break;
				}
				break;
	
				// gov=... only 0-1 occurrences in training data
			case NONE:
			case PRESPART:
			case FUTURE:
			case INFINITIVE:
			case PASSIVE:
			case PASTPART:
				break;
		}
		return relation;
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

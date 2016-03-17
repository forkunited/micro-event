package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.DependencyPath;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLAspect;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLClass;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLTense;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;

public class MethodClassificationTLinkTypeGeneralGovernor extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	
	public MethodClassificationTLinkTypeGeneralGovernor() {
		
	}
	
	public MethodClassificationTLinkTypeGeneralGovernor(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
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
		return new MethodClassificationTLinkTypeGeneralGovernor(this.context);
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeGeneralGovernor(context);
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
		return "GeneralGovernor";
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		int correct = 0;
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
			DependencyPath path = document.getDependencyParse(e1Span.getSentenceIndex()).getPath(e1Span.getStartTokenIndex(), e2Span.getStartTokenIndex());
			if (path == null || path.getDependencyLength() != 1)
				continue;

			String type = path.getDependencyType(0);
			EventMention eGov = null;
			EventMention eDep = null;
			if (path.isAllGoverning()) {
				eGov = e1;
				eDep = e2;
			} else {
				eGov = e2;
				eDep = e1;
			}
			
			TimeMLRelType govDepRel = null;
			if (type.equals("xcomp"))
				govDepRel = classifyEventPair_xcomp(eGov, eDep);
			else if (type.equals("ccomp"))
				govDepRel = classifyEventPair_ccomp(eGov, eDep);
			else if (type.equals("conj_and"))
				govDepRel = classifyEventPair_conj_and(eGov, eDep);
			else if (type.equals("nsubj")) 
				govDepRel = classifyEventPair_nsubj(eGov, eDep);
			else if (type.equals("advcl")) 
				govDepRel = classifyEventPair_advcl(eGov, eDep);
			else if (type.equals("conj_but"))
				govDepRel = classifyEventPair_conj_but(eGov, eDep);
			else if (type.equals("conj_or"))
				govDepRel = classifyEventPair_conj_or(eGov, eDep);
			else if (type.equals("dobj"))
				govDepRel = classifyEventPair_dobj(eGov, eDep);
			 
			if (govDepRel != null) {
				TimeMLRelType rel = govDepRel;
				if (!e1.getId().equals(eGov.getId()))
					rel = TLink.getConverseTimeMLRelType(govDepRel);
				
				if (datum.getLabel().equals(rel))
					correct++;
				
				System.out.println(data.getReferenceName() + " OUTPUT FROM GENGOV : " + type + " " + eGov.getSourceInstanceId() + "->" + eDep.getSourceInstanceId() + "=" + govDepRel.toString() + " (" + rel + ")" + datum.getLabel().equals(rel) + " TRUE-" + datum.getLabel());
				
				map.put(datum, rel);
			}
		}
		System.out.println("PRECISION " + correct + " " + map.size());
		return map;
	}
	
	private TimeMLRelType classifyEventPair_conj_or(EventMention eGov, EventMention eDep) {
		return TimeMLRelType.VAGUE;
	}

	private TimeMLRelType classifyEventPair_dobj(EventMention eGov, EventMention eDep) {
		TimeMLClass eGovClass = eGov.getTimeMLClass();
		TimeMLTense eDepTense = eDep.getTimeMLTense();
		TimeMLAspect eDepAspect = eDep.getTimeMLAspect();
	 
		if (eGovClass == TimeMLClass.ASPECTUAL) {
			return TimeMLRelType.IS_INCLUDED;
		} else if (eDepTense == TimeMLTense.NONE && eDepAspect == TimeMLAspect.NONE) {
			TokenSpan depSpan = eDep.getTokenSpan();
			DependencyParse parse = depSpan.getDocument().getDependencyParse(depSpan.getSentenceIndex());
			List<Dependency> deps = parse.getGovernedDependencies(depSpan.getStartTokenIndex());
			for (Dependency dep : deps) {
				if (dep.getType().equals("det")) {
					String detStr = depSpan.getDocument().getTokenStr(depSpan.getSentenceIndex(), dep.getDependentTokenIndex()).toLowerCase();
					if (detStr.equals("a"))
						return TimeMLRelType.BEFORE;
				}
			}
		} else {
			return TimeMLRelType.VAGUE;
		}
		
		return null;
	}

	private TimeMLRelType classifyEventPair_conj_but(EventMention eGov, EventMention eDep) {
		return TimeMLRelType.BEFORE;
	}

	private TimeMLRelType classifyEventPair_advcl(EventMention eGov, EventMention eDep) {
		// Find the "marker" (i.e. the word that introduces the adverbial clause complement (i.e. the dependent)).
		String mark = null;
		
		TokenSpan depSpan = eDep.getTokenSpan();
		DependencyParse parse = depSpan.getDocument().getDependencyParse(depSpan.getSentenceIndex());
		List<Dependency> deps = parse.getGovernedDependencies(depSpan.getStartTokenIndex());
		for (Dependency dep : deps) {
			if (dep.getType().equals("mark"))
				mark = depSpan.getDocument().getTokenStr(depSpan.getSentenceIndex(), dep.getDependentTokenIndex());
		}

		if (mark == null) {
			for (Dependency dep : deps) {
				if (dep.getType().equals("advmod"))
					mark = depSpan.getDocument().getTokenStr(depSpan.getSentenceIndex(), dep.getDependentTokenIndex());
			}
		}
		 
		if (mark != null && mark.toLowerCase().equals("until")) {
			return TimeMLRelType.BEFORE;
		} else if (mark != null && mark.toLowerCase().equals("once")) {
			return TimeMLRelType.AFTER;
		} else if (mark != null && mark.toLowerCase().equals("after")) {
			return TimeMLRelType.AFTER;
		} else if (mark != null && mark.toLowerCase().equals("before")) {
			return TimeMLRelType.BEFORE;
		} else if (mark != null && mark.toLowerCase().equals("since")) {
			return TimeMLRelType.AFTER;
		} else if (mark != null && mark.toLowerCase().equals("when")) {
			return TimeMLRelType.VAGUE;
		} else if (mark != null && mark.toLowerCase().equals("as")) {
			return TimeMLRelType.SIMULTANEOUS;
		} else if (mark != null && mark.toLowerCase().equals("because")) {
			return TimeMLRelType.BEFORE;
		} else {
			return null;
		} 
	}

	private TimeMLRelType classifyEventPair_nsubj(EventMention eGov, EventMention eDep) {
		TimeMLClass eGovClass = eGov.getTimeMLClass();
		if (eGovClass == TimeMLClass.ASPECTUAL) {
			return TimeMLRelType.IS_INCLUDED;
		}
		return TimeMLRelType.VAGUE;
	}

	private TimeMLRelType classifyEventPair_conj_and(EventMention eGov, EventMention eDep) {
		return TimeMLRelType.VAGUE;
	}

	private TimeMLRelType classifyEventPair_xcomp(EventMention eGov, EventMention eDep) {
		TimeMLTense eDepTense = eDep.getTimeMLTense();
		TimeMLClass eGovClass = eGov.getTimeMLClass();
		
		if (eDepTense == TimeMLTense.PRESPART) { 
			if (eGovClass == TimeMLClass.OCCURRENCE) {
				return TimeMLRelType.SIMULTANEOUS;
			} else {
				return TimeMLRelType.IS_INCLUDED;
			}
		} else if (eGovClass == TimeMLClass.ASPECTUAL) {
			return TimeMLRelType.IS_INCLUDED;
		} else {
			return TimeMLRelType.BEFORE;
		}
	}

	private TimeMLRelType classifyEventPair_ccomp(EventMention eGov, EventMention eDep) {
		TimeMLTense eDepTense = eDep.getTimeMLExtendedTense();
		TimeMLTense eGovTense = eGov.getTimeMLExtendedTense();

		TimeMLClass eDepClass = eDep.getTimeMLClass();
		TimeMLClass eGovClass = eGov.getTimeMLClass();
		TimeMLAspect eDepAspect = eDep.getTimeMLAspect();
		TimeMLAspect eGovAspect = eGov.getTimeMLAspect();
	
		if (eDepTense == TimeMLTense.FUTURE) {
			return TimeMLRelType.BEFORE;
		} else if (eDepAspect == TimeMLAspect.PERFECTIVE) {
			if (eGovAspect == TimeMLAspect.NONE)
				return TimeMLRelType.AFTER;
			else if (eGovTense == TimeMLTense.PAST)
				return TimeMLRelType.AFTER;
			else if (eGovClass == TimeMLClass.REPORTING)
				return TimeMLRelType.AFTER;
			else if (eDepClass == TimeMLClass.OCCURRENCE)
				return TimeMLRelType.AFTER;
		} else if (eGovAspect == TimeMLAspect.PERFECTIVE) {
			return TimeMLRelType.VAGUE; 
		} else if (eDepTense == TimeMLTense.PAST) {
			if (eGovTense == TimeMLTense.PAST) {
				if (eGovAspect == TimeMLAspect.NONE && eDepAspect == TimeMLAspect.NONE) {
					return TimeMLRelType.AFTER;
				}
			}
		} else if (eDepTense == TimeMLTense.PRESENT) {
			if (eGovClass == TimeMLClass.REPORTING) {
				return TimeMLRelType.IS_INCLUDED;
			} else if ((eDepClass == TimeMLClass.STATE || eDepClass == TimeMLClass.I_STATE) &&
								 (eGovTense == TimeMLTense.PRESENT && eGovAspect == TimeMLAspect.PROGRESSIVE)) {
				return TimeMLRelType.IS_INCLUDED;
			} else
				return TimeMLRelType.VAGUE;
		}
		
		return null;
	}
}

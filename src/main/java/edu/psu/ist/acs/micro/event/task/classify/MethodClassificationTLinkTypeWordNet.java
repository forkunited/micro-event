package edu.psu.ist.acs.micro.event.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.WordNet;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;

public class MethodClassificationTLinkTypeWordNet extends MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context;
	
	public MethodClassificationTLinkTypeWordNet() {
		
	}
	
	public MethodClassificationTLinkTypeWordNet(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
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
		MethodClassificationTLinkTypeWordNet clone = new MethodClassificationTLinkTypeWordNet(this.context);
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new MethodClassificationTLinkTypeWordNet(context);
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
		return "WordNet";
	}
	
	@Override
	public Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> classify(DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data) {
		Map<TLinkDatum<TimeMLRelType>, TimeMLRelType> map = new HashMap<TLinkDatum<TimeMLRelType>, TimeMLRelType>();
		WordNet wordNet = this.context.getDataTools().getWordNet();
		
		for (TLinkDatum<TimeMLRelType> datum : data) {
			TLink tlink = datum.getTLink();
			
			if (tlink.getType() != TLink.Type.TIME_TIME && tlink.getType() != TLink.Type.EVENT_EVENT)
				continue;
			
			TokenSpan sourceSpan = datum.getTLink().getSource().getTokenSpan();
			TokenSpan targetSpan = datum.getTLink().getTarget().getTokenSpan();
			if (sourceSpan.getSentenceIndex() < 0 || targetSpan.getSentenceIndex() < 0)
				continue;
			
			DocumentNLP sourceDocument = sourceSpan.getDocument();
			DocumentNLP targetDocument = targetSpan.getDocument();

			String sourceWord = sourceDocument.getTokenStr(sourceSpan.getSentenceIndex(), sourceSpan.getEndTokenIndex() - 1);
			String targetWord = targetDocument.getTokenStr(targetSpan.getSentenceIndex(), targetSpan.getEndTokenIndex() - 1);
			PoSTag sourcePos = sourceDocument.getPoSTag(sourceSpan.getSentenceIndex(), sourceSpan.getEndTokenIndex() - 1);
			PoSTag targetPos = targetDocument.getPoSTag(targetSpan.getSentenceIndex(), targetSpan.getEndTokenIndex() - 1);
			String sourceLemma = wordNet.getLemma(sourceWord, sourcePos);
			String targetLemma = wordNet.getLemma(targetWord, targetPos);
			
			TimeMLRelType rel = null;
			if (tlink.getType() == TLink.Type.TIME_TIME) {
				if (sourceLemma.equals(targetLemma))
					rel = TimeMLRelType.SIMULTANEOUS;
			} else { // EVENT_EVENT
				if (sourcePos == targetPos && wordNet.areSynonyms(sourceWord, sourcePos, targetWord, targetPos))
					rel = TimeMLRelType.VAGUE;
				else if (sourceLemma != null && sourceLemma.equals(targetLemma))
					rel = TimeMLRelType.VAGUE;
			}
			
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

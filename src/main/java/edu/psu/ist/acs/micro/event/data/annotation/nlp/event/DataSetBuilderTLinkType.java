package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Singleton;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TextDirection;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;

public class DataSetBuilderTLinkType extends DataSetBuilderDocumentFiltered<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private enum CrossDocumentMode {
		NONE,
		ALL,
		ONLY_TIME_TIME
	}
	
	private enum DirectionMode {
		FORWARD,
		BACKWARD,
		ALL
	}
	
	private DirectionMode directionMode = DirectionMode.FORWARD;
	private String tlinks;
	private int maxSentenceDistance = -1;
	private CrossDocumentMode crossDocMode = CrossDocumentMode.NONE;
	private String[] parameterNames = { "directionMode", "tlinks", "maxSentenceDistance", "crossDocMode" };
	
	public DataSetBuilderTLinkType() {
		this(null);
	}
	
	public DataSetBuilderTLinkType(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		String[] parentParameterNames = super.getParameterNames();
		String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + parentParameterNames.length);
		for (int i = 0; i < parentParameterNames.length; i++)
			parameterNames[this.parameterNames.length + i] = parentParameterNames[i];
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("tlinks"))
			return (this.tlinks == null) ? null : Obj.stringValue(this.tlinks.toString());
		else if (parameter.equals("maxSentenceDistance"))
			return Obj.stringValue(String.valueOf(this.maxSentenceDistance));
		else if (parameter.equals("crossDocMode"))
			return Obj.stringValue(String.valueOf(this.crossDocMode));
		else if (parameter.equals("directionMode"))
			return Obj.stringValue(String.valueOf(this.directionMode));
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tlinks"))
			this.tlinks = (parameterValue == null) ? null : this.context.getMatchValue(parameterValue);
		else if (parameter.equals("maxSentenceDistance"))
			this.maxSentenceDistance = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("crossDocMode"))
			this.crossDocMode = CrossDocumentMode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("directionMode"))
			this.directionMode = DirectionMode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
	
	@Override
	public DataSetBuilder<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new DataSetBuilderTLinkType(context);
	}

	private boolean linkMeetsCrossDocumentMode(TLink link, boolean labeled) {
		return !link.isBetweenDocuments() || this.crossDocMode == CrossDocumentMode.ALL || (this.crossDocMode == CrossDocumentMode.ONLY_TIME_TIME && link.getType() == TLink.Type.TIME_TIME);
	}
	
	@Override
	public DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> build() {
		StoredItemSetInMemoryLazy<TLink, TLink> tlinkSet = (this.tlinks == null) ? null :
				this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.tlinks);
		
		DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data = new DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType>(this.context.getDatumTools());
		
		Map<String, Set<String>> labeledPairs = new HashMap<>();
		
		Singleton<Integer> idObj = new Singleton<>(0);
		
		if (tlinkSet != null) {
			tlinkSet.map(new Fn<TLink, Boolean>() {
				@Override
				public Boolean apply(TLink tlink) {
					DocumentNLP doc = tlink.getSource().getTokenSpan().getDocument();
					if (!matchesFilter(doc))
						return true;
					
					int sourceSI = tlink.getSource().getTokenSpan().getSentenceIndex();
					int targetSI = tlink.getTarget().getTokenSpan().getSentenceIndex();
					if (maxSentenceDistance >= 0 && 
							sourceSI >= 0 && targetSI >= 0 && 
							Math.abs(sourceSI - targetSI) > maxSentenceDistance) {
						return true;
					}
					
					TextDirection d = tlink.getTextDirection();
					if (directionMode == DirectionMode.FORWARD && d != TextDirection.FORWARD && d != TextDirection.NONE)
						return true;
					if (directionMode == DirectionMode.BACKWARD && d != TextDirection.BACKWARD && d != TextDirection.NONE)
						return true;
						
					synchronized (data) {
						if (!labeledPairs.containsKey(tlink.getSource().getId()))
							labeledPairs.put(tlink.getSource().getId(), new HashSet<>());
						labeledPairs.get(tlink.getSource().getId()).add(tlink.getTarget().getId());				
						
						if (labelMode != LabelMode.ONLY_UNLABELED && linkMeetsCrossDocumentMode(tlink, true)) {	
							data.add(new TLinkDatum<TimeMLRelType>(
								Integer.valueOf(tlink.getId()), tlink, (labelMapping != null) ? labelMapping.map(tlink.getTimeMLRelType()) : tlink.getTimeMLRelType()));
						}
						idObj.set(Math.max(idObj.get(), Integer.valueOf(tlink.getId())));
					}
					
					return true;
				}
				
			}, this.context.getMaxThreads(), this.context.getDataTools().getGlobalRandom());
		}
		
		PairFn<Pair<TokenSpan, StoreReference>, TLink> fn = new PairFn<Pair<TokenSpan, StoreReference>, TLink>() {
			@Override
			public TLink apply(Pair<TokenSpan, StoreReference> o1, Pair<TokenSpan, StoreReference> o2) {
				String id1 = o1.getSecond().getIndexValue(0).toString();
				String id2 = o2.getSecond().getIndexValue(0).toString();
				
				if (maxSentenceDistance >= 0 && o1.getFirst() != null && o2.getFirst() != null) {
					int si1 = o1.getFirst().getSentenceIndex();
					int si2 = o2.getFirst().getSentenceIndex();
					if (o1.getFirst().getDocument().getName().equals(o2.getFirst().getDocument().getName()) 
							&& si1 >= 0 && si2 >= 0 && Math.abs(si1 - si2) > maxSentenceDistance)
						return null;
				}
				
				TLink link = new TLink(context.getDataTools(), null, id1 + "_" + id2, null, o1.getSecond(), o2.getSecond(), null,null,null);
				TextDirection d = link.getTextDirection();
				if (directionMode == DirectionMode.FORWARD && d != TextDirection.FORWARD && d != TextDirection.NONE)
					return null;
				if (directionMode == DirectionMode.BACKWARD && d != TextDirection.BACKWARD && d != TextDirection.NONE)
					return null;

				if (labeledPairs.containsKey(id1) && labeledPairs.get(id1).contains(id2))
					return null;
				else if (linkMeetsCrossDocumentMode(link, false))
					return link;
				else
					return null;
			}
		};
		
		if (this.labelMode != LabelMode.ONLY_LABELED || this.crossDocMode != CrossDocumentMode.NONE) {
			Map<String, Set<String>> documentClusters = getDocumentClusters();
			DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> docs = getDocuments();
			TreeMap<String, TLink> unlabeledLinks = new TreeMap<String, TLink>();
			ThreadMapper<Entry<String, Set<String>>, Boolean> threads = new ThreadMapper<Entry<String, Set<String>>, Boolean>(
				new Fn<Entry<String, Set<String>>, Boolean>() {
					@Override
					public Boolean apply(Entry<String, Set<String>> item) {
						List<TLink> links = new ArrayList<TLink>();
						
						for (String docName1 : item.getValue()) {
							DocumentNLP doc1 = docs.getDocumentByName(docName1, true);
							List<Pair<TokenSpan, StoreReference>> doc1Mentions = doc1.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION);
							doc1Mentions.addAll(doc1.getTokenSpanAnnotations(AnnotationTypeNLPEvent.TIME_EXPRESSION));
							doc1Mentions.add(new Pair<TokenSpan, StoreReference>(null, doc1.getDocumentAnnotation(AnnotationTypeNLPEvent.CREATION_TIME)));
							
							if (labelMode != LabelMode.ONLY_LABELED)
								links = runAllPairs(doc1Mentions, fn, links, true); 
							
							if (crossDocMode == CrossDocumentMode.NONE)
								continue;
							
							for (String docName2 : item.getValue()) {
								if (docName1.equals(docName2))
									continue;
								
								DocumentNLP doc2 = docs.getDocumentByName(docName2, true);
								List<Pair<TokenSpan, StoreReference>> doc2Mentions = doc2.getTokenSpanAnnotations(AnnotationTypeNLPEvent.TIME_EXPRESSION);
								doc2Mentions.add(new Pair<TokenSpan, StoreReference>(null, doc2.getDocumentAnnotation(AnnotationTypeNLPEvent.CREATION_TIME)));
								doc2Mentions.addAll(doc2.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION));
								
								links = runAllPairs(doc1Mentions, doc2Mentions, fn, links); 
							}
						}
						
						synchronized (unlabeledLinks) {
							for (TLink link : links)
								unlabeledLinks.put(link.getId(), link);
						}
						
						return true;
					}
				});
			
			threads.run(documentClusters.entrySet(), this.context.getMaxThreads());
			
			int id = idObj.get() + 1;
			for (Entry<String, TLink> entry : unlabeledLinks.entrySet()) {
				data.add(new TLinkDatum<TimeMLRelType>(id, entry.getValue(), null));
				id++;
			}
				
		}
		
		return data;
	}

	@Override
	public String getGenericName() {
		return "TLinkType";
	}

}

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
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;

public class DataSetBuilderTLinkType extends DataSetBuilderDocumentFiltered<TLinkDatum<TimeMLRelType>, TimeMLRelType> {
	private String tlinks;
	private String[] parameterNames = { "tlinks" };
	
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
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tlinks"))
			this.tlinks = (parameterValue == null) ? null : this.context.getMatchValue(parameterValue);
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
	
	@Override
	public DataSetBuilder<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new DataSetBuilderTLinkType(context);
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
					
					synchronized (data) {
						if (!labeledPairs.containsKey(tlink.getSource().getId()))
							labeledPairs.put(tlink.getSource().getId(), new HashSet<>());
						labeledPairs.get(tlink.getSource().getId()).add(tlink.getTarget().getId());
						
						if (labelMode != LabelMode.ONLY_UNLABELED)	
							data.add(new TLinkDatum<TimeMLRelType>(
									Integer.valueOf(tlink.getId()), tlink, (labelMapping != null) ? labelMapping.map(tlink.getTimeMLRelType()) : tlink.getTimeMLRelType()));
					
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
				if (labeledPairs.containsKey(id1) && labeledPairs.get(id1).contains(id2))
					return null;
				else
					return new TLink(context.getDataTools(), null, id1 + "_" + id2, null,o1.getSecond(), o2.getSecond(), null,null,null);
			}
		};
		
		if (this.labelMode != LabelMode.ONLY_LABELED) {
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
								links = runAllPairs(doc1Mentions, fn, links, true); 

								for (String docName2 : item.getValue()) {
									if (docName1.equals(docName2))
										continue;
									
									DocumentNLP doc2 = docs.getDocumentByName(docName2, true);
									List<Pair<TokenSpan, StoreReference>> doc2Mentions = doc2.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION);
									doc2Mentions.addAll(doc2.getTokenSpanAnnotations(AnnotationTypeNLPEvent.TIME_EXPRESSION));
									doc2Mentions.add(new Pair<TokenSpan, StoreReference>(null, doc2.getDocumentAnnotation(AnnotationTypeNLPEvent.CREATION_TIME)));
									
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
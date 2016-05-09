package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;

public class DataSetBuilderEventCoref extends DataSetBuilderDocumentFiltered<EventPairDatum<CorefRelType>, CorefRelType> {
	//private String[] parameterNames = {  };
	
	public DataSetBuilderEventCoref() {
		this(null);
	}
	
	public DataSetBuilderEventCoref(DatumContext<EventPairDatum<CorefRelType>, CorefRelType> context) {
		this.context = context;
	}
	
	@Override
	public DataSetBuilder<EventPairDatum<CorefRelType>, CorefRelType> makeInstance(
			DatumContext<EventPairDatum<CorefRelType>, CorefRelType> context) {
		return new DataSetBuilderEventCoref(context);
	}

	@Override
	public DataSet<EventPairDatum<CorefRelType>, CorefRelType> build() {
		DataSet<EventPairDatum<CorefRelType>, CorefRelType> data = new DataSet<EventPairDatum<CorefRelType>, CorefRelType>(this.context.getDatumTools());

		PairFn<Event, Triple<Event, Event, CorefRelType>> fn = new PairFn<Event, Triple<Event, Event, CorefRelType>>() {
			@Override
			public Triple<Event, Event, CorefRelType> apply(Event event1, Event event2) {
				if (event1 == null || event2 == null) {
					if (labelMode == LabelMode.ONLY_LABELED)
						return null;
					return new Triple<Event, Event, CorefRelType>(event1, event2, null);
				} else {
					if (labelMode == LabelMode.ONLY_UNLABELED)
						return null;
					return new Triple<Event, Event, CorefRelType>(event1, event2, 
							(event1.getId().equals(event2.getId()) ? CorefRelType.COREF : CorefRelType.NOT_COREF));
				}
			}
		};

		Map<String, Set<String>> documentClusters = getDocumentClusters();
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> docs = getDocuments();
		TreeMap<String, Triple<Event, Event, CorefRelType>> datums = new TreeMap<>();
		ThreadMapper<Entry<String, Set<String>>, Boolean> threads = new ThreadMapper<Entry<String, Set<String>>, Boolean>(
			new Fn<Entry<String, Set<String>>, Boolean>() {
				@Override
				public Boolean apply(Entry<String, Set<String>> item) {
					Set<String> doneDocs = new HashSet<String>();
					List<Triple<Event, Event, CorefRelType>> cDatums = new ArrayList<>();
					
					for (String docName1 : item.getValue()) {
						DocumentNLP doc1 = docs.getDocumentByName(docName1, true);
						
						List<Pair<TokenSpan, StoreReference>> spanMentions1 = doc1.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION);
						List<Event> events1 = new ArrayList<>();
						for (Pair<TokenSpan, StoreReference> spanMention : spanMentions1) {
							EventMention mention = context.getDataTools().getStoredItemSetManager().resolveStoreReference(spanMention.getSecond(), true);
							List<StoreReference> singleMentions = new ArrayList<>();
							singleMentions.add(mention.getStoreReference());
							Event singletonEvent = new Event(context.getDataTools(), 
															 null, 
															 mention.getId(),
															 mention.getEvent().getACEType(),
															 mention.getEvent().getACEGenericity(),
															 mention.getEvent().getACESubtype(),
															 new ArrayList<Pair<StoreReference, String>>(),
															 singleMentions);
															 
							events1.add(singletonEvent);
						}
						
						cDatums = runAllPairs(events1, fn, cDatums, false);
						
						doneDocs.add(docName1);
						for (String docName2 : item.getValue()) {
							if (doneDocs.contains(docName2))
								continue;
							
							DocumentNLP doc2 = docs.getDocumentByName(docName2, true);
							List<Pair<TokenSpan, StoreReference>> spanMentions2 = doc2.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION);
							List<Event> events2 = new ArrayList<>();
							for (Pair<TokenSpan, StoreReference> spanMention : spanMentions2) {
								EventMention mention = context.getDataTools().getStoredItemSetManager().resolveStoreReference(spanMention.getSecond(), true);
								List<StoreReference> singleMentions = new ArrayList<>();
								singleMentions.add(mention.getStoreReference());
								Event singletonEvent = new Event(context.getDataTools(), 
																 null, 
																 mention.getId(),
																 mention.getEvent().getACEType(),
																 mention.getEvent().getACEGenericity(),
																 mention.getEvent().getACESubtype(),
																 new ArrayList<Pair<StoreReference, String>>(),
																 singleMentions);
																 
								events2.add(singletonEvent);
							}
							
							cDatums = runAllPairs(events1, events2, fn, cDatums); 
						}
					}
					
					synchronized (datums) {
						for (Triple<Event, Event, CorefRelType> datum : cDatums)
							datums.put(datum.getFirst().getId() + "_" + datum.getSecond().getId(), datum);
					}
					
					return true;
				}
			});
		
		threads.run(documentClusters.entrySet(), this.context.getMaxThreads());
		
		int id = 1;
		for (Entry<String, Triple<Event, Event, CorefRelType>> entry : datums.entrySet()) {
			data.add(new EventPairDatum<CorefRelType>(id, entry.getValue().getFirst(), entry.getValue().getSecond(), entry.getValue().getThird()));
			id++;
		}
				
		return data;
	}

	@Override
	public String getGenericName() {
		return "EventCoref";
	}

}

package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;

public class DataSetBuilderACE2005EventMentionPairCoref extends DataSetBuilderACE2005<EventMentionPairDatum<CorefRelType>, CorefRelType> {
	public DataSetBuilderACE2005EventMentionPairCoref() {
		this(null);
	}
	
	public DataSetBuilderACE2005EventMentionPairCoref(DatumContext<EventMentionPairDatum<CorefRelType>, CorefRelType> context) {
		this.context = context;
	}

	@Override
	public DataSetBuilder<EventMentionPairDatum<CorefRelType>, CorefRelType> makeInstance(DatumContext<EventMentionPairDatum<CorefRelType>, CorefRelType> context) {
		return new DataSetBuilderACE2005EventMentionPairCoref(context);
	}

	@Override
	public String getGenericName() {
		return "ACE2005EventMentionPairCoref";
	}
	
	@Override
	public DataSet<EventMentionPairDatum<CorefRelType>, CorefRelType> build() {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> docs = 
				new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.collection));
				
		Set<String> devDocs = new HashSet<String>();
		Set<String> testDocs = new HashSet<String>();
		devDocs.addAll(Arrays.asList(DEV_DOC_NAMES));
		testDocs.addAll(Arrays.asList(TEST_DOC_NAMES));
		
		List<Triple<EventMention, EventMention, CorefRelType>> dataList = new ArrayList<>();
		
		docs.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP doc) {
				String docName = doc.getName();
				if ((part == Part.DEV && !devDocs.contains(docName)
				||  (part == Part.TEST && !testDocs.contains(docName))
				||  (part == Part.TRAIN && (devDocs.contains(docName) || testDocs.contains(docName)))))
					return true;
				
				List<Pair<TokenSpan, StoreReference>> spanMentions = doc.getTokenSpanAnnotations(AnnotationTypeNLPEvent.EVENT_MENTION);
				List<EventMention> mentions = new ArrayList<>();
				for (Pair<TokenSpan, StoreReference> spanMention : spanMentions) {
					mentions.add(context.getDataTools().getStoredItemSetManager().resolveStoreReference(spanMention.getSecond(), true));
				}
				
				Set<String> doneMentions = new HashSet<>();
				for (EventMention mention1 : mentions) {
					String eventId1 = mention1.getEvent().getId();
					doneMentions.add(mention1.getId());
					for (EventMention mention2 : mentions) {
						if (doneMentions.contains(mention2.getId()))
							continue;
						String eventId2 = mention2.getEvent().getId();
						
						CorefRelType relType = null;
						if (eventId1.equals(eventId2)) {
							relType = CorefRelType.COREF;
						} else {
							relType = CorefRelType.NOT_COREF;
						}
						
						synchronized (dataList) {
							dataList.add(new Triple<>(mention1, mention2, relType));
						}
					}
				}
				
				return true;
			}
			
		}, this.context.getMaxThreads(), this.context.getDataTools().getGlobalRandom());
		
		Collections.sort(dataList, new Comparator<Triple<EventMention, EventMention, CorefRelType>>() {
			@Override
			public int compare(
					Triple<EventMention, EventMention, CorefRelType> o1,
					Triple<EventMention, EventMention, CorefRelType> o2) {
				return (o1.getFirst().getId() + "_" + o1.getSecond().getId())
						.compareTo(
						o2.getFirst().getId() + "_" + o2.getSecond().getId());
			}
		
		});
		
		DataSet<EventMentionPairDatum<CorefRelType>, CorefRelType> data = new DataSet<EventMentionPairDatum<CorefRelType>, CorefRelType>(this.context.getDatumTools());
		for (Triple<EventMention, EventMention, CorefRelType> triple : dataList) {
			data.add(new EventMentionPairDatum<CorefRelType>(getNextDatumId(), triple.getFirst(), triple.getSecond(), triple.getThird()));
		}
		
		return data;
	}
	
}

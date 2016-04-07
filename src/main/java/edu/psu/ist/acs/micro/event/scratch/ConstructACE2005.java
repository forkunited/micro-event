package edu.psu.ist.acs.micro.event.scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPMateTools;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford.BSONTokenizer;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.psu.ist.acs.micro.event.data.EventDataTools;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.ACEDocumentType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Entity;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EntityMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Event;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.NormalizedTimeValue;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Relation;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.RelationMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Value;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.ValueMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLPolarity;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention.TimeMLTense;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.RelationMention.ACELexicalCondition;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression.TimeMLDocumentFunction;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression.TimeMLMod;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression.TimeMLType;

public class ConstructACE2005 {
	public static class ACESourceDocument {
		private String name;
		private ACEDocumentType type;
		private String dctStr;
		private Pair<Integer, Integer> dctStrCharRange;
		private TreeMap<Integer, Pair<String, String>> bodyParts; // Map start char index to (part name, part value)
		
		public ACESourceDocument(String name,
								ACEDocumentType type,
								String dctStr, 
								Pair<Integer, Integer> dctStrCharRange,
								TreeMap<Integer, Pair<String, String>> bodyParts) {
			this.name = name;
			this.type = type;
			this.dctStr = dctStr;
			this.dctStrCharRange = dctStrCharRange;
			this.bodyParts = bodyParts;
		}
		
		public String getName() {
			return this.name;
		}
		
		public ACEDocumentType getType() {
			return this.type;
		}
		
		public String getDCTStr() {
			return this.dctStr;
		}
		
		public boolean indexInDCTStr(int index) {
			return index >= this.dctStrCharRange.getFirst() && index < this.dctStrCharRange.getSecond();
		}
		
		public TreeMap<Integer, Pair<String, String>> getBodyParts() {
			return this.bodyParts;
		}
	}
	
	public static final String DOCUMENT_COLLECTION = "ace_docs";
	public static final String EVENT_MENTION_COLLECTION = "ace_ementions";
	public static final String EVENT_COLLECTION = "ace_events";
	public static final String ENTITY_MENTION_COLLECTION = "ace_enmentions";
	public static final String ENTITY_COLLECTION = "ace_entities";
	public static final String RELATION_MENTION_COLLECTION = "ace_rmentions";
	public static final String RELATION_COLLECTION = "ace_relations";
	public static final String VALUE_MENTION_COLLECTION = "ace_vmentions";
	public static final String VALUE_COLLECTION = "ace_values";
	public static final String TIME_EXPRESSION_COLLECTION = "ace_timexes";
	public static final String TIME_VALUE_COLLECTION = "ace_tvalues";
	
	private static String storageName;
	private static DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> storedDocuments;
	private static StoredItemSetInMemoryLazy<EventMention, EventMention> storedEventMentions;
	private static StoredItemSetInMemoryLazy<Event, Event> storedEvents;
	private static StoredItemSetInMemoryLazy<EntityMention, EntityMention> storedEntityMentions;
	private static StoredItemSetInMemoryLazy<Entity, Entity> storedEntities;
	private static StoredItemSetInMemoryLazy<RelationMention, RelationMention> storedRelationMentions;
	private static StoredItemSetInMemoryLazy<Relation, Relation> storedRelations;
	private static StoredItemSetInMemoryLazy<ValueMention, ValueMention> storedValueMentions;
	private static StoredItemSetInMemoryLazy<Value, Value> storedValues;
	private static StoredItemSetInMemoryLazy<TimeExpression, TimeExpression> storedTimeExpressions;
	private static StoredItemSetInMemoryLazy<NormalizedTimeValue, NormalizedTimeValue> storedTimeValues;
	
	private static EventDataTools dataTools;
	
	private static PipelineNLPStanford tokenPipeline;
	private static PipelineNLP nlpPipeline;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		storageName = args[0];
		Map<String, Pair<File, File>> inputFiles = getInputFiles(args);
		
		dataTools = new EventDataTools();
		tokenPipeline = new PipelineNLPStanford();
		tokenPipeline.initialize(AnnotationTypeNLP.POS);
		
		PipelineNLPStanford stanfordPipe = new PipelineNLPStanford();
		stanfordPipe.initialize(null, new BSONTokenizer());
		nlpPipeline = stanfordPipe.weld(new PipelineNLPMateTools(dataTools.getProperties()));
		
		Storage<?, ?> storage = dataTools.getStoredItemSetManager().getStorage(storageName);
		if (storage.hasCollection(DOCUMENT_COLLECTION))
			storage.deleteCollection(DOCUMENT_COLLECTION);
		if (storage.hasCollection(EVENT_MENTION_COLLECTION))
			storage.deleteCollection(EVENT_MENTION_COLLECTION);
		if (storage.hasCollection(EVENT_COLLECTION))
			storage.deleteCollection(EVENT_COLLECTION);
		if (storage.hasCollection(ENTITY_MENTION_COLLECTION))
			storage.deleteCollection(ENTITY_MENTION_COLLECTION);
		if (storage.hasCollection(ENTITY_COLLECTION))
			storage.deleteCollection(ENTITY_COLLECTION);
		if (storage.hasCollection(RELATION_MENTION_COLLECTION))
			storage.deleteCollection(RELATION_MENTION_COLLECTION);
		if (storage.hasCollection(RELATION_COLLECTION))
			storage.deleteCollection(RELATION_COLLECTION);
		if (storage.hasCollection(VALUE_MENTION_COLLECTION))
			storage.deleteCollection(VALUE_MENTION_COLLECTION);
		if (storage.hasCollection(VALUE_COLLECTION))
			storage.deleteCollection(VALUE_COLLECTION);
		if (storage.hasCollection(TIME_EXPRESSION_COLLECTION))
			storage.deleteCollection(TIME_EXPRESSION_COLLECTION);
		if (storage.hasCollection(TIME_VALUE_COLLECTION))
			storage.deleteCollection(TIME_VALUE_COLLECTION);
		
		Map<String, Serializer<?, ?>> serializers = dataTools.getSerializers();
		
		storedDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				dataTools.getStoredItemSetManager()
				.getItemSet(storageName, DOCUMENT_COLLECTION, true, (Serializer<DocumentNLPMutable, Document>)serializers.get("DocumentNLPBSON")));
		storedEventMentions = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, EVENT_MENTION_COLLECTION, true,(Serializer<EventMention, Document>)serializers.get("JSONBSONEventMention"));
		storedEvents = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, EVENT_COLLECTION, true,(Serializer<Event, Document>)serializers.get("JSONBSONEvent"));
		storedEntityMentions = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, ENTITY_MENTION_COLLECTION, true,(Serializer<EntityMention, Document>)serializers.get("JSONBSONEntityMention"));
		storedEntities = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, ENTITY_COLLECTION, true,(Serializer<Entity, Document>)serializers.get("JSONBSONEntity"));
		storedRelationMentions = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, RELATION_MENTION_COLLECTION, true,(Serializer<RelationMention, Document>)serializers.get("JSONBSONRelationMention"));
		storedRelations = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, RELATION_COLLECTION, true,(Serializer<Relation, Document>)serializers.get("JSONBSONRelation"));
		storedValueMentions = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, VALUE_MENTION_COLLECTION, true,(Serializer<ValueMention, Document>)serializers.get("JSONBSONValueMention"));
		storedValues = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, VALUE_COLLECTION, true,(Serializer<Value, Document>)serializers.get("JSONBSONValue"));
		storedTimeExpressions = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, TIME_EXPRESSION_COLLECTION, true,(Serializer<TimeExpression, Document>)serializers.get("JSONBSONTimeExpression"));	
		storedTimeValues = dataTools.getStoredItemSetManager()
				.getItemSet(storageName, TIME_VALUE_COLLECTION, true,(Serializer<NormalizedTimeValue, Document>)serializers.get("JSONBSONNormalizedTimeValue"));	
		
		Map<String, Set<String>> summary = new TreeMap<String, Set<String>>();
		for (Entry<String, Pair<File, File>> entry : inputFiles.entrySet()) {
			summarizeAnnotations(entry.getValue().getSecond(), summary);
			System.out.println("Processing files " + entry.getKey() + "...");
			if (!parseAndOutputDocuments(entry.getValue().getFirst(), entry.getValue().getSecond())) {
				throw new IllegalStateException("Failed to parse and output document " + entry.getKey());
			}
		}
		
		System.out.println("Annotation summary");
		for (Entry<String, Set<String>> entry : summary.entrySet()) {
			System.out.println(entry.getKey());
			if (entry.getValue().size() > 50) {
				System.out.println("\t(" + entry.getValue().size() + " values)");
			} else {
				for (String child : entry.getValue())
					System.out.println("\t" + child);
			}
		}
	}
	
	private static boolean parseAndOutputDocuments(File contentFile, File annotationFile) {		
		List<ACESourceDocument> docs = constructSourceDocuments(contentFile);
		Element annotationsRoot = getDocumentRootElementAndString(annotationFile).getFirst();
		TreeMap<Integer, List<Element>> charseqElements = getCharseqElements(annotationsRoot);
		Map<Element, TokenSpan> charseqSpans = new HashMap<Element, TokenSpan>();
		Map<String, DocumentNLPMutable> annotatedDocs = new HashMap<>();
		
		for (int i = 0; i < docs.size(); i++) {
			DocumentNLPMutable annotatedDoc = annotateDocument(docs.get(i), charseqElements, charseqSpans);
			annotatedDoc.setDocumentAnnotation("ace_2005", AnnotationTypeNLPEvent.ACE_DOCUMENT_TYPE, new Pair<ACEDocumentType, Double>(docs.get(i).getType(), null));
			annotatedDocs.put(annotatedDoc.getName(), annotatedDoc);
		}
		
		parseAndOutputEntities(annotationsRoot, annotatedDocs);
		parseAndOutputEntityMentions(annotationsRoot, annotatedDocs, charseqSpans); 
		parseAndOutputValues(annotationsRoot, annotatedDocs);
		parseAndOutputValueMentions(annotationsRoot, annotatedDocs, charseqSpans); 
		parseAndOutputEvents(annotationsRoot, annotatedDocs);
		parseAndOutputEventMentions(annotationsRoot, annotatedDocs, charseqSpans); 
		parseAndOutputRelations(annotationsRoot, annotatedDocs);
		parseAndOutputRelationMentions(annotationsRoot, annotatedDocs, charseqSpans); 
		parseAndOutputTimeValues(annotationsRoot, annotatedDocs);
		parseAndOutputTimeExpressions(annotationsRoot, annotatedDocs, charseqSpans, docs); 
	
		for (DocumentNLPMutable annotatedDoc : annotatedDocs.values())
			storedDocuments.addItem(annotatedDoc);
		
		return true;
	}
	
	private static boolean parseAndOutputEntities(Element annotationsRoot, Map<String, DocumentNLPMutable> docs) {
		List<Element> entityElements = annotationsRoot.getChild("document").getChildren("entity");
		for (Element entityElement : entityElements) {
			String id = entityElement.getAttributeValue("ID");
			StoreReference ref = new StoreReference(storageName, ENTITY_COLLECTION, "id", String.valueOf(id));
			Entity.ACEClass aceClass = Entity.ACEClass.valueOf(entityElement.getAttributeValue("CLASS").replace('-', '_'));
			Entity.ACEType aceType = Entity.ACEType.valueOf(entityElement.getAttributeValue("TYPE").replace('-', '_'));
			Entity.ACESubtype aceSubtype = Entity.ACESubtype.valueOf(entityElement.getAttributeValue("SUBTYPE").replace('-', '_'));
			
			String defaultName = null;
			Element attElement = entityElement.getChild("entity_attributes");
			if (attElement != null) {
				Element nameElement = attElement.getChild("name");
				if (nameElement != null)
					defaultName = nameElement.getAttributeValue("NAME");
				
			}
			
			Entity entity = new Entity(dataTools,
										ref,
										id,
										defaultName,
										aceClass,
										aceSubtype,
										aceType);
			
			storedEntities.addItem(entity);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputEntityMentions(Element annotationsRoot, Map<String, DocumentNLPMutable> docs, Map<Element, TokenSpan> seqSpans) {
		Map<String, List<Triple<TokenSpan, StoreReference, Double>>> annotations = new HashMap<>();
		List<Element> entityElements = annotationsRoot.getChild("document").getChildren("entity");

		for (Element entityElement : entityElements) {
			StoreReference entityRef = new StoreReference(storageName, ENTITY_COLLECTION, "id", String.valueOf(entityElement.getAttributeValue("ID")));
			List<Element> mentionElements = entityElement.getChildren("entity_mention");
			for (Element mentionElement : mentionElements) {
				String id = mentionElement.getAttributeValue("ID");
				StoreReference ref = new StoreReference(storageName, ENTITY_MENTION_COLLECTION, "id", String.valueOf(id));
				Boolean metonymy = mentionElement.getAttribute("METONYMY_MENTION") != null ? Boolean.valueOf(mentionElement.getAttributeValue("METONYMY_MENTION")) : null;
				EntityMention.ACEType aceType = EntityMention.ACEType.valueOf(mentionElement.getAttributeValue("TYPE").replace('-', '_'));
				EntityMention.ACERole aceRole = mentionElement.getAttributeValue("ROLE") == null ? null : EntityMention.ACERole.valueOf(mentionElement.getAttributeValue("ROLE").replace('-', '_'));
				TokenSpan tokenSpan = getSpan(mentionElement.getChild("extent").getChild("charseq"), seqSpans);
				TokenSpan head = mentionElement.getChild("head") != null ? getSpan(mentionElement.getChild("head").getChild("charseq"), seqSpans) : null;
				if (tokenSpan == null) {
					tokenSpan = head;
				}
				
				EntityMention mention = new EntityMention(dataTools,
								  ref,
								  id,
								  metonymy,
								  aceRole,
								  aceType,
								  tokenSpan,
								  head,
								  entityRef);
				
				storedEntityMentions.addItem(mention);
				
				if (!annotations.containsKey(tokenSpan.getDocument().getName()))
					annotations.put(tokenSpan.getDocument().getName(), new ArrayList<>());
				annotations.get(tokenSpan.getDocument().getName()).add(new Triple<TokenSpan, StoreReference, Double>(tokenSpan, mention.getStoreReference(), null));
			}
		}		
		
		for (Entry<String, List<Triple<TokenSpan, StoreReference, Double>>> entry : annotations.entrySet()) {
			PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
			pipeline.extend(new AnnotatorTokenSpan<StoreReference>() {
				public String getName() { return "ace_2005"; }
				public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.ENTITY_MENTION; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, StoreReference, Double>> annotate(DocumentNLP document) {
					return entry.getValue();
				}
			});
			
			DocumentNLPMutable doc = docs.get(entry.getKey());
			pipeline.run(doc);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputValues(Element annotationsRoot, Map<String, DocumentNLPMutable> docs) {
		List<Element> valueElements = annotationsRoot.getChild("document").getChildren("value");
		for (Element valueElement : valueElements) {
			String id = valueElement.getAttributeValue("ID");
			StoreReference ref = new StoreReference(storageName, VALUE_COLLECTION, "id", String.valueOf(id));
			Value.ACEType aceType = Value.ACEType.valueOf(valueElement.getAttributeValue("TYPE").replace('-', '_'));
			Value.ACESubtype aceSubtype = valueElement.getAttributeValue("SUBTYPE") == null ? null : Value.ACESubtype.valueOf(valueElement.getAttributeValue("SUBTYPE").replace('-', '_'));
			
			Value value = new Value(dataTools,
									ref,
									id,
									aceSubtype,
									aceType);
			
			storedValues.addItem(value);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputValueMentions(Element annotationsRoot, Map<String, DocumentNLPMutable> docs, Map<Element, TokenSpan> seqSpans) {
		Map<String, List<Triple<TokenSpan, StoreReference, Double>>> annotations = new HashMap<>();
		List<Element> valueElements = annotationsRoot.getChild("document").getChildren("value");

		for (Element valueElement : valueElements) {
			StoreReference valueRef = new StoreReference(storageName, VALUE_COLLECTION, "id", String.valueOf(valueElement.getAttributeValue("ID")));
			List<Element> mentionElements = valueElement.getChildren("value_mention");
			for (Element mentionElement : mentionElements) {
				String id = mentionElement.getAttributeValue("ID");
				StoreReference ref = new StoreReference(storageName, VALUE_MENTION_COLLECTION, "id", String.valueOf(id));
				TokenSpan tokenSpan = getSpan(mentionElement.getChild("extent").getChild("charseq"), seqSpans);
				
				
				ValueMention mention = new ValueMention(dataTools,
														ref,
														id,
														tokenSpan,
														valueRef);
				
				storedValueMentions.addItem(mention);
				
				if (!annotations.containsKey(tokenSpan.getDocument().getName()))
					annotations.put(tokenSpan.getDocument().getName(), new ArrayList<>());
				annotations.get(tokenSpan.getDocument().getName()).add(new Triple<TokenSpan, StoreReference, Double>(tokenSpan, mention.getStoreReference(), null));
			}
		}		
		
		for (Entry<String, List<Triple<TokenSpan, StoreReference, Double>>> entry : annotations.entrySet()) {
			PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
			pipeline.extend(new AnnotatorTokenSpan<StoreReference>() {
				public String getName() { return "ace_2005"; }
				public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.VALUE_MENTION; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, StoreReference, Double>> annotate(DocumentNLP document) {
					return entry.getValue();
				}
			});
			
			DocumentNLPMutable doc = docs.get(entry.getKey());
			pipeline.run(doc);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputEvents(Element annotationsRoot, Map<String, DocumentNLPMutable> docs) {
		List<Element> eventElements = annotationsRoot.getChild("document").getChildren("event");
		for (Element eventElement : eventElements) {
			String id = eventElement.getAttributeValue("ID");
			StoreReference ref = new StoreReference(storageName, EVENT_COLLECTION, "id", String.valueOf(id));
			Event.ACEType aceType = Event.ACEType.valueOf(eventElement.getAttributeValue("TYPE").replace('-', '_'));
			Event.ACESubtype aceSubtype = Event.ACESubtype.valueOf(eventElement.getAttributeValue("SUBTYPE").replace('-', '_'));
			Event.ACEGenericity aceGenericity = Event.ACEGenericity.valueOf(eventElement.getAttributeValue("GENERICITY").replace('-', '_'));
			
			List<Element> argElements = eventElement.getChildren("event_argument");
			List<Pair<StoreReference, String>> argRefs = new ArrayList<>();
			for (Element argElement : argElements) {
				String argId = argElement.getAttributeValue("REFID");
				String[] argIdParts = argId.split("\\-");
				if (argIdParts[argIdParts.length - 1].startsWith("E") && !argIdParts[argIdParts.length - 1].startsWith("EV")) {
					argRefs.add(new Pair<StoreReference, String>(
						new StoreReference(storageName, ENTITY_COLLECTION, "id", argId), 
						argElement.getAttributeValue("ROLE")
					));
				} else if (argIdParts[argIdParts.length - 1].startsWith("V")) {
					argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, VALUE_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
				} else if (argIdParts[argIdParts.length - 1].startsWith("T")) {
					argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
				} else {
					System.err.println("Invalid argument for " + id + ": " + argId);
					System.exit(1);
				}	
			}
			
			Event event = new Event(dataTools,
									ref, 
									id,
									aceType,
									aceGenericity,
									aceSubtype,
									argRefs);
			
			storedEvents.addItem(event);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputEventMentions(Element annotationsRoot, Map<String, DocumentNLPMutable> docs, Map<Element, TokenSpan> seqSpans) {
		Map<String, List<Triple<TokenSpan, StoreReference, Double>>> annotations = new HashMap<>();
		List<Element> eventElements = annotationsRoot.getChild("document").getChildren("event");

		for (Element eventElement : eventElements) {
			StoreReference eventRef = new StoreReference(storageName, EVENT_COLLECTION, "id", String.valueOf(eventElement.getAttributeValue("ID")));
			String modality = eventElement.getAttributeValue("MODALITY");
			TimeMLTense tense = getTimeMLTense(eventElement.getAttributeValue("TENSE"));
			TimeMLPolarity polarity = null;
			if (eventElement.getAttributeValue("POLARITY") != null) {
				if (eventElement.getAttributeValue("POLARITY").equals("Negative"))
					polarity = TimeMLPolarity.NEG;
				else if (eventElement.getAttributeValue("POLARITY").equals("Positive"))
					polarity = TimeMLPolarity.POS;
			}
			
			List<Element> mentionElements = eventElement.getChildren("event_mention");
			for (Element mentionElement : mentionElements) {
				String id = mentionElement.getAttributeValue("ID");
				StoreReference ref = new StoreReference(storageName, EVENT_MENTION_COLLECTION, "id", String.valueOf(id));
				TokenSpan anchor = getSpan(mentionElement.getChild("anchor").getChild("charseq"), seqSpans);
				TokenSpan extent = getSpan(mentionElement.getChild("extent").getChild("charseq"), seqSpans);
				
				List<Element> argElements = mentionElement.getChildren("event_mention_argument");
				List<Pair<StoreReference, String>> argRefs = new ArrayList<>();
				for (Element argElement : argElements) {
					String argId = argElement.getAttributeValue("REFID");
					String[] argIdParts = argId.split("\\-");
					if (argIdParts[argIdParts.length - 2].startsWith("E") && !argIdParts[argIdParts.length - 2].startsWith("EV")) {
						argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, ENTITY_MENTION_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
					} else if (argIdParts[argIdParts.length - 2].startsWith("V")) {
						argRefs.add(new Pair<StoreReference, String>(
								new StoreReference(storageName, VALUE_MENTION_COLLECTION, "id", argId), 
								argElement.getAttributeValue("ROLE")
							));
					} else if (argIdParts[argIdParts.length - 2].startsWith("T")) {
						argRefs.add(new Pair<StoreReference, String>(
								new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", argId), 
								argElement.getAttributeValue("ROLE")
							));
					} else {
						System.err.println("Invalid argument for " + id + ": " + argId);
						System.exit(1);
					}	
				}
				
				EventMention mention = 	new EventMention(dataTools,
						ref, 
						id, 
						null, 
						null,
						anchor,
						null,
						tense, 
						null, 
						null, 
						polarity, 
						null, 
						null, 
						null,
						modality,
						null,
						extent,
						eventRef,
						argRefs);
				
				storedEventMentions.addItem(mention);
				
				if (!annotations.containsKey(anchor.getDocument().getName()))
					annotations.put(anchor.getDocument().getName(), new ArrayList<>());
				annotations.get(anchor.getDocument().getName()).add(new Triple<TokenSpan, StoreReference, Double>(anchor, mention.getStoreReference(), null));
			}
		}		
		
		for (Entry<String, List<Triple<TokenSpan, StoreReference, Double>>> entry : annotations.entrySet()) {
			PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
			pipeline.extend(new AnnotatorTokenSpan<StoreReference>() {
				public String getName() { return "ace_2005"; }
				public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.EVENT_MENTION; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, StoreReference, Double>> annotate(DocumentNLP document) {
					return entry.getValue();
				}
			});
			
			DocumentNLPMutable doc = docs.get(entry.getKey());
			pipeline.run(doc);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputRelations(Element annotationsRoot, Map<String, DocumentNLPMutable> docs) {
		List<Element> relationElements = annotationsRoot.getChild("document").getChildren("relation");
		for (Element relationElement : relationElements) {
			String id = relationElement.getAttributeValue("ID");
			StoreReference ref = new StoreReference(storageName, RELATION_COLLECTION, "id", String.valueOf(id));
			
			Relation.ACEType aceType = Relation.ACEType.valueOf(relationElement.getAttributeValue("TYPE").replace('-', '_'));
			Relation.ACESubtype aceSubtype = Relation.ACESubtype.valueOf(relationElement.getAttributeValue("SUBTYPE").replace('-', '_'));
			String modality = relationElement.getAttributeValue("MODALITY");
			
			List<Element> argElements = relationElement.getChildren("relation_argument");
			List<Pair<StoreReference, String>> argRefs = new ArrayList<>();
			for (Element argElement : argElements) {
				String argId = argElement.getAttributeValue("REFID");
				String[] argIdParts = argId.split("\\-");
				if (argIdParts[argIdParts.length - 1].startsWith("E") && !argIdParts[argIdParts.length - 1].startsWith("EV")) {
					argRefs.add(new Pair<StoreReference, String>(
						new StoreReference(storageName, ENTITY_COLLECTION, "id", argId), 
						argElement.getAttributeValue("ROLE")
					));
				} else if (argIdParts[argIdParts.length - 1].startsWith("V")) {
					argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, VALUE_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
				} else if (argIdParts[argIdParts.length - 1].startsWith("T")) {
					argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
				} else {
					System.err.println("Invalid argument for " + id + ": " + argId);
					System.exit(1);
				}	
			}
			
			Relation relation = new Relation(dataTools,
											ref, 
											id, 
											aceType,
											aceSubtype,
											modality,
											argRefs);
			
			storedRelations.addItem(relation);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputRelationMentions(Element annotationsRoot, Map<String, DocumentNLPMutable> docs, Map<Element, TokenSpan> seqSpans) {
		Map<String, List<Triple<TokenSpan, StoreReference, Double>>> annotations = new HashMap<>();
		List<Element> relationElements = annotationsRoot.getChild("document").getChildren("relation");

		for (Element relationElement : relationElements) {
			StoreReference relationRef = new StoreReference(storageName, RELATION_COLLECTION, "id", String.valueOf(relationElement.getAttributeValue("ID")));
			TimeMLTense tense = getTimeMLTense(relationElement.getAttributeValue("TENSE"));
	
			List<Element> mentionElements = relationElement.getChildren("relation_mention");
			for (Element mentionElement : mentionElements) {
				String id = mentionElement.getAttributeValue("ID");
				StoreReference ref = new StoreReference(storageName, RELATION_MENTION_COLLECTION, "id", String.valueOf(id));
				TokenSpan extent = getSpan(mentionElement.getChild("extent").getChild("charseq"), seqSpans);
				ACELexicalCondition aceLexicalCondition = mentionElement.getAttributeValue("LEXICALCONDITION") == null ? null : ACELexicalCondition.valueOf(mentionElement.getAttributeValue("LEXICALCONDITION"));
				
				List<Element> argElements = mentionElement.getChildren("relation_mention_argument");
				List<Pair<StoreReference, String>> argRefs = new ArrayList<>();
				for (Element argElement : argElements) {
					String argId = argElement.getAttributeValue("REFID");
					String[] argIdParts = argId.split("\\-");
					if (argIdParts[argIdParts.length - 2].startsWith("E") && !argIdParts[argIdParts.length - 2].startsWith("EV")) {
						argRefs.add(new Pair<StoreReference, String>(
							new StoreReference(storageName, ENTITY_MENTION_COLLECTION, "id", argId), 
							argElement.getAttributeValue("ROLE")
						));
					} else if (argIdParts[argIdParts.length - 2].startsWith("V")) {
						argRefs.add(new Pair<StoreReference, String>(
								new StoreReference(storageName, VALUE_MENTION_COLLECTION, "id", argId), 
								argElement.getAttributeValue("ROLE")
							));
					} else if (argIdParts[argIdParts.length - 2].startsWith("T")) {
						argRefs.add(new Pair<StoreReference, String>(
								new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", argId), 
								argElement.getAttributeValue("ROLE")
							));
					} else {
						System.err.println("Invalid argument for " + id + ": " + argId);
						System.exit(1);
					}	
				}
				
				RelationMention mention = new RelationMention(dataTools,
						ref, 
						id, 
						extent,
						tense, 
						aceLexicalCondition,
						relationRef,
						argRefs);
				
				storedRelationMentions.addItem(mention);
				
				if (!annotations.containsKey(extent.getDocument().getName()))
					annotations.put(extent.getDocument().getName(), new ArrayList<>());
				annotations.get(extent.getDocument().getName()).add(new Triple<TokenSpan, StoreReference, Double>(extent, mention.getStoreReference(), null));
			}
		}
		
		for (Entry<String, List<Triple<TokenSpan, StoreReference, Double>>> entry : annotations.entrySet()) {
			PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
			pipeline.extend(new AnnotatorTokenSpan<StoreReference>() {
				public String getName() { return "ace_2005"; }
				public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.RELATION_MENTION; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, StoreReference, Double>> annotate(DocumentNLP document) {
					return entry.getValue();
				}
			});
			
			DocumentNLPMutable doc = docs.get(entry.getKey());
			pipeline.run(doc);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputTimeValues(Element annotationsRoot, Map<String, DocumentNLPMutable> docs) {
		List<Element> timexElements = annotationsRoot.getChild("document").getChildren("timex2");
		for (Element timexElement : timexElements) {
			String id = timexElement.getAttributeValue("ID");
			StoreReference ref = new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", String.valueOf(id));
			String value = timexElement.getAttributeValue("VAL");
			
			NormalizedTimeValue tvalue = new NormalizedTimeValue(dataTools, ref, id, value);
			storedTimeValues.addItem(tvalue);
		}
		
		return true;
	}
	
	private static boolean parseAndOutputTimeExpressions(Element annotationsRoot, Map<String, DocumentNLPMutable> docs, Map<Element, TokenSpan> seqSpans, List<ACESourceDocument> sourceDocs) {		
		Map<String, List<Triple<TokenSpan, StoreReference, Double>>> annotations = new HashMap<>();
		List<Element> timexElements = annotationsRoot.getChild("document").getChildren("timex2");

		for (Element timexElement : timexElements) {
			StoreReference valueRef = new StoreReference(storageName, TIME_VALUE_COLLECTION, "id", String.valueOf(timexElement.getAttributeValue("ID")));
			TimeMLMod mod = timexElement.getAttributeValue("MOD") != null ? TimeMLMod.valueOf(timexElement.getAttributeValue("MOD")) : null;
			
			List<Element> mentionElements = timexElement.getChildren("timex2_mention");
			for (Element mentionElement : mentionElements) {
				String id = mentionElement.getAttributeValue("ID");
				StoreReference ref = new StoreReference(storageName, TIME_EXPRESSION_COLLECTION, "id", String.valueOf(id));
				TokenSpan tokenSpan = getSpan(mentionElement.getChild("extent").getChild("charseq"), seqSpans);
				TimeMLDocumentFunction fn = TimeMLDocumentFunction.NONE;
				if (tokenSpan == null) {
					for (ACESourceDocument sourceDoc : sourceDocs) {
						if (sourceDoc.indexInDCTStr(
							Integer.valueOf(mentionElement.getChild("extent").getChild("charseq").getAttributeValue("START"))
							)
						) {
							tokenSpan = new TokenSpan(docs.get(sourceDoc.getName()), -1, -1, -1);
							break;
						}
					}
					
					fn = TimeMLDocumentFunction.CREATION_TIME;
					
					if (tokenSpan == null) {
						System.err.println("ERROR: Failed to find document for creation time " + id);
						System.exit(1);
					} else {
						PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
						pipeline.extend(new AnnotatorDocument<StoreReference>() {
							public String getName() { return "ace_2005"; }
							public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.CREATION_TIME; };
							public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
							public boolean measuresConfidence() { return false; }
							public Pair<StoreReference, Double> annotate(DocumentNLP document) {
								return new Pair<StoreReference, Double>(ref, null);
							}
						});
						
						DocumentNLPMutable doc = (DocumentNLPMutable)tokenSpan.getDocument();
						pipeline.run(doc);
					}
				} 
				
				TimeExpression mention = new TimeExpression(dataTools, 
															  ref,
															  tokenSpan,
															  id,
															  null,
															  TimeMLType.TIME,
															  null,
															  null,
															  null,
															  null,
															  valueRef,
															  fn,
															  false,
															  null,
															  null,
															  mod);
				
				storedTimeExpressions.addItem(mention);
				
				if (fn != TimeMLDocumentFunction.CREATION_TIME) {
					if (!annotations.containsKey(tokenSpan.getDocument().getName()))
						annotations.put(tokenSpan.getDocument().getName(), new ArrayList<>());
					annotations.get(tokenSpan.getDocument().getName()).add(new Triple<TokenSpan, StoreReference, Double>(tokenSpan, mention.getStoreReference(), null));
				}
			}
		}		
		
		for (Entry<String, List<Triple<TokenSpan, StoreReference, Double>>> entry : annotations.entrySet()) {
			PipelineNLPExtendable pipeline = new PipelineNLPExtendable();
			pipeline.extend(new AnnotatorTokenSpan<StoreReference>() {
				public String getName() { return "ace_2005"; }
				public AnnotationType<StoreReference> produces() { return AnnotationTypeNLPEvent.TIME_EXPRESSION; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, StoreReference, Double>> annotate(DocumentNLP document) {
					return entry.getValue();
				}
			});
			
			DocumentNLPMutable doc = docs.get(entry.getKey());
			pipeline.run(doc);
		}
		
		return true;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static DocumentNLPMutable annotateDocument(ACESourceDocument sourceDocument, TreeMap<Integer, List<Element>> charseqElements, Map<Element, TokenSpan> charseqSpans) {
		DocumentNLPMutable doc = new DocumentNLPInMemory(dataTools, sourceDocument.getName(), "");
		
		TreeMap<Integer, Pair<String, String>> bodyParts = sourceDocument.getBodyParts();
		
		List<Pair[]> tokens = new ArrayList<Pair[]>();
		StringBuilder modifiedText = new StringBuilder();
		for (Entry<Integer, Pair<String, String>> entry : bodyParts.entrySet()) {
			String text = entry.getValue().getSecond();
			if (text.trim().length() == 0)
				continue;
			DocumentNLPMutable partialDoc = new DocumentNLPInMemory(dataTools, sourceDocument.getName(), text);
			partialDoc = tokenPipeline.run(partialDoc);
			for (int i = 0; i < partialDoc.getSentenceCount(); i++) {
				Pair[] sentenceTokens = new Pair[partialDoc.getSentenceTokenCount(i)];
				for (int j = 0; j < partialDoc.getSentenceTokenCount(i); j++) {
					Token tempToken = partialDoc.getToken(i, j);
					String tokenStr = tempToken.getStr();
					
					int tempStartIndex = tempToken.getCharSpanStart() + entry.getKey();
					int tempEndIndex = tempToken.getCharSpanEnd() + entry.getKey();
					
					for (Entry<Integer, List<Element>> charseqEntry : charseqElements.subMap(tempStartIndex, tempEndIndex).entrySet()) {
						List<Element> curCharseqs = charseqEntry.getValue();
						List<TokenSpan> spans = getTokenSpansForCharseqs(entry.getKey(), partialDoc, i, j, curCharseqs, doc, tokens.size());
						for (int k = 0; k < curCharseqs.size(); k++)
							if (spans.get(k) != null)
								charseqSpans.put(curCharseqs.get(k), spans.get(k));
					}
					
					sentenceTokens[j] = new Pair<Token, Double>(
						new Token(doc, tokenStr, modifiedText.length(), modifiedText.length() + tokenStr.length()),
						null);
					
					modifiedText.append(tokenStr).append(" ");
				}
				
				tokens.add(sentenceTokens);
			}
		}
		
		Pair<Token, Double>[][] tokenArr = new Pair[tokens.size()][];
		for (int i = 0; i < tokens.size(); i++)
			tokenArr[i] = tokens.get(i);
		
		doc.setTokenAnnotation(tokenPipeline.getAnnotatorName(AnnotationTypeNLP.TOKEN), 
							   AnnotationTypeNLP.TOKEN,
							   tokenArr);
		
		SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(doc);
		doc.setDocumentAnnotation("ace_2005", AnnotationTypeNLP.ORIGINAL_TEXT, new Pair<String, Double>(serializer.serializeToString(doc), null));
		doc = nlpPipeline.run(doc);
		doc.setDocumentAnnotation("ace_2005", AnnotationTypeNLP.ORIGINAL_TEXT, new Pair<String, Double>(modifiedText.toString(), null));
		
		return doc;
	}
	
	private static List<TokenSpan> getTokenSpansForCharseqs(int offset, DocumentNLPMutable partialDocument, int partialSentenceIndex, int startTokenIndex, List<Element> seqs, DocumentNLPMutable spanDocument, int sentenceIndex) {
		List<TokenSpan> spans = new ArrayList<TokenSpan>();
		
		for (Element seq : seqs) {
			boolean foundSpan = false;
			for (int i = startTokenIndex; i < partialDocument.getSentenceTokenCount(partialSentenceIndex); i++) {
				try {
					if (partialDocument.getToken(partialSentenceIndex, i).getCharSpanEnd() + offset - 1 >= seq.getAttribute("END").getIntValue() && partialDocument.getToken(partialSentenceIndex, i).getCharSpanStart() + offset <= seq.getAttribute("END").getIntValue()) {
						//String[] seqParts = seq.getText().split("\\s+");
						//if (partialDocument.getTokenStr(partialSentenceIndex, i).contains(seqParts[seqParts.length - 1])
						//		|| seqParts[seqParts.length - 1].contains(partialDocument.getTokenStr(partialSentenceIndex, i)))
							foundSpan = true;
						//else
						//	foundSpan = false;
						spans.add(new TokenSpan(spanDocument, sentenceIndex, startTokenIndex, i + 1));
						break;
					}
				} catch (DataConversionException e) {
					System.err.println("Bad char span.  Expecting integer type.");
					System.exit(1);
				}
			}
			
			if (!foundSpan) {
				System.err.println("WARNING: Failed to match charseq to token span in " + spanDocument.getName());
				System.err.println("Seq: " + seq.getText() + " (" + seq.getAttributeValue("START") + " " + seq.getAttributeValue("END") + ")");
				System.err.println("Sentence: " + partialDocument.getSentence(partialSentenceIndex));
				for (int i = startTokenIndex; i < partialDocument.getSentenceTokenCount(partialSentenceIndex); i++) {
					System.err.println(partialDocument.getTokenStr(partialSentenceIndex, i) + " " + (partialDocument.getToken(partialSentenceIndex, i).getCharSpanStart() + offset) + " " + (partialDocument.getToken(partialSentenceIndex, i).getCharSpanEnd() + offset));
				}
				System.err.println("");
				spans.add(null);
			}
		}
		
		return spans;
	}
	
	private static TokenSpan getSpan(Element charseq, Map<Element, TokenSpan> charseqSpans) {
		if (!charseqSpans.containsKey(charseq)) {
			System.out.println("WARNING: Failed to find charseq " + charseq.getText() + " " + charseq.getAttributeValue("START") + " " + charseq.getAttributeValue("END") + " " + charseqSpans.size());
		}
		
		return charseqSpans.get(charseq);
	}
	
	private static TreeMap<Integer, List<Element>> getCharseqElements(Element annotationsRoot) {
		TreeMap<Integer, List<Element>> seqs = new TreeMap<>();
		List<Element> elements = traverse(annotationsRoot);
		for (Element element : elements) {
			if (element.getName().equals("charseq")) {
				int startIndex = Integer.valueOf(element.getAttribute("START").getValue());
				if (!seqs.containsKey(startIndex))
					seqs.put(startIndex, new ArrayList<>());
				seqs.get(startIndex).add(element);
			}
		}
		return seqs;
	}
	
	private static List<ACESourceDocument> constructSourceDocuments(File contentFile) {
		Pair<Element, String> rootAndString = getDocumentRootElementAndString(contentFile);
		Element rootElement = rootAndString.getFirst();
		List<Element> elementsInOrder = traverse(rootElement);
		Map<Element, Integer> charOffsets = getCharOffsets(rootAndString.getSecond(), elementsInOrder);
		List<ACESourceDocument> retDocs = new ArrayList<ACESourceDocument>();
		if (isNewswire(rootElement))
			retDocs.add(getSingleSourceDocument(ACEDocumentType.NEWS_WIRE, rootElement, charOffsets));
		else if (isBroadcastConversation(rootElement))
			retDocs.add(getSingleSourceDocument(ACEDocumentType.BROADCAST_CONVERSATION, rootElement, charOffsets));
		else if (isBroadcastNews(rootElement))
			retDocs.add(getSingleSourceDocument(ACEDocumentType.BROADCAST_NEWS, rootElement, charOffsets));
		else if (isTelephone(rootElement))
			retDocs.add(getSingleSourceDocument(ACEDocumentType.TELEPHONE, rootElement, charOffsets));
		else if (isUsenet(rootElement))
			retDocs.addAll(getPostSourceDocuments(ACEDocumentType.USENET, rootElement, charOffsets));
		else if (isWeblog(rootElement))
			retDocs.addAll(getPostSourceDocuments(ACEDocumentType.WEBLOG, rootElement, charOffsets));
		else {
			System.err.println("Unrecognized document source type " + contentFile.getAbsolutePath());
			System.exit(1);
		}
		
		return retDocs;
	}
	
	private static boolean isNewswire(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("newswire");
	}
	
	private static boolean isBroadcastConversation(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("broadcast conversation");
	}
	
	private static boolean isBroadcastNews(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("broadcast news");
	}
	
	private static boolean isTelephone(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("telephone");
	}
	
	private static boolean isUsenet(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("usenet");
	}
	
	private static boolean isWeblog(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("weblog");
	}
	
	private static ACESourceDocument getSingleSourceDocument(ACEDocumentType type, Element root, Map<Element, Integer> charOffsets) {
		String name = root.getChildText("DOCID").trim();
		String dctStr = root.getChildText("DATETIME");
		int dctStrOffset = charOffsets.get(root.getChild("DATETIME"));
		Pair<Integer, Integer> dctStrCharRange = new Pair<>(dctStrOffset, dctStrOffset + dctStr.length());
		
		TreeMap<Integer, Pair<String, String>> bodyParts = new TreeMap<>();
		getElementParts(root.getChild("BODY"), charOffsets.get(root.getChild("BODY")), bodyParts, "", false);
		
		ACESourceDocument document = new ACESourceDocument(
				name,
				type,
				dctStr, 
				dctStrCharRange,
				bodyParts);
		
		return document;
	}
	
	
	
	private static List<ACESourceDocument> getPostSourceDocuments(ACEDocumentType type, Element root, Map<Element, Integer> charOffsets) {
		List<ACESourceDocument> retDocuments = new ArrayList<>();
		
		String name = root.getChildText("DOCID").trim();
		String dctStr = root.getChildText("DATETIME");
		int dctStrOffset = charOffsets.get(root.getChild("DATETIME"));
		Pair<Integer, Integer> dctStrCharRange = new Pair<>(dctStrOffset, dctStrOffset + dctStr.length());

		TreeMap<Integer, Pair<String, String>> bodyParts = new TreeMap<>();
		getElementParts(root.getChild("BODY"), charOffsets.get(root.getChild("BODY")), bodyParts, "POST", false);
		
		retDocuments.add(new ACESourceDocument(
				name,
				type,
				dctStr, 
				dctStrCharRange,
				bodyParts));
		
		List<Element> postRoots = root.getChild("BODY").getChildren("POST").size() == 0 ? root.getChild("BODY").getChild("TEXT").getChildren("POST") : root.getChild("BODY").getChildren("POST");
		int postIndex = 0;
		for (Element postRoot : postRoots) {
			String postName = name + "_p" + postIndex;
			String postDctStr = postRoot.getChildText("POSTDATE");
			int postDctStrOffset = charOffsets.get(postRoot.getChild("POSTDATE"));
			Pair<Integer, Integer> postDctStrCharRange = new Pair<>(postDctStrOffset, postDctStrOffset + postDctStr.length());
			TreeMap<Integer, Pair<String, String>> postParts = new TreeMap<>();
			getElementParts(postRoot, charOffsets.get(postRoot), postParts, "POSTDATE", false);
			
			retDocuments.add(new ACESourceDocument(
					postName,
					type,
					postDctStr, 
					postDctStrCharRange,
					postParts));
			
			postIndex++;
		}
		
		return retDocuments;
	}
	
	private static Pair<Element, String> getDocumentRootElementAndString(File xmlFile) {
		BufferedReader r = FileUtil.getFileReader(xmlFile.getAbsolutePath());
		StringBuilder xmlStr = new StringBuilder();
		StringBuilder fullStr = new StringBuilder();
		try {
			String line = null;
			while ((line = r.readLine()) != null) {
				line = line.replace('&', '^'); // FIXME Hack
				if (!line.startsWith("<!DOCTYPE"))
					xmlStr.append(line).append("\n");
				fullStr.append(line).append("\n");
			}
			
			r.close();
		} catch (IOException e1) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			System.exit(1);
		}
		
		SAXBuilder builder = new SAXBuilder();
		Document xml = null;
		try {
			xml = builder.build(new StringReader(xmlStr.toString()));
		} catch (JDOMException e) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			e.printStackTrace();
			System.exit(1);
		}
		
		return new Pair<Element, String>(xml.getRootElement(), fullStr.toString());
	}
	
	private static Map<String, Pair<File, File>> getInputFiles(String[] args) {		
		Map<String, Pair<File, File>> files = new HashMap<>();
		for (int i = 1; i < args.length; i++) {
			String inputDirPath = args[i];
			File inputDir = new File(inputDirPath);
			File[] inputDirFiles = inputDir.listFiles();
			for (File file : inputDirFiles) {
				String fileName = file.getName();
				if (fileName.endsWith(".apf.xml")) {
					String name = fileName.substring(0, fileName.length() - ".apf.xml".length());
					if (!files.containsKey(name))
						files.put(name, new Pair<File, File>(null, file));
					else
						files.get(name).setSecond(file);
				} else if (fileName.endsWith(".sgm")) {
					String name = fileName.substring(0, fileName.length() - ".sgm".length());
					if (!files.containsKey(name))
						files.put(name, new Pair<File, File>(file, null));
					else
						files.get(name).setFirst(file);
				}
			}
		}
		
		return files;
	}
	
	private static Map<String, Set<String>> summarizeAnnotations(File annotationFile, Map<String, Set<String>> summary) {
		Pair<Element, String> rootAndStr = getDocumentRootElementAndString(annotationFile);
		Element rootElement = rootAndStr.getFirst();
		
		Stack<Pair<String, Object>> toVisit = new Stack<>();
		toVisit.add(new Pair<String, Object>("", rootElement));
		
		while (!toVisit.isEmpty()) {
			Pair<String, Object> curPair = toVisit.pop();
			String parent = curPair.getFirst();
			Object o = curPair.getSecond();
			if (o instanceof Element) {
				Element element = (Element)o;
				String name = parent + " " + element.getName() + " (element)";
				if (!summary.containsKey(name))
					summary.put(name, new HashSet<String>());
				Set<String> childNames = summary.get(name);
				
				List<Attribute> attributes = element.getAttributes();
				for (Attribute attribute : attributes) {
					childNames.add(attribute.getName() + " (attribute)");
					toVisit.add(new Pair<String, Object>(element.getName(), attribute));
				}
				
				List<Element> children = element.getChildren();
				for (Element child : children) {
					childNames.add(child.getName() + " (element)");					
					toVisit.add(new Pair<String, Object>(element.getName(), child));
				}
				
			} else {
				Attribute attribute = (Attribute)o;
				String name = parent + " " + attribute.getName() + " (attribute)";
				if (!summary.containsKey(name))
					summary.put(name, new HashSet<String>());
				summary.get(name).add(attribute.getValue() + " (value)");
			}
		}
		
		return summary;
	}
	
	private static List<Element> traverse(Element root) {
		Stack<Element> toVisit = new Stack<Element>();
		toVisit.push(root);
		List<Element> traversal = new ArrayList<Element>();
		while (!toVisit.isEmpty()) {
			Element current = toVisit.pop();
			traversal.add(current);
			
			List<Element> children = current.getChildren();
			List<Element> reverseChildren = new ArrayList<Element>();
			for (int i = 0; i < children.size(); i++)
				reverseChildren.add(children.get(children.size() - 1 - i));
			for (Element child : reverseChildren) {
				toVisit.push(child);
			}
		}
		
		return traversal;
	}
	
	private static Map<Element, Integer> getCharOffsets(String docString, List<Element> elements) {
		Map<Element, Integer> charOffsets = new HashMap<Element, Integer>();
		char[] chars = docString.toCharArray();
		boolean inStartTag = false;
		boolean inEndTag = false;
		boolean inQuote = false;
		int curOffset = 0;
		char prevChar = 0;
		int tagIndex = -1;
		for (int i = 0; i < chars.length; i++) {
			if (inStartTag) {
				if (chars[i] == '"') {
					inStartTag = false;
					inQuote = true;
				} else if (chars[i] == '>') {
					inStartTag = false;
					charOffsets.put(elements.get(tagIndex), curOffset);
				}
			} else if (inEndTag) {
				if (chars[i] =='>')
					inEndTag = false;
			} else if (inQuote) {
				if (chars[i] == '"') {
					inQuote = false;
					inStartTag = true;
				}
			} else {
				if (chars[i] == '<' && prevChar != '\\' && i + 1 != chars.length && !Character.isWhitespace(chars[i + 1])) {
					if (chars[i + 1] == '/')
						inEndTag = true;
					else {
						inStartTag = true;
						tagIndex++;
					}
				} else {
					curOffset++;
				}
			}
			
			prevChar = chars[i];
		}
		
		return charOffsets;
	}
	
	private static int getElementParts(Element element, int offset, TreeMap<Integer, Pair<String, String>> parts, String except, boolean inExcept) {
		List<Content> content = element.getContent();
		for (int i = 0; i < content.size(); i++) {
			if (content.get(i) instanceof Text) {
				Text text = (Text)content.get(i);
				if (!inExcept)
					parts.put(offset, new Pair<String, String>(element.getName(), text.getText()));
				offset += text.getText().length();
			} else if (content.get(i) instanceof Element) {
				offset = getElementParts((Element)content.get(i), offset, parts, except, (inExcept || element.getName().equals(except)));
			}
		}
		
		return offset;
	}
	
	private static TimeMLTense getTimeMLTense(String aceTense) {
		if (aceTense == null)
			return null;
		aceTense = aceTense.toUpperCase();
		if (aceTense.equals("UNSPECIFIED"))
			return TimeMLTense.NONE;
		else
			return TimeMLTense.valueOf(aceTense);
	}
}

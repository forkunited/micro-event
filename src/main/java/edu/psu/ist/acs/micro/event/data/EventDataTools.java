package edu.psu.ist.acs.micro.event.data;

import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.SerializerJSONBSON;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Signal;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkDatum;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TimeExpression;
import edu.psu.ist.acs.micro.event.util.EventProperties;

/**
 * EventDataTools contains definitions of cleaning 
 * functions, gazetteers, and other tools used by
 * features in the event models.
 * 
 * @author Bill McDowell
 *
 */
public class EventDataTools extends DataTools {
	public EventDataTools() {
		this(new EventProperties());
		
	}
	
	public EventDataTools(EventProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public EventDataTools(OutputWriter outputWriter, EventDataTools dataTools) {
		this(outputWriter, (EventProperties)dataTools.properties);
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	public EventDataTools(OutputWriter outputWriter, EventProperties properties) {
		super(outputWriter, properties);
		
		this.properties = properties;
		
		this.addGenericContext(new DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType>(TLinkDatum.getTimeMLRelTypeTools(this), "TLinkType"));
		this.addGenericContext(new DatumContext<TLinkDatum<Boolean>, Boolean>(TLinkDatum.getBooleanTools(this), "TLinkBoolean"));
	
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.CREATION_TIME);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.TIME_EXPRESSION);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.EVENT_MENTION);
		this.addAnnotationTypeNLP(AnnotationTypeNLPEvent.ACE_DOCUMENT_TYPE);
		
		for (TimeMLRelType relType : TimeMLRelType.values())
			this.addGenericWeightedStructure(new WeightedStructureRelationBinary(relType.toString()));
		this.addGenericWeightedStructure(new WeightedStructureRelationUnary("O"));
	}
	
	@Override
	public DataTools makeInstance() {
		return new EventDataTools(this.outputWriter, this);
	}
	
	@Override
	public Map<String, Serializer<?, ?>> getSerializers() {
		Map<String, Serializer<?, ?>> serializers = super.getSerializers();
		
		SerializerJSONBSON<EventMention> eventMentionSerializer = new SerializerJSONBSON<EventMention>("EventMention", new EventMention(this));
		SerializerJSONBSON<Signal> signalSerializer = new SerializerJSONBSON<Signal>("Signal", new Signal(this));
		SerializerJSONBSON<TimeExpression> timeExpressionSerializer = new SerializerJSONBSON<TimeExpression>("TimeExpression", new TimeExpression(this));
		SerializerJSONBSON<TLink> tlinkSerializer = new SerializerJSONBSON<TLink>("TLink", new TLink(this));
		
		serializers.put(eventMentionSerializer.getName(), eventMentionSerializer);
		serializers.put(signalSerializer.getName(), signalSerializer);
		serializers.put(timeExpressionSerializer.getName(), timeExpressionSerializer);
		serializers.put(tlinkSerializer.getName(), tlinkSerializer);
		
		return serializers;
	}
}

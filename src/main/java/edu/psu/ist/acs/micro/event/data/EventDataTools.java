package edu.psu.ist.acs.micro.event.data;

import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.SerializerJSONBSON;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.Signal;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink;
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
	private EventProperties properties;
	
	public EventDataTools() {
		this(new EventProperties());
		
	}
	
	public EventDataTools(EventProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public EventDataTools(OutputWriter outputWriter, EventDataTools dataTools) {
		this(outputWriter, dataTools.properties);
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	public EventDataTools(OutputWriter outputWriter, EventProperties properties) {
		super(outputWriter);
		
		this.properties = properties;
		
		// FIXME Make clean fns for this project?
		DataTools catDataTools = new CatDataTools();
		this.addCleanFn(catDataTools.getCleanFn("CatBagOfWordsFeatureCleanFn"));
	}
	
	public EventProperties getProperties() {
		return this.properties;
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

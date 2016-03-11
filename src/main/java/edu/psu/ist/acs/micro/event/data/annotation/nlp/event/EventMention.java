package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.SerializationType;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;


/**
 * Time represents a TimeML Event instance.  In TimeBank,
 * this is represented by an EVENT and a MAKEINSTANCE for
 * that event (with an eid and an eiid identifier)
 * 
 * See http://timeml.org/site/index.html for details.
 * 
 * @author Bill McDowell
 * 
 */
public class EventMention implements TLinkable {	
	public enum TimeMLTense {
		FUTURE,
		INFINITIVE,
		PAST,
		PASTPART,
		PRESENT,
		PRESPART,
		NONE
	}
	
	public enum TimeMLAspect {
		PROGRESSIVE,
		
		IMPERFECTIVE, // Spanish only
		IMPERFECTIVE_PROGRESSIVE, // Spanish only
		
		PERFECTIVE,
		PERFECTIVE_PROGRESSIVE,
		NONE
	}
	
	public enum TimeMLPolarity {
		POS,
		NEG
	}
	
	public enum TimeMLClass {
		OCCURRENCE,
		PERCEPTION,
		REPORTING,
		ASPECTUAL,
        STATE,
        I_STATE, 
        I_ACTION,
        NONE
	}
	
	public enum TimeMLPoS {
		ADJECTIVE,
		NOUN,
		VERB,
		PREPOSITION,
		OTHER
	}
	
	public enum TimeMLMood {
		INDICATIVE,
		SUBJUNCTIVE,
		CONDITIONAL,
		IMPERATIVE,
		NONE
	}
	
	public enum TimeMLVerbForm {
		INFINITIVE,
		GERUNDIVE,
		PARTICIPLE,
		NONE
	}

	private String id;
	private TokenSpan tokenSpan;
	private String sourceId;
	private String sourceInstanceId;
	private Signal signal;
	private TimeMLTense timeMLTense;
	private TimeMLAspect timeMLAspect;
	private TimeMLPolarity timeMLPolarity = TimeMLPolarity.POS;
	private TimeMLClass timeMLClass;
	private TimeMLPoS timeMLPoS;
	private TimeMLMood timeMLMood;
	private TimeMLVerbForm timeMLVerbForm;
	private String modality;
	private String cardinality;
	
	private StoreReference reference;
	private DataTools dataTools;
	
	public EventMention(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public EventMention(DataTools dataTools, StoreReference reference) {
		this.dataTools = dataTools;
		this.reference = reference;
	}
	
	public EventMention(DataTools dataTools,
						StoreReference reference, 
						String id, 
						String sourceId, 
						String sourceInstanceId,
						TokenSpan tokenSpan,
						Signal signal,
						TimeMLTense timeMLTense, 
						TimeMLAspect timeMLAspect, 
						TimeMLClass timeMLClass, 
						TimeMLPolarity timeMLPolarity, 
						TimeMLMood timeMLMood, 
						TimeMLVerbForm timeMLVerbForm, 
						TimeMLPoS timeMLPoS,
						String modality,
						String cardinality) {
		this.dataTools = dataTools;
		this.reference = reference;
		this.id = id;
		this.sourceId = sourceId;
		this.sourceInstanceId = sourceInstanceId;
		this.signal = signal;
		this.tokenSpan = tokenSpan;
		this.timeMLTense = timeMLTense;
		this.timeMLAspect = timeMLAspect;
		this.timeMLClass = timeMLClass;
		this.timeMLPolarity = timeMLPolarity;
		this.timeMLMood = timeMLMood;
		this.timeMLVerbForm = timeMLVerbForm;
		this.timeMLPoS = timeMLPoS;
		this.modality = modality;
		this.cardinality = cardinality;
	}
	
	public TLinkable.Type getTLinkableType() {
		return TLinkable.Type.EVENT;
	}
	
	/**
	 * @return the event instance id (eiid) for this event
	 */
	public String getId() {
		return this.id;
	}
	
	public TokenSpan getTokenSpan() {
		return this.tokenSpan;
	}
	
	/**
	 * @return the event id (eid) for this event
	 */
	public String getSourceId() {
		return this.sourceId;
	}
	
	public String getSourceInstanceId() {
		return this.sourceInstanceId;
	}
	
	public Signal getSignal() {
		return this.signal;
	}
	
	public TimeMLTense getTimeMLTense() {
		return this.timeMLTense;
	}
	
	public TimeMLAspect getTimeMLAspect() {
		return this.timeMLAspect;
	}
	
	public TimeMLPolarity getTimeMLPolarity() {
		return this.timeMLPolarity;
	}
	
	public TimeMLClass getTimeMLClass() {
		return this.timeMLClass;
	}
	
	/**
	 * @return part-of-speech according to TimeML (some
	 * versions of TempEval have their own PoS tags given
	 * in the data set)
	 */
	public TimeMLPoS getTimeMLPoS() {
		return this.timeMLPoS;
	}
	
	public TimeMLMood getTimeMLMood() {
		return this.timeMLMood;
	}
	
	public TimeMLVerbForm getTimeMLVerbForm() {
		return this.timeMLVerbForm;
	}
	
	public String getModality() {
		return this.modality;
	}
	
	public String getCardinality() {
		return this.cardinality;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			if (this.id != null)
				json.put("id", this.id);
			if (this.tokenSpan != null)
				json.put("tokenSpan", this.tokenSpan.toJSON(SerializationType.SENTENCE));
			if (this.sourceId != null)
				json.put("sourceId", this.sourceId);
			if (this.sourceInstanceId != null)
				json.put("sourceInstanceId", this.sourceInstanceId);
			if (this.signal != null)
				json.put("signal", this.signal.getStoreReference().toJSON()); 
			if (this.timeMLTense != null)
				json.put("timeMLTense", this.timeMLTense.toString());
			if (this.timeMLAspect != null)
				json.put("timeMLAspect", this.timeMLAspect.toString());
			if (this.timeMLPolarity != null)
				json.put("timeMLPolarity", this.timeMLPolarity.toString());
			if (this.timeMLClass != null)
				json.put("timeMLClass", this.timeMLClass.toString());
			if (this.timeMLPoS != null)
				json.put("timeMLPoS", this.timeMLPoS.toString());
			if (this.timeMLMood != null)
				json.put("timeMLMood", this.timeMLMood.toString());
			if (this.timeMLVerbForm != null)
				json.put("timeMLVerbForm", this.timeMLVerbForm.toString());
			if (this.modality != null)
				json.put("modality", this.modality);
			if (this.cardinality != null)
				json.put("cardinality", this.cardinality);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}
	
	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			if (json.has("id"))
				this.id = json.getString("id");
			if (json.has("tokenSpan"))
				this.tokenSpan = TokenSpan.fromJSON(json.getJSONObject("tokenSpan"), this.dataTools.getStoredItemSetManager());
			if (json.has("sourceId"))
				this.sourceId = json.getString("sourceId");
			if (json.has("sourceInstanceId"))
				this.sourceInstanceId = json.getString("sourceInstanceId");
			if (json.has("signal")) {
				StoreReference r = new StoreReference();
				if (!r.fromJSON(json.getJSONObject("signal")))
					return false;
				this.signal = this.dataTools.getStoredItemSetManager().resolveStoreReference(r, true);
			}
			if (json.has("timeMLTense"))
				this.timeMLTense = TimeMLTense.valueOf(json.getString("timeMLTense"));
			if (json.has("timeMLAspect"))
				this.timeMLAspect = TimeMLAspect.valueOf(json.getString("timeMLAspect"));
			if (json.has("timeMLPolarity"))
				this.timeMLPolarity = TimeMLPolarity.valueOf(json.getString("timeMLPolarity"));
			if (json.has("timeMLClass"))
				this.timeMLClass = TimeMLClass.valueOf(json.getString("timeMLClass"));
			if (json.has("timeMLPoS"))
				this.timeMLPoS = TimeMLPoS.valueOf(json.getString("timeMLPoS"));
			if (json.has("timeMLMood"))
				this.timeMLMood = TimeMLMood.valueOf(json.getString("timeMLMood"));
			if (json.has("timeMLVerbForm"))
				this.timeMLVerbForm = TimeMLVerbForm.valueOf(json.getString("timeMLVerbForm"));
			if (json.has("modality"))
				this.modality = json.getString("modality");
			if (json.has("polarity"))
				this.cardinality = json.getString("cardinality");
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}

	@Override
	public StoredJSONSerializable makeInstance(StoreReference reference) {
		return new EventMention(this.dataTools, reference);
	}

	@Override
	public StoreReference getStoreReference() {
		return this.reference;
	}
}

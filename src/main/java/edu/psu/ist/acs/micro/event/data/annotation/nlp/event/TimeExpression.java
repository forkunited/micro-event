package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.SerializationType;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;

/**
 * Time represents a TimeML Timex (temporal expression).
 * 
 * See http://timeml.org/site/index.html for details.
 * 
 * @author Bill McDowell
 * 
 */
public class TimeExpression implements TLinkable {
	private static boolean FORCE_DATE_DCT = true;
	
	public enum TimeMLType {
		DATE,
		TIME,
		DURATION,
		SET
	}
	
	public enum TimeMLDocumentFunction {
		CREATION_TIME,
		EXPIRATION_TIME,
		MODIFICATION_TIME,
		PUBLICATION_TIME,
		RELEASE_TIME,
		RECEPTION_TIME,
		NONE
	}
	
	public enum TimeMLMod {
		BEFORE,
		AFTER,
		ON_OR_BEFORE,
		ON_OR_AFTER,
		LESS_THAN,
		MORE_THAN,
		EQUAL_OR_LESS,
		EQUAL_OR_MORE,
		START,
		MID,
		END,
		APPROX
	}
	
	private TokenSpan tokenSpan;
	
	private String id;
	private String sourceId;
	private TimeMLType timeMLType;
	private StoreReference startTimeReference;
	private StoreReference endTimeReference;
	private String quant;
	private String freq;
	private NormalizedTimeValue value;
	private TimeMLDocumentFunction timeMLDocumentFunction = TimeMLDocumentFunction.NONE;
	private boolean temporalFunction;
	private StoreReference anchorTimeReference;
	private StoreReference valueFromFunctionReference;
	private TimeMLMod timeMLMod;
	
	private StoreReference reference;
	private DataTools dataTools;
	
	public TimeExpression(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public TimeExpression(DataTools dataTools, StoreReference reference) {
		this.dataTools = dataTools;
		this.reference = reference;
	}
	
	public TimeExpression(DataTools dataTools, 
						  StoreReference reference,
						  TokenSpan tokenSpan,
						  String id,
						  String sourceId,
						  TimeMLType timeMLType,
						  StoreReference startTimeReference,
						  StoreReference endTimeReference,
						  String quant,
						  String freq,
						  NormalizedTimeValue value,
						  TimeMLDocumentFunction timeMLDocumentFunction,
						  boolean temporalFunction,
						  StoreReference anchorTimeReference,
						  StoreReference valueFromFunctionReference,
						  TimeMLMod timeMLMod) {
		this.dataTools = dataTools;
		this.reference = reference;
		this.tokenSpan = tokenSpan;
		this.id = id;
		this.sourceId = sourceId;
		this.timeMLType = timeMLType;
		this.startTimeReference = startTimeReference;
		this.endTimeReference = endTimeReference;
		this.quant = quant;
		this.freq = freq;
		this.timeMLDocumentFunction = timeMLDocumentFunction;
		this.temporalFunction = temporalFunction;
		this.anchorTimeReference = anchorTimeReference;
		this.valueFromFunctionReference = valueFromFunctionReference;
		this.timeMLMod = timeMLMod;
		setValue(value);
	}
	
	private boolean setValue(NormalizedTimeValue value) {
		if (this.timeMLDocumentFunction == TimeMLDocumentFunction.CREATION_TIME) {
			if (FORCE_DATE_DCT) {
				NormalizedTimeValue dateValue = value.toDate();
				if (dateValue != null)
					this.value = dateValue;
				else
					this.value = value;
			}
		} else {
			this.value = value;
		}
		
		return true;
	}
	
	public TLinkable.Type getTLinkableType() {
		return TLinkable.Type.TIME;
	}
	
	public TokenSpan getTokenSpan() {
		return this.tokenSpan;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getSourceId() {
		return this.sourceId;
	}
	
	public TimeMLType getTimeMLType() {
		return this.timeMLType;
	}
	
	public TimeExpression getStartTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.startTimeReference, true);
	}
	
	public TimeExpression getEndTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.endTimeReference, true);
	}
	
	public String getQuant() {
		return this.quant;
	}
	
	public String getFreq() {
		return this.freq;
	}
	
	/**
	 * @return a NormalizedTimeValue representing the 
	 * grounded time-interval referenced by the Time
	 * expression
	 */
	public NormalizedTimeValue getValue() {
		return this.value;
	}
	
	public TimeMLDocumentFunction getTimeMLDocumentFunction() {
		return this.timeMLDocumentFunction;
	}
	
	public boolean getTemporalFunction() {
		return this.temporalFunction;
	}
	
	public TimeExpression getAnchorTime() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.anchorTimeReference, true);
	}
	
	public TimeExpression getValueFromFunction() {
		return this.dataTools.getStoredItemSetManager().resolveStoreReference(this.valueFromFunctionReference, true);
	}
	
	public TimeMLMod getTimeMLMod() {
		return this.timeMLMod;
	}
	
	/**
	 * @param time
	 * @return TLink relation type between this Time and the given
	 * time according to their grounded time intervals.  
	 */
	public TLink.TimeMLRelType getRelationToTime(TimeExpression time) {
		return getRelationToTime(time, true);
	}
	
	public TLink.TimeMLRelType getRelationToTime(TimeExpression time, boolean unknownVague) {
		if (this.timeMLType != TimeExpression.TimeMLType.DATE && this.timeMLType != TimeExpression.TimeMLType.TIME)
			return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
		if (time.timeMLType != TimeExpression.TimeMLType.DATE && time.timeMLType != TimeExpression.TimeMLType.TIME)
			return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
		
		DocumentNLP timeDocument = time.getTokenSpan().getDocument();
		TimeExpression timeCreationTime = null;
		if (timeDocument.hasAnnotationType(AnnotationTypeNLPEvent.CREATION_TIME))
			timeCreationTime = timeDocument.getDocumentAnnotation(AnnotationTypeNLPEvent.CREATION_TIME).resolve(this.dataTools, true);
		
		DocumentNLP thisDocument = getTokenSpan().getDocument();
		TimeExpression thisCreationTime = null;
		if (thisDocument.hasAnnotationType(AnnotationTypeNLPEvent.CREATION_TIME))
			thisCreationTime = thisDocument.getDocumentAnnotation(AnnotationTypeNLPEvent.CREATION_TIME).resolve(this.dataTools, true);

		if (this.value.getReference() != NormalizedTimeValue.Reference.NONE 
				|| time.value.getReference() != NormalizedTimeValue.Reference.NONE) {
			if (thisCreationTime == null || timeCreationTime == null 
					|| thisCreationTime.value.getReference() != NormalizedTimeValue.Reference.NONE
					|| timeCreationTime.value.getReference() != NormalizedTimeValue.Reference.NONE)
				return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			
			int thisCtTimeCt = 0;
			if (!thisDocument.getName().equals(timeDocument.getName())) {
				TLink.TimeMLRelType ctRel = thisCreationTime.getRelationToTime(timeCreationTime);
				if (ctRel == TLink.TimeMLRelType.BEFORE)
					thisCtTimeCt = -1;
				else if (ctRel == TLink.TimeMLRelType.AFTER)
					thisCtTimeCt = 1;
				else if (ctRel == TLink.TimeMLRelType.SIMULTANEOUS
						|| ctRel == TLink.TimeMLRelType.INCLUDES
						|| ctRel == TLink.TimeMLRelType.IS_INCLUDED)
					thisCtTimeCt = 0;
				else 
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			}
			
			
			// Relate this to creation and time to creation based on past
			// and future references
			int thisCt = 0, timeCt = 0; 
			
			if (this.value.getReference() != NormalizedTimeValue.Reference.NONE) {
				if (this.value.getReference() == NormalizedTimeValue.Reference.FUTURE)
					thisCt = 1;
				else if (this.value.getReference() == NormalizedTimeValue.Reference.PAST)
					thisCt = -1;
				else
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			} else {
				TLink.TimeMLRelType thisCtRelation = getRelationToTime(thisCreationTime);
				if (thisCtRelation == TLink.TimeMLRelType.VAGUE)
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
				else if (thisCtRelation == TLink.TimeMLRelType.BEFORE)
					thisCt = -1;
				else if (thisCtRelation == TLink.TimeMLRelType.AFTER)
					thisCt = 1;
				/*else if (thisCtRelation == TLink.TimeMLRelType.SIMULTANEOUS || thisCtRelation == TLink.TimeMLRelType.IS_INCLUDED)
					thisCt = 0;*/
				else
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			}
			
			if (time.value.getReference() != NormalizedTimeValue.Reference.NONE) {
				if (time.value.getReference() == NormalizedTimeValue.Reference.FUTURE)
					timeCt = 1;
				else if (time.value.getReference() == NormalizedTimeValue.Reference.PAST)
					timeCt = -1;
				else
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			} else {
				TLink.TimeMLRelType timeCtRelation = time.getRelationToTime(timeCreationTime);
				if (timeCtRelation == TLink.TimeMLRelType.VAGUE)
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
				else if (timeCtRelation == TLink.TimeMLRelType.BEFORE)
					timeCt = -1;
				else if (timeCtRelation == TLink.TimeMLRelType.AFTER)
					timeCt = 1;
				/*else if (timeCtRelation == TLink.TimeMLRelType.SIMULTANEOUS || timeCtRelation == TLink.TimeMLRelType.IS_INCLUDED)
					timeCt = 0;*/
				else
					return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
			}
			
			if (thisCt < timeCt && thisCtTimeCt <= 0)
				return TLink.TimeMLRelType.BEFORE;
			else if (timeCt < thisCt && thisCtTimeCt >= 0)
				return TLink.TimeMLRelType.AFTER;
			else
				return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
		}
		
		Pair<Calendar, Calendar> thisInterval = this.value.getRange();
		Pair<Calendar, Calendar> timeInterval = time.value.getRange();
		
		if (thisInterval == null || timeInterval == null)
			return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
		
		int startStart = thisInterval.getFirst().compareTo(timeInterval.getFirst());
		int startEnd = thisInterval.getFirst().compareTo(timeInterval.getSecond());
		int endStart = thisInterval.getSecond().compareTo(timeInterval.getFirst());
		int endEnd = thisInterval.getSecond().compareTo(timeInterval.getSecond());
		
		if (startStart == 0 && endEnd == 0)
			return TLink.TimeMLRelType.SIMULTANEOUS;
		else if (endStart <= 0)
			return TLink.TimeMLRelType.BEFORE;
		else if (startEnd >= 0)
			return TLink.TimeMLRelType.AFTER;
		else if (startStart < 0 && endEnd > 0)
			return TLink.TimeMLRelType.INCLUDES;
		else if (startStart > 0 && endEnd < 0)
			return TLink.TimeMLRelType.IS_INCLUDED;
		else if (startStart > 0 && startEnd < 0 && endEnd > 0)
			return TLink.TimeMLRelType.OVERLAPPED_BY;
		else if (startStart < 0 && endStart > 0 && endEnd < 0)
			return TLink.TimeMLRelType.OVERLAPS;
		else
			return (unknownVague) ? TLink.TimeMLRelType.VAGUE : null;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			if (this.id != null)
				json.put("id", this.id);
			if (this.sourceId != null)
				json.put("sourceId", this.sourceId);
			if (this.tokenSpan != null)
				json.put("tokenSpan", this.tokenSpan.toJSON(SerializationType.STORE_REFERENCE));
			if (this.timeMLType != null)
				json.put("timeMLType", this.timeMLType.toString());
			if (this.startTimeReference != null)
				json.put("startTime", this.startTimeReference.toJSON());
			if (this.endTimeReference != null)
				json.put("endTime", this.endTimeReference.toJSON());	
			if (this.freq != null)
				json.put("freq", this.freq);
			if (this.value != null)
				json.put("value", this.value.toString());
			if (this.quant != null)
				json.put("quant", this.quant);
			if (this.timeMLDocumentFunction != null)
				json.put("timeMLDocumentFunction", this.timeMLDocumentFunction.toString());
			
			json.put("temporalFunction", this.temporalFunction);
			
			if (this.anchorTimeReference != null)
				json.put("anchorTime", this.anchorTimeReference.toJSON());
			if (this.valueFromFunctionReference != null)
				json.put("valueFromFunction", this.valueFromFunctionReference.toJSON());
			if (this.timeMLMod != null)
				json.put("timeMLMod", this.timeMLMod.toString());
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
			if (json.has("sourceId"))
				this.sourceId = json.getString("sourceId");
			if (json.has("tokenSpan"))
				this.tokenSpan = TokenSpan.fromJSON(json.getJSONObject("tokenSpan"), this.dataTools.getStoredItemSetManager());
			if (json.has("timeMLType"))
				this.timeMLType = TimeMLType.valueOf(json.getString("timeMLType"));
			if (json.has("startTime"))
				this.startTimeReference = StoreReference.makeFromJSON(json.getJSONObject("startTime"));
			if (json.has("endTime"))
				this.endTimeReference = StoreReference.makeFromJSON(json.getJSONObject("endTime"));
			if (json.has("freq"))
				this.freq = json.getString("freq");
			if (json.has("quant"))
				this.quant = json.getString("quant");
			if (json.has("timeMLDocumentFunction"))
				this.timeMLDocumentFunction = TimeMLDocumentFunction.valueOf(json.getString("timeMLDocumentFunction"));
			if (json.has("temporalFunction"))
				this.temporalFunction = json.getBoolean("temporalFunction");
			if (json.has("anchorTime"))
				this.anchorTimeReference = StoreReference.makeFromJSON(json.getJSONObject("anchorTime"));
			if (json.has("valueFromFunction"))
				this.valueFromFunctionReference = StoreReference.makeFromJSON(json.getJSONObject("valueFromFunction"));
			if (json.has("timeMLMod"))
				this.timeMLMod = TimeMLMod.valueOf(json.getString("timeMLMod"));
			if (json.has("value"))
				setValue(new NormalizedTimeValue(json.getString("value")));
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}

	@Override
	public StoredJSONSerializable makeInstance(StoreReference reference) {
		return new TimeExpression(this.dataTools, reference);
	}

	@Override
	public StoreReference getStoreReference() {
		return this.reference;
	}
}

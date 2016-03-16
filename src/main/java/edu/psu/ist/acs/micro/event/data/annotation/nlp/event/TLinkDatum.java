package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLinkable.Type;
import edu.psu.ist.acs.micro.event.data.feature.FeatureTLinkAttribute;
import edu.psu.ist.acs.micro.event.data.feature.FeatureTLinkEventAttribute;
import edu.psu.ist.acs.micro.event.data.feature.FeatureTLinkTimeAttribute;
import edu.psu.ist.acs.micro.event.data.feature.FeatureTLinkTimeRelation;
import edu.psu.ist.acs.micro.event.data.feature.FeatureTLinkableType;
import edu.psu.ist.acs.micro.event.task.classify.MethodClassificationTLinkTypeAdjacentEventTime;
import edu.psu.ist.acs.micro.event.task.classify.MethodClassificationTLinkTypeGeneralGovernor;
import edu.psu.ist.acs.micro.event.task.classify.MethodClassificationTLinkTypeReichenbach;
import edu.psu.ist.acs.micro.event.task.classify.MethodClassificationTLinkTypeReportingGovernor;
import edu.psu.ist.acs.micro.event.task.classify.MethodClassificationTLinkTypeTimeTime;

public class TLinkDatum<L> extends Datum<L> {
	private TLink tlink;

	public TLinkDatum(int id, TLink tlink, L label) {
		this.id = id;
		this.tlink = tlink;
		this.label = label;
	}
	
	public TLink getTLink() {
		return this.tlink;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(": ").append(this.tlink.getId());
		
		return str.toString();
	}
	
	public static Tools<String> getStringTools(DataTools dataTools) {
		Tools<String> tools =  new Tools<String>(dataTools) {
			@Override
			public String labelFromString(String str) {
				return str;
			}
		};
	
		return tools;
	}
	
	public static Tools<Boolean> getBooleanTools(DataTools dataTools) {
		Tools<Boolean> tools =  new Tools<Boolean>(dataTools) {
			@Override
			public Boolean labelFromString(String str) {
				if (str == null)
					return null;
				return str.toLowerCase().equals("true") || str.equals("1");
			}
		};
	
		return tools;
	}
	
	public static Tools<TimeMLRelType> getTimeMLRelTypeTools(DataTools dataTools) {
		Tools<TimeMLRelType> tools =  new Tools<TimeMLRelType>(dataTools) {
			@Override
			public TimeMLRelType labelFromString(String str) {
				if (str == null)
					return null;
				return TimeMLRelType.valueOf(str);
			}
		};
	
		tools.addGenericClassifyMethod(new MethodClassificationTLinkTypeAdjacentEventTime());
		tools.addGenericClassifyMethod(new MethodClassificationTLinkTypeGeneralGovernor());
		tools.addGenericClassifyMethod(new MethodClassificationTLinkTypeReichenbach());
		tools.addGenericClassifyMethod(new MethodClassificationTLinkTypeReportingGovernor());
		tools.addGenericClassifyMethod(new MethodClassificationTLinkTypeTimeTime());
		
		tools.addLabelMapping(new LabelMapping<TimeMLRelType>() {
			@Override
			public TimeMLRelType map(TimeMLRelType label) {
				if (label == null)
					return null;
				else if (label == TimeMLRelType.AFTER)
					return TimeMLRelType.AFTER;
				else if (label == TimeMLRelType.BEFORE)
					return TimeMLRelType.BEFORE;
				else if (label == TimeMLRelType.IS_INCLUDED)
					return TimeMLRelType.IS_INCLUDED;
				else if (label == TimeMLRelType.INCLUDES)
					return TimeMLRelType.INCLUDES;
				else if (label == TimeMLRelType.SIMULTANEOUS)
					return TimeMLRelType.SIMULTANEOUS;
				else 
					return TimeMLRelType.VAGUE;
			}
			
			@Override
			public String toString() {
				return "TBD";
			}
		});

		tools.addGenericDataSetBuilder(new DataSetBuilderTimeBankDense());
		
		return tools;
	}
	
	public static abstract class Tools<L> extends Datum.Tools<TLinkDatum<L>, L> { 
		public Tools(DataTools dataTools) {
			super(dataTools);
			
			this.addGenericFeature(new FeatureTLinkableType<L>());
			this.addGenericFeature(new FeatureTLinkAttribute<L>());
			this.addGenericFeature(new FeatureTLinkEventAttribute<L>());
			this.addGenericFeature(new FeatureTLinkTimeAttribute<L>());
			this.addGenericFeature(new FeatureTLinkTimeRelation<L>());
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "Source";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					return new TokenSpan[] { tlinkDatum.getTLink().getSource().getTokenSpan() } ;
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "Target";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					return new TokenSpan[] { tlinkDatum.getTLink().getTarget().getTokenSpan() } ;
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "SourceTarget";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					return new TokenSpan[] {  tlinkDatum.getTLink().getSource().getTokenSpan(),
							tlinkDatum.getTLink().getTarget().getTokenSpan() };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "BetweenSourceTarget";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					TokenSpan source = tlinkDatum.getTLink().getSource().getTokenSpan();
					TokenSpan target = tlinkDatum.getTLink().getTarget().getTokenSpan();
					if (!source.getDocument().getName().equals(target.getDocument().getName())
							|| source.getSentenceIndex() != target.getSentenceIndex())
						return new TokenSpan[0];
					
					
					return new TokenSpan[] { 
							new TokenSpan(
								source.getDocument(), 
								source.getSentenceIndex(),
								Math.min(source.getEndTokenIndex(), target.getEndTokenIndex()),
								Math.max(source.getStartTokenIndex(), target.getStartTokenIndex())
							) 
					};
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstEvent";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					if (tlinkDatum.getTLink().getSource().getTLinkableType() == Type.EVENT)
						return new TokenSpan[] { tlinkDatum.getTLink().getSource().getTokenSpan() };
					else if (tlinkDatum.getTLink().getTarget().getTLinkableType() == Type.EVENT)
						return new TokenSpan[] { tlinkDatum.getTLink().getTarget().getTokenSpan() };
					else
						return new TokenSpan[0];
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TLinkDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTime";
				}
				
				@Override
				public TokenSpan[] extract(TLinkDatum<L> tlinkDatum) {
					if (tlinkDatum.getTLink().getSource().getTLinkableType() == Type.TIME)
						return new TokenSpan[] { tlinkDatum.getTLink().getSource().getTokenSpan() };
					else if (tlinkDatum.getTLink().getTarget().getTLinkableType() == Type.TIME)
						return new TokenSpan[] { tlinkDatum.getTLink().getTarget().getTokenSpan() };
					else
						return new TokenSpan[0];
				}
			});
		}
		
		@Override
		public TLinkDatum<L> datumFromJSON(JSONObject json) {
			try {
				int id = Integer.valueOf(json.getString("id"));
				
				L label = (json.has("label")) ? labelFromString(json.getString("label")) : null;
				TLink tlink = this.dataTools.getStoredItemSetManager()
						.resolveStoreReference(StoreReference.makeFromJSON(json.getJSONObject("tlink")), true);
				return new TLinkDatum<L>(id, tlink, label);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public JSONObject datumToJSON(TLinkDatum<L> datum) {
			JSONObject json = new JSONObject();
			
			try {
				json.put("id", String.valueOf(datum.id));
				if (datum.label != null)
					json.put("label", datum.label.toString());
				json.put("tlink", datum.tlink.getStoreReference().toJSON());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return json;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				TLinkDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			
			TLinkDatum<Boolean> binaryDatum = new TLinkDatum<Boolean>(datum.getId(), datum.getTLink(), 
					(labelIndicator == null || datum.getLabel() == null) ? null : labelIndicator.indicator(datum.getLabel()));
			
			if (labelIndicator != null && datum.getLabel() != null) {
				double labelWeight = labelIndicator.weight(datum.getLabel());
				binaryDatum.setLabelWeight(true, labelWeight);
			}
			
			return (T)(binaryDatum);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(
				LabelIndicator<L> labelIndicator) {
			OutputWriter genericOutput = this.dataTools.getOutputWriter();
			OutputWriter output = new OutputWriter(
					(genericOutput.getDebugFilePath() != null) ? new File(genericOutput.getDebugFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getResultsFilePath() != null) ? new File(genericOutput.getResultsFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getDataFilePath() != null) ? new File(genericOutput.getDataFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getModelFilePath() != null) ? new File(genericOutput.getModelFilePath() + "." + labelIndicator.toString()) : null
				);
			DataTools dataTools = this.dataTools.makeInstance(output);
			Datum.Tools<T, Boolean> binaryTools = (Datum.Tools<T, Boolean>)TLinkDatum.getBooleanTools(dataTools);
			
			return binaryTools;
			
		}
	}
}
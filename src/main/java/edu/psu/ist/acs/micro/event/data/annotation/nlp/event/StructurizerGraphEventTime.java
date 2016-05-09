package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.StructurizerGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerGraphEventTime<L> extends StructurizerGraph<EventTimeDatum<L>, L> {
	public StructurizerGraphEventTime() {
		super();
	}
	
	public StructurizerGraphEventTime(DatumContext<EventTimeDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected WeightedStructureRelation makeDatumStructure(EventTimeDatum<L> datum, L label) {
		if (label == null)
			return null;
		
		WeightedStructureRelationBinary binaryRel = (WeightedStructureRelationBinary)this.context.getDatumTools().getDataTools().makeWeightedStructure(label.toString(), this.context);
		return new WeightedStructureRelationBinary(
				label.toString(), 
				this.context, 
				String.valueOf(datum.getId()), 
				new WeightedStructureRelationUnary("O", this.context, datum.getEvent().getId()),
				new WeightedStructureRelationUnary("O", this.context, datum.getTime().getId()),
				binaryRel.isOrdered());
	}

	@Override
	protected String getStructureId(EventTimeDatum<L> datum) {
		return "0";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected List<WeightedStructureRelation> getDatumRelations(EventTimeDatum<L> datum, WeightedStructureGraph graph) {
		return (List<WeightedStructureRelation>)(List)graph.getEdges(datum.getEvent().getId(), datum.getTime().getId());
	}

	@Override
	public Structurizer<EventTimeDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<EventTimeDatum<L>, L> context) {
		return new StructurizerGraphEventTime<L>(context);
	}

	@Override
	public String getGenericName() {
		return "GraphEventTime";
	}
}

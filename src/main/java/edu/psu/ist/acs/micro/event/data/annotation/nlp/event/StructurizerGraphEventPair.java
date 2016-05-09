package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.StructurizerGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerGraphEventPair<L> extends StructurizerGraph<EventPairDatum<L>, L> {
	public StructurizerGraphEventPair() {
		super();
	}
	
	public StructurizerGraphEventPair(DatumContext<EventPairDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected WeightedStructureRelation makeDatumStructure(EventPairDatum<L> datum, L label) {
		if (label == null)
			return null;
		
		WeightedStructureRelationBinary binaryRel = (WeightedStructureRelationBinary)this.context.getDatumTools().getDataTools().makeWeightedStructure(label.toString(), this.context);
		return new WeightedStructureRelationBinary(
				label.toString(), 
				this.context, 
				String.valueOf(datum.getId()), 
				new WeightedStructureRelationUnary("O", this.context, datum.getSource().getId()),
				new WeightedStructureRelationUnary("O", this.context, datum.getTarget().getId()),
				binaryRel.isOrdered());
	}

	@Override
	protected String getStructureId(EventPairDatum<L> datum) {
		return "0";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected List<WeightedStructureRelation> getDatumRelations(EventPairDatum<L> datum, WeightedStructureGraph graph) {
		return (List<WeightedStructureRelation>)(List)graph.getEdges(datum.getSource().getId(), datum.getTarget().getId());
	}

	@Override
	public Structurizer<EventPairDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<EventPairDatum<L>, L> context) {
		return new StructurizerGraphEventPair<L>(context);
	}

	@Override
	public String getGenericName() {
		return "GraphEventPair";
	}
}

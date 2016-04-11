package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.StructurizerDocumentNLPGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerDocumentNLPGraphEventMentionPair<L> extends StructurizerDocumentNLPGraph<EventMentionPairDatum<L>, L> {
	public StructurizerDocumentNLPGraphEventMentionPair() {
		super();
	}
	
	public StructurizerDocumentNLPGraphEventMentionPair(DatumContext<EventMentionPairDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected WeightedStructureRelation makeDatumStructure(EventMentionPairDatum<L> datum, L label) {
			return new WeightedStructureRelationBinary(
					label.toString(), 
					this.context, 
					String.valueOf(datum.getId()), 
					new WeightedStructureRelationUnary("EM", this.context, datum.getSourceMention().getId()),
					new WeightedStructureRelationUnary("EM", this.context, datum.getTargetMention().getId()),
					false);
	}

	@Override
	protected String getDocumentNLPStructureId(EventMentionPairDatum<L> datum) {
		String id = datum.getSourceMention().getTokenSpan().getDocument().getName();
		String id2 = datum.getTargetMention().getTokenSpan().getDocument().getName();
		if (!id2.equals(id)) {
			if (id.compareTo(id2) < 0)
				id = id + "_" + id2;
			else 
				id = id2 + "_" + id;
		}
		return id;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected List<WeightedStructureRelation> getDatumRelations(EventMentionPairDatum<L> datum, WeightedStructureGraph graph) {
		return (List<WeightedStructureRelation>)(List)graph.getEdges(datum.getSourceMention().getId(), datum.getTargetMention().getId());
	}

	@Override
	public Structurizer<EventMentionPairDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<EventMentionPairDatum<L>, L> context) {
		return new StructurizerDocumentNLPGraphEventMentionPair<L>(context);
	}

	@Override
	public String getGenericName() {
		return "DocumentNLPGraphEventMentionPair";
	}
}

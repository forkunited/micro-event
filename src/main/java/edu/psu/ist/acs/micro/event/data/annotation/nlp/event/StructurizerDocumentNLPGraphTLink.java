package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.StructurizerDocumentNLPGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerDocumentNLPGraphTLink<L> extends StructurizerDocumentNLPGraph<TLinkDatum<L>, L> {
	public StructurizerDocumentNLPGraphTLink() {
		super();
	}
	
	public StructurizerDocumentNLPGraphTLink(DatumContext<TLinkDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected WeightedStructureRelation makeDatumStructure(TLinkDatum<L> datum, L label) {
			return new WeightedStructureRelationBinary(
					label.toString(), 
					this.context, 
					String.valueOf(datum.getId()), 
					new WeightedStructureRelationUnary("O", this.context, datum.getTLink().getSource().getId()),
					new WeightedStructureRelationUnary("O", this.context, datum.getTLink().getTarget().getId()),
					false);
	}

	@Override
	protected String getDocumentNLPStructureId(TLinkDatum<L> datum) {
		TLink tlink = datum.getTLink();
		String id = tlink.getSource().getTokenSpan().getDocument().getName();
		String targetName = tlink.getTarget().getTokenSpan().getDocument().getName();
		if (!targetName.equals(id)) {
			if (id.compareTo(targetName) < 0)
				id = id + "_" + targetName;
			else 
				id = targetName + "_" + id;
		}
		return id;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected List<WeightedStructureRelation> getDatumRelations(TLinkDatum<L> datum, WeightedStructureGraph graph) {
		return (List<WeightedStructureRelation>)(List)graph.getEdges(datum.getTLink().getSource().getId(), datum.getTLink().getTarget().getId());
	}

	@Override
	public Structurizer<TLinkDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<TLinkDatum<L>, L> context) {
		return new StructurizerDocumentNLPGraphTLink<L>(context);
	}

	@Override
	public String getGenericName() {
		return "DocumentNLPGraphTLink";
	}
}

package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerTLinkDocument<L> extends Structurizer<TLinkDatum<L>, L, WeightedStructureGraph>  {

	private LabelMapping<L> labelMapping;
	private String[] parameterNames = { "labelMapping" };
	
	public StructurizerTLinkDocument() {
		
	}
	
	public StructurizerTLinkDocument(DatumContext<TLinkDatum<L>, L> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("labelMapping"))
			return Obj.stringValue(this.labelMapping.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("labelMapping"))
			this.labelMapping = this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Map<String, WeightedStructureGraph> addToStructures(TLinkDatum<L> datum, L label, double weight, Map<String, WeightedStructureGraph> structures) {
		WeightedStructureGraph graph = getOrConstructStructure(datum, structures);
		WeightedStructureRelationBinary link = makeTLinkStructure(datum, label);
		
		graph.add(link, weight);
	 
		return structures;
	}

	@Override
	public Map<String, WeightedStructureGraph> makeStructures() {
		return new HashMap<String, WeightedStructureGraph>();
	}

	@Override
	public Map<L, Double> getLabels(TLinkDatum<L> datum, Map<String, WeightedStructureGraph> structures) {
		WeightedStructureGraph graph = getOrConstructStructure(datum, structures);
		String id1 = datum.getTLink().getSource().getId();
		String id2 = datum.getTLink().getTarget().getId();
		List<WeightedStructureRelationBinary> edges = graph.getEdges(id1, id2);
		Map<L, Double> labels = new HashMap<L, Double>();
		
		for (WeightedStructureRelationBinary edge : edges) {
			L label = this.context.getDatumTools().labelFromString(edge.getType());
			
			if (this.labelMapping != null)
				label = this.labelMapping.map(label);
			
			if (label == null)
				continue;

			labels.put(label, graph.getEdgeWeight(edge));
		}
		
		return labels;
	}

	@Override
	public Structurizer<TLinkDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<TLinkDatum<L>, L> context) {
		return new StructurizerTLinkDocument<L>(context);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "TLinkDocument";
	}

	private String getStructureId(TLinkDatum<L> datum) {
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
	
	private WeightedStructureGraph getOrConstructStructure(TLinkDatum<L> datum, Map<String, WeightedStructureGraph> structures) {
		String id = getStructureId(datum);
		if (!structures.containsKey(id)) {
			WeightedStructureGraph graph = (WeightedStructureGraph)this.context.getDataTools().makeWeightedStructure("Graph", this.context);
			structures.put(id, graph);
		}
		
		return structures.get(id);
	}
	
	private WeightedStructureRelationBinary makeTLinkStructure(TLinkDatum<L> datum, L label) {
		return new WeightedStructureRelationBinary(
				label.toString(), 
				this.context, 
				String.valueOf(datum.getId()), 
				new WeightedStructureRelationUnary("O", this.context, datum.getTLink().getSource().getId()),
				new WeightedStructureRelationUnary("O", this.context, datum.getTLink().getTarget().getId()),
				true);
	}
}

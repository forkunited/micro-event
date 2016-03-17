package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.TLink.TimeMLRelType;

public class DataSetBuilderTimeBankDense extends DataSetBuilder<TLinkDatum<TimeMLRelType>, TimeMLRelType> {	
	public enum Part {
		TRAIN,
		DEV,
		TEST;
	}
	
	public static final String[] DEV_DOC_NAMES = { 
		"APW19980227.0487.tml", 
		"CNN19980223.1130.0960.tml", 
		"NYT19980212.0019.tml",  
		"PRI19980216.2000.0170.tml", 
		"ed980111.1130.0089.tml" 
	};
	
	public static final String[] TEST_DOC_NAMES = { 
		"APW19980227.0489.tml",
		"APW19980227.0494.tml",
		"APW19980308.0201.tml",
		"APW19980418.0210.tml",
		"CNN19980126.1600.1104.tml",
		"CNN19980213.2130.0155.tml",
		"NYT19980402.0453.tml",
		"PRI19980115.2000.0186.tml",
		"PRI19980306.2000.1675.tml" 
	};
	
	private String storage;
	private String collection;
	private Part part = Part.TRAIN;
	private LabelMapping<TimeMLRelType> labelMapping;
	private String[] parameterNames = { "storage", "collection", "part", "labelMapping" };
	
	private static Integer datumId = 1;
	
	public DataSetBuilderTimeBankDense() {
		this(null);
	}
	
	public DataSetBuilderTimeBankDense(DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		this.context = context;
	}
	
	private int getNextDatumId() {
		int nextDatumId = 0;
		synchronized (datumId) {
			nextDatumId = datumId;
			datumId++;
		}
		return nextDatumId;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("storage"))
			return Obj.stringValue(this.storage);
		else if (parameter.equals("collection"))
			return Obj.stringValue(this.collection);
		else if (parameter.equals("part"))
			return Obj.stringValue(this.part.toString());
		else if (parameter.equals("labelMapping"))
			return this.labelMapping == null ? null : Obj.stringValue(this.labelMapping.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("storage"))
			this.storage = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("collection"))
			this.collection = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("part"))
			this.part = Part.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("labelMapping"))
			this.labelMapping = (parameterValue == null) ? null : this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}


	@Override
	public DataSetBuilder<TLinkDatum<TimeMLRelType>, TimeMLRelType> makeInstance(
			DatumContext<TLinkDatum<TimeMLRelType>, TimeMLRelType> context) {
		return new DataSetBuilderTimeBankDense(context);
	}

	@Override
	public DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> build() {
		StoredItemSetInMemoryLazy<TLink, TLink> tlinkSet = 
				this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.collection);
		
		DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType> data = new DataSet<TLinkDatum<TimeMLRelType>, TimeMLRelType>(this.context.getDatumTools());
		
		Set<String> devDocs = new HashSet<String>();
		Set<String> testDocs = new HashSet<String>();
		devDocs.addAll(Arrays.asList(DEV_DOC_NAMES));
		testDocs.addAll(Arrays.asList(TEST_DOC_NAMES));
		
		tlinkSet.map(new Fn<TLink, Boolean>() {
			@Override
			public Boolean apply(TLink tlink) {
				String doc = tlink.getSource().getTokenSpan().getDocument().getName();
				if ((part == Part.DEV && !devDocs.contains(doc)
				||  (part == Part.TEST && !testDocs.contains(doc))
				||  (part == Part.TRAIN && (devDocs.contains(doc) || testDocs.contains(doc)))))
					return true;
				
				synchronized (data) {
					data.add(new TLinkDatum<TimeMLRelType>(
						getNextDatumId(), tlink, (labelMapping != null) ? labelMapping.map(tlink.getTimeMLRelType()) : tlink.getTimeMLRelType()));
				}
				
				return true;
			}
			
		}, this.context.getMaxThreads(), this.context.getDataTools().getGlobalRandom());
		
		return data;
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
		return "TimeBankDense";
	}
}

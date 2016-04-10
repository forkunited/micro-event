package edu.psu.ist.acs.micro.event.data.annotation.nlp.event;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public abstract class DataSetBuilderACE2005<D extends Datum<L>, L>  extends DataSetBuilder<D, L> {
	public enum Part {
		TRAIN,
		DEV,
		TEST;
	}
	
	public static final String[] TEST_DOC_NAMES = { 
		"AFP_ENG_20030401.0476",
		"AFP_ENG_20030413.0098",
		"AFP_ENG_20030415.0734",
		"AFP_ENG_20030417.0004",
		"AFP_ENG_20030417.0307",
		"AFP_ENG_20030417.0764",
		"AFP_ENG_20030418.0556",
		"AFP_ENG_20030425.0408",
		"AFP_ENG_20030427.0118",
		"AFP_ENG_20030428.0720",
		"AFP_ENG_20030429.0007",
		"AFP_ENG_20030430.0075",
		"AFP_ENG_20030502.0614",
		"AFP_ENG_20030504.0248",
		"AFP_ENG_20030508.0118",
		"AFP_ENG_20030508.0357",
		"AFP_ENG_20030509.0345",
		"AFP_ENG_20030514.0706",
		"AFP_ENG_20030519.0049",
		"AFP_ENG_20030519.0372",
		"AFP_ENG_20030522.0878",
		"AFP_ENG_20030527.0616",
		"AFP_ENG_20030528.0561",
		"AFP_ENG_20030530.0132",
		"AFP_ENG_20030601.0262",
		"AFP_ENG_20030607.0030",
		"AFP_ENG_20030616.0715",
		"AFP_ENG_20030617.0846",
		"AFP_ENG_20030625.0057",
		"AFP_ENG_20030630.0271",
		"APW_ENG_20030304.0555",
		"APW_ENG_20030306.0191",
		"APW_ENG_20030308.0314",
		"APW_ENG_20030310.0719",
		"APW_ENG_20030311.0775",
		"APW_ENG_20030318.0689",
		"APW_ENG_20030319.0545",
		"APW_ENG_20030322.0119",
		"APW_ENG_20030324.0768",
		"APW_ENG_20030325.0786"
	};
	
	public static final String[] DEV_DOC_NAMES = { 
		"CNN_CF_20030303.1900.02",
		"CNN_IP_20030329.1600.00-2",
		"CNN_IP_20030402.1600.00-1",
		"CNN_IP_20030405.1600.01-1",
		"CNN_IP_20030409.1600.02",
		"marcellapr_20050228.2219",
		"rec.games.chess.politics_20041216.1047",
		"rec.games.chess.politics_20041217.2111",
		"soc.org.nonprofit_20050218.1902",
		"FLOPPINGACES_20050217.1237.014",
		"AGGRESSIVEVOICEDAILY_20041116.1347",
		"FLOPPINGACES_20041117.2002.024",
		"FLOPPINGACES_20050203.1953.038",
		"TTRACY_20050223.1049",
		"CNNHL_ENG_20030304_142751.10",
		"CNNHL_ENG_20030424_123502.25",
		"CNNHL_ENG_20030513_220910.32",
		"CNN_ENG_20030304_173120.16",
		"CNN_ENG_20030328_150609.10",
		"CNN_ENG_20030424_070008.15",
		"CNN_ENG_20030512_170454.13",
		"CNN_ENG_20030620_085840.7",
		"AFP_ENG_20030304.0250",
		"AFP_ENG_20030305.0918",
		"AFP_ENG_20030311.0491",
		"AFP_ENG_20030314.0238",
		"AFP_ENG_20030319.0879",
		"AFP_ENG_20030320.0722",
		"AFP_ENG_20030327.0022",
		"AFP_ENG_20030327.0224"
	};
	
	protected String storage;
	protected String collection;
	protected Part part = Part.TRAIN;
	protected String[] parameterNames = { "storage", "collection", "part" };
	
	private static Integer datumId = 1;
	
	public DataSetBuilderACE2005() {
		this(null);
	}
	
	public DataSetBuilderACE2005(DatumContext<D, L> context) {
		this.context = context;
	}
	
	protected int getNextDatumId() {
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
		else
			return false;
		return true;
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
}

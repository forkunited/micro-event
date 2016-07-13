package edu.psu.ist.acs.micro.event.scratch;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.util.FileUtil;

public class TestCAEVOFeatureOutput {
	public static void main(String[] args) throws JSONException {
		String caevoStr = FileUtil.readFile(args[0]);
		String microStr = FileUtil.readFile(args[1]);
		
		String[] caevoLines = caevoStr.split("\n");
		String[] microLines = microStr.split("\n");
		
		Arrays.sort(caevoLines);
		Arrays.sort(microLines);
		
		for (int i = 0; i < microLines.length; i++) {
			JSONObject caevoJson = new JSONObject(caevoLines[i].split("\t")[3]);
			JSONObject microJson = new JSONObject(microLines[i].split("\t")[3]);
			
			if (caevoJson.length() != microJson.length())
				throw new UnsupportedOperationException("Incorrect number of features:\n" + microLines[i] + "\n" + caevoLines[i]);
			
			microJson = convertMicroToCaevo(microJson);
			
			String[] caevoNames = JSONObject.getNames(caevoJson);
			for (String caevoName : caevoNames) {
				if (!microJson.has(caevoName))
					throw new UnsupportedOperationException("Missing feature (" + caevoName + "):\n" + microLines[i] + "\n" + caevoLines[i]);
				if (!microJson.get(caevoName).equals(caevoJson.get(caevoName)))
					throw new UnsupportedOperationException("Mismatch value (" + caevoName + "):\n" + microLines[i] + "\n" + caevoLines[i]);

			}
		}
		
		System.out.println("SUCCESS!");
	}
	
	private static JSONObject convertMicroToCaevo(JSONObject microJson) throws JSONException {
		JSONObject caevoJson = new JSONObject();
		String[] names = JSONObject.getNames(microJson);
		String caevoName = null;
		for (String name : names) {
			if (name.startsWith("fsToken_")) {
				caevoName = name.replace("fsToken_", "token1-");
			} else if (name.startsWith("fsPos_")) {
				caevoName = name.replace("fsPos_", "pos1-0-");
			} else if (name.startsWith("fsPosB2_0_")) {
				caevoName = name.replace("fsPosB2_0_", "pos1-1-");
				caevoName = caevoName.replace("PRE-0", "<s>");
			} else if (name.startsWith("fsPosB2_1_")) {
				caevoName = name.replace("fsPosB2_1_", "pos1-2-");
				caevoName = caevoName.replace("PRE-0", "<s>");
				caevoName = caevoName.replace("PRE-1", "<pre-s>");
			} else if (name.startsWith("fsPosBNI_")) {
				caevoName = name.replace("fsPosBNI_", "pos1-bi-");
				caevoName = caevoName.replace("PRE-0", "<s>");
				caevoName = caevoName.replace("PRE-1", "<pre-s>");
				caevoName = caevoName.replace("_", "-");
			} else if (name.startsWith("fsLemma_")) {
				caevoName = name.replace("fsLemma_", "lemma1-");
			} else if (name.startsWith("fsSynset1_")) {
				caevoName = name.replace("fsSynset1_", "synset1-");
			} else if (name.startsWith("ftToken_")) {
				caevoName = name.replace("ftToken_", "token2-");	
			} else if (name.startsWith("ftPos_")) {
				caevoName = name.replace("ftPos_", "pos2-0-");
			} else if (name.startsWith("ftPosB2_0_")) {
				caevoName = name.replace("ftPosB2_0_", "pos2-1-");
				caevoName = caevoName.replace("PRE-0", "<s>");
			} else if (name.startsWith("ftPosB2_1_")) {
				caevoName = name.replace("ftPosB2_1_", "pos2-2-");
				caevoName = caevoName.replace("PRE-0", "<s>");
				caevoName = caevoName.replace("PRE-1", "<pre-s>");
			} else if (name.startsWith("ftPosBNI_")) {
				caevoName = name.replace("ftPosBNI_", "pos2-bi-");
				caevoName = caevoName.replace("PRE-0", "<s>");
				caevoName = caevoName.replace("PRE-1", "<pre-s>");
				caevoName = caevoName.replace("_", "-");
			} else if (name.startsWith("ftLemma_")) {
				caevoName = name.replace("ftLemma_", "lemma2-");
			} else if (name.startsWith("ftSynset1_")) {
				caevoName = name.replace("ftSynset1_", "synset2-");
			} else if (name.startsWith("fstToken_")) {
				caevoName = name.replace("fstToken_", "BI");
				caevoName = caevoName.replace("//", "_");
			} else if (name.startsWith("fstPos_")) {
				caevoName = name.replace("fstPos_", "posBi_");
				caevoName = caevoName.replace("//", "-");
				caevoName = caevoName.replace("_", "");
			} else if (name.startsWith("fdepPathEE_")) {
				caevoName = name.replace("fdepPathEE_", "");
			} else if (name.startsWith("fconPathEE_")) {
				caevoName = name.replace("fconPathEE_", "pathnopos-");
			} else if (name.startsWith("fconPathPosEE_")) {
				caevoName = name.replace("fconPathPosEE_", "pathfull-");
			} else if (name.startsWith("foverEventEE_true")) {
				caevoName = name.replace("foverEventEE_true", "notsequential");
			} else if (name.startsWith("foverEventEE_false")) {
				caevoName = name.replace("foverEventEE_false", "sequential");
			} else if (name.startsWith("fdomEE_DOMINATING")) {
				caevoName = name.replace("fdomEE_DOMINATING", "dominates");
			} else if (name.startsWith("fdomEE_DOMINATED")) {
				caevoName = name.replace("fdomEE_DOMINATED", "isDominated");
			} else if (name.startsWith("fconstant_")) {
				caevoName = name.replace("fconstant_c", "order-sameSent-before");
			} else if (name.startsWith("fconstant1_")) {
				caevoName = name.replace("fconstant1_c", "order-sameSent");
			} else if (name.startsWith("fconstant2_")) {
				caevoName = name.replace("fconstant2_c", "order-before");
			} else {
				throw new UnsupportedOperationException("Unrecognized feature: " + name);
			}
			
			caevoJson.put(caevoName, microJson.get(name));
		}
		
		return caevoJson;
	}
}

package edu.psu.ist.acs.micro.event.scratch;

import java.io.BufferedReader;
import java.io.IOException;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.psu.ist.acs.micro.event.data.EventDataTools;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;

public class ConstructMID5NewsDocumentSet {
	private static EventDataTools dataTools;
	private static PipelineNLP nlpPipeline;
	
	public static void main(String[] args) throws IOException {
		dataTools = new EventDataTools();
		
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		pipelineStanford.initialize();
		
		NELLMentionCategorizer mentionCategorizer = new NELLMentionCategorizer(
				new CategoryList(CategoryList.Type.ALL_NELL_CATEGORIES, new CatDataTools()), 
				NELLMentionCategorizer.DEFAULT_MENTION_MODEL_THRESHOLD, NELLMentionCategorizer.DEFAULT_LABEL_TYPE, 
				1);
		PipelineNLPExtendable pipelineMicroCat = new PipelineNLPExtendable();
		pipelineMicroCat.extend(mentionCategorizer);
		
		
		nlpPipeline = pipelineStanford.weld(pipelineMicroCat);
	
		constructDocumentsFromBulkText(args[0]);
	}
	
	public static void constructDocumentsFromBulkText(String bulkTextPath) throws IOException {
		BufferedReader reader = FileUtil.getFileReader(bulkTextPath);
		String line = null;
		boolean documentContentLine = false;
		
		StringBuilder documentContent = new StringBuilder();
		PipelineNLP fullPipeline = null;
		
		String documentName = null;
		
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0)
				continue;
			if (line.toLowerCase().endsWith("-files.list")) {
				/* Parse meta-data */
				documentName = line.substring(0, line.indexOf("-files.list"));
				final String title = reader.readLine().trim();
				final String pubDate = reader.readLine().trim();
				final String source = reader.readLine().trim().substring("News source: ".length());
				final double svmScore = Double.valueOf(reader.readLine().trim().substring("SVM score: ".length()));
				
				PipelineNLPExtendable metaDataPipeline = new PipelineNLPExtendable();
				metaDataPipeline.extend(new AnnotatorDocument<String>() {
					public String getName() { return "MID5-News"; }
					public boolean measuresConfidence() { return false; }
					public AnnotationType<String> produces() { return AnnotationTypeNLPEvent.ARTICLE_TITLE; }
					public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
					public Pair<String, Double> annotate(DocumentNLP document) {
						return new Pair<String, Double>(title, null);
					}
				});
				
				metaDataPipeline.extend(new AnnotatorDocument<String>() {
					public String getName() { return "MID5-News"; }
					public boolean measuresConfidence() { return false; }
					public AnnotationType<String> produces() { return AnnotationTypeNLPEvent.ARTICLE_PUBLICATION_DATE; }
					public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
					public Pair<String, Double> annotate(DocumentNLP document) {
						return new Pair<String, Double>(pubDate, null); // FIXME Normalize this
					}
				});
				
				metaDataPipeline.extend(new AnnotatorDocument<String>() {
					public String getName() { return "MID5-News"; }
					public boolean measuresConfidence() { return false; }
					public AnnotationType<String> produces() { return AnnotationTypeNLPEvent.ARTICLE_SOURCE; }
					public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
					public Pair<String, Double> annotate(DocumentNLP document) {
						return new Pair<String, Double>(source, null);
					}
				});
				
				metaDataPipeline.extend(new AnnotatorDocument<Double>() {
					public String getName() { return "MID5-News"; }
					public boolean measuresConfidence() { return false; }
					public AnnotationType<Double> produces() { return AnnotationTypeNLPEvent.MID_SVM_RELEVANCE_SCORE; }
					public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
					public Pair<Double, Double> annotate(DocumentNLP document) {
						return new Pair<Double, Double>(svmScore, null);
					}
				});
				
				fullPipeline = nlpPipeline.weld(metaDataPipeline);
				
				documentContentLine = true;
			} else if (line.equals("---------------------------------------------------------------")) {
				/* End of document text, so construct document */
				DocumentNLP document = new DocumentNLPInMemory(dataTools, documentName, documentContent.toString(), Language.English, fullPipeline);
				System.out.println(document.toHtmlString());
				fullPipeline = null;
				documentContentLine = false;
			} else if (documentContentLine) {
				documentContent.append(line);
			}
		}
	}
}

package edu.psu.ist.acs.micro.event.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;

/**
 * 
 * AnnotationTypeNLPEvent represents types of annotations that
 * the micro-event project can add to NLP documents
 * 
 * @author Bill McDowell
 *
 */
public class AnnotationTypeNLPEvent {
	public static final AnnotationTypeNLP<Double> MID_SVM_RELEVANCE_SCORE = new AnnotationTypeNLP<Double>("mid-svm-rel-score", Double.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_TITLE = new AnnotationTypeNLP<String>("article-title", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_PUBLICATION_DATE = new AnnotationTypeNLP<String>("article-pub-date", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_SOURCE = new AnnotationTypeNLP<String>("article-source", String.class, Target.DOCUMENT);
}
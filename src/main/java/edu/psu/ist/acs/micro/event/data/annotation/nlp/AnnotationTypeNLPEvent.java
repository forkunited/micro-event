package edu.psu.ist.acs.micro.event.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;

/**
 * 
 * AnnotationTypeNLPEvent represents types of annotations that
 * the micro-event project can add to NLP documents
 * 
 * @author Bill McDowell
 *
 */
public class AnnotationTypeNLPEvent {
	public static final AnnotationTypeNLP<StoreReference> CREATION_TIME = new AnnotationTypeNLP<StoreReference>("dct", StoreReference.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<StoreReference> EVENT_MENTION = new AnnotationTypeNLP<StoreReference>("ev_mention", StoreReference.class, Target.TOKEN_SPAN);
	public static final AnnotationTypeNLP<StoreReference> TIME_EXPRESSION = new AnnotationTypeNLP<StoreReference>("timex", StoreReference.class, Target.TOKEN_SPAN);
}

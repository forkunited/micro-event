package edu.psu.ist.acs.micro.event.scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;

public class ConstructACE2005 {
	private static class ACESourceDocument {
		private String dctStr;
		private Pair<Integer, Integer> dctStrCharRange;
		private Map<String, String> metaData;
		private TreeMap<Integer, Pair<String, String>> bodyParts; // Map start char index to (part name, part value)
		
		public ACESourceDocument(String dctStr, 
								Pair<Integer, Integer> dctStrCharRange,
								Map<String, String> metaData,
								TreeMap<Integer, Pair<String, String>> bodyParts) {
			this.dctStr = dctStr;
			this.dctStrCharRange = dctStrCharRange;
			this.metaData = metaData;
			this.bodyParts = bodyParts;
		}
		
		public String getDCTStr() {
			return this.dctStr;
		}
		
		public boolean indexInDCTStr(int index) {
			return index >= this.dctStrCharRange.getFirst() && index < this.dctStrCharRange.getSecond();
		}
		
		public Map<String, String> getMetaData() {
			return this.metaData;
		}
		
		public Pair<String, String> getBodyPart(int charIndex) {
			return this.bodyParts.floorEntry(charIndex).getValue();
		}
	}
	
	public static void main(String[] args) {
		Map<String, Pair<File, File>> inputFiles = getInputFiles(args);
		
		Map<String, Set<String>> summary = new TreeMap<String, Set<String>>();
		for (Entry<String, Pair<File, File>> entry : inputFiles.entrySet()) {
			summarizeAnnotations(entry.getValue().getSecond(), summary);
			
			if (!parseAndOutputDocument(entry.getKey(), entry.getValue().getFirst(), entry.getValue().getSecond())) {
				throw new IllegalStateException("Failed to parse and output document " + entry.getKey());
			}
		}
		
		System.out.println("Annotation summary");
		for (Entry<String, Set<String>> entry : summary.entrySet()) {
			System.out.println(entry.getKey());
			if (entry.getValue().size() > 50) {
				System.out.println("\t(" + entry.getValue().size() + " values)");
			} else {
				for (String child : entry.getValue())
					System.out.println("\t" + child);
			}
		}
	}
	
	private static boolean parseAndOutputDocument(String documentName, File contentFile, File annotationFile) {		
		// FIXME
		constructSourceDocuments(contentFile);
		return true;
	}
	
	private static List<ACESourceDocument> constructSourceDocuments(File contentFile) {
		Pair<Element, String> rootAndString = getDocumentRootElementAndString(contentFile);
		Element rootElement = rootAndString.getFirst();
		Map<Element, Integer> charOffsets = getCharOffsets(rootAndString.getSecond(), traverse(rootElement));
		
		for (Entry<Element, Integer> entry : charOffsets.entrySet())
			System.out.println(entry.getKey().getName() + " " + entry.getValue());
		System.exit(1);
		
		if (isNewswire(rootElement))
			return getNewswireSource(rootElement);
		else if (isBroadcastConversation(rootElement))
			return getBroadcastConversationSource(rootElement);
		else if (isBroadcastNews(rootElement))
			return getBroadcastNewsSource(rootElement);
		else if (isTelephone(rootElement))
			return getTelephoneSource(rootElement);
		else if (isUsenet(rootElement))
			return getUsenetSource(rootElement);
		else if (isWeblog(rootElement))
			return getWeblogSource(rootElement);
		else {
			System.err.println("Unrecognized document source type " + contentFile.getAbsolutePath());
			System.exit(1);
		}
		return null;
	}
	
	private static boolean isNewswire(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("newswire");
	}
	
	private static List<ACESourceDocument> getNewswireSource(Element root) {
		
		// FIXME 
		/*
		 * <DOCID> AFP_ENG_20030522.0878 </DOCID>
	<DOCTYPE SOURCE="newswire"> NEWS STORY </DOCTYPE>
	<DATETIME> 20030522 </DATETIME>
	<BODY>
	<HEADLINE>
	Chinese leader favours Russia in first foreign tour by Henry Meyer
	</HEADLINE>
	<TEXT>
		 */
		return null;
	}
	
	private static boolean isBroadcastConversation(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("broadcast conversation");
	}

	private static List<ACESourceDocument> getBroadcastConversationSource(Element root) {
	/*
	 * <DOC>
<DOCID> CNN_CF_20030303.1900.00 </DOCID>
<DOCTYPE SOURCE="broadcast conversation"> STORY </DOCTYPE>
<DATETIME> 2003-03-03T19:00:00-05:00 </DATETIME>
<BODY>
<HEADLINE>
New Questions About Attacking Iraq; Is Torturing Terrorists Necessary?
</HEADLINE>
<TEXT>
<TURN>
<SPEAKER> BEGALA </SPEAKER>
adsfasdfsdf
</TURN>
	 */
		// FIXME
		return null;
	}
	
	private static boolean isBroadcastNews(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("broadcast news");
	}

	private static List<ACESourceDocument> getBroadcastNewsSource(Element root) {
		/* FIXME
		 * <DOC>
	<DOCID> CNN_ENG_20030327_163556.20 </DOCID>
	<DOCTYPE SOURCE="broadcast news"> NEWS STORY </DOCTYPE>
	<DATETIME> 2003-03-27 16:58:58 </DATETIME>
	<BODY>
	<TEXT>
	<TURN>
	for some americans who find themselves spending hours on
		 */
		return null;
	}
	
	private static boolean isTelephone(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("telephone");
	}
	
	private static List<ACESourceDocument> getTelephoneSource(Element root) {
		/*
		 * <DOC>
	<DOCID> fsh_29139 </DOCID>
	<DOCTYPE SOURCE="telephone"> CONVERSATION </DOCTYPE>
	<DATETIME> 20041130-18:20:45 </DATETIME>
	<BODY>
	<TEXT>
	<TURN>
	<SPEAKER> prompt </SPEAKER>
	2. Workplace Culture Describe the organizational structure of your
	current or former workplace. How many people work there, and what is
	the hierarchy?  Who works for whom?  What kind of job do you have?
	Describe some of the people that you work with and the jobs they do.
	Do you think your company is well-run?  If not, what could make it
	better?
	</TURN>
	<TURN>
	<SPEAKER> B </SPEAKER>
	And then we're not supposed to answer it. Well, um all I can say about
	big corporations -- is there's too many -- chiefs.
	</TURN>
		 */
		return null;
	}
	
	private static boolean isUsenet(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("usenet");
	}
	
	private static List<ACESourceDocument> getUsenetSource(Element root) {
		/*
		 * <DOC>
	<DOCID> alt.sys.pc-clone.dell_20050226.2350 </DOCID>
	<DOCTYPE SOURCE="usenet"> WEB TEXT </DOCTYPE>
	<DATETIME> 2005-02-26T23:50:00 </DATETIME>
	<BODY>
	<HEADLINE>
	Dell sued for "bait and switch" and false promises
	</HEADLINE>
	<TEXT>
	<POST>
	<POSTER> Timothy Daniels </POSTER>
	<POSTDATE> Sat, 26 Feb 2005 20:50:40 -0800 </POSTDATE>
	<SUBJECT> Dell sued for "bait and switch" and false promises </SUBJECT>
	
	Dell is involved in a class action suit for "bait and switch", where a
	nurse claims Dell switched parts and charged her for the more expensive
	items, and for promising "easy credit" for which no one qualifies and
	then charges ridiculously high interest rates.
	
	*TimDaniels*
	
	</POST>
	<POST>
	<POSTER> RRR_News </POSTER>
	<POSTDATE> Sun, 27 Feb 2005 12:14:24 -0500 </POSTDATE>
	<SUBJECT> Re: Dell sued for "bait and switch" and false promises </SUBJECT>
	
	It seems someone did not read the credit terms, before purchasing item,
	"buyers' remorse". And lawyers trying to make a buck from it. Hope federal
	tort reform gets passed by the congress, so we can get rid of these
	charlatans.
	
	Rich/rerat
	
	(RRR News) (message rule)
	((Previous Text Snipped to Save Bandwidth When Appropriate))
	
	<QUOTE PREVIOUSPOST="
	Timothy Daniels (TDani...@NoSpamDot.com) wrote in message
	
	Dell is involved in a class action suit for
	''bait and switch'', where a nurse claims Dell
	switched parts and charged her for the more
	expensive items, and for promising ''easy credit''
	for which no one qualifies and then charges
	ridiculously high interest rates.
	http://money.cnn.com/2005/02/2 2/technology/dell_lawsuit.reut /
	http://www.lerachlaw.com/lcsr- cgi-bin/mil?templ=featured/del l.html
	*TimDaniels
	
	"/>
	
	</POST>
	<POST>
	
		 */
		return null;
	}
	
	private static boolean isWeblog(Element root) {
		return root.getChild("DOCTYPE").getAttribute("SOURCE").getValue().equals("weblog");
	}
	
	private static List<ACESourceDocument> getWeblogSource(Element root) {
		/*
		 * <DOC>
	<DOCID> AGGRESSIVEVOICEDAILY_20041218.1004 </DOCID>
	<DOCTYPE SOURCE="weblog"> WEB TEXT </DOCTYPE>
	<DATETIME> 2004-12-18T10:04:00 </DATETIME>
	<BODY>
	<HEADLINE>
	Woman Charged With Murder, Kidnapping Fetus-Thingy
	</HEADLINE>
	<TEXT>
	<POST>
	<POSTER> Scott </POSTER>
	<POSTDATE> 2004-12-18T10:04:00 </POSTDATE>
	From the Associated Press : A baby girl who had been cut out of her
	
		 */
		return null;
	}
	
	private static Pair<Element, String> getDocumentRootElementAndString(File xmlFile) {
		BufferedReader r = FileUtil.getFileReader(xmlFile.getAbsolutePath());
		StringBuilder xmlStr = new StringBuilder();
		StringBuilder fullStr = new StringBuilder();
		try {
			String line = null;
			while ((line = r.readLine()) != null) {
				if (!line.startsWith("<!DOCTYPE"))
					xmlStr.append(line).append("\n");
				fullStr.append(line).append("\n");
			}
			
			r.close();
		} catch (IOException e1) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			System.exit(1);
		}
		
		SAXBuilder builder = new SAXBuilder();
		Document xml = null;
		try {
			xml = builder.build(new StringReader(xmlStr.toString()));
		} catch (JDOMException e) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Failed to read file " + xmlFile.getAbsolutePath());
			e.printStackTrace();
			System.exit(1);
		}
		
		return new Pair<Element, String>(xml.getRootElement(), fullStr.toString());
	}
	
	private static Map<String, Pair<File, File>> getInputFiles(String[] inputDirPaths) {
		Map<String, Pair<File, File>> files = new HashMap<>();
		for (String inputDirPath : inputDirPaths) {
			File inputDir = new File(inputDirPath);
			File[] inputDirFiles = inputDir.listFiles();
			for (File file : inputDirFiles) {
				String fileName = file.getName();
				if (fileName.endsWith(".apf.xml")) {
					String name = fileName.substring(0, fileName.length() - ".apf.xml".length());
					if (!files.containsKey(name))
						files.put(name, new Pair<File, File>(null, file));
					else
						files.get(name).setSecond(file);
				} else if (fileName.endsWith(".sgm")) {
					String name = fileName.substring(0, fileName.length() - ".sgm".length());
					if (!files.containsKey(name))
						files.put(name, new Pair<File, File>(file, null));
					else
						files.get(name).setFirst(file);
				}
			}
		}
		
		return files;
	}
	
	private static Map<String, Set<String>> summarizeAnnotations(File annotationFile, Map<String, Set<String>> summary) {
		Pair<Element, String> rootAndStr = getDocumentRootElementAndString(annotationFile);
		Element rootElement = rootAndStr.getFirst();
		
		Stack<Pair<String, Object>> toVisit = new Stack<>();
		toVisit.add(new Pair<String, Object>("", rootElement));
		
		while (!toVisit.isEmpty()) {
			Pair<String, Object> curPair = toVisit.pop();
			String parent = curPair.getFirst();
			Object o = curPair.getSecond();
			if (o instanceof Element) {
				Element element = (Element)o;
				String name = parent + " " + element.getName() + " (element)";
				if (!summary.containsKey(name))
					summary.put(name, new HashSet<String>());
				Set<String> childNames = summary.get(name);
				
				List<Attribute> attributes = element.getAttributes();
				for (Attribute attribute : attributes) {
					childNames.add(attribute.getName() + " (attribute)");
					toVisit.add(new Pair<String, Object>(element.getName(), attribute));
				}
				
				List<Element> children = element.getChildren();
				for (Element child : children) {
					childNames.add(child.getName() + " (element)");					
					toVisit.add(new Pair<String, Object>(element.getName(), child));
				}
				
			} else {
				Attribute attribute = (Attribute)o;
				String name = parent + " " + attribute.getName() + " (attribute)";
				if (!summary.containsKey(name))
					summary.put(name, new HashSet<String>());
				summary.get(name).add(attribute.getValue() + " (value)");
			}
		}
		
		return summary;
	}
	
	private static List<Element> traverse(Element root) {
		Stack<Element> toVisit = new Stack<Element>();
		toVisit.push(root);
		List<Element> traversal = new ArrayList<Element>();
		while (!toVisit.isEmpty()) {
			Element current = toVisit.pop();
			traversal.add(root);
			List<Element> children = current.getChildren();
			for (Element child : children) {
				traversal.add(child);
			}
		}
		
		return traversal;
	}
	
	private static Map<Element, Integer> getCharOffsets(String docString, List<Element> elements) {
		Map<Element, Integer> charOffsets = new HashMap<Element, Integer>();
		char[] chars = docString.toCharArray();
		boolean inTag = false;
		boolean inQuote = false;
		int curOffset = 0;
		char prevChar = 0;
		int tagIndex = -1;
		for (int i = 0; i < chars.length; i++) {
			if (inTag) {
				if (chars[i] == '"') {
					inTag = false;
					inQuote = true;
				} else if (chars[i] == '>') {
					inTag = false;
					charOffsets.put(elements.get(tagIndex), curOffset);
				}
			} else if (inQuote) {
				if (chars[i] == '"') {
					inQuote = false;
					inTag = true;
				}
			} else {
				if (chars[i] == '<' && prevChar != '\\' && (i + 1 == chars.length || !Character.isWhitespace(chars[i + 1]))) {
					inTag = true;
					tagIndex++;
				} else {
					curOffset++;
				}
			}
			
			prevChar = chars[i];
		}
		
		return charOffsets;
	}
}

package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.tirix.emetamath.editors.MMPartitionScanner;
import org.tirix.emetamath.editors.MMWhitespaceDetector;
import org.tirix.emetamath.editors.proofassistant.MMPScanner.MMLabelDetector;

public class MMPPartitionScanner extends MMPartitionScanner {
	public static final String MM_KEYWORD = "__mmKeyword";
	public static final String MM_LABEL_LIST = "__mmLabelList";

	public MMPPartitionScanner() {
		IToken mmComment = new Token(MM_COMMENT);
		IToken mmLabelList = new Token(MM_LABEL_LIST);
		IToken mmText = new Token(IDocument.DEFAULT_CONTENT_TYPE);

		// Don't enable this, otherwise one partition per character is created!
		//setDefaultReturnToken(mmText);
		
		IPredicateRule[] rules = new IPredicateRule[2];

		rules[0] = new MultiLineRule("$(", "$)", mmComment);
		rules[1] = new MMPPartitionRule(mmLabelList);

		setPredicateRules(rules);
	}

	public static class MMPPartitionRule implements IPredicateRule {
		IToken labelListToken;
		IWordDetector labelDetector;
		MMWhitespaceDetector whiteSpaceDetector;
		
		public MMPPartitionRule(IToken labelListToken) {
			this.labelListToken = labelListToken;
			this.labelDetector = new MMLabelDetector();
			this.whiteSpaceDetector = new MMWhitespaceDetector();
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			if(resume) {
				int column = scanner.getColumn();
				for(int i=0;i<column;i++) scanner.unread();
			}
			return evaluate(scanner);
		}

		@Override
		public IToken getSuccessToken() {
			return labelListToken;
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			if(scanner.getColumn() != 0) return Token.UNDEFINED;
			int c = scanner.read();
			if(!labelDetector.isWordPart((char) c)) return Token.UNDEFINED;

			// search the first semicolon
			// TODO check that we don't change lines ?
			do { c = scanner.read(); } while(c != ':' && c != EOF);
			if(c == EOF) return Token.EOF;

			// search the second semicolon
			do { c = scanner.read(); } while(c != ':' && c != EOF);
			if(c == EOF) return Token.EOF;

			// allow for any number of white spaces
			do { c = scanner.read(); } while(whiteSpaceDetector.isWhitespace((char) c));
			if(c == EOF) return Token.EOF;
			
			// search the end of the last label
			do { c = scanner.read(); } while(labelDetector.isWordPart((char) c));
			if(c == EOF) return Token.EOF;
			
			return labelListToken;
		}
		
	}
}

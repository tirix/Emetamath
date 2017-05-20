package org.tirix.emetamath.editors.proofassistant;

import java.util.Arrays;
import java.util.Comparator;

import mmj.pa.PaConstants;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.Token;
import org.tirix.emetamath.editors.MMPartitionScanner;
import org.tirix.emetamath.editors.MMWhitespaceDetector;
import org.tirix.emetamath.editors.proofassistant.MMPScanner.MMLabelDetector;

public class MMPPartitionScanner extends MMPartitionScanner {
	public static final String MMP_HEADER = "__mmpHeader";
	public static final String MMP_LABEL_LIST = "__mmpLabelList";
	public static final String MMP_PROOF = "__mmpProof";
	public static final String MMP_DISJOINT = "__mmpDisjoint";
	public static final String MMP_COMMENT = "__mmpComment";

	public MMPPartitionScanner() {
		IToken mmComment = new Token(MMP_COMMENT);
		IToken mmLabelList = new Token(MMP_LABEL_LIST);
		IToken mmHeader = new Token(MMP_HEADER);
		IToken mmProof = new Token(MMP_PROOF);
		IToken mmDisjoint = new Token(MMP_DISJOINT);
		IToken mmText = new Token(IDocument.DEFAULT_CONTENT_TYPE);

		// Don't enable this, otherwise one partition per character is created!
		//setDefaultReturnToken(mmText);
		
		IPredicateRule[] rules = new IPredicateRule[4];

		rules[0] = new MMPHeaderRule(mmHeader);
		rules[1] = new MMPCommentRule(mmComment);
		rules[2] = new MMPProofRule(mmProof);
		rules[3] = new MMPLabelListRule(mmLabelList);

		setPredicateRules(rules);
	}

	public static abstract class MMPPartitionRule extends PatternRule {
		protected IWordDetector labelDetector;
		protected MMWhitespaceDetector whiteSpaceDetector;
		protected char[][] fSortedLineDelimiters;
		
		protected MMPPartitionRule(IToken token) {
			super("*", "", token, (char) 0, true); // some dummy, as we only want to reuse some utility functions
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

		/**
		 * @param scanner
		 */
		protected IToken detectFirstToken(ICharacterScanner scanner, String token) {
			int c = scanner.read();
			scanner.unread();

			if(c != token.charAt(0)) return Token.UNDEFINED;
			if(scanner.getColumn() != 0) return Token.UNDEFINED;

			c = scanner.read();
			if(!sequenceDetected(scanner, token.toCharArray(), true)) {
				scanner.unread();
				return Token.UNDEFINED;
				}
			return fToken;
		}

		public IToken detectSequence(ICharacterScanner scanner, String sequence) {
			int c = scanner.read();

			if(c != sequence.charAt(0)) {
				scanner.unread();
				return Token.UNDEFINED;
			}
			if(!sequenceDetected(scanner, sequence.toCharArray(), true)) {
				scanner.unread();
				return Token.UNDEFINED;
				}
			return fToken;
		}
		
		public IToken skipWhiteSpaces(ICharacterScanner scanner) {
			int c;
			do { c = scanner.read(); } while(whiteSpaceDetector.isWhitespace((char) c));
			if(c == EOF) return Token.EOF;
			scanner.unread();
			return Token.WHITESPACE;
		}
		
		public IToken skipLabel(ICharacterScanner scanner) {
			int c;
			do { c = scanner.read(); } while(labelDetector.isWordPart((char) c));
			if(c == EOF) return Token.EOF;
			scanner.unread();
			return fToken;
		}
		
		protected void buildEOLSequence(ICharacterScanner scanner) {
			if(fSortedLineDelimiters == null) {
				char[][] lineDelimiters = scanner.getLegalLineDelimiters();
				int count= lineDelimiters.length;
				fSortedLineDelimiters= new char[count][];

				if (count != 0) {
					Comparator<char[]> fLineDelimiterComparator= new Comparator<char[]>() {
						public int compare(char[] o1, char[] o2) {
							return (o2).length - (o1).length;
						}
					};
					System.arraycopy(lineDelimiters, 0, fSortedLineDelimiters, 0, lineDelimiters.length);
					Arrays.sort(fSortedLineDelimiters, fLineDelimiterComparator);
				}
			}
		}
	}
	
	public static class MMPLabelListRule extends MMPPartitionRule {
		IToken labelListToken;
		
		public MMPLabelListRule(IToken labelListToken) {
			super(labelListToken);
			this.labelListToken = labelListToken;
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

	public static class MMPCommentRule extends MMPPartitionRule {
		public final static int COMMENT_CHAR = '*';
		IToken commentToken;
		
		public MMPCommentRule(IToken commentToken) {
			super(commentToken);
			this.commentToken = commentToken;
			}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			scanner.unread();

			// we always start with the '*', and at the beginning of a line
			if(c != COMMENT_CHAR) return Token.UNDEFINED;
			if(scanner.getColumn() != 0) return Token.UNDEFINED;

			buildEOLSequence(scanner);

			do {
				// search the next line not starting with whitespace
				boolean eol = false;
				do { c = scanner.read(); 
					// Check for end of line
					for (int i= 0; i < fSortedLineDelimiters.length; i++) {
						if (c == fSortedLineDelimiters[i][0] && sequenceDetected(scanner, fSortedLineDelimiters[i], true))
							eol =true;
					}
				} while(!eol && c != EOF);
				if(c == EOF) return Token.EOF;
				c = scanner.read();
			} while(whiteSpaceDetector.isWhitespace((char) c));
			if(c == EOF) return Token.EOF;
			scanner.unread();
			return commentToken;
		}
	}

	public static class MMPHeaderRule extends MMPPartitionRule {
		IToken headerToken;

		public MMPHeaderRule(IToken headerToken) {
			super(headerToken);
			this.headerToken = headerToken;
			}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			if(detectFirstToken(scanner, PaConstants.HEADER_STMT_TOKEN) == Token.UNDEFINED) {
				return detectFirstToken(scanner, "$)");
			}

			// skip any number of white spaces
			if(skipWhiteSpaces(scanner) == Token.EOF) return Token.EOF;
			
			// detect the <MM> marker
			if(detectSequence(scanner, PaConstants.HEADER_MM_TOKEN) == Token.UNDEFINED) return Token.UNDEFINED;

			// skip any number of white spaces
			if(skipWhiteSpaces(scanner) == Token.EOF) return Token.EOF;
			
			// detect the <PROOF_ASST> marker
			if(detectSequence(scanner, PaConstants.HEADER_PROOF_ASST_TOKEN) == Token.UNDEFINED) return Token.UNDEFINED;

			// skip any number of white spaces
			if(skipWhiteSpaces(scanner) == Token.EOF) return Token.EOF;
			
			// detect the THEOREM= marker
			if(detectSequence(scanner, PaConstants.HEADER_THEOREM_EQUAL_PREFIX) == Token.UNDEFINED) return Token.UNDEFINED;

			// skip the theorem name label
			if(skipLabel(scanner) == Token.EOF) return Token.EOF;
			
			// skip any number of white spaces
			if(skipWhiteSpaces(scanner) == Token.EOF) return Token.EOF;
			
			// detect the LOC_AFTER= marker
			if(detectSequence(scanner, PaConstants.HEADER_LOC_AFTER_EQUAL_PREFIX) == Token.UNDEFINED) return Token.UNDEFINED;

			// skip the theorem name label
			if(skipLabel(scanner) == Token.EOF) return Token.EOF;
			if(scanner.read() != '?') { scanner.unread(); }
			
			return headerToken;
		}
	}

	public static class MMPProofRule extends MMPPartitionRule {
		IToken proofToken;

		public MMPProofRule(IToken proofToken) {
			super(proofToken);
			this.proofToken = proofToken;
			}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c;
			if(detectFirstToken(scanner, PaConstants.GENERATED_PROOF_STMT_TOKEN) == Token.UNDEFINED) return Token.UNDEFINED;

			do {
			
				// skip any number of white spaces
				if(skipWhiteSpaces(scanner) == Token.EOF) return Token.EOF;
				
				// skip the theorem name label
				if(skipLabel(scanner) == Token.EOF) return Token.EOF;
			
				c = scanner.read();
				scanner.unread();
			} while(c != PaConstants.END_PROOF_STMT_TOKEN.charAt(0));

			if(detectSequence(scanner, PaConstants.END_PROOF_STMT_TOKEN)  == Token.UNDEFINED) 
				return Token.UNDEFINED;

			return proofToken;
		}
	}
}

package org.tirix.emetamath.editors;

/**
 * Region Provider for Metamath text
 * 
 * This provides the region selected when double-clicking.
 */

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.tirix.emetamath.editors.MMScanner.MMLabelDetector;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;
import org.tirix.emetamath.editors.proofassistant.MMPScanner;

public class MMRegionProvider {
	final static MMTokenDetector DETECTOR = new MMTokenDetector();
	final static MMLabelDetector LABEL_DETECTOR = new MMLabelDetector();
	final static MMWhitespaceDetector WHITESPACE_DETECTOR = new MMWhitespaceDetector();
	
	// TODO optimize those methods, they are all similar
	public static IRegion getComment(IDocument doc, int offset) {
		int startPos, endPos;

		try {
			int pos = offset;
			char c1 = ' ';
			char c2 = ' ';

			while (pos >= 0) {
				c2 = c1;
				c1 = doc.getChar(pos);
				if (c1 == '$' && c2 == '(') {
					break;
				}
				if (c1 == '$' && c2 == ')') {
					return null;
				}
				--pos;
			}
			startPos = pos - 1;
			if(startPos == -2) return null;
			
			pos = offset;
			int length = doc.getLength();
			c1 = ' ';
			c2 = ' ';

			while (pos < length) {
				c1 = c2;
				c2 = doc.getChar(pos);
				if (c1 == '$' && c2 == ')')
					break;
				++pos;
			}
			endPos = pos + 1;
			if(endPos == length + 1) return null;

			return new Region(startPos + 1, endPos - startPos - 1);
		} catch (BadLocationException x) {
		}

		return null;
	}

	public static IRegion getWord(IDocument doc, int offset) {
		int startPos, endPos;

		try {

			int pos = offset;
			char c;

			while (pos >= 0) {
				c = doc.getChar(pos);
				if (!DETECTOR.isWordPart(c))
					break;
				--pos;
			}

			startPos = pos;

			pos = offset;
			int length = doc.getLength();

			while (pos < length) {
				c = doc.getChar(pos);
				if (!DETECTOR.isWordPart(c))
					break;
				++pos;
			}

			endPos = pos;
			return new Region(startPos + 1, endPos - startPos - 1);

		} catch (BadLocationException x) {
		}

		return null;
	}

	public static IRegion getLabel(IDocument doc, int offset) {
		int startPos, endPos;

		try {

			int pos = offset;
			char c;

			while (pos >= 0) {
				c = doc.getChar(pos);
				if (!LABEL_DETECTOR.isWordPart(c))
					break;
				--pos;
			}

			startPos = pos;

			pos = offset;
			int length = doc.getLength();

			while (pos < length) {
				c = doc.getChar(pos);
				if (!LABEL_DETECTOR.isWordPart(c))
					break;
				++pos;
			}

			endPos = pos;
			return new Region(startPos + 1, endPos - startPos - 1);

		} catch (BadLocationException x) {
		}

		return null;
	}

	public static IRegion getStatement(IDocument doc, int offset) {
		int startPos, endPos;

		try {
			int pos = offset;
			char c1 = ' ';
			char c2 = ' ';

			while (pos >= 0) {
				c2 = c1;
				c1 = doc.getChar(pos);
				if (c1 == '$' && (c2 == 'a' || c2 == 'e' || c2 == 'p')) {
					break;
				}
				if (c1 == '$' && (c2 == '.' || c2 == '=')) {
					return null;
				}
				--pos;
			}
			startPos = pos - 1;
			if(startPos == -2) return null;
			
			pos = offset;
			int length = doc.getLength();
			c1 = ' ';
			c2 = ' ';

			while (pos < length) {
				c1 = c2;
				c2 = doc.getChar(pos);
				if (c1 == '$' && (c2 == '.' || c2 == '='))
					break;
				++pos;
			}
			endPos = pos + 1;
			if(endPos == length + 1) return null;

			return new Region(startPos + 1 + 2, endPos - startPos - 1 - 4);
		} catch (BadLocationException x) {
		}

		return null;
	}

	public static IRegion getMMPComment(IDocument doc, int offset) {
		int startPos, endPos;
	
		try {
			int pos = offset;
			char c1 = ' ';
			char c2 = ' ';
	
			while (pos >= 0) {
				c2 = c1;
				c1 = doc.getChar(pos);
				if (c1 == '\n' && c2 == '*') {
					break;
				}
				if (c1 == '\n' && !WHITESPACE_DETECTOR.isWhitespace(c2)) {
					return null;
				}
				--pos;
			}
			startPos = pos - 1;
			if(startPos == -2) return null;
			
			pos = offset;
			int length = doc.getLength();
			c1 = ' ';
			c2 = ' ';
	
			while (pos < length) {
				c1 = c2;
				c2 = doc.getChar(pos);
				if (c1 == '\n' && !WHITESPACE_DETECTOR.isWhitespace(c2))
					break;
				++pos;
			}
			endPos = pos - 1;
			return new Region(startPos + 1, endPos - startPos - 1);
		} catch (BadLocationException x) {
		}
	
		return null;
	}

	public static IRegion getMMPStep(IDocument doc, int offset) {
		int startPos, endPos;
	
		try {
			int pos = offset;
			char c1 = ' ';
			char c2 = ' ';
	
			while (pos >= 0) {
				c2 = c1;
				c1 = doc.getChar(pos);
				if (c1 == '\n' && !WHITESPACE_DETECTOR.isWhitespace(c2)) {
					break;
				}
				--pos;
			}
			startPos = pos;
			if(startPos == -1) return null;
			
			pos = offset;
			int length = doc.getLength();
			c1 = ' ';
			c2 = ' ';
	
			while (pos < length) {
				c1 = c2;
				c2 = doc.getChar(pos);
				if (c1 == '\n' && !WHITESPACE_DETECTOR.isWhitespace(c2))
					break;
				++pos;
			}
			endPos = pos - 1;
			return new Region(startPos + 1, endPos - startPos - 1);
		} catch (BadLocationException x) {
		}
	
		return null;
	}
	
	public static IRegion getMMPStatement(IDocument doc, int offset) {
		IRegion stepRegion = getMMPStep(doc, offset);
		MMPScanner scanner = new MMPScanner();
		scanner.setRange(doc, stepRegion.getOffset(), stepRegion.getLength());
		IToken token;
		do { token = scanner.nextToken(); } while(token != Token.EOF && token != MMPScanner.TOKEN_FORMULA);
		int pos = scanner.getTokenOffset();
		return new Region(pos, stepRegion.getOffset() + stepRegion.getLength() - pos);
	}
}
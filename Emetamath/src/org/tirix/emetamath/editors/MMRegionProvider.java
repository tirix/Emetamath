package org.tirix.emetamath.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;

public class MMRegionProvider {
	final static MMTokenDetector DETECTOR = new MMTokenDetector();
	
	public static IRegion getComment(ITextViewer textViewer, int offset) {
		IDocument doc = textViewer.getDocument();
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

			return new Region(startPos + 1, endPos - startPos - 1);
		} catch (BadLocationException x) {
		}

		return null;
	}

	public static IRegion getWord(ITextViewer textViewer, int offset) {
		IDocument doc = textViewer.getDocument();
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
}

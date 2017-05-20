package org.tirix.emetamath.editors;

import org.eclipse.jface.text.*;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;

public class MMDoubleClickStrategy implements ITextDoubleClickStrategy {
	final static MMTokenDetector DETECTOR = new MMTokenDetector();
	protected ITextViewer fText;

	public void doubleClicked(ITextViewer part) {
		int pos = part.getSelectedRange().x;

		if (pos < 0)
			return;

		fText = part;

		if (!selectComment(pos)) {
			selectWord(pos);
		}
	}

	protected boolean selectWord(int caretPos) {
		IRegion region = MMRegionProvider.getWord(fText, caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}

	protected boolean selectComment(int caretPos) {
		IRegion region = MMRegionProvider.getComment(fText, caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}
}
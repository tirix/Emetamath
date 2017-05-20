package org.tirix.emetamath.editors;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;

public class MMDoubleClickStrategy implements ITextDoubleClickStrategy {
	final static MMTokenDetector DETECTOR = new MMTokenDetector();
	protected ITextViewer fText;
	
	public void doubleClicked(ITextViewer part) {
		int offset = part.getSelectedRange().x;
		if (offset < 0)
			return;

		fText = part;

		if(!selectComment(offset)) {
			selectWord(offset);
		}
	}

	protected boolean selectLabel(int caretPos) {
		IRegion region = MMRegionProvider.getLabel(fText.getDocument(), caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}

	protected boolean selectWord(int caretPos) {
		IRegion region = MMRegionProvider.getWord(fText.getDocument(), caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}

	protected boolean selectComment(int caretPos) {
		IRegion region = MMRegionProvider.getComment(fText.getDocument(), caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}
}
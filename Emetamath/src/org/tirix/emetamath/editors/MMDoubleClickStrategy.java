package org.tirix.emetamath.editors;

import org.eclipse.jface.text.*;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;
import org.tirix.emetamath.editors.proofassistant.MMPPartitionScanner;

public class MMDoubleClickStrategy implements ITextDoubleClickStrategy {
	final static MMTokenDetector DETECTOR = new MMTokenDetector();
	protected ITextViewer fText;
	
	public void doubleClicked(ITextViewer part) {
		int offset = part.getSelectedRange().x;
		String contentType = null;
		try {
			ITypedRegion partition = part.getDocument().getPartition(offset);
			contentType = partition.getType();
		} catch (BadLocationException e) {
			;
		}
		if (offset < 0)
			return;

		fText = part;

		if(MMPPartitionScanner.MM_LABEL_LIST.equals(contentType))
			selectLabel(offset);
		else if (!selectComment(offset)) {
			selectWord(offset);
		}
	}

	protected boolean selectLabel(int caretPos) {
		IRegion region = MMRegionProvider.getLabel(fText, caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
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
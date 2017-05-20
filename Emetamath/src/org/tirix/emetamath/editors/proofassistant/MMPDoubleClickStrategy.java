package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.tirix.emetamath.editors.MMDoubleClickStrategy;
import org.tirix.emetamath.editors.MMRegionProvider;

//TODO too sad, we could have one strategy per contentType, and here we compute again the content type...
public class MMPDoubleClickStrategy extends MMDoubleClickStrategy {
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

		if(MMPPartitionScanner.MMP_LABEL_LIST.equals(contentType)
				|| MMPPartitionScanner.MMP_HEADER.equals(contentType)
				|| MMPPartitionScanner.MMP_PROOF.equals(contentType)
				)
			selectLabel(offset);
		else if(MMPPartitionScanner.MMP_COMMENT.equals(contentType)) {
			selectMMPComment(offset);
		}
		else 
			selectWord(offset);
	}

	protected boolean selectMMPComment(int caretPos) {
		IRegion region = MMRegionProvider.getMMPComment(fText.getDocument(), caretPos);
		if(region != null) {
			fText.setSelectedRange(region.getOffset(), region.getLength());
			return true;
		}
		return false;
	}
}
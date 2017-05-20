package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tirix.eclipse.FastPresentationReconciler;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.IMMColorConstants;
import org.tirix.emetamath.editors.MMPartitionScanner;
import org.tirix.emetamath.editors.MMRegionProvider;
import org.tirix.emetamath.editors.MMSourceViewerConfiguration;
import org.tirix.emetamath.editors.NonRuleBasedDamagerRepairer;
import org.tirix.emetamath.editors.MMSourceViewerConfiguration.MetamathTextHover;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofViewerConfiguration extends MMSourceViewerConfiguration {
	private MMPScanner mmpScanner;
	private MMPHeaderScanner mmpHeaderScanner;
	private MMPProofScanner mmpProofScanner;
	
	public ProofViewerConfiguration(ITextEditor textEditor, ColorManager colorManager) {
		super(textEditor, colorManager);
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			MMPPartitionScanner.MMP_HEADER,
			MMPPartitionScanner.MMP_COMMENT,
			MMPPartitionScanner.MMP_PROOF,
			MMPPartitionScanner.MMP_LABEL_LIST,
			};
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(
			ISourceViewer sourceViewer,
			String contentType) {
			if (doubleClickStrategy == null)
				doubleClickStrategy = new MMPDoubleClickStrategy();
			return doubleClickStrategy;
		}

	protected MMPScanner getMMPScanner() {
		if (mmpScanner == null) {
			mmpScanner = new MMPScanner(MetamathProjectNature.getNature(fTextEditor.getEditorInput()), colorManager);
		}
		return mmpScanner;
	}

	protected MMPHeaderScanner getMMPHeaderScanner() {
		if (mmpHeaderScanner == null) {
			mmpHeaderScanner = new MMPHeaderScanner(MetamathProjectNature.getNature(fTextEditor.getEditorInput()), colorManager);
		}
		return mmpHeaderScanner;
	}

	protected MMPProofScanner getMMPProofScanner() {
		if (mmpProofScanner == null) {
			mmpProofScanner = new MMPProofScanner(MetamathProjectNature.getNature(fTextEditor.getEditorInput()), colorManager);
		}
		return mmpProofScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		FastPresentationReconciler reconciler = (FastPresentationReconciler) super.getPresentationReconciler(sourceViewer);
		DefaultDamagerRepairer dr;

		dr= new DefaultDamagerRepairer(getMMPScanner());
		reconciler.setDamager(dr, MMPPartitionScanner.MMP_LABEL_LIST);
		reconciler.setRepairer(dr, MMPPartitionScanner.MMP_LABEL_LIST);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(getMMPHeaderScanner());
		reconciler.setDamager(dr, MMPPartitionScanner.MMP_HEADER);
		reconciler.setRepairer(dr, MMPPartitionScanner.MMP_HEADER);

		dr= new DefaultDamagerRepairer(getMMPProofScanner());
		reconciler.setDamager(dr, MMPPartitionScanner.MMP_PROOF);
		reconciler.setRepairer(dr, MMPPartitionScanner.MMP_PROOF);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IMMColorConstants.COMMENT)));
		reconciler.setDamager(ndr, MMPPartitionScanner.MMP_COMMENT);
		reconciler.setRepairer(ndr, MMPPartitionScanner.MMP_COMMENT);
		
		return reconciler;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if(!contentType.equals(MMPPartitionScanner.MMP_COMMENT)) {
			return new MMPTextHover(MetamathProjectNature.getNature(fTextEditor.getEditorInput()));
			}
		return null;
	}

	protected static final class MMPTextHover extends MetamathTextHover {
		public MMPTextHover(MetamathProjectNature nature2) {
			super(nature2);
		}

		@Override
		public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
			String contentType = null;
			try {
				contentType = textViewer.getDocument().getPartition(offset).getType();
			} 
			catch (BadLocationException e) { }
			if(MMPPartitionScanner.MMP_LABEL_LIST.equals(contentType)
					|| MMPPartitionScanner.MMP_HEADER.equals(contentType)
					|| MMPPartitionScanner.MMP_PROOF.equals(contentType)
					)
				return MMRegionProvider.getLabel(textViewer.getDocument(), offset);
			else
				return MMRegionProvider.getWord(textViewer.getDocument(), offset);
		}
	}
}
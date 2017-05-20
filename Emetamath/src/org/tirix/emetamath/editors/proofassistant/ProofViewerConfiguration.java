package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.IMMColorConstants;
import org.tirix.emetamath.editors.MMPartitionScanner;
import org.tirix.emetamath.editors.MMSourceViewerConfiguration;
import org.tirix.emetamath.editors.NonRuleBasedDamagerRepairer;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofViewerConfiguration extends MMSourceViewerConfiguration {
	private MMPScanner mmpScanner;
	
	public ProofViewerConfiguration(ITextEditor textEditor, ColorManager colorManager) {
		super(textEditor, colorManager);
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			MMPartitionScanner.MM_COMMENT,
			MMPPartitionScanner.MM_LABEL_LIST,
			};
	}

	protected MMPScanner getMMPScanner() {
		if (mmpScanner == null) {
			mmpScanner = new MMPScanner(MetamathProjectNature.getNature(fTextEditor.getEditorInput()), colorManager);
		}
		return mmpScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);
		DefaultDamagerRepairer dr;

		dr= new DefaultDamagerRepairer(getMMPScanner());
		reconciler.setDamager(dr, MMPPartitionScanner.MM_LABEL_LIST);
		reconciler.setRepairer(dr, MMPPartitionScanner.MM_LABEL_LIST);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IMMColorConstants.COMMENT)));
		reconciler.setDamager(ndr, MMPartitionScanner.MM_COMMENT);
		reconciler.setRepairer(ndr, MMPartitionScanner.MM_COMMENT);

		return reconciler;
	}
}
package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.MMSourceViewerConfiguration;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofViewerConfiguration extends MMSourceViewerConfiguration {
	private MMPScanner mmpScanner;
	
	public ProofViewerConfiguration(ITextEditor textEditor, ColorManager colorManager) {
		super(textEditor, colorManager);
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
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}
}
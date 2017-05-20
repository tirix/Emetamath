package org.tirix.emetamath.editors;

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
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofViewerConfiguration extends SourceViewerConfiguration {
	private MMDoubleClickStrategy doubleClickStrategy;
	private MMScanner mmScanner;
	private MMCommentScanner mmCommentScanner;
	private BufferedRuleBasedScanner mmFileInclusionScanner;
	private ColorManager colorManager;
	private ITextEditor fTextEditor;
	
	public ProofViewerConfiguration(ITextEditor textEditor, ColorManager colorManager) {
		this.colorManager = colorManager;
		this.fTextEditor = textEditor;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			MMPartitionScanner.MM_COMMENT,
			MMPartitionScanner.MM_TYPESETTING,
			MMPartitionScanner.MM_FILEINCLUSION,
			MMScanner.MM_KEYWORD,
			MMScanner.MM_CONSTANT, 
			MMScanner.MM_VARIABLE,
			MMScanner.MM_DISJOINT,
			MMScanner.MM_VARHYP,
			MMScanner.MM_LOGHYP,
			MMScanner.MM_AXIOM,
			MMScanner.MM_THEOREM
			};
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new MMDoubleClickStrategy();
		return doubleClickStrategy;
	}

	/**
	 * Single token scanner.
	 */
	static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	protected MMScanner getMMScanner() {
		if (mmScanner == null) {
			mmScanner = new MMScanner(MetamathProjectNature.getNature(fTextEditor.getEditorInput()), colorManager);
		}
		return mmScanner;
	}

	protected MMCommentScanner getMMCommentScanner() {
		if (mmCommentScanner == null) {
			mmCommentScanner = new MMCommentScanner(colorManager);
		}
		return mmCommentScanner;
	}

	protected ITokenScanner getMMFileInclusionScanner() {
		if (mmFileInclusionScanner == null) {
			mmFileInclusionScanner = new SingleTokenScanner(new TextAttribute(
						colorManager.getColor(IMMColorConstants.DEFAULT)));
		}
		return mmFileInclusionScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		DefaultDamagerRepairer dr;

		dr= new DefaultDamagerRepairer(getMMScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(getMMFileInclusionScanner());
		reconciler.setDamager(dr, MMPartitionScanner.MM_FILEINCLUSION);
		reconciler.setRepairer(dr, MMPartitionScanner.MM_FILEINCLUSION);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IMMColorConstants.COMMENT)));
		reconciler.setDamager(ndr, MMPartitionScanner.MM_COMMENT);
		reconciler.setRepairer(ndr, MMPartitionScanner.MM_COMMENT);

		return reconciler;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		IReconcilingStrategy reconcilingStrategy = new MMReconcilingStrategy(sourceViewer, fTextEditor);
		MonoReconciler reconciler = new MonoReconciler(reconcilingStrategy, false);
		reconciler.setProgressMonitor(new NullProgressMonitor());
		reconciler.setDelay(500);
		return reconciler;
	}
}
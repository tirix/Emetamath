package org.tirix.emetamath.editors;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tirix.eclipse.FastPresentationReconciler;
import org.tirix.emetamath.editors.MMScanner.MMProofScanner;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;
import org.tirix.emetamath.preferences.PreferenceConstants;
import org.tirix.emetamath.views.MMHTMLPrinter;

import mmj.lang.MObj;

// 
@SuppressWarnings("restriction")
public class MMSourceViewerConfiguration extends TextSourceViewerConfiguration implements SystemLoadListener {
	public static String EDITOR_FONT_REGISTRY_NAME = "org.tirix.emetamath.preferences.editorFont";
	protected MMDoubleClickStrategy doubleClickStrategy;
	protected MMScanner mmScanner;
	protected MMProofScanner mmProofScanner;
	protected MMCommentScanner mmCommentScanner;
	protected BufferedRuleBasedScanner mmFileInclusionScanner;
	protected ColorManager colorManager;
	protected ITextEditor fTextEditor;
	protected static Font fEditorFont;
	
	public MMSourceViewerConfiguration(ITextEditor textEditor, ColorManager colorManager) {
		this.colorManager = colorManager;
		this.fTextEditor = textEditor;
		FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
		MMSourceViewerConfiguration.fEditorFont = fontRegistry.get(EDITOR_FONT_REGISTRY_NAME);
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			MMPartitionScanner.MM_COMMENT,
			MMPartitionScanner.MM_PROOF,
			MMPartitionScanner.MM_TYPESETTING,
			MMPartitionScanner.MM_FILEINCLUSION,
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

	protected MMScanner getMMProofScanner() {
		if (mmProofScanner == null) {
			mmProofScanner = new MMProofScanner(colorManager);
		}
		return mmProofScanner;
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
		FastPresentationReconciler reconciler = new FastPresentationReconciler();
		DefaultDamagerRepairer dr;

		dr= new DefaultDamagerRepairer(getMMScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(getMMFileInclusionScanner());
		reconciler.setDamager(dr, MMPartitionScanner.MM_FILEINCLUSION);
		reconciler.setRepairer(dr, MMPartitionScanner.MM_FILEINCLUSION);

		dr = new DefaultDamagerRepairer(getMMProofScanner());
		reconciler.setDamager(dr, MMPartitionScanner.MM_PROOF);
		reconciler.setRepairer(dr, MMPartitionScanner.MM_PROOF);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IMMColorConstants.COMMENT)));
		reconciler.setDamager(ndr, MMPartitionScanner.MM_COMMENT);
		reconciler.setRepairer(ndr, MMPartitionScanner.MM_COMMENT);

		return reconciler;
	}
	
//	/*
//	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
//	 */
//	public IReconciler getReconciler(ISourceViewer sourceViewer) {
//		IReconcilingStrategy reconcilingStrategy = new MMReconcilingStrategy(sourceViewer, fTextEditor);
//		MonoReconciler reconciler = new MonoReconciler(reconcilingStrategy, false);
//		reconciler.setProgressMonitor(new NullProgressMonitor());
//		reconciler.setDelay(500);
//		return reconciler;
//	}
	
	@Override
	public int getTabWidth(ISourceViewer sourceViewer) {
		if(fPreferenceStore != null) return fPreferenceStore.getInt(PreferenceConstants.P_EDITOR_SPACES_FOR_TABS);
		return 2;
	}
	
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if(!contentType.equals(MMPartitionScanner.MM_COMMENT)) {
			MetamathProjectNature nature = MetamathProjectNature.getNature(fTextEditor.getEditorInput());
			if(nature != null) return new MetamathTextHover(nature);
			}
		return null;
	}
	
	@Override
	public void systemLoaded() {
		// TODO Auto-generated method stub
		
	}

	public static class MetamathTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {
		private MetamathProjectNature nature;
		private IInformationControlCreator creator;

		
		public MetamathTextHover(MetamathProjectNature nature2) {
			this.nature = nature2;
		}

		@Override
		public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
			try {
				String text = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
				if(!nature.isLogicalSystemLoaded()) return null;
				MObj mobj = nature.getMObj(text);
				if(mobj == null) return null;
				// TODO find a way to store the comment immediately following a constant as the constant's description (provided there is no line feed) 
				return MMHTMLPrinter.printMObj(nature, mobj);
			} catch (BadLocationException e) {
				return null;
			}
		}

		@Override
		public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
			return MMRegionProvider.getWord(textViewer.getDocument(), offset);
		}

		@Override
		public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
			return getHoverInfo(textViewer, hoverRegion);
		}

		@Override
		public IInformationControlCreator getHoverControlCreator() {
			if(creator == null) creator = new AbstractReusableInformationControlCreator() {
				@Override
				protected IInformationControl doCreateInformationControl(Shell parent) {
					if(BrowserInformationControl.isAvailable(parent)) { 
						// see also the variant with tool bar
						BrowserInformationControl iControl = new BrowserInformationControl(parent, EDITOR_FONT_REGISTRY_NAME, true) {
							@Override
							public IInformationControlCreator getInformationPresenterControlCreator() {
								return creator;
								}
							@Override
							public Point computeSizeHint() {
								Point size = super.computeSizeHint();
								size.y += 50;
								return size;
								}
							
							@Override
							public Point computeSizeConstraints(int widthInChars, int heightInChars) {
								Point size = super.computeSizeConstraints(widthInChars, heightInChars);
								size.y += 50;
								return size;
								}
							};
							// cannot access the methods that would allow us to set the hover information size constraints (protected or private access)
							// TextViewer.getTextHoveringController().setSizeConstraints(widthInChar, heightInChar, enforceAsMinimalSize, enforceAsMaximalSize);
							// AbstractTextEditor.fInformationPresenter.setSizeConstraints(widthInChar, heightInChar, enforceAsMinimalSize, enforceAsMaximalSize);
							return iControl;
						}
					return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
				}
			};
			return creator;
		}
	}
}
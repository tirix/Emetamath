package org.tirix.emetamath.editors;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewerExtension8.EnrichMode;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;
import org.tirix.emetamath.popup.actions.NextLevelFormulaAction;
import org.tirix.emetamath.preferences.PreferenceConstants;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.ProofExplorerView;

import mmj.lang.Stmt;
import mmj.lang.Sym;

@SuppressWarnings("restriction")
public class MetamathEditor extends TextEditor implements IShowInSource {
	public static final String EDITOR_ID = "org.tirix.emetamath.MetamathEditor";
	protected final static String[] BRACKETS= { "$(", "$)", "${", "$}", "$[", "$]", "$t", "$)", "$d", "$.", "$a", "$.", "$e", "$.", "$f", "$.", "$p", "$.", "{", "}", "(", ")", "[", "]", "<.", ">." };

	//	protected MMContentOutlinePage fOutlinePage;
	/** The editor's bracket matcher */
	protected MetamathPairMatcher fBracketMatcher= new MetamathPairMatcher(BRACKETS);
	
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new MMSourceViewerConfiguration(this, new ColorManager()));
		setEditorContextMenuId(EDITOR_ID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// TODO here, we are overriding the property PREFERENCE_HOVER_ENRICH_MODE - find a way to revert to default behavior, ex. by controlling the AbstractHoverInformationControlManager.setInformationControlReplacer()  
		((TextViewer)getSourceViewer()).setHoverEnrichMode(EnrichMode.ON_CLICK);

		//setTabWidthPreference();
		//showMargin();
	}

	@Override
	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
		// TODO we shall move it to MetamathEditor ?
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS, PreferenceConstants.P_EDITOR_MATCHING_BRACKETS_COLOR, PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS);
		support.setMarginPainterPreferenceKeys(PreferenceConstants.P_EDITOR_PRINT_MARGIN, PreferenceConstants.P_EDITOR_PRINT_MARGIN_COLOR, PreferenceConstants.P_EDITOR_PRINT_MARGIN_COLUMN);
		super.configureSourceViewerDecorationSupport(support);
	}

	@Override
	protected boolean isTabsToSpacesConversionEnabled() {
		return getPreferenceStore() != null && getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_SPACES_FOR_TABS);
	}
	
	@Override
	protected void doSetInput(final IEditorInput input) throws CoreException {
//		ISourceViewer sourceViewer= getSourceViewer();
//
//		// un/reinstall & un/re-register preference store listener
//		getSourceViewerDecorationSupport(sourceViewer).uninstall();
//		if(sourceViewer != null) ((ISourceViewerExtension2)sourceViewer).unconfigure();
//		setPreferenceStore(Activator.getDefault().getPreferenceStore());
//		if(sourceViewer != null) sourceViewer.configure(getSourceViewerConfiguration());
//		getSourceViewerDecorationSupport(sourceViewer).install(getPreferenceStore());

		// invalidate highlighting on System Loaded
		super.doSetInput(input);
		MetamathProjectNature nature = MetamathProjectNature.getNature(input);
		if(nature == null) return;
		if(!nature.isLogicalSystemLoaded()) nature.addSystemLoadListener(new SystemLoadListener() {
			@Override
			public void systemLoaded() {
//				Job updateSyntaxHighlightingJob = new Job("Updating "+input.getName()+" syntax highlighting") {
//					@Override
//					protected IStatus run(IProgressMonitor monitor) {
//	            	   if(MetamathEditor.this.getSourceViewer() != null) 
//	            		   MetamathEditor.this.getSourceViewer().invalidateTextPresentation();
//	            	   return Status.OK_STATUS;
//					}};
//				updateSyntaxHighlightingJob.schedule();
				// cannot run this task as a job, as it requires UI access: it has to run in the main thread
				Display.getDefault().asyncExec(new Runnable() {
	               public void run() {
	            	   if(MetamathEditor.this.getSourceViewer() != null) 
	            		   MetamathEditor.this.getSourceViewer().invalidateTextPresentation();
	               }});
			}});
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(getEditorInput(), getContextSelection());
	}

	public void setFocus() {
		//getSourceViewer().invalidateTextPresentation();  // This seems to lead to some endless loops
		super.setFocus();
	}
	
	public ISelection getContextSelection() {
		TextSelection textSelection = (TextSelection)getSelectionProvider().getSelection();
		MetamathProjectNature nature = MetamathProjectNature.getNature(getEditorInput());
		if(nature == null || !nature.isLogicalSystemLoaded()) return null;

		// first try with symbols
		Map<String, Sym> symTbl = nature.getLogicalSystem().getSymTbl();
		Sym sym = symTbl.get(textSelection.getText());
		if(sym != null) 
			return new StructuredSelection(sym);

		// then try with statements
		Map<String, Stmt> stmtTbl = nature.getLogicalSystem().getStmtTbl();
		Stmt stmt = stmtTbl.get(textSelection.getText());
		if(stmt != null) 
			return new StructuredSelection(stmt);
		return textSelection;
	}

	private static final String[] SHOW_IN_TARGETS= new String[] { 
		ProofExplorerView.VIEW_ID, 
		ProofBrowserView.VIEW_ID,
		WebBrowserView.WEB_BROWSER_VIEW_ID,
		//MathView.VIEW_ID,
		//IPageLayout.ID_OUTLINE 
		};
	public static final IShowInTargetList SHOW_IN_TARGET_LIST= new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};
	
	public Object getAdapter(@SuppressWarnings("rawtypes") Class required) {
//		if (IContentOutlinePage.class.equals(required)) {
//			if (fOutlinePage == null) {
//				fOutlinePage= new MMContentOutlinePage(getDocumentProvider(), this) {};
//				if (getEditorInput() != null)
//					fOutlinePage.setInput(getEditorInput());
//			}
//			return fOutlinePage;
//		}
		if (MetamathProjectNature.class.equals(required)) {
			return MetamathProjectNature.getNature(getEditorInput());
		}
		if (IShowInTargetList.class.equals(required)) {
			return SHOW_IN_TARGET_LIST;
		}
		return super.getAdapter(required);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createNavigationActions()
	 */
	@Override
	protected void createNavigationActions() {
		super.createNavigationActions();

		ISourceViewer viewer = getSourceViewer();
		StyledText textWidget = viewer.getTextWidget();

		IAction action = new NextLevelFormulaAction(this, viewer, ST.WORD_PREVIOUS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_PREVIOUS);
		setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

		action= new NextLevelFormulaAction(this, viewer, ST.WORD_NEXT);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_NEXT);
		setAction(ITextEditorActionDefinitionIds.WORD_NEXT, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

		action= new NextLevelFormulaAction(this, viewer, ST.SELECT_WORD_PREVIOUS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
		setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);

		action= new NextLevelFormulaAction(this, viewer, ST.SELECT_WORD_NEXT);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
		setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);
	}

//	TODO Can be removed, preferences have been added
//	public void setTabWidthPreference() {
//		getSourceViewer().getTextWidget().setTabs(2);
//		installTabsToSpacesConverter();
//	}

//	/**
//	 * Shows the margin.
//	 * See SourceViewerDecorationSupport.showMargin()
//	 * TODO Can be removed, preferences have been added
//	 */
//	private void showMargin() {
//		ISourceViewer sourceViewer = getSourceViewer();
//		if (sourceViewer instanceof ITextViewerExtension2) {
//			MarginPainter marginPainter= new MarginPainter(sourceViewer);
//			marginPainter.setMarginRulerColor(getSharedColors().getColor(new RGB(192, 192, 192)));
//			marginPainter.setMarginRulerColumn(87); // TODO this shall be 80!
//			ITextViewerExtension2 extension= (ITextViewerExtension2) sourceViewer;
//			extension.addPainter(marginPainter);
//
//			// fFontPropertyChangeListener= new FontPropertyChangeListener();
//			// JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
//		}
//	}

//	public static class MMContentOutlinePage extends ContentOutlinePage implements SystemLoadListener, IShowInTarget {
//		MetamathEditor editor;
//		IEditorInput fInput;
//		
//		public MMContentOutlinePage(IDocumentProvider documentProvider,
//				MetamathEditor metamathEditor) {
//			this.editor = metamathEditor;
//		}
//
//		public void setInput(IEditorInput editorInput) {
//			this.fInput = editorInput;
//			if(editorInput != null) {
//				MetamathProjectNature nature = MetamathProjectNature.getNature(editorInput);
//				if(nature != null) nature.addSystemLoadListener(this);
//			}
//		}
//		
//		@Override
//		public void selectionChanged(SelectionChangedEvent event) {
//			if(event.getSelection() instanceof ITreeSelection) {
//				if(((ITreeSelection)event.getSelection()).getFirstElement() instanceof SourceElement) {
//					SourcePosition position = ((SourceElement)(((ITreeSelection)event.getSelection()).getFirstElement())).getPosition();
//					editor.selectAndReveal(position.charStartNbr, position.charEndNbr - position.charStartNbr);
//					}
//				}
//			super.selectionChanged(event);
//		}
//
//		@Override
//		public void createControl(Composite parent) {
//
//			super.createControl(parent);
//			TreeViewer viewer= getTreeViewer();
//			viewer.setContentProvider(new MMContentProvider());
//			viewer.setLabelProvider(new MMLabelProvider());
//			viewer.addSelectionChangedListener(this);
//
//			if (fInput != null) {
//				viewer.setInput(fInput);
//				((MMLabelProvider)viewer.getLabelProvider()).setNature(MetamathProjectNature.getNature(fInput));
//			}
//		}
//
//		@Override
//		public void systemLoaded() {
//			Display.getDefault().asyncExec(new Runnable() {
//	               public void run() {
//				    	  getTreeViewer().refresh();
//	               }});
//			Display.getDefault().asyncExec(new Runnable() {
//	               public void run() {
//	            	   if(editor.getSourceViewer() != null) 
//	            		   editor.getSourceViewer().invalidateTextPresentation();
//	               }});
//		}
//
//		@Override
//		public boolean show(ShowInContext context) {
//			ISelection selection= context.getSelection();
//			if (selection instanceof IStructuredSelection) {
//				getTreeViewer().setSelection((IStructuredSelection)selection, true);
//				return true;
//			}
//
////			Object   input= context.getInput();
////			if (input instanceof IEditorInput) {
////				Object   elementOfInput= getInputFromEditor((IEditorInput) input);
////				return elementOfInput != null && (tryToReveal(elementOfInput) == IStatus.OK);
////			}
//			return false;
//		}
//	}
}

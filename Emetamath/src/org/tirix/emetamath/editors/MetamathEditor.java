package org.tirix.emetamath.editors;

import java.util.Map;

import mmj.lang.Stmt;
import mmj.lang.Sym;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.ITextViewerExtension8.EnrichMode;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;
import org.tirix.emetamath.views.MathView;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.ProofExplorerView;

public class MetamathEditor extends TextEditor implements IShowInSource {
	public static final String EDITOR_ID = "org.tirix.emetamath.MetamathEditor";
//	protected MMContentOutlinePage fOutlinePage;
	
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new MMSourceViewerConfiguration(this, new ColorManager()));
		setEditorContextMenuId(EDITOR_ID);
		}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// TODO here, we are overriding the property PREFERENCE_HOVER_ENRICH_MODE - find a way to revert to default behavior, ex. by controlling the AbstractHoverInformationControlManager.setInformationControlReplacer()  
		((TextViewer)getSourceViewer()).setHoverEnrichMode(EnrichMode.ON_CLICK);
	}
	
	@Override
	protected void doSetInput(final IEditorInput input) throws CoreException {
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
		getSourceViewer().invalidateTextPresentation();
		super.setFocus();
	}
	
	public ISelection getContextSelection() {
		TextSelection textSelection = (TextSelection)getSelectionProvider().getSelection();
		MetamathProjectNature nature = MetamathProjectNature.getNature(getEditorInput());
		if(!nature.isLogicalSystemLoaded()) return null;

		// first try with symbols
		Map<String, Sym> symTbl = nature.getLogicalSystem().getSymTbl();
		Sym sym = symTbl.get(textSelection.getText());
		if(sym != null) 
			return new StructuredSelection(sym);

		// the try with statements
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
		MathView.VIEW_ID,
		//IPageLayout.ID_OUTLINE 
		};
	public static final IShowInTargetList SHOW_IN_TARGET_LIST= new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};
	
	public Object getAdapter(Class required) {
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

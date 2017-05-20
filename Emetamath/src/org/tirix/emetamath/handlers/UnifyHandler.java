package org.tirix.emetamath.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.DocumentSource;
import org.tirix.emetamath.nature.MetamathProjectNature.MetamathMessageHandler;
import org.tirix.emetamath.nature.MetamathProjectNature.ResourceSource;

import mmj.mmio.Source;
import mmj.mmio.SourcePosition;
import mmj.pa.PreprocessRequest;
import mmj.pa.ProofAsst;
import mmj.pa.ProofWorksheet;
import mmj.pa.StepRequest;
import mmj.tl.StoreInMMTFolderTLRequest;
import mmj.tl.TLRequest;

public class UnifyHandler extends MetamathEditorActionHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				false, // no renum
				false, // replace work vars with dummy vars in derivation steps
	            null,  // no preprocess request
	            null, //  no step selector request
	            null, // do not store into MMT Folder, MMT folder is not specified in eMetamath! was // new StoreInMMTFolderTLRequest(),
	            false // don't pring OK messages
		);

		return null; // must return null...
	}

    protected void unify(
    		IEvaluationContext context,
    		ProofAssistantEditor editor,
    		boolean           renumReq,
    		boolean 		  noConvertWV,
            PreprocessRequest preprocessRequest,
            StepRequest       stepRequest,
            TLRequest         tlRequest, 
            boolean 		  printOkMessages) throws ExecutionException {

		MetamathProjectNature nature = getNature(context);
		ProofAsst proofAsst = nature.getProofAsst();
		int inputCursorPos = editor.getCursorPos();

		Source sourceId = new DocumentSource(editor.getDocument(), editor, "Proof Text");
		if(editor.getEditorInput() instanceof FileEditorInput) {
			// clear markers - unification will re-create them 
			IFile file = ((FileEditorInput)editor.getEditorInput()).getFile(); // TODO consider WorkStorageEditor case
			((MetamathMessageHandler)nature.getMessageHandler()).clearMessages(file);
			
			// also clear annotation
			IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			for(@SuppressWarnings("unchecked")
			Iterator<Annotation> i = annotationModel.getAnnotationIterator();i.hasNext();) annotationModel.removeAnnotation(i.next());
		}
		if(proofAsst == null) return; // TODO showMessage to user -  using MessageDialog
		
		// TODO use a Request or a Worker with Progress to perform the search in the background
		// TODO use the Store interface and do changes directly in the editor's TextStore...
        ProofWorksheet w =
            proofAsst.
                unify(renumReq,
                	  noConvertWV,
                	  sourceId,
                	  preprocessRequest,
                      stepRequest,
                      tlRequest,
                      inputCursorPos, 
                      printOkMessages);

        if (!w.hasStructuralErrors()) {
	        if (w.getStepSelectorStore() != null) {
	        	if(w.getStepSelectorStore().isEmpty()) {
		       		System.out.println("Unification completed. No step to show.");
	       			MessageBox messageBox = new MessageBox(editor.getSite().getShell(), SWT.OK | SWT.ICON_INFORMATION);
	       			messageBox.setText("Step Selection Result");
	       			messageBox.setMessage("No unifiable assertion was found for step "+w.getStepSelectorStore().getStep());
	       			messageBox.open();
	       			MetamathUI.showInStepSelector(nature, w.getStepSelectorStore(), editor);
	       		}
	       		else {
		       		System.out.println("Unification completed. Showing StepSelector.");
	       			MetamathUI.showInStepSelector(nature, w.getStepSelectorStore(), editor);
	       		}
	        }
	        else {
	       		System.out.println("Unification completed. No StepSelector, moving to "+w.getProofCursor().getCaretCharNbr());
	        	editor.displayProofWorksheet(w);
	        	editor.setCursorPos(inputCursorPos);
	        	editor.selectAndReveal(inputCursorPos, 0); //w.getProofCursor().getCaretCharNbr(), 0);
	        	editor.setFocus();
            }
        }
        else {
       		System.out.println("Unification completed. Structural errors, moving to "+w.getProofCursor().getCaretCharNbr());
        	// TODO showMessage to user
        	// TODO set cursor to w.proofAsstCursor
        	editor.selectAndReveal(inputCursorPos, 0); //w.getProofCursor().getCaretCharNbr(), 0);
        	MetamathUI.showProblemsView();
        }
	}
}

// from ProofAsstGUI
//if (stepRequest != null
//&& (stepRequest.type == StepRequestType.GeneralSearch
//    || stepRequest.type == StepRequestType.SearchOptions))
//proofAsstPreferences.getSearchMgr()
//    .execShowSearchOptions(w);
//else if (stepRequest != null
//&& stepRequest.type == StepRequestType.StepSearch
//&& w.searchOutput != null)
//{
//final String s = ProofWorksheet
//    .getOutputMessageTextAbbrev(proofAsst.getMessageHandler());
//if (s != null)
//    displayRequestMessages(s);
//proofAsstPreferences.getSearchMgr().execShowSearchResults();
//}
//else if (w.stepSelectorStore != null) {
//disposeOfOldSelectorDialog();
//stepSelectorDialog = new StepSelectorDialog(mainFrame,
//    w.stepSelectorStore.getResults(), ProofAsstGUI.this,
//    proofAsstPreferences, proofFont);
//}
//else {
//displayProofWorksheet(w, false);
//if (stepRequest != null
//    && stepRequest.type == StepRequestType.SelectorChoice)
//    getMainFrame().setVisible(true);
//}
//}

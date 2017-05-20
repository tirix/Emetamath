package org.tirix.emetamath.handlers;

import java.util.Collection;

import mmj.pa.PreprocessRequest;
import mmj.pa.ProofAsst;
import mmj.pa.ProofWorksheet;
import mmj.pa.StepRequest;
import mmj.tl.StoreInLogSysAndMMTFolderTLRequest;
import mmj.tl.TLRequest;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;
import org.tirix.emetamath.editors.proofassistant.ProofDocument;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.MetamathMessageHandler;

public class UnifyHandler extends MetamathEditorActionHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				false, // no renum
	            null,  // no preprocess request
	            null, //  no step selector request
	            new StoreInLogSysAndMMTFolderTLRequest()
		);

		return null; // must return null...
	}

    protected void unify(
    		IEvaluationContext context,
    		ProofAssistantEditor editor,
    		boolean           renumReq,
            PreprocessRequest preprocessRequest,
            StepRequest       stepRequest,
            TLRequest         tlRequest) throws ExecutionException {

    	
    	Collection<TextSelection> selectionSet = (Collection<TextSelection>) context.getDefaultVariable();
		TextSelection selection = selectionSet.toArray(new TextSelection[1])[0];

		MetamathProjectNature nature = getNature(context);
		ProofAsst proofAsst = nature.getProofAsst();
		ProofDocument proofDocument = (ProofDocument) editor.getDocumentProvider().getDocument(editor.getEditorInput());
		String proofText = proofDocument.get();
		int inputCursorPos = selection != null ? selection.getOffset() + 1 : 0;

		Object sourceId = null;
		if(editor.getEditorInput() instanceof FileEditorInput) {
			// clear markers - unification will re-create them 
			IFile file = ((FileEditorInput)editor.getEditorInput()).getFile(); // TODO consider WorkStorageEditor case
			((MetamathMessageHandler)nature.getMessageHandler()).clearMessages(file);
			sourceId = file;
		}
		if(proofAsst == null) return; // TODO showMessage to user
		
        ProofWorksheet w =
            proofAsst.
                unify(renumReq,
                	  proofText,
                	  sourceId,
                      preprocessRequest,
                      stepRequest,
                      tlRequest,
                      inputCursorPos);

        if (!w.hasStructuralErrors()) {
	        if (w.getStepSelectorStore() != null) {
	       		if(w.getStepSelectorStore().isEmpty()) {
	       			MessageBox messageBox = new MessageBox(editor.getSite().getShell(), SWT.OK | SWT.ICON_INFORMATION);
	       			messageBox.setText("Step Selection Result");
	       			messageBox.setMessage("No unifiable assertion was found for "+w.getStepSelectorStore().getStep());
	       			messageBox.open();
	       		}
	       		else 
	       			MetamathUI.showInStepSelector(nature, w.getStepSelectorStore(), editor);
	        }
	        else {
	        	editor.displayProofWorksheet(w);
	        	editor.selectAndReveal(w.getProofCursor().getCaretCharNbr(), 0);
	        	editor.setFocus();
            }
        }
        else {
        	// TODO showMessage to user
        	// TODO set cursor to w.proofAsstCursor
        	MetamathUI.showProblemsView();
        }
	}
}

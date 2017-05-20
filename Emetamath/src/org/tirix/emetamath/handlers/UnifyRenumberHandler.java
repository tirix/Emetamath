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

public class UnifyRenumberHandler extends UnifyHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				true, 	// renumber!
	            null, 	// no preprocess request
	            null,	//  no step selector request
	            new StoreInLogSysAndMMTFolderTLRequest()
		);

		return null; // must return null...
	}
}

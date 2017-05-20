package org.tirix.emetamath.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;

import mmj.tl.StoreInMMTFolderTLRequest;

public class UnifyRenumberHandler extends UnifyHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				true, 	// renumber!
	            false,  // no convertion of working variables
	            null, 	// no preprocess request
	            null,	//  no step selector request
	            null, // do not store into MMT Folder, MMT folder is not specified in eMetamath! was // new StoreInMMTFolderTLRequest(),
	            false	// don't print Ok messages
		);

		return null; // must return null...
	}
}

package org.tirix.emetamath.handlers;

import mmj.pa.PaConstants;
import mmj.pa.StepRequest;
import mmj.pa.StepRequest.StepRequestType;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;

public class SearchStepSelectionHandler extends UnifyHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		StepRequest stepRequest = new StepRequest(
                StepRequestType.SelectorSearch);
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				false,	// no renum
	            false,  // no convertion of working variables
	            null,	// no preprocess request
	            stepRequest, //  step selector request
	            null,	// no TheoremLoader request
	            false	// don't print Ok messages
		);

		return null; // must return null...
	}
}

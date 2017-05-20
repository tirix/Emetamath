package org.tirix.emetamath.handlers;

import mmj.pa.PaConstants;
import mmj.pa.StepRequest;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;

public class SearchStepSelectionHandler extends UnifyHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		StepRequest stepRequest = new StepRequest(
                PaConstants.
                STEP_REQUEST_SELECTOR_SEARCH);
		unify(context,
				(ProofAssistantEditor)getEditor(context),
				false,	// no renum
	            null,	// no preprocess request
	            stepRequest, //  step selector request
	            null	// no TheoremLoader request
		);

		return null; // must return null...
	}
}

package org.tirix.emetamath.handlers;

import mmj.lang.Assrt;
import mmj.pa.PaConstants;
import mmj.pa.StepRequest;
import mmj.pa.StepRequest.StepRequestType;
import mmj.pa.StepSelectorItem;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.tirix.emetamath.views.StepSelectorView;

/**
 * A Command Handler to apply the selection to the Step and unify the proof.
 *  
 * @author Thierry
 */
public class ApplyStepSelectionHandler extends UnifyHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		StepSelectorView stepSelectorView = getStepSelector(context);
		ISelection selection = stepSelectorView.getViewer().getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();
		if(obj instanceof StepSelectorItem) {
	        Assrt assrtChoice = ((StepSelectorItem)obj).getAssrt();
			StepRequest stepRequest = new StepRequest(
					StepRequestType.SelectorChoice,
                    stepSelectorView.getStep(),
                    assrtChoice);
			unify(context,
					stepSelectorView.getActiveEditor(),
					false,	// no renum
		            false,  // no convertion of working variables
		            null,	// no preprocess request
		            stepRequest, //  step selector request
		            null,	// no TheoremLoader request
		            false	// don't print Ok messages
			);
		}

		return null; // must return null...
	}
}

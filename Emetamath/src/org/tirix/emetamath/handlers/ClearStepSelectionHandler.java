package org.tirix.emetamath.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.views.StepSelectorView;

public class ClearStepSelectionHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		StepSelectorView view = getStepSelector((IEvaluationContext)event.getApplicationContext());
		view.clear();
		return null; // must return null...
	}
}

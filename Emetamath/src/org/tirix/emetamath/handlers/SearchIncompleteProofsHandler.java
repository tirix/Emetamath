package org.tirix.emetamath.handlers;


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.search.ui.NewSearchUI;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.search.MetamathSearchIncompleteQuery;

public class SearchIncompleteProofsHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MetamathProjectNature nature = getNature(context);
		NewSearchUI.runQueryInBackground(new MetamathSearchIncompleteQuery(nature));
		return null; // must return null...
	}
}

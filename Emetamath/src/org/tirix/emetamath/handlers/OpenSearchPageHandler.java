package org.tirix.emetamath.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.tirix.emetamath.search.MetamathSearchPage;

public class OpenSearchPageHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		IWorkbenchWindow window = (IWorkbenchWindow)context.getVariable("activeWorkbenchWindow");
		NewSearchUI.openSearchDialog(window, MetamathSearchPage.PAGE_ID);
		return null; // must return null...
	}
}

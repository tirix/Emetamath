package org.tirix.emetamath.handlers;

import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.pa.ProofAsst;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.ProofBrowserView;

public class BrowseBackHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		ProofBrowserView proofBrowser = getBrowserView(context);
		proofBrowser.back();
		return null; // must return null...
	}
}

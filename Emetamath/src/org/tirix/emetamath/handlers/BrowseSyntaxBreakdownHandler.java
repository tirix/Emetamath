package org.tirix.emetamath.handlers;

import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.pa.ProofAsst;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class BrowseSyntaxBreakdownHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MetamathProjectNature nature = getNature(context);
		
		MObj obj = getSelectedMObj(context);
		if(!(obj instanceof Stmt)) return null;

		// TODO also adapt to simple TextSelection to be parsed
		
		ProofAsst proofAsst = nature.getProofAsst();
		if(proofAsst == null) return null;
		
		Stmt[] exprRPN = ((Stmt)obj).getExprRPN();
		
		//ProofBrowserView = 
		//MetamathUI.openInEditor(w, nature);
		return null; // must return null...
	}
}

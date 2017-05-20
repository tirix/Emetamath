package org.tirix.emetamath.handlers;

import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.lang.Sym;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class OpenNotationHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MObj obj = getSelectedMObj(event);
		if(!(obj instanceof Sym)) {
			System.out.println("Selected object is not a symbol, it is "+obj==null?null:obj.getClass());
			return null;
		}
		MetamathProjectNature nature = getNature(context);
		Stmt notation = nature.getNotation((Sym)obj);
		MetamathUI.openInEditor(notation);
		return null; // must return null...
	}
}

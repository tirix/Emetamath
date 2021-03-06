package org.tirix.emetamath.handlers;


import mmj.lang.MObj;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.search.ui.NewSearchUI;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.search.MetamathSearchReferenceQuery;

public class SearchReferencesHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MObj obj = getSelectedMObj(event);
		MetamathProjectNature nature = getNature(context);
		
		if(obj != null)
			NewSearchUI.runQueryInBackground(new MetamathSearchReferenceQuery(nature, obj));
		return null; // must return null...
	}
}

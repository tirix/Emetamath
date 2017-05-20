package org.tirix.emetamath.handlers;

import mmj.lang.MObj;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.ui.handlers.HandlerUtil;
import org.tirix.emetamath.MetamathUI;

public class OpenDeclarationHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MObj obj = getSelectedMObj(event);
		//IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().findEditor(MetamathEditor.EDITOR_ID);
		MetamathUI.openInEditor(obj);
		return null; // must return null...
	}
}

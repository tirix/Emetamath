package org.tirix.emetamath.handlers;

import java.util.Set;

import mmj.lang.LogicalSystem;
import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.IViewDescriptor;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class OpenDeclarationHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MObj obj = getSelectedMObj((IEvaluationContext)event.getApplicationContext());
		//IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().findEditor(MetamathEditor.EDITOR_ID);
		MetamathUI.openInEditor(obj);
		return null; // must return null...
	}
}

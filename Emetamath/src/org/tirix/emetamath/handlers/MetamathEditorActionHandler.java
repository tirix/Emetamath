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

public abstract class MetamathEditorActionHandler extends AbstractHandler {

	protected MetamathProjectNature getNature(IEvaluationContext context) throws ExecutionException {
		MetamathProjectNature nature = null;

		WorkbenchPart part = (WorkbenchPart)context.getVariable("activePart");
		try {
			if(part instanceof EditorPart) {
				EditorPart editorPart = (EditorPart)part;
				nature = (MetamathProjectNature)((IFileEditorInput)editorPart.getEditorInput()).getFile().getProject().getNature(MetamathProjectNature.NATURE_ID);
			}
		} catch (CoreException e) {
			e.printStackTrace();
			throw new ExecutionException("Cannot find editor input", e);
		}
		return nature;
	}

	// TODO continue or replace with IEditorActionDelegate (simpler)...
	protected MObj getSelectedMObj(IEvaluationContext context) throws ExecutionException {
		MetamathProjectNature nature = getNature(context);
		if(nature == null) throw new ExecutionException("Cannot find metamath project nature");
		if(!nature.isLogicalSystemLoaded()) throw new ExecutionException("Logical System not yet loaded");
		
		Set<TextSelection> selectionSet = (Set<TextSelection>) context.getDefaultVariable();
		TextSelection selection = selectionSet.toArray(new TextSelection[1])[0];
		String objectName = selection.getText();

		MObj mobj = nature.getMObj(objectName);
		if(mobj != null) return mobj;

		throw new ExecutionException("Declaration not found");
	}
}

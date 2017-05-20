package org.tirix.emetamath.handlers;

import java.awt.Toolkit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.MarkSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.StepSelectorView;

import mmj.lang.MObj;

public abstract class MetamathEditorActionHandler extends AbstractHandler {

	protected MetamathEditor getEditor(IEvaluationContext context) throws ExecutionException {
		WorkbenchPart part = (WorkbenchPart)context.getVariable("activePart");
		if(part instanceof MetamathEditor) {
			return (MetamathEditor)part;
		}
		throw new ExecutionException("Cannot find editor input");
	}

	protected StepSelectorView getStepSelector(IEvaluationContext context) throws ExecutionException {
		WorkbenchPart part = (WorkbenchPart)context.getVariable("activePart");
		if(part instanceof StepSelectorView) {
			return (StepSelectorView)part;
		}
		throw new ExecutionException("Cannot find step selector input");
	}

	protected ProofBrowserView getBrowserView(IEvaluationContext context) throws ExecutionException {
		WorkbenchPart part = (WorkbenchPart)context.getVariable("activePart");
		if(part instanceof ProofBrowserView) {
			return (ProofBrowserView)part;
		}
		throw new ExecutionException("Cannot find proof browser");
	}

	protected MetamathProjectNature getNature(IEvaluationContext context) throws ExecutionException {
		WorkbenchPart part = (WorkbenchPart)context.getVariable("activePart");
		return (MetamathProjectNature)part.getAdapter(MetamathProjectNature.class);
	}
	
	protected MObj getSelectedMObj(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MetamathProjectNature nature = getNature(context);
		if(nature == null) throw new ExecutionException("Cannot find metamath project nature");
		if(!nature.isLogicalSystemLoaded()) throw new ExecutionException("Logical System not yet loaded");

		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if(selection instanceof IStructuredSelection) {
			Object selectedObject =((IStructuredSelection)selection).getFirstElement();
			if(selectedObject instanceof MObj) return (MObj)selectedObject;
			throw new ExecutionException("Selected object "+selectedObject+" of selection "+selection+" is not an MObj!");
		} else if (selection instanceof ITextSelection) {
			String selectedText = ((ITextSelection)selection).getText();
			MObj mobj = nature.getMObj(selectedText);
			if(mobj != null) return mobj;
			// TODO here, instead of throwing an exception, we shall emit a "beep" to tell the user we can't handle the request
			//throw new ExecutionException("Selected string "+selectedText+" does not resolve to an MObj!");
			PlatformUI.getWorkbench().getDisplay().beep();
			return null;
		} else if (selection instanceof MarkSelection) {
			//throw new ExecutionException("Don't know how to get MObj from MarkSelection "+selection);
			PlatformUI.getWorkbench().getDisplay().beep();
			return null;
		} else {
			//throw new ExecutionException("Don't know how to get MObj from "+selection);
			PlatformUI.getWorkbench().getDisplay().beep();
			return null;
		}
	}
}

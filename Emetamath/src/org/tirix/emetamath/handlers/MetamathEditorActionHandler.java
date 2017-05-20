package org.tirix.emetamath.handlers;

import java.util.Collection;

import mmj.lang.MObj;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.part.WorkbenchPart;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.StepSelectorView;

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

	protected MObj getSelectedMObj(IEvaluationContext context) throws ExecutionException {
		MetamathProjectNature nature = getNature(context);
		if(nature == null) throw new ExecutionException("Cannot find metamath project nature");
		if(!nature.isLogicalSystemLoaded()) throw new ExecutionException("Logical System not yet loaded");
		
		Collection<?> selectionCollection = (Collection<?>)context.getDefaultVariable();
		if(selectionCollection.size() == 0) throw new ExecutionException("No object selected");
		Object selection = selectionCollection.toArray()[0];

		MObj mobj = null;
		if(selection instanceof MObj) {
			mobj = (MObj)selection;
		}
		if(selection instanceof TextSelection) {
			String objectName = ((TextSelection)selection).getText();
			mobj = nature.getMObj(objectName);
		}
		if(mobj != null) return mobj;

		throw new ExecutionException("Declaration not found");
	}
}

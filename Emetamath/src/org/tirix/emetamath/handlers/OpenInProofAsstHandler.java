package org.tirix.emetamath.handlers;

import mmj.lang.MObj;
import mmj.lang.Theorem;
import mmj.pa.ProofAsst;
import mmj.pa.ProofWorksheet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class OpenInProofAsstHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MetamathProjectNature nature = getNature(context);
		
		MObj obj = getSelectedMObj(context);
		if(!(obj instanceof Theorem)) return null;
		
		ProofAsst proofAsst = nature.getProofAsst();
		if(proofAsst == null) return null;
		
		ProofWorksheet w =
            proofAsst.getExistingProof((Theorem)obj,
                                   true,	//proofUnified
                                   false);	//hypsRandomized

		MetamathUI.openInEditor(w, nature);
		return null; // must return null...
	}
}

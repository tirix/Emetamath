package org.tirix.emetamath.nature;

import org.eclipse.core.expressions.PropertyTester;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;

import mmj.pa.StepSelectorStore;

public class ProofUnificationTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		ProofAssistantEditor editor = (ProofAssistantEditor)receiver;
		if(editor == null) return false;
		if(property.equals("isProofUnified")) return editor.isProofUnified();
		return false;
	}
}

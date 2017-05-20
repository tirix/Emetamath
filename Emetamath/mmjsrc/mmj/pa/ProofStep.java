package mmj.pa;

import mmj.lang.Formula;
import mmj.lang.Stmt;

/**
 * The interface to which conforms any step that can be shown in an explorer.
 * This is valid both for Proof Steps (complete with reference to ProofWorksheet) 
 * and Syntax Breakdown steps (simplified version)  
 * @author Thierry
 */
public interface ProofStep {
	public String getStepName();
	
	public String[] getHypSteps();
	public int getHypCount();
	
	public void setRef(Stmt ref);
	public Stmt getRef();
	public String getRefLabel();

	public Formula getFormula();

	public static boolean isDerivationStep(ProofStep step) {
		return step.getHypCount() > 0;
	}

	public static boolean isHypothesisStep(ProofStep step) {
		return step.getHypCount() == 0;
	}
}

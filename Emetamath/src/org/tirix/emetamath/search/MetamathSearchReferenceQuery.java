package org.tirix.emetamath.search;

import org.tirix.emetamath.nature.MetamathProjectNature;

import mmj.lang.MObj;
import mmj.lang.ParseTree.RPNStep;
import mmj.lang.Stmt;
import mmj.lang.Sym;

public class MetamathSearchReferenceQuery extends MetamathSearchQuery {
	private MObj obj;

	public MetamathSearchReferenceQuery(MetamathProjectNature nature, MObj obj) {
		super(nature, (obj instanceof Sym ? MetamathSearchMode.IN_MM_STATEMENTS : 
			          (obj instanceof Stmt ? MetamathSearchMode.IN_MM_PROOFS : null)));
		this.obj = obj;
	}

	@Override
	public String getLabel() {
		return "Search for Metamath References of '"+obj+"'";
	}

	@Override
	public String getSearchingLabel() {
		return "Searching for '"+obj+"'";
	}
	
	@Override
	public boolean checkProofStep(Stmt stmt, RPNStep step) {
		return step != null && obj.equals(step.stmt);
	}

	@Override
	public boolean checkStmtSym(Stmt stmt, Sym sym) {
		return obj.equals(sym);
	}
}

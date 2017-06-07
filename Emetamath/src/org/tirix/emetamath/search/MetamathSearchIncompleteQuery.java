/**
 * 
 */
package org.tirix.emetamath.search;

import java.util.ArrayList;
import java.util.Map;

import mmj.lang.MObj;
import mmj.lang.ParseTree.RPNStep;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Theorem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.internal.ui.text.SearchResultUpdater;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class MetamathSearchIncompleteQuery extends MetamathSearchQuery {
	ArrayList<Stmt> incompleteTheorems;
	
	public MetamathSearchIncompleteQuery(MetamathProjectNature nature) {
		super(nature, MetamathSearchMode.IN_MM_PROOFS);
	}

	@Override
	public String getLabel() {
		return "Search for Metamath Incomplete Proofs";
	}

	@Override
	public String getSearchingLabel() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean checkProofStep(Stmt stmt, RPNStep step) {
		if(step == null || incompleteTheorems.contains(step.stmt)) {
			incompleteTheorems.add(stmt);
			return true;
		}
		return false;
	}

	@Override
	public boolean checkStmtSym(Stmt stmt, Sym sym) {
		return false; // unused, always in proof mode
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		incompleteTheorems = new ArrayList<Stmt>();
		return super.run(monitor);
	}
}


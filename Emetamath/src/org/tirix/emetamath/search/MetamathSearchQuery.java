/**
 * 
 */
package org.tirix.emetamath.search;

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

public abstract class MetamathSearchQuery implements ISearchQuery {
	protected MetamathProjectNature nature;
	private ISearchResult fResult;
	private MetamathSearchMode mode;
	protected enum MetamathSearchMode { IN_MM_PROOFS, IN_MM_STATEMENTS };
	public static final int ALL_OCCURRENCES = 0xFFFFFFFF;
	public static final int PROOFS = 1;
	public static final int HYPOTHESIS = 2;
	public static final int AXIOMS = 4;
	public static final int DEFINITIONS = 8;
	public static final int THEOREMS = 16;

	public static final int MM_FILES = 1;
	public static final int MMP_FILES = 2;
	
	public MetamathSearchQuery(MetamathProjectNature nature, MetamathSearchMode mode) {
		this.nature = nature;
		this.mode = mode;
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	public MetamathProjectNature getNature() {
		return nature;
	}
	
	@Override
	public ISearchResult getSearchResult() {
		if (fResult == null) {
			MetamathSearchResult result= new MetamathSearchResult(this);
			new SearchResultUpdater(result);
			fResult= result;
		}
		return fResult;
	}

	public abstract String getSearchingLabel();
	public abstract boolean checkProofStep(Stmt stmt, RPNStep step);
	public abstract boolean checkStmtSym(Stmt stmt, Sym sym);
	
	@Override
	public IStatus run(IProgressMonitor monitor)
			throws OperationCanceledException {
		final MetamathSearchResult result= (MetamathSearchResult) getSearchResult();
		result.removeAll();

		int count = 0;
		int max = Integer.MAX_VALUE; // TODO store this as a Metamath preference !
		
		Map<String, Stmt> stmtTbl = nature.getLogicalSystem().getStmtTbl();
		monitor.beginTask(getSearchingLabel(), stmtTbl.size());
		
		switch(mode) {
		case IN_MM_STATEMENTS:
			symLoop:
			for(Stmt stmt:stmtTbl.values()) {
				for(Sym sym:stmt.getFormula().getExpr()) {
					if(checkStmtSym(stmt, sym)) {
						result.addMatch(new MetamathMatch(stmt));
						if(count++ == max) break symLoop;
					}
				}
				monitor.worked(1);
			}
			monitor.done();
			break;

		case IN_MM_PROOFS:
			stmtLoop:
			for(Stmt stmt:stmtTbl.values()) {
				if(stmt instanceof Theorem) {
					for(RPNStep proofStep:((Theorem)stmt).getProof()) {
						if(checkProofStep(stmt, proofStep)) {
							result.addMatch(new MetamathMatch(stmt)); 
							if(count++ == max) break stmtLoop;
						}
					}
				}
				monitor.worked(1);
			}
			monitor.done();
			break;
		default:
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Object to be searched shall be either a Symbol or Statement", null);
		}
		return new Status(IStatus.OK, Activator.PLUGIN_ID, 0, "Found "+result.getMatchCount()+" references", null);
	}
}


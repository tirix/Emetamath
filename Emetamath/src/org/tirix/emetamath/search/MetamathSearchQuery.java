/**
 * 
 */
package org.tirix.emetamath.search;

import java.util.Map;

import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Theorem;
import mmj.mmio.SourcePosition;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.internal.ui.text.SearchResultUpdater;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class MetamathSearchQuery implements ISearchQuery {
	private MetamathProjectNature nature;
	private MObj obj;
	private ISearchResult fResult;
	
	public MetamathSearchQuery(MetamathProjectNature nature, MObj obj) {
		this.obj = obj;
		this.nature = nature;
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public String getLabel() {
		return "Search for Metamath References of '"+obj+"'";
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

	@Override
	public IStatus run(IProgressMonitor monitor)
			throws OperationCanceledException {
		final MetamathSearchResult result= (MetamathSearchResult) getSearchResult();
		result.removeAll();

		if(obj instanceof Sym) {
			Map<String, Stmt> stmtTbl = nature.getLogicalSystem().getStmtTbl();
			monitor.beginTask("Searching "+obj, stmtTbl.size());
			
			for(Stmt stmt:stmtTbl.values()) {
				for(Sym sym:stmt.getFormula().getExpr()) {
					if(sym.equals(obj)) result.addMatch(new MetamathMatch(stmt)); 
				}
				monitor.worked(1);
			}
			monitor.done();
		}
		else if(obj instanceof Stmt) {
			Map<String, Stmt> stmtTbl = nature.getLogicalSystem().getStmtTbl();
			monitor.beginTask("Searching "+obj, stmtTbl.size());
			
			for(Stmt stmt:stmtTbl.values()) {
				if(stmt instanceof Theorem) {
					for(Stmt proofStep:((Theorem)stmt).getProof()) {
						if(proofStep.equals(obj)) 
							result.addMatch(new MetamathMatch(stmt)); 
					}
				}
				monitor.worked(1);
			}
			monitor.done();
		} else {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Object to be searched shall be either a Symbol or Statement", null);
		}
		return new Status(IStatus.OK, Activator.PLUGIN_ID, 0, "Found "+result.getMatchCount()+" references", null);
	}
}


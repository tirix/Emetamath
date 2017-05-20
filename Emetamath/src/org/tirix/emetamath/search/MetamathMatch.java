/**
 * 
 */
package org.tirix.emetamath.search;

import mmj.lang.Stmt;

import org.eclipse.search.ui.text.Match;

public class MetamathMatch extends Match {
	Stmt stmt;
	
	public MetamathMatch(Stmt stmt) {
		super(stmt.getPosition().source, stmt.getPosition().charStartNbr, stmt.getPosition().getLength());
		this.stmt = stmt;
	}
	
	@Override
	public Object getElement() {
		return stmt;
	}
}
package org.tirix.mmj;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import mmj.lang.Formula;
import mmj.lang.ParseNode;
import mmj.lang.ParseTree;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.WorkVarHyp;
import mmj.pa.ProofStep;
import mmj.pa.ProofWorkStmt;
import mmj.verify.SubstMapEntry;

public class SyntaxBreakdownSheet {
    List<ProofStep> proofWorkStmtList;
    Map<Formula, SyntaxBreakdownStep> stepsByFormula;
    int lastId;
    
    public SyntaxBreakdownSheet(ParseTree parseTree) {
        proofWorkStmtList = new ArrayList<ProofStep>();
        stepsByFormula = new Hashtable<Formula, SyntaxBreakdownStep>();
        lastId = 0;
        addNode(parseTree.getRoot());
    }

    protected SyntaxBreakdownStep addNode(ParseNode node) {
    	int i=0;
    	SyntaxBreakdownStep[] hyps = new SyntaxBreakdownStep[node.child.length];
    	SubstMapEntry[] substMap = new SubstMapEntry[node.child.length];
    	for(ParseNode child:node.child) {
    		hyps[i] = addNode(child);
    		substMap[i] = new SubstMapEntry(node.stmt.getMandVarHypArray()[i].getVar(), hyps[i].formula.getExpr());
    		i++;
    	}

    	Formula formula = node.stmt.getFormula().apply(substMap);
    	// we could have done the same faster, by mapping nodes to steps, but ParseNode.equals and ParseNode.hashCode would have been required
    	SyntaxBreakdownStep step = stepsByFormula.get(formula);
    	if(step != null) return step;
    	lastId++;
    	step = new SyntaxBreakdownStep(Integer.toString(lastId), hyps, node.stmt, formula);
    	proofWorkStmtList.add(step);
    	stepsByFormula.put(formula, step);
    	return step;
    }

	public List<ProofStep> getProofStepList() {
		return proofWorkStmtList;
	}
    
    public static class SyntaxBreakdownStep implements ProofStep {
    	String stepName;
    	SyntaxBreakdownStep[] hyps;
    	String[] hypSteps;
    	Formula formula;
    	Stmt ref;
    	
    	public SyntaxBreakdownStep(String name, SyntaxBreakdownStep[] hyps, Stmt ref, Formula formula) {
    		this.stepName = name;
    		this.hyps = hyps;
    		this.ref = ref;
    		this.formula = formula;
    	}
    	
		@Override
		public Formula getFormula() {
			return formula;
		}

		@Override
		public int getHypCount() {
			return hyps.length;
		}

		@Override
		public String[] getHypSteps() {
			if(hypSteps == null) {
				hypSteps = new String[hyps.length];
				for(int i=0;i<hyps.length;i++) hypSteps[i] = hyps[i].getStepName(); 
			}
			return hypSteps;
		}

		@Override
		public Stmt getRef() {
			return ref;
		}

		@Override
		public String getRefLabel() {
			return ref.getLabel();
		}

		@Override
		public String getStepName() {
			return stepName;
		}

		@Override
		public void setRef(Stmt ref) {
			this.ref = ref;
		}
    }
}

package org.tirix.emetamath.editors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;
import org.tirix.emetamath.editors.proofassistant.ProofDocument;
import org.tirix.emetamath.nature.MetamathProjectNature;

import mmj.lang.Axiom;
import mmj.lang.Formula;
import mmj.lang.Hyp;
import mmj.lang.LangException;
import mmj.lang.LogicalSystem;
import mmj.lang.ParseNode;
import mmj.lang.ParseTree;
import mmj.lang.Sym;
import mmj.lang.Var;
import mmj.lang.VarHyp;
import mmj.mmio.MMIOException;
import mmj.mmio.Source;
import mmj.mmio.Source.StringSource;
import mmj.mmio.SrcStmt;
import mmj.mmio.Statementizer;
import mmj.mmio.Tokenizer;

/**
 * Provides well-formed formulas around a given cursor position
 * That is, a text snippet which corresponds to a syntax builders (in set.mm, that means a setvar, a class or a wff).
 * @author Thierry
 */
public class MMFormulaProvider {
	/** Formula must enclose the current selection either from the left, or the right */
	public static final int LEFT_OR_RIGHT = 1;
	/** Formula must start before the current selection */
	public static final int LEFT = 2;
	/** Formula must end after the current selection */
	public static final int RIGHT = 3;
	/**
	 * Returns the next bigger formula containing the given region.
	 * @param doc
	 * @param offset
	 * @param length
	 * @return
	 */
	public static IRegion getShortestFormula(IDocument doc, MetamathProjectNature nature, int offset, int length, int mode) throws BadLocationException {
		IRegion full = (doc instanceof ProofDocument)? MMRegionProvider.getMMPStatement(doc, offset): MMRegionProvider.getStatement(doc, offset);
		try {
			assert (offset >= full.getOffset() && offset+length <= full.getOffset()+full.getLength()):"Search region outside of full region!";
			String formulaStr = doc.get(full.getOffset(), full.getLength());
			IRegion shortest = getShortestFormula(nature, formulaStr, offset - full.getOffset(), length, mode);
			if(shortest == null) return null;
			return new Region(full.getOffset()+shortest.getOffset(), shortest.getLength());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @deprecated
	 */
	public static Formula getFormula(MetamathProjectNature nature, String formulaStr) throws LangException, IOException, MMIOException {
		Source s = new StringSource("temp $a "+formulaStr+" $.", "selection");
		Tokenizer t = new Tokenizer(s);
		Statementizer st = new Statementizer(t);
		SrcStmt ss = st.getStmt();
		ArrayList<Hyp> hypList = new ArrayList<>();
		Formula f = new Formula(nature.getLogicalSystem().getSymTbl(), ss.typ, ss.symList, hypList);
		st.close();
		return f;
	}
	
	public static IRegion getShortestFormula(MetamathProjectNature nature, String formulaStr, int offset, int length, int mode) {
        final LogicalSystem logicalSystem = nature.getLogicalSystem();
        final Map<String, Sym> symTbl = logicalSystem.getSymTbl();
        int count = formulaStr.split(" ").length + 1;
        final ArrayList<Sym> symList = new ArrayList<>(count);
        final MMTokenDetector detector = MMRegionProvider.DETECTOR;
        
        // Parse symbols, retaining their offset and length
        final int[] start = new int[count];
        final int[] end = new int[count];
        final char[] chars = formulaStr.toCharArray();
        count = 0;
        for(int i=0; i<chars.length;i++) {
        		char c = chars[i];
			if (detector.isWordStart((char) c)) {
				start[count] = i;
				do { c = ++i<chars.length?chars[i]:0xFF; } while (detector.isWordPart((char) c)) ;
				end[count] = i;
				symList.add(symTbl.get(formulaStr.substring(start[count], end[count])));
				count++;
			}
        }

        // Build and parse the corresponding formula
        final Formula f = new Formula(symList);
        ParseTree tree = nature.getGrammar()
            .parseFormula(nature.getMessageHandler(), symTbl,
                logicalSystem.getStmtTbl(), f, null,
                Integer.MAX_VALUE, null);
        if(tree == null) {
        		System.err.println("No tree for "+formulaStr);
        		return null;
        }
        return shortestRegion(tree.getRoot(), start, end, new int[] { 0 }, offset, length, mode);
	}
	
	/**
	 * Returns the shortest region within node n containing the region given by offset/length
	 */
	public static IRegion shortestRegion(ParseNode n, int[] start, int[] end, int[] index, int offset, int length, int mode) {
		final Formula f = n.stmt.getFormula();
        int elementIndex = 0;
        int startIndex = index[0];
		final int[] reseq = n.stmt instanceof Axiom ? ((Axiom)n.stmt).getSyntaxAxiomVarHypReseq() : null;
		for (int i = 1; i < f.getCnt(); i++) {
            final Sym s = f.getSym()[i];
            if (s instanceof Var && !(n.stmt instanceof VarHyp)) {
            		IRegion region = shortestRegion(n.child[reseq == null ? elementIndex++
                    : reseq[elementIndex++]], start, end, index, offset, length, mode);
            		if(region != null) return region;
            }
            else {
            		index[0]++;
            }
        }
		boolean left = start[startIndex+1] < offset && end[index[0]] >= offset+length;
		boolean right = start[startIndex+1] <= offset && end[index[0]] > offset+length;
		boolean smaller;
		switch(mode) {
		case LEFT: smaller = left; break;
		case RIGHT: smaller = right; break;
		case LEFT_OR_RIGHT: default: smaller = left || right; break;
		}
		if(smaller) return new Region(start[startIndex+1], end[index[0]]-start[startIndex+1]);
		return null;
	}
}

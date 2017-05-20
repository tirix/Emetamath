//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Axiom.java  0.03 11/01/2006
 *
 *  5-Dec-2005: --> added isAxiom() for ProofAsst
 *
 *  Version 0.03 -- 11/01/2006:
 *      Aug-27-2006: - added renderParsedSubExpr() for TMFF project
 *      Oct-06-2006: - added  getWidthOfWidestExprCnst() for
 *                     TMFF project
 */

package mmj.lang;

import java.util.*;

/**
 *  Axiom embodies Metamath $a statements, the "axiomatic
 *  assertions".
 *  <p>
 *  There are actually several different kinds of Axioms:
 *  Syntax Axioms, Definitions and Logical Axioms. mmj does
 *  not distinguish (presently) between Definitions and
 *  Logical Axioms, but mmj.verify.Grammar is *very* interested
 *  in Syntax Axioms :)
 *  <p>
 *  Some Syntax Axiom specific information is stored here.
 *  The Syntax related information is stored by
 *  mmj.verify.Grammar and is generally unavailable until
 *  the grammar has been successfully initialized.
 *  <p>
 *  A few more words are in order...to save space later...
 *  <p>
 *  <u><b><code>int[]  syntaxAxiomVarHypReseq</code></b></u>
 *  Array of indexes for resequencing a Syntax Axiom's
 *  VarHyp's from order of appearance in the Axiom's
 *  Formula to database sequence.
 *  <p>
 *  Stmt VarHypArray's are stored in .seq order -- appearance
 *  in database. Counter-intuitively, RPN's must adhere to
 *  the same sequence for variables -- i.e. "wph wps wi" is
 *  correct because wph appears before wps in the database,
 *  not because ph appears before ps in wi.
 *  <p>
 *  For each Notation Syntax Axiom whose formula's variables
 *  appear in a different order than their variable hypotheses
 *  database sequence -- that is, for Notation Syntax Axioms
 *  whose variables must be resequenced -- this array is
 *  present; for all others, it is null (that means that
 *  we will not store arrays such as [ 0, 1, 2] because
 *  no resequencing need be done.
 *  <p>
 *  Example: wsbc
 *  <code>
 *   Notation=wsbc, Typ wff:  [ class ___ / set ___ ] wff ___
 *   Axiom        seq= 27140 label=wsbc formula=wff [ A / x ] ph
 *       VarHyp   seq= 120   label=wph  formula=wff ph
 *       VarHyp   seq= 12840 label=vx   formula=set x
 *       VarHyp   seq= 19860 label=cA   formula=class A
 *   syntaxAxiomVarHypReseq = {2, 1, 0}
 *  </code>
 *  <p>
 *   Therefore...Stmt.exprRPN would be displayed as
 *   "wph vx cA wsbc" AND
 *   when wsbc is used to create a more complex formula,
 *   the RPN for that formula will reflect the resequenced
 *   VarHyp's.
 *  <p>
 *  NOTE: The reason for all of this is the way the Proof Verifier
 *        works: hypotheses are pushed onto the referenced
 *        assertion's hypothesis stack in database sequence,
 *        and substitutions from the Proof Work stack are
 *        made positionally. The RPN's *must* reflect this
 *        or else they would substitute expressions for
 *        the wrong variables.
 *  <p>
 *  NOTE2: A "hidden" requirement for axioms we deem "Notation
 *         Syntax Axioms" is that no variable can appear more than
 *         once in the formula. This is seemingly paradoxical
 *         because Notation Axiom variables are positional -- the
 *         variable type matters, not the name -- except for the
 *         problem of .seq on the hypothesis stack. Think about it:
 *         if "wi" were coded as "wi $a wff ( ph -> ph ) $." would
 *         it be saying that there is one variable or two? And if
 *         two, what would be their sequence on the hypothesis
 *         stack. Thus, we'll have to have a validation error
 *         message for this scenario!
 *
 *
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public class Axiom extends Assrt {

    /**
     *  Array of indexes for resequencing a Syntax Axiom's
     *  VarHyp's from order of appearance in the Axiom's
     *  Formula to database sequence.
     */
    private   int[]  syntaxAxiomVarHypReseq;

    /**
     *  Is this Axiom a "Syntax Axiom"? Hmmm...
     *  <p>
     *  An Axiom is a "Syntax Axiom" if its Type Code is
     *  not equal to a mmj.verify.Grammar "provableLogicStmtTyp"
     *  (i.e. "|-", the default.)
     *  @see mmj.verify.Grammar
     */
    private   boolean isSyntaxAxiom;

    /**
     *  Set to true for Syntax Axiom whose Formula Expression
     *  contains a Sym such that
     *  ((Cnst)sym[i])).getNbrOccInSyntaxAxioms == 1.
     */
    private   boolean syntaxAxiomHasUniqueCnst;

    /**
     *  Used for the TMFF project, and pertains only to
     *  Syntax Axioms.
     *  <p>
     *  Contains the number of characters used by the longest
     *  Cnst in the Axiom's Formula.
     *  <p>
     *  To make things simple, we just compute it at start-up
     *  for all Axioms. In theory we could compute it only
     *  if needed, and also init it to -1 so that the computation
     *  is only done once, but there just aren't that many
     *  Axioms and the computation is quick enough that it
     *  must as well just get done and stay done.
     */
    private   int     widthOfWidestExprCnst;

    /**
     *  Construct using the whole enchilada of parameters!
     *
     *  @param seq          MObj.seq sequence number
     *  @param scopeDefList Scope info in effect at the time
     *  @param symTbl       Symbol Table (Map)
     *  @param stmtTbl      Statement Table (Map)
     *  @param labelS       Axiom label String
     *  @param typS         Axiom Formula Type Code String
     *  @param symList      Axiom Expression Sym String List.
     */
    public Axiom(int       seq,
                 ArrayList scopeDefList,
                 Map       symTbl,
                 Map       stmtTbl,
                 String    labelS,
                 String    typS,
                 ArrayList symList)
                                throws LangException {
        super(seq,
              scopeDefList,
              symTbl,
              stmtTbl,
              labelS,
              typS,
              symList);

        widthOfWidestExprCnst     =
            formula.computeWidthOfWidestExprCnst();
    }

    /**
     *  Return Axiom's syntaxAxiomVarHypReseq.
     *  <p>
     *  Array of indexes for resequencing a Syntax Axiom's
     *  VarHyp's from order of appearance in the Axiom's
     *  Formula to database sequence.
     *
     *  @return Axiom's syntaxAxiomVarHypReseq (may be null).
     */
    public int[] getSyntaxAxiomVarHypReseq() {
        return syntaxAxiomVarHypReseq;
    }

    /**
     *  Set Axiom's syntaxAxiomVarHypReseq (may be null).
     *  <p>
     *  Array of indexes for resequencing a Syntax Axiom's
     *  VarHyp's from order of appearance in the Axiom's
     *  Formula to database sequence.
     *
     *  @param syntaxAxiomVarHypReseq  array of int.
     */
    public void setSyntaxAxiomVarHypReseq(
             int[] syntaxAxiomVarHypReseq) {
        this.syntaxAxiomVarHypReseq =
             syntaxAxiomVarHypReseq;
    }

    /**
     *  Is the Assrt an Axiom?
     *  <p>
     *  Someone asked the question, so it is answered...
     *
     *  @return true (this is an Axiom :)
     */
    public boolean isAxiom() {
        return true;
    }

    /**
     *  Answer, Is Axiom a Syntax Axiom?
     *  <p>
     *  An Axiom is a "Syntax Axiom" if its Type Code is
     *  not equal to a mmj.verify.Grammar "provableLogicStmtTyp"
     *  (i.e. "|-", the default -- see mmj.verify.Grammar).
     *  <p>
     *  This answer is unavailable until Grammar has been
     *  initialized successfully.
     *
     *  @return isSyntaxAxiom true or false.
     */
    public boolean getIsSyntaxAxiom() {
        return isSyntaxAxiom;
    }

    /**
     *  Set isSyntaxAxiom, true or false.
     *
     *  An Axiom is a "Syntax Axiom" if its Type Code is
     *  not equal to a mmj.verify.Grammar "provableLogicStmtTyp"
     *  (i.e. "|-", the default -- see mmj.verify.Grammar).
     *
     *  @param isSyntaxAxiom true or false.
     */
    public void setIsSyntaxAxiom(boolean isSyntaxAxiom) {
        this.isSyntaxAxiom = isSyntaxAxiom;
    }

    /**
     *  Return syntaxAxiomHasUniqueCnst, true or false.
     *  <p>
     *  True for Syntax Axiom whose Formula Expression
     *  contains a Sym such that
     *  ((Cnst)sym[i])).getNbrOccInSyntaxAxioms == 1.
     *
     *  @return syntaxAxiomHasUniqueCnst true or false.
     */
    public boolean getSyntaxAxiomHasUniqueCnst() {
        return        syntaxAxiomHasUniqueCnst;
    }

    /**
     *  Set syntaxAxiomHasUniqueCnst, true or false.
     *  <p>
     *  True for Syntax Axiom whose Formula Expression
     *  contains a Sym such that
     *  ((Cnst)sym[i])).getNbrOccInSyntaxAxioms == 1.
     *
     *  @param syntaxAxiomHasUniqueCnst true or false.
     */
    public void setSyntaxAxiomHasUniqueCnst(
           boolean syntaxAxiomHasUniqueCnst) {
        this.syntaxAxiomHasUniqueCnst =
             syntaxAxiomHasUniqueCnst;
    }

    /**
     *  Converts a parse sub-tree into a sub-expression
     *  which is output into a String Buffer.
     *  <p>
     *  Note: this will not work for a proof node! The
     *        ParseNode's stmt must be a VarHyp or
     *        a Syntax Axiom.
     *  <p>
     *  The output sub-expression is generated into
     *  text not to exceed the given maxLength. If the
     *  number of output characters exceeds maxLength
     *  output terminates, possibly leaving a dirty
     *  StringBuffer.
     *  <p>
     *  The depth of the sub-tree is checked against
     *  the input maxDepth parameter, and if the depth
     *  exceeds this number, output terminates,, possibly
     *  leaving a dirty StringBuffer.
     *  <p>
     *  Depth is computed as 1 for each Notation Syntax
     *  Axiom Node. VarHyp nodes and Nulls Permitted,
     *  Type Conversion and NamedTypedConstant Syntax
     *  Axiom nodes are assigned depth = 0 for purposes of
     *  depth checking.
     *  <p>
     *  @param sb            StringBuffer already initialized
     *                       for appending characters.
     *
     *  @param maxDepth      maximum depth of Notation Syntax
     *                       axioms in sub-tree to be printed.
     *                       Set to Integer.MAX_VALUE to turn
     *                       off depth checking.
     *
     *  @param maxLength     maximum length of output
     *                       sub-expression.
     *                       Set to Integer.MAX_VALUE to turn
     *                       off depth checking.
     *
     *  @param child         array of ParseNode, corresponding
     *                       to VarHyp nodes to be substituted
     *                       into the Stmt.
     *
     *  @return length of sub-expression characters
     *          appended to the input StringBuffer --
     *          or -1 if maxDepth or maxLength exceeded.
     */
    public int renderParsedSubExpr(StringBuffer sb,
                                   int          maxDepth,
                                   int          maxLength,
                                   ParseNode[]  child) {

        if (!getIsSyntaxAxiom()) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_BAD_PARSE_STMT_1);
        }

        /*
         *  For TypeConversion Syntax Axiom, make recursive
         *  call to output leaf node Var symbol
         *  (remember: Type Conversions can be chained!)
         */
        if (formula.sym.length == 2
            &&
            varHypArray.length == 1) {
            // is Type Conversion Syntax Axiom...has to be!
            return (child[0].getStmt()).renderParsedSubExpr(
                                              sb,
                                              maxDepth,
                                              maxLength,
                                              child[0].getChild());
        }

        /*
         *  Process the syntax axiom's expression, outputting
         *  the Cnst symbols and the children of this ParseNode
         *  which represent the substitutions into the variables
         *  of the syntax axiom. Note that the child nodes
         *  may be in a different sequence than the formula's
         *  variables, so resequence if necessary.
         */
        String    s;
        int       sLen;
        int       varCnt          = -1;
        ParseNode subNode;
        int       substNbrHyps;
        int       totSLen         = 0;
        for (int i = 1; i < formula.sym.length; i++) {
            if (formula.sym[i].isCnst()) {
                s                 = formula.sym[i].getId();
                sLen              = s.length() + 1;

                if (sLen > maxLength) {
                    return -1;
                }

                sb.append(' ');
                sb.append(s);
                maxLength        -= sLen;
                totSLen          += sLen;
                continue;
            }

            ++varCnt;
            if (syntaxAxiomVarHypReseq == null) {
                subNode           = child[varCnt];
            }
            else {
                subNode           =
                    child[syntaxAxiomVarHypReseq[varCnt]];
            }

            substNbrHyps          =
                subNode.getStmt().getMandVarHypArray().length;

            /* If child node is TypeConversion, NullsPermitted or
             * NamedTypedConstant Syntax Axiom substituting
             * into this variable we output it without checking
             * "maxDepth" -- and we don't count this node's depth.
             */
            if (substNbrHyps == 0  // all Cnst or is NullsPermitted
                   ||
                (substNbrHyps == 1 &&
                 subNode.getStmt().getFormula().getCnt() == 2)) {
                sLen              =
                    (subNode.getStmt()).renderParsedSubExpr(
                                              sb,
                                              maxDepth,
                                              maxLength,
                                              subNode.getChild());
            }
            else {
                /* See following call to outputSubExpr to
                 * see how maxDepth is recursively decremented.
                 */
                if (maxDepth < 2) { // we're at level 1 now, bye!
                    return -1;
                }
                sLen              =
                    (subNode.getStmt()).renderParsedSubExpr(
                                              sb,
                                              maxDepth - 1,
                                              maxLength,
                                              subNode.getChild());
            }

            if (sLen < 0) {
                return sLen;
            }

            maxLength            -= sLen;
            totSLen              += sLen;
        }

        return totSLen;
    }

    /**
     *  Gets the width in characters of the widest constant
     *  in the Axiom's Formula's Expression.
     *  <p>
     *  Used for the TMFF project, and used only with
     *  Syntax Axioms.
     *  <p>
     *  @return length in characters of the widest constant
     *          in the Axiom's Formula's Expression.
     */
    public int getWidthOfWidestExprCnst() {
        return widthOfWidestExprCnst;
    }
}

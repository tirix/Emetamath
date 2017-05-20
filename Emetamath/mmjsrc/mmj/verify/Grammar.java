//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Grammar.java  0.06 03/01/2008
 *
 *  Sep-25-2005:
 *  - comment typo fixed.
 *
 *  Version 0.04 -- Dec-14-2005:
 *               - change parse to return ParseTree
 *                 instead of RPN -- will store
 *                 tree instead of RPN to optimize
 *                 ProofAsst processing.
 *               - added parseFormulaWithoutSafetyNet()
 *                 for convenience of mmj.pa.ProofWorksheet.java.
 *                 NOTE: input to this include Hyp[] instead
 *                       of VarHyp[].
 *
 *  Version 0.05 -- 08/01/2007
 *               - Correct comment typo(s).
 *
 *  Version 0.06 -- 1-Mar-2008
 *               - Modify to compute ParseTrees' maxDepth and
 *                 levelOneTwo as well as assertions' logHypsMaxDepth
 *                 and logHypsL1HiLoKey at grammatical parse time
 *                 so that memory buffers aren't dirtied in
 *                 future usage with multi-core processors. Once
 *                 the input .mm file is loaded and parsed the
 *                 symTbl and stmtTbl data will remain constant
 *                 (unless add/upd/del capabilities are added
 *                 later.)
 */

package mmj.verify;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import mmj.lang.Assrt;
import mmj.lang.Axiom;
import mmj.lang.Cnst;
import mmj.lang.Formula;
import mmj.lang.Hyp;
import mmj.lang.LogHyp;
import mmj.lang.MObj;
import mmj.lang.MandFrame;
import mmj.lang.MessageHandler;
import mmj.lang.ParseNode;
import mmj.lang.ParseNodeHolder;
import mmj.lang.ParseTree;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.SyntaxVerifier;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.lang.VarHyp;
import mmj.lang.VerifyException;
import mmj.mmio.SourcePosition;

/**
 *  Grammar processes a mmj LogicalSystem, extracts a Grammar,
 *  which it also validates, and parses statements in the
 *  system.
 *  <p>
 *  The first thing to note is that Grammar builds a "grammar" --
 *  a set of replacement rules -- from a Metamath ".mm" file/database.
 *  The distinction made between "building" and extracting what
 *  is already there is important because Metamath's set.mm
 *  database uses "overloaded functions", such as "weq" and "wceq",
 *  which are tecnically "ambiguous" (two valid parse trees exist.)
 *  <p>
 *  These "non-essential" ambiguities are resolved during grammar
 *  generation by a process of "combinatorial explosion", which
 *  creates a Notation Rule for every permutation of input
 *  parameters, and then priorities those rules -- first come,
 *  first served, dropping any duplicates. Assuming that
 *  "weq" precedes "wceq" in the database, "set = set" will
 *  always parse to "weq" and the other variations such as
 *  "set = class", "class = set" will parse to "wceq".
 *  <p>
 *  The second important topic is that Grammar imposes new
 *  validation rules on a Metamath .mm file/database. But
 *  these rules never come into force unless a user desires
 *  to use Grammar to perform parsing (aka "syntactical analysis".)
 *  A user with a non-Grammar compliant database can still use
 *  mmj to perform Proof Verification and to print reports.
 *  <p>
 *  The additional mmj validation rules are not extremely restrictive,
 *  and in fact, the entire mmj system is designed to operate
 *  on set.mm with no changes to any default parameters or
 *  settings. Let me give examples.
 *  <p>
 *  The Metamath symbol "|-" termed "It is provable that..."
 *  is the default in mmj, and it is given a special name,
 *  the "provableLogicStmtTyp". If your .mm file uses a
 *  different symbol, you must tell mmj (via mmj.util.RunParmFile
 *  or a constructor parameter.)  Moreover, if your .mm
 *  file encodes a new logical theory involving TWO
 *  provableLogicStmtTyp codes, you're going to be hit with
 *  an error, and Grammar will refuse to continue. In addition,
 *  your provableLogicStmtTyp code cannot be the Type Code
 *  of any variables in your system -- you cannot define a
 *  Syntax Axiom for it, and you thus, cannot do a Meta-Metamath
 *  theory using that approach (not directly anyway.) There
 *  may be an important need for two provableLogicStmtTyp
 *  codes in a single .mm file, but my imagination is limited,
 *  and so was my time. Another restriction is using a grammatical
 *  Type Code as a constant in an expression, for example, like
 *  this: "xyz $p |- ( wff -> ph ) $.", where "wff" is a
 *  Type Code for "well-formed formula" variables. See
 *  ConsolidatedListOfGrammarValidations.html for more info.
 *  <p>
 *  One excuse I must make is that the Grammar Rule Trees and
 *  Forests (mmj.verify.GRForest.java and mmj.verify.GRNode.java)
 *  were part of the BottomUpParser.java debacle.
 *  They are still used for detecting duplicate NotationRules
 *  and for printing out the rules, but if I were starting
 *  from scratch I would not use them.
 *  <p>
 *  Finally, mmj.verify.GrammarAmbiguity.java is mostly
 *  a stub -- incomplete. There is a lot of work needed to
 *  detect and report on grammatical ambiguities, we've just
 *  scratched the surface. In fact, according to a famous
 *  parsing techniques expert, developing a generalized
 *  program able to process any context-free grammar and decide
 *  whether it is unambiguous is mathematically impossible.
 *  So that is definitely an opportunity for achievement,
 *  right there. It will take a bit longer than initially
 *  expected :)
 *
 *  @see <a href="../../ConsolidatedListOfGrammarValidations.html">
 *       ConsolidatedListOfGrammarValidations.html</a>
 *  @see <a href="../../BasicsOfSyntaxAxiomsAndTypes.html">
 *       BasicsOfSyntaxAxiomsAndTypes.html</a>
 *  @see <a href="../../EssentialAmbiguityExamples.html">
 *       EssentialAmbiguityExamples.html</a>
 *  @see <a href="../../CreatingGrammarRulesFromSyntaxAxioms.html">
 *       CreatingGrammarRulesFromSyntaxAxioms.html</a>
 *  @see <a href="../../GrammarRuleTreeNotes.html">
 *       GrammarRuleTreeNotes.html</a>
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public class Grammar implements SyntaxVerifier {

    private   String[] provableLogicStmtTypCodes;

    private   String[] logicStmtTypCodes;

    private   boolean  doCompleteGrammarAmbiguityEdits;
    private   boolean  doCompleteStmtAmbiguityEdits;


    /**
     *  Size of parseTreeArray used during grammatical parsing of
     *  non-Syntax Axiom statements.
     *
     *  Limits the search for a
     *  grammatical parse for a statement to 1 or 2 (based on
     *  doCompleteGrammarAmbiguityEdits). If set to 1 then the
     *  first successful grammatical parse of a statement is
     *  returned, presumably on the basis of a need for speed
     *  after having already proven that the grammar is totally,
     *  100% unambiguous. A setting of 2 is used when
     *  doCompleteGrammarAmbiguityEdits is set to true and
     *  amounts to a belt-and-suspenders re-proof of a grammar
     *  by checking every possibility during a statement's
     *  grammatical parse -- and continuing the search for
     *  parse trees even after the first is found.
     */
    private   int      parseTreeMax;

    /**
     *  grammarInitialized -
     *  State variable: has initialization of the grammar
     *  related data been performed, true or false.
     *  <p>
     *  This is
     *  needed because a) SyntaxVerifier.verifyOneFormulaSyntax
     *  could be called at any time, even before initialization,
     *  which must take place before non-Syntax statements
     *  can be parsed; and b) SyntaxVerifier updates elements
     *  of LogicalSystem.symTbl and LogicalSystem.stmtTbl,
     *  and initialization is required before verification
     *  (and since there is no rollback capability, we require
     *  want a clean slate before beginning the updates.)
     *
     * @see mmj.lang.LogicalSystem
     */
    private     boolean     grammarInitialized;

    //Grammar "state" variables (not counting those stored in
    //Cnst and Axiom.
    private     Cnst[]      provableLogicStmtTypArray;
    private     Cnst[]      logicStmtTypArray;
    private     TreeSet     varHypTypSet;
    private     TreeSet     syntaxAxiomTypSet;
    private     TreeSet     nullsPermittedTypSet;
    private     ArrayList   nullsPermittedGRList;
    private     ArrayList   typeConversionGRList;
    private     TreeSet     notationGRSet;
//  private     TreeSet     baseGRSet;
    private     int         notationGRGimmeMatchCnt;

    //global variables stored here for mere convenience
    private     Map         symTbl;

    private     Map         stmtTbl;
    private     MessageHandler messageHandler;

    private     int         lastGrammarRuleNbr = 0;
    public int assignNextGrammarRuleNbr() {
        return ++lastGrammarRuleNbr;
    }

    private     GrammaticalParser grammaticalParser;

    /**
     *  Computed maximum?
     */
    private     int         maxFormulaCnt = 0;

    /**
     * derivedRuleQueue --
     * Sorts "pending" derived GrammarRules for
     * processing in maxSeqNbr and ruleNbr order
     * which ensures that the Grammar gives
     * precedence to earlier Metamath Syntax Axioms
     * over later.
     */
    private   PriorityQueue derivedRuleQueue;

    /**
     * Constructor.
     *
     * @param provableLogicStmtTypCodes   lists Type Codes used on
     *          Theorems involving Logical Statements
     *          thus identifying all other Type Codes as
     *          Syntax Type Codes -- if array is empty or null,
     *          the default is "|-".
     *
     * @param logicStmtTypCodes - lists Type Codes used on
     *          statements of logic (such as "wff" but not "set",
     *          etc) -- if array is empty or null, the default is "wff".
     *
     * @param doCompleteGrammarAmbiguityEdits   Thorough (attempt to)
     *          prove grammar is not Ambiguous. If Ambiguity is found,
     *          an error message is generated (hopefully listing a formula
     *          with two different grammatical parsings in RPN format.)
     *          These edits are redundant unless the grammar or software
     *          have changed since the last edit; and they may be long and
     *          tedious, which is why this option is provided. The default
     *          is True.
     *
     * @param doCompleteStmtAmbiguityEdits   Check each non-Grammar Stmt
     *          being parsed to see if there are two different, valid
     *          parse trees -- meaning that the Stmt is ambiguous, an
     *          error. Note that this option will often require much more
     *          time due to generation of many duplicate parse trees
     *          (not an error).
     */
    public Grammar(String[] provableLogicStmtTypCodes,
                   String[] logicStmtTypCodes,
                   boolean  doCompleteGrammarAmbiguityEdits,
                   boolean  doCompleteStmtAmbiguityEdits) {

        this.provableLogicStmtTypCodes = provableLogicStmtTypCodes;
        this.logicStmtTypCodes         = logicStmtTypCodes;
        editGrammarConstructorTypParams();

        this.doCompleteGrammarAmbiguityEdits =
             doCompleteGrammarAmbiguityEdits;

        this.doCompleteStmtAmbiguityEdits =
             doCompleteStmtAmbiguityEdits;

        if (doCompleteStmtAmbiguityEdits) {
            parseTreeMax          =
                GrammarConstants.PARSE_TREE_MAX_FOR_AMBIG_EDIT;
        }
        else {
            parseTreeMax          = 1; //faster, takes first parse
        }

    }

    /**
     * Constructor using default doCompleteStmtAmbiguityEdits value.
     *
     * @param provableLogicStmtTypCodes   lists Type Codes used on
     *          Theorems involving Logical Statements
     *          thus identifying all other Type Codes as
     *          Syntax Type Codes -- if array is empty or null,
     *          the default is "|-".
     *
     * @param logicStmtTypCodes - lists Type Codes used on
     *          statements of logic (such as "wff" but not "set",
     *          etc) -- if array is empty or null, the default is "wff".
     *
     * @param doCompleteGrammarAmbiguityEdits   Thorough (attempt to)
     *          prove grammar is not Ambiguous. If Ambiguity is found,
     *          an error message is generated (hopefully listing a formula
     *          with two different grammatical parsings in RPN format.)
     *          These edits are redundant unless the grammar or software
     *          have changed since the last edit; and they may be long and
     *          tedious, which is why this option is provided. The default
     *          is True.
     *
     */
    public Grammar(String[] provableLogicStmtTypCodes,
                   String[] logicStmtTypCodes,
                   boolean  doCompleteGrammarAmbiguityEdits) {

        this(provableLogicStmtTypCodes,
             logicStmtTypCodes,
             doCompleteGrammarAmbiguityEdits,
             GrammarConstants.DEFAULT_COMPLETE_STATEMENT_AMBIG_EDITS);

    }

    /**
     * Constructor using default doCompleteStmtAmbiguityEdits and
     * doCompleteGrammarAmbiguityEdits values.
     *
     * @param provableLogicStmtTypCodes   lists Type Codes used on
     *          Theorems involving Logical Statements
     *          thus identifying all other Type Codes as
     *          Syntax Type Codes -- if array is empty or null,
     *          the default is "|-".
     *
     * @param logicStmtTypCodes - lists Type Codes used on
     *          statements of logic (such as "wff" but not "set",
     *          etc) -- if array is empty or null, the default is "wff".
     *
     */
    public Grammar(String[] provableLogicStmtTypCodes,
                   String[] logicStmtTypCodes) {

        this(provableLogicStmtTypCodes,
             logicStmtTypCodes,
             GrammarConstants.DEFAULT_COMPLETE_GRAMMAR_AMBIG_EDITS,
             GrammarConstants.DEFAULT_COMPLETE_STATEMENT_AMBIG_EDITS);

    }

    /**
     * Constructor using all default values for
     * doCompleteStmtAmbiguityEdits,
     * doCompleteGrammarAmbiguityEdits,
     * provableLogicStmtTyp Code (ie "|-") and
     * logicStmtTyp Code (ie "wff").
     */
    public Grammar() {
        this(GrammarConstants.DEFAULT_PROVABLE_LOGIC_STMT_TYP_CODES,
             GrammarConstants.DEFAULT_LOGIC_STMT_TYP_CODES,
             GrammarConstants.DEFAULT_COMPLETE_GRAMMAR_AMBIG_EDITS,
             GrammarConstants.DEFAULT_COMPLETE_STATEMENT_AMBIG_EDITS);

    }

    /**
     *  Return grammarInitialized flag.
     *
     *  @return grammarInitialized flag.
     */
    public boolean getGrammarInitialized() {
        return grammarInitialized;
    }

    /**
     *  Set grammarInitialized flag to false.
     *
     *  (Don't allow it to be set to true externally.)
     */
    public void setGrammarInitializedFalse() {
        grammarInitialized = false;
    }

    /**
     *  Initialize derivedRuleQueue.
     */
    public void derivedRuleQueueInit() {
        derivedRuleQueue =
                new PriorityQueue(
                    GrammarConstants.MAX_DERIVED_RULE_QUEUE_SIZE,
                    GrammarRule.MAX_SEQ_NBR);
    }

    /**
     *  Set derivedRuleQueue.
     *
     *  @param p  PriorityQueue.
     */
    public void setDerivedRuleQueue(PriorityQueue p) {
        derivedRuleQueue = p;
    }

    /**
     *  Add rule to derivedRuleQueue.
     *
     *  @param gR GrammarRule to add.
     */
    public void derivedRuleQueueAdd(GrammarRule gR) {
        derivedRuleQueue.offer(gR);
    }

    /**
     *  Read GrammarRule from derivedRuleQueue.
     *
     *  @return GrammarRule, or null if queue empty.
     */
    public GrammarRule derivedRuleQueueRead() {
        return (GrammarRule)derivedRuleQueue.poll();
    }

    /**
     *  Return size of Symbol Table.
     *
     *  @return size of Symbol Table.
     */
    public int getSymTblSize() {
        return symTbl.size();
    }


    /**
     *  Add NotationRule to Notation Rule Set.
     *
     *  @param notationRule to add to set.
     */
    public void notationGRSetAdd(NotationRule notationRule) {
        notationGRSet.add(notationRule);
    }

    /**
     *  Return NotationRule Set.
     *
     *  @return Set of NotationRules.
     */
    public Set getNotationGRSet() {
        return notationGRSet;
    }

    /**
     * add
     */
//  public void baseGRSetAdd(GrammarRule grammarRule) {
//      baseGRSet.add(grammarRule);
//  }

    /**
     *  getBaseGRSet
     *  Return global List of BaseRules
     */
//  public Set getBaseGRSet() {
//      return baseGRSet;
//  }

    /**
     *  Increment (add 1) to Count to NotationRules deemed to
     *  be "gimme matches".
     */
    public void incNotationGRGimmeMatchCnt() {
        ++notationGRGimmeMatchCnt;
    }

    /**
     *  Set Count of NotationRules deemed to
     *  be "gimme matches".
     *
     *  @param cnt Count of NotationRules deemed to
     *             be "gimme matches".
     */
    public void setNotationGRGimmeMatchCnt(int cnt) {
        notationGRGimmeMatchCnt = cnt;
    }

    /**
     *  Get Count of NotationRules deemed to
     *  be "gimme matches".
     *
     *  @return Count of NotationRules deemed to
     *             be "gimme matches".
     */
    public int getNotationGRGimmeMatchCnt() {
        return notationGRGimmeMatchCnt;
    }


    /**
     *  Add TypeConversionRule to List.
     *
     *  @param typeConversionRule to be added.
     *
     *  @return index of new rule in list.
     */
    public int typeConversionGRListAdd(
                          TypeConversionRule typeConversionRule) {
        int ruleIndex = typeConversionGRList.size();
        typeConversionGRList.add(typeConversionRule);
        return ruleIndex;
    }

    /**
     *  Return TypeConversionRule List.
     *
     *  @return TypeConversionRule List.
     */
    public List getTypeConversionGRList() {
        return typeConversionGRList;
    }


    /**
     *  Add NullsPermittedRule to List.
     *
     *  @param r NullsPermittedRule to be added.
     *
     *  @return index of new NullsPermittedRule.
     */
    public int nullsPermittedGRListAdd(NullsPermittedRule r) {
        int ruleIndex = nullsPermittedGRList.size();
        nullsPermittedGRList.add(r);
        return ruleIndex;
    }

    /**
     *  Return NullsPermittedRule List.
     *
     *  @return NullsPermittedRule List.
     */
    public List getNullsPermittedGRList() {
        return nullsPermittedGRList;
    }

    /**
     *  Return NullsPermittedRule Type Code Set.
     *
     *  This is the set of Type Codes for which Nulls
     *  are permitted.
     *
     *  @return NullsPermittedRule Type Code Set.
     */
    public Set getNullsPermittedTypSet() {
        return nullsPermittedTypSet;
    }


    /**
     *  Return VarHyp Type Code Set.
     *
     *  This is the set of Type Codes with defined VarHyp's.
     *
     *  @return VarHyp Type Code Set.
     */
    public Set getVarHypTypSet() {
        return varHypTypSet;
    }

    /**
     *  Return Syntax Axiom Type Code Set.
     *
     *  This is the set of Type Codes for Syntax Axioms are
     *  defined.
     *
     *  @return Syntax Axiom Type Code Set.
     */
    public Set getSyntaxAxiomTypSet() {
        return syntaxAxiomTypSet;
    }

    /**
     *  Return Array of Logic Stmt Type Codes (like "wff").
     *
     *  At this time, the array will have exactly one element.
     *
     *  @return Array of Logic Stmt Type Codes.
     */
    public Cnst[] getLogicStmtTypArray() {
        return logicStmtTypArray;
    }

    /**
     *  Return Array of Provable Logic Stmt Type Codes (like "|-").
     *
     *  At this time, the array will have exactly one element.
     *
     *  @return Array of Logic Stmt Type Codes.
     */
    public Cnst[] getProvableLogicStmtTypArray() {
        return provableLogicStmtTypArray;
    }

    /**
     *  Parse a single Statement.
     *  <p>
     *  If used with VarHyp or SyntaxAxiom, simply
     *  returns stmt.getExprParseTree().
     *  <p>
     *  Note: access to symTbl and stmtTbl is required in
     *        case the grammar needs to be re-initialized.
     *
     *  @param messageHandler MessageHandler object for error/info messages.
     *  @param symTbl      Symbol Table (Map).
     *  @param stmtTbl     Statement Table (Map).
     *  @param stmt        Stmt in stmtTbl to parse.
     *
     *  @return ParseTree of Stmt
     */
    public ParseTree parseOneStmt(MessageHandler messageHandler,
                                  Map       symTbl,
                                  Map       stmtTbl,
                                  Stmt      stmt) {

        checkVerifySyntaxParams(messageHandler,
                                symTbl,
                                stmtTbl);
        if (!grammarInitialized) {
            try {
                initializeGrammarTables();
            }
            catch (VerifyException e) {
            	// TODO I believe Verify exception is never thrown in the catch block
                accumErrorMsgInList(null, e.getMessage());
            }
            if (!grammarInitialized) {
                return null;
            }
        }

        if (stmt instanceof VarHyp
            ||
            (stmt instanceof Axiom &&
             ((Axiom)stmt).getIsSyntaxAxiom())) {
//          return stmt.getExprRPN();
            return stmt.getExprParseTree();
        }

        return grammaticalParseOneFormula(
                   stmt.getFormula(),
                   stmt.getMandVarHypArray(),
                   stmt.getSeq(),
                   stmt.getLabel(),
                   stmt.getPosition());

    }

    /**
     *  Parse a single formula.
     *  <p>
     *  Note: access to symTbl and stmtTbl is required in
     *        case the grammar needs to be re-initialized.
     *  <p>
     *  Note: <b>highestSeq</b> is similar to the restriction in
     *        proof verification: a proof can only refer to
     *        previous statements. However, setting this to
     *        java.lang.Integer.MAX_VALUE says, parse this
     *        formula with the entire grammar -- which ought
     *        to result in the same parse, unless the
     *        grammar is ambiguous (new Grammar Rules should
     *        be "disjoint" from previous ones.)
     *
     *  @param messageHandler MessageHandler object for error/info messages.
     *  @param symTbl      Symbol Table (Map).
     *  @param stmtTbl     Statement Table (Map).
     *  @param formula     Formula to parse.
     *  @param varHypArray VarHyp's for the Formula's Var's.
     *  @param highestSeq  Max MObj.seq that can be referenced.
     *  @param defaultStmt Default Stmt for output RPN/message.
     *
     *  @return ParseTree of Stmt containing RPN.
     */
    public ParseTree parseFormula(MessageHandler messageHandler,
                                  Map       symTbl,
                                  Map       stmtTbl,
                                  Formula   formula,
                                  VarHyp[]  varHypArray,
                                  int       highestSeq,
                                  Stmt      defaultStmt) {


        checkVerifySyntaxParams(messageHandler,
                                symTbl,
                                stmtTbl);
        if (!grammarInitialized) {
            try {
                initializeGrammarTables();
            }
            catch (VerifyException e) {
            	// TODO I believe Verify exception is never thrown in the catch block
                accumErrorMsgInList(null, e.getMessage());
            }
            if (!grammarInitialized) {
                return null;
            }
        }
        return grammaticalParseOneFormula(formula,
                                          varHypArray,
                                          highestSeq,
                                          defaultStmt.getLabel(),
                                          defaultStmt.getPosition());
    }

    /**
     *  Parse all Statement Formulas and update stmtTbl with results.
     *  <p>
     *
     *  @param messageHandler MessageHandler object for error/info messages.
     *  @param symTblParam  Symbol Table (Map).
     *  @param stmtTblParam Statement Table (Map).
     */
    public void parseAllFormulas(MessageHandler messageHandler,
                                 Map      symTblParam,
                                 Map      stmtTblParam) {
        checkVerifySyntaxParams(messageHandler,
                                symTblParam,
                                stmtTblParam);
        if (!grammarInitialized) {
            try {
                initializeGrammarTables();
            }
            catch (VerifyException e) {
            	// TODO I believe Verify exception is never thrown in the catch block
                accumErrorMsgInList(null, e.getMessage());
            }
            if (!grammarInitialized) {
                return;
            }
        }

        Stmt        stmt;
        Assrt       assrt;
        VarHyp[]    varHypArray;

        ParseTree   exprParseTree;

        TreeSet     stmtTblBySeq    = new TreeSet(Stmt.SEQ);
        stmtTblBySeq.addAll(stmtTbl.values());

        Iterator    i               = stmtTblBySeq.iterator();
        nextStmt:   while (i.hasNext() &&
                           !messageHandler.maxErrorMessagesReached()) {

            stmt                    = (Stmt)i.next();
            varHypArray             = stmt.getMandVarHypArray();
            if (stmt  instanceof VarHyp
                ||
                (stmt instanceof Axiom &&
                 ((Axiom)stmt).getIsSyntaxAxiom())) {
                // already done during initializeGrammarTables()
                continue nextStmt;
            }
            else {
                exprParseTree       =
                          grammaticalParseOneFormula(
                                              stmt.getFormula(),
                                              varHypArray,
                                              stmt.getSeq(),
                                              stmt.getLabel(),
                                              stmt.getPosition());

            }
            if (exprParseTree == null) {
                exprParseTree     =
                        buildDefaultExprParseTree(stmt,
                                                  varHypArray);
            }
            stmt.setExprParseTree(exprParseTree);

            // Prime these values so they aren't computed later
            // (and so that the buffers don't get dirtied later).
            // The "get" routines cache results...so...
            exprParseTree.getMaxDepth();
            exprParseTree.getLevelOneTwo();
            if (stmt.isAssrt()) {
                assrt             = (Assrt)stmt;
                assrt.getLogHypsMaxDepth();
                assrt.getLogHypsL1HiLoKey();
            }
        }
    }

    /**
     *  Initializes the grammar.
     *  <p>
     *  Normally this is handled automatically. It initializes
     *  "the grammar" but does not parse every Stmt in stmtTbl.
     *  <p>
     *  The intended use of this function would be to
     *  initialize the grammar without parsing every statement
     *  (parsing every statement in set.mm requires 8ish seconds!)
     *
     *  @param messageHandler MessageHandler object for error/info messages.
     *  @param symTblParam  Symbol Table (Map).
     *  @param stmtTblParam Statement Table (Map).
     *
     *  @return true if grammar initializes successfully, else false.
     */
    public boolean initializeGrammar(MessageHandler messageHandler,
                                     Map      symTblParam,
                                     Map      stmtTblParam) {
        grammarInitialized = false;

        checkVerifySyntaxParams(messageHandler,
                                symTblParam,
                                stmtTblParam);
        try {
            initializeGrammarTables();
        }
        catch (VerifyException e) {
        	// TODO I believe Verify exception is never thrown in the catch block
            accumErrorMsgInList(null, e.getMessage());
        }
        return grammarInitialized;
    }


    /**
     *  Alternate access to parse algorithm for syntax axiom/
     *  grammar rule type expressions which considers a
     *  the existence of a parse tree to be an error.
     *
     *  @param formulaTyp Type Code of the Expression to be parsed.
     *  @param parseNodeHolderExpr Expression rewritten with VarHyp's
     *                             replacing Var's.
     *  @param highestSeq Maximum Stmt.seq that may be used to
     *                    parse the expression.
     *
     *  @param syntaxAxiomLabel Parent label of grammar rule.
     *
     *  @return String error message if expression parseable.
     */
    String grammaticalParseSyntaxExpr(
                             Cnst              formulaTyp,
                             ParseNodeHolder[] parseNodeHolderExpr,
                             int               highestSeq,
                             String            syntaxAxiomLabel)
                                throws VerifyException {

        ParseTree[] parseTreeArray
                                  = new ParseTree[
                    GrammarConstants.PARSE_TREE_MAX_FOR_AMBIG_EDIT];

        int parseTreeCnt          =
            grammaticalParser.parseExpr(parseTreeArray,
                                        formulaTyp,
                                        parseNodeHolderExpr,
                                        highestSeq, 
                                        false);
        if (parseTreeCnt <= 0) {
            return null;
        }
        if (parseTreeCnt == 1) {
            if (parseTreeArray[0].getRoot().getStmt().getLabel()
                == syntaxAxiomLabel) {
                return null;
            }
            return new String(
                GrammarConstants.ERRMSG_GRAMMAR_RULE_PARSEABLE_1
                + syntaxAxiomLabel
                + GrammarConstants.ERRMSG_GRAMMAR_RULE_PARSEABLE_2
                + parseTreeArray[0].toString());
        }
        return new String(
            GrammarConstants.ERRMSG_GRAMMAR_RULE_2_PARSEABLE_1
            + syntaxAxiomLabel
            + GrammarConstants.ERRMSG_GRAMMAR_RULE_2_PARSEABLE_2
            + parseTreeArray[0].toString()
            + GrammarConstants.ERRMSG_GRAMMAR_RULE_2_PARSEABLE_3
            + parseTreeArray[1].toString()
            );
    }


    /**
     *  Alternate access to parse algorithm for use in
     *  Proof Assistant with no pre-check of Grammar
     *  initialization or error messageHandler.
     *
     *  This method assumes that we're being invoked from
     *  someplace like BatchMMJ2 that has already confirmed
     *  that the Grammar is initialized. In theory this
     *  could cause a problem if a non-syntax error comes
     *  out and is taken as a syntax error by the user.
     *  However, with the current parser that should not
     *  be a problem (as seen in the following method).
     *
     *  @param formula formula to be parsed.
     *  @param hypArray Hyp's for Formula's Var's.
     *  @param highestSeq Maximum Stmt.seq that may be used to
     *                    parse the expression.
     *
     *  @return ParseTree or null if parse errors.
     */
    public ParseTree parseFormulaWithoutSafetyNet(
                                     Formula   formula,
                                     Hyp[]     hypArray,
                                     int       highestSeq) {
        ParseTree   exprParseTree = null;

        ParseTree[] parseTreeArray
                                  = new ParseTree[1];
        int         parseTreeCnt;
        try {
            parseTreeCnt = grammaticalParser.parseExpr(
                        parseTreeArray,
                        formula.getTyp(),
                        formula.getParseNodeHolderExpr(hypArray),
                        highestSeq, 
                        false);
            if (parseTreeCnt > 0) {
                exprParseTree     = parseTreeArray[0];
            }
        }
        catch (VerifyException e) {
        }
        return exprParseTree;
    }


//  private Stmt[] grammaticalParseOneFormula(
    private ParseTree grammaticalParseOneFormula(
                                     Formula   formula,
                                     VarHyp[]  varHypArray,
                                     int       highestSeq,
                                     String    defaultStmtLabel,
                                     SourcePosition position) {
//      Stmt[]      exprRPN = null;
        ParseTree   exprParseTree = null;

        ParseTree[] parseTreeArray
                                  = new ParseTree[parseTreeMax];
        int         parseTreeCnt;
        try {
// /* --
//          System.out.println("$$$$$$$$$$$$$ "
//                             + defaultStmtLabel);
// */
        	boolean debug = false; //defaultStmtLabel.equals("df-mlim");

        	parseTreeCnt = grammaticalParser.parseExpr(
                        parseTreeArray,
                        formula.getTyp(),
                        formula.getParseNodeHolderExpr(varHypArray),
                        highestSeq, 
                        debug);
            if (parseTreeCnt < 0) {
                accumErrorMsgInList(
                	new SourcePosition(position, ((parseTreeCnt * -1) + 1)), // TODO Complete with more information from parseTreeCnt
                    GrammarConstants.ERRMSG_PARSE_FAILED_AT_POS_1
                    + defaultStmtLabel
                    + GrammarConstants.ERRMSG_PARSE_FAILED_AT_POS_2
                    + ((parseTreeCnt * -1) + 1));
            }
            else {
                switch (parseTreeCnt) {
                    case 1:
//                      exprRPN =
//                          parseTreeArray[0].convertToRPN();
                        exprParseTree =
                            parseTreeArray[0];
                        break;
                    case 0:
                        accumErrorMsgInList(
                        	position,
                            GrammarConstants.ERRMSG_PARSE_FAILED
                            + defaultStmtLabel);
                        break;
                    case 2:
                        /**
                         *  return 1st parse tree of n
                         */
//                      exprRPN =
//                          parseTreeArray[0].convertToRPN();
                        exprParseTree =
                            parseTreeArray[0];
                        accumInfoMsgInList(
                        	position,
                            GrammarConstants.ERRMSG_2_PARSE_TREES_1
                            + defaultStmtLabel
                            + GrammarConstants.ERRMSG_2_PARSE_TREES_2
                            + parseTreeArray[0].toString()
                            + GrammarConstants.ERRMSG_2_PARSE_TREES_3
                            + parseTreeArray[1].toString());
                        break;
                    default:
                        /**
                         *  return 1st parse tree of n
                         */
//                      exprRPN =
//                          parseTreeArray[0].convertToRPN();
                        exprParseTree =
                            parseTreeArray[0];
                        StringBuffer s = new StringBuffer(100);
                        s.append(
                            GrammarConstants.ERRMSG_N_PARSE_TREES_1);
                        s.append(defaultStmtLabel);
                        for (int i = 0; i < parseTreeCnt; i++) {
                          s.append(
                            GrammarConstants.ERRMSG_N_PARSE_TREES_2);
                          s.append(i);
                          s.append(
                            GrammarConstants.ERRMSG_N_PARSE_TREES_3);
                          s.append(parseTreeArray[i].toString());
                        }
                        accumInfoMsgInList(position, s.toString());
                        break;
                }
            }
        }
        catch (VerifyException e) {
            accumErrorMsgInList(position, e.getMessage()
                + GrammarConstants.ERRMSG_LABEL_CAPTION
                + defaultStmtLabel);
        }
//      return exprRPN;
        return exprParseTree;
    }


    private   void checkVerifySyntaxParams(MessageHandler messageHandlerParam,
                                           Map      symTblParam,
                                           Map      stmtTblParam) {

    //  store as global variables for convenience throughout
        messageHandler      = messageHandlerParam;
        symTbl        = symTblParam;
        stmtTbl       = stmtTblParam;
    }


//  /**
//   *  buildDefaultExprRPN --
//   *  The default RPN, which is also the "atomic" RPN used
//   *  for Syntax Axioms consists of the statement's variable
//   *  hypotheses -- in database .seq order -- followed by
//   *  the Stmt itself. Example: the RPN for wi is "ph ps wi".
//   *
//   *  NOTE: the default RPN can always be used to reconstruct
//   *        the original expression, even if the expression
//   *        has not been parsed into sub-expressions with
//   *        Syntax Axioms; just feed the RPN into an analogue
//   *        of the Proof Verifier with its stack and variable
//   *        substitution algorithms.
//   */
//  private   Stmt[] buildDefaultExprRPN(Stmt     stmt,
//                                       VarHyp[] varHypArray) {
//
//      Stmt[] exprRPN = new Stmt[varHypArray.length + 1];
//      for (int i = 0; i < varHypArray.length; i++) {
//          exprRPN[i] = varHypArray[i];
//      }
//      exprRPN[varHypArray.length] = stmt;
//      return exprRPN;
//  }

    /**
     *  buildDefaultExprParseTree --
     *  The default, which is also the "atomic" parse used
     *  for Syntax Axioms consists of the statement's variable
     *  hypotheses -- in database .seq order -- and the
     *  the Stmt itself. Example: the RPN for wi is "ph ps wi".
     *
     *  NOTE: the default RPN can always be used to reconstruct
     *        the original expression, even if the expression
     *        has not been parsed into sub-expressions with
     *        Syntax Axioms; just feed the RPN into an analogue
     *        of the Proof Verifier with its stack and variable
     *        substitution algorithms.
     */
    private ParseTree buildDefaultExprParseTree
                                (Stmt     stmt,
                                VarHyp[] varHypArray) {

        ParseNode root            = new ParseNode();
        root.setStmt(stmt);

        ParseNode[] children      =
                    new ParseNode[varHypArray.length];
        for (int i = 0; i < varHypArray.length; i++) {
            children[i]           =
                    new ParseNode(varHypArray[i]);
        }
        root.setChild(children);

        return new ParseTree(root);
    }


    /* friendly */ void accumErrorMsgInList(SourcePosition position, String errorMsg) {
        messageHandler.accumErrorMessage(position, errorMsg);
    }

    /* friendly */ void accumInfoMsgInList(SourcePosition position, String infoMsg) {
        messageHandler.accumInfoMessage(position, infoMsg);
    }

    /**
     * editGrammarConstructorTypParams --
     * Grungy validation of input Constructor Type Code
     * parameters. More validation needed later, but
     * it seems good practice to validate input at the
     * source.
     */
    private   void editGrammarConstructorTypParams() {
        if (provableLogicStmtTypCodes == null ||
            provableLogicStmtTypCodes.length != 1) {
            throw new IllegalArgumentException(
                GrammarConstants.ERRMSG_PROVABLE_TYP_PARAM_INVALID);
        }
        if (logicStmtTypCodes == null ||
            logicStmtTypCodes.length < 1) {
            throw new IllegalArgumentException(
                GrammarConstants.ERRMSG_LOGIC_TYP_PARAM_INVALID);
        }
        for (int i = 0; i < provableLogicStmtTypCodes.length; i++) {
            if (provableLogicStmtTypCodes[i].length() < 1 ||
                provableLogicStmtTypCodes[i].indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                    GrammarConstants.ERRMSG_PROVABLE_TYP_CD_BOGUS_1
                    + i
                    +
                    GrammarConstants.ERRMSG_PROVABLE_TYP_CD_BOGUS_2);
            }
            for (int j = i + 1;
                 j < provableLogicStmtTypCodes.length;
                 j++) {
                if (provableLogicStmtTypCodes[i].equals(
                    provableLogicStmtTypCodes[j])) {
                    throw new IllegalArgumentException(
                        GrammarConstants.ERRMSG_PROVABLE_TYP_DUPS_1
                        + i
                        +
                        GrammarConstants.ERRMSG_PROVABLE_TYP_DUPS_2
                        + j
                        +
                        GrammarConstants.ERRMSG_PROVABLE_TYP_DUPS_3);
                }
            }
            for (int j = 0; j < logicStmtTypCodes.length; j++) {
                if (provableLogicStmtTypCodes[i].equals(
                    logicStmtTypCodes[j])) {
                    throw new IllegalArgumentException(
                    GrammarConstants.ERRMSG_PROVABLE_DUP_OF_LOGICAL_1
                    + i
                    +
                    GrammarConstants.ERRMSG_PROVABLE_DUP_OF_LOGICAL_2
                    + j);
                }
            }
        }
        for (int i = 0; i < logicStmtTypCodes.length; i++) {
            if (logicStmtTypCodes[i].length() < 1 ||
                logicStmtTypCodes[i].indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                    GrammarConstants.ERRMSG_LOGIC_TYP_CD_BOGUS_1
                    + i
                    + GrammarConstants.ERRMSG_LOGIC_TYP_CD_BOGUS_2);
            }
            for (int j = i + 1; j < logicStmtTypCodes.length; j++) {
                if (logicStmtTypCodes[i].equals(
                    logicStmtTypCodes[j])) {
                    throw new IllegalArgumentException(
                        GrammarConstants.ERRMSG_LOGIC_TYP_DUPS_1
                        + i
                        + GrammarConstants.ERRMSG_LOGIC_TYP_DUPS_2
                        + j
                        + GrammarConstants.ERRMSG_LOGIC_TYP_DUPS_3);
                }
            }
        }
    }

    private   boolean initializeGrammarTables()
                                        throws VerifyException {

        grammarInitialized          = false;

        TreeSet allSyntaxAxiomSet   = new TreeSet(Stmt.SEQ);

        if (!setInitialGrammarTableValues(allSyntaxAxiomSet)) {
            return false;
        }

        derivedRuleQueueInit();
        boolean     errorsFound    = false;
        Iterator    iterator       = allSyntaxAxiomSet.iterator();
        Axiom       axiom;
        while (iterator.hasNext() &&
               !messageHandler.maxErrorMessagesReached()) {
            axiom                  = (Axiom)iterator.next();
            if (!GrammarRule.add(axiom,
                                 this)) {
                errorsFound = true;
            }
        }

        setDerivedRuleQueue(null);

        if (errorsFound) {
            return false;
        }

// //*
//      grammaticalParser = new BottomUpParser(this,
// //*
        /**
         *  Note: the BottomUpParser is perfectly acceptable
         *        for parsing Grammar Rules, and it may even
         *        be somewhat better for ambiguous grammars
         *        as it seems to find shorter parsers first.
         *        It is also simpler than EarleyParse, by
         *        an order of magnitude -- if certainty is
         *        desired it can be used in parallel with or
         *        even instead of EarleyParse EXCEPT for
         *        databases like set.mm (it works fine
         *        parsing set.mm's GrammarRules but takes
         *        massive amounts of time on statements like
         *        "supeu".) In short, it is a somewhat good
         *        idea to use BottomUpParser for the
         *        GrammarAmbiguity edits BUT in the interests
         *        of reducing the amount of code used in a
         *        run, the EarleyParser is being used now
         *        everywhere.
         */
        grammaticalParser = new EarleyParser(this,
                                             maxFormulaCnt);

        GrammarAmbiguity g = new GrammarAmbiguity(
                                    this,
                                    doCompleteGrammarAmbiguityEdits);
        if (g.basicAmbiguityEdits()) {
            if (doCompleteGrammarAmbiguityEdits) {
                if (g.fullAmbiguityEdits()) {
                    grammarInitialized = true;
                }
            }
            else {
                grammarInitialized = true;
            }
        }
        g = null;

// //*  //See comment above concerning BottomUpParser...
//      grammaticalParser = new EarleyParser(this,
//                                           maxFormulaCnt);
// //*

        return grammarInitialized;
    }


    private   boolean setInitialGrammarTableValues(
                                TreeSet allSyntaxAxiomSet) {

        boolean errorsFound       = false;

        lastGrammarRuleNbr        = 0;

        provableLogicStmtTypArray =
                        new Cnst[provableLogicStmtTypCodes.length];
        logicStmtTypArray         =
                        new Cnst[logicStmtTypCodes.length];

        varHypTypSet              = new TreeSet(MObj.SEQ);
        syntaxAxiomTypSet         = new TreeSet(MObj.SEQ);
        nullsPermittedTypSet      = new TreeSet(MObj.SEQ);
        nullsPermittedGRList      = new ArrayList(5);
        typeConversionGRList      = new ArrayList(5);
        notationGRSet             = new TreeSet(
                                            GrammarRule.MAX_SEQ_NBR);
//      baseGRSet                = new TreeSet(
//                                          GrammarRule.MAX_SEQ_NBR);
        notationGRGimmeMatchCnt   = 0;

        Sym        sym;
        Cnst       cnst;
        Stmt       stmt;
        Axiom      axiom;
        VarHyp[]   varHypArray;

        Collection collection   = symTbl.values();
        Iterator   collIterator = collection.iterator();
        while (collIterator.hasNext()) {
            sym = (Sym)collIterator.next();
            if (sym.isCnst()) {
                cnst = (Cnst)sym;
                cnst.setIsVarTyp(false);
                cnst.setIsProvableLogicStmtTyp(false);
                cnst.setIsLogicStmtTyp(false);
                cnst.setIsSyntaxAxiomTyp(false);
                cnst.setIsGrammaticalTyp(false);
                cnst.setNbrOccInCnstSyntaxAxioms(0);
                cnst.setNbrOccInSyntaxAxioms(0);
                cnst.setGRRoot(null);
                cnst.setConvFromTypGRArray(null);
                cnst.setNullsPermittedGR(null);
                cnst.setLen1CnstNotationRule(null);
                cnst.setEarleyRules(null);
                cnst.setEarleyFIRST(null);
            }
        }

        for (int i = 0; i < provableLogicStmtTypCodes.length; i++) {
            sym = (Sym)symTbl.get(provableLogicStmtTypCodes[i]);
            if (sym == null ||
                !sym.isCnst()) {
                accumErrorMsgInList(
                	sym.getPosition(), 
                    GrammarConstants.ERRMSG_PROVABLE_TYP_NOT_A_CNST_1
                    + i
                    +
                    GrammarConstants.ERRMSG_PROVABLE_TYP_NOT_A_CNST_2
                    + provableLogicStmtTypCodes[i]
                    +
                    GrammarConstants.ERRMSG_PROVABLE_TYP_NOT_A_CNST_3
                    );
                errorsFound = true;
            }
            else {
                cnst = (Cnst)sym;
                provableLogicStmtTypArray[i] = cnst;
                cnst.setIsProvableLogicStmtTyp(true);
                cnst.setIsGrammaticalTyp(true);
            }
        }

        for (int i = 0; i < logicStmtTypCodes.length; i++) {
            sym = (Sym)symTbl.get(logicStmtTypCodes[i]);
            if (sym == null ||
                !sym.isCnst()) {
                accumErrorMsgInList(
                	sym.getPosition(), 
                    GrammarConstants.ERRMSG_LOGIC_TYP_NOT_A_CNST_1
                    + i
                    + GrammarConstants.ERRMSG_LOGIC_TYP_NOT_A_CNST_2
                    + logicStmtTypCodes[i]
                    + GrammarConstants.ERRMSG_LOGIC_TYP_NOT_A_CNST_3
                    );
                errorsFound = true;
            }
            else {
                cnst = (Cnst)sym;
                logicStmtTypArray[i] = cnst;
                cnst.setIsLogicStmtTyp(true);
                cnst.setIsGrammaticalTyp(true);
            }
        }

        if (errorsFound) {
            return false;
        }

        collection   = stmtTbl.values();
        collIterator = collection.iterator();

        int formulaLength;
        while (collIterator.hasNext()) {
            stmt = (Stmt)collIterator.next();

            // compute maxFormulaCnt for GrammaticalParser's use.
            formulaLength     = stmt.getFormula().getCnt();
            if (formulaLength > maxFormulaCnt) {
                maxFormulaCnt = formulaLength;
            }

            cnst = stmt.getTyp();
            if (stmt instanceof Theorem) {
//              stmt.setExprRPN(null);
                stmt.setExprParseTree(null);
                continue;
            }
            if (stmt instanceof LogHyp) {
//              stmt.setExprRPN(null);
                stmt.setExprParseTree(null);
                continue;
            }
            if (stmt instanceof VarHyp) {
                varHypTypSet.add(cnst);
                cnst.setIsVarTyp(true);
                cnst.setIsGrammaticalTyp(true);

//              Stmt[] varHypRPN = new Stmt[1]; //default RPN
//              varHypRPN[0]     = stmt;        //looks circular, eh?
//              stmt.setExprRPN(varHypRPN);
                stmt.setExprParseTree(
                    new ParseTree(
                        new ParseNode((VarHyp)stmt)));

                if (cnst.getIsProvableLogicStmtTyp()) {
                    accumErrorMsgInList(
                    	cnst.getPosition(),
                        GrammarConstants.ERRMSG_VARHYP_TYP_PROVABLE_1
                        + stmt.getLabel()
                        +
                        GrammarConstants.ERRMSG_VARHYP_TYP_PROVABLE_2
                        );
                    errorsFound = true;
                }
                continue;
            }
            if (stmt instanceof Axiom) {
                axiom = (Axiom)stmt;
                axiom.setSyntaxAxiomHasUniqueCnst(false);
                if (!cnst.getIsProvableLogicStmtTyp()) {
                    allSyntaxAxiomSet.add(axiom);
                    syntaxAxiomTypSet.add(cnst);
                    cnst.setIsSyntaxAxiomTyp(true);
                    cnst.setIsGrammaticalTyp(true);
                    if (!initSyntaxAxiomPart1(axiom)) {
                        errorsFound = true;
                    }
                }
                else {
                    axiom.setIsSyntaxAxiom(false);
                    axiom.setSyntaxAxiomVarHypReseq(null);
//                  axiom.setExprRPN(null);
                    axiom.setExprParseTree(null);
                }
                continue;
            }
        }

        collIterator = allSyntaxAxiomSet.iterator();
        while (collIterator.hasNext()) {
            axiom = (Axiom)collIterator.next();
            if (!initSyntaxAxiomPart2(axiom)) {
                errorsFound = true;
            }
        }
        if (errorsFound) {
            return false;
        }

        return !errorsFound;
    }


    private   boolean initSyntaxAxiomPart1(Axiom axiom) {

        boolean errorsFound = false;

        axiom.setIsSyntaxAxiom(true);

        VarHyp[] varHypArray = axiom.getMandVarHypArray();

//      Stmt[]   syntaxRPN   = new Stmt[varHypArray.length + 1];
//      for (int i = 0; i < varHypArray.length; i++) {
//          syntaxRPN[i] = varHypArray[i];
//      }
//      syntaxRPN[varHypArray.length] = axiom;
//      axiom.setExprRPN(syntaxRPN);

        axiom.setExprParseTree(
            buildDefaultExprParseTree(axiom,
                                      varHypArray));

        MandFrame mandFrame = axiom.getMandFrame();
        if (mandFrame.djVarsArray.length != 0) {
            accumErrorMsgInList(
            	axiom.getPosition(),
                GrammarConstants.ERRMSG_DJ_VARS_ON_SYNTAX_1
                + axiom.getLabel()
                + GrammarConstants.ERRMSG_DJ_VARS_ON_SYNTAX_2);
            errorsFound = true;
        }
        if (mandFrame.hypArray.length != varHypArray.length) {
            accumErrorMsgInList(
               	axiom.getPosition(),
                GrammarConstants.ERRMSG_LOGHYP_FOR_SYNTAX_1
                + axiom.getLabel()
                + GrammarConstants.ERRMSG_LOGHYP_FOR_SYNTAX_2);
            errorsFound = true;
        }

        boolean isResequenced = false;
        int[] reseqVarHyp     = new int[varHypArray.length];
        Sym[] sym             = axiom.getFormula().getSym();
        int   dest            = 0;
        Cnst  cnst;
        Var   var;

        if (sym.length == 1) {
            /**
             * Subsequent Nulls Permitted types may be identified
             * as the grammar rules are generated -- these would
             * be "indirect" (or "derived") Nulls Permitted types.
             */
            nullsPermittedTypSet.add((Cnst)sym[0]);
        }
        else {
            //start at 1 to bypass Formula.Typ
            loopExpr: for (int src = 1; src < sym.length; src++) {
                if (sym[src].isCnst()) {
                    cnst = (Cnst)sym[src];
                    cnst.incNbrOccInSyntaxAxioms();
                    if (varHypArray.length == 0) {
                        cnst.incNbrOccInCnstSyntaxAxioms();
                    }
                    continue loopExpr;
                }
                var = (Var)sym[src];
                loopVarHyp: for (int i = 0;
                                 i < varHypArray.length;
                                 i++) {
                    if (varHypArray[i].getVar() == var) {
                        if (dest >= reseqVarHyp.length) {
                            accumErrorMsgInList(
                                	axiom.getPosition(),
                     GrammarConstants.ERRMSG_SYNTAX_VARHYP_MISMATCH_1
                                + axiom.getLabel()
                                +
                     GrammarConstants.ERRMSG_SYNTAX_VARHYP_MISMATCH_2
                                + axiom.getFormula().toString());
                            errorsFound = true;
                        }
                        else {
                            if (reseqVarHyp[dest] != 0) {
                                accumErrorMsgInList(
                                    	axiom.getPosition(),
                        GrammarConstants.ERRMSG_SYNTAX_VAR_GT_1_OCC_1
                                    + axiom.getLabel()
                                    +
                        GrammarConstants.ERRMSG_SYNTAX_VAR_GT_1_OCC_2
                                    + sym[src].toString()
                                    +
                        GrammarConstants.ERRMSG_SYNTAX_VAR_GT_1_OCC_3
                                    + axiom.getFormula().toString());
                                errorsFound = true;
                            }
                            else {
                                if (dest != i) {
                                    isResequenced = true;
                                }
                                reseqVarHyp[dest++] = i;
                            }
                        }
                        break loopVarHyp;
                    }
                }
            }
            if (dest != reseqVarHyp.length) {
                accumErrorMsgInList(
                    	axiom.getPosition(),
                    GrammarConstants.ERRMSG_SYNTAX_VARHYP_MISMATCH_1
                    + axiom.getLabel()
                    +
                    GrammarConstants.ERRMSG_SYNTAX_VARHYP_MISMATCH_2
                    + axiom.getFormula().toString());
                errorsFound = true;
            }
        }
        if (isResequenced != true) {
            reseqVarHyp = null;
        }
        axiom.setSyntaxAxiomVarHypReseq(reseqVarHyp);

        return !errorsFound;
    }

    /**
     * initSyntaxAxiomPart2 -- this final bit of work
     * requires that a pass through all of the Syntax
     * Axioms AND other statements has already been
     * completed.
     *
     */
    private   boolean initSyntaxAxiomPart2(Axiom axiom) {

        boolean errorsFound = false;

        Sym[] sym            = axiom.getFormula().getSym();
        Cnst  cnst;
        int   varHypArrayLen = (axiom.getMandVarHypArray()).length;

        loopExpr: for (int i = 1; i < sym.length; i++) {
            if (!sym[i].isCnst()) {
                continue loopExpr;
            }
            cnst = (Cnst)sym[i];
            if (cnst.getNbrOccInSyntaxAxioms() == 1) {
                axiom.setSyntaxAxiomHasUniqueCnst(true);
            }
            /**
             *  Note: consider expanding this to check
             *        for non-constant axioms with all
             *        variables having nulls permitted;
             *        this would prevent rules from being
             *        generated that violate this edit.
             *        BUT there are legitimate uses of
             *        patterns like this, so consider
             *        other ways of solving the problem.
             */
          //if (varHypArrayLen == 0 &&
          //    sym.length      > 2 &&
          //    cnst.getNbrOccInSyntaxAxioms() > 1) {
          //    accumErrorMsgInList(
          //        "Syntax Axiom, label = "
          //        + axiom.getLabel()
          //        + ", is an all-Constant Syntax Axiom"
          //        + " with more than one Constant, which"
          //        + " contains a Constant, "
          //        + cnst.toString()
          //        + ", that appears in at least one other"
          //        + " Syntax Axiom. This grammatical"
          //        + " parser does not support this class"
          //        + " of grammars, which, although legal,"
          //        + " introduce complexities beyond the"
          //        + " scope of this programming effort.");
          //    errorsFound = true;
          //}
            if (cnst.getIsGrammaticalTyp()) {
                accumErrorMsgInList(
                    	axiom.getPosition(),
                    GrammarConstants.ERRMSG_SYNTAX_USES_TYP_AS_CNST_1
                    + axiom.getLabel()
                    +
                    GrammarConstants.ERRMSG_SYNTAX_USES_TYP_AS_CNST_2
                    + cnst.toString()
                    +
                    GrammarConstants.ERRMSG_SYNTAX_USES_TYP_AS_CNST_3
                    );
                errorsFound = true;
            }
        }
        return !errorsFound;
    }

    /**
     * Resets this grammar object to its initial state
     */
    public void clear() {
    	grammarInitialized = false;
    	
    }
}

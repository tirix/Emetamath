//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  LogicalSystem.java  0.06 08/01/2008
 *
 *      --> 10-Dec-2005: add "prematureEOF" param to finalizeEOF()
 *          so that a user-requested termination of the load at
 *          a label or statement number can exit gracefully (need
 *          to exit nested scopes).
 *      --> made following methods public for use with ProofAsst
 *              getScopeDefList()
 *              getCurrScopeDef()
 *              getScopeLvl()
 *  Version 0.04:
 *      --> added addTheorem() variant with compressed proof parms
 *
 *  Version 0.05:
 *
 *  Oct-12-2006: - added Sym/StmtTbls to constructors and modified to
 *                 conform to Metamath.pdf spec change of 6-24-2006
 *                 prohibiting Stmt label and Sym id namespace
 *                 collisions.
 *
 *  Version 0.06 08/01/2008:
 *      --> added provableLogicStmtTypeParm and logicStmtTypeParm
 *          which are the String values of the Cnst's for
 *          "|-" and "wff", respectively. Added "get" methods
 *          also. These are still needed in Grammar, but are
 *          needed prior to that during the load phase.
 *      --> added BookManager() and related items.
 *      --> added seqAssigner() and related items.
 *      --> removed automatic proof and syntax validation
 *          at EOF. this cannot work any longer because
 *          most mmj.util.*Boss routines re-initialize their
 *          state variables when they see a "LoadFile".
 *      --> Add dup checking for Sym and Stmt adds for the
 *          benefit of TheoremLoader, as well as general
 *          paranoia: see ERRMSG_DUP_SYM_MAP_PUT_ATTEMPT (A-LA-0041)
 *          and ERRMSG_DUP_STMT_MAP_PUT_ATTEMPT (A-LA-0042).
 */

package mmj.lang;

import java.util.*;
import mmj.verify.GrammarConstants;
import mmj.mmio.SourcePosition;
import mmj.tl.*;

/**
 * The <code>LogicalSystem</code>, along with the rest of the
 * <code>mmj.lang</code> package, implements the abstract portion
 * of the Metamath language -- the "object" language of Metamath,
 * as opposed to the "metalanguage" of Metamath source files.
 *
 * For example, note that <code>LogicalSystem.java</code> does
 * not care which characters are used in Constant, Variable and Label
 * strings. The characters are represented internally in Unicode,
 * not the 7-bit ASCII employed in Metamath (.mm) source files.
 * Nevertheless, the logical rules concerning Constant, Variable
 * and Label <i>namespaces</i> are enforced rigorously :)
 *
 * As much as possible, the concerns of source files are
 * delegated to the <code>mmj.mmio</code> package and its
 * highest level class, <code>Systemizer.java</code>, which uses
 * <code>LogicalSystem.java</code> to create a Metamath
 * logical system in memory. Once <code>Systemizer.java</code>
 * has input a set of Metamath statements into a
 * <code>LogicalSystem.java</code> object, the Logical System
 * can be processed independently of all <code>mmj.mmio</code>
 * code. In fact, a GUI system need not use <code>mmj.mmio</code>
 * or <code>Systemizer.java</code> at all (however it should be
 * noted that none of this code is "thread aware" as of now...)
 *
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public class LogicalSystem implements SystemLoader {

    private   String           provableLogicStmtTypeParm;
    private   String           logicStmtTypeParm;

    private   BookManager      bookManager;

    private   SeqAssigner      seqAssigner;
    private   LinkedList       theoremLoaderCommitListeners;

    private   ArrayList        scopeDefList;
    private   ScopeDef         currScopeDef;
    private   int              scopeLvl;
    private   int              mObjCount
                                  = 0;

    private   ProofVerifier    proofVerifier;
    private   SyntaxVerifier   syntaxVerifier;
    private   ProofCompression proofCompression;

    // Sym table (was sorted, asc order by Sym.id, but HashMap is
    // faster...)
    private   Map              symTbl;

    // stmtTbl (was asc order by Stmt.label, but HashMap is
    // faster...)
    private   Map              stmtTbl;

    /**
     * Construct with full set of parameters.
     *
     * @param provableLogicStmtTypeParm Type Code String of
     *        provable statements. Defaults to "|-".
     *
     * @param logicStmtTypeParm Type Code String value of logic
     *        expressions. Defaults to "wff".
     *
     * @param bookManager optional, used for tracking MObjs by
     *        Chapter and Section number.
     *
     * @param symTblInitialSize  should equal number of constant
     *        and variables, times 1.33 plus a fudge factor. If set
     *        too small the system will automatically increase,
     *        but at the expense of reallocating and copying.
     *        BUT, if the input value is less than 10, the program
     *        assumes a programming error exists and throws an
     *        IllegalArgumentException. A suggested default value
     *        is provided in LangConstants.java.
     *
     * @param stmtTblInitialSize  should equal number of variable
     *        and logical hypotheses plus the number of axioms and
     *        theorems, times 1.33 plus a fudge factor. If set
     *        too small the system will automatically increase,
     *        but at the expense of reallocating and copying.
     *        BUT, if the input value is less than 100, the program
     *        assumes a programming error exists and throws an
     *        IllegalArgumentException. A suggested default value
     *        is provided in LangConstants.java.
     *
     * @param proofVerifier  ProofVerifier interface object.
     *        Leave null if automatic proof verification not
     *        desired.
     *
     * @param syntaxVerifier SyntaxVerifier interface object.
     *        Leave null if automatic loading of ExprRPN's is not
     *        desired.
     *
     */
    public LogicalSystem(String         provableLogicStmtTypeParm,
                         String         logicStmtTypeParm,
                         BookManager    bookManager,
                         SeqAssigner    seqAssigner,
                         int            symTblInitialSize,
                         int            stmtTblInitialSize,
                         ProofVerifier  proofVerifier,
                         SyntaxVerifier syntaxVerifier) {

        mObjCount = 0;

        this.provableLogicStmtTypeParm
                                  = provableLogicStmtTypeParm;
        this.logicStmtTypeParm    = logicStmtTypeParm;

        this.bookManager          = bookManager;

        this.seqAssigner          = seqAssigner;
        theoremLoaderCommitListeners
                                  = new LinkedList();

        if (symTblInitialSize <
            LangConstants.SYM_TBL_INITIAL_SIZE_MINIMUM) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_SYM_TBL_TOO_SMALL
                + LangConstants.SYM_TBL_INITIAL_SIZE_MINIMUM);
        }

        if (stmtTblInitialSize <
            LangConstants.STMT_TBL_INITIAL_SIZE_MINIMUM) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_STMT_TBL_TOO_SMALL
                + LangConstants.STMT_TBL_INITIAL_SIZE_MINIMUM);
        }

        symTbl                     =  new HashMap(symTblInitialSize);
        stmtTbl                    =  new HashMap(stmtTblInitialSize);


        this.syntaxVerifier        =  syntaxVerifier;
        this.proofVerifier         =  proofVerifier;

        //init stack of scope levels
        scopeDefList               = new ArrayList();
        beginScope();   //initialize global scope level

    }


    /**
     *  Construct using default initialization paramters.
     */
    public LogicalSystem() {

        this(GrammarConstants.
                DEFAULT_PROVABLE_LOGIC_STMT_TYP_CODES[0],
             GrammarConstants.
                DEFAULT_LOGIC_STMT_TYP_CODES[0],
             new BookManager(
                 LangConstants.
                    BOOK_MANAGER_ENABLED_DEFAULT,
                 GrammarConstants.
                    DEFAULT_PROVABLE_LOGIC_STMT_TYP_CODES[0]),
             new SeqAssigner(),
             LangConstants.SYM_TBL_INITIAL_SIZE_DEFAULT,
             LangConstants.STMT_TBL_INITIAL_SIZE_DEFAULT,
             LangConstants.PROOF_VERIFIER_DEFAULT,
             LangConstants.SYNTAX_VERIFIER_DEFAULT);
    }


    /**
     *  Add Cnst to Logical System.
     *  <p>
     *  1) validates constant:
     *  <p>
     *  <ul>
     *      <li>declaration allowed only at global scope level
     *      <li>not a dup of any other constant or variable
     *  </ul>
     *  <p>
     *  2)adds to symTbl table
     *  <p>
     *  3)return the constant object that now exists in the
     *    symbol table (this is done for consistency with
     *    the other addXXX methods -- we want everyone to
     *    use the only objects that exist in LogicalSystem...
     *    otherwise, CHAOS would occur!
     *
     * @param id  Constant's symbol string to be added to the
     *        Logical System.
     *
     * @return Cnst added to LogicalSystem.
     *
     * @throws   LangException if duplicate symbol, etc.
     */
    public Cnst addCnst(String id)
                        throws LangException {
        if (scopeLvl != 0) {
            throw new LangException(
                LangConstants.ERRMSG_MUST_DEF_CNST_AT_GLOBAL_LVL);
        }

        Cnst c = new Cnst(seqAssigner.nextSeq(),
                          symTbl,
                          stmtTbl,
                          id);
        Sym existingSym           =
            (Sym)symTbl.put(id,
                            c);

        dupCheckSymAdd(existingSym);

        bookManager.assignChapterSectionNbrs(c);

        return c;
    }

    /**
     *  Add Var to Logical System.
     *  <p>
     *  1) validates variable:
     *  <ul>
     *      <li>must not be redeclared unless currently inactive
     *      <li>not a dup of any other constant or variable
     *  </ul>
     *  <p>
     *  2) add to symTbl table; if already there but inactive,
     *     mark it active.
     *  <p>
     *  3) get current ScopeDef array list item, (if any) and add
     *     the new var to the scope array list of Var's.
     *  <p>
     *  4) return the variable object that now exists in the
     *     symbol table (a key step so that everyone is using
     *     the same object!!!)
     *
     * @param id  Var's symbol string to be added to the
     *        Logical System.
     *
     * @return Var added to LogicalSystem.
     *
     * @throws  LangException if duplicate symbol, etc.
     */
    public Var addVar(String id)
                        throws LangException {

        Var v = Var.declareVar(seqAssigner.nextSeq(),
                               symTbl,
                               stmtTbl,
                               id);
        currScopeDef.scopeVar.add(v);

        bookManager.assignChapterSectionNbrs(v);

        return v;
    }


    /**
     *  Add VarHyp to Logical System.
     *  <p>
     *  1)Validates VarHyp label:
     *  <p>
     *  <ul>
     *    --must not already exist (label must be unique.)
     *  </ul>
     *  <p>
     *  2)Validates VarHyp typ and var:
     *  <p>
     *  <ul>
     *    --must exist in symTbl as active Cnst and Var,
     *      respectively.
     *    --marks the var as "active" and the typ as a variable
     *      type.
     *  </ul>
     *  <p>
     *  3)Manufacture an active VarHyp, complete with sequence
     *    number
     *  <p>
     *  4)Add the new variable hypothesis to the statement table.
     *  <p>
     *  5) get current ScopeDef array list item, (if any) and add
     *     the new VarHyp to the scope array list of VarHyp's
     *  <p>
     *  6) return the VarHyp object that now exists in the
     *    statement table.
     *
     * @param labelS   String label of variable hypothesis
     * @param typS     String Metamath constant character (type code)
     * @param varS     String Metamath variable character
     *
     * @return VarHyp added to LogicalSystem.
     *
     * @throws       LangException if duplicate symbol, etc.
     *               (see <code>mmj.lang.LangConstants.java</code>)
     */
    public VarHyp addVarHyp(String labelS,
                            String typS,
                            String varS)
                        throws LangException {

        VarHyp vH = new VarHyp(seqAssigner.nextSeq(),
                               symTbl,
                               stmtTbl,
                               varS,
                               labelS,
                               typS);

        Stmt existingStmt         =
            (Stmt)stmtTbl.put(labelS,
                              vH);

        dupCheckStmtAdd(existingStmt);

        (vH.getVar()).setActiveVarHyp(vH);

        (vH.getTyp()).setIsVarTyp(true);

        currScopeDef.scopeVarHyp.add(vH);

        bookManager.assignChapterSectionNbrs(vH);

        return vH;
    }


    /**
     *  Add DjVars (Disjoint Variables Restriction) to Logical
     *  System.
     *  <p>
     *  1) validates disjoint variables:
     *  <p>
     *  <ul>
     *      <li>must be active in scope
     *      <li>not duplicates of each other
     *  </ul>
     *  <p>
     *  2) construct DjVars element (swapping the items if
     *     necessary to ensure varLo < varHi.--see constructor).
     *  <p>
     *  3) get current ScopeDef array list item
     *  <p>
     *  4) add the DjVars elment to the scope list of DjVars,
     *     but only if the pair of variables is not already
     *     there (we cannot use "==" in this case because
     *     DjVars are not maintained as "unique" in the same
     *     way as Sym's and Stmt's.)
     *
     * @param djVar1S    disjoint variable symbol string 1
     * @param djVar2S    disjoint variable symbol string 2
     *
     * @return DjVars (pair) added to LogicalSystem.within
     *         the current scope, *or* the existing DjVars
     *         object in the current scope.
     *
     * @throws   LangException if duplicate vars, etc.
     *           (see <code>mmj.lang.LangConstants.java</code>)
     */
    public DjVars addDjVars(String djVar1S,
                            String djVar2S)
                        throws LangException {

        DjVars djVars = new DjVars(symTbl,
                                   djVar1S,
                                   djVar2S);

        int i = currScopeDef.scopeDjVars.indexOf(djVars);
        if (i == -1) {
            currScopeDef.scopeDjVars.add(djVars);
        }
        else {
            djVars = (DjVars)currScopeDef.scopeDjVars.get(i);
        }

        return djVars;
    }


    /**
     *  Add LogHyp (Logical Hypothesis) to Logical System.
     *  <p>
     *  Basically a clone of <code>addAxiom</code> below,
     *      except that:
     *  <p>
     *  <ul>
     *      <li>there is no Frame for a logical hypothesis
     *      <li><code>LogHyp is a <code>Hyp</code>, not an
     *          <code>Assrt</code> and has
     *          attribute <code>"VarHyp[] varHypArray</code>.
     *  </ul>
     *  <p>
     *  Required processing:
     *  <p>
     *  1) validates input label, type and expression symbols,
     *     and in the process obtaining references to their
     *     existing definitions in LogicalSystem:
     *  <p>
     *  <ul>
     *      <li> must be active in scope
     *      <li> not be a duplicate
     *      <li> active VarHyp must exist for each Var.
     *  </ul>
     *  <p>
     *  2) construct the LogHyp object
     *  <p>
     *  3) add it to the stmtTbl and to the existing scope.
     *  <p>
     *  4) return it to the caller
     *
     * @param labelS    logical hypothesis label string
     * @param typS      logical hypothesis type code (symbol) string
     * @param symList   list containing expression symbol strings
     *                  (zero or more symbols).
     *
     * @return LogHyp newly constructed LogHyp added to
     *                LogicalSystem.
     *
     * @throws   LangException if duplicate label, undefined vars,
     *           etc.
     */
    public LogHyp addLogHyp(String    labelS,
                            String    typS,
                            ArrayList symList)
                                throws LangException {

        LogHyp logHyp = new LogHyp(seqAssigner.nextSeq(),
                                   symTbl,
                                   stmtTbl,
                                   symList,
                                   labelS,
                                   typS);

        Stmt existingStmt         =
            (Stmt)stmtTbl.
                put(labelS,
                    logHyp);

        dupCheckStmtAdd(existingStmt);

        currScopeDef.scopeLogHyp.add(logHyp);

        bookManager.assignChapterSectionNbrs(logHyp);

        return logHyp;


    }

    /**
     *  Add LogHyp (Logical Hypothesis) to Logical System.
     *  for TheoremLoader.
     *  <p>
     *  The main difference from the regular addLogHyp is
     *  that the BookManager is not called.
     *
     * @param seq       preassigned MObj seq number
     * @param labelS    logical hypothesis label string
     * @param typS      logical hypothesis type code (symbol) string
     * @param symList   list containing expression symbol strings
     *                  (zero or more symbols).
     *
     * @return LogHyp newly constructed LogHyp added to
     *                LogicalSystem.
     *
     * @throws   LangException if duplicate label, undefined vars,
     *           etc.
     */
    public LogHyp addLogHypForTheoremLoader(
                            int       seq,
                            String    labelS,
                            String    typS,
                            ArrayList symList)
                                throws LangException {

        LogHyp logHyp = new LogHyp(seq,
                                   symTbl,
                                   stmtTbl,
                                   symList,
                                   labelS,
                                   typS);

        Stmt existingStmt         =
            (Stmt)stmtTbl.
                put(labelS,
                    logHyp);

        dupCheckStmtAdd(existingStmt);

        currScopeDef.scopeLogHyp.add(logHyp);

        return logHyp;
    }


    /**
     *  Add Axiom to Logical System.
     *  <p>
     *  1) validates input label, type and expression symbols,
     *     and in the process obtaining references to their
     *     existing definitions in LogicalSystem:
     *  <p>
     *  <ul>
     *   <li>not a duplicate
     *   <li>active VarHyp must exist for each Var.
     *  </ul>
     *  <p>
     *  2) Build the MetaMath "Frame" for the Axiom (see MandFrame)
     *  <p>
     *  3) construct the Axiom object
     *  <p>
     *  4) add it to the stmtTbl
     *  <p>
     *  5) return it to the caller
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol
     *                 strings (zero or more symbols).
     *
     * @return Axiom newly constructed Axiom added to LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    public Axiom addAxiom(String    labelS,
                          String    typS,
                          ArrayList symList)
                                        throws LangException {

        Axiom axiom = new Axiom(seqAssigner.nextSeq(),
                                scopeDefList,
                                symTbl,
                                stmtTbl,
                                labelS,
                                typS,
                                symList);

        Stmt existingStmt         =
            (Stmt)stmtTbl.put(labelS,
                              axiom);

        dupCheckStmtAdd(existingStmt);

        bookManager.assignChapterSectionNbrs(axiom);

        return axiom;
    }


    /**
     *  Add Theorem to Logical System.
     *  <p>
     *  addTheorem is basically a clone of addAxiom except that
     *  it builds an "extended" Frame containing
     *  "optional" hypotheses and disjoint variable
     *  restriction information. And it validates
     *  the input proof list then stores it with
     *  the new Theorem object.
     *  <p>
     *  1) validates input label, type and expression symbols,
     *     and in the process obtaining references to their
     *     existing definitions in LogicalSystem:
     *  <ul>
     *   <li>must be active in scope
     *   <li>not a duplicate
     *   <li>active VarHyp must exist for each Var.
     *  </ul>
     *  <p>
     *  2) Build the MetaMath Extended Frame for the Theorem
     *     (consists here in mmj of MandFrame + OptFrame).
     *  <p>
     *  3) Validates proof steps
     *  <p>
     *  4) Construct Theorem object
     *  <p>
     *  5) add it to the stmtTbl
     *  <p>
     *  6) return it to the caller
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol strings
     *                   (zero or more symbols).
     * @param proofList  list containing proof step symbol strings
     *                   (1 or more symbols -- which may be "?" if a
     *                   step is unknown).
     *
     * @return Theorem  newly constructed Theorem added to
     *                  LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    public Theorem addTheorem(String    labelS,
                              String    typS,
                              ArrayList symList,
                              ArrayList proofList)
                                            throws LangException {

        Theorem theorem = new Theorem(seqAssigner.nextSeq(),
                                      scopeDefList,
                                      symTbl,
                                      stmtTbl,
                                      labelS,
                                      typS,
                                      symList,
                                      proofList);

        Stmt existingStmt         =
            (Stmt)stmtTbl.
                put(labelS,
                    theorem);

        dupCheckStmtAdd(existingStmt);

        bookManager.assignChapterSectionNbrs(theorem);

        return theorem;
    }

    /**
     *  Add Theorem to Logical System for TheoremLoader.
     *  <p>
     *  Main difference of regular addTheorem is that it
     *  does not call BookManager.
     *  <p>
     * @param seq      preassigned MObj seq number
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol strings
     *                   (zero or more symbols).
     * @param proofList  list containing proof step symbol strings
     *                   (1 or more symbols -- which may be "?" if a
     *                   step is unknown).
     *
     * @return Theorem  newly constructed Theorem added to
     *                  LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    public Theorem addTheoremForTheoremLoader(
                              int       seq,
                              String    labelS,
                              String    typS,
                              ArrayList symList,
                              ArrayList proofList)
                                            throws LangException {
        Theorem theorem = new Theorem(seq,
                                      scopeDefList,
                                      symTbl,
                                      stmtTbl,
                                      labelS,
                                      typS,
                                      symList,
                                      proofList);

        Stmt existingStmt         =
            (Stmt)stmtTbl.
                put(labelS,
                    theorem);

        dupCheckStmtAdd(existingStmt);

        return theorem;
    }


    /**
     *  Add Theorem to Logical System.
     *  <p>
     *  addTheorem is basically a clone of addAxiom except that
     *  it builds an "extended" Frame containing
     *  "optional" hypotheses and disjoint variable
     *  restriction information. And it validates
     *  the input proof list then stores it with
     *  the new Theorem object.
     *  <p>
     *  This variant is invoked when the input contains
     *  a compressed proof.
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol strings
     *                 (zero or more symbols).
     * @param proofList  list containing the contents of the
     *                 parenthesized portion of a compressed proof
     *                 (does not include the parentheses)
     * @param proofBlockList  list containing one or more blocks
     *                 of compressed proof symbols.
     *
     * @return Theorem  newly constructed Theorem added to
     *                  LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    public Theorem addTheorem(String    labelS,
                              String    typS,
                              ArrayList symList,
                              ArrayList proofList,
                              ArrayList proofBlockList)
                                            throws LangException {

        Theorem theorem = new Theorem(seqAssigner.nextSeq(),
                                      scopeDefList,
                                      symTbl,
                                      stmtTbl,
                                      labelS,
                                      typS,
                                      symList,
                                      proofList,
                                      proofBlockList,
                                      getProofCompression());

        Stmt existingStmt         =
            (Stmt)stmtTbl.put(labelS,
                              theorem);

        dupCheckStmtAdd(existingStmt);

        bookManager.assignChapterSectionNbrs(theorem);

        return theorem;
    }


    /**
     *  Begin a new (nested) scope level for the Logical System.
     *  <p>
     *  Adds a new ScopeDef item to the "stack" and
     *  increments the level number by 1 (all of the
     *  hard work is in endScope()!)
     *  <p>
     *  Note: *global* scope has level number = 0.
     *
     *  @see mmj.lang.ScopeDef
     */
    public void beginScope() {
        currScopeDef = new ScopeDef();
        scopeDefList.add(currScopeDef);

        //global scope level = 0
        scopeLvl     = scopeDefList.size() - 1;
    }


    /**
     *  Ends a (nested) scope level for the Logical System.
     *  <p>
     *  Processing:
     *  <p>
     *  1) if scope level = 0 (global), raise LangException.
     *  <p>
     *  otherwise,
     *  <p>
     *  2)
     *  <ol>
     *      <li>go through items in ScopeDef for the
     *          current level (Var, VarHyp, and LogVar)
     *          and mark them inactive (not forgetting the
     *          Var attribute "activeVarHyp" when deactivating
     *          the corresponding VarHyp.)
     *      <li>remove the scope level from the stack
     *      <li>decrement current scope level number
     *  </ol>
     *  @see mmj.lang.ScopeDef
     *
     *  @throws   LangException if scope is already at the global
     *            scope level.
     */
    public void endScope()
                        throws LangException {

        if (scopeLvl < 1) {
            throw new LangException(
                LangConstants.ERRMSG_CANNOT_END_GLOBAL_SCOPE);
        }

        ListIterator    x;

        x = currScopeDef.scopeLogHyp.listIterator();
        while (x.hasNext()) {
            ((LogHyp)x.next()).setActive(false);
        }

        x = currScopeDef.scopeVarHyp.listIterator();
        VarHyp vH;
        while (x.hasNext()) {
            vH = (VarHyp)x.next();
            vH.setActive(false);
            /**
             *  Can be only ONE active VarHyp for a Var at a time, so
             *  when deactivating VarHyp, go to its Var and remove
             *  set its "activeVarHyp" value to null.
             */
            (vH.getVar()).setActiveVarHyp(null);
        }

        x = currScopeDef.scopeVar.listIterator();
        while (x.hasNext()) {
            ((Var)x.next()).setActive(false);
        }

        scopeDefList.remove(scopeLvl);
        scopeLvl = scopeDefList.size() - 1; //global scope level = 0
        currScopeDef = (ScopeDef)scopeDefList.get(scopeLvl);
    }


    /**
     *  EOF processing for Logical System after file loaded.
     *  <p>
     *  Called at end of file to verify
     *  that there was an End Scope statement for each
     *  Begin Scope. At the time this routine is called,
     *  the ScopeDef list should contain just one entry,
     *  the "global" scope level (which is, by definition,
     *  level 0.)
     *  <p>
     *
     * @param  messages Messages object to error reporting.
     *
     * @param  prematureEOF signals LoadLimit requested by user
     *         has been reached, so stop loading even if in the
     *         middle of a scope level.
     *
     * @throws       LangException if scope is NOT at the global
     *               scope level UNLESS premature EOF signalled.
     *               (see <code>mmj.lang.LangConstants.java</code>)
     */
   public void finalizeEOF(MessageHandler messages,
                           boolean  prematureEOF)
                                throws LangException {
        setProofCompression(null); //free up memory
        if (scopeLvl != 0) {
            if (prematureEOF) {
                while (true) {
                    if (scopeLvl > 0) {
                        endScope();
                        continue;
                    }
                    break;
                }
            }
            else throw new LangException(
                    LangConstants.ERRMSG_MISSING_END_SCOPE_AT_EOF);
            }
        }

    /**
     *  Does Syntactical Analysis of the grammar and all
     *  statements in the LogicalSystem.
     *
     *  @param messageHandler  <code>MessageHandler</code> object for
     *                   reporting errors.
     */
    public void verifyAllSyntax(MessageHandler messageHandler)
                                            throws VerifyException {
        syntaxVerifier.parseAllFormulas(messageHandler,
                                        symTbl,
                                        stmtTbl);

    }

    /**
     *  Verifies every theorem's proof according to Metamath.pdf
     *  specifications.
     *
     *  @param messageHandler  <code>MessageHandler</code> object for
     *                   reporting errors.
     */
    public void verifyProofs(MessageHandler messageHandler)
                                            throws VerifyException {
        proofVerifier.verifyAllProofs(messageHandler,
                                      stmtTbl);
    }

    /**
     *  Double-checks the results of <code>verifyAllSyntax</code>
     *  by feeding the output <code>exprRPN</code>'s through
     *  the ProofVerifier.
     *  <p>
     *  Based on the fact that the output of the grammatical
     *  parser is a *proof* that a statement's formula can
     *  be generated using the grammatical parse tree -- which
     *  is stored in LogicalSystem in Reverse Polish Notation,
     *  like proofs.
     *  <p>
     *  Should not be necessary in normal use, but is very
     *  helpful for testing.
     *
     *  @param messageHandler  <code>MessageHandler</code> object for
     *                   reporting errors.
     */
    public void verifyAllExprRPNAsProofs(MessageHandler messageHandler)
                                            throws VerifyException {
        proofVerifier.verifyAllExprRPNAsProofs(messageHandler,
                                               stmtTbl);
    }

    /**
     *  Get LogicalSystem scopeDefList
     *  <p>
     *  scopeDefList is an ArrayList of ScopeDef objects where
     *  element 0 is global scope, element 1 is scope level 1, etc.
     *  At the end of LoadFile the scopeDefList should contain
     *  only 1 element -- the global ScopeDef.
     *  <p>
     *  @return ArrayList of ScopeDef objects.
     */
    public ArrayList getScopeDefList() {
        return scopeDefList;
    }

    /**
     *  Get current ScopeDef object in use.
     *  <p>
     *  @return current ScopeDef object in use.
     */
    public ScopeDef getCurrScopeDef() {
        return currScopeDef;
    }

    /**
     *  Get current scope level.
     *  <p>
     *  Level 0 is global scope, 1 is level 1, etc.
     *  <p>
     *  @return current scope level.
     */
    public int getScopeLvl() {
        return scopeLvl;
    }

    /**
     *  Returns the current ProofVerifier.
     */
    public ProofVerifier getProofVerifier() {
        return proofVerifier;
    }

    /**
     *  Sets the current ProofVerifier.
     *
     *  @param newProofVerifier a ProofVerifier or null is ok.
     *
     *  @return the old ProofVerifier in use in LogicalSystem.
     */
    public ProofVerifier setProofVerifier(
                            ProofVerifier newProofVerifier) {
        ProofVerifier oldProofVerifier = proofVerifier;
        proofVerifier                  = newProofVerifier;
        return        oldProofVerifier;
    }

    /**
     *  Returns the current SyntaxVerifier.
     */
    public SyntaxVerifier getSyntaxVerifier() {
        return syntaxVerifier;
    }

    /**
     *  Sets the current SyntaxVerifier.
     *
     *  @param newSyntaxVerifier  a SyntaxVerifier or null is ok.
     *
     *  @return the old SyntaxVerifier in use in LogicalSystem.
     */
    public SyntaxVerifier setSyntaxVerifier(
                             SyntaxVerifier newSyntaxVerifier) {
        SyntaxVerifier oldSyntaxVerifier = syntaxVerifier;
        syntaxVerifier                   = newSyntaxVerifier;
        return        oldSyntaxVerifier;
    }

    /**
     *  Returns the current symTbl, a Map containing all
     *  <code>Cnst</code>s and <code>Var</code>s.
     *  <p>
     *  Note: in theory, symTbl is an excellent candidate
     *        for being a real class. There are numerous
     *        operations involving it, and more possibilities.
     *
     *  @return symTbl map of all <code>Cnst</code>s and
     *        <code>Var</code>s.
     */
    public Map     getSymTbl() {
        return symTbl;
    }

    /**
     *  Returns the current stmtTbl, a Map containing all
     *  <code>Hyp</code>s and <code>Assrt</code>s.
     *  <p>
     *  Note: in theory, stmtTbl is an excellent candidate
     *        for being a real class. There are numerous
     *        operations involving it, and more possibilities.
     *
     *  @return stmtTbl map of all <code>Hyp</code>s and
     *        <code>Assrt</code>s.
     */
    public Map     getStmtTbl() {
        return stmtTbl;
    }

    /**
     *  Returns an instance of ProofCompression.
     *  <p>
     *  Reuses the existing ProofCompression instance if
     *  present, otherwise constructs one.
     *
     *  @return ProofCompression instance.
     */
    public ProofCompression getProofCompression() {
        if (proofCompression == null) {
            setProofCompression(
                new ProofCompression());
        }
        return proofCompression;
    }

    /**
     *  Sets the reference to the local ProofCompression
     *  instance.
     *  <p>
     *
     *  @param proofCompression instance or null.
     */
    public void setProofCompression(
                        ProofCompression proofCompression) {
        this.proofCompression     = proofCompression;
    }

    /**
     *  Returns the provable logic stmt type code string
     *  value.
     *  <p>
     *  (Default value is "|-", fyi.)
     *  <p>
     *  @return provableLogicStmtTypeParm.
     */
    public String getProvableLogicStmtTypeParm() {
        return provableLogicStmtTypeParm;
    }

    /**
     *  Returns the logic stmt type code string
     *  value.
     *  <p>
     *  (Default value is "wff", fyi.)
     *  <p>
     *  @return logicStmtTypeParm.
     */
    public String getLogicStmtTypeParm() {
        return logicStmtTypeParm;
    }

    /**
     *  Returns the Book Manager.
     *
     *  @return Book Manager in use (may not be enabled!)
     */
    public BookManager getBookManager() {
        return bookManager;
    }

    /**
     *  Add new BookManager Chapter.
     *  <p>
     *  See mmj.lang.BookManager.java for more info.
     *  <p>
     * @param  chapterTitle Title of chapter or blank or empty String.
     */
    public void addNewChapter(String chapterTitle, SourcePosition position) {
        if (isBookManagerEnabled()) {
            bookManager.addNewChapter(chapterTitle, position);
        }
    }

    /**
     *  Add new BookManager Section.
     *  <p>
     *  See mmj.lang.BookManager.java for more info.
     *  <p>
     * @param  sectionTitle Title of section or blank or empty String.
     */
    public void addNewSection(String sectionTitle, SourcePosition position) {
        if (isBookManagerEnabled()) {
            bookManager.addNewSection(sectionTitle, position);
        }
    }

    /**
     *  Is BookManager enabled?
     *  <p>
     *  If BookManager is enabled then Chapters and Sections
     *  will be stored.
     *  <p>
     * @return true if BookManager is enabled.
     */
    public boolean isBookManagerEnabled() {
        return bookManager.isEnabled();
    }

    /**
     *  Returns the SeqAssigner.
     *  <p>
     *  @return seqAssigner.
     */
    public SeqAssigner getSeqAssigner() {
        return seqAssigner;
    }

    /**
     *  Empties the TheoremLoaderCommitListener list.
     */
    public void clearTheoremLoaderCommitListenerList() {
        theoremLoaderCommitListeners.clear();
    }

    /**
     *  Adds a TheoremLoaderCommitListener to the
     *  TheoremLoaderCommitListener list if the instance
     *  is not already in the list.
     *  <p>
     *  @param t TheoremLoaderCommitListener object.
     */
    public void accumTheoremLoaderCommitListener(
                                TheoremLoaderCommitListener t) {

        if (!theoremLoaderCommitListeners.contains(t)) {
            theoremLoaderCommitListeners.add(t);
        }
    }

    /**
     *  Removes a TheoremLoaderCommitListener from the
     *  TheoremLoaderCommitListener list if the instance
     *  is in the list.
     *  <p>
     *  @param t TheoremLoaderCommitListener object.
     */
    public void removeTheoremLoaderCommitListener(
                                TheoremLoaderCommitListener t) {

        theoremLoaderCommitListeners.remove(t);
    }

    /**
     *  Reverses any changes made to the mmj2 state by TheoremLoader
     *  for a mmtTheoremSet set of updates.
     *  <p>
     *  Reverses any changes to the LogicalSystem scopeDefList.
     *  <p>
     *  Rollsback any changes to the SeqAssigner.
     *  <p>
     *  Scans the mmtTheoremSet and un-does each TheoremStmtGroup
     *  object's updates, if any,
     *  <p>
     *  @param mmtTheoremSet set of MMTTheoremStmtGroups which
     *         may or may not have already been updated into mmj2.
     *  @param errorMessage an explanatory message about the cause
     *         of the rollback to be inserted into a final message
     *         if the rollback itself fails.
     *  @param messages Messages object.
     *  @param auditMessages flag indicating whether or not audit
     *         messages about the rollback are to be written to
     *         the Messages object.
     *  @throws IllegalArgumentException if the rollback operation
     *         fails.
     */
    public void theoremLoaderRollback(MMTTheoremSet mmtTheoremSet,
                                      String        errorMessage,
                                      MessageHandler      messages,
                                      boolean       auditMessages) {

        String abortMessage       = null;
        try {
            // almost forgot :-) should be only one level to undo...
            if (getScopeLvl() > 0) {
                endScope();
            }

            // only seqAssigner is rolled back because the other
            // places where stmtTbl entries are distributed are
            // not updated until the Theorem Loader updates are
            // committed (see LogicalSystem.theoremLoaderCommit()).
            if (seqAssigner != null) {
                seqAssigner.rollback(mmtTheoremSet,
                                     messages,
                                     auditMessages);
            }

            Iterator i            = mmtTheoremSet.iterator();
            TheoremStmtGroup g;
            while (i.hasNext()) {
                g                 = (TheoremStmtGroup)i.next();
                g.reverseStmtTblUpdates(stmtTbl);
            }
            return;
        }
        catch (LangException e) {
            abortMessage          = new String(
                LangConstants.
                    ERRMSG_THEOREM_LOADER_ROLLBACK_FAILED_1
                + " (1) "
                + errorMessage
                + LangConstants.
                    ERRMSG_THEOREM_LOADER_ROLLBACK_FAILED_2

                + e.getMessage());
        }
        catch (IllegalArgumentException e) {
            abortMessage          = new String(
                LangConstants.
                    ERRMSG_THEOREM_LOADER_ROLLBACK_FAILED_1
                + " (2) "
                + errorMessage
                + LangConstants.
                    ERRMSG_THEOREM_LOADER_ROLLBACK_FAILED_2
                + e.getMessage());
        }
        throw new IllegalArgumentException(abortMessage);
    }

    /**
     *  Finalizes any changes made to the mmj2 state by TheoremLoader
     *  for a mmtTheoremSet set of updates.
     *  <p>
     *  Commits SeqAssigner updates.
     *  <p>
     *  Commits BookManager updates.
     *  <p>
     *  Sends commit() request to every TheoremLoaderCommitListener.
     *  <p>
     *  <p>
     *  @param mmtTheoremSet set of MMTTheoremStmtGroups which
     *         may or may not have already been updated into mmj2.
     *  @throws IllegalArgumentException if the commit operation
     *         fails.
     */
    public void theoremLoaderCommit(MMTTheoremSet mmtTheoremSet) {

        try {
            if (seqAssigner != null) {
                seqAssigner.commit(mmtTheoremSet);
            }

            if (bookManager != null) {
                bookManager.commit(mmtTheoremSet);
            }

            Iterator i                =
                theoremLoaderCommitListeners.iterator();
            while (i.hasNext()) {
                ((TheoremLoaderCommitListener)i.next()).
                    commit(
                        mmtTheoremSet);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                LangConstants.
                    ERRMSG_THEOREM_LOADER_COMMIT_FAILED
                + e.getMessage());
        }
    }

    private void dupCheckSymAdd(Sym existingSym) {

        if (existingSym != null) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_DUP_SYM_MAP_PUT_ATTEMPT
                + existingSym.getId());
        }
    }

    private void dupCheckStmtAdd(Stmt existingStmt) {

        if (existingStmt != null) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_DUP_STMT_MAP_PUT_ATTEMPT
                + existingStmt.getLabel());
        }
    }
}

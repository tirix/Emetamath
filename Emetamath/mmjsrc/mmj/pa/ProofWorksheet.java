//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  ProofWorksheet.java  0.09 08/01/2008
 *  <code>
 *  Version 0.02:
 *      - See extended "Derive" Feature notes below
 *      - update misc. comments
 *      - add new ProofWorksheet fields to support "Derive" feature:
 *            int   greatestStepNbr;
 *            int   greatestDummy VarNbr;
 *
 *      - add new ProofStepStmt fields to support "Derive" feature:
 *            boolean generatedByDeriveFeature;
 *
 *      - add new DerivationStep fields to support "Derive" feature:
 *            boolean deriveStepFormula;
 *            boolean deriveStepHyps;
 *            int     nbrMissingHyps;
 *
 *      - added new helper routines for ProofUnifier
 *            - generateNewGreatestDummy VarNbr()
 *            - generateNewDerivedStepNbr()
 *            - loadGeneratedFormulaIntoDerivStep()
 *            - findMatchingStepFormula()
 *            - addDerivStepForDeriveFeature()
 *
 *      - added renumbering feature:
 *            - proofWorksheet.renumberProofSteps()
 *
 *  07-Sep-2006 Version 0.03 - add TMFF stuff (extensive).
 *            - added hintRefList for partial unification Hints
 *              which list assertions that unify with the
 *              DerivationStep's formula regardless of
 *              hypotheses (not done for derived steps
 *              or formulas and displayed only if the step
 *              fails to unify perfectly.)
 *            - modified to conform to Metamath.pdf spec change
 *              of 6-24-2006 prohibiting Stmt label and Sym id
 *              namespace collisions.
 *
 *
 *  01-Jun-2007 Version 0.04
 *            - de-nest inner classes
 *            - replace ProofWorkStmt.status
 *
 *
 *  Aug-01-2007 Version 0.05
 *            - misc changes for Work Var enhancement
 *            - ...one of which is removing "dummy vars"!
 *
 *  Nov-01-2007 Version 0.06
 *            - replace (comment out) posCursorAtFirstIncompleteStmt()
 *              with posCursorAtLastIncompleteStmt().
 *            - add loadWorksheetProofLevelNumbers() call to
 *              loadWorksheet() in the *finale* of the load.
 *            - Modify buildExportTheoremProofBody() to pass
 *              ProofDerivationStepEntry.proofLevel to the
 *              DerivationStep and HypothesisStep constructors.
 *            - ModifyBuildEmptyTheoremProofBody() and
 *              buildDummyProofBody() to pass
 *              proofLevel 0 to the DerivationStep and
 *              HypothesisStep constructors.
 *            - Modify loadWorksheet() to handle the new
 *              "Local Ref" escape character, "#" and
 *              call loadLocalRefDerivationStep().
 *            - Add ERRMSG_HYP_HAS_LOCAL_REF_1, "E-PA-0377"
 *            - Add ERRMSG_QED_HAS_LOCAL_REF_1, "E-PA-0378"
 *            - Add findFirstMatchingRefOrStep()
 *            - Add posCursorAtLastIncompleteOrQedStmt() for
 *              use by ProofAsst -- to call when RPN proof
 *              generated successfully.
 *
 *  Feb-01-2008 Version 0.07
 *            - In getOutputMessageText() display error messages
 *              before info messages. This corresponds to the
 *              Nov-01-2007 release's cursor positioning scheme
 *              so that when the cursor is positioned to the
 *              first error, the error messages appear at the
 *              top of the message area.
 *            - Add ProofAsstCursor "proofInputCursor" to ProofWorksheet.
 *              and added code to loadWorksheet to invoke
 *              setInputCursorStmtIfHere() or
 *              setInputCursorPosIfHere() to "find" the input cursor
 *              during load operations.
 *            - Modify tmffReformat() to reformat just one step.
 *            - "re-added back" posCursorAtFirstIncompleteOrQedStmt();
 *            - Add outputCursorInstrumentationIfEnabled()
 *
 *  Mar-01-2008 Version 0.08
 *            - Added code for StepSelector enhancement.
 *
 *  Aug-01-2008 Version 0.09
 *            - Added getGeneratedProofStmt() to make it available
 *              after successful unification. Returns null if
 *              the GeneratedProofStmt is not present in the
 *              ProofWorksheet (e.g. not unified...)
 *            - Added getHypothesisStepFromList()
 *                and getHypStepCnt()
 *  </code>
 */

package mmj.pa;

import  java.io.File;
import  java.io.IOException;
import  java.util.ArrayList;
import  java.util.LinkedList;
import  java.util.Iterator;
import  java.util.HashMap;
import  java.util.NoSuchElementException;
import  mmj.util.DelimitedTextParser;
import  mmj.lang.*;
import  mmj.verify.*;
import  mmj.mmio.*;
import  mmj.tmff.*;

/**
 *  ProofWorksheet is generated from a text area (String)
 *  using ProofWorksheet methods.
 *  <p>
 *  There are several inner classes, and due to the
 *  interrelated nature of the statements of a proof
 *  all of these classes are put together in ProofWorksheet
 *  (inner classes can access the outer class methods and
 *  elements.)
 *  <p>
 *  A large quantity of information and useful stuff is
 *  contained in mmj.pa.PaConstants.java.
 *  <p>
 *  =====================================================
 *  <p>
 *  <b>"Derive" Feature Notes (Version 0.02)
 *  <p>
 *  =====================================================
 *  <p>
 *  The are a few changes in ProofWorksheet to support the
 *  Proof Assistant "Derive" feature. The major changes involve
 *  the Hyp and Formula fields in DerivationStep:
 *  <p>
 *  1. Formula: on DerivationSteps except for the "qed" step,
 *  Formula is now optional, but if Formula is not entered
 *  then Ref is required. Simple: Formula and/or Ref required
 *  on non-"qed" derivation steps.
 *  <p>
 *     If Formula is not entered, then the first token after
 *     the Step:Hyp:Ref field's token must be the start of a
 *     new statement in column 1 of a new line.
 *  <p>
 *     The new "deriveStepFormula" flag is set to true if Formula
 *     not input on a non-"qed" derivation step.
 *  <p>
 *  2, Hyp: "?" Hyp entries in the Hyp sub-field of Step:Hyp:Ref
 *  have a new meaning if Ref is input; there are new
 *  validation edits in this case. NOTE: this applies even to
 *  the "qed" step, but of course not the HypothesisSteps,
 *  for which a Hyp entry is meaningless and forbidden.
 *  <p>
 *     If one or more Hyp entries is "?" and Ref is input, then
 *     the ProofUnifier will invoke the "Derive" feature. The
 *     new "DeriveStepHyps" flag is set to true and the number
 *     of non-"?" Hyp entries must be < the number of Logical Hyps
 *     for the Ref assertion. If fewer Hyp entries are input
 *     than needed by the Ref, and one of the Hyp entries is "?",
 *     then extra "?" entries are suffixed to the Hyp sub-field.
 *     For example, if the user inputs Hyp = "?,2" and the Ref
 *     requires 3 Logical Hypotheses then the Hyp is automatically
 *     expanded to "?,2,?". Excessive "?" Hyps are also "forgiven",
 *     meaning that if the user inputs "?,2,?" and the Ref requires
 *     only 2 Logical Hypotheses, then the Hyp is shortened to
 *     "?,2" (the excess "?" Hyps are removed from the right.)
 *  <p>
 *     The new int field, "nbrMissingHyps" is set to the number
 *     of "?" Hyp entries -- after adjustment for the number of
 *     Logical Hypotheses needed by the Ref.
 *  <p>
 *     The new ProofStepStmt field,
 *     <code>generatedByDeriveFeature</code> designates a step
 *     as being automatically generated by the "Derive" feature.
 *     When set to true a proof step can be subjected to
 *     extra/custom processing, for example, after the unification
 *     search process.
 *  <p>
 *  =====================================================
 */
public class ProofWorksheet {

    ProofAsstPreferences        proofAsstPreferences;
    LogicalSystem               logicalSystem;
    Grammar                     grammar;
    MessageHandler              messageHandler;
    Tokenizer                   proofTextTokenizer;
    TMFFPreferences             tmffPreferences;
    TMFFStateParams             tmffSP;
    StringBuffer                tmffFormulaSB;


    StepSelectorResults         stepSelectorResults = null;
    StepRequest                 stepRequest         = null;

    /* friendly */  boolean            structuralErrors;
    /* friendly */  int                nbrDerivStepsReadyForUnify
                                  = 0;

    /*  hasWorkVarsOrDerives is set to true in DerivationStep
        and is used in ProofUnifier to decide whether or not
     *  to make a preliminary pass through the Proof Worksheet
     *  to deal with WorkVars and/or DeriveStep/DeriveFormula.
     */
    /* friendly */  boolean hasWorkVarsOrDerives;


    /* friendly */  boolean            newTheorem
                                  = true;
    /* friendly */  Theorem            theorem;
    /* friendly */  Stmt               locAfter;

    /* friendly */  int                maxSeq
                                  = Integer.MAX_VALUE;

    /* friendly */  MandFrame          comboFrame;
    /* friendly */  HashMap            comboVarMap;

    public VarHyp getVarHypFromComboFrame(Var v) {
        Hyp[] a                   = comboFrame.hypArray;
        for (int i = 0; i < a.length; i++) {
            if (a[i].isVarHyp() &&
                ((VarHyp)a[i]).getVar() == v) {
                return (VarHyp)a[i];
            }
        }
        return null;
    }

    /* friendly */  ArrayList          proofWorkStmtList;
    /* friendly */  HeaderStmt         headerStmt;

    /* friendly */  FooterStmt         footerStmt;

    /* friendly */  DerivationStep     qedStep;

                    //set when created after successful unification...
    /* friendly */  GeneratedProofStmt generatedProofStmt;

    /* friendly */  int                greatestStepNbr;

    /* friendly */  int                dvStmtCnt;
    /* friendly */  DistinctVariablesStmt[]
                                dvStmtArray;

    /* friendly */  int                hypStepCnt;

    /* friendly */  ProofAsstCursor    proofCursor;

    /* friendly */  ProofAsstCursor    proofInputCursor;

                    //this is an an ArrayList of ArrayLists
    /* friendly */  ArrayList proofSoftDjVarsErrorList;

    /**
     *  Constructor for skeletal ProofWorksheet.
     *
     *  This constructor is used in
     *  ProofAsst.updateWorksheetWithException().
     *  to create a worksheet that has "structuralErrors".
     *  When the GUI displays a worksheet with structural
     *  errors it does not update its proofTextArea,
     *  and thus, the original user input is left untouched.
     *
     *  @param proofAsstPreferences variable settings
     *  @param messages the mmj.lang.Messages object used to store
     *                  error and informational messages.
     *  @param structuralErrors indicates whether or not the
     *                          ProofWorksheet has severe/fatal
     *                          validation errors.
     *  @param proofCursor ProofAsstCursor set to position
     *                     of error.
     */
    public ProofWorksheet(
                ProofAsstPreferences proofAsstPreferences,
                MessageHandler       messageHandler,
                boolean              structuralErrors,
                ProofAsstCursor      proofCursor) {

        this.proofAsstPreferences =  proofAsstPreferences;
        this.messageHandler       =  messageHandler;
        setStructuralErrors(structuralErrors);
        this.proofCursor          =  proofCursor;
        proofInputCursor          =  new ProofAsstCursor();
        proofWorkStmtList         =  new ArrayList();
    }

    /**
     *  Constructor creating empty ProofWorksheet to be loaded
     *  using a Tokenizer.
     *
     *  This constructor is used by ProofWorksheetParser.next().
     *
     *  @param proofTextTokenizer the mmj.mmio.Tokenizer input stream
     *                            parser.
     *  @param proofAsstPreferences variable settings
     *  @param logicalSystem the loaded Metamath data
     *  @param grammar the mmj.verify.Grammar object
     *  @param messages the mmj.lang.Messages object used to store
     *                  error and informational messages.
     */
    public ProofWorksheet(
                Tokenizer            proofTextTokenizer,
                ProofAsstPreferences proofAsstPreferences,
                LogicalSystem        logicalSystem,
                Grammar              grammar,
                MessageHandler       messageHandler) {

        this.proofTextTokenizer   =  proofTextTokenizer;
        this.proofAsstPreferences =  proofAsstPreferences;
        this.logicalSystem        =  logicalSystem;
        this.grammar              =  grammar;
        this.messageHandler       =  messageHandler;

        proofCursor               =  new ProofAsstCursor();
        proofInputCursor          =  new ProofAsstCursor();

        proofWorkStmtList         =  new ArrayList();

        // initialize StepUnifier prior to parsing input
        // tokens, which may contain work variables!!!
        proofAsstPreferences.
            getStepUnifier().
                startProofWorksheet();


        initTMFF();
    }

    /**
     *  Constructor creating a ProofWorksheet initialized
     *  for a new proof for a specific theorem label
     *
     *  This constructor is used by ProofAsst.startNewProof().
     *
     *  Note: the ProofWorksheet created here is not a fully
     *        populated object -- it is destined for a one-way
     *        trip to the output screen via the GUI.
     *
     *  @param newTheoremLabel Theorem label String.
     *  @param proofAsstPreferences variable settings
     *  @param logicalSystem the loaded Metamath data
     *  @param grammar the mmj.verify.Grammar object
     *  @param messages the mmj.lang.Messages object used to store
     *                  error and informational messages.
     */
    public ProofWorksheet(
                String               newTheoremLabel,
                ProofAsstPreferences proofAsstPreferences,
                LogicalSystem        logicalSystem,
                Grammar              grammar,
                MessageHandler       messageHandler) {

        this.proofAsstPreferences =  proofAsstPreferences;
        this.logicalSystem        =  logicalSystem;
        this.grammar              =  grammar;
        this.messageHandler       =  messageHandler;

        proofCursor               =  new ProofAsstCursor();
        proofInputCursor          =  new ProofAsstCursor();

        proofWorkStmtList         =  new ArrayList();

        initTMFF();

        //build header
        buildHeader(newTheoremLabel);

        if (isNewTheorem()) {
            buildDummyProofBody();
        }
        else {
            buildTheoremDescription(theorem);
            buildEmptyTheoremProofBody(theorem);
        }

        buildFooter();

    }

    /**
     *  Constructor used for exporting a Proof Worksheet
     *  containing a completed proof.
     *
     *  Note: the worksheet created by this constructor
     *        is "skeletal" in the sense that it is
     *        destined for output only.
     *
     *  This constructor is used by ProofAsst.exportToFile().
     *
     *  @param theorem to be used to create ProofWorksheet.
     *  @param proofDerivationStepList ArrayList of
     *                   mmj.verify.ProofDerivationStepEntry
     *                   created by VerifyProofs
     *  @param proofAsstPreferences variable settings
     *  @param logicalSystem the loaded Metamath data
     *  @param grammar the mmj.verify.Grammar object
     *  @param messages the mmj.lang.Messages object used to store
     *                  error and informational messages.
     */
    public ProofWorksheet(
                Theorem              theorem,
                ArrayList            proofDerivationStepList,
                boolean              deriveFormulas,
                ProofAsstPreferences proofAsstPreferences,
                LogicalSystem        logicalSystem,
                Grammar              grammar,
                MessageHandler       messageHandler) {

        this.proofAsstPreferences =  proofAsstPreferences;
        this.logicalSystem        =  logicalSystem;
        this.grammar              =  grammar;
        this.messageHandler       =  messageHandler;

        proofCursor               =  new ProofAsstCursor();
        proofInputCursor          =  new ProofAsstCursor();

        initTMFF();

        this.theorem              =  theorem;

        setMaxSeq(theorem.getSeq());

        setNewTheorem(false);

        loadComboFrameAndVarMap(); //for formula parsing

        proofWorkStmtList         =  new ArrayList();

        buildHeader(theorem.getLabel());

        buildTheoremDescription(theorem);

        buildExportTheoremProofBody(theorem,
                                    proofDerivationStepList,
                                    deriveFormulas);
        buildFooter();
    }


    private void initTMFF() {
        tmffPreferences           =
            proofAsstPreferences.getTMFFPreferences();

        tmffFormulaSB             = new StringBuffer();

        tmffSP                    =
            new TMFFStateParams(tmffFormulaSB,
                                0, //prevColNbr
                                tmffPreferences);
    }


    /**
     *  Gets structuralErrors switch for ProofWorksheet.
     *
     *  A "structural error" means that the Proof Worksheet
     *  contains one or more validation errors that
     *  prevent Unification. An example would be any
     *  individual field error or a parse error, theorem
     *  label not found, etc. When ProofWorksheet is
     *  done validating a proof it must be *clean* for
     *  ProofUnifier (or else bad things would happen.)
     *
     *  @return      boolean structuralErrors
     */
    public boolean hasStructuralErrors() {
        return structuralErrors;
    }

    /**
     *  Sets structuralErrors switch for ProofWorksheet.
     *  <p>
     *  See hasStructuralErrors() for more info.
     *  <p>
     *  @param structuralErrors boolean, true or false.
     */
    public void setStructuralErrors(boolean structuralErrors) {
        this.structuralErrors     = structuralErrors;
    }

    /**
     *  Gets the number of proof derivation steps that are
     *  ready for Unification.
     *  <p>
     *  Note: a derivation step with a "?" in its Hyp field
     *  is not ready for unification, but interestingly, a
     *  subsequent step that refers to that step as one of its
     *  Hyps, can be unified (it just can't be "proved".)
     *
     *  @return      number of derivation steps in the proof that
     *           are ready for Unification.
     */
    public int getNbrDerivStepsReadyForUnify() {
        return nbrDerivStepsReadyForUnify;
    }

    public void incNbrDerivStepsReadyForUnify() {
        ++nbrDerivStepsReadyForUnify;
    }

    /**
     *  Returns the isNewTheorem boolean value indicating
     *  whether the theorem is new or is already in the
     *  Metamath file that was loaded.
     *
     *  @return      boolean isNewTheorem, which if true means
     *           that the theorem being proved is not already
     *           in the Metamath database that was loaded.
     */
    public boolean isNewTheorem() {
        return newTheorem;
    }

    public void setNewTheorem(boolean newTheorem) {
        this.newTheorem           = newTheorem;
    }


    /**
     *  Returns the ProofWorksheet's Theorem reference.
     *
     *  @return      Theorem may be null if new theorem or errors
     *           in ProofWorksheet!
     */
    public Theorem getTheorem() {
        return theorem;
    }

    /**
     *  Returns the ProofWorksheet theorem maxSeq value.
     *  <p>
     *  For an existing theorem (in the loaded database),
     *  maxSeq is just the MObj.seq number of the theorem
     *  itself. For a new theorem the LocAfter statement
     *  label defines the maxSeq (maxSeq = locAfter.seq + 1)
     *  <p>
     *  The maxSeq value sets a boundary for parsing, proofs,
     *  formulas, etc. A Metamath statement cannot legitimately
     *  use or refer to another Metamath statement with a
     *  sequence number >= its own (no recursive references.)
     *  <p>
     *  @return      maxSeq number in use for the current proof.
     */
    public int getMaxSeq() {
        return maxSeq;
    }

    public void setMaxSeq(int maxSeq) {
        this.maxSeq               = maxSeq;
    }

    /**
     *  Returns the proof theorem's "comboFrame".
     *  <p>
     *  "comboFrame" and comboVarMap combine the optional and
     *  mandatory frame entries for the theorem, including
     *  any $d statements added as part of the proof.
     *  <p>
     *  For an existing theorem this just means merging the
     *  Assrt.mandFrame, Theorem.optFrame and any proof $d's,
     *  and then deriving the comboVarMap from the set
     *  of VarHyp's in the comboFrame.
     *  <p>
     *  For a new theorem this means constructing comboFrame
     *  using (ScopeDef)(LogicalSystem.getScopeDefList()).get(0)
     *  to obtain the sets of globally active Var's, VarHyp's
     *  and DjVars (new Theorems in ProofAsst can only use
     *  global scope Var's and VarHyp's) and adding in any
     *  $d's from the proof.
     *  <p>
     *  The REASON why the optional and mandatory frames can
     *  be combined in this way is a little bit subtle: they
     *  only need to be separate if the new theorem is going
     *  to be referred to in later theorems' proofs, and
     *  ProofAsst does not provide that capability at this
     *  time.
     *  <p>
     *  The REASON why we want to combine the optional and
     *  mandatory frames is to simplify handling of
     *  derivation steps within the proof. Variables used
     *  in the intermediate steps would normally be part of
     *  the optional frame, if not used in the theorem's
     *  formula or its LogHyp formulas. That would create
     *  more work, especially at grammatical parse time,
     *  when we need to match each Var to its active VarHyp.
     *  Soooo...instead of building a mandatory frame for each
     *  intermediate step we just build a combo frame for
     *  use in every step (and note that DjVars apply to
     *  every step regardless of the location of the $d
     *  ProofWorkStmt within the Proof Text area.
     *
     *  @return      MandFrame combined with OptFrame values for
     *           the theorem.
     */
    public MandFrame getComboFrame() {
        return comboFrame;
    }


    /**
     *  Gets the hypStepCnt counter of the number of HypothesisStep
     *  statements in the ProofWorksheet.
     *  <p>
     *  @return the hypStepCnt.
     */
    public int getHypStepCnt() {
        return hypStepCnt;
    }

    /**
     *  Returns a given HypothesisStep from the
     *  ProofWorkStmtList.
     *  <p>
     *  @param h the LogHyp sought in the ProofWorkStmtList.
     *  @return the HypothesisStep if found, or null.
     */
    public HypothesisStep getHypothesisStepFromList(LogHyp h) {
        Iterator iterator         = getProofWorkStmtListIterator();
        while (iterator.hasNext()) {
            ProofWorkStmt w       = (ProofWorkStmt)iterator.next();
            if (w.isHypothesisStep()) {
                HypothesisStep hypothesisStep
                                  = (HypothesisStep)w;
                if (h == hypothesisStep.getRef()) {
                    return hypothesisStep;
                }
            }
        }
        return null;
    }


    /**
     *  Returns an Iterator over the ProofWorksheet
     *  ProofWorkStmt ArrayList.
     *  <p>
     *
     *  @return Iterator over ProofWorkStmtList.
     */
    public Iterator getProofWorkStmtListIterator() {
        return proofWorkStmtList.iterator();
    }

    /**
     *  Returns the count of items in the ProofWorksheet
     *  ProofWorkStmt ArrayList.
     *  <p>
     *  @return count of items in ProofWorkStmtList.
     */
    public int getProofWorkStmtListCnt() {
        return proofWorkStmtList.size();
    }

    /**
     *  Computes the line number of a ProofWorkStmt on the screen
     *  text area.
     *  <p>
     *  This algorithm requires that we know in advance how
     *  many lines are occupied by each ProofWorkStmt. The
     *  computation is then simple: just total the previous
     *  lineCnt's and add 1. (But if lineCnt is wrong, then
     *  we are doomed -- note that TMFF went to a lot of
     *  trouble to obtain lineCnt, and lineCnt is computed
     *  during parsing of an input ProofWorksheet!)
     *  <p>
     *  @return line number in ProofWorksheet text area.
     */
    public int computeProofWorkStmtLineNbr(ProofWorkStmt x) {
        int           total       = 0;
        ProofWorkStmt y;
        Iterator      iterator    = getProofWorkStmtListIterator();
        while (iterator.hasNext()) {
            y                     =
                (ProofWorkStmt)iterator.next();
            if (x == y) {
                return total + 1;
            }
            total                += y.getLineCnt();
        }
        return total;
    }

    /**
     *  Determines which ProofWorkStmt is located at a given
     *  line number of the screen text area.
     *  <p>
     *  @return ProofWorkStmt at the input line number, or null.
     */
    public ProofWorkStmt
                        computeProofWorkStmtOfLineNbr(int n) {
        int           total       = 0;
        ProofWorkStmt y;
        Iterator      iterator    = getProofWorkStmtListIterator();
        while (iterator.hasNext()) {
            y                     =
                (ProofWorkStmt)iterator.next();
            if (total + y.getLineCnt() >= n) {
                return y;
            }
            total                += y.getLineCnt();
        }
        return null;
    }

    /**
     *  Computes the total number of text area lines required
     *  to display all ProofWorkStmt objects in the ProofWorksheet.
     *  <p>
     *  @return total number of lines required for
     *          ProofWorkStmt's.
     */
    public int computeTotalLineCnt() {
        Iterator iterator         = proofWorkStmtList.iterator();
        ProofWorkStmt x;
        int           total       = 0;
        while (iterator.hasNext()) {
            x = (ProofWorkStmt)iterator.next();
            total                += x.getLineCnt();
        }
        return total;
    }


    /**
     *  Returns the ProofWorksheet FooterStmt object.
     *  <p>
     *  @return FooterStmt of ProofWorksheet.
     */
    public FooterStmt getFooterStmt() {
        return footerStmt;
    }

    /**
     *  Returns the QED step of the proof, which is the
     *  final derivation step.
     *  <p>
     *  Note: the nomenclature here "qed step" is
     *        something made up for ProofAssistant
     *        to make things easier to explain.
     *
     *  @return      the final DerivationStep in the ProofWorksheet.
     */
    public DerivationStep getQedStep() {
        return qedStep;
    }

    /**
     *  Returns the proof RPN of the QED step of the proof.
     *  <p>
     *  Note that each DerivationStep will have its own
     *  proof -- if the proof is valid -- but the QED
     *  step's proof is the proof of the theorem itself!
     *
     *  @return      the RPN proof of the final DerivationStep
     *           in the ProofWorksheet.
     */
    public Stmt[] getQedStepProofRPN() {
        Stmt[] rpn                = null;
        if (qedStep != null) {
            ParseTree p           = qedStep.getProofTree();
            if (p != null) {
                rpn               = p.convertToRPN();
            }
        }
        return rpn;
    }



    private int updateNextGreatestStepNbr(int stepNbr) {
        if (stepNbr > greatestStepNbr) {
            greatestStepNbr       = stepNbr;
        }
        return greatestStepNbr;
    }


    /**
     *  Get the ProofWorksheet ProofCursor object.
     *
     *  @return ProofCursor object for ProofWorksheet.
     */
    public ProofAsstCursor getProofCursor() {
        return proofCursor;
    }

    /**
     *  Set the ProofWorksheet ProofCursor object.
     *
     *  @param proofCursor object for ProofWorksheet.
     */
    public void setProofCursor(ProofAsstCursor proofCursor) {
        this.proofCursor          = proofCursor;
    }

    /**
     *  If cursor not already set, positions the ProofWorksheet
     *  ProofCursor at the last ProofWorkStmt with status =
     *  incomplete and sets the cursor at the start of the Ref
     *  sub-field. If cursor still not set (no incomplete steps),
     *  positions the cursor at the 'qed' step.
     */
    public void posCursorAtLastIncompleteOrQedStmt() {
        if (!proofCursor.cursorIsSet) {
            posCursorAtLastIncompleteStmt();
            if (!proofCursor.cursorIsSet) {
                proofCursor.
                    setCursorAtProofWorkStmt(
                        qedStep,
                        PaConstants.FIELD_ID_REF);
            }
        }
    }

    /**
     *  If cursor not already set, positions the ProofWorksheet
     *  ProofCursor at the first ProofWorkStmt with status =
     *  incomplete and sets the cursor at the start of the Ref
     *  sub-field. If cursor still not set (no incomplete steps),
     *  positions the cursor at the 'qed' step.
     */
    public void posCursorAtFirstIncompleteOrQedStmt() {
        if (!proofCursor.cursorIsSet) {
            posCursorAtFirstIncompleteStmt();
            if (!proofCursor.cursorIsSet) {
                proofCursor.
                    setCursorAtProofWorkStmt(
                        qedStep,
                        PaConstants.FIELD_ID_REF);
            }
        }
    }

    /**
     *  Positions the ProofWorksheet ProofCursor at the
     *  last ProofWorkStmt with status = incomplete and
     *  sets the cursor at the start of the Ref sub-field.
     */
    public void posCursorAtLastIncompleteStmt() {

        ProofWorkStmt s;
        int i                     = proofWorkStmtList.size();
        while (--i > 0) {
            s                     =
                (ProofWorkStmt)proofWorkStmtList.get(i);

            if (s.stmtIsIncomplete()) {
                proofCursor.
                    setCursorAtProofWorkStmt(
                        s,
                        PaConstants.FIELD_ID_REF);
                break;
            }
        }
    }

    /**
     *  Positions the ProofWorksheet ProofCursor at the
     *  first ProofWorkStmt with status = incomplete and
     *  sets the cursor at the start of the Ref sub-field.
     */
    public void posCursorAtFirstIncompleteStmt() {

        ProofWorkStmt s;
        Iterator i                = getProofWorkStmtListIterator();
        while (i.hasNext()) {
            s                     = (ProofWorkStmt)i.next();

            if (s.stmtIsIncomplete()) {
                proofCursor.
                    setCursorAtProofWorkStmt(
                        s,
                        PaConstants.FIELD_ID_REF);
                break;
            }
        }
    }

    public void outputCursorInstrumentationIfEnabled() {
        if (proofAsstPreferences.getOutputCursorInstrumentation()) {
        	messageHandler.accumInfoMessage(
                proofCursor.
                    outputCursorInstrumentation(
                        getErrorLabelIfPossible()));
        }
    }

    public String getErrorLabelIfPossible() {
        String label              = getTheoremLabel();
        if (label == null) {
            return PaConstants.PA_UNKNOWN_THEOREM_LABEL;
        }
        return label;
    }

    /**
     *  Returns the theorem label, if present.
     *
     *  @return      String containing theorem label, may be null;
     */
    public String getTheoremLabel() {
        if (headerStmt != null) {
            return headerStmt.theoremLabel;
        }
        else {
            return null;
        }
    }


    /**
     *  Searches up to an exclusive endpoint in the
     *  proofWorkStmtList for a step whose formula
     *  matches the input formula.
     *
     *  @param  searchFormula Formula we're looking for
     *  @param  exclusiveEndpointStep Exclusive endpoint of the
     *               search (return null as soon as this step
     *               is reached, even if its formula matches.)
     *
     *  @return ProofStepStmt matching the formula or null
     *          if Not Found.
     */
    public ProofStepStmt findMatchingStepFormula(
                             Formula       searchFormula,
                             ProofStepStmt exclusiveEndpointStep) {

        Iterator      iterator    = getProofWorkStmtListIterator();
        ProofWorkStmt o;
        ProofStepStmt matchStep;
        while (iterator.hasNext()) {
            o                     = (ProofWorkStmt)iterator.next();
            if (o == exclusiveEndpointStep) {
                break;
            }
            if (o.isProofStep()) {
                matchStep         = (ProofStepStmt)o;
                if (matchStep.formula.equals(searchFormula)) {
                    return matchStep;
                }
            }
        }
        return null;
    }

    /**
     *  Renumbers each ProofWorkStmt according to an input
     *  renumberInterval and alters each Hyp reference to
     *  conform to the new step numbers.
     *
     *  @param  renumberInterval is the number to add to each
     *          new step number. Commonly equal to 1.
     */
    public void renumberProofSteps(int renumberInterval) {

        Iterator       iterator   = getProofWorkStmtListIterator();
        ProofWorkStmt  o;

        ProofStepStmt  renumberProofStepStmt;
        HypothesisStep renumberHypothesisStep;
        DerivationStep renumberDerivationStep;

        int            renumber   = 0;
        String         renumberStep;
        String         oldStep;

        HashMap renumberHashMap   =
            new HashMap(getProofWorkStmtListCnt() * 2);

        while (iterator.hasNext()) {
            o                     = (ProofWorkStmt)iterator.next();
            if (!o.isProofStep()) {
                continue;
            }

            renumberProofStepStmt = (ProofStepStmt)o;
            oldStep               = renumberProofStepStmt.step;

            renumber             += renumberInterval;
            renumberStep          = Integer.toString(renumber);

            if (!oldStep.equals(renumberStep) &&
                !oldStep.equals(PaConstants.QED_STEP_NBR)) {
                renumberHashMap.put(oldStep,
                                    renumberStep);
            }

            //no attempt at polymorphism, just quick and dirty fix
            if (o.isHypothesisStep()) {
                renumberHypothesisStep
                                  = (HypothesisStep)o;
                renumberHypothesisStep.renum(renumberHashMap);
                continue;
            }
            if (o.isDerivationStep()) {
                renumberDerivationStep
                                  = (DerivationStep)o;
                renumberDerivationStep.renum(renumberHashMap);
                continue;
            }
        }
    }

    /**
     *  Reformats all or just one ProofStepStmt using TMFF.
     *
     *  @param inputCursorStep set to true to reformat just
     *                         the proof step underneath the
     *                         cursor.

     */
    public void tmffReformat(boolean inputCursorStep) {
        ProofWorkStmt o           = null;
        if (inputCursorStep) {
            if (proofInputCursor.cursorIsSet &&
                (o = proofInputCursor.proofWorkStmt) != null) {
                o.tmffReformat();
            }
        }
        else {
            Iterator i            = getProofWorkStmtListIterator();
            while (i.hasNext()) {
                (o = (ProofWorkStmt)i.next()).tmffReformat();
            }
        }

        if (o == getQedStep()) {
            doubleSpaceQedStep();
        }
    }

    /**
     *  Add extra newline to end of qed step so that
     *  the footer step has a blank line before it.
     */
    public void doubleSpaceQedStep() {
        DerivationStep d          = getQedStep();
        if (d != null) {
            d.stmtText.append(
                PaConstants.PROOF_WORKSHEET_NEW_LINE);
            d.lineCnt++;
        }

    }

    /**
     *  Generates a DerivationStep and adds it to the
     *  proofWorkStmtList ArrayList on behalf of
     *  ProofUnifier.
     *  <p>
     *  If workVarList.size() > 0 then the new step
     *  is marked incomplete and given a Hyp = "?" -- no
     *  unification need be attempted. Otherwise, unification
     *  can be attempted using no Hyps. If this fails then
     *  because the step is marked "generated", the step
     *  can be updated to show Hyp "?" (this is a helpful
     *  feature for the users, going the extra mile...)
     *
     *  @param workVarList ArrayList of Work Vars in formula.
     *  @param formula Formula of new step.
     *  @param formulaParseTree ParseTree of new Formula
     *  @param derivStep insert point for new step.
     *
     *  @return DerivationStep added to the ProofWorksheet.
     */
    public DerivationStep addDerivStepForDeriveFeature(
                                ArrayList      workVarList,
                                Formula        formula,
                                ParseTree      formulaParseTree,
                                DerivationStep derivStep) {

        String           generatedStep
                                  =
           Integer.toString(
               generateNewDerivedStepNbr());

        ProofStepStmt[] generatedHyp;
        String[]        generatedHypStep;

        boolean         generatedHypFldIncomplete
                                  = false;
        boolean         generatedFormulaIsIncomplete
                                  = false;

        if (workVarList.size() > 0) {
            generatedHyp          = new ProofStepStmt[1];
            generatedHypStep      = new String[1];
            generatedHypStep[0]   = PaConstants.DEFAULT_STMT_LABEL;
            generatedHypFldIncomplete
                                  = true;
            generatedFormulaIsIncomplete
                                  = true;
        }
        else {
            generatedHyp          = new ProofStepStmt[0];
            generatedHypStep      = new String[0];
        }

        DerivationStep out        =
            new DerivationStep(
                this,
                generatedStep,
                generatedHyp,
                generatedHypStep,
                formula,
                formulaParseTree,
                generatedFormulaIsIncomplete,
                generatedHypFldIncomplete,
                true,  //generated by derive
                workVarList);

        for (int i = 0; i < proofWorkStmtList.size(); i++) {
            if (proofWorkStmtList.get(i) == derivStep) {
                proofWorkStmtList.add(i,
                                      out);
                return out;
            }
        }

        throw new IllegalArgumentException(
            PaConstants.ERRMSG_DERIVE_FEATURE_STEP_NOTFND_1);
    }

    /**
     *  Generates the next value of greatestStepNbr for use
     *  in the ProofUnifier Derive feature and returns the
     *  new value.
     *
     *  @return value of new greatestStepNbr.
     */
    public int generateNewDerivedStepNbr() {
        updateNextGreatestStepNbr(
            greatestStepNbr
            +
            PaConstants.GREATEST_STEP_NBR_INCREMENT_AMT);
        return greatestStepNbr;
    }



    /**
     *   Load the ProofWorksheet starting with the input token.
     *   <p>
     *   <ul>
     *   <li>- The first token of each statement must start
     *     in column 1. this means that
     *         ((nextToken.length -
     *           proofTextTokenizer.getCurrentColumnNbr()) == 0)
     *     must be true
     *     -->else throw ProofAsstException!
     *
     *   <li>- First statement must be a Header, so
     *     Load Header using input nextToken
     *     If error, throw ProofAsstException.
     *
     *   <li>- Loads proof text statements until footer reached
     *     or end of file, discarding any generated proof
     *     statements along the way:
     *         - reject a 2nd Header, if found;
     *
     *   <li>- After loading each statement makes sure that worksheet:
     *       - begins with a header statement;
     *       - contains a "qed" proof step;
     *       - ends with a footer statement;
     *     If missing footer, qed step or header step,
     *         throw ProofAsstException to terminate the parse
     *
     *   <li>- If no errors so far,
     *         performs remaining "relational" edits on worksheet
     *         statements.
     *
     *   <li>- During processing here and in any subroutines, a
     *     thrown ProofAsstException indicates that the parse
     *     is terminated completely.
     *   </ul>
     *   <p>
     *     Otherwise, return the nextToken value which should
     *     be the first token of the next ProofWorksheet or
     *     null if EOF. (Each routine that loads a statement
     *     will return the next token after the end of the
     *     statement's input text.)
     *
     *
     *  @param nextToken the first token to be loaded.
     *
     *  @param inputCursorPos offset plus one of Caret in
     *         Proof TextArea or -1 if not available.
     *
     *  @param stepRequestIn may be null, or StepSelector Search or
     *                     Choice request and will be loaded into
     *                     the ProofWorksheet.
     *
     *  @return      String token starting the next ProofWorksheet
     *           or null.
     */
    public String loadWorksheet(String      nextToken,
                                int         inputCursorPos,
                                StepRequest stepRequestIn)
                            throws IOException,
                                   MMIOError,
                                   ProofAsstException {

        /*  If StepSelectorDialog user chose an Assrt
            then we'll splice it into the relevant step
            (first occurrence), otherwise if user selected
            null, drop the StepRequest completely.
         */
        boolean stepSelectorChoiceRequired
                                  = false;
        stepRequest               = stepRequestIn;
        if (stepRequest            != null &&
            stepRequest.request ==
            PaConstants.STEP_REQUEST_SELECTOR_CHOICE) {
            if (stepRequest.param1  == null) {
                stepRequest           = null;
            }
            else {
                stepSelectorChoiceRequired
                                      = true;
            }
        }


        ArrayList stepsWithLocalRefs
                                  = new ArrayList();

        if (nextToken.length() == 0) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_PROOF_EMPTY_1);
        }
        if (!nextToken.equals(PaConstants.HEADER_STMT_TOKEN)) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_HDR_TOKEN_ERR_1
                + nextToken);
        }

        DelimitedTextParser stepHypRefParser =
            new DelimitedTextParser();
        stepHypRefParser.setParseDelimiter(
            PaConstants.FIELD_DELIMITER_COLON);
        stepHypRefParser.setQuoterEnabled(false);

        String  prefixField       = null;
        String  stepField         = null;
        String  hypField          = null;
        String  refField          = null;
        String  localRefField     = null;
        String  extraField        = null;



        stmtLoop: while (true) {
            if (nextToken.length() == 0) {
                // eof
                if (qedStep == null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_QED_MISSING_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_QED_MISSING_2);
                }
                if (footerStmt == null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_FOOTER_MISSING_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_FOOTER_MISSING_2);
                }
                break;
            }

            if (nextToken.length() !=
                proofTextTokenizer.getCurrentColumnNbr()) {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_COL1_ERROR_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_COL1_ERROR_2
                    + (int)proofTextTokenizer.getCurrentLineNbr()
                    + PaConstants.ERRMSG_COL1_ERROR_3
                    + nextToken);
            }

            if (nextToken.equals(PaConstants.HEADER_STMT_TOKEN)) {
                if (headerStmt != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_MULT_HDR_ERROR_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_MULT_HDR_ERROR_2);
                }
                headerStmt        = new HeaderStmt(this);
                nextToken         = headerStmt.load(nextToken);
                proofWorkStmtList.add(headerStmt);

                setInputCursorStmtIfHere(headerStmt,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
                continue stmtLoop;
            }

            if (nextToken.equals(PaConstants.FOOTER_STMT_TOKEN)) {
                if (qedStep == null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_QED_MISSING2_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_QED_MISSING2_2);
                }
                FooterStmt footerStmt
                                  = new FooterStmt(this);
                nextToken         = footerStmt.load(nextToken);
                proofWorkStmtList.add(footerStmt);

                setInputCursorStmtIfHere(footerStmt,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
                break;
            }

            if (nextToken.equals(
                    PaConstants.GENERATED_PROOF_STMT_TOKEN)) {
                GeneratedProofStmt x
                                  = new GeneratedProofStmt(this);
                nextToken         = x.load(nextToken);

                setInputCursorPosIfHere(inputCursorPos,
                                        nextToken,
                                        proofTextTokenizer);
                continue stmtLoop;
            }

            if (nextToken.equals(
                    PaConstants.DISTINCT_VARIABLES_STMT_TOKEN)) {
                DistinctVariablesStmt x
                                  = new DistinctVariablesStmt(this);
                nextToken         = x.load(nextToken);
                proofWorkStmtList.add(x);
                ++dvStmtCnt;

                setInputCursorStmtIfHere(x,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
                continue stmtLoop;
            }

            int origStepHypRefLength  = nextToken.length();

            prefixField           =
                (nextToken.substring(0,1)).toLowerCase();

            if (prefixField.equals(
                    PaConstants.COMMENT_STMT_TOKEN_PREFIX)) {
                CommentStmt x     = new CommentStmt(this);
                nextToken         = x.load(nextToken);
                proofWorkStmtList.add(x);

                setInputCursorStmtIfHere(x,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
                continue stmtLoop;
            }

            // now work on ProofSteps, starting with step/hyp/ref
            // fields.
            if (prefixField.equals(PaConstants.HYP_STEP_PREFIX)) {
                stepHypRefParser.setParseString(
                    nextToken.substring(1));
            }
            else {
                prefixField       = null;
                stepHypRefParser.setParseString(
                    nextToken);
            }

            extraField            = null;
            refField              = null;
            localRefField         = null;
            stepField             = stepHypRefParser.nextField();
            if (stepField != null) {
                hypField          = stepHypRefParser.nextField();
                if (hypField != null) {
                    if (hypField.length() == 0) {
                        hypField  = null;
                    }
                    refField      = stepHypRefParser.nextField();
                    if (refField != null) {
                        if (refField.length() == 0) {
                            refField
                                  = null;
                        }
                        else {
                            if (refField.charAt(0) ==
                               PaConstants.LOCAL_REF_ESCAPE_CHAR) {
                                if (refField.length() > 1) {
                                    localRefField
                                  = refField.substring(1);
                                }
                                else {
                                    localRefField
                                  = "";
                                }
                                refField
                                  = null;
                            }
                        }
                        extraField
                                  = stepHypRefParser.nextField();
                    }
                }
                stepField             =
                    validateStepField(prefixField, //throws exception
                                      stepField);
            }
            else {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_SHR_BAD_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_SHR_BAD_2
                    + (int)proofTextTokenizer.getCurrentLineNbr()
                    + PaConstants.ERRMSG_SHR_BAD_3
                    + nextToken);
            }

            if (extraField != null) {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_SHR_BAD2_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_SHR_BAD2_2
                    + stepField
                    + PaConstants.ERRMSG_SHR_BAD2_3
                    + (int)proofTextTokenizer.getCurrentLineNbr()
                    + PaConstants.ERRMSG_SHR_BAD2_4
                    + nextToken);
            }

            int lineStartCharNbr  =
                (int)proofTextTokenizer.getCurrentCharNbr();

            if (prefixField != null) {
                if (hypField != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_HYP_HAS_HYP_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_HYP_HAS_HYP_2
                        + stepField
                        + PaConstants.ERRMSG_HYP_HAS_HYP_3);
                }

                if (localRefField != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_HYP_HAS_LOCAL_REF_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_HYP_HAS_LOCAL_REF_2
                        + stepField
                        + PaConstants.ERRMSG_HYP_HAS_LOCAL_REF_3);
                }

                if (qedStep != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_QED_NOT_END_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_QED_NOT_END_2
                        + stepField
                        + PaConstants.ERRMSG_QED_NOT_END_3);
                }
                if (stepSelectorChoiceRequired &&
                    stepField.equals(stepRequest.step)) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_HYP_HAS_SELECTOR_CHOICE_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_HYP_HAS_SELECTOR_CHOICE_2
                        + stepField
                        + PaConstants.ERRMSG_HYP_HAS_SELECTOR_CHOICE_3
                        );
                }
                HypothesisStep x  = new HypothesisStep(this);
                nextToken         =
                    x.loadHypothesisStep(origStepHypRefLength,
                                         lineStartCharNbr,
                                         stepField,
                                         refField);
                proofWorkStmtList.add(x);
                ++hypStepCnt;

                setInputCursorStmtIfHere(x,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
                continue stmtLoop;
            }

            if (stepField.equals(PaConstants.QED_STEP_NBR)) {
                if (qedStep != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_MULT_QED_ERROR_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_MULT_QED_ERROR_2
                        + stepField
                        + PaConstants.ERRMSG_MULT_QED_ERROR_3);
                }

                if (localRefField != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_QED_HAS_LOCAL_REF_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_QED_HAS_LOCAL_REF_2
                        + stepField
                        + PaConstants.ERRMSG_QED_HAS_LOCAL_REF_3);
                }

                if (stepSelectorChoiceRequired &&
                    stepField.equals(stepRequest.step)) {
                    refField      =
                        ((Assrt)(stepRequest.param1)).getLabel();
                    stepSelectorChoiceRequired
                                  = false;
                    stepRequest   = null; //done, so null it.
                }

                qedStep           = new DerivationStep(this);
                nextToken         =
                    qedStep.loadDerivationStep(origStepHypRefLength,
                                               lineStartCharNbr,
                                               stepField,
                                               hypField,
                                               refField);
                proofWorkStmtList.add(qedStep);

                setInputCursorStmtIfHere(qedStep,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer);
            }
            else {
                if (qedStep != null) {
                    triggerLoadStructureException(
                        PaConstants.ERRMSG_QED_NOT_END2_1
                        + getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_QED_NOT_END2_2
                        + stepField
                        + PaConstants.ERRMSG_QED_NOT_END2_3);
                }

                DerivationStep x  = new DerivationStep(this);

                if (localRefField != null) {

                    if (stepSelectorChoiceRequired &&
                        stepField.equals(stepRequest.step)) {
                        triggerLoadStructureException(
                            PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_CHOICE_1
                            + getErrorLabelIfPossible()
                            + PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_CHOICE_2
                            + stepField
                            + PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_CHOICE_3
                            );
                    }

                    nextToken     =
                        x.loadLocalRefDerivationStep(
                                         origStepHypRefLength,
                                         lineStartCharNbr,
                                         stepField,
                                         hypField,
                                         localRefField);
                    stepsWithLocalRefs.add(x);
                }
                else {

                    if (stepSelectorChoiceRequired &&
                        stepField.equals(stepRequest.step)) {
                        refField      =
                            ((Assrt)(stepRequest.param1)).getLabel();
                        stepSelectorChoiceRequired
                                      = false;
                        stepRequest   = null; //done, so null it.
                    }

                    nextToken     =
                        x.loadDerivationStep(
                                         origStepHypRefLength,
                                         lineStartCharNbr,
                                         stepField,
                                         hypField,
                                         refField);
                }
                proofWorkStmtList.add(x);

                if (setInputCursorStmtIfHere(x,
                                         inputCursorPos,
                                         nextToken,
                                         proofTextTokenizer)) {
                    if (localRefField != null &&
                        stepRequest   != null &&
                        stepRequest.request == PaConstants.
                            STEP_REQUEST_SELECTOR_SEARCH) {
                        triggerLoadStructureException(
                            PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_SEARCH_1
                            + getErrorLabelIfPossible()
                            + PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_SEARCH_2
                            + stepField
                            + PaConstants.
                                ERRMSG_LOCAL_REF_HAS_SELECTOR_SEARCH_3
                            );
                    }
                }
            }
        }
        // end of stmtLoop! oy.


        /*
         *  =====================================================
         *  <<<<<This is the loadWorksheet "finale" section.>>>>>
         *  =====================================================
         */

        if (stepRequest != null     &&
            stepRequest.request == PaConstants.
                                    STEP_REQUEST_SELECTOR_SEARCH) {
            if (!proofInputCursor.cursorIsSet          ||
                proofInputCursor.proofWorkStmt == null ||
                !proofInputCursor.proofWorkStmt.isDerivationStep()) {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_SELECTOR_SEARCH_STEP_NOTFND_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_SELECTOR_SEARCH_STEP_NOTFND_2
                    );
            }
            stepRequest.step      =
                ((DerivationStep)
                    proofInputCursor.
                        proofWorkStmt).
                            step;
            stepRequest.param1    = proofInputCursor.proofWorkStmt;
        }

        if (stepSelectorChoiceRequired) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_SELECTOR_CHOICE_STEP_NOTFND_1
                + getErrorLabelIfPossible()
                + PaConstants.ERRMSG_SELECTOR_CHOICE_STEP_NOTFND_2);
        }

        if (stepsWithLocalRefs.size() > 0) {
            makeLocalRefRevisionsToWorksheet(stepsWithLocalRefs);
        }

        loadWorksheetStmtArrays();

        if (dvStmtCnt > 0) {
            updateComboFrameDjVars();
        }

        /**
         *   Verify correct number of hyp proof steps
         *   entered for theorem, regard it as a
         *   structural defect if the count is wrong!!!
         */
        if (!isNewTheorem()
            &&
            theorem.getLogHypArrayLength() != hypStepCnt) {

            throw new ProofAsstException(
            	new SourcePosition(null, 
                1,                                            //line
                (PaConstants.PROOF_TEXT_HEADER_1.length() + 1), //col
                -1,                                           //char
                -1),
                PaConstants.ERRMSG_THRM_NBR_HYPS_ERROR_1
                + getErrorLabelIfPossible()
                + PaConstants.ERRMSG_THRM_NBR_HYPS_ERROR_2
                + hypStepCnt
                + PaConstants.ERRMSG_THRM_NBR_HYPS_ERROR_3
                + theorem.getLogHypArrayLength()
                + PaConstants.ERRMSG_THRM_NBR_HYPS_ERROR_4);
        }

        /**
         *  Compute level numbers for the proof steps.
         */
        loadWorksheetProofLevelNumbers();

        return nextToken;

    }


    private boolean setInputCursorStmtIfHere(
                        ProofWorkStmt      proofWorkStmt,
                        int                inputCursorPos,
                        String             nextToken,
                        Tokenizer          proofTextTokenizer) {
        if (inputCursorPos == -1
            ||
            proofInputCursor.cursorIsSet
            ||
            inputCursorPos >
            (proofTextTokenizer.getCurrentCharNbr()
             - nextToken.length())
            ) {
            return false;
        }
        else {
            proofInputCursor.
                setCursorAtProofWorkStmt(
                    proofWorkStmt,
                    PaConstants.FIELD_ID_REF);
            return true;
        }
    }

    private void setInputCursorPosIfHere(
                        int                inputCursorPos,
                        String             nextToken,
                        Tokenizer          proofTextTokenizer) {
        if (inputCursorPos == -1
            ||
            proofInputCursor.cursorIsSet
            ||
            inputCursorPos >
            (proofTextTokenizer.getCurrentCharNbr()
             - nextToken.length())
            ) {
        }
        else {
            proofInputCursor.
                setCursorAtCaret(inputCursorPos,
                                 -1,
                                 -1);
        }
    }


    /*
     *  Modify each Hyp "pointers" to a step with a localRef
     *  to the step the localRef is pointing to. And then
     *  delete each localRef step from the ProofWorksheet.
     */
    private void makeLocalRefRevisionsToWorksheet(
                            ArrayList stepsWithLocalRefs) {
        ProofWorkStmt  x;
        DerivationStep dI;
        DerivationStep dJ;
        boolean        stepUpdated;

        loopI: for (int i = proofWorkStmtList.size() - 1;
                    i > 0;
                    i--) {
            x                     =
                (ProofWorkStmt)proofWorkStmtList.get(i);

            if (!x.isDerivationStep()) {
                continue loopI;
            }
            dI                    = (DerivationStep)x;

            if (dI.hyp.length == 0 ||
                dI.localRef != null) {
                continue loopI;
            }

            stepUpdated           = false;
            for (int j = stepsWithLocalRefs.size() - 1; j >= 0; j--) {
                dJ                =
                    (DerivationStep)stepsWithLocalRefs.get(j);

                loopK: for (int k = dI.hyp.length - 1; k >= 0; k--) {
                    if (dI.hyp[k] == dJ) {
                        stepUpdated
                                  = true;
                        dI.hypStep[k]
                                  = dJ.localRef.step;
                        dI.hyp[k] = dJ.localRef;
                    }
                }
            }
            if (stepUpdated) {

                dI.reloadStepHypRefInStmtText();
                dI.reloadLogHypKeysAndMaxDepth();
                dI.resetSortedHypArray();
            }
        }

        for (int j = stepsWithLocalRefs.size() - 1; j >= 0; j--) {
            removeFromProofWorkStmtList(
                (ProofWorkStmt)stepsWithLocalRefs.
                    get(j));
        }
    }

    // fix the cursor: localRef step going byebye!
    private void removeFromProofWorkStmtList(ProofWorkStmt x) {
        if (proofInputCursor.cursorIsSet &&
            x == proofInputCursor.proofWorkStmt) {
            proofInputCursor.proofWorkStmt
                              = null;
        }
        proofWorkStmtList.remove(x);
    }

    /**
     *  Initial load of proof worksheet step level numbers.
     *
     *  Note: this assumes that each ProofStepStmt.proofLevel
     *        is pre-initialized to zero.
     *
     */
    public void loadWorksheetProofLevelNumbers() {

        ProofWorkStmt s;

        for (int i = proofWorkStmtList.size() - 1; i > 0; i--) {
            s                     =
                (ProofWorkStmt)proofWorkStmtList.get(i);
            if (s == qedStep) {
                int stepIndex     = i;
                while (true) {
                    if (s.isDerivationStep()) {
                        ((DerivationStep)s).
                            loadDerivationStepHypLevels();
                    }
                    if (--stepIndex > 0) {
                        s         =
                            (ProofWorkStmt)proofWorkStmtList.
                                get(stepIndex);
                    }
                    else {
                        return;
                    }
                }
            }
        }
    }

    /**
     *  Obtain output messages.
     *  <p>
     *  Note: this is a key function used by ProofAssistantEditor.
     *  <p>
     *  Note: with word wrap 'on', newlines are ignored in
     *  JTextArea, so we insert spacer lines.
     *
     *  @return      MessageHandler object having accumulated all messages.
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     *  Obtain output message text from ProofWorksheet.
     *  <p>
     *  Note: this is a key function used by ProofAsstGUI.
     *  <p>
     *  Note: with word wrap 'on', newlines are ignored in
     *  JTextArea, so we insert spacer lines.
     *
     *  @return      Proof Error Message Text area as String.
     */
    public String getOutputMessageText() {
        return ProofWorksheet.getOutputMessageText(messageHandler);
    }

    /**
     *  Obtain output message text from ProofWorksheet.
     *  <p>
     *  Note: this is a key function used by ProofAsstGUI.
     *  <p>
     *  Note: with word wrap 'on', newlines are ignored in
     *  JTextArea, so we insert spacer lines.
     *
     *  @param  messages Messages object.
     *  @return      Proof Error Message Text area as String.
     */
    public static String getOutputMessageText(MessageHandler messages) {
    	return messages.getOutputMessageText();
    }

   	/**
     *  Obtain output proof text from ProofWorksheet.
     *  <p>
     *  Note: this is a key function used by ProofAssistantEditor.
     *  <p>
     *  Note: with word wrap 'on', newlines are ignored in
     *  JTextArea, so we insert spacer lines.
     *
     *  @return      Proof Text area as String.
     */
    public String getOutputProofText() {
        if (hasStructuralErrors()) {
            return null;
        }

        StringBuffer sb           =
            new StringBuffer(proofWorkStmtList.size() * 80);
        Iterator iterator         = proofWorkStmtList.iterator();
        ProofWorkStmt x;
        while (iterator.hasNext()) {
            x = (ProofWorkStmt)iterator.next();
            x.appendToProofText(sb);
        }
        return sb.toString();
    }

    /**
     *  Insert a GeneratedProofStmt into the ProofWorksheet
     *  <p>
     *  Note: this is used by ProofAsst after successful unification.
     *
     *  @param rpnProof Proof Stmt array.
     */
    public void addGeneratedProofStmt(Stmt[] rpnProof) {
        GeneratedProofStmt x =
            new GeneratedProofStmt(this,
                                   rpnProof);
        //add just before footer
        proofWorkStmtList.add((proofWorkStmtList.size() - 1),
                               x);
        generatedProofStmt        = x;
    }

    /**
     *  Returns the GeneratedProofStmt from the ProofWorksheet
     *  <p>
     *  Note: returns null if unification unsuccessful or not
     *        yet performed.
     *
     *  @return generatedProofStmt or null if not unified
     *          successfully.
     */
    public GeneratedProofStmt getGeneratedProofStmt() {
        return generatedProofStmt;
    }

    /**
     *  Returns the DistinctVariablesStmt array from the
     *  ProofWorksheet.
     *  <p>
     *  Note: may return null.
     *
     *  @return DistinctVariablesStmt array or null if there
     *          are none.
     */
    public DistinctVariablesStmt[] getDvStmtArray() {
        return dvStmtArray;
    }

    /**
     *  Generate DistinctVariablesStmt set for soft DjVars
     *  errors.
     *  <p>
     *  Input is ProofWorksheet.proofSoftDjVarsErrorList and
     *  ProofAsstPreferences, which determines whether
     *  a full replacement set of DistinctVariableStmt's
     *  must be created, or only the differences to what
     *  is already on the theorem in the .mm database.
     *  <p>
     *  Note that the ProofWorksheet's dvStmtArray,
     *  dvStmtCnt and comboFrame are updated -- even
     *  though at this time there is no known use of
     *  these data items after this point in the
     *  processing (which is just prior to displaying
     *  the fully-unified proof on the GUI screen). However,
     *  there may be a use for the updated data items
     *  in testing, and in any case it is best no to
     *  leave loose ends dangling.
     *  <p>
     */
    public void generateAndAddDjVarsStmts() {

        DjVars[] diffDvArray      =
            DjVars.sortAndCombineDvListOfLists(
                proofSoftDjVarsErrorList);

        DjVars[] replDvArray;
        if (proofAsstPreferences.
                getDjVarsSoftErrorsGenerateNew()) {
            // don't use existing $d's if GenerateNew
            replDvArray           = diffDvArray;
        }
        else {
            replDvArray           =
                DjVars.sortAndCombineDvArrays(
                    comboFrame.djVarsArray,
                    diffDvArray);
        }

        ArrayList dvGroups1;
        if (proofAsstPreferences.
                getDjVarsSoftErrorsGenerateDiffs()) {

            dvGroups1             =
                MandFrame.consolidateDvGroups(
                    diffDvArray);

        }
        else {
            dvGroups1             =
                MandFrame.consolidateDvGroups(
                    replDvArray);
        }

        ArrayList dvGroups        =
            DistinctVariablesStmt.
                eliminateDvGroupsAlreadyPresent(
                    dvStmtArray,
                    dvGroups1);

        int newDvStmtCnt          = dvStmtCnt +
                                    dvGroups.size();

        DistinctVariablesStmt[] newDvStmtArray
                                  =
            new DistinctVariablesStmt[newDvStmtCnt];

        int loadIndex             = 0;
        while (loadIndex < dvStmtArray.length) {
            newDvStmtArray[loadIndex]
                              = dvStmtArray[loadIndex];
            ++loadIndex;
        } //ok, now create the rest...

        DistinctVariablesStmt x;
        Iterator iterator         = dvGroups.iterator();
        while (iterator.hasNext()) {
            x                     =
                new DistinctVariablesStmt(
                    this,
                    (ArrayList)iterator.next());

            //add just before footer
            proofWorkStmtList.add((proofWorkStmtList.size() - 1),
                                  x);

            newDvStmtArray[loadIndex++]
                                  = x;
        }

        dvStmtCnt                 = newDvStmtCnt;
        dvStmtArray               = newDvStmtArray;
        comboFrame.djVarsArray    = replDvArray;
    }


    public Cnst getProvableLogicStmtTyp() {
        return (grammar.getProvableLogicStmtTypArray())[0];
    }

    public ProofWorkStmt findFirstMatchingRefOrStep(String localRef) {
        Iterator iterator         = proofWorkStmtList.iterator();
        ProofWorkStmt x;
        while (iterator.hasNext()) {
            x = (ProofWorkStmt)iterator.next();
            if (x.hasMatchingRefLabel(localRef) ||
                x.hasMatchingStepNbr(localRef)) {
                return x;
            }
        }
        return null;
    }

    public ProofWorkStmt findMatchingStepNbr(String newStepNbr) {
        Iterator iterator         = proofWorkStmtList.iterator();
        ProofWorkStmt x;
        while (iterator.hasNext()) {
            x = (ProofWorkStmt)iterator.next();
            if (x.hasMatchingStepNbr(newStepNbr)) {
                return x;
            }
        }
        return null;
    }


    /**
     *  Load the combo frame and var array object
     *  for use throughout the ProofWorksheet.
     *
     *  For new theorems uses global ScopeDef from
     *  logical system, pruned by eliminating MObj's
     *  with seq >= maxSeq.
     *
     *  For existing theorems, merges MandFrame and OptFrame.
     *
     *  comboVarMap always built using MandFrame
     *  for simplicity and reliability even though
     *  it would, theoretically be possible to derive
     *  it for new theorems using the ScopeDef
     *  (curiously, MandFrame/OptFrame do not
     *  directly store Var's -- probably because
     *  each VarHyp *has* an associated Var and
     *  it would be redundant.)
     */
    public void loadComboFrameAndVarMap() {
        if (isNewTheorem()) {
            comboFrame            =
                new MandFrame(
                        (ScopeDef)
                            (logicalSystem.getScopeDefList(
                                )).get(0),
                        getMaxSeq());
        }
        else {
            comboFrame            =
                new MandFrame(theorem.getMandFrame(),
                              theorem.getOptFrame());

        }
        comboVarMap               = comboFrame.getVarMap();
    }

    private void updateComboFrameDjVars() {
        Var[][] dvGroupArray  = new Var[dvStmtCnt][];
        for (int i = 0; i < dvStmtCnt; i++) {
            dvGroupArray[i]   = dvStmtArray[i].getDv();
        }
        comboFrame.addDjVarGroups(dvGroupArray);
    }


    private String validateStepField(String prefixField,
                                     String stepField)
                                         throws ProofAsstException {
        if (stepField.equals("") ||
            stepField.equals(PaConstants.DEFAULT_STMT_LABEL)) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_STEP_NBR_MISSING_1
                + getErrorLabelIfPossible()
                + PaConstants.ERRMSG_STEP_NBR_MISSING_2);
        }

        String outputStep         = stepField.toLowerCase();
        if (outputStep.equals(PaConstants.QED_STEP_NBR)) {
            if (prefixField != null) {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_QED_HYP_STEP_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_QED_HYP_STEP_2
                    + outputStep
                    + PaConstants.ERRMSG_QED_HYP_STEP_3);
            }
            return outputStep;
        }

        try {
            Integer stepInteger   = new Integer(stepField);
            if (stepInteger <= 0) {
                triggerLoadStructureException(
                    PaConstants.ERRMSG_STEP_LE_0_1
                    + getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_STEP_LE_0_2
                    + stepField
                    + PaConstants.ERRMSG_STEP_LE_0_3);
            }
            updateNextGreatestStepNbr(stepInteger); //derive feature
            outputStep                = stepInteger.toString();
        }
        catch (NumberFormatException e) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_STEP_NOT_INT_1
                + getErrorLabelIfPossible()
                + PaConstants.ERRMSG_STEP_NOT_INT_2
                + stepField
                + PaConstants.ERRMSG_STEP_NOT_INT_3);
        }

        ProofWorkStmt x           =
            findMatchingStepNbr(outputStep);
        if (x != null) {
            triggerLoadStructureException(
                PaConstants.ERRMSG_STEP_NBR_DUP_1
                + getErrorLabelIfPossible()
                + PaConstants.ERRMSG_STEP_NBR_DUP_2
                + outputStep
                + PaConstants.ERRMSG_STEP_NBR_DUP_3);
        }
        return outputStep;
    }

    public void triggerLoadStructureException(String errorMessage)
                        throws ProofAsstException {
        setStructuralErrors(true);
        throw new ProofAsstException(
            proofTextTokenizer.getCurrentPosition(),
            errorMessage
                 + PaConstants.ERRMSG_READER_POSITION_LITERAL);
    }

    public void triggerLoadStructureException(String errorMessage,
                                               int    errorFldChars)
                        throws ProofAsstException {
        setStructuralErrors(true);
        SourcePosition position = proofTextTokenizer.getCurrentPosition();
        position.charStartNbr = position.charEndNbr - errorFldChars + 1;
        throw new ProofAsstException(position,
            errorMessage
                 + PaConstants.ERRMSG_READER_POSITION_LITERAL);
    }

    private void buildHeader(String newTheoremLabel) {
        String theoremLabel;
        if (newTheoremLabel != null) {
            theoremLabel = newTheoremLabel.trim();
            if (theoremLabel.equals("")) {
                theoremLabel = PaConstants.DEFAULT_STMT_LABEL;
            }
        }
        else {
            theoremLabel = PaConstants.DEFAULT_STMT_LABEL;
        }

        headerStmt                =
            new HeaderStmt(this,
                           theoremLabel,
                           PaConstants.DEFAULT_STMT_LABEL);

        proofWorkStmtList.add(headerStmt);
    }


    private void buildEmptyTheoremProofBody(Theorem t) {

        String[]       dummyHypStep
                                  = { "" };

        DerivationStep derivationStep;
        HypothesisStep hypothesisStep;

        LogHyp[]       logHypArray = t.getLogHypArray();

        int            stepNbr     = 0;

        for (int i = 0; i < logHypArray.length; i++) {

            hypothesisStep        =
                new HypothesisStep(
                        this,
                        String.valueOf(++stepNbr),   //step
                        logHypArray[i].getLabel(),   //refLabel
                        logHypArray[i].getFormula(), //formula
                        logHypArray[i].getExprParseTree(),
                        false,                       //set caret
                        0);                          //proofLevel 0

            proofWorkStmtList.add(hypothesisStep);
        }

        qedStep                   =
            new DerivationStep(
                    this,
                    PaConstants.QED_STEP_NBR,      //step

                    dummyHypStep,                  //hypStep
                    null,                          //refLabel
                    t.getFormula(),                //formula
                    t.getExprParseTree(),          //parseTree
                    true,                          //set caret
                    0);                            //proofLevel 0

        proofWorkStmtList.add(qedStep);

        hypStepCnt                = logHypArray.length;

      /*
       *  No need to loadWorksheetStmtArrays here because this
       *  worksheet is headed straight for output on the GUI,
       *  not Unification.
       */
      //loadWorksheetStmtArrays();

    }

    private void buildTheoremDescription(Theorem theorem) {
        String description        = theorem.getDescription();
        if (description != null) {
            proofWorkStmtList.add(
                new CommentStmt(this,
                                description,
                                true)); // true = doublespace after desc
        }
    }

    private void buildExportTheoremProofBody(
                                Theorem   theorem,
                                ArrayList proofDerivationStepList,
                                boolean   deriveFormulas) {

        DerivationStep derivationStep
                                  = null;
        HypothesisStep hypothesisStep
                                  = null;

//      derivStepCnt              = 0;
        hypStepCnt                = 0;

        ProofDerivationStepEntry e;

        int            stepCnt    = proofDerivationStepList.size();
        for (int i = 0; i < stepCnt; i++) {
            e                     =
                (ProofDerivationStepEntry)
                    proofDerivationStepList.get(i);

            if (deriveFormulas
                &&
                e.step.compareToIgnoreCase(
                    PaConstants.QED_STEP_NBR) != 0
                && !e.isHyp) {
                derivationStep    =
                    new DerivationStep(this,
                                       e.step,
                                       e.hypStep,
                                       e.refLabel,
                                       null, //formula,
                                       null, //formulaParseTree,
                                       false, //no caret
                                       e.proofLevel);
                proofWorkStmtList.add(derivationStep);
                continue;
            }

            if (e.formulaParseTree == null) {
                e.formulaParseTree
                                  =
                    grammar.
                        parseFormulaWithoutSafetyNet(
                            e.formula,
                            comboFrame.hypArray,
                            getMaxSeq());
            }

            if (e.isHyp) {
                ++hypStepCnt;
                hypothesisStep    =
                    new HypothesisStep(this,
                                       e.step,
                                       e.refLabel,
                                       e.formula,
                                       e.formulaParseTree,
                                       false,
                                       e.proofLevel);
                proofWorkStmtList.add(hypothesisStep);
            }
            else {
                derivationStep    =
                    new DerivationStep(this,
                                       e.step,
                                       e.hypStep,
                                       e.refLabel,
                                       e.formula,
                                       e.formulaParseTree,
                                       false, //no caret
                                       e.proofLevel);
                proofWorkStmtList.add(derivationStep);
            }
        }

        qedStep                   = derivationStep; //final step...

      /*
       *  No need to loadWorksheetStmtArrays here because this
       *  worksheet is headed straight for output on the GUI,
       *  not Unification.
       */
      //loadWorksheetStmtArrays();
    }

    private void buildDummyProofBody() {

        Formula        dummyFormula
                                  =
            Formula.constructTempDummyFormula(
                getProvableLogicStmtTyp(),
                PaConstants.DEFAULT_STMT_LABEL);

        String[] dummyHypStep     = { "?" };

        HypothesisStep hs         =
            new HypothesisStep(this,
                               "1",             //step
                               null,            //refLabel
                               dummyFormula,    //formula,
                               null,            //parseTree
                               true,            //set caret
                               0);              //proofLevel 0

        proofWorkStmtList.add(hs);

        DerivationStep ds         =
            new DerivationStep(this,
                               "2",             //step
                               dummyHypStep,    //hypStep
                               null,            //refLabel
                               dummyFormula,    //formula,
                               null,            //parseTree
                               false,           //set caret
                               0);              //proofLevel 0

        proofWorkStmtList.add(ds);

        qedStep                   =
            new DerivationStep(this,
                              PaConstants.QED_STEP_NBR, //step
                              dummyHypStep,     //hypStep
                              null,             //refLabel
                              dummyFormula,     //formula,
                              null,             //parseTree
                              false,            //set caret
                              0);               //proofLevel 0

        proofWorkStmtList.add(qedStep);

        hypStepCnt                = 1;

      /*
       *  No need to loadWorksheetStmtArrays here because this
       *  worksheet is headed straight for output on the GUI,
       *  not Unification.
       */
      //loadWorksheetStmtArrays();

    }

    private void buildFooter() {

        footerStmt                =
            new FooterStmt(this);
        footerStmt.loadDefault();

        proofWorkStmtList.add(footerStmt);
    }

    private void loadWorksheetStmtArrays() {
        // load proof steps and $d's into convenient arrays
        dvStmtArray               =
            new DistinctVariablesStmt[dvStmtCnt];

        Iterator iterator         = proofWorkStmtList.iterator();
        ProofWorkStmt x;
        int           dv          = 0;
        while (iterator.hasNext()) {
            x = (ProofWorkStmt)iterator.next();
            if (x instanceof DistinctVariablesStmt) {
                dvStmtArray[dv++]
                          = (DistinctVariablesStmt)x;
            }
        }
    }
}

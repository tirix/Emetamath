//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  TheoremStmtGroup.java  0.01 08/01/2008
 *
 *  Version 0.01:
 *      --> new.
 */

package mmj.tl;
import java.util.*;
import java.io.*;
import mmj.lang.*;
import mmj.mmio.*;
import mmj.pa.PaConstants;

/**
 *  TheoremStmtGroup represents the contents of a MMTTheoremFile
 *  as well as state variables pertaining to work performed
 *  loading the theorem into the LogicalSystem.
 *  <p>
 *  Note: TheoremStmtGroup refers to the fact that a MMTTheoremFile
 *        consists of a group of Metamath .mm statements.
 */
public class TheoremStmtGroup {

    private     MMTTheoremFile  mmtTheoremFile;

    private     String          theoremLabel;

    /* initially loaded data from input mmtTheoremFile */
    private     SrcStmt         beginScopeSrcStmt;
    private     ArrayList       dvSrcStmtList;
    private     ArrayList       logHypSrcStmtList;
    private     SrcStmt         theoremSrcStmt;
    private     SrcStmt         endScopeSrcStmt;


    /* derived or computed items follow: */

    private     MObj            maxExistingMObjRef;

    private     int             insertSectionNbr; //for BookManager.

    private     boolean         isTheoremNew;

    private     LogHyp[]        logHypArray;
    private     Theorem         theorem;


    /* store previous values for backout */
    private     Stmt[]          oldProof;
    private     DjVars[]        oldDjVarsArray;
    private     DjVars[]        oldOptDjVarsArray;



    /* derived based on relational edit/analysis
       of the MMTTheoremSet */
    private     boolean         isProofIncomplete;

    private     boolean         mustAppend;

    private     LinkedList      theoremStmtGroupUsedList;
    private     LinkedList      usedByTheoremStmtGroupList;



    /* derived based on updating access of LogicalSystem */
    private     boolean         wasTheoremUpdated;

    private     boolean[]       wasLogHypInserted;
    private     boolean         wasTheoremInserted;

    private     boolean[]       wasLogHypAppended;
    private     boolean         wasTheoremAppended;

    private     int[]           assignedLogHypSeq;
    private     int             assignedTheoremSeq;

    /**
     *  Constructor for TheoremStmtGroup.
     *  <p>
     *  The constructor loads the input file into the
     *  TheoremStmtGroup and performs data validation of
     *  the input data.
     *  <p>
     *  @param mmtTheoremFile MMTTheoremFile to be read.
     *  @param logicalSystem LogicalSystem object.
     *  @param messages Messages object.
     *  @param tlPreferences TlPreferences object.
     *  @throws TheoremLoaderException is thrown if there are
     *          data errors in the input MMTTheoremFile.
     */
    public TheoremStmtGroup(MMTTheoremFile mmtTheoremFile,
                            LogicalSystem  logicalSystem,
                            MessageHandler       messages,
                            TlPreferences  tlPreferences)
                                throws TheoremLoaderException {

        this.mmtTheoremFile       = mmtTheoremFile;

        dvSrcStmtList             = new ArrayList(
            TlConstants.DEFAULT_DV_SRC_STMT_LIST_SIZE);

        logHypSrcStmtList         = new ArrayList(
            TlConstants.DEFAULT_LOG_HYP_SRC_STMT_LIST_SIZE);

        Statementizer statementizer
                                  =
            mmtTheoremFile.constructStatementizer();

        loadParsedInputIntoStmtGroup(statementizer);

        theoremLabel              = theoremSrcStmt.label;

        int n                     = logHypSrcStmtList.size();
        logHypArray               = new LogHyp[n];
        wasLogHypInserted         = new boolean[n];
        wasLogHypAppended         = new boolean[n];
        assignedLogHypSeq         = new int[n];

        theoremStmtGroupUsedList  = new LinkedList();
        usedByTheoremStmtGroupList
                                  = new LinkedList();

        validateStmtGroupData(logicalSystem,
                              messages,
                              tlPreferences);

    }

    /**
     *  Initializes the mustAppend flag for the TheoremStmtGroup.
     *  <p>
     *  The initial (default) setting of mustAppend is false
     *  unless the theorem is new and has an incomplete proof.
     */
    public void initializeMustAppend() {

        mustAppend                = false;

        if (getIsTheoremNew()) {
            if (isProofIncomplete) {
                mustAppend            = true;
            }
        }
    }

    /**
     *  Validates the labels used in the TheoremStmtGroup
     *  proof.
     *  <p>
     *
     *  During processing the highest sequence number
     *  referred to is updated,
     *  <p>
     *  Also, isProofIncomplete is set to true if a "?" is
     *  found in the proof.
     *  <p>
     *  And, two key lists about which theorems are
     *  used are updated: <code>theoremStmtGroupUsedList</code>
     *  and <code>usedByTheoremStmtGroupList</code>. These
     *  lists are the basis for determining the order in which
     *  theorems are loaded into the Logical System.
     *  <p>
     *  @param logicalSystem LogicalSystem object.
     *  @param theoremStmtGroupTbl the MMTTheoremSet Map
     *            containing the MMT Theorems in the set.
     *  @throws TheoremLoaderException is thrown if there are
     *          data errors in the input MMTTheoremFile.
     */
    public void validateTheoremSrcStmtProofLabels(
                            LogicalSystem logicalSystem,
                            Map           theoremStmtGroupTbl)
                                throws TheoremLoaderException {

        Map              stmtTbl  = logicalSystem.getStmtTbl();

        String           stepLabel;
        Stmt             ref;
        TheoremStmtGroup g;

        Iterator         i        =
            theoremSrcStmt.proofList.iterator();
        while (i.hasNext()) {
            stepLabel             = (String)i.next();

            if (stepLabel.compareTo(
                    PaConstants.DEFAULT_STMT_LABEL) == 0) {

                isProofIncomplete = true;
                continue;
            }

            g                     = null;
            ref                   = (Stmt)stmtTbl.get(stepLabel);
            if (ref == null ||
                ref instanceof Theorem) {
                g                 = (TheoremStmtGroup)
                                        theoremStmtGroupTbl.
                                            get(stepLabel);
                if (g != null) {
                    accumInList(theoremStmtGroupUsedList,
                                g);
                    g.accumInList(g.usedByTheoremStmtGroupList,
                                  this);
                }
            }

            if (ref == null) {
                if (g == null &&
                    !isLabelInLogHypList(stepLabel)) {
                    throw new TheoremLoaderException(
                        TlConstants.ERRMSG_PROOF_LABEL_ERR_1
                        + stepLabel
                        + TlConstants.ERRMSG_PROOF_LABEL_ERR_2
                        + theoremLabel
                        + TlConstants.ERRMSG_PROOF_LABEL_ERR_3
                        + mmtTheoremFile.getSourceFileName());
                }
            }
            else {
                updateMaxExistingMObjRef(ref);

                if (!getIsTheoremNew() &&

                    maxExistingMObjRef.getSeq() >=
                    theorem.getSeq()) {

                    throw new TheoremLoaderException(
                        TlConstants.
                                ERRMSG_PROOF_LABEL_SEQ_TOO_HIGH_1
                        + theoremLabel
                        + TlConstants.
                                ERRMSG_PROOF_LABEL_SEQ_TOO_HIGH_2
                        + theorem.getSeq()
                        + TlConstants.
                                ERRMSG_PROOF_LABEL_SEQ_TOO_HIGH_3
                        + ref.getLabel()
                        + TlConstants.
                                ERRMSG_PROOF_LABEL_SEQ_TOO_HIGH_4
                        + ref.getSeq()
                        + TlConstants.
                                ERRMSG_PROOF_LABEL_SEQ_TOO_HIGH_5
                        + mmtTheoremFile.getSourceFileName());
                }
            }
        }
    }

    /**
     *  Queues the theorem into either the ready list or the
     *  waiting list.
     *  <p>
     *  If the theorem's theoremStmtGroupUsedList is empty
     *  then it is ready to update because it doesn't refer to
     *  any other theorems in the MMTTheoremSet. Otherwise
     *  it goes into the waiting list. (When a theorem is stored
     *  into the LogicalSystem it is removed from the
     *  theoremStmtGroupUsedList of each theorem which refers to
     *  it.)
     *  <p>
     *  @param readyQueue queue of MMTTheorems ready for updating
     *             into the LogicalSystem.
     *  @param waitingList list of MMTTheorems which are not yet
     *             ready to update into the LogicalSystem.
     */
    public void queueForUpdates(LinkedList readyQueue,
                                LinkedList waitingList) {

        if (theoremStmtGroupUsedList.size() == 0) {
            readyQueue.add(this);
        }
        else {
            waitingList.add(this);
        }
    }


    /**
     *  Requeues every MMT Theorem which uses this theorem.
     *  <p>
     *  When a theorem is stored into the LogicalSystem it is
     *  removed from the theoremStmtGroupUsedList of each
     *  theorem which refers to it. To determine which
     *  theorems need requeueing, the updated theorem's
     *  usedByTheoremStmtGroup list is read and each theorem
     *  in that list is requeued.
     *  <p>
     *  @param readyQueue queue of MMTTheorems ready for updating
     *             into the LogicalSystem.
     *  @param waitingList list of MMTTheorems which are not yet
     *             ready to update into the LogicalSystem.
     *  @throws TheoremLoaderException if a data error is
     *          discovered resulting from the theorem update.
     */
    public void queueDependentsForUpdate(LinkedList readyQueue,
                                         LinkedList waitingList)
                            throws TheoremLoaderException {

        Iterator i                =
            usedByTheoremStmtGroupList.iterator();

        while (i.hasNext()) {

            ((TheoremStmtGroup)i.next()).
                reQueueAfterUsedTheoremUpdated(this,
                                               readyQueue,
                                               waitingList);
        }
    }

    /**
     *  Adds or updates the LogicalSystem with the MMT Theorem
     *  and if the Logical System has a Proof Verifier it
     *  runs the Metamath Proof Verification algorithm.
     *  <p>
     *  Note: a proof verification error does not trigger
     *  a TheoremLoaderException, which would thus halt the
     *  update of the entire MMTTheoremSet. Instead, any
     *  proof verification errors are stored in the Messages
     *  object for later display.
     *  <p>
     *  When a theorem is stored into the LogicalSystem it is
     *  removed from the theoremStmtGroupUsedList of each
     *  theorem which refers to it. To determine which
     *  theorems need requeueing, the updated theorem's
     *  usedByTheoremStmtGroup list is read and each theorem
     *  in that list is requeued.
     *  <p>
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param tlPreferences TlPreferences object.
     *  @throws TheoremLoaderException if a data error is
     *          discovered.
     *  @throws LangException if a data error is discovered.
     */
    public void updateLogicalSystem(
                            LogicalSystem  logicalSystem,
                            MessageHandler messageHandler,
                            TlPreferences  tlPreferences)
                                throws TheoremLoaderException,
                                       LangException {
        if (getIsTheoremNew()) {

            addTheoremToLogicalSystem(logicalSystem,
                                      messageHandler,
                                      tlPreferences);
        }
        else {

            updateTheoremInLogicalSystem(logicalSystem,
                                         messageHandler,
                                         tlPreferences);
        }

        if (!isProofIncomplete) {
            ProofVerifier proofVerifier
                                  = logicalSystem.getProofVerifier();
            if (proofVerifier != null) {
                try {
                	proofVerifier.verifyOneProof(theorem);
                }
                catch(LangException e) {
                    // don't halt the update over a proof error
                    messageHandler.accumErrorMessage(e.getMessage());
                }
            }
        }
    }

    /**
     *  Backs out the updates made into the Logical System.
     *  <p>
     *  If the theorem is new, it and its logical hypotheses
     *  are removed from the Logical System's statement table.
     *  <p>
     *  If the theorem was updated then the previous value
     *  of the theorem's proof and its $d restrictions are restored.
     *  @param stmtTbl the LogicalSystem object's stmtTbl.
     */
    public void reverseStmtTblUpdates(Map stmtTbl) {
        if (getIsTheoremNew()) {
            for (int i = 0; i < logHypArray.length; i++) {
                if (logHypArray[i] != null) {
                    stmtTbl.remove(logHypArray[i].getLabel());
                }
            }
            if (theorem != null) {
                stmtTbl.remove(theorem.getLabel());
            }
        }
        else {
            if (wasTheoremUpdated) {
                theorem.proofUpdates(oldProof,
                                     oldDjVarsArray,
                                     oldOptDjVarsArray);
            }
        }
    }

    /**
     *  Gets the isTheoremNew flag.
     *  <p>
     *  @return isTheoremNew flag.
     */
    public boolean getIsTheoremNew() {
        return isTheoremNew;
    }

    /**
     *  Gets the theorem label.
     *  <p>
     *  @return theorem label.
     */
    public String getTheoremLabel() {
        return theoremLabel;
    }

    /**
     *  Returns the MMTTheoremFile absolute pathname.
     *  <p>
     *  @return MMTTheoremFile absolute pathname.
     */
    public String getSourceFileName() {
        return mmtTheoremFile.getSourceFileName();
    }

    /**
     *  Gets the wasTheoremUpdated flag.
     *  <p>
     *  @return wasTheoremUpdated flag.
     */
    public boolean getWasTheoremUpdated() {
        return wasTheoremUpdated;
    }

    /**
     *  Gets the wasTheoremInserted flag.
     *  <p>
     *  @return wasTheoremInserted flag.
     */
    public boolean getWasTheoremInserted() {
        return wasTheoremInserted;
    }

    /**
     *  Gets the wasTheoremAppended flag.
     *  <p>
     *  @return wasTheoremAppended flag.
     */
    public boolean getWasTheoremAppended() {
        return wasTheoremAppended;
    }

    /**
     *  Gets the Theorem object or null if the
     *  theorem is new and has not yet been stored
     *  in the Logical System.
     *  <p>
     *  @return Theorem object or null.
     */
    public Theorem getTheorem() {
        return theorem;
    }

    /**
     *  Gets the wasLogHypInserted flag array.
     *  <p>
     *  @return wasLogHypInserted flag array.
     */
    public boolean[] getWasLogHypInsertedArray() {
        return wasLogHypInserted;
    }

    /**
     *  Gets the wasLogHypAppended flag array.
     *  <p>
     *  @return wasLogHypAppended flag array.
     */
    public boolean[] getWasLogHypAppendedArray() {
        return wasLogHypAppended;
    }

    /**
     *  Gets the LogHyp array which may contain nulls
     *  if the Theorem is new and has not yet been
     *  stored in the Logical System.
     *  <p>
     *  @return LogHyp array.
     */
    public LogHyp[] getLogHypArray() {
        return logHypArray;
    }

    /**
     *  Gets the BookManager insertSectionNbr for the theorem.
     *  <p>
     *  @return BookManager insertSectionNbr for the theorem.
     */
    public int getInsertSectionNbr() {
        return insertSectionNbr;
    }

    /**
     *  Returns the assigned sequence number array for
     *  new Logical Hypotheses.
     *  <p>
     *  @return assigned seq numbers of new Logical Hypotheses.
     */
    public int[] getAssignedLogHypSeq() {
        return assignedLogHypSeq;
    }

    /**
     *  Returns the assigned sequence number for a new Theorem.
     *  <p>
     *  @return assigned seq numbers of a new Theorem.
     */
    public int getAssignedTheoremSeq() {
        return assignedTheoremSeq;
    }

    /**
     *  Converts TheoremStmtGroup to String.
     *
     *  @return returns TheoremStmtGroup string;
     */
    public String toString() {
        return theoremLabel.toString();
    }

    /*
     * Computes hashcode for this TheoremStmtGroup
     *
     * @return hashcode for the TheoremStmtGroup
     */
    public int hashCode() {
        return theoremLabel.hashCode();
    }

    /**
     *  Compares TheoremStmtGroup object based on the label.
     *
     *  @param obj TheoremStmtGroup object to compare to
     *             this TheoremStmtGroup
     *
     *  @return returns negative, zero, or a positive int
     *  if this TheoremStmtGroup object is less than, equal to
     *  or greater than the input parameter obj.
     *
     */
    public int compareTo(Object obj) {
        return theoremLabel.compareTo(
                    ((TheoremStmtGroup)obj).
                        theoremLabel);
    }


    /*
     *  Compare for equality with another TheoremStmtGroup.
     *  <p>
     *  Equal if and only if the TheoremStmtGroup labels
     *  are equal and the obj to be compared to this object
     *  is not null and is a TheoremStmtGroup as well.
     *
     *  @return returns true if equal, otherwise false.
     */
    public boolean equals(Object obj) {
        return (this == obj) ? true
                : !(obj instanceof TheoremStmtGroup) ? false
                        : (theoremLabel.equals(
                                ((TheoremStmtGroup)obj).
                                    theoremLabel)
                          );
    }

    /**
     *  SEQ sequences by TheoremStmtGroup.theorem.getSeq().
     */
    static public final Comparator SEQ
            = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ( ((TheoremStmtGroup)o1).theorem.getSeq()
                     -
                     ((TheoremStmtGroup)o2).theorem.getSeq()
                   );
        }
    };

    /**
     *  NBR_LOG_HYP_SEQ sequences by number of LogHyps and Seq.
     */
    static public final Comparator NBR_LOG_HYP_SEQ
            = new Comparator() {

        public int compare(Object o1, Object o2) {
            int n                 =

                ((TheoremStmtGroup)o1).theorem.getLogHypArrayLength()
                -
                ((TheoremStmtGroup)o2).theorem.getLogHypArrayLength();

            if (n == 0) {

                n                 =

                ((TheoremStmtGroup)o1).theorem.getSeq()
                -
                ((TheoremStmtGroup)o2).theorem.getSeq();
            }

            return n;
        }
    };

    //
    // =======================================================
    // * Validation before beginning updates
    // =======================================================
    //

    private void reQueueAfterUsedTheoremUpdated(
                        TheoremStmtGroup usedTheoremStmtGroup,
                        LinkedList       readyQueue,
                        LinkedList       waitingList)
                            throws TheoremLoaderException {

        updateMaxExistingMObjRef(usedTheoremStmtGroup.theorem);

        if (getIsTheoremNew()) {

            if (usedTheoremStmtGroup.wasTheoremAppended) {
                mustAppend        = true;
            }
        }
        else {

            if (maxExistingMObjRef.getSeq() >= theorem.getSeq()) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_USED_THEOREM_SEQ_TOO_HIGH_1
                    + theoremLabel
                    + TlConstants.ERRMSG_USED_THEOREM_SEQ_TOO_HIGH_2
                    + theorem.getSeq()
                    + TlConstants.ERRMSG_USED_THEOREM_SEQ_TOO_HIGH_3
                    + usedTheoremStmtGroup.theoremLabel
                    + TlConstants.ERRMSG_USED_THEOREM_SEQ_TOO_HIGH_4
                    + usedTheoremStmtGroup.theorem.getSeq()
                    + TlConstants.ERRMSG_USED_THEOREM_SEQ_TOO_HIGH_5
                    + mmtTheoremFile.getSourceFileName());
            }
        }


        theoremStmtGroupUsedList.remove(usedTheoremStmtGroup);
        if (theoremStmtGroupUsedList.size() == 0) {
            readyQueue.add(this);
            waitingList.remove(this);
        }
    }

    private void loadParsedInputIntoStmtGroup(
                        Statementizer statementizer)
                            throws TheoremLoaderException {

        SrcStmt currSrcStmt;
        char    c;
        try {
            while ((currSrcStmt   = statementizer.getStmt())
                   != null) {

                c                 = currSrcStmt.keyword.charAt(1);

                if (c == MMIOConstants.
                            MM_BEGIN_COMMENT_KEYWORD_CHAR) {
                    continue;
                }

                if (c == MMIOConstants.
                            MM_AXIOMATIC_ASSRT_KEYWORD_CHAR        ||
                    c == MMIOConstants.MM_VAR_HYP_KEYWORD_CHAR     ||
                    c == MMIOConstants.MM_VAR_KEYWORD_CHAR         ||
                    c == MMIOConstants.MM_CNST_KEYWORD_CHAR        ||
                    c == MMIOConstants.MM_BEGIN_FILE_KEYWORD_CHAR) {

                    throw new TheoremLoaderException(
                        TlConstants.
                            ERRMSG_MMT_THEOREM_FILE_BAD_KEYWORD_1
                        + mmtTheoremFile.getSourceFileName()
                        + TlConstants.
                            ERRMSG_MMT_THEOREM_FILE_BAD_KEYWORD_2
                        + c);
                }

                if (c == MMIOConstants.MM_BEGIN_SCOPE_KEYWORD_CHAR) {

                    if (currSrcStmt.seq > 1) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_BEGIN_SCOPE_MUST_BE_FIRST_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    beginScopeSrcStmt
                                  = currSrcStmt;
                    continue;
                }

                if (endScopeSrcStmt != null) {
                    throw new TheoremLoaderException(
                        TlConstants.ERRMSG_END_SCOPE_MUST_BE_LAST_1
                        + mmtTheoremFile.getSourceFileName());
                }

                if (c == MMIOConstants.MM_END_SCOPE_KEYWORD_CHAR) {

                    if (beginScopeSrcStmt == null) {
                        throw new TheoremLoaderException(
                            TlConstants.ERRMSG_BEGIN_SCOPE_MISSING_1_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    endScopeSrcStmt
                                  = currSrcStmt;
                    continue;
                }

                if (c == MMIOConstants.MM_PROVABLE_ASSRT_KEYWORD_CHAR) {

                    if (theoremSrcStmt != null) {
                        throw new TheoremLoaderException(
                            TlConstants.ERRMSG_EXTRA_THEOREM_STMT_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    if (mmtTheoremFile.getLabel().compareTo(
                            currSrcStmt.label) != 0) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_THEOREM_LABEL_MISMATCH_1
                            + mmtTheoremFile.getLabel()
                            + TlConstants.
                                ERRMSG_THEOREM_LABEL_MISMATCH_2
                            + mmtTheoremFile.getSourceFileName());
                    }

                    if (isLabelInLogHypList(currSrcStmt.label)) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_THEOREM_LABEL_HYP_DUP_1
                            + currSrcStmt.label
                            + TlConstants.
                                ERRMSG_THEOREM_LABEL_HYP_DUP_2
                            + mmtTheoremFile.getSourceFileName());
                    }

                    if (currSrcStmt.proofBlockList != null) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_THEOREM_PROOF_COMPRESSED_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    theoremSrcStmt
                                  = currSrcStmt;
                    continue;
                }

                if (c == MMIOConstants.MM_LOG_HYP_KEYWORD_CHAR) {

                    if (theoremSrcStmt != null) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_THEOREM_LOG_HYP_SEQ_ERR_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    if (isLabelInLogHypList(currSrcStmt.label)) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_LOG_HYP_LABEL_HYP_DUP_1
                            + currSrcStmt.label
                            + TlConstants.
                                ERRMSG_LOG_HYP_LABEL_HYP_DUP_2
                            + mmtTheoremFile.getSourceFileName());
                    }

                    logHypSrcStmtList.add(currSrcStmt);
                    continue;
                }

                if (c == MMIOConstants.MM_DJ_VAR_KEYWORD_CHAR) {

                    if (theoremSrcStmt != null) {
                        throw new TheoremLoaderException(
                            TlConstants.
                                ERRMSG_THEOREM_DV_SEQ_ERR_1
                            + mmtTheoremFile.getSourceFileName());
                    }

                    dvSrcStmtList.add(currSrcStmt);
                    continue;
                }


                throw new TheoremLoaderException(
                    TlConstants.
                        ERRMSG_MMT_THEOREM_FILE_BOGUS_KEYWORD_1
                    + mmtTheoremFile.getSourceFileName()
                    + TlConstants.
                        ERRMSG_MMT_THEOREM_FILE_BOGUS_KEYWORD_2
                    + c);

            }

            if (theoremSrcStmt == null) {
                throw new TheoremLoaderException(
                    TlConstants.
                        ERRMSG_THEOREM_FILE_THEOREM_MISSING_1
                    + mmtTheoremFile.getSourceFileName());
            }

            if (endScopeSrcStmt == null &&
                beginScopeSrcStmt != null) {

                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_END_SCOPE_MISSING_2_1
                    + mmtTheoremFile.getSourceFileName());
            }

            if (beginScopeSrcStmt == null) {
                if (logHypSrcStmtList.size() > 0  ||
                    dvSrcStmtList.size()     > 0) {
                    throw new TheoremLoaderException(
                        TlConstants.
                            ERRMSG_BEGIN_END_SCOPE_PAIR_MISSING_3_1
                        + mmtTheoremFile.getSourceFileName());
                }
            }
        }
        catch (MMIOException e) {
            throw new TheoremLoaderException(e.getMessage());
        }
        catch (IOException e) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_THEOREM_FILE_IO_ERROR_1
                + mmtTheoremFile.getSourceFileName()
                + TlConstants.ERRMSG_MMT_THEOREM_FILE_IO_ERROR_2
                + e.getMessage());
        }
        finally {
            statementizer.close();
        }
    }

    private boolean isLabelInLogHypList(String label) {
        Iterator i                = logHypSrcStmtList.iterator();
        while (i.hasNext()) {
            SrcStmt s             = (SrcStmt)i.next();
            if (s.label.compareTo(label) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Compute a variety of facts about the SrcStmt
     *  objects read in from the MMTTheoremFile and
     *  check for validity of the data using the LogicalSystem.
     */
    private void validateStmtGroupData(
                                LogicalSystem logicalSystem,
                                MessageHandler      messages,
                                TlPreferences tlPreferences)
                                    throws TheoremLoaderException {

        Map symTbl                = logicalSystem.getSymTbl();
        Map stmtTbl               = logicalSystem.getStmtTbl();

        for (int i = 0; i < dvSrcStmtList.size(); i++) {
            validateDvSrcStmt(
                (SrcStmt)dvSrcStmtList.get(i),
                symTbl);
        }

        for (int i = 0; i < logHypSrcStmtList.size(); i++) {
            validateLogHypSrcStmt(
                tlPreferences,
                (SrcStmt)logHypSrcStmtList.get(i),
                symTbl,
                stmtTbl,
                i);
        }

        validateTheoremSrcStmt(tlPreferences,
                               symTbl,
                               stmtTbl);
    }

    private void validateDvSrcStmt(SrcStmt dvSrcStmt,
                                   Map     symTbl)
                               throws TheoremLoaderException {

        checkVarMObjRef(dvSrcStmt,
                        symTbl);
    }

    private void validateLogHypSrcStmt(TlPreferences tlPreferences,
                                       SrcStmt       logHypSrcStmt,
                                       Map           symTbl,
                                       Map           stmtTbl,
                                       int           logHypIndex)
                               throws TheoremLoaderException {

        checkSymMObjRef(logHypSrcStmt,
                        symTbl);

        validateMMTTypeCd(tlPreferences,
                          logHypSrcStmt);


        Stmt h                    =
            (Stmt)stmtTbl.get(logHypSrcStmt.label);

        if (h == null) {
            // is new?
        }
        else {
            if (!(h instanceof LogHyp)) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_LOG_HYP_STMT_MISMATCH_1
                    + logHypSrcStmt.label
                    + TlConstants.ERRMSG_LOG_HYP_STMT_MISMATCH_2
                    + logHypSrcStmt.seq
                    + TlConstants.ERRMSG_LOG_HYP_STMT_MISMATCH_3
                    + mmtTheoremFile.getSourceFileName());
            }

            if (!h.getFormula().srcStmtEquals(logHypSrcStmt)) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_LOG_HYP_FORMULA_MISMATCH_1
                    + logHypSrcStmt.label
                    + TlConstants.ERRMSG_LOG_HYP_FORMULA_MISMATCH_2
                    + logHypSrcStmt.seq
                    + TlConstants.ERRMSG_LOG_HYP_FORMULA_MISMATCH_3
                    + mmtTheoremFile.getSourceFileName());
            }
            updateMaxExistingMObjRef(h);
            logHypArray[logHypIndex]
                                  = (LogHyp)h;
        }
    }

    private void validateTheoremSrcStmt(TlPreferences tlPreferences,
                                        Map           symTbl,
                                        Map           stmtTbl)
                                    throws TheoremLoaderException {

        checkSymMObjRef(theoremSrcStmt,
                        symTbl);

        validateMMTTypeCd(tlPreferences,
                          theoremSrcStmt);

        Stmt t                    =
            (Stmt)stmtTbl.get(theoremLabel);

        if (t == null) {
            isTheoremNew          = true;
            for (int i = 0; i < logHypArray.length; i++) {
                if (logHypArray[i] != null) {
                    throw new TheoremLoaderException(
                        TlConstants.ERRMSG_NEW_THEOREM_OLD_LOG_HYP_1
                        + theoremLabel
                        + TlConstants.ERRMSG_NEW_THEOREM_OLD_LOG_HYP_2
                        + theoremSrcStmt.seq
                        + TlConstants.ERRMSG_NEW_THEOREM_OLD_LOG_HYP_3
                        + mmtTheoremFile.getSourceFileName());
                }
            }
        }
        else {
            if (!(t instanceof Theorem)) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_THEOREM_STMT_MISMATCH_1
                    + theoremLabel
                    + TlConstants.ERRMSG_THEOREM_STMT_MISMATCH_2
                    + theoremSrcStmt.seq
                    + TlConstants.ERRMSG_THEOREM_STMT_MISMATCH_3
                    + mmtTheoremFile.getSourceFileName());
            }

            if (!t.getFormula().srcStmtEquals(theoremSrcStmt)) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_THEOREM_FORMULA_MISMATCH_1
                    + theoremLabel
                    + TlConstants.ERRMSG_THEOREM_FORMULA_MISMATCH_2
                    + theoremSrcStmt.seq
                    + TlConstants.ERRMSG_THEOREM_FORMULA_MISMATCH_3
                    + mmtTheoremFile.getSourceFileName());
            }

            theorem               = (Theorem)t;

            checkInputLogHypsAgainstTheorem(theorem);

            oldProof              = theorem.getProof();
            oldDjVarsArray        = theorem.
                                        getMandFrame().
                                            djVarsArray;
            oldOptDjVarsArray     = theorem.
                                        getOptFrame().
                                            optDjVarsArray;
        }
    }

    private void validateMMTTypeCd(TlPreferences tlPreferences,
                                   SrcStmt       currSrcStmt)
                                    throws TheoremLoaderException {
        if (currSrcStmt.typ != null
                &&
            currSrcStmt.typ.equals(
                tlPreferences.getProvableLogicStmtTypeParm())) {
            return;
        }

        throw new TheoremLoaderException(
            TlConstants.ERRMSG_MMT_TYP_CD_NOT_VALID_1
            + currSrcStmt.typ
            + TlConstants.ERRMSG_MMT_TYP_CD_NOT_VALID_2
            + currSrcStmt.label
            + TlConstants.ERRMSG_MMT_TYP_CD_NOT_VALID_3
            + currSrcStmt.seq
            + TlConstants.ERRMSG_MMT_TYP_CD_NOT_VALID_4
            + mmtTheoremFile.getSourceFileName());
    }

    private void checkInputLogHypsAgainstTheorem(
                                    Theorem theorem)
                               throws TheoremLoaderException {

        boolean match             = true;

        LogHyp[] theoremLogHypArray
                                  = theorem.getLogHypArray();

        if (theoremLogHypArray.length == logHypArray.length) {

            loopI: for (int i = 0;
                        i < theoremLogHypArray.length;
                        i++) {

                loopJ: for (int j = 0;
                            j < logHypArray.length;
                            j++) {

                    if (logHypArray[j] == theoremLogHypArray[i]) {
                        continue loopI;
                    }
                }
                match             = false;
                break loopI;
            }
        }
        else {
            match                 = false;
        }

        if (match == false) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_LOG_HYPS_DONT_MATCH_1
                + theoremLabel
                + TlConstants.ERRMSG_LOG_HYPS_DONT_MATCH_2
                + theoremSrcStmt.seq
                + TlConstants.ERRMSG_LOG_HYPS_DONT_MATCH_3
                + mmtTheoremFile.getSourceFileName());
        }
    }

    private void checkSymMObjRef(SrcStmt x,
                                 Map     symTbl)
                                throws TheoremLoaderException {
        Sym     sym;
        String  s;
        if (x.typ != null) {
            sym                   = (Sym)symTbl.get(x.typ);
            if (sym == null) {
                generateSymNotFndError(x,
                                       x.typ);
            }
            else {
                updateMaxExistingMObjRef(sym);
            }
        }

        Iterator i                = x.symList.iterator();
        while (i.hasNext()) {
            s                     = (String)i.next();
            sym                   = (Sym)symTbl.get(s);
            if (sym == null) {
                generateSymNotFndError(x,
                                       s);
            }
            else {
                updateMaxExistingMObjRef(sym);
            }
        }
    }

    private void checkVarMObjRef(SrcStmt x,
                                  Map     symTbl)
                                throws TheoremLoaderException {
        Sym     sym;
        String  s;

        Iterator i                = x.symList.iterator();
        while (i.hasNext()) {
            s                     = (String)i.next();
            sym                   = (Sym)symTbl.get(s);
            if (sym == null) {
                generateSymNotFndError(x,
                                       s);
            }
            else {
                updateMaxExistingMObjRef(sym);
            }
            if (!sym.isVar()) {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_DJ_VAR_SYM_NOT_A_VAR_1
                    + s
                    + TlConstants.ERRMSG_DJ_VAR_SYM_NOT_A_VAR_2
                    + x.seq
                    + TlConstants.ERRMSG_DJ_VAR_SYM_NOT_A_VAR_3
                    + mmtTheoremFile.getSourceFileName());
            }
        }
    }


    private void generateSymNotFndError(SrcStmt x,
                                        String  id)
                            throws TheoremLoaderException {

        throw new TheoremLoaderException(
            TlConstants.ERRMSG_SRC_STMT_SYM_NOTFND_1
            + id
            + TlConstants.ERRMSG_SRC_STMT_SYM_NOTFND_2
            + x.seq
            + TlConstants.ERRMSG_SRC_STMT_SYM_NOTFND_3
            + mmtTheoremFile.getSourceFileName());
    }



    //
    // =======================================================
    // * Updates
    // =======================================================
    //

    private void updateTheoremInLogicalSystem(
                            LogicalSystem  logicalSystem,
                            MessageHandler       messages,
                            TlPreferences  tlPreferences)
                                throws TheoremLoaderException,
                                       LangException {

        wasTheoremUpdated         = true;

        Stmt[]  newProof          =
            theorem.
                setProof(
                    logicalSystem.getStmtTbl(),
                    theoremSrcStmt.proofList);

        if (tlPreferences.getDjVarsNoUpdate()) {
        }
        else {

            LinkedList mandDjVarsUpdateList
                                  = new LinkedList();
            LinkedList optDjVarsUpdateList
                                  = new LinkedList();
            buildMandAndOptDjVarsUpdateLists(
                                logicalSystem.getSymTbl(),
                                mandDjVarsUpdateList,
                                optDjVarsUpdateList);

            if (tlPreferences.getDjVarsMerge()) {
                theorem.mergeDjVars(mandDjVarsUpdateList,
                                    optDjVarsUpdateList);
            }

            else {
                if (tlPreferences.getDjVarsReplace()) {
                    theorem.replaceDjVars(mandDjVarsUpdateList,
                                          optDjVarsUpdateList);
                }
            }
        }

        if (tlPreferences.getAuditMessages()) {
            StringBuffer sb       = new StringBuffer();
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_UPD_1);
            sb.append(theorem.getLabel());
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_UPD_2);
            sb.append(theorem.getSeq());
//          sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_UPD_3);
//          sb.append(theorem.getChapterNbr());
//          sb.append(".");
//          sb.append(theorem.getSectionNbr());
//          sb.append(".");
//          sb.append(theorem.getSectionMObjNbr());
            messages.accumInfoMessage(sb.toString());
        }
    }

    private void addTheoremToLogicalSystem(
                            LogicalSystem  logicalSystem,
                            MessageHandler       messageHandler,
                            TlPreferences  tlPreferences)
                                throws TheoremLoaderException,
                                       LangException {

        SrcStmt     currSrcStmt;
        Iterator    iterator;

        SeqAssigner seqAssigner   = logicalSystem.getSeqAssigner();

        logicalSystem.beginScope();

        insertSectionNbr          = -1; // for BookManager.

        for (int i = 0; i < logHypArray.length; i++) {
            currSrcStmt           =
                (SrcStmt)logHypSrcStmtList.get(i);

            LogHyp s              =
                (LogHyp)(logicalSystem.
                            getStmtTbl().
                                get(currSrcStmt.label));
            if (s != null) {
                // another new mmt theorem already added it, so...
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_HYP_ADDED_TWICE_ERR_1
                    + theoremLabel
                    + TlConstants.ERRMSG_HYP_ADDED_TWICE_ERR_2
                    + currSrcStmt.label
                    + TlConstants.ERRMSG_HYP_ADDED_TWICE_ERR_3
                    + mmtTheoremFile.getSourceFileName());
            }

            assignedLogHypSeq[i]  = -1;
            if (!mustAppend &&
                maxExistingMObjRef != null) {

                assignedLogHypSeq[i]
                                  =
                    seqAssigner.
                        nextInsertSeq(
                            maxExistingMObjRef.
                                getSeq());
            }

            if (assignedLogHypSeq[i] == -1) {

                assignedLogHypSeq[i]
                                  =
                    seqAssigner.nextSeq();

                wasLogHypAppended[i]
                                  = true;
            }
            else {
                wasLogHypInserted[i]
                                  = true;

                //save this info for Book Manager!
                if (insertSectionNbr == -1) {
                    insertSectionNbr
                                  =
                        maxExistingMObjRef.getSectionNbr();
                }
            }

            logHypArray[i]        =
                loadLogHyp(logicalSystem,
                           assignedLogHypSeq[i],
                           currSrcStmt);

            if (tlPreferences.getAuditMessages()) {
                messageHandler.accumInfoMessage(
                    buildAddAuditMessage(
                        logHypArray[i],
                        wasLogHypAppended[i]));
            }

            updateMaxExistingMObjRef(logHypArray[i]);

            loadStmtParseTree(messageHandler,
                              logicalSystem,
                              logHypArray[i],
                              currSrcStmt);
        }

        assignedTheoremSeq        = -1;
        if (!mustAppend &&
            maxExistingMObjRef != null) {

            assignedTheoremSeq    =
                seqAssigner.
                    nextInsertSeq(
                        maxExistingMObjRef.
                            getSeq());
        }

        if (assignedTheoremSeq == -1) {

            assignedTheoremSeq    = seqAssigner.nextSeq();
            wasTheoremAppended    = true;
        }
        else {
            wasTheoremInserted    = true;
            //save this info for Book Manager!
            if (insertSectionNbr == -1) {
                insertSectionNbr
                                  =
                    maxExistingMObjRef.getSectionNbr();
            }
        }

        theorem                   =
            loadTheorem(logicalSystem,
                        assignedTheoremSeq);

        logicalSystem.endScope();

        loadStmtParseTree(messageHandler,
                          logicalSystem,
                          theorem,
                          theoremSrcStmt);

        if (dvSrcStmtList.size() > 0) {

            LinkedList mandDjVarsUpdateList
                                  = new LinkedList();
            LinkedList optDjVarsUpdateList
                                  = new LinkedList();
            buildMandAndOptDjVarsUpdateLists(
                                  logicalSystem.getSymTbl(),
                                  mandDjVarsUpdateList,
                                  optDjVarsUpdateList);

            theorem.replaceDjVars(mandDjVarsUpdateList,
                                  optDjVarsUpdateList);
        }

        if (tlPreferences.getAuditMessages()) {
            messageHandler.accumInfoMessage(
                buildAddAuditMessage(
                    theorem,
                    wasTheoremAppended));
        }
    }

    private String buildAddAuditMessage(Stmt    stmt,
                                        boolean appended) {
        StringBuffer sb           = new StringBuffer();
        sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_1);
        if (stmt instanceof Theorem) {
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_2a);
        }
        else {
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_2b);
        }
        sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_3);
        sb.append(stmt.getLabel());
        sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_4);
        sb.append(stmt.getSeq());
        if (appended) {
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_5a);
        }
        else {
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_5b);
        }
        sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_6);
        if (maxExistingMObjRef == null) {
            sb.append(" ");
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_7);
            sb.append(" ");
        }
        else {
            if (maxExistingMObjRef instanceof Sym) {
                sb.append(((Sym)maxExistingMObjRef).getId());
            }
            else {
                sb.append(((Stmt)maxExistingMObjRef).getLabel());
            }
            sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_7);
            sb.append(maxExistingMObjRef.getSeq());
        }
//      sb.append(TlConstants.ERRMSG_AUDIT_MSG_THEOREM_ADD_8);
//      sb.append(stmt.getChapterNbr());
//      sb.append(".");
//      sb.append(stmt.getSectionNbr());
//      sb.append(".");
//      sb.append(stmt.getSectionMObjNbr());
        return sb.toString();
    }

    private void buildMandAndOptDjVarsUpdateLists(
                                Map        symTbl,
                                LinkedList mandDjVarsUpdateList,
                                LinkedList optDjVarsUpdateList)
                                    throws LangException {

        SrcStmt   currSrcStmt;

        ArrayList inputDjVarsStmtList
                                  = new ArrayList();

        Iterator  dv              = dvSrcStmtList.iterator();
        while (dv.hasNext()) {

            currSrcStmt           = (SrcStmt)dv.next();
            // note: DjVar Vars used in an existing theorem must
            //       be defined in the mandatory or optional
            //       frame of the theorem.

            inputDjVarsStmtList.add(currSrcStmt.symList);
        }

        theorem.loadMandAndOptDjVarsUpdateLists(
                                    symTbl,
                                    inputDjVarsStmtList,
                                    mandDjVarsUpdateList,
                                    optDjVarsUpdateList);
    }

    // Note: In a rare case, the formula is parseable
    //       parse error but the proof is complete and
    //       invalid so that the theorem is inserted
    //       at a location which prevents parsing (the
    //       insert seq number is lower than the syntax
    //       axioms used). The solution for this is to
    //       not put a totally bogus proof in a theorem --
    //       if the proof is unknown, use a "?" to
    //       signify that the proof is incomplete.
    private void loadStmtParseTree(MessageHandler messageHandler,
                                   LogicalSystem logicalSystem,
                                   Stmt          stmt,
                                   SrcStmt       currSrcStmt)
                                throws TheoremLoaderException {

        SyntaxVerifier syntaxVerifier
                                  =
            logicalSystem.getSyntaxVerifier();

        if (syntaxVerifier == null) {
            return; // user loading all before grammar checking
        }

        ParseTree parseTree       =
            syntaxVerifier.
                parseOneStmt(messageHandler,
                             logicalSystem.getSymTbl(),
                             logicalSystem.getStmtTbl(),
                             stmt);
        if (parseTree == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_STMT_PARSE_ERR_1
                + currSrcStmt.label
                + TlConstants.ERRMSG_MMT_STMT_PARSE_ERR_2
                + currSrcStmt.seq
                + TlConstants.ERRMSG_MMT_STMT_PARSE_ERR_3
                + mmtTheoremFile.getSourceFileName());
        }
        stmt.setExprParseTree(parseTree);
    }


    private Theorem loadTheorem(LogicalSystem logicalSystem,
                                int           seq)
                                        throws LangException {

        Theorem theorem           =
            logicalSystem.
                addTheoremForTheoremLoader(
                    seq,
                    theoremLabel,
                    theoremSrcStmt.typ,
                    theoremSrcStmt.symList,
                    theoremSrcStmt.proofList);

        if (theoremSrcStmt.comment != null) {
            theorem.setDescription(theoremSrcStmt.comment);
        }

        return theorem;
    }


    /**
     *  Sends logical hypothesis to LogicalSystem.
     */
    private LogHyp loadLogHyp(LogicalSystem logicalSystem,
                              int           seq,
                              SrcStmt       currSrcStmt)
                        throws LangException {

        return logicalSystem.
                   addLogHypForTheoremLoader(
                       seq,
                       currSrcStmt.label,
                       currSrcStmt.typ,
                       currSrcStmt.symList);
    }


    //
    // =======================================================
    // * General Stuff
    // =======================================================
    //

    private void accumInList(List   list,
                             Object object) {
        if (!list.contains(object)) {
            list.add(object);
        }
    }

    private void updateMaxExistingMObjRef(MObj mObj) {

        if (maxExistingMObjRef == null ||
            mObj.getSeq() >  maxExistingMObjRef.getSeq()) {
            maxExistingMObjRef    = mObj;
        }
    }
}

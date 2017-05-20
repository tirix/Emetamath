//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  ProofAssistantEditor.java  0.09 08/01/2008
 *
 *  Version 0.02:
 *  ==> Add renumber feature
 *
 *  09-Sep-2006 - Version 0.03 - TMFF enhancement.
 *
 *  Version 0.04 06/01/2007
 *  ==> misc.
 *
 *  Version 0.05 08/01/2007
 *  ==> Modified to not rebuild the RequestMessagesGUI frame
 *      each time. The user should position the screen and
 *      resize it so that it is visible underneath (or above)
 *      the ProofAssistantEditor screen -- or just Alt-Tab to view
 *      any messages.
 *
 *  Version 0.06 09/11/2007
 *  ==> Bug fix -> set foreground/background at initialization.
 *  ==> Modify setProofTextAreaCursorPos(ProofWorksheet w) to
 *      compute the column number of the ProofAsstCursor's
 *      fieldId.
 *  ==> Added stuff for new "Set Indent" and
 *      "Reformat Proof: Swap Alt" menu items.
 *
 *  Version 0.07 02/01/2008
 *  ==> Add "accelerator" key definitions for
 *          Edit/Increase Font Size = Ctrl + "="
 *          Edit/Decrease Font Size = Ctrl + "-"
 *          Edit/Reformat Proof     = Ctrl + "R"
 *      Note: Ctrl + "+" seems to require Ctrl-Shift + "+",
 *            so in practice we code for Ctrl + "=", since
 *            "=" and "+" are most often on the same physical
 *            key and "=" is the unshifted glyph.
 *      Note: These Ctrl-Plus/Ctrl-Minus commands to increase/
 *            decrease font size are familiar to users of
 *            the Mozilla browser...
 *  ==> Fix bug: Edit/Decrease Font Size now checks for
 *            minimum font size allowed (8) and does not
 *            allow further reductions (a request to go from
 *            8 to 6 is treated as a change from 8 to 8.) This
 *            bug manifested as 'Exception in thread
 *            "AWT-EventQueue-0" java.lang.ArithmeticException:
 *            / by zero at javax.swing.text.PlainView.paint(
 *            Unknown Source)'. Also added similar range checking
 *            for Edit/Increase Font Size.
 *  ==> Modify request processing for unify and tmffReformat
 *      to pass offset of caret plus one as "inputCaretPos"
 *      for use in later caret positioning.
 *  ==> Tweak: Do not reformat when format number or indent
 *             amount is changed. This allows for single step
 *             reformatting -- but requires that the user
 *             manually initiate reformatting after changing
 *             format number or indent amount.
 *  ==> Add "Reformat Step" and "Reformat Step: Swap Alt" to
 *      popup menu. Then modified tmffReformat-related stuff
 *      to pass the boolean "inputCursorStep" to the standard
 *      reformatting procedure(s) so that the request can be
 *      handled using the regular, all-steps logic.
 *  ==> Turn "Greetings, friend" literal into PaConstants
 *      constant, PROOF_ASST_GUI_STARTUP_MSG.
 *  ==> Add "Incomplete Step Cursor" Edit menu item.
 *
 *  Version 0.08 03/01/2008
 *  ==> Add StepSelectorSearch to Unify menu
 *  ==> Add "callback" function for use by StepSelectionDialog,
 *          proofAsstGUI.unifyWithStepSelectorChoice()
 *  ==> Add Unify + Rederive to Unify menu
 *  ==> Eliminate Unify + Get Hints from Unify Menu
 *
 *  Version 0.09 08/01/2008
 *  ==> Add TheoremLoader stuff.
 */

package org.tirix.emetamath.editors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mmj.pa.*;
import mmj.lang.Assrt;
import mmj.lang.Theorem;
import mmj.lang.Messages;
import mmj.tmff.TMFFException;
import mmj.tmff.TMFFConstants;
import mmj.tl.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.editors.text.TextEditor;

/**
 *  The <code>ProofAssistantEditor</code> class is the main user
 *  interface for the mmj2 Proof Assistant feature.
 *  <p>
 *  A proof is represented in the GUI as a single text
 *  area, and the GUI knows nothing about the contents
 *  inside; all work on the proof is done elsewhere via
 *  mmj.pa.ProofAsst.java.
 *  <p>
 *  Note: ProofAssistantEditor is single-threaded in the ProofAsst
 *  process which is triggered in BatchMMJ2. The RunParm
 *  that triggers ProofAssistantEditor does not terminate until
 *  ProofAssistantEditor terminates.
 *  <p>
 *  The main issues dealt with in the GUI have to do with
 *  doing all of the screen updating code on the Java
 *  event thread. Unification is performed using a separate
 *  thread which "calls back" to ProofAssistantEditor when/if the
 *  Unificatin process is complete. (As of February 2006,
 *  the longest theorem unification computation is around
 *  1/2 second.)
 */
public class ProofAssistantEditor3 extends TextEditor {

    // save constructor parms: proofAsst, proofAsstPreferences
    private ProofAsst               proofAsst;
    private ProofAsstPreferences
                                    proofAsstPreferences;

    private TheoremLoader           theoremLoader;
    private TlPreferences           tlPreferences;

    private JTextArea               proofMessageArea;

    private String                  proofTheoremLabel = "";

    private ProofTextChanged        proofTextChanged;
    private int                     nbrTimesSavedSinceNew;

    private UndoManager             undoManager;
    private ProofAsstGUIUndoableEditListener
            proofAsstGUIUndoableEditListener;
    private JMenuItem               editUndoItem;
    private JMenuItem               editRedoItem;

    private JFileChooser            fileChooser;
    private String                  screenTitle;

    private JFileChooser            mmtFolderChooser;

    private JMenuItem               cancelRequestItem;

    private JMenuItem               fontStyleBoldItem;
    private JMenuItem               fontStylePlainItem;

    private JPopupMenu              popupMenu;

    private RequestThreadStuff
                                    requestThreadStuff;

    private Font                    proofFont;

    private RequestMessagesGUI      requestMessagesGUI;

    private StepSelectorDialog      stepSelectorDialog;
    private ProofAssistantEditor            proofAsstGUI;

    /**
     *  Sequence number of Proof Worksheet theorem.
     *  <p>
     *  Set to MObj.seq if proof theorem already exists.
     *  Otherwise, set to LOC_AFTER stmt sequnce + 1
     *  if LOC_AFTER input (else Integer.MAX_VALUE).
     */
    private int currProofMaxSeq   = Integer.MAX_VALUE;

    /**
     *  Get sequence number of Proof Worksheet theorem.
     *  <p>
     *  Equals MObj.seq if proof theorem already exists.
     *  Otherwise, set to LOC_AFTER stmt sequnce + 1
     *  if LOC_AFTER input (else Integer.MAX_VALUE).
     *
     *  @return currProofMaxSeq number of Proof Worksheet theorem.
     */
    public int getCurrProofMaxSeq() {
        return currProofMaxSeq;
    }

    /**
     *  Set sequence number of Proof Worksheet theorem.
     *  <p>
     *  Equals MObj.seq if proof theorem already exists.
     *  Otherwise, set to LOC_AFTER stmt sequnce + 1
     *  if LOC_AFTER input (else Integer.MAX_VALUE).
     *
     *  @param currProofMaxSeq number of Proof Worksheet theorem.
     */
    public void setCurrProofMaxSeq(int currProofMaxSeq) {
        this.currProofMaxSeq      = currProofMaxSeq;
    }

    /**
     *  Normal constructor for setting up ProofAssistantEditor.
     *
     *  @param proofAsst ProofAsst object
     *  @param proofAsstPreferences variable settings
     *  @param theoremLoader mmj.tl.TheoremLoader object
     */
    public ProofAssistantEditor3(ProofAsst            proofAsst,
                        ProofAsstPreferences proofAsstPreferences,
                        TheoremLoader        theoremLoader) {

        this.proofAsst            = proofAsst;
        this.proofAsstPreferences = proofAsstPreferences;

        this.theoremLoader        = theoremLoader;
        tlPreferences             = theoremLoader.getTlPreferences();

        proofAsstGUI              = this;

        File startupProofWorksheetFile
                                  =
            proofAsstPreferences.getStartupProofWorksheetFile();


        if (startupProofWorksheetFile == null) {
            if (proofAsstPreferences.getProofFolder() != null) {
                buildFileChooser(
                    new File(
                        proofAsstPreferences.getProofFolder(),
                        PaConstants.SAMPLE_PROOF_LABEL
                        + proofAsstPreferences.
                            getDefaultFileNameSuffix()));
            }
            else {
                buildFileChooser(
                    new File(
                        PaConstants.SAMPLE_PROOF_LABEL
                        + proofAsstPreferences.
                            getDefaultFileNameSuffix()));
            }
            updateScreenTitle(fileChooser.getSelectedFile());
            buildGUI(PaConstants.SAMPLE_PROOF_TEXT);
        }
        else {
            buildFileChooser(
                startupProofWorksheetFile);
            updateScreenTitle(fileChooser.getSelectedFile());
            buildGUI(
                readProofTextFromFile(
                    startupProofWorksheetFile));
        }
    }

    public void unifyWithStepSelectorChoice(
                            StepRequest stepRequest) {

        startUnificationAction(false, // no renum
                               null,  // no preprocess request
                               stepRequest, // s/b SELECTOR_CHOICE
                               null); // no TL Request
    }

    private void buildGUI(String newProofText) {

        displayRequestMessagesGUI(
            PaConstants.PROOF_ASST_GUI_STARTUP_MSG);

        if (proofAsstPreferences.getTextAtTop()) {
            buildGUIProofTextStuff(newProofText);
            buildGUIMessageTextStuff();
        }
        else {
            buildGUIMessageTextStuff();
            buildGUIProofTextStuff(newProofText);
        }
    }

    private class ProofTextChanged implements DocumentListener {
        boolean changes;
        public ProofTextChanged(boolean changes) {
            this.changes = changes;
        }
        public synchronized boolean getChanges() {
            return changes;
        }
        public synchronized void setChanges(
                                    boolean changes) {
            this.changes = changes;
        }
        public void changedUpdate(DocumentEvent e) {
            setChanges(true);
        }
        public void insertUpdate(DocumentEvent e) {
            setChanges(true);
        }
        public void removeUpdate(DocumentEvent e) {
            setChanges(true);
        }
    }

    private String getProofTheoremLabel() {
        return proofTheoremLabel;
    }
    private void setProofTheoremLabel(String s) {
        proofTheoremLabel = s;
    }
    private void setNbrTimesSavedSinceNew(int n) {
        nbrTimesSavedSinceNew = n;
    }
    private void incNbrTimesSavedSinceNew() {
        ++nbrTimesSavedSinceNew;
    }
    private int getNbrTimesSavedSinceNew() {
        return nbrTimesSavedSinceNew;
    }

    /*
     *  Build title using ProofAssistantEditor caption + full path name.
     *  If title length > textColumns - 15
     *      build title using ProofAssistantEditor caption + just file name
     *          if title length > textColumns - 15
     *              build title using just file name
     *              if title length > textColumns - 15
     *                  build title using just ProofAssistantEditor caption.
     */
    private String buildScreenTitle(IFile file) {
        int maxLength             =
            proofAsstPreferences.getTextColumns()
            - 15;

        if (file == null ||
            file.getName().length() > maxLength) {
            return PaConstants.PROOF_ASST_FRAME_TITLE;
        }

        StringBuffer s            = new StringBuffer(maxLength);

        s.append(PaConstants.PROOF_ASST_FRAME_TITLE);

        if (appendToScreenTitle(s, " - ") < 0) {
            return s.toString();
        }

        if (appendToScreenTitle(s, file.getName()) < 0) {
            if (appendToScreenTitle(s, file.getName()) < 0) {
                return file.getName();
            }
        }
        return s.toString();
    }
    private int appendToScreenTitle(StringBuffer s,
                                    String       t) {
        if (t.length() > s.capacity()) {
            return -1;
        }
        s.append(t);
        return 0;
    }

    private String getNewIncompleteStepCursorOption() {

        String s                  =
            proofAsstPreferences.getIncompleteStepCursorOptionNbr();

        String incompleteStepCursorOptionListString
                                  =
            proofAsstPreferences.
                getIncompleteStepCursorOptionListString();

        String origPromptString   =
               PaConstants.PROOF_ASST_INCOMPLETE_STEP_CURSOR_OPTION_LIST
               + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
               + incompleteStepCursorOptionListString
               + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
               + PaConstants.
                PA_GUI_SET_INCOMPLETE_STEP_CURSOR_OPTION_PROMPT;

        String promptString       = origPromptString;

        while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) {
                return s; //cancelled input
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            try {
                return(
                    proofAsstPreferences.
                        validateIncompleteStepCursorOptionNbr(
                            s));
            }
            catch (ProofAsstException e) {
                promptString      =
                    origPromptString
                    + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                    + e.getMessage();
            }
        }
    }

    private String getNewSoftDjErrorOption() {

        String s                  =
            proofAsstPreferences.getDjVarsSoftErrorsOptionNbr();

        String softDjErrorOptionListString
                                  =
            proofAsstPreferences.
                getSoftDjErrorOptionListString();

        String origPromptString   =
               PaConstants.PROOF_ASST_SOFT_DJ_ERROR_OPTION_LIST
               + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
               + softDjErrorOptionListString
               + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
               + PaConstants.PA_GUI_SET_SOFT_DJ_ERROR_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) {
                return s; //cancelled input
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            try {
                return(
                    proofAsstPreferences.
                        validateDjVarsSoftErrorsOptionNbr(
                            s));
            }
            catch (ProofAsstException e) {
                promptString      =
                    origPromptString
                    + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                    + e.getMessage();
            }
        }
    }

    private Theorem getTheorem() {
        String s                  = new String("");

        String promptString       =
            PaConstants.PA_GUI_GET_THEOREM_LABEL_PROMPT;

        Theorem theorem;

        while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) {
                return null;  //cancelled input
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString       =
                    PaConstants.PA_GUI_GET_THEOREM_LABEL_PROMPT;
                continue;
            }
            theorem               = proofAsst.getTheorem(s);
            if (theorem != null) {
                return theorem;
            }
            promptString          =
                new String(
                    PaConstants.PA_GUI_GET_THEOREM_LABEL_PROMPT_2_1
                    + s
                    + PaConstants.
                        PA_GUI_GET_THEOREM_LABEL_PROMPT_2_2);
        }
    }

    private String readProofTextFromFile(IFile file) {
        String newProofText;

        char[]       cBuffer      = new char[1024];

        StringBuffer sb           = new StringBuffer(cBuffer.length);

        int          len          = 0;
        try {
            BufferedReader r      =
                new BufferedReader(new InputStreamReader(file.getContents()));
            while ((len = r.read(cBuffer, 0, cBuffer.length)) != -1) {
                sb.append(cBuffer, 0, len);
            }
            r.close();
            newProofText          = new String(sb);
        }
        catch (IOException e) {
            newProofText =
                PaConstants.ERRMSG_PA_GUI_READ_PROOF_IO_ERR_1
                + e.getMessage();
        }

        return newProofText;
    }

    private void doFileSaveAction(boolean exitingNow) {

        IFile file;
        if (getNbrTimesSavedSinceNew() > 0) {
            file = fileChooser.getSelectedFile();
            if (file.exists()) {
                saveOldProofTextFile(file);
                updateMainFrameTitleIfNecessary(exitingNow);
                return;
            }
            else {
                setNbrTimesSavedSinceNew(0); //should not happen
            }
        }

        int returnVal             =
                fileChooser.showSaveDialog(getMainFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file                  = fileChooser.getSelectedFile();
            if (file.exists()) {
                saveOldProofTextFile(file);
            }
            else {
                saveNewProofTextFile(file);
            }
            updateMainFrameTitleIfNecessary(exitingNow);
        }
    }

    private void doFileSaveAsAction() {
        File newFile;

        File oldFile              = fileChooser.getSelectedFile();

        int  returnVal            =
                fileChooser.showSaveDialog(getMainFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            newFile               = fileChooser.getSelectedFile();
            if (newFile.exists()) {
                if (getYesNoAnswer(
                        PaConstants.ERRMSG_PA_GUI_FILE_EXISTS_1
                        + newFile.getAbsolutePath()
                        + PaConstants.ERRMSG_PA_GUI_FILE_EXISTS_2)
                    == JOptionPane.YES_OPTION) {
                    saveOldProofTextFile(newFile);
                }
                else {
                    fileChooser.setSelectedFile(oldFile);
                }
            }
            else {
                saveNewProofTextFile(newFile);
            }
        }
        updateMainFrameTitleIfNecessary(false);

        // this prevents a title and filename update if the
        // user changes the THEOREM= label now...because they
        // used SaveAs we are taking them at their word that
        // this is the file name to use regardless!!!
        setProofTheoremLabel(null); //tricky - avoid title update

    }

    private void updateMainFrameTitleIfNecessary(
                                    boolean exitingNow) {
        if (!exitingNow) {
            String newScreenTitle =
                buildScreenTitle(
                    fileChooser.getSelectedFile());
            if (screenTitle.compareTo(newScreenTitle) != 0) {
                startRequestAction(
                    new RequestUpdateMainFrameTitle());
            }
        }
    }

    private int getYesNoAnswer(String messageAboutIt) {
        int answer                = JOptionPane.YES_OPTION; //default
        try {
            answer                =
                JOptionPane.showConfirmDialog(
                            getMainFrame(),
                            messageAboutIt,
                            PaConstants.PA_GUI_YES_NO_TITLE,
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                            );
        }
        catch (HeadlessException e) {
        }
        return answer;
    }

    private void saveNewProofTextFile(IFile file) {
        try {
        	file.setContents(inputStream, arg1, arg2)
            BufferedWriter w      =
                new BufferedWriter(
                    new FileWriter(
                        file));
            String s              = getProofTextAreaText();
            w.write(s, 0, s.length());
            w.close();
        }
        catch (Throwable e) {
            String s              =
                PaConstants.ERRMSG_PA_GUI_SAVE_IO_ERROR_1
               + e.getMessage();

            JOptionPane.showMessageDialog(
                    getMainFrame(),
                    s,
                    PaConstants.PA_GUI_SAVE_NEW_PROOF_TEXT_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }

        proofTextChanged.setChanges(false);
        clearUndoRedoCaches();
        setNbrTimesSavedSinceNew(1);
    }

    private void saveOldProofTextFile(File file) {
        try {
            BufferedWriter w      =
                new BufferedWriter(
                    new FileWriter(
                        file));
            String s              = getProofTextAreaText();
            w.write(s, 0, s.length());
            w.close();
        }
        catch (Throwable e) {
            String s              =
                PaConstants.ERRMSG_PA_GUI_SAVE_IO_ERROR2_1
                    + e.getMessage();
            JOptionPane.showMessageDialog(
                    getMainFrame(),
                    s,
                    PaConstants.PA_GUI_SAVE_OLD_PROOF_TEXT_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }

        proofTextChanged.setChanges(false);
        clearUndoRedoCaches();
        incNbrTimesSavedSinceNew();

    }

    // ------------------------------------------------------
    // | Unify menu stuff                                   |
    // ------------------------------------------------------


    private void doSetShowSubstitutionsItemAction() {

        boolean newShowSubstitutions
                                  = getNewShowSubstitutions();

        proofAsstPreferences.
             setStepSelectorShowSubstitutions(
                        newShowSubstitutions);
    }

    private boolean getNewShowSubstitutions() {

        String s                  =
            Boolean.toString(
                proofAsstPreferences.
                    getStepSelectorShowSubstitutions());

        String origPromptString   =
            PaConstants.PA_GUI_SET_SHOW_SUBST_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return proofAsstPreferences.
                           getStepSelectorShowSubstitutions();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            try {
                return(
                    proofAsstPreferences.
                        validateStepSelectorShowSubstitutions(
                            s));
            }
            catch (IllegalArgumentException e) {
                promptString      =
                    origPromptString
                    + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                    + e.getMessage();
            }
        }
    }

    private void doSetMaxResultsItemAction() {

        int newMaxResults         = getNewMaxResults();

        if (newMaxResults != -1) {
            proofAsstPreferences.
                    setStepSelectorMaxResults(
                        newMaxResults);
        }
    }

    private int getNewMaxResults() {

        String s                  =
            Integer.toString(
                proofAsstPreferences.
                    getStepSelectorMaxResults());

        String origPromptString   =
            PaConstants.PA_GUI_SET_MAX_RESULTS_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) {
                return -1; //cancelled input
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            try {
                return(
                    proofAsstPreferences.
                        validateStepSelectorMaxResults(
                            s));
            }
            catch (IllegalArgumentException e) {
                promptString      =
                    origPromptString
                    + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                    + e.getMessage();
            }
        }
    }

    private void startUnificationAction(
                            boolean           renumReq,
                            PreprocessRequest preprocessRequest,
                            StepRequest       stepRequest,
                            TLRequest         tlRequest) {
        RequestUnify request      =
            new RequestUnify(proofTextChanged.getChanges(),
                             renumReq,
                             preprocessRequest,
                             stepRequest,
                             tlRequest);
        startRequestAction(request);
    }

    private void reshowStepSelectorDialogAction() {
        if (stepSelectorDialog != null) {
            stepSelectorDialog.setVisible(true);
        }
    }

    /*
     *  Get rid of this thing if it is still hanging around,
     *  we may need the memory space.
     */
    private void disposeOfOldSelectorDialog() {
        if (stepSelectorDialog != null) {
            stepSelectorDialog.dispose();
        }
    }


    // ------------------------------------------------------
    // | TL menu stuff                                      |
    // ------------------------------------------------------


    private void doUnifyPlusStoreInLogSysAndMMTFolderItemAction() {
        startUnificationAction(
            false, // no renum
            null,  // no preprocess request
            null, //  no step selector request
            new StoreInLogSysAndMMTFolderTLRequest());
    }

    private void doUnifyPlusStoreInMMTFolderItemAction() {
        startUnificationAction(
            false, // no renum
            null,  // no preprocess request
            null, //  no step selector request
            new StoreInMMTFolderTLRequest());
    }

    private void doLoadTheoremsFromMMTFolderItemAction() {
        startRequestAction(
            new RequestLoadTheoremsFromMMTFolder());
    }

    private void doExtractTheoremToMMTFolderItemAction() {
        Theorem theorem           = getTheorem();
        if (theorem != null) {
            startRequestAction(
                new RequestExtractTheoremToMMTFolder(
                        theorem));
        }
    }

    private void doVerifyAllProofsItemAction() {
        startRequestAction(
            new RequestVerifyAllProofs());
    }

    private void doSetTLMMTFolderItemAction() {
        MMTFolder mmtFolder       = getNewMMTFolder();
    }

    private MMTFolder getNewMMTFolder() {

        String title              = "";
        File file                 =
            tlPreferences.getMMTFolder().getFolderFile();
        if (file != null) {
            title                 = file.getAbsolutePath();
        }
        mmtFolderChooser.setDialogTitle(title);

        int    returnVal;
        String s;
        String errMsg;
        while (true) {
            returnVal             =
                mmtFolderChooser.
                    showDialog(
                        getMainFrame(),
                        PaConstants.
                            PA_GUI_SET_TL_MMT_FOLDER_OPTION_PROMPT_1
                        );
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file              =
                    mmtFolderChooser.getSelectedFile();
                errMsg            =
                    tlPreferences.setMMTFolder(file);
                if (errMsg == null) {
                    break;
                }
                if (getYesNoAnswer(
                        errMsg
                        + PaConstants.
                            PA_GUI_SET_TL_MMT_FOLDER_OPTION_PROMPT_2)
                    == JOptionPane.YES_OPTION) {
                    continue;
                }
            }
            break;
        }
        return tlPreferences.getMMTFolder();
    }

    private void doSetTLDjVarsOptionItemAction() {
        String newDjVarsOption
                                  = getNewTLDjVarsOption();
    }

    private String getNewTLDjVarsOption() {

        String s                  =
            tlPreferences.
                getDjVarsOption();

        String origPromptString   =
            PaConstants.
                PA_GUI_SET_TL_DJ_VARS_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return tlPreferences.
                           getDjVarsOption();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            if (tlPreferences.setDjVarsOption(s)) {
                return tlPreferences.
                           getDjVarsOption();
            }
            promptString          =
                origPromptString
                + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                + TlConstants.ERRMSG_INVALID_DJ_VARS_OPTION_1
                + s
                + TlConstants.ERRMSG_INVALID_DJ_VARS_OPTION_2;
        }
    }

    private void doSetTLStoreMMIndentAmtItemAction() {
        int newStoreMMIndentAmt
                                  = getNewTLStoreMMIndentAmt();
    }

    private int getNewTLStoreMMIndentAmt() {

        String s                  =
            Integer.toString(
                tlPreferences.
                    getStoreMMIndentAmt());

        String origPromptString   =
            PaConstants.
                PA_GUI_SET_TL_STORE_MM_INDENT_AMT_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return tlPreferences.
                           getStoreMMIndentAmt();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            if (tlPreferences.setStoreMMIndentAmt(s)) {
                return tlPreferences.
                           getStoreMMIndentAmt();
            }
            promptString          =
                origPromptString
                + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                + TlConstants.ERRMSG_INVALID_STORE_MM_INDENT_AMT_1
                + s
                + TlConstants.ERRMSG_INVALID_STORE_MM_INDENT_AMT_2;
        }
    }

    private void doSetTLStoreMMRightColItemAction() {
        int newStoreMMRightCol
                                  = getNewTLStoreMMRightCol();
    }

    private int getNewTLStoreMMRightCol() {

        String s                  =
            Integer.toString(
                tlPreferences.
                    getStoreMMRightCol());

        String origPromptString   =
            PaConstants.
                PA_GUI_SET_TL_STORE_MM_RIGHT_COL_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return tlPreferences.
                           getStoreMMRightCol();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            if (tlPreferences.setStoreMMRightCol(s)) {
                return tlPreferences.
                           getStoreMMRightCol();
            }
            promptString          =
                origPromptString
                + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                + TlConstants.ERRMSG_INVALID_STORE_MM_RIGHT_COL_1
                + s
                + TlConstants.ERRMSG_INVALID_STORE_MM_RIGHT_COL_2;
        }
    }

    private void doSetTLStoreFormulasAsIsItemAction() {
        boolean newStoreFormulasAsIs
                                  = getNewTLStoreFormulasAsIs();
    }

    private boolean getNewTLStoreFormulasAsIs() {

        String s                  =
            Boolean.toString(
                tlPreferences.
                    getStoreFormulasAsIs());

        String origPromptString   =
            PaConstants.
                PA_GUI_SET_TL_STORE_FORMULAS_AS_IS_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return tlPreferences.
                           getStoreFormulasAsIs();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            if (tlPreferences.setStoreFormulasAsIs(s)) {
                return tlPreferences.
                           getStoreFormulasAsIs();
            }
            promptString          =
                origPromptString
                + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                + TlConstants.ERRMSG_INVALID_STORE_FORMULAS_ASIS_1
                + s
                + TlConstants.ERRMSG_INVALID_STORE_FORMULAS_ASIS_2;
        }
    }

    private void doSetTLAuditMessagesItemAction() {
        boolean newAuditMessages  = getNewTLAuditMessages();
    }

    private boolean getNewTLAuditMessages() {

        String s                  =
            Boolean.toString(
                tlPreferences.
                    getAuditMessages());

        String origPromptString   =
            PaConstants.PA_GUI_SET_TL_AUDIT_MESSAGES_OPTION_PROMPT;

        String promptString       = origPromptString;

        promptLoop: while (true) {
            s                     =
                (JOptionPane.
                    showInputDialog(
                        getMainFrame(),
                        promptString,
                        s)
                );
            if (s == null) { //cancelled input
                return tlPreferences.
                           getAuditMessages();
            }
            s                     = s.trim();
            if (s.equals("")) {
                promptString      = origPromptString;
                continue;
            }
            if (tlPreferences.setAuditMessages(s)) {
                return tlPreferences.
                           getAuditMessages();
            }
            promptString          =
                origPromptString
                + PaConstants.PROOF_WORKSHEET_NEW_LINE_STRING
                + TlConstants.ERRMSG_INVALID_AUDIT_MESSAGES_1
                + s
                + TlConstants.ERRMSG_INVALID_AUDIT_MESSAGES_2;

        }
    }


    // ------------------------------------------------------
    // | Inner classes to make requests for processing      |
    // | that occur on separate threads off of the event    |
    // | loop.                                              |
    // ------------------------------------------------------

    abstract class Request {
        ProofWorksheet w;
        Request() {
        }
        abstract void send();
        abstract void receive();

    }

    class RequestExtractTheoremToMMTFolder extends Request {
        Messages messageHandler;
        Theorem  theorem;
        RequestExtractTheoremToMMTFolder(Theorem theorem) {
            super();
            this.theorem          = theorem;
        }
        void send() {
        	messageHandler              =
                proofAsst.extractTheoremToMMTFolder(theorem);
        }
        void receive() {
            String s              =
                getOutputMessageText(messageHandler);
            if (s == null) {
                s                 =
                  PaConstants.
                   ERRMSG_PA_GUI_EXTRACT_THEOREMS_TO_MMT_FOLDER_NO_MSGS;
            }
            displayRequestMessages(s);
        }
    }

    class RequestLoadTheoremsFromMMTFolder extends Request {
        Messages messageHandler;
        RequestLoadTheoremsFromMMTFolder() {
            super();
        }
        void send() {
        	messageHandler              =
                proofAsst.loadTheoremsFromMMTFolder();
        }
        void receive() {
            String s              =
                getOutputMessageText(messageHandler);
            if (s == null) {
                s                 =
                  PaConstants.
                   ERRMSG_PA_GUI_LOAD_THEOREMS_FROM_MMT_FOLDER_NO_MSGS;
            }
            displayRequestMessages(s);
        }
    }

    class RequestVerifyAllProofs extends Request {
        Messages messages;
        RequestVerifyAllProofs() {
            super();
        }
        void send() {
            messages              =
                proofAsst.verifyAllProofs();
        }
        void receive() {
            String s              =
                getOutputMessageText(messages);
            if (s == null) {
                s                 =
                    PaConstants.
                        ERRMSG_PA_GUI_VERIFY_ALL_PROOFS_NO_MSGS;
            }
            displayRequestMessages(s);
        }
    }

    class RequestUpdateMainFrameTitle extends Request {
        File newFile;
        RequestUpdateMainFrameTitle() {
            super();
            newFile               = fileChooser.getSelectedFile();
        }
        RequestUpdateMainFrameTitle(File f) {
            super();
            newFile               = f;
        }
        void send() {
        }
        void receive() {
            updateScreenTitle(newFile);
            updateMainFrameTitle();
        }
    }

    class RequestEditUndo extends Request {
        RequestEditUndo() {
            super();
        }
        void send() {
        }
        void receive() {
            try {
                undoManager.undo();
                updateUndoRedoItems();
            }
            catch(CannotUndoException e) {
                displayRequestMessages(
                    e.getMessage());
            }
        }
    }

    class RequestEditRedo extends Request {
        RequestEditRedo() {
            super();
        }
        void send() {
        }
        void receive() {
            try {
                undoManager.redo();
                updateUndoRedoItems();
            }
            catch(CannotRedoException e) {
                displayRequestMessages(
                    e.getMessage());
            }
        }
    }

    class RequestUnify extends Request {
        boolean             renumReq;
        PreprocessRequest   preprocessRequest;
        StepRequest         stepRequest;
        TLRequest           tlRequest;
        boolean             textChangedBeforeUnify;
        RequestUnify(boolean            textChangedBeforeUnify,
                     boolean            renumReq,
                     PreprocessRequest  preprocessRequest,
                     StepRequest        stepRequest,
                     TLRequest          tlRequest) {
            super();
            this.textChangedBeforeUnify
                                  =
                 textChangedBeforeUnify;
            this.renumReq         = renumReq;
            this.preprocessRequest
                                  = preprocessRequest;
            this.stepRequest      = stepRequest;
            this.tlRequest        = tlRequest;
        }
        void send() {
            w                     =
                proofAsst.
                    unify(renumReq,
                          getProofTextAreaText(),
                          preprocessRequest,
                          stepRequest,
                          tlRequest,
                          proofTextArea.getCaretPosition() + 1);

        }
        void receive() {
            if (w.stepSelectorResults != null) {
                disposeOfOldSelectorDialog();
                stepSelectorDialog =
                    new StepSelectorDialog(
                            mainFrame,
                            w.stepSelectorResults,
                            proofAsstGUI,
                            proofAsstPreferences,
                            proofFont);
            }
            else {
                displayProofWorksheet(w);
            }
            proofTextChanged.setChanges(
                textChangedBeforeUnify);
        }
    }

    class RequestNewProof extends Request {
        String newTheoremLabel;
        RequestNewProof(String newTheoremLabel) {
            super();
            this.newTheoremLabel = newTheoremLabel;
        }
        void send() {
            w                     =
                proofAsst.startNewProof(newTheoremLabel);
        }
        void receive(){
            setProofTheoremLabel(""); //tricky - force title update
            displayProofWorksheet(w);

            clearUndoRedoCaches();
            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(0);
            disposeOfOldSelectorDialog();
        }
    }


    class RequestNewNextProof extends Request {
        int     currProofMaxSeq;

        RequestNewNextProof(int currProofMaxSeq) {
            super();
            this.currProofMaxSeq  = currProofMaxSeq;
        }
        void send() {
            w                     =
                proofAsst.startNewNextProof(currProofMaxSeq);
        }
        void receive() {
            setProofTheoremLabel(""); //tricky - force title update
            displayProofWorksheet(w);

            clearUndoRedoCaches();
            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(0);
            disposeOfOldSelectorDialog();
        }
    }

    class RequestFileOpen extends Request {
        File   selectedFile;
        String s;

        RequestFileOpen(File selectedFile) {

            super();
            this.selectedFile     = selectedFile;
        }
        void send() {
            s                     =
                readProofTextFromFile(selectedFile);
        }
        void receive() {
            setProofTextAreaText(s);

            setProofTheoremLabel(null); //tricky - avoid title update
            updateScreenTitle(fileChooser.getSelectedFile());
            updateMainFrameTitle();

            clearUndoRedoCaches();

            setProofTextAreaCursorPos(
                ProofAsstCursor.
                    makeProofStartCursor(),
                s.length());

            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(1);
            disposeOfOldSelectorDialog();
        }
    }

    class RequestGetProof extends Request {
        Theorem oldTheorem;
        boolean proofUnified;
        boolean hypsRandomized;

        RequestGetProof(Theorem oldTheorem,
                        boolean proofUnified,
                        boolean hypsRandomized) {
            super();
            this.oldTheorem       = oldTheorem;
            this.proofUnified     = proofUnified;
            this.hypsRandomized   = hypsRandomized;
        }
        void send() {
            w                     =
                proofAsst.getExistingProof(oldTheorem,
                                           proofUnified,
                                           hypsRandomized);
        }
        void receive() {
            setProofTheoremLabel(""); //tricky - force title update
            displayProofWorksheet(w);
            clearUndoRedoCaches();
            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(0);
            disposeOfOldSelectorDialog();
        }
    }

    class RequestFwdProof extends Request {
        int     currProofMaxSeq;
        boolean proofUnified;
        boolean hypsRandomized;

        RequestFwdProof(int     currProofMaxSeq,
                        boolean proofUnified,
                        boolean hypsRandomized) {
            super();
            this.currProofMaxSeq  = currProofMaxSeq;
            this.proofUnified     = proofUnified;
            this.hypsRandomized   = hypsRandomized;
        }
        void send() {
            w                     =
                proofAsst.getNextProof(currProofMaxSeq,
                                       proofUnified,
                                       hypsRandomized);
        }
        void receive() {
            setProofTheoremLabel(""); //tricky - force title update
            displayProofWorksheet(w);
            clearUndoRedoCaches();
            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(0);
            disposeOfOldSelectorDialog();
        }
    }

    class RequestBwdProof extends Request {
        int     currProofMaxSeq;
        boolean proofUnified;
        boolean hypsRandomized;

        RequestBwdProof(int     currProofMaxSeq,
                        boolean proofUnified,
                        boolean hypsRandomized) {
            super();
            this.currProofMaxSeq  = currProofMaxSeq;
            this.proofUnified     = proofUnified;
            this.hypsRandomized   = hypsRandomized;
        }
        void send() {
            w                     =
                proofAsst.getPreviousProof(getCurrProofMaxSeq(),
                                           true,   // proof unified
                                           false); // hyps Randomized
        }
        void receive() {
            setProofTheoremLabel(""); //tricky - force title update
            displayProofWorksheet(w);
            clearUndoRedoCaches();
            proofTextChanged.setChanges(false);
            setNbrTimesSavedSinceNew(0);
            disposeOfOldSelectorDialog();
        }
    }

    class RequestTMFFReformat extends Request {
        boolean inputCursorStep;
        boolean textChangedBeforeReformat;

        RequestTMFFReformat(boolean inputCursorStep,
                            boolean textChangedBeforeReformat) {
            super();
            this.inputCursorStep  = inputCursorStep;
            this.textChangedBeforeReformat
                                  =
                 textChangedBeforeReformat;
        }
        void send() {
            w                     =
                proofAsst.
                    tmffReformat(
                        inputCursorStep,
                        getProofTextAreaText(),
                        proofTextArea.getCaretPosition() + 1);
        }

        void receive(){
            displayProofWorksheet(w);
            proofTextChanged.setChanges(
                textChangedBeforeReformat);
        }
    }

    // ------------------------------------------------------
    // | Inner classes to manage thread requests            |
    // | that occur on separate threads off of the event    |
    // | loop.                                              |
    // ------------------------------------------------------

    public class RequestThreadStuff {

        Request request;

        Runnable displayRequestResults;

        Runnable sendRequest;

        /**
         *  Thread used for Unification of proof.
         */
        public Thread requestThread;

        /**
         *  Set the Thread value in RequestThreadStuff object.
         *
         *  @param t Thread used in RequestThreadStuff
         */
        public synchronized void setRequestThread(Thread t) {
            requestThread = t;
        }

        /**
         *  Get the Thread value in RequestThreadStuff object.
         *
         *  @return      Thread used in RequestThreadStuff
         */
        public synchronized Thread getRequestThread() {
            return requestThread;
        }


        /**
         *  Start the Thread used in the RequestThreadStuff object.
         */
        public void startRequestThread() {
            getRequestThread().start();
        }

        /**
         *  Cancel the Thread used in the RequestThreadStuff object
         *  if it exists (not null).
         */
        public void cancelRequestThread() {
            Thread requestThread = getRequestThread();
            if (requestThread != null) {
                requestThread.interrupt();
            }
        }

        /**
         *  Constructor.
         *
         *  Builds object for sending processing request
         *  and receiving the finished results.
         *
         *  @param r Request object reference
         */
         public RequestThreadStuff(Request r) {

            request               = r;

            displayRequestResults = new Runnable() {
                public void run() {
                    try {
                        request.receive();
                    }
                    finally {
                        tidyUpRequestStuff();
                    }
                }
            };

            sendRequest           = new Runnable() {
                public void run() {
                    try {
                        request.send();
                        EventQueue.invokeLater(
                            displayRequestResults);
                    }
                    finally {
                        setRequestThread(null);
                    }
                }
            };

            setRequestThread(
                new Thread(sendRequest));
        }
    }

    private synchronized RequestThreadStuff
                      getRequestThreadStuff() {
        return requestThreadStuff;
    }

    private synchronized void setRequestThreadStuff(
                                 RequestThreadStuff x) {
        requestThreadStuff        = x;
    }

    private synchronized void startRequestAction(Request r) {
        if (getRequestThreadStuff() == null) {

            setRequestThreadStuff(
                new RequestThreadStuff(r));

            getRequestThreadStuff().startRequestThread();

            cancelRequestItem.setEnabled(true);
            getMainFrame().
                setCursor(
                    Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
            proofTextArea.
                setCursor(
                    Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
        }
    }

    private synchronized void cancelRequestAction() {
        RequestThreadStuff k;
        if ((k = getRequestThreadStuff()) != null) {
            k.cancelRequestThread();
            tidyUpRequestStuff();
        }
    }

    private void tidyUpRequestStuff() {
        setRequestThreadStuff(null);
        cancelRequestItem.setEnabled(false);
        getMainFrame().setCursor(null);
        proofTextArea.setCursor(null);
    }

    private void displayProofWorksheet(ProofWorksheet w) {

        // keep this number for browsing forward and back!
        setCurrProofMaxSeq(w.getMaxSeq());

        String s                  = w.getOutputProofText();
        int    proofTextLength    = Integer.MAX_VALUE;
        if (s != null) { //no structural errors...
            setProofTextAreaText(s);
            proofTextLength       = s.length();
        }

        s                         = w.getTheoremLabel();
        if (s != null &&
            getProofTheoremLabel() != null) {
            if (s.compareToIgnoreCase(getProofTheoremLabel())
                != 0) {
                updateFileChooserFileForProofLabel(s);
                updateScreenTitle(fileChooser.getSelectedFile());
                updateMainFrameTitle();
                setProofTheoremLabel(s);
                setNbrTimesSavedSinceNew(0);
            }
        }

        setProofTextAreaCursorPos(w,
                                  proofTextLength);

        s                         = w.getOutputMessageText();
        displayRequestMessages(s);

    }

    private void displayRequestMessages(String s) {

        String messages;
        if (s == null) {
            messages              =
                PaConstants.ERRMSG_NO_MESSAGES_MSG_1;
        }
        else {
            messages              = s;
        }

        proofMessageArea.setText(s);
        setCursorToStartOfMessageArea();

        displayRequestMessagesGUI(messages);

    }

    private void displayRequestMessagesGUI(String messages) {

        RequestMessagesGUI u      = getRequestMessagesGUI();
        if (u == null) {
            if (messages == null) {
                return;
            }

            u                     =
                new RequestMessagesGUI(messages,
                                       proofAsstPreferences);
            setRequestMessagesGUI(u);
            u.showFrame(u.buildFrame());
        }
        else {
            u.changeFrameText(messages);
            u.setCursorToStartOfMessageArea();
        }
    }

    /**
     *  Positions the input caret to start of message text
     *  area and scrolls viewport to ensure that the start
     *  of the message text area is visible.
     *
     *  This is called only after updates to an existing
     *  AuxFrameGUI screen. It is automatically invoked
     *  during the initial display sequence of events..
     */
    public void setCursorToStartOfMessageArea() {

        try {

            proofMessageArea.setCaretPosition(0);

            JViewport v           =
                proofMessageScrollPane.getViewport();

            v.scrollRectToVisible(
                new Rectangle(0,      // x
                              0,      // y
                              1,      // width
                              1));    // height
        }
        catch (Exception e) {
            //ignore, don't care, did our best.
        }
    }

    /**
     *  Obtain output message text from ProofWorksheet.
     *  <p>
     *  Note: this is a key function used by ProofAssistantEditor.
     *  <p>
     *  Note: with word wrap 'on', newlines are ignored in
     *  JTextArea, so we insert spacer lines.
     *
     *  @param  messages Messages object.
     *  @return      Proof Error Message Text area as String.
     */
    public static String getOutputMessageText(Messages messages) {

        if (messages.getErrorMessageCnt() == 0 &&
            messages.getInfoMessageCnt() == 0) {
            return null;
        }

        StringBuffer sb           =
            new StringBuffer(
                (messages.getErrorMessageCnt() +
                 messages.getInfoMessageCnt())
                 * 80); //guessing average message length
        String[] msgArray         =
            messages.getErrorMessageArray();
        int msgCount              =
            messages.getErrorMessageCnt();
        for (int i = 0; i < msgCount; i++) {
            sb.append(msgArray[i]);
            sb.append(PaConstants.PROOF_WORKSHEET_NEW_LINE);
            sb.append(PaConstants.ERROR_TEXT_SPACER_LINE);
            sb.append(PaConstants.PROOF_WORKSHEET_NEW_LINE);
        }
        msgArray                  =
            messages.getInfoMessageArray();
        msgCount                  =
            messages.getInfoMessageCnt();
        for (int i = 0; i < msgCount; i++) {
            sb.append(msgArray[i]);
            sb.append(PaConstants.PROOF_WORKSHEET_NEW_LINE);
            sb.append(PaConstants.ERROR_TEXT_SPACER_LINE);
            sb.append(PaConstants.PROOF_WORKSHEET_NEW_LINE);
        }
        messages.clearMessages();
        return sb.toString();
    }

    private synchronized void
                setRequestMessagesGUI(RequestMessagesGUI u) {

        this.requestMessagesGUI = u;
    }
    private synchronized RequestMessagesGUI
                getRequestMessagesGUI() {

        return requestMessagesGUI;
    }
    private synchronized void
                disposeOfRequestMessagesGUI() {

        RequestMessagesGUI u    = getRequestMessagesGUI();
        if (u != null) {
            u.dispose();
        }
    }
}

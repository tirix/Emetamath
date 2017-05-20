//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  ProofAsstPreferences.java  0.09 08/01/2008
 *
 *  Version 0.02:
 *      - added new items for the Proof Assistant "Derive"
 *        feature:
 *              - maxUnifyAlternates
 *              - dummy VarPrefix
 *
 *  Version 0.03
 *      - moved left/right column info to TMFFPreferences
 *        and stowed instance of TMFFPreferences here.
 *        TMFF will be treated as a necessary sub-system
 *        for Proof Assistant, and its preferences are our
 *        preferences :)
 *      - other misc. preference items added, such as color.
 *
 *  Version 0.04, 06/01/2007
 *      - added setDjVarsSoftErrorsOption, etc.
 *
 *  Version 0.05, 08/01/2007
 *      - added get/setWorkVarManager(), etc.
 *      - removed dummy var stuff.
 *
 *  Varsion 0.06 - 11/01/2007
 *      - Add "ProofAsstErrorMessageRows"    RunParm
 *      - Add "ProofAsstErrorMessageColumns" RunParm
 *      - Add "ProofAsstTextAtTop"           RunParm
 *
 *  Varsion 0.07 - 02/01/2008
 *      - Add "ProofAsstIncompleteStepCursor"        RunParm
 *      - Add "ProofAsstOutputCursorInstrumentation" RunParm
 *      - Add "ProofAsstAutoReformat"                RunParm
 *
 *  Varsion 0.08 - 03/01/2008
 *      - Add "StepSelectorMaxResults"               RunParm
 *      - Add "StepSelectorShowSubstitutions"        RunParm
 *      - Remove Hints feature
 *      - Remove "ProofAsstMaxUnifyAlternates"       RunParm
 *
 *  Varsion 0.09 - 08/01/2008
 *      - Add "ProofAsstAssrtListFreespace"          RunParm
 */

package mmj.pa;
import  mmj.lang.Assrt;
import  mmj.lang.WorkVarManager;
import  mmj.tmff.TMFFPreferences;

import  java.io.File;
import  java.awt.Color;
import  java.awt.Font;
import  java.awt.GraphicsEnvironment;
import  java.util.Iterator;
import  java.util.TreeSet;

/**
 *  Holds user settings/preferences used by the
 *  Proof Assistant.
 */
public class ProofAsstPreferences {

    private   File           proofFolder;

    private   File           startupProofWorksheetFile;

    private   String         defaultFileNameSuffix;

    private   int            fontSize;

    private   String         fontFamily;

    private   boolean        fontBold;

    private   int            errorMessageRows;
    private   int            errorMessageColumns;
    private   boolean        textAtTop;

    private   TMFFPreferences
                             tmffPreferences;

    private   int            rpnProofLeftCol;

    private   int            rpnProofRightCol;

    // using VerifyProofs engine.
    private   boolean        recheckProofAsstUsingProofVerifier;

    private   boolean        exportFormatUnified;

    // randomizes LogHyps on output proof steps as a test of
    // proof assistant!
    private   boolean        exportHypsRandomized;

    private   boolean        exportDeriveFormulas;

    private   boolean        importCompareDJs;
    private   boolean        importUpdateDJs;

    private   Assrt[]        unifySearchExclude;

    private   int            stepSelectorMaxResults;
    private   boolean        stepSelectorShowSubstitutions;
    private   int            stepSelectorDialogPaneWidth;
    private   int            stepSelectorDialogPaneHeight;

    private   int            assrtListFreespace;

    private   boolean        outputCursorInstrumentation;
    private   boolean        autoReformat;

    private   boolean        undoRedoEnabled;

    private   Color          foregroundColor;

    private   Color          backgroundColor;

    private   boolean        djVarsSoftErrorsIgnore;
    private   boolean        djVarsSoftErrorsReport;
    private   boolean        djVarsSoftErrorsGenerate;
    private   boolean        djVarsSoftErrorsGenerateNew;
    private   boolean        djVarsSoftErrorsGenerateRepl;
    private   boolean        djVarsSoftErrorsGenerateDiffs;

    private   WorkVarManager workVarManager;

    private   StepUnifier    stepUnifier;

    private   String         incompleteStepCursor;
    private   boolean        incompleteStepCursorFirst;
    private   boolean        incompleteStepCursorLast;
    private   boolean        incompleteStepCursorAsIs;

    /**
     *  Default constructor.
     */
    public ProofAsstPreferences() {
        proofFolder               = null;
        startupProofWorksheetFile = null;

        defaultFileNameSuffix     =
            PaConstants.PA_GUI_DEFAULT_FILE_NAME_SUFFIX;


        fontSize                  =
            PaConstants.PROOF_ASST_FONT_SIZE_DEFAULT;
        fontBold                  =
            PaConstants.PROOF_ASST_FONT_BOLD_DEFAULT;
        fontFamily                =
            PaConstants.PROOF_ASST_FONT_FAMILY_DEFAULT;


        errorMessageRows          =
            PaConstants.PROOF_ASST_ERROR_MESSAGE_ROWS_DEFAULT;
        errorMessageColumns       =
            PaConstants.PROOF_ASST_ERROR_MESSAGE_COLUMNS_DEFAULT;
        textAtTop                 =
            PaConstants.PROOF_ASST_TEXT_AT_TOP_DEFAULT;


        tmffPreferences           = new TMFFPreferences();


        rpnProofLeftCol           =
            PaConstants.PROOF_ASST_RPN_PROOF_LEFT_COL_DEFAULT;
        rpnProofRightCol          =
            PaConstants.PROOF_ASST_RPN_PROOF_RIGHT_COL_DEFAULT;


        recheckProofAsstUsingProofVerifier
                                  =
            PaConstants.
                RECHECK_PROOF_ASST_USING_PROOF_VERIFIER_DEFAULT;


        exportFormatUnified       =
            PaConstants.PROOF_ASST_EXPORT_FORMAT_UNIFIED_DEFAULT;

        exportHypsRandomized      =
            PaConstants.PROOF_ASST_EXPORT_HYPS_RANDOMIZED_DEFAULT;

        exportDeriveFormulas      =
            PaConstants.PROOF_ASST_EXPORT_DERIVE_FORMULAS_DEFAULT;

        importCompareDJs          =
            PaConstants.PROOF_ASST_IMPORT_COMPARE_DJS_DEFAULT;

        importUpdateDJs           =
            PaConstants.PROOF_ASST_IMPORT_UPDATE_DJS_DEFAULT;


        unifySearchExclude        = new Assrt[0];


        stepSelectorMaxResults    =
            PaConstants.STEP_SELECTOR_MAX_RESULTS_DEFAULT;

        stepSelectorShowSubstitutions
                                  =
            PaConstants.STEP_SELECTOR_SHOW_SUBSTITUTIONS_DEFAULT;

        stepSelectorDialogPaneWidth
                                  =
            PaConstants.STEP_SELECTOR_DIALOG_PANE_WIDTH_DEFAULT;

        stepSelectorDialogPaneHeight
                                  =
            PaConstants.STEP_SELECTOR_DIALOG_PANE_HEIGHT_DEFAULT;


        assrtListFreespace
                                  =
            PaConstants.ASSRT_LIST_FREESPACE_DEFAULT;


        outputCursorInstrumentation
                                  =
            PaConstants.OUTPUT_CURSOR_INSTRUMENTATION_DEFAULT;

        autoReformat
                                  =
            PaConstants.AUTO_REFORMAT_DEFAULT;

        undoRedoEnabled           =
            PaConstants.UNDO_REDO_ENABLED_DEFAULT;

        foregroundColor           =
            PaConstants.DEFAULT_FOREGROUND_COLOR;

        backgroundColor           =
            PaConstants.DEFAULT_BACKGROUND_COLOR;

        setDjVarsSoftErrorsOption(
            PaConstants.PROOF_ASST_DJ_VARS_SOFT_ERRORS_DEFAULT);

        setIncompleteStepCursor(
            PaConstants.PROOF_ASST_INCOMPLETE_STEP_CURSOR_DEFAULT);

        // Note: this default constructor is available for test
        //       of ProofAssistantEditor in batch mode -- but is
        //       mainly used by mmj.util.ProofAsstBoss, which
        //       is responsible for loading workVarManager!
        //       "null" is not a valid default and would
        //       eventually result in an exception if not
        //       updated with an actual WorkVarmanager. We
        //       are *not* invoking the default WorkVarManager()
        //       constructor here because, in all likelyhood,
        //       all of its work would need to be thrown away
        //       and redone with the correct WorkVar settings.
        workVarManager            = null;
        stepUnifier               = null;
    }

    /**
     *  Set proof folder used for storing proof text areas
     *  in ProofAssistantEditor.
     *
     *  @param proofFolder proof folder used for storing
     *                     proof text areas
     */
    public void setProofFolder(File proofFolder) {
        this.proofFolder          = proofFolder;
    }

    /**
     *  Get proof folder used for storing proof text areas
     *  in ProofAssistantEditor.
     *
     *  @return proofFolder proof folder used for storing
     *                      proof text areas
     */
    public File getProofFolder() {
        return proofFolder;
    }

    /**
     *  Set startup Proof Worksheet File to be displayed when
     *  the ProofAssistantEditor is first displayed.
     *  in ProofAssistantEditor.
     *
     *  @param startupProofWorksheetFile File object or null.
     */
    public void setStartupProofWorksheetFile(
              File startupProofWorksheetFile) {
        this.startupProofWorksheetFile
                                  = startupProofWorksheetFile;
    }

    /**
     *  Get startup Proof Worksheet File to be displayed when
     *  the ProofAssistantEditor is first displayed.
     *
     *  @return startupProofWorksheetFile File object or null.
     */
    public File getStartupProofWorksheetFile() {
        return startupProofWorksheetFile;
    }

    /**
     *  Set default file name suffix.
     *
     *  @param defaultFileNameSuffix such as ".txt" or ".mmp"
     */
    public void setDefaultFileNameSuffix(String defaultFileNameSuffix) {
        this.defaultFileNameSuffix
                                  = defaultFileNameSuffix;
    }

    /**
     *  Get default file name suffix.
     *
     *  @return defaultFileNameSuffix such as ".txt" or ".mmp"
     */
    public String getDefaultFileNameSuffix() {
        return defaultFileNameSuffix;
    }

    /**
     *  Set Font Family Name used in ProofAssistantEditor.
     *  <p>
     *  Note: Proof Assistant formatting of formulas (via TMFF)
     *        REQUIRES a fixed-width font for symbol alignment!
     *        A proportional or variable-width font can be used
     *        but symbol alignments may be off.
     *  <p>
     *  Note: The default is "Monospaced", which works just fine...
     *  <p>
     *  @param fontFamily for ProofAssistantEditor
     */
    public synchronized void setFontFamily(String fontFamily) {
        this.fontFamily           = fontFamily;
    }

    /**
     *  Get Font Family Name used in ProofAssistantEditor.
     *
     *  @return font family name used in ProofAssistantEditor.
     */
    public synchronized String getFontFamily() {
        return fontFamily;
    }

    /**
     *  Set Font style to bold or regular.
     *  <p>
     *  Note: The default is "Bold", which seems excellent to me.
     *  <p>
     *  @param fontBold yes or no parameter.
     */
    public synchronized void setFontBold(boolean fontBold) {
        this.fontBold             = fontBold;
    }

    /**
     *  Get Font Bold style parameter used in ProofAssistantEditor.
     *
     *  @return fontBold yes or no.
     */
    public synchronized boolean getFontBold() {
        return fontBold;
    }


    /**
     *  Set font size used in ProofAssistantEditor.
     *
     *  NOTE: presently, font size is set in ProofAsstBoss
     *        as part of the start-up of ProofAssistantEditor, based
     *        on a RunParm. Then during operation of
     *        ProofAssistantEditor the user can increase or decrease
     *        the font size used, and those settings
     *        propagate to these ProofAsstPreferences
     *        (but are not stored externally for use in
     *        the next session -- permanent setting should
     *        be made in the RunParm file.)
     *
     *  @param fontSize font size for ProofAssistantEditor
     */
    public synchronized void setFontSize(int fontSize) {
        this.fontSize             = fontSize;
    }

    /**
     *  Get font size used in ProofAssistantEditor.
     *
     *  @return fontSize font size for ProofAssistantEditor.
     */
    public synchronized int getFontSize() {
        return fontSize;
    }

    /**
     *  Set line wrap on or off.
     *  <p>
     *  If line wrap is on then Newlines (carraige returns)
     *  will not be used to split formulas. Instead, space
     *  characters will be written to fill out the remaining
     *  text columns on the line.
     *  <p>
     *  @param lineWrap setting, on or off.
     */
    public void setLineWrap(boolean lineWrap) {
        tmffPreferences.setLineWrap(lineWrap);
    }

    /**
     *  Get the current lineWrap setting.
     *
     *  @return lineWrap setting.
     */
    public boolean getLineWrap() {
        return tmffPreferences.getLineWrap();
    }

    /**
     *  Set number of text columns used to display formulas.
     *  <p>
     *  This number is used to line wrapping and basically
     *  corresponds to the window used to display formulas.
     *  <p>
     *  A formula can be longer than this number, and
     *  the Frame should scroll -- assuming that lineWrap
     *  is off and there are no NewLines.
     *  <p>
     *  @param textColumns number of text columns.
     */
    public void setTextColumns(int textColumns) {
        tmffPreferences.setTextColumns(textColumns);
    }

    /**
     *  Get number of text columns used to display formulas.
     *  <p>
     *  This number is used to line wrapping and basically
     *  corresponds to the window used to display formulas.
     *  <p>
     *  A formula can be longer than this number, and
     *  the Frame should scroll -- assuming that lineWrap
     *  is off and there are no NewLines.
     */
    public int getTextColumns() {
        return tmffPreferences.getTextColumns();
    }


    /**
     *  Set number of text rows used to display formulas.
     *  <p>
     *  @param textRows number of text rows.
     */
    public void setTextRows(int textRows) {
        tmffPreferences.setTextRows(textRows);
    }

    /**
     *  Get number of text rows used to display formulas.
     *  <p>
     */
    public int getTextRows() {
        return tmffPreferences.getTextRows();
    }

    /**
     *  Set number of error message rows on the ProofAssistantEditor.
     *  <p>
     *  @param errorMessageRows number of error message rows.
     */
    public void setErrorMessageRows(int errorMessageRows) {
        this.errorMessageRows     = errorMessageRows;
    }

    /**
     *  Get number of error message rows on the ProofAssistantEditor.
     *  <p>
     */
    public int getErrorMessageRows() {
        return errorMessageRows;
    }

    /**
     *  Set number of error message columns on the ProofAssistantEditor.
     *  <p>
     *  @param errorMessageColumns number of error message columns.
     */
    public void setErrorMessageColumns(int errorMessageColumns) {
        this.errorMessageColumns     = errorMessageColumns;
    }

    /**
     *  Get number of error message columns on the ProofAssistantEditor.
     *  <p>
     */
    public int getErrorMessageColumns() {
        return errorMessageColumns;
    }

    /**
     *  Set Proof Text At Top option for ProofAssistantEditor.
     *  <p>
     *  @param textAtTop number of error message columns.
     */
    public void setTextAtTop(boolean textAtTop) {
        this.textAtTop     = textAtTop;
    }

    /**
     *  Get Proof Text At Top option for ProofAssistantEditor.
     *  <p>
     */
    public boolean getTextAtTop() {
        return textAtTop;
    }

    /**
     *  Set formula left column used in formatting proof
     *  text areas.
     *
     *  @param formulaLeftCol formula LeftCol used for formatting
     *                     formula text areas
     */
    public void setFormulaLeftCol(int formulaLeftCol) {
        tmffPreferences.setFormulaLeftCol(formulaLeftCol);
    }

    /**
     *  Get formula left column used in formatting proof
     *  text areas.
     *
     *  @return formulaLeftCol formula LeftCol used for
     *                      formatting formula text areas
     */
    public int getFormulaLeftCol() {
        return tmffPreferences.getFormulaLeftCol();
    }

    /**
     *  Set formula right column used in formatting proof
     *  text areas.
     *
     *  @param formulaRightCol formula RightCol used for
     *                     formatting formula text areas
     */
    public void setFormulaRightCol(int formulaRightCol) {
        tmffPreferences.setFormulaRightCol(formulaRightCol);
    }

    /**
     *  Get formula right column used in formatting proof
     *  text areas.
     *
     *  @return formulaRightCol formula RightCol used for
     *                      formatting formula text areas
     */
    public int getFormulaRightCol() {
        return tmffPreferences.getFormulaRightCol();
    }

    /**
     *  Set left column number for RPN statement labels
     *  when creating ProofAsstWorksheet.GeneratedProofStmt
     *
     *  @param rpnProofLeftCol left column for RPN label
     */
    public void setRPNProofLeftCol(int rpnProofLeftCol) {
        this.rpnProofLeftCol      = rpnProofLeftCol;
    }

    /**
     *  Get left column number for RPN statement labels
     *  when creating ProofAsstWorksheet.GeneratedProofStmt
     *
     *  @return rpnProofLeftCol left column or RPN label
     */
    public int getRPNProofLeftCol() {
        return rpnProofLeftCol;
    }


    /**
     *  Set right column number for RPN statement labels
     *  when creating ProofAsstWorksheet.GeneratedProofStmt
     *
     *  @param rpnProofRightCol right column for RPN label
     */
    public void setRPNProofRightCol(int rpnProofRightCol) {
        this.rpnProofRightCol      = rpnProofRightCol;
    }

    /**
     *  Get right column number for RPN statement labels
     *  when creating ProofAsstWorksheet.GeneratedProofStmt
     *
     *  @return rpnProofRightCol right column or RPN label
     */
    public int getRPNProofRightCol() {
        return rpnProofRightCol;
    }

    /**
     *  Set on/off indicator instructing Proof Assistant
     *  to double-check every proof steps generated proof
     *  tree using the Proof Engine (mmj.verify.VerifyProofs.java).
     *
     *  @param recheckProofAsstUsingProofVerifier
     */
    public void setRecheckProofAsstUsingProofVerifier(
                   boolean recheckProofAsstUsingProofVerifier) {
        this.recheckProofAsstUsingProofVerifier
                                  =
             recheckProofAsstUsingProofVerifier;
    }

    /**
     *  Get on/off indicator instructing Proof Assistant
     *  to double-check every proof steps generated proof
     *  tree using the Proof Engine (mmj.verify.VerifyProofs.java).
     *
     *  @return recheckProofAsstUsingProofVerifier
     */
    public boolean getRecheckProofAsstUsingProofVerifier() {
        return recheckProofAsstUsingProofVerifier;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Export
     *  to use unified or "un-unified" format for exported proofs.
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @param exportFormatUnified yes/no.
     */
    public void setExportFormatUnified(
                   boolean exportFormatUnified) {
        this.exportFormatUnified =
             exportFormatUnified;
    }

    /**
     *  Get on/off indicator instructing the Proof Assistant Export
     *  to use unified or "un-unified" format for exported proofs.
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @return exportFormatUnified yes/no.
     */
    public boolean getExportFormatUnified() {
        return exportFormatUnified;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Export
     *  to "Randomize" or "NotRandomize" the output proof step
     *  logical hypotheses (a testing feature for Proof Assistant.)
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @param exportHypsRandomized yes/no.
     */
    public void setExportHypsRandomized(
                   boolean exportHypsRandomized) {
        this.exportHypsRandomized =
             exportHypsRandomized;
    }

    /**
     *  Get on/off indicator instructing the Proof Assistant Export
     *  to export "Randomized" or "NotRandomized" logical hypotheses
     *  on proof steps.
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @return ExportHypsRandomized
     */
    public boolean getExportHypsRandomized() {
        return exportHypsRandomized;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Export
     *  to output blank formulas -- or not -- for non-qed
     *  derivation steps (not logical hyps).
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @param exportDeriveFormulas yes/no.
     */
    public void setExportDeriveFormulas(
                   boolean exportDeriveFormulas) {
        this.exportDeriveFormulas =
             exportDeriveFormulas;
    }

    /**
     *  Get on/off indicator instructing the Proof Assistant Export
     *  to output blank formulas -- or not -- for non-qed
     *  derivation steps (not logical hyps).
     *  <p>
     *  Note: this applies to exported proofs written via
     *        ProofAsst.exportToFile, which is triggered via
     *        BatchMMJ2 "RunParm ProofAsstExportToFile" as
     *        well as the "ProofAsstBatchTest" (the latter when
     *        no input file is specified and an "export to
     *        memory" is implicitly requested.)
     *
     *  @return exportDeriveFormulas
     */
    public boolean getExportDeriveFormulas() {
        return exportDeriveFormulas;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Batch
     *  Test Import to compare generated Dj Vars with the originals.
     *  <p>
     *
     *  @param importCompareDJs yes/no.
     */
    public void setImportCompareDJs(
                   boolean importCompareDJs) {
        this.importCompareDJs =
             importCompareDJs;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Batch
     *  Test Import to compare generated Dj Vars with the originals.
     *  <p>
     *
     *  @return importCompareDJs
     */
    public boolean getImportCompareDJs() {
        return importCompareDJs;
    }


    /**
     *  Set on/off indicator instructing the Proof Assistant Batch
     *  Test Import to update the originals that are stored in
     *  memory (does not update the .mm file though.)
     *  <p>
     *
     *  @param importUpdateDJs yes/no.
     */
    public void setImportUpdateDJs(
                   boolean importUpdateDJs) {
        this.importUpdateDJs =
             importUpdateDJs;
    }

    /**
     *  Set on/off indicator instructing the Proof Assistant Batch
     *  Test Import to update the originals that are stored in
     *  memory (does not update the .mm file though.)
     *
     *  @return importUpdateDJs
     */
    public boolean getImportUpdateDJs() {
        return importUpdateDJs;
    }


    /**
     *  Set array of assertions that will be excluded from the
     *  proof unification search process.
     *  <p>
     *  This feature is primarily needed for redundant theorems
     *  that are carried in a Metamath database because they
     *  have a different proof (other possibilities exist.)
     *  <p>
     *
     *  @param unifySearchExclude array
     */
    public void setUnifySearchExclude(Assrt[] unifySearchExclude) {
        this.unifySearchExclude =
             unifySearchExclude;
    }

    /**
     *  Get array of assertions that will be excluded from the
     *  proof unification search process.
     *  <p>
     *  This feature is primarily needed for redundant theorems
     *  that are carried in a Metamath database because they
     *  have a different proof (other possibilities exist.)
     *  <p>
     *
     *  @return      unifySearchExclude array
     */
    public Assrt[] getUnifySearchExclude() {
        return unifySearchExclude;
    }

    /**
     *  Search array of assertions to see if a given assertion
     *  should be excluded from the unification search process.
     *  <p>
     *  Assuming that the number of exclusions is small, we're
     *  using an array. If the number were very large a
     *  hash table could be used, but the array is searched
     *  only during the first pass through the LogicalSystem
     *  Statement Table (see ProofUnifier.java).
     *  <p>
     *  @return      true if assertion should be excluded
     */
    public boolean checkUnifySearchExclude(Assrt assrt) {
        for (int i = 0; i < unifySearchExclude.length; i++) {
            if (assrt == unifySearchExclude[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Sets boolean value enabling or disabling
     *  "instrumentation" of the OutputCursor for
     *  regression testing.
     *
     *  @param outputCursorInstrumentation true or false.
     */
    public void setOutputCursorInstrumentation(
                        boolean outputCursorInstrumentation) {
        this.outputCursorInstrumentation
                                  = outputCursorInstrumentation;
    }

    /**
     *  Gets boolean value enabling or disabling
     *  "instrumentation" of the OutputCursor for
     *  regression testing.
     *
     *  @return outputCursorInstrumentation true or false.
     */
    public boolean getOutputCursorInstrumentation() {
        return outputCursorInstrumentation;
    }

    /**
     *  Sets boolean value enabling or disabling
     *  AutoReformat of proof step formulas after
     *  Work Variables are resolved.
     *
     *  @param autoReformat true or false.
     */
    public void setAutoReformat(
                        boolean autoReformat) {
        this.autoReformat         = autoReformat;
    }

    /**
     *  Gets boolean value enabling or disabling
     *  AutoReformat of proof step formulas after
     *  Work Variables are resolved.
     *
     *  @return autoReformat true or false.
     */
    public boolean getAutoReformat() {
        return autoReformat;
    }

    /**
     *  Sets boolean value enabling or disabling use of
     *  Undo/Redo Menu Items on the Proof Assistant GUI.
     *
     *  @param undoRedoEnabled true or false.
     */
    public void setUndoRedoEnabled(boolean undoRedoEnabled) {
        this.undoRedoEnabled      = undoRedoEnabled;
    }

    /**
     *  Gets boolean value enabling or disabling use of
     *  Undo/Redo Menu Items on the Proof Assistant GUI.
     *
     *  @return undoRedoEnabled true or false.
     */
    public boolean getUndoRedoEnabled() {
        return undoRedoEnabled;
    }


    /**
     *  Sets foreground color for Proof Asst GUI.
     *
     *  @param foregroundColor Color object
     */
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor      = foregroundColor;
    }

    /**
     *  Gets foreground color for Proof Asst GUI.
     *
     *  @return foregroundColor true or false.
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }


    /**
     *  Sets background color for Proof Asst GUI.
     *
     *  @param backgroundColor Color object
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor      = backgroundColor;
    }

    /**
     *  Gets background color for Proof Asst GUI.
     *
     *  @return backgroundColor true or false.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     *  Set TMFF Prefernces.
     *
     *  @param tmffPreferences instance of TMFFPreferences.
     */
    public void setTMFFPreferences(
                   TMFFPreferences tmffPreferences) {
        this.tmffPreferences      = tmffPreferences;
    }

    /**
     *  Get TMFF Preferences.
     *
     *  @return tmffPreferences instances.
     */
    public TMFFPreferences getTMFFPreferences() {
        return tmffPreferences;
    }

    /**
     *  Set WorkVarManager
     *
     *  @param workVarManager instance of WorkVarManager.
     */
    public void setWorkVarManager(
                   WorkVarManager workVarManager) {
        this.workVarManager      = workVarManager;
    }

    /**
     *  Get WorkVarManager.
     *
     *  @return workVarManager instance.
     */
    public WorkVarManager getWorkVarManager() {
        return workVarManager;
    }

    /**
     *  Set StepUnifier
     *
     *  @param stepUnifier instance of StepUnifier or null.
     */
    public void setStepUnifier(
                   StepUnifier stepUnifier) {
        this.stepUnifier      = stepUnifier;
    }

    /**
     *  Get StepUnifier.
     *
     *  @return stepUnifier instance.
     */
    public StepUnifier getStepUnifier() {
        return stepUnifier;
    }

    /**
     *  Get StepUnifier Instance.
     *
     *  @return stepUnifier instance.
     */
    public StepUnifier getStepUnifierInstance() {
        StepUnifier s             = getStepUnifier();
        if (s == null) {
            s                     =
                new StepUnifier(
                    getWorkVarManager());
            setStepUnifier(s);
        }
        return s;
    }

    /**
     *  A simple routine to build a list of all options for
     *  Soft Dj Vars error handling.
     *  <p>
     *  This routine is used by ProofAsstPreferences.
     *  <p>
     *  @return Soft Dj Vars Error List
     */
    public String getSoftDjErrorOptionListString() {

        StringBuffer sb           = new StringBuffer();

        for (int i = 0;
             i < PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE.length;
             i++) {
            sb.append(i + 1);
            sb.append(" - ");
            sb.append(
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE[i]);
            sb.append('\n');
        }

        sb.append('\n');

        return sb.toString();
    }


    /**
     *  A simple routine to build a list of all defined Fonts
     *  Families.
     *  <p>
     *  This routine is used by ProofAsstPreferences.
     *  <p>
     *  @return Font Family List String
     */
    public String getFontListString() {

        GraphicsEnvironment g     =
            GraphicsEnvironment.getLocalGraphicsEnvironment();

        Font[] f                  = g.getAllFonts();

        TreeSet t                 = new TreeSet();

        for (int i = 0; i < f.length; i++) {
            t.add(
                f[i].
                    getFamily());
        }

        StringBuffer sb           = new StringBuffer();

        int      lineLength       =
                    PaConstants.FONT_LIST_STARTING_LINE_LENGTH;
        int      maxNbrOfLines    =
                    PaConstants.FONT_LIST_MAX_LINES;

        int      loopCnt          = 0;
        int      lineCnt;
        int      lineMax;
        Iterator iterator;
        if (t.size() > 0) {

            loopA: while (true) {

                ++loopCnt;
                lineMax           = loopCnt * lineLength;
                sb.setLength(0);
                lineCnt           = 1;
                iterator          = t.iterator();
                sb.append((String)iterator.next());

                loopB: while (iterator.hasNext()) {

                    if (sb.length() > (lineMax * lineCnt)) {
                        sb.append('\n');
                        ++lineCnt;
                        if (lineCnt > maxNbrOfLines) {
                            continue loopA;
                        }
                    }
                    else {
                        sb.append(", ");
                    }
                    sb.append((String)iterator.next());
                    continue loopB;
                }
                break loopA;
            }
        }
        else {
            sb.append(" ");
        }

        sb.append('\n');

        return sb.toString();
    }

    /**
     *  A stupid routine to validate a Font Family Name.
     *  <p>
     *  This routine is used by ProofAsstPreferences.
     *  <p>
     *  @param familyName font family name, which must be
     *         available in GraphicsEnvironment.getAllFonts().
     *
     *  @return Family Name adjust for cap/lower variations.
     *  @throws ProofAsstException if input familyName not
     *          installed in the system.
     */
    public String validateFontFamily(String familyName)
                        throws ProofAsstException {

        String n;
        if (familyName == null) {
            n                     = new String(" ");
        }
        else {
            n                     = familyName.trim();
        }

        Font[] f                  =
            GraphicsEnvironment.
                getLocalGraphicsEnvironment().
                    getAllFonts();

        for (int i = 0; i < f.length; i++) {
            if (f[i].
                    getFamily().
                        compareToIgnoreCase(
                            n)
                == 0) {
                return f[i].getFamily();
            }
        }
        throw new ProofAsstException(
            PaConstants.ERRMSG_INVALID_FONT_FAMILY_NAME_1
                + familyName);
    }


    /**
     *  A stupid routine to validate the entered number
     *  indicating a Dj Vars Soft Error Option.
     *  <p>
     *  The entered number, minus 1, is looked up in
     *  PaConstants.PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE
     *  using the option number as an index.
     *  <p>
     *  This routine is used by ProofAsstPreferences.
     *  <p>
     *  @param option number corresponding to Dj Vars Soft
     *                Error Option name
     *
     *  @return Dj Vars Soft Error Option Name String
     *  @throws ProofAsstException if input option number
     *          is out of range or is not a number.
     */
    public String validateDjVarsSoftErrorsOptionNbr(
                                            String option)
                        throws ProofAsstException {

        int n                     = -1;
        if (option != null) {
            try {
                n                 = Integer.parseInt(option);
            }
            catch (NumberFormatException e) {
            }
        }
        else {
            option = "";
        }

        if (n < 1
            ||
            n > PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE.length) {

            throw new ProofAsstException(
                PaConstants.
                    ERRMSG_INVALID_SOFT_DJ_ERROR_OPTION_NBR
                + option);
        }

        return PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE[
                        n - 1];
    }


    public boolean getDjVarsSoftErrorsIgnore() {
        return djVarsSoftErrorsIgnore;
    }
    public boolean getDjVarsSoftErrorsReport() {
        return djVarsSoftErrorsReport;
    }
    public boolean getDjVarsSoftErrorsGenerate() {
        return djVarsSoftErrorsGenerate;
    }
    public boolean getDjVarsSoftErrorsGenerateNew() {
        return djVarsSoftErrorsGenerateNew;
    }
    public boolean getDjVarsSoftErrorsGenerateRepl() {
        return djVarsSoftErrorsGenerateRepl;
    }
    public boolean getDjVarsSoftErrorsGenerateDiffs() {
        return djVarsSoftErrorsGenerateDiffs;
    }


    public String getDjVarsSoftErrorsOptionNbr() {
        String s                  =
            getDjVarsSoftErrorsOption();
        for (int i = 0;
             i < PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE.length;
             i++) {
            if (s.compareTo(
                    PaConstants.
                        PROOF_ASST_DJ_VARS_SOFT_ERRORS_TABLE[i])
                == 0) {
                return Integer.toString(i + 1);
            }
        }
        throw new IllegalArgumentException("");
    }



    public String getDjVarsSoftErrorsOption() {
        if (djVarsSoftErrorsIgnore) {
            return
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_IGNORE;
        }
        if (djVarsSoftErrorsReport) {
            return
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_REPORT;
        }
        if (djVarsSoftErrorsGenerateNew) {
            return
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_NEW;
        }
        if (djVarsSoftErrorsGenerateRepl) {
            return
              PaConstants.
                PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_REPLACEMENTS;
        }
        if (djVarsSoftErrorsGenerateDiffs) {
            return
              PaConstants.
                PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_DIFFERENCES;
        }

        throw new IllegalArgumentException("");
    }


    public boolean setDjVarsSoftErrorsOption(String s) {
        if (s == null) {
            return false; //error
        }

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_REPLACEMENTS)
            == 0) {

            djVarsSoftErrorsIgnore
                                  = false;
            djVarsSoftErrorsReport
                                  = false;

            djVarsSoftErrorsGenerate
                                  = true;
            djVarsSoftErrorsGenerateNew
                                  = false;
            djVarsSoftErrorsGenerateRepl
                                  = true;
            djVarsSoftErrorsGenerateDiffs
                                  = false;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_NEW)
            == 0) {

            djVarsSoftErrorsIgnore
                                  = false;
            djVarsSoftErrorsReport
                                  = false;

            djVarsSoftErrorsGenerate
                                  = true;
            djVarsSoftErrorsGenerateNew
                                  = true;
            djVarsSoftErrorsGenerateRepl
                                  = false;
            djVarsSoftErrorsGenerateDiffs
                                  = false;
            return true; //no error
        }


        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_DJ_VARS_SOFT_ERRORS_GENERATE_DIFFERENCES)
            == 0) {

            djVarsSoftErrorsIgnore
                                  = false;
            djVarsSoftErrorsReport
                                  = false;

            djVarsSoftErrorsGenerate
                                  = true;
            djVarsSoftErrorsGenerateNew
                                  = false;
            djVarsSoftErrorsGenerateRepl
                                  = false;
            djVarsSoftErrorsGenerateDiffs
                                  = true;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_REPORT)
            == 0) {

            djVarsSoftErrorsIgnore
                                  = false;
            djVarsSoftErrorsReport
                                  = true;

            djVarsSoftErrorsGenerate
                                  = false;
            djVarsSoftErrorsGenerateNew
                                  = false;
            djVarsSoftErrorsGenerateRepl
                                  = false;
            djVarsSoftErrorsGenerateDiffs
                                  = false;

            return true; //no error
        }

        if (s.compareToIgnoreCase(
                PaConstants.
                    PROOF_ASST_DJ_VARS_SOFT_ERRORS_IGNORE)
            == 0) {

            djVarsSoftErrorsIgnore
                                  = true;
            djVarsSoftErrorsReport
                                  = false;

            djVarsSoftErrorsGenerate
                                  = false;
            djVarsSoftErrorsGenerateNew
                                  = false;
            djVarsSoftErrorsGenerateRepl
                                  = false;
            djVarsSoftErrorsGenerateDiffs
                                  = false;

            return true; //no error
        }

        return false;
    }

    /**
     *  A stupid routine to validate the entered number
     *  indicating an Incomplete Step Cursor Option.
     *  <p>
     *  The entered number, minus 1, is looked up in
     *  PaConstants.PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE
     *  using the option number as an index.
     *  <p>
     *  @param option number corresponding to Incomplete
     *                Step Cursor option name
     *
     *  @return Incomplete Step Cursor Option Name String
     *  @throws ProofAsstException if input option number
     *          is out of range or is not a number.
     */
    public String validateIncompleteStepCursorOptionNbr(
                                            String option)
                        throws ProofAsstException {

        int n                     = -1;
        if (option != null) {
            try {
                n                 = Integer.parseInt(option);
            }
            catch (NumberFormatException e) {
            }
        }
        else {
            option = "";
        }

        if (n < 1
            ||
            n > PaConstants.
                    PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE.length) {

            throw new ProofAsstException(
                PaConstants.
                    ERRMSG_INVALID_INCOMPLETE_STEP_CURSOR_OPTION_NBR
                + option);
        }

        return PaConstants.
                    PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE[
                        n - 1];
    }



    /**
     *  Validates ProofAsstIncompleteStepCursor option
     *  and updates.
     *
     *  @param s either "First", "Last" or "AsIs".
     *  @return true if valid otherwise false.
     */
    public boolean setIncompleteStepCursor(String s) {
        if (s == null) {
            return false; //error
        }

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_INCOMPLETE_STEP_CURSOR_FIRST)
            == 0) {

            incompleteStepCursor  = s;
            incompleteStepCursorFirst
                                  = true;
            incompleteStepCursorLast
                                  = false;
            incompleteStepCursorAsIs
                                  = false;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_INCOMPLETE_STEP_CURSOR_LAST)
            == 0) {

            incompleteStepCursor  = s;
            incompleteStepCursorFirst
                                  = false;
            incompleteStepCursorLast
                                  = true;
            incompleteStepCursorAsIs
                                  = false;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
              PaConstants.
                PROOF_ASST_INCOMPLETE_STEP_CURSOR_ASIS)
            == 0) {

            incompleteStepCursor  = s;
            incompleteStepCursorFirst
                                  = false;
            incompleteStepCursorLast
                                  = false;
            incompleteStepCursorAsIs
                                  = true;
            return true; //no error
        }

        return false;
    }

    /**
     *  Get incompleteStepCursor parameter.
     *
     *  @return incompleteStepCursor parameter.
     */
    public String getIncompleteStepCursor() {
        return incompleteStepCursor;
    }


    /**
     *  Get incompleteStepCursorFirst parameter.
     *
     *  @return incompleteStepCursorFirst parameter.
     */
    public boolean getIncompleteStepCursorFirst() {
        return incompleteStepCursorFirst;
    }

    /**
     *  Get incompleteStepCursorLast parameter.
     *
     *  @return incompleteStepCursorLast parameter.
     */
    public boolean getIncompleteStepCursorLast() {
        return incompleteStepCursorLast;
    }

    /**
     *  Get incompleteStepCursorAsIs parameter.
     *
     *  @return incompleteStepCursorAsIs parameter.
     */
    public boolean getIncompleteStepCursorAsIs() {
        return incompleteStepCursorAsIs;
    }

    /**
     *  Get current incompleteStepCursor option number.
     *
     *  @return incompleteStepCursor option number.
     */
    public String getIncompleteStepCursorOptionNbr() {
        String s                  =
            getIncompleteStepCursor();
        for (int i = 0;
             i < PaConstants.
                    PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE.length;
             i++) {
            if (s.compareTo(
                    PaConstants.
                        PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE[i])
                == 0) {
                return Integer.toString(i + 1);
            }
        }
        throw new IllegalArgumentException("");
    }

    /**
     *  A simple routine to build a list of all options for
     *  Incomplete Step Cursor options.
     *  <p>
     *  @return Incomplete Step Cursor option list string.
     */
    public String getIncompleteStepCursorOptionListString() {

        StringBuffer sb           = new StringBuffer();

        for (int i = 0;
             i < PaConstants.
                    PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE.length;
             i++) {
            sb.append(i + 1);
            sb.append(" - ");
            sb.append(
                PaConstants.
                    PROOF_ASST_INCOMPLETE_STEP_CURSOR_TABLE[i]);
            sb.append('\n');
        }

        sb.append('\n');

        return sb.toString();
    }


    /**
     *  A stupid routine to validate StepSelectorMaxResults.
     *  <p>
     *  This routine is used by ProofAssistantEditor.
     *  <p>
     *  @param maxResultsString integer max results for
     *         StepSelectorSearch.
     *
     *  @return maxResults number.
     *  @throws ProofAsstException if input maxResults invalid.
     */
    public int validateStepSelectorMaxResults(
                            String maxResultsString)
                        throws IllegalArgumentException {

        int n                     = -1;
        if (maxResultsString != null) {
            try {
                n                 =
                    Integer.
                        parseInt(
                            maxResultsString);
            }
            catch (NumberFormatException e) {
            }
        }

        if (n < 1
            ||
            n > PaConstants.
                    STEP_SELECTOR_MAX_RESULTS_MAXIMUM) {

            throw new IllegalArgumentException(
                PaConstants.
                    ERRMSG_INVALID_STEP_SELECTOR_MAX_RESULTS_NBR_1
                + maxResultsString
                + PaConstants.
                    ERRMSG_INVALID_STEP_SELECTOR_MAX_RESULTS_NBR_2
                + PaConstants.
                    STEP_SELECTOR_MAX_RESULTS_MAXIMUM
                );
        }

        return n;
    }

    /**
     *  Sets maximum number of StepSelector Results.
     *
     *  @param stepSelectorMaxResults number
     */
    public void setStepSelectorMaxResults(
                                int stepSelectorMaxResults) {
        this.stepSelectorMaxResults
                                  = stepSelectorMaxResults;
    }

    /**
     *  Gets maximum number of StepSelector Results.
     *
     *  @return stepSelectorMaxResults number
     */
    public int getStepSelectorMaxResults() {
        return stepSelectorMaxResults;
    }

    /**
     *  A stupid routine to validate
     *  StepSelectorShowSubstitutions.
     *  <p>
     *  This routine is used by ProofAssistantEditor.
     *  <p>
     *  @param showSubstitutionsString yes or no or true
     *            or false or on or off.
     *  @return boolean true or false
     *  @throws ProofAsstException if invalid value.
     */
    public boolean validateStepSelectorShowSubstitutions(
                                 String showSubstitutionsString)
                        throws IllegalArgumentException {
        String s;
        if (showSubstitutionsString != null) {
            s                     =
                showSubstitutionsString.trim().toLowerCase();
            if (s.equals(PaConstants.SYNONYM_TRUE_1) ||
                s.equals(PaConstants.SYNONYM_TRUE_2) ||
                s.equals(PaConstants.SYNONYM_TRUE_3)) {
                return true;
            }
            if (s.equals(PaConstants.SYNONYM_FALSE_1) ||
                s.equals(PaConstants.SYNONYM_FALSE_2) ||
                s.equals(PaConstants.SYNONYM_FALSE_3)) {
                return false;
            }
        }
        else {
            s                     = " ";
        }

        throw new IllegalArgumentException(
            PaConstants.
                ERRMSG_INVALID_STEP_SELECTOR_SHOW_SUBSTITUTIONS_1
            + s
            + PaConstants.
                ERRMSG_INVALID_STEP_SELECTOR_SHOW_SUBSTITUTIONS_2
            );
    }

    /**
     *  Sets StepSelectorShowSubstitutions RunParm option.
     *
     *  @param stepSelectorShowSubstitutions option.
     */
    public void setStepSelectorShowSubstitutions(
                        boolean stepSelectorShowSubstitutions) {
        this.stepSelectorShowSubstitutions
                                  = stepSelectorShowSubstitutions;
    }

    /**
     *  Gets StepSelectorShowSubstitutions RunParm option.
     *
     *  @return stepSelectorShowSubstitutions option
     */
    public boolean getStepSelectorShowSubstitutions() {
        return stepSelectorShowSubstitutions;
    }

    /**
     *  Sets StepSelectorDialogPaneWidth RunParm option.
     *
     *  @param stepSelectorDialogPaneWidth option.
     */
    public void setStepSelectorDialogPaneWidth(
                        int stepSelectorDialogPaneWidth) {
        this.stepSelectorDialogPaneWidth
                                  = stepSelectorDialogPaneWidth;
    }

    /**
     *  Gets StepSelectorDialogPaneWidth RunParm option.
     *
     *  @return stepSelectorDialogPaneWidth option
     */
    public int getStepSelectorDialogPaneWidth() {
        return stepSelectorDialogPaneWidth;
    }

    /**
     *  Sets StepSelectorDialogPaneHeight RunParm option.
     *
     *  @param stepSelectorDialogPaneHeight option.
     */
    public void setStepSelectorDialogPaneHeight(
                        int stepSelectorDialogPaneHeight) {
        this.stepSelectorDialogPaneHeight
                                  = stepSelectorDialogPaneHeight;
    }

    /**
     *  Gets StepSelectorDialogPaneHeight RunParm option.
     *
     *  @return stepSelectorDialogPaneHeight option
     */
    public int getStepSelectorDialogPaneHeight() {
        return stepSelectorDialogPaneHeight;
    }

    /**
     *  Sets ProofAsstAssrtListFreespace RunParm option.
     *
     *  @param assrtListFreespace option.
     */
    public void setAssrtListFreespace(
                        int assrtListFreespace) {
        this.assrtListFreespace
                                  = assrtListFreespace;
    }

    /**
     *  Gets ProofAsstAssrtListFreespace RunParm option.
     *
     *  @return proofAsstAssrtListFreespace option
     */
    public int getAssrtListFreespace() {
        return assrtListFreespace;
    }
}

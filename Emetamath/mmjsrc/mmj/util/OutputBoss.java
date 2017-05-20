//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  OutputBoss.java  0.07 08/01/2008
 *
 *  22-Jan-2006 --> added sysOutPrint and sysErrPrint
 *                  for use by Proof Assistant.
 *
 *  Version 0.04 Sep-03-2006:
 *      -->Add TMFF stuff
 *
 *  Version 0.05 06/01/2007:
 *      -->Add OutputVerbosity RunParm
 *      -->Add StartInstrumentationTimer and
 *         StopInstrumentationTimer.
 *
 *  Version 0.06 11/01/2007:
 *      -->Fix bug: MaxErrorMessages and MaxInfoMessages
 *         parms not taking effect correctly!
 *
 *  Version 0.07 08/01/2008:
 *      -->Add new Print commands for mmj.lang.BookManager:
 *             - PrintBookManagerChapters
 *             - PrintBookManagerSections
 *             - PrintBookManagerSectionDetails
 */

package mmj.util;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import mmj.lang.BookManager;
import mmj.lang.LangConstants;
import mmj.lang.LogicalSystem;
import mmj.lang.Messages;
import mmj.lang.Section;
import mmj.lang.Stmt;
import mmj.lang.VerifyException;
import mmj.mmio.MMIOException;
import mmj.tmff.TMFFPreferences;
import mmj.verify.Grammar;

/**
 *  Responsible for managing and using Messages, Dump and
 *  writing to sysOut/sysErr.
 *  <p>
 *  OutputBoss' main responsibility is directing output to
 *  the user-designated destination, so it provides its own
 *  "print-and-clear-messages" function for the other Boss
 *  classes to use.
 *  <p>
 *  A key point to note is that in BatchMMJ2, Messages are
 *  printed and cleared immediately after being generated, they
 *  are not accumulated for some later purpose. Therefore,
 *  OutputBoss uses messages.reallocateInfoMessages and
 *  messages.reallocateErrorMessages when a MaxErrorMessages
 *  or MaxInfoMessages runparm is changed. It also uses
 *  LangConstants.MAX_ERROR_MESSAGES_DEFAULT and
 *  LangConstants.MAX_INFO_MESSAGES_DEFAULT if the
 *  relevant runParms are *not* input.
 */
public class OutputBoss extends Boss {

    protected PrintWriter sysOut;
    protected PrintWriter sysErr;

    protected int         maxErrorMessagesParm;
    protected int         maxInfoMessagesParm;
    protected Messages    messages;

    protected int         maxStatementPrintCountParm;
    protected String      captionParm;
    protected Dump        dump;

    protected int         outputVerbosityParm
                                  =
        UtilConstants.OUTPUT_VERBOSITY_DEFAULT;

    /**
     *  Constructor with BatchFramework for access to environment.
     *
     *  @param batchFramework for access to environment.
     */
    public OutputBoss(BatchFramework batchFramework) {
        super(batchFramework);
    }

    /**
     *  Executes a single command from the RunParmFile.
     *
     *  @param runParm the RunParmFile line to execute.
     */
    public boolean doRunParmCommand(
                             RunParmArrayEntry runParm)
                        throws IllegalArgumentException,
                               MMIOException,
                               FileNotFoundException,
                               IOException,
                               VerifyException {

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_CLEAR)
            == 0) {
            maxErrorMessagesParm
                                  = 0;
            maxInfoMessagesParm
                                  = 0;
            messages              = null;
            maxStatementPrintCountParm
                                  = 0;
            dump                  = null;
            outputVerbosityParm   =
                UtilConstants.OUTPUT_VERBOSITY_DEFAULT;

            return false;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_SYSOUT_FILE)
            == 0) {
            editSysOutFile(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_SYSERR_FILE)
            == 0) {
            editSysErrFile(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_MAX_ERROR_MESSAGES)
            == 0) {
            editMaxErrorMessages(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_MAX_INFO_MESSAGES)
            == 0) {
            editMaxInfoMessages(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_MAX_STATEMENT_PRINT_COUNT)
            == 0) {
            editMaxStatementPrintCount(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_CAPTION)
            == 0) {
            editCaption(runParm);
            return true;
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_PRINT_SYNTAX_DETAILS)
            == 0) {
            doPrintSyntaxDetails(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_PRINT_STATEMENT_DETAILS)
            == 0) {
            doPrintStatementDetails(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_OUTPUT_VERBOSITY)
            == 0) {
            editOutputVerbosity(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_START_INSTRUMENTATION_TIMER)
            == 0) {
            editStartInstrumentationTimer(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_STOP_INSTRUMENTATION_TIMER)
            == 0) {
            editStopInstrumentationTimer(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_PRINT_BOOK_MANAGER_CHAPTERS)
            == 0) {
            doPrintBookManagerChapters(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_PRINT_BOOK_MANAGER_SECTIONS)
            == 0) {
            doPrintBookManagerSections(runParm);
            return true; // "consumed"
        }

        if (runParm.name.compareToIgnoreCase(
            UtilConstants.RUNPARM_PRINT_BOOK_MANAGER_SECTION_DETAILS)
            == 0) {
            doPrintBookManagerSectionDetails(runParm);
            return true; // "consumed"
        }


        return false;
    }

    /**
     *  Print all error/info messages, then clear the messages
     *  from the Messages repository.
     */
    public void printAndClearMessages() {
        if (sysOut == null) {
            getMessages().printAndClearMessages();
        }
        else {
            getMessages().writeAndClearMessages(sysOut);
        }
    }

    /**
     *  Get a Messages object.
     *
     *  @return Messages object, ready to go.
     */
    public Messages getMessages() {
        if (messages == null) {
            initializeMessages();
        }
        return messages;
    }

    /**
     *  Common routine for printing a line to SysOut
     *  if the input verbosity number is less than
     *  or equal to the OutputVerbosity RunParm
     *
     *  @param s line to print.
     *  @param v verbosity of line to print.
     */
    public void sysOutPrintln(String s,
                              int    v)
                        throws IOException {
        if (v > outputVerbosityParm) {
        }
        else {
            sysOutPrintln(s);
        }

    }


    /**
     *  Common routine for printing a line to SysOut.
     *
     *  @param s line to print.
     */
    public void sysOutPrintln(String s)
                        throws IOException {
        if (sysOut == null) {
            System.out.println(s);
        }
        else {
            sysOut.println(s);
            checkSysOutError();
        }
    }

    /**
     *  Common routine for printing to SysOut.
     *  if the input verbosity number is less than
     *  or equal to the OutputVerbosity RunParm
     *
     *  @param s string to print.
     *  @param v verbosity of string to print.
     *
     */
    public void sysOutPrint(String s,
                            int    v)
                        throws IOException {

        if (v > outputVerbosityParm) {
        }
        else {
            sysOutPrint(s);
        }
    }


    /**
     *  Common routine for printing to SysOut.
     *
     *  @param s string to print.
     */
    public void sysOutPrint(String s)
                        throws IOException {
        if (sysOut == null) {
            System.out.print(s);
        }
        else {
            sysOut.print(s);
            checkSysOutError();
        }
    }

    /**
     *  Common routine for printing a line to SysErr.
     *
     *  @param s line to print.
     */
    public void sysErrPrintln(String s)
                        throws IOException {
        if (sysErr == null) {
            System.err.println(s);
        }
        else {
            sysErr.println(s);
            checkSysErrError();
        }
    }

    /**
     *  Common routine for printing to SysErr.
     *
     *  @param s String to print.
     */
    public void sysErrPrint(String s)
                        throws IOException {
        if (sysErr == null) {
            System.err.print(s);
        }
        else {
            sysErr.print(s);
            checkSysErrError();
        }
    }

    /**
     *  Close SysOut and SysErr.
     */
    public void close() {
        closeSysOut();
        closeSysErr();
    }


    /**
     *  Executes the PrintSyntaxDetails command, prints any
     *  messages, etc.
     *
     *  @param runParm RunParmFile line.
     */
    public void doPrintSyntaxDetails(RunParmArrayEntry runParm)
                            throws IllegalArgumentException {

        LogicalSystem logicalSystem
                              =
            batchFramework.logicalSystemBoss.getLogicalSystem();

        Grammar       grammar =
            batchFramework.grammarBoss.getGrammar();

        Dump d                = getDump();
        d.printSyntaxDetails(getCaption(),
                             logicalSystem,
                             grammar);

    }

    /**
     *  Executes the PrintStatementDetails command, prints any
     *  messages, etc.
     *
     *  @param runParm RunParmFile line.
     */
    public void doPrintStatementDetails(RunParmArrayEntry runParm)
                            throws IllegalArgumentException {

        LogicalSystem logicalSystem
                              =
            batchFramework.logicalSystemBoss.getLogicalSystem();

        Dump d                = getDump();

        String optionValue    = runParm.values[0].trim();
        if (optionValue.compareTo(
              UtilConstants.RUNPARM_OPTION_VALUE_ALL)
            == 0) {
            int  n            = maxStatementPrintCountParm;
            if (n <= 0) {
                n             =
                    UtilConstants.MAX_STATEMENT_PRINT_COUNT_DEFAULT;
            }
            d.printStatementDetails(getCaption(),
                                    logicalSystem.getStmtTbl(),
                                    n);
        }
        else {
            Stmt stmt =
                editRunParmValueStmt(
                    optionValue,
                    UtilConstants.RUNPARM_PRINT_STATEMENT_DETAILS,
                    logicalSystem);
            d.printOneStatementDetails(stmt);
        }
        batchFramework.outputBoss.printAndClearMessages();
    }

    /**
     *  Executes the PrintBookManagerChapters command.
     *
     *  @param runParm RunParmFile line.
     */
    public void doPrintBookManagerChapters(RunParmArrayEntry runParm)
                            throws IllegalArgumentException {

        LogicalSystem logicalSystem
                              =
            batchFramework.logicalSystemBoss.getLogicalSystem();

        BookManager bookManager
                              =
            checkBookManagerReady(
                runParm,
                UtilConstants.
                    RUNPARM_PRINT_BOOK_MANAGER_CHAPTERS,
                logicalSystem);

        Dump d                = getDump();

        d.printBookManagerChapters(
                    UtilConstants.
                        RUNPARM_PRINT_BOOK_MANAGER_CHAPTERS,
                    bookManager);

        batchFramework.outputBoss.printAndClearMessages();
    }

    /**
     *  Executes the PrintBookManagerSections command.
     *
     *  @param runParm RunParmFile line.
     */
    public void doPrintBookManagerSections(RunParmArrayEntry runParm)
                            throws IllegalArgumentException {

        LogicalSystem logicalSystem
                              =
            batchFramework.logicalSystemBoss.getLogicalSystem();

        BookManager bookManager
                              =
            checkBookManagerReady(
                runParm,
                UtilConstants.
                    RUNPARM_PRINT_BOOK_MANAGER_SECTIONS,
                logicalSystem);

        Dump d                = getDump();

        d.printBookManagerSections(
            UtilConstants.
                RUNPARM_PRINT_BOOK_MANAGER_SECTIONS,
            bookManager);

        batchFramework.outputBoss.printAndClearMessages();
    }

    /**
     *  Executes the PrintBookManagerSectionDetails command
     *
     *  @param runParm RunParmFile line.
     */
    public void doPrintBookManagerSectionDetails(
                                        RunParmArrayEntry runParm)
                            throws IllegalArgumentException {

        LogicalSystem logicalSystem
                              =
            batchFramework.logicalSystemBoss.getLogicalSystem();

        BookManager bookManager
                              =
            checkBookManagerReady(
                runParm,
                UtilConstants.
                    RUNPARM_PRINT_BOOK_MANAGER_SECTION_DETAILS,
                logicalSystem);

        Dump d                = getDump();

        Section section       = null;
        String optionValue    = runParm.values[0].trim();
        if (optionValue.compareTo(
              UtilConstants.RUNPARM_OPTION_VALUE_ALL)
            != 0) {
            section           =
                editBookManagerSectionNbr(
                    runParm,
                    UtilConstants.
                        RUNPARM_PRINT_BOOK_MANAGER_SECTION_DETAILS,
                    1,
                    bookManager);
        }

        d.printBookManagerSectionDetails(
                    runParm,
                    logicalSystem,
                    bookManager,
                    section);

        batchFramework.outputBoss.printAndClearMessages();
    }

    /**
     *  Get a Dump object.
     *
     *  @return Dump object, ready to go.
     */
    public Dump getDump() {
        if (dump == null) {
            dump = new Dump(sysOut);
        }
        else {
            dump.setSysOut(sysOut);
        }

        TMFFPreferences tmffPreferences
                                  =
            batchFramework.tmffBoss.getTMFFPreferences();

        dump.setTMFFPreferences(tmffPreferences);

        return dump;
    }

    /**
     *  Validate System Output File Runparm.
     */
    protected void editSysOutFile(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        closeSysOut();
        sysOut                = null;
        sysOut                =
            editPrintWriterRunParm(
                     runParm,
                     UtilConstants.RUNPARM_SYSOUT_FILE);

    }

    /**
     *  Validate System Error File Runparm.
     */
    protected void editSysErrFile(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        closeSysErr();
        sysErr                = null;
        sysErr                =
            editPrintWriterRunParm(
                     runParm,
                     UtilConstants.RUNPARM_SYSERR_FILE);

    }


    /**
     *  Validate Max Error Messages Runparm.
     */
    protected void editMaxErrorMessages(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        maxErrorMessagesParm =
            editRunParmValueReqPosInt(
            runParm,
            UtilConstants.RUNPARM_MAX_ERROR_MESSAGES,
            1);
        if (messages != null) {
            printAndClearMessages();
            messages.
                reallocateErrorMessages(
                    maxErrorMessagesParm);
        }
    }

    /**
     *  Validate Max Info Messages Runparm.
     */
    protected void editMaxInfoMessages(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        maxInfoMessagesParm =
            editRunParmValueReqPosInt(
            runParm,
            UtilConstants.RUNPARM_MAX_INFO_MESSAGES,
            1);
        if (messages != null) {
            printAndClearMessages();
            messages.
                reallocateInfoMessages(
                    maxInfoMessagesParm);
        }
    }

    /**
     *  Validate Caption Runparm.
     */
    protected void editCaption(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        editRunParmValuesLength(
                 runParm,
                 UtilConstants.RUNPARM_CAPTION,
                 1);
        captionParm           =
                runParm.values[0].trim();
        if (captionParm.length() == 0) {
            captionParm       = new String(" ");
        }
    }

    /**
     *  Get Caption Parm Option.
     *
     *  @return Caption string.
     */
    protected String getCaption() {
        if (captionParm == null) {
            return new String(" ");
        }
        else {
            return captionParm;
        }
    }

    /**
     *  Validate Max Statement Print Count RunParm.
     */
    protected void editMaxStatementPrintCount(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        maxStatementPrintCountParm =
            editRunParmValueReqPosInt(
            runParm,
            UtilConstants.RUNPARM_MAX_STATEMENT_PRINT_COUNT,
            1);
    }

    /**
     *  Validate OutputVerbosity Runparm.
     */
    protected void editOutputVerbosity(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        outputVerbosityParm =
            editRunParmValueReqInt(
            runParm,
            UtilConstants.RUNPARM_OUTPUT_VERBOSITY,
            1);
    }


    /**
     *  Validate StartInstrumentationTimer Runparm.
     */
    protected void editStartInstrumentationTimer(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        editRunParmValuesLength(
                 runParm,
                 UtilConstants.RUNPARM_START_INSTRUMENTATION_TIMER,
                 1);

        getMessages().
            startInstrumentationTimer(runParm.values[0]);
    }


    /**
     *  Validate StopInstrumentationTimer Runparm.
     */
    protected void editStopInstrumentationTimer(
                        RunParmArrayEntry runParm)
            throws IllegalArgumentException {

        editRunParmValuesLength(
                 runParm,
                 UtilConstants.RUNPARM_STOP_INSTRUMENTATION_TIMER,
                 1);

        getMessages().
            stopInstrumentationTimer(runParm.values[0]);
        printAndClearMessages();
    }

    /**
     *  Checks to see if BookManager is initialized and enabled.
     *  <p>
     *  Caution: throws IllegalArgumentException if BookManager
     *           is not enabled! Ouch.
     *  <p>
     *  @param runParm RunParmFile line.
     *  @param valueCaption String identifying RunParm value
     *  @param logicalSystem the LogicalSystem in use.
     *  @return BookManager object in enabled status.
     */
    protected BookManager checkBookManagerReady(
                                RunParmArrayEntry runParm,
                                String            valueCaption,
                                LogicalSystem     logicalSystem) {

        BookManager bookManager   = logicalSystem.getBookManager();
        if (!bookManager.isEnabled()) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_BOOK_MANAGER_NOT_ENABLED_1
                + valueCaption
                + UtilConstants.ERRMSG_BOOK_MANAGER_NOT_ENABLED_2);
        }
        return bookManager;
    }

    /**
     *  Checks to see if BookManager is initialized and enabled.
     *  <p>
     *  Caution: throws IllegalArgumentException if Section Number
     *           is not a positive interer, or if it is not
     *           found within the BookManager! Ouch.
     *  <p>
     *  @param runParm RunParmFile line.
     *  @param valueCaption String identifying RunParm value
     *  @param valueFieldNbr number of value field within RunParm.
     *  @return Section BookManager section.
     */
    protected Section editBookManagerSectionNbr(
                                    RunParmArrayEntry runParm,
                                    String            valueCaption,
                                    int               valueFieldNbr,
                                    BookManager       bookManager) {

        int sectionNbr            =
                editRunParmValueReqPosInt(
                    runParm,
                    valueCaption,
                    valueFieldNbr);

        Section section           =
            bookManager.getSection(sectionNbr);
        if (section == null) {
            throw new IllegalArgumentException(
                UtilConstants.
                    ERRMSG_BOOK_MANAGER_SECTION_NBR_NOT_FOUND_1
                + sectionNbr
                + UtilConstants.
                    ERRMSG_BOOK_MANAGER_SECTION_NBR_NOT_FOUND_2
                + valueCaption);
        }
        return section;
    }

    /**
     *  Initialize Messages Object.
     */
    protected void initializeMessages() {

        int e = maxErrorMessagesParm;
        if (e <= 0) {
            e = LangConstants.MAX_ERROR_MESSAGES_DEFAULT;
        }

        int i = maxInfoMessagesParm;
        if (i <= 0) {
            i = LangConstants.MAX_INFO_MESSAGES_DEFAULT;
        }

        messages = new Messages(e,
                                i);
    }

    /**
     *  Check SysErr to see if I/O Error has occurred.
     */
    protected void checkSysErrError()
                        throws IOException {

        if (sysErr.checkError()) {
            sysErr = null;
            throw new IOException(
                UtilConstants.ERRMSG_SYSERR_PRINT_WRITER_IO_ERROR_1);
        }
    }

    /**
     *  Check SysOut to see if I/O Error has occurred.
     */
    protected void checkSysOutError()
                        throws IOException {

        if (sysOut.checkError()) {
            sysOut = null;
            throw new IOException(
                UtilConstants.ERRMSG_SYSOUT_PRINT_WRITER_IO_ERROR_1);
        }
    }


    /**
     *  Close SysOut.
     */
    protected void closeSysOut() {
        if (sysOut != null) {
            sysOut.close();
        }
    }

    /**
     *  Close SysErr.
     */
    protected void closeSysErr() {
        if (sysErr != null) {
            sysErr.close();
        }
    }
}

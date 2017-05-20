//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  BatchFramework.java  0.08 08/01/2008
 *
 *  Sep-25-2005:
 *      -->Start counting RunParmFile lines at 1. Duh.
 *  Dec-03-2005:
 *      -->Add ProofAsst stuff
 *      -->Added exception/error message and stack trace
 *         print directly to System.err in runIt() because
 *         NullPointException was just dumping with the
 *         helpful message "null".
 *  Sep-03-2006:
 *      -->Add TMFF stuff
 *  Jun-01-2007 - Version 0.06
 *      -->OutputVerbosity RunParm stuff.
 *  Sep-01-2007 - Version 0.07
 *      -->Add WorkVarBoss.
 *  Aug-01-2008 - Version 0.08
 *      -->Add SvcBoss.
 *      -->Make sure LogicalSystemBoss is at the end of the
 *         Boss list. This gives other bosses a chance to
 *         see the LoadFile command and re-initialize
 *         themselves (rare situation: only if multiple
 *         sets of commands in a single RunParm file.)
 *      -->Add TheoremLoaderBoss
 */

package mmj.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import mmj.lang.TheoremLoaderException;
import mmj.lang.VerifyException;
import mmj.mmio.MMIOException;

/**
 *  BatchFramework is a quick hack to run mmj2 without
 *  the JUnit training wheels.
 *  <p>
 *  An example of using this "framework" is BatchMMJ2
 *  which sub-classes BatchFramework and is invoked
 *  via "main(String[] args)", passing those parameters
 *  in turn, to BatchFramework.
 *  <p>
 *  A RunParmFile is used to provide flexibility for
 *  many, many parameters in the future, and to allow
 *  for files using different code sets.
 *  <p>
 *  This code is experimental and goofy looking :)
 *  No warranty provided... The theme is that the
 *  order of input RunParmFile commands is unknown
 *  and therefore, each function must not make any
 *  assumptions about what has been done previously.
 *  <p>
 *  "Boss" classes are used to manage "state"
 *  information, and "get" commands build
 *  objects as needed, invoking functions under the
 *  control of other Bosses to obtain needed objects
 *  (which are *not* to be retained given the dynamic
 *  flux of state information.) This all seems very
 *  inefficient but the overhead is very small
 *  compared to the amount of work performed by each
 *  RunParmFile command.
 *  <p>
 *  A list of Bosses in use during a given run is
 *  maintained and each input RunParm line is
 *  sent to each Boss, in turn, which may or may
 *  not do anything with the command. Upon exit,
 *  a Boss returns "consumed" to indicate that
 *  the RunParm need not be sent to any other
 *  Bosses, that the job is done. Adding a new
 *  function, such as "Soundness checking" should
 *  be simple.
 *  <p>
 *  An alternate way to use BatchFramework is to
 *  instantiate one but instead of executing "runIt",
 *  invoke initializeBatchFramework() and then
 *  directly call routines in the various Boss
 *  classes, passing them hand-coded RunParmArrayEntry
 *  objects built using the UtilConstants RunParm
 *  name and value literals.. In other words, don't
 *  use a RunParmFile. This approach provides a
 *  short-cut for invoking mmj2 functions, to which
 *  new functions could be easily applied.
 *
 */
public abstract class BatchFramework {

    protected    boolean batchFrameworkInitialized
                                   = false;

    protected    RunParmFile       runParmFile;
    protected    int               runParmCnt;

    /*friendly*/ String            runParmExecutableCaption;
    /*friendly*/ String            runParmCommentCaption;

    /*friendly*/ ArrayList         bossList;

    /*friendly*/ OutputBoss        outputBoss;

    /*friendly*/ LogicalSystemBoss logicalSystemBoss;

    /*friendly*/ VerifyProofBoss   verifyProofBoss;

    /*friendly*/ GrammarBoss       grammarBoss;

    /*friendly*/ ProofAsstBoss     proofAsstBoss;

    /*friendly*/ TMFFBoss          tmffBoss;

    /*friendly*/ WorkVarBoss       workVarBoss;

    /*friendly*/ SvcBoss           svcBoss;

    /*friendly*/ TheoremLoaderBoss theoremLoaderBoss;


    /**
     *  Default Constructor.
     */
    public BatchFramework() {
    }

    /**
     *  Initialize BatchFramework with Boss list and
     *  any captions that may have been overridden.
     *
     *  The purpose of doing this here is to allow a
     *  BatchFramework to be constructed without
     *  executing any runparms from a file: every
     *  "doRunParm" function is public and can be
     *  called from a program (assuming the program
     *  can create a valid RunParmArrayEntry to
     *  provide RunParm option values.) This provides
     *  a shortcut to invoking complicated mmj2 functions
     *  that would otherwise require lots of setup
     *  and parameters.
     */
    public void initializeBatchFramework() {
        batchFrameworkInitialized = true;
        bossList                  = new ArrayList();

        outputBoss                = new OutputBoss(this);
        addBossToBossList(outputBoss);

        verifyProofBoss           = new VerifyProofBoss(this);
        addBossToBossList(verifyProofBoss);

        grammarBoss               = new GrammarBoss(this);
        addBossToBossList(grammarBoss);

        proofAsstBoss             = new ProofAsstBoss(this);
        addBossToBossList(proofAsstBoss);

        tmffBoss                  = new TMFFBoss(this);
        addBossToBossList(tmffBoss);

        workVarBoss               = new WorkVarBoss(this);
        addBossToBossList(workVarBoss);

        svcBoss                   = new SvcBoss(this);
        addBossToBossList(svcBoss);

        theoremLoaderBoss        = new TheoremLoaderBoss(this);
        addBossToBossList(theoremLoaderBoss);

        addAnyExtraBossesToBossList();

        //NOTE: LogicalSystemBoss should be at the end
        //      because the "LoadFile" RunParm is a signal
        //      to many other the other bosses that they
        //      need to re-initialize their state (for
        //      example, Grammar requires reinitialization
        //      for new input .mm files.)
        logicalSystemBoss         = new LogicalSystemBoss(this);
        addBossToBossList(logicalSystemBoss);

        setRunParmExecutableCaption();
        setRunParmCommentCaption();
    }

    /**
     *  Uses command line run parms to build a RunParmFile,
     *  object, initializes processing Boss objects and
     *  processes each RunParmFile line.
     *  <p>
     *  <b>args:</b>
     *  <ol>
     *  <li>args[0] = RunParmFile file name (no attempt
     *                is made to control directory, so
     *                it may be absolute or relative path,
     *                and either works or doesn't.)
     *  <li>args[1] = RunParmFile file name charset name.
     *                Optional, if null or empty string,
     *                the platform default charset is used.
     *                See UtilConstants for charset info.
     *  <li>args[2] = Delimiter char for DelimitedTextParser.
     *                Optional. Default is
     *                UtilConstants.RUNPARM_FIELD_DELIMITER_DEFAULT.
     *                Specify parm "" to accept default.
     *  <li>args[2] = "Quoter" char for DelimitedTextParser.
     *                Optional. Default is
     *                UtilConstants.RUNPARM_FIELD_QUOTER_DEFAULT
     *                Specify parm "" to accept default.
     *  </ol>
     *
     *  @param args command line parms for RunParmFile constructor.
     *              (See RunParmFile.java for detailed doc
     *
     *  @return return code 0 if BatchFramework was successful
     *          (however many mmj/Metamath errors were found),
     *          or 16, if BatchFramework failed to complete
     *          (probably due to a RunParmFile error.)
     */
    public int runIt(String[] args) {

        int retCd                 = 0;

        if (!batchFrameworkInitialized) {
            initializeBatchFramework();
        }

        try {
            runParmFile           = new RunParmFile(args);
        }
        catch(Exception e) {
            System.err.println(
                UtilConstants.ERRMSG_RUNPARM_FILE_BOGUS_1
                + e.getMessage());
            retCd                 = 16;
        }

        if (retCd == 0) {
            try {
                while (runParmFile.hasNext()) {
                    executeRunParmCommand(runParmFile.next());
                }
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
                retCd                 = 16;
                String finalMessage   = e.getMessage();
                try {
                    outputBoss.sysErrPrintln(finalMessage);
                }
                catch(IOException f) {
                    System.err.println(finalMessage
                                       + f.getMessage());
                }
            }
            catch (Error e) {
                retCd                 = 16;
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }
            outputBoss.close();
        }
        return retCd;
    }

    /**
     *  Add a processing Boss to the Boss List.
     *
     *  @param boss
     */
    public void addBossToBossList(Boss boss) {
        bossList.add(boss);
    }

    /**
     *  Placeholder to be overridden to dynamically add
     *  a new processing Boss to the list.
     *  <p>
     *  Override this to include extra processing. Each
     *  Boss on the bossList will get a chance to process
     *  each RunParmFile line that hasn't already been
     *  "consumed" by other bosses.
     */
    public void addAnyExtraBossesToBossList() {
    }

    /**
     *  Override this to alter what prints out before
     *  an executable RunParmFile line is processed.
     */
    public void setRunParmExecutableCaption() {
        runParmExecutableCaption   =
            UtilConstants.ERRMSG_RUNPARM_EXECUTABLE_CAPTION;
    }

    /**
     *  Override this to alter what prints out for
     *  a RunParmFile comment line.
     */
    public void setRunParmCommentCaption() {
        runParmCommentCaption   =
            UtilConstants.ERRMSG_RUNPARM_COMMENT_CAPTION;
    }

    /**
     *  Processes a single RunParmFile line.
     *
     *  @param runParm RunParmFileLine parsed into a
     *         RunParmArrayEntry object.
     */
    public void executeRunParmCommand(RunParmArrayEntry runParm)
                            throws IllegalArgumentException,
                                   MMIOException,
                                   FileNotFoundException,
                                   IOException,
                                   VerifyException,
                                   TheoremLoaderException {


        boolean  consumed;
        Iterator iterator;
        ++runParmCnt;
        if (runParm.commentLine != null) {
            printCommentRunParmLine(
                             runParmCommentCaption,
                             runParmCnt,
                             runParm);
        }
        else {
            printExecutableRunParmLine(
                             runParmExecutableCaption,
                             runParmCnt,
                             runParm);

            if (runParm.name.compareToIgnoreCase(
                UtilConstants.RUNPARM_JAVA_GARBAGE_COLLECTION)
                == 0) {
                System.gc();
            }
            else {
                consumed = false;
                iterator = bossList.iterator();
                while (iterator.hasNext()
                       &&
                       !consumed) {
                    consumed      =
                        ((Boss)iterator.next()
                            ).doRunParmCommand(runParm);
                }
                if (!consumed &&
                    runParm.name.compareToIgnoreCase(
                        UtilConstants.RUNPARM_CLEAR)
                    != 0) {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_RUNPARM_NAME_INVALID_1
                        + runParm.name
                        + UtilConstants.ERRMSG_RUNPARM_NAME_INVALID_2);
                }
            }
        }
    }

    /**
     *  Override this to change or eliminate the printout
     *  of each executable RunParmFile line.
     *
     *  @param caption to print
     *  @param cnt     RunParmFile line number
     *  @param runParm RunParmFile line parsed into
     *                 object RunParmArrayEntry.
     */
    public void printExecutableRunParmLine(
                                 String            caption,
                                 int               cnt,
                                 RunParmArrayEntry runParm)
                            throws IOException {
        outputBoss.sysOutPrintln(
                        caption
                        + cnt
                        + UtilConstants.ERRMSG_EQUALS_LITERAL
                        + runParm,
                        UtilConstants.RUNPARM_LINE_DUMP_VERBOSITY);
    }

    /**
     *  Override this to change or eliminate the printout
     *  of each Comment RunParmFile line.
     *
     *  @param caption to print
     *  @param cnt     RunParmFile line number
     *  @param runParm RunParmFile line parsed into
     *                 object RunParmArrayEntry.
     */
    public void printCommentRunParmLine(
                                 String            caption,
                                 int               cnt,
                                 RunParmArrayEntry runParm)
                            throws IOException {
        outputBoss.sysOutPrintln(
                        caption
                        + cnt
                        + UtilConstants.ERRMSG_EQUALS_LITERAL
                        + runParm,
                        UtilConstants.RUNPARM_LINE_DUMP_VERBOSITY);
    }
}

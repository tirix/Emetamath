//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Boss.java  0.09 08/01/2008
 *
 *  Dec-10-2005
 *      -->Added folder-related routines for use by ProofAsst:
 *             editExistingFolderRunParm(
 *             editExistingFolderNameParm(
 *             buildFileObjectForExistingFolder(
 *         These are called by mmj.util.ProofAsstBoss.java
 *  Jan-16-2006
 *      -->Added editYesNoRunParm
 *      -->Added buffering in doConstructPrintWriter.
 *      -->Added doConstructBufferedFileWriter for exporting proofs
 *         from Proof Assistant.
 *      -->changed name of method editPrintWriterFileNameParm() to
 *          editFileNameParm().
 *
 *  Version 0.04:
 *      -->Added editRunParmPrintableNoBlanksString
 *         for ProofAsstBoss
 *
 *  Sep-09-2006 - Version 0.05 - TMFF Enhancement
 *      -->Added editOnOffRunParm()
 *
 *  Jun-01-2007 - Version 0.06
 *      -->err msg bug fix, see "PATCH 2007-10-01"
 *
 *  Nov-01-2007 - Version 0.07
 *      -->Misc.
 *
 *  Feb-01-2008 - Version 0.08
 *      -->Remove old, commented-out code (patch note.)
 *
 *  Aug-01-2008 - Version 0.09
 *      -->Add TheoremLoaderException.
 *      -->editRunParmNonNegativeInteger() and
 *         editRunParmValueReqNonNegativeInt().
 */

package mmj.util;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

import mmj.lang.LogicalSystem;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.lang.TheoremLoaderException;
import mmj.lang.VerifyException;
import mmj.mmio.MMIOException;
import mmj.pa.PaConstants;

/**
 *  Boss is the superclass of GrammarBoss, LogicalSystemBoss,
 *  etc, which are used by BatchFramework to "exercise"
 *  mmj2 in batch mode.
 *  <p>
 *  Boss consists of the abstract "doRunParmCommand" and
 *  some common parameter validation functions used
 *  in sub-classes of Boss.
 */
public abstract class Boss {

    protected BatchFramework batchFramework;

    /**
     *  Constructor with BatchFramework for access to environment.
     *
     *  @param batchFramework for access to environment.
     */
    public Boss(BatchFramework batchFramework) {
        this.batchFramework       = batchFramework;
    }

    /**
     *  Executes a single command from the RunParmFile.
     *
     *  @param runParm the RunParmFile line to execute.
     */
    public abstract boolean
        doRunParmCommand(RunParmArrayEntry runParm)
                        throws IllegalArgumentException,
                               MMIOException,
                               FileNotFoundException,
                               IOException,
                               VerifyException,
                               TheoremLoaderException;


    // =======================================================
    // === bazillions of subroutines used by Boss subclasses
    // =======================================================

    /**
     *  Validate existing folder RunParm (must exist!)
     *
     *  @param runParm RunParmFile line parsed into RunParmArrayEntry.
     *  @param valueCaption name of RunParm, for error message output.
     */
    protected File editExistingFolderRunParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                    throws IllegalArgumentException {

        String folderNameParm     =
            editExistingFolderNameParm(
                runParm,
                valueCaption,
                valueFieldNbr);

        return buildFileObjectForExistingFolder(
                   valueCaption,
                   folderNameParm);
    }

    /**
     *  Validate existing folder RunParm (must exist!)
     *
     *  @param runParm RunParmFile line parsed into RunParmArrayEntry.
     *  @param valueCaption name of RunParm, for error message output.
     */
    protected File editExistingFileRunParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                    throws IllegalArgumentException {

        String fileNameParm     =
            editFileNameParm(
                runParm,
                valueCaption,
                valueFieldNbr);

        return buildFileObjectForExistingFile(
                   valueCaption,
                   fileNameParm);
    }

    /**
     *  Validate Proof Worksheet File Name Suffix
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return String validated file name suffix
     */
    protected String editProofWorksheetFileNameSuffix(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                            throws IllegalArgumentException {
        editRunParmValuesLength(
            runParm,
            valueCaption,
            1);
        String fileNameSuffixParm   =
            runParm.values[valueFieldNbr - 1].trim();
        if (fileNameSuffixParm.compareTo(
            PaConstants.PA_GUI_FILE_CHOOSER_FILE_SUFFIX_TXT)  == 0
            ||
            fileNameSuffixParm.compareTo(
            PaConstants.PA_GUI_FILE_CHOOSER_FILE_SUFFIX_TXT2) == 0
            ||
            fileNameSuffixParm.compareTo(
            PaConstants.PA_GUI_FILE_CHOOSER_FILE_SUFFIX_MMP) == 0
            ||
            fileNameSuffixParm.compareTo(
            PaConstants.PA_GUI_FILE_CHOOSER_FILE_SUFFIX_MMP2) == 0) {
            return fileNameSuffixParm;
        }

        throw new IllegalArgumentException(
            UtilConstants.ERRMSG_BAD_FILE_NAME_SUFFIX_1
            + valueCaption
            + UtilConstants.ERRMSG_BAD_FILE_NAME_SUFFIX_2
            + valueFieldNbr
            + UtilConstants.ERRMSG_BAD_FILE_NAME_SUFFIX_3);
    }


    /**
     *  Validate name of folder
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return String validated folder name.
     */
    protected String editExistingFolderNameParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                            throws IllegalArgumentException {
        editRunParmValuesLength(
            runParm,
            valueCaption,
            1);
        String folderNameParm   =
            runParm.values[valueFieldNbr - 1].trim();
        if (folderNameParm.length() == 0) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FOLDER_NAME_BLANK_1
                + valueCaption
                + UtilConstants.ERRMSG_FOLDER_NAME_BLANK_2
                + valueFieldNbr
                + UtilConstants.ERRMSG_FOLDER_NAME_BLANK_3);
        }
        return folderNameParm;
    }

    /**
     *  Build a File object for a Folder Name
     *
     *  @param valueCaption    name of RunParm, for error message
     *                         output.
     *  @param folderNameParm  name of folder
     *
     *  @return File           File object for folder
     */
    protected File buildFileObjectForExistingFolder(
                    String valueCaption,
                    String folderNameParm)
                        throws IllegalArgumentException {

        File        folder;
        try {
            folder                = new File(folderNameParm);
            if (folder.exists()) {
                if (folder.isDirectory()) {
                    // okey dokey!
                }
                else {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_NOT_A_FOLDER_1
                        + valueCaption
                        + UtilConstants.ERRMSG_NOT_A_FOLDER_2
                        + folderNameParm
                        + UtilConstants.ERRMSG_NOT_A_FOLDER_3);
                }
            }
            else {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_FOLDER_NOTFND_1
                    + valueCaption
                    + UtilConstants.ERRMSG_FOLDER_NOTFND_2
                    + folderNameParm
                    + UtilConstants.ERRMSG_FOLDER_NOTFND_3);
            }

        }
        catch(Exception e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FOLDER_MISC_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_FOLDER_MISC_ERROR_2
                + folderNameParm
                + UtilConstants.ERRMSG_FOLDER_MISC_ERROR_3
                + e.getMessage());
        }
        return folder;
    }

    /**
     *  Build a File object for an existing File Name
     *
     *  @param valueCaption    name of RunParm, for error message
     *                         output.
     *  @param fileNameParm    name of file
     *
     *  @return File           File object for file.
     */
    protected File buildFileObjectForExistingFile(
                    String valueCaption,
                    String fileNameParm)
                        throws IllegalArgumentException {

        File file;
        try {
            file                = new File(fileNameParm);
            if (file.exists()) {
                if (!file.isDirectory()) {
                    // okey dokey!
                }
                else {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_NOT_A_FILE_1
                        + valueCaption
                        + UtilConstants.ERRMSG_NOT_A_FILE_2
                        + fileNameParm
                        + UtilConstants.ERRMSG_NOT_A_FILE_3);
                }
            }
            else {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_FILE_NOTFND_1
                    + valueCaption
                    + UtilConstants.ERRMSG_FILE_NOTFND_2
                    + fileNameParm
                    + UtilConstants.ERRMSG_FILE_NOTFND_3);
            }

        }
        catch(Exception e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_MISC_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_2
                + fileNameParm
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_3
                + e.getMessage());
        }
        return file;
    }


    /**
     *  Validate PrintWriter RunParm and its options.
     *
     *  @param runParm RunParmFile line parsed into RunParmArrayEntry.
     *  @param valueCaption name of RunParm, for error message output.
     */
    protected PrintWriter editPrintWriterRunParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption)
                    throws IllegalArgumentException {

        String fileNameParm   =
            editFileNameParm(
                runParm,
                valueCaption,
                1);

        String fileUsageParm  =
            editFileUsageParm(
                runParm,
                valueCaption,
                2);

        String fileCharsetParm
                              =
            editFileCharsetParm(
                runParm,
                valueCaption,
                3);

        return doConstructPrintWriter(
                   valueCaption,
                   fileNameParm,
                   fileUsageParm,
                   fileCharsetParm);
    }


    /**
     *  Construct a PrintWriter using RunParm options.
     *
     *  @param valueCaption    name of RunParm, for error message
     *                         output.
     *  @param fileNameParm    RunParmFile line parsed into
     *                         RunParmArrayEntry.
     *  @param fileUsageParm   "new" or "update"
     *  @param fileCharsetParm optional, "UTF-8", etc.
     *
     *  @return PrintWriter object.
     */
    protected PrintWriter doConstructPrintWriter(
                    String valueCaption,
                    String fileNameParm,
                    String fileUsageParm,
                    String fileCharsetParm)
                        throws IllegalArgumentException {

        PrintWriter printWriter
                              = null;
        File        file;
        try {
            file              = new File(fileNameParm);
            if (file.exists()) {
                if (fileUsageParm.compareTo(
                      UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW)
                    == 0) {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_1
                        + valueCaption
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_2
                        + fileNameParm
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_3
                        + UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_4
                        );
                }
                else {
                    if (file.isFile()
                        &&
                        file.canWrite()
                        &&
                        !file.isDirectory()) {
                        // okey dokey!
                    }
                    else {
                        throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_1
                        + valueCaption
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_2
                        + fileNameParm
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_3
                        +
                        UtilConstants.RUNPARM_OPTION_FILE_OUT_UPDATE
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_4
                        );
                    }
                }
            }

            if (fileCharsetParm.length() == 0) {
                printWriter       =
                    new PrintWriter(
                        new BufferedWriter(
                            new FileWriter(fileNameParm)
                            )
                        );
            }
            else {
                printWriter       =
                    new PrintWriter(
                        new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(
                                    fileNameParm),
                                fileCharsetParm
                            )
                        )
                    );

            }
        }
        catch(Exception e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_MISC_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_2
                + fileNameParm
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_3
                + e.getMessage());
        }
        return printWriter;
    }

    /**
     *  Construct a Buffered File Reader using RunParm
     *  options plus an optional parent directory File object.
     *
     *  @param valueCaption    name of RunParm, for error message
     *                         output.
     *  @param fileNameParm    RunParmFile line parsed into
     *                         RunParmArrayEntry.
     *
     *  @return BufferedReader file object.
     */
    protected Reader doConstructBufferedFileReader(
                    String valueCaption,
                    String fileNameParm,
                    File   parentDirectory)
                        throws IllegalArgumentException {

        Reader bufferedFileReader = null;

        File           file       = new File(fileNameParm);
        if (parentDirectory == null ||
            file.isAbsolute()) {
            //ok, use as-is
        }
        else {
            // fileName relative to parentDirectory.
            file                  = new File(parentDirectory,
                                             fileNameParm);
        }

        try {
            if (!file.exists()) {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_FILE_NOTFND_1
                    + valueCaption
                    + UtilConstants.ERRMSG_FILE_NOTFND_2
                    + fileNameParm
                    + UtilConstants.ERRMSG_FILE_NOTFND_3
                    );
            }
            if (file.isFile()
                &&
                file.canRead()
                &&
                !file.isDirectory()) {
                        // okey dokey!
            }
            else {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_FILE_READ_NOT_ALLOWED_1
                    + valueCaption
                    +
                    UtilConstants.ERRMSG_FILE_READ_NOT_ALLOWED_2
                    + fileNameParm
                    +
                    UtilConstants.ERRMSG_FILE_READ_NOT_ALLOWED_3
                    );
            }

            bufferedFileReader    =
                new BufferedReader(
                    new FileReader(file));
        }
        catch(Exception e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_MISC_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_2
                + fileNameParm
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_3
                + e.getMessage());
        }
        return bufferedFileReader;
    }



    /**
     *  Construct a Buffered File Writer using RunParm
     *  options plus an optional parent directory File object.
     *
     *  @param valueCaption    name of RunParm, for error message
     *                         output.
     *  @param fileNameParm    RunParmFile line parsed into
     *                         RunParmArrayEntry.
     *  @param fileUsageParm   "new" or "update"
     *
     *  @return BufferedWriter file object.
     */
    protected BufferedWriter doConstructBufferedFileWriter(
                    String valueCaption,
                    String fileNameParm,
                    String fileUsageParm,
                    File   parentDirectory)
                        throws IllegalArgumentException {

        BufferedWriter bufferedFileWriter
                                  = null;
        File           file       = new File(fileNameParm);
        if (parentDirectory == null ||
            file.isAbsolute()) {
            //ok, use as-is
        }
        else {
            // fileName relative to parentDirectory.
            file                  = new File(parentDirectory,
                                             fileNameParm);
        }

        try {
            if (file.exists()) {
                if (fileUsageParm.compareTo(
                      UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW)
                    == 0) {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_1
                        + valueCaption
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_2
                        + fileNameParm
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_3
                        + UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW
                        + UtilConstants.ERRMSG_FILE_USAGE_ERR_EXISTS_4
                        );
                }
                else {
                    if (file.isFile()
                        &&
                        file.canWrite()
                        &&
                        !file.isDirectory()) {
                        // okey dokey!
                    }
                    else {
                        throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_1
                        + valueCaption
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_2
                        + fileNameParm
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_3
                        +
                        UtilConstants.RUNPARM_OPTION_FILE_OUT_UPDATE
                        +
                        UtilConstants.ERRMSG_FILE_UPDATE_NOT_ALLOWED_4
                        );
                    }
                }
            }

            bufferedFileWriter    =
                new BufferedWriter(
                    new FileWriter(file));
        }
        catch(Exception e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_MISC_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_2
                + fileNameParm
                + UtilConstants.ERRMSG_FILE_MISC_ERROR_3
                + e.getMessage());
        }
        return bufferedFileWriter;
    }


    /**
     *  Validate File Name.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return String validated file name.
     */
    protected String editFileNameParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                            throws IllegalArgumentException {
        editRunParmValuesLength(
            runParm,
            valueCaption,
            1);
        String fileNameParm   =
            runParm.values[valueFieldNbr - 1].trim();
        if (fileNameParm.length() == 0) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_NAME_BLANK_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_NAME_BLANK_2
                + valueFieldNbr
                + UtilConstants.ERRMSG_FILE_NAME_BLANK_3);
        }
        return fileNameParm;
    }

    /**
     *  Validate File Usage Parm ("new" or "update").
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return String validated file usage parm.
     */
    protected String editFileUsageParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                            throws IllegalArgumentException {

        if (runParm.values.length < valueFieldNbr ) {
            return UtilConstants.OPTION_FILE_OUT_USAGE_DEFAULT;
        }

        String fileUsageParm  =
            runParm.values[valueFieldNbr - 1].trim();
        if (fileUsageParm.length() == 0) {
            return UtilConstants.OPTION_FILE_OUT_USAGE_DEFAULT;
        }

        if (fileUsageParm.compareToIgnoreCase(
                UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW)
            == 0) {
            return UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW;
        }

        if (fileUsageParm.compareToIgnoreCase(
                UtilConstants.RUNPARM_OPTION_FILE_OUT_UPDATE)
            == 0) {
            return UtilConstants.RUNPARM_OPTION_FILE_OUT_UPDATE;
        }

        throw new IllegalArgumentException(
            UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_1
            + valueCaption
            + UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_2
            + valueFieldNbr
            + UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_3
            + UtilConstants.RUNPARM_OPTION_FILE_OUT_NEW
            + UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_4
            + UtilConstants.RUNPARM_OPTION_FILE_OUT_UPDATE
            + UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_5
            + fileUsageParm
            + UtilConstants.ERRMSG_FILE_USAGE_PARM_UNRECOG_6);
    }


    /**
     *  Validate File Charset Parm ("" or "UTF-8", etc).
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return String validated file usage parm.
     */
    protected String editFileCharsetParm(
                        RunParmArrayEntry runParm,
                        String            valueCaption,
                        int               valueFieldNbr)
                            throws IllegalArgumentException {

        if (runParm.values.length < valueFieldNbr ) {
            return new String("");
        }

        String fileCharsetParm  =
            runParm.values[valueFieldNbr - 1].trim();
        if (fileCharsetParm.length() == 0) {
            return new String("");
        }

        boolean isSupported;
        try {
            isSupported       =
                Charset.isSupported(fileCharsetParm);
        }
        catch(IllegalCharsetNameException e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_CHARSET_INVALID_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_CHARSET_INVALID_2
                + valueFieldNbr
                + UtilConstants.ERRMSG_FILE_CHARSET_INVALID_3
                + fileCharsetParm
                + UtilConstants.ERRMSG_FILE_CHARSET_INVALID_4
                + e.getMessage());
        }

        if (!isSupported) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_FILE_CHARSET_UNSUPPORTED_1
                + valueCaption
                + UtilConstants.ERRMSG_FILE_CHARSET_UNSUPPORTED_2
                + valueFieldNbr
                + UtilConstants.ERRMSG_FILE_CHARSET_UNSUPPORTED_3
                + fileCharsetParm
                + UtilConstants.ERRMSG_FILE_CHARSET_UNSUPPORTED_4);
        }
        return fileCharsetParm;
    }

    /**
     *  Get SelectorAll RunParm Option if present or null.
     *  <p>
     *  If "*" input returns true Boolean value
     *  otherwise, null;
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return boolean true(yes) or false(no)
     */
    protected Boolean getSelectorAllRunParmOption(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr) {

        if (runParm.values.length >= valueFieldNbr
            &&
            (runParm.values[valueFieldNbr - 1].trim()).
                equals(UtilConstants.RUNPARM_OPTION_VALUE_ALL)) {
            return Boolean.valueOf(true);
        }
        else {
            return null;
        }
    }

    /**
     *  Get SelectorCount RunParm Option if present or null.
     *  <p>
     *  If positive integer input returns Integer value,
     *  otherwise, if negative or zero integer, throws
     *  an exception. If none of the above returns null;
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return boolean true(yes) or false(no)
     */
    protected Integer getSelectorCountRunParmOption(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        Integer count             = null;
        if (runParm.values.length >= valueFieldNbr) {
            try {
                // NumberFormatException if not integer
                count                 =
                    Integer.valueOf(
                        runParm.values[valueFieldNbr - 1].trim());
                // IllegalArgumentException if not > 0
                count                 =
                    Integer.valueOf(
                        editRunParmValueReqPosInt(
                            runParm,
                            valueCaption,
                            valueFieldNbr));
            }
            catch (NumberFormatException e) {
                count                 = null;
            }
        }
        return count;
    }

    /**
     *  Get SelectorTheorem RunParm Option if present or null.
     *  <p>
     *  If present, and not a valid Theorem label, an
     *  IllegalArgumentException is thrown.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *  @param stmtTbl        stmtTbl map from LogicalSystem.
     *
     *  @return Theorem or null
     */
    protected Theorem getSelectorTheoremRunParmOption(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr,
                    Map               stmtTbl)
                        throws IllegalArgumentException {

        Object  mapValue;
        String  label;
        if (runParm.values.length >= valueFieldNbr) {
            label             =
                runParm.values[valueFieldNbr - 1].trim();
            mapValue          =
                stmtTbl.get(label);
            if (mapValue == null) {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_SELECTOR_NOT_A_STMT_1
                    + valueCaption
                    + UtilConstants.ERRMSG_SELECTOR_NOT_A_STMT_2
                    + valueFieldNbr
                    + UtilConstants.ERRMSG_SELECTOR_NOT_A_STMT_3
                    + label
                    + UtilConstants.ERRMSG_SELECTOR_NOT_A_STMT_4);
            }
            if (mapValue instanceof Theorem) {
                return (Theorem)mapValue;
            }
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_SELECTOR_NOT_A_THEOREM_1
                + valueCaption
                + UtilConstants.ERRMSG_SELECTOR_NOT_A_THEOREM_2
                + valueFieldNbr
                + UtilConstants.
                    ERRMSG_SELECTOR_NOT_A_THEOREM_3
                + label
                + UtilConstants.
                    ERRMSG_SELECTOR_NOT_A_THEOREM_4);
        }
        return null;
    }


    /**
     *  Validate Required Yes/No Parm.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return boolean true(yes) or false(no)
     */
    protected boolean editYesNoRunParm(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);
        boolean yesNoBoolean;
        String yesNoParm      =
            (runParm.values[valueFieldNbr - 1]).toLowerCase().trim();
        if (yesNoParm.equals(UtilConstants.RUNPARM_OPTION_YES)
            ||
            yesNoParm.equals(
                UtilConstants.RUNPARM_OPTION_YES_ABBREVIATED)) {
            yesNoBoolean          = true;
        }
        else {
            if (yesNoParm.equals(
                    UtilConstants.RUNPARM_OPTION_NO)
                ||
                yesNoParm.equals(
                    UtilConstants.RUNPARM_OPTION_NO_ABBREVIATED)) {
                yesNoBoolean      = false;
            }
            else {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_RECHECK_PA_1
                    + valueCaption
                    + UtilConstants.ERRMSG_RECHECK_PA_2);
            }
        }
        return yesNoBoolean;
    }

    /**
     *  Validate Required On/Off Parm.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return boolean true(yes) or false(no)
     */
    protected boolean editOnOffRunParm(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);
        boolean onOffBoolean;
        String onOffParm          =
            (runParm.values[valueFieldNbr - 1]).toLowerCase().trim();
        if (onOffParm.equals(UtilConstants.RUNPARM_OPTION_ON)) {
            onOffBoolean          = true;
        }
        else {
            if (onOffParm.equals(UtilConstants.RUNPARM_OPTION_OFF)) {
                onOffBoolean      = false;
            }
            else {
                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_BAD_ON_OFF_PARM_1
                    + valueCaption
                    + UtilConstants.ERRMSG_BAD_ON_OFF_PARM_2);
            }
        }
        return onOffBoolean;
    }

    /**
     *  Validate Required, RGB Color Parms
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @return int positive integer.
     */
    protected Color editRunParmValueReqRGBColor(
                    RunParmArrayEntry runParm,
                    String            valueCaption)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            UtilConstants.RUNPARM_NBR_RGB_COLOR_VALUES);

        int[] rgb                 =
            new int[UtilConstants.RUNPARM_NBR_RGB_COLOR_VALUES];

        for (int valueFieldNbr    = 0;
             valueFieldNbr <
                UtilConstants.RUNPARM_NBR_RGB_COLOR_VALUES;
             valueFieldNbr++) {

            rgb[valueFieldNbr]    =
                editRunParmValueInteger(
                    runParm.values[valueFieldNbr],
                    valueCaption);

            if (rgb[valueFieldNbr] <
                UtilConstants.RUNPARM_OPTION_MIN_RGB_COLOR
                ||
                rgb[valueFieldNbr] >
                UtilConstants.RUNPARM_OPTION_MAX_RGB_COLOR) {

                throw new IllegalArgumentException(
                    UtilConstants.ERRMSG_RUNPARM_RGB_RANGE_1
                    + valueCaption
                    + UtilConstants.ERRMSG_RUNPARM_RGB_RANGE_2
                    + UtilConstants.RUNPARM_OPTION_MIN_RGB_COLOR
                    + UtilConstants.ERRMSG_RUNPARM_RGB_RANGE_3
                    + UtilConstants.RUNPARM_OPTION_MAX_RGB_COLOR
                    + UtilConstants.ERRMSG_RUNPARM_RGB_RANGE_4
                    + Integer.toString(rgb[valueFieldNbr]));
            }
        }

        return new Color(rgb[0],
                         rgb[1],
                         rgb[2]);
    }

    /**
     *  Validate Required, Positive Integer Parm.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return int positive integer.
     */
    protected int editRunParmValueReqPosInt(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);
        Integer i             =
            editRunParmValueInteger(
                runParm.values[valueFieldNbr - 1],
                valueCaption);
        return editRunParmPositiveInteger(
                    i,
                    valueCaption);
    }

    /**
     *  Validate Required, Non-negative Integer Parm.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return int positive integer.
     */
    protected int editRunParmValueReqNonNegativeInt(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);
        Integer i             =
            editRunParmValueInteger(
                runParm.values[valueFieldNbr - 1],
                valueCaption);
        return editRunParmNonNegativeInteger(
                    i,
                    valueCaption);
    }

    /**
     *  Validate Required Integer Parm.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  number of field in RunParm line.
     *
     *  @return int integer.
     */
    protected int editRunParmValueReqInt(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);
        Integer i             =
            editRunParmValueInteger(
                runParm.values[valueFieldNbr - 1],
                valueCaption);
        return i;
    }


    /**
     *  Validate Required Number of RunParm fields.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param requiredNbrValueFields required number of fields
     *                        in the RunParm line.
     */
    protected void editRunParmValuesLength(
                     RunParmArrayEntry runParm,
                     String            valueCaption,
                     int               requiredNbrValueFields)
                        throws IllegalArgumentException {

        if (runParm.values.length < requiredNbrValueFields) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_NOT_ENOUGH_FIELDS_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_NOT_ENOUGH_FIELDS_2
                + requiredNbrValueFields
                + UtilConstants.ERRMSG_RUNPARM_NOT_ENOUGH_FIELDS_3);
        }
    }


    /**
     *  Validate Integer Parm.
     *
     *  @param integerString  String supposedly containing a number.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *
     *  @return int an integer.
     */
    protected Integer editRunParmValueInteger(
                String integerString,
                String valueCaption)
                    throws IllegalArgumentException {

        Integer i = null;
        try {
            i = Integer.valueOf(integerString.trim());
        }
        catch(NumberFormatException e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_NBR_FORMAT_ERROR_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_NBR_FORMAT_ERROR_2
                + e.getMessage());
        }
        return i;
    }

    /**
     *  Validate Positive Integer Parm.
     *
     *  @param i              an integer, supposedly positive.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *
     *  @return int a positive integer.
     */
    protected int editRunParmPositiveInteger(
                    Integer i,
                    String  valueCaption)
                        throws IllegalArgumentException {
        int n = i.intValue();
        if (n <= 0) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_NBR_LE_ZERO_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_NBR_LE_ZERO_2
                + i.toString());
        }
        return n;
    }

    /**
     *  Validate Non-Negative Integer Parm.
     *
     *  @param i              an integer, supposedly greater than
     *                        or equal to zero.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *
     *  @return int a positive integer.
     */
    protected int editRunParmNonNegativeInteger(
                    Integer i,
                    String  valueCaption)
                        throws IllegalArgumentException {
        int n = i.intValue();
        if (n < 0) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_NBR_LT_ZERO_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_NBR_LT_ZERO_2
                + i.toString());
        }
        return n;
    }

    /**
     *  Validate RunParm Theorem Label String.
     *
     *  @param stmtLabel      String, supposedly a Theorem label.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param logicalSystem  Uh-oh, Mr. Big. Heavy validation
     *                        using LogicalSystem.stmtTbl.
     *
     *  @return Theorem if stmtLabel is valid.
     */
    protected Theorem editRunParmValueTheorem(
                String        stmtLabel,
                String        valueCaption,
                LogicalSystem logicalSystem)
                    throws IllegalArgumentException {
        Stmt stmt             =
            editRunParmValueStmt(stmtLabel,
                                 valueCaption,
                                 logicalSystem);
        if (stmt instanceof Theorem) {
            return (Theorem)stmt;
        }
        else {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_STMT_NOT_THEOREM_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_STMT_NOT_THEOREM_2
                + stmtLabel
                + UtilConstants.ERRMSG_RUNPARM_STMT_NOT_THEOREM_3);
        }
    }

    /**
     *  Validate RunParm Statement Label String.
     *
     *  @param stmtLabel      String, supposedly a Stmt label.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param logicalSystem  Uh-oh, Mr. Big. Heavy validation
     *                        using LogicalSystem.stmtTbl.
     *
     *  @return Stmt if stmtLabel is valid.
     */
    protected Stmt editRunParmValueStmt(
                String        stmtLabel,
                String        valueCaption,
                LogicalSystem logicalSystem)
                    throws IllegalArgumentException {
        if (stmtLabel.length() == 0) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_STMT_LABEL_BLANK_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_STMT_LABEL_BLANK_2);
        }

        Stmt stmt             =
            (Stmt)(logicalSystem.getStmtTbl().get(stmtLabel));
        if (stmt == null) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_STMT_LABEL_NOTFND_1
                + valueCaption
                + UtilConstants.ERRMSG_RUNPARM_STMT_LABEL_NOTFND_2
                + stmtLabel
                + UtilConstants.ERRMSG_RUNPARM_STMT_LABEL_NOTFND_3);
        }
        return stmt;
    }


    /**
     *  Validate RunParm String with length greater than
     *  zero and no embedded blanks or unprintable characters.
     *
     *  @param runParm        RunParmFile line.
     *  @param valueCaption   name of RunParm, for error message
     *                        output.
     *  @param valueFieldNbr  required number of fields
     *                        in the RunParm line.
     *
     *  @return String if valid.
     */
    protected String editRunParmPrintableNoBlanksString(
                    RunParmArrayEntry runParm,
                    String            valueCaption,
                    int               valueFieldNbr)
                        throws IllegalArgumentException {

        editRunParmValuesLength(
            runParm,
            valueCaption,
            valueFieldNbr);

        String printableNoBlanksString
                                  =
            runParm.values[valueFieldNbr - 1].trim();

        boolean   err             = true;
        char      c;
        if (printableNoBlanksString.length() > 0) {
            err                   = false;
            for (int i = 0;
                 i < printableNoBlanksString.length();
                 i++) {

                c                 = printableNoBlanksString.charAt(i);
                if (c > 127                   ||
                    Character.isWhitespace(c) ||
                    Character.isISOControl(c)) {
                    err           = true;
                    break;
                }
            }
        }

        if (err) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_NONBLANK_PRINT_STR_BAD_1
                + valueCaption
                + UtilConstants.
                    ERRMSG_RUNPARM_NONBLANK_PRINT_STR_BAD_2);
        }
        return printableNoBlanksString;
    }
}

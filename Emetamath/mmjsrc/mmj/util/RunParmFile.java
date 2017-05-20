//********************************************************************/
//* Copyright (C) 2005  MEL O'CAT  mmj2 (via) planetmath (dot) org   */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  RunParmFile.java.java  0.02 08/24/2005
 *
 *  Nov-1-2006: Version 0.03:
 *          --> Fixed bug involving "" input parms.
 */

package mmj.util;

import java.io.*;

/**
 *  RunParmFile reads lines designed to be parsed by
 *  DelimitedTextParser and returns RunParmArrayEntry
 *  objects.
 *  <p>
 *  Comment lines are identified in RunParmFileArrayEntry
 *  and this class knows nothing whatsoever about mmj
 *  or Metamath.
 *  <p>
 *  Coincidentally (ha), the "String[] args" parameter
 *  for the constructor is the same as the BatchMMJ2
 *  command line parms.
 */
public class RunParmFile {

    private   String              fileName      = null;
    private   String              codeSet       = null;
    private   char                delimiter;
    private   char                quoter;

    private   String              inputLine     = null;
    private   int                 lineCount     = 0;

    private   BufferedReader      fileIn        = null;
    private   boolean             eofReached    = false;

    private   DelimitedTextParser delimitedTextParser;


    /**
     *  Construct using "String[] args" parameters.
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
     *  @param args Array of String: filename, charset, delimiter
     *              and quoter.
     *
     *  @throws IllegalArgumentException see UtilConstants.
     */
    public RunParmFile(String[] args)
                throws  IOException,
                        IllegalArgumentException {

        /**
         *  first argument, the RunParmFile name, is required.
         */
        if (args            == null ||
            args.length     == 0    ||
            args[0]         == null ||
            args[0].length() < 1) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_ARG1_ERROR);
        }
        fileName = args[0];

        /**
         *  second argument, RunParmFile charset name is optional,
         *  but if entered must look halfway valid. Default
         *  is the system default codeset.
         */
        if (args.length > 1) {
            if (args[1] == null ||
                args[1].length() == 0) {
                codeSet = new String("");
            }
            else {
                if (args[1].length() > 0 &&
                    args[1].length() < 3) {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_RUNPARM_ARG2_ERROR);
                }
                codeSet = args[1];
            }
        }
        else {
            codeSet = new String("");
        }


        /**
         *  third argument, delimiter character for parsing
         *  lines in the RunParmFile, is also optional.
         *  Default is ',' (comma).
         */
        if (args.length > 2) {
            if (args[2] == null ||
                args[2].length() == 0) {
                delimiter =
                    UtilConstants.RUNPARM_FIELD_DELIMITER_DEFAULT;
            }
            else {
                if (args[2].length() != 1 ||
                    args[2].charAt(0) == ' ') {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_RUNPARM_ARG3_ERROR);
                }
                delimiter = args[2].charAt(0);
            }
        }
        else {
            delimiter =
                UtilConstants.RUNPARM_FIELD_DELIMITER_DEFAULT;
        }


        /**
         *  fourth argument, "quoter" character for parsing
         *  lines in the RunParmFile, is also optional.
         */
        if (args.length > 3) {
            if (args[3] == null ||
                args[3].length() == 0) {
                quoter =
                    UtilConstants.RUNPARM_FIELD_QUOTER_DEFAULT;
            }
            else {
                if (args[3].length() != 1   ||
                    args[3].charAt(0) == ' ' ||
                    args[3].charAt(0) == delimiter) {
                    throw new IllegalArgumentException(
                        UtilConstants.ERRMSG_RUNPARM_ARG4_ERROR);
                }
                quoter = args[3].charAt(0);
            }
        }
        else {
            quoter =
                UtilConstants.RUNPARM_FIELD_QUOTER_DEFAULT;
        }

        delimitedTextParser =
            new DelimitedTextParser(delimiter,
                                    quoter);

        try {
            if (codeSet.length() > 0) {
                fileIn =
                    new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(
                                    fileName),
                                codeSet
                        )
                    );
            }
            else {
                fileIn =
                    new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(
                                    fileName)
                        )
                    );
            }
        }
        catch(FileNotFoundException e) {
            throw new IllegalArgumentException(
                UtilConstants.ERRMSG_RUNPARM_FILE_NOT_FOUND_1
                + fileName
                + UtilConstants.ERRMSG_RUNPARM_FILE_NOT_FOUND_2
                + e.getMessage());
        }


        inputLine = fileIn.readLine();
        if (inputLine == null) {
            throw new IllegalStateException(
                    UtilConstants.ERRMSG_RUNPARM_FILE_EMPTY);
        }

        lineCount = 1;
    }

    /**
     *  Checks to see if another line of input is available.
     *
     *  @return true if another line of input is available.
     */
    public boolean hasNext() {
        if (inputLine == null) {
            return false;
        }
        return true;
    }


    /**
     *  Returns next line of RunParmFile formatted as
     *  a fully parsed RunParmArrayEntry object.
     *
     *  @return RunParmArrayEntry object, ex-post parsing.
     *
     *  @throws IllegalStateException if called after EOF.
     *  @throws IllegalArgumentException if parsing problem.
     *  @throws IOException if I/O error on RunParmFile.
     */
    public RunParmArrayEntry next()
            throws  IOException,
                    IllegalArgumentException,
                    IllegalStateException {

        RunParmArrayEntry ae      = null;

        if (inputLine == null) {
            if (eofReached) {
                throw new IllegalStateException(
                    UtilConstants.ERRMSG_RUNPARM_NEXT_AFTER_EOF);
            }
            eofReached = true;
        }
        else {
            ++lineCount;
            delimitedTextParser.setParseString(inputLine);
            ae                    = new RunParmArrayEntry(
                                            delimitedTextParser);
            inputLine             = fileIn.readLine();
        }

        return ae;
    }

    /**
     *  Close RunParmFile.
     */
    public void close() throws IOException {
        fileIn.close();
        return;
    }

}

//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  BatchMMJ2.java  0.04 08/01/2008
 *
 *  Sep-03-2006:
 *      -->Comment out extraneous "import" statements. Duh.
 *      -->Fixed "" arg default handling bug (doh!)
 *      -->Check java.version for use of mmj2 with jar file.
 *  Version 0.04 -- Aug-01-2008:
 *      -->Add entry point for svc callbacks
 *      -->Move Java version check to constructor initiation.
 */

package mmj.util;

import mmj.svc.*;


/**
 *  BatchMMJ2 is the main batch mmj2 program to process a
 *  Metamath (.mm) file.
 *  <p>
 *  <b>Command Line args:</b>
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
 */
public class BatchMMJ2 extends BatchFramework {

    /**
     *  Default Constructor.
     */
    public BatchMMJ2() {
        super();
        BatchMMJ2.checkVersion();
    }

    /**
     *  Main function interfacing to Java environment,
     *  running the BatchMMJ2 functions.
     *
     * @param args see class description.
     */
    public static void main(String[] args) {

        BatchMMJ2 batchMMJ2 = new BatchMMJ2();

        int returnCode = batchMMJ2.runIt(args);
        if (returnCode != 0) {
            System.exit(returnCode);
        }
    }

    /*
     *  Secondary entry point for BatchMMJ2 to trigger
     *  a callback to the designated object.
     *  <p>
     *  To use this entry point insert the following
     *  code, or something like it, in your program:
     *  </p>
     *  <code>
     *
     *      BatchMMJ2 batchMMJ2 = new BatchMMJ2();
     *      String[] myArgs     = { "RunParms.txt" };
     *      MyMMJSvcCallback mySvcCallback
     *                          = new MyMMJSvcCallback();
     *      batchMMJ2.generateSvcCallback(myArgs,
     *                                    mySvcCallback);
     *
     *  </code>
     *  <p>
     *  See mmj2 for info about the content of the RunParms
     *  file, as well as the description for this class
     *  for information about other args.
     *  <p>
     *  The call to generateSvcCallback will run the
     *  BatchMMJ2 process in the normal way, and if
     *  there are no errors and a RunParm is encountered
     *  specifying execution of a SvcCallback, your
     *  SvcCallback's "go" method will be invoked.
     *  <p>
     *  From that point on your SvcCallback can utilize
     *  mmj2 objects as a "service" -- for example, you
     *  can unload the Metamath objects to s-expressions.
     *  Simply return from your SvcCallback.go() method
     *  to terminate.
     *  <p>
     *  <code>
     *  NOTE: It is not necessary to use this entry point.
     *        A SvcCallback class can be specified via
     *        mmj2 RunParm. If that is done then a "callback"
     *        can be generated via RunParm command, which,
     *        in effect, would make your callback a subroutine
     *        of BatchMMJ2. You would just need to ensure
     *        that your class is in the Java Classpath
     *        in use when BatchMMJ2 is run.
     *  </code>
     *  <p>
     *  <code>
     *  NOTE: The SvcCallback parameter passed in here will
     *        be overridden by a RunParm SvcCallback -- but
     *        the "Clear" RunParm does not erase a
     *        SvcCallback parameter passed via setSvcCallback
     *        (contrary to the behavior of "Clear" elsewhere.)
     *  </code>
     *  <p>
     * @param args see class description.
     * @param svcCallback your concrete instantiation of the
     *                    mmj SvcCallback interface.
     * @return zero if no errors, otherwise severe error
     *         is signified and processing should terminate!
     */
    public int generateSvcCallback(String[]    args,
                                   SvcCallback svcCallback) {

        if (!batchFrameworkInitialized) {
            initializeBatchFramework();
        }

        svcBoss.setSvcCallback(svcCallback);
        return runIt(args);
    }

    /*
     *  Checks to see that the version of Java is
     *  sufficiently advanced to support mmj2.
     *  <p>
     *  (((Java 1.5 is the minimum as of May 2008 but
     *  this is subject to change if absolutely necessary.)))
     *  <p>
     * @throws  IllegalArgumentException if Java version is
     *          too ancient.
     */
    public static void checkVersion()
                                throws IllegalArgumentException {
        String javaVersion        =
            System.getProperty(
                UtilConstants.JAVA_VERSION_PROPERTY_NAME);

        int maj                   =
            Integer.parseInt(javaVersion.substring(0,1));

        int min                   =
            Integer.parseInt(javaVersion.substring(2,3));

        if (maj < UtilConstants.JAVA_VERSION_MMJ2_MAJ ||
            (maj == UtilConstants.JAVA_VERSION_MMJ2_MAJ
             &&
             min < UtilConstants.JAVA_VERSION_MMJ2_MIN)) {
            throw new IllegalArgumentException(
                UtilConstants.JAVA_VERSION_MMJ2_RUNTIME_ERROR_MSG
                + javaVersion);
        }
    }
}

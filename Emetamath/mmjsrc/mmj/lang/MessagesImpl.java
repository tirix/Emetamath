//********************************************************************/
//* Copyright (C) 2005  MEL O'CAT  mmj2 (via) planetmath (dot) org   */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/**
 *  Messages.java  0.03 06/01/2007
 *
 *  Version 0.02 -- 08/23/2005
 *
 *  Version 0.03 -- 06/01/2007
 *      --> added startInstrumentationTimer() and
 *                stopInstrumentationTimer().
 */

package mmj.lang;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Hashtable;

/**
 *  Repository of error and informational messages during
 *  mmj processing.
 *  <p>
 *  The number of reported errors can be limited, thus
 *  providing a "governor" on processing (to a point).
 *  <p>
 *  Keeps track of the number of error and informational
 *  messages so that processing can be terminated if
 *  desired when no further output will be accumulated,
 *  particularly with error messages... The rationale
 *  of which is that if the Metamath .mm has basic
 *  Metamath language errors, such as invalid keywords,
 *  etc., there is no point in attempting Proof
 *  Verification or Syntax Verification. Also, processes
 *  that follow the System Load require a "clean" system,
 *  and the code assumes the integrity of LogicalSystem's
 *  data.
 *  <p>
 *  Various "print" functions in Messages accept a
 *  java.io.PrintWriter object so that output
 *  can be directed somewhere besides <b>System.out</b>. This
 *  also allows for the possibility of writing in
 *  other, non-default character sets such as UTF-8.
 */
public class MessagesImpl {

    /**
     *  Count of error messages stored in Messages object.
     */
    protected int      errorMessageCnt;

    /**
     *  Count of info messages stored in Messages object.
     */
     protected int      infoMessageCnt;

    /**
     *  String array of error messages in Messages object.
     */
    protected String[] errorMessageArray;

    /**
     *  String array of info messages in Messages object.
     */
    protected String[] infoMessageArray;

    protected Hashtable instrumentationTable;

    /**
     *  Default constructor using
     *  LangConstants.MAX_ERROR_MESSAGES_DEFAULT and
     *  LangConstants.MAX_INFO_MESSAGES_DEFAULT.
     */
    public MessagesImpl() {
        this(LangConstants.MAX_ERROR_MESSAGES_DEFAULT,
             LangConstants.MAX_INFO_MESSAGES_DEFAULT);
    }

    /**
     *  Constructor using max error/info message params.
     *
     *  @param maxErrorMessages max error messages to be stored.
     *  @param maxInfoMessages  max info messages to be stored.
     *
     *  @throws IllegalArgumentException if "max" params < 1.
     */
    public MessagesImpl(int maxErrorMessages,
                    int maxInfoMessages) {

        if (maxErrorMessages < 1) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_MAX_ERROR_MSG_LT_1);
        }
        if (maxInfoMessages < 1) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_MAX_INFO_MSG_LT_1);
        }

        errorMessageCnt          = 0;
        infoMessageCnt           = 0;

        errorMessageArray        = new String[maxErrorMessages];
        infoMessageArray         = new String[maxInfoMessages];
    }

    /**
     *  Reallocate error message array with new size.
     *
     *  @param maxErrorMessages max error messages to be stored.
     *
     *  @throws IllegalArgumentException if "max" param < 1.
     */
    public void reallocateErrorMessages(int maxErrorMessages) {
        if (maxErrorMessages < 1) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_MAX_ERROR_MSG_LT_1);
        }
        errorMessageCnt          = 0;
        errorMessageArray        = new String[maxErrorMessages];
    }

    /**
     *  Reallocate info message array with new size.
     *
     *  @param maxInfoMessages max info messages to be stored.
     *
     *  @throws IllegalArgumentException if "max" param < 1.
     */
    public void reallocateInfoMessages(int maxInfoMessages) {
        if (maxInfoMessages < 1) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_MAX_INFO_MSG_LT_1);
        }
        infoMessageCnt           = 0;
        infoMessageArray         = new String[maxInfoMessages];
    }


    /**
     *  Accum error message in Messages repository.
     *  <p>
     *  Stores the new message if there is room in the
     *  array.
     *
     *  @param errorMessage error message.
     *
     *  @return true if message stored, false if no room left.
     */
    public boolean accumErrorMessage(String errorMessage) {
        if (errorMessageCnt < errorMessageArray.length) {
            errorMessageArray[errorMessageCnt++]
                                  = errorMessage;
            return true;
        }
        return false;
    }

    /**
     *  Accum info message in Messages repository.
     *  <p>
     *  Stores the new message if there is room in the
     *  array.
     *
     *  @param infoMessage info message.
     *
     *  @return true if message stored, false if no room left.
     */
    public boolean accumInfoMessage(String infoMessage) {
        if (infoMessageCnt < infoMessageArray.length) {
            infoMessageArray[infoMessageCnt++]
                                  = infoMessage;
            return true;
        }
        return false;
    }

    /**
     *  Return count of error messages stored in Messages object.
     *  <p>
     *
     *  @return error message count.
     */
    public int getErrorMessageCnt() {
        return errorMessageCnt;
    }

    /**
     *  Check max error messages (table full).
     *  <p>
     *
     *  @return true if no room for more error messages, otherwise
     *          false.
     */
    public boolean maxErrorMessagesReached() {
        if (errorMessageCnt < errorMessageArray.length) {
            return false;
        }
        return true;
    }

    /**
     *  Return count of info messages stored in Messages object.
     *  <p>
     *
     *  @return info message count.
     */
    public int getInfoMessageCnt() {
        return infoMessageCnt;
    }

    /**
     *  Return error message array.
     *
     *  @return error message array.
     */
    public String[] getErrorMessageArray() {
        return errorMessageArray;
    }

    /**
     *  Return info message array.
     *
     *  @return info message array.
     */
    public String[] getInfoMessageArray() {
        return infoMessageArray;
    }

    /**
     *  Print all messages to System.out and clear message arrays.
     */
    public void printAndClearMessages() {
        printMessages();
        clearMessages();
    }

    /**
     *  Print all messages to System.out.
     */
    public void printMessages() {
        printInfoMessages();
        printErrorMessages();
    }

    /**
     *  Print all messages to printStream and
     *  clear message arrays.
     */
    public void printAndClearMessages(PrintStream printStream) {
        printMessages(printStream);
        clearMessages();
    }

    /**
     *  Print all messages to printStream.
     */
    public void printMessages(PrintStream printStream) {
        printInfoMessages(printStream);
        printErrorMessages(printStream);
    }

    /**
     *  Write all messages to printWriter and
     *  clear message arrays.
     */
    public void writeAndClearMessages(PrintWriter printWriter) {
        writeMessages(printWriter);
        clearMessages();
    }

    /**
     *  Write all messages to printWriter.
     */
    public void writeMessages(PrintWriter printWriter) {
        writeInfoMessages(printWriter);
        writeErrorMessages(printWriter);
    }

    /**
     *  Print error messages to System.out.
     */
    public void printErrorMessages() {
        printErrorMessages(System.out);
    }

    /**
     *  Print info messages to System.out.
     */
    public void printInfoMessages() {
        printInfoMessages(System.out);
    }

    /**
     *  Print error messages to printStream.
     */
    public void printErrorMessages(PrintStream printStream) {
        for (int i = 0; i < errorMessageCnt; i++) {
            printStream.println(errorMessageArray[i]);
        }
    }

    /**
     *  Write error messages to printWriter.
     */
    public void writeErrorMessages(PrintWriter printWriter) {
        for (int i = 0; i < errorMessageCnt; i++) {
            printWriter.println(errorMessageArray[i]);
        }
    }

    /**
     *  Print info messages to printStream.
     */
    public void printInfoMessages(PrintStream printStream) {
        for (int i = 0; i < infoMessageCnt; i++) {
            printStream.println(infoMessageArray[i]);
        }
    }

    /**
     *  Write info messages to printWriter.
     */
    public void writeInfoMessages(PrintWriter printWriter) {
        for (int i = 0; i < infoMessageCnt; i++) {
            printWriter.println(infoMessageArray[i]);
        }
    }

    /**
     *  Empty message arrays and reset counters to zero.
     */
    public void clearMessages() {
        errorMessageCnt = 0;
        infoMessageCnt  = 0;
    }

    /**
     *
     */
    public void startInstrumentationTimer(String timerID) {

        if (instrumentationTable == null) {
            instrumentationTable  = new Hashtable();
        }

        instrumentationTable.put(timerID.trim(),
                                 new InstrumentationTimer());
    }

    /**
     *
     */
    public void stopInstrumentationTimer(String inTimerID) {

        InstrumentationTimer tNow = new InstrumentationTimer();

        String timerID            = inTimerID.trim();

        if (instrumentationTable == null) {
            instrumentationTable  = new Hashtable();
        }

        InstrumentationTimer tThen
                                  =
            (InstrumentationTimer)instrumentationTable.get(timerID);

        if (tThen == null) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_TIMER_ID_NOTFND_1
                + timerID
                + LangConstants.ERRMSG_TIMER_ID_NOTFND_2);
        }

        accumInfoMessage(
            LangConstants.ERRMSG_TIMER_ID_1
            + timerID

            + LangConstants.ERRMSG_TIMER_ID_2
            + (tNow.millisTime -
               tThen.millisTime)

            + LangConstants.ERRMSG_TIMER_ID_3
            + tNow.totalMemory
            + LangConstants.ERRMSG_TIMER_ID_4
            + (tNow.totalMemory -
               tThen.totalMemory)

            + LangConstants.ERRMSG_TIMER_ID_5
            + tNow.maxMemory
            + LangConstants.ERRMSG_TIMER_ID_6
            + (tNow.maxMemory -
               tThen.maxMemory)

            + LangConstants.ERRMSG_TIMER_ID_7
            + tNow.freeMemory
            + LangConstants.ERRMSG_TIMER_ID_8
            + (tNow.freeMemory -
               tThen.freeMemory)
            + LangConstants.ERRMSG_TIMER_ID_9
            );
    }

    public class InstrumentationTimer {
        public long millisTime;
        public long freeMemory;
        public long totalMemory;
        public long maxMemory;
        public InstrumentationTimer() {
            millisTime            = System.currentTimeMillis();
            Runtime r             = Runtime.getRuntime();
            freeMemory            = r.freeMemory();
            totalMemory           = r.totalMemory();
            maxMemory             = r.maxMemory();
        }
    }
}

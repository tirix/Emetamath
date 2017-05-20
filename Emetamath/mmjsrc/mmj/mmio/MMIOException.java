//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * MMIOException.java  0.03 02/01/2006
 *
 *  Dec-22-2005:
 *  - add charNbr, store line, column and charNbr
 */

package mmj.mmio;


/**
 * Thrown when Metamath source file has a non-fatal error such
 * as a syntax error.
 */
public class MMIOException extends Exception {
    public SourcePosition position;

    /**
     * Default Constructor, <code>MMIOException</code>.
     */
    public MMIOException() {
        super();
    }

    /**
     * Contructor, <code>MMIOException</code> with
     * error message.
     *
     * @param   errorMessage  error message.
     */
    public MMIOException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Contructor, <code>MMIOException</code> with
     * source position and error message.
     *
     * @param   position      Source position of error
     * @param   errorMessage  error message.
     */
    public MMIOException(SourcePosition position,
                         String  errorMessage) {
        super(errorMessage
              + MMIOConstants.ERRMSG_TXT_SOURCE_ID
              + position.sourceId
              + MMIOConstants.ERRMSG_TXT_LINE
              + position.lineNbr
              + MMIOConstants.ERRMSG_TXT_COLUMN
              + position.columnNbr);
        this.position			  = position;
    }

    /**
     * Contructor, <code>MMIOException</code> with
     * file name, line number, column number and error message.
     *
     * @param   sourceId      String identifying source of error
     * @param   lineNbr       line number assigned to the error
     * @param   columnNbr     column number assigned to the error
     * @param   charNbr       character number of the error
     * @param   errorMessage  error message.
     */
    public MMIOException(Object  sourceId,
    					 long    lineNbr,
                         long    columnNbr,
                         long    charStartNbr,
                         long    charEndNbr,
                         String  errorMessage) {
        super(errorMessage
              + MMIOConstants.ERRMSG_TXT_SOURCE_ID
              + sourceId
              + MMIOConstants.ERRMSG_TXT_LINE
              + lineNbr
              + MMIOConstants.ERRMSG_TXT_COLUMN
              + columnNbr);
        position = new SourcePosition(sourceId, lineNbr, columnNbr, charStartNbr, charEndNbr);
    }
}

//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * ProofAsstException.java  0.01 02/01/2006
 */

package mmj.pa;

import mmj.mmio.SourcePosition;

/**
 *  Custom exception for ProofAsst.
 */
public class ProofAsstException extends Exception {

    public SourcePosition position;

    /**
     * Default Constructor.
     */
    public ProofAsstException() {
        super();
    }

    /**
     * Contructor with error message.
     *
     * @param   errorMessage  error message.
     */
    public ProofAsstException(String errorMessage) {
        super(errorMessage);
    }

    /**
     *  Contructor, <code>ProofAsstException</code> with
     *  line number, column number and error message.
     *  <p>
     *  Appends input stream line and column number to
     *  input message.
     *
     *  @param   lineNbr       line number assigned to the error
     *  @param   columnNbr     column number assigned to the error
     *  @param   charNbr       character number of the error
     *  @param   errorMessage  error message.
     */
    public ProofAsstException(SourcePosition position, String  errorMessage) {
        super(errorMessage
              + PaConstants.ERRMSG_TXT_LINE
              + position.lineNbr
              + PaConstants.ERRMSG_TXT_COLUMN
              + position.columnNbr);
        this.position              = position;
    }

    /**
     * Contructor, <code>ProofAsstException</code> with
     * character number (offset + 1) and error message.
     *
     * @param   charNbr       character number of the error
     * @param   errorMessage  error message.
     */
    public ProofAsstException(long    charNbr,
                              String  errorMessage) {
        super(errorMessage);
        this.position = new SourcePosition(null, -1, -1, charNbr-1, charNbr);
    }

}

//********************************************************************/
//* Copyright (C) 2005, 2006                                         */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  MMIOError.java  0.03 02/01/2006
 *
 *  Dec-22-2005:
 *  - add charNbr, store line, column and charNbr
 */

package mmj.mmio;


/**
 * Thrown when a parsing error is found in a MetaMath source stream.
 */
public class MMIOError extends Error {
	private SourcePosition position;

    /**
     * Default Constructor, <code>MMIOError</code>.
     */
    public MMIOError() {
        super();
    }

    /**
     * Contructor, <code>MMIOError</code> with
     * error message.
     *
     * @param   errorMessage  error message.
     */
    public MMIOError(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Contructor, <code>MMIOError</code> with
     * source position and error message.
     *
     * @param   position      Source position of error
     * @param   errorMessage  error message.
     */
    public MMIOError(SourcePosition position,
                         String  errorMessage) {
        super(errorMessage
              + MMIOConstants.ERRMSG_TXT_SOURCE_ID
              + position.sourceId
              + MMIOConstants.ERRMSG_TXT_LINE
              + position.lineNbr
              + MMIOConstants.ERRMSG_TXT_COLUMN
              + position.columnNbr);
        this.setPosition(position);
    }

    /**
     * Contructor, <code>MMIOError</code> with
     * file name, line number, column number and error message.
     *
     * @param   sourceId      String identifying location of error
     * @param   lineNbr       line number assigned to the error
     * @param   columnNbr     column number of the error
     * @param   charNbr       character number of the error
     * @param   errorMessage  error message.
     */
    public MMIOError(Object  sourceId,
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
        setPosition(new SourcePosition(sourceId, lineNbr, columnNbr, charStartNbr, charEndNbr));
    }

	/**
	 * @param position the position to set
	 */
	private void setPosition(SourcePosition position) {
		this.position = position;
	}

	/**
	 * @return the position
	 */
	public SourcePosition getPosition() {
		return position;
	}
}

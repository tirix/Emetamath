package mmj.lang;

import mmj.mmio.SourcePosition;
import mmj.pa.ErrorCode;
import mmj.pa.MMJException;

public interface MessageHandler {

   	/**
	 *  Accum error message in Messages repository.
	 *  <p>
	 *  Stores the new message if there is room in the
	 *  array.
	 *
	 *  @param e exception.
	 *
	 *  @return true if message stored, false if no room left.
	 */
    public abstract boolean accumException(MMJException e);
	public abstract boolean accumMessage(ErrorCode code, Object... args);
	public abstract boolean accumMessage(SourcePosition position, ErrorCode code, Object... args);

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
	public abstract boolean accumErrorMessage(String errorMessage, Object... args);

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
	public abstract boolean accumInfoMessage(String infoMessage, Object... args);
	public abstract boolean accumInfoMessage(SourcePosition position, String infoMessage, Object... args);
	
	/**
     *  Check max error messages (table full).
     *  <p>
     *
     *  @return true if no room for more error messages, otherwise
     *          false.
     */
	public abstract boolean maxErrorMessagesReached();

//    /**
//     *  Return count of error messages stored in Messages object.
//     *  <p>
//     *
//     *  @return error message count.
//     */
//	public abstract int getErrorMessageCnt();
//	public abstract int getInfoMessageCnt();
	boolean hasErrors();

    /**
     *  Obtain output message text from these messages.
     */
	public abstract String getOutputMessageText();
	public abstract String getOutputMessageTextAbbrev();
}
package mmj.lang;

import mmj.mmio.MMIOException;
import mmj.mmio.SourcePosition;

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
    public abstract boolean accumMMIOException(MMIOException e);

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
	public abstract boolean accumErrorMessage(String errorMessage);
	public abstract boolean accumErrorMessage(SourcePosition position, String errorMessage);

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
	public abstract boolean accumInfoMessage(String infoMessage);
	public abstract boolean accumInfoMessage(SourcePosition position, String errorMessage);

    /**
     *  Check max error messages (table full).
     *  <p>
     *
     *  @return true if no room for more error messages, otherwise
     *          false.
     */
	public abstract boolean maxErrorMessagesReached();

    /**
     *  Obtain output message text from these messages.
     */
	public abstract String getOutputMessageText();
}
package mmj.lang;

import mmj.mmio.MMIOException;

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

    /**
     *  Check max error messages (table full).
     *  <p>
     *
     *  @return true if no room for more error messages, otherwise
     *          false.
     */
	public abstract boolean maxErrorMessagesReached();
}
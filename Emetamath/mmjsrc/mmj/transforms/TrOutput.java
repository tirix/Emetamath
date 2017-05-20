//*****************************************************************************/
//* Copyright (C) 2014                                                        */
//* ALEXEY MERKULOV  steelart (dot) alex (at) gmail (dot) com                 */
//* License terms: GNU General Public License Version 2                       */
//*                or any later version                                       */
//*****************************************************************************/
package mmj.transforms;

import mmj.lang.MessageHandler;
import mmj.lang.Messages;
import mmj.pa.ErrorCode;

public class TrOutput {
    public TrOutput(final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    MessageHandler messageHandler; // for debug reasons

    public void errorMessage(final ErrorCode errorMessage,
        final Object... args)
    {
    	messageHandler.accumMessage(errorMessage, args);
    }

    public void dbgMessage(final boolean print, final ErrorCode infoMessage,
        final Object... args)
    {
        if (!print)
            return;

        messageHandler.accumMessage(infoMessage, args);
        // System.out.format(infoMessage + "\n", args);
    }
}

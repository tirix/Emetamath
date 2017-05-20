//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  StoreInMMTFolderTLRequest.java  0.01 08/01/2008
 *
 *  Version 0.01:
 *  ==> New.
 */

package mmj.tl;
import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.TheoremLoaderException;
import mmj.pa.ProofAsst;
import mmj.pa.ProofWorksheet;

/**
 *  StoreInMMTFolderTLRequest implements a user request for a
 *  TheoremLoader operation.
 *  <p>
 *  This class is basically just a name which is used by the
 *  ProofAssistantEditor to identify the user's request, and a call
 *  to implement the request.
 *  <p>
 *  The input ProofWorksheet must be already successfully unified,
 *  or else a TheoremLoaderException is thrown. The ProofWorksheet
 *  is converted to Metamath .mm format and stored in the MMT Folder,
 */
public class StoreInMMTFolderTLRequest extends TLRequest {

    /**
     *  Constructor for StoreInMMTFolderTLRequest
     */
    public StoreInMMTFolderTLRequest() {
    }

    /**
     *  Implements the request to store a ProofWorksheet in
     *  the MMT Folder.
     *  <p>
     *  @param theoremLoader TheoremLoader object.
     *  @param proofWorksheet ProofWorksheet object.
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param proofAsst ProofAsst object.
     *  @throws TheoremLoaderException if there are any data errors
     *         encountered while performing the requested function.
     */
    public void doIt(TheoremLoader  theoremLoader,
                     ProofWorksheet proofWorksheet,
                     LogicalSystem  logicalSystem,
                     MessageHandler messageHandler,
                     ProofAsst      proofAsst)
                                    throws TheoremLoaderException {
        theoremLoader.
            storeInMMTFolder(
                proofWorksheet,
                logicalSystem,
                messageHandler,
                proofAsst);

        messageHandler.
            accumInfoMessage(
                TlConstants.
                    ERRMSG_STORE_IN_MMT_FOLDER_NO_MSGS);
    }
}

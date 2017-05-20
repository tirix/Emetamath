//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  TheoremLoader.java  0.01 08/01/2008
 *
 *  Version 0.01:
 *      - new.
 */

package mmj.tl;
import java.util.LinkedList;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.Theorem;
import mmj.lang.TheoremLoaderException;
import mmj.pa.ProofAsst;
import mmj.pa.ProofWorksheet;

/**
 *  The Theorem Loader facility's main control module.
 *  <p>
 *  Note: this class is primarily an entry point and convenience
 *  for Theorem Loader users (batch or GUI.)
 */
public class TheoremLoader {

    private TlPreferences tlPreferences;

    /**
     *  Main constructor for TheoremLoader.
     *  <p>
     *  @param tlPreferences TlPreferences object.
     */
    public TheoremLoader(TlPreferences tlPreferences) {
        this.tlPreferences        = tlPreferences;
    }

    /**
     *  Unifies mmj2 Proof Text area and stores the theorem
     *  in the Logical System and MMT Folder.
     *  <p>
     *  @param proofWorksheetText text area holding an mmj2 Proof
     *                            Worksheet.
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param proofAsst ProofAsst object.
     *  @param inputProofWorksheetFileName String used for error
     *                                     reporting.
     *  @return unified ProofWorksheet object
     *  @throws TheoremLoaderException if data errors encountered,
     *          including the case where the ProofWorksheet cannot
     *          be unified.
     */
    public ProofWorksheet unifyPlusStoreInLogSysAndMMTFolder(
                        String         proofWorksheetText,
                        LogicalSystem  logicalSystem,
                        MessageHandler messageHandler,
                        ProofAsst      proofAsst,
                        String         inputProofWorksheetFileName)
                            throws TheoremLoaderException {

        ProofWorksheet proofWorksheet
                                  =
            getUnifiedProofWorksheet(proofWorksheetText,
                                     proofAsst,
                                     inputProofWorksheetFileName);

        storeInLogSysAndMMTFolder(proofWorksheet,
                                  logicalSystem,
                                  messageHandler,
                                  proofAsst);

        return proofWorksheet;
    }

    /**
     *  Unifies mmj2 Proof Text area and stores the theorem
     *  in the MMT Folder.
     *  <p>
     *  @param proofWorksheetText text area holding an mmj2 Proof
     *                            Worksheet.
     *  @param logicalSystem LogicalSystem object.
     *  @param messages Messages object.
     *  @param proofAsst ProofAsst object.
     *  @param inputProofWorksheetFileName String used for error
     *                                     reporting.
     *  @return unified ProofWorksheet object
     *  @throws TheoremLoaderException if data errors encountered,
     *          including the case where the ProofWorksheet cannot
     *          be unified.
     */
    public ProofWorksheet unifyPlusStoreInMMTFolder(
                            String         proofWorksheetText,
                            LogicalSystem  logicalSystem,
                            MessageHandler       messages,
                            ProofAsst      proofAsst,
                            String         inputProofWorksheetFileName)
                            throws TheoremLoaderException {

        ProofWorksheet proofWorksheet
                                  =
            getUnifiedProofWorksheet(proofWorksheetText,
                                     proofAsst,
                                     inputProofWorksheetFileName);

        storeInMMTFolder(proofWorksheet,
                         logicalSystem,
                         messages,
                         proofAsst);

        return proofWorksheet;
    }

    /**
     *  Stores a unified ProofWorksheet in the Logical System and
     *  the MMT Folder.
     *  <p>
     *  @param proofWorksheet ProofWorksheet object already
     *                                       successfully unified.
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param proofAsst ProofAsst object.
     *  @throws TheoremLoaderException if data errors encountered,
     *          including the case where the ProofWorksheet is not
     *          already unified.
     */
    public void storeInLogSysAndMMTFolder(
                                ProofWorksheet proofWorksheet,
                                LogicalSystem  logicalSystem,
                                MessageHandler messageHandler,
                                ProofAsst      proofAsst)
                                    throws TheoremLoaderException {

        storeInMMTFolder(proofWorksheet,
                         logicalSystem,
                         messageHandler,
                         proofAsst);

        loadTheoremsFromMMTFolder(proofWorksheet.getTheoremLabel(),
                                  logicalSystem,
                                  messageHandler);
    }

    /**
     *  Stores a unified ProofWorksheet in the MMT Folder.
     *  <p>
     *  @param proofWorksheet ProofWorksheet object already
     *                                       successfully unified.
     *  @param logicalSystem LogicalSystem object.
     *  @param messages Messages object.
     *  @param proofAsst ProofAsst object.
     *  @throws TheoremLoaderException if data errors encountered,
     *          including the case where the ProofWorksheet is not
     *          already unified.
     */
    public void storeInMMTFolder(
                                ProofWorksheet proofWorksheet,
                                LogicalSystem  logicalSystem,
                                MessageHandler       messages,
                                ProofAsst      proofAsst)
                                    throws TheoremLoaderException {

        if (proofWorksheet.getGeneratedProofStmt() == null) {
            throw new TheoremLoaderException(
                TlConstants.
                    ERRMSG_EXPORT_FORMAT_PROOF_WORKSHEET_ERR_2_1
                + proofWorksheet.getErrorLabelIfPossible());
        }

        MMTTheoremExportFormatter mmtTheoremExportFormatter
                                  =
            new MMTTheoremExportFormatter(tlPreferences);

        LinkedList mmtTheoremLines
                                  =
            mmtTheoremExportFormatter.
                buildStringBufferLineList(
                    proofWorksheet);

        tlPreferences.
            getMMTFolder().
                storeMMTTheoremFile(
                    proofWorksheet.getTheoremLabel(),
                    mmtTheoremLines);

    }

    /**
     *  Loads all MMT Theorems in the MMT Folder into the Logical
     *  System.
     *  <p>
     *  Note: the current MMT Folder is obtained from the
     *        TlPreferences object.
     *  <p>
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @throws TheoremLoaderException if data errors encountered.
     */
    public void loadTheoremsFromMMTFolder(
                                LogicalSystem logicalSystem,
                                MessageHandler messageHandler)
                                    throws TheoremLoaderException {

        MMTTheoremSet mmtTheoremSet
                                  =
            tlPreferences.
                getMMTFolder().
                    constructMMTTheoremSet(logicalSystem,
                    					   messageHandler,
                                           tlPreferences);

        mmtTheoremSet.updateLogicalSystem();
    }

    /**
     *  Loads one theorem from the MMT Folder into the Logical
     *  System.
     *  <p>
     *  Note: the input theoremLabel is used to construct the
     *        file name to be read from the MMT Folder.
     *  <p>
     *  @param theoremLabel label of the theorem to be loaded.
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @throws TheoremLoaderException if data errors encountered.
     */
    public void loadTheoremsFromMMTFolder(
                                String        theoremLabel,
                                LogicalSystem logicalSystem,
                                MessageHandler messageHandler)
                                    throws TheoremLoaderException {

        MMTTheoremSet mmtTheoremSet
                                  =
            tlPreferences.
                getMMTFolder().
                    constructMMTTheoremSet(theoremLabel,
                                           logicalSystem,
                                           messageHandler,
                                           tlPreferences);

        mmtTheoremSet.updateLogicalSystem();
    }

    /**
     *  Reads a theorem from the Logical System and writes it
     *  to the MMT Folder.
     *  System.
     *  <p>
     *  Note: the theorem Label is used to construct the
     *        file name to be written to the MMT Folder.
     *  <p>
     *  @param theorem Theorem to be written to the MMT Folder.
     *  @param logicalSystem LogicalSystem object.
     *  @param messages Messages object.
     *  @throws TheoremLoaderException if data errors encountered.
     */
    public void extractTheoremToMMTFolder(
                                Theorem       theorem,
                                LogicalSystem logicalSystem,
                                MessageHandler      messages)
                                    throws TheoremLoaderException {

        MMTTheoremExportFormatter mmtTheoremExportFormatter
                                  =
            new MMTTheoremExportFormatter(tlPreferences);

        LinkedList mmtTheoremLines
                                  =
            mmtTheoremExportFormatter.
                buildStringBufferLineList(
                    theorem);

        tlPreferences.
            getMMTFolder().
                storeMMTTheoremFile(theorem.getLabel(),
                                    mmtTheoremLines);
    }

    /**
     *  Unifies an mmj2 Proof Text area.
     *  <p>
     *  @param proofWorksheetText text of a ProofWorksheet.
     *  @param proofAsst ProofAsst object
     *  @param filenameOrDataSourceId text for diagnostics
     *  @return ProofWorksheet if unified successfully.
     *  @throws TheoremLoaderException if there is an error
     *          in the proof.
     */
    public ProofWorksheet getUnifiedProofWorksheet(
                                String    proofWorksheetText,
                                ProofAsst proofAsst,
                                String    filenameOrDataSourceId)
                                    throws TheoremLoaderException {

        ProofWorksheet proofWorksheet
                                  =
            proofAsst.unify(false, // renumReq
                            proofWorksheetText,
                            null,  // preprocessRequest
                            null,  // stepRequest
                            null,   //no TL request
                            -1);   // inputCursorPos

        if (proofWorksheet.getGeneratedProofStmt() == null) {
            throw new TheoremLoaderException(
                TlConstants.
                    ERRMSG_THEOREM_LOADER_TEXT_UNIFY_ERROR_1
                + proofWorksheet.getErrorLabelIfPossible()
                + TlConstants.
                    ERRMSG_THEOREM_LOADER_TEXT_UNIFY_ERROR_2
                + filenameOrDataSourceId
                + '\n'
                + proofWorksheet.getOutputMessageText());
        }

        return proofWorksheet;
    }

    public TlPreferences getTlPreferences() {
        return tlPreferences;
    }

}

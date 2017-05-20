//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  MMTFolder.java  0.01 08/01/2008
 *
 *  Version 0.01:
 *      --> new.
 */

package mmj.tl;
import java.io.File;
import java.util.List;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.TheoremLoaderException;


/**
 *   MMTFolder is a helper class for the Theorem Loader.
 */
public class MMTFolder {

    private File folderFile;

    /**
     *   Default constructor for MMTFolder.
     */
    public MMTFolder() {
        folderFile                = null;
    }

    /**
     *  Constructor for MMTFolder using pathname String.
     *  <p>
     *  @param mmtFolderName String containing absolute or
     *           relative pathname.
     *  @throws TheoremLoaderException if mmtFolderName is null, is blank,
     *              doesn't exist, is not a directory, or if there
     *              is a SecurityException.
     */
    public MMTFolder(String mmtFolderName)
                                throws TheoremLoaderException {

        if (mmtFolderName                 == null ||
            mmtFolderName.trim().length() == 0) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_NAME_BLANK_1);
        }

        try {
            folderFile            = new File(mmtFolderName);
            if (folderFile.exists()) {
                if (folderFile.isDirectory()) {
                    // okey dokey!
                }
                else {
                    throw new TheoremLoaderException(
                        TlConstants.ERRMSG_NOT_A_MMT_FOLDER_1
                        + mmtFolderName
                        + TlConstants.ERRMSG_NOT_A_MMT_FOLDER_2);
                }
            }
            else {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_MMT_FOLDER_NOTFND_1
                    + mmtFolderName
                    + TlConstants.ERRMSG_MMT_FOLDER_NOTFND_2);
            }

        }
        catch(SecurityException e) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_MISC_ERROR_1
                + mmtFolderName
                + TlConstants.ERRMSG_MMT_FOLDER_MISC_ERROR_2
                + e.getMessage());
        }
    }

    /**
     *  Constructor for MMTFolder using File object.
     *  <p>
     *  @param file File object.
     *  @throws TheoremLoaderException if input file is null,
     *              doesn't exist, is not a directory, or if there
     *              is a SecurityException.
     */
    public MMTFolder(File file)
                                throws TheoremLoaderException {
        if (file == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_FILE_NULL_1);
        }

        try {
            folderFile            = file;
            if (folderFile.exists()) {
                if (folderFile.isDirectory()) {
                    // okey dokey!
                }
                else {
                    throw new TheoremLoaderException(
                        TlConstants.ERRMSG_NOT_A_MMT_FOLDER_1
                        + folderFile.getAbsolutePath()
                        + TlConstants.ERRMSG_NOT_A_MMT_FOLDER_2);
                }
            }
            else {
                throw new TheoremLoaderException(
                    TlConstants.ERRMSG_MMT_FOLDER_NOTFND_1
                    + folderFile.getAbsolutePath()
                    + TlConstants.ERRMSG_MMT_FOLDER_NOTFND_2);
            }

        }
        catch(SecurityException e) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_MISC_ERROR_1
                + folderFile.getAbsolutePath()
                + TlConstants.ERRMSG_MMT_FOLDER_MISC_ERROR_2
                + e.getMessage());
        }
    }

    /**
     *  Returns the File object for the MMTFolder.
     *  <p>
     *  @return File object for the MMTFolder.
     */
    public File getFolderFile() {
        return folderFile;
    }

    /**
     *  Builds the MMTTheoremSet object for an MMTFolder
     *  using all the files in the folder with file type ".mmt".
     *  <p>
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param tlPreferences TlPreferences object.
     *  @return MMTTheoremSet the MMTTheoremSet object loaded with
     *                        the MMTFolder's data.
     *  @throws TheoremLoaderException if the MMTFolder File object
     *              is null or if there is an I/O error reading the
     *              directory.
     */
    public MMTTheoremSet constructMMTTheoremSet(
                            LogicalSystem   logicalSystem,
                            MessageHandler  messageHandler,
                            TlPreferences   tlPreferences)
                                throws TheoremLoaderException {

        if (folderFile == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_UNSPECIFIED_1);
        }

        File[] fileArray          =
            folderFile.listFiles(new MMTFileFilter());

        if (fileArray == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_READ_ERROR_1
                + folderFile.getAbsolutePath());
        }

        return new MMTTheoremSet(fileArray,
                                 logicalSystem,
                                 messageHandler,
                                 tlPreferences);

    }

    /**
     *  Builds an MMTTheoremSet object for a single theorem
     *  in a file in the MMTFolder.
     *  <p>
     *  @param theoremLabel  Metamath label of the theorem to
     *                       load into the MMTTheoremSet.
     *  @param logicalSystem LogicalSystem object.
     *  @param messageHandler MessageHandler object.
     *  @param tlPreferences TlPreferences object.
     *  @return MMTTheoremSet an MMTTheoremSet object loaded with
     *                        the MMTFolder's data for the requested
     *                        theorem.
     *  @throws TheoremLoaderException if the MMTFolder File object
     *              is null or if the theorem label is null or
     *              an empty string, or if the given theorem is not a
     *              valid MMT Theorem file in the directory (i.e. not
     *              found), or if there is a security exception.
     */
    public MMTTheoremSet constructMMTTheoremSet(
                            String          theoremLabel,
                            LogicalSystem   logicalSystem,
                            MessageHandler  messageHandler,
                            TlPreferences   tlPreferences)
                                throws TheoremLoaderException {

        if (folderFile == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_UNSPECIFIED_1);
        }

        MMTTheoremFile mmtTheoremFile
                                  =
            new MMTTheoremFile(this,
                               theoremLabel,
                               true); // true = input file

        return new MMTTheoremSet(mmtTheoremFile,
                                 logicalSystem,
                                 messageHandler,
                                 tlPreferences);
    }

    /**
     *  Stores a theorem in the MMTFolder as a MMT Theorem file.
     *  <p>
     *  Note: the input mmtTheoremLines List does not contain newline
     *        characters. Those are created by the program
     *        in a platform neutral manner.
     *  <p>
     *  @param theoremLabel  Metamath label of the theorem to
     *                       store into the MMTTheoremSet.
     *  @param mmtTheoremLines List of StringBuffer objects with
     *                       one line (no newline!) per StringBuffer
     *                       object, already formatted into Metamath
     *                       .mm format.
     *  @return MMTTheoremFile the output MMTTheoremFile object as
     *              created during the store operation.
     *  @throws TheoremLoaderException if the MMTFolder File object
     *              is null or if the theorem label is null or
     *              an empty string, or if there is an I/O error
     *              during the attempt to create an MMTTheoremFile.
     */
    public MMTTheoremFile storeMMTTheoremFile(String theoremLabel,
                                              List   mmtTheoremLines)
                                    throws TheoremLoaderException {

        if (folderFile == null) {
            throw new TheoremLoaderException(
                TlConstants.ERRMSG_MMT_FOLDER_UNSPECIFIED_1);
        }

        MMTTheoremFile mmtTheoremFile
                                  =
            new MMTTheoremFile(this,
                               theoremLabel,
                               false); //false = not input file

        mmtTheoremFile.writeTheoremToMMTFolder(mmtTheoremLines);

        return mmtTheoremFile;
    }

    /**
     *  Returns the absolute pathname of the MMTFolder.
     *  <p>
     *  @return Absolute pathname of the MMTFolder or null if the
     *              underlying File is null.TheoremFile.
     */
    public String getAbsolutePath() {
        if (folderFile == null) {
            return null;
        }
        return folderFile.getAbsolutePath();
    }

}

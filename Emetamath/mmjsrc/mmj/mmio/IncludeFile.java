//********************************************************************/
//* Copyright (C) 2005  MEL O'CAT  mmj2 (via) planetmath (dot) org   */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  IncludeFile.java  0.02 08/20/2005
 */
package mmj.mmio;
import java.io.*;
import java.util.*;

import mmj.util.Progress;

/**
 * Nitty-gritty IncludeFile work switching Tokenizers and
 * keeping a list, checking it twice.
 * <p>
 * Strategy overview:
 * The top level MetaMath source file Tokenizer is never closed
 * as include files are processed, but a nested include file
 * statement will close the outer include file, process the
 * inner and then "restart" the outer include file where it
 * left off. This is slightly crude but is a fair compromise
 * since nested include files are unlikely AND there is
 * probably an extra file handle hanging around, so we don't
 * need to close the top level MetaMath source file.
 */

public class IncludeFile {
	private static	ReaderProvider	readerProvider		= new DefaultReaderProvider();
    private     String      	fileName                = null;
    private     long        	restartCharsToBypass    = 0;
    private     Tokenizer   	tokenizer               = null;
    private     Tokenizer   	prevTokenizer           = null;


	public static Reader openFile(String fileName, Progress loadProgress) throws IOException {
        Reader readerIn = readerProvider.createReader(fileName);
        loadProgress.addTask(readerProvider.getSize(fileName));
        
        return readerIn;
	}

	/**
     * <p>
     * Switches Statementizer processing to an include file
     * after recording restart information of the previous
     * include file.
     * <p>
     * Note that the first
     * include file entry in <code>fileArrayList</code>
     * stores the <code>Tokenizer</code> of the top level
     * MetaMath source file read; this is used to restore
     * Statementizer processing where it left off after
     * end of file on the include file.
     * <p>
     *
     * @param fileList  ArrayList used to store information
     *        about IncludeFiles. Initialize to empty list
     *        at start of processing:
     *        <code> fileList = new ArrayList();</code>.
     *        and that's all that is necessary.
     *
     * @param f  File object, previously constructed, that
     *        will be used to create a Reader for the new
     *        include file.
     *
     * @param fileName  include string file name from
     *        Metamath file '$[ xx.mm $]' statement.
     *
     * @param statementizer  the Statementizer presently
     *        in use; used here to switch tokenizers.
     *
     * @return returns Tokenizer for the included file
     *        to which the input Statementizer has been
     *        switched.
     *
     * @throws    FileNotFoundException if bogus include file name.
     */
    public static Tokenizer initIncludeFile(
                                ArrayList<IncludeFile> fileList,
                                Progress  loadProgress,
                                String        fileName,
                                Statementizer statementizer)
                                    throws FileNotFoundException,
                                           IOException {

        if (!fileList.isEmpty()) {
            IncludeFile prevI     =
                (IncludeFile)fileList.get(fileList.size() - 1);
            prevI.restartCharsToBypass
                                  =
                prevI.tokenizer.getCurrentCharNbr();
            prevI.tokenizer.close();
        }

        IncludeFile i             = new IncludeFile();
        i.fileName                = fileName;
        i.restartCharsToBypass    = 0;
        i.tokenizer               =
            new Tokenizer(
            	readerProvider.createReader(fileName),
                readerProvider.getSource(fileName));
        i.prevTokenizer           = statementizer.setTokenizer(
                                                        i.tokenizer);
        fileList.add(i);

        loadProgress.addTask(readerProvider.getSize(fileName));

        return i.tokenizer;
    }

    /**
     *  Terminates processing of the current include file,
     *  "pops the stack", and restores the previous include file
     *  for further Statementizer processing.
     *
     *  @param fileList  ArrayList used to store information
     *         about IncludeFiles.
     *
     *  @param statementizer  the Statementizer presently
     *         in use; used here to switch tokenizers.
     *
     *  @return returns Tokenizer to which the Statementizer
     *         has been switched (it will be either the original
     *         top level Tokenizer or an include file tokenizer).
     *
     *  @throws    FileNotFoundException if bogus include file name.
     */
    public static Tokenizer termIncludeFile(
                                ArrayList     fileList,
                                Statementizer statementizer)
                                    throws FileNotFoundException,
                                           IOException {
        Tokenizer retTokenizer;

        if (fileList.isEmpty()) {
            throw new IllegalArgumentException(
                MMIOConstants.ERRMSG_INCLUDE_FILE_ARRAY_EMPTY);
        }

        // closes current file and tokenizer and removes from fileList
        IncludeFile currI         =
                (IncludeFile)fileList.get(fileList.size() - 1);
        currI.tokenizer.close();

        // save previous -- this will be the top level, original,
        // Metamath source tokenizer, still open and ready to go
        // if this is the only remaining include file in fileList.
        retTokenizer              = currI.prevTokenizer;

        fileList.remove(fileList.size() - 1);

        if (fileList.isEmpty()) {
            statementizer.setTokenizer(retTokenizer);
        }
        else {
            /**
             *  otherwise...we are terminating a nested include
             *  file... then recreate its Tokenizer using the
             *  "skipahead" constructor to reposition to the character
             *  where it left off (when the $[ xx.mm $] include
             *  statement was read.)
             */
            currI                 =
                (IncludeFile)fileList.get(fileList.size() - 1);

            currI.tokenizer       =
                new Tokenizer(
                	readerProvider.createReader(currI.fileName),
                	readerProvider.getSource(currI.fileName),
                    currI.restartCharsToBypass);

            currI.restartCharsToBypass
                                  = 0;
            retTokenizer          = currI.tokenizer;
        }
        return retTokenizer;
    }

    public static void setReaderProvider(ReaderProvider readerProvider) {
    	IncludeFile.readerProvider = readerProvider;
    }
    
    public static interface ReaderProvider {
    	public Reader createReader(String fileName) throws FileNotFoundException;
		public long getSize(String fileName) throws FileNotFoundException;
    	public Object getSource(String fileName);
    	public String getFileName(Object sourceId);
    }

    public static class DefaultReaderProvider implements ReaderProvider {
		@Override
    	public Reader createReader(String fileName) throws FileNotFoundException {
    		return new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(fileName)
                    ),
                MMIOConstants.READER_BUFFER_SIZE
                );
    		}

		@Override
		public long getSize(String fileName) throws FileNotFoundException {
			File f = new File(fileName);
			return f.length();
		}

		@Override
		public Object getSource(String fileName) {
			return fileName;
		}

		@Override
		public String getFileName(Object sourceId) {
			return (String)sourceId;
		}
    }
}

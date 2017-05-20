//********************************************************************/
//* Copyright (C) 2005, 2006, 2008                                   */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * @(#)Tokenizer.java  0.03 08/01/2008
 *
 * Dec-22-2006
 * --> added charNbr to MMIOError
 *
 * Version 0.03 -- 08/01/2008
 * --> Modified close() to mask IOException for ease of use.
 */

package mmj.mmio;

import java.io.*;

/**
 * Parse a Metamath .mm file into Metamath tokens.
 * <p>
 * This class (presently) provides input services for a
 * MetaMath.mm file. Its primary purpose is to return
 * MetaMath tokens, but if desired, the whitespace
 * in between tokens can be obtained, as well as other
 * information.
 * <p>
 * Note: this file validates the input file to ensure
 *       that only valid characters are present. These
 *       are the various 7-bit ASCII printable characters and
 *       white space characters documented in metamath.pdf
 *       at www.metamath.org. ANSI or DOS (cr or cr/lf)
 *       line terminators are accepted and are considered
 *       part of the white space. Invalid characters
 *       will generate a MMIOError exception
 *       (a FAR more serious problem than a mere
 *       MetaMathParseException exception ;-)
 * <p>
 * Note: Tokenizer should work with UTF-8 and other
 *       character sets that code the 7-bit ASCII
 *       characters the same way(?), but because it
 *       uses hard coded values it would need to be
 *       cloned if someone wants to be able to input
 *       full Unicode (an interface could be developed.)
 * <p>
 * Note: This class does not expand MetaMath include
 *       statements (i.e. "$[ infiniteset.mm $]"). That
 *       feat can be handled in the parsing logic -- this
 *       is just a simple, uneducated tokenizer class.
 *       Or....conveniently use the technique described
 *       in mmverify.py at www.metamath.org.
 * <p>
 *       (from mmverify.py):
 *
 *       "Before using this program, any compressed
 *        proofs must be expanded with the Metamath
 *        program, e.g.:
 * <p>
 *        <code>$ ./metamath 'r set.mm' 'sa p *' 'w s set.mm' q > /dev/null</code>
 * <p>
 *        That somewhat cryptic (unix/linux) command says
 *        1) Read file set.mm; 2) Save all proofs in default
 *        format (uncompressed); 3) Save to disk; and
 *        4) (?) send Metamath output commentary about what
 *        it is being asked to do to to the bit bucket...
 * <p>
 *        Here's a version from a Windows ".bat" file that
 *        saves the expanded file to a different file name:
 * <p>
 *        <code>metamath.exe "r myset.mm" "v proof *" "sa p *" "w s expmyset.mm" "exit" >> expmyset.txt</code>
 *
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public class Tokenizer
        extends Object {

    private   Reader            reader                  = null;
    private   Object            sourceId                = null;

    private   long              lineNbr                 = 1;
    private   long              columnNbr               = 0;
    private   long              charNbr                 = 0;
    private   long              lastCharNbr             = 0;

    private   long              lastProgress            = 0;

    private   int               prevChar                = -1;
    private   int               currChar                = -1;
    private   int               nextChar                = -1;

    /**
     * Constructs Tokenizer from a Reader.
     *
     * @param r Reader (may be StringReader or BufferedReader
     *          but PushbackReader and LineNumberReader are
     *          not helpful choices.)
     *
     * @param s Source Id Text, such as filename or test ID.
     *        May be empty string if N/A. Used solely for
     *        diagnostic messages.
     *
     * @throws IOException if I/O error
     *
     */
    public Tokenizer(Reader  r, Object s)
            throws  IOException {

        reader                  = r;
        sourceId                = s;

        nextChar = reader.read();
        if (nextChar == -1) {
            lineNbr = 0;
        }
        else {
            nextChar &= 0x00ff;
        }

        return;
    }

    /**
     * Constructs Tokenizer from a Reader, with "skipahead n",
     * where n = nbrCharsToBypass.
     *
     * @param r Reader (may be StringReader or BufferedReader
     *          but PushbackReader and LineNumberReader are
     *          not helpful choices.)
     *
     * @param s Source Id Text, such as filename or test ID.
     *        May be empty string if N/A. Used solely for
     *        diagnostic messages.
     *
     * @param nbrCharsToBypass Used to reposition reader
     *        with previously returned charNbr from
     *        <code>getCurrentCharNbr</code> method.
     *
     * @throws IOException if I/O error
     * @throws IllegalArgumentException if <code>nbrCharsToBypass
     *         is less than zero.
     *
     */
    public Tokenizer(Reader r, String s, long nbrCharsToBypass)
            throws  IOException,
                    IllegalArgumentException {
        this(r, s);
        if (nbrCharsToBypass < 0) {
            throw new IllegalArgumentException();
        }

        while (nbrCharsToBypass > charNbr &&
               (getChar()) != -1) {
        }

        if (nbrCharsToBypass != charNbr) {
            throw new MMIOError(
                sourceId,
                lineNbr,
                columnNbr,
                lastCharNbr,
                charNbr,
                MMIOConstants.ERRMSG_SKIP_AHEAD_FAILED + charNbr);
        }
        return;
    }

    /**
     * <p>Gets next MetaMath token from the input file and
     * stores it in <code>strBuf</code>.
     *
     * @param strBuf the <code>StringBuffer</code> where the next
     *               available MetaMath token will be inserted
     * @param offset insertion point offset in strBuf for token. (If
     *               strBuf contains 3 characters then set offset to
     *               3 because it is relative to zero.)
     *
     * @return length of token, or -1 if EOF reached.
     *
     * @throws IOException if I/O error
     * @throws MMIOError if invalid character read
     */
    public int getToken(StringBuffer strBuf, int offset)
            throws IOException {

        int x;
        int len = 0;

        /**
         *  bypass any whitespace (we can code a getWhiteSpace routine
         *  if desired because we always peek ahead before doing the
         *  get, and at the end of a token, the following whitespace,
         *  if any, still lies ahead.)
         */
        while (((x = peekNextChar()) != -1)
                &&
                ((MMIOConstants.VALID_CHAR_ARRAY[x] &
                  MMIOConstants.WHITE_SPACE)
                  != 0)) {
            getChar();
        }
        lastCharNbr = charNbr;
        
        if (x == -1) {
            len = -1;
        }
        else {
            /**
             *  get the printables as a single token, stopping at EOF
             *  or an end-comment, making sure not to read beyond the
             *  end of the token.
             */
            do {
                strBuf.insert(offset++, (char)getChar());
                ++len;
            } while (((x = peekNextChar()) != -1)
                     &&
                     ((MMIOConstants.VALID_CHAR_ARRAY[x] &
                       MMIOConstants.PRINTABLE)
                       != 0));
        }

        return len;
    }

    /**
     * Gets next chunk of whitespace from MetaMath file
     * and stores it in <code>strBuf</code>.
     *
     * @param strBuf the <code>StringBuffer</code> where the next
     *               whitespace chunk will be inserted
     * @param offset insertion point offset in strBuf for whitespace
     *
     * @return length of whitespace, zero if next char on file is
     *         start of token, or -1 if EOF reached.
     *
     * @throws       IOException if I/O error
     * @throws       MMIOError if invalid character read
     */
    public int getWhiteSpace(StringBuffer strBuf, int offset)
            throws IOException {

        int x;
        int len = 0;
        while (((x = peekNextChar()) != -1)
               &&
                ((MMIOConstants.VALID_CHAR_ARRAY[x] &
                  MMIOConstants.WHITE_SPACE)
                != 0)) {
            strBuf.insert(offset++, (char)getChar());
            ++len;
        }
        if ((len == 0) && (x == -1)) {
            return -1;
        }
        else {
            return len;
        }
    }

    /**
     * Return current line number in the file.
     * <p>
     * Current line number corresponds to the line
     * number of the last token or whitespace chunk
     * returned.
     * <p>
     * Line number is incremented when a character
     * is processed AFTER end of line.
     * <p>
     * Note: "cr/lf" and "cr" are valid line
     *       terminators. Using them in combination
     *       is inadvisable. The algorithm used here
     *       differs from the Java language
     *       algorithm in that cr/cr/lf is counted
     *       as TWO line terminators instead of
     *       just one (and cr/cr/cr/lf is 3, etc.)
     *
     * @return current line number in the file (starts
     *         with 1).
     */
    public long getCurrentLineNbr() {
        return lineNbr;
    }

    /**
     * Return current column number in the current
     * line in the file.
     * <p>
     * Note: tab characters are ignored for purposes
     *       of computing column number.
     *
     * @return current column number (starts with 1).
     *
     */
    public long getCurrentColumnNbr() {
        return columnNbr;
    }

    /**
     * Return last character number in the file.
     * <p>
     * Following getToken or getWhiteSpace this will
     * be the character number of the last character
     * returned in the token or whitespace.
     * <p>
     * Intended to be used with startPosition
     * method, say, with MetaMath file include
     * processing which closes a reader and
     * then re-starts where it left off.
     *
     * @return current column number (starts with 1).
     */
    public long getLastCharNbr() {
        return lastCharNbr;
    }

    /**
     * Return current character number in the file.
     * <p>
     * Following getToken or getWhiteSpace this will
     * be the character number of the last character
     * returned in the token or whitespace.
     * <p>
     * Intended to be used with startPosition
     * method, say, with MetaMath file include
     * processing which closes a reader and
     * then re-starts where it left off.
     *
     * @return current column number (starts with 1).
     */
    public long getCurrentCharNbr() {
        return charNbr;
    }

    /**
     * Get "Source Id" for the file (for use in
     * error/diagnostic/testing messages.)
     *
     * @return sourceId
     */
    public Object getSourceId() {
        return sourceId;
    }

    /**
     * Returns an object containing complete position
     * information about the current token  
     * @return
     */
	public SourcePosition getCurrentPosition() {
		return new SourcePosition(sourceId, lineNbr, columnNbr, lastCharNbr, charNbr);
	}

    /**
     * Returns an object containing complete position
     * information about the current token, whereas the start position is specified
     * @return
     */
	public SourcePosition getCurrentPositionStartingAt(long startCharNbr) {
		return new SourcePosition(sourceId, lineNbr, columnNbr, startCharNbr, charNbr);
	}

	/**
     * Close file/reader.
     */
    public void close() {

        try {
            if (reader != null) {
                reader.close();
            }
        }
        catch (Exception e) {
        }

        return;
    }

    /**
     * Count number of lines in input file.
     * <p>
     * This is provided for testing purposes. Delete if desired.
     * Since rewind() is not supported, use of countNbrLines
     * cannot be mixed with other functions -- it is a one-shot
     * use of a newly constructed Tokenizer, and a new
     * Tokenizer would need to be constructed for other
     * uses.
     *
     * @return number of lines in the file.
     *
     * @throws       IOException if I/O error
     */
    public long countNbrLines()
            throws IOException {
        while (getChar() != -1) {
        }
        return getCurrentLineNbr();
    }

    /**
     * Returns the next character from the file after validating
     * it.
     * <p>
     * Also keeps track of current line and column number
     * within the file. Returns -1 at end of file.
     * <p>
     * From Metamath.pdf, 4.1:
     * <p>
     * The only characters that are allowed to appear in a Metamath
     * source file are the 94 printable characters on standard ascii
     * keyboards, which are digits, upper and lower case letters,
     * and the following 32 special characters (plus the following
     * non-printable (white space) characters: space, tab, carriage
     * return, line feed, and form feed.:
     * <ul>
     * <li> <code>` ~ ! @ # $ % ^ & * ( ) - _ = + </code>
     * <li> <code>[ ] { } ; : ' " , . < > / ? \ | </code>
     *
     * @return       ASCII (7-BIT!)character read (as integer) or
     *               -1 if EOF reached
     *
     * @throws       IOException if I/O error
     * @throws       MMIOError if invalid character read
     *
     */
    private   int getChar()
                        throws  MMIOError,
                                IOException {
        prevChar = currChar;
        if (nextChar == -1) {
            return -1;
        }
        currChar = nextChar;
        nextChar = reader.read();
        if (nextChar != -1) {
            nextChar &= 0x00ff;
        }

        ++columnNbr;
        ++charNbr;

        if (prevChar == '\n') {
            ++lineNbr;
            columnNbr = 1;
        }
        else {
            if (prevChar == '\r' &&
                currChar != '\n') {
                ++lineNbr;
                columnNbr = 1;
            }
        }

        if (currChar >= 0 &&
            MMIOConstants.VALID_CHAR_ARRAY[currChar] > 0) {
        }
        else {
            throw new MMIOError(
                sourceId,
                lineNbr,
                columnNbr,
                lastCharNbr,
                charNbr,
                MMIOConstants.ERRMSG_INV_INPUT_CHAR + currChar);
        }

        return currChar;
    }

    /**
     * Non-destructive "peek" at next character in the file.
     *
     * @return next character, valid or not.
     *
     * @throws       IOException if I/O error
     */
    private int peekNextChar() {
        return nextChar;
    }

	public long getProgress() {
		long progress = charNbr - lastProgress;
		lastProgress = charNbr;
		return progress;
	}
}

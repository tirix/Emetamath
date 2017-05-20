//********************************************************************/
//* Copyright (C) 2005-2011                                          */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * Systemizer.java  0.08 11/01/2011
 *
 * Sep-25-2005
 *     -> do finalizeEOF even if error messages found so that
 *        final End Scope can be checked (LogicalSystem
 *        already performs the error message count checking.)
 * Dec-10-2005
 *     -> Add LoadLimit inner class and associated methods
 *        for setting load limits:
 *
 *            setLimitLoadEndpointStmtNbr(int stmtNbr)
 *            setLimitLoadEndpointStmtLabel(int stmtLabel)
 *
 *        Call these after construction of Systemizer but
 *        before loading file(s).
 *
 *        (NOTE: There is no way to reset the LoadLimit after
 *        use -- a reset could be added but there appears
 *        to be no use for it.)
 * Dec-22-2005
 *     -> Added character number (offset + 1) to
 *        MMIOException
 *
 * Apr-1-2006: Version 0.05:
 *     -> Added compressed proof code.
 *
 * Nov-1-2006: Version 0.06:
 *     -> Added logic to load input comments and
 *        to store Theorem description.
 *     -> Added logic to *not* load proofs
 *
 * Aug-01-2008: Version 0.07:
 *     -> Modified loadAxiom() to load the MObj.description
 *        using the curr SrcStmt's comment, if LoadComments
 *        is "on" (previously only theorem comments were
 *        hoovered up.)
 *
 * Nov-01-2011: Version 0.08:
 *     -> Modified loadComment() to grab $t comments for GMFF.
 *     -> Modified for mmj2 Paths Enhancement:
 *        -> add "path" argument to all load() methods.
 */

package mmj.mmio;

import java.io.*;
import java.util.*;

import mmj.lang.*;
import mmj.mmio.MMIOConstants.FileContext;
import mmj.mmio.Systemizer.DependencyListener;
import mmj.pa.MMJException;
import mmj.util.Progress;

/**
 * Feed {@code SystemLoader} interface with {@code SrcStmt} objects from
 * {@code Statementizer}.
 * <p>
 * Notes:
 * <ul>
 * <li>Intercepts include statements and converts them into {@code SrcStmt}
 * objects -- transparently with respect to {@code SystemLoader}.
 * <li>Keeps track of end of file and notifies {@code SystemLoader} of that
 * condition.
 * <li>Has no concept of comment statements that are embedded inside other
 * statements -- if comments are ever to be used, this needs a redesign!
 * </ul>
 *
 * @see <a href="../../MetamathERNotes.html"> Nomenclature and
 *      Entity-Relationship Notes</a>
 */
public class Systemizer {

    private Tokenizer tokenizer = null;
    private Statementizer statementizer = null;

    private MessageHandler messageHandler;

    private SystemLoader systemLoader;

    private boolean eofReached = false;
    private Deque<IncludeFile> fileList = new ArrayDeque<>(); // the stack of include files
    private final List<String> filesAlreadyLoaded = new ArrayList<>();

    private SrcStmt currSrcStmt = null;

    private final LoadLimit loadLimit = new LoadLimit();

    private boolean loadComments = MMIOConstants.LOAD_COMMENTS_DEFAULT;

    private boolean loadProofs = MMIOConstants.LOAD_PROOFS_DEFAULT;

    private Progress loadProgress = null;

    private final List<String> defaultProofList = new ArrayList<>(
        Arrays.asList(MMIOConstants.MISSING_PROOF_STEP));

    private   DependencyListener	dependencyListener = null;

    /**
     * Initialize (or re-initialize) a {@code Systemizer} from a
     * {@code Messages} object and a {@code SystemLoader} object.
     *
     * @param messages -- repository of error and info messages that provides a
     *            limit on the number of errors output before processing is
     *            halted.
     * @param systemLoader -- a SystemLoader initialized with any customizing
     *            parameters and ready to be loaded with data.
     * @param loadProofs If true then Metamath proofs are loaded into
     *            LogicalSystem, otherwise just a "?" proof is stored with each
     *            Theorem.
     * @param loadComments If true then Metamath comments (at least, for
     *            Theorems) will be loaded.
     * @param loadEndpointStmtLabel last Metamath statement label to load.
     * @param loadEndpointStmtNbr maximum number of Metamath statements to be
     *            loaded.
     */
    public void init(final MessageHandler messageHandler, final SystemLoader systemLoader,
        final int loadEndpointStmtNbr, final String loadEndpointStmtLabel,
        final boolean loadComments, final boolean loadProofs)
    {
        this.messageHandler = messageHandler;
        this.systemLoader = systemLoader;
        loadLimit.loadEndpointStmtNbr = loadEndpointStmtNbr;
        loadLimit.loadEndpointStmtLabel = loadEndpointStmtLabel;
        this.loadComments = loadComments;
        this.loadProofs = loadProofs;
    }

    public void setLoadProgress(Progress loadProgress) {
    	this.loadProgress = loadProgress;
    }
    
    public void setDependencyListener(DependencyListener dependencyListener) {
    	this.dependencyListener = dependencyListener;
    }
    
    /**
     * Get SystemLoader, as-is.
     *
     * @return -- SystemLoader structure in use.
     */
    public SystemLoader getSystemLoader() {
        return systemLoader;
    }

    /**
     * Get {@code MessageHandler} object
     *
     * @return {@code MessageHandler} object
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Reads MetaMath source file via {@code SystemLoader}.
     * <p>
     * Note: multiple files can be loaded in serial fashion.
     *
     * @param sourceId -- source of the data to be read.
     * @return {@code Messages} object, which can be tested to see if any error
     *         messages were generated
     * @see mmj.lang.MessageHandler#getErrorMessageCnt() MessageHandler.getErrorMessageCnt()
     * @throws IOException if I/O error
     */
    public MessageHandler read(final Source sourceId) throws IOException
    {

        tokenizer = new Tokenizer(sourceId);
        statementizer = new Statementizer(tokenizer);

        eofReached = false;
        getNextStmt();
        if (eofReached && !messageHandler.hasErrors())
            handleParseException(
                new MMIOException(MMIOConstants.ERRMSG_INPUT_FILE_EMPTY));
        else {
            while (!eofReached && !messageHandler.maxErrorMessagesReached()) {
                loadStmt();
                if (loadLimit.endpointReached) {
                    finalizePrematureEOF();
                    tokenizer.close();
                    return messageHandler;
                }
                getNextStmt();
            }
            if (eofReached == true)
                finalizeEOF();
        }
        tokenizer.close();
        return messageHandler;
    }

    /**
     * Clone of Load function using fileNameIn instead of readerIn
     *
     * @param  source the source of the data to be loaded.
     * @return {@code Messages} object, which can be tested to see if any error
     *         messages were generated
     * @throws MMIOException if file requested has already been loaded or does
     *             not exist.
     */
    public MessageHandler load(final Source source) throws MMIOException
    {
        try {
            if(isInFilesAlreadyLoaded(filesAlreadyLoaded, source))
                throw new MMIOException(MMIOConstants.ERRMSG_LOAD_REQ_FILE_DUP,
                    source.toString());
            loadProgress.addTask(source.getSize());
        } catch (final IOException e) {
            throw new MMIOException(MMIOConstants.ERRMSG_LOAD_REQ_FILE_NOTFND,
            		source.toString());
        }

        try {
            return read(source);
        } catch (final IOException e) {
            throw new MMIOException(e, MMIOConstants.ERRMSG_LOAD_MISC_IO,
            		source.toString(), e.getMessage());
        }
    }

    public void clearFilesAlreadyLoaded() {
    	filesAlreadyLoaded.clear();
    	fileList.clear();
    }
    
    // =========================================================

    /**
     * Get next SrcStmt from Statementizer. This is a weird little routine
     * because it must check to see if end of file (eof -- indicated by a null
     * SrcStmt from Statementizer) is simply the end of an include file, and if
     * so, pop the include file stack and go get the NEXT statement from the
     * parent file. Also, it must bypass statements with errors and keep
     * processing until the maximum number of parse errors is reached, or end of
     * file.
     *
     * @throws IOException if an error occurred
     */
    private void getNextStmt() throws IOException {
        currSrcStmt = null;
        while (true)
            try {
                currSrcStmt = statementizer.getStmt();
                if (currSrcStmt == null) {
                    if (fileList.isEmpty()) {
                        eofReached = true;
                        return;
                    }
                    else
                        termIncludeFile();
                }
                else {
                    loadLimit.checkEndpointReached(currSrcStmt);
                    return;
                }
            } catch (final MMIOException e) {
                handleParseException(e);
                if (messageHandler.maxErrorMessagesReached()) {
                    eofReached = true;
                    return;
                }
                else
                    statementizer.bypassErrorStmt();
            }
    }

    /**
     * Loads next SrcStmt from Statementizer into memory. The main quirk here is
     * that an unrecognized keyword indicates a programming error. None such
     * should be returned by Statementizer -- or else this routine's logic is
     * bogus!
     *
     * @throws IOException if an error occurred
     */
    private void loadStmt() throws IOException {
        try {
            switch (currSrcStmt.keyword) {

                // these case statements are sequenced by guesstimated
                // frequency of occurrence in www.metamath.org\set.mm

                case MMIOConstants.MM_BEGIN_COMMENT_KEYWORD:
                    loadComment(currSrcStmt.comment);
                    break;

                case MMIOConstants.MM_PROVABLE_ASSRT_KEYWORD:
                    loadTheorem();
                    break;

                case MMIOConstants.MM_LOG_HYP_KEYWORD:
                    loadLogHyp();
                    break;

                case MMIOConstants.MM_BEGIN_SCOPE_KEYWORD:
                    loadBeginScope();
                    break;

                case MMIOConstants.MM_END_SCOPE_KEYWORD:
                    loadEndScope();
                    break;

                case MMIOConstants.MM_AXIOMATIC_ASSRT_KEYWORD:
                    loadAxiom();
                    break;

                case MMIOConstants.MM_VAR_HYP_KEYWORD:
                    loadVarHyp();
                    break;

                case MMIOConstants.MM_VAR_KEYWORD:
                    loadVar();
                    break;

                case MMIOConstants.MM_DJ_VAR_KEYWORD:
                    loadDjVar();
                    break;

                case MMIOConstants.MM_CNST_KEYWORD:
                    loadCnst();
                    break;

                case MMIOConstants.MM_BEGIN_FILE_KEYWORD:
                    initIncludeFile();
                    break;

                default:
                    throw new IllegalArgumentException(
                        MMIOConstants.ERRMSG_INV_KEYWORD + currSrcStmt.keyword);
            }
        } catch (final MMIOException e) {
            handleParseException(e);
        } catch (final MMJException e) {
            handleLangException(e);
        }
    }

    /**
     * Tell SystemLoader that a new level of scoping is starting (to group
     * logical hypotheses, etc. with assertions).
     */
    private void loadBeginScope() {
        systemLoader.beginScope();
    }

    /**
     * Sends each constant symbol to SystemLoader, which handles final
     * validations, etc.
     *
     * @throws MMJException if an error occurred
     */
    private void loadCnst() throws MMJException {

        for (final String x : currSrcStmt.symList) {
            Cnst c = systemLoader.addCnst(x);
            c.setPosition(currSrcStmt.position);
        }

        updateLoadProgress();
    }

    /**
     * Sends each var symbol to SystemLoader, which handles final validations,
     * etc.
     *
     * @throws MMJException if an error occurred
     */
    private void loadVar() throws MMJException {

        for (final String x : currSrcStmt.symList) {
            Var v = systemLoader.addVar(x);
            v.setPosition(currSrcStmt.position);
        }
    }

    /**
     * Sends variable hypothesis to SystemLoader.
     *
     * @throws MMJException if an error occurred
     */
    private void loadVarHyp() throws MMJException {

        // note: only one symbol in variable hypothesis, hence get(0);
    	VarHyp vh = systemLoader.addVarHyp(currSrcStmt.label, currSrcStmt.typ,
            currSrcStmt.symList.get(0));
    	vh.setPosition(currSrcStmt.position);
    }

    /**
     * Sends logical hypothesis to SystemLoader.
     *
     * @throws MMJException if an error occurred
     */
    private void loadLogHyp() throws MMJException {

        LogHyp lh = systemLoader.addLogHyp(currSrcStmt.label, currSrcStmt.typ,
            currSrcStmt.symList);
        lh.setPosition(currSrcStmt.position);
    }

    /**
     * Sends axiom to SystemLoader.
     *
     * @throws MMJException if an error occurred
     */
    private void loadAxiom() throws MMJException {

        final Axiom axiom = systemLoader.addAxiom(currSrcStmt.label,
            currSrcStmt.typ, currSrcStmt.symList);
        axiom.setPosition(currSrcStmt.position);

        if (loadComments && currSrcStmt.comment != null)
            axiom.setDescription(currSrcStmt.comment);

    }

    /**
     * Sends theorem to SystemLoader.
     * <p>
     * If loadProofs false, add Theorem with just a "?" proof step.
     * <p>
     * Otherwise, If proofBlockList not null, invoke the variant of addTheorem
     * that handles compressed proofs.
     * <p>
     * Otherwise, add Theorem with uncompressed proof.
     * <p>
     * If comments are to be loaded and a description is available for the
     * Theorem, store the description in the new Theorem object.
     *
     * @throws MMJException if an error occurred
     */
    private void loadTheorem() throws MMJException {

        Theorem theorem;
        if (loadProofs) {
            if (currSrcStmt.proofBlockList == null)
                theorem = systemLoader.addTheorem(currSrcStmt.label,
                    currSrcStmt.column, currSrcStmt.typ, currSrcStmt.symList,
                    currSrcStmt.proofList, currSrcStmt.position, messageHandler);
            else
                theorem = systemLoader.addTheorem(currSrcStmt.label,
                    currSrcStmt.column, currSrcStmt.typ, currSrcStmt.symList,
                    currSrcStmt.proofList, currSrcStmt.proofBlockList, currSrcStmt.position,
                    messageHandler);
        }
        else
            theorem = systemLoader.addTheorem(currSrcStmt.label,
                currSrcStmt.column, currSrcStmt.typ, currSrcStmt.symList,
                defaultProofList, currSrcStmt.position, messageHandler);
        theorem.setPosition(currSrcStmt.position);

        if (loadComments && currSrcStmt.comment != null)
            theorem.setDescription(currSrcStmt.comment);
    }

    /**
     * Sends End Scope command to SystemLoader.
     *
     * @throws MMJException if an error occurred
     */
    private void loadEndScope() throws MMJException {
        systemLoader.endScope();
    }

    /**
     * Sends Dj Vars to SystemLoader.
     *
     * @throws MMJException if an error occurred
     */
    private void loadDjVar() throws MMJException {

        final int iEnd = currSrcStmt.symList.size() - 1;
        String djVarI;

        final int jEnd = iEnd + 1;
        String djVarJ;

        for (int i = 0; i < iEnd; i++) {
            djVarI = currSrcStmt.symList.get(i);
            for (int j = i + 1; j < jEnd; j++) {
                djVarJ = currSrcStmt.symList.get(j);
                systemLoader.addDjVars(djVarI, djVarJ);
            }
        }
    }

    /**
     * As of we are just loading Chapter and Sections for BookManager and $t
     * typesetting definition comments for GMFFManager.
     * <p>
     * Note: per agreement with Norm, the "$t" token identifying typesetting
     * definitions in a comment is the first non-whitespace token after the "$("
     * in the comment.
     *
     * @param comment the comment string
     */
    private void loadComment(final String comment) {

        // for GMFFManager, grab this $t comment!
        final int i = Statementizer.bypassWhitespace(comment, 1);
        if (comment.startsWith(MMIOConstants.TYPESETTING_COMMENT_ID_STRING,
            i))
        {
            systemLoader.cacheTypesettingCommentForGMFF(comment);
            return; // exit, this not the droid you looking for.
        }

        if (systemLoader.isBookManagerEnabled()) {
            String s = Statementizer.getTitleIfApplicable(comment,
                MMIOConstants.CHAPTER_ID_STRING);
            if (s != null)
                systemLoader.addNewChapter(s);
            else {
                s = Statementizer.getTitleIfApplicable(comment,
                    MMIOConstants.SECTION_ID_STRING);
                if (s != null)
                    systemLoader.addNewSection(s);
            }
        }
    }

    /**
     * Switches to the indicated include file, making sure to save the new
     * tokenizer reference for use in error reporting.
     *
     * @param filePath the path to this .mm file (not the include)
     * @throws MMIOException if an error occurred
     * @throws IOException if an error occurred
     */
    private void initIncludeFile()
        throws MMIOException, IOException
    {

    	Source newSource = tokenizer.getSourceId().createSourceId(currSrcStmt.includeFileName);
    	if(dependencyListener != null) dependencyListener.addDependency(tokenizer.getSourceId(), newSource, tokenizer.getCurrentCharNbr());

        try {
        	if (isInFilesAlreadyLoaded(filesAlreadyLoaded, newSource))
                raiseParseException(
                    new MMIOException(MMIOConstants.ERRMSG_INCL_FILE_DUP,
                        currSrcStmt.includeFileName));
            tokenizer = IncludeFile.initIncludeFile(fileList, newSource, statementizer);
            loadProgress.addTask(newSource.getSize());
        } catch (final FileNotFoundException e) {
            raiseParseException(new MMIOException(
                MMIOConstants.ERRMSG_INCL_FILE_NOTFND, newSource.toString()));
        }
    }

    /**
     * Pops the {@code fileList} stack of include files and throws a hard error
     * if it is unable to switch back to the parent source file (should always
     * work...or there is a logic error...or someone deleted the parent file
     * while we were busy processing a nested include file.)
     *
     * @throws IOException if an error occurred
     * @throws MMIOException if an error occurred
     */
    private void termIncludeFile() throws IOException, MMIOException {
        try {
            tokenizer = IncludeFile.termIncludeFile(fileList, statementizer);
        } catch (final FileNotFoundException e) {
            throw new MMIOException(MMIOConstants.ERRMSG_INCLUDE_FILE_LIST_ERR);

        }
    }

    private boolean isInFilesAlreadyLoaded(final List<String> filesAlreadyLoaded,
        final Source sourceId) throws IOException
    {

        final String absPath = sourceId.getUniqueId();

        for (int i = 0; i < filesAlreadyLoaded.size(); i++)
            if (filesAlreadyLoaded.get(i).equals(absPath))
                return true;
        filesAlreadyLoaded.add(absPath);

        return false;
    }

    private void finalizePrematureEOF() throws IOException {
        try {
            while (!fileList.isEmpty())
                termIncludeFile();
        } catch (final MMIOException e) {
            handleLangEOFException(e);
        }

        try {
            systemLoader.finalizeEOF(messageHandler, true); // premature eof
        } catch (final MMJException e) {
            handleLangEOFException(e);
        }
    }

    private void finalizeEOF() {
        try {
            systemLoader.finalizeEOF(messageHandler, false); // !premature eof
        } catch (final MMJException e) {
            handleLangEOFException(e);
        }
    }

    private void handleLangException(final MMJException e) {
    	messageHandler.accumException(tokenizer.addContext(e));
    }

    private void handleLangEOFException(final MMJException e) {
    	messageHandler.accumException(e.addContext(MMIOConstants.EOF_ERRMSG)
            .setPosition(tokenizer.getCurrentPosition())
    		.addContext(new FileContext(tokenizer.getSourceId())));
    }

    private void handleParseException(final MMIOException e) {
    	messageHandler.accumException(e);
    }

    private void raiseParseException(final MMIOException e)
        throws MMIOException
    {
        throw tokenizer.addContext(e);
    }

	protected void updateLoadProgress() {
    	loadProgress.worked(tokenizer.getProgress());
    }
    
    private class LoadLimit {
        public int loadEndpointStmtNbr = 0;
        public String loadEndpointStmtLabel = null;
        public boolean endpointReached = false;

        public boolean checkEndpointReached(final SrcStmt srcStmt) {
            if (loadEndpointStmtNbr > 0 && srcStmt.seq >= loadEndpointStmtNbr) {
                endpointReached = true;
                messageHandler.accumException(new MMJException(
                    MMIOConstants.ERRMSG_LOAD_LIMIT_STMT_NBR_REACHED,
                    loadEndpointStmtNbr));
            }
            if (loadEndpointStmtLabel != null && srcStmt.label != null
                && srcStmt.label.equals(loadEndpointStmtLabel))
            {
                endpointReached = true;
                messageHandler.accumException(new MMJException(
                    MMIOConstants.ERRMSG_LOAD_LIMIT_STMT_LABEL_REACHED,
                    loadEndpointStmtLabel));
            }
            return endpointReached;
        }
    }

    public interface DependencyListener {
    	/**
    	 * Registers an include file dependency
    	 * @param includerSourceId the sourceId of the file containing the include statement $[ ... $]
    	 * @param includedSourceId the sourceId of the file included
    	 * @param offset the offset in characters within the source where the include statement appears
    	 */
    	public abstract void addDependency(Source includerSourceId, Source includedSourceId, long offset);
    }
}

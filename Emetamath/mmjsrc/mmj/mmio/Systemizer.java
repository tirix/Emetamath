//********************************************************************/
//* Copyright (C) 2005, 2006, 2008                                   */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Systemizer.java  0.07 08/01/2008
 *
 *  Sep-25-2005
 *      -> do finalizeEOF even if error messages found so that
 *         final End Scope can be checked (LogicalSystem
 *         already performs the error message count checking.)
 *  Dec-10-2005
 *      -> Add LoadLimit inner class and associated methods
 *         for setting load limits:
 *
 *             setLimitLoadEndpointStmtNbr(int stmtNbr)
 *             setLimitLoadEndpointStmtLabel(int stmtLabel)
 *
 *         Call these after construction of Systemizer but
 *         before loading file(s).
 *
 *         (NOTE: There is no way to reset the LoadLimit after
 *         use -- a reset could be added but there appears
 *         to be no use for it.)
 *  Dec-22-2005
 *      -> Added character number (offset + 1) to
 *         MMIOException
 *
 *  Apr-1-2006: Version 0.05:
 *      -> Added compressed proof code.
 *
 *  Nov-1-2006: Version 0.06:
 *      -> Added logic to load input comments and
 *         to store Theorem description.
 *      -> Added logic to *not* load proofs
 *
 * Aug-01-2008: Version 0.07:
 *      -> Modified loadAxiom() to load the MObj.description
 *         using the curr SrcStmt's comment, if LoadComments
 *         is "on" (previously only theorem comments were
 *         hoovered up.)
 */

package mmj.mmio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ListIterator;

import mmj.lang.Axiom;
import mmj.lang.Cnst;
import mmj.lang.LangException;
import mmj.lang.LogHyp;
import mmj.lang.MessageHandler;
import mmj.lang.SystemLoader;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.lang.VarHyp;
import mmj.util.Progress;

/**
 * Feed <code>SystemLoader</code> interface with <code>SrcStmt</code>
 * objects from <code>Statementizer</code>.
 * <p>
 * Notes:
 * <ul>
 * <li> Intercepts include statements and converts them into
 *      <code>SrcStmt</code> objects -- transparently with
 *      respect to <code>SystemLoader</code>.
 * <li> Keeps track of end of file and notifies
 *      <code>SystemLoader</code> of that condition.
 * <li> Has no concept of comment statements that are
 *      embedded inside other statements -- if comments
 *      are ever to be used, this needs a redesign!
 * </ul>
 *
 *
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public class Systemizer {


    private   Tokenizer         tokenizer;
    private   Statementizer     statementizer;

    private   MessageHandler    messageHandler;

    private   SystemLoader      systemLoader;

    private   boolean           eofReached          = false;
    private   ArrayList<IncludeFile> fileList       = null;
    private   ArrayList<String>	filesAlreadyLoaded  = null;

    private   SrcStmt           currSrcStmt         = null;

    private   LoadLimit         loadLimit           = null;

    private   boolean           loadComments        = false;

    private   boolean           loadProofs          = true;

    private   Progress		loadProgress		= null;

    private   ArrayList         defaultProofList    = null;
	
    private   DependencyListener	dependencyListener = null;
    /**
     * Construct <code>Systemizer</code> from a
     * <code>Messages</code> object and a
     * <code>SystemLoader</code> object.
     *
     * @param messageHandler -- repository of error and info messageHandler that
     *        provides a limit on the number of errors output
     *        before processing is halted.
     *
     * @param systemLoader -- a SystemLoader initialized with any
     *        customizing parameters and ready to be loaded with
     *        data.
     */
    public Systemizer(MessageHandler messageHandler,
                      SystemLoader  systemLoader)      {

        this.messageHandler = messageHandler;
        this.systemLoader   = systemLoader;

        filesAlreadyLoaded  = new ArrayList<String>();
        tokenizer           = null;
        statementizer       = null;

        loadLimit           = new LoadLimit();

        loadComments        =
            MMIOConstants.LOAD_COMMENTS_DEFAULT;

        loadProofs          =
            MMIOConstants.LOAD_PROOFS_DEFAULT;

        defaultProofList    = new ArrayList(1);
        defaultProofList.add(MMIOConstants.MISSING_PROOF_STEP);

    }

    /**
     *  Get SystemLoader, as-is.
     *
     * @return -- SystemLoader structure in use.
     */
    public SystemLoader getSystemLoader() {
        return systemLoader;
    }

    /**
     *  Set SystemLoader
     *
     *  @param systemLoader <code>SystemLoader</code>
     *         can be input or changed after construction.
     */
    public void setSystemLoader(SystemLoader systemLoader) {
        this.systemLoader = systemLoader;
    }

    /**
     *  Get <code>Messages</code> object
     *
     * @return <code>Messages</code> object
     *
     */
    public MessageHandler getMessages() {
        return messageHandler;
    }

    /**
     *  Set Messages object
     *
     *  @param messageHandler <code>MessageHandler</code> object can
     *         be set or changed after construction.
     *
     */
    public void setMessages(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     *  Get <code>DependencyListener</code> object
     *
     * @return <code>DependencyListener</code> object, or null if not set
     *
     */
	public void setDependencyListener(DependencyListener dependencyListener) {
		this.dependencyListener = dependencyListener;
	}

    /**
     *  Set LoadLimit Stmt Nbr parm
     *
     *  @param stmtNbr maximum number of Metamath statements
     *         to be loaded.
     *
     */
    public void setLimitLoadEndpointStmtNbr(int stmtNbr) {
        loadLimit.setLoadEndpointStmtNbr(stmtNbr);
    }

    /**
     *  Set LoadLimit Stmt Label parm
     *
     *  @param stmtLabel last Metamath statement label to load.
     *
     */
    public void setLimitLoadEndpointStmtLabel(String stmtLabel) {
        loadLimit.setLoadEndpointStmtLabel(stmtLabel);
    }

    /**
     *  Set loadComments boolean parm.
     *  <p>
     *  If loadComments is true then Metamath comments
     *  (at least, for Theorems) will be loaded.
     *  <p>
     *  @param loadComments true/false load Metamath Comments.
     *
     */
    public void setLoadComments(boolean loadComments) {
        this.loadComments         = loadComments;
    }

    /**
     *  Set loadProofs boolean parm.
     *  <p>
     *  If loadProofs is true then Metamath proofs are loaded
     *  into LogicalSystem, otherwise just a "?" proof is
     *  stored with each Theorem.
     *  <p>
     *  @param loadProofs true/false load Metamath Proofs in
     *         Theorem objects.
     *
     */
    public void setLoadProofs(boolean loadProofs) {
        this.loadProofs         = loadProofs;
    }


	public void setLoadProgress(Progress loadProgress) {
		this.loadProgress		= loadProgress;
	}

	/**
	 * Loads MetaMath source file via <code>SystemLoader</code>.
	 * <p>
	 * Note: multiple files can be loaded in serial fashion.
	 *
	 * @param readerIn -- may be StringReader or BufferedReader
	 *          but PushbackReader and LineNumberReader are
	 *          not helpful choices.) Will be closed at EOF.
	 *
	 * @param sourceId  -- caption such as filename or test ID.
	 *        May be empty string if N/A. Used solely for
	 *        diagnostic/testing messageHandler.
	 *
	 * @return <code>Messages</code> object, which can be
	 *        tested to see if any error messageHandler were
	 *        generated
	 *
	 * @throws    IOException if I/O error
	 *
	 */
	public MessageHandler load(Reader readerIn,
	                     Object sourceId)
	                        throws IOException {
	
	    tokenizer                 =
	        new Tokenizer(readerIn, sourceId);
	    statementizer             =
	        new Statementizer(tokenizer);
	
	    boolean errorFound        = false;
	
	    //init stack of include files
	    fileList                  = new ArrayList<IncludeFile>();
	
	    eofReached                = false;
	
	    getNextStmt();
	    if (eofReached &&
	        !errorFound) {
	        handleParseErrorMessage(
	            MMIOConstants.ERRMSG_INPUT_FILE_EMPTY);
	    }
	    else {
	        while (!eofReached    &&
	               !messageHandler.maxErrorMessagesReached()) {
	        	errorFound |= !loadStmt();
	            if (loadLimit.getEndpointReached()) {
	                finalizePrematureEOF();
	                tokenizer.close();
	                return messageHandler;
	            }
	            errorFound |= !getNextStmt();
	        }
	        if (eofReached == true) {
	            finalizeEOF();
	        }
	    }
	    tokenizer.close();
	    return messageHandler;
	}

    /**
     * Clone of Load function using fileNameIn instead of readerIn
     *
     * @param fileNameIn -- input .mm file name String.
     *
     * @param sourceId  -- test such as filename or test ID.
     *        May be empty string if N/A. Used solely for
     *        diagnostic/testing messageHandler.
     *
     * @throws IOException if I/O error
     * @throws MMIOException if file requested has already
     *         been loaded.
     * @throws MMIOError if file requested does not exist.
     *
     */
    public MessageHandler load(String fileNameIn,
                         Object sourceId)
                            throws MMIOException,
                                   IOException {
        Reader readerIn;
        try {
        	if(isInFilesAlreadyLoaded(filesAlreadyLoaded, fileNameIn)) {
                throw new MMIOException(
                        MMIOConstants.ERRMSG_LOAD_REQ_FILE_DUP +
                            fileNameIn);
                }
        	readerIn = IncludeFile.openFile(fileNameIn, loadProgress);
        }
        catch (FileNotFoundException e) {
            throw new MMIOError(
                MMIOConstants.ERRMSG_LOAD_REQ_FILE_NOTFND +
                    fileNameIn);
        }

        return load(readerIn, sourceId);
    }

    /**
     * Clone of Load function using fileNameIn instead of readerIn.
     *
     * @param fileNameIn -- input .mm file name String.
     *
     * @throws IOException if I/O error
     * @throws MMIOException if file requested has already
     *         been loaded.
     * @throws MMIOError if file requested does not exist.
     *
     */
    public MessageHandler load(String fileNameIn)
                            throws MMIOException,
                                   IOException {
        return load(fileNameIn, fileNameIn);
    }

    // =========================================================

    /**
     * Get next SrcStmt from Statementizer.
     *
     * This is a weird little routine because it must check
     * to see if end of file (eof -- indicated by a null
     * SrcStmt from Statementizer) is simply the end of an
     * include file, and if so, pop the include file stack
     * and go get the NEXT statement from the parent file.
     *
     * Also, it must bypass statements with errors and
     * keep processing until the maximum number of parse
     * errors is reached, or end of file.
     * 
     * @return true if successful, i.e. no error occurred
     *
     */
    private   boolean getNextStmt()
                            throws IOException {
        boolean errorFound = false;
    	currSrcStmt = null;
        while (true) {
            try {
                currSrcStmt = statementizer.getStmt();
                if (currSrcStmt == null) {
                    if (fileList.isEmpty()) {
                        eofReached = true;
                        return !errorFound;
                    }
                    else {
                        termIncludeFile();
                    }
                }
                else {
                    loadLimit.checkEndpointReached(currSrcStmt);
                    return !errorFound;
                }
            }
            catch (MMIOException e) {
                handleParseException(e);
                errorFound = true;
                if (messageHandler.maxErrorMessagesReached()) {
                    eofReached = true;
                    return !errorFound;
                }
                else {
                    statementizer.bypassErrorStmt();
                }
            }
        }
    }

    /**
     * Loads next SrcStmt from Statementizer into memory.
     *
     * The main quirk here is that an unrecognized keyword
     * indicates a programming error. None such should be
     * returned by Statementizer -- or else this routine's
     * logic is bogus!
     * 
     * @return false if an error occurred when parsing
     *
     */
    private   boolean loadStmt()
                            throws IOException {
        try {
            switch (currSrcStmt.keyword.charAt(1)) {

                //these case statements are sequenced by guesstimated
                //frequency of occurrence in www.metamath.org\set.mm

                case MMIOConstants.MM_BEGIN_COMMENT_KEYWORD_CHAR:
                    loadComment(currSrcStmt.comment);
                    break;

                case MMIOConstants.MM_PROVABLE_ASSRT_KEYWORD_CHAR:
                    loadTheorem();
                    break;

                case MMIOConstants.MM_LOG_HYP_KEYWORD_CHAR:
                    loadLogHyp();
                    break;

                case MMIOConstants.MM_BEGIN_SCOPE_KEYWORD_CHAR:
                    loadBeginScope();
                    break;

                case MMIOConstants.MM_END_SCOPE_KEYWORD_CHAR:
                    loadEndScope();
                    break;

                case MMIOConstants.MM_AXIOMATIC_ASSRT_KEYWORD_CHAR:
                    loadAxiom();
                    break;

                case MMIOConstants.MM_VAR_HYP_KEYWORD_CHAR:
                    loadVarHyp();
                    break;

                case MMIOConstants.MM_VAR_KEYWORD_CHAR:
                    loadVar();
                    break;

                case MMIOConstants.MM_DJ_VAR_KEYWORD_CHAR:
                    loadDjVar();
                    break;

                case MMIOConstants.MM_CNST_KEYWORD_CHAR:
                    loadCnst();
                    break;

                case MMIOConstants.MM_BEGIN_FILE_KEYWORD_CHAR:
                    initIncludeFile();
                    break;

                default:
                    throw new IllegalArgumentException(
                        MMIOConstants.ERRMSG_INV_KEYWORD +
                            currSrcStmt.keyword);
            }
        }
        catch (MMIOException e) {
            handleParseException(e);
            return false;
        }
        catch (LangException e) {
            handleLangErrorMessage(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     *  Tell SystemLoader that a new level of scoping
     *  is starting (to group logical hypotheses, etc.
     *  with assertions).
     */
    private void loadBeginScope() {
        systemLoader.beginScope();
    }

    /**
     *  Sends each constant symbol to SystemLoader,
     *  which handles final validations, etc.
     */
    private void loadCnst()
                        throws LangException {

        ListIterator x = currSrcStmt.symList.listIterator();
        while (x.hasNext()) {
            Cnst c = systemLoader.addCnst((String)x.next());
            c.setPosition(currSrcStmt.position);

            if (loadComments &&
                    !x.hasNext() &&
            		currSrcStmt.comment != null) {
                c.setDescription(currSrcStmt.comment);
            }
        }

        updateLoadProgress();
    }

    /**
     *  Sends each var symbol to SystemLoader,
     *  which handles final validations, etc.
     */
    private void loadVar()
                        throws LangException {

        ListIterator x = currSrcStmt.symList.listIterator();
        while (x.hasNext()) {
            Var v = systemLoader.addVar((String)x.next());
            v.setPosition(currSrcStmt.position);
        }
    }

    /**
     *  Sends variable hypothesis to SystemLoader.
     */
    private void loadVarHyp()
                        throws LangException {

        //note: only one symbol in variable hypothesis, hence get(0);
        VarHyp vh = systemLoader.addVarHyp(currSrcStmt.label,
                         currSrcStmt.typ,
                         (String)(currSrcStmt.symList.get(0)));
        vh.setPosition(currSrcStmt.position);
    }

    /**
     *  Sends logical hypothesis to SystemLoader.
     */
    private void loadLogHyp()
                        throws LangException {

        LogHyp lh = systemLoader.addLogHyp(currSrcStmt.label,
                         currSrcStmt.typ,
                         currSrcStmt.symList);
        lh.setPosition(currSrcStmt.position);
    }

    /**
     *  Sends axiom to SystemLoader.
     */
    private void loadAxiom()
                        throws LangException {

        Axiom axiom               =
            systemLoader.addAxiom(currSrcStmt.label,
                            currSrcStmt.typ,
                            currSrcStmt.symList);
        axiom.setPosition(currSrcStmt.position);
        
        if (loadComments &&
            currSrcStmt.comment != null) {
            axiom.setDescription(currSrcStmt.comment);
        }

    }

    /**
     *  Sends theorem to SystemLoader.
     *  <p>
     *  If loadProofs false, add Theorem with just a "?"
     *  proof step.
     *  <p>
     *  Otherwise,
     *  If proofBlockList not null, invoke the variant
     *  of addTheorem that handles compressed proofs.
     *  <p>
     *  Otherwise, add Theorem with uncompressed proof.
     *  <p>
     *  If comments are to be loaded and a description
     *  is available for the Theorem, store the description
     *  in the new Theorem object.
     */
    private void loadTheorem()
                        throws LangException {

        Theorem theorem;
        if (loadProofs) {
            if (currSrcStmt.proofBlockList == null) {
                theorem               =
                    systemLoader.addTheorem(currSrcStmt.label,
                                      currSrcStmt.typ,
                                      currSrcStmt.symList,
                                      currSrcStmt.proofList);
            }
            else {
                theorem               =
                    systemLoader.addTheorem(currSrcStmt.label,
                                      currSrcStmt.typ,
                                      currSrcStmt.symList,
                                      currSrcStmt.proofList,
                                      currSrcStmt.proofBlockList);
            }
        }
        else {
            theorem               =
                systemLoader.addTheorem(currSrcStmt.label,
                                  currSrcStmt.typ,
                                  currSrcStmt.symList,
                                  defaultProofList);
        }
        theorem.setPosition(currSrcStmt.position);

        if (loadComments &&
            currSrcStmt.comment != null) {
            theorem.setDescription(currSrcStmt.comment);
        }
    }

    /**
     *  Sends End Scope command to SystemLoader.
     */
    private void loadEndScope()
                        throws LangException {
        systemLoader.endScope();
    }

    /**
     *  Sends Dj Vars to SystemLoader.
     */
    private void loadDjVar()
                        throws LangException {

        int     iEnd = currSrcStmt.symList.size() - 1;
        String  djVarI;

        int     jEnd = iEnd + 1;
        String  djVarJ;

        for (int i = 0; i < iEnd; i++) {
            djVarI = (String)currSrcStmt.symList.get(i);
            for (int j = i + 1; j < jEnd; j++) {
                djVarJ = (String)currSrcStmt.symList.get(j);
                    systemLoader.addDjVars(djVarI,
                                           djVarJ);
            }
        }
    }

    /**
     *  As of we are just loading Chapter and Sections for
     *  BookManager.
     */
    private void loadComment(String comment) {
        if (systemLoader.isBookManagerEnabled()) {
            String s              =
                Statementizer.getTitleIfApplicable(
                    comment,
                    MMIOConstants.CHAPTER_ID_STRING);
            if (s != null) {
                systemLoader.addNewChapter(s, currSrcStmt.position);
            }
            else {
                s                 =
                Statementizer.getTitleIfApplicable(
                    comment,
                    MMIOConstants.SECTION_ID_STRING);
                if (s != null) {
                    systemLoader.addNewSection(s, currSrcStmt.position);
                }
            }
        }
    }

    /**
     * Switches to the indicated include file, making
     * sure to save the new tokenizer reference for
     * use in error reporting.
     */
    private void initIncludeFile()
                        throws MMIOException,
                               IOException {

    	if(dependencyListener != null) dependencyListener.addDependency(tokenizer.getSourceId(), IncludeFile.getSource(currSrcStmt.includeFileName));

    	try {
            if (isInFilesAlreadyLoaded(
                    filesAlreadyLoaded,
                    currSrcStmt.includeFileName)) {
                raiseParseException(
                    MMIOConstants.ERRMSG_INCL_FILE_DUP +
                        currSrcStmt.includeFileName);
            }
            tokenizer             =
                IncludeFile.initIncludeFile(
                    fileList,
                    loadProgress,
                    currSrcStmt.includeFileName,
                    statementizer);
        }
        catch (FileNotFoundException e) {
            raiseParseException(
                MMIOConstants.ERRMSG_INCL_FILE_NOTFND);
        }
    }

    /**
     * Pops the <code>fileList</code> stack of include files
     * and throws a hard error if it is unable to switch back
     * to the parent source file (should always work...or
     * there is a logic error...or someone deleted the parent
     * file while we were busy processing a nested include file.)
     */
    private   void termIncludeFile()
                            throws IOException {
        try {
            tokenizer =
                IncludeFile.termIncludeFile(fileList,
                                            statementizer);
        }
        catch (FileNotFoundException e) {
            throw new IllegalStateException(
                MMIOConstants.ERRMSG_INCLUDE_FILE_LIST_ERR);

        }
    }

    private boolean isInFilesAlreadyLoaded(
                                       ArrayList<String> filesAlreadyLoaded,
                                       String    fileNameIn)
                              throws FileNotFoundException,
                                     IOException {

    	boolean alreadyLoaded = filesAlreadyLoaded.contains(fileNameIn);
    	if(!alreadyLoaded) filesAlreadyLoaded.add(fileNameIn);
    	return alreadyLoaded;
    }

    public void clearFilesAlreadyLoaded() {
    	filesAlreadyLoaded = new ArrayList<String>();
    	fileList = new ArrayList<IncludeFile>();
    }
    
    private void finalizePrematureEOF() {
	    try {
	        while (true) {
	            if (fileList.isEmpty()) {
	                break;
	            }
	            else {
	                termIncludeFile();
	            }
	        }
	    }
	    catch (IOException e) {
	        handleParseErrorMessage(e.getMessage());
	    }
	
	    try {
	        systemLoader.finalizeEOF(messageHandler,
	                                 true);  //premature eof
	    }
	    catch (LangException e) {
	        handleLangEOFErrorMessage(e.getMessage());
	    }
	}

    private void finalizeEOF() {
        try {
            systemLoader.finalizeEOF(messageHandler,
                                     false);  // !premature eof
        }
        catch (LangException e) {
            handleLangEOFErrorMessage(e.getMessage());
        }
    }

    private   void handleLangErrorMessage(String eMessage) {
        messageHandler.accumMMIOException(new MMIOException(
        		tokenizer.getSourceId(),
        		tokenizer.getCurrentLineNbr(),
        		tokenizer.getCurrentColumnNbr(),
        		tokenizer.getLastCharNbr(),
        		tokenizer.getCurrentCharNbr(),
        		eMessage));
    }

    private   void handleLangEOFErrorMessage(String eMessage) {
    	handleParseErrorMessage(eMessage);
//            eMessage
//            + MMIOConstants.ERRMSG_TXT_SOURCE_ID
//            + MMIOConstants.EOF_ERRMSG
//            + tokenizer.getSourceId());
    }


    private   void handleParseException(MMIOException e) {
        messageHandler.accumMMIOException(e);
    }

    private   void handleParseErrorMessage(String eMessage) {
        messageHandler.accumMMIOException(new MMIOException(
                tokenizer.getSourceId(),
                tokenizer.getCurrentLineNbr(),
                tokenizer.getCurrentColumnNbr(),
        		tokenizer.getLastCharNbr(),
                tokenizer.getCurrentCharNbr(),
                eMessage));
    }


    private   void raiseParseException(String errmsg)
                                    throws MMIOException {
        throw new MMIOException(
            tokenizer.getSourceId(),
            tokenizer.getCurrentLineNbr(),
            tokenizer.getCurrentColumnNbr(),
    		tokenizer.getLastCharNbr(),
            tokenizer.getCurrentCharNbr(),
            errmsg);
    }

	protected void updateLoadProgress() {
    	loadProgress.worked(tokenizer.getProgress());
    }
    
    private class LoadLimit {
        int    loadEndpointStmtNbr;
        String loadEndpointStmtLabel;
        boolean endpointReached;
        public LoadLimit() {
            loadEndpointStmtNbr   = 0;
            loadEndpointStmtLabel = null;
            endpointReached       = false;
        }
        public void setLoadEndpointStmtNbr(
                        int loadEndpointStmtNbr) {
            this.loadEndpointStmtNbr
                                  = loadEndpointStmtNbr;
        }
        public void setLoadEndpointStmtLabel(
                        String loadEndpointStmtLabel) {
            this.loadEndpointStmtLabel
                                  = loadEndpointStmtLabel;
        }
        public boolean checkEndpointReached(SrcStmt srcStmt) {
            if (loadEndpointStmtNbr > 0 &&
                srcStmt.seq >= loadEndpointStmtNbr) {
                endpointReached   = true;
                messageHandler.accumInfoMessage(
                    MMIOConstants.
                        ERRMSG_LOAD_LIMIT_STMT_NBR_REACHED
                        + loadEndpointStmtNbr);
            }
            if (loadEndpointStmtLabel != null &&
                srcStmt.label         != null &&
                srcStmt.label.equals(loadEndpointStmtLabel)) {
                endpointReached   = true;
                messageHandler.accumInfoMessage(
                    MMIOConstants.
                        ERRMSG_LOAD_LIMIT_STMT_LABEL_REACHED
                        + loadEndpointStmtLabel);
            }
            return endpointReached;
        }
        public boolean getEndpointReached() {
            return endpointReached;
        }
    }

    // TODO need to add the offset in the toSourceId
    public interface DependencyListener {
    	/**
    	 * Registers an include file dependency
    	 * @param includerSourceId the sourceId of the file containing the include statement $[ ... $]
    	 * @param includedSourceId the sourceId of the file included
    	 */
    	public abstract void addDependency(Object includerSourceId, Object includedSourceId);
    }
}

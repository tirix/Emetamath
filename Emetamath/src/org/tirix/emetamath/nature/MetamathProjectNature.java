package org.tirix.emetamath.nature;


import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import mmj.lang.Axiom;
import mmj.lang.BookManager;
import mmj.lang.Cnst;
import mmj.lang.LangConstants;
import mmj.lang.LogicalSystem;
import mmj.lang.MObj;
import mmj.lang.MessageHandler;
import mmj.lang.Messages;
import mmj.lang.SeqAssigner;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Var;
import mmj.mmio.IncludeFile;
import mmj.mmio.MMIOConstants;
import mmj.mmio.MMIOException;
import mmj.mmio.SourcePosition;
import mmj.mmio.Systemizer;
import mmj.mmio.IncludeFile.ReaderProvider;
import mmj.util.RunParmArrayEntry;
import mmj.util.UtilConstants;
import mmj.verify.Grammar;
import mmj.verify.GrammarConstants;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.popup.actions.SetMainFileAction;

/**
 * Metamath Project Nature
 * 
 * This project nature stores the Metamath Logical System 
 * (mm file parsed into logical Mobj objects)
 * It acts as a repository for this information. 
 * It also stores all Metamath project parameters.
 * Core functionality is the same as MMJ2's LogicalSystemBoss,
 * except that it does not make reference to OutputBoss and to Messages.
 * (which are handled differently in order to generate annotations)
 *  
 * @author Thierry
 *
 */
public class MetamathProjectNature implements IProjectNature {

	public static final String MARKER_TYPE = "org.tirix.emetamath.MMProblem";

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "org.tirix.emetamath.metamathNature";
	public static final QualifiedName MAINFILE_PROPERTY = new QualifiedName("org.tirix.emetamath", "mainFile");
	public static final QualifiedName ISMAINFILE_PROPERTY = new QualifiedName("org.tirix.emetamath", "isMainFile");
	
	public static final String DEFINITION_PREFIX = "df-";
	
	private IProject project;
	
	/**
	 * set.mm specific : 
	 * A table enabling to find the type of each variable
	 */
	private Hashtable<Sym,Stmt> notations;
	private Cnst provableType, wffType, classType, setType;
	
	/**
	 * The main file, with which parsing/verification shall start
	 */
	protected IResource mainFile;
	
	/**
	 * The messageHandler to be provided to MMJ2
	 * redirects messages to the Eclipse Error Log.
	 */
	protected MetamathMessageHandler messageHandler = new MetamathMessageHandler();

	/*
	 * Parameters
	 */
    protected String         provableLogicStmtTypeParm;
    protected String         logicStmtTypeParm;

    protected boolean        bookManagerEnabledParm;
    protected BookManager    bookManager;

    protected int            seqAssignerIntervalSizeParm;
    protected int            seqAssignerIntervalTblInitialSizeParm;
    protected SeqAssigner    seqAssigner;

    protected int            symTblInitialSizeParm;
    protected int            stmtTblInitialSizeParm;
    protected int            loadEndpointStmtNbrParm;
    protected String         loadEndpointStmtLabelParm;

    protected boolean        loadComments;
    protected boolean        loadProofs;

    protected LogicalSystem  logicalSystem;

    protected Systemizer     systemizer;

	protected Grammar		 grammar;

	protected boolean        logicalSystemLoaded;
	private ArrayList<SystemLoadListener> listeners;
	
	protected ReaderProvider readerProvider;
	
    public MetamathProjectNature() {
    	listeners = new ArrayList<SystemLoadListener>();
   	}
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MetamathBuilder.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(MetamathBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MetamathBuilder.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
    	readerProvider = new EclipseReaderProvider(project);
    	messageHandler.setDefaultResource(project);
    	initStateVariables();
		try {
			setMainFile(getMainFile());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

    private void initStateVariables() {
        logicalSystemLoaded       = false;

        symTblInitialSizeParm     = 0;
        stmtTblInitialSizeParm    = 0;
        loadEndpointStmtNbrParm   = 0;
        loadEndpointStmtLabelParm = null;
        logicalSystem             = null;
        systemizer                = null;

        loadComments              =
            MMIOConstants.LOAD_COMMENTS_DEFAULT;
        loadProofs                =
            MMIOConstants.LOAD_PROOFS_DEFAULT;

        provableLogicStmtTypeParm =
            GrammarConstants.
                DEFAULT_PROVABLE_LOGIC_STMT_TYP_CODES[0];

        logicStmtTypeParm         =
            GrammarConstants.DEFAULT_LOGIC_STMT_TYP_CODES[0];

        bookManagerEnabledParm    =
            LangConstants.BOOK_MANAGER_ENABLED_DEFAULT;
        bookManager               = null;

        seqAssignerIntervalSizeParm
                                  =
            LangConstants.
                SEQ_ASSIGNER_INTERVAL_SIZE_DEFAULT;

        seqAssignerIntervalTblInitialSizeParm
                                  =
            LangConstants.
                SEQ_ASSIGNER_INTERVAL_TBL_INITIAL_SIZE_DEFAULT;

        seqAssigner               = null;

    }

    public void clearLogicalSystem(Object sourceId, MessageHandler messageHandler) {
		logicalSystemLoaded   = false;
	    
	    if (logicalSystem == null) {
	        if (bookManager == null) {
	            bookManager       =
	                new BookManager(bookManagerEnabledParm,
	                                provableLogicStmtTypeParm);
	        }
	
	        if (seqAssigner == null) {
	            seqAssigner       =
	                new SeqAssigner(
	                    seqAssignerIntervalSizeParm,
	                    seqAssignerIntervalTblInitialSizeParm);
	        }
	
	        int i = symTblInitialSizeParm;
	        if (i <= 0) {
	            i = LangConstants.SYM_TBL_INITIAL_SIZE_DEFAULT;
	        }
	        int j = symTblInitialSizeParm;
	        if (j <= 0) {
	            j = LangConstants.STMT_TBL_INITIAL_SIZE_DEFAULT;
	        }
	
	        logicalSystem     =
	            new LogicalSystem(
	                    provableLogicStmtTypeParm,
	                    logicStmtTypeParm,
	                    bookManager,
	                    seqAssigner,
	                    i,
	                    j,
	                    null,  //use null to override default
	                    null); //use null to override default
	    }
	    else {
	        // precautionary, added for 08/01/2008 release
	    	logicalSystem.setSyntaxVerifier(null);
	        logicalSystem.setProofVerifier(null);
	        logicalSystem.clearTheoremLoaderCommitListenerList();

	    	logicalSystem.clear();
	        bookManager.clear();
	        seqAssigner.clear();
	    }
	
	    if (systemizer == null) {
	        systemizer        =
	        new Systemizer(messageHandler,
	                       logicalSystem);
	    }
	    else {
	    	systemizer.clearFilesAlreadyLoaded(); // TODO remove only the specified file
	    	systemizer.setSystemLoader(logicalSystem);
	        systemizer.setMessages(messageHandler);
	    }
    }
    
    /**
     *  Get reference to LogicalSystem.
     *
     *  If LogicalSystem has not been successfully loaded
     *  with a .mm file -- and no load errors -- then
     *  throw an exception. Either the RunParmFile lines
     *  are misordered or the LoadFile command is missing,
     *  or the Metamath file has errors, or?
     *
     *  @return LogicalSystem object reference.
     */
    public LogicalSystem getLogicalSystem() {
        if (!logicalSystemLoaded) {
//            throw new IllegalArgumentException(
//                UtilConstants.ERRMSG_MM_FILE_NOT_LOADED_1
//                + UtilConstants.RUNPARM_LOAD_FILE
//                + UtilConstants.ERRMSG_MM_FILE_NOT_LOADED_2
//                + UtilConstants.RUNPARM_LOAD_FILE
//                + UtilConstants.ERRMSG_MM_FILE_NOT_LOADED_3);
        }
        return logicalSystem;
    }


    /**
     *  An initializeGrammar subroutine.
     */
    protected void initializeGrammar(MessageHandler messageHandler) {
        Grammar grammar           = getGrammar();
        grammar.initializeGrammar(
                    messageHandler,
                    logicalSystem.getSymTbl(),
                    logicalSystem.getStmtTbl());
    }

    /**
     *  Fetch a Grammar object, building it if necessary
     *  from previously input RunParms.
     *  <p>
     *  NOTE: The returned Grammar is "ready to go" but
     *        may not have been "initialized", which means
     *        grammar validation, etc. The reason that
     *        grammar is not initialized here is that
     *        a previous attempt to "initialize" may have
     *        failed due to grammar errors, so to re-do
     *        it here would result in doubled-up error
     *        messages. The Initialize Grammar RunParm
     *        Command should be used prior to PrintSyntaxDetails
     *        if a "load and print syntax" is desired.
     *
     *  @return Grammar object, ready to go.
     */
    public Grammar getGrammar() {
    	if(logicalSystem == null) throw new RuntimeException("Cannot get grammar if logical system is not yet loaded");
    		
        if (grammar != null) {
            return grammar;
        }

        String[] pTyp             = new String[1];
        pTyp[0]                   =
            logicalSystem.getProvableLogicStmtTypeParm();
        String[] lTyp             = new String[1];
        lTyp[0]                   =
            logicalSystem.getLogicStmtTypeParm();

        boolean gComplete         =
            GrammarConstants.DEFAULT_COMPLETE_GRAMMAR_AMBIG_EDITS;

        boolean sComplete         =
          GrammarConstants.DEFAULT_COMPLETE_STATEMENT_AMBIG_EDITS;

        grammar                   = new Grammar(pTyp,
                                                lTyp,
                                                gComplete,
                                                sComplete);
        return grammar;
    }

    public void clearGrammar() {
    	grammar.clear();
    }
    
    public BookManager getBookManager() {
		return bookManager;
	}

	public void addSystemLoadListener(SystemLoadListener l) {
		listeners.add(l);
		if(logicalSystemLoaded) l.systemLoaded();
	}
	
	public void setLogicalSystemLoaded() {
		initializeTypes();
		logicalSystemLoaded = true;
		for(SystemLoadListener l:listeners) 
			l.systemLoaded();
	}
	
	private void initializeTypes() {
		notations = new Hashtable<Sym, Stmt>();
		wffType = (Cnst)logicalSystem.getSymTbl().get("wff");
		provableType = (Cnst)logicalSystem.getSymTbl().get("|-"); // TODO replace by correct default value
		setType = (Cnst)logicalSystem.getSymTbl().get("set");
		classType = (Cnst)logicalSystem.getSymTbl().get("class");
		
		grammar = getGrammar();
		initializeGrammar(messageHandler);
	}
	
	public boolean isWff(Var var) {
		if(!logicalSystemLoaded) return false;
		return getType(var).equals(wffType);
	}
	
	public boolean isSet(Var var) {
		if(!logicalSystemLoaded) return false;
		return getType(var).equals(setType);
	}
	
	public boolean isClass(Var var) {
		if(!logicalSystemLoaded) return false;
		return getType(var).equals(classType);
	}
	
	/**
	 * Return the type of a symbol sym.
	 * @param sym
	 * @return type of the symbol sym.
	 */
	public Cnst getType(Sym sym) {
		Stmt notation = getNotation(sym);
		if(notation != null) return notation.getTyp();
		else return null;
	}
	
	/**
	 * Return the notation statement for a symbol sym.
	 * Search for the first Axiom or Logical Hypothesis ($f) where "sym" is the complete formula. 
	 * @param sym
	 * @return notation statement for the symbol sym.
	 */
	public Stmt getNotation(Sym sym) {
		Stmt notation = notations.get(sym);
		if(notation == null) {
			Collection<Stmt> statements = logicalSystem.getStmtTbl().values();
			for(Stmt stmt:statements) {
				Sym[] expr = stmt.getFormula().getExpr();
				if(expr.length == 1 && expr[0].equals(sym) && !stmt.getTyp().equals(provableType)) {
					notations.put(sym, stmt);
					notation = stmt;
				}
			}
		}
		return notation;
	}

	/**
	 * Return the definition statement for a symbol sym.
	 * Lazy implmentation looking for the first axiom in which the symbol appears *as the first symbol in the axiom*
	 * @param sym
	 * @return notation statement for the symbol sym.
	 */
	public Axiom getDefinition(Sym sym) {
		Collection<Stmt> statements = logicalSystem.getStmtTbl().values();
		for(Stmt stmt:statements) {
			if(stmt instanceof Axiom && stmt.getLabel().startsWith(DEFINITION_PREFIX)) {
				Sym[] expr = stmt.getFormula().getExpr();
				if(expr[0].equals(sym)) {
					return (Axiom)stmt;
				}
			}
		}
		return null;
	}

	public static MetamathProjectNature getNature(Object inputElement) {
		try {
			FileEditorInput input = (FileEditorInput)inputElement;
			return (MetamathProjectNature)input.getFile().getProject().getNature(MetamathProjectNature.NATURE_ID);
		} catch (CoreException e) {
			//e.printStackTrace();
			return null;
		}
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	public IResource getMainFile() throws CoreException {
		String mainFileName = getProject().getPersistentProperty(MAINFILE_PROPERTY);
		if(mainFileName == null) return null;
		return getProject().getFile(mainFileName);
	}

	public void setMainFile(final IResource mainFile) throws CoreException {
		if(this.mainFile != null) this.mainFile.setPersistentProperty(ISMAINFILE_PROPERTY, null);
		this.mainFile = mainFile;
		if(this.mainFile == null) return; 
		mainFile.setPersistentProperty(ISMAINFILE_PROPERTY, Boolean.toString(true));
		getProject().setPersistentProperty(MAINFILE_PROPERTY, mainFile.getName());

		// perform an incremental build, starting from this new file
		Job buildJob = new Job("Rebuild") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.setTaskName("Verifying proofs for "+mainFile.getName());
					getProject().refreshLocal(IResource.DEPTH_ONE, null);
					getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					return new Status(Status.OK, "eMetamath", "Job finished"); 
				} catch (CoreException e) {
					return new Status(Status.ERROR, "eMetamath", "Error during rebuild", e); 
				}
			}};
		buildJob.setSystem(true);
		buildJob.schedule();
	}

	public static interface SystemLoadListener {
		public void systemLoaded();
	}

	public boolean isLogicalSystemLoaded() {
		return logicalSystemLoaded;
	}

	public MObj getMObj(String objectName) {
		if(logicalSystem == null) return null;
		
		Sym sym = (Sym)logicalSystem.getSymTbl().get(objectName);
		if(sym != null) return sym;

		Stmt stmt = (Stmt)logicalSystem.getStmtTbl().get(objectName);
		if(stmt != null) return stmt;

		return null;
	}
	
	public static class EclipseReaderProvider implements ReaderProvider {
		IContainer rootContainer;
		
		public EclipseReaderProvider(IContainer rootContainer) {
			this.rootContainer = rootContainer;
		}

		@Override
		public Reader createReader(String fileName)
				throws FileNotFoundException {
			try {
				IFile file = rootContainer.getFile(new Path(fileName));
				return new InputStreamReader(file.getContents());
			}
			catch(CoreException e) {
				throw new FileNotFoundException("File not found : "+fileName);
			}
		}

		@Override
		public long getSize(String fileName) throws FileNotFoundException {
			IFile file = rootContainer.getFile(new Path(fileName));
			return file.getLocation().toFile().length();
		}

		@Override
		public Object getSource(String fileName) {
			return rootContainer.getFile(new Path(fileName));
		}

		@Override
		public String getFileName(Object sourceId) {
			return ((IFile)sourceId).getProjectRelativePath().toString();
		}
	}
	
	public static class MetamathMessageHandler implements MessageHandler {
		protected IResource defaultResource;
		
		MetamathMessageHandler() {
		}
		
		MetamathMessageHandler(IResource defaultResource) {
			setDefaultResource(defaultResource);
		}

		public void setDefaultResource(IResource defaultResource) {
			this.defaultResource = defaultResource;
		}
		
		private void addMarker(String message, int severity) {
			MetamathProjectNature.addMarker(new SourcePosition(defaultResource, 0, 0, 0, 0), message, severity);
		}

		@Override
		public boolean accumMMIOException(MMIOException e) {
			MetamathProjectNature.addMarker(e.position, e.getMessage(), IMarker.SEVERITY_ERROR);
			return false;
		}

		@Override
		public boolean accumErrorMessage(String errorMessage) {
			addMarker(errorMessage, IMarker.SEVERITY_ERROR);
			return false;
		}

		@Override
		public boolean accumErrorMessage(SourcePosition position, String errorMessage) {
			MetamathProjectNature.addMarker(position, errorMessage, IMarker.SEVERITY_ERROR);
			return false;
		}

		@Override
		public boolean accumInfoMessage(String infoMessage) {
			addMarker(infoMessage, IMarker.SEVERITY_INFO);
			return false;
		}

		@Override
		public boolean accumInfoMessage(SourcePosition position, String infoMessage) {
			MetamathProjectNature.addMarker(position, infoMessage, IMarker.SEVERITY_INFO);
			return false;
		}

		@Override
		public boolean maxErrorMessagesReached() {
			return false;
		}

		@Override
		public String getOutputMessageText() {
			throw new RuntimeException("This implementation does not provide consolidated text output.");
		}
	}

	protected static void addMarker(SourcePosition position, String message, int severity) {
		try {
			IResource res = (IResource)position.sourceId;
			refinePosition(position);
			IMarker marker = res.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (position.lineNbr == -1) {
				position.lineNbr = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, position.lineNbr);
			marker.setAttribute(IMarker.CHAR_START, position.charStartNbr);
			marker.setAttribute(IMarker.CHAR_END, position.charEndNbr);
		} catch (CoreException e) {
		}
	}

	/**
	 * Some positions are given inside a formula, and the exact char numbers have not been stored.
	 * The position therefore needs to be refined to find back the exact char numbers.
	 * @param position
	 */
	protected static void refinePosition(SourcePosition position) {
		if(position.symbolNbr == -1) return;
		try {
			IResource res = (IResource)position.sourceId;
			MetamathProjectNature nature = (MetamathProjectNature) res.getProject().getNature(MetamathProjectNature.NATURE_ID);
			position.refinePosition(nature.readerProvider);
		} catch (CoreException e) {
		}
	}
}

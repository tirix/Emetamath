package org.tirix.emetamath.nature;


import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mmj.lang.Axiom;
import mmj.lang.BookManager;
import mmj.lang.Cnst;
import mmj.lang.LangConstants;
import mmj.lang.LogicalSystem;
import mmj.lang.MObj;
import mmj.lang.MessageHandler;
import mmj.lang.SeqAssigner;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Var;
import mmj.lang.VerifyException;
import mmj.lang.WorkVarManager;
import mmj.mmio.MMIOConstants;
import mmj.mmio.MMIOException;
import mmj.mmio.SourcePosition;
import mmj.mmio.Systemizer;
import mmj.mmio.IncludeFile.ReaderProvider;
import mmj.mmio.Systemizer.DependencyListener;
import mmj.pa.ProofAsst;
import mmj.pa.ProofAsstPreferences;
import mmj.tl.TheoremLoader;
import mmj.tl.TlPreferences;
import mmj.util.UtilConstants;
import mmj.verify.Grammar;
import mmj.verify.GrammarConstants;
import mmj.verify.VerifyProofs;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IFileEditorInput;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.editors.proofassistant.ProofWorksheetDocumentProvider.ProofWorksheetInput;
import org.tirix.emetamath.views.MMLabelProvider;
import org.tirix.mmj.MathMLTypeSetting;
import org.tirix.mmj.TypeSetting;

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
public class MetamathProjectNature implements IProjectNature, DependencyListener {

	public static final String MARKER_TYPE = "org.tirix.emetamath.MMProblem";

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "org.tirix.emetamath.metamathNature";
	public static final QualifiedName MAINFILE_PROPERTY = new QualifiedName("org.tirix.emetamath", "mainFile");
	public static final QualifiedName ISMAINFILE_PROPERTY = new QualifiedName("org.tirix.emetamath", "isMainFile");
	public static final QualifiedName PROVABLE_TYPE_PROPERTY = new QualifiedName("org.tirix.emetamath", "provableType");
	public static final QualifiedName TYPES_PROPERTY = new QualifiedName("org.tirix.emetamath", "types");
	public static final QualifiedName COLORS_PROPERTY = new QualifiedName("org.tirix.emetamath", "typeColors");
	public static final QualifiedName ICONS_PROPERTY = new QualifiedName("org.tirix.emetamath", "typeIcons");
	
	private static final QualifiedName EXPLORER_BASE_URL_PROPERTY = new QualifiedName("org.tirix.emetamath", "explorerBaseUrl");

	public static final String EXPLORER_BASE_URL_DEFAULT_VALUE = "http://us.metamath.org/mpegif/";
	public static final String PROVABLE_TYPE_DEFAULT_VALUE = "|-";
	public static final String TYPES_DEFAULT_VALUE = "wff$set$class";
	public static final String COLORS_DEFAULT_VALUE = "0,0,255$255,0,0$255,0,255";
	public static final String ICONS_DEFAULT_VALUE = "mmWff.gif$mmSet.gif$mmClass.gif";
	public static final String DEFINITION_PREFIX_DEFAULT_VALUE = "df-";

	private IProject project;
	
	/**
	 * A table enabling to find the type of each variable
	 */
	private Cnst provableType;
	private List<Cnst> types;
	private Hashtable<Cnst, RGB> typeColors;
	private Hashtable<Cnst, Image> typeIcons;
	private Hashtable<Cnst, String> typeIconURLs;
	private Hashtable<Sym,Stmt> notations;
	//private Cnst wffType, classType, setType;
	
	/**
	 * The main file, with which parsing/verification shall start
	 */
	protected IResource mainFile;
	
	/**
	 * The messageHandler to be provided to MMJ2
	 * redirects messages to the Eclipse Error Log.
	 */
	protected MetamathMessageHandler messageHandler = new MetamathMessageHandler();

	/**
	 * The dependency map : contains the set of files depending on a given file
	 */
	protected Map<IFile, Set<IFile>> dependencies = new HashMap<IFile, Set<IFile>>();
	
	/**
	 * A label provider for this nature
	 */
	protected MMLabelProvider labelProvider;
	
	/**
	 * The different typesettings
	 */
	protected TypeSetting mathMLTypeSetting = new MathMLTypeSetting();
	
	
	/*
	 * Parameters
	 */
    protected String         provableLogicStmtTypeParm;
    protected String         logicStmtTypeParm;

    protected boolean        bookManagerEnabledParm;
    protected BookManager    bookManager;

    // TODO move this into (project) preferences
    protected int            seqAssignerIntervalSizeParm;
    protected int            seqAssignerIntervalTblInitialSizeParm;
    protected SeqAssigner    seqAssigner;

    protected int            symTblInitialSizeParm;
    protected int            stmtTblInitialSizeParm;
    protected int            loadEndpointStmtNbrParm;
    protected String         loadEndpointStmtLabelParm;

    protected boolean        loadComments;
    protected boolean        loadProofs;

    protected MetamathPreferences preferences;
    
    
    protected LogicalSystem  logicalSystem;

    protected Systemizer     systemizer;

	protected Grammar		 grammar;

	protected boolean        logicalSystemLoaded;
	private ArrayList<SystemLoadListener> listeners;

	protected WorkVarManager workVarManager;
	protected VerifyProofs	 verifyProofs;
	protected boolean		 allProofsVerifiedSuccessfully;
	protected boolean		 allStatementsParsedSuccessfully;
	protected ProofAsst      proofAsst;

    protected ReaderProvider readerProvider;

	private TheoremLoader theoremLoader;

    public MetamathProjectNature() {
    	listeners = new ArrayList<SystemLoadListener>();
    	types = new ArrayList<Cnst>();
		typeColors = new Hashtable<Cnst, RGB>();
		typeIcons = new Hashtable<Cnst, Image>();
		typeIconURLs = new Hashtable<Cnst, String>();
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
    	preferences = new MetamathPreferences();
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
	        systemizer.setDependencyListener(this);
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


    /**
     *  Return initialized VerifyProofs object
     *
     *  @return VerifyProofs object
     */
    public VerifyProofs getVerifyProofs() {
        if (verifyProofs == null) {
            verifyProofs          = new VerifyProofs();
            allProofsVerifiedSuccessfully
                                  = false;
//            allStatementsParsedSuccessfully
//                                  = false;
        }
        return verifyProofs;
    }


    /**
     *  Fetch a WorkVarManager object.
     *  <p>
     *  Requires that a LogicalSystem be loaded with a .mm
     *  file and that an initialized Grammar object be
     *  available.
     *  <p>
     *  @return WorkVarManager object, ready to go, or null.
     */
    public WorkVarManager getWorkVarManager() {

        if (workVarManager != null) {
            return workVarManager;
        }

        if (grammar.getGrammarInitialized()) {
            workVarManager        = new WorkVarManager(grammar);
        }
        else {
            messageHandler.accumErrorMessage(
                UtilConstants.ERRMSG_WV_MGR_REQUIRES_GRAMMAR_INIT);
        }

        return workVarManager;
    }


    /**
     *  Fetch a TheoremLoader object.
     *  <p>
     *
     *  @return TheoremLoader object, ready to go, or null;.
     */
    public TheoremLoader getTheoremLoader() {

        if (theoremLoader != null) {
            return theoremLoader;
        }

        TlPreferences tlPreferences
                                  = preferences.getTlPreferences(logicalSystem);

        theoremLoader             =
            new TheoremLoader(tlPreferences);

        return theoremLoader;
    }

    /**
     *  Fetch a ProofAsst object.
     *  <p>
     *
     *  @return ProofAsst object, ready to go, or null;.
     */
    public ProofAsst getProofAsst() {

        if (proofAsst != null) {
            return proofAsst;
        }

        getLogicalSystem();
        getVerifyProofs();
        getGrammar();

        try {
	        if (grammar.getGrammarInitialized() &&
	        	allStatementsParsedSuccessfully) {
	
	            ProofAsstPreferences proofAsstPreferences
	                                  = preferences.getProofAsstPreferences();
	
	            WorkVarManager workVarManager = getWorkVarManager();
	
	            if (!workVarManager.areWorkVarsDeclared()) {
						workVarManager.
						    declareWorkVars(grammar,
						                    logicalSystem);
	            }
	
	            proofAsstPreferences.
	                setWorkVarManager(
	                    workVarManager);
	
	            TheoremLoader theoremLoader = getTheoremLoader();
	
	            proofAsst             =
	                new ProofAsst(proofAsstPreferences,
	                              logicalSystem,
	                              grammar,
	                              verifyProofs,
	                              theoremLoader);
	
	            if (!proofAsst.getInitializedOK()) {
	                proofAsst.initializeLookupTables(messageHandler);
	            }
	
	            logicalSystem.
	                accumTheoremLoaderCommitListener(
	                    proofAsst);
	
	        }
	        else {
	            proofAsst             = null;
	            messageHandler.accumErrorMessage(
	                UtilConstants.ERRMSG_PA_GUI_REQUIRES_GRAMMAR_INIT);
	        }
	
	        return proofAsst;

        } catch (VerifyException e) {
			messageHandler.accumErrorMessage(e.getMessage());
			return null;
		}
    }


    public BookManager getBookManager() {
		return bookManager;
	}

	public TypeSetting getMathMLTypeSetting() {
		return mathMLTypeSetting;
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
		
		String provableTypeProperty = null;
		String typesProperty = null;
		String colorsProperty = null;
		String iconsProperty = null;
		try {
			provableTypeProperty = getProvableTypeString();
			typesProperty = getProject().getPersistentProperty(TYPES_PROPERTY);
			colorsProperty = getProject().getPersistentProperty(COLORS_PROPERTY);
			iconsProperty = getProject().getPersistentProperty(ICONS_PROPERTY);
		}
		catch(CoreException e) {}
		if(provableTypeProperty == null) provableTypeProperty = PROVABLE_TYPE_DEFAULT_VALUE;
		if(typesProperty == null) typesProperty = TYPES_DEFAULT_VALUE;
		if(colorsProperty == null) colorsProperty = COLORS_DEFAULT_VALUE;
		if(iconsProperty == null) iconsProperty = ICONS_DEFAULT_VALUE;

		// get the 'provable' type
		provableType = (Cnst)logicalSystem.getSymTbl().get(provableTypeProperty);

		// get the other types
		for(String type:typesProperty.split("\\$")) types.add((Cnst)logicalSystem.getSymTbl().get(type));

		// get the coloring attributes for the other types
		String[] colors = colorsProperty.split("\\$");
		for(int i=0;i<colors.length && i<types.size();i++) {
			String[] rgb = colors[i].split(",");
			RGB color = new RGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			typeColors.put(types.get(i), color);
		}
		if(colors.length != types.size()) System.out.println("Project "+getProject()+": "+types.size()+" types ("+types+") but "+colors.length+" colors ("+colors+") defined!\n");
		
		// get the icons for the other types
		String[] icons = iconsProperty.split("\\$");
		for(int i=0;i<icons.length && i<types.size();i++) {
			typeIcons.put(types.get(i), Activator.getImage("icons/"+icons[i]));
			typeIconURLs.put(types.get(i), "icons/"+icons[i]);
		}
		if(icons.length != types.size()) System.out.println("Project "+getProject()+": "+types.size()+" types ("+types+") but "+icons.length+" icons ("+icons+") defined!\n");

//		wffType = (Cnst)logicalSystem.getSymTbl().get("wff");
//		setType = (Cnst)logicalSystem.getSymTbl().get("set");
//		classType = (Cnst)logicalSystem.getSymTbl().get("class");
		
		grammar = getGrammar();
		initializeGrammar(messageHandler);
	}
	
	public Map<Cnst, RGB> getTypeColors() {
		return typeColors;
	}
	
	public Map<Cnst, Image> getTypeIcons() {
		return typeIcons;
	}
	
	public Map<Cnst, String> getTypeIconURLs() {
		return typeIconURLs;
	}
	
	public boolean isType(Sym sym) {
		if(!(sym instanceof Cnst)) return false;
		if(!logicalSystemLoaded) return false;
		if(sym.equals(provableType)) return true;
		for(Cnst type:types) if(type.equals(sym)) return true;
//		if(sym.equals(wffType)) return true;
//		if(sym.equals(setType)) return true;
//		if(sym.equals(classType)) return true;
		return false;
	}
	
//	public boolean isWff(Var var) {
//		if(!logicalSystemLoaded) return false;
//		return getType(var).equals(wffType);
//	}
//	
//	public boolean isSet(Var var) {
//		if(!logicalSystemLoaded) return false;
//		return getType(var).equals(setType);
//	}
//	
//	public boolean isClass(Var var) {
//		if(!logicalSystemLoaded) return false;
//		return getType(var).equals(classType);
//	}

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
			if(stmt instanceof Axiom && stmt.getLabel().startsWith(DEFINITION_PREFIX_DEFAULT_VALUE)) {
				Sym[] expr = stmt.getFormula().getExpr();
				if(expr[0].equals(sym)) {
					return (Axiom)stmt;
				}
			}
		}
		return null;
	}

	public Cnst getProvableType() {
		return provableType;
	}

	public String getProvableTypeString() throws CoreException {
		String provableTypeString =  getProject().getPersistentProperty(PROVABLE_TYPE_PROPERTY);
		if(provableTypeString == null) provableTypeString = PROVABLE_TYPE_DEFAULT_VALUE;
		return provableTypeString;
	}

	public void setProvableType(Cnst provableType) throws CoreException {
		this.provableType = provableType;
		getProject().setPersistentProperty(PROVABLE_TYPE_PROPERTY, provableType.getId());
	}

	// TODO - generalize...
	public static MetamathProjectNature getNature(IResource resource) {
		try {
			return (MetamathProjectNature)resource.getProject().getNature(MetamathProjectNature.NATURE_ID);
		} catch (CoreException e) {
			//e.printStackTrace();
			return null;
		}
	}

	public static MetamathProjectNature getNature(Object inputElement) {
		if(inputElement instanceof IFileEditorInput) return getNature(((IFileEditorInput)inputElement).getFile());
		if(inputElement instanceof ProofWorksheetInput) return ((ProofWorksheetInput)inputElement).getNature();
		if(inputElement instanceof MetamathProjectNature) return (MetamathProjectNature)inputElement;
		Exception e = new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Cannot adapt to Metamath nature from "+inputElement));
		e.printStackTrace();
		return null;
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public MMLabelProvider getLabelProvider() {
		if(labelProvider == null) {
			labelProvider = new MMLabelProvider();
			labelProvider.setNature(this);
		}
		return labelProvider;
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

	public String getWebExplorerURL() {
		String webExplorerURL = null;
		try {
			webExplorerURL = getProject().getPersistentProperty(EXPLORER_BASE_URL_PROPERTY);
		} catch (CoreException e) {
		}
		if(webExplorerURL == null) webExplorerURL = EXPLORER_BASE_URL_DEFAULT_VALUE;
		return webExplorerURL;
	}

	public void setWebExplorerURL(String baseURL) throws CoreException {
		getProject().setPersistentProperty(EXPLORER_BASE_URL_PROPERTY, baseURL);
	}

	@Override
	public void addDependency(Object includerSourceId, Object includedSourceId) {
		Set<IFile> dependentList = dependencies.get((IFile)includedSourceId);
		if(dependentList == null) {
			dependentList = new HashSet<IFile>();
			dependencies.put((IFile)includedSourceId, dependentList);
		}
		dependentList.add((IFile)includerSourceId);
	}
	
	/**
	 * Returns the set of files depending of the given file
	 * @param fromSourceId
	 * @return
	 */
	public Set<IFile> getDependencies(IFile fromFile) {
		return dependencies.get((IFile)fromFile);
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
	
	public Stmt getStmt(String objectName) {
		if(logicalSystem == null) return null;
		
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

		public void clearMessages(IResource resource) {
			try {
				resource.deleteMarkers(MetamathProjectNature.MARKER_TYPE, false, IResource.DEPTH_ZERO);
			} catch (CoreException ce) {
			}
		}
	}

	protected static void addMarker(SourcePosition position, String message, int severity) {
		if(position == null) { System.out.println("Skipped marker for "+message); return; }
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

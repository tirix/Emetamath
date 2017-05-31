package org.tirix.emetamath.nature;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.editors.proofassistant.ProofWorksheetDocumentProvider.ProofWorksheetInput;
import org.tirix.emetamath.nature.MetamathProjectNature.DocumentSource;
import org.tirix.emetamath.views.MMLabelProvider;
import org.tirix.mmj.MathMLTypeSetting;
import org.tirix.mmj.TypeSetting;

import mmj.gmff.GMFFManager;
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
import mmj.lang.WorkVarManager;
import mmj.mmio.MMIOConstants;
import mmj.mmio.MMIOConstants.SourcePositionContext;
import mmj.mmio.Source;
import mmj.mmio.SourcePosition;
import mmj.mmio.Systemizer;
import mmj.mmio.Systemizer.DependencyListener;
import mmj.pa.ErrorCode;
import mmj.pa.MMJException;
import mmj.pa.MMJException.ErrorContext;
import mmj.pa.MacroManager;
import mmj.pa.ProofAsst;
import mmj.pa.ProofAsstPreferences;
import mmj.tl.TheoremLoader;
import mmj.tl.TlPreferences;
import mmj.util.UtilConstants;
import mmj.verify.Grammar;
import mmj.verify.GrammarConstants;
import mmj.verify.GrammaticalParser;
import mmj.verify.VerifyException;
import mmj.verify.VerifyProofs;

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
	public static final QualifiedName LOGIC_STMT_TYPE_PROPERTY = new QualifiedName("org.tirix.emetamath", "logicStmtType");
	public static final QualifiedName TYPES_PROPERTY = new QualifiedName("org.tirix.emetamath", "types");
	public static final QualifiedName COLORS_PROPERTY = new QualifiedName("org.tirix.emetamath", "typeColors");
	public static final QualifiedName ICONS_PROPERTY = new QualifiedName("org.tirix.emetamath", "typeIcons");
	public static final QualifiedName WORKVARS_PROPERTY = new QualifiedName("org.tirix.emetamath", "typeWorkVars");
	public static final QualifiedName AUTO_TRANSFORMATIONS_ENABLED_PROPERTY = new QualifiedName("org.tirix.emetamath", "autoTransformationsEnabled");
	public static final QualifiedName LAST_FLAT_EXPORT_PROPERTY = new QualifiedName("org.tirix.emetamath", "lastFlatExport");
	
	private static final QualifiedName EXPLORER_BASE_URL_PROPERTY = new QualifiedName("org.tirix.emetamath", "explorerBaseUrl");
	
	public static final String EXPLORER_BASE_URL_DEFAULT_VALUE = "http://us.metamath.org/mpegif/";
	public static final String PROVABLE_TYPE_DEFAULT_VALUE = "|-";
	public static final String LOGIC_STMT_TYPE_DEFAULT_VALUE = "wff";
	public static final String TYPES_DEFAULT_VALUE = "wff$set$class";
	public static final String COLORS_DEFAULT_VALUE = "0,0,255$255,0,0$255,0,255";
	public static final String ICONS_DEFAULT_VALUE = "mmWff.gif$mmSet.gif$mmClass.gif";
	public static final String WORKVARS_DEFAULT_VALUE = "&W$&S$&C";
	public static final String DEFINITION_PREFIX_DEFAULT_VALUE = "df-";
	public static final Boolean AUTO_TRANSFORMATIONS_ENABLED_DEFAULT_VALUE = true;
	private IProject project;
	
	/**
	 * A table enabling to find the type of each variable
	 */
	private Cnst provableType;
	private List<Cnst> types;
	private Map<Cnst, RGB> typeColors;
	private Map<Cnst, Image> typeIcons;
	private Map<Cnst, String> typeIconURLs;
	private Map<Cnst, String> typeWorkVars;
	private Map<Sym,Stmt> notations;
	//private Cnst wffType, classType, setType;
	
	/**
	 * The main file, with which parsing/verification shall start
	 */
	protected IResource mainFile;
	
	/**
	 * The messageHandler to be provided to MMJ2
	 * redirects messages to the Eclipse Error Log.
	 */
	protected final MetamathMessageHandler messageHandler = new MetamathMessageHandler();

	/**
	 * The dependency map : contains the set of files depending on a given file
	 */
	protected final DependencyManager<IResource> dependencies = new DependencyManager<>();
	
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

    protected GMFFManager    gmffManager;

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
    
    protected Class<? extends GrammaticalParser> parserPrototype;
    
    protected LogicalSystem  logicalSystem;

    protected Systemizer     systemizer;

	protected Grammar		 grammar;

	protected boolean        logicalSystemLoaded;
	private Deque<SystemLoadListener> listeners;

	protected WorkVarManager workVarManager;
	protected VerifyProofs	 verifyProofs;
	protected boolean		 allProofsVerifiedSuccessfully;
	protected boolean		 allStatementsParsedSuccessfully;
	protected ProofAsst      proofAsst;

	private TheoremLoader theoremLoader;

    public MetamathProjectNature() {
    	listeners = new ArrayDeque<SystemLoadListener>();
    	types = new ArrayList<Cnst>();
		typeColors = new Hashtable<Cnst, RGB>();
		typeIcons = new Hashtable<Cnst, Image>();
		typeIconURLs = new Hashtable<Cnst, String>();
		typeWorkVars = new Hashtable<Cnst, String>();
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
    	preferences = MetamathPreferences.getInstance();
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
        gmffManager               = null;
        systemizer                = null;

        loadComments              =
            MMIOConstants.LOAD_COMMENTS_DEFAULT;
        loadProofs                =
            MMIOConstants.LOAD_PROOFS_DEFAULT;

        try {
        	provableLogicStmtTypeParm = getProvableTypeString();
        	logicStmtTypeParm = getLogicStmtTypeString();
        	if(provableLogicStmtTypeParm.equals(logicStmtTypeParm)) throw new RuntimeException("Provable type shall not be the same as the statement type!");
        } catch(Exception e) {
        	e.printStackTrace();
	        provableLogicStmtTypeParm =
	            GrammarConstants.
	                DEFAULT_PROVABLE_LOGIC_STMT_TYP_CODES[0];
	
	        logicStmtTypeParm         =
	            GrammarConstants.DEFAULT_LOGIC_STMT_TYP_CODES[0];
        }
        
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

    /**
     * Clear the logical system.
     * 
     * This is called when the environment was just opened, or when the MM files have been changed, 
     * before re-parsing and verifying it.
     * 
     * @param resource
     * @param offset
     * @param messageHandler
     */
    public void clearLogicalSystem(IResource resource, long offset, MessageHandler messageHandler) {
    	dependencies.clearDependenciesFrom(null, offset);
    	
    	logicalSystemLoaded   = false;
	    
	    if (logicalSystem == null) {
	        if (bookManager == null) {
	            bookManager       =
	                new BookManager(bookManagerEnabledParm,
	                                provableLogicStmtTypeParm);
	        }
	
            if (gmffManager == null)
                gmffManager = new GMFFManager(null, messageHandler);

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
	                    gmffManager,
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
	        systemizer = new Systemizer();
	        systemizer.setDependencyListener(this);
	    }
	    else {
	    	systemizer.clearFilesAlreadyLoaded(); // TODO remove only the specified file
	    }
	    
	    if(workVarManager != null) {
	    	workVarManager.clear();
	    }
	    
	    // TODO - initialize the existing ProofAsst instance instead of setting it to null
	    proofAsst = null;
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


	public MetamathPreferences getPreferences() {
		return preferences;
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

        parserPrototype = GrammarConstants.DEFAULT_PARSER_PROTOTYPE;
        
        try {
			grammar                   = new Grammar(pTyp,
			                                        lTyp,
			                                        gComplete,
			                                        sComplete,
			                                        parserPrototype);
		} catch (VerifyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
            messageHandler.accumMessage(
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
	            proofAsstPreferences.deriveAutocomplete.set(true); // TODO : include in a preference page
	
	            TheoremLoader theoremLoader = getTheoremLoader();
	            MacroManager macroManager = null;
	            
	            proofAsst             =
	                new ProofAsst(proofAsstPreferences,
	                              logicalSystem,
	                              grammar,
	                              verifyProofs,
	                              theoremLoader,
	                              macroManager);
	            
	            if (!proofAsst.getInitializedOK()) {
	                proofAsst.initializeLookupTables(messageHandler);
	            }
	
	            // TODO : include in a preference page
	            boolean autoTransformationsEnabled = true;
	            boolean autoTransformationsDebugOutput = false;
	            boolean autoTransformationsSupportPrefix = true;
	            proofAsst.initAutotransformations(
	            	autoTransformationsEnabled, autoTransformationsDebugOutput,
	            	autoTransformationsSupportPrefix);

	            logicalSystem.
	                accumTheoremLoaderCommitListener(
	                    proofAsst);
	
	        }
	        else {
	            proofAsst             = null;
	            messageHandler.accumMessage(
	                UtilConstants.ERRMSG_PA_REQUIRES_GRAMMAR_INIT);
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

	public boolean getLoadProofs() {
		return loadProofs;
	}

	private void initializeTypes() {
		notations = new Hashtable<Sym, Stmt>();
		types = new ArrayList<Cnst>();
		
		String provableTypeProperty = null;
		String typesProperty = null;
		String colorsProperty = null;
		String iconsProperty = null;
		String workvarsProperty = null;
		try {
			provableTypeProperty = getProvableTypeString();
			typesProperty = getProject().getPersistentProperty(TYPES_PROPERTY);
			colorsProperty = getProject().getPersistentProperty(COLORS_PROPERTY);
			iconsProperty = getProject().getPersistentProperty(ICONS_PROPERTY);
			workvarsProperty = getProject().getPersistentProperty(WORKVARS_PROPERTY);
		}
		catch(CoreException e) {}
		if(provableTypeProperty == null) provableTypeProperty = PROVABLE_TYPE_DEFAULT_VALUE;
		if(typesProperty == null) typesProperty = TYPES_DEFAULT_VALUE;
		if(colorsProperty == null) colorsProperty = COLORS_DEFAULT_VALUE;
		if(iconsProperty == null) iconsProperty = ICONS_DEFAULT_VALUE;
		if(workvarsProperty == null) workvarsProperty = WORKVARS_DEFAULT_VALUE;

		// get the 'provable' type
		provableType = (Cnst)logicalSystem.getSymTbl().get(provableTypeProperty);

		// get the other types
		types = parseTypesString(typesProperty);
		
		// get the coloring attributes for the other types
		typeColors = parseTypeColorsString(colorsProperty, types);
		
		// get the icons for the other types
		typeIcons = parseIconsString(iconsProperty, types);
		
		// get the workvars prefix for the other types
		typeWorkVars = parseWorkVars(workvarsProperty, types);
		
//		wffType = (Cnst)logicalSystem.getSymTbl().get("wff");
//		setType = (Cnst)logicalSystem.getSymTbl().get("set");
//		classType = (Cnst)logicalSystem.getSymTbl().get("class");
		
		grammar = getGrammar();
		initializeGrammar(messageHandler);
		
		// Configure the Working Variables manager
		for(Cnst type:typeWorkVars.keySet())
			try {
				if(!type.getId().equals(logicStmtTypeParm))
					getWorkVarManager().defineWorkVarType(grammar, type.getId(), typeWorkVars.get(type), 200);
			} catch (VerifyException e) {
				messageHandler.accumException(e);
			}
	}
	
	private List<Cnst> parseTypesString(String input) {
		List<Cnst> types = new ArrayList<Cnst>();
		for(String typeName:input.split("\\$")) {
			Cnst type = (Cnst)logicalSystem.getSymTbl().get(typeName);
			if(type != null) types.add(type);
			else System.err.println("Could not find type "+typeName);
		}
		return types;
	}
	
	private Map<Cnst, RGB> parseTypeColorsString(String input, List<Cnst> types) {
		Map<Cnst, RGB> typeColors = new Hashtable<Cnst, RGB>();
		String[] colors = input.split("\\$");
		for(int i=0;i<colors.length && i<types.size();i++) {
			String[] rgb = colors[i].split(",");
			RGB color = new RGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			typeColors.put(types.get(i), color);
		}
		if(colors.length != types.size()) System.out.println("Project "+getProject()+": "+types.size()+" types ("+types+") but "+colors.length+" colors ("+colors+") defined!\n");
		return typeColors;
	}
	
	private Map<Cnst, Image> parseIconsString(String iconsProperty, List<Cnst> types) {
		Hashtable<Cnst, Image> typeIcons = new Hashtable<Cnst, Image>();
		String[] icons = iconsProperty.split("\\$");
		for(int i=0;i<icons.length && i<types.size();i++) {
			typeIcons.put(types.get(i), Activator.getImage("icons/"+icons[i]));
			typeIconURLs.put(types.get(i), "icons/"+icons[i]);
		}
		if(icons.length != types.size()) System.out.println("Project "+getProject()+": "+types.size()+" types ("+types+") but "+icons.length+" icons ("+icons+") defined!\n");
		return typeIcons;
	}

	private Map<Cnst, String> parseWorkVars(String workVarsProperty, List<Cnst> types) {
		Hashtable<Cnst, String> typeWorkVars = new Hashtable<Cnst, String>();
		String[] workvars = workVarsProperty.split("\\$");
		for(int i=0;i<workvars.length && i<types.size();i++) {
			typeWorkVars.put(types.get(i), workvars[i]);
		}
		if(workvars.length != types.size()) System.out.println("Project "+getProject()+": "+types.size()+" types ("+types+") but "+workvars.length+" icons ("+workvars+") defined!\n");
		return typeWorkVars;
	}

	private String toTypeString(List<Cnst> types) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<types.size();i++) {
			if(i!=0) sb.append("$");
			sb.append(types.get(i).getId());
		}
		return sb.toString();
	}
	
	private String toTypeColorString(Map<Cnst, RGB> typeColors) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<typeColors.size();i++) {
			if(i!=0) sb.append("$");
			RGB color = typeColors.get(types.get(i));
			sb.append(color.red+","+color.green+","+color.blue+",");
		}
		return sb.toString();
	}
	
	private String toTypeWorkVarsString(Map<Cnst, String> typeWorkVars) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<typeWorkVars.size();i++) {
			if(i!=0) sb.append("$");
			sb.append(typeWorkVars.get(types.get(i)));
		}
		return sb.toString();
	}
	
	public Map<Cnst, RGB> getDefaultTypeColors() {
		// TODO there should be a way to guess which are the types, and assign them default colors.
		List<Cnst> defaultTypes = parseTypesString(TYPES_DEFAULT_VALUE);
		return parseTypeColorsString(COLORS_DEFAULT_VALUE, defaultTypes);
	}

	public void setTypeColors(Map<Cnst, RGB> typeColors) throws CoreException {
		this.types = new ArrayList<Cnst>();
		this.types.addAll(typeColors.keySet());
		this.typeColors = typeColors;
		getProject().setPersistentProperty(TYPES_PROPERTY, toTypeString(types));
		getProject().setPersistentProperty(COLORS_PROPERTY, toTypeColorString(typeColors));
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
	
	public void setTypeWorkVars(Map<Cnst, String> typeWorkVars) throws CoreException {
		this.typeWorkVars = typeWorkVars;
		getProject().setPersistentProperty(WORKVARS_PROPERTY, toTypeWorkVarsString(typeWorkVars));
	}
	
	public Map<Cnst, String> getTypeWorkVars() {
		return typeWorkVars;
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

	public Cnst getLogicStmtType() {
		Sym logicStmtType = getLogicalSystem().getSymTbl().get(logicStmtTypeParm);
		if(logicStmtType instanceof Cnst) return (Cnst)logicStmtType;
		return null;
	}

	public String getLogicStmtTypeString() throws CoreException {
		String logicStmtTypeString =  getProject().getPersistentProperty(LOGIC_STMT_TYPE_PROPERTY);
		if(logicStmtTypeString == null) logicStmtTypeString = LOGIC_STMT_TYPE_DEFAULT_VALUE;
		return logicStmtTypeString;
	}

	public void setLogicStmtType(Cnst logicStmtType) throws CoreException {
		System.out.println("Setting logicStmtType="+logicStmtType);
		if(logicStmtType == null) return;
		this.logicStmtTypeParm = logicStmtType.getId();
		getProject().setPersistentProperty(LOGIC_STMT_TYPE_PROPERTY, logicStmtType.getId());
		rebuild();
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
		System.out.println("Setting provableType="+provableType);
		this.provableType = provableType;
		if(provableType == null) return;
		getProject().setPersistentProperty(PROVABLE_TYPE_PROPERTY, provableType.getId());
		rebuild();
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
		if(mainFile == null || !mainFile.exists()) return;
		this.mainFile = mainFile;
		mainFile.setPersistentProperty(ISMAINFILE_PROPERTY, Boolean.toString(true));
		getProject().setPersistentProperty(MAINFILE_PROPERTY, mainFile.getName());
		dependencies.setMainFile(mainFile);
		rebuild();
	}

	public void rebuild() {
		//System.out.println("Rebuilding metamath");
		// perform an incremental build, starting from this new file
		Job buildJob = new Job("Rebuild") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.setTaskName("Verifying proofs for "+mainFile.getName());
					getProject().refreshLocal(IResource.DEPTH_ONE, null);
					getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
//System.out.println("Rebuilding metamath complete");
					return new Status(Status.OK, "eMetamath", "Job finished"); 
				} catch (CoreException e) {
					return new Status(Status.ERROR, "eMetamath", "Error during rebuild", e); 
				}
			}};
		//buildJob.setSystem(true); // System jobs are typically not revealed to users in any UI presentation of jobs. 
		buildJob.schedule();
	}
	
	public IResource getLastFlatExport() throws CoreException {
		String lastFlatExportFileName = getProject().getPersistentProperty(LAST_FLAT_EXPORT_PROPERTY);
		if(lastFlatExportFileName == null) return null;
		return getProject().getFile(lastFlatExportFileName);
	}

	public void setLastFlatExport(final IResource lastFlatExport) throws CoreException {
		getProject().setPersistentProperty(LAST_FLAT_EXPORT_PROPERTY, lastFlatExport.getName());
	}

	public boolean isAutoTransformationsEnabled() throws CoreException {
		return Boolean.valueOf(getProject().getPersistentProperty(AUTO_TRANSFORMATIONS_ENABLED_PROPERTY));
	}

	public void setAutoTransformationsEnabled(boolean enabled) throws CoreException {
		getProject().setPersistentProperty(AUTO_TRANSFORMATIONS_ENABLED_PROPERTY, Boolean.toString(enabled));
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

    /**
     * Set the grammar parser by class name.
     */
    @SuppressWarnings("unchecked")
    public void setParserType(String parserType) {
        try {
            parserPrototype = (Class<? extends GrammaticalParser>)Class
                .forName(parserType);
        } catch (final ClassNotFoundException e) {
            //throw error(e, ERRMSG_RUNPARM_PARSER_BAD_CLASS, parserType);
        }
    }

	@Override
	public void addDependency(Source includerSourceId, Source includedSourceId, long offset) {
		dependencies.addDependency(
			(IFile)((ResourceSource)includerSourceId).resource, 
			(IFile)((ResourceSource)includedSourceId).resource,
			offset);
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
	
	/**
	 * A source implementation for Eclipse resources
	 * @author Thierry
	 */
	public static class ResourceSource implements Source {
		public final IContainer rootContainer;
		public final IResource resource;
		
		public ResourceSource(IResource resource, IContainer rootContainer) {
			this.rootContainer = rootContainer;
			this.resource = resource;
		}

		@Override
		public Reader createReader() throws FileNotFoundException {
			if(!(resource instanceof IFile)) throw new FileNotFoundException("Not a file resource! "+resource);
			IFile file = (IFile)resource;
			try {
				return new InputStreamReader(file.getContents());
			}
			catch(CoreException e) {
				throw new FileNotFoundException("File not found : "+file.getName());
			}
		}

		@Override
		public String getContents() throws IOException {
			Reader reader = createReader();
			int intValueOfChar;
			String targetString = "";
		    while ((intValueOfChar = reader.read()) != -1) {
		        targetString += (char) intValueOfChar;
		    }
		    return targetString;
		}
		
		@Override
		public void replace(int offset, int length, String text) {
			throw new UnsupportedOperationException("Replacing contents of resource source is not implemented - normally not used!");
		}
		
		@Override
		public int getSize() throws FileNotFoundException {
			return (int)resource.getLocation().toFile().length();
		}

		@Override
		public String toString() {
			return resource.getProjectRelativePath().toString();
		}

		@Override
		public String getUniqueId() throws IOException {
			return resource.getLocation().makeAbsolute().toString();
		}

		@Override
		public Source createSourceId(String fileName) {
			return new ResourceSource(rootContainer.getFile(new Path(fileName)), rootContainer);
		}

		/**
		 * 
		 * @param defaultPosition
		 * @param message
		 * @param severity
		 */
		public void addMarker(SourcePosition position, String message, int severity) {
			if(position == null) { System.out.println("Skipped marker for "+message); return; }
			try {
				IMarker marker = resource.createMarker(MARKER_TYPE);

				position = position.refinePosition();
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.SEVERITY, severity);
				int lineNbr = position.lineNbr;
				if (lineNbr == -1) {
					lineNbr = 1;
				}
				int charEndNbr = position.charEndNbr;
				int charStartNbr = position.charStartNbr;
				if (charStartNbr == -1) {
					charStartNbr = position.getCharStartNbr();
					charEndNbr += charStartNbr;
				}
				marker.setAttribute(IMarker.LINE_NUMBER, lineNbr);
				marker.setAttribute(IMarker.CHAR_START, charStartNbr);
				marker.setAttribute(IMarker.CHAR_END, charEndNbr);
			} catch (CoreException e) {
			}
		}

		public SourcePosition getEndPosition() throws FileNotFoundException {
			return new SourcePosition(this, -1, -1, getSize(), getSize());
		}
	}
	
	/**
	 * A source implementation for Eclipse JFace Text Documents
	 */
	public static class DocumentSource implements Source {
		public final TextEditor editor;
		public final IDocument document;
		public final String name;
		
		public DocumentSource(IDocument document, TextEditor editor, String name) {
			this.document = document;
			this.editor = editor;
			this.name = name;
		}

		@Override
		public Reader createReader() throws FileNotFoundException {
			return new Reader() {
				int offset = 0;
				
				@Override
				public int read(char[] cbuf, int off, int len) throws IOException {
					if(!ready()) return -1;
					len = Math.min(len, document.getLength() - offset);
					for(int i = 0; i< len; i++)
						try {
							cbuf[off + i] = document.getChar(offset+i);
						} catch (BadLocationException e) {
							throw new IOException(e);
						}
					offset += len;
					return len;
				}
				
				@Override
				public boolean ready() throws IOException {
					return offset < document.getLength();
				}
				
				@Override
				public void close() throws IOException {
				}
			};
		}

		public void replace(int offset, int length, String text) throws IOException {
			try {
				document.replace(offset, length, text);
			} catch (BadLocationException e) {
				throw new IOException(e);
			}
		}
		
		@Override
		public String getContents() throws IOException {
			return document.get();
		}
		
		@Override
		public int getSize() throws FileNotFoundException {
			return document.getLength();
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public String getUniqueId() throws IOException {
			return name;
		}

		@Override
		public Source createSourceId(String fileName) {
			throw new UnsupportedOperationException("Cannot create source from a document source!");
		}

		/**
		 * Creates an annotation at the given position.
		 * 
		 * @param position the position in the source where to display the annotation. Source shall match this object.
		 * @param message the message to be displayed when hovering over the annotation
		 * @param severity one of IMarker.SEVERITY_INFO, IMarker.SEVERITY_WARNING, IMarker.SEVERITY_ERROR. Defines the image and color displayed (info / warning / error)
		 */
		public void addAnnotation(SourcePosition position, String message, int severity) {
			if(!editor.isDirty()) {
				// if editor has been saved, revert to the marker...
				IFile resource = ((FileEditorInput)editor.getEditorInput()).getFile();
				new ResourceSource(resource, resource.getProject()).addMarker(position, message, severity);
				return;
			}
			IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			if(annotationModel == null) return;
				
		    String annotationType;
			switch(severity) {
			case IMarker.SEVERITY_INFO: annotationType = "org.eclipse.ui.workbench.texteditor.info"; break;
			case IMarker.SEVERITY_WARNING: annotationType = "org.eclipse.ui.workbench.texteditor.warning"; break;
			case IMarker.SEVERITY_ERROR: annotationType = "org.eclipse.ui.workbench.texteditor.error"; break;
			default: annotationType = "org.eclipse.ui.workbench.texteditor.info"; break;
		    }
//			System.out.println("Adding annotation @ "+position.getCharStartNbr() + " len=" + position.getLength()+" : "+message);
			Annotation annotation = new Annotation(annotationType, false, message);
			annotationModel.addAnnotation(annotation, new Position(position.getCharStartNbr(), position.getLength()));

//			@SuppressWarnings( "unchecked" )
//			Iterator<Annotation> i = annotationModel.getAnnotationIterator();
//			int count = 0;
//			while(i.hasNext()) { System.out.println("  - "+i.next().getText()); count++; }
//			System.out.println("Now "+count+" annotations in "+this);
		}
	}
	
	public static class MetamathMessageHandler implements MessageHandler {
		protected SourcePosition defaultPosition;
		protected ResourceSource defaultResource;
		private boolean hasErrors;
		
		MetamathMessageHandler() {
		}
		
		MetamathMessageHandler(IResource defaultResource) {
			setDefaultResource(defaultResource);
		}
		
		public int codeSeverity(ErrorCode code) {
			// return e.code.level.error ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_INFO;
			int severity = 0;
			switch(code.level) {
			case Abort: severity = IMarker.SEVERITY_ERROR; break;
			case Error: severity = IMarker.SEVERITY_ERROR; break;
			case Warn: severity = IMarker.SEVERITY_WARNING; break;
			case Info: severity = IMarker.SEVERITY_INFO; break;
			case Debug: severity = IMarker.SEVERITY_INFO; break;
			}
			return severity;
		}

		public void setDefaultResource(IResource defaultResource) {
			this.defaultResource = new ResourceSource(defaultResource, null);
			this.defaultPosition = new SourcePosition(this.defaultResource, 0, 0, 0, 0);
		}
		
		private void addMarker(SourcePosition position, String message, int severity) {
			if(position == null) {
				defaultResource.addMarker(defaultPosition, message, severity);
			} else if(position.source instanceof DocumentSource) {
				((DocumentSource)position.source).addAnnotation(position, message, severity);
//				// Test - add dummy annotation
//				((DocumentSource)position.source).addAnnotation(new SourcePosition(position.source, -1, -1, 400, 410), "This is a test annotation", IMarker.SEVERITY_ERROR);
			} else if(position.source instanceof ResourceSource) {
				((ResourceSource)position.source).addMarker(position, message, severity);
			} else {
				defaultResource.addMarker(defaultPosition, message, severity);
			}
		}

		@Override
		public boolean accumException(MMJException e) {
	        if (e == null || !e.code.use()) return true;
	        SourcePosition position = e.position;
	        for(ErrorContext ctxt : e.ctxt)
	        	if(ctxt instanceof SourcePositionContext)
	        		position = ((SourcePositionContext)ctxt).getPosition();
	        return accumMessage(position, e.getMessage(), codeSeverity(e.code));
		}

		@Override
		public boolean accumErrorMessage(String errorMessage, Object... args) {
			return accumMessage(null, ErrorCode.format(errorMessage, args), IMarker.SEVERITY_ERROR);
		}

		@Override
		public boolean accumInfoMessage(SourcePosition position, String infoMessage, Object... args) {
			return accumMessage(position, ErrorCode.format(infoMessage, args), IMarker.SEVERITY_INFO);
		}

		@Override
		public boolean accumInfoMessage(String infoMessage, Object... args) {
			return accumInfoMessage(null, args);
		}

		@Override
		public boolean accumMessage(ErrorCode code, Object... args) {
			return accumMessage(null, code, args);
		}

		@Override
		public boolean accumMessage(SourcePosition position, ErrorCode code, Object... args) {
			return accumMessage(position, code.message(args), codeSeverity(code));
		}

		public boolean accumMessage(SourcePosition position, String message, int severity) {
			addMarker(position, message, severity);
			hasErrors = severity == IMarker.SEVERITY_ERROR;
			return false;
		}
		
		@Override
		public boolean hasErrors() {
			return hasErrors;
		}

		@Override
		public boolean maxErrorMessagesReached() {
			return false;
		}

		@Override
		public String getOutputMessageText() {
			throw new RuntimeException("This implementation does not provide consolidated text output.");
		}
		
		@Override
		public String getOutputMessageTextAbbrev() {
			throw new RuntimeException("This implementation does not provide consolidated text output.");
		}

		public void clearMessages(IResource resource) {
			try {
				resource.deleteMarkers(MetamathProjectNature.MARKER_TYPE, false, IResource.DEPTH_ZERO);
			} catch (CoreException ce) {
			}
		}
	}
}

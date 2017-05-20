package org.tirix.emetamath.nature;


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
import mmj.lang.SeqAssigner;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Var;
import mmj.mmio.MMIOConstants;
import mmj.mmio.Systemizer;
import mmj.util.RunParmArrayEntry;
import mmj.util.UtilConstants;
import mmj.verify.GrammarConstants;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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

    protected boolean        logicalSystemLoaded;
	private ArrayList<SystemLoadListener> listeners;
    
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
	
	public IResource getMainFile() throws CoreException {
		String mainFileName = getProject().getPersistentProperty(MAINFILE_PROPERTY);
		if(mainFileName == null) return null;
		return getProject().getFile(mainFileName);
	}

	public void setMainFile(final IResource mainFile) throws CoreException {
		if(this.mainFile != null) this.mainFile.setPersistentProperty(ISMAINFILE_PROPERTY, null);
		this.mainFile = mainFile;
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
}

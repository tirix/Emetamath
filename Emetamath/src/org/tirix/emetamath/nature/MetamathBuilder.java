package org.tirix.emetamath.nature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import mmj.lang.BookManager;
import mmj.lang.LangConstants;
import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.SeqAssigner;
import mmj.mmio.MMIOException;
import mmj.mmio.SourcePosition;
import mmj.mmio.Systemizer;
import mmj.util.LoadProgress;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Makes use of Mel'O Cat's MMJ library to load, parse and verify a metamath file. 
 * @author Thierry
 */
public class MetamathBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_ID = "org.tirix.emetamath.metamathBuilder";

	private static final String MARKER_TYPE = "org.tirix.emetamath.MMProblem";

	
	/**
	 * Adds the processing builder to a project
	 * 
	 * @param project the project whose build spec we are to modify
	 */
	public static void addBuilderToProject(IProject project){
		
		//cannot modify closed projects
		if (!project.isOpen())
				return;
		
		IProjectDescription description;
		try{
			description = project.getDescription();
		} catch (Exception e){
			e.printStackTrace();
			return;
		}
		
		// Look for builders already associated with the project
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++){
			if (cmds[j].getBuilderName().equals(BUILDER_ID))
				return;
		}
		
		//Associate builder with project.
		ICommand newCmd = description.newCommand();
		newCmd.setBuilderName(BUILDER_ID);
		List newCmds = new ArrayList();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.add(newCmd);
		description.setBuildSpec(
			(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		try{
			project.setDescription(description,null);
		} catch (CoreException e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * Remove the processing builder from the project
	 * 
	 * @param project the project whose build spec we are to modify
	 */
	public static void removeBuilderFromProject(IProject project){
		
		//cannot modify closed projects
		if (!project.isOpen())
				return;
		
		IProjectDescription description;
		try{
			description = project.getDescription();
		} catch (Exception e){
			e.printStackTrace();
			return;
		}
		
		// Look for the builder
		int index = -1;
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++){
			if (cmds[j].getBuilderName().equals(BUILDER_ID)){
				index = j;
				break;
			}
		}
		if (index == -1)
			return;
		
		//Remove builder with project.
		List newCmds = new ArrayList();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.remove(index);
		description.setBuildSpec(
			(ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
		try{
			project.setDescription(description,null);
		} catch (CoreException e){
			e.printStackTrace();
		}				
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		
		MetamathProjectNature nature = (MetamathProjectNature)getProject().getNature(MetamathProjectNature.NATURE_ID);
		
		// TODO for now, always do full build...
		fullBuild(nature, monitor);
		
//		if (kind == FULL_BUILD) {
//			fullBuild(nature, monitor);
//		} else {
//			IResourceDelta delta = getDelta(getProject());
//			if (delta == null) {
//				fullBuild(nature, monitor);
//			} else {
//				incrementalBuild(delta, nature, monitor);
//			}
//		}
		return null;
	}

	/**
	 * Perform a full build
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void fullBuild(MetamathProjectNature nature, final IProgressMonitor monitor) throws CoreException {
		// delete all project markers
		nature.getProject().deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		// empty logical system, in order not to have duplicate symbols
		nature.logicalSystem = null;
		buildMetamath(nature, nature.getMainFile(), monitor);
	}

	/**
	 * Perform an incremental build
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void incrementalBuild(IResourceDelta delta, MetamathProjectNature nature, 
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new MmDeltaVisitor(nature, monitor));
	}

	public static class MmDeltaVisitor implements IResourceDeltaVisitor {
		MetamathProjectNature nature;
		IProgressMonitor monitor;
		
		public MmDeltaVisitor(MetamathProjectNature nature, IProgressMonitor monitor) {
			this.nature = nature;
			this.monitor = monitor;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				buildMetamath(nature, resource, monitor);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				buildMetamath(nature, resource, monitor);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	public static class MmResourceVisitor implements IResourceVisitor {
		MetamathProjectNature nature;
		IProgressMonitor monitor;

		public MmResourceVisitor(MetamathProjectNature nature, IProgressMonitor monitor) {
			this.nature = nature;
			this.monitor = monitor;
		}

		public boolean visit(IResource resource) {
			buildMetamath(nature, resource, monitor);
			//return true to continue visiting children.
			return true;
		}
	}

	private static void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	static void buildMetamath(MetamathProjectNature nature, IResource resource, IProgressMonitor monitor) {
		if (resource instanceof IFile && resource.getName().endsWith(".mm")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);
			MetamathMessageHandler messageHandler = new MetamathMessageHandler(file);
			try {
				// TODO get nature from file.getProject().getNature()?
				doLoadFile(file, nature, messageHandler, monitor);
			} catch (Exception e) {
			}
		}
	}

	/**
	 *  Execute the LoadFile command:
	 *  validates RunParm, loads the Metamath file, prints
	 *  any error messages and keeps a reference to the
	 *  loaded LogicalSystem for future reference.
	 *  <p>
	 *  Note: Systemizer does not (yet) have a Tokenizer
	 *        setter method or constructor. This would
	 *        be needed to enable use of non-ASCII
	 *        codesets (there is only one Tokenizer
	 *        at present and it hardcodes character
	 *        values based on the Metamath.pdf
	 *        specification.) To make this change it
	 *        would be necessary to create a Tokenizer
	 *        interface.
	 *
	 *  @param runParm RunParmFile line.
	 * @param nature TODO
	 */
	public static void doLoadFile(IFile file, MetamathProjectNature nature, MessageHandler messageHandler, IProgressMonitor monitor)
	                    throws IllegalArgumentException,
	                           MMIOException,
	                           FileNotFoundException,
	                           IOException {
	
	    nature.logicalSystemLoaded   = false;
	
	    if (nature.logicalSystem == null) {
	        if (nature.bookManager == null) {
	            nature.bookManager       =
	                new BookManager(nature.bookManagerEnabledParm,
	                                nature.provableLogicStmtTypeParm);
	        }
	
	        if (nature.seqAssigner == null) {
	            nature.seqAssigner       =
	                new SeqAssigner(
	                    nature.seqAssignerIntervalSizeParm,
	                    nature.seqAssignerIntervalTblInitialSizeParm);
	        }
	
	        int i = nature.symTblInitialSizeParm;
	        if (i <= 0) {
	            i = LangConstants.SYM_TBL_INITIAL_SIZE_DEFAULT;
	        }
	        int j = nature.symTblInitialSizeParm;
	        if (j <= 0) {
	            j = LangConstants.STMT_TBL_INITIAL_SIZE_DEFAULT;
	        }
	
	        nature.logicalSystem     =
	            new LogicalSystem(
	                    nature.provableLogicStmtTypeParm,
	                    nature.logicStmtTypeParm,
	                    nature.bookManager,
	                    nature.seqAssigner,
	                    i,
	                    j,
	                    null,  //use null to override default
	                    null); //use null to override default
	    }
	    else {
	        // precautionary, added for 08/01/2008 release
	        nature.logicalSystem.setSyntaxVerifier(null);
	        nature.logicalSystem.setProofVerifier(null);
	        nature.logicalSystem.clearTheoremLoaderCommitListenerList();
	    }
	
	    if (nature.systemizer == null) {
	        nature.systemizer        =
	        new Systemizer(messageHandler,
	                       nature.logicalSystem);
	    }
	    else {
	        nature.systemizer.setSystemLoader(nature.logicalSystem);
	        nature.systemizer.setMessages(messageHandler);
	    }
	
	    LoadProgressMonitor loadProgress = new LoadProgressMonitor("Loading System", monitor);
	    loadProgress.addTask(file.getLocation().toFile().length());
	    
	    nature.systemizer.setLimitLoadEndpointStmtNbr(
	            nature.loadEndpointStmtNbrParm);
	    nature.systemizer.setLimitLoadEndpointStmtLabel(
	            nature.loadEndpointStmtLabelParm);
	    nature.systemizer.setLoadComments(nature.loadComments);
	    nature.systemizer.setLoadProofs(nature.loadProofs);
	    nature.systemizer.setLoadProgress(loadProgress);
	    
	    try {
			nature.systemizer.load(new InputStreamReader(file.getContents()), file);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	    nature.setLogicalSystemLoaded();
	}
	
	public static class MetamathMessageHandler implements MessageHandler {
		protected IFile file;
		
		MetamathMessageHandler(IFile file) {
			this.file = file;
		}
		
		private void addMarker(String message, int severity) {
			MetamathBuilder.addMarker(new SourcePosition(file, 0, 0, 0, 0), message, severity);
		}

		@Override
		public boolean accumMMIOException(MMIOException e) {
			MetamathBuilder.addMarker(e.position, e.getMessage(), IMarker.SEVERITY_ERROR);
			return false;
		}

		@Override
		public boolean accumErrorMessage(String errorMessage) {
			addMarker(errorMessage, IMarker.SEVERITY_ERROR);
			return false;
		}

		@Override
		public boolean accumInfoMessage(String infoMessage) {
			addMarker(infoMessage, IMarker.SEVERITY_INFO);
			return false;
		}

		@Override
		public boolean maxErrorMessagesReached() {
			return false;
		}
	}

	protected static void addMarker(SourcePosition position, String message, int severity) {
		try {
			IFile file = (IFile)position.sourceId;
			IMarker marker = file.createMarker(MARKER_TYPE);
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

	protected static class LoadProgressMonitor implements LoadProgress {
		long workDone = 0;
		long workRemaining;
		private IProgressMonitor monitor;
		private final int TOTAL_WORK = 1000;
		
		public LoadProgressMonitor(String taskName, IProgressMonitor monitor) {
			this.monitor = monitor;
			monitor.beginTask(taskName, TOTAL_WORK);
		}

		@Override
		public void addTask(long work) {
			workRemaining += work - workDone;
			workDone = 0;
		}

		@Override
		public void worked(long work) {
			workDone += work;
			monitor.worked((int)(work * TOTAL_WORK / workRemaining));
		}
	}
}

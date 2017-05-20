package org.tirix.emetamath.nature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.tirix.emetamath.nature.MetamathProjectNature.MetamathMessageHandler;
import org.tirix.emetamath.nature.MetamathProjectNature.ResourceSource;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.ParseTree;
import mmj.lang.ParseTree.RPNStep;
import mmj.lang.Stmt;
import mmj.mmio.MMIOException;
import mmj.mmio.Source;
import mmj.util.Progress;
import mmj.util.UtilConstants;
import mmj.verify.Grammar;
import mmj.verify.VerifyException;
import mmj.verify.VerifyProofs;

/**
 * Makes use of Mel'O Cat's MMJ library to load, parse and verify a metamath file. 
 * @author Thierry
 */
public class MetamathBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_ID = "org.tirix.emetamath.metamathBuilder";

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
		List<ICommand> newCmds = new ArrayList<ICommand>();
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
		List<ICommand> newCmds = new ArrayList<ICommand>();
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
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
			throws CoreException {
		
		MetamathProjectNature nature = (MetamathProjectNature)getProject().getNature(MetamathProjectNature.NATURE_ID);
		
		if (kind == FULL_BUILD) {
			fullBuild(nature, monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(nature, monitor);
			} else {
				incrementalBuild(delta, nature, monitor);
			}
		}
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
		nature.getProject().deleteMarkers(MetamathProjectNature.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		// empty logical system, in order not to have duplicate symbols
		nature.clearLogicalSystem(nature.getMainFile(), 0, nature.getMessageHandler());
		buildMetamath(nature, nature.getMainFile(), 0, monitor);
	}

	/**
	 * Perform an incremental build
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void incrementalBuild(IResourceDelta delta, final MetamathProjectNature nature, 
			final IProgressMonitor monitor) throws CoreException {

		// Start with an empty build list
		nature.dependencies.clearBuildList();
		
		// the visitor does the work.
		//delta.accept(new MmDeltaVisitor(nature, monitor));
		delta.accept(new IResourceDeltaVisitor() {
			@Override
			public boolean visit(IResourceDelta delta) throws CoreException {
				// if the project itself was changed (no file extension), skip
				if(delta.getResource().getFileExtension() == null) return true;

				// if a MMP file was changed, do nothing 
				if(delta.getResource().getFileExtension().equals("mmp")) return true;
				
				// if a MM file was changed, mark it for building, taking dependencies into account.
				if(delta.getResource().getFileExtension().equals("mm")) { 
					System.out.println("Register change: "+delta+" "+delta.getKind());
					nature.dependencies.addChange(delta.getResource(), 0);
					return false; 
				}

				// if an XML typesetting file was changed, parse it again.
				if(delta.getResource().getName().equals("typesetting.xml")) { 
					System.out.println("Reloading Typesetting because of "+delta+" "+delta.getKind());
					loadTypesetting(nature, (IFile)delta.getResource(), monitor);
					return false; 
				}
				return false;
			}
		});

		// do a partial build based on information accumulated in nature.dependencies 
		partialBuild(nature, monitor);
	}

	/**
	 * Perform a partial build of only the listed resources
	 * @throws CoreException 
	 */
	protected void partialBuild(MetamathProjectNature nature, IProgressMonitor monitor) throws CoreException {
		// delete all project markers
		for(IResource r:nature.dependencies.getBuildList().keySet()) r.deleteMarkers(MetamathProjectNature.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		
		IResource target = nature.dependencies.getTopBuildFile();
		long offset = nature.dependencies.getTopBuildFileOffset();
		if(target == null) {
			System.out.println("Partial build: Nothing to be built!");
			return;
		}
		System.out.println("Would do Partial build: start from "+target+" @ "+offset);
		
		// TODO here we do a full build anyway...
		System.out.println("Full build");
		
		// empty logical system, in order not to have duplicate symbols
		nature.clearLogicalSystem(target, offset, nature.getMessageHandler()); // here a new MessageHandler was created...
		buildMetamath(nature, target, offset, monitor);
	}

	private void loadTypesetting(MetamathProjectNature nature, IFile typeSettingFile, IProgressMonitor monitor) {
		try {
			nature.getMathMLTypeSetting().setData(new InputStreamReader(typeSettingFile.getContents(), "UTF-8"));
		} catch (Exception e) {
			// use the monitor to report the exception?
			//e.printStackTrace();
			System.err.println(e.getMessage());
		} 
	}

		//	public static class MmDeltaVisitor implements IResourceDeltaVisitor {
//		MetamathProjectNature nature;
//		IProgressMonitor monitor;
//		
//		public MmDeltaVisitor(MetamathProjectNature nature, IProgressMonitor monitor) {
//			this.nature = nature;
//			this.monitor = monitor;
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
//		 */
//		public boolean visit(IResourceDelta delta) throws CoreException {
//			IResource resource = delta.getResource();
//			switch (delta.getKind()) {
//			case IResourceDelta.ADDED:
//				// handle added resource
//				buildMetamath(nature, resource, monitor);
//				break;
//			case IResourceDelta.REMOVED:
//				// handle removed resource
//				break;
//			case IResourceDelta.CHANGED:
//				// handle changed resource
//				buildMetamath(nature, resource, monitor);
//				break;
//			}
//			//return true to continue visiting children.
//			return true;
//		}
//	}
//
//	public static class MmResourceVisitor implements IResourceVisitor {
//		MetamathProjectNature nature;
//		IProgressMonitor monitor;
//
//		public MmResourceVisitor(MetamathProjectNature nature, IProgressMonitor monitor) {
//			this.nature = nature;
//			this.monitor = monitor;
//		}
//
//		public boolean visit(IResource resource) {
//			buildMetamath(nature, resource, monitor);
//			//return true to continue visiting children.
//			return true;
//		}
//	}

	static void buildMetamath(MetamathProjectNature nature, IResource resource, long offset, IProgressMonitor monitor) {
		if (resource instanceof IFile && resource.getName().endsWith(".mm")) {
			IFile file = (IFile) resource;
			// TODO why don't we use the MessageHandler from the ProjectNature ?
			MetamathMessageHandler messageHandler = new MetamathMessageHandler(file);
			messageHandler.clearMessages(file);
			try {
				SubMonitor progress = SubMonitor.convert(monitor, 100);
				ResourceSource source = new ResourceSource(file, nature.getProject());
				// TODO get nature from file.getProject().getNature()?
				doLoadFile(source, nature, messageHandler, progress.newChild(30));
				doInitGrammar(nature, messageHandler, progress.newChild(10));
				doParse(source, nature, messageHandler, progress.newChild(30));
				doVerifyProof(nature, messageHandler, progress.newChild(30));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				monitor.done();
			}
		}
	}

	private static void doInitGrammar(MetamathProjectNature nature, MetamathMessageHandler messageHandler,
				IProgressMonitor monitor) {
		monitor.beginTask("Initializing Metamath Grammar", 100);
		nature.initializeGrammar(messageHandler);
		monitor.worked(100);
		monitor.done();
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
	public static void doLoadFile(ResourceSource source, MetamathProjectNature nature, MessageHandler messageHandler, IProgressMonitor monitor)
	                    throws IllegalArgumentException,
	                           MMIOException,
	                           FileNotFoundException,
	                           IOException {
	
		// selectively clear the logical system for this file
		nature.clearLogicalSystem(source.resource, 0, messageHandler);

	    MMProgressMonitor loadProgress = new MMProgressMonitor("Loading Metamath Project", monitor);
	    
	    nature.systemizer.init(
	    		nature.messageHandler,
	    		nature.logicalSystem,
	    		nature.loadEndpointStmtNbrParm, 
	    		nature.loadEndpointStmtLabelParm, 
	    		nature.loadComments, 
	    		nature.loadProofs);
	    nature.systemizer.setLoadProgress(loadProgress);
	    
	    try {
			nature.systemizer.load(source);
		} catch (MMIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			monitor.done();
		}
	
	    nature.setLogicalSystemLoaded();
	}

    /**
     *  Executes the Parse command, prints any messages,
     *  etc.
     *
     *  @param runParm RunParmFile line.
     */
    public static void doParse(Source source, MetamathProjectNature nature, MessageHandler messageHandler, IProgressMonitor monitor)
                        throws IllegalArgumentException,
                               IOException,
                               VerifyException {

        LogicalSystem logicalSystem = nature.getLogicalSystem();

	    MMProgressMonitor parseProgress = new MMProgressMonitor("Parsing Metamath Project", monitor);
        
        Grammar grammar = nature.getGrammar();
        boolean parseAll = true;
        if (parseAll) {
            grammar.parseAllFormulas(
                messageHandler,
                logicalSystem.getSymTbl(),
                logicalSystem.getStmtTbl(),
                parseProgress);
            nature.allStatementsParsedSuccessfully = true;
        }
        else {
            Stmt stmt = null;
            ParseTree parseTree =
                grammar.parseOneStmt(messageHandler,
                                     logicalSystem.getSymTbl(),
                                     logicalSystem.getStmtTbl(),
                                     stmt);
            if (parseTree != null) {
                final RPNStep[] exprRPN = parseTree.convertToRPN();
                final StringBuilder sb = new StringBuilder();
                for (final RPNStep element : exprRPN)
                    sb.append(element).append(" ");
                messageHandler.accumMessage(UtilConstants.ERRMSG_PARSE_RPN, stmt, sb);
            }
        }

        logicalSystem.setSyntaxVerifier(grammar);
    }

    /**
     * Executes the VerifyProof command, prints any messages, etc.
     */
    public static void doVerifyProof(MetamathProjectNature nature, MessageHandler messageHandler, IProgressMonitor monitor) {
        final LogicalSystem logicalSystem = nature.getLogicalSystem();
        final VerifyProofs verifyProofs = nature.getVerifyProofs();

	    MMProgressMonitor verifyProgress = new MMProgressMonitor("Verifying Metamath Proofs", monitor);

        verifyProofs.setVerifyProgress(verifyProgress);
        verifyProofs.verifyAllProofs(messageHandler, logicalSystem.getStmtTbl());

        logicalSystem.setProofVerifier(verifyProofs);
    }

    /**
     * Executes the VerifyParse command, prints any messages, etc.
     */
    public static void doVerifyParse(MetamathProjectNature nature, MessageHandler messageHandler) {
        final LogicalSystem logicalSystem = nature.getLogicalSystem();
        final VerifyProofs verifyProofs = nature.getVerifyProofs();
        
        verifyProofs.verifyAllExprRPNAsProofs(messageHandler,
            logicalSystem.getStmtTbl());
        
        logicalSystem.setProofVerifier(verifyProofs);
    }

    public static class MMProgressMonitor implements Progress {
		long workDone = 0;
		long workRemaining = 0;
		private IProgressMonitor monitor;
		private final int TOTAL_WORK = 1000;
		
		public MMProgressMonitor(String taskName, IProgressMonitor monitor) {
			this.monitor = monitor;
			monitor.beginTask(taskName, TOTAL_WORK);
		}

		@Override
		public void addTask(long work) {
			workRemaining += work;
		}

		@Override
		public void worked(long work) {
			workDone += work;
			monitor.worked((int)(work * TOTAL_WORK / workRemaining));
			if(monitor.isCanceled()) throw new RuntimeException(new InterruptedException("Cancelled by user"));
		}
	}
}

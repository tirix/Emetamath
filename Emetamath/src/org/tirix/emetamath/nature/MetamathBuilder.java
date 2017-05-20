package org.tirix.emetamath.nature;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.lang.ParseTree;
import mmj.lang.Stmt;
import mmj.lang.VerifyException;
import mmj.mmio.IncludeFile;
import mmj.mmio.MMIOException;
import mmj.util.Progress;
import mmj.util.UtilConstants;
import mmj.verify.Grammar;

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
import org.tirix.emetamath.nature.MetamathProjectNature.MetamathMessageHandler;

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
		nature.clearLogicalSystem(null, new MetamathMessageHandler(nature.getProject()));
		buildMetamath(nature, nature.getMainFile(), monitor);
	}

	/**
	 * Perform an incremental build
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	protected void incrementalBuild(IResourceDelta delta, final MetamathProjectNature nature, 
			final IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		//delta.accept(new MmDeltaVisitor(nature, monitor));
		delta.accept(new IResourceDeltaVisitor() {
			@Override
			public boolean visit(IResourceDelta delta) throws CoreException {
				// if the project itself was changed (no file extension), skip
				if(delta.getResource().getFileExtension() == null) return true;

				// if a MMP file was changed, do nothing 
				if(delta.getResource().getFileExtension().equals("mmp")) return true;
				
				// if a MM file was changed, do a full build and stop.
				if(delta.getResource().getFileExtension().equals("mm")) { 
					// TODO for now, always do full build...
					System.out.println("Triggering full build because of "+delta+" "+delta.getKind());
					fullBuild(nature, monitor); 
					return false; 
				}

				// if an XML typesetting file was changed, parse it again.
				if(delta.getResource().getName().equals("typesetting.xml")) { 
					// TODO for now, always do full build...
					System.out.println("Reloading Typesetting because of "+delta+" "+delta.getKind());
					loadTypesetting(nature, (IFile)delta.getResource(), monitor);
					return false; 
				}
				return false;
			}
		});
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

	static void buildMetamath(MetamathProjectNature nature, IResource resource, IProgressMonitor monitor) {
		if (resource instanceof IFile && resource.getName().endsWith(".mm")) {
			IFile file = (IFile) resource;
			MetamathMessageHandler messageHandler = new MetamathMessageHandler(file);
			messageHandler.clearMessages(file);
			try {
				// TODO get nature from file.getProject().getNature()?
				doLoadFile(file, nature, messageHandler, monitor);
				nature.initializeGrammar(messageHandler);
				doParse(file, nature, messageHandler);
			} catch (Exception e) {
				e.printStackTrace();
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
	
		// selectively clear the logical system for this file
		nature.clearLogicalSystem(file, messageHandler);

	    LoadProgressMonitor loadProgress = new LoadProgressMonitor("Loading System", monitor);
	    loadProgress.addTask(file.getLocation().toFile().length());
	    
	    nature.systemizer.setLimitLoadEndpointStmtNbr(
	            nature.loadEndpointStmtNbrParm);
	    nature.systemizer.setLimitLoadEndpointStmtLabel(
	            nature.loadEndpointStmtLabelParm);
	    nature.systemizer.setLoadComments(nature.loadComments);
	    nature.systemizer.setLoadProofs(nature.loadProofs);
	    nature.systemizer.setLoadProgress(loadProgress);
	    
	    IncludeFile.setReaderProvider(nature.readerProvider);
	    
	    try {
			nature.systemizer.load(new InputStreamReader(file.getContents()), file);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	    nature.setLogicalSystemLoaded();
	}

    /**
     *  Executes the Parse command, prints any messages,
     *  etc.
     *
     *  @param runParm RunParmFile line.
     */
    public static void doParse(IFile file, MetamathProjectNature nature, MessageHandler messageHandler)
                        throws IllegalArgumentException,
                               IOException,
                               VerifyException {

        LogicalSystem logicalSystem = nature.getLogicalSystem();

        Grammar grammar = nature.getGrammar();

        if (true) {
            grammar.parseAllFormulas(
                messageHandler,
                logicalSystem.getSymTbl(),
                logicalSystem.getStmtTbl());
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
                Stmt[] exprRPN    =
                    parseTree.convertToRPN();
                StringBuffer sb = new StringBuffer();
                sb.append(
                    UtilConstants.ERRMSG_PARSE_RPN_1);
                sb.append(stmt.getLabel());
                sb.append(
                    UtilConstants.ERRMSG_PARSE_RPN_2);
                for (int i = 0; i < exprRPN.length; i++) {
                    sb.append(exprRPN[i].getLabel());
                    sb.append(" ");
                }
                messageHandler.accumInfoMessage(stmt.getPosition(), sb.toString());
            }
        }

        logicalSystem.setSyntaxVerifier(grammar);
    }

    protected static class LoadProgressMonitor implements Progress {
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

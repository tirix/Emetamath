package org.tirix.emetamath.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class SetMainFileAction implements IObjectActionDelegate {

	private Shell shell;
	private IResource selectedFile; 
	private MetamathProjectNature nature;

	/**
	 * Constructor for Action1.
	 */
	public SetMainFileAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if(!action.isEnabled()) return;
		try {
			MetamathProjectNature nature = (MetamathProjectNature) selectedFile.getProject().getNature(MetamathProjectNature.NATURE_ID);
			nature.setMainFile(selectedFile);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			selectedFile = (IResource)((TreeSelection)selection).getFirstElement();
			nature = (MetamathProjectNature)(selectedFile.getProject().getNature(MetamathProjectNature.NATURE_ID));
		} catch (CoreException e) {
			selectedFile = null;
		} catch (ClassCastException e) {
			selectedFile = null;
		}
		action.setEnabled(selectedFile != null);
	}

}

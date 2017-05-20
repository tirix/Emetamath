package org.tirix.emetamath.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
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
			action.setEnabled(selectedFile != null && !selectedFile.equals(nature.getMainFile()));
		} catch (CoreException e) {
			action.setEnabled(false);
		} catch (ClassCastException e) {
			action.setEnabled(false);
		}
	}
}

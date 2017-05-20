package org.tirix.emetamath.exportWizards;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ExportFlatMMWizard extends Wizard implements IExportWizard {
    private IProject root;

    private ExportFlatMMWizardPage mainPage;

	public ExportFlatMMWizard() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings
		        .getSection("FlatMMExportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ExportFlatMMWizard.java");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    public void addPages() {
    	if(root == null) return; // TODO error handling!
    	super.addPages();
        mainPage = new ExportFlatMMWizardPage(MetamathProjectNature.getNature(root));
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
		if(root == null) return false;
		MetamathProjectNature nature = MetamathProjectNature.getNature(root);
		IFile targetFile = mainPage.getTargetFile();
		boolean checkLineWidth = mainPage.getCheckLineWidth();
		boolean checkTabs = mainPage.getCheckTabs();
		System.out.println("Exporting project "+nature+" to "+targetFile);
		try {
			IFile mainFile = (IFile)nature.getMainFile();
			StringBuffer buffer = new StringBuffer();
			flattenFile(mainFile, buffer);
			boolean force = true;
			boolean keepHistory = true;
			IProgressMonitor monitor = null;
			targetFile.setContents(new StringBufferInputStream(buffer.toString()), force, keepHistory, monitor);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object first = selection.getFirstElement();
		if(first instanceof IProject) this.root = (IProject)first;
	}
	
	protected void flattenFile(IFile inputFile, StringBuffer out) throws IOException, CoreException {
		InputStreamReader reader = new InputStreamReader(inputFile.getContents());
		LineNumberReader lnr = new LineNumberReader(reader);
		Pattern pattern = Pattern.compile("\\$\\[ (.*) \\$\\]");
		String str;
		while((str = lnr.readLine()) != null) {
			Matcher matcher = pattern.matcher(str);
			if(matcher.matches()) {
				String fileName = matcher.group(1);
				IFile file = root.getFile(new Path(fileName));
				flattenFile(file, out);
			}
			else {
				out.append(str+"\n");
			}
		}
	}
}

package org.tirix.emetamath.exportWizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.WizardArchiveFileResourceExportPage1;

public class ExportMathMLWizard extends Wizard implements IExportWizard {
    private IStructuredSelection selection;

    private ExportMathMLWizardPage mainPage;

	public ExportMathMLWizard() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings
		        .getSection("MathMLExportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ExportMathMLWizard.java");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    public void addPages() {
        super.addPages();
        mainPage = new ExportMathMLWizardPage(selection);
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub

	}

}

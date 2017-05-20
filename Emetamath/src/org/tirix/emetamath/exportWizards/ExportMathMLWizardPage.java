package org.tirix.emetamath.exportWizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;

public class ExportMathMLWizardPage extends WizardExportResourcesPage implements IWizardPage {

	public ExportMathMLWizardPage(IStructuredSelection selection) {
		super("mathMLExportPage", selection);
        setTitle("Export MathML");
        setDescription("Export as HTML page containing MathML");
	}

	@Override
	protected void createDestinationGroup(Composite parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}

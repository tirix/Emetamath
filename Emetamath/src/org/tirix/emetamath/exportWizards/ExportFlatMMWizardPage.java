package org.tirix.emetamath.exportWizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ExportFlatMMWizardPage extends WizardPage implements IWizardPage {

	private MetamathProjectNature nature;
	private TreeViewer resourceTreeViewer;
    private Button checkTabs;
    private Button checkLineWidth;
    
	public ExportFlatMMWizardPage(MetamathProjectNature nature) {
		super("flatMMExportPage");
        setTitle("Export Flat Metamath file");
        setDescription("Export as flat Metamath file");
        this.nature = nature;
	}

	@Override
	public void createControl(Composite parent) {

        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite.setFont(font);

//        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
//                IIDEHelpContextIds.XXX_WIZARD_PAGE);

        Label referenceLabel = new Label(composite, SWT.NONE);
        referenceLabel.setText("Target Metamath file");
        referenceLabel.setFont(font);

        resourceTreeViewer = new TreeViewer(composite, SWT.SINGLE);
        resourceTreeViewer.getTree().setFont(composite.getFont());
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);

        //data.heightHint = getDefaultFontHeight(resourceTreeViewer .getTree());
        resourceTreeViewer.getTree().setLayoutData(data);
        resourceTreeViewer.setLabelProvider(WorkbenchLabelProvider
                .getDecoratingWorkbenchLabelProvider());
        resourceTreeViewer.setContentProvider(getContentProvider());
        resourceTreeViewer.setComparator(new ViewerComparator());
        resourceTreeViewer.setInput(ResourcesPlugin.getWorkspace());
        resourceTreeViewer.setSelection(getlastFlatExported() , true);
        
        Label checkTabsLabel = new Label(composite, SWT.NONE);
        checkTabsLabel.setText("Check tabs");
        checkTabs = new Button(composite, SWT.CHECK);
        checkTabs.setSelection(true);

        Label checkLineWidthLabel = new Label(composite, SWT.NONE);
        checkLineWidthLabel.setText("Check line width");
        checkLineWidth = new Button(composite, SWT.CHECK);
        checkLineWidth.setSelection(true);

        setControl(composite);
	}

    private ISelection getlastFlatExported() {
		try {
			return new StructuredSelection(nature.getLastFlatExport());
		} catch (Exception e) {
			return null;
		}
	}

	/**
     * Returns a content provider for the reference project
     * viewer. It will return all metamath files in the workspace.
     *
     * @return the content provider
     */
    protected IStructuredContentProvider getContentProvider() {
        return new WorkbenchContentProvider() {
            @Override
			public Object[] getChildren(Object element) {
                return super.getChildren(element);
            }
        };
    }

    /**
     * Returns the target MM file selected by the user.
     *
     * @return the target MM file
     */
    public IFile getTargetFile() {
        IStructuredSelection selection = resourceTreeViewer.getStructuredSelection();
        if(selection.getFirstElement() instanceof IFile) {
        	IFile targetFile = (IFile)selection.getFirstElement();
        	try {
				nature.setLastFlatExport(targetFile);
			} catch (CoreException e) {
				// do nothing
			}
        	return targetFile;
        } else {
	        System.out.println("No resource selected - "+selection.getFirstElement());
	        return null;
        }
    }

    /**
     * @return whether the export wizard shall check tabs
     */
    public boolean getCheckTabs() {
        return checkTabs.isEnabled();
    }

    /**
     * @return whether the export wizard shall line width
     */
    public boolean getCheckLineWidth() {
        return checkLineWidth.isEnabled();
    }
}

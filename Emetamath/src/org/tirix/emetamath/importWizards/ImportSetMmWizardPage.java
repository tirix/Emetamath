package org.tirix.emetamath.importWizards;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardResourceImportPage;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.tirix.emetamath.Activator;


public class ImportSetMmWizardPage extends WizardResourceImportPage {
	
	protected FileFieldEditor editor;

	public ImportSetMmWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(pageName); //NON-NLS-1
		setDescription("Import the set.mm file from the Metamath web site into the workspace"); //NON-NLS-1
	}

	@Override
	protected void createSourceGroup(Composite parent) {
		// TODO - propose the different metamath mirrors as options
	}

	@Override
	protected ITreeContentProvider getFileProvider() {
		return null;
	}

	@Override
	protected ITreeContentProvider getFolderProvider() {
		return null;
	}

	protected InputStream getWebFileInputStream() throws IOException {
//		URI fileUri = new URI("http://us2.metamath.org:88/metamath/set.mm.bz2");
//	    URL fileUrl = fileUri.toURL();
		URL fileUrl = new URL("http://us2.metamath.org:88/metamath/set.mm.bz2");
	    return fileUrl.openStream();
	}
	
    /**
     *	Execute the passed import operation.  Answer a boolean indicating success.
     */
    protected boolean executeImportOperation(ImportFromInputStreamOperation op) {
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            displayErrorDialog(e.getTargetException());
            return false;
        }

        IStatus status = op.getStatus();
        if (!status.isOK()) {
            ErrorDialog
                    .openError(getContainer().getShell(), DataTransferMessages.FileImport_importProblems,
                            null, // no special message
                            status);
            return false;
        }

        return true;
    }

    /**
     *	The Finish button was pressed.  Try to do the required work now and answer
     *	a boolean indicating success.  If false is returned then the wizard will
     *	not close.
     *
     * @return boolean
     */
    public boolean finish() {
        saveWidgetValues();

		try {
			InputStream webFileInputStream = getWebFileInputStream();
	        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(webFileInputStream); 
			ImportFromInputStreamOperation operation = new ImportFromInputStreamOperation(getSpecifiedContainer(), "set.mm", bzIn);
	        return executeImportOperation(operation);
		} catch (IOException e) {
            ErrorDialog
            .openError(getContainer().getShell(), DataTransferMessages.FileImport_importProblems,
                    null, // no special message
                    new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not open set.mm from the web", e));
			return false;
		}
    }
    
    public static class ImportFromInputStreamOperation extends WorkspaceModifyOperation {
    	IStatus status;
    	IContainer targetContainer;
    	String fileName;
    	InputStream in;
    	
		public ImportFromInputStreamOperation(IContainer targetContainer,
				String fileName, InputStream in) {
			this.in = in;
			this.fileName = fileName;
			this.targetContainer = targetContainer;
			status = Status.OK_STATUS;
		}

		@Override
		protected void execute(IProgressMonitor monitor) throws CoreException,
				InvocationTargetException, InterruptedException {
			monitor.beginTask("Fetching set.mm", 12);
			
			monitor.subTask("Creating file");
	        IFile targetResource = targetContainer.getFile(new Path(fileName));
	        monitor.worked(1);
			
	        try {
	            if (targetResource.exists()) {
					targetResource.setContents(in, IResource.KEEP_HISTORY, monitor);
				} else {
					targetResource.create(in, false, monitor);
				}
    	        monitor.worked(10);
	        } catch (CoreException e) {
	            status = e.getStatus();
	        } finally {
	            try {
	                in.close();
	    	        monitor.done();
	            } catch (IOException e) {
	            }
	        }
		}

		public IStatus getStatus() {
			return status;
		}
    }
}

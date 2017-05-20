package org.tirix.emetamath.editors.proofassistant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;

import mmj.pa.ProofWorksheet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.tirix.eclipse.ReaderInputStream;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;

/**
 * A DocumentProvider which provides ProofDocuments from ProofWorksheet objects.
 * @author Thierry
 *
 */
public class ProofWorksheetDocumentProvider extends StorageDocumentProvider {

	@Override
	protected IDocument createEmptyDocument() {
		return new ProofDocument();
	}

	/**
	 * Sets up the given document as it would be provided for the given element. 
	 * 
	 * @param element the blue-print element
	 * @param document the document to set up
	 */
	protected void setupDocument(Object element, IDocument document) {
		if(element instanceof ProofWorksheetInput) {
			ProofWorksheetStorage storage = ((ProofWorksheetInput)element).storage;
			((ProofDocument)document).setup(storage.proofWorksheet, storage.nature);
		}
		// do nothing
	}

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new MMPPartitionScanner(),
					new String[] {
						MMPPartitionScanner.MMP_HEADER,
						MMPPartitionScanner.MMP_COMMENT,
						MMPPartitionScanner.MMP_PROOF,
						MMPPartitionScanner.MMP_LABEL_LIST,
						IDocument.DEFAULT_CONTENT_TYPE
						});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}

	@Override
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
		ProofWorksheetStorage storage = ((ProofWorksheetInput)element).storage;
		IProject project = storage.nature.getProject();
		final IFile newFile = project.getFile(new Path(storage.getName()));
		
		monitor.beginTask("Saving " + storage.getName(), 4);

		// TODO check if already exists ?
		if(newFile.exists()) {}
		monitor.worked(1);
		
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		MessageBox messageBox = new MessageBox(workbench.getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
		messageBox.setText ("Saving Metamath New Proof");
		messageBox.setMessage ("Create a new proof file named \""+storage.getName()+"\" in the \""+project.getName()+"\" project?");
		if(messageBox.open() != SWT.OK) {
			monitor.setCanceled(true);
			throw new CoreException(Status.CANCEL_STATUS);
		}
		monitor.worked(1);
		
		try {
			//InputStream stream = storage.getContents(); // this gives the initial content of the document, not the current status
			InputStream stream = new ReaderInputStream(new StringReader(document.get()), "ASCII");
			if (newFile.exists()) {
				newFile.setContents(stream, true, true, monitor);
			} else {
				newFile.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		monitor.worked(1);
		
		// From now on, the DocumentProvider shall be a FileDocumentProvider...
		// Replace the editor input and place the caret at the same position
		ProofAssistantEditor editor = (ProofAssistantEditor) workbench.getActiveWorkbenchWindow().getActivePage().findEditor((IEditorInput) element);
		TextSelection selection = (TextSelection)editor.getSelectionProvider().getSelection();
		editor.setInput(new FileEditorInput(newFile));
		editor.selectAndReveal(selection.getOffset(), selection.getLength());
		monitor.done();
	}


	/**
	 * We provide a dummy annotation model...
	 */
	@Override
	protected IAnnotationModel createAnnotationModel(Object element) {
		return new AbstractMarkerAnnotationModel() {
			@Override
			protected void deleteMarkers(IMarker[] markers) throws CoreException { }

			@Override
			protected boolean isAcceptable(IMarker marker) { return false; }

			@Override
			protected void listenToMarkerChanges(boolean listen) { }

			@Override
			protected IMarker[] retrieveMarkers() throws CoreException { return null; }
			};
	}
	
	public static class ProofWorksheetInput implements IStorageEditorInput {
	      private ProofWorksheetStorage storage;
	      public ProofWorksheetInput(ProofWorksheetStorage storage) {this.storage = storage;}
	      public boolean exists() {return true;}
	      public ImageDescriptor getImageDescriptor() {return null;}
	      public String getName() {
	         return storage.getName();
	      }
	      public IPersistableElement getPersistable() {return null;}
	      public IStorage getStorage() {
	         return storage;
	      }
	      public String getToolTipText() {
	         return "Metamath proof for " + storage.getName();
	      }
	      public Object getAdapter(Class adapter) {
	        return null;
	      }
		public MetamathProjectNature getNature() {
			return storage.nature;
		}
	   }

	public static class ProofWorksheetStorage implements IStorage {
		private ProofWorksheet proofWorksheet;
		MetamathProjectNature nature;
	
		public ProofWorksheetStorage(ProofWorksheet input, MetamathProjectNature nature) {
			this.proofWorksheet = input;
			this.nature = nature;
		}
	
		public InputStream getContents() throws CoreException {
			String string = proofWorksheet.getOutputProofText();
			return new ByteArrayInputStream(string.getBytes());
		}
	
		public IPath getFullPath() {
			return null;
		}
	
		public String getName() {
			return proofWorksheet.getTheoremLabel()+".mmp";
		}
	
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}
	
		public boolean isReadOnly() {
			return false;
		}
	}
}
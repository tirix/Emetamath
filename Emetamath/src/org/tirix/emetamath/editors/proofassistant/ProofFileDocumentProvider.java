package org.tirix.emetamath.editors.proofassistant;



import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofFileDocumentProvider extends FileDocumentProvider {

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
	@Override
	protected void setupDocument(Object element, IDocument document) {
		try {
			if(element instanceof FileEditorInput) {
				FileEditorInput input = (FileEditorInput)element;
				String theoremName = input.getName();
				MetamathProjectNature nature;
					nature = (MetamathProjectNature)input.getFile().getProject().getNature(MetamathProjectNature.NATURE_ID);
				((ProofDocument)document).setup(theoremName, nature);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			
			IResource resource = ((IFileEditorInput)element).getFile();
			IMarker marker = resource.createMarker("org.tirix.emetamath.unusedStep.down");
			marker.setAttribute(IMarker.LINE_NUMBER, 1);
		}
		return document;
	}
	
//	@Override
//	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
//		FileInfo info= (FileInfo) getElementInfo(element);
//		if(info != null && element instanceof IFileEditorInput) {
//			ProofDocument document = (ProofDocument)info.fDocument;
//			IResource resource = ((IFileEditorInput)element).getFile();
//			return new ProofAnnotationModel(resource, document);
//		}
//		return super.createAnnotationModel(element);
//	}
}
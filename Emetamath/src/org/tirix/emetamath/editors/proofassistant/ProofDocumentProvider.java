package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofDocumentProvider extends FileDocumentProvider {

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
						MMPPartitionScanner.MM_COMMENT,
						MMPPartitionScanner.MM_LABEL_LIST,
						IDocument.DEFAULT_CONTENT_TYPE
						});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
}
package org.tirix.emetamath.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class MMDocumentProvider extends FileDocumentProvider {

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new MMPartitionScanner(),
					new String[] {
						MMPartitionScanner.MM_FILEINCLUSION,
						MMPartitionScanner.MM_COMMENT,
						MMPartitionScanner.MM_PROOF,
						MMPartitionScanner.MM_TYPESETTING,
						IDocument.DEFAULT_CONTENT_TYPE
						});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
}
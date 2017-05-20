package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

public class ProofAnnotationModel extends ResourceMarkerAnnotationModel {

	public ProofAnnotationModel(IResource resource, ProofDocument document) {
		super(resource);
	}

	public static class ProofMarkerAnnotation extends MarkerAnnotation implements IAnnotationPresentation {

		public ProofMarkerAnnotation(IMarker marker) {
			super(marker);
			// TODO Auto-generated constructor stub
		}
	}
	
// TODO Whenever one step is selected, highlight all occurrences of this step (used, declared), so that dependencies are more obvious
	
//	public static class ProofAnnotationModelFactory implements IAnnotationModelFactory {
//		@Override
//		public IAnnotationModel createAnnotationModel(IPath path) {
//			if(!path.getFileExtension().equals("mmp")) return null;
//			IFile file= FileBuffers.getWorkspaceFileAtLocation(path);
//			if (file == null) return null;
//			MetamathProjectNature nature = (MetamathProjectNature)file.getProject().getNature(MetamathProjectNature.NATURE_ID);
//			IDocumentProvider documentProvider = Workbench.getInstance().getAdapter(IDocumentProvider.class);
//			documentProvider.
//			
//			return new ProofAnnotationModel(file, path);
//		}
//	}
}

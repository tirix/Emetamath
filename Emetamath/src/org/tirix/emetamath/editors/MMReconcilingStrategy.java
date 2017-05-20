package org.tirix.emetamath.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Reconciling strategy for Java parts in JSP files.
 *
 * @since 3.0
 */
 public class MMReconcilingStrategy implements  IReconcilingStrategy, IReconcilingStrategyExtension {

	 private ITextEditor fTextEditor;
	 private IProgressMonitor fProgressMonitor;

	 public MMReconcilingStrategy(ISourceViewer sourceViewer,
			 ITextEditor textEditor) {
		 fTextEditor = textEditor;
		 //getFile()
	 }

	 /*
	  * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	  */
	 public void setDocument(IDocument document) {
		 //TODO setInputModel(new DocumentAdapter(document));
	 }

	 /*
	  * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	  */
	 public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		 removeTemporaryAnnotations();
		 //process(fFirstStep.reconcile(dirtyRegion, subRegion));
	 }

	 /*
	  * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	  */
	 public void reconcile(IRegion partition) {
		 removeTemporaryAnnotations();
		 //process(fFirstStep.reconcile(partition));
	 }

	 /*
	  * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	  */
	 public void setProgressMonitor(IProgressMonitor monitor) {
		 //fFirstStep.setProgressMonitor(monitor);
		 fProgressMonitor = monitor;

	 }

	 /*
	  * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	  */
	 public void initialReconcile() {
		 //fFirstStep.reconcile(null);
		 reconcile(null);
	 }

	 private void process(final IReconcileResult[] results) {

		 if (results == null)
			 return;

		 IRunnableWithProgress runnable = new WorkspaceModifyOperation(
				 null) {
			 /*
			  * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
			  */
			 protected void execute(IProgressMonitor monitor)
			 throws CoreException, InvocationTargetException,
			 InterruptedException {
				 for (int i = 0; i < results.length; i++) {

					 if (fProgressMonitor != null
							 && fProgressMonitor.isCanceled())
						 return;

/* TODO Include when Annotation will be used

					 if (!(results[i] instanceof  AnnotationAdapter))
						 continue;

					 AnnotationAdapter result = (AnnotationAdapter) results[i];
					 Position pos = result.getPosition();

					 Annotation annotation = result.createAnnotation();
					 getAnnotationModel().addAnnotation(annotation, pos);
*/				 }
			 }
		 };
		 try {
			 runnable.run(null);
		 } catch (InvocationTargetException e) {
			 e.printStackTrace();
		 } catch (InterruptedException e) {
			 e.printStackTrace();
		 }
	 }

	 private IAnnotationModel getAnnotationModel() {
		 return fTextEditor.getDocumentProvider().getAnnotationModel(
				 fTextEditor.getEditorInput());
	 }

	 /*
	  * XXX: A "real" implementation must be smarter
	  * 		i.e. don't remove and add the annotations
	  * 		which are the same.
	  */
	 private void removeTemporaryAnnotations() {
		 Iterator iter = getAnnotationModel().getAnnotationIterator();
		 while (iter.hasNext()) {
			 Object annotation = iter.next();
			 if (annotation instanceof  Annotation) {
				 Annotation extension = (Annotation) annotation;
				 if (!extension.isPersistent())
					 getAnnotationModel().removeAnnotation(
							 (Annotation) annotation);
			 }
		 }
	 }

	 private IFile getFile() {
		 IEditorInput input = fTextEditor.getEditorInput();
		 if (!(input instanceof  IFileEditorInput))
			 return null;

		 return ((IFileEditorInput) input).getFile();
	 }
 }
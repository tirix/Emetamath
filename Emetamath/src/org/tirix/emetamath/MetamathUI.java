package org.tirix.emetamath;

import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;
import mmj.pa.ProofWorksheet;
import mmj.pa.StepSelectorStore;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;
import org.tirix.emetamath.editors.proofassistant.ProofWorksheetDocumentProvider.ProofWorksheetInput;
import org.tirix.emetamath.editors.proofassistant.ProofWorksheetDocumentProvider.ProofWorksheetStorage;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.StepSelectorView;

public class MetamathUI {
	public static void openInEditor(SourceElement elt) {
		openInEditor(elt, true);
	}

	public static void openInEditor(SourceElement elt, boolean activate) {
		if(elt == null) return;
		selectAndReveal(elt.getPosition(), activate);
	}

	public static void openInEditor(ProofWorksheet w, MetamathProjectNature nature) {
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		try{
			IStorageEditorInput input = new ProofWorksheetInput(new ProofWorksheetStorage(w, nature));
			page.openEditor(input, ProofAssistantEditor.EDITOR_ID, true);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public static void selectAndReveal(SourcePosition position, boolean activate) {
		if(position == null) return;
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		IEditorPart part;
		try {
			part = page.openEditor(new FileEditorInput((IFile)position.sourceId), MetamathEditor.EDITOR_ID);
			((MetamathEditor)part).selectAndReveal(position.charStartNbr, position.charEndNbr - position.charStartNbr);
			if (part !=null) // part.getEditorSite().getPage() -> page 
				if(activate) page.activate(part);
				else page.bringToTop(part);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public static void showInStepSelector(MetamathProjectNature nature, StepSelectorStore stepSelectorStore, ProofAssistantEditor editor) {
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		StepSelectorView stepSelectorView = (StepSelectorView) page.findView(StepSelectorView.VIEW_ID);
		stepSelectorView.setData(nature, stepSelectorStore, editor);
		try {
			page.showView(StepSelectorView.VIEW_ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public static void showProblemsView() {
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		try {
			page.showView(IPageLayout.ID_PROBLEM_VIEW);
		} catch (PartInitException e) {
		}
	}
}

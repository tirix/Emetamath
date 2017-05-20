package org.tirix.emetamath;

import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

public class MetamathUI {
	public static void openInEditor(SourceElement elt) {
		openInEditor(elt, true);
	}

	public static void openInEditor(SourceElement elt, boolean activate) {
		IWorkbench workbench= Activator.getDefault().getWorkbench();
		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		SourcePosition position = elt.getPosition();
		IEditorPart part= page.findEditor(new FileEditorInput((IFile)position.sourceId));
		if(part instanceof MetamathEditor)
		((MetamathEditor)part).selectAndReveal(position.charStartNbr, position.charEndNbr - position.charStartNbr);
		if (part !=null && activate)
			part.getEditorSite().getPage().activate(part);
	}
}

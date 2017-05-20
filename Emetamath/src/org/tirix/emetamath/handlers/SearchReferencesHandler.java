package org.tirix.emetamath.handlers;


import mmj.lang.LogicalSystem;
import mmj.lang.MObj;
import mmj.mmio.SourceElement;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.IViewDescriptor;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.search.MetamathSearchQuery;
import org.tirix.emetamath.search.MetamathSearchResultViewPage;

public class SearchReferencesHandler extends MetamathEditorActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MObj obj = getSelectedMObj(context);
		MetamathProjectNature nature = getNature(context);
		
		NewSearchUI.runQueryInBackground(new MetamathSearchQuery(nature, obj));
		return null; // must return null...
	}
}

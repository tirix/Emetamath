/**
 * 
 */
package org.tirix.emetamath.search;

import java.util.ArrayList;

import mmj.lang.MObj;
import mmj.mmio.Source;
import mmj.mmio.SourcePosition;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.tirix.emetamath.nature.MetamathProjectNature.ResourceSource;

public class MetamathSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {
	MetamathSearchQuery fQuery;
	
	public MetamathSearchResult(MetamathSearchQuery query) {
		fQuery = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		return fQuery.getLabel() + " - "+getMatchCount()+" references in project '"+fQuery.getNature().getProject().getName()+"'";
	}

	@Override
	public ISearchQuery getQuery() {
		return fQuery;
	}

	@Override
	public String getTooltip() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IEditorPart editor) {
		return computeContainedMatches(result, (IFile)editor.getEditorInput().getAdapter(IFile.class));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult, org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		ArrayList<Match> containedMatches = new ArrayList<Match>();
		for(Object element:result.getElements()) {
			if(getFile(element).equals(file))
				for(Match match:result.getMatches(element))
					containedMatches.add(match);
		}
		return containedMatches.toArray(new Match[containedMatches.size()]);
	}
	
	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		Object element= match.getElement();
		if (element instanceof MObj) {
			element= getFile(element); // source file 
		} 
		if (element instanceof IFile) {
			return element != null && element.equals(editor.getEditorInput().getAdapter(IFile.class));
		}
		return false;
	}

	@Override
	public IFile getFile(Object element) {
		if (element instanceof MObj) {
			Source source = ((MObj)element).getPosition().source;
			if(source instanceof ResourceSource) {
				IResource resource = ((ResourceSource)source).resource;
				if(resource instanceof IFile) return (IFile)resource;
			}
		}
		return null;
	}
}
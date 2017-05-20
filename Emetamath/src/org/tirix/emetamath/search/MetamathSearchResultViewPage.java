/**
 * 
 */
package org.tirix.emetamath.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mmj.lang.MObj;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInTargetList;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.MMContentProvider;
import org.tirix.emetamath.views.MMLabelProvider;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.ProofExplorerView;

public class MetamathSearchResultViewPage extends AbstractTextSearchViewPage implements IAdaptable {
	private MetamathSearchContentProvider fContentProvider;
	private static final int DEFAULT_ELEMENT_LIMIT = 1000;

	public MetamathSearchResultViewPage() {
		//initSortActions();
		//initGroupingActions();
		setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
	}
		
	@Override
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setUseHashlookup(true);
		viewer.setLabelProvider(new MMLabelProvider());
		fContentProvider=new MetamathSearchContentProvider(this);
		viewer.setContentProvider(fContentProvider);
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		viewer.setUseHashlookup(true);
		viewer.setLabelProvider(new MMLabelProvider());
		fContentProvider= new MetamathSearchContentProvider(this);
		viewer.setContentProvider(fContentProvider);
	}

	protected StructuredViewer getViewer() {
		// override so that it's visible in the package.
		return super.getViewer();
	}
	
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		MetamathUI.openInEditor((MObj)match.getElement(), activate);
	}

	@Override
	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	@Override
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null)
			fContentProvider.elementsChanged(objects);
	}

	private static final String[] SHOW_IN_TARGETS= new String[] { 
		ProofExplorerView.VIEW_ID, 
		ProofBrowserView.VIEW_ID,
		//IPageLayout.ID_OUTLINE 
		};
	public static final IShowInTargetList SHOW_IN_TARGET_LIST= new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};
	
	@Override
	public Object getAdapter(Class adapter) {
		if (IShowInTargetList.class.equals(adapter)) {
			return SHOW_IN_TARGET_LIST;
		}
		return null;
	}

	public static class MetamathSearchContentProvider implements ITreeContentProvider {
		protected final Object[] EMPTY_ARR= new Object[0];
		private MMContentProvider fContentProvider;
		private AbstractTextSearchResult fResult;
		private MetamathSearchResultViewPage fPage;
		private Map fChildrenMap;
	
		MetamathSearchContentProvider(MetamathSearchResultViewPage page) {
			fPage= page;
			fContentProvider = new MMContentProvider();
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if(newInput != null) {
				MetamathProjectNature nature = ((MetamathSearchQuery)((MetamathSearchResult)newInput).getQuery()).getNature();
				if(viewer instanceof ContentViewer && ((ContentViewer)viewer).getLabelProvider() instanceof MMLabelProvider) ((MMLabelProvider)((ContentViewer)viewer).getLabelProvider()).setNature(nature); 
				fContentProvider.setNature(nature);
			}
			initialize((AbstractTextSearchResult) newInput);
		}
		
		public Object getParent(Object child) {
			return fContentProvider.getParent(child);
		}

		public Object[] getChildren(Object parentElement) {
			Set children= (Set) fChildrenMap.get(parentElement);
			if (children == null)
				return EMPTY_ARR;
			int limit= getPage().getElementLimit().intValue();
			if (limit != -1 && limit < children.size()) {
				Object[] limitedArray= new Object[limit];
				Iterator iterator= children.iterator();
				for (int i= 0; i < limit; i++) {
					limitedArray[i]= iterator.next();
				}
				return limitedArray;
			}
			
			return children.toArray();
		}

		public boolean hasChildren(Object element) {
			Set children= (Set) fChildrenMap.get(element);
			return children != null && !children.isEmpty();
		}

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
	
		protected void initialize(AbstractTextSearchResult result) {
			fResult= result;
			fChildrenMap = new HashMap();
			if (result != null) {
				Object[] elements= result.getElements();
				for (int i= 0; i < elements.length; i++) {
					if (getPage().getDisplayedMatchCount(elements[i]) > 0) {
						insert(null, null, elements[i]);
					}
				}
			}
		}
		
		protected void insert(Map toAdd, Set toUpdate, Object child) {
			Object parent= getParent(child);
			while (parent != null) {
				if (insertChild(parent, child)) {
					if (toAdd != null)
						insertInto(parent, child, toAdd);
				} else {
					if (toUpdate != null)
						toUpdate.add(parent);
					return;
				}
				child= parent;
				parent= getParent(child);
			}
			if (insertChild(getSearchResult(), child)) {
				if (toAdd != null)
					insertInto(getSearchResult(), child, toAdd);
			}
		}

		private boolean insertChild(Object parent, Object child) {
			return insertInto(parent, child, fChildrenMap);
		}

		private boolean insertInto(Object parent, Object child, Map map) {
			Set children= (Set) map.get(parent);
			if (children == null) {
				children= new TreeSet();
				map.put(parent, children);
			}
			return children.add(child);
		}

		protected void remove(Set toRemove, Set toUpdate, Object element) {
			// precondition here:  fResult.getMatchCount(child) <= 0
		
			if (hasChildren(element)) {
				if (toUpdate != null)
					toUpdate.add(element);
			} else {
				if (getPage().getDisplayedMatchCount(element) == 0) {
					fChildrenMap.remove(element);
					Object parent= getParent(element);
					if (parent != null) {
						if (removeFromSiblings(element, parent)) {
							remove(toRemove, toUpdate, parent);
						}
					} else {
						if (removeFromSiblings(element, getSearchResult())) {
							if (toRemove != null)
								toRemove.add(element);
						}
					}
				} else {
					if (toUpdate != null) {
						toUpdate.add(element);
					}
				}
			}
		}

		/**
		 * @param element
		 * @param parent
		 * @return returns true if it really was a remove (i.e. element was a child of parent).
		 */
		private boolean removeFromSiblings(Object element, Object parent) {
			Set siblings= (Set) fChildrenMap.get(parent);
			if (siblings != null) {
				return siblings.remove(element);
			} else {
				return false;
			}
		}

		public void elementsChanged(Object[] updatedElements) {
			if (getSearchResult() == null)
				return;
			
			AbstractTreeViewer viewer= (AbstractTreeViewer) getPage().getViewer();

			Set toRemove= new HashSet();
			Set toUpdate= new HashSet();
			Map toAdd= new HashMap();
			for (int i= 0; i < updatedElements.length; i++) {
				if (getPage().getDisplayedMatchCount(updatedElements[i]) > 0)
					insert(toAdd, toUpdate, updatedElements[i]);
				else
					remove(toRemove, toUpdate, updatedElements[i]);
			}
			
			viewer.remove(toRemove.toArray());
			for (Iterator iter= toAdd.keySet().iterator(); iter.hasNext();) {
				Object parent= iter.next();
				Set children= (Set) toAdd.get(parent);
				viewer.add(parent, children.toArray());
			}
			for (Iterator elementsToUpdate= toUpdate.iterator(); elementsToUpdate.hasNext();) {
				viewer.refresh(elementsToUpdate.next());
			}
			
		}
	
		public void dispose() {
			// nothing to do
		}
	
		MetamathSearchResultViewPage getPage() {
			return fPage;
		}
		
		AbstractTextSearchResult getSearchResult() {
			return fResult;
		}
	
		public void clear() {
			initialize(getSearchResult());
			getPage().getViewer().refresh();
		}
	}
}
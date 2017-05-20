package org.tirix.emetamath.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IContributedContentsView;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class ProofExplorerView extends PageBookView implements ISelectionProvider, ISelectionChangedListener {
	public static final String VIEW_ID = "org.tirix.emetamath.views.ProofExplorerView";
    private Map<MetamathProjectNature, IPageBookViewPage> natureToPage;
	
	public ProofExplorerView() {
		natureToPage  = new HashMap<MetamathProjectNature, IPageBookViewPage>();
	}

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        getSelectionProvider().addSelectionChangedListener(listener);
    }

    /* (non-Javadoc)
     * Method declared on PageBookView.
     */
    protected IPage createDefaultPage(PageBook book) {
        MessagePage page = new MessagePage();
        initPage(page);
        page.createControl(book);
        page.setMessage("No Metamath editor has been selected.");
        return page;
    }

    /**
     * The <code>PageBookView</code> implementation of this <code>IWorkbenchPart</code>
     * method creates a <code>PageBook</code> control with its default page showing.
     */
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }

    /* (non-Javadoc)
     * Get the nature of the given part, and create the ProofExplorerViewPage.
     * Method declared on PageBookView.
     */
    protected PageRec doCreatePage(IWorkbenchPart part) {
        if(!(part instanceof IEditorPart)) return null;
    	MetamathProjectNature nature = MetamathProjectNature.getNature(((IEditorPart)part).getEditorInput());
    	if(nature == null) return null;
    	IPageBookViewPage page = natureToPage.get(nature);
    	if(page == null) {
    		page = new ProofExplorerViewPage(nature);
			initPage((IPageBookViewPage) page);
            page.createControl(getPageBook());
            natureToPage.put(nature, page);
    	}
        return new PageRec(part, page);
    }

    /* (non-Javadoc)
     * Method declared on PageBookView.
     */
    protected void doDestroyPage(IWorkbenchPart part, PageRec rec) {
        IPageBookViewPage page = (IPageBookViewPage) rec.page;
        page.dispose();
        rec.dispose();
    }

    /* (non-Javadoc)
     * Method declared on IAdaptable.
     */
    public Object getAdapter(Class key) {
        if (key == IContributedContentsView.class) {
			return new IContributedContentsView() {
                public IWorkbenchPart getContributingPart() {
                    return getContributingEditor();
                }
            };
		}
        return super.getAdapter(key);
    }

    /* (non-Javadoc)
     * Method declared on PageBookView.
     */
    // TODO get a Metamath Editor Part? 
    protected IWorkbenchPart getBootstrapPart() {
        IWorkbenchPage page = getSite().getPage();
        if (page != null) {
			return page.getActiveEditor();
		}

        return null;
    }

    /**
     * Returns the editor which contributed the current 
     * page to this view.
     *
     * @return the editor which contributed the current page
     * or <code>null</code> if no editor contributed the current page
     */
    private IWorkbenchPart getContributingEditor() {
        return getCurrentContributingPart();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public ISelection getSelection() {
        // get the selection from the selection provider
        return getSelectionProvider().getSelection();
    }

    /* (non-Javadoc)
     * Method declared on PageBookView.
     * We only want to track editors.
     */
    protected boolean isImportant(IWorkbenchPart part) {
        //We only care about editors
        return (part instanceof IEditorPart);
    }

    /* (non-Javadoc)
     * Method declared on IViewPart.
     * Treat this the same as part activation.
     */
    public void partBroughtToTop(IWorkbenchPart part) {
        partActivated(part);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void removeSelectionChangedListener(
            ISelectionChangedListener listener) {
        getSelectionProvider().removeSelectionChangedListener(listener);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionChangedListener.
     */
    public void selectionChanged(SelectionChangedEvent event) {
        getSelectionProvider().selectionChanged(event);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void setSelection(ISelection selection) {
        getSelectionProvider().setSelection(selection);
    }

    /**
     * The <code>ProofExplorer</code> implementation of this <code>PageBookView</code> method
     * extends the behavior of its parent to use the current page as a selection provider.
     * 
     * @param pageRec the page record containing the page to show
     */
    protected void showPageRec(PageRec pageRec) {
        IPageSite pageSite = getPageSite(pageRec.page);
        ISelectionProvider provider = pageSite.getSelectionProvider();
        if (provider == null && (pageRec.page instanceof ProofExplorerViewPage)) {
			// This means that the page did not set a provider during its initialization 
            // so for backward compatibility we will set the page itself as the provider.
            pageSite.setSelectionProvider((ProofExplorerViewPage) pageRec.page);
		}
        super.showPageRec(pageRec);
    }
}

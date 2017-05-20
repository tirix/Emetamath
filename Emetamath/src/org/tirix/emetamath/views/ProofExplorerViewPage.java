package org.tirix.emetamath.views;

import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

public class ProofExplorerViewPage extends Page implements ISelectionProvider, ISelectionChangedListener, IShowInSource, IShowInTarget, SystemLoadListener, IAdaptable, IDoubleClickListener {
    private ListenerList selectionChangedListeners = new ListenerList();
    private MetamathProjectNature fNature;
    private TreeViewer treeViewer;
    private boolean linkWithEditor;
    
	public ProofExplorerViewPage(MetamathProjectNature nature) {
		this.fNature = nature;
		this.linkWithEditor = false; // TODO add setter method and button...
	}

	@Override
    public void createControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        treeViewer.setContentProvider(new MMContentProvider());
        treeViewer.setLabelProvider(fNature.getLabelProvider());
        treeViewer.addSelectionChangedListener(this);
        treeViewer.addDoubleClickListener(this);
		treeViewer.setInput(fNature);
		((MMLabelProvider)treeViewer.getLabelProvider()).setNature(fNature);
		fNature.addSystemLoadListener(this);
		createContextMenu();
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				// The menu only contains sections, that we can extend
				manager.add(new Separator("open"));
				MenuManager showInSubMenu = new MenuManager("Show In");
				showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(ProofExplorerViewPage.this.getSite().getWorkbenchWindow()));
				manager.add(showInSubMenu);
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(ProofExplorerView.VIEW_ID, menuMgr, this);
	}

	@Override
    public Object getAdapter(Class key) {
        if (key == MetamathProjectNature.class) {
			return fNature;
		}
        if (key == IShowInSource.class) {
        	return this;
        }
        if (key == IShowInTargetList.class) {
        	return SHOW_IN_TARGET_LIST;
        }
        return null;
    }


	@Override
	public void systemLoaded() {
		Display.getDefault().asyncExec(new Runnable() {
               public void run() {
            	   if(!treeViewer.getControl().isDisposed())
            		   treeViewer.refresh();
               }});
//		Display.getDefault().asyncExec(new Runnable() {
//               public void run() {
//            	   if(editor.getSourceViewer() != null) 
//            		   editor.getSourceViewer().invalidateTextPresentation();
//               }});
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection= context.getSelection();
		if (selection instanceof IStructuredSelection) {
			treeViewer.setSelection((IStructuredSelection)selection, true);
			return true;
		}

//		Object   input= context.getInput();
//		if (input instanceof IEditorInput) {
//			Object   elementOfInput= getInputFromEditor((IEditorInput) input);
//			return elementOfInput != null && (tryToReveal(elementOfInput) == IStatus.OK);
//		}
		return false;
	}

	/* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    /**
     * Fires a selection changed event.
     *
     * @param selection the new selection
     */
    protected void fireSelectionChanged(ISelection selection) {
        // create an event
        final SelectionChangedEvent event = new SelectionChangedEvent(this,
                selection);

        // fire the event
        Object[] listeners = selectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }

    /* (non-Javadoc)
     * Method declared on IPage (and Page).
     */
    public Control getControl() {
        if (treeViewer == null) {
			return null;
		}
        return treeViewer.getControl();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public ISelection getSelection() {
        if (treeViewer == null) {
			return StructuredSelection.EMPTY;
		}
        return treeViewer.getSelection();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void removeSelectionChangedListener(
            ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionChangeListener.
     * Gives notification that the tree selection has changed.
     */
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
		if(!linkWithEditor) return;
    	if(event.getSelection() instanceof ITreeSelection) {
    		if(((ITreeSelection)event.getSelection()).getFirstElement() instanceof SourceElement) {
				SourcePosition position = ((SourceElement)(((ITreeSelection)event.getSelection()).getFirstElement())).getPosition();
				MetamathUI.selectAndReveal(position, false);
			}
		}
        fireSelectionChanged(event.getSelection());
    }

	@Override
	public void doubleClick(DoubleClickEvent event) {
    	if(event.getSelection() instanceof ITreeSelection) {
			if(((ITreeSelection)event.getSelection()).getFirstElement() instanceof SourceElement) {
				SourcePosition position = ((SourceElement)(((ITreeSelection)event.getSelection()).getFirstElement())).getPosition();
				MetamathUI.selectAndReveal(position, false);
			}
		}
	}

    /**
     * Sets focus to a part in the page.
     */
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void setSelection(ISelection selection) {
        if (treeViewer != null) {
			treeViewer.setSelection(selection);
		}
    }

	private static final String[] SHOW_IN_TARGETS= new String[] { 
		ProofBrowserView.VIEW_ID,
		WebBrowserView.WEB_BROWSER_VIEW_ID,
		//IPageLayout.ID_OUTLINE 
		};
	public static final IShowInTargetList SHOW_IN_TARGET_LIST= new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};
	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(fNature, treeViewer.getSelection());
	}
}

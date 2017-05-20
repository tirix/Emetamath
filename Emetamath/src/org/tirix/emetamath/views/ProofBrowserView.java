package org.tirix.emetamath.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mmj.lang.Formula;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.pa.DerivationStep;
import mmj.pa.ProofAsst;
import mmj.pa.ProofStep;
import mmj.pa.ProofWorksheet;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.internal.browser.ImageResource;
import org.eclipse.ui.internal.browser.Messages;
import org.eclipse.ui.internal.browser.ToolbarLayout;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.tirix.eclipse.StyledTableViewer;
import org.tirix.eclipse.MMLayout.TopCenterLayout;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.MMScanner;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.mmj.SyntaxBreakdownSheet;

public class ProofBrowserView extends ViewPart implements IShowInTarget {
	public static final String VIEW_ID = "org.tirix.emetamath.views.ProofBrowserView";
	protected static final int COL_STEP = 0, COL_HYP = 1, COL_REF = 2, COL_EXPR = 3;
	protected static final Color HOVER_BGCOLOR = new Color(Display.getDefault(), new RGB(232, 242, 254));
	//protected PageBook book;
	protected StyledTableViewer viewer;
	protected ProofBrowserTableLabelProvider labelProvider;
	protected MetamathProjectNature nature;
	protected NavigationHistory navigationHistory;
	
	protected Combo combo;
	protected ToolItem forward;
	protected ToolItem back;
	
	public ProofBrowserView() {
	}

	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new TopCenterLayout(SWT.DEFAULT, 3));
		
		// The Tool bar
		Composite toolbarComp = new Composite(composite, SWT.NONE);
		toolbarComp.setLayout(new ToolbarLayout());
		toolbarComp.setLayoutData(new GridData(
		      GridData.VERTICAL_ALIGN_BEGINNING
		      | GridData.FILL_HORIZONTAL));
		
		createToolbar(toolbarComp);
		createLocationBar(toolbarComp);
		createTable(composite);
	}
	

    private ToolBar createLocationBar(Composite parent) {
        combo = new Combo(parent, SWT.DROP_DOWN);

        updateHistory();

        combo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent we) {
                try {
                    if (combo.getSelectionIndex() != -1 && !combo.getListVisible()) {
                        setProof(combo.getItem(combo.getSelectionIndex()));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        });
        combo.addListener(SWT.DefaultSelection, new Listener() {
            public void handleEvent(Event e) {
            	setProof(combo.getText());
            }
        });
        
        ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

        ToolItem go = new ToolItem(toolbar, SWT.NONE);
        go.setImage(ImageResource.getImage(ImageResource.IMG_ELCL_NAV_GO));
        go.setHotImage(ImageResource.getImage(ImageResource.IMG_CLCL_NAV_GO));
        go.setDisabledImage(ImageResource
                .getImage(ImageResource.IMG_DLCL_NAV_GO));
        go.setToolTipText(Messages.actionWebBrowserGo);
        go.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
            	setProof(combo.getText());
            }
        });
		  
		  return toolbar;
    }

    private ToolBar createToolbar(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		  
        // create back and forward actions
        back = new ToolItem(toolbar, SWT.NONE);
        back.setImage(ImageResource
                .getImage(ImageResource.IMG_ELCL_NAV_BACKWARD));
        back.setHotImage(ImageResource
                .getImage(ImageResource.IMG_CLCL_NAV_BACKWARD));
        back.setDisabledImage(ImageResource
                .getImage(ImageResource.IMG_DLCL_NAV_BACKWARD));
        back.setToolTipText(Messages.actionWebBrowserBack);
        back.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                back();
            }
        });

        forward = new ToolItem(toolbar, SWT.NONE);
        forward.setImage(ImageResource
                .getImage(ImageResource.IMG_ELCL_NAV_FORWARD));
        forward.setHotImage(ImageResource
                .getImage(ImageResource.IMG_CLCL_NAV_FORWARD));
        forward.setDisabledImage(ImageResource
                .getImage(ImageResource.IMG_DLCL_NAV_FORWARD));
        forward.setToolTipText(Messages.actionWebBrowserForward);
        forward.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                forward();
            }
        });

        ToolItem refresh = new ToolItem(toolbar, SWT.NONE);
        refresh.setImage(ImageResource
                .getImage(ImageResource.IMG_ELCL_NAV_REFRESH));
        refresh.setHotImage(ImageResource
                .getImage(ImageResource.IMG_CLCL_NAV_REFRESH));
        refresh.setDisabledImage(ImageResource
                .getImage(ImageResource.IMG_DLCL_NAV_REFRESH));
        refresh.setToolTipText(Messages.actionWebBrowserRefresh);
        refresh.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                refresh();
            }
        });
		  
        return toolbar;
    }

	public void createTable(Composite parent) {
		final Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SCROLL_LINE | SWT.FULL_SELECTION );
		//viewer = new TableViewer(table);
		viewer = new StyledTableViewer(table, COL_EXPR);
		labelProvider = new ProofBrowserTableLabelProvider();

        TableLayout layout = new TableLayout();
        table.setLayout(layout);
		table.setHeaderVisible(true);
		String[] columnNames = { "Step", "Hypothesis", "Reference", "Expression" };
	    ColumnLayoutData[] columnLayouts = {
	        new ColumnWeightData(25),
	        new ColumnWeightData(50), 
	        new ColumnWeightData(50),
	        new ColumnWeightData(300)
	        };
	    final int columnCount = columnNames.length;

		for(int i=0;i<columnCount;i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setText(columnNames[i]);
			tc.setResizable(columnLayouts[i].resizable);
			layout.addColumnData(columnLayouts[i]);
		}
		viewer.setContentProvider(new ProofBrowserContentProvider());
		viewer.setLabelProvider(labelProvider);

		table.addListener(SWT.MouseMove, new Listener() {
			protected TableItem lastHoverItem = null;
			//protected Control lastHoverControl = null;
			protected int lastHoverColumn = 0;
			
			public void handleEvent(Event event) {
				if(table.isDisposed()) return;
				Point pt = new Point(event.x, event.y);
				TableItem item = table.getItem(pt);
				if (item == null)
					return;
				for(int i=0;i<columnCount;i++)
					if(item.getBounds(i).contains(pt)) {
						if(lastHoverItem != null && !lastHoverItem.isDisposed()) lastHoverItem.setBackground(lastHoverColumn, null);
						//if(lastHoverControl != null) lastHoverControl.setBackground(null);
						//lastHoverControl = viewer.getControl(item);
						item.setBackground(i, HOVER_BGCOLOR);
						//if(i == COL_EXPR) lastHoverControl.setBackground(HOVER_BGCOLOR);
						lastHoverItem = item;
						lastHoverColumn = i;
					}
				}
			});
		table.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				if(table.isDisposed()) return;
				Point pt = new Point(event.x, event.y);
				TableItem item = table.getItem(pt);
				if (item == null)
					return;
				ProofStep step = (ProofStep)item.getData();
				if(item.getBounds(COL_REF).contains(pt)) {
					show(nature, step.getRef(), true);
					};
				}
			});
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), VIEW_ID);
		createContextMenu();
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				// The menu only contains sections, that we can extend
				manager.add(new Separator("open"));
				MenuManager showInSubMenu = new MenuManager("Show In");
				showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(ProofBrowserView.this.getSite().getWorkbenchWindow()));
				manager.add(showInSubMenu);
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(ProofBrowserView.VIEW_ID, menuMgr, viewer);
	}

	@Override
	public void setFocus() {
		updateHistory();
	}

	public void setProof(String stmtName) {
		// first check in the history if the theorem is known
		// and take the chance to list all the natures used
		Set<MetamathProjectNature> natures = new HashSet<MetamathProjectNature>();
		for(NavigationElement e:navigationHistory) {
			if(e.stmt.getLabel().equals(stmtName)) {
				show(e.nature, e.stmt, true);
				return;
			}
			natures.add(e.nature);
		}
		for(MetamathProjectNature nature:natures) {
			Stmt stmt = nature.getStmt(stmtName);
			if(stmt != null) {
				show(nature, stmt, true);
				return;
			}
		}
		MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.OK | SWT.ICON_ERROR);
		messageBox.setText("Proof Explorer View");
		messageBox.setMessage("Statement \""+stmtName+"\" not found.");
		messageBox.open();
	}
	
	@Override
	public boolean show(ShowInContext context) {
		ISelection selection= context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			MetamathProjectNature nature = MetamathProjectNature.getNature(context.getInput()); //()Platform.getAdapterManager().getAdapter(context.getInput(), MetamathProjectNature.class);
			Object selectedObject = ssel.getFirstElement();
			if(selectedObject instanceof Stmt) {
				show(nature, (Stmt)selectedObject, true);
				return true;
			}
		}
		return false;
	}
	
	public void show(MetamathProjectNature nature, Stmt stmt, boolean addToHistory) {
		if(nature == null) return;
		labelProvider.setNature(nature);
		this.nature = nature;
		
		if(stmt instanceof Theorem) showProof((Theorem)stmt);
		else showSyntaxBreakdown((Stmt)stmt);

		viewer.setRule(MMScanner.createSymbolRule(nature, new ColorManager()));
		if(addToHistory) navigationHistory .add(new NavigationElement(nature, stmt));
        combo.setText(stmt.getLabel());
        updateHistory();
	}
	
	private void showProof(Theorem theorem) {
		ProofAsst proofAsst = nature.getProofAsst();
		if(proofAsst == null) return; // TODO throw an exception!
		ProofWorksheet w =
            proofAsst.getExistingProof(theorem,
                                   true,	//proofUnified
                                   false);	//hypsRandomized
		viewer.setInput(w.getProofWorkStmtList());
    }

	private void showSyntaxBreakdown(Stmt stmt) {
		SyntaxBreakdownSheet sheet = new SyntaxBreakdownSheet(stmt.getExprParseTree());
		viewer.setInput(sheet.getProofStepList());
	}
	
	public void back() {
		NavigationElement last = navigationHistory.back();
		if(last != null) show(last.nature, last.stmt, false);
	}

	protected void forward() {
		NavigationElement next = navigationHistory.forward();
		if(next != null) show(next.nature, next.stmt, false);
	}

	protected void refresh() {
		NavigationElement current = navigationHistory.getCurrent();
		if(current != null) show(current.nature, current.stmt, false);
	}

    /**
     * Update the history list to the global/shared copy.
     */
    protected void updateHistory() {
        if (combo == null)
            return;

        String temp = combo.getText();
        if (navigationHistory == null)
    		navigationHistory = new NavigationHistory();
//        	navigationHistory = ProofBrowserPreference.getInternalProofBrowserHistory();

        String[] historyList = new String[navigationHistory.size()];
        int i=0; for(NavigationElement e:navigationHistory) historyList[i++] = e.toString();
        combo.setItems(historyList);
        combo.setText(temp);
        
        // also update the "enabled" state of the back and forward buttons
        back.setEnabled(!navigationHistory.isFirst());
        forward.setEnabled(!navigationHistory.isLast());
    }

	public static class ProofBrowserContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object input) {
			ArrayList<ProofStep> list = new ArrayList<ProofStep>();
			for(Object element:(List<?>)input) if(element instanceof ProofStep) list.add((ProofStep)element);
			return list.toArray();
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
	}
	
	public static class ProofBrowserTableLabelProvider implements ITableLabelProvider, ITableFontProvider {
	    private Image stepItemImage = Activator.getImage("icons/mmStepItem.gif"); 
	    private Image stepLastImage = Activator.getImage("icons/mmStepDerivation.gif"); 
	    protected MetamathProjectNature nature;
	    
	    public void setNature(MetamathProjectNature nature) {
	    	this.nature = nature;
	    }

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			ProofStep step = (ProofStep)element;
			step.setRef((Stmt)nature.getMObj(step.getRefLabel()));
			switch(columnIndex) {
			case COL_STEP:
				if("qed".equals(step.getStepName())) return nature.getLabelProvider().getStepLastImage();
				if(step.isHypothesisStep()) return nature.getLabelProvider().getStepHypImage();
				if(step.isDerivationStep()) return nature.getLabelProvider().getStepItemImage();
				return null; // should not happen anyway
			case COL_REF:
				return nature.getLabelProvider().getImage(step.getRef());
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			ProofStep step = (ProofStep)element; 
			switch(columnIndex) {
			case COL_STEP: 
				return step.getStepName();
			
			case COL_HYP: 
				if(step.isDerivationStep()) {
					StringBuffer str = new StringBuffer();
					boolean first = true;
					for(String hypStep:step.getHypSteps()) { str.append((first?"":", ") + hypStep); first = false; }
					return str.toString();
				}
				return null;
			
			case COL_REF: 
				return step.getRefLabel();

			case COL_EXPR: 
				return step.getFormula().getTyp()+" "+step.getFormula().exprToString();

			default:
				return null;
			}
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			if(columnIndex == COL_EXPR) return nature.getLabelProvider().getEditorFont();
			return null; // use the default font
		}

		@Override
		public void dispose() {
	        stepItemImage.dispose(); 
	        stepItemImage = null; 
	        stepLastImage.dispose(); 
	        stepLastImage = null; 
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
			
		}
	}
	
	protected static class NavigationHistory implements Iterable<NavigationElement> {
		protected List<NavigationElement> history = new ArrayList<NavigationElement>();
		protected final static int CAPACITY = 100;
		protected int activeEntry = 0;

		/*
	     * Adds the specified entry to the history.
	     */
	    public void add(NavigationElement entry) {
	        removeForwardEntries();
	        if (history.size() == CAPACITY) {
	            NavigationElement e = history.remove(0);
	        }
	        history.add(entry);
	        activeEntry = history.size() - 1;
	    }

		/*
	     * Remove all entries after the active entry.
	     */
	    private void removeForwardEntries() {
	        int length = history.size();
	        for (int i = activeEntry + 1; i < length; i++) {
	        	NavigationElement e = history.remove(activeEntry + 1);
	        }
	    }

	    /*
	     * Shift the history backwards
	     */
	    public NavigationElement back() {
	        return getEntry(--activeEntry);
	    }

	    /*
	     * Shift the history forward
	     */
	    public NavigationElement forward() {
	        return getEntry(++activeEntry);
	    }

		public NavigationElement getCurrent() {
			return getEntry(activeEntry);
		}

	    /*
	     * Returns the history entry indexed by <code>index</code>
	     */
	    private NavigationElement getEntry(int index) {
	        if (0 <= index && index < history.size()) {
				return history.get(index);
			}
	        return null;
	    }

	    public boolean isFirst() {
	    	return activeEntry == 0;
	    }
	    
	    public boolean isLast() {
	    	return activeEntry >= history.size() - 1;
	    }
	    
	    public int size() {
			return history.size();
		}

		@Override
		public Iterator<NavigationElement> iterator() {
			return history.iterator();
		}
	}
	
	protected static class NavigationElement {
		MetamathProjectNature nature;
		Stmt stmt;

		public NavigationElement(MetamathProjectNature nature2, Stmt stmt2) {
			this.nature = nature2;
			this.stmt = stmt2;
		}
		
		public String toString() {
			return stmt.getLabel();
		}
	}
}

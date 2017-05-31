package org.tirix.emetamath.views;


import mmj.lang.Assrt;
import mmj.lang.Formula;
import mmj.lang.LogHyp;
import mmj.pa.PaConstants;
import mmj.pa.StepSelectorItem;
import mmj.pa.StepSelectorStore;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.tirix.eclipse.MMLayout;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.MetamathMessageHandler;


/**
 * The step selector class, which proposes the different possible steps.
 * <p>
 */
// TODO Clean-up what is not used...
public class StepSelectorView extends ViewPart {
	public static final String VIEW_ID = "org.tirix.emetamath.views.StepSelectorView";
//	public static final String MENU_ID = "org.tirix.emetamath.views.StepSelectorView";
	public static final String APPLY_STEP_SELECTION_COMMAND_ID = "org.tirix.emetamath.applyStepSelectionCommand";

	protected TreeViewer viewer;
	protected ProofAssistantEditor proofAsstEditor;
	protected StepSelectorStore stepSelectorStore;
	private Label fStepSelectionLabel;
	
//	private Action action1;
//	private Action action2;
//	private Action doubleClickAction;

	/**
	 * The constructor.
	 */
	public StepSelectorView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite fParent = new Composite(parent, SWT.NONE);
		
		// Creating my own layout seems simpler than reusing one ?
		int barHeight = SWT.DEFAULT;
		int margin = 3; 
		Layout layout = new MMLayout.TopCenterLayout(barHeight, margin);
		fParent.setLayout(layout);

		fStepSelectionLabel = new Label(fParent, SWT.NONE);
		
		Composite composite = new Composite(fParent, SWT.NONE);
		composite.setLayout(new FillLayout());

		viewer = new TreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new StepSelectorContentProvider());
		viewer.setLabelProvider(new MMLabelProvider());
		//viewer.setSorter(new StepSorter());
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), VIEW_ID);
//		makeActions();
		registerContextMenu();
		registerDoubleClickAction();
//		contributeToActionBars();
	}

	public void setData(MetamathProjectNature nature, StepSelectorStore stepSelectorStore, ProofAssistantEditor editor) {
		((MMLabelProvider)viewer.getLabelProvider()).setNature(nature);
		if(stepSelectorStore != null) {
			int n = stepSelectorStore.getStoreItems().length;
			fStepSelectionLabel.setText((stepSelectorStore.hasMore()?"More than ":"")+n+" unifiable assertion"+(n>1?"s":"")+" for step "+stepSelectorStore.getStep());
		}
		else fStepSelectionLabel.setText(null);
		this.stepSelectorStore = stepSelectorStore;
		this.proofAsstEditor = editor;
		viewer.setInput(stepSelectorStore);
	}
	
	private void registerContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(IMenuManager manager) {
//				StepSelectorView.this.fillContextMenu(manager);
//			}
//		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	//	private void contributeToActionBars() {
//		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
//		fillLocalToolBar(bars.getToolBarManager());
//	}
//
//	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(action1);
//		manager.add(new Separator());
//		manager.add(action2);
//	}
//
//	private void fillContextMenu(IMenuManager manager) {
//		manager.add(action1);
//		manager.add(action2);
//		// Other plug-ins can contribute there actions here
//		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//	}
//	
//	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(action1);
//		manager.add(action2);
//	}
//
//	private void makeActions() {
//		action1 = new Action() {
//			public void run() {
//				showMessage("Action 1 executed");
//			}
//		};
//		action1.setText("Action 1");
//		action1.setToolTipText("Action 1 tooltip");
//		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		
//		action2 = new Action() {
//			public void run() {
//				showMessage("Action 2 executed");
//			}
//		};
//		action2.setText("Action 2");
//		action2.setToolTipText("Action 2 tooltip");
//		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		doubleClickAction = new Action() {
//			public void run() {
//				ISelection selection = viewer.getSelection();
//				Object obj = ((IStructuredSelection)selection).getFirstElement();
//				showMessage("Double-click detected on "+obj.toString());
//			}
//		};
//	}

	private void registerDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
//				doubleClickAction.run();
				IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand(APPLY_STEP_SELECTION_COMMAND_ID, null);
				} catch (Exception ex) {
					throw new RuntimeException("Exception while executing "+APPLY_STEP_SELECTION_COMMAND_ID, ex);
				}
			}
		});
	}

//	private void showMessage(String message) {
//		MessageDialog.openInformation(
//			viewer.getControl().getShell(),
//			"Metamath View",
//			message);
//	}

	@Override
    public Object getAdapter(Class key) {
        if (key == MetamathProjectNature.class) {
			return getNature();
		}
        return null;
    }

	public void clear() {
		// clear all messages about this proof
		MetamathProjectNature nature = getNature();
		FileEditorInput input = (FileEditorInput)proofAsstEditor.getEditorInput();
		if(input != null)
			((MetamathMessageHandler)nature.getMessageHandler()).clearMessages(input.getFile());

		// clear step list
		viewer.setInput(null);
		proofAsstEditor = null;
		stepSelectorStore = null;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public ContentViewer getViewer() {
		return viewer;
	}

	public ProofAssistantEditor getActiveEditor() {
		return proofAsstEditor;
	}

	public MetamathProjectNature getNature() {
		// TODO ugly.. better store the active nature?
		// Also, does not always work
		if(!(viewer.getLabelProvider() instanceof MMLabelProvider)) return null;
		return ((MMLabelProvider)viewer.getLabelProvider()).getNature();
	}

	public String getStep() {
		return stepSelectorStore.getStep();
	}

	public StepSelectorStore getStore() {
		return stepSelectorStore;
	}

	/**
	 * A content provider to provide Assertions and steps
	 */
	class StepSelectorContentProvider implements ITreeContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object parent) {
			return getChildren(parent); // new Object[] { parent }; // 
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof StepSelectorStore) {
				return ((StepSelectorStore)parentElement).getStoreItems();
			}
			if(parentElement instanceof StepSelectorItem) {
				String[] selection = ((StepSelectorItem)parentElement).getSelection();
				StepSelectorLine[] items = new StepSelectorLine[selection.length];
				items[0] = new StepSelectorLine(selection[0], PaConstants.STEP_SELECTOR_FORMULA_LABEL_SEPARATOR);
				for(int i=1;i<selection.length-1;i++) { items[i] = new StepSelectorLine(selection[i], PaConstants.STEP_SELECTOR_FORMULA_LOG_HYP_SEPARATOR); }
				items[selection.length - 1] = new StepSelectorLine(selection[selection.length - 1], PaConstants.STEP_SELECTOR_FORMULA_YIELDS_SEPARATOR);
				items[selection.length - 1].lastLine = true;
				return items;
			}
			if(parentElement instanceof Assrt) {
				Assrt assrt = (Assrt)parentElement;
				LogHyp[] logHypArray = assrt.getLogHypArray();
				Formula[] children = new Formula[logHypArray.length];
				for(int i=0;i<logHypArray.length - 1;i++) children[i] = logHypArray[i].getFormula();
				children[logHypArray.length - 1] = assrt.getFormula();
				return children;
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if(element instanceof StepSelectorStore) return true;
			if(element instanceof StepSelectorItem) return true;
			if(element instanceof Assrt) return true;
			return false;
		}
	}

	class StepSelectorLine {
		String line;
		boolean lastLine;
		
		public StepSelectorLine(String line, String separator) {
			this.line = line.substring(line.indexOf(separator) + separator.length());
		}

		@Override
		public String toString() { return line; }
	}
	
//	private class StepSelectorLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider {
//		private Font fFont;
//
//		public StepSelectorLabelProvider() {
//			FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
//			fFont = fontRegistry.get("org.tirix.emetamath.preferences.editorFont");
//		}
//
//		@Override
//		public String getColumnText(Object obj, int index) {
//			return getText(obj);
//		}
//
//		@Override
//		public Image getColumnImage(Object obj, int index) {
//			return getImage(obj);
//		}
//
//		@Override
//		public Image getImage(Object obj) {
//			return PlatformUI.getWorkbench().
//					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
//		}
//
//		@Override
//		public Font getFont(Object element, int columnIndex) {
//			return fFont;
//		}
//	}

	class StepSorter extends ViewerSorter {
	}
}
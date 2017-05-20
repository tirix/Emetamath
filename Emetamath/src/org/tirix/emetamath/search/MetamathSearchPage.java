package org.tirix.emetamath.search;

import java.util.ArrayList;
import java.util.List;

import mmj.lang.Assrt;
import mmj.lang.Cnst;
import mmj.lang.Formula;
import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Var;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class MetamathSearchPage extends DialogPage implements ISearchPage {
	public final static String PAGE_ID = "org.tirix.emetamath.search.metamathSearchPage";
	private ISearchPageContainer fContainer;
	private Button[] fSearchFor;
	private Button[] fLimitTo;
	private Button[] fIncludeMasks;
	private Group fLimitToGroup;
	private Combo fPattern;

	private MetamathProjectNature nature;
	private MObj mobj;
	private Formula formula;

	private boolean fFirstTime;
	private SearchData fInitialData;
	private List<SearchData> fPreviousSearchPatterns;

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite result= new Composite(parent, SWT.NONE);
		
		GridLayout layout= new GridLayout(2, false);
		layout.horizontalSpacing= 10;
		result.setLayout(layout);
		
		Control expressionComposite= createExpression(result);
		expressionComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		
		Label separator= new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint= convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		Control searchFor= createSearchFor(result);
		searchFor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		Control limitTo= createLimitTo(result);
		limitTo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		Control includeMask= createIncludeMask(result);
		includeMask.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
				
		//createParticipants(result);
		
		SelectionAdapter elementInitializer= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
//				if (getSearchFor() == fInitialData.getSearchFor())
//					fJavaElement= fInitialData.getJavaElement();
//				else
//					fJavaElement= null;
//				setLimitTo(getSearchFor(), getLimitTo());
//				setIncludeMask(getIncludeMask());
//				doPatternModified();
			}
		};

		for (int i= 0; i < fSearchFor.length; i++) {
			fSearchFor[i].addSelectionListener(elementInitializer);
		}

		setControl(result);

		Dialog.applyDialogFont(result);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, IJavaHelpContextIds.JAVA_SEARCH_PAGE);	
	}

	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				initSelections();
			}
			fPattern.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}
	
	private void initSelections() {
		try{
			String[] projectNames= getContainer().getSelectedProjectNames();
			if(projectNames.length < 1) nature = null;
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			nature = (MetamathProjectNature) root.getProject(projectNames[0]).getNature(MetamathProjectNature.NATURE_ID);
		}
		catch(CoreException e) {
			nature = null;
		}

		ISelection sel= getContainer().getSelection();

		SearchData initData= null;

		// TODO use AdapterFactory ?
//		if (sel instanceof IStructuredSelection) {
//			initData= tryStructuredSelection((IStructuredSelection) sel);
//		} else 
		if (sel instanceof ITextSelection) {
			initData.nature = nature;
			initData.mobj = nature.getMObj(((ITextSelection)sel).getText());
			// TODO determine other fields!
		}
		if (initData == null) {
			if (!fPreviousSearchPatterns.isEmpty()) {
				initData= (SearchData) fPreviousSearchPatterns.get(0);
			}
			else  initData= new SearchData();
		}
		
		fInitialData= initData;
		nature = initData.nature;
		mobj = initData.mobj;
		
		setSearchFor(initData.searchFor);
		setLimitTo(initData.searchFor, initData.limitTo);
		setIncludeMask(initData.includeMask);
		
		fPattern.setText(initData.pattern);
	}

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fPreviousSearchPatterns.size();
		String [] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchData) fPreviousSearchPatterns.get(i)).pattern;
		return patterns;
	}

	private Control createExpression(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		result.setLayout(layout);

		// Pattern text + info
		Label label= new Label(result, SWT.LEFT);
		label.setText("Search String"); 
		label.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));

		// Pattern combo
		fPattern = new Combo(result, SWT.SINGLE | SWT.BORDER);
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handlePatternSelected();
				updateOKStatus();
			}
		});
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doPatternModified();
				updateOKStatus();

			}
		});
		//TextFieldNavigationHandler.install(fPattern);
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

//		// Ignore case checkbox		
//		fCaseSensitive= new Button(result, SWT.CHECK);
//		fCaseSensitive.setText("Case sensitive"); 
//		fCaseSensitive.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				fIsCaseSensitive= fCaseSensitive.getSelection();
//			}
//		});
//		fCaseSensitive.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		
		return result;
	}

	private Control createSearchFor(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText("Search For"); 
		result.setLayout(new GridLayout(2, true));

		fSearchFor= new Button[] {
			createButton(result, SWT.RADIO, "Symbol", Sym.class, true),
			createButton(result, SWT.RADIO, "Constant", Cnst.class, false),
			createButton(result, SWT.RADIO, "Variable", Var.class, false),
			createButton(result, SWT.RADIO, "Formula", Formula.class, true),
			createButton(result, SWT.RADIO, "Label", Stmt.class, false),
		};

		// Fill with dummy radio buttons
		Label filler= new Label(result, SWT.NONE);
		filler.setVisible(false);
		filler.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		return result;		
	}
	
	private Control createLimitTo(Composite parent) {
		fLimitToGroup= new Group(parent, SWT.NONE);
		fLimitToGroup.setText("Limit To"); 
		fLimitToGroup.setLayout(new GridLayout(2, false));

		fillLimitToGroup(Sym.class, MetamathSearchQuery.ALL_OCCURRENCES);
		
		return fLimitToGroup;
	}
		
	private Control createIncludeMask(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		result.setText("Search In"); 
		result.setLayout(new GridLayout(4, false));
		fIncludeMasks= new Button[] {
			createButton(result, SWT.CHECK, "Metamath Files", MetamathSearchQuery.MM_FILES, true),
			createButton(result, SWT.CHECK, "Proof Assistant Files", MetamathSearchQuery.MMP_FILES, true),
		};
		return result;
	}
	
	private void fillLimitToGroup(Class<?> searchFor, int limitTo) {
		Control[] children= fLimitToGroup.getChildren();
		for (int i= 0; i < children.length; i++) {
			children[i].dispose();
		}
		
		ArrayList<Button> buttons= new ArrayList<Button>();
		buttons.add(createButton(fLimitToGroup, SWT.RADIO, "All occurrences", MetamathSearchQuery.ALL_OCCURRENCES, limitTo == MetamathSearchQuery.ALL_OCCURRENCES));
		if(searchFor.equals(Stmt.class)) {
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Proofs", MetamathSearchQuery.PROOFS, limitTo == MetamathSearchQuery.PROOFS));
		}

		if (Sym.class.isAssignableFrom(searchFor)) {
			//buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Declarations ", MetamathSearchQuery.DECLARATIONS, limitTo == MetamathSearchQuery.DECLARATIONS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Hypothesis", MetamathSearchQuery.HYPOTHESIS, limitTo == MetamathSearchQuery.HYPOTHESIS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Axioms", MetamathSearchQuery.AXIOMS, limitTo == MetamathSearchQuery.AXIOMS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Definitions", MetamathSearchQuery.DEFINITIONS, limitTo == MetamathSearchQuery.DEFINITIONS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Theorems", MetamathSearchQuery.THEOREMS, limitTo == MetamathSearchQuery.THEOREMS));
		}
		fLimitTo= (Button[]) buttons.toArray(new Button[buttons.size()]);
		
		SelectionAdapter listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				performLimitToSelectionChanged((Button) e.widget);
			}
		};
		for (int i= 0; i < fLimitTo.length; i++) {
			fLimitTo[i].addSelectionListener(listener);
		}
		Dialog.applyDialogFont(fLimitToGroup); // re-apply font as we disposed the previous widgets
		
		fLimitToGroup.layout();
	}

	private Button createButton(Composite parent, int style, String text, Object data, boolean isSelected) {
		Button button= new Button(parent, style);
		button.setText(text);
		button.setData(data);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setSelection(isSelected);
		return button;
	}
	
	protected final void performLimitToSelectionChanged(Button button) {
		if (button.getSelection()) {
			for (int i= 0; i < fLimitTo.length; i++) {
				Button curr= fLimitTo[i];
				if (curr != button) {
					curr.setSelection(false);
				}
			}
		}
		setIncludeMask(getIncludeMask());
	}

	private void handlePatternSelected() {
		int selectionIndex= fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
			return;
		
		SearchData initialData= (SearchData) fPreviousSearchPatterns.get(selectionIndex);

		mobj = initialData.mobj;
		nature = initialData.nature;
		setSearchFor(initialData.searchFor);
		setLimitTo(initialData.searchFor, initialData.limitTo);
		setIncludeMask(initialData.includeMask);

		fPattern.setText(initialData.pattern);
		fInitialData= initialData;
	}
	
	private void doPatternModified() {
		if (fInitialData != null && getPattern().equals(fInitialData.pattern) && fInitialData.mobj != null && fInitialData.searchFor == getSearchFor()) {
			nature = fInitialData.nature;
			mobj = fInitialData.mobj;
		} else {
			mobj = null;
		}
	}
	
	final void updateOKStatus() {
		boolean isValid= isValidSearchPattern();
		getContainer().setPerformActionEnabled(isValid);
	}
	
	private boolean isValidSearchPattern() {
		if (getPattern().length() == 0) {
			return false;
		}
		if (nature == null) {
			return false;
		}
		if (mobj == null || formula == null) {
			return false;
		}
		return true;		
	}

	@Override
	public boolean performAction() {
		MetamathSearchQuery searchJob= new MetamathSearchQuery(fInitialData.nature, fInitialData.mobj);
		NewSearchUI.runQueryInBackground(searchJob);
		return true;
	}
	
	private Class<?> getSearchFor() {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			if (button.getSelection()) {
				return (Class<?>) button.getData();
			}
		}
		throw new RuntimeException("No \"Search For\" button selected!");
	}
	
	private void setSearchFor(Class<?> searchFor) {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			button.setSelection(searchFor.equals(button.getData()));
		}
	}
	
	private int getLimitTo() {
		for (int i= 0; i < fLimitTo.length; i++) {
			Button button= fLimitTo[i];
			if (button.getSelection()) {
				return (Integer)button.getData();
			}
		}
		return -1;
	}

	private int setLimitTo(Class<?> searchFor, int limitTo) {
		if (searchFor.equals(Assrt.class) && limitTo != MetamathSearchQuery.PROOFS) {
			limitTo= MetamathSearchQuery.PROOFS;
		}

		if (Sym.class.isAssignableFrom(searchFor) && limitTo == MetamathSearchQuery.PROOFS) {
			limitTo= MetamathSearchQuery.ALL_OCCURRENCES;
		}
		fillLimitToGroup(searchFor, limitTo);
		return limitTo;
	}
	
	private String getPattern() {
		return fPattern.getText();
	}
	
	private int getIncludeMask() {
		int mask= 0;
		for (int i= 0; i < fIncludeMasks.length; i++) {
			Button button= fIncludeMasks[i];
			if (button.getSelection()) {
				mask |= (Integer)button.getData();
			}
		}
		return mask;
	}
	
	private void setIncludeMask(int includeMask) {
		for (int i= 0; i < fIncludeMasks.length; i++) {
			Button button= fIncludeMasks[i];
			button.setSelection((includeMask & (Integer)button.getData()) != 0);
		}
	}

	/**
	 * Sets the search page's container.
	 * @param container the container to set
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}

	/**
	 * Returns the search page's container.
	 * @return the search page container
	 */
	private ISearchPageContainer getContainer() {
		return fContainer;
	}
	
	protected static class SearchData {
		MetamathProjectNature nature;
		MObj mobj;
		Formula formula;
		String pattern;

		Class<?> searchFor;
		int limitTo;
		int includeMask;
	}
}

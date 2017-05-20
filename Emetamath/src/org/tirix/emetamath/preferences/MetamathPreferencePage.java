package org.tirix.emetamath.preferences;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tirix.emetamath.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class MetamathPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	private static final int INDENT = 20;
	private Button fBracketHighlightingCheckbox;
	private Button fEnclosingBracketsRadioButton;
	private Button fMatchingBracketAndCaretLocationRadioButton;
	/** List of master/slave listeners when there's a dependency. */
	private ArrayList<SelectionListener> fMasterSlaveListeners= new ArrayList<SelectionListener>();
	private Button fMatchingBracketRadioButton;

	public MetamathPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		//setDescription("Preferences for the eMetamath plugin");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(
		new BooleanFieldEditor(
			PreferenceConstants.P_DOUBLE_CLICK_FOR_STEP_SELECTION,
			"&Double Click to open Step Selector",
			getFieldEditorParent()));

		dumpPreferences("init");
		createBracketPreferenceFields();
		initializeBracketHighlightingPreferences();
		initializeFields();
		
//		addField(new DirectoryFieldEditor(PreferenceConstants.P_PATH, 
//				"&Directory preference:", getFieldEditorParent()));
//		addField(
//			new BooleanFieldEditor(
//				PreferenceConstants.P_BOOLEAN,
//				"&An example of a boolean preference",
//				getFieldEditorParent()));
//
//		addField(new RadioGroupFieldEditor(
//				PreferenceConstants.P_CHOICE,
//			"An example of a multiple-choice preference",
//			1,
//			new String[][] { { "&Choice 1", "choice1" }, {
//				"C&hoice 2", "choice2" }
//		}, getFieldEditorParent()));
//		addField(
//			new StringFieldEditor(PreferenceConstants.P_STRING, "A &text preference:", getFieldEditorParent()));
	}

	private void createBracketPreferenceFields() {
		// TODO rewrite using BooleanFieldEditor ?
		Composite parent = getFieldEditorParent();
		
		Label spacer= new Label(parent, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		spacer.setLayoutData(gd);

		String label= "Bracket highlighting";
		fBracketHighlightingCheckbox= addButton(parent, SWT.CHECK, label, 0, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS, fBracketHighlightingCheckbox.getSelection());
			}
		});

		Composite radioComposite= new Composite(parent, SWT.NONE);
		GridLayout radioLayout= new GridLayout();
		radioLayout.marginWidth= 0;
		radioLayout.marginHeight= 0;
		radioComposite.setLayout(radioLayout);

		label= "Matching bracket";
		fMatchingBracketRadioButton = addButton(radioComposite, SWT.RADIO, label, 0, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fMatchingBracketRadioButton.getSelection()) {
					getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, false);
					getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS, false);
				}
				dumpPreferences("fMatchingBracketRadioButton selected");
			}
		});
		createDependency(fBracketHighlightingCheckbox, fMatchingBracketRadioButton);

		label= "Matching bracket and caret location";
		fMatchingBracketAndCaretLocationRadioButton= addButton(radioComposite, SWT.RADIO, label, 0, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fMatchingBracketAndCaretLocationRadioButton.getSelection()) 
					getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, true);
				dumpPreferences("fMatchingBracketAndCaretLocationRadioButton selected");
			}
		});
		createDependency(fBracketHighlightingCheckbox, fMatchingBracketAndCaretLocationRadioButton);

		label= "Enclosing brackets";
		fEnclosingBracketsRadioButton= addButton(radioComposite, SWT.RADIO, label, 0, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selection= fEnclosingBracketsRadioButton.getSelection();
				getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS, selection);
				if (selection)
					getPreferenceStore().setValue(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, true);
				dumpPreferences("fEnclosingBracketsRadioButton selected");
			}
		});
		createDependency(fBracketHighlightingCheckbox, fEnclosingBracketsRadioButton);

		spacer= new Label(parent, SWT.LEFT);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		spacer.setLayoutData(gd);
	}

	private Button addButton(Composite parent, int style, String label, int indentation, SelectionListener listener) {
		Button button= new Button(parent, style);
		button.setText(label);

		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		button.setLayoutData(gd);
		button.addSelectionListener(listener);
		//makeScrollableCompositeAware(button);

		return button;
	}

	protected void createDependency(final Button master, final Control slave) {
		Assert.isNotNull(slave);
		indent(slave);
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean state= master.getSelection();
				slave.setEnabled(state);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		master.addSelectionListener(listener);
		fMasterSlaveListeners.add(listener);
	}

	protected static void indent(Control control) {
		((GridData) control.getLayoutData()).horizontalIndent+= INDENT;
	}

	private void initializeBracketHighlightingPreferences() {
		boolean matchingBrackets= getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS);
		boolean highlightBracketAtCaretLocation= getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION);
		boolean enclosingBrackets= getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS);
		
		fBracketHighlightingCheckbox.setSelection(matchingBrackets);
		fEnclosingBracketsRadioButton.setEnabled(matchingBrackets);
		fMatchingBracketRadioButton.setEnabled(matchingBrackets);
		fMatchingBracketAndCaretLocationRadioButton.setEnabled(matchingBrackets);
		fEnclosingBracketsRadioButton.setSelection(enclosingBrackets);
		if (!enclosingBrackets) {
			fMatchingBracketRadioButton.setSelection(!highlightBracketAtCaretLocation);
			fMatchingBracketAndCaretLocationRadioButton.setSelection(highlightBracketAtCaretLocation);
		}
	}

	private void initializeFields() {
        // Update slaves
        Iterator<SelectionListener> iter3= fMasterSlaveListeners.iterator();
        while (iter3.hasNext()) {
            SelectionListener listener= iter3.next();
            listener.widgetSelected(null);
        }
	}	
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	@Override
	public void performDefaults() {
		getPreferenceStore().setToDefault(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS);
		getPreferenceStore().setToDefault(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION);
		getPreferenceStore().setToDefault(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS);

		super.performDefaults();
		initializeBracketHighlightingPreferences();
		dumpPreferences("defaults");
	}

	private void dumpPreferences(String info) {
		System.out.println(info);
		System.out.println("  P_EDITOR_MATCHING_BRACKETS="+getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS));
		System.out.println("  P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION="+getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION));
		System.out.println("  P_EDITOR_ENCLOSING_BRACKETS="+getPreferenceStore().getBoolean(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}
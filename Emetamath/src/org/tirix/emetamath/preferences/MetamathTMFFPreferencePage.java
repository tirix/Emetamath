package org.tirix.emetamath.preferences;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathPreferences;

import mmj.tmff.TMFFPreferences;

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

public class MetamathTMFFPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public MetamathTMFFPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Text Mode Formula Formatting (TMFF) Preferences:");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
//		addField(
//		new ComboFieldEditor(
//			PreferenceConstants.P_TMFF_FORMAT_NBR,
//			"&Method",
//			new String[][] { 
//				{ "Two-Column Alignment", "TMFFTwoColumnAlignment" },
//				{ "Align Column", "TMFFAlignColumn" },
//				{ "Flat", "TMFFFlat" },
//				{ "Unformatted", "TMFFUnformatted" },
//				},
//			getFieldEditorParent()));
		TMFFPreferences pref = MetamathPreferences.getInstance().getTMFFPreferences();
		addField(new MetamathPreferenceField.Integer(PreferenceConstants.P_TMFF_FORMAT_NBR, "&Format Number", pref.currFormatNbr).getFieldEditor(getFieldEditorParent()));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	@Override
	public void performDefaults() {
		getPreferenceStore().setToDefault(PreferenceConstants.P_TMFF_FORMAT_NBR);
		super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}
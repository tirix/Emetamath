package org.tirix.emetamath.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.tirix.emetamath.Activator;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_DOUBLE_CLICK_FOR_STEP_SELECTION, false);
		store.setDefault(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(PreferenceConstants.P_EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, false);
		store.setDefault(PreferenceConstants.P_EDITOR_ENCLOSING_BRACKETS, false);
		store.setDefault(PreferenceConstants.P_EDITOR_MATCHING_BRACKETS_COLOR, "192,192,192");
		store.setDefault(PreferenceConstants.P_EDITOR_PRINT_MARGIN, true);
		store.setDefault(PreferenceConstants.P_EDITOR_PRINT_MARGIN_COLOR, "192,192,192");
		store.setDefault(PreferenceConstants.P_EDITOR_PRINT_MARGIN_COLUMN, "87");
		store.setDefault(PreferenceConstants.P_EDITOR_SPACES_FOR_TABS, true);
		store.setDefault(PreferenceConstants.P_EDITOR_TAB_WIDTH, "2");

		store.setDefault(PreferenceConstants.P_TMFF_FORMAT_NBR, "2");
	}
}

package org.tirix.emetamath.nature;

import org.eclipse.jface.preference.IPreferenceStore;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.preferences.PreferenceConstants;

import mmj.lang.LogicalSystem;
import mmj.pa.ProofAsstPreferences;
import mmj.pa.SessionStore;
import mmj.tl.TlPreferences;
import mmj.tmff.TMFFPreferences;

public class MetamathPreferences {
	
    private TMFFPreferences tmffPreferences;
    private ProofAsstPreferences  proofAsstPreferences;
	private TlPreferences tlPreferences;

	private SessionStore store;
	
	private static MetamathPreferences singleton;
	
	public static MetamathPreferences getInstance() {
		if(singleton == null) singleton = new MetamathPreferences();
		return singleton;
	}
	
	/**
	 * Returns (MMJ) Preference Store
	 * @return
	 */
	public SessionStore getSessionStore() {
		if(store == null) store = new SessionStore();
		// TODO link this with the Eclipse Preferences page!
    	return store;
	}

	/**
	 * Returns (Eclipse) Preference Store
	 * @return
	 */
	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
    /**
     *  Fetches a reference to the ProofAsstPreferences,
     *  first initializing it if necessary.
     *
     *  Note: must re-initialize the TMFFPreferences
     *        reference in ProofAsstPreferences because
     *        TMFFBoss controls which instance of
     *        TMFFPreferences is active!!!
     *
     *  @return ProofAsstPreferences object ready to go.
     */
    public ProofAsstPreferences getProofAsstPreferences() {
        if (proofAsstPreferences == null) {
            proofAsstPreferences  = new ProofAsstPreferences();
            proofAsstPreferences.tmffPreferences = getTMFFPreferences();
        }
        proofAsstPreferences.rpnProofRightCol.set(80);
        return proofAsstPreferences;
    }

    /**
     *  Fetches a reference to the TMFFPreferences,
     *  first initializing it if necessary.
     *
     *  @return TMFFPreferences object ready to go.
     */
    public TMFFPreferences getTMFFPreferences() {

        if (tmffPreferences == null) {
            tmffPreferences   = buildTMFFPreferences();
            tmffPreferences.currFormatNbr.set(getPreferenceStore().getInt(PreferenceConstants.P_TMFF_FORMAT_NBR));
        }
        return tmffPreferences;
    }

    /**
     *  Construct TMFFPreferences object from scratch.
     *
     *  @return TMFFPreferences object ready to go.
     */
    protected TMFFPreferences buildTMFFPreferences() {

        return new TMFFPreferences(getSessionStore());
    }

    /**
     *  Fetches a reference to the TlPreferences,
     *  first initializing it if necessary.
     *  <p>
     *  @param logicalSystem the
     *  @return TlPreferences object ready to go.
     */
    public TlPreferences getTlPreferences(LogicalSystem logicalSystem) {

        if (tlPreferences == null) {

            tlPreferences         =
                new TlPreferences(logicalSystem, getSessionStore());
        }

        return tlPreferences;
    }
}

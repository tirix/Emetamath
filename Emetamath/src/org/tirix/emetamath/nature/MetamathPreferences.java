package org.tirix.emetamath.nature;

import mmj.lang.LogicalSystem;
import mmj.pa.ProofAsstPreferences;
import mmj.tl.TlPreferences;
import mmj.tmff.TMFFPreferences;

public class MetamathPreferences {
	
    private TMFFPreferences tmffPreferences;
    private ProofAsstPreferences  proofAsstPreferences;
	private TlPreferences tlPreferences;

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
            proofAsstPreferences.
                setTMFFPreferences(getTMFFPreferences());
        }

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
        }
        return tmffPreferences;
    }

    /**
     *  Construct TMFFPreferences object from scratch.
     *
     *  @return TMFFPreferences object ready to go.
     */
    protected TMFFPreferences buildTMFFPreferences() {

        return new TMFFPreferences();
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
                new TlPreferences(logicalSystem);
        }

        return tlPreferences;
    }
}

package org.tirix.emetamath.editors.proofassistant;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.pa.ProofAsstPreferences;
import mmj.pa.ProofWorksheet;
import mmj.verify.Grammar;

import org.eclipse.jface.text.Document;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

public class ProofDocument extends Document implements SystemLoadListener {
	ProofWorksheet proofWorksheet;
	MetamathProjectNature nature;
	boolean proofWorksheetInitialized;
	String newTheoremLabel;

	public ProofDocument() {
		proofWorksheetInitialized = false;
	}
	
	public ProofWorksheet getWorksheet() {
		return proofWorksheet;
	}
	
	public void setup(ProofWorksheet proofWorksheet, MetamathProjectNature nature) {
		this.newTheoremLabel = proofWorksheet.getTheoremLabel();
		this.nature = nature;
		this.proofWorksheet = proofWorksheet;
		proofWorksheetInitialized = true;
	}

	public void setup(String newTheoremLabel, MetamathProjectNature nature) {
		this.newTheoremLabel = newTheoremLabel;
		this.nature = nature;
		if(!nature.isLogicalSystemLoaded()) { 
			nature.addSystemLoadListener(this); 
			return; 
		}
		LogicalSystem logicalSystem = nature.getLogicalSystem();
		Grammar grammar = nature.getGrammar();
		MessageHandler messageHandler = nature.getMessageHandler();
		ProofAsstPreferences proofAsstPreferences = new ProofAsstPreferences();
		proofWorksheet = new ProofWorksheet(newTheoremLabel, proofAsstPreferences, logicalSystem, grammar, messageHandler);
		proofWorksheetInitialized = true;
	}

	public void set(ProofWorksheet w) {
		this.proofWorksheet = w;
		set(w.getOutputProofText());
	}

	@Override
	public void systemLoaded() {
		setup(newTheoremLabel, nature);
	}
}

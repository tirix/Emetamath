package org.tirix.emetamath.editors.proofassistant;

import mmj.lang.LogicalSystem;
import mmj.lang.MessageHandler;
import mmj.pa.ProofAsstPreferences;
import mmj.pa.ProofWorksheet;
import mmj.verify.Grammar;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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
		System.out.println("Proof Worksheet Successfully initialized");
	}

	@Override
	public void systemLoaded() {
		setup(newTheoremLabel, nature);
	}
}

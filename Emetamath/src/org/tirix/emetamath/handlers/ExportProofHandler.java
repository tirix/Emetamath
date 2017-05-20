package org.tirix.emetamath.handlers;

import java.io.FileNotFoundException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.editors.proofassistant.ProofAssistantEditor;
import org.tirix.emetamath.editors.proofassistant.ProofDocument;
import org.tirix.emetamath.nature.MetamathPreferences;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.ResourceSource;

import mmj.mmio.SourcePosition;
import mmj.pa.ProofWorksheet;
import mmj.tl.MMTTheoremExportFormatter;
import mmj.tl.TheoremLoaderException;
import mmj.tl.TlPreferences;

public class ExportProofHandler extends MetamathEditorActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		final ProofAssistantEditor editor = (ProofAssistantEditor)getEditor(context);
		final ProofDocument document = editor.getDocument();
		final ProofWorksheet proofWorksheet = document.getWorksheet();

		final MetamathProjectNature nature = getNature(context);
		final MetamathPreferences preferences = nature.getPreferences();
		final TlPreferences tlPreferences = preferences.getTlPreferences(nature.getLogicalSystem());
		final MMTTheoremExportFormatter mmtTheoremExportFormatter = new MMTTheoremExportFormatter(tlPreferences);
		// TODO - actually, the proof from the MMTTheoremExportFormatter is not the same as the unified proof !?
		
		try {
	        SourcePosition position = null;
			List<StringBuilder> mmtTheoremLines = null;
	        if(proofWorksheet.isNewTheorem()){
	        	if(proofWorksheet.locAfter == null) {
	        		position = new ResourceSource(nature.getMainFile(), nature.getProject()).getEndPosition();
	        	} else {
	        		position = proofWorksheet.locAfter.getPosition().after();
					// TODO Position this better?
	        	}
	        	mmtTheoremLines = mmtTheoremExportFormatter.buildStringBuilderLineList(proofWorksheet);	        
	        } else {
		        position = proofWorksheet.theorem.getPosition();
	        	mmtTheoremLines = mmtTheoremExportFormatter.buildConclusionStringBuilderLineList(proofWorksheet);	        
	        }
			StringBuilder sb = new StringBuilder();
	        for(StringBuilder line:mmtTheoremLines) sb.append(line+"\n");

	        replaceString(position, sb.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (TheoremLoaderException e) {
			nature.getMessageHandler().accumException(e);
		}
		return null;
	}

	private void replaceString(SourcePosition position, String string) {
        try {
            MetamathEditor editor = MetamathUI.selectAndReveal(position, true);
            if(editor == null) return;
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			document.replace(position.charStartNbr, position.charEndNbr-position.charStartNbr, string);
			editor.selectAndReveal(position.charStartNbr, string.length());
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
		}
	}
}


//// DONE Use MMTTheoremExportFormatter
//System.out.println("Exporting proof:" + w.getTheoremLabel());
//StringBuilder sb = new StringBuilder();
//String indent = "\n  ";
//sb.append(MMIOConstants.MM_BEGIN_SCOPE_KEYWORD);
//sb.append(indent);
//for(ProofWorkStmt stmt:w.proofWorkStmtList) {
//	if(stmt instanceof CommentStmt) {
//		sb.append(MMIOConstants.MM_BEGIN_COMMENT_KEYWORD);
//		sb.append(" "+((CommentStmt)stmt).getStmtText()+" ");
//		sb.append(MMIOConstants.MM_END_COMMENT_KEYWORD);
//		sb.append(indent);
//	}
//	if(stmt instanceof HypothesisStep) {
//		// Assuming we only want to use LogHyp, and never VarHyp...
//		sb.append(((HypothesisStep)stmt).getRefLabel()+" ");
//		sb.append(MMIOConstants.MM_LOG_HYP_KEYWORD);
//		sb.append(" "+((HypothesisStep)stmt).getFormula()+" ");
//		sb.append(MMIOConstants.MM_END_STMT_KEYWORD);
//		sb.append(indent);
//	}
//}
//sb.append(w.getTheoremLabel()+" ");
//sb.append(MMIOConstants.MM_PROVABLE_ASSRT_KEYWORD);
//sb.append(" "+w.getQedStep().getFormula()+" ");
//sb.append(MMIOConstants.MM_START_PROOF_KEYWORD);
//sb.append(indent);
//sb.append(w.generatedProofStmt.getStmtText().substring(PaConstants.GENERATED_PROOF_STMT_TOKEN.length())+" ");
//sb.append("\n");
//sb.append(MMIOConstants.MM_END_SCOPE_KEYWORD);
//System.out.println(sb);

package org.tirix.emetamath.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.tirix.emetamath.editors.MMRegionProvider;
import org.tirix.emetamath.editors.MMWhitespaceDetector;
import org.tirix.emetamath.editors.MetamathEditor;

public class AddBlockComment extends MetamathEditorActionHandler {
	IWhitespaceDetector whitespaceDetector = new MMWhitespaceDetector();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MetamathEditor editor = getEditor((IEvaluationContext) event.getApplicationContext());
		TextSelection selection = (TextSelection)editor.getSelectionProvider().getSelection();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		
		// check that start and end are not already inside a comment...
		if(MMRegionProvider.getComment(doc, selection.getOffset()) != null) return null;
		if(MMRegionProvider.getComment(doc, selection.getOffset() + selection.getLength()) != null) return null;
		
		// first make sure that the selection starts and ends on word boundaries
		IRegion endRegion = MMRegionProvider.getWord(doc, selection.getOffset() + selection.getLength());
		int offset = MMRegionProvider.getWord(doc, selection.getOffset()).getOffset()-1;
		int length = (endRegion.getOffset() + endRegion.getLength()) - offset + 1;

		try {
			char[] text = doc.get(offset, length).toCharArray();
			
			// replace all '$' at the start of a word by '@' 
			for(int i=0;i<text.length;i++) {
				if(text[i] == '$' && (i ==0 || whitespaceDetector.isWhitespace(text[i-1]))) {
					text[i] = '@';
				}
			}
			// also add the enclosing braces $( and $)
			doc.replace(offset, length, "$(\n"+new String(text)+"$)\n");
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}

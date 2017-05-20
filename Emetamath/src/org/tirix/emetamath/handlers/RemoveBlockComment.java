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

public class RemoveBlockComment extends MetamathEditorActionHandler {
	IWhitespaceDetector whitespaceDetector = new MMWhitespaceDetector();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MetamathEditor editor = getEditor((IEvaluationContext) event.getApplicationContext());
		TextSelection selection = (TextSelection)editor.getSelectionProvider().getSelection();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		
		// get the whole comment region...
		IRegion comment = MMRegionProvider.getComment(doc, selection.getOffset());
		if(comment == null || !comment.equals(MMRegionProvider.getComment(doc, selection.getOffset() + selection.getLength()))) return null;
		
		try {
			char[] text = doc.get(comment.getOffset()+2, comment.getLength()-4).toCharArray();
			
			// replace all '@' at the start of a word by '$' 
			for(int i=0;i<text.length;i++) {
				if(text[i] == '@' && (i ==0 || whitespaceDetector.isWhitespace(text[i-1]))) {
					text[i] = '$';
				}
			}
			// also add the enclosing braces $( and $)
			doc.replace(comment.getOffset(), comment.getLength(), new String(text));
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}

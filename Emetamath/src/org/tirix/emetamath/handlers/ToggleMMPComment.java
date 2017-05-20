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

public class ToggleMMPComment extends MetamathEditorActionHandler {
	final static MMWhitespaceDetector WHITESPACE_DETECTOR = new MMWhitespaceDetector();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MetamathEditor editor = getEditor((IEvaluationContext) event.getApplicationContext());
		TextSelection selection = (TextSelection)editor.getSelectionProvider().getSelection();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		
		// check that start and end are not already inside a comment...
		IRegion startComment = MMRegionProvider.getMMPComment(doc, selection.getOffset());
		IRegion endComment = MMRegionProvider.getMMPComment(doc, selection.getOffset() + selection.getLength());
		boolean toggleOn = startComment == null && endComment == null;
		//System.out.println("Toggle comments "+(toggleOn?"ON":"OFF")+" ("+startComment+"/"+endComment+")");
		int offset = selection.getOffset();
		int end = offset + selection.getLength();
		try {
			while(offset < end) {
				IRegion stepLine = MMRegionProvider.getMMPStep(doc, offset);

				if(toggleOn) {
					// add a star at the beginning of the line
					doc.replace(stepLine.getOffset(), 0, "* ");
					offset = stepLine.getOffset() + stepLine.getLength() + 3;
				} else {
					// remove stars at the beginning of the line
					int count = 0;
					if(doc.getChar(stepLine.getOffset()) == '*') {
						count = 1;
						while(WHITESPACE_DETECTOR.isWhitespace(doc.getChar(stepLine.getOffset()+count))) count++;
					}
					doc.replace(stepLine.getOffset(), count, "");
					offset = stepLine.getOffset() + stepLine.getLength() + 1;
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}

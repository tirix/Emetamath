package org.tirix.emetamath.popup.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.tirix.emetamath.editors.MMFormulaProvider;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathProjectNature;

/**
 * Text navigation action to navigate to the next higher level formula.
 *
 * @since 3.0
 */
public class NextLevelFormulaAction extends TextNavigationAction {
	protected MetamathEditor editor;
	protected ISourceViewer viewer;
	protected boolean end;
	protected boolean select;
	// TODO possibly implement this as a Command/Handler
	
	/**
	 * Creates a new next higher level formula action.
	 *
	 * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
	 */
	public NextLevelFormulaAction(MetamathEditor editor, ISourceViewer viewer, int code) {
		super(viewer.getTextWidget(), code);
		this.editor = editor;
		this.viewer = viewer;
		this.select = code == ST.SELECT_WORD_NEXT || code == ST.SELECT_WORD_PREVIOUS;
		this.end = code == ST.WORD_NEXT || code == ST.SELECT_WORD_NEXT;
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		final IDocument document= viewer.getDocument();
		final Point selection = getTextWidget().getSelectionRange();
		final MetamathProjectNature nature = MetamathProjectNature.getNature(editor.getEditorInput());
		if(nature == null || !nature.isLogicalSystemLoaded()) return;
		try {
			int mode = select?MMFormulaProvider.LEFT_OR_RIGHT:(end?MMFormulaProvider.RIGHT:MMFormulaProvider.LEFT);
			IRegion region = (IRegion)MMFormulaProvider.getShortestFormula(document, nature, selection.x, selection.y, mode);
			if (region != null) setNextLevel(region.getOffset(), region.getLength());
		} catch (BadLocationException x) {
			// ignore
		}
	}

	/**
	 * Sets the caret position and/or selection.
	 *
	 * @param offset Starting offset of the formula the action should use
	 * @param length Length of the formula the action should use
	 */
	protected void setNextLevel(int offset, int length) {
		final StyledText text = getTextWidget();
		if (text != null && !text.isDisposed()) {
			if(select) {
				if(end) {
					text.setSelectionRange(offset, length);
				} else {
					text.setSelectionRange(offset+length, -length);
				}
				getTextWidget().showSelection();
				fireSelectionChanged();
			} else {
				if(end) {
					text.setCaretOffset(offset+length);
				} else {
					text.setCaretOffset(offset);
				}
			}
		}
	}
}

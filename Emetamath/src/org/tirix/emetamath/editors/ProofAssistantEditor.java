//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  ProofAssistantEditor.java  0.09 08/01/2008
 *
 *  Version 0.02:
 *  ==> Add renumber feature
 *
 *  09-Sep-2006 - Version 0.03 - TMFF enhancement.
 *
 *  Version 0.04 06/01/2007
 *  ==> misc.
 *
 *  Version 0.05 08/01/2007
 *  ==> Modified to not rebuild the RequestMessagesGUI frame
 *      each time. The user should position the screen and
 *      resize it so that it is visible underneath (or above)
 *      the ProofAssistantEditor screen -- or just Alt-Tab to view
 *      any messages.
 *
 *  Version 0.06 09/11/2007
 *  ==> Bug fix -> set foreground/background at initialization.
 *  ==> Modify setProofTextAreaCursorPos(ProofWorksheet w) to
 *      compute the column number of the ProofAsstCursor's
 *      fieldId.
 *  ==> Added stuff for new "Set Indent" and
 *      "Reformat Proof: Swap Alt" menu items.
 *
 *  Version 0.07 02/01/2008
 *  ==> Add "accelerator" key definitions for
 *          Edit/Increase Font Size = Ctrl + "="
 *          Edit/Decrease Font Size = Ctrl + "-"
 *          Edit/Reformat Proof     = Ctrl + "R"
 *      Note: Ctrl + "+" seems to require Ctrl-Shift + "+",
 *            so in practice we code for Ctrl + "=", since
 *            "=" and "+" are most often on the same physical
 *            key and "=" is the unshifted glyph.
 *      Note: These Ctrl-Plus/Ctrl-Minus commands to increase/
 *            decrease font size are familiar to users of
 *            the Mozilla browser...
 *  ==> Fix bug: Edit/Decrease Font Size now checks for
 *            minimum font size allowed (8) and does not
 *            allow further reductions (a request to go from
 *            8 to 6 is treated as a change from 8 to 8.) This
 *            bug manifested as 'Exception in thread
 *            "AWT-EventQueue-0" java.lang.ArithmeticException:
 *            / by zero at javax.swing.text.PlainView.paint(
 *            Unknown Source)'. Also added similar range checking
 *            for Edit/Increase Font Size.
 *  ==> Modify request processing for unify and tmffReformat
 *      to pass offset of caret plus one as "inputCaretPos"
 *      for use in later caret positioning.
 *  ==> Tweak: Do not reformat when format number or indent
 *             amount is changed. This allows for single step
 *             reformatting -- but requires that the user
 *             manually initiate reformatting after changing
 *             format number or indent amount.
 *  ==> Add "Reformat Step" and "Reformat Step: Swap Alt" to
 *      popup menu. Then modified tmffReformat-related stuff
 *      to pass the boolean "inputCursorStep" to the standard
 *      reformatting procedure(s) so that the request can be
 *      handled using the regular, all-steps logic.
 *  ==> Turn "Greetings, friend" literal into PaConstants
 *      constant, PROOF_ASST_GUI_STARTUP_MSG.
 *  ==> Add "Incomplete Step Cursor" Edit menu item.
 *
 *  Version 0.08 03/01/2008
 *  ==> Add StepSelectorSearch to Unify menu
 *  ==> Add "callback" function for use by StepSelectionDialog,
 *          proofAsstGUI.unifyWithStepSelectorChoice()
 *  ==> Add Unify + Rederive to Unify menu
 *  ==> Eliminate Unify + Get Hints from Unify Menu
 *
 *  Version 0.09 08/01/2008
 *  ==> Add TheoremLoader stuff.
 */

package org.tirix.emetamath.editors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mmj.pa.*;
import mmj.lang.Assrt;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.lang.Messages;
import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;
import mmj.tmff.TMFFException;
import mmj.tmff.TMFFConstants;
import mmj.tl.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.tirix.emetamath.editors.MetamathEditor.MMContentOutlinePage;

/**
 *  The <code>ProofAssistantEditor</code> class is the main user
 *  interface for the mmj2 Proof Assistant feature.
 *  <p>
 *  A proof is represented in the GUI as a single text
 *  area, and the GUI knows nothing about the contents
 *  inside; all work on the proof is done elsewhere via
 *  mmj.pa.ProofAsst.java.
 *  <p>
 *  Note: ProofAssistantEditor is single-threaded in the ProofAsst
 *  process which is triggered in BatchMMJ2. The RunParm
 *  that triggers ProofAssistantEditor does not terminate until
 *  ProofAssistantEditor terminates.
 *  <p>
 *  The main issues dealt with in the GUI have to do with
 *  doing all of the screen updating code on the Java
 *  event thread. Unification is performed using a separate
 *  thread which "calls back" to ProofAssistantEditor when/if the
 *  Unification process is complete. (As of February 2006,
 *  the longest theorem unification computation is around
 *  1/2 second.)
 */
public class ProofAssistantEditor extends TextEditor {

	public static final String EDITOR_ID = "org.tirix.emetamath.ProofAssistant";
	// save constructor parms: proofAsst, proofAsstPreferences
    private ProofAsst               proofAsst;
    private ProofAsstPreferences
                                    proofAsstPreferences;

    private TheoremLoader           theoremLoader;
    private TlPreferences           tlPreferences;

    protected Stmt					fStmt;
    
	protected MMContentOutlinePage	fOutlinePage;
	protected Composite				fParent, fDefaultComposite, fProofAssistantBar;
	protected Label					fTheoremLabel;
	protected Text					fInsertAfterText;
	
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new ProofViewerConfiguration(this, new ColorManager()));
		}

	/**
	 * If the input is of type FileEditorInput, convert it to ProofEditorInput
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
	}

	@Override
	public void createPartControl(Composite parent) {
		fParent = new Composite(parent, SWT.NONE);
		
		// Creating my own layout seems simpler than reusing one ?
		final int barHeight = SWT.DEFAULT;
		Layout layout = new TopCenterLayout(barHeight);
		fParent.setLayout(layout);

		fProofAssistantBar = createProofAssistantBar(fParent);
		
		fDefaultComposite = new Composite(fParent, SWT.NONE);
		fDefaultComposite.setLayout(new FillLayout());

		super.createPartControl(fDefaultComposite);
		
		}

	private Composite createProofAssistantBar(Composite parent) {
		Composite bar = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 7;
		bar.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		bar.setLayoutData(data);

		//new Label(bar, SWT.NONE).setText("Theorem : ");
		fTheoremLabel = new Label(bar, SWT.NONE);
		fTheoremLabel.setData("Test2");
		new Label(bar, SWT.BORDER); // Vertical Separator
		new Label(bar, SWT.NONE).setText("Insert after : ");
		fInsertAfterText = new Text(bar, SWT.SINGLE | SWT.BORDER);
		fInsertAfterText.setData("Test3");
		
		// horizontal strut
		new Label(bar, SWT.NONE).setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		
		ToolBar toolBar = new ToolBar(bar, SWT.FLAT | SWT.WRAP);
		ToolItem item = new ToolItem(toolBar, SWT.PUSH);
		item.setText("Test 0");
		return bar;
	}

    public static final class TopCenterLayout extends Layout {
		private final int barHeight;

		public TopCenterLayout(int barHeight) {
			this.barHeight = barHeight;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Control[] children = composite.getChildren();
			Control top = children[0];
			Point topSize = top.computeSize(wHint, barHeight, flushCache);

			Control center = children[1];
			Point centerSize = center.computeSize(wHint, SWT.DEFAULT, flushCache);
			
			return new Point(Math.max(topSize.x, centerSize.x), topSize.y + centerSize.y);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle rect = composite.getClientArea();
			Control[] children = composite.getChildren();
			
			Control top = children[0];
			Point pt = top.computeSize(SWT.DEFAULT, barHeight, flushCache);
			top.setBounds(rect.x, rect.y, rect.width, pt.y);

			Control center = children[1];
			center.setBounds(rect.x, rect.y + pt.y, rect.width, rect.height - pt.y);
		}
	}

    public static final class BarLayout extends Layout {
		private final int margin;

		public BarLayout(int margin) {
			this.margin = margin;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			int width = 0;
			
			for(Control child:composite.getChildren()) {
				Point size = child.computeSize(SWT.DEFAULT, hHint, flushCache);
				width += size.x + margin;
			}
			return new Point(width, hHint);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle rect = composite.getClientArea();
			int x = rect.x;
			
			for(Control child:composite.getChildren()) {
				Point size = child.computeSize(SWT.DEFAULT, rect.height, flushCache);
				child.setBounds(x, rect.y, size.x, rect.height);
				x += size.x + margin;
			}
		}
	}
}

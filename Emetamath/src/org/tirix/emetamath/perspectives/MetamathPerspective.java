/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tirix.emetamath.perspectives;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.tirix.emetamath.views.MathView;
import org.tirix.emetamath.views.ProofBrowserView;
import org.tirix.emetamath.views.ProofExplorerView;
import org.tirix.emetamath.views.StepSelectorView;


/**
 *  This class is meant to serve as an example for how various contributions 
 *  are made to a perspective. Note that some of the extension point id's are
 *  referred to as API constants while others are hardcoded and may be subject 
 *  to change. 
 */
public class MetamathPerspective implements IPerspectiveFactory {

	private IPageLayout factory;

	public MetamathPerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout factory) {
		this.factory = factory;
		addViews();
		addActionSets();
		addNewWizardShortcuts();
		addPerspectiveShortcuts();
		addViewShortcuts();
		addShowInParts();
	}

	private void addViews() {
		// Creates the overall folder layout. 
		// Note that each new Folder uses a percentage of the remaining EditorArea.
		
//		IFolderLayout proofAsst =
//			factory.createFolder(
//				"proofAsst", //NON-NLS-1
//				IPageLayout.BOTTOM,
//				0.70f,
//				factory.getEditorArea());
//		proofAsst.addPlaceholder(ProofAssistantEditor.EDITOR_ID);
//		

		IFolderLayout right =
			factory.createFolder(
				"topRight", //NON-NLS-1
				IPageLayout.RIGHT,
				0.80f,
				factory.getEditorArea());
		right.addView(ProofExplorerView.VIEW_ID);
		right.addPlaceholder(IPageLayout.ID_OUTLINE);


		IFolderLayout bottom =
			factory.createFolder(
				"bottomRight", //NON-NLS-1
				IPageLayout.BOTTOM,
				0.60f,
				factory.getEditorArea());
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView(StepSelectorView.VIEW_ID);
		bottom.addPlaceholder(ProofBrowserView.VIEW_ID);
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		bottom.addPlaceholder(WebBrowserView.WEB_BROWSER_VIEW_ID);
		bottom.addPlaceholder(MathView.VIEW_ID);
		bottom.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		
		IFolderLayout topLeft =
			factory.createFolder(
				"topLeft", //NON-NLS-1
				IPageLayout.LEFT,
				0.20f,
				factory.getEditorArea());
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
	}

	private void addActionSets() {
		factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); //NON-NLS-1
	}

	private void addPerspectiveShortcuts() {
		factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //NON-NLS-1
	}

	private void addNewWizardShortcuts() {
		factory.addNewWizardShortcut("org.tirix.emetamath.wizards.MetamathNewProjectWizard");//NON-NLS-1
		factory.addNewWizardShortcut("org.tirix.emetamath.wizards.MetamathNewFileWizard");//NON-NLS-1
		factory.addNewWizardShortcut("org.tirix.emetamath.wizards.ProofNewFileWizard");//NON-NLS-1
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//NON-NLS-1
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//NON-NLS-1
	}

	private void addViewShortcuts() {
		factory.addShowViewShortcut(ProofExplorerView.VIEW_ID);
		factory.addShowViewShortcut(ProofBrowserView.VIEW_ID);
		factory.addShowViewShortcut(StepSelectorView.VIEW_ID);
		factory.addShowViewShortcut("org.eclipse.ui.navigator.ProjectExplorer");
		factory.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		factory.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		factory.addShowViewShortcut(MathView.VIEW_ID);
		factory.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		//factory.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}

	private void addShowInParts() {
		factory.addShowViewShortcut(ProofExplorerView.VIEW_ID);
		factory.addShowViewShortcut(ProofBrowserView.VIEW_ID);
		factory.addShowViewShortcut(WebBrowserView.WEB_BROWSER_VIEW_ID);
		factory.addShowViewShortcut(MathView.VIEW_ID);
		//factory.addShowInPart(IPageLayout.ID_OUTLINE);
	}
}

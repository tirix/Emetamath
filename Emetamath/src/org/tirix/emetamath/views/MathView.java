package org.tirix.emetamath.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import mmj.lang.Formula;
import mmj.lang.ParseNode;
import mmj.lang.ParseTree;
import mmj.lang.Stmt;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.mmj.TypeSetting;

public class MathView extends ViewPart implements IShowInTarget {
	public static final String VIEW_ID = "org.tirix.emetamath.views.MathView";
	Browser fBrowser;
	String fInternalFileUrl;
	File fFile = null;

	@Override
	public void createPartControl(Composite parent) {
		fBrowser = new Browser(parent, SWT.NONE);
				
		Display display= getSite().getShell().getDisplay();
		fBrowser.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		fBrowser.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		setText(MathMLPrinter.test());
	}

	public void setText(String html) {
		try {
			updateFile(html);
			fBrowser.setUrl(fInternalFileUrl);
			fBrowser.refresh();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setFocus() {
		fBrowser.setFocus();
	}

	private void updateFile(String str) throws IOException {
		if(fFile == null) {
			fFile = Activator.getDefault().getStateLocation().append("mathview.xhtml").toFile();
			if (fFile.exists()) {
				// has not been deleted on previous shutdown
				fFile.delete();
			}
			fInternalFileUrl = fFile.toURI().toURL().toString();
		}
		FileWriter fos = new FileWriter(fFile);
		fos.write(str);
		fos.close();
	}
	
	
	public void dispose() {
		if (fFile != null) {
			fFile.delete();
		}
		fFile = null;
	}

	@Override
	public boolean show(ShowInContext context) {
		MetamathProjectNature nature = MetamathProjectNature.getNature(context.getInput()); //()Platform.getAdapterManager().getAdapter(context.getInput(), MetamathProjectNature.class);
		ISelection selection= context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			Object selectedObject = ssel.getFirstElement();
			if(selectedObject instanceof Stmt) {
				TypeSetting typeSetting = nature.getMathMLTypeSetting();
				if(typeSetting != null) setText(typeSetting.format((Stmt)selectedObject));
				return true;
			}
		}
		return false;
	}
}

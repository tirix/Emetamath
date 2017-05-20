/**
 * 
 */
package org.tirix.emetamath.nature;

import mmj.lang.Stmt;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

public final class ShowInAdapterFactory implements IAdapterFactory {
	private Class[] ADAPTER_LIST = new Class[] { IShowInTarget.class };

	@Override
	public Object getAdapter(final Object adaptableObject, Class adapterType) {
		if(adaptableObject instanceof WebBrowserView) return new IShowInTarget() {
			@Override
			public boolean show(ShowInContext context) {
				try {
					MetamathProjectNature nature = MetamathProjectNature.getNature(context.getInput());
					String stmtLabel = ((Stmt)((IStructuredSelection)context.getSelection()).getFirstElement()).getLabel();
					String baseURL = "http://us.metamath.org/mpegif/";
					if(nature != null) baseURL = nature.getWebExplorerURL();
					String url = baseURL+stmtLabel+".html"; // TODO use a project property!
					((WebBrowserView)adaptableObject).setURL(url); 
					return true;
				}
				catch(ClassCastException e) {
					return false;
					}
			}};
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}
}
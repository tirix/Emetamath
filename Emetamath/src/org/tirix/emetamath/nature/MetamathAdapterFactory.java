package org.tirix.emetamath.nature;

import mmj.pa.StepSelectorStore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.views.StepSelectorView;

public class MetamathAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if(MetamathProjectNature.class.equals(adapterType) && adaptableObject instanceof FileEditorInput) 
			return MetamathProjectNature.getNature(adaptableObject);  
		if(MetamathProjectNature.class.equals(adapterType) && adaptableObject instanceof IResource) 
			return MetamathProjectNature.getNature((IResource)adaptableObject);  
		if(MetamathProjectNature.class.equals(adapterType) && adaptableObject instanceof IAdaptable) 
			return ((IAdaptable)adaptableObject).getAdapter(adapterType);  
		if(StepSelectorStore.class.equals(adapterType) && adaptableObject instanceof StepSelectorView) 
			return ((StepSelectorView)adaptableObject).getStore();  
		return null;
	}

	private Class[] ADAPTER_LIST = new Class[] { MetamathProjectNature.class, StepSelectorStore.class };

	@Override
	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

}

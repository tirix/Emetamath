package org.tirix.emetamath.editors;


import java.util.ArrayList;

import mmj.lang.BookManager;
import mmj.lang.Chapter;
import mmj.lang.LangConstants;
import mmj.lang.LogicalSystem;
import mmj.lang.MObj;
import mmj.lang.Section;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.part.FileEditorInput;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

public class MMContentProvider implements ITreeContentProvider, SystemLoadListener {
	MetamathProjectNature nature;
	MObj[][] sectionMObjArray;

	public void setNature(MetamathProjectNature nature) {
		this.nature = nature;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(newInput != null) {
			nature = MetamathProjectNature.getNature(newInput);
			if(nature != null) nature.addSystemLoadListener(this);
		}
	}

	public Object[] getElements(Object inputElement) {
		if(nature == null) {
			MetamathProjectNature nature = MetamathProjectNature.getNature(inputElement);
			if(nature == null) return new Object[] { "Could not load nature" };
		}
		BookManager bookManager = nature.getBookManager();
		if(bookManager == null) return new Object[] { "File not parsed yet" }; 
		return bookManager.getChapterList().toArray();
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}

	public Object[] getChildren(Object element) {
		if(element instanceof Chapter) {
			ArrayList<Section> children = new ArrayList<Section>();
			for(int i=0; i<((Chapter)element).getSectionList().size() / LangConstants.SECTION_NBR_CATEGORIES;i++)
				children.add(((Chapter)element).getSectionList().get(i * LangConstants.SECTION_NBR_CATEGORIES));
			return children.toArray(); 
			//return ((Chapter)element).getSectionList().toArray();
		}
		if(element instanceof Section) {
			ArrayList<MObj> children = new ArrayList<MObj>();
			for(int i=0; i<LangConstants.SECTION_NBR_CATEGORIES;i++)
				if(sectionMObjArray[((Section)element).getSectionNbr()+i-1] != null) 
					for(MObj obj:sectionMObjArray[((Section)element).getSectionNbr()+i-1])
						children.add(obj);
			return children.toArray(); 
			//return sectionMObjArray[((Section)element).getSectionNbr()-1];
		}
		return null;
	}

	public Object getParent(Object element) {
		try {
			if(element instanceof Chapter) {
				// TODO implement ?
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Trying to get parent of Chapter : Need to find the root FileEditorInput"));
				//return nature.getMainFile();
			}
			if(element instanceof Section) {
				return ((Section)element).getSectionChapter();
			}
			if(element instanceof MObj) {
				int sectionNbr = ((MObj)element).getSectionNbr();
				sectionNbr -= ((sectionNbr - 1) % LangConstants.SECTION_NBR_CATEGORIES);
				return nature.getBookManager().getSection(sectionNbr);
			}
		} catch (CoreException e) {
			//e.printStackTrace();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if(element instanceof Chapter) {
			return !((Chapter)element).getSectionList().isEmpty();
		}
		if(element instanceof Section) {
			int count = 0;
			for(int i=0; i<LangConstants.SECTION_NBR_CATEGORIES;i++)
				if(sectionMObjArray[((Section)element).getSectionNbr()+i-1] != null) 
					count +=sectionMObjArray[((Section)element).getSectionNbr()+i-1].length;
			return count > 0; 
		}
		return false;
	}

	@Override
	public void systemLoaded() {
		sectionMObjArray = nature.getBookManager().getSectionMObjArray(nature.getLogicalSystem());
	}
}

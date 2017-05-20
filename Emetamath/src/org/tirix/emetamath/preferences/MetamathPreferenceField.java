package org.tirix.emetamath.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import mmj.pa.Setting;
import mmj.pa.ProofAsstException;
import mmj.pa.SessionStore.OnChangeListener;

public abstract class MetamathPreferenceField<T> implements IPropertyChangeListener {
	Setting<T> setting;
	String name;
	String labelText;
	FieldEditor fieldEditor;
	
	public MetamathPreferenceField(String name, String labelText, Setting<T> setting) {
		this.name = name;
		this.labelText = labelText;
		this.setting = setting;
	}
	
//	public static MetamathPreferenceField createField(String name, String labelText, Setting<?> setting) {
//		String typeName = setting.getClass().getTypeParameters()[0].getName();
//		if(typeName.equals("Integer")) return new Integer().init(name, labelText, setting);
//		if(typeName.equals("Boolean")) return new Boolean().init(name, labelText, setting);
//	}
	
	public FieldEditor getFieldEditor(Composite parent) {
		if(fieldEditor == null) {
			fieldEditor = createFieldEditor(parent);
			fieldEditor.setPropertyChangeListener(this);
		}
		return fieldEditor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		setting.set((T)event.getNewValue());
	}
	
	abstract FieldEditor createFieldEditor(Composite parent);
	
	public static class Integer extends MetamathPreferenceField<java.lang.Integer> {
		public Integer(String name, String labelText, Setting<java.lang.Integer> currFormatNbr) {
			super(name, labelText, currFormatNbr);
		}

		@Override
		FieldEditor createFieldEditor(Composite parent) {
			return new IntegerFieldEditor(name, labelText, parent) {
				@Override
			    protected boolean doCheckState() {
					Text text = getTextControl();
			        String numberString = text.getText();
		            int number = java.lang.Integer.valueOf(numberString).intValue();
		        	try {
						setting.validate(number);
						return true;
					} catch (ProofAsstException e) {
						setErrorMessage(e.getMessage());
						return false;
					}
			    }
			};
		}
	}

	public static class Boolean extends MetamathPreferenceField<java.lang.Boolean> {
		public Boolean(String name, String labelText, Setting<java.lang.Boolean> setting) {
			super(name, labelText, setting);
		}

		@Override
		FieldEditor createFieldEditor(Composite parent) {
			return new BooleanFieldEditor(name, labelText, parent);
		}
	}
}

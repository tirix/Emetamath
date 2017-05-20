package org.tirix.mmj;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import mmj.lang.Axiom;
import mmj.lang.LangException;
import mmj.lang.Stmt;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


public class MathMLTypeSetting extends TypeSetting {
	Map<Axiom, List<MathMLTypeSettingNode>> typeSettings;
	
	@Override
	public void setData(String data) throws LangException {
		// TODO Auto-generated method stub
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(new MathMLTypeSettingParser());
			reader.parse(new InputSource(new StringReader(data)));
		}
		catch(Exception e) {
			LangException ex = new LangException(e.getClass() + " : " + e.getMessage());
			ex.setStackTrace(e.getStackTrace());
			throw ex;
		}
	}

	@Override
	public String format(Stmt stmt) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public class MathMLTypeSettingParser extends DefaultHandler {
		@Override
		public void startDocument() throws SAXException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endDocument() throws SAXException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startElement(String arg0, String arg1, String arg2,
				Attributes arg3) throws SAXException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endElement(String arg0, String arg1, String arg2)
				throws SAXException {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static class MathMLTypeSettingNode {
		
	}
}

package org.tirix.mmj;

import java.io.StringReader;
import java.util.Hashtable;
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
			RuntimeException ex = new RuntimeException(e.getClass() + " : " + e.getMessage());
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
			typeSettings = new Hashtable<Axiom, List<MathMLTypeSettingNode>>();
		}

		@Override
		public void endDocument() throws SAXException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startElement(String namespaceURI, String localName, String qualifiedName,
				Attributes attr) throws SAXException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qualifiedName)
				throws SAXException {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static class MathMLTypeSettingNode {
		
	}
	
}

/*

<typesetting>
	<stmt name="wph"><mi>&phi;</mi></stmt>
	<stmt name="cA"><mi>A</mi></stmt>
	<stmt name="cB"><mi>B</mi></stmt>
	<stmt name="cC"><mi>C</mi></stmt>
	<stmt name="caddc"><mo>+</mo></stmt>

	<stmt name="wi"><mrow>#ph# &rArr; #ps#</mrow></stmt>
	<stmt name="weq"><mrow>#x#<mo>=</mo>#y#</mrow></stmt>
	<stmt name="wbr"><mrow>#A# #R# #B#</mrow></stmt>
	<stmt name="cdit">
  		<mrow>
	  		<munderover>
	    			<mo>&int;</mo>
			    <mn>#A#</mn>
	    			<mn>#B#</mn>
	    		</munderover>
			#C#
	    		<mi>d</mi>#x#
    		</mrow>
	</stmt>
</typesetting>


 */

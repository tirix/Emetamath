package org.tirix.mmj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import mmj.lang.LangException;
import mmj.lang.Stmt;

public abstract class TypeSetting {
	public abstract void setData(String data) throws LangException;
	public abstract String format(Stmt stmt);
	
	public void setData(Reader r) throws IOException, LangException {
	    BufferedReader reader = new BufferedReader(r);
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) sb.append(line + "\n");
	    r.close();
	    setData(sb.toString());
	}
}

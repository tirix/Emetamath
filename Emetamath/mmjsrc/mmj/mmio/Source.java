package mmj.mmio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

/**
 * Serves as an abstraction for a file in the file system.
 * It is used to feedback to the user about the position of an error.
 * 
 * At the same time, it is also a factory to create new SourceId 
 * for include statements.
 * 
 * @author Thierry Arnoux
 */
public interface Source {
	 /** 
	  * Returns a {@link java.io.Reader} for this source
	  * 
      * (may be StringReader or BufferedReader but PushbackReader
      *  and LineNumberReader are not helpful choices.)
      */
	Reader createReader() throws FileNotFoundException;

	/**
	 * Returns the full contents of this source
	 * @return
	 * @throws IOException 
	 */
	String getContents() throws IOException;
	
	/**
	 * Substitutes the given text for the specified document range.
	 *
	 * @param offset the document offset
	 * @param length the length of the specified range
	 * @param text the substitution text
	 * @exception IOException if the offset is invalid in this document
	 */
	void replace(int offset, int length, String text) throws IOException;

	 /** Returns the size of this source */
	int getSize() throws FileNotFoundException;

	/** Returns an unique string defining this source */
	String getUniqueId() throws IOException; 
	
	/** Returns a new Source object using the provided file name. 
	 * This is used for include statements */
	Source createSourceId(String fileName);
	
	/**
	 * Default implementation of Source using {@link java.io.File}
	 * Used for Metamath file sources
	 */
	public static class FileSource implements Source {
		final File file;
		
		public FileSource(File path, String fileName) {
			this.file = new File(path, fileName);
		}
		
		public FileSource(File f) {
			this.file = f;
		}
		
		@Override
		public String getContents() {
			throw new UnsupportedOperationException("Getting full contents of file source is not implemented - normally not used!");
		}
		
		@Override
		public void replace(int offset, int length, String text) {
			throw new UnsupportedOperationException("Replacing contents of file source is not implemented - normally not used!");
		}
		
		@Override
		public String getUniqueId() throws IOException {
			return file.getCanonicalPath();
		}
		
		@Override
		public String toString() {
			return file.getName();
		}

		@Override
		public Reader createReader() throws FileNotFoundException {
			return new BufferedReader(
			    new InputStreamReader(
			        new FileInputStream(file)
			        ),
			    MMIOConstants.READER_BUFFER_SIZE
			    );
    		}

		@Override
		public int getSize() throws FileNotFoundException {
			return (int)file.length();
		}

		@Override
		public Source createSourceId(final String fileName) {
	        File f = new File(fileName);
	        if (!f.isAbsolute()) f = new File(file.getPath(), fileName);
			return new FileSource(f);
		}
	}

	/**
	 * Default implementation of Source using {@link java.lang.String} as contents
	 * Used for Proof text sources
	 */
	public static class StringSource implements Source {
		String contents;
		final String name;
		
		public StringSource(String contents, String name) {
			this.contents = contents;
			this.name = name;
		}
		
		@Override
		public String getContents() {
			return contents;
		}
		
		@Override
		public void replace(int offset, int length, String text) {
			contents = contents.substring(0, offset) + text + contents.substring(offset + length);
		}
		
		@Override
		public String getUniqueId() throws IOException {
			return name;
		}
		
		@Override
		public String toString() {
			return "\""+contents+"\"";
		}

		@Override
		public Reader createReader() {
			return new StringReader(contents);
    		}

		@Override
		public int getSize() {
			return contents.length();
		}

		@Override
		public Source createSourceId(String fileName) {
			throw new UnsupportedOperationException("Cannot create source from a string source!");
		}
	}
}

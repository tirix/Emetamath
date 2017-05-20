package mmj.mmio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

import mmj.mmio.IncludeFile.ReaderProvider;

/**
 * Used to store a position in the Metamath source files.
 */
public class SourcePosition {

    public Object sourceId		  = null;
	public int lineNbr           = -1;
    public int columnNbr         = -1;
    public int charStartNbr      = -1;
    public int charEndNbr        = -1;

    public int symbolNbr		 = -1;
    
    /**
     * Default contructor
     */
    public SourcePosition() {
    	
    }

    /**
     * Contructor, <code>SourcePosition</code> with
     * file name, line number, column number.
     *
     * @param   sourceId      String identifying source
     * @param   lineNbr       line number assigned
     * @param   columnNbr     column number assigned
     * @param   charStartNbr  character number of the first character
     * @param   charEndNbr    character number of the last character
     */
    public SourcePosition(Object  sourceId,
    		long    lineNbr,
    		long    columnNbr,
    		long    charStartNbr,
    		long    charEndNbr) {
        this.sourceId			  = sourceId;
        this.lineNbr              = (int)lineNbr;
        this.columnNbr            = (int)columnNbr;
        this.charStartNbr         = (int)charStartNbr;
        this.charEndNbr           = (int)charEndNbr;
    }
    
    /**
     * Contructor, <code>SourcePosition</code> with
     * previous SourcePosition and symbol number inside the formula.
     *
     * @param   sourceId      String identifying source
     * @param   lineNbr       line number assigned
     * @param   columnNbr     column number assigned
     * @param   charStartNbr  character number of the first character
     * @param   charEndNbr    character number of the last character
     */
    public SourcePosition(
    		SourcePosition position,
    		int symbolNbr) {
        this.sourceId			  = position.sourceId;
        this.lineNbr              = position.lineNbr;
        this.columnNbr            = position.columnNbr;
        this.charStartNbr         = position.charStartNbr;
        this.charEndNbr           = position.charEndNbr;
        this.symbolNbr			  = symbolNbr;
    }
    
    public int getLength() {
    	return charEndNbr - charStartNbr;
    }
    
    public void refinePosition(ReaderProvider provider) {
    	if(symbolNbr == -1) return;
    	try {
			Reader reader = provider.createReader((provider.getFileName(sourceId)));
			Tokenizer tokenizer = new Tokenizer(reader, sourceId, charStartNbr);
			StringBuffer strBuf = new StringBuffer();
			
			// skip the 2 first tokens (label and $a, $e or $p keyword), plus the given number of symbols
			for(int i=0;i<2+symbolNbr;i++) {
				strBuf.setLength(0);
				charStartNbr += tokenizer.getToken(strBuf, 0);
			}
			charStartNbr = (int)tokenizer.getLastCharNbr();
			charEndNbr = (int)tokenizer.getCurrentCharNbr();
		} catch (IOException e) {
			; // ignore
		}
		symbolNbr = -1;
    }
}

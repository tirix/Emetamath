package mmj.mmio;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/**
 * Used to store a position in the Metamath source files.
 */
public class SourcePosition {

    final public Source source;
    final public int lineNbr;
    final public int columnNbr;
    final public int charStartNbr;
    final public int charEndNbr;

    public int symbolNbr = -1;
    
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
    public SourcePosition(Source sourceId,
    		long    lineNbr,
    		long    columnNbr,
    		long    charStartNbr,
    		long    charEndNbr) {
        this.source			  = sourceId;
        this.lineNbr              = (int)lineNbr;
        this.columnNbr            = (int)columnNbr;
        this.charStartNbr         = (int)charStartNbr;
        this.charEndNbr           = (int)charEndNbr;
    }
    
    /**
     * Contructor, <code>SourcePosition</code> with
     * file name, line number, column number.
     *
     * @param   sourceId      String identifying source
     * @param   lineNbr       line number assigned
     * @param   columnNbr     column number assigned
     * @param	length		  length of the position
     */
    public SourcePosition(final Source sourceId,
    		long    lineNbr,
    		long    columnNbr,
    		long    length) {
        this.source			  = sourceId;
        this.lineNbr              = (int)lineNbr;
        this.columnNbr            = (int)columnNbr;
        this.charStartNbr         = -1;
        this.charEndNbr           = (int)length;
    }
    
    /**
     * Contructor, <code>SourcePosition</code> with
     * previous SourcePosition and symbol number inside the formula.
     *
     * @param   source      String identifying source
     * @param   lineNbr       line number assigned
     * @param   columnNbr     column number assigned
     * @param   charStartNbr  character number of the first character
     * @param   charEndNbr    character number of the last character
     */
    public SourcePosition(
    		SourcePosition position,
    		int symbolNbr) {
        this.source			  = position.source;
        this.lineNbr              = position.lineNbr;
        this.columnNbr            = position.columnNbr;
        this.charStartNbr         = position.charStartNbr;
        this.charEndNbr           = position.charEndNbr;
        this.symbolNbr			  = symbolNbr;
    }
    
    public int getLength() {
    	if(charStartNbr == -1) return charEndNbr;
    	return charEndNbr - charStartNbr;
    }
    
	/**
	 * Some positions are given inside a formula, and the exact char numbers have not been stored.
	 * The position therefore needs to be refined to find back the exact char numbers.
	 */
    public SourcePosition refinePosition() {
    	if(symbolNbr == -1) return this;
    	try {
			Tokenizer tokenizer = new Tokenizer(source, charStartNbr);
			StringBuilder strBuf = new StringBuilder();
			int charStartNbr = this.charStartNbr;
			int charEndNbr = this.charEndNbr;
			
			// skip the 2 first tokens (label and $a, $e or $p keyword), plus the given number of symbols
			for(int i=0;i<2+symbolNbr;i++) {
				strBuf.setLength(0);
				charStartNbr += tokenizer.getToken(strBuf, 0);
			}
			charStartNbr = (int)tokenizer.getLastCharNbr();
			charEndNbr = (int)tokenizer.getCurrentCharNbr();
			tokenizer.close();
			return new SourcePosition(source, lineNbr, -1, charStartNbr, charEndNbr);
    	} catch (IOException e) {
			return this; // ignore
		} catch (MMIOException e) {
			return this; // ignore
		}
    }

    /**
     * Compute the charNbr position when it is not known
     * Hook for Prooftexts, which are usually small enough
     * @return
     */
	public int getCharStartNbr() {
		if(charStartNbr != -1) return charStartNbr;
		try {
			if(source.getSize() > 100000) return 0; // This is not intended for big files like set.mm...
			LineNumberReader r = new LineNumberReader(source.createReader());
			int charNbr = 0;
			while(r.ready() && r.getLineNumber() < lineNbr - 1) charNbr += r.readLine().length() + 1;
			charNbr += columnNbr - 1;
			//System.out.println("Line "+lineNbr+" col "+columnNbr+" => "+charNbr+" charEnd="+charEndNbr);
			return charNbr;
		} catch(IOException e) {
			return 0;
		}
	}

	/**
	 * Returns the position immediately afterwards this one
	 * 
	 * @return a new position object
	 */
	public SourcePosition after() {
		return new SourcePosition(source, -1, -1, charStartNbr + charEndNbr, 0);
	}
	
	@Override
	public String toString() {
		return "Position : "+source+" @ "+charStartNbr+" ~ "+charEndNbr+" line="+lineNbr+" col="+columnNbr+" ";
	}
}

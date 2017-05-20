package mmj.mmio;

/**
 * Used to store a position in the Metamath source files.
 */
public class SourcePosition {

    public Object sourceId		  = null;
	public int lineNbr           = -1;
    public int columnNbr         = -1;
    public int charStartNbr      = -1;
    public int charEndNbr        = -1;

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
    
    public int getLength() {
    	return charEndNbr - charStartNbr;
    }
}

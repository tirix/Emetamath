package mmj.mmio;

import java.util.ArrayList;
import java.util.List;

import mmj.lang.LangConstants;
import mmj.lang.LangException;

public class BlockList {
    private final StringBuilder blocks = new StringBuilder();
    private final List<SourcePosition> positions = new ArrayList<SourcePosition>();
    public boolean marked = false;
    private int index = 0;

    public int getIndex() {
        return index;
    }

    public void addBlock(final String block, final SourcePosition position) {
        blocks.append(block);
        positions.add(position);
    }

    public int getNext(final String theoremLabel) throws LangException {
        if (index == blocks.length())
            return -1;

        final int blockLen = blocks.length();

        int decompressNbr = 0;

        while (true) {
            if (index >= blockLen)
                throw new LangException(
                    LangConstants.ERRMSG_COMPRESS_PREMATURE_END, theoremLabel).setPosition(getIndexPosition());

            final char nextChar = blocks.charAt(index++);
            if (nextChar >= LangConstants.COMPRESS_VALID_CHARS.length)
                throw new LangException(LangConstants.ERRMSG_COMPRESS_NOT_ASCII,
                    theoremLabel, index, nextChar).setPosition(getIndexPosition());

            // translate 'A' to 0, 'B' to 1, etc. (1 is added to
            // 'A' thru 'Z' later -- curiously but effectively :)
            final byte nextCharCode = LangConstants.COMPRESS_VALID_CHARS[nextChar];

            if (nextCharCode == LangConstants.COMPRESS_ERROR_CHAR_VALUE)
                throw new LangException(LangConstants.ERRMSG_COMPRESS_BAD_CHAR,
                    theoremLabel, index, nextChar).setPosition(getIndexPosition());

            if (nextCharCode == LangConstants.COMPRESS_UNKNOWN_CHAR_VALUE) {
                if (decompressNbr > 0)
                    throw new LangException(
                        LangConstants.ERRMSG_COMPRESS_BAD_UNK, theoremLabel,
                        index).setPosition(getIndexPosition());
                return 0;
            }

            if (nextCharCode == LangConstants.COMPRESS_REPEAT_CHAR_VALUE)
                throw new LangException(LangConstants.ERRMSG_COMPRESS_BAD_RPT,
                    theoremLabel, index).setPosition(getIndexPosition());

            if (nextCharCode >= LangConstants.COMPRESS_LOW_BASE) {
                decompressNbr = decompressNbr * LangConstants.COMPRESS_HIGH_BASE
                    + nextCharCode;
                continue;
            }

            // else...
            decompressNbr += nextCharCode + 1; // 'A' = 1 etc

            if (marked = index < blockLen
                && blocks.charAt(index) == LangConstants.COMPRESS_REPEAT_CHAR)
                index++;
            return decompressNbr;
        }
    }

    public boolean isEmpty() {
        return blocks.length() == 0;
    }

	/**
	 * Returns the position corresponding to the current index within the block.
	 * @return
	 */
    public SourcePosition getIndexPosition() {
		if(positions.size() == 0) return null;
		SourcePosition position = positions.get(0);
		int length = 0, i = 1;
		while(index > length + position.getLength() && i < positions.size()) { 
			length += position.getLength();
			position = positions.get(i++); 
		}
		if(index - length > position.getLength()) length = index - position.getLength(); // handling errors nicely, as this is to display them
		return new SourcePosition(position.source, position.lineNbr, position.columnNbr + (index - length), position.charStartNbr + (index - length), position.charStartNbr + (index - length) + 1 );
	}

    @Override
    public String toString() {
        return blocks.toString();
    }
}

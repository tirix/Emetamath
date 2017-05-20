package org.tirix.emetamath.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcherExtension;

/**
 * Bracket Pair Matching for Metamath
 * 
 * Metamath matching pairs can be made of two or more characters 
 * (e.g. <code><. A , B >.</code> or <code>$a ... $.</code>), so the default implementation
 * <code>DefaultCharacterPairMatcher</code> cannot be used.
 * 
 * @author Thierry
 */

public class MetamathPairMatcher implements ICharacterPairMatcher, ICharacterPairMatcherExtension {
	final String[] brackets;
	final String bracketChars;
	Position startPosition, endPosition;
	int anchor;
	
	public MetamathPairMatcher(final String[] brackets) {
		this.brackets = brackets;
		StringBuffer bracketCharBuffer = new StringBuffer();
		for(String bracket:brackets) bracketCharBuffer.append(bracket);
		bracketChars = bracketCharBuffer.toString();
	}

	@Override
	public IRegion match(IDocument document, int offset) {
		return match(document, offset, 0);
	}

	/**
	 * If the caracter located just before or at the offset provided is part of a bracket,
	 * finds the matching bracket and returns it.
	 */
	@Override
	public IRegion match(IDocument document, int offset, int length) {
		if (document == null || offset < 0 || offset > document.getLength() || Math.abs(length) > 1)
			return null;
//		System.out.println("match "+offset+" "+length);

		if(length < 0) { length = -length; offset -= length; }
		Position position = new Position(offset, length);
		
		try {
			String start = getWord(document, position);
			if(start == null) return null;

			Pair pair = new Pair();
			buildPair(start, pair);
			if(!pair.match) return null;
			
			Position startPosition = position;
//			System.out.println("  > start="+start+" end="+end+" forward="+isForward);
			
			anchor= pair.forward ? ICharacterPairMatcher.LEFT : ICharacterPairMatcher.RIGHT;

			if(!findMatchingPeer(document, position, pair)) return null;
			this.startPosition = startPosition;
			this.endPosition = position;
			int regionOffset = Math.min(offset, position.offset);
			return new Region(regionOffset, Math.max(offset+length, position.offset + position.length) - regionOffset);
			} 
		catch(BadLocationException e) {
			System.out.println("Bad Location : "+position+" in document "+document+" of length "+document.getLength());
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * Finds the position of the matching bracket, 
	 * taking nesting into account
	 * 
	 * The position object is updated with the position of the match.
	 * 
	 * @param document the document to work on
	 * @param position the position of the start bracket
	 * @param pair the pair to be searched (start, end and direction)
	 * 
	 * @return whether a match was found
	 * @throws BadLocationException
	 */
	private boolean findMatchingPeer(IDocument document, Position position, Pair pair) throws BadLocationException {
		int nestingLevel = 0;
		while((pair.forward && position.offset < document.getLength() - 1) 
				|| (!pair.forward && position.offset > 0)) {
			String word = nextWord(document, position, pair.forward);
//				System.out.println("  @ "+position.offset+" "+position.length+" : "+word+" level "+nestingLevel);
			if(word.equals(pair.end)) {
				if(nestingLevel == 0) return true;
				nestingLevel--;
			}
			if(word.equals(pair.start))
				nestingLevel++;
		}
		return false;
	}

	/**
	 * Finds a matching bracket for the given string
	 * 
	 * @param start the starting bracket
	 * @param pair a pair object to be used
	 * 
	 * @return a Pair object describing the bracket pair and the direction to go
	 */
	private void buildPair(String start, Pair pair) {
		int index = 0;
		for(String bracket:brackets) {
			if(bracket.equals(start)) break;
			index++;
		}
		if(index == brackets.length) {
			pair.match = false;
		} else {
			pair.match = true;
			pair.forward = index % 2 == 0;
			pair.start = start;
			pair.end = brackets[index + 1 - 2 * (index % 2)];
		}
	}

	/**
	 * Gets the word at the given position in the document, or just before
	 */
	private String getWord(IDocument document, Position position) throws BadLocationException {
		if(!MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset))) {
			if(position.offset-- < 0) return null;
			if(!MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset))) {
				return null;
			}
		}

		IRegion region = MMRegionProvider.getWord(document, position.offset);
		position.offset = region.getOffset();
		position.length = region.getLength();
		return document.get(position.offset, position.length);
	}

	/**
	 * Gets the next word at the given position in the document, looking forward or backward
	 */
	public String nextWord(IDocument document, Position position, boolean forward) throws BadLocationException {
		if(forward) {
			int end = document.getLength();
			position.offset += position.length;
			position.length = 0;
			while(position.offset < end && !MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset))) position.offset++;
			while(position.offset < end &&  MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset+position.length))) position.length++;
		}
		else {
			position.offset--;
			position.length = 0;
			while(position.offset >= 0 && !MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset))) position.offset--;
			while(position.offset >= 0 &&  MMRegionProvider.DETECTOR.isWordPart(document.getChar(position.offset))) { position.offset--; position.length++; }
			position.offset++;
		}
		return document.get(position.offset, position.length);
	}
	
	@Override
	public IRegion findEnclosingPeerCharacters(IDocument document, int offset, int length) {
		if (document == null || offset < 0 || offset > document.getLength() || Math.abs(length) > 1)
			return null;
		if(length < 0) { length = -length; offset -= length; }

		Pair pair = new Pair();
		Position position = null;

		try {
			Position position1 = new Position(offset, length);
			String word = getWord(document, position1);
			if(word == null) word = nextWord(document, position1, true);
//			Position position2 = new Position(offset+length, 0);
//			
//			do {
//				word = nextWord(document, position1, false);
//				buildPair(word, pair);
//			} while(!pair.match);
//			position = position1;
//			if(pair.forward && findMatchingPeer(document, position, pair)) {
//				if(position.offset < offset && position1.o)
//			// TODO
//			}
//			
//			
//			do {
//				word = nextWord(document, position2, true);
//				buildPair(word, pair);
//			} while(!pair.match);
//			position = position2;
//			if(!pair.forward && findMatchingPeer(document, position, pair)) {
//
//			// TODO
//			}
//			
			
			return match(document, offset, length);
		} catch(BadLocationException e) {
			System.out.println("Bad Location : "+position);
			return null;
		}
	}

	@Override
	public boolean isMatchedChar(char ch) {
		return bracketChars.indexOf(ch) != -1;
	}

	@Override
	public boolean isMatchedChar(char ch, IDocument document, int offset) {
		try {
			IRegion region = MMRegionProvider.getWord(document, offset);
			String word = document.get(region.getOffset(), region.getLength());
			for(String bracket:brackets)
				if(bracket.equals(word)) return true;
			return false;
		} catch (BadLocationException e) {
			return false;
		}
	}

	@Override
	public boolean isRecomputationOfEnclosingPairRequired(IDocument document, IRegion currentSelection, IRegion previousSelection) {
		if(startPosition == null || endPosition == null) return true;
		
		// TODO The lazy way ;) - instead, we shall find out if anything between startPosition and endPosition was impacted
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void clear() {
		anchor= -1;
		startPosition = null;
		endPosition = null;
	}

	@Override
	public int getAnchor() {
		return anchor;
	}

	protected static class Pair {
		String start, end;
		boolean forward;
		boolean match;
	}
	
}

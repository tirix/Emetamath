package org.tirix.emetamath.nature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages dependencies for incremental builds 
 * In Metamath, "building" means parsing and verifying
 * @author Thierry
 */
public class DependencyManager<F> {
	private final List<Dependency<F>> dependencies = new ArrayList<Dependency<F>>();
    private final Map<F, Long> buildList = new HashMap<>();
    private F main = null;
    private F topBuildFile = null;
    private long topBuildFileOffset = 0;

	public void setMainFile(F main) {
		this.main = main;
	}

    /**
     * Registers a dependency between two Metamath files
     * @param includerFile the file including
     * @param includedFile the file being included
     */
	public void addDependency(final F includerFile, final F includedFile, final long offset) {
		Dependency<F> d = new Dependency<F>(includerFile, includedFile, offset);
		// TODO check for cyclic dependencies
		if(!dependencies.contains(d)) dependencies.add(d);
	}

	/**
	 * Clears the registered dependencies, starting from the given position.
	 * Used before rebuilding, since user might have removed/changed includes.
	 * @param file starting where dependencies have to be cleared
	 * @param offset the offset starting which dependencies are to be cleared
	 */
	public void clearDependenciesFrom(final F file, final long offset) {
		// dependencies are added in the order of parsing.
		boolean passed = false;
		for(int i=0;i<dependencies.size();i++) {
			Dependency<F> d = dependencies.get(i);
			if(d.includer.equals(file) && d.offset >= offset) passed = true;
			if(passed) {
				dependencies.remove(d);
				i--;
			}
		}
	}

	/**
	 * Returns the list of files to be built, as well as the offset from which the build shall start
	 * Note that this is not provided in any specific build order
	 */
	public Map<F, Long> getBuildList() {
		return buildList;
	}
	
	/**
	 * Returns the file to be built in case of partial build
	 */
	public F getTopBuildFile() {
		return topBuildFile;
	}

	/**
	 * Returns the offset in the top build file where to start build 
	 */
	public long getTopBuildFileOffset() {
		return topBuildFileOffset;
	}

	/**
	 * Clears the list of files to be built
	 */
	public void clearBuildList() {
		buildList.clear();
		topBuildFile = null;
		topBuildFileOffset = 0;
	}
	
	/**
	 * Adds one file to the build list
	 * 
	 * @param file the file to be rebuild
	 * @param offset the offset starting from which the file has to be rebuilt
	 * @return whether this file was already in the list
	 */
	private boolean addToBuildList(final F file, final long offset) {
		if(buildList.containsKey(file)) {
			long previousOffset = buildList.get(file);
			if(offset >= previousOffset) return true;
		}
		buildList.put(file, offset);
		return false;
	}
	
	/**
	 * Registers a change to a file.
	 * This adds this file to the build list, as well as all other files depending on this one
	 * @param file
	 */
	public void addChange(final F file, final long offset) {
		if(main == null) return;
		if(!in(file)) return;

		// the file itself shall be rebuilt
		if(addToBuildList(file, offset)) return;

		boolean wasIncluded = false;
		for(Dependency<F> dependency:dependencies) {
			// all files including this file shall be rebuilt, as if they had changed themselves, starting from the offset of the inclusion
			if(dependency.includee.equals(file))  {
				addChange(dependency.includer, dependency.offset); 
				wasIncluded = true;
			}
			
			// all files included by this file after the offset shall be rebuilt, as if they had changed themselves
			if(dependency.includer.equals(file) && dependency.offset >= offset) {
				addChange(dependency.includee, 0);	
			}
		}
		
		if(!wasIncluded && before(file, offset, topBuildFile, topBuildFileOffset)) {
			topBuildFile = file;
			topBuildFileOffset = offset;
		}
	}
	
	/**
	 * The metamath inclusion is a linear process - find out the earliest position out of the two positions provided
	 */
	private boolean in(F f) {
		if(main.equals(f)) return true;
		for(Dependency<F> d:dependencies)
			if(d.includer.equals(f) || d.includee.equals(f)) return true;
		return false;
	}

	/**
	 * The metamath inclusion is a linear process - find out the earliest position out of the two positions provided
	 */
	private boolean before(F f1, long o1, F f2, long o2) {
		if(f1.equals(f2)) return o1 < o2;
		if(f2 == null) return true;
		
		// the dependencies are added in order. Find out the first file included.
		for(Dependency<F> d:dependencies) {
			if(d.includer.equals(f1)) return true;
			if(d.includer.equals(f2)) return false;
		}
		throw new RuntimeException("Files not found in the dependency!");
	}
	
	/**
	 * Stores a dependency between File
	 * @author Thierry
	 *
	 * @param <F>
	 */
	private static class Dependency<F> {
		F includer, includee;
		long offset;

		public Dependency(F includerFile, F includedFile, long offset) {
			this.includer = includerFile;
			this.includee = includedFile;
			this.offset = offset;
		}

		@Override
		public int hashCode() {
			return includer.hashCode() ^ includee.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			@SuppressWarnings("unchecked")
			Dependency<F> d = (Dependency<F>)obj;
			return d.includer == includer && d.includee == includee;
		}
	}
}

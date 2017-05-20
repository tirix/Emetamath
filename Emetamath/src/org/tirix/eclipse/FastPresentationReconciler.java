package org.tirix.eclipse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentPartitioningChangedEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IDocumentPartitioningListenerExtension;
import org.eclipse.jface.text.IDocumentPartitioningListenerExtension2;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationReconcilerExtension;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;

/**
 * Faster implementation of <code>IPresentationReconciler</code>. 
 * This implementation applies for cases where the task performed by its 
 * presentation damagers and repairers is CPU intensive or the text document
 * to be reconciled is very large. This presentation reconciler
 * attempts to repair only the portion overlapping with the viewer's view port.
 * <p>
 * Usually, clients instantiate this class and configure it before using it.
 * </p>
 */
public class FastPresentationReconciler implements IPresentationReconciler, IPresentationReconcilerExtension {

	/** Prefix of the name of the position category for tracking damage regions. */
	protected final static String TRACKED_PARTITION= "__reconciler_tracked_partition"; //$NON-NLS-1$

	/** The map of presentation damagers. */
	private Map<String, IPresentationDamager> fDamagers;
	/** The map of presentation repairers. */
	private Map<String, IPresentationRepairer> fRepairers;
	/** The target viewer. */
	private ITextViewer fViewer;
	/** The internal listener. */
	private InternalListener fInternalListener= new InternalListener();
	/** The name of the position category to track damage regions. */
	private String fPositionCategory;
	/** The position updated for the damage regions' position category. */
	private IPositionUpdater fPositionUpdater;
	/** The positions representing the damage regions. */
	private TypedPosition fRememberedPosition;
	/** Flag indicating the receipt of a partitioning changed notification. */
	private boolean fDocumentPartitioningChanged= false;
	/** The range covering the changed partitioning. */
	private IRegion fChangedDocumentPartitions= null;
	/**
	 * The partitioning used by this presentation reconciler.
	 * @since 3.0
	 */
	private String fPartitioning;
	/** The view port currently visible in the text viewer */
	private IRegion fViewport;
	/** A list of damaged regions */
	private RegionList fDamagedRegions;
	
	/**
	 * Creates a new presentation reconciler. There are no damagers or repairers
	 * registered with this reconciler by default. The default partitioning
	 * <code>IDocumentExtension3.DEFAULT_PARTITIONING</code> is used.
	 */
	public FastPresentationReconciler() {
		super();
		fPartitioning= IDocumentExtension3.DEFAULT_PARTITIONING;
		fPositionCategory= TRACKED_PARTITION + hashCode();
		fPositionUpdater= new DefaultPositionUpdater(fPositionCategory);
		fDamagedRegions = new RegionList();
	}

	public void updateViewport() {
		int lineCount = fViewer.getTextWidget().getLineCount();
		try {
			int offset = fViewer.getTextWidget().getOffsetAtLine(Math.max(fViewer.getTopIndex()-1, 0));
			int end = fViewer.getTextWidget().getOffsetAtLine(Math.min(fViewer.getBottomIndex()+2, lineCount-1));
			this.fViewport = new Region(offset, end - offset);
		}
		catch(IllegalArgumentException e) {
			System.out.println("lineCount="+lineCount+" topIndex="+fViewer.getTopIndex()+" bottomIndex="+fViewer.getBottomIndex());
			e.printStackTrace();
		}
		if(fDamagedRegions.intersects(fViewport)) {
			processDamage(fViewport, fViewer.getDocument());
		}
	}

	/**
	 * Sets the document partitioning for this presentation reconciler.
	 *
	 * @param partitioning the document partitioning for this presentation reconciler.
	 * @since 3.0
	 */
	public void setDocumentPartitioning(String partitioning) {
		Assert.isNotNull(partitioning);
		fPartitioning= partitioning;
	}

	/*
	 * @see org.eclipse.jface.text.presentation.IPresentationReconcilerExtension#geDocumenttPartitioning()
	 * @since 3.0
	 */
	public String getDocumentPartitioning() {
		return fPartitioning;
	}

	/**
	 * Registers the given presentation damager for a particular content type.
	 * If there is already a damager registered for this type, the old damager
	 * is removed first.
	 *
	 * @param damager the presentation damager to register, or <code>null</code> to remove an existing one
	 * @param contentType the content type under which to register
	 */
	public void setDamager(IPresentationDamager damager, String contentType) {

		Assert.isNotNull(contentType);

		if (fDamagers == null)
			fDamagers= new HashMap<String, IPresentationDamager>();

		if (damager == null)
			fDamagers.remove(contentType);
		else
			fDamagers.put(contentType, damager);
	}

	/**
	 * Registers the given presentation repairer for a particular content type.
	 * If there is already a repairer registered for this type, the old repairer
	 * is removed first.
	 *
	 * @param repairer the presentation repairer to register, or <code>null</code> to remove an existing one
	 * @param contentType the content type under which to register
	 */
	public void setRepairer(IPresentationRepairer repairer, String contentType) {

		Assert.isNotNull(contentType);

		if (fRepairers == null)
			fRepairers= new HashMap<String, IPresentationRepairer>();

		if (repairer == null)
			fRepairers.remove(contentType);
		else
			fRepairers.put(contentType, repairer);
	}

	/*
	 * @see IPresentationReconciler#install(ITextViewer)
	 */
	public void install(ITextViewer viewer) {
		Assert.isNotNull(viewer);

		fViewer= viewer;
		fViewer.addTextInputListener(fInternalListener);
		fViewer.addViewportListener(fInternalListener);
		
		StyledText widget = fViewer.getTextWidget();
		if(widget != null)
			widget.addFocusListener(fInternalListener);
		
		IDocument document= viewer.getDocument();
		if (document != null)
			fInternalListener.inputDocumentChanged(null, document);
	}

	/*
	 * @see IPresentationReconciler#uninstall()
	 */
	public void uninstall() {
		fViewer.removeTextInputListener(fInternalListener);
		fViewer.removeViewportListener(fInternalListener);
		
		StyledText widget = fViewer.getTextWidget();
		if(widget != null)
			widget.removeFocusListener(fInternalListener);
		
		// Ensure we uninstall all listeners
		fInternalListener.inputDocumentAboutToBeChanged(fViewer.getDocument(), null);
	}

	/*
	 * @see IPresentationReconciler#getDamager(String)
	 */
	public IPresentationDamager getDamager(String contentType) {

		if (fDamagers == null)
			return null;

		return fDamagers.get(contentType);
	}

	/*
	 * @see IPresentationReconciler#getRepairer(String)
	 */
	public IPresentationRepairer getRepairer(String contentType) {

		if (fRepairers == null)
			return null;

		return fRepairers.get(contentType);
	}

	/**
	 * Informs all registered damagers about the document on which they will work.
	 *
	 * @param document the document on which to work
	 */
	protected void setDocumentToDamagers(IDocument document) {
		if (fDamagers != null) {
			Iterator<IPresentationDamager> e= fDamagers.values().iterator();
			while (e.hasNext()) {
				IPresentationDamager damager= e.next();
				damager.setDocument(document);
			}
		}
	}

	/**
	 * Informs all registered repairers about the document on which they will work.
	 *
	 * @param document the document on which to work
	 */
	protected void setDocumentToRepairers(IDocument document) {
		if (fRepairers != null) {
			Iterator<IPresentationRepairer> e= fRepairers.values().iterator();
			while (e.hasNext()) {
				IPresentationRepairer repairer= e.next();
				repairer.setDocument(document);
			}
		}
	}

	/**
	 * Constructs a "repair description" for the given damage and returns this
	 * description as a text presentation. For this, it queries the partitioning
	 * of the damage region and asks the appropriate presentation repairer for
	 * each partition to construct the "repair description" for this partition.
	 *
	 * @param damage the damage to be repaired
	 * @param document the document whose presentation must be repaired
	 * @return the presentation repair description as text presentation or
	 *         <code>null</code> if the partitioning could not be computed
	 */
	protected TextPresentation createPresentation(IRegion damage, IDocument document) {
		try {
			if (fRepairers == null || fRepairers.isEmpty()) {
				TextPresentation presentation= new TextPresentation(damage, 100);
				presentation.setDefaultStyleRange(new StyleRange(damage.getOffset(), damage.getLength(), null, null));
				return presentation;
			}

			TextPresentation presentation= new TextPresentation(damage, 1000);

			ITypedRegion[] partitioning= TextUtilities.computePartitioning(document, getDocumentPartitioning(), damage.getOffset(), damage.getLength(), false);
			for (int i= 0; i < partitioning.length; i++) {
				ITypedRegion r= partitioning[i];
				IPresentationRepairer repairer= getRepairer(r.getType());
				if (repairer != null)
					repairer.createPresentation(presentation, r);
			}

			return presentation;

		} catch (BadLocationException x) {
			return null;
		}
	}


	/**
	 * Checks for the first and the last affected partition affected by a
	 * document event and calls their damagers. Invalidates everything from the
	 * start of the damage for the first partition until the end of the damage
	 * for the last partition.
	 *
	 * @param e the event describing the document change
	 * @param optimize <code>true</code> if partition changes should be
	 *        considered for optimization
	 * @return the damaged caused by the change or <code>null</code> if
	 *         computing the partitioning failed
	 * @since 3.0
	 */
	private IRegion getDamage(DocumentEvent e, boolean optimize) {
		int length= e.getText() == null ? 0 : e.getText().length();
		
		if (fDamagers == null || fDamagers.isEmpty()) {
			length= Math.max(e.getLength(), length);
			length= Math.min(e.getDocument().getLength() - e.getOffset(), length);
			return new Region(e.getOffset(), length);
		}

		boolean isDeletion= length == 0;
		IRegion damage= null;
		try {
			int offset= e.getOffset();
			if (isDeletion)
				offset= Math.max(0, offset - 1);
			ITypedRegion partition= getPartition(e.getDocument(), offset);
			IPresentationDamager damager= getDamager(partition.getType());
			if (damager == null)
				return null;

			IRegion r= damager.getDamageRegion(partition, e, fDocumentPartitioningChanged);

			if (!fDocumentPartitioningChanged && optimize && !isDeletion) {
				damage= r;
			} else {

				int damageEnd= getDamageEndOffset(e);

				int parititionDamageEnd= -1;
				if (fChangedDocumentPartitions != null)
					parititionDamageEnd= fChangedDocumentPartitions.getOffset() + fChangedDocumentPartitions.getLength();

				int end= Math.max(damageEnd, parititionDamageEnd);

				damage= end == -1 ? r : new Region(r.getOffset(), end - r.getOffset());
			}

		} catch (BadLocationException x) {
		}

		return damage;
	}

	/**
	 * Returns the end offset of the damage. If a partition has been split by
	 * the given document event also the second half of the original
	 * partition must be considered. This is achieved by using the remembered
	 * partition range.
	 *
	 * @param e the event describing the change
	 * @return the damage end offset (excluding)
	 * @exception BadLocationException if method accesses invalid offset
	 */
	private int getDamageEndOffset(DocumentEvent e) throws BadLocationException {

		IDocument d= e.getDocument();

		int length= 0;
		if (e.getText() != null) {
			length= e.getText().length();
			if (length > 0)
				-- length;
		}

		ITypedRegion partition= getPartition(d, e.getOffset() + length);
		int endOffset= partition.getOffset() + partition.getLength();
		if (endOffset == e.getOffset())
			return -1;

		int end= fRememberedPosition == null ? -1 : fRememberedPosition.getOffset() + fRememberedPosition.getLength();
		if (endOffset < end && end < d.getLength())
			partition= getPartition(d, end);

		IPresentationDamager damager= getDamager(partition.getType());
		if (damager == null)
			return -1;

		IRegion r= damager.getDamageRegion(partition, e, fDocumentPartitioningChanged);

		return r.getOffset() + r.getLength();
	}

	/**
	 * Processes the given damage.
	 * @param damage the damage to be repaired
	 * @param document the document whose presentation must be repaired
	 */
	private void processDamage(IRegion damage, IDocument document) {
		if(damage == null || fViewport == null) return;
		fDamagedRegions.add(damage);
		IRegion targetRegion = RegionList.intersection(damage, fViewport); 
		if (targetRegion.getLength() > 0) {
			TextPresentation p= createPresentation(targetRegion, document);
			if (p != null)
				applyTextRegionCollection(p);
			fDamagedRegions.remove(targetRegion);
		}
	}
	
	/**
	 * Applies the given text presentation to the text viewer the presentation
	 * reconciler is installed on.
	 *
	 * @param presentation the text presentation to be applied to the text viewer
	 */
	private void applyTextRegionCollection(TextPresentation presentation) {
		fViewer.changeTextPresentation(presentation, false);
	}

	/**
	 * Returns the partition for the given offset in the given document.
	 *
	 * @param document the document
	 * @param offset the offset
	 * @return the partition
	 * @throws BadLocationException if offset is invalid in the given document
	 * @since 3.0
	 */
	private ITypedRegion getPartition(IDocument document, int offset) throws BadLocationException {
		return TextUtilities.getPartition(document, getDocumentPartitioning(), offset, false);
	}
	

	/**
	 * Internal listener class.
	 */
	class InternalListener implements
			ITextInputListener, IDocumentListener, ITextListener,
			IDocumentPartitioningListener, IDocumentPartitioningListenerExtension, IDocumentPartitioningListenerExtension2,
			IViewportListener, FocusListener {

		/** Set to <code>true</code> if between a document about to be changed and a changed event. */
		private boolean fDocumentChanging= false;
		/**
		 * The cached redraw state of the text viewer.
		 * @since 3.0
		 */
		private boolean fCachedRedrawState= true;

		/*
		 * @see ITextInputListener#inputDocumentAboutToBeChanged(IDocument, IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldDocument, IDocument newDocument) {
			if (oldDocument != null) {
				try {

					fViewer.removeTextListener(this);
					oldDocument.removeDocumentListener(this);
					oldDocument.removeDocumentPartitioningListener(this);

					oldDocument.removePositionUpdater(fPositionUpdater);
					oldDocument.removePositionCategory(fPositionCategory);

				} catch (BadPositionCategoryException x) {
					// should not happened for former input documents;
				}
			}
		}

		/*
		 * @see ITextInputListener#inputDocumenChanged(IDocument, IDocument)
		 */
		public void inputDocumentChanged(IDocument oldDocument, IDocument newDocument) {

			fDocumentChanging= false;
			fCachedRedrawState= true;

			if (newDocument != null) {
				if(fViewport == null) updateViewport();
				
				newDocument.addPositionCategory(fPositionCategory);
				newDocument.addPositionUpdater(fPositionUpdater);

				newDocument.addDocumentPartitioningListener(this);
				newDocument.addDocumentListener(this);
				fViewer.addTextListener(this);

				setDocumentToDamagers(newDocument);
				setDocumentToRepairers(newDocument);
				processDamage(new Region(0, newDocument.getLength()), newDocument);
			}
		}

		/*
		 * @see IDocumentPartitioningListener#documentPartitioningChanged(IDocument)
		 */
		public void documentPartitioningChanged(IDocument document) {
			if (!fDocumentChanging && fCachedRedrawState)
				processDamage(new Region(0, document.getLength()), document);
			else
				fDocumentPartitioningChanged= true;
		}

		/*
		 * @see IDocumentPartitioningListenerExtension#documentPartitioningChanged(IDocument, IRegion)
		 * @since 2.0
		 */
		public void documentPartitioningChanged(IDocument document, IRegion changedRegion) {
			if (!fDocumentChanging && fCachedRedrawState) {
				processDamage(new Region(changedRegion.getOffset(), changedRegion.getLength()), document);
			} else {
				fDocumentPartitioningChanged= true;
				fChangedDocumentPartitions= changedRegion;
			}
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentPartitioningListenerExtension2#documentPartitioningChanged(org.eclipse.jface.text.DocumentPartitioningChangedEvent)
		 * @since 3.0
		 */
		public void documentPartitioningChanged(DocumentPartitioningChangedEvent event) {
			IRegion changedRegion= event.getChangedRegion(getDocumentPartitioning());
			if (changedRegion != null)
				documentPartitioningChanged(event.getDocument(), changedRegion);
		}

		/*
		 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent e) {

			fDocumentChanging= true;
			if (fCachedRedrawState) {
				try {
					int offset= e.getOffset() + e.getLength();
					ITypedRegion region= getPartition(e.getDocument(), offset);
					fRememberedPosition= new TypedPosition(region);
					e.getDocument().addPosition(fPositionCategory, fRememberedPosition);
				} catch (BadLocationException x) {
					// can not happen
				} catch (BadPositionCategoryException x) {
					// should not happen on input elements
				}
			}
		}

		/*
		 * @see IDocumentListener#documentChanged(DocumentEvent)
		 */
		public void documentChanged(DocumentEvent e) {
			if (fCachedRedrawState) {
				try {
					e.getDocument().removePosition(fPositionCategory, fRememberedPosition);
				} catch (BadPositionCategoryException x) {
					// can not happen on input documents
				}
			}
			fDocumentChanging= false;
		}

		/*
		 * @see ITextListener#textChanged(TextEvent)
		 */
		public void textChanged(TextEvent e) {

			fCachedRedrawState= e.getViewerRedrawState();
	 		if (!fCachedRedrawState)
	 			return;

	 		IRegion damage= null;
	 		IDocument document= null;

		 	if (e.getDocumentEvent() == null) {
		 		document= fViewer.getDocument();
		 		if (document != null)  {
			 		if (e.getOffset() == 0 && e.getLength() == 0 && e.getText() == null) {
						// redraw state change, damage the whole document
						damage= new Region(0, document.getLength());
			 		} else {
						IRegion region= widgetRegion2ModelRegion(e);
						try {
							String text= document.get(region.getOffset(), region.getLength());
							DocumentEvent de= new DocumentEvent(document, region.getOffset(), region.getLength(), text);
							damage= getDamage(de, false);
						} catch (BadLocationException x) {
						}
			 		}
		 		}
		 	} else  {
		 		DocumentEvent de= e.getDocumentEvent();
		 		document= de.getDocument();
		 		damage= getDamage(de, true);
		 	}

			if (damage != null && document != null)
				processDamage(damage, document);

			fDocumentPartitioningChanged= false;
			fChangedDocumentPartitions= null;
		}

		/**
		 * Translates the given text event into the corresponding range of the viewer's document.
		 *
		 * @param e the text event
		 * @return the widget region corresponding the region of the given event
		 * @since 2.1
		 */
		protected IRegion widgetRegion2ModelRegion(TextEvent e) {

			String text= e.getText();
			int length= text == null ? 0 : text.length();

			if (fViewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension= (ITextViewerExtension5) fViewer;
				return extension.widgetRange2ModelRange(new Region(e.getOffset(), length));
			}

			IRegion visible= fViewer.getVisibleRegion();
			IRegion region= new Region(e.getOffset() + visible.getOffset(), length);
			return region;
		}

		@Override
		public void viewportChanged(int verticalOffset) {
			updateViewport();
		}

		@Override
		public void focusGained(FocusEvent e) {
			updateViewport();
		}

		@Override
		public void focusLost(FocusEvent e) {
			// nothing to do
		}
	}

	public static class RegionList {
		TreeSet<IRegion> regions;
		
		public RegionList() {
			regions = new TreeSet<IRegion>(new Comparator<IRegion>() {
				@Override
				public int compare(IRegion r1, IRegion r2) {
					int diff = r2.getOffset() - r1.getOffset();
					if(diff != 0) return diff;
					return r2.getLength() - r1.getLength();
				}});
		}
		
		public void add(IRegion region) {
			IRegion preceding = regions.floor(region);
			if(preceding != null && intersects(preceding, region)) {
				region = union(region, preceding);
				regions.remove(preceding);
			};
			IRegion following = regions.ceiling(region);
			if(following != null && intersects(following, region)) {
				region = union(region, following);
				regions.remove(following);
			};
			regions.add(region);
		}

		public void remove(IRegion region) {
			IRegion preceding = regions.floor(region);
			if(preceding != null) {
				regions.remove(preceding);
				regions.addAll(subtract(preceding, region));
			}

			IRegion following = regions.ceiling(region);
			if(following != null) {
				regions.remove(following);
				regions.addAll(subtract(following, region));
			}
		}

		public boolean intersects(IRegion region) {
			IRegion preceding = regions.floor(region);
			if(preceding != null && intersects(preceding, region)) return true;
			IRegion following = regions.ceiling(region);
			if(following != null && intersects(following, region)) return true;
			return false;
		}

		public static IRegion union(IRegion r1, IRegion r2) {
			int targetOffset = Math.min(r1.getOffset(), r2.getOffset());
			int targetRegionEnd = Math.max(r1.getOffset() + r1.getLength(), r2.getOffset() + r2.getLength());
			return new Region(targetOffset, targetRegionEnd - targetOffset); 
		}
		
		public static IRegion intersection(IRegion r1, IRegion r2) {
			int targetOffset = Math.max(r1.getOffset(), r2.getOffset());
			int targetRegionEnd = Math.min(r1.getOffset() + r1.getLength(), r2.getOffset() + r2.getLength());
			int targetLength = Math.max(0, targetRegionEnd - targetOffset);
			return new Region(targetOffset, targetLength); 
		}
		
		public static boolean intersects(IRegion r1, IRegion r2) {
			return intersection(r1, r2).getLength() > 0; 
		}

		/**
		 * Subtracts region r2 from region r1.
		 * There are three cases:
		 * - this operation shortens r1 from the start, and results in a single new region
		 * - this operation shortens r1 from the end, and results in a single new region
		 * - this operation "digs a hole" into r1, and results in two new regions
		 */
		public static Collection<IRegion> subtract(IRegion r1, IRegion r2) {
			ArrayList<IRegion> results = new ArrayList<IRegion>();
			if(!intersects(r1, r2)) results.add(r1);
			else {
				if(r2.getOffset() < r1.getOffset()) {
					int targetOffset = Math.max(r1.getOffset(), r2.getOffset() + r2.getLength());
					int targetRegionEnd = Math.min(r1.getOffset() + r1.getLength(), r2.getOffset() + r2.getLength());
					results.add(new Region(targetOffset, targetRegionEnd - targetOffset));
				}
				else {
					if(r2.getOffset() + r2.getLength() > r1.getOffset() + r1.getLength())
						results.add(new Region(r1.getOffset(), r2.getOffset() - r1.getOffset()));
					else {
						results.add(new Region(r1.getOffset(), r2.getOffset() - r1.getOffset()));
						results.add(new Region(r2.getOffset() + r2.getLength(), r1.getOffset() + r1.getLength() - (r2.getOffset() + r2.getLength())));
					}
				}
			}
			return results; 
		}
	}
}

package org.tirix.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class MMLayout {

	public static final class TopCenterLayout extends Layout {
		private final int barHeight;
		private final int margin;
	
		public TopCenterLayout(int barHeight, int margin) {
			this.barHeight = barHeight;
			this.margin = margin;
		}
	
		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Control[] children = composite.getChildren();
			Control top = children[0];
			Point topSize = top.computeSize(wHint, barHeight, flushCache);
	
			Control center = children[1];
			Point centerSize = center.computeSize(wHint, SWT.DEFAULT, flushCache);
			
			return new Point(Math.max(topSize.x, centerSize.x), topSize.y + margin + centerSize.y);
		}
	
		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle rect = composite.getClientArea();
			Control[] children = composite.getChildren();
			
			Control top = children[0];
			Point pt = top.computeSize(SWT.DEFAULT, barHeight, flushCache);
			top.setBounds(rect.x, rect.y + margin/2, rect.width, pt.y );
	
			Control center = children[1];
			center.setBounds(rect.x, rect.y + pt.y + margin, rect.width, rect.height - pt.y  - margin);
		}
	}

	public static final class BarLayout extends Layout {
		private final int margin;
	
		public BarLayout(int margin) {
			this.margin = margin;
		}
	
		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			int width = 0;
			
			for(Control child:composite.getChildren()) {
				Point size = child.computeSize(SWT.DEFAULT, hHint, flushCache);
				width += size.x + margin;
			}
			return new Point(width, hHint);
		}
	
		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle rect = composite.getClientArea();
			int x = rect.x;
			
			for(Control child:composite.getChildren()) {
				Point size = child.computeSize(SWT.DEFAULT, rect.height, flushCache);
				child.setBounds(x, rect.y, size.x, rect.height);
				x += size.x + margin;
			}
		}
	}

}

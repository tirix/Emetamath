package org.tirix.emetamath;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.browser.WebBrowserView;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.services.IServiceLocator;
import org.osgi.framework.BundleContext;
import org.tirix.eclipse.ImagesOnFileSystemRegistry;
import org.tirix.emetamath.editors.MetamathEditor;
import org.tirix.emetamath.nature.MetamathAdapterFactory;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.ShowInAdapterFactory;
import org.tirix.emetamath.views.ProofExplorerView;
import org.tirix.emetamath.views.StepSelectorView;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.tirix.emetamath";

	// The shared instance
	private static Activator plugin;
	
	private static ImagesOnFileSystemRegistry imagesOnFSRegistry;
	
	/**
	 * The constructor
	 */
	public Activator() {
		}

	/**
	 * Register the Metamath adapters
	 */
	private static void registerAdapters() {
		IAdapterFactory adapterFactory = new MetamathAdapterFactory();
		IAdapterManager manager = Platform.getAdapterManager();
		manager.registerAdapters(adapterFactory, FileEditorInput.class);
		manager.registerAdapters(adapterFactory, MetamathEditor.class);
		manager.registerAdapters(adapterFactory, ProofExplorerView.class);
		manager.registerAdapters(adapterFactory, StepSelectorView.class);
		manager.registerAdapters(adapterFactory, IResource.class);
		
		IAdapterFactory showInAdapterFactory = new ShowInAdapterFactory();
		manager.registerAdapters(showInAdapterFactory, WebBrowserView.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// previously in constructor with the note "shouldn't we do this in start() ?"
		addToSearchMenu();
		registerAdapters();

		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
    public static Image getImage(String imagePath) {
        ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, imagePath);
        Image image = imageDescriptor.createImage();
        return image;
    }

	public static URL getImageURL(String path) {
		ImageDescriptor descriptor = getImageDescriptor(path);
		if(descriptor == null) return null;
    	if(imagesOnFSRegistry == null) imagesOnFSRegistry = new ImagesOnFileSystemRegistry(getDefault());
    	return imagesOnFSRegistry.getImageURL(descriptor);
	}

	/**
	 * Add our command to the "Search" menu.
	 * Required due to Eclipse bug #213385 
	 */
	public static void addToSearchMenu() {
       final IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
       final ImageDescriptor searchIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/mmSearch.gif");
       AbstractContributionFactory factory = new AbstractContributionFactory("menu:org.eclipse.search.menu?after=dialogGroup", null) {
    	   @Override
    	   public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
    		   CommandContributionItem item = new CommandContributionItem(new CommandContributionItemParameter(
    				   serviceLocator,
    				   "org.eclipse.jdt.internal.ui.search.openJavaSearchPage",
    				   "org.eclipse.jdt.internal.ui.search.openJavaSearchPage",
    				   null, searchIcon, null, null, null, null, null,
    				   CommandContributionItem.STYLE_PUSH, null, false));
    		   additions.addContributionItem(item, null);
    	   }
       };
       menuService.addContributionFactory(factory);
	}
	
	public static void log(String message, Exception e) {
		plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, e)); //$NON-NLS-1$
	}
}

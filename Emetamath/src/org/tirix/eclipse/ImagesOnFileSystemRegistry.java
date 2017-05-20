package org.tirix.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;


/**
 * Generic Image registry that keeps its images on the local file system.
 * 
 * @since 3.4
 */
public class ImagesOnFileSystemRegistry {
	
	private static final String IMAGE_DIR= "mm-images"; //$NON-NLS-1$
	
	private Plugin fPlugin;
	private HashMap<ImageDescriptor, URL> fURLMap;
	private final File fTempDir;
	private int fImageCount;

	public ImagesOnFileSystemRegistry(Plugin plugin) {
		fURLMap = new HashMap<ImageDescriptor, URL>();
		fPlugin = plugin;
		fTempDir = getTempDir();
		fImageCount = 0;
	}
	
	private File getTempDir() {
		try {
			File imageDir= fPlugin.getStateLocation().append(IMAGE_DIR).toFile();
			if (imageDir.exists()) {
				// has not been deleted on previous shutdown
				delete(imageDir);
			}
			if (!imageDir.exists()) {
				imageDir.mkdir();
			}
			if (!imageDir.isDirectory()) {
				System.out.println("Failed to create image directory " + imageDir.toString()); //$NON-NLS-1$
				return null;
			}
			return imageDir;
		} catch (IllegalStateException e) {
			// no state location
			return null;
		}
	}
	
	private void delete(File file) {
		if (file.isDirectory()) {
			File[] listFiles= file.listFiles();
			for (int i= 0; i < listFiles.length; i++) {
				delete(listFiles[i]);
			}
		}
		file.delete();
	}

	public URL getImageURL(ImageDescriptor descriptor) {
		if (fTempDir == null)
			return null;
		
		URL url= fURLMap.get(descriptor);
		if (url != null)
			return url;

		File imageFile= getNewFile();
		ImageData imageData= descriptor.getImageData();
		if (imageData == null) {
			return null;
		}

		ImageLoader loader= new ImageLoader();
		loader.data= new ImageData[] { imageData };
		loader.save(imageFile.getAbsolutePath(), SWT.IMAGE_PNG);
		
		try {
			url= imageFile.toURI().toURL();
			fURLMap.put(descriptor, url);
			return url;
		} catch (MalformedURLException e) {
			System.out.println(e);
		}
		return null;
	}
	
	private File getNewFile() {
		File file;
		do {
			file= new File(fTempDir, String.valueOf(fImageCount++) + ".png"); //$NON-NLS-1$
		} while (file.exists());
		return file;
	}
	
	public void dispose() {
		if (fTempDir != null) {
			delete(fTempDir);
		}
		fURLMap= null;
	}
}

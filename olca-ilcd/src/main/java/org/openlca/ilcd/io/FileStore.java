package org.openlca.ilcd.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStore implements DataStore {

	private File rootDir;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private XmlBinder binder = new XmlBinder();

	public FileStore(String pathToFolder) {
		this(new File(pathToFolder));
	}

	public FileStore(File rootDir) {
		log.trace("Create file store {}", rootDir);
		this.rootDir = new File(rootDir, "ILCD");
		checkRootDir();
	}

	private void checkRootDir() {
		log.trace("Check root directory {}", rootDir);
		if (!rootDir.exists()) {
			rootDir.mkdirs();
		} else if (!rootDir.isDirectory()) {
			throw new IllegalArgumentException("The file " + rootDir
					+ " is not a directory.");
		}
	}

	public void prepareFolder() throws DataStoreException {
		log.trace("Prepare ILCD folder {}", rootDir);
		ILCDFolder folder = new ILCDFolder(rootDir);
		try {
			folder.makeFolder();
		} catch (Exception e) {
			String message = "Cannot create ILCD folder "
					+ rootDir.getAbsolutePath();
			log.error(message, e);
			throw new DataStoreException(message);
		}
	}

	public File getRootFolder() {
		return rootDir;
	}

	@Override
	public <T> T get(Class<T> type, String id) throws DataStoreException {
		log.trace("Get {} for id {} from file", type, id);
		try {
			File file = findFile(type, id);
			if (file != null) {
				log.trace("Unmarshal from file {}", file);
				return binder.fromFile(type, file);
			}
			log.trace("No file found, return null");
			return null;
		} catch (Exception e) {
			String message = "Cannot unmarshal file.";
			log.error(message, e);
			throw new DataStoreException(message);
		}
	}

	@Override
	public void put(Object obj, String id) throws DataStoreException {
		log.trace("Store {} for id {} in file.", obj, id);
		try {
			File file = newFile(obj.getClass(), id);
			binder.toFile(obj, file);
		} catch (Exception e) {
			String message = "Cannot store in file";
			log.error(message, e);
			throw new DataStoreException(message);
		}
	}

	@Override
	public <T> boolean delete(Class<T> type, String id)
			throws DataStoreException {
		log.trace("Delete file if exists for class {} with id {}", type, id);
		File file = findFile(type, id);
		if (file == null)
			return false;
		else {
			boolean b = file.delete();
			log.trace("Deleted={}", b);
			return b;
		}
	}

	@Override
	public <T> Iterator<T> iterator(Class<T> type) throws DataStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> boolean contains(Class<T> type, String id)
			throws DataStoreException {
		log.trace("Contains file for class {} with id {}", type, id);
		File file = findFile(type, id);
		boolean contains = file != null && file.exists();
		log.trace("Contains={}", contains);
		return contains;
	}

	private File newFile(Class<?> clazz, String id) {
		log.trace("Make file for class {} with id {}", clazz, id);
		File dir = getFolder(clazz);
		File file = new File(dir, id + ".xml");
		log.trace("New file: {}", file);
		return file;
	}

	private File findFile(Class<?> clazz, String id) {
		log.trace("Find file for class {} with id {}", clazz, id);
		File dir = getFolder(clazz);
		File file = null;
		for (File f : dir.listFiles()) {
			if (f.getName().toLowerCase().contains(id.toLowerCase())) {
				file = f;
				break;
			}
		}
		log.trace("Return file: {}", file);
		return file;
	}

	public File getFolder(Class<?> clazz) {
		String name = Path.forClass(clazz);
		File folder = findFolder(name, rootDir);
		if (folder == null) {
			folder = new File(rootDir, name);
			folder.mkdirs();
		}
		return folder;
	}

	private File findFolder(String name, File dir) {
		log.trace("Search folder {} in {}", name, dir);
		File folder = null;
		if (dir.getName().equalsIgnoreCase(name)) {
			folder = dir;
		} else {
			File[] files = dir.listFiles();
			int i = 0;
			while (folder == null && i < files.length) {
				if (files[i].isDirectory()) {
					folder = findFolder(name, files[i]);
				}
				i++;
			}
		}
		return folder;
	}

	@Override
	public void close() throws IOException {
	}

}

package org.openlca.cloud.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.openlca.cloud.util.Directories;
import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryConfig {

	private final static Logger log = LoggerFactory
			.getLogger(RepositoryConfig.class);
	private final IDatabase database;
	private final String baseUrl;
	private final String repositoryId;
	private final String username;
	private final String password;
	private String lastCommitId;

	RepositoryConfig(IDatabase database, String baseUrl, String repositoryId,
			String lastCommitId, String username, String password) {
		this.database = database;
		this.baseUrl = baseUrl;
		this.repositoryId = repositoryId;
		this.lastCommitId = lastCommitId;
		this.username = username;
		this.password = password;
	}

	public static RepositoryConfig loadFor(IDatabase database) {
		File configFile = getConfigFile(database);
		if (!configFile.exists())
			return null;
		Properties properties = new Properties();
		try (FileInputStream stream = new FileInputStream(configFile)) {
			properties.load(stream);
			String baseUrl = properties.getProperty("baseUrl");
			String repositoryId = properties.getProperty("repositoryId");
			String lastCommitId = properties.getProperty("lastCommitId");
			String username = properties.getProperty("username");
			String password = properties.getProperty("password");
			if ("null".equals(lastCommitId))
				lastCommitId = null;
			return new RepositoryConfig(database, baseUrl, repositoryId,
					lastCommitId, username, password);
		} catch (IOException e) {
			log.error("Error loading repository properties", e);
			return null;
		}
	}

	public void save() {
		File configFile = getConfigFile(database);
		if (configFile.exists())
			configFile.delete();
		try (FileOutputStream stream = new FileOutputStream(configFile)) {
			configFile.createNewFile();
			Properties properties = new Properties();
			properties.setProperty("baseUrl", baseUrl);
			properties.setProperty("repositoryId", repositoryId);
			if (lastCommitId == null)
				lastCommitId = "null";
			properties.setProperty("lastCommitId", lastCommitId);
			properties.setProperty("username", username);
			properties.setProperty("password", password); // TODO encrypt
			properties.store(stream, "");
		} catch (IOException e) {
			log.error("Error saving repository properties", e);
		}
	}

	public static RepositoryConfig connect(IDatabase database, String baseUrl,
			String repositoryId, String username, String password) {
		RepositoryConfig config = new RepositoryConfig(database, baseUrl,
				repositoryId, null, username, password);
		config.save();
		return config;
	}

	public void disconnect() {
		File configFile = getConfigFile(database);
		configFile.delete();
		File fileStorage = database.getFileStorageLocation();
		Directories.delete(new File(fileStorage, "cloud/" + getRepositoryId()));
	}

	private static File getConfigFile(IDatabase database) {
		return new File(database.getFileStorageLocation(),
				"repository.properties");
	}

	String getBaseUrl() {
		return baseUrl;
	}

	public String getServerUrl() {
		int slashIndex = -1;
		if (baseUrl.contains("://"))
			slashIndex = baseUrl.indexOf('/', baseUrl.indexOf("://") + 3);
		else
			slashIndex = baseUrl.indexOf('/');
		if (slashIndex == -1)
			return baseUrl;
		return baseUrl.substring(0, slashIndex);
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public String getRepositoryOwner() {
		return repositoryId.split("/")[0];
	}

	public String getRepositoryName() {
		return repositoryId.split("/")[1];
	}

	void setLastCommitId(String lastCommitId) {
		this.lastCommitId = lastCommitId;
		save();
	}

	public String getLastCommitId() {
		return lastCommitId;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public String getUsername() {
		return username;
	}

	String getPassword() {
		return password;
	}

}

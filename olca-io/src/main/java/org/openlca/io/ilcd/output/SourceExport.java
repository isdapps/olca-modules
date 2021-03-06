package org.openlca.io.ilcd.output;

import java.io.File;

import org.openlca.core.database.FileStore;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.ilcd.commons.ClassificationInfo;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.sources.AdminInfo;
import org.openlca.ilcd.sources.DataEntry;
import org.openlca.ilcd.sources.DataSetInfo;
import org.openlca.ilcd.sources.DigitalFileRef;
import org.openlca.ilcd.sources.Publication;
import org.openlca.ilcd.sources.Source;
import org.openlca.ilcd.sources.SourceInfo;
import org.openlca.ilcd.util.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceExport {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final ExportConfig config;
	private String baseUri;
	private org.openlca.core.model.Source source;

	public SourceExport(ExportConfig config) {
		this.config = config;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public Source run(org.openlca.core.model.Source source)
			throws DataStoreException {
		if (config.store.contains(Source.class, source.getRefId()))
			return config.store.get(Source.class, source.getRefId());
		this.source = source;
		log.trace("Run source export with {}", source);
		Source iSource = new Source();
		iSource.version = "1.1";
		iSource.administrativeInformation = makeAdminInfo();
		SourceInfo info = new SourceInfo();
		iSource.sourceInformation = info;
		DataSetInfo dataSetInfo = makeDateSetInfo();
		info.dataSetInformation = dataSetInfo;
		File extFile = getExternalFile();
		if (extFile == null)
			config.store.put(iSource, source.getRefId());
		else {
			addFileRef(dataSetInfo, extFile);
			config.store.put(iSource, source.getRefId(), extFile);
		}
		return iSource;
	}

	private File getExternalFile() {
		String name = source.getExternalFile();
		if (name == null)
			return null;
		File dbDir = config.db.getFileStorageLocation();
		if (dbDir == null)
			return null;
		String path = FileStore.getPath(ModelType.SOURCE, source.getRefId());
		File docDir = new File(dbDir, path);
		if (!docDir.exists())
			return null;
		File file = new File(docDir, name);
		return file.exists() ? file : null;
	}

	private DataSetInfo makeDateSetInfo() {
		log.trace("Create data set information.");
		DataSetInfo info = new DataSetInfo();
		info.uuid = source.getRefId();
		LangString.set(info.shortName, source.getName(), config.lang);
		if (source.getDescription() != null) {
			LangString.set(info.sourceDescriptionOrComment,
					source.getDescription(), config.lang);
		}
		addTextReference(info);
		CategoryConverter converter = new CategoryConverter();
		ClassificationInfo classInfo = converter
				.getClassificationInformation(source.getCategory());
		info.classificationInformation = classInfo;
		return info;
	}

	private void addTextReference(DataSetInfo dataSetInfo) {
		log.trace("Create text reference.");
		String cit = source.getTextReference();
		if (cit == null)
			return;
		if (source.getYear() != null)
			cit += " " + source.getYear();
		dataSetInfo.sourceCitation = cit;
	}

	private void addFileRef(DataSetInfo info, File extFile) {
		DigitalFileRef fileRef = new DigitalFileRef();
		fileRef.uri = "../external_docs/" + extFile.getName();
		info.referenceToDigitalFile.add(fileRef);
	}

	private AdminInfo makeAdminInfo() {
		AdminInfo info = new AdminInfo();
		DataEntry entry = new DataEntry();
		info.dataEntryBy = entry;
		entry.timeStamp = Out.getTimestamp(source);
		entry.referenceToDataSetFormat.add(
				Reference.forIlcdFormat());
		addPublication(info);
		return info;
	}

	private void addPublication(AdminInfo info) {
		Publication pub = new Publication();
		info.publicationAndOwnership = pub;
		pub.dataSetVersion = Version.asString(source.getVersion());
		if (baseUri == null)
			baseUri = "http://openlca.org/ilcd/resource/";
		if (!baseUri.endsWith("/"))
			baseUri += "/";
		pub.permanentDataSetURI = baseUri + "sources/" + source.getRefId();
	}
}

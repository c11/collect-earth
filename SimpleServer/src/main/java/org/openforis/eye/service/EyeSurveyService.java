package org.openforis.eye.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.openforis.collect.manager.RecordManager;
import org.openforis.collect.manager.SurveyManager;
import org.openforis.collect.model.CollectRecord;
import org.openforis.collect.model.CollectRecord.Step;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.persistence.RecordPersistenceException;
import org.openforis.collect.persistence.SurveyImportException;
import org.openforis.idm.metamodel.Schema;
import org.openforis.idm.metamodel.xml.IdmlParseException;
import org.openforis.idm.model.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EyeSurveyService {

	private static final String ROOT_ENTITY = "plot";

	private static final String EYE_SURVEY_NAME = "eye";

	@Autowired
	SurveyManager surveyManager;

	@Autowired
	RecordManager recordManager;

	@Autowired
	CollectParametersHandler collectParametersHandler;

	@Autowired
	LocalPropertiesService localPropertiesService;

	CollectSurvey collectSurvey;

	String idmFilePath;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	public EyeSurveyService(String idmFilePath) {
		super();
		this.idmFilePath = idmFilePath;
	}

	private String getIdmFilePath() {
		return idmFilePath;
	}

	public void init() throws FileNotFoundException, IdmlParseException, SurveyImportException {
		collectSurvey = surveyManager.get(EYE_SURVEY_NAME);
		if (collectSurvey == null) {
			collectSurvey = surveyManager.unmarshalSurvey(new FileInputStream(new File(getIdmFilePath())));
			collectSurvey.setName(EYE_SURVEY_NAME);
			surveyManager.importModel(collectSurvey);
		}
	}

	public Map<String,String> getPlacemark(String placemarkId) {
		List<CollectRecord> summaries = recordManager.loadSummaries(collectSurvey, ROOT_ENTITY, placemarkId);
		CollectRecord record = null;
		Map<String, String> placemarkParameters = null;
		if (summaries.size() > 0) {
			record = summaries.get(0);
			record = recordManager.load(collectSurvey, record.getId(), Step.ENTRY.getStepNumber());
			placemarkParameters = collectParametersHandler.getParameters(record.getRootEntity());
		}
		return placemarkParameters;
	}


	public void storePlacemark(Map<String, String> parameters, String sessionId) throws RecordPersistenceException {
		List<CollectRecord> summaries = recordManager.loadSummaries(collectSurvey, ROOT_ENTITY, parameters.get("collect_text_id"));
		// Add the operator to the collected data
		parameters.put("collect_text_operator", localPropertiesService.getOperator());
		
		CollectRecord record = null;
		boolean update = false;
		Entity plotEntity = null;
		if (summaries.size() > 0) { // DELETE IF ALREADY PRESENT
			record = summaries.get(0);
			// recordManager.delete(record.getId());
			update = true;
			plotEntity = record.createRootEntity(ROOT_ENTITY);
			logger.warn("Update a plot entity with data " + parameters.toString());
		} else {
			// Create new record
			Schema schema = collectSurvey.getSchema();
			record = recordManager.create(collectSurvey, schema.getRootEntityDefinition(ROOT_ENTITY), null, null, sessionId);
			plotEntity = record.getRootEntity();
			logger.warn("Creating a new plot entity with data " + parameters.toString());
		}
		// Populate the data of the record using the HTTP parameters received
		collectParametersHandler.saveToEntity(parameters, plotEntity);

		recordManager.save(record, sessionId);
	}

	public boolean isPlacemarSaved(Map<String, String> placemarkParameters) {
		return placemarkParameters != null && placemarkParameters.get("collect_boolean_actively_saved") != null
				&& placemarkParameters.get("collect_boolean_actively_saved").equals("true");
	}
	// protected NodeChangeSet updateRecord(CollectRecord record,
	// NodeUpdateRequestSet nodeUpdateOptionSet) throws
	// RecordPersistenceException, RecordIndexException {
	// List<NodeUpdateRequest> opts = nodeUpdateOptionSet.getRequests();
	// NodeChangeMap result = new NodeChangeMap();
	// for (NodeUpdateRequest req : opts) {
	// NodeChangeSet partialChangeSet = updateRecord(record, req);
	// List<NodeChange<?>> changes = partialChangeSet.getChanges();
	// for (NodeChange<?> change : changes) {
	// result.addOrMergeChange(change);
	// }
	// }
	// return new NodeChangeSet(result.getChanges());
	// }

}

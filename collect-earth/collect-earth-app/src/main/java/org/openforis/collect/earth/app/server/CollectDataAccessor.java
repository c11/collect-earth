package org.openforis.collect.earth.app.server;

import java.util.Map;

import org.openforis.collect.earth.app.service.EarthSurveyService;
import org.openforis.collect.earth.core.model.PlacemarkLoadResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class to get/set data for the given placemark using Collect API.
 * @author Alfonso Sanchez-Paus Diaz
 *
 */
public class CollectDataAccessor implements DataAccessor {

	@Autowired
	private EarthSurveyService earthSurveyService;

	@Deprecated
	@Override
	public Map<String, String> getData(String placemarkId) {
		return earthSurveyService.getPlacemark(placemarkId,true);
	}

	@Override
	public PlacemarkLoadResult loadDataExpanded(String placemarkId) {
		return earthSurveyService.loadPlacemarkExpanded(placemarkId);
	}
	
	@Deprecated
	@Override
	public boolean saveData(Map<String, String> collectedData) {
		return earthSurveyService.storePlacemarkOld(collectedData, null);
	}

	@Override
	public PlacemarkLoadResult updateData(String placemarkId, Map<String, String> collectedData) {
		return earthSurveyService.updatePlacemarkData(placemarkId, collectedData, null);
	}
}

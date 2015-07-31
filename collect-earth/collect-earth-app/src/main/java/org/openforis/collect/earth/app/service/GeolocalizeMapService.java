package org.openforis.collect.earth.app.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openforis.collect.earth.app.service.LocalPropertiesService.EarthProperty;
import org.openforis.collect.earth.sampler.model.SimplePlacemarkObject;
import org.openforis.collect.earth.sampler.processor.KmlGenerator;
import org.openforis.collect.earth.sampler.utils.FreemarkerTemplateUtils;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import freemarker.template.TemplateException;

/**
 * Customizes a Bing Map page to open zoomed into a coordinate. The HTML page is created in a temporary file and its URL is returned so that it can be opened in a browser.
 * A freemarker template that contains the javascript code to customize the Bing Map is used and the parameters for the specific coordinates are applied to it.
 * This service uses the same code than the KML generator to get the plot sample deign as chosen through the configuration by the user.
 * @author Alfonso Sanchez-Paus Diaz
 * 
 */
@Component
public class GeolocalizeMapService {

	private static final String RESOURCES_FOLDER = "resources";



	/**
	 * The file that contains the freemarker template used to produce the Bing Maps code.
	 */
	public static final String FREEMARKER_BING_HTML_TEMPLATE = RESOURCES_FOLDER + File.separator + "collectBing.fmt";
	
	

	public static final String FREEMARKER_GEE_PLAYGROUND_TEMPLATE_FILE_NAME = "eePlaygroundScript.fmt";
	
	/**
	 * The file that contains the freemarker template used to produce script that is run in GEE Playground.
	 */
	public static final String FREEMARKER_GEE_PLAYGROUND_TEMPLATE = RESOURCES_FOLDER + File.separator + FREEMARKER_GEE_PLAYGROUND_TEMPLATE_FILE_NAME;



	public static final String FREEMARKER_HERE_HTML_TEMPLATE = RESOURCES_FOLDER + File.separator + "collectHereMaps.fmt";

	
	@Autowired
	LocalPropertiesService localPropertiesService;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	KmlGeneratorService kmlGeneratorService;

	private File applyData(Map<String,Object>  data, String freemarkerTemplateFile) throws IOException, TemplateException {

		final File templateFileSrc = new File(freemarkerTemplateFile);
		
		final File tempFileDst = File.createTempFile("selenium", ".html");
		tempFileDst.deleteOnExit();

		FreemarkerTemplateUtils.applyTemplate(templateFileSrc, tempFileDst, data);

		return tempFileDst;

	}

	public Map<String, Object> getPlacemarkData(String[] centerLatLong) {
		final Map<String, Object> data = new HashMap<String, Object>();
		final SimplePlacemarkObject placemark = new SimplePlacemarkObject(centerLatLong);

		final Float distanceBetweenSamplingPoints = Float.parseFloat( localPropertiesService.getValue(EarthProperty.DISTANCE_BETWEEN_SAMPLE_POINTS));
		final Float distancePlotBoundary = Float.parseFloat(localPropertiesService.getValue(EarthProperty.DISTANCE_TO_PLOT_BOUNDARIES));

		KmlGenerator kmlGenerator = kmlGeneratorService.getKmlGenerator();
		
		final double[] centerLatLongD = new double[] { Double.parseDouble(centerLatLong[0]), Double.parseDouble(centerLatLong[1])};
		
		try {
			kmlGenerator.fillSamplePoints(distanceBetweenSamplingPoints, centerLatLongD, "", placemark);
			kmlGenerator.fillExternalLine(distanceBetweenSamplingPoints.floatValue(), distancePlotBoundary.floatValue(), centerLatLongD,
					placemark);

			data.put("placemark", placemark);
		} catch (final TransformException e) {
			logger.error("Exception producing Bing map data for html ", e);
		}
		return data;
	}

	/**
	 * Produces a temporary file with the necessary HTML code to show the plot in Bing Maps
	 * @param centerCoordinates The coordinates of the center of the plot.
	 * @param freemarkerTemplate The path to the freemarker template that is used to produce the file.
	 * @return The URL to the temporary file that can be used to load it in a browser.
	 */
	public URL getTemporaryUrl(String[] centerCoordinates, String freemarkerTemplate) {

		final Map<String,Object> data = getPlacemarkData(centerCoordinates);
		return processTemplateWithData(freemarkerTemplate, data);

	}

	private URL processTemplateWithData(String freemarkerTemplate, final Map<String, Object> data) {
		File transformedHtml = null;
		try {
			transformedHtml = applyData(data, freemarkerTemplate);
		} catch (final Exception e) {
			logger.error("Exception when applying template for Bing map", e);
		}
		if (transformedHtml != null) {
			try {
				return transformedHtml.toURI().toURL();
			} catch (MalformedURLException e) {
				logger.error("Error generating URL for File " + transformedHtml.getAbsolutePath());
				return null;
			}
		} else {
			logger.error("No Bing map HTML generated.");
			return null;
		}
	}
	
	/**
	 * Produces a temporary file with the necessary HTML code to show the plot in Bing Maps
	 * @param centerCoordinates The coordinates of the center of the plot.
	 * @param bingMapsKey The bing maps key used, obtained from the Local Properties service
	 * @param freemarkerTemplate The path to the freemarker template that is used to produce the file.
	 * @return The URL to the temporary file that can be used to load it in a browser.
	 */
	public URL getBingUrl(String[] centerCoordinates, String bingMapsKey, String freemarkerTemplate) {

		final Map<String,Object> data = getPlacemarkData(centerCoordinates);
		data.put("bingMapsKey", bingMapsKey);
		return processTemplateWithData(freemarkerTemplate, data);
	}
	
	/**
	 * Produces a temporary file with the necessary HTML code to show the plot in Here Maps
	 * @param centerCoordinates The coordinates of the center of the plot.
	 * @param hereAppId The Here Maps app ID
	 * @param hereAppCode The Here Maps app code
	 * @param freemarkerTemplate The path to the freemarker template that is used to produce the file.
	 * @return The URL to the temporary file that can be used to load it in a browser.
	 */
	public URL getHereUrl(String[] centerCoordinates, String hereAppId, String hereAppCode, String freemarkerTemplate) {

		final Map<String,Object> data = getPlacemarkData(centerCoordinates);
		data.put("hereAppId", hereAppId);
		data.put("hereAppCode", hereAppCode);
		return processTemplateWithData(freemarkerTemplate, data);
	}

}

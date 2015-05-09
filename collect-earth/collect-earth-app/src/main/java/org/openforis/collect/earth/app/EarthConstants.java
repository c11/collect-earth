package org.openforis.collect.earth.app;

import java.io.File;
import java.util.Locale;

import org.openforis.collect.earth.app.service.FolderFinder;

/**
 * Constant container with constants used widely in the application.
 * @author Alfonso Sanchez-Paus Diaz
 *
 */
public class EarthConstants {
	
	public static final String COLLECT_EARTH_APPDATA_FOLDER = "CollectEarth";

	public static final String DATE_FORMAT_HTTP = "EEE, dd MMM yyyy HH:mm:ss zzz";
	
	public static final String LIST_FILLED_IMAGE = "images/list_filled.png";

	public static final String LIST_NON_FILLED_IMAGE = "images/list_empty.png";

	public static final String LIST_NOT_FINISHED_IMAGE = "images/list_not_finished.png";
	public static final String GENERATED_FOLDER_SUFFIX =  "generated";
	public static final String GENERATED_FOLDER = FolderFinder.getLocalFolder() + File.separator + GENERATED_FOLDER_SUFFIX;

	public static final String FOLDER_COPIED_TO_KMZ = "earthFiles";

	public static final String PLACEMARK_FOUND_PARAMETER = "placemark_found";

	public static final String ROOT_ENTITY_NAME = "plot";

	public static final String CHROME_BROWSER = "chrome";

	public static final String FIREFOX_BROWSER = "firefox";

	public static final String EARTH_SURVEY_NAME = "earth";
	
	public enum OperationMode{ SERVER_MODE, CLIENT_MODE};
	
	public static final String COLLECT_EARTH_DATABASE_SQLITE_DB = FolderFinder.getLocalFolder() + File.separator + "collectEarthDatabase.db";
	
	public enum SAMPLE_SHAPE{ SQUARE_CIRCLE, SQUARE, CIRCLE, OCTAGON};
	
	public enum UI_LANGUAGE{ 
		FR( "Français", new Locale("fr", "FR") ) , EN( "English", new Locale("en", "EN") ), ES( "Español", new Locale("es", "ES")), PT("Português", new Locale("pt","PT") );
		
		private Locale locale;
		private String label;
		
		private UI_LANGUAGE(String label, Locale locale){
			this.label = label;
			this.locale = locale;
		}
		
		public Locale getLocale(){
			return locale;
		}
		
		public String getLabel(){
			return label;
		}
	};
	
	public enum CollectDBDriver{ 
		SQLITE("org.sqlite.JDBC", "jdbc:sqlite:" + COLLECT_EARTH_DATABASE_SQLITE_DB ), 
		POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://REPLACE_HOSTNAME:REPLACE_PORT/REPLACE_DBNAME");
		
		private String driverClass;
		private String url;

		private CollectDBDriver(String driverClass, String url) {
			this.driverClass = driverClass;
			this.url = url;
		}
		public String getDriverClass() {
			return driverClass;
		}

		public String getUrl() {
			return url;
		}

	};

	private EarthConstants() {
		 throw new AssertionError();
	}
}

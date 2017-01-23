package org.openforis.collect.earth.app.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.earth.app.CollectEarthUtils;
import org.openforis.collect.earth.app.EarthConstants.OperationMode;
import org.openforis.collect.earth.app.EarthConstants.UI_LANGUAGE;
import org.openforis.collect.earth.app.desktop.EarthApp;
import org.openforis.collect.earth.app.service.AnalysisSaikuService;
import org.openforis.collect.earth.app.service.BackupSqlLiteService;
import org.openforis.collect.earth.app.service.DataImportExportService;
import org.openforis.collect.earth.app.service.EarthProjectsService;
import org.openforis.collect.earth.app.service.EarthSurveyService;
import org.openforis.collect.earth.app.service.FolderFinder;
import org.openforis.collect.earth.app.service.KmlImportService;
import org.openforis.collect.earth.app.service.LocalPropertiesService;
import org.openforis.collect.earth.app.service.MissingPlotService;
import org.openforis.collect.earth.app.view.ExportActionListener.RecordsToExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Alfonso Sanchez-Paus Diaz
 * 
 */
@Component
@Lazy(false)
public class CollectEarthWindow{

	@Autowired
	private LocalPropertiesService localPropertiesService;

	@Autowired
	private DataImportExportService dataImportExportService;

	@Autowired
	private EarthSurveyService earthSurveyService;

	@Autowired
	private AnalysisSaikuService analysisSaikuService;

	@Autowired
	private KmlImportService kmlImportService;

	@Autowired
	private EarthProjectsService earthProjectsService;

	@Autowired
	private BackupSqlLiteService backupSqlLiteService;
	
	@Autowired
	private MissingPlotService missingPlotService;
	
	@Autowired
	private CollectEarthTransferHandler collectEarthTransferHandler;
	
	public static void endWaiting(Window frame) {
		frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	}

	public static void startWaiting(Window frame) {
		frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
	}

	private JFrame frame;
	private final Logger logger = LoggerFactory.getLogger(CollectEarthWindow.class);

	public static final Color ERROR_COLOR = new Color(225, 124, 124);

	private final List<JMenuItem> serverMenuItems = new ArrayList<JMenuItem>();

	

	public CollectEarthWindow() throws IOException {
		// Create and set up the window.
		JFrame framePriv = new JFrame(Messages.getString("CollectEarthWindow.19") );//$NON-NLS-1$
	
		setFrame(framePriv ); 
	}

	@PostConstruct
	public void init(){
		Messages.setLocale(localPropertiesService.getUiLanguage().getLocale());
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {			
					
					CollectEarthWindow.this.openWindow();
					
				} catch (final Exception e) {
					logger.error("Cannot start Earth App", e); //$NON-NLS-1$
					System.exit(0);
				}
			}
		});
	}

	@PreDestroy
	public void cleanUp() throws InvocationTargetException, InterruptedException{
		SwingUtilities.invokeAndWait( new Runnable() {
			
			@Override
			public void run() {
				CollectEarthWindow.this.getFrame().dispose();
			}
		});
	}

	private void addImportExportMenu(JMenu menu) {

		final JMenu ieSubmenu = new JMenu(Messages.getString("CollectEarthWindow.44")); //$NON-NLS-1$
		JMenuItem menuItem;
		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.13")); //$NON-NLS-1$
		menuItem.addActionListener(getExportActionListener(DataFormat.CSV, RecordsToExport.ALL));
		ieSubmenu.add(menuItem);

		final JMenu xmlExportSubmenu = new JMenu(Messages.getString("CollectEarthWindow.24")); //$NON-NLS-1$

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.45")); //$NON-NLS-1$
		menuItem.addActionListener(getExportActionListener(DataFormat.ZIP_WITH_XML, RecordsToExport.ALL));
		xmlExportSubmenu.add(menuItem);

		final JMenuItem exportModifiedRecords = new JMenuItem(Messages.getString("CollectEarthWindow.61")); //$NON-NLS-1$
		exportModifiedRecords.addActionListener(getExportActionListener(DataFormat.ZIP_WITH_XML, RecordsToExport.MODIFIED_SINCE_LAST_EXPORT));
		xmlExportSubmenu.add(exportModifiedRecords);

		final JMenuItem exportDataRangeRecords = new JMenuItem("Export data to XML (from specific date)");
		exportDataRangeRecords.addActionListener(getExportActionListener(DataFormat.ZIP_WITH_XML, RecordsToExport.PICK_FROM_DATE));
		xmlExportSubmenu.add(exportDataRangeRecords);
		
		ieSubmenu.add(xmlExportSubmenu);
		
		final JMenu backupExportSubmenu = new JMenu("Export to Collect Backup");
		
		final JMenuItem exportDataBackup = new JMenuItem("Export data as Collect backup (all data)");
		exportDataBackup.addActionListener(getExportActionListener(DataFormat.COLLECT_BACKUP, RecordsToExport.ALL));
		backupExportSubmenu.add(exportDataBackup);
		
		final JMenuItem exportDataRangeBackup = new JMenuItem("Export data as Collect backup (from date)");
		exportDataRangeBackup.addActionListener(getExportActionListener(DataFormat.COLLECT_BACKUP, RecordsToExport.PICK_FROM_DATE));
		backupExportSubmenu.add(exportDataRangeBackup);
		
		ieSubmenu.add( backupExportSubmenu );

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.6")); //$NON-NLS-1$
		menuItem.addActionListener(getExportActionListener(DataFormat.FUSION, RecordsToExport.ALL));
		ieSubmenu.add(menuItem);

		ieSubmenu.addSeparator();

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.46")); //$NON-NLS-1$
		menuItem.addActionListener(getImportActionListener(DataFormat.ZIP_WITH_XML));
		ieSubmenu.add(menuItem);

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.55")); //$NON-NLS-1$
		menuItem.addActionListener(getImportActionListener(DataFormat.CSV));
		ieSubmenu.add(menuItem);

		menu.add(ieSubmenu);

		serverMenuItems.add(ieSubmenu); // This menu should only be shown if the DB is local ( not if Collect Earth is acting as a client )
	}

	private void addWindowClosingListener() {
		getFrame().addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				try {
					final String keepOpen = Messages.getString("CollectEarthWindow.37"); //$NON-NLS-1$
					final String close = Messages.getString("CollectEarthWindow.42"); //$NON-NLS-1$
					final String[] options = new String[] { close, keepOpen };

					final int confirmation = JOptionPane.showOptionDialog(getFrame(), Messages.getString("CollectEarthWindow.22"), //$NON-NLS-1$
							Messages.getString("CollectEarthWindow.23"),  //$NON-NLS-1$ 
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, keepOpen);

					if (confirmation == JOptionPane.YES_OPTION) {
						final Thread stopServer = new Thread() {
							@Override
							public void run() {
								try {
									//getServerController().stopServer();
									EarthApp.quitServer();
								} catch (final Exception e) {
									logger.error("Error when trying to closing the server", e); //$NON-NLS-1$
								}
							};
						};

						getFrame().setVisible(false);
						getFrame().dispose();
						stopServer.start();
						Thread.sleep(5000);

						System.exit(0);
					}
				} catch (final Exception e1) {
					logger.error("Error when trying to shutdown the server when window is closed", e1); //$NON-NLS-1$
				}

			}
		});
	}

	private void disableMenuItems() {
		if (localPropertiesService.getOperationMode().equals(OperationMode.CLIENT_MODE)) {
			for (final JMenuItem menuItem : serverMenuItems) {
				menuItem.setEnabled(false);
			}
		}
	}

	private void displayWindow() {
		getFrame().setLocationRelativeTo(null);
		getFrame().pack();
		getFrame().setVisible(true);
	}

	private ActionListener getCloseActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getFrame().dispatchEvent(new WindowEvent(getFrame(), WindowEvent.WINDOW_CLOSING));
			}
		};
	}


	private String getDisclaimerFilePath() {
		final String suffix_lang = localPropertiesService.getUiLanguage().getLocale().getLanguage();
		if (new File( "resources/disclaimer_" + suffix_lang + ".txt" ).exists() ){ //$NON-NLS-1$ //$NON-NLS-2$
			return "resources/disclaimer_" + suffix_lang + ".txt";
		}else{
			return  "resources/disclaimer_en.txt";
		}
	}


	private ActionListener getExportActionListener(final DataFormat exportFormat, final RecordsToExport xmlExportType) {
		return new ExportActionListener(exportFormat, xmlExportType, getFrame(), localPropertiesService, dataImportExportService,
				earthSurveyService);
	}

	public JFrame getFrame() {
		return frame;
	}

	private ActionListener getImportActionListener(final DataFormat importFormat) {
		return new ImportActionListener(importFormat, getFrame(), localPropertiesService, dataImportExportService);
	}

	private JMenu getLanguageMenu() {

		final ActionListener actionLanguage = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					final String langName = ((JRadioButtonMenuItem) e.getSource()).getName();
					final UI_LANGUAGE language = UI_LANGUAGE.valueOf(langName);
					CollectEarthUtils.setFontDependingOnLanguaue(language);
					localPropertiesService.setUiLanguage(language);

					SwingUtilities.invokeLater( new Thread(){
						@Override
						public void run() {

							getFrame().getContentPane().removeAll();
							getFrame().dispose();

							openWindow();
						};
					});

				} catch (final Exception ex) {
					ex.printStackTrace();
					logger.error("Error while changing language", ex); //$NON-NLS-1$
				}
			}
		};

		final JMenu menuLanguage = new JMenu(Messages.getString("CollectEarthWindow.2")); //$NON-NLS-1$

		final ButtonGroup group = new ButtonGroup();
		final UI_LANGUAGE[] languages = UI_LANGUAGE.values();

		for (final UI_LANGUAGE language : languages) {
			final JRadioButtonMenuItem langItem = new JRadioButtonMenuItem(language.getLabel());
			langItem.setName(language.name());
			langItem.addActionListener(actionLanguage);
			menuLanguage.add(langItem);
			group.add(menuLanguage);
			if (localPropertiesService.getUiLanguage().equals(language)) {
				langItem.setSelected(true);
			}

		}

		return menuLanguage;
	}


	private String getLogFilePath() {
		return FolderFinder.getAppDataFolder() + "/earth_error.log"; //$NON-NLS-1$ 
	}

	public JMenuBar getMenu(JFrame frame) {
		// Where the GUI is created:
		JMenuBar menuBar;
		JMenuItem menuItem;

		// Create the menu bar.
		menuBar = new JMenuBar();

		// Build file menu in the menu bar.
		final JMenu fileMenu = new JMenu(Messages.getString("CollectEarthWindow.10")); //$NON-NLS-1$

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.11")); //$NON-NLS-1$
		menuItem.addActionListener(getCloseActionListener());
		fileMenu.add(menuItem);
		menuBar.add(fileMenu);

		// Build tools menu in the menu bar.
		final JMenu toolsMenu = new JMenu(Messages.getString("CollectEarthWindow.12")); //$NON-NLS-1$

		addImportExportMenu(toolsMenu);

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.14")); //$NON-NLS-1$
		menuItem.addActionListener(getSaikuAnalysisActionListener());
		toolsMenu.add(menuItem);


		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.54")); //$NON-NLS-1$
		menuItem.addActionListener( new ApplyOptionChangesListener(this.getFrame(), localPropertiesService) {

			@Override
			protected void applyProperties() {

				try {
					if( kmlImportService.prompToOpenKml( CollectEarthWindow.this.getFrame()) ){
						restartEarth();
					}
				} catch (Exception e1) {
					JOptionPane.showMessageDialog( CollectEarthWindow.this.getFrame(), e1.getMessage(), Messages.getString("CollectEarthWindow.63"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					logger.error("Error importing KML file", e1); //$NON-NLS-1$
				}

			}

		});
		serverMenuItems.add(menuItem); // This menu should only be shown if the DB is local ( not if Collect Earth is acting as a client )
		toolsMenu.add(menuItem);
		
		menuItem = new JMenuItem("Open data folder"); //$NON-NLS-1$
		menuItem.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					CollectEarthUtils.openFolderInExplorer(FolderFinder.getAppDataFolder() );
				} catch (IOException e1) {
					logger.error("Could not findthe data folder", e1 );
				}
				
			}
		});
		serverMenuItems.add(menuItem); // This menu should only be shown if the DB is local ( not if Collect Earth is acting as a client )
		toolsMenu.add(menuItem);


		toolsMenu.addSeparator();

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.15")); //$NON-NLS-1$
		menuItem.addActionListener(getPropertiesAction(frame));
		toolsMenu.add(menuItem);

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.18")); //$NON-NLS-1$
		menuItem.addActionListener(new MissingPlotsListener(frame, localPropertiesService, missingPlotService));
		serverMenuItems.add(menuItem); // This menu should only be shown if the DB is local ( not if Collect Earth is acting as a client )
		toolsMenu.add(menuItem);

		toolsMenu.addSeparator();
		final JMenu languageMenu = getLanguageMenu();
		toolsMenu.add(languageMenu);

		menuBar.add(toolsMenu);

		// Build help menu in the menu bar.
		final JMenu menuHelp = new JMenu(Messages.getString("CollectEarthWindow.16")); //$NON-NLS-1$

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.56")); //$NON-NLS-1$
		menuItem.addActionListener( new OpenAboutDialogListener(frame, Messages.getString("CollectEarthWindow.62")) ); //$NON-NLS-1$
		menuHelp.add(menuItem);


		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.17")); //$NON-NLS-1$
		menuItem.addActionListener(new OpenTextFileListener(frame, getDisclaimerFilePath(), Messages.getString("CollectEarthWindow.4")));//$NON-NLS-1$
		menuHelp.add(menuItem);

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.50")); //$NON-NLS-1$
		menuItem.addActionListener(new OpenUserManualListener());
		menuHelp.add(menuItem);

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.64")); //$NON-NLS-1$
		menuItem.addActionListener(new OpenSupportForum());
		menuHelp.add(menuItem);

		menuHelp.addSeparator();

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.52")); //$NON-NLS-1$
		menuItem.addActionListener(new OpenTextFileListener(frame, getLogFilePath(), Messages.getString("CollectEarthWindow.53"))); //$NON-NLS-1$
		menuHelp.add(menuItem);

		menuHelp.addSeparator();

		menuItem = new JMenuItem(Messages.getString("CollectEarthWindow.51")); //$NON-NLS-1$
		menuItem.addActionListener(new CheckForUpdatesListener());
		menuHelp.add(menuItem);

		menuBar.add(menuHelp);

		return menuBar;
	}

	private String getOperator() {
		return localPropertiesService.getOperator();
	}

	private ActionListener getPropertiesAction(final JFrame owner) {

		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog dialog = new OptionWizard(owner, localPropertiesService, earthProjectsService, backupSqlLiteService.getAutomaticBackUpFolder().getPath(), analysisSaikuService);
				dialog.setVisible(true);
				dialog.pack();
			}
		};

	}


	private ActionListener getSaikuAnalysisActionListener() {
		return new SaikuAnalysisListener(getFrame(), getSaikuStarter());
	}


	private SaikuStarter getSaikuStarter() {
		return new SaikuStarter(analysisSaikuService, getFrame());

	}

	private void initializeMenu() {
		getFrame().setJMenuBar(getMenu(getFrame()));

		disableMenuItems();
	}

	private void initializePanel() {
		final JPanel pane = new JPanel(new GridBagLayout());
		
		final Border raisedetched = BorderFactory.createRaisedBevelBorder();
		pane.setBorder(raisedetched);

		// Handle Drag and Drop of files into the panel
		pane.setTransferHandler( collectEarthTransferHandler );
		
		final GridBagConstraints c = new GridBagConstraints();

		final JTextField operatorTextField = new JTextField(getOperator(), 30);
		if (StringUtils.isBlank(getOperator())) {
			operatorTextField.setBackground(ERROR_COLOR);
		}

		final JLabel operatorTextLabel = new JLabel(Messages.getString("CollectEarthWindow.26"), SwingConstants.CENTER); //$NON-NLS-1$
		operatorTextLabel.setSize(100, 20);

		final JButton updateOperator = new JButton(Messages.getString("CollectEarthWindow.27")); //$NON-NLS-1$
		c.insets = new Insets(3, 3, 3, 3);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(operatorTextLabel, c);

		c.weightx = 0;
		c.gridx = 1;
		c.gridy = 0;
		pane.add(operatorTextField, c);

		c.gridx = 2;
		c.gridy = 0;
		pane.add(updateOperator, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pane.add(new JLabel(Messages.getString("CollectEarthWindow.28") + "<br>" //$NON-NLS-1$ //$NON-NLS-2$
				+ Messages.getString("CollectEarthWindow.30")), c); //$NON-NLS-1$

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		final JButton closeButton = new JButton(Messages.getString("CollectEarthWindow.32")); //$NON-NLS-1$
		closeButton.addActionListener(getCloseActionListener());
		pane.add(closeButton, c);

		getFrame().getContentPane().add(pane);

		updateOperator.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final String operatorName = operatorTextField.getText().trim();
				if (operatorName.length() > 5 && operatorName.length() < 50) {
					localPropertiesService.saveOperator(operatorName);
					operatorTextField.setBackground(Color.white);
				} else {
					JOptionPane.showMessageDialog(getFrame(), Messages.getString("CollectEarthWindow.33"), //$NON-NLS-1$
							Messages.getString("CollectEarthWindow.34"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					operatorTextField.setText(getOperator());
				}

			}
		});
		
		
	}

	private void initializeWindow() {

		// Initialize the translations
		Messages.setLocale(localPropertiesService.getUiLanguage().getLocale());

		// frame.setSize(400, 300);
		getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		getFrame().setResizable(false);
		try {
			getFrame().setIconImage(new ImageIcon(new File("images/smallOpenForisBanner.png").toURI().toURL()).getImage()); //$NON-NLS-1$
		} catch (final MalformedURLException e2) {
			logger.error(Messages.getString("CollectEarthWindow.21"), e2); //$NON-NLS-1$
		}

		addWindowClosingListener();
	}

	protected void openWindow() {

		initializeWindow();
		initializePanel();
		initializeMenu();
		displayWindow();

		if (StringUtils.isBlank(getOperator())) {
			JOptionPane.showMessageDialog(getFrame(), Messages.getString("CollectEarthWindow.35"), //$NON-NLS-1$
					Messages.getString("CollectEarthWindow.36"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		}
		
		changeFrameTitle();
	}

	public void changeFrameTitle() {
		String name = " - No survey loaded";
		if( earthSurveyService.getCollectSurvey() != null ){
			if( !StringUtils.isBlank( earthSurveyService.getCollectSurvey().getProjectName( localPropertiesService.getUiLanguage().getLocale().getLanguage() ) ) ){
				name =  " - " +earthSurveyService.getCollectSurvey().getProjectName( localPropertiesService.getUiLanguage().getLocale().getLanguage() );
			}else if( !StringUtils.isBlank( earthSurveyService.getCollectSurvey().getProjectName() ) ){
				name =  " - " +earthSurveyService.getCollectSurvey().getProjectName();
			}else{
				name =  " - " + earthSurveyService.getCollectSurvey().getDescription( localPropertiesService.getUiLanguage().getLocale().getLanguage() );
			}
		}
		getFrame().setTitle( Messages.getString("CollectEarthWindow.19") + name );
	}

	void setFrame(JFrame frame) {
		this.frame = frame;
	}

	
}
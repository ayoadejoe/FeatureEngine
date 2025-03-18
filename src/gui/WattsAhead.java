package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.MatteBorder;

import cleaning.DuplicateCleaner;
import cleaning.InterpolationEnergyEngine;
import cleaning.LinearInterpolator;
import com.toedter.calendar.JDateChooser;

import connections.ClientConnect;
import connections.FileDownloader;
import functions.ProgressListener;
import functions.TaskPublisher;
import merger.Fuser;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;


public class WattsAhead extends JFrame{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String urlString = null; //"http://eucalyptus.iq-joy.com/joseph1/historical_power.php";
	private String startDate = "2024-10-01 00:00:00";
	private String endDate = "2024-11-30 23:59:59"; // Fixed date issue
	private String outputEnergyFilePath = "Raw_Energy.csv"; // Local file path
	private JProgressBar progressBar = new JProgressBar();

	private final JPanel progressPanel = new JPanel();
	private final JTextArea logTextArea = new JTextArea();
	private final JSplitPane splitPane = new JSplitPane();
	private final JPanel panelFuse = new JPanel();
	private final JButton btnFuse = new JButton("<<FEATURE TRANSFORM>>");
	private final JPanel panel_1 = new JPanel();
	private final JCheckBox chckbxNewCheckBox = new JCheckBox("Use Weather Data");
	private final JButton exportCSVBtn = new JButton("Export CSV");
	private final JPanel panelDate = new JPanel();
	private final JTextField endpointText = new JTextField();
	private final JPanel panel = new JPanel();
	private final JButton btnFetchData = new JButton("Acquire Energy Data");

	private DataTable historicalData = new DataTable(new Color(213, 233, 255));

	private final JLabel lblNewLabel = new JLabel("Start Time & Date");
	private final JLabel lblEndtimeDate = new JLabel("End Time & Date");
	private final JPanel processPanel = new JPanel();
	private final JPanel cleaningPanel = new JPanel();
	private final JButton btnMissingRowsEnergy = new JButton("Clean & Fill Missing Data");
	private final JLabel lblNewLabel_1 = new JLabel("Connect to Server ");
	private final JPanel weatherPanel = new JPanel();
	private final JPanel ecoPanel = new JPanel();
	private final JTextField txtWeatherUrl = new JTextField();

	private final JLabel lblNewLabel_4 = new JLabel("Enter Start Date:");
	private final JLabel lblNewLabel_5 = new JLabel("Enter End Date:");
	private final JProgressBar weatherProgress = new JProgressBar();
	private final JButton btnAcquireWeather = new JButton("Acquire Weather Data");
	private final JLabel energyAvailable = new JLabel("No energy data ");
	private final JLabel lblNewLabel_7 = new JLabel("");
	private final JTextField txtEnterEcoUrl = new JTextField();
	private JPanel pan = new JPanel();
	private final JLabel lblNewLabel_4_1 = new JLabel("Enter Start Date:");

	private final JLabel lblNewLabel_5_1 = new JLabel("Enter End Date:");
	private final JCheckBox chckbxNewCheckBox_1 = new JCheckBox("Use Economic Data");
	private final JButton economicData = new JButton("Acquire Economic Data");
	private final JProgressBar ecoProgressBar = new JProgressBar();
	private final JLabel lblNewLabel_8 = new JLabel("Missing Data Updates");
	private final JTextArea statusEnergy = new JTextArea();
	private final JCheckBox checkEcoNG = new JCheckBox("nigeria");
	private final JCheckBox checkEcoIE = new JCheckBox("ireland");
	private final ButtonGroup buttonGroup = new ButtonGroup();
	// Create JSpinner for Time Selection
	private final SpinnerDateModel timeModel = new SpinnerDateModel();
	private final SpinnerDateModel timeEndModel = new SpinnerDateModel();

	//core time selection
	private JDateChooser startDateChooser = new JDateChooser();
	private JDateChooser endDateChooser = new JDateChooser();
	private Calendar selectedDate = Calendar.getInstance();
	private Calendar selectedTime = Calendar.getInstance();
	private Calendar selectedEndDate = Calendar.getInstance();
	private Calendar selectedEndTime = Calendar.getInstance();
	private String startDateTime, endDateTime = null;
	private final JSpinner timeSpinner = new JSpinner(timeModel);
	private final JSpinner timeEndSpinner = new JSpinner(timeEndModel);

	//weather
	private final JDateChooser weatherStartDate = new JDateChooser();
	private final JDateChooser weatherEndDate = new JDateChooser();
	private Calendar selectedWeatherDate = Calendar.getInstance();
	private Calendar selectedWeatherTime = Calendar.getInstance();
	private Calendar selectedWeatherEndDate = Calendar.getInstance();
	private Calendar selectedWeatherEndTime = Calendar.getInstance();
	private String startWeatherDateTime, endWeatherDateTime = null;

	private final JDateChooser ecoStartDate = new JDateChooser();
	private final JDateChooser ecoEndDate = new JDateChooser();
	private Calendar selectedEcoDate = Calendar.getInstance();
	private Calendar selectedEcoTime = Calendar.getInstance();
	private Calendar selectedEcoEndDate = Calendar.getInstance();
	private Calendar selectedEcoEndTime = Calendar.getInstance();
	private String startEcoDateTime, endEcoDateTime = null;

	private DataTable csvPanel, outputPanel;
	private final JScrollPane scrollStatusEnergy = new JScrollPane();

	private TaskPublisher publisher = new TaskPublisher();
	private final JCheckBox forceFullDay = new JCheckBox("Force 24 Hours");
	private String cleanEnergyFile = "cleaned_"+outputEnergyFilePath;
	private String duplicateCleanFile = "duplicate_clean_data.csv";
	private String finalCleanedFile = "final_trained.csv";
	private final JTextArea statusWeather = new JTextArea("Status");
	private final JButton weatherClean = new JButton("Clean/Fill Missing Data");
	private final JButton ecoClean = new JButton("Clean/Fill Missing Data");
	private final JTextArea statusEco = new JTextArea("Status");

	private  LinearInterpolator interpolator = new LinearInterpolator();
	private String weatherOutputPath = "weather_data_ikeja.csv";
	private String cleanWeatherData = "cleaned_ikeja_weather_data.csv";

	private String ecoOutputPath = "nigeria_economic_data.csv";
	private String cleanEcoData = "clean_nigeria_economic_data.csv";

	private final JTextArea spectacularPrompt = new JTextArea();
	public WattsAhead() {
		System.out.println("Application written by Joseph Ayoade for Cleaning, aggregating, interpolation and transformation of energy availability dataset. 2025");
		txtWeatherUrl.setHorizontalAlignment(SwingConstants.RIGHT);
		txtWeatherUrl.setText("http://tus-project.iq-joy.com/weather_metrics.php");
		txtWeatherUrl.setColumns(10);
		setTitle("ACTI FEATURE ENGINE [<--->]");

		JToolBar toolBar = new JToolBar();
		toolBar.setPreferredSize(new Dimension(13, 50));
		getContentPane().add(toolBar, BorderLayout.NORTH);
		panelDate.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)));
		toolBar.add(panelDate);
		panelDate.setLayout(new GridLayout(0, 2, 0, 0));

		panelDate.setPreferredSize(new Dimension(700, 10));
		startDateChooser.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				System.out.println("Focus lost start date");
				weatherStartDate.setDate(startDateChooser.getDate());
				ecoStartDate.setDate(startDateChooser.getDate());
			}
		});

		startDateChooser.setDate(new Date()); // Set default date to today
		// Create JSpinner for Time Selection
		panelDate.add(startDateChooser);

		startDateChooser.add(timeSpinner, BorderLayout.WEST);
		JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss"); // Time format
		timeSpinner.setEditor(timeEditor);
		timeSpinner.setValue(new Date()); // Default to current time
		lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);

		startDateChooser.add(lblNewLabel, BorderLayout.SOUTH);
		endDateChooser.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				System.out.println("Focus lost end date");
				weatherEndDate.setDate(endDateChooser.getDate());
				ecoEndDate.setDate(endDateChooser.getDate());
			}
		});


		endDateChooser.setDate(new Date());
		panelDate.add(endDateChooser);

		endDateChooser.add(timeEndSpinner, BorderLayout.WEST);
		JSpinner.DateEditor timeEndEditor = new JSpinner.DateEditor(timeEndSpinner, "HH:mm:ss"); // Time format
		timeEndSpinner.setEditor(timeEndEditor);
		timeEndSpinner.setValue(new Date()); // Default to current time
		lblEndtimeDate.setHorizontalAlignment(SwingConstants.CENTER);
		lblEndtimeDate.setFont(new Font("Lucida Grande", Font.PLAIN, 10));

		endDateChooser.add(lblEndtimeDate, BorderLayout.SOUTH);


		toolBar.addSeparator();

		toolBar.add(panel);
		panel.setLayout(new GridLayout(1, 2, 0, 0));
		btnFuse.setBackground(new Color(255, 192, 203));
		panel.add(btnFuse);
		toolBar.addSeparator();
		panelFuse.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)));

		TaskPublisher promptPublisher = new TaskPublisher();
		promptPublisher.subscribe(spectacularPrompt);

		btnFuse.addActionListener(z->{
			promptPublisher.publish("Feature Transformation starts. Three datasets would be used for this fusion...");
			ExecutorService executorService = Executors.newSingleThreadExecutor();

			Future<?> threadProcess = executorService.submit(()->{
				try {
					new Fuser(duplicateCleanFile, cleanWeatherData,
							cleanEcoData, finalCleanedFile, promptPublisher);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});


			//the lambda in the submit is the service we are going to monitor for completion
			Executors.newSingleThreadExecutor().submit(()->{
				promptPublisher.publish("Fusion completed. Loading Data to Table now.");
				try {
					threadProcess.get(300, TimeUnit.SECONDS);
					SwingUtilities.invokeLater(() -> updateUI(finalCleanedFile));
				} catch (InterruptedException e) {
					e.printStackTrace();
					promptPublisher.publish("Error:"+e.getMessage());
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					e.printStackTrace();
					promptPublisher.publish("Error:"+e.getMessage());
					throw new RuntimeException(e);
				} catch (TimeoutException e) {
					e.printStackTrace();
					promptPublisher.publish("Error:"+e.getMessage());
					throw new RuntimeException(e);
				}finally {
					executorService.shutdown();
					promptPublisher.publish("All done!");
				}
			});

		});

		toolBar.add(panelFuse);
		panelFuse.setLayout(new GridLayout(0, 1, 0, 0));

		panelFuse.add(panel_1);
		panel_1.setLayout(new GridLayout(0, 1, 0, 0));
		exportCSVBtn.setBackground(new Color(245, 255, 250));
		exportCSVBtn.addActionListener(p -> {
		// Define the source file path
		java.nio.file.Path sourcePath = java.nio.file.Paths.get(finalCleanedFile);

		// Check if the file exists
		if (!Files.exists(sourcePath)) {
			JOptionPane.showMessageDialog(null,
					"Final Transformed Dataset not found. Please ensure the transformation process has completed.",
					"File Not Found",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Open file chooser for saving
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Final Dataset csv As");
		fileChooser.setSelectedFile(new java.io.File(finalCleanedFile)); // Suggest default name
		int userSelection = fileChooser.showSaveDialog(null);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			java.io.File destinationFile = fileChooser.getSelectedFile();
			try {
				// Copy the file to the selected location
				Files.copy(sourcePath, destinationFile.toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				JOptionPane.showMessageDialog(null,
						"File saved successfully to " + destinationFile.getAbsolutePath(),
						"Save Successful",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(null,
						"Error saving file: " + ex.getMessage(),
						"Save Error",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			}
		});
		panel_1.add(exportCSVBtn);

		getContentPane().add(progressPanel, BorderLayout.SOUTH);
		progressPanel.setLayout(new BorderLayout(0, 0));
		progressPanel.setPreferredSize(new Dimension(200, 100));
		progressBar.setStringPainted(true);
		progressPanel.add(progressBar, BorderLayout.NORTH);

		JScrollPane scrollLog = new JScrollPane();
		logTextArea.setBorder(new TitledBorder(null, "Energy Server Communication Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		logTextArea.setBackground(new Color(250, 240, 230));
		logTextArea.setLineWrap(true);
		logTextArea.setWrapStyleWord(true);
		scrollLog.setViewportView(logTextArea);
		progressPanel.add(scrollLog, BorderLayout.CENTER);

		JScrollPane spectacularPane = new JScrollPane();
		//spectacularPrompt.setPreferredSize(new Dimension(500, 22));
		spectacularPrompt.setLineWrap(true);
		spectacularPrompt.setWrapStyleWord(true);
		spectacularPrompt.setBorder(new TitledBorder(null, "Transformation Notifications", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		spectacularPane.setViewportView(spectacularPrompt);
		spectacularPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		progressPanel.add(spectacularPane, BorderLayout.EAST);
		splitPane.setLeftComponent(historicalData);

		pan.setPreferredSize(new Dimension(300, 200));
		splitPane.setRightComponent(pan);
		splitPane.setDividerSize(2);

		getContentPane().add(splitPane, BorderLayout.CENTER);
		splitPane.setDividerLocation(750);
		processPanel.setBorder(new LineBorder(UIManager.getColor("Button.background"), 6));

		getContentPane().add(processPanel, BorderLayout.WEST);
		processPanel.setLayout(new GridLayout(0, 1, 10, 10));
		cleaningPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Energy Data", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(0, 0, 0)));

		processPanel.add(cleaningPanel);
		GridBagLayout gbl_cleaningPanel = new GridBagLayout();
		gbl_cleaningPanel.columnWidths = new int[]{95, 79, 0};
		gbl_cleaningPanel.rowHeights = new int[]{26, 0, 0, 34, 27, 0, 0, 0};
		gbl_cleaningPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_cleaningPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		cleaningPanel.setLayout(gbl_cleaningPanel);

		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblNewLabel_1.gridwidth = 2;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 0;
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 11));
		cleaningPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		btnMissingRowsEnergy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				publisher.subscribe(statusEnergy);
				//main processing task (executorService) - runs concurrently with the Monitor thread, see below.
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				Future<?> processingTask = executorService.submit(() -> {
					String threadName = Thread.currentThread().getName();
					publisher.publish("Please wait: Aggregation and Interpolation started on " + threadName);

					try {
						// Run InterpolationEnergyEngine
						new InterpolationEnergyEngine(outputEnergyFilePath, cleanEnergyFile, publisher, forceFullDay.isSelected());
						publisher.publish("Interpolation completed on " + threadName);

						// Verify output file before proceeding
						java.nio.file.Path outputPath = java.nio.file.Paths.get(cleanEnergyFile);
						if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
							publisher.publish("Error: Interpolation output file is missing or empty");
							return; // Skip DuplicateCleaner if interpolation failed
						}

						// Run DuplicateCleaner
						new DuplicateCleaner(cleanEnergyFile, cleanWeatherData,cleanEcoData, duplicateCleanFile, publisher);
						publisher.publish("Duplicate cleaning completed on " + threadName);
					} catch (IOException ex) {
						publisher.publish("Error: " + ex.getMessage());
						throw new RuntimeException(ex);
					}

					publisher.publish("Operation completed on " + threadName);
					publisher.flush(); // Ensure all messages are flushed
				});

				// This separates the monitoring logic from the main processing task (executorService), allowing it to run concurrently and wait without blocking the EDT or the original executor
				Executors.newSingleThreadExecutor().submit(() -> {
					try {
						//monitoring waits for the main processing to complete for a max of 10 minutes
						processingTask.get(600, TimeUnit.SECONDS); // Wait for entire task
						// must occur on the EDT to avoid thread-safety issues in Swing. This ensures the UI refreshes only after processing is fully done
						SwingUtilities.invokeLater(() -> updateUI(duplicateCleanFile));
					} catch (TimeoutException ex) {
						executorService.shutdownNow();
						SwingUtilities.invokeLater(() -> publisher.publish("Operation terminated after 10 minutes"));
						System.out.println("Terminated after 10 mins");
					} catch (Exception ex) {
						executorService.shutdownNow();
						SwingUtilities.invokeLater(() -> publisher.publish("Operation failed: " + ex.getMessage()));
						System.out.println("Error: " + ex.getMessage());
						ex.printStackTrace();
					} finally {
						executorService.shutdown();
					}
				});
			}
		});

		GridBagConstraints gbc_endpointText = new GridBagConstraints();
		gbc_endpointText.fill = GridBagConstraints.HORIZONTAL;
		gbc_endpointText.gridwidth = 2;
		gbc_endpointText.insets = new Insets(0, 0, 5, 0);
		gbc_endpointText.gridx = 0;
		gbc_endpointText.gridy = 1;
		cleaningPanel.add(endpointText, gbc_endpointText);
		endpointText.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
		endpointText.setText("http://eucalyptus.iq-joy.com/joseph1/historical_power.php");
		endpointText.setColumns(10);
		GridBagConstraints gbc_btnFetchData = new GridBagConstraints();
		gbc_btnFetchData.anchor = GridBagConstraints.WEST;
		gbc_btnFetchData.insets = new Insets(0, 0, 5, 5);
		gbc_btnFetchData.gridx = 0;
		gbc_btnFetchData.gridy = 2;
		cleaningPanel.add(btnFetchData, gbc_btnFetchData);

		btnFetchData.addActionListener(e -> {
			if (!dateCheck(selectedDate, selectedTime, startDateChooser, endDateChooser, selectedEndDate, selectedEndTime, timeSpinner, timeEndSpinner)) {
				return;
			}
			urlString = endpointText.getText().trim();
			// Validate URL

			if (urlString != null && !isValidURL(urlString)) {
				JOptionPane.showMessageDialog(this, "Invalid URL! Please enter a valid URL starting with http:// or https://", "Error", JOptionPane.ERROR_MESSAGE);
				return; // Stop execution if invalid
			}

			// Format the selected date-time
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateTime = sdf.format(selectedDate.getTime());
			endDateTime = sdf.format(selectedEndDate.getTime());

			System.out.println("Start Date: " + startDateTime);
			System.out.println("End Date: " + endDateTime);

			logTextArea.setText("Start Date Selected: " + startDateTime + "\nEnd Date Selected: " + endDateTime);

			// Proceed with data fetch
			logTextArea.append("\nStarting Download...");

			// Start ClientConnect with a callback that triggers createTable()
			new ClientConnect(urlString, startDateTime, endDateTime, outputEnergyFilePath, logTextArea, energyAvailable, progressBar, () -> {
				// This runs only after the download is complete
				SwingUtilities.invokeLater(() -> {
					logTextArea.append("\nLoading Data into Table...");

					if(csvPanel != null) {
						csvPanel.destroyTable();
					}
					csvPanel = new DataTable(new Color(222, 224, 253, 100));
					csvPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Raw Energy", TitledBorder.CENTER, TitledBorder.TOP, null, null));

					csvPanel.createTable(outputEnergyFilePath);

					splitPane.setLeftComponent(csvPanel); // Update UI
					splitPane.revalidate();
				});
			}).execute();
		});


		btnFetchData.setHorizontalAlignment(SwingConstants.RIGHT);
		btnFetchData.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		GridBagConstraints gbc_energyAvailable = new GridBagConstraints();
		gbc_energyAvailable.insets = new Insets(0, 0, 5, 0);
		gbc_energyAvailable.gridx = 1;
		gbc_energyAvailable.gridy = 2;
		cleaningPanel.add(energyAvailable, gbc_energyAvailable);

		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblNewLabel_8.gridwidth = 2;
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_8.gridx = 0;
		gbc_lblNewLabel_8.gridy = 3;
		cleaningPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);

		GridBagConstraints gbc_scrollStatusEnergy = new GridBagConstraints();
		gbc_scrollStatusEnergy.gridheight = 2;
		gbc_scrollStatusEnergy.fill = GridBagConstraints.BOTH;
		gbc_scrollStatusEnergy.gridwidth = 2;
		gbc_scrollStatusEnergy.insets = new Insets(0, 0, 5, 0);
		gbc_scrollStatusEnergy.gridx = 0;
		gbc_scrollStatusEnergy.gridy = 4;
		cleaningPanel.add(scrollStatusEnergy, gbc_scrollStatusEnergy);
		statusEnergy.setBackground(new Color(255, 255, 224));
		scrollStatusEnergy.setViewportView(statusEnergy);
		statusEnergy.setWrapStyleWord(true);
		statusEnergy.setLineWrap(true);
		scrollStatusEnergy.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		btnMissingRowsEnergy.setAlignmentX(Component.RIGHT_ALIGNMENT);

		GridBagConstraints gbc_btnMissingRowsEnergy = new GridBagConstraints();
		gbc_btnMissingRowsEnergy.fill = GridBagConstraints.BOTH;
		gbc_btnMissingRowsEnergy.insets = new Insets(0, 0, 0, 5);
		gbc_btnMissingRowsEnergy.gridx = 0;
		gbc_btnMissingRowsEnergy.gridy = 6;
		cleaningPanel.add(btnMissingRowsEnergy, gbc_btnMissingRowsEnergy);

		GridBagConstraints gbc_forceFullDay = new GridBagConstraints();
		gbc_forceFullDay.gridx = 1;
		gbc_forceFullDay.gridy = 6;
		forceFullDay.setSelected(true);
		cleaningPanel.add(forceFullDay, gbc_forceFullDay);
		weatherPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Fill Weather", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(0, 0, 0)));

		processPanel.add(weatherPanel);
		GridBagLayout gbl_weatherPanel = new GridBagLayout();
		gbl_weatherPanel.columnWidths = new int[]{145, 0, 0};
		gbl_weatherPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 27, 0};
		gbl_weatherPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_weatherPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		weatherPanel.setLayout(gbl_weatherPanel);

		GridBagConstraints gbc_txtWeatherUrl = new GridBagConstraints();
		gbc_txtWeatherUrl.gridheight = 2;
		gbc_txtWeatherUrl.insets = new Insets(0, 0, 5, 0);
		gbc_txtWeatherUrl.gridwidth = 2;
		gbc_txtWeatherUrl.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtWeatherUrl.gridx = 0;
		gbc_txtWeatherUrl.gridy = 0;
		weatherPanel.add(txtWeatherUrl, gbc_txtWeatherUrl);

		GridBagConstraints gbc_statusWeather = new GridBagConstraints();
		gbc_statusWeather.fill = GridBagConstraints.BOTH;
		gbc_statusWeather.insets = new Insets(0, 0, 5, 5);
		gbc_statusWeather.gridx = 0;
		gbc_statusWeather.gridy = 2;
		statusWeather.setWrapStyleWord(true);
		statusWeather.setBackground(new Color(255, 250, 240));
		weatherPanel.add(statusWeather, gbc_statusWeather);

		GridBagConstraints gbc_weatherClean = new GridBagConstraints();
		gbc_weatherClean.insets = new Insets(0, 0, 5, 0);
		gbc_weatherClean.gridx = 1;
		gbc_weatherClean.gridy = 2;
		weatherClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusWeather.setText("Starting...");
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				executorService.submit(() -> {
					String threadName = Thread.currentThread().getName();
					publisher.subscribe(statusEnergy);
					publisher.publish("Weather Data Cleaning started on " + threadName);
					try {
						interpolator.interpolate(weatherOutputPath, cleanWeatherData);

					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
					publisher.publish("Weather Data Cleaning completed on " + threadName);
				});
				statusWeather.setText("Completed!");
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
						executorService.shutdownNow();
						statusWeather.setText("Error!");
					}
				} catch (InterruptedException f) {
					executorService.shutdownNow();
					statusWeather.setText("Error!");
				}finally {
					{
						SwingUtilities.invokeLater(() -> {
							if(outputPanel != null) {
								outputPanel.destroyTable();
							}
							outputPanel = new DataTable(new Color(213, 233, 255) );
							outputPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
									"Cleaned Energy", TitledBorder.CENTER, TitledBorder.TOP, null, null));
							outputPanel.createTable(cleanWeatherData);
							splitPane.remove(pan);
							splitPane.setRightComponent(outputPanel); // Update UI
							splitPane.setDividerLocation(0.5); // 50% of the split pane size
							splitPane.revalidate();
							statusWeather.setText("View Right Table-->");
						});
					}
				}
			}
		});
		weatherPanel.add(weatherClean, gbc_weatherClean);

		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 3;
		weatherPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);

		GridBagConstraints gbc_weatherStartDate = new GridBagConstraints();
		gbc_weatherStartDate.insets = new Insets(0, 0, 5, 0);
		gbc_weatherStartDate.gridwidth = 2;
		gbc_weatherStartDate.fill = GridBagConstraints.BOTH;
		gbc_weatherStartDate.gridx = 0;
		gbc_weatherStartDate.gridy = 4;
		weatherPanel.add(weatherStartDate, gbc_weatherStartDate);

		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 5;
		weatherPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);

		GridBagConstraints gbc_weatherEndDate = new GridBagConstraints();
		gbc_weatherEndDate.insets = new Insets(0, 0, 5, 0);
		gbc_weatherEndDate.gridwidth = 2;
		gbc_weatherEndDate.fill = GridBagConstraints.BOTH;
		gbc_weatherEndDate.gridx = 0;
		gbc_weatherEndDate.gridy = 6;
		weatherPanel.add(weatherEndDate, gbc_weatherEndDate);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 0;
		gbc_chckbxNewCheckBox.gridy = 7;
		weatherPanel.add(chckbxNewCheckBox, gbc_chckbxNewCheckBox);

		GridBagConstraints gbc_btnAcquireWeather = new GridBagConstraints();
		gbc_btnAcquireWeather.insets = new Insets(0, 0, 5, 0);
		gbc_btnAcquireWeather.gridx = 1;
		gbc_btnAcquireWeather.gridy = 7;
		btnAcquireWeather.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				weatherDataAcquirement();

			}
		});
		weatherPanel.add(btnAcquireWeather, gbc_btnAcquireWeather);

		GridBagConstraints gbc_weatherProgress = new GridBagConstraints();
		gbc_weatherProgress.gridwidth = 2;
		gbc_weatherProgress.fill = GridBagConstraints.HORIZONTAL;
		gbc_weatherProgress.gridx = 0;
		gbc_weatherProgress.gridy = 8;
		weatherProgress.setStringPainted(true);
		weatherPanel.add(weatherProgress, gbc_weatherProgress);
		ecoPanel.setBorder(new TitledBorder(null, "Economic Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		processPanel.add(ecoPanel);
		GridBagLayout gbl_ecoPanel = new GridBagLayout();
		gbl_ecoPanel.columnWidths = new int[]{0, 0, 0};
		gbl_ecoPanel.rowHeights = new int[]{27, 0, 25, 25, 28, 25, 30, 28, 23, 17, 0};
		gbl_ecoPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_ecoPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		ecoPanel.setLayout(gbl_ecoPanel);

		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_7.gridwidth = 2;
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 0;
		lblNewLabel_7.setFont(new Font("Tahoma", Font.BOLD, 13));
		ecoPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);

		GridBagConstraints gbc_txtEnterEcoUrl = new GridBagConstraints();
		gbc_txtEnterEcoUrl.insets = new Insets(0, 0, 5, 0);
		gbc_txtEnterEcoUrl.gridwidth = 2;
		gbc_txtEnterEcoUrl.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtEnterEcoUrl.gridx = 0;
		gbc_txtEnterEcoUrl.gridy = 1;
		txtEnterEcoUrl.setText("http://tus-project.iq-joy.com/economic_metrics.php");
		txtEnterEcoUrl.setHorizontalAlignment(SwingConstants.RIGHT);
		txtEnterEcoUrl.setColumns(10);
		ecoPanel.add(txtEnterEcoUrl, gbc_txtEnterEcoUrl);

		GridBagConstraints gbc_checkEcoNG = new GridBagConstraints();
		gbc_checkEcoNG.insets = new Insets(0, 0, 5, 5);
		gbc_checkEcoNG.gridx = 0;
		gbc_checkEcoNG.gridy = 2;
		buttonGroup.add(checkEcoNG);
		checkEcoNG.setSelected(true);
		ecoPanel.add(checkEcoNG, gbc_checkEcoNG);

		GridBagConstraints gbc_checkEcoIE = new GridBagConstraints();
		gbc_checkEcoIE.insets = new Insets(0, 0, 5, 0);
		gbc_checkEcoIE.gridx = 1;
		gbc_checkEcoIE.gridy = 2;
		buttonGroup.add(checkEcoIE);
		ecoPanel.add(checkEcoIE, gbc_checkEcoIE);

		GridBagConstraints gbc_statusEco = new GridBagConstraints();
		gbc_statusEco.fill = GridBagConstraints.BOTH;
		gbc_statusEco.insets = new Insets(0, 0, 5, 5);
		gbc_statusEco.gridx = 0;
		gbc_statusEco.gridy = 3;
		statusEco.setWrapStyleWord(true);
		statusEco.setBackground(new Color(255, 240, 245));
		ecoPanel.add(statusEco, gbc_statusEco);

		GridBagConstraints gbc_ecoClean = new GridBagConstraints();
		gbc_ecoClean.insets = new Insets(0, 0, 5, 0);
		gbc_ecoClean.gridx = 1;
		gbc_ecoClean.gridy = 3;
		ecoClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusEco.setText("Starting...");
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				executorService.submit(() -> {
					String threadName = Thread.currentThread().getName();
					publisher.subscribe(statusEnergy);

					publisher.publish("Economic Data Cleaning started on " + threadName);
					try {
						interpolator.interpolate(ecoOutputPath, cleanEcoData);

					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
					publisher.publish("Economic Data Cleaning completed on " + threadName);
				});
				statusEco.setText("Completed!");
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
						executorService.shutdownNow();
						statusEco.setText("Error!");
					}
				} catch (InterruptedException f) {
					executorService.shutdownNow();
					statusEco.setText("Error!");
				}finally {
					{
						SwingUtilities.invokeLater(() -> {
							if(outputPanel != null) {
								outputPanel.destroyTable();
							}
							outputPanel = new DataTable(new Color(213, 233, 255) );
							outputPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
									"Cleaned Energy", TitledBorder.CENTER, TitledBorder.TOP, null, null));
							outputPanel.createTable(cleanEcoData);
							splitPane.remove(pan);
							splitPane.setRightComponent(outputPanel); // Update UI
							splitPane.setDividerLocation(0.5); // 50% of the split pane size
							splitPane.revalidate();
							statusEco.setText("View Right Table->");
						});
					}
				}
			}
		});
		ecoPanel.add(ecoClean, gbc_ecoClean);

		GridBagConstraints gbc_lblNewLabel_4_1 = new GridBagConstraints();
		gbc_lblNewLabel_4_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4_1.gridx = 0;
		gbc_lblNewLabel_4_1.gridy = 4;
		ecoPanel.add(lblNewLabel_4_1, gbc_lblNewLabel_4_1);

		GridBagConstraints gbc_ecoStartDate = new GridBagConstraints();
		gbc_ecoStartDate.insets = new Insets(0, 0, 5, 0);
		gbc_ecoStartDate.gridwidth = 2;
		gbc_ecoStartDate.fill = GridBagConstraints.BOTH;
		gbc_ecoStartDate.gridx = 0;
		gbc_ecoStartDate.gridy = 5;

		ecoPanel.add(ecoStartDate, gbc_ecoStartDate);

		GridBagConstraints gbc_lblNewLabel_5_1 = new GridBagConstraints();
		gbc_lblNewLabel_5_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5_1.gridx = 0;
		gbc_lblNewLabel_5_1.gridy = 6;
		ecoPanel.add(lblNewLabel_5_1, gbc_lblNewLabel_5_1);

		GridBagConstraints gbc_ecoEndDate = new GridBagConstraints();
		gbc_ecoEndDate.insets = new Insets(0, 0, 5, 0);
		gbc_ecoEndDate.gridwidth = 2;
		gbc_ecoEndDate.fill = GridBagConstraints.BOTH;
		gbc_ecoEndDate.gridx = 0;
		gbc_ecoEndDate.gridy = 7;
		ecoPanel.add(ecoEndDate, gbc_ecoEndDate);

		GridBagConstraints gbc_chckbxNewCheckBox_1 = new GridBagConstraints();
		gbc_chckbxNewCheckBox_1.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox_1.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox_1.gridx = 0;
		gbc_chckbxNewCheckBox_1.gridy = 8;
		ecoPanel.add(chckbxNewCheckBox_1, gbc_chckbxNewCheckBox_1);

		GridBagConstraints gbc_economicData = new GridBagConstraints();
		gbc_economicData.insets = new Insets(0, 0, 5, 0);
		gbc_economicData.gridx = 1;
		gbc_economicData.gridy = 8;
		economicData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String urlEco = txtEnterEcoUrl.getText();
				if(urlEco.length()<7 && urlEco.contains("http://") ) {
					JOptionPane.showMessageDialog(WattsAhead.this, "Please enter the url in the correct format (http://)");
					return;
				}
				String country = "nigeria";
				if(checkEcoIE.isSelected()) country = checkEcoIE.getActionCommand();

				if (!dateCheck(selectedEcoDate, selectedEcoTime, ecoStartDate, ecoEndDate, selectedEcoEndDate, selectedEcoEndTime, timeSpinner, timeEndSpinner)) {
					return;
				}
				// Format the selected date-time
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				startEcoDateTime = sdf.format(selectedEcoDate.getTime());
				endEcoDateTime = sdf.format(selectedEcoEndDate.getTime());

				System.out.println("Start Date: " + startEcoDateTime);
				System.out.println("End Date: " + endEcoDateTime);

				// Define the URL and default output path
				String url = urlEco+"?start-date="+startEcoDateTime+"&end-date="+endEcoDateTime+"&country="+country;
				String defaultOutputPath = country+"_economic_data.csv";

				// Create a single-thread ExecutorService
				ExecutorService executorService = Executors.newSingleThreadExecutor();

				try {
					// Submit the download task with progress listener
					Future<?> future = executorService.submit(() -> {
						ProgressListener listener = (bytesDownloaded, totalBytes, percentage) -> {
							SwingUtilities.invokeLater(() -> {
								if (percentage >= 0) {
									ecoProgressBar.setValue((int) percentage);
									logTextArea.append(String.format("Downloaded %d of %d bytes (%.2f%%)%n", bytesDownloaded, totalBytes, percentage));
									ecoProgressBar.setString(String.format("Downloaded %d of %d bytes (%.2f%%)%n", bytesDownloaded, totalBytes, percentage));
								} else {
									logTextArea.append(String.format("Downloaded %d bytes (unknown total)%n", bytesDownloaded));
									ecoProgressBar.setString(String.format("Downloaded %d bytes (unknown total)%n", bytesDownloaded));
								}
							});
						};
						try {
							FileDownloader.downloadFile(url, defaultOutputPath, listener);
						} catch (IOException x) {
							SwingUtilities.invokeLater(() -> {
								logTextArea.append("Error: " + x.getMessage() + "\n");
								ecoProgressBar.setString(String.format("Error: " + x.getMessage() + "\n"));
								x.printStackTrace();
							});
						}
					});

					// Optionally wait for the task to complete
					future.get(); // Blocks until done; remove for non-blocking

					SwingUtilities.invokeLater(() -> {
						logTextArea.append("Download completed!\n");
						ecoProgressBar.setValue(100);
						if(csvPanel != null) {
							csvPanel.destroyTable();
						}
						csvPanel = new DataTable(new Color(253, 233, 235, 180));
						csvPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Raw Economic Data", TitledBorder.CENTER, TitledBorder.TOP, null, null));

						csvPanel.createTable(defaultOutputPath);
						splitPane.setLeftComponent(csvPanel); // Update UI
						splitPane.revalidate();
					});
				} catch (Exception g) {
					System.err.println("Exception in executor: " + g.getMessage());
					g.printStackTrace();
				} finally {
					// Shutdown the executor service
					executorService.shutdown();
					try {
						if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
							executorService.shutdownNow();
							System.err.println("Executor did not terminate gracefully.");
						}
					} catch (InterruptedException p) {
						executorService.shutdownNow();
						Thread.currentThread().interrupt();
						System.err.println("Interrupted while waiting for termination: " + p.getMessage());
					}
				}
			}
		});
		ecoPanel.add(economicData, gbc_economicData);
		ecoProgressBar.setStringPainted(true);

		GridBagConstraints gbc_ecoProgressBar = new GridBagConstraints();
		gbc_ecoProgressBar.fill = GridBagConstraints.BOTH;
		gbc_ecoProgressBar.gridwidth = 2;
		gbc_ecoProgressBar.gridx = 0;
		gbc_ecoProgressBar.gridy = 9;
		ecoPanel.add(ecoProgressBar, gbc_ecoProgressBar);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//setLocationRelativeTo(null);
		setSize(1200, 1005);
		setVisible(true);

	}

	private void weatherDataAcquirement() {
		String urlWeather = txtWeatherUrl.getText();
		if(urlWeather.length()<7 && urlWeather.contains("http://") ) {
			JOptionPane.showMessageDialog(WattsAhead.this, "Please enter the url in the correct format (http://)");
			return;
		}

		if (!dateCheck(selectedWeatherDate, selectedWeatherTime, weatherStartDate, weatherEndDate, selectedWeatherEndDate, selectedWeatherEndTime, timeSpinner, timeEndSpinner)) {
			return;
		}
		// Format the selected date-time
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		startWeatherDateTime = sdf.format(selectedWeatherDate.getTime());
		endWeatherDateTime = sdf.format(selectedWeatherEndDate.getTime());

		System.out.println("Start Date: " + startWeatherDateTime);
		System.out.println("End Date: " + endWeatherDateTime);

		// Define the URL and default output path
		String url = urlWeather+"?start-date="+startWeatherDateTime+"&end-date="+endWeatherDateTime;

		weatherProgress.setStringPainted(true);


		// Create a single-thread ExecutorService
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			// Submit the download task with progress listener
			Future<?> future = executorService.submit(() -> {
				ProgressListener listener = (bytesDownloaded, totalBytes, percentage) -> {
					SwingUtilities.invokeLater(() -> {
						if (percentage >= 0) {
							weatherProgress.setValue((int) percentage);
							logTextArea.append(String.format("Downloaded %d of %d bytes (%.2f%%)%n", bytesDownloaded, totalBytes, percentage));
							weatherProgress.setString(String.format("Downloaded %d of %d bytes (%.2f%%)%n", bytesDownloaded, totalBytes, percentage));
						} else {
							logTextArea.append(String.format("Downloaded %d bytes (unknown total)%n", bytesDownloaded));
							weatherProgress.setString(String.format("Downloaded %d bytes (unknown total)%n", bytesDownloaded));
						}
					});
				};
				try {
					FileDownloader.downloadFile(url, weatherOutputPath, listener);
				} catch (IOException x) {
					SwingUtilities.invokeLater(() -> {
						logTextArea.append("Error: " + x.getMessage() + "\n");
						weatherProgress.setString(String.format("Error: " + x.getMessage() + "\n"));
						x.printStackTrace();
					});
				}
			});

			// Optionally wait for the task to complete
			future.get(); // Blocks until done; remove for non-blocking

			SwingUtilities.invokeLater(() -> {
				logTextArea.append("Download completed!\n");
				weatherProgress.setValue(100);
				if(csvPanel != null) {
					csvPanel.destroyTable();
				}
				csvPanel = new DataTable(new Color(213, 253, 233, 150));
				csvPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Raw Weather", TitledBorder.CENTER, TitledBorder.TOP, null, null));

				csvPanel.createTable(weatherOutputPath);
				splitPane.setLeftComponent(csvPanel); // Update UI
				splitPane.revalidate();
			});
		} catch (Exception g) {
			System.err.println("Exception in executor: " + g.getMessage());
			g.printStackTrace();
		} finally {
			// Shutdown the executor service
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
					System.err.println("Executor did not terminate gracefully.");
				}
			} catch (InterruptedException p) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
				System.err.println("Interrupted while waiting for termination: " + p.getMessage());
			}
		}
	}

	private boolean dateCheck(Calendar selectedDate, Calendar selectedTime, JDateChooser startDateChooser, JDateChooser endDateChooser,
							  Calendar selectedEndDate, Calendar selectedEndTime, JSpinner timeSpinner, JSpinner timeEndSpinner) {
		// 1. Merge Date from JDateChooser and Time from JSpinner
		// StartDate
		selectedDate.setTime(startDateChooser.getDate());
		selectedTime.setTime((Date) timeSpinner.getValue());
		selectedDate.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
		selectedDate.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
		selectedDate.set(Calendar.SECOND, selectedTime.get(Calendar.SECOND));

		// EndDate
		selectedEndDate.setTime(endDateChooser.getDate());
		selectedEndTime.setTime((Date) timeEndSpinner.getValue());
		selectedEndDate.set(Calendar.HOUR_OF_DAY, selectedEndTime.get(Calendar.HOUR_OF_DAY));
		selectedEndDate.set(Calendar.MINUTE, selectedEndTime.get(Calendar.MINUTE));
		selectedEndDate.set(Calendar.SECOND, selectedEndTime.get(Calendar.SECOND));

		// Validate date-time selection
		if (!validateDateTimeSelection(selectedDate, selectedEndDate)) {
			return false; // Stop if validation fails
		}

		return true;
	}

	// This method safely updates the UI
	private void updateUI(String filePath) {
		statusEnergy.append("\nLoading Data into Table...");
		if (outputPanel != null) {
			outputPanel.destroyTable();
		}
		outputPanel = new DataTable(new Color(213, 233, 255));
		outputPanel.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)),
				"Cleaned Energy", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		outputPanel.createTable(filePath); // Use "duplicate_clean_data.csv"
		splitPane.remove(pan);
		splitPane.setRightComponent(outputPanel);
		splitPane.setDividerLocation(0.5);
		splitPane.revalidate();
		splitPane.repaint();
	}

	private boolean validateDateTimeSelection(Calendar selectedDate, Calendar selectedEndDate) {
		if (selectedDate == null || selectedEndDate == null) {
			JOptionPane.showMessageDialog(this, "Both start and end date-time must be selected!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// Normalize both dates by removing milliseconds
		selectedDate.set(Calendar.MILLISECOND, 0);
		selectedEndDate.set(Calendar.MILLISECOND, 0);

		long startMillis = selectedDate.getTimeInMillis();
		long endMillis = selectedEndDate.getTimeInMillis();

		if (endMillis == startMillis) { // Directly compare timestamps
			JOptionPane.showMessageDialog(this, "Start and End date-time cannot be the same!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (endMillis < startMillis) { // Ensure end is strictly after start
			JOptionPane.showMessageDialog(this, "End date-time must be after start date-time!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true; // Validation passed
	}

	private boolean isValidURL(String url) {
		String urlRegex = "^(https?|ftp)://[\\w.-]+(?:\\.[\\w.-]+)+[/#?]?.*$";
		Pattern pattern = Pattern.compile(urlRegex);
		Matcher matcher = pattern.matcher(url);
		return matcher.matches();
	}


	public static void main(String[] args) {
		SwingUtilities.invokeLater(()->{
			new WattsAhead(); // Instantiate class to execute request
		});

	}
}

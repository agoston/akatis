import java.lang.Integer;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;

/**********************************************************************************
 * MAIN
 **********************************************************************************/
public class LevelEditor implements ListSelectionListener, ActionListener, ChangeListener {
	JFrame editorFrame;
	JPanel scrollPanel, enemyPanel, balPanel;
	JList dirList, itemList;
	DisplayPanel displayPanel;
	JCheckBox showFileNameCB;
	JMenuItem menuFileNew, menuFileLoad, menuFileSave, menuFileQuit, menuFileSaveAs, menuFileProperties;
	JCheckBoxMenuItem cbGrid, cbBG, cbTile, cbSep;
	JScrollPane scrollPane, itemPane;
	Container editorPanel;
	PropertiesWindow propWindow;
	SpinnerNumberModel layerSpinnerModel;
	JSpinner layerSpinner;
	JTextField speedIn;
	JComboBox stretchIn;
	JSpinner pvWidthSpinner, pvHeightSpinner;
	JButton pvStartStopButton;
	JScrollPane pvScrollPane;
	JPanel pvScrollPanel, jobbPanel;
	PreviewPanel pvPanel;

	JPanel enPanel;
	JComboBox enType;
	JSpinner enPath;
	JTextField endx, endy, enbgpos;
	JTextField actorParams;
	JLabel enProp;
	
	final static Insets ZEROINSETS = new Insets(0, 0, 0, 0);
			
	HashMap images = new HashMap();
	
	String inHand = null;	// melyik kep van a kezeben
	int enHand = -1;		// melyik ellen-tipus van a kezeben
	int inHandNum = 0;
	int selectedLayer;
	int actEnemy;
	
	boolean editEnemy;
	PreviewPanelScrollerThread scrollerThread;
	
	final ImageIcon closedIcon;
	final ImageIcon openIcon;
	final Image transparentImage;
	final Image missingTileImage;
	final Image missingObjectImage;

	EnemyType[] entype = new EnemyType[255];

	/**
	 *  Constructor for the LevelEditor object
	 */
	public LevelEditor() {

		closedIcon = new ImageIcon(getClass().getResource("/folder-closed.png"));
		openIcon = new ImageIcon(getClass().getResource("/folder-open.png"));
		transparentImage = (new ImageIcon(getClass().getResource("/transparent.png"))).getImage();
		missingTileImage = (new ImageIcon(getClass().getResource("/transparent_small.png"))).getImage();
		missingObjectImage = (new ImageIcon(getClass().getResource("/wtf.png"))).getImage();
		
		//Create and set up the window.
		editorFrame = new JFrame("Editor");
		editorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		editorFrame.setSize(new Dimension(900, 900));

		editorPanel = editorFrame.getContentPane();
		GridBagLayout editorGBL = new GridBagLayout();
		editorPanel.setLayout(editorGBL);
		
		//Create and set up the panel.
		balPanel = new JPanel(new GridBagLayout());
		balPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		// konyvtar lista
		File[] subDirs = (new File(".")).listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});
		String[] listElements = new String[subDirs.length];
		for (int i = 0; i < subDirs.length; i++) {
			listElements[i] = subDirs[i].getName();
		}

/******************** BalPanel
 ********************/
		dirList = new JList(listElements);
		dirList.setLayoutOrientation(JList.VERTICAL);
		dirList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dirList.setCellRenderer(new FolderListCellRenderer());
		dirList.addListSelectionListener(this);
		dirList.setFixedCellWidth(120);
		balPanel.add(dirList, new GridBagConstraints(	0, 0, 1, 1,		/** X, Y, Width, Height */
														1.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.HORIZONTAL,	/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */


		showFileNameCB = new JCheckBox("Filenames", false);
		showFileNameCB.addActionListener(this);
		balPanel.add(showFileNameCB, new GridBagConstraints(0, 1, 1, 1,		/** X, Y, Width, Height */
															1.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.HORIZONTAL,	/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */


		itemList = new JList();
		itemPane = new JScrollPane(itemList);
		itemList.setLayoutOrientation(JList.VERTICAL);
		itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		itemList.setCellRenderer(new ItemListCellRenderer());
		itemList.addListSelectionListener(this);
		balPanel.add(itemPane, new GridBagConstraints(	0, 2, 1, 1,		/** X, Y, Width, Height */
														1.0, 1.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.BOTH,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */


		editorPanel.add(balPanel, new GridBagConstraints(	0, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.VERTICAL,	/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
 
		
/******************** MainPanel
 ********************/
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		
		layerSpinnerModel = new SpinnerNumberModel(0, 0, 0, 1);
		layerSpinner = new JSpinner(layerSpinnerModel);
		layerSpinner.addChangeListener(this);
		layerSpinner.setFont(new Font("Verdana", Font.BOLD, 32));
		mainPanel.add(layerSpinner, new GridBagConstraints(	0, 0, 1, 2,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		JLabel lSpeed = new JLabel("Speed:");
		mainPanel.add(lSpeed, new GridBagConstraints(	1, 0, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														new Insets(0, 30, 0, 0),		/** Insets */
														0, 0));							/** pad */

		speedIn = new JTextField(4);
		speedIn.addActionListener(this);
		mainPanel.add(speedIn, new GridBagConstraints(	2, 0, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */
		
		JLabel lStretch = new JLabel("Stretch:");
		mainPanel.add(lStretch, new GridBagConstraints(	1, 1, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														new Insets(0, 30, 0, 0),		/** Insets */
														0, 0));							/** pad */

		String[] stretchData = {"Normal", "Repeat", "Justify"};
		stretchIn = new JComboBox(stretchData);
		stretchIn.addActionListener(this);
		mainPanel.add(stretchIn, new GridBagConstraints(	2, 1, 1, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
															
		displayPanel = new DisplayPanel();
		mainPanel.add(displayPanel, new GridBagConstraints(	0, 2, 4, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */


		scrollPanel = new JPanel();
		scrollPane = new JScrollPane(scrollPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(20, 768));
		scrollPane.setMaximumSize(new Dimension(20, 768));
		scrollPane.getViewport().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				displayPanel.repaint();
			}
		});
		mainPanel.add(scrollPane, new GridBagConstraints(	4, 2, 1, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */

															
		editorPanel.add(mainPanel, new GridBagConstraints(	1, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.VERTICAL,	/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */

/******************** pvPanel
 ********************/
		jobbPanel = new JPanel(new GridBagLayout());
		jobbPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		JLabel tPreviewWidth = new JLabel("Width:");
		jobbPanel.add(tPreviewWidth, new GridBagConstraints(0, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		
		pvWidthSpinner = new JSpinner(new SpinnerNumberModel(128, 128, 256, 16));
		pvWidthSpinner.addChangeListener(this);
		jobbPanel.add(pvWidthSpinner, new GridBagConstraints(	1, 0, 1, 1,		/** X, Y, Width, Height */
																0.2, 0.0, 		/** weight */
																GridBagConstraints.NORTHWEST,	/** anchor */
																GridBagConstraints.NONE,		/** fill */
																new Insets(0, 5, 0, 15),		/** Insets */
																0, 0));							/** pad */
		
		JLabel tPreviewHeight = new JLabel("Height:");
		jobbPanel.add(tPreviewHeight, new GridBagConstraints(	2, 0, 1, 1,		/** X, Y, Width, Height */
																0.0, 0.0, 		/** weight */
																GridBagConstraints.NORTHWEST,	/** anchor */
																GridBagConstraints.NONE,		/** fill */
																ZEROINSETS,						/** Insets */
																0, 0));							/** pad */
		
		pvHeightSpinner = new JSpinner(new SpinnerNumberModel(160, 128, 256, 16));
		pvHeightSpinner.addChangeListener(this);
		jobbPanel.add(pvHeightSpinner, new GridBagConstraints(	3, 0, 1, 1,		/** X, Y, Width, Height */
																0.2, 0.0, 		/** weight */
																GridBagConstraints.NORTHWEST,	/** anchor */
																GridBagConstraints.NONE,		/** fill */
																new Insets(0, 5, 0, 15),		/** Insets */
																0, 0));							/** pad */

		pvStartStopButton = new JButton(">>");
		pvStartStopButton.addActionListener(this);
		jobbPanel.add(pvStartStopButton, new GridBagConstraints(	0, 1, 2, 1,		/** X, Y, Width, Height */
																	0.0, 0.0, 		/** weight */
																	GridBagConstraints.NORTHWEST,	/** anchor */
																	GridBagConstraints.NONE,		/** fill */
																	ZEROINSETS,						/** Insets */
																	0, 0));							/** pad */
		
		
		pvScrollPanel = new JPanel();
		pvScrollPane = new JScrollPane(pvScrollPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		pvScrollPane.setPreferredSize(new Dimension(256, 20));
		pvScrollPane.setMaximumSize(new Dimension(256, 20));
		pvScrollPane.getViewport().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				pvPanel.repaint();
			}
		});
		jobbPanel.add(pvScrollPane, new GridBagConstraints(	0, 2, 5, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		pvPanel = new PreviewPanel();
		jobbPanel.add(pvPanel, new GridBagConstraints(	0, 3, 5, 1,		/** X, Y, Width, Height */
														1.0, 0.1, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.BOTH,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */
		
		
		editorPanel.add(jobbPanel, new GridBagConstraints(	2, 0, 1, 1,		/** X, Y, Width, Height */
																0.0, 1.0, 		/** weight */
																GridBagConstraints.NORTHWEST,	/** anchor */
																GridBagConstraints.VERTICAL,	/** fill */
																ZEROINSETS,						/** Insets */
																0, 0));							/** pad */

																
/******************** EnemyPanel
 ********************/
		enPanel = new JPanel(new GridBagLayout());
		enPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Enemy properties"));

		JLabel tEnemyType = new JLabel("Type:");
		enPanel.add(tEnemyType, new GridBagConstraints(0, 0, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		enType = new JComboBox();
		enType.setRenderer(new EnemyTypeCellRenderer());
		enType.addActionListener(this);
		enPanel.add(enType, new GridBagConstraints(1, 0, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		
		JLabel tEnemyPath = new JLabel("Path:");
		enPanel.add(tEnemyPath, new GridBagConstraints(0, 1, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */


		enPath = new JSpinner(new SpinnerNumberModel(0, 0, 102, 1));
		enPath.addChangeListener(this);
		enPanel.add(enPath, new GridBagConstraints(1, 1, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		JLabel tEnemyBGPos = new JLabel("bgPos:");
		enPanel.add(tEnemyBGPos, new GridBagConstraints(0, 2, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		enbgpos = new JTextField(4);
		enbgpos.addActionListener(this);
		enPanel.add(enbgpos, new GridBagConstraints(1, 2, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		JLabel tEnemydx = new JLabel("dx:");
		enPanel.add(tEnemydx, new GridBagConstraints(0, 3, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		endx = new JTextField(4);
		endx.addActionListener(this);
		enPanel.add(endx, new GridBagConstraints(1, 3, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		JLabel tEnemydy = new JLabel("dy:");
		enPanel.add(tEnemydy, new GridBagConstraints(0, 4, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		endy = new JTextField(4);
		endy.addActionListener(this);
		enPanel.add(endy, new GridBagConstraints(1, 4, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		enProp = new JLabel("");
		enPanel.add(enProp, new GridBagConstraints(0, 5, 2, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		JLabel tActorParams = new JLabel("Actor Params:");
		enPanel.add(tActorParams, new GridBagConstraints(0, 6, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */

		actorParams = new JTextField(12);
		actorParams.addActionListener(this);
		enPanel.add(actorParams, new GridBagConstraints(1, 6, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */

		jobbPanel.add(enPanel, new GridBagConstraints(	0, 4, 5, 1,		/** X, Y, Width, Height */
														1.0, 0.1, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.BOTH,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */
		
		// glue
		jobbPanel.add(new JPanel(), new GridBagConstraints(	0, 5, 5, 1,		/** X, Y, Width, Height */
														1.0, 1.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.BOTH,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */
		
	
/******************** GLUE
 ********************/
		editorPanel.add(new JPanel(), new GridBagConstraints(	3, 0, 1, 1,		/** X, Y, Width, Height */
																0.1, 1.0, 		/** weight */
																GridBagConstraints.NORTHWEST,	/** anchor */
																GridBagConstraints.BOTH,		/** fill */
																ZEROINSETS,						/** Insets */
																0, 0));							/** pad */

																
/******************** MenuBar
 ********************/
		JMenuBar menuBar = new JMenuBar();
		JMenu menuFile = new JMenu("File");
		menuBar.add(menuFile);
		
		menuFileNew = new JMenuItem("New");
		menuFile.add(menuFileNew);
		menuFileNew.addActionListener(this);
		menuFile.addSeparator();

		menuFileLoad = new JMenuItem("Load");
		menuFile.add(menuFileLoad);
		menuFileLoad.addActionListener(this);

		menuFileSave = new JMenuItem("Save");
		menuFile.add(menuFileSave);
		menuFileSave.addActionListener(this);

		menuFileSaveAs = new JMenuItem("Save as...");
		menuFile.add(menuFileSaveAs);
		menuFileSaveAs.addActionListener(this);
		menuFile.addSeparator();

		menuFileProperties = new JMenuItem("Properties...");
		menuFile.add(menuFileProperties);
		menuFileProperties.addActionListener(this);
		menuFile.addSeparator();

		menuFileQuit = new JMenuItem("Quit");
		menuFile.add(menuFileQuit);
		menuFileQuit.addActionListener(this);

		// grid
		menuBar.add(Box.createHorizontalStrut(20));
		cbGrid = new JCheckBoxMenuItem("Grid", true);
		cbGrid.addActionListener(this);
		cbGrid.setMaximumSize(new Dimension(48, 38));
		menuBar.add(cbGrid);

		// tile
		menuBar.add(Box.createHorizontalStrut(5));
		cbTile = new JCheckBoxMenuItem("Tile", true);
		cbTile.addActionListener(this);
		cbTile.setMaximumSize(new Dimension(48, 38));
		menuBar.add(cbTile);

		// BG
		menuBar.add(Box.createHorizontalStrut(5));
		cbBG = new JCheckBoxMenuItem("BG", true);
		cbBG.addActionListener(this);
		cbBG.setMaximumSize(new Dimension(40, 38));
		menuBar.add(cbBG);

		// Separator
		menuBar.add(Box.createHorizontalStrut(5));
		cbSep = new JCheckBoxMenuItem("Sep", true);
		cbSep.addActionListener(this);
		cbSep.setMaximumSize(new Dimension(40, 38));
		menuBar.add(cbSep);

		menuBar.add(Box.createHorizontalGlue());
		
		editorFrame.setJMenuBar(menuBar);
		dirList.setSelectedIndex(0);	// generates a list-updated event

		//Display the window.
		//editorFrame.pack();
		editorFrame.setVisible(true);
		
		// convenience open
		actionPerformed(new ActionEvent(menuFileLoad, 0, "command"));
	}
	
	void updateItemList(String dir) {
		// konyvtar lista
		editEnemy = dir.equals("en");
		setActEnemy(-1);
		if (editEnemy) {
			loadEnemyData(dir);
			itemList.setCellRenderer(new EnemyTypeCellRenderer());
			ArrayList al = new ArrayList();
			for (int i = 0; i < entype.length; i++) {
				if (entype[i] != null) al.add(i);
			}
			Object[] temp = al.toArray();
			itemList.setListData(temp);

		} else {
		
			File[] subDirs = (new File(dir)).listFiles(
				new FileFilter() {
					public boolean accept(File file) {
						boolean ret = file.isFile() && file.getName().toLowerCase().endsWith(".png");
						if (editEnemy) ret = ret && file.getName().startsWith("e");
						return ret;
					}
				});

			String[] listElements = new String[subDirs.length];
			for (int i = 0; i < subDirs.length; i++) {
				String actElem = "/"+dir+"/"+subDirs[i].getName();
				listElements[i] = actElem;

				// toltsuk be
				Image actIm = Toolkit.getDefaultToolkit().createImage(actElem.substring(1));
				images.put(actElem, actIm);
			}
			
			itemList.setCellRenderer(new ItemListCellRenderer());
			itemList.setListData(listElements);
			editorPanel.validate();
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == dirList) {
			if (e.getValueIsAdjusting()) return;
			String dir = dirList.getSelectedValue().toString();
			updateItemList(dir);
			updateBackgroundImages();
			
		} else if (e.getSource() == itemList) {
			if (!itemList.isSelectionEmpty()) {
				inHandNum = Integer.MAX_VALUE;
				setActEnemy(-1);

				if (editEnemy) {
					enHand = (Integer)(itemList.getSelectedValue());
					inHand = null;
				} else {
					inHand = itemList.getSelectedValue().toString();
					enHand = -1;
				}
			}
		}
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == layerSpinner) {
			selectedLayer = ((Number)layerSpinnerModel.getValue()).intValue();
			String speedString = Double.toString(Math.round(bgSpeed[selectedLayer]/256.0*100.0)/100.0);
			if (speedString.length() > 4) speedString = speedString.substring(0, 4);
			speedIn.setText(speedString);
			stretchIn.setSelectedIndex(bgStretch[selectedLayer]);
			displayPanel.repaint();
			
		} else if (e.getSource() == pvWidthSpinner) {
			pvPanel.setWidth(((Number)pvWidthSpinner.getValue()).intValue());
			jobbPanel.repaint();
			
		} else if (e.getSource() == pvHeightSpinner) {
			pvPanel.setHeight(((Number)pvHeightSpinner.getValue()).intValue());
			jobbPanel.repaint();
		} else if (e.getSource() == enPath) {
			enemies.get(actEnemy).path = ((Number)enPath.getValue()).intValue();
		}
	}
	
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == showFileNameCB) {
			valueChanged(new ListSelectionEvent(dirList, 0, 0, false));

		} else if (o == menuFileNew) {
			newLevel();

		} else if (o == menuFileLoad) {
			JFileChooser chooser = new JFileChooser(".");
			int returnVal = chooser.showOpenDialog(editorFrame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String levelPath = chooser.getSelectedFile().getAbsolutePath();
				try {
					loadLevel(levelPath);
				} catch (Exception e) {e.printStackTrace();}
			}

		} else if (o == menuFileSave) {
			saveLevel();

		} else if (o == menuFileSaveAs) {
			saveLevelAs();

		} else if (o == menuFileProperties) {
			propWindow = new PropertiesWindow();

		} else if (o == menuFileQuit) {
			System.exit(0);

		} else if (o == cbGrid || o == cbBG || o == cbTile || o == cbSep) {
			displayPanel.repaint();
			
		} else if (o == speedIn) {
			bgSpeed[selectedLayer] = (int)(Double.parseDouble(speedIn.getText())*256);
			recalcLevelLength();

		} else if (o == pvStartStopButton) {
			if (pvStartStopButton.getText().equals(">>")) {
				pvStartStopButton.setText("||");
				scrollerThread = new PreviewPanelScrollerThread();
				scrollerThread.start();
			} else {
				pvStartStopButton.setText(">>");
				scrollerThread.interrupt();
			}
			
		} else if (o == enType) {
			enemies.get(actEnemy).type = ((Number)enType.getSelectedItem()).intValue();
			displayPanel.repaint();
			
		} else if (o == enbgpos) {
			enemies.get(actEnemy).bgPos = Integer.parseInt(enbgpos.getText());
			displayPanel.repaint();

        } else if (o == endx) {
			enemies.get(actEnemy).dx = Integer.parseInt(endx.getText());
			displayPanel.repaint();
			
		} else if (o == endy) {
			enemies.get(actEnemy).dy = Integer.parseInt(endy.getText());
			
		} else if (o == actorParams) {
			String[] par = actorParams.getText().split(",");
			Enemy acten = enemies.get(actEnemy);
			acten.actorData = new byte[par.length];
			for (int i = 0; i < par.length; i++) acten.actorData[i] = Byte.parseByte(par[i].trim());
			
		} else if (o == stretchIn) {
			bgStretch[selectedLayer] = stretchIn.getSelectedIndex();
			pvPanel.repaint();
			
		}
	}

	private static void createAndShowGUI() {
		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);

		LevelEditor le = new LevelEditor();
	}

	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					createAndShowGUI();
				}
			});
	}


/**********************************************************************************
 * CELLRENDERER
 **********************************************************************************/
	class FolderListCellRenderer extends JLabel implements ListCellRenderer {
	
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String s = value.toString();
			setText(s);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				setIcon(openIcon);
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
				setIcon(closedIcon);
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}

/**********************************************************************************/
	class ItemListCellRenderer extends JLabel implements ListCellRenderer {
	
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String s = value.toString();
			ImageIcon icon = new ImageIcon((Image)(images.get(s)));
			setIcon(icon);

			if (showFileNameCB.isSelected()) {
				setText(s.substring(s.lastIndexOf('/')+1, s.length()-4));
				setBorder(null);
			} else {
				setText(null);
				if (isSelected) {
					setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red));
				} else {
					setBorder(null);
				}
			}
			
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}

/**********************************************************************************/
	class ImageListCellRenderer extends JLabel implements ListCellRenderer {
	
		final HashMap images;
		public ImageListCellRenderer(HashMap _images) {
			super();
			images = _images;
		}
		
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Image actIm = (Image)images.get(value);
			if (actIm == null) actIm = missingObjectImage;	// just in case
			ImageIcon icon = new ImageIcon(actIm);
			setIcon(icon);
			if (value != null) setText(value.toString());

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}

/**********************************************************************************/
	class EnemyTypeCellRenderer extends JLabel implements ListCellRenderer {
	
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			if (value != null) {
				EnemyType actent = (EnemyType)entype[(Integer)value];

				Image actIm = enemyImages.get(actent.imageNum);
				if (actIm == null) actIm = missingObjectImage;	// just in case
				ImageIcon icon = new ImageIcon(actIm);
				setIcon(icon);
				setText(actent.toString());
			}

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}
	
/**********************************************************************************
/* LevelLoader
/**********************************************************************************/
	static final int ST_NORMAL = 0;
	static final int ST_REPEAT = 1;
	static final int ST_JUSTIFY = 2;
	
	HashMap<Integer, Image> enemyImages = new HashMap<Integer, Image>();
	static final int[] enemyImagesnr = {2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 5, 2, 2, 1, 1, 1, 1};
	ArrayList<Enemy> enemies = new ArrayList<Enemy>();
	Image[] bgImages;
	byte[][] bg = new byte[10][];
	int[] bgSpeed = new int[10];
	int[] bgStretch = new int[10];
	int bgNum;
	int bgLen;
	ArrayList trl;
	String levelPath;
	String levelName;
	String levelPathAbs;
	
	void initLevel() {
		if (levelName == null || levelName.length() == 0) {
			levelName = "untitled";
		}
		if (levelPath == null || levelPath.length() == 0) {
			levelPath = ".";
			levelPathAbs = "."+File.separator+"untitled";
		}
		editorFrame.setTitle(levelName);
		layerSpinnerModel.setMaximum(new Integer(bgNum-1));
		layerSpinnerModel.setValue(new Integer(0));
		recalcLevelLength();
	}
	
	void newLevel() {
		if (JOptionPane.showConfirmDialog(editorFrame, "Vigyazz, kitorli az egesz palyat!\nEzt akarod?", "ZOH", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
			bgImages = new Image[0];
			bg = new byte[10][];
			bgSpeed = new int[10];
			bgNum = 1;
			bg[0] = new byte[120];
			Arrays.fill(bg[0], (byte)-1);
			bgSpeed[0] = 256;
			bgStretch[0] = ST_NORMAL;
			trl = new ArrayList();
			enemies.clear();
			initLevel();
		}
	}
	
	void updateBackgroundImages() {
		if (trl != null && trl.size() > 0) {
			for (int i = 0; i < bgImages.length; i++) {
				Image actIm = (Image)images.get(trl.get(i));
				if (actIm == null) {
					actIm = (new ImageIcon(levelPath+trl.get(i))).getImage();
					System.out.println("Loading "+i);
				}
				bgImages[i] = actIm;
			}
		}
	}
	
	void loadLevel(String _levelPathAbs) throws IOException {
		levelPathAbs = _levelPathAbs;
		
		levelPath = levelPathAbs.substring(0, levelPathAbs.lastIndexOf(File.separator));
		levelName = levelPathAbs.substring(levelPathAbs.lastIndexOf(File.separator)+1);
		{
			Vector v = new Vector(20,10);
			InputStream is = new FileInputStream(levelPathAbs+"trl");
			trl = new ArrayList();
		
			for (int i = 0; ; i++) {
				String imageFile = readLine(is);
				if (imageFile.length() <= 1) break;
				if (!imageFile.startsWith("/")) imageFile = "/"+imageFile;
				trl.add(imageFile);
				v.addElement((new ImageIcon(levelPath+imageFile)).getImage());
			}
			bgImages = new Image[v.size()];
			v.copyInto(bgImages);
		}

		{
			try {
				InputStream is = new FileInputStream(levelPathAbs);
    			enemies.clear();
				bgNum = is.read();
				for (int i = 0; i < bgNum; i++) {
					int len = is.read();
					if (len == -1) break;
					len += is.read()<<8;
					
					int speed = is.read();
					speed += is.read()<<8;
					
					int actstretch = is.read();
					
					byte[] actbg = new byte[len];
					is.read(actbg);
					
					bg[i] = actbg;
					bgSpeed[i] = speed;
					bgStretch[i] = actstretch;
				}
				
				int enNum = is.read();
				for (int i = 0; i < enNum; i++) {
					int type = is.read();
					int bgPos = is.read();
					bgPos += is.read()<<8;
					
					if (type >= 64) {
						byte[] actorData = null;
						switch (type) {
							case 64:
								actorData = new byte[2];
								actorData[0] = (byte)is.read();
								actorData[1] = (byte)is.read();
								break;
							
							case 66:
								actorData = new byte[bgNum*2];
								for (int j = 0; j < actorData.length; j++) 
									actorData[j] = (byte)is.read();
								break;
						}
						
						enemies.add(new Enemy(type, bgPos, actorData));
						
					} else {
						int path = is.read();
						int dx = is.read();
						int dy = is.read();
					
						enemies.add(new Enemy(type, bgPos, path, dx, dy));
					}
				}
				
				
			} catch (Exception e) {}
		}

		initLevel();
	}
	
	void optimizeLevel() {
		ArrayList<Integer> todel = new ArrayList<Integer>();
		
		// kereses
main:	for (int i = 0; i < bgImages.length; i++) {
			for (int j = 0; j < bgNum; j++) {
				for (int k = 0; k < bg[j].length; k++) {
					if (bg[j][k] == i) continue main;
				}
			}
			
			todel.add(i);
		}
		
		// javitas
		ArrayList bgIm = new ArrayList(Arrays.asList(bgImages));
		
		for (int i = 0; i < todel.size(); i++) {
			int delIndex = todel.get(i);
			
			trl.set(delIndex, null);
			bgIm.set(delIndex, null);
			
			for (int j = 0; j < bgNum; j++) {
				for (int k = 0; k < bg[j].length; k++) {
					if (bg[j][k] > delIndex) bg[j][k]--;
				}
			}
		}
		
		// torles
		for (int i = 0; i < trl.size(); i++) {
			if (trl.get(i) == null) {
				trl.remove(i);
				bgIm.remove(i);
				i--;
			}
		}
		
		bgImages = new Image[bgIm.size()];
		bgIm.toArray(bgImages);
	}		
	
	void saveLevel() {
		if (levelPathAbs == null || levelPathAbs.length() == 0) {
			saveLevelAs();
			return;
		}
		
		optimizeLevel();
		
		try {
			// trl
			FileWriter fw = new FileWriter(levelPathAbs+"trl");
			for (int i = 0; i < trl.size(); i++) {
				String writtenLine = trl.get(i).toString();
				if (!writtenLine.startsWith("/")) writtenLine = "/"+writtenLine;
				fw.write(writtenLine);
				fw.write("\n");
			}
			fw.close();
		
			// level
			OutputStream os = new FileOutputStream(levelPathAbs);
			os.write(bgNum);
			for (int i = 0; i < bgNum; i++) {
				os.write(bg[i].length&0xff);
				os.write(bg[i].length>>8);
				os.write(bgSpeed[i]&0xff);
				os.write(bgSpeed[i]>>8);
				os.write(bgStretch[i]);
				os.write(bg[i]);
			}
			
			// enemy
			os.write(enemies.size());
			Enemy[] ensort = new Enemy[enemies.size()];
			enemies.toArray(ensort);
			Arrays.sort(ensort, new EnemyComparator());
			for (Enemy acten: ensort) {
				os.write(acten.type);
				os.write(acten.bgPos&0xff);
				os.write(acten.bgPos>>8);
				
				if (acten.type >= 64) {
					switch (acten.type) {
						case 64:
							os.write(acten.actorData[0]);
							os.write(acten.actorData[1]);
							break;

						case 66:
							for (int j = 0; j < bgNum*2; j++) 
								if (acten.actorData != null && j < acten.actorData.length) {
									os.write(acten.actorData[j]);
								} else {
									os.write(0);
								}
							break;
					}
					
				} else {
					os.write(acten.path);
					os.write(acten.dx);
					os.write(acten.dy);
				}
			}
					
			os.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(editorFrame, "Error saving level:\n"+e);
		}
	}
	
	void recalcLevelLength() {
		bgLen = 0;
		for (int i=0; i<bgNum; i++) {
			int actbgpx = bg[i].length/12*32;
			int actbgLen = ((actbgpx)<<8)/bgSpeed[i];
			
			System.out.println("Loading layer "+i+", length: "+actbgLen);
			if (bgLen < actbgLen) bgLen = actbgLen;
		}
		
		System.out.println("Level length: "+bgLen);
		scrollPanel.setMinimumSize(new Dimension(1, Math.round(bgLen)+768));
		scrollPanel.setPreferredSize(new Dimension(1, Math.round(bgLen)+768));
		scrollPanel.setMaximumSize(new Dimension(1, Math.round(bgLen)+768));
		scrollPanel.revalidate();
		
		pvScrollPanel.setMinimumSize(new Dimension(Math.round(bgLen*16)+256, 1));
		pvScrollPanel.setPreferredSize(new Dimension(Math.round(bgLen*16)+256, 1));
		pvScrollPanel.setMaximumSize(new Dimension(Math.round(bgLen*16)+256, 1));
		pvScrollPanel.revalidate();
	}
	
	void saveLevelAs() {
		levelPathAbs = JOptionPane.showInputDialog("No, mi legyen a palya neve?", levelName==null?"":levelName);

		if (levelPathAbs != null && levelPathAbs.length() > 0) {
			int sepPos = levelPathAbs.lastIndexOf(File.separator);
			if (sepPos >= 0) {
				levelPath = levelPathAbs.substring(0, sepPos);
				levelName = levelPathAbs.substring(levelPathAbs.lastIndexOf(File.separator)+1);
			} else {
				levelPath = ".";
				levelName = levelPathAbs;
				levelPathAbs = "."+File.separator+levelName;
			}
			
			editorFrame.setTitle(levelName);
			saveLevel();
		}
	}
	
	String readLine(InputStream is) throws IOException {
		byte[] buf = new byte[256];
		int index = 0;
		for (buf[index] = (byte)is.read(); buf[index] > 0 && buf[index] != '\n'; buf[++index] = (byte)is.read());
		return new String(buf, 0, index);
	}
	
/**********************************************************************************
/* Level Manipulator
/**********************************************************************************/
	int translateToLevelY(int selectedLayer, int screenY) {
		return getBgPos(selectedLayer)+768-screenY;
	}
	
	int getBgPos(int selectedLayer) {
		return ((scrollPane.getViewport().getViewPosition().y*bgSpeed[selectedLayer])>>8) - 384;
	}
	
	String removeTile(int layer, int x, int y) {
		int trlIndex = bg[layer][y*12+x];
		bg[layer][y*12+x] = -1;
		if (trlIndex >= 0 && trlIndex < trl.size()) {
			return (String)(trl.get(trlIndex));
		} else {
			return null;
		}
	}

	int loadTile(String tileName) {
		int retIndex = trl.indexOf(tileName); 
		if (retIndex >= 0) return retIndex;
			
		System.out.println("Loading "+tileName);
		Image newImage = (new ImageIcon(levelPath+"/"+tileName)).getImage();
		
		retIndex = trl.size();
		trl.add(tileName);
		Image[] temp = new Image[bgImages.length+1];
		System.arraycopy(bgImages, 0, temp, 0, bgImages.length);
		temp[bgImages.length] = newImage;
		bgImages = temp;
		
		return retIndex;
	}
	
	void setTile(int layer, int x, int y, String tileName) {
		int trlIndex = loadTile(tileName);
		bg[layer][y*12+x] = (byte)(trlIndex);
	}
	
	void insertRow(int selectedLayer, int posY) {
		// masolat keszitese
		byte[] tempbg = new byte[bg[selectedLayer].length+12];
		Arrays.fill(tempbg, (byte)-1);
		
		if (posY <= 0) {
			System.arraycopy(bg[selectedLayer], 0, tempbg, 12, bg[selectedLayer].length);
		} else if (posY >= bg[selectedLayer].length/12) {
			System.arraycopy(bg[selectedLayer], 0, tempbg, 0, bg[selectedLayer].length);
		} else {
			System.arraycopy(bg[selectedLayer], 0, tempbg, 0, posY*12);
			System.arraycopy(bg[selectedLayer], posY*12, tempbg, (posY+1)*12, bg[selectedLayer].length-(posY*12));
		}
		
		bg[selectedLayer] = tempbg;
		recalcLevelLength();
	}
	
	void deleteRow(int selectedLayer, int posY) {
		if (posY < 0 || posY >= bg[selectedLayer].length/12) return;

		byte[] tempbg = new byte[bg[selectedLayer].length-12];
		System.arraycopy(bg[selectedLayer], 0, tempbg, 0, posY*12);
		System.arraycopy(bg[selectedLayer], (posY+1)*12, tempbg, posY*12, bg[selectedLayer].length-((posY+1)*12));
		bg[selectedLayer] = tempbg;
		recalcLevelLength();
	}

/**********************************************************************************
/* Enemy
/**********************************************************************************/
	Image getEnemyImage(int type) {
        Image ret = null;
		try {
            ret = enemyImages.get(entype[type].imageNum);
        } catch (NullPointerException e) {
			JOptionPane.showMessageDialog(editorFrame, "Enemy type "+type+" not defined, but used in level!");
            System.exit(1);
        }
		if (ret == null) return missingObjectImage;
		return ret;
	}
	
	void loadEnemyData(String dir) {
		Arrays.fill(entype, null);
		try {
			LineNumberReader in = new LineNumberReader(new FileReader("entypes"));
			String actLine = in.readLine();
			for (int i = 0; actLine != null; actLine = in.readLine(), i++) {
				String[] sp = actLine.split(",");
				if (sp.length != 6)
					throw new Error("entypes incorrect at line "+in.getLineNumber());

				int[] isp = new int[sp.length];
				for (int j = 0; j < sp.length; j++) isp[j] = Integer.parseInt(sp[j].trim());

				entype[i] = new EnemyType(isp[0], isp[1], isp[2], isp[3], isp[4], isp[5]);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(editorFrame, e.toString());
			e.printStackTrace();
			System.exit(1);
		}

		// actors
		entype[64] = new EnemyType(64, 0, 0, 0, 0, 0);
		entype[65] = new EnemyType(65, 0, 0, 0, 0, 0);
		entype[66] = new EnemyType(66, 0, 0, 0, 0, 0);
		entype[67] = new EnemyType(67, 0, 0, 0, 0, 0);
		
		enemyImages.clear();
		
		File[] subDirs = (new File(dir)).listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					return file.isFile() && file.getName().toLowerCase().startsWith("e") && file.getName().toLowerCase().endsWith(".png");
				}
			});

		for (File f : subDirs) {
			String actel = f.getName();
			Image actIm = Toolkit.getDefaultToolkit().createImage(dir+"/"+actel);
			Integer actNum = new Integer(actel.substring(1, actel.length()-4));
			enemyImages.put(actNum, actIm);
		}
		
		ArrayList al = new ArrayList();
		for (int i = 0; i < entype.length; i++) {
			if (entype[i] != null && i < 64) al.add(i);
		}
		Object[] temp = al.toArray();
		itemList.setListData(temp);
		enType.setModel(new DefaultComboBoxModel(temp));
	}
	
	void setActEnemy(int actennum) {
		actEnemy = actennum;
		if (actennum < 0) {
			enType.setEnabled(false);
			enPath.setEnabled(false);
			enbgpos.setEnabled(false);
			endx.setEnabled(false);
			endy.setEnabled(false);
			actorParams.setEnabled(false);
		} else {
			Enemy acten = enemies.get(actennum);
			if (acten.type < 64) {
				enType.setEnabled(true);
				enPath.setEnabled(true);
				enbgpos.setEnabled(true);
				endx.setEnabled(true);
				endy.setEnabled(true);
				actorParams.setEnabled(false);

				enType.setSelectedItem(acten.type);
				enPath.setValue(acten.path);
                enbgpos.setText(Integer.toString(acten.bgPos));
				endx.setText(Integer.toString(acten.dx));
				endy.setText(Integer.toString(acten.dy));

			} else {
				enType.setEnabled(false);
				enPath.setEnabled(false);
    			enbgpos.setEnabled(true);
				endx.setEnabled(false);
				endy.setEnabled(false);
				actorParams.setEnabled(true);
				
                enbgpos.setText(Integer.toString(acten.bgPos));
				if (acten.actorData != null) {
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < acten.actorData.length; i++) {
						sb.append(acten.actorData[i]).append(",");
					}
					if (sb.length() > 0) sb.setLength(sb.length()-1);
					actorParams.setText(sb.toString());
				} else {
					actorParams.setText("");
				}
			}
		}
	}
	
	int getEnemyWidth(int type) {
		if (type < 64) {
            int enImagesNr = 1;
            try {
                enImagesNr = enemyImagesnr[entype[type].imageNum];
            } catch (Exception e) {}
			return getEnemyImage(type).getWidth(null)/enImagesNr;
		} else {
			return getEnemyImage(type).getWidth(null);
		}
	}
	
	int getEnemyAt(int posX, int posY) {
		for (int i = 0; i < enemies.size(); i++) {
			Enemy en = enemies.get(i);
			Image enIm = getEnemyImage(en.type);
			if (posX >= en.dx && posX < en.dx+getEnemyWidth(en.type) && posY >= en.bgPos-enIm.getHeight(null) && posY < en.bgPos) {
				return i;
			}
		}
		return -1;
	}
	
	class Enemy {
		public int bgPos, type, path, dx, dy;
		public byte[] actorData;
		
		public Enemy(int _type, int _bgPos, byte[] _actorData) {
			type = _type;
			bgPos = _bgPos;
			actorData = _actorData;
		}
		
		public Enemy(int _type, int _bgPos, int _path, int _dx, int _dy) {
			type = _type;
			bgPos = _bgPos;
			path = _path;
			dx = _dx;
			dy = _dy;
		}
	}
	
	class EnemyComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			return ((Enemy)o1).bgPos - ((Enemy)o2).bgPos;
		}
		
		public boolean equals(Object o) {
			return this == o;
		}
	}
	
	class EnemyType {
		public int imageNum, speed, hp, point, bulletType, bulletFreq;
		
		public EnemyType(int _imageNum, int _speed, int _hp, int _point, int _bulletType, int _bulletFreq) {
			imageNum = _imageNum;
			speed = _speed;
			hp = _hp;
			point = _point;
			bulletType = _bulletType;
			bulletFreq = _bulletFreq;
		}
		
		public String toString() {
			if (imageNum < 64) return Integer.toString(hp)+"HP, "+point+"P, "+speed+"px/s; B: "+bulletType;
			else {
				switch (imageNum) {
					case 64: return "Message";
					case 65: return "Checkpoint";
					case 66: return "Change speed";
					case 67: return "End level";
					default:
						return "";
				}
			}
		}
	}

	class EnemyTypeComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			EnemyType e1 = (EnemyType)o1;
			EnemyType e2 = (EnemyType)o2;
			if (e1.imageNum != e2.imageNum) return e1.imageNum - e2.imageNum;
			return e1.point - e2.point;
		}
		
		public boolean equals(Object o) {
			return this == o;
		}
	}
	
	
/**********************************************************************************
/* DisplayPanel
/**********************************************************************************/
	class DisplayPanel extends JPanel implements MouseListener, MouseMotionListener {
	
		int[] MB = new int[3];
		int MX, MY;
		final Stroke sepStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1, 0}, 0);
		final Stroke gridStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1, 1}, 0);
		final Stroke tileStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1, 1}, 1);
		final static int pheight = 768;
		final static int tileHeightToDraw = ((pheight+128)>>5);
		
		public DisplayPanel() {
			setMaximumSize(new Dimension(getWidth(), getHeight()));
			setMinimumSize(new Dimension(getWidth(), getHeight()));
			setPreferredSize(new Dimension(getWidth(), getHeight()));
			setBackground(Color.black);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
	
		public void paintComponent(Graphics g) {
			if (cbBG.isSelected()) {
				g.drawImage(transparentImage, 0, 0, null);
				g.drawImage(transparentImage, 0, 256, null);
				g.drawImage(transparentImage, 0, 512, null);
			} else {
				super.paintComponent(g);
			}
			if (bg == null || bgNum == 0) return;

			Graphics2D g2 = (Graphics2D)g;
			
			// palya kirajzolas
			for (int i=0; i<bgNum; i++) {
				if (selectedLayer != i && !editEnemy) continue;
				
				int bgPos = getBgPos(i);
				int bgPosMod = bgPos&0x1f;
				int bgPosDiv = bgPos>>5;

				// kirajzolas
				for (int y = 1; y <= tileHeightToDraw; y++) {
					for (int x = 0; x < 12; x++) {
						int bgIndex = ((bgPosDiv+y-1)*12)+x;  
						int xPos = x<<4;
						int yPos = pheight + bgPosMod - (y<<5);
						
						if (bgIndex < 0 || bgIndex >= bg[i].length) {
							if (!editEnemy) g.drawImage(missingTileImage, xPos, yPos, null);
						} else {
						
							int actImIndex = bg[i][bgIndex];
							if (actImIndex < 0 || actImIndex >= bgImages.length) continue;
						
							// Y irany check
							if (bgImages[actImIndex].getHeight(null) < -yPos) continue;
						
							g.drawImage(bgImages[actImIndex], xPos, yPos, null);
						
							if (!editEnemy && cbTile.isSelected()) {
								g2.setStroke(tileStroke);
								g2.setColor(Color.RED);
								g2.draw(new Line2D.Float(xPos, yPos, xPos, yPos+32));
								g2.setColor(Color.YELLOW);
								g2.draw(new Line2D.Float(xPos, yPos, xPos+16, yPos));
							}
						}
					}
				}

				// grid
				if (!editEnemy && cbGrid.isSelected() && selectedLayer == i) {
					g2.setStroke(gridStroke);
					g2.setColor(Color.white);
					for (int x = 1; x <= 12; x++) {
						g2.draw(new Line2D.Float(x*16, 0, x*16, 768));
					}
					for (int y = -1; y < tileHeightToDraw; y++) {
						g2.draw(new Line2D.Float(0, y*32+bgPosMod, 256, y*32+bgPosMod));
						g2.setFont(new Font("Verdana", Font.BOLD, 10));
						float time = ((float)(bgPosDiv+y)*32*256)/bgSpeed[selectedLayer];
						g2.drawString(""+time, 192, pheight+bgPosMod-(y*32)-3);
					}
				}
				
				// separator
				if (!editEnemy && cbSep.isSelected()) {
					g2.setStroke(sepStroke);
					g2.setColor(Color.red);
					g2.draw(new Line2D.Float(96, 0, 96, 768));
				}
				
				// inhand
				if (!editEnemy && selectedLayer == i && inHand != null && MX >= 0 && MY >= 0) {
					
					int xPos = MX&0xfffffff0;
					int yPos = ((MY-bgPosMod)&0xffffffe0)+bgPosMod;
					int trlIndex = loadTile(inHand);
					if (trlIndex < 0) throw new RuntimeException("BUG! "+inHand);
					
					g.drawImage(bgImages[trlIndex], xPos, yPos, null);
				}
				
				// enemy
				if (editEnemy && i == bgNum-1) {
					for (int j = 0; j < enemies.size(); j++) {
						if (j == actEnemy && enHand >= 0) continue;
						Enemy acten = enemies.get(j);
						Image actim = getEnemyImage(acten.type);
						
						if (acten.bgPos >= bgPos && acten.bgPos <= bgPos+pheight+actim.getHeight(null)) {
							int enxPos = acten.dx;
							int enyPos = pheight-acten.bgPos+bgPos;
							g.setClip(enxPos, enyPos, getEnemyWidth(acten.type), actim.getHeight(null));
							g.drawImage(actim, enxPos, enyPos, null);
						}
					}
				}
				
				// enemy inHand
				if (editEnemy && enHand >= 0 && MX >= 0 && MY >= 0) {
					Image actim = getEnemyImage(enHand);
					g.setClip(MX, MY, getEnemyWidth(enHand), actim.getHeight(null));
					g.drawImage(actim, MX, MY, null);
				}
				
				g.setClip(0, 0, getWidth(), getHeight());
			}
			
			g.dispose();
		}
		
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {			// kezben levo ojjektum lerakasa
				if (editEnemy) {
					int posY = translateToLevelY(bgNum-1, e.getY());
					int posX = e.getX();
					int acten = getEnemyAt(posX, posY);
					
					if (enHand < 0) {
						if (actEnemy < 0) {
							if (acten >= 0) {		// sehol semmi, enemyre kattintott
								setActEnemy(acten);
							}
							return;					// sehol semmi, semmire se kattintott
							
						} else {
							if (acten == actEnemy) {	// kezben semmi, enemy kivalasztva, enemyre kattintott
								Enemy en = enemies.get(acten);
								enHand = en.type;
								inHandNum = 1;
								
							} else {					// kezben semmi, enemy kivalasztva, masik enemyre (vagy sehova se) kattintott
								setActEnemy(acten);	
								return;
							}
						}
						
					} else {
						if (actEnemy < 0) {		// kez tele, enemy nincs kivalasztva
							// uj ellen
							Enemy en = (enHand<64) ? new Enemy(enHand, posY, 0, posX, 0) : new Enemy(enHand, posY, null);
							enemies.add(en);
							inHandNum--;
							if (inHandNum == 0) enHand = -1;
						
						} else {				// kez tele, enemy kivalasztva - lerakas
							Enemy en = enemies.get(actEnemy);
							en.bgPos = posY;
							en.dx = posX;
							inHandNum--;
							if (inHandNum == 0) enHand = -1;
						}
						
						if (enHand < 0) setActEnemy(actEnemy);	// update-elje a koordinatakat lerakaskor
					}
					
				} else {
					int posY = translateToLevelY(selectedLayer, e.getY());
					int posX = e.getX();

					if (inHand == null) {		// felszedes
						inHand = removeTile(selectedLayer, posX>>4, posY>>5);
						inHandNum = 1;
					} else {					// lerakas
						setTile(selectedLayer, posX>>4, posY>>5, inHand);
						inHandNum--;
					 	if (inHandNum == 0) {
							inHand = null;
							itemList.clearSelection();
						}
					}
				}
				displayPanel.repaint();
				
			} else if (e.getButton() == MouseEvent.BUTTON3) {	// kezben levo objektum elhajitasa
				if (editEnemy) {
					if (enHand >= 0 && actEnemy >= 0) enemies.remove(actEnemy);
					enHand = -1;
					inHandNum = 0;
					setActEnemy(-1);
					itemList.clearSelection();
					repaint();
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
				case MouseEvent.BUTTON1: MB[0] = e.getX(); break;
				case MouseEvent.BUTTON2: MB[1] = e.getX(); break;
				case MouseEvent.BUTTON3: MB[2] = e.getX(); break;
			}
		}

		public void mouseReleased(MouseEvent e) {
			switch (e.getButton()) {
				case MouseEvent.BUTTON1: MB[0] = -1; break;
				case MouseEvent.BUTTON2: MB[1] = -1; break;
				case MouseEvent.BUTTON3: 
					if (MB[2] >= 0 && !editEnemy) {
						int posY = translateToLevelY(selectedLayer, e.getY());
						
						if (e.getX() - MB[2] > 10) {
							deleteRow(selectedLayer, posY>>5);
						} else if (MB[2] - e.getX() > 10) {
							insertRow(selectedLayer, posY>>5);
						} else {
							inHand = null;
							inHandNum = 0;
							itemList.clearSelection();
						}
						repaint();
					}
					MB[2] = -1;
					break;
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
			//MX = -1;
			//MY = -1;
		}
		
		public void mouseDragged(MouseEvent e) {
		}
		
		public void mouseMoved(MouseEvent e) {
			MX = e.getX();
			MY = e.getY();
			
			if ((!editEnemy && inHand != null) || (editEnemy && enHand >= 0)) repaint();
		}
		
		public int getHeight() {
			return pheight;
		}
	
		public int getWidth() {
			return 224;
		}
	}

/**********************************************************************************
/* PreviewPanel
/**********************************************************************************/
	class PreviewPanel extends JPanel {
	
		int pheight, pwidth, tileHeightToDraw, tileWidthToDraw, startTile, endTile, justCenterTile;
		int vscrl, vscrr, vscrdelta = 0;
		int pleft = 0, pright, ptop = 0, pbottom;

		public PreviewPanel() {
			setMaximumSize(new Dimension(256, 256));
			setMinimumSize(new Dimension(256, 256));
			setPreferredSize(new Dimension(256, 256));
			setBackground(Color.black);
			setHeight(160);
			setWidth(128);
		}
	
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (bg == null || bgNum == 0) return;

			for (int l = 0; l < bgNum; l++) {
				int bgPos = ((pvScrollPane.getViewport().getViewPosition().x*bgSpeed[l])>>12); /** modified for editor */ 

				int bgPosMod = bgPos&0x1f;
				int bgPosDiv = bgPos>>5;
				int vscrleft = vscrl + ((vscrdelta*bgSpeed[l])>>16);
				int vscrright = vscrr + ((vscrdelta*bgSpeed[l])>>16);

				// kirajzolas
				if (bgStretch[l] == ST_NORMAL) {
					int bgIndex = bgPosDiv*12;
					for (int i = 1; i <= tileHeightToDraw; i++) {
						for (int j = 0; j < 12; j++) {
							if (bgIndex >= bg[l].length) continue;          /** modified for editor */
							int actImIndex = bg[l][bgIndex++];
							if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

							// Y irany check
							int topPos = i<<5;
							if (topPos-bgImages[actImIndex].getHeight(null) > pheight+bgPosMod) continue;   /** modified for editor */

							// X irany check
							int leftPos = (j << 4)+vscrleft;
							if (leftPos >= pright || bgImages[actImIndex].getWidth(null)+leftPos < pleft) continue;   /** modified for editor */

							g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, null);
						}
					}
					
				} else if (bgStretch[l] == ST_REPEAT) {
					for (int i = 1; i <= tileHeightToDraw; i++) {
						int bgIndex = (bgPosDiv+i-1)*12;
						if (bgIndex >= bg[l].length) continue;
						
						for (int j = -7; j < endTile; j++) {
							int actInd = j;
							while (actInd < 0) actInd += 6;
							while (actInd >= 12) actInd -= 6;
							int actImIndex = bg[l][bgIndex+actInd];
							if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

							// Y irany check
							int topPos = i<<5;
							if (topPos-bgImages[actImIndex].getHeight(null) > pheight+bgPosMod) continue;

							// X irany check
							int leftPos = (j << 4)+vscrleft;
							if (leftPos >= pright || bgImages[actImIndex].getWidth(null)+leftPos < pleft) continue;

							g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, null);
						}
					}
					
				} else if (bgStretch[l] == ST_JUSTIFY) {
					
					// bal oldal
					g.setClip(pleft, ptop, pwidth>>1, pheight);
					for (int i = 1; i <= tileHeightToDraw; i++) {
						int bgIndex = (bgPosDiv+i-1)*12;
						if (bgIndex >= bg[l].length) continue;
						
						for (int j = 0; j < justCenterTile; j++) {
							int actImIndex = bg[l][bgIndex+j];
							if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

							// Y irany check
							int topPos = i<<5;
							if (topPos-bgImages[actImIndex].getHeight(null) > pheight+bgPosMod) continue;

							int leftPos = j<<4;
							
							g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, null);
						}
					}

					//jobb oldal
					g.setClip(pwidth>>1, ptop, pwidth>>1, pheight);
					for (int i = 1; i <= tileHeightToDraw; i++) {
						int bgIndex = (bgPosDiv+i-1)*12;
						if (bgIndex >= bg[l].length) continue;
						
						for (int j = 6; j < 12; j++) {
							int actImIndex = bg[l][bgIndex+j];
							if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

							// X irany check
							int leftPos = pwidth - ((12-j)<<4);
							if (leftPos >= pright || bgImages[actImIndex].getWidth(null)+leftPos < pleft+(pwidth>>2)) continue;
							
							// Y irany check
							int topPos = i<<5;
							if (topPos-bgImages[actImIndex].getHeight(null) > pheight+bgPosMod) continue;

							g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, null);
						}
					}
				}
			}

			g.dispose();
		}
		
		public void setHeight(int h) {
			pheight = h;
			pbottom = h; 
			tileHeightToDraw = (pheight+128)>>5;
			if ((pheight&0x1f) != 0) tileHeightToDraw++;
		}
		
		public void setWidth(int w) {
			pwidth = w;
			pright = w;
			vscrl = (pwidth>>1) - 96;
			vscrr = (pwidth>>1) + 96;
			tileWidthToDraw = pwidth>>4;
			if ((pwidth&0xf) != 0) tileWidthToDraw++;
			
			startTile = 6-(tileWidthToDraw>>1);
			endTile = 6+(tileWidthToDraw>>1);
			
			justCenterTile = pwidth/32;
			if ((pwidth&0x1f) != 0) justCenterTile++;
			if (justCenterTile > 6) justCenterTile = 6;
		}

		public int getHeight() {
			return pheight;
		}
	
		public int getWidth() {
			return pwidth;
		}
	}

	class PreviewPanelScrollerThread extends Thread {
		public void run() {
			try {
				for (;;) {
					Thread.sleep(64);
					Point p = pvScrollPane.getViewport().getViewPosition();
					if (p.x < bgLen*16) {
						p.x += 1;
						pvScrollPane.getViewport().setViewPosition(p);
					} else {
						pvStartStopButton.setText(">>");
						return;
					}
				}
			} catch (Exception e) {}
		}
	}
	
/**********************************************************************************
/* PropertiesWindow
/**********************************************************************************/
	class PropertiesWindow implements ActionListener {

		JButton bAdd, bRemove, bUp, bDown, bApply;
		JList bgList;
		JFrame propFrame;
		DefaultListModel dlm = new DefaultListModel();
		
		PropertiesWindow() {
			propFrame = new JFrame("Properties of "+levelName);
			propFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			propFrame.setSize(new Dimension(320, 240));
	
			Container propPanel = propFrame.getContentPane();
			GridBagLayout propGBL = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			propPanel.setLayout(propGBL);
			
			for (int i = 0; i < bgNum; i++) dlm.addElement(new Integer(i));
			
			JLabel lCurrentLayers = new JLabel("Current layers:");
			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			propPanel.add(lCurrentLayers, c);
			
			bgList = new JList(dlm);
			bgList.setLayoutOrientation(JList.VERTICAL);
			bgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			bgList.setFixedCellWidth(50);
			c.gridx = 0;
			c.gridy = 1;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			c.gridheight = 4;
			propPanel.add(bgList, c);
			c.gridheight = 1;
			
			bAdd = new JButton("Add new");
			bAdd.addActionListener(this);
			c.gridx = 1;
			c.gridy = 1;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			propPanel.add(bAdd, c);
	
			bRemove = new JButton("Remove");
			bRemove.addActionListener(this);
			c.gridx = 1;
			c.gridy = 2;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			propPanel.add(bRemove, c);
	
			bUp = new JButton("Up");
			bUp.addActionListener(this);
			c.gridx = 1;
			c.gridy = 3;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			propPanel.add(bUp, c);
	
			bDown = new JButton("Down");
			bDown.addActionListener(this);
			c.gridx = 1;
			c.gridy = 4;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			propPanel.add(bDown, c);
	
			bApply = new JButton("Apply");
			bApply.addActionListener(this);
			c.gridx = 0;
			c.gridy = 5;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = c.NONE;
			c.anchor = c.NORTHWEST;
			c.insets = new Insets(15, 0, 0, 0);
			propPanel.add(bApply, c);
	
			propFrame.setVisible(true);
		}

		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == bApply) {
				byte[][] bgtemp = new byte[10][];
				int[] bgSpeedtemp = new int[10];
				int[] bgStretchtemp = new int[10];
				for (int i = 0; i < dlm.size(); i++) {
					int actind = ((Integer)dlm.get(i)).intValue();
					if (actind < bgNum) {
						bgtemp[i] = bg[actind];
						bgSpeedtemp[i] = bgSpeed[actind];
						bgStretchtemp[i] = bgStretch[actind];
						
					} else {
						bgtemp[i] = new byte[120];
						Arrays.fill(bgtemp[i], (byte)-1);
						bgSpeedtemp[i] = 256;
						bgStretchtemp[i] = 0;
					}
				}
				bg = bgtemp;
				bgSpeed = bgSpeedtemp;
				bgStretch = bgStretchtemp;
				if (bgNum != dlm.size()) {
					bgNum = dlm.size();
					layerSpinnerModel.setMaximum(new Integer(bgNum-1));
					layerSpinnerModel.setValue(new Integer(0));
				}
				recalcLevelLength();
				
			} else if (event.getSource() == bUp) {
				int ind = bgList.getSelectedIndex();
				if (ind <= 0) return;
				dlm.add(ind-1, dlm.remove(ind));
				bgList.setSelectedIndex(ind-1);

			} else if (event.getSource() == bDown) {
				int ind = bgList.getSelectedIndex();
				if (ind >= dlm.size()-1) return;
				dlm.add(ind+1, dlm.remove(ind));
				bgList.setSelectedIndex(ind+1);

			} else if (event.getSource() == bAdd) {
				dlm.addElement(new Integer(dlm.size()));
				bgList.setSelectedIndex(dlm.size()-1);
				
			} else if (event.getSource() == bRemove) {
				int ind = bgList.getSelectedIndex();
				dlm.remove(ind);
			}
		}
	}
}


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
public class PathEditor implements ActionListener, ChangeListener {
	JFrame editorFrame;
	JMenuItem menuFileSave, menuFileQuit;
	Container editorPanel;
	PathPanel pathPanel;
	JPanel balPanel, jobbPanel;
	JCheckBoxMenuItem cbGrid;
	
	SpinnerNumberModel pathSpinnerModel;
	JSpinner pathSpinner;
	JButton bNew, bClear; 

	TitledBorder pointBorder;
	JTextField pointSpeed, pointWait;
	JButton bInsertBefore, bInsertAfter;
	
	final static Insets ZEROINSETS = new Insets(0, 0, 0, 0);
	
	ArrayList<ArrayList<PathPoint>> p = new ArrayList<ArrayList<PathPoint>>(10);
	int actPath, actPoint = -1, inHandPoint = -1;
	
	/**
	 *  Constructor for the PathEditor object
	 */
	public PathEditor() {
		
		//Create and set up the window.
		editorFrame = new JFrame("Path Editor");
		editorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		editorFrame.setSize(new Dimension(425, 360));

		editorPanel = editorFrame.getContentPane();
		GridBagLayout editorGBL = new GridBagLayout();
		editorPanel.setLayout(editorGBL);
		
/******************** balPanel
 ********************/
		balPanel = new JPanel(new GridBagLayout());

		JLabel tPath = new JLabel("Path:");
		balPanel.add(tPath, new GridBagConstraints(0, 0, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													ZEROINSETS,						/** Insets */
													0, 0));							/** pad */
		
		
		pathSpinnerModel = new SpinnerNumberModel(0, 0, 0, 1);
		pathSpinner = new JSpinner(pathSpinnerModel);
		pathSpinner.addChangeListener(this);
		balPanel.add(pathSpinner, new GridBagConstraints(	1, 0, 1, 1,		/** X, Y, Width, Height */
															0.2, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															new Insets(0, 5, 0, 15),		/** Insets */
															0, 0));							/** pad */
		

		bNew = new JButton("New");															
		bNew.addActionListener(this);
		balPanel.add(bNew, new GridBagConstraints(	2, 0, 1, 1,		/** X, Y, Width, Height */
													0.0, 0.0, 		/** weight */
													GridBagConstraints.NORTHWEST,	/** anchor */
													GridBagConstraints.NONE,		/** fill */
													new Insets(0, 0, 0, 15),		/** Insets */
													0, 0));							/** pad */

		bClear = new JButton("Clear");
		bClear.addActionListener(this);
		balPanel.add(bClear, new GridBagConstraints(	3, 0, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														ZEROINSETS,						/** Insets */
														0, 0));							/** pad */
		
		
		pathPanel = new PathPanel();
		balPanel.add(pathPanel, new GridBagConstraints(	0, 1, 4, 1,		/** X, Y, Width, Height */
														1.0, 1.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.BOTH,		/** fill */
														new Insets(5, 5, 0, 0),			/** Insets */
														0, 0));							/** pad */
		
		
		editorPanel.add(balPanel, new GridBagConstraints(	0, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.VERTICAL,	/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		
/******************** jobbPanel
 ********************/
		jobbPanel = new JPanel(new GridBagLayout());
		pointBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Point");
		jobbPanel.setBorder(pointBorder);

		JLabel tPointSpeed = new JLabel("Speed:");
		jobbPanel.add(tPointSpeed, new GridBagConstraints(0, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		
		pointSpeed = new JTextField(4);
		pointSpeed.addActionListener(this);
		jobbPanel.add(pointSpeed, new GridBagConstraints(1, 0, 1, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															new Insets(0, 5, 0, 0),		/** Insets */
															0, 0));							/** pad */

		JLabel tPointWait = new JLabel("Wait:");
		jobbPanel.add(tPointWait, new GridBagConstraints(0, 1, 1, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */
		
		
		pointWait = new JTextField(4);
		pointWait.addActionListener(this);
		jobbPanel.add(pointWait, new GridBagConstraints(1, 1, 1, 1,		/** X, Y, Width, Height */
														0.0, 0.0, 		/** weight */
														GridBagConstraints.NORTHWEST,	/** anchor */
														GridBagConstraints.NONE,		/** fill */
														new Insets(0, 5, 0, 0),			/** Insets */
														0, 0));							/** pad */
		
		bInsertBefore = new JButton("Insert Before");
		bInsertBefore.addActionListener(this);
		jobbPanel.add(bInsertBefore, new GridBagConstraints(0, 2, 2, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															new Insets(10, 0, 0, 0),		/** Insets */
															0, 0));							/** pad */

		bInsertAfter = new JButton("Insert After");
		bInsertAfter.addActionListener(this);
		jobbPanel.add(bInsertAfter, new GridBagConstraints(0, 3, 2, 1,		/** X, Y, Width, Height */
															0.0, 0.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,		/** fill */
															new Insets(10, 0, 0, 0),		/** Insets */
															0, 0));							/** pad */

		
		balPanel.add(jobbPanel, new GridBagConstraints(	5, 1, 1, 1,		/** X, Y, Width, Height */
															0.0, 1.0, 		/** weight */
															GridBagConstraints.NORTHWEST,	/** anchor */
															GridBagConstraints.NONE,	/** fill */
															ZEROINSETS,						/** Insets */
															0, 0));							/** pad */

																
/******************** GLUE
 ********************/
		editorPanel.add(new JPanel(), new GridBagConstraints(	1, 0, 1, 1,		/** X, Y, Width, Height */
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
		
		menuFileSave = new JMenuItem("Save");
		menuFile.add(menuFileSave);
		menuFileSave.addActionListener(this);

		menuFileQuit = new JMenuItem("Quit");
		menuFile.add(menuFileQuit);
		menuFileQuit.addActionListener(this);

		// grid
		menuBar.add(Box.createHorizontalStrut(20));
		cbGrid = new JCheckBoxMenuItem("Grid", true);
		cbGrid.addActionListener(this);
		cbGrid.setMaximumSize(new Dimension(48, 38));
		menuBar.add(cbGrid);
		
		editorFrame.setJMenuBar(menuBar);

		loadPath();
		
		//Display the window.
		//editorFrame.pack();
		editorFrame.setVisible(true);
	}



	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == pathSpinner) {
			actPath = ((Number)pathSpinnerModel.getValue()).intValue();
			setActPoint(-1);
			pathPanel.repaint();
		}
	}
	
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == bNew) {
			p.add(new ArrayList<PathPoint>());
			pathSpinnerModel.setMaximum(p.size()-1);
			pathSpinner.setValue(new Integer(p.size()-1));
			
		} else if (event.getSource() == bClear) {
			p.get(actPath).clear();
			setActPoint(-1);
			pathPanel.repaint();

		} else if (event.getSource() == bInsertBefore) {
			ArrayList<PathPoint> pp = p.get(actPath);
			if (actPoint <= 0) {
				pp.add(0, new PathPoint(10, 10, 256, 0));
			} else {
				PathPoint a = pp.get(actPoint);
				PathPoint b = pp.get(actPoint-1);
				pp.add(actPoint, new PathPoint((a.x+b.x)/2, (a.y+b.y)/2, 256, 0));
			}
			actPoint++;
			setActPoint(actPoint);
			pathPanel.repaint();
			
		} else if (event.getSource() == bInsertAfter) {
			ArrayList<PathPoint> pp = p.get(actPath);
			if (actPoint == pp.size()-1 || actPoint < 0) {
				PathPoint actp = new PathPoint(10, 10, 256, 0);
				pp.add(actp);
			} else {
				PathPoint a = pp.get(actPoint);
				PathPoint b = pp.get(actPoint+1);
				pp.add(actPoint+1, new PathPoint((a.x+b.x)/2, (a.y+b.y)/2, 256, 0));
			}
			pathPanel.repaint();

		} else if (event.getSource() == menuFileSave) {
			savePath();
			
		} else if (event.getSource() == pointSpeed) {
			if (actPoint >= 0) {
				p.get(actPath).get(actPoint).speed = (int)(Double.parseDouble(pointSpeed.getText())*256);
			}

		} else if (event.getSource() == pointWait) {
			if (actPoint >= 0) {
				p.get(actPath).get(actPoint).wait = (int)(Double.parseDouble(pointWait.getText())*256);
			}

		} else if (event.getSource() == menuFileQuit) {
			System.exit(0);
			
		} else if (event.getSource() == cbGrid) {
			pathPanel.repaint();
			
		}
	}

	void setActPoint(int i) {
		actPoint = i;
		if (actPoint < 0) {
			pointBorder.setTitle("None");
			pointSpeed.setText("");
			pointWait.setText("");
			jobbPanel.repaint();
		
		} else {
			pointBorder.setTitle("Point "+actPoint);
			
			PathPoint actp = p.get(actPath).get(actPoint);
		
			String speedString = Double.toString(Math.round(actp.speed/256.0*100.0)/100.0);
			if (speedString.length() > 4) speedString = speedString.substring(0, 4);
			pointSpeed.setText(speedString);

			String waitString = Double.toString(Math.round(actp.wait/256.0*100.0)/100.0);
			if (waitString.length() > 4) waitString = waitString.substring(0, 4);
			pointWait.setText(waitString);

			jobbPanel.repaint();
		}
	}

	private static void createAndShowGUI() {
		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);

		PathEditor le = new PathEditor();
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
/* PathLoader
/**********************************************************************************/
	
	void loadPath() {
		try {
			InputStream is = new FileInputStream("p");
			int pNum = is.read();
			for (int i = 0; i < pNum; i++) {
				int len = is.read();
				ArrayList<PathPoint> pp = new ArrayList<PathPoint>(len);
				for (int j = 0; j < len; j++) {
					PathPoint actp = new PathPoint();
					actp.x = is.read();
					actp.y = is.read();
					actp.speed = is.read();
					actp.speed += is.read()<<8;
					actp.wait = is.read();
					actp.wait += is.read()<<8;
					pp.add(actp);
				}
				p.add(pp);
			}
			pathSpinnerModel.setMaximum(p.size()-1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void savePath() {
		try {
			OutputStream os = new FileOutputStream("p");
			
			os.write(p.size());
			for (int i = 0; i < p.size(); i++) {
				ArrayList<PathPoint> pp = p.get(i);
				os.write(pp.size());
				for (int j = 0; j < pp.size(); j++) {
					PathPoint actp = pp.get(j);
					os.write(actp.x);
					os.write(actp.y);
					os.write(actp.speed&0xff);
					os.write(actp.speed>>8);
					os.write(actp.wait&0xff);
					os.write(actp.wait>>8);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
/**********************************************************************************
/* PathPoint
/**********************************************************************************/
	class PathPoint {
		public int x, y;
		public int speed, wait;
		
		public PathPoint() {
		}
		
		public PathPoint(int _x, int _y) {
			x = _x;
			y = _y;
		}

		public PathPoint(int _x, int _y, int _speed, int _wait) {
			x = _x;
			y = _y;
			speed = _speed;
			wait = _wait;
		}
		
		public PathPoint clone() {
			return new PathPoint(x, y, speed, wait);
		}
	}


/**********************************************************************************
/* PathPanel
/**********************************************************************************/
	class PathPanel extends JPanel implements MouseListener, MouseMotionListener {
	
		int[] MB = new int[3];
		int MX, MY;
		final Stroke lineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1, 1}, 0);
		final Stroke gridStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1, 3}, 0);
		
		public PathPanel() {
			setMaximumSize(new Dimension(getWidth(), getHeight()));
			setMinimumSize(new Dimension(getWidth(), getHeight()));
			setPreferredSize(new Dimension(getWidth(), getHeight()));
			setBackground(Color.black);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
	
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setFont(new Font("Verdana", Font.PLAIN, 9));
			
			// grid
			if (cbGrid.isSelected()) {
				g2.setStroke(gridStroke);
				g2.setColor(Color.GRAY);
				for (int i = 63; i < 256; i += 64) {
					g2.draw(new Line2D.Float(i, 0, i, 256));
					g2.draw(new Line2D.Float(0, i, 256, i));
				}
			}
			
			
			ArrayList<PathPoint> pp = p.get(actPath);
			PathPoint prevp = null;
			for (int i = 0; i < pp.size(); i++) {
				PathPoint actp = pp.get(i);
				
				if (inHandPoint == i) {
					g2.setColor(Color.BLUE);
				} else if (actPoint == i) {
					g2.setColor(Color.GREEN);
				} else {
					g2.setColor(Color.RED);
				}
				g2.fill(new Rectangle2D.Float(actp.x-3, actp.y-3, 6, 6));

				if (prevp != null) {
					g2.setStroke(lineStroke);
					g2.setColor(Color.YELLOW);
					g2.draw(new Line2D.Float(prevp.x, prevp.y, actp.x, actp.y));
				}
				
				g2.setColor(Color.WHITE);
				g2.drawString(Integer.toString(i), actp.x, actp.y);
				
				prevp = actp;
			}
			
			g.dispose();
		}
		
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {			// kezben levo ojjektum lerakasa
				if (inHandPoint < 0) {
					int posX = e.getX();
					int posY = e.getY();

					// melyik pontra katt
					ArrayList<PathPoint> pp = p.get(actPath);
					int clickedPoint = -1;
					for (int i = 0; i < pp.size(); i++) {
						int px = pp.get(i).x;
						int py = pp.get(i).y;

						if (px >= posX-3 && px < posX+3 && py >= posY-3 && py <= posY+3) {
							clickedPoint = i;
							break;
						}
					}

					if (clickedPoint < 0) return;

					if (actPoint < 0 || actPoint != clickedPoint) {
						setActPoint(clickedPoint);
						repaint();

					} else {
						inHandPoint = clickedPoint;
						repaint();
					}
					
				} else {	// lerakas
					inHandPoint = -1;
					repaint();
				}
				
			} else if (e.getButton() == MouseEvent.BUTTON3) {	// kezben levo objektum elhajitasa
				if (inHandPoint >= 0) {
					p.get(actPath).remove(inHandPoint);
					inHandPoint = -1;
					repaint();
				} else if (actPoint >= 0) {
					setActPoint(-1);
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
				case MouseEvent.BUTTON3: MB[2] = -1; break;
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
		
		public void mouseDragged(MouseEvent e) {
		}
		
		public void mouseMoved(MouseEvent e) {
			if (inHandPoint >= 0) {
				PathPoint actp = p.get(actPath).get(inHandPoint);
				actp.x = e.getX();
				actp.y = e.getY();
				repaint();
			}
		}
		
		public int getHeight() {
			return 256;
		}
	
		public int getWidth() {
			return 256;
		}
	}
}


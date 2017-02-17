import ij.*;
import ij.plugin.*;
import ij.plugin.frame.RoiManager;
import ij.gui.*;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.function.Function;

import javax.swing.*;

/**
 * This PlugIn for imageJ, is designed to be used as a utility tool, for use
 * with High-Throughput microscopes. The plugin takes a stitched image of a
 * scan, and has the user select regions they want to be re-scanned. The program
 * then creates a pointlist for the microscope to re-scan.
 */
public class Freehand_Generator implements PlugIn, KeyListener, ImageListener {
	ImageWindow win;
	ImageCanvas canvas;
	ImagePlus img;
	RoiManager RM;
	String directory;

	/**
	 * This method sets up the ImageWindow, Canvas, ImagePlus, RoiManager and
	 * KeyListener for the program. The KeyListener overrides
	 * 
	 * @param arg
	 *            Runtime String argument, none are used in this program.
	 */
	public void run(String arg) {

		ImagePlus img = IJ.getImage();
		this.img = img;
		win = img.getWindow();
		canvas = win.getCanvas();
		win.removeKeyListener(IJ.getInstance());
		canvas.removeKeyListener(IJ.getInstance());
		win.addKeyListener(this);
		canvas.addKeyListener(this);
		ImagePlus.addImageListener(this);
		RoiManager RM = new RoiManager();
		this.RM = RM;
	}

	/**
	 * Handles the closing of active imageWindow, and hands back key controls to
	 * ImageJ.
	 * 
	 * @param imp
	 *            The ImagePlus being closed
	 */
	public void imageClosed(ImagePlus imp) {
		if (win != null)
			win.removeKeyListener(this);
		if (canvas != null)
			canvas.removeKeyListener(this);
		ImagePlus.removeImageListener(this);
	}

	/**
	 * The Program does not perform any functions on the release of a key
	 * 
	 * @param e
	 *            The KeyEvent event that occured.
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * The Program does not perform any functions on the typing of a key
	 * 
	 * @param e
	 *            The KeyEvent event that occured.
	 */
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * The Program does not perform any functions on the opening of an image
	 * 
	 * @param e
	 *            The ImagePlus the was opened.
	 */
	public void imageOpened(ImagePlus imp) {
	}

	/**
	 * The Program does not perform any functions on the updating of an image.
	 * 
	 * @param e
	 *            The ImagePlus that was updated
	 */
	public void imageUpdated(ImagePlus imp) {
	}

	/**
	 * When a key is pressed, it passes through this program, and if the key is
	 * "E", or key code 69, this plugin handles it, otherwise it sends the key
	 * press event to imageJ.
	 * 
	 * @param e
	 *            The Key that was pressed.
	 */
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode == 69) {
			saveAndExport();
		} else
			IJ.getInstance().keyPressed(e);
	}

	/**
	 * The method called by pressing of the "e" key. This method acts as the
	 * main thread of the program, and executes other methods in order, rather
	 * than performing them here.
	 */
	private void saveAndExport() {
		PolyInfo[] ROIs = getRois();
		XDCEInfo info = readFile();
		double[] input = getInput();
		BigDecimal fieldSize = new BigDecimal(info.getSize()).multiply(new BigDecimal(input[0]));
		IJ.log(fieldSize.toString());
		writeFieldLocation(ROIs, info, input, fieldSize);
	}

	/**
	 * This method actually writes the field locations onto a pointlist file for
	 * exporting.
	 * 
	 * @param ROIs
	 *            The regions of interest to be exported.
	 * @param info
	 *            Information acquired from the XDCE file to correctly position
	 *            the fields.
	 * @param input
	 *            A double array that represents the two pieces of information a
	 *            user inputted.
	 * @param fieldSize
	 *            The FieldSize to be used...in microns.
	 */
	private void writeFieldLocation(PolyInfo[] ROIs, XDCEInfo info, double[] input, BigDecimal fieldSize) {
		IJ.log(directory);
		
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter("C:\\Users\\misha\\Desktop\\Save Folder" + "\\pointlist.xfpf"));
			/*
			 * The setup for writing pointlist
			 */
			bw.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>");
			bw.newLine();
			bw.write("<PointList type=\"POINTLIST\">");
			bw.newLine();
			bw.write("<Plate name=\"slide 1x3\"/>");
			bw.newLine();
			/*
			 * getting half the width and height of the image in microns, this
			 * is used to map the image's micron values onto the microscope's
			 * co-ordinate system.
			 */
			BigDecimal widthMicrons = info.getpSize().multiply(new BigDecimal(img.getWidth()));
			BigDecimal halfWidth = widthMicrons.divide(new BigDecimal(2));

			BigDecimal heightMicrons = info.getpSize().multiply(new BigDecimal(img.getHeight()));
			BigDecimal halfHeight = heightMicrons.divide(new BigDecimal(2));
			/*
			 * Getting each field's overlap as a percent, then as a micron
			 * value, additionally getting half of the field's size, this will
			 * be used to convert from Java's "top left" style of image analysis
			 * to the microscope's middle style.
			 */
			double percentOverlap = input[1] * 0.01;
			BigDecimal micronOverlap = new BigDecimal(percentOverlap).multiply(fieldSize);
			BigDecimal halfSize = fieldSize.divide(new BigDecimal(2));
			/*
			 * This is the main for loop of the program, and loops through each
			 * ROI selected by the user.
			 */
			int i = 0;
			for (PolyInfo roi : ROIs) {
				/*
				 * converting the pixel values associated with each ROI's
				 * bounding rectangle into micron values...NOTE: these values
				 * are still calculated based on the top left of the image.
				 */
				BigDecimal xMicrons = new BigDecimal(roi.getX()).multiply(info.getpSize());
				BigDecimal yMicrons = new BigDecimal(roi.getY()).multiply(info.getpSize());
				BigDecimal width = new BigDecimal(roi.getWidth()).multiply(info.getpSize());
				BigDecimal height = new BigDecimal(roi.getHeight()).multiply(info.getpSize());
				/*
				 * This Function Object maps an x value onto the microscope
				 * grid, as well as applying the Slide's offsets.
				 */
				Function<BigDecimal, BigDecimal> Mapx = mv -> {
					BigDecimal fMV = mv.subtract(halfWidth);

					fMV = fMV.add(info.getXWellOffset().multiply(new BigDecimal(1000)));
					IJ.log(fMV.toString());
					return fMV;
				};
				/*
				 * This Function Object maps a y value onto the microscope grid,
				 * as well as applying the Slide's offsets.
				 */
				Function<BigDecimal, BigDecimal> Mapy = mv -> {
					BigDecimal fMV = mv.subtract(halfHeight);

					fMV = fMV.add(info.getYWellOffset().multiply(new BigDecimal(1000)));
					IJ.log(fMV.toString());
					return fMV;
				};
				/*
				 * writes a comment in the .XFPF indicating which ROI's points
				 * are about to be listed.
				 */
				bw.write("<!--ROI " + i + "-->");
				bw.newLine();
				int xNum = 1;
				int yNum = 1;
				/*
				 * Since java analyzes shapes from the top left, half the width
				 * and height must be subtracted from the initial value, in
				 * order to center the co-ordinate
				 */
				BigDecimal x = xMicrons.subtract(halfSize);
				BigDecimal y = yMicrons.subtract(halfSize);
				BigDecimal totalWidth = fieldSize;
				BigDecimal totalWidth2 = fieldSize;
				/*
				 * while the total width of fields created is less than the
				 * total width of the ROI, or this is the first field
				 */
				while (totalWidth2.compareTo(width) == -1 || xNum == 1) {
					// Relic from ages long past
					BigDecimal weird = new BigDecimal(1);
					// Resettng y values to 1, due to loop-in-loop structure.
					y = yMicrons.subtract(halfSize);
					totalWidth = fieldSize;
					yNum = 1;
					// Exact same loop as above
					while (totalWidth.compareTo(height) == -1) {
						bw.write("<Point x=\"" + Mapx.apply(x) + "\" y=\"" + Mapy.apply(y) + "\"/>");
						bw.newLine();

						y = y.add(fieldSize.multiply(weird)).subtract(micronOverlap.multiply(weird));
						totalWidth = totalWidth.add(fieldSize.multiply(weird)).subtract(micronOverlap.multiply(weird));
						yNum++;
					}
					x = x.add(fieldSize.multiply(weird)).subtract(micronOverlap.multiply(weird));
					totalWidth2 = totalWidth2.add(fieldSize.multiply(weird)).subtract(micronOverlap.multiply(weird));
					xNum++;
				}
				bw.write("<!-- %" + (xNum - 1) + "% #" + (yNum - 1) + "# -->");
				bw.newLine();
				i++;
			}

			/*
			 * The closing for writing pointlist
			 */
			bw.newLine();
			bw.write("</PointList>");
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * This method asks the user information about how they would like the
	 * fields in the rescan to be placed.
	 * 
	 * @return 2 integers, each representing a user's choice.
	 */
	private double[] getInput() {
		/*
		 * NOTE: THIS METHOD EASILY BREAKS WITH USERS DOING UNEXPECTED THINGS
		 */
		double[] result = new double[2];
		/*
		 * Setting the "look and feel" of this dialog box to be similar to
		 * windows.
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		/*
		 * Asking the user about which zoom they would like to re-image at, and
		 * using a lookup table to get the respective pixel sizes.
		 */
		String[] choices = { "4x", "10x", "20x", "60x" };
		String imageZoom = (String) JOptionPane.showInputDialog(null, null, "Zoom to Re-Image at",
				JOptionPane.QUESTION_MESSAGE, null, choices, choices[2]);
		switch (imageZoom) {
		case "4x":
			result[0] = 1.2;
			break;
		case "10x":
			result[0] = 0.65;
			break;
		case "20x":
			result[0] = 0.325;
			break;
		case "60x":
			result[0] = 0.108;
			break;
		}
		/*
		 * Asking the user how much they would like each field to overlap, and
		 * supports negative values
		 */
		int overlap = Integer.parseInt((String) JOptionPane.showInputDialog(null, null, "Image Overlap (As a Percent)",
				JOptionPane.QUESTION_MESSAGE, null, null, 10));
		result[1] = (double) overlap;
		return result;
	}

	/**
	 * Asks the user where the XDCE file for the scan is, read it, and remove
	 * unwanted information
	 * 
	 * @return an XDCEInfo object, containing relevant information about the
	 *         initial scan.
	 */
	private XDCEInfo readFile() {
		/*
		 * This initial chunk creates the File Dialog, and customizes it. Also
		 * handles pressing of cancel vs approve buttons.
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		double sWidth = screen.getWidth();
		double sHeight = screen.getHeight();

		final JFileChooser fc = new JFileChooser();
		fc.setPreferredSize(new Dimension((int) sWidth / 3, (int) sHeight / 3));
		fc.setDialogTitle("Related XDCE File");
		fc.setApproveButtonText("Select");
		fc.setApproveButtonToolTipText("XDCE file created by the microscope during imaging");
		int returnVal = fc.showOpenDialog(fc);
		/*
		 * Beginning here, is the block of code that extracts information from
		 * the XDCE File, assuming the user selected the correct file...
		 */
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			ArrayList<String> basicContents = getXdceContents(fc.getSelectedFile().getPath());
			directory = fc.getSelectedFile().getPath();
			basicContents.removeIf(el -> FileFilter(el));
			for (String s : basicContents) {
				IJ.log(s);
			}
			XDCEInfo contents = new XDCEInfo(basicContents);
			return contents;
		}
		/*
		 * Returns null if the user cancelled the dialog box, does not destroy
		 * thread to try and preserve user's selections...
		 */
		else {
			return null;
		}
	}

	/**
	 * an IPredicate, used to determine whether an element should be removed
	 * from an array
	 * 
	 * @param el
	 *            The element to be analyzed
	 * @return Whether the element should be removed
	 */
	private boolean FileFilter(String el) {
		/*
		 * if else ladder, can be replaced with switch statement...
		 */
		if (el.contains("<ObjectiveCalibration "))
			return false;
		else if (el.contains("<Shifting "))
			return false;
		else if (el.contains("TopLeft"))
			return false;
		else if (el.contains("<Binning "))
			return false;
		else if (el.contains("<Size "))
			return false;
		else if (el.contains("<offset_point index=\"0\""))
			return false;
		else
			return true;
	}

	private ArrayList<String> getXdceContents(String path) {
		BufferedReader br;
		/*
		 * This section gets all lines in the XDCE that aren't null.
		 */
		ArrayList<String> lines = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(path));

			String line = br.readLine();
			while (line != null) {
				lines.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	private PolyInfo[] getRois() {
		/*
		 * Converts the ROIs into java.awt.Polygon objects, for increased
		 * capabilities
		 */
		Roi[] regions = RM.getRoisAsArray();
		PolyInfo[] results = new PolyInfo[regions.length];
		for (int i = 0; i < regions.length; i++) {

			Polygon p = regions[i].getPolygon();
			results[i] = new PolyInfo(p);
		}
		return results;
	}

	/**
	 * This is simply a utility class, designed in conjunction with
	 * <code>Freehand_Generator</code>
	 * 
	 * @author Michael Wrana
	 *
	 */
	class PolyInfo {
		private Polygon poly;
		private Rectangle bounding;

		public PolyInfo(Polygon p) {
			this.poly = p;
			this.bounding = p.getBounds();
		}

		/**
		 * @return Bounding rectangle's X-coordinate
		 */
		public int getX() {
			return bounding.x;
		}

		/**
		 * @return Bounding rectangle's Y-coordinate
		 */
		public int getY() {
			return bounding.y;
		}

		/**
		 * @return Bounding rectangle's width
		 */
		public int getWidth() {
			return bounding.width;
		}

		/**
		 * @return Bounding rectangle's height
		 */
		public int getHeight() {
			return bounding.height;
		}

		/**
		 * @return The X-coordinates of the polygon's sides
		 */
		public int[] getXSides() {
			return poly.xpoints;
		}

		/**
		 * @return The Y-coordinates of the polygon's sides
		 */
		public int[] getYSides() {
			return poly.ypoints;
		}

		/**
		 * @return The Java.awt.Polygon that represents a user's ROI.
		 */
		public Polygon getShape() {
			return poly;
		}
	}

	/**
	 * This is simply a utility class, designed in conjunction with
	 * <code>Freehand_Generator</code>. THis class contains the relevant
	 * information inside a microscope-generated XDCE file.
	 * 
	 * @author Michael Wrana
	 *
	 */
	class XDCEInfo {

		private String pixelSizeString;
		private String binningString;
		private String sizeString;
		private String wellOffsetString;
		private String shiftingString;

		public XDCEInfo(ArrayList<String> basicContents) {
			for (String element : basicContents) {
				/*
				 * If-else ladder, can be replaced with switch statement.
				 */
				if (element.contains("pixel_height")) {
					this.pixelSizeString = element;
				} else if (element.contains("Binning")) {
					this.binningString = element;
				} else if (element.contains("<Size")) {
					this.sizeString = element;
				} else if (element.contains("TopLeft")) {
					this.wellOffsetString = element;
				} else if (element.contains("Shifting")) {
					this.shiftingString = element;
				}
			}
		}

		/**
		 * |
		 * 
		 * @return the pixel width, without calculating binning
		 */
		private BigDecimal getpWidth() {
			return extractBD(this.pixelSizeString, "pixel_width=\"", "\"");
		}

		/**
		 * 
		 * @return The pixel size multiplied by the binning
		 */
		public BigDecimal getpSize() {
			return getpWidth().multiply(new BigDecimal(getBinning()));
		}

		/**
		 * The field size divided by the binning
		 */
		public int getImgSize() {
			return (getSize() / getBinning());
		}

		/**
		 * @return The binning value
		 */
		public int getBinning() {
			int l = this.binningString.indexOf("X");
			String s = this.binningString.substring(l + 2, l + 3);
			return Integer.valueOf(s);
		}

		/**
		 * 
		 * @return image size, not divided by binning
		 */
		private int getSize() {
			return extractInteger(this.sizeString, "height=\"", "\"");
		}

		/**
		 * @return THe x shift value
		 */
		public BigDecimal getxShift() {
			return extractBD(this.shiftingString, "x=\"", "\"");
		}

		/**
		 * @return THe y shift value
		 */
		public BigDecimal getyShift() {
			return extractBD(this.shiftingString, "y=\"", "\"");
		}

		/**
		 * 
		 * @return the X well offset
		 */
		private BigDecimal getXWellOffset() {
			return extractBD(this.wellOffsetString, "horizontal=\"", "\"");
		}

		/**
		 * 
		 * @return the Y well offset
		 */
		private BigDecimal getYWellOffset() {
			return extractBD(this.wellOffsetString, "vertical=\"", "\"");
		}

		/**
		 * utility method for extracting an integer value from a string
		 * 
		 * @param phrase
		 *            String in which a value is being extracted
		 * @param vo
		 *            A set of unique characters before the value that is to be
		 *            extracted
		 * @param vc
		 *            the FIRST character after the value
		 * @return the value, as an integer.
		 */
		private int extractInteger(String phrase, String vo, String vc) {
			int l = phrase.indexOf(vo);
			int m = vo.length();
			String a = phrase.substring(l + m);
			String b = a.substring(0, a.indexOf(vc));
			return Integer.valueOf(b);
		}

		/**
		 * utility method for extracting an BigDecimal value from a string
		 * 
		 * @param phrase
		 *            String in which a value is being extracted
		 * @param vo
		 *            A set of unique characters before the value that is to be
		 *            extracted
		 * @param vc
		 *            the FIRST character after the value
		 * @return the value, as a BigDecimal.
		 */
		private BigDecimal extractBD(String phrase, String vo, String vc) {
			int l = phrase.indexOf(vo);
			int m = vo.length();
			String a = phrase.substring(l + m);
			String b = a.substring(0, a.indexOf(vc));
			return new BigDecimal(b);
		}
	}

}

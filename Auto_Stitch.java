import ij.*;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.OpenDialog;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFileChooser;

import ij.plugin.*;

import java.awt.CheckboxGroup;
import java.awt.TextField;
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This Fiji Plugin is designed to automatically stitch a folder of images, and
 * can also be used to stitch images as they are created in said folder.
 * 
 * @author Michael Wrana
 *
 */
public class Auto_Stitch implements PlugIn {
	String imgDir;
	boolean timepoints = false;
	int totalP;
	int rows;
	int columns;
	ArrayList<String> imageQueue = new ArrayList<String>();
	ArrayList<String> finishedImages = new ArrayList<String>();
	String leading;
	int numF;
	String type;
	String order;
	int spacing;
	String renamed;
	String frontV;
	String dash;
	String macroLocation;
	int numMins = 1;

	/**
	 * This method is the main thread in which the PlugIn runs.
	 */
	@Override
	public void run(String arg) {
		getDir();

		File sf = new File(imgDir + "\\Stitched");
		sf.mkdirs();
		while (finishedImages.size() < totalP) {
			long beginning = System.currentTimeMillis();

			File folder = new File(imgDir);
			File[] images = folder.listFiles();

			numF = rows * columns;
			int numFieldLength = Integer.toString(numF).length();

			for (File f : images) {
				if (f.getName().contains("d 01")) {
					leading = "0";
				} else if (f.getName().contains("d 001")) {
					leading = "00";
				} else if (f.getName().contains("d 0001")) {
					leading = "000";
				}

			}

			if (leading.length() + 1 == numFieldLength)
				frontV = "";
			else
				frontV = "0";

			for (File f : images) {
				if (initialFound(f.getName()))
					if (finalFound(f.getName(), images))
						if (notInQueue(f.getName())) {
							imageQueue.add(f.getName());
							finishedImages.add(f.getName());
						}
			}
			IJ.log(finishedImages.size() + " total images found");
			IJ.log(imageQueue.size() + " new images found");
			int i = 0;
			while (i < imageQueue.size()) {
				String configName = "TileConfig" + 0;
				String fileName = replaceIs(imageQueue.get(i));
				String arg1 = "type=[" + type + "] order=[" + order + "]grid_size_x=" + columns + " grid_size_y=" + rows
						+ " tile_overlap=" + spacing + " first_file_index_i=1 directory=[" + imgDir + "] file_names=["
						+ fileName + "] output_textfile_name=" + configName
						+ " fusion_method=[Linear Blending] regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50 compute_overlap computation_parameters=[Save memory (but be slower)] image_output=[Fuse and display]";
				if (notAlreadyStitched(sf))
					IJ.runMacroFile(macroLocation, arg1);

				/*
				 * boolean stitchNotFinished = true; while (stitchNotFinished) {
				 */
				if (WindowManager.getIDList() == null) {

				} else {
					// stitchNotFinished = false;
					ImagePlus imp = WindowManager.getCurrentImage();
					imp.changes = false;
					FileSaver fs = new FileSaver(imp);
					fs.saveAsTiff(sf.getPath() + "\\" + renamed);
					imp.close();
				}
				// }
				i++;
			}

			imageQueue.clear();
			long end = System.currentTimeMillis();
			long timeElapsed = end - beginning;
			long remainingTime = (numMins * 60000) - timeElapsed;
			if (timeElapsed < (numMins * 60000)) {
				try {
					Date d = new Date(System.currentTimeMillis() + remainingTime);
					IJ.log("Next Scan at approx:" + DateFormat.getTimeInstance(DateFormat.LONG).format(d));
					Thread.sleep(remainingTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * 
	 * @param sf
	 * @return
	 */
	private boolean notAlreadyStitched(File sf) {
		File stitchedImage = new File(sf.getPath() + "\\" + renamed);
		if (stitchedImage.exists())
			return false;
		else
			return true;
	}

	/**
	 * This method, when given a string of the format "fld xxx ", replaces "xxx"
	 * with the letter i, and surrounds them with { and }. The method uses a
	 * StringBuffer to complete said task.
	 * 
	 * @param The
	 *            String in which the Is are to be replaced
	 * @return The String with Is replaced
	 */
	private String replaceIs(String sa) {
		int i1 = sa.indexOf("fld ") + 4;
		String s1 = sa.substring(0, i1);
		int i2 = 0;
		// Dash or Space???
		i2 = sa.indexOf(dash, i1);
		StringBuffer sb = new StringBuffer(sa);
		sb.replace(i1 - 4, i2, "Stitched");
		renamed = sb.toString();

		int i3 = i2 - (i1);
		String s2 = sa.substring(i1 + i3);

		String is = "";
		for (int i = 0; i < i3; i++) {
			is += "i";
		}
		is = "{" + is + "}";
		String finalName = s1 + is + s2;
		return finalName;
		// This is also a comment//on top of that this is a comment, within the
		// other comment
	}

	/**
	 * This method determines whether an image has already been stitched, to
	 * prevent repeat stitches.
	 * 
	 * @param Name
	 *            of the first file in a set to be stitched
	 * @return A boolean value representing whether the image set has been
	 *         stitched before or not.
	 */
	private boolean notInQueue(String fName) {
		boolean result = true;
		for (int i = 0; i < finishedImages.size(); i++) {
			if (finishedImages.get(i).equals(fName)) {
				result = false;
			}
		}
		if (result || finishedImages.size() < 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method determines whether an image name is the first of a set of
	 * images to be stitched. It determines this based on leading zeros, and the
	 * assumption the first file in each set is labeled 1.
	 * 
	 * @param The
	 *            file name to be analyzed
	 * @return a boolean value representing whether the file is the first in a
	 *         set.
	 */
	public boolean initialFound(String fName) {
		if (fName.contains("fld " + leading + "1"))
			return true;
		else
			return false;
	}

	/**
	 * This method determines, based on an initial file name, and all the file
	 * names in a folder, whether the respective final file name is also
	 * found...It assumes that if the final file name is found, all the file
	 * names in between also exist.
	 * 
	 * @param fName
	 *            The first file name
	 * @param imgs
	 *            The set of image names to be scanned
	 * @return whether the first file name has a respective last file name.
	 */
	public boolean finalFound(String fName, File[] imgs) {
		int numBeg = fName.indexOf("fld " + leading + "1") + 4;
		int numEnd = numBeg + leading.length() + 1;
		StringBuffer sb = new StringBuffer(fName);
		sb.replace(numBeg, numEnd, frontV + numF);
		String finalName = sb.toString();
		boolean result = false;
		for (File f : imgs) {
			if (f.getName().contains(finalName)) {
				result = true;
			}
		}
		if (result)
			return true;
		else
			return false;
	}

	/**
	 * This method begins the process of scanning a folder for files to be
	 * stitched. At the moment, the method asks for the file folder that
	 * contains the images. the respective XAQP protocol file for those images,
	 * the number of fields to be scanned, and the order in which they were
	 * scanned.
	 */
	private void getDir() {
		File macroLocator = new File("C:\\AutoStitch.txt");
		if (macroLocator.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(macroLocator));
				macroLocation = br.readLine();
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			OpenDialog od = new OpenDialog("Macro File Location");
			String direct = od.getDirectory();
			String fn = od.getFileName();
			macroLocation = direct + fn;
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(macroLocator));
				bw.write(direct + fn);
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		IJ.log(macroLocation);

		JFileChooser j = new JFileChooser();
		j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		j.setDialogTitle("Image Folder");
		Integer opt = j.showSaveDialog(j);
		File dir = j.getSelectedFile();
		imgDir = dir.toString();
		OpenDialog od = new OpenDialog("XAQP Location");
		String direc = od.getDirectory();
		String fn = od.getFileName();
		ArrayList<String> lines = new ArrayList<String>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(direc + fn));

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
		ArrayList<String> linesCopy = (ArrayList<String>) lines.clone();
		ArrayList<String> secondCopy = (ArrayList<String>) lines.clone();
		lines.removeIf(element -> XACQFilter(element));
		linesCopy.removeIf(element -> copyFilter(element));
		secondCopy.removeIf(element -> secondCopyFilter(element));
		getInfo(linesCopy);
		int wells = getSCInfo(secondCopy);
		int wavL = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains("<Layout")) {
				wavL = i;
				rows = extractInteger("rows=\"", "\"", lines.get(i));
				columns = extractInteger("columns=\"", "\"", lines.get(i));
			}
		}

		String[] wavInfo = new String[wavL];
		for (int i = 0; i < wavInfo.length; i++) {
			wavInfo[i] = lines.get(i);
		}

		int[] layers = new int[wavInfo.length];
		for (int i = 0; i < layers.length; i++) {
			layers[i] = extractInteger("z_slice=\"", "\"", wavInfo[i]);
			if (layers[i] == 0)
				layers[i] = 1;
		}

		int wavTotal = 0;
		for (int i : layers) {
			wavTotal += i * layers.length;
		}
		int numT = 1;
		dash = " ";
		if (lines.size() > layers.length + 1) {
			timepoints = true;
			numT = lines.size() - (layers.length + 1);
			dash = "-";
		}

		GenericDialog gd = new GenericDialog("Dialog");
		gd.addNumericField("Minutes Between File Checks", 30, 0);
		String[] labels = { "Horizontal Snake", "Vertical Snake", "Horizontal Row by Row", "Vertical Row by Row" };
		gd.addRadioButtonGroup("Imaging Method", labels, 4, 1, "Horizontal Snake");
		gd.showDialog();

		if (gd.wasOKed()) {
			TextField tf = (TextField) gd.getNumericFields().firstElement();
			numMins = Integer.parseInt(tf.getText());

			CheckboxGroup chk = (CheckboxGroup) gd.getRadioButtonGroups().firstElement();
			lookup(chk.getSelectedCheckbox().getLabel());
		} else
			return;
		totalP = numT * wavTotal * wells;
		IJ.log(String.valueOf(wells));
	}

	private int getSCInfo(ArrayList<String> secondCopy) {
		int r = extractInteger("columns=\"", "\"", secondCopy.get(0));
		int c = extractInteger("rows=\"", "\"", secondCopy.get(0));
		return r * c - (secondCopy.size() - 1);

	}

	private boolean secondCopyFilter(String element) {
		if (element.contains("<Plate "))
			return false;
		else if (element.contains("<Exclude "))
			return false;
		else
			return true;
	}

	/**
	 * Extracts the spacing percentage value from a specfic line in the XAQP
	 * file.
	 * 
	 * @param linesCopy
	 */
	private void getInfo(ArrayList<String> linesCopy) {
		spacing = extractDInteger("spacing_percentage=\"", "\"", linesCopy.get(0));
	}

	/**
	 * A simple lookup table designed to convert the terms used by the
	 * microscope, into those used by the stitching plugin
	 * 
	 * @param pattern
	 */
	private void lookup(String pattern) {
		if (pattern.equals("Vertical Snake")) {
			type = "Grid: snake by columns";
			order = "Down & Right                ";
		} else if (pattern.equals("Horizontal Snake")) {
			type = "Grid: snake by rows";
			order = "Right & Down                ";
		} else if (pattern.equals("Horizontal Row by Row")) {
			type = "Grid: row-by-row";
			order = "Right & Down                ";
		} else if (pattern.equals("Vertical Row by Row")) {
			type = "Grid: column-by-column";
			order = "Down & Right                ";
		}
	}

	/**
	 * An iPredicate method used to sort the XAQP file, in order to extract
	 * relevant information. This method is designed to pair up with a
	 * removeIf() statement, so it returns false when a line is found to be
	 * relevant to the program.
	 * 
	 * @param element
	 * @return
	 */
	private boolean XACQFilter(String element) {
		if (element.contains("z_slice="))
			return false;
		else if (element.contains("<Layout "))
			return false;
		else if (element.contains("<TimePoint "))
			return false;
		else
			return true;
	}

	/**
	 * Similar to the XAQP filter, this program is designed to find other
	 * information relevant to the program.
	 * 
	 * @param el
	 * @return
	 */
	private boolean copyFilter(String el) {
		if (el.contains("<Spacing "))
			return false;
		else
			return true;
	}

	/**
	 * This method extracts an integer value from a string, given a unique
	 * section of text before the integer is encountered, and the first
	 * character after the integer.
	 * 
	 * @param The
	 *            phrase before the integer, that is unique to it.
	 * @param The
	 *            first character after the integer
	 * @param The
	 *            String to be analyzed
	 * @return The integer that has been found
	 */
	private int extractInteger(String vo, String vc, String phrase) {

		int l = phrase.indexOf(vo);
		int m = vo.length();
		String a = phrase.substring(l + m);
		String b = a.substring(0, a.indexOf(vc));

		return Integer.valueOf(b);
	}

	/**
	 * Similar to extract integer, but converts the found value to an integer
	 * from a double.
	 * 
	 * @param The
	 *            phrase before the integer, that is unique to it.
	 * @param The
	 *            first character after the integer
	 * @param The
	 *            String to be analyzed
	 * @return The integer that has been found
	 */
	private int extractDInteger(String vo, String vc, String phrase) {

		int l = phrase.indexOf(vo);
		int m = vo.length();
		String a = phrase.substring(l + m);
		String b = a.substring(0, a.indexOf(vc));

		double d = Double.parseDouble(b);

		return (int) d;
	}

}

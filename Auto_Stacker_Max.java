import java.awt.TextField;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

public class Auto_Stacker implements PlugIn {
	String dir;
	ArrayList<String> initialImages = new ArrayList<String>();
	ArrayList<String> stackedImages = new ArrayList<String>();
	int stacksProcessed;
	int numLayers;

	@Override
	public void run(String arg) {
		TimeInfo ti = getDir();
		if (ti.equals(null))
			System.exit(0);
		File stackedFolder = new File(dir + "\\" + "Stacked");
		stackedFolder.mkdirs();
		try {
			beginlayering(ti, stackedFolder);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void beginlayering(TimeInfo ti, File direc) throws InterruptedException {
		int emptyChecks = 0;
		int c = 0;
		while (emptyChecks < ti.getChecks()) {
			initialImages.clear();
			int length = listInitialLayers();
			String zeros = "";
			if (length > 1)
				zeros = "0";

			IJ.log(String.valueOf(numLayers));
			if (initialImages.size() < 1)
				emptyChecks++;
			else
				for (String img : initialImages) {
					String[] Images = new String[numLayers];
					for (int layer = 0; layer < numLayers; layer++) {
						StringBuffer sb = new StringBuffer(img);
						sb.replace(img.indexOf("z "), img.indexOf("z ") + length + 2, "z " + zeros + (layer + 1));
						Images[layer] = sb.toString();
					}
					for (String image : Images) {
						IJ.open(dir + "\\" + image);
					}
					IJ.runMacroFile(getStackMacro());
					ImagePlus stackedVersion = WindowManager.getCurrentImage();
					IJ.runMacroFile(getProjectMacro());
					ImagePlus projectedVersion = WindowManager.getCurrentImage();
					stackedVersion.changes = false;
					stackedVersion.close();
					projectedVersion.changes = false;
					FileSaver fs = new FileSaver(projectedVersion);
					fs.saveAsTiff(direc + "\\" + img);
					projectedVersion.close();

				}
			Thread.sleep((long) (ti.getTime() * 60000));
			c++;
		}

		// a
	}

	private String getStackMacro() {
		File macroLocation = new File(IJ.getDirectory("macros") + "\\" + "ImageStacking.ijm");
		if (macroLocation.exists()) {
			return macroLocation.getPath();
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(macroLocation.getPath()));
				bw.write("run(\"Images to Stack\");");
				bw.close();

			} catch (IOException e) {
			}

			return macroLocation.getPath();
		}
	}

	private String getProjectMacro() {
		File macroLocation = new File(IJ.getDirectory("macros") + "\\" + "ImageProjecting.ijm");
		if (macroLocation.exists()) {
			return macroLocation.getPath();
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(macroLocation.getPath()));
				bw.write("run(\"Z Project...\", \"projection=[Max Intensity]\");");
				bw.close();

			} catch (IOException e) {
			}

			return macroLocation.getPath();
		}
	}

	private int listInitialLayers() {
		File imageDir = new File(dir);
		File[] images = imageDir.listFiles();
		int length = 1;
		for (File image : images) {
			if (image.getName().contains("z 01") && notStacked(image)) {
				initialImages.add(image.getName());
				stackedImages.add(image.getName());
				length = 2;
			} else if (image.getName().contains("z 1") && notStacked(image)) {
				initialImages.add(image.getName());
				stackedImages.add(image.getName());
			}

		}
		return length;
	}

	private boolean notStacked(File comparator) {
		for (String imgName : stackedImages) {
			if (comparator.getName().equals(imgName)) {
				return false;
			}
		}
		return true;// a
	}

	private TimeInfo getDir() {
		JFileChooser j = new JFileChooser();
		j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		j.setDialogTitle("Image Folder");
		Integer opt = j.showSaveDialog(j);
		File dir = j.getSelectedFile();
		this.dir = dir.toString();

		GenericDialog gd = new GenericDialog("Stacking Info");
		gd.addNumericField("Checks Before Program Cancel", 3.0, 0);
		gd.addNumericField("Time Between Checks (Minutes)", 10.0, 0);
		gd.showDialog();
		int checks = Integer.valueOf(((TextField) gd.getNumericFields().firstElement()).getText());
		int time = Integer.valueOf(((TextField) gd.getNumericFields().elementAt(1)).getText());

		OpenDialog od = new OpenDialog("XAQP Location");
		String xaqpDir = od.getDirectory();
		String xaqpName = od.getFileName();
		String xaqpLocation = xaqpDir + xaqpName;
		getNumLayers(xaqpLocation);

		if (gd.wasOKed()) {
			return new TimeInfo(checks, time);
		} else {
			return null;
		}

	}

	private void getNumLayers(String xaqpLocation) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(xaqpLocation));
			ArrayList<String> lines = new ArrayList<String>();
			String line = br.readLine();
			while (line != null) {
				lines.add(line);
				line = br.readLine();
			}

			lines.removeIf(element -> xaqpFilter(element));

			int l = lines.get(0).indexOf("z_slice=\"");
			int m = "z_slice=\"".length();
			String a = lines.get(0).substring(l + m);
			String b = a.substring(0, a.indexOf("\""));

			numLayers = Integer.valueOf(b);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean xaqpFilter(String element) {
		if (element.contains(" z_slice=\"") && element.contains("3-D"))
			return false;
		else
			return true;
	}

	class TimeInfo {
		private final int checks;
		private final int time;

		public TimeInfo(int checks, int time) {
			this.checks = checks;
			this.time = time;
		}

		public int getChecks() {
			return checks;
		}

		public int getTime() {
			return time;
		}
	}

}

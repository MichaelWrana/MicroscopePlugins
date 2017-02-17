import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;

import java.awt.Checkbox;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import static java.nio.file.StandardCopyOption.*;

import ij.plugin.*;

public class ROI_Renamer_Stitcher implements PlugIn {
	String[] Fields;
	ArrayList<String> XDCE = new ArrayList<String>();
	ArrayList<String> XFPF = new ArrayList<String>();
	String[] imgNames;
	String imgDir;
	int numChannels;
	int[] widths;
	int[] heights;
	String[] formatNames;

	@Override
	public void run(String arg) {
		/*
		 * IF THE PROGRAM DOES NOT WORK CORRECTLY, THE AREA WITH THE LEAST
		 * TESTING ETC, IS LINES 50-65
		 */
		boolean[] b = readFiles();
		if (b == null) {
			IJ.showMessage("Program Cancelled");
		} else {
			sortFiles();
			String[] sA = sortXDCE();
			readFolder(sA);
			int[] iA = sortXFPF();
			renameFiles(sA, iA, b);
			stitchFiles(iA, b);

		}

	}

	private void stitchFiles(int[] ia, boolean[] b) {
		String[] sa = replaceIs(formatNames, b[1]);
		ArrayList<String> done = new ArrayList<String>();
		int i = 1;
		while (sa.length < done.size()) {
			for (String s : sa) {
				if (s.contains("ROI - 0" + i)) {
					stitchSet(s, widths[i - 1], heights[i - 1], "TileConfig");
					done.add("-");
				}

			}
			i++;
		}

	}

	private void stitchSet(String fName, int width, int height, String configName) {
		String arg = "type=[Grid: column-by-column] order=[Down & Right                ]grid_size_x=" + width
				+ " grid_size_y=" + height + " tile_overlap=5 first_file_index_i=1 directory=[" + imgDir
				+ "] file_names=[" + fName + "] output_textfile_name=" + configName
				+ " fusion_method=[Linear Blending] regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50 compute_overlapcomputation_parameters=[Save memory (but be slower)]image_output=[Fuse and display]";
		IJ.runMacroFile("C:\\Users\\admin\\Desktop\\Fiji.app\\macros\\stitch_from_args.ijm", arg);
	}

	private String[] replaceIs(String[] sa, boolean b) {
		String[] result = new String[sa.length];
		int j = 0;
		for (String e : sa) {
			int i1 = e.indexOf("fld ") + 4;
			String s1 = e.substring(0, i1);
			int i2 = 0;
			if (b == false) {
				i2 = e.indexOf(")", i1);
			} else {
				i2 = e.indexOf(" ", i1);
			}

			int i3 = i2 - (i1);
			String s2 = e.substring(i1 + i3);

			String is = "";
			for (int i = 0; i < i3; i++) {
				is += "i";
			}
			is = "{" + is + "}";
			String finalName = s1 + is + s2;
			result[j] = finalName;
			j++;
		}
		return result;
	}

	private void renameFiles(String[] sa, int[] ia, boolean[] b) {
		formatNames = new String[ia.length * numChannels];
		int numNumbers = 0;

		for (int i = 0; i < numChannels; i++) {
			int roiNum = 0;
			int l = 0;
			for (int k = i; k < imgNames.length; k += numChannels) {

				String s = imgNames[k];
				String s2 = "fld ";
				int i1 = s.indexOf(s2) + 4;
				String s3 = s.substring(0, i1);
				String s4 = "";
				if (b[1] == false) {
					s4 = extractString(s2, ")", s);
				} else {
					s4 = extractString(s2, " ", s);
				}
				int i2 = s4.length();
				numNumbers = i2;
				String s5 = s.substring(i1 + i2);
				String s6 = "%0" + i2 + "d";
				String s7 = String.format(s6, l + 1);
				String s9 = s3.substring(s3.indexOf("("));
				String s10 = "ROI - " + roiNum;
				String s11 = s10 + s9 + s7 + s5;

				File dir = new File(imgDir + "Renamed_Images");
				dir.mkdirs();
				File f = new File(imgDir + s11);
				File f2 = new File(imgDir + imgNames[k]);

				Path source = Paths.get(imgDir + imgNames[k]);
				Path target = Paths.get(dir.getPath() + "\\" + s11);
				IJ.log(source.toString());
				IJ.log(target.toString());
				try {
					Files.copy(source, target, REPLACE_EXISTING);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;

				}
				f2.renameTo(f);

				l++;

				if (l == ia[roiNum]) {
					roiNum++;
					l = 0;

				}
			}

		}
		File folder = new File(imgDir);
		String[] fileList = folder.list();
		ArrayList<String> imageList = new ArrayList<String>();
		for (String e : fileList) {
			imageList.add(e);
		}
		imageList.removeIf(element -> nameFilter(element));
		imgNames = new String[imageList.size()];
		for (int i = 0; i < imageList.size(); i++) {
			imgNames[i] = imageList.get(i);
		}
		Arrays.sort(imgNames);

		String s0 = String.format("%0" + numNumbers + "d", 1);
		int m = 0;
		for (String s : imgNames) {
			if (s.contains(s0)) {
				formatNames[m] = s;
				IJ.log(s);
				m++;
			}
		}
	}

	private int[] sortXFPF() {
		String[] ROIs = new String[XFPF.size() / 2];
		int j = 0;
		for (int i = 1; i < XFPF.size(); i += 2) {
			ROIs[j] = XFPF.get(i);
			j++;
		}
		int roiSizes[] = new int[ROIs.length];
		widths = new int[ROIs.length];
		heights = new int[ROIs.length];
		for (int i = 0; i < ROIs.length; i++) {
			int width = extractValue("%", "%", ROIs[i]);
			int height = extractValue("#", "#", ROIs[i]);
			widths[i] = width;
			heights[i] = height;
			roiSizes[i] = width * height;
		}
		return roiSizes;

	}

	private String[] sortXDCE() {
		String[] channels = new String[XDCE.size()];
		for (int i = 0; i < XDCE.size() - 1; i++) {
			channels[i] = extractString("name=\"", "\"", XDCE.get(i));
		}
		channels[XDCE.size() - 1] = extractString(" pattern=\"", "\"", XDCE.get(XDCE.size() - 1));
		return channels;
	}

	private void readFolder(String[] channels) {
		File folder = new File(imgDir);
		String[] fileList = folder.list();
		ArrayList<String> imageList = new ArrayList<String>();
		for (String e : fileList) {
			imageList.add(e);
		}
		imageList.removeIf(element -> nameFilter(element));
		imgNames = new String[imageList.size()];
		for (int i = 0; i < imageList.size(); i++) {
			imgNames[i] = imageList.get(i);
		}
		Arrays.sort(imgNames);

		String[] Channels = new String[channels.length - 1];
		for (int i = 0; i < channels.length - 1; i++) {
			Channels[i] = channels[i];
		}
		numChannels = Channels.length / 2;
	}

	private String getImageDirectory() {
		OpenDialog od = new OpenDialog("Directory Containing Images");
		return od.getDirectory();
	}

	public void sortFiles() {
		XDCE.removeIf(e -> xdceFilter(e));
		XFPF.removeIf(e -> xfpfFilter(e));
	}

	public boolean[] readFiles() {
		OpenDialog od1 = new OpenDialog("xdce file location");
		String dir1 = od1.getDirectory();
		String fn1 = od1.getFileName();

		try {
			BufferedReader br = new BufferedReader(new FileReader(dir1 + fn1));
			String line = br.readLine();
			while (line != null) {
				XDCE.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			IJ.error("File Read Error", e.getMessage());
			e.printStackTrace();
		}

		OpenDialog od = new OpenDialog("xfpf file location");
		String dir = od.getDirectory();
		String fn = od.getFileName();

		try {
			BufferedReader br = new BufferedReader(new FileReader(dir + fn));
			String line = br.readLine();
			while (line != null) {
				XFPF.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			IJ.error("File Read Error", e.getMessage());
			e.printStackTrace();
		}

		GenericDialog gd = new GenericDialog("Message");
		String[] labels = { ".xdce file in same directory as image files", "Multiple wavelengths used" };
		boolean[] values = { true, false };
		gd.addCheckboxGroup(2, 1, labels, values);
		gd.showDialog();

		Checkbox Dir = (Checkbox) gd.getCheckboxes().elementAt(0);
		Checkbox Wav = (Checkbox) gd.getCheckboxes().elementAt(1);

		if (gd.wasCanceled()) {
			return null;
		} else {
			boolean[] results = new boolean[2];
			results[0] = Dir.getState();
			if (results[0] == true) {
				imgDir = dir1;
			} else {
				imgDir = getImageDirectory();
			}
			results[1] = Wav.getState();
			return results;
		}

	}

	public boolean xdceFilter(String element) {
		if (element.contains("<ExcitationFilter ") && element.contains("wavelength"))
			return false;
		else if (element.contains("<EmissionFilter ") && element.contains("wavelength"))
			return false;
		else if (element.contains(" pattern=\""))
			return false;
		else
			return true;

	}

	public boolean xfpfFilter(String element) {
		if (element.contains("<!--"))
			return false;
		else
			return true;

	}

	public boolean nameFilter(String element) {
		if (element.contains("fld"))
			return false;
		else
			return true;
	}

	public boolean wavFilter(String element, String filter) {
		return false;

	}

	public int extractValue(String vo, String vc, String phrase) {

		int l = phrase.indexOf(vo);
		int m = vo.length();
		String a = phrase.substring(l + m);
		String b = a.substring(0, a.indexOf(vc));

		return Integer.valueOf(b);
	}

	public String extractString(String vo, String vc, String phrase) {

		int l = phrase.indexOf(vo);
		int m = vo.length();
		String a = phrase.substring(l + m);
		String b = a.substring(0, a.indexOf(vc));

		return b;
	}

}

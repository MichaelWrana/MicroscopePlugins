# MicroscopePlugins

###Auto_Stitcher.java
This program is designed to stitch individual microscope images, as they are added to a folder.  This is useful since creating a large image stitch is a slow process in Fiji, and often the microscope images are generated over 8+ hours during the night.

###Auto_Stacker_Max.java
Similarly to the stitcher, the stacker takes a series of microscope images which are being slowly added to a folder overnight, and creates an Image Stack from them.

###Freehand_Generator.java
This PlugIn for imageJ, is designed to be used as a utility tool, for use with High-Throughput microscopes. The plugin takes a stitched image of a scan, and has the user select regions they want to be re-scanned. The program then creates a pointlist for the microscope to re-scan.

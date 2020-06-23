package spqPrep_jnh;
/** ===============================================================================
* SpermQ Preparator Version 0.0.2
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*  
* See the GNU General Public License for more details.
*  
* Copyright (C) Jan Niklas Hansen
* Date: July 09, 2018 (This Version: June 23, 2020)
*   
* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
* =============================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.text.*;

import javax.swing.UIManager;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.text.*;

public class SpqPrepMain implements PlugIn, Measurements {
	// Name variables
	static final String PLUGINNAME = "SpermQ Preparator";
	static final String PLUGINVERSION = "0.0.2";

	// Fix fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font TextFont = new Font("Sansserif", Font.PLAIN, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	Font roiFont = new Font("Sansserif", Font.PLAIN, 20);

	// Fix formats
	DecimalFormat dformat6 = new DecimalFormat("#0.000000");
	DecimalFormat dformat3 = new DecimalFormat("#0.000");
	DecimalFormat dformat0 = new DecimalFormat("#0");
	DecimalFormat dformatDialog = new DecimalFormat("#0.000000");

	static final String[] nrFormats = { "US (0.00...)", "Germany (0,00...)" };

	static SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// Progress Dialog
	ProgressDialog progressDialog;
	boolean processingDone = false;
	boolean continueProcessing = true;

	// -----------------define params for Dialog-----------------
	static final String[] taskVariant = { "active image in FIJI", "multiple images (open multi-task manager)",
			"all images open in FIJI" };
	String selectedTaskVariant = taskVariant[1];
	int tasks = 1;

	boolean gb1 = true, substMin = false, substMedi = false, subtractBG1 = true, crop = true, invert = false,
			rescale = false, addHeads = false;
	double gaussianSigma1 = 0.5, gaussianSigmaHead = 3;
	int subtrBG1Rad = 5;

	String[] thresholdMethods = { "Default", "IJ_IsoData", "Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean",
			"MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen" };
	String selectedThresholdMethod = "Otsu";

	// static final String[] bioFormats = {".tif" , "raw microscopy file (e.g.
	// OIB-file)"};
	// String bioFormat = bioFormats [0];
	//
	boolean saveDate = false;
	// -----------------define params for Dialog-----------------

	// Variables for processing of an individual task
	// enum channelType {PLAQUE,CELL,NEURITE};

public void run(String arg) {
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//-------------------------GenericDialog--------------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	GenericDialog gd = new GenericDialog(PLUGINNAME + " - set parameters");	
	//show Dialog-----------------------------------------------------------------
	//.setInsets(top, left, bottom)
	gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2016-2020 JN Hansen", SuperHeadingFont);	
	gd.setInsets(5,0,0);	gd.addChoice("process ", taskVariant, selectedTaskVariant);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("1. invert image (e.g. for phase contrast imaging)", invert);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("2. rescale pixel values to maximal range (min/max)", rescale);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("3. crop to user defined ROI", crop);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("4. perform Gaussian blur", gb1);
	gd.setInsets(0,0,0);	gd.addNumericField("Sigma of Gaussian blur", gaussianSigma1, 2);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("5. subtract stack projection (Minimum) and rescale intensities", substMin);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("6. subtract stack projection (Median) and rescale intensities", substMedi);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("7. subtract background", subtractBG1);
	gd.setInsets(0,0,0);	gd.addNumericField("Subtract BG radius (px)", subtrBG1Rad, 0);
	
	//TODO implement and test
//	gd.setInsets(10,0,0);	gd.addCheckbox("8. find and add heads by threshold algorithm", addHeads);
//	gd.setInsets(0,0,0);	gd.addNumericField("Sigma of Gaussian blur before thresholding", gaussianSigmaHead, 2);
//	gd.setInsets(0,0,0);	gd.addChoice("Thresholding Method", thresholdMethods, selectedThresholdMethod);
	
//	gd.setInsets(10,0,0);	gd.addChoice("Input filetype", bioFormats, bioFormat);
	
	gd.setInsets(10,0,0);	gd.addMessage("Output settings", SubHeadingFont);
	gd.setInsets(0,0,0);	gd.addCheckbox("save date in output file names", saveDate);
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//read and process variables--------------------------------------------------	
	selectedTaskVariant = gd.getNextChoice();
	
	invert = gd.getNextBoolean();
	rescale = gd.getNextBoolean();
	crop = gd.getNextBoolean();
	gb1 = gd.getNextBoolean();
	gaussianSigma1 = gd.getNextNumber();
	substMin = gd.getNextBoolean();
	substMedi = gd.getNextBoolean();
	subtractBG1 = gd.getNextBoolean();
	subtrBG1Rad = (int)gd.getNextNumber();
	
//	addHeads = gd.getNextBoolean();
//	gaussianSigmaHead = gd.getNextNumber();
//	selectedThresholdMethod = gd.getNextChoice();
	
//	bioFormat = gd.getNextChoice();
	
	saveDate = gd.getNextBoolean();
	dformat6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	dformat3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	dformat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	dformatDialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	//read and process variables--------------------------------------------------
	if (gd.wasCanceled()) return;
	
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//---------------------end-GenericDialog-end----------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&


	String name [] = {"",""};
	String dir [] = {"",""};
	ImagePlus allImps [] = new ImagePlus [2];
	{
		//Improved file selector
		try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
		if(selectedTaskVariant.equals(taskVariant[1])){
			OpenFilesDialog od = new OpenFilesDialog ();
			od.setLocation(0,0);
			od.setVisible(true);
			
			od.addWindowListener(new java.awt.event.WindowAdapter() {
		        public void windowClosing(WindowEvent winEvt) {
		        	return;
		        }
		    });
		
			//Waiting for od to be done
			while(od.done==false){
				try{
					Thread.currentThread().sleep(50);
			    }catch(Exception e){
			    }
			}
			
			tasks = od.filesToOpen.size();
			name = new String [tasks];
			dir = new String [tasks];
			for(int task = 0; task < tasks; task++){
				name[task] = od.filesToOpen.get(task).getName();
				dir[task] = od.filesToOpen.get(task).getParent() + System.getProperty("file.separator");
			}		
		}else if(selectedTaskVariant.equals(taskVariant[0])){
			if(WindowManager.getIDList()==null){
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				return;
			}
			FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
			name [0] = info.fileName;	//get name
			dir [0] = info.directory;	//get directory
			tasks = 1;
		}else if(selectedTaskVariant.equals(taskVariant[2])){	// all open images
			if(WindowManager.getIDList()==null){
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				return;
			}
			int IDlist [] = WindowManager.getIDList();
			tasks = IDlist.length;	
			if(tasks == 1){
				selectedTaskVariant=taskVariant[0];
				FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
				name [0] = info.fileName;	//get name
				dir [0] = info.directory;	//get directory
			}else{
				name = new String [tasks];
				dir = new String [tasks];
				allImps = new ImagePlus [tasks];
				for(int i = 0; i < tasks; i++){
					allImps[i] = WindowManager.getImage(IDlist[i]); 
					FileInfo info = allImps[i].getOriginalFileInfo();
					name [i] = info.fileName;	//get name
					dir [i] = info.directory;	//get directory
				}		
			}
					
		}
	}
	 	
	//add progressDialog
	progressDialog = new ProgressDialog(name, tasks);
	progressDialog.setLocation(0,0);
	progressDialog.setVisible(true);
	progressDialog.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(WindowEvent winEvt) {
        	if(processingDone==false){
        		IJ.error("Script stopped...");
        	}
        	continueProcessing = false;	        	
        	return;
        }
	});
		
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//------------------------------PROCESSING------------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

	//set ROIs
	Roi [] selections = new Roi [tasks];
   	ImagePlus imp; 	  	
   	if(crop){
		IJ.setTool("rectangle");
		{
			ImagePlus maxImp;
			for(int task = 0; task < tasks; task++){
				if(selectedTaskVariant.equals(taskVariant[1])){
					imp = IJ.openVirtual(dir [task] + name [task]);
				}else if(selectedTaskVariant.equals(taskVariant[0])){
		   			imp = WindowManager.getCurrentImage().duplicate();
		   			imp.deleteRoi();
		   		}else{
		   			imp = allImps[task].duplicate();
		   			imp.deleteRoi();
		   		}
				
				IJ.run(imp, "Z Project...", "projection=[Max Intensity]");
				maxImp = WindowManager.getCurrentImage();
				
				imp.changes = false;
				imp.close();
								
				while(true){
					progressDialog.replaceBarText("user interaction required... [task " + (task+1) + "/" + tasks + "]");
					new WaitForUserDialog("Set a Roi containing parts of the cell in every frame [task " + (task+1) + "/" + tasks + "]").show();
					if(maxImp.getRoi()!=null) break;
				}		
				selections [task] = maxImp.getRoi();
				
				maxImp.changes = false;
				maxImp.close();
				System.gc();
			}
		}
		System.gc();
	}
	
   	RoiEncoder re;
   	for(int task = 0; task < tasks; task++){
	running: while(continueProcessing){
		Date startDate = new Date();
		progressDialog.updateBarText("in progress...");
		//Check for problems
				if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".txt")){
					progressDialog.notifyMessage("Task " + (task+1) + "/" + tasks + ": A file is no image! Could not be processed!", ProgressDialog.ERROR);
					progressDialog.moveTask(task);	
					break running;
				}
				if(name[task].substring(name[task].lastIndexOf("."),name[task].length()).equals(".zip")){	
					progressDialog.notifyMessage("Task " + (task+1) + "/" + tasks + ": A file is no image! Could not be processed!", ProgressDialog.ERROR);
					progressDialog.moveTask(task);	
					break running;
				}		
		//Check for problems
				
		//open Image
		   	try{
		   		if(selectedTaskVariant.equals(taskVariant[1])){
		   			//TODO implement bioformats mode
//		   			if(bioFormat.equals(bioFormats[0])){
		   				//TIFF file
		   				imp = IJ.openImage(""+dir[task]+name[task]+"");		
//		   			}else if(bioFormat.equals(bioFormats[1])){
//		   				//bio format reader
//		   				IJ.run("Bio-Formats", "open=[" +dir[task] + name[task]
//		   						+ "] autoscale color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT");
//		   				imp = WindowManager.getCurrentImage();		   				
//		   			}else{
//		   				progressDialog.notifyMessage("Task " + (task+1) + "/" + tasks + ": Image could not be opened!", ProgressDialog.ERROR);
//						progressDialog.moveTask(task);	
//						break running;
//		   			}
		   			
		   		}else if(selectedTaskVariant.equals(taskVariant[0])){
		   			imp = WindowManager.getCurrentImage().duplicate();
		   			imp.deleteRoi();
		   		}else{
		   			imp = allImps[task].duplicate();
		   			imp.deleteRoi();
		   		}
		   	}catch (Exception e) {
		   		progressDialog.notifyMessage("Task " + (task+1) + "/" + tasks + ": file is no image - could not be processed!", ProgressDialog.ERROR);
				progressDialog.moveTask(task);	
				break running;
			}
		   	imp.hide();
			imp.deleteRoi();
		   	imp.lock();
			
	   	/******************************************************************
		*** 						PROCESSING							***	
		*******************************************************************/
			
		   	imp.deleteRoi();
	   		imp.unlock();
	   		
	   		ImagePlus impCopy = imp;
	   		if(addHeads){
	   			impCopy = imp.duplicate();
	   		}
	   		
	   		if(invert){
	   			invertIntensities(imp);
//	   			IJ.run(imp, "Invert", "stack");
	   	 	}
	   		
	   		double min = Double.MAX_VALUE, max = 0.0, value;	   		
	   		if(rescale){
	   			//determine min / max
	   			for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s);
		   					if(value < min){
		   						min = value;
		   					}
		   					if(value > max){
		   						max = value;
		   					}
		   				}
		   			}
		   		}	
	   			
	   			for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s);
		   					value = (value-min)/(max-min)*(Math.pow(2.0,(double)imp.getBitDepth())-1);
		   					imp.getStack().setVoxel(x, y, s, value);
		   				}
		   			}
		   		}
	   	 	}
	   		
		   	if(crop){
		   		imp.setRoi(selections[task]);
		   		IJ.run(imp, "Crop", "");
		   		if(addHeads){
			   		//TODO implement and tested
		   			impCopy.setRoi(selections[task]);
			   		IJ.run(impCopy, "Crop", "");
		   		}
		   		
		   	}
		   	
		   	if(gb1){
			   	for(int s = 0; s < imp.getStackSize(); s++){
			   		imp.setSlice(s+1);
			   		imp.getProcessor().blurGaussian(gaussianSigma1);
			   	}		   		
		   	}
		   	
		   	double minMax = 0.0, conversionFactorMin = 1.0;
		   	if(substMin){
		   		ImagePlus minImp = minIntensityProjection(imp);
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s)-minImp.getStack().getVoxel(x, y, 0);
		   					if(value<0.0) value = 0.0;
		   					if(value>minMax)	minMax = value;
//	   						imp.getStack().setVoxel(x, y, s, value);
		   				}
		   			}
		   		}
		   		conversionFactorMin = (Math.pow(2.0, (double)imp.getBitDepth())-1.0) / minMax ;
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s)-minImp.getStack().getVoxel(x, y, 0);
		   					if(value<0.0) value = 0.0;
		   					value *= conversionFactorMin;
	   						imp.getStack().setVoxel(x, y, s, value);
		   				}
		   			}
		   		}
		   	}
		   	
		   	double mediMax = 0.0, conversionFactorMedi = 1.0;
		   	if(substMedi){		   		
		   		ImagePlus mediImp = medianIntensityProjection(imp);		   		
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s)-mediImp.getStack().getVoxel(x, y, 0);
		   					if(value<0.0) value = 0.0;
		   					if(value>mediMax)	mediMax = value;
//	   						imp.getStack().setVoxel(x, y, s, value);
		   				}
		   			}
		   		}		   		
		   		conversionFactorMedi = (Math.pow(2.0, (double)imp.getBitDepth())-1.0) / mediMax;
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					value = imp.getStack().getVoxel(x, y, s)-mediImp.getStack().getVoxel(x, y, 0);
		   					if(value<0.0) value = 0.0;
		   					value *= conversionFactorMedi;
	   						imp.getStack().setVoxel(x, y, s, value);
		   				}
		   			}
		   		}
		   	}
		   	
		   	if(subtractBG1){
		   		if(imp.getStackSize()>1){
		   			IJ.run(imp, "Subtract Background...", "rolling=" + subtrBG1Rad + " stack");
		   		}else{
		   			IJ.run(imp, "Subtract Background...", "rolling=" + subtrBG1Rad);
		   		}
		   	}
		   	
		   	if(addHeads){
		   		//TODO implement and tested
//		   		imp.show();
//		   		new WaitForUserDialog("").show();
		   		double maxForHeads [] = new double [imp.getStackSize()];
		   		Arrays.fill(maxForHeads, Double.NEGATIVE_INFINITY);
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					if(imp.getStack().getVoxel(x, y, s) > maxForHeads [s]){
		   						maxForHeads [s] = imp.getStack().getVoxel(x, y, s);
		   					}
		   				}
		   			}
		   		}	
		   		double maxForHead = getMedian(maxForHeads);
		   		if(maxForHead < (Math.pow(2.0, (double)imp.getBitDepth())-1.0)){
		   			maxForHead += 1.0;
		   		}
		   		
		   		if(gaussianSigmaHead>0.0){
		   			for(int s = 0; s < impCopy.getStackSize(); s++){
				   		impCopy.setSlice(s+1);
				   		impCopy.getProcessor().blurGaussian(gaussianSigmaHead);
				   	}
		   		}		   					   			
		   		for(int s = 0; s < impCopy.getStackSize(); s++){
		   			thresholdImage(impCopy, selectedThresholdMethod, s+1);
		   		}	
//		   		int counter;
		   		for(int x = 0; x < imp.getWidth(); x++){
		   			for(int y = 0; y < imp.getHeight(); y++){
//		   				counter = 0;
//		   				for(int s = 0; s < imp.getStackSize(); s++){
//		   					if(impCopy.getStack().getVoxel(x, y, s) > 0.0){
//		   						counter ++
//		   					}
//		   					
//		   				}
		   				for(int s = 0; s < imp.getStackSize(); s++){
		   					if(impCopy.getStack().getVoxel(x, y, s) > 0.0){
		   						imp.getStack().setVoxel(x, y, s, maxForHead);
		   					}
		   				}
		   			}
		   		}
		   	}
		   	imp.lock();

			
		/******************************************************************
		*** 						OUPUT OPTIONS						***	
		*******************************************************************/
		//Define Output File Names
			Date currentDate = new Date();
			
			String filePrefix;
			if(name[task].contains(".")){
				filePrefix = name[task].substring(0,name[task].lastIndexOf(".")) + "_SpQP";
			}else{
				filePrefix = name[task] + "_SpQP";
			}
			
			if(saveDate){
				filePrefix += "_" + NameDateFormatter.format(currentDate);
			}
						
			filePrefix = dir[task] + filePrefix;
		//Define Output File Names
					
		//start metadata file
			TextPanel tp1 =new TextPanel("results");
			
			tp1.append("Saving date:	" + FullDateFormatter.format(currentDate)
						+ "	Starting date:	" + FullDateFormatter.format(startDate));
			tp1.append("Image name:	" + name[task]);
			tp1.append("");
			tp1.append("Processing settings:");
			if(invert){
				tp1.append("Image intensities inverted");
			}
			if(rescale){
				tp1.append("Pixel values rescaled to maximal range (adapted min/max)");
				tp1.append("Old min:	" + dformatDialog.format(min) + "	= new min:	" + dformatDialog.format(0.0));
				tp1.append("Old max:	" + dformatDialog.format(max) 
					+ "	= new max:	" + dformatDialog.format((max-min)/(max-min)*(Math.pow(2.0,(double)imp.getBitDepth())-1)));
			}
			if(crop){
				tp1.append("Image cropped to Roi");
				re = new RoiEncoder(filePrefix + "_roi");
				try {
					re.write(selections [task]);
				} catch (IOException e) {
					IJ.error("ROI Manager", e.getMessage());
				}
//				IJ.save
			}
			if(gb1){
				tp1.append("Gaussian blur - sigma" + dformatDialog.format(gaussianSigma1));
			}
			if(substMin){
				tp1.append("Subtract stack-(minimum-)projection from entire stack and rescale intensities:");
				tp1.append("	multiplication factor applied for rescaling =	" + dformat6.format(conversionFactorMin));
			}
			if(substMedi){
				tp1.append("Subtract stack-(median-)projection from entire stack and rescale intensities:");
				tp1.append("	multiplication factor applied for rescaling =	" + dformat6.format(conversionFactorMedi));
			}
			if(subtractBG1){
				tp1.append("Subtract background - radius" + dformatDialog.format(subtrBG1Rad));
			}
			if(addHeads){
		   		//TODO implement and tested
				tp1.append("Add Heads by threshold from raw image");
				tp1.append("	threshold algorithm:	" + this.selectedThresholdMethod);
				if(gaussianSigmaHead>0.0){

					tp1.append("	Gaussian blur - sigma" + dformatDialog.format(gaussianSigmaHead));
				}
			}
			addFooter(tp1, currentDate);				
			tp1.saveAs(filePrefix + "m.txt");
			
		//Output Datafiles
			IJ.saveAsTiff(imp, filePrefix + ".tif");			
			imp.unlock();
			imp.changes = false;
			imp.close();
		//Output Datafile
		processingDone = true;
		break running;
	}	
	progressDialog.updateBarText("finished!");
	progressDialog.setBar(1.0);
	progressDialog.moveTask(task);
}
}

	public static double getMedian(double [] values){
		double [] medians = new double [values.length];
		for(int i = 0; i < values.length; i++){
			medians [i] = values [i];
		}
		
		Arrays.sort(medians);
		
		if(medians.length%2==0){
			return (medians[(int)((double)(medians.length)/2.0)-1]+medians[(int)((double)(medians.length)/2.0)])/2.0;
		}else{
			return medians[(int)((double)(medians.length)/2.0)];
		}		
	}

	private void addFooter(TextPanel tp, Date currentDate) {
		tp.append("");
		tp.append("Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '" + PLUGINNAME
				+ "', an ImageJ plug-in by Jan Niklas Hansen (jan.hansen@uni-bonn.de), see https://github.com/hansenjn/SpermQ_Preparator for details.");
		tp.append("The plug-in '" + PLUGINNAME + "' is distributed in the hope that it will be useful,"
				+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
				+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
		tp.append("Plug-in version:	V" + PLUGINVERSION);

	}

	private static ImagePlus minIntensityProjection(ImagePlus imp) {
		ImagePlus impMin = IJ.createImage("minimum projection", imp.getWidth(), imp.getHeight(), 1, imp.getBitDepth());

		int maxValue = (int) (Math.pow(2.0, imp.getBitDepth()) - 1);

		for (int x = 0; x < imp.getWidth(); x++) {
			for (int y = 0; y < imp.getHeight(); y++) {
				impMin.getStack().setVoxel(x, y, 0, maxValue);
				for (int s = 0; s < imp.getStackSize(); s++) {
					if (imp.getStack().getVoxel(x, y, s) < impMin.getStack().getVoxel(x, y, 0)) {
						impMin.getStack().setVoxel(x, y, 0, imp.getStack().getVoxel(x, y, s));
					}
				}
			}
		}

		impMin.setCalibration(imp.getCalibration());
		return impMin;
	}

	private static ImagePlus medianIntensityProjection(ImagePlus imp) {
		ImagePlus impMedi = IJ.createImage("median projection", imp.getWidth(), imp.getHeight(), 1, imp.getBitDepth());

		double medians[] = new double[imp.getStackSize()];
		double median;
		for (int x = 0; x < imp.getWidth(); x++) {
			for (int y = 0; y < imp.getHeight(); y++) {
				// Arrays.fill(medians, Double.NaN);
				for (int s = 0; s < imp.getStackSize(); s++) {
					medians[s] = imp.getStack().getVoxel(x, y, s);
				}
				Arrays.sort(medians);

				if (medians.length % 2 == 0) {
					median = (medians[(int) ((double) (medians.length) / 2.0) - 1]
							+ medians[(int) ((double) (medians.length) / 2.0)]) / 2.0;
				} else {
					median = medians[(int) ((double) (medians.length) / 2.0)];
				}
				impMedi.getStack().setVoxel(x, y, 0, median);
			}
		}

		impMedi.setCalibration(imp.getCalibration());
		return impMedi;
	}

	/**
	 * @param slice:
	 *            1 <= stackImage <= stacksize
	 */
	private static void thresholdImage(ImagePlus imp, String algorithm, int stackImage) {
		// threshold image
		imp.setSlice(stackImage);
		IJ.setAutoThreshold(imp, (algorithm + " dark"));
		double minThreshold = imp.getProcessor().getMinThreshold();
		double imageMax = Math.pow(2.0, imp.getBitDepth()) - 1.0;

		for (int x = 0; x < imp.getWidth(); x++) {
			for (int y = 0; y < imp.getHeight(); y++) {
				if (imp.getStack().getVoxel(x, y, stackImage - 1) >= minThreshold) {
					imp.getStack().setVoxel(x, y, stackImage - 1, imageMax);
				} else {
					imp.getStack().setVoxel(x, y, stackImage - 1, 0.0);
				}
			}
		}
		// IJ.log("bin ");userCheck(impMax);
	}
	
	private static void invertIntensities (ImagePlus imp) {
		double value = Math.pow(2.0,imp.getBitDepth())-1;
		for (int z = 0; z < imp.getStackSize();z++) {
			for (int x = 0; x < imp.getWidth(); x++) {
				for (int y = 0; y < imp.getHeight(); y++) {
					imp.getStack().setVoxel(x,y,z,value - imp.getStack().getVoxel(x,y,z));
				}
			}			
		}
	}

}// end main class
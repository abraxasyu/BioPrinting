import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.io.*;

import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.process.ImageConverter;
import ij.plugin.Thresholder;
import ij.gui.GenericDialog;
import ij.plugin.filter.BackgroundSubtracter;


public class BioPrinting_ implements PlugIn {

	public void run(String arg){
		long temptime=System.currentTimeMillis();
		
		double[][] dataGrid=iniGrid();
		
		DirectoryChooser dc = new DirectoryChooser("Choose source directory...");
		String dcDirectory = dc.getDirectory();
		File imageFolder = new File(dcDirectory);
		File[] imageList = imageFolder.listFiles();
		
		Opener op = new Opener();
		ImageCalculator ic = new ImageCalculator();
		Thresholder th = new Thresholder();
		th.showLegacyDialog=false;
		BackgroundSubtracter bs = new BackgroundSubtracter();
		ImageStack stack;
		ImageConverter imc;
		ResultsTable rt;
		double valRID = 0.0;
		int progCount = 0;
		double progCountTot=532.0;
		
		//user variable parameters
		int brifilnum=10;
		int minhue=20;
		int maxhue=130;
		
		String curImageName="";
		ImagePlus curimp=null;
		int row=0, col=0;
		
		//GenericDialog
		GenericDialog gd = new GenericDialog("BioPrinting Image Analysis");
		String label="label";
		gd.addStringField("Label: ",label);
		//hue filter
		gd.setInsets(0,0,0);
		gd.addMessage("Hue Filter:");
		String plugpath=IJ.getDirectory("plugins");
		ImagePlus colorRange = new ImagePlus(plugpath+"\\BioPrinting\\ColorRange.png");
		gd.addImage(colorRange);
		//minhue
		gd.addSlider("Minimum Hue",0,255,minhue);
		//maxhue
		gd.addSlider("Maximum Hue",0,255,maxhue);
		//bgsub bool
		gd.setInsets(0,0,0);
		gd.addMessage("Background Fluorescence:");
		boolean bgsubt = true;
		gd.setInsets(0,20,0);
		gd.addCheckbox("Background Subtraction (Warning: Slow)",bgsubt);
		//brifilnum val
		gd.addSlider("Brightness Filter:",0,50,brifilnum);
		//output
		gd.setInsets(0,0,0);
		gd.addMessage("Output:");
		//saveimage bool
		boolean saveImg = true;
		gd.addCheckbox("Save Image",saveImg);
		//set oval roi
		boolean setROI = true;
		gd.addCheckbox("Measure only the pillar area (ROI)",setROI);
		//exclude outliers
		boolean exoutlier = true;
		gd.addCheckbox("Exclude Outliers",exoutlier);
		//measure/txt bool
		boolean measurebool = true;
		gd.addCheckbox("Measure & Save to .scn",measurebool);
		//timelog bool
		boolean timelog = false;
		gd.addCheckbox("Enable time log",timelog);
		gd.setInsets(0,0,0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		if (minhue>maxhue) return;
		label=gd.getNextString();
		minhue=(int)gd.getNextNumber();
		maxhue=(int)gd.getNextNumber();
		bgsubt=gd.getNextBoolean();
		brifilnum=(int)gd.getNextNumber();
		saveImg=gd.getNextBoolean();
		setROI=gd.getNextBoolean();
		exoutlier=gd.getNextBoolean();
		measurebool=gd.getNextBoolean();
		timelog=gd.getNextBoolean();
		String savePath="";
		if (saveImg || measurebool){
			savePath = dcDirectory+"/"+label+"/";
			File saveFolder = new File(savePath);
			saveFolder.mkdirs();
		}
		
		if (timelog){IJ.log("Initialize: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
		
		if (imageList!=null){
			for (File curImage: imageList){
				if (curImage.isFile() && curImage.getName().indexOf(".")!=-1 && curImage.getName().split("\\.")[1].equals("tiff")){
					if (timelog){IJ.log(progCount+"- LoopStart: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					
					//open & name parse 0.150 sec
					curImageName=curImage.getName().split("\\.")[0];
					col=Integer.parseInt(curImageName.split("_")[0])-1;
					row=Integer.parseInt(curImageName.split("_")[1])-1;
					curimp = op.openImage(dcDirectory,curImage.getName());
					
					if (timelog){IJ.log(progCount+"-open & name parse: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					
					if (brifilnum>0){
						//bri filter 0.010 sec
						IJ.setMinAndMax(curimp,brifilnum,255);
						
						if (timelog){IJ.log(progCount+"-bri filter: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					}
					
					if (bgsubt){
						//bgsubtraction 0.800 sec	
						bs.rollingBallBackground(curimp.getProcessor(), 50.0, false, false, false, false, false);						
						
						if (timelog){IJ.log(progCount+"-bg subt: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					}
					ImagePlus curimp_copy = new ImagePlus("curimp_copy",curimp.getProcessor());
					
					//Hue Split 0.275
					imc = new ImageConverter(curimp);
					imc.convertToHSB();
					stack = curimp.getStack();
					
					ImagePlus curimp_h = new ImagePlus("0",stack.getProcessor(1));
					curimp_h.getProcessor().setThreshold(minhue,maxhue,curimp_h.getProcessor().NO_LUT_UPDATE);
					th.applyThreshold(curimp_h,false);
					curimp_h.getProcessor().invert();
					ImagePlus curimp_s = new ImagePlus("1",stack.getProcessor(2));
					curimp_s.getProcessor().setThreshold(0,255,curimp_s.getProcessor().NO_LUT_UPDATE);
					th.applyThreshold(curimp_s,false);
					ImagePlus curimp_b = new ImagePlus("2",stack.getProcessor(3));
					curimp_b.getProcessor().setThreshold(0,255,curimp_b.getProcessor().NO_LUT_UPDATE);
					th.applyThreshold(curimp_b,false);
					ImagePlus impr0=ic.run("AND create", curimp_h,curimp_s);
					ImagePlus impr00=ic.run("AND create", impr0,curimp_b);
					ImagePlus impr000=ic.run("AND create",curimp_copy,impr00);
					
					curimp.close();
					impr00.close();
					impr0.close();
					curimp_s.close();
					curimp_b.close();
					//curimp_h.changes=false;
					curimp_h.close();
					curimp=new ImagePlus("newcurimp",impr000.getProcessor());
					impr000.close();
					
					if (timelog){IJ.log(progCount+"-hue split: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					
					if (saveImg){
						//saveImage 0.250 sec
						IJ.saveAsTiff(curimp,savePath+curImageName);
						
						if (timelog){IJ.log(progCount+"-save image: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					}
					if (setROI){
						curimp.setRoi(new OvalRoi(525,325,550,550));
					}
					if (measurebool){
						//Measure 0.017 sec
						IJ.run("Set Measurements...", "integrated redirect=None decimal=0");
						IJ.run(curimp,"Measure","");
						rt = ResultsTable.getResultsTable();
						valRID = rt.getValue("RawIntDen", 0);
						dataGrid[row][col]=valRID;
						IJ.run("Clear Results","");
						
						
						if (timelog){IJ.log(progCount+"-measure: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
					}
					
					//progess count 0.000 sec
					progCount++;
					IJ.showProgress(progCount/progCountTot);
					
					if (timelog){IJ.log(progCount+"-prog count: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
				}
			}
		}
		if (measurebool){
			if (exoutlier){
				dataGrid=cleanFile(dataGrid);
			}
			//write to file 0.006 sec
			writeFile(savePath,label,dataGrid);
			if (timelog){IJ.log(progCount+"-write file: "+Long.toString(System.currentTimeMillis()-temptime));temptime=System.currentTimeMillis();}
		}
		IJ.log("Complete");
	}
	
	private double[][] cleanFile(double[][] gridIn){
		double mean1=0,mean2=0,mean3=0,mean4=0,mean5=0,mean6=0;
		double sd1=0,sd2=0,sd3=0,sd4=0,sd5=0,sd6=0;
		for(int row=0;row<38;row++){
			for (int col=0;col<14;col++){
				if (row>=1 && row <=6){			mean1+=gridIn[col][row];}
				else if (row>=7 && row <=12){	mean2+=gridIn[col][row];}
				else if (row>=13 && row <=18){	mean3+=gridIn[col][row];}
				else if (row>=19 && row <=24){	mean4+=gridIn[col][row];}
				else if (row>=25 && row <=30){	mean5+=gridIn[col][row];}
				else if (row>=31 && row <=36){	mean6+=gridIn[col][row];}
			}
		}
		mean1=mean1/84.0;
		mean2=mean2/84.0;
		mean3=mean3/84.0;
		mean4=mean4/84.0;
		mean5=mean5/84.0;
		mean6=mean6/84.0;
		
		for(int row=0;row<38;row++){
			for (int col=0;col<14;col++){
				if (row>=1 && row <=6){			sd1+=Math.pow(gridIn[col][row]-mean1,2);}
				else if (row>=7 && row <=12){	sd2+=Math.pow(gridIn[col][row]-mean2,2);}
				else if (row>=13 && row <=18){	sd3+=Math.pow(gridIn[col][row]-mean3,2);}
				else if (row>=19 && row <=24){	sd4+=Math.pow(gridIn[col][row]-mean4,2);}
				else if (row>=25 && row <=30){	sd5+=Math.pow(gridIn[col][row]-mean5,2);}
				else if (row>=31 && row <=36){	sd6+=Math.pow(gridIn[col][row]-mean6,2);}
			}
		}
		sd1=Math.sqrt(sd1/84);
		sd2=Math.sqrt(sd2/84);
		sd3=Math.sqrt(sd3/84);
		sd4=Math.sqrt(sd4/84);
		sd5=Math.sqrt(sd5/84);
		sd6=Math.sqrt(sd6/84);
		
		for(int row=0;row<38;row++){
			for (int col=0;col<14;col++){
				if (row>=1 && row <=6){			if (Math.abs(gridIn[col][row]-mean1)>(2*sd1)){gridIn[col][row]=mean1;}}
				else if (row>=7 && row <=12){	if (Math.abs(gridIn[col][row]-mean2)>(2*sd2)){gridIn[col][row]=mean2;}}
				else if (row>=13 && row <=18){	if (Math.abs(gridIn[col][row]-mean3)>(2*sd3)){gridIn[col][row]=mean3;}}
				else if (row>=19 && row <=24){	if (Math.abs(gridIn[col][row]-mean4)>(2*sd4)){gridIn[col][row]=mean4;}}
				else if (row>=25 && row <=30){	if (Math.abs(gridIn[col][row]-mean5)>(2*sd5)){gridIn[col][row]=mean5;}}
				else if (row>=31 && row <=36){	if (Math.abs(gridIn[col][row]-mean6)>(2*sd6)){gridIn[col][row]=mean6;}}
			}
		}
		return gridIn;
	}
	
	private void writeFile(String savePath,String fileName,double[][] dataGrid){
		String dl = "\t";
		try{
			FileWriter tempFW = new FileWriter(savePath+fileName+".scn");
			for(int row=0;row<38;row++){
				for (int col=0;col<14;col++){
					tempFW.write(""+dataGrid[col][row]+dl);
				}
				tempFW.write("\n");
			}
			tempFW.close();
		}catch(IOException e){}
		try{
			int num;
			FileWriter tempFW = new FileWriter(savePath+fileName+".tsv");
			for(int row=1;row<37;row++){
				for (int col=0;col<14;col++){
					if (row>=31){num=6;}
					else if (row>=25){num=5;}
					else if (row>=19){num=4;}
					else if (row>=13){num=3;}
					else if (row>=7){num=2;}
					else{num=1;}
					tempFW.write(num+dl+dataGrid[col][row]+"\n");
				}
			}
			tempFW.close();
		}catch(IOException e){}
	}
	
	private double[][] iniGrid(){
		double[][] retGrid = new double[14][38];
		for(int row=0;row<14;row++){
			for (int col=0;col<38;col++){
				retGrid[row][col]=0;
			}
		}
		return retGrid;
	}
	
}
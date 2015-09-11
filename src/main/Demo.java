package main;

import grid.Configuration;
import grid.Grid;
import grid.GridLeafTraHashItem;
import grid.RoICell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map.Entry;


import loadData.BBFOldLoad;
import prediction.MacroState;
import prediction.Predictor;
import prediction.StateGridFilter;

import visulalization.VisGridCanvas;

public class Demo {


	public static void main(String args[]){
		
			run();

	}
		
	
//=================================method for R2-D2===================================================
	public static void run(){
		
		Predictor predictor=new Predictor();
		Configuration.BITS_PER_GRID=4;
		Configuration.MAX_LEVEL=3;

	
		Configuration.T_period=200;
		
		Configuration.BrinkConstraintRoI=16;
		
		//define parameter related with MacroState and MicroState 
		Configuration.MaxRadius=5000; //maximum size of state, not import parameters. It is defined on real distance
		Configuration.MaxStateDis=500;
		Configuration.AlphaRadius=1.5;//should be larger than 1
		Configuration.AlphaScore=1/16.0;
		Configuration.ProDown=0.2;
		Configuration.MAPPro=0.2;
		
		Configuration.MicroStateRadius=Configuration.cellRadius*2;
				
		int refTime=4;
		
		int DBBackStep=3;
		
		int sampleNum=1000;
		int sampleLen=10;
		
		int timeStart=0;
		int timeEnd=15;
		
		int sampleStart=0;
		int sampleEnd=15;

        BBFOldLoad tl=new BBFOldLoad(sampleNum, sampleLen,sampleStart,sampleEnd);
		System.out.println("#start loading data and sample queries from data/BigBrinkhoff/bigBrinkhoff.db tablename: BBFOldTest");
		Grid g=tl.Load2Grid("data/BigBrinkhoff/bigBrinkhoff.db", "BBFOldTest",timeStart ,timeEnd);
		System.out.println("#finished loading data");
		System.out.println("#show part of the map");
		new VisGridCanvas(g, 0, 0, Configuration.GRID_DIVIDED, Configuration.GRID_DIVIDED);//show a map
		
		HashMap<Integer, ArrayList<RoICell>> gridRes=tl.getGridSampleList();
		
		Iterator<Entry<Integer,ArrayList<RoICell>>> lItr=gridRes.entrySet().iterator();
		int[] avgCount=new int[sampleLen+refTime];
		double[] avgDT=new double[sampleLen+refTime];

		int stpCount=0;
		for(int j=0;j<sampleLen+refTime;j++){
			avgDT[j]=0;
			avgCount[j]=0;
		}
		
		double countSum=gridRes.size();
		int vlCount=0;
		
		System.out.println("#start prediction and statistic the prediction rate and prediction error");
		while(lItr.hasNext()){
			Entry<Integer,ArrayList<RoICell>> gridItem=lItr.next();
			ArrayList<RoICell> rcGridList=gridItem.getValue();
			ArrayList<RoICell> ref=new ArrayList<>(rcGridList.subList(refTime-DBBackStep+1, refTime+1));
            ArrayList<Entry<Long, GridLeafTraHashItem>>  testBres=g.queryRangeTimeSeqCells(ref);

			 if(null==testBres||testBres.size()<Configuration.TraSupport) continue;
			
			 StateGridFilter sgf=predictor.predictPath(testBres, g, Configuration.ProDown, Configuration.MAPPro, Configuration.MicroStateRadius);
			 ArrayList<MacroState> mp=sgf.gfStates.getMacroStatePath();

            int len=mp.size()-1;
				
				 for(int i=1;i<=len;i++){
					 
					 MacroState pItem=mp.get(i);
					 if(i+refTime>rcGridList.size()-1){
						 continue;
					 }
					 RoICell tItem=rcGridList.get(i+refTime);
					 double d_dt=pItem.getDisCenter(tItem.roiX, tItem.roiY);
					 
					 avgDT[i]+=d_dt;
				 avgCount[i]++;	 
			 }
		}
		
		
		
		System.out.println("#time	count	Rate	DT_error	");
		for(int i=1;i<avgDT.length;i++){
			System.out.println(i+"	"+avgCount[i]+"	"+avgCount[i]/(countSum-vlCount)+"	"+avgDT[i]/avgCount[i]*9+"	");//error is to multiple the cell width
		}
	
		System.out.print("#stp count:"+stpCount);
	}
	
	

		
}

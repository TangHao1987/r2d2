package grid;

import java.util.HashMap;

public class Configuration {
	//for time profiling
	public static double time_r2d2 = 0;//total time search+prediction
	public static double time_search=0;
	public static int time_search_count=0;
	public static double time_prediction = 0;//total time retrieve+cluster+hmm
	public static int time_prediction_count = 0;
	public static int time_retrieve = 0;
	public static double time_cluster = 0;
	public static int time_cluster_count = 0;
	public static double time_hmm = 0;
	public static int time_hmm_count = 0;
	//end for time profiling
	
	
	public static int BITS_PER_GRID=4;
	public static int MAX_LEVEL=3;
	public static int GridDivided=2048;//maximum number of grids at each axes
	
	public static int T_Sample=1;//the sample time interval for each update
	public static int T_period=1000;//the period of trajectories which we think they are not old
	public static double ExtremLowVelocityLat=0;
	public static double ExtremLowVelocityLng=0;
	
	public static int BrinkConstraintRoI=3; 
	public static double BrinkThreshold=0.1;
	//the support of reference trajectories. How many minimum trajectories are required 
	public static int TraSupport=5;
	
	public static double cellRadius=Math.sqrt(2)/2;//use to compute the micro state, each cell has a radius sqrt(2)/2
	public static double MicroStateRadius=4*cellRadius;
	
	public static int minNumPerMic=1;//the minimum number of tra id at each micro state, 
	
	

	
	//define parameter related with MacroState and MicroState 
	public static double ProDown=0.9;//stop to go down to next level of the macro states
	public static double MAPPro=0.0001;//terminate the MAP process
	public static int MaxRadius=40;
	public static double MaxStateDis=40;
	public static double AlphaRadius=2;//should be larger than 1
	
	public static double AlphaScore=0.5;
	
	//for continuous prediction
	public static boolean doSelfCorrection=false;
	public static HashMap<Integer,Integer> lifetimeMap=new HashMap<Integer,Integer>();//entry <traId,lifetime>
	public static int lossCount=0;//with debugging purpose
	public static int fullcount=0;//with debugging purpose
	public static double doSelfParameter=3;
	
	
	/**
	 * 
	 * @author workshop
	 * this class is the leaf,which store the trajectories information of the grid.
	 * note that:
	 *
	 * 
	 * 1. For 4k page, the size of 
	 * type				size	byte
	 * LinkedHashMap    135   4002
	 * TreeMap 			140	  4063
	 * 
	 * Noted that, the key is traId<<32+time, i.e. the traId and offset is
	 * the key of HashMap, while, the value of HashMap is the location in the grid of traId. The corresponding next time can be
	 * calculated by currrent timestamp + T_sample, where T_sample the fixed sample time interval 
	 */
	public static int CapacityPerPage=135000;
	public static int PageSize=4096000;
	public static String GridFile="GridDiskBuffer";
	
	
	public static int getTraId(Long key){
		int traid=(int)(key>>32);
		return traid;
	}
	
	public static int getTime(Long key){
		long id2=key>>32;
		id2<<=32;	
		long timeLong=key-id2;
		int time=(int)(timeLong);
		return time;
	}
	
	public static Long getKey(int traId,int time){
		Long key=new Long(traId);
		key<<=32;
		key+=time;
		return key;
	}
	
	public static int hitCount=0;
	
	
	private static int stateId=0;
	
	public static int getStateId(){
		return stateId++;
	}
	
	

	
	// BigBrinkhoffOldenburgTest BBFOld
	//public static int BBFOld_T_Sample=1;
	public static double BBFOldXMin=292.0;
	public static double BBFOldYMin=3935.0;
	public static double BBFOldXMax=23056.0;
	public static double BBFOldYMax=30851.0;
	public static double BBFOldUnitCell=18;
	public static int BBFOldTraNum=1005;

	
	
	
}

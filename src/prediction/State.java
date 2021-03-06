package prediction;

import grid.Configuration;
import grid.RoICell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class State {

	int id;//each micro state has one id
	
	public int n;//the number of points, (LC.size())
	//public double minBound;//the distance to farthest point
	public HashSet<RoICell> LC;//record all the cells( or points)
	//public ArrayList<Integer> LT;//record all the trajectory id//change it into a hashmap
	public HashMap<Integer,RoICell> LT;//traId and its corresponding cells
	
	public double SumDensity;//the sum of density of all cells
	public double LSX;//x-the sum the position of all cells
	public double LSY;//y-..
	public double SSX;//x-the square sum of the position of all cells
	public double SSY;//y-..
	
	
	public State(int inId){
	
		id=inId;
		
		LC=new HashSet<RoICell>();
		//LT=new ArrayList<Integer>();
		LT= new HashMap<Integer,RoICell>();
		
		n=0;		
		SumDensity=0;
		LSX=0;
		LSY=0;
		SSX=0;
		SSY=0;
	}
	
	public ArrayList<Integer> getLTArray(){

		Set<Integer> LTKeys=LT.keySet();
		
		ArrayList<Integer> res=new ArrayList<Integer>(LTKeys);
		return res;
	}
	
	public State(int inId,State ms1,State ms2){
		id=inId;
		
		LC=new HashSet<RoICell>();
		//LT=new ArrayList<Integer>();
		LT= new HashMap<Integer,RoICell>();
		
		LC.addAll(ms1.LC);
		LC.addAll(ms2.LC);
		//LT.addAll(ms1.LT);
		//LT.addAll(ms2.LT);
		mergeLT(LT,ms1.LT);
		mergeLT(LT,ms2.LT);
		
		n=ms1.n+ms2.n;
		
		//sum of position
		LSX=ms1.LSX+ms2.LSX;
		LSY=ms1.LSY+ms2.LSY;
		
		//sum of square position
		SSX=ms1.SSX+ms2.SSX;
		SSY=ms1.SSY+ms2.SSY;
		
		//sum of density
		SumDensity=ms1.SumDensity+ms2.SumDensity;
	}
	
	public State(int inId, State ms1){
		id=inId;
		
		LC=new HashSet<RoICell>();
		LT=new HashMap<Integer,RoICell>();
		LC.addAll(ms1.LC);
		//LT.addAll(ms1.LT);
		mergeLT(LT,ms1.LT);
		
		n=ms1.n;
		
		//sum of position
		LSX=ms1.LSX;
		LSY=ms1.LSY;
		
		//sum of square position
		SSX=ms1.SSX;
		SSY=ms1.SSY;
		
		//sum of density
		SumDensity=ms1.SumDensity;
	}
	
	/**
	 * get the current center of 
	 * @return
	 */
	public double[] getCenter(){
		double[] c=new double[2];
		if(!Configuration.doSelfCorrection){
		c[0]=LSX/n;
		c[1]=LSY/n;
		return c;
		}else{
			return  getCenterWithLifetime();
		
		}
		
	}
	
	public double getStateTraLifetime(){
		if(!Configuration.doSelfCorrection){
			return LT.size();
		}
		else{
			double ltSum=0;
			Set<Entry<Integer,RoICell>> LTSet=LT.entrySet();
            for (Entry<Integer, RoICell> LTSetItem : LTSet) {
                //Integer lt=null;
                Integer lt = Configuration.lifetimeMap.get(LTSetItem.getKey());//get the lifetime
                Configuration.fullcount++;//for debug
                if (null == lt) {//if empty, just 1
                    ltSum += 1;
                    Configuration.lossCount++;//for debug
                } else {

                    double ltWeight = Math.pow(Configuration.doSelfParameter, lt - 1);//Exponentially increase
                    //double ltWeight=lt;
                    ltSum += ltWeight;
                }
            }
			return ltSum;
		}
	}
	
	/**
	 * get center with lifetime
	 * @return
	 */
	public double[] getCenterWithLifetime(){
		
			double[] c=new double[2];
			double LSXlt=0;
			double LSYlt=0;
			double ltSum=0;
			
			Set<Entry<Integer,RoICell>> LTSet=LT.entrySet();
        for (Entry<Integer, RoICell> LTSetItem : LTSet) {
            //Integer lt=null;
            Integer lt = Configuration.lifetimeMap.get(LTSetItem.getKey());//get the lifetime
            Configuration.fullcount++;//for debug
            if (null == lt) {//if empty, just 1
                LSXlt += LTSetItem.getValue().roiX;
                LSYlt += LTSetItem.getValue().roiY;
                ltSum += 1;
                Configuration.lossCount++;//for debug
            } else {

                double ltWeight = Math.pow(Configuration.doSelfParameter, lt - 1);//Exponentially increase
                //double ltWeight=lt;
                LSXlt += LTSetItem.getValue().roiX * ltWeight;
                LSYlt += LTSetItem.getValue().roiY * ltWeight;
                ltSum += ltWeight;
            }
        }
			c[0]=LSXlt/ltSum;
			c[1]=LSYlt/ltSum;
			
			

			
			c[0]=LSXlt/ltSum;
			c[1]=LSYlt/ltSum;
			return c;
	
		
		
	}
	
	//Add Hashmap B into A
	private void mergeLT(HashMap<Integer,RoICell> LTA,HashMap<Integer,RoICell> LTB){
		if(null==LTB){
			return;
		}
		Set<Entry<Integer,RoICell>> LTBSet=LTB.entrySet();

        for (Entry<Integer, RoICell> LTBItem : LTBSet) {
            LTA.put(LTBItem.getKey(), LTBItem.getValue());
        }
	}
	
	/**
	 * the distance from center to a point
	 * @param txi
	 * @param tyi
	 * @return
	 */
	public double getDisCenter(double txi, double tyi) {
		// TODO Auto-generated method stub
		double c[]=getCenter();
		
		double p[]={txi,tyi};
		
		double d=Math.sqrt((c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]));
		
		return d;
	}
	
	/**
	 * the distance from center to a center of the other micro state
	 * @param ms
	 * @return
	 */
	public double getDisCenter(State ms){
		double d;
	
		double c[]=getCenter();
		double p[]=ms.getCenter();
		d=Math.sqrt((c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]));
		
		return d;
	}

	/**
	 * merge two state
	 * @param ms
	 */
	public void addState(State ms){
		//sum of position
		LSX+=ms.LSX;
		LSY+=ms.LSY;
		
		//sum of square position
		SSX+=ms.SSX;
		SSY+=ms.SSY;
		
		//sum of density
		SumDensity+=ms.SumDensity;
		
		LC.addAll(ms.LC);
		//LT.addAll(ms.LT);
		mergeLT(LT,ms.LT);
		
		n+=ms.n;
	}

	public int getSize(){
		return n;
	}
	
	
}

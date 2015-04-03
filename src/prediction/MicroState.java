package prediction;

import grid.Configuration;
import grid.Grid;
import grid.GridCell;
import grid.GridLeafTraHashItem;
import grid.RoICell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 
 * @author workshop
 *
 */
public class MicroState extends State{
//	int id;//each micro state has one id
//	
	
	//public HashMap<Integer,Entry<Long,GridLeafTraHashItem>> Ltra;//record all the information to retrieve the next location cells
	public HashMap<RoICell,ArrayList<Long>> Ltratime;
	public HashMap<Long,RoICell> LTraCell;//RoIcell is the next location of (traId+time); i.e. traId+time----> RoICell
	//	
//	public int n;//the number of points, (LC.size())
	public double minBound;//the distance to farthest point
//	
//	public double SumDensity;//the sum of density of all cells
//	public double LSX;//x-the sum the position of all cells
//	public double LSY;//y-..
//	public double SSX;//x-the square sum of the position of all cells
//	public double SSY;//y-..
	
	/**
	 * 
	 */
	public MicroState(int inId){
	
		super(inId);
		
		//Ltra=new HashMap<Integer,Entry<Long,GridLeafTraHashItem>>();
		Ltratime=new HashMap<RoICell,ArrayList<Long>>();
		LTraCell=new HashMap<Long,RoICell>();
		minBound=-1;
	}
	
	public MicroState(int inId,MicroState ms){
		super(inId,ms);
		
		//Ltra=new HashMap<Integer,Entry<Long,GridLeafTraHashItem>>();
		//Ltra.putAll(ms.Ltra);
		
		Ltratime=new HashMap<RoICell,ArrayList<Long>>();
		Ltratime.putAll(ms.Ltratime);
		
		LTraCell=new HashMap<Long,RoICell>();
		LTraCell.putAll(ms.LTraCell);
	
		minBound=ms.minBound;
	}
	
	public MicroState(int inId,MicroState ms1,MicroState ms2){
		super(inId,ms1,ms2);
		//Ltra=new HashMap<Integer,Entry<Long,GridLeafTraHashItem>>();
		//Ltra.putAll(ms1.Ltra);
		//Ltra.putAll(ms2.Ltra);
		
		Ltratime= new HashMap<RoICell, ArrayList<Long>>();
		Ltratime.putAll(ms1.Ltratime);//put a into res
		addLtratimeHash(Ltratime,ms2.Ltratime);
		
		LTraCell=new HashMap<Long,RoICell>();
		LTraCell.putAll(ms1.LTraCell);
		LTraCell.putAll(ms2.LTraCell);
		
		
		this.setMinBound();
	}
	
	/**
	 * merge ltt into b
	 * @param ltt
	 * @param b
	 */
	private void addLtratimeHash(
			HashMap<RoICell, ArrayList<Long>> ltt,
			HashMap<RoICell, ArrayList<Long>> b) {
		Iterator<Entry<RoICell, ArrayList<Long>>> bitr = b.entrySet()
				.iterator();//visit every element of b

		while (bitr.hasNext()) {
			Entry<RoICell, ArrayList<Long>> bItem = bitr.next();

			ArrayList<Long> resItem = ltt.get(bItem.getKey());
			if (null == resItem) {
				resItem = new ArrayList<Long>();//if empty, create a new one
				ltt.put(bItem.getKey(), resItem);
			}
			resItem.addAll(bItem.getValue());
		}
	}

	/**
	 * and <rc,e> into Ltratime hashmap
	 * @param ltt
	 * @param rc
	 * @param e
	 */
	private void addLtratimeHash(HashMap<RoICell, ArrayList<Long>> ltt,RoICell rc,Long e){
		ArrayList<Long> lttItem=ltt.get(rc);//get array of Long
		if(null==lttItem){//if null, creat new one and put it into hashmap
			lttItem=new ArrayList<Long>();
			ltt.put(rc, lttItem);
		}
		lttItem.add(e);
	}
	
	/**
	 * delete <rc,e> from Ltratime HashMap
	 * @param ltt
	 * @param rc
	 * @param e
	 */
	private void deleteLtratimeHash(HashMap<RoICell,ArrayList<Long>> ltt,RoICell rc,Long e){
		ArrayList<Long> res=ltt.get(rc);
		res.remove(e);
	}
	/**
	 * add one cell into the microstate
	 * @param x
	 * @param y
	 * @param den
	 */
	public void addPoint(int x,int y,double den,Entry<Long,GridLeafTraHashItem> e){
	
		addPoint( x, y,  den, e, true);
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param den
	 * @param e
	 * @param setBound
	 */
	public void addPoint(int x,int y, double den, Entry<Long,GridLeafTraHashItem>e, boolean setBound){
		//sum of position
		LSX+=x;
		LSY+=y;
		
		//sum of square
		SSX+=x*x;
		SSY+=y*y;
		
		//sum of density
		SumDensity+=den;
		
		//add to LC
		
		RoICell rc=new RoICell(x,y);
		LC.add(rc);
		if(null!=e){
		int key=Configuration.getTraId(e.getKey());
		//LT.add(key);
		LT.put(key, rc);
		
		addLtratimeHash(Ltratime,rc,e.getKey());
		
		LTraCell.put(e.getKey(),rc);
		}

		n+=1;//size ++
		if(setBound){
		setMinBound();
		}
	}
	
	/**
	 * delete a point from this micro state
	 * @param point
	 * @param g
	 * @param setBound: if true, compute the minimum bound again.
	 */
	public void deletePoint(Long point){
				
		RoICell rc=LTraCell.remove(point);//remove this traId, and get RoICell
		assert(rc!=null);
		
		deleteLtratimeHash(Ltratime,rc,point);//remove from Ltratime
		Integer traId=Configuration.getTraId(point);
		LT.remove(traId);//remove from LT
		LC.remove(rc);//remove from LC
		
		
		int x=rc.roiX;
		int y=rc.roiY;
		
		LSX-=x;
		LSY-=y;
		
		SSX-=x*x;
		SSY-=y*y;
		
		//GridCell gc=g.getGridCell(x, y);//process later, and once
		//double den=gc.density;
		//SumDensity-=den;
		
		n-=1;
		
		//if(setBound){//process latter,and once
		//	setMinBound();
		//}
		
		
	}
	
	/**
	 * merge two state
	 * @param ms
	 */
	public void addMicroState(MicroState ms){
		
		super.addState(ms);
		///LC.addAll(ms.LC);
		//LT.addAll(ms.LT);
		//Ltra.putAll(ms.Ltra);
		addLtratimeHash(Ltratime,ms.Ltratime);
		LTraCell.putAll(ms.LTraCell);
		//sum size
		//set minimum bound, if we add up state, this is not necessary
		setMinBound();
	}
	
	/**
	 * update the density of this state by one scan
	 * @param g
	 */
	public void updateDensity(Grid g){
		SumDensity=0;
		//visit every RoI and get the density
		 Iterator<RoICell> LTraCellVItr=LTraCell.values().iterator();
		 while(LTraCellVItr.hasNext()){
			 RoICell lcvItem=LTraCellVItr.next();
			 GridCell gc=g.getGridCell(lcvItem.roiX, lcvItem.roiY);
			 if(null!=gc){
			 SumDensity+=gc.density;
			 }
		 }
	}
	
	/**
	 * set the minimum bound for each micro state
	 */
	public void setMinBound(){
		//if there is only one cell, the radius is sqrt(2)/2
		if(LC.size()==1){
			minBound=Configuration.cellRadius;
			return ;
		}
		
		double c[]=getCenter();//get center
		double max=-1;
		double temp=0;
		
		//visit all element
		Iterator<RoICell> itr=LC.iterator();
		while(itr.hasNext()){
			RoICell rc=itr.next();
			double p[]={rc.roiX,rc.roiY};
			temp=(c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]);//distance
			
			if(temp>max){//find maximum distance
				max=temp;
			}
		}
		minBound=Math.sqrt(max);
	}
	
	/**
	 * 
	 * @return
	 */
	public String toLCString(){
		
		String str="";
		for(RoICell rc:LC){
			str+=rc.toString();
		}
		
		return str;
	}
	
	/**
	 * 
	 * @return: the number of points in this state
	 */
	public int getSize(){
		return n;
	}
	
}

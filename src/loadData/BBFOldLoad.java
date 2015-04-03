package loadData;

import grid.Configuration;
import grid.Grid;
import grid.MoveObjCacheBBFOld;

import grid.RoICell;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;




import stp.predictor.Point;

public class BBFOldLoad {
	double lat0 = 0;// the origin point of whole coordinate latitude
	double lng0 = 0;// the origin point of whole coordinate longitude
	double step = 0;// 4X4, four level of grid, approximate one meter width for
	
	// record the sample trajectory part to do experiment
	private HashMap<Integer, ArrayList<RoICell>> sampleGridHash;//in grid coordination
	private HashMap<Integer, ArrayList<Point>> sampleLocHash;//in location coordination
	
	private int sampleNum;
	private int sampleLen;
	private int[] sampleIdx;
	private int sampleStartTime=-1;
	private int sampleEndTime=-1;
	
	public BBFOldLoad() {
		//MOSet = new HashMap<Integer, MovingObject>();
		
		//IdMap=new HashMap<String,Integer>();

		// record the sample, for experiment
		sampleGridHash = null;
		sampleLocHash=null;
		sampleNum = -1;
		sampleLen = -1;
		sampleIdx = null;
		
		sampleStartTime=-1;
		sampleEndTime=-1;
		
	}
	
	
	public void setSample(int inSampleNum, int inSampleLen,
			int inSampleStartTime, int inSampleEndTime) {
		
		setSample(inSampleNum,inSampleLen);
		
		sampleStartTime=inSampleStartTime;
		sampleEndTime=inSampleEndTime;
	
	}
	
	public HashMap<Integer, ArrayList<RoICell>> getGridSampleList() {
		return this.sampleGridHash;
	}
 
	public HashMap<Integer,ArrayList<Point>> getLocSampleList(){
		return this.sampleLocHash;
	}
	
	
	/**
	 * 
	 * @param inSampleNum
	 * @param inSampleLen
	 */
	public void setSample(int inSampleNum, int inSampleLen) {

		sampleGridHash = new HashMap<Integer, ArrayList<RoICell>>();
		sampleLocHash =new HashMap<Integer,ArrayList<Point>>();

		sampleNum = inSampleNum;
		sampleLen = inSampleLen;

		// for(int i=0;i<sampleNum;i++){
		// sampleList.add(new ArrayList<RoICell>());
		// }
	}
	
	
	
	/**
	 * 
	 * @param minLat
	 * @param minLng
	 * @param maxLat
	 * @param maxLng
	 */
	private void CalibrationParameter(double minLat, double minLng,
			double maxLat, double maxLng) {

		lat0 = minLat;
		lng0 = minLng;

		double xScale = maxLat - minLat;
		double yScale = maxLng - minLng;

		double maxScale = (xScale > yScale) ? xScale : yScale;
		int divided = Configuration.GridDivided;
		
		assert maxScale>Configuration.GridDivided:"maxScale>Configuration.GridDivided is false";
		step = maxScale / divided;
	}
	
	
	/**
	 * 
	 * @param maxNumTras
	 */
	private void setGridSampleIds(String db,String table,int timeStart,int timeEnd) {
		
		if (sampleLen != -1) {
			int [] idse=SQLiteDriver.getMITTraStartEndId(db, table, timeStart, timeEnd);
			//Iterator<Entry<Integer, MovingObject>> moItr = MOSet.entrySet()
			//		.iterator();
			int startId=idse[0];
			int endId=idse[1];
			
			int resIdx = 0;
			sampleIdx = new int[sampleNum];
			int count = startId;
			
			assert (endId-startId)>sampleNum:"!((endId-startId)>sampleNum)";
			
			int interval = (endId-startId)/ sampleNum;
			
			while(count<endId){
				if (count % interval == 1 && resIdx < sampleIdx.length) {
					sampleIdx[resIdx] = count;
					resIdx++;
				}
				count++;
			}
		}
	}
	
	
	/**
	  * 
	  * @param db
	  * @param table
	  * @param startTime
	  * @param endTime
	  * @return
	  */
	private Grid BBFOldTraLoad2Grid(String db, String table, int startTime,
			int endTime) {

		Grid g = new Grid();

		MoveObjCacheBBFOld moc = new MoveObjCacheBBFOld(g);

		if (this.sampleNum > 0) {
			
			if(sampleStartTime==-1||sampleEndTime==-1){
				sampleStartTime=startTime;
				sampleEndTime=endTime;
			}
			
			int ssInt=sampleStartTime;
			int seInt=sampleEndTime;
			
			moc.setSample(this.sampleIdx, sampleLen,ssInt,seInt);
			
		}

		SQLiteDriver.openDB(db);
	
		SQLiteDriver.loadBBFOld(table, startTime, endTime);
		

		try {
			while (SQLiteDriver.rs.next()) {
				MovingObject mo = this.ParseMovingObject(SQLiteDriver.rs);
				//mo.tranferToGrid();
				//if(isOutlier(mo.lat,mo.lng)) continue;
				//MOSet.put(mo.id,mo);
	      		//g.updatePoint(mo.gridX,mo.gridY, mo.timeStamp,mo.id,mo.sequence);
	    		moc.update(mo.id, mo.x, mo.y, mo.t);

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

  	if(null!=this.sampleGridHash){
  		this.prosSampleResList(moc.getGridSampleHashList(),moc.getLocSampleHashList());
  	}
		SQLiteDriver.closeDB();

		return g;
	}
	

	private void prosSampleResList(HashMap<Integer, ArrayList<RoICell>> sgh,HashMap<Integer, ArrayList<Point>> loc) {
		Iterator<Entry<Integer, ArrayList<RoICell>>> shItr = sgh.entrySet()
				.iterator();
		while (shItr.hasNext()) {
			Entry<Integer, ArrayList<RoICell>> item = shItr.next();
			if (item.getValue().size() >= this.sampleLen) {
				this.sampleGridHash.put(item.getKey(),item.getValue());
			}
		}
	
		Iterator<Entry<Integer, ArrayList<Point>>> locItr = loc.entrySet()
		.iterator();
		while(locItr.hasNext()){
			Entry<Integer, ArrayList<Point>> item = locItr.next();
			if(item.getValue().size()>=this.sampleLen){
				this.sampleLocHash.put(item.getKey(), item.getValue());
			}
		}
	}
	
	
	 /**
	  * 
	  * @param db
	  * @param table
	  * @param timeStart
	  * @param timeEnd
	  * @return
	  */
	 public Grid Load2Grid(String db,String table,int timeStart,int timeEnd){
		 
		 CalibrationParameter(Configuration.BBFOldXMin,Configuration.BBFOldYMin,Configuration.BBFOldXMax,Configuration.BBFOldYMax);
		 
		 setGridSampleIds( db, table, timeStart, timeEnd);
		 
		// MOSet.clear();

		 MapLoc2Grid.setParameter(this.lat0, this.lng0, this.step);
		 
		 Grid g=BBFOldTraLoad2Grid( db, table, timeStart, timeEnd);
		 
		 return g; 
	 }
	 
	 
	 private class MovingObject {
			String type="";
			//  nextX INTEGER, nextY INTEGER)

			int id = -1;
			int seq = -1;
			int objClass=-1;
			//int classId = -1;
			int t = -1;
			double x = -1;
			double y = -1;
			double speed=0;
			double nextX=-1;
			double nextY=-1;

			int gridX = -1;
			int gridY = -1;
		}
	 
	 /**
	  * parsing moving object from string
	  * @param res
	  * @return
	  */
	 private MovingObject ParseMovingObject(ResultSet sqlRs){
		 MovingObject mo=new MovingObject();
		 try{
		 mo.t=sqlRs.getInt("t");
		 mo.id=sqlRs.getInt("id");

		 mo.x=sqlRs.getInt("x");
		 mo.y=sqlRs.getInt("y");
		 mo.seq=sqlRs.getInt("seq");
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		 return mo;
	 }
}

package loadData;

import grid.Configuration;
import grid.Grid;
import grid.MoveObjCacheBBFOld;

import grid.RoICell;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class BBFOldLoad {
	double step = 0;// 4X4, four level of grid, approximate one meter width for

	private HashMap<Integer, ArrayList<RoICell>> sampleGridHash;//in grid coordination
	private int[] sampleIdx;

    private int sampleNum;
    private int sampleLen;
	private int sampleStartTime;
	private int sampleEndTime;

    private boolean initialized = false;

	
	public BBFOldLoad(int inSampleNum, int inSampleLen,
                      int inSampleStartTime, int inSampleEndTime) {
		sampleGridHash = new HashMap<>();
        sampleNum = inSampleNum;
        sampleLen = inSampleLen;
        sampleStartTime=inSampleStartTime;
        sampleEndTime=inSampleEndTime;
        initialized = true;
    }
	
	public HashMap<Integer, ArrayList<RoICell>> getGridSampleList() {
		return this.sampleGridHash;
	}

	private void calibrateParams() {
		double xScale = Configuration.MAX_LATITUDE - Configuration.MIN_LATITUDE;
		double yScale = Configuration.MAX_LONGITUDE - Configuration.MIN_LONGITUDE;
		double maxScale = (xScale > yScale) ? xScale : yScale;
		int divided = Configuration.GRID_DIVIDED;
        step = maxScale / divided;
	}

	private void setGridSampleIds(String db,String table,int timeStart,int timeEnd) {
		if (initialized) {
			int [] idse = SQLiteDriver.getMITTraStartEndId(db, table, timeStart, timeEnd);

			int startId=idse[0];
			int endId=idse[1];
			int resIdx = 0;

			sampleIdx = new int[sampleNum];

			int count = startId;
			assert (endId-startId)> sampleNum:"!((endId-startId)>sampleNum)";
			int interval = (endId-startId)/ sampleNum;
			
			while(count < endId){
				if (count % interval == 1 && resIdx < sampleIdx.length) {
					sampleIdx[resIdx] = count;
					resIdx++;
				}
				count++;
			}
		}
	}

	private Grid createGrid(String db, String table, int startTime,
                            int endTime) {

		Grid g = new Grid();

		MoveObjCacheBBFOld moc = new MoveObjCacheBBFOld(g);

		if (this.sampleNum > 0){
			moc.setSample(this.sampleIdx, sampleLen,sampleStartTime,sampleEndTime);
		}

		SQLiteDriver.openDB(db);
		SQLiteDriver.loadBBFOld(table, startTime, endTime);
		

		try {
			while (SQLiteDriver.rs.next()) {
				MovingObject mo = this.ParseMovingObject(SQLiteDriver.rs);
	    		moc.update(mo.id, mo.x, mo.y, mo.t);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

  	if(null!=this.sampleGridHash){
  		this.prosSampleResList(moc.getGridSampleHashList());
  	}
		SQLiteDriver.closeDB();

		return g;
	}
	

	private void prosSampleResList(HashMap<Integer, ArrayList<RoICell>> sgh) {
        for (Entry<Integer, ArrayList<RoICell>> item : sgh.entrySet()) {
            if (item.getValue().size() >= this.sampleLen) {
                this.sampleGridHash.put(item.getKey(), item.getValue());
            }
        }
	}

	 public Grid Load2Grid(String db,String table,int timeStart,int timeEnd){
		 
		 calibrateParams();
		 setGridSampleIds( db, table, timeStart, timeEnd);
		 MapLoc2Grid.setParameter(Configuration.MIN_LATITUDE, Configuration.MIN_LONGITUDE, step);
         return createGrid(db, table, timeStart, timeEnd);
	 }
	 
	 
	 private class MovingObject {
			private int id = -1;
			int seq = -1;
			int t = -1;
			double x = -1;
			double y = -1;
		}
	 
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

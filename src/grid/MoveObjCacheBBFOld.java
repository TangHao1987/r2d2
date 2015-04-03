package grid;

import java.util.*;

import stp.predictor.Point;

import loadData.MapLoc2Grid;
public class MoveObjCacheBBFOld {
	
	HashMap<Integer,MOTuple> moc=null;
	
	HashMap<Integer,ArrayList<RoICell>> sampleGridHashList=null;
	HashMap<Integer,ArrayList<Point>> sampleLocHashList=null;
	int sampleLen=-1;

	Grid grid=null;
	
	int sampleStartTime=0;
	int sampleEndTime=0;

	/**
	 * T_u is the update frequency. By default, it can be one.
	 * @param inTu
	 */
	public MoveObjCacheBBFOld(Grid inGrid){
		moc=new HashMap<Integer,MOTuple>();

		grid=inGrid;
		

		 sampleStartTime=-1;
		 sampleEndTime=-1;
	}
	
	public void setSample(int [] sa,int sl,int inStartTime,int inEndTime){
		sampleLen=sl;
		sampleGridHashList=new HashMap<Integer,ArrayList<RoICell>>();
		sampleLocHashList=new HashMap<Integer,ArrayList<Point>>();
		for(int i:sa){
			ArrayList<RoICell> item=new ArrayList<RoICell>();
			sampleGridHashList.put(i, item);
			
			ArrayList<Point> locItem=new ArrayList<Point>();
			sampleLocHashList.put(i, locItem);
		}
		
		sampleStartTime=inStartTime;
		sampleEndTime=inEndTime;
	}
	
	public void setSample(int [] sa,int sl){
		sampleLen=sl;
		sampleGridHashList=new HashMap<Integer,ArrayList<RoICell>>();
		sampleLocHashList=new HashMap<Integer,ArrayList<Point>>();
		for(int i:sa){
			ArrayList<RoICell> item=new ArrayList<RoICell>();
			sampleGridHashList.put(i, item);
			
			ArrayList<Point> locItem=new ArrayList<Point>();
			sampleLocHashList.put(i, locItem);
		}
		
		sampleStartTime=-1;
		sampleEndTime=-1;
	}
	
	
	
		
	public void insertSampleGrid(java.awt.Point p1,int traId){
		if(null==sampleGridHashList) return;
	
		ArrayList<RoICell> sitem=sampleGridHashList.get(traId);
	
		if(null!=sitem){
			if(sitem.size()<this.sampleLen){
			sitem.add(new RoICell(p1.x,p1.y));
			
			}
		}
	}
	
	public void insertSampleLoc(Point loc1, int traId){
		if(null==sampleLocHashList) return;
		ArrayList<Point> locItem=sampleLocHashList.get(traId);
		if(null!=locItem){
			if(locItem.size()<this.sampleLen){
				locItem.add(new Point(loc1.x,loc1.y));
			}
		}
	}
	
	
	
	public void update(int inTraId,double inLat,double inLng,int inTimestamp){
		MOTuple moTuple=moc.get(inTraId);
		
		
		
		if(moTuple==null){//if this is the first update of this moving object, store it in cache
			MOTuple tuple=new MOTuple(inLat,inLng,inTimestamp);
			moc.put(inTraId,tuple);
		} else {
			int ti=inTimestamp-moTuple.getTimestamp();
		
			
			if(ti<Configuration.T_Sample) return;//ignore this update
			else{
				double lat2=0,lng2=0,lat1=0,lng1=0;
				int t2=0,t1=0;
				
				lat1=moTuple.getLat();
				lng1=moTuple.getLng();
				t1=moTuple.getTimestamp();
				
				double voc_lat=(inLat-lat1)/ti;
				double voc_lng=(inLng-lng1)/ti;
				
				//outlier, ignore this update
			//	if(Math.abs(voc_lat)>Configuration.TaxiExtremVelocityLat||Math.abs(voc_lng)>Configuration.TaxiExtremVelocityLng){
			//		return;
			//	}
				
				int ts=Configuration.T_Sample;//count the time
				
				while(ts<=ti){
								
				 lat2=voc_lat*Configuration.T_Sample+lat1;
				 lng2=voc_lng*Configuration.T_Sample+lng1;
				 t2=Configuration.T_Sample+t1;

				//transfer to the coordination in x-y grid map
				java.awt.Point p2=MapLoc2Grid.transferGrid(lat2,lng2);
				java.awt.Point p1=MapLoc2Grid.transferGrid(lat1, lng1);
				

				
				grid.updateLineTra(p1.x,p1.y,t1, p2.x, p2.y, t2, inTraId);//update the line to grid
				
				if(this.sampleStartTime==-1||this.sampleEndTime==-1||(t1>=sampleStartTime&&t1<=sampleEndTime)){
				insertSampleGrid( p1, inTraId);
				
				Point loc1=new Point(inLat,inLng);
				insertSampleLoc(loc1,inTraId);
				}
				
				
				 lat1=lat2;
				 lng1=lng2;
				 t1=t2;
				 ts+=Configuration.T_Sample;
				}
				
				moTuple.setNewTuple(lat2,lng2, t2);//update the cache 
			}
		}
		
	}
	public HashMap<Integer,ArrayList<RoICell>> getGridSampleHashList(){
		return this.sampleGridHashList;
	}
	
	public HashMap<Integer,ArrayList<Point>> getLocSampleHashList(){
		return this.sampleLocHashList;
	}
	
	/**
	 * tuple in the cache structure
	 * @author workshop
	 *
	 */
	private class MOTuple{
		int timestamp;
		double lat;
		double lng;
		
		public MOTuple(double inLat,double inLng,int inTimestamp){
			lat=inLat;
			lng=inLng;
			timestamp=inTimestamp;
		}
		
		public void setNewTuple(double inLat,double inLng,int inTimestamp){
			lat=inLat;
			lng=inLng;
			timestamp=inTimestamp;
		}
		
		public int getTimestamp(){return timestamp;}
		public double getLat(){return lat;}
		public double getLng() {return lng;}
	}
}

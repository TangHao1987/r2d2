package query;

import grid.RoICell;

import java.awt.Point;
import java.util.ArrayList;

import traStore.TraStoreListItem;

/**
 * this class is used to process the input trajectories, and divided the input recent trajectory into 
 * equal interval reference point
 * @author workshop
 *
 */



public class RecentTraProcessor {

	private double lat0;
	private double lng0;
	private double step;
	
		public RecentTraProcessor(double inLat0,
				double inLng0,double inStep){
		lat0=inLat0;
		lng0=inLng0;
		step=inStep;
	}
	
	/**for recentTra, the time from 0 to n is from less to larger, 
	 * i.e. the larger index, the larger time stamp
	 * 
	 * @param recentTra
	 * @param divided divided provide the information that processor will divide the recent trajectory into several
	 * equal parts
	 */
	public ArrayList<Point> proRecentTra(ArrayList<TraStoreListItem> recentTra,int divided){
		
		if(null==recentTra) return null;
		
		ArrayList<Point> queryCells=new ArrayList<Point>();
		
		int startTime=recentTra.get(0).timestamp;
		int endTime=recentTra.get(recentTra.size()-1).timestamp;
		
		int duration=endTime-startTime;
		
		int interval=duration/divided;
		
		int timeSum=0;
		
		int dividedCount=divided;
		
		for(int i=0;i<=divided;i++){
			queryCells.add(null);
		}
		
		TraStoreListItem lastItem=recentTra.get(recentTra.size()-1);
		
		Point s=this.transferToGrid(lastItem.lat,lastItem.lng);
		
		queryCells.set(dividedCount, s);
		
		for(int i=recentTra.size()-2;i>=0;i--){
			int temp=recentTra.get(i+1).timestamp-recentTra.get(i).timestamp;
			timeSum+=temp;
			while(timeSum>=interval){
				timeSum=timeSum-interval;
				//xi=x1+vt=x1+(x2-x1)/t*(timeSum-interval)
				double lati=recentTra.get(i).lat+((recentTra.get(i+1).lat-recentTra.get(i).lat)*timeSum)/temp;
				double lngi=recentTra.get(i).lng+((recentTra.get(i+1).lng-recentTra.get(i).lng)*timeSum)/temp;
				
				Point d=transferToGrid(lati,lngi);
				
				dividedCount--;
				if(dividedCount<0){
					break;
				}
				queryCells.set(dividedCount, d);
			}
			//if(timeCount>interval)
		}
	
		return queryCells;
		
	}
	
	
	/**
	 * transfer a location in coordination system into the grid map.
	 * @param lat
	 * @param lng
	 * @return
	 */
	private Point transferToGrid(double lat, double lng) {
		double offx = lat - lat0;
		double offy = lng - lng0;

		int gridX = (int) (offx / step);
		int gridY = (int) (offy / step);

		return new Point(gridX, gridY);
	}

}

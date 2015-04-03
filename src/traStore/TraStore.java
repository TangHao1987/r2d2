package traStore;

import grid.TraListItem;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

public class TraStore {
	public HashMap<Integer, ArrayList<TraStoreListItem>> traHash = null;

	public TraStore() {
		traHash = new HashMap<Integer, ArrayList<TraStoreListItem>>();
	}

	public void appendTra(int traId,double lat,double lng,int time){
		ArrayList<TraStoreListItem> item=traHash.get(traId);//get the trajectory with traId
		
		if(item==null){
			item=new ArrayList<TraStoreListItem> ();
			item.clear();
			traHash.put(traId, item);
		}
		
		item.add(new TraStoreListItem(lat,lng,time));
	}
	
	/**
	 * query the trajectory by trajectory id, trajectory off and future trajectory off
	 * @param traId
	 * @param off
	 * @param deltaOff
	 * @return
	 */
	public ArrayList<TraStoreListItem> queryTraByOff(TraListItem traInfo, int deltaOff){
		
		ArrayList<TraStoreListItem> resTraList=new ArrayList<TraStoreListItem>();
		
		ArrayList<TraStoreListItem> tra=traHash.get(traInfo.traId);
		
		int len=traInfo.off+deltaOff;
		if(len>tra.size()) len=tra.size();
		
		for(int i=traInfo.off;i<len;i++){
			resTraList.add(tra.get(i));
		}
		
		return resTraList;
	}
	
	public ArrayList<TraStoreListItem> queryTraByIdOff(int traId, int off,int delta){
		
		ArrayList<TraStoreListItem> tra=traHash.get(traId);
		
		ArrayList<TraStoreListItem> res=new ArrayList<TraStoreListItem>();
		
		for(int i=off;i<off+delta&&i<tra.size();i++){
			res.add(tra.get(i));
		}
		
		return res;
	}
	
	/**
	 * special noted: the traInfo do not provide the useful information for future prediction, 
	 * therefore, we do not 
	 * include this offset into reference trajectories parts
	 * @param traInfo
	 * @param deltaTime
	 * @return
	 */
	public ArrayList<TraStoreListItem> queryTraByTime(TraListItem traInfo, int deltaTime){
		ArrayList<TraStoreListItem> resTraList=new ArrayList<TraStoreListItem>();
		ArrayList<TraStoreListItem> tra=traHash.get(traInfo.traId);
		
		int stopTime=traInfo.timestamp+deltaTime;
		
		int curTime=traInfo.timestamp;
		
		//special noted: the traInfo do not provide the useful information for future prediction, therefore, we do not 
		//include this offset into reference trajectories parts
		for(int i=traInfo.off+1;i<tra.size()&&curTime<=stopTime;i++){
			TraStoreListItem tsi=tra.get(i);
			
			curTime=tsi.timestamp;
			
			if(curTime>stopTime){
				//using linear interpolation to get middle virtual point
				int k=i-1;
				if(k<0) break;
				
				TraStoreListItem preTsi=tra.get(k);//get the next point, and user linear interpolation to get a virtual point
				
				double frac=((double)(stopTime-preTsi.timestamp))/((double)(curTime-preTsi.timestamp));//the intepolation parameter
				
				double midLat=(tsi.lat-preTsi.lat)*frac+preTsi.lat;
				double midLng=(tsi.lng-preTsi.lng)*frac+preTsi.lng;
				int midTime=stopTime;//(tsi.timestamp+preTsi.timestamp)/2;
				
				TraStoreListItem midTsi=new TraStoreListItem(midLat,midLng,midTime);
				resTraList.add(midTsi);
				break;
			}
			resTraList.add(tsi);
		}
		
		return resTraList;
	}
	
	
	public int getTraNum(){
		
		return traHash.size();
		
	}
	
	public int getLengthOfTra(int traId){
		
		return traHash.get(traId).size();
		
	}

}

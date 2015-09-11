package grid;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import storagemanager.DiskStorageManager;
import storagemanager.PropertySet;
import storagemanager.IStorageManager;
import utl.serialization.BinarySerializer;

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
 * 
 * Note that, for a query store pair <long, GridLeafTraHashItem>, Long store the traId and current time stamp,while GridLeafTraHashItem store
 * the next location 
 */
public class GridLeafEntry  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 761235399918911071L;
	static int PAGE_ID_CUR=-10;
	static int PAGE_ID_NIL=-1;
	
	//ArrayList<GridLeafTraListItem> traList=null;
	
	//the hashmap to record the trajectory, which is resident in memory. when the size of 
	//this map is larger than Configuration.capacityPerPage, we will write the whole map into a disk page
	LinkedHashMap<Long,GridLeafTraHashItem> traHash=null;
	
	//the reference of diskstoragemanager
	IStorageManager m_pStorageManager;

	//record the start time of each disk pages
	private ArrayList<PageStartTimeItem> pageStartTimes;
	
	//record the start time of current data(in memory)
	int curPageStartTime;
	
	//current update time
	int curTime;
	
	//count the total IO of this leaf
	int countIO;

	public GridLeafEntry(IStorageManager sm){
		
		m_pStorageManager=sm;
		
		//traList=new ArrayList<GridLeafTraListItem>();
		traHash=new LinkedHashMap<Long,GridLeafTraHashItem>();
		
		pageStartTimes=new ArrayList<PageStartTimeItem>();
		
		curPageStartTime=-1;
		
		curTime=-1;
		
		countIO=0;
	}
	
	public int getSizeOf(){
		int s=-1;
		try {
			byte[] traHashByte;
			traHashByte = BinarySerializer.getByteSerialize(traHash);
			
			int traHashSizeof=traHashByte.length;
			
			byte[] pageStartTimesByte=BinarySerializer.getByteSerialize(pageStartTimes);
			
			int pageStartTimesSizeof=pageStartTimesByte.length;
			
			int otherSize=4+4+4+8;
			
			s=traHashSizeof+pageStartTimesSizeof+otherSize;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
		
	}

	/**
	 * if there are updates, update the linkedhashmap, if linkedhashmap is too large, flush it into disk
	 * t1 is the update time of traId, and cellx2 and celly2 is the next cells locaiton of traId after current update
	 * @param traId
	 * @param t1
	 * @param cellx2
	 * @param celly2
	 */
	public void append(int traId,int t1,int cellx2,int celly2){
		
		curTime=t1;
		GridLeafTraHashItem hashItem=new GridLeafTraHashItem(cellx2,celly2);
		Long key=Configuration.getKey(traId, t1);
		traHash.put(key, hashItem);
		
		if(traHash.size()==1){
			curPageStartTime=t1;
		}
		
		//after append, check the hashmap size. all the flush and remove are issued by insert
		flushBuffer();
	}
	
	
	
	/**
	 * query recent trajectories point start or larter than forwardTiemStamp
	 * this is down(towards old data) query
	 * @return
	 */
	public ArrayList<Entry<Long,GridLeafTraHashItem>> queryTimeRangeForward(int forwardTimeStamp){
		//store the result
		ArrayList<Entry<Long,GridLeafTraHashItem>> res
				=new ArrayList<Entry<Long,GridLeafTraHashItem>>();
		
		//all traId which are update after queryTime are included

        //if queryTime is smaller than pageQueryStartTime, we need to read one more page
		int pageQueryStartTime=this.curPageStartTime;
		
		//query result in traHash
		queryTimeRangePerTraHash(traHash, forwardTimeStamp,res);
		
		try{
			
		LinkedHashMap<Long,GridLeafTraHashItem> pageHashTra=null;
		//the most recent page is in the tail of pageStartTimes
		int idx=pageStartTimes.size()-1;
		
		//if(idx<0) return res;//
		
		//queryTime<pageQueryStartTime: if queryTime is smaller than pageQueryStartTime, we need to read one more page
		// idx>=0: there is no pages in disk, return immediately
		while(forwardTimeStamp <pageQueryStartTime&&idx>=0){
			//get item
			PageStartTimeItem pstItem=pageStartTimes.get(idx);
			 assert( pstItem!=null ) :"GridLeafEntry: wrong pageStartTimes ";
			idx--;
			
			pageQueryStartTime=pstItem.start_time;//update time
			pageHashTra=this.loadPageHashTra(pstItem.pageId);//read the hashmap from disk 
			
			 assert( pageHashTra!=null ) :"GridLeafEntry: load traHash from page "+pstItem.pageId+" failed";
			
			
			//query result in pageHashTra
			queryTimeRangePerTraHash(pageHashTra, forwardTimeStamp,res);

		};
		}catch(Exception e){
			e.printStackTrace();
		}

		return res;
	}
	
	/**
	 * query the next cell of traId and time in current cell
	 */
	public GridLeafTraHashItem queryTraIdTime(int inTraId,int inTraTime){
		
		if(inTraTime>=this.curPageStartTime){//in case, we do not need to load disk page
			return this.queryTraIdTimePerTraHash(traHash, inTraId, inTraTime);
		} else{
			int pageId=getPageIdByTime(inTraTime);//get the corresponding page id
			if(PAGE_ID_NIL==pageId) return null;
	
			LinkedHashMap<Long,GridLeafTraHashItem> pageTraHash=loadPageHashTra(pageId);//load the hashmap from disk page
			return this.queryTraIdTimePerTraHash(pageTraHash, inTraId, inTraTime);//get query result
		}
		
	}
	
	/**
	 * query a set of tra from this single cell
	 * 
	 * @param inTraSet
	 * @return
	 */
	public ArrayList<Entry<Long, GridLeafTraHashItem>> queryTraSet(
			ArrayList<Long> inTraSet) {
		// store result
		HashMap<Long, GridLeafTraHashItem> resHash = new HashMap<Long, GridLeafTraHashItem>();
		// collect the entry according their page id
		HashMap<Integer, ArrayList<Long>> collect = new HashMap<Integer, ArrayList<Long>>();
		if (null == inTraSet)
			return null;
		
		//divide the tra by their page id
		for (Long traItem : inTraSet) {
			int timeItem = Configuration.getTime(traItem)
					+ Configuration.T_Sample;//time + T_Smaple to get next time
			int itemPageId = getPageIdByTime(timeItem);//get page id, if page id is PAGE_ID_CUR, it is stored in current tra hash
			
			//put this tra into collect hashmap
			ArrayList<Long> itemLongArray = collect.get(itemPageId);
			if (null == itemLongArray) {
				itemLongArray = new ArrayList<Long>();
				collect.put(itemPageId, itemLongArray);
			}
			itemLongArray.add(traItem);// put tra_id in the same page together
		}

		Iterator<Entry<Integer, ArrayList<Long>>> itr = collect.entrySet()
				.iterator();

		while (itr.hasNext()) {
			Entry<Integer, ArrayList<Long>> pageItem = itr.next();
			LinkedHashMap<Long, GridLeafTraHashItem> pageTraHash;
			
			if(PAGE_ID_CUR==pageItem.getKey()){//return current traHash
				pageTraHash=traHash;
			}else if(PAGE_ID_NIL==pageItem.getKey()){
				pageTraHash=null;
			}else{
				pageTraHash= loadPageHashTra(pageItem.getKey());//load from disk
			}
			
			//visit every item in this pageTraHash
			if(pageTraHash!=null){//if trahash cannot be found, ignore it 
			for (Long key : pageItem.getValue()) {
				int keyTraId = Configuration.getTraId(key);//id
				int keyTime = Configuration.getTime(key)
						+ Configuration.T_Sample;//cur+T_Sample
				GridLeafTraHashItem glItem = queryTraIdTimePerTraHash(
						pageTraHash, keyTraId, keyTime);//get item
				if(null!=glItem){//the end of trajectory, do not get result
				Long newKey = Configuration.getKey(keyTraId, keyTime);

				resHash.put(newKey, glItem);
				}
			}
		}
		}
		
		ArrayList<Entry<Long, GridLeafTraHashItem>> res = new ArrayList<Entry<Long, GridLeafTraHashItem>>(
				resHash.entrySet());
		return res;
	}
	
	/**
	 * query each traHash by queryTime, all points that are more recent(with larger timestamp) than inQueryTime
	 *  are included in query result 
	 * @param inTraLinkedHash
	 * @param inQueryTime
	 * @param outRes
	 */
	private void queryTimeRangePerTraHash(LinkedHashMap<Long,GridLeafTraHashItem> inTraLinkedHash,int inQueryTime,
			ArrayList<Entry<Long,GridLeafTraHashItem>> outRes){
		//visit the linkedhashmap by reverse order, more recent traId has more larger timestamp
		ListIterator <Entry<Long,GridLeafTraHashItem>>  list 
		 		= new ArrayList<Entry<Long,GridLeafTraHashItem>> (inTraLinkedHash.entrySet()).listIterator(inTraLinkedHash.size());
	
		while(list.hasPrevious()){
			Entry<Long,GridLeafTraHashItem> item=(Entry<Long,GridLeafTraHashItem> )list.previous();
			int itemTime=Configuration.getTime(item.getKey());
			//the more recent traId, the larger timestamp
			if(itemTime>=inQueryTime){
				outRes.add(item);
			}
		}
	}
	
	/**
	 * query a item specified by a traId and time
	 * @param inTraLinkedHash
	 * @param traId
	 * @param traTime
	 * @return
	 */
	private GridLeafTraHashItem queryTraIdTimePerTraHash(LinkedHashMap<Long,GridLeafTraHashItem> inTraLinkedHash,
			int traId,int traTime){
		
		Long key=Configuration.getKey(traId, traTime);
		GridLeafTraHashItem gltItem=inTraLinkedHash.get(key);
		return gltItem;
		
	}
	

	/**
	 * check the size of linkedhashmap, if the size of the map is larger that the flush threshold, flush all the 
	 * items in linkedhashmap into disk
	 * 
	 * Think more about it........................!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	private void flushBuffer(){
		
		if(traHash.size()>=Configuration.CapacityPerPage){
			
			try{
				
			byte[] data=BinarySerializer.getByteSerialize(traHash);
			int id=m_pStorageManager.storeByteArray(IStorageManager.NewPage, data);
			countIO++;
			
			if(pageStartTimes.size()>=1){			
				removeOldPages();//too old disk page are discarded
			}
			
			PageStartTimeItem pstItem=new PageStartTimeItem(curPageStartTime,id);
			pageStartTimes.add(pstItem);
			
			traHash.clear();
			
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * if some pages are too old, remove them
	 */
	public void removeOldPages(){
		//the oldest page is in the front of the list, and the most recent page is in the tail of the list
		int duration=curTime-pageStartTimes.get(0).start_time;
		int count=0;
		
		//pay special attention to >=, if ==, still need to go ahead. 
		while(duration>=Configuration.T_period){
			count++;
			if(count>=pageStartTimes.size()) break;
			duration=curTime-pageStartTimes.get(count).start_time+1;			
		}
		
		//all previous data need to be erased. 
		for(int i=0;i<count-1;i++){
			m_pStorageManager.deleteByteArray(pageStartTimes.get(i).pageId);
		}
		
		//shifting, move all the data Item to the front of the list
		for(int j=count-1;j>=0&&j<pageStartTimes.size();j++){
			pageStartTimes.set(j-count+1,pageStartTimes.get(j));
		}
		
		//erase the data at the tail after shifting
		for(int i=0;i<count-1;i++){
			pageStartTimes.remove(pageStartTimes.size()-1);
		}
	}
	
	/**
	 * load hashmap from page(id) into memory
	 * @param id
	 * @return
	 */
	private LinkedHashMap<Long,GridLeafTraHashItem> loadPageHashTra(int id){
		try{
			
			byte[] data=this.m_pStorageManager.loadByteArray(id);
			countIO++;
			
			LinkedHashMap<Long,GridLeafTraHashItem> pageHashTra=null;
			pageHashTra=(LinkedHashMap<Long,GridLeafTraHashItem>)BinarySerializer.getByteDeserialize(data);
			
			//System.out.println("Hit datastream to hash");
			
			Configuration.hitCount++;
			
			return pageHashTra;
			
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * 
	 * @param traTime
	 * @return if return PAGE_ID_CUR, current hashmap is the storage location for thus time, else, return the id for this page 
	 */
	private int getPageIdByTime(int traTime){
		
		if(traTime>=this.curPageStartTime){//in case, we do not need to load disk page
			return PAGE_ID_CUR;
		} 
		
		int pageIdIdx=this.binarySearch(traTime, this.pageStartTimes);
		
		if(pageIdIdx==this.PAGE_ID_NIL) return this.PAGE_ID_NIL;
		
		return pageStartTimes.get(pageIdIdx).pageId;
	}
	
	/**
	 * binary search
	 * @param traTime
	 * @param array
	 * @return
	 */
	private int binarySearch(int traTime,ArrayList<PageStartTimeItem> array){
		
		int startIdx=0,endIdx=array.size()-1,midIdx=-1;
		
		if(0==array.size()) return PAGE_ID_NIL;
		
		int midTime=-1,startTime=-1,endTime=-1;
		
		
		startTime=array.get(0).start_time;
		if(traTime<startTime) return PAGE_ID_NIL;
		

		endTime=array.get(endIdx).start_time;
		if (traTime>=endTime) return endIdx;
			
		while(endIdx-startIdx>1){
			
			startTime=array.get(startIdx).start_time;
			endTime=array.get(endIdx).start_time;
		
			midIdx=(startIdx+endIdx)>>1;
			midTime=array.get(midIdx).start_time;
			
			if(traTime>=midTime) startIdx=midIdx;
			else if(traTime<midTime) endIdx=midIdx;
		}
		
		return startIdx;
		
	}
	
	
	
	
	/**
	 * 
	 * @author workshop
	 *
	 */
	private static class PageStartTimeItem implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2440219611035970010L;

		int start_time;
		
		int pageId;
		
		public PageStartTimeItem(int inStart_time,int inPageId){
			start_time=inStart_time;
			pageId=inPageId;
		}
	}
	
	
	public static void main(String[] args){
		
		testShift();
	}
	
	public static void testShift(){
		long id=1;
		long time=65538;
		
		id<<=32;
		
		long key=id+time;
		
		long id2=key>>32;
		
		id2<<=32;
		
		int time2=(int)(key-id2);
		
		System.out.println(time2);
	}
}



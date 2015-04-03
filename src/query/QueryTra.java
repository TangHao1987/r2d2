package query;

import grid.Grid;
import grid.GridCell;
import grid.RoICell;
import grid.RoIState;
import grid.TraListItem;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;


import traStore.TraStore;
import traStore.TraStoreListItem;

/**
 * 
 * @author workshop
 *
 */
public class QueryTra {
	
	 Grid grid;//grid map record density
	
	 TraStore traStore;//@param : traStore record trajectory
	 RecentTraProcessor recentTraProcessor;
	 double defaultThreshold=-1;
	 int defaultQueryConstraintX=-1;
	 int defaultQueryConstraintY=-1;
	 int defaultQueryDivided=-1;
	 
	 /**
	  * the constructor function of QueryTra
	  * @param inGrid: the grid map, which records the statistic information of whole area
	  * @param inTraStore: the TraStore, which records all the trajectories
	  * @param inLat0: the original lat value of the data, which is the original point in logical space(grid map)
	  * @param inLng0:  the original lng value of the data, which is the original point in logical space
	  * @param inStep:  the step to divided the data
	  * @param inThreshold: the threshold to classify the interesting  cells, we can compute a global threshold in load data class
	  * @param inConstraintX: The constraint for querying the related trajectories in grid map, the unit is one in grid map
	  * @param inConstraintY: The constraint for querying the related trajectories in grid map, the unit is one in grid map
	  * @param inQueryDivided: the default value of divided, it means divide the recent trajectory into divided part
	  * 						Noted that there divided+1 number of query point at last
	  */
	 
	 public QueryTra(Grid inGrid,TraStore inTraStore,double inLat0,
				double inLng0,double inStep,
				double inThreshold,int inQueryConstraintX, int inQueryConstraintY,
				int inQueryDivided){
		 grid=inGrid;
		 traStore=inTraStore;
		 recentTraProcessor=new RecentTraProcessor(inLat0,inLng0,inStep);
		 
		 defaultThreshold=inThreshold;
		 defaultQueryConstraintX=inQueryConstraintX;
		 defaultQueryConstraintY=inQueryConstraintY;
		 defaultQueryDivided=inQueryDivided;
	 }
	 
	 /**
	  * return all the trajectory that is covered by the region of interest in roiSet
	  * @param roiSet
	  * @return
	  */
	 public  Hashtable<Integer,TraListItem> traCollect(HashSet<RoICell> roiSet){
		 Hashtable<Integer,TraListItem> resTra=new Hashtable<Integer,TraListItem>();
		 
		 Iterator<RoICell> itrSet=roiSet.iterator();
		 while(itrSet.hasNext()){
			
			 
			 RoICell rc=itrSet.next();
			 
			 GridCell gc=grid.getGridCell(rc.roiX,rc.roiY);
			 if(gc==null) continue;
			 
			 ArrayList<TraListItem> gcTraList=gc.traList;
			 if(gcTraList==null) continue;
			 
			 for(TraListItem itm:gcTraList){

				 if(!resTra.containsKey(itm.traId)){
					 resTra.put(new Integer(itm.traId), new TraListItem(itm.traId,itm.off,itm.timestamp));
					 
				 }else{
					 TraListItem temp=resTra.get(itm.traId);
					 if(itm.off<temp.off){
						 itm.off=temp.off;
						 itm.timestamp=temp.timestamp;
					 }
				 }
			 }
		 }
		 
		
		 return resTra;
	 }
	 
	 /**
	  * find the intersection of A -> B, there is a time order for those trajectories
	  * @param A
	  * @param B
	  */
	 public Hashtable<Integer,TraListItem> IntersectHashTable(
			 Hashtable<Integer,TraListItem> A,Hashtable<Integer,TraListItem> B){
		 Hashtable<Integer,TraListItem> res=new Hashtable<Integer,TraListItem>();
		 //

		 Enumeration<TraListItem> enuA=A.elements();
		 while(enuA.hasMoreElements()){
			 TraListItem itemA=enuA.nextElement();
			 if(B.containsKey(itemA.traId)){
				 TraListItem itemB=B.get(itemA.traId);
				 if(itemB.timestamp>=itemA.timestamp){
				 TraListItem newResItem=new TraListItem(itemB.traId,itemB.off,itemB.timestamp);
				 res.put(newResItem.traId,newResItem);
				 }
				
			 }
		 }

		 return res;
	 }
	 
	 /**
	  * query all the trajectory by a single cell, the cell is grid position of an user
	  * @param gridX
	  * @param gridY
	  * @param crX
	  * @param crY
	  * @param threshold
	  * @return
	  */
	 public Hashtable<Integer,TraListItem> QueryTraSingleCell(int gridX,int gridY,int crX,int crY,double threshold){
		 // public HashSet<RoICell> findRoI(int gridX,int gridY,int crX,int crY,double threshold){
		RoIState  roiState=grid.findConstraintRoI(gridX,gridY,crX,crY,threshold);
		HashSet<RoICell> roiSet=roiState.roiSet;
		 Hashtable<Integer,TraListItem> res=traCollect(roiSet);
		 
		 return res;
	 }
	 
	 public Hashtable<Integer,TraListItem> QueryTraMultiCell(ArrayList<Point> gridPos){
		 
		 return QueryTraMultiCell(gridPos, this.defaultQueryConstraintX, 
				 this.defaultQueryConstraintY, this.defaultThreshold);
	 }
	 
	 /**
	  * query by multiple cells. Cells are stored in gridPos, and the order is from low to high(0->...)
	  * @param gridPos
	  * @param crX
	  * @param crY
	  * @param threshold
	  * @return
	  */
	 public Hashtable<Integer,TraListItem> QueryTraMultiCell(ArrayList<Point> gridPos,int crX,int crY,double threshold){
		
		 if(gridPos==null||gridPos.size()==0) return null;
		 
		 ArrayList<HashSet<RoICell> > roiSetArray=new ArrayList<HashSet<RoICell> >();
		 
		for(int i=0;i<gridPos.size();i++){
			Point item=gridPos.get(i);
			RoIState roiState=grid.findConstraintRoI(item.x,item.y,crX,crY,threshold);//find the query region in Grid
			
			HashSet<RoICell> roiSet=roiState.roiSet;
			roiSetArray.add(roiSet);//store such region 
		}
		 
		
		
		Hashtable<Integer,TraListItem> sumTra=traCollect(roiSetArray.get(0));//collect the trajectories id that pass first grid region
		
		
		for(int i=1;i<gridPos.size();i++){
			Hashtable<Integer,TraListItem> itemTra=traCollect(roiSetArray.get(i));//the following region
			
			sumTra=this.IntersectHashTable(sumTra,itemTra);//get the joint trajectories that have past all the regions
		}
		 
		 return sumTra;
	 }
	 
	 /**
	  * get the future trajectories according to the trajectories id got by RoI
	  * @param traOffSet
	  * @param futureTime//future prediction time
	  * @param traStore
	  * @return
	  */
	 public Hashtable<Integer,ArrayList<TraStoreListItem>> QueryFutureTraSet(Hashtable<Integer,TraListItem> traOffSet,
			 int futureTime,TraStore traStore){
		 
		 Enumeration<TraListItem > traElm=traOffSet.elements();
		 Hashtable<Integer,ArrayList<TraStoreListItem>> res=new Hashtable<Integer,ArrayList<TraStoreListItem>>();
		 while(traElm.hasMoreElements()){
			 TraListItem item=traElm.nextElement();
			 
			 TraListItem traInfo=traOffSet.get(item.traId);//get the (trajectory id,off) information
			 
			 ArrayList<TraStoreListItem> traStoreListItems=traStore.queryTraByTime(traInfo,futureTime);//query data from trajectory Store
			 
			 res.put(traInfo.traId, traStoreListItems);
		 }
		 
		 return res;
		 
	 }
	 
	 /**
	  * query trajectories using default parameter 
	  * @param recentTra
	  * @param inFutureTime
	  * @return
	  */
	 public Hashtable<Integer,ArrayList<TraStoreListItem>> QueryByRecentTra(ArrayList<TraStoreListItem> recentTra,int inFutureTime){
		 
		 return QueryByRecentTra(recentTra,inFutureTime,this.defaultQueryDivided,this.defaultQueryConstraintX,
				 this.defaultQueryConstraintY,this.defaultThreshold);
	 }
	 
	/**
	 * 
	 * @param recentTra: The recent trajectory, we use recent trajectory to retrieve several reference trajectory in order to make prediction
	 * @param inFutureTime : the future prediction time, it means how long we want to predict
	 * @param inDivided: divide the recent trajectory into divided parts
	 * @param inConstraintX: constraint X for query reference trajectories
	 * @param inConstraintY: constraint Y for query reference trajectories
	 * @param inThreshold: the threshold for classifying the interesting cells or not
	 * @return traSet: which records all the trajectories that is related with recent trajectories
	 */
	 public Hashtable<Integer,ArrayList<TraStoreListItem>> QueryByRecentTra( ArrayList<TraStoreListItem> recentTra,
			 int inFutureTime,int inDivided,int inConstraintX,int inConstraintY,double inThreshold){
		
		 ArrayList<Point> queryPointPos=recentTraProcessor.proRecentTra(recentTra, inDivided);
		 
		 //get the trajectories that pass all the query region(region is defied by a set of query grid cells), the offset of each trajecories in
		 //the largest off(timestamp) in each trajectories, therefore, the first offset should not consider the the reference point for prediction
		 Hashtable<Integer,TraListItem> interRes=QueryTraMultiCell(queryPointPos, inConstraintX, inConstraintY, inThreshold);
		 
		 Hashtable<Integer,ArrayList<TraStoreListItem>> traSet
			= QueryFutureTraSet(interRes, inFutureTime, traStore);
		 
		 return traSet;
		 
	 }
	 
	 
	 
		/**
		 * 
		 * @param interRes
		 * @param traSet
		 */
		public void visitQueryResult(Hashtable<Integer,TraListItem> interRes, 
				Hashtable<Integer,ArrayList<TraStoreListItem>> traSet){
			
			visitQueryPastCells(interRes);
			  visitQueryFutureTra(traSet);
			  
		}
		
		/**
		 * query the cells that has been past by recent trajectories
		 * @param interRes
		 */
		public void visitQueryPastCells(Hashtable<Integer,TraListItem> interRes) {
		
			  Enumeration<TraListItem> interCol=  interRes.elements();
				
			  System.out.println("intersection result");
				
			  while(interCol.hasMoreElements()){
				  TraListItem item=interCol.nextElement();
				  System.out.println("trajectory id:"+item.traId);
				  System.out.println("trajectory off:"+item.off);
			  }
			  
		}
		
		/**
		 * visualization of future trajectories which are related with recent trajectories
		 * @param traSet
		 */
		public void visitQueryFutureTra(Hashtable<Integer,ArrayList<TraStoreListItem>> traSet){
			  Enumeration<ArrayList<TraStoreListItem> > traElements=traSet.elements();
			  Enumeration<Integer> traKeys=traSet.keys();
			  
			  while(traElements.hasMoreElements()&&traKeys.hasMoreElements()){
				  ArrayList<TraStoreListItem> itemRes=traElements.nextElement();
				  int itemTraId=traKeys.nextElement();
				  System.out.println("trajectory id is:"+itemTraId);
				  for(TraStoreListItem offItem:itemRes){
					  System.out.print("<lat:"+offItem.lat+" lng:"+offItem.lng+" time:"+offItem.timestamp+"> ");
				  }
				  System.out.println();
			  }
			  
			  
		}
	 //
	 
	 public  static void main( String args[]){
			
			
	}
	 
}

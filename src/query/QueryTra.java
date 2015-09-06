package query;

import grid.*;
import traStore.TraStore;
import traStore.TraStoreListItem;

import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

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

	 public  Hashtable<Integer,TraListItem> traCollect(HashSet<RoICell> roiSet){
		 Hashtable<Integer,TraListItem> resTra=new Hashtable<>();

         for (RoICell rc : roiSet) {
             GridCell gc = grid.getGridCell(rc.roiX, rc.roiY);
             if (gc == null) continue;

             ArrayList<TraListItem> gcTraList = gc.traList;
             if (gcTraList == null) continue;

             for (TraListItem itm : gcTraList) {
                 if (!resTra.containsKey(itm.traId)) {
                     resTra.put(itm.traId, new TraListItem(itm.traId, itm.off, itm.timestamp));
                 } else {
                     TraListItem temp = resTra.get(itm.traId);
                     if (itm.off < temp.off) {
                         itm.off = temp.off;
                         itm.timestamp = temp.timestamp;
                     }
                 }
             }
         }
		 
		
		 return resTra;
	 }

	 public Hashtable<Integer,TraListItem> IntersectHashTable(
			 Hashtable<Integer,TraListItem> A,Hashtable<Integer,TraListItem> B){
		 Hashtable<Integer,TraListItem> res=new Hashtable<>();
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


	 public Hashtable<Integer,TraListItem> QueryTraMultiCell(ArrayList<Point> gridPos,int crX,int crY,double threshold){
		
		 if(gridPos==null||gridPos.size()==0) return null;
		 
		 ArrayList<HashSet<RoICell> > roiSetArray=new ArrayList<HashSet<RoICell> >();

         for (Point item : gridPos) {
             RoIState roiState = grid.findConstraintRoI(item.x, item.y, crX, crY, threshold);//find the query region in Grid

             HashSet<RoICell> roiSet = roiState.roiSet;
             roiSetArray.add(roiSet);//store such region
         }
		 
		
		
		Hashtable<Integer,TraListItem> sumTra=traCollect(roiSetArray.get(0));//collect the trajectories id that pass first grid region
		
		
		for(int i=1;i<gridPos.size();i++){
			Hashtable<Integer,TraListItem> itemTra=traCollect(roiSetArray.get(i));//the following region
			
			sumTra=this.IntersectHashTable(sumTra,itemTra);//get the joint trajectories that have past all the regions
		}
		 
		 return sumTra;
	 }

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

	 public Hashtable<Integer,ArrayList<TraStoreListItem>> QueryByRecentTra(ArrayList<TraStoreListItem> recentTra,int inFutureTime){

		 return QueryByRecentTra(recentTra,inFutureTime,this.defaultQueryDivided,this.defaultQueryConstraintX,
				 this.defaultQueryConstraintY,this.defaultThreshold);
	 }


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


		public void visitQueryResult(Hashtable<Integer,TraListItem> interRes,
				Hashtable<Integer,ArrayList<TraStoreListItem>> traSet){

			visitQueryPastCells(interRes);
			  visitQueryFutureTra(traSet);

		}


		public void visitQueryPastCells(Hashtable<Integer,TraListItem> interRes) {
		
			  Enumeration<TraListItem> interCol=  interRes.elements();
				
			  System.out.println("intersection result");
				
			  while(interCol.hasMoreElements()){
				  TraListItem item=interCol.nextElement();
				  System.out.println("trajectory id:"+item.traId);
				  System.out.println("trajectory off:"+item.off);
			  }
			  
		}
		

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

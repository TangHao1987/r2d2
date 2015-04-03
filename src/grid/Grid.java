package grid;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import prediction.MicroState;
import prediction.RelaxMicroState;
import prediction.TimeTraItem;
import storagemanager.DiskStorageManager;
import storagemanager.IStorageManager;
import storagemanager.PropertySet;



public class Grid implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9215443852086117712L;

	private GridCell root;//the root of grid(i.e. quad-tree)

	private int maxIndex;//the upper bound of the grid index,this stores the maximum possible cellx and celly coordinations in the grid

	private IStorageManager g_Storage;
	private int curTime;
	
	private double[][] mask;//define the mask for density update
	private double maskSum;

	QueryCache qc;
	

	//construct parameter

	
	public Grid() {
			
		//set up the disk storage manager
		PropertySet ps=new PropertySet();
		Boolean b = new Boolean(true);
		ps.setProperty("Overwrite", b);//overwrite the file if it exists.
		ps.setProperty("FileName", Configuration.GridFile + ".grid");// .idx and .dat extensions will be added.
		// specify the page size. Since the index may also contain user defined data
		// there is no way to know how big a single node may become. The storage manager
		// will use multiple pages per node if needed. Off course this will slow down performance.
		ps.setProperty("PageSize", Configuration.PageSize);
		
		qc=null;//as default, QueryCache is closed
		
		initialGrid(ps);
	}
	
	public int getCellNum(int x0, int y0, int x1, int y1){
		int count = 0;
		for(int i=x0;i<x1;i++){
			for(int j=y0;j<y1;j++){
				if(this.getGridCell(i, j)!=null){
					count++;
				}
			}
		}
		return count;
	}
	public int getSizeof(){
		return root.getSizeOf();
		
	}
	
	public void closeQC(){
		qc=null;
	}
	
	public void openQC(){
		qc=new QueryCache();
	}
	
	public boolean clearQC(){
		if(null==qc) return false;
		qc.clear();
		return true;
	}
	
	public int getQCHitcount(){
		if(null==qc) return -1;
		return qc.hitCount;
	}
	
	public Grid(PropertySet ps){
		initialGrid(ps);	//
	}
	
	
	public void initialGrid(PropertySet ps){
		setDefaultMask();//set the mask for update density
		curTime=0;
		
		try{
		g_Storage=new DiskStorageManager(ps);//create new disk storage manager based on property set
		}catch(Exception e){
			e.printStackTrace();
		}
		
		int nodeCount=(int)Math.pow(2,Configuration.BITS_PER_GRID);
		maxIndex=(int)Math.pow(nodeCount, Configuration.MAX_LEVEL)-1;//compute the maximum possible cellx and celly coordinations in the grid
		
		this.initialGridRootNode();//create the root of the grid(quad-tree)
		//threshold=this.EstimateThreshold(x1, y1, x2, y2)
	}
	

	
	/**
	 * initialize the root of the grid. the root itself is a grid cell
	 */
	private void initialGridRootNode() {
		root=new GridCell(Configuration.MAX_LEVEL,this.g_Storage);//create the root of the tree
	}
	
	/**
	 * get the cell with coordinate x y.
	 * @param x
	 * @param y
	 * @return
	 */
	public GridCell getGridCell(int x, int y) {
		//return gridArray[x * width + y];
		if (x < 0||y<0||x>=this.maxIndex||y>=this.maxIndex)//if out of boundary, just return null
			return null;
		
		int level_count=root.level;//start from root
		GridCell[][] array=root.gridArray;
		
		int level_x=-1;
		int level_y=-1;
		GridCell gc=null;
		while(level_count>0){//go down to the bottom, if there is empty cell, return immediately. this only visits the 
			//the inner node, and ignore the leaf, therefore, level_count>0
			
			level_count--;
			
			level_x=x>>(level_count*Configuration.BITS_PER_GRID);//find the grid cell by high bit
			level_y=y>>(level_count*Configuration.BITS_PER_GRID);
			
			gc=array[level_x][level_y];
			
			if(gc==null) return null;
			else{
				array=gc.gridArray;
				
				x-=level_x<<(level_count*Configuration.BITS_PER_GRID);//minus the high bits
				y-=level_y<<(level_count*Configuration.BITS_PER_GRID);
			
			}
		}
		return gc;
	}
	
	/**set the mask
	 * 
	 */
	public void setMask(double [][] m){
		mask=m;
		
		maskSum=0;
		for(int i=0;i<=2;i++){
			for(int j=0;j<=2;j++){
				maskSum+=mask[i][j];
			}
			
		}
		
		for(int i=0;i<=2;i++){
			for(int j=0;j<=2;j++){
				mask[i][j]/=maskSum;
			}
		}
	}
	
	/**
	 * the default of mask, which is an estimation of Gaussian Distribution
	 */
	private void setDefaultMask(){
		mask=new double[3][3];
		mask[0][0]=1;
		mask[0][1]=2;
		mask[0][2]=1;
		
		mask[1][0]=2;
		mask[1][1]=8;
		mask[1][2]=2;
		
		mask[2][0]=1;
		mask[2][1]=2;
		mask[2][2]=1;
		
		maskSum=20.;
		
		for(int i=0;i<=2;i++){
			for(int j=0;j<=2;j++){
				mask[i][j]/=maskSum;
			}
		}
	}
	


	
	/**
	 * increase the density of the GridCell, with delta, x and y corresponding to the cell location
	 * @param x
	 * @param y
	 * @param delta
	 */
	public void increaseDensityDirect(int x,int y,double delta){
		//if out of boundary, return immediately
		if (x < 0||y<0||x>=this.maxIndex||y>=this.maxIndex)
			return;

		// similar code with function of public GridCell getGridCell(int x, int y), by create new cell
		//right away when find a null grid cell
		int level_count = root.level;
		GridCell[][] array = root.gridArray;

		int level_x = -1;
		int level_y = -1;
		GridCell gc = null;
		while (level_count > 0) {// go down to the bottom, only visits the inner node, therefore, level_count > 0
		
			level_count--;
			
			level_x = x >> (level_count * Configuration.BITS_PER_GRID);
			level_y = y >> (level_count * Configuration.BITS_PER_GRID);

			gc = array[level_x][level_y];

			if (gc == null){
				array[level_x][level_y]=new GridCell(level_count,this.g_Storage);//create new cells
				gc=array[level_x][level_y];
			}

			array = gc.gridArray;
			
			x -= level_x << (level_count * Configuration.BITS_PER_GRID);
			y -= level_y << (level_count * Configuration.BITS_PER_GRID);

		//	level_count--;

		}
		
		gc.density+=delta;
	}
	
	
	/**
	 * update the density of such cell, with mask operation, i.e., we update 9 cells at the same time
	 * @param x
	 * @param y
	 * @param density
	 */
	public void updateDensityMask(int x,int y){
		
		for(int s=-1;s<=1;s++){
			for(int t=-1;t<=1;t++){
				increaseDensityDirect(x+s,y+t,mask[1+s][1+t]);
			}
		}
	}
	
	
	
	
	/**
	 * a point stay here, therefore, the next location of moving object in GridLeafEntry is still the same cells
	 * @param cellx1
	 * @param celly1
	 * @param t1
	 * @param inTraId
	 */
	private void updatePointAndDensity(int cellx1,int celly1,int t1,int inTraId){
		updateDensityMask(cellx1,celly1);
		GridCell gc=this.getGridCell(cellx1, celly1);
		if(gc!=null)
		gc.gridLeafEntry.append(inTraId, t1, cellx1, celly1);
	}
	
	/**
	 * append the line to current cell, and record the next cell of this moving object,
	 * note that the time for cellx2, celly2 is not necessary, as it can be computed by t2=t1+Configuration.T_sample
	 * @param cellx1
	 * @param celly1
	 * @param t1
	 * @param cellx2
	 * @param celly2
	 * @param inTraId
	 */
	private void updatePointAndNext(int cellx1,int celly1,int t1,int cellx2,int celly2, int inTraId){
		GridCell gc=this.getGridCell(cellx1, celly1);
		if(gc==null){return;}
		
		gc.gridLeafEntry.append(inTraId, t1, cellx2, celly2);
	}
	
	/**
	 *  update the density of line, from (x1,y1) to (x2,y2)
	 * @param cellx1
	 * @param celly1
	 * @param t1
	 * @param cellx2
	 * @param celly2
	 * @param t2  t2 is not recorded in fact, as the t2-t1=Configuration.T_sample, in this function, t2 is used to update curTime of the grid
	 * @param traId
	 */
	public void updateLineTra(int cellx1,int celly1,int t1,int cellx2,int celly2,int t2,int traId){
		int k;
		double x,y,t,dx,dy,dt;
		int x0=0,y0=0,t0=0;
		curTime=t2;
		k=Math.abs(cellx2-cellx1);
		if(Math.abs(celly2-celly1)>k) k=Math.abs(celly2-celly1);
		
		if(k==0){
			//stay in the same grid cells, without moving
			updatePointAndDensity( cellx1, celly1, t1, traId);
			return;
		}
		
		//DDA
		dx=(double)(cellx2-cellx1)/k;
		dy=(double)(celly2-celly1)/k;
		dt=(double)(t2-t1)/k;
		
		x=(double)(cellx1);
		y=(double)(celly1);
		t=(double)(t1);
		
		for(int i=0;i<k;i++){
			x0=(int)(x+0.5);
			y0=(int)(y+0.5);
			updateDensityMask(x0,y0);
			
			t0=(int) (t+0.5);
			//updateTra(x0,y0,traId,off1,t0); //we maybe do not consider such interpolation of trajectory, only increase the density
			
			x+=dx;
			y+=dy;
			t+=dt;
		}
		
		//update with point, only the start and end point
		updatePointAndNext(cellx1,celly1,t1,cellx2,celly2,traId);
	}
	

	 /**
	  * find constraint region of interest
	  * @param gridX//seed point
	  * @param gridY//seed point
	  * @param crX// constraint for x
	  * @param crY//constraint for y
	  * @param threshold//threshold for roi
	  * @return 
	  */
	 public RoIState findConstraintRoI(int gridX,int gridY,int crX,int crY,double threshold){
		 
		 int crXMin=gridX-crX;
		 if(crXMin<0) crXMin=0;
		 
		 int crXMax=gridX+crX;
		 if(crXMax>maxIndex) crXMax=maxIndex;
		 
		 int crYMin=gridY-crY;
		 if(crYMin<0) crYMin=0;
		 
		 int crYMax=gridY+crY;
		 if(crYMax>maxIndex) crYMax=maxIndex;
		 
		 RoIState roiState=new RoIState();
		 //roiSet.clear();
		 
		 recursive_findRoI(gridX,gridY,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		 
		 return roiState;
		
	 }
	 
	 
	 /**
	  * find RoI by recursive method. result is stored in roiSet
	  * @param gridX
	  * @param gridY
	  * @param crXLeft
	  * @param crXRight
	  * @param crYUp
	  * @param crYDown
	  * @param threshold
	  * @param roiSet//return result
	  */
	 private void recursive_findRoI(int gridX,int gridY,
			 int crXMin,int crXMax,int crYMin,int crYMax,double threshold,RoIState roiState){
		 
		  if(gridX>crXMax||gridX<crXMin) return;
		  if(gridY>crYMax||gridY<crYMin) return;
		   
		  if(roiState.contains(gridX, gridY)) return;
		  
		  GridCell gc=getGridCell(gridX,gridY);
		  if(gc==null||gc.density<threshold){
			  return;
		  }
		  else{
			  roiState.addRoICell(gridX, gridY, gc.density);
		  }
		  //recursive
		  recursive_findRoI(gridX,gridY+1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX,gridY-1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX-1,gridY,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX+1,gridY,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX+1,gridY+1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX+1,gridY-1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX-1,gridY+1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);
		  recursive_findRoI(gridX-1,gridY-1,crXMin,crXMax,crYMin,crYMax,threshold,roiState);

	 }
	
	 /**
	  * query all the trajectories that pass a cell (cellX,cellY)
	  * @param cellx
	  * @param celly
	  * @param time time is a downward time range
	  * @return
	  */
	 private ArrayList<Entry<Long,GridLeafTraHashItem>> queryByCellTimeRange(int cellx,int celly,int timeRange){
		 GridCell gc=this.getGridCell(cellx, celly);//find the corresponding leaf of this cell
		 if(gc==null) return null;//if cell is empty, return null
		 
		 int st=this.curTime-timeRange;
		 return gc.gridLeafEntry.queryTimeRangeForward(st);//return result of leaf query
	 }
	
	 /**
	  * queyr all the trajectires that pass this RoI
	  * @param roiState
	  * @param timeRange  downward time scale
	  * @return <key,value>  key-- traId+time;  value:(cell_x,cell_y)
	  */
	 private ArrayList<Entry<Long,GridLeafTraHashItem>> queryByRoITimeRange(RoIState roiState,int timeRange){
		 
		 	RoICell[] roiCells=roiState.toArray();//convert to array of RoICell
			
		 	//store the result
			ArrayList<Entry<Long,GridLeafTraHashItem>> res=new ArrayList<Entry<Long,GridLeafTraHashItem>>();
			
			for(int i=0;i<roiCells.length;i++){
				//find corresponding leaf( grid cell)
				GridCell gc=this.getGridCell(roiCells[i].roiX, roiCells[i].roiY);
				
				//query and add to result
				if(null!=gc){
				int st=this.curTime-timeRange;
				ArrayList<Entry<Long,GridLeafTraHashItem>> resItem=gc.gridLeafEntry.queryTimeRangeForward(st);
				res.addAll(resItem);
				}
			} 
			return res;//return result
	 }
	 
	 /**query all the trajectories that pass a set of RoI, and the order sequence of the trajectories are roiArray[0]->roiArray[1]->roiArray[2]->.....
	  * return traId from roiArray[0]->roiArray[1]->roiArray[2]->.....
	  * @param roiArray
	  * @return
	  */
	 public ArrayList<Entry<Long,GridLeafTraHashItem>> queryByMultiRoITimeRange(ArrayList<RoIState> roiArray){
		 
		 return queryByMultiRoITimeRange(roiArray,Configuration.T_period);
	 }
	 
	 /**query all the trajectories that pass a set of RoI, and the order sequence of the trajectories are roiArray[0]->roiArray[1]->roiArray[2]->.....
	  * return traId from roiArray[0]->roiArray[1]->roiArray[2]->.....
	  * @param roiArray
	  * @param timeRange  downward time scale,
	  * @return ArrayList<Entry<key,value>>  key-- traId+time;  value:(cell_x,cell_y)
	  */
	 public ArrayList<Entry<Long,GridLeafTraHashItem>> queryByMultiRoITimeRange(ArrayList<RoIState> roiArray,int timeRange){
		 
		 //query all the trajectories at state 0
		 ArrayList<Entry<Long,GridLeafTraHashItem>> roiQA=this.queryByRoITimeRange(roiArray.get(0), timeRange);
		 
		 if(roiArray.size()==1) return roiQA;//if there is only one RoI, return result immediately.
		 else{

			 HashMap<Integer,Entry<Long,GridLeafTraHashItem>> resHash=
				 				new HashMap<Integer,Entry<Long,GridLeafTraHashItem>>();
			 
			 //add the trajectories of first state into hashmap
			 for(Entry<Long,GridLeafTraHashItem> AItem:roiQA ){
				 int AItemTraId=Configuration.getTraId(AItem.getKey());//get tra id
				 int AItemTime=Configuration.getTime(AItem.getKey());
				 resHash.put(AItemTraId, AItem);
			 }
			 
			 //first state is computed again
			 for(int i=1;i<roiArray.size();i++){
				 ArrayList<Entry<Long,GridLeafTraHashItem>> BItem=this.queryByRoITimeRange(roiArray.get(i), timeRange);
				 //direction is  resHash->B
				 resHash=this.IntersectMultiRoIQuery(resHash, BItem);
			 }
			 
			 
			 //only keep the values
			 ArrayList<Entry<Long,GridLeafTraHashItem>> resArray=new  ArrayList<Entry<Long,GridLeafTraHashItem>>(resHash.values());
			 return resArray;
		 }
	 }
	 
	 /**
	  * find the intersection of A -> B, there is a time order for those trajectories. 
	  * @param A
	  * @param B
	  */
	 private HashMap<Integer,Entry<Long,GridLeafTraHashItem>> IntersectMultiRoIQuery(
			 HashMap<Integer,Entry<Long,GridLeafTraHashItem>>  A, ArrayList<Entry<Long,GridLeafTraHashItem>>  B){
		 
		 HashMap<Integer,Entry<Long,GridLeafTraHashItem>> res=new HashMap<Integer, Entry<Long,GridLeafTraHashItem>> ();
		 
		 for(Entry<Long,GridLeafTraHashItem> BItem:B){
			 int BItemTraId=Configuration.getTraId(BItem.getKey());//get tra id
			 
			 Entry<Long,GridLeafTraHashItem> AItem=res.get(BItemTraId);//test whether traId is in Res, which means it is in both A and B
			 if(null==AItem){
			 AItem=A.get(BItemTraId);//test whether traId is in HashMap A
			 }
			 if(null!=AItem){//if traId is in A, or in (A and B)
				 int BItemTime=Configuration.getTime(BItem.getKey());//get the time of traId at RoI state B
				 int AItemTime=Configuration.getTime(AItem.getKey());//get the time of traId at RoI state A
				 if(BItemTime>=AItemTime){//only add most recent traId item
					 res.put(BItemTraId, BItem);
				 }
			 }
		 }
		 return res;
	 }
	 
	 
	 /**
	  * query all the trajectories with a RoI, the RoI is found by a seed cell(gridX,gridY) with a constraint crX and crY.
	  * @param gridX
	  * @param gridY
	  * @param crX
	  * @param crY
	  * @param timeRange  downward time scale
	  * @param threshold  classify the interesting cell or not
	  * @return
	  */
	 public ArrayList<Entry<Long,GridLeafTraHashItem>> queryRangeTimePerCell(int  gridX,int gridY,
			 int crX,int crY,int timeRange,double threshold){
		 
		 RoIState roiState=this.findConstraintRoI(gridX, gridY, crX, crY, threshold);//find RoI
		 ArrayList<Entry<Long,GridLeafTraHashItem>> res=this.queryByRoITimeRange(roiState, timeRange);//get trajectories
		 
		 return res;
	 }
	
	 /**
		 * Given a sequence of cells, then infer a sequence of RoI. Find all trajectories that pass the sequence of RoI
		  * cellArray:cellArray[0]->cellArray[1]->cellArray[2]->...
		 * @param cellArray
		 * @return  ArrayList<Entry<key,value>>  key-- traId+time;  value:(cell_x,cell_y)
		 */
		public ArrayList<Entry<Long,GridLeafTraHashItem>> queryRangeTimeSeqCells(ArrayList<RoICell> cellArray){
			
			return queryRangeTimeSeqCells(cellArray,
					 Configuration.BrinkConstraintRoI, Configuration.BrinkConstraintRoI, Configuration.T_period,Configuration.BrinkThreshold);
		}
		
	 
	 
	 /**Given a sequence of cells, then infer a sequence of RoI. Find all trajectories that pass the sequence of RoI
	  * cellArray:cellArray[0]->cellArray[1]->cellArray[2]->...
	  * @param cellArray
	  * @param crX
	  * @param crY
	  * @param timeRange
	  * @param threshold
	  * @return   ArrayList<Entry<key,value>>  key-- traId+time;  value:(cell_x,cell_y)
	  */
	 public ArrayList<Entry<Long,GridLeafTraHashItem>> queryRangeTimeSeqCells(ArrayList<RoICell> cellArray,
			 int crX,int crY, int timeRange,double threshold){
		 
		 ArrayList<RoIState> roiArray=new ArrayList<RoIState>();
		 for(RoICell c:cellArray){
			 RoIState roi=this.findConstraintRoI(c.roiX, c.roiY, crX, crY, threshold);
			 if(roi.getSize()<1) continue;
			 roiArray.add(roi);// a sequence of RoI
		 }
		 if(roiArray.size()>=1){
		 ArrayList<Entry<Long,GridLeafTraHashItem>> res=this.queryByMultiRoITimeRange(roiArray, timeRange);//get trajectories
		 putQCClearTime(res);
		 
		 return res;
		 }else{
			 return null;
		 }
		 
	 }
	 
	
	 
	/**
	 * given a list of micro state, query the grid, and get a list of query
	 * result for each micro state
	 * 
	 * @param mics
	 *            : a list of micro state
	 * @return: a list of query result
	 */
	public ArrayList<RelaxMicroState> forwardQueryMics(
			ArrayList<MicroState> mics) {
		if (null == mics||0==mics.size())
			return null;

		ArrayList<RelaxMicroState> res = new ArrayList<RelaxMicroState>();//

		for (MicroState micItem : mics) {

			RelaxMicroState micRelax = new RelaxMicroState();
			res.add(micRelax);

			// visit every cells in this micro state
			Iterator<Entry<RoICell, ArrayList<Long>>> micLtratimeItr = micItem.Ltratime
					.entrySet().iterator();
			while (micLtratimeItr.hasNext()) {
				// get cell and all the tra id in this cell
				Entry<RoICell, ArrayList<Long>> cellMicItem = micLtratimeItr.next();
				
				ArrayList<Long> storeQ=new ArrayList<Long>();
				//query from QueryCache firstly
				ArrayList<Entry<Long,GridLeafTraHashItem>> cRes=queryQC(cellMicItem.getValue(),storeQ);
				if(null==cRes){//if return null, all the data still is in storeQ
					storeQ=cellMicItem.getValue();
					
				}else{
					micRelax.addLtratime(cRes);
				}
				
				if(0==storeQ.size()){
					continue;
				}

				// query grid cell
				GridCell gcMicItem = this.getGridCell(cellMicItem.getKey().roiX, cellMicItem.getKey().roiY);
				
				// query the next cell position for all tra id in this cell
				ArrayList<Entry<Long, GridLeafTraHashItem>> gcMicItemRes = gcMicItem.gridLeafEntry//be here!!!
						.queryTraSet(cellMicItem.getValue());
				
				putQC(gcMicItemRes);
				
				micRelax.addLtratime(gcMicItemRes);// add to result
				//micRelax.Ltratime.addAll(gcMicItemRes);// add to result
			}

		}

		return res;
	}
	
	/**
	 * insert into querycache, and also set the expire time
	 * @param seqQres
	 */
	 private void putQCClearTime(ArrayList<Entry<Long, GridLeafTraHashItem>> seqQres){
			if(null==qc||null==seqQres||0==seqQres.size()) return;
			
			for(Entry<Long,GridLeafTraHashItem> ei:seqQres){
				int traId=Configuration.getTraId(ei.getKey());
				int time=Configuration.getTime(ei.getKey());
				qc.setCacheExpire(traId, time);
				qc.insert(traId, time, ei);
			
				
			}
	 }
	
	/**
	 * put it into querycache
	 * @param gcMicItemRes
	 */
	private void putQC(ArrayList<Entry<Long, GridLeafTraHashItem>> gcQres){
		if(null==qc||null==gcQres||0==gcQres.size()) return;
		
		for(Entry<Long,GridLeafTraHashItem> ei:gcQres){
			int traId=Configuration.getTraId(ei.getKey());
			int time=Configuration.getTime(ei.getKey());
			qc.insert(traId, time, ei);
		}
	}
	
	/**
	 * query by QueryCache, the result is put in return, and outQ is used to store all the query that is not hit in cache
	 * @param q
	 * @param qres
	 * @return
	 */
	private ArrayList<Entry<Long,GridLeafTraHashItem>> queryQC(ArrayList<Long> q,ArrayList<Long> outQ){
		if(null==qc||qc.size()<=0) return null;
		if(null==q||q.size()<=0) return null;//return if no result
		ArrayList<Entry<Long,GridLeafTraHashItem>> res=new ArrayList<Entry<Long,GridLeafTraHashItem>>();//store result
		
		//visit every query
		for(Long item:q){
			int traId=Configuration.getTraId(item);
			int time=Configuration.getTime(item)+Configuration.T_Sample;
			
			Entry<Long,GridLeafTraHashItem> eitem=qc.hitCache(traId, time);//get query result
			
			if(null!=eitem){
				res.add(eitem);//res
			} else{
				outQ.add(item);//this should be queried from storage
			}
		}
		
		return res;
		
	}
	
	
	 public static void main(String[] args){
		 testQueryGrid();
	 }
	 
		 public static void testGeneral(){
				
				try{
				//IStorageManager diskfile = new DiskStorageManager(ps);
				
			    ArrayList<Point> queryStore=new ArrayList<Point>();
				Grid grid=new Grid();
				
				Random rd=new Random(2);
				
				int x=0,y=0;
				
				int recordTraId=5;
				int recordTime=-1;
				int recordNextX=-1;
				int recordNextY=-1;
				
				int recordPageNextX=-1;
				int recordPageNextY=-1;
				int recordStopTime=10000-350;
				int recordPageTime=-1;
				
			
				
				long startLoad=System.currentTimeMillis();
				for(int j=0;j<10000000;j++){
					int cx=rd.nextInt(128);
					int cy=rd.nextInt(128);

					int oldx=x;
					int oldy=y;
					
					if(queryStore.size()<1000000)
					queryStore.add(new Point(oldx,oldy));
					
					x=(int)Math.pow(-1,cx)*rd.nextInt(128);
					y=(int)Math.pow(-1, cy)*rd.nextInt(128);
					int id=rd.nextInt(10);
					if(x<0) x=-x; if(y<0) y=-y;
					grid.updateLineTra(oldx, oldy, j, x, y, j+4, id);
					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
				
				}
				
				
				
				long endLoad=System.currentTimeMillis();
				System.out.println("load time is:"+(endLoad-startLoad));
				
				long start1=System.currentTimeMillis();
				for(Point p:queryStore){
					grid.queryByCellTimeRange(p.x,p.y,10000);
				}
				long end1=System.currentTimeMillis();
				System.out.println("total time is:"+(double)(end1-start1));
				System.out.println("hit data stream to hash:"+Configuration.hitCount);
				
				
				}catch(Exception e){
					e.printStackTrace();
				}

			}
		 
		 public static void testQueryGrid(){
				
				try{
				//IStorageManager diskfile = new DiskStorageManager(ps);
				
			    ArrayList<Point> queryStore=new ArrayList<Point>();
				Grid grid=new Grid();
				
				Random rd=new Random(2);
				
				int x=0,y=0;			
				int count=0;
				int testX,testY;
				testX=16;
				testY=4;
				long startLoad=System.currentTimeMillis();
				int dur=1000000;
				
				int xr=32,yr=32;
				
				int map[][] =new int[xr][];
				for(int i=0;i<map.length;i++){
					map[i]=new int[yr];
					for(int j=0;j<map[i].length;j++){
						map[i][j]=0;
					}
				}
				
				for(int j=0;j<dur/2;j++){
					int cx=rd.nextInt(4);
					int cy=rd.nextInt(4);
					
					cx=(int)Math.pow(-1, cx)*cx;;
					cy=(int)Math.pow(-1, cy)*cy;
					
					int oldx=x;
					int oldy=y;
					map[oldx][oldy]++;
					x+=cx;
					y+=cy;
					
					if(x<0||x>=xr) x-=2*cx;
					if(y<0||y>=yr) y-=2*cy;
		
					int id=rd.nextInt(10);
					if(oldx==testX&&oldy==testY){
						count++;
					}
					grid.updateLineTra(oldx, oldy, j, x, y, j+1, id);
					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
				
				}
				for(int i=0;i<64;i++){
					if(i>=11&&i<=13){
						grid.updateLineTra(i-3, i-3, dur+i, i+1, i+1, dur+i+1, 11);
						//	grid.updateLineTra(i, i, dur+i, i+1, i+1, dur+i+1, 11);
					} else{
					grid.updateLineTra(i, i, dur+i, i+1, i+1, dur+i+1, 11);
					}
				}
				
				for(int j=dur/2;j<dur;j++){
					int cx=rd.nextInt(4);
					int cy=rd.nextInt(4);
					
					cx=(int)Math.pow(-1, cx)*cx;;
					cy=(int)Math.pow(-1, cy)*cy;
					
					int oldx=x;
					int oldy=y;
					map[oldx][oldy]++;
					x+=cx;
					y+=cy;
					
					if(x<0||x>=xr) x-=2*cx;
					if(y<0||y>=yr) y-=2*cy;
		
					int id=rd.nextInt(10);
					if(oldx==testX&&oldy==testY){
						count++;
					}
					grid.updateLineTra(oldx, oldy, j, x, y, j+1, id);
					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
				
				}
				
				
				//test-1   grid.queryByCellTimeRange
				//	ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByCellTimeRange(testX,testY,100000);
				
				//test-2  grid.queryByRoITimeRange
				/*RoIState roiState=new RoIState();
				for(int rx=15;rx<=17;rx++){
					for(int ry=15;ry<=17;ry++){
						roiState.addRoICell(rx,ry,0);
					}
				}
				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByRoITimeRange(roiState, dur/2+500);
				for(int i=0;i<res.size();i++){
					System.out.println("triaId:"+Configuration.getTraId(res.get(i).getKey())+" time:"+Configuration.getTime(res.get(i).getKey())+
							" x:"+res.get(i).getValue().getCellX()+" y:"+res.get(i).getValue().getCellY());
				}*/
				
				
				//test-3  queryByMultiRoITimeRange
			/*	ArrayList<RoIState> queryRoIs=new ArrayList<RoIState>();
				
				for(int k=8;k<=16;k+=4){
					RoIState roiState=new RoIState();
					
					for(int rx=k-2;rx<=k+2;rx++){
						for(int ry=k-2;ry<=k+2;ry++){
							roiState.addRoICell(rx, ry, 0);
						}
					}
					queryRoIs.add(roiState);
				}
				
				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByMultiRoITimeRange(queryRoIs, dur/2+500);
				for(int i=0;i<res.size();i++){
					System.out.println("triaId:"+Configuration.getTraId(res.get(i).getKey())+" time:"+Configuration.getTime(res.get(i).getKey())+
							" x:"+res.get(i).getValue().getCellX()+" y:"+res.get(i).getValue().getCellY());
				}
			 */
				
			//test 4-- queryRangeTimePerCell
				ArrayList<RoICell> cells=new ArrayList<RoICell>();
				for(int k=8;k<=16;k+=4){
					RoICell rc=new RoICell(k,k);
					cells.add(rc);
				}
				
				//ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryRangeTimePerCell(16, 16, 2, 2, dur/2+500, 1);
				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryRangeTimeSeqCells(cells, 2, 2, dur/2+500, 1);
				for(int i=0;i<res.size();i++){
					System.out.println("triaId:"+Configuration.getTraId(res.get(i).getKey())+" time:"+Configuration.getTime(res.get(i).getKey())+
							" x:"+res.get(i).getValue().getCellX()+" y:"+res.get(i).getValue().getCellY());
				}
				
				
			    System.out.println("count:"+count);
				
			    for(int i=0;i<xr;i++){
			    	for(int j=0;j<yr;j++){
			    		System.out.print(map[i][j]+ " ");
			    	}
			    	System.out.println();
			    }
				
				}catch(Exception e){
					e.printStackTrace();
				}

		 }
	 
}




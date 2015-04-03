package grid;

public class RoICell {
public int roiX;
public int roiY;
public double density;
/**
 * RoICell is used to store the region of interest cell. It is an element of RoIState
 * @param gridX
 * @param gridY
 */
public RoICell(int gridX,int gridY){
	roiX=gridX;
	roiY=gridY;
	density=0;
}

public RoICell(int gridX,int gridY,double inDensity){
	roiX=gridX;
	roiY=gridY;
	density=inDensity;
}
@Override
public boolean equals(Object obj) {
	
	boolean one=(obj instanceof RoICell) ;
	boolean two=(roiX ==(((RoICell)obj).roiX));
	boolean three=(roiY==(((RoICell)obj).roiY));
	
    return (one && two&&three);
  }
@Override
public int hashCode() { 
    
    return roiX<<16+roiY;
  }

@Override
public String toString(){
	
	//String str="<RoICell gridX:"+roiX+" gridY:"+roiY+">";
	String str="<"+roiX+","+roiY+">";
	return str;
	
}
}

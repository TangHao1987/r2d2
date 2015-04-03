package grid;

public class TraListItem {
public int traId;
public int timestamp;
public int off;//the timestamp is guaranteed to be after the off

/**
 * 
 * @param inputTraId
 * @param inOff
 * @param inputTimestamp
 */
public TraListItem(int inputTraId,int inOff,int inputTimestamp){
	this.traId=inputTraId;
	this.timestamp=inputTimestamp;
	this.off=inOff;
	
}
}

package loadData;

import java.awt.Point;

public  class MapLoc2Grid {
	private static double lat0;
	private static double lng0;
	private static double step0;
	
	public static Point transferGrid(double lat,double lng){
		double offx=lat-lat0;
		double offy=lng-lng0;
	
		
		
		int gridX=(int)(offx/(step0));
		int gridY=(int)(offy/(step0));
		
		return new Point(gridX,gridY);
	}
	
	public static void setParameter(double inLat0,double inLng0,double inStep0){
		lat0=inLat0;
		lng0=inLng0;
		step0=inStep0;
	}
}

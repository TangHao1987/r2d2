package loadData;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class SQLiteDriver {
	public static Connection conn;
	public static ResultSet rs;
	
	/**
	 * Open DB, togather with closeDB and exeSQL
	 * @param db
	 */
	public static void openDB(String db){
		try {
			Class.forName("org.sqlite.JDBC");
		
		String conStr="jdbc:sqlite:"+db;
		 conn = DriverManager.getConnection(conStr);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * close DB, with openDB and exeSQL
	 */
	public static void closeDB(){
		try {
		conn.close();
		rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * with opendb and closedb
	 * @param sql
	 */
	public static void exeSQL(String sql){
		try{
		 Statement stat = conn.createStatement(); 
		
		 rs = stat.executeQuery(sql);
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param db
	 * @param timeStart
	 * @param timeEnd
	 * @return there are five elements, and they are [minLat,minLng,maxLat,maxLng,num of taxi]
	 */
	public static double[] getMaxMinNum(String db,String table,String timeStart,String timeEnd){
		double[] res=new double[5];
		
		try {
			openDB( db);
			 String sql="select min(lat),min(lng),max(lat),max(lng),count(distinct  id) from "+table
			 + " where time>time(\""+timeStart+"\") and time<time(\""+timeEnd+"\")";
			 
			 exeSQL(sql);
			 
			 while(rs.next()){
				 res[0]=rs.getDouble(1);
				 res[1]=rs.getDouble(2);
				 res[2]=rs.getDouble(3);
				 res[3]=rs.getDouble(4);
				 res[4]=rs.getInt(5);
			 }
			 closeDB();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	
	//important, by time order
	public static void loadWxgVideoTraDB(String table, int timeStart,int timeEnd){
		String sql= "select * from "+
		 table+ " where t>"+timeStart+" and t<"+timeEnd+" order by t asc";
		exeSQL(sql);
	}
	
	//by time order
	public static void loadBBFOld(String table, int timeStart, int timeEnd){
		String sql= "select id,seq,x,y,t from "+
		 table+ " where t>"+timeStart+" and t<"+timeEnd+" order by t asc";
		exeSQL(sql);
	}
	
	public static int[] getMITTraStartEndId(String db,String table, int timeStart, int timeEnd){
		int[] res= new int[2];;
		try {
			openDB( db);
			 String sql="select min(id),max(id) from "+table
			 + " where t>"+timeStart+" and t<"+timeEnd+"";
			 
			 exeSQL(sql);
			
			 while(rs.next()){
				 res[0]=rs.getInt(1);
				 res[1]=rs.getInt(2);
			 }
			 closeDB();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			res=null;
			e.printStackTrace();
		}
		
		
		return res;
	}
	
	public static int getSeconds(String str){

		String[] resStr=str.split(":|,");
		int h=Integer.parseInt(resStr[0]);
		int m=Integer.parseInt(resStr[1]);
		int s=Integer.parseInt(resStr[2]);
		
		return h*3600+m*60+s;
	}
	
	
	

	
	public static void main(String[] args) throws Exception {
		
		
	}
		
	
}

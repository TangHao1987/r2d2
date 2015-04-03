package utl.serialization;

import grid.GridLeafTraHashItem;


import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class BinarySerializer {
	 public static byte[] getByteSerialize(Object obj) throws IOException {
	        ByteArrayOutputStream b = new ByteArrayOutputStream();
	        ObjectOutputStream o = new ObjectOutputStream(b);
	        o.writeObject(obj);
	        return b.toByteArray();
	    }
	 
	 public static Object getByteDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
	        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
	        ObjectInputStream o = new ObjectInputStream(b);
	        return o.readObject();
	    }
	 
	 public static void   testTime(){

		 
		 Hashtable<Integer,GridLeafTraHashItem> ht=new Hashtable<Integer,GridLeafTraHashItem>();
		 
		 for(int i=0;i<10;i++){
			 GridLeafTraHashItem pi=new GridLeafTraHashItem(i*i,i*i);
			 ht.put(i,pi);
		 }
		 try{
		 long start=System.currentTimeMillis();
		 for(int j=0;j<100;j++){
			 byte[] x=BinarySerializer.getByteSerialize(ht);
			 
			 Object obj=BinarySerializer.getByteDeserialize(x);
			 
			 ht=(Hashtable<Integer,GridLeafTraHashItem>) obj;
			 
		 }
		 long end=System.currentTimeMillis();
		 long ave=(end-start)/100;
		 
		 for(int i=0;i<10;i++){
			 GridLeafTraHashItem temp=ht.get(i);
			 System.out.println(temp.getCellX()+" "+temp.getCellY());
		 }
		 
		 System.out.println("the time is " +ave+" ms");
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		 
	 }
	 
	
	 public static void testLinkedMapSize(){
		LinkedHashMap<Long,GridLeafTraHashItem> ht=new LinkedHashMap<Long,GridLeafTraHashItem>();
		
		for(int i=50000;i<50135;i++){
			GridLeafTraHashItem pi=new GridLeafTraHashItem(i*i,i*i);
			ht.put(new Long(i<<32+i*i), pi);
		}
		
		 try{
			 byte[] ht_a=BinarySerializer.getByteSerialize(ht);
		
			 System.out.println("linkedhashmap size is:"+ht_a.length+" byte");
			
			 }catch(Exception e){
				 e.printStackTrace();
			 }
	 }
	 
	 public static void main(String[] args){
		 testLinkedMapSize();
	 }
}



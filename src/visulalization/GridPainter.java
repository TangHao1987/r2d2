package visulalization;

import grid.Grid;
import grid.GridCell;
import grid.GridLeafTraHashItem;
import grid.RoICell;
import grid.RoIState;
import grid.TraListItem;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JFrame;

import prediction.TimeTraState;

import traStore.TraStoreListItem;

@SuppressWarnings("serial")
public class GridPainter extends Canvas{
	private int width=800;
	private int height=800;
	private Grid visGid=null;
	private int gridX0;
	private int gridX1;
	private int gridY0;
	private int gridY1;
		

	private boolean drawMap=true;
	private boolean drawPath=true;
	
	private ArrayList<Point> path;
	private ArrayList<Integer> pathRadius;
	
	private  int      lastMouseX;
	private  int      lastMouseY;
	
	private  Graphics2D context; 
	Color timeColorSet[];
	int timeColorNum;
	public GridPainter(Grid g,int inGridX0,int inGridY0,int inGridX1,int inGridY1){
		visGid=g;
		
		gridX0=inGridX0;
		gridY0=inGridY0;
		gridX1=inGridX1;
		gridY1=inGridY1;
		
		width=gridX1-gridX0;
		height=gridY1-gridY0;
		
		lastMouseX=-1;
		lastMouseY=-1;
		
		path=new ArrayList<Point>();
		pathRadius=new ArrayList<Integer>();
		
		 this.enableEvents( AWTEvent.MOUSE_EVENT_MASK| AWTEvent.MOUSE_MOTION_EVENT_MASK );   
		 
		 timeColorNum=10;
		 timeColorSet=new Color[timeColorNum];
		 timeColorSet[0]=Color.RED;
		 timeColorSet[1]=Color.BLUE;
		 timeColorSet[2]=Color.YELLOW;
		 timeColorSet[3]=Color.GREEN;
		 timeColorSet[4]=Color.PINK;
		 timeColorSet[5]=Color.CYAN;
		 timeColorSet[6]=Color.ORANGE;
		 timeColorSet[7]=Color.MAGENTA;
		 timeColorSet[8]=Color.WHITE;
		 timeColorSet[9]=Color.DARK_GRAY;
	}
	
	 public void addNotify() { 
		 super.addNotify();   
		 context = (Graphics2D)this.getGraphics().create();
     } // End addNotify.
	 
	 public void paint(Graphics g) {
	      		 
		 g.setColor(Color.white);
		 g.fillRect(0, 0, width,height);
	    if(drawMap){
		 for(int x=gridX0;x<gridX1;x++){
			 for(int y=gridY0;y<gridY1;y++){
				 GridCell gc=visGid.getGridCell(x, y);
				 if(gc!=null){
					 double d=gc.density;
					 int l=255-(int)(d/0.04);
					 if(l<0) l=0;
					 g.setColor(new Color(l,l,l));
					 g.drawLine(x-gridX0, y-gridY0, x-gridX0, y-gridY0);
				 }
				 
			 }
		 }
		 
		 if(this.drawPath){
			 if(null!=path){
				 for(int i=0;i<path.size();i++){
					 context.setColor(timeColorSet[i%timeColorNum]);
					 drawCircle(context,path.get(i).x,path.get(i).y,pathRadius.get(i));
				 }
			 }
		 }
	    }
		 
		 
		 
	 }
	 
	 private void drawX(Graphics2D inContex,int x, int y,int r){
		 inContex.drawLine(r+x, r+y,
				  -r+x,-r+y); 
		 inContex.drawLine(r+x, -r+y,
				  -r+x,r+y); 
		
	 }
	 
	 private void drawX(Graphics2D inContex,int x, int y){
		 drawX(inContex,x,y,2);
	 }
	 
	 private void drawCircle(Graphics inContext,int x,int y){
		 drawCircle(inContext,x,y,7);
	 }
	 
	private void drawCircle(Graphics inContext,int x,int y, int r){
		inContext.fillOval(x-r/2, y-r/2, r, r);
	}
	 private void drawXCircle(Graphics2D inContex,Point obj){
		 drawXCircle(inContex,obj,5);
	 }
	 private void drawXCircle(Graphics2D inContex,Point obj,int r){
		 Stroke defaultStroke=inContex.getStroke();
		 Color defaultColor=inContex.getColor();
		 inContex.setColor(Color.BLACK);
		 inContex.setStroke(new BasicStroke(2));
		 inContex.drawOval(obj.x-r, obj.y-r, 2*r, 2*r);
		 drawX(inContex,obj.x,obj.y,5);
		 inContex.setStroke(defaultStroke);
		 inContex.setColor(defaultColor);
	 }
		 
	 public static GridPainter visGridPart(Grid grid,int x0,int y0, int x1,int y1){
		 GridPainter canvas=new GridPainter(grid,x0,y0,x1,y1);
		 JFrame frame=new JFrame();
		 
		 frame.setSize(x1-x0,y1-y0);
		  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  frame.getContentPane().add(canvas);
		  frame.setVisible(true);
		  return canvas;
	 }
	 
	 protected void processMouseEvent(MouseEvent e){  
	        if(e.getID()==MouseEvent.MOUSE_PRESSED){  
	        	lastMouseX=e.getX();
	        	lastMouseY=e.getY();
	           // System.out.println("x:"+e.getX()+" y:"+e.getY());
	          //repaint();
	        	context.setColor(new Color(255,0,0));
	        	context.drawString("x:"+lastMouseX+" y:"+lastMouseY, lastMouseX, lastMouseY);
	            }  
	        else if(e.getID()==MouseEvent.MOUSE_RELEASED){  
	        	
	            } 
	      
	        }  
	 protected void processMouseMotionEvent( MouseEvent event) { 
		     
		      if ( event.getID() == MouseEvent.MOUSE_DRAGGED) { 
		         int currentX = event.getX();
		         int currentY = event.getY();
		         
		            context.drawLine( lastMouseX, lastMouseY, currentX, currentY); 
		            lastMouseX = currentX;
		            lastMouseY = currentY;  
		            repaint();
		        } // End if.  
		      else if(event.getID()==MouseEvent.MOUSE_WHEEL){
		    	  repaint();
		      }
		     
		      } // End processMouseMotionEvent.

	
     
   
	 
     public void setDrawMap(boolean d){
    	 drawMap=d;
     }
     
     public void setDrawPathOn(ArrayList<Point> inPath,ArrayList<Integer> radius){
    	 this.drawPath=true;
    	this.path=inPath;
    	this.pathRadius=radius;
    	
    	 repaint();
     }
     public void setDrawPathOff(){
    	 if(null!=path){
    	 this.path.clear();
    	 this.pathRadius.clear();
    	 }
    	 this.drawPath=false;
    	 
    	 repaint();
     }
	 public static void main(String[] args)
	    {//(Grid grid,int x0,int y0, int x1,int y1){
		 Grid g=new Grid();
		 visGridPart(g,0,0,400,400);
	    }
}

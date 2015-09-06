package visulalization;

import grid.Grid;
import grid.GridCell;
import grid.GridLeafTraHashItem;
import grid.RoICell;
import prediction.TimeTraState;
import traStore.TraStoreListItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class VisGridCanvas extends Canvas {
    public static double densityUnit = 0.1;
    private int width = 800;
    private int height = 800;
    private Grid visGid = null;
    private int gridX0;
    private int gridX1;
    private int gridY0;
    private int gridY1;

    private double lat0 = 0.0;
    private double lng0 = 0.0;
    private double step = 0.0;

    Hashtable<Integer, ArrayList<TraStoreListItem>> traSet = null;
    TimeTraState timeTraState = null;
    int[] MAPPath = null;
    double moveObjLat = -1;
    double moveObjLng = -1;

    ArrayList<Entry<Long, GridLeafTraHashItem>> traGrid = null;

    private int lastMouseX;
    private int lastMouseY;
    private Graphics2D context;
    private Color timeColorSet[];
    private int timeColorNum;

    public VisGridCanvas(Grid g, int inGridX0, int inGridY0, int inGridX1, int inGridY1) {
        visGid = g;
        gridX0 = inGridX0;
        gridY0 = inGridY0;
        gridX1 = inGridX1;
        gridY1 = inGridY1;

        width = gridX1 - gridX0;
        height = gridY1 - gridY0;

        lastMouseX = -1;
        lastMouseY = -1;

        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        timeColorNum = 10;
        timeColorSet = new Color[timeColorNum];
        timeColorSet[0] = Color.RED;
        timeColorSet[1] = Color.BLUE;
        timeColorSet[2] = Color.YELLOW;
        timeColorSet[3] = Color.GREEN;
        timeColorSet[4] = Color.PINK;
        timeColorSet[5] = Color.CYAN;
        timeColorSet[6] = Color.ORANGE;
        timeColorSet[7] = Color.MAGENTA;
        timeColorSet[8] = Color.WHITE;
        timeColorSet[9] = Color.DARK_GRAY;

        JFrame frame = new JFrame();
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.setVisible(true);
    }

    public void addNotify() {
        super.addNotify();
        context = (Graphics2D) this.getGraphics().create();
    } // End addNotify.

    public void paint(Graphics g) {

        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);

        for (int x = gridX0; x < gridX1; x++) {
            for (int y = gridY0; y < gridY1; y++) {
                GridCell gc = visGid.getGridCell(x, y);
                if (gc != null) {
                    double d = gc.density;
                    int l = 255 - (int) (d / densityUnit);
                    if (l < 0) l = 0;
                    g.setColor(new Color(l, l, 255));
                    g.drawLine(x - gridX0, y - gridY0, x - gridX0, y - gridY0);
                    g.fillOval(x-gridX0,  y-gridY0, 20, 20);
                }

            }

        }
        if (traSet != null) {

            // g.setColor(new Color(255,0,0));
            // g.drawLine(0, 0, 50,50);
            context.setColor(new Color(255, 0, 0));
            Enumeration<ArrayList<TraStoreListItem>> traElements = traSet.elements();
            Enumeration<Integer> traKeys = traSet.keys();
            // context.drawLine((int)(0), (int)(0),
            //	  (int)(200),(int)(200));
            int colorSwitch = 0;
            while (traElements.hasMoreElements() && traKeys.hasMoreElements()) {
                ArrayList<TraStoreListItem> itemRes = traElements.nextElement();
                int itemTraId = traKeys.nextElement();
                // System.out.println("trajectory id is:"+itemTraId);

                Color colorItem = timeColorSet[colorSwitch % timeColorNum];
                context.setColor(colorItem);
                for (int i = 0; i < itemRes.size(); i++) {
                    TraStoreListItem offItem = itemRes.get(i);
                    if (i == 0) {
                        context.setColor(Color.BLACK);
                        context.drawString("traid:" + itemTraId + " off_set:" + offItem.off,
                                5 + (int) ((offItem.lat - lat0) / step), 5 + (int) ((offItem.lng - lng0) / step));
                        context.setColor(colorItem);
                    }

                    //	  System.out.print("<lat:"+offItem.lat+" lng:"+offItem.lng+" time:"+offItem.timestamp+"> ");
                    context.drawLine(2 + (int) ((offItem.lat - lat0) / step), 2 + (int) ((offItem.lng - lng0) / step),
                            -2 + (int) ((offItem.lat - lat0) / step), -2 + (int) ((offItem.lng - lng0) / step));
                    context.drawLine(2 + (int) ((offItem.lat - lat0) / step), -2 + (int) ((offItem.lng - lng0) / step),
                            -2 + (int) ((offItem.lat - lat0) / step), 2 + (int) ((offItem.lng - lng0) / step));

                }
                colorSwitch++;
                //  System.out.println();
            }
        } else if (null != timeTraState && null == MAPPath) {
            context.setColor(new Color(255, 0, 0));

            for (int k = 1; k < timeTraState.getTimeLength() + 1; k++) {
                for (int i = 0; i < timeTraState.getStateNum(k); i++) {
                    HashSet<RoICell> roiSet = timeTraState.getState(k, i).roiSet;
                    for (RoICell rc : roiSet) {
                        Color colorItem = timeColorSet[(k - 1) % timeColorNum];
                        context.setColor(colorItem);
                        context.drawLine(rc.roiX, rc.roiY, rc.roiX, rc.roiY);
                    }
                }
            }
        } else if (null != timeTraState) {
            for (int i = 1; i < MAPPath.length; i++) {
                Color colorItem = timeColorSet[(i - 1) % timeColorNum];
                context.setColor(colorItem);
                int x = (int) (timeTraState.getState(i, MAPPath[i]).getCenterX() + 0.5);
                int y = (int) (timeTraState.getState(i, MAPPath[i]).getCenterY() + 0.5);
                drawCircle(context, x, y);
                //	System.out.println("time:"+i+" pos x:"+timeTraState.getState(i,MAPPath[i]).getCenterX()+" y:"+timeTraState.getState(i, MAPPath[i]).getCenterY());
            }
        } else if (null != traGrid) {
            context.setColor(new Color(255, 0, 0));
            for (Entry<Long, GridLeafTraHashItem> item : traGrid) {
                drawX(context, item.getValue().getCellX(), item.getValue().getCellY(), 0);
            }
        }

        if (-1 != moveObjLat && -1 != moveObjLng) {
            paintMoveObj(context, moveObjLat, moveObjLng);
        }
    }

    private void drawX(Graphics2D inContex, int x, int y, int r) {
        inContex.drawLine(r + x, r + y,
                -r + x, -r + y);
        inContex.drawLine(r + x, -r + y,
                -r + x, r + y);

    }

    private void drawCircle(Graphics inContext, int x, int y) {
        inContext.fillOval(x, y, 7, 7);
    }

    private void paintMoveObj(Graphics2D inContext, double lat, double lng) {
        int x = (int) ((lat - lat0) / step);
        int y = (int) ((lng - lng0) / step);
        drawXCircle(inContext, new Point(x, y));
    }

    private void drawXCircle(Graphics2D inContex, Point obj) {
        drawXCircle(inContex, obj, 5);
    }

    private void drawXCircle(Graphics2D inContex, Point obj, int r) {
        Stroke defaultStroke = inContex.getStroke();
        Color defaultColor = inContex.getColor();
        inContex.setColor(Color.BLACK);
        inContex.setStroke(new BasicStroke(2));
        inContex.drawOval(obj.x - r, obj.y - r, 2 * r, 2 * r);
        drawX(inContex, obj.x, obj.y, 5);
        inContex.setStroke(defaultStroke);
        inContex.setColor(defaultColor);
    }

    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            context.setColor(new Color(255, 0, 0));
            context.drawString("x:" + lastMouseX + " y:" + lastMouseY, lastMouseX, lastMouseY);
        }
    }

    protected void processMouseMotionEvent(MouseEvent event) {

        if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
            int currentX = event.getX();
            int currentY = event.getY();
            context.drawLine(lastMouseX, lastMouseY, currentX, currentY);
            lastMouseX = currentX;
            lastMouseY = currentY;
            repaint();
        } // End if.
        else if (event.getID() == MouseEvent.MOUSE_WHEEL) {
            repaint();
        }

    } // End processMouseMotionEvent.

}

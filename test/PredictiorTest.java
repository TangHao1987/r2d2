import org.junit.*;
import grid.Configuration;
import grid.Grid;
import grid.GridLeafTraHashItem;

import prediction.AgglomerativeCluster;
import prediction.MacroState;
import prediction.MicroState;
import prediction.StatesDendrogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PredictiorTest {


    @Test
    public void test(){
        Grid g=new Grid();
        AgglomerativeCluster ac=new AgglomerativeCluster(g);
        HashMap<Long,GridLeafTraHashItem> hm=new HashMap<>();

        GridLeafTraHashItem gti[]=new GridLeafTraHashItem[27];
        gti[0]=new GridLeafTraHashItem(2,2);
        gti[1]=new GridLeafTraHashItem(2,3);
        gti[2]=new GridLeafTraHashItem(2,4);
        gti[3]=new GridLeafTraHashItem(2,5);
        gti[4]=new GridLeafTraHashItem(3,2);
        gti[5]=new GridLeafTraHashItem(3,3);
        gti[6]=new GridLeafTraHashItem(3,4);
        gti[7]=new GridLeafTraHashItem(3,5);
        gti[8]=new GridLeafTraHashItem(4,2);
        gti[9]=new GridLeafTraHashItem(4,3);
        gti[10]=new GridLeafTraHashItem(4,4);
        gti[11]=new GridLeafTraHashItem(4,5);
        gti[12]=new GridLeafTraHashItem(5,2);
        gti[13]=new GridLeafTraHashItem(5,3);
        gti[14]=new GridLeafTraHashItem(5,4);
        gti[15]=new GridLeafTraHashItem(5,5);

        gti[16]=new GridLeafTraHashItem(9,3);
        gti[17]=new GridLeafTraHashItem(10,3);
        gti[18]=new GridLeafTraHashItem(10,4);
        gti[19]=new GridLeafTraHashItem(10,5);
        gti[20]=new GridLeafTraHashItem(10,6);
        gti[21]=new GridLeafTraHashItem(10,7);
        gti[22]=new GridLeafTraHashItem(9,7);

        gti[23]=new GridLeafTraHashItem(3,7);
        gti[24]=new GridLeafTraHashItem(4,8);
        gti[25]=new GridLeafTraHashItem(5,9);
        gti[26]=new GridLeafTraHashItem(6,10);

        double sumx=0,sumy=0;

        for(int i=0;i<27;i++){
            Long key= Configuration.getKey(Configuration.getStateId(), i);
            hm.put(key, gti[i]);

        }

        int start=16,end=26;

        for(int i=start;i<=end;i++){
            sumx+=gti[i].getCellX();
            sumy+=gti[i].getCellY();
        }
        sumx/=(end-start+1);
        sumy/=(end-start+1);

        double radius=0;
        for(int i=start;i<=end;i++){
            double x=gti[i].getCellX();
            double y=gti[i].getCellY();

            double r=(x-sumx)*(x-sumx)+(y-sumy)*(y-sumy);

            radius+=r;
        }

        System.out.println(""+Math.sqrt(radius/27));


        ArrayList<Map.Entry<Long,GridLeafTraHashItem>> query=new ArrayList<>(hm.entrySet());

        ArrayList<MicroState> res=ac.InitialMicroState(query, Math.sqrt(2)*2);
        System.out.println(res.get(0).getDisCenter(res.get(1)));
        System.out.println(res.get(1).getDisCenter(res.get(2)));

        System.out.println(res.get(0).getDisCenter(res.get(2)));

        System.out.println(MacroState.getRadius(res.get(0), res.get(1)));
        System.out.println(MacroState.getRadius(res.get(1), res.get(2)));

        query=new ArrayList<>(hm.entrySet());

        //ArrayList<ArrayList<MacroState>> macRes=ac.getDendrogram(query, 2.58/2);
        StatesDendrogram sd=ac.getDendrogram(query, 2.59/4);
        System.out.println(sd.getMacsTree().size());
    }
}

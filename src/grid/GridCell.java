package grid;

import java.io.Serializable;
import java.util.ArrayList;

import storagemanager.IStorageManager;

public class GridCell implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115911727212467856L;
	public double density;
	 public ArrayList<TraListItem> traList;

	public int level;
	public GridCell[][] gridArray = null;
	public GridLeafEntry gridLeafEntry = null;

	public GridCell(int inLevel, IStorageManager inStorageManager) {
		density = 0;
		traList=null;
		gridArray = null;
		level = inLevel;
		if (level > 0) {
			initialGrid();
		} else if (level == 0) {// if it is the leaf level, create a leaf entry.
			gridLeafEntry = new GridLeafEntry(inStorageManager);
		}
		// level=0;
	}

	private void initialGrid() {
		int w = (int) Math.pow(2, Configuration.BITS_PER_GRID);
		int l = w;
		gridArray = new GridCell[w][l];

		for (int i = 0; i < w; i++) {
			gridArray[i] = new GridCell[l];
		}

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < l; j++) {
				gridArray[i][j] = null;
			}
		}
	}

	public int getSizeOf() {
		int size=-1;
		int leafSize=0;
		if(null!=gridLeafEntry){
			leafSize=gridLeafEntry.getSizeOf();
		}
		int arraySize=0;
		if(null!=this.gridArray){
			
			int w = (int) Math.pow(2, Configuration.BITS_PER_GRID);
			int l = w;
			
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < l; j++) {
					if(null!=gridArray[i][j]){
						arraySize+=gridArray[i][j].getSizeOf();
					}
				}
			}
		}
		
		int othersizeof=8+4+4+4;
		
		size=leafSize+arraySize+othersizeof;
		
		return size;
	}

}

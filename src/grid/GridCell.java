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
        gridArray = new GridCell[w][w];

		for (int i = 0; i < w; i++) {
			gridArray[i] = new GridCell[w];
		}

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < w; j++) {
				gridArray[i][j] = null;
			}
		}
	}

}

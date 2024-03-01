package Symbol_table.Symbols;

import java.util.ArrayList;

public class ArraySymbol extends NorSymbol {
    public int level;
    public int level1=0;
    public int level2=0;
    public ArrayList<Integer> values;

    public ArraySymbol(String name, boolean isConst) {
        super(name, isConst);
    }

    public ArraySymbol(String name,boolean isConst,int level,int level2) {
        super(name,isConst);
        this.level=level;
        this.level2=level2;
    }

    public ArraySymbol(String name, int offset) {
        super(name, offset);
    }

    public ArraySymbol(String name, int offset, boolean ispointer) {
        super(name, offset, ispointer);
    }

    public ArraySymbol(String name, boolean isConst, int level) {
        super(name, isConst, level);
    }

    public ArraySymbol(String name, boolean isConst, int level, ArrayList<Integer> values) {
        super(name, isConst);
        this.level = level;
        this.values = values;
    }

    public ArraySymbol(String name, boolean isConst, int level, int level2, ArrayList<Integer> values) {
        super(name, isConst);
        this.level = level;
        this.level2 = level2;
        this.values = values;
    }
}

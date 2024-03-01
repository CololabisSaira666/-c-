package Symbol_table.Symbols;

public class NorSymbol {
    public String name;
    public boolean isConst;
    public int level = 0;
    public int line = 0;
    public int offset=0;
    public boolean ispointer = false;

    public NorSymbol() {

    }

    public NorSymbol(String name, boolean isConst) {
        this.name = name;
        this.isConst=isConst;
    }
    public NorSymbol(String name, boolean isConst,int level) {
        this.name = name;
        this.isConst=isConst;
        this.level=level;
    }

    public NorSymbol(String name, int offset) {
        this.name = name;
        this.offset = offset;
    }
    public NorSymbol(String name, int offset, boolean ispointer) {
        this.name = name;
        this.offset = offset;
        this.ispointer=ispointer;
    }
}

package Symbol_table;

import Symbol_table.Symbols.FuncSymbol;
import Symbol_table.Symbols.NorSymbol;

import java.util.HashMap;

public class IntergerTable {
    public HashMap<String, NorSymbol> maps = new HashMap<>();
    public IntergerTable out = null;
    public int contentlength = 0;

    public IntergerTable() {

    }

    public IntergerTable(IntergerTable out) {
        this.out = out;
    }
}

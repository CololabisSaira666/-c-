package Symbol_table;

import Symbol_table.Symbols.FuncSymbol;
import Symbol_table.Symbols.NorSymbol;

import java.util.HashMap;

public class FuncTable {
    public HashMap<String, FuncSymbol> maps = new HashMap<>();
    public FuncTable out;

    public FuncTable() {
    }
}

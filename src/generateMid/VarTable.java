package generateMid;

import java.util.HashMap;

public class VarTable {
    HashMap<String, NewSym> table;
    int id;
    int preId;
    public VarTable(int preId, int id) {
        this.preId = preId;
        this.id = id;
        this.table = new HashMap<>();
    }

    public void p() {
        for (String key : table.keySet()) {
            System.out.println("Key = " + key);
        }
    }

    public void addVarTable(String n, NewSym sym) {
        this.table.put(n, sym);
    }

    public NewSym getSym(String n) {
        return this.table.get(n);
    }

    public boolean haveSym(String n) {
        if (this.table.containsKey(n)) {
            return true;
        } else {
            return false;
        }
    }
}

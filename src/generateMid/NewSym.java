package generateMid;

public class NewSym {
    String name;
    int type;
    int col;
    int value;
    public NewSym(String n, int type) {
        this.name = n;
        this.type = type;
    }

    public NewSym(String n, int type, int c) {
        this.name = n;
        this.type = type;
        this.col = c;
    }
}

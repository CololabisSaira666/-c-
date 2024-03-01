package error;

public class Errors implements Comparable<Errors>{
    private char errorType;
    private int errorLine;

    public Errors(char t, int l) {
        this.errorType = t;
        this.errorLine = l;
    }

    public char getType() {
        return this.errorType;
    }

    public int getErrorLine() {
        return this.errorLine;
    }

    @Override
    public String toString() {
        return errorLine + " " + errorType + "\n";
    }

    @Override
    public int compareTo(Errors e) {
        return this.errorLine - e.errorLine;
    }
}

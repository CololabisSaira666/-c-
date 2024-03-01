
import error.Errors;
import generateMid.GenerateMid;
import generateMid.MidCode;
import lexer.Lexer;
import mips.Mips;
import parser.Parser;
import symbols.FindBugs;
import tree.Node;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Compiler {
    public static void main(String args[]) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader("testfile.txt"));
        BufferedWriter parseout = new BufferedWriter(new FileWriter("output.txt"));
        BufferedWriter errs = new BufferedWriter(new FileWriter("error.txt"));
        BufferedWriter mips = new BufferedWriter(new FileWriter("mips.txt"));
        Lexer lexer = new Lexer(in);
        Parser parser = new Parser(lexer);
        Node tree = parser.parserRun();
        // parseout.close();
        // 错误处理
        FindBugs errorHandling = new FindBugs(tree);
        errorHandling.analyse();
        List<Errors> allbug = new ArrayList<>();
        allbug.addAll(parser.errorList);
        allbug.addAll(errorHandling.bugs);
        Collections.sort(allbug);
        //System.out.println(allbug.size());
        if(allbug.size() != 0) {
            for (Errors i : allbug) {
                //System.out.print(i.toString());
                errs.write(i.toString());
            }
        } else {
            // 中间代码生成，这里先判断前面有没有错误，没有了再处理？
            GenerateMid mid = new GenerateMid(tree);
            mid.GenerateRun();
            //System.out.println(mid.codes.size());
            //ArrayList<MidCode> test = new ArrayList<>(mid.codes.subList(0,17));
            for (MidCode i : mid.codes) {
                //parseout.write(i.putAll() + "\n");
                parseout.write(i.toString() + "\n");
                //System.out.println(i.putAll());
            }
            parseout.close();
            // mips 生成
            Mips mipsCode = new Mips(mid.codes, mid.strings);
            mipsCode.printCodes(mips);
            in.close();

        }

        mips.close();
        errs.close();
    }
}

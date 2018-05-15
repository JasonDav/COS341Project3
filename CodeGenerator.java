import jdk.jfr.TransitionFrom;

import java.io.File;
import java.io.PrintWriter;
import java.net.Inet4Address;

public class CodeGenerator {

    static int labels = -1;
    SemanticTable st;
    SemanticTable.TableEntry te;
    int index = 0;
    String code = "";
    public CodeGenerator(SemanticTable semtable)
    {
        st = semtable;
        te = next();

        while ((te = next())!=null)
            code += TransStat();

        System.out.println("_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_\n" +
                "\t\tBasic Code\n" +
                "_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_\n"+code);

        try {
            PrintWriter writer = new PrintWriter(new File("intermediateCode.bas"));
            writer.write(code+"\n");
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    private String TransStat()
    {
        String output = "";
        switch (te.node.type)
        {
            case IO:
                return TransIO();

            case ASSIGN:
                    return TransAssign();

            case COND_BRANCH:
                return TransIF();

            case CONDLOOP:
                return TransLoop();

            case INSTR:
                    return TransHALT();

            case NAME:
//                return TransProc();

            case PROC:
//                return TransProcDefs();
        }
        return "";
    }

    private String TransLoop() {

        String out = newLabel()+" IF (NOT ("; ///reverse if loop to simplify construction

        out += TransBool()+ ")) THEN GOTO ";//GOTO false-else label

//        te = next();
        String thenPart = TransLoopBody(te.node.data);

        Integer thenLabel = labels+1;//else start

        out+=thenLabel+"\n";
        out+=thenPart+"\n";

        return out;

    }

    private String TransIF() {

        String out = newLabel()+" IF (NOT ("; ///reverse if loop to simplify construction

        out += TransBool()+ ")) THEN GOTO ";//GOTO false-else label

        String thenPart = TransThen();

        if(te.node.data.equals("else"))
        {
            Integer thenLabel = labels+2;//else start

            out+=thenLabel+"\n";
            out+=thenPart;
            out+=newLabel()+" GOTO ";

            String elsePart = TransElse();
            Integer endOfIfLabel = labels+1;

            out+=+endOfIfLabel+"\n";
            out+=elsePart;

            return out;
        }
        else
        {
            Integer thenLabel = labels+1;//else start

            out+=thenLabel+"\n";
            out+=thenPart;

            return out;
        }
    }

    private String TransThen() {

        String out = "";

        te = next();//then

        while((te=next()).node.parent.data.equals("then"))
        {
            out+=TransStat();
        }

        return out;
    }

    private String TransLoopBody(String loopType) {

        String out = "";

//        te = next();//loop header

        while((te=next()).node.parent.data.equals(loopType))
        {
            out+=TransStat();
        }

        return out;
    }

    private String TransElse() {
        String out = "";

        //while still in else
        while((te=next()).node.parent.data.equals("else"))
        {
            out+=TransStat();
        }

        index--;

        return out;
    }

    private String TransHALT() {
        return newLabel() + " END";
    }

    private String TransAssign() {
        String temp = newLabel()+" ";

        te = next();

        if(te.varType.equals("S"))
        {//string
            temp += "LET " + te.name + "$ = ";
            te = next();

            temp+=te.name;
        }
        else
        {//bool or number
            temp+="LET "+te.name+" = ";
            te = next();

            if(te.node.type == NodeType.CALC)
                temp+=" "+ TransCalc();
            else
                temp+=te.name;
        }

        return temp+"\n";

    }

    private String TransBool() {

        String boolString = "";

        te=next();

        if(te.decl_id!=null)
            return te.name;

        switch (te.name)
        {
            case "<":
                boolString = "<";
                break;

            case ">":
                boolString = ">";
                break;

            case "eq":
                boolString = "=";
                break;

            case "or":
                boolString = "OR";
                break;

            case "and":
                boolString = "AND";
                break;
        }

        String out = "";
        return TransBool() +" "+boolString+" "+ TransBool();

    }

    private String TransCalc() {

        String out = " "+te.name+" ";
        te = next();

        //first param - is another calc
        if(te.node.type == NodeType.CALC)
            out = "("+TransCalc()+")" + out;
        else
            out = te.name + out;

        te = next();//next param

        //second param - is another calc
        if(te.node.type == NodeType.CALC)
            out += "("+TransCalc()+")";
        else
            out += te.name;

        return out;
    }

    private String TransIO() {

        String temp = newLabel()+" ";
        String vartype = "";
        if(te.node.data.equals("input"))
        {
            te = next();
            if(te.varType.equals("S"))
                vartype ="$";

            temp+="INPUT\""+te.node.data+"\";"+te.name+vartype;
        }
        else
        {
            te = next();
            if(te.varType.equals("S"))
                temp+="PRINT $"+te.name;
            else
                temp+="PRINT "+te.name;
        }

        return temp+"\n";
    }

    private SemanticTable.TableEntry next()
    {
        if(index<st.entries.size())
            return st.entries.get(index++);
        else
            return null;
    }

    private boolean isLast(SemanticTable.TableEntry te)
    {
        return st.entries.getLast()==te;
    }

    private int newLabel()
    {
        labels+=1;
        return labels;
    }

    private int getLabel()
    {
        return labels;
    }

    private SemanticTable.TableEntry next(SemanticTable.TableEntry te)
    {
        if(te.t_id<st.entries.size())
            return st.getEntry(te.t_id+1);
        else
            return null;//done
    }
}

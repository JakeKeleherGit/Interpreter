import java.io.*;
import java.util.*;

public class Interpreter 
{

    // stores one parsed instruction from the bytecode
    // op is the command name and parts holds the rest of the tokens
    static class Command 
    {
        String op;
        ArrayList<String> parts;

        Command(String op) 
        {
            this.op = op;
            this.parts = new ArrayList<String>();
        }

        public String toString() 
        {
            StringBuilder sb = new StringBuilder(op);
            for (String p : parts) 
            {
                sb.append(" ").append(p);
            }
            return sb.toString();
        }
    }
    // class for vector3 registers
    // each vector has rgb float values
    static class Vector3 
    {
        double r;
        double g;
        double b;

        Vector3() 
        {
            this(0, 0, 0);
        }

        Vector3(double r, double g, double b) 
        {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        // Makes a seprate copy so assigning one vector to another
        // does not make them point to the same object
        Vector3 copy() 
        {
            return new Vector3(r, g, b);
        }

        public boolean equals(Object obj) 
        {
            if (!(obj instanceof Vector3)) 
            {
                return false;
            }
            Vector3 other = (Vector3) obj;
            return Double.compare(r, other.r) == 0
                    && Double.compare(g, other.g) == 0
                    && Double.compare(b, other.b) == 0;
        }
    }
    
    // holds the parsed program
    // commands is the list of instructions
    // labels maps each label name to its command index
    static class Program 
    {
        ArrayList<Command> commands = new ArrayList<Command>();
        HashMap<String, Integer> labels = new HashMap<String, Integer>();
    }
    
    // types stores the declared type of each register name
    // values stores the actual current value of each register
    static HashMap<String, String> types = new HashMap<String, String>();
    static HashMap<String, Object> values = new HashMap<String, Object>();

    public static void main(String[] args) 
    {
        // the filename must be passed in when the program runs
        if (args.length == 0) {
            System.out.println("Usage: java Interpreter sample.txt");
            return;
        }

        try 
        {
            // first, i need to break the input file into tokens
            ArrayList<String> tokens = tokenizeFile(args[0]);
            
            // next, i need to turn the tokens into command objects
            Program program = parseProgram(tokens);
            
            // lastly, i need to run the program command by command
            runProgram(program);
        } 
        catch (Exception e) 
        {
            System.out.println("Runtime error: " + e.getMessage());
        }
    }

    static ArrayList<String> tokenizeFile(String filename) throws IOException 
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        
        // read the whole file into one big string
        // this works even if the entire bytecode is on one line
        while ((line = br.readLine()) != null) 
        {
            sb.append(line).append(' ');
        }
        br.close();
      
        ArrayList<String> tokens = new ArrayList<String>();
        String text = sb.toString();
        int i = 0;
        
        // split by whitespace, but keep quoted strings together
        while (i < text.length()) 
        {
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) 
            {
                i++;
            }

            if (i >= text.length()) 
            {
                break;
            }
            
            // if a token starts witha quote, read until the closing quote.
            if (text.charAt(i) == '"') 
            {
                int j = i + 1;
                while (j < text.length() && text.charAt(j) != '"') 
                {
                    j++;
                }
                tokens.add(text.substring(i, j + 1));
                i = j + 1;
            } 
            else 
            {
                int j = i;
                while (j < text.length() && !Character.isWhitespace(text.charAt(j))) 
                {
                    j++;
                }
                tokens.add(text.substring(i, j));
                i = j;
            }
        }

        return tokens;
    }

    static Program parseProgram(ArrayList<String> tokens) 
    {
        Program program = new Program();
        int i = 0;
         
        // walk through the token list andbuild Command objects 
        while (i < tokens.size()) 
        {
            String token = tokens.get(i);

            if (token.equals("function")) 
            {
                Command cmd = new Command("function");
                cmd.parts.add(tokens.get(i + 1));
                i += 2;
                if (tokens.get(i).equals("params")) 
                {
                    i++;
                    while (!tokens.get(i).equals("endparams")) 
                    {
                        cmd.parts.add(tokens.get(i));
                        i++;
                    }
                    i++;
                }
                program.commands.add(cmd);
            }
            else if (token.equals("endfunction")) 
            {
                program.commands.add(new Command("endfunction"));
                i++;
            }
            else if (token.equals("float") || token.equals("vector3") || token.equals("bool") || token.equals("string")) 
            {
                Command cmd = new Command(token);
                cmd.parts.add(tokens.get(i + 1));
                program.commands.add(cmd);
                i += 2;
            }
            else if (token.equals("label")) 
            {
                Command cmd = new Command("label");
                cmd.parts.add(tokens.get(i + 1));
                
                // save where the label appears so jumps can find it later
                program.labels.put(tokens.get(i + 1), program.commands.size());
                program.commands.add(cmd);
                i += 2;
            }
            else if (token.equals("jump")) 
            {
                Command cmd = new Command("jump");
                cmd.parts.add(tokens.get(i + 1));
                program.commands.add(cmd);
                i += 2;
            }
            else if (token.equals("jumpif")) 
            {
                Command cmd = new Command("jumpif");
                cmd.parts.add(tokens.get(i + 1));
                cmd.parts.add(tokens.get(i + 2));
                program.commands.add(cmd);
                i += 3;
            }
            else if (token.equals("return")) 
            {
                Command cmd = new Command("return");
                i++;
                while (!tokens.get(i).equals("endreturn")) 
                {
                    cmd.parts.add(tokens.get(i));
                    i++;
                }
                i++;
                program.commands.add(cmd);
            }
            else if (token.equals("callfunction")) 
            {
                Command cmd = new Command("callfunction");
                i++;
                cmd.parts.add(tokens.get(i));
                i++;
                  
                // some call syntax includes a return variable and some uses void 
                if (!tokens.get(i).equals("args")) 
                {
                    cmd.parts.add(tokens.get(i));
                    i++;
                } else {
                    cmd.parts.add("void");
                }

                if (tokens.get(i).equals("args")) 
                {
                    i++;
                }

                while (!tokens.get(i).equals("endargs")) 
                {
                    cmd.parts.add(tokens.get(i));
                    i++;
                }
                i++;
                program.commands.add(cmd);
            }
            else if (isOperator(token)) 
            {
                Command cmd = new Command(token);
                int count = operandCount(token);
                for (int j = 1; j <= count; j++) 
                {
                    cmd.parts.add(tokens.get(i + j));
                }
                program.commands.add(cmd);
                i += count + 1;
            }
            else 
            {
                i++;
            }
        }

        return program;
    }
    // need to check whether a token is one of the supported operators
    static boolean isOperator(String s) 
    {
        return s.equals("=") || s.equals("<=") || s.equals(">=") || s.equals(">") || s.equals("<")
                || s.equals("==") || s.equals("!=") || s.equals("+") || s.equals("-")
                || s.equals("*") || s.equals("/") || s.equals("%") || s.equals("&")
                || s.equals("|") || s.equals("!");
    }
    
    // most of my operands will use 3 tokens after the operator
    // = and ! only use 2
    static int operandCount(String op) 
    {
        if (op.equals("=")) 
        {
            return 2;
        }
        if (op.equals("!")) 
        {
            return 2;
        }
        return 3;
    }
    
    static void runProgram(Program program) 
    {
        // pc will be my program counter
        // it tells me which command is currently executing
        int pc = 0;

        while (pc < program.commands.size()) 
        {
            Command cmd = program.commands.get(pc);
            String op = cmd.op;

            if (op.equals("function")) 
            {
                pc++;
            }
            else if (op.equals("endfunction")) 
            {
                break;
            }
            else if (op.equals("float")) 
            {
                declare(cmd.parts.get(0), "float");
                pc++;
            }
            else if (op.equals("vector3")) 
            {
                declare(cmd.parts.get(0), "vector3");
                pc++;
            }
            else if (op.equals("bool")) 
            {
                declare(cmd.parts.get(0), "bool");
                pc++;
            }
            else if (op.equals("string")) 
            {
                declare(cmd.parts.get(0), "string");
                pc++;
            }
            else if (op.equals("label")) 
            {
                // labels do nothing while running.
                pc++;
            }
            else if (op.equals("jump")) 
            {
                pc = program.labels.get(cmd.parts.get(0));
            }
            else if (op.equals("jumpif")) 
            {
                String first = cmd.parts.get(0);
                String second = cmd.parts.get(1);
                String labelName;
                String boolName;
                
                // this will support either order, jumpif label bool or jumpif bool label
                if (program.labels.containsKey(first)) 
                {
                    labelName = first;
                    boolName = second;
                } else 
                {
                    boolName = first;
                    labelName = second;
                }

                if (getBool(boolName)) 
                {
                    pc = program.labels.get(labelName);
                } 
                else 
                {
                    pc++;
                }
            }
            else if (op.equals("return")) 
            {
                break;
            }
            else if (op.equals("callfunction")) 
            {
                runCall(cmd);
                pc++;
            }
            else if (op.equals("=")) 
            {
                assign(cmd.parts.get(0), cmd.parts.get(1));
                pc++;
            }
            else if (op.equals("+")) 
            {
                setFloatTarget(cmd.parts.get(0), getFloat(cmd.parts.get(1)) + getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("-")) 
            {
                setFloatTarget(cmd.parts.get(0), getFloat(cmd.parts.get(1)) - getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("*")) 
            {
                setFloatTarget(cmd.parts.get(0), getFloat(cmd.parts.get(1)) * getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("/")) 
            {
                setFloatTarget(cmd.parts.get(0), getFloat(cmd.parts.get(1)) / getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("%")) 
            {
                setFloatTarget(cmd.parts.get(0), getFloat(cmd.parts.get(1)) % getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals(">")) 
            {
                setBool(cmd.parts.get(0), getFloat(cmd.parts.get(1)) > getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals(">=")) 
            {
                setBool(cmd.parts.get(0), getFloat(cmd.parts.get(1)) >= getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("<")) 
            {
                setBool(cmd.parts.get(0), getFloat(cmd.parts.get(1)) < getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("<=")) 
            {
                setBool(cmd.parts.get(0), getFloat(cmd.parts.get(1)) <= getFloat(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("==")) 
            {
                setBool(cmd.parts.get(0), isEqual(cmd.parts.get(1), cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("!=")) 
            {
                setBool(cmd.parts.get(0), !isEqual(cmd.parts.get(1), cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("&")) 
            {
                setBool(cmd.parts.get(0), getBool(cmd.parts.get(1)) && getBool(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("|")) 
            {
                setBool(cmd.parts.get(0), getBool(cmd.parts.get(1)) || getBool(cmd.parts.get(2)));
                pc++;
            }
            else if (op.equals("!")) 
            {
                setBool(cmd.parts.get(0), !getBool(cmd.parts.get(1)));
                pc++;
            }
            else 
            {
                pc++;
            }
        }
    }

    static void declare(String name, String type) 
    {
        // save the type of the register
        types.put(name, type);
         
        // default value when it is declared
        if (type.equals("float")) 
        {
            values.put(name, 0.0);
        } 
        else if (type.equals("bool")) 
        {
            values.put(name, false);
        } 
        else if (type.equals("string")) 
        {
            values.put(name, "");
        } 
        else if (type.equals("vector3")) 
        {
            values.put(name, new Vector3());
        }
    }

    static void assign(String left, String right) 
    {
        // if the left side is something like v.r, assign to that float part
        if (isVectorPart(left)) 
        {
            setFloatTarget(left, getFloat(right));
            return;
        }

        String type = types.get(left);

        if (type.equals("float")) 
        {
            values.put(left, getFloat(right));
        }
        else if (type.equals("bool")) 
        {
            values.put(left, getBool(right));
        }
        else if (type.equals("string")) 
        {
            values.put(left, getString(right));
        }
        else if (type.equals("vector3")) 
        {
            values.put(left, getVector(right).copy());
        }
    }

    static void runCall(Command cmd) 
    {
        String name = cmd.parts.get(0);
        
        // the only function my interpreter handle is print
        if (name.equals("print")) 
        {
            String label = getString(cmd.parts.get(2));
            double number = getFloat(cmd.parts.get(3));
            System.out.println(label + " " + trimDouble(number));
        }
    }

    static boolean isEqual(String a, String b) 
    {
        String ta = getValueType(a);
        String tb = getValueType(b);
        
        // compare values based on their type
        if (ta.equals("float") && tb.equals("float")) 
        {
            return Double.compare(getFloat(a), getFloat(b)) == 0;
        }
        if (ta.equals("bool") && tb.equals("bool")) 
        {
            return getBool(a) == getBool(b);
        }
        if (ta.equals("string") && tb.equals("string")) 
        {
            return getString(a).equals(getString(b));
        }
        if (ta.equals("vector3") && tb.equals("vector3")) 
        {
            return getVector(a).equals(getVector(b));
        }

        return false;
    }

    static String getValueType(String token) 
    {
        if (isStringLiteral(token)) 
        {
            return "string";
        }
        if (isNumber(token)) 
        {
            return "float";
        }
        if (isVectorPart(token)) 
        {
            return "float";
        }
        return types.get(token);
    }

    static boolean isStringLiteral(String token) 
    {
        return token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"");
    }

    static boolean isNumber(String token) 
    {
        try 
        {
            Double.parseDouble(token);
            return true;
        } 
        catch (Exception e) 
        {
            return false;
        }
    }

    static boolean isVectorPart(String token) 
    {
        return token.contains(".") && token.split("\\.").length == 2;
    }

    static double getFloat(String token) 
    {
        // numeric constant like 5 or 2.2
        if (isNumber(token)) 
        {
            return Double.parseDouble(token);
        }
         
        // vector component like v.r
        if (isVectorPart(token)) 
        {
            String[] pieces = token.split("\\.");
            Vector3 v = (Vector3) values.get(pieces[0]);
            if (pieces[1].equals("r")) 
            {
                return v.r;
            }
            if (pieces[1].equals("g")) 
            {
                return v.g;
            }
            return v.b;
        }
        
        // regular float register, orbook converted to 1/0 if needed
        Object value = values.get(token);
        if (value instanceof Double) 
        {
            return (Double) value;
        }
        if (value instanceof Boolean) 
        {
            return (Boolean) value ? 1.0 : 0.0;
        }
        return 0.0;
    }

    static void setFloatTarget(String token, double value) 
    {
        // either store into a normal float register or into v.r / v.g / v.b
        if (isVectorPart(token)) 
        {
            String[] pieces = token.split("\\.");
            Vector3 v = (Vector3) values.get(pieces[0]);
            if (pieces[1].equals("r")) 
            {
                v.r = value;
            } 
            else if (pieces[1].equals("g")) 
            {
                v.g = value;
            } 
            else 
            {
                v.b = value;
            }
        } 
        else 
        {
            values.put(token, value);
        }
    }

    static boolean getBool(String token) 
    {
        // a numeric constant is true if it is not 0
        if (isNumber(token)) 
        {
            return Double.parseDouble(token) != 0;
        }
        Object value = values.get(token);
        if (value instanceof Boolean) 
        {
            return (Boolean) value;
        }
        if (value instanceof Double) 
        {
            return ((Double) value) != 0.0;
        }
        return false;
    }

    static void setBool(String token, boolean value) 
    {
        values.put(token, value);
    }

    static String getString(String token) 
    {
        // if it is a string literal, remove the quote marks
        if (isStringLiteral(token)) 
        {
            return token.substring(1, token.length() - 1);
        }
        Object value = values.get(token);
        return value == null ? "" : value.toString();
    }

    static Vector3 getVector(String token) 
    {
        return (Vector3) values.get(token);
    }

    static String trimDouble(double value) 
    {
        // prints 5 instead of 5.0 when possible
        if (value == (long) value) 
        {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
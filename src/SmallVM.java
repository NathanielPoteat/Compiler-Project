/*
 * Nathaniel C. Poteat
 * Dr. Abi Salimi
 * CSCI 4200 Programming Languages
 * Spring 2023
 *
 * The purpose of this project is to make an interpreter for a small, simulated
 * assembly-level programming language. Code is loaded into simulated memory through
 * reading in a text file.
 */



import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class SmallVM
{
    public static final int MAX_MEMORY_SIZE = 500; // 500 abstract memory cells
    public static final File INPUT_FILE = new File("src\\mySmallVM_Prog.txt");
    public static final File OUTPUT_FILE = new File("src\\mySmallVM_Output.txt");

    public static BufferedWriter programOut; // Allows printing to the output file.
    public static Object[] instructionStack = new Object[MAX_MEMORY_SIZE];
    public static Identifier[] variableStack = new Identifier[MAX_MEMORY_SIZE];
    public static int instructionStackEnd = 0;
    public static int instructionPointer = 0;
    public static int variableStackEnd = 0;



    /*
     * EXECUTE-FETCH-REPEAT PHASE METHODS
     */

    public static void main(String[] args)
    {
        Scanner scanInstruction = null;

        // Loading phase
        loadToMemory();

        /*
         * Output has to be implemented here in order to ensure proper formatting and to allow the bufferedWriter to
         * output the instructions as they're executed. Another potential solution is to use System.setOut() to write
         * to the file instead of the console, but I still want to be able to use the console to debug and eventually
         * be the main interface for user interaction. BufferedWriter must be encapsulated in try/catch statements
         * because it throws an IOException.
         */
        try {
            programOut = new BufferedWriter(new FileWriter(OUTPUT_FILE));
            outputHeader();
            programOut.flush();
        } catch (Exception e) {
            System.out.println("Unable to output to file: " + e);
        }

	    // Initialize program counter to address of the first instruction.

        String instruction = "";
        String operator = "";
        //System.out.println("instructionPointer initialized to 0."); // Debugging code

        // Repeat fetch-execute-cycle until operation is HALT.
        while (!operator.equals("HALT")) {
            instruction = instructionStack[instructionPointer].toString(); // Fetch
            //System.out.println("instruction " + instruction + " at address " + instructionPointer + " fetched."); // Debugging code

            instructionPointer++; // Increment
            //System.out.println("instructionPointer incremented to " + instructionPointer + "."); // Debugging code

            // Decode the fetched instruction.
            scanInstruction = new Scanner(instruction);
            operator = scanInstruction.next();
            //System.out.println("operation is " + operator + "."); // Debugging code

            // Execute the fetched instruction.
            executeInstruction(instruction, operator);

            try {
                programOut.flush();
            } catch (Exception e) {
                System.out.println("Unable to print to output file at end of cycle.");
            }

            //System.out.println("");
        }
        //System.out.println("Process halted.");

        // Output phase
        try {
            programOut.close();
        } catch (Exception e) {
            System.out.println("Unable to close output stream: " + e);
        }
    }



    public static void executeInstruction(String instruction, String operator)
    {
        Scanner scanInstruction = new Scanner(instruction);
        scanInstruction.next(); // Bypass operator.

        // Most operations are put in their own methods to improve readability and modularity.
        switch (operator) {
            case ";":
                // Comment, ignored by interpreter.
                break;
            case "ADD":
                String addDestination = scanInstruction.next();
                String addSource1 = scanInstruction.next();
                String addSource2 = scanInstruction.next();
                addOperation(addDestination, addSource1, addSource2);
                break;
            case "DIV":
                String divDestination = scanInstruction.next();
                String divSource1 = scanInstruction.next();
                String divSource2 = scanInstruction.next();
                divOperation(divDestination, divSource1, divSource2);
                break;
            case "HALT":
                //System.out.println("Halting process."); // Debugging code
                break;
            case "Identifier":
                // Not an operator.
                break;
            case "IN":
                String in = scanInstruction.next();
                inOperation(in);
                break;
            case "MUL":
                String mulDestination = scanInstruction.next();
                String mulSource1 = scanInstruction.next();
                String mulSource2 = scanInstruction.next();
                mulOperation(mulDestination, mulSource1, mulSource2);
                break;
            case "OUT":
                String out = scanInstruction.nextLine();
                outOperation(out);
                break;
            case "STO":
                String stoDestination = scanInstruction.next();

                // There are two cases for the STO operation, so this sorts them out.
                if (scanInstruction.hasNextInt()) {
                    int stoSource = scanInstruction.nextInt();
                    stoOperation(stoDestination, stoSource);
                } else {
                    String stoSource = scanInstruction.next();
                    stoOperation(stoDestination, stoSource);
                }

                break;
            case "SUB":
                String subDestination = scanInstruction.next();
                String subSource1 = scanInstruction.next();
                String subSource2 = scanInstruction.next();
                subOperation(subDestination, subSource1, subSource2);
                break;
            default:
                System.out.println("Invalid operation call.");
                break;
            }
    }



    /*
     * OPERATION METHODS
     *
     * It is possible to make the following methods more readable by turning some redundant code into its own method,
     * however for the sake of time and in the belief that this code is still sufficiently readable, I have decided not
     * to do that.
     */

    public static void inOperation(String variable)
    {
        try {
            Scanner input = new Scanner(System.in);
            int value = input.nextInt();
            programOut.write(value + "\n");

            variable = variable.trim();
            Identifier identifier = new Identifier(variable, instructionStackEnd, value);

            variableStack[variableStackEnd] = identifier;
            variableStackEnd++;

            //System.out.println(identifier.getName() + " = " + identifier.getValue()); // Debugging code
        } catch (Exception e) {
            System.out.println("Something went wrong in the IN operation: " + e);
        }
    }

    public static void outOperation(String source)
    {
        try {
            source = source.trim();

            if (source.contains("\"") || source.contains("'")) {
                source = source.replaceAll("\"", "");

                System.out.println(source);
                programOut.write(source + "\n");
            } else {
                for (int i = 0; i < variableStackEnd; i++) {
                    if (variableStack[i].getName().equals(source)) {
                        System.out.println(variableStack[i].getValue());
                        programOut.write(variableStack[i].getValue() + "\n");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Something went wrong in the OUT operation method.");
        }
    }

    // This method is for source variables.
    public static void stoOperation(String destinationName, String sourceName)
    {
        int destinationAddress = -1;
        int sourceAddress = -1;
        //System.out.println("Storing value of identifier " + sourceName + " into identifier " + destinationName); // Debugging code

        // Two separate for loops to decrease time complexity at cost of readability
        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destinationName)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destinationName, variableStackEnd, 0);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(sourceName)) {
                sourceAddress = i;
                break;
            }
        }

        variableStack[destinationAddress].setValue(variableStack[sourceAddress].getValue());
        //System.out.println("Identifier " + variableStack[destinationAddress].getName() + "'s value is now " + variableStack[destinationAddress].getValue());
    }

    // This method is for source ints.
    public static void stoOperation(String destinationName, int source)
    {
        int destinationAddress = -1;

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destinationName)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destinationName, variableStackEnd, source);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        variableStack[destinationAddress].setValue(source);
        //System.out.println("Destination identifier confirmed: " + variableStack[destinationAddress].getName() + " Value is: " + variableStack[destinationAddress].getValue());
    }

    public static void addOperation(String destination, String source1, String source2)
    {
        int sum = 0;
        int a = 0;
        int b = 0;
        int destinationAddress = -1;
        destination = destination.trim();
        source1 = source1.trim();
        source2 = source2.trim();

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destination)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destination, variableStackEnd, 0);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        if (Character.isAlphabetic(source1.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source1)) {
                    a = variableStack[i].getValue();
                    //System.out.println("source1 = " + a); // Debugging code
                    break;
                }
            }
        } else {
            a = Integer.parseInt(source1);
            //System.out.println("source1 = " + a); // Debugging code
        }

        if (Character.isAlphabetic(source2.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source2)) {
                    b = variableStack[i].getValue();
                    //System.out.println("source2 = " + b); // Debugging code
                    break;
                }
            }
        } else {
            b = Integer.parseInt(source2);
            //System.out.println("source2 = " + b); // Debugging code
        }

        sum = a + b;
        //System.out.println(a + " + " + b + " = " + sum);
        variableStack[destinationAddress].setValue(sum);
    }

    public static void subOperation(String destination, String source1, String source2)
    {
        int difference = 0;
        int a = 0;
        int b = 0;
        int destinationAddress = -1;
        destination = destination.trim();
        source1 = source1.trim();
        source2 = source2.trim();

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destination)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destination, variableStackEnd, 0);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        if (Character.isAlphabetic(source1.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source1)) {
                    a = variableStack[i].getValue();
                    //System.out.println("source1 = " + a); // Debugging code
                    break;
                }
            }
        } else {
            a = Integer.parseInt(source1);
            //System.out.println("source1 = " + a); // Debugging code
        }

        if (Character.isAlphabetic(source2.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source2)) {
                    b = variableStack[i].getValue();
                    //System.out.println("source2 = " + b); // Debugging code
                    break;
                }
            }
        } else {
            b = Integer.parseInt(source2);
            //System.out.println("source2 = " + b); // Debugging code
        }

        difference = a - b;
        //System.out.println(a + " - " + b + " = " + difference);
        variableStack[destinationAddress].setValue(difference);
    }

    public static void mulOperation(String destination, String source1, String source2)
    {
        int product = 0;
        int a = 0;
        int b = 0;
        int destinationAddress = -1;
        destination = destination.trim();
        source1 = source1.trim();
        source2 = source2.trim();

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destination)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destination, variableStackEnd, 0);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        if (Character.isAlphabetic(source1.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source1)) {
                    a = variableStack[i].getValue();
                    //System.out.println("source1 = " + a); // Debugging code
                    break;
                }
            }
        } else {
            a = Integer.parseInt(source1);
            //System.out.println("source1 = " + a); // Debugging code
        }

        if (Character.isAlphabetic(source2.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source2)) {
                    b = variableStack[i].getValue();
                    //System.out.println("source2 = " + b); // Debugging code
                    break;
                }
            }
        } else {
            b = Integer.parseInt(source2);
            //System.out.println("source2 = " + b); // Debugging code
        }

        product = a * b;
        //System.out.println(a + " * " + b + " = " + product);
        variableStack[destinationAddress].setValue(product);
    }

    public static void divOperation(String destination, String source1, String source2)
    {
        int quotient = 0;
        int a = 0;
        int b = 0;
        int destinationAddress = -1;
        destination = destination.trim();
        source1 = source1.trim();
        source2 = source2.trim();

        for (int i = 0; i < variableStackEnd; i++) {
            if (variableStack[i].getName().equals(destination)) {
                destinationAddress = i;
                break;
            }
        }

        if (destinationAddress < 0) {
            variableStack[variableStackEnd] = new Identifier(destination, variableStackEnd, 0);
            destinationAddress = variableStackEnd;
            variableStackEnd++;
        }

        if (Character.isAlphabetic(source1.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source1)) {
                    a = variableStack[i].getValue();
                    //System.out.println("source1 = " + a); // Debugging code
                    break;
                }
            }
        } else {
            a = Integer.parseInt(source1);
            //System.out.println("source1 = " + a); // Debugging code
        }

        if (Character.isAlphabetic(source2.charAt(0))) {
            for (int i = 0; i < variableStackEnd; i++) {
                if (variableStack[i].getName().equals(source2)) {
                    b = variableStack[i].getValue();
                    //System.out.println("source2 = " + b); // Debugging code
                    break;
                }
            }
        } else {
            b = Integer.parseInt(source2);
            //System.out.println("source2 = " + b); // Debugging code
        }

        quotient = a / b;
        //System.out.println(a + " / " + b + " = " + quotient);
        variableStack[destinationAddress].setValue(quotient);
    }



    /*
     * INPUT/OUTPUT METHODS
     */

    public static void loadToMemory()
    {
        // try/catch statement is needed to use FileReader.
        try {
            BufferedReader parser = new BufferedReader(new FileReader(INPUT_FILE));

            String loader;

            // Loads each line of the input file into its own index/address in the memory array.
            try {
                while ((loader = parser.readLine()) != null) {
                    instructionStack[instructionStackEnd] = loader;
                    //System.out.println(instructionStack[instructionStackEnd] + " loaded into address " + instructionStackEnd);
                    instructionStackEnd++;
                }
            } catch (Exception e) {
                System.out.println(e + "; means STACK OVERFLOW! Please ensure your code can fit within the allocated " + MAX_MEMORY_SIZE + " cells of memory.");
            }

            System.out.println("");
        } catch (Exception e) {
            System.out.println("Loading unsuccessful: " + e);
        }
    }



    public static void outputHeader()
    {
        // Create the 46x * divider.
        String divider = "";
        for (int i = 0; i < 46; i++)
            divider += "*";

        try {
            programOut.write("Nathaniel C. Poteat, CSCI 4200, Spring 2023\n" + divider +"\n");

            int pointer = 0;
            while (instructionStack[pointer] != null && pointer < MAX_MEMORY_SIZE) {
                programOut.write(instructionStack[pointer] + "\n");
                pointer++;
            }

            programOut.write(divider + "\n");
        } catch (Exception e) {

        }
    }
}



/*
 * IDENTIFIER HOLDS VARIABLES
 */

class Identifier {
    private String name;
    private int address; // index in variableStack
    private int value;
    // Can add type, value, and scope in the future.

    public Identifier(String name, int address, int value)
    {
        this.name = name;
        this.address = address;
        this.value = value;
    }

    public void setName(String name)
    {
        if (Pattern.matches("[A-Za-z][A-Za-z0-9_]*", name)) {
            this.name = name;
        } else {
            System.out.println("Invalid identifier/variable name.");
        }
    }
    public void setValue(int value) { this.value = value; }
    public void setAddress(int address) { this.address = address; }
    public String getName() { return this.name; }
    public int getValue() { return this.value; }
    public int getAddress() { return this.address; }
}



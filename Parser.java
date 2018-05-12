import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.LinkedList;
import java.io.PrintWriter;
import java.lang.String;
public class Parser
{	

	String fileString;
	LinkedList<Token> tokenArray;
	int currentPos;
	Token next;
	ParseNode currentNode;
	int tabs = 0, prunedTabs = 0;
	String tree;
	String prunedTree;
	String tabsStr = "", prunedTabsStr = "";

	static SemanticTable st;

	public static void main(String [] args)
	{
		if(args.length == 0)
		{
			System.out.println("No File Specified");
			System.exit(0);
		}

		//lexing and parsing
		Lexer l = new Lexer(args[0]);
		Parser p = new Parser("output.txt");

		//semantic table analysis
		ScopeAnalyzer sa = new ScopeAnalyzer(st);
		TypeChecker tc = new TypeChecker(sa.st,sa.root);//parse the tc the semtable and ast
		tc.printTree(true);

		boolean onlyNo = false;
		if(args.length>1)
			onlyNo = (args[1].equals("--no"));

		ValueChecker vc = new ValueChecker(sa.st,sa.root,onlyNo);
	}

	public Parser(String fileName)
	{
		currentPos = 0;
		Scanner sc;
		try{
			sc = new Scanner(new File(fileName));			

			fileString = sc.useDelimiter("\\Z").next(); //scan entire file

		tokenArray = getTokens(fileString);

		}catch(FileNotFoundException e)
		{
			System.out.println("File: " + fileName + " not found. Exiting");			
		}

		next = tokenArray.get(0);

		parseS();

		//print parse tree to file
		//printToFile();
		System.out.println(tree);
		printToFile();
		System.out.println("============================================\n"
						  + "\t\tPruned Tree\n"  
							+"============================================\n"
							+prunedTree);
		writePrunedToFile();
	}
	
	public LinkedList<Token> getTokens(String tokenString)
	{
		LinkedList<Token> tempList = new LinkedList<Token>();

		Scanner sc = new Scanner(tokenString);
		String temp ="";
		String [] singleToken;


		while(sc.hasNext())
		{
			temp = sc.nextLine();
			singleToken = temp.split("\\|");

			// for(int i = 0; i < singleToken.length;i++)
			// 	System.out.println(singleToken[i]);

			String data = "";
			if(singleToken.length==3)
				data =  singleToken[2];


			tempList.add(new Token(Lexer.TokenType.valueOf(singleToken[1]),data));
		}

		sc.close();
		tempList.add(new Token(Lexer.TokenType.EOF, "$"));
		return tempList;		
	}

	public void printNode()
	{
		if(tabs*2>=tabsStr.length())
			for(int i = tabsStr.length(); i < tabs*2;i+=2)
				tabsStr+="|\t";
		else
		{
			int tabStrLength = tabsStr.length();

			for(int i = tabs*2;i<tabStrLength;i+=2)
				tabsStr = tabsStr.substring(0,tabsStr.length()-2);
		}


		tree+=tabsStr+currentNode.toString()+"\n";
	}

	public void printNodeToPrunedTree()
	{
		//add to normal tree
		printNode();

		//add to prunedTree
		if(prunedTabs>=prunedTabsStr.length())
			for(int i = prunedTabsStr.length(); i < prunedTabs;i++)
				prunedTabsStr+="\t";
		else
		{
			int tabStrLength = prunedTabsStr.length();
			for(int i = prunedTabs;i<tabStrLength;i++)
				prunedTabsStr = prunedTabsStr.substring(0,prunedTabsStr.length()-1);
		}

		prunedTree+=prunedTabsStr+currentNode.printPruned()+"\n";
	}

	public void printToFile()
	{
		try{
			PrintWriter writer = new PrintWriter("temp.txt");
			String output= "";

			ParseNode temp = currentNode;
			boolean cont = true;

			LinkedList<ParseNode> queue = new LinkedList<ParseNode>();
			queue.add(temp);
			while(!queue.isEmpty())
			{
				temp = queue.remove();
				output += temp.toString() + "\n";
				for(ParseNode n : temp.children)
				{
					queue.add(n);
				}
			}

			writer.write(output);

			writer.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
	}

	public void writePrunedToFile()
	{
		try{
			PrintWriter w = new PrintWriter("pruned.txt");
			w.write(prunedTree);
			w.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public void parseVAR()
	{	
		currentNode = currentNode.addChild(new ParseNode(NodeType.VAR));
		tabs++;
		prunedTabs++;

		//@todo check what type of variable var is
		if(next.type == Lexer.TokenType.STR)
		{
			//var is string
			currentNode.type = NodeType.STR;
			currentNode.data = next.data;
			printNodeToPrunedTree();
			match(Lexer.TokenType.STR);
		}
		else if (next.type == Lexer.TokenType.VAR)
		{
			currentNode.data = next.data;
			printNodeToPrunedTree();
			match(Lexer.TokenType.VAR);
		}
		else if(next.type == Lexer.TokenType.INT)
		{
			parseNUMEXPR();
		}
		else
		{
			System.out.println("Error: Cannot parse VAR with "+next.data);
			System.exit(0);
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public ParseNode parseVARwithoutPrint()
	{
		tabs++;
		currentNode = currentNode.addChild(new ParseNode(NodeType.VAR));
		
		//check what type of variable var is
		if(next.type == Lexer.TokenType.STR)
		{
			//var is string
			currentNode.type = NodeType.STR;
			currentNode.data = next.data;
			match(Lexer.TokenType.STR);
		}
		else if (next.type == Lexer.TokenType.VAR)
		{
			currentNode.data = next.data;
			match(Lexer.TokenType.VAR);
		}
		else if(next.type == Lexer.TokenType.INT)
		{
			parseNUMEXPR();
		}
		else
		{
			System.out.println("Error: Cannot parse VAR with "+next.data);
			System.exit(0);
		}
		tabs--;
		ParseNode temp = currentNode;
		currentNode = currentNode.parent;
		return temp;
	}

	public void parseAssign()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing ASSIGN");

		currentNode = currentNode.addChild(new ParseNode(NodeType.ASSIGN));
		printNodeToPrunedTree();
		parseVAR();
		match("=");

		if(next.type == Lexer.TokenType.STR || next.type == Lexer.TokenType.VAR)
		{
			parseVAR();
		}
		else if(next.type == Lexer.TokenType.INT || next.type == Lexer.TokenType.NUM_OP)
			parseNUMEXPR();
		else
			parseBOOL();

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseNUMEXPR()
	{
		tabs++;
		System.out.println("Passing NUMEXPR with " +next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.NUMEXPR));
		printNode();

		if(next.type == Lexer.TokenType.VAR)
		{
			printNode();
			parseVAR();			
		}
		else if(next.type == Lexer.TokenType.INT)
		{
			prunedTabs++;
			currentNode.data = next.data;
			printNodeToPrunedTree();
			match(Lexer.TokenType.INT);
			prunedTabs--;
		}
		else if(next.type == Lexer.TokenType.NUM_OP)
		{
			printNode();
			parseCALC();//next is ADD,SUB or MULT
		}
		
		//printNode();
		currentNode = currentNode.parent;
		tabs--;
	}

	public void parseCALC()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing CALC with "+ next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.CALC));

		if(next.data.equals("add"))//which is next?
		{
			currentNode.data="+";
			printNodeToPrunedTree();
			match("add");
			//next is an add operation
			match("(");
			parseNUMEXPR();
			match(",");
			parseNUMEXPR();
			match(")");
		}
		else if(next.data.equals("sub"))
		{
			currentNode.data="-";
			printNodeToPrunedTree();
			match("sub");
			//next is a sub operation
			match("(");
			parseNUMEXPR();
			match(",");
			parseNUMEXPR();
			match(")");
		}
		else if(next.data.equals("mult"))
		{
			currentNode.data="*";
			printNodeToPrunedTree();
			match("mult");
			//next is a mult operation
			match("(");
			parseNUMEXPR();
			match(",");
			parseNUMEXPR();
			match(")");
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseCOND_BRANCH()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing COND_BRANCH with "+ next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.COND_BRANCH));

		if(next.data.equals("if"))
		{
			currentNode.data = "if";
			printNodeToPrunedTree();
			match("if");
			match("(");
			parseBOOL();
			match(")");

			tabs++;
			prunedTabs++;
			currentNode = currentNode.addChild(new ParseNode(NodeType.COND_BRANCH,"then"));
			printNodeToPrunedTree();
			match("then");
			match("{");
			parseCODE();
			match("}");

			currentNode = currentNode.parent;
			tabs--;
			prunedTabs--;

			if(next.data.equals("else"))
			{
				tabs++;
				prunedTabs++;
				currentNode = currentNode.addChild(new ParseNode(NodeType.COND_BRANCH,"else"));
				printNodeToPrunedTree();
				match("else");
				match("{");
				parseCODE();
				match("}");
				tabs--;
				prunedTabs--;

				currentNode = currentNode.parent;
			}
			
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseBOOL()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing BOOL with "+ next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.BOOL));

		if(next.data.equals("eq"))//operation
		{		
			currentNode.data = "eq";
			printNodeToPrunedTree();
			match("eq");
			match("(");
			parseVAR();
			match(",");
			parseVAR();
			match(")");
			
		}
		else if(next.data.equals("not"))//not operation
		{
			currentNode.data = "not";
			printNodeToPrunedTree();
			match("not");
			parseBOOL();

		}
		else if(next.data.equals("and"))//and operation
		{
			currentNode.data = "and";
			printNodeToPrunedTree();
			match("and");
			match("(");
			parseBOOL();
			match(",");
			parseBOOL();
			match(")");

		}
		else if(next.data.equals("or"))//or operation
		{
			currentNode.data = "or";
			printNodeToPrunedTree();
			match("or");
			match("(");
			parseBOOL();
			match(",");
			parseBOOL();
			match(")");

		}
		else if(next.data.equals("(")) //bool in brackets - expresion
		{
			match("(");

			ParseNode temp = parseVARwithoutPrint();
			ParseNode tempCurrent = currentNode;

			if(next.data.equals("<"))
			{
				currentNode.data = "<";
				printNodeToPrunedTree();
				tabs++;
				prunedTabs++;

				currentNode = temp;
				printNodeToPrunedTree();
				currentNode = tempCurrent;

				tabs--;
				prunedTabs--;

				match("<");
				parseVAR();
			}
			else if(next.data.equals(">"))
			{
				currentNode.data = ">";
				printNodeToPrunedTree();

				tabs++;
				prunedTabs++;

				currentNode = temp;
				printNodeToPrunedTree();
				currentNode = tempCurrent;

				tabs--;
				prunedTabs--;

				match(">");
				parseVAR();
			}
			match(")");


		}
		else if(next.type == Lexer.TokenType.VAR)
		{
			ParseNode temp = parseVARwithoutPrint();
			ParseNode tempCurrent = currentNode;
			if(next.data.equals("<"))
			{
				currentNode.data = "<";
				printNodeToPrunedTree();

				tabs++;
				prunedTabs++;

				currentNode = temp;
				printNodeToPrunedTree();
				currentNode = tempCurrent;

				tabs--;
				prunedTabs--;

				match("<");
				parseVAR();
			}
			else if(next.data.equals(">"))
			{
				currentNode.data = ">";
				printNodeToPrunedTree();

				tabs++;
				prunedTabs++;

				currentNode = temp;
				printNodeToPrunedTree();
				currentNode = tempCurrent;

				tabs--;
				prunedTabs--;

				match(">");
				parseVAR();
			}
			else
			{
				currentNode.data = next.data;
				currentNode = temp;
				printNodeToPrunedTree();
				currentNode = tempCurrent;
			}
		}
		else if(next.type == Lexer.TokenType.TRUTH)
		{
			currentNode.data = next.data;
			printNodeToPrunedTree();
			match(Lexer.TokenType.TRUTH);
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseCONDLOOP()
	{
		tabs++;
		System.out.println("Parsing CONDLOOP wiht "+ next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.CONDLOOP));

		if(next.data.equals("while"))//while loop
		{
			prunedTabs++;
			currentNode.data = "while";
			printNodeToPrunedTree();
			match("while");
			match("(");
			parseBOOL();
			match(")");
			match("{");
			parseCODE();
			match("}");	
			prunedTabs--;
		}
		else if(next.data.equals("for")) //for loop
		{
			prunedTabs++;
			currentNode.data = "for";
			printNodeToPrunedTree();
			match("for");
			//initial var setup
			match("(");
			parseVAR();
			match("=");
			match("0");
			match(";");

			//condition
			parseVAR();
			match("<");	//less than expression
			parseVAR();
			match(";");

			//increment
			parseVAR();
			match("=");	//assignment
			match("add");	//add expression
			match("(");
			parseVAR();	
			match(",");
			match("1");	//NUM 1
			match(")");
			match(")");//close for

			//body
			match("{");
			parseCODE();
			match("}");
			prunedTabs--;
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
	}

	public void parseS()
	{
		System.out.println("Starting parseS()");
		currentNode = new ParseNode(NodeType.START,"root");
		printNodeToPrunedTree();

		parsePROG();
		// match(Lexer.TokenType.EOF);
		//printNode();
	}

	public void parsePROG()
	{	
		tabs++;
		System.out.println("Parsing PROG");
		currentNode = currentNode.addChild(new ParseNode(NodeType.PROG,currentNode.data)); 
		printNode();

		parseCODE();
		if(next.data.equals(";"))
		{
			match(";");
			parsePROC_DEFS();
		}

		//printNode();
		currentNode = currentNode.parent;
		tabs--;

	}

	public void parsePROC_DEFS()
	{
		tabs++;
		// prunedTabs++;
		System.out.println("Parsing PROC_DEFS");

		currentNode = currentNode.addChild(new ParseNode(NodeType.PROC_DEFS));
		// printNodeToPrunedTree();

		parsePROC();
		if(next.type == Lexer.TokenType.PROC)
			parsePROC_DEFS();

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		// prunedTabs--;
	}

	public void parsePROC()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing PROC");

		currentNode = currentNode.addChild(new ParseNode(NodeType.PROC));	

		match("proc");
		currentNode.data = next.data;
		printNodeToPrunedTree();
		match(Lexer.TokenType.VAR);
		match("{");
		parsePROG();
		match("}");

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseCODE()
	{
		tabs++;
		System.out.println("Parsing CODE with "+next.data);

		currentNode = currentNode.addChild(new ParseNode(NodeType.CODE));
		printNode();
		
		parseINSTR();

		if(next.data.equals(";"))
		{
			if(tokenArray.get(currentPos+1).type != Lexer.TokenType.PROC)
			{
				match(";");
				if(!next.data.equals("}"))
					parseCODE();
			}
		}
	
		//printNode();
		currentNode = currentNode.parent;
		tabs--;
	}

	public void parseINSTR()
	{
		tabs++;
		System.out.println("Parsing INSTR with "+ next.type);

		currentNode = currentNode.addChild(new ParseNode(NodeType.INSTR));

		switch(next.type)
		{
			case HALT:
				prunedTabs++;
				currentNode.data = "halt";
				printNodeToPrunedTree();
				match(Lexer.TokenType.HALT);
				prunedTabs--;
				break;
			case TYPE:
				printNode();
				parseDECL();
				break;
			case IO:
				printNode();
				parseIO();
				break;
			case VAR:
				printNode();
				if(tokenArray.get(currentPos + 1).data.equals("="))
					parseAssign();
				else parseNAME();
				break;
			case STRUC:
				printNode();
				if(next.data.equals("if"))
					parseCOND_BRANCH();
				else parseCONDLOOP();
				break;
			default:
				System.out.println("Error: Cannot parse INSTR with "+ next.data);
				System.exit(0);
				break;

		}	

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
	}

	public void parseIO()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing IO");

		currentNode = currentNode.addChild(new ParseNode(NodeType.IO));

		if(next.data.equals("input"))
			currentNode.data = "input";
		else
			currentNode.data = "output";

		printNodeToPrunedTree();

		match(Lexer.TokenType.IO);
		match("(");
		parseVAR();
		match(")");

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseDECL()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing DECL");

		currentNode = currentNode.addChild(new ParseNode(NodeType.DECL));
		printNodeToPrunedTree();

		parseTYPE();
		parseVAR();

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void parseNAME()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing NAME");

		currentNode = currentNode.addChild(new ParseNode(NodeType.NAME,next.data));
		printNodeToPrunedTree();

		match(Lexer.TokenType.VAR);
		//printNode();
		prunedTabs--;
		currentNode = currentNode.parent;
		tabs--;
	}


	public void parseTYPE()
	{
		tabs++;
		prunedTabs++;
		System.out.println("Parsing TYPE");

		currentNode = currentNode.addChild(new ParseNode(NodeType.TYPE,next.data));
		printNodeToPrunedTree();

		match(Lexer.TokenType.TYPE);

		//printNode();
		currentNode = currentNode.parent;
		tabs--;
		prunedTabs--;
	}

	public void match(Lexer.TokenType type)
	{
		System.out.println("Matching "+ type);
		if(next.type != type)
		{
			System.out.println("Syntax Error: Unable to match token type " + type.toString() + " at position " + currentPos);
			System.out.println("Found " + next.type.toString() + " instead");
			System.exit(1);
		}
		currentNode.addChild(new ParseNode(NodeType.TERMINAL, next.data));
		next = tokenArray.get(++currentPos);
	}
	public void match(String input)
	{
		System.out.println("Matching "+input);
		if(!next.data.equals(input))
		{
			System.out.println("Syntax Error: Unable to match token " + input + " at position " + currentPos);
			System.out.println("Found " + next.data + " instead");
			System.exit(1);
		}
		currentNode.addChild(new ParseNode(NodeType.TERMINAL, next.data));
		next = tokenArray.get(++currentPos);
	}
}

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.PrintWriter;
public class Lexer
{
	Scanner sc;
	String input="";
	char[] srcArray;
	int currentPosition = 0;
	int wordBeginIndex = 0;
	LinkedList<Token> tokenList;
	char current = ' ';

	public static void main(String [] args)
	{
		Lexer l = new Lexer(args[0]);
	}

	public enum TokenType {
		VAR, INT, STRUC ,COMP, BOOL, NUM_OP, 
		STR, STR_INDIC, GROUP, ASSIGN, IO, HALT, TYPE, 
		PROC, INVALID, EOF, TRUTH, PROG;	
	}

	public Lexer(String path)
	{
		try{
			sc = new Scanner(new File(path));			
		}catch(FileNotFoundException e)
		{
			System.out.println("File: " + path + " not found. Exiting");
			System.exit(0);
		}

		tokenList = new LinkedList<Token>();

		// sc.useDelimiter("\\z");

		if(!sc.hasNext())
		{
			System.out.println("Empty File");
			System.exit(0);
		}

		// input = sc.next();
		String temp = "";

		while(sc.hasNextLine())
		{
			temp = sc.nextLine();
			if(temp.length()>0)
			{
				System.out.println(temp);
				input+="\n"+temp;
			}
		}

		srcArray = input.toCharArray();
		System.out.println("Array length: "+ srcArray.length);

		read();
		printToFile();
	}

	public void printToFile()
	{
		try{
			PrintWriter writer = new PrintWriter("output.txt");
			String output= "";
			for(int i = 0; i < tokenList.size(); i ++)
				output+=tokenList.get(i).toString() + "\n";

			writer.write(output);
			writer.write(new Token(TokenType.EOF,"$").toString());

			writer.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
	}

	public void read()
	{
		while(currentPosition < srcArray.length)
		{
			current = srcArray[currentPosition];

			System.out.println("read: "+ current + " at " + currentPosition);

			//check if current char is a letter
			if(current >= 'a' && current <= 'z')
			{
				// System.out.println("Processing Letter");
				processLetter();
			}
			else if(current == ' ' || current == '\n' || current == 13 || Character.isWhitespace(current))
			{
				//current is a seperator symbol
				// System.out.println("Skipping space or endline");
				currentPosition++;
				continue;

			}
			else if((current >= '0' && current <= '9') || current == '-')
			{
				//process input as number
				System.out.println("Processing number");
				processNumber();
			}
			else
			{
				switch(current)
				{
					case '\"':
						//String symbol
						//add symbol to linked list
						//tokenList.add(new Token(TokenType.STR_INDIC,"\""));
						parseString();
						//tokenList.add(new Token(TokenType.STR_INDIC,"\""));
						currentPosition--;
						break;
					case '<':
						//comparison symbol
						tokenList.add(new Token(TokenType.COMP,"<"));
						break;
					case '>':
						//comparison symbol
						tokenList.add(new Token(TokenType.COMP,">"));
						break;
					case '(':
						//circle bracket - grouping
						tokenList.add(new Token(TokenType.GROUP,"("));
						break;
					case ')':
						//circle bracket - grouping
						tokenList.add(new Token(TokenType.GROUP,")"));
						break;
					case '{':
						//curly bracket - grouping
						tokenList.add(new Token(TokenType.GROUP,"{"));
						break;
					case '}':
						//curly bracket - grouping
						tokenList.add(new Token(TokenType.GROUP,"}"));
						break;
					case ',':
						//comma - grouping
						tokenList.add(new Token(TokenType.GROUP,","));
						break;
					case ';':
						//semi-colon - grouping
						tokenList.add(new Token(TokenType.GROUP,";"));
						break;
					case '=':
						tokenList.add(new Token(TokenType.ASSIGN,"="));
						//assignment operator
						break;
					case 'T':
						tokenList.add(new Token(TokenType.TRUTH, "T"));
						break;
					case 'F':
						tokenList.add(new Token(TokenType.TRUTH, "F"));
						break;
					default:
						System.out.println("Error reading char: "+ current + " aborting");
						System.exit(0);
				}

				currentPosition++;
			}			
		}
	}

	public void processLetter()
	{		
		//check if possible for word to be a reserved word
		if( current == 'c' || current == 'd' || current == 'g' ||
			current == 'j' || current == 'k' || current == 'l' ||
			current == 'q' ||	current == 'r' || 
			current == 'u' || current == 'v' ||	current == 'x' ||
			current == 'y' || current == 'z')
		{
			//word cannot be a reserved word
			//therefor we assume variable until spacer or invalid (not allowed)
			// System.out.println("Processing as variable");
			processVariable();
		}
		else
		{	
			//keep track of read characters - can be replaced with a counter as in processVariable()
			String word = current+"";
			char next;
			Token temp;

			//remember where word begins
			// wordBeginIndex = currentPosition++;
			currentPosition++; //skip "

			//while next char is valid
			while(currentPosition<srcArray.length)
			{
				next = srcArray[currentPosition];

				if((next >= 'a' && next <= 'z') || (next >= '0' && next <= '9'))
				{					
					word += next;
					currentPosition++;					
				}
				else
				{
					//check if word is a keyword
					temp = isKeyWord(word);
					if(temp!=null)
					{
						tokenList.add(temp);
						return;
					}

					break;				
				}
			}

			temp = isKeyWord(word);
			if(temp != null)
			{
				tokenList.add(temp);
				return;
			}
			//add variable token
			tokenList.add(new Token(TokenType.VAR,word));
		}
	}

	public void processNumber()
	{
		char next = srcArray[currentPosition++]; //post increment = current
		String num = "";

		//working with a negative int
		if(next == '-')
		{
			//add negative symbol
			//System.out.println("negative number");
			num = next+"";
			next = srcArray[currentPosition++];
			if(next == '0')
			{
				System.out.println("Number Error: '-0' is not allowed!");
				System.exit(0);
			}
		}		
		
		//first number must be from 1-9
		if(next >= '0' && next <= '9')
		{
			num += next;					

			//continue
			while(currentPosition < srcArray.length)
			{
				next = srcArray[currentPosition];

				if(next >= '0' && next <= '9')
				{
					num+= next;
					currentPosition++;
				}
				else
					break;
			}
			
			//add int num to token - leave as string as it is written to file anyway
			tokenList.add(new Token(TokenType.INT,num));
		}
		else
		{
			//throw numeric error
			System.out.println("Number Error: At " + (currentPosition-1) +" with char '" + next+"'");
			System.exit(0);
		}
	}

	//demonstrates a possible method of avoiding explicit memory
	//aka no String variable kept
	public void processVariable()
	{
		wordBeginIndex = currentPosition++;
		char c;

		//loop until end of word
		//check not out of bounds
		while(currentPosition<srcArray.length)
		{
			c = srcArray[currentPosition]; //get next char

			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
				currentPosition++;//move on
			else
				break; //exit while loop
		}


		//construct a new token with a string made from src array
		//this prevents added memory
		//add token to linked list
		tokenList.add(new Token(TokenType.VAR,new String(srcArray,wordBeginIndex,currentPosition-wordBeginIndex)));
	}

	public void parseString()
	{
		char next;
		String temp ="";
		int strLength = 0;
		currentPosition++;
		
		//while we are in bounds and string is not greater than 8 chars long
		while(currentPosition < srcArray.length)
		{
			if(strLength > 8)
			{
				System.out.println("String Error: Length " + strLength + " out of bounds");
				System.exit(0);
			}

			//move to next char
			next = srcArray[currentPosition++];
			System.out.println("Reading character " + next);
			if(!((next >= 'a' && next <= 'z') || (next >= '0' && next <= '9') || next == '\"' || next == ' '))
			{
				System.out.println("String Error: Character " + next + " is not a valid string character!");
				System.exit(0);
			}

			//make sure char isnt an "invisible" char
			if(next == '\n' || next == 13)
			{
				currentPosition++;
				continue;
			}

			strLength++;

			if(next == '\"')
				break;//exit loop - reached end of string
			
			// currentPosition++;

			temp+=next; //add char to string
		}

		//add String token to list
		tokenList.add(new Token(TokenType.STR,temp));
	}

	public Token isKeyWord(String word)
	{
		//check for keywords by length
		switch(word.length())
		{
			case 2:// eq || or || if
				if(word.equals("eq"))
					return new Token(TokenType.COMP,"eq");
				else if(word.equals("if"))
					return new Token(TokenType.STRUC,"if");
				else if(word.equals("or"))
					return new Token(TokenType.BOOL,"or");
				break;

			case 3://and || not || add || sub || for || num
				if(word.equals("and"))
					return new Token(TokenType.BOOL,"and");
				else if(word.equals("not"))
					return new Token(TokenType.BOOL,"not");
				else if(word.equals("add"))
					return new Token(TokenType.NUM_OP,"add");
				else if(word.equals("sub"))
					return new Token(TokenType.NUM_OP,"sub");
				else if(word.equals("for"))
					return new Token(TokenType.STRUC,"for");
				else if(word.equals("num"))
					return new Token(TokenType.TYPE,"num");
				break;

			case 4:
				if(word.equals("mult"))
					return new Token(TokenType.NUM_OP,"mult");
				else if(word.equals("then"))
					return new Token(TokenType.STRUC,"then");
				else if(word.equals("else"))
					return new Token(TokenType.STRUC,"else");
				else if(word.equals("halt"))
					return new Token(TokenType.HALT,"halt");
				else if(word.equals("bool"))
					return new Token(TokenType.TYPE,"bool");
				else if(word.equals("proc"))
					return new Token(TokenType.PROC,"proc");
				break;

			case 5://while || input
				if(word.equals("while"))
					return new Token(TokenType.STRUC,"while");
				else if(word.equals("input"))
					return new Token(TokenType.IO,"input");
				break;

			case 6: //string || output
				if(word.equals("string"))
					return new Token(TokenType.TYPE,"string"); //'string' type
				else if(word.equals("output"))
					return new Token(TokenType.IO,"output");
		}

		return null; //not a keyword
	}
}

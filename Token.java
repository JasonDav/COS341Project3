public class Token
{
	Lexer.TokenType type;
	String data;
	static int num = 1;
	int myNum;
	public Token(Lexer.TokenType type, String data)
	{	
		this.type = type;
		this.data = data;
		myNum = num++;
		
		//System.out.println("Token " + num + " - Type: "+ type + " - Data: " + data);
	}

	public String toString()
	{
		return "T"+myNum+"|"+type+"|"+data;
	}
}
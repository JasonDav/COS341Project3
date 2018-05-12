import java.util.LinkedList;
public class ParseNode
{

	NodeType type;
	LinkedList<ParseNode> children;
	String data;
	ParseNode parent = null;
	static int id;
	int num;
	boolean visited = false;
	int tabs;
	LinkedList<Integer> scopes;
	boolean hasValue = false;
	ParseNode thenBranch = null;

	public ParseNode(NodeType type)
	{
		this.type = type;
		children = new LinkedList<ParseNode>();
		num = id++;
	}

	public ParseNode(NodeType type, String data)
	{
		this(type);
		this.data = data;
	}	

	//adds child and returns it
	public ParseNode addChild(ParseNode node)
	{
		node.parent=this;
		children.add(node);
		return node;
	}

	public String toString()
	{
		String out = num+"|"+type+"|"+data;
		for(ParseNode n : children)
			out += "|" + n.num;
		return out;
	}

	public String printPruned()
	{
		return num+"|"+type+"|"+data;
	}

	public String printWithTabs()
	{
		String tabStr = "";
		for(int i = 0; i < tabs; i++)
			tabStr+="\t";

		return tabStr+num+"|"+type+"|"+data;
	}

	public String getTabs()
	{
		String tabStr = "";
		for(int i = 0; i < tabs; i++)
			tabStr+="\t";

		return tabStr;
	}

	public String printWithScopes()
	{
		String out = "";
		for(Integer i: scopes)
			out+=" "+i;

		return printWithTabs() + out;
	}

	public void addScopeToNode(Integer s)
	{
		if(scopes==null)
			scopes = new LinkedList<Integer>();

		if(!scopes.contains(s))
			scopes.add(s);
	}
}

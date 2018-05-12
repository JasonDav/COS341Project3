import java.util.LinkedList;
public class SemanticTable
{
	public static int tableID = 0;
	static int procNums = 0;
	static int varNum = 0;
	static int forVarNum = 0;

	static boolean forLoop = false;

	LinkedList<TableEntry> entries;
	LinkedList<TableEntry> declerations;
	LinkedList<TableEntry> forLoopVars;
	public SemanticTable()
	{	
		//semantic table holds rows
		entries = new LinkedList<TableEntry>();
		declerations = new LinkedList<TableEntry>();
		forLoopVars = new LinkedList<TableEntry>();
	}

	public void addEntry(ParseNode node)
	{
		entries.add(new TableEntry(node));
	}

	public void scopeTable()
	{
		for(TableEntry t : entries)
		{

			if(t.decl_entry!=null)
			{
				t.name = t.decl_entry;
				continue;
			}

			//check if type is of variable or proc
			if(t.node.type == NodeType.VAR)
			{
				//the var is inside a for check
				if(t.node.parent!=null && t.node.parent.data.equals("for"))
				{	
					//check if varibale has been declared!
					TableEntry temp = isDeclaredFor(t.node);
					if(temp!=null)//chck if var is the for counter
					{
						t.decl_id = temp.t_id;
						t.name = temp.decl_entry;
						t.varType = temp.varType;
						t.isForVar = temp.isForVar;
					}
					else if((temp=isDeclared(t.node))!=null && t.scopes.contains(temp.decl_scope))
					{
						t.decl_id = temp.t_id;
						t.name = temp.decl_entry;
						t.varType = temp.varType;
					}
					else
					{						
						//var is undeclared
						t.name = "U";
					}
				}
				else
				{
					//check if varibale has been declared!
					TableEntry temp = isDeclared(t.node);
					if(temp!=null && t.scopes.contains(temp.decl_scope))//the var is declared at :
					{
						t.decl_id = temp.t_id;
						t.name = temp.decl_entry;
						t.varType = temp.varType;

					}
					else
					{
						//var is undeclared
						t.name = "U";
					}
				}
			}
			else if(t.node.type == NodeType.NAME)//a reference to a PROC
			{
				// System.out.println("handling proc: "+t.name+" at: " + t.t_id);
				TableEntry temp = isDeclared(t.node);
				System.out.println("handling proc: "+t.name+" at: " + t.t_id+" temp: "+(temp==null));

				if(temp!=null)//the var is declared at :
				{
					t.decl_id = temp.t_id;
					t.name = temp.decl_entry;
					t.varType = temp.varType;

				}
				else
				{
					//var is undeclared
					t.name = "U";
				}
			}
		}
	}

	public TableEntry isDeclared(ParseNode node)
	{
		TableEntry temp = null;
		int match = 0;
		int c = 0;
		for(TableEntry t : declerations) 					//var and var or proc and name
			if( ( (node.type==NodeType.VAR && t.node.type==node.type) || (node.type==NodeType.NAME && t.node.type==NodeType.PROC) ) 
				&& t.node.data.equals(node.data) && (c = countScopes(t,node))>match)
			{
				temp = t;
				match = c;
			}

		return temp;
	}

	int countScopes(TableEntry t, ParseNode n)
	{
		int temp = 0;
		for(Integer i: t.scopes)
			if(n.scopes.contains(i))
				temp++;

		return temp;
	}

	public TableEntry isDeclaredFor(ParseNode node)
	{
		for(TableEntry t : forLoopVars)
			if(t.node.data.equals(node.data) && t.node.parent==node.parent)
			{
				return t;
			}

		return null;
	}

	boolean isValid(TableEntry current)
	{
		//multiple declerations of the same name
		//in the same scope are not allowed!
		int count = 0, size = current.scopes.size();

		for(TableEntry t: declerations)
		{
			if(t.scopes.size()!=size)
				continue;

			if(!t.node.data.equals(current.node.data))
				continue;

			if(current.node.type!=t.node.type)
				continue;

			count = 0;

			for(Integer i: current.scopes)
			{
				if(!t.scopes.contains(i))
					break;

				count++;
			}

			if(count==size)
			{
				current.decl_entry = "duplicate declerations not allowed!";
				return false;
			}
		}			

		return true;

	}

	public void printTable(boolean withTypes)
	{
		System.out.println("-------------------\n"
						  +"  Semantic Table\n"
						  +"-------------------");

		for(TableEntry te : entries)
			if(!withTypes)
				System.out.println(te);
			else
				System.out.println(te.toTypeString());
	}

	public TableEntry getEntry(int i)
	{
		return entries.get(i);
	}

	public TableEntry getVarDecleration(ParseNode node)
	{
		Integer temp = entries.get(node.num).decl_id;
		if(temp!=null)
			return entries.get(temp);//return entry that has the decleration in
		else
			return null;
	}

	public class TableEntry
	{
		String name;//proc or var names 
		int nodeID;
		LinkedList<Integer> scopes;
		Integer decl_id = null; //id of entry that declares this entry
		int t_id;
		ParseNode node;
		String decl_entry = null;//name (v1) of the entry
		int decl_scope = -1;
		boolean isForVar = false;
		String varType;
		HasValue hv;// has a value or not
		int latestUpdateNum;
		int valueDepth = 0;
		LinkedList<TableEntry> updatedVars;

		public TableEntry(ParseNode node)
		{
			this.nodeID = node.num;
			t_id = SemanticTable.tableID++;
			this.scopes = node.scopes;
			this.node = node;
			// type = node.type+"";

			if(node.parent!=null && node.type == NodeType.VAR && node.parent.type == NodeType.DECL)
			{
				if(isValid(this))//prevent declerations of identical vars
				{
					decl_entry = "v"+varNum++;					
					decl_scope = node.scopes.get(0);
					varType = getType(node);
					hv = HasValue.NO;
					latestUpdateNum = t_id;
					updatedVars = new LinkedList<TableEntry>();
					updatedVars.add(this);
					declerations.add(this);
				}

			}
			else if(node.type == NodeType.PROC)
			{
				if(isValid(this))//prevent declerations of identical vars
				{
					decl_entry = "p"+procNums++;
					decl_scope = node.scopes.get(0);
					varType = "P";
					declerations.add(this);
				}
			}
			else if(node.parent!=null && node.parent.data.equals("for"))
			{
				if(forLoop)//first var from loop to be seen
				{
					forLoop=false;
					decl_scope = -2;
					decl_entry = "f"+forVarNum++;
					varType = "N";
					isForVar = true;
					forLoopVars.add(this);
				}
			}
			else if(node.type==NodeType.NUMEXPR)
			{
				varType = "N";
				hv = HasValue.YES;
			}


			// if(node.parent!=null)
			// {
			// 	if(node.parent.type == NodeType.COND_BRANCH)
			// 	{

			// 	}
			// 	else if(node.parent.type == NodeType.CONDLOOP)
			// 	{
					
			// 	}
			// }
			
			name = node.data;

			if(name.equals("for"))
				forLoop=true;
		} 

		String getType(ParseNode node)
		{
			if(node.parent.children.size()!=2)
				return "error getting node type";

			String type = "not a valid type";

			//other node is the type
			if(node.parent.children.get(0)==node)
			{
				type = node.parent.children.get(1).data+"";
			}
			else
				type = node.parent.children.get(0).data+"";

			if(type.contains("s"))
				type = "S";
			else if(type.contains("n"))
				type = "N";
			else if(type.contains("b"))
			 	type = "B";

			return type;
		}

		public TableEntry getLatest()
		{
			if(this.decl_id!=null)
				return getEntry(decl_id).updatedVars.getLast();

			return null; 
		}

		public TableEntry getLatestValid(int depth)
		{
			TableEntry var =null;
			if(this.decl_id!=null)
				var = getEntry(decl_id);
			else
				return null;

			for(int i = var.updatedVars.size()-1;i>=0;i--)
				if(var.updatedVars.get(i).valueDepth<=depth)
					return var.updatedVars.get(i);

			return var.updatedVars.get(0);//return decl entry
		}

		public boolean hasLatestValue()
		{
			return getEntry(decl_id).updatedVars.isEmpty();
		}

		public void update()
		{
			getEntry(decl_id).updatedVars.add(this);
		}

		public String toString()
		{
			String scopeStr = "";

			for(Integer i : this.scopes)
				scopeStr+=i+" ";

			String decl_id_str ="";
			if(decl_id!=null)
				decl_id_str = "delcared at: " + decl_id+"";
			else if(decl_entry==null && (node.type == NodeType.VAR || node.type == NodeType.PROC || node.type == NodeType.NAME))
				decl_id_str = "undeclared or out of scope";
			else if(decl_scope!=-1 && decl_entry!=null)
				decl_id_str = "declared: "+node.data;

			String nameStr = name;
			if(nameStr==null)
				nameStr = node.type+"";

			if(this.scopes.size()<4)
				return t_id + "\t|"+scopeStr + "\t\t|"+nameStr+"\t|"+ nodeID + "\t|" + decl_id_str;
			else
				return t_id + "\t|"+scopeStr + "\t|"+nameStr+"\t|"+ nodeID + "\t|" + decl_id_str;

		}

		public String toTypeString()
		{
			String scopeStr = "";

			for(Integer i : this.scopes)
				scopeStr+=i+" ";

			String decl_id_str="";
			if(decl_id!=null)
				if(decl_id>9)
					decl_id_str = "delcared at: " + decl_id;
				else
					decl_id_str = "delcared at: " + decl_id+" ";
			else if(decl_entry==null && (node.type == NodeType.VAR || node.type == NodeType.PROC || node.type == NodeType.NAME))
				decl_id_str = "undeclared or OoS";
			else if(decl_scope!=-1 && decl_entry!=null)
				decl_id_str = "declared: "+node.data;

			String varTypeStr="";
			if(varType!=null)
				varTypeStr+=varType;


			String nameStr = name;
			if(nameStr.equals("null"))
				nameStr = node.type+"";
			else if(decl_id!=null)
				nameStr +=" ("+node.data+")";

			return spaceData(t_id+"",1)+spaceData("|"+scopeStr,1)+spaceData("|"+nameStr,2)+spaceData("|"+decl_id_str,3)+spaceData("|"+varTypeStr,1);
		}

		public String toValueFlowString()
		{
			if(hv!=null)
				return toTypeString() + spaceData("|"+hv,1);
			else
				return toTypeString()+ spaceData("|",1);
		}

		public String toValueCheckSummary()
		{
			// String output = "";
			if(this.decl_id==null)//declaration var
			{
				if(this.node.type==NodeType.VAR || this.node.type==NodeType.NAME || this.node.type==NodeType.BOOL)
				{
					return this.spaceData(this.name+" ("+this.node.data+"):",2)+this.hv+"\n";
				}

				return "";
				
			}

			if(this.hv==null)
			{
				if(this.node.type != NodeType.NAME)
					return this.spaceData(this.name+" ("+this.node.data+"):",2)+"Error - not reachable!"+"\n";
				else
					return this.spaceData(this.name+" ("+this.node.data+"):",2)+"PROC CALL"+"\n";

			}

			// var = st.getEntry(te.decl_id);//decl var
			// var = st.getEntry(var.latestUpdateNum);

			switch (this.hv) {
				case MAYBE:
					return this.spaceData(this.name+" ("+this.node.data+"):",2)+"Error - Might not be initialized!\n";
				default:
					return this.spaceData(this.name+" ("+this.node.data+"):",2)+this.hv+"\n";
			}
		}

		public String spaceData(String input, int size)
		{	
			int inputSize;

			if(input.equals(""))
				inputSize = size;
			else
				inputSize = (int) (Math.floor((input.length())/8));

			int tabs = size - inputSize;

			String tabsString = "";
			for(int i = 0; i < tabs;i++)
				tabsString+="\t";

			return input+tabsString;
		}

		public TableEntry getLatestNotThis() {
			if(this.decl_id!=null)
				if(getEntry(decl_id).updatedVars.getLast()==this)
					if(getEntry(decl_id).updatedVars.size()>1)
						return getEntry(decl_id).updatedVars.get(getEntry(decl_id).updatedVars.size()-2);
				else
					return getEntry(decl_id).updatedVars.getLast();

			return null;
		}
	}

}
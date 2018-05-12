import java.util.Stack;
public class TypeChecker
{
	SemanticTable st;
	ParseNode root;

	public TypeChecker(SemanticTable semTable, ParseNode r)
	{
		st = semTable;
		root = r;

		System.out.println(checkTypes());
		st.printTable(true);
	}

	Stack<ParseNode> stack = new Stack<ParseNode>();
	public String checkTypes()
	{
		String output = "";

		ParseNode current;
		stack.push(root);
		Boolean status;

		while(!stack.isEmpty())
		{
			current = stack.pop();
			status = typeCheck(current);

			//if current node is a type check node
			if(status==null)
			{
				//null status means not a valid node to check
				for(int i = current.children.size()-1;i>=0;i--)
				{
					stack.push(current.children.get(i));
				}	
			}
			else if(status==false)//false
			{
				output+=current.printPruned()+ " -> " + "is not correctly typed!\n";
				continue;
			}
			else if(status)
			{
				continue;
			}			
		}

		return output;
	}

	Boolean typeCheck(ParseNode current)
	{
		switch(current.type)
		{
			case ASSIGN:
				return isAssignSameAsVar(current);
			case CALC:
				System.out.print("CALC ("+current.data+"): ");
				return isBothChildrenNumbers(current);
			case CONDLOOP:
				return isLoopTypedCorrectly(current);
			case COND_BRANCH:
				return isBranchTypedCorrectly(current);
			case BOOL:
				return isNodeABool(current);
			case NAME:
				return isNodeAProc(current);
		}

		return null;
	}

	boolean isNodeAProc(ParseNode node)
	{
		return (node.type==NodeType.NAME && st.getVarDecleration(node)!=null);
	}

	boolean isBranchTypedCorrectly(ParseNode node)
	{
		for(int i = 1; i < node.children.size();i++)
			stack.push(node.children.get(i));

		if(node.data.contains("t") || node.data.contains("s"))//then or else
		{
			stack.push(node.children.get(0));
			return true;
		}

		System.out.print("COND_BRANCH (IF): ");

		if(isNodeABool(node.children.get(0)))
		{
			st.getEntry(node.num).varType="B";
			System.out.println(" -> B");
			return true;
		}
		else
		{
			st.getEntry(node.num).varType="?";
			System.out.println("-> is not correctly typed!");
			return false;
		}
	}

	boolean isLoopTypedCorrectly(ParseNode node)
	{
		if(node.data.contains("f"))//for loop
		{
			System.out.print("CONDLOOP (FOR): ");
			SemanticTable.TableEntry child;
			boolean status = true;
			//check that every var child is of type N
			for(ParseNode n: node.children)
			{	
				child = st.getEntry(n.num);
				if(child.node.type==NodeType.VAR)
				{
					if(child.varType==null)
					{
						System.out.println(n.printPruned() + " -> is not declared!");
						status = false;
						continue;
					}

					if(!child.varType.equals("N"))
					{
						System.out.println(node.printPruned() + " -> is not correctly typed!");
						status = false;
					}					
				}
				else
					stack.push(n);
			}

			if(status)
			{
				System.out.println(node.printPruned() + " -> N");
				return true;
			}
			else
				return false;
		}
		else
		if(node.data.contains("w"))//while loop
		{
			boolean status;
			//fisrt child must evaluate to type B
			System.out.print("CONDLOOP (WHILE): ");
			if( isNodeABool(node.children.get(0)) )
			{
				st.getEntry(node.num).varType="B";
				System.out.println("-> B");

				status = true;
			}
			else
			{
				st.getEntry(node.num).varType="?";
				System.out.println(" -> ?");
				status = false;
			}

			for(int i = 1; i < node.children.size();i++)
				stack.push(node.children.get(i));

			return status;
		}
		else
		{
			System.out.println("Error typing CONDLOOP at: "+node.num);
			return false;
		}
	}

	boolean isBothChildrenTheSame(ParseNode node)
	{
		if(node.children.size()!=2)
			return false;

		ParseNode left = node.children.get(0);
		ParseNode right = node.children.get(1);
		SemanticTable.TableEntry temp = st.getVarDecleration(left);

		if(temp==null)
		{
			System.out.println(node.printPruned() + " -> has not been declared!");
			return false;
		}

		//left is first child
		if(temp.varType!=null)
		{
			if(temp.varType.equals("N"))
			{
				// System.out.println("left is N");
				return isNodeANumber(right);
			}

			if(temp.varType.equals("S"))
			{
				// System.out.println("left is S");
				return isNodeAString(right);
			}

			if(temp.varType.equals("B"))
			{
				// System.out.println("left is B");
				return isNodeABool(right);
			}

			System.out.println(node.printPruned()+" -> cannot match type!");

		}
		else
		{
			if(isNodeABool(left))
				return isNodeABool(right);

			if(isNodeANumber(left))
				return isNodeANumber(right);

			System.out.println(node.printPruned()+" -> cannt match to sibling!");
		}

		return false;
		
	}

	boolean isAssignSameAsVar(ParseNode node)
	{
		if(node.children.size()!=2)
			return false; 

		ParseNode left = node.children.get(0);
		ParseNode right = node.children.get(1);

		SemanticTable.TableEntry temp = st.getVarDecleration(left);

		if(temp==null)
		{
			System.out.println(left.printPruned()+" -> is not declared!");
			return false; //var is not declared
		}

		//left has to be a var - left is first child
		if(temp.varType==null)
		{
			System.out.println(left.printPruned() +" -> is not a var!");
			return false;
		}

		System.out.print("Assign: " + temp.varType);

		if(temp.varType.equals("N"))
		// if(isNodeANumber(left))
			if(isNodeANumber(right))
			{
				System.out.println(" -> N");
				st.getEntry(node.num).varType = "N = N";

				// st.getEntry(left.num).hv = st.getEntry(right.num).hv;
				return true;
			}

		if(temp.varType.equals("S"))
		// if(isNodeAString(left))
			if(isNodeAString(right))
			{
				System.out.println(" -> S");
				st.getEntry(node.num).varType = "S = S";

				// st.getEntry(left.num).hv = st.getEntry(right.num).hv;
				return true;
			}

		if(temp.varType.equals("B"))
		// if(isNodeABool(left))
			if(isNodeABool(right))
			{
				System.out.println(" -> B");
				st.getEntry(node.num).varType = "B = B";

				// st.getEntry(left.num).hv = st.getEntry(right.num).hv;
				return true;
			}

		System.out.println(" -> ? \n"+node.printPruned()+" -> type is not the same on both sides of assign!");
		st.getEntry(node.num).varType = temp.varType + " = ?";
		return false;
	}

	boolean isNodeABool(ParseNode node)
	{
		if(node.data.equals("T") || node.data.equals("F"))
		{
			st.getEntry(node.num).hv =  HasValue.YES;
			st.getEntry(node.num).varType = "B";
			return true;
		}

		if(node.data.equals("not"))
			if(!isNodeABool(node.children.get(0)))
				return false;

		SemanticTable.TableEntry temp;

		if(node.type==NodeType.VAR)
		{
			temp = st.getVarDecleration(node);
			if(temp==null || temp.varType==null || !temp.varType.equals("B"))
				return false;
		}
		else if(node.type==NodeType.BOOL)//is it an expr?
		{
			//is it an explicit < or > expr?
			if(node.data.equals(">")||node.data.equals("<"))
				if(isBothChildrenNumbers(node))
				{
					st.getEntry(node.num).varType = "B";
					return true;
				}
				else
				{	
					st.getEntry(node.num).varType = "?";
					return false;
				}

			if(node.data.equals("eq"))
			{
				if(isBothChildrenTheSame(node))
				{
					st.getEntry(node.num).varType="B";
					return true;
				}
				else
				{
					st.getEntry(node.num).varType="?";
					return false;
				}
			}

			if(!isBothChildrenBool(node))
				return false;
		}
		else//therefore it is incorrect
		{
			return false;
		}

		return true;
	}

	boolean isBothChildrenBool(ParseNode node)
	{
		//check if we have reached a terminal
		if(node.children.size()!=2)
			return true;

		ParseNode left = node.children.get(0);
		ParseNode right = node.children.get(1);
		boolean lb,rb;

		if ( isNodeABool(left) && isNodeABool(right) )
		// if(lb && rb)
		{
			st.getEntry(node.num).varType = "B";
			
			// if(st.getEntry(left.num).hv == st.getEntry(right.num).hv && st.getEntry(left.num).hv == HasValue.YES)
			// 	st.getEntry(node.num).hv=HasValue.YES;
			// else
			// 	st.getEntry(node.num).hv = HasValue.MAYBE;
			return true;
		}
		else
		{
			st.getEntry(node.num).varType = "?";
			return false;
		}
	}

	boolean isNodeAString(ParseNode node)
	{
		SemanticTable.TableEntry te = st.getEntry(node.num);
		if(te.varType!=null && te.varType.equals("S"))
			return true;

		if(node.type==NodeType.STR)
		{
			st.getEntry(node.num).hv = HasValue.YES;
			st.getEntry(node.num).varType = "S";
			return true;
		}

		SemanticTable.TableEntry temp = st.getVarDecleration(node);
		return ( temp!=null && temp.varType.equals("S") );
	}

	boolean isNodeANumber(ParseNode node)
	{
		SemanticTable.TableEntry te = st.getEntry(node.num);
		if(te.varType!=null && te.varType.equals("N"))
			return true;

		SemanticTable.TableEntry temp;
		//check left - child 0
		if(node.type==NodeType.VAR)
		{
			temp = st.getVarDecleration(node);
			// System.out.println(temp.node.printPruned());
			if(temp==null || temp.varType==null || !temp.varType.equals("N"))
			{
				System.out.println(node.printPruned() + " -> is not a number!");
				return false;
			}
		}
		else if(node.type==NodeType.CALC)//is it an expr?
		{
			if(!isBothChildrenNumbers(node))
				return false;
		}
		else if(node.type==NodeType.NUMEXPR)
		{
			st.getEntry(node.num).hv = HasValue.YES;
			return true;
		}
		else
			return false;

		return true;
	}

	boolean isBothChildrenNumbers(ParseNode node)
	{	
		//check if we have reached a terminal
		if(node.children.size()!=2)
			return true;

		ParseNode left = node.children.get(0);
		ParseNode right = node.children.get(1);

		if(isNodeANumber(left) && isNodeANumber(right))
		{
			// if(st.getEntry(left.num).hv == st.getEntry(right.num).hv && st.getEntry(left.num).hv == HasValue.YES)
			// 	st.getEntry(node.num).hv = HasValue.YES;
			// else
			// 	st.getEntry(left.num).hv = HasValue.MAYBE;

			return true;
		}
		else
		{
			// st.getEntry(node.num).hv = HasValue.NO;
			return false;
		}

	}

	void printTree(boolean verbose)
	{
		String output="";

		if(verbose)
		{
			for(SemanticTable.TableEntry t: st.entries)
				if(t.varType==null)
					output+=t.node.printWithTabs()+"\n";
				else
					output+=t.node.printWithTabs()+" -> " + t.varType+"\n";
		}
		else
		{
			for(SemanticTable.TableEntry t: st.entries)
				if(t.varType==null)
					output+=t.node.getTabs()+t.t_id+"|"+t.node.type+"|"+t.name+"\n";
				else
					output+=t.node.getTabs()+t.t_id+"|"+t.node.type+"|"+t.name+" -> "+t.varType+"\n";
		}


		System.out.println(output);
	}

}
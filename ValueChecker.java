import java.util.Stack;
public class ValueChecker
{
	SemanticTable st;
	ParseNode root;
	HasValue thv = HasValue.YES;//becomes MAYBE when working inside a loop or branch
	int currentDepth = 0;
	int preProcNode = -1;

	public ValueChecker(SemanticTable semTable, ParseNode r, boolean onlyNo)
	{
		st = semTable;
		root = r;
		System.out.println(checkValueFlow());
		printTable();
		printSummary();
		printTree(onlyNo);
		st.writeToFile();
	}

	Stack<ParseNode> stack = new Stack<ParseNode>();
	public String checkValueFlow()
	{
		String output = "";
		String temp = "";
		ParseNode current = null;
		stack.push(root);

		while(!stack.isEmpty())
		{
			temp="";
			current = stack.pop();
			// output+="\nChecking: "+current.toString()+"\n";

			//stop when we hit a halt
			if(current.data.equals("halt"))
				break;

			//if the node hasnt been value checked
			//this exlcudes procs that are already checked
			if(st.getEntry(current.num).hv == null)
				temp=valueCheck(current);

			if(temp.length()>0)
				output+=temp+"\n";

			//dont want to check a PROC if it is never called/b4 it is neccessary
			// if(current.type!=NodeType.PROC)
			// {
				//alright to add children to the stack!
				// System.out.println("adding children");
				// for(int i = current.children.size()-1;i>=0;i--)
				// {
				// 	stack.push(current.children.get(i));
				// }	
			// }



			if(!current.data.equals("for"))
				for(int i = current.children.size()-1;i>=0;i--)
				{
					stack.push(current.children.get(i));
				}

		}

		return output;
	}

	public String valueCheck(ParseNode current)
	{
		// does our current node have a value?
		if(st.getEntry(current.num).hv!=null)
		{
			System.out.println("Has value: "+current.toString()+"\n");
			return "";
		}

		if(current.type==NodeType.DECL)
			return "";

		System.out.println("Checking: "+current.toString()+"\n");

		switch(current.type)
		{
			case ASSIGN:
				return checkAssign(current);
			case CALC:
				// System.out.print("CALC ("+current.data+"): ");
				return checkCalc(current);
			case CONDLOOP:
				return checkLoop(current);
			case COND_BRANCH:
				return checkBranch(current);
			case BOOL:
				return checkBool(current);
			case VAR:
				return checkVar(current);
			case NAME:
				return checkProc(current);
			case IO:
				return checkIO(current);
		}

		return "";
	}

	String checkLoop (ParseNode node)
	{
		String divider = "";
		boolean forLoop = false;

		if(node.data.equals("for"))//for loop
		{
			forLoop = true;
			divider = "^^^^^^^";
		}
		else
			divider = "+++++++";

		String output = "\n"+divider+"\nLOOP at "+node.num+":    ("+node.data+")\n";

		//check condition if an if branch
		if(forLoop)
		{
			//3rd child is the value we check against
			st.getEntry(node.children.get(0).num).hv = HasValue.YES;
			st.getEntry(node.children.get(1).num).hv = HasValue.YES;
			st.getEntry(node.children.get(3).num).hv = HasValue.YES;
			st.getEntry(node.children.get(4).num).hv = HasValue.YES;
			output+=checkVar(node.children.get(2));
		}
		else
			output+=checkBool(node.children.get(0));


		currentDepth++;
		//check bodies
		// thv = HasValue.MAYBE;

		if(forLoop)
			for(int i = 5;i<node.children.size();i++)
				output += "\n\t"+valueCheck(node.children.get(i));
			// for(ParseNode n: node.children)
			// 	if(!st.getEntry(n.num).isForVar)
			// 		output += "\n\t"+valueCheck(n);
		else
			for(int i = 1;i<node.children.size();i++)
				output += "\n\t"+valueCheck(node.children.get(i));

		// thv = HasValue.YES;

		currentDepth--;

		output += "\n"+divider+"\n";
		return output;
	}

	String checkBranch(ParseNode node)
	{
		String divider = "";
		boolean ifBranch = false;

		if(node.data.equals("if"))
		{
			ifBranch = true;
			divider = "#######";
		}
		else
			divider = "*******";

		String output = "\n"+divider+"\nBRANCH at "+node.num+":    ("+node.data+")\n";

		//check condition if an if branch
		if(ifBranch)
			output+=checkBool(node.children.get(0));

		//check bodies
		// thv = HasValue.MAYBE;

		if(ifBranch)
			for(int i = 1;i<node.children.size();i++)
				output += "\n\t"+valueCheck(node.children.get(i));
		else
		{
			currentDepth++;

			SemanticTable.TableEntry ass ;
			// SemanticTable.TableEntry declVar ;
			SemanticTable.TableEntry updateVar ;

			//then or else
			for(ParseNode n: node.children)
			{
				// output += "\n\t"+valueCheck(n);


				// 	continue; //other var is not updated in the THEN branch
				if(node.data.equals("then"))
				{
					if(n.type==NodeType.NAME)
					{
						ParseNode proc = st.getVarDecleration(n).node;
						if(st.getEntry(proc.num).hv==null)
						for(ParseNode tn: proc.children) {
							tn.thenBranch = node;
						}
					}

					n.thenBranch = node;

					output += "\n\t"+valueCheck(n);

					continue;
				}

				//if in else branch - and see an assign or input - check if this happens in then branch - if yes update
				if(node.data.equals("else"))
				{
					if(n.type == NodeType.ASSIGN || (n.type == NodeType.IO && n.data.contains("i")))
					{
						ass = st.getEntry(n.num);

						updateVar = st.getEntry(n.children.get(0).num).getLatest();

						output += "\n\t"+valueCheck(n);

						//check if has got a value from the THEN branch - parent.parent == IF
						//if var is in both then and else it has a value in the depth-1 of the else
						if(updateVar.node.parent.parent.parent == ass.node.parent.parent && updateVar.node.parent.parent.data.equals("then"))
						{
							// ass.hv = HasValue.YES;
							// declVar.latestUpdateNum = var.t_id;
							st.getEntry(n.children.get(0).num).valueDepth-=1;
							// ass.update();
							output+="VAR ("+ass.name+", "+n.data+") is present in both sides of the condition -> "+ass.hv;
						}
					}
					else if(n.type == NodeType.NAME)
					{
						output+=valueCheck((n));
						ParseNode proc = st.getVarDecleration(n).node;

						//go through then branch looking for any input matches with variables in this else branch
						for (ParseNode pn: proc.children)
						{
							if(pn.type == NodeType.ASSIGN || (pn.type == NodeType.IO && pn.data.contains("i")))
							{
								ass = st.getEntry(pn.num);

								updateVar = st.getEntry(pn.children.get(0).num).getLatestNotThis();

								//check if has got a value from the THEN branch - parent.parent == IF
								//if var is in both then and else it has a value in the depth-1 of the else
								if(updateVar!=null && updateVar.node.parent.thenBranch!=null && updateVar.node.parent.thenBranch.parent == node.parent)
								{
									// ass.hv = HasValue.YES;
									// declVar.latestUpdateNum = var.t_id;
									st.getEntry(pn.children.get(0).num).valueDepth-=1;
//									 ass.update();
									st.getEntry(pn.children.get(0).num).update();

									output+="VAR ("+ass.name+", "+n.data+") is present in both sides of the condition -> "+ass.hv;
								}
							}
						}
					}
				}
				else
					output += "\n\t"+valueCheck(n);
			}

			currentDepth--;
		}

		// thv = HasValue.YES;

		output += "\n"+divider+"\n";
		return output;
	}

	String checkIO(ParseNode node)
	{
		String output="";
		ParseNode var = node.children.get(0);
		SemanticTable.TableEntry child = st.getEntry(var.num);
		//input or output
		if(node.data.contains("i"))//input
		{
			output+= "Input at: "+node.num;

			//input means var gets a value!
			// st.getEntry(var.num).hv = thv;
			child.hv = HasValue.YES;

			child.valueDepth = currentDepth;

			// st.getEntry(te.decl_id).latestUpdateNum = child.t_id;
			child.update();

			output+= " -> Setting latest update of " +child.name+ "("+var.data
			+") to input ("+HasValue.YES+")\n";

			// output+= "\n"+checkVar(node.children.get(0));
		}
		else//output
		{
			output+= "Output at: "+node.num;
			output+= " -> "+checkVar(var);

			if(child.getLatestValid(currentDepth).hv == HasValue.NO)
//			if(child.hv == HasValue.NO)
			{
				output+=" -> Error: output requires a var with a value!";
			}
			else
				output+=" -> Success";

			st.getEntry(node.num).hv = child.hv;
		}

		st.getEntry(node.num).hv = child.hv;
		return output;
	}



	String checkVar(ParseNode node)
	{
		//track which version of var is latest with a latestUpdate TableEntry
		//in the var's decl_id's vars object.
		//always use latest value of var when checking values

		SemanticTable.TableEntry var = st.getEntry(node.num);

		//beware of undeclared variables!

		if(var.name.equals("U"))
			return "VAR is UNDECLARED at "+node.num;

		if(var.isForVar)
			return "VAR ("+var.name+", "+node.data+") at "+node.num + " is a FOR VAR"; 
		
		if(node.type == NodeType.BOOL && (node.data.equals("T") || node.data.equals("F")))
			return "TRUTH ("+var.name+") at "+node.num + " -> "+var.hv; 


		// SemanticTable.TableEntry updateVar = st.getEntry(var.decl_id);//decl var

		// updateVar = st.getEntry(updateVar.latestUpdateNum);
		// System.out.println(var.name+ "("+var.node.data
		// 	+") depth: " +currentDepth);

		SemanticTable.TableEntry result = var.getLatestValid(currentDepth);
		if( result.valueDepth < currentDepth)
		{

		}

		var.hv = var.getLatestValid(currentDepth).hv;

		// var.hv = updateVar.hv;
		return "VAR ("+var.name+", "+node.data+") at "+node.num + " -> "+var.hv; 
	}

	String checkBool(ParseNode node)
	{
		String output = "BOOL at "+node.num+":";
		String temp = "";

		//a terminal bool - var etc
		if(node.children.size()==0)
			return checkVar(node);

		if(node.children.size()==1)//not
		{
			String out = checkBool(node.children.get(0));
			st.getEntry(node.num).hv = st.getEntry(node.children.get(0).num).hv;
			return out;
		}

		SemanticTable.TableEntry left = st.getEntry(node.children.get(0).num);
		SemanticTable.TableEntry right = st.getEntry(node.children.get(1).num);

		temp+="Left: "+valueCheck(left.node)+"\n";
		temp+="Right: "+valueCheck(right.node)+"\n";


		//set hv
		if(left.hv == HasValue.YES && right.hv==HasValue.YES)//left and right both yes
		{
			// st.getEntry(node.num).hv = thv;
			st.getEntry(node.num).hv = HasValue.YES;
		}
		else if((left.hv == HasValue.MAYBE || right.hv == HasValue.MAYBE)
			&& (left.hv != HasValue.NO && right.hv != HasValue.NO))
			st.getEntry(node.num).hv = HasValue.MAYBE;
		else
			st.getEntry(node.num).hv = HasValue.NO;
		

		output+=" -> "+st.getEntry(node.num).hv+"\n";

		return output += temp;
	}

	String checkCalc(ParseNode node)
	{
		String output = "CALC at "+node.num+":\n";
		String temp = "";

		SemanticTable.TableEntry left = st.getEntry(node.children.get(0).num);
		SemanticTable.TableEntry right = st.getEntry(node.children.get(1).num);

		temp+="Left: "+valueCheck(left.node)+"\n";
		temp+="Right: "+valueCheck(right.node)+"\n";

		//set hv
		if(left.hv == HasValue.YES && left.hv == right.hv)//left and right both yes
		{
			st.getEntry(node.num).hv = HasValue.YES;
			// st.getEntry(node.num).hv = thv;
		}
		else if((left.hv == HasValue.MAYBE || right.hv == HasValue.MAYBE)
			&& (left.hv != HasValue.NO && right.hv != HasValue.NO))
			st.getEntry(node.num).hv = HasValue.MAYBE;
		else
			st.getEntry(node.num).hv = HasValue.NO;

		output+=" -> "+st.getEntry(node.num).hv+"\n";

		return output += temp;
	}

	String checkProc(ParseNode node)
	{
		String output = "\n--------\nPROC at "+node.num+":    ("+node.data+")\n";
		//jump to proc

		if(st.getVarDecleration(node).hv!=null)
			return "";
		else
			st.getVarDecleration(node).hv=HasValue.PROC;

		System.out.println("inside PROC " + node.data);
		ParseNode procNode = st.getVarDecleration(node).node;

		//for every child in proc
		for(int i = 0;i<procNode.children.size();i++)
			output += "\n\t"+valueCheck(procNode.children.get(i));

		// st.getEntry(st.getEntry(node.num).decl_id).hv = HasValue.PROC;

		return output+"\n-------\n";

	}

	public String checkAssign(ParseNode node)
	{

		String output = "ASSIGN at "+node.num+": -> ";
		String temp = "";

		//var that is getting value
		ParseNode left = node.children.get(0);
		//var giving a value over
		ParseNode right = node.children.get(1);

		// output=valueCheck(right).concat(output);//print in correct order
		temp=valueCheck(right);//print in correct order
		
		// output+=setVals(st.getEntry(left.num),st.getEntry(right.num));	


		SemanticTable.TableEntry rightEntry = st.getEntry(right.num);
		SemanticTable.TableEntry leftEntry = st.getEntry(left.num);

		if(rightEntry.hv==HasValue.YES)
		{
			output = "Setting latest update of " +leftEntry.name+ "("+leftEntry.node.data
			+") to " + leftEntry.t_id +" -> "+HasValue.YES;
			leftEntry.hv = HasValue.YES;
		}
		else
		{	
			output = "VAR " +leftEntry.name+ "("+leftEntry.node.data
			+") to " + rightEntry.name+ "("+rightEntry.node.data
			+") -> ";
			leftEntry.hv = rightEntry.hv;
		}

		if(leftEntry.decl_id!=null)
			// st.getEntry(leftEntry.decl_id).latestUpdateNum = leftEntry.t_id;
			leftEntry.update();

		System.out.println(leftEntry.name+ "("+leftEntry.node.data
			+") depth: " +currentDepth);
		leftEntry.valueDepth = currentDepth;
		st.getEntry(node.num).hv = leftEntry.hv;

		output+=temp;

		return output;

	}

	void setVals(SemanticTable.TableEntry left, SemanticTable.TableEntry right)
	{
		//want to assign right to left

	}	


	public void printSummary()
	{
		String summary = "";

		for(SemanticTable.TableEntry te: st.entries)
			summary+=te.toValueCheckSummary();

		System.out.println(summary);
	}

	public void printTable()
	{
		System.out.println("----------------------------------------------\n"
						  +"  Semantic Table with Types and Value Flow\n"
						  +"----------------------------------------------");

		for(SemanticTable.TableEntry te : st.entries)
			System.out.println(te.toValueFlowString());
	}

	public void printTree(boolean onlyNo)
	{
		String output = "\n\n";
		for(SemanticTable.TableEntry t: st.entries)
			// if(t.varType==null)
			// 		output+=t.node.printWithTabs()+"\n";
			// else if(t.hv==HasValue.NO)
			// 	output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";
			// else if(t.hv==null)
			// 	output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";
			// else
			// 	output+=t.node.printWithTabs()+" -> " + t.varType+"\n";
			if(t.node.type==NodeType.BOOL || t.node.type == NodeType.VAR || t.node.type == NodeType.CALC)
			{
				if(onlyNo)
					if(t.hv==HasValue.NO)
						output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";
					else if(t.hv==null&& t.node.type != NodeType.PROC && t.node.type !=NodeType.NAME)
						output+=t.node.printWithTabs()+" -> " + t.varType+" -- Error: Not reachable\n";
					else
						output+=t.node.printWithTabs()+"\n";
				else if(t.hv==null&& t.node.type != NodeType.PROC && t.node.type !=NodeType.NAME)
					output+=t.node.printWithTabs()+" -> " + t.varType+" -- Error: Not reachable\n";
				else
					output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";
			}
			else if(t.varType!=null && !t.varType.equals("U") && t.node.type != NodeType.CONDLOOP && t.node.type != NodeType.COND_BRANCH)
			{
				if(onlyNo)
					if(t.hv==HasValue.NO)
						output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";
					else if(t.hv==null && t.node.type != NodeType.PROC && t.node.type !=NodeType.NAME)
						output+=t.node.printWithTabs()+" -> " + t.varType+" -- Error: Not reachable\n";
					else
						output+=t.node.printWithTabs()+"\n";
				else if(t.hv==null && t.node.type != NodeType.PROC && t.node.type !=NodeType.NAME)
					output+=t.node.printWithTabs()+" -> " + t.varType+" -- Error: Not reachable\n";
				else
					output+=t.node.printWithTabs()+" -> " + t.varType+" -- " +t.hv+"\n";	
			}
			else
				output+=t.node.printWithTabs()+"\n";


		System.out.println(output);
	}
}
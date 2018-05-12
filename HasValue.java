public enum HasValue{
	YES,NO,MAYBE,PROC;

	public String toString()
	{
		if(this == YES)
			return "Has a value!";

		if(this == NO)
			return "Has NO value!";
		if(this == PROC)
			return "Has been checked";

		return "MIGHT have a value!";
	}
}
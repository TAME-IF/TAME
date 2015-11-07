package net.mtrop.tame.lang;

/**
 * Arithmetic operators.
 * @author Matthew Tropiano
 */
public enum ArithmeticOperator
{
	ABSOLUTE (12, false, true)
	{ 
		@Override
		public Value doOperation(Value value1) 
		{
			return Value.absolute(value1);
		}
	},
	
	NEGATE (12, false, true)
	{  
		@Override
		public Value doOperation(Value value1) 
		{
			return Value.negate(value1);
		}
	},
	
	LOGICAL_NOT (12, false, true)
	{  
		@Override
		public Value doOperation(Value value1) 
		{
			return Value.logicalNot(value1);
		}
	},
	
	NOT (12, false, true)
	{  
		@Override
		public Value doOperation(Value value1) 
		{
			return Value.not(value1);
		}
	},
	
	ADD (10, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.add(value1, value2);
		}
	},
	
	SUBTRACT (10, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.subtract(value1, value2);
		}
	},
	
	MULTIPLY (11, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.multiply(value1, value2);
		}
	},
	
	DIVIDE (11, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.divide(value1, value2);
		}
	},
	
	MODULO (11, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.modulo(value1, value2);
		}
	},
	
	AND (6, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.and(value1, value2);
		}
	},
	
	OR (4, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.or(value1, value2);
		}
	},
	
	XOR (5, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.xor(value1, value2);
		}
	},
	
	LSHIFT (9, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.leftShift(value1, value2);
		}
	},
	
	RSHIFT (9, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.rightShift(value1, value2);
		}
	},
	
	RSHIFTPAD (9, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.rightShiftPadded(value1, value2);
		}
	},
	
	LOGICAL_AND (3, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.logicalAnd(value1, value2);
		}
	},
	
	LOGICAL_OR (1, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.logicalOr(value1, value2);
		}
	},
	
	LOGICAL_XOR (2, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.logicalXOr(value1, value2);
		}
	},
	
	EQUALS (7, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.equals(value1, value2);
		}
	},
	
	NOT_EQUALS (7, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.notEquals(value1, value2);
		}
	},
	
	STRICT_EQUALS (7, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.strictEquals(value1, value2);
		}
	},
	
	STRICT_NOT_EQUALS (7, true, false)
	{  
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.strictNotEquals(value1, value2);
		}
	},
	
	LESS (8, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.less(value1, value2);
		}
	},
	
	LESS_OR_EQUAL (8, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.lessOrEqual(value1, value2);
		}
	},
	
	GREATER (8, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.greater(value1, value2);
		}
	},
	
	GREATER_OR_EQUAL (8, true, false)
	{ 
		@Override
		public Value doOperation(Value value1, Value value2) 
		{
			return Value.greaterOrEqual(value1, value2);
		}
	},
	;
	
	private int precedence;
	private boolean binary;
	private boolean rightAssociative;
	private ArithmeticOperator(int precedence, boolean binary, boolean rightAssociative)
	{
		this.precedence = precedence;
		this.binary = binary;
		this.rightAssociative = rightAssociative;
	}
	
	/**
	 * Operator precedence.
	 * @return an operator precedence value. Higher has earlier ordering.
	 */
	public int getPrecedence()
	{
		return precedence;
	}
	
	/**
	 * IS this a binary operator?
	 * @return true if so, false if not.
	 */
	public boolean isBinary() 
	{
		return binary;
	}
	
	/**
	 * Is this operator right-associative?
	 * @return true if so, false if not.
	 */
	public boolean isRightAssociative()
	{
		return rightAssociative;
	}
	
	/**
	 * Performs the action on one value. 
	 * @param value the value
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public Value doOperation(Value value1)
	{
		throw new ArithmeticException("Not a unary operator.");
	}
	
	/**
	 * Performs the action on two values. 
	 * @param value1 the first value.
	 * @param value2 the second value.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public Value doOperation(Value value1, Value value2)
	{
		throw new ArithmeticException("Not a binary operator.");
	}
	
}
package net.mtrop.tame.lang;

import java.util.Iterator;

import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;

import com.blackrook.commons.linkedlist.Queue;

/**
 * A set of commands in one block.
 * @author Matthew Tropiano
 */
public class Block implements Statement, Iterable<Statement>
{
	/** List of statements. */
	private Queue<Statement> statementQueue; 

	/**
	 * Creates a new empty block.
	 */
	public Block()
	{
		this.statementQueue = new Queue<Statement>();
	}

	/**
	 * Adds a statement to the block.
	 * @param statement the statement to add.
	 */
	public void add(Statement statement)
	{
		statementQueue.add(statement);
	}
	
	@Override
	public Iterator<Statement> iterator()
	{
		return statementQueue.iterator();
	}

	/**
	 * @return the amount of statements in the block.
	 */
	public int getCount()
	{
		return statementQueue.size();
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response)
	{
		for (Statement statement : this)
			statement.execute(request, response);
	}
	
}

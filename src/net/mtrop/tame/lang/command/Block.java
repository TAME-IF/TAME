package net.mtrop.tame.lang.command;

import java.util.Iterator;

import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ExecutableType;

import com.blackrook.commons.linkedlist.Queue;

/**
 * A set of commands in one block.
 * @author Matthew Tropiano
 */
public class Block implements ExecutableType, Iterable<Command>
{
	/** List of statements. */
	private Queue<Command> statementQueue; 

	/**
	 * Creates a new empty block.
	 */
	public Block()
	{
		this.statementQueue = new Queue<Command>();
	}

	/**
	 * Adds a statement to the block.
	 * @param statement the statement to add.
	 */
	public void add(Command statement)
	{
		statementQueue.add(statement);
	}
	
	@Override
	public Iterator<Command> iterator()
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
	public void execute(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		for (Command command : this)
			command.execute(request, response);
	}
	
	@Override
	public String toString()
	{
		return statementQueue.toString();
	}
	
}

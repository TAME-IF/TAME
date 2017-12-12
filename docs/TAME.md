# TAME #
#### &copy; 2016-2017 Matthew Tropiano ####

## 1. What is TAME? ##

TAME stands for **T**ext **A**dventure **M**odule **E**ngine. It is a Virtual Machine that allows for the authoring of Interactive Fiction. Text Adventures are a genre of games that started in the early 80’s when multi-color graphics and synthesized sound were not widely available to home consumers, and this type of video game enabled gamers everywhere to experience new, fictional worlds though descriptive text and reading comprehension. 

Players would send commands to this machine via text input. Usually commands consisted of an action plus an object or two, such as:

	pick up lamp
	look at dog
	use key with door

...and so on, in order to communicate their next move. TAME provides a means by which the player of the game can communicate with the world, and handles the logic for interpreting the player’s commands and manipulating the current state of the world, and enables the writers of the world to create the framework by which this is possible.

## 2. Module Overview ##

A TAME program, or *Module*, is a complete set of TAME elements that make up the fictional world. The elements that make up a complete world in TAME are: *Actions*, *Worlds* (of which there are only one), *Players*, *Rooms*, and *Objects*.

### 2.1. Actions ###

*Actions* describe what a player can do in the world. They are also what the player types into the client in order for the engine to figure out what to do and what code to run and how to manipulate the context. There are five different action types recognized by TAME: General, Modal, Transitive, Ditransitive, and Open.

**General** Actions are objectless actions. The player can type these into the interpreter without an object attached to it. Examples of possible "general" actions are "help," "yell," "run," "look around," and so forth – ones that don’t have an explicit target. These are handled by Players, Rooms, and the World.

**Modal** Actions are actions with a distinct set of valid targets. An example of a "modal" action would be "go" with the valid targets "west," "north," "south," and "east," and nothing else. These too are handled by Players, Rooms, and the World.

**Transitive** Actions are actions that target a single object in the world in the current tangible space. For example, "examine" could be a transitive action, because you can "examine the bookcase" or "examine table" or "examine your zipper." Transitive actions are handled by objects.

**Ditransitive** Actions are actions that target one or two objects. For example, "use" can be a "ditransitive" action because you can "use book" or "use key with door". Ditransitive actions are handled by objects, sometimes by more than one.

**Open** Actions are like Modal actions, except they can have any target. In fact, these have to be handled exclusively in programming. They are handled by Rooms, Players, or the World, but there are no "automatic" handlers for deciding what to do with the responses (because obviously, it can be anything). Their targets are passed as a value, specifically the one that was typed in by the player.

### 2.2. Players ###

A Player in TAME is generally a viewpoint in the World. Only one can be "current" at a time, and serves as the context’s "window" into the fictional microverse. When a Player is designated as current, or when one gains "focus," according to the command that grants it in the language, it is considered the semantic vehicle through which all of the actions take place. In other words, it is the avatar through which the game player acts in the world. 

A module can have no Players, or no current Players selected, and all of the actions would be handled by the World instead of "Player first, World second." Players can also hold Objects. 

Writers of a TAME module can also restrict what Actions a Player can use.

### 2.3. Rooms ###

A Room in TAME describes where Players can go in the world. The current Room (or the room with "focus") is associated with a Player, so switching Players can potentially switch the focus of the current Room. Just like Players are an abstraction of a viewpoint, Rooms are an abstraction of a localized container for a group of Objects or for a separate area of focus. For example, a "Living Room" is quite literally a "room" and an actual container, but a close-up of a multi-page book can also be a type of "room," since you may be able to do specific things within the book that you can’t do in other "rooms" (like turn pages or read the current page). Rooms, of course, can also hold Objects. 

Writers of a TAME module can also restrict what Actions can take place in a Room.

### 2.4. Objects ###

Objects are the primary interaction points in TAME. Objects can be owned by the World, Players, or Rooms, but not more than one container at a time (or none at all). These are the potential targets in Transitive and Ditransitive Actions. 

An Object is considered "accessible" if it is owned by the World, the current focused Player, or the current focused Room. The author of a TAME module does not need to worry about accessibility of an object – this is handled internally.

### 2.5. Worlds ###

Every module has a single instance of the World. The World is the topmost level of the module, and all actions that are not handled by Players and Rooms are handled here. The World holds the global state, and everything in the world can be seen by everything. 

Objects placed in the World are accessible from anywhere. There is no such thing as a world-less module.

## 3. The Language ##

The language used by TAME is what makes creating these worlds possible. Using the TAME language, all Actions, Players, Rooms, Objects, and the World can be described and fleshed out to the extent that the author wants or needs. Actions can be abstract – you aren’t limited to "look at," "take," or any other "built in" actions that other engines may offer, but if you don’t define them, they aren’t available to the player (that also includes "quitting" TAME – you will have to define an action for allowing the player to quit your game).

TAME’s language is very similar to most "curly brace" languages, like C/C++ or Java. Its expression syntax for describing arithmetic operations is infix, which is a fancy way of saying "like how humans write it." Like C, statements are terminated by semicolons, and command functions are keywords followed by a list of arguments in parenthesis. Unlike C, however (and other various languages), all keywords, commands, and variables are NOT CASE-SENSITIVE.

In order to showcase an example World in TAME, here is the most common program type of them all: "Hello, World!"

	world
	{
	    init()
    	{
        	textln("Hello, World!");
			quit;
    	}
	}

All TAME Clients, when running this module, should print "Hello, World!" to the screen on its own line, and then tell the client to terminate input. That’s it. The above program features the World declaration, its name, and the entry point executed when TAME loads the module. And since "quit" is called in the initializing function, the player never gets a chance to respond once TAME attempts to give control back to the player.

TAME’s language is compiled before it is executed. The language is turned into a series of abstract bytes by a compiler to read later by the engine. Compiling reduces the potential size of a completed module and obfuscates the content.


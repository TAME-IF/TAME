/**
 * TAMEScript Syntax Highlighter for SyntaxHighlighter by Alex Gorbatchev
 * @copyright
 * Matt Tropiano (C) 2017
 */
;(function()
{
	function Brush()
	{
		var keywords = 
			'if else while for quit end break continue module world room player object ' + 
			'container action general modal transitive ditransitive open restricted named ' + 
			'tagged modes uses conjunctions determiners forbids allows local clear archetype '+
			'function return';
		
		var commands = 
			'NOOP QUEUEACTION QUEUEACTIONSTRING QUEUEACTIONOBJECT ' + 
			'QUEUEACTIONFOROBJECTSIN QUEUEACTIONFORTAGGEDOBJECTSIN QUEUEACTIONOBJECT2 ADDCUE ' + 
			'TEXT TEXTLN TEXTF TEXTFLN PAUSE WAIT TIP INFO ASBOOLEAN ASINT ASFLOAT ASSTRING ' + 
			'STRLENGTH STRCONCAT STRREPLACE STRREPLACEPATTERN STRREPLACEPATTERNALL STRINDEX ' + 
			'STRLASTINDEX STRCONTAINS STRCONTAINSPATTERN STRCONTAINSTOKEN STRSTARTSWITH STRENDSWITH ' + 
			'SUBSTRING STRLOWER STRUPPER STRCHAR STRTRIM FLOOR CEILING ROUND FIX SQRT PI E SIN COS ' + 
			'TAN MIN MAX CLAMP RANDOM FRANDOM GRANDOM TIME SECONDS MINUTES HOURS DAYS FORMATTIME ' + 
			'OBJECTHASNAME OBJECTHASTAG ADDOBJECTNAME ADDOBJECTTAG ADDOBJECTTAGTOALLIN REMOVEOBJECTNAME ' + 
			'REMOVEOBJECTTAG REMOVEOBJECTTAGFROMALLIN GIVEOBJECT REMOVEOBJECT MOVEOBJECTSWITHTAG ' + 
			'OBJECTCOUNT HASOBJECT OBJECTHASNOOWNER PLAYERISINROOM PLAYERCANACCESSOBJECT BROWSE ' + 
			'BROWSETAGGED SETPLAYER SETROOM PUSHROOM POPROOM SWAPROOM CURRENTPLAYERIS NOCURRENTPLAYER ' + 
			'CURRENTROOMIS NOCURRENTROOM IDENTITY';
		
		var entrypoints = 
			'INIT AFTERREQUEST AFTERMODULEINIT PROCEDURE ONACTION ONACTIONWITH ' + 
			'ONACTIONWITHOTHER ONMODALACTION ONWORLDBROWSE ONROOMBROWSE ONPLAYERBROWSE ONCONTAINERBROWSE ' + 
			'ONAMBIGUOUSACTION ONBADACTION ONINCOMPLETEACTION ONUNKNOWNACTION ONFAILEDACTION ' + 
			'ONFORBIDDENACTION ONROOMFORBIDDENACTION';
			
		var otherConstants = 'true false Infinity NaN';
		
		this.regexList = [
			{ regex: /^\s*#.*/gm, 											css: 'preprocessor' },	// preprocessor tags
			{ regex: SyntaxHighlighter.regexLib.multiLineCComments, 		css: 'comments' },
			{ regex: SyntaxHighlighter.regexLib.singleLineCComments, 		css: 'comments' },
			{ regex: SyntaxHighlighter.regexLib.doubleQuotedString, 		css: 'string' },
			{ regex: new RegExp(this.getKeywords(keywords), 'gmi'), 		css: 'keyword' },
			{ regex: new RegExp(this.getKeywords(commands), 'gmi'), 		css: 'color2' },
			{ regex: new RegExp(this.getKeywords(entrypoints), 'gmi'), 		css: 'color1' },
			{ regex: new RegExp(this.getKeywords(otherConstants), 'gmi'), 	css: 'constants' },
			{ regex: /[0-9][0-9]*\.?[0-9]*([Ee][+-]?[0-9]+)?/gm, 			css: 'constants' },		// numeric constants
			{ regex: /0[Xx][0-9A-Fa-f][0-9A-Fa-f]*/gm, 						css: 'constants' }		// hexadecimal constants
		];
	};

	Brush.prototype	= new SyntaxHighlighter.Highlighter();
	Brush.aliases	= ['tamescript', 'tame'];

	SyntaxHighlighter.brushes.TameScript = Brush;
})();

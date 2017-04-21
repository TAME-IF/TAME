/*
	TAMEScript Highlighter for PrismJS
	(C) Matt Tropiano 2017
*/

Prism.languages.tamescript = {
	'comment': [
		{
			pattern: /(^|[^\\])\/\*[\w\W]*?\*\//,
			lookbehind: true
		},
		{
			pattern: /(^|[^\\:])\/\/.*/,
			lookbehind: true
		}
	],
	'string': {
		pattern: /(["'])(\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/,
		greedy: true
	},
	'entry-name': /\b(INIT|AFTERREQUEST|START|ON(ACTION(WITH(OTHER)?)?|MODALACTION|WORLDBROWSE|ROOMBROWSE|PLAYERBROWSE|CONTAINERBROWSE|AMBIGUOUSACTION|BADACTION|INCOMPLETEACTION|UNKNOWNACTION|FAILEDACTION|FORBIDDENACTION|ROOMFORBIDDENACTION))\b/i,
	'function': /\b(NOOP|QUEUE(ACTION(FOR(TAGGEDOBJECTSIN|OBJECTSIN)?|STRING|OBJECT(2)?)?)|ADDCUE|TEXT(F(LN)?|LN)|PAUSE|WAIT|TIP|INFO|AS(BOOLEAN|INT|FLOAT|STRING)|STR(LENGTH|CONCAT|REPLACE(PATTERN(ALL)?)?|INDEX|LASTINDEX|CONTAINS(PATTERN|TOKEN)?|STARTSWITH|ENDSWITH|LOWER|UPPER|CHAR|TRIM)|SUBSTRING|FLOOR|CEILING|ROUND|FIX|SQRT|PI|E|SIN|COS|TAN|MIN|MAX|CLAMP|RANDOM|FRANDOM|GRANDOM|TIME|SECONDS|MINUTES|HOURS|DAYS|FORMATTIME|OBJECT(HAS(NAME|TAG|NOOWNER)?|COUNT)|ADDOBJECT(NAME|TAG(TOALLIN)?)|REMOVEOBJECT(NAME|TAG(FROMALLIN)?)?|GIVEOBJECT|MOVEOBJECTSWITHTAG|HASOBJECT|PLAYER(ISINROOM|CANACCESSOBJECT)|BROWSE(TAGGED)?|SET(PLAYER|ROOM)|PUSHROOM|POPROOM|SWAPROOM|CURRENT(PLAYERIS|ROOMIS)|NOCURRENT(PLAYER|ROOM)|IDENTITY)/i,
	'keyword': /\b(if|else|while|for|quit|end|break|continue|module|world|room|player|object|container|action|general|modal|transitive|ditransitive|open|restricted|named|tagged|modes|uses|conjunctions|determiners|forbids|allows|local|clear|archetype|function|return)\b/i,
	'boolean': /\b(true|false|Infinity|NaN)\b/i,
	'number': /\b-?(?:0x[\da-f]+|\d*\.?\d+(?:e[+-]?\d+)?)\b/i,
	'operator': /(\(|\)|\[|\]|:|;|\.|,|\+|\-|\!|~|\*\*?|\/|%|&\||\^|<<?|<=?|>>?>?|>=?|==?=?|\!==?)/
};

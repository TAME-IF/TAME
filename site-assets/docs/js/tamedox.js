var $Q1 = $Q1 || function(x)
{
	return document.querySelector(x);
}

var BodyElement = $Q1("body");
var DocsSidebar = $Q1("#tamedox-sidebar");

function tamedoxOpenBar()
{
	DocsSidebar.style.display = "block";
}

function tamedoxCloseBar() 
{
	DocsSidebar.style.display = "none";
}

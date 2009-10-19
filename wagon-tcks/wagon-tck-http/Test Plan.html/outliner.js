var img_expanded  = 'Expanded.png';
var img_collapsed = 'Collapsed.png';
var img_blank = 'blank.png';
var img_leaf = 'LeafRowHandle.png';

new Image(9,9),src = img_expanded; // caching
new Image(9,9),src = img_collapsed; // caching
new Image(9,9),src = img_blank; // caching
new Image(9,9),src = img_leaf; // caching

function hover(iNode, over) {

    if (over) {
        t = document.getElementById(iNode).alt;
        
        if (t == '*') {
            document.getElementById(iNode).src=img_leaf;
        } else if (t == 'V') {
            document.getElementById(iNode).src=img_expanded;
        } else {
            document.getElementById(iNode).src=img_collapsed;
        }
    
    } else {
        document.getElementById(iNode).src=img_blank;
    }
}

function expand(ioNode) {
	ioWedge = "i" + ioNode.substr(1);

	if (document.getElementById && document.getElementById(ioNode) !=  null) {

		document.getElementById(ioNode).className='expanded';

		if (document.getElementById(ioWedge) !=  null) {		
			document.getElementById(ioWedge).src=img_expanded;
			document.getElementById(ioWedge).title='collapse';
			document.getElementById(ioWedge).alt='V';
		}
	}
}

function collapse(ioNode) {
	ioWedge = "i" + ioNode.substr(1);

	if (document.getElementById && document.getElementById(ioNode) != null) {

		document.getElementById(ioNode).className='collapsed';

		if (document.getElementById(ioWedge) !=  null) {		
			document.getElementById(ioWedge).src=img_collapsed;
			document.getElementById(ioWedge).title='expand';
			document.getElementById(ioWedge).alt='>';
		}
	}
}

function ioSwitch(ioNode,fully) {

	if (document.getElementById && document.getElementById(ioNode) !=  null) {
		nodeState = document.getElementById(ioNode).className;
	}

    if (nodeState == 'collapsed') {
        if (fully) {
            expandAll();
        } else {
    		expand(ioNode);
        }
	}

	else {
        if (fully) {
            collapseAll();
        } else {
    		collapse(ioNode);
        }
	}
}

function expandAll() {

	if (document.getElementsByTagName) {
		nodeList = document.getElementsByTagName('div');

		for (var i = 0; i < nodeList.length; i++) {
	
			if (nodeList.item(i).className == 'expanded' || nodeList.item(i).className == 'collapsed') {
				expand(nodeList.item(i).id);	
			}
		}
	}

	else {
		alert ("Sorry, don't know how to make this run in your browser.");
	}
}
function collapseAll() {

	if (document.getElementsByTagName) {
		nodeList = document.getElementsByTagName('div');

		for (var i = 0; i < nodeList.length; i++) {
	
			if (nodeList.item(i).className == 'expanded' || nodeList.item(i).className == 'collapsed') {
				collapse(nodeList.item(i).id);	
			}
		}
	}

	else {
		alert ("Sorry, don't know how to make this run in your browser.");
	}
}

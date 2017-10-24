var r = 20;
var paused = 0;

function drawCircle(ctx, x, y, color) {
	ctx.beginPath();
	ctx.arc(x, y, r, 0, 2 * Math.PI);
	ctx.fillStyle = '#' + color;
	ctx.fill();
	ctx.stroke();
	ctx.fillStyle = 'black';
}

function drawSock(ctx, x, y, color) {
	ctx.beginPath();

	ctx.beginPath();
	ctx.moveTo(x - 15, y - 15);
	ctx.lineTo(x, y - 15);
	ctx.quadraticCurveTo(x, y, x + 20, y);
	ctx.quadraticCurveTo(x + 30, y + 5, x + 20, y + 10);
	ctx.lineTo(x, y + 10);
	ctx.quadraticCurveTo(x - 15, y + 7, x - 10, y);
	// ctx.lineTo(x - 5, y - 5);
	ctx.closePath();
	ctx.fillStyle='#' + color;
	ctx.fill();
	ctx.stroke();
	ctx.fillStyle='black';

}

function drawArrow(ctx, fromx, fromy, tox, toy) {
    var headlen = 10;   // length of head in pixels
    var angle = Math.atan2(toy-fromy,tox-fromx);
    ctx.beginPath();
    ctx.moveTo(fromx, fromy);
    ctx.lineTo(tox, toy);
    ctx.lineTo(tox-headlen*Math.cos(angle-Math.PI/6),toy-headlen*Math.sin(angle-Math.PI/6));
    ctx.moveTo(tox, toy);
    ctx.lineTo(tox-headlen*Math.cos(angle+Math.PI/6),toy-headlen*Math.sin(angle+Math.PI/6));
    ctx.stroke();
}

function drawBoldArrow(ctx, fromx, fromy, tox, toy) {
    var headlen = 10;   // length of head in pixels
    var angle = Math.atan2(toy-fromy,tox-fromx);
    ctx.beginPath();
    ctx.lineWidth=4;
    ctx.moveTo(fromx, fromy);
    ctx.lineTo(tox, toy);
    ctx.lineTo(tox-headlen*Math.cos(angle-Math.PI/6),toy-headlen*Math.sin(angle-Math.PI/6));
    ctx.moveTo(tox, toy);
    ctx.lineTo(tox-headlen*Math.cos(angle+Math.PI/6),toy-headlen*Math.sin(angle+Math.PI/6));
    ctx.strokeStyle='#FF0000';
    ctx.stroke();
    ctx.lineWidth=1;
    ctx.strokeStyle='#000000';
}

function process(data) {
	// data: refresh,turn,p,transactions,[name,score,RGB1,RGB2),request1(id,rank),request2(id,rank)],[transaction(id1,rank1,id2,rank2)]
	var terms = data.split(",");
	var refresh = Number(terms[0]);
	var turn = Number(terms[1]);
	var p = Number(terms[2]);
	var trans = Number(terms[3]);

	var offers = new Array(p);
	var names = new Array(p);
	var scores = new Array(p);
	var requests = new Array(p);
	var transactions = new Array(trans);

	var i, j = 4;
	for (i = 0; i < p; ++ i) {
		names[i] = terms[j]; ++ j;
		scores[i] = terms[j]; ++ j;
		offers[i] = new Array(2);
		offers[i][0] = terms[j]; ++ j;
		offers[i][1] = terms[j]; ++ j;
		requests[i] = new Array(2);
		requests[i][0] = {};
		requests[i][0].id = Number(terms[j]); ++ j;
		requests[i][0].rank = Number(terms[j]) - 1; ++ j;
		requests[i][1] = {};
		requests[i][1].id = Number(terms[j]); ++ j;
		requests[i][1].rank = Number(terms[j]) - 1; ++ j;
	}
	for (i = 0; i < p; ++ i) {
		transactions[i] = new Array(2);
		transactions[i][0] = {};
		transactions[i][0].id = Number(terms[j]); ++ j;
		transactions[i][0].rank = Number(terms[j]) - 1; ++ j;
		transactions[i][1] = {};	
		transactions[i][1].id = Number(terms[j]); ++ j;
		transactions[i][1].rank = Number(terms[j]) - 1; ++ j;
	}

	var canvas = document.getElementById("canvas");
	var ctx = canvas.getContext("2d");
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	document.getElementById("turn").innerHTML = "Turn: " + turn;
	ctx.font = "20px Ariel"

	var r1 = 180, r2 = 280, r3 = 320, r4 = 620;
	var c1 = 20;

	var cgap = 100, ctop, cmid, cbot;

	ctx.fillText("Embarrassment", 20, c1);
	ctx.fillText("Player", r1 + 0, c1);
	ctx.fillText("Offers", r2, c1);
	ctx.fillText("Requests", r4 - 10, c1);




	for (i = 0; i < p; ++ i) {
		ctop = cgap * (i) + c1;
		cbot = cgap * (i + 1) + c1;
		cmid = (ctop + cbot) * 0.5;
		//draw names and scores
		ctx.fillText(scores[i], 50, cmid);
		ctx.fillText(names[i], r1 + 10, cmid);
		// draw offers
		if (offers[i][0] != 'no')
			drawSock(ctx, (r2 + r3) * 0.5, (ctop + cmid) * 0.5 + 5, offers[i][0]);
		if (offers[i][1] != 'no')
			drawSock(ctx, (r2 + r3) * 0.5, (cbot + cmid) * 0.5 - 5, offers[i][1]);

		// draw requests
		// console.log(requests[i]);
		if (requests[i][0].id >= 0) {
			drawSock(ctx, r4 + 20, (ctop + cmid) * 0.5 + 5, offers[requests[i][0].id][requests[i][0].rank]);
			var ry = requests[i][0].id * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * requests[i][0].rank) + requests[i][0].rank * cgap * 0.5;
			drawArrow(ctx, r3 + 2, ry, r4 - 2, (ctop + cmid) * 0.5 + 5);

			ctx.fillText("from " + names[requests[i][0].id], r4 + 70, (ctop + cmid) * 0.5 + 5);
		}
		if (requests[i][1].id >= 0) {
			drawSock(ctx, r4 + 20, (cbot + cmid) * 0.5 - 5, offers[requests[i][1].id][requests[i][1].rank]);
			var ry = requests[i][1].id * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * requests[i][1].rank) + requests[i][1].rank * cgap * 0.5;
			drawArrow(ctx, r3 + 2, ry, r4 - 2, (cbot + cmid) * 0.5 - 5);

			ctx.fillText("from " + names[requests[i][1].id], r4 + 70, (cbot + cmid) * 0.5 - 5);
		}
	}

	var id1, id2, rank1, rank2;
	var fromx, fromy, tox, toy;
	for (i = 0; i < trans; ++ i) {
		id1 = transactions[i][0].id, rank1 = transactions[i][0].rank;
		id2 = transactions[i][1].id, rank2 = transactions[i][1].rank;
		// redraw offer
		drawSock(ctx, (r2 + r3) * 0.5, id1 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * rank1) + rank1 * cgap * 0.5, offers[id2][rank2]);
		drawSock(ctx, (r2 + r3) * 0.5, id2 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * rank2) + rank2 * cgap * 0.5, offers[id1][rank1]);
		
		// Draw id1 line
		tox = r3 + 2; fromx = r4 - 2;
		toy = id1 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * rank1) + rank1 * cgap * 0.5;
		if (id2 == requests[id1][0].id && rank2 == requests[id1][0].rank)
			fromy = id1 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * 0) + 0 * cgap * 0.5;
		else
			fromy = id1 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * 1) + 1 * cgap * 0.5;
		drawBoldArrow(ctx, fromx, fromy, tox, toy);

		// Draw id2 line
		toy = id2 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * rank2) + rank2 * cgap * 0.5, offers[id2][rank2];
		if (id1 == requests[id2][0].id && rank1 == requests[id2][0].rank)
			fromy = id2 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * 0) + 0 * cgap * 0.5;
		else
			fromy = id2 * cgap + c1 + cgap * 0.25 + 5 * (1 - 2 * 1) + 1 * cgap * 0.5;
		drawBoldArrow(ctx, fromx, fromy, tox, toy);
	}

	
	return refresh;
}

var latest_version = -1;

function ajax(version, retries, timeout) {
	console.log("Version " + version);
	var xhttp = new XMLHttpRequest();
	xhttp.onload = (function() {
			var refresh = -1;
			try {
				if (xhttp.readyState != 4)
					throw "Incomplete HTTP request: " + xhttp.readyState;
				if (xhttp.status != 200)
					throw "Invalid HTTP status: " + xhttp.status;
				//console.log(xhttp.responseText);
				refresh = process(xhttp.responseText);
				//console.log(refresh);
				if (latest_version < version && paused == 0)
					latest_version = version;
				else refresh = -1;
			} catch (message) {
				alert(message);
			}
			if (refresh >= 0)
				setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
		});
	xhttp.onabort = (function() { location.reload(true); });
	xhttp.onerror = (function() { location.reload(true); });
	xhttp.ontimeout = (function() {
			if (version <= latest_version)
				console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
			else if (retries == 0)
				location.reload(true);
			else {
				console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
				ajax(version, retries - 1, timeout * 2);
			}
		});
	xhttp.open("GET", "data.txt", true);
	xhttp.responseType = "text";
	xhttp.timeout = timeout;
	xhttp.send();
}

function pause() {
	paused = 1 - paused;
}

ajax(1, 10, 100)
// var data="100,1,2,1,g0,100.0,FF0000,00FFFF,1,1,-1,-1,g1,200.21,0000FF,no,0,1,0,2,0,2,1,1"
//var data = "12,1,0,2,1,11,0,3,2,3,4,0,0,0,0,0,4,9,10,8,7,1,5,1,6,0";

// process(data);

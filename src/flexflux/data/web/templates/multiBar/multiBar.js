
//var str="name,essential,dispensable,dead\nc1,12,14,3\nc2,4,15,17\n"

// var data = [{key:"essential", values:[{x:"c1", y:12}, {x:"c2", y:2}]}, {key:"dispensable", values:[{x:"c1", y:14}, {x:"c2", y:4}]}];


var essential = [];
var dispensable = [];
var dead = [];

data = d3.csv.parse(str);

data.forEach(function(d) {
  
  console.log(d);
  
  var name = d.name;
 
  var tabEssential = {};
  tabEssential['x'] = name
  tabEssential['y'] = +d.essential;
  
  var tabDispensable = {};
  tabDispensable['x'] = name
  tabDispensable['y'] = +d.dispensable;
  
  var tabDead = {};
  tabDead['x'] = name
  tabDead['y'] = +d.dead;
  
  
  essential.push(tabEssential);
  dispensable.push(tabDispensable);
  dead.push(tabDead);
  
});

console.log("essential");
console.log(essential);


var geneTable = [{key:"essential", values:essential}, {key:"dispensable", values:dispensable}, {key:"dead", values:dead}];


console.log(geneTable);

nv.addGraph(function() {
    var chart = nv.models.multiBarChart()
      .transitionDuration(350)
      .reduceXTicks(false)   //If 'false', every single x-axis tick label will be rendered.
      .rotateLabels(-90)      //Angle to rotate x-axis labels.
      .showControls(true)   //Allow user to switch between 'Grouped' and 'Stacked' mode.
      .groupSpacing(0.1)    //Distance between each group of bars.
      .stacked(true)
    ;
    
    chart.margin({bottom: 100})
    
    
    

//     chart.xAxis
//         .tickFormat(d3.format(',f'));

    chart.yAxis
        .tickFormat(d3.format(',.1f'));    
	
	
    d3.select('#chart1 svg')
        .datum(geneTable)
        .call(chart);

    nv.utils.windowResize(chart.update);

    return chart;
});

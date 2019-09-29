// Import the messaging module
import * as messaging from "messaging";
//import * as neural from "./neural_stage_new.js"


var hrHistory=[];  //buffers for historical data
var gyroHistory=[];
var accHistory=[];
var hrLong=[];



//stats functions

function standardDeviation(values){
  var avg = average(values);
  
  var squareDiffs = values.map(function(value){
    var diff = value - avg;
    var sqrDiff = diff * diff;
    return sqrDiff;
  });
  
  var avgSquareDiff = average(squareDiffs);

  var stdDev = Math.sqrt(avgSquareDiff);
  return stdDev;
}

function average(data){
  var sum = data.reduce(function(sum, value){
    return sum + value;
  }, 0);

  var avg = sum / data.length;
  return avg;
}



//Event handler--called when new data received from fitbit
messaging.peerSocket.onmessage = function(evt) {
  //console.log(JSON.stringify(evt.data));
  let data=evt.data;
  fetch("http://127.0.0.1:8085/rawdata?data="+encodeURIComponent(JSON.stringify(data)));  //send fitbit raw data to local server on port 8085

  //send fitbit data to streatming server.
  fetch("https://biostream-1024.appspot.com/sendps?user=fitbitstream&data="+JSON.stringify(data));  //todo: let user specify username
  
  
  
  //neural network code--the sleep staging network is currently under development
  
  hrHistory.push(data.hr);
  hrLong.push(data.hr);
  accHistory.push(data.accx);
  gyroHistory.push(data.gyrox);
  if (hrHistory.length > 239) {
    hrHistory.shift();
    gyroHistory.shift();
    accHistory.shift();
    var inData={};
	
	//these parameters increase subject-specific overfitting so they're left out of the latest NN model
	/*
    inData["acc239"]=(average(hrLong)-55.2963306100677)/4.61615285246901; //average heart rate
    inData["gyro239"]=(standardDeviation(hrLong)); //averge hrv
    inData["hr239"]=(data.seconds-12661.5882253853)/7234.51546531613; //seconds since onset
	*/
	
	//normalize data for neural network
    inData["accx"]=(data.accx+0.663322664869894)/5.29857281752319;
    inData["accy"]=(data.accy+1.06022319548555)/4.17678624086259;
    inData["accz"]=(data.accz+5.87862741795119)/3.80963800679073;
    inData["gx"]=(data.gyrox-0.000485555461973)/0.143583038820586;
    inData["gy"]=(data.gyroy+0.000826609393863)/0.096771026773328;
    inData["gz"]=(data.gyroz+0.000576377775906)/0.102219990828171;
    inData["hrc"]=(data.hr-55.2963306100677)/4.61615285246901;
	
	
    for(var i=0; i< 239;i++){
      inData["acc"+i]=((accHistory[i]+0.663322664869894)/5.29857281752319);
      inData["gyro"+i]=((gyroHistory[i]-0.000485555461973)/0.143583038820586);
      inData["hr"+i]=((hrHistory[i]-55.2963306100677)/4.61615285246901);
    }
	
	//var result=(neural.score(inData,[]));
	 // fetch("https://biostream-1024.appspot.com/sendps?user=fitbitstreamstages&data="+JSON.stringify(result));  //send the neural network sleep stages to server. Note this data is noisy at a second by second level and better results are achieved by averaging stage probabilitie over a ~250 second window
	//todo: average stage probabilities on device (more effective since we don't risk losing samples like we do over a network connection)
  }
}




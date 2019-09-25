/*
 * Entry point for the watch app
 */
import { HeartRateSensor } from "heart-rate";
import { Accelerometer } from "accelerometer";
import { Gyroscope } from "gyroscope";
import * as messaging from "messaging";
import { me } from "appbit";
import { vibration } from "haptics";
import document from "document";

const hrm = new HeartRateSensor();
const accel = new Accelerometer();
const gyro = new Gyroscope();


const status = document.getElementById("status");

me.appTimeoutEnabled=false;
hrm.start();
gyro.start();
accel.start();

var secondsRecorded=0;

messaging.peerSocket.onmessage = function(evt) { //Companion app can send a message back to wake the user in a specific sleep state--currently not used
  setInterval(function(){
  vibration.start("bump");
  
}, 6000);
}


console.log("App code started");
setInterval(function(){
  secondsRecorded++;
  var hr=0;
  if (hrm.heartRate == null) { }
  else{ 
	  hr=hrm.heartRate;
  }
var dataToSend={"hr":hr,"accx":accel.x,"accy":accel.y,"accz":accel.z,"gyrox":gyro.x,"gyroy":gyro.y,"gyroz":gyro.z,"seconds":secondsRecorded};
 if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
    // Send the data to peer as a message
    messaging.peerSocket.send(dataToSend);
	status.text="Transmitting..."
  }
  else {
    console.log("No connection");
  }
  
}, 1000);
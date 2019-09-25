# dreamcatcher
DreamCatcher project for automated TMR using Fitbit and EEG

This is a project by the Paller lab to develop systems for automatic targeted memory reactivation in home settings. Currently it comprises:

* A Fitbit app for acquiring and transmitting heart rate and motion data
  This can be installed through Fitbit Studio, or by creating a new project in the Fitbit CLI developer interface and then copying over the files
* An Android app for acquiring EEG from a portable device (HypnoDyne zMax) and using EEG/Fitbit data to control TMR
  This is an Android studio project
* A server for easily connecting components and streaming data to a remote location
  Written for the old (Python 2.7) version of Google App Engine



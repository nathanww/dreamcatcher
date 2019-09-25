
#
import webapp2
import os
from google.appengine.api import memcache
from random import *
from google.appengine.ext import db

class sleepdata(db.Model):
  sleepdat = db.TextProperty(required=True)

class expdata(db.Model):
  sleepdat = db.TextProperty(required=True)

class sendData(webapp2.RequestHandler):
    def get(self):
        try:
            data=self.request.get("data",default_value="")
            key=self.request.get("user",default_value="0")
            if (memcache.set(key,data,time=30)):
                self.response.out.write("Ok!")
            else:
                self.response.out.write("Err")
        except EnvironmentError:
            self.response.out.write("Err")

class getData(webapp2.RequestHandler):
    def get(self):
        try:
            foo=memcache.get(self.request.get("user",default_value="0"))
            if foo:
                self.response.out.write(foo)
            else:
                foo2="No data"
                self.response.out.write(foo2)            
        except Exception:
            self.response.out.write("Err")
app = webapp2.WSGIApplication([
    ('/sendps', sendData),('/getps', getData)
], debug=True)

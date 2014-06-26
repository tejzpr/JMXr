import Settings
import tornado.ioloop
import tornado.httpserver
import tornado.web
import csv
import os
import redis
import StringIO
import ConfigParser
import json

host = {};

redisObj = redis.StrictRedis(host='localhost', port=6379, db=0)
def get_last_row(hostname):
    host[hostname]=json.loads(redisObj.get(hostname))

class Application(tornado.web.Application):
    def __init__(self):
        handlers = [
            (r"/", MainHandler),
	    (r"/host", HostHandler),
	    (r"/cpu", CPUHandler),
            (r"/heap", HeapHandler),
            (r"/threadcount", ThreadHandler),
            (r"/permgen", PermGenHandler)
        ]
        settings = {
            "template_path": Settings.TEMPLATE_PATH,
            "static_path": Settings.STATIC_PATH,
        }
        tornado.web.Application.__init__(self, handlers, **settings)


config = StringIO.StringIO()
config.write('[dummysection]\n')
config.write(open('/apps/elsdev/ProdMonitor/hosts.properties').read())
config.seek(0, os.SEEK_SET)
cp = ConfigParser.ConfigParser()
cp.readfp(config)
propItems = cp.items('dummysection')

class HostHandler(tornado.web.RequestHandler):
    def post(self):
        for item in propItems:
           get_last_row(item[0]);
        self.write(host)

class CPUHandler(tornado.web.RequestHandler):
    def get(self):
        self.render("cpu.html")

class HeapHandler(tornado.web.RequestHandler):
    def get(self):
        self.render("heap.html")

class ThreadHandler(tornado.web.RequestHandler):
    def get(self):
        self.render("threadcount.html")

class PermGenHandler(tornado.web.RequestHandler):
    def get(self):
        self.render("permgen.html")



class MainHandler(tornado.web.RequestHandler):
    def get(self):
        self.render("index.html")


def main():
    pid=str(os.getpid())
    f = open('pid_server.txt', 'w');
    f.write(pid);
    f.close();
    io_loop = tornado.ioloop.IOLoop.instance()
    try:
       applicaton = Application()
       http_server = tornado.httpserver.HTTPServer(applicaton)
       http_server.listen(9945)
       io_loop.start()
    except:
       io_loop.stop()

if __name__ == "__main__":
    main()

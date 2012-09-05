package controllers;

import play.mvc.*;
import play.api.Play;
import play.data.*;

import models.Task;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import play.Logger;


public class Application extends Controller {
  static Form<Task> taskForm = form(Task.class);
  
  static DB db;
  static long globalid = 0;
  static {
    db = getDb();
  }

  public static Result index() {
      return tasks();
  }
  
  public static Result tasks() {
    return ok(views.html.index.render(getTaskList(), taskForm));
  }
  public static List<Task> getTaskList() {
      DBCollection coll = db.getCollection("tasklist");
        DBCursor cursor = coll.find();
        StringBuffer sBuffer = new StringBuffer();
        List<Task> listTask = new LinkedList<Task>();
        Gson gson = new Gson();
        try {
          while(cursor.hasNext()) {
            String json = cursor.next().toString();
            sBuffer.append(json);
            Task jTask = gson.fromJson(json, Task.class);
            listTask.add(jTask);
          }
        } finally {
          cursor.close();
        }
        return listTask;
  }

  public static Result newTask() {
      Form<Task> filledForm = taskForm.bindFromRequest();
      if(filledForm.hasErrors()) {
          return badRequest(
                  views.html.index.render(getTaskList(), filledForm)
                  );
      } else {
          java.util.Map<String,String> data = filledForm.data();
          String label = data.get("label");
          BasicDBObject doc = new BasicDBObject();
          long id = globalid++;

          doc.put("id", id);
          doc.put("label", label);
          DBCollection coll = db.getCollection("tasklist");
          coll.insert(doc);
          return tasks();
      }
  }
  
  public static Result deleteTask(Long id) {
      BasicDBObject query = new BasicDBObject();
      query.put("id", id);
      DBCollection coll = db.getCollection("tasklist");
      Object cursor = coll.find(query);

      try {
          while(((DBCursor) cursor).hasNext()) {
              DBObject dbObject = ((DBCursor) cursor).next();
              coll.remove(dbObject);
          }
      } finally {
          ((DBCursor) cursor).close();
      }
      return tasks();
  }
 /*
  * mongo.local.hostname=localhost
  * mongo.local.port=27017
  * mongo.remote.hostname=
  * mongo.remote.port=25189
  * mongo.remote.username=
  * mongo.remote.password=
  */
  private static DB getDb() {
      String userName = play.Configuration.root().getString("mongo.remote.username");
      String password = play.Configuration.root().getString("mongo.remote.password");
      boolean local  = true;
    
      String localHostName = play.Configuration.root().getString("mongo.local.hostname");
      Integer  localPort = play.Configuration.root().getInt("mongo.local.port");
      
      String remoteHostName = play.Configuration.root().getString("mongo.remote.hostname");
      Integer remotePort = play.Configuration.root().getInt("mongo.remote.port");

      Mongo m;
      DB db = null;
      if(local){
        String hostname = localHostName;
        int port = localPort;
        try {
        m = new Mongo( hostname, port);   
        db = m.getDB( "db" );
        }catch(Exception e) {
           Logger.error("Exception while intiating Local MongoDB", e);    
        }
      }else {
          String hostname = remoteHostName;
            int port = remotePort;
            try {
            m = new Mongo( hostname , port);   
            db = m.getDB( "db" );
            boolean auth = db.authenticate(userName, password.toCharArray());
            }catch(Exception e) {
                Logger.error("Exception while intiating Local MongoDB", e);    
            }
      }
      return db;
  }
}

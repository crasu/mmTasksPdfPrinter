var fs = require('fs');
var qs = require('querystring');
var https = require('https');

var config = require('./readConfig')(fs);
var encodedProjectConfig = qs.stringify(config.stepNames);

var jiraConnector = require('jiraconnector');

var app = module.exports = {
  tasksQueue: [],
  addTaskToTasksQueue: function (task) {
    if(task) {
      if(!app.tasksQueue) {
        app.tasksQueue = [task];
      } else if(JSON.stringify(app.tasksQueue).indexOf(JSON.stringify(task)) === -1) {
        app.tasksQueue.push(task);
      } else {
        console.log("WARNING: Skipped Task:", task);
      }
    }
  }
};



/**
 * Read and Save Tasks to the Tasks-Queue
 */

var readTasksQueue = function () {
  try {
    var tasks = JSON.parse(fs.readFileSync(__dirname + '/.jiractl.tasks-queue', 'utf8'));
    var i;
    for(i = 0; i < tasks.length; i++) {
      app.addTaskToTasksQueue(tasks[i]);
    }
  } catch (err) {
    console.log("Error reading TasksQueue:", err);
  }
  /*log "Reading Tasks-Queue:"*/
  /*log app.tasksQueue*/
};

var saveTasksQueue = function () {
  fs.writeFileSync(__dirname + '/.jiractl.tasks-queue', JSON.stringify(app.tasksQueue), 'utf8');
};



/**
 * HTTPS-Request to the Jiractl-Server
 */

var checkTasks = function () {
  var request = https.request({
    host: config.jiractlHost,
    port: config.jiractlHostPort,
    path: config.jiractlUriPrefix + '/jiraupdate/' + config.jiractlProjectId,
    auth: 'jira:' + config.jiractlProjectPass,
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': encodedProjectConfig.length
    }
  }, function (res) {
    res.setEncoding('utf8');
    res.on('data', function (data) {
      if(data === 'Unauthorized') {
        console.log("ERROR: Acces denied. Please check your Jira-Credentials in config");
      } else {
        /*log "##### HTTPS-Request Callback #####"*/
        var tasksToUpdate = JSON.parse(data);
        console.log("Tasks to Update:", tasksToUpdate);
        var i;
        for(i = 0; i < tasksToUpdate.length; i++) {
          app.addTaskToTasksQueue(tasksToUpdate[i]);
        }
        /*log "REQUEST FINISHED"*/
      }
    });
  });

  request.on('error', function (err) {
    console.log(err);
  });

  // Add the current Project Configuration to the HTTPS-Request
  request.write(encodedProjectConfig, 'utf8');
  request.end();
};



/**
 * Process the Tasks in the Tasks-Queue
 */

var jiraConnectorCallback = function (err, task) {
  if(err) {
    console.log("Error:", err);
  } else if(task) {
    app.addTaskToTasksQueue(task);
  }
};

var updateTasksInJira = function () {
  if(app.tasksQueue) {
    var j;
    for(j = 0; j < app.tasksQueue.length; j++) {
      jiraConnector.progressTask(config, app.tasksQueue[j].jiraTask, config.stepNames[app.tasksQueue[j].statusCode], app.tasksQueue[j].user, jiraConnectorCallback);
    }
    app.tasksQueue.splice(0);
  }
};



/**
 * Main Loop sending out the requests and processing the Tasks in JIRA
 */

app.mainLoop = function () {
  checkTasks();
  updateTasksInJira();
};

if(!module.parent) {
  /**
   * Main Program:
   */

  readTasksQueue();

  app.mainLoop();
  setInterval(app.mainLoop, config.jiraUpdateInterval);

  process.on('SIGINT', process.exit);
  process.on('exit', saveTasksQueue);
}

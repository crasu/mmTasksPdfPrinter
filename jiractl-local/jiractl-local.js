var fs = require('fs');
var defaultConfig = {
  "jiraUpdateInterval": 300000,
  "jiractlHost": 'jiractl.herokuapp.com',
  "jiractlHostPort": '443',
  "jiractlUriPrefix": '',
  "jiractlProjectPass": '',
  "jiraCliPath": '',
  "jiraUrl": 'http://localhost:8080',
  "jiraUser": '',
  "jiraPass": '',
  "projectName": ''
};
try {
  var configFile = fs.readFileSync(__dirname + '/config.json', 'utf8');
  var config = require(__dirname + '/lib/readConfig').readConfig(configFile, defaultConfig);
  config.jiractlUriPrefix = require(__dirname + '/lib/readConfig').ensureSlashPrefix(config.jiractlUriPrefix);
} catch(err) {
  process.exit(1);
}

var encodedProjectConfig = require('querystring').stringify(config.stepNames);

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
  console.log("Reading Tasks-Queue:");
  console.log(app.tasksQueue);
};

var saveTasksQueue = function () {
  fs.writeFileSync(__dirname + '/.jiractl.tasks-queue', JSON.stringify(app.tasksQueue), 'utf8');
};



/**
 * HTTPS-Request to the Jiractl-Server
 */

var https = require('https');

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
        var tasksToUpdate = JSON.parse(data);
        console.log("Tasks to Update:", tasksToUpdate);
        var i;
        for(i = 0; i < tasksToUpdate.length; i++) {
          app.addTaskToTasksQueue(tasksToUpdate[i]);
        }
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

var jiraConnector = require('jiraconnector');

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

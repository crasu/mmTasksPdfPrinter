var config;
var encodedProjectConfig = "";

var jiraConnector = require('jiraconnector');

var https = require('https');
var fs = require('fs');
var qs = require('querystring');

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

var readConfig = function () {
  config = function () {
    var configFile = global.fs.readFileSync(__dirname + '/config.json', 'utf8');
    var config;
    try {
      config = JSON.parse(configFile);
    } catch (err) {
      console.log("Invalid JSON in Config-File:");
      console.log(err);
      process.exit(1);
    }
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
    if(defaultConfig.jiractlUriPrefix && defaultConfig.jiractlUriPrefix[0] !== '/') {
      defaultConfig.jiractlUriPrefix = '/' + defaultConfig.jiractlUriPrefix;
    }
    var prop;
    for(prop in defaultConfig) {
      if(typeof config[prop] === 'undefined') {
        config[prop] = defaultConfig[prop];
      }
    }
    return config;
  }();
  encodedProjectConfig = qs.stringify(config.stepNames);
};

/*log "Reading Config:"*/
readConfig();
/*log "config: jiraUpdateInterval:", config.jiraUpdateInterval*/
/*log "config: jiractlHost:", config.jiractlHost*/
/*log "config: jiractlHostPort:", config.jiractlHostPort*/
/*log "config: jiractlUriPrefix:", config.jiractlUriPrefix*/
/*log "config: jiractlProjectId:", config.jiractlProjectId*/
/*log "config: jiractlProjectPass:", config.jiractlProjectPass*/
/*log "config: jiraCliPath:", config.jiraCliPath*/
/*log "config: jiraUrl:", config.jiraUrl*/
/*log "config: jiraUser:", config.jiraUser*/
/*log "config: jiraPass:", config.jiraPass*/
/*log "config: projectName:", config.projectName*/
/*log "config: stepNames:", config.stepNames*/

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
};

/*log "Reading Tasks-Queue:"*/
readTasksQueue();
/*log app.tasksQueue*/

/**
 * HTTPS-Request to the Jiractl-Server
 */

var saveTasksQueue = function () {
  fs.writeFileSync(__dirname + '/.jiractl.tasks-queue', JSON.stringify(app.tasksQueue), 'utf8');
};

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

process.on('SIGINT', process.exit);
process.on('exit', saveTasksQueue);

/**
 * Main Program:
 */

app.mainLoop = function () {
  checkTasks();
  updateTasksInJira();
};

if(!module.parent) {
  app.mainLoop();
  setInterval(app.mainLoop, config.jiraUpdateInterval);
}

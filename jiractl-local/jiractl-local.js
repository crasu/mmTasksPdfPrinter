var app = module.exports = {
  https: require('https'),
  fs: require('fs'),
  qs: require('querystring'),
  configParser: require('configparser'),
  taskSerializer: require('taskserializer'),
  jiraConnector: require('jiraconnector'),
  encodedProjectConfig: "",
  tasksQueue: [],
  readConfig: function () {
    var config = app.fs.readFileSync(__dirname + '/config', 'utf8');
    app.jiraUpdateInterval = app.configParser.getPropertyValue('jiraUpdateInterval', config, 300000);
    app.jiractlHost = app.configParser.getPropertyValue('jiractlHost', config, 'jiractl.herokuapp.com');
    app.jiractlHostPort = app.configParser.getPropertyValue('jiractlHostPort', config, '443');
    app.jiractlUriPrefix = app.configParser.getPropertyValue('jiractlUriPrefix', config, '');
    if(app.jiractlUriPrefix && app.jiractlUriPrefix[0] !== '/') {
      app.jiractlUriPrefix = '/' + app.jiractlUriPrefix;
    }
    app.jiractlProjectId = app.configParser.getPropertyValue('jiractlProjectId', config);
    app.jiractlProjectPass = app.configParser.getPropertyValue('jiractlProjectPass', config, '');
    app.jiraCliPath = app.configParser.getPropertyValue('jiraCliPath', config, '');
    app.jiraUrl = app.configParser.getPropertyValue('jiraUrl', config, 'http://localhost:8080');
    app.jiraUser = app.configParser.getPropertyValue('jiraUser', config, '');
    app.jiraPass = app.configParser.getPropertyValue('jiraPass', config, '');
    app.projectName = app.configParser.getPropertyValue('projectName', config, '');
    app.numberOfSteps = app.configParser.getPropertyValue('numberOfSteps', config, 5);
    app.stepNames = [];
    var i;
    for(i = 0; i < app.numberOfSteps; i++) {
      app.stepNames.push(app.configParser.getPropertyValue('stepId-' + i, config));
    }
    app.encodedProjectConfig = app.qs.stringify(app.stepNames);
  },
  readTasksQueue: function () {
    var tasks = app.taskSerializer.deserializeTasks(app.fs.readFileSync(__dirname + '/.jiractl.tasks-queue', 'utf8'));
    if(tasks) {
      var i;
      for(i = 0; i < tasks.length; i++) {
        app.addToTasksQueue(tasks[i]);
      }
    }
  },
  saveTasksQueue: function () {
    /*log "Saving Tasks:", app.taskSerializer.serializeTasks(app.tasksQueue)*/
    app.fs.writeFileSync(__dirname + '/.jiractl.tasks-queue', app.taskSerializer.serializeTasks(app.tasksQueue), 'utf8');
  },
  addToTasksQueue: function (task) {
    if(task) {
      if(!app.tasksQueue) {
        app.tasksQueue = [task];
      } else if(app.taskSerializer.serializeTasks(app.tasksQueue).indexOf(app.taskSerializer.serializeTasks([task])) === -1) {
        app.tasksQueue.push(task);
      } else {
        console.log("WARNING: Skipped Task:", task);
      }
    }
  }
};

/**
 * Program:
 */

/*log "Reading Config:"*/
app.readConfig();
/*log "config: jiraUpdateInterval:", app.jiraUpdateInterval*/
/*log "config: jiractlHost:", app.jiractlHost*/
/*log "config: jiractlHostPort:", app.jiractlHostPort*/
/*log "config: jiractlUriPrefix:", app.jiractlUriPrefix*/
/*log "config: jiractlProjectId:", app.jiractlProjectId*/
/*log "config: jiractlProjectPass:", app.jiractlProjectPass*/
/*log "config: jiraCliPath:", app.jiraCliPath*/
/*log "config: jiraUrl:", app.jiraUrl*/
/*log "config: jiraUser:", app.jiraUser*/
/*log "config: jiraPass:", app.jiraPass*/
/*log "config: projectName:", app.projectName*/
/*log "config: numberOfSteps:", app.numberOfSteps*/
/*log "config: stepNames:", app.stepNames*/

/*log "Reading Tasks-Queue:"*/
app.readTasksQueue();
/*log app.tasksQueue*/

/**
 * HTTPS-Request to the Jiractl-Server
 */

app.sendRequest = function () {
  app.request = app.https.request({
    host: app.jiractlHost,
    port: app.jiractlHostPort,
    path: app.jiractlUriPrefix + '/jiraupdate/' + app.jiractlProjectId,
    auth: 'jira:' + app.jiractlProjectPass,
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': app.encodedProjectConfig.length
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
          app.addToTasksQueue(tasksToUpdate[i]);
        }
        var jiraConnectorCallback = function (err, task) {
          if(err) {
            console.log("Error:", err);
          } else if(task) {
            app.addToTasksQueue(task);
          }
        };
        var j;
        for(j = 0; j < app.tasksQueue.length; j++) {
          app.jiraConnector.progressTask(app.jiraCliPath + '/jira.sh', app.jiraUrl, app.jiraUser, app.jiraPass, app.projectName, app.tasksQueue[j].jiraTask, app.stepNames[app.tasksQueue[j].statusCode], app.tasksQueue[j].user, jiraConnectorCallback);
          app.tasksQueue.splice(j, 1);
          j--;
        }
        /*log "REQUEST FINISHED"*/
      }
    });
  });

  app.request.on('error', function (err) {
    console.log(err);
  });

  // Add the current Project Configuration to the HTTPS-Request
  app.request.write(app.encodedProjectConfig, 'utf8');
  app.request.end();
};

process.on('SIGINT', process.exit);
process.on('exit', app.saveTasksQueue);

setInterval(app.sendRequest, app.jiraUpdateInterval);
app.sendRequest();

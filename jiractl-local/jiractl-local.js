var app = module.exports = {
  https: require('https'),
  cp: require('child_process'),
  fs: require('fs'),
  qs: require('querystring'),
  configParser: require('configparser'),
  taskSerializer: require('taskserializer'),
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
    app.jiraCliPath = app.configParser.getPropertyValue('jiraCliPath', config);
    app.jiraUrl = app.configParser.getPropertyValue('jiraUrl', config, 'http://localhost:8080');
    app.jiraUser = app.configParser.getPropertyValue('jiraUser', config);
    app.jiraPass = app.configParser.getPropertyValue('jiraPass', config);
    app.projectName = app.configParser.getPropertyValue('projectName', config);
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
            tmpFailedTasksQueue = [];
        console.log("Tasks to Update:", tasksToUpdate);
        var i;
        for(i = 0; i < tasksToUpdate.length; i++) {
          app.addToTasksQueue(tasksToUpdate[i]);
        }
        app.cp.jiraError = function (nr, id) {
          return function (err) {
            if(app.projectName && app.tasksQueue && app.tasksQueue[id] && app.tasksQueue[id].jiraTask) {
              console.log("Error progressing Issue " + app.projectName + "-" + app.tasksQueue[id].jiraTask + ":", err.toString());
            }
            if(nr === 1) {
              tmpFailedTasksQueue.push(app.tasksQueue[id]);
              if(id === app.tasksQueue.length - 1) {
                app.tasksQueue = tmpFailedTasksQueue;
              }
            } else if(nr === 2) {
              app.cp.jiraOutput(2, id)("Error Nr.2 -> jiraOutput(2," + id + ")");
            }
          };
        };
        app.cp.jiraOutput = function (nr, id) {
          if(nr === 1) {
            return function (data) {
              /*log "jiraOutput(1, " + id + "):", data.toString()*/
              app.cp.jira[id] = app.cp.spawn(app.jiraCliPath + '/jira.sh', ['-a', 'progressIssue', '-s', app.jiraUrl, '-u', app.jiraUser, '-p', app.jiraPass, '--project', app.projectName, '--issue', app.projectName + '-' + app.tasksQueue[id].jiraTask, '--step', app.stepNames[app.tasksQueue[id].statusCode]]);
              app.cp.jira[id].stderr.on('data', app.cp.jiraError(2, id));
              app.cp.jira[id].stdout.once('data', app.cp.jiraOutput(2, id));
            };
          } else if(nr === 2) {
            return function (data) {
              if(data) {
                /*log "jiraOutput(2, " + id + "):", data.toString()*/
              }
              app.cp.jira[id] = app.cp.spawn(app.jiraCliPath + '/jira.sh', ['-a', 'updateIssue', '-s', app.jiraUrl, '-u', app.jiraUser, '-p', app.jiraPass, '--project', app.projectName, '--issue', app.projectName + '-' + app.tasksQueue[id].jiraTask, '--assignee', app.tasksQueue[id].user]);
              app.cp.jira[id].stderr.on('data', app.cp.jiraError(3, id));
              app.cp.jira[id].stdout.once('data', app.cp.jiraOutput(3, id));
            };
          } else if(nr === 3) {
            return function (data) {
              var output = data.toString();
              /*log "jiraOutput(3, " + id + "):", output*/
              if(id === app.tasksQueue.length - 1) {
                app.tasksQueue = tmpFailedTasksQueue;
              }
            };
          }
        };
        app.cp.jira = [];
        var j;
        for(j = 0; j < app.tasksQueue.length; j++) {
            app.cp.jira[j] = app.cp.spawn(app.jiraCliPath + '/jira.sh', ['-a', 'updateIssue', '-s', app.jiraUrl, '-u', app.jiraUser, '-p', app.jiraPass, '--project', app.projectName, '--issue', app.projectName + '-' + app.tasksQueue[j].jiraTask, '--assignee', app.jiraUser]);
            app.cp.jira[j].stderr.on('data', app.cp.jiraError(1, j));
            app.cp.jira[j].stdout.once('data', app.cp.jiraOutput(1, j));
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

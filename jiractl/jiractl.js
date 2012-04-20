/**
 * JSLint Init
 */
'use strict';



/**
 * Init Global Object:
 */

var express = require('express'),
    cp = require('child_process'),
    fs = require('fs'),
    bcrypt = require('bcrypt');

var app;

var config;
   


/**
 * Init Config-File and Config-Parser
 */

var readConfig = function () {
  config = function () {
    var configFile = fs.readFileSync(__dirname + '/config.json', 'utf8');
    var config;
    try {
      config = JSON.parse(configFile);
    } catch (err) {
      console.log("Invalid JSON in Config-File:");
      console.log(err);
      process.exit(1);
    }
    var defaultConfig = {
      "useInternalHTTPS": "no",
      "useLocalMode": "no",
      "uriPrefix": "",
      "manageUser": "admin",
      "managePass": "admin"
    };
    var prop;
    for(prop in defaultConfig) {
      if(typeof config[prop] === 'undefined') {
        config[prop] = defaultConfig[prop];
      }
    }
    return config;
  }();

  if(config.uriPrefix && config.uriPrefix[0] !== '/') {
    config.uriPrefix = '/' + config.uriPrefix;
  }

  if(process.env.PORT) {
    config.port = process.env.PORT;
  }

  /*log "Read Config:", config*/
}();



/**
 * Init Express Server:
 */

if(config.useInternalHTTPS === 'yes') {
  var options = {
    key: require('fs').readFileSync(__dirname + '/ssl/jiractl-key.pem'),
    cert: require('fs').readFileSync(__dirname + '/ssl/jiractl-cert.pem')
  },
  app = module.exports = express.createServer(options);
  /*log "DEBUG: Using internal HTTPS-Support"*/
} else {
  app = module.exports = express.createServer();
}

config.useLocalMode = (config.useLocalMode === 'yes');



/**
 * General functionality
 */

var sendFileCallback = function (filename) {
  return function (err) {
    if(err) {
      console.log(err);
      next(err);
    } else {
      /*log "Transfer:", filename, " - succesful."*/
    }
  };
};

var getStepNames;
var getTaskInfo;
var updateTask;
var comparePass;
var getProjects;

if(config.useLocalMode) {
  /**
   * Local Mode
   */

  var readLocalConfig = function () {
    config.localconfig = function () {
      var localconfigFile = fs.readFileSync(__dirname + '/localconfig.json', 'utf8');
      var localconfig;
      try {
        localconfig = JSON.parse(localconfigFile);
      } catch (err) {
        console.log("Invalid JSON in Local-Config-File:");
        console.log(err);
        process.exit(1);
      }
      var defaultLocalConfig = {
        "jiraCliPath": '',
        "jiraUrl": 'http://localhost:8080',
        "jiraUser": '',
        "jiraPass": '',
        "projectName": '',
        "projectPass": '',
        "users": '["admin"]'
      };
      var prop;
      for(prop in defaultLocalConfig) {
        if(typeof localconfig[prop] === 'undefined') {
          localconfig[prop] = defaultLocalConfig[prop];
        }
      }
      return localconfig;
    }();
    /*log "Read Local-Config:", config.localconfig*/
  }();

  var jiraconnector = require('jiraconnector');

  getStepNames = function (projectId, taskId, callback) {
    jiraconnector.getAvailableWorksteps(config.localconfig, taskId, callback);
  };

  getTaskInfo = function (projectId, taskId, callback) {
    jiraconnector.getAvailableWorksteps(config.localconfig, taskId, function (err, stepNames) {
      if(err) {
        callback(err);
      } else {
        var taskInfo = {};
        taskInfo.stepNames = stepNames;
        jiraconnector.getIssueInfo(config.localconfig, taskId, function (err, issueInfo) {
          if(err) {
            callback(err);
          } else {
            taskInfo.issueInfo = issueInfo;
            callback(null, taskInfo);
          }
        });
      }
    });
  };

  updateTask = function (projectId, taskId, statusCode, user, callback) {
    getStepNames(projectId, taskId, function (err, stepNames) {
      if(err) {
        callback(err);
      } else {
        jiraconnector.progressTask(config.localconfig, taskId, stepNames[statusCode], user, callback);
      }
    });
  };

  comparePass = function (pass1, pass2) {
    return (pass1 === pass2);
  };

  getProjects = function (projectId, callback) {
    callback(null, [{
      project: config.localconfig.projectName,
      password: config.localconfig.projectPass,
      users: config.localconfig.users
    }]);
  };

} else {
  /**
   * Standard Mode
   */

  /**
   * Mongoose: Init MongoDB
   */

  var mongoose = require('db_core')(config.mongoUser, config.mongoPass, config.mongoHost, config.mongoPort, config.mongoDB);

  var Task = require('db_tasks')(mongoose);
  var Project = require('db_projects')(mongoose, Task);

  getStepNames = function (projectId, taskId, callback) {
    Project.getProjectsFromDB(projectId, function (err, projects) {
      if(err) {
        console.log("Error finding Project in DB:", err);
        callback(new Error("Error finding Project in DB"), undefined);
      } else if(projects && projects[0] && projects[0].stepNames) {
        callback(null, projects[0].stepNames);
      } else {
        callback(new Error("Invalid Project found in DB"), undefined);
      }
    });
  };

  getTaskInfo = function (projectId, taskId, callback) {
    getStepNames(projectId, taskId, function (err, stepNames) {
      if(err) {
        callback(err);
      } else {
        var taskInfo = {};
        taskInfo.stepNames = stepNames;
        callback(null, taskInfo);
      }
    });
  };

  updateTask = function (projectId, taskId, statusCode, user, callback) {
    var task = {
      project: projectId,
      jiraTask: taskId,
      statusCode: statusCode,
      user: user
    };
    Task.saveTaskToDB(task, callback);
  };

  comparePass = function (pass1, pass2) {
    return bcrypt.compareSync(pass1, pass2);
  };

  getProjects = Project.getProjectsFromDB;
}



/**
 * Authentication Middleware:
 */

basicAuth = {
  user: function (req, res, next) {
    basicAuth.projectPass(req, res, next, function (user, project) {
      return (project.users.indexOf(user) !== -1);
    });
  },

  jira: function (req, res, next) {
    basicAuth.projectPass(req, res, next, function (user) {
      return (user === 'jira');
    });
  },

  admin: function (user, pass) {
    return (user === config.manageUser && pass === config.managePass);
  },

  projectPass: function (req, res, next, validateUser) {
    var projectId = req.params.project || req.session.project;
    if(projectId) {
      getProjects(projectId, function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB with ID:", projectId, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else {
          express.basicAuth(function (user, pass) {
            if(projects && projects[0] && projects[0].password) {
              if(validateUser && typeof validateUser === 'function') {
                return (validateUser(user, projects[0]) && (comparePass(pass, projects[0].password)));
              } else {
                return (comparePass(pass, projects[0].password));
              }
            } else {
              return false;
            }
          })(req, res, next);
        }
      });
    } else {
      express.basicAuth(function (user, pass) {
        return false;
      })(req, res, next);
    }
  },

  getCredentials: function (req) {
    return new Buffer(req.headers.authorization.split(' ')[1], 'base64').toString().split(':');
  }
};
  


/**
 * Express: Configuration
 */

cp.exec('openssl rand -base64 48', {
  encoding: 'utf8',
  timeout: '0',
  maxBuffer: 200*1024,
  killSignal: 'SIGTERM',
  cwd: null,
  env: null
}, function (err, stdout, stderr) {
  var secretSessionHashKey;
  if(err) {
    console.log("WARN: Using default random key to generate session hash codes...");
    secretSessionHashKey = "default-random-key-8738675454u89732375789ncgv8fvhnfcdhsduyigbfzucgnsdugkgngnuewhbdkufy4egvnjehcgfykzegb2skuvfngyezucbygsnucbfygukzegcuzgdfbgcysesfgdn";
  } else {
    secretSessionHashKey = stdout;
  }
  
  /*notequal secretSessionHashKey, "default-random-key-8738675454u89732375789ncgv8fvhnfcdhsduyigbfzucgnsdugkgngnuewhbdkufy4egvnjehcgfykzegb2skuvfngyezucbygsnucbfygukzegcuzgdfbgcysesfgdn", "Use random generated SessionHashKey"*/

  app.configure(function () {
    app.set('views', __dirname + '/templates');
    app.set('view engine', 'jst');
    app.set('view options', {
      layout: false
    });
    app.use(express.bodyParser());
    app.use(express.methodOverride());
    app.use(express.cookieParser());
    app.use(express.session({
      secret: secretSessionHashKey
    }));
    app.use(app.router);
    app.use(express['static'](__dirname + '/public'));
  });

  app.configure('development', function () {
    app.use(express.errorHandler({
      dumpExceptions: true,
      showStack: true
    })); 
  });

  app.configure('production', function () {
    app.use(express.errorHandler()); 
  });
 


  /**
   * Express: Routes
   */

  // Debugging:
  app.get('/*', function (req, res, next) {
    /*log "GET-Request:", req.url*/
    if(req.session) {
      if(req.session.project) {
        /*log "SESSION: Project:", req.session.project*/
      }
      if(req.session.task) {
        /*log "SESSION: Task:", req.session.task*/
      }
    }
    if(config.useInternalHTTPS !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + config.hostname + req.url);
        } else {
          next();
        }
      } else {
        console.log("WARNING: Internal HTTPS-Support is off and req.headers['x-forwarded-proto'] was not found - Bypassing Heroku HTTPS-Redirect");
        next();
      }
    } else {
      next();
    }
  });

  app.post('/*', function (req, res, next) {
    /*log "POST-Request:", req.url*/
    if(req.session) {
      if(req.session.project) {
        /*log "SESSION: Project:", req.session.project*/
      }
      if(req.session.task) {
        /*log "SESSION: Task:", req.session.task*/
      }
    }
    if(config.useInternalHTTPS !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + config.hostname + req.url);
        } else {
          next();
        }
      } else {
        console.log("WARNING: Internal HTTPS-Support is off and req.headers['x-forwarded-proto'] was not found - Bypassing Heroku HTTPS-Redirect");
        next();
      }
    } else {
      next();
    }
  });

  // AgileCards Jira-Plugin-Support
  app.get(config.uriPrefix + '/update/:jiraKey', function (req, res, next) {
    /*log "update/:jiraKey"*/
    if(req.params.jiraKey.indexOf('-') !== -1) {
      /*log "Old URL:", req.url*/
      req.url = config.uriPrefix + '/update/' + req.params.jiraKey.replace('-', '/');
      /*log "New URL:", req.url*/
      next();
    }
  });

  // Incoming updates from mobile clients
  app.get(config.uriPrefix + '/update/:project/:jiraTask', basicAuth.user, function (req, res) {
    /*log "ID['project']:", req.params.project*/
    /*log "ID['jiraTask']:", req.params.jiraTask*/
    var credentials = basicAuth.getCredentials(req);
    if(req.session) {
      req.session.task = {
        project: req.params.project,
        jiraTask: req.params.jiraTask,
        user: credentials[0]
      };
      getTaskInfo(req.params.project, req.params.jiraTask, function (err, taskInfo) {
        if(err) {
          console.log("Error on update:", err, "Task-Info:", taskInfo);
          res.redirect(config.uriPrefix + '/error.html');
        } else {
          var renderProps = taskInfo;
          renderProps.uriPrefix = config.uriPrefix;
          if(req.session.updateInfo) {
            renderProps.updateInfo = req.session.updateInfo;
            delete req.session.updateInfo;
          }
          res.render('update', renderProps);
        }
      });
    } else {
      /*log "req.session:", req.session*/
      res.redirect(config.uriPrefix + '/session_not_found.html');
    }
  });

  app.post(config.uriPrefix + '/updatestatus/:statusCode', function (req, res) {
    if(req.session && req.session.task && req.session.task.project && req.session.task.jiraTask) {
      updateTask(req.session.task.project, req.session.task.jiraTask, req.params.statusCode, req.session.task.user, function (err) {
        if(err) {
          console.log("Error on updatestatus:", err, "Task:", req.session.task);
          res.redirect(config.uriPrefix + '/error.html');
        } else {
          req.session.updateInfo = '<p class="updateInfo">Successfully updated Issue ' + req.session.task.jiraTask + '.</p>';
          res.redirect(config.uriPrefix + '/update/' + req.session.task.project + '/' + req.session.task.jiraTask);
        }
        delete req.session.task;
      });
    } else {
      res.redirect(config.uriPrefix + '/session_not_found.html');
    }
  });

  if(!config.useLocalMode) {
    // Jiractl-local Polling
    app.post(config.uriPrefix + '/jiraupdate/:project', basicAuth.jira, function (req, res) {
      Task.getTasksFromDB(req.params.project, function (err, tasksToUpdate) {
        if(err) {
          console.log("Error finding Tasks in DB:", err);
        } else {
          res.json(tasksToUpdate);
          Task.remove({
            project: req.params.project
          }, function (err, numberOfRemovedDocs) {
            if(err) {
              console.log("Error removing Tasks from DB:", err);
            } else {
              /*log "Removed", numberOfRemovedDocs, "Task-Document(s) from DB."*/
            }
          });
        }
      });
      if(req.body) {
        var project = {
          project: req.params.project,
          stepNames: []
        },
          stepName;
        for(stepName in req.body) {
          if(req.body.hasOwnProperty(stepName)) {
            project.stepNames.push(req.body[stepName]);
          }
        }
        Project.updateProjectStepNamesToDB(project, function (err, updatedProject) {
          if(err) {
            console.log("Error updating Project's stepNames:", project);
          } else {
            /*log "Successfully updated Project:", updatedProject*/
          }
        });
      }
    });

    // Administration Interface
    app.post(config.uriPrefix + '/close', basicAuth.user, function (req, res) {
      if(req.session && req.session.project) {
        Project.deleteProjectsFromDB(req.session.project, function (err, numberOfRemovedDocs) {
          if(err) {
            console.log("Error closing Project:", err);
            res.send('Error closing Project.', 337);
          } else {
            /*log "Removed", numberOfRemovedDocs, "Project-Document(s) from DB."*/
            res.end();
          }
        });
      } else {
        res.redirect(config.uriPrefix + '/session_not_found.html');
      }
    });

    app.get(config.uriPrefix + '/manage', express.basicAuth(basicAuth.admin), function (req, res) {
      Project.getAllProjectsFromDB(function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB:", err);
          res.send('Error checking for existing Projects.', 337);
        } else {
          var projectList = "";
          if(projects) {
            var i;
            for(i = 0; i < projects.length; i++) {
              projectList += '<tr><td align="center"><a href="' + config.uriPrefix + '/manage/' + projects[i].project + '">' + projects[i].project + '</a></td></tr>';
            }
          }
          res.render('manage', {
            uriPrefix: config.uriPrefix,
            projectList: projectList
          });
        }
      });
    });

    app.post(config.uriPrefix + '/init/:project/:password', express.basicAuth(basicAuth.admin), function (req, res) {
      Project.getProjectsFromDB(req.params.project, function (err, projects) {
        if (err) {
          console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else {
          // Security note: Only use var-statements here because you have function-scope
          var pwSalt = bcrypt.genSaltSync(10);
          var pwHash = bcrypt.hashSync(req.params.password, pwSalt);
          if(projects.length === 0) {
            var credentials = basicAuth.getCredentials(req);
            Project.saveProjectToDB({
              project: req.params.project,
              password: pwHash,
              stepNames: [],
              users: [credentials[0]]
            }, function (err) {
              if(err) {
                console.log("Error saving Project:", err);
                res.send('Error saving Project.', 337);
              } else {
                res.redirect(config.uriPrefix + '/manage');
              }
            });
          } else {
            res.send('Project already exists.', 337);
          }
        }
      });
    });

    app.get(config.uriPrefix + '/manage/:project', basicAuth.user, function (req, res) {
      if(req.session) {
        req.session.project = req.params.project;
      }
      Project.getProjectsFromDB(req.params.project, function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else if(projects && projects[0] && projects[0].users) {
          var i,
              userList = "";
          for(i = 0; i < projects[0].users.length; i++) {
            userList += '<tr><td>' + i + '</td><td>' + projects[0].users[i] + '</td></tr>';
          }
          res.render('manageProject', {
            uriPrefix: config.uriPrefix,
            userList: userList
          });
        } else {
          res.send('Error: Project not found.', 404);
        }
      });
    });

    app.post(config.uriPrefix + '/useradd/:user', basicAuth.user, function (req, res) {
      /*log "useradd:", req.params.user*/
      if(req.session && req.session.project) {
        Project.updateProjectUserAddToDB(req.session.project, req.params.user, function (err) {
          if(err) {
            console.log("Error adding User:", err);
            res.send('Error adding User.', 337);
          } else {
            /*log "Added User '" + req.params.user + "' to Project", req.session.project*/
            res.redirect(config.uriPrefix + '/manage/' + req.session.project);
          }
        });
      } else {
        res.redirect(config.uriPrefix + '/session_not_found.html');
      }
    });
  
    app.post(config.uriPrefix + '/userdel/:user', basicAuth.user, function (req, res) {
      if(req.session && req.session.project) {
        Project.updateProjectUserDelFromDB(req.session.project, req.params.user, function (err) {
          if(err) {
            console.log("Error deleting User:", err);
            res.send('Error deleting User.', 337);
          } else {
            res.redirect(config.uriPrefix + '/manage/' + req.session.project);
          }
        });
      } else {
        res.redirect(config.uriPrefix + '/session_not_found.html');
      }
    });
  }

  app.get('*/favicon.ico', function (req, res) {
    res.end();
  });
  
  // Remove uriPrefix from Request-Uri to serve the correct static content
  app.get(config.uriPrefix + '/*', function (req, res, next) {
    req.url = req.url.slice(config.uriPrefix.length);
    next();
  });

  app.post(config.uriPrefix + '/*', function (req, res, next) {
    req.url = req.url.slice(config.uriPrefix.length);
    next();
  });



  if(!module.parent) {
    /**
     * Express: Start Server
     */

    app.listen(config.port);
    console.log("Port: %d, UriPrefix: %s, LocalMode: %s\n  ***  %s mode  ***", app.address().port, config.uriPrefix, config.useLocalMode, app.settings.env);
  }
});

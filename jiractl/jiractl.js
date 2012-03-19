/**
 * JSLint Init
 */
'use strict';



/**
 * Init Global Object:
 */

var express = require('express'),
    global = {
      cp: require('child_process'),
      fs: require('fs'),
      bcrypt: require('bcrypt'),
      configParser: require('configparser')
    };



/**
 * Init Config-File and Config-Parser
 */

global.readConfig = function () {
  global.config = { };
  var config = global.fs.readFileSync(__dirname + '/config', 'utf8');
  global.uriPrefix = global.configParser.getPropertyValue('uriPrefix', config, '');
  if(global.uriPrefix && global.uriPrefix[0] !== '/') {
    global.uriPrefix = '/' + global.uriPrefix;
  }
  global.config.useInternalHTTPS = global.configParser.getPropertyValue('useInternalHTTPS', config, "no");
  global.config.useLocalMode = global.configParser.getPropertyValue('useLocalMode', config, "no");
  global.config.hostname = global.configParser.getPropertyValue('hostname', config);
  global.config.port = global.configParser.getPropertyValue('port', config);
  global.config.mongoUser = global.configParser.getPropertyValue('mongoUser', config);
  global.config.mongoPass = global.configParser.getPropertyValue('mongoPass', config);
  global.config.mongoHost = global.configParser.getPropertyValue('mongoHost', config);
  global.config.mongoPort = global.configParser.getPropertyValue('mongoPort', config);
  global.config.mongoDB = global.configParser.getPropertyValue('mongoDB', config);
  global.config.manageUser = global.configParser.getPropertyValue('manageUser', config, "admin");
  global.config.managePass = global.configParser.getPropertyValue('managePass', config, "admin");
}();

global.port = process.env.PORT || global.config.port;



/**
 * Init Express Server:
 */

if(global.config.useInternalHTTPS === 'yes') {
  var options = {
    key: require('fs').readFileSync(__dirname + '/ssl/jiractl-key.pem'),
    cert: require('fs').readFileSync(__dirname + '/ssl/jiractl-cert.pem')
  },
  app = module.exports = express.createServer(options);
  console.log("DEBUG: Using internal HTTPS-Support");
} else {
  var app = module.exports = express.createServer();
}

if(global.config.useLocalMode === 'yes') {
  global.localMode = true;
}



/**
 * General functionality
 */

global.sendFileCallback = function (filename) {
  return function (err) {
    if(err) {
      console.log(err);
      next(err);
    } else {
      /*log "Transfer:", filename, " - succesful."*/
    }
  };
};

if(global.localMode) {
  /**
   * Local Mode
   */

  global.readLocalConfig = function () {
    global.localconfig = { };
    var config = global.fs.readFileSync(__dirname + '/localconfig', 'utf8');
    global.localconfig.jiraCliPath = global.configParser.getPropertyValue('jiraCliPath', config, '');
    global.localconfig.jiraUrl = global.configParser.getPropertyValue('jiraUrl', config, 'http://localhost:8080');
    global.localconfig.jiraUser = global.configParser.getPropertyValue('jiraUser', config, '');
    global.localconfig.jiraPass = global.configParser.getPropertyValue('jiraPass', config, '');
    global.localconfig.projectName = global.configParser.getPropertyValue('projectName', config, '');
    global.localconfig.projectPass = global.configParser.getPropertyValue('projectPass', config, '');
    global.localconfig.users = JSON.parse(global.configParser.getPropertyValue('users', config, '["admin"]'));
  }();

  global.jiraconnector = require('jiraconnector');

  global.getStepNames = function (projectId, taskId, callback) {
    global.jiraconnector.getAvailableWorksteps(global.localconfig.jiraCliPath + '/jira.sh', global.localconfig.jiraUrl, global.localconfig.jiraUser, global.localconfig.jiraPass, projectId, taskId, callback);
  }

  global.updateTask = function (projectId, taskId, statusCode, user, callback) {
    global.getStepNames(projectId, taskId, function (err, stepNames) {
      if(err) {
        callback(err);
      } else {
        global.jiraconnector.progressTask(global.localconfig.jiraCliPath + '/jira.sh', global.localconfig.jiraUrl, global.localconfig.jiraUser, global.localconfig.jiraPass, projectId, taskId, stepNames[statusCode], user, callback);
      }
    });
  };

  global.comparePass = function (pass1, pass2) {
    return (pass1 === pass2);
  };

  global.getProjects = function (projectId, callback) {
    callback(null, [{
      project: global.localconfig.projectName,
      password: global.localconfig.projectPass,
      users: global.localconfig.users
    }]);
  };
} else {
  /**
   * Standard Mode
   */

  global.getStepNames = function (projectId, taskId, callback) {
    global.Project.getProjectsFromDB(req.params.project, function (err, projects) {
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

  global.updateTask = function (projectId, taskId, statusCode, user, callback) {
    var task = {
      project: projectId,
      jiraTask: taskId,
      statusCode: statusCode,
      user: user
    };
    global.Task.saveTaskToDB(task, callback);
  };

  global.comparePass = function (pass1, pass2) {
    return global.bcrypt.compareSync(pass1, pass2);
  };



  /**
   * Mongoose: Init MongoDB
   */

  global.mongoose = require('db_core')(global.config.mongoUser, global.config.mongoPass, global.config.mongoHost, global.config.mongoPort, global.config.mongoDB);

  global.Task = require('db_tasks')(global.mongoose);
  global.Project = require('db_projects')(global.mongoose, global.Task);

  global.getProjects = global.Project.getProjectsFromDB;
}



/**
 * Authentication Middleware:
 */

global.basicAuth = {
  user: function (req, res, next) {
    global.basicAuth.projectPass(req, res, next, function (user, project) {
      return (project.users.indexOf(user) !== -1);
    });
  },

  jira: function (req, res, next) {
    global.basicAuth.projectPass(req, res, next, function (user) {
      return (user === 'jira');
    });
  },

  admin: function (user, pass) {
    return (user === global.config.manageUser && pass === global.config.managePass);
  },

  projectPass: function (req, res, next, validateUser) {
    var projectId = req.params.project || req.session.project;
    if(projectId) {
      global.getProjects(projectId, function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB with ID:", projectId, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else {
          express.basicAuth(function (user, pass) {
            if(projects && projects[0] && projects[0].password) {
              if(validateUser && typeof validateUser === 'function') {
                return (validateUser(user, projects[0]) && (global.comparePass(pass, projects[0].password)));
              } else {
                return (global.comparePass(pass, projects[0].password));
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

global.cp.exec('openssl rand -base64 48', {
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
    if(global.config.useInternalHTTPS !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + global.config.hostname + req.url);
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
    if(global.config.useInternalHTTPS !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + global.config.hostname + req.url);
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

  // Incoming updates from mobile clients
  app.get(global.uriPrefix + '/update/:project/:jiraTask', global.basicAuth.user, function (req, res) {
    /*log "ID['project']:", req.params.project*/
    /*log "ID['jiraTask']:", req.params.jiraTask*/
    var credentials = global.basicAuth.getCredentials(req);
    if(req.session) {
      req.session.task = {
        project: req.params.project,
        jiraTask: req.params.jiraTask,
        user: credentials[0]
      };
      global.getStepNames(req.params.project, req.params.jiraTask, function (err, stepNames) {
        if(err) {
          res.redirect(global.uriPrefix + '/error.html');
        } else {
          res.render('update', {
            uriPrefix: global.uriPrefix,
            stepNames: stepNames
          });
        }
      });
    } else {
      /*log "req.session:", req.session*/
      res.redirect(global.uriPrefix + '/session_not_found.html');
    }
  });

  app.post(global.uriPrefix + '/updatestatus/:statusCode', function (req, res) {
    if(req.session && req.session.task) {
      global.updateTask(req.session.task.project, req.session.task.jiraTask, req.params.statusCode, req.session.task.user, function (err, task) {
        if(err || task) {
          res.redirect(global.uriPrefix + '/error.html');
        } else {
          res.redirect(global.uriPrefix + '/done.html');
        }
      });
      req.session.destroy();
    } else {
      res.redirect(global.uriPrefix + '/session_not_found.html');
    }
  });

  if(!global.localMode) {
    // Jiractl-local Polling
    app.post(global.uriPrefix + '/jiraupdate/:project', global.basicAuth.jira, function (req, res) {
      global.Task.getTasksFromDB(req.params.project, function (err, tasksToUpdate) {
        if(err) {
          console.log("Error finding Tasks in DB:", err);
        } else {
          res.json(tasksToUpdate);
          global.Task.remove({
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
        global.Project.updateProjectStepNamesToDB(project, function (err, updatedProject) {
          if(err) {
            console.log("Error updating Project's stepNames:", project);
          } else {
            /*log "Successfully updated Project:", updatedProject*/
          }
        });
      }
    });

    // Administration Interface
    app.post(global.uriPrefix + '/close', global.basicAuth.user, function (req, res) {
      if(req.session && req.session.project) {
        global.Project.deleteProjectsFromDB(req.session.project, function (err, numberOfRemovedDocs) {
          if(err) {
            console.log("Error closing Project:", err);
            res.send('Error closing Project.', 337);
          } else {
            /*log "Removed", numberOfRemovedDocs, "Project-Document(s) from DB."*/
            res.end();
          }
        });
      } else {
        res.redirect(global.uriPrefix + '/session_not_found.html');
      }
    });

    app.get(global.uriPrefix + '/manage', express.basicAuth(global.basicAuth.admin), function (req, res) {
      global.Project.getAllProjectsFromDB(function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB:", err);
          res.send('Error checking for existing Projects.', 337);
        } else {
          var projectList = "";
          if(projects) {
            var i;
            for(i = 0; i < projects.length; i++) {
              projectList += '<tr><td align="center"><a href="' + global.uriPrefix + '/manage/' + projects[i].project + '">' + projects[i].project + '</a></td></tr>';
            }
          }
          res.render('manage', {
            uriPrefix: global.uriPrefix,
            projectList: projectList
          });
        }
      });
    });

    app.post(global.uriPrefix + '/init/:project/:password', express.basicAuth(global.basicAuth.admin), function (req, res) {
      global.Project.getProjectsFromDB(req.params.project, function (err, projects) {
        if (err) {
          console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else {
          // Security note: Only use var-statements here because you have function-scope
          var pwSalt = global.bcrypt.genSaltSync(10);
          var pwHash = global.bcrypt.hashSync(req.params.password, pwSalt);
          if(projects.length === 0) {
            var credentials = global.basicAuth.getCredentials(req);
            global.Project.saveProjectToDB({
              project: req.params.project,
              password: pwHash,
              stepNames: [],
              users: [credentials[0]]
            }, function (err) {
              if(err) {
                console.log("Error saving Project:", err);
                res.send('Error saving Project.', 337);
              } else {
                res.redirect(global.uriPrefix + '/manage');
              }
            });
          } else {
            res.send('Project already exists.', 337);
          }
        }
      });
    });

    app.get(global.uriPrefix + '/manage/:project', global.basicAuth.user, function (req, res) {
      if(req.session) {
        req.session.project = req.params.project;
      }
      global.Project.getProjectsFromDB(req.params.project, function (err, projects) {
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
            uriPrefix: global.uriPrefix,
            userList: userList
          });
        } else {
          res.send('Error: Project not found.', 404);
        }
      });
    });

    app.post(global.uriPrefix + '/useradd/:user', global.basicAuth.user, function (req, res) {
      /*log "useradd:", req.params.user*/
      if(req.session && req.session.project) {
        global.Project.updateProjectUserAddToDB(req.session.project, req.params.user, function (err) {
          if(err) {
            console.log("Error adding User:", err);
            res.send('Error adding User.', 337);
          } else {
            /*log "Added User '" + req.params.user + "' to Project", req.session.project*/
            res.redirect(global.uriPrefix + '/manage/' + req.session.project);
          }
        });
      } else {
        res.redirect(global.uriPrefix + '/session_not_found.html');
      }
    });
  
    app.post(global.uriPrefix + '/userdel/:user', global.basicAuth.user, function (req, res) {
      if(req.session && req.session.project) {
        global.Project.updateProjectUserDelFromDB(req.session.project, req.params.user, function (err) {
          if(err) {
            console.log("Error deleting User:", err);
            res.send('Error deleting User.', 337);
          } else {
            res.redirect(global.uriPrefix + '/manage/' + req.session.project);
          }
        });
      } else {
        res.redirect(global.uriPrefix + '/session_not_found.html');
      }
    });
  }

  app.get('*/favicon.ico', function (req, res) {
    res.end();
  });
  
  // Remove uriPrefix from Request-Uri to serve the correct static content
  app.get(global.uriPrefix + '/*', function (req, res, next) {
    req.url = req.url.slice(global.uriPrefix.length);
    next();
  });

  app.post(global.uriPrefix + '/*', function (req, res, next) {
    req.url = req.url.slice(global.uriPrefix.length);
    next();
  });



  if(!module.parent) {
    /**
     * Express: Start Server
     */

    app.listen(global.port);
    console.log("Port: %d, UriPrefix: %s, LocalMode: %s\n  ***  %s mode  ***", app.address().port, global.uriPrefix, global.config.useLocalMode, app.settings.env);
  }
});

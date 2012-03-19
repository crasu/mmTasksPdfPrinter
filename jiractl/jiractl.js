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
      mongoose: require('mongoose'),
      bcrypt: require('bcrypt'),
      configParser: require('configparser')
    };



/**
 * Init Config File
 */

global.config = global.fs.readFileSync(__dirname + '/config', 'utf8');
global.port = process.env.PORT || global.configParser.getPropertyValue('port', global.config);



/**
 * Init Express Server:
 */

if(global.configParser.getPropertyValue('useInternalHTTPS', global.config) === 'yes') {
  var options = {
    key: require('fs').readFileSync(__dirname + '/ssl/jiractl-key.pem'),
    cert: require('fs').readFileSync(__dirname + '/ssl/jiractl-cert.pem')
  },
  app = module.exports = express.createServer(options);
  console.log("DEBUG: Using internal HTTPS-Support");
} else {
  var app = module.exports = express.createServer();
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
      /*log "Transfer:", filename, "succesful."*/
    }
  };
};

global.uriPrefix = global.configParser.getPropertyValue('uriPrefix', global.config, '');
if(global.uriPrefix && global.uriPrefix[0] !== '/') {
  global.uriPrefix = '/' + global.uriPrefix;
}



/**
 * Mongoose: Init MongoDB
 */

global.mongoose.connect('mongodb://' + ((global.configParser.getPropertyValue('mongoUser', global.config) && global.configParser.getPropertyValue('mongoPass', global.config)) ? (global.configParser.getPropertyValue('mongoUser', global.config) + ':' + global.configParser.getPropertyValue('mongoPass', global.config) + '@') : '') + global.configParser.getPropertyValue('mongoHost', global.config) + (global.configParser.getPropertyValue('mongoPort', global.config) ? (':' + global.configParser.getPropertyValue('mongoPort', global.config)) : '') + '/' + global.configParser.getPropertyValue('mongoDB', global.config));
global.dbSchemas = {
  task: new global.mongoose.Schema({
    project: Number,
    jiraTask: Number,
    statusCode: Number,
    user: String
  }),
  project: new global.mongoose.Schema({
    project: Number,
    stepNames: [String],
    password: String,
    users: [String]
  })
};
global.Task = global.mongoose.model('task', global.dbSchemas.task);
global.Project = global.mongoose.model('project', global.dbSchemas.project);



/**
 * Database Communication
 */

global.getTasksFromDB = function (projectId, callback) {
  if(callback) {
    if(projectId) {
      global.Task.find({
        project: projectId
      }, callback);
    } else {
      callback(new Error("Invalid ProjectId passed to getTasksFromDB"));
    }
  } else {
    throw new Error("No Callback passed to getTasksFromDB");
  }
};

/*testcbnoe global.getTasksFromDB, [1], "Get Tasks From DB"*/
/*throws global.getTasksFromDB, [], function (err) {return true;}, "Get Tasks From DB: Throw an Error if no Arguments are passed"*/
/*throws global.getTasksFromDB, [1], function (err) {return true;}, "Get Tasks From DB: Throw an Error if no Callback is passed"*/
/*testcberror global.getTasksFromDB, [undefined], "Get Tasks From DB: Reject undefined ProjectId"*/

global.getProjectsFromDB = function (projectId, callback) {
  if(callback) {
    if(projectId) {
      global.Project.find({
        project: projectId
      }, callback);
    } else {
      callback(new Error("Invalid ProjectId passed to getProjectsFromDB"));
    }
  } else {
    throw new Error("No Callback passed to getProjectsFromDB");
  }
};

/*testcbnoe global.getProjectsFromDB, [1], "Get Projects From DB"*/
/*throws global.getProjectsFromDB, [], function (err) {return true;}, "Get Projects From DB: Throw an Error if no Arguments are passed"*/
/*throws global.getProjectsFromDB, [1], function (err) {return true;}, "Get Projects From DB: Throw an Error if no Callback is passed"*/
/*testcberror global.getProjectsFromDB, [undefined], "Get Projects From DB: Reject undefined ProjectId"*/

global.getAllProjectsFromDB = function (callback) {
  if(callback) {
    global.Project.find({ }, callback);
  } else {
    throw new Error("No Callback passed to getProjectsFromDB");
  }
};

/*testcbnoe global.getAllProjectsFromDB, [], "Get All Projects From DB"*/
/*throws global.getAllProjectsFromDB, [], function (err) {return true;}, "Get All Projects From DB: Throw an Error if no Callback is passed"*/

global.saveTaskToDB = function (task, callback) {
  if(callback) {
    if(task && task.project && task.jiraTask && task.statusCode && task.user) {
      var dbTask = new global.Task();
      dbTask.project = task.project;
      dbTask.jiraTask = task.jiraTask;
      dbTask.statusCode = task.statusCode;
      dbTask.user = task.user;
      dbTask.save(callback);
    } else {
      callback(new Error("Invalid Task passed to saveTaskToDB"));
    }
  } else {
   throw new Error("No Callback passed to saveTaskToDB");
  }
};

/*testcbnoe global.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3, user: "testuser"}], "Save Task to DB"*/
/*throws global.saveTaskToDB, [], function (err) {return true;}, "Save Task To DB: Throw an Error if no Arguments are passed"*/
/*throws global.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3, user: "testuser"}], function (err) {return true;}, "Save Task To DB: Throw an Error if no Callback is passed"*/
/*testcberror global.saveTaskToDB, [undefined], "Save Task To DB: Reject undefined Task"*/
/*testcberror global.saveTaskToDB, [{jiraTask: 2, statusCode: 3, user: "testuser"}], "Save Task To DB: Reject Task without ProjectId"*/
/*testcberror global.saveTaskToDB, [{project: 1, statusCode: 3, user: "testuser"}], "Save Task To DB: Reject Task without Jira-Task"*/
/*testcberror global.saveTaskToDB, [{project: 1, jiraTask: 2, user: "testuser"}], "Save Task To DB: Reject Task without Status-Code"*/
/*testcberror global.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3}], "Save Task To DB: Reject Task without User"*/

global.saveProjectToDB = function (project, callback) {
  if(callback) {
    if(project && project.project && project.stepNames && project.password && project.users) {
      global.getProjectsFromDB(project.project, function (err, projects) {
        if(!err && projects && projects.length === 0) {
          var dbProject = new global.Project();
          dbProject.project = project.project;
          dbProject.stepNames = project.stepNames;
          dbProject.password = project.password;
          dbProject.users = project.users;
          dbProject.save(callback);
        } else {
          if(err) {
            /*print "ERROR:".bold.red, err*/
          } else {
            /*print "Found Projects in DB:".cyan, projects*/
          }
          callback(null);
        }
      });
    } else {
      /*print "Invalid Project passed to saveProjectToDB:", project*/
      callback(new Error("Invalid Project passed to saveProjectToDB"));
    }
  } else {
    throw new Error("No Callback passed to saveProjectToDB");
  }
};

/*testcbnoe global.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty", users: ["test"]}], "Save Project to DB"*/
/*throws global.saveProjectToDB, [], function (err) {return true;}, "Save Project To DB: Throw an Error if no Arguments are passed"*/
/*throws global.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty", users: []}], function (err) {return true;}, "Save Project To DB: Throw an Error if no Callback is passed"*/
/*testcberror global.saveProjectToDB, [undefined], "Save Project To DB: Reject undefined Project"*/
/*testcberror global.saveProjectToDB, [{stepNames: [], password: "maynotbeempty", users: []}], "Save Project To DB: Reject Project without ProjectId"*/
/*testcberror global.saveProjectToDB, [{project: 1, password: "maynotbeempty", users: []}], "Save Project To DB: Reject Project without stepNames"*/
/*testcberror global.saveProjectToDB, [{project: 1, stepNames: [], users: []}], "Save Project To DB: Reject Project without Password"*/
/*testcberror global.saveProjectToDB, [{project: 1, stepNames: [], password: "", users: []}], "Save Project To DB: Reject Project with empty Password"*/
/*testcberror global.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty"}], "Save Project To DB: Reject Project without Users"*/

global.updateProjectStepNamesToDB = function (project, callback) {
  if(callback) {
    if(project && project.project && project.stepNames) {
      global.Project.update({
        project: project.project
      }, {
        stepNames: project.stepNames
      }, { }, callback);
    } else {
      callback(new Error("Invalid Project passed to updateProjectStepNamesToDB"));
    }
  } else {
    throw new Error("No Callback passed to updateProjectStepNamesToDB");
  }
};

/*testcbnoe global.updateProjectStepNamesToDB, [{project: 1, stepNames: ["Start Progress", "Stop Progress"]}], "Update Project Step-Names To DB"*/
/*throws global.updateProjectStepNamesToDB, [], function (err) {return true;}, "Update Project Step-Names To DB: Throw an Error if no Arguments are passed"*/
/*throws global.updateProjectStepNamesToDB, [{project: 1, stepNames: ["Start Progress", "Stop Progress"]}], function (err) {return true;}, "Update Project Step-Names To DB: Throw an Error if no Callback is passed"*/
/*testcberror global.updateProjectStepNamesToDB, [undefined], "Update Project Step-Names To DB: Reject undefined Project"*/
/*testcberror global.updateProjectStepNamesToDB, [{stepNames: []}], "Update Project Step-Names To DB: Reject Project without ProjectId"*/
/*testcberror global.updateProjectStepNamesToDB, [{project: 1}], "Update Project Step-Names To DB: Reject Project without Step-Names"*/

global.updateProjectUserAddToDB = function (projectId, user, callback) {
  if(callback) {
    if(projectId && user) {
      global.Project.update({
        project: projectId
      }, {
        $push: { users: user }
      }, { }, callback);
    } else {
      callback(new Error("Invalid Username or ProjectId passed to updateProjectUserAddToDB"));
    }
  } else {
    throw new Error("No Callback passed to updateProjectUserAddToDB");
  }
};

/*testcbnoe global.updateProjectUserAddToDB, [1, "testuser"], "Update Project: User Add To DB"*/
/*throws global.updateProjectUserAddToDB, [], function (err) {return true;}, "Update Project: User Add To DB: Throw an Error if no Arguments are passed"*/
/*throws global.updateProjectUserAddToDB, [1, "testuser"], function (err) {return true;}, "Update Project: User Add To DB: Throw an Error if no Callback is passed"*/
/*testcberror global.updateProjectUserAddToDB, [undefined, undefined], "Update Project: User Add To DB: Reject undefined ProjectId and User"*/
/*testcberror global.updateProjectUserAddToDB, [undefined, "testuser"], "Update Project: User Add To DB: Reject undefined ProjectId"*/
/*testcberror global.updateProjectUserAddToDB, [1, undefined], "Update Project: User Add To DB: Reject undefined User"*/

global.updateProjectUserDelFromDB = function (projectId, user, callback) {
  if(callback) {
    if(projectId && user) {
      global.Project.update({
        project: projectId
      }, {
        $pull: { users: user }
      }, { }, callback);
    } else {
      callback(new Error("Invalid Username or ProjectId passed to updateProjectUserDelFromDB"));
    }
  } else {
    throw new Error("No Callback passed to updateProjectUserDelFromDB");
  }
};

/*testcbnoe global.updateProjectUserDelFromDB, [1, "testuser"], "Update Project: User Del From DB"*/
/*throws global.updateProjectUserDelFromDB, [], function (err) {return true;}, "Update Project: User Del From DB: Throw an Error if no Arguments are passed"*/
/*throws global.updateProjectUserDelFromDB, [1, "testuser"], function (err) {return true;}, "Update Project: User Del From DB: Throw an Error if no Callback is passed"*/
/*testcberror global.updateProjectUserDelFromDB, [undefined, undefined], "Update Project: User Add To DB: Reject undefined ProjectId and User"*/
/*testcberror global.updateProjectUserDelFromDB, [undefined, "testuser"], "Update Project: User Add To DB: Reject undefined ProjectId"*/
/*testcberror global.updateProjectUserDelFromDB, [1, undefined], "Update Project: User Add To DB: Reject undefined User"*/

global.deleteProjectsFromDB = function (projectId, callback) {
  if(callback) {
    if(projectId) {
      // Removing all Task-Updates that belong to the Project
      global.Task.remove({
        project: projectId
      }, function (err) {
        if(err) {
          /*print "Aborting deleteProjectsFromDB:", err*/
          callback(err);
        } else {
          global.Project.remove({
            project: projectId
          }, callback);
        }
      });
    } else {
      callback(new Error("Invalid ProjectId passed to deleteProjectsFromDB"));
    }
  } else {
    throw new Error("No Callback passed to deleteProjectsFromDB");
  }
};

/*testcbnoe global.deleteProjectsFromDB, [1], "Delete Projects From DB"*/
/*throws global.deleteProjectsFromDB, [], function (err) {return true;}, "Delete Projects From DB: Throw an Error if no Arguments are passed"*/
/*throws global.deleteProjectsFromDB, [1], function (err) {return true;}, "Delete Projects From DB: Throw an Error if no Callback is passed"*/
/*testcberror global.deleteProjectsFromDB, [undefined], "Delete Projects From DB: Reject undefined "*/



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
    return (user === global.configParser.getPropertyValue('manageUser', global.config) && pass === global.configParser.getPropertyValue('managePass', global.config));
  },

  projectPass: function (req, res, next, validateUser) {
    var projectId = req.params.project || req.session.project;
    if(projectId) {
      global.getProjectsFromDB(projectId, function (err, projects) {
        if(err) {
          console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
          res.send('Error checking for existing Projects with this ID.', 337);
        } else {
          express.basicAuth(function (user, pass) {
            if(projects && projects[0] && projects[0].password) {
              if(validateUser && typeof validateUser === 'function') {
                return (validateUser(user, projects[0]) && global.bcrypt.compareSync(pass, projects[0].password));
              } else {
                return (global.bcrypt.compareSync(pass, projects[0].password));
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
    if(global.configParser.getPropertyValue('useInternalHTTPS', global.config) !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + global.configParser.getPropertyValue('hostname', global.config) + req.url);
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
    if(global.configParser.getPropertyValue('useInternalHTTPS', global.config) !== 'yes') {
      if(req.headers && req.headers['x-forwarded-proto']) {
        if(req.headers['x-forwarded-proto'] !== 'https') {
          res.redirect('https://' + global.configParser.getPropertyValue('hostname', global.config) + req.url);
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

  // Jiractl-local Polling
  app.post(global.uriPrefix + '/jiraupdate/:project', global.basicAuth.jira, function (req, res) {
    global.getTasksFromDB(req.params.project, function (err, tasksToUpdate) {
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
      global.updateProjectStepNamesToDB(project, function (err, updatedProject) {
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
      global.deleteProjectsFromDB(req.session.project, function (err, numberOfRemovedDocs) {
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
    global.getAllProjectsFromDB(function (err, projects) {
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
    global.getProjectsFromDB(req.params.project, function (err, projects) {
      if (err) {
        console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
        res.send('Error checking for existing Projects with this ID.', 337);
      } else {
        // Security note: Only use var-statements here because you have function-scope
        var pwSalt = global.bcrypt.genSaltSync(10);
        var pwHash = global.bcrypt.hashSync(req.params.password, pwSalt);
        if(projects.length === 0) {
          var credentials = global.basicAuth.getCredentials(req);
          global.saveProjectToDB({
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
    global.getProjectsFromDB(req.params.project, function (err, projects) {
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
      global.updateProjectUserAddToDB(req.session.project, req.params.user, function (err) {
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
      global.updateProjectUserDelFromDB(req.session.project, req.params.user, function (err) {
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

  app.get('*/favicon.ico', function (req, res) {
    res.end();
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
      global.getProjectsFromDB(req.params.project, function (err, projects) {
        if(err) {
          console.log("Error finding Project in DB:", err);
          res.redirect(global.uriPrefix + '/error.html');
        } else if(projects && projects[0] && projects[0].stepNames) {
          var i,
              taskUpdateList = "";
          for(i = 0; i < projects[0].stepNames.length; i++) {
            taskUpdateList += '<form method="post" action="' + global.uriPrefix + '/updatestatus/' + i + '"><input type="submit" value="' + projects[0].stepNames[i] + '" class="button" /></form>\n';
          }
          res.render('update', {
            uriPrefix: global.uriPrefix,
            taskUpdateList: taskUpdateList
          });
        } else {
          res.redirect(global.uriPrefix + '/error.html');
        }
      });
    } else {
      /*log "req.session:", req.session*/
      res.redirect(global.uriPrefix + '/session_not_found.html');
    }
  });
  
  app.post(global.uriPrefix + '/updatestatus/:statusCode', function (req, res) {
    if(req.session && req.session.task) {
      var task = req.session.task;
      req.session.destroy();
      task.statusCode = req.params.statusCode;
      /*log "Task:", task*/
      global.saveTaskToDB(task, function (err) {
        if(err) {
          res.redirect(global.uriPrefix + '/error.html');
        } else {
          res.redirect(global.uriPrefix + '/done.html');
        }
      });
    } else {
      res.redirect(global.uriPrefix + '/session_not_found.html');
    }
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
    console.log("Port: %d UriPrefix: %s\n  ***  %s mode  ***", app.address().port, global.uriPrefix, app.settings.env);
  }
});

/**
 * Standard Mode
 */
module.exports = function (config) {
  var bcrypt = require('bcrypt');

  var dbdao = require(__dirname + '/db_dao');
  dbdao.initialize(config.mongoUser, config.mongoPass, config.mongoHost, config.mongoPort, config.mongoDB);
  var Project = dbdao.Project;
  var Task = dbdao.Task;

  var interfaceObj = {
    getStepNames: function (projectId, taskId, callback) {
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
    },

    getTaskInfo: function (projectId, taskId, callback) {
      interfaceObj.getStepNames(projectId, taskId, function (err, stepNames) {
        if(err) {
          callback(err);
        } else {
          var taskInfo = {};
          taskInfo.stepNames = stepNames;
          callback(null, taskInfo);
        }
      });
    },

    updateTask: function (projectId, taskId, statusCode, user, callback) {
      var task = {
        project: projectId,
        jiraTask: taskId,
        statusCode: statusCode,
        user: user
      };
      Task.saveTaskToDB(task, callback);
    },

    comparePass: function (pass1, pass2) {
      return bcrypt.compareSync(pass1, pass2);
    },

    getProjects: Project.getProjectsFromDB,

    routes: {
      // Jiractl-local Polling
      jiraupdate: function (req, res) {
        Task.getTasksFromDB(req.params.project, function (err, tasksToUpdate) {
          if(err) {
            console.log("Error finding Tasks in DB:", err);
            res.end();
          } else {
            res.json(tasksToUpdate);
            Task.remove({
              project: req.params.project
            }, function (err, numberOfRemovedDocs) {
              if(err) {
                console.log("Error removing Tasks from DB:", err);
              }
            });
          }
        });
        if(req.body) {
          var project = {
            project: req.params.project,
            stepNames: []
          };
          var stepName;
          for(stepName in req.body) {
            if(req.body.hasOwnProperty(stepName)) {
              project.stepNames.push(req.body[stepName]);
            }
          }
          Project.updateProjectStepNamesToDB(project, function (err, updatedProject) {
            if(err) {
              console.log("Error updating Project's stepNames:", project);
            }
          });
        }
      },

      // Administration Interface
      close: function (req, res) {
        if(req.session && req.session.project) {
          Project.deleteProjectsFromDB(req.session.project, function (err, numberOfRemovedDocs) {
            if(err) {
              console.log("Error closing Project:", err);
              res.send('Error closing Project.', 337);
            } else {
              res.redirect(config.uriPrefix + '/manage');
            }
          });
        } else {
          res.redirect(config.uriPrefix + '/session_not_found.html');
        }
      },

      manage: function (req, res) {
        Project.getAllProjectsFromDB(function (err, projects) {
          if(err) {
            console.log("Error getting Projects from DB:", err);
            res.send('Error checking for existing Projects.', 337);
          } else {
            var projectList = "";
            if(projects) {
              var i;
              for(i = 0; i < projects.length; i++) {
                projectList += '<tr><td align="center"><a href="' + config.uriPrefix + '/projects/' + projects[i].project + '/manage">' + projects[i].project + '</a></td></tr>';
              }
            }
            res.render('manage', {
              uriPrefix: config.uriPrefix,
              projectList: projectList
            });
          }
        });
      },

      initProject: function (req, res) {
        Project.getProjectsFromDB(req.params.project, function (err, projects) {
          if (err) {
            console.log("Error getting Projects from DB with ID:", req.params.project, "\nERROR:", err);
            res.send('Error checking for existing Projects with this ID.', 337);
          } else {
            // Security note: Only use var-statements here because you have function-scope
            var pwSalt = bcrypt.genSaltSync(10);
            var pwHash = bcrypt.hashSync(req.body.password, pwSalt);
            if(projects.length === 0) {
              Project.saveProjectToDB({
                project: req.params.project,
                password: pwHash,
                stepNames: [],
                users: [req.remoteUser]
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
      },

      manageProject: function (req, res) {
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
      },

      useradd: function (req, res) {
        if(req.session && req.session.project) {
          Project.updateProjectUserAddToDB(req.session.project, req.params.user, function (err) {
            if(err) {
              console.log("Error adding User:", err);
              res.send('Error adding User.', 337);
            } else {
              res.redirect(config.uriPrefix + '/projects/' + req.session.project + '/manage');
            }
          });
        } else {
          res.redirect(config.uriPrefix + '/session_not_found.html');
        }
      },

      userdel: function (req, res) {
        if(req.session && req.session.project) {
          Project.updateProjectUserDelFromDB(req.session.project, req.params.user, function (err) {
            if(err) {
              console.log("Error deleting User:", err);
              res.send('Error deleting User.', 337);
            } else {
              res.redirect(config.uriPrefix + '/projects/' + req.session.project + '/manage');
            }
          });
        } else {
          res.redirect(config.uriPrefix + '/session_not_found.html');
        }
      }
    }
  };
  return interfaceObj;
};

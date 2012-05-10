module.exports = {
  initialize: function (mongoUser, mongoPass, mongoHost, mongoPort, mongoDB) {
    var mongoose = require('mongoose');
    mongoose.connect('mongodb://' + ((mongoUser && mongoPass) ? (mongoUser + ':' + mongoPass + '@') : '') + mongoHost + (mongoPort ? (':' + mongoPort) : '') + '/' + mongoDB);


    /**
     * Tasks
     */

    var taskSchema = new mongoose.Schema({
      project: Number,
      jiraTask: Number,
      statusCode: Number,
      user: String
    }),
      Task = mongoose.model('task', taskSchema);



    /**
     * Tasks: Database Communication
     */

    Task.getTasksFromDB = function (projectId, callback) {
      if(callback) {
        if(projectId) {
          Task.find({
            project: projectId
          }, callback);
        } else {
          callback(new Error("Invalid ProjectId passed to getTasksFromDB"));
        }
      } else {
        throw new Error("No Callback passed to getTasksFromDB");
      }
    };

    /*testcbnoe Task.getTasksFromDB, [1], "Get Tasks From DB"*/
    /*throws Task.getTasksFromDB, [], function (err) {return true;}, "Get Tasks From DB: Throw an Error if no Arguments are passed"*/
    /*throws Task.getTasksFromDB, [1], function (err) {return true;}, "Get Tasks From DB: Throw an Error if no Callback is passed"*/
    /*testcberror Task.getTasksFromDB, [undefined], "Get Tasks From DB: Reject undefined ProjectId"*/

    Task.saveTaskToDB = function (task, callback) {
      if(callback) {
        if(task && task.project && task.jiraTask && task.statusCode && task.user) {
          var dbTask = new Task();
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

    /*testcbnoe Task.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3, user: "testuser"}], "Save Task to DB"*/
    /*throws Task.saveTaskToDB, [], function (err) {return true;}, "Save Task To DB: Throw an Error if no Arguments are passed"*/
    /*throws Task.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3, user: "testuser"}], function (err) {return true;}, "Save Task To DB: Throw an Error if no Callback is passed"*/
    /*testcberror Task.saveTaskToDB, [undefined], "Save Task To DB: Reject undefined Task"*/
    /*testcberror Task.saveTaskToDB, [{jiraTask: 2, statusCode: 3, user: "testuser"}], "Save Task To DB: Reject Task without ProjectId"*/
    /*testcberror Task.saveTaskToDB, [{project: 1, statusCode: 3, user: "testuser"}], "Save Task To DB: Reject Task without Jira-Task"*/
    /*testcberror Task.saveTaskToDB, [{project: 1, jiraTask: 2, user: "testuser"}], "Save Task To DB: Reject Task without Status-Code"*/
    /*testcberror Task.saveTaskToDB, [{project: 1, jiraTask: 2, statusCode: 3}], "Save Task To DB: Reject Task without User"*/

    Task.deleteTasksFromDB = function (projectId, taskId, callback) {
      if(callback) {
        if(projectId) {
          if(taskId) {
            // Removing all Task-Updates that belong to the Task
            Task.remove({
              project: projectId,
              jiraTask: taskId
            }, callback);
          } else {
            callback(new Error("Invalid TaskId passed to deleteTasksFromDB"));
          }
        } else {
          callback(new Error("Invalid ProjectId passed to deleteTasksFromDB"));
        }
      } else {
        throw new Error("No Callback passed to deleteTasksFromDB");
      }
    };

    /*testcbnoe Task.deleteTasksFromDB, [1, 1], "Delete Tasks From DB"*/
    /*throws Task.deleteTasksFromDB, [], function (err) {return true;}, "Delete Tasks From DB: Throw an Error if no Arguments are passed"*/
    /*throws Task.deleteTasksFromDB, [1, 1], function (err) {return true;}, "Delete Tasks From DB: Throw an Error if no Callback is passed"*/
    /*testcberror Task.deleteTasksFromDB, [undefined, undefined], "Delete Tasks From DB: Reject undefined ProjectId and TaskId"*/
    /*testcberror Task.deleteTasksFromDB, [undefined, 1], "Delete Tasks From DB: Reject undefined ProjectId"*/
    /*testcberror Task.deleteTasksFromDB, [1, undefined], "Delete Tasks From DB: Reject undefined TaskId"*/

    module.exports.Task = Task;



    /**
     * Projects
     */

    var projectSchema = new mongoose.Schema({
      project: Number,
      stepNames: [String],
      password: String,
      users: [String]
    }),
      Project = mongoose.model('project', projectSchema);



    /**
     * Projects: Database Communication
     */

    Project.getProjectsFromDB = function (projectId, callback) {
      if(callback) {
        if(projectId) {
          Project.find({
            project: projectId
          }, callback);
        } else {
          callback(new Error("Invalid ProjectId passed to getProjectsFromDB"));
        }
      } else {
        throw new Error("No Callback passed to getProjectsFromDB");
      }
    };

    /*testcbnoe Project.getProjectsFromDB, [1], "Get Projects From DB"*/
    /*throws Project.getProjectsFromDB, [], function (err) {return true;}, "Get Projects From DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.getProjectsFromDB, [1], function (err) {return true;}, "Get Projects From DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.getProjectsFromDB, [undefined], "Get Projects From DB: Reject undefined ProjectId"*/

    Project.getAllProjectsFromDB = function (callback) {
      if(callback) {
        Project.find({ }, callback);
      } else {
        throw new Error("No Callback passed to getProjectsFromDB");
      }
    };

    /*testcbnoe Project.getAllProjectsFromDB, [], "Get All Projects From DB"*/
    /*throws Project.getAllProjectsFromDB, [], function (err) {return true;}, "Get All Projects From DB: Throw an Error if no Callback is passed"*/

    Project.saveProjectToDB = function (project, callback) {
      if(callback) {
        if(project && project.project && project.stepNames && project.password && project.users) {
          Project.getProjectsFromDB(project.project, function (err, projects) {
            if(!err && projects && projects.length === 0) {
              var dbProject = new Project();
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

    /*testcbnoe Project.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty", users: ["test"]}], "Save Project to DB"*/
    /*throws Project.saveProjectToDB, [], function (err) {return true;}, "Save Project To DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty", users: []}], function (err) {return true;}, "Save Project To DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.saveProjectToDB, [undefined], "Save Project To DB: Reject undefined Project"*/
    /*testcberror Project.saveProjectToDB, [{stepNames: [], password: "maynotbeempty", users: []}], "Save Project To DB: Reject Project without ProjectId"*/
    /*testcberror Project.saveProjectToDB, [{project: 1, password: "maynotbeempty", users: []}], "Save Project To DB: Reject Project without stepNames"*/
    /*testcberror Project.saveProjectToDB, [{project: 1, stepNames: [], users: []}], "Save Project To DB: Reject Project without Password"*/
    /*testcberror Project.saveProjectToDB, [{project: 1, stepNames: [], password: "", users: []}], "Save Project To DB: Reject Project with empty Password"*/
    /*testcberror Project.saveProjectToDB, [{project: 1, stepNames: [], password: "maynotbeempty"}], "Save Project To DB: Reject Project without Users"*/

    Project.updateProjectStepNamesToDB = function (project, callback) {
      if(callback) {
        if(project && project.project && project.stepNames) {
          Project.update({
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

    /*testcbnoe Project.updateProjectStepNamesToDB, [{project: 1, stepNames: ["Start Progress", "Stop Progress"]}], "Update Project Step-Names To DB"*/
    /*throws Project.updateProjectStepNamesToDB, [], function (err) {return true;}, "Update Project Step-Names To DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.updateProjectStepNamesToDB, [{project: 1, stepNames: ["Start Progress", "Stop Progress"]}], function (err) {return true;}, "Update Project Step-Names To DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.updateProjectStepNamesToDB, [undefined], "Update Project Step-Names To DB: Reject undefined Project"*/
    /*testcberror Project.updateProjectStepNamesToDB, [{stepNames: []}], "Update Project Step-Names To DB: Reject Project without ProjectId"*/
    /*testcberror Project.updateProjectStepNamesToDB, [{project: 1}], "Update Project Step-Names To DB: Reject Project without Step-Names"*/

    Project.updateProjectUserAddToDB = function (projectId, user, callback) {
      if(callback) {
        if(projectId && user) {
          Project.update({
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

    /*testcbnoe Project.updateProjectUserAddToDB, [1, "testuser"], "Update Project: User Add To DB"*/
    /*throws Project.updateProjectUserAddToDB, [], function (err) {return true;}, "Update Project: User Add To DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.updateProjectUserAddToDB, [1, "testuser"], function (err) {return true;}, "Update Project: User Add To DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.updateProjectUserAddToDB, [undefined, undefined], "Update Project: User Add To DB: Reject undefined ProjectId and User"*/
    /*testcberror Project.updateProjectUserAddToDB, [undefined, "testuser"], "Update Project: User Add To DB: Reject undefined ProjectId"*/
    /*testcberror Project.updateProjectUserAddToDB, [1, undefined], "Update Project: User Add To DB: Reject undefined User"*/

    Project.updateProjectUserDelFromDB = function (projectId, user, callback) {
      if(callback) {
        if(projectId && user) {
          Project.update({
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

    /*testcbnoe Project.updateProjectUserDelFromDB, [1, "testuser"], "Update Project: User Del From DB"*/
    /*throws Project.updateProjectUserDelFromDB, [], function (err) {return true;}, "Update Project: User Del From DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.updateProjectUserDelFromDB, [1, "testuser"], function (err) {return true;}, "Update Project: User Del From DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.updateProjectUserDelFromDB, [undefined, undefined], "Update Project: User Add To DB: Reject undefined ProjectId and User"*/
    /*testcberror Project.updateProjectUserDelFromDB, [undefined, "testuser"], "Update Project: User Add To DB: Reject undefined ProjectId"*/
    /*testcberror Project.updateProjectUserDelFromDB, [1, undefined], "Update Project: User Add To DB: Reject undefined User"*/

    Project.deleteProjectsFromDB = function (projectId, callback) {
      if(callback) {
        if(projectId) {
          // Removing all Task-Updates that belong to the Project
          if(Task) {
            Task.remove({
              project: projectId
            }, function (err) {
              if(err) {
                /*print "Aborting deleteProjectsFromDB:", err*/
                callback(err);
              } else {
                Project.remove({
                  project: projectId
                }, callback);
              }
            });
          } else {
            Project.remove({
              project: projectId
            }, callback);
          }
        } else {
          callback(new Error("Invalid ProjectId passed to deleteProjectsFromDB"));
        }
      } else {
        throw new Error("No Callback passed to deleteProjectsFromDB");
      }
    };

    /*testcbnoe Project.deleteProjectsFromDB, [1], "Delete Projects From DB"*/
    /*throws Project.deleteProjectsFromDB, [], function (err) {return true;}, "Delete Projects From DB: Throw an Error if no Arguments are passed"*/
    /*throws Project.deleteProjectsFromDB, [1], function (err) {return true;}, "Delete Projects From DB: Throw an Error if no Callback is passed"*/
    /*testcberror Project.deleteProjectsFromDB, [undefined], "Delete Projects From DB: Reject undefined "*/

    module.exports.Project = Project;
  }
};

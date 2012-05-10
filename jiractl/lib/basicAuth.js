/**
 * Authentication Middleware:
 */
module.exports = function (express, config, getProjects, comparePass) {
  var async = require('async');

  var retrieveProjects = function (projectId) {
    return function (cb) {
      getProjects(projectId, cb);
    };
  };
  var validateFirstProject = function (projects, cb) {
    if(projects && projects[0] && projects[0].password) {
      cb(null, projects[0]);
    } else {
      cb(new Error("Received invalid Project from DB."));
    }
  };

  var basicAuth = {
    user: function (projectId) {
      return function (user, pass, callback) {
        async.waterfall([
          retrieveProjects(projectId),
          validateFirstProject,
          function (project, cb) {
            if((project.users.indexOf(user) !== -1) && (comparePass(pass, project.password))) {
              cb(null, user);
            } else {
              cb(null, null);
            }
          }
        ], callback);
      };
    },

    jira: function (projectId) {
      return function (user, pass, callback) {
        async.waterfall([
          retrieveProjects(projectId),
          validateFirstProject,
          function (project, cb) {
            if((user === 'jira') && (comparePass(pass, project.password))) {
              callback(null, user);
            } else {
              callback(null, null);
            }
          }
        ], callback);
      };
    },

    admin: function (user, pass) {
      return (user === config.manageUser && pass === config.managePass);
    }
  };

  var getProjectIdFromRequest = function (req) {
    return function (cb) {
      var projectId = req.params.project || req.session.project;
      if(projectId) {
        cb(null, projectId);
      } else {
        cb(new Error("ProjectId not set - can't check authentication for this route ('" + req.url + "'). Denying access."));
      }
    };
  };

  return {
    user: function (req, res, next) {
      async.waterfall([
        getProjectIdFromRequest(req),
        function (projectId, cb) {
          express.basicAuth(basicAuth.user(projectId))(req, res, next);
        }
      ], function (err) {
        if(err) {
          console.log(err);
          express.basicAuth(function (user, pass) {
            return false;
          })(req, res, next);
        }
      });
    },

    jira: function (req, res, next) {
      async.waterfall([
        getProjectIdFromRequest(req),
        function (projectId, cb) {
          express.basicAuth(basicAuth.jira(projectId))(req, res, next);
        }
      ], function (err) {
        if(err) {
          console.log(err);
          express.basicAuth(function (user, pass) {
            return false;
          })(req, res, next);
        }
      });
    },

    admin: function (req, res, next) {
      express.basicAuth(basicAuth.admin)(req, res, next);
    }
  };
};

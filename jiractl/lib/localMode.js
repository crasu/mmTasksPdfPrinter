/**
 * Local Mode
 */

var fs = require('fs');
var async = require('async');

var defaultLocalConfig = {
  "jiraCliPath": '',
  "jiraUrl": 'http://localhost:8080',
  "jiraUser": '',
  "jiraPass": '',
  "projectName": '',
  "projectPass": '',
  "users": '["admin"]'
};
try {
  var localConfigFile = fs.readFileSync(__dirname + '/../localconfig.json', 'utf8');
  var localconfig = require(__dirname + '/readConfig').readConfig(localConfigFile, defaultLocalConfig);
} catch(err) {
  console.log(err);
  process.exit(1);
}

var jiraconnector = require('jiraconnector');

module.exports = {
  getStepNames: function (projectId, taskId, callback) {
    jiraconnector.getAvailableWorksteps(localconfig, taskId, callback);
  },

  getTaskInfo: function (projectId, taskId, callback) {
    async.parallel({
      stepNames: function (cb) {
        jiraconnector.getAvailableWorksteps(localconfig, taskId, cb);
      },
      issueInfo: function (cb) {
        jiraconnector.getIssueInfo(localconfig, taskId, cb);
      }
    }, callback);
  },

  updateTask: function (projectId, taskId, statusCode, user, callback) {
    async.waterfall([
      function (cb) {
        module.exports.getStepNames(projectId, taskId, cb);
      },
      function (stepNames, cb) {
        jiraconnector.progressTask(localconfig, taskId, stepNames[statusCode], user, cb);
      }
    ], callback);
  },

  comparePass: function (pass1, pass2) {
    return (pass1 === pass2);
  },

  getProjects: function (projectId, callback) {
    callback(null, [{
      project: localconfig.projectName,
      password: localconfig.projectPass,
      users: localconfig.users
    }]);
  },

  routes: { }
};

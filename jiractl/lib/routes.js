/**
 * Express: Routes
 */
module.exports = function (config, getTaskInfo, updateTask) {
  return {
    redirect: function (req, res, next) {
      if(!config.useInternalHTTPS) {
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
    },

    // AgileCards Jira-Plugin-Support
    updateJiraKey: function (req, res, next) {
      if(req.params.jiraKey.indexOf('-') !== -1) {
        req.url = config.uriPrefix + '/update/' + req.params.jiraKey.replace('-', '/');
        next();
      }
    },

    // Incoming updates from mobile clients
    updateProjectTask: function (req, res) {
      if(req.session) {
        req.session.task = {
          project: req.params.project,
          jiraTask: req.params.jiraTask,
          user: req.remoteUser
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
        res.redirect(config.uriPrefix + '/session_not_found.html');
      }
    },

    updatestatus: function (req, res) {
      if(req.session && req.session.task && req.session.task.project && req.session.task.jiraTask) {
        updateTask(req.session.task.project, req.session.task.jiraTask, req.params.statusCode, req.session.task.user, function (err) {
          if(err) {
            console.log("Error on updatestatus:", err.toString(), "Task:", req.session.task);
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
    },

    favicon: function (req, res) {
      res.end();
    },
    
    // Remove uriPrefix from Request-Uri to serve the correct static content
    removePrefix: function (req, res, next) {
      req.url = req.url.slice(config.uriPrefix.length);
      next();
    }
  };
};

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
    async = require('async');

var app;

var defaultConfig = {
  "useInternalHTTPS": false,
  "useLocalMode": false,
  "uriPrefix": "",
  "manageUser": "admin",
  "managePass": "admin"
};

try {
  var configFile = fs.readFileSync(__dirname + '/config.json', 'utf8');
  var config = require(__dirname + '/lib/readConfig').readConfig(configFile, defaultConfig);
  config.uriPrefix = require(__dirname + '/lib/readConfig').ensureSlashPrefix(config.uriPrefix);
} catch(err) {
  process.exit(1);
}



/**
 * Init Express Server:
 */

if(config.useInternalHTTPS) {
  var options = {
    key: require('fs').readFileSync(__dirname + '/ssl/jiractl-key.pem'),
    cert: require('fs').readFileSync(__dirname + '/ssl/jiractl-cert.pem')
  },
  app = module.exports = express.createServer(options);
  console.log("DEBUG: Using internal HTTPS-Support");
} else {
  app = module.exports = express.createServer();
}

config.useLocalMode = (config.useLocalMode);



/**
 * General functionality
 */

var getStepNames;
var getTaskInfo;
var updateTask;
var comparePass;
var getProjects;
var modeSpecificRoutes;

var initInterface = function (interfaceObj) {
  getStepNames = interfaceObj.getStepNames;
  getTaskInfo = interfaceObj.getTaskInfo;
  updateTask = interfaceObj.updateTask;
  comparePass = interfaceObj.comparePass;
  getProjects = interfaceObj.getProjects;
  modeSpecificRoutes = interfaceObj.routes;
};

if(config.useLocalMode) {
  initInterface(require(__dirname + '/lib/localMode'));
} else {
  initInterface(require(__dirname + '/lib/normalMode')(config));
}

var basicAuth = require(__dirname + '/lib/basicAuth')(express, config, getProjects, comparePass);

/**
 * Express: Configuration
 */
async.parallel({
  secretSessionHashKey: function (cb) {
    cp.exec('openssl rand -base64 48', {
      encoding: 'utf8',
      timeout: '0',
      maxBuffer: 200*1024,
      killSignal: 'SIGTERM',
      cwd: null,
      env: null
    }, function (err, stdout, stderr) {
      cb(err, stdout);
    });
  }
}, function (err, result) {
  if(err) {
    console.log("ERROR: " + err);
  } else {
    var secretSessionHashKey;
    if(!secretSessionHashKey) {
      console.log("WARN: Using default random key to generate session hash codes...");
      secretSessionHashKey = "default-random-key-8738675454u89732375789ncgv8fvhnfcdhsduyigbfzucgnsdugkgngnuewhbdkufy4egvnjehcgfykzegb2skuvfngyezucbygsnucbfygukzegcuzgdfbgcysesfgdn";
    } else {
      secretSessionHashKey = result.secretSessionHashKey;
    }

    /*notequal secretSessionHashKey, "default-random-key-8738675454u89732375789ncgv8fvhnfcdhsduyigbfzucgnsdugkgngnuewhbdkufy4egvnjehcgfykzegb2skuvfngyezucbygsnucbfygukzegcuzgdfbgcysesfgdn", "Use random generated SessionHashKey"*/

    require(__dirname + '/lib/configureApplication')(app, express, secretSessionHashKey); 
    var routes = require(__dirname + '/lib/routes')(config, getTaskInfo, updateTask);

    app.all('/*', routes.redirect);
    app.get(config.uriPrefix + '/update/:jiraKey', routes.updateJiraKey);
    app.get(config.uriPrefix + '/update/:project/:jiraTask', basicAuth.user, routes.updateProjectTask);
    app.post(config.uriPrefix + '/updatestatus/:statusCode', routes.updatestatus);

    if(!config.useLocalMode) {
      app.post(config.uriPrefix + '/jiraupdate/:project', basicAuth.jira, modeSpecificRoutes.jiraupdate);
      app.post(config.uriPrefix + '/close', basicAuth.user, modeSpecificRoutes.close);
      app.get(config.uriPrefix + '/manage', basicAuth.admin, modeSpecificRoutes.manage);
      app.post(config.uriPrefix + '/projects/:project/init', basicAuth.admin, modeSpecificRoutes.initProject);
      app.get(config.uriPrefix + '/projects/:project/manage', basicAuth.user, modeSpecificRoutes.manageProject);
      app.post(config.uriPrefix + '/users/:user/add', basicAuth.user, modeSpecificRoutes.useradd);
      app.post(config.uriPrefix + '/users/:user/delete', basicAuth.user, modeSpecificRoutes.userdel);
    }

    app.get('*/favicon.ico', routes.favicon);
    app.all(config.uriPrefix + '/*', routes.removePrefix);



    if(!module.parent) {
      /**
       * Express: Start Server
       */

      app.listen(process.env.PORT || config.port);
      console.log("Port: %d, UriPrefix: %s, LocalMode: %s\n  ***  %s mode  ***", app.address().port, config.uriPrefix, config.useLocalMode, app.settings.env);
    }
  }
});

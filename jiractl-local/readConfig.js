module.exports = function (fs) {
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
    "jiraUpdateInterval": 300000,
    "jiractlHost": 'jiractl.herokuapp.com',
    "jiractlHostPort": '443',
    "jiractlUriPrefix": '',
    "jiractlProjectPass": '',
    "jiraCliPath": '',
    "jiraUrl": 'http://localhost:8080',
    "jiraUser": '',
    "jiraPass": '',
    "projectName": ''
  };
  if(defaultConfig.jiractlUriPrefix && defaultConfig.jiractlUriPrefix[0] !== '/') {
    defaultConfig.jiractlUriPrefix = '/' + defaultConfig.jiractlUriPrefix;
  }
  var prop;
  for(prop in defaultConfig) {
    if(typeof config[prop] === 'undefined') {
      config[prop] = defaultConfig[prop];
    }
  }

  /*log "Reading Config:", config*/

  return config;
};

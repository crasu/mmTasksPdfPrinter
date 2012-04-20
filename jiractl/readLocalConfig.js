module.exports = function () {
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

  /*log "Read Local-Config:", config.localconfig*/

  return localconfig;
};

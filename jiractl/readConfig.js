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
    "useInternalHTTPS": "no",
    "useLocalMode": "no",
    "uriPrefix": "",
    "manageUser": "admin",
    "managePass": "admin"
  };
  var prop;
  for(prop in defaultConfig) {
    if(typeof config[prop] === 'undefined') {
      config[prop] = defaultConfig[prop];
    }
  }

  if(config.uriPrefix && config.uriPrefix[0] !== '/') {
    config.uriPrefix = '/' + config.uriPrefix;
  }

  if(process.env.PORT) {
    config.port = process.env.PORT;
  }

  /*log "Read Config:", config*/

  return config;
};

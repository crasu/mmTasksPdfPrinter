var mergeConfigurations = function (config, defaultConfig) {
  if(typeof config === 'object' && typeof defaultConfig === 'object') {
    var prop;
    for(prop in defaultConfig) {
      if((defaultConfig.hasOwnProperty(prop)) && (typeof config[prop] === 'undefined')) {
        config[prop] = defaultConfig[prop];
      }
    }
  }
  return config;
};

module.exports = {
  ensureSlashPrefix: function (property) {
    if(typeof property === 'string' && typeof property[0] === 'string' && property[0] !== '/') {
      return '/' + property;
    } else {
      return property;
    }
  },

  readConfig: function (configFile, defaultConfig) {
    var config;

    try {
      config = JSON.parse(configFile);
    } catch (err) {
      console.log("Invalid JSON in Config-File:");
      console.log(err);
      throw new Error("Invalid JSON in Config-File!");
    }

    console.log("Read Config:", config);

    return config;
  }
};

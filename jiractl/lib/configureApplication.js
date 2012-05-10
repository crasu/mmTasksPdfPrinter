module.exports = function (app, express, secretSessionHashKey) {
  app.configure(function () {
    app.set('views', __dirname + '/../templates');
    app.set('view engine', 'jst');
    app.set('view options', {
      layout: false
    });
    app.use(express.bodyParser());
    app.use(express.methodOverride());
    app.use(express.cookieParser());
    app.use(express.session({
      secret: secretSessionHashKey
    }));
    app.use(app.router);
    app.use(express['static'](__dirname + '/../public'));
  });

  app.configure('development', function () {
    app.use(express.errorHandler({
      dumpExceptions: true,
      showStack: true
    })); 
  });

  app.configure('production', function () {
    app.use(express.errorHandler()); 
  });
};

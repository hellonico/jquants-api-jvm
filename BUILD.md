# Building

Invoke a library API function from the command-line:

    $ clojure -X hellonico.jquants-api/daily-fuzzy {:CompanyNameEnglish "KAWASE" :date 20220920} 
    

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build a JAR (this will fail until you edit the tests to pass):

    $ clojure -T:build mine

Install it locally (requires the `mine` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.hellonico/jquants-api on clojars.org by default.
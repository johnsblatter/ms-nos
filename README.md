ms-nos
======

A simple network operating system for Microservices (java implementation). This is a work in progress, the first public stable release will be available soon!

[![Build Status](https://travis-ci.org/workshare/ms-nos.svg?branch=master)](https://travis-ci.org/workshare/ms-nos)
<!--
[![Coverage Status](https://coveralls.io/repos/bbossola/ms-nos/badge.png)](https://coveralls.io/r/workshare/ms-nos)
-->

[![Built with Maven](http://maven.apache.org/images/logos/maven-feather.png)](http://maven.apache.org/)

## Summary
MSNOS is a library built in order to mantain and survive a microservice based architecture :) Every microservice will surface spontaneously in a cloud, without the need of any application configuration 

#### What is the context?
- you use a microservice architecture
- microservice are distributed across the world
- each microservice provide different APIs
- several instances of the same microservices are available

#### How do I publish a new microservice?
The microservice will explicitly publish his own APIs to the cloud, and every other microservice will automatically discover it
```
  // create a cloud and join it
  cloud = new Microcloud(new Cloud(params.uuid(), params.signature()));
  self = new Microservice(params.name());
  self.join(cloud);

  // publish to the cloud myendpoints
  RestApi[] { apis = new RestApi[] {
    new RestApi("/hello", port),
    new RestApi("/wassup", port),
    new RestApi("/health", port).asHealthCheck(),
    new RestApi("/msnos", port, Type.MSNOS_HTTP),
  };
  self.publish(apis);
```

#### How do a microservice find the best API to use?
Each node in an msnos powered system mantains a full list of all available APIs and it's capable to select the best one for your call using strategies based on location, load and availability
```
  api = cloud.find(self, '/hello')
```

#### How do I expose some APIs to the external world?
A [fast http non-blocking i/o proxy](https://github.com/workshare/ms-nos-proxy) is available out of the box, and it will automatically expose any public API of the cloud to the external world


## License

Released under the MIT License.  See the [LICENSE](LICENSE) file for further details.


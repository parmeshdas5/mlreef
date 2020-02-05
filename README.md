![Build Status](https://gitlab.com/mlreef/frontend/badges/master/build.svg)

MLReef Frontend
====================
Please read the [Contribution Guidelines](CONTRIBUTE.md) carefully


### Module Structure
* **/**:   the root module contains docker and infrastructure sources
* **web**: the npm based react frontend


Getting Started
--------------------

### 1. Setup your developer environment
 1. Install latest Docker
 2. Login to our private docker registry with your gitlab credentials by executing `docker login registry.gitlab.com`


### 2. Setup Gitlab (in docker) infrastructure
For running locally we are using _docker-compose_. The full docker compose file contains all services (inluding the frontend).


#### MacOs / Linux:
For starting and setting up your local docker environment run: `bin/setup-local-environment.sh` and follow the instructions for setting up your local instance including gitlab runners

During setup, you **WILL** ecounter the following error message: `ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0` until the setup has completed successfully.

This is the gitlab runner complain about its missing configuration. As soon as the last step of the `bin/docker-compose-new.sh` script has been performed you will encounter no more errors.

The runner hot-reloads the config file ervery 5 sconds, so no restart is necessary.

#### Windows:
For starting and setting up your local docker environment run: `bin/setup-local-environment.bat` and follow the instructions for setting up your local instance, but without gitlab runners.
gitlab runners are not working yet under windows.

For understanding:

* this injects the GITLAB_ADMIN_TOKEN into the gitlab-postgres database in a encrypted way.
  * **Do not change the gitlab salts afterwards**, as everything encrypted (all tokens and passwords) would then be lost.
* gitlab runners will be set up via browser




### Start Frontend Development
* Stop the frontend docker  `docker stop frontend`
* and start locally with "npm start"

For actually developing in the frontend, you might want to remove (or comment) the complete `frontend`
section from the `docker-compose.yml` file.

For further information on running locally, please refer to the web module's [README.md](web/README.md)


Styleguide
--------------------
Here is the XD file for the definitions of all elements: https://xd.adobe.com/spec/e23711b1-f385-4729-5034-632fbe73bb6b-9406/

**Color Pallette:**

<p>Deep dark (primary blue): #1D2B40</p>
<p>Almost white (base color, very light grey): #e5e5e5</p>
<p>Signal fire (red): #f5544d</p>
<p>Algae (green): #15B785</p>
<p>Sponge (yellow): #EABA44</p>
<p>Lagoon (light blue): #16ADCE</p>
<p>Less white (grey): #b2b2b2</p>

**UI Elements**
In this [list](uielements.md) you will all used standard UI elements. 



Infrastructure
--------------------

### Per-Branch-Environment Cloud Deployment
MLReef's infrastructure is deployed to the AWS cloud automatically. One separate fresh environment for every branch
(feature-branch or other) deployed freshly for every developer push.

These per-brachn-instances are created and installed automatically and receive an autogenerated IP adress and correspongint URL like for example: `ec2-3-122-224-139.eu-central-1.compute.amazonaws.com`

After installation the command `docker-compose up` is issued which starts the complete docker stack on the machine.

Currently, the following steps are performed automatically during deployment.
1. Build docker image for the frontend NodeJS App
2. Provision a new ec2 instance with docker pre-installed
3. Docker's `.env` file is created which contains the URL of the new instance
4. The Frontend's `web/.env` file is updated which contains the URL of the new instance
5. Start the MLReef service stack - based on our `docker-compose.yml` - on the ec2 instance
   The service stack consists of postgres, redis, gitlab, gitlab runner, micro services, NodeJS Frontend
6. Configure the gitlab-runner-dispatcher
   (This has to be done after gitlab has successfully started)
   The Gitlab runner dispatcher boots new ec2 instances for running gitlab pipelines

After a branch is merged/deleted the Gitlab pipeline is used to delete the ec2 instance.


### Networking
All the services defined in the _docker-compose.yml_ run as separate containers inside the docker network. This means that they can talk to each other using local ULRS. The backend for example can access the local Gitlab instance **internally** via http://gitlab:80.

From the **outside** the adresses and ports are mapped a little bit differnetly. The Gitlab instance is reachable direclty at `http://ec-123-123-123-123..eu-central-1.compute.amazonaws.com:10080` (note the port number). Please look at the _docker-compose.yml_ for the correct port numbers of the ther services


### HTTP Reverse Proxy
The _docker-compose.yml_ features **nginx** as reverse proxy running on port 80. One reason for this setup is, so that
we can route API calls to the Backend, Gitlab and other services through the same _orgign_ and preven CORS errors.


### Instance Maintainance
To connect to a development instance you will need either the IP or hostname of the instance.
This can be found in the [Environments Section](https://gitlab.com/mlreef/frontend/-/environments) on gitlab.com

You will also need the most current `development.pem` which contains the private key to authenticate with the ec2 instance

To login to the an instance located at `ec2-123-123-123-123.eu-central-1.compute.amazonaws.com` use the following command.

```bash
ssh -i "development.pem" ubuntu@ec2-123-123-123-123.eu-central-1.compute.amazonaws.com

# to switch to the root user type:
sudo bash
```

### Docker Maintenance
To display all running containers
```bash
docker ps
docker ps -all
```

To look at the container's logs
```bash
docker logs $CONTAINER_NAME

# to follow the container log live:
docker logs $CONTAINER_NAME -f
```


Initialise Docker for development ENV
--------------------
The systems relies on just one ENV variable which must be provided and must not have a default:

* ***GITLAB_ADMIN_TOKEN*** (secret and mandatory)
  * the Private Access Token (PTA) to access the gitlab container as gitlab admin
  * must not be changed after initialisation (without resetting gitlab)
  * must be provided to backend and gitlab
  * must not be used in the frontend


#### Optional
As we are in development for pre-alpha: Set GITLAB_ADMIN_TOKEN is sufficent.

More ENV vars can be used for local adaption, development and debugging:

* GITLAB_ROOT_URL (optional)
  * may default to "http://gitlab:80" but could be provided anyway in the backend
* GITLAB_SECRETS_DB_KEY_BASE (should be: secret, mandatory)
  * use the same salt for gitlab and for gitlab-postgres
  * salt for encryption: **must not be changed after initialisation!**
  * currently "long-and-random-alphanumeric-string" is used for dev env
* SPRING_PROFILES_ACTIVE (optional defined)
  * provides useful defaults for GITLAB_ROOT_URL and logging output 
  * provide "docker" for docker development env: less logging, recreates the database
  * provide "dev" for development env: much logging, recreates the database
  * provide "test" for testing: uses testcontainers instead of docker services for tests
  * provide "prod" for testing: less logging  

TODO: Refactor this in the frontend:

* BACKEND_INSTANCE_URL (optional)
  * may default to "http://localhost:20080" but must be provided for the frontend connection
* GITLAB_ROOT_URL (optional)
  * may default to "http://gitlab:80" but should be provided anyway in the backend


### Debugging the connection

When, and only when, backend found the gitlab instance the backend will start.
Otherwise a crash will inform you about bad credentials (GITLAB_ADMIN_TOKEN) or a invalid GITLAB_ROOT_URL

You can ask the backend how it feels:

Test if backend is reachable
```bash
curl "http://localhost:8080/api/v1/info/status" -i -X GET -H "Content-Type: application/json" -H "Accept: application/json"
```

Print out information about gitlab connection:
```bash
curl "http://localhost:8080/api/v1/info/health" -i -X GET -H "Content-Type: application/json" -H "Accept: application/json"
```

```bash
docker ps

# you may need to restart backend or gitlab after the setup-gitlab
docker-compose up -d backend

# restart the frontend
docker-compose up -d frontend  nginx-proxy
```

#### Attention: 2nd Proxy for local dev environment
There is a middleware proxy in setupProxy.js to proxy during development (does not need nginx-proxy)

This is meant as a helper for frontend developers and a work-in-progress.

To stop the nginx-proxy run this:
```
cd frontend/
docker stop nginx-proxy
cd web/
npm start
```

#### Register additional test users

As registration is not implemented in the frontend yet, a user must be registered via backend REST-service.
This requirement will vanish, as we are working on a better way to populate with test data.

This is not necessary now, as the backend creates a user when started in "dev" or "docker" spring environment

```bash
curl 'http://localhost:8080/api/v1/auth/register' -i -X POST \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d '{
  "username" : "mlreef",
  "email" : "mlreef-demo@example.org",
  "password" : "password",
  "name" : "name"
}'
```

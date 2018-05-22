# Search Management UI (SMUI) - Manual version 1.3.0

## INSTALLATION

Please follow the above steps in this order.

### Step 1: Install RPM

Example script (command line):

```
rpm -i PATH/search-management-ui-VERSION.noarch.rpm
```

Note:
* Ensure the user running the search-management-ui (e.g. `smui`) has read permission to the necessary files (e.g. binary JAR files) and write permission for logs, temp file as well as application's PID (see "Step 3").
* Ensure `search-management-ui` service is being included to your Server's start up sequence (e.g. `init.d`).
* It might be necessary to execute command with root rights.

### Step 2: Create and configure database (SQL level)

Create MariaDB- or MySQL-database, user and assign according permissions. Example script (SQL):

```
CREATE USER 'smui'@'localhost' IDENTIFIED BY 'smui';
CREATE DATABASE smui;
GRANT ALL PRIVILEGES ON smui.* TO 'smui'@'localhost' WITH GRANT OPTION;
```

### Step 3: Configure runtime and application

#### Configure runtime (shell level)

The following settings can be made on (JVM) runtime level:

variable name | description
--- | ---
`SMUI_CONF_PID_PATH` | Path to Play 2.6 PID file.
`SMUI_CONF_LOG_BASE_PATH` | Base path for the logs to happen.
`SMUI_CONF_LOGBACK_XML_PATH` | logback.xml config file path.
`SMUI_CONF_APP_CONF` | application.conf config file path.
`SMUI_CONF_HTTP_PORT` | Application's HHTP port.

If present, the following file can manipulate these variables:

```
/srv/search-management-ui/service-start-config.sh
```

This config script filename is hard coded and will be called by the start script, if the file is present. Example script:

```
#!/usr/bin/env bash

SMUI_CONF_PID_PATH=/srv/var/run/search-management-ui/play.pid
SMUI_CONF_LOG_BASE_PATH=/srv/var/log
SMUI_CONF_LOGBACK_XML_PATH=${app_home}/../conf/logback.xml
SMUI_CONF_APP_CONF=${app_home}/../conf/application.conf
SMUI_CONF_HTTP_PORT=8080
```

If no config script is present, the startup script will take (in this order):

1. defaults configured in the startup script (e.g. `addJava "-DLOG_BASE_PATH=/var/log`),
2. if none given: Framework's default values

#### Configure application (Play 2.6 configuration level)

The configuration file for the application by default is located under:

```
/usr/share/search-management-ui/conf/application.conf
```

An extension to this file overwriting specific settings should be defined in an own `smui-prod.conf`, e.g.:

Important: This config file - extending the existing settings - must firstly include those settings!

```
include "application.conf"

db.default.url="jdbc:mysql://localhost/smui?autoReconnect=true&useSSL=false"
db.default.username="smui"
db.default.password="smui"

smui2solr.SRC_TMP_FILE="/PATH/TO/TMP/FILE.tmp"
smui2solr.DST_CP_FILE_TO="PATH/TO/SOLR/CORE/CONF/rules.txt"
smui2solr.SOLR_HOST="localhost:8983"

toggle.ui-concept.updown-rules.combined=true
toggle.ui-concept.all-rules.with-solr-fields=true

play.http.secret.key="generated application secret"
```

The following sections describe application configs in more detail.

##### Configure basic settings

The following settings can (and should) be overwritten on application.conf in your own `smui-prod.conf` level:

conf key | description
--- | ---
`db.default.*` | Login host and credentials to the database (connection string)
`smui2solr.SRC_TMP_FILE` | Path to temp file (when rules.txt gernation happens)
`smui2solr.DST_CP_FILE_TO` | Path to productive querqy rules.txt (within Solr context)
`smui2solr.SOLR_HOST` | Solr host
`play.http.secret.key` | Encryption key for server/client communication (Play 2.6 standard)

##### Configure Feature Toggle (application behaviour)

Optional. The following settings in the `application.conf` define its (frontend) behaviour:

conf key | description | default
--- | --- | ---
`toggle.ui-concept.updown-rules.combined` | Show UP(+++) fields instead of separated rule and intensity fields. | `true`
`toggle.ui-concept.all-rules.with-solr-fields` | Offer a separated "Solr Field" input to the user (UP/DOWN, FILTER). | `true`

#### First time start the application

Then first time start the service. Example script (command line):

```
search-management-ui &
```

Or via `service` command, or automatic startup after reboot respectively. Now navigate to SMUI application in the browser (e.g. `http://smui-server:9000/`) and make sure you see the application running (the application needs to bootstrap the database scheme).

### Step 4: Create initial data (SQL level)

Once the database scheme has been established, the initial data can be inserted.

#### Solr Collections to maintain Search Management rules for

There must exist a minimum of 1 Solr Collection, that Search Management rules are maintained for. This must be created before the application can be used. Example script (SQL):

```
INSERT INTO solr_index (name, description) VALUES ('core_name1', 'Solr Search Index/Core');
[...]
```

Note: `solr_index.name` (in this case `core_name1`) will be used as the name of the Solr core, when performing a Core Reload (see `smui2solr.sh`).

#### Initial Solr Fields

Optional. Example script (SQL):

```
INSERT INTO suggested_solr_field (name, solr_index_id) values ('microline1', 1);
[...]
```

Refresh Browser window and you should be ready to go.

#### Convert existing rules.txt

Optional. The following RegEx search and replace pattern can be helpful (example search & replace regexes with Atom.io):

Input terms:
```
From: (.*?) =>
To  : INSERT INTO search_input (term, solr_index_id) VALUES ('$1', 1);\nSET @last_id_si = LAST_INSERT_ID();
```

Synonyms (directed-only assumed):
```
From: ^[ \t].*?SYNONYM: (.*)
To  : INSERT INTO synonym_rule (synonym_type, term, search_input_id) VALUES (1, '$1', @last_id_si);
```

UP/DOWN:
```
From: ^[ \t].*?UP\((\d*)\): (.*)
To  : INSERT INTO up_down_rule (up_down_type, boost_malus_value, term, search_input_id) VALUES (0, $1, '$2', @last_id_si);

From: ^[ \t].*?DOWN\((\d*)\): (.*)
To  : INSERT INTO up_down_rule (up_down_type, boost_malus_value, term, search_input_id) VALUES (1, $1, '$2', @last_id_si);
```

FILTER:
```
tbd
```

DELETE:
```
tbd
```

Replace comments:
```
From: #
To  : --
```

Hint: Other querqy compatible rules not editable with SMUI (e.g. DECORATE) must be removed to have a proper converted SQL script ready.

## MAINTENANCE

## Log data

The Log file(s) by default is/are located under the following path:

```
/var/log/search-management-ui/
```

Server log can be watched by example script (command line):

```
tail -f /var/log/search-management-ui/search-management-ui.log
```

### Add a new Solr Collection (SQL level)

See "Step 4". Example script (SQL):

```
INSERT INTO solr_index (name, description) VALUES ('added_core_name2', 'Added Index/Core Description #2');
INSERT INTO solr_index (name, description) VALUES ('added_core_name3', 'Added Index/Core Description #3');
[...]
```

### Add new Solr Fields (SQL level)

See "Step 4". Example script (SQL):

```
INSERT INTO suggested_solr_field (name, solr_index_id) values ('added_solr_field2', 1);
INSERT INTO suggested_solr_field (name, solr_index_id) values ('added_solr_field3', 1);
[...]
```
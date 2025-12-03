CREATE KEYSPACE IF NOT EXISTS monitoring WITH replication = {'class':'SimpleStrategy','replication_factor':1};

USE monitoring;

CREATE TABLE IF NOT EXISTS service (
    uuid timeuuid PRIMARY KEY,
    service_name text,
    description text,
    updated_time timestamp
);

CREATE TABLE IF NOT EXISTS api_scenario (
    service_uuid timeuuid,
    scenario_uuid timeuuid,
    service_name text,
    url text,
    method text,
    headers map<text, text>,
    requestbody text,
    description text,
    created_time timestamp,
    updated_time timestamp,
PRIMARY KEY (service_uuid, scenario_uuid)
);

CREATE TABLE IF NOT EXISTS tcp_scenario (
    service_uuid timeuuid,
    scenario_uuid timeuuid,
    service_name text,
    ip text,
    port int,
    description text,
    created_time timestamp,
    updated_time timestamp,
PRIMARY KEY (service_uuid, scenario_uuid)
);

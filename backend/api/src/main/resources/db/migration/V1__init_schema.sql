create table devices (
    is_active boolean,
    max_value float4,
    min_value float4,
    last_seen timestamp(6),
    farm_id uuid not null,
    id uuid not null,
    model_id uuid not null,
    adafruit_feed_key varchar(100) unique,
    name varchar(100) not null,
    connection_status varchar(255),
    operating_mode varchar(255),
    status varchar(255) not null,
    primary key (id)
);

create table farms (
    created_at timestamp(6),
    id uuid not null,
    owner_id uuid not null,
    name varchar(100) not null,
    location varchar(255),
    primary key (id)
);

create table models (
    id uuid not null,
    manufacturer varchar(100),
    model_name varchar(100) not null,
    device_type varchar(255) not null,
    metric_type varchar(255) not null,
    primary key (id)
);

create table notifications (
    is_read boolean not null,
    created_at timestamp(6) not null,
    id uuid not null,
    user_id uuid not null,
    message text not null,
    primary key (id)
);

create table rules (
    is_active boolean not null,
    threshold_value float4,
    operator varchar(5),
    action_device_id uuid not null,
    farm_id uuid not null,
    id uuid not null,
    trigger_device_id uuid,
    cron_expression varchar(100),
    rule_name varchar(100) not null,
    action_command varchar(255) not null,
    rule_type varchar(255) not null,
    primary key (id)
);

create table telemetry_data (
    value float4 not null,
    created_at timestamp(6) not null,
    device_id uuid not null,
    id uuid not null,
    metric_type varchar(50) not null,
    primary key (id)
);

create table users (
    created_at timestamp(6),
    id uuid not null,
    username varchar(50) not null unique,
    email varchar(100) not null unique,
    password varchar(255) not null,
    role varchar(255) not null,
    primary key (id)
);

alter table if exists devices add constraint FKqsv24ijb9g3s6dt8438e9dnbf foreign key (farm_id) references farms;
alter table if exists devices add constraint FKirc2ii289xtn4rf4hlaa3t0ul foreign key (model_id) references models;
alter table if exists farms add constraint FKs0bidbivsex2c3d47hs2c8sjl foreign key (owner_id) references users;
alter table if exists notifications add constraint FK9y21adhxn0ayjhfocscqox7bh foreign key (user_id) references users;
alter table if exists rules add constraint FK62wkdrcps9q8rjsv6mp9mfgu foreign key (action_device_id) references devices;
alter table if exists rules add constraint FKehgtw0qe4ln5kmlcto2yh4xfa foreign key (farm_id) references farms;
alter table if exists rules add constraint FKs235mwja19b32ruf63skf4d0c foreign key (trigger_device_id) references devices;

create table users (
    created_at      timestamp(6),
    id              uuid         not null,
    username        varchar(50)  not null unique,
    email           varchar(100) not null unique,
    password        varchar(255) not null,
    role            varchar(255) not null,
    primary key (id)
);

create table farms (
    created_at  timestamp(6),
    id          uuid         not null,
    owner_id    uuid         not null,
    name        varchar(100) not null,
    location    varchar(255),
    primary key (id)
);

create table models (
    id          uuid         not null,
    manufacturer varchar(100),
    model_name  varchar(100) not null,
    device_type varchar(255) not null,
    metric_type varchar(255) not null,
    primary key (id)
);

create table devices (
    is_active          boolean,
    max_value          float4,
    min_value          float4,
    last_seen          timestamp(6),
    farm_id            uuid         not null,
    id                 uuid         not null,
    model_id           uuid         not null,
    adafruit_feed_key  varchar(100) unique,
    name               varchar(100) not null,
    connection_status  varchar(255),
    operating_mode     varchar(255),
    status             varchar(255) not null,
    primary key (id)
);

create table notifications (
    is_read     boolean      not null,
    created_at  timestamp(6) not null,
    id          uuid         not null,
    user_id     uuid         not null,
    message     text         not null,
    primary key (id)
);

create table rules (
    is_active        boolean      not null,
    threshold_value  float4,
    operator         varchar(5),
    action_device_id uuid         not null,
    farm_id          uuid         not null,
    id               uuid         not null,
    trigger_device_id uuid,
    cron_expression  varchar(100),
    rule_name        varchar(100) not null,
    action_command   varchar(255) not null,
    rule_type        varchar(255) not null,
    primary key (id)
);

create table telemetry_data (
    value       float4       not null,
    created_at  timestamp(6) not null,
    device_id   uuid         not null,
    id          uuid         not null,
    metric_type varchar(50)  not null,
    primary key (id)
);

alter table devices      add constraint fk_devices_farm    foreign key (farm_id)          references farms;
alter table devices      add constraint fk_devices_model   foreign key (model_id)         references models;
alter table farms        add constraint fk_farms_owner     foreign key (owner_id)         references users;
alter table notifications add constraint fk_notif_user     foreign key (user_id)          references users;
alter table rules        add constraint fk_rules_action    foreign key (action_device_id) references devices;
alter table rules        add constraint fk_rules_farm      foreign key (farm_id)          references farms;
alter table rules        add constraint fk_rules_trigger   foreign key (trigger_device_id) references devices;
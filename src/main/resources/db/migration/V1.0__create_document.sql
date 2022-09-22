create table document
(
    id         bigserial    not null,
    created_at timestamp    not null default current_timestamp,
    name       varchar(255) not null,
    path       varchar(255) not null,
    status     int4,
    updated_at timestamp    not null default current_timestamp,
    primary key (id)
)
;
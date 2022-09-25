create table presentment
(
    id         bigint                                not null auto_increment,
    attempts   integer     default 0                 not null,
    name       varchar(255)                          not null,
    path       varchar(255)                          not null,
    status     integer                               not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime default CURRENT_TIMESTAMP not null,
    primary key (id)
)
;
create table users (
    id            bigint       not null auto_increment,
    username      varchar(100) not null,
    password_hash varchar(255) not null,
    role          varchar(50)  not null,
    enabled       bit(1)       not null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    deleted_at    datetime(6)  null,
    primary key (id),
    constraint users_username_key unique (username)
);

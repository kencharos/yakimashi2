# --- !Ups

create table T_USER (
  id                  varchar(255) not null,
  password             varchar(255) not null,
  constraint pk_T_USER primary key (id))
;

create table T_LABEL (
  id                  varchar(10) not null,
  name             varchar(255) not null,
  constraint PK_T_LABEL primary key (id))
;

create table T_PHOTO (
  album    varchar(255) not null,
  name     varchar(255) not null,
  etc   integer,
  comment varchar(300),
  no_disp boolean,
  constraint PK_T_PHOTO primary key (album, name))
;


create table T_PHOTO_IMAGE (
  album    varchar(255) not null,
  name     varchar(255) not null,
  image    bytea not null,
  constraint PK_T_PHOTO_IMAGE primary key (album, name))
;

create table T_PHOTO_REQUEST (
  album                  varchar(255) not null,
  name             varchar(255) not null,
  label_id                  varchar(10) not null,
  constraint PK_T_PHOTO_REQUEST primary key (album, name, label_id))
;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists T_USER;
drop table if exists T_LABEL;
drop table if exists T_PHOTO;
drop table if exists T_PHOTO_REQUEST;
drop table if exists T_PHOTO_IMAGE;

SET REFERENTIAL_INTEGRITY TRUE;



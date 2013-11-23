# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "tweet" ("id" SERIAL NOT NULL PRIMARY KEY,"content" json NOT NULL,"fetchedAt" timestamp NOT NULL);

# --- !Downs

drop table "tweet";


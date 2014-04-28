# --- !Ups

create table "reminders" ("id" SERIAL NOT NULL PRIMARY KEY,"userid" BIGINT NOT NULL,"createdat" timestamp NOT NULL,"repeat" VARCHAR(254) NOT NULL,"firsttime" timestamp NOT NULL,"what" VARCHAR(254) NOT NULL,"tweetId" BIGINT NOT NULL);
create table "scheduledreminders" ("id" SERIAL NOT NULL PRIMARY KEY,"reminderid" BIGINT NOT NULL,"userid" BIGINT NOT NULL,"time" timestamp NOT NULL,"executed" BOOLEAN NOT NULL,"cancelled" BOOLEAN NOT NULL);
create table "tweets" ("id" SERIAL NOT NULL PRIMARY KEY,"userId" BIGINT NOT NULL,"twitterid" BIGINT NOT NULL,"screenName" VARCHAR(254) NOT NULL,"content" json NOT NULL,"fetchedat" timestamp NOT NULL);
create table "users" ("id" SERIAL NOT NULL PRIMARY KEY,"screenname" VARCHAR(254) NOT NULL,"createdat" timestamp NOT NULL);

# --- !Downs

drop table "reminders";
drop table "scheduledreminders";
drop table "tweets";
drop table "users";

